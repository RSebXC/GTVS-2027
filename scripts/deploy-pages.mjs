import {execFileSync} from 'node:child_process';
import {mkdir,cp,writeFile,readFile} from 'node:fs/promises';
import {resolve} from 'node:path';
const run=(args,cwd=process.cwd(),input)=>execFileSync('git',args,{cwd,input,encoding:'utf8',stdio:['pipe','pipe','pipe']}).trim();
// Credentials stay in memory; never appear in arguments, repository files or output.
const credentials=run(['credential','fill'],process.cwd(),'protocol=https\nhost=github.com\n\n');
const token=credentials.split('\n').find(l=>l.startsWith('password='))?.slice(9);
if(!token)throw new Error('No hay credencial GitHub disponible.');
const headers={Authorization:'Bearer '+token,Accept:'application/vnd.github+json','X-GitHub-Api-Version':'2022-11-28'};
const url='https://api.github.com/repos/RSebXC/GTVS-2027/pages';
const staging=resolve('.pages-deploy');await mkdir(staging,{recursive:true});
run(['init','-b','gh-pages'],staging);run(['config','user.name','RSebXC'],staging);run(['config','user.email','RSebXC@users.noreply.github.com'],staging);
try{run(['remote','add','origin','https://github.com/RSebXC/GTVS-2027.git'],staging);}catch{}
try{run(['fetch','origin','gh-pages'],staging);run(['checkout','-B','gh-pages','origin/gh-pages'],staging);}catch{}
await cp('dist',staging,{recursive:true});await writeFile(resolve(staging,'.nojekyll'),'');
run(['add','.'],staging);try{run(['commit','-m','deploy: actualizar demostración web GTVS V2'],staging);}catch{}
console.log(run(['push','-u','origin','gh-pages'],staging));
const existing=await fetch(url,{headers});
const response=await fetch(url,{method:existing.status===404?'POST':'PUT',headers:{...headers,'Content-Type':'application/json'},body:JSON.stringify({source:{branch:'gh-pages',path:'/'}})});
if(!response.ok)throw new Error('Pages configuración HTTP '+response.status+': '+(await response.json()).message+' (la rama ya está publicada)');
console.log('GitHub Pages configurado: https://rsebxc.github.io/GTVS-2027/');
