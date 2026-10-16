# 기존 인프라에 프론트엔드·백엔드 배포

PostgreSQL, Kafka, Flink가 **이미 실행 중인 Linux 서버**에 배포하는 구성입니다. 기존 인프라 컨테이너와 데이터를 생성·변경·재시작하지 않습니다. 이 디렉터리는 `infra/docker-compose.yml`과 별개의 Compose 프로젝트입니다.

```text
브라우저 → HTTP 서버IP:8088 → frontend Nginx :8080
                                └ /api/* → backend :8085
                                               ├ 기존 PostgreSQL
                                               ├ 기존 Kafka
                                               ├ 기존 Flink REST / SQL Gateway
                                               └ Atlas
```

접속 주소는 `http://서버IP:8088`입니다. 프론트엔드는 `/api` 상대 경로를 사용하므로 별도 CORS 설정이 필요 없습니다. 로그인 활성화·데모 비활성화는 이미지 빌드 시 고정됩니다. Nginx가 HTTP 요청을 받고 API 요청을 내부 백엔드로 전달합니다.

## 서버 준비

- Docker Engine, Compose v2.24 이상, BuildKit, Python 3.9 이상, Bash, Git, curl이 필요합니다. 서버에 Node나 JDK를 설치할 필요는 없습니다.
- 앱 컨테이너 기본 메모리 한도 합계는 약 1.2GB입니다. 이미지 빌드를 위한 추가 메모리·디스크와 **기존 Kafka/Flink/DB 사용량**을 함께 확보하세요. 빌드가 부담되면 별도 Linux 빌드 환경에서 이미지를 만들고 레지스트리를 통해 전달할 수 있습니다.
- 접속할 PC에서 서버의 TCP 8088 포트에 접근할 수 있도록 서버 방화벽과 클라우드 보안 그룹을 설정하세요. 다른 포트를 쓰려면 `FRONTEND_HTTP_PORT`를 변경합니다.
- SSH는 관리 주소에만 허용하고 DB/Kafka/Flink 관리 포트의 기존 방화벽·포트 바인딩도 확인하세요. Docker 그룹과 Docker 소켓 접근 권한은 서버 관리자 수준으로 취급합니다.

## 1. 서버별 설정

현재 작업 폴더에는 아직 커밋하지 않은 프론트엔드 변경도 포함될 수 있습니다. 서버에서 Git으로 배포한다면 필요한 작업을 검토·커밋·전달한 뒤 해당 커밋을 사용하세요. 현재 소스 그대로 전송하려면 저장소 루트에서 배포 묶음을 만들 수 있습니다.

```bash
python3 infra/production/package-release.py
scp .artifacts/streamcell-deploy.tar.gz .artifacts/streamcell-deploy.tar.gz.sha256 user@SERVER:~/

# 서버에서 최초 설치 또는 새로운 릴리스 디렉터리로 전개
sha256sum -c ~/streamcell-deploy.tar.gz.sha256  # 두 파일이 있는 디렉터리에서 실행
mkdir -p ~/streamcell/releases/20261016-1
tar -xzf ~/streamcell-deploy.tar.gz -C ~/streamcell/releases/20261016-1
cd ~/streamcell/releases/20261016-1/streamcell-deploy
```

묶음에는 현재 프론트엔드·백엔드 소스, 배포 도구와 DDL이 들어갑니다. 실제 `.env`, `.git`, `node_modules`, 빌드 산출물, 로그와 백업은 제외합니다. 운영 서버 주소·계정이 들어 있는 별도 사용자 작성 소스 파일까지 자동으로 판별하지는 않으므로 전달 전에 검토하세요. 새 버전을 묶을 때는 다른 `--output` 경로를 지정하거나 이전 묶음을 별도 보관하세요.

저장소 루트에서 실행합니다.

```bash
python3 infra/production/init-env.py
chmod 600 infra/production/.env
editor infra/production/.env
```

