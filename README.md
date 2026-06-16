# StreamCell
## 개요
StreamCell은 Kafka, Flink, Platform API Server로 구성된 스트리밍 플랫폼입니다.<br>
사용자는 Platform API Server를 통해 AI를 이용하여 자동화된 실시간 파이프라인을 생성(Flink SQL)하거나
직접 파이프라인 Job을 개발하여 배포할 수 있습니다.
즉, <b>StreamCell은 간단한 분석은 AI에이전트를 통한 Flink SQL 방식으로, 복잡한 실시간 처리는 개발자가 직접 Flink Job을 개발하는 방식으로 배포하는<br> AI-assisted 실시간 스트리밍 파이프라인 플랫폼입니다.</b>

## 목표 및 핵심기능
사용자 권한에 따라 특정 Kafka Topic에 접근하고 제어할 수 있습니다.<br>
자연어 기반으로 Flink SQL 분석 파이프라인을 생성합니다.<br>
사용자가 직접 작성한 Flink Job JAR 파일을 업로드하고 배포할 수 있도록 합니다.<br>
사용자가 등록한 파이프라인의 실행, 중지, 상태 모니터링을 지원합니다.<br>
실시간 처리 결과를 PostgreSQL에 저장하고, 대시보드에서 확인할 수 있도록 합니다.<br>

## 기술스택

## 아키텍처

## 프로젝트 구조

## 로컬 실행 방법

## 향후 고도화 계획
### Flink
1차 -> Flink Session Cluster<br>
2차 -> Kubernetes + Flink Kubernetes Operator + Application Mode