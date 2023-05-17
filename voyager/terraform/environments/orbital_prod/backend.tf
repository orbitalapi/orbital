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
         version = "~> 3.4.0"
      }
      null = {
         source  = "hashicorp/null"
         version = "3.2.1"
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
      role_arn = "arn:aws:iam::801563263500:role/TerraformAccessRole"
   }
}
