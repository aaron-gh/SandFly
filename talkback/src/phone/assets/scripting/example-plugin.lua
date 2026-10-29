local manifest = {
  id = 'example-plugin',
  name = 'Example plugin',
  version = '1.0',
  author = 'SandFly',
  homepage = 'https://github.com/aaron-gh/SandFly/blob/master/docs/scripting/examples/example-plugin.lua',
  minApiVersion = 1,
  testedApiVersion = 2,
  description = 'Says the name of each app you switch to, and counts how often you open each one.',
  -- screen: a script for all apps is told which app is in front. speech: sf.speak.
  permissions = { 'screen', 'speech' },
  settings = {
    {
      key = 'sayApp',
      type = 'switch',
      title = 'Say the app when you switch apps',
      default = true,
    },
    {
      key = 'sayCount',
      type = 'switch',
      title = 'Also say how often you opened it',
      default = false,
    },
  },
}

sf.onAppEnter(function(app)
  if not app.package then
    return
  end
  local counts = sf.storage.get('counts', {})
  counts[app.package] = (counts[app.package] or 0) + 1
  sf.storage.set('counts', counts)

  if not sf.settings.get('sayApp') then
    return
  end
  local name = app.package:match('([^.]+)$')
  local opened = counts[app.package]
  local count = ''
  if sf.settings.get('sayCount') then
    count = ', opened ' .. opened .. (opened == 1 and ' time' or ' times')
  end
  sf.speak(name .. count)
end)

print('Example plugin loaded')
