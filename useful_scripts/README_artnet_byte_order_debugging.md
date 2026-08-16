# Pacman Art-Net Byte-Order Debugging Scripts

These are the scripts worth keeping from the August 2026 Pacman yellow-edge debugging session.

## Quick Health Check

Run these after changing Pacman fixture/output mapping:

```bash
python3 useful_scripts/repair_pacman_byte_orders_from_baseline.py te-app/Projects/BM2024_Pacman.lxp
python3 useful_scripts/report_pacman_strip_classification.py
```

Expected healthy state:

```text
Corrections: 0
Rows requiring attention:
```

`Rows requiring attention:` should be empty.

## Scripts

### `analyze_artnet_tcpdump.py`

Parses a `tcpdump -X` text export of Art-Net traffic. This is how we confirmed the original Pacman output was one byte late on the wire.

Typical capture workflow:

```bash
sudo tcpdump -i en1 -c 200 -w /tmp/pacman-yellow-en1.pcap 'udp and dst host 10.1.1.47 and dst port 6454'
tcpdump -nn -vv -X -r /tmp/pacman-yellow-en1.pcap > /tmp/pacman-yellow-en1.txt
python3 useful_scripts/analyze_artnet_tcpdump.py /tmp/pacman-yellow-en1.txt
```

### `migrate_pacman_byte_orders_after_channel_fix.py`

One-time migration script for the byte-order labels after fixing `PacmanStrip.lxf` from:

```text
channel: "$dmxChannel"
```

to:

```text
channel: "$dmxChannel-1"
```

This should not be run repeatedly. It has guards because repeated rotation corrupts the project.

### `repair_pacman_byte_orders_from_baseline.py`

Recomputes the expected Pacman strip `byteOrder` values from the pre-migration backup:

```text
useful_scripts/pacman_byte_order_baseline_20260816.json
```

This includes the important row-55 rule:

```text
if 510 - dmxChannel < 3, keep the baseline byteOrder
```

That catches implicit universe-overflow strips like `Strip 55` and `Strip 55b`.

The baseline manifest is intentionally committed as a small JSON file so the
repair/report scripts do not depend on local `.lxp.backup_*` files.

### `report_pacman_strip_classification.py`

Walks every Pacman strip and compares current state to the baseline-derived expected state.

This is the main "are all strips fixed?" script.

It reports:

- total Pacman strips
- physical row chains
- current byte-order counts
- rows requiring attention
- implicit universe-overflow strips
- full per-strip walk

### `classify_pacman_row_byte_order.py`

Targeted helper for a future live test where one physical row still has wrong primary colors.

Use it only when we have concrete observed primary-color behavior, such as:

```text
red appears green
green appears blue
blue appears red
```

Do not use this as a first-pass migration tool. The baseline repair/report scripts are safer.

## Removed Dead-End Helpers

The following helpers were intentionally removed after the row-55 fix:

- `audit_pacman_row_spans.py`: superseded by `report_pacman_strip_classification.py`
- `set_pacman_strip_byte_order.py`: too manual and too easy to use as a hardcoded row fix

The rule going forward: prefer evidence-based report/repair scripts over manual per-strip edits.
