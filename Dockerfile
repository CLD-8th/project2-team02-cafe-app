# syntax=docker/dockerfile:1
# 다단계 빌드 : 1단계에서 jar 를 만들고, 2단계(JRE 만 있는 작은 이미지)로 jar 만 옮김

# ---------- 1단계 : 빌드 ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# 의존성 내려받기를 먼저 해 두어, 소스만 바뀌면 이 층은 캐시에서 재사용됨
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies -q || true

COPY src ./src
RUN ./gradlew --no-daemon clean bootJar -x test \
 && cp build/libs/cafe-*.jar app.jar

# ---------- 2단계 : 실행 ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

# root 로 띄우지 않음 — UID · GID 1000 고정 (매니페스트 runAsUser · runAsGroup 1000 과 맞춤)
# 베이스(Ubuntu)에 이미 있는 ubuntu 사용자(UID 1000)를 지우고 app 으로 다시 만듦
RUN userdel --remove ubuntu 2>/dev/null || true \
 && groupadd --gid 1000 app \
 && useradd --uid 1000 --gid app --no-create-home --shell /usr/sbin/nologin app
COPY --from=build /workspace/app.jar app.jar
RUN chown app:app app.jar
# 숫자로 지정해야 쿠버네티스 runAsNonRoot 가 root 가 아님을 확인할 수 있음
USER 1000:1000

EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=prod
ENTRYPOINT ["java", "-jar", "app.jar"]
