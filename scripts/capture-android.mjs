import {execFileSync} from 'node:child_process';import {writeFile,mkdir} from 'node:fs/promises';import {join} from 'node:path';
const adb=join(process.env.ANDROID_HOME||join(process.env.LOCALAPPDATA,'Android','Sdk'),'platform-tools','adb.exe');
const devices=execFileSync(adb,['devices'],{encoding:'utf8'}).split('\n').filter(x=>/\tdevice\r?$/.test(x)).map(x=>x.split('\t')[0]);if(devices.length!==1)throw new Error('Se requiere un teléfono autorizado conectado.');
const call=args=>execFileSync(adb,['-s',devices[0],...args],{maxBuffer:20000000});
console.log(call(['install','-r','android/app/build/outputs/apk/debug/app-debug.apk']).toString().trim());
call(['shell','am','start','-n','gt.edu.usac.gtvs/.MainActivity']);await new Promise(r=>setTimeout(r,4500));
await mkdir('tmp',{recursive:true});await writeFile('tmp/android-demo4.png',call(['exec-out','screencap','-p']));console.log('Captura de GTVS guardada para revisión local.');
