terraform {
   required_providers {
      aws = {
         source = "hashicorp/aws"
         version = "~> 3.27"
      }
   }
}

provider "aws" {
   profile = "default"
   region = var.region
}


resource "random_id" "hash" {
   byte_length = 8
}

# Create a VPC to launch our instances into
resource "aws_vpc" "benchmark_vpc" {
   cidr_block = "10.0.0.0/16"

   tags = {
      Name = "Vyne-Benchmark-VPC-${random_id.hash.hex}"
   }
}

# Create an internet gateway to give our subnet access to the outside world
resource "aws_internet_gateway" "vyne" {
   vpc_id = aws_vpc.benchmark_vpc.id
}

# Grant the VPC internet access on its main route table
resource "aws_route" "internet_access" {
   route_table_id = aws_vpc.benchmark_vpc.main_route_table_id
   destination_cidr_block = "0.0.0.0/0"
   gateway_id = aws_internet_gateway.vyne.id
}

# Create a subnet to launch our instances into
resource "aws_subnet" "benchmark_subnet" {
   vpc_id = aws_vpc.benchmark_vpc.id
   cidr_block = "10.0.2.0/24"
   map_public_ip_on_launch = true
   availability_zone = var.az
}

resource "aws_subnet" "benchmark_subnet_2" {
   vpc_id = aws_vpc.benchmark_vpc.id
   cidr_block = "10.0.3.0/24"
   map_public_ip_on_launch = true
   availability_zone = var.az2
}

resource "aws_db_subnet_group" "db-subnet" {
   name = "vyne-db-subnet-group-${random_id.hash.hex}"
   subnet_ids = [
      aws_subnet.benchmark_subnet.id,
      aws_subnet.benchmark_subnet_2.id]
}


resource "aws_security_group" "benchmark_security_group" {
   name = "terraform-vyne-${random_id.hash.hex}"
   vpc_id = aws_vpc.benchmark_vpc.id

   # SSH access from anywhere
   ingress {
      from_port = 22
      to_port = 22
      protocol = "tcp"
      cidr_blocks = [
         "0.0.0.0/0"]
   }

   # access postgres from anywhere
   ingress {
      from_port = 5432
      to_port = 5432
      protocol = "tcp"
      cidr_blocks = [
         "0.0.0.0/0"]
   }

   # access vyne ui from anywhere
   ingress {
      from_port = 9022
      to_port = 9022
      protocol = "tcp"
      cidr_blocks = [
         "0.0.0.0/0"]
   }

   # Eureka
   ingress {
      from_port = 8761
      to_port = 8761
      protocol = "tcp"
      cidr_blocks = [
         "0.0.0.0/0"]
   }

   # File Schema Servers
   ingress {
      from_port = 9301
      to_port = 9301
      protocol = "tcp"
      cidr_blocks = [
         "0.0.0.0/0"]
   }

   # Cask
   ingress {
      from_port = 8800
      to_port = 8800
      protocol = "tcp"
      cidr_blocks = [
         "0.0.0.0/0"]
   }

   # prometheus
   ingress {
      from_port = 9090
      to_port = 9090
      protocol = "tcp"
      cidr_blocks = [
         "0.0.0.0/0"]
   }

   # orhestrator
   ingress {
      from_port = 9600
      to_port = 9600
      protocol = "tcp"
      cidr_blocks = [
         "0.0.0.0/0"]
   }

   # pipeline runner
   ingress {
      from_port = 9610
      to_port = 9610
      protocol = "tcp"
      cidr_blocks = [
         "0.0.0.0/0"]
   }


   # grafana
   ingress {
      from_port = 3000
      to_port = 3000
      protocol = "tcp"
      cidr_blocks = [
         "0.0.0.0/0"]
   }

   # kibana
   ingress {
      from_port = 5601
      to_port = 5601
      protocol = "tcp"
      cidr_blocks = [
         "0.0.0.0/0"]
   }

   # elastic
   ingress {
      from_port = 9200
      to_port = 9200
      protocol = "tcp"
      cidr_blocks = [
         "0.0.0.0/0"]
   }

   ingress {
      from_port = 9300
      to_port = 9300
      protocol = "tcp"
      cidr_blocks = [
         "0.0.0.0/0"]
   }

   # logstash
   ingress {
      from_port = 5044
      to_port = 5044
      protocol = "tcp"
      cidr_blocks = [
         "0.0.0.0/0"]
   }

   # summary page
   ingress {
      from_port = 8080
      to_port = 8080
      protocol = "tcp"
      cidr_blocks = [
         "0.0.0.0/0"]
   }

   # All ports open within the VPC
   ingress {
      from_port = 0
      to_port = 65535
      protocol = "tcp"
      cidr_blocks = [
         "10.0.0.0/16"]
   }

   # outbound internet access
   egress {
      from_port = 0
      to_port = 0
      protocol = "-1"
      cidr_blocks = [
         "0.0.0.0/0"]
   }

   tags = {
      Name = "Benchmark-Security-Group-${random_id.hash.hex}"
   }
}

