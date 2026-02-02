variable "aws_region" {
  description = "Região AWS"
  default     = "us-east-1"
}

variable "app_name" {
  description = "Nome da aplicação"
  default     = "video-uploader-service"
}

variable "cluster_name" {
  description = "Nome do cluster ECS"
  default     = "video-uploader-cluster"
}

variable "container_port" {
  description = "Porta do container"
  default     = 8080
}


variable "aws_s3_bucket" {
  description = "Nome do bucket S3 para vídeos"
  type        = string
  default     = "challenge-hackathon-video-assets" 
}
variable "aws_access_key_id" {
  description = "AWS Access Key ID"
  type        = string
  sensitive   = true
  default     = "ASIAUJBCHKTQWRHV657V"
}

variable "aws_secret_access_key" {
  description = "AWS Secret Access Key"
  type        = string
  sensitive   = true
  default     = "bogZ4Yd1g/61DIjzifvdl+6ZS9zVbbaqvvRkeLvV"
}

variable "aws_session_token" {
  description = "AWS Session Token"
  type        = string
  sensitive   = true
  default     = "IQoJb3JpZ2luX2VjEA8aCXVzLXdlc3QtMiJIMEYCIQC/7d1QQyXLnkfSA9oPZk2TWCnjHMjrA3Byx2YO2VelRwIhAODDlHeGILJFgIpCMZFCCqFNjViq7kipoXVr8n4FxGYeKsECCNj//////////wEQARoMMjk0Mjc3MDQzNDI1Igz+MWl8F55AhgH7tcsqlQJVldW72uUPfs6xe6EldssHeAuWH2aeJhg60FAWXhXyRIT3vjCwCe0C/j+pRjDAgEhXX471cZoLv3vnfvCAh+BHSmGLQugCpU7QHqqZ+Aq8AEbMvBBtad4VO5pF3rKB7iLNGFQXa+tfWrX94GjWkDvwkuC2Zr7Hso9wNZmqqRAfICkjmSIz8mVzCXuA0gGaJbJvPixLlOsxerdZit+odvFmkDm6qQnjhVFzcIjUW0BYK56c/V7IxHDjWz8sqVAB0A5ejFpUsQt53gZB+x79/hF2b9/wCD7oH6hCjA+B/AlTk6yiIKEErTQ4zvJOZPHBKpxEYhn+48OkK0gscRtkcxWgAI+5hsXTAI+pyHKODQI1ZIgx6jZcMO21/8sGOpwBPyV9Oj4qGEy1Fh5Lf2EQozMDxn8CGad3nMYqYBQtmlIXuqqpafQv1oIRlkhib7TnRXudkNp/C+7Q1ArFJ+6hrA0tj/uAKRm/CclnYrCrUYBaQpEQpeOP5MOx/FCzt6b5hJxf/p4aTx/QF4kNJOWl8b27qXT4lIWAWJrWntJ/QU/uWyE1//b/RtY6FzQfBaR93RNOnpGYxTnDnkXL"
}

variable "jwt_secret" {
  description = "Secret key para assinatura de tokens JWT"
  type        = string
  default     = "techchallenge"
}