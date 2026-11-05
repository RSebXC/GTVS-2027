import {readFile} from 'node:fs/promises';
import {createHash} from 'node:crypto';
const expected=await readFile('dist/index.html','utf8');
const asset=expected.match(/src="([^"]+\/assets\/index-[^"]+\.js)"/)?.[1];
if(!asset)throw new Error('No se encontró el recurso de producción.');
const local=await readFile('dist/assets/'+asset.split('/').pop());
const hash=data=>createHash('sha256').update(data).digest('hex');
let success=false;
for(let i=0;i<15;i++){
 const page=await fetch('https://rsebxc.github.io/GTVS-2027/?verify=demo2-'+Date.now());const html=await page.text();
 if(page.ok&&html.includes(asset)&&html.includes('theme-toggle')){const response=await fetch('https://rsebxc.github.io'+asset);if(response.ok&&hash(Buffer.from(await response.arrayBuffer()))===hash(local)){success=true;break;}}
 await new Promise(resolve=>setTimeout(resolve,2000));
}
if(!success)throw new Error('La publicación todavía no coincide con el build local.');
console.log('Publicación comprobada: HTTP 200; interfaz demo.2 y JavaScript idéntico al build local.');
