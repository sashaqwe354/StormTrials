const { spawn } = require('node:child_process');
const { once } = require('node:events');
const path = require('node:path');
const assert = require('node:assert/strict');
const mineflayer = require('mineflayer');

const jvm = spawn('C:/Program Files/Java/jdk-27/bin/java.exe', [
  '-Dterminal.jline=false', '-Dterminal.ansi=false', '-Xms1G', '-Xmx2G', '-jar', 'paper.jar', '--nogui'
], { cwd: path.join(__dirname, '..', process.env.STORM_PAPER_TEST || 'test-server'), stdio: ['pipe', 'pipe', 'inherit'] });
const exit = once(jvm, 'exit');
const log = [];
let pending = '';
let bot;
let teammate;
jvm.stdout.on('data', chunk => {
  pending += chunk;
  const lines = pending.split(/\r?\n/);
  pending = lines.pop();
  for (const line of lines) {
    log.push(line);
    if (/ERROR|Exception|StormTrials|Done \(/.test(line)) console.log(line);
  }
});

const pause = ms => new Promise(resolve => setTimeout(resolve, ms));
async function until(condition, label, timeout = 10000) {
  const deadline = Date.now() + timeout;
  while (Date.now() < deadline) {
    if (condition()) return;
    if (jvm.exitCode !== null) throw new Error('Paper exited while waiting for ' + label);
    await pause(100);
  }
  throw new Error('Timed out waiting for ' + label);
}
const command = text => jvm.stdin.write(text + '\n');
const seen = expression => log.some(line => expression.test(line));

async function useSlot(slot) {
  bot.setQuickBarSlot(slot);
  await pause(275);
  bot.activateItem();
  await pause(175);
  bot.deactivateItem();
}

async function run() {
  await until(() => seen(/Done \([\d.,]+s\)! For help/), 'Paper boot', 100000);
  assert(seen(/15 rotating maps, 20 recipes/));
  const username = process.env.STORM_PLAYER || 'SupplyT' + Date.now().toString(36).slice(-6);
  if (process.env.STORM_RECHECK) {
    bot = mineflayer.createBot({ host: '127.0.0.1', port: 25574, username, version: '1.21.11' });
    let arrived = false;
    bot.on('messagestr', message => { if (message.includes('Раунды начинаются автоматически.')) arrived = true; });
    await until(() => arrived && bot.entity && bot.inventory.slots[39]?.name === 'echo_shard', 'post-restart lobby');
    assert(Math.abs(bot.entity.position.x - 1.5) < 0.25, 'The custom spawn was not restored');
    bot.chat('/storm accessories');
    await until(() => bot.currentWindow?.slots[16]?.name === 'lodestone', 'post-restart accessory menu');
    const lore = JSON.stringify(bot.currentWindow.slots[16].customLore);
    assert(lore.includes('Выбран на следующий раунд'), 'Accessory loadout not restored: ' + lore);
    console.log('Verified configured lobby spawn and equipped accessory after a server restart.');
    return;
  }
  const maps = ['HARBOR', 'OBSERVATORY', 'FORT', 'STATION', 'GREENHOUSE', 'QUARRY', 'VILLAGE',
    'REACTOR', 'MARKET', 'LIBRARY', 'OILRIG', 'RESERVOIR', 'AIRFIELD', 'MONASTERY', 'PYRAMID'];
  for (const map of maps) {
    command('storm map ' + map);
    await until(() => seen(new RegExp('Arena loaded: ' + map + '$')), 'arena ' + map);
  }
  console.log('All 15 arenas built in-place and rotated successfully.');

  const messages = [];
  bot = mineflayer.createBot({ host: '127.0.0.1', port: 25574, username, version: '1.21.11' });
  bot.on('messagestr', message => {
    messages.push(message);
    if (/\[StormTrials\]|\[Снабжение\]/.test(message)) console.log(message);
  });
  bot.on('error', error => console.error(error));
  bot.on('kicked', reason => console.error(reason));
  const heard = phrase => messages.some(message => message.includes(phrase));

  await until(() => bot.inventory?.slots[39]?.name === 'echo_shard', 'lobby accessories button', 25000);
  command('op ' + username);
  teammate = mineflayer.createBot({ host: '127.0.0.1', port: 25574, username: 'ReleaseMate', version: '1.21.11' });
  teammate.on('error', error => console.error(error));
  await until(() => teammate.inventory?.slots[39]?.name === 'echo_shard', 'second player joins', 12000);
  command('op ReleaseMate');
  command('storm start flood');
  await until(() => bot.inventory.slots[36]?.name === 'crafting_table', 'round bench');
  await until(() => teammate.inventory.slots[36]?.name === 'crafting_table', 'round includes both players');
  command('execute in minecraft:storm_islands run minecraft:tp ReleaseMate 1.5 102 -2.5');
  await useSlot(0);
  await until(() => bot.currentWindow?.slots[19]?.name === 'bundle', 'twenty recipes published to the menu');
  assert(bot.currentWindow.slots.slice(0, 20).every(Boolean));
  bot.closeWindow(bot.currentWindow);

  await until(() => heard('Аирдроп снижается'), 'airdrops announced', 18000);
  const drop = () => Object.values(bot.entities).find(entity => entity.name === 'interaction'
    && Math.abs(entity.position.x + 21.5) < 1);
  await until(() => drop(), 'interactive airdrop spawned');
  await pause(3200);
  const target = drop();
  command('execute in minecraft:storm_islands run minecraft:tp ' + username + ' ' +
    (target.position.x + 1) + ' ' + (target.position.y + 0.25) + ' ' + target.position.z);
  await pause(600);
  await bot.activateEntityAt(target, target.position.offset(0, 0.6, 0));
  await until(() => heard('Аирдроп собран'), 'loot delivery');
  command('minecraft:damage ' + username + ' 6 minecraft:generic');
  await until(() => bot.health <= 14, 'test damage');

  await useSlot(0);
  await until(() => bot.currentWindow?.slots[0]?.name === 'paper', 'reopen bench');
  bot.clickWindow(0, 0, 0).catch(() => {});
  await until(() => heard('Изготовлено: Походный бинт'), 'craft deducts airdrop resources');
  bot.clickWindow(0, 1, 0).catch(() => {});
  await until(() => heard('Применено: Походный бинт') && bot.health === 20, 'crafted item heals');
  bot.clickWindow(16, 0, 0).catch(() => {});
  await until(() => heard('Аксессуар открыт: Амулет устойчивости'), 'craft a permanent accessory');
  bot.closeWindow(bot.currentWindow);
  bot.chat('/storm lobby');
  await until(() => bot.inventory.slots[39]?.name === 'echo_shard', 'return to lobby');
  await useSlot(3);
  await until(() => bot.currentWindow?.slots[16]?.name === 'lodestone', 'lobby accessory menu');
  bot.clickWindow(16, 0, 0).catch(() => {});
  await until(() => heard('На следующий раунд выбран: Амулет устойчивости'), 'equip accessory');
  bot.closeWindow(bot.currentWindow);
  bot.chat('/storm build');
  await until(() => bot.game.gameMode === 'creative', 'builder mode');
  command('execute in minecraft:storm_islands run minecraft:tp ' + username + ' 1.5 150 -57.5');
  await until(() => Math.abs(bot.entity.position.x - 1.5) < 0.25, 'new lobby position');
  bot.chat('/storm setlobby');
  await until(() => heard('Новая точка лобби сохранена'), 'save custom lobby spawn');
  bot.chat('/storm build');
  await until(() => bot.game.gameMode === 'adventure', 'return to adventure');
  await until(() => seen(/Round ended: survivors=1/), 'teammate finishes the flood round', 61000);
  const drops = log.filter(line => line.includes('Airdrop launched at round second ')).map(line => Number(line.split(' ').at(-1)));
  assert.deepEqual(drops, [12, 42]);
  assert(!seen(/Server thread\/ERROR.*(Exception|StormTrials)/));
  console.log('Verified two players, exactly two scheduled drops, 20 recipes, the permanent accessory, and lobby editing.');
}

(async () => {
  try {
    await run();
  } catch (error) {
    console.error(error);
    process.exitCode = 1;
  } finally {
    if (bot) bot.quit();
    if (teammate) teammate.quit();
    if (jvm.exitCode === null) command('stop');
    const watchdog = setTimeout(() => jvm.kill(), 30000);
    const [code] = await exit;
    clearTimeout(watchdog);
    if (code !== 0) process.exitCode = 1;
  }
})();
