import {coordinate,type Candidate,type Coordinate} from './risk.ts';
const headers={'User-Agent':'GTVS-2027 academic-demo (github.com/RSebXC/GTVS-2027)','Accept':'application/json'};
const cache=new Map<string,{at:number,value:unknown}>();
let lastSearch=0;
export async function searchPlaces(query:string){
 if(query.trim().length<3||query.length>140)throw new Error('Escribe entre 3 y 140 caracteres.');
 const key='q:'+query.trim().toLowerCase(),found=cache.get(key);if(found&&Date.now()-found.at<3600000)return found.value;
 if(Date.now()-lastSearch<1100)throw new Error('Espera un momento antes de otra búsqueda.');lastSearch=Date.now();
 const url=new URL('https://photon.komoot.io/api/');url.searchParams.set('q',query);url.searchParams.set('lat','14.63');url.searchParams.set('lon','-90.53');url.searchParams.set('limit','6');url.searchParams.set('bbox','-90.69,14.45,-90.35,14.8');
 const response=await fetch(url,{headers,signal:AbortSignal.timeout(15000)});if(!response.ok)throw new Error('La búsqueda externa no está disponible. Selecciona un punto en el mapa.');
 const json=await response.json() as any;
 const places=(json.features||[]).flatMap((f:any)=>{try{return [{name:[f.properties.name,f.properties.street,f.properties.housenumber,f.properties.district,f.properties.city].filter(Boolean).join(', '),coordinate:coordinate(f.geometry.coordinates)}];}catch{return [];}});
 if(cache.size>128)cache.clear();cache.set(key,{at:Date.now(),value:places});return places;
}
export const routing=(origin:Coordinate,destination:Coordinate)=>queryRouting([origin,destination]);
export const routingVia=(origin:Coordinate,via:Coordinate[],destination:Coordinate)=>queryRouting([origin,...via,destination]);
async function queryRouting(points:Coordinate[]):Promise<Candidate[]>{
 const key='r:'+JSON.stringify(points),found=cache.get(key);if(found&&Date.now()-found.at<300000)return found.value as Candidate[];
 const base=process.env.OSRM_BASE_URL||'https://routing.openstreetmap.de/routed-car';
 const url=`${base}/route/v1/driving/${points.map(p=>p.join(',')).join(';')}?alternatives=${points.length===2?'3':'false'}&steps=false&overview=full&geometries=geojson`;
 const response=await fetch(url,{headers,signal:AbortSignal.timeout(points.length>2?4500:10000)});
 if(!response.ok)throw new Error('El servicio gratuito de rutas no está disponible. Intenta más tarde.');
 const json=await response.json() as any;
 if(json.code!=='Ok'||!Array.isArray(json.routes))throw new Error('No se encontró una ruta vehicular entre los puntos.');
 const routes=json.routes.slice(0,5).filter((r:any)=>r.distance>0&&r.duration>0&&r.geometry?.type==='LineString').map((r:any)=>({geometry:r.geometry,distance:r.distance,duration:r.duration,provider:'OSRM / FOSSGIS · OpenStreetMap',...(points.length>2?{via:points.slice(1,-1)}:{})}));
 const distinct=[...new Map<string,Candidate>(routes.map((r:Candidate)=>[JSON.stringify(r.geometry.coordinates),r])).values()];
 if(cache.size>128)cache.clear();cache.set(key,{at:Date.now(),value:distinct});return distinct;
}
