import sys
import pymysql

COMMON_PASSWORDS = ["", "root", "password", "123456", "12345678", "admin", "mysql"]
DB_NAME = "pulse_check_db"
HOSTS = ["localhost", "127.0.0.1"]
PORT = 3306
USER = "root"

def probe_and_setup():
    print("--- Watchdog Sentinel Auto-DB Setup ---")
    connection = None
    working_password = None
    working_host = None

    # Probe for working credentials
    for host in HOSTS:
        for pwd in COMMON_PASSWORDS:
            try:
                print(f"Probing MySQL connection: host='{host}', user='{USER}', password='{pwd}'...")
                connection = pymysql.connect(
                    host=host,
                    port=PORT,
                    user=USER,
                    password=pwd,
                    connect_timeout=1
                )
                working_password = pwd
                working_host = host
                print(f"Successfully connected to MySQL on {host}!")
                break
            except Exception:
                continue
        if connection:
            break

    if not connection:
        print("\n[WARNING] Could not connect to MySQL using standard development credentials.")
        print("We will generate a default .env template file with default values.")
        print("Please edit the .env file in the root folder with your correct MySQL credentials if they differ.")
        write_env(DB_NAME, USER, "your_password", "127.0.0.1", PORT)
        return

    # Create Database if not exists
    try:
        with connection.cursor() as cursor:
            cursor.execute(f"CREATE DATABASE IF NOT EXISTS {DB_NAME} CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;")
        print(f"Database '{DB_NAME}' created or verified successfully!")
    except Exception as e:
        print(f"[ERROR] Failed to create database '{DB_NAME}': {e}")
        # Write .env anyway so we have it
        write_env(DB_NAME, USER, working_password, working_host, PORT)
        return
    finally:
        connection.close()

    # Write .env file
    write_env(DB_NAME, USER, working_password, working_host, PORT)
    print("\n[SUCCESS] Setup complete! .env file created successfully.")

def write_env(db_name, db_user, db_pass, db_host, db_port):
    env_content = f"""# Watchdog Sentinel Environment Configuration
DEBUG=True
SECRET_KEY=django-insecure-critmon-sentinel-pulsecheckapi-watchdog-key-2026

# MySQL Database
DB_NAME={db_name}
DB_USER={db_user}
DB_PASSWORD={db_pass}
DB_HOST={db_host}
DB_PORT={db_port}

# Redis Server
REDIS_URL=redis://127.0.0.1:6379/0
"""
    with open(".env", "w") as f:
        f.write(env_content)
    print("Written config variables to .env")

if __name__ == "__main__":
    probe_and_setup()
