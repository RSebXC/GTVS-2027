import {meters,pathDistance,type Candidate,type Coordinate,type Flag} from './risk';
/** Muestreo acotado de puntos intermedios: no es búsqueda global ni exclusión de polígonos OSRM. */
export function bypassPoints(origin:Coordinate,destination:Coordinate,flag:Flag):Coordinate[]{
 const lat=flag.latitude,scale=Math.cos(lat*Math.PI/180),dx=(destination[0]-origin[0])*scale,dy=destination[1]-origin[1],length=Math.hypot(dx,dy);
 if(!length)return [];const normal=[-dy/length,dx/length];
 return [2.2,3.6].flatMap(factor=>[-1,1].map(side=>{const distance=Number(flag.radius_m)*factor+250;return [Number(flag.longitude)+normal[0]*side*distance/(111195*scale),lat+normal[1]*side*distance/111195] as Coordinate;}));
}
export function bypassPlans(origin:Coordinate,destination:Coordinate,flag:Flag):Coordinate[][]{
 const scale=Math.cos(flag.latitude*Math.PI/180),dx=(destination[0]-origin[0])*scale,dy=destination[1]-origin[1],length=Math.hypot(dx,dy);if(!length)return [];
 const ux=dx/length,uy=dy/length,along=Number(flag.radius_m)*2+250;
 return [2.2,3.6].flatMap(factor=>[-1,1].map(side=>{const offset=Number(flag.radius_m)*factor+250;return [-1,1].map(end=>[Number(flag.longitude)+(ux*along*end-uy*offset*side)/(111195*scale),Number(flag.latitude)+(uy*along*end+ux*offset*side)/111195] as Coordinate);}));
}
export function sectorPlans(origin:Coordinate,destination:Coordinate,flags:Flag[]):Coordinate[][]{
 const center:Coordinate=[flags.reduce((s,f)=>s+Number(f.longitude),0)/flags.length,flags.reduce((s,f)=>s+Number(f.latitude),0)/flags.length],scale=Math.cos(center[1]*Math.PI/180);
 const dx=(destination[0]-origin[0])*scale,dy=destination[1]-origin[1],len=Math.hypot(dx,dy);if(!len)return [];
 const ux=dx/len,uy=dy/len;let minA=Infinity,maxA=-Infinity,minN=Infinity,maxN=-Infinity;
 for(const f of flags){const x=(Number(f.longitude)-center[0])*111195*scale,y=(Number(f.latitude)-center[1])*111195,r=Number(f.radius_m),a=x*ux+y*uy,n=-x*uy+y*ux;minA=Math.min(minA,a-r);maxA=Math.max(maxA,a+r);minN=Math.min(minN,n-r);maxN=Math.max(maxN,n+r);}
 return [180,500].flatMap(pad=>[minN-pad,maxN+pad].map(n=>[minA-pad,maxA+pad].map(a=>[center[0]+(a*ux-n*uy)/(111195*scale),center[1]+(a*uy+n*ux)/111195] as Coordinate)));
}
export async function expandCandidates(origin:Coordinate,destination:Coordinate,initial:Candidate[],flags:Flag[],via:(o:Coordinate,p:Coordinate[],d:Coordinate)=>Promise<Candidate[]>){
 const crossed=flags.filter(f=>f.active&&initial.some(c=>pathDistance([Number(f.longitude),Number(f.latitude)],c.geometry.coordinates)<=Number(f.radius_m)));
 if(!crossed.length)return {routes:initial,attempted:0,failed:0};
 // El destino/origen dentro de un sector no puede considerarse evitado.
 const relevant=crossed.find(f=>meters(origin,[Number(f.longitude),Number(f.latitude)])>Number(f.radius_m)&&meters(destination,[Number(f.longitude),Number(f.latitude)])>Number(f.radius_m));
 if(!relevant)return {routes:initial,attempted:0,failed:0};
 // Agrupar círculos próximos evita desviar desde uno directamente hacia otro.
 const nearby=flags.filter(f=>f.active&&meters([Number(f.longitude),Number(f.latitude)],[Number(relevant.longitude),Number(relevant.latitude)])<3500);
 const routes=[...initial];let attempted=0,failed=0;
 for(const plan of sectorPlans(origin,destination,nearby)){
  attempted++;try{routes.push(...await via(origin,plan,destination));}catch{failed++;}
  if(attempted>=2&&routes.some(c=>flags.filter(f=>f.active).every(f=>pathDistance([Number(f.longitude),Number(f.latitude)],c.geometry.coordinates)>Number(f.radius_m))))break;
 }
 const unique=[...new Map(routes.map(c=>[JSON.stringify(c.geometry.coordinates),c])).values()];
 return {routes:unique,attempted,failed};
}
