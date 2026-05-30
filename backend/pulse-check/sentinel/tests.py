import json
from django.test import TestCase, Client
from django.urls import reverse
from django.utils import timezone
from datetime import timedelta
from sentinel.models import Monitor, AlertLog

class WatchdogSentinelAPITests(TestCase):
    def setUp(self):
        self.client = Client()
        self.device_id = "device-123"
        self.timeout = 60
        self.alert_email = "admin@critmon.com"
        
        # Paths
        self.register_url = reverse('sentinel:register')
        self.heartbeat_url = reverse('sentinel:heartbeat', args=[self.device_id])
        self.pause_url = reverse('sentinel:pause', args=[self.device_id])

    def test_register_new_monitor_success(self):
        """User Story 1: Registering a monitor returns 201 Created and saves monitor."""
        payload = {
            "id": self.device_id,
            "timeout": self.timeout,
            "alert_email": self.alert_email
        }
        response = self.client.post(
            self.register_url,
            data=json.dumps(payload),
            content_type='application/json'
        )
        
        self.assertEqual(response.status_code, 201)
        data = response.json()
        self.assertEqual(data["status"], "created")
        self.assertIn("watchdog active", data["message"])
        
        # Verify DB record
        monitor = Monitor.objects.get(id=self.device_id)
        self.assertEqual(monitor.timeout, self.timeout)
        self.assertEqual(monitor.alert_email, self.alert_email)
        self.assertEqual(monitor.status, 'active')
        # Check that next_alert_at is close to last_heartbeat + 60s
        self.assertAlmostEqual(
            monitor.next_alert_at,
            monitor.last_heartbeat + timedelta(seconds=self.timeout),
            delta=timedelta(seconds=1)
        )

    def test_register_missing_fields_fails(self):
        """Registering with missing payload returns 400 Bad Request."""
        payload = {
            "id": self.device_id,
            "timeout": 30
            # missing alert_email
        }
        response = self.client.post(
            self.register_url,
            data=json.dumps(payload),
            content_type='application/json'
        )
        self.assertEqual(response.status_code, 400)
        self.assertIn("error", response.json())

    def test_register_invalid_timeout_fails(self):
        """Registering with non-integer or non-positive timeout returns 400 Bad Request."""
        payloads = [
            {"id": self.device_id, "timeout": -10, "alert_email": self.alert_email},
            {"id": self.device_id, "timeout": "abc", "alert_email": self.alert_email}
        ]
        
        for payload in payloads:
            response = self.client.post(
                self.register_url,
                data=json.dumps(payload),
                content_type='application/json'
            )
            self.assertEqual(response.status_code, 400)
            self.assertIn("error", response.json())

    def test_heartbeat_existing_monitor_success(self):
        """User Story 2: Heartbeat resets countdown and returns 200 OK."""
        # Setup monitor
        monitor = Monitor.objects.create(
            id=self.device_id,
            timeout=self.timeout,
            alert_email=self.alert_email,
            status='active',
            last_heartbeat=timezone.now() - timedelta(seconds=10) # 10 seconds ago
        )
        old_next_alert = monitor.next_alert_at
        
        response = self.client.post(self.heartbeat_url)
        self.assertEqual(response.status_code, 200)
        
        data = response.json()
        self.assertEqual(data["status"], "ok")
        self.assertIn("reset", data["message"])
        
        # Verify DB record
        monitor.refresh_from_db()
        self.assertEqual(monitor.status, 'active')
        self.assertTrue(monitor.last_heartbeat > timezone.now() - timedelta(seconds=2))
        self.assertTrue(monitor.next_alert_at > old_next_alert)

    def test_heartbeat_not_found_fails(self):
        """Heartbeat to a non-existent device returns 404 Not Found."""
        bad_url = reverse('sentinel:heartbeat', args=["non-existent-device"])
        response = self.client.post(bad_url)
        self.assertEqual(response.status_code, 404)
        self.assertIn("error", response.json())

    def test_pause_existing_monitor_success(self):
        """Bonus User Story: Pause stops monitoring, sets status to paused."""
        monitor = Monitor.objects.create(
            id=self.device_id,
            timeout=self.timeout,
            alert_email=self.alert_email,
            status='active'
        )
        
        response = self.client.post(self.pause_url)
        self.assertEqual(response.status_code, 200)
        
        data = response.json()
        self.assertEqual(data["status"], "paused")
        self.assertIn("paused", data["message"])
        
        # Verify DB
        monitor.refresh_from_db()
        self.assertEqual(monitor.status, 'paused')

    def test_pause_not_found_fails(self):
        """Pausing a non-existent device returns 404 Not Found."""
        bad_url = reverse('sentinel:pause', args=["non-existent-device"])
        response = self.client.post(bad_url)
        self.assertEqual(response.status_code, 404)
        self.assertIn("error", response.json())

    def test_heartbeat_unpauses_automatically(self):
        """Bonus User Story: Heartbeat on a paused monitor automatically unpauses it."""
        monitor = Monitor.objects.create(
            id=self.device_id,
            timeout=self.timeout,
            alert_email=self.alert_email,
            status='paused',
            last_heartbeat=timezone.now() - timedelta(seconds=30)
        )
        
        response = self.client.post(self.heartbeat_url)
        self.assertEqual(response.status_code, 200)
        
        # Verify DB unpaused
        monitor.refresh_from_db()
        self.assertEqual(monitor.status, 'active')
        self.assertAlmostEqual(
            monitor.next_alert_at,
            timezone.now() + timedelta(seconds=self.timeout),
            delta=timedelta(seconds=2)
        )
