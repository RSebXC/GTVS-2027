import {config} from 'dotenv';config({path:'.env.local',quiet:true});
const {serve}=await import('@hono/node-server');
const {default:app}=await import('./server.ts');
serve({fetch:app.fetch,port:8787,hostname:'127.0.0.1'},()=>console.log('GTVS API local: http://127.0.0.1:8787 · Neon compartido · solo demostración'));
