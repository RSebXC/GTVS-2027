import {writeFile} from 'node:fs/promises';
const base='https://br-morning-resonance-belnuka7-gtvsapi.compute.c-9.us-east-2.aws.neon.tech/api';
const scenarios=[{name:'Círculo ficticio referencia El Gallito',origin:[-90.541,14.636],destination:[-90.516,14.636]},{name:'Círculo ficticio referencia Landívar',origin:[-90.546,14.6218],destination:[-90.526,14.6218]}];
const results=[];
for(const scenario of scenarios){const response=await fetch(base+'/routes',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(scenario),signal:AbortSignal.timeout(100000)});const data=await response.json();if(!response.ok)throw new Error(data.error);const result={...scenario,avoidance:data.avoidance,routes:data.routes.map(r=>({profiles:r.profiles,via:r.via||[],crossings:r.risk.restrictions.length,distance:r.distance,duration:r.duration,index:r.risk.value}))};results.push(result);console.log(JSON.stringify(result));}
await writeFile('docs/evidencias/v2/desvios_demo4.json',JSON.stringify({at:new Date().toISOString(),notice:'Sectores inventados y candidatos OSRM reales. No valida seguridad urbana.',results},null,2));
if(!results.some(r=>r.routes.some(x=>x.crossings>0)&&r.routes.some(x=>x.crossings===0)))throw new Error('No se encontró aún una comparación de referencia y desvío sin cruces.');
