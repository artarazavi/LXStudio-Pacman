#!/usr/bin/env python3
"""
Migrate Pacman strip byteOrder values after fixing the one-byte Art-Net offset.

Background:
  Pacman strip dmxChannel values were authored as human DMX channels (1-based),
  but LX fixture output channel is a zero-based byte offset. Before the fixture
  fix, every strip was output one byte late. The physical byte groups therefore
  saw a one-byte rotation of the configured byteOrder.

  After changing PacmanStrip.lxf from:
    channel: "$dmxChannel"
  to:
    channel: "$dmxChannel-1"
  the channel phase is correct, but the byteOrder values that were calibrated
  under the old one-byte-late behavior must be rotated to preserve the visual
  color calibration.

Default mode is a dry run. Pass --apply to write changes.
"""

from __future__ import annotations

import argparse
import json
import shutil
from collections import Counter
from datetime import datetime
from pathlib import Path
import re


# ByteOrder names are payload order names. Old physical grouping saw:
#   [previous byte 2, current byte 0, current byte 1]
# For a uniform test color this is a rotate-right of the configured order.
ROTATE_TO_PRESERVE_OLD_VISUAL = {
    "RGB": "BRG",
    "RBG": "GRB",
    "GRB": "BGR",
    "GBR": "RGB",
    "BRG": "GBR",
    "BGR": "RBG",
}


DMX_BYTES_PER_PIXEL = 3
DMX_MAX_CHANNELS_USED = 510


def is_pacman_strip(fixture: dict) -> bool:
    label = fixture.get("parameters", {}).get("label", "")
    component = fixture.get("componentType", "")
    fixture_type = fixture.get("fixtureType", "")
    return (
        isinstance(label, str)
        and label.startswith("Strip")
        and (
            "PacmanStrip" in component
            or "PacmanStrip" in fixture_type
            or "jsonParameters" in fixture
        )
        and "byteOrder" in fixture.get("jsonParameters", {})
        and "dmxChannel" in fixture.get("jsonParameters", {})
    )


def load_project(path: Path) -> dict:
    return json.loads(path.read_text())


def save_project(path: Path, project: dict) -> None:
    # Match the existing project style closely enough to keep diffs reviewable.
    path.write_text(json.dumps(project, indent=2) + "\n")


def strip_identity(fixture: dict) -> tuple[str, float, str] | None:
    label = fixture.get("parameters", {}).get("label", "")
    match = re.fullmatch(r"Strip ([0-9.]+)(b?)", label)
    if not match:
        return None
    side = "B" if match.group(2) else "A"
    return label, float(match.group(1)), side


def detect_split_row_mismatches(project: dict) -> list[tuple[str, int, str, str]]:
    """Find row-split fixtures whose old byte-order labels disagree.

    These rows are not safe for blind global migration. A typical split row is a
    large fixture starting at channel 1, followed by a tiny tail fixture that
    continues the same universe to channel 510.
    """
    rows: list[dict] = []
    for fixture in project.get("model", {}).get("fixtures", []):
        if not is_pacman_strip(fixture):
            continue
        identity = strip_identity(fixture)
        if identity is None:
            continue
        label, number, side = identity
        params = fixture["jsonParameters"]
        channel = int(params["dmxChannel"])
        points = int(params["points"])
        rows.append({
            "label": label,
            "number": number,
            "side": side,
            "universe": int(params["universe"]),
            "channel": channel,
            "points": points,
            "end": channel + points * 3 - 1,
            "byteOrder": str(params["byteOrder"]),
        })

    mismatches: list[tuple[str, int, str, str]] = []
    by_side = {"A": [], "B": []}
    for row in rows:
        by_side[row["side"]].append(row)

    for side_rows in by_side.values():
        side_rows.sort(key=lambda row: row["number"])
        by_number = {row["number"]: row for row in side_rows}
        for main in side_rows:
            tail = by_number.get(main["number"] + 1)
            if tail is None:
                continue
            is_split_row = (
                main["channel"] == 1
                and main["end"] < 510
                and tail["universe"] == main["universe"]
                and tail["channel"] == main["end"] + 1
                and tail["points"] <= 40
            )
            if not is_split_row:
                continue

            chain = [main, tail]
            continuation = by_number.get(tail["number"] + 0.5)
            if (
                continuation is not None
                and tail["end"] == 510
                and continuation["universe"] == tail["universe"] + 1
                and continuation["channel"] == 1
            ):
                chain.append(continuation)

            orders = [row["byteOrder"] for row in chain]
            if len(set(orders)) > 1:
                labels = " + ".join(row["label"] for row in chain)
                order_text = "/".join(orders)
                mismatches.append((labels, main["universe"], order_text, "physical row chain"))

    return mismatches


def should_rotate_for_old_one_byte_late_phase(dmx_channel: int) -> bool:
    """Return whether the old output phase could fit at least one pixel.

    Before the fixture fix, the human-authored 1-based DMX channel was handed to
    LX as a zero-based byte offset. Most strips therefore started one byte late
    and need a byteOrder rotation after the channel fix.

    But if the old start left fewer than 3 bytes in that universe, LX could not
    fit even one RGB pixel and overflowed the entire strip to the next universe
    at channel 0. Those strips did not experience the same one-byte physical
    phase and must keep their old byteOrder.
    """
    available_old_bytes = DMX_MAX_CHANNELS_USED - dmx_channel
    return available_old_bytes >= DMX_BYTES_PER_PIXEL


