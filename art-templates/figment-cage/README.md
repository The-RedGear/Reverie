# Figment Cage texture source

The Figment Cage is a simple cube with the same texture on all six faces.

Its five 16×16 RGBA textures represent the complete charge sequence:

- `Figment_Cage_0.png` — empty, emits light level 0
- `Figment_Cage_1.png` — one charge, emits light level 2
- `Figment_Cage_2.png` — two charges, emits light level 4
- `Figment_Cage_3.png` — three charges, emits light level 6
- `Figment_Cage_4.png` — fully charged, emits light level 8

The production copies are stored under `src/main/resources/assets/reverie/textures/block/` using lowercase names. `Cage.psd` and `Sprite-0001.aseprite` are the editable source files.
