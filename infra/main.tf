terraform {
  required_version = ">= 1.0"
  
  backend "s3" {
    bucket = "challenge-hackathon"
    key    = "video-uploader/terraform.tfstate"
    region = "us-east-1"
  }

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.0"
    }
  }
}

provider "aws" {
  region = var.aws_region
}

# --- Data Sources ---
data "aws_vpc" "default" {
  default = true
}

data "aws_subnets" "all" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.default.id]
  }
}

data "aws_iam_role" "lab_role" {
  name = "LabRole"
}

# --- Gerador de Sufixo Aleatório ---
resource "random_id" "suffix" {
  byte_length = 4
}

# --- Recursos de Dados ---

resource "aws_s3_bucket" "video_bucket" {
  bucket        = "video-storage-${random_id.suffix.hex}"
  force_destroy = true
}

resource "aws_sqs_queue" "video_queue" {
  name                      = "video-processing-queue"
  max_message_size          = 262144
  message_retention_seconds = 86400
  receive_wait_time_seconds = 10
}

# --- ECR ---
resource "aws_ecr_repository" "video_uploader" {
  name                 = "video-uploader-repo"
  image_tag_mutability = "MUTABLE"
  force_delete         = true

  image_scanning_configuration {
    scan_on_push = true
  }
}

# --- Security Groups ---

resource "aws_security_group" "alb_sg" {
  name        = "${var.app_name}-alb-sg"
  description = "SG do Load Balancer"
  vpc_id      = data.aws_vpc.default.id

  ingress {
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_security_group" "ecs_sg" {
  name        = "${var.app_name}-ecs-sg"
  description = "SG da Tarefa ECS"
  vpc_id      = data.aws_vpc.default.id

  ingress {
    from_port       = var.container_port
    to_port         = var.container_port
    protocol        = "tcp"
    security_groups = [aws_security_group.alb_sg.id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

# --- Load Balancer (ALB) ---

resource "aws_lb" "uploader_alb" {
  name                       = "${var.app_name}-alb"
  internal                   = false
  load_balancer_type         = "application"
  security_groups            = [aws_security_group.alb_sg.id]
  subnets                    = slice(data.aws_subnets.all.ids, 0, min(2, length(data.aws_subnets.all.ids)))
  enable_deletion_protection = false
  idle_timeout               = 600
}

resource "aws_lb_target_group" "uploader_tg" {
  name        = "${var.app_name}-tg"
  port        = var.container_port
  protocol    = "HTTP"
  vpc_id      = data.aws_vpc.default.id
  target_type = "ip"

  health_check {
    path                = "/actuator/health" 
    interval            = 60
    timeout             = 30
    healthy_threshold   = 2
    unhealthy_threshold = 5
    matcher             = "200"
  }
}

resource "aws_lb_listener" "http" {
  load_balancer_arn = aws_lb.uploader_alb.arn
  port              = "80"
  protocol          = "HTTP"

  default_action {
    type = "fixed-response"
    fixed_response {
      content_type = "text/plain"
      message_body = "Acesso Direto Negado. Use o API Gateway."
      status_code  = "403"
    }
  }
}

resource "aws_lb_listener_rule" "allow_gateway" {
  listener_arn = aws_lb_listener.http.arn
  priority     = 100

  action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.uploader_tg.arn
  }

  condition {
    http_header {
      http_header_name = "x-apigateway-token"
      values           = ["tech-challenge-hackathon"]
    }
  }
}

# --- ECS Cluster & Task ---

resource "aws_ecs_cluster" "uploader_cluster" {
  name = var.cluster_name
}

resource "aws_ecs_task_definition" "uploader_task" {
  family                   = var.app_name
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = 512
  memory                   = 1024
  execution_role_arn       = data.aws_iam_role.lab_role.arn
  task_role_arn            = data.aws_iam_role.lab_role.arn

  container_definitions = jsonencode([{
    name      = var.app_name
    image     = "${aws_ecr_repository.video_uploader.repository_url}:latest"
    essential = true
    
    portMappings = [{
      containerPort = var.container_port
      hostPort      = var.container_port
      protocol      = "tcp"
    }]

    logConfiguration = {
      logDriver = "awslogs"
      options = {
        awslogs-group         = "/ecs/${var.app_name}"
        awslogs-region        = var.aws_region
        awslogs-stream-prefix = "ecs"
        awslogs-create-group  = "true"
      }
    }

    environment = [
      { name = "SERVER_PORT", value = tostring(var.container_port) },
      { name = "AWS_REGION", value = var.aws_region },
      { name = "JWT_SECRET", value = var.jwt_secret },
      
      { name = "AWS_S3_BUCKET", value = aws_s3_bucket.video_bucket.bucket },
      { name = "AWS_SQS_QUEUE_URL", value = aws_sqs_queue.video_queue.url },

      { name = "AWS_ACCESS_KEY_ID", value = var.aws_access_key_id },
      { name = "AWS_SECRET_ACCESS_KEY", value = var.aws_secret_access_key },
      { name = "AWS_SESSION_TOKEN", value = var.aws_session_token },

      { name = "SPRING_SERVLET_MULTIPART_MAX_FILE_SIZE", value = "500MB" },
      { name = "SPRING_SERVLET_MULTIPART_MAX_REQUEST_SIZE", value = "500MB" }
    ]
  }])
}

resource "aws_ecs_service" "uploader_service" {
  name            = var.app_name
  cluster         = aws_ecs_cluster.uploader_cluster.id
  task_definition = aws_ecs_task_definition.uploader_task.arn
  desired_count   = 1
  launch_type     = "FARGATE"

  health_check_grace_period_seconds = 300

  network_configuration {
    subnets          = slice(data.aws_subnets.all.ids, 0, min(2, length(data.aws_subnets.all.ids)))
    security_groups  = [aws_security_group.ecs_sg.id]
    assign_public_ip = true
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.uploader_tg.arn
    container_name   = var.app_name
    container_port   = var.container_port
  }

  # --- CORREÇÃO AQUI ---
  depends_on = [aws_lb_listener_rule.allow_gateway]
}

# --- Outputs ---

output "ecr_repo_url" {
  value = aws_ecr_repository.video_uploader.repository_url
}

output "api_url" {
  value = "http://${aws_lb.uploader_alb.dns_name}"
}

output "s3_bucket_name" {
  value = aws_s3_bucket.video_bucket.bucket
}

output "sqs_queue_url" {
  value = aws_sqs_queue.video_queue.url
}