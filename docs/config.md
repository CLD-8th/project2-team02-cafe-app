# 설정 · 환경 변수

이미지는 하나. 환경마다 다른 값은 전부 환경 변수로 받음. **이미지에도, 이 저장소에도 비밀을 넣지 않음** (조직원 전원이 읽는 저장소).

## 01. 프로파일

| 프로파일 | DB | 사진 | 언제 |
| --- | --- | --- | --- |
| `local` (기본) | H2 메모리 | 디스크 `./data/menu-images` | 내 PC · 테스트 · JDK 21 만 있으면 됨 |
| `prod` | RDS MySQL | S3 | 컨테이너 (Dockerfile 이 `SPRING_PROFILES_ACTIVE=prod` 로 띄움) |

local 의 JWT 키 · 직원 계정(`staff` / `staff1234`)은 `application-local.yml` 의 **local 전용 테스트 값**. prod 에서는 쓰이지 않음.

## 02. 환경 변수 (prod)

infra · manifests 담당에게 넘기는 목록. 어디서 주입할지는 인프라 구성이 정해지면 연결. 목록은 [`.env.example`](../.env.example) 에도 있음.

| 변수 | 필수 | 비밀 | 예 |
| --- | --- | --- | --- |
| `DB_URL` | ✅ | | `jdbc:mysql://<rds_endpoint>:3306/cafe?serverTimezone=Asia/Seoul&characterEncoding=UTF-8` |
| `DB_USERNAME` | ✅ | ✅ | — |
| `DB_PASSWORD` | ✅ | ✅ | — |
| `JWT_SECRET` | ✅ | ✅ | 32바이트 이상 임의 문자열 · **모든 Pod 같은 값** (다르면 다른 Pod 가 토큰을 거부) |
| `APP_S3_BUCKET` | ✅ | | 팀 사진 버킷 이름 |
| `AWS_REGION` | | | `ap-northeast-2` (기본) |
| `STAFF_USERNAME` · `STAFF_PASSWORD` | | ✅ | 둘 다 있으면 기동 시 직원 계정 생성 |
| `DDL_AUTO` | | | `update` (기본) / `validate` |
| `SPRING_DATASOURCE_HIKARI_MAXIMUMPOOLSIZE` | | | `5` · Pod 수 × 이 값 < RDS `max_connections` |
| `SPRING_DATASOURCE_HIKARI_CONNECTIONTIMEOUT` | | | `3000` · DB 가 멈추면 3초 안에 실패 |

- 필수 값이 비어 있으면 기동 실패 — 일부러 기본값을 두지 않음.
- `JWT_SECRET` 을 바꾸면 발급된 토큰이 모두 무효 → 모두 다시 로그인.
- AWS 자격 증명은 환경 변수로 넣지 않음. S3 는 기본 자격 증명 체인이 Pod Identity 를 찾아 씀.

## 03. 앱 설정값 (이미지 안 · 비밀 아님)

| 설정 | 값 | 어디 |
| --- | --- | --- |
| 토큰 유효 시간 | 3600초 · 리프레시 없음 | `app.jwt.ttl-seconds` |
| 사진 상한 | 2MB · jpg/png/webp | `spring.servlet.multipart` · `ImageValidator` |
| 사진 URL 유효 시간 | 600초 | `app.s3.url-ttl-seconds` |
| 사진 key | `menus/{메뉴id}/{uuid}.{확장자}` | IAM 정책 Resource 를 `menus/*` 로 좁힐 수 있음 |

## 04. actuator

```yaml
management:
  endpoints.web.exposure.include: health,info,prometheus
  endpoint.health.show-details: never
  metrics.distribution.percentiles-histogram.http.server.requests: true   # p99 질의용 _bucket
```

| 엔드포인트 | 쓰는 곳 |
| --- | --- |
| `/actuator/health` | ALB 대상 그룹 헬스 체크 · readinessProbe. prod 는 DB 검사 포함 |
| `/actuator/info` | 배포 확인 (`build.version`) · 화면 오른쪽 위에도 표시 |
| `/actuator/prometheus` | ServiceMonitor 가 긁어 감 · 골든 시그널 `http_server_requests_seconds_*` |

S3 는 health 에 넣지 않음 — 사진 저장소가 잠깐 안 돼도 주문은 되어야 하므로.

**manifests 담당과 정할 것** — livenessProbe 를 `/actuator/health` 로 하면 RDS 장애 때 모든 Pod 가 재시작됨(재시작으로 안 고쳐짐). DB 를 보지 않는 쪽이 안전.

## 05. 컨테이너

| 항목 | 값 |
| --- | --- |
| 포트 | 8080 |
| 사용자 | non-root `app` (Dockerfile 2단계) |
| 베이스 | `eclipse-temurin:21-jdk` → `eclipse-temurin:21-jre` |
| 권장 requests | cpu `250m` · memory `512Mi` (HPA 분모) |
| 권장 limits | memory `768Mi` |

숫자는 실제 기동 후 `kubectl top pod` 로 다시 맞춤.
