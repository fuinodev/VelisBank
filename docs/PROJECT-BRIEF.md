I kept the **same VelisBank scope and functionality** from your original context—no new banking features. I mainly improved the wording, organization, consistency, UI/UX descriptions, and how each screen should behave. Your core direction remains a simple modern banking application built around cards, clear actions, transaction feedback, customer/admin views, and responsive design. 

# VelisBank

## Simple Banking Application — UI/UX and System Context

VelisBank is a **simple simulated digital banking application** designed as a Java Spring Boot final project.

The goal is to create a banking system that is easy to understand, easy to demonstrate, and visually organized while still showing important programming concepts such as:

- User authentication
- Customer accounts
- Deposits
- Withdrawals
- Fund transfers
- Transaction records
- Account management
- Administrative controls
- PostgreSQL database integration

The system should feel like a small real banking dashboard instead of a basic school CRUD application.

The application should remain **simple enough to develop and explain during assessment**.

---

# 1. Overall Design

VelisBank should use a clean, modern, and professional banking interface.

### Visual Style

Use:

- Clean light background
- Dark navy as the main brand color
- White cards
- Rounded corners
- Soft shadows
- Simple icons
- Clear typography
- Consistent spacing
- Responsive layouts
- Large balance displays
- Minimal information per screen

The interface should avoid unnecessary visual complexity.

The main design principle is:

> Important information should be grouped inside cards instead of displaying everything as plain text or large tables.

Cards provide visual separation between different types of information.

For example:

```text
┌──────────────┬──────────────────────────────────────┐
│ VelisBank    │ Good afternoon, Joshua              │
│              │                                      │
│ Dashboard    │ ┌──────────────────────────────────┐ │
│ My Account   │ │ AVAILABLE BALANCE                │ │
│ Transfer     │ │                                  │ │
│ Transactions │ │ ₱12,500.00                       │ │
│ Profile      │ │ Savings •••• 2834                │ │
│              │ └──────────────────────────────────┘ │
│ Logout       │                                      │
│              │ [Deposit] [Withdraw] [Transfer]      │
│              │                                      │
│              │ Recent Transactions                  │
│              │ ┌──────────────────────────────────┐ │
│              │ │ Transfer              -₱500.00   │ │
│              │ │ Sep 12, 2026          Completed  │ │
│              │ └──────────────────────────────────┘ │
└──────────────┴──────────────────────────────────────┘
```

---

# 2. Landing Page

The Landing Page is the first screen visitors see when opening VelisBank.

Its purpose is to introduce the application and direct the user toward registration or login.

### Navigation

```text
VelisBank                              Login   Register
```

### Hero Section

```text
┌──────────────────────────────────────────────┐
│                                              │
│          Simple. Secure. Banking.            │
│                                              │
│ Manage your simulated bank account           │
│ easily through VelisBank.                    │
│                                              │
│ [Open an Account]       [Login]              │
│                                              │
└──────────────────────────────────────────────┘
```

### Feature Cards

```text
┌──────────────────┐
│ Secure Account   │
│                  │
│ Protected        │
│ authentication   │
└──────────────────┘

┌──────────────────┐
│ Easy Transfer    │
│                  │
│ Send funds to    │
│ VelisBank users. │
└──────────────────┘

┌──────────────────┐
│ Track Activity   │
│                  │
│ View your        │
│ transactions.    │
└──────────────────┘
```

The Landing Page should **not contain actual banking operations**.

A visitor should immediately understand:

> VelisBank allows users to create an account, log in, manage simulated funds, and monitor their transactions.

---

# 3. Registration Page

Registration should be presented inside one organized card.

```text
             Create your VelisBank account

┌─────────────────────────────────────────────┐
│ Personal Information                        │
│                                             │
│ First Name          Last Name               │
│ [____________]      [____________]          │
│                                             │
│ Phone Number        Email                   │
│ [____________]      [____________]          │
│                                             │
│ Address                                     │
│ [_________________________________]         │
│                                             │
│ Account Information                         │
│                                             │
│ Username                                    │
│ [_________________________________]         │
│                                             │
│ Password            Confirm Password        │
│ [____________]      [____________]          │
│                                             │
│            [Create Account]                 │
│                                             │
│ Already have an account? Login              │
└─────────────────────────────────────────────┘
```

The form should clearly separate:

**Personal Information**
and
**Account Information**

This makes the registration process easier to understand.

---

# 4. Successful Registration

After registration succeeds, the user should not see raw database information.

Instead, display a simple confirmation card.

