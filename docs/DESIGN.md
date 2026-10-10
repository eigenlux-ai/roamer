# Roamer Design System

This document describes the visual system implemented in `ui/theme/`, `ui/Glass.kt`, and `ui/Controls.kt`.

## 1. Design Direction

Roamer follows the Liquid Glass language of iOS 27, with Brass Amber as its single highlight color on cool, blue-tinted neutrals. The interface has two layers:

- **Content layer**: solid, grouped cards with large corner radii on a softly lit ground. Everything the user reads or edits lives here.
- **Control layer**: glass elements that float above the content and blur what scrolls beneath them. The toolbar, the Restore all bar, and result messages live here.

Glass is reserved for the control layer. Content is never placed on glass.

System behavior stays native to Android: the system Back gesture, edge-to-edge insets, and TalkBack roles all work as on any Android app. Only the visual language is Apple's.

## 2. Color Palette (`Color.kt` / `Theme.kt`)

### Accent (Brass Amber)
| Role | Light | Dark |
| --- | --- | --- |
| `primary` / `tertiary` (text and icon accent) | `#9A6800` | `#F0C069` |
| `primaryContainer` (filled buttons) | `#F2B84B` | `#F0C069` |
| `onPrimaryContainer` | `#3A2A00` | `#3A2A00` |
| `tertiaryContainer` (overridden badge, active chip) | `#F6E0B4` | `#6A4E12` |

Amber is the one highlight color. It marks what the user can act on (the main button of a group, toolbar icons, plain buttons, selection marks) and what is currently overridden (the ring around a card, the Overridden badge, the active quick-switch chip, the second ambient glow).

### Ambient (Harbor Blue)
| Role | Light | Dark |
| --- | --- | --- |
| ambient glow | `#0E6BA8` | `#8ECBF0` |

Blue is no longer an accent. It only tints the neutral surfaces and the ground glow, which gives the amber a cool field to stand out against.

### Success
| Role | Light | Dark |
| --- | --- | --- |
| success | `#2E7D57` | `#6FD3A3` |
| success container | `#D5EFE0` | `#1F4D38` |
| on success container | `#0B3D26` | `#BFEBD3` |

Used for the Shizuku status, successful results, and the on state of a toggle.

### Neutral Surfaces
All neutrals are tinted toward the brand blue. The full Material surface-container ramp is defined so that no component falls back to the default purple-tinted baseline.

| Role | Light | Dark |
| --- | --- | --- |
| `background` | `#F5F8FA` | `#0C1117` |
| `surface` (cards) | `#FFFFFF` | `#121A21` |
| `surfaceContainer` (inset groups) | `#EAEFF3` | `#19222B` |
| `surfaceContainerHigh` (menus) | `#E3EAF0` | `#222C36` |
| `surfaceBright` (segmented thumb) | `#FFFFFF` | `#2F3A45` |
| `onSurface` | `#111A22` | `#E1E7ED` |
| `onSurfaceVariant` | `#45525C` | `#BEC9D2` |

### Ground
The backdrop behind the content is the background color with a fixed radial glow of the ambient blue at the top left. While any SIM is overridden, an amber glow fades in at the top right. The ground does not scroll, so the glass toolbar always has color to refract.

### Color Rules
- Dynamic wallpaper colors are disabled to keep the brand palette.
- Body text keeps a contrast of at least 4.5:1 on its surface, including on glass.
- Text on a tinted container uses the matching `on…Container` role, never the accent color itself.

## 3. Glass Material (`Glass.kt`)

Glass is built from three parts, following the iOS 27 revision of Liquid Glass:

1. **Diffusion**: a 28dp backdrop blur with a tint of about 65% opacity, strong enough to keep text legible over any content.
2. **Specular highlight**: a bright line along the top edge.
3. **Darkened edge**: a dark line along the lower edge, plus a soft drop shadow, to separate the glass from the content.

| Element | Treatment |
| --- | --- |
| Toolbar | Transparent at the top of the list. Becomes uniform glass across the full width once content scrolls beneath it. |
| Toolbar title | Centered, 17sp semibold. A page's back chevron or the main screen's Settings icon sits on the left, actions on the right. |
| Toolbar buttons | Bare icons with no container. Actions (refresh) use the amber accent; navigation (back, settings) uses the neutral text color. |
| Restore all bar | Glass bar with a `16dp` radius that springs in from the bottom while a SIM is overridden. |
| Result message | Glass pill above the bottom edge. Skipped when the log section is already on screen. |

Backdrop blur uses the Haze library. Menus open in their own window and cannot blur the app behind them, so they use an opaque surface container instead.

Controls that sit on solid content (secondary buttons, chips, the search field) are flat: a soft fill with no rim, blur, or shadow.

