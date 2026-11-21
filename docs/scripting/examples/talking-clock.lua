local manifest = {
  id = 'talking-clock',
  name = 'Talking clock',
  version = '1',
  author = 'SandFly',
  description = 'Says the time every hour or half hour, and with the Say the time command. Turn on Keep running with the screen off to hear it while the screen is off.',
  minApiVersion = 6,
  testedApiVersion = 6,
  -- The user can keep the script running with the screen off, on its page.
  keepLoaded = true,
  -- speech: sf.speak.
  permissions = { 'speech' },
  settings = {
    {
      key = 'every',
      type = 'list',
      title = 'Say the time',
      options = {
        { value = '60', label = 'Every hour' },
        { value = '30', label = 'Every half hour' },
        { value = '0', label = 'Only with the command' },
      },
    },
    { key = 'vibrate', type = 'switch', title = 'Vibrate before the time', default = false },
  },
  -- No gesture or keys: the user assigns Say the time in Commands, so no input permission.
  commands = {
    { id = 'time', title = 'Say the time' },
  },
}

local function say_time()
  if sf.settings.get('vibrate') then
    sf.vibrate(100)
  end
  sf.speak(os.date('%H:%M'))
end

local timer

-- Sets a timer for the next full hour or half hour. Timers end when the script unloads, and the
-- script sets a new one each time it loads.
local function schedule()
  if timer then
    sf.clearTimer(timer)
    timer = nil
  end
  local every = tonumber(sf.settings.get('every')) or 0
  if every <= 0 then
    return
  end
  local now = os.date('*t')
  local seconds = (every - now.min % every) * 60 - now.sec
  timer = sf.setTimeout(function()
    timer = nil
    say_time()
    schedule()
  end, seconds * 1000)
end

sf.onSettingChange(function(key)
  if key == 'every' then
    schedule()
  end
end)

sf.onCommand('time', say_time)

schedule()
