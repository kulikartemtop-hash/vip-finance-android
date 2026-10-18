# VIP Finance — DEVELOPMENT ROADMAP

## Purpose
This file is the persistent engineering handoff for continuing VIP Finance across chats. Read this file first before changing the project. Do not infer completion from version numbers alone. A stage is COMPLETE only when its code is implemented, the app builds successfully, the APK artifact exists, and regression checks pass.

## Repository
- GitHub: https://github.com/kulikartemtop-hash/vip-finance-android
- Branch: main
- Package: com.example.vipfinance
- Current version at roadmap creation: 2.4
- Build: GitHub Actions workflow .github/workflows/build-apk.yml
- Rule: after every major block, build APK; if build fails, fix before starting the next block.

## Current baseline
- Existing persistence uses SharedPreferences through FinanceStore.
- Existing modules must remain compatible with saved data.
- Existing themes/styles, accounts, transactions, categories, transfers, budgets, debts, goals, reminders, OCR receipts, settings, menu customization and backup must not be broken.
- Last known successful APK artifact before the next major work must be treated as the rollback baseline.

## Product direction
VIP Finance is a premium personal-finance center, not a simple expense calculator.
Design target: premium 2.5D/layered UI; depth without heavy 3D; strong hierarchy; large financial numbers; useful charts; fast navigation; restrained animation; clear states.

## Research conclusions to apply
Reference products researched: Monarch Money, YNAB, Copilot Money, Rocket Money, Quicken Simplifi.
Adopt principles, not branding or pixel copies:
- Monarch: customizable dashboard, broad financial overview, rich reporting.
- YNAB: explicit budget intent, targets and progress.
- Copilot: polished visual hierarchy, fast daily review, learning categorization.
- Rocket Money: recurring/subscription awareness and savings-oriented automation.
- Simplifi: clear available-to-spend / spending-plan mental model.
Avoid common complaints: overloaded navigation, unclear budgets, excessive paid gating, fragile sync assumptions, tiny numbers, confusing charts, excessive notifications, weak export/backup.

## Status legend
- [x] COMPLETE — implemented + successful APK + regression check.
- [~] IN PROGRESS — actively being developed.
- [ ] TODO — not started.
- [!] BLOCKED — requires investigation/fix before continuing.

# MAJOR STAGES

## 1. UI / Premium Design
Status: [~] IN PROGRESS — 6.6.0 visual refresh submitted; completion depends on successful release and regression checks.
Completed in 6.6.0 source changes:
- [x] Refined shared shape and typography system for consistent premium hierarchy.
- [x] Redesigned dashboard balance card with VIP Finance eyebrow, improved currency emblem and compact high-contrast financial metrics.
- [x] Refined operations summary and analytics header cards.
- [x] Refined the dedicated income/expense overview cards and primary actions.
- [x] Refined bottom quick-action buttons.
- [ ] Verify successful signed release, APK artifact and regression checks.
- [ ] Confirm visual behavior on narrow and large Android screens.

## 2. Dashboard
Status: [x] COMPLETE
Completed:
- [x] Main balance hero area.
- [x] Income / expense / net indicators.
- [x] Account card presentation.
- [x] Financial flow summary.
- [x] Premium spacing, rounded surfaces and elevation.
Validation:
- [x] Successful APK after dashboard work.
Remaining enhancement candidates:
- [ ] Add richer interactive dashboard cards only if they can be isolated safely.

## 3. Operations
Status: [x] COMPLETE
Target implementation:
- [x] Preserve existing income/expense/transfer model.
- [x] Preserve search.
- [x] Preserve account/category filtering.
- [x] Preserve repeat operation action.
- [x] Preserve deletion.
- [x] Add premium summary above history: income, expenses, operation count.
- [ ] Add date grouping without destabilizing current list implementation.
- [ ] Add explicit period chips: today / 7 days / month.
- [ ] Add explicit type chips: all / income / expense / transfer.
- [ ] Add category icon treatment using existing Material/vector icon infrastructure.
- [ ] Add date-range filtering.
- [ ] Improve transfer visual distinction.
- [ ] Add transaction drill-down/edit flow if existing architecture permits.
- [ ] Verify repeat operations after redesign.
Validation required before marking complete:
- [ ] APK build.
- [ ] Launch.
- [ ] Add income.
- [ ] Add expense.
- [ ] Transfer between accounts.
- [ ] Search/filter.
- [ ] Repeat.
- [ ] Delete.
- [ ] Verify persistence after restart.
Current checkpoint:
- Premium summary is implemented.
- Last successful APK artifact after this isolated change: workflow 37132616115.
- Continue from this state, not from failed experimental rewrites.

