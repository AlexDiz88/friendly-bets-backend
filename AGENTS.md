# AGENTS.md

## Git workflow (Cursor Cloud)

- **Default branch:** `dev`. Commit and push changes directly to `dev` unless the user asks otherwise.
- Do not create feature branches or open PRs into `main` by default; target `dev` for this environment.
- Before starting work: `git checkout dev && git pull origin dev`.
