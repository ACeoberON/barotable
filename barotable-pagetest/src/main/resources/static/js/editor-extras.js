/* HTML UI: 좌석도 편집 보완
 * - 점주 첫 설정(온보딩) 중이면 '배치 저장 후 고객 화면 확인' 버튼을 보여 준다 */
(()=>{
 if(sessionStorage.getItem('baro-owner-onboarding')==='layout'){
  const bar=document.createElement('div');bar.style.cssText='position:sticky;top:8px;z-index:40;display:flex;justify-content:flex-end;margin:0 0 12px';
  bar.innerHTML='<button type="button" class="btn btn-primary" id="onboard-next">배치 저장 후 고객 화면 확인</button>';
  const main=document.querySelector('.owner-main');if(main)main.prepend(bar);
  bar.querySelector('button').addEventListener('click',()=>{const save=document.querySelector('#layout-save');if(save)save.click();sessionStorage.setItem('baro-owner-onboarding','preview');setTimeout(()=>BT.go('owner-onboarding-preview'),250);});
 }
})();