`init-env.py`는 64바이트 난수의 Base64 JWT 키를 생성하고 기존 `.env`를 덮어쓰지 않습니다. **기존 DB 비밀번호를 새로 생성하거나 변경하지 않습니다.** `.env`는 Git 및 Docker 빌드 컨텍스트에서 제외됩니다.

업데이트 때는 기존 `.env`를 보호된 방식으로 새 릴리스 디렉터리에 가져오고 `IMAGE_TAG`만 새 값으로 변경합니다. `COMPOSE_PROJECT_NAME`은 유지해야 같은 업로드 볼륨을 계속 사용합니다. JWT 키도 매번 새로 만들지 않습니다.

필수 입력값:

| 설정 | 입력할 값 |
| --- | --- |
| `IMAGE_TAG` | 프론트엔드·백엔드 이미지에 붙일 배포 버전. 첫 배포 예: `20261016-1` |
| `FRONTEND_HTTP_PORT` | 브라우저에서 접속할 서버 포트. 기본 `8088` |
| `FRONTEND_BIND_ADDRESS` | 서버의 수신 주소. 기본 `0.0.0.0`으로 외부 HTTP 접속 허용 |
| `POSTGRES_URL` | API 컨테이너에서 접근 가능한 `jdbc:postgresql://주소:포트/DB` |
| `POSTGRES_USERNAME`, `POSTGRES_PASSWORD` | 기존 DB의 실제 계정과 비밀번호 |
| `FLINK_JDBC_URL` | **Flink 컨테이너**에서 접근 가능한 PostgreSQL JDBC URL |
| `KAFKA_BOOTSTRAP_SERVERS` | API와 Flink 모두 접근 가능한 Kafka `호스트:포트` 목록 |
| `FLINK_JOB_MANAGER_URL` | Flink REST URL, 예: `http://flink-jobmanager:8081` |
| `FLINK_SQL_GATEWAY` | SQL Gateway URL, 예: `http://flink-sql-gateway:8083` |
| `ATLAS_URL`, `ATLAS_AGENT_ID`, `ATLAS_API_KEY` | 기존 Atlas 주소·에이전트·키 |
| `INFRA_NETWORK` | 같은 Docker 호스트의 기존 인프라 네트워크 이름 |

### IMAGE_TAG 입력 방법

`IMAGE_TAG`는 배포 버전을 구분하는 이름입니다. 첫 배포에는 예시 파일의 `20261016-1`을 그대로 사용해도 됩니다.

```dotenv
IMAGE_PREFIX=streamcell
IMAGE_TAG=20261016-1
```

`deploy.sh deploy`를 실행하면 다음 두 이미지를 이 이름으로 빌드합니다.

```text
streamcell/frontend:20261016-1
streamcell/backend:20261016-1
```

수정한 코드를 다시 배포할 때는 `20261016-2`, 다음 날짜 첫 배포는 `20261017-1`처럼 새 태그를 사용하세요. `v1.0.0`이나 검토한 Git 커밋의 짧은 SHA도 사용할 수 있습니다. 영문·숫자·점·밑줄·하이픈으로 작성하며 공백과 `/`는 넣지 않습니다. 이전 태그의 이미지를 남겨 두면 그 버전으로 롤백할 수 있습니다. 별도 빌드 호스트나 레지스트리에서 이미지를 받아 `up`으로 시작할 때는 전달받은 이미지와 같은 태그를 입력합니다.

프로덕션은 PostgreSQL 드라이버를 직접 사용합니다. 개발용 `jdbc:log4jdbc:postgresql://...` URL을 넣지 마세요. API와 Flink의 JDBC URL은 달라도 되지만, 현재 애플리케이션은 두 연결에 **같은 DB 계정·비밀번호**를 사용합니다. 해당 계정에 기존 `web`, `platform` 접근 권한과 Flink 결과 테이블 생성·쓰기 권한이 필요합니다.

