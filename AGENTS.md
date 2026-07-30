# AGENTS.md

## Git workflow (Cursor Cloud — environment `friendly-bets-FULLSTACK`)

- **Environment:** `friendly-bets-FULLSTACK` (Friendly Bets fullstack: backend + frontend).
- **Default branch:** `dev`. Always work on `origin/dev` unless the user explicitly asks otherwise.
- **Before starting work:** `git checkout dev && git pull origin dev`.
- **Commits:** commit and push directly to `dev` (`git push origin dev`). Do not accumulate work only on feature branches.
- **Do not** create feature branches (`cursor/...`) or open PRs into `main` by default in this environment.
- **PRs:** if a PR is needed, target `dev` as the base branch, not `main`.
