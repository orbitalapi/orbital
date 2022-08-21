output "chart_repository_url" {
  value = aws_s3_bucket.chart_repository.bucket_domain_name
}
