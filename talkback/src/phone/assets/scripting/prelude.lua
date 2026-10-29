--[[
Copyright 2026 SandFly contributors

Licensed under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License. You may obtain a copy of
the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations under
the License.
]]

-- Runs once in each script's Lua state, before the script. It builds the table the script runs
-- in, with the sf API, and keeps everything else, including the host functions, out of reach.

local host_call, read_module, overtime, with_java, memory_kb = ...

local G = _G
local assert, error, getmetatable, ipairs, next, pairs, pcall, rawequal, rawget, rawlen, rawset,
  select, setmetatable, tonumber, tostring, type, xpcall, load, collectgarbage =
  assert, error, getmetatable, ipairs, next, pairs, pcall, rawequal, rawget, rawlen, rawset,
  select, setmetatable, tonumber, tostring, type, xpcall, load, collectgarbage
local string, table, math, utf8, coroutine, os, debug = string, table, math, utf8, coroutine, os, debug
local byte, char, find, format, gsub, rep, sub, lower, upper =
  string.byte, string.char, string.find, string.format, string.gsub, string.rep, string.sub,
  string.lower, string.upper
local concat, insert, remove, pack, unpack, sort =
  table.concat, table.insert, table.remove, table.pack, table.unpack, table.sort
local floor, huge, mtype = math.floor, math.huge, math.type
local co_create, co_resume, co_yield, co_status, co_close, co_isyieldable, co_running =
  coroutine.create, coroutine.resume, coroutine.yield, coroutine.status, coroutine.close,
  coroutine.isyieldable, coroutine.running
local sethook, traceback = debug.sethook, debug.traceback
local java = rawget(G, 'java')

-- Limits. The messages match LuaEngine.OUT_OF_TIME and OUT_OF_MEMORY.
local OUT_OF_TIME = 'The script ran out of time'
local OUT_OF_MEMORY = 'The script used too much memory'
local HOOK_COUNT = 10000
local MAX_STRING = 8 * 1024 * 1024

local function is_kill(e)
  return e == OUT_OF_TIME or e == OUT_OF_MEMORY
end

local function hook()
  if overtime() then
    error(OUT_OF_TIME, 0)
  end
  if collectgarbage('count') > memory_kb then
    collectgarbage('collect')
    if collectgarbage('count') > memory_kb then
      error(OUT_OF_MEMORY, 0)
    end
  end
end

-- A coroutine doesn't take the hook of the thread that made it, so every one gets its own.
local function guard(co)
  sethook(co, hook, '', HOOK_COUNT)
  return co
end
sethook(hook, '', HOOK_COUNT)

---------------------------------------------------------------------------------------------------
-- JSON. Only text crosses to SandFly.

local json = {}

local escapes = {
  ['"'] = '\\"', ['\\'] = '\\\\', ['\b'] = '\\b', ['\f'] = '\\f', ['\n'] = '\\n', ['\r'] = '\\r',
  ['\t'] = '\\t',
}

local function encode_string(s)
  -- Text that isn't UTF-8 can't reach Java intact.
  if not utf8.len(s) then
    s = gsub(s, '[\128-\255]', '?')
  end
  return '"' .. gsub(s, '[%c"\\]', function(c)
    return escapes[c] or format('\\u%04x', byte(c))
  end) .. '"'
end

local function is_array(t)
  local n = rawlen(t)
  if n == 0 then
    return next(t) == nil
  end
  for k in pairs(t) do
    if mtype(k) ~= 'integer' or k < 1 or k > n then
      return false
    end
  end
  return true
end

