# AI_SQL 백엔드 구현 계약

이 문서는 새 AI_SQL 등록 화면에 맞춘 **신규 API 제안**입니다. 아래 생성·등록 API는 현재 백엔드에 없습니다. 프론트엔드 호출 코드와 요청/응답 타입은 `src/api/aiSql.ts`에 준비되어 있습니다. 백엔드 Java/SQL 코드는 이번 변경에 포함하지 않았습니다.

## 현재 구현과 화면 동작

- `POST /api/v1/platform/pipeline/pipelines`는 소유자, 이름, 설명, 타입만 받습니다. CUSTOM_JAR 등록은 이 경로를 유지합니다.
- AI_SQL은 이 기존 경로로 메타데이터만 생성하지 않습니다. `설정 → 미리보기 생성 → 검토 → 전체 설정 저장` 흐름을 사용합니다.
- Topic 목록과 상세는 기존 `GET /api/v1/platform/topic/topics`, `GET /api/v1/platform/topic/topics/{topicId}`를 사용합니다. 상세에서 `schemaJson`, `timeField`, `messageFormat`을 가져옵니다.
- 사용자는 소유자를 입력하지 않습니다. 인증 연동 전에는 콘솔의 현재 개발 사용자 ID를 요청에 넣습니다. Spring Security 연동 시 서버가 principal에서 소유자를 정하도록 변경합니다.
- API가 없으면 준비 중 안내를 표시하고 폼 입력을 유지합니다. DB 저장 성공으로 표시하거나 샘플 Plan/SQL을 반환하지 않습니다. 페이지 이동/새로고침 시 입력은 유지되지 않습니다.
- 생성된 SQL은 읽기 전용입니다. 요청/설정을 고친 뒤 다시 생성해 수정합니다. 모든 입력 변경 및 사용자 변경 시 이전 검토 결과는 무효가 됩니다.

## 1. 미리보기 생성

`POST /api/v1/platform/pipeline/pipelines/ai-sql/preview`

```json
{
  "ownerUserId": 1,
  "pipelineName": "상품별 주문 집계",
  "description": "5분 단위 상품별 주문 건수와 총금액",
  "pipelineType": "AI_SQL",
  "inputTopicIds": [1],
  "naturalLanguageRequest": "취소된 주문을 제외하고 5분 단위로 상품별 주문 건수와 총금액을 집계해줘.",
  "timeConfig": {
    "mode": "EVENT_TIME",
    "eventTimeFields": [{ "topicId": 1, "field": "eventTime" }],
    "watermarkDelaySeconds": 5,
    "timezone": "Asia/Seoul"
  },
  "sinkConfig": {
    "sinkType": "POSTGRESQL",
    "tableNaming": "AUTO",
    "tableName": null
  },
  "executionConfig": {
    "startupMode": "LATEST",
    "parallelism": 1
  }
}
```

| 필드 | 계약 |
| --- | --- |
| `ownerUserId` | 인증 전 개발 사용자. 존재하는 사용자 ID인지 검증 |
| `pipelineName` | trim 후 1~100자 |
| `description` | 선택, 최대 1,000자 |
| `inputTopicIds` | 정확히 1개의 유효한 Topic ID를 담은 배열. 프론트는 단일 선택, 서버도 배열 길이 1 검증 |
| `naturalLanguageRequest` | trim 후 1~4,000자 |
| `timeConfig.mode` | `EVENT_TIME` 또는 `PROCESSING_TIME` |
| `eventTimeFields` | EVENT_TIME이면 선택한 Topic의 필드 1개. PROCESSING_TIME이면 빈 배열 |
| `watermarkDelaySeconds` | 0 이상의 정수. PROCESSING_TIME 요청은 0 |
| `timezone` | `Asia/Seoul` 기본값, 서버가 지원하는 IANA 시간대 검증 |
| `sinkType` | 1차 범위는 관리형 `POSTGRESQL` |
| `tableNaming` | `AUTO` 또는 `CUSTOM` |
| `tableName` | AUTO이면 null. CUSTOM이면 `^[a-z][a-z0-9_]{0,62}$` |
| `startupMode` | `LATEST`(새 데이터) 또는 `EARLIEST`(보관 중인 데이터). 신규 실행에 적용 |
| `parallelism` | 1 이상의 정수, 서버에서 실제 허용 상한도 검증 |

서버 처리:

