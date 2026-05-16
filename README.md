# Church Administration Platform

Această aplicație este versiunea Spring Boot + Thymeleaf a Administrației Bisericești.

## Cum rulezi

1. Rulează aplicația:
   ```bash
   mvn spring-boot:run
   ```
2. Pentru dev/local fără Postgres, pornește cu profilul `dev` (H2):
   ```bash
   mvn spring-boot:run -Dspring-boot.run.profiles=dev
   ```
3. Porturi:
   - `prod`: `http://localhost:8080/dashboard`
   - `dev`: `http://localhost:85/dashboard`

## Ce poți face acum

- vezi lista de evenimente; clasificarea se bazează pe `EventService`.
- creezi / editezi evenimente prin formulare Thymeleaf.
- ștergi evenimente.

## Ce urmează

Următorii pași de migrare pot fi parcurși incremental:
1. extinde controllerul MVC cu pagini pentru persoane, finanțe și vizite (folosind serviciile `PersonService`, `FinanceService`, `VisitService` din backend).
2. adaugă validări și fragmentare de layout (ex. `fragments/header`, `fragments/footer`).
3. adaptează securitatea/session-ul dacă sunt necesare roluri.

## Testare

Comenzi utile:

1. Toate testele unitare:
   ```bash
   mvn -Punit test
   ```
2. Faza de integrare (failsafe, teste `*IT`):
   ```bash
   mvn -Pintegration verify
   ```
3. Build rapid fără teste:
   ```bash
   mvn -DskipTests compile
   ```

Configurația CI rulează automat pe `push` și `pull_request` în
`.github/workflows/ci.yml`.
CI rulează `mvn -Punit verify` (inclusiv gate JaCoCo) și `mvn -Pintegration verify`,
apoi publică artefactele: `target/site/jacoco`, `target/surefire-reports`, `target/failsafe-reports`.

## Standard De Calitate (Production-Ready)

### Suite De Teste

1. `Unit` (`mvn -Punit test`)
   - validează logică de business, validări, normalizări și fallback-uri.
2. `Integration` (`mvn -Pintegration verify`)
   - validează fluxurile de securitate și contractele de resurse.
   - testele cu Testcontainers rulează când Docker este disponibil; în lipsa Docker sunt marcate `skipped` controlat.

### Praguri Recomandate (Minim)

1. `Security + Auth`: acoperire funcțională 100% pe rutele critice (`/login`, `/logout`, `/admin/**`, CSRF pe `POST`).
2. `Settings`: acoperire pe fallback-uri, profile (`desktop`/non-desktop), import/export error handling.
3. `Pastoral`: acoperire pe redirect-uri sigure, tranziții de status/stage, validări input.
4. `Calendar`: acoperire pe recurență și agregare evenimente în grid.

Notă: pragurile sunt orientate pe risc funcțional (nu doar procent global de linii).
Prag JaCoCo activ curent: `25%` linii pe bundle, cu plan de creștere incrementală.

## Checklist Release

Rulat înainte de fiecare release:

1. `mvn -B -DskipTests compile`
2. `mvn -B -Punit test`
3. `mvn -B -Pintegration verify`
4. verificare manuală UI pe fluxurile critice:
   - login/logout
   - settings/system save
   - admin users (create/update/delete)
   - follow-ups (create/status/stage)
5. verificare i18n:
   - `ro`/`en` fără chei lipsă pe paginile principale
6. verificare migrare/config:
   - profile corecte (`default`/`dev`/`desktop`)
   - runtime mode desktop (dacă este folosit) scrie config valid
7. confirmare CI verde pe branch-ul release.

## Politică Push / PR

1. Nu se face merge în `main`/`master` dacă CI este roșu.
2. Pentru schimbări în securitate/settings/pastoral sunt necesare:
   - teste noi sau actualizate
   - dovadă rulare locală (`unit` + `integration`)
