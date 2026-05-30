import sys
import json
import time
import threading
from django.core.management.base import BaseCommand
from django.utils import timezone
from django.conf import settings
from sentinel.models import Monitor, AlertLog
import redis

class Command(BaseCommand):
    help = "Runs the Watchdog Sentinel daemon listening to Redis key expirations and running dual-safety polling"

    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.redis_client = None
        self.pubsub = None
        self.stop_event = threading.Event()
        self.polling_thread = None

    def handle(self, *args, **options):
        self.stdout.write(self.style.SUCCESS("=== Watchdog Sentinel Daemon Starting ==="))
        
        # Connect to Redis
        try:
            self.redis_client = redis.Redis.from_url(settings.REDIS_URL, decode_responses=True)
            self.redis_client.ping()
            self.stdout.write(self.style.SUCCESS("[REDIS] Connected successfully!"))
        except Exception as e:
            self.stdout.write(self.style.ERROR(f"[REDIS ERROR] Failed to connect: {e}"))
            self.stdout.write(self.style.WARNING("[SYSTEM] Running in dual-safety polling ONLY mode (Redis disabled)."))

        # Configure Redis Keyspace Notifications
        if self.redis_client:
            try:
                self.redis_client.config_set("notify-keyspace-events", "Ex")
                self.stdout.write(self.style.SUCCESS("[REDIS] Enabled keyspace notifications ('Ex' - Expired events)"))
            except Exception as e:
                self.stdout.write(self.style.WARNING(f"[REDIS WARNING] Could not configure CONFIG SET notify-keyspace-events. Ensure notifications are enabled in redis.conf: {e}"))

        # Start the Dual-Safety & Self-Healing polling thread
        self.polling_thread = threading.Thread(target=self.run_dual_safety_polling, daemon=True)
        self.polling_thread.start()
        self.stdout.write(self.style.SUCCESS("[SENTINEL] Dual-Safety self-healing polling loop active (runs every 5s)"))

        # If Redis is active, enter Pub/Sub listening block
        if self.redis_client:
            try:
                self.pubsub = self.redis_client.pubsub()
                # Subscribe to expired keyspace events on DB 0
                self.pubsub.subscribe("__keyevent@0__:expired")
                self.stdout.write(self.style.SUCCESS("[SENTINEL] Subscribed to Redis keyspace expired notifications..."))
                
                # Listen to events blockingly in the main thread
                for message in self.pubsub.listen():
                    if self.stop_event.is_set():
                        break
                    
                    if message['type'] == 'message':
                        key = message['data']
                        self.process_expired_key(key)
                        
            except KeyboardInterrupt:
                self.stdout.write(self.style.WARNING("\n[SYSTEM] Interrupted by user. Shutting down..."))
            except Exception as e:
                self.stdout.write(self.style.ERROR(f"[SYSTEM ERROR] Redis pubsub crashed: {e}"))
            finally:
                self.shutdown()
        else:
            # If Redis is not available, block the main thread using sleep
            try:
                while not self.stop_event.is_set():
                    time.sleep(1)
            except KeyboardInterrupt:
                self.stdout.write(self.style.WARNING("\n[SYSTEM] Interrupted by user. Shutting down..."))
            finally:
                self.shutdown()

    def process_expired_key(self, key):
        # Format: sentinel:monitor:{id}:timer
        if key.startswith("sentinel:monitor:") and key.endswith(":timer"):
            monitor_id = key.split(":")[2]
            self.stdout.write(self.style.NOTICE(f"[EVENT] Redis timer expired for device '{monitor_id}'"))
            self.trigger_device_alert(monitor_id, reason="Redis TTL Expired")

    def trigger_device_alert(self, monitor_id, reason):
        try:
            # Query the database
            monitor = Monitor.objects.filter(id=monitor_id).first()
            if not monitor:
                return

            if monitor.status == 'active':
                # Transition monitor to down state
                monitor.status = 'down'
                monitor.save(update_fields=['status']) # Saves status changes

                # Output requested console alert
                timestamp = timezone.now().isoformat()
                alert_payload = {
                    "ALERT": f"Device {monitor.id} is down!",
                    "time": timestamp,
                    "reason": reason
                }
                # Print alert payload to console in bright red/bold style or standard JSON
                sys.stdout.write(json.dumps(alert_payload) + "\n")
                sys.stdout.flush()

                # Log alert in database for dashboard feed
                AlertLog.objects.create(
                    monitor_id=monitor.id,
                    message=f"Device {monitor.id} went offline! (Reason: {reason})"
                )
        except Exception as e:
            self.stdout.write(self.style.ERROR(f"[ALERT TRIGGER ERROR] Failed to fire alert for {monitor_id}: {e}"))

    def run_dual_safety_polling(self):
        """Runs in background thread, polling the DB every 5s to catch missed expirations and self-heal Redis."""
        while not self.stop_event.is_set():
            try:
                now = timezone.now()
                # Query all active monitors
                active_monitors = list(Monitor.objects.filter(status='active'))

                for monitor in active_monitors:
                    # 1. EXPIRATION FALLBACK: If current time exceeds next_alert_at, trigger alert
                    if monitor.next_alert_at <= now:
                        self.stdout.write(self.style.WARNING(f"[FALLBACK] Missed heartbeat detected via DB polling for '{monitor.id}'"))
                        self.trigger_device_alert(monitor.id, reason="Dual-Safety Polling Timeout Fallback")
                    
                    # 2. SELF-HEALING REDIS SYNC: If monitor is active but Redis key is missing (e.g. Redis restart), restore it!
                    elif self.redis_client:
                        redis_key = f"sentinel:monitor:{monitor.id}:timer"
                        try:
                            # Check if key exists in Redis
                            if not self.redis_client.exists(redis_key):
                                # Calculate remaining TTL
                                remaining_seconds = int((monitor.next_alert_at - now).total_seconds())
                                if remaining_seconds > 0:
                                    self.redis_client.setex(redis_key, remaining_seconds, "active")
                                    self.stdout.write(self.style.SUCCESS(f"[HEALED] Restored missing Redis timer for active device '{monitor.id}' with {remaining_seconds}s TTL"))
                        except Exception as re:
                            # Ignore Redis-specific errors in this thread to avoid crashing polling
                            pass
            except Exception as e:
                # Log any database connection or model lookup issues
                pass
            
            # Sleep for 5 seconds
            time.sleep(5)

    def shutdown(self):
        self.stdout.write(self.style.WARNING("[SYSTEM] Cleaning up resources..."))
        self.stop_event.set()
        if self.pubsub:
            try:
                self.pubsub.close()
            except Exception:
                pass
        self.stdout.write(self.style.SUCCESS("=== Watchdog Sentinel Daemon Stopped ==="))