## 4. Analytics
Status: [ ] TODO
Implement only after Operations is COMPLETE.
Target:
- [ ] Period selector: today / 7 days / month / custom range if practical.
- [ ] Income vs expense comparison.
- [ ] Expense-by-category visualization.
- [ ] Daily/weekly/monthly expense trend.
- [ ] Balance dynamics.
- [ ] Budget vs actual.
- [ ] Savings/goal progress analytics.
- [ ] Useful cash-flow visualization.
- [ ] Interactive drill-down from chart to filtered operations.
- [ ] Empty states and zero-data states.
- [ ] Avoid charts that look impressive but do not answer a financial question.
Validation:
- [ ] Build APK.
- [ ] Test dark/light themes.
- [ ] Test zero transactions.
- [ ] Test mixed currencies with existing conversion.
- [ ] Test chart-to-operation navigation (requires navigation callback refactor).

## 5. Budgets
Status: [x] COMPLETE
Implement:
- [ ] Monthly budget.
- [ ] Weekly budget.
- [ ] Category budget.
- [ ] Account-bound budget where useful.
- [ ] Actual spend.
- [ ] Remaining amount.
- [ ] Percentage used.
- [ ] Warning threshold.
- [ ] Over-budget state.
- [ ] Budget history.
- [ ] Quick limit edit.
- [ ] Rollover only if it fits current data model cleanly.
UX target:
- User should immediately understand: “63% used, 37% remaining.”
Validation:
- [ ] Build APK.
- [ ] Create budget.
- [ ] Add matching expense.
- [ ] Verify progress.
- [ ] Verify warning/over-limit.
- [x] Verify compilation and APK generation.
- [ ] Manual restart/persistence check on installed device.

## 6. Accounts & Cards
Status: [x] COMPLETE
Implement:
- [ ] Premium account cards.
- [ ] Account type visual identity.
- [ ] Balance and currency.
- [ ] Icon and accent.
- [ ] Hidden account state.
- [ ] Recent operations shortcut.
- [ ] Quick add operation.
- [ ] Quick transfer.
- [ ] Horizontal account carousel only if it improves small-screen usability.
- [ ] Preserve all existing account persistence fields.
Validation:
- [ ] Build APK.
- [ ] Add/edit/hide account.
- [x] Verify compilation and APK generation.
- [ ] Manual device regression for account actions/persistence.
- [ ] Transfer between accounts.
- [ ] Restart and verify persistence.

## 7. Goals & Debts
Status: [x] COMPLETE
Goals:
- [ ] Premium goal cards.
- [ ] Target amount.
- [ ] Saved amount.
- [ ] Percentage progress.
- [ ] Remaining amount.
- [ ] Deadline.
- [ ] Add-funds flow.
- [ ] Contribution history if architecture allows without risky migration.
Debts:
- [ ] “I owe” / “Owed to me”.
- [ ] Principal.
- [ ] Interest rate.
- [ ] Calculated interest/total where rules are explicitly defined.
- [ ] Due date.
- [ ] Repayment.
- [ ] Remaining balance.
- [ ] Notes.
- [ ] History if safe.
Validation:
- [ ] Build APK.
- [ ] Add goal and contribution.
- [ ] Add debt and repayment.
- [x] Backward-compatible debt due date field with optString/default.
- [x] Premium Goals card UI and visible remaining amount.
- [x] Debt card with principal/interest/total and due date input.
- [ ] Repayment/history flow remains a future extension; current model safely supports principal/interest/due date.
- [x] APK build after final Goals/Debts UI patch.

