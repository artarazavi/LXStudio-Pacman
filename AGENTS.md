# LXStudio-Pacman Startup

Start narrow in this repository.

## First Reads

- Read this file first.
- If `/Users/arta/Desktop/pacman-2026-hive/` exists, then read `/Users/arta/Desktop/pacman-2026-hive/wiki/meta/Agent Startup.md`.
- After that, open only the one repo file or one hive handoff note needed for the current task.

## Do Not Read On Startup

- `README.md` in full.
- `te-app/Projects/`
- `te-app/Autosave/`
- `te-app/target/`
- `repo/`
- `.git/`
- `utils/*.app/`
- large media, `.lxp`, transcript, or archive files.

## Preferred Tactics

- Use `rg`, `git diff --name-only`, `git diff --stat`, and targeted line ranges.
- For pattern work, inspect the exact pattern file and `te-app/src/main/java/heronarts/lx/studio/TEApp.java` registration only if needed.
- Update the hive with one current handoff note during long or iterative debugging so future sessions do not re-solve the same problem.
