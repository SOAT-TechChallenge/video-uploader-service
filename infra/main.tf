terraform {
  required_version = ">= 1.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = "us-east-1"
}

# -----------------------
# VPC E SUBNETS (DEFAULT)
# -----------------------
data "aws_vpc" "default" {
  default = true
}

data "aws_subnets" "all" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.default.id]
  }
}

# -----------------------
# IAM ROLE (LAB)
# -----------------------
data "aws_iam_role" "lab_role" {
  name = "LabRole"
}

# -----------------------
# SECURITY GROUP - ALB
# -----------------------
resource "aws_security_group" "alb_sg" {
  name        = "video-uploader-service-alb-sg"
  description = "Security group for ALB"
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

  tags = {
    Name = "video-uploader-service-alb-sg"
  }
}

# -----------------------
# SECURITY GROUP - ECS
# -----------------------
resource "aws_security_group" "ecs_sg" {
  name        = "video-uploader-service-ecs-sg"
  description = "Security group for ECS tasks"
  vpc_id      = data.aws_vpc.default.id

  ingress {
    from_port       = 8080
    to_port         = 8080
    protocol        = "tcp"
    security_groups = [aws_security_group.alb_sg.id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "video-uploader-service-ecs-sg"
  }
}

# -----------------------
# ALB
# -----------------------
resource "aws_lb" "video_uploader_alb" {
  name               = "video-uploader-service-alb"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [aws_security_group.alb_sg.id]
  subnets            = slice(data.aws_subnets.all.ids, 0, min(2, length(data.aws_subnets.all.ids)))

  enable_deletion_protection = false

  tags = {
    Name = "video-uploader-service-alb"
  }
}

# -----------------------
# TARGET GROUP
# -----------------------
resource "aws_lb_target_group" "video_uploader_tg" {
  name        = "video-uploader-tg"
  port        = 8080
  protocol    = "HTTP"
  vpc_id      = data.aws_vpc.default.id
  target_type = "ip"

  health_check {
    path                = "/"
    interval            = 60
    timeout             = 30
    healthy_threshold   = 2
    unhealthy_threshold = 5
    matcher             = "200-399"
  }

  tags = {
    Name = "video-uploader-tg"
  }
}

# -----------------------
# LISTENER
# -----------------------
resource "aws_lb_listener" "video_uploader_listener" {
  load_balancer_arn = aws_lb.video_uploader_alb.arn
  port              = "80"
  protocol          = "HTTP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.video_uploader_tg.arn
  }
}

# -----------------------
# ECS CLUSTER
# -----------------------
resource "aws_ecs_cluster" "video_uploader_cluster" {
  name = "video-uploader-cluster"

  setting {
    name  = "containerInsights"
    value = "disabled"
  }

  tags = {
    Name = "video-uploader-cluster"
  }
}

# -----------------------
# TASK DEFINITION
# -----------------------
resource "aws_ecs_task_definition" "video_uploader_task" {
  family                   = "video-uploader-service"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = 512
  memory                   = 1024
  execution_role_arn       = data.aws_iam_role.lab_role.arn

  container_definitions = jsonencode([
    {
      name  = "video-uploader-service"
      image = "rodrigopatricio19/video-uploader-service:latest"

      portMappings = [{
        containerPort = 8080
        hostPort      = 8080
        protocol      = "tcp"
      }]

      essential = true

      healthCheck = {
        command     = ["CMD-SHELL", "wget -q -O - http://localhost:8080/ || exit 1"]
        interval    = 60
        timeout     = 20
        retries     = 3
        startPeriod = 120
      }

      logConfiguration = {
        logDriver = "awslogs"
        options = {
          awslogs-group         = "/ecs/video-uploader-service"
          awslogs-region        = "us-east-1"
          awslogs-stream-prefix = "ecs"
        }
      }

      environment = [
        {
          name  = "SERVER_PORT"
          value = "8080"
        },
        {
          name  = "AWS_REGION"
          value = "us-east-1"
        },
        {
          name  = "AWS_S3_BUCKET"
          value = "my-video-bucket"
        },
        {
          name  = "AWS_SQS_QUEUE_URL"
          value = "https://sqs.us-east-1.amazonaws.com/123456789012/video-queue"
        }
      ]
    }
  ])

  tags = {
    Name = "video-uploader-service-task"
  }
}

# -----------------------
# ECS SERVICE
# -----------------------
resource "aws_ecs_service" "video_uploader_service" {
  name            = "video-uploader-service"
  cluster         = aws_ecs_cluster.video_uploader_cluster.id
  task_definition = aws_ecs_task_definition.video_uploader_task.arn
  desired_count   = 1
  launch_type     = "FARGATE"

  health_check_grace_period_seconds = 600

  network_configuration {
    security_groups  = [aws_security_group.ecs_sg.id]
    subnets          = slice(data.aws_subnets.all.ids, 0, min(2, length(data.aws_subnets.all.ids)))
    assign_public_ip = true
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.video_uploader_tg.arn
    container_name   = "video-uploader-service"
    container_port   = 8080
  }

  deployment_circuit_breaker {
    enable   = true
    rollback = true
  }

  tags = {
    Name = "video-uploader-service"
  }
}

# -----------------------
# LOG GROUP
# -----------------------
resource "aws_cloudwatch_log_group" "video_uploader_service" {
  name              = "/ecs/video-uploader-service"
  retention_in_days = 7
}
