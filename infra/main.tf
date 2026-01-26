terraform {
  required_version = ">= 1.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = "~> 2.23"
    }
  }
}

provider "aws" {
  region = var.aws_region
}

data "aws_caller_identity" "current" {}

# Data source para a VPC default
data "aws_vpc" "default" {
  default = true
}

# Listar todas as subnets da VPC default
data "aws_subnets" "all" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.default.id]
  }
}

# Data source para obter detalhes de cada subnet
data "aws_subnet" "details" {
  for_each = toset(data.aws_subnets.all.ids)
  id       = each.value
}

locals {
  eks_supported_zones = ["us-east-1a", "us-east-1b", "us-east-1c", "us-east-1d", "us-east-1f"]

  # Filtrar subnets que estão nas zonas suportadas
  filtered_subnets = [
    for subnet_id in data.aws_subnets.all.ids :
    subnet_id
    if contains(local.eks_supported_zones, data.aws_subnet.details[subnet_id].availability_zone)
  ]

  # Usar apenas as primeiras 2-3 subnets
  selected_subnets = slice(local.filtered_subnets, 0, min(3, length(local.filtered_subnets)))

  common_tags = {
    Project = var.app_name
  }

  cluster_name = var.cluster_name

  # Porta que aberta nos Nodes para o ALB conversar com o K8s
  node_port = 30007
}

# --- 1. Security Groups (Atualizados para ALB) ---

