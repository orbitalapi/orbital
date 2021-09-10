# Pre-requisites

* [Install Terraform](https://learn.hashicorp.com/tutorials/terraform/install-cli?in=terraform/aws-get-started)
* [Install AWS CLI](https://learn.hashicorp.com/tutorials/terraform/aws-build)
* Make sure you already have a private / public key pair to SSH, if not create one.

# Purpose

This terraform project will create:

 * One EC2 host to run a dockerised kafka broker (lensesio/fast-data-dev:latest) and a kafka produce to push xml messages
 * One EC2 host run dockerised Eureka, Vyne Query Server and File Schema Server
 * an RDS Postgres Server
 * Once EC2 host per given number of Cask instances (by default 2, can be set via cask_count variable)

# How to Run

* Run 
```
terraform init
```
* Validate by running 
```
terraform validate
```
* Single vyne Server deployment - Create the infrastructure on AWS by running: (update public_key_path and private_key_path according to you SSH key setup) 
```
    terraform apply -var public_key_path="~/.ssh/id_rsa.pub" -var private_key_path="~/.ssh/id_rsa"
```

* Clustering deployment - Create the infrastructure on AWS by running: (update public_key_path and private_key_path according to you SSH key setup)

Set vyne_count to the desired cluster size
```
    terraform apply -var public_key_path="~/.ssh/id_rsa.pub" -var private_key_path="~/.ssh/id_rsa" -var vyne_compose_template="docker-compose-hazelcast.tpl"  -var vyne_count=3
```


* Run 
 ```
terraform destroy -auto-approve -var public_key_path="~/.ssh/id_rsa.pub" -var private_key_path="~/.ssh/id_rsa"
 ```
 to destroy the infrastructure
 
 ## Parameters
 
 * vyne_version
 
 The Vyne docker tag that we want to deploy.
 
 * large_instance_type
 
    ```
    default value: `t3.large`
    description: Ec2 instance type for the box running eureka, vyne and file schema server docker images
   ``` 
   
 * high_performance_instance_type

```
    default value: `i3.large`
    description: Ec2 instance type for the box running kafka and kafka publisher docker images
   ``` 


see [./variables.tf](./variables.tf) for other variables.
 
 
 