1. 요청을 검증하고 DB에서 Topic Schema/형식/시간 필드를 다시 조회합니다. Schema 원문은 프론트 요청에 포함하지 않습니다.
2. 시간 필드 존재뿐 아니라 타입, 타임스탬프 형식/정밀도/시간대와 변환 가능 여부를 확인합니다. 입력 Topic은 하나만 허용합니다.
3. 자연어와 Schema를 이용해 집계 주기, 그룹 키, 필터, 집계식, 조인, 결과 컬럼을 담은 구조화된 Plan을 생성합니다. 모호한 요청은 미확정 조건을 설명하고 `validation.valid=false`로 응답합니다.
4. 실제 실행 환경에 맞는 Flink SQL을 생성하고, 해당 환경의 파서/플래너 및 연결 가능한 connector로 검증합니다. 생성 과정에서 Job 제출이나 결과 테이블 생성은 하지 않습니다.
5. 검토 가능한 SQL에서는 연결 비밀번호 등 비밀값을 제외합니다. 실제 실행 시 서버가 연결 정보를 주입합니다.
6. 정규화한 입력, 사용자 ID, Topic Schema snapshot/hash, Plan, SQL, 결과 테이블명, 유효기간을 `previewId`와 연결해 서버에 저장합니다. AUTO 테이블명도 이 단계에 결정하여 저장/배포 때 바뀌지 않게 합니다.

응답은 기존 `BaseResponse.body` 형식을 유지합니다. 예시 Plan/SQL 문자열은 구조 설명용이며 실제 응답에서는 생성·검증 결과를 반환해야 합니다.

```json
{
  "status": 200,
  "message": "success",
  "timestamp": "2026-09-22T12:00:00+09:00",
  "body": {
    "previewId": "server-generated-uuid",
    "expiresAt": "2026-09-22T12:15:00+09:00",
    "pipelinePlan": {
      "summary": "취소 주문을 제외한 상품별 5분 집계",
      "steps": [
        { "title": "입력", "description": "orders Topic, eventTime 기준" },
        { "title": "필터 및 집계", "description": "취소 주문 제외, 상품별 주문 건수와 총금액" },
        { "title": "결과 저장", "description": "관리형 PostgreSQL의 Pipeline 전용 테이블" }
      ]
    },
    "generatedSql": "실제 생성 및 검증된 Flink SQL 문자열",
    "warnings": [],
    "validation": { "valid": true, "errors": [] }
  }
}
```

프론트는 `validation.valid=true`이고 오류가 없고 유효기간이 남은 응답에 대해 사용자의 검토 확인을 받아 등록 버튼을 활성화합니다. 생성 자체가 실패하면 정상 preview 대신 오류 응답을 사용합니다.

## 2. 검토한 AI_SQL Pipeline 등록

`POST /api/v1/platform/pipeline/pipelines/ai-sql`

위 미리보기 요청의 모든 필드에 `previewId`를 추가한 JSON을 받습니다. 클라이언트가 SQL 또는 Plan 원문을 임의로 제출하는 형태가 아닙니다.

서버 처리:

1. preview의 사용자, 유효기간, 검증 결과 및 요청 내용을 대조합니다. 모든 설정이 미리보기와 같아야 합니다.
2. Topic Schema가 미리보기 이후 변경됐으면 재생성을 요구합니다.
3. 서버에 보관한 Plan/SQL과 입력 설정을 이용해 Pipeline 및 아래 설정 테이블을 한 트랜잭션으로 저장합니다. 부분적으로 생성된 Pipeline이 남으면 안 됩니다.
4. 같은 사용자와 `previewId`의 재요청은 같은 `pipelineId`를 반환하도록 멱등성을 보장합니다. 네트워크 오류 재시도 시 중복 등록을 방지합니다.
5. 이 요청에서는 실행하지 않고 `DRAFT`로 저장합니다. 배포는 별도 요청입니다.

응답: 기존 `PipelineResponse.Pipeline`을 `BaseResponse.body`에 반환합니다. 특히 양수 `pipelineId`, `ownerUserId`, `pipelineName`, `pipelineType: AI_SQL`, `pipelineStatus: DRAFT`가 필요합니다.

## 3. 저장 구조 및 상세 조회 확장

기존 `platform.pipeline`의 `natural_language_request`, `pipeline_plan_json`, `generated_sql`을 사용합니다. `pipeline_sink_config`의 기존 `sink_type`, `sink_table_name`도 사용 가능합니다.

