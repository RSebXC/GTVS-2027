import {test} from 'node:test';
import assert from 'node:assert/strict';
import {sessionJWT} from '../web/session-token.ts';
test('JWT se obtiene del formato de sesión que devuelve el SDK, no de data.token',()=>assert.equal(sessionJWT({session:{token:'header.payload.signature'}}),'header.payload.signature'));
test('una sesión ausente o un identificador opaco no se envía como Bearer',()=>{for(const data of [null,{}, {session:{token:'opaque-session'}}])assert.throws(()=>sessionJWT(data));});
