const {chromium} = require('playwright');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const base = process.env.BASE_URL || 'http://127.0.0.1:8080';
const out = path.resolve('target/ui-checks');
fs.mkdirSync(out,{recursive:true});
const user = 'qa' + Date.now();
const password = 'TestingPass!2026';
const checks = [];
(async()=>{
 const browser=await chromium.launch({headless:true,executablePath:process.env.CHROMIUM_EXECUTABLE || undefined});
 const context=await browser.newContext({viewport:{width:1440,height:1000}});
 const page=await context.newPage();
 const errors=[];page.on('pageerror',e=>errors.push(e.message));
 page.setDefaultTimeout(15000);
 const shot=async name=>{await page.screenshot({path:path.join(out,name+'.png'),fullPage:true,animations:'disabled'});};
 async function login(username,pass) {
  await page.goto(base+'/#login');await page.getByLabel('Username',{exact:true}).fill(username);await page.getByLabel('Password',{exact:true}).fill(pass);await page.getByRole('button',{name:'Log in',exact:true}).click();await page.waitForURL('**/#dashboard');await page.locator('.page-heading').waitFor();
 }
 async function verifyPinIfShown() {if(await page.locator('#keypad-form').isVisible()){await page.getByLabel('Six-digit PIN',{exact:true}).fill('582941');await page.getByRole('button',{name:'Continue',exact:true}).click();}}
 async function logout() {await page.getByRole('button',{name:'Log out',exact:true}).click();await page.waitForURL('**/#login');}
 async function waitText(locator,text) {await locator.filter({hasText:text}).waitFor();}
 try {
  await page.goto(base);await page.getByRole('heading',{name:/Your money/}).waitFor();await shot('landing-desktop');checks.push('Landing page');
  await login('joshua','Customer!2026');await waitText(page.locator('.balance-value'),'12,500.00');await shot('dashboard-desktop');
  await page.getByRole('button',{name:'Show details'}).click();assert.equal(await page.locator('.flip-back').getAttribute('aria-hidden'),'false');await page.getByRole('button',{name:'Hide details'}).click();checks.push('Login, dashboard, account masking');
  await page.setViewportSize({width:390,height:844});await shot('dashboard-mobile');assert.ok(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth),'Mobile dashboard must not overflow');await page.getByRole('link',{name:'My account',exact:true}).click();await page.locator('.workspace[data-route="account"]').waitFor();checks.push('Mobile layout and navigation');
  await page.setViewportSize({width:1440,height:1000});await logout();
  await login('juan','Customer!2026');await page.getByRole('button',{name:'Show details'}).click();await page.getByRole('button',{name:'Show account number',exact:true}).click();const recipient=await page.locator('.info-row').filter({has:page.locator('dt',{hasText:'Account number'})}).locator('dd').innerText();await logout();
  await page.getByRole('link',{name:'Open an account',exact:true}).click();
  for(const [label,value] of [['First name','Browser'],['Last name','Test'],['Phone number','09171234567'],['Email address',user+'@example.com'],['Address','Quezon City'],['Username',user],['Password',password],['Confirm password',password]]) await page.getByLabel(label,{exact:true}).fill(value);
  await page.getByRole('button',{name:'Create account'}).click();await page.getByRole('heading',{name:'Account created.'}).waitFor();checks.push('Registration and success card');
  await page.getByRole('link',{name:'Continue to login'}).click();await login(user,password);await page.getByRole('heading',{name:'No transactions yet'}).waitFor();checks.push('New account and empty state');
  await page.getByRole('link',{name:'Profile',exact:true}).click();await page.getByRole('button',{name:'Set up PIN',exact:true}).click();await page.getByLabel('Current password',{exact:true}).fill(password);await page.getByRole('button',{name:'Verify password',exact:true}).click();await page.getByLabel('Six-digit PIN',{exact:true}).fill('582941');await page.getByRole('button',{name:'Continue',exact:true}).click();await page.getByLabel('Six-digit PIN',{exact:true}).fill('582941');await page.getByRole('button',{name:'Continue',exact:true}).click();await page.getByRole('dialog').waitFor({state:'hidden'});await page.getByRole('link',{name:'Dashboard',exact:true}).click();
  await page.getByRole('button',{name:'Deposit Add funds'}).click();await page.getByLabel('Amount',{exact:true}).fill('1500');await page.getByRole('button',{name:'Continue',exact:true}).click();await verifyPinIfShown();await page.getByRole('heading',{name:'Confirm deposit',exact:true}).waitFor();
  await waitText(page.locator('.balance-value'),'0.00');await page.getByRole('button',{name:'Confirm deposit',exact:true}).click();await page.getByRole('heading',{name:'Deposit successful'}).waitFor();await shot('deposit-receipt');await page.getByRole('button',{name:'Back to dashboard'}).click();await waitText(page.locator('.balance-value'),'1,500.00');checks.push('Deposit review, confirmation and receipt');
  await page.getByRole('button',{name:'Withdraw Take funds'}).click();await page.getByLabel('Amount',{exact:true}).fill('2000');await page.getByRole('button',{name:'Continue',exact:true}).click();await verifyPinIfShown();await waitText(page.locator('#error-amount'),'Insufficient');await page.getByLabel('Amount',{exact:true}).fill('200');await page.getByRole('button',{name:'Continue',exact:true}).click();await verifyPinIfShown();await page.getByRole('button',{name:'Confirm withdrawal',exact:true}).click();await page.getByRole('heading',{name:'Withdrawal successful'}).waitFor();await page.getByRole('button',{name:'Back to dashboard'}).click();await waitText(page.locator('.balance-value'),'1,300.00');checks.push('Overdraft error and withdrawal');
  await page.getByRole('button',{name:'Transfer Send funds'}).click();await page.getByLabel('Recipient account number').fill(recipient);await page.getByLabel('Amount',{exact:true}).fill('100');await page.getByLabel('Description').fill('Browser transfer check');await page.getByRole('button',{name:'Continue',exact:true}).click();await verifyPinIfShown();await page.getByRole('dialog').getByText('Juan Dela Cruz',{exact:true}).waitFor();await shot('transfer-review');await page.getByRole('button',{name:'Confirm transfer',exact:true}).click();await page.getByRole('heading',{name:'Transfer successful'}).waitFor();await page.getByRole('button',{name:'Back to dashboard'}).click();await waitText(page.locator('.balance-value'),'1,200.00');checks.push('Transfer recipient review and receipt');
  await page.getByRole('link',{name:'Transactions',exact:true}).click();await page.getByRole('button',{name:'Transfers',exact:true}).click();assert.equal(await page.locator('.transaction-row').count(),1);await page.locator('.transaction-row').click();await page.getByRole('heading',{name:'Transaction details'}).waitFor();await page.getByRole('dialog').getByText('Browser transfer check').waitFor();await page.getByRole('button',{name:'Close dialog'}).click();checks.push('Transaction filter and details');
  await page.getByRole('link',{name:'Profile',exact:true}).click();await page.getByRole('button',{name:'Edit profile'}).click();await page.getByLabel('Address',{exact:true}).fill('Cebu City');await page.getByRole('button',{name:'Save changes'}).click();await page.getByText('Cebu City',{exact:true}).waitFor();checks.push('Profile editing');
  await logout();await login('admin','AdminDemo!2026');await page.getByRole('heading',{name:'Bank overview'}).waitFor();await shot('admin-desktop');await page.getByRole('link',{name:'Customers',exact:true}).click();await page.getByRole('textbox',{name:'Search customers'}).fill(user);await page.getByRole('link',{name:'View',exact:true}).click();await page.getByRole('button',{name:'Freeze account',exact:true}).click();await page.getByRole('dialog').getByRole('button',{name:'Freeze account',exact:true}).click();await page.getByRole('button',{name:'Unfreeze account',exact:true}).waitFor();checks.push('Admin search, customer details and freeze confirmation');
  await logout();await login(user,password);await page.getByText('Account temporarily frozen',{exact:true}).waitFor();assert.equal(await page.getByRole('button',{name:'Deposit Add funds'}).isDisabled(),true);await shot('frozen-dashboard');checks.push('Frozen customer dashboard');
  await logout();await login('admin','AdminDemo!2026');await page.getByRole('link',{name:'Customers',exact:true}).click();await page.getByRole('textbox',{name:'Search customers'}).fill(user);await page.getByRole('link',{name:'View',exact:true}).click();await page.getByRole('button',{name:'Unfreeze account',exact:true}).click();await page.getByRole('dialog').getByRole('button',{name:'Unfreeze account',exact:true}).click();await page.getByRole('button',{name:'Freeze account',exact:true}).waitFor();checks.push('Admin unfreeze');
  await logout();await login(user,password);await page.getByRole('link',{name:'Profile',exact:true}).click();await page.getByRole('button',{name:'Change password',exact:true}).click();await page.getByLabel('Current password',{exact:true}).fill(password);await page.getByLabel('New password',{exact:true}).fill('ChangedPass!2026');await page.getByLabel('Confirm new password',{exact:true}).fill('ChangedPass!2026');await page.getByRole('button',{name:'Update password'}).click();await page.waitForURL('**/#login');await login(user,'ChangedPass!2026');checks.push('Password change and session invalidation');
  assert.deepEqual(errors,[],'No uncaught browser errors');
  fs.writeFileSync(path.join(out,'results.json'),JSON.stringify({passed:true,user,checks,errors},null,2));console.log(JSON.stringify({passed:true,checks:checks.length,user}));
 } catch(e) {await shot('failure');fs.writeFileSync(path.join(out,'results.json'),JSON.stringify({passed:false,user,checks,error:e.message,errors},null,2));throw e;}
 finally {await browser.close();}
})().catch(e=>{console.error(e);process.exitCode=1;});