```text
┌───────────────────────────────────┐
│ ✓ Account Created                 │
│                                   │
│ Welcome to VelisBank.             │
│                                   │
│ Account Number                    │
│ 1029384756                        │
│                                   │
│ Account Type                      │
│ Savings                           │
│                                   │
│ Initial Balance                   │
│ ₱0.00                             │
│                                   │
│        [Continue to Login]        │
└───────────────────────────────────┘
```

This gives the user the important account information without overwhelming them.

---

# 5. Login Page

The Login Page should remain minimal.

```text
                 VelisBank

               Welcome back

┌─────────────────────────────────┐
│ Username                        │
│ [___________________________]   │
│                                 │
│ Password                        │
│ [___________________________] 👁│
│                                 │
│ [            Login           ]  │
│                                 │
│ Don't have an account? Register │
└─────────────────────────────────┘
```

### Invalid Login

Errors should appear directly near the form.

```text
⚠ Incorrect username or password.
```

Do not rely on JavaScript alert boxes for normal validation messages.

---

# 6. Customer Application Layout

After login, the customer enters the main VelisBank application.

Desktop layout:

```text
┌───────────────┬─────────────────────────────────┐
│ VelisBank     │                                 │
│               │ Main Content                    │
│ Dashboard     │                                 │
│ My Account    │                                 │
│ Transfer      │                                 │
│ Transactions  │                                 │
│ Profile       │                                 │
│               │                                 │
│ Logout        │                                 │
└───────────────┴─────────────────────────────────┘
```

### Sidebar Navigation

Use an icon together with a text label.

```text
Dashboard
My Account
Transfer
Transactions
Profile

Logout
```

Use an actual icon library in the implementation instead of emoji.

The sidebar should remain consistent throughout the customer area.

---

# 7. Customer Dashboard

The dashboard is the main screen of VelisBank.

It should immediately show the most important account information.

At the top:

```text
Good afternoon, Joshua

Here's your account overview.
```

The page should contain:

1. Balance Card
2. Quick Actions
3. Account Information
4. Recent Transactions

---

# 8. Balance Card

The Balance Card should receive the most visual attention.

```text
┌──────────────────────────────────────────────┐
│ Savings Account                     ACTIVE  │
│                                              │
│ Available Balance                            │
│                                              │
│ ₱12,500.00                                   │
│                                              │
│ Account Number                               │
│ •••• •••• 4752                               │
│                                              │
│ [Show Account Number]                        │
└──────────────────────────────────────────────┘
```

The balance is displayed prominently because it is the information customers are most likely to check first.

---

# 9. Quick Actions

Directly below the Balance Card, display the three main banking actions.

```text
┌──────────────┐ ┌──────────────┐ ┌──────────────┐
│      +       │ │      −       │ │      ⇄       │
│   Deposit    │ │   Withdraw   │ │   Transfer   │
│              │ │              │ │              │
│ Add funds    │ │ Take funds   │ │ Send funds   │
└──────────────┘ └──────────────┘ └──────────────┘
```

Each card should be clickable.

These actions should remain easy to identify rather than being mixed with unrelated buttons.

---

# 10. Account Information

The Account Information Card contains basic banking information.

```text
┌──────────────────────────────────────────────┐
│ Account Information                          │
│                                              │
│ Account Holder      Joshua Andrew Aboga      │
│ Account Type        Savings                  │
│ Account Number      1029384756               │
│ Account Status      ● Active                 │
│                                              │
└──────────────────────────────────────────────┘
```

This section is mainly read-only.

---

# 11. Recent Transactions

The dashboard does not need to display every transaction.

Only show approximately the latest **five transactions**.

```text
┌───────────────────────────────────────────────┐
│ Recent Transactions            View All →     │
│                                               │
│ ↓ Deposit                                     │
│   Sep 12, 2026                    +₱2,000.00   │
│                                               │
│ ↑ Transfer to •••• 5732                       │
│   Sep 11, 2026                      -₱500.00   │
│                                               │
│ ↓ Transfer from •••• 8921                     │
│   Sep 10, 2026                    +₱1,000.00   │
└───────────────────────────────────────────────┘
```

The customer can select **View All** to open the complete transaction history.

---

# 12. Deposit Page

The Deposit Page contains two primary cards.

## Current Account

```text
┌──────────────────────────────┐
│ Current Balance              │
│                              │
│ ₱12,500.00                   │
│                              │
│ Savings •••• 4752            │
└──────────────────────────────┘
```

## Deposit Funds

