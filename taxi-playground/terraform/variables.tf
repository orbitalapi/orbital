variable "region" {
  type    = string
  default = "eu-west-2"
}

variable "az" {
  type    = string
  default = "eu-west-2a"
}

variable "az2" {
  type    = string
  default = "eu-west-2b"
}

variable "environment" {
  type        = string
  description = "Name of the environment that is used as part of the resource names and domains. "
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

variable "taxi_playground_docker_image_id" {
  type        = string
  description = "The specific image ID which should be deployed"
}
