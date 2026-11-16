# CI

## 01. 워크플로 파일

| 파일 | 언제 | 하는 일 |
| --- | --- | --- |
| `ci.yml` | PR → main | 테스트 (`./gradlew test`) — 브랜치 보호의 필수 검사 이름 `test` |
| `_test.yml` | 다른 워크플로가 부름 | 테스트 잡 하나 (재사용) |
| `build-and-update.yml` | push → main · 수동 | 테스트 → 이미지 빌드 · ECR push → 매니페스트 저장소 `newTag` 커밋 |

## 02. 지금 상태

- PR 마다 테스트가 돎.
- main 에 들어오면 `build-and-update.yml` 이 돎. 아래 값이 비어 있으면 빌드 · 배포 잡은 **자동으로 건너뜀** (main push 가 실패로 뜨지 않음).

| 종류 | 이름 | 값 |
| --- | --- | --- |
| Actions 변수 | `AWS_ROLE_ARN` | CI 배포 역할 `team02-gha-deploy-role` ARN (GitHub OIDC 로 맡음 · 저장소에 키 없음) |
| Actions 변수 | `ECR_REPOSITORY` | `team02/cafe` |
| Actions 변수 | `MANIFESTS_REPO` · `MANIFESTS_KUSTOMIZATION` | 매니페스트 저장소 이름 · 태그를 바꿀 kustomization 파일 경로 |
| Actions 비밀 | `MANIFESTS_DEPLOY_KEY` | 매니페스트 저장소 쓰기용 Deploy key |

- 이미지 태그는 커밋 앞 7자리 · `latest` 를 쓰지 않음. ECR 이 Immutable 이라 같은 태그가 있으면 push 를 건너뜀.

## 03. 확인

| 확인 | 기대 |
| --- | --- |
| PR 의 Checks | `test` 성공 |
