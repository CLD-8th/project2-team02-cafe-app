# cafe API

모든 응답은 JSON. 실패는 `{status, code, message, timestamp}` 한 가지 형태.
로그인이 필요한 API 는 `Authorization: Bearer <accessToken>` 헤더를 보냄.

## 01. 목록

| 방법 | 경로 | 누가 | 설명 | 성공 |
| --- | --- | --- | --- | --- |
| POST | `/api/auth/signup` | 누구나 | 손님 가입. `{username, password}` | 201 |
| POST | `/api/auth/login` | 누구나 | 로그인 → `accessToken` (1시간) | 200 |
| GET | `/api/auth/me` | 로그인 | 내 정보 (`role` = `CUSTOMER` / `STAFF`) | 200 |
| GET | `/api/menus` | 누구나 | 판매 중인 메뉴 (사진 URL 포함) | 200 |
| GET | `/api/menus?all=true` | 직원 | 판매 중지 포함 | 200 |
| POST | `/api/menus` | 직원 | 메뉴 추가. `{name, price}` | 201 + `Location` |
| PATCH | `/api/menus/{id}` | 직원 | 수정. `{name?, price?, available?}` — 넘긴 값만 | 200 |
| DELETE | `/api/menus/{id}` | 직원 | 판매 중지 (행은 남음 · `available:true` 로 되살림) | 204 |
| PUT | `/api/menus/{id}/image` | 직원 | 사진 올리기 · 교체. multipart 항목 `file` · jpg/png/webp · 2MB | 200 |
| **POST** | **`/api/orders`** | 로그인 | **주문 생성.** `{menuId, quantity}` · `RECEIVED` 로 시작 | 201 + `Location` |
| **GET** | **`/api/orders/{id}`** | 로그인 | **주문 조회.** 손님은 내 주문만 | 200 |
| **PATCH** | **`/api/orders/{id}/status`** | 로그인 | **상태 변경.** `{status}` — 직원: 전이표대로 / 손님: `RECEIVED` 일 때 `CANCELED` 만 | 200 |
| **GET** | **`/api/orders?status=`** | 로그인 | **주문 목록.** 최근 100건 · 직원: 전체 / 손님: 내 주문 | 200 |
| GET | `/actuator/health` | 누구나 | 생존 확인. prod 는 DB 검사 포함 · 실패하면 503 | 200 |
| GET | `/actuator/info` | 누구나 | 빌드 버전 · 시각 | 200 |
| GET | `/actuator/prometheus` | 누구나 | Prometheus 지표 | 200 |

굵은 4개가 가이드의 핵심 API (생성 · 조회 · 상태 변경 · 목록). 상태 전이는 [domain.md](domain.md).

## 02. 실패 코드

| 상태 | code | 언제 |
| --- | --- | --- |
| 400 | `BAD_REQUEST` | 입력 형식 · 범위 오류 · 없는 메뉴 · 판매 중지 메뉴 주문 · 중복 username/메뉴 이름 · 사진 형식 |
| 401 | `UNAUTHORIZED` | 토큰 없음 · 만료 · 위조 · 로그인 실패 |
| 403 | `FORBIDDEN` | 손님이 직원 API 호출 · 다른 손님 주문 · 손님이 취소 말고 다른 상태 요청 |
| 404 | `NOT_FOUND` | 없는 주문 · 메뉴 |
| 409 | `INVALID_STATUS_TRANSITION` | 허용하지 않는 상태 전이 (손님이 제조 중 주문 취소 포함) |
| 413 | `PAYLOAD_TOO_LARGE` | 사진이 멀티파트 상한 초과 |
| 500 | `INTERNAL_ERROR` · `STORAGE_ERROR` | 그 밖의 오류 · 사진 저장 실패. 사유는 서버 로그에만 |

## 03. curl 예

```bash
BASE=http://localhost:8080          # 클러스터에서는 ALB 주소

# 가입 · 로그인 (가상 계정)
curl -s -X POST $BASE/api/auth/signup -H 'Content-Type: application/json' -d '{"username":"guest01","password":"testpass1"}'
TOKEN=$(curl -s -X POST $BASE/api/auth/login -H 'Content-Type: application/json' \
  -d '{"username":"guest01","password":"testpass1"}' | sed -E 's/.*"accessToken":"([^"]+)".*/\1/')
AUTH="Authorization: Bearer $TOKEN"

# 메뉴 · 주문
curl -s $BASE/api/menus
curl -s -X POST $BASE/api/orders -H "$AUTH" -H 'Content-Type: application/json' -d '{"menuId":1,"quantity":2}'
curl -s $BASE/api/orders/1 -H "$AUTH"
curl -s "$BASE/api/orders?status=RECEIVED" -H "$AUTH"
curl -s -X PATCH $BASE/api/orders/1/status -H "$AUTH" -H 'Content-Type: application/json' -d '{"status":"CANCELED"}'

# 직원 (local 기본 계정 staff / staff1234 — local 전용 테스트 값)
STAFF="Authorization: Bearer $(curl -s -X POST $BASE/api/auth/login -H 'Content-Type: application/json' \
  -d '{"username":"staff","password":"staff1234"}' | sed -E 's/.*"accessToken":"([^"]+)".*/\1/')"
curl -s -X PATCH $BASE/api/orders/2/status -H "$STAFF" -H 'Content-Type: application/json' -d '{"status":"PREPARING"}'
curl -s -X POST $BASE/api/menus -H "$STAFF" -H 'Content-Type: application/json' -d '{"name":"카페모카","price":5500}'
curl -s -X PUT $BASE/api/menus/6/image -H "$STAFF" -F "file=@mocha.png;type=image/png"

# 버전 · 지표
curl -s $BASE/actuator/info
curl -s $BASE/actuator/prometheus | grep http_server_requests_seconds_count
```

## 04. 응답 예

```json
// POST /api/auth/login → 200
{ "accessToken": "eyJhbGciOiJIUzI1NiJ9…", "tokenType": "Bearer", "expiresIn": 3600, "username": "guest01", "role": "CUSTOMER" }

// GET /api/menus → 200  (prod 의 imageUrl 은 S3 프리사인 URL)
[
  { "id": 1, "name": "아메리카노", "price": 4500, "available": true, "imageUrl": "/local-images/menus/1/f77e3f06-….png" },
  { "id": 2, "name": "카페라테", "price": 5000, "available": true, "imageUrl": null }
]

// POST /api/orders → 201, Location: /api/orders/1
{
  "id": 1, "menuId": 1, "menuName": "아메리카노", "unitPrice": 4500, "quantity": 2, "totalPrice": 9000,
  "status": "RECEIVED", "customer": "guest01", "createdAt": "2026-11-12T04:02:07.120Z"
}

// 실패 → 409
{ "status": 409, "code": "INVALID_STATUS_TRANSITION", "message": "COMPLETED 에서 PREPARING 로 바꿀 수 없음", "timestamp": "2026-11-12T13:16:10.33" }
```

메뉴 · 계정은 모두 가상 데이터.
