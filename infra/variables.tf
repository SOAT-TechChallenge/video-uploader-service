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
  default     = "leynerbueno/video-uploader-service"
}

variable "docker_image_tag" {
  description = "Tag da imagem"
  default     = "latest"
}

variable "aws_access_key_id" {
  description = "AWS Access Key ID"
  type        = string
  sensitive   = true
  default     = "ASIAUJBCHKTQ3T2CX5Q6"
}

variable "aws_secret_access_key" {
  description = "AWS Secret Access Key"
  type        = string
  sensitive   = true
  default     = "6JtKu6harqvxeajOGXEEClQ04KOptfD4XShYKO43"
}

variable "aws_session_token" {
  description = "AWS Session Token"
  type        = string
  sensitive   = true
  default     = "IQoJb3JpZ2luX2VjEPj//////////wEaCXVzLXdlc3QtMiJHMEUCIEPys8syD4E8qtPL7omgx0nSXRvCZV+RoK3Ek3SIpv2sAiEA6Z0aeF2WOvxVkiP9KRLLp3KCOYByaLFGW/zdidW9OpsqwQIIwf//////////ARABGgwyOTQyNzcwNDM0MjUiDKVH11mfZN5N8TU3DCqVAsWrxmVZJB12I5QHAiVm5RACSVcxqIjzSg4vH165y+fDr0q4I9MvdCJ6C0tHMOCQS5WFG6f6bGN48z4QgemKrSHsHYngZXAKBQHG9JHoTZ6Rkm4C8+HVr00Xx+MwAVyBZUD4jqlBCN4dkLyl2jvRqzglSOA6Pn+UbDULYmks/0BNhFyW0aPXsUT4IfbjThHzi4zIhMAYJoBEaKYgZrFXNOU1fi6ZJi8JujLVEYe0uCCwv+jTaXrQkYjN9v9ztmxzjIXghtICdA7XsxgwYh4+n+nGxcrz1WM4vDABkY3oPS33IpmGXhWP65n0+Tz66GzW+gdwj8qKheB17N1YXGDA96DHT9VPtosNzt18qVFqjP3DMg4fV/gw3Kj6ywY6nQHpPsig2ew3MGVsLD/VpXbmvmTIWKQR6i1GR+EHG7F20MZbjSIrinMFCBHJ71phs5i12ecajC92SE+0mosfbbkZ0qnEke/9u5KyWPom4TZa/3ZpIgjlXbv/UhdNliAu2dwz2bdf+LiwGynMzaCOXsLUulcLqpw0sHfGAYwLdzPIZi5fWibl08yqPop6XhUiHj5fctKCxxA8YdZ0JY4m"
  }

variable "aws_s3_bucket" {
  description = "Nome do bucket S3 para vídeos"
  type        = string
  default     = "challenge-hackathon"
}

variable "aws_sqs_queue_url" {
  description = "URL da fila SQS"
  type        = string
  default     = "https://sqs.us-east-1.amazonaws.com/294277043425/challenge-hackathon"
}

variable "jwt_secret" {
  description = "Secret key para assinatura de tokens JWT"
  type        = string
  default     = "techChallenge"
}