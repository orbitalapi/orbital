variable "system_name" {
  type        = string
  description = "System name."
}

variable "environment" {
  type        = string
  description = "Name of the environment that is used as part of the resource names and domains. "
}

variable "region" {
  type        = string
  description = "The AWS region like eu-west-1."
}

variable "external_connectivity_security_group_id" {
  type        = string
  description = "Security group id to be used for the load balancer."
}

variable "vpc_id" {
  type        = string
  description = "The VPC id."
}

variable "subnets" {
  type        = set(string)
  description = "The VPC subnets."
}

variable "subnet_1_id" {
  type        = string
  description = "A subnet's id. Needs to match the subnet_1_arn."
}

variable "subnet_1_arn" {
  type        = string
  description = "A subnet's ARN. Needs to match the subnet_1_id."
}

variable "subnet_2_id" {
  type        = string
  description = "The second subnet's id."
}

variable "cert_arn" {
  type        = string
  description = "The load balancer certificate ARN."
}

variable "gitlab_docker_registry_username" {
  type        = string
  sensitive   = true
  description = "The username for the Gitlab Docker registry that is needed for the images."
}

variable "gitlab_docker_registry_password" {
  type        = string
  sensitive   = true
  description = "The password for the Gitlab Docker registry matching the given username."
}

variable "route53_zone_id" {
  type        = string
  description = "The Route53 zone id."
}

variable "domain_name" {
  type        = string
  description = "The sub domain name being used for this environment"
}

variable "taxi_playground_docker_image_id" {
  type        = string
  description = "The specific image ID which should be deployed"
}


