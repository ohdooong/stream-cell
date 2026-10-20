# 실시간 Producer 토픽별 필드 타입

Kafka value 본문의 필드를 `"필드명": "타입"` 형식으로 정리했습니다. `STRING`은 문자열, `INT`는 정수, `BOOLEAN`은 불리언, `TIMESTAMP(3)`은 밀리초 정밀도의 시각, `NULL`은 null입니다.

모든 필드는 메시지에 포함됩니다. `event_time`의 스키마 타입은 `TIMESTAMP(3)`입니다. Kafka JSON에는 현재 `yyyy-MM-dd HH:mm:ss` 문자열로 전송되며, 파싱한 시각의 밀리초 부분은 `000`입니다. 소요 시간을 나타내는 `duration_ms`, `latency_ms`는 밀리초 단위의 `INT`입니다. `application-logs.error_code`는 정상 이벤트에서 `NULL`, 이상 이벤트에서 `STRING`입니다. 위·경도와 역률을 포함한 모든 숫자 필드는 정수입니다.

## iot-environment

온도, 습도, 초미세먼지, 배터리 잔량을 전송하는 환경 센서 토픽입니다.

Producer: `environment` · 메시지 Key: `device_id`

```json
{
  "event_id": "STRING",
  "event_type": "STRING",
  "schema_version": "INT",
  "event_time": "TIMESTAMP(3)",
  "site_id": "STRING",
  "zone_id": "STRING",
  "is_anomaly": "BOOLEAN",
  "device_id": "STRING",
  "temperature_c": "INT",
  "humidity_pct": "INT",
  "pm25_ug_m3": "INT",
  "battery_pct": "INT"
}
```

## iot-equipment

모터의 진동, 베어링 온도, 회전수와 운전 상태를 전송하는 설비 모니터링 토픽입니다.

Producer: `equipment` · 메시지 Key: `equipment_id`

```json
{
  "event_id": "STRING",
  "event_type": "STRING",
  "schema_version": "INT",
  "event_time": "TIMESTAMP(3)",
  "site_id": "STRING",
  "zone_id": "STRING",
  "is_anomaly": "BOOLEAN",
  "equipment_id": "STRING",
  "vibration_mm_s": "INT",
  "bearing_temperature_c": "INT",
  "rpm": "INT",
  "operating_state": "STRING"
}
```

## iot-energy

전압, 전류, 역률, 유효전력, 주파수를 전송하는 전력 계측 토픽입니다.

Producer: `energy` · 메시지 Key: `meter_id`

```json
{
  "event_id": "STRING",
  "event_type": "STRING",
  "schema_version": "INT",
  "event_time": "TIMESTAMP(3)",
  "site_id": "STRING",
  "zone_id": "STRING",
  "is_anomaly": "BOOLEAN",
  "meter_id": "STRING",
  "voltage_v": "INT",
  "current_a": "INT",
  "power_factor": "INT",
  "active_power_kw": "INT",
  "frequency_hz": "INT"
}
```

## application-logs

서비스의 처리 로그와 소요 시간, 오류 정보를 전송하는 애플리케이션 로그 토픽입니다.

Producer: `application-log` · 메시지 Key: `service_name`

```json
{
  "event_id": "STRING",
  "event_type": "STRING",
  "schema_version": "INT",
  "event_time": "TIMESTAMP(3)",
  "site_id": "STRING",
  "zone_id": "STRING",
  "is_anomaly": "BOOLEAN",
  "service_name": "STRING",
  "instance_id": "STRING",
  "log_level": "STRING",
  "message": "STRING",
  "trace_id": "STRING",
  "duration_ms": "INT",
  "error_code": "STRING | NULL"
}
```

## http-access-logs

HTTP 요청의 메서드, 경로, 응답 상태, 지연시간과 응답 크기를 전송하는 접근 로그 토픽입니다.

Producer: `http-access` · 메시지 Key: `service_name`

```json
{
  "event_id": "STRING",
  "event_type": "STRING",
  "schema_version": "INT",
  "event_time": "TIMESTAMP(3)",
  "site_id": "STRING",
  "zone_id": "STRING",
  "is_anomaly": "BOOLEAN",
  "service_name": "STRING",
  "http_method": "STRING",
  "request_path": "STRING",
  "status_code": "INT",
  "latency_ms": "INT",
  "response_bytes": "INT",
  "client_ip": "STRING"
}
```

## security-events

로그인 성공·실패, 접속 IP, 실패 횟수와 위험 점수를 전송하는 보안 이벤트 토픽입니다.

Producer: `security` · 메시지 Key: `user_id`

```json
{
  "event_id": "STRING",
  "event_type": "STRING",
  "schema_version": "INT",
  "event_time": "TIMESTAMP(3)",
  "site_id": "STRING",
  "zone_id": "STRING",
  "is_anomaly": "BOOLEAN",
  "user_id": "STRING",
  "security_action": "STRING",
  "outcome": "STRING",
  "source_ip": "STRING",
  "failed_attempts": "INT",
  "risk_score": "INT",
  "reason": "STRING"
}
```

## worker-safety-events

작업자의 안전모·안전조끼 착용, 낙상, 심박수와 제한구역 진입을 전송하는 안전관리 토픽입니다.

Producer: `worker-safety` · 메시지 Key: `worker_id`

```json
{
  "event_id": "STRING",
  "event_type": "STRING",
  "schema_version": "INT",
  "event_time": "TIMESTAMP(3)",
  "site_id": "STRING",
  "zone_id": "STRING",
  "is_anomaly": "BOOLEAN",
  "worker_id": "STRING",
  "helmet_detected": "BOOLEAN",
  "safety_vest_detected": "BOOLEAN",
  "fall_detected": "BOOLEAN",
  "heart_rate_bpm": "INT",
  "restricted_zone_entered": "BOOLEAN",
  "alert_type": "STRING"
}
```

## gas-detection-events

일산화탄소, 황화수소, 산소, 가연성가스 측정값과 경보 상태를 전송하는 가스 감지 토픽입니다.

Producer: `gas` · 메시지 Key: `sensor_id`

```json
{
  "event_id": "STRING",
  "event_type": "STRING",
  "schema_version": "INT",
  "event_time": "TIMESTAMP(3)",
  "site_id": "STRING",
  "zone_id": "STRING",
  "is_anomaly": "BOOLEAN",
  "sensor_id": "STRING",
  "co_ppm": "INT",
  "h2s_ppm": "INT",
  "oxygen_pct": "INT",
  "combustible_gas_pct_lel": "INT",
  "alarm_active": "BOOLEAN"
}
```

## fire-detection-events

연기, 온도, 불꽃 감지 여부와 경보 상태를 전송하는 화재 감지 토픽입니다.

Producer: `fire` · 메시지 Key: `sensor_id`

```json
{
  "event_id": "STRING",
  "event_type": "STRING",
  "schema_version": "INT",
  "event_time": "TIMESTAMP(3)",
  "site_id": "STRING",
  "zone_id": "STRING",
  "is_anomaly": "BOOLEAN",
  "sensor_id": "STRING",
  "smoke_obscuration_pct_m": "INT",
  "temperature_c": "INT",
  "flame_detected": "BOOLEAN",
  "alarm_active": "BOOLEAN"
}
```

## vehicle-telemetry

지게차의 위치, 속도, 적재량, 접근거리와 안전벨트 상태를 전송하는 차량 모니터링 토픽입니다.

Producer: `vehicle` · 메시지 Key: `vehicle_id`

```json
{
  "event_id": "STRING",
  "event_type": "STRING",
  "schema_version": "INT",
  "event_time": "TIMESTAMP(3)",
  "site_id": "STRING",
  "zone_id": "STRING",
  "is_anomaly": "BOOLEAN",
  "vehicle_id": "STRING",
  "latitude": "INT",
  "longitude": "INT",
  "speed_kmh": "INT",
  "load_kg": "INT",
  "proximity_distance_m": "INT",
  "seatbelt_fastened": "BOOLEAN",
  "alert_type": "STRING"
}
```

