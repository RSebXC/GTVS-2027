export type Coordinate = [number,number]; // longitud, latitud (GeoJSON)
export interface Incident {id:string; latitude:number; longitude:number; severity:number; confidence:number; occurred_at:string; type:string; title?:string}
export interface Flag {id:string;latitude:number;longitude:number;radius_m:number;reason:string;active:boolean;review_at:string}
export interface Candidate {geometry:{type:'LineString';coordinates:Coordinate[]};distance:number;duration:number;provider:string}
export const MODEL={version:'gtvs-demo-2.0',halfLifeDays:7,radiusMeters:350,notice:'Parámetros ilustrativos; no calibrados para Guatemala.'};
// Bbox de interfaz, no equivale al límite administrativo oficial del municipio.
export const VIEW_BOUNDS={west:-90.69,south:14.45,east:-90.35,north:14.8};
export function coordinate(value:unknown):Coordinate {
 if(!Array.isArray(value)||value.length!==2||!value.every(x=>typeof x==='number'&&Number.isFinite(x)))throw new Error('Coordenadas inválidas.');
 const [lon,lat]=value;
 if(lon<VIEW_BOUNDS.west||lon>VIEW_BOUNDS.east||lat<VIEW_BOUNDS.south||lat>VIEW_BOUNDS.north)throw new Error('El punto está fuera del ámbito de demostración.');
 return [lon,lat];
}
export function meters(a:Coordinate,b:Coordinate){const r=Math.PI/180,lat=(a[1]+b[1])/2*r;return Math.hypot((a[0]-b[0])*r*Math.cos(lat),(a[1]-b[1])*r)*6371000;}
function distanceToSegment(p:Coordinate,a:Coordinate,b:Coordinate){
 const scale=Math.cos(p[1]*Math.PI/180),xy=(c:Coordinate)=>[(c[0]-p[0])*scale*111195,(c[1]-p[1])*111195];
 const u=xy(a),v=xy(b),dx=v[0]-u[0],dy=v[1]-u[1],den=dx*dx+dy*dy;
 const t=den===0?0:Math.max(0,Math.min(1,-(u[0]*dx+u[1]*dy)/den));return Math.hypot(u[0]+t*dx,u[1]+t*dy);
}
export function pathDistance(p:Coordinate,line:Coordinate[]){if(line.length<2)return Infinity;let d=Infinity;for(let i=1;i<line.length;i++)d=Math.min(d,distanceToSegment(p,line[i-1],line[i]));return d;}
export function assess(candidate:Candidate,incidents:Incident[],flags:Flag[],at=Date.now()){
 const unique=[...new Map(incidents.map(i=>[i.id,i])).values()];
 const contributions=unique.flatMap(i=>{
  const age=(at-Date.parse(i.occurred_at))/86400000;if(!Number.isFinite(age)||age<0)return [];
  const distance=pathDistance([Number(i.longitude),Number(i.latitude)],candidate.geometry.coordinates);
  if(distance>MODEL.radiusMeters)return [];
  const temporal=Math.exp(-Math.log(2)*age/MODEL.halfLifeDays),proximity=1-distance/MODEL.radiusMeters;
  return [{incidentId:i.id,type:i.type,ageDays:age,distanceMeters:distance,contribution:Number(i.severity)*Number(i.confidence)*temporal*proximity}];
 });
 const restrictions=flags.filter(f=>f.active&&pathDistance([Number(f.longitude),Number(f.latitude)],candidate.geometry.coordinates)<=Number(f.radius_m));
 return {value:contributions.reduce((s,c)=>s+c.contribution,0),uniqueIncidents:contributions.length,contributions,restrictions:restrictions.map(f=>({id:f.id,reason:f.reason,reviewAt:f.review_at,decay:false})),coverage:'desconocida',synthetic:true};
}
export function rank(candidates:Candidate[],incidents:Incident[],flags:Flag[],at=Date.now()){
 if(!candidates.length)throw new Error('El proveedor no devolvió alternativas.');
 const assessed=candidates.map((c,i)=>({...c,id:`ruta-${i+1}`,risk:assess(c,incidents,flags,at),profiles:[] as string[]}));
 const fastest=Math.min(...assessed.map(c=>c.duration));
 const scores={rapida:(c:typeof assessed[number])=>c.duration, equilibrada:(c:typeof assessed[number])=>c.duration/fastest+0.35*c.risk.value+10*c.risk.restrictions.length,menor_riesgo_estimado:(c:typeof assessed[number])=>c.risk.value+0.05*c.duration/fastest+100*c.risk.restrictions.length};
 for(const [profile,score] of Object.entries(scores)){const winner=[...assessed].sort((a,b)=>score(a)-score(b))[0];winner.profiles.push(profile);}
 return assessed.filter(c=>c.profiles.length).map(c=>({...c,risk:{...c.risk,value:Number(c.risk.value.toFixed(4))}}));
}
