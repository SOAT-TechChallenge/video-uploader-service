output "alb_dns_name" {
  description = "DNS name of the ALB"
  value       = aws_lb.video_uploader_alb.dns_name
}

output "service_url" {
  description = "URL para acessar o serviço"
  value       = "http://${aws_lb.video_uploader_alb.dns_name}"
}

output "ecs_cluster_name" {
  description = "Name of the ECS cluster"
  value       = aws_ecs_cluster.video_uploader_cluster.name
}

output "ecs_service_name" {
  description = "Name of the ECS service"
  value       = aws_ecs_service.video_uploader_service.name
}

output "ecs_task_definition_family" {
  description = "Task Definition family name"
  value       = aws_ecs_task_definition.video_uploader_task.family
}
