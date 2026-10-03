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
Status: [x] COMPLETE
Completed:
- [x] Premium dashboard hierarchy.
- [x] Layered/hero balance card.
- [x] Large balance and income/expense/net presentation.
- [x] Account cards with icons and visual surfaces.
- [x] Theme-aware colors and existing light/dark support preserved.
- [x] Existing theme styles preserved.
Validation:
- [x] GitHub Actions build succeeded.
- [x] APK artifact produced.
Notes:
- Do not replace the existing theme architecture just for visual experimentation.

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
Status: [~] IN PROGRESS
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
- [ ] Premium Goals card UI and visible remaining amount.
- [ ] Debt card with principal/interest/total and due date input.
- [ ] Repayment/history flow if it can be added without unsafe data migration.
- [ ] APK build after final Goals/Debts UI patch.

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
Status: [~] IN PROGRESS
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
- [ ] Verify notifications do not break normal use.

## 10. Settings / Backup / Final Polish
Status: [ ] TODO
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
- [ ] Final regression of every major module.

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
Implement Stage 9 useful automation only, build APK, validate recurring/subscription/anomaly/forecast logic, then update this file and finish Stage 7 Goals/Debts before Final Polish.