resource "aws_key_pair" "auth" {
   key_name = "${var.key_name}-${random_id.hash.hex}"
   public_key = file(var.public_key_path)
}



/* prometheus grafana*/
resource "aws_instance" "prometheus" {
   key_name = aws_key_pair.auth.id
   ami = var.ubuntu_ami_id
   instance_type = var.medium_instance_type
   tags = {
      Name = "prometheus-grafana-${var.vyne_version}"
   }
   subnet_id = aws_subnet.benchmark_subnet.id
   vpc_security_group_ids = [
      aws_security_group.benchmark_security_group.id]


   connection {
      host = self.public_ip
      user = "ubuntu"
      private_key = file(var.private_key_path)
   }

   provisioner "file" {
      source = "${path.module}/monitoring/docker-compose.yml"
      destination = "docker-compose.yml"
   }

   provisioner "file" {
      content = templatefile("${path.module}/monitoring/prometheus/env/prometheus.yml", {
         vyne-ip = "127.0.0.1"
      })
      destination = "/tmp/prometheus.yml"
   }

   provisioner "file" {
      content = "${path.module}/monitoring/grafana/config.ini"
      destination = "/tmp/config.ini"
   }

   provisioner "file" {
      source = "${path.module}/monitoring/grafana/dashboards"
      destination = "/tmp"
   }

   provisioner "file" {
      source = "${path.module}/monitoring/grafana/provisioning"
      destination = "/tmp"
   }

   provisioner "remote-exec" {
      // install docker and docker compose.
      inline = [
         "sudo apt-get update",
         "sudo DEBIAN_FRONTEND=noninteractive apt-get upgrade -y -o DPkg::options::=\"--force-confdef\" -o DPkg::options::=\"--force-confold\"",
         "sudo apt-get install -y docker.io docker-compose",
         "sudo usermod -aG docker $USER",
         "sudo docker-compose up -d"
      ]
   }
}


/*Elk */
resource "aws_instance" "elk" {
   key_name = aws_key_pair.auth.id
   ami = var.ubuntu_ami_id
   instance_type = var.medium_instance_type
   tags = {
      Name = "elastic-${var.vyne_version}"
   }
   subnet_id = aws_subnet.benchmark_subnet.id
   vpc_security_group_ids = [
      aws_security_group.benchmark_security_group.id]


   connection {
      host = self.public_ip
      user = "ubuntu"
      private_key = file(var.private_key_path)
   }

   provisioner "file" {
      source = "${path.module}/elk/docker-compose.yml"
      destination = "docker-compose.yml"
   }

   provisioner "file" {
      source = "${path.module}/elk/elasticsearch"
      destination = "/tmp"
   }

   provisioner "file" {
      source = "${path.module}/elk/kibana"
      destination = "/tmp"
   }

   provisioner "file" {
      source = "${path.module}/elk/logstash"
      destination = "/tmp"
   }

   provisioner "remote-exec" {
      // install docker and docker compose.
      inline = [
         "sudo apt-get update",
         "sudo DEBIAN_FRONTEND=noninteractive apt-get upgrade -y -o DPkg::options::=\"--force-confdef\" -o DPkg::options::=\"--force-confold\"",
         "sudo apt-get install -y docker.io docker-compose",
         "sudo usermod -aG docker $USER",
         "sudo docker-compose up -d"
      ]
   }
}




output "monitoring_public_ip" {
   depends_on = [
      aws_instance.prometheus]
   value = aws_instance.prometheus.public_ip
}


output "elastic_public_ip" {
   depends_on = [
      aws_instance.elk]
   value = aws_instance.elk.public_ip
}

output "grafana_url" {
   depends_on = [
      aws_instance.prometheus]
   value = "http://${aws_instance.prometheus.public_ip}:3000"
}

output "kibana_url" {
   depends_on = [
      aws_instance.elk]
   value = "http://${aws_instance.elk.public_ip}:5601"
}
