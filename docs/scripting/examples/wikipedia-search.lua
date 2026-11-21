local manifest = {
  id = 'wikipedia-search',
  name = 'Wikipedia',
  version = '2',
  author = 'SandFly',
  description = 'Searches English Wikipedia and opens the chosen article. Assign Search Wikipedia in Commands.',
  onDemand = true,
  minApiVersion = 6,
  testedApiVersion = 6,
  -- dialogs: the search field and the results. network: sf.fetch. system: sf.system.openUrl.
  -- speech: sf.speak.
  permissions = { 'dialogs', 'network', 'system', 'speech' },
  commands = {
    { id = 'search', title = 'Search Wikipedia' },
  },
}

local function encode(text)
  return (text:gsub('[^%w%-_.~]', function(char)
    return string.format('%%%02X', char:byte())
  end))
end

sf.onCommand('search', function()
  local query = sf.ui.prompt('Search Wikipedia', { hint = 'Search for' })
  if not query or query == '' then
    return
  end
  -- sf.fetch raises an error when there is no connection, so pcall catches it.
  local fetched, response = pcall(
    sf.fetch,
    'https://en.wikipedia.org/w/api.php?action=opensearch&limit=20&format=json&search=' .. encode(query),
    { headers = { ['User-Agent'] = 'SandFly-script/1.0' } }
  )
  if not fetched or not response.ok then
    sf.speak('Search failed')
    return
  end
  -- The response is { query, titles, descriptions, urls }.
  local results = response:json()
  local options = {}
  for i, title in ipairs(results[2]) do
    options[i] = { value = results[4][i], label = title }
  end
  if #options == 0 then
    sf.speak('No results')
    return
  end
  local url = sf.ui.choose('Results', options)
  if url then
    sf.system.openUrl(url)
  end
end)
