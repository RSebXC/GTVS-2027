import {execFileSync} from 'node:child_process';
import {readFile} from 'node:fs/promises';
import {parse} from 'dotenv';
const files=execFileSync('git',['diff','--cached','--name-only','--diff-filter=ACMR','-z'],{encoding:'utf8'}).split('\0').filter(Boolean);
let env={};try{env=parse(await readFile('.env.local'));}catch{}
const privateValues=Object.entries(env).filter(([k,v])=>/DATABASE_URL|PASSWORD|SECRET|GTVS_ADMIN_EMAIL/.test(k)&&v.length>8).map(([,v])=>v);
for(const file of files){
 if(/(^|\/)\.env(\.|$)/.test(file)&&file!=='.env.example')throw new Error('Archivo de entorno privado en staging');
 const data=await readFile(file);if(data.includes(0))continue;
 const text=data.toString('utf8');
 if(privateValues.some(value=>text.includes(value))||/postgres(?:ql)?:\/\/[^\s:@]+:[^\s@]+@/i.test(text))throw new Error('Posible secreto en '+file);
}
console.log('Revisión de publicación: '+files.length+' archivos; sin valores privados del entorno detectados.');
