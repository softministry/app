# Plan incremental de dezvoltare Thymeleaf

Acest document este sursa de stare pentru functionalitatile noi care vor imbogati proiectul dupa migrarea Angular -> Thymeleaf.

Regula de lucru:
- fiecare milestone se implementeaza incremental;
- dupa fiecare increment finalizat, checkbox-ul se marcheaza cu `[x]`;
- la final se adauga o nota in sectiunea `Istoric implementari`;
- daca apar decizii noi, se noteaza aici inainte de implementare.

## Status general

- [x] Migrare de baza Thymeleaf pentru dashboard, persoane, evenimente, vizite, calendar, settings, church, families, preaching, finance
- [x] Layout comun Thymeleaf pentru head assets, meniu, selector biserica, tema si limba
- [x] Attendance separat pe zi/program
- [ ] Grupuri / echipe / slujiri
- [ ] Follow-up pastoral
- [ ] Rapoarte
- [ ] Comunicare
- [ ] Event registrations
- [ ] Finance upgrade

## Milestone 1: Attendance separat pe zi/program

Scop: sa putem marca prezenta persoanelor pe o zi si un program (`DEFAULT`, `MORNING`, `EVENING`) si sa folosim aceste date ulterior in dashboard, rapoarte si follow-up pastoral.

### Backend

- [x] Definire `AttendanceStatus`: `PRESENT`, `ABSENT`, `EXCUSED`
- [x] Definire entitate `AttendanceRecord`
- [x] Campuri minime: `id`, `churchId`, `attendanceDate`, `serviceSession`, `person`, `status`, `checkedInAt`, `checkedOutAt`, `recordedAt`
- [x] Repository pentru attendance
- [x] Service pentru listare/salvare bulk pe zi si program
- [x] Filtrare dupa biserica activa prin `churchId` si persoana
- [x] Migrare DB/Flyway sau schema update compatibila cu setup-ul curent

### UI Thymeleaf

- [x] Pagina separata `/attendance` cu link in meniu
- [x] Selector zi
- [x] Selector program: default/dimineata/seara
- [x] Lista persoane din biserica activa
- [x] Cautare in lista de persoane
- [x] Bifa per persoana prezenta
- [x] Salvare bulk
- [x] Mesaj de succes/eroare
- [x] Persistenta dupa refresh
- [x] Sectiunea initiala din editarea evenimentului a fost scoasa
- [x] Componenta cu programele inregistrate: zi, program, prezenti, absenti, total
- [x] Click pe program inregistrat pentru detalii prezenti/absenti
- [x] Detaliile programului de prezenta se deschid in pop-up fara navigare
- [x] Cautare persoana in pop-up-ul de detalii si pagina principala simplificata la tabelul de programe

### Dashboard

- [x] Card cu ultimele 5 sesiuni de prezenta si numar participanti
- [x] Link rapid spre sesiunea de prezenta

### Verificare

- [x] `mvn -q -pl teamleaf_approuch -am compile`
- [x] selectare zi/program in pagina `/attendance`
- [ ] marcare prezenta pentru mai multe persoane
- [ ] refresh si verificare persistenta
- [ ] schimbare biserica si verificare ca datele nu se amesteca
- [x] test automat pentru `AttendanceService` pe salvare, update si izolare pe biserica

## Milestone 2: Grupuri / echipe / slujiri

Scop: organizarea persoanelor in grupuri pastorale sau operationale: tineret, cor, copii, tehnic, primire, grupuri mici.

- [x] Entitate `Group`
- [x] Tip grup: `SMALL_GROUP`, `MINISTRY`, `TEAM`, `CLASS`, `OTHER`
- [x] Lider grup
- [x] Membri grup
- [x] Pagina lista grupuri
- [x] Pagina detalii grup
- [x] Adaugare/stergere membri din grup
- [x] Legare grup de evenimente
- [x] Attendance filtrat pe grup
- [x] Link din profil persoana catre grupurile din care face parte

## Milestone 3: Follow-up pastoral

Scop: sa ajute conducerea bisericii sa urmareasca persoanele care au nevoie de contact, integrare sau ingrijire pastorala.

