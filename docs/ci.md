# CI

## 01. 워크플로 파일

| 파일 | 언제 | 하는 일 |
| --- | --- | --- |
| `ci.yml` | PR → main | 테스트 (`./gradlew test`) — 브랜치 보호의 필수 검사 이름 `test` |
| `_test.yml` | 다른 워크플로가 부름 | 테스트 잡 하나 (재사용) |
| `build-and-update.yml` | push → main · 수동 | 테스트 → 이미지 빌드 · 배포 단계 |

## 02. 지금 상태

- 테스트는 바로 동작함.
- `build-and-update.yml` 의 이미지 빌드 · 배포 단계는 운영 환경(인프라)이 정해지면 연결.
  - 그 전에는 저장소 Actions 변수(vars)가 비어 있어 **자동으로 건너뜀** → main push 가 실패로 뜨지 않음.
  - 이미지 태그는 커밋 앞 7자리 · `latest` 를 쓰지 않음.
- 배포 단계를 켜는 날은 팀이 함께 확인하고, 필요한 변수 · 권한을 이 문서에 정리함.

## 03. 확인

| 확인 | 기대 |
| --- | --- |
| PR 의 Checks | `test` 성공 |
| main push 후 Actions | `test` 성공 · 배포 잡은 skipped (변수 연결 전) |
