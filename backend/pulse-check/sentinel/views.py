import json
from django.http import JsonResponse, HttpResponse
from django.shortcuts import render, get_object_or_404
from django.views.decorators.csrf import csrf_exempt
from django.utils import timezone
from django.conf import settings
from .models import Monitor, AlertLog
import redis

# Helper to get Redis client
def get_redis_client():
    try:
        r = redis.Redis.from_url(settings.REDIS_URL, decode_responses=True)
        r.ping()
        return r
    except Exception as e:
        print(f"[SYSTEM WARNING] Redis connection failed: {e}. Running in DB-only fallback mode.")
        return None

# CENTRAL TIMER SYNC: Updates Redis state based on model state
def sync_redis_timer(monitor, r=None):
    if r is None:
        r = get_redis_client()
    if not r:
        return
    
    redis_key = f"sentinel:monitor:{monitor.id}:timer"
    if monitor.status == 'active':
        # Calculate remaining timeout in seconds
        now = timezone.now()
        remaining = int((monitor.next_alert_at - now).total_seconds())
        if remaining > 0:
            r.setex(redis_key, remaining, "active")
        else:
            # Already expired, delete key just in case
            r.delete(redis_key)
    else:
        # Paused or Down: remove active timer
        r.delete(redis_key)

# HTML Dashboard View (Developer's Choice)
def dashboard(request):
    monitors = Monitor.objects.all().order_by('-created_at')
    alert_logs = AlertLog.objects.all()[:15] # last 15 alerts
    
    # Calculate stats
    total_count = monitors.count()
    active_count = monitors.filter(status='active').count()
    paused_count = monitors.filter(status='paused').count()
    down_count = monitors.filter(status='down').count()
    
    # Check Redis status
    r = get_redis_client()
    redis_connected = r is not None

    context = {
        'monitors': monitors,
        'alert_logs': alert_logs,
        'total_count': total_count,
        'active_count': active_count,
        'paused_count': paused_count,
        'down_count': down_count,
        'redis_connected': redis_connected,
        'current_time': timezone.now(),
    }
    return render(request, 'sentinel/dashboard.html', context)

# 1. Register a Monitor: POST /monitors
@csrf_exempt
def register_monitor(request):
    if request.method != 'POST':
        return JsonResponse({"error": "Only POST method is allowed"}, status=405)
    
    try:
        data = json.loads(request.body)
        monitor_id = data.get("id")
        timeout = data.get("timeout")
        alert_email = data.get("alert_email")
        
        if not monitor_id or not alert_email:
            return JsonResponse({"error": "Missing required fields: 'id' and 'alert_email' are mandatory"}, status=400)
        
        # Parse and validate timeout
        try:
            timeout = int(timeout)
            if timeout <= 0:
                raise ValueError()
        except (TypeError, ValueError):
            return JsonResponse({"error": "Invalid field: 'timeout' must be a positive integer"}, status=400)
        
        # Get or create monitor
        monitor, created = Monitor.objects.get_or_create(
            id=monitor_id,
            defaults={
                'timeout': timeout,
                'alert_email': alert_email,
                'status': 'active',
                'last_heartbeat': timezone.now()
            }
        )
        
        if not created:
            # Update existing monitor configuration
            monitor.timeout = timeout
            monitor.alert_email = alert_email
            monitor.status = 'active'
            monitor.last_heartbeat = timezone.now()
            monitor.save()
        else:
            # Force save to compute next_alert_at
            monitor.save()
            
        # Set or update the Redis timer to match the monitor state
        r = get_redis_client()
        sync_redis_timer(monitor, r)
            
        status_code = 201 if created else 200
        response_msg = "Monitor registered successfully." if created else "Monitor configuration updated."
        
        return JsonResponse({
            "status": "created" if created else "updated",
            "message": f"Device '{monitor_id}' watchdog active. {response_msg}",
            "monitor": {
                "id": monitor.id,
                "timeout": monitor.timeout,
                "alert_email": monitor.alert_email,
                "status": monitor.status,
                "last_heartbeat": monitor.last_heartbeat.isoformat(),
                "next_alert_at": monitor.next_alert_at.isoformat()
            }
        }, status=status_code)
        
    except json.JSONDecodeError:
        return JsonResponse({"error": "Invalid JSON in request body"}, status=400)
    except Exception as e:
        return JsonResponse({"error": f"Internal server error: {str(e)}"}, status=500)

# 2. Heartbeat Reset: POST /monitors/{id}/heartbeat
@csrf_exempt
def heartbeat_monitor(request, monitor_id):
    if request.method != 'POST':
        return JsonResponse({"error": "Only POST method is allowed"}, status=405)
        
    try:
        monitor = Monitor.objects.filter(id=monitor_id).first()
        if not monitor:
            return JsonResponse({"error": f"Monitor with ID '{monitor_id}' not found"}, status=404)
        
        # Acceptance criteria & snooze: Reset automatically "un-pauses"
        # and restarts the timer from the beginning
        monitor.status = 'active'
        monitor.last_heartbeat = timezone.now()
        monitor.save() # recalculates next_alert_at
        
        # Keep Redis timer aligned with the monitor state
        r = get_redis_client()
        sync_redis_timer(monitor, r)
            
        return JsonResponse({
            "status": "ok",
            "message": f"Heartbeat acknowledged for device '{monitor.id}'. Watchdog sentinel reset.",
            "monitor": {
                "id": monitor.id,
                "status": monitor.status,
                "last_heartbeat": monitor.last_heartbeat.isoformat(),
                "next_alert_at": monitor.next_alert_at.isoformat()
            }
        }, status=200)
        
    except Exception as e:
        return JsonResponse({"error": f"Internal server error: {str(e)}"}, status=500)

# 3. Snooze / Pause: POST /monitors/{id}/pause
@csrf_exempt
def pause_monitor(request, monitor_id):
    if request.method != 'POST':
        return JsonResponse({"error": "Only POST method is allowed"}, status=405)
        
    try:
        monitor = Monitor.objects.filter(id=monitor_id).first()
        if not monitor:
            return JsonResponse({"error": f"Monitor with ID '{monitor_id}' not found"}, status=404)
            
        # Pause monitor
        monitor.status = 'paused'
        monitor.save()
        
        # Keep Redis timer aligned with the monitor state
        r = get_redis_client()
        sync_redis_timer(monitor, r)
            
        return JsonResponse({
            "status": "paused",
            "message": f"Watchdog sentinel paused for device '{monitor.id}'. Snooze active.",
            "monitor": {
                "id": monitor.id,
                "status": monitor.status
            }
        }, status=200)
        
    except Exception as e:
        return JsonResponse({"error": f"Internal server error: {str(e)}"}, status=500)
