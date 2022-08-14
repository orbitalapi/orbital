locals {
  path_to_helm = "${path.module}/../../../helm/vyne-helm/charts"
}

resource "aws_s3_bucket" "chart_repository" {
  bucket = "vyne-helm-chart-repository-${var.environment}"
}

resource "aws_s3_bucket_acl" "chart_repository" {
  bucket = aws_s3_bucket.chart_repository.id
  acl    = "public-read"
}

resource "aws_s3_object" "charts" {
  for_each = fileset(local.path_to_helm, "**/*")

  bucket = aws_s3_bucket.chart_repository.bucket
  key    = "charts/${each.value}"
  source = "${local.path_to_helm}/${each.value}"
  acl    = "public-read"
}
