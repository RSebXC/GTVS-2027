/** Neon 0.8.2 entrega el JWT en session.token; /token puede devolver la sesión en caché. */
export function sessionJWT(data:{session?:{token?:string}|null}|null|undefined):string{
 const value=data?.session?.token;
 if(typeof value!=='string'||value.split('.').length!==3)throw new Error('No se pudo recuperar la sesión. Cierra sesión e ingresa nuevamente.');
 return value;
}
