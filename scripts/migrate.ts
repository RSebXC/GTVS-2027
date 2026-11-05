import {config} from 'dotenv';
config({path:'.env.local',quiet:true});
import pg from 'pg';
import {readFile} from 'node:fs/promises';
const db=new pg.Client({connectionString:process.env.DATABASE_URL_UNPOOLED});
try{await db.connect();const before=await db.query("select to_regnamespace('gtvs') as schema");console.log('Esquema previo:',before.rows[0].schema?'existente':'nuevo');await db.query('BEGIN');for(const file of ['db/001_gtvs.sql','db/002_admin_invitation.sql','db/003_demo_batch.sql'])await db.query(await readFile(file,'utf8'));await db.query('COMMIT');console.log('Migraciones 001–003 aplicadas; tablas existentes ajenas preservadas.');}catch(e){await db.query('ROLLBACK').catch(()=>{});throw e;}finally{await db.end();}
