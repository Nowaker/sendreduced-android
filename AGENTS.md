# AGENTS.md

Agent guidance for the **Send Reduced (dev)** fork. See [README.md](README.md)
for the project overview and build instructions.

Also read **@AGENTS.local.md** if it exists — it holds local-only notes (the
device install target and APK archiving) that are not part of the public repo's
guidance.

## Versioning

`versionName` / `versionCode` live in [app/build.gradle](app/build.gradle).
Versioning starts at **2.0.0**. Bump it on every change you ship, and decide
which kind of bump applies:

- **Small fix** (bug fix, tweak, small refactor): bump the patch, **+0.0.1**
  — e.g. `2.0.0 → 2.0.1`.
- **Significant feature**: bump the minor and reset the patch, **+0.1.0**
  — e.g. `2.0.3 → 2.1.0`.

Keep `versionCode` monotonic and in sync with `versionName` using
`major*10000 + minor*100 + patch` (`2.0.0 → 20000`, `2.0.1 → 20001`,
`2.1.0 → 20100`).
