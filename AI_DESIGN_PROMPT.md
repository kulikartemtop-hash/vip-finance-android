# VIP Finance — Google Stitch design direction

Create a completely redesigned premium Android finance app while preserving every existing capability and data model.

PRODUCT: VIP Finance. Existing functionality must remain intact: dashboard, operations, income/expense/transfer, accounts/cards, custom categories with selectable icon/color, budgets, analytics/charts, currency converter, debts with interest and due date, savings goals with top-up, reminders, receipt OCR, smart local finance signals, settings, themes, JSON backup/export, persistence.

VISUAL DIRECTION: premium Material 3, layered 2.5D depth, controlled elevation, soft surfaces, subtle gradients, large financial numbers, strong hierarchy, calm dark/light modes, refined indigo/cyan/violet palette. Avoid generic template UI. Every screen should feel intentionally composed.

SCREENS: redesign the entire visual language across Home, Operations, Accounts, Categories, Budgets, Analytics, Converter, Debts, Goals, Reminders, Receipts, Settings and every dialog. Use coherent spacing, typography, card hierarchy, polished empty states, premium action areas and visual previews.

INTERACTION: preserve existing navigation and behavior. Do not remove features. Dialogs should feel like designed product surfaces, not default Android alerts. Keep touch targets comfortable and accessibility readable.

ANDROID: Kotlin + Jetpack Compose + Material 3. Maintain existing persistence and backward compatibility. Deliver a production-ready visual system that can be implemented without changing business logic.