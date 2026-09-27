const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const test = require('node:test');
const vm = require('node:vm');

const scriptPath = path.resolve(
  __dirname,
  '../../main/assets/official_live_fullscreen.js'
);

function createHarness(options = {}) {
  const classNames = new Set();
  const styles = new Map();
  const listeners = new Map();
  let fullscreenRequests = 0;
  let observerCount = 0;
  let intervalCount = 0;
  const qualityClicks = [];
  let pendingVideoFrameCallback = null;
  const intervalCallbacks = new Map();
  let clockMs = 0;
  let totalFrames = 0;
  let droppedFrames = 0;

  const video = {
    paused: options.videoPaused === true,
    videoWidth: options.videoWidth || 1920,
    videoHeight: options.videoHeight || 1080,
    currentTime: 0,
    readyState: options.readyState === undefined ? 4 : options.readyState,
    buffered: {
      length: 1,
      start() { return 0; },
      end() { return video.currentTime + 2; },
    },
    getVideoPlaybackQuality() {
      return { totalVideoFrames: totalFrames, droppedVideoFrames: droppedFrames };
    },
    removeAttribute() {},
    addEventListener(name, callback) {
      listeners.set(name, callback);
    },
    requestVideoFrameCallback(callback) {
      if (options.supportsVideoFrameCallback !== false) {
        pendingVideoFrameCallback = callback;
        return 1;
      }
      return undefined;
    },
    requestFullscreen() {
      fullscreenRequests += 1;
      return options.rejectFullscreen
        ? Promise.reject(new Error('fullscreen denied'))
        : Promise.resolve();
    },
  };
  const player = {
    attributes: new Map(),
    setAttribute(name, value) {
      this.attributes.set(name, value);
    },
    removeAttribute(name) {
      this.attributes.delete(name);
    },
    querySelector(selector) {
      return selector === 'video' ? video : null;
    },
  };
  const qualityElements = (options.qualities || []).map((label) => ({
    textContent: label,
    innerText: label,
    hidden: false,
    offsetParent: {},
    click() {
      qualityClicks.push(label);
    },
    getAttribute() {
      return '';
    },
    matches() {
      return false;
    },
  }));
  const documentElement = {
    classList: {
      add(name) {
        classNames.add(name);
      },
      remove(name) {
        classNames.delete(name);
      },
      contains(name) {
        return classNames.has(name);
      },
    },
  };
  const document = {
    documentElement,
    body: {},
    fullscreenElement: null,
    webkitFullscreenElement: null,
    head: {
      appendChild(node) {
        styles.set(node.id, node);
      },
    },
    createElement(tagName) {
      assert.equal(tagName, 'style');
      return { id: '', textContent: '' };
    },
    getElementById(id) {
      if (id === 'player' && options.fallbackPlayer !== true) return player;
      return styles.get(id) || null;
    },
    querySelector(selector) {
      if (options.fallbackPlayer === true && selector === '.video_box') return player;
      return null;
    },
    querySelectorAll() {
      return qualityElements;
    },
  };
  class MutationObserver {
    constructor(callback) {
      this.callback = callback;
      observerCount += 1;
      harnessObserver = this;
    }
    observe() {}
    disconnect() {}
  }
  let harnessObserver = null;
  const context = {
    document,
    MutationObserver,
    Promise,
    JSON,
    Date: { now: () => clockMs },
    setInterval(callback) {
      intervalCount += 1;
      intervalCallbacks.set(intervalCount, callback);
      return intervalCount;
    },
    clearInterval(id) {
      intervalCallbacks.delete(id);
    },
    setTimeout(callback) {
      callback();
      return 1;
    },
    clearTimeout() {},
  };
  context.window = context;

  return {
    context,
    documentElement,
    player,
    video,
    qualityClicks,
    get fullscreenRequests() {
      return fullscreenRequests;
    },
    get observerCount() {
      return observerCount;
    },
    get intervalCount() {
      return intervalCount;
    },
    getStyle(id) {
      return styles.get(id);
    },
    trigger(name) {
      const callback = listeners.get(name);
      if (callback) callback();
    },
    triggerMutation() {
      if (harnessObserver) harnessObserver.callback();
    },
    triggerFirstFrame() {
      const callback = pendingVideoFrameCallback;
      pendingVideoFrameCallback = null;
      if (callback) callback(0, {});
    },
    advanceStableSeconds(seconds) {
      for (let i = 0; i < seconds; i += 1) {
        clockMs += 1000;
        video.currentTime += 1;
        totalFrames += 30;
        Array.from(intervalCallbacks.values()).forEach((callback) => callback());
      }
    },
  };
}

