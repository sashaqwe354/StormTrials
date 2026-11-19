const { spawn } = require('node:child_process');
const { once } = require('node:events');
const assert = require('node:assert/strict');
const path = require('node:path');
const mineflayer = require('mineflayer');
const protocol = require('minecraft-protocol');

const java = spawn('C:/Program Files/Java/jdk-27/bin/java.exe', [
  '-Dterminal.jline=false', '-Dterminal.ansi=false', '-Xms1G', '-Xmx2G', '-jar', 'paper.jar', '--nogui'
], { cwd: path.join(__dirname, '../test-release'), stdio: ['pipe', 'pipe', 'inherit'] });
const exit = once(java, 'exit');
const log = [];
let lineBuffer = '';
let bot;
let idle;
java.stdout.on('data', data => {
  lineBuffer += data;
  const lines = lineBuffer.split(/\r?\n/);
  lineBuffer = lines.pop();
  for (const line of lines) {
    log.push(line);
    if (/ERROR|Exception|StormTrials|\[TAB\]|Done \(/.test(line)) console.log(line);
  }
});

const pause = milliseconds => new Promise(resolve => setTimeout(resolve, milliseconds));
const seen = expression => log.some(line => expression.test(line));
const matching = expression => log.filter(line => expression.test(line));
const command = text => java.stdin.write(text + '\n');
async function until(condition, label, timeout = 10000) {
  const deadline = Date.now() + timeout;
  while (Date.now() < deadline && java.exitCode === null) {
    if (condition()) return;
    await pause(100);
  }
  throw new Error('Timed out: ' + label);
}

async function run() {
  await until(() => seen(/Done \([\d.,]+s\)! For help/), 'server ready', 95000);
  assert(seen(/20 rotating maps, 30 disasters, 28 recipes/));
  assert(seen(/\[TAB\].*Enabled/));
  const status = await new Promise((resolve, reject) => {
    protocol.ping({ host: '127.0.0.1', port: 25574, closeTimeout: 5000 }, (error, value) => error ? reject(error) : resolve(value));
  });
  assert(JSON.stringify(status.description).includes('Штормовой архипелаг'), 'Missing MOTD');
  const icon = Buffer.from(status.favicon.split(',')[1], 'base64');
  assert.equal(icon.readUInt32BE(16), 64);
  assert.equal(icon.readUInt32BE(20), 64);
  for (const map of ['LIGHTHOUSE', 'SHIP', 'CITADEL', 'VOLCANO', 'SKYBRIDGE', 'LIBRARY']) {
    command('storm map ' + map);
    await until(() => seen(new RegExp('Arena loaded: ' + map + '$')), map + ' build');
  }
  const username = 'Weather' + Date.now().toString(36).slice(-6);
  const messages = [];
  let tabHeader = '';
  bot = mineflayer.createBot({ host: '127.0.0.1', port: 25574, username, version: '1.21.11' });
  bot.on('error', error => console.error(error));
  bot.on('messagestr', message => {
    messages.push(message);
    if (message.includes('Достижение')) console.log(message);
  });
  bot._client.on('playerlist_header', data => { tabHeader = JSON.stringify(data); });
  await until(() => bot.inventory?.slots[42]?.name === 'book', 'lobby achievements item');
  await until(() => tabHeader.includes('Штормовой архипелаг'), 'TAB branded header');
  command('op ' + username);
  const idleMessages = [];
  idle = mineflayer.createBot({ host: '127.0.0.1', port: 25574, username: 'Idle' + Date.now().toString(36).slice(-6), version: '1.21.11' });
  idle.on('messagestr', message => idleMessages.push(message));
  await until(() => idle.inventory?.slots[41]?.name === 'lime_dye', 'second player lobby');
  idle.chat('/storm pause');
  await until(() => idleMessages.some(m => m.includes('Ты пропускаешь раунды')), 'explicit participation pause');
  bot.chat('/storm afk');
  await until(() => messages.some(m => m.includes('AFK включён')), 'manual AFK enabled');
  command('storm start');
  await pause(3200);
  assert.equal(matching(/Round started:/).length, 0, 'No round while both players are excluded');
  bot.chat('/storm join');
  await until(() => messages.some(m => /Автостарт через (?:39|40) с/.test(m)), '40-second intermission');
  bot.chat('/storm shop');
  await until(() => bot.currentWindow?.slots[27]?.name === 'diamond_boots', 'full shop catalog');
  assert(JSON.stringify(bot.currentWindow.slots[0].components).includes('stormtrials:bandage'), 'Optional model identifier');
  bot.clickWindow(0, 0, 0).catch(() => {});
  await until(() => messages.some(m => m.includes('Куплено на следующий раунд: Походный бинт')), 'buy consumable');
  bot.closeWindow(bot.currentWindow);

  for (const [index, disaster] of ['ice_comets', 'sand_wall', 'sinkholes', 'waterspout'].entries()) {
    command('storm map LIBRARY');
    command('storm start ' + disaster);
    await until(() => matching(/Round started:/).length > index, disaster + ' round start');
    await until(() => bot.inventory.slots[36]?.name === 'crafting_table', 'field bench');
    assert.equal(idle.inventory.slots[41]?.name, 'gray_dye', 'Paused player remains in lobby');
    if (index === 0) {
      await until(() => messages.some(message => message.includes('Достижение открыто: Первый шторм')), 'first achievement');
      await until(() => messages.some(message => message.includes('Покупки доставлены')), 'purchased item delivered');
      command('minecraft:damage ' + username + ' 4 minecraft:generic');
      await until(() => bot.health < 20, 'test damage');
      bot.chat('/storm kit');
      await until(() => bot.currentWindow?.slots[27]?.name === 'diamond_boots', 'new accessories catalog');
      assert(bot.currentWindow.slots.slice(0, 28).every(Boolean));
      bot.clickWindow(0, 1, 0).catch(() => {});
      await until(() => messages.some(m => m.includes('Применено: Походный бинт')) && bot.health === 20, 'use bought gear');
      bot.closeWindow(bot.currentWindow);
    }
    await until(() => messages.filter(m => m.includes('Бафф:') && m.includes('Дебафф:')).length > index, 'round modifiers announced');
    command('minecraft:effect give ' + username + ' minecraft:resistance 20 4 true');
    await pause(index === 1 ? 19000 : 14500);
    bot.chat('/storm lobby');
    await until(() => matching(/Round ended:/).length > index, 'destruction statistics for ' + disaster);
    const round = matching(/Round ended:/).at(-1);
    assert(/destroyed map blocks=[1-9]\d*/.test(round), round);
    command('minecraft:effect clear ' + username + ' minecraft:resistance');
  }
  bot.chat('/storm achievements');
  await until(() => bot.currentWindow?.slots[0]?.name === 'compass' && bot.currentWindow?.slots[10]?.name === 'gray_dye',
    'achievement GUI shows earned and locked medals');
  assert(!seen(/Server thread\/ERROR.*(?:StormTrials|Exception)/));
  console.log('Verified 30 scenario registrations, four new hazard patterns, modifiers, purchased gear, models, 40-second intermission, AFK and opt-out.');
}

(async () => {
  try {
    await run();
  } catch (error) {
    process.exitCode = 1;
    console.error(error);
  } finally {
    if (bot) bot.quit();
    if (idle) idle.quit();
    if (java.exitCode === null) command('stop');
    const watchdog = setTimeout(() => java.kill(), 30000);
    const [code] = await exit;
    clearTimeout(watchdog);
    if (code !== 0) process.exitCode = 1;
  }
})();
