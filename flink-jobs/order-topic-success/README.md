# Orders topic success job

StreamCell의 Custom JAR 업로드/배포 성공 경로를 확인하기 위한 최소 Flink Job입니다.
Kafka의 `orders` 토픽에서 새 메시지를 읽고 빈 메시지를 제외한 뒤 TaskManager 로그에
`orders> ...` 형태로 출력합니다. 별도 sink가 없어 Kafka 소비와 Flink 배포 자체를
검증하기에 적합합니다.

## Build

저장소 루트에서 실행합니다.

```powershell
.\backend\streamcell-api\streamcell\gradlew.bat `
  -p flink-jobs/order-topic-success clean build
```

업로드할 파일은 다음 위치에 생성됩니다.

```text
flink-jobs/order-topic-success/build/libs/order-topic-success-job-1.0.0.jar
```

이 JAR은 Flink 런타임 의존성을 포함하지 않는 thin JAR입니다. 저장소의
`infra/docker-compose.yml`처럼 Flink 1.19.1과 Kafka connector
`3.2.0-1.19`가 클러스터에 설치되어 있어야 합니다.

## StreamCell Custom Job 설정

- entry class: `com.streamcell.jobs.order.OrderTopicSuccessJob`
- input topic ID: 로컬 seed 데이터 기준 `orders`의 topic ID (`1`)
- parallelism: `1`
- program args:

```json
{
  "--bootstrap-servers": "kafka-1:9092,kafka-2:9092,kafka-3:9092",
  "--topic": "orders",
  "--group-id": "streamcell-order-topic-success",
  "--startup-mode": "latest"
}
```

`latest`는 Job 기동 뒤 들어오는 주문부터 처리합니다. 이미 토픽에 쌓인 주문도
확인하려면 아직 사용하지 않은 group ID와 `earliest`를 지정합니다.

## Direct Flink execution

컨테이너 내부에서 직접 실행할 때의 예시입니다.

```bash
flink run \
  -c com.streamcell.jobs.order.OrderTopicSuccessJob \
  order-topic-success-job-1.0.0.jar \
  --bootstrap-servers kafka-1:9092,kafka-2:9092,kafka-3:9092 \
  --topic orders \
  --group-id streamcell-order-topic-success \
  --startup-mode latest
```

producer를 실행해 주문을 넣은 다음 TaskManager 로그에서 소비 결과를 확인합니다.

```powershell
python producer/order-producer/producer.py
docker logs -f streamcell-flink-taskmanager
```
