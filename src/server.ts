import {Hono} from 'hono';
import {cors} from 'hono/cors';
import {bodyLimit} from 'hono/body-limit';
import {secureHeaders} from 'hono/secure-headers';
import {createRemoteJWKSet,jwtVerify} from 'jose';
import {randomUUID,createHash} from 'node:crypto';
import {pool} from './db.ts';
import {coordinate,rank,MODEL,VIEW_BOUNDS,meters,type Incident,type Flag} from './risk.ts';
import {routing,searchPlaces} from './providers.ts';

type Variables={userId:string;admin:boolean};
const app=new Hono<{Variables:Variables}>();
const types=['robo_ataque','obstruccion','cierre','otro'];
const origins=['http://localhost:5173','http://127.0.0.1:5173','http://localhost:8787','http://127.0.0.1:8787','https://rsebxc.github.io'];
app.use('*',cors({origin:o=>origins.includes(o)?o:'',allowHeaders:['Authorization','Content-Type','Idempotency-Key'],allowMethods:['GET','POST','PATCH','OPTIONS'],maxAge:600}));
app.use('*',secureHeaders());app.use('*',bodyLimit({maxSize:16384,onError:c=>c.json({error:'La solicitud excede 16 KiB.'},413)}));
app.onError((error,c)=>{if(error instanceof InputError)return c.json({error:error.message},422);console.error('Operación no completada:',error.name);return c.json({error:'El servicio no pudo completar la operación. Intenta nuevamente.'},503);});
class InputError extends Error{}
function text(v:unknown,min:number,max:number){if(typeof v!=='string'||v.trim().length<min||v.length>max)throw new InputError(`El texto debe tener entre ${min} y ${max} caracteres.`);return v.trim();}
function point(v:unknown){try{return coordinate(v);}catch(e){throw new InputError((e as Error).message);}}
function source(v:unknown){if(v==null||v==='')return null;const s=text(v,8,500);let u:URL;try{u=new URL(s);}catch{throw new InputError('Referencia inválida.');}if(u.protocol!=='https:')throw new InputError('La referencia debe utilizar HTTPS.');return s;}
function occurred(v:unknown){if(v==null||v==='')return null;if(typeof v!=='string'||!/(Z|[+-]\d{2}:\d{2})$/.test(v))throw new InputError('La fecha debe incluir zona horaria.');const time=Date.parse(v);if(!Number.isFinite(time)||time>Date.now()+300000)throw new InputError('Fecha inválida o futura.');return new Date(time).toISOString();}
function incidentFields(b:any){
 if(!b||typeof b!=='object'||b.synthetic!==true)throw new InputError('Esta demostración solo acepta datos ficticios declarados.');
 if(!types.includes(b.type)||!Number.isInteger(b.severity)||b.severity<1||b.severity>5)throw new InputError('Tipo o gravedad inválidos.');
 const p=point(b.coordinate);return {type:b.type,severity:b.severity,description:text(b.description,10,1500),coordinate:p,occurredAt:occurred(b.occurredAt),source:source(b.sourceUrl)};
}
let jwks:ReturnType<typeof createRemoteJWKSet>|undefined;
app.use('/api/*',async(c,next)=>{
 if(c.req.method==='OPTIONS')return next();
 const header=c.req.header('Authorization');
 if(header){
  try{if(!header.startsWith('Bearer ')||!process.env.NEON_AUTH_JWKS_URL||!process.env.NEON_AUTH_BASE_URL)throw new Error();
   jwks??=createRemoteJWKSet(new URL(process.env.NEON_AUTH_JWKS_URL));const {payload}=await jwtVerify(header.slice(7),jwks,{issuer:new URL(process.env.NEON_AUTH_BASE_URL).origin});if(!payload.sub)throw new Error();
   c.set('userId',payload.sub);let {rowCount}=await pool.query('SELECT user_id FROM gtvs.admins WHERE user_id=$1',[payload.sub]);
   if(!rowCount){
    const {rows}=await pool.query('SELECT email,"emailVerified" FROM neon_auth."user" WHERE id=$1',[payload.sub]);
    if(rows[0]?.emailVerified===true){const hash=createHash('sha256').update(rows[0].email.trim().toLowerCase()).digest('hex');
     const client=await pool.connect();try{await client.query('BEGIN');await client.query("SELECT pg_advisory_xact_lock(hashtext('gtvs-admin-bootstrap'))");const granted=await client.query('INSERT INTO gtvs.admins(user_id) SELECT $1 WHERE EXISTS(SELECT 1 FROM gtvs.admin_invitations WHERE email_hash=$2) AND NOT EXISTS(SELECT 1 FROM gtvs.admins) ON CONFLICT DO NOTHING RETURNING user_id',[payload.sub,hash]);await client.query('COMMIT');rowCount=granted.rowCount;}catch(e){await client.query('ROLLBACK');throw e;}finally{client.release();}
    }
   }
   c.set('admin',Boolean(rowCount));
  }catch{return c.json({error:'La sesión no es válida o expiró. Vuelve a iniciar sesión.'},401);}
 }
 return next();
});
async function limit(c:any,scope:string,max:number){
 const id=c.get('userId')||c.req.header('x-forwarded-for')?.split(',')[0]||'public-demo';
 const key=createHash('sha256').update(scope+id).digest('hex'),bucket=Math.floor(Date.now()/60000);
 const {rows}=await pool.query('INSERT INTO gtvs.rate_limits(key,bucket,count) VALUES($1,$2,1) ON CONFLICT(key) DO UPDATE SET count=CASE WHEN gtvs.rate_limits.bucket=EXCLUDED.bucket THEN gtvs.rate_limits.count+1 ELSE 1 END,bucket=EXCLUDED.bucket RETURNING count',[key,bucket]);
 if(rows[0].count>max)throw new InputError('Límite de demostración alcanzado. Espera un minuto.');
}
function signed(c:any){if(!c.get('userId'))return c.json({error:'Inicia sesión para registrar un reporte.'},401);return null;}
function admin(c:any){return signed(c)||(!c.get('admin')?c.json({error:'Esta operación requiere administrador.'},403):null);}
app.get('/api/health',async c=>{await pool.query('SELECT 1');return c.json({ok:true,version:'2.0.0-dev.1',database:'Neon Postgres',synthetic:true});});
app.get('/api/config',c=>c.json({name:'GTVS',version:'2.0.0-dev.1',bounds:VIEW_BOUNDS,scope:'Ámbito geográfico de demostración; límite municipal oficial pendiente de integrar.',model:MODEL,types,synthetic:true,authUrl:process.env.NEON_AUTH_BASE_URL,notice:'Mapa real, incidentes ficticios. No usar para evaluar la seguridad real de un lugar.'}));
app.get('/api/me',c=>signed(c)||c.json({userId:c.get('userId'),admin:c.get('admin')}));
app.get('/api/search',async c=>{await limit(c,'search',12);return c.json({places:await searchPlaces(text(c.req.query('q'),3,140)),provider:'Photon / OpenStreetMap'});});
app.get('/api/incidents',async c=>{const {rows}=await pool.query('SELECT id,type,title,latitude,longitude,occurred_at,severity,confidence,source_url,synthetic FROM gtvs.incidents WHERE active AND synthetic ORDER BY occurred_at DESC LIMIT 300');return c.json({incidents:rows,synthetic:true});});
app.get('/api/restrictions',async c=>{const {rows}=await pool.query('SELECT id,latitude,longitude,radius_m,reason,review_at,active FROM gtvs.restrictions WHERE active AND synthetic ORDER BY created_at DESC LIMIT 100');return c.json({restrictions:rows,synthetic:true});});
app.post('/api/routes',async c=>{
 await limit(c,'route',15);const body=await c.req.json();const origin=point(body.origin),destination=point(body.destination);if(meters(origin,destination)<30)throw new InputError('Selecciona dos puntos separados.');
 const candidates=await routing(origin,destination);
 const client=await pool.connect();let incidents:Incident[],flags:Flag[];
 try{await client.query('BEGIN ISOLATION LEVEL REPEATABLE READ READ ONLY');incidents=(await client.query('SELECT id,type,title,latitude,longitude,severity,confidence,occurred_at FROM gtvs.incidents WHERE active AND synthetic ORDER BY occurred_at DESC LIMIT 1000')).rows;flags=(await client.query('SELECT id,latitude,longitude,radius_m,reason,active,review_at FROM gtvs.restrictions WHERE active AND synthetic LIMIT 100')).rows;await client.query('COMMIT');}catch(e){await client.query('ROLLBACK');throw e;}finally{client.release();}
 const at=Date.now();return c.json({routes:rank(candidates,incidents,flags,at),candidates:candidates.length,calculatedAt:new Date(at).toISOString(),synthetic:true,model:MODEL,limitations:['Se reordenan candidatos de OSRM; no es optimización global sobre todos los caminos.','Un costo nulo no demuestra riesgo real bajo. La cobertura es desconocida.','Las restricciones administrativas penalizan candidatos, pero no garantizan que el proveedor genere un desvío.']});
});
app.post('/api/reports',async c=>{
 const denied=signed(c);if(denied)return denied;await limit(c,'report',8);const fields=incidentFields(await c.req.json());const key=text(c.req.header('Idempotency-Key'),8,100);
 const args=[randomUUID(),c.get('userId'),fields.type,fields.description,fields.coordinate[1],fields.coordinate[0],fields.occurredAt,fields.severity,fields.source,key];
 const {rows}=await pool.query('INSERT INTO gtvs.reports(id,user_id,type,description,latitude,longitude,occurred_at,severity,source_url,idempotency_key) VALUES($1,$2,$3,$4,$5,$6,$7,$8,$9,$10) ON CONFLICT(user_id,idempotency_key) DO NOTHING RETURNING id,state',args);
 if(rows.length)return c.json({report:rows[0],synthetic:true},201);
 const prior=(await pool.query('SELECT id,state,type,description,latitude,longitude,occurred_at,severity,source_url FROM gtvs.reports WHERE user_id=$1 AND idempotency_key=$2',[c.get('userId'),key])).rows[0];
 const same=prior.type===fields.type&&prior.description===fields.description&&Number(prior.latitude)===fields.coordinate[1]&&Number(prior.longitude)===fields.coordinate[0]&&prior.severity===fields.severity&&(prior.occurred_at?new Date(prior.occurred_at).toISOString():null)===fields.occurredAt&&(prior.source_url||null)===fields.source;
 return same?c.json({report:{id:prior.id,state:prior.state},replay:true}):c.json({error:'La clave corresponde a otro contenido.'},409);
});
app.get('/api/admin/reports',async c=>{const denied=admin(c);if(denied)return denied;const {rows}=await pool.query('SELECT id,type,description,latitude,longitude,occurred_at,severity,source_url,state,revision,incident_id,created_at FROM gtvs.reports ORDER BY created_at DESC LIMIT 100');return c.json({reports:rows});});
app.post('/api/admin/reports/:id/review',async c=>{
 const denied=admin(c);if(denied)return denied;const b=await c.req.json();if(!['approve','reject','pending'].includes(b.action)||!Number.isInteger(b.revision))throw new InputError('Decisión o revisión inválida.');const reason=text(b.reason,10,1000);
 const client=await pool.connect();try{
  await client.query('BEGIN');const r=(await client.query('SELECT * FROM gtvs.reports WHERE id=$1 FOR UPDATE',[c.req.param('id')])).rows[0];
  if(!r){await client.query('ROLLBACK');return c.json({error:'Reporte no encontrado.'},404);}
  if(r.revision!==b.revision||r.state!=='pending'){await client.query('ROLLBACK');return c.json({error:'El reporte cambió o ya fue resuelto.'},409);}
  let destination:string|null=null;
  if(b.action==='approve'){
   if(b.incidentId){const prior=(await client.query('SELECT id,type,latitude,longitude FROM gtvs.incidents WHERE id=$1 AND active',[b.incidentId])).rows[0];if(!prior||prior.type!==r.type||meters([Number(prior.longitude),Number(prior.latitude)],[r.longitude,r.latitude])>500)throw new InputError('El incidente de destino no es compatible.');destination=prior.id;}
   else{const edited=b.edits?incidentFields({...b.edits,synthetic:true}):{type:r.type,description:r.description,coordinate:[r.longitude,r.latitude],occurredAt:r.occurred_at,severity:r.severity,source:r.source_url};if(!edited.occurredAt)throw new InputError('No se crea un hecho temporal sin fecha de ocurrencia.');destination=randomUUID();await client.query('INSERT INTO gtvs.incidents(id,type,title,latitude,longitude,occurred_at,severity,source_url) VALUES($1,$2,$3,$4,$5,$6,$7,$8)',[destination,edited.type,'SIMULACIÓN · '+edited.description.slice(0,180),edited.coordinate[1],edited.coordinate[0],edited.occurredAt,edited.severity,edited.source]);}
  }
  const state={approve:'approved',reject:'rejected',pending:'pending'}[b.action as 'approve'|'reject'|'pending'];
  await client.query('UPDATE gtvs.reports SET state=$1,incident_id=$2,revision=revision+1 WHERE id=$3',[state,destination,r.id]);
  await client.query('INSERT INTO gtvs.reviews(id,report_id,reviewer_id,action,reason,incident_id) VALUES($1,$2,$3,$4,$5,$6)',[randomUUID(),r.id,c.get('userId'),b.action,reason,destination]);
  await client.query('COMMIT');return c.json({state,incidentId:destination});
 }catch(e){await client.query('ROLLBACK');throw e;}finally{client.release();}
});
app.post('/api/admin/restrictions',async c=>{
 const denied=admin(c);if(denied)return denied;const b=await c.req.json(),p=point(b.coordinate),reason=text(b.reason,15,1000),reviewAt=occurredFuture(b.reviewAt);
 if(b.synthetic!==true||!Number.isInteger(b.radiusMeters)||b.radiusMeters<30||b.radiusMeters>2000)throw new InputError('Define un radio de prueba entre 30 y 2000 metros.');
 const id=randomUUID();await pool.query('INSERT INTO gtvs.restrictions(id,latitude,longitude,radius_m,reason,review_at,created_by) VALUES($1,$2,$3,$4,$5,$6,$7)',[id,p[1],p[0],b.radiusMeters,reason,reviewAt,c.get('userId')]);return c.json({id,synthetic:true},201);
});
function occurredFuture(v:unknown){if(typeof v!=='string'||!Number.isFinite(Date.parse(v))||Date.parse(v)<=Date.now())throw new InputError('Indica una fecha futura de revisión.');return new Date(v).toISOString();}
app.patch('/api/admin/restrictions/:id',async c=>{const denied=admin(c);if(denied)return denied;const b=await c.req.json();if(b.active!==false)throw new InputError('Esta operación solo retira una restricción.');const {rowCount}=await pool.query('UPDATE gtvs.restrictions SET active=false WHERE id=$1',[c.req.param('id')]);return rowCount?c.json({retired:true}):c.json({error:'Restricción no encontrada.'},404);});
app.notFound(c=>c.json({error:'Recurso no encontrado.'},404));
export default app;
