#!/usr/bin/env python3
"""Classify and fix one Pacman physical row from observed primary-color output.

This is the evidence-based companion to the row-span audit. The audit can prove
that a row chain is internally consistent, but only a live primary-color test
can prove whether that row's byteOrder is physically correct.

Example observation:
  current row order is RGB
  expected red appears green
  expected green appears blue
  expected blue appears red

The script computes which payload slot controls each physical channel, derives
the correcting byteOrder, expands the anchor strip to its full physical row
chain, and applies one order to the whole chain.
"""

from __future__ import annotations

import argparse
import json
import re
import shutil
from dataclasses import dataclass
from datetime import datetime
from pathlib import Path


CHANNELS = ("R", "G", "B")
COLOR_TO_CHANNEL = {
    "red": "R",
    "green": "G",
    "blue": "B",
    "r": "R",
    "g": "G",
    "b": "B",
}
VALID_ORDERS = {"RGB", "RBG", "GRB", "GBR", "BRG", "BGR"}


@dataclass(frozen=True)
class Strip:
    fixture: dict
    label: str
    number: float
    side: str
    universe: int
    channel: int
    points: int
    end: int
    byte_order: str


def normalize_color(value: str) -> str:
    key = value.strip().lower()
    if key not in COLOR_TO_CHANNEL:
        raise argparse.ArgumentTypeError(f"expected red/green/blue, got {value!r}")
    return COLOR_TO_CHANNEL[key]


def parse_strips(project: dict) -> list[Strip]:
    strips: list[Strip] = []
    for fixture in project.get("model", {}).get("fixtures", []):
        label = fixture.get("parameters", {}).get("label", "")
        match = re.fullmatch(r"Strip ([0-9.]+)(b?)", label)
        if not match:
            continue
        params = fixture.get("jsonParameters", {})
        if "byteOrder" not in params or "dmxChannel" not in params:
            continue
        channel = int(params["dmxChannel"])
        points = int(params["points"])
        strips.append(Strip(
            fixture=fixture,
            label=label,
            number=float(match.group(1)),
            side="B" if match.group(2) else "A",
            universe=int(params["universe"]),
            channel=channel,
            points=points,
            end=channel + points * 3 - 1,
            byte_order=str(params["byteOrder"]),
        ))
    return strips


def physical_row_chains(strips: list[Strip]) -> list[tuple[Strip, ...]]:
    by_key = {(strip.side, strip.number): strip for strip in strips}
    chains: list[tuple[Strip, ...]] = []
    for main in strips:
        if main.channel != 1 or main.end >= 510 or main.number % 1 != 0:
            continue
        tail = by_key.get((main.side, main.number + 1))
        if not (
            tail
            and tail.universe == main.universe
            and tail.channel == main.end + 1
            and tail.points <= 40
        ):
            continue
        chain: list[Strip] = [main, tail]
        continuation = by_key.get((tail.side, tail.number + 0.5))
        if (
            continuation
            and tail.end == 510
            and continuation.universe == tail.universe + 1
            and continuation.channel == 1
        ):
            chain.append(continuation)
        chains.append(tuple(chain))
    return sorted(chains, key=lambda chain: (chain[0].side, chain[0].number))


def chain_for_anchor(chains: list[tuple[Strip, ...]], anchor: str) -> tuple[Strip, ...]:
    for chain in chains:
        if any(strip.label == anchor for strip in chain):
            return chain
    raise SystemExit(f"No physical row chain contains anchor strip {anchor!r}")


def derive_correcting_order(current_order: str, observed: dict[str, str]) -> str:
    if current_order not in VALID_ORDERS:
        raise SystemExit(f"Unsupported current byteOrder: {current_order}")

    slot_to_physical: dict[int, str] = {}
    for logical_channel, physical_channel in observed.items():
        slot = current_order.index(logical_channel)
        slot_to_physical[slot] = physical_channel

    if set(slot_to_physical) != {0, 1, 2} or set(slot_to_physical.values()) != set(CHANNELS):
        raise SystemExit(f"Observed mapping is not a full RGB permutation: {observed}")

    corrected = ["?"] * 3
    for slot, physical_channel in slot_to_physical.items():
        # Put each logical channel into the slot that drives the matching
        # physical channel.
        corrected[slot] = physical_channel

    new_order = "".join(corrected)
    if new_order not in VALID_ORDERS:
        raise SystemExit(f"Derived invalid byteOrder: {new_order}")
    return new_order


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("project", type=Path, nargs="?", default=Path("te-app/Projects/BM2024_Pacman.lxp"))
    parser.add_argument("--anchor", required=True, help='Any strip label in the observed bad physical row, e.g. "Strip 27.5"')
    parser.add_argument("--red-appears", required=True, type=normalize_color)
    parser.add_argument("--green-appears", required=True, type=normalize_color)
    parser.add_argument("--blue-appears", required=True, type=normalize_color)
    parser.add_argument("--apply", action="store_true")
    parser.add_argument("--no-backup", action="store_true")
    args = parser.parse_args()

    project = json.loads(args.project.read_text())
    strips = parse_strips(project)
    chain = chain_for_anchor(physical_row_chains(strips), args.anchor)
    current_orders = {strip.byte_order for strip in chain}
    if len(current_orders) != 1:
        raise SystemExit(
            "Refusing to classify a row chain with mixed current byteOrder values: "
            + ", ".join(f"{strip.label}={strip.byte_order}" for strip in chain)
        )

    current_order = next(iter(current_orders))
    observed = {
        "R": args.red_appears,
        "G": args.green_appears,
        "B": args.blue_appears,
    }
    new_order = derive_correcting_order(current_order, observed)

    print(f"Project: {args.project}")
    print(f"Mode: {'APPLY' if args.apply else 'DRY RUN'}")
    print("Observed logical -> physical mapping:")
    print(f"  red   -> {observed['R']}")
    print(f"  green -> {observed['G']}")
    print(f"  blue  -> {observed['B']}")
    print(f"Current row byteOrder: {current_order}")
    print(f"Correcting byteOrder: {new_order}")
    print("Physical row chain:")
    for strip in chain:
        print(
            f"  {strip.label:10s} u{strip.universe:03d} ch{strip.channel:03d}-{strip.end:03d} "
            f"pts{strip.points:03d}: {strip.byte_order} -> {new_order}"
        )

    if args.apply:
        if not args.no_backup:
            stamp = datetime.now().strftime("%Y%m%d_%H%M%S")
            backup = args.project.with_suffix(args.project.suffix + f".backup_row_byte_order_classification_{stamp}")
            shutil.copy2(args.project, backup)
            print(f"Backup written: {backup}")
        for strip in chain:
            strip.fixture["jsonParameters"]["byteOrder"] = new_order
        args.project.write_text(json.dumps(project, indent=2) + "\n")
        print(f"Updated: {args.project}")
    else:
        print("Dry run only. Re-run with --apply to write changes.")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
