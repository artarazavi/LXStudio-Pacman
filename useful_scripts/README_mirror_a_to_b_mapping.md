# Mirror Strip Mapping Script

Generic script to mirror strip configurations between sides (A ↔ B), handling splits and coordinate mapping.

## Overview

This script takes carefully mapped strips from one side and applies the same configuration to the corresponding strips on the other side. It:

1. **Analyzes source side splits**: Finds which strips are already split into `.5` versions
2. **Applies same splits to target side**: Uses the `split_strip.py` logic to split target strips identically  
3. **Copies X coordinates**: Mirrors source side X coordinate mapping to target side
4. **Preserves target side specifics**: Keeps target side Z coordinates and DMX channel logic intact

## Usage

```bash
python mirror_a_to_b_mapping.py <project_file> <from_side> <to_side> [min_strip] [--single]
```

### Arguments
- **project_file**: Path to the .lxp project file
- **from_side**: Source side (`a` or `b`)
- **to_side**: Target side (`a` or `b`)  
- **min_strip**: Minimum strip number to process (optional, defaults to 1)
- **--single**: Mirror only the specified strip number (optional)

### Examples
```bash
# Mirror A-side to B-side for strips 44+
python mirror_a_to_b_mapping.py BM2024_Pacman.lxp a b 44

# Mirror B-side to A-side for strips 50+  
python mirror_a_to_b_mapping.py BM2024_Pacman.lxp b a 50

# Mirror A-side to B-side for all strips
python mirror_a_to_b_mapping.py BM2024_Pacman.lxp a b

# Mirror only strip 45 from A-side to B-side
python mirror_a_to_b_mapping.py BM2024_Pacman.lxp a b 45 --single
```

## What It Does

### Step 1: Analysis
- Scans source side strips (from min_strip to 67) for existing splits
- Reports which strips have `.5` overflow versions
- Counts total strips to process

### Step 2: Target Side Splitting
- For each split source strip, checks if target side equivalent exists
- If target strip isn't split yet, calls `split_strip.py <to_side> <strip_number>`
- Maintains target side universe and DMX channel logic

### Step 3: Coordinate Mirroring
- Copies source side X coordinates to corresponding target side strips
- Updates both main strip and overflow strip (`.5` version)
- **Preserves** target side Y and Z coordinates (important for offset setups)

## Example Output

```
🔍 Analyzing A-side splits (Strip 44+)...
   ✅ Strip 45: 15 + 138 LEDs
   ✅ Strip 55: 1 + 124 LEDs

📊 Found 2 split A-side strips to mirror
💾 Backup created: BM2024_Pacman.lxp.backup

🔄 Processing Strip 45...
   🔧 Splitting Strip 45b...
   ✅ Strip 45b split successfully
   📍 Copying coordinates...
      Strip 45b: X = 22.0
      Strip 45.5b: X = 22.8

🔄 Processing Strip 55...
   📍 Copying coordinates...
      Strip 55b: X = 33.0
      Strip 55.5b: X = 32.2

✅ Mirror mapping complete!
   Processed 2 strip pairs
   B-side strips now match A-side X coordinates
   B-side Z coordinates and DMX channels preserved
```

## Safety Features

- **Automatic backup**: Creates backup before any changes
- **Error handling**: Skips strips that can't be processed
- **Preservation**: Keeps B-side Z offsets and DMX logic
- **Validation**: Checks strip existence before processing

## What It Doesn't Change

- ❌ **B-side Z coordinates**: Your side offset is preserved
- ❌ **B-side Y coordinates**: Maintains original positioning  
- ❌ **DMX channels/universes**: B-side channel logic untouched
- ❌ **A-side strips**: No changes to your mapped A-side

## Dependencies

- Requires `split_strip.py` to be working and in `useful_scripts/`
- Uses the same regex patterns as other working scripts
- Calls `split_strip.py` as subprocess for B-side splitting

## Use Cases

- **Mirroring mapped sections**: Copy your detailed A-side work to B-side
- **Batch coordinate updates**: Apply X positioning to multiple strip pairs
- **Maintaining consistency**: Ensure A and B sides have matching split patterns

## Technical Details

The script uses the proven patterns from `split_strip.py` and `comprehensive_fixes_logger.py` to:
- Find strips using the working regex pattern
- Parse strip data reliably
- Update coordinates with targeted replacements
- Maintain JSON structure integrity
