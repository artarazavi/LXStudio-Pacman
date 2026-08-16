#!/usr/bin/env python3
"""
Analyze a tcpdump text export of Art-Net traffic and summarize universe payloads.

Expected input:
  tcpdump -nn -vv -X -r capture.pcap > capture.txt

This script extracts ArtDMX packets, decodes universe/sequence/length, and can
show the byte window for a specific strip from the Pacman project file.
"""

from __future__ import annotations

import argparse
import json
import re
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable


ARTNET_HEADER = b"Art-Net\x00"
ARTNET_HEADER_LEN = 18
UNIVERSE_LSB = 14
UNIVERSE_MSB = 15
DATA_LENGTH_MSB = 16
DATA_LENGTH_LSB = 17
SEQUENCE_INDEX = 12


@dataclass
class Packet:
    universe: int
    sequence: int
    data: bytes


@dataclass
class StripInfo:
    label: str
    universe: int
    channel: int
    points: int
    byte_order: str

    @property
    def start_index_one_based(self) -> int:
        return self.channel

    @property
    def start_index_zero_based(self) -> int:
        return self.channel - 1

    @property
    def span(self) -> int:
        return self.points * 3


def parse_tcpdump_text(path: Path) -> list[Packet]:
    packets: list[Packet] = []
    current_bytes = bytearray()

    hex_line = re.compile(r"^\s*0x[0-9a-f]+:\s+((?:[0-9a-f]{4}\s+)+)", re.IGNORECASE)
    packet_header = re.compile(r"^\d\d:\d\d:\d\d\.\d+\s+IP\b")

    def flush() -> None:
        nonlocal current_bytes
        header_index = current_bytes.find(ARTNET_HEADER)
        if header_index < 0:
            current_bytes = bytearray()
            return
        if header_index > 0:
            current_bytes = current_bytes[header_index:]
        if len(current_bytes) < ARTNET_HEADER_LEN:
            current_bytes = bytearray()
            return
        universe = current_bytes[UNIVERSE_LSB] | (current_bytes[UNIVERSE_MSB] << 8)
        sequence = current_bytes[SEQUENCE_INDEX]
        data_len = (current_bytes[DATA_LENGTH_MSB] << 8) | current_bytes[DATA_LENGTH_LSB]
        payload = bytes(current_bytes[ARTNET_HEADER_LEN : ARTNET_HEADER_LEN + data_len])
        packets.append(Packet(universe=universe, sequence=sequence, data=payload))
        current_bytes = bytearray()

    for raw_line in path.read_text(errors="ignore").splitlines():
        if packet_header.match(raw_line):
            if current_bytes:
                flush()
            continue

        match = hex_line.match(raw_line)
        if match is None:
            continue

        hex_groups = match.group(1).split()
        for group in hex_groups:
            if len(group) % 2 != 0:
                continue
            try:
                current_bytes.extend(bytes.fromhex(group))
            except ValueError:
                continue

    if current_bytes:
        flush()

    return packets


def load_project_strips(project_path: Path) -> list[StripInfo]:
    project = json.loads(project_path.read_text())
    strips: list[StripInfo] = []
    for fixture in project["model"]["fixtures"]:
        label = fixture.get("parameters", {}).get("label")
        params = fixture.get("jsonParameters", {})
        if not label or not label.startswith("Strip"):
            continue
        strips.append(
            StripInfo(
                label=label,
                universe=int(params["universe"]),
                channel=int(params["dmxChannel"]),
                points=int(params["points"]),
                byte_order=str(params["byteOrder"]),
            )
        )
    return strips


def summarize_nonzero_spans(data: bytes) -> str:
    spans: list[tuple[int, int]] = []
    start = None
    for idx, value in enumerate(data):
        if value != 0 and start is None:
            start = idx
        elif value == 0 and start is not None:
            spans.append((start, idx - 1))
            start = None
    if start is not None:
        spans.append((start, len(data) - 1))
    if not spans:
        return "all-zero"
    return ", ".join(f"{a}-{b}" for a, b in spans[:8])


def packet_groups(values: Iterable[int], group_size: int = 3) -> str:
    vals = list(values)
    groups = [vals[i : i + group_size] for i in range(0, len(vals), group_size)]
    return " | ".join(",".join(f"{v:03d}" for v in group) for group in groups)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("dump_text", type=Path, help="tcpdump text file produced with -X or -XX")
    parser.add_argument(
        "--project",
        type=Path,
        default=Path("te-app/Projects/BM2024_Pacman.lxp"),
        help="Pacman project file for strip metadata, relative to the repository root by default",
    )
    parser.add_argument("--universe", type=int, default=None, help="Only show packets for this universe")
    parser.add_argument("--strip", type=str, default=None, help="Show byte windows for this strip label")
    parser.add_argument(
        "--packet-index",
        type=int,
        default=0,
        help="Which matching packet to inspect for --strip (default 0 = first)",
    )
    args = parser.parse_args()

    packets = parse_tcpdump_text(args.dump_text)
    if args.universe is not None:
        packets = [p for p in packets if p.universe == args.universe]

    print(f"Found {len(packets)} ArtDMX packets")
    for idx, packet in enumerate(packets[:40]):
        print(
            f"[{idx:03d}] universe={packet.universe:>3} seq={packet.sequence:>3} "
            f"len={len(packet.data):>3} nonzero={summarize_nonzero_spans(packet.data)}"
        )

    if args.strip:
        strips = load_project_strips(args.project)
        strip = next((s for s in strips if s.label == args.strip), None)
        if strip is None:
            raise SystemExit(f"Strip not found: {args.strip}")

        matching = [p for p in packets if p.universe == strip.universe]
        if not matching:
            raise SystemExit(f"No packets found for universe {strip.universe}")
        if args.packet_index >= len(matching):
            raise SystemExit(
                f"Packet index {args.packet_index} out of range for universe {strip.universe} "
                f"(have {len(matching)})"
            )

        packet = matching[args.packet_index]
        print()
        print(
            f"Strip {strip.label}: universe={strip.universe} channel={strip.channel} "
            f"points={strip.points} span={strip.span} byteOrder={strip.byte_order}"
        )

        for base_name, start in (
            ("one-based-start", strip.start_index_one_based),
            ("zero-based-start", strip.start_index_zero_based),
        ):
            lo = max(0, start - 6)
            hi = min(len(packet.data), start + strip.span + 6)
            window = packet.data[lo:hi]
            print(f"{base_name}: bytes[{lo}:{hi}]")
            print(packet_groups(window))
            print()

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