test('official player is promoted to a viewport-sized fullscreen layer', async () => {
  assert.equal(fs.existsSync(scriptPath), true, 'fullscreen script must be packaged as an app asset');
  const source = fs.readFileSync(scriptPath, 'utf8');
  const harness = createHarness();

  vm.runInNewContext(source, harness.context);
  await Promise.resolve();

  assert.equal(
    harness.documentElement.classList.contains('starflow-official-fullscreen'),
    true
  );
  assert.equal(harness.player.attributes.get('data-starflow-fullscreen'), 'true');
  const style = harness.getStyle('starflow-official-fullscreen-style');
  assert.ok(style, 'fullscreen stylesheet must be installed');
  assert.match(style.textContent, /\[data-starflow-fullscreen="true"\]\s*\{[^}]*position:\s*fixed\s*!important/i);
  assert.match(style.textContent, /\[data-starflow-fullscreen="true"\]\s*\{[^}]*width:\s*100vw\s*!important/i);
  assert.match(style.textContent, /\[data-starflow-fullscreen="true"\]\s*\{[^}]*height:\s*100vh\s*!important/i);
  assert.equal(harness.fullscreenRequests, 1);
});

test('reinjection is idempotent and exit restores the webpage', () => {
  assert.equal(fs.existsSync(scriptPath), true, 'fullscreen script must be packaged as an app asset');
  const source = fs.readFileSync(scriptPath, 'utf8');
  const harness = createHarness();

  vm.runInNewContext(source, harness.context);
  vm.runInNewContext(source, harness.context);

  assert.equal(harness.observerCount, 1);
  assert.equal(harness.intervalCount, 0);
  harness.context.window.__starflowOfficialFullscreen.exit();
  assert.equal(
    harness.documentElement.classList.contains('starflow-official-fullscreen'),
    false
  );
  assert.equal(harness.player.attributes.has('data-starflow-fullscreen'), false);
});

test('fallback player selector receives the same fullscreen styling', () => {
  assert.equal(fs.existsSync(scriptPath), true, 'fullscreen script must be packaged as an app asset');
  const source = fs.readFileSync(scriptPath, 'utf8');
  const harness = createHarness({ fallbackPlayer: true });

  vm.runInNewContext(source, harness.context);

  assert.equal(harness.player.attributes.get('data-starflow-fullscreen'), 'true');
  assert.match(
    harness.getStyle('starflow-official-fullscreen-style').textContent,
    /\[data-starflow-fullscreen="true"\]\s*\{[^}]*position:\s*fixed\s*!important/i
  );
});

test('exit prevents delayed video events from re-entering native fullscreen', async () => {
  assert.equal(fs.existsSync(scriptPath), true, 'fullscreen script must be packaged as an app asset');
  const source = fs.readFileSync(scriptPath, 'utf8');
  const harness = createHarness({ videoPaused: true });

  vm.runInNewContext(source, harness.context);
  harness.context.window.__starflowOfficialFullscreen.exit();
  harness.trigger('playing');
  await Promise.resolve();

  assert.equal(harness.fullscreenRequests, 0);
});

