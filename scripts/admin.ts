import {config} from 'dotenv';config({path:'.env.local',quiet:true});
import pg from 'pg';
const email=process.argv[2];if(!email)throw new Error('Uso: npm run admin -- correo-de-la-cuenta-ya-creada');
const db=new pg.Client({connectionString:process.env.DATABASE_URL_UNPOOLED});
try{await db.connect();const {rows}=await db.query('SELECT id FROM neon_auth."user" WHERE lower(email)=lower($1)',[email]);if(rows.length!==1)throw new Error('La cuenta debe existir primero en GTVS.');const existing=await db.query('SELECT user_id FROM gtvs.admins');if(existing.rows.length&&!existing.rows.some(r=>r.user_id===rows[0].id))throw new Error('Ya existe el administrador inicial; no se agrega otro sin revisar el alcance.');await db.query('INSERT INTO gtvs.admins(user_id) VALUES($1) ON CONFLICT DO NOTHING',[rows[0].id]);console.log('Administrador inicial configurado; no se publicaron credenciales.');}finally{await db.end();}