추가 저장 항목(테이블명은 제안):

- `pipeline_ai_sql_config`: pipeline_id, time_mode, watermark_delay_seconds, timezone, startup_mode, parallelism, preview_id/version.
- `pipeline_input_topic`: pipeline_id, topic_id, event_time_field, 검증 시점 Schema snapshot/hash. AI_SQL Pipeline마다 입력 Topic 하나를 저장하고 유일성 제약을 둡니다.
- `pipeline_sink_config`: 자동/사용자 지정 구분을 위한 table_naming. 결과 컬럼 타입, 집계 키/upsert 키 등 실행에 필요한 정보는 Plan 또는 별도 설정으로 보존합니다.
- preview 저장소: TTL, owner, 요청 fingerprint, Schema fingerprint, Plan/SQL, 생성된 pipelineId. DB 또는 TTL 저장소를 사용할 수 있으며 등록 멱등성 기록은 만료와 별도로 보존합니다.

기존 Pipeline 상세 GET에 `inputTopicIds`, `timeConfig`, `sinkConfig`, `executionConfig`를 확장하면 향후 상세 화면에서 저장된 설정을 복원할 수 있습니다. 프론트 상세 화면에 이 확장 응답을 표시하는 작업은 별도 연결 대상입니다.

## 4. AI_SQL 배포 구현

현재 `PipelineDeploymentServiceImpl.deploy`는 Artifact와 CustomJobConfig를 필수로 조회하고 JAR를 업로드/실행합니다. AI_SQL 배포에는 별도 분기가 필요합니다.

- AI_SQL은 저장된 검증 SQL/Plan 및 실행 설정을 조회합니다. JAR 업로드를 요구하지 않습니다.
- 현재 Flink 환경에서 사용할 SQL 제출 방식(SQL Gateway 또는 별도 실행 계층)을 정하고 연결합니다.
- 관리형 PostgreSQL 결과 테이블의 컬럼, 집계 키, 타입을 Plan에 맞게 준비합니다. 기존 테이블명을 임의로 덮어쓰지 않도록 충돌과 소유 범위를 검증합니다.
- 결과 변경 방식에 맞는 쓰기/갱신 처리를 구성하고, 실행 시 연결 정보를 주입합니다.
- `DEPLOYING → RUNNING/FAILED`, Job ID, 실행자, 배포 ID, 오류 및 실제 deploymentType을 기록합니다. 예외로 실패 이력이 롤백되지 않도록 트랜잭션 경계를 검토합니다.
- 구현 후 프론트 `PipelineDetail`의 배포 가능 조건에 AI_SQL을 추가해야 합니다. 현재 버튼은 CUSTOM_JAR만 허용합니다.

## 오류 계약과 완료 확인

프론트는 HTTP 실패 응답의 `message`를 표시합니다. 실패를 HTTP 200으로 감싸지 않습니다.

- 400: 필수값, Schema/필드, SQL 검증 등 입력 오류(사용자가 수정할 수 있는 메시지).
- 404/405/501: 기능 미제공. 프론트는 준비 중 안내.
- 409: Schema/입력 불일치, 결과 테이블 충돌. 프론트는 재생성을 안내.
- 410: preview 만료. 프론트는 재생성을 안내.
- 5xx: 생성 모델 또는 실행 시스템 오류. 재시도 가능한 메시지.

백엔드 완료 기준:

1. 단일 Topic, Event/Processing Time, AUTO/CUSTOM 결과 테이블, LATEST/EARLIEST 설정의 생성·저장 round-trip. 입력 Topic 0개 또는 2개 이상은 거부.
2. 존재하지 않는 Topic, 미등록 Schema, 잘못된 시간 타입, 유효하지 않은 SQL에 대한 실패 응답.
3. 설정/사용자/Schema가 바뀐 preview 및 만료 preview의 등록 거부.
4. 동일 preview 재시도 시 Pipeline 중복 생성 없음, 일부 설정 저장 실패 시 원자적 롤백.
5. AI_SQL 배포 성공/실패 시 Job ID와 상태·실패 이력이 실제 값으로 남음.

인증·인가는 후속 작업으로 둡니다. 연동 시 소유자는 principal에서 확정하고, Topic QUERY/DEPLOY 및 Pipeline 소유 권한을 각 API에서 확인합니다.
