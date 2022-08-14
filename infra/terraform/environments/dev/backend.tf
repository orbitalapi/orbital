terraform {
  backend "http" {
  }
}

provider "aws" {
  region = "eu-west-1"
  assume_role {
    role_arn = "arn:aws:iam::647692306194:role/terraform"
  }
  default_tags {
    tags = {
      Name        = "dev"
      Environment = "dev"
    }
  }
}
