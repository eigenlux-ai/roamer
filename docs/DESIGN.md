# Roamer Design System

This document outlines the visual structure, color system, typography, and component conventions implemented in `ui/theme/`.

## 1. Design Direction

The UI is designed as a clean, functional developer tool. Visual hierarchy and clarity are prioritized over decorative elements.

## 2. Color Palette (`Color.kt` / `Theme.kt`)

### Primary (Harbor Blue)
| Role | Light | Dark |
| --- | --- | --- |
| `primary` | `#0E6BA8` | `#8ECBF0` |
| `onPrimary` | `#FFFFFF` | `#04283F` |
| `primaryContainer` | `#CDE5F6` | `#0C4C76` |
| `onPrimaryContainer` | `#06344F` | `#CDE5F6` |

### Tertiary / Accent (Brass Amber)
Used for status chips, overridden highlights, and accent icons.
| Role | Light | Dark |
| --- | --- | --- |
| `tertiary` | `#9A6800` | `#F0C069` |
| `tertiaryContainer` | `#F6E0B4` | `#6A4E12` |

### Neutral Surfaces
- **Light Theme**: Background `#F5F8FA`, Surface `#FFFFFF`, Surface Variant `#E3EAF0`, Outline `#74808B`
- **Dark Theme**: Background `#0C1117`, Surface `#121A21`, Surface Variant `#3F4A54`, Outline `#89939D`
- **Text Color**: Light `#111A22` / Dark `#E1E7ED` (`onSurface`)

### Color Rules
- Dynamic wallpaper colors (`dynamicColor`) are disabled by default to maintain consistent visual identity.
- Contrast ratios adhere to WCAG AA guidelines (body text on surface >= 4.5:1, button text >= 5:1).

## 3. Typography (`Type.kt`)

- UI text uses system default sans-serif fonts (Roboto / Noto Sans CJK).
- Technical parameter values (MCC, MNC, ISO, subId) use `FontFamily.Monospace` via the custom `RoamerCode` text style to visually distinguish raw data from human-readable copy.

## 4. Spacing & Components

- Spacing scale: `4dp`, `8dp`, `12dp`, `16dp`, `24dp`, `32dp`.
- Corner radii: Cards `16dp`, Buttons `12dp`, Chips `8dp`.
- Single-column layout optimized for mobile screens.
- Status chips indicate active states (Amber for overridden, Green for success, Red for error).

## 5. Accessibility

- Dual light and dark themes matching system preferences.
- State indicators combine color changes with explicit icons and text.
- Touch targets maintain minimum sizes of 48dp.
