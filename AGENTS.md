# AGENTS.md

## About this repository

This is a personal fork of [Ivy Wallet](https://github.com/Ivy-Apps/ivy-wallet).
Upstream has not been maintained since Nov 2024. The purpose of this fork is to **redesign the UI**
(Material 3 Expressive). Everything else should stay as close to upstream as possible.

| Area         | Rule                                                                                                                                                 |
|--------------|------------------------------------------------------------------------------------------------------------------------------------------------------|
| UI           | Free to change. This is what the fork is for.                                                                                                        |
| Behavior     | Preserve upstream behavior as much as possible. Do not change business logic, calculations, or user flows unless the task explicitly asks for it.   |
| Data formats | **Must never change.** No exceptions.                                                                                                                |

"Data formats" includes at least:

- The Room database: entities (`shared/data/core/src/main/java/com/ivy/data/db/entity/`), DAOs' table/column
  names, the DB version, migrations, and the exported schemas in `shared/data/core/schemas/`.
- The backup/export format produced and read by `BackupDataUseCase`, including every `@Serializable` class and
  field name it touches.
- The import formats handled by `feature/import-data`.
- DataStore and SharedPreferences keys and value types.

Data written by upstream Ivy Wallet must remain readable by this fork, and vice versa. If a task looks like it
requires a data-format change, stop and ask instead of making it.

## Fork point

The last upstream commit is **`710733db`** (2025-07-18). Every commit after it is fork work. Clones may be
shallow; run `git fetch --unshallow` if the commit is missing. Upstream can also be added as a remote:

```sh
git remote add upstream https://github.com/Ivy-Apps/ivy-wallet.git
```

## When you run into a problem

1. **Check whether it existed before the fork.** For example:
   - `git log 710733db..HEAD -- <path>` to see what the fork changed there.
   - `git diff 710733db -- <path>` or `git show 710733db:<path>` to compare with upstream.
   - Check out `710733db` in a separate worktree and build or run the tests there.
2. **If upstream did not have the problem**, the fork introduced it. Follow upstream's original approach as
   closely as possible rather than inventing a new one. Reverting the fork's changes (fully or partially) is
   allowed; say in the commit message which fork commit is being reverted and why.
3. **If upstream had the same problem**, fix it with the smallest change that respects the behavior and
   data-format rules above.

## Existing guidelines

The upstream developer guidelines still apply. Read the relevant one when you need it. **Treat these files as
read-only**; do not edit them.

| Topic                                        | File                                                                   |
|----------------------------------------------|------------------------------------------------------------------------|
| Overall philosophy (keep it simple)          | [`docs/Guidelines.md`](docs/Guidelines.md)                             |
| Data modeling (ADTs, explicit/exact types)   | [`docs/guidelines/Data-Modeling.md`](docs/guidelines/Data-Modeling.md) |
| Error handling (Arrow `Either`, no throwing) | [`docs/guidelines/Error-Handling.md`](docs/guidelines/Error-Handling.md) |
| Architecture (data / domain / UI layers)     | [`docs/guidelines/Architecture.md`](docs/guidelines/Architecture.md)   |
| Screen architecture (UDF/MVI, Compose VMs)   | [`docs/guidelines/Screen-Architecture.md`](docs/guidelines/Screen-Architecture.md) |
| Unit testing                                 | [`docs/guidelines/Unit-Testing.md`](docs/guidelines/Unit-Testing.md)   |
| CI checks and how to run them locally        | [`docs/CI-Troubleshooting.md`](docs/CI-Troubleshooting.md)             |

The fork's own UI redesign specs and plans live in `docs/superpowers/specs/` and `docs/superpowers/plans/`.

## Commits

- Use semantic commit messages: `<type>(<optional scope>): <summary>`, e.g. `feat(home): ...`, `fix: ...`.
  Types: `feat`, `fix`, `refactor`, `style`, `docs`, `test`, `build`, `ci`, `chore`.
- Keep the message as concise as possible. The body is optional and must not exceed two short paragraphs.
- Do not append session links (e.g. `Claude-Session: ...`) to commit messages.