```text
┌──────────────────────────────┐
│ Deposit Funds                │
│                              │
│ Amount                       │
│ ₱ [____________________]     │
│                              │
│ [       Deposit Funds      ] │
└──────────────────────────────┘
```

Before processing the transaction:

```text
Deposit ₱500.00?

[Cancel]       [Confirm Deposit]
```

After completion:

```text
┌──────────────────────────────────┐
│ ✓ Deposit Successful             │
│                                  │
│ ₱500.00 was added to your        │
│ account.                         │
│                                  │
│ New Balance                      │
│ ₱13,000.00                       │
│                                  │
│ Reference                        │
│ VEL-000154                       │
└──────────────────────────────────┘
```

---

# 13. Withdraw Page

The withdrawal screen should follow the same visual structure as Deposit.

## Available Balance

```text
┌──────────────────────────────┐
│ Available Balance            │
│                              │
│ ₱13,000.00                   │
│                              │
│ Savings •••• 4752            │
└──────────────────────────────┘
```

## Withdraw Funds

```text
┌──────────────────────────────┐
│ Withdraw Funds               │
│                              │
│ Amount                       │
│ ₱ [____________________]     │
│                              │
│ [         Withdraw         ] │
└──────────────────────────────┘
```

### Insufficient Balance

```text
┌─────────────────────────────────────┐
│ ⚠ Insufficient Balance              │
│                                     │
│ You don't have enough funds for     │
│ this withdrawal.                    │
└─────────────────────────────────────┘
```

The system should clearly explain why the transaction cannot continue.

---

# 14. Transfer Page

Transfers require more information than deposits and withdrawals, so the screen should clearly separate the sender and recipient information.

## From

```text
┌─────────────────────────────────┐
│ From                            │
│                                 │
│ VelisBank Savings               │
│ •••• 4752                       │
│                                 │
│ Available Balance               │
│ ₱13,000.00                      │
└─────────────────────────────────┘
```

## Send Money

```text
┌─────────────────────────────────┐
│ Send Money                      │
│                                 │
│ Recipient Account Number        │
│ [___________________________]   │
│                                 │
│ Amount                          │
│ ₱ [_________________________]   │
│                                 │
│ Description (Optional)          │
│ [___________________________]   │
│                                 │
│ [          Continue         ]   │
└─────────────────────────────────┘
```

The system should validate the recipient account and amount before moving to confirmation.

---

# 15. Transfer Confirmation

A transfer should never be processed immediately after selecting Continue.

The customer first reviews the transaction.

```text
┌────────────────────────────────────┐
│ Confirm Transfer                   │
│                                    │
│ Recipient                          │
│ Juan Dela Cruz                     │
│                                    │
│ Account                            │
│ •••• 8932                          │
│                                    │
│ Amount                             │
│ ₱1,000.00                          │
│                                    │
│ Description                        │
│ School payment                     │
│                                    │
│ [Back]          [Confirm Transfer] │
└────────────────────────────────────┘
```

The confirmation screen reduces accidental transfers and gives the customer one final opportunity to verify the information.

---

# 16. Successful Transfer

After a transfer is completed, show a digital receipt.

```text
┌────────────────────────────────┐
│               ✓                │
│                                │
│ Transfer Successful            │
│                                │
│          ₱1,000.00             │
│                                │
│ To                             │
│ Juan Dela Cruz                 │
│ •••• 8932                      │
│                                │
│ Reference Number               │
│ VEL-20260912-A93F              │
│                                │
│ September 12, 2026             │
│                                │
│ [Back to Dashboard]            │
└────────────────────────────────┘
```

The reference number identifies the completed transaction.

---

# 17. Transactions Page

The Transactions Page contains the complete history of account activity.

Header:

```text
Transactions

View and track your account activity.
```

## Summary

```text
┌─────────────────┐ ┌─────────────────┐
│ Money In        │ │ Money Out       │
│                 │ │                 │
│ ₱8,500.00       │ │ ₱4,200.00       │
└─────────────────┘ └─────────────────┘
```

## Filters

```text
[All] [Deposit] [Withdrawal] [Transfer]
```

## Transaction Records

```text
┌──────────────────────────────────────────┐
│ ↑ Transfer Out                -₱500.00   │
│                                          │
│ To •••• 3921                             │
│ September 12, 2026 • 2:35 PM             │
│ VEL-009123                               │
└──────────────────────────────────────────┘

┌──────────────────────────────────────────┐
│ ↓ Deposit                    +₱2,000.00  │
│                                          │
│ September 11, 2026 • 10:41 AM            │
│ VEL-009122                               │
└──────────────────────────────────────────┘
```

