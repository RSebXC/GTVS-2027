import {config} from 'dotenv';config({path:'.env.local',quiet:true});
import {createHash} from 'node:crypto';import pg from 'pg';
const email=process.env.GTVS_ADMIN_EMAIL;if(!email)throw new Error('Configura GTVS_ADMIN_EMAIL en .env.local.');
const db=new pg.Client({connectionString:process.env.DATABASE_URL_UNPOOLED});
try{await db.connect();const {rows}=await db.query('SELECT email_hash FROM gtvs.admin_invitations');const hash=createHash('sha256').update(email.trim().toLowerCase()).digest('hex');if(rows.some(r=>r.email_hash!==hash))throw new Error('Ya existe otra invitación; revisar antes de cambiarla.');await db.query('INSERT INTO gtvs.admin_invitations(email_hash) VALUES($1) ON CONFLICT DO NOTHING',[hash]);console.log('Invitación del administrador preparada. Requiere cuenta con correo verificado. No se imprimió el correo.');}finally{await db.end();}