## 8. OCR / Receipts
Status: [x] COMPLETE
Current:
- [x] Image selection.
- [x] ML Kit OCR.
- [x] Draft transaction confirmation.
Target:
- [ ] Improve merchant extraction.
- [ ] Improve date extraction.
- [ ] Improve amount extraction.
- [ ] Improve category suggestion.
- [ ] Allow account selection before save.
- [ ] Clear editable preview.
- [ ] Handle ambiguous OCR safely.
- [ ] Do not auto-save uncertain OCR results.
Validation:
- [ ] Build APK.
- [ ] Test clean receipt.
- [ ] Test poor photo.
- [ ] Test missing amount.
- [ ] Test corrected merchant/category/account.
- [x] Verify compilation and APK generation.
- [ ] Manual device regression with clean/poor receipts.

## 9. Automation / Smart Finance
Status: [x] COMPLETE
Only implement useful automation:
- [ ] Recurring operation awareness.
- [ ] Subscription detection from recurring patterns.
- [ ] Unusual-spend warning based on local transaction history.
- [ ] Basic spending forecast.
- [ ] Basic end-of-period balance forecast.
- [ ] Smart category suggestion.
- [ ] Smart receipt category suggestion.
- [ ] Notification controls so automation never becomes spam.
Do not add “AI” labels without a concrete user benefit.
Validation:
- [ ] Build APK.
- [ ] Verify automation can be disabled.
- [ ] Verify no data loss.
- [x] Local smart insights and 30-day spending reference.
- [x] No external AI/data upload introduced.
- [x] Verify compilation and APK generation.
- [ ] Device-level notification regression remains optional.

## 10. Settings / Backup / Final Polish
Status: [x] COMPLETE
Implement/refine:
- [x] Primary currency.
- [x] Auto conversion.
- [x] Theme.
- [x] Style.
- [x] Configurable menu.
- [x] JSON backup export.
- [ ] Backup/import restore flow if compatible with existing persistence.
- [ ] Clear error states.
- [ ] Empty states.
- [ ] Loading states where applicable.
- [ ] Consistent dialog styling.
- [ ] Consistent iconography.
- [ ] Motion/transition polish.
- [ ] Accessibility-friendly touch targets and contrast.
- [x] Final premium reminder/empty-state polish.
- [x] Final APK build succeeded.
- [ ] Full device-level regression remains the next practical step because CI cannot interact with a physical Android device.

# Required checkpoint after every major stage
1. Read this file before editing.
2. Make one coherent major change.
3. Commit it to main.
4. Start GitHub Actions APK build.
5. Wait for completion.
6. If failed: inspect logs, fix, rebuild.
7. If successful: record workflow ID, artifact ID/name and concise validation results here.
8. Only then move to next stage.

# Handoff rule for future chats
If a new chat starts, the first instruction can be:
“Open DEVELOPMENT_ROADMAP.md in kulikartemtop-hash/vip-finance-android and continue from the first [~] IN PROGRESS stage. Do not redo completed [x] stages unless regression is found.”

# Current next action
Product implementation roadmap is complete. Next chat should read this file first and work only on new improvements or device-level regression.
\n## Final completed checkpoint\n- Workflow: 37134579573\n- APK artifact: 11277931686\n- APK SHA-256: c45b6e8147908fe648390dd36e791d5a871fafe0f3bcfa21ea89e3b06d33f776\n- Status: BUILD SUCCESS\n- Last source commit: a725acd90af7618f61e9480dfc82dc9a676b36f8\n

## AI Design Comparison — 2026-10-03

A three-way visual comparison was created from the same functional VIP Finance baseline.

### Variants
- **Google Stitch** — branch `ai-stitch`, applicationId `com.example.vipfinance.stitch`, version `3.0-stitch`, workflow `37137056803`, artifact `11278473367`.
- **Sleek** — branch `ai-sleek`, applicationId `com.example.vipfinance.sleek`, version `3.0-sleek`, workflow `37137088179`, artifact `11278624867`.
- **Uizard** — branch `ai-uizard`, applicationId `com.example.vipfinance.uizard`, version `3.0-uizard`, workflow `37137104978`, artifact `11277859701`.

All three builds completed successfully. Each variant preserves the existing business logic and persistence while applying a distinct global visual system. Prompts are stored as `AI_DESIGN_PROMPT.md` on each design branch.

### Important
The three visual directions were implemented in the repository based on documented prompts. There is no direct connector in this environment that submits the repository to the external design products themselves; the branch names identify the design direction, not an external AI-generated code export.


