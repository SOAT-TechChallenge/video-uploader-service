output "ecr_repo_url" { value = aws_ecr_repository.video_uploader.repository_url }
output "api_url"      { value = "http://${aws_lb.uploader_alb.dns_name}" }
output "s3_bucket_name" { value = aws_s3_bucket.video_bucket.bucket }
output "sqs_queue_url"  { value = aws_sqs_queue.video_queue.url }