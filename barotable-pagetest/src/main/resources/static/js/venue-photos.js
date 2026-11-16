/* HTML UI: 매장 대표 사진을 ‹ › 버튼으로 넘긴다 */
(()=>{
 const grid=document.querySelector('.venue-photo-grid');if(!grid)return;
 const main=grid.querySelector('.venue-photo-large'),small=grid.querySelector('.venue-photo-small');
 const first=main&&main.querySelector('img'),second=small&&small.querySelector('img');if(!main||!first||!second)return;
 const alt=second.cloneNode(true);alt.alt='매장 내부 사진';alt.style.cssText='position:absolute;inset:0;width:100%;height:100%;object-fit:cover;display:none';main.appendChild(alt);
 main.style.position='relative';let current=0;
 const controls=document.createElement('div');controls.style.cssText='position:absolute;right:14px;top:14px;display:flex;gap:6px;z-index:5';
 controls.innerHTML='<button type="button" aria-label="이전 사진" style="width:34px;height:34px;border:1px solid #ffffffaa;border-radius:50%;background:#ffffffdf;font-size:19px;cursor:pointer">‹</button><button type="button" aria-label="다음 사진" style="width:34px;height:34px;border:1px solid #ffffffaa;border-radius:50%;background:#ffffffdf;font-size:19px;cursor:pointer">›</button>';main.appendChild(controls);
 const show=n=>{current=(n+2)%2;first.style.display=current===0?'block':'none';alt.style.display='block';first.style.opacity=current===0?'1':'0';alt.style.opacity=current===1?'1':'0';};
 controls.children[0].addEventListener('click',()=>show(current-1));controls.children[1].addEventListener('click',()=>show(current+1));show(0);
})();
