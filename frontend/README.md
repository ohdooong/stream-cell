# StreamCell Web

React + TypeScript 기반의 StreamCell 관리 콘솔입니다. 인증은 Spring Security API를 소유한 백엔드와 연동하며, 프론트에는 권한 판정이나 비밀값을 저장하지 않습니다.

## 실행

```powershell
cd frontend
npm install
npm run dev
```

개발 서버는 기본적으로 `/api` 요청을 `http://localhost:8085`으로 프록시합니다. 배포 환경에서는 `.env.example`을 복사한 뒤 `VITE_API_BASE_URL`에 API origin을 지정하세요.

## 인증 연동 계약

기본 로그인 요청은 `POST /api/v1/auth/login`이며 본문은 아래입니다.

```json
{ "username": "name@example.com", "password": "password", "rememberMe": true }
```

Spring Security의 세션 쿠키 방식이면 응답에 `Set-Cookie`를 설정하면 됩니다. JWT 방식이면 응답 JSON의 `accessToken`, `access_token`, 또는 `token` 속성을 반환하세요. 토큰은 페이지 세션 동안에만 메모리에 유지되고, 이후 API 호출의 `Authorization: Bearer` 헤더에 자동으로 붙습니다.

로그인 상태 확인은 `GET /api/v1/auth/me`, 로그아웃은 `POST /api/v1/auth/logout`입니다. `/me` 응답은 `{ "userId": 1, "username": "...", "displayName": "...", "roles": ["ROLE_USER"] }` 형태를 권장합니다. 실제 엔드포인트가 달라질 경우 환경 변수의 경로만 바꾸면 됩니다.