비밀번호에 `$`가 있으면 Compose 보간을 막도록 `.env`에서 작은따옴표로 감싸세요. 예: `POSTGRES_PASSWORD='literal$secret'`. `.env`를 셸에서 `source`할 필요가 없습니다. `VITE_*`는 브라우저 코드에 노출되므로 비밀값을 넣지 않습니다.

### 같은 서버의 기존 Docker 인프라

기본 설정을 사용합니다.

```dotenv
COMPOSE_FILE=compose.yaml:compose.infra.yaml
INFRA_NETWORK=streamcell-net
```

실제 이름은 서버에서 확인합니다.

```bash
docker network ls
docker network inspect streamcell-net
```

PostgreSQL, Kafka, Flink 컨테이너가 해당 네트워크에 연결되어 있고 설정한 서비스 이름으로 조회되어야 합니다. 이 저장소의 기존 인프라는 `streamcell-net`을 사용합니다. 다른 네트워크를 사용한다면 `INFRA_NETWORK`를 변경하세요. **동일 호스트라도 다른 네트워크의 서비스 이름은 자동으로 조회되지 않습니다.**

### 별도 서버의 기존 인프라

`COMPOSE_FILE`에서 `compose.infra.yaml`을 제거하고, 인프라 주소를 실제 사설 DNS/IP로 변경합니다.

```dotenv
COMPOSE_FILE=compose.yaml
POSTGRES_URL=jdbc:postgresql://10.20.0.10:5432/streamcell
```

컨테이너의 `localhost`는 호스트 서버가 아닙니다. 호스트에서 직접 실행 중인 서비스는 Docker 브리지에서 접근 가능한 호스트 사설 IP로 연결하고 방화벽에서 필요한 범위만 허용하세요.

Kafka는 bootstrap 접속 후 브로커의 `advertised.listeners`로 재접속합니다. 브로커가 `localhost:19092` 등을 광고하면 API/Flink에서 동작하지 않습니다. 같은 Docker 네트워크에서는 `kafka-1:9092` 등의 내부 리스너, 원격에서는 양쪽에서 접근 가능한 광고 주소를 사용합니다. 현재 SQL 생성기는 Spring Kafka 설정의 bootstrap 주소를 사용하며, SASL/TLS Kafka 옵션을 생성 SQL에 전달하지 않습니다. SASL/TLS가 필수인 클러스터는 해당 기능을 추가한 뒤 AI_SQL을 사용해야 합니다.

### HTTP 접속 주소와 포트

기본값은 서버의 모든 IPv4 인터페이스에서 8088 포트로 요청을 받습니다.

```dotenv
FRONTEND_BIND_ADDRESS=0.0.0.0
FRONTEND_HTTP_PORT=8088
```

브라우저에서 `http://서버IP:8088`로 접속하세요. 포트를 `80`으로 변경했다면 `http://서버IP`로 접속할 수 있습니다. 서버에 이미 같은 포트를 사용하는 서비스가 있다면 다른 포트를 선택하세요. 특정 서버 주소에서만 받으려면 `FRONTEND_BIND_ADDRESS`를 해당 IPv4 주소로 설정합니다. `127.0.0.1`로 설정하면 서버 내부에서만 접속됩니다. 백엔드 8085는 Docker 내부에서만 사용하며 프론트엔드가 `/api` 요청을 전달합니다.

## 2. 기존 DB와 업로드 확인

기존 DB에 `sql/ddl/V2__init_schema.sql`에 해당하는 테이블·컬럼이 있어야 합니다. 이 배포는 SQL 초기화나 스키마 마이그레이션을 자동 실행하지 않습니다. `V1`에서 사용하던 DB는 별도의 검토된 마이그레이션이 필요합니다. V2의 `CREATE TABLE IF NOT EXISTS`는 기존 테이블 컬럼을 변경하지 않습니다.

