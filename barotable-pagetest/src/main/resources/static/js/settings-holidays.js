/* HTML UI: 운영 조건 보완
 * - 휴무일 설정 카드 (매주 정기 휴무 + 특정 날짜 휴무, 이 브라우저에만 저장하는 시연 데이터) */
(()=>{
 const host=document.querySelector('.owner-main')||document.querySelector('main');if(!host)return;
 const card=document.createElement('section');card.className='card stack';card.style.cssText='margin-top:16px';
 card.innerHTML=`<div><h2>휴무일 설정</h2><p class="hint">매주 반복되는 정기 휴무일과 특정 날짜의 임시 휴무일을 설정합니다.</p></div>
 <div class="field"><label>매주 정기 휴무</label><div id="weekly-days" style="display:flex;flex-wrap:wrap;gap:8px">${['일','월','화','수','목','금','토'].map((d,i)=>`<label style="display:flex;align-items:center;gap:5px;border:1px solid var(--line);border-radius:9px;padding:8px 11px"><input type="checkbox" value="${i}"> ${d}요일</label>`).join('')}</div></div>
 <div class="field"><label for="holiday-date">특정 날짜 휴무</label><div style="display:flex;gap:8px;flex-wrap:wrap"><input class="input" id="holiday-date" type="date" style="max-width:220px"><input class="input" id="holiday-name" type="text" placeholder="휴무 사유 (선택)" style="max-width:240px"><button class="btn btn-ghost" id="holiday-add" type="button">날짜 추가</button></div><ul id="holiday-list" style="padding-left:20px;margin:8px 0"></ul></div>
 <div style="display:flex;justify-content:flex-end"><button class="btn btn-primary" id="holiday-save" type="button">휴무일 저장</button></div>`;
 const grid=host.querySelector('.grid.cols-2');if(grid)grid.appendChild(card);else host.appendChild(card);
 let data={weekly:[],dates:[]};try{data=JSON.parse(sessionStorage.getItem('baro-holidays')||'{"weekly":[],"dates":[]}')}catch(e){}
 card.querySelectorAll('#weekly-days input').forEach(x=>x.checked=data.weekly.includes(Number(x.value)));
 const list=card.querySelector('#holiday-list');const draw=()=>{list.innerHTML='';data.dates.forEach((d,i)=>{const li=document.createElement('li');li.textContent=d.date+(d.reason?' · '+d.reason:'')+' ';const b=document.createElement('button');b.type='button';b.className='btn btn-ghost btn-sm';b.textContent='삭제';b.addEventListener('click',()=>{data.dates.splice(i,1);draw()});li.appendChild(b);list.appendChild(li)})};draw();
 card.querySelector('#holiday-add').addEventListener('click',()=>{const date=card.querySelector('#holiday-date').value;if(!date)return alert('휴무 날짜를 선택해 주세요.');if(!data.dates.some(x=>x.date===date))data.dates.push({date,reason:card.querySelector('#holiday-name').value.trim()});card.querySelector('#holiday-date').value='';card.querySelector('#holiday-name').value='';draw()});
 card.querySelector('#holiday-save').addEventListener('click',()=>{data.weekly=[...card.querySelectorAll('#weekly-days input:checked')].map(x=>Number(x.value));sessionStorage.setItem('baro-holidays',JSON.stringify(data));alert('휴무일 설정을 저장했습니다. (현재 브라우저 시연 데이터)')});
})();