Desktop can use a clean table if appropriate.

Mobile should display transactions as cards.

---

# 18. Transaction Details

Selecting a transaction displays its complete information.

```text
┌───────────────────────────────────────┐
│ Transaction Details                   │
│                                       │
│              -₱500.00                 │
│                                       │
│ Type              Transfer Out        │
│ Recipient         Juan Dela Cruz      │
│ Account           •••• 3921           │
│ Balance After     ₱12,500.00          │
│ Reference         VEL-009123          │
│ Date              Sep 12, 2026        │
│ Status            ✓ Completed         │
└───────────────────────────────────────┘
```

---

# 19. Profile Page

The Profile Page should initially display information instead of immediately showing a large editable form.

## Profile

```text
┌─────────────────────────────────────┐
│ JA                                  │
│                                     │
│ Joshua Andrew Aboga                 │
│ Customer                            │
│                                     │
│ Email                               │
│ example@email.com                   │
│                                     │
│ Phone                               │
│ 09XXXXXXXXX                         │
│                                     │
│ Address                             │
│ Quezon City                         │
│                                     │
│              [Edit Profile]         │
└─────────────────────────────────────┘
```

## Security

```text
┌─────────────────────────────────────┐
│ Security                            │
│                                     │
│ Password                            │
│ ••••••••••••                        │
│                                     │
│ [Change Password]                   │
└─────────────────────────────────────┘
```

---

# 20. Frozen Account

When an administrator freezes an account, VelisBank should clearly communicate the account status.

```text
┌─────────────────────────────────────────┐
│ ⚠ Account Temporarily Frozen            │
│                                         │
│ Transactions from this account are      │
│ currently unavailable.                  │
│                                         │
│ You can still view your account and     │
│ transaction history.                    │
└─────────────────────────────────────────┘
```

The following actions become unavailable:

```text
Deposit
Withdraw
Transfer
```

The customer should still be able to view:

- Balance
- Account information
- Transaction history
- Profile

The user should never be left wondering why transaction buttons stopped working.

---

# 21. Admin Layout

The administrator uses a similar interface but with different navigation.

```text
┌──────────────┬───────────────────────────────┐
│ VelisBank    │ Admin Dashboard               │
│ ADMIN        │                               │
│              │                               │
│ Dashboard    │                               │
│ Customers    │                               │
│ Accounts     │                               │
│ Transactions │                               │
│              │                               │
│ Logout       │                               │
└──────────────┴───────────────────────────────┘
```

Customer and administrator areas should remain visually related while clearly representing different roles.

---

# 22. Admin Dashboard

The Admin Dashboard provides a simple overview of the system.

Use statistic cards.

```text
┌──────────────────┐
│ Customers        │
│                  │
│       125        │
│                  │
│ Registered users │
└──────────────────┘

┌──────────────────┐
│ Active Accounts  │
│                  │
│       119        │
│                  │
│ Currently active │
└──────────────────┘

┌──────────────────┐
│ Frozen Accounts  │
│                  │
│         6        │
│                  │
│ Restricted       │
└──────────────────┘

┌──────────────────┐
│ Transactions     │
│                  │
│      1,842       │
│                  │
│ Total records    │
└──────────────────┘
```

The numbers can simply be generated from PostgreSQL queries.

Complex analytics are unnecessary for this project.

---

# 23. Admin Customers Page

The administrator should be able to search registered customers.

## Search

```text
┌─────────────────────────────────────────┐
│ Search Customers                        │
│                                         │
│ [Name, username or email____________]   │
└─────────────────────────────────────────┘
```

## Customer List

| Customer | Email | Account | Status | Action |
|---|---|---|---|---|
| Joshua Aboga | user@email.com | •••• 4752 | Active | View |
| Juan Cruz | juan@email.com | •••• 2831 | Active | View |

Selecting **View** opens the customer details.

---

# 24. Admin Customer Details

The page should divide information into smaller cards.

## Customer

```text
┌─────────────────────────────┐
│ Customer                    │
│                             │
│ Joshua Andrew Aboga         │
│ example@email.com           │
│ 09XXXXXXXXX                 │
└─────────────────────────────┘
```

## Account

```text
┌─────────────────────────────┐
│ Savings Account             │
│                             │
│ 1029384756                  │
│                             │
│ Balance                     │
│ ₱12,500.00                  │
│                             │
│ Status                      │
│ ● Active                    │
└─────────────────────────────┘
```