test('a rejected native fullscreen request is not retried on every mutation', async () => {
  assert.equal(fs.existsSync(scriptPath), true, 'fullscreen script must be packaged as an app asset');
  const source = fs.readFileSync(scriptPath, 'utf8');
  const harness = createHarness({ rejectFullscreen: true });

  vm.runInNewContext(source, harness.context);
  await Promise.resolve();
  harness.triggerMutation();
  harness.triggerMutation();
  await Promise.resolve();

  assert.equal(harness.fullscreenRequests, 1);
});

test('highest quality ramps one step at a time after stable playback', () => {
  const source = fs.readFileSync(scriptPath, 'utf8');
  const harness = createHarness({ videoHeight: 480, qualities: ['流畅', '高清', '超清'] });

  vm.runInNewContext(source, harness.context);
  harness.context.window.__starflowOfficialFullscreen.setQuality('highest');

  assert.deepEqual(harness.qualityClicks, [], 'highest must not replace the adaptive stream before a frame is shown');
  harness.triggerFirstFrame();
  harness.advanceStableSeconds(7);
  assert.deepEqual(harness.qualityClicks, [], 'the first quality step must wait for a stable window');
  harness.advanceStableSeconds(1);
  assert.deepEqual(harness.qualityClicks, ['高清']);
  harness.advanceStableSeconds(8);
  assert.deepEqual(harness.qualityClicks, ['高清', '超清']);
});

test('1080p preference degrades to the best available lower official quality', () => {
  const source = fs.readFileSync(scriptPath, 'utf8');
  const harness = createHarness({ videoHeight: 360, qualities: ['流畅', '高清'] });

  vm.runInNewContext(source, harness.context);
  harness.context.window.__starflowOfficialFullscreen.setQuality('1080p');

  assert.deepEqual(harness.qualityClicks, [], 'fixed quality must wait until the first frame');
  harness.triggerFirstFrame();
  harness.advanceStableSeconds(8);
  assert.equal(harness.qualityClicks.at(-1), '高清');
});

test('auto quality leaves the official player adaptive choice untouched', () => {
  const source = fs.readFileSync(scriptPath, 'utf8');
  const harness = createHarness({ qualities: ['流畅', '高清', '超清'] });

  vm.runInNewContext(source, harness.context);
  harness.context.window.__starflowOfficialFullscreen.setQuality('auto');

  harness.triggerFirstFrame();
  harness.advanceStableSeconds(20);
  assert.deepEqual(harness.qualityClicks, []);
});

test('highest quality waits for loadeddata and stable playback when frame callbacks are unavailable', () => {
  const source = fs.readFileSync(scriptPath, 'utf8');
  const harness = createHarness({ supportsVideoFrameCallback: false, readyState: 0, videoHeight: 480, qualities: ['高清'] });

  vm.runInNewContext(source, harness.context);
  harness.context.window.__starflowOfficialFullscreen.setQuality('highest');
  assert.deepEqual(harness.qualityClicks, []);
  harness.trigger('loadeddata');
  harness.advanceStableSeconds(8);
  assert.equal(harness.qualityClicks.at(-1), '高清');
});


test('a stall after a quality upgrade falls back to adaptive playback', () => {
  const source = fs.readFileSync(scriptPath, 'utf8');
  const harness = createHarness({ videoHeight: 480, qualities: ['自动', '高清', '超清'] });

  vm.runInNewContext(source, harness.context);
  harness.context.window.__starflowOfficialFullscreen.setQuality('highest');
  harness.triggerFirstFrame();
  harness.advanceStableSeconds(8);
  assert.equal(harness.qualityClicks.at(-1), '高清');

  harness.trigger('waiting');
  assert.equal(harness.qualityClicks.at(-1), '自动');
});

test('reports actual video resolution from the official player', () => {
  const source = fs.readFileSync(scriptPath, 'utf8');
  const harness = createHarness({ videoWidth: 1920, videoHeight: 1080 });

  vm.runInNewContext(source, harness.context);

  assert.equal(
    harness.context.window.__starflowOfficialFullscreen.getResolution(),
    '1920×1080'
  );
});
