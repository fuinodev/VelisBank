# Mobile login and SMS setup

Customer login is now: mobile number → SMS OTP → six-digit PIN.
New customers register their personal details without a username/password, verify their number, then create and confirm a PIN.
Forgot PIN and Profile → Reset PIN require a new SMS OTP, then a different PIN and confirmation.
Transactions require the PIN only. Administrator username/password login is retained under Administrator on the login screen.

## Local testing (no actual texts)

Run in PowerShell before starting the app:

```powershell
$env:SMS_MODE = 'local'
.\scripts\start.ps1
```

The OTP screen clearly labels this mode. The application log prints the code with a `LOCAL TEST ONLY` label (in the terminal, or `data/application.log` when redirected there). You can also open `data/local-sms/<country-code-and-number>.txt` locally to read the most recent test message. The folder is ignored by Git. No code is returned by the API. Never enable this mode on a public server. Real SMS codes are not logged.

## Real texts with Twilio

Create your own Twilio account, fund/enable messaging and obtain the sender approval required for your destination country. Configure these privately in the server environment:

```powershell
$env:SMS_MODE = 'twilio'
$env:TWILIO_ACCOUNT_SID = '<your account SID>'
$env:TWILIO_AUTH_TOKEN = '<your auth token>'
$env:TWILIO_SENDER = '<your approved sender>'
.\scripts\start.ps1
```

Do not commit these values or paste them into chat. SMS is disabled by default; configuration errors fail closed. Twilio acceptance does not guarantee handset delivery. The SMS body includes VelisBank; a displayed VelisBank sender name requires provider/carrier approval (including registration for Philippine recipients).

Message: `VelisBank: Your verification code is <code>. Valid for 30 seconds. Never share this code. If you did not request it, ignore this message.`

## Validation

- Philippine mobile formats 09XXXXXXXXX, 9XXXXXXXXX and +639XXXXXXXXX normalize to one identity.
- One registered account per mobile number; ambiguous existing duplicate numbers require administrator assistance.
- Codes expire after 30 seconds; a new code cannot be sent until that expiry, including after successful verification.
- Codes are hashed in the database, bound to the requesting session and purpose, and consumed after success.
- Three incorrect codes block that code until expiry. An IP request limit protects send/registration endpoints.
- OTP verification alone does not create an authenticated customer session. Correct PIN or successful first/reset PIN creation is required.
- Three incorrect PINs enforce the existing persistent five-minute PIN lock.
- PIN selection and confirmation mistakes do not count as incorrect login PIN attempts.
- Verified mobile numbers cannot be edited through the ordinary profile form.
- Existing balances/history are preserved; internal username identifiers remain for database compatibility but customers no longer enter them.

Provider documentation:
https://www.twilio.com/docs/messaging/api/message-resource
https://www.twilio.com/en-us/guidelines/ph/sms

Administrator entry: open `http://localhost:8080/#admin-login` directly or bookmark it. Customer login, registration and PIN recovery do not display an Administrator link. This separate page preserves the existing administrator role enforcement and login cooldown; a hidden URL is not an authorization control. Administrator MFA is not implemented by this page separation.
