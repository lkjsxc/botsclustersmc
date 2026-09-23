// A scripted human-observer protocol test, never a policy actor or training source.
import assert from 'node:assert/strict';
import { setTimeout as delay } from 'node:timers/promises';
const { default: mineflayer } = await import(new URL('../.build/observer-client/node_modules/mineflayer/index.js',import.meta.url));
const port=Number(process.env.OBSERVER_TEST_PORT||25581);
assert.ok(port>=1024&&port<=65535&&port!==25565,'Use a disposable loopback test port, never the production server.');
const messages=[],errors=[];let particles=0;
const bot=mineflayer.createBot({host:'127.0.0.1',port,username:'ObserverFixture',auth:'offline',version:'1.21.11',profilesFolder:'.build/observer-client/session'});
bot.on('messagestr',text=>messages.push(text));bot.on('error',e=>errors.push(String(e)));
bot._client.on('world_particles',()=>particles++);
async function until(predicate,label,seconds=20){
 const deadline=Date.now()+seconds*1000;
 while(Date.now()<deadline){if(predicate())return;await delay(100);}
 throw new Error(label+': '+JSON.stringify({messages:messages.slice(-12),position:bot.entity?.position,errors}));
}
async function command(text,reply){
 const start=messages.length;bot.chat('/bots '+text);
 await until(()=>messages.slice(start).some(m=>m.includes(reply)),text);
}
try {
 await until(()=>bot.entity&&bot.game.gameMode==='spectator','spectator join',60);bot.physicsEnabled=false;
 await until(()=>bot.entity.position.y>75,'automatic overview');
 await command('progress','Stages');await command('inspect 0','body=VILLAGER');
 await command('watch 0','Observing #0');await until(()=>bot.entity.position.y>68&&bot.entity.position.y<73,'tracking camera');
 assert.ok(particles>0,'Client received goal particles');
 const before=bot.entity.position.clone();
 await command('watch 127','Observing #127');await until(()=>bot.entity.position.distanceTo(before)>100,'cross-island tracking',30);
 await command('tour 127','Observing #127');
 await command('pause','Permission denied');await command('remove all','Permission denied');
 await command('overview 0','Observing #0');await until(()=>bot.entity.position.y>80,'free-flight overview');
 await command('unwatch','Tracking stopped');assert.equal(bot.game.gameMode,'spectator');
 await command('inspect 999999','Rejected');
 assert.deepEqual(errors,[]);
 console.log('PASS non-operator spectator join, live HUD/particles, cross-island follow, overview, unwatch, diagnostics and denied administration');
 console.log(JSON.stringify({client:bot.version,particles,position:bot.entity.position,messages:messages.slice(-12)}));
} finally {bot.quit('Observer test complete');await delay(500);}
