import pg from 'pg';
import { attachDatabasePool } from '@neon/functions';
export const pool=new pg.Pool({connectionString:process.env.DATABASE_URL,max:3,connectionTimeoutMillis:15000,idleTimeoutMillis:10000});
attachDatabasePool(pool);
pool.on('error',()=>console.error('Conexión de base no disponible; se restablecerá en la próxima solicitud.'));
