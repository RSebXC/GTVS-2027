import { defineConfig } from 'vite';
import { config } from 'dotenv';
config({path:'.env.local',quiet:true});
export default defineConfig({
 root:'web',base:process.env.PUBLIC_BASE||'/',
 define:{'import.meta.env.VITE_NEON_AUTH_URL':JSON.stringify(process.env.NEON_AUTH_BASE_URL||''),'import.meta.env.VITE_API_URL':JSON.stringify(process.env.VITE_API_URL||'/api')},
 build:{outDir:'../dist',emptyOutDir:true},
 server:{port:5173,proxy:{'/api':'http://127.0.0.1:8787'}}
});
