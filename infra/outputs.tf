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

output "ssm_alb_dns_path" {
  value = aws_ssm_parameter.uploader_alb_dns.name
}
output "ssm_sqs_url_path" {
  value = aws_ssm_parameter.sqs_queue_url.name
}
output "ssm_s3_bucket_path" {
  value = aws_ssm_parameter.video_bucket_name.name
}
