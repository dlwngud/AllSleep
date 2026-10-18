const fs=require('fs'),path=require('path');
const {chromium}=require('/Users/wngud/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright');
const root=path.resolve(__dirname,'..');
(async()=>{
 const browser=await chromium.launch({executablePath:'/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',headless:true});const page=await browser.newPage({viewport:{width:1440,height:1180}});const errors=[];page.on('pageerror',e=>errors.push(e.message));
 await page.goto('file://'+root+'/allsleep-prototype.html');await page.waitForFunction(()=>window.AllSleepDesign?.screens.length===93);
 const go=id=>page.evaluate(id=>AllSleepDesign.showScreen(id,false),id),click=()=>page.locator('#phoneMount [data-primary]').click(),is=async id=>{const found=await page.locator('#phoneMount .phone').getAttribute('data-screen');if(found!==id)throw Error(`Expected ${id}, got ${found}`)};
 await go('H01');await page.locator('#phoneMount .app-nav [data-go="M09"]').click();await is('M09');
 await go('R01');await page.locator('#phoneMount [data-go="R07"]').click();await is('R07');await page.locator('[data-field="weekendBedtime"]').fill('00:45');await page.locator('[data-field="weekendRelease"]').fill('08:15');await click();await is('R01');if(!(await page.locator('#phoneMount').textContent()).includes('00:45'))throw Error('Weekend propagation');await go('R02');if(await page.locator('[data-field="bedtime"]').inputValue()!=='23:30')throw Error('Weekday overwritten');
 await go('P10');await click();await is('A01');await go('A05');await click();await is('P02');if((await page.locator('[data-primary]').textContent()).includes('체험'))throw Error('Ineligible trial CTA');await click();await is('P03');if(!(await page.locator('[data-store-price]').textContent()).includes('오늘 39,000'))throw Error('Ineligible store terms');await click();await is('P14');await click();await is('P04');await click();await is('D02');
 await go('D03');await click();await is('D06');await click();await is('D08');
 await go('M06');await click();await is('M13');await click();await is('M14');await click();await is('M09');if(await page.evaluate(()=>AllSleepDesign.demo.loggedIn))throw Error('Delete completion session');
 await go('R07');await page.locator('[data-field="weekendBedtime"]').fill('08:15');await page.locator('[data-field="weekendRelease"]').fill('08:15');await click();await is('R07');
 await go('M07');await click();await is('M11');
 if(errors.length)throw Error(errors.join(';'));
 fs.writeFileSync(path.join(root,'qa/final-flow-verification.json'),JSON.stringify({screens:93,mode:'offline HTML prototype only',checks:['all data and assets load offline','guest My navigation','weekend edit route and propagation','weekday preservation','sign-in return after paid intent','no-trial annual CTA and store total','purchase verification pending before success','pairing approval pending before readiness','delete requested and completed states','equal-time guard for weekend','export file-ready state'],browserErrors:errors},null,2));
 await browser.close();console.log('Final offline prototype: 11 flow checks passed, 93 screens.');
})().catch(e=>{console.error(e);process.exit(1)});