resource "aws_security_group" "alb_sg" {
  name        = "${var.app_name}-alb-sg"
  description = "Security group for ALB - Allow HTTP"
  vpc_id      = data.aws_vpc.default.id

  ingress {
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
    description = "Allow HTTP from world"
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = local.common_tags
}

# Security Group para o Cluster EKS
resource "aws_security_group" "eks_cluster" {
  name        = "${var.cluster_name}-cluster-sg"
  description = "Security group for EKS cluster"
  vpc_id      = data.aws_vpc.default.id

  ingress {
    description = "Allow pods to communicate with each other"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = [data.aws_vpc.default.cidr_block]
  }

  ingress {
    description = "Allow worker nodes to communicate with cluster"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = [data.aws_vpc.default.cidr_block]
  }

  # Permitir que o ALB fale com os Nodes na porta do NodePort
  ingress {
    description     = "Allow ALB to access NodePort"
    from_port       = local.node_port
    to_port         = local.node_port
    protocol        = "tcp"
    security_groups = [aws_security_group.alb_sg.id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = local.common_tags
}

# --- 2. EKS Cluster & Nodes ---

resource "aws_eks_cluster" "main" {
  name     = local.cluster_name
  role_arn = "arn:aws:iam::${data.aws_caller_identity.current.account_id}:role/LabRole"

  vpc_config {
    subnet_ids              = local.selected_subnets
    endpoint_public_access  = true
    endpoint_private_access = false
    security_group_ids      = [aws_security_group.eks_cluster.id]
  }

  tags = local.common_tags
}

resource "aws_eks_node_group" "main" {
  cluster_name    = aws_eks_cluster.main.name
  node_group_name = "default-nodegroup"
  node_role_arn   = "arn:aws:iam::${data.aws_caller_identity.current.account_id}:role/LabRole"
  subnet_ids      = local.selected_subnets

  scaling_config {
    desired_size = 1
    max_size     = 2
    min_size     = 1
  }

  instance_types = ["t3.medium"]
  capacity_type  = "ON_DEMAND"

  tags = local.common_tags

  depends_on = [aws_eks_cluster.main]
}

# --- 3. ALB MANUAL (Para aplicar a regra de Header) ---

resource "aws_lb" "app_alb" {
  name                       = "${var.app_name}-alb"
  internal                   = false
  load_balancer_type         = "application"
  security_groups            = [aws_security_group.alb_sg.id]
  subnets                    = local.selected_subnets
  enable_deletion_protection = false

  tags = local.common_tags
}

resource "aws_lb_target_group" "app_tg" {
  name     = "${var.app_name}-tg"
  port     = local.node_port # Aponta para a porta exposta no Node
  protocol = "HTTP"
  vpc_id   = data.aws_vpc.default.id

  health_check {
    path                = "/actuator/health"
    port                = "traffic-port" # Usa a mesma porta do NodePort
    interval            = 30
    timeout             = 5
    healthy_threshold   = 2
    unhealthy_threshold = 2
    matcher             = "200"
  }
}

# Listener Padrão (Bloqueia tudo)
resource "aws_lb_listener" "http" {
  load_balancer_arn = aws_lb.app_alb.arn
  port              = "80"
  protocol          = "HTTP"

  default_action {
    type = "fixed-response"

    fixed_response {
      content_type = "text/plain"
      message_body = "Acesso Direto Negado."
      status_code  = "403"
    }
  }
}

# Regra VIP (Libera com Header)
resource "aws_lb_listener_rule" "allow_gateway" {
  listener_arn = aws_lb_listener.http.arn
  priority     = 100

  action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.app_tg.arn
  }

  condition {
    http_header {
      http_header_name = "x-apigateway-token"
      values           = ["tech-challenge-hackathon"]
    }
  }
}

# Conecta o Auto Scaling Group do EKS ao Target Group do ALB
resource "aws_autoscaling_attachment" "eks_nodes_to_tg" {
  # Pega o nome do ASG criado dinamicamente pelo EKS Node Group
  autoscaling_group_name = aws_eks_node_group.main.resources[0].autoscaling_groups[0].name
  lb_target_group_arn    = aws_lb_target_group.app_tg.arn
  
  depends_on = [aws_eks_node_group.main, aws_lb_target_group.app_tg]
}

# --- 4. Kubernetes Resources ---

data "aws_eks_cluster_auth" "cluster_auth" {
  name = aws_eks_cluster.main.name
}

provider "kubernetes" {
  host                   = aws_eks_cluster.main.endpoint
  cluster_ca_certificate = base64decode(aws_eks_cluster.main.certificate_authority[0].data)
  token                  = data.aws_eks_cluster_auth.cluster_auth.token

  exec {
    api_version = "client.authentication.k8s.io/v1beta1"
    args        = ["eks", "get-token", "--cluster-name", aws_eks_cluster.main.name]
    command     = "aws"
  }
}

resource "kubernetes_namespace" "app" {
  metadata {
    name = var.app_name
  }
  depends_on = [aws_eks_node_group.main]
}

resource "kubernetes_secret" "aws_credentials" {
  metadata {
    name      = "aws-credentials"
    namespace = kubernetes_namespace.app.metadata[0].name
  }

  data = {
    AWS_ACCESS_KEY_ID     = var.aws_access_key_id
    AWS_SECRET_ACCESS_KEY = var.aws_secret_access_key
    AWS_SESSION_TOKEN     = var.aws_session_token
  }

  type = "Opaque"
}

resource "kubernetes_config_map" "app_config" {
  metadata {
    name      = "${var.app_name}-config"
    namespace = kubernetes_namespace.app.metadata[0].name
  }

  data = {
    AWS_S3_BUCKET      = var.aws_s3_bucket
    AWS_SQS_QUEUE_URL  = var.aws_sqs_queue_url
    AWS_REGION         = var.aws_region
    SERVER_PORT        = "8080"
    LOG_LEVEL          = "INFO"
    SPRING_PROFILES_ACTIVE = "prod"
  }
}

resource "kubernetes_deployment" "app" {
  metadata {
    name      = var.app_name
    namespace = kubernetes_namespace.app.metadata[0].name
  }

  spec {
    replicas = 1

    selector {
      match_labels = {
        app = var.app_name
      }
    }

    template {
      metadata {
        labels = {
          app = var.app_name
        }
      }

      spec {
        container {
          name  = var.app_name
          image = "${var.docker_image}:${var.docker_image_tag}"

          port {
            container_port = var.container_port
          }

          env_from {
            config_map_ref {
              name = kubernetes_config_map.app_config.metadata[0].name
            }
          }

          env {
            name = "AWS_ACCESS_KEY_ID"
            value_from {
              secret_key_ref {
                name = kubernetes_secret.aws_credentials.metadata[0].name
                key  = "AWS_ACCESS_KEY_ID"
              }
            }
          }

          env {
            name = "AWS_SECRET_ACCESS_KEY"
            value_from {
              secret_key_ref {
                name = kubernetes_secret.aws_credentials.metadata[0].name
                key  = "AWS_SECRET_ACCESS_KEY"
              }
            }
          }

          env {
            name = "AWS_SESSION_TOKEN"
            value_from {
              secret_key_ref {
                name = kubernetes_secret.aws_credentials.metadata[0].name
                key  = "AWS_SESSION_TOKEN"
              }
            }
          }

          env {
            name  = "AWS_DEFAULT_REGION"
            value = var.aws_region
          }

          env {
            name  = "AWS_REGION"
            value = var.aws_region
          }

          liveness_probe {
            tcp_socket {
              port = var.container_port
            }
            initial_delay_seconds = 60
            period_seconds        = 10
            timeout_seconds       = 5
            failure_threshold     = 3
          }

          readiness_probe {
            tcp_socket {
              port = var.container_port
            }
            initial_delay_seconds = 10
            period_seconds        = 5
            timeout_seconds       = 5
            failure_threshold     = 3
          }

          resources {
            limits = {
              cpu    = "500m"
              memory = "1024Mi"
            }
            requests = {
              cpu    = "250m"
              memory = "512Mi"
            }
          }
        }
      }
    }
  }

  depends_on = [
    kubernetes_config_map.app_config,
    kubernetes_secret.aws_credentials
  ]
}

# --- Service Alterado: NodePort ---
resource "kubernetes_service" "app" {
  metadata {
    name      = var.app_name
    namespace = kubernetes_namespace.app.metadata[0].name
  }

  spec {
    selector = {
      app = var.app_name
    }

    # NodePort abre uma porta fixa nos Nodes
    # para o ALB conseguir entregar o tráfego
    type = "NodePort"

    port {
      port        = 80
      target_port = var.container_port
      node_port   = local.node_port
      protocol    = "TCP"
    }
  }

  depends_on = [kubernetes_deployment.app]
}