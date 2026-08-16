#!/usr/bin/env python3
"""Report Pacman strip/physical-row byte-order classification state.

This does not pretend to know the physical byte order of a strip from JSON
alone. It walks every Pacman strip, groups the split physical row chains that
can be inferred from universe/channel continuity, compares the current project
against a baseline backup when provided, and highlights rows that need live
primary-color classification.
"""

from __future__ import annotations

import argparse
import json
import re
from collections import Counter
from dataclasses import dataclass
from pathlib import Path

from migrate_pacman_byte_orders_after_channel_fix import (
    DMX_BYTES_PER_PIXEL,
    DMX_MAX_CHANNELS_USED,
    migrated_byte_order,
    should_rotate_for_old_one_byte_late_phase,
)


DEFAULT_BASELINE = Path(__file__).with_name("pacman_byte_order_baseline_20260816.json")


@dataclass(frozen=True)
class Strip:
    label: str
    number: float
    side: str
    universe: int
    channel: int
    points: int
    end: int
    byte_order: str
    reverse: bool
    x: float
    y: float


def parse_project(path: Path) -> list[Strip]:
    project = json.loads(path.read_text())
    if "strips" in project:
        return sorted(
            [
                Strip(
                    label=str(strip["label"]),
                    number=float(re.fullmatch(r"Strip ([0-9.]+)(b?)", str(strip["label"])).group(1)),
                    side="B" if str(strip["label"]).endswith("b") else "A",
                    universe=int(strip["universe"]),
                    channel=int(strip["dmxChannel"]),
                    points=int(strip["points"]),
                    end=int(strip["dmxChannel"]) + int(strip["points"]) * 3 - 1,
                    byte_order=str(strip["byteOrder"]),
                    reverse=bool(strip.get("reverse", False)),
                    x=0,
                    y=0,
                )
                for strip in project["strips"]
                if re.fullmatch(r"Strip ([0-9.]+)(b?)", str(strip["label"]))
            ],
            key=lambda s: (s.side, s.y, s.number, s.channel),
        )

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
        transform = fixture.get("parameters", {})
        strips.append(Strip(
            label=label,
            number=float(match.group(1)),
            side="B" if match.group(2) else "A",
            universe=int(params["universe"]),
            channel=channel,
            points=points,
            end=channel + points * 3 - 1,
            byte_order=str(params["byteOrder"]),
            reverse=bool(params.get("reverse", False)),
            x=float(transform.get("x", 0)),
            y=float(transform.get("y", 0)),
        ))
    return sorted(strips, key=lambda s: (s.side, s.y, s.number, s.channel))


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
    return sorted(chains, key=lambda c: (c[0].side, c[0].y, c[0].number))


def chain_key(chain: tuple[Strip, ...]) -> tuple[str, float]:
    return chain[0].side, chain[0].number


def expected_orders_from_baseline(chain: tuple[Strip, ...], baseline_by_label: dict[str, Strip]) -> list[str]:
    expected: list[str] = []
    for strip in chain:
        baseline = baseline_by_label.get(strip.label)
        expected.append(migrated_byte_order(baseline.byte_order, baseline.channel) if baseline else "?")
    return expected


def migration_reason(baseline: Strip | None) -> str:
    if baseline is None:
        return "no-baseline"
    if should_rotate_for_old_one_byte_late_phase(baseline.channel):
        return "phase-rotated"
    available = DMX_MAX_CHANNELS_USED - baseline.channel
    return f"no-fit-overflow-keeps-old({available}<{DMX_BYTES_PER_PIXEL})"


def classify_chain(chain: tuple[Strip, ...], baseline_by_label: dict[str, Strip]) -> str:
    current = [s.byte_order for s in chain]
    expected = expected_orders_from_baseline(chain, baseline_by_label)
    if "?" in expected:
        return "NO_BASELINE"
    if current == expected:
        return "GENERIC_MIGRATION"
    if len(set(current)) > 1:
        return "MIXED_CURRENT_ROW"
    if len(set(current)) == 1 and len(set(expected)) > 1:
        return "ROW_LEVEL_OVERRIDE"
    return "DIFFERS_FROM_GENERIC"


