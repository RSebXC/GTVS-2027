import {icon} from './icons';
const labels:Record<string,string>={robo_ataque:'Robo o ataque',obstruccion:'Obstrucción',cierre:'Cierre',otro:'Otro contexto'};
const symbols:Record<string,string>={robo_ataque:'shield',obstruccion:'report',cierre:'barrier',otro:'info'};
const el=(tag:string,text?:string,cls?:string)=>{const e=document.createElement(tag);if(text)e.textContent=text;if(cls)e.className=cls;return e;};
const $=(id:string)=>document.getElementById(id)!;
export function reportsPanel(options:{api:(path:string)=>Promise<any>;incidents:()=>any[];locate:(item:any)=>void;login:()=>void}){
 let userId:string|null=null,own:any[]=[];let scope='public';
 function render(){
  const all=options.incidents(),type=($('feed-type') as HTMLSelectElement).value,days=Number(($('feed-age') as HTMLSelectElement).value);
  const items=(scope==='mine'?own:all).filter(i=>(!type||i.type===type)&&(!days||Date.parse(i.occurred_at||i.created_at)>Date.now()-days*86400000));
  $('feed-total').textContent=String(all.length);$('feed-seeded').textContent=String(all.filter(i=>i.demo_batch).length);$('feed-recent').textContent=String(all.filter(i=>Date.parse(i.occurred_at)>Date.now()-86400000).length);
  $('feed-count').textContent=items.length+' registros visibles';$('feed-list').replaceChildren();
  if(!items.length)$('feed-list').append(el('p',scope==='mine'?'No tienes reportes con estos filtros.':'No hay incidentes con estos filtros.','empty-state'));
  for(const item of items){const card=el('article',undefined,'incident-card');const heading=el('div',undefined,'incident-heading');heading.append(icon(symbols[item.type]),el('strong',labels[item.type]||'Contexto'));card.append(heading);
   card.append(el('span',item.demo_batch?'ESCENARIO PRECARGADO · FICTICIO':scope==='mine'?({pending:'PENDIENTE',approved:'APROBADO',rejected:'RECHAZADO'} as any)[item.state]+' · FICTICIO':'INCIDENTE FICTICIO','incident-badge'));
   card.append(el('p',item.title||item.description),el('p',new Date(item.occurred_at||item.created_at).toLocaleString('es-GT'),'incident-date'));
   if(!item.occurred_at)card.append(el('small','Ocurrencia desconocida; se muestra fecha de registro.'));
   if(scope==='mine')card.append(el('p',item.state==='pending'?'Pendiente de moderación: aún no influye en rutas.':item.state==='approved'?'Aporte aprobado por moderación.':'Aporte rechazado: no crea contexto de ruta.','help'));
   const button=el('button',undefined,'button outline') as HTMLButtonElement;button.type='button';button.append(icon('pin'),el('span','Ver en el mapa'));button.onclick=()=>options.locate(item);card.append(button);$('feed-list').append(card);
  }
 }
 $('feed-type').onchange=render;$('feed-age').onchange=render;
 $('feed-public').onclick=()=>{scope='public';$('feed-public').classList.add('active');$('feed-mine').classList.remove('active');render();};
 $('feed-mine').onclick=async()=>{if(!userId){options.login();return;}try{const current=userId;const data=await options.api('/reports/mine');if(current!==userId)return;own=data.reports;scope='mine';$('feed-mine').classList.add('active');$('feed-public').classList.remove('active');render();}catch(e){$('feed-list').replaceChildren(el('p',(e as Error).message,'form-error'));}};
 return {render,syncIdentity(id:string|null){if(id!==userId){userId=id;own=[];scope='public';$('feed-public').classList.add('active');$('feed-mine').classList.remove('active');render();}}};
}
