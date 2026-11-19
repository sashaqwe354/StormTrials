const { spawn } = require('node:child_process');
const { once } = require('node:events');
const path = require('node:path');
const mineflayer = require('mineflayer');

const jvm = spawn('C:/Program Files/Java/jdk-27/bin/java.exe', [
  '-Dterminal.jline=false', '-Dterminal.ansi=false',
  '-Xms1G', '-Xmx2G', '-jar', 'paper.jar', '--nogui'
], { cwd: path.join(__dirname, '../test-server'), stdio: ['pipe', 'pipe', 'inherit'] });
const closed = once(jvm, 'exit');
const timeout = setTimeout(() => {
  console.error('Integration test timed out.');
  process.exitCode = 1;
  jvm.kill();
}, 175000);

let bot;
let done = false;
let result = false;
let roundStarted = false;
let summary = '';
const maps = new Set();
const names = ['Обсерватория «Север»', 'Приливная верфь', 'Базальтовые сады'];
let pluginStates = new Set();
let buffer = '';

function finish(error) {
  if (done) return;
  done = true;
  if (error) {
    console.error(error);
    process.exitCode = 1;
  }
  if (bot) bot.quit();
  if (jvm.stdin.writable) jvm.stdin.write('stop\n');
}

jvm.stdout.on('data', chunk => {
  buffer += chunk;
  const lines = buffer.split(/\r?\n/);
  buffer = lines.pop();
  for (const line of lines) {
    if (/ERROR|Exception|Disabling|StormTrials|Enabling Essentials|Done \(/.test(line)) console.log(line);
    for (const name of ['Essentials', 'EssentialsSpawn', 'StormTrials']) {
      if (line.includes('Enabling ' + name + ' v')) pluginStates.add(name);
    }
    if (/\bDone \([\d.,]+s\)! For help/.test(line) && !bot) {
      if (pluginStates.size !== 3) return finish(new Error('Did not see all three plugins enable.'));
      connect();
    }
    if (/\[Server thread\/ERROR\]/.test(line) && !done) finish(new Error(line));
  }
});

function connect() {
  bot = mineflayer.createBot({ host: '127.0.0.1', port: 25574, username: 'StormTest', version: '1.21.11' });
  bot.once('spawn', () => bot.chat('/storm maps'));
  bot.on('error', finish);
  bot.on('kicked', reason => finish(new Error(JSON.stringify(reason))));
  bot.on('messagestr', message => {
    if (!message.includes('[StormTrials]')) return;
    console.log(message);
    for (const name of names) {
      if (message.endsWith(name)) maps.add(name);
    }
    if (maps.size === 3 && !summary) {
      summary = 'queued';
      bot.chat('/storm join');
    }
    if (message.includes('Ты в очереди.')) {
      jvm.stdin.write('storm start\n');
    }
    if (names.some(name => message.includes(name + ' — '))) {
      roundStarted = true;
    }
    if (roundStarted && /Ты пережил бедствие|Бедствие тебя настигло|покинул границы/.test(message)) {
      result = true;
      bot.chat('/storm stats');
    }
    if (result && /Раунды: [1-9]\d*/.test(message)) bot.chat('/storm leave');
    if (result && message.includes('Ты вышел из режима.')) {
      setTimeout(() => bot.chat('/sethome storm-check'), 500);
    }
  });
  bot.on('messagestr', message => {
    if (/home set|точка дома установлена/i.test(message)) {
      console.log('Verified three arenas, lobby, round start and result, stats, exit and non-op /sethome.');
      finish();
    }
    if (/permission.*command/i.test(message)) finish(new Error(message));
  });
}

closed.then(([code]) => {
  clearTimeout(timeout);
  if (!done || code !== 0) {
    console.error('Unexpected Java exit:', code);
    process.exitCode = 1;
  }
});