### Shadows
Elevation shadows are not used anywhere; their hard directional edge reads as Material. Glass elements cast one soft drop shadow (24dp blur, 8dp down, low opacity). The switch and segmented thumbs cast a smaller one (6dp blur, 2dp down). Buttons cast none.

## 4. Typography (`Type.kt`)

- UI text uses the system sans-serif (Roboto / Noto Sans CJK).
- The screen title is 22sp bold with slightly tight tracking. Section headers are 17sp semibold.
- Technical values (MCC, MNC, ISO, subId, iccId) use `FontFamily.Monospace` through the `RoamerCode` style.
- All sizes are in `sp`. Rows must hold up at a font scale of 1.3 and in English, where labels are longer: values truncate with an ellipsis and never wrap into a neighboring element.

### Layout across languages
English copy runs much longer than Chinese, so widths are never tuned to one language:

- In the Original / Current rows, the label column and the carrier column take the measured width of their longest text. The country name gets everything that is left.
- A menu row shows the country name without its ISO code. A value that still does not fit shrinks from 16sp down to 13sp before it is ellipsized. The code stays visible in the menu and on the Override button.
- Titles and footnotes that wrap use balanced line breaks, so no single word is left on its own line.

## 5. Shape and Spacing (`Shape.kt`)

- Spacing scale: `4dp`, `8dp`, `12dp`, `16dp`, `24dp`, `32dp`. Sections are separated by `32dp`.
- Corner radii: cards `26dp`, summary tiles and the log `22dp`, inset groups `14dp`.
- Controls are rounded rectangles, not capsules: buttons, chips, and the search field `12dp`; the segmented track `10dp` and its thumb `8dp`; badges `8dp`.
- Only the toggle switch is fully round.

## 6. Controls (`Controls.kt`)

All controls follow one language. Material components with a recognizable Material look (Switch, SegmentedButton, Checkbox, FilterChip, filled and text buttons) are not used.

| Control | Form |
| --- | --- |
| Primary button | Rounded rectangle filled with the bright amber, dark text. |
| Soft button | Flat rounded rectangle with a soft fill, for secondary actions and chips. |
| Plain button | Text in the amber accent, no container. |
| Toolbar icon button | Bare icon; amber for actions, neutral for navigation. |
| Toggle switch | Compact `46×28dp` track with a round thumb; green when on. |
| Segmented control | Rounded track with a thumb that slides to the selection. |
| Menu row | Label on the left, current value and a chevron on the right; opens a menu with a check mark on the current choice. |
| Check circle | Filled check when selected, empty ring otherwise. |
| Group row | Row of an inset grouped list (`ui/GroupedList.kt`): solid surface, `20dp` corners on the first and last row, hairline between rows. |
| Editable row | Group row with a drag handle behind while its list is in edit mode. An entry that can be removed shows a red minus button in front; a preset carrier shows a toggle instead. Adding uses a green plus button. |
| Tag | Small neutral label beside a row title. "Custom" marks every region or carrier the user defined. A carrier that is turned off is dimmed and labeled "Off". |

Every tappable control keeps a touch target of at least 48dp.

### Settings pages
Settings pages share one layout: the ground, a scrolling grouped list, and the toolbar with a back chevron and a centered title. A list that can be edited puts **Edit** in the toolbar; outside edit mode a row navigates, inside it rows can be added, removed, and dragged into order.

Edits are a draft and do not take effect until saved. In edit mode the toolbar shows **Done** while nothing has changed. Once the draft differs from the saved list, it shows **Revert** (neutral color, drops the changes and stays in edit mode) and **Save** (accent color, applies them and leaves edit mode). Leaving the page drops an unsaved draft. A dragged row follows the finger, its neighbors slide aside, and the list scrolls when the row is held near an edge.

### Confirmation
Only an action that cannot be undone asks for confirmation, in an alert with two equal buttons: a neutral Cancel and a red confirming button. Its row in the list is labeled in the same red. Resetting the configuration is the one such action today.

## 7. Motion and Feedback

- **No ripple.** Buttons scale down slightly and dim while pressed; rows show a flat highlight.
- Position changes use springs: the switch thumb, the segmented thumb, the Restore all bar, and press scale.
- Values that an override or restore changes crossfade in place.
- Pages stack over the main screen (`ui/ScreenStack.kt`): a page slides in from the right, and the system Back gesture drags it along with the finger, revealing the main screen beneath with a slight parallax and dim. Releasing completes the return or settles the page back. The Back button and the toolbar's back chevron play the same motion.
- A completed operation gives a confirm or reject haptic.
- The progress indicator is a 2dp line on the bottom edge of the toolbar; it never shifts the content.

## 8. Accessibility

- Light and dark themes are both first-class and follow the system by default.
- State is never carried by color alone: overridden cards also show a badge and an icon, and results show an icon and a label.
- Custom controls expose the matching semantic role (switch, radio button, checkbox, button).
