# Product Overview

## Target Audience

Android developers and QA engineers who need to test location- and carrier-dependent app behavior on physical Android devices.

## Primary Objective

Provide a rootless, reliable method to override telephony CarrierConfig properties (country ISO and carrier name) and per-app locales via Shizuku, allowing fast switching between target test environments and clean, baseline-free restoration.

## Core Design Principles

1. **Clear Capability Boundaries**: Faithfully represent what can and cannot be overridden (e.g., MCC and network registration values are explicitly marked read-only).
2. **Distinct Data Display**: Technical data codes (MCC/MNC/ISO) are visually distinct from UI copy via monospace formatting.
3. **Reversible Operations**: Provide immediate restore mechanisms that re-derive baseline state directly from hardware inputs without relying on stored snapshots.
4. **Accessibility Compliance**: Support WCAG AA contrast standards, system dark mode, and touch target accessibility.
