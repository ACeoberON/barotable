/* HTML UI: 마우스로 누른 채 끌어서 스크롤 (포인터 이벤트). 손가락 스크롤은 브라우저 기본 동작 */
(function(){
  const selector='.sheet-horizontal,.sheet-time-scroll,.booking-map-scroll,.sf-area,.tt-scroll,.list-scroll,.adm-table-scroll,body[data-screen="owner-dashboard"] .map-scroll,body[data-screen="owner-guests"] .map-scroll,body[data-screen="owner-editor"] #canvas-scroll,[data-drag-scroll]';
  let drag=null, suppressClick=false, suppressTimer=null;
  function getArea(target){ return target && target.closest ? target.closest(selector) : null; }
  document.addEventListener('pointerdown',function(e){
    if(e.pointerType==='touch' || e.button!==0) return;
    if(e.target.closest('input,textarea,select,option,[contenteditable="true"]')) return;
    const el=getArea(e.target); if(!el) return;
    if(el.id==='canvas-scroll' && e.target.closest('.ed-item')) return;
    if(el.scrollWidth<=el.clientWidth+2 && el.scrollHeight<=el.clientHeight+2) return;
    drag={el,id:e.pointerId,x:e.clientX,y:e.clientY,left:el.scrollLeft,top:el.scrollTop,moved:false,oldCursor:el.style.cursor,oldUserSelect:el.style.userSelect};
  },true);
  document.addEventListener('pointermove',function(e){
    if(!drag || e.pointerId!==drag.id) return;
    const dx=e.clientX-drag.x,dy=e.clientY-drag.y;
    if(!drag.moved && Math.hypot(dx,dy)<5) return;
    drag.moved=true; drag.el.classList.add('is-dragging');
    drag.el.style.cursor='grabbing'; drag.el.style.userSelect='none';
    e.preventDefault(); drag.el.scrollLeft=drag.left-dx; drag.el.scrollTop=drag.top-dy;
  },{capture:true,passive:false});
  function finish(e){
    if(!drag || (e && e.pointerId!==undefined && e.pointerId!==drag.id)) return;
    const ended=drag; drag=null; ended.el.classList.remove('is-dragging');
    ended.el.style.cursor=ended.oldCursor; ended.el.style.userSelect=ended.oldUserSelect;
    if(ended.moved){ suppressClick=true; if(suppressTimer)clearTimeout(suppressTimer); suppressTimer=setTimeout(function(){suppressClick=false;},500); }
  }
  document.addEventListener('pointerup',finish,true);
  document.addEventListener('pointercancel',finish,true);
  document.addEventListener('click',function(e){
    if(!suppressClick)return;
    if(getArea(e.target)){e.preventDefault();e.stopImmediatePropagation();}
    suppressClick=false;if(suppressTimer)clearTimeout(suppressTimer);
  },true);
  document.addEventListener('dragstart',function(e){if(drag && getArea(e.target)===drag.el)e.preventDefault();},true);
})();
