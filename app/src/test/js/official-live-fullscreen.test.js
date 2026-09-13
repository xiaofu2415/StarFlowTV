const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const test = require('node:test');
const vm = require('node:vm');

const scriptPath = path.resolve(
  __dirname,
  '../../main/assets/official_live_fullscreen.js'
);

function createHarness() {
  const classNames = new Set();
  const styles = new Map();
  const listeners = new Map();
  let fullscreenRequests = 0;
  let observerCount = 0;
  let intervalCount = 0;

  const video = {
    paused: false,
    removeAttribute() {},
    addEventListener(name, callback) {
      listeners.set(name, callback);
    },
    requestFullscreen() {
      fullscreenRequests += 1;
      return Promise.resolve();
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
      if (id === 'player') return player;
      return styles.get(id) || null;
    },
    querySelector() {
      return null;
    },
  };
  class MutationObserver {
    constructor(callback) {
      this.callback = callback;
      observerCount += 1;
    }
    observe() {}
    disconnect() {}
  }
  const context = {
    document,
    MutationObserver,
    Promise,
    setInterval() {
      intervalCount += 1;
      return intervalCount;
    },
    clearInterval() {},
  };
  context.window = context;

  return {
    context,
    documentElement,
    player,
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
  assert.match(style.textContent, /#player\s*\{[^}]*position:\s*fixed\s*!important/i);
  assert.match(style.textContent, /#player\s*\{[^}]*width:\s*100vw\s*!important/i);
  assert.match(style.textContent, /#player\s*\{[^}]*height:\s*100vh\s*!important/i);
  assert.equal(harness.fullscreenRequests, 1);
});

test('reinjection is idempotent and exit restores the webpage', () => {
  assert.equal(fs.existsSync(scriptPath), true, 'fullscreen script must be packaged as an app asset');
  const source = fs.readFileSync(scriptPath, 'utf8');
  const harness = createHarness();

  vm.runInNewContext(source, harness.context);
  vm.runInNewContext(source, harness.context);

  assert.equal(harness.observerCount, 1);
  assert.equal(harness.intervalCount, 1);
  harness.context.window.__starflowOfficialFullscreen.exit();
  assert.equal(
    harness.documentElement.classList.contains('starflow-official-fullscreen'),
    false
  );
  assert.equal(harness.player.attributes.has('data-starflow-fullscreen'), false);
});
