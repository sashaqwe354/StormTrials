const { spawn } = require('node:child_process');
const { once } = require('node:events');
const path = require('node:path');
const assert = require('node:assert/strict');
const mineflayer = require('mineflayer');

const jvm = spawn('C:/Program Files/Java/jdk-27/bin/java.exe', [
  '-Dterminal.jline=false', '-Dterminal.ansi=false',
  '-Xms1G', '-Xmx2G', '-jar', 'paper.jar', '--nogui'
], { cwd: path.join(__dirname, '../test-server'), stdio: ['pipe', 'pipe', 'inherit'] });
const exit = once(jvm, 'exit');
const log = [];
let bot;
let pending = '';
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
    await pause(125);
  }
  throw new Error('Timed out waiting for ' + label);
}
const command = text => jvm.stdin.write(text + '\n');
const saw = expression => log.some(line => expression.test(line));

async function useSlot(slot) {
  bot.setQuickBarSlot(slot);
  await pause(300);
  bot.activateItem();
  await pause(200);
  bot.deactivateItem();
}

async function run() {
  await until(() => saw(/Done \([\d.,]+s\)! For help/), 'Paper startup', 100000);
  assert(saw(/StormTrials tower ready/), 'Tower and maps failed to initialize');

  const messages = [];
  const username = 'StormV2' + Date.now().toString(36).slice(-6);
  bot = mineflayer.createBot({ host: '127.0.0.1', port: 25574, username, version: '1.21.11' });
  bot.on('messagestr', message => {
    messages.push(message);
    if (message.includes('[StormTrials]')) console.log(message);
  });
  bot.on('error', error => console.error('Bot error:', error));
  bot.on('kicked', reason => console.error('Bot kicked:', JSON.stringify(reason)));

  await until(() => bot.entity?.position.y > 149 && bot.inventory.slots[36]?.name === 'clock', 'tower spawn and lobby items', 25000);
  assert.equal(bot.game.gameMode, 'adventure');
  console.log('Verified forced spawn on tower deck and lobby items.');
  await useSlot(2);
  await until(() => bot.currentWindow?.slots[11]?.name === 'golden_apple', 'opening shop using emerald');
  const purchase = bot.clickWindow(11, 0, 0);
  purchase.catch(error => console.error('Shop click:', error.message));
  await until(() => messages.some(message => message.includes('Куплено: Аптечка')), 'shop purchase');
  bot.closeWindow(bot.currentWindow);

  await useSlot(4);
  await until(() => bot.game.gameMode === 'spectator', 'spectator compass');
  bot.chat('/storm lobby');
  await until(() => bot.entity.position.y > 149 && bot.game.gameMode === 'adventure', 'spectator return');

  command('op ' + username);
  command('execute in minecraft:storm_islands run minecraft:tp ' + username + ' 41.5 76.5 0.5');
  await until(() => bot.entity.position.y < 80, 'ocean warp');
  bot.setControlState('forward', true);
  await until(() => bot.entity.position.y > 149, 'deadly ocean return');
  bot.setControlState('forward', false);
  console.log('Verified spectator entry and exit, ocean contact, and return to tower.');

  command('execute in minecraft:overworld run minecraft:tp ' + username + ' 0 80 0');
  await pause(1000);
  assert(bot.entity.position.y >= 149, 'Teleport to ordinary world escaped the lobby');

  bot.chat('/storm build');
  await until(() => bot.game.gameMode === 'creative', 'owner build mode');
  bot.chat('/storm setlobby');
  await until(() => messages.some(message => message.includes('Новая точка лобби сохранена')), 'custom lobby spawn');
  await pause(16000);
  assert(!saw(/Round started:/), 'Builder was pulled into a round');
  bot.chat('/storm build');
  await until(() => bot.game.gameMode === 'adventure', 'leaving build mode');
  assert.equal(bot.inventory.slots[43]?.name, 'spyglass');
  command('storm start flood');
  await until(() => saw(/Round started:/), 'first map and round');
  await until(() => bot.inventory.slots[40]?.name === 'golden_apple', 'purchased medkit issued');
  await useSlot(4);
  await until(() => messages.some(message => message.includes('Аптечка использована')), 'medkit action');
  command('execute in minecraft:storm_islands run minecraft:tp ' + username + ' 0.5 102 -2.5');
  await until(() => bot.entity.position.y >= 101, 'climb to the pyramid peak');
  await pause(37000);
  command('execute in minecraft:storm_islands if block 32 81 0 minecraft:water run minecraft:list');
  await until(() => saw(/There are \d+ of a max/), 'raised flood uses real water blocks');
  command('execute in minecraft:storm_islands if block 0 101 -3 minecraft:sea_lantern run minecraft:list');
  await until(() => log.filter(line => /There are \d+ of a max/.test(line)).length >= 2, 'flood preserves the pyramid peak');
  bot.chat('/storm lobby');
  await until(() => saw(/destroyed map blocks=[1-9]\d*/), 'destruction of map buildings');
  command('execute in minecraft:storm_islands if block 34 80 0 minecraft:sea_lantern run minecraft:list');
  await until(() => log.filter(line => /There are \d+ of a max/.test(line)).length >= 3, 'platform block still present');
  await until(() => log.filter(line => /Round started:/.test(line)).length >= 2, 'automatic next round without queue', 20000);
  assert(bot.entity.position.y < 149, 'The second round did not pull the player from the lobby');
  assert(!saw(/Server thread\/ERROR.*(Exception|StormTrials)/));
  console.log('Verified queue, live round, purchased perk, destruction, protected platform, and arena rotation.');
}

(async () => {
  try {
    await run();
  } catch (error) {
    console.error(error);
    process.exitCode = 1;
  } finally {
    if (bot) bot.quit();
    if (jvm.exitCode === null) command('stop');
    const watchdog = setTimeout(() => jvm.kill(), 30000);
    const [code] = await exit;
    clearTimeout(watchdog);
    if (code !== 0) process.exitCode = 1;
  }
})();