## Account Management

```text
┌─────────────────────────────┐
│ Account Management          │
│                             │
│ [Freeze Account]            │
└─────────────────────────────┘
```

---

# 25. Freeze Account Confirmation

Freezing an account affects the customer's ability to perform transactions.

The system must therefore request confirmation.

```text
┌────────────────────────────────────┐
│ Freeze Account?                    │
│                                    │
│ Joshua Andrew Aboga                │
│ Account •••• 4752                  │
│                                    │
│ This customer will temporarily     │
│ be unable to deposit, withdraw,    │
│ or transfer funds.                 │
│                                    │
│ [Cancel]          [Freeze Account] │
└────────────────────────────────────┘
```

---

# 26. Admin Transactions

Administrators can review transaction records.

## Search and Filter

```text
┌─────────────────────────────────────────┐
│ Search Transactions                     │
│                                         │
│ [Reference / Account_______________]    │
│                                         │
│ Type: [All ▼]                           │
└─────────────────────────────────────────┘
```

## Transaction Table

| Reference | Account | Type | Amount | Date |
|---|---|---|---:|---|
| VEL-1234 | •••• 4752 | Deposit | +₱500 | Sep 12 |
| VEL-1235 | •••• 8921 | Transfer | -₱1,000 | Sep 12 |

The page should prioritize readability instead of advanced reporting.

---

# 27. Card System

VelisBank should use a small reusable card system so every page follows the same visual language.

## Balance Card

Used for:

- Current balance
- Account type
- Account number
- Account status

## Information Card

Used for:

- Account information
- Profile information
- Customer information

## Action Card

Used for:

- Deposit
- Withdraw
- Transfer

## Statistic Card

Used for administrator statistics:

- Customers
- Accounts
- Transactions

## Transaction Card

Used for individual account activities.

## Warning Card

Used for:

- Frozen accounts
- Insufficient balance
- Important transaction messages

## Success Card

Used after:

- Registration
- Deposit
- Withdrawal
- Transfer

Consistent cards prevent every screen from looking like a completely different application.

---

# 28. Button System

Keep the number of button styles limited.

## Primary Button

Used for the main action.

```text
[Transfer]
```

Examples:

- Login
- Register
- Deposit
- Withdraw
- Transfer
- Save

## Secondary Button

Used for less important actions.

```text
[Cancel]
```

## Danger Button

Used for restrictive administrative actions.

```text
[Freeze Account]
```

## Text Action

Used mainly for navigation.

```text
View All →
```

---

# 29. Status Badges

Statuses should appear as small visual badges.

Instead of:

```text
ACTIVE
```

Display:

```text
● Active
```

Other examples:

```text
● Active
● Frozen
✓ Completed
```

Badges make statuses easier to recognize when scanning information.

---

# 30. Validation

Validation messages should appear close to the field that caused the error.

Example:

```text
Account Number

┌─────────────────────────┐
│ 123                     │
└─────────────────────────┘

Account number was not found.
```

Invalid inputs can receive a red border.

Avoid generic messages such as:

```text
ERROR!
```

Instead, tell the user exactly what needs to be corrected.

Examples:

```text
Please enter an amount.

Amount must be greater than ₱0.

Insufficient balance.

Recipient account was not found.

Passwords do not match.

Incorrect username or password.
```

---

# 31. Processing State

While processing financial actions, temporarily disable the main button.

Example:

```text
[ Processing... ]
```

This prevents the user from rapidly clicking the same action multiple times.

Examples include:

```text
[ Depositing... ]

[ Withdrawing... ]

[ Processing Transfer... ]
```

The backend should still validate every operation.

---

# 32. Empty States

When there is no data, do not show an unexplained empty section.

Example:

```text
┌────────────────────────────────────┐
│          No transactions yet       │
│                                    │
│ Your banking activity will appear  │
│ here after your first transaction. │
└────────────────────────────────────┘
```

The same principle can be used whenever a list has no records.

---

# 33. Responsive Design

VelisBank should remain usable on desktop and mobile devices.

## Desktop

```text
┌──── Sidebar ────┬──────── Content ─────────┐
│                 │                          │
│                 │                          │
│                 │                          │
└─────────────────┴──────────────────────────┘
```

## Mobile

```text
┌─────────────────────┐
│ VelisBank       ☰   │
├─────────────────────┤
│                     │
│ Balance Card        │
│                     │
├─────────────────────┤
│ Deposit             │
├─────────────────────┤
│ Withdraw            │
├─────────────────────┤
│ Transfer            │
└─────────────────────┘
```