def print_chain(chain: tuple[Strip, ...], baseline_by_label: dict[str, Strip]) -> None:
    old = [baseline_by_label[s.label].byte_order if s.label in baseline_by_label else "?" for s in chain]
    expected = expected_orders_from_baseline(chain, baseline_by_label)
    current = [s.byte_order for s in chain]
    reasons = [migration_reason(baseline_by_label.get(s.label)) for s in chain]
    labels = " + ".join(s.label for s in chain)
    channels = " + ".join(f"u{s.universe:03d}:ch{s.channel:03d}-{s.end:03d}" for s in chain)
    points = "+".join(str(s.points) for s in chain)
    status = classify_chain(chain, baseline_by_label)
    print(
        f"{chain[0].side} y={chain[0].y:05.1f} {labels:<38} "
        f"pts={points:<11} {channels:<62} "
        f"old={'/'.join(old):<11} expected={'/'.join(expected):<11} "
        f"current={'/'.join(current):<11} {status} reason={'/'.join(reasons)}"
    )


def expected_order_for_strip(strip: Strip, baseline_by_label: dict[str, Strip]) -> tuple[str, str]:
    baseline = baseline_by_label.get(strip.label)
    if baseline is None:
        return "?", "no-baseline"
    return migrated_byte_order(baseline.byte_order, baseline.channel), migration_reason(baseline)


def is_implicit_overflow_strip(strip: Strip, baseline_by_label: dict[str, Strip]) -> bool:
    baseline = baseline_by_label.get(strip.label)
    if baseline is None:
        return False
    return baseline.end > DMX_MAX_CHANNELS_USED and not should_rotate_for_old_one_byte_late_phase(baseline.channel)


def print_single(strip: Strip, baseline_by_label: dict[str, Strip]) -> None:
    baseline_strip = baseline_by_label.get(strip.label)
    old = baseline_strip.byte_order if baseline_strip else "?"
    expected, reason = expected_order_for_strip(strip, baseline_by_label)
    status = "GENERIC_MIGRATION" if strip.byte_order == expected else "DIFFERS_FROM_EXPECTED"
    overflow = " implicit-overflow" if is_implicit_overflow_strip(strip, baseline_by_label) else ""
    print(
        f"{strip.side} y={strip.y:05.1f} {strip.label:<10} "
        f"u{strip.universe:03d}:ch{strip.channel:03d}-{strip.end:03d} "
        f"pts={strip.points:<3} old={old:<3} expected={expected:<3} "
        f"current={strip.byte_order:<3} {status}{overflow} reason={reason}"
    )


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("project", type=Path, nargs="?", default=Path("te-app/Projects/BM2024_Pacman.lxp"))
    parser.add_argument(
        "--baseline",
        type=Path,
        default=DEFAULT_BASELINE,
        help="pre-byte-order-migration baseline manifest or backup used to compute expected orders",
    )
    args = parser.parse_args()

    strips = parse_project(args.project)
    baseline = parse_project(args.baseline) if args.baseline.exists() else []
    baseline_by_label = {strip.label: strip for strip in baseline}
    chains = physical_row_chains(strips)
    chained = {strip.label for chain in chains for strip in chain}
    singles = [strip for strip in strips if strip.label not in chained]

    print(f"Project: {args.project}")
    print(f"Baseline: {args.baseline if args.baseline.exists() else 'missing'}")
    print(f"Pacman strips: {len(strips)}")
    print(f"Physical row chains: {len(chains)}")
    print(f"Single/non-chain strip fixtures: {len(singles)}")
    print(f"Current byteOrder counts: {dict(sorted(Counter(s.byte_order for s in strips).items()))}")
    if baseline:
        print(f"Baseline byteOrder counts: {dict(sorted(Counter(s.byte_order for s in baseline).items()))}")
    print()
    print("Physical row chain walk:")
    for chain in chains:
        print_chain(chain, baseline_by_label)
    print()
    print("Rows requiring attention:")
    for chain in chains:
        status = classify_chain(chain, baseline_by_label)
        if status != "GENERIC_MIGRATION":
            print_chain(chain, baseline_by_label)
    for strip in singles:
        expected, _reason = expected_order_for_strip(strip, baseline_by_label)
        if strip.byte_order != expected:
            print_single(strip, baseline_by_label)
    print()
    print("Implicit universe-overflow strips:")
    for strip in singles:
        if is_implicit_overflow_strip(strip, baseline_by_label):
            print_single(strip, baseline_by_label)
    print()
    print("Non-chain strip walk:")
    for strip in singles:
        print_single(strip, baseline_by_label)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
