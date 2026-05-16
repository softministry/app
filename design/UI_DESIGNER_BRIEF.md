# UI Designer Brief - MinistryProject

Acest document este versiunea non-tehnică pentru design (UX/UI), derivată din documentația completă.

Referință tehnică completă: `design/UI_DOCUMENTATION.md`

## 1) Context produs
- Aplicație de administrare biserică (membri, grupuri, evenimente, prezență, follow-up pastoral, finanțe).
- Aplicație multi-church: utilizatorul poate schimba biserica activă din header.
- Majoritatea paginilor sunt server-rendered și includ formulare clasice + acțiuni inline.

## 2) Ecrane principale (ce face fiecare)

### Authentication
- `Login`: autentificare user.
- `Forgot Password`: resetare parolă prin întrebări de securitate.
- `Logged Out`: confirmare ieșire din cont.

### Dashboard
- Panou de overview: recomandări pastorale, inbox, aniversări, acțiuni rapide.
- Ecran orientat pe prioritizare și follow-up rapid.

### Persons
- `Persons List`: căutare și navigare persoane.
- `Person Form`: creare/editare profil persoană.
- `Person Details`: profil complet, istoric relevant, notițe private cu acces pe roluri.

### Families
- `Family View`: vizualizare relații familiale.

### Groups
- `Groups List`: listă + filtrare + editare rapidă grupuri.
- `Group Form`: formular dedicat creare/editare.
- `Group View`: detalii grup și membri.

### Attendance
- `Attendance Index`: alegere dată/sesiune/grup.
- `Attendance Details`: marcarea prezenței (bulk, listă persoane).

### Events
- `Events List`: listare, filtrare, sortare, paginare.
- `Event Form`: creare/editare eveniment (status, prioritate, recurență, reminders).
- `Event Tasks`: task-uri în cadrul evenimentului.
- `Move Event`: mutare eveniment între biserici.

### Calendar
- `Calendar View`: vizualizare temporală evenimente.

### Visits
- `Visits List`: listă vizite pastorale.
- `Visit Form`: creare/editare vizită.

### Follow-ups
- `Follow-ups List`: board/list de cazuri pastorale cu status/stage.
- `Follow-up Form`: creare/editare caz, date contact, next steps.

### Finance
- `Finance`: registru tranzacții (venit/cheltuială) + adăugare rapidă.

### Reports
- `Reports`: rapoarte pe interval (from/to), cu export.

### Preaching
- `Preaching Assistant`: formular de generare structură predică.
- Sub-secțiune pentru trimitere Kindle.

### Church
- `Church View`: profil biserică activă.
- `Church Form`: editare date biserică + avatar.

### Settings
- `Appearance`: teme + customizări vizuale/status labels.
- `Church`: biserică implicită + profil biserică.
- `System`: setări aplicație, import/export, runtime mode.
- `Pastoral`: reguli/scoruri/template-uri recomandări pastorale.

### Account
- `Account Security`: schimbare parolă + actualizare întrebări securitate.

### Admin Users
- `Users Admin`: creare/editare/ștergere utilizatori și roluri.

## 3) Câmpuri cheie pe domenii

### Identitate persoană
- Nume, prenume, tip membru, dată naștere, telefon, rol în biserică, adresă, poziție.

### Relații
- Soț/soție, copii, apartenență la grupuri.

### Grupuri
- Nume grup, tip grup, lider, membri, descriere.

### Evenimente
- Nume, tip, status, prioritate, descriere, dată, responsabil, grup, recurență, reminders.

### Follow-up pastoral
- Persoană, grup, status, metodă contact, last/next contact date, notițe.

### Finanțe
- Tip tranzacție, nume, descriere, sumă.

### Biserică/organizație
- Nume, adresă, avatar/logo, pastor, secretar, casier (+ telefoane).

### Securitate
- Username, parolă, rol, răspunsuri întrebări securitate.

## 4) Zone cu prioritate mare pentru redesign
- Dashboard (densitate mare de informație + acțiuni rapide).
- Event Form (complexitate ridicată, multe secțiuni).
- Settings (foarte multe opțiuni într-un singur template).
- Follow-ups List (board + acțiuni multiple pe item).
- Persons Details (mix de informații, note private, follow-up).

## 5) Recomandări UX pentru designer
- Clarificare ierarhie vizuală pentru acțiunile primare vs secundare.
- Pattern consistent pentru formulare mari (sections, progressive disclosure).
- Standardizare componente list/filter/toolbar între pages.
- Unificare tratament pentru statusuri și priorități (color system + chips).
- Optimizare mobile pentru pagini dense (Dashboard, Events, Settings).

## 6) Livrabile sugerate pentru design
- Design system mini: culori, tipografie, spacing, form controls, tables, chips.
- Wireframe + UI high fidelity pentru:
  - Dashboard
  - Persons (List + Form + Details)
  - Events (List + Form)
  - Follow-ups (List + Form)
  - Settings (tabbed sections)
- Prototype pentru flow-uri cheie:
  - Create person -> assign group -> create follow-up
  - Create event -> add tasks -> recurrence/reminder
  - Dashboard alerts -> resolve actions
