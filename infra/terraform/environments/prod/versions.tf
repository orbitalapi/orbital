terraform {
  required_version = "=1.1.9" # Needs to match the one in GitLab CI..
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 4.0"
    }
  }
}
