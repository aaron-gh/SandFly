local manifest = {
  id = 'notepad',
  name = 'Notepad',
  version = '1',
  author = 'SandFly',
  description = 'Notes kept in SandFly, opened from the SandFly menu.',
  minApiVersion = 2,
  testedApiVersion = 2,
  permissions = { 'dialogs', 'input', 'clipboard', 'speech' },
  commands = {
    { id = 'open', title = 'Notepad', menu = true, keys = 'sf+n' },
  },
}

-- Notes are { id, text, time }, newest first. sf.storage keeps them when the script unloads.
local function load_notes()
  return sf.storage.get('notes', {})
end

local function save_notes(notes)
  sf.storage.set('notes', notes)
end

local function title_of(note)
  local first = note.text:match('^%s*([^\n]+)') or ''
  if first == '' then
    first = 'Empty note'
  end
  return first .. ', ' .. os.date('%d %b %H:%M', note.time)
end

local function rows(notes)
  local list = {}
  for i, note in ipairs(notes) do
    list[i] = { value = tostring(note.id), label = title_of(note) }
  end
  return list
end

local function find(notes, id)
  for i, note in ipairs(notes) do
    if tostring(note.id) == tostring(id) then
      return note, i
    end
  end
end

local function refresh(list_window)
  list_window:set('notes', { items = rows(load_notes()) })
end

-- Opens a note in a window of its own, over the list. Closing it saves the text.
local function edit(id, list_window)
  local notes = load_notes()
  local note = find(notes, id)
  if not note then
    note = { id = os.time() * 1000 + math.random(999), text = '', time = os.time() }
    table.insert(notes, 1, note)
    save_notes(notes)
  end
  sf.ui.window({
    title = 'Note',
    items = {
      { type = 'edit', id = 'text', label = 'Text', value = note.text, multiline = true },
      {
        type = 'button',
        id = 'delete',
        label = 'Delete note',
        onClick = function(form)
          if sf.ui.confirm('Delete this note?', { ok = 'Delete' }) then
            local current = load_notes()
            local _, index = find(current, note.id)
            if index then
              table.remove(current, index)
              save_notes(current)
            end
            note = nil
            form:close()
          end
        end,
      },
    },
    onClose = function(values)
      if note then
        local current = load_notes()
        local saved, index = find(current, note.id)
        if saved and values.text ~= saved.text then
          saved.text = values.text
          saved.time = os.time()
          table.remove(current, index)
          table.insert(current, 1, saved)
          save_notes(current)
        end
      end
      refresh(list_window)
      list_window:focus('notes')
    end,
  })
end

local function open_notepad()
  local list_window
  list_window = sf.ui.window({
    title = 'Notepad',
    items = {
      {
        type = 'button',
        id = 'new',
        label = 'New note',
        onClick = function()
          edit(nil, list_window)
        end,
      },
      { type = 'heading', id = 'heading', label = 'Notes' },
      {
        type = 'items',
        id = 'notes',
        items = rows(load_notes()),
        empty = 'No notes',
        actions = { 'Copy', 'Delete' },
        onSelect = function(id)
          edit(id, list_window)
        end,
        onAction = function(action, id, form)
          local notes = load_notes()
          local note, index = find(notes, id)
          if not note then
            return
          end
          if action == 'Copy' then
            sf.clipboard.set(note.text)
            sf.message('Copied')
          elseif action == 'Delete' and sf.ui.confirm('Delete "' .. title_of(note) .. '"?', { ok = 'Delete' }) then
            table.remove(notes, index)
            save_notes(notes)
            refresh(form)
          end
        end,
      },
    },
  })
end

sf.onCommand('open', open_notepad)
