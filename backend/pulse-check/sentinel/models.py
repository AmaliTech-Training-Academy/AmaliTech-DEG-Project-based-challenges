from django.db import models
from django.utils import timezone
from datetime import timedelta

class Monitor(models.Model):
    STATUS_CHOICES = [
        ('active', 'Active'),
        ('paused', 'Paused'),
        ('down', 'Down'),
    ]

    id = models.CharField(max_length=100, primary_key=True, help_text="Unique device/monitor identifier")
    timeout = models.PositiveIntegerField(default=60, help_text="Timeout countdown in seconds")
    alert_email = models.EmailField(help_text="Email to notify when the watchdog triggers")
    status = models.CharField(
        max_length=20,
        choices=STATUS_CHOICES,
        default='active',
        help_text="Current state of the watchdog monitor"
    )
    last_heartbeat = models.DateTimeField(default=timezone.now, help_text="Timestamp of the last received ping")
    next_alert_at = models.DateTimeField(help_text="Calculated timestamp when the monitor will expire")
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    def save(self, *args, **kwargs):
        # Always update the expiration timestamp when the monitor is saved.
        self.next_alert_at = self.last_heartbeat + timedelta(seconds=self.timeout)
        super().save(*args, **kwargs)

    def __str__(self):
        return f"{self.id} ({self.status.upper()} - {self.timeout}s)"

class AlertLog(models.Model):
    monitor_id = models.CharField(max_length=100)
    alert_time = models.DateTimeField(default=timezone.now)
    message = models.TextField()

    class Meta:
        ordering = ['-alert_time']

    def __str__(self):
        return f"ALERT [{self.monitor_id}] at {self.alert_time}"
