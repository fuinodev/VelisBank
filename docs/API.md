# API overview

All request and response bodies use JSON, except the form-encoded login. Monetary inputs accept decimal strings to preserve decimal precision. All modifying calls require the session's CSRF token in the header returned by `GET /api/csrf`. Obtain a fresh token after login/logout.

## Public/authentication

- `GET /api/csrf`: token and header name.
- `POST /api/register`: firstName, lastName, email, phone, address, username, password, confirmPassword. Creates one zero-balance savings account.
- `POST /api/login`: form fields username and password. Establishes the HTTP-only session cookie.
- `POST /api/logout`: invalidates the session.

## Signed-in user

- `GET /api/me`: profile and role, with no password hash.
- `PUT /api/profile`: firstName, lastName, email, phone, address.
- `PUT /api/password`: currentPassword, password, confirmPassword. Invalidates the current session on success.

## Customer only

- `GET /api/account`: the authenticated customer's savings account.
- `GET /api/transactions`: that customer's ledger entries, newest first.
- `GET /api/transactions/{id}`: an owned entry; another customer's entries are not exposed.
- `POST /api/money/review`: validates details and returns recipient identity for transfers. Does not move funds.
- `POST /api/money`: revalidates and atomically commits a money operation.

Example money body:

```json
{
  "type": "TRANSFER",
  "amount": "500.00",
  "recipient": "1029384756",
  "description": "School payment",
  "requestKey": "fe718a65-1655-4c2a-bba5-7fcd526c48f0"
}
```

Types are `DEPOSIT`, `WITHDRAWAL`, and `TRANSFER`. Amounts range from 0.01 to 999999999.99 with at most two decimal places. The request key must be 16–64 letters, digits, or hyphens; use a UUID and reuse it when retrying the same operation. Recipient is required for transfers; description is optional and limited to 140 characters.

## Administrator only

- `GET /api/admin/accounts`: all customer accounts and profile details.
- `GET /api/admin/accounts/{id}`: customer/account detail.
- `PATCH /api/admin/accounts/{id}/status`: `{"status":"FROZEN"}` or `{"status":"ACTIVE"}`.
- `GET /api/admin/transactions`: all ledger entries. Transfer-in and transfer-out share a reference.

## Error format

Validation failures return HTTP 400 with `message` and an `errors` object mapping field names to messages. Unauthenticated calls return 401, forbidden roles/invalid CSRF return 403, and conflicting unique customer details return 409. Authentication failures deliberately use a generic incorrect-credentials message.
