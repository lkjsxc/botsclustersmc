import assert from 'node:assert/strict';
const { chromium }=await import(new URL('../.build/browser/node_modules/playwright/index.mjs',import.meta.url));
const url=process.env.MONITOR_URL||'http://127.0.0.1:8766/';
const browser=await chromium.launch({headless:true});
const page=await browser.newPage({viewport:{width:1280,height:1000},locale:'en-US'}),errors=[];
page.on('pageerror',error=>errors.push(error.message));
try{
 const response=await page.request.get(new URL('/api/evaluation',url).href),data=await response.json();
 assert.ok(data.result?.complete&&data.result.tasks.length>0,'A real completed evaluation is required');
 assert.equal((await page.request.post(new URL('/api/evaluation',url).href)).status(),405);
 for(const path of ['/training.bcmc','/evaluation-details.json','/control.properties'])assert.equal((await page.request.get(new URL(path,url).href)).status(),404);
 await page.goto(url,{waitUntil:'networkidle'});
 await page.waitForFunction(()=>document.querySelectorAll('#evaluation-results .row').length>0);
 assert.equal(await page.locator('#evaluation-results .row').count(),data.result.tasks.length);
 for(const task of data.result.tasks)assert.ok((await page.locator('#evaluation-results').innerText()).includes(task.passed+'/'+task.cases));
 assert.ok((await page.locator('#evaluation-identity').innerText()).includes('tested snapshot'));
 await page.screenshot({path:'.build/evaluation-desktop.png',fullPage:true});
 await page.setViewportSize({width:390,height:844});
 assert.ok(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth));
 await page.screenshot({path:'.build/evaluation-mobile.png',fullPage:true});
 await page.route('**/api/evaluation',async route=>{
  const response=await route.fetch(),sample=await response.json();
  sample.monitor={state:'running',epoch_millis:Date.now()-120000,detail:'Isolated browser fixture'};
  await route.fulfill({response,json:sample});
 });
 await page.reload({waitUntil:'networkidle'});
 await page.waitForFunction(()=>document.getElementById('evaluation-progress')?.textContent.includes('stale'));
 await page.unroute('**/api/evaluation');
 await page.route('**/api/evaluation',async route=>{
  const response=await route.fetch(),sample=await response.json();sample.result.tasks[0].passed=-1;
  await route.fulfill({response,json:sample});
 });
 await page.reload({waitUntil:'networkidle'});
 await page.waitForFunction(()=>document.getElementById('evaluation-progress')?.textContent.includes('Invalid'));
 assert.equal(await page.locator('#evaluation-results .row').count(),0);
 assert.equal(await page.locator('#state').textContent(),'RUNNING');
 assert.deepEqual(errors,[]);
 console.log('PASS measured evaluation rows, snapshot identity, desktop/mobile layout, stale evaluator, malformed-report isolation and read-only routes');
}finally{await browser.close();}
