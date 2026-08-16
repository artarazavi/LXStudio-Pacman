#!/usr/bin/env python3
"""Repair Pacman byte orders from a pre-migration baseline.

This re-applies the channel-fix migration rule using the current best model:
most strips need the one-byte phase compensation, but strips whose old start
could not fit a complete RGB pixel in the remaining universe bytes keep their
baseline byte order.
"""

from __future__ import annotations

import argparse
import json
import shutil
from datetime import datetime
from pathlib import Path

from migrate_pacman_byte_orders_after_channel_fix import is_pacman_strip, migrated_byte_order


DEFAULT_BASELINE = Path(__file__).with_name("pacman_byte_order_baseline_20260816.json")


def label_for(fixture: dict) -> str:
    return fixture.get("parameters", {}).get("label", "")


def load_baseline(path: Path) -> dict[str, dict]:
    data = json.loads(path.read_text())
    if "strips" in data:
        return {
            str(strip["label"]): {
                "jsonParameters": {
                    "byteOrder": strip["byteOrder"],
                    "dmxChannel": strip["dmxChannel"],
                }
            }
            for strip in data["strips"]
        }
    return {
        label_for(fixture): fixture
        for fixture in data.get("model", {}).get("fixtures", [])
        if is_pacman_strip(fixture)
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("project", type=Path, nargs="?", default=Path("te-app/Projects/BM2024_Pacman.lxp"))
    parser.add_argument(
        "--baseline",
        type=Path,
        default=DEFAULT_BASELINE,
    )
    parser.add_argument("--apply", action="store_true")
    parser.add_argument("--no-backup", action="store_true")
    args = parser.parse_args()

    project = json.loads(args.project.read_text())
    baseline_by_label = load_baseline(args.baseline)

    changes: list[tuple[str, int, int, str, str, str]] = []
    for fixture in project.get("model", {}).get("fixtures", []):
        if not is_pacman_strip(fixture):
            continue
        label = label_for(fixture)
        baseline_fixture = baseline_by_label.get(label)
        if baseline_fixture is None:
            raise SystemExit(f"Missing baseline fixture for {label}")
        params = fixture["jsonParameters"]
        baseline_params = baseline_fixture["jsonParameters"]
        old_order = str(baseline_params["byteOrder"])
        dmx_channel = int(baseline_params["dmxChannel"])
        expected = migrated_byte_order(old_order, dmx_channel)
        current = str(params["byteOrder"])
        if current != expected:
            reason = "no-fit-overflow-keeps-old" if expected == old_order else "phase-rotated"
            changes.append((label, int(params["universe"]), int(params["dmxChannel"]), current, expected, reason))
            if args.apply:
                params["byteOrder"] = expected

    print(f"Project: {args.project}")
    print(f"Baseline: {args.baseline}")
    print(f"Mode: {'APPLY' if args.apply else 'DRY RUN'}")
    print(f"Corrections: {len(changes)}")
    for label, universe, channel, current, expected, reason in changes:
        print(f"  {label:>12} u{universe:03d} ch{channel:03d}: {current} -> {expected} ({reason})")

    if args.apply:
        if not args.no_backup:
            stamp = datetime.now().strftime("%Y%m%d_%H%M%S")
            backup = args.project.with_suffix(args.project.suffix + f".backup_byte_order_repair_{stamp}")
            shutil.copy2(args.project, backup)
            print(f"Backup written: {backup}")
        args.project.write_text(json.dumps(project, indent=2) + "\n")
        print(f"Updated: {args.project}")
    else:
        print("Dry run only. Re-run with --apply to write changes.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
