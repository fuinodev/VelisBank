const {chromium}=require('playwright');
const assert=require('node:assert/strict');
const path=require('node:path');
(async()=>{
 const browser=await chromium.launch({headless:true,executablePath:process.env.CHROMIUM_EXECUTABLE||undefined});
 const page=await browser.newPage({viewport:{width:390,height:844},reducedMotion:'reduce'});
 let verified=true,createPin=false,cleared=0;
 const errors=[];page.on('pageerror',e=>errors.push(e.message));
 await page.route('**/*',async route=>{
  const url=new URL(route.request().url());
  if(url.pathname.startsWith('/api/')){
   if(url.pathname==='/api/mobile/otp'){assert.deepEqual(route.request().postDataJSON(),{phone:'+639123452253',purpose:'RESET'});return route.fulfill({json:{remainingMillis:30000,localTest:true}});}
   if(url.pathname==='/api/mobile/verification'){
    if(route.request().method()==='DELETE'){verified=false;cleared++;return route.fulfill({json:{ok:true}});}
    return route.fulfill({json:{verified,createPin,phone:'+639123452253'}});
   }
   const data={'/api/csrf':{headerName:'X-CSRF-TOKEN',token:'fixture'},'/api/session':{authenticated:false}};
   assert.ok(Object.hasOwn(data,url.pathname),'Unexpected API '+url.pathname);
   return route.fulfill({json:data[url.pathname]});
  }
  return route.fulfill({path:path.resolve('src/main/resources/static','.'+(url.pathname==='/'?'/index.html':url.pathname))});
 });
 try{
  await page.goto('http://velis.test/#login');
  await page.getByRole('heading',{name:'Enter your PIN',exact:true,level:2}).waitFor();
  {const back=await page.locator('#pin-back').boundingBox(),forgot=await page.locator('.forgot-verified-pin').boundingBox();assert.ok(Math.abs(back.y-forgot.y)<2,'PIN actions must share one row');}
  assert.equal(await page.locator('#mobile-login-form').count(),0);
  await page.reload();
  await page.getByRole('heading',{name:'Enter your PIN',exact:true,level:2}).waitFor();
  await page.getByRole('button',{name:'Close dialog',exact:true}).click();
  await page.getByRole('button',{name:'Continue with PIN',exact:true}).click();
  await page.getByRole('heading',{name:'Enter your PIN',exact:true,level:2}).waitFor();
  await page.getByRole('button',{name:'Back',exact:true}).click();
  await page.getByRole('button',{name:'Use another mobile number',exact:true}).click();
  await page.locator('#mobile-login-form').waitFor();assert.equal(cleared,1);
  await page.reload();await page.locator('#mobile-login-form').waitFor();
  verified=true;createPin=false;
  await page.reload();await page.getByRole('button',{name:'Forgot PIN?',exact:true}).click();
  await page.getByRole('heading',{name:'Forgot PIN',exact:true}).waitFor();
  await page.getByText('09*****2253',{exact:true}).waitFor();
  assert.equal(await page.locator('.reset-key').count(),1);
  await page.setViewportSize({width:390,height:844});
  await page.screenshot({path:'target/forgot-pin-mobile.png',fullPage:true});
  assert.ok(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth));
  verified=true;createPin=true;
  await page.reload();await page.getByRole('heading',{name:'Create your PIN',exact:true,level:2}).waitFor();
  await page.getByRole('button',{name:'Close dialog',exact:true}).click();
  verified=false;
  await page.getByRole('button',{name:'Continue with PIN',exact:true}).click();
  await page.locator('#mobile-login-form').waitFor();
  assert.deepEqual(errors,[]);
  console.log('Passed: PIN resumes on login and reload; close/back recover; changing number clears grant; PIN setup and expiry route correctly.');
 }finally{await browser.close();}
})().catch(e=>{console.error(e);process.exitCode=1;});
