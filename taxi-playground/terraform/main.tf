locals {
   system_name = "voyager"

   any_ip       = "0.0.0.0/0"
   ips_for_http = [local.any_ip] # TODO Remove any_ip

   subnet_ids = [aws_subnet.subnet_1.id, aws_subnet.subnet_2.id]

   domain_name             = "voyager.vyne.co"
   environment_domain_name = var.environment == "prod" ? local.domain_name : "${var.environment}.${local.domain_name}"
}

resource "aws_acm_certificate" "cert" {
   domain_name               = local.environment_domain_name
   subject_alternative_names = ["*.${local.environment_domain_name}"]
   validation_method         = "DNS"

   tags = {
      Name : local.environment_domain_name
   }

   lifecycle {
      create_before_destroy = true
   }
}

data "aws_route53_zone" "primary" {
   name = local.domain_name
}

resource "aws_route53_record" "cert_validation" {
   for_each = {
   for dvo in aws_acm_certificate.cert.domain_validation_options : dvo.domain_name => {
      name   = dvo.resource_record_name
      record = dvo.resource_record_value
      type   = dvo.resource_record_type
   }
   }

   allow_overwrite = true
   name            = each.value.name
   records         = [each.value.record]
   ttl             = 60
   type            = each.value.type
   zone_id         = data.aws_route53_zone.primary.zone_id
}

resource "aws_acm_certificate_validation" "validation" {
   certificate_arn         = aws_acm_certificate.cert.arn
   validation_record_fqdns = [for record in aws_route53_record.cert_validation : record.fqdn]
}

module "app" {
   source                                  = "./modules/app"
   region                                  = var.region
   vpc_id                                  = aws_vpc.main.id
   cert_arn                                = aws_acm_certificate.cert.arn
   environment                             = var.environment
   system_name                             = local.system_name
   subnets                                 = local.subnet_ids
   external_connectivity_security_group_id = aws_security_group.main.id
   subnet_1_id                             = aws_subnet.subnet_1.id
   subnet_1_arn                            = aws_subnet.subnet_1.arn
   subnet_2_id                             = aws_subnet.subnet_2.id
   gitlab_docker_registry_username         = var.gitlab_docker_registry_username
   gitlab_docker_registry_password         = var.gitlab_docker_registry_password
   route53_zone_id                         = data.aws_route53_zone.primary.zone_id
   domain_name                             = local.environment_domain_name
   taxi_playground_docker_image_id         = var.taxi_playground_docker_image_id
   database_host                           = aws_db_instance.voyager-db.address
   database_username                       = aws_db_instance.voyager-db.username
   database_password                       = aws_db_instance.voyager-db.password

   depends_on = [
      aws_internet_gateway.main
   ]
}

resource "random_password" "database" {
   length  = 24
   special = false
}


resource "random_string" "database_snapshot_suffix" {
   length  = 8
   special = false
}
resource "aws_db_subnet_group" "voyager-platform" {
   name       = "voyager-${var.environment}"
   subnet_ids = local.subnet_ids
   tags       = {
      Name = "Voyager DB subnet group"
   }
}

resource "aws_security_group" "database" {
   name   = "database-${var.environment}"
   vpc_id = aws_vpc.main.id

   ingress {
      from_port   = 5432
      to_port     = 5432
      protocol    = "tcp"
      cidr_blocks = ["10.0.0.0/16"]
   }

   egress {
      from_port   = 0
      to_port     = 0
      protocol    = "-1"
      cidr_blocks = [
         "0.0.0.0/0"
      ]
   }
}

resource "aws_db_instance" "voyager-db" {
   allocated_storage         = 10
   engine                    = "postgres"
   engine_version            = "13.4"
   instance_class            = "db.t3.micro"
   db_name                   = "voyager"
   username                  = "voyager"
   password                  = random_password.database.result
   final_snapshot_identifier = "voyager-pre-delete-snapshot-${var.environment}-${random_string.database_snapshot_suffix.result}"
   db_subnet_group_name      = aws_db_subnet_group.voyager-platform.name
   backup_retention_period   = 7
   multi_az                  = false
   storage_encrypted         = false
   vpc_security_group_ids    = [
      aws_security_group.database.id
   ]
   tags = {
      Name = "voyager-${var.environment}"
   }
}
