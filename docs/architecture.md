# cafe 앱 아키텍처

![cafe 앱 아키텍처](architecture.svg)

PNG 버전 (Notion · 발표 자료용) : [architecture.png](architecture.png)

## 01. 한 줄

카페 주문 서비스. 손님은 가입해서 메뉴를 보고 주문하고, 직원은 주문을 처리하고 메뉴 · 사진을 관리함. 화면(바닐라 JS)과 API 가 **이미지 하나** 에 들어 있음.

## 02. 구성

| 층 | 무엇 | 비고 |
| --- | --- | --- |
| 화면 | `static/` HTML · CSS · JS | 빌드 도구 없음 · 같은 앱이 내려줌 |
| 보안 | Spring Security · JWT(HS256) | 세션 없음 → Pod 가 여러 개 · 롤링 배포여도 로그인 유지 |
| API | `api/` 컨트롤러 | `/api/auth` · `/api/menus` · `/api/orders` — [api.md](api.md) |
| 업무 규칙 | `service/` | 상태 전이 · 권한 · 메뉴 판매 중지 — [domain.md](domain.md) |
| 저장 | JPA (`users` · `menus` · `orders`) · `ImageStorage` | local = H2 · 디스크 / prod = RDS · S3 |
| 운영 | actuator | `/actuator/health` (ALB · Probe) · `info` (버전) · `prometheus` (지표) |

## 03. 프로파일

| | local (내 PC · 테스트) | prod (컨테이너) |
| --- | --- | --- |
| DB | H2 메모리 · 끄면 사라짐 | RDS MySQL |
| 사진 | `./data/menu-images` · 앱이 `/local-images/**` 로 내려줌 | S3 비공개 버킷 · 프리사인 GET URL (10분) |
| JWT 키 · 직원 계정 | 설정 파일의 테스트 값 | 환경 변수 (Secret) — [config.md](config.md) |
| 필요한 것 | JDK 21 만 | DB · S3 · Secret (인프라 연결 후) |

## 04. 인프라와 만나는 곳 (나중에 연결)

인프라는 다른 팀원이 설계 중. 앱은 아래 값만 받으면 붙음 — 코드에는 주소 · 비밀이 하나도 없음.

| 앱이 필요로 하는 것 | 앱이 받는 방법 | 인프라 쪽에서 할 일 |
| --- | --- | --- |
| DB | `DB_URL` · `DB_USERNAME` · `DB_PASSWORD` | RDS · DB `cafe` · 앱 SG → DB SG 3306 |
| 사진 저장 | `APP_S3_BUCKET` · `AWS_REGION` | 버킷 · Pod Identity 역할 (`menus/*` 에 Put · Get · Delete) |
| 로그인 서명 키 | `JWT_SECRET` (32바이트 이상) | Secrets Manager → ESO → Secret |
| 직원 계정 | `STAFF_USERNAME` · `STAFF_PASSWORD` | Secrets Manager → ESO → Secret |
| 헬스 체크 · 지표 | `/actuator/health` · `/actuator/prometheus` | Ingress · Probe · ServiceMonitor |
| 이미지 | ECR `team02/cafe` · 태그 = 커밋 7자리 | ECR · GitHub OIDC 역할 — [ci.md](ci.md) |

## 05. 설계 선택

| 선택 | 이유 |
| --- | --- |
| 화면을 앱 안에 (바닐라 JS) | 이미지 · Deployment · CI 가 하나 그대로. 프로젝트 평가는 배포 · 운영 |
| JWT · 세션 없음 | Pod 여러 개 + 롤링 배포에서 세션 공유(Redis 등) 없이 로그인 유지 |
| 메뉴 삭제 = 판매 중지 | 실수로 지워도 되살림 · 지난 주문이 없는 메뉴를 가리키지 않음 |
| 주문에 메뉴 이름 · 가격 복사 | 메뉴 가격이 바뀌어도 지난 주문 금액 유지 |
| 사진은 앱 경유 업로드 · 프리사인 GET | 직원만 작은 사진을 올림 → 앱이 검사. 버킷은 비공개 유지 · CloudFront 불필요 |
| 캐시 없음 (지금) | 팀이 ElastiCache 를 쓰기로 하면 메뉴 목록 조회에 cache-aside 로 추가 |