def migrated_byte_order(old: str, dmx_channel: int) -> str:
    if old not in ROTATE_TO_PRESERVE_OLD_VISUAL:
        raise ValueError(f"Unsupported byteOrder {old!r}")
    if not should_rotate_for_old_one_byte_late_phase(dmx_channel):
        return old
    return ROTATE_TO_PRESERVE_OLD_VISUAL[old]


def migrate(project: dict) -> list[tuple[str, int, int, str, str]]:
    changes: list[tuple[str, int, int, str, str]] = []
    for fixture in project.get("model", {}).get("fixtures", []):
        if not is_pacman_strip(fixture):
            continue
        params = fixture["jsonParameters"]
        label = fixture.get("parameters", {}).get("label", "<unknown>")
        old = str(params["byteOrder"])
        channel = int(params["dmxChannel"])
        new = migrated_byte_order(old, channel)
        if old != new:
            universe = int(params["universe"])
            changes.append((label, universe, channel, old, new))
            params["byteOrder"] = new
    return changes


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "project",
        type=Path,
        nargs="?",
        default=Path("te-app/Projects/BM2024_Pacman.lxp"),
        help="Pacman .lxp project file",
    )
    parser.add_argument("--apply", action="store_true", help="Write the migrated project file")
    parser.add_argument("--no-backup", action="store_true", help="Do not create a timestamped backup on --apply")
    parser.add_argument("--force", action="store_true", help="Allow running even when the project looks already migrated")
    parser.add_argument(
        "--allow-split-mismatches",
        action="store_true",
        help="Apply generic migration even when split physical rows have mismatched old byte orders",
    )
    args = parser.parse_args()

    project_path = args.project
    project = load_project(project_path)

    before = Counter()
    for fixture in project.get("model", {}).get("fixtures", []):
        if is_pacman_strip(fixture):
            before[str(fixture["jsonParameters"]["byteOrder"])] += 1

    # This script is a one-time migration from the old offset-bug calibration.
    # Refuse likely second runs by default; otherwise a rerun rotates every strip
    # again and makes debugging much worse.
    migrated_only_orders = {"BGR", "BRG"}
    if not args.force and (set(before) & migrated_only_orders):
        print(f"Project: {project_path}")
        print("Refusing to run: this project already looks migrated.")
        print(f"Current byteOrder counts: {dict(sorted(before.items()))}")
        print("Use --force only if you intentionally want another rotation.")
        return 2

    split_mismatches = detect_split_row_mismatches(project)
    if split_mismatches and not args.allow_split_mismatches:
        print(f"Project: {project_path}")
        print("Refusing to run: split-row byteOrder mismatch detected.")
        print("These rows are not safe for blind global migration because one physical row is split across two fixtures with different old byte orders.")
        for labels, universe, order_text, mismatch_kind in split_mismatches:
            print(f"  {labels} u{universe:03d}: {order_text} ({mismatch_kind})")
        print("Resolve these rows with targeted physical calibration, or pass --allow-split-mismatches to apply the generic rule anyway.")
        return 3

    changes = migrate(project)

    after = Counter()
    for fixture in project.get("model", {}).get("fixtures", []):
        if is_pacman_strip(fixture):
            after[str(fixture["jsonParameters"]["byteOrder"])] += 1

    print(f"Project: {project_path}")
    print(f"Mode: {'APPLY' if args.apply else 'DRY RUN'}")
    print(f"Pacman strips changed: {len(changes)}")
    print(f"Before byteOrder counts: {dict(sorted(before.items()))}")
    print(f"After byteOrder counts:  {dict(sorted(after.items()))}")
    print()
    print("Migration rule:")
    for old, new in ROTATE_TO_PRESERVE_OLD_VISUAL.items():
        count = sum(1 for change in changes if change[3] == old)
        if count:
            print(f"  {old} -> {new}: {count}")
    if split_mismatches:
        print("Split-row mismatches allowed:")
        for labels, universe, order_text, mismatch_kind in split_mismatches:
            print(f"  {labels} u{universe:03d}: {order_text} ({mismatch_kind})")

    print()
    print("First 30 changes:")
    for label, universe, channel, old, new in changes[:30]:
        print(f"  {label:>12} u{universe:03d} ch{channel:03d}: {old} -> {new}")

    if args.apply:
        if not args.no_backup:
            stamp = datetime.now().strftime("%Y%m%d_%H%M%S")
            backup = project_path.with_suffix(project_path.suffix + f".backup_byte_order_migration_{stamp}")
            shutil.copy2(project_path, backup)
            print()
            print(f"Backup written: {backup}")
        save_project(project_path, project)
        print(f"Updated: {project_path}")
    else:
        print()
        print("Dry run only. Re-run with --apply to write changes.")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
