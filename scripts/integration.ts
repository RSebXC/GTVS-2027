import {config} from 'dotenv';config({path:'.env.local',quiet:true});
import {randomBytes,randomUUID} from 'node:crypto';
import {mkdir,writeFile} from 'node:fs/promises';
import pg from 'pg';
const {pool}=await import('../src/db.ts');
const base=process.env.NEON_AUTH_BASE_URL;
if(!base)throw new Error('Falta Neon Auth; ejecutar neon deploy antes de verificar.');
const results:{case:string;ok:boolean}[]=[];
const users:string[]=[],reportIds:string[]=[],incidentIds:string[]=[],flagIds:string[]=[];
function check(condition:unknown,label:string){if(!condition)throw new Error(label);results.push({case:label,ok:true});console.log('OK · '+label);}
async function authUser(){
 let cookies='';
 async function request(path:string,data?:unknown){const r=await fetch(base+path,{method:data?'POST':'GET',headers:{'Content-Type':'application/json','Origin':'http://localhost:5173',...(cookies?{Cookie:cookies}:{})},body:data?JSON.stringify(data):undefined});const newCookies=r.headers.getSetCookie().map(s=>s.split(';')[0]);if(newCookies.length)cookies=newCookies.join('; ');let body:any;try{body=await r.json();}catch{throw new Error('Auth no respondió JSON');}if(!r.ok)throw new Error(`Auth ${path}: HTTP ${r.status} ${body.code||body.message||''}`);return body;}
 const email='gtvs-e2e-'+randomUUID()+'@example.com',password=randomBytes(24).toString('base64url');
 const created=await request('/sign-up/email',{name:'Prueba automática GTVS',email,password});const id=created.user?.id;if(!id)throw new Error('No se creó el usuario de prueba.');users.push(id);
 await request('/sign-out',{});cookies='';await request('/sign-in/email',{email,password});const session=await request('/get-session');if(session.user?.id!==id)throw new Error('La sesión no se restauró.');const jwt=await request('/token');if(typeof jwt.token!=='string')throw new Error('El proveedor no devolvió JWT.');return {id,token:jwt.token};
}
async function call(path:string,jwt?:string,body?:unknown,method=body?'POST':'GET',key?:string){const response=await fetch((process.env.GTVS_TEST_BASE_URL||'http://127.0.0.1:8787')+path,{method,headers:{...(jwt?{Authorization:'Bearer '+jwt}:{}),...(body?{'Content-Type':'application/json'}:{}),...(key?{'Idempotency-Key':key}:{})},body:body?JSON.stringify(body):undefined,signal:AbortSignal.timeout(30000)});return {status:response.status,body:await response.json() as any};}
let failed:string|undefined;
try{
 const health=await call('/api/health');check(health.status===200&&health.body.ok,'consulta real a Neon Postgres');
 check((await call('/api/reports',undefined,{})).status===401,'reporte anónimo rechazado');
 const a=await authUser(),b=await authUser();check(Boolean(a.token&&b.token),'alta, salida, ingreso, sesión y JWT de dos cuentas reales de prueba');
 check((await call('/api/me',a.token)).status===200,'JWT del proveedor validado por la API');
 check((await call('/api/admin/reports',a.token)).status===403,'colaborador no puede consultar administración');
 await pool.query('INSERT INTO gtvs.admins(user_id) VALUES($1)',[b.id]);
 const before=(await pool.query('SELECT count(*)::integer AS n FROM gtvs.incidents WHERE active')).rows[0].n;
 const payload={type:'obstruccion',description:'Simulación automática, no describe un hecho real.',coordinate:[-90.52,14.63],severity:3,occurredAt:new Date().toISOString(),synthetic:true};
 const key=randomUUID(),r=await call('/api/reports',a.token,payload,'POST',key);check(r.status===201&&r.body.report.state==='pending','reporte autenticado persistido como pendiente');reportIds.push(r.body.report.id);
 const repeat=await call('/api/reports',a.token,payload,'POST',key);check(repeat.body.report.id===r.body.report.id,'reenvío idempotente no duplica reporte');
 check((await call('/api/reports',a.token,{...payload,severity:4},'POST',key)).status===409,'clave con contenido distinto rechazada');
 check((await pool.query('SELECT count(*)::integer AS n FROM gtvs.incidents WHERE active')).rows[0].n===before,'pendiente no crea incidente');
 const approved=await call('/api/admin/reports/'+r.body.report.id+'/review',b.token,{action:'approve',reason:'Hecho ficticio creado solo para comprobar integración.',revision:1});if(approved.body.incidentId)incidentIds.push(approved.body.incidentId);check(approved.status===200&&approved.body.incidentId,'administrador acepta y consolida evidencia');
 const r2=await call('/api/reports',a.token,{...payload,description:'Segunda evidencia ficticia del mismo hecho de prueba.'},'POST',randomUUID());reportIds.push(r2.body.report.id);
 const linked=await call('/api/admin/reports/'+r2.body.report.id+'/review',b.token,{action:'approve',reason:'Se vincula al mismo hecho ficticio para no contarlo dos veces.',revision:1,incidentId:approved.body.incidentId});
 check(linked.status===200&&(await pool.query('SELECT count(*)::integer AS n FROM gtvs.incidents WHERE active')).rows[0].n===before+1,'vinculación de dos evidencias conserva un incidente');
 check((await call('/api/admin/reports/'+r2.body.report.id+'/review',b.token,{action:'reject',reason:'Revisión obsoleta que debe ser rechazada.',revision:1})).status===409,'revisión concurrente obsoleta rechazada');
 const flag=await call('/api/admin/restrictions',b.token,{coordinate:[-90.52,14.63],radiusMeters:150,reason:'Restricción ficticia temporal para prueba de integración.',reviewAt:new Date(Date.now()+86400000).toISOString(),synthetic:true});if(flag.body.id)flagIds.push(flag.body.id);check(flag.status===201,'restricción administrativa persistida con revisión');
 check((await call('/api/admin/restrictions/'+flag.body.id,b.token,{active:false},'PATCH')).body.retired,'restricción puede retirarse');
 const external=await call('/api/routes',undefined,{origin:[-90.521,14.625],destination:[-90.511,14.638]});check(external.status===200&&external.body.routes.length>0,'ruta real de OSRM consultada y evaluada con Neon');
 const search=await call('/api/search?q='+encodeURIComponent('Parque Central Guatemala'));check(search.status===200&&search.body.places.length>0,'búsqueda geográfica real de Photon');
}catch(e){failed=(e as Error).message;console.error('FALLÓ · '+failed);process.exitCode=1;}finally{
 // Solo limpiar identificadores creados por esta ejecución en la rama de demostración.
 const cleanup=await pool.connect();try{await cleanup.query('BEGIN');if(reportIds.length){await cleanup.query('DELETE FROM gtvs.reviews WHERE report_id=ANY($1::uuid[])',[reportIds]);await cleanup.query('DELETE FROM gtvs.reports WHERE id=ANY($1::uuid[])',[reportIds]);}if(incidentIds.length)await cleanup.query('DELETE FROM gtvs.incidents WHERE id=ANY($1::uuid[])',[incidentIds]);if(flagIds.length)await cleanup.query('DELETE FROM gtvs.restrictions WHERE id=ANY($1::uuid[])',[flagIds]);if(users.length)await cleanup.query('DELETE FROM gtvs.admins WHERE user_id=ANY($1::text[])',[users]);await cleanup.query('COMMIT');}catch{await cleanup.query('ROLLBACK');console.error('Revisar limpieza de datos de prueba.');}finally{cleanup.release();}
 await mkdir('docs/evidencias/v2',{recursive:true});await writeFile('docs/evidencias/v2/integracion.json',JSON.stringify({at:new Date().toISOString(),scope:'Datos ficticios; Neon Auth, Postgres y proveedores externos reales. Sin validación de riesgo urbano.',results,failed:failed||null,notes:'Se retiraron filas de prueba de GTVS y permisos temporales. Las cuentas de prueba sin datos personales permanecen en el servicio Auth; no se imprimen credenciales.'},null,2));await pool.end();
}
