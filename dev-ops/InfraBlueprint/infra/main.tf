module "networking" {
  source   = "./modules/networking"
  vpc_cidr = var.vpc_cidr
}

module "storage" {
  source         = "./modules/storage"
  s3_bucket_name = var.s3_bucket_name
}

module "compute" {
  source           = "./modules/compute"
  vpc_id           = module.networking.vpc_id
  public_subnet_id = module.networking.public_subnet_1_id
  allowed_ssh_cidr = var.allowed_ssh_cidr
  s3_bucket_arn    = module.storage.bucket_arn
}

module "database" {
  source             = "./modules/database"
  vpc_id             = module.networking.vpc_id
  private_subnet_ids = [module.networking.private_subnet_1_id, module.networking.private_subnet_2_id]
  web_sg_id          = module.compute.web_sg_id
  db_username        = var.db_username
  db_password        = var.db_password
}