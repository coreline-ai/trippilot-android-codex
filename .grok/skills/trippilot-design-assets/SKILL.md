---
name: trippilot-design-assets
description: Create and install TripPilot local design assets for approved empty-state slots only. Use when the user asks for 디자인 에셋, empty state 이미지, 앱 일러스트, VectorDrawable 저장, or runs /trippilot-design-assets.
---

# TripPilot design assets

Make only the empty-state illustrations listed in [references/allowed-slots.json](references/allowed-slots.json). Save them as local SVG + VectorDrawable. Do not generate city photos, icons, hero replacements, or remote images.

## Before anything else

1. Read `design/design-direction.md`, `docs/asset-manifest.md`, and `references/allowed-slots.json`.
2. If the requested slot is not in the catalog, refuse. Suggest a new plan instead of inventing a slot.
3. Functional icons stay in `design/tokens.md` Material Symbols. Do not emit icon PNG/SVG packs.

## Format rules

- Default: original 240×160 SVG in `design/assets/`, then a matching VectorDrawable in `app/src/main/res/drawable/`.
- Use token colors from `design/tokens.json` light palette. Do not invent a second palette.
- No `<image href>`, external `use`, web fonts, or city/flag/map/brand artwork.
- Raster is out of scope for this skill version. Do not call image generation tools.
- Artwork is decorative. Screen meaning stays in text and semantics.

## Save sequence

1. Write the SVG to `design/assets/staging/<slot-id>/` first if the user has not approved it.
2. After approval, copy the SVG to the catalog `source` path.
3. Write the VectorDrawable to the catalog `android` path. Do not overwrite another slot's file.
4. Run `python3 .grok/skills/trippilot-design-assets/scripts/register_asset.py --slot <id>`.
5. Run `python3 .grok/skills/trippilot-design-assets/scripts/install_android_asset.py --slot <id>`.
6. Point Compose `EmptyState` at the catalog `drawable` name. Do not change Room, OAuth, or navigation.
7. Run `python3 scripts/verify_phase0_design.py`.

`install_android_asset.py` copies only an already-registered VectorDrawable and refuses a missing manifest hash.

## After install

If the empty screen of 준비, 예약, or 출처 changed, say that `design/visual-baseline.md` review sequence 5–6 (and golden `01` only if the trip list empty art changed) should be re-checked. Do not auto-update goldens.