On smaller screens, cards should stack vertically.

The desktop sidebar can become a mobile navigation menu.

---

# 34. Customer Journey

The customer experience should follow a simple and understandable flow.

```text
LANDING
   ↓
REGISTER
   ↓
ACCOUNT CREATED
   ↓
LOGIN
   ↓
DASHBOARD
   │
   ├── Balance
   │
   ├── Deposit
   │
   ├── Withdraw
   │
   ├── Transfer
   │
   ├── Recent Transactions
   │
   └── Account Information
   │
   ├───────────────┐
   ↓               ↓
TRANSACTIONS     PROFILE
   │
   ↓
LOGOUT
```

The customer should always know where they are and how to return to the dashboard.

---

# 35. Administrator Journey

```text
LOGIN
  ↓
ADMIN DASHBOARD
  │
  ├── Statistics
  │
  ├── Customers
  │      ↓
  │   Customer Details
  │      ↓
  │   Freeze / Unfreeze
  │
  ├── Accounts
  │
  └── Transactions
```

The administrator focuses mainly on monitoring customers, accounts, and transactions.

---

# 36. Main VelisBank Screens

VelisBank does not need dozens of separate pages.

The project can be organized around approximately **14 primary screens**.

| # | Screen | Main Content |
|---:|---|---|
| 1 | Landing | Hero + feature cards |
| 2 | Register | Registration card |
| 3 | Login | Login card |
| 4 | Customer Dashboard | Balance + quick actions |
| 5 | Account | Account information |
| 6 | Deposit | Balance + deposit form |
| 7 | Withdraw | Balance + withdrawal form |
| 8 | Transfer | Sender + recipient information |
| 9 | Transfer Confirmation | Transfer review |
| 10 | Transactions | Summary + transaction history |
| 11 | Profile | Profile + security |
| 12 | Admin Dashboard | Statistic cards |
| 13 | Customers / Accounts | Search + management table |
| 14 | Admin Customer Detail | Customer + account management |

Some confirmation and success states can appear inside these flows rather than requiring completely separate navigation pages.

---

# 37. System Structure

The overall VelisBank experience can be summarized as:

```text
                    VELISBANK
                        │
              ┌─────────┴─────────┐
              │                   │
           CUSTOMER             ADMIN
              │                   │
              ▼                   ▼
          Dashboard            Dashboard
              │                   │
        ┌─────┼─────┐       ┌─────┼─────────┐
        │     │     │       │     │         │
        ▼     ▼     ▼       ▼     ▼         ▼
     Deposit Withdraw     Customers Accounts Transactions
              │
           Transfer
              │
              ▼
        Transactions
              │
              ▼
           History
```

---

# 38. How VelisBank Should Feel

VelisBank should look like a **small digital banking dashboard**, not simply a collection of HTML forms connected to PostgreSQL.

The design should communicate four things clearly:

### Account State

The customer can quickly understand:

- Their current balance
- Account number
- Account type
- Account status

### Available Actions

The customer can immediately find:

- Deposit
- Withdraw
- Transfer

### Banking Activity

The customer can review:

- Recent transactions
- Complete transaction history
- Transaction details
- Reference numbers

### System Feedback

VelisBank should clearly communicate:

- Successful transactions
- Validation errors
- Insufficient balance
- Frozen accounts
- Processing states

---

# Final Project Direction

VelisBank should remain a **simple banking application with intentional UI/UX**.

Its strength does not come from having many features.

Its strength comes from implementing the existing features properly and consistently.

The completed application should demonstrate:

```text
Clean Interface
      +
Authentication
      +
Customer Accounts
      +
Deposit
      +
Withdrawal
      +
Transfer
      +
Transaction Records
      +
Admin Management
      +
PostgreSQL
      =
VelisBank
```

The cards are functional parts of the interface rather than decoration.

- **Balance Cards** communicate financial state.
- **Action Cards** expose primary banking operations.
- **Information Cards** organize account and customer details.
- **Transaction Cards** explain financial activity.
- **Statistic Cards** summarize administrator information.
- **Warning Cards** communicate restrictions or problems.
- **Success Cards** confirm completed operations.

The result should be a system that is **simple to use, simple to demonstrate, and simple enough to complete as a TESDA Java NC III project while still appearing organized and professionally designed.**

I intentionally **did not add features such as OTP, loans, bills payment, cards management, QR payments, notifications, or other banking modules**. The scope remains the same as your original VelisBank concept. 