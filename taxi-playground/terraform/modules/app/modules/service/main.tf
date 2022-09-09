
locals {
  task_environment = [
    for k, v in var.environment_variables : {
      name  = k
      value = v
    }
  ]
}

resource "aws_lb_target_group" "task" {
  name        = "${var.service_name}-${var.environment}"
  vpc_id      = var.vpc_id
  protocol    = var.protocol
  port        = var.port
  target_type = "ip"


  dynamic "health_check" {
    for_each = [var.health_check]
    content {
      enabled  = lookup(health_check.value, "enabled", null)
      path     = lookup(health_check.value, "path", null)
      port     = lookup(health_check.value, "port", null)
      protocol = lookup(health_check.value, "protocol", null)
      matcher  = lookup(health_check.value, "matcher", null)
    }
  }

  lifecycle {
    create_before_destroy = true
  }
}


resource "aws_ecs_task_definition" "task" {
  family                   = var.service_name
  execution_role_arn       = var.execution_role_arn
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = var.task_definition_cpu
  memory                   = var.task_definition_memory
  task_role_arn            = var.task_role_arn
  container_definitions    = <<EOF
[{
  "name": "${var.service_name}",
  "image": "${var.image}",
  "essential": true,
  "portMappings": [
    {
      "containerPort": ${var.port},
      "protocol":"tcp"
    }
  ],
  "logConfiguration": {
    "logDriver": "awslogs",
    "options": {
      "awslogs-group": "${var.cloudwatch_log_group_name}",
      "awslogs-region": "${var.region}",
      "awslogs-stream-prefix": "container"
    }
  },
  "memory": ${var.task_definition_memory},
  "cpu": ${var.task_definition_cpu},
  "environment": ${jsonencode(local.task_environment)},
  %{if var.repository_credentials_secret_arn != null~}
  "repositoryCredentials": {
    "credentialsParameter": "${var.repository_credentials_secret_arn}"
  },
  %{~endif}
  %{if var.mount_points != null~}
  "mountPoints": ${jsonencode(var.mount_points)},
  %{~endif}
  "secrets": ${jsonencode(var.secrets)}
}]
EOF

  dynamic "volume" {
    for_each = var.volumes
    content {
      name = volume.value.name
      efs_volume_configuration {
        file_system_id = volume.value.file_system_id
        root_directory = lookup(volume.value, "root_directory", null)
      }
    }
  }
}

data "aws_ecs_task_definition" "task" {
  task_definition = aws_ecs_task_definition.task.family
}

resource "aws_ecs_service" "service" {
  name = var.service_name

  cluster         = var.cluster_id
  task_definition = "${aws_ecs_task_definition.task.family}:${max(aws_ecs_task_definition.task.revision, data.aws_ecs_task_definition.task.revision)}"

  desired_count  = 1
  propagate_tags = "TASK_DEFINITION"

  platform_version = "1.4.0"
  launch_type      = "FARGATE"

  force_new_deployment   = true
  wait_for_steady_state  = true
  enable_execute_command = true

  network_configuration {
    subnets          = var.subnets
    security_groups  = concat(var.security_groups, var.efs_security_group != null ? [var.efs_security_group] : [])
    assign_public_ip = true
  }
  load_balancer {
    container_name   = var.service_name
    container_port   = var.port
    target_group_arn = aws_lb_target_group.task.arn
  }
}

resource "aws_alb_listener" "service" {
  for_each          = { for listener in var.load_balancer_listeners : listener.port => listener }
  load_balancer_arn = each.value.load_balancer_arn
  port              = each.value.port
  protocol          = each.value.protocol
  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.task.arn
  }
}
