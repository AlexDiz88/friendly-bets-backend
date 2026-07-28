FROM maven:3.9.3-eclipse-temurin-17 AS build
WORKDIR /workspace/app

COPY pom.xml .
COPY src src

RUN mvn -DskipTests=true clean package
RUN mkdir -p target/dependency && (cd target/dependency; jar -xf ../*.jar)

# Runtime: JRE + Node (Melbet Digitain WASM decrypt via decrypt-cli.cjs)
FROM eclipse-temurin:17-jre-alpine
RUN apk add --no-cache nodejs \
	&& node -v \
	&& test -x "$(command -v node)"

ARG DEPENDENCY=/workspace/app/target/dependency
COPY --from=build ${DEPENDENCY}/BOOT-INF/lib /app/lib
COPY --from=build ${DEPENDENCY}/META-INF /app/META-INF
COPY --from=build ${DEPENDENCY}/BOOT-INF/classes /app
ENTRYPOINT ["java","-cp","app:app/lib/*", "-Dspring.profiles.active=prod", "net.friendly_bets.FriendlyBetsApplication"]
