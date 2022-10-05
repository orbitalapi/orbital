terraform {
  backend "http" {
  }
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 4.0"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.3.0"
    }
    null = {
      source  = "hashicorp/null"
      version = "3.1.1"
    }
  }
}

provider "aws" {
  region = "eu-west-2"
  default_tags {
    tags = {
      Name        = "prod"
      Environment = "prod"
    }
  }

  assume_role {
    role_arn     = "arn:aws:iam::647692306194:role/TerraformAccessRole"
  }
}
