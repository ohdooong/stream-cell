"""Synthetic streaming scenarios. Thresholds are demo values, not safety standards."""
import uuid
from datetime import datetime, timezone


KINDS = {
    "environment": ("iot-environment", "device_id"),
    "equipment": ("iot-equipment", "equipment_id"),
    "energy": ("iot-energy", "meter_id"),
    "application-log": ("application-logs", "service_name"),
    "http-access": ("http-access-logs", "service_name"),
    "security": ("security-events", "user_id"),
    "worker-safety": ("worker-safety-events", "worker_id"),
    "gas": ("gas-detection-events", "sensor_id"),
    "fire": ("fire-detection-events", "sensor_id"),
    "vehicle": ("vehicle-telemetry", "vehicle_id"),
}


def create_event(kind, rng, anomaly_rate=0.1):
    abnormal = rng.random() < anomaly_rate
    entity = rng.randint(1, 20)
    site = f"site-{rng.randint(1, 3):03d}"
    zone = f"zone-{rng.randint(1, 5):03d}"
    number = lambda low, high: round(rng.uniform(low, high), 2)
    if kind == "environment":
        data = dict(device_id=f"env-{entity:03d}", temperature_c=number(45, 65) if abnormal else number(18, 30), humidity_pct=number(30, 75), pm25_ug_m3=number(100, 200) if abnormal else number(5, 30), battery_pct=number(10, 100))
    elif kind == "equipment":
        data = dict(equipment_id=f"motor-{entity:03d}", vibration_mm_s=number(10, 20) if abnormal else number(0.2, 3), bearing_temperature_c=number(90, 120) if abnormal else number(35, 65), rpm=rng.randint(1400, 1600), operating_state="DEGRADED" if abnormal else "RUNNING")
    elif kind == "energy":
        voltage = number(210, 230)
        current = number(80, 120) if abnormal else number(5, 30)
        factor = number(0.85, 0.99)
        data = dict(meter_id=f"meter-{entity:03d}", voltage_v=voltage, current_a=current, power_factor=factor, active_power_kw=round(voltage * current * factor / 1000, 3), frequency_hz=number(59.8, 60.2))
    elif kind == "application-log":
        data = dict(service_name=rng.choice(["streamcell-api", "pipeline-service", "collector"]), instance_id=f"instance-{entity:03d}", level="ERROR" if abnormal else rng.choice(["INFO", "DEBUG"]), message="Upstream request timed out" if abnormal else "Request processed successfully", trace_id=uuid.uuid4().hex, duration_ms=rng.randint(2000, 5000) if abnormal else rng.randint(5, 150), error_code="UPSTREAM_TIMEOUT" if abnormal else None)
    elif kind == "http-access":
        status = rng.choice([500, 502, 503]) if abnormal else rng.choice([200, 201, 204])
        data = dict(service_name="streamcell-api", method=rng.choice(["GET", "POST"]), path=rng.choice(["/api/topics", "/api/pipelines", "/api/deployments"]), status_code=status, latency_ms=rng.randint(1000, 5000) if abnormal else rng.randint(10, 200), response_bytes=0 if abnormal else rng.randint(100, 10000), client_ip=f"192.0.2.{entity}")
    elif kind == "security":
        data = dict(user_id=f"user-{entity:03d}", action="LOGIN", outcome="FAILURE" if abnormal else "SUCCESS", source_ip=f"198.51.100.{entity}", failed_attempts=rng.randint(5, 20) if abnormal else 0, risk_score=rng.randint(80, 100) if abnormal else rng.randint(0, 20), reason="REPEATED_INVALID_PASSWORD" if abnormal else "AUTHENTICATED")
    elif kind == "worker-safety":
        data = dict(worker_id=f"worker-{entity:03d}", helmet_detected=not abnormal, safety_vest_detected=True, fall_detected=abnormal and rng.choice([True, False]), heart_rate_bpm=rng.randint(130, 180) if abnormal else rng.randint(65, 100), restricted_zone_entered=abnormal, alert_type="UNSAFE_WORKER" if abnormal else "NONE")
    elif kind == "gas":
        data = dict(sensor_id=f"gas-{entity:03d}", co_ppm=number(100, 250) if abnormal else number(0, 10), h2s_ppm=number(20, 50) if abnormal else number(0, 1), oxygen_pct=number(16, 18) if abnormal else number(20, 21), combustible_gas_pct_lel=number(25, 60) if abnormal else number(0, 3), alarm_active=abnormal)
    elif kind == "fire":
        data = dict(sensor_id=f"fire-{entity:03d}", smoke_obscuration_pct_m=number(15, 40) if abnormal else number(0, 1), temperature_c=number(70, 150) if abnormal else number(18, 30), flame_detected=abnormal, alarm_active=abnormal)
    elif kind == "vehicle":
        data = dict(vehicle_id=f"forklift-{entity:03d}", latitude=number(37.50, 37.51), longitude=number(127.00, 127.01), speed_kmh=number(20, 35) if abnormal else number(0, 8), load_kg=number(0, 1500), proximity_distance_m=number(0.1, 0.5) if abnormal else number(3, 20), seatbelt_fastened=not abnormal, alert_type="COLLISION_RISK" if abnormal else "NONE")
    else:
        raise ValueError(f"Unknown producer: {kind}")
    return dict(event_id=str(uuid.uuid4()), event_type=kind, schema_version=1,
                event_time=datetime.now(timezone.utc).isoformat(timespec="milliseconds").replace("+00:00", "Z"),
                site_id=site, zone_id=zone, is_anomaly=abnormal, **data)
