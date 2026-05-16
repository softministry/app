# Release TODO

Incremental checklist for the first releasable build.

- [x] Clean repository hygiene
  - Ignore generated artifacts, local IDE files, runtime data, logs, local databases, and packaged output.
  - Remove already-tracked generated artifacts from the release index.
  - Stage source/test files that were previously untracked but are required by the passing build.
- [x] Secure the default admin bootstrap
  - Removed the fixed `admin/admin` startup credential.
  - Added explicit initial admin password support through `ministryadmin.initial-admin-password`.
  - Generate a random temporary password when no explicit password is configured.
  - Covered the bootstrap behavior with tests.
- [x] Review and stage real source/config changes
  - Staged the remaining docs, packaging, source, resource, and test changes for the first release.
  - Kept generated files and local-only state out of the release commit through `.gitignore` and tracked artifact removals.
- [x] Expand CI release checks
  - Run `mvn -B -Punit verify`.
  - Run `mvn -B -Pintegration verify`.
- [x] Prepare release versioning
  - Replaced the web artifact version with `0.1.0`.
  - Updated packaging jar references and default app versions to `0.1.0`.
  - Pinned `ministryadmin-core` to the locally available `0.0.1-SNAPSHOT` until a matching core release exists.
- [x] Desktop/package smoke test
  - Started the `0.1.0` jar with an isolated smoke-test home and verified `/login`.
  - Built the Windows portable app-image/ZIP.
  - Verified the packaged executable starts with the bundled runtime and serves `/login`.
