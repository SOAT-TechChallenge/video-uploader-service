variable "aws_region" {
  description = "Região AWS"
  default     = "us-east-1"
}

variable "app_name" {
  description = "Nome da aplicação"
  default     = "video-uploader-service"
}

variable "cluster_name" {
  description = "Nome do cluster EKS"
  default     = "video-uploader-cluster"
}

variable "container_port" {
  description = "Porta do container"
  default     = 8080
}

variable "docker_image" {
  description = "Imagem Docker"
  default     = "breno091073/video-uploader-service"
}

variable "docker_image_tag" {
  description = "Tag da imagem"
  default     = "latest"
}

variable "aws_access_key_id" {
  description = "AWS Access Key ID"
  type        = string
  sensitive   = true
  default     = ""
}

variable "aws_secret_access_key" {
  description = "AWS Secret Access Key"
  type        = string
  sensitive   = true
  default     = ""
}

variable "aws_session_token" {
  description = "AWS Session Token"
  type        = string
  sensitive   = true
  default     = ""
}

variable "aws_s3_bucket" {
  description = "Nome do bucket S3 para vídeos"
  type        = string
  default     = ""
}

variable "aws_sqs_queue_url" {
  description = "URL da fila SQS"
  type        = string
  default     = ""
}