# 배포 준비 검증 기록

검증일: 2026-10-16 (Asia/Seoul)

## 통과

- 프론트엔드 TypeScript/Vite 운영 빌드와 기존 프론트엔드 테스트 전체.
- Linux Docker 이미지 빌드: 프론트엔드 Node 22 → Nginx, 백엔드 Java 24 → Spring Boot executable JAR.
- 배포 설정 검사 단위 테스트 6개.
- HTTP Compose 4개 조합: 원격/기존 Docker 인프라 × 전체 인터페이스/로컬 수신 주소.
- 읽기 전용 파일시스템·권한 제한 상태의 Nginx 설정 검사.
- 서버에 게시한 HTTP 포트에서 `/healthz` 200 응답과 본문 확인.
- Linux Bash 배포 스크립트 문법 검사.
- 격리된 PostgreSQL 16에서 수정한 V2 DDL 및 읽기 전용 스키마 검사.
- 기존 AI 설정 고유키가 없을 때 배포 검사 실패 및 선택적 마이그레이션 적용 후 통과.
- BCrypt 관리자 생성, 중복 관리자 생성 시 트랜잭션 실패.
- 운영 프로필 시작, DB를 포함한 readiness 성공.
- 프론트엔드 프록시를 통한 JWT 로그인 및 인증된 사용자 API 요청.
- 익명 보호 API 거부, 외부 actuator 차단, SPA 직접 경로와 누락 asset 처리.
- API 컨테이너 재생성 후 업로드 볼륨 파일 보존 및 프록시 연결 복구.
- API 일시 중지 → PostgreSQL custom dump·업로드 압축 → API 재시작 및 인증 API 복구.

검증은 고유 이름의 임시 프로젝트·DB·네트워크를 사용했으며 해당 임시 컨테이너와 볼륨은 종료 시 정리했습니다. 실제 서버의 DB·Kafka·Flink에는 접속하지 않았습니다.

## 대상 서버에서 확인할 사항

- 실제 서버 OS·아키텍처, 메모리·디스크, Docker 및 Compose 설치.
- `.env`의 실제 인프라 주소·계정, Atlas 설정과 HTTP 수신 포트.
- 기존 DB에 `check-schema.sql`을 실행한 결과 및 기존 업로드 파일 경로.
- 실제 Kafka `advertised.listeners`, Flink REST/SQL Gateway와 connector JAR, Atlas 연결.
- 서버 IP와 HTTP 수신 주소·포트, 방화벽·보안 그룹 및 외부 PC에서의 접속.
- 기존 Pipeline에 영향을 주지 않는 테스트 Pipeline으로 배포·결과 조회.
- Java 24 지원 종료에 따른 LTS 전환 계획. 기존 springdoc 2.x는 Spring Boot 4 공식 호환 범위 밖이며 운영 프로필은 API 문서를 비활성화합니다.

Windows 호스트의 `gradlew test`는 기존 Gradle Worker 클래스 로딩 오류로 완료하지 못했습니다. Linux Docker에서는 백엔드 컴파일·패키징과 운영 컨테이너 시작·인증·DB 연결을 확인했습니다. 외부 환경을 요구하는 기존 백엔드 테스트 전체의 통과를 주장하지 않습니다.
