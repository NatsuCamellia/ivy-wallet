# Ivy Wallet

A personal finance app for tracking money across accounts. This fork redesigns the UI; the domain is unchanged from upstream.

## Language

### Money and accounts

**Account**:
A place money is kept, such as a bank account, cash or a card, with its own currency and balance.
_zh-TW_: 帳戶

**Excluded account**:
An account whose balance is not counted in the Total balance.
_zh-TW_: 未計入總額的帳戶
_Avoid_: hidden account, 排除, 不含

**Total balance**:
The sum of all non-excluded account balances, converted to the base currency.
_zh-TW_: 總餘額

**Base currency**:
The currency the user picked for totals and conversions.
_zh-TW_: 主要幣別

**Savings goal**:
An amount the user wants to keep untouched across all accounts.
_zh-TW_: 儲蓄目標
_Avoid_: buffer

### Transactions

**Transaction**:
A single money movement: an Income, an Expense or a Transfer.
_zh-TW_: 交易

**Income** / **Expense**:
A transaction that adds money to, or takes money from, one account.
_zh-TW_: 收入 / 支出

**Transfer**:
A transaction that moves money between two of the user's own accounts.
_zh-TW_: 轉帳
_Avoid_: 傳輸, 轉移

**Category**:
A user-defined label grouping transactions by what they were for.
_zh-TW_: 類別

**Planned payment**:
A rule that produces transactions due on a future date, once or repeatedly; it can be an income or an expense.
_zh-TW_: 預定交易
_Avoid_: recurring transaction, 經常交易, 定期付款

**Due**:
Planned transactions not yet paid, made up of Overdue and Upcoming ones.
_zh-TW_: 待處理
_Avoid_: Pending

**Overdue** / **Upcoming**:
A due transaction whose date has passed, or is still ahead.
_zh-TW_: 逾期 / 即將到期
_Avoid_: 即將推出

**Period**:
The date range the Home screen summarizes, a month by default.
_zh-TW_: 期間

**Cashflow**:
Income minus expenses within a period.
_zh-TW_: 淨額
_Avoid_: 現金流

### Screen elements

**Balance banner**:
The large rounded card at the top of Home and Accounts that leads with the Total balance.
_zh-TW_: 總餘額 Banner

**Type filter**:
The All / Expense / Income chips that narrow what a list shows.
_zh-TW_: 收支篩選

**Tip card**:
A dismissible card on Home suggesting a next step to the user.
_zh-TW_: 提示卡
_Avoid_: customer journey card
