const fs=require('fs'),vm=require('node:vm'),assert=require('node:assert/strict');
const source=fs.readFileSync('src/main/resources/static/app.js','utf8');
const requests=[];let authenticated=false;
const context=vm.createContext({Intl,Date,URLSearchParams,setTimeout,clearTimeout,location:{hash:'#dashboard'},document:{title:'',getElementById:()=>({removeAttribute(){},setAttribute(){}}),body:{classList:{add(){},remove(){}}}},fetch:async(url,options={})=>{requests.push({url,options});return {ok:true,status:200,json:async()=>url==='/api/csrf'?{headerName:'X-CSRF-TOKEN',token:'fresh-token'}:url==='/api/session'?{authenticated}: {ok:true}};}});
vm.runInContext(source.slice(0,source.indexOf('function toast')),context);
vm.runInContext(`const titles={};function closeModal(){};function shell(){throw new Error('Unexpected protected screen');}`,context);
vm.runInContext(source.slice(source.indexOf('async function renderRoute()'),source.indexOf("document.addEventListener('click'")),context);
(async()=>{
 vm.runInContext("state.csrf={headerName:'X-CSRF-TOKEN',token:'expired-token'}",context);
 await vm.runInContext("api('/login',{method:'POST',body:'username=admin'})",context);
 assert.deepEqual(requests.map(r=>r.url),['/api/csrf','/api/login']);assert.equal(requests[1].options.headers['X-CSRF-TOKEN'],'fresh-token');
 requests.length=0;vm.runInContext("state.user={role:'ADMIN'};state.accounts=[{id:1}];state.adminTransactions=[{id:2}]",context);
 await vm.runInContext('renderRoute()',context);
 assert.deepEqual(requests.map(r=>r.url),['/api/session']);assert.equal(context.location.hash,'login');assert.equal(vm.runInContext('state.user',context),null);assert.equal(vm.runInContext('state.accounts.length+state.adminTransactions.length',context),0);
 console.log('Passed: stale CSRF replaced before login; expired admin session redirects before protected requests and clears private state.');
})().catch(e=>{console.error(e);process.exitCode=1});

