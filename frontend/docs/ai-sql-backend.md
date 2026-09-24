# AI_SQL 백엔드 구현 계약 (MVP)

이 문서는 현재 AI_SQL 등록 화면이 호출하는 신규 API 제안입니다. 백엔드 Java/SQL 구현은 이번 프론트엔드 변경에 포함하지 않습니다.

MVP 입력 범위는 Pipeline 이름, 설명, 단일 입력 Topic, 자연어 처리 요청입니다. 시간 기준, 결과 저장 설정, 고급 실행 설정은 후속 고도화 범위이며 현재 요청·응답과 화면에 포함하지 않습니다.

## 현재 화면 동작

- Topic 목록과 상세는 기존 `GET /api/v1/platform/topic/topics`, `GET /api/v1/platform/topic/topics/{topicId}`를 사용합니다.
- 사용자는 소유자를 입력하지 않습니다. 인증 연동 전에는 콘솔의 현재 개발 사용자 ID를 요청에 넣습니다. Spring Security 연동 후에는 서버가 principal에서 소유자를 정해야 합니다.
- AI_SQL은 `설정 입력 → Plan·SQL 미리보기 → 사용자 검토 → Pipeline 등록` 순서로 처리합니다.
- 생성된 SQL은 읽기 전용입니다. 입력을 변경하면 기존 미리보기와 검토 상태가 무효화됩니다.
- 신규 API가 아직 없으면 화면은 준비 중 안내를 표시하고 입력값을 유지합니다.

## 1. Plan·SQL 미리보기 생성

`POST /api/v1/platform/pipeline/pipelines/ai-sql/preview`

```json
{
  "ownerUserId": 1,
  "pipelineName": "상품별 주문 집계",
  "description": "상품별 주문 건수와 총금액",
  "pipelineType": "AI_SQL",
  "inputTopicId": 1,
  "naturalLanguageRequest": "취소된 주문을 제외하고 상품별 주문 건수와 총금액을 집계해줘."
}
```

| 필드 | MVP 계약 |
| --- | --- |
| `ownerUserId` | 인증 전 개발 사용자 ID. 존재 여부 검증 |
| `pipelineName` | trim 후 1~100자 |
| `description` | 선택, 최대 1,000자 |
| `pipelineType` | 항상 `AI_SQL` |
| `inputTopicId` | 유효한 Topic ID 하나. 배열이 아님 |
| `naturalLanguageRequest` | trim 후 1~4,000자 |

서버 처리:

1. 사용자와 Topic 존재 여부를 확인하고 Topic Schema를 서버에서 다시 조회합니다. Schema 원문은 클라이언트 요청을 신뢰하지 않습니다.
2. 자연어 요청과 Schema를 이용해 처리 단계를 구조화한 Pipeline Plan과 Flink SQL을 생성합니다.
3. 생성 SQL을 실제 실행 환경의 파서·플래너와 connector 기준으로 검증합니다. 이 단계에서는 Job을 제출하지 않습니다.
4. 정규화한 요청, 사용자 ID, Topic Schema fingerprint, Plan, SQL, 유효기간을 `previewId`에 연결해 보관합니다.
5. 모호하거나 실행할 수 없는 요청은 가능한 범위의 설명과 함께 `validation.valid=false` 및 오류 목록을 반환합니다.

응답은 기존 `BaseResponse.body` 형식을 유지합니다.

```json
{
  "status": 200,
  "message": "success",
  "timestamp": "2026-09-24T12:00:00+09:00",
  "body": {
    "previewId": "server-generated-uuid",
    "expiresAt": "2026-09-24T12:15:00+09:00",
    "pipelinePlan": {
      "summary": "취소 주문을 제외한 상품별 집계",
      "steps": [
        { "title": "입력", "description": "orders Topic을 읽습니다." },
        { "title": "필터", "description": "취소 상태 주문을 제외합니다." },
        { "title": "집계", "description": "상품별 주문 건수와 총금액을 계산합니다." }
      ]
    },
    "generatedSql": "실제 생성 및 검증된 Flink SQL 문자열",
    "warnings": [],
    "validation": { "valid": true, "errors": [] }
  }
}
```

프론트는 `validation.valid=true`, 빈 오류 목록, 남아 있는 유효기간을 모두 확인한 뒤 사용자의 검토 확인을 받아 등록 버튼을 활성화합니다.