다음 검사는 데이터를 변경하지 않고 필수 스키마 항목과 AI_SQL 등록용 고유키를 확인합니다. 실제 DB 접속 사용자·DB 이름으로 실행하세요.

```bash
docker exec -i streamcell-postgres psql -U streamcell -d streamcell \
  < infra/production/check-schema.sql
```

초기 V2 DDL의 AI 설정 테이블에는 스키마 이름이 빠진 외래키 참조와 API의 `ON CONFLICT`에 필요한 고유키가 누락되어 있어 이 저장소의 DDL을 수정했습니다. 이미 만들어진 DB는 이 수정으로 바뀌지 않습니다. 검사 결과 고유키만 누락된 경우, 백업과 검토 후 `migrations/001-ai-config-unique.sql`을 별도로 실행할 수 있습니다. 중복 데이터가 있거나 잠금을 빨리 얻지 못하면 실패하며 데이터를 자동 삭제하지 않습니다.

V2 파일에는 **평문 비밀번호의 개발용 사용자**가 포함되어 있어 운영 초기화용으로 통째로 실행하면 안 됩니다. 현재 로그인은 BCrypt를 요구합니다. 이미 BCrypt 비밀번호를 가진 운영 계정이 있다면 그대로 사용하세요.

새 관리자가 필요한 경우에만 `bootstrap-admin.sql`을 사용합니다. 먼저 비밀번호를 인자로 노출하지 않고 해시를 생성합니다.

```bash
docker run --rm -it httpd:2.4-alpine htpasswd -nBC 12 admin
# 두 번 비밀번호 입력 → 출력의 'admin:' 뒤에 있는 $2y$... 해시를 복사

read -r -p '새 관리자 로그인 ID: ' ADMIN_LOGIN_ID
read -r -p '새 관리자 이메일: ' ADMIN_EMAIL
read -r -s -p 'BCrypt 해시: ' ADMIN_PASSWORD_HASH
printf '\n'
export ADMIN_LOGIN_ID ADMIN_EMAIL ADMIN_PASSWORD_HASH

# 실제 PostgreSQL 컨테이너 이름 및 관리 접속 사용자/DB로 변경
docker exec -i \
  -e ADMIN_LOGIN_ID -e ADMIN_EMAIL -e ADMIN_PASSWORD_HASH \
  streamcell-postgres psql -U streamcell -d streamcell \
  < infra/production/bootstrap-admin.sql

unset ADMIN_LOGIN_ID ADMIN_EMAIL ADMIN_PASSWORD_HASH
```

스크립트는 기존 계정의 비밀번호를 변경하지 않습니다. 같은 로그인 ID/이메일이 있으면 트랜잭션이 실패합니다. 원격 DB에서는 환경변수를 전달받은 `psql` 클라이언트로 동일 SQL을 실행하세요. 공개 운영 전에 기존 개발 계정의 삭제·비활성화 또는 비밀번호 교체도 별도 검토하세요.

기존 API에서 업로드한 파일이 있다면 새 볼륨에 파일을 옮기고 DB에 저장된 경로도 확인해야 합니다. 새 컨테이너 경로는 `/data/uploads/flink/artifacts`입니다. 현재 파일 서비스는 DB에 절대 경로를 저장하므로 **볼륨만 복사하면 기존 Windows/Linux 경로가 자동 변환되지 않습니다.** 기존 파일 경로를 유지하도록 볼륨·프로퍼티를 조정하거나 백업 후 검토한 경로 마이그레이션을 진행하세요. 빈 신규 API 배포라면 이 작업은 필요 없습니다.

## 3. 검사·빌드·시작

```bash
bash infra/production/deploy.sh check
bash infra/production/deploy.sh deploy
bash infra/production/deploy.sh status
```

`check`는 비밀값을 출력하지 않고 필수값, JWT 키 길이, 운영 JDBC 형식, Kafka 주소, 릴리스 태그, Docker 엔진과 외부 네트워크 존재 여부를 검사합니다. 실제 자격 증명·네트워크 접속 성공까지 보장하지는 않습니다.

