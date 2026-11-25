# Pisanie skryptów SandFly

Ten dodatek opisuje wersję 6 API skryptów SandFly. Skrypt to program w Lua, który zmienia działanie SandFly w wybranej aplikacji lub we wszystkich aplikacjach. Może też dodawać własne polecenia. Instalację, zarządzanie skryptami i tworzenie ich bez pisania kodu opisano w rozdziale „Skrypty i własne etykiety”.

## Możliwości {#overview}

Skrypty działają na telefonach i tabletach. Zegarki i telewizory ich nie obsługują.

### Co może zrobić skrypt {#what-a-script-can-do}

- [Reguły](#rules) mogą nazwać lub ukryć element, zmienić kolejność odczytu lub przeczytać wiersz jako jeden element. Mogą też ograniczyć odczyt do wybranych części elementu i dodać pozycje do jego menu **Czynności**.
- [Obsługa mowy](#speech-hooks) pozwala zmienić wypowiedź dotyczącą elementu, powiadomienia lub komunikatu oraz całą mowę.
- [Procedury obsługi zdarzeń](#hooks) reagują między innymi na otwarcie aplikacji, zmianę fokusu, edycję tekstu, przewinięcie i powiadomienie.
- [Polecenia](#commands) mogą działać z gestu, klawiatury, menu i sterowania czytaniem. [Opcje nawigacji](#navigation) przechodzą po wybranych elementach.
- Kod może znajdować elementy na ekranie, klikać, przewijać, wpisywać tekst, przekazywać dotknięcia, naciskać klawisze i rozpoznawać tekst w wybranym obszarze.
- Skrypt może odczytywać tekst, odtwarzać dźwięki i tony, uruchamiać wibracje i wyświetlać tekst w brajlu.
- Może wyświetlać okna dialogowe i pełnoekranowe, zapisywać dane i ustawienia, pobierać dane z internetu, tłumaczyć tekst, uruchamiać makra i polecenia SandFly oraz otwierać aplikacje, linki i strony ustawień. Może też zmieniać ustawienia urządzenia, takie jak Wi-Fi i głośność.

### Ograniczenia skryptu {#what-a-script-cant-do}

- Skrypt może korzystać tylko z [uprawnień wymienionych w manifeście](#permissions). Użytkownik widzi je przed instalacją i może wyłączyć każde z osobna.
- Skrypt nie zmienia [ekranów SandFly](#sandfly-screens), na przykład jego ustawień.
- Dostęp do plików ogranicza się do odczytu zawartości własnego pakietu. Funkcje `io`, `os.execute`, `debug` i `package` są niedostępne.
- Wywołania Javy lub Androida wymagają [uprawnienia `java`](#java).
- Każde wywołanie skryptu ma limit 2 sekund. Zobacz [Limity](#limits).
- Skrypty nie działają po wstrzymaniu SandFly ani po włączeniu **Wyłącz wszystkie skrypty**. [Działanie przy wygaszonym ekranie](#with-the-screen-off) wymaga osobnej zgody.

### Wersje API {#api-versions}

`sf.apiVersion` zwraca wersję API tej kompilacji SandFly, w której skrypt działa. Elementy dodane po wersji 1 mają adnotację, na przykład *Od wersji 2*. W `minApiVersion` w manifeście podaj najwyższą wersję wymaganą przez elementy, z których korzysta skrypt. Wersje od 2 do 6 rozszerzają API. Skrypty korzystające z wersji 1 działają dalej na tych samych zasadach.

## Pierwszy skrypt {#write-your-first-script}

Ten skrypt odczytuje „Hello from a script” po otwarciu ustawień Androida. Dodaje też polecenie, które odczytuje, w jaki sposób je uruchomiono.

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

1. Zapisz skrypt jako plik tekstowy `hello.lua`. Umieść go na telefonie, na przykład w katalogu pobranych plików.
2. W ustawieniach SandFly otwórz **Skrypty** > **Importuj skrypt** i wybierz `hello.lua`.
3. Sprawdź dane w oknie instalacji i wybierz **Zainstaluj**. Po instalacji skrypt jest włączony.
4. Otwórz ustawienia Androida. Usłyszysz „Hello from a script”.
5. W ustawieniach SandFly otwórz **Skrypty** > **Hello** > **Dziennik**. Funkcja `print` zapisała tam wiersz `Started in com.android.settings`.
6. Otwórz **Polecenia** > **Skrypty** i przypisz gest do **Hello: Say hello**. Wróć do ustawień Androida i wykonaj gest. Usłyszysz „Hello, run from command”.

Żeby zaktualizować skrypt, zmień plik, zwiększ `version` i zaimportuj ponownie. Ten sam `id` zastąpi poprzednią wersję. Dane, ustawienia i wybory uprawnień zostaną zachowane. W [części o testowaniu i diagnozowaniu](#testing-and-debugging) opisano dziennik, błędy i przeładowywanie skryptów. Dłuższe skrypty do wykorzystania jako punkt wyjścia znajdziesz w [przykładach](#example-scripts).

## Pliki skryptów {#script-files}

- Plik `.lua` do 1 MB jest plikiem `main.lua` skryptu.
- Archiwum `.zip` może zawierać `main.lua`, kolejne pliki Lua, dźwięki, makra, dane i przekłady. Limit wynosi 5 MB archiwum, 5 MB po rozpakowaniu i 200 plików. `main.lua` może być na najwyższym poziomie albo w pojedynczym wspólnym katalogu. Wpisy zaczynające się od kropki i katalog `__MACOSX` są pomijane.

Nazwa pliku musi mieć rozszerzenie `.lua` lub `.zip`. Do plików w archiwum można sięgnąć przez [require i sf.readFile](#lua-and-files), [sf.playSound](#speech-sound-and-vibration) oraz [sf.macros.run](#macros). [Tłumaczenia](#translations) znajdują się w `locales`.

Instalację rozpocznij przez **Skrypty** > **Importuj skrypt** albo otwórz lub udostępnij plik `.lua`, `.zip` lub prowadzący do niego adres `https` przez **Zainstaluj skrypt SandFly**. Okno instalacji pokazuje pochodzenie skryptu, informację o podpisie SandFly, uprawnienia i polecenia.

### Środowisko Lua {#the-lua-environment}

SandFly uruchamia [Lua 5.4](https://www.lua.org/manual/5.4/) przez [LuaJava](https://github.com/gudzpoz/luajava). Wczytuje wyłącznie tekst źródłowy. W skrypcie są dostępne następujące funkcje i biblioteki.

- Funkcje `assert`, `error`, `ipairs`, `next`, `pairs`, `pcall`, `xpcall`, `print`, `rawequal`, `rawget`, `rawlen`, `rawset`, `select`, `tonumber`, `tostring`, `type`, `getmetatable`, `setmetatable`, `load`, `require`, `collectgarbage`, `_G`, `_VERSION`.
- Biblioteki `string`, `table`, `math`, `utf8` i `coroutine`.
- Funkcje `os.time`, `os.clock`, `os.date` i `os.difftime`.
- `json` do obsługi tekstu w formacie JSON oraz moduł `sf` opisany w tym dodatku.

Część funkcji działa w ograniczonym zakresie.

- `load` przyjmuje tekst lub funkcję. Nie wczytuje skompilowanego Lua. Uruchamia fragment w środowisku skryptu, chyba że wskażesz inne.
- `setmetatable` odrzuca metatabele z `__gc`.
- `collectgarbage` przyjmuje tylko `'collect'`, `'count'` i `'step'`.
- `string.dump` jest niedostępne. `string.rep` odmawia utworzenia tekstu dłuższego niż 8 MB.
- `pcall` i `xpcall` nie mogą przechwycić błędów zatrzymujących skrypt po przekroczeniu limitu czasu lub pamięci.
- Korutyny utworzone przez skrypt mogą wywoływać funkcje [czekające na wynik](#waiting), jeśli wznawiasz je z kodu wykonywanego w zadaniu.

Pozostałe funkcje `os` oraz `io`, `debug`, `package`, `dofile` i `loadfile` są niedostępne.

## Manifest {#the-manifest}

Umieść `local manifest = { ... }` przed kodem w `main.lua`. SandFly odczyta wartości tekstowe, liczby, `true`, `false`, `nil` i tabele. Komentarze są dozwolone. Zmienne, wywołania funkcji i operator `..` są niedozwolone.

Tabela z wartościami jest listą, na przykład `{ 'speech', 'screen' }`. Tabela z polami o nazwach jest rekordem, na przykład `{ id = 'x' }`. Nie mieszaj tych postaci. `{}` oznacza pustą listę. W kodzie możesz odczytać manifest przez `sf.manifest()`.

| Pole | Typ | Zastosowanie |
| --- | --- | --- |
| `id` | Tekst | Wymagany. Do 64 znaków z małych liter, cyfr, kropek (`.`), łączników (`-`) i podkreśleń (`_`). Pierwszy znak to litera albo cyfra. Skrypt o tym samym ID zastępuje poprzedni. |
| `name` | Tekst | Wymagany. Widoczny na liście skryptów i na początku tytułu okien skryptu. |
| `author` | Tekst | Wymagany. |
| `minApiVersion` | Liczba | Wymagana najstarsza [wersja API](#api-versions). |
| `testedApiVersion` | Liczba | Najwyższa sprawdzona wersja. Wymagana, nie niższa od `minApiVersion`. SandFly może ostrzec o zmianie wywołań po podanej wersji. Między wersjami 1 i 6 takie zmiany nie występują. |
| `version` | Tekst | Wersja skryptu, na przykład `'1.2'`. Domyślna to `'1'`. Porównywana po częściach liczbowych, więc `'1.10'` jest nowsza od `'1.9'`. Zwiększ przy [publikacji zmian](#publish-a-script). |
| `description` | Tekst | Opis na stronie skryptu i przy instalacji. |
| `homepage` | Tekst | Adres `http` lub `https`. Na stronie skryptu pojawia się wtedy **Otwórz stronę domową**. |
| `apps` | Tekst lub lista | [Zakres działania](#where-a-script-runs). Brak pola obejmuje wszystkie aplikacje. |
| `permissions` | Lista | [Wymagane uprawnienia](#permissions). |
| `settings` | Lista | [Ustawienia](#settings) na stronie skryptu. |
| `rules` | Lista | [Reguły](#rules) zmieniające elementy. |
| `commands` | Lista | [Polecenia](#commands) dla użytkownika. |
| `navigation` | Lista | [Sposoby nawigacji](#navigation). |
| `onDemand` | Wartość logiczna | Od wersji 4. [Ładuje skrypt](#scripts-loaded-on-demand) wyłącznie do wykonania polecenia. |
| `keepLoaded` | Wartość logiczna | Od wersji 5. Dodaje **Działaj przy wyłączonym ekranie** na stronie skryptu. Zobacz [działanie przy wygaszonym ekranie](#with-the-screen-off). |

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

- `activity` ogranicza działanie do jednego ekranu aplikacji. Podaj pełną nazwę klasy aktywności albo końcową część tej nazwy. SandFly rozpoznaje aktywność, gdy zmienia się okno aplikacji. Do pierwszej zmiany, na przykład tuż po uruchomieniu SandFly przy otwartej aplikacji, skrypt działa na wszystkich jej ekranach.
- `window` ogranicza działanie do okien, których tytuł zawiera podany tekst. Wielkość liter nie ma znaczenia.

Skrypt aplikacji ładuje się, gdy ta pojawia się na pierwszym planie, i zostaje usunięty z pamięci po wyjściu. Za każdym razem startuje od nowa. Żeby zachować wartości, użyj [sf.storage](#storage). Jeśli nie podasz aktywności ani okna, pasuje również inne okno tego pakietu, w którym jest fokus SandFly, na przykład okno klawiatury.

Skrypt bez `apps` działa we wszystkich aplikacjach. Pozostaje w pamięci, dopóki jest włączony, chyba że [ładuje się na żądanie](#scripts-loaded-on-demand).

Wszystkie skrypty są zwalniane z pamięci po wstrzymaniu SandFly albo po włączeniu opcji **Wyłącz wszystkie skrypty**. Są zwalniane też po wygaszeniu ekranu, z wyjątkiem tych, które mają wtedy [pozostać aktywne](#with-the-screen-off). Po zwolnieniu skryptu kończą się jego liczniki czasu i żądania, a otwarte przez niego okna są zamykane.

### Ekrany SandFly {#sandfly-screens}

Skrypty nie zmieniają ekranów SandFly. Dzięki temu **Skrypty** w ustawieniach można otworzyć niezależnie od działania skryptu. Gdy ekran SandFly jest na pierwszym planie, obowiązują następujące zasady.

- SandFly pomija reguły, przechwytywanie mowy, czynności, gesty, klawisze, pozycje menu i opcje sterowania czytaniem dodane przez skrypty. Skrypty nie otrzymują zdarzeń z SandFly.
- Wywołania wymagające `screen`, `actions`, `system` lub `dialogs` zgłaszają błąd. Dotyczy to też `sf.commands.run`, `sf.macros.run`, `sf.feedback.mute` i `sf.touch.lock`. Tych błędów SandFly nie liczy do limitu powodującego wyłączenie skryptu.
- `sf.speak` nie przerywa trwającej wypowiedzi, a `sf.speech.stop` nie zatrzymuje mowy.

Nie można wskazać SandFly w `apps`. Skrypt globalny pozostaje załadowany, więc działają jego ustawienia. Uprawnienie `java` daje pełny dostęp i wykracza poza te ograniczenia.

### Działanie przy wygaszonym ekranie {#with-the-screen-off}

Od wersji 5 skrypt z `keepLoaded = true` może pozostać w pamięci przy wygaszonym ekranie. Dotyczy to na przykład skryptu odczytującego godzinę lub przypomnienia. Użytkownik włącza **Działaj przy wyłączonym ekranie** na stronie skryptu. Po instalacji opcja jest wyłączona. Skrypt nie może jej sam włączyć. Tylko skrypty bez `apps` i `onDemand` mogą używać `keepLoaded`.

Przy wygaszonym ekranie nadal działają liczniki, żądania i `sf.speak`. Zdarzenia ekranu, takie jak `onFocus`, nie są obsługiwane. Liczniki nie budzą telefonu i mogą wykonać się później.

### Ładowanie na żądanie {#scripts-loaded-on-demand}

Od wersji 4 `onDemand = true` pozwala trzymać skrypt poza pamięcią, dopóki nie uruchomisz jednego z jego poleceń. SandFly ładuje skrypt, wykonuje polecenie, a po zakończeniu czeka jeszcze około dwóch sekund przed zwolnieniem skryptu z pamięci. Odliczanie zaczyna się, gdy skrypt nie ma żadnych otwartych okien, liczników ani oczekujących operacji, w tym wywołań [obiektów `java.proxy`](#java).

Zmienne nie przechodzą między uruchomieniami. Do trwałych danych użyj [`sf.storage`](#storage). Obsługa zdarzeń działa tylko przy załadowanym skrypcie. Skrypt ładowany na żądanie ma następujące ograniczenia.

- Musi mieć co najmniej jedno polecenie. Każde z nich ma tylko `id` i `title`. Użytkownik przypisze polecenia na ekranie **Polecenia**.
- Nie może mieć `apps`, `rules`, `navigation` ani ustawień typu przycisk.
- Nie może wywoływać `sf.bind` ani `sf.unbind`.

Zobacz [przykład wyszukiwania w Wikipedii](scripting/examples/wikipedia-search.lua).

### Uprawnienia {#permissions}

Manifest określa dostęp. Instalacja nadaje uprawnienia po pokazaniu listy. Aktualizacja zachowuje poprzednie odmowy i dodaje nowe po potwierdzeniu. Każde uprawnienie można wyłączyć na stronie skryptu.

Wywołanie bez wymaganego dostępu zgłosi błąd „This script needs the *name* permission”, gdzie *name* to nazwa uprawnienia. Procedura, reguła, czynność lub przypisanie wymagające dostępu są pomijane i pozostawiają jedno ostrzeżenie w dzienniku.

| Uprawnienie | Dostęp |
| --- | --- |
| `screen` | Czytanie ekranu, w tym `sf.screen`, odczyty [elementu](#items), `sf.speech.last()` i rozpoznawanie tekstu. Elementy przekazywane do procedur obsługi zdarzeń, poleceń i czynności mają bez tego uprawnienia wartość `nil`. Wymagają go `onFocus`, `onAnnouncement`, `onTextChange`, `onContentChange`, `onScroll`, `onSelectionChange`, `onClick`, `onLongClick`, `onInputFocus` i czynności odczytujące tekst elementu. W skrypcie globalnym obejmuje również `sf.app()`, `onAppEnter`, `onAppLeave` i `onWindowChange`. |
| `actions` | Czynności elementu, w tym `item:focus()`, `item:setText()`, `item:tap()` i pozostałe operacje dotykowe, `sf.screen.click`, `sf.touch`, `sf.keys.type`, `sf.keys.press`, `buttonActions` i czynności reguł poza odczytem. |
| `speech` | `sf.speak`, `sf.message`, `sf.speech.stop`, `sf.braille.show`, `sf.feedback.mute`, reguły modyfikujące element, `sf.rules` i zmienianie wypowiedzi, w tym `onSpeech`. |
| `passwords` | Tekst pól haseł w elementach i zdarzeniach. Bez zgody ma wartość `nil`. |
| `input` | Przypisania `gesture`, `keys`, `menu`, `control`, `sf.bind`, `sf.unbind`, `navigation`, `sf.commands.run`, `sf.keys.passAll`, `onActions` i czynności w regułach. Od wersji 6 polecenia bez własnych przypisań go nie potrzebują. Wcześniej było wymagane dla każdego polecenia. |
| `network` | `sf.fetch` i `sf.translate`. |
| `clipboard` | `sf.clipboard`. |
| `system` | `sf.system`, `sf.resume`, `sf.device.setVolume`, `sf.device.setRingerMode`, `sf.device.set` i `sf.device.media`. |
| `notifications` | `onNotification` z powiadomieniami wszystkich aplikacji. |
| `events` | `onEvent` ze zdarzeniami dostępności wszystkich aplikacji, również wpisywaniem. |
| `dialogs` | `sf.ui`. |
| `java` | Moduł [Java](#java) z LuaJava. |

Bez uprawnień można odtwarzać dźwięki, tony i wibracje, używać `sf.speech.isSpeaking`, odczytywać informacje o urządzeniu oraz używać danych, ustawień, liczników, `sf.text`, `sf.readFile`, `require`, `sf.commands.list`, dziennika, JSON i lokalizacji. `sf.app()` nie potrzebuje dodatkowej zgody w skrypcie z `apps`.

SandFly oznacza `passwords`, `network`, `clipboard`, `system`, `notifications`, `events`, `dialogs` i `java` jako uprawnienia dla zaufanych skryptów. Bez `input` SandFly odrzuci manifest z `navigation` lub z poleceniami zawierającymi własne przypisania `gesture`, `keys`, `menu` albo `control`.

### Java {#java}

Uprawnienie `java` włącza [moduł LuaJava](https://luajava.iroiro.party/api.html), między innymi `java.import`, `java.new` i `java.proxy`. Kod może wykonywać wywołania Javy z dostępem SandFly w każdej aplikacji. Pozostałe uprawnienia skryptu nie ograniczają wywołań Javy. Potrzebujesz go na przykład do operacji niedostępnych w `sf`, jak zapisanie zrzutu do pliku lub udostępnienie pliku innej aplikacji.

```lua
local Build = java.import('android.os.Build')
sf.speak('This is a ' .. Build.MODEL)
```

Gdy skrypt działa, jego stan Lua jest zajęty. Wywołanie metody `java.proxy` z innego wątku poczeka na zakończenie bieżącego wywołania. Skrypt nie powinien czekać synchronicznie na czynność, która potrzebuje wątku głównego SandFly. Po zwolnieniu skryptu z pamięci wywołanie metody obiektu z `java.proxy` nie wykona żadnej czynności. Zwróci `nil`, 0 lub `false`. Wcześniej wyrejestruj obiekty w Androidzie, na przykład w `onAppLeave`.

W skrypcie [ładowanym na żądanie](#scripts-loaded-on-demand) taki obiekt utrzymuje skrypt w pamięci do końca pierwszego wywołania albo przez 30 sekund. Może tak czekać najwyżej 100 obiektów jednocześnie. Trwale zarejestrowany słuchacz potrzebuje skryptu bez `onDemand`. Tabela lub funkcja przekazana bezpośrednio do metody nie jest liczona do limitu. Gdy potrzebujesz utrzymać skrypt na czas wywołania, użyj `java.proxy`.

## Reguły {#rules}

Reguły zmieniają elementy bez wywoływania kodu. To rozwiązanie nie wymaga uruchamiania skryptu przy każdym odczycie. Reguły działają także wtedy, gdy kod skryptu jest zajęty. Zmiany elementu wymagają `speech`, a czynności `input`.

```lua
rules = {
  { id = 'send_button', label = 'Send' },
  { match = { className = 'ImageView', parentId = 'toolbar' }, hide = true },
  { id = 'row', group = true, hint = 'Double-tap to open' },
},
```

Wskaż element przez `id`, `match` lub oba pola.

- `id` to identyfikator widoku, krótki, na przykład `send_button`, lub pełny, na przykład `com.example:id/send_button`.
- `match` przyjmuje [zapytanie](#queries).

`window` zawęża regułę do fragmentu tytułu, bez względu na wielkość liter. `activity` dopasowuje końcówkę nazwy klasy aktywności. Przed wykryciem aktywności reguła nadal może działać, tak jak przy [zasięgu skryptu](#where-a-script-runs).

Reguła musi zawierać przynajmniej jedną zmianę:

| Pole | Wartość | Działanie |
| --- | --- | --- |
| `label` | Tekst | Zastępuje nazwę elementu odczytywaną z aplikacji. |
| `speak` | Tekst | Zastępuje całą wypowiedź o elemencie, łącznie z nazwą, rolą i stanem. |
| `hide` | `true` lub `'all'` | `true` pomija element przy przesuwaniu fokusu. `'all'` pomija też jego zawartość. Ukryty element nadal jest na ekranie, więc skrypt może go kliknąć. |
| `silence` | `true` lub `'all'` | `true` wycisza zmiany, zaznaczenia i komunikaty elementu, na przykład aktualizacje paska postępu. `'all'` wycisza także zawartość, na przykład suwak w zgrupowanym wierszu. Po przejściu fokusem na element SandFly nadal go odczytuje. |
| `role` | Tekst | Zmienia rolę, na przykład na `'button'`. Nieznana wartość powoduje błąd z listą ról. |
| `state` | Tekst | Ustawia stan elementu, na przykład `'Selected'`. |
| `hint` | Tekst | Ustawia podpowiedź dotyczącą użycia elementu. |
| `heading` | `true` lub `false` | Określa, czy element jest nagłówkiem. |
| `group` | `true` | Czyta element i jego zawartość razem. Jeśli element ma własny opis lub tekst, używa go bez tekstów dzieci. |
| `readBefore`, `readAfter` | ID lub zapytanie | Przesuwa element przed innym albo za nim. Użyj jednego pola. |
| `activate` | Zapytanie | Wskazuje element wewnętrzny, który uaktywni podwójne stuknięcie, na przykład część wiersza otwierającą wpis. |
| `liftToActivate` | `true` | Uaktywnia element po uniesieniu palca, tak jak **Aktywowanie po puszczeniu**, niezależnie od stanu tej opcji. |
| `longPressAfter` | Liczba | Przy `liftToActivate` ustala od 100 do 10 000 ms, po których następuje przytrzymanie. Uniesienie już niczego nie uruchamia. Elementy bez długiego kliknięcia zachowują poprzednią obsługę. |
| `touchToActivate` | `true` | Dotyka elementu na ekranie, zamiast wysyłać kliknięcie dostępności. Wymaga Androida 11. W starszych wersjach działa zwykłe kliknięcie. |
| `read` | Lista | Od wersji 6. Zastępuje zwykłą wypowiedź tekstem z maksymalnie 12 części, oddzielonych przecinkami. |
| `actions` | Lista | Do dziesięciu [czynności](#actions-in-rules) w menu elementu. |
| `buttonActions` | `true` | Od wersji 6. Przy otwieraniu menu **Czynności** dodaje do niego maksymalnie 20 przycisków z elementu, do 8 poziomów w głąb. Nazwą jest opis, tekst lub pierwszy tekst wewnątrz. Przycisk zagnieżdżony w innym pomija się. Wymaga `actions` i `input`. |
| `skipButtons` | Lista tekstów | Od wersji 6. `buttonActions` pomija przyciski z podanym fragmentem nazwy. Do 20 wpisów, bez rozróżniania wielkości liter. |

Każda część `read` wskazuje źródło tekstu. Może mieć także opcjonalne pola `before` i `fromAbove`.

- `{ path = { 2, 1 } }` wskazuje numery kolejnych dzieci w ścieżce od elementu reguły. Numery zaczynają się od 1 i liczą tylko widoczne elementy w kolejności odczytu. Tę samą kolejność pokazuje **Elementy wewnątrz** po użyciu **Sprawdź element z fokusem**. Ścieżka może zawierać do 12 numerów.
- `{ from = query }` wskazuje pierwszy pasujący element wewnątrz. `{ from = {} }` odczytuje tekst samego elementu.
- `before = 'in reply to'` dodaje podane słowa przed częścią.
- `fromAbove = true` uzupełnia pustą część z najbliższego pasującego spośród czterech elementów wyżej na tej samej liście. Możesz tak na przykład odczytać nadawcę wiadomości.

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
| `touchHold` | ID lub zapytanie | Od wersji 6. Trzyma dotyk przez sekundę, tak jak podwójne stuknięcie i przytrzymanie. Przydaje się, gdy przycisk reaguje tylko na przytrzymany dotyk, na przykład gdy przycisk polubienia w ten sposób pokazuje reakcje. |
| `focus` | ID lub zapytanie | Przenosi fokus SandFly. |
| `scrollForward`, `scrollBackward` | ID lub zapytanie | Przewija element. |
| `speak` | Tekst albo `{ textOf = item }` | Czyta podany tekst lub treść elementu określonego identyfikatorem albo zapytaniem. `textOf` wymaga `screen`. |
| `steps` | Lista | Od wersji 6. Do dziesięciu kroków wykonywanych po kolei. |

Wszystkie poza `speak` wymagają `actions`. Od wersji 5 możesz dodać `first = true`, żeby czynność pojawiała się przed czynnościami aplikacji, na przykład `{ title = 'Archive', click = 'archive_button', first = true }`. Domyślnie czynności skryptu pojawiają się po czynnościach aplikacji.

SandFly najpierw szuka wewnątrz elementu, do którego należy menu, potem w całym oknie. Jeśli znajduje na przykład napis przycisku, idzie do najbliższego rodzica obsługującego działanie, do sześciu poziomów wyżej. Gdy nie ma dopasowania, odtwarza dźwięk i wpisuje ostrzeżenie do dziennika. Jeśli w każdym wierszu jest taki sam przycisk, użyj [onActions](#hooks) i wyszukiwania od wiersza, żeby uniknąć kliknięcia pierwszego przycisku z okna.

Od wersji 6 krok w `steps` ma postać `{ click = item, where = 'item' }`. Zamiast `click` możesz podać `longClick`, `touchHold`, `focus`, `scrollForward` lub `scrollBackward`. Wartością jest zapytanie, lista do sześciu kolejnych prób albo `{}` dla samego elementu czynności. Domyślne `where = 'item'` szuka wewnątrz niego. `where = 'screen'` szuka w oknach aplikacji, na przykład w menu z poprzedniego kroku. SandFly zamyka własne menu i czeka do 2 sekund na każdy krok. Po niepowodzeniu odtwarza dźwięk i naciska Wstecz, jeśli wcześniejszy krok się wykonał.

```lua
{ title = 'Add to queue', steps = {
  { longClick = {} },
  { click = { text = 'Add to queue' }, where = 'screen' },
} }
```

### Jak sprawdzić dane elementu {#find-the-details-of-an-item}

Do napisania reguły lub zapytania potrzebujesz identyfikatora, klasy, roli i tekstów elementu. Pokazują je trzy pozycje w kategorii **Ustawienia programisty** na ekranie **Polecenia**. Możesz przypisać im gest albo dodać je do menu SandFly.

- **Sprawdź element z fokusem** otwiera **Szczegóły elementu**. W **Elementy wewnątrz** znajdziesz listę zawartości elementu w kolejności odczytu. Przy każdej pozycji zobaczysz poziom, klasę, klikalność i teksty. Poziom 1 oznacza bezpośrednie dzieci. Dzięki tym danym możesz odróżnić zwykły tekst od przycisku z takim samym napisem.
- **Kopiuj szczegóły elementu** kopiuje te dane do schowka.
- **Kopiuj drzewo ekranu** kopiuje drzewo wszystkich elementów okna. Na początku podaje aplikację, aktywność i tytuł okna. Potem zapisuje po jednym wierszu na element, zawsze przed jego dziećmi. Każdy wiersz podaje poziom, klasę, ID, rolę, stany, teksty, nazwane działania i pasujące reguły skryptów. Przykład to `level 5: Button; id: send; clickable, focusable; description: Send`. Poziom 0 to najwyższy element okna. Rodzicem pozycji jest najbliższy wcześniejszy wiersz o niższym poziomie.

## Zapytania {#queries}

Zapytania służą do wyszukiwania w regułach, nawigacji i kodzie. Podaj ID jako tekst lub rekord. Wszystkie pola rekordu muszą pasować.

| Pole | Dopasowanie |
| --- | --- |
| `id` | Identyfikator widoku. Krótkie `send` pasuje do końcówki po `:id/`. ID zawierające dwukropek wymaga pełnej zgodności. |
| `text` | Dokładny tekst elementu. |
| `contentDescription` | Dokładny opis zawartości. |
| `textContains` | Fragment tekstu lub opisu, niezależnie od wielkości liter. |
| `className` | Końcówka klasy. `'Button'` pasuje do `android.widget.Button`. |
| `role` | Rola wynikająca z klasy elementu, tak jak w polu `role` obiektu, na przykład `'button'` lub `'check_box'`. |
| `clickable` | `true` lub `false`. |
| `parentId` | Krótkie lub pełne ID rodzica. |
| `inside` | Jeden z przodków musi pasować do podanego zapytania. Na przykład `{ className = 'Button', inside = { textContains = 'play video' } }` pasuje do przycisków wewnątrz wiersza filmu. Sam wiersz nie pasuje. W środku nie można użyć `inside` ani `has`. Od wersji 2 możesz podać listę do czterech zapytań. Każde musi pasować do któregoś z przodków. Na przykład `{ clickable = true, inside = { { clickable = true }, { id = 'results' } } }` wybiera klikalne elementy wewnątrz innych klikalnych elementów na liście `results`. Pomija same wiersze listy. |
| `has` | Sprawdza, czy pasuje któryś z elementów wewnątrz, do sześciu poziomów i 60 pozycji, od najbliższych. Na przykład `{ parentId = 'results', has = { textContains = 'Subscribe to' } }` znajduje wiersz listy zawierający przycisk subskrypcji. W środku nie można użyć `has` ani `inside`. Od wersji 6 `hasDepth` ogranicza szukanie do od 1 do 6 poziomów. Wartość 1 sprawdza tylko bezpośrednie dzieci, dzięki czemu może pasować sam wiersz, a zawierająca całą listę karta już nie. |
| `checkable`, `checked`, `longClickable`, `focusable`, `focused`, `selected`, `enabled`, `editable`, `scrollable`, `visible`, `heading`, `password` | Od wersji 2. Wartości logiczne, na przykład `{ className = 'CheckBox', checked = false }` dla niezaznaczonych pól. |
| `hint`, `stateDescription` | Od wersji 6. Dokładna podpowiedź lub opis stanu. |
| `contains` | Od wersji 6. Fragment tekstu, opisu zawartości, podpowiedzi, stanu lub dymka. Wielkość liter nie ma znaczenia. Działa jak **Dowolny tekst** w teście makra. Użyj `field`, żeby sprawdzać tylko jedno pole. |
| `matches` | Od wersji 6. Szuka wyrażenia regularnego w tekście, opisie zawartości, podpowiedzi, stanie lub dymku. Wielkość liter nie ma znaczenia, tak jak przy polu **Wyrażenie regularne** w teście makra. Przy `field` szuka tylko we wskazanym polu. Na przykład `{ matches = '^new post$', field = 'contentDescription' }` pasuje do elementu, którego cały opis to „New post”. Powtarzane grupy nie mogą zawierać powtórzeń ani alternatyw (`\|`). W każdym polu sprawdzane jest tylko pierwsze 1000 znaków. |
| `field` | Od wersji 6. Zawęża `contains` i `matches` tak jak opcja **Dopasuj w** w teście makra. Przyjmuje `'text'`, `'contentDescription'` lub `'description'`, `'hint'`, `'stateDescription'` lub `'state'`, `'tooltip'` albo `'id'`. Identyfikator jest porównywany z nazwą pakietu i bez niej. Na przykład `'^send$'` pasuje do `com.example:id/send`. |
| `kind` | Od wersji 6. Typ elementu z testu makra lub lista typów, z których wszystkie muszą pasować. Dozwolone są `'clickable'`, `'longClickable'`, `'editable'`, `'scrollable'`, `'adjustable'`, `'checkable'`, `'checked'`, `'selected'`, `'heading'`, `'enabled'`. Typy klikalne sięgają do przodków, najwyżej sześć poziomów. `'scrollable'` sprawdza czynność przewinięcia, a `'adjustable'` suwak lub pasek postępu. Na przykład `{ contains = 'new post', field = 'description', kind = 'clickable' }` odpowiada testowi makra szukającemu klikalnego elementu z „new post” w opisie. |

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
| `checkable`, `checked`, `clickable`, `longClickable`, `focusable`, `focused`, `accessibilityFocused`, `selected`, `enabled`, `editable`, `password`, `scrollable`, `visible`, `heading` | `true` lub `false`. `focused` to fokus klawiatury, a `accessibilityFocused` to fokus SandFly. |
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
- `item:find(query)` zwraca pierwszy element pasujący do [zapytania](#queries). Przeszukuje element i całą jego zawartość. Jeśli brak dopasowania, zwraca `nil`. `item:findAll(query, limit)` zwraca listę maksymalnie `limit` dopasowań. Domyślnie jest to 50, a maksymalna wartość to 200.
- `item:refresh()` pobiera nową migawkę lub zwraca `nil`, jeśli element zniknął.
- Od wersji 2 `item:labeledBy()` wskazuje element etykiety, a `item:labelFor()` element opisywany przez tę etykietę. Jeśli go nie ma, zwraca `nil`.
- Od wersji 2 `item:recognizeText()` [rozpoznaje tekst](#recognizing-text) w obszarze elementu.

### Wykonywanie czynności na elemencie {#acting-on-an-item}

Te metody wymagają `actions`. Jeśli mogą [poczekać](#waiting), zwracają `true` lub `false`. W wywołaniu, które musi odpowiedzieć od razu, na przykład w procedurze mowy, wykonują działanie i zwracają `nil`.

- `item:click()`, `item:longClick()`, `item:scrollForward()`, `item:scrollBackward()` klikają, przytrzymują i przewijają.
- Od wersji 2 `item:showOnScreen()`, `item:expand()`, `item:collapse()`, `item:dismiss()` pokazują, rozwijają, zwijają i odrzucają.
- `item:focus()` przenosi fokus SandFly na element.
- `item:setText(text)` zastępuje tekst w polu edycji.
- Od wersji 2 `item:setSelection(start, end)` zaznacza tekst od pozycji liczonych od 0. Bez `end` przesuwa sam kursor.
- Od wersji 2 `item:moveCursor(unit, direction, extend)` przechodzi po `'character'` (domyślnie), `'word'`, `'line'`, `'paragraph'` lub `'page'`, w kierunku `'next'` (domyślnie) lub `'previous'`. `extend = true` zaznacza mijany tekst.
- Od wersji 2 `item:setProgress(value)` ustawia wartość suwaka od `range.min` do `range.max`.
- Od wersji 2 `item:scrollTo(row, column)` przewija listę lub siatkę do wskazanego wiersza i kolumny. Numeracja zaczyna się od 1.
- Od wersji 2 `item:nextHtmlElement(element, direction)` przechodzi do następnego lub poprzedniego elementu określonego rodzaju w treści WWW, na przykład `'HEADING'`, `'LINK'`, `'BUTTON'`, `'LANDMARK'` albo `'TABLE'`.
- Od wersji 2 `item:tap()`, `item:doubleTap()` i `item:longPress(ms)` dotykają środka elementu tak, jak zrobiłby to palec. Użyj ich dla elementów bez czynności kliknięcia. Domyślny czas `ms` to 1000 milisekund. Metody działają tak samo jak [`sf.touch`](#touch-and-keys). Przekazanie elementu, który nie ma położenia na ekranie, zgłasza błąd.

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

SandFly pamięta do 500 ostatnio używanych elementów. Użycie starszego elementu powoduje błąd „That item is no longer available”.

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

Jeśli procedura musi odpowiedzieć natychmiast, tak jak `onFocus`, wywołania zachowują się na dwa sposoby.

- Czynności elementów, dotyk, klawisze, `sf.commands.run`, `sf.device.set`, `sf.system.openApp`, działania systemowe i `sf.playSound` nadal wykonują działanie i zwracają `nil`.
- Pozostałe wywołania oczekujące zgłaszają błąd „This waits for an answer, so call it from an event handler, a command, a timer or sf.run”.

Użyj `sf.run(function() ... end)`, żeby wykonać operację oczekującą w zadaniu.

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
| `sf.onNotification` | Przed odczytem powiadomienia lub krótkiego komunikatu Androida. | `{ package, toast, title, text, category }` i tekst odczytu. `toast` ma wartość `true` dla krótkiego komunikatu typu toast. | `notifications` |
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

Zadeklaruj polecenie w manifeście i dodaj obsługę w kodzie. Użytkownik może je wywołać gestem, klawiszami, z menu SandFly, ze sterowania czytaniem albo przez własne przypisanie na ekranie **Polecenia**.

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
| `id` | Wymagany i unikatowy w skrypcie. Do 64 liter, cyfr, kropek (`.`), łączników (`-`) i podkreśleń (`_`). |
| `title` | Wymagana nazwa pokazywana na ekranie **Polecenia**, w menu i przy sterowaniu czytaniem. |
| `gesture` | Nazwa [gestu](#gestures) lub lista nazw. |
| `keys` | [Klawisze](#keys) lub ich lista. |
| `menu` | `true` dodaje do menu SandFly. |
| `control` | `true` dodaje do sterowania czytaniem. Ruch w górę lub w dół wywołuje polecenie z `direction` równym `'previous'` lub `'next'`. |

`gesture`, `keys`, `menu` i `control` wymagają `input`. Od wersji 6 polecenie zawierające tylko `id` i `title` nie potrzebuje tego uprawnienia. Wcześniej wymagało go każde polecenie. Skrypt wykonujący zadanie po użyciu polecenia, na przykład włączający latarkę, deklaruje tylko uprawnienia potrzebne do wykonania zadania. Manifest mieści do 50 poleceń.

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

Zwróć `false` przed czekaniem na jakikolwiek wynik, żeby SandFly wykonał standardowe przypisanie gestu lub klawiszy. Skróty skryptu mają pierwszeństwo przed zwykłymi przypisaniami. Wyjątkiem jest gest **Włącz lub wyłącz wszystkie skrypty**. Gdy dwa skrypty używają tego samego gestu lub klawiszy, pierwszeństwo mają skrypty aplikacji. Później liczy się kolejność ustawiona przez **Uruchamiaj wcześniej** i **Uruchamiaj później** na stronach skryptów. Użytkownik może na stronie skryptu wyłączyć lub zmienić każdy gest i skrót.

Od wersji 4 wszystkie polecenia włączonego skryptu pojawiają się w **Polecenia** > **Skrypty**. Każda pozycja zawiera nazwę skryptu i polecenia, na przykład **Hello: Say hello**. Tak jak przy poleceniach SandFly użytkownik może przypisać pozycję do gestu lub skrótu, dodać ją do menu SandFly, własnego menu lub własnej opcji sterowania czytaniem albo przypisać do gestu w aplikacji. Takie przypisania mają `event.via = 'command'` i nie pojawiają się na stronie skryptu.

Jeśli polecenie uruchomi się przy wyłączonym skrypcie, poza aplikacjami, w których ten działa, albo na ekranie SandFly, usłyszysz komunikat o niedostępności skryptu na tym ekranie.

### Skróty klawiszowe {#keys}

Podaj jedną kombinację albo dwie oddzielone przecinkiem. Elementy kombinacji łączy `+`.

- `sf` to klawisz SandFly. Od wersji 2 można go pominąć.
- Modyfikatory to `shift`, `ctrl` lub `control`, `alt` i `meta` lub `search`.
- Możesz podać literę, cyfrę lub jeden z symboli (`.`, `/`, `;`, `'`, `-`, `=`, `[`, `]`, `\` lub znak grawisu). Możesz też użyć nazwy klawisza Androida bez prefiksu `KEYCODE_`, na przykład `space`, `enter`, `tab` lub `f5`, albo jednej z nazw `up`, `down`, `left`, `right`, `backspace`, `delete`, `esc`, `home`, `end`, `pageup`, `pagedown` i `return`.

Przykłady to `'sf+shift+n'`, `'sf+t, s'`, `'k'`, `'shift+n'`, `'space'` i `'t, s'`. `sf` może być tylko w pierwszej kombinacji. Po pierwszej części SandFly gra dźwięk i czeka 3 sekundy. Jeśli drugi klawisz nie uruchamia żadnego polecenia, SandFly odtwarza inny dźwięk.

Od wersji 2 skróty bez `sf` są pomijane w polu edycji. Poza nim SandFly czeka na procedurę do 30 ms. Żeby klawisz trafił do aplikacji, procedura musi w tym czasie zwrócić `false`, zanim zacznie czekać na wynik. Po tym czasie `false` nie przekaże już klawisza do aplikacji. Przytrzymanie powtarza polecenie przy każdym powtórzeniu klawisza. Włączona nawigacja strzałkami ma pierwszeństwo dla strzałek.

### Przypisywanie gestów i klawiszy w kodzie {#assign-gestures-and-keys-from-code}

`sf.bind(command, assignment)` dodaje, a `sf.unbind(command, assignment)` usuwa przypisanie. Podaj `{ gesture = name }` albo `{ keys = keys }`. Zmiana jest trwała i widać ją na stronie skryptu. Funkcje wymagają `input`. Funkcje zgłaszają błąd przy nieznanym geście lub skrócie, poleceniu niezadeklarowanym w manifeście albo skrypcie ładowanym na żądanie.

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

**`sf.speak(text, options)`** odczytuje `text`. W rekordzie `options` możesz podać poniższe pola.

- `interrupt` to wartość logiczna, domyślnie `false`. Gdy ustawisz `true`, funkcja przerywa obecny odczyt. Bez tego wypowiedź czeka w kolejce.
- `rate` i `pitch` mnożą ustawienia użytkownika przez 0,25 do 4. Domyślnie jest to 1.
- `language` określa język tekstu. Podaj znacznik, na przykład `'de'`.
- Gdy ustawisz `wait` na `true`, funkcja [czeka](#waiting) na zakończenie. Zwraca `true` przy pełnym odczycie albo `false` po przerwaniu, wyciszeniu lub upływie 2 minut.

Funkcja działa podczas odtwarzania multimediów, rozmowy telefonicznej, nagrywania mikrofonem przez inną aplikację i dyktowania. Przy wyciszonej mowie nic nie wypowiada. Potrzebuje `speech`.

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
| `sf.speech.last()` | Od wersji 2. [Czeka na wynik](#waiting) i zwraca tekst ostatniej wypowiedzi SandFly lub `nil`. Wymaga `screen`. |
| `sf.braille.show(text)` | Od wersji 2. Przez chwilę pokazuje do 1000 znaków na podłączonym monitorze, tak jak SandFly pokazuje komunikaty. Wymaga `speech`. |
| `sf.playSound(name)` | Odtwarza nazwany dźwięk lub plik pakietu, na przykład `'sounds/chime.wav'`. W zadaniu zwraca informację, czy dźwięk został odtworzony. W przypadku pliku czeka na koniec odtwarzania. |
| `sf.playTone(frequency, ms, options)` | Odtwarza ton sinusoidalny. `frequency` podaj w hercach, domyślnie 440. `ms` to czas w milisekundach, domyślnie 100. `options.volume` ma zakres od 0 do 1, domyślnie 0,5. |
| `sf.playAudio(samples, options)` | Odtwarza próbki od -1 do 1, do 10 sekund. `options.sampleRate` od 8000 do 48 000, domyślnie 22 050. `options.volume` od 0 do 1, domyślnie 1. |
| `sf.vibrate(pattern)` | Czas w ms, domyślnie 50, albo lista do 20 wartości określających na przemian wibrację i przerwę. Pierwsza wartość to czas wibracji. Pojedynczy czas to do 2000 ms, suma do 5000 ms. |

Nazwy wbudowanych dźwięków to `focus`, `actionable`, `click`, `longClick`, `scroll`, `listEnter`, `listExit`, `end`, `enter`, `typo` i `beep`. Dźwięki, tony i wibracje nie wymagają uprawnień. Stosują ustawienia **Sygnał dźwiękowy** i **Sygnał wibracyjny**. Podczas wstrzymania lub wyciszenia SandFly nie są odtwarzane. Skrypt może odtwarzać równocześnie maksymalnie cztery pliki, tony i strumienie próbek. Następne są pomijane.

Od wersji 6 `sf.feedback.mute(what, ms)` wycisza mowę, dźwięki lub wibracje do 10 000 ms. W rekordzie `what` ustaw co najmniej jedno z `speech`, `sounds`, `vibration` na `true`. Trwająca mowa zostaje przerwana. W czasie wyciszenia nowe wypowiedzi, dźwięki i wibracje są pomijane. Dotyczy to również `sf.speak` i dźwięków samego skryptu. Nic z tego nie trafia do kolejki. Uprawnienie `speech` jest wymagane. Wywołaj `release()` na zwróconym uchwycie, żeby zakończyć wyciszenie.

Od wersji 6 `sf.feedback.unmute()` kończy wyciszenie rozpoczęte przez ten skrypt.

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
| `sf.screen.windows()` | Od wersji 2. Lista okien od najwyższego, bez okien SandFly, takich jak menu SandFly. Każde okno ma pola `{ id, type, title, layer, active, focused, bounds, root }`. `root` jest najwyższym elementem okna. Przez `window.root:find(query)` możesz szukać w innym oknie, na przykład klawiaturze. |
| `sf.screen.waitFor(query, timeout, interval)` | Od wersji 2. [Czeka](#waiting), aż pojawi się dopasowanie w aktywnym oknie. Zwraca znaleziony element lub `nil` po upływie `timeout` milisekund. Domyślnie czeka 5000 ms, najwyżej 60 000. Sprawdza co `interval` milisekund, domyślnie co 250, co najmniej co 100. |
| `sf.screen.click(query)` | Od wersji 2. Szuka elementu lub przyjmuje element i go klika, a bez czynności kliknięcia dotyka. Zwraca powodzenie lub `false`, gdy nie ma wyniku. Wymaga też `actions`. |
| `sf.screen.recognizeText()` | Od wersji 2. [Rozpoznaje tekst](#recognizing-text) na całym ekranie. |

Typy okien to `'application'`, `'inputMethod'`, `'system'`, `'accessibilityOverlay'`, `'splitScreenDivider'` i `'magnificationOverlay'`.

**`sf.app()`** opisuje aplikację na pierwszym planie jako `{ package, label, activity, window, meta }`. `label` to nazwa aplikacji. `activity` może mieć wartość `nil` do chwili zmiany okna. `window` to tytuł, a `meta` zawiera tekstowe, liczbowe lub logiczne dane manifestu, na przykład `sf.app().meta['com.example.flag']`. Skrypt globalny potrzebuje `screen`.

```lua
sf.run(function()
  local button = sf.screen.waitFor({ text = 'Continue' }, 10000)
  if button then button:click() else sf.speak('No Continue button') end
end)
```

### Rozpoznawanie tekstu {#recognizing-text}

Od wersji 2 `sf.screen.recognizeText()` i `item:recognizeText()` rozpoznają tekst ze zrzutu lokalnie. Wybór pisma zależy od języka mowy i urządzenia. Dostępne są alfabet łaciński, chiński, japoński, koreański i dewanagari.

Wywołanie [czeka na wynik](#waiting) i wymaga `screen` oraz Androida 11 lub nowszego. Zwraca `{ text, blocks }`. Blok to `{ text, bounds, lines }`, a wiersz `{ text, bounds }`. Współrzędne są w pikselach. Możesz ich użyć do dotknięcia wyniku przez `sf.touch.tap`.

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

Dotykanie ekranu wymaga Androida 11 lub nowszego. Punkty to współrzędne ekranu w pikselach, od 0. Podczas dotyku wykonywanego przez skrypt dotknięcia trafiają bezpośrednio do aplikacji. Dzięki temu SandFly nie rozpoznaje dotknięć skryptu jako swoich gestów.

- `sf.touch.tap(x, y)` dotyka punktu.
- `sf.touch.doubleTap(x, y)` dotyka go dwa razy.
- `sf.touch.longPress(x, y, ms)` przytrzymuje, domyślnie przez 1000 ms.
- `sf.touch.swipe(x1, y1, x2, y2, ms)` przesuwa od punktu do punktu, domyślnie w 300 ms.
- `sf.touch.gesture(strokes)` wykonuje do dziesięciu ruchów jednocześnie. Służy do gestów wykonywanych kilkoma palcami. Każdy ma `{ points, start, duration }`. Podaj od 1 do 100 punktów `{ x, y }`. Długość ruchu to od 1 do 10 000 ms, domyślnie 50. Początek liczy się od startu gestu, od 0 do 10 000 ms.

```lua
-- A two-finger swipe up in the middle of a 1080-pixel-wide screen.
sf.touch.gesture({
  { points = { { x = 440, y = 1600 }, { x = 440, y = 800 } }, duration = 300 },
  { points = { { x = 640, y = 1600 }, { x = 640, y = 800 } }, duration = 300 },
})
```

Od wersji 6 `sf.touch.lock(ms)` blokuje dotyk przez `ms` milisekund, najwyżej 10 000. Dotknięcia i gesty użytkownika w tym czasie nie działają ani nie trafiają do aplikacji. Gesty skryptu z `sf.touch` nadal do niej trafiają. Funkcja zwraca uchwyt z metodą `release()`. Metoda kończy blokadę. Możesz też wywołać `sf.touch.unlock()`, żeby zakończyć blokadę rozpoczętą przez ten skrypt.

Blokada kończy się, gdy skrypt zostanie zwolniony z pamięci, zgłosi błąd lub aplikacja, w której ją włączono, przestanie być widoczna. Użytkownik może ją zakończyć, naciskając oba klawisze głośności jednocześnie lub wykonując **Zablokuj lub odblokuj dotyk**. Nowa blokada zastępuje poprzednią także wtedy, gdy pochodzi z innego skryptu. Blokada jest niedostępna na ekranach SandFly. Do jej działania wymagany jest Android 13 lub nowszy oraz włączone **Używaj gestów w SandFly**.

### Pisanie i naciskanie klawiszy {#type-and-press-keys}

`sf.keys.type(text)` wpisuje do 2000 znaków w polu z fokusem klawiatury, w miejscu kursora, i zastępuje zaznaczenie. W polu hasła lub bez pola korzysta z Shizuku. Nowy wiersz i tabulator przechodzą w Enter i Tab. Inne znaki sterujące są pomijane.

`sf.keys.press(keys)` naciska przez Shizuku od 1 do 100 klawiszy po kolei. Podaj listę albo nazwy oddzielone spacją. Możesz użyć nazw Androida z `KEYCODE_` lub bez niego, niezależnie od wielkości liter. Możesz na przykład podać `'enter'`, `'tab'` lub `'KEYCODE_MEDIA_PLAY_PAUSE'`. Dostępne są też `up`, `down`, `left`, `right`, `center`, `backspace`, `delete`, `esc`, `return`, `home`, `end`, `pageup`, `pagedown`, `playpause`, `next`, `previous`, `volumeup` i `volumedown`. `home` i `end` odnoszą się do wiersza. Aby przejść na ekran główny, użyj `sf.system.home()`.

Bez uruchomionego Shizuku i zgody dla SandFly operacje wymagające Shizuku zgłoszą błąd. Zobacz ustawienia **Shizuku i root**.

```lua
sf.keys.type('Meeting at 3')
sf.keys.press('enter')
```

### Przekazywanie wszystkich klawiszy do aplikacji {#pass-every-key-to-the-app}

Od wersji 3 `sf.keys.passAll(on)` przekazuje wszystkie klawisze Androidowi i aplikacji na pierwszym planie, tak jak polecenie **Włącz lub wyłącz obsługę klawiatury przez SandFly**. Dotyczy to klawisza SandFly i jego skrótów, klawiszy trybu przeglądania, nawigacji strzałkami, kursora przeglądu oraz skrótów wszystkich skryptów, w tym bieżącego. Nadal działa przypisanie samego polecenia **Włącz lub wyłącz obsługę klawiatury przez SandFly**.

Użyj `sf.keys.passAll()` lub `sf.keys.passAll(true)`, żeby zacząć przekazywanie. `sf.keys.passAll(false)` je kończy. Wymagane jest `input`.

Przekazywanie działa, dopóki skrypt jest w pamięci. Skrypt z `apps` kończy przekazywanie po opuszczeniu jego aplikacji. Skrypt działający we wszystkich aplikacjach przekazuje klawisze, dopóki nie zakończy pracy. Ekrany SandFly nie przekazują klawiszy w tym trybie. Użycie skrótu **Włącz lub wyłącz obsługę klawiatury przez SandFly** kończy przekazywanie aż do ponownego wywołania `sf.keys.passAll`. SandFly nie ogłasza początku ani końca przekazywania ze skryptu. Dodaj więc własny komunikat dla użytkownika.

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

Od wersji 6 **`sf.macros.run(file, values)`** uruchamia makro z pliku w pakiecie, na przykład `'macros/reply.json'`, tak jak SandFly uruchamia makra użytkownika. Plik może mieć do 1 MB i korzysta z tego samego formatu co **Eksportuj do pliku**. SandFly uruchamia pierwsze makro z pliku. Zobacz [Makra w opisie formatu plików](item-files.md#macros).

- Jeśli inne makro już działa, zostanie zatrzymane. Funkcja zwraca `true` od razu. Kroki są wykonywane po wznowieniu skryptu, więc możesz wywołać funkcję z dowolnej procedury obsługi zdarzenia.
- `values` to rekord tekstów wstawianych w miejscach `{name}` w krokach, na przykład `{ text = item.text }`. Możesz podać do 200 nazw, każdą do 64 znaków. Używaj liter, cyfr, kropek (`.`), łączników (`-`) i podkreśleń (`_`). Każda wartość może mieć do 10 000 znaków. Makro otrzymuje też wartości zapisane przez makra tego skryptu krokami **Ustaw wartość**, **Pobierz z linku** i **Przetłumacz tekst**.
- Wywołanie zgłasza błąd na ekranie SandFly, przy pliku bez makra lub jeśli makro używa polecenia **Włącz lub wyłącz wszystkie skrypty** albo **Włącz lub wyłącz obsługę klawiatury przez SandFly**.

Skrypt potrzebuje uprawnień kroków:

| Uprawnienie | Czynności makra |
| --- | --- |
| `screen` | Czytanie ekranu i warunki, także `{item}`. |
| `screen` razem z `actions` | Klika, przytrzymuje, przenosi fokus lub działa na znalezionym elemencie. |
| `actions` | Dotyk w punkcie, wpisywanie i naciskanie klawiszy. |
| `speech` | Czytanie tekstu. |
| `input` | Uruchamianie poleceń. Obowiązują również uprawnienia poszczególnych poleceń, tak jak przy [`sf.commands.run`](#run-sandfly-commands). |
| `system` | Otwieranie aplikacji, adresów i ustawień, udostępnianie, zmiany głośności i trybu dzwonka, opcje urządzenia, multimedia, alarmy, minutniki, połączenia, wiadomości i aparat. |
| `clipboard` | Kopiowanie i `{clipboard}`. |
| `dialogs` | Wyświetlanie komunikatu. |
| `network` | Pobieranie i tłumaczenie. |

**`sf.macros.stop()`** zatrzymuje makro uruchomione przez ten skrypt. Kreator umieszcza kroki poleceń i wyzwalaczy w plikach `macros/` i uruchamia je przez `sf.macros.run`.

## Uruchamianie poleceń SandFly {#run-sandfly-commands}

Od wersji 2 skrypt może uruchamiać polecenia, którym w SandFly można przypisać gest lub klawisze. Może na przykład przejść do następnego nagłówka, rozpocząć czytanie od góry lub zmienić sterowanie czytaniem.

**`sf.commands.list()`** zwraca rekordy `{ id, title, category }` w kolejności z ekranu **Polecenia** w ustawieniach SandFly. Lista pomija **Włącz lub wyłącz wszystkie skrypty**, **Włącz lub wyłącz obsługę klawiatury przez SandFly**, **Otwórz aplikację**, skróty do aplikacji oraz polecenia skryptów.

**`sf.commands.run(id)`** uruchamia działanie z listy i zwraca wynik, tak jak jego gest. Wymaga `input`. Na ekranie SandFly albo przy nieznanym ID powoduje błąd.

Niektóre polecenia wymagają dodatkowych uprawnień.

- `system` jest potrzebne do wstrzymania lub wyciszenia SandFly oraz do zmiany odczytu powiadomień. Dotyczy też poleceń systemowych, takich jak Wstecz, ekran główny, ostatnie aplikacje, poprzednia aplikacja, powiadomienia, szybkie ustawienia, zrzuty, blokada ekranu, menu zasilania, wszystkie aplikacje i przycisk ułatwień dostępu. Wymagają go również odbieranie połączeń, regulacja głośności, zmiana klawiatury, użycie mikrofonu do dyktowania lub poleceń głosowych, przełączanie powiększenia, monitora brajlowskiego, przekazywania gestu i dotyku bezpośredniego oraz polecenia SandFly Pro.
- `clipboard` jest potrzebne do kopiowania, wycinania, wklejania i dodawania do schowka. Wymagają go również kopiowanie tekstu z ekranu, ostatniej wypowiedzi, drzewa ekranu lub szczegółów elementu, zaznaczanie ostatniej wypowiedzi oraz otwieranie schowka lub historii mowy.

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

`sf.storage` zachowuje dane, dopóki skrypt jest zainstalowany. Dane pozostają dostępne po ponownym załadowaniu i aktualizacji. Klucz ma od 1 do 200 znaków. Wartością może być tekst, liczba, `true`, `false` lub tabela z takimi wartościami.

- **`sf.storage.get(key, fallback)`** zwraca zapisaną wartość lub `fallback`.
- **`sf.storage.set(key, value)`** zapisuje. `nil` usuwa wartość. Błąd „Storage is full” pojawia się, gdy klucze i wartości skryptu zapisane jako tekst JSON przekroczyłyby 524 288 znaków.
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
| `key` | Wymagany i unikatowy. Do 64 liter, cyfr, kropek (`.`), łączników (`-`) i podkreśleń (`_`). |
| `type` | `'switch'` (domyślnie), `'list'`, `'text'`, `'number'` albo `'button'`. |
| `title` | Wymagana nazwa. |
| `summary` | Tekst pod nazwą. Lista pokazuje zamiast niego bieżącą opcję. |
| `default` | Wartość początkowa. Bez pola to `false`, pierwsza opcja listy, `''` lub 0, zależnie od typu. |
| `options` | Wymagana dla listy. Wartości lub rekordy `{ value, label }`. |

Przycisk nie ma wartości. Jego naciśnięcie wywołuje [`sf.onButton`](#hooks) z kluczem przycisku. Przy wyłączonym skrypcie pojawi się komunikat o konieczności włączenia skryptu.

- **`sf.settings.get(key)`** zwraca wartość. Brak klucza w manifeście lub klucz przycisku powoduje błąd.
- **`sf.settings.all()`** zwraca rekord wszystkich wartości.
- **`sf.settings.set(key, value)`** zmienia i wywołuje `onSettingChange`. Wartość jako JSON ma do 16 384 znaków.
- **`sf.settings.setOptions(key, options)`** zastępuje od 1 do 500 opcji listy, do 16 384 znaków łącznie.
- **`sf.settings.setTitle(key, text)`** i **`sf.settings.setSummary(key, text)`** od wersji 3 zmieniają tytuł lub opis widoczny przy ustawieniu. Możesz na przykład ustawić przyciskowi tytuł „Miejsce 1: Przewiń wstecz, YouTube”. Tekst może mieć do 500 znaków. `nil` przywraca tekst manifestu.

## Okna i komunikaty {#dialogs-and-windows}

Każde okno wymaga `dialogs`. Tytuł każdego okna ma postać „nazwa skryptu: tytuł”. Dzięki nazwie można odróżnić okno skryptu od okna aplikacji lub systemu. Tekst jest przycinany do 20 000 znaków.

| Funkcja | Działanie |
| --- | --- |
| `sf.ui.alert(message, options)` | Pokazuje komunikat z **OK** i czeka na zamknięcie. `options.title` podaje tytuł. |
| `sf.ui.confirm(message, options)` | Pokazuje komunikat z przyciskami **OK** i **Anuluj**. Zwraca `true` po wybraniu **OK**, w pozostałych przypadkach `false`. Opcje to `title` z tytułem oraz `ok` i `cancel` z napisami przycisków. |
| `sf.ui.prompt(title, options)` | Pokazuje pole edycji. Zwraca wpisany tekst lub `nil` po anulowaniu. Opcje to początkowa treść `text` i podpowiedź `hint`. |
| `sf.ui.choose(title, choices, options)` | Pokazuje listę 1–500 wartości lub rekordów `{ value, label }`. Zwraca wybór lub `nil`. `options.selected` ustawia wartość początkową. Powyżej 10 pozycji dodaje filtr. |
| `sf.ui.text(title, text)` | Od wersji 2. Pokazuje tekst do przeglądania po znakach, słowach i wierszach. Tekst można zaznaczać. Funkcja czeka na zamknięcie. Użyj jej na przykład, gdy wynik jest za długi do odczytania na głos. |
| `sf.ui.dialog(form)` | Otwiera formularz i zwraca go od razu. |
| `sf.ui.window(form)` | Od wersji 2. Otwiera formularz w pełnoekranowym oknie. Przydaje się w skryptach działających jak osobne aplikacje, na przykład w notatniku. Wstecz zamyka okno. |

Wszystkie te metody poza `dialog` i `window` [czekają na wynik](#waiting). Od wersji 2 okno może otworzyć się nad innym otwartym oknem. Na przykład `sf.ui.confirm` możesz wywołać z pełnoekranowego okna, które pozostanie otwarte pod potwierdzeniem. Możesz mieć maksymalnie osiem otwartych okien. Otwarcie następnego zamyka najstarsze. Zwolnienie skryptu z pamięci zamyka jego okna, na przykład po opuszczeniu aplikacji. Wartości wpisane przez użytkownika możesz zachować w [`sf.storage`](#storage).

### Formularze {#forms}

Formularz to `{ title, items, onClose }`. Podaj od 1 do 50 pozycji. `onClose` dostaje wartości po zamknięciu. Każda pozycja ma `type` i niepowtarzalne `id`. Brak ID daje `item1`, `item2` i tak dalej. Zwykle dodasz też `label`. `enabled = false` wyłącza pozycję, a `visible = false` ją ukrywa.

| Typ | Pozostałe pola |
| --- | --- |
| `label` | `label` zawiera tekst. Od wersji 2 `selectable = true` pozwala czytać i zaznaczać znaki, słowa i wiersze. |
| `heading` | Od wersji 2. Etykieta pełniąca funkcję nagłówka, po której można przechodzić między częściami okna. |
| `edit` | `value`, `hint`, `number`, `password`, `multiline`, `onChange`. Od wersji 2 `readOnly = true` pozostawia odczyt i zaznaczanie. `onSubmit` otrzymuje tekst po Enter lub przycisku zatwierdzenia w jednowierszowym polu. |
| `switch` | `value` i `onChange`. |
| `list` | `items` z wartościami lub rekordami `{ value, label }`, wybrane `value` i `onChange`. Wybierasz jedną pozycję. |
| `items` | Od wersji 2. Wiersze, które można otworzyć, na przykład lista notatek. `items` zawiera do 500 wartości lub rekordów `{ value, label }`. `empty` to tekst pustej listy. `actions` zawiera nazwy czynności wiersza widoczne w menu **Czynności**. `onSelect` otrzymuje wartość po dwukrotnym stuknięciu, `onLongPress` po długim naciśnięciu, a `onAction` nazwę czynności i wartość wiersza. |
| `slider` | Od wersji 2. `min`, `max`, `step` domyślnie 0, 100 i 1, do 10 000 kroków. `value` domyślnie równe `min`, dostępne `onChange`. |
| `button` | `onClick`. `close = true` zamyka formularz po kliknięciu. |

`onChange`, `onSelect`, `onLongPress` i `onSubmit` dostają wartość oraz formularz. `onClick` dostaje formularz, a `onAction` nazwę czynności, wartość i formularz. Wszystkie działają jako zadania i mogą [czekać na wynik](#waiting).

- `form:get(id)` zwraca wartość, a `form:values()` wszystkie wartości według ID.
- `form:set(id, changes)` zmienia `value`, `label`, `hint`, `items`, `actions`, `empty`, `min`, `max`, `step`, `enabled` lub `visible`.
- `form:close()` zamyka formularz. `form.closed` przyjmuje wtedy `true`.
- `form:wait()` [czeka](#waiting) na zamknięcie i zwraca wartości.
- Od wersji 2 `form:add(item, options)` dodaje na końcu lub za pozycją `options.after` i zwraca ID.
- Od wersji 2 `form:remove(id)` usuwa pozycję.
- Od wersji 2 `form:focus(id)` przenosi fokus na pozycję lub pierwszy wiersz listy `items`.
- Od wersji 2 `form:setTitle(title)` zmienia tytuł formularza.

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

[Przykład notatnika](scripting/examples/notepad.lua) wyświetla listę notatek w oknie. Stuknij dwa razy w notatkę, żeby otworzyć ją w osobnym oknie. Menu **Czynności** pozwala skopiować lub usunąć notatkę.

## Schowek {#clipboard}

Obie funkcje wymagają `clipboard`.

- **`sf.clipboard.get()`** zwraca tekst schowka lub `nil`.
- **`sf.clipboard.set(text)`** zapisuje tekst w schowku.

## System {#system}

Te funkcje wymagają `system`.

- **`sf.system.back()`**, **`home()`**, **`recents()`**, **`notifications()`**, **`quickSettings()`**, **`powerDialog()`**, **`lockScreen()`**, **`takeScreenshot()`** i **`allApps()`** wykonują czynność systemową i w zadaniu zwracają wynik. Zrzut Androida nie jest przekazywany skryptowi.
- **`sf.system.openApp(packageName)`** otwiera aplikację i w zadaniu potwierdza wynik. **`sf.system.apps()`** od wersji 2 zwraca listę aplikacji z ikoną, według nazwy, w rekordach `{ package, label }`.
- **`sf.system.openUrl(url)`** od wersji 2 przekazuje do aplikacji adres `http`, `https`, `mailto`, `tel`, `sms`, `smsto`, `geo` lub `market` i zwraca wynik.
- **`sf.system.share(text, options)`** od wersji 2 otwiera ekran udostępniania Androida z podanym tekstem. W `options` możesz ustawić `title` tego ekranu i `subject`.
- **`sf.system.openSettings(page, packageName)`** od wersji 2 otwiera ekran ustawień i wraca z wynikiem. Dozwolone wartości to `settings` (domyślnie), `wifi`, `bluetooth`, `mobileData`, `airplaneMode`, `nfc`, `location`, `sound`, `display`, `battery`, `batterySaver`, `accessibility`, `apps`, `defaultApps`, `notifications`, `doNotDisturb`, `dateTime`, `language`, `keyboard`, `storage`, `security`, `privacy`, `developer`, `about`, `castScreen`, `hotspot`. Z nazwą pakietu możesz podać `app` lub `appNotifications`.
- **`sf.system.startActivity(intent)`** i **`sf.system.sendBroadcast(intent)`** od wersji 2 przyjmują `{ action, data, type, package, className, categories, extras }`. Konieczne jest `action` lub `className`, a przy klasie także `package`. Komunikat rozgłoszeniowy wymaga pakietu. `extras` zawiera tekst, liczby, `true` lub `false`. Tych wywołań nie można kierować do SandFly. Otwarcie aktywności zwraca powodzenie.

```lua
sf.system.startActivity({
  action = 'android.intent.action.VIEW',
  data = 'https://example.com',
  package = 'com.android.chrome',
})
```

Od wersji 6 kilka funkcji przekazuje żądanie do obsługującej je aplikacji i zwraca, czy je przyjęła:

- **`sf.system.setTimer(seconds, label)`** uruchamia minutnik od 1 do 86 400 sekund w aplikacji zegara. Jeśli aplikacja to umożliwia, nie otwiera jej okna.
- **`sf.system.setAlarm(hour, minute, label)`** ustawia alarm w aplikacji zegara. Jeśli aplikacja to umożliwia, nie otwiera jej okna.
- **`sf.system.call(number)`** dzwoni albo otwiera ekran wybierania, jeśli brakuje zgody na połączenia.
- **`sf.system.message(number, text)`** otwiera aplikację wiadomości z podanym tekstem.
- **`sf.system.camera(video)`** otwiera aparat. `video = true` wybiera film.

**`sf.resume()`** wznawia wstrzymany SandFly.

## Sieć {#network}

**`sf.fetch(url, options)`** wymaga `network` i [czeka](#waiting) na odpowiedź z adresu `https`. W `options` możesz podać poniższe pola.

- `method` przyjmuje `GET` (domyślnie), `POST`, `PUT`, `DELETE`, `HEAD` lub `OPTIONS`.
- `headers` to rekord z nazwami i wartościami nagłówków.
- `body` to tekst albo tabela wysyłana jako JSON. Dla tabeli domyślny nagłówek to `Content-Type: application/json`. Możesz go zmienić w `headers`. Treść ma do 1 MB.
- `timeout` określa czas łączenia i odczytu w ms. Domyślnie 30 000, maksymalnie 60 000.

Odpowiedź zawiera `status`, `statusText`, `ok`, `url`, `headers` i tekstowe `body`. `ok` ma wartość `true` dla kodów od 200 do 299. Nazwy nagłówków są zapisane małymi literami. Wartości tego samego nagłówka rozdziela przecinek ze spacją. `response:json()` dekoduje treść, a `response:text()` zwraca tekst. Odpowiedź może mieć do 5 MB. Skrypt może mieć w toku do ośmiu żądań naraz. Kod taki jak 404 daje odpowiedź. Nieudane połączenie zgłasza błąd zaczynający się od „Network error”.

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

Od wersji 2 **`sf.translate(text, options)`** tłumaczy przez usługę wybraną na ekranie **Tłumaczenie**. Wymaga `network`, [czeka na wynik](#waiting) i zwraca `{ text, from, to }`. W `options.from` i `options.to` podaj znaczniki języka, na przykład `'fr'`. Bez `from` usługa wykrywa język, a bez `to` tłumaczy na język z ustawień. Błąd usługi lub brak konfiguracji powoduje błąd funkcji. Przy tłumaczeniu lokalnym funkcja czeka na pobranie języków.

## Urządzenie {#device}

Od wersji 2 odczyty działają bez dodatkowych uprawnień.

- **`sf.device.info()`** zwraca `{ manufacturer, model, androidVersion, sdk, sandflyVersion, screen }`. `screen` ma `{ width, height, density, orientation }`. Orientacja to `'portrait'` lub `'landscape'`.
- **`sf.device.battery()`** zwraca `{ level, charging, full, plugged }`. `level` to procent, `plugged` to `'ac'`, `'usb'`, `'wireless'`, `'dock'` lub `nil`.
- **`sf.device.network()`** zwraca `{ connected, wifi, cellular, ethernet, vpn, metered }`.
- **`sf.device.volume(stream)`** zwraca `{ level, min, max, muted }`. Kanały to `'media'` (domyślny), `'ring'`, `'notification'`, `'alarm'`, `'call'`, `'system'` i `'accessibility'`.
- **`sf.device.ringerMode()`** zwraca `'normal'`, `'vibrate'` lub `'silent'`.

Do zmian potrzebne jest `system`.

- **`sf.device.setVolume(stream, level)`** ustawia głośność kanału. Wartość jest ograniczana do przedziału od `min` do `max`.
- **`sf.device.setRingerMode(mode)`** zmienia tryb dzwonka. Android odrzuca niektóre zmiany, gdy działa tryb Nie przeszkadzać. Funkcja zgłosi wtedy błąd.
- Od wersji 6 **`sf.device.media(key)`** przekazuje klawisz do aplikacji odtwarzającej multimedia. Dostępne są `'playPause'`, `'play'`, `'pause'`, `'next'`, `'previous'`, `'stop'`, `'rewind'` lub `'fastForward'`.

### Szybkie ustawienia urządzenia {#device-controls}

Od wersji 6 funkcje mogą odwoływać się do elementów panelu szybkich ustawień SandFly przez ich nazwy.

`wifi`, `bluetooth`, `mobileData`, `hotspot`, `airplaneMode`, `dataSaver`, `doNotDisturb`, `soundMode`, `flashlight`, `flashlightBrightness`, `location`, `autoRotate`, `batterySaver`, `darkTheme`, `nightLight`, `extraDim`, `colorInversion`, `colorCorrection`, `nfc`, `microphoneAccess`, `cameraAccess`, `brightness`, `adaptiveBrightness`, `screenTimeout`, `fontSize`, `displaySize`, `mediaVolume`, `callVolume`, `ringVolume`, `notificationVolume`, `alarmVolume`, `accessibilityVolume` i `systemVolume`.

- **`sf.device.controls()`** zwraca listę nazw elementów dostępnych na urządzeniu.
- **`sf.device.get(name)`** zwraca `true` lub `false` dla przełącznika, `{ value, min, max }` dla suwaka i `{ value, label, options }` dla listy wyboru. Lista przyjmująca także liczby z pewnego zakresu zawiera `min` i `max`. Jeśli urządzenie nie ma danego elementu lub nie można go odczytać, wynikiem jest `nil`.
- **`sf.device.set(name, value)`** [czeka na wynik](#waiting) i wymaga `system`. Przełącznik przyjmuje `true`, `false`, `'on'`, `'off'` lub `'toggle'`. Suwak przyjmuje liczbę, procent swojego zakresu, na przykład `'50%'`, lub przesunięcie o krok `'+1'` albo `'-1'`. Lista przyjmuje wartość, etykietę pozycji lub `'next'`. Funkcja zwraca `true` po zmianie. Jeśli zmiana się nie uda, zgłosi błąd z przyczyną, na przykład przy zmianie Wi-Fi bez Shizuku. Gdy brakuje uprawnienia do zmiany ustawienia, otworzy się odpowiedni ekran Androida.

`fontSize` przyjmuje również dowolny procent od 25 do 200, a przed Androidem 14 do 130. Możesz podać `50` lub `'50%'`, także poniżej 85% dostępnych w Androidzie. `displaySize` ustawia wielkość obrazu tak, jak ustawienie rozmiaru wyświetlania w Androidzie, w procentach domyślnej gęstości. Możesz podać dowolną wartość od `min` do `max`. `min` utrzymuje krótszą krawędź poniżej 600 dp, bo przy 600 dp aplikacje mogą przełączyć się na układ tabletowy. `max` pozostawia na krótszej krawędzi co najmniej 320 dp. Zmiana wymaga Shizuku. Niektóre aplikacje zmieniają układ już przy mniejszym rozmiarze. Reguły dopasowane do aktywności lub pozycji listy mogą wtedy przestać pasować.

```lua
sf.run(function()
  local ok, why = pcall(sf.device.set, 'flashlight', 'toggle')
  if not ok then sf.speak(why) end
end)
```

## Tłumaczenia {#translations}

W katalogu `locales` archiwum umieść pliki JSON nazwane według znacznika języka, na przykład `locales/de.json` i `locales/pt-BR.json`. W każdym pliku przypisz angielskiemu tekstowi jego tłumaczenie. Jeśli tekst ma formy liczby mnogiej, podaj rekord form używanych w danym języku. Do wyboru są `zero`, `one`, `two`, `few`, `many` i `other`.

```json
{
  "Next unread": "Nächste ungelesene",
  "{count} message": { "one": "{count} Nachricht", "other": "{count} Nachrichten" }
}
```

SandFly czyta plik dla języka urządzenia oraz języka bez regionu. Bardziej szczegółowa wersja ma pierwszeństwo. Tłumaczenie obejmuje nazwę i opis skryptu, nazwy, opisy i opcje ustawień, etykiety i wypowiedzi z reguł, stany, podpowiedzi, nazwy czynności oraz poleceń i nawigacji.

- **`sf._(text, params)`** zwraca tłumaczenie albo oryginał. Uzupełnia pola `{name}` z `params`.
- **`sf.ngettext(singular, plural, count, params)`** szuka tłumaczenia według `singular` i wybiera formę dla `count`. Wstawia `{count}` i pozostałe pola.
- **`sf.locale()`** podaje znacznik języka urządzenia, na przykład `'de-DE'`.
- **`sf.language()`** podaje znacznik języka używanego do odczytu tłumaczeń.

```lua
local n = 3
sf.speak(sf.ngettext('{count} message', '{count} messages', n)) -- '3 messages'
```

## Tekst {#text}

Od wersji 6 dostępne są funkcje z wyrażeniami regularnymi Javy, bez dodatkowego uprawnienia. Wielkość liter nie ma znaczenia, tak jak przy dopasowywaniu w makrach. Powtarzane grupy nie mogą zawierać powtórzeń ani operatora alternatywy `|`. Tekst jest przycinany do 10 000 znaków.

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

- **`print(...)`** i **`sf.log(...)`** zapisują argumenty oddzielone spacjami w **Dziennik** skryptu na poziomie informacyjnym. Użyj `sf.log.debug`, `sf.log.info`, `sf.log.warn` lub `sf.log.error`, aby podać poziom. Tabela bez metatabeli zostanie zapisana jako JSON. Zobacz [dziennik i błędy](#testing-and-debugging).
- **`sf.setTimeout(fn, ms, ...)`** uruchamia `fn` raz po `ms` milisekundach, a **`sf.setInterval(fn, ms, ...)`** uruchamia ją co `ms` milisekund, przy odstępie co najmniej 10 ms. Pozostałe argumenty trafiają do funkcji. Obie zwracają numer dla **`sf.clearTimer(number)`**. Może działać do 100 liczników. Funkcje liczników są zadaniami.
- **`sf.sleep(ms)`** [czeka](#waiting) `ms` milisekund.
- **`sf.run(fn, ...)`** uruchamia funkcję w zadaniu, które może czekać.
- **`sf.clock()`** od wersji 2 zwraca monotoniczny czas w milisekundach do pomiarów. **`os.time()`** i **`os.date()`** podają datę i godzinę.
- **`sf.apiVersion`** podaje wersję API skryptów tej kompilacji SandFly.
- **`sf.manifest()`** zwraca manifest skryptu.

### Lua i pliki {#lua-and-files}

- **`require(name)`** ładuje plik Lua z pakietu raz i zwraca jego wynik. Na przykład `require('lib.words')` czyta `lib/words.lua`. Nazwa może zawierać litery, cyfry, podkreślenia (`_`), łączniki (`-`) i kropki (`.`).
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

- Trzy lub cztery stuknięcia jednym palcem. `tripleTap`, `quadrupleTap`.

- Przesunięcia przy krawędzi. `edgeSwipeUpFromBottom`, `edgeLongSwipeUpFromBottom`, `edgeSwipeDownFromTopLeft`, `edgeLongSwipeDownFromTopLeft`, `edgeSwipeDownFromTopRight`, `edgeLongSwipeDownFromTopRight`, `edgeSwipeRightFromLeftTop`, `edgeLongSwipeRightFromLeftTop`, `edgeSwipeRightFromLeftBottom`, `edgeLongSwipeRightFromLeftBottom`, `edgeSwipeLeftFromRightTop`, `edgeLongSwipeLeftFromRightTop`, `edgeSwipeLeftFromRightBottom`, `edgeLongSwipeLeftFromRightBottom`, `edgeSwipeUpAlongLeft`, `edgeSwipeDownAlongLeft`, `edgeSwipeUpAlongRight`, `edgeSwipeDownAlongRight`, `edgeSwipeLeftAlongTop`, `edgeSwipeRightAlongTop`, `edgeSwipeLeftAlongBottom`, `edgeSwipeRightAlongBottom`.
- Stuknięcia w rogu. `cornerTapTopLeft`, `cornerDoubleTapTopLeft`, `cornerTripleTapTopLeft`, `cornerDoubleTapAndHoldTopLeft`, `cornerTripleTapAndHoldTopLeft`, `cornerTapTopRight`, `cornerDoubleTapTopRight`, `cornerTripleTapTopRight`, `cornerDoubleTapAndHoldTopRight`, `cornerTripleTapAndHoldTopRight`, `cornerTapBottomLeft`, `cornerDoubleTapBottomLeft`, `cornerTripleTapBottomLeft`, `cornerDoubleTapAndHoldBottomLeft`, `cornerTripleTapAndHoldBottomLeft`, `cornerTapBottomRight`, `cornerDoubleTapBottomRight`, `cornerTripleTapBottomRight`, `cornerDoubleTapAndHoldBottomRight`, `cornerTripleTapAndHoldBottomRight`.
- Stuknięcie i przesunięcie jednym palcem. `twoFingerTapThenOneFingerSwipeUp`, `twoFingerTapThenOneFingerSwipeDown`, `twoFingerTapThenOneFingerSwipeLeft`, `twoFingerTapThenOneFingerSwipeRight`, `twoFingerDoubleTapThenOneFingerSwipeUp`, `twoFingerDoubleTapThenOneFingerSwipeDown`, `twoFingerDoubleTapThenOneFingerSwipeLeft`, `twoFingerDoubleTapThenOneFingerSwipeRight`, `twoFingerTripleTapThenOneFingerSwipeUp`, `twoFingerTripleTapThenOneFingerSwipeDown`, `twoFingerTripleTapThenOneFingerSwipeLeft`, `twoFingerTripleTapThenOneFingerSwipeRight`.
- Stuknięcie i przesunięcie dwoma palcami. `twoFingerTapThenSwipeUp`, `twoFingerTapThenSwipeDown`, `twoFingerTapThenSwipeLeft`, `twoFingerTapThenSwipeRight`, `twoFingerDoubleTapThenSwipeUp`, `twoFingerDoubleTapThenSwipeDown`, `twoFingerDoubleTapThenSwipeLeft`, `twoFingerDoubleTapThenSwipeRight`, `twoFingerTripleTapThenSwipeUp`, `twoFingerTripleTapThenSwipeDown`, `twoFingerTripleTapThenSwipeLeft`, `twoFingerTripleTapThenSwipeRight`.
- Stuknięcie i przesunięcie trzema palcami. `threeFingerTapThenSwipeUp`, `threeFingerTapThenSwipeDown`, `threeFingerTapThenSwipeLeft`, `threeFingerTapThenSwipeRight`, `threeFingerDoubleTapThenSwipeUp`, `threeFingerDoubleTapThenSwipeDown`, `threeFingerDoubleTapThenSwipeLeft`, `threeFingerDoubleTapThenSwipeRight`, `threeFingerTripleTapThenSwipeUp`, `threeFingerTripleTapThenSwipeDown`, `threeFingerTripleTapThenSwipeLeft`, `threeFingerTripleTapThenSwipeRight`.
- Stuknięcie i przesunięcie czterema palcami. `fourFingerTapThenSwipeUp`, `fourFingerTapThenSwipeDown`, `fourFingerTapThenSwipeLeft`, `fourFingerTapThenSwipeRight`, `fourFingerDoubleTapThenSwipeUp`, `fourFingerDoubleTapThenSwipeDown`, `fourFingerDoubleTapThenSwipeLeft`, `fourFingerDoubleTapThenSwipeRight`, `fourFingerTripleTapThenSwipeUp`, `fourFingerTripleTapThenSwipeDown`, `fourFingerTripleTapThenSwipeLeft`, `fourFingerTripleTapThenSwipeRight`.
- Zsuwanie i rozsuwanie palców oraz szybki ruch dwoma palcami tam i z powrotem. `twoFingerPinch`, `twoFingerSpread`, `threeFingerPinch`, `threeFingerSpread`, `fourFingerPinch`, `fourFingerSpread`, `twoFingerScrub`.
- Przesunięcie i przytrzymanie. `swipeUpAndHold`, `swipeDownAndHold`, `swipeLeftAndHold`, `swipeRightAndHold`, `swipeUpThenDownAndHold`, `swipeDownThenUpAndHold`, `swipeLeftThenRightAndHold`, `swipeRightThenLeftAndHold`, `swipeUpThenLeftAndHold`, `swipeUpThenRightAndHold`, `swipeDownThenLeftAndHold`, `swipeDownThenRightAndHold`, `swipeLeftThenUpAndHold`, `swipeLeftThenDownAndHold`, `swipeRightThenUpAndHold`, `swipeRightThenDownAndHold`.

Przesunięcia trzyczęściowe mają nazwy takie jak `swipeRightThenUpThenRight`, `swipeUpThenDownThenUp`. Gesty oddzielone uniesieniem mają nazwy takie jak `swipeRightLiftSwipeUp`, `swipeDownLiftSwipeDown`, `swipeUpThenLeftLiftSwipeRight`, `swipeUpLiftSwipeRightThenDown`. Te ostatnie wymagają ustawienia **Odstęp między przesunięciami dwuetapowymi**. Gdy krawędź służy do otwierania menu, ruchy wzdłuż niej trafiają do menu.

## Testowanie i diagnozowanie {#testing-and-debugging}

### Dziennik {#the-log}

Na stronie skryptu otwórz **Dziennik**, żeby zobaczyć tekst zapisany przez `print` i `sf.log`, wszystkie błędy skryptu i ostrzeżenia SandFly. Wpisy są uporządkowane od najnowszego, a przy każdym jest czas i poziom. SandFly przechowuje 300 ostatnich wpisów na skrypt od chwili swojego uruchomienia. Każdy wpis ma do 4000 znaków. **Kopiuj dziennik** kopiuje wpisy od najstarszego. **Wyczyść dziennik** je usuwa.

SandFly zapisuje też własne wpisy.

- „Loaded for *pakiet*” na poziomie diagnostycznym przy każdym załadowaniu skryptu.
- Jedno ostrzeżenie dla każdej procedury obsługi zdarzenia, reguły, czynności lub przypisania pominiętego z powodu braku uprawnienia, na przykład „onFocus needs the screen permission, so it is not used”.
- Ostrzeżenie przy każdej czynności reguły, która nie znalazła elementu, oraz po jednym ostrzeżeniu dla każdej procedury mowy, która przekracza limit czasu.
- Ostrzeżenie, jeśli wersje API z manifestu nie pasują do tej wersji SandFly.

**Ustawienia programisty** > **Poziom rejestrowania** określa, które wpisy trafiają do dziennika systemowego z tagiem `BacktalkScript`. Mogą zawierać tekst ekranu. Dziennik skryptu zachowuje je także wtedy, gdy domyślne ustawienia pomijają je w dzienniku systemowym.

### Błędy {#errors}

Gdy procedura obsługi zdarzenia, polecenie, licznik lub funkcja wywołana przez okno zgłosi błąd, w dzienniku pojawi się miejsce wystąpienia, komunikat i stos wywołań. Przykładowe oznaczenie miejsca to `onFocus:` albo `command unread:`. Skrypt pozostanie w pamięci, a pozostałe procedury nadal będą działać. Błąd podczas wykonywania `main.lua` przy ładowaniu zaczyna się od `Loading:`. Skrypt nie zostanie wtedy załadowany.

Podczas importu SandFly odrzuca skrypt, jeśli nie może odczytać manifestu. Wyświetla powód, na przykład „The manifest needs author” lub „Unknown rule fields: lable”.

SandFly wyłącza skrypt po pięciu błędach w minutę albo trzech przekroczeniach czasu w pięć minut. Usłyszysz nazwę wyłączonego skryptu i wskazówkę, żeby sprawdzić jego dziennik w **Skrypty**. Dziennik zawiera wpis „Turned off after repeated errors”. Jeśli pojedyncze wywołanie blokuje wszystkie skrypty przez ponad 20 sekund, SandFly wyłącza dany skrypt i ponownie uruchamia pozostałe. Po naprawie użyj **Włącz** na stronie skryptu.

### Ponowne ładowanie {#reload-a-script}

- Zaimportuj plik ponownie przez **Importuj skrypt**. Nowy kod zastąpi poprzedni, a dane, ustawienia i wybory uprawnień zostaną zachowane.
- Na stronie skryptu wyłącz przełącznik **Włącz**, a potem włącz go ponownie. Skrypt wczyta się od nowa i zacznie z początkowymi wartościami zmiennych.
- Skrypt z `apps` ładuje się za każdym razem, gdy jego aplikacja trafia na pierwszy plan.

### Wskazówki do testów {#tips}

- Przypisz gest do **Włącz lub wyłącz wszystkie skrypty**, zanim zaczniesz próbę, jeśli nie wiesz, jak zadziała skrypt. Skrypt nie może przejąć tego gestu. Możesz też użyć **Skrypty** > **Wyłącz wszystkie skrypty**.
- Żeby poznać identyfikatory i teksty potrzebne do zapytań, użyj **Sprawdź element z fokusem**, **Kopiuj szczegóły elementu** i **Kopiuj drzewo ekranu**. Zobacz [Jak sprawdzić dane elementu](#find-the-details-of-an-item).
- Użyj `print` na rekordzie zdarzenia. `sf.onScroll(function(e) print(e) end)` zapisze zdarzenie przewijania jako JSON.
- Otocz `pcall` wywołania, które mogą się nie udać, na przykład `sf.fetch` i `sf.device.set`.
- Krótkie procedury mowy mogą użyć zmiennych obliczonych wcześniej w `onAppEnter` lub liczniku.

## Limity {#limits}

| Zasób | Limit |
| --- | --- |
| Ładowanie i wywołanie | 2 sekundy na główny plik lub wywołanie. W zadaniu czekającym liczy się każda część między pauzami. |
| Procedury mowy i menu | 30 ms łącznie na mowę. `onActions` może działać do 250 ms, ale pozycje trafią do menu tylko przy odpowiedzi w 50 ms. Szukanie elementów wlicza się do czasu i zatrzymuje się po jego upływie. |
| Pamięć | 32 MB dla skryptu i 8 MB tekstu z `string.rep`. |
| Manifest | 500 reguł, w tym do 100 z `match`, 50 poleceń i 20 opcji nawigacji. Do 10 czynności w regule, 10 kroków w czynności i 12 części w `read`. |
| Zasoby podczas działania | 200 dodatkowych reguł, 100 liczników, 8 żądań, 8 okien i formularzy oraz 2 rozpoznania tekstu równocześnie. |
| Dane | 524 288 znaków w `sf.storage`. Klucz od 1 do 200 znaków. Klucz ustawienia i ID polecenia do 64. |
| Tekst | Do 20 000 znaków mowy lub okna, 1000 znaków w brajlu, 4000 znaków wpisu i 10 000 znaków dla `sf.text`. |
| Pliki | `.lua` do 1 MB. `.zip` do 5 MB przed i po rozpakowaniu oraz 200 plików. `sf.readFile` i plik makra do 1 MB. |

SandFly sprawdza czas i pamięć co 10 000 instrukcji Lua. Przekroczenie powoduje błąd, którego `pcall` nie przechwyci. Wywołanie biblioteczne trwa do końca, więc używaj prostych wzorców przy długim tekście, zwłaszcza przy `.-` i `.*`.

## Publikowanie skryptu {#publish-a-script}

Żeby udostępnić skrypt, umieść plik `.lua` lub `.zip` w publicznym repozytorium na GitHubie, GitLabie, Codebergu, Gitei (gitea.com) lub Bitbuckecie. Użytkownicy mogą dodać repozytorium, jego gałąź lub katalog jako źródło przez **Skrypty** > **Źródła skryptów** > **Dodaj źródło**. Przykładowe adresy znajdziesz poniżej.

- `github.com/owner/repository` lub `github.com/owner/repository/tree/main/scripts`.
- `gitlab.com/group/repository` lub `gitlab.com/group/subgroup/repository/-/tree/main/scripts`.
- `codeberg.org/owner/repository` lub `codeberg.org/owner/repository/src/branch/main/scripts`.
- `gitea.com/owner/repository`, z układem ścieżki jak na Codebergu.
- `bitbucket.org/workspace/repository` lub `bitbucket.org/workspace/repository/src/main/scripts`.

Źródło pokazuje do 200 plików `.lua` i `.zip` umieszczonych bezpośrednio w danym katalogu. Skrypt możesz też zainstalować z adresu `https` prowadzącego do pliku.

Raz dziennie SandFly sprawdza, czy w źródle instalacji pod tą samą nazwą pliku jest nowsza `version`. Skrypt zainstalowany bez zapamiętanego źródła może znaleźć po nazwie pliku pochodzącej z `id`, na przykład `my-script.lua`. Gdy **Aktualizuj skrypty automatycznie** jest włączone, SandFly automatycznie instaluje nowszą wersję, jeśli pochodzi z tego samego źródła, ma ten sam `author`, nie wymaga nowych uprawnień i jest zgodna z wersją API tej kompilacji SandFly. Pozostałe aktualizacje znajdziesz w **Skrypty** > **Aktualizacje**. Okno instalacji pokazuje nowe uprawnienia i informuje o zmianie autora.

SandFly podpisuje pliki w [oficjalnym repozytorium skryptów](https://github.com/aaron-gh/sandfly-scripts). Ostatni wiersz `main.lua` zaczyna się od `-- SandFly signature:`. Podpis obejmuje wszystkie pliki. W oknie instalacji zobaczysz **Podpisany przez SandFly.** przy poprawnym podpisie lub **Niepodpisany przez SandFly.** przy braku podpisu. Jeśli plik zmienił się po podpisaniu, zobaczysz informację o niezgodności podpisu. Aktualizacja z oficjalnego repozytorium instaluje się automatycznie tylko przy poprawnym podpisie.

## Kreator skryptów {#the-script-builder}

Przez **Skrypty** > **Nowy skrypt** możesz tworzyć skrypty bez pisania kodu. W kreatorze ustawisz aplikacje, reguły, sterowanie czytaniem, polecenia, wyzwalacze, zmiany mowy i ustawienia. Możesz też dodać kod Lua. Sposób użycia opisano w części „Utwórz skrypt w kreatorze” w rozdziale „Skrypty i własne etykiety”.

- Kreator zapisuje zwykły skrypt. W `main.lua` umieszcza manifest, po jednym wierszu Lua dla każdego polecenia, wyzwalacza i zmiany mowy oraz na końcu tekst z **Kod** > **Kod Lua**. Kroki poleceń i wyzwalaczy zapisuje w plikach w `macros/`, a uruchamia przez [`sf.macros.run`](#macros).
- Kreator dodaje do manifestu uprawnienia wymagane przez używane funkcje. Przez **Kod** > **Dodatkowe uprawnienia** możesz dodać kolejne, na przykład `java`. Przed zapisem kreator sprawdza składnię Lua.
- Jeśli zmiana utworzyłaby błędny manifest, na przykład reguły w skrypcie ładowanym na żądanie, kreator jej nie zapisze i poda powód.
- Na stronie skryptu utworzonego poza kreatorem **Kopiuj do kreatora skryptów** tworzy jego edytowalną kopię. Przenosi pola manifestu do odpowiednich części kreatora, kod do **Kod Lua** i dołącza pozostałe pliki.
- **Eksportuj skrypt** zapisuje projekt jako `.zip`. **Importuj skrypt** instaluje go z zachowaniem możliwości dalszej edycji w kreatorze.
- **Utwórz skrypt za pomocą SandFly Pro** i **Zmień za pomocą SandFly Pro** tworzą lub zmieniają projekt na podstawie opisu. Dopóki użytkownik nie wybierze **Akceptuj**, skrypt działa bez uprawnień `network` i `java`.

## Przykładowe skrypty {#example-scripts}

- [example-plugin.lua](scripting/examples/example-plugin.lua) odczytuje nazwę aplikacji po przełączeniu i liczy jej otwarcia. Pokazuje `onAppEnter`, `sf.storage` i przełącznik. Wbudowaną kopię znajdziesz w **Skrypty** > **Dodaj przykładowy skrypt**.
- [notepad.lua](scripting/examples/notepad.lua) otwiera notatnik z menu SandFly lub po naciśnięciu klawisza SandFly z N. Pokazuje `sf.ui.window`, listy `items` z czynnościami, okno potwierdzenia `sf.ui.confirm` otwierane nad innym oknem oraz zapisywanie danych.
- [wikipedia-search.lua](scripting/examples/wikipedia-search.lua) wyszukuje artykuł i otwiera wybrany. Pokazuje ładowanie na żądanie, `sf.ui.prompt`, `sf.fetch` z JSON, `sf.ui.choose` i `sf.system.openUrl`.
- [talking-clock.lua](scripting/examples/talking-clock.lua) odczytuje godzinę co godzinę albo pół godziny i na polecenie. Pokazuje `keepLoaded`, ustawienia listy, `onSettingChange` i liczniki.

Pliki znajdują się w [katalogu przykładów](scripting/examples).
