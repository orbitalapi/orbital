resource "aws_security_group" "alb" {
  name   = "alb-${var.environment}"
  vpc_id = var.vpc_id

  ingress {
    protocol    = "-1"
    from_port   = 0
    to_port     = 0
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_alb" "main" {
  name            = "alb-${var.environment}"
  internal        = false
  security_groups = [var.external_connectivity_security_group_id, aws_security_group.alb.id]
  subnets         = var.subnets
}

# Enables HTTPS
resource "aws_lb_listener" "redirect_http_to_https" {
  load_balancer_arn = aws_alb.main.arn
  port              = 80
  protocol          = "HTTP"

  default_action {
    type = "redirect"

    redirect {
      port        = "443"
      protocol    = "HTTPS"
      status_code = "HTTP_301"
    }
  }
}

resource "aws_wafv2_web_acl" "external" {
  name        = "ExternalACL_${var.environment}"
  scope       = "REGIONAL"

  default_action {
    allow {}
  }

  rule {
    name     = "AWS-AWSManagedRulesCommonRuleSet"
    priority = 1

    override_action {
      count {}
    }

    statement {
      managed_rule_group_statement {
        name        = "AWSManagedRulesCommonRuleSet"
        vendor_name = "AWS"
      }
    }

    visibility_config {
      cloudwatch_metrics_enabled = true
      metric_name                = "alb_waf_${var.environment}"
      sampled_requests_enabled   = false
    }
  }

  rule {
    name     = "AWS-AWSManagedRulesBotControlRuleSet"
    priority = 2

    override_action {
      count {}
    }

    statement {
      managed_rule_group_statement {
        name        = "AWSManagedRulesBotControlRuleSet"
        vendor_name = "AWS"
      }
    }

    visibility_config {
      cloudwatch_metrics_enabled = true
      metric_name                = "alb_waf_${var.environment}"
      sampled_requests_enabled   = false
    }
  }

  visibility_config {
    cloudwatch_metrics_enabled = true
    metric_name                = "ExternalACL"
    sampled_requests_enabled   = false
  }
}

resource "aws_wafv2_web_acl_association" "acl-association" {
  resource_arn = aws_alb.main.arn
  web_acl_arn = aws_wafv2_web_acl.external.arn
}

resource "aws_cloudwatch_log_group" "wafv2-log-group" {
  name              = "aws-waf-logs-taxi-playground/${var.environment}"
  retention_in_days = 90
}

resource "aws_wafv2_web_acl_logging_configuration" "waf_logging_configuration" {
  log_destination_configs = [aws_cloudwatch_log_group.wafv2-log-group.arn]
  resource_arn            = aws_wafv2_web_acl.external.arn
  depends_on              = [aws_cloudwatch_log_group.wafv2-log-group]
}

