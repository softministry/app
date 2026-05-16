# i18n Guidelines

This folder contains UI translations used by `src/main/resources/static/js/i18n.js`.

## Files
- `ro.json` Romanian translations
- `en.json` English translations

## Rules
- Keep key structure identical in both files.
- Use the same key naming style everywhere (`camelCase` inside nested sections).
- Prefer concise, UI-ready labels (no backend jargon).
- Keep punctuation/style consistent between languages.

## Validation
Run:

```bash
node scripts/check-i18n.mjs
```

The check fails if any key exists only in one language file.