local function encode(value, depth, out)
  local kind = type(value)
  if value == nil or value == json.null then
    out[#out + 1] = 'null'
  elseif kind == 'boolean' then
    out[#out + 1] = value and 'true' or 'false'
  elseif kind == 'number' then
    if value ~= value or value == huge or value == -huge then
      out[#out + 1] = 'null'
    elseif mtype(value) == 'integer' then
      out[#out + 1] = format('%d', value)
    else
      out[#out + 1] = format('%.17g', value)
    end
  elseif kind == 'string' then
    out[#out + 1] = encode_string(value)
  elseif kind == 'table' then
    if depth > 64 then
      error('The value is nested too deeply', 0)
    end
    if is_array(value) then
      out[#out + 1] = '['
      for i = 1, rawlen(value) do
        if i > 1 then
          out[#out + 1] = ','
        end
        encode(value[i], depth + 1, out)
      end
      out[#out + 1] = ']'
    else
      out[#out + 1] = '{'
      local first = true
      for k, v in pairs(value) do
        local tk = type(k)
        if (tk == 'string' or tk == 'number') and type(v) ~= 'function' then
          if not first then
            out[#out + 1] = ','
          end
          first = false
          out[#out + 1] = encode_string(tostring(k))
          out[#out + 1] = ':'
          encode(v, depth + 1, out)
        end
      end
      out[#out + 1] = '}'
    end
  else
    out[#out + 1] = 'null'
  end
end

function json.encode(value)
  local out = {}
  encode(value, 0, out)
  return concat(out)
end

local function decode_error(text, i, what)
  error(format('Bad JSON at character %d: %s', i, what), 0)
end

local function skip(text, i)
  return find(text, '[^ \t\r\n]', i) or #text + 1
end

local decode_value

local function decode_string(text, i)
  local parts = {}
  local j = i + 1
  while true do
    local s, e, c = find(text, '(["\\])', j)
    if not s then
      decode_error(text, i, 'unclosed text')
    end
    parts[#parts + 1] = sub(text, j, s - 1)
    if c == '"' then
      return concat(parts), e + 1
    end
    local esc = sub(text, e + 1, e + 1)
    if esc == 'u' then
      local code = tonumber(sub(text, e + 2, e + 5), 16)
      if not code then
        decode_error(text, e, 'bad \\u escape')
      end
      j = e + 6
      if code >= 0xD800 and code <= 0xDBFF and sub(text, j, j + 1) == '\\u' then
        local low = tonumber(sub(text, j + 2, j + 5), 16)
        if low and low >= 0xDC00 and low <= 0xDFFF then
          code = 0x10000 + (code - 0xD800) * 0x400 + (low - 0xDC00)
          j = j + 6
        end
      end
      parts[#parts + 1] = utf8.char(code)
    else
      local map = { b = '\b', f = '\f', n = '\n', r = '\r', t = '\t', ['"'] = '"', ['\\'] = '\\', ['/'] = '/' }
      local ch = map[esc]
      if not ch then
        decode_error(text, e, 'bad escape')
      end
      parts[#parts + 1] = ch
      j = e + 2
    end
  end
end

decode_value = function(text, i, depth)
  if depth > 200 then
    decode_error(text, i, 'nested too deeply')
  end
  i = skip(text, i)
  local c = sub(text, i, i)
  if c == '{' then
    local result = {}
    i = skip(text, i + 1)
    if sub(text, i, i) == '}' then
      return result, i + 1
    end
    while true do
      if sub(text, i, i) ~= '"' then
        decode_error(text, i, 'expected a name')
      end
      local key
      key, i = decode_string(text, i)
      i = skip(text, i)
      if sub(text, i, i) ~= ':' then
        decode_error(text, i, 'expected :')
      end
      local value
      value, i = decode_value(text, i + 1, depth + 1)
      result[key] = value
      i = skip(text, i)
      c = sub(text, i, i)
      if c == '}' then
        return result, i + 1
      elseif c ~= ',' then
        decode_error(text, i, 'expected , or }')
      end
      i = skip(text, i + 1)
    end
  elseif c == '[' then
    local result = {}
    local n = 0
    i = skip(text, i + 1)
    if sub(text, i, i) == ']' then
      return result, i + 1
    end
    while true do
      local value
      value, i = decode_value(text, i, depth + 1)
      n = n + 1
      result[n] = value
      i = skip(text, i)
      c = sub(text, i, i)
      if c == ']' then
        return result, i + 1
      elseif c ~= ',' then
        decode_error(text, i, 'expected , or ]')
      end
      i = i + 1
    end
  elseif c == '"' then
    return decode_string(text, i)
  elseif sub(text, i, i + 3) == 'true' then
    return true, i + 4
  elseif sub(text, i, i + 4) == 'false' then
    return false, i + 5
  elseif sub(text, i, i + 3) == 'null' then
    return nil, i + 4
  else
    local s, e = find(text, '^-?%d+%.?%d*[eE]?[-+]?%d*', i)
    if not s then
      decode_error(text, i, 'unexpected ' .. (c == '' and 'end' or c))
    end
    local number = tonumber(sub(text, s, e))
    if not number then
      decode_error(text, i, 'bad number')
    end
    return math.tointeger(number) or number, e + 1
  end
end

function json.decode(text)
  if type(text) ~= 'string' then
    error('json.decode needs text', 2)
  end
  local value, i = decode_value(text, 1, 0)
  if skip(text, i) <= #text then
    decode_error(text, i, 'more after the value')
  end
  return value
end

-- Stands for null in values given to json.encode.
json.null = setmetatable({}, { __tostring = function() return 'null' end, __metatable = false })

---------------------------------------------------------------------------------------------------
-- Calls to SandFly.

local function call(method, args)
  local ok, result = host_call(method, args ~= nil and json.encode(args) or nil)
  if not ok then
    error(result, 3)
  end
  if result == nil then
    return nil
  end
  return json.decode(result)
end

local function send(method, args)
  call(method, args)
end

local function describe(value)
  if type(value) == 'string' then
    return value
  elseif type(value) == 'table' and getmetatable(value) == nil then
    local ok, text = pcall(json.encode, value)
    if ok then
      return text
    end
  end
  return tostring(value)
end

local function report(where, message)
  host_call('error', json.encode({ where = where, message = describe(message) }))
end

local function log(level, ...)
  local n = select('#', ...)
  local parts = {}
  for i = 1, n do
    parts[i] = describe((select(i, ...)))
  end
  host_call('log', json.encode({ level = level, message = concat(parts, ' ') }))
end

---------------------------------------------------------------------------------------------------
-- Tasks. Calls that wait for SandFly, such as sf.ui.confirm, yield the task's coroutine with WAIT
-- and a key, and the task goes on when the answer comes.

local WAIT = setmetatable({}, { __metatable = false })
local waiting = {}

local function raw_resume_checked(co, ...)
  local r = pack(co_resume(co, ...))
  if not r[1] and is_kill(r[2]) then
    error(r[2], 0)
  end
  return r
end

local function park(key, co, where)
  local list = waiting[key]
  if not list then
    list = {}
    waiting[key] = list
  end
  list[#list + 1] = { co = co, where = where }
end

-- Runs a task until it ends or waits. Returns true and its results if it ended, true and nil if
-- it waits, or false if it failed.
local function step(co, where, ...)
  local r = raw_resume_checked(co, ...)
  if not r[1] then
    report(where, traceback(co, describe(r[2])))
    return false
  end
  if co_status(co) == 'dead' then
    return true, r
  end
  if r[2] == WAIT then
    park(r[3], co, where)
    return true, nil
  end
  report(where, 'coroutine.yield was called outside a coroutine')
  co_close(co)
  return false
end

local function spawn(where, fn, ...)
  return step(guard(co_create(fn)), where, ...)
end

local function wake(key, ...)
  local list = waiting[key]
  if not list then
    return
  end
  waiting[key] = nil
  for _, w in ipairs(list) do
    step(w.co, w.where, ...)
  end
end

local function can_wait()
  return co_isyieldable()
end

local function wait_for(key)
  if not can_wait() then
    error('This waits for an answer, so call it from an event handler, a command, a timer or sf.run', 3)
  end
  return co_yield(WAIT, key)
end

-- Waits for a host promise. With optional set, a call outside a task doesn't wait and gives nil.
local function await(result, optional)
  if type(result) ~= 'table' or mtype(result.promise) ~= 'integer' then
    return result
  end
  if optional and not can_wait() then
    return nil
  end
  local ok, value = wait_for(result.promise)
  if not ok then
    error(value, 3)
  end
  return value
end

---------------------------------------------------------------------------------------------------
-- The script's own environment.

local env = {}
local loaded = {}

local function rethrow_kill(ok, ...)
  if not ok and is_kill((...)) then
    error((...), 0)
  end
  return ok, ...
end

-- A resume that passes waits on to the task, so that sf calls work inside the script's own
-- coroutines too.
local function script_resume(co, ...)
  local r = pack(co_resume(co, ...))
  while r[1] and r[2] == WAIT and co_status(co) == 'suspended' do
    local back = pack(co_yield(WAIT, r[3]))
    r = pack(co_resume(co, unpack(back, 1, back.n)))
  end
  if not r[1] and is_kill(r[2]) then
    error(r[2], 0)
  end
  return unpack(r, 1, r.n)
end

local script_coroutine = {
  create = function(fn)
    return guard(co_create(fn))
  end,
  resume = script_resume,
  wrap = function(fn)
    local co = guard(co_create(fn))
    return function(...)
      local r = pack(script_resume(co, ...))
      if not r[1] then
        error(r[2], 0)
      end
      return unpack(r, 2, r.n)
    end
  end,
  yield = co_yield,
  status = co_status,
  running = co_running,
  isyieldable = co_isyieldable,
  close = function(co)
    return rethrow_kill(co_close(co))
  end,
}

-- string.rep is the one call that can ask for any amount of memory at once.
string.rep = function(s, n, sep)
  s = tostring(s)
  n = tonumber(n) or 0
  local size = #s * n + (sep and #tostring(sep) * (n - 1) or 0)
  if size > MAX_STRING then
    error('string.rep would make text longer than ' .. MAX_STRING .. ' bytes', 2)
  end
  return rep(s, n, sep)
end
string.dump = nil
getmetatable('').__metatable = false

env.assert = assert
env.error = error
env.ipairs = ipairs
env.next = next
env.pairs = pairs
env.rawequal = rawequal
env.rawget = rawget
env.rawlen = rawlen
env.rawset = rawset
env.select = select
env.tonumber = tonumber
env.tostring = tostring
env.type = type
env.getmetatable = getmetatable
env._VERSION = _VERSION
env.string = string
env.table = table
env.math = math
env.utf8 = utf8
env.coroutine = script_coroutine
env.os = { time = os.time, clock = os.clock, date = os.date, difftime = os.difftime }
env.json = json

env.setmetatable = function(t, mt)
  if type(mt) == 'table' and rawget(mt, '__gc') ~= nil then
    error('Scripts cannot use __gc', 2)
  end
  return setmetatable(t, mt)
end

env.pcall = function(fn, ...)
  return rethrow_kill(pcall(fn, ...))
end

env.xpcall = function(fn, handler, ...)
  return rethrow_kill(xpcall(fn, function(e)
    if is_kill(e) then
      return e
    end
    return handler(e)
  end, ...))
end

env.collectgarbage = function(option)
  if option == nil or option == 'collect' or option == 'count' or option == 'step' then
    return collectgarbage(option)
  end
  error('collectgarbage only takes collect, count or step here', 2)
end

env.load = function(chunk, name, _, chunk_env)
  if type(chunk) ~= 'string' and type(chunk) ~= 'function' then
    error('load needs text or a function', 2)
  end
  return load(chunk, name, 't', chunk_env or env)
end

env.require = function(name)
  if type(name) ~= 'string' or not find(name, '^[%w_%-]+[%w_%-%.]*$') then
    error('require needs a module name, such as utils or lib.strings', 2)
  end
  local found = loaded[name]
  if found ~= nil then
    return found
  end
  local path = gsub(name, '%.', '/') .. '.lua'
  local source = read_module(path)
  if source == nil then
    error('The script has no ' .. path, 2)
  end
  local fn, err = load(source, '@' .. path, 't', env)
  if not fn then
    error(err, 2)
  end
  local result = fn(name)
  if result == nil then
    result = true
  end
  loaded[name] = result
  return result
end

env.print = function(...)
  log('info', ...)
end

if with_java and java then
  env.java = java
end
G.java = nil
env._G = env

---------------------------------------------------------------------------------------------------
-- Items.

local Node = {}
Node.__index = Node
Node.__metatable = false

local function node(snapshot)
  if type(snapshot) ~= 'table' then
    return nil
  end
  return setmetatable(snapshot, Node)
end

local function nodes(list)
  local result = {}
  for i, snapshot in ipairs(list or {}) do
    result[i] = node(snapshot)
  end
  return result
end

local function query_of(query)
  if type(query) == 'string' then
    return { id = query }
  end
  return query or {}
end

local function find_nodes(query, limit, root)
  return call('screen.find', { query = query_of(query), limit = limit, root = root })
end

Node.__eq = function(a, b)
  return rawget(a, 'handle') == rawget(b, 'handle')
end

Node.__tostring = function(self)
  return self.text or self.contentDescription or self.id or self.className or 'item'
end

function Node:parent()
  return node(call('node.parent', { handle = self.handle }))
end

function Node:children()
  return nodes(call('node.children', { handle = self.handle }))
end

function Node:child(index)
  return self:children()[index]
end

function Node:refresh()
  return node(call('node.refresh', { handle = self.handle }))
end

function Node:find(query)
  return node(find_nodes(query, 1, self.handle)[1])
end

function Node:findAll(query, limit)
  return nodes(find_nodes(query, limit or 50, self.handle))
end

function Node:perform(action, args)
  return await(call('node.action', { handle = self.handle, action = action, args = args }), true)
end

function Node:setText(text)
  return self:perform('setText', { text = tostring(text) })
end

function Node:focus()
  return await(call('node.focus', { handle = self.handle }), true)
end

for _, action in ipairs({
  'click', 'longClick', 'scrollForward', 'scrollBackward', 'showOnScreen', 'expand', 'collapse',
  'dismiss',
}) do
  Node[action] = function(self)
    return self:perform(action)
  end
end

function Node:labeledBy()
  return node(call('node.labeledBy', { handle = self.handle }))
end

function Node:labelFor()
  return node(call('node.labelFor', { handle = self.handle }))
end

function Node:setSelection(start, finish)
  start = tonumber(start) or 0
  return self:perform('setSelection', { start = start, ['end'] = tonumber(finish) or start })
end

function Node:setProgress(value)
  return self:perform('setProgress', { value = tonumber(value) or 0 })
end

function Node:scrollTo(row, column)
  return self:perform('scrollToPosition', { row = tonumber(row) or 1, column = tonumber(column) or 1 })
end

-- Moves the cursor of an edit field, or reads on in other text, by a unit such as 'word'.
function Node:moveCursor(unit, direction, extend)
  local action = direction == 'previous' and 'previousAtGranularity' or 'nextAtGranularity'
  return self:perform(action, { unit = unit or 'character', extend = extend == true })
end

function Node:nextHtmlElement(element, direction)
  local action = direction == 'previous' and 'previousHtmlElement' or 'nextHtmlElement'
  return self:perform(action, { element = element or '' })
end

local function center(item)
  local b = item.bounds
  if not b or b.right <= b.left or b.bottom <= b.top then
    error('The item has no place on screen', 3)
  end
  return floor((b.left + b.right) / 2), floor((b.top + b.bottom) / 2)
end

local touch -- Set below.

function Node:tap()
  return touch.tap(center(self))
end

function Node:doubleTap()
  return touch.doubleTap(center(self))
end

function Node:longPress(ms)
  local x, y = center(self)
  return touch.longPress(x, y, ms)
end

function Node:recognizeText()
  return await(call('screen.recognizeText', { handle = self.handle }))
end

---------------------------------------------------------------------------------------------------
-- Hooks and commands.

local HOOKS = {
  'focus', 'speech', 'notification', 'announcement', 'windowChange', 'appEnter', 'appLeave',
  'settingChange', 'button', 'textChange', 'contentChange', 'event', 'actions', 'scroll',
  'selectionChange', 'click', 'longClick', 'inputFocus',
}
local handlers = {}
for _, name in ipairs(HOOKS) do
  handlers[name] = {}
end
local commands = {}

local function handler_name(name)
  return 'on' .. upper(sub(name, 1, 1)) .. sub(name, 2)
end

local function sync_hooks()
  local names, ids = {}, {}
  for _, name in ipairs(HOOKS) do
    if #handlers[name] > 0 then
      names[#names + 1] = name
    end
  end
  for id in pairs(commands) do
    ids[#ids + 1] = id
  end
  sort(ids)
  send('hooks', { names = names, commands = ids })
end

local function listen(name)
  return function(fn)
    if type(fn) ~= 'function' then
      error(handler_name(name) .. ' needs a function', 2)
    end
    local list = handlers[name]
    list[#list + 1] = fn
    sync_hooks()
    return function()
      for i, f in ipairs(list) do
        if f == fn then
          remove(list, i)
          sync_hooks()
          return
        end
      end
    end
  end
end

local declared_commands
local function on_command(id, fn)
  if type(fn) ~= 'function' then
    error('onCommand needs a function', 2)
  end
  if not declared_commands then
    declared_commands = {}
    for _, command in ipairs(call('manifest').commands or {}) do
      declared_commands[command.id] = true
    end
  end
  if not declared_commands[id] then
    error('The manifest declares no command ' .. tostring(id) .. ". Add it to the manifest's commands.", 2)
  end
  commands[id] = fn
  sync_hooks()
  return function()
    if commands[id] == fn then
      commands[id] = nil
      sync_hooks()
    end
  end
end

-- Calls a hook that must answer at once. It can't wait.
local function call_now(where, fn, ...)
  local r = pack(pcall(fn, ...))
  if not r[1] then
    if is_kill(r[2]) then
      error(r[2], 0)
    end
    report(where, describe(r[2]))
    return nil
  end
  return unpack(r, 2, r.n)
end

local function notify(name, ...)
  for _, fn in ipairs(handlers[name]) do
    spawn(handler_name(name), fn, ...)
  end
end

local function speech_of(result, text)
  if result == false then
    return ''
  end
  local kind = type(result)
  if kind == 'string' then
    return result
  elseif kind ~= 'table' then
    return text
  elseif result.silent then
    return ''
  elseif type(result.speak) == 'string' then
    return result.speak
  end
  local parts = {}
  for _, part in ipairs({ result.before, text, result.after }) do
    if type(part) == 'string' then
      parts[#parts + 1] = part
    end
  end
  return concat(parts, ' ')
end

local function rewrite(name, args_of, input)
  local text = input
  for _, fn in ipairs(handlers[name]) do
    text = speech_of(call_now(handler_name(name), fn, args_of(text)), text)
  end
  if text == input then
    return nil
  end
  return { text = text }
end

local function run_command(data)
  local fn = commands[data.id]
  if not fn then
    return { fallback = true }
  end
  local event = { id = data.id, via = data.via, direction = data.direction, node = node(data.node) }
  local ok, results = spawn('command ' .. data.id, fn, event)
  if ok and results and results[2] == false then
    return { fallback = true }
  end
  return nil
end

local MAX_OFFERS = 5
local offers = {}
local offer_order = {}
local next_offer = 1

local function offer_actions(data)
  local item = node(data.node)
  local offered, titles = {}, {}
  for _, fn in ipairs(handlers.actions) do
    local list = call_now('onActions', fn, item)
    if type(list) == 'table' then
      for _, action in ipairs(list) do
        local title = type(action) == 'table' and type(action.title) == 'string'
          and gsub(action.title, '^%s*(.-)%s*$', '%1') or ''
        if title ~= '' and type(action.run) == 'function' and not offered[title] then
          local run = action.run
          offered[title] = function()
            return run(item)
          end
          titles[#titles + 1] = title
        end
      end
    end
  end
  local offer = next_offer
  next_offer = next_offer + 1
  offers[offer] = offered
  offer_order[#offer_order + 1] = offer
  if #offer_order > MAX_OFFERS then
    offers[remove(offer_order, 1)] = nil
  end
  return { offer = offer, titles = titles }
end

local function run_action(data)
  local offered = offers[data.offer]
  local run = offered and offered[data.title]
  if run then
    spawn('action ' .. data.title, run)
  end
end

---------------------------------------------------------------------------------------------------
-- Timers.

local timers = {}
local next_timer = 1

local function add_timer(fn, ms, args, repeating)
  if type(fn) ~= 'function' then
    error('A timer needs a function', 3)
  end
  local id = next_timer
  next_timer = next_timer + 1
  local delay = math.max(repeating and 10 or 0, tonumber(ms) or 0)
  timers[id] = { fn = fn, args = args, delay = delay, repeating = repeating }
  send('timer.set', { id = id, ms = delay })
  return id
end

local function clear_timer(id)
  if timers[id] then
    timers[id] = nil
    send('timer.clear', { id = id })
  end
end

local function sleep(ms)
  if not can_wait() then
    error('sf.sleep waits, so call it from an event handler, a command, a timer or sf.run', 2)
  end
  local id = next_timer
  next_timer = next_timer + 1
  timers[id] = { sleep = true }
  send('timer.set', { id = id, ms = math.max(0, tonumber(ms) or 0) })
  co_yield(WAIT, 'timer:' .. id)
end

local function run_timer(data)
  local timer = timers[data.id]
  if not timer then
    return
  end
  if timer.sleep then
    timers[data.id] = nil
    wake('timer:' .. data.id, true)
    return
  end
  if timer.repeating then
    send('timer.set', { id = data.id, ms = timer.delay })
  else
    timers[data.id] = nil
  end
  spawn('timer', timer.fn, unpack(timer.args, 1, timer.args.n))
end

local function settle(data)
  if data.ok then
    wake(data.id, true, data.value)
  else
    wake(data.id, false, data.error)
  end
end

---------------------------------------------------------------------------------------------------
-- Dialogs.

local function text_or_nil(value)
  if value == nil then
    return nil
  end
  return tostring(value)
end

local function option_of(option)
  if type(option) == 'table' then
    return { value = tostring(option.value), label = tostring(option.label or option.value) }
  end
  return tostring(option)
end

local function options_of(list)
  local result = {}
  for i, option in ipairs(list or {}) do
    result[i] = option_of(option)
  end
  return result
end

local forms = {}
local next_form = 1

local function form_item(item, index)
  local sent = {}
  for k, v in pairs(item) do
    if k ~= 'onClick' and k ~= 'onChange' and type(v) ~= 'function' then
      sent[k] = v
    end
  end
  sent.id = tostring(item.id or ('item' .. index))
  sent.notify = type(item.onChange) == 'function'
  sent.submit = type(item.onSubmit) == 'function'
  if type(item.items) == 'table' then
    sent.items = options_of(item.items)
  end
  return sent
end

local function copy(t)
  local result = {}
  for k, v in pairs(t) do
    result[k] = v
  end
  return result
end

local function open_form(spec, full_screen)
  spec = spec or {}
  local id = next_form
  next_form = next_form + 1
  local items = spec.items or {}
  local sent, by_id, values = {}, {}, {}
  for i, item in ipairs(items) do
    local s = form_item(item, i)
    sent[i] = s
    by_id[s.id] = item
    if s.value ~= nil then
      values[s.id] = s.value
    end
  end
  local form = { closed = false }
  local added = 0
  function form:get(item)
    return values[item]
  end
  function form:values()
    return copy(values)
  end
  function form:set(item, props)
    if props.value ~= nil then
      values[item] = props.value
    end
    local sent_props = copy(props)
    if type(props.items) == 'table' then
      sent_props.items = options_of(props.items)
    end
    send('ui.update', { dialog = id, item = tostring(item), props = sent_props })
  end
  function form:close()
    send('ui.close', { dialog = id })
  end
  -- Adds an item at the end, or after the item with the id in opts.after.
  function form:add(item, opts)
    added = added + 1
    local s = form_item(item, 'added' .. added)
    by_id[s.id] = item
    if s.value ~= nil then
      values[s.id] = s.value
    end
    send('ui.add', { dialog = id, item = s, after = text_or_nil((opts or {}).after) })
    return s.id
  end
  function form:remove(item)
    item = tostring(item)
    by_id[item] = nil
    values[item] = nil
    send('ui.remove', { dialog = id, item = item })
  end
  function form:focus(item)
    send('ui.focus', { dialog = id, item = tostring(item) })
  end
  function form:setTitle(title)
    send('ui.setTitle', { dialog = id, title = text_or_nil(title) })
  end
  function form:wait()
    if self.closed then
      return copy(values)
    end
    return wait_for('form:' .. id)
  end
  forms[id] = { form = form, by_id = by_id, values = values, on_close = spec.onClose }
  send('ui.dialog', {
    dialog = id, title = text_or_nil(spec.title), items = sent, fullScreen = full_screen == true,
  })
  return form
end

local function form_event(data)
  local open = forms[data.dialog]
  if not open then
    return
  end
  for k, v in pairs(data.values or {}) do
    open.values[k] = v
  end
  if data.event == 'close' then
    forms[data.dialog] = nil
    open.form.closed = true
    if type(open.on_close) == 'function' then
      spawn('onClose', open.on_close, copy(open.values))
    end
    wake('form:' .. data.dialog, copy(open.values))
    return
  end
  local item = open.by_id[data.item]
  if not item then
    return
  end
  local handlers_of = {
    click = { item.onClick, open.form },
    change = { item.onChange, data.value, open.form },
    select = { item.onSelect, data.value, open.form },
    longPress = { item.onLongPress, data.value, open.form },
    submit = { item.onSubmit, data.value, open.form },
  }
  if data.event == 'action' and type(item.onAction) == 'function' and type(data.value) == 'table' then
    spawn('onAction ' .. data.item, item.onAction, data.value.action, data.value.value, open.form)
    return
  end
  local h = handlers_of[data.event]
  if h and type(h[1]) == 'function' then
    spawn(data.event .. ' ' .. data.item, h[1], unpack(h, 2, 3))
  end
end

local function dialog(method, args)
  return await(call(method, args))
end

local ui = {
  dialog = function(spec) return open_form(spec, false) end,
  window = function(spec) return open_form(spec, true) end,
  choose = function(title, options, opts)
    opts = opts or {}
    return dialog('ui.choose', {
      title = text_or_nil(title), options = options_of(options), selected = text_or_nil(opts.selected),
    })
  end,
  prompt = function(title, opts)
    opts = opts or {}
    return dialog('ui.prompt', {
      title = text_or_nil(title), text = tostring(opts.text or ''), hint = tostring(opts.hint or ''),
    })
  end,
  confirm = function(message, opts)
    opts = opts or {}
    return dialog('ui.confirm', {
      message = tostring(message), title = text_or_nil(opts.title), ok = text_or_nil(opts.ok),
      cancel = text_or_nil(opts.cancel),
    })
  end,
  alert = function(message, opts)
    opts = opts or {}
    dialog('ui.alert', { message = tostring(message), title = text_or_nil(opts.title) })
  end,
}

-- Shows text that can be read line by line and selected, and waits until it's closed.
function ui.text(title, text)
  local form = open_form({
    title = title,
    items = {
      { type = 'edit', id = 'text', value = tostring(text or ''), multiline = true, readOnly = true },
      { type = 'button', id = 'close', label = 'Close', close = true },
    },
  })
  form:wait()
end

---------------------------------------------------------------------------------------------------
-- Network.

local Response = {}
Response.__index = Response
Response.__metatable = false

function Response:json()
  return json.decode(self.body)
end

function Response:text()
  return self.body
end

local function fetch(url, opts)
  opts = opts or {}
  local headers = {}
  local has_type = false
  for k, v in pairs(opts.headers or {}) do
    headers[tostring(k)] = tostring(v)
    if lower(tostring(k)) == 'content-type' then
      has_type = true
    end
  end
  local body = opts.body
  if body ~= nil and type(body) ~= 'string' then
    body = json.encode(body)
    if not has_type then
      headers['Content-Type'] = 'application/json'
    end
  end
  local r = await(call('fetch', {
    url = tostring(url), method = opts.method or 'GET', headers = headers, body = body,
    timeout = opts.timeout,
  }))
  local lowered = {}
  for k, v in pairs(r.headers or {}) do
    lowered[lower(k)] = v
  end
  return setmetatable({
    status = r.status, statusText = r.statusText, url = r.url, headers = lowered, body = r.body,
    ok = r.status >= 200 and r.status < 300,
  }, Response)
end

---------------------------------------------------------------------------------------------------
-- Translations.

local function between(n, low, high)
  return n >= low and n <= high
end

local function slavic(n, few)
  if n % 10 == 1 and n % 100 ~= 11 then
    return 'one'
  elseif few(n % 10, n % 100) then
    return 'few'
  end
  return 'many'
end

local function slavic_few(d, h)
  return between(d, 2, 4) and not between(h, 12, 14)
end

local PLURAL_RULES = {
  { { 'ja', 'zh', 'ko', 'vi', 'th', 'id', 'ms', 'lo', 'my', 'km' }, function() return 'other' end },
  { { 'fr', 'pt', 'hy' }, function(n) return n < 2 and 'one' or 'other' end },
  { { 'ru', 'uk', 'be' }, function(n) return slavic(n, slavic_few) end },
  { { 'pl' }, function(n)
    if n == 1 then return 'one' end
    return slavic_few(n % 10, n % 100) and 'few' or 'many'
  end },
  { { 'cs', 'sk' }, function(n)
    if n == 1 then return 'one' end
    return between(n, 2, 4) and 'few' or 'other'
  end },
  { { 'hr', 'sr', 'bs' }, function(n)
    local category = slavic(n, slavic_few)
    return category == 'many' and 'other' or category
  end },
  { { 'lt' }, function(n)
    if between(n % 100, 11, 19) then return 'other' end
    if n % 10 == 1 then return 'one' end
    return n % 10 >= 2 and 'few' or 'other'
  end },
  { { 'lv' }, function(n)
    if n % 10 == 0 or between(n % 100, 11, 19) then return 'zero' end
    return n % 10 == 1 and 'one' or 'other'
  end },
  { { 'ro' }, function(n)
    if n == 1 then return 'one' end
    return (n == 0 or between(n % 100, 1, 19)) and 'few' or 'other'
  end },
  { { 'sl' }, function(n)
    return ({ [0] = 'other', 'one', 'two', 'few', 'few' })[n % 100] or 'other'
  end },
  { { 'he' }, function(n)
    if n == 1 then return 'one' end
    return n == 2 and 'two' or 'other'
  end },
  { { 'ar' }, function(n)
    if n == 0 then return 'zero' end
    if n == 1 then return 'one' end
    if n == 2 then return 'two' end
    if between(n % 100, 3, 10) then return 'few' end
    return between(n % 100, 11, 99) and 'many' or 'other'
  end },
}

local function plural_category(language, count)
  local n = math.abs(tonumber(count) or 0)
  if n ~= floor(n) then
    return 'other'
  end
  n = floor(n)
  for _, rule in ipairs(PLURAL_RULES) do
    for _, l in ipairs(rule[1]) do
      if l == language then
        return rule[2](n)
      end
    end
  end
  return n == 1 and 'one' or 'other'
end

local translations
local function i18n()
  if not translations then
    translations = call('i18n')
    translations.messages = translations.messages or {}
  end
  return translations
end

local function fill(text, params)
  params = params or {}
  return (gsub(tostring(text), '{(%w+)}', function(name)
    local value = params[name]
    if value == nil then
      return nil
    end
    return tostring(value)
  end))
end

local function translate(text, params)
  local value = i18n().messages[text]
  return fill(type(value) == 'string' and value ~= '' and value or text, params)
end

local function ngettext(singular, plural, count, params)
  local t = i18n()
  local forms_of = t.messages[singular]
  local form
  if type(forms_of) == 'table' then
    local language = gsub(t.language or '', '%-.*$', '')
    form = forms_of[plural_category(language, count)] or forms_of.other
  end
  local all = copy(params or {})
  all.count = count
  local english = tonumber(count) == 1 and singular or plural
  return fill(type(form) == 'string' and form ~= '' and form or english, all)
end

---------------------------------------------------------------------------------------------------
-- Sound.

local SAMPLE_RATE = 22050

local function play_audio(samples, opts)
  opts = opts or {}
  local list = {}
  for i, v in ipairs(samples) do
    list[i] = tonumber(v) or 0
  end
  send('audio', { samples = list, sampleRate = opts.sampleRate or SAMPLE_RATE, volume = opts.volume or 1 })
end

local function tone(frequency, ms)
  local length = floor(ms * SAMPLE_RATE / 1000 + 0.5)
  local fade = math.min(length / 2, SAMPLE_RATE / 200)
  local samples = {}
  for i = 0, length - 1 do
    local envelope = math.min(1, i / fade, (length - i) / fade)
    samples[i + 1] = math.sin(2 * math.pi * frequency * i / SAMPLE_RATE) * envelope
  end
  return samples
end

---------------------------------------------------------------------------------------------------
-- The sf table.

local function keyed(key, more)
  local args = more or {}
  args.key = tostring(key)
  return args
end

local function binding(method)
  return function(command, opts)
    opts = opts or {}
    send(method, { command = tostring(command), gesture = opts.gesture, keys = opts.keys })
  end
end

local system = {}
for _, name in ipairs({
  'back', 'home', 'recents', 'notifications', 'quickSettings', 'powerDialog', 'lockScreen',
  'takeScreenshot', 'allApps',
}) do
  system[name] = function()
    return await(call('system.action', { name = name }), true)
  end
end
function system.openApp(package_name)
  return await(call('system.openApp', { package = tostring(package_name) }), true)
end
function system.openUrl(url)
  return call('system.openUrl', { url = tostring(url) })
end
function system.share(text, opts)
  opts = opts or {}
  return call('system.share', {
    text = tostring(text), title = text_or_nil(opts.title), subject = text_or_nil(opts.subject),
  })
end
function system.openSettings(page, package_name)
  return call('system.openSettings', { page = page or 'settings', package = text_or_nil(package_name) })
end
function system.startActivity(intent)
  return call('system.startActivity', intent or {})
end
function system.sendBroadcast(intent)
  send('system.sendBroadcast', intent or {})
end
function system.apps()
  return call('system.apps')
end

local function point(x, y)
  x, y = tonumber(x), tonumber(y)
  if not x or not y then
    error('A point needs x and y', 3)
  end
  return { x = x, y = y }
end

local function gesture(strokes)
  return await(call('touch.gesture', { strokes = strokes }), true)
end

touch = {
  gesture = gesture,
  tap = function(x, y)
    return gesture({ { points = { point(x, y) }, duration = 50 } })
  end,
  doubleTap = function(x, y)
    local p = point(x, y)
    return gesture({ { points = { p }, duration = 50 }, { points = { p }, start = 150, duration = 50 } })
  end,
  longPress = function(x, y, ms)
    return gesture({ { points = { point(x, y) }, duration = tonumber(ms) or 1000 } })
  end,
  swipe = function(x1, y1, x2, y2, ms)
    return gesture({ { points = { point(x1, y1), point(x2, y2) }, duration = tonumber(ms) or 300 } })
  end,
}

local function key_list(keys)
  if type(keys) == 'table' then
    local list = {}
    for i, k in ipairs(keys) do
      list[i] = tostring(k)
    end
    return list
  end
  local list = {}
  for k in string.gmatch(tostring(keys), '%S+') do
    list[#list + 1] = k
  end
  return list
end

local function find_item(query)
  if getmetatable(query) == Node then
    return query
  end
  return node(find_nodes(query, 1)[1])
end

-- Waits until an item matching the query is on screen, and gives it, or nil after timeout ms.
local function wait_for(query, timeout, interval)
  if not can_wait() then
    error('sf.screen.waitFor waits, so call it from an event handler, a command, a timer or sf.run', 2)
  end
  local started = call('clock')
  timeout = tonumber(timeout) or 5000
  interval = math.max(100, tonumber(interval) or 250)
  while true do
    local found = node(find_nodes(query, 1)[1])
    if found then
      return found
    end
    if call('clock') - started >= timeout then
      return nil
    end
    sleep(interval)
  end
end

-- Clicks an item, or taps it where it has no click action. Gives whether that worked.
local function click_item(query)
  local item = find_item(query)
  if not item then
    return false
  end
  local clicked = item:click()
  if clicked == false then
    return item:tap()
  end
  return clicked ~= nil and clicked or true
end

local speech = {
  stop = function() send('speech.stop') end,
  isSpeaking = function() return call('speech.isSpeaking') end,
  last = function() return await(call('speech.last')) end,
}

local braille = {
  show = function(text) send('braille.show', { text = tostring(text) }) end,
}

local device = {
  info = function() return call('device.info') end,
  battery = function() return call('device.battery') end,
  network = function() return call('device.network') end,
  volume = function(stream) return call('device.volume', { stream = stream }) end,
  setVolume = function(stream, level)
    send('device.setVolume', { stream = stream, level = tonumber(level) or 0 })
  end,
  ringerMode = function() return call('device.ringerMode') end,
  setRingerMode = function(mode) send('device.setRingerMode', { mode = tostring(mode) }) end,
}

local sf_log = setmetatable({
  debug = function(...) log('debug', ...) end,
  info = function(...) log('info', ...) end,
  warn = function(...) log('warn', ...) end,
  error = function(...) log('error', ...) end,
}, {
  __call = function(_, ...) log('info', ...) end,
  __metatable = false,
})

local sf = {
  apiVersion = call('apiVersion'),
  manifest = function() return call('manifest') end,
  app = function() return call('app') end,
  locale = function() return call('locale') end,
  language = function() return i18n().language end,
  onCommand = on_command,
  _ = translate,
  ngettext = ngettext,
  log = sf_log,
  json = json,
  run = function(fn, ...)
    if type(fn) ~= 'function' then
      error('sf.run needs a function', 2)
    end
    spawn('sf.run', fn, ...)
  end,
  sleep = sleep,
  setTimeout = function(fn, ms, ...)
    return add_timer(fn, ms, pack(...), false)
  end,
  setInterval = function(fn, ms, ...)
    return add_timer(fn, ms, pack(...), true)
  end,
  clearTimer = clear_timer,
  speak = function(text, opts)
    opts = opts or {}
    local wait = opts.wait == true
    local result = call('speak', {
      text = tostring(text), interrupt = opts.interrupt == true, rate = opts.rate, pitch = opts.pitch,
      language = opts.language, wait = wait,
    })
    if wait then
      return await(result)
    end
  end,
  message = function(text, opts)
    opts = opts or {}
    text = tostring(text)
    send('speak', { text = text, interrupt = opts.interrupt ~= false })
    send('braille.show', { text = text })
  end,
  speech = speech,
  braille = braille,
  touch = touch,
  keys = {
    press = function(keys)
      return await(call('keys.press', { keys = key_list(keys) }), true)
    end,
    type = function(text)
      return await(call('keys.type', { text = tostring(text) }), true)
    end,
  },
  commands = {
    list = function() return call('commands.list') end,
    run = function(id) return await(call('commands.run', { id = tostring(id) }), true) end,
  },
  device = device,
  translate = function(text, opts)
    opts = opts or {}
    return await(call('translate', { text = tostring(text), from = text_or_nil(opts.from), to = text_or_nil(opts.to) }))
  end,
  readFile = function(path) return call('files.read', { path = tostring(path) }) end,
  clock = function() return call('clock') end,
  playAudio = play_audio,
  playTone = function(frequency, ms, opts)
    play_audio(tone(tonumber(frequency) or 440, tonumber(ms) or 100), { volume = (opts or {}).volume or 0.5 })
  end,
  playSound = function(name)
    return await(call('sound', { name = tostring(name) }), true)
  end,
  vibrate = function(pattern)
    if type(pattern) ~= 'table' then
      pattern = { tonumber(pattern) or 50 }
    end
    send('vibrate', { pattern = pattern })
  end,
  resume = function() send('resume') end,
  rules = {
    add = function(rule) return call('rules.add', { rule = rule }) end,
    remove = function(id) send('rules.remove', { id = id }) end,
    clear = function() send('rules.clear') end,
  },
  bind = binding('bindings.add'),
  unbind = binding('bindings.remove'),
  screen = {
    focused = function() return node(call('screen.focused')) end,
    inputFocused = function() return node(call('screen.inputFocused')) end,
    root = function() return node(call('screen.root')) end,
    find = function(query) return node(find_nodes(query, 1)[1]) end,
    findAll = function(query, limit) return nodes(find_nodes(query, limit or 50)) end,
    at = function(x, y) return node(call('screen.at', point(x, y))) end,
    windows = function()
      local list = call('screen.windows') or {}
      for _, window in ipairs(list) do
        window.root = node(window.root)
      end
      return list
    end,
    waitFor = wait_for,
    click = click_item,
    recognizeText = function() return await(call('screen.recognizeText', {})) end,
  },
  storage = {
    get = function(key, fallback)
      local stored = call('storage.get', keyed(key))
      if stored == nil or stored.value == nil then
        return fallback
      end
      return stored.value
    end,
    set = function(key, value) send('storage.set', keyed(key, { value = value })) end,
    remove = function(key) send('storage.remove', keyed(key)) end,
    keys = function() return call('storage.keys') end,
    clear = function() send('storage.clear') end,
  },
  settings = {
    get = function(key) return call('settings.get', keyed(key)) end,
    all = function() return call('settings.all') end,
    set = function(key, value) send('settings.set', keyed(key, { value = value })) end,
    setOptions = function(key, options) send('settings.setOptions', keyed(key, { options = options })) end,
  },
  clipboard = {
    get = function() return call('clipboard.get') end,
    set = function(text) send('clipboard.set', { text = tostring(text) }) end,
  },
  system = system,
  ui = ui,
  fetch = fetch,
}
for _, name in ipairs(HOOKS) do
  sf[handler_name(name)] = listen(name)
end
env.sf = sf

---------------------------------------------------------------------------------------------------
-- Entry points that SandFly calls. They are in the prelude's globals, which the script can't see.

local function with_info(name)
  return function(data)
    return rewrite(name, function(text) return data.arg, text end, data.text)
  end
end

local function with_node(name)
  return function(data)
    local event = copy(data)
    event.node = node(data.node)
    notify(name, event)
  end
end

local function plain(name)
  return function(data)
    notify(name, data)
  end
end

local dispatchers = {
  focus = function(data)
    return rewrite('focus', function(text) return node(data.arg), text end, data.text)
  end,
  speech = function(data)
    return rewrite('speech', function(text) return text end, data.text)
  end,
  notification = with_info('notification'),
  announcement = with_info('announcement'),
  textChange = with_node('textChange'),
  contentChange = with_node('contentChange'),
  scroll = with_node('scroll'),
  selectionChange = with_node('selectionChange'),
  click = with_node('click'),
  longClick = with_node('longClick'),
  inputFocus = with_node('inputFocus'),
  event = plain('event'),
  windowChange = plain('windowChange'),
  appEnter = plain('appEnter'),
  appLeave = plain('appLeave'),
  settingChange = function(data) notify('settingChange', data.key, data.value) end,
  button = function(data) notify('button', data.key) end,
  command = run_command,
  actions = offer_actions,
  dialogEvent = form_event,
  runAction = run_action,
  timer = run_timer,
  settle = settle,
}

function G.__sf_dispatch(text)
  local event = json.decode(text)
  local dispatcher = dispatchers[event.type]
  if not dispatcher then
    return nil
  end
  local result = dispatcher(event.data)
  if result == nil then
    return nil
  end
  return json.encode(result)
end

function G.__sf_main(source, name)
  if byte(source, 1) == 27 then
    error('The script must be Lua text, not compiled Lua', 0)
  end
  local fn, err = load(source, '@' .. name, 't', env)
  if not fn then
    error(err, 0)
  end
  local co = guard(co_create(fn))
  local r = raw_resume_checked(co)
  if not r[1] then
    error(traceback(co, describe(r[2])), 0)
  end
  -- The main file may wait, like any task, and goes on later.
  if co_status(co) == 'suspended' then
    if r[2] ~= WAIT then
      co_close(co)
      error('coroutine.yield was called outside a coroutine', 0)
    end
    park(r[3], co, name)
  end
end
