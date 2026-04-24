# Data tier: PostgreSQL in private subnets, reachable only from the web security group on 5432.

resource "aws_security_group" "db_sg" {
  name        = "vela-db-sg"
  description = "Allow Postgres inbound only from web tier"
  vpc_id      = aws_vpc.main.id

  ingress {
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.web_sg.id]
  }

  egress {
    description = "Outbound for maintenance and engine operations"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

}

resource "aws_db_subnet_group" "main" {
  name       = "vela-db-subnet"
  subnet_ids = [aws_subnet.private_1.id, aws_subnet.private_2.id]
}

resource "aws_db_instance" "main" {
  identifier                 = "vela-db"
  engine                     = "postgres"
  engine_version             = "15"
  instance_class             = "db.t3.micro"
  allocated_storage          = 20
  storage_type               = "gp3"
  storage_encrypted          = true
  db_name                    = "veladb"
  username                   = var.db_username
  password                   = var.db_password
  db_subnet_group_name       = aws_db_subnet_group.main.name
  vpc_security_group_ids     = [aws_security_group.db_sg.id]
  publicly_accessible        = false
  multi_az                   = false
  skip_final_snapshot        = true
  backup_retention_period    = 1
  auto_minor_version_upgrade = true
}
