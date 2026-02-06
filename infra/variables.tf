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


variable "aws_access_key_id" {
  description = "AWS Access Key ID"
  type        = string
  sensitive   = true
}

variable "aws_secret_access_key" {
  description = "AWS Secret Access Key"
  type        = string
  sensitive   = true
}

variable "aws_session_token" {
  description = "AWS Session Token"
  type        = string
  sensitive   = true
}

variable "jwt_secret" {
  description = "Secret key para assinatura de tokens JWT"
  type        = string
  sensitive   = true
}