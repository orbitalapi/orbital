
resource "aws_cloudwatch_log_group" "main" {
  name              = var.system_name
  retention_in_days = 90
}

resource "aws_cloudwatch_log_metric_filter" "app_error_metric_filter" {
  name           = "app_error_metric_filter_${var.environment}"
  pattern        = "?ERROR ?Exception ?\"Failed to fetch data from the Ultumus API\""
  log_group_name = aws_cloudwatch_log_group.main.name

  metric_transformation {
    name      = "ErrorCount_${var.environment}"
    namespace = "ECS_gateway"
    value     = "1"
  }
}

data "aws_sns_topic" "chatbot_topic" {
  name = "taxi-playground-gateway-events"
}

resource "aws_cloudwatch_metric_alarm" "gateway_app_error_alarm" {
  alarm_name = "gateway_app_errors_${var.environment}"
  metric_name         = aws_cloudwatch_log_metric_filter.app_error_metric_filter.metric_transformation[0].name
  threshold           = "0"
  statistic           = "Sum"
  comparison_operator = "GreaterThanThreshold"
  datapoints_to_alarm = "1"
  evaluation_periods  = "1"
  period              = "60"
  namespace           = "ECS_gateway"
  alarm_actions       = [data.aws_sns_topic.chatbot_topic.arn]
}
