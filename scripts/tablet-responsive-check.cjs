const {chromium}=require('playwright');
const assert=require('node:assert/strict');
const fs=require('node:fs');
const path=require('node:path');
const root=path.resolve('src/main/resources/static');
const out=path.resolve('target/tablet-checks');
fs.mkdirSync(out,{recursive:true});
const customer={id:1,firstName:'Alexandra',lastName:'Dela Cruz',username:'fixture',email:'alexandra@example.test',phone:'09171234567',address:'Quezon City',createdAt:'2026-01-01T00:00:00Z',role:'CUSTOMER'};
const account={id:1,number:'1234567890',balance:12500,status:'ACTIVE',customer};
const transactions=['DEPOSIT','WITHDRAWAL','TRANSFER_OUT'].map((type,i)=>({id:i+1,type,amount:i===0?12500:-1500,balanceAfter:12500,accountNumber:account.number,reference:'fixture-reference-'+i,createdAt:'2026-10-01T00:00:00Z',description:'Tablet fixture',...(i===2?{counterpartyName:'Maria Teresa Santos',counterpartyNumber:'9876543210'}:{})}));
(async()=>{
 const browser=await chromium.launch({headless:true,executablePath:process.env.CHROMIUM_EXECUTABLE||undefined});
 const page=await browser.newPage({reducedMotion:'reduce'});
 const errors=[],failures=[];let role='CUSTOMER',count=0;
 page.on('pageerror',e=>errors.push(e.message));
 await page.route('**/*',async route=>{
  const url=new URL(route.request().url());
  if(url.pathname.startsWith('/api/')){
   const data={'/api/csrf':{headerName:'X-CSRF-TOKEN',token:'fixture'},'/api/session':{authenticated:true,user:{...customer,role}},'/api/account':account,'/api/transactions':transactions,'/api/pin':{configured:true},'/api/admin/accounts':[account],'/api/admin/transactions':transactions};
   if(!Object.hasOwn(data,url.pathname)){errors.push('Unexpected API request: '+url.pathname);return route.fulfill({status:404,json:{}});}
   return route.fulfill({json:data[url.pathname]});
  }
  const file=path.resolve(root,'.'+(url.pathname==='/'?'/index.html':url.pathname));
  if(!file.startsWith(root+path.sep))return route.abort();
  return route.fulfill({path:file});
 });
 try{
  for(role of ['CUSTOMER','ADMIN'])for(const width of [320,390,650,651,768,820,850,851,1000,1001,1024,1100,1101,1180,1440]){
   await page.goto('about:blank');
   await page.setViewportSize({width,height:width>=1000?768:1024});
   for(const route of role==='CUSTOMER'?['dashboard','account','deposit','withdraw','transfer','transactions','profile']:['dashboard','customers','accounts','customer/1','transactions','profile']){
    await page.goto('http://velis.test/#'+route);
    await page.locator('.workspace[data-route="'+route.split('/')[0]+'"]').waitFor();
    await page.evaluate(()=>document.fonts.ready);
    const issues=await page.evaluate(()=>{
     const result=[];
     if(document.documentElement.scrollWidth>innerWidth)result.push('Page overflow: '+document.documentElement.scrollWidth);
     for(const el of document.querySelectorAll('.filter,.action-card strong,.summary-grid .stat-value')){
      const r=el.getBoundingClientRect();if(r.width&&el.scrollWidth>el.clientWidth+1)result.push('Clipped label: '+el.textContent.trim());
     }
     const buttons=[...document.querySelectorAll('.filter')];
     for(const el of buttons){const a=el.getBoundingClientRect();for(const other of buttons){if(el===other)continue;const b=other.getBoundingClientRect();if(a.left<b.right&&a.right>b.left&&a.top<b.bottom&&a.bottom>b.top)result.push('Overlapping filters');}}
     return result;
    });
    if(issues.length)failures.push({role,width,route,issues});
    if([768,1024].includes(width)&&['dashboard','transactions','account'].includes(route))await page.screenshot({path:path.join(out,role+'-'+width+'-'+route+'.png'),fullPage:true});
    if(route==='transactions'){
     await page.getByRole('button',{name:'Withdrawals',exact:true}).click();
     assert.equal(await page.locator('[data-filter="WITHDRAWAL"]').getAttribute('aria-pressed'),'true');
     assert.equal(await page.locator(role==='CUSTOMER'?'.transaction-row':'#admin-tx-rows tbody tr').count(),1);
     await page.getByRole('button',{name:'All activity',exact:true}).click();
     await page.getByRole('textbox',{name:'Search transactions'}).fill('no-match');
     await page.getByRole('heading',{name:role==='CUSTOMER'?'Nothing to show here':'No matching transactions'}).waitFor();
    }
    count++;
   }
  }
  fs.writeFileSync(path.join(out,'results.json'),JSON.stringify({count,failures,errors},null,2));
  assert.deepEqual(errors,[]);assert.deepEqual(failures,[]);
  console.log('Passed '+count+' responsive route checks, including filter interaction and search.');
 }finally{await browser.close();}
})().catch(e=>{console.error(e);process.exitCode=1;});
