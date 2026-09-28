# AI_SQL 프론트엔드 API 연동

AI_SQL Pipeline 등록 화면은 다음 세 API를 순서대로 사용합니다.

1. Pipeline Plan과 Flink SQL 미리보기 생성
2. 기본 Pipeline 생성
3. 생성된 Pipeline에 검토한 AI_SQL 정보 등록

사용자는 소유자를 직접 입력하지 않습니다. 인증 연동 전에는 현재 개발 사용자 ID를 `userId`와 `ownerUserId`에 사용합니다.

## 1. 미리보기 생성

`POST /api/v1/platform/pipeline/pipelines/ai-sql/preview`

요청:

```json
{
  "inputTopicId": 1,
  "userId": 1,
  "naturalLanguageRequest": "5분마다 상품별 주문 건수와 평균 결제금액을 계산해줘."
}
```

응답의 `body`:

```json
{
  "pipelinePlan": {
    "sourceTopicId": 1,
    "window": {
      "type": "TUMBLE",
      "size": 5,
      "unit": "MINUTE"
    },
    "groupBy": ["product_id"],
    "aggregations": [
      {
        "function": "COUNT",
        "field": "*",
        "alias": "order_count"
      }
    ],
    "filters": [
      {
        "field": "payment_amount",
        "operator": "GTE",
        "value": 10000
      }
    ]
  },
  "generatedFlinkSql": "SELECT ..."
}
```

프론트는 Pipeline Plan을 입력 Topic, Window, 그룹 기준, 필터, 집계 항목으로 나누어 표시합니다. `generatedFlinkSql`은 읽기 전용 SQL 미리보기로 표시합니다. 선택한 Topic과 응답의 `sourceTopicId`가 다르거나 필수 Plan 구조가 없으면 등록을 허용하지 않습니다.

## 2. 기본 Pipeline 생성

사용자가 Plan과 SQL을 확인하고 등록 버튼을 누르면 먼저 기본 Pipeline을 생성합니다.

`POST /api/v1/platform/pipeline/pipelines`

```json
{
  "ownerUserId": 1,
  "pipelineName": "상품별 주문 집계",
  "description": "상품별 주문 건수와 평균 결제금액",
  "pipelineType": "AI_SQL"
}
```

응답의 `body.pipelineId`를 다음 AI 정보 등록 API의 경로에 사용합니다.

## 3. AI_SQL 정보 등록

`POST /api/v1/platform/pipeline/pipelines/{pipelineId}/ai-sql`

```json
{
  "inputTopicId": 1,
  "userId": 1,
  "naturalLanguageRequest": "5분마다 상품별 주문 건수와 평균 결제금액을 계산해줘.",
  "pipelinePlan": {
    "sourceTopicId": 1,
    "window": {
      "type": "TUMBLE",
      "size": 5,
      "unit": "MINUTE"
    },
    "groupBy": ["product_id"],
    "aggregations": [
      {
        "function": "COUNT",
        "field": "*",
        "alias": "order_count"
      }
    ],
    "filters": []
  }
}
```

미리보기 응답에서 사용자가 확인한 `pipelinePlan` 객체를 변경하지 않고 전송합니다. SQL 문자열은 현재 `CreateAISqlConfig` DTO에 포함되지 않으므로 이 요청에는 보내지 않습니다.

기본 Pipeline 생성 후 AI 정보 등록만 실패하면 프론트는 생성된 `pipelineId`를 보존합니다. 같은 Pipeline에 AI 정보 등록만 다시 시도하므로 중복 Pipeline 생성을 방지합니다.

## 4. AI_SQL 배포

Pipeline 상세 화면에서 `AI SQL 배포` 버튼을 누르면 다음 API를 호출합니다.

`POST /api/v1/platform/pipeline/pipelines/deployment/{pipelineId}/ai-sql/deploy`

요청 본문은 없습니다. 프론트는 백엔드의 `PipelineDeploymentPolicy`에 맞춰 AI_SQL Pipeline 상태가 `DRAFT` 또는 `CREATED`일 때만 버튼을 활성화합니다.

성공 응답의 `body`는 `pipelineId`, `deploymentId`, `flinkJobId`, `status`를 포함하는 Deployment 응답으로 처리합니다. AI_SQL 배포에는 JAR가 없으므로 `flinkJarId`는 없어도 됩니다. 배포 요청 후 Pipeline 상세와 목록을 다시 조회해 최신 상태를 표시합니다.

## 백엔드 확인 사항

현재 `CreateAISqlConfig` DTO에는 `pipelinePlan`이 있지만 서비스 구현에서는 전달된 Plan을 `pipeline_plan_json`에 설정하는 처리가 확인되지 않습니다. 또한 미리보기의 `generatedFlinkSql`은 AI 정보 등록 DTO에 포함되지 않습니다. 사용자가 검토한 Plan·SQL을 그대로 저장해야 한다면 백엔드에서 두 값을 저장하는 계약과 구현을 추가해야 합니다.

AI 정보 등록 처리 중 DB 반영 후 응답만 실패한 경우에도 안전하게 재시도할 수 있도록 `(pipelineId)` 기준 upsert 또는 멱등 처리를 권장합니다.

## 인증 연동 시 변경할 부분

Spring Security 적용 후에는 백엔드가 principal에서 사용자를 확정하는 것이 안전합니다.

- 기본 Pipeline 생성의 `ownerUserId`
- 미리보기와 AI 정보 등록의 `userId`

인증 적용 후에도 프론트가 값을 보내야 한다면 서버에서 principal과 일치하는지 반드시 확인해야 합니다.
