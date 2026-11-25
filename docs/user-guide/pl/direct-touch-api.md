# Obszary dotyku bezpośredniego

Dotyk bezpośredni przekazuje dotknięcia aplikacji z pominięciem eksploracji SandFly. Może obejmować cały ekran albo wybrane obszary, na przykład planszę gry. Na reszcie ekranu nadal działa eksploracja. Zasady można ustawić w manifeście dla danej aktywności i stanu, a także zmienić podczas działania.

## Ustawienia użytkownika {#what-the-user-controls}

- Jeśli aplikacja zadeklarowała obszary lub uprawnienie, SandFly włącza dotyk bezpośredni, gdy zobaczy ją po raz pierwszy. Użytkownik może go wyłączyć w **Dotyk** > **Dotyk bezpośredni**. Deklaracje i żądania aplikacji nie obowiązują wtedy.
- SandFly przejmuje dotyk po otwarciu okna dialogowego, innej aplikacji, powiadomień lub szybkich ustawień, po przejściu do pola tekstowego i gdy ekran zgaśnie. Po zamknięciu takiego okna, opuszczeniu pola albo ponownym włączeniu ekranu dotyk bezpośredni wraca na obszary aplikacji.
- Okna systemowe, takie jak pasek stanu, pozostają pod obsługą SandFly. Dotyczy to również klawiatury, chyba że użytkownik włączy pisanie bezpośrednie dla aplikacji. Pasek nawigacji przyjmuje dotknięcia bezpośrednio tylko wtedy, gdy użytkownik włączy **Aktywowanie przycisków paska nawigacji kliknięciem**.
- Gdy obszary aplikacji zaczynają odbierać dotknięcia, SandFly mówi „Dotyk bezpośredni włączony”. Gdy przestają, mówi „Dotyk bezpośredni wyłączony”. Oba komunikaty można wyłączyć opcją **Odczytuj zmiany stanu dotyku bezpośredniego**.

## Wybór obszarów {#which-areas-apply}

Gdy aplikacja jest na pierwszym planie, obowiązuje pierwsza pasująca możliwość:

1. Ostatnie żądanie aplikacji, aż do `reset`.
2. Reguły manifestu odpowiadające bieżącej aktywności i stanowi.
3. Brak dotyku bezpośredniego dla aplikacji z deklaracją lub uprawnieniem.
4. Cały ekran dla innych aplikacji z włączoną funkcją. Tak działa na przykład gra włączona przez użytkownika na liście **Aplikacje** albo gra deklarująca `dev.nvgt.capability.DIRECT_TOUCH`.

## Deklaracja w manifeście {#declaring-areas-in-the-manifest}

Wewnątrz `<application>` dodaj `<meta-data>` z zasobem XML:

```xml
<meta-data
    android:name="io.github.aaron_gh.sandfly.direct_touch"
    android:resource="@xml/direct_touch" />
```

W `res/xml/direct_touch.xml` elementy wewnątrz `<direct-touch>` są regułami:

```xml
<direct-touch>
    <!-- The whole screen in GameActivity. -->
    <screen activity=".GameActivity" />

    <!-- The board view in MainActivity while the state is "playing". -->
    <area activity=".MainActivity" state="playing" view="@id/board" />

    <!-- The bottom half of the window in MainActivity while the state is "playing". -->
    <area activity=".MainActivity" state="playing" top="50%" />

    <!-- A strip 200 dp tall along the bottom of the window while the state is "racing". -->
    <area state="racing" bottom="100%" height="200dp" />

    <!-- The whole screen in any activity while the state is "racing". -->
    <screen state="racing" />
</direct-touch>
```

| Element | Obszar |
| --- | --- |
| `<screen>` | Cały ekran. |
| `<area>` z `view` | Widoczny widok o danym identyfikatorze, niezależnie od położenia. Można podać `@id/board`, `board` lub `com.example.game:id/board`. |
| `<area>` z krawędziami | [Obszar okna](#areas-of-the-window). |

Możesz dodać warunek aktywności lub stanu. Pominięcie warunku obejmuje wszystkie wartości.

| Atrybut | Dopasowanie |
| --- | --- |
| `activity` | Bieżąca aktywność, na przykład `.GameActivity`, `GameActivity` lub `com.example.game.GameActivity`. `*` pasuje do każdej. |
| `state` | Stan ustawiony przez `setState`. Reguła zaczyna pasować po ustawieniu stanu przez aplikację. |

### Obszary okna {#areas-of-the-window}

`<area>` bez `view` przyjmuje opcjonalne atrybuty:

| Atrybut | Znaczenie |
| --- | --- |
| `left`, `right` | Odległość od lewej krawędzi okna. |
| `top`, `bottom` | Odległość od górnej krawędzi okna. |
| `width`, `height` | Rozmiar przy jednej krawędzi na danej osi albo bez obu krawędzi. |

Wartość może być procentem lub ułamkiem rozmiaru okna (`30%`, `0.3`), rozmiarem w dp (`48dp`) albo w pikselach (`120px`). Ułamek powyżej 100% jest przycinany do 100%. Wartości ujemne są niedozwolone. Pominięta krawędź oznacza krawędź okna. Wszystkie współrzędne są liczone od lewego górnego rogu.

| Deklaracja | Wynik |
| --- | --- |
| `top="30%"` | Od 30% wysokości do dołu. |
| `bottom="30%"` | Górne 30% okna. |
| `left="30%" right="70%"` | Środkowe 40% szerokości i cała wysokość. |
| `bottom="100%" height="200dp"` | Pas o wysokości 200 dp u dołu. |
| `left="100%" width="80dp"` | Pusty obszar, bo zaczyna się na prawej krawędzi. Użyj `right="100%" width="80dp"`, żeby wybrać pas przy prawej krawędzi. |
| `width="150px" height="150px"` | Kwadrat 150 pikseli w lewym górnym rogu. |

Jeśli podasz obie krawędzie na danej osi, rozmiar na tej osi jest ignorowany. Liczy się okno aplikacji, które może zaczynać się pod paskiem stanu. Obszary są przycinane do jego granic. Jednostka dp uwzględnia gęstość ekranu, dlatego piksele sprawdzają się przy znanej wielkości.

Jeśli pasuje reguła całego ekranu, obejmuje również poszczególne obszary. W innych przypadkach pasujące obszary się sumują. SandFly czyta do 64 reguł i pomija nieznane elementy. Puste `<direct-touch />` pozostawia obsługę SandFly aż do żądania aplikacji.

SandFly ustala bieżącą aktywność w chwili jej otwarcia. Jeśli jedna z aktywności aplikacji była już otwarta podczas uruchamiania SandFly, reguły wskazujące nazwę aktywności zaczną obowiązywać od chwili otwarcia następnej aktywności. Po aktualizacji aplikacji SandFly ponownie wczyta deklarację.

## Żądania w trakcie działania {#asking-while-your-app-runs}

Dodaj uprawnienie do manifestu:

```xml
<uses-permission android:name="io.github.aaron_gh.sandfly.permission.DIRECT_TOUCH" />
```

Od Androida 11 dodaj widoczność pakietu:

```xml
<queries>
    <package android:name="io.github.aaron_gh.sandfly" />
</queries>
```

Skopiuj `talkback/src/main/aidl/io/github/aaron_gh/sandfly/directtouch/IDirectTouch.aidl` i zachowaj pakiet `io.github.aaron_gh.sandfly.directtouch`. Połącz się z usługą:

```kotlin
val intent =
  Intent("io.github.aaron_gh.sandfly.action.DIRECT_TOUCH").setPackage("io.github.aaron_gh.sandfly")
context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
```

Każde wywołanie przyjmuje obiekt `Binder` klienta. Utwórz go raz i zachowaj przez czas życia procesu. Po jego zakończeniu SandFly usuwa żądanie i stan klienta.

```kotlin
val client = Binder()
val directTouch = IDirectTouch.Stub.asInterface(service)

// The whole screen.
directTouch.requestScreen(client)

// The top half of the screen, and the joystick view wherever it is.
val board = Rect(0, 0, screenWidth, screenHeight / 2)
directTouch.requestAreas(client, arrayOf(board), arrayOf("joystick"))

// No direct touch, whatever the manifest declares, until the next request.
directTouch.stop(client)

// Back to what the manifest declares.
directTouch.reset(client)

// The state that the manifest's rules match.
directTouch.setState(client, "playing")
directTouch.setState(client, null)
```

| Metoda | Działanie |
| --- | --- |
| `requestScreen(client)` | Przekazuje dotyk z całego ekranu. |
| `requestAreas(client, areas, viewIds)` | Przekazuje dotyk z prostokątów ekranu w pikselach i nazwanych widoków. Nazwa bez pakietu, na przykład `joystick`, oznacza widok z pakietu aplikacji. Każda tablica może być pusta lub zawierać do 32 pozycji. |
| `stop(client)` | Wstrzymuje dotyk bezpośredni mimo deklaracji lub ustawienia. Obowiązuje do nowego żądania lub `reset`. |
| `reset(client)` | Usuwa żądanie i przywraca deklarację manifestu. |
| `setState(client, state)` | Ustawia stan do dopasowania reguł. `null` go usuwa. |

Każde żądanie zastępuje poprzednie. Wywołania są jednokierunkowe i wracają od razu. Żądanie obowiązuje, tylko gdy aplikacja jest na pierwszym planie. Jeśli wyślesz je z tła, zacznie działać po przejściu aplikacji na pierwszy plan. Obszary są przycinane do okna aplikacji. Po zmianie układu wyślij ponownie prostokąty podane w pikselach ekranu. Możesz też użyć identyfikatorów widoków. SandFly przy zmianach zawartości ponownie ustali ich położenie.

Po ponownym uruchomieniu SandFly połączenie zgłasza `onServiceDisconnected`, a potem `onServiceConnected`. Wyślij żądanie i stan ponownie w `onServiceConnected`.

## Klawiatury w nakładce ułatwień dostępu {#keyboards-in-an-accessibility-overlay}

Klawiatura z własną usługą ułatwień dostępu, która rysuje klawisze w nakładce ułatwień dostępu, sama ustawia dla tej nakładki obszar przepuszczania dotyku z pominięciem czytania dotykiem. Android przechowuje jeden taki obszar dla wszystkich usług ułatwień dostępu, więc każdy obszar ustawiony przez SandFly zastępowałby obszar klawiatury. Gdy taka klawiatura jest klawiaturą domyślną, jej usługa ułatwień dostępu jest włączona, a nakładka widoczna na ekranie, SandFly dodaje granice nakładki do każdego ustawianego obszaru przepuszczania, na przykład dla dotyku bezpośredniego lub paska nawigacji. Funkcja SandFly, która na chwilę przejmuje cały obszar przepuszczania, na przykład menu kołowe, nadal dostaje go w całości, a zablokowanie dotyku nadal go czyści.

Aby sprawdzić, czy zainstalowany SandFly tak działa, odczytaj metadane `io.github.aaron_gh.sandfly.keeps_keyboard_region` jego usługi dotyku bezpośredniego. Nie wymaga to uprawnień, tylko opisanego wyżej elementu `<queries>`:

```kotlin
val keepsKeyboardRegion =
  context.packageManager
    .queryIntentServices(
      Intent("io.github.aaron_gh.sandfly.action.DIRECT_TOUCH").setPackage("io.github.aaron_gh.sandfly"),
      PackageManager.GET_META_DATA,
    )
    .firstOrNull()
    ?.serviceInfo
    ?.metaData
    ?.getBoolean("io.github.aaron_gh.sandfly.keeps_keyboard_region") == true
```

SandFly 0.7.0 i starsze nie mają tych metadanych i mogą zastąpić obszar klawiatury.

## Przestarzałe nazwy {#deprecated-names}

Te nazwy są przestarzałe:

| Przestarzała nazwa | Nowa nazwa |
| --- | --- |
| `fyi.quin.backtalk.direct_touch` | `io.github.aaron_gh.sandfly.direct_touch` |
| `fyi.quin.backtalk.action.DIRECT_TOUCH` | `io.github.aaron_gh.sandfly.action.DIRECT_TOUCH` |
| `fyi.quin.backtalk.directtouch.IDirectTouch` | `io.github.aaron_gh.sandfly.directtouch.IDirectTouch` |

Przestarzała akcja łączy się z przestarzałym interfejsem, więc zmień obie nazwy jednocześnie. Gdy manifest zawiera oba klucze metadanych, SandFly odczytuje nowy.

SandFly 27.0 i nowsze nadal przyjmują te nazwy od aplikacji, które z nich korzystają. Gdy aplikacja użyje jednej z nich po raz pierwszy od uruchomienia SandFly, SandFly zapisuje w logu ostrzeżenie z tagiem `SandFlyApi`. Aby wyświetlić ostrzeżenia:

```sh
adb logcat -s SandFlyApi
```

Gdy aplikacja jest kompilacją z możliwością debugowania, SandFly wyświetla też powiadomienie z listą użytych przez nią przestarzałych nazw.

Przestarzałe nazwy zostaną usunięte w jednej z kolejnych wersji. SandFly 0.7.0 i starsze przyjmują tylko przestarzałe nazwy. Aby obsługiwać także te wersje, połącz się z usługą najpierw przez nową akcję, a gdy `bindService` zwróci false, przez przestarzałą.
