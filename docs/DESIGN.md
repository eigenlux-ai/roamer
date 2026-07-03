# Roamer Design System (DESIGN.md)

> Status: **implemented in v1**. Realized in `ui/theme/` (Color / Theme / Type); builds and previews directly.
> Register: **product** (a tool; design serves function). Strategy: **restrained** (neutral surfaces + a single brand color + amber accent ≤10%).

---

## 1. Mood

**"Harbor at Dusk · Departure."** The brand seed color sits at hue ~230° (Harbor Blue), evoking nautical instruments, brass dials, setting off and roaming — a direct match for the name Roamer. The tool's professionalism and restraint come from structure and information hierarchy; the "roaming" soul comes from the Harbor Blue primary and the Brass Amber accent, **not** from a flashy background or passport skeuomorphism.

**Explicitly avoided** (anti-references):
- TikTok-style flashy purple/neon lead-gen looks (the Garcia ims marketing feel)
- Android Studio default Material purple, the instantly recognizable template
- Toy-like, over-rounded cartoon feel
- Screens full of bouncy/sliding show-off motion

---

## 2. Color (implemented in `Color.kt` / `Theme.kt`)

### Brand primary · Harbor Blue
| Role | Light | Dark |
|------|-------|------|
| primary | `#0E6BA8` | `#8ECBF0` |
| onPrimary | `#FFFFFF` | `#04283F` |
| primaryContainer | `#CDE5F6` | `#0C4C76` |
| onPrimaryContainer | `#06344F` | `#CDE5F6` |

### Accent · Brass Amber (= tertiary) — "overridden / active" highlight
| Role | Light | Dark |
|------|-------|------|
| tertiary | `#9A6800` | `#F0C069` |
| tertiaryContainer | `#F6E0B4` | `#6A4E12` |

> Brass is used only for **status chips, emphasis labels, and icons**, never for body text (light `#9A6800` is only ~4.6:1 on white — enough for labels/large text, not for body).

### Support · Slate Blue (secondary) / neutral surfaces / semantic error
- **Light surfaces**: bg `#F5F8FA` · surface `#FFFFFF` · surfaceVariant `#E3EAF0` · outline `#74808B`
- **Dark surfaces**: bg `#0C1117` · surface `#121A21` · surfaceVariant `#3F4A54` · outline `#89939D`
- **Body ink**: light `#111A22` / dark `#E1E7ED` (onSurface); secondary onSurfaceVariant light `#45525C` / dark `#BEC9D2`
- **Error**: light `#BA1A1A` / dark `#FFB4AB`; **Success**: light `#2E7D57` / dark `#6FD3A3`

### Key decisions
- **dynamicColor off by default**: no wallpaper-derived colors, ensuring consistent branding across devices (can be enabled explicitly).
- **Contrast met**: body onSurface vs surface ≥ 12:1; secondary text ≥ 4.5:1; white text on the primary button ≥ 5:1. Placeholders do not use light gray.

---

## 3. Typography (implemented in `Type.kt`)

- **Only 2 font families**: the system sans-serif (Roboto / Noto Sans CJK) carries the UI; `FontFamily.Monospace` carries technical code values. Zero font assets, builds directly.
- **All technical code values are monospace**: MCC / MNC / ISO / subId / MCCMNC all use the exported `RoamerCode` style (e.g. `46007`, `cn`, `subId=3`). **Machine-readable values are visually separated from human copy** — this is the key touch that gives the "technical tool" feel and improves scanning.
- **Hierarchy**: headlineSmall 24 / titleLarge 20 / titleMedium 16 / titleSmall 15 / body 16·14 / label 14·12·11, using size + weight (Normal/Medium/SemiBold) for contrast rather than stacking fonts.

---

## 4. Layout & spacing

- **Spacing scale** (dp): `4 · 8 · 12 · 16 · 24 · 32`. Card inner padding 16, gap between cards 12, gap between sections 24.
- **Cards are the primary container**, but **cards are never nested**; same-level information uses dividers/whitespace, not a second card layer.
- Primarily a single-column vertical scroll (mobile tool); state is shown with chips laid out horizontally.
- Corner-radius scale: cards 16dp, buttons 12dp, chips 8dp — restrained medium radii, no pill-shaped cartoon feel.

---

## 5. Component conventions

| Component | Convention |
|-----------|------------|
| **SIM card** | surface fill + 1dp outline; title titleLarge; code values use `RoamerCode`; "overridden" uses a Brass chip |
| **Status chip** | overridden = tertiaryContainer (amber), applied-success = Success, failure = error, not-ready = surfaceVariant |
| **Primary button** (apply) | filled, primary Harbor Blue; labels are "verb + object" (e.g. "Apply", "Restore default") |
| **Secondary button** (restore) | outlined / tonal |
| **Dropdown** (country/carrier) | Material3 `ExposedDropdownMenuBox`; use a popover layer to avoid being clipped by the card's `clip` |
| **Read-only field** (MNC) | clearly grayed out + "read-only" note, visually distinct from editable fields |
| **Shizuku not ready** | top status banner + guidance button, no interrupting dialog |

---

## 6. Motion (restrained)

- Only **functional motion**: state-change crossfades, chip state changes, list-item staggered entrance.
- Easing is ease-out (exponential curve), **no bounce, no elastic**.
- Override-applied / restore uses a brief color transition (→ Brass → back) for feedback, ≤ 250ms.
- Respect the system "reduce motion" setting: degrade to instant switch or plain fade.

---

## 7. Icons & imagery

- Line icons (Material Symbols Outlined), 1.5–2dp stroke, consistent with the restrained character.
- A very small amount of "roaming" imagery (heading/compass/signal) may be used as accents, only in empty states or brand slots; **do not overuse**, avoid skeuomorphism.

---

## 8. Accessibility

- Light/dark dual themes, following the system.
- All contrast meets WCAG AA (body 4.5:1, large text 3:1).
- State is **never conveyed by color alone**: chips carry text/icons (color-blind friendly).
- Touch targets ≥ 48dp; supports reduced motion.

---

## 9. Open questions & possible future adjustments

1. **Visual direction**: the shipped default is "developer tool · restrained technical feel + Harbor Blue roaming soul." If an explicit travel / boarding-pass style is wanted (direction B), the amber can be dialed up, with ticket notches and map imagery introduced.
2. **Light/dark**: default follows the system (both are implemented).
3. **Brand primary**: Harbor Blue (seed hue 230). Can be nudged ±10° toward cyan or indigo if desired.