- [ ] Task-uri pe persoana, nu doar pe evenimente
- [ ] Note pastorale private
- [ ] Lista vizitatori noi
- [ ] Lista persoane absente de X saptamani
- [ ] Lista persoane fara telefon/adresa
- [ ] Lista zile de nastere apropiate
- [ ] Status follow-up: `NEW`, `CONTACTED`, `IN_PROGRESS`, `DONE`
- [ ] Dashboard cu follow-up-uri urgente

## Milestone 4: Rapoarte

Scop: raportare operationala si pastorala pe baza datelor deja colectate.

- [x] Raport prezenta pe perioada
- [ ] Raport prezenta pe eveniment
- [x] Raport persoane pe categorii: membri/copii/prieteni
- [ ] Raport persoane inactive
- [x] Raport evenimente dupa status/tip
- [x] Raport financiar pe luna
- [ ] Export CSV pentru rapoartele principale
- [ ] Export PDF, optional dupa CSV

## Milestone 5: Comunicare

Scop: comunicare targetata catre persoane, grupuri si voluntari.

- [ ] Email catre o persoana
- [ ] Email catre grup
- [ ] Email catre lista filtrata
- [ ] Template-uri email
- [ ] Istoric comunicare pe persoana
- [ ] Reminder email pentru evenimente
- [ ] SMS/WhatsApp doar ca etapa ulterioara, daca se alege un provider

## Milestone 6: Event registrations

Scop: inscrieri la evenimente si pregatirea pentru check-in/QR.

- [ ] Activare inscrieri pe eveniment
- [ ] Formular intern de inscriere
- [ ] Campuri custom simple
- [ ] Limita de locuri
- [ ] Lista de asteptare
- [ ] Lista participanti
- [ ] URL public/privat pentru inscriere
- [ ] QR code pentru eveniment

## Milestone 7: Finance upgrade

Scop: zona financiara mai apropiata de nevoile reale ale unei biserici.

- [ ] Categorii/fonduri: zeciuiala, misiune, constructie, ajutorare, administrativ
- [ ] Bugete pe categorie/fond
- [ ] Tranzactii legate de biserica activa
- [ ] Raport lunar/anual
- [ ] Export CSV
- [ ] Chitante/declaratii contributii

## Backlog tehnic

- [x] Fragment comun pentru head/header/nav
- [ ] Curatare dark mode: decizie finala intre setare globala din `settings` si toggle local
- [ ] Impartire `app.css` in fisiere mai mici
- [ ] Traduceri complete pentru toate paginile
- [ ] Validari server-side uniforme pentru formulare
- [ ] Testare manuala end-to-end pentru toate rutele migrate
- [ ] Persistenta preferintelor de tabel: `size`, `scrollOnly`, sortare

## Istoric implementari

- 2026-04-20: Creat planul incremental si stabilit milestone-ul 1 ca urmator pas: Attendance pentru evenimente.
- 2026-04-20: Layout-ul comun Thymeleaf pentru head/header/nav este deja implementat si marcat ca finalizat in backlog-ul tehnic.
- 2026-04-20: Implementat milestone-ul 1 Attendance: entitate/repository/service, migrare DB, sectiune de prezenta in editarea evenimentului, salvare bulk si sumar pe dashboard.
- 2026-04-20: Refactor Attendance dupa decizie noua: scos din editarea evenimentului, mutat in pagina separata `/attendance` cu tabel persoane, selector zi, selector program si bifa de prezenta.
- 2026-04-20: Adaugata componenta cu programele de prezenta inregistrate si selectie rapida catre detaliile cu prezenti/absenti.
- 2026-04-20: Schimbat selectia programului inregistrat: detaliile prezenta/absenta se incarca intr-un pop-up.
- 2026-04-20: Adaugata cautare in pop-up-ul de prezenta si scos tabelul vechi de bifare din pagina `/attendance`.
- 2026-04-20: Reintrodusa selectarea explicita zi/program in `/attendance` si editarea/salvarea prezentei direct din pop-up, inclusiv pentru programe noi.
- 2026-04-20: Adaugate teste automate pentru `AttendanceService` si deduplicare defensiva a `personIds` la salvarea prezentei.
