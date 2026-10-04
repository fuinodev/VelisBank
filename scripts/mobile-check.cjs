const {chromium}=require('playwright');
const assert=require('node:assert/strict');
(async()=>{
 const browser=await chromium.launch({headless:true,executablePath:process.env.CHROMIUM_EXECUTABLE});
 const page=await browser.newPage({viewport:{width:390,height:844}});
 await page.goto('http://127.0.0.1:8080/#login');
 await page.getByLabel('Username',{exact:true}).fill('joshua');await page.getByLabel('Password',{exact:true}).fill('Customer!2026');await page.getByRole('button',{name:'Log in',exact:true}).click();await page.waitForURL('**/#dashboard');await page.locator('.workspace[data-route="dashboard"]').waitFor();
 assert.equal(await page.locator('.app-navigation').isVisible(),true);
 const widths=await page.locator('.balance-card').evaluateAll(els=>els.map(el=>el.getBoundingClientRect().width));assert.ok(widths.every(w=>w>=340));
 await page.screenshot({path:'target/ui-checks/dashboard-mobile.png',fullPage:true,animations:'disabled'});
 await page.getByRole('link',{name:'Transactions',exact:true}).click();await page.locator('.workspace[data-route="transactions"]').waitFor();assert.equal(await page.locator('.app-navigation').isVisible(),true);
 await page.screenshot({path:'target/ui-checks/transactions-mobile.png',fullPage:true,animations:'disabled'});
 await browser.close();console.log('Mobile widths, bottom navigation and activity layout passed.');
})().catch(e=>{console.error(e);process.exitCode=1});

