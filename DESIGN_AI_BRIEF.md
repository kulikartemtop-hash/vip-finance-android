# VIP Finance — AI Design Brief

## Selected design AI
Google Stitch (Google Labs). It is being used as the design reference/briefing system for the redesign because it supports high-fidelity UI generation from natural language, iterative variants, prototypes, and design-system handoff.

## Prompt used for the VIP Finance redesign

Design a premium Android personal-finance app called VIP Finance. This is an existing Kotlin + Jetpack Compose application; do not redesign it as a generic banking clone. Preserve the existing product functionality and data model while creating a much more expensive, polished, coherent visual identity.

The user wants the app to feel like a premium financial product: sophisticated, calm, precise, luxurious and highly usable. It should look excellent on every screen size and in every selected theme. Avoid cheap-looking gradients, excessive glassmorphism, giant empty dialogs, generic template cards, random colors, and dense form layouts.

### Visual direction
- Premium 2.5D/layered interface with subtle depth, controlled shadows, soft surfaces and deliberate elevation.
- Strong visual hierarchy: large financial numbers, concise labels, clear section headers, generous spacing.
- Modern Material 3 foundations, but with a distinctive VIP Finance identity rather than default Material styling.
- Rounded geometry with consistent corner radii and a disciplined spacing scale.
- Use gradients only as restrained accents in hero surfaces; content areas should remain readable and calm.
- Use semantic colors for income, expenses, transfers, warnings and goals.
- Typography should feel editorial and premium; avoid excessive bold text.
- Icons must be visual, consistent and immediately recognizable.

### Themes
Provide multiple complete visual themes. Every theme must have both light and dark variants and must preserve contrast/readability:
1. Midnight — deep luxury violet/teal.
2. Platinum — cool graphite/silver, restrained and professional.
3. Emerald — premium green financial identity.
4. Royal — violet + warm gold accents.
5. Rose — refined rose + teal accents.
Theme switching must change the complete color system, not just one accent color.

### Personalization
- Users must be able to choose icons for accounts/cards and expense/income categories.
- Users must be able to choose an accent color for accounts and categories.
- Icon/color selection should be visual: icon grid, color swatches and an immediate preview card, not text-only chips.
- Account cards should clearly communicate account type, currency, icon, balance and selected accent.
- Category cards/chips should use the selected icon and color consistently across dashboard, operations, analytics and budgets.

### Core screens to redesign consistently
- Dashboard: premium balance hero, income/expense summary, recent operations, quick actions, meaningful analytics.
- Operations: clean timeline/list, strong amount hierarchy, account/category identity, filters/search.
- Accounts: premium account cards, totals, currency, icon/color identity.
- Categories: visual category management and personalization.
- Budgets: progress visualization, remaining amount, warnings without clutter.
- Analytics: readable charts, period controls, top categories and cash flow.
- Goals: progress, saved amount, target and deadline with motivating but restrained visuals.
- Debts: principal, interest, due date and direction presented clearly.
- Receipts/OCR: simple capture → recognition → confirmation flow.
- Settings: structured premium settings sections and theme preview.

### Dialogs
Dialogs must look like intentional premium surfaces, not default Android popups. Use a designed header/hero area, grouped sections, clear primary action, visual preview and enough content breathing room. Avoid long unstructured columns of outlined text fields.

### Existing functionality that must not be lost
- Accounts/cards and balances.
- Income, expense and transfer operations.
- Categories and custom categories.
- Budgets.
- Debts with interest and due dates.
- Savings goals.
- Reminders.
- Receipts/OCR recognition.
- Multiple currencies and automatic conversion.
- Local smart finance signals.
- JSON backup/export.
- Persistent local data.

### Engineering constraint
The final implementation is Android Kotlin + Jetpack Compose. The design is a visual direction and component specification; production code must remain maintainable, preserve current persistence compatibility, and build successfully into an APK. Do not replace working business logic merely to achieve visual changes.

## Implementation checkpoint
- Backup branch created before redesign: backup/pre-design-ai-redesign-2026-10-03
- Current redesign includes five theme families and visual account/category icon + color editors.
- Next build is mandatory before continuing with further visual changes.