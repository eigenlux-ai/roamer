# Product

## Register

product

## Users

For **developers and technical users** debugging region/carrier-related app logic on **their own Android devices**. Use case: quickly switch the local SIM's home region (country ISO) and carrier display name to a target region, observe how a target app behaves, then restore once verified. Prerequisite: Shizuku is installed and authorized (or trigger via adb/Termux).

## Product Purpose

Override the system CarrierConfig without root to simulate a "SIM in a different country/carrier" environment for on-device debugging. Success criteria: pick a SIM → pick a target region/carrier → apply in one tap → the target app reflects the change → restore in one tap, all in real time, reliably, and reversibly. The capability boundary is honest and transparent (only the ISO / carrier name are changed; MCC, on-network truth, and the real phone number are not).

## Brand Personality

Professional, restrained, trustworthy, with a faint "departure / roaming" travel character. Three words: **precise · grounded · en route**. Emotional goal: it should feel like a tool that understands the low-level internals, is worth trusting, and does not play tricks — not a marketing-driven lead-gen app.

## Anti-references

- TikTok-style flashy purple/neon lead-gen looks (e.g. Garcia ims marketing cards)
- Android Studio default Material purple, the instantly recognizable template feel
- Toy-like, over-rounded cartoon character
- Screens full of bouncy/sliding show-off motion
- Overblown promises (claiming it can change the real phone number or fool every app)

## Design Principles

1. **Honest boundaries**: the UI faithfully shows what can and cannot be changed (MNC marked read-only; no promise of a real phone number).
2. **Code-value separation**: machine-readable values (MCC/MNC/ISO) use a monospace font to distinguish them from human copy, for at-a-glance scanning.
3. **Reversible first**: every override comes with an explicit restore, with clear state (overridden / restored).
4. **Restraint in service of function**: visuals and motion serve clarity and trust; they never upstage the content.
5. **Professional yet distinctive**: identity comes from the Harbor Blue brand color and structure, not from following defaults or piling on decoration.

## Accessibility & Inclusion

WCAG AA: body contrast ≥ 4.5:1, large text ≥ 3:1. Light/dark dual themes follow the system. State is never conveyed by color alone (chips carry text/icons, color-blind friendly). Touch targets ≥ 48dp. Respect the system "reduce motion" setting, degrading to instant/fade transitions.
