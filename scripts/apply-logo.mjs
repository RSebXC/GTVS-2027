import {readFile,writeFile,copyFile,mkdir,access} from 'node:fs/promises';
const original=process.argv.includes('--original'),image=original?'logo-gtvs.jpg':'logo-emblema.png',resource=original?'logo_gtvs':'logo_emblema';
await access('web/public/'+image);
const page='web/index.html';let html=await readFile(page,'utf8');html=html.replaceAll('logo-gtvs.jpg',image).replaceAll('logo-emblema.png',image).replace(/type="image\/(?:png|jpeg)"/,`type="image/${original?'jpeg':'png'}"`);await writeFile(page,html);
await mkdir('android/app/src/main/res/drawable-nodpi',{recursive:true});await copyFile('web/public/'+image,'android/app/src/main/res/drawable-nodpi/'+resource+(original?'.jpg':'.png'));
const manifest='android/app/src/main/AndroidManifest.xml';await writeFile(manifest,(await readFile(manifest,'utf8')).replace(/@drawable\/(?:logo_gtvs|logo_emblema)/g,'@drawable/'+resource));
const kotlin='android/app/src/main/java/gt/edu/usac/gtvs/MainActivity.kt';await writeFile(kotlin,(await readFile(kotlin,'utf8')).replace(/R\.drawable\.(?:logo_gtvs|logo_emblema)/g,'R.drawable.'+resource));
console.log(original?'Logotipo original seleccionado.':'Emblema sin marco seleccionado.');
