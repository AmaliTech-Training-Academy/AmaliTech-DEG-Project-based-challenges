import logging
from datetime import datetime
from typing import Dict, Any
import json
import os

class IdempotencyLogger:
    def __init__(self):
        # Get the project root directory
        self.project_root = os.getcwd()
        self.log_file = os.path.join(self.project_root, 'idempotency.log')
        
        # Create logger
        self.logger = logging.getLogger('idempotency-gateway')
        self.logger.setLevel(logging.INFO)
        
        # Remove old handlers to avoid duplicates
        self.logger.handlers.clear()
        
        # Create file handler - this will CREATE the file
        file_handler = logging.FileHandler(self.log_file, mode='a')
        file_handler.setLevel(logging.INFO)
        
        # Create console handler
        console_handler = logging.StreamHandler()
        console_handler.setLevel(logging.INFO)
        
        # Create formatter
        formatter = logging.Formatter('%(asctime)s - %(name)s - %(levelname)s - %(message)s')
        file_handler.setFormatter(formatter)
        console_handler.setFormatter(formatter)
        
        # Add handlers
        self.logger.addHandler(file_handler)
        self.logger.addHandler(console_handler)
        
        # Test if file was created
        if os.path.exists(self.log_file):
            self.logger.info(f"Log file ready: {self.log_file}")
        else:
            # Try to create file manually
            try:
                with open(self.log_file, 'w') as f:
                    f.write("")  # Create empty file
                self.logger.info(f"Log file created: {self.log_file}")
            except Exception as e:
                print(f"Could not create log file: {e}")
    
    def log_request(self, idempotency_key: str, request_body: dict, cache_status: str):
        """Log each request with its cache status"""
        log_entry = {
            "event": "request_received",
            "idempotency_key": idempotency_key,
            "request_body": request_body,
            "cache_status": cache_status,
            "timestamp": datetime.now().isoformat()
        }
        self.logger.info(json.dumps(log_entry))
    
    def log_cache_hit(self, idempotency_key: str):
        """Log cache hits for monitoring"""
        log_entry = {
            "event": "cache_hit",
            "idempotency_key": idempotency_key,
            "timestamp": datetime.now().isoformat()
        }
        self.logger.info(json.dumps(log_entry))
    
    def log_cache_miss(self, idempotency_key: str):
        """Log cache misses (actual processing)"""
        log_entry = {
            "event": "cache_miss",
            "idempotency_key": idempotency_key,
            "timestamp": datetime.now().isoformat()
        }
        self.logger.info(json.dumps(log_entry))
    
    def log_conflict(self, idempotency_key: str, old_body: dict, new_body: dict):
        """Log conflicts (same key, different body)"""
        log_entry = {
            "event": "conflict_detected",
            "idempotency_key": idempotency_key,
            "original_request": old_body,
            "conflicting_request": new_body,
            "timestamp": datetime.now().isoformat()
        }
        self.logger.warning(json.dumps(log_entry))
    
    def get_stats(self):
        """Calculate statistics from log file"""
        stats = {
            "total_requests": 0,
            "cache_hits": 0,
            "cache_misses": 0,
            "conflicts": 0
        }
        
        try:
            if os.path.exists(self.log_file):
                with open(self.log_file, 'r') as f:
                    for line in f:
                        try:
                            # Parse the log line
                            if ' - ' in line:
                                # Extract JSON part (after the last ' - ')
                                parts = line.split(' - ')
                                # The JSON is the last part
                                json_part = parts[-1]
                                log_entry = json.loads(json_part)
                            else:
                                log_entry = json.loads(line)
                            
                            event = log_entry.get("event")
                            if event == "request_received":
                                stats["total_requests"] += 1
                            elif event == "cache_hit":
                                stats["cache_hits"] += 1
                            elif event == "cache_miss":
                                stats["cache_misses"] += 1
                            elif event == "conflict_detected":
                                stats["conflicts"] += 1
                        except:
                            pass
        except Exception as e:
            print(f"Error reading log: {e}")
        
        return stats

# Create global logger instance
logger = IdempotencyLogger()