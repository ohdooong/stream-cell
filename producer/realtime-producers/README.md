# 실시간 Kafka Producer 10종

실제 장비 연결 없이 IoT·로그·안전관리 이벤트를 합성하는 테스트용 Producer입니다. 기존 orders Producer와 별도로 사용합니다. 공통 실행기가 종류별 생성기를 실행하며, `all`은 매 회차 10개 토픽에 각각 1건씩 전송합니다.

| --kind | Kafka 토픽 | 메시지 Key | 데이터 |
| --- | --- | --- | --- |
| environment | iot-environment | device_id | 온도, 습도, 초미세먼지, 배터리 |
| equipment | iot-equipment | equipment_id | 모터 진동, 베어링 온도, RPM, 운전상태 |
| energy | iot-energy | meter_id | 전압, 전류, 역률, 유효전력, 주파수 |
| application-log | application-logs | service_name | 로그레벨, 메시지, Trace ID, 처리시간, 오류코드 |
| http-access | http-access-logs | service_name | HTTP 메서드, 경로, 상태코드, 지연시간, 응답크기 |
| security | security-events | user_id | 로그인 결과, 실패횟수, IP, 위험점수 |
| worker-safety | worker-safety-events | worker_id | 안전모, 안전조끼, 낙상, 심박수, 제한구역 진입 |
| gas | gas-detection-events | sensor_id | CO, H₂S, 산소, 가연성가스, 경보 |
| fire | fire-detection-events | sensor_id | 연기, 온도, 불꽃, 경보 |
| vehicle | vehicle-telemetry | vehicle_id | 지게차 위치, 속도, 적재량, 접근거리, 안전벨트 |

## 실행 (프로젝트 루트, PowerShell)

Kafka 없이 모든 종류의 샘플 1건씩 출력:

```powershell
python producer/realtime-producers/producer.py --dry-run --count 1
```

의존성 설치 및 기존 인프라 실행:

```powershell
python -m venv producer/realtime-producers/.venv
producer/realtime-producers/.venv/Scripts/python.exe -m pip install -r producer/realtime-producers/requirements.txt
docker compose -f infra/docker-compose.yml up -d
```

전체 종류를 1초마다 전송하고 필요한 토픽 생성:

```powershell
producer/realtime-producers/.venv/Scripts/python.exe producer/realtime-producers/producer.py --kind all --create-topics
```

### 전체 실행 쉘 스크립트 (Linux / macOS / Git Bash)

위 의존성 설치와 Kafka 시작 후 다음 명령 하나로 10종을 모두 실행합니다. 기존 orders Producer는 포함하지 않습니다.

```bash
bash producer/realtime-producers/run-all.sh --create-topics
```

Kafka 없이 10종 샘플을 확인하거나 공통 전송 주기와 이상 비율을 지정할 수도 있습니다.

```bash
bash producer/realtime-producers/run-all.sh --dry-run --count 1
bash producer/realtime-producers/run-all.sh --create-topics --interval 0.5 --anomaly-rate 0.3
```

스크립트는 작업 디렉터리와 무관하게 자신의 위치에서 `producer.py`를 찾으며, 한 프로세스에서 10종을 실행합니다. `Ctrl+C`로 전체 종료합니다. `.venv`의 Python을 우선 사용하고 없으면 `python3` 또는 `python`을 사용합니다. `PYTHON_BIN` 환경변수로 Python 실행파일 경로를 직접 지정할 수 있습니다. 나머지 인자는 그대로 Producer에 전달됩니다. Linux/macOS에서 처음 설치할 때는 `python3 -m venv producer/realtime-producers/.venv`와 `producer/realtime-producers/.venv/bin/python -m pip install -r producer/realtime-producers/requirements.txt`를 사용하세요.

가스 감지 이벤트만 0.5초마다 100건, 이상 이벤트 비율 30%:

```powershell
producer/realtime-producers/.venv/Scripts/python.exe producer/realtime-producers/producer.py --kind gas --interval 0.5 --count 100 --anomaly-rate 0.3 --create-topics
```

`--count 0`(기본값)은 무제한이며 Ctrl+C로 종료하고 대기 중인 메시지를 flush합니다. `--count N`은 **종류당** N건입니다. 종류별로 다른 주기가 필요하면 터미널 여러 개에서 개별 실행하세요. Kafka UI는 http://localhost:8080 에서 토픽별 메시지를 확인할 수 있습니다.

기본 브로커는 `localhost:19092,localhost:19093,localhost:19094`입니다. `--bootstrap-servers` 또는 `KAFKA_BOOTSTRAP_SERVERS` 환경변수로 변경할 수 있습니다. Docker 네트워크 안에서는 `kafka-1:9092,kafka-2:9092,kafka-3:9092`를 사용하세요. 기존 Compose의 kafka-init을 다시 실행하려면 `docker compose -f infra/docker-compose.yml run --rm kafka-init`을 사용하세요. `--create-topics`는 기존 토픽을 유지하며, 기본 파티션 3개/복제계수 3입니다. 단일 브로커에서는 `--replication-factor 1`을 지정하세요.

## 데이터 형식

Kafka value는 UTF-8 JSON이며 공통 필드는 `event_id`(UUID), `event_type`, `schema_version`, `event_time`(UTC ISO 8601 밀리초), `site_id`, `zone_id`, `is_anomaly`입니다. 종류별 필드는 위 표를 참고하세요. 정상/이상 여부에 따라 관련 수치와 상태를 함께 변경합니다. `--anomaly-rate 0`은 정상만, `1`은 이상만 생성합니다. 기본값 0.1은 확률이므로 실제 건수의 정확한 10%를 보장하지 않습니다.

```json
{"event_id":"example-uuid","event_type":"gas","schema_version":1,"event_time":"2026-10-04T00:00:00.000Z","site_id":"site-001","zone_id":"zone-001","is_anomaly":true,"sensor_id":"gas-001","co_ppm":150.0,"h2s_ppm":30.0,"oxygen_pct":17.0,"combustible_gas_pct_lel":40.0,"alarm_active":true}
```

Flink에서는 `event_time`을 STRING으로 읽고 `TO_TIMESTAMP_LTZ` 등 사용 환경에 맞는 변환을 적용하세요. 기존 orders의 `yyyy-MM-dd HH:mm:ss` 형식과 다릅니다. 동일 엔티티 Key는 같은 파티션으로 라우팅합니다. 장비 상태는 매번 독립 생성되며 실제 센서 시계열의 연속성을 모델링하지 않습니다. `--seed`는 종류별 값과 선택을 재현하지만 UUID와 현재 시각은 매 실행 달라집니다. 안전 관련 수치와 경보는 데모 시나리오이며 실제 안전기준으로 사용하지 마세요.

전송은 idempotence와 `acks=all`을 사용합니다. 전송 콜백 오류나 flush 후 미전송 메시지가 있으면 종료코드 1을 반환합니다. 재실행 간 중복 제거까지 보장하지 않습니다.

## 검증

```powershell
python -m unittest discover -s producer/realtime-producers -p "test_*.py"
```
