# StreamCell Web

React + TypeScript 기반의 StreamCell 관리 콘솔입니다. 로그인은 Spring Security JWT API를 사용합니다.

## 실행

```powershell
cd frontend
npm install
npm run dev
```

개발 서버는 기본적으로 `/api` 요청을 `http://localhost:8085`으로 프록시합니다. 배포 환경에서는 `.env.example`을 복사한 뒤 `VITE_API_BASE_URL`에 API origin을 지정하세요.

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
