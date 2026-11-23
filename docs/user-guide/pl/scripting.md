# Pisanie skryptów SandFly

Ten dodatek opisuje API skryptów w wersji 6. Skrypt to program w Lua, który zmienia zachowanie SandFly w wybranej aplikacji, w całym systemie lub dodaje własne polecenia. Instalację i tworzenie skryptu bez kodu opisuje rozdział „Skrypty i własne etykiety”.

## Możliwości {#overview}

Skrypty działają na telefonach i tabletach. Zegarki i telewizory ich nie obsługują.

### Co może zrobić skrypt {#what-a-script-can-do}

- [Reguły](#rules) mogą nazwać lub ukryć element, przeczytać wiersz jako całość lub tylko jego wybrane części, zmienić kolejność albo dodać czynności.
- [Obsługa mowy](#speech-hooks) pozwala zmienić wypowiedź dotyczącą elementu, powiadomienia lub komunikatu oraz całą mowę.
- [Procedury obsługi zdarzeń](#hooks) reagują między innymi na otwarcie aplikacji, zmianę fokusu, edycję tekstu, przewinięcie i powiadomienie.
- [Polecenia](#commands) mogą działać z gestu, klawiatury, menu i sterowania czytaniem. [Opcje nawigacji](#navigation) przechodzą po wybranych elementach.
- Kod może czytać ekran, klikać, przewijać, pisać, naciskać klawisze i rozpoznawać tekst w wybranym obszarze.
- Skrypt może mówić, odtwarzać dźwięki, wibrować i wyświetlać tekst w brajlu.
- Może wyświetlać okna, zapisywać dane i ustawienia, pobierać treść, tłumaczyć, używać makr i poleceń, otwierać aplikacje lub adresy i zmieniać ustawienia urządzenia.

### Ograniczenia skryptu {#what-a-script-cant-do}

Obowiązują [uprawnienia z manifestu](#permissions), które użytkownik widzi przy instalacji i może wyłączyć. Skrypt nie zmienia [ekranów SandFly](#sandfly-screens). Ma dostęp do odczytu plików własnego pakietu. Funkcje `io`, `os.execute`, `debug` i `package` są niedostępne. Dostęp do Javy i Androida wymaga [uprawnienia java](#java).

Każde wywołanie skryptu ma [limit 2 sekund](#limits). Skrypty nie działają po wstrzymaniu SandFly ani gdy użytkownik włączy **Wyłącz wszystkie skrypty**. [Przy wygaszonym ekranie](#with-the-screen-off) potrzebna jest osobna zgoda.

### Wersje API {#api-versions}

`sf.apiVersion` zwraca wersję API w danej kompilacji SandFly. Pozycje dodane po wersji 1 mają adnotację o wersji. W `minApiVersion` podaj najwyższą z wersji wymaganych przez używane elementy. Wersje od 2 do 6 dodają funkcje i zachowują działanie istniejących.

## Pierwszy skrypt {#write-your-first-script}

Przykład wita użytkownika po otwarciu ustawień Androida. Dodaje też polecenie, które podaje sposób wywołania.

```lua
local manifest = {
  id = 'example.hello',
  name = 'Hello',
  author = 'Your name',
  version = '1',
  minApiVersion = 6,
  testedApiVersion = 6,
  apps = { 'com.android.settings' },
  permissions = { 'speech' },
  commands = {
    { id = 'hello', title = 'Say hello' },
  },
}

sf.onAppEnter(function(app)
  sf.speak('Hello from a script')
  print('Started in', app.package)
end)

sf.onCommand('hello', function(event)
  sf.speak('Hello, run from ' .. event.via)
end)
```

Tabela `manifest` opisuje skrypt, jego zasięg i uprawnienia. SandFly odczytuje ją bez wykonywania kodu. Reszta uruchamia się przy każdym załadowaniu. Dodaje obsługę `onAppEnter` i polecenia `hello`.

1. Zapisz przykład jako `hello.lua` i przenieś do pamięci telefonu.
2. Otwórz **Skrypty** > **Importuj skrypt**. Wybierz `hello.lua`, sprawdź dane i wybierz **Zainstaluj**.
3. Otwórz ustawienia Androida. SandFly wypowie tekst z przykładu.
4. Otwórz **Skrypty** > **Hello** > **Dziennik**. Znajdziesz tam wpis `Started in com.android.settings`.
5. Otwórz **Polecenia** > **Skrypty** i przypisz gest do **Hello: Say hello**. Wróć do ustawień Androida i wykonaj gest. Odczyt będzie zawierał informację o sposobie uruchomienia.

Żeby zaktualizować skrypt, zmień plik, zwiększ `version` i zaimportuj ponownie. Ten sam `id` zastąpi poprzednią wersję. Dane, ustawienia i wybory uprawnień zostaną zachowane. Zobacz też [diagnozowanie](#testing-and-debugging) i [przykładowe skrypty](#example-scripts).

## Pliki skryptów {#script-files}

- Plik `.lua` do 1 MB jest plikiem `main.lua` skryptu.
- Archiwum `.zip` może zawierać `main.lua`, kolejne pliki Lua, dźwięki, makra, dane i przekłady. Limit wynosi 5 MB archiwum, 5 MB po rozpakowaniu i 200 plików. `main.lua` może być na najwyższym poziomie albo w pojedynczym wspólnym katalogu. Wpisy zaczynające się od kropki i katalog `__MACOSX` są pomijane.

Do plików w archiwum można sięgnąć przez [require i sf.readFile](#lua-and-files), [sf.playSound](#speech-sound-and-vibration) oraz [sf.macros.run](#macros). [Tłumaczenia](#translations) znajdują się w `locales`.

Instalację rozpocznij przez **Skrypty** > **Importuj skrypt** albo udostępnij plik lub jego adres HTTPS przez **Zainstaluj skrypt SandFly**. Przed instalacją SandFly pokaże źródło, podpis, uprawnienia i polecenia.

### Środowisko Lua {#the-lua-environment}

SandFly wykonuje tekst [Lua 5.4](https://www.lua.org/manual/5.4/) przez [LuaJava](https://github.com/gudzpoz/luajava). Dostępne są:

- Funkcje `assert`, `error`, `ipairs`, `next`, `pairs`, `pcall`, `xpcall`, `print`, `rawequal`, `rawget`, `rawlen`, `rawset`, `select`, `tonumber`, `tostring`, `type`, `getmetatable`, `setmetatable`, `load`, `require`, `collectgarbage`, `_G`, `_VERSION`.
- Biblioteki `string`, `table`, `math`, `utf8`, `coroutine`, `json` oraz funkcje `os.time`, `os.clock`, `os.date` i `os.difftime`.
- Moduł `sf` opisany w tym dodatku.

`load` przyjmuje tekst lub funkcję i używa środowiska skryptu, chyba że wskażesz inne. `setmetatable` odrzuca metatabele z `__gc`. Do `collectgarbage` można przekazać `'collect'`, `'count'` i `'step'`. `string.dump` jest niedostępne. `string.rep` nie tworzy tekstu dłuższego niż 8 MB. `pcall` i `xpcall` nie przechwytują błędów przekroczenia czasu lub pamięci. Korutynę, która [czeka na wynik](#waiting), wznawiaj wewnątrz zadania.

Pozostałe funkcje `os` oraz `io`, `debug`, `package`, `dofile` i `loadfile` są niedostępne.

## Manifest {#the-manifest}

Umieść `local manifest = { ... }` przed kodem w `main.lua`. SandFly odczyta wartości tekstowe, liczby, `true`, `false`, `nil` i tabele. Komentarze są dozwolone. Zmienne, wywołania funkcji i operator `..` są niedozwolone.

Tabela z wartościami jest listą, na przykład `{ 'speech', 'screen' }`. Tabela z polami o nazwach jest rekordem, na przykład `{ id = 'x' }`. Nie mieszaj tych postaci. `{}` oznacza pustą listę. W kodzie możesz odczytać manifest przez `sf.manifest()`.

| Pole | Typ | Zastosowanie |
| --- | --- | --- |
| `id` | Tekst | Wymagany. Do 64 znaków z małych liter, cyfr, kropek, myślników i podkreśleń. Pierwszy znak to litera albo cyfra. Skrypt o tym samym ID zastępuje poprzedni. |
| `name` | Tekst | Wymagany. Widoczny na liście skryptów i na początku tytułu okien skryptu. |
| `author` | Tekst | Wymagany. |
| `minApiVersion` | Liczba | Wymagana najstarsza [wersja API](#api-versions). |
| `testedApiVersion` | Liczba | Najwyższa sprawdzona wersja. Wymagana, nie niższa od `minApiVersion`. SandFly może ostrzec o zmianie wywołań po podanej wersji. Między wersjami 1 i 6 takie zmiany nie występują. |
| `version` | Tekst | Wersja skryptu, domyślnie `'1'`. Porównywana po częściach liczbowych, więc `'1.10'` jest nowsza od `'1.9'`. Zwiększ przy [publikacji zmian](#publish-a-script). |
| `description` | Tekst | Opis na stronie skryptu i przy instalacji. |
| `homepage` | Tekst | Adres HTTP lub HTTPS dla **Otwórz stronę domową**. |
| `apps` | Tekst lub lista | [Zakres działania](#where-a-script-runs). Brak pola obejmuje wszystkie aplikacje. |
| `permissions` | Lista | [Wymagane uprawnienia](#permissions). |
| `settings` | Lista | [Ustawienia](#settings) na stronie skryptu. |
| `rules` | Lista | [Reguły](#rules) zmieniające elementy. |
| `commands` | Lista | [Polecenia](#commands) dla użytkownika. |
| `navigation` | Lista | [Sposoby nawigacji](#navigation). |
| `onDemand` | Wartość logiczna | Od wersji 4. [Ładuje skrypt](#scripts-loaded-on-demand) wyłącznie do wykonania polecenia. |
| `keepLoaded` | Wartość logiczna | Od wersji 5. Udostępnia [działanie przy wygaszonym ekranie](#with-the-screen-off). |

SandFly ignoruje inne pola najwyższego poziomu. Nieznane pola reguł, zapytań, czynności lub nawigacji powodują odrzucenie manifestu. SandFly poda nazwę pola. Jeśli `minApiVersion` jest za wysokie, nadal możesz zainstalować skrypt, ale SandFly wyświetli informację o zgodności przy instalacji, na stronie i w dzienniku.

### Gdzie działa skrypt {#where-a-script-runs}

`apps` przyjmuje nazwę pakietu lub listę nazw i rekordów:

```lua
apps = {
  'com.example.mail',
  { package = 'com.example.chat', activity = '.ConversationActivity' },
  { package = 'com.example.chat', window = 'Settings' },
},
```

`activity` porównuje nazwę klasy aktywności lub jej zakończenie. SandFly dowiaduje się o aktywności przy zmianie okna. Przed wykryciem, na przykład gdy SandFly włączył się w środku aplikacji, zakres obejmuje jej wszystkie ekrany. `window` porównuje fragment tytułu bez względu na wielkość liter.

Skrypt aplikacji ładuje się, gdy ta pojawia się na pierwszym planie, i zostaje usunięty z pamięci po wyjściu. Za każdym razem startuje od nowa. Żeby zachować wartości, użyj [sf.storage](#storage). Jeśli nie podasz aktywności ani okna, pasuje również inne okno tego pakietu, w którym jest fokus SandFly, na przykład okno klawiatury.

Skrypt bez `apps` pozostaje załadowany we wszystkich aplikacjach, chyba że [ładuje się na żądanie](#scripts-loaded-on-demand). Wstrzymanie SandFly, wyłączenie skryptów i wygaszenie ekranu kończą pracę. Wyjątek dla wygaszenia jest opisany poniżej. Przy usunięciu skryptu z pamięci SandFly zamyka związane z nim okna, liczniki i żądania.

### Ekrany SandFly {#sandfly-screens}

Gdy widać ekran SandFly, czytnik pomija reguły, przechwytywanie mowy, skryptowe czynności, gesty, klawisze, menu i opcje czytania. Skrypt nie dostaje zdarzeń z SandFly. Przez to ekran **Skrypty** pozostaje dostępny nawet wtedy, gdy skrypt utrudnia obsługę programu.

Wywołania wymagające `screen`, `actions`, `system` lub `dialogs`, a także `sf.commands.run`, `sf.macros.run`, `sf.feedback.mute` i `sf.touch.lock` zwracają błąd. Te błędy nie wliczają się do limitu wyłączenia skryptu. `sf.speak` nie przerywa mowy, a `sf.speech.stop` jej nie zatrzymuje.

Nie można wskazać SandFly w `apps`. Skrypt globalny pozostaje załadowany, więc działają jego ustawienia. Uprawnienie `java` daje pełny dostęp i wykracza poza te ograniczenia.

### Działanie przy wygaszonym ekranie {#with-the-screen-off}

Od wersji 5 globalny skrypt bez `onDemand` może zadeklarować `keepLoaded = true`. Użytkownik musi włączyć **Działaj przy wyłączonym ekranie** na stronie skryptu. Skrypt sam nie może tego ustawić.

Przy wygaszonym ekranie nadal działają liczniki, żądania i `sf.speak`. Zdarzenia ekranu, takie jak `onFocus`, nie są obsługiwane. Liczniki nie budzą telefonu i mogą wykonać się później.

### Ładowanie na żądanie {#scripts-loaded-on-demand}

Od wersji 4 `onDemand = true` ładuje skrypt przy użyciu jego polecenia. SandFly usuwa go z pamięci mniej więcej 2 sekundy po zamknięciu okien, ustaniu oczekiwania, zakończeniu liczników i wykorzystaniu [obiektów java.proxy](#java).

Zmienne nie przechodzą między załadowaniami. Użyj [sf.storage](#storage), aby zapisać wartości. Obsługa zdarzeń działa w trakcie załadowania. Skrypt musi mieć co najmniej jedno polecenie i w każdym podaje tylko `id` oraz `title`. Użytkownik przypisze polecenie w **Polecenia**. Nie dodawaj `apps`, `rules`, `navigation` ani ustawień typu przycisk. `sf.bind` i `sf.unbind` są niedostępne.

Zobacz [przykład wyszukiwania w Wikipedii](scripting/examples/wikipedia-search.lua).

### Uprawnienia {#permissions}

Manifest określa dostęp. Instalacja nadaje uprawnienia po pokazaniu listy. Aktualizacja zachowuje poprzednie odmowy i dodaje nowe po potwierdzeniu. Każde uprawnienie można wyłączyć na stronie skryptu.

Wywołanie bez zgody zgłasza błąd z nazwą uprawnienia. Procedura, reguła, czynność lub przypisanie wymagające dostępu są pomijane i pozostawiają jedno ostrzeżenie w dzienniku.

| Uprawnienie | Dostęp |
| --- | --- |
| `screen` | Czytanie ekranu, w tym `sf.screen`, odczyty [elementu](#items), `sf.speech.last()` i rozpoznawanie tekstu. Elementy przekazywane do zdarzeń i poleceń mają bez niego wartość `nil`. Wymagają go `onFocus`, `onAnnouncement`, `onTextChange`, `onContentChange`, `onScroll`, `onSelectionChange`, `onClick`, `onLongClick`, `onInputFocus` i czynności odczytujące tekst elementu. W skrypcie globalnym obejmuje również `sf.app()`, `onAppEnter`, `onAppLeave` i `onWindowChange`. |
| `actions` | Czynności elementu, zmiana fokusu i tekstu, dotyk, `sf.screen.click`, `sf.touch`, `sf.keys.type`, `sf.keys.press`, `buttonActions` i czynności reguł poza odczytem. |
| `speech` | `sf.speak`, `sf.message`, `sf.speech.stop`, `sf.braille.show`, `sf.feedback.mute`, reguły modyfikujące element, `sf.rules` i zmienianie wypowiedzi, w tym `onSpeech`. |
| `passwords` | Tekst pól haseł w elementach i zdarzeniach. Bez zgody ma wartość `nil`. |
| `input` | Przypisania `gesture`, `keys`, `menu`, `control`, `sf.bind`, `sf.unbind`, `navigation`, `sf.commands.run`, `sf.keys.passAll`, `onActions` i czynności w regułach. Od wersji 6 polecenia bez przypisań go nie potrzebują. |
| `network` | `sf.fetch` i `sf.translate`. |
| `clipboard` | `sf.clipboard`. |
| `system` | `sf.system`, `sf.resume`, `sf.device.setVolume`, `sf.device.setRingerMode`, `sf.device.set` i `sf.device.media`. |
| `notifications` | `onNotification` z powiadomieniami wszystkich aplikacji. |
| `events` | `onEvent` ze zdarzeniami dostępności wszystkich aplikacji, również wpisywaniem. |
| `dialogs` | `sf.ui`. |
| `java` | Moduł [Java](#java) z LuaJava. |

Bez uprawnień można odtwarzać dźwięki, tony i wibracje, używać `sf.speech.isSpeaking`, odczytywać informacje o urządzeniu oraz używać danych, ustawień, liczników, `sf.text`, `sf.readFile`, `require`, `sf.commands.list`, dziennika, JSON i lokalizacji. `sf.app()` nie potrzebuje dodatkowej zgody w skrypcie z `apps`.

SandFly oznacza `passwords`, `network`, `clipboard`, `system`, `notifications`, `events`, `dialogs` i `java` jako uprawnienia dla zaufanych skryptów. `navigation` i przypisania poleceń w manifeście wymagają `input`.

### Java {#java}

Uprawnienie `java` włącza [moduł LuaJava](https://luajava.iroiro.party/api.html), między innymi `java.import`, `java.new` i `java.proxy`. Kod może wykonywać wywołania Javy z dostępem SandFly w każdej aplikacji. Pozostałe uprawnienia skryptu nie ograniczają wywołań Javy. Potrzebujesz go na przykład do operacji niedostępnych w `sf`, jak udostępnienie pliku innej aplikacji.

```lua
local Build = java.import('android.os.Build')
sf.speak('This is a ' .. Build.MODEL)
```

Gdy skrypt działa, jego stan Lua jest zajęty. Wywołanie metody `java.proxy` z innego wątku poczeka na zakończenie bieżącego wywołania. Skrypt nie powinien czekać synchronicznie na czynność, która potrzebuje wątku głównego SandFly. Po usunięciu skryptu z pamięci metoda obiektu zwraca `nil`, 0 lub `false`. Wcześniej wyrejestruj obiekty w Androidzie, na przykład w `onAppLeave`.

W skrypcie [ładowanym na żądanie](#scripts-loaded-on-demand) taki obiekt utrzymuje skrypt w pamięci do końca pierwszego wywołania albo przez 30 sekund. Limit to 100 obiektów. Trwale zarejestrowany słuchacz potrzebuje skryptu bez `onDemand`. Tabela lub funkcja przekazana bezpośrednio do metody nie jest liczona do limitu. Gdy potrzebujesz utrzymać skrypt na czas wywołania, użyj `java.proxy`.

## Reguły {#rules}

Reguły zmieniają elementy bez wywoływania kodu. Działają także wtedy, gdy skrypt ma inne zajęcie. Zmiany elementu wymagają `speech`, a czynności `input`.

```lua
rules = {
  { id = 'send_button', label = 'Send' },
  { match = { className = 'ImageView', parentId = 'toolbar' }, hide = true },
  { id = 'row', group = true, hint = 'Double-tap to open' },
},
```

Wskaż element przez `id`, [zapytanie](#queries) w `match` lub oba pola. `id` może mieć formę krótką, na przykład `send_button`, albo pełną, na przykład `com.example:id/send_button`. `window` zawęża regułę do fragmentu tytułu, bez względu na wielkość liter. `activity` dopasowuje końcówkę nazwy klasy aktywności. Przed wykryciem aktywności reguła nadal może działać, tak jak przy [zasięgu skryptu](#where-a-script-runs).

Reguła musi zawierać przynajmniej jedną zmianę:

| Pole | Wartość | Działanie |
| --- | --- | --- |
| `label` | Tekst | Zastępuje nazwę elementu odczytywaną z aplikacji. |
| `speak` | Tekst | Zastępuje całą wypowiedź o elemencie, łącznie z nazwą, rolą i stanem. |
| `hide` | `true` lub `'all'` | Pomija element w nawigacji. `'all'` pomija też zawartość. Skrypt może nadal go kliknąć. |
| `silence` | `true` lub `'all'` | Wycisza zmiany, zaznaczenia i komunikaty, na przykład zmianę postępu. `'all'` obejmuje też zawartość. Ustawienie fokusu nadal czyta element. |
| `role` | Tekst | Zmienia rolę, na przykład na `'button'`. Nieznana wartość powoduje błąd z listą ról. |
| `state`, `hint` | Tekst | Zmienia stan lub podpowiedź użytkowania. |
| `heading` | Wartość logiczna | Określa, czy element jest nagłówkiem. |
| `group` | `true` | Czyta element i jego zawartość razem. Jeśli element ma własny opis lub tekst, używa go bez tekstów dzieci. |
| `readBefore`, `readAfter` | ID lub zapytanie | Przesuwa element przed innym albo za nim. Użyj jednego pola. |
| `activate` | Zapytanie | Wskazuje element wewnętrzny, który uaktywni podwójne stuknięcie, na przykład część wiersza otwierającą wpis. |
| `liftToActivate` | `true` | Uaktywnia element po uniesieniu palca, niezależnie od ogólnych ustawień. |
| `longPressAfter` | Liczba | Przy `liftToActivate` ustala od 100 do 10 000 ms, po których następuje przytrzymanie. Uniesienie już niczego nie uruchamia. Elementy bez długiego kliknięcia zachowują poprzednią obsługę. |
| `touchToActivate` | `true` | Dotyka elementu na ekranie, zamiast wysyłać kliknięcie dostępności. Wymaga Androida 11. W starszych wersjach działa zwykłe kliknięcie. |
| `read` | Lista | Od wersji 6. Wybiera do 12 części do odczytu, oddzielonych przecinkami. |
| `actions` | Lista | Do dziesięciu [czynności](#actions-in-rules) w menu elementu. |
| `buttonActions` | `true` | Od wersji 6. Dodaje do menu do 20 przycisków, do 8 poziomów w głąb. Nazwą jest opis, tekst lub pierwszy tekst wewnątrz. Przycisk zagnieżdżony w innym pomija się. Wymaga `actions` i `input`. |
| `skipButtons` | Lista tekstów | Od wersji 6. `buttonActions` pomija przyciski z podanym fragmentem nazwy. Do 20 wpisów, bez rozróżniania wielkości liter. |

Część `read` może wskazać ścieżkę `{ path = { 2, 1 } }` widocznych dzieci w kolejności czytania. Każdy numer liczy się od 1, a ścieżka ma do 12 pozycji. Tę samą kolejność zobaczysz w **Elementy wewnątrz**. Możesz też podać `{ from = query }`, żeby odczytać pierwszy pasujący element wewnątrz. `{ from = {} }` czyta tekst samego elementu. `before` dodaje słowa przed częścią. `fromAbove = true` przy braku tekstu sięga do najbliższego pasującego wpisu spośród czterech nad nim na tej samej liście, na przykład po nadawcę wiadomości.

```lua
{ id = 'message_row', read = {
  { from = 'sender', fromAbove = true },
  { from = 'body' },
  { from = 'time', before = 'sent' },
} }
```

Jeśli pasuje kilka reguł, dla danego pola liczy się pierwsza, która je ustawia. `group`, `liftToActivate` i `touchToActivate` obowiązują, jeśli ustawia je choć jedna. Najpierw stosowane są skrypty aplikacji, potem globalne. Kolejność zmienisz przez **Uruchamiaj wcześniej** i **Uruchamiaj później**. W ramach skryptu `sf.rules.add` ma pierwszeństwo przed manifestem.

Manifest mieści do 500 reguł, w tym maksymalnie 100 z `match`.

### Czynności w regułach {#actions-in-rules}

```lua
{
  id = 'message_row',
  actions = {
    { title = 'Archive', click = 'archive_button' },
    { title = 'Read time', speak = { textOf = 'timestamp' } },
  },
}
```

Każda czynność ma `title` i jedno z pól:

| Pole | Wartość | Działanie |
| --- | --- | --- |
| `click`, `longClick` | ID lub zapytanie | Klika lub przytrzymuje. |
| `touchHold` | ID lub zapytanie | Od wersji 6. Trzyma dotyk przez sekundę, tak jak podwójne stuknięcie i przytrzymanie. Przydaje się, gdy przycisk reaguje na fizycznie przytrzymany dotyk. |
| `focus` | ID lub zapytanie | Przenosi fokus SandFly. |
| `scrollForward`, `scrollBackward` | ID lub zapytanie | Przewija element. |
| `speak` | Tekst albo `{ textOf = item }` | Czyta tekst lub treść elementu. `textOf` wymaga `screen`. |
| `steps` | Lista | Od wersji 6. Do dziesięciu kroków wykonywanych po kolei. |

Wszystkie poza `speak` wymagają `actions`. Od wersji 5 możesz dodać `first = true`, żeby czynność pojawiała się przed pozycjami aplikacji. Domyślnie pojawia się po nich.

SandFly najpierw szuka wewnątrz elementu, do którego należy menu, potem w całym oknie. Jeśli znajduje na przykład napis przycisku, idzie do najbliższego rodzica obsługującego działanie, do sześciu poziomów wyżej. Gdy nie ma dopasowania, odtwarza dźwięk i wpisuje ostrzeżenie do dziennika. Jeśli w każdym wierszu jest taki sam przycisk, użyj [onActions](#hooks) i wyszukiwania od wiersza, żeby uniknąć kliknięcia pierwszego przycisku z okna.

Każdy krok może użyć `click`, `longClick`, `touchHold`, `focus`, `scrollForward` lub `scrollBackward`. Wartością jest zapytanie, lista do sześciu kolejnych prób albo `{}` dla samego elementu czynności. Domyślne `where = 'item'` szuka wewnątrz niego. `where = 'screen'` szuka w oknach aplikacji, na przykład w menu z poprzedniego kroku. SandFly zamyka własne menu i czeka do 2 sekund na każdy krok. Po niepowodzeniu odtwarza dźwięk i naciska Wstecz, jeśli wcześniejszy krok się wykonał.

```lua
{ title = 'Add to queue', steps = {
  { longClick = {} },
  { click = { text = 'Add to queue' }, where = 'screen' },
} }
```

### Jak sprawdzić dane elementu {#find-the-details-of-an-item}

W kategorii **Ustawienia programisty** na ekranie **Polecenia** znajdziesz trzy przydatne działania. Możesz przypisać im gest albo dodać do menu.

- **Sprawdź element z fokusem** otwiera **Szczegóły elementu**. W sekcji **Elementy wewnątrz** zobaczysz poziom (1 to bezpośrednie dzieci), klasę, klikalność i teksty w kolejności czytania.
- **Kopiuj szczegóły elementu** kopiuje te dane do schowka.
- **Kopiuj drzewo ekranu** kopiuje aplikację, aktywność i tytuł okna, a następnie jeden wiersz na element. Zawiera poziom, klasę, ID, rolę, stany, tekst, nazwane działania i pasujące reguły. Poziom 0 to korzeń. Rodzicem jest najbliższy wcześniejszy wiersz o niższym poziomie.

## Zapytania {#queries}

Zapytania służą do wyszukiwania w regułach, nawigacji i kodzie. Podaj ID jako tekst lub rekord. Wszystkie pola rekordu muszą pasować.

| Pole | Dopasowanie |
| --- | --- |
| `id` | Identyfikator widoku. Krótkie `send` pasuje do końcówki po `:id/`. ID zawierające dwukropek wymaga pełnej zgodności. |
| `text`, `contentDescription` | Dokładny tekst lub opis. |
| `textContains` | Fragment tekstu lub opisu, niezależnie od wielkości liter. |
| `className` | Końcówka klasy. `'Button'` pasuje do `android.widget.Button`. |
| `role` | Rola z klasy elementu, na przykład `'button'` lub `'check_box'`. |
| `clickable` | Wartość logiczna. |
| `parentId` | Krótkie lub pełne ID rodzica. |
| `inside` | Jeden z przodków musi pasować do podanego zapytania. Na przykład `{ className = 'Button', inside = { textContains = 'play video' } }` pasuje do przycisków w wierszu filmu. W środku nie można użyć `inside` ani `has`. Od wersji 2 można podać do czterech zapytań. Każde musi pasować do któregoś z przodków. |
| `has` | Sprawdza, czy pasuje któryś z elementów wewnątrz, do sześciu poziomów i 60 pozycji, od najbliższych. W środku nie można użyć `has` ani `inside`. Od wersji 6 `hasDepth` ustawia od 1 do 6 poziomów. Wartość 1 sprawdza tylko bezpośrednie dzieci. |
| `checkable`, `checked`, `longClickable`, `focusable`, `focused`, `selected`, `enabled`, `editable`, `scrollable`, `visible`, `heading`, `password` | Od wersji 2. Wartości logiczne, na przykład `{ className = 'CheckBox', checked = false }` dla niezaznaczonych pól. |
| `hint`, `stateDescription` | Od wersji 6. Dokładna podpowiedź lub opis stanu. |
| `contains` | Od wersji 6. Fragment tekstu, opisu, podpowiedzi, stanu lub dymka. Wielkość liter nie ma znaczenia. Użyj `field`, żeby sprawdzać tylko jedno pole. |
| `matches` | Od wersji 6. Wyrażenie regularne dopasowane we wskazanych polach bez względu na wielkość liter. Sprawdza do 1000 znaków. Powtarzane grupy nie mogą zawierać powtórzeń ani alternatyw. |
| `field` | Od wersji 6. Zawęża `contains` i `matches` do `'text'`, `'contentDescription'` lub `'description'`, `'hint'`, `'stateDescription'` lub `'state'`, `'tooltip'` albo `'id'`. Przy ID dopasowanie obejmuje nazwę z pakietem i bez pakietu. |
| `kind` | Od wersji 6. Jeden lub kilka typów jak w makrach. Dozwolone są `'clickable'`, `'longClickable'`, `'editable'`, `'scrollable'`, `'adjustable'`, `'checkable'`, `'checked'`, `'selected'`, `'heading'`, `'enabled'`. Typy klikalne sięgają do przodków, najwyżej sześć poziomów. `'scrollable'` sprawdza czynność przewinięcia, a `'adjustable'` suwak lub pasek postępu. |

`contains` i `matches` pomijają białe znaki przy krawędziach. Pozostałe pola tekstowe porównują bez przycinania. Zapytanie w manifeście musi zawierać przynajmniej jedno pole. Błędny `kind`, `field` lub wzorzec w kodzie powoduje błąd w chwili użycia.

## Elementy {#items}

Obsługa zdarzeń, polecenia, czynności i `sf.screen` przekazują migawkę elementu. Pola opisują stan z chwili pobrania. Użyj `item:refresh()` po zmianie ekranu. Brakujące pole ma wartość `nil`.

| Pole | Wartość |
| --- | --- |
| `text`, `contentDescription`, `hint`, `stateDescription` | Tekst. Treść haseł daje `nil` bez `passwords`. |
| `id` | Pełny identyfikator zasobu, na przykład `com.example:id/send`. |
| `className`, `packageName` | Nazwa klasy i pakietu. |
| `role` | Rola wynikająca z klasy, na przykład `'button'`, `'edit_text'` lub `'none'`. |
| `windowId`, `childCount` | Liczby. |
| `bounds` | `{ left, top, right, bottom }` w pikselach ekranu. |
| `selectionStart`, `selectionEnd` | Granice zaznaczenia od 0 albo -1. |
| `checkable`, `checked`, `clickable`, `longClickable`, `focusable`, `focused`, `accessibilityFocused`, `selected`, `enabled`, `editable`, `password`, `scrollable`, `visible`, `heading` | Wartości logiczne. `focused` to fokus klawiatury, a `accessibilityFocused` to fokus SandFly. |
| `actions` | Lista `{ id, label }` dla czynności z etykietą lub znanych metodzie `item:perform`. |
| `roleDescription` | Od wersji 2. Rola podana przez aplikację, na przykład `"tab"` na stronie. |
| `error` | Od wersji 2. Tekst błędu pola. |
| `tooltip`, `paneTitle`, `containerTitle` | Od wersji 2. Tekst. |
| `uniqueId` | Od wersji 2. Identyfikator stały w czasie życia elementu, dostępny od Androida 13. |
| `multiLine`, `dismissable`, `contentInvalid`, `textSelectable`, `importantForAccessibility`, `screenReaderFocusable` | Od wersji 2. Wartości logiczne. |
| `maxTextLength`, `inputType`, `drawingOrder` | Od wersji 2. Liczby. |
| `liveRegion` | Od wersji 2. `'polite'`, `'assertive'` lub `nil`. |
| `range` | Od wersji 2. Zakres suwaka lub paska `{ min, max, current, type }`. Typ to `'integer'`, `'float'`, `'percent'` lub `'indeterminate'`. |
| `collection` | Od wersji 2. Dane listy, siatki lub tabeli `{ rows, columns, hierarchical }`. |
| `collectionItem` | Od wersji 2. Dane wiersza lub komórki `{ row, column, rowSpan, columnSpan, heading, rowTitle, columnTitle }`. `row` i `column` liczą się od 1. |

`a == b` porównuje tożsamość elementu w dwóch migawkach. `tostring(item)` zwraca pierwszą dostępną wartość spośród tekstu, opisu, ID i klasy.

### Odczyt sąsiednich elementów {#reading-around-an-item}

Te metody wymagają `screen`.

- `item:parent()` zwraca rodzica lub `nil`.
- `item:children()` zwraca listę bezpośrednich dzieci. `item:child(index)` wybiera dziecko według numeru od 1.
- `item:find(query)` szuka pierwszego dopasowania w elemencie i jego zawartości. `item:findAll(query, limit)` zwraca listę, domyślnie do 50, maksymalnie do 200 pozycji.
- `item:refresh()` pobiera nową migawkę lub zwraca `nil`, jeśli element zniknął.
- Od wersji 2 `item:labeledBy()` wskazuje element etykiety, a `item:labelFor()` element opisywany przez tę etykietę. Jeśli go nie ma, zwraca `nil`.
- Od wersji 2 `item:recognizeText()` [rozpoznaje tekst](#recognizing-text) w obszarze elementu.

### Wykonywanie czynności na elemencie {#acting-on-an-item}

Te metody wymagają `actions`. Jeśli mogą [poczekać](#waiting), zwracają `true` lub `false`. W wywołaniu, które musi odpowiedzieć od razu, wykonują działanie i zwracają `nil`.

- `item:click()`, `item:longClick()`, `item:scrollForward()`, `item:scrollBackward()` klikają, przytrzymują i przewijają.
- Od wersji 2 `item:showOnScreen()`, `item:expand()`, `item:collapse()`, `item:dismiss()` pokazują, rozwijają, zwijają i odrzucają.
- `item:focus()` przenosi fokus, a `item:setText(text)` zastępuje tekst pola.
- Od wersji 2 `item:setSelection(start, end)` zaznacza tekst od pozycji liczonych od 0. Bez `end` przesuwa sam kursor.
- Od wersji 2 `item:moveCursor(unit, direction, extend)` przechodzi po `'character'` (domyślnie), `'word'`, `'line'`, `'paragraph'` lub `'page'`, w kierunku `'next'` lub `'previous'`. `extend = true` zaznacza mijany tekst.
- Od wersji 2 `item:setProgress(value)` ustawia wartość od `range.min` do `range.max`. `item:scrollTo(row, column)` przewija do wiersza i kolumny od 1.
- Od wersji 2 `item:nextHtmlElement(element, direction)` przechodzi po wybranym rodzaju elementu WWW, na przykład `'HEADING'`, `'LINK'`, `'BUTTON'`, `'LANDMARK'` albo `'TABLE'`.
- Od wersji 2 `item:tap()`, `item:doubleTap()` i `item:longPress(ms)` dotykają środka elementu w sposób opisany w [sekcji o dotyku](#touch-and-keys). Domyślne przytrzymanie to 1000 ms. Element bez położenia na ekranie powoduje błąd.

`item:perform(action, args)` wybiera czynność przez nazwę, numer lub etykietę własnej czynności. Etykieta pomija wielkość liter.

- Bez argumentów możesz podać `click`, `longClick`, `scrollForward`, `scrollBackward`, `expand`, `collapse`, `dismiss`, `copy`, `cut`, `paste`, `select`. Wersja 2 dodaje `clearSelection`, `focusInput`, `clearFocus`, `showOnScreen`, `scrollUp`, `scrollDown`, `scrollLeft`, `scrollRight`, `pageUp`, `pageDown`, `pageLeft`, `pageRight`, `contextClick`, `imeEnter`, `pressAndHold`, `showTooltip`, `hideTooltip`, `showTextSuggestions`.
- Od wersji 2 możesz podać argumenty: `setText` (`text`), `setSelection` (`start`, `end`), `setProgress` (`value`), `scrollToPosition` (`row`, `column` od 1), `nextAtGranularity` i `previousAtGranularity` (`unit`, `extend`) oraz `nextHtmlElement` i `previousHtmlElement` (`element`).

```lua
local field = sf.screen.find({ className = 'EditText', editable = true })
if field then
  field:setText('Hello')
  field:perform('imeEnter')
end
```

SandFly pamięta do 500 ostatnio używanych elementów. Użycie starszego daje błąd o niedostępnym elemencie.

## Oczekiwanie na wynik {#waiting}

Na wynik mogą czekać okna, `sf.fetch`, `sf.sleep`, czynności, dotyk i klawisze, `sf.commands.run`, `sf.screen.waitFor`, rozpoznawanie tekstu, `sf.translate`, `sf.speech.last`, `sf.device.set`, `sf.system.openApp` i inne działania systemowe, `sf.playSound` oraz `sf.speak` z `wait`.

Oczekiwanie działa w zadaniach. Należą do nich główny plik, zdarzenia poza czterema procedurami mowy, sam `onActions`, polecenia, czynności, liczniki, odpowiedzi z okien i funkcje przekazane do `sf.run`. Gdy jedno zadanie czeka, pozostałe mogą działać. Korutyny też mogą czekać, jeśli biegną w zadaniu.

```lua
sf.onCommand('delete', function()
  if sf.ui.confirm('Delete the message?') then
    sf.screen.click('delete_button')
  end
end)
```

W procedurze, która musi odpowiedzieć natychmiast, takiej jak `onFocus`, czynności, dotyk, klawisze, polecenia, `sf.device.set`, `sf.system.openApp`, działania systemowe i `sf.playSound` wykonają się i zwrócą `nil`. Inne wywołania oczekujące spowodują błąd. Użyj `sf.run(function() ... end)`, żeby przenieść operację do zadania.

## Obsługa zdarzeń {#hooks}

Funkcja rejestrująca przyjmuje procedurę i zwraca funkcję do jej usunięcia. Do jednego zdarzenia można podpiąć kilka procedur.

```lua
local stop = sf.onAppEnter(function(app) print(app.package) end)
stop() -- removes it
```

| Funkcja | Moment wywołania | Argumenty | Uprawnienie |
| --- | --- | --- | --- |
| `sf.onFocus` | Przed odczytaniem elementu, na którym jest fokus. | Element i planowany tekst. | `screen` |
| `sf.onSpeech` | Przed wypowiedzią SandFly. | Tekst. | `speech` |
| `sf.onNotification` | Przed odczytem powiadomienia lub krótkiego komunikatu Androida. | `{ package, toast, title, text, category }` i tekst odczytu. `toast` oznacza krótki komunikat. | `notifications` |
| `sf.onAnnouncement` | Przed komunikatem aplikacji obsługiwanej przez skrypt. | `{ package, text }` i tekst odczytu. | `screen` |
| `sf.onAppEnter`, `sf.onAppLeave` | Przy ładowaniu i usuwaniu skryptu, a globalnie także przy zmianie aplikacji na pierwszym planie. | `{ package, activity, window, meta }`, jak z `sf.app()` bez `label`. | `screen` w skrypcie globalnym |
| `sf.onWindowChange` | Przy otwarciu lub zmianie okna. | `{ package, activity, window, meta }`. | `screen` w skrypcie globalnym |
| `sf.onTextChange` | Przy zmianie pola edycji. | `{ package, node, text, before, from, added, removed, password }`. `before` to poprzednia treść, a `from` liczy pozycję od 0. | `screen` |
| `sf.onContentChange` | 200 ms po zmianie części ekranu, raz dla aplikacji. | `{ package, node, changeTypes }`. | `screen` |
| `sf.onScroll` | Od wersji 2. 150 ms po przewinięciu listy lub strony, dla ostatniego przewinięcia. | `{ package, node, from, to, count, scrollX, scrollY, deltaX, deltaY }`. Jeśli aplikacja je podaje, `from` i `to` oznaczają widoczne pozycje od 1, a `count` całą liczbę. | `screen` |
| `sf.onSelectionChange` | Od wersji 2. Przy przesunięciu kursora lub zmianie zaznaczenia. | `{ package, node, text, start, end, length, password }`. | `screen` |
| `sf.onClick`, `sf.onLongClick` | Od wersji 2. Przy kliknięciu lub przytrzymaniu elementu. | `{ package, node }`. | `screen` |
| `sf.onInputFocus` | Od wersji 2. Gdy element otrzyma fokus klawiatury. | `{ package, node }`. | `screen` |
| `sf.onEvent` | Dla każdego zdarzenia dostępności we wszystkich aplikacjach. | `{ type, package, className, text, contentDescription, beforeText, password, windowId, time }`. Dodatkowo `changeTypes` dla treści i okien albo `notification` jak w `onNotification`. `type` to na przykład `'viewClicked'` lub `'windowStateChanged'`. | `events` |
| `sf.onSettingChange` | Przy zmianie ustawienia skryptu w menu lub przez `sf.settings.set`. | Klucz i nowa wartość. | Brak |
| `sf.onButton` | Po naciśnięciu przycisku w ustawieniach skryptu. | Klucz. | Brak |
| `sf.onActions` | Przy przygotowaniu menu czynności. | Element. Zwróć listę `{ title, run }`. | `input` |

`node` to źródłowy element lub `nil`. Bez `passwords` tekst pola hasła jest zastąpiony przez `nil`. Procedury z polem `node` dostają zdarzenia tylko z aplikacji objętych skryptem. SandFly pomija własne zdarzenia. Przy 100 oczekujących `onEvent` odrzuca następne.

`onAppEnter` działa też przy załadowaniu skryptu z aplikacją na pierwszym planie. `onAppLeave` dla skryptu z `apps` ma przy usuwaniu z pamięci 250 ms.

`sf.onActions` musi zwrócić listę w 50 ms. Samo przygotowanie listy nie może czekać. Każda funkcja `run` jest wywoływana z elementem jako zadanie i może poczekać na wynik. Domyślnie czynności pojawiają się za pozycjami aplikacji. Od wersji 5 `first = true` umieszcza je przed nimi.

```lua
-- Needs screen, actions and input.
sf.onActions(function(row)
  local reply = row:find('reply_button')
  if reply then
    return { { title = 'Reply', first = true, run = function() reply:click() end } }
  end
end)
```

### Zmiany wypowiedzi {#speech-hooks}

`onFocus`, `onSpeech`, `onNotification` i `onAnnouncement` mogą zmienić mowę. Zwróć:

- tekst lub `{ speak = text }`, żeby zastąpić oryginał,
- `{ before = text, after = text }`, żeby dopisać tekst oddzielony spacjami,
- `false` albo `{ silent = true }`, żeby wyciszyć,
- pusty wynik, żeby zostawić wypowiedź.

```lua
sf.onFocus(function(item, text)
  if item.id and item.id:find(':id/timestamp$') then
    return { before = 'Sent' }
  end
end)
```

Zmiany wymagają `speech`. Każda kolejna procedura otrzymuje wynik poprzedniej. Najpierw działają skrypty aplikacji, potem pozostałe według kolejności **Uruchamiaj wcześniej** i **Uruchamiaj później**.

SandFly czeka łącznie do 30 ms, a po przekroczeniu czasu czyta bez zmian. Procedury mowy muszą odpowiadać bez [oczekiwania na wynik](#waiting). Dostają pierwszeństwo przed pracą czekającą w kolejce, lecz nie przerywają wykonywanego kodu Lua. Gdy skrypt właśnie zajmuje się dłuższym zadaniem, SandFly nie czeka. [sf.text.edit](#text) wykonuje zmiany takie jak kreator.

## Polecenia {#commands}

Zadeklaruj polecenie w manifeście i dodaj obsługę w kodzie. Użytkownik może je wywołać gestem, klawiszami, z menu, ze sterowania czytaniem albo przez własne przypisanie.

```lua
commands = {
  { id = 'unread', title = 'Next unread', gesture = 'swipeUpThenRight', keys = 'sf+u' },
  { id = 'summary', title = 'Read summary', menu = true },
  { id = 'messages', title = 'Messages', control = true },
  { id = 'search', title = 'Search' },
},
```

| Pole | Zastosowanie |
| --- | --- |
| `id` | Wymagany i unikatowy w skrypcie. Do 64 liter, cyfr, kropek, myślników i podkreśleń. |
| `title` | Wymagana nazwa pokazywana na ekranie **Polecenia**, w menu i przy sterowaniu czytaniem. |
| `gesture` | Nazwa [gestu](#gestures) lub lista nazw. |
| `keys` | [Klawisze](#keys) lub ich lista. |
| `menu` | `true` dodaje do menu SandFly. |
| `control` | `true` dodaje do sterowania czytaniem. Ruch w górę lub w dół wywołuje polecenie z `direction` równym `'previous'` lub `'next'`. |

`gesture`, `keys`, `menu` i `control` wymagają `input`. Od wersji 6 samo `id` i `title` można zadeklarować bez tej zgody. Skrypt nadal podaje uprawnienia potrzebne do wykonania zadania. Manifest mieści do 50 poleceń.

`sf.onCommand(id, fn)` rejestruje procedurę i zwraca funkcję do usunięcia przypisania. ID spoza manifestu powoduje błąd. Dopiero zarejestrowana procedura wykonuje polecenie.

```lua
sf.onCommand('unread', function(event)
  -- event.id, event.via, event.direction, event.node
end)
```

Rekord zdarzenia zawiera:

- `id` polecenia,
- `via` równe `'gesture'`, `'keys'`, `'menu'`, `'control'` lub `'command'` dla własnego przypisania,
- `direction` równe `'next'` albo `'previous'` przy sterowaniu czytaniem, w pozostałych przypadkach `nil`,
- `node`, czyli element, na którym jest fokus lub na którym otwarto menu. Bez `screen` jest to `nil`.

Zwróć `false` przed czekaniem na jakikolwiek wynik, żeby SandFly wykonał standardowe przypisanie gestu lub klawiszy. Skróty skryptu mają pierwszeństwo przed zwykłymi przypisaniami. Wyjątkiem jest gest **Włącz lub wyłącz wszystkie skrypty**. Przy konflikcie pierwszeństwo ma skrypt aplikacji, a później kolejność ze strony skryptów. Użytkownik może wyłączyć lub zmienić przypisanie na stronie skryptu.

Od wersji 4 wszystkie polecenia włączonego skryptu widać w **Polecenia** > **Skrypty**. Użytkownik może je przypisać tak jak działania SandFly. Takie powiązania mają `event.via = 'command'` i nie pojawiają się na stronie skryptu. SandFly poinformuje, jeśli polecenie zostanie wybrane, gdy skrypt nie działa na bieżącym ekranie.

### Skróty klawiszowe {#keys}

Podaj jedną kombinację albo dwie oddzielone przecinkiem. Elementy kombinacji łączy `+`.

- `sf` to klawisz SandFly. Od wersji 2 można go pominąć.
- Modyfikatory to `shift`, `ctrl` lub `control`, `alt` i `meta` lub `search`.
- Używaj liter, cyfr, symboli, nazw Androida bez `KEYCODE_` albo nazw `up`, `down`, `left`, `right`, `backspace`, `delete`, `esc`, `home`, `end`, `pageup`, `pagedown` i `return`.

Przykłady to `'sf+shift+n'`, `'sf+t, s'`, `'k'`, `'shift+n'`, `'space'` i `'t, s'`. `sf` może być tylko w pierwszej kombinacji. Po pierwszej części SandFly gra dźwięk i czeka 3 sekundy. Niepasujący drugi klawisz oznajmia innym dźwiękiem.

Od wersji 2 skróty bez `sf` są pomijane w polu edycji. Poza nim SandFly czeka na procedurę do 30 ms. Aby klawisz trafił do aplikacji, procedura musi w tym czasie zwrócić `false` bez oczekiwania. Przytrzymanie powtarza polecenie przy każdym powtórzeniu klawisza. Włączona nawigacja strzałkami ma pierwszeństwo dla strzałek.

### Przypisywanie gestów i klawiszy w kodzie {#assign-gestures-and-keys-from-code}

`sf.bind(command, assignment)` dodaje, a `sf.unbind(command, assignment)` usuwa przypisanie. Podaj `{ gesture = name }` albo `{ keys = keys }`. Zmiana jest trwała i widać ją na stronie skryptu. Funkcje wymagają `input`. Nieobsługiwany skrót, nieznane polecenie lub tryb ładowania na żądanie powodują błąd.

```lua
sf.bind('unread', { gesture = 'twoFingerSwipeUpThenRight' })
```

## Nawigacja {#navigation}

```lua
navigation = {
  { title = 'Unread messages', match = { className = 'UnreadRow' } },
  { title = 'Headlines', id = 'headline' },
},
```

Każdy wpis dodaje opcję sterowania czytaniem o nazwie `title`. Ruch w górę lub w dół przechodzi do poprzedniego albo następnego widocznego elementu w oknie pasującego do `id`, `match` lub obu. Brak wyniku uruchamia dźwięk. Wymagane jest `input`. W manifeście może być do 20 takich wpisów.

## Mowa, dźwięk i wibracje {#speech-sound-and-vibration}

**`sf.speak(text, options)`** czyta tekst. Opcje:

- `interrupt` (domyślnie `false`) przerywa obecny odczyt. Bez tego wypowiedź czeka w kolejce.
- `rate` i `pitch` mnożą ustawienia użytkownika przez 0,25 do 4. Domyślnie jest to 1.
- `language` podaje znacznik języka, na przykład `'de'`.
- `wait` [czeka](#waiting) na zakończenie. Zwraca `true` przy pełnym odczycie albo `false` po przerwaniu, wyciszeniu lub upływie 2 minut.

Funkcja działa podczas odtwarzania, rozmowy, nagrywania przez inną aplikację i dyktowania. Przy wyciszonej mowie nic nie wypowiada. Potrzebuje `speech`.

```lua
-- In a command: wait for Done to be said, then play a sound.
sf.speak('Downloading', { interrupt = true })
if sf.speak('Done', { wait = true }) then sf.playSound('end') end
```

| Wywołanie | Działanie |
| --- | --- |
| `sf.message(text, options)` | Od wersji 2. Czyta i pokazuje tekst na podłączonym monitorze brajlowskim. Domyślnie `interrupt` to `true`. Wymaga `speech`. |
| `sf.speech.stop()` | Od wersji 2. Zatrzymuje mowę. Wymaga `speech`. |
| `sf.speech.isSpeaking()` | Od wersji 2. Sprawdza, czy SandFly mówi. |
| `sf.speech.last()` | Od wersji 2. Czeka na ostatnią wypowiedź lub zwraca `nil`. Wymaga `screen`. |
| `sf.braille.show(text)` | Od wersji 2. Pokazuje do 1000 znaków na monitorze tak jak komunikat. Wymaga `speech`. |
| `sf.playSound(name)` | Odtwarza nazwany dźwięk lub plik pakietu, na przykład `'sounds/chime.wav'`. W zadaniu zwraca wynik i czeka na zakończenie pliku. |
| `sf.playTone(frequency, ms, options)` | Sinusoida. Domyślnie 440 Hz przez 100 ms. `options.volume` od 0 do 1, domyślnie 0,5. |
| `sf.playAudio(samples, options)` | Odtwarza próbki od -1 do 1, do 10 sekund. `options.sampleRate` od 8000 do 48 000, domyślnie 22 050. `options.volume` od 0 do 1, domyślnie 1. |
| `sf.vibrate(pattern)` | Czas w ms, domyślnie 50, albo lista do 20 czasów wibracji i przerw na zmianę. Pojedynczy czas to do 2000 ms, suma do 5000 ms. |

Nazwy wbudowanych dźwięków to `focus`, `actionable`, `click`, `longClick`, `scroll`, `listEnter`, `listExit`, `end`, `enter`, `typo` i `beep`. Dźwięki, tony i wibracje działają bez uprawnień i stosują ustawienia SandFly. Są pomijane przy wstrzymaniu lub wyciszeniu. Do czterech plików, tonów i strumieni próbek może grać równocześnie.

Od wersji 6 `sf.feedback.mute(what, ms)` wycisza mowę, dźwięki lub wibracje do 10 000 ms. W rekordzie `what` ustaw co najmniej jedno z `speech`, `sounds`, `vibration` na `true`. Trwająca mowa zostaje przerwana. Nowe sygnały są pomijane, także ze skryptu. Uprawnienie `speech` jest wymagane. Wywołaj `release()` na zwróconym uchwycie albo `sf.feedback.unmute()`, żeby zakończyć wyciszenie własnego skryptu.

Wyciszenie kończy się również po usunięciu skryptu lub błędzie, zmianie aplikacji oraz dotknięciu, geście lub klawiszu użytkownika. Działania skryptu go nie kończą. Kolejne wyciszenie zastępuje poprzednie. Wyciszenie na ekranie SandFly jest niedostępne.

```lua
-- In a command: silence SandFly while the script pages through a list.
local list = sf.screen.find({ scrollable = true })
if list then
  local mute = sf.feedback.mute({ speech = true, sounds = true }, 3000)
  for _ = 1, 5 do list:scrollForward() end
  mute.release()
end
```

## Ekran {#screen}

Wywołania wymagają `screen` i działają na aktywnym oknie aplikacji. Na ekranie SandFly zwracają błąd.

| Funkcja | Wynik |
| --- | --- |
| `sf.screen.focused()` | Element z bieżącym fokusem SandFly albo `nil`. |
| `sf.screen.inputFocused()` | Od wersji 2. Element, do którego trafia klawiatura, albo `nil`. |
| `sf.screen.root()` | Korzeń aktywnego okna albo `nil`. |
| `sf.screen.find(query)` | Pierwsze [dopasowanie](#queries) albo `nil`. |
| `sf.screen.findAll(query, limit)` | Lista do `limit` elementów, domyślnie 50, maksymalnie 200. Wyszukiwanie sprawdza rodzica przed dziećmi, maksymalnie 3000 elementów i do [końca czasu](#limits). |
| `sf.screen.at(x, y)` | Od wersji 2. Najgłębiej zagnieżdżony widoczny element w punkcie podanym w pikselach, w najwyższym oknie w tym miejscu. |
| `sf.screen.windows()` | Od wersji 2. Lista okien od najwyższego, bez okien SandFly. Każde to `{ id, type, title, layer, active, focused, bounds, root }`. Przez `window.root:find(query)` możesz szukać w innym oknie, na przykład klawiaturze. |
| `sf.screen.waitFor(query, timeout, interval)` | Od wersji 2. Czeka na dopasowanie w aktywnym oknie lub zwraca `nil`. `timeout` to domyślnie 5000 ms, najwyżej 60 000. `interval` domyślnie 250 ms, minimum 100. |
| `sf.screen.click(query)` | Od wersji 2. Szuka elementu lub przyjmuje element i go klika, a bez czynności kliknięcia dotyka. Zwraca powodzenie lub `false`, gdy nie ma wyniku. Wymaga też `actions`. |
| `sf.screen.recognizeText()` | Od wersji 2. [Rozpoznaje tekst](#recognizing-text) na całym ekranie. |

Typy okien to `'application'`, `'inputMethod'`, `'system'`, `'accessibilityOverlay'`, `'splitScreenDivider'` i `'magnificationOverlay'`.

**`sf.app()`** daje `{ package, label, activity, window, meta }`. `label` to nazwa aplikacji. `activity` może mieć wartość `nil` do chwili zmiany okna. `window` to tytuł, a `meta` zawiera tekstowe, liczbowe lub logiczne dane manifestu, na przykład `sf.app().meta['com.example.flag']`. Skrypt globalny potrzebuje `screen`.

```lua
sf.run(function()
  local button = sf.screen.waitFor({ text = 'Continue' }, 10000)
  if button then button:click() else sf.speak('No Continue button') end
end)
```

### Rozpoznawanie tekstu {#recognizing-text}

Od wersji 2 `sf.screen.recognizeText()` i `item:recognizeText()` rozpoznają tekst ze zrzutu lokalnie. Wybór pisma zależy od języka mowy i urządzenia. Dostępne są alfabet łaciński, chiński, japoński, koreański i dewanagari.

Wywołanie czeka na wynik, wymaga `screen` i Androida 11. Zwraca `{ text, blocks }`. Blok to `{ text, bounds, lines }`, a wiersz `{ text, bounds }`. Współrzędne są w pikselach. Możesz ich użyć do dotknięcia wyniku przez `sf.touch.tap`.

Skrypt może mieć dwa równoległe rozpoznania. Android ogranicza liczbę zrzutów na sekundę. SandFly ponawia odrzucony zrzut dwukrotnie, a potem zgłasza błąd. Zgłosi go także dla elementu spoza ekranu.

```lua
-- In a command: tap the first line that says Skip.
local result = sf.screen.recognizeText()
for _, block in ipairs(result.blocks) do
  for _, line in ipairs(block.lines) do
    if line.text:find('Skip') then
      local b = line.bounds
      sf.touch.tap((b.left + b.right) // 2, (b.top + b.bottom) // 2)
      return
    end
  end
end
```

## Dotyk i klawisze {#touch-and-keys}

Od wersji 2. Wymagają `actions`, a gdy można [poczekać](#waiting), zwracają wynik.

### Dotykanie ekranu {#touch-the-screen}

Od Androida 11. Punkty to nieujemne współrzędne w pikselach. W trakcie dotyku skryptu gesty trafiają do aplikacji.

- `sf.touch.tap(x, y)` dotyka punktu. `sf.touch.doubleTap(x, y)` dotyka dwa razy.
- `sf.touch.longPress(x, y, ms)` przytrzymuje, domyślnie przez 1000 ms.
- `sf.touch.swipe(x1, y1, x2, y2, ms)` przesuwa od punktu do punktu, domyślnie w 300 ms.
- `sf.touch.gesture(strokes)` wykonuje do dziesięciu ruchów naraz. Każdy ma `{ points, start, duration }`. Podaj od 1 do 100 punktów `{ x, y }`. Długość ruchu to od 1 do 10 000 ms, domyślnie 50. Początek liczy się od startu gestu, od 0 do 10 000 ms.

```lua
-- A two-finger swipe up in the middle of a 1080-pixel-wide screen.
sf.touch.gesture({
  { points = { { x = 440, y = 1600 }, { x = 440, y = 800 } }, duration = 300 },
  { points = { { x = 640, y = 1600 }, { x = 640, y = 800 } }, duration = 300 },
})
```

Od wersji 6 `sf.touch.lock(ms)` blokuje dotyk użytkownika do 10 000 ms. `sf.touch` skryptu nadal działa. `release()` na uchwycie lub `sf.touch.unlock()` kończy blokadę. Kończy się też po błędzie albo usunięciu skryptu, zmianie aplikacji, naciśnięciu obu klawiszy głośności lub wykonaniu polecenia blokady dotyku. Następna blokada zastępuje poprzednią. Potrzeba Androida 13 i **Używaj gestów w SandFly**. Blokada nie działa na ekranach SandFly.

### Pisanie i naciskanie klawiszy {#type-and-press-keys}

`sf.keys.type(text)` wpisuje do 2000 znaków w polu z fokusem klawiatury, w miejscu kursora, i zastępuje zaznaczenie. W polu hasła lub bez pola korzysta z Shizuku. Nowy wiersz i tabulator przechodzą w Enter i Tab. Inne znaki sterujące są pomijane.

`sf.keys.press(keys)` naciska przez Shizuku od 1 do 100 klawiszy po kolei. Podaj listę albo nazwy oddzielone spacją. Możesz użyć nazw Androida z `KEYCODE_` lub bez niego, niezależnie od wielkości liter. Działają między innymi `enter`, `tab`, `up`, `down`, `left`, `right`, `center`, `backspace`, `delete`, `esc`, `return`, `home`, `end`, `pageup`, `pagedown`, `playpause`, `next`, `previous`, `volumeup` i `volumedown`. `home` i `end` odnoszą się do wiersza. Aby przejść na ekran główny, użyj `sf.system.home()`.

Bez uruchomionego Shizuku i zgody dla SandFly operacje wymagające Shizuku zgłoszą błąd. Zobacz ustawienia **Shizuku i root**.

```lua
sf.keys.type('Meeting at 3')
sf.keys.press('enter')
```

### Przekazywanie wszystkich klawiszy do aplikacji {#pass-every-key-to-the-app}

Od wersji 3 `sf.keys.passAll()` i `sf.keys.passAll(true)` oddają klawisze Androidowi i aplikacji, w tym klawisz SandFly, wybór w trybie przeglądania, nawigację strzałkami, kursor przeglądu i skróty skryptów. Tylko skrót przełączania klawiatury SandFly działa nadal. Wymagane jest `input`.

Wyłącz przez `sf.keys.passAll(false)` lub skrótem przełączenia klawiatury. Tryb działa w zasięgu załadowanego skryptu i nie obejmuje ekranów SandFly. Po zmianie skrótem skrypt musi ponownie zażądać przekazywania. SandFly sam tego nie ogłasza, więc poinformuj użytkownika.

```lua
-- A game that uses every key.
local manifest = {
  id = 'example.game-keys', name = 'Game keys', author = 'Example',
  minApiVersion = 3, testedApiVersion = 6,
  apps = 'com.example.game',
  permissions = { 'input', 'speech' },
}

sf.keys.passAll()
sf.message('Keys go to the game')
```

## Makra {#macros}

Od wersji 6 **`sf.macros.run(file, values)`** uruchamia pierwsze makro z pliku pakietu, na przykład `'macros/reply.json'`. Plik o rozmiarze do 1 MB ma [format eksportu makr](item-files.md#macros).

Jeśli działa inne makro, zostanie zatrzymane. Funkcja zwraca `true` od razu. Kroki ruszają, gdy skrypt biegnie dalej, więc wywołanie działa także w procedurze zdarzenia. `values` to rekord tekstów wstawianych w miejsca `{name}` w krokach. Podaj do 200 nazw po najwyżej 64 znaki. Używaj liter, cyfr, kropek, myślników i podkreśleń. Każda wartość ma do 10 000 znaków. Makro otrzymuje też wartości zapisane przez inne makra tego skryptu przez **Ustaw wartość**, **Pobierz z linku** i **Przetłumacz tekst**.

Wywołanie powoduje błąd na ekranie SandFly, przy pliku bez makra lub jeśli makro przełącza wszystkie skrypty albo klawiaturę SandFly.

Skrypt potrzebuje uprawnień kroków:

| Uprawnienie | Czynności makra |
| --- | --- |
| `screen` | Czytanie ekranu i warunki, także `{item}`. |
| `screen` razem z `actions` | Klika, przytrzymuje, przenosi fokus lub działa na znalezionym elemencie. |
| `actions` | Dotyk w punkcie, wpisywanie i naciskanie klawiszy. |
| `speech` | Czytanie tekstu. |
| `input` | Uruchamianie [poleceń](#run-sandfly-commands) wraz z uprawnieniami danego polecenia. |
| `system` | Otwieranie aplikacji, adresów i ustawień, udostępnianie, zmiany głośności i trybu dzwonka, opcje urządzenia, multimedia, alarmy, minutniki, połączenia, wiadomości i aparat. |
| `clipboard` | Kopiowanie i `{clipboard}`. |
| `dialogs` | Wyświetlanie komunikatu. |
| `network` | Pobieranie i tłumaczenie. |

**`sf.macros.stop()`** zatrzymuje makro uruchomione przez ten skrypt. Kreator umieszcza kroki poleceń i wyzwalaczy w plikach `macros/` i uruchamia je przez `sf.macros.run`.

## Uruchamianie poleceń SandFly {#run-sandfly-commands}

Od wersji 2 **`sf.commands.list()`** zwraca rekordy `{ id, title, category }` w kolejności ekranu **Polecenia**. Pomija przełączanie wszystkich skryptów i klawiatury SandFly, otwieranie aplikacji i ich skrótów oraz polecenia skryptowe.

**`sf.commands.run(id)`** uruchamia działanie z listy i zwraca wynik, tak jak jego gest. Wymaga `input`. Na ekranie SandFly albo przy nieznanym ID powoduje błąd.

Dodatkowo `system` jest potrzebne do poleceń systemowych, wstrzymania lub wyciszenia SandFly, przełączania odczytu powiadomień, odbioru połączeń, regulacji głośności, zmiany klawiatury, używania mikrofonu, przełączania powiększenia, brajla, przekazania gestu i dotyku bezpośredniego, a także do poleceń SandFly Pro. `clipboard` jest potrzebne do kopiowania, wycinania, wklejania, zaznaczania wypowiedzi, odczytów kopiowanych do schowka i otwierania historii schowka lub mowy.

```lua
sf.commands.run('NEXT')          -- Next item
sf.commands.run('READ_FROM_TOP') -- Read from top
```

## Reguły dodawane z kodu {#rules-from-code}

Funkcje wymagają `speech`. Dodane reguły mają pierwszeństwo przed manifestem i znikają po usunięciu skryptu z pamięci.

- **`sf.rules.add(rule)`** sprawdza [regułę](#rules) tak samo jak manifest i zwraca jej numer. Możesz dodać do 200.
- **`sf.rules.remove(number)`** usuwa dodaną regułę.
- **`sf.rules.clear()`** usuwa wszystkie reguły, które dodał ten skrypt.

```lua
local rule = sf.rules.add({ id = 'promo_banner', hide = 'all' })
```

## Trwałe dane {#storage}

`sf.storage` działa między uruchomieniami i aktualizacjami skryptu. Klucz ma od 1 do 200 znaków. Wartością może być tekst, liczba, wartość logiczna lub tabela z takich wartości.

- **`sf.storage.get(key, fallback)`** zwraca zapisaną wartość lub `fallback`.
- **`sf.storage.set(key, value)`** zapisuje. `nil` usuwa wartość. Błąd po przepełnieniu pojawia się, gdy wspólny JSON kluczy i wartości przekroczyłby 524 288 znaków.
- **`sf.storage.remove(key)`** usuwa wartość.
- **`sf.storage.keys()`** podaje listę kluczy.
- **`sf.storage.clear()`** usuwa wszystkie wartości.

Użytkownik może je usunąć także przez **Wyczyść zapisane dane** na stronie skryptu.

```lua
local count = sf.storage.get('count', 0) + 1
sf.storage.set('count', count)
```

## Ustawienia {#settings}

Sekcja **Ustawienia** na stronie skryptu pokazuje opcje z manifestu.

```lua
settings = {
  { key = 'sayApp', type = 'switch', title = 'Say the app', default = true },
  { key = 'voice', type = 'list', title = 'Voice', options = { 'low', { value = 'hi', label = 'High' } } },
  { key = 'name', type = 'text', title = 'Your name', summary = 'Used in greetings' },
  { key = 'count', type = 'number', title = 'How many' },
  { key = 'reset', type = 'button', title = 'Reset counts' },
},
```

| Pole | Zastosowanie |
| --- | --- |
| `key` | Wymagany i unikatowy. Do 64 liter, cyfr, kropek, myślników i podkreśleń. |
| `type` | `'switch'` (domyślnie), `'list'`, `'text'`, `'number'` albo `'button'`. |
| `title` | Wymagana nazwa. |
| `summary` | Tekst pod nazwą. Lista pokazuje zamiast niego bieżącą opcję. |
| `default` | Wartość początkowa. Bez pola to `false`, pierwsza opcja listy, `''` lub 0, zależnie od typu. |
| `options` | Wymagana dla listy. Wartości lub rekordy `{ value, label }`. |

Przycisk nie ma wartości. Wywołuje [sf.onButton](#hooks) z kluczem. Jeśli skrypt jest wyłączony, SandFly prosi o włączenie.

- **`sf.settings.get(key)`** zwraca wartość. Brak klucza w manifeście lub klucz przycisku powoduje błąd.
- **`sf.settings.all()`** zwraca rekord wszystkich wartości.
- **`sf.settings.set(key, value)`** zmienia i wywołuje `onSettingChange`. Wartość jako JSON ma do 16 384 znaków.
- **`sf.settings.setOptions(key, options)`** zastępuje od 1 do 500 opcji listy, do 16 384 znaków łącznie.
- **`sf.settings.setTitle(key, text)`** i **`sf.settings.setSummary(key, text)`** od wersji 3 zmieniają nazwę lub opis do 500 znaków. `nil` przywraca wartość manifestu.

## Okna i komunikaty {#dialogs-and-windows}

Każde okno wymaga `dialogs`. Tytuł zaczyna się od nazwy skryptu, żeby użytkownik mógł rozpoznać, skąd pochodzi. Teksty mają do 20 000 znaków.

| Funkcja | Działanie |
| --- | --- |
| `sf.ui.alert(message, options)` | Pokazuje komunikat z **OK** i czeka na zamknięcie. `options.title` podaje tytuł. |
| `sf.ui.confirm(message, options)` | Pyta i zwraca `true` po zatwierdzeniu, w innym przypadku `false`. Opcje to `title`, `ok` i `cancel`. |
| `sf.ui.prompt(title, options)` | Zwraca wpisany tekst lub `nil` po anulowaniu. Opcje to początkowy `text` i `hint`. |
| `sf.ui.choose(title, choices, options)` | Pokazuje listę 1–500 wartości lub rekordów `{ value, label }`. Zwraca wybór lub `nil`. `options.selected` ustawia wartość początkową. Powyżej 10 pozycji dodaje filtr. |
| `sf.ui.text(title, text)` | Od wersji 2. Pokazuje tekst do przeglądania i zaznaczania znakami, słowami i wierszami. Czeka na zamknięcie. |
| `sf.ui.dialog(form)` | Otwiera formularz i zwraca go od razu. |
| `sf.ui.window(form)` | Od wersji 2. Otwiera formularz na całym ekranie. Wstecz zamyka okno. |

Pozostałe wywołania [czekają](#waiting). Od wersji 2 okna i komunikaty mogą układać się na sobie. Przy dziewiątym oknie najstarsze się zamknie. Usunięcie skryptu z pamięci zamyka jego okna, na przykład po opuszczeniu aplikacji. Wartości można zachować przez [sf.storage](#storage).

### Formularze {#forms}

Formularz to `{ title, items, onClose }`. Podaj od 1 do 50 pozycji. `onClose` dostaje wartości po zamknięciu. Każda pozycja ma `type` i niepowtarzalne `id`. Brak ID daje `item1`, `item2` i tak dalej. Zwykle dodasz też `label`. `enabled = false` wyłącza pozycję, a `visible = false` ją ukrywa.

| Typ | Pozostałe pola |
| --- | --- |
| `label` | `label` zawiera tekst. Od wersji 2 `selectable = true` pozwala czytać i zaznaczać znaki, słowa i wiersze. |
| `heading` | Od wersji 2. Etykieta będąca nagłówkiem. |
| `edit` | `value`, `hint`, `number`, `password`, `multiline`, `onChange`. Od wersji 2 `readOnly = true` pozostawia odczyt i zaznaczanie. `onSubmit` otrzymuje tekst po Enter lub przycisku zatwierdzenia w jednowierszowym polu. |
| `switch` | `value` i `onChange`. |
| `list` | `items` z wartościami lub rekordami `{ value, label }`, wybrane `value` i `onChange`. Wybierasz jedną pozycję. |
| `items` | Od wersji 2. Do 500 otwieranych wierszy, `items` z wartościami lub rekordami, `empty` jako komunikat i `actions` z nazwami czynności. `onSelect` otrzymuje wartość po stuknięciu, `onLongPress` po przytrzymaniu, a `onAction` nazwę i wartość po wybraniu czynności. |
| `slider` | Od wersji 2. `min`, `max`, `step` domyślnie 0, 100 i 1, do 10 000 kroków. `value` domyślnie równe `min`, dostępne `onChange`. |
| `button` | `onClick`. `close = true` zamyka formularz po kliknięciu. |

`onChange`, `onSelect`, `onLongPress` i `onSubmit` dostają wartość oraz formularz. `onClick` dostaje formularz, a `onAction` nazwę czynności, wartość i formularz. Wszystkie działają w zadaniach i mogą czekać.

- `form:get(id)` zwraca wartość, a `form:values()` wszystkie wartości według ID.
- `form:set(id, changes)` zmienia `value`, `label`, `hint`, `items`, `actions`, `empty`, `min`, `max`, `step`, `enabled` lub `visible`.
- `form:close()` zamyka formularz. `form.closed` przyjmuje wtedy `true`. `form:wait()` czeka na zamknięcie i zwraca wartości.
- Od wersji 2 `form:add(item, options)` dodaje na końcu lub za pozycją `options.after` i zwraca ID. `form:remove(id)` usuwa.
- Od wersji 2 `form:focus(id)` przenosi fokus na pozycję lub pierwszy wiersz listy `items`. `form:setTitle(title)` zmienia tytuł.

```lua
-- In a command: ask for a number of minutes.
local form = sf.ui.dialog({
  title = 'Timer',
  items = {
    { type = 'slider', id = 'minutes', label = 'Minutes', min = 1, max = 60, value = 5 },
    { type = 'button', id = 'start', label = 'Start', close = true },
  },
})
local values = form:wait()
```

[Przykład notatnika](scripting/examples/notepad.lua) otwiera notatki z listy, a przez menu **Czynności** kopiuje je lub usuwa.

## Schowek {#clipboard}

`sf.clipboard.get()` zwraca tekst schowka lub `nil`. `sf.clipboard.set(text)` wstawia tekst. Obie funkcje wymagają `clipboard`.

## System {#system}

Te funkcje wymagają `system`.

- **`sf.system.back()`**, **`home()`**, **`recents()`**, **`notifications()`**, **`quickSettings()`**, **`powerDialog()`**, **`lockScreen()`**, **`takeScreenshot()`** i **`allApps()`** wykonują czynność systemową i w zadaniu zwracają wynik. Zrzut Androida nie jest przekazywany skryptowi.
- **`sf.system.openApp(packageName)`** otwiera aplikację i w zadaniu potwierdza wynik. **`sf.system.apps()`** od wersji 2 zwraca listę aplikacji z ikoną, według nazwy, w rekordach `{ package, label }`.
- **`sf.system.openUrl(url)`** od wersji 2 przekazuje do aplikacji adres `http`, `https`, `mailto`, `tel`, `sms`, `smsto`, `geo` lub `market` i zwraca wynik.
- **`sf.system.share(text, options)`** od wersji 2 otwiera udostępnianie. Opcje to `title` okna i `subject`.
- **`sf.system.openSettings(page, packageName)`** od wersji 2 otwiera ekran ustawień i wraca z wynikiem. Dozwolone wartości to `settings` (domyślnie), `wifi`, `bluetooth`, `mobileData`, `airplaneMode`, `nfc`, `location`, `sound`, `display`, `battery`, `batterySaver`, `accessibility`, `apps`, `defaultApps`, `notifications`, `doNotDisturb`, `dateTime`, `language`, `keyboard`, `storage`, `security`, `privacy`, `developer`, `about`, `castScreen`, `hotspot`. Z nazwą pakietu możesz podać `app` lub `appNotifications`.
- **`sf.system.startActivity(intent)`** i **`sf.system.sendBroadcast(intent)`** od wersji 2 przyjmują `{ action, data, type, package, className, categories, extras }`. Konieczne jest `action` lub `className`, a przy klasie także `package`. Komunikat rozgłoszeniowy wymaga pakietu. `extras` zawiera tekst, liczby lub wartości logiczne. Tych wywołań nie można kierować do SandFly. Otwarcie aktywności zwraca powodzenie.

```lua
sf.system.startActivity({
  action = 'android.intent.action.VIEW',
  data = 'https://example.com',
  package = 'com.android.chrome',
})
```

Od wersji 6 kilka funkcji przekazuje żądanie do obsługującej je aplikacji i zwraca, czy je przyjęła:

- **`sf.system.setTimer(seconds, label)`** uruchamia od 1 do 86 400 sekund. **`sf.system.setAlarm(hour, minute, label)`** ustawia godzinę. Jeśli aplikacja zegara pozwala, nie musi się otwierać.
- **`sf.system.call(number)`** dzwoni albo otwiera ekran wybierania, jeśli brakuje zgody na połączenia.
- **`sf.system.message(number, text)`** otwiera aplikację wiadomości z podanym tekstem.
- **`sf.system.camera(video)`** otwiera aparat. `video = true` wybiera film.

**`sf.resume()`** wznawia wstrzymany SandFly.

## Sieć {#network}

**`sf.fetch(url, options)`** wymaga `network` i [czeka](#waiting) na odpowiedź z adresu HTTPS. Opcje:

- `method` przyjmuje `GET` (domyślnie), `POST`, `PUT`, `DELETE`, `HEAD` lub `OPTIONS`.
- `headers` to rekord nagłówków.
- `body` to tekst albo tabela do wysłania jako JSON. Domyślny typ tabeli to `application/json`, chyba że podasz własny. Limit to 1 MB.
- `timeout` określa czas łączenia i odczytu w ms. Domyślnie 30 000, maksymalnie 60 000.

Odpowiedź zawiera `status`, `statusText`, `ok` dla kodów od 200 do 299, `url`, `headers`, `body`, `response:json()` i `response:text()`. Nazwy nagłówków są małymi literami, a wspólne wartości rozdziela przecinek ze spacją. Odpowiedź ma do 5 MB. Jednocześnie można wysłać do ośmiu żądań. Kod taki jak 404 daje odpowiedź, a problem z połączeniem zwraca błąd sieciowy.

```lua
-- In a command, a hook or sf.run.
local ok, response = pcall(sf.fetch, 'https://api.example.com/items', {
  method = 'POST',
  body = { name = 'Test' },
})
if ok and response.ok then
  local data = response:json()
end
```

### Tłumaczenie tekstu {#translating-text}

Od wersji 2 **`sf.translate(text, options)`** tłumaczy przez usługę wybraną na ekranie **Tłumaczenie**. Wymaga `network`, czeka i zwraca `{ text, from, to }`. W `options.from` i `options.to` podaj znaczniki języka, na przykład `'fr'`. Bez `from` usługa wykrywa język, a bez `to` tłumaczy na język z ustawień. Błąd usługi lub brak konfiguracji powoduje błąd funkcji. Przy tłumaczeniu lokalnym funkcja czeka na pobranie języków.

## Urządzenie {#device}

Od wersji 2 odczyty działają bez dodatkowych uprawnień.

- **`sf.device.info()`** zwraca `{ manufacturer, model, androidVersion, sdk, sandflyVersion, screen }`. `screen` ma `{ width, height, density, orientation }`. Orientacja to `'portrait'` lub `'landscape'`.
- **`sf.device.battery()`** zwraca `{ level, charging, full, plugged }`. `level` to procent, `plugged` to `'ac'`, `'usb'`, `'wireless'`, `'dock'` lub `nil`.
- **`sf.device.network()`** zwraca `{ connected, wifi, cellular, ethernet, vpn, metered }`.
- **`sf.device.volume(stream)`** zwraca `{ level, min, max, muted }`. Kanały to `'media'` (domyślny), `'ring'`, `'notification'`, `'alarm'`, `'call'`, `'system'` i `'accessibility'`.
- **`sf.device.ringerMode()`** zwraca `'normal'`, `'vibrate'` lub `'silent'`.

Do zmian potrzebne jest `system`. **`sf.device.setVolume(stream, level)`** ogranicza wartość do przedziału `min`–`max`. **`sf.device.setRingerMode(mode)`** zmienia tryb i zgłasza błąd, jeśli Android go odrzuca, na przykład w trybie Nie przeszkadzać. Od wersji 6 **`sf.device.media(key)`** wysyła do odtwarzacza `'playPause'`, `'play'`, `'pause'`, `'next'`, `'previous'`, `'stop'`, `'rewind'` lub `'fastForward'`.

### Szybkie ustawienia urządzenia {#device-controls}

Od wersji 6 funkcje sięgają do panelu SandFly przez nazwy:

`wifi`, `bluetooth`, `mobileData`, `hotspot`, `airplaneMode`, `dataSaver`, `doNotDisturb`, `soundMode`, `flashlight`, `flashlightBrightness`, `location`, `autoRotate`, `batterySaver`, `darkTheme`, `nightLight`, `extraDim`, `colorInversion`, `colorCorrection`, `nfc`, `microphoneAccess`, `cameraAccess`, `brightness`, `adaptiveBrightness`, `screenTimeout`, `fontSize`, `displaySize`, `mediaVolume`, `callVolume`, `ringVolume`, `notificationVolume`, `alarmVolume`, `accessibilityVolume` i `systemVolume`.

- **`sf.device.controls()`** zwraca opcje dostępne na urządzeniu.
- **`sf.device.get(name)`** zwraca wartość logiczną dla przełącznika, `{ value, min, max }` dla suwaka, `{ value, label, options }` dla listy. Lista przyjmująca także liczby zawiera `min` i `max`. Brak opcji lub odczytu daje `nil`.
- **`sf.device.set(name, value)`** czeka i wymaga `system`. Przełącznik przyjmuje wartość logiczną, `'on'`, `'off'` lub `'toggle'`. Suwak przyjmuje liczbę, procent, na przykład `'50%'`, lub krok `'+1'` albo `'-1'`. Lista przyjmuje wartość, etykietę lub `'next'`. Funkcja zwraca `true` albo zgłasza przyczynę braku zmiany. Przy braku zgody może otworzyć ekran Androida.

`fontSize` przyjmuje również dowolny procent od 25 do 200, a przed Androidem 14 do 130. Możesz podać `50` lub `'50%'`, także poniżej 85% dostępnych w Androidzie. `displaySize` ustawia procent domyślnej gęstości. `min` utrzymuje krótszą krawędź poniżej 600 dp, a `max` co najmniej 320 dp. Potrzeba Shizuku. Zmiana rozmiaru może zmienić układ aplikacji, więc dotychczasowe reguły mogą przestać pasować.

```lua
sf.run(function()
  local ok, why = pcall(sf.device.set, 'flashlight', 'toggle')
  if not ok then sf.speak(why) end
end)
```

## Tłumaczenia {#translations}

W katalogu `locales` archiwum umieść pliki JSON nazwane według znacznika języka, na przykład `locales/de.json` i `locales/pt-BR.json`. Kluczem jest tekst angielski. Tłumaczenie z liczbą mnogą podaj jako rekord z potrzebnymi formami spośród `zero`, `one`, `two`, `few`, `many` i `other`.

```json
{
  "Next unread": "Nächste ungelesene",
  "{count} message": { "one": "{count} Nachricht", "other": "{count} Nachrichten" }
}
```

SandFly czyta plik dla języka urządzenia oraz języka bez regionu. Bardziej szczegółowa wersja ma pierwszeństwo. Tłumaczenie obejmuje nazwę i opis skryptu, nazwy, opisy i opcje ustawień, etykiety i wypowiedzi z reguł, stany, podpowiedzi, nazwy czynności oraz poleceń i nawigacji.

- **`sf._(text, params)`** zwraca tłumaczenie albo oryginał. Uzupełnia pola `{name}` z `params`.
- **`sf.ngettext(singular, plural, count, params)`** szuka tłumaczenia według `singular` i wybiera formę dla `count`. Wstawia `{count}` i pozostałe pola.
- **`sf.locale()`** podaje znacznik urządzenia, na przykład `'de-DE'`. **`sf.language()`** podaje znacznik języka odczytu tłumaczeń.

```lua
local n = 3
sf.speak(sf.ngettext('{count} message', '{count} messages', n)) -- '3 messages'
```

## Tekst {#text}

Od wersji 6 dostępne są funkcje z wyrażeniami regularnymi Javy, bez dodatkowego uprawnienia. Wielkość liter nie ma znaczenia. Powtarzane grupy nie mogą zawierać powtórzeń ani alternatyw. Tekst ma do 10 000 znaków.

**`sf.text.replace(text, pattern, with)`** zastępuje wszystkie dopasowania. W `with` odwołaj się do pierwszej grupy przez `$1`. **`sf.text.find(text, pattern)`** sprawdza, czy wzorzec pasuje w tekście.

**`sf.text.edit(text, edits)`** stosuje kolejno zmiany z kreatora. Wynik to `{ text = ... }`, `{ silent = true }` albo `nil`, gdy tekst się nie zmienił. Procedura mowy zwraca `result.text` dla zmiany lub `false` dla ciszy. Każda edycja ma pole `op`:

| Operacja | Działanie |
| --- | --- |
| `remove`, `replace` | Usuwa lub zastępuje `find`. Przy `regex = true` pole `find` jest wzorcem i `with` może użyć `$1`. |
| `removeRepeats` | Usuwa powtarzające się części. Części są rozdzielone przecinkami. |
| `removeParts` | Usuwa części zawierające `find` lub pasujące przy `regex = true`. |
| `keepParts` | Zachowuje pierwsze `count` części. |
| `say`, `before`, `after` | Czyta `text` zamiast wypowiedzi, przed nią lub po niej. `{text}` wstawia jej oryginał. |
| `silence` | Pomija wypowiedź. |

```lua
sf.onNotification(function(n, said)
  local result = sf.text.edit(said, {
    { op = 'remove', find = 'new message from' },
    { op = 'keepParts', count = 2 },
  })
  if result == nil then return nil end -- unchanged
  if result.silent then return false end
  return result.text
end)
```

## Dziennik, liczniki, pliki i JSON {#logs-timers-files-and-json}

- **`print(...)`** i **`sf.log(...)`** zapisują argumenty oddzielone spacjami na poziomie informacji. Użyj `sf.log.debug`, `sf.log.info`, `sf.log.warn` lub `sf.log.error`, aby podać poziom. Tabela bez metatabeli zostanie zapisana jako JSON. Zobacz [dziennik i błędy](#testing-and-debugging).
- **`sf.setTimeout(fn, ms, ...)`** uruchamia funkcję po odczekaniu, a **`sf.setInterval(fn, ms, ...)`** ponawia ją w odstępach co najmniej 10 ms. Pozostałe argumenty trafiają do funkcji. Obie zwracają numer dla **`sf.clearTimer(number)`**. Może działać do 100 liczników. Funkcje liczników są zadaniami.
- **`sf.sleep(ms)`** [czeka](#waiting) podaną liczbę milisekund. **`sf.run(fn, ...)`** uruchamia funkcję w zadaniu, które może czekać.
- **`sf.clock()`** od wersji 2 zwraca monotoniczny czas w milisekundach do pomiarów. **`os.time()`** i **`os.date()`** podają datę i godzinę.
- **`sf.apiVersion`** podaje wersję API, a **`sf.manifest()`** manifest.

### Lua i pliki {#lua-and-files}

- **`require(name)`** ładuje plik Lua z pakietu raz i zwraca jego wynik. Na przykład `require('lib.words')` czyta `lib/words.lua`. Nazwa może zawierać litery, cyfry, podkreślenia, myślniki i kropki.
- **`sf.readFile(path)`** od wersji 2 zwraca tekst pliku pakietu, na przykład `'data/words.txt'`, do 1 MB lub `nil` przy braku pliku.
- **`json.encode(value)`** i **`json.decode(text)`**, dostępne też jako `sf.json`, przeliczają między Lua i JSON. Pusta tabela staje się `[]`, a tabela samych wartości listą. Użyj `json.null` dla `null`. Dekodowanie `null` zwraca `nil`.

## Gesty {#gestures}

Nazw z tej listy używaj w polu `gesture` i w `sf.bind`.

- Jeden palec. `swipeUp`, `swipeDown`, `swipeLeft`, `swipeRight`, `swipeUpThenDown`, `swipeDownThenUp`, `swipeLeftThenRight`, `swipeRightThenLeft`, `swipeUpThenLeft`, `swipeUpThenRight`, `swipeDownThenLeft`, `swipeDownThenRight`, `swipeLeftThenUp`, `swipeLeftThenDown`, `swipeRightThenUp`, `swipeRightThenDown`, `doubleTap`, `doubleTapAndHold`.
- Dwa palce. `twoFingerTap`, `twoFingerDoubleTap`, `twoFingerTripleTap`, `twoFingerTapAndHold`, `twoFingerDoubleTapAndHold`, `twoFingerTripleTapAndHold`, `twoFingerSwipeUp`, `twoFingerSwipeDown`, `twoFingerSwipeLeft`, `twoFingerSwipeRight`, `twoFingerSwipeUpThenLeft`, `twoFingerSwipeUpThenRight`, `twoFingerSwipeDownThenLeft`, `twoFingerSwipeDownThenRight`, `twoFingerSwipeLeftThenUp`, `twoFingerSwipeLeftThenDown`, `twoFingerSwipeRightThenUp`, `twoFingerSwipeRightThenDown`, `twoFingerRotateClockwise`, `twoFingerRotateCounterclockwise`.
- Trzy palce. `threeFingerTap`, `threeFingerDoubleTap`, `threeFingerTripleTap`, `threeFingerQuadrupleTap`, `threeFingerTapAndHold`, `threeFingerDoubleTapAndHold`, `threeFingerTripleTapAndHold`, `threeFingerSwipeUp`, `threeFingerSwipeDown`, `threeFingerSwipeLeft`, `threeFingerSwipeRight`.
- Cztery palce. `fourFingerTap`, `fourFingerDoubleTap`, `fourFingerTripleTap`, `fourFingerDoubleTapAndHold`, `fourFingerSwipeUp`, `fourFingerSwipeDown`, `fourFingerSwipeLeft`, `fourFingerSwipeRight`.
- Czytnik linii papilarnych. `fingerprintSwipeUp`, `fingerprintSwipeDown`, `fingerprintSwipeLeft`, `fingerprintSwipeRight`.

Poniższe dodatkowe gesty SandFly rozpoznaje tylko wtedy, gdy używa ich polecenie lub skrypt.

- Przesunięcia przy krawędzi. `edgeSwipeUpFromBottom`, `edgeLongSwipeUpFromBottom`, `edgeSwipeDownFromTopLeft`, `edgeLongSwipeDownFromTopLeft`, `edgeSwipeDownFromTopRight`, `edgeLongSwipeDownFromTopRight`, `edgeSwipeRightFromLeftTop`, `edgeLongSwipeRightFromLeftTop`, `edgeSwipeRightFromLeftBottom`, `edgeLongSwipeRightFromLeftBottom`, `edgeSwipeLeftFromRightTop`, `edgeLongSwipeLeftFromRightTop`, `edgeSwipeLeftFromRightBottom`, `edgeLongSwipeLeftFromRightBottom`, `edgeSwipeUpAlongLeft`, `edgeSwipeDownAlongLeft`, `edgeSwipeUpAlongRight`, `edgeSwipeDownAlongRight`, `edgeSwipeLeftAlongTop`, `edgeSwipeRightAlongTop`, `edgeSwipeLeftAlongBottom`, `edgeSwipeRightAlongBottom`.
- Stuknięcia w rogu. `cornerTapTopLeft`, `cornerDoubleTapTopLeft`, `cornerTripleTapTopLeft`, `cornerDoubleTapAndHoldTopLeft`, `cornerTripleTapAndHoldTopLeft`, `cornerTapTopRight`, `cornerDoubleTapTopRight`, `cornerTripleTapTopRight`, `cornerDoubleTapAndHoldTopRight`, `cornerTripleTapAndHoldTopRight`, `cornerTapBottomLeft`, `cornerDoubleTapBottomLeft`, `cornerTripleTapBottomLeft`, `cornerDoubleTapAndHoldBottomLeft`, `cornerTripleTapAndHoldBottomLeft`, `cornerTapBottomRight`, `cornerDoubleTapBottomRight`, `cornerTripleTapBottomRight`, `cornerDoubleTapAndHoldBottomRight`, `cornerTripleTapAndHoldBottomRight`.
- Stuknięcie i przesunięcie jednym palcem. `twoFingerTapThenOneFingerSwipeUp`, `twoFingerTapThenOneFingerSwipeDown`, `twoFingerTapThenOneFingerSwipeLeft`, `twoFingerTapThenOneFingerSwipeRight`, `twoFingerDoubleTapThenOneFingerSwipeUp`, `twoFingerDoubleTapThenOneFingerSwipeDown`, `twoFingerDoubleTapThenOneFingerSwipeLeft`, `twoFingerDoubleTapThenOneFingerSwipeRight`, `twoFingerTripleTapThenOneFingerSwipeUp`, `twoFingerTripleTapThenOneFingerSwipeDown`, `twoFingerTripleTapThenOneFingerSwipeLeft`, `twoFingerTripleTapThenOneFingerSwipeRight`.
- Stuknięcie i przesunięcie dwoma palcami. `twoFingerTapThenSwipeUp`, `twoFingerTapThenSwipeDown`, `twoFingerTapThenSwipeLeft`, `twoFingerTapThenSwipeRight`, `twoFingerDoubleTapThenSwipeUp`, `twoFingerDoubleTapThenSwipeDown`, `twoFingerDoubleTapThenSwipeLeft`, `twoFingerDoubleTapThenSwipeRight`, `twoFingerTripleTapThenSwipeUp`, `twoFingerTripleTapThenSwipeDown`, `twoFingerTripleTapThenSwipeLeft`, `twoFingerTripleTapThenSwipeRight`.
- Stuknięcie i przesunięcie trzema palcami. `threeFingerTapThenSwipeUp`, `threeFingerTapThenSwipeDown`, `threeFingerTapThenSwipeLeft`, `threeFingerTapThenSwipeRight`, `threeFingerDoubleTapThenSwipeUp`, `threeFingerDoubleTapThenSwipeDown`, `threeFingerDoubleTapThenSwipeLeft`, `threeFingerDoubleTapThenSwipeRight`, `threeFingerTripleTapThenSwipeUp`, `threeFingerTripleTapThenSwipeDown`, `threeFingerTripleTapThenSwipeLeft`, `threeFingerTripleTapThenSwipeRight`.
- Stuknięcie i przesunięcie czterema palcami. `fourFingerTapThenSwipeUp`, `fourFingerTapThenSwipeDown`, `fourFingerTapThenSwipeLeft`, `fourFingerTapThenSwipeRight`, `fourFingerDoubleTapThenSwipeUp`, `fourFingerDoubleTapThenSwipeDown`, `fourFingerDoubleTapThenSwipeLeft`, `fourFingerDoubleTapThenSwipeRight`, `fourFingerTripleTapThenSwipeUp`, `fourFingerTripleTapThenSwipeDown`, `fourFingerTripleTapThenSwipeLeft`, `fourFingerTripleTapThenSwipeRight`.
- Zsuwanie i rozsuwanie palców oraz szybki ruch dwoma palcami tam i z powrotem. `twoFingerPinch`, `twoFingerSpread`, `threeFingerPinch`, `threeFingerSpread`, `fourFingerPinch`, `fourFingerSpread`, `twoFingerScrub`.
- Przesunięcie i przytrzymanie. `swipeUpAndHold`, `swipeDownAndHold`, `swipeLeftAndHold`, `swipeRightAndHold`, `swipeUpThenDownAndHold`, `swipeDownThenUpAndHold`, `swipeLeftThenRightAndHold`, `swipeRightThenLeftAndHold`, `swipeUpThenLeftAndHold`, `swipeUpThenRightAndHold`, `swipeDownThenLeftAndHold`, `swipeDownThenRightAndHold`, `swipeLeftThenUpAndHold`, `swipeLeftThenDownAndHold`, `swipeRightThenUpAndHold`, `swipeRightThenDownAndHold`.

Przesunięcia trzyczęściowe mają nazwy takie jak `swipeRightThenUpThenRight`, `swipeUpThenDownThenUp`. Gesty oddzielone uniesieniem mają nazwy takie jak `swipeRightLiftSwipeUp`, `swipeDownLiftSwipeDown`, `swipeUpThenLeftLiftSwipeRight`, `swipeUpLiftSwipeRightThenDown`. Te ostatnie wymagają ustawienia czasu między przesunięciami. Gdy krawędź służy do otwierania menu, ruchy wzdłuż niej trafiają do menu.

## Testowanie i diagnozowanie {#testing-and-debugging}

### Dziennik {#the-log}

**Dziennik** na stronie skryptu pokazuje najnowsze wpisy od uruchomienia SandFly, wraz z godziną i poziomem. Zawiera `print`, `sf.log`, błędy i ostrzeżenia SandFly. Zachowuje do 300 wpisów po 4000 znaków. **Kopiuj dziennik** kopiuje je od najstarszego. **Wyczyść dziennik** je usuwa.

SandFly dodaje wpis poziomu diagnostycznego po załadowaniu dla pakietu i ostrzeżenie o brakującym uprawnieniu dla pominiętego działania. Ostrzega też, gdy czynność reguły nie znajduje elementu, procedura mowy przekracza czas lub wersja API nie pasuje.

**Ustawienia programisty** > **Poziom rejestrowania** określa, które wpisy trafiają do dziennika systemowego z tagiem `BacktalkScript`. Mogą zawierać tekst ekranu. Dziennik skryptu zachowuje je także wtedy, gdy domyślne ustawienia pomijają je w dzienniku systemowym.

### Błędy {#errors}

Po błędzie zdarzenia, polecenia, licznika lub odpowiedzi z okna dziennik podaje miejsce, wiadomość i stos wywołań. Skrypt nadal działa. Błąd podczas ładowania `main.lua` rozpoczyna się od `Loading:` i powstrzymuje załadowanie. Przy błędnym manifeście import podaje przyczynę, na przykład brak autora lub nieznane pole reguły.

SandFly wyłącza skrypt po pięciu błędach w minutę albo trzech przekroczeniach czasu w pięć minut. Podaje komunikat i zapisuje przyczynę. Jeśli wywołanie blokuje wszystkie skrypty ponad 20 sekund, SandFly wyłącza sprawcę i ponownie uruchamia resztę. Po naprawie użyj **Włącz** na stronie skryptu.

### Ponowne ładowanie {#reload-a-script}

- **Importuj skrypt** zastępuje kod, zachowując dane, ustawienia i wybory uprawnień.
- Wyłączenie i ponowne włączenie wczytuje od nowa kod i zmienne.
- Skrypt z `apps` ładuje się za każdym razem, gdy jego aplikacja trafia na pierwszy plan.

### Wskazówki do testów {#tips}

- Przypisz gest do **Włącz lub wyłącz wszystkie skrypty**, zanim zaczniesz próbę, jeśli nie wiesz, jak zadziała skrypt. Skrypt nie może przejąć tego gestu. Możesz też użyć **Skrypty** > **Wyłącz wszystkie skrypty**.
- Dane do zapytań odczytaj przez [sprawdzanie elementu](#find-the-details-of-an-item).
- Użyj `print` na rekordzie zdarzenia. `sf.onScroll(function(e) print(e) end)` zapisze zdarzenie przewijania jako JSON.
- Otocz `pcall` wywołania, które mogą się nie udać, na przykład `sf.fetch` i `sf.device.set`.
- Krótkie procedury mowy mogą użyć zmiennych obliczonych wcześniej w `onAppEnter` lub liczniku.

## Limity {#limits}

| Zasób | Limit |
| --- | --- |
| Ładowanie i wywołanie | 2 sekundy na główny plik lub wywołanie. W zadaniu czekającym liczy się każda część między pauzami. |
| Procedury mowy i menu | 30 ms łącznie na mowę. `onActions` może działać do 250 ms, ale pozycje trafią do menu tylko przy odpowiedzi w 50 ms. Szukanie elementów wlicza się do czasu. |
| Pamięć | 32 MB dla skryptu i 8 MB tekstu z `string.rep`. |
| Manifest | 500 reguł, w tym do 100 z `match`, 50 poleceń i 20 opcji nawigacji. Do 10 czynności w regule, 10 kroków w czynności i 12 części w `read`. |
| Zasoby podczas działania | 200 dodatkowych reguł, 100 liczników, 8 żądań, 8 okien i formularzy, 2 rozpoznania tekstu. |
| Dane | 524 288 znaków. Klucz od 1 do 200 znaków. Klucz ustawienia i ID polecenia do 64. |
| Tekst | Do 20 000 znaków mowy lub okna, 1000 znaków w brajlu, 4000 znaków wpisu i 10 000 znaków dla `sf.text`. |
| Pliki | `.lua` do 1 MB. `.zip` do 5 MB przed i po rozpakowaniu oraz 200 plików. `sf.readFile` i plik makra do 1 MB. |

SandFly sprawdza czas i pamięć co 10 000 instrukcji Lua. Przekroczenie powoduje błąd, którego `pcall` nie przechwyci. Wywołanie biblioteczne trwa do końca, więc używaj prostych wzorców przy długim tekście, zwłaszcza przy `.-` i `.*`.

## Publikowanie skryptu {#publish-a-script}

Aby udostępnić skrypt, umieść plik `.lua` lub `.zip` w publicznym repozytorium. Użytkownicy mogą dodać adres przez **Skrypty** > **Źródła skryptów** > **Dodaj źródło**. Możesz podać repozytorium lub konkretną gałąź i katalog, na przykład:

- `github.com/owner/repository` lub `github.com/owner/repository/tree/main/scripts`.
- `gitlab.com/group/repository` lub `gitlab.com/group/subgroup/repository/-/tree/main/scripts`.
- `codeberg.org/owner/repository` lub `codeberg.org/owner/repository/src/branch/main/scripts`.
- `gitea.com/owner/repository`, z układem ścieżki jak na Codebergu.
- `bitbucket.org/workspace/repository` lub `bitbucket.org/workspace/repository/src/main/scripts`.

Źródło pokazuje do 200 plików bezpośrednio w katalogu. Możesz też udostępnić adres HTTPS konkretnego pliku.

Raz dziennie SandFly sprawdza `version` w źródle i pod tą samą nazwą pliku. Skrypt bez zapamiętanego źródła może znaleźć przez plik nazwany według `id`, na przykład `my-script.lua`. Automatycznie instaluje zgodną aktualizację, jeśli pochodzi z tego samego źródła, ma tego samego autora i nie wymaga nowych uprawnień. Inne aktualizacje są w **Skrypty** > **Aktualizacje**. Przed instalacją zobaczysz nowe uprawnienia i ewentualną zmianę autora.

SandFly podpisuje pliki w [oficjalnym repozytorium skryptów](https://github.com/aaron-gh/sandfly-scripts). Ostatni wiersz `main.lua` zaczyna się od `-- SandFly signature:`. Podpis obejmuje wszystkie pliki. Przy instalacji SandFly zgłasza poprawny podpis, brak podpisu lub niezgodność po zmianie. Aktualizacja z oficjalnego repozytorium instaluje się automatycznie tylko przy poprawnym podpisie.

## Kreator skryptów {#the-script-builder}

**Skrypty** > **Nowy skrypt** daje dostęp do kreatora opisanego w rozdziale „Skrypty i własne etykiety”. Kreator zapisuje zwykły skrypt. W `main.lua` umieszcza manifest, wiersze dla poleceń, wyzwalaczy i zmian mowy oraz **Kod Lua** na końcu. Kroki przechowuje w `macros/` i uruchamia przez [sf.macros.run](#macros).

Kreator sam wypełnia uprawnienia. **Kod** > **Dodatkowe uprawnienia** pozwala dodać kolejne, na przykład `java`. Przed zapisem kreator sprawdza składnię Lua i manifest. Przy niepoprawnej kombinacji, na przykład regułach w skrypcie na żądanie, pokazuje powód i nie zapisuje zmiany.

**Kopiuj do kreatora skryptów** tworzy edytowalną kopię z części manifestu, kodu i pozostałych plików. **Eksportuj skrypt** zapisuje archiwum do ponownego importu i edycji. **Utwórz skrypt za pomocą SandFly Pro** i **Zmień za pomocą SandFly Pro** tworzą lub zmieniają projekt z opisu. Do akceptacji wynik działa bez uprawnień `network` i `java`.

## Przykładowe skrypty {#example-scripts}

- [example-plugin.lua](scripting/examples/example-plugin.lua) odczytuje nazwę aplikacji po przełączeniu i liczy jej otwarcia. Pokazuje `onAppEnter`, `sf.storage` i przełącznik. Wbudowaną kopię znajdziesz w **Skrypty** > **Dodaj przykładowy skrypt**.
- [notepad.lua](scripting/examples/notepad.lua) otwiera notatnik z menu lub skrótu klawisz SandFly + N. Pokazuje `sf.ui.window`, listę `items` z czynnościami, `sf.ui.confirm` i pamięć danych.
- [wikipedia-search.lua](scripting/examples/wikipedia-search.lua) wyszukuje artykuł i otwiera wybrany. Pokazuje ładowanie na żądanie, `sf.ui.prompt`, `sf.fetch` z JSON, `sf.ui.choose` i `sf.system.openUrl`.
- [talking-clock.lua](scripting/examples/talking-clock.lua) odczytuje godzinę co godzinę albo pół godziny i na polecenie. Pokazuje `keepLoaded`, ustawienia listy, `onSettingChange` i liczniki.

Pliki znajdują się w [katalogu przykładów](scripting/examples).