## 2. 검토한 AI_SQL Pipeline 등록

`POST /api/v1/platform/pipeline/pipelines/ai-sql`

미리보기 요청과 같은 필드에 `previewId`만 추가합니다. 클라이언트는 Plan 또는 SQL 원문을 다시 보내지 않습니다.

```json
{
  "ownerUserId": 1,
  "pipelineName": "상품별 주문 집계",
  "description": "상품별 주문 건수와 총금액",
  "pipelineType": "AI_SQL",
  "inputTopicId": 1,
  "naturalLanguageRequest": "취소된 주문을 제외하고 상품별 주문 건수와 총금액을 집계해줘.",
  "previewId": "server-generated-uuid"
}
```

서버 처리:

1. preview의 사용자, 만료 여부, 검증 결과, 요청 fingerprint가 등록 요청과 일치하는지 확인합니다.
2. Topic Schema가 미리보기 이후 변경됐으면 등록을 거부하고 재생성을 요구합니다.
3. Pipeline, 입력 Topic 연결, 자연어 요청, 서버에 보관한 Plan·SQL을 한 트랜잭션으로 저장합니다.
4. 같은 사용자와 `previewId`의 재요청은 같은 `pipelineId`를 반환하도록 멱등성을 보장합니다.
5. 등록 시 바로 실행하지 않고 `DRAFT` 상태로 저장합니다. 배포는 별도 API 흐름입니다.

응답은 기존 `PipelineResponse.Pipeline`을 `BaseResponse.body`에 반환합니다. 양수 `pipelineId`, `ownerUserId`, `pipelineName`, `pipelineType: AI_SQL`, `pipelineStatus: DRAFT`가 필요합니다.

## 3. 저장 구조 제안

기존 `platform.pipeline`의 `natural_language_request`, `pipeline_plan_json`, `generated_sql`을 사용합니다.

- 입력 Topic: `pipeline_input_topic` 같은 연결 테이블에 `pipeline_id`, `topic_id`, 검증 시점 Schema fingerprint를 저장하고 AI_SQL Pipeline당 한 건만 허용합니다. 또는 AI_SQL 전용 설정 테이블에 단일 `input_topic_id` FK를 둘 수 있습니다.
- preview: TTL, 사용자, 요청 fingerprint, Schema fingerprint, Plan·SQL, 생성된 `pipelineId`를 보관합니다. 등록 멱등성 기록은 preview 만료 이후에도 필요한 기간 동안 유지합니다.
- 상세 조회: AI_SQL 상세 응답에는 최소한 `inputTopicId`, `naturalLanguageRequest`, `pipelinePlan`, `generatedSql`을 제공하는 것이 좋습니다.

## 4. 오류 계약

프론트는 HTTP 실패 응답의 `message`를 표시합니다. 실패를 HTTP 200으로 감싸지 않습니다.

- 400: 필수값 누락, Topic Schema 부재, SQL 검증 실패 등 사용자가 수정할 수 있는 오류
- 404/405/501: API 미구현. 프론트는 준비 중 안내 표시
- 409: 요청 또는 Topic Schema가 preview와 달라짐. 재생성 안내
- 410: preview 만료. 재생성 안내
- 5xx: 생성 모델 또는 실행 환경 오류

## 5. 백엔드 MVP 완료 기준

1. 유효한 사용자, 단일 Topic, 자연어 요청으로 미리보기 생성 및 Pipeline 저장이 가능해야 합니다.
2. Topic 미선택, 존재하지 않는 Topic, Schema 미등록, 유효하지 않은 생성 SQL은 명확한 오류로 거부해야 합니다.
3. 요청·사용자·Schema가 바뀌었거나 만료된 preview의 등록을 거부해야 합니다.
4. 동일 preview 재시도 시 Pipeline 중복 생성이 없어야 하며, 일부 저장 실패 시 전체가 롤백되어야 합니다.
5. 인증·인가 연동 시 `ownerUserId` 요청값은 제거하거나 무시하고 principal로 소유자를 확정해야 합니다.

시간 기준(Event/Processing Time, Watermark, 시간대), 결과 저장소·테이블 설정, 시작 지점·병렬도 같은 고급 실행 설정은 향후 고도화 API로 별도 설계합니다.
