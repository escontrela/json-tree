# Design Adaptation Audit — 2026-06-26

## Source

- Design baseline: `DESIGN.md`
- Platform: JavaFX

## Summary

The current UI does not follow the new BMW-inspired design system.

Main gaps:

- The app still uses a dark neon palette with purple glow accents instead of the light canvas plus BMW blue system.
- Typography is still monospaced across the shell, while the new design standard expects a corporate editorial hierarchy with strong display weights and light body copy.
- Buttons, pills, cards, and containers are heavily rounded, while the new design standard is mostly rectangular with minimal corner radius.
- The main screen and history screen still read like a compact developer utility instead of a measured automotive-style interface built from clear bands, cards, and structural spacing.
- The ASCII tree itself should remain monospace for alignment, but it must be framed inside the new system instead of driving the whole app typography.

## Implementation Guidance

- Apply the BMW design language to shell chrome, navigation, buttons, lists, spacing, and status components.
- Keep the ASCII tree content monospace as a justified functional exception for alignment and readability.
- Prefer CSS tokenization in JavaFX so both `main.fxml` and `history.fxml` can share the same palette and component vocabulary.
- Avoid a partial recolor. The migration should explicitly remove the current purple glow / rounded-pill language.

## Ticket Strategy

The adaptation can be delivered in up to five tickets:

1. Theme tokens and typography foundation.
2. Main window shell and action bars.
3. ASCII viewer card and status presentation.
4. History screen redesign.
5. Interaction states and JavaFX polish.