`deploy`는 두 이미지를 빌드하고, API readiness 성공 후 프론트엔드를 시작합니다. API readiness에는 DB 연결 검사가 포함됩니다. API는 일반 사용자로 실행되며 루트 파일시스템은 읽기 전용이고, 업로드 디렉터리만 영구 볼륨에 저장됩니다. 로그는 콘솔에 기록하고 Docker가 회전합니다. Atlas 키와 본문을 기록하는 기존 HTTP 로깅 인터셉터는 운영 프로필에서 비활성화합니다. 업로드 요청 전체 크기는 50MiB 이하여야 하므로 실제 파일은 multipart 오버헤드를 고려해 그보다 작게 사용하세요.

서버 외부의 접속할 PC에서도 HTTP 연결을 확인하세요. 아래 주소의 `SERVER_IP`와 포트는 실제 설정으로 바꿉니다.

```bash
curl --fail http://SERVER_IP:8088/healthz
curl --fail -I http://SERVER_IP:8088/
# 인증 없이 보호 API를 호출하면 401 또는 403이어야 합니다.
curl -s -o /dev/null -w '%{http_code}\n' http://SERVER_IP:8088/api/v1/web/user/items
# 운영 엔드포인트는 웹 프록시에서 차단되어 404여야 합니다.
curl -s -o /dev/null -w '%{http_code}\n' http://SERVER_IP:8088/actuator/health
```

브라우저에서 운영 계정 로그인 → Topic 동기화·조회 → 기존 Pipeline 상세·상태 → 테스트 Pipeline의 JAR 등록/배포 또는 AI SQL 생성 → 결과 조회를 확인하세요. 기존 운영 Pipeline을 검증 목적으로 중지하지 마세요. DB readiness가 정상이어도 Kafka/Flink/Atlas 연결이나 사용자 권한이 정상이라는 의미는 아닙니다.

### 이미지 전달로 배포

별도 빌드 호스트와 운영 서버 모두 같은 배포 설정을 사용하되 `.env`의 비밀값은 안전하게 각각 관리합니다. Linux 대상 아키텍처도 맞춰야 합니다.

```bash
# 빌드 호스트 .env: IMAGE_PREFIX=registry.company.com/streamcell
bash infra/production/deploy.sh build
cd infra/production
docker compose --env-file .env push backend frontend

# 운영 서버: 같은 IMAGE_PREFIX/IMAGE_TAG 설정 및 레지스트리 로그인 후
cd infra/production
docker compose --env-file .env pull backend frontend
cd ../..
bash infra/production/deploy.sh up
```

직접 `docker compose`를 실행할 때는 이 디렉터리에서 `--env-file .env`를 사용하세요. 파일에 들어 있는 `COMPOSE_FILE`이 필요한 옵션 파일을 선택합니다. 환경변수 `COMPOSE_FILE`, `COMPOSE_PROJECT_NAME`, `IMAGE_TAG`가 이미 셸에 설정되어 있으면 `.env`보다 우선하므로 확인하세요.

## 4. 업데이트·롤백

검토한 소스를 배포하고 `.env`의 `IMAGE_TAG`를 새 값으로 바꾼 뒤 `deploy`를 실행합니다. 기존 태그에 새 이미지를 덮어쓰지 마세요. 이전 실행 이미지 참조는 `.release-backend`, `.release-frontend`에 저장됩니다. 이전 이미지는 롤백 기간 동안 보존합니다.

```bash
bash infra/production/deploy.sh deploy

# 실패 시 이전 두 앱 이미지가 로컬에 남아 있는지 확인하고 이전 태그로 복귀
bash infra/production/deploy.sh rollback 20261016-1
# 이후 .env의 IMAGE_TAG도 복귀한 값으로 맞춤
```

