# StreamCell Web

React + TypeScript 기반의 StreamCell 관리 콘솔입니다. 로그인은 Spring Security JWT API를 사용합니다.

## 실행

```powershell
cd frontend
npm install
npm run dev
```

개발 서버는 기본적으로 `/api` 요청을 `http://localhost:8085`으로 프록시합니다. 배포 환경에서는 `.env.example`을 복사한 뒤 `VITE_API_BASE_URL`에 API origin을 지정하세요.

## 결과 Dashboard 예시 데모

개발 서버 실행 후 `http://localhost:5173/demo/results`를 열면 로그인과 백엔드 없이 결과 Dashboard를 확인할 수 있습니다.

- AI_SQL 주문 집계 / CUSTOM_JAR 세션 분석 Pipeline 선택
- 5초마다 샘플 1분 윈도우 추가, 자동 갱신 일시정지·재개 및 수동 갱신
- 지표·그룹 선택에 따라 동일한 결과 데이터로 Table과 Chart 갱신
- 빈 결과 화면과 제안된 `columns`, `rows`, `chart` 형태의 샘플 API 응답 확인

모든 수치는 시뮬레이션 데이터이며 실제 백엔드 호출은 발생하지 않습니다.

## 결과 Dashboard API 연결

로그인 후 **결과 Dashboard** 메뉴에서 Pipeline을 선택하거나 Pipeline 상세의 **결과 Dashboard →** 버튼으로 이동합니다.

`GET /api/v1/platform/pipeline/pipelines/{pipelineId}/results?limit=100&rangeMinutes=20`을 기존 JWT 클라이언트로 호출합니다. 조회 기간과 최대 결과 행을 변경할 수 있으며, 화면을 보고 있는 동안 5초 간격으로 자동 조회합니다. 일시정지·수동 새로고침도 제공합니다.

- `body` 또는 `data` 응답 래퍼와 직접 반환된 결과 객체를 지원합니다.
- `columns: [{ key, label, type }]`로 Table의 컬럼을 구성합니다. 조회 결과는 12행씩 페이지를 전환할 수 있습니다.
- `rows: [{ ...컬럼별 값 }]`를 Table과 Chart의 공통 데이터로 사용합니다. null 값은 `—`로 표시합니다.
- `chart: { xKey, series: [{ key, label, groupKeys: ["productId", "region"], unit? }] }`로 지표 선택과 다중 그룹 조합별 선을 구성합니다. `groupKeys: []`는 전체 집계입니다.
- 누락된 차트 값은 0으로 만들지 않고 선을 끊습니다. 시간 X축은 오름차순으로 표시하며 Table은 최신순으로 표시합니다.
- `pipelineId`, `pipelineStatus`, `updatedAt`은 선택 항목입니다. chart 설정이 없어도 Table 조회는 가능합니다.
- 조회 오류, 빈 결과, 최대 행 도달을 별도로 표시합니다. 실패 시 샘플 데이터로 대체하지 않습니다.

현재 체크아웃에는 결과 API의 백엔드 구현이 없어 위 응답 규격을 기준으로 연결했습니다. 실제 응답의 필드명이 다르면 매핑을 맞춰야 합니다.

## JWT 로그인

`npm run dev`에서도 로그인 화면이 표시됩니다. 백엔드가 실행 중이어야 로그인할 수 있습니다.

`POST /api/v1/web/auth/login` 요청:

```json
{ "loginId": "streamcell-user", "password": "password" }
```

응답의 `body.accessToken`을 메모리에 보관하며 이후 API 요청에 `Authorization: Bearer <token>`을 붙입니다. 페이지를 새로고침하거나 로그아웃하면 메모리의 토큰이 사라져 다시 로그인해야 합니다. 보호된 API가 401을 반환해도 로그인 화면으로 돌아갑니다.

현재 로그인 응답에는 사용자 ID가 없으므로 토큰을 받은 뒤 `GET /api/v1/web/user/items`에서 `loginId`가 일치하는 사용자를 찾아 소유 사용자 ID로 사용합니다. 인증된 화면에서 사용자 ID를 임의로 바꾸는 선택 UI는 표시하지 않습니다. 백엔드에 `/me` API가 추가되면 사용자 목록 조회를 그 API로 교체하는 것이 좋습니다.

로컬에서 인증 없이 UI만 점검해야 할 때는 `.env.development`의 `VITE_AUTH_ENABLED=false`로 설정할 수 있습니다.

## Management Console 데모 범위

- Flink Cluster, TaskManager, Slot 및 실행 중 Job Dashboard
- Kafka Topic Sync, Schema 편집 및 Event Time 설정
- Topic별 사용자 권한 조회·추가와 내 사용 가능 Topic
- AI_SQL 자연어 요청, Pipeline Plan 및 Flink SQL Preview
- Custom JAR, Entry Class, Topic, Parallelism 및 Program Arguments 입력
- Pipeline 실행·중지, 상태와 Deployment 이력
- 실시간 처리 결과 Table·Chart Dashboard
- 원본 Exception, AI 원인 분석 및 권장 조치
