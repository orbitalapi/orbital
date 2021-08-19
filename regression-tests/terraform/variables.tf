variable "region" {
  default = "eu-west-2"
}

variable medium_instance_type {
  default = "t3.medium"
}
variable large_instance_type {
  default = "t3.large"
}

variable high_performance_instance_type {
  default = "i3.xlarge"
}

variable db_instance_class {
  // see https://aws.amazon.com/rds/instance-types/
  default = "db.m5.large"
}

// see https://cloud-images.ubuntu.com/locator/ec2/
variable ubuntu_ami_id {
  default = "ami-096cb92bb3580c759"
}

variable ami {
  description = "Custom AMI, if empty will use latest Ubuntu LTS"
  default     = ""
}

variable "az" {
  default = "eu-west-2a"
}

variable "az2" {
  default = "eu-west-2c"
}

variable "public_key_path" {
  description = <<DESCRIPTION
Path to the SSH public key to be used for authentication.
Ensure this keypair is added to your local SSH agent so provisioners can
connect.

Example: ~/.ssh/kafka_aws.pub
DESCRIPTION
}

variable "private_key_path" {
  description = "The private key material of the key pair associated with the instance. Example: ~/.ssh/kafka_aws"
  type        = string
}


variable "key_name" {
  default     = "vyne-benchmark-key"
  description = "Desired name prefix for the AWS key pair"
}

variable "kafka_topic" {
  default     = "otc-swap"
  description = "Kafka Topic Name"
}

variable "kafka_topic_partition_count" {
  default     = 40
  description = "Kafka Topic Partition Count"
  type = number
}

variable "kafka_consumer_count" {
  default     = 6
  description = "Number of Kafka consumers within each Cask"
  type = number
}

variable "message_count" {
  default     = 2000000
  description = "Numer of fpml messages to be published."
  type = number
}

variable "test_type_name" {
  default = "Swap"
  description = "Taxi type name of the test type."
}

variable "db_password" {
  default     = "vynedbtest"
  description = "DB Password"
}

variable "cask_count" {
  default     = 1
  description = "Number of cask instances"
  type = number
}

variable "vyne_count" {
   default = 1
   description = "Number of Vyne instances"
   type = number
}

variable "vyne_version" {
  default = "latest-snapshot"
  description = "Vyne System Docker Image Version"
}


