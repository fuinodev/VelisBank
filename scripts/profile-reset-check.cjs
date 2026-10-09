const {chromium}=require('playwright');const assert=require('node:assert/strict');const path=require('node:path');
(async()=>{
 const browser=await chromium.launch({headless:true});const page=await browser.newPage({viewport:{width:390,height:844},reducedMotion:'reduce'});
 let cancelled=0,completed=0;const errors=[];page.on('pageerror',e=>errors.push(e.message));
 const user={firstName:'Alex',lastName:'Test',role:'CUSTOMER',phone:'+639123452253',email:'test@example.test',address:'Manila',createdAt:'2026-01-01'};
 await page.route('**/*',async r=>{const u=new URL(r.request().url()),p=u.pathname;
 if(p.startsWith('/api/')){
  if(p==='/api/pin/reset'){if(r.request().method()==='DELETE'){cancelled++;return r.fulfill({json:{ok:true}});}return r.fulfill({json:{id:'flow',maskedPhone:'09*****2253',remainingMillis:30000,localTest:true}});}
  if(p==='/api/pin/reset/current'&&r.request().postDataJSON().code!=='582941')return r.fulfill({status:400,json:{message:'Invalid current PIN.'}});
  if(p.startsWith('/api/pin/reset/')){if(p.endsWith('/complete'))completed++;return r.fulfill({json:{ok:true}});}
  const data={'/api/csrf':{headerName:'X-CSRF-TOKEN',token:'test'},'/api/session':{authenticated:true,user},'/api/pin':{configured:true},'/api/account':{id:1,customer:user,number:'1234567890',balance:100,status:'ACTIVE'},'/api/transactions':[]};assert.ok(p in data,p);return r.fulfill({json:data[p]});
 }return r.fulfill({path:path.resolve('src/main/resources/static','.'+(p==='/'?'/index.html':p))});});
 const heading=title=>page.getByRole('heading',{name:title,exact:true,level:2}).waitFor();
 const pin=async value=>{await page.getByLabel('Six-digit PIN',{exact:true}).fill(value);await page.getByRole('button',{name:'Continue',exact:true}).click();};
 const start=async()=>{await page.getByRole('button',{name:'Reset PIN',exact:true}).click();await heading('Reset PIN OTP validation');assert.equal(await page.getByLabel('Mobile number',{exact:true}).count(),0);assert.equal(await page.getByRole('button',{name:/^(Back|Cancel)$/}).count(),0);};
 const otp=async()=>{await page.getByLabel('Verification code digit 1',{exact:true}).fill('123456');await page.getByRole('button',{name:'Verify OTP',exact:true}).click();await heading('Enter your current PIN');};
 try{
  await page.goto('http://velis.test/#profile');
  for(const stage of ['otp','current','new','confirm']){
   await start();if(stage!=='otp'){await otp();if(stage!=='current'){await pin('582941');await heading('Enter new PIN');if(stage==='confirm'){await pin('739251');await heading('Confirm new PIN');}}}
   await page.getByRole('button',{name:'Close dialog',exact:true}).click();await page.getByRole('dialog').waitFor({state:'hidden'});assert.equal(await page.locator('.workspace').getAttribute('data-route'),'profile');
  }
  await start();await page.getByRole('button',{name:'Close dialog',exact:true}).click();await page.getByRole('dialog').waitFor({state:'hidden'});
  await start();await page.getByRole('button',{name:'Close dialog',exact:true}).click();await page.getByRole('dialog').waitFor({state:'hidden'});
  await start();await page.screenshot({path:'target/profile-reset-otp.png',fullPage:true});await otp();await pin('000000');await page.getByText('Invalid current PIN.',{exact:true}).waitFor();await pin('582941');await heading('Enter new PIN');await pin('739251');await heading('Confirm new PIN');await pin('111111');await page.getByText('PINs do not match. Try again.',{exact:true}).waitFor();await pin('739251');await page.getByRole('dialog').waitFor({state:'hidden'});
  assert.equal(completed,1);assert.equal(cancelled,6);assert.deepEqual(errors,[]);console.log('PASS: profile reset stages, invalid PIN, mismatch, Back at every stage, Cancel, close, fresh restart and successful completion.');
 }finally{await browser.close();}
})().catch(e=>{console.error(e);process.exitCode=1;});
