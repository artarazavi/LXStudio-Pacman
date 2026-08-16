# Agent Guide for LXStudio-Pacman

This file is guidance for AI coding agents working in this repository. It is intended to be safe to commit publicly.

## Working Copy

- Use the repository checkout that the operator is actively running in IntelliJ/Chromatik.
- A common local location for that checkout is:
  - `~/Desktop/LXStudio-Pacman`
- Do not move active implementation work into throwaway worktrees unless the operator explicitly asks for that.
- Before assuming a pattern is missing, verify both:
  - the source file exists in the repository
  - the pattern is registered in `te-app/src/main/java/heronarts/lx/studio/TEApp.java`

## Optional Hive Memory

This project may have an external Obsidian knowledge base, referred to as the Pacman hive.

- Expected local location:
  - `~/Desktop/pacman-2026-hive`
- The hive may be synced through Google Drive and then downloaded or copied back to the Desktop.
- If the hive folder exists, use it as durable project memory for meaningful setup, architecture, debugging, mapping, MIDI, shader, and pattern-development work.
- If the hive folder does not exist, continue normally without it.
- Do not create a replacement hive inside this repository unless explicitly asked.
- Do not commit the hive contents into this repository.

## Optional Codex Obsidian Skills

If Codex Obsidian skills are available, use them when working with the hive. Prefer the skill workflow over ad-hoc note writing.

Useful skills may include:

- `wiki`
- `wiki-cli`
- `obsidian-markdown`
- `wiki-query`
- `wiki-ingest`
- `save`
- `wiki-lint`

If these skills are not available, do not pretend to use them. Fall back to normal repository work and clearly state that hive automation is unavailable.

## Handoff Discipline

Long debugging sessions can lose important context. Preserve durable state outside short-term chat memory.

When a non-trivial lesson, decision, or debugging result is discovered:

- Write down what changed.
- Write down what was verified.
- Write down what failed or was ruled out.
- Write down what remains uncertain.
- Prefer updating the hive, when available, while the context is fresh.

This is especially important after regressions, hardware tests, fixture mapping changes, Art-Net investigations, and shader/pattern migration experiments.

## Safety Rules

- Do not modify framework or core runtime behavior when the task is only pattern work unless the operator explicitly asks for framework changes.
- Preserve merged baseline functionality such as custom blend modes unless explicitly asked to remove it.
- Keep original Arta patterns intact as reference implementations. Build experimental or `v2` patterns in parallel.
- Treat live car mapping and fixture changes as high-risk. Prefer scripts, reports, backups, and reproducible migrations over hand edits.
- Do not commit generated `.lxp.backup_*` files unless there is a specific reason.

## Pacman Mapping Debugging

The Pacman fixture/project mapping has historically had subtle Art-Net and byte-order issues. Prefer evidence-based tools over visual guessing.

Important scripts live in `useful_scripts/`:

- `analyze_artnet_tcpdump.py`
- `migrate_pacman_byte_orders_after_channel_fix.py`
- `repair_pacman_byte_orders_from_baseline.py`
- `report_pacman_strip_classification.py`
- `classify_pacman_row_byte_order.py`

The main health check is:

```bash
python3 useful_scripts/repair_pacman_byte_orders_from_baseline.py te-app/Projects/BM2024_Pacman.lxp
python3 useful_scripts/report_pacman_strip_classification.py
```

A healthy state should report zero repair corrections and no rows requiring attention.

## Pattern Development

- Inspect existing Titanic's End patterns before inventing new rendering approaches.
- Prefer established TE rendering/control conventions where practical.
- For shader or `v2` work, keep the implementation incremental and testable.
- When debugging real-world LED output, separate these concerns:
  - logical pattern color
  - LX mixer/effects state
  - fixture output mapping
  - Art-Net packet bytes
  - controller/strip physical behavior

## Commit Hygiene

Before preparing a PR:

- Run relevant compile/check commands.
- Review `git status --short`.
- Exclude generated backups, local captures, temporary logs, and private hive files.
- Include durable scripts/docs that are needed to reproduce or validate the change.
