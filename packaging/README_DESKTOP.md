# Church Administration Platform Desktop Packaging

Acest pachet pregătește distribuția local-first pentru end user:

- JRE inclus prin `jpackage`
- instalator nativ (`.msi` / `.dmg` / `.deb`)
- profil Spring `desktop` (SQLite local + backup automat + deschidere browser)

## 1. Build aplicație

Din `teamleaf_approuch`:

```bash
mvn -DskipTests package
```

## 2. Generează installer

```bash
./packaging/build-desktop-package.sh
```

Scriptul alege tipul de pachet după OS:

- macOS -> `dmg`
- Linux -> `deb`
- Windows -> `msi`

Fișierul rezultat este în:

`teamleaf_approuch/packaging/dist`

### macOS (script dedicat)

Pe macOS poți folosi direct:

```bash
./packaging/build-macos-dmg.sh
```

Opțional poți suprascrie metadate:

```bash
APP_VERSION=0.1.0 APP_IDENTIFIER=ro.churchoffice.churchadministrationplatform ./packaging/build-macos-dmg.sh
```

Rezultatul este un fișier `.dmg` în `packaging/dist`.

### macOS signed + notarized (recomandat pentru distribuție)

Scriptul suportă semnare și notarizare prin variabile de mediu:

```bash
SIGN_APP=true \
SIGNING_IDENTITY="Developer ID Application: Nume Prenume (TEAMID)" \
NOTARIZE_DMG=true \
APPSTORE_PROFILE="notary-profile" \
APP_VERSION=0.1.0 \
./packaging/build-macos-dmg.sh
```

Alternativ, fără profil salvat în keychain:

```bash
SIGN_APP=true \
SIGNING_IDENTITY="Developer ID Application: Nume Prenume (TEAMID)" \
NOTARIZE_DMG=true \
APPLE_ID="you@example.com" \
TEAM_ID="TEAMID" \
APP_PASSWORD="app-specific-password" \
APP_VERSION=0.1.0 \
./packaging/build-macos-dmg.sh
```

Scriptul folosește iconul aplicației din `src/main/resources/static/img/church-tab.svg` și generează automat:

- `packaging/icons/MinistryAdmin.icns` (icon app)
- `packaging/icons/MinistryAdmin-volume.icns` (icon volum DMG)

## 3. Comportament runtime (profil desktop)

La pornire, aplicația folosește automat:

- DB local: `${user.home}/ChurchAdministrationPlatform/data/ministryadmin.sqlite.db`
- Uploads: `${user.home}/ChurchAdministrationPlatform/uploads`
- Backups: `${user.home}/ChurchAdministrationPlatform/backups`
- Logs: `${user.home}/ChurchAdministrationPlatform/logs/ministryadmin.log`

La startup:

- pornește pe `http://localhost:8085`
- deschide browserul automat pe `http://localhost:8085/dashboard`
- rulează backup automat la intervalul configurat

## Windows (script PowerShell dedicat)

Pe Windows, rulează din root-ul proiectului:

```powershell
powershell -ExecutionPolicy Bypass -File .\packaging\build-windows-msi.ps1
```

Pentru o variantă self-contained, fără Java instalat pe PC-ul țintă, folosește:

```powershell
powershell -ExecutionPolicy Bypass -File .\packaging\build-windows-portable.ps1
```

Rezultatul este un `app-image` cu runtime inclus, plus un ZIP distributabil în `packaging\dist`.

Opțional poți suprascrie metadate:

```powershell
powershell -ExecutionPolicy Bypass -File .\packaging\build-windows-msi.ps1 `
  -AppVersion "0.1.0" `
  -AppName "Church Administration Platform" `
  -AppVendor "Church Administration Platform"
```