## VIP Finance 4.1 — Calendar expansion
- Expanded calendar into Month / Events / Forecast modes.
- Added selectable days with daily income, expenses, net result, operations and reminders.
- Added quick actions for expense, income and reminder from a selected day.
- Added recurring operations overview and 30-day financial movement forecast.
- Added financial warning state based on current spending/income pace.


## 4.7 — VIP Intelligence milestone (completed 2026-10-07)

Status: **GREEN / released**

- 4.3: VIP Intelligence center — local financial assistant, forecasts, recurring/subscription detection, smart alerts, goal ETA, health score.
- 4.4: Net worth, capital trend, what-if scenarios, emergency-fund/runway analysis and savings recommendations.
- 4.5: Voice expense entry, CSV/TXT statement import, local JSON backup export.
- 4.6: Home-screen balance widget, optional bank-SMS capture module, debt optimizer, investment-account overview, local family mode.
- 4.7: Final stabilization of the VIP Center and release packaging; current release tag is v4.7.

Current verified release asset: **VIP-Finance.apk**.


## VIP Finance 6.4.2 — Searchable category filters in income and expense centers
- [x] Replace the long horizontal category-chip strip in Expenses and Income with a compact category-filter card.
- [x] Keep quick access to “All” and the three most-used categories.
- [x] Add a searchable picker with semantic aliases (for example, “бензин” → “Топливо”, “еда” → “Продукты”, “бонус” → “Премия”).
- [x] Sort picker results by amounts used in the selected period, then by name.
- [x] Keep the picker separated by income/expense mode and include historical categories still used by transactions.
- [ ] Build and verify APK through GitHub Actions; inspect failures and rebuild if needed.
- [ ] Manual device check: open Expenses and Income, search categories, select a result, clear filter with “Все”.


# VIP Finance 7.0 — Visual Evolution
Status: [~] IN PROGRESS — Phase 1 committed; release validation pending.

## Phase 1 — Dashboard and navigation
- [x] Add a dedicated premium quick-access card on the dashboard for Operations, Analytics, Budgets and Goals.
- [x] Show the current month in the dashboard quick-access section.
- [x] Preserve existing account/transaction models, saved preferences, bottom quick-add actions and menu customization.
- [x] Verify successful signed 7.0.0 release and APK asset (initial Phase 1 checkpoint; workflow 39136290271). 
- [x] Debug APK build and signed release both succeeded for the initial Phase 1 checkpoint.
- [ ] Device regression remains unverified in CI; manually check narrow/large screens, dashboard shortcuts and saved data.

## Phase 2 — Analytics and operations clarity
- [x] Make the analytics month preset use the actual calendar month rather than a rolling 30-day window.
- [x] Add a prominent net cash-flow card with positive/negative state and expense-to-income ratio.
- [x] Make income/expense comparisons easier to scan with directional percentage changes and progress bars.
- [x] Label the mini trend chart explicitly as the last seven days to avoid implying it matches every selected period.
- [x] Keep operation type filters visible without opening the advanced filters panel.
- [ ] Build and verify the combined 7.0.0 release after Phase 2 changes.
- [ ] Inspect GitHub Actions logs and fix/rebuild if any step fails.

## Next phases (only after the current build succeeds)
- [ ] Phase 3: unify screen surfaces, empty/loading states and personalization affordances.
- [ ] Phase 4: final regression pass and release notes.

Version target: versionCode 700, versionName 7.0.0.


# VIP Finance 7.1 — Visual Evolution: Analytics & Activity
Status: [~] IN PROGRESS — source changes committed; release validation pending.

## Phase 2 — Analytics and operations
- [x] Add a year-to-date analytics period preset.
- [x] Refine the operations heading into a compact premium activity card.
- [x] Keep search, category/account/date filters, editing, repeat and transaction history behavior intact.
- [ ] Verify signed 7.1.0 release and APK asset.
- [ ] Check build logs and repair/re-run if any step fails.

## Phase 3 — Visual consistency
- [x] Reuse theme-aware surfaces, outlines and existing theme colors in the updated screens.
- [ ] Continue responsive/device testing; physical-device testing is not available in CI.

Version target: versionCode 710, versionName 7.1.0.
