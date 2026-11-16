# Known gaps

Known gaps are listed here.

- Reorder with a list taller than the sheet (3.4a): `flow_reorder` runs on the two default accounts only, so scrolling and dragging inside a sheet whose rows overflow it is not covered by `m3-smoke.sh`. Onboarding offers no way to seed more than two accounts, and adding ten or more accounts through the UI would make the flow several minutes long. Needs a seeded database or a debug intent before it can be automated.

## Remaining drawable references

Spec grep 4 (`git grep -nE 'R\.drawable\.ic_' -- 'feature/*.kt' 'temp/*.kt'`) still prints 27 hits at the end of Task 6.1. None is a hand-drawn interface icon of the old design system.

- `R.drawable.ic_custom_account_m`, `ic_custom_category_m`, `ic_custom_loan_m` and `ic_custom_category_s` (25 hits). These are the default glyphs of user-picked account, category and loan icons: `IconBadge` and `customIconResource` take the user's saved icon name and fall back to this drawable, and the icon picker offers the whole `ic_custom_*` set. They are content chosen by the user, loaded with `getIdentifier`, so they stay (spec: keep `ic_custom_*`). Hits:
  - `feature/accounts/src/main/java/com/ivy/accounts/AccountsTab.kt:327`
  - `feature/budgets/src/main/java/com/ivy/budgets/BudgetModal.kt:274`
  - `feature/categories/src/main/java/com/ivy/categories/CategoriesScreen.kt:376`
  - `feature/loans/src/main/java/com/ivy/loans/loan/LoansScreen.kt:282`
  - `feature/loans/src/main/java/com/ivy/loans/loandetails/LoanDetailsScreen.kt:279`
  - `feature/loans/src/main/java/com/ivy/loans/loandetails/LoanDetailsScreen.kt:441`
  - `feature/onboarding/src/main/java/com/ivy/onboarding/steps/OnboardingAccounts.kt:185`
  - `feature/onboarding/src/main/java/com/ivy/onboarding/steps/OnboardingCategories.kt:172`
  - `feature/piechart/src/main/java/com/ivy/piechart/PieChartStatisticScreen.kt:256`
  - `feature/reports/src/main/java/com/ivy/reports/FilterOverlay.kt:464`
  - `feature/reports/src/main/java/com/ivy/reports/FilterOverlay.kt:511`
  - `feature/transactions/src/main/java/com/ivy/transactions/TransactionsScreen.kt:504`
  - `feature/transactions/src/main/java/com/ivy/transactions/TransactionsScreen.kt:506`
  - `temp/legacy-code/src/main/java/com/ivy/legacy/ui/component/AccountsRow.kt:53`
  - `temp/legacy-code/src/main/java/com/ivy/legacy/ui/component/TransactionRow.kt:148`
  - `temp/legacy-code/src/main/java/com/ivy/legacy/ui/component/TransactionRow.kt:161`
  - `temp/legacy-code/src/main/java/com/ivy/legacy/ui/component/TransactionRow.kt:242`
  - `temp/legacy-code/src/main/java/com/ivy/legacy/ui/component/edit/core/Category.kt:32`
  - `temp/legacy-code/src/main/java/com/ivy/legacy/ui/sheet/AccountModal.kt:139`
  - `temp/legacy-code/src/main/java/com/ivy/legacy/ui/sheet/CategoryModal.kt:141`
  - `temp/legacy-code/src/main/java/com/ivy/legacy/ui/sheet/ChooseCategoryModal.kt:106`
  - `temp/legacy-code/src/main/java/com/ivy/legacy/ui/sheet/ChooseIconModal.kt:126`
  - `temp/legacy-code/src/main/java/com/ivy/legacy/ui/sheet/LoanModal.kt:162`
  - `temp/legacy-code/src/test/java/com/ivy/legacy/ui/component/TransactionRowPaparazziTest.kt:137`
  - `temp/legacy-code/src/test/java/com/ivy/legacy/ui/component/TransactionRowPaparazziTest.kt:138`
- `R.drawable.ic_notification` at `temp/legacy-code/src/main/java/com/ivy/legacy/android/notification/NotificationService.kt:23`. The small icon of a system notification; the status bar cannot draw a Material vector from a Compose `ImageVector`.
