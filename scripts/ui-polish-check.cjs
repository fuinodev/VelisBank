const {chromium}=require('playwright');
const assert=require('node:assert/strict');
const fs=require('node:fs');
const out='target/ui-refined';fs.mkdirSync(out,{recursive:true});
(async()=>{
 const browser=await chromium.launch({headless:true,executablePath:process.env.CHROMIUM_EXECUTABLE});
 const page=await browser.newPage({viewport:{width:1440,height:1000}});page.setDefaultTimeout(18000);
 const errors=[];page.on('pageerror',e=>errors.push(e.message));
 const shot=async name=>{await page.evaluate(()=>document.fonts.ready);await page.screenshot({path:`${out}/${name}.png`,fullPage:true,animations:'disabled'});};
 try {
 await page.goto('http://127.0.0.1:8080/');await page.getByRole('heading',{name:/Your money/}).waitFor();await shot('landing');
 await page.setViewportSize({width:390,height:844});await shot('landing-mobile');assert.ok(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth));
 await page.setViewportSize({width:1440,height:1000});await page.goto('http://127.0.0.1:8080/#login');await page.getByLabel('Username',{exact:true}).fill('joshua');await page.getByLabel('Password',{exact:true}).fill('Customer!2026');await page.getByRole('button',{name:'Log in',exact:true}).click();await page.locator('.flip-shell').waitFor();await shot('dashboard');
 assert.equal(await page.locator('.brandmark').evaluate(el=>el.tagName),'IMG');
 await page.locator('.app-navigation').evaluate(el=>el.dataset.retained='yes');
 await page.getByRole('link',{name:'My account',exact:true}).click();await page.getByRole('heading',{name:'My account',exact:true}).waitFor();assert.equal(await page.locator('.app-navigation').getAttribute('data-retained'),'yes');
 await page.getByRole('link',{name:'Dashboard',exact:true}).click();await page.getByRole('heading',{name:'Recent transactions'}).waitFor();
 await page.getByRole('button',{name:'Deposit Add funds'}).click();await page.getByLabel('Amount',{exact:true}).fill('25');await page.getByRole('button',{name:'Continue',exact:true}).click();await page.getByRole('dialog').waitFor();assert.equal(await page.locator('#app').evaluate(el=>el.inert),true);await shot('confirmation');await page.keyboard.press('Escape');await page.getByRole('dialog').waitFor({state:'detached'});assert.equal(await page.locator('#app').evaluate(el=>el.inert),false);assert.equal(await page.getByLabel('Amount',{exact:true}).inputValue(),'25');
 await page.getByRole('link',{name:'Dashboard',exact:true}).click();await page.getByRole('heading',{name:'Recent transactions'}).waitFor();await page.setViewportSize({width:390,height:844});await shot('dashboard-mobile');assert.ok(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth));
 await page.getByRole('link',{name:'Transactions',exact:true}).click();await page.locator('.workspace[data-route="transactions"]').waitFor();await shot('transactions-mobile');
 await page.setViewportSize({width:1440,height:1000});await page.emulateMedia({reducedMotion:'reduce'});await page.getByRole('link',{name:'Dashboard',exact:true}).click();await page.getByRole('heading',{name:'Recent transactions'}).waitFor();assert.equal(await page.locator('.content').evaluate(el=>getComputedStyle(el).animationName),'none');
 await page.getByRole('button',{name:'Log out',exact:true}).click();await page.getByLabel('Username',{exact:true}).fill('admin');await page.getByLabel('Password',{exact:true}).fill('AdminDemo!2026');await page.getByRole('button',{name:'Log in',exact:true}).click();await page.getByRole('heading',{name:'Bank overview'}).waitFor();await shot('admin');await page.getByRole('link',{name:'View active accounts',exact:true}).click();await page.getByRole('heading',{name:'Accounts',exact:true}).waitFor();assert.equal(await page.getByRole('combobox',{name:'Account status'}).inputValue(),'ACTIVE');
 assert.deepEqual(errors,[]);console.log('Gradient UI: desktop/mobile layouts, SVG logo, card destinations, persistent navigation, modal focus/close and reduced motion passed.');
 }catch(e){await shot('failure');throw e;}finally{await browser.close();}
})().catch(e=>{console.error(e);process.exitCode=1});
