output "cluster_name" {
  value = aws_eks_cluster.main.name
}

output "selected_subnets" {
  value = local.selected_subnets
}

output "load_balancer_url" {
  value = "http://${kubernetes_service.app.status.0.load_balancer.0.ingress.0.hostname}"
  depends_on = [kubernetes_service.app]
}

output "namespace" {
  value = kubernetes_namespace.app.metadata[0].name
}

output "service_name" {
  value = kubernetes_service.app.metadata[0].name
}