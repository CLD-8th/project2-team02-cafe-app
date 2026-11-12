# CI 흐름

```
앱 commit(main) → test → build-push (ECR · 태그 = 커밋 앞 7자리)
               → update-manifests (매니페스트 저장소의 newTag 한 줄 커밋)
               → Argo CD 감지 → 동기화 → Deployment 롤링 → 새 버전 응답
```

CI 는 매니페스트 저장소에 커밋하는 데서 끝남. 클러스터에는 손대지 않음 (교안 GitOps · 태그 write-back).

## 01. 워크플로 파일

| 파일 | 언제 | 하는 일 |
| --- | --- | --- |
| `ci.yml` | PR → main | 테스트만 (브랜치 보호의 필수 검사 `test`) |
| `_test.yml` | 다른 워크플로가 부름 | 테스트 잡 하나 (재사용) |
| `build-and-update.yml` | push → main · 수동 | test → build-push → update-manifests |

## 02. build-and-update.yml

| 잡 | 실행 조건 | 내용 |
| --- | --- | --- |
| `test` | 항상 | `./gradlew test` |
| `build-push` | `vars.AWS_ROLE_ARN` · `vars.ECR_REPOSITORY` 가 있을 때 | OIDC 역할 → ECR 로그인 → 빌드 · push · 이미 있는 태그면 건너뜀 · `outputs.image_tag` |
| `update-manifests` | `vars.MANIFESTS_REPO` · `vars.MANIFESTS_KUSTOMIZATION` 이 있을 때 | 쓰기 Deploy key 로 clone → `sed` 로 `newTag: "<sha7>"` → `cafe: <sha7>` 커밋 · push |

- 인프라 · 매니페스트 저장소가 준비되기 전에는 vars 가 비어 있어 뒤 두 잡을 **건너뜀** — main push 가 실패로 뜨지 않음.
- `concurrency: build-and-update` · 취소 안 함 → 매니페스트 커밋이 순서대로 쌓임.
- `sed` 는 파일 안의 `newTag:` 줄을 모두 바꿈 → kustomization 파일에 이미지가 하나라고 가정.

## 03. Actions 설정값

`vars` 는 보여도 되는 값, `secrets` 는 감추는 값. app 저장소 Settings > Secrets and variables > Actions.

| 종류 | 이름 | 값 | 누가 정함 |
| --- | --- | --- | --- |
| vars | `AWS_ROLE_ARN` | `team02-` 로 시작하는 GitHub Actions 역할 ARN | infra |
| vars | `ECR_REPOSITORY` | `team02/cafe` | infra |
| vars | `MANIFESTS_REPO` | `CLD-8th/project2-team02-cafe-manifests` | — |
| vars | `MANIFESTS_KUSTOMIZATION` | 예 `apps/cafe/overlays/eks/kustomization.yaml` | manifests |
| secrets | `MANIFESTS_DEPLOY_KEY` | manifests 저장소에 **쓰기 허용** 으로 등록한 Deploy key 의 개인 키 | manifests |

Argo CD 가 manifests 를 읽는 Deploy key(읽기 전용)와는 다른 키. 새어도 그 저장소 하나에 그침.

## 04. infra 담당에게 넘길 것

- **GitHub OIDC 역할** — 공급자는 공용(`data` 로 읽음) · 이름 `team02-…` · 권한 경계 `Student-Boundary` · 신뢰 조건 `repo:CLD-8th/project2-team02-cafe-app:ref:refs/heads/main` · 권한은 ECR `team02/cafe` push + `ecr:DescribeImages` (`ecr:GetAuthorizationToken` 은 `*`)
- **ECR** `team02/cafe` — 태그 Immutable · 푸시 시 스캔
- **앱 Pod 역할 (Pod Identity)** — S3 사진 버킷의 `menus/*` 에 `s3:PutObject` · `s3:GetObject` · `s3:DeleteObject`

## 05. 확인

| 확인 | 기대 |
| --- | --- |
| Actions 실행 | `test` → `build-push` → `update-manifests` 성공 |
| ECR `team02/cafe` | 태그 = 커밋 앞 7자리 · `latest` 없음 |
| manifests 저장소 | `cafe: <sha7>` 커밋 · `newTag` 한 줄만 바뀜 |
| Argo CD | 동기화 이력에 같은 커밋 · `Synced` · `Healthy` |
| `curl <ALB>/actuator/info` · 화면 오른쪽 위 | 새 버전 |
