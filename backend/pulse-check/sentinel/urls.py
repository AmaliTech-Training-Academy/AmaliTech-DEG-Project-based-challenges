from django.urls import path
from . import views

app_name = 'sentinel'

urlpatterns = [
    path('', views.dashboard, name='dashboard'),
    path('monitors', views.register_monitor, name='register'),
    path('monitors/<str:monitor_id>/heartbeat', views.heartbeat_monitor, name='heartbeat'),
    path('monitors/<str:monitor_id>/pause', views.pause_monitor, name='pause'),
]
