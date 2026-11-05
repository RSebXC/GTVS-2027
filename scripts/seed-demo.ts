import {config} from 'dotenv';config({path:'.env.local',quiet:true});
import pg from 'pg';
import {createHash} from 'node:crypto';
import {readFile} from 'node:fs/promises';
const link=JSON.parse(await readFile('.neon','utf8'));if(link.branch!=='demo-v2')throw new Error('La carga solo se permite en demo-v2.');
const batch='presentacion-2026-11-v1';
// Coordenadas de escenarios de software, sin correspondencia con delitos reales.
const centers:[string,number,number][]=[
 ['Escenario A',-90.5218,14.6349],['Escenario B',-90.5155,14.6173],
 ['Escenario C',-90.5277,14.6111],['Escenario D',-90.5505,14.6201],
 ['Escenario E',-90.5352,14.5905],['Escenario F',-90.5034,14.6097],
 ['Escenario G',-90.4932,14.6430],['Escenario H',-90.5433,14.6478],
 ['Escenario I',-90.5612,14.6106],['Escenario J',-90.5074,14.5928],
 ['Escenario K',-90.5201,14.6578],['Escenario L',-90.5531,14.5918],
];
const kinds=['robo_ataque','obstruccion','cierre','otro'];
const labels:Record<string,string>={robo_ataque:'robo o ataque ficticio',obstruccion:'obstrucción ficticia',cierre:'cierre ficticio',otro:'contexto ficticio'};
const db=new pg.Client({connectionString:process.env.DATABASE_URL_UNPOOLED});
let inserted=0;
try{
 await db.connect();await db.query('BEGIN');
 for(let c=0;c<centers.length;c++)for(let j=0;j<3;j++){
  const [name,lon,lat]=centers[c],kind=kinds[c%4],key=`${batch}:${c}:${j}`;
  const hex=createHash('sha256').update(key).digest('hex');const id=`${hex.slice(0,8)}-${hex.slice(8,12)}-4${hex.slice(13,16)}-a${hex.slice(17,20)}-${hex.slice(20,32)}`;
  const ageHours=[2,26,240][j]+c;const occurred=new Date(Date.now()-ageHours*3600000).toISOString();
  const result=await db.query('INSERT INTO gtvs.incidents(id,type,title,latitude,longitude,occurred_at,severity,confidence,synthetic,demo_batch) VALUES($1,$2,$3,$4,$5,$6,$7,0.6,true,$8) ON CONFLICT(id) DO NOTHING RETURNING id',[id,kind,`SIMULACIÓN · ${name} / ${j+1} · ${labels[kind]}`,lat+j*.00045,lon+j*.00035,occurred,2+(c+j)%4,batch]);inserted+=result.rowCount||0;
 }
 await db.query('COMMIT');const count=await db.query('SELECT count(*)::int n FROM gtvs.incidents WHERE demo_batch=$1',[batch]);
 console.log(JSON.stringify({batch,inserted,total:count.rows[0].n,synthetic:true,notice:'Escenarios ficticios precargados, no reportes reales ni hechos corroborados.'}));
}catch(e){await db.query('ROLLBACK').catch(()=>{});throw e;}finally{await db.end();}
