variable "gitlab_docker_registry_username" {
  type      = string
  sensitive = true
}

variable "gitlab_docker_registry_password" {
  type      = string
  sensitive = true
}

variable "taxi_playground_docker_image_id" {
  type      = string
}


module "platform" {
  source                          = "../../"
  environment                     = "prod"
  gitlab_docker_registry_username = var.gitlab_docker_registry_username
  gitlab_docker_registry_password = var.gitlab_docker_registry_password
   taxi_playground_docker_image_id = var.taxi_playground_docker_image_id
}
