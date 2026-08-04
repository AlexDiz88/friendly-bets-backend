# AGENTS.md

## Git workflow — environment `friendly-bets-FULLSTACK` (телефон / Cloud Agent)

**Это главное правило для этого окружения. Не игнорировать.**

- **Окружение:** `friendly-bets-FULLSTACK` (Friendly Bets: backend + frontend). Запросы с телефона через это окружение всегда следуют этим правилам.
- **Единственная рабочая ветка:** `dev` на remote (`origin/dev`). Вся работа — только в `dev`.
- **Перед началом:** `git checkout dev && git pull origin dev`.
- **Коммиты и пуш:** сразу в `dev` — `git commit` и `git push origin dev`.
- **НЕ создавать** feature-ветки (`cursor/...`, `feature/...` и любые другие).
- **НЕ открывать** pull request'ы. Изменения попадают в репозиторий только через прямой push в `origin/dev`.
- **НЕ пушить** в `main` и не мержить в `main` из этого окружения.
- Исключение только если пользователь **явно** попросил другую ветку или PR — в обычной работе с телефона через `friendly-bets-FULLSTACK` это не применяется.

## Cursor Cloud specific instructions

Spring Boot 2.7 (Java 17) REST API for the "Friendly Bets" app. Frontend lives in a separate repo (`friendly-bets-frontend`).

### Services / how to run
- Build: `mvn -DskipTests clean package` (requires Java 17 — `JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64`; the project targets Java 17, not the system-default JDK).
- Run (dev): `mvn spring-boot:run -Dspring-boot.run.profiles=dev` → serves on port `8080`; root `/` redirects to `/swagger-ui.html`.
- Test: `mvn test` (unit + integration tests; integration tests use flapdoodle embedded Mongo by default, so they do NOT need a running MongoDB).

### MongoDB requirement (non-obvious)
- The app uses MongoDB **transactions**, which require a **replica set**. A standalone `mongod` fails at startup with `Transaction numbers are only allowed on a replica set member or mongos`.
- Run MongoDB as a single-node replica set: `mongod --dbpath /data/db --replSet rs0 --fork --logpath /var/log/mongodb/mongod.log`, then once: `mongosh --eval 'rs.initiate({_id:"rs0",members:[{_id:0,host:"127.0.0.1:27017"}]})'`.
- `mongod` is started manually (no systemd in the VM). The replica-set config persists in `/data/db`, so after a restart you only need to start `mongod` again (no re-initiate).

### Config (non-obvious)
- The **dev** profile imports `./db.properties` (git-ignored). It must exist at the repo root with `mongodb.uri=mongodb://localhost:27017`. Without it, dev startup fails on placeholder `${mongodb.uri}`.
- Mail is disabled by default (`app.mail.enabled=false`) and email confirmation is not required for login, so registration + login work fully offline. New accounts show an "email not confirmed" banner — this is expected, not a bug.
- External data scrapers (Marathonbet/Melbet/Soccer365/etc.) hit the public internet; not needed to run/test the core app.
