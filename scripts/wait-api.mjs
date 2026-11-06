const base='https://br-morning-resonance-belnuka7-gtvsapi.compute.c-9.us-east-2.aws.neon.tech/api/health';let ready=false;
for(let i=0;i<25;i++){try{const r=await fetch(base,{signal:AbortSignal.timeout(4000)});if((await r.json()).version==='2.0.0-demo.4'){ready=true;break;}}catch{}await new Promise(r=>setTimeout(r,1500));}
if(!ready)throw new Error('El despliegue de API demo.4 aún no está listo.');console.log('API demo.4 confirmada.');
