output "cluster_name" {
  value = aws_eks_cluster.main.name
}

output "selected_subnets" {
  value = local.selected_subnets
}

# --- CORREÇÃO AQUI ---
output "load_balancer_url" {
  description = "URL pública do Application Load Balancer"
  value       = "http://${aws_lb.app_alb.dns_name}"
}

output "namespace" {
  value = kubernetes_namespace.app.metadata[0].name
}

output "service_name" {
  value = kubernetes_service.app.metadata[0].name
}