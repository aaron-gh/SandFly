# Pliki pojedynczych elementów SandFly

Taki plik pozwala przenieść wybrane makra, profile, skróty, słowniki, znaki brajlowskie lub polecenia głosowe. Możesz go wyeksportować, udostępnić, edytować i zaimportować na innym telefonie z SandFly. Do pełnej kopii służy **Kopia zapasowa i przywracanie** > **Eksportuj ustawienia**.

## Eksport i import {#export-and-import}

**Eksportuj do pliku** zapisuje całą listę lub pojedynczą pozycję, zależnie od otwartego ekranu.

| Zawartość | Gdzie ją znaleźć w ustawieniach |
| --- | --- |
| Makra | **Makra** |
| Profile gestów | **Gesty** > **Profile gestów** |
| Gesty aplikacji | **Gesty** > **Gesty w aplikacjach** |
| Skróty klawiszowe aplikacji | **Skróty klawiszowe** > **Skróty klawiszowe w aplikacjach** |
| Mapa klawiatury | **Skróty klawiszowe**. Eksport zapisuje aktualną mapę. |
| Profile ustawień | **Profile ustawień** |
| Słownik mowy | **Mowa** > **Słownik mowy** |
| Słownik symboli | **Mowa** > **Słownik symboli** |
| Słownik brajlowski | **Brajl** > **Słownik brajlowski** |
| Własne znaki brajlowskie | **Brajl** > **Klawiatura brajlowska** > **Własne znaki brajlowskie** |
| Własne polecenia głosowe | **Polecenia głosowe** |

Przycisk **Importuj z pliku** na każdym z tych ekranów wczytuje wszystkie pozycje z pliku. Nowa pozycja zastępuje pozycję tego samego rodzaju o tej samej nazwie, niezależnie od wielkości liter. Gdy importujesz słownik, liczy się szukany tekst i aplikacja. Dla znaków brajlowskich liczy się tekst i tablica, a dla poleceń głosowych fraza. SandFly podaje pominięte pozycje, na przykład nieznane polecenia, niepoprawne skróty, brakujące nazwy i odrzucone wyrażenia.

**Kopia zapasowa i przywracanie** > **Opis formatu pliku eksportu** zapisuje opis wraz z gestami, poleceniami i ustawieniami dostępnymi na urządzeniu oraz ich aktualnymi wartościami. Jeśli chcesz, żeby AI przygotowała Ci plik, dołącz ten opis do polecenia.

## Struktura pliku {#the-file}

Plik jest obiektem JSON. Pole `format` przyjmuje `"sandfly-items"`, a `version` wartość `1`. Każdy rodzaj ma własną listę. Listy możesz pominąć lub podać w dowolnej kolejności.

```json
{
  "format": "sandfly-items",
  "version": 1,
  "macros": [],
  "gestureProfiles": [],
  "keymaps": [],
  "apps": [],
  "settingsProfiles": [],
  "speechDictionary": [],
  "symbolDictionary": [],
  "brailleDictionary": [],
  "brailleCharacters": [],
  "voiceCommands": []
}
```

SandFly ignoruje tekst przed pierwszym `{` i po ostatnim `}`. Możesz więc zapisać również odpowiedź czatu z otoczką bloku kodu. Dozwolone są komentarze zaczynające się od `//` lub `#`. Limit to 2 MB na plik i 1000 pozycji na listę.

## Polecenia {#commands}

Użyj identyfikatora działania, na przykład `"READ_FROM_TOP"` albo `"NEXT"`. Opis formatu z urządzenia zawiera bieżące nazwy i identyfikatory. `"UNASSIGNED"` wyłącza przypisanie w danej aplikacji lub profilu.

Do otwarcia aplikacji użyj `"OPEN_APP:"` z nazwą pakietu, na przykład `"OPEN_APP:com.google.android.gm"`. Możesz też podać komponent z opisu formatu. Aplikacja musi być zainstalowana przy imporcie.

Makro zapisujesz jako `"MACRO:"` z jego `id` lub nazwą, na przykład `"MACRO:Open mail"`. Profil gestów to `"GESTURE_PROFILE:"` z `id` lub nazwą. Samo `"GESTURE_PROFILE:"` przywraca profil domyślny. Eksport używa identyfikatorów i dołącza makra, od których zależą pozycje.

## Gesty {#gestures}

Użyj klucza z opisu formatu. Na przykład `"pref_shortcut_up"` oznacza przesunięcie w górę.

## Skróty klawiszowe {#keyboard-shortcuts}

Połącz nazwy klawiszy znakiem `+`, na przykład `"SandFly+Shift+Right"` lub `"Ctrl+Alt+H"`. `SandFly` oznacza klawisz z bieżącej mapy. Modyfikatory to `Shift`, `Ctrl`, `Alt`, `Meta`, `Sym` i `Fn`.

Możesz podać literę, cyfrę lub nazwę. Przykłady to `Left`, `Right`, `Up`, `Down`, `Enter`, `Space`, `Backspace`, `Delete`, `Escape`, `Tab`, `PageUp`, `PageDown`, `Home`, `End`, `Insert`, `Menu`, `Grave`, `Minus`, `Equals`, `LeftBracket`, `RightBracket`, `Backslash`, `Semicolon`, `Apostrophe`, `Comma`, `Period` i `Slash`. Działa też nazwa Androida bez prefiksu `KEYCODE_`, na przykład `F5` lub `Numpad_5`. Wielkość liter nie ma znaczenia.

Przecinek rozdziela części skrótu sekwencyjnego, na przykład `"SandFly+O, H"`. `"SandFly twice"` oznacza dwa naciśnięcia klawisza SandFly.

Mapa telefonu z klawiaturą obsługuje dodatkowe zapisy. `alone` rozpoznaje sam modyfikator (`"SandFly alone"`), `twice` podwójne naciśnięcie (`"Enter twice"`, `"Alt alone twice"`), a `held` przytrzymanie (`"Alt+Shift held"`, `"K held"`). Poza wpisywaniem krótki nacisk klawisza z przypisaniem `held` nadal wykonuje zwykły skrót.

## Makra {#macros}

```json
{
  "id": "open-mail",
  "name": "Open mail",
  "condition": [{ "test": "keyboard", "not": true }],
  "steps": [
    { "type": "open_app", "app": "OPEN_APP:com.google.android.gm" },
    { "type": "tap", "label": "Compose", "timeout": 5000 },
    { "type": "wait", "ms": 500 },
    { "type": "command", "command": "READ_FROM_TOP" }
  ]
}
```

`id` jest opcjonalne. Inne pozycje mogą się nim posłużyć. `condition` jest opcjonalne. Bez niego makro można uruchomić zawsze. `app` ogranicza je do pakietu, na przykład `"com.whatsapp"`. Jeśli pole jest pominięte, a nieodwrócony warunek `app` nie ma alternatywy `"or"`, SandFly może przejąć pakiet z warunku.

`"actions": true` dodaje makro do menu **Czynności**. `actionsCondition` ma tę samą strukturę co `condition` i ogranicza widoczność pozycji, na przykład `[{ "test": "focused", "label": "Send", "not": true }]`.

Rodzaje kroków:

| `type` | Pola | Działanie |
| --- | --- | --- |
| `command` | `command` | Wykonuje polecenie. |
| `wait` | `ms` | Czeka do 600 000 ms. |
| `tap` | `label`, `timeout`, `regex`, `field`, `kind` | Dotyka elementu pasującego do `label`. |
| `tap_position` | `x`, `y` | Stuka w punkt w pikselach od lewego górnego rogu. |
| `touch_hold` | `label`, `hold`, `slide`, `distance`, `timeout`, `regex`, `field`, `kind` | Przytrzymuje środek przez `hold` ms, opcjonalnie przesuwa o `distance` dp w stronę `slide` i unosi. |
| `focus_item` | `label`, `timeout`, `regex`, `field`, `kind` | Przenosi fokus dostępności. |
| `speak_item` | `label`, `timeout`, `regex`, `field`, `kind` | Czyta element. |
| `act_item` | `act`, `label`, `times`, `timeout`, `regex`, `field`, `kind` | Przytrzymuje lub przewija `times` razy. `act` to `"long_press"`, `"scroll_forward"` lub `"scroll_backward"`. Bez `label` przewija największą listę. |
| `speak_text` | `text` | Czyta tekst. |
| `open_app` | `app` | Otwiera aplikację. Podaj polecenie otwarcia. |
| `type_text` | `text`, `replace` | Wpisuje tekst. `replace` zastępuje całą zawartość. |
| `if` | `condition` | Wykonuje kroki po spełnieniu warunku, do `else_if`, `else` lub `end_if`. |
| `else_if` | `condition` | Sprawdza kolejną możliwość. |
| `else` | | Obejmuje pozostałe przypadki. |
| `end_if` | | Kończy warunek. |
| `repeat` | `count` | Powtarza kroki do `end_repeat`, maksymalnie 1000 razy. |
| `repeat_while` | `condition` | Powtarza do `end_repeat`, póki jest spełniony warunek. |
| `end_repeat` | | Kończy blok powtórzeń. |
| `exit_loop` | | Wychodzi z powtórzenia. |
| `stop` | | Zatrzymuje makro. |
| `wait_until` | `condition`, `timeout` | Czeka na spełnienie warunku. Po upływie `timeout` ms zatrzymuje makro. |

Przy wyszukiwaniu `timeout` określa oczekiwanie od 500 do 60 000 ms. Domyślnie wynosi 3000 ms. `label` pasuje do fragmentu zawartości niezależnie od wielkości liter. `"regex": true` włącza wyrażenie regularne. `field` zawęża dopasowanie do `"text"`, `"description"`, `"hint"`, `"state"`, `"tooltip"` lub `"id"`. `kind` pozwala wybrać typ. Bez niego pasuje każdy.

`x` i `y` przyjmują wartości od 0 do 10 000. Punkt poza ekranem kończy krok błędem. `hold` przyjmuje od 100 do 600 000 ms, domyślnie 1000. `slide` to `"up"`, `"down"`, `"left"` lub `"right"`. Bez kierunku palec zostaje uniesiony w tym samym miejscu. `distance` określa od 1 do 1000 dp, domyślnie 100. Koniec przesunięcia zostaje na ekranie. Polecenie odczytu współrzędnych odczytuje i kopiuje środek elementu. Makro mieści do 200 kroków.

Warunek to lista do 20 testów połączonych przez „i”. `"or": true` przy teście rozpoczyna grupę połączoną z poprzednią przez „lub”. `"not": true` odwraca wynik.

| `test` | Pola | Kiedy pasuje |
| --- | --- | --- |
| `item` | `label`, `kind`, `regex`, `field` | Element na ekranie zawiera `label`. |
| `focused` | `label`, `kind`, `regex`, `field` | Element, na którym jest fokus, zawiera `label`. Pusta wartość pasuje do dowolnego tekstu. |
| `app` | `package` | Aktywne okno ma podany pakiet. |
| `keyboard` | | Klawiatura ekranowa jest widoczna. |
| `locked` | | Ekran blokady jest widoczny. |

Opcjonalne `kind` może mieć wartość `clickable`, `long_clickable`, `editable`, `scrollable`, `adjustable`, `checkable`, `checked`, `selected`, `heading` lub `enabled`. `clickable`, `long_clickable` i `checked` sprawdzają również kilka poziomów w pobliżu elementu z tekstem. `checked` uwzględnia także zaznaczony przełącznik lub pole w tym samym wierszu.

## Profile gestów {#gesture-profiles}

```json
{
  "name": "Reading",
  "swipesBeforeLift": false,
  "gestures": {
    "pref_shortcut_up": "READ_FROM_TOP",
    "pref_shortcut_down": "READ_FROM_CURRENT",
    "pref_shortcut_right_and_down": "MACRO:Open mail"
  }
}
```

Gesty pominięte w `gestures` zachowują wcześniejsze działanie. `swipesBeforeLift` ustawia rozpoznawanie przed uniesieniem w tym profilu i jest opcjonalne. Nazwa profilu domyślnego jest zarezerwowana.

## Mapy klawiatury {#keymaps}

```json
{
  "name": "My keymap",
  "sandFlyKey": "Insert",
  "sequences": false,
  "arrowsNavigate": false,
  "plainKeys": false,
  "shortcuts": {
    "NEXT": "SandFly+Right",
    "PREVIOUS": "SandFly+Left",
    "READ_FROM_TOP": "SandFly+A"
  }
}
```

Po imporcie mapa trafia do własnych map. Otwórz **Skróty klawiszowe** i wybierz ją z listy. Działania pominięte w `shortcuts` nie mają skrótu. Aby zmienić kilka skrótów, zacznij od eksportu działającej mapy. Każde polecenie ma jeden skrót i każdy skrót prowadzi do jednego polecenia.

`sandFlyKey` przyjmuje `Meta`, `Alt`, `Ctrl+Alt`, `Ctrl+Meta`, `Insert`, `Numpad Insert`, `Sym`, `Right Alt`, `Ctrl`, `Right Shift` lub `Fn`. Domyślnie jest to `Meta`. `sequences` pozwala na skróty złożone. `arrowsNavigate` pozwala używać skrótów bez klawisza SandFly również poza trybem przeglądania, jak w NVDA. `plainKeys` pozwala wywoływać skrót dla zwykłego klawisza z dodanym klawiszem SandFly także podczas wpisywania, jak w mapie telefonu z klawiaturą. Początkowo jest wyłączone.

## Gesty i skróty w aplikacjach {#gestures-and-keyboard-shortcuts-in-apps}

```json
{
  "package": "org.telegram.messenger",
  "name": "Telegram",
  "gestures": { "pref_shortcut_up": "MACRO:Open mail" },
  "keys": { "Ctrl+Enter": "UNASSIGNED", "SandFly+M": "READ_FROM_TOP" }
}
```

Każde pole odpowiada jednej aplikacji według pakietu i zastępuje jej zestaw. Skrót w `keys` bez składnika `SandFly` działa bez tego klawisza.

## Profile ustawień {#settings-profiles}

```json
{
  "name": "Reading",
  "apps": ["com.amazon.kindle"],
  "web": false,
  "reading": true,
  "editing": false,
  "keyboards": ["com.example.keyboard/.KeyboardService"],
  "settings": {
    "pref_vibration": false,
    "pref_soundback": true
  }
}
```

`apps`, `web`, `reading`, `editing` i `keyboards` określają automatyczny wybór profilu w aplikacji, treści WWW, czytaniu ciągłym, edycji oraz przy wyświetlonej klawiaturze. `keyboards` zawiera identyfikatory klawiatur z listy danego urządzenia.

W `settings` podaj klucze i wartości, które mają obowiązywać przy włączonym profilu. Użyj formatu z opisu urządzenia: wartości logicznej, liczby, napisu lub listy napisów. Ustawienie zapisane jako tekst wymaga cudzysłowów, na przykład `"1.5"`. `null` przywraca dla profilu wartość domyślną. Dla klawiatury i monitora brajlowskiego użyj prefiksu pliku, na przykład `"braille_keyboard/key"`.

Profil pomija klucze API, licencję Pro i stan urządzenia, na przykład informację o wyświetlonych samouczkach.

## Słowniki {#dictionaries}

`speechDictionary`, `symbolDictionary` i `brailleDictionary` to listy wpisów.

```json
{ "find": "btw", "replace": "by the way", "regex": false, "wholeWord": true, "app": "", "appName": "" }
```

Puste `replace` usuwa dopasowany tekst. `regex` i `wholeWord` są opcjonalne. Słownik symboli ich nie obsługuje. `app` ogranicza wpis do pakietu. Pusta wartość obejmuje wszystkie aplikacje. SandFly odrzuca powtarzane grupy zawierające inne powtórzenia lub alternatywy, na przykład `(a+)+`. Po imporcie limit to 500 wpisów dla symboli i 5000 dla mowy lub brajla.

## Własne znaki brajlowskie {#custom-braille-characters}

W `brailleCharacters` można przypisać tekst do własnej kombinacji komórek w jednej tablicy.

```json
{ "text": ")", "cells": "56-2356", "table": "POLISH" }
```

`text` ma od 1 do 50 znaków. `cells` to punkty od 1 do 8 w każdej komórce. Oddziel komórki myślnikiem. Maksimum to 20 komórek. `table` identyfikuje tablicę, na przykład `"POLISH"`, `"POLISH_COMP8"`, `"UEB"` lub `"GERMAN"`. Identyfikatory swoich tablic znajdziesz w pliku wyeksportowanym z ekranu własnych znaków. SandFly zachowuje do 500 znaków niestandardowych.

## Własne polecenia głosowe {#custom-voice-commands}

```json
{ "phrase": "open mail", "command": "MACRO:Open mail" }
```

SandFly zachowuje do 500 własnych poleceń głosowych.
