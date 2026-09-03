# Dreamweaver's Bed texture template

`dreamweavers_bed_template.png` is the editable 64x64 working file. It is an exact copy of Minecraft 1.21.1's white-bed UV layout, so repaint pixels in place without moving, resizing, or rotating any islands.

`vanilla_white_bed_reference.png` is an untouched reference copy. Keep it unchanged so you can recover the original shading and UV boundaries.

## Painting rules

- Keep the canvas at exactly 64x64 pixels.
- Use hard-edged, nearest-neighbor pixel tools. Disable smoothing and antialiasing.
- Preserve transparency outside the existing UV islands.
- Repaint the existing cloth, pillow, mattress sides, underside, and legs in place.
- Test the shared seam between the head and foot halves in game.
- Save the finished texture as `dreamweavers_bed.png` without indexed-color conversion.

The current mod still renders Dreamweaver's Bed through vanilla's white-bed material. The finished custom texture will need a small renderer/material hook before the game can select it independently without replacing every vanilla white bed.
