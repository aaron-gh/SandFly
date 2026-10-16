/*
 * Copyright 2026 Backtalk contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

(() => {
  'use strict';

  const host = globalThis.__host;
  delete globalThis.__host;

  const call = (method, args) => {
    const result = host(method, args === undefined ? undefined : JSON.stringify(args));
    return result === undefined ? undefined : JSON.parse(result);
  };
  const send = (method, args) => {
    call(method, args);
  };
  const handlerName = (name) => `on${name[0].toUpperCase()}${name.slice(1)}`;
  const isThenable = (value) => typeof value?.then === 'function';

  const format = (value) => {
    if (typeof value === 'string') return value;
    if (value instanceof Error) return value.stack ? `${value}\n${value.stack}` : String(value);
    try {
      return typeof value === 'object' && value !== null ? JSON.stringify(value) : String(value);
    } catch (e) {
      return String(value);
    }
  };
  const reportError = (where, error) => send('error', { where, message: format(error) });
  const guard = (where, fn, ...args) => {
    try {
      const result = fn(...args);
      if (isThenable(result)) result.then(undefined, (error) => reportError(where, error));
      return result;
    } catch (error) {
      reportError(where, error);
      return undefined;
    }
  };

  const log = (level) => (...args) => send('log', { level, message: args.map(format).join(' ') });
  globalThis.console = Object.freeze(
    Object.fromEntries(
      [['log', 'info'], ['info', 'info'], ['debug', 'debug'], ['warn', 'warn'], ['error', 'error']]
        .map(([name, level]) => [name, log(level)]),
    ),
  );

  const timers = new Map();
  let nextTimer = 1;
  const addTimer = (fn, ms, args, repeat) => {
    if (typeof fn !== 'function') throw new TypeError('A timer needs a function');
    const id = nextTimer++;
    const delay = Math.max(repeat ? 10 : 0, Number(ms) || 0);
    timers.set(id, { fn, args, delay, repeat });
    send('timer.set', { id, ms: delay });
    return id;
  };
  const clearTimer = (id) => {
    if (timers.delete(id)) send('timer.clear', { id });
  };
  globalThis.setTimeout = (fn, ms, ...args) => addTimer(fn, ms, args, false);
  globalThis.setInterval = (fn, ms, ...args) => addTimer(fn, ms, args, true);
  globalThis.clearTimeout = clearTimer;
  globalThis.clearInterval = clearTimer;

  const MAX_OFFERS = 5;
  const pending = new Map();
  const promised = (result) =>
    typeof result?.promise === 'number'
      ? new Promise((resolve, reject) => pending.set(result.promise, { resolve, reject }))
      : Promise.resolve(result);

  const queryOf = (query) => (typeof query === 'string' ? { id: query } : query || {});
  const find = (query, limit, root) => call('screen.find', { query: queryOf(query), limit, root });
  const Node = class {
    constructor(snapshot) {
      Object.assign(this, snapshot);
      Object.freeze(this);
    }
    get parent() {
      return node(call('node.parent', { handle: this.handle }));
    }
    get children() {
      return call('node.children', { handle: this.handle }).map(node);
    }
    child(index) {
      return this.children[index] ?? null;
    }
    refresh() {
      return node(call('node.refresh', { handle: this.handle }));
    }
    find(query) {
      return node(find(query, 1, this.handle)[0]);
    }
    findAll(query, limit = 50) {
      return find(query, limit, this.handle).map(node);
    }
    setText(text) {
      return this.perform('setText', { text: String(text) });
    }
    focus() {
      return promised(call('node.focus', { handle: this.handle }));
    }
    perform(action, args) {
      return promised(call('node.action', { handle: this.handle, action, args }));
    }
    equals(other) {
      return other instanceof Node && other.handle === this.handle;
    }
    toString() {
      return this.text || this.contentDescription || this.id || this.className || 'item';
    }
  };
  for (const action of ['click', 'longClick', 'scrollForward', 'scrollBackward']) {
    Node.prototype[action] = function () {
      return this.perform(action);
    };
  }
  Object.freeze(Node.prototype);
  const node = (snapshot) => (snapshot ? new Node(snapshot) : null);

  const handlers = Object.fromEntries(
    [
      'focus',
      'speech',
      'notification',
      'announcement',
      'windowChange',
      'appEnter',
      'appLeave',
      'settingChange',
      'button',
      'textChange',
      'contentChange',
      'event',
      'actions',
    ].map((name) => [name, []]),
  );
  const commands = new Map();
  const syncHooks = () =>
    send('hooks', {
      names: Object.keys(handlers).filter((name) => handlers[name].length > 0),
      commands: [...commands.keys()],
    });
  const subscribe = (add, remove) => {
    add();
    syncHooks();
    return () => {
      if (remove()) syncHooks();
    };
  };
  const listen = (name) => (fn) => {
    if (typeof fn !== 'function') throw new TypeError(`${handlerName(name)} needs a function`);
    const list = handlers[name];
    return subscribe(
      () => list.push(fn),
      () => list.includes(fn) && list.splice(list.indexOf(fn), 1).length > 0,
    );
  };
  let declaredCommands;
  const onCommand = (id, fn) => {
    if (typeof fn !== 'function') throw new TypeError('onCommand needs a function');
    declaredCommands ??= new Set((call('manifest').commands || []).map((command) => command.id));
    if (!declaredCommands.has(id)) {
      throw new Error(`The manifest declares no command ${id}. Add it to the manifest's commands.`);
    }
    return subscribe(
      () => commands.set(id, fn),
      () => commands.get(id) === fn && commands.delete(id),
    );
  };

  const notify = (name, ...args) => {
    for (const fn of handlers[name]) guard(name, fn, ...args);
  };

  const speechOf = (result, text) => {
    if (result === false || result?.silent) return '';
    if (typeof result === 'string') return result;
    if (typeof result?.speak === 'string') return result.speak;
    if (typeof result !== 'object' || result === null || isThenable(result)) return text;
    return [result.before, text, result.after].filter((part) => typeof part === 'string').join(' ');
  };
  const rewrite = (name, argsOf, input) => {
    let text = input;
    for (const fn of handlers[name]) {
      try {
        text = speechOf(fn(...argsOf(text)), text);
      } catch (error) {
        reportError(handlerName(name), error);
      }
    }
    return text === input ? undefined : { text };
  };

  const runCommand = ({ id, via, direction, node: snapshot }) => {
    const fn = commands.get(id);
    if (!fn) return { fallback: true };
    const event = Object.freeze({
      id,
      via,
      direction: direction ?? undefined,
      node: node(snapshot),
    });
    return guard(`command ${id}`, fn, event) === false ? { fallback: true } : undefined;
  };

  const offers = new Map();
  let nextOffer = 1;
  const offerActions = ({ node: snapshot }) => {
    const item = node(snapshot);
    const offered = new Map();
    for (const fn of handlers.actions) {
      const list = guard('onActions', fn, item);
      if (!Array.isArray(list)) continue;
      for (const action of list) {
        const title = typeof action?.title === 'string' ? action.title.trim() : '';
        if (title && typeof action.run === 'function' && !offered.has(title)) {
          offered.set(title, () => action.run(item));
        }
      }
    }
    const offer = nextOffer++;
    offers.set(offer, offered);
    if (offers.size > MAX_OFFERS) offers.delete(offers.keys().next().value);
    return { offer, titles: [...offered.keys()] };
  };
  const runAction = ({ offer, title }) => {
    const run = offers.get(offer)?.get(title);
    if (run) guard(`action ${title}`, run);
  };

  const runTimer = ({ id }) => {
    const timer = timers.get(id);
    if (!timer) return;
    if (timer.repeat) {
      send('timer.set', { id, ms: timer.delay });
    } else {
      timers.delete(id);
    }
    guard('timer', timer.fn, ...timer.args);
  };

  const settle = ({ id, ok, value, error }) => {
    const waiting = pending.get(id);
    if (!waiting) return;
    pending.delete(id);
    if (ok) {
      waiting.resolve(value);
    } else {
      waiting.reject(new Error(error));
    }
  };

  const withInfo = (name) => (data) =>
    rewrite(name, (text) => [Object.freeze(data.arg), text], data.text);
  const withNode = (name) => (data) =>
    notify(name, Object.freeze({ ...data, node: node(data.node) }));
  const plain = (name) => (data) => notify(name, Object.freeze(data));
  const dispatchers = {
    focus: (data) => rewrite('focus', (text) => [node(data.arg), text], data.text),
    speech: (data) => rewrite('speech', (text) => [text], data.text),
    notification: withInfo('notification'),
    announcement: withInfo('announcement'),
    textChange: withNode('textChange'),
    contentChange: withNode('contentChange'),
    event: plain('event'),
    windowChange: plain('windowChange'),
    appEnter: plain('appEnter'),
    appLeave: plain('appLeave'),
    settingChange: (data) => notify('settingChange', data.key, data.value),
    button: (data) => notify('button', data.key),
    command: runCommand,
    actions: offerActions,
    dialogEvent: (data) => formEvent(data),
    runAction,
    timer: runTimer,
    settle,
  };
  globalThis.__bt_dispatch = (json) => {
    const { type, data } = JSON.parse(json);
    return dispatchers[type]?.(data) ?? undefined;
  };

  const Headers = class {
    #values;
    constructor(values) {
      this.#values = values;
      Object.freeze(this);
    }
    get(name) {
      return this.#values[String(name).toLowerCase()] ?? null;
    }
    has(name) {
      return String(name).toLowerCase() in this.#values;
    }
    forEach(fn) {
      for (const [key, value] of Object.entries(this.#values)) fn(value, key, this);
    }
  };
  const Response = class {
    #body;
    constructor({ status, statusText, url, headers, body }) {
      Object.assign(this, { status, statusText, url, ok: status >= 200 && status < 300 });
      this.headers = new Headers(headers);
      this.#body = body;
      Object.freeze(this);
    }
    text() {
      return Promise.resolve(this.#body);
    }
    json() {
      return this.text().then(JSON.parse);
    }
  };
  globalThis.fetch = async (url, { method = 'GET', headers = {}, body = null, timeout } = {}) => {
    const sent = Object.fromEntries(
      Object.entries(headers).map(([key, value]) => [key, String(value)]),
    );
    const json = body !== null && typeof body !== 'string';
    if (json && !Object.keys(sent).some((key) => key.toLowerCase() === 'content-type')) {
      sent['Content-Type'] = 'application/json';
    }
    const request = {
      url: String(url),
      method,
      headers: sent,
      body: json ? JSON.stringify(body) : body,
      timeout,
    };
    return new Response(await promised(call('fetch', request)));
  };

  const systemAction = (name) => () => promised(call('system.action', { name }));
  const system = Object.fromEntries(
    [
      'back',
      'home',
      'recents',
      'notifications',
      'quickSettings',
      'powerDialog',
      'lockScreen',
      'takeScreenshot',
      'allApps',
    ].map((name) => [name, systemAction(name)]),
  );
  system.openApp = (packageName) =>
    promised(call('system.openApp', { package: String(packageName) }));

  const keyed = (key, more) => ({ key: String(key), ...more });

  const between = (n, low, high) => n >= low && n <= high;
  const slavic = (n, few) =>
    n % 10 === 1 && n % 100 !== 11 ? 'one' : few(n % 10, n % 100) ? 'few' : 'many';
  const PLURAL_RULES = [
    [['ja', 'zh', 'ko', 'vi', 'th', 'id', 'ms', 'lo', 'my', 'km'], () => 'other'],
    [['fr', 'pt', 'hy'], (n) => (n < 2 ? 'one' : 'other')],
    [['ru', 'uk', 'be'], (n) => slavic(n, (d, h) => between(d, 2, 4) && !between(h, 12, 14))],
    [
      ['pl'],
      (n) =>
        n === 1 ? 'one' : between(n % 10, 2, 4) && !between(n % 100, 12, 14) ? 'few' : 'many',
    ],
    [['cs', 'sk'], (n) => (n === 1 ? 'one' : between(n, 2, 4) ? 'few' : 'other')],
    [
      ['hr', 'sr', 'bs'],
      (n) => {
        const category = slavic(n, (d, h) => between(d, 2, 4) && !between(h, 12, 14));
        return category === 'many' ? 'other' : category;
      },
    ],
    [
      ['lt'],
      (n) =>
        between(n % 100, 11, 19)
          ? 'other'
          : n % 10 === 1
            ? 'one'
            : n % 10 >= 2
              ? 'few'
              : 'other',
    ],
    [
      ['lv'],
      (n) =>
        n % 10 === 0 || between(n % 100, 11, 19)
          ? 'zero'
          : n % 10 === 1
            ? 'one'
            : 'other',
    ],
    [
      ['ro'],
      (n) => (n === 1 ? 'one' : n === 0 || between(n % 100, 1, 19) ? 'few' : 'other'),
    ],
    [
      ['sl'],
      (n) => ['other', 'one', 'two', 'few', 'few'][n % 100] ?? 'other',
    ],
    [['he'], (n) => (n === 1 ? 'one' : n === 2 ? 'two' : 'other')],
    [
      ['ar'],
      (n) =>
        n === 0
          ? 'zero'
          : n === 1
            ? 'one'
            : n === 2
              ? 'two'
              : between(n % 100, 3, 10)
                ? 'few'
                : between(n % 100, 11, 99)
                  ? 'many'
                  : 'other',
    ],
  ];
  const pluralCategory = (language, count) => {
    const n = Math.abs(Number(count));
    if (!Number.isInteger(n)) return 'other';
    const rule = PLURAL_RULES.find(([languages]) => languages.includes(language))?.[1];
    return rule ? rule(n) : n === 1 ? 'one' : 'other';
  };
  let translations;
  const i18n = () => (translations ??= call('i18n'));
  const fill = (text, params = {}) =>
    String(text).replace(/\{(\w+)\}/g, (match, name) =>
      name in params ? String(params[name]) : match,
    );
  const translate = (text, params) => {
    const value = i18n().messages[text];
    return fill(typeof value === 'string' && value ? value : text, params);
  };
  const ngettext = (singular, plural, count, params = {}) => {
    const { language, messages } = i18n();
    const forms = messages[singular];
    const form =
      forms && typeof forms === 'object'
        ? forms[pluralCategory(language.split('-')[0], count)] ?? forms.other
        : undefined;
    const english = Number(count) === 1 ? singular : plural;
    return fill(typeof form === 'string' && form ? form : english, { count, ...params });
  };

  const SAMPLE_RATE = 22050;
  const playAudio = (samples, { sampleRate = SAMPLE_RATE, volume = 1 } = {}) =>
    send('audio', { samples: Array.from(samples, Number), sampleRate, volume });
  const tone = (frequency, ms, sampleRate = SAMPLE_RATE) => {
    const length = Math.round((ms * sampleRate) / 1000);
    const fade = Math.min(length / 2, sampleRate / 200);
    return Array.from({ length }, (_, i) => {
      const envelope = Math.min(1, i / fade, (length - i) / fade);
      return Math.sin((2 * Math.PI * frequency * i) / sampleRate) * envelope;
    });
  };
  const optionOf = (option) =>
    typeof option === 'object' && option !== null
      ? { value: String(option.value), label: String(option.label ?? option.value) }
      : String(option);
  const text = (value) => (value == null ? undefined : String(value));
  const dialog = (method, args) => promised(call(method, args));
  const openForms = new Map();
  let nextForm = 1;
  const formItem = (item, index) => {
    const { onClick, onChange, ...props } = item;
    return {
      ...props,
      id: String(item.id ?? `item${index + 1}`),
      notify: typeof onChange === 'function',
      ...(Array.isArray(item.items) ? { items: item.items.map(optionOf) } : {}),
    };
  };
  const openForm = ({ title, items = [], onClose } = {}) => {
    const id = nextForm++;
    const sent = Array.from(items, formItem);
    const handlers = new Map(sent.map((item, index) => [item.id, items[index]]));
    const values = Object.fromEntries(
      sent.filter((item) => 'value' in item).map((item) => [item.id, item.value]),
    );
    let resolveClosed;
    const closed = new Promise((resolve) => {
      resolveClosed = resolve;
    });
    const dialog = Object.freeze({
      get: (item) => values[item],
      get values() {
        return { ...values };
      },
      set: (item, props) => {
        if ('value' in props) values[item] = props.value;
        const sentProps = Array.isArray(props.items)
          ? { ...props, items: props.items.map(optionOf) }
          : props;
        send('ui.update', { dialog: id, item: String(item), props: sentProps });
      },
      close: () => send('ui.close', { dialog: id }),
      closed,
    });
    openForms.set(id, { dialog, handlers, values, onClose, resolveClosed });
    send('ui.dialog', { dialog: id, title: text(title), items: sent });
    return dialog;
  };
  const formEvent = ({ dialog: id, item, event, value, values }) => {
    const form = openForms.get(id);
    if (!form) return;
    Object.assign(form.values, values);
    if (event === 'close') {
      openForms.delete(id);
      if (typeof form.onClose === 'function') guard('onClose', form.onClose, { ...form.values });
      form.resolveClosed({ ...form.values });
      return;
    }
    const handler = form.handlers.get(item);
    if (event === 'click' && typeof handler?.onClick === 'function') {
      guard(`onClick ${item}`, handler.onClick, form.dialog);
    } else if (event === 'change' && typeof handler?.onChange === 'function') {
      guard(`onChange ${item}`, handler.onChange, value, form.dialog);
    }
  };
  const ui = Object.freeze({
    dialog: openForm,
    choose: (title, options, { selected } = {}) =>
      dialog('ui.choose', {
        title: text(title),
        options: Array.from(options, optionOf),
        selected: text(selected),
      }),
    prompt: (title, { text: value = '', hint = '' } = {}) =>
      dialog('ui.prompt', { title: text(title), text: String(value), hint: String(hint) }),
    confirm: (message, { title, ok, cancel } = {}) =>
      dialog('ui.confirm', {
        message: String(message),
        title: text(title),
        ok: text(ok),
        cancel: text(cancel),
      }),
    alert: (message, { title } = {}) =>
      dialog('ui.alert', { message: String(message), title: text(title) }).then(() => undefined),
  });
  const binding = (method) => (command, { gesture, keys } = {}) =>
    send(method, { command: String(command), gesture, keys });

  const backtalk = Object.freeze({
    apiVersion: call('apiVersion'),
    get manifest() {
      return call('manifest');
    },
    get app() {
      return call('app');
    },
    ...Object.fromEntries(Object.keys(handlers).map((name) => [handlerName(name), listen(name)])),
    onCommand,
    get locale() {
      return call('locale');
    },
    get language() {
      return i18n().language;
    },
    _: translate,
    ngettext,
    speak: (text, { interrupt = false, rate, pitch, language } = {}) =>
      send('speak', { text: String(text), interrupt, rate, pitch, language }),
    playAudio,
    playTone: (frequency, ms = 100, { volume = 0.5 } = {}) =>
      playAudio(tone(Number(frequency), Number(ms)), { volume }),
    rules: Object.freeze({
      add: (rule) => call('rules.add', { rule }),
      remove: (id) => send('rules.remove', { id }),
      clear: () => send('rules.clear'),
    }),
    bind: binding('bindings.add'),
    unbind: binding('bindings.remove'),
    playSound: (name) => promised(call('sound', { name: String(name) })),
    vibrate: (pattern) =>
      send('vibrate', { pattern: Array.isArray(pattern) ? pattern : [Number(pattern) || 50] }),
    resume: () => send('resume'),
    screen: Object.freeze({
      focused: () => node(call('screen.focused')),
      root: () => node(call('screen.root')),
      find: (query) => node(find(query, 1)[0]),
      findAll: (query, limit = 50) => find(query, limit).map(node),
    }),
    storage: Object.freeze({
      get: (key, fallback) => {
        const stored = call('storage.get', keyed(key));
        return stored == null ? fallback : stored.value;
      },
      set: (key, value) => send('storage.set', keyed(key, { value })),
      remove: (key) => send('storage.remove', keyed(key)),
      keys: () => call('storage.keys'),
      clear: () => send('storage.clear'),
    }),
    settings: Object.freeze({
      get: (key) => call('settings.get', keyed(key)),
      all: () => call('settings.all'),
      set: (key, value) => send('settings.set', keyed(key, { value })),
      setOptions: (key, options) => send('settings.setOptions', keyed(key, { options })),
    }),
    clipboard: Object.freeze({
      get: () => call('clipboard.get') ?? null,
      set: (text) => send('clipboard.set', { text: String(text) }),
    }),
    system: Object.freeze(system),
    ui,
  });

  Object.defineProperty(globalThis, 'backtalk', { value: backtalk, enumerable: true });
})();
