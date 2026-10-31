# Interface

FoxSu keeps KernelSU in the kernel and rewrites the manager. This page is about the latter — the part you actually see and touch every day.

> Root in the kernel, experience in the manager.

## Five skins

Switch in Settings → Interface style. The switch is global: home, modules, logs, settings — every screen changes its layout at once, not just a new wallpaper.

| Skin | Layout language | Notes |
| --- | --- | --- |
| **Redesign** (Miuix) | Glass cards + capsule dock | The default, densest layout: one status card plus stats, device and security cards |
| **Editorial** | Magazine layout | Paper background, ink rules, serif headings, native dock that can be swapped for a capsule |
| **Pages** (Fresh) | Generous whitespace | Large titles and sectioned lists for one-handed use |
| **Glass** | Liquid glass | Translucent surfaces and soft highlights; pairs well with a wallpaper |
| **Nocturne** | Dark neon | Dark-first, neon outlines and glowing accents |

All five share the same features and settings, so switching never loses a configuration.

## Twenty-one dock styles

The dock is decoupled from the skin and selected globally. Metrics stay fixed (78dp items, 60dp tall) so the bar never jumps when you switch styles.

`Coral bubble` · `Film strip` · `Neon tube` · `Glass stack` · `Single focus` · `Cloud sticker` · `Slide track` · `Original capsule` · `Expanding pill` · `Side drawer` · `Flowing cursor` · `Neon outline` · `Block cards` · `Solid gradient` · `Top indicator` · `Floating disc` · `Centre avatar` · `Editorial ink` · `White pill` · `Liquid pill` · `Nocturne neon`

The first seven use a different grid: 48dp square hit areas, 6dp inset, and some variants keep a fox avatar in the middle as the donation entry. Side drawer makes the page content give way and can dock left or right.

Buttons come in ten shapes, with selected and unselected being two states of the same shape:

`Pill` · `Circle` · `Squircle` · `Outline` · `Double` · `Cut` · `Keycap` · `Card` · `Glass` · `Glow`

## Forty tap effects

The inside of the dock and its outline are two independent pickers. Use one, or stack them — a breathing outline with bouncing icons, for instance. Both live in Customisation → Dock effects.

### Twenty inside the dock

Applied to the icon and the selected capsule. All but `Gradient flow` are one-shot timelines that stop when finished.

`Jelly slide` · `Bounce landing` · `Jelly shake` · `Spin-in` · `Heartbeat` · `Wave` · `Shine sweep` · `Halo ring` · `Bubbles` · `Stardust` · `Capsule pop` · `Wiggle` · `3D flip` · `Gradient flow` · `Label lift` · `Magnet pull` · `Dot trail` · `Ripple` · `Elastic overshoot` · `Mini firework`

### Twenty on the outline

Applied to the border of the whole dock. The first ten loop continuously; the last ten play once per page change or press.

`Breathing glow` · `Marquee dot` · `Border flow` · `Rainbow stroke` · `Dashed rotation` · `Flowing glow` · `Tap ripple` · `Selection flash` · `Stroke breathing` · `Electric stroke` · `Follow halo` · `Sweep` · `Shockwave` · `Selection spot` · `Selection orbit` · `Glint` · `Elastic frame` · `Shadow lift` · `Edge sparks` · `Soundwave`

## Twenty dock icon sets

Only the shape changes. Colouring still follows the accent colour and selection state, so swapping icons never breaks the palette.

`Architectural section` · `Origami` · `Map top-down` · `Pixel 8-bit` · `Sticky note` · `Molecular` · `Gauge ticks` · `Overlapping rings` · `Fault slice` · `Crayon doodle` · `Block grid` · `Contour lines` · `Soundwave` · `Building blocks` · `Stamp` · `Document folder` · `Wireframe` · `Folding fan` · `Ripple` · `Star trail`

## Customisation centre

Every option is wired to real settings and applies immediately, no restart needed.

### Wallpaper

- Pick from the gallery, with drag-to-crop and zoom
- Dim it separately so foreground text stays readable
- Switch the global background off entirely and go back to a flat colour

### Cards

- **Artwork**: separate pictures for the hero card, the stat cards and each module card, with per-item opacity
- **Colours**: a global tint plus per-card overrides, and one-tap rainbow cards
- **Radius**: one global corner radius

### Fonts

Four presets — system default, Noto Serif SC, JetBrains Mono and LXGW WenKai — plus importing your own font file.

### Title and icon

- Custom text and colour for the home title
- Five fox launcher icons plus the original KernelSU icon, following light/dark mode
- Ten dock button shapes, swapped as a whole row

### Ambience

Cat paws, small hearts and four-point stars drift up from the bottom of the screen on the home screen, the donate page and the customisation header card. It has its own switch, ignores the motion level, and never takes pointer events, so taps pass through.

### Light

Immersive glow, liquid glass, rainbow icons and Monet tinting are independent switches — turn on just one or two if that is all you want.

## See also

- [Install FoxSu](guide/installation)
- [App profile and permissions](guide/app-profile)
- [Module development guide](guide/module)
