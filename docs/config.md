# 설정 · 환경 변수

앱은 하나. 환경마다 다른 값은 전부 환경 변수로 받음. **코드에도, 이 저장소에도 비밀을 넣지 않음** (조직원 전원이 읽는 저장소).

## 01. 프로파일

| 프로파일 | DB | 사진 | 언제 |
| --- | --- | --- | --- |
| `local` (기본) | H2 메모리 | 디스크 `./data/menu-images` | 내 PC · 테스트 · JDK 21 만 있으면 됨 |
| `prod` | MySQL | S3 (`APP_S3_BUCKET`) | 운영 (Dockerfile 이 `SPRING_PROFILES_ACTIVE=prod` 로 띄움) |

local 의 JWT 키 · 직원 계정(`staff` / `staff1234`)은 `application-local.yml` 의 **local 전용 테스트 값**. prod 에서는 쓰이지 않음.

## 02. 환경 변수 (prod)

앱이 받는 값 목록. 값을 어디서 · 어떻게 넣을지는 운영 환경이 정해지면 연결. 목록은 [`.env.example`](../.env.example) 에도 있음.

| 변수 | 필수 | 비밀 | 설명 |
| --- | --- | --- | --- |
| `DB_URL` | ✅ | | `jdbc:mysql://<db_host>:3306/cafe?serverTimezone=Asia/Seoul&characterEncoding=UTF-8` |
| `DB_USERNAME` | ✅ | ✅ | DB 계정 |
| `DB_PASSWORD` | ✅ | ✅ | DB 비밀번호 |
| `JWT_SECRET` | ✅ | ✅ | 32바이트 이상 임의 문자열 · **앱이 여러 개면 모두 같은 값** (다르면 서로 토큰을 거부) |
| `STAFF_USERNAME` · `STAFF_PASSWORD` | | ✅ | 둘 다 있으면 기동 시 직원 계정 생성 |
| `APP_S3_BUCKET` | ✅ | | 메뉴 사진 버킷 (비공개 · 자격 증명은 Pod Identity, 키 없음) |
| `AWS_REGION` | | | 기본 `ap-northeast-2` |
| `DDL_AUTO` | | | `update` (기본) / `validate` |
| `SPRING_DATASOURCE_HIKARI_MAXIMUMPOOLSIZE` | | | 앱 하나당 DB 연결 수 (기본 10) |
| `SPRING_DATASOURCE_HIKARI_CONNECTIONTIMEOUT` | | | DB 연결 대기 ms (기본 30000) |

- 필수 값이 비어 있으면 기동 실패 — 일부러 기본값을 두지 않음.
- `JWT_SECRET` 을 바꾸면 발급된 토큰이 모두 무효 → 모두 다시 로그인.

## 03. 앱 설정값 (비밀 아님)

| 설정 | 값 | 어디 |
| --- | --- | --- |
| 토큰 유효 시간 | 3600초 · 리프레시 없음 | `app.jwt.ttl-seconds` |
| 사진 상한 | 2MB · jpg/png/webp | `spring.servlet.multipart` · `ImageValidator` |
| 사진 key | `menus/{메뉴id}/{uuid}.{확장자}` | `MenuService` |

## 04. actuator

```yaml
management:
  endpoints.web.exposure.include: health,info,prometheus
  endpoint.health.show-details: never
  metrics.distribution.percentiles-histogram.http.server.requests: true   # p99 질의용 _bucket
```

| 엔드포인트 | 무엇 |
| --- | --- |
| `/actuator/health` | 생존 확인. prod 는 DB 검사 포함 · DB 가 안 되면 503 |
| `/actuator/info` | 빌드 버전 · 화면 오른쪽 위에도 표시 |
| `/actuator/prometheus` | 요청 수 · 오류 · 응답 시간 지표 (`http_server_requests_seconds_*`) |

사진 저장소는 health 에 넣지 않음 — 사진이 잠깐 안 돼도 주문은 되어야 하므로.

## 05. 컨테이너 (Dockerfile)

| 항목 | 값 |
| --- | --- |
| 포트 | 8080 |
| 사용자 | non-root `app` |
| 베이스 | `eclipse-temurin:21-jdk` 로 빌드 → `eclipse-temurin:21-jre` 로 실행 |
