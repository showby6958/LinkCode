# syntax=docker/dockerfile:1

# ─────────────────────────── build stage ───────────────────────────
# 빌드 컨텍스트는 레포 루트(멀티모듈)여야 한다. compose에서
#   build: { context: ., dockerfile: api-gateway/Dockerfile } 로 지정한다.
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /workspace

# Gradle 소스 전체 복사 (build/·.gradle/·.idea 등은 .dockerignore로 제외됨)
COPY . .

# 이 모듈의 실행 가능한 부트 jar만 빌드. 테스트는 이미지 빌드에서 제외(CI에서 별도).
# --mount=type=cache: 빌드 간 Gradle 의존성 캐시를 재사용한다(BuildKit 필요).
RUN --mount=type=cache,target=/root/.gradle \
    chmod +x gradlew && ./gradlew :api-gateway:bootJar --no-daemon -x test

# ─────────────────────────── runtime stage ─────────────────────────
FROM eclipse-temurin:17-jre-jammy AS runtime
WORKDIR /app

# 비루트 유저로 실행
RUN groupadd --system app && useradd --system --gid app --home-dir /app app

# bootJar만 생성되도록 build.gradle에서 jar 비활성 → libs에 부트 jar 하나뿐
COPY --from=build /workspace/api-gateway/build/libs/*.jar app.jar
USER app

# 외부 진입점(게이트웨이)
EXPOSE 8080

# 컨테이너 메모리 한도를 인식해 힙을 잡는다
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"
ENTRYPOINT ["sh","-c","exec java $JAVA_OPTS -jar /app/app.jar"]
