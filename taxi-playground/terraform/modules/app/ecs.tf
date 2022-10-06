resource "aws_ecs_cluster" "cluster" {
  name = var.system_name
}

resource "aws_security_group" "ecs_service" {
  vpc_id      = var.vpc_id
  name_prefix = var.system_name
  description = "Fargate service security group for ${var.environment}"

  revoke_rules_on_delete = true

  ingress {
    protocol        = "-1"
    from_port       = 0
    to_port         = 0
    security_groups = [var.external_connectivity_security_group_id, aws_security_group.alb.id]
  }

  egress {
    protocol         = "-1"
    from_port        = 0
    to_port          = 0
    cidr_blocks      = ["0.0.0.0/0"]
    ipv6_cidr_blocks = ["::/0"]
  }

  lifecycle {
    create_before_destroy = true
  }
}

module "voyager" {
  source                    = "./modules/service"
  service_name              = "voyager"
  environment               = var.environment
  region                    = var.region
  vpc_id                    = var.vpc_id
  subnets                   = var.subnets
  image                     = "registry.gitlab.com/vyne/vyne/taxi-playground:${var.taxi_playground_docker_image_id}"
  task_definition_cpu       = 2048
  task_definition_memory    = 4096
  port                      = 9500
  protocol                  = "HTTP"
  execution_role_arn        = aws_iam_role.execution.arn
  task_role_arn             = aws_iam_role.task.arn
  cluster_id                = aws_ecs_cluster.cluster.id
  cloudwatch_log_group_name = aws_cloudwatch_log_group.main.name
  security_groups           = [aws_security_group.ecs_service.id, var.external_connectivity_security_group_id]
  repository_credentials_secret_arn = aws_secretsmanager_secret.private_registry_access.arn
#  load_balancer_listeners   = [
#    { protocol = "HTTP", port = 9500, load_balancer_arn = aws_alb.main.arn }
#  ]
  health_check = {
    path = "/actuator/health"
    port = 9500
  }
  environment_variables = {
    # TODO Add any environment variables and/or delete the below
    spring_profiles_active = var.environment
  }
}
