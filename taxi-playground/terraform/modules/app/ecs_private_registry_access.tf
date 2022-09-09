resource "aws_secretsmanager_secret" "private_registry_access" {
  name = "gitlab-docker-registry-access-${var.environment}"
}

resource "aws_secretsmanager_secret_version" "private_registry_access" {
  secret_id     = aws_secretsmanager_secret.private_registry_access.id
  secret_string = <<EOF
{
  "username" : "${var.gitlab_docker_registry_username}",
  "password" : "${var.gitlab_docker_registry_password}"
}
EOF
}

data "aws_iam_policy_document" "private_registry_access" {
  statement {
    effect = "Allow"

    resources = [
      aws_secretsmanager_secret.private_registry_access.arn,
    ]

    actions = [
      "ssm:GetParameters",
      "secretsmanager:GetSecretValue"
    ]
  }
}

resource "aws_iam_role_policy" "read_repository_credentials" {
  name   = "gitlab-docker-access-${var.environment}"
  role   = aws_iam_role.execution.id
  policy = data.aws_iam_policy_document.private_registry_access.json
}
