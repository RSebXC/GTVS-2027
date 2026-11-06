import {test} from 'node:test';import assert from 'node:assert/strict';
import {expandCandidates,bypassPoints} from '../src/avoidance';
import {pathDistance,type Candidate,type Flag} from '../src/risk';
const flag:Flag={id:'demo',latitude:14.63,longitude:-90.53,radius_m:230,reason:'SIMULACIÓN',active:true,review_at:'2026-12-01'};
const origin:[number,number]=[-90.54,14.63],destination:[number,number]=[-90.52,14.63];
const direct:Candidate={geometry:{type:'LineString',coordinates:[origin,destination]},distance:2200,duration:300,provider:'fixture'};
test('se conservan referencia y desvío geométrico, sin dibujar una línea ficticia como ruta',async()=>{const result=await expandCandidates(origin,destination,[direct],[flag],async(o,p,d)=>[{...direct,geometry:{type:'LineString',coordinates:[o,...p,d]},via:p}]);assert.equal(result.attempted,2);assert.equal(result.routes.length,3);assert.ok(pathDistance([flag.longitude,flag.latitude],result.routes[1].geometry.coordinates)>flag.radius_m);});
test('no se promete evitación cuando fallan los proveedores',async()=>{const result=await expandCandidates(origin,destination,[direct],[flag],async()=>{throw new Error('offline')});assert.equal(result.routes.length,1);assert.equal(result.failed,4);});
test('no se consulta al proveedor si no hay sectores cruzados',async()=>{const result=await expandCandidates(origin,destination,[direct],[],async()=>{throw new Error('no debe llamarse')});assert.equal(result.attempted,0);assert.equal(bypassPoints(origin,origin,flag).length,0);});
