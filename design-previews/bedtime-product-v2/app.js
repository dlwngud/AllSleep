const byId = id => SCREENS.find(s=>s.id===id);
const demo = {bedtime:'23:30',release:'07:00',plan:'monthly',flow:'first',screen:'O01',history:[],loggedIn:false,plus:false,trialEligible:true,authTarget:'M01'};
const navItems=[['오늘','home','H01'],['루틴','clock','R01'],['기록','chart','S01'],['마이','user','M01']];
let toastTimer;
function toast(text){const el=document.getElementById('toast');el.textContent=text;el.classList.add('show');clearTimeout(toastTimer);toastTimer=setTimeout(()=>el.classList.remove('show'),2300)}
function renderPhone(id,staticMode=false){
 const s=byId(id);if(!s)throw new Error('Unknown screen '+id);
 const sub=s.secondary.map(([a,b])=>`<button data-go="${b}">${a}</button>`).join('');
 const title=s.custom?'':`<h1>${s.title}</h1>${s.desc?`<p class="screen-desc">${s.desc}</p>`:''}`;
 let screenBody=(title+s.body).replaceAll('[[BEDTIME]]',staticMode?'23:30':demo.bedtime).replaceAll('[[RELEASE]]',staticMode?'07:00':demo.release);
 if(!staticMode){
  const [h,m]=demo.bedtime.split(':').map(Number),prep=(h*60+m-15+1440)%1440;
  screenBody=screenBody.replaceAll('23:15',String(Math.floor(prep/60)).padStart(2,'0')+':'+String(prep%60).padStart(2,'0'));
  if(['O03','R02'].includes(id))screenBody=screenBody.replace('value="23:30"',`value="${demo.bedtime}"`).replace('value="07:00"',`value="${demo.release}"`);
  if(id==='H01'&&demo.skipped)screenBody=screenBody.replace('예약 준비 완료','오늘 예약 건너뜀').replace('추가 기기는 Plus에서 연결','다음 회차부터 기본 루틴으로 돌아와요');
  if(id==='N06'&&demo.earlyEnd)screenBody=screenBody.replaceAll('7시간 17분','4분').replaceAll('07:00 예정 해제','23:42 직접 종료').replaceAll('잠깐 사용 1회 · 5분','필요에 따라 오늘 제한을 끝냈어요.');
  if(id==='P05'&&demo.plan==='annual'){screenBody=screenBody.replace('월간 Plus','연간 Plus').replace('₩4,900','₩39,000').replace('/ 월','/ 년').replace('2026년 11월 12일',demo.trialEligible?'2026년 10월 19일':'2027년 10월 12일').replace('매월 자동 갱신',demo.trialEligible?'체험 후 매년 자동 갱신':'매년 자동 갱신');if(demo.trialEligible)screenBody=screenBody.replace('이용 중','체험 중')}
  if(id==='R01')screenBody=screenBody.replace('00:00',demo.weekendBedtime||'00:00').replace('08:00',demo.weekendRelease||'08:00');
 }
 if(id==='P06'&&demo.plan==='annual')screenBody=screenBody.replaceAll('11월 12일',demo.trialEligible?'10월 19일':'10월 12일');
 if(!staticMode&&id==='R07')screenBody=screenBody.replace('value="00:00"',`value="${demo.weekendBedtime||'00:00'}"`).replace('value="08:00"',`value="${demo.weekendRelease||'08:00'}"`);
 const rootToolbar=s.brand||s.nav||s.quiet;
 const top=`<div class="app-topbar">${rootToolbar?'<div class="app-brand">All<span>Sleep</span></div>':`<button aria-label="이전 화면" data-back="${s.back||''}">${ico('back')}</button>`}<span class="step">${s.step||s.nav||''}</span>${rootToolbar?'':`<button data-go="H01" aria-label="오늘 화면으로">${ico('close')}</button>`}</div>`;
 let cta=s.cta;if(s.plan&&!staticMode&&demo.plan==='annual')cta=demo.trialEligible?'7일 체험 후 연 39,000원':'연 39,000원으로 구독';
 return `<div class="phone ${s.nav?'has-nav':''} ${s.center?'center':''} ${s.quiet?'quiet':''}" data-screen="${s.id}"><div class="statusbar"><span>${s.morning?'07:10':s.quiet?'23:38':'22:45'}</span><div class="status-icons"><span class="signal"><i></i><i></i><i></i></span>${ico('wifi')}<span class="battery"><i></i></span></div></div>${top}<div class="screen-scroll">${screenBody}</div><div class="screen-footer"><button class="primary ${s.danger?'danger':''}" data-primary data-go="${s.next}">${cta}</button>${sub?`<div class="footer-secondary">${sub}</div>`:''}${s.nav?'':'<span class="gesture"></span>'}</div>${s.nav?`<nav class="app-nav" aria-label="앱 주요 화면">${navItems.map(([label,icon,id])=>`<button data-go="${!staticMode&&label==='마이'?(demo.loggedIn?(demo.plus?'M10':'M01'):'M09'):id}" class="${label===s.nav?'active':''}">${ico(icon)}<span>${label}</span></button>`).join('')}<span class="gesture"></span></nav>`:''}</div>`;
}
function renderCatalog(search=''){const term=search.toLowerCase();document.getElementById('catalogList').innerHTML=GROUPS.map(g=>{const list=SCREENS.filter(s=>s.group===g&&(!term||`${s.name} ${s.id} ${s.group}`.toLowerCase().includes(term)));return list.length?`<h3 class="group-title">${g} · ${list.length}</h3>${list.map(s=>`<button class="catalog-item ${s.id===demo.screen?'selected':''}" data-screen-select="${s.id}"><span>${s.id}</span>${s.name}</button>`).join('')}`:''}).join('')}
function plain(html){return html.replace(/<[^>]*>/g,' ').replace(/\s+/g,' ').trim()}
function renderSpec(s){document.getElementById('specPanel').innerHTML=`<span class="spec-id">${s.id} / ${s.group}</span><h2>${s.name}</h2><div class="spec-section"><h3>이 화면의 역할</h3><p>${s.goal}</p></div><div class="spec-section"><h3>주요 행동</h3><div class="spec-cta">${s.cta}<br><small>→ ${s.next} · ${byId(s.next).name}</small></div></div><div class="spec-section"><h3>예외와 상태</h3><p>${s.edge}</p></div><div class="spec-section"><h3>구현 완료 기준</h3><p>${s.accept}</p></div><div class="spec-section"><h3>시안에서 확인할 수 있는 것</h3><p>주요·보조 버튼과 하단 탭으로 화면을 이동합니다. 선택 항목·알림 스위치·요일·요금제를 변경할 수 있습니다. 입력값은 저장·전송하지 않습니다.</p></div><div class="spec-links"><a href="SCREEN-SPEC.md" target="_blank">전체 화면 명세 ↗</a><a href="product-plan.html" target="_blank">제품 기획서 ↗</a></div>`}
function showScreen(id,push=true){if(!byId(id))return;if(push&&id!==demo.screen)demo.history.push(demo.screen);demo.screen=id;document.getElementById('phoneMount').innerHTML=renderPhone(id);document.getElementById('previewId').textContent=id;document.getElementById('previewName').textContent=byId(id).name;renderSpec(byId(id));renderCatalog(document.getElementById('screenSearch').value);const f=FLOWS[demo.flow];const i=f.ids.indexOf(id);document.getElementById('flowPosition').textContent=i>=0?`${i+1} / ${f.ids.length}`:'연결 화면';if(id==='P02')applyPlan();if(id==='P03')applyStore();history.replaceState(null,'',`#${id}`)}
function switchView(view){for(const v of ['prototype','gallery','principles'])document.getElementById(v+'-view').classList.toggle('hidden',v!==view);document.querySelectorAll('[data-view]').forEach(el=>el.classList.toggle('active',el.dataset.view===view));if(view==='gallery')renderGallery();}
function renderGallery(){const g=document.getElementById('galleryGroup').value;const ss=SCREENS.filter(s=>g==='all'||s.group===g);document.getElementById('galleryGrid').innerHTML=ss.map(s=>`<article class="gallery-card">${renderPhone(s.id,true)}<div class="gallery-meta"><div><span>${s.id} / ${s.group}</span><h2>${s.name}</h2></div><button class="gallery-go" data-screen-select="${s.id}">화면 열기 ↗</button></div></article>`).join('')}
function applyPlan(){const mount=document.getElementById('phoneMount');mount.querySelectorAll('[data-plan]').forEach(el=>{const yes=el.dataset.plan===demo.plan;el.classList.toggle('selected',yes);el.setAttribute('aria-pressed',yes)});const info=mount.querySelector('[data-plan-info]');if(info)info.innerHTML=demo.plan==='monthly'?'<strong>오늘 4,900원 결제</strong><p>매월 4,900원 자동 갱신.<br>Google Play 구독 관리에서 해지할 수 있어요.</p>':'<strong>7일 체험 후 연 39,000원</strong><p>체험 시작 예시 10월 12일 → 10월 19일 청구.<br>이후 매년 39,000원 자동 갱신.<br>체험 종료 전 Google Play에서 해지 가능.</p>';const cta=mount.querySelector('[data-primary]');if(cta)cta.textContent=demo.plan==='monthly'?'월 4,900원으로 구독':demo.trialEligible?'7일 체험 후 연 39,000원':'연 39,000원으로 구독';if(demo.plan==='annual'&&!demo.trialEligible&&info)info.innerHTML='<strong>오늘 39,000원 결제</strong><p>무료 체험이 적용되지 않아요.<br>이후 매년 39,000원 자동 갱신.<br>Google Play에서 해지할 수 있어요.</p>'}
function applyStore(){const mount=document.getElementById('phoneMount');const p=mount.querySelector('[data-store-price]');const t=mount.querySelector('[data-store-terms]');if(p)p.textContent=demo.plan==='annual'?(demo.trialEligible?'7일 체험 후 연 39,000원':'오늘 39,000원 · 연간'):'월 4,900원';if(t)t.innerHTML=demo.plan==='annual'?(demo.trialEligible?'체험 종료 예시 2026년 10월 19일<br>이후 매년 자동 갱신 · 스토어에서 해지 가능':'체험 없음 · 오늘 결제<br>이후 매년 자동 갱신 · 스토어에서 해지 가능'):'매월 자동 갱신<br>스토어에서 해지 가능'}
function saveFields(){const mount=document.getElementById('phoneMount');for(const n of ['bedtime','release']){const f=mount.querySelector(`[data-field="${n}"]`);if(f&&f.value)demo[n]=f.value}const over=mount.querySelector('[data-field="override"]');if(over&&over.value)demo.bedtime=over.value;if(demo.screen==='R07'){for(const n of ['weekendBedtime','weekendRelease']){const f=mount.querySelector(`[data-field="${n}"]`);if(f)demo[n]=f.value}}if(demo.screen==='H03')demo.skipped=!!mount.querySelector('[data-choice="override"].selected strong')?.textContent.includes('건너뛰기');if(demo.screen==='H05')demo.bedtime='23:40';if(demo.screen==='N04')demo.earlyEnd=true;if(demo.screen==='H06')demo.earlyEnd=false}
document.addEventListener('click',e=>{
 const target=e.target.closest('button,a');if(!target)return;
 if(target.dataset.view){switchView(target.dataset.view);return}
 if(target.dataset.screenSelect){switchView('prototype');showScreen(target.dataset.screenSelect);return}
 if(target.hasAttribute('data-back')){const id=target.dataset.back||demo.history.pop()||'H01';showScreen(id,false);return}
 if(target.hasAttribute('data-toggle')){const on=target.getAttribute('aria-pressed')!=='true';target.setAttribute('aria-pressed',on);target.querySelector('.switch').classList.toggle('on',on);return}
 if(target.hasAttribute('data-day')){const on=!target.classList.contains('selected');target.classList.toggle('selected',on);target.setAttribute('aria-pressed',on);return}
 if(target.dataset.choice){const multi=['apps','choice'].includes(target.dataset.choice);const parent=target.closest('.screen-scroll');if(!multi)parent.querySelectorAll(`[data-choice="${target.dataset.choice}"]`).forEach(el=>{if(el!==target){el.classList.remove('selected');el.setAttribute('aria-pressed','false');el.querySelector('.select-mark').innerHTML=''}});if(target.dataset.choice==='choice'&&!target.classList.contains('selected')&&parent.querySelectorAll('[data-choice="choice"].selected').length>=2){toast('최대 2개를 선택할 수 있어요.');return}const yes=!target.classList.contains('selected');target.classList.toggle('selected',yes);target.setAttribute('aria-pressed',yes);target.querySelector('.select-mark').innerHTML=yes?ico('check'):'';return}
 if(target.dataset.plan){demo.plan=target.dataset.plan;applyPlan();return}
 if(target.dataset.go){
  if(target.hasAttribute('data-primary')){
   if(['O03','R02','H03','R07','R08'].includes(demo.screen)){const m=document.getElementById('phoneMount');const start=m.querySelector('[data-field="bedtime"],[data-field="override"],[data-field="weekendBedtime"],[data-field="customBedtime"]');const end=m.querySelector('[data-field="release"],[data-field="weekendRelease"],[data-field="customRelease"]');if(start&&end&&start.value===end.value){toast('시작과 해제 시각을 다르게 선택해 주세요.');return}}
   if(demo.screen==='O04'&&!document.querySelector('#phoneMount .app-row.selected')){toast('제한할 앱을 한 개 이상 선택해 주세요.');return}
   saveFields();if(['R02','R03','R04','R05','M02','H03','H05'].includes(demo.screen))toast('시안에 선택을 적용했어요. 실제 설정은 변경하지 않아요.');
  }
  let next=target.dataset.go;
  if(next==='O04'&&demo.screen.startsWith('R'))demo.appReturn=demo.screen;
  if(target.hasAttribute('data-primary')){
   if(demo.screen==='O04'&&demo.appReturn){next=demo.appReturn;demo.appReturn=null}
   if(['D02','P02','P10'].includes(demo.screen)&&!demo.loggedIn){demo.authTarget=demo.screen==='D02'?'D02':'P02';next='A01'}
   if(demo.screen==='A05'){demo.loggedIn=true;next=demo.authTarget==='D02'&&!demo.plus?'P01':demo.authTarget}
   if(demo.screen==='P04'){demo.plus=true;demo.loggedIn=true}
   if(['M12','M14'].includes(demo.screen)){demo.loggedIn=false;demo.plus=false}
   if(demo.screen==='P10'){demo.plan='annual';demo.trialEligible=false}
  }
  showScreen(next);return
 }
});
document.getElementById('screenSearch').addEventListener('input',e=>renderCatalog(e.target.value));
document.getElementById('phoneMount').addEventListener('input',e=>{if(e.target.dataset.field==='appSearch'){const term=e.target.value.toLowerCase();document.querySelectorAll('#phoneMount .app-row').forEach(el=>el.hidden=!el.textContent.toLowerCase().includes(term));}});
document.getElementById('flowSelect').innerHTML=Object.entries(FLOWS).map(([k,f])=>`<option value="${k}">${f.label}</option>`).join('');
document.getElementById('flowSelect').addEventListener('change',e=>{demo.flow=e.target.value;showScreen(FLOWS[demo.flow].ids[0]);});
document.getElementById('flowStart').addEventListener('click',()=>{demo.history=[];showScreen(FLOWS[demo.flow].ids[0],false)});
document.getElementById('backScreen').addEventListener('click',()=>showScreen(demo.history.pop()||'O01',false));
document.getElementById('nextFlow').addEventListener('click',()=>{const list=FLOWS[demo.flow].ids;const i=list.indexOf(demo.screen);showScreen(list[(i+1)%list.length])});
document.getElementById('galleryGroup').insertAdjacentHTML('beforeend',GROUPS.map(g=>`<option value="${g}">${g}</option>`).join(''));
document.getElementById('galleryGroup').addEventListener('change',renderGallery);
document.getElementById('screenCount').textContent=SCREENS.length;
document.getElementById('catalogCount').textContent=SCREENS.length+' SCREENS';
const initial=location.hash.slice(1);showScreen(byId(initial)?initial:'O01',false);
window.AllSleepDesign={screens:SCREENS,groups:GROUPS,renderPhone,showScreen,switchView,demo};