현재 구성은 단일 인스턴스이므로 교체 중 잠깐 요청이 실패할 수 있습니다. 무중단 배포나 자동 롤백은 제공하지 않습니다. 롤백은 앱 이미지만 바꾸며 DB 스키마·데이터와 업로드 파일은 되돌리지 않습니다. API의 종료 유예는 45초이며, 진행 중 요청을 마무리하도록 graceful shutdown을 사용합니다.

## 5. 백업·복구

API를 잠시 중지할 수 있는 점검 시간에 실행합니다. 백업은 **API를 일시 중지하고** PostgreSQL custom dump와 업로드 볼륨 압축 파일을 생성한 뒤 API를 다시 시작합니다. 실행 중인 Flink가 DB에 계속 기록할 수 있으므로 파이프라인까지 같은 시점으로 복원해야 한다면 별도의 점검·savepoint 계획이 필요합니다.

```bash
python3 infra/production/backup.py
# PostgreSQL 서버가 16보다 최신이면 해당 버전의 pg_dump 이미지 지정
python3 infra/production/backup.py --postgres-image postgres:17-alpine
```

백업은 `infra/production/backups/<UTC시간>/`에 저장됩니다. `database.dump`, `uploads.tar.gz`, `manifest.json`이 모두 있고 `.partial`이 없어야 완료된 백업입니다. 다른 서버의 제한된 저장소로 복사하고 보관 기간을 정하세요. `.env`는 이 백업에 포함되지 않으므로 별도로 보호·보관합니다. 원격 DB에 사용자 지정 CA/클라이언트 인증서가 필요하면 해당 인증서를 마운트하는 백업 실행 환경을 준비하세요. 이 도구는 `sslmode`만 URL에서 전달합니다.

복구는 운영 DB 위에 바로 덮어쓰기보다 별도 DB/볼륨에서 먼저 검증합니다.

1. 동일 PostgreSQL 버전 이상 클라이언트의 `pg_restore --list database.dump`와 `tar -tzf uploads.tar.gz`로 파일을 검사합니다.
2. API와 관련 Flink 쓰기 작업을 점검 상태로 두고, 복구할 새 DB를 만듭니다.
3. 새 DB에 `pg_restore --no-owner --no-acl --dbname=<새 DB 연결정보> database.dump`를 실행합니다. 비밀번호는 `PGPASSWORD` 환경변수나 보호된 `.pgpass`로 전달합니다. 일반 DB 이름만 넣지 말고 올바른 호스트/사용자를 지정하세요.
4. 새 업로드 볼륨의 `/data/uploads`에 압축을 풀고 UID/GID `10001:10001`로 맞춥니다. Compose가 그 볼륨을 사용하도록 override 파일을 지정합니다. DB에 저장된 절대 경로도 함께 확인합니다.
5. `POSTGRES_URL`, `FLINK_JDBC_URL`을 복구 DB로 맞추고 시작한 뒤 로그인·기존 Artifact·결과 조회를 검증합니다. Flink job/savepoint 복구는 기존 스트리밍 운영 절차를 따릅니다.

기존 데이터를 삭제하는 `docker compose down -v`, `docker volume prune`, `pg_restore --clean`은 일반 배포·롤백 절차에 포함하지 않습니다.

## 점검·문제 해결

```bash
bash infra/production/deploy.sh status
bash infra/production/deploy.sh logs

cd infra/production
docker compose --env-file .env logs --tail=200 backend
docker compose --env-file .env exec -T backend \
  curl --fail --silent http://127.0.0.1:8085/actuator/health/readiness
docker compose --env-file .env exec -T frontend nginx -t
```

