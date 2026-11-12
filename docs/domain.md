# 도메인 · 상태 전이

## 01. 표

엔티티 3개 = 표 3개. local 은 H2, prod 는 RDS MySQL 데이터베이스 `cafe`. `ddl-auto: update` 라 첫 기동 때 앱이 표를 만듦.

```sql
CREATE TABLE users (
  id            BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
  username      VARCHAR(20)  NOT NULL UNIQUE,
  password_hash VARCHAR(100) NOT NULL,          -- BCrypt
  role          VARCHAR(20)  NOT NULL,          -- CUSTOMER · STAFF
  created_at    DATETIME(6)  NOT NULL
);

CREATE TABLE menus (
  id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
  name        VARCHAR(30)  NOT NULL UNIQUE,
  price       INT          NOT NULL,            -- 100 ~ 100000
  available   BOOLEAN      NOT NULL,            -- false = 판매 중지 (삭제 대신)
  image_key   VARCHAR(200),                     -- menus/{id}/{uuid}.jpg · 없으면 NULL
  created_at  DATETIME(6)  NOT NULL
);

CREATE TABLE orders (
  id             BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
  menu_id        BIGINT       NOT NULL,
  menu_name      VARCHAR(30)  NOT NULL,         -- 주문 당시 이름
  unit_price     INT          NOT NULL,         -- 주문 당시 가격
  quantity       INT          NOT NULL,         -- 1 ~ 10
  status         VARCHAR(20)  NOT NULL,
  customer_id    BIGINT       NOT NULL,
  customer_name  VARCHAR(20)  NOT NULL,
  created_at     DATETIME(6)  NOT NULL,
  INDEX idx_orders_status (status),
  INDEX idx_orders_customer (customer_id)
);
```

| 결정 | 이유 |
| --- | --- |
| 표 이름 `orders` · `users` | `ORDER` · `USER` 는 SQL 예약어 (엔티티 이름도 `CafeOrder` · `AppUser`) |
| 실제 개인정보 없음 | username 만 받음 — 이메일 · 이름 · 전화번호 안 받음 (프로젝트 규칙) |
| 주문에 메뉴 이름 · 가격 복사 | 메뉴가 바뀌어도 지난 주문 금액 유지 |
| 주문에 `customer_name` 복사 | username 은 바뀌지 않음 · 직원 목록에서 조인 없이 표시 |
| FK 없음 | 메뉴 · 회원은 지우지 않으므로 참조가 깨지지 않음 · 표 생성 순서 신경 안 씀 |

## 02. 기본 데이터

| 무엇 | 언제 | 내용 |
| --- | --- | --- |
| 메뉴 | `menus` 가 비어 있으면 기동 시 | 아메리카노 4500 · 카페라테 5000 · 바닐라라테 5500 · 콜드브루 5000 · 녹차라테 5500 (가상) |
| 직원 계정 | `STAFF_USERNAME` · `STAFF_PASSWORD` 가 있고 그 username 이 없으면 기동 시 | 역할 `STAFF` |

- prod 는 RDS 에 저장되므로 Pod 가 재시작돼도 남음. 이미 있으면 건너뜀 → `STAFF_PASSWORD` 만 바꿔도 기존 비밀번호는 안 바뀜.
- Pod 여러 개가 동시에 넣으려 하면 유일 제약으로 하나만 성공 · 나머지는 로그만 남기고 정상 기동.

## 03. 상태 전이

```
RECEIVED ──▶ PREPARING ──▶ COMPLETED
    │             │
    └──────┬──────┘
           ▼
       CANCELED
```

| 지금 \ 바꿀 값 | `RECEIVED` | `PREPARING` | `COMPLETED` | `CANCELED` |
| --- | --- | --- | --- | --- |
| `RECEIVED` | 409 | 직원 | 409 | 직원 · 손님(내 주문) |
| `PREPARING` | 409 | 409 | 직원 | 직원 (손님은 409) |
| `COMPLETED` | 409 | 409 | 409 | 409 |
| `CANCELED` | 409 | 409 | 409 | 409 |

- 전이 규칙은 `OrderStatus.canChangeTo` 한 곳. 손님 제한(취소만 · 접수 상태만)은 `OrderService.changeStatus`.
- 손님이 취소 말고 다른 상태를 요청하면 403, 제조 중 주문을 취소하려 하면 409.

## 04. 패키지 구조

file-service 와 같은 계층별 나눔.

```
com.example.cafe
├── CafeApplication
├── api        컨트롤러 · dto · CurrentUsers(JWT → CurrentUser)
├── service    엔티티 · 리포지토리 · 업무 규칙 (Auth · Menu · Order · ImageValidator)
├── storage    ImageStorage — LocalImageStorage(!prod) · S3ImageStorage(prod)
├── config     SecurityConfig · JwtConfig · MenuSeeder · StaffAccountInitializer
└── common     ErrorResponse · GlobalExceptionHandler · 예외들
```

## 05. 배포 중 호환

롤링 배포 동안 옛 버전과 새 버전이 같은 표를 함께 씀.

- 열 추가는 nullable 로 먼저 → 다음 배포에서 사용. 열 삭제 · 이름 변경은 한 번에 하지 않음.
- 상태 enum 에 값을 추가하면 옛 버전이 그 값을 읽을 때 실패 → "읽기 지원" 배포 다음에 "쓰기" 배포.
- JWT 클레임(`uid` · `role`)을 바꾸면 옛 토큰이 새 버전에서 깨짐 → 클레임은 추가만.
