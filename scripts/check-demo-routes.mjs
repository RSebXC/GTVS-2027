import {writeFile} from 'node:fs/promises';
const base='https://br-morning-resonance-belnuka7-gtvsapi.compute.c-9.us-east-2.aws.neon.tech/api';
const pairs=[{name:'Ejemplo centro-sur',origin:[-90.5218,14.6349],destination:[-90.5352,14.5905]},{name:'Ejemplo oeste-este',origin:[-90.5505,14.6201],destination:[-90.5034,14.6097]},{name:'Ejemplo oeste-sur',origin:[-90.5505,14.6201],destination:[-90.5352,14.5905]}];
const results=[];
for(const pair of pairs){const r=await fetch(base+'/routes',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(pair),signal:AbortSignal.timeout(35000)});const data=await r.json();if(!r.ok)throw new Error(data.error);const result={...pair,candidates:data.candidates,visible:data.routes.length,routes:data.routes.map(x=>({profiles:x.profiles,incidents:x.risk.uniqueIncidents,index:x.risk.value}))};results.push(result);console.log(JSON.stringify(result));}
await writeFile('docs/evidencias/v2/recorridos_demo3.json',JSON.stringify({at:new Date().toISOString(),notice:'Respuestas reales del proveedor; incidentes ficticios; no son resultados de validación de riesgo.',results},null,2));