- DB 연결 실패: 컨테이너 기준 주소, 네트워크, 비밀번호, JDBC 접두어, PostgreSQL `pg_hba.conf`/TLS 설정을 확인합니다.
- 로그인 실패: V2 스키마, 계정의 BCrypt 해시, ADMIN/ANALYST/ENGINEER 역할, JWT Base64 키를 확인합니다. 기존 평문 seed 비밀번호는 BCrypt 로그인과 호환되지 않습니다.
- Kafka 동기화 실패: 내부 DNS와 `advertised.listeners`를 API/Flink 양쪽에서 확인합니다.
- AI SQL 배포 실패: Atlas 설정, SQL Gateway v2 endpoint, Flink에 설치된 Kafka/JDBC/PostgreSQL connector JAR, Flink에서 접근하는 sink DB 주소·권한을 확인합니다.
- 502/504: 백엔드 health와 로그를 확인합니다. Nginx는 Docker DNS를 주기적으로 다시 조회하므로 API 교체 후 새 IP로 연결합니다.
- JAR 경로 오류: 업로드 볼륨과 DB의 저장 경로를 함께 확인합니다.
- 외부 HTTP 접속 실패: 서버 IP, `FRONTEND_BIND_ADDRESS`, `FRONTEND_HTTP_PORT`, 기존 포트 점유와 방화벽·보안 그룹을 확인합니다.

## 검증 및 남은 운영 판단

```bash
python3 -m unittest discover -s infra/production/tests -v
python3 infra/production/tests/check_compose.py
python3 infra/production/preflight.py --config-only

# 프론트엔드
cd frontend
npm ci
npm run build
node --test tests/*.test.mjs  # Linux Bash

# 백엔드 (Java 24)
cd ../backend/streamcell-api/streamcell
./gradlew test bootJar
```

전체 백엔드 테스트에는 외부 DB/Flink 등을 요구하는 테스트가 있으므로 검증용 서비스와 환경변수를 사용해야 합니다. Dockerfile의 `bootJar` 빌드는 테스트를 실행하지 않습니다. 운영 비밀값을 CI 로그나 테스트 출력에 넣지 마세요.

Docker 이미지 빌드 후 다음 격리 검증을 선택적으로 실행할 수 있습니다. 고유한 임시 프로젝트·네트워크·DB를 만들고 종료 시 해당 자원만 정리합니다. 실제 인프라를 대상으로 실행하지 않으며, 임시 PostgreSQL 데이터와 백업 파일은 테스트 후 삭제합니다.

```bash
docker build -t streamcell-deployment-validation-backend backend/streamcell-api/streamcell
docker build -t streamcell-deployment-validation-frontend frontend
python3 infra/production/tests/smoke.py
```

이 검증은 V2 DDL·스키마 검사, BCrypt 관리자 생성과 중복 방지, 운영 컨테이너 시작·DB readiness, JWT 로그인·API 프록시, SPA 경로·비공개 actuator, 컨테이너 교체 후 업로드 보존, DB·파일 백업과 API 재시작을 확인합니다. Kafka/Flink/Atlas 연결 및 서버 외부에서의 HTTP 접근은 대상 서버에서 별도로 검증해야 합니다.

프로젝트의 기존 Gradle Java 24 설정과 일치하는 이미지를 사용합니다. Java 24는 장기 지원 버전이 아니며 업데이트 제공 기간이 종료되어 있으므로, 공개 운영 전에는 지원 중인 LTS JDK로의 전환을 권장합니다. Spring Boot 4와 함께 쓰는 기존 springdoc 2.x도 공식 호환 범위 밖이므로 호환되는 3.x 버전으로 전환하고 API 문서가 필요한 개발 환경에서 확인해야 합니다. 운영 프로필에서는 API 문서를 비활성화합니다. 이 배포 작업에서는 라이브러리·Java 버전을 일괄 변경하지 않습니다.

참고: [Compose 시작 순서와 healthcheck](https://docs.docker.com/compose/how-tos/startup-order/), [Spring Boot health endpoint](https://docs.spring.io/spring-boot/reference/actuator/endpoints.html), [Temurin 지원 기간](https://adoptium.net/support/), [springdoc 호환성](https://springdoc.org/).
