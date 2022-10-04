variable "environment" {
  type        = string
  description = "Name of the environment that is used as part of the resource names and domains."
}

variable "region" {
  type        = string
  description = "The AWS region like eu-west-1."
}

variable "vpc_id" {
  type        = string
  description = "The VPC id."
}

variable "subnets" {
  type        = set(string)
  description = "The VPC subnets."
}

variable "service_name" {
  type        = string
  description = "The name of the service like \"schema-server\"."
}

variable "task_role_arn" {
  type        = string
  description = "The ARN of the task role."
}

variable "execution_role_arn" {
  type        = string
  description = "The ARN of the execution role."
}

variable "task_definition_cpu" {
  type        = number
  description = "The CPU specification for the task definition like 2048."
}

variable "task_definition_memory" {
  type        = number
  description = "The memory specification for the task definition like 2048."
}

variable "cloudwatch_log_group_name" {
  type        = string
  description = "The CloudWatch log group name."
}

variable "cluster_id" {
  type        = string
  description = "The ECS cluster id."
}

variable "security_groups" {
  type        = list(string)
  description = "The security groups to associate with the service."
}

variable "efs_security_group" {
  type        = string
  default     = null
  description = "The security group created for EFS access, if needed."
}

variable "image" {
  type        = string
  description = "The image with the tag to use."
}

variable "port" {
  type        = number
  description = "The port to run the service in."
}

variable "protocol" {
  type        = string
  description = "The protocol exposed by the service like \"HTTP\" or \"TCP\"."
  default     = "HTTP"
}

variable "health_check" {
  type        = object({ port = string, path = string })
  description = "A health block containing health check settings for the target group. Specify just a key \"enabled\" to false to disable health checks."
  default = {
    port = "traffic-port"
    path = "/api/actuator/health"
  }
}

variable "environment_variables" {
  type        = map(string)
  description = "The environment variables to pass to a container."
  default     = {}
}

variable "secrets" {
  type        = list(map(string))
  description = "The secrets to pass to the container."
  default     = []
}

variable "load_balancer_listeners" {
  type        = list(object({ port = string, protocol = string, load_balancer_arn = string }))
  default     = []
  description = "The load balancer listeners to register for the service. "
}

variable "volumes" {
  type        = list(object({ name = string, file_system_id = string }))
  description = "Configurations for volumes to use. This is a list of maps, where each map should contain \"name\", \"host_path\", and \"efs_volume_configuration\". Full set of options can be found at https://www.terraform.io/docs/providers/aws/r/ecs_task_definition.html"
  default     = []
}

variable "repository_credentials_secret_arn" {
  type        = string
  default     = null
  description = "The ARN of the Secrets Manager secret to be used for the repository credentials."
}

variable "mount_points" {
  description = "The mount points for data volumes in the container. Please note that the casing is camelCase due to this being the actual JSON that is passed to the task definition."
  type        = list(object({ sourceVolume = string, containerPath = string }))
  default     = null
}
