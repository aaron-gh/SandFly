## Pierwsze kroki {#getting-started}

SandFly to czytnik ekranu dla Androida. Za pomocą syntezy mowy odczytuje treść ekranu, odtwarza dźwięki i wibruje. Dzięki gestom SandFly możesz obsługiwać urządzenie bez patrzenia na ekran. Możesz też korzystać z monitora brajlowskiego, ekranowej klawiatury brajlowskiej albo klawiatury fizycznej.

### Urządzenia i wersje Androida {#getting-started-devices}

- Na telefonach i tabletach SandFly działa od Androida 8.
- Na zegarkach działa z systemem [Wear OS](#devices-watch).
- Na telewizorach obsługuje [Android TV i Google TV](#devices-tv).

Niektóre funkcje wymagają nowszego Androida albo dostępu przez [Shizuku lub root](#updates-shizuku). Shizuku i root udostępniają uprawnienia, które Android przyznaje tylko przez adb. Wymagania znajdziesz przy opisie danej funkcji. [SandFly Pro](#pro) to subskrypcja, która udostępnia dodatkowe funkcje AI.

### Zainstaluj SandFly {#getting-started-install}

Na [stronie wydań SandFly](https://github.com/aaron-gh/SandFly/releases/latest) znajdziesz `sandfly.apk` dla telefonów, tabletów i telewizorów oraz `sandfly-wear.apk` dla zegarków.

Na telefonie lub tablecie wykonaj te kroki.

1. Otwórz w przeglądarce [najnowsze wydanie](https://github.com/aaron-gh/SandFly/releases/latest).
2. Pobierz `sandfly.apk`.
3. Otwórz pobrany plik.
4. Jeśli Android wyświetli prośbę o zgodę na instalowanie aplikacji z przeglądarki lub menedżera plików, zezwól na to, wróć i otwórz plik ponownie.
5. Wybierz **Zainstaluj**.

SandFly to osobna aplikacja z własnymi ustawieniami. Możesz ją zainstalować obok innego czytnika. Kolejne [aktualizacje](#updates) zainstalujesz już z poziomu SandFly.

Uwaga: Android 13 i nowsze mogą blokować usługi ułatwień dostępu zainstalowane z przeglądarki lub menedżera plików. Jeśli nie da się włączyć SandFly w ustawieniach ułatwień dostępu, otwórz informacje o aplikacji SandFly w ustawieniach Androida. Otwórz menu **Więcej** i wybierz **Zezwól na ograniczone ustawienia**. Nazwy opcji mogą się różnić zależnie od telefonu.

### Włącz SandFly {#getting-started-turn-on}

Najpierw wyłącz dotychczasowy czytnik. Dwa włączone czytniki mogą jednocześnie reagować na gesty i czytać ekran.

1. Otwórz **Ustawienia** Androida.
2. Wejdź w **Ułatwienia dostępu**. Na niektórych telefonach SandFly znajdziesz w podsekcji **Zainstalowane aplikacje** lub **Pobrane aplikacje**.
3. Wybierz **SandFly**.
4. Włącz przełącznik i potwierdź.

Usłyszysz „SandFly włączony”. Przy [pierwszym uruchomieniu](#getting-started-first-start) SandFly otworzy okno wyboru gestów i samouczek.

#### Włączaj i wyłączaj SandFly skrótem dostępności {#getting-started-shortcut}

W ustawieniach Androida otwórz **Ułatwienia dostępu** > **SandFly** i włącz skrót dostępności. Android może pozwolić na przykład na przytrzymanie obu klawiszy głośności przez kilka sekund lub użycie przycisku dostępności. Wybór zależy od wersji Androida i telefonu.

### Co się dzieje przy pierwszym uruchomieniu {#getting-started-first-start}

Przy pierwszym włączeniu SandFly otwiera kolejno dwa ekrany.

1. Okno **Wybierz gesty** na urządzeniach z ekranem dotykowym. Zobacz [](#getting-started-gesture-layout).
2. Samouczek SandFly. Zobacz [](#getting-started-tutorial).

Jeśli korzystasz już z SandFly i właśnie go zaktualizowałeś, SandFly otworzy [strony o nowych funkcjach](#getting-started-new-features) zamiast samouczka. Jeśli jest zainstalowany Backtalk Aaron, SandFly najpierw [przenosi z niego ustawienia i własne etykiety](#getting-started-backtalk).

#### Wybierz układ gestów {#getting-started-gesture-layout}

W oknie **Wybierz gesty** masz dwie możliwości.

- **Gesty SandFly** to układ domyślny. Przesunięcie dwoma palcami trafia do aplikacji i służy do przewijania.
- **Gesty VoiceOver** przypominają obsługę czytnika na iPhonie. Przesunięcia dwoma palcami wywołują polecenia SandFly, na przykład czytanie od góry ekranu.

Jeśli zamkniesz okno bez wyboru, SandFly przejdzie na domyślny układ. Gdy wyjdziesz z okna na ekran główny, otworzy się ono znowu przy następnym uruchomieniu. Po wyborze samouczek pokaże gesty z wybranego układu.

Później możesz zmienić układ. Wybierz **Gesty** > **Układ gestów**. Więcej o przypisaniach znajdziesz w [rozdziale o gestach](#gestures).

#### Samouczek SandFly {#getting-started-tutorial}

Podstawowych gestów nauczysz się, wykonując ćwiczenia na ekranie. Aby wrócić do samouczka, wybierz **Pomoc i opinie** > **Samouczek SandFly** lub użyj polecenia **Otwórz samouczek**.

Samouczek składa się z 16 lekcji w pięciu częściach.

- **Podstawy** obejmują lekcje o rozpoczęciu pracy, czytaniu dotykiem, przechodzeniu między elementami, aktywowaniu elementów i wstrzymywaniu mowy.
- W części **Czytanie** poznasz sterowanie czytaniem, nagłówki i czytanie ciągłe.
- **Elementy sterujące i tekst** obejmują przewijanie list, suwaki, czynności i pisanie.
- W części **Poruszanie się** przećwiczysz cofanie, powrót na ekran główny, otwieranie powiadomień i menu SandFly oraz przydatne polecenia.
- W części **Ćwiczenia** możesz praktykować gesty.

Instrukcja jest pierwszym elementem po nazwie lekcji. Wykonaj opisany gest, żeby przejść do następnego kroku. SandFly odczytuje instrukcje także wtedy, gdy w tle gra dźwięk. Jeśli przez 25 sekund nie dotkniesz ekranu, SandFly powtórzy instrukcję. Zrobi to najwyżej trzy razy.

Samouczek korzysta z Twoich przypisań. Jeśli czynność nie ma gestu, dowiesz się o tym w instrukcji i możesz użyć **Pomiń krok**. Na końcu lekcji SandFly poda gest potrzebny do przejścia dalej i ustawi fokus na przycisku **Następna lekcja** z nazwą lekcji. Ostatnią lekcję zamkniesz przyciskiem **Zakończ samouczek**.

Podczas lekcji SandFly opisuje działanie gestów cofania, powrotu na ekran główny, ostatnich aplikacji i powiadomień, zamiast opuścić samouczek. Tak samo opisuje gesty wyłączania mowy i wstrzymywania SandFly. Polecenie **Wstecz** nadal zamyka klawiaturę i menu.

Wybierz **Spis treści**, żeby otworzyć dowolną lekcję lub sprawdzić swoje postępy. Tam też znajdziesz **Zamknij samouczek**. Po ponownym otwarciu trafisz do ostatnio wybranej lekcji lub do następnej, jeśli poprzednią udało Ci się ukończyć.

W trybie **Ćwiczenie gestów** SandFly podaje nazwę gestu i przypisanego polecenia. Polecenie się wtedy nie wykonuje. Żeby dojść do **Zakończ ćwiczenie**, możesz nadal przechodzić między elementami i je aktywować. Aby od razu wejść do tej lekcji, wybierz **Pomoc i opinie** > **Ćwiczenie gestów** lub otwórz **Ćwiczenie gestów** na ekranie **Gesty**.

Uwaga: Samouczek przechwytuje gesty rozpoznawane przez SandFly. Jeśli wyłączysz **Ustawienia programisty** > **Używaj gestów w SandFly**, za nawigację odpowiada Android i jego gesty mogą wyprowadzić Cię z lekcji. Do lekcji o suwakach i czynnościach potrzebujesz włączonej opcji **Sterowanie czytaniem** > **Automatycznie zmieniaj opcję sterowania czytaniem**. Domyślnie ta opcja jest włączona.

#### Samouczek klawiatury {#getting-started-keyboard-tutorial}

Na urządzeniu z klawiaturą fizyczną i bez ekranu dotykowego przy pierwszym uruchomieniu otworzy się **Samouczek klawiatury SandFly**. Ma 15 lekcji, w tym **Lista skrótów klawiszowych**, **Tryb przeglądania** stron i **Ćwiczenie klawiatury**. Ćwiczysz skróty z używanego układu klawiszy.

Na urządzeniu z ekranem dotykowym i klawiaturą fizyczną polecenie **Samouczek SandFly** otworzy okno wyboru samouczka. Dostępne są **Samouczek dotyczący obsługi dotykowej** i **Samouczek dotyczący klawiatury**. Szczegóły znajdziesz w [rozdziale o klawiaturach](#keyboards). [Zegarki i telewizory](#devices) mają własne samouczki.

#### Co nowego w SandFly {#getting-started-new-features}

Po aktualizacji z nowymi funkcjami SandFly pokaże ich opis przy kolejnym uruchomieniu. Żeby wrócić do tych stron, wybierz **Nowe funkcje w SandFly** u góry ekranu ustawień.

### Podstawowe gesty {#getting-started-basics}

Poniższe gesty dotyczą układu **Gesty SandFly**. W układzie VoiceOver część poleceń ma inne przypisania. Możesz sprawdzić [pełny wykaz](#appendix-gestures) albo [zmienić przypisania](#gestures).

- **Czytanie dotykiem**. Przesuwaj palcem po ekranie, żeby czytać elementy pod palcem.
- Przesuń palcem w prawo, żeby przejść do następnego elementu, lub w lewo, żeby wrócić do poprzedniego.
- Stuknij dwa razy w dowolnym miejscu, żeby aktywować element, na którym jest fokus. Jeśli chcesz go nacisnąć i przytrzymać, po drugim stuknięciu zostaw palec na ekranie.
- **Przewijanie**. Przesuń dwa palce w górę lub w dół, żeby przewinąć listę. SandFly przewija też sam, gdy przechodzisz do kolejnego elementu poza ekranem.
- Stuknij dwoma palcami, żeby wstrzymać mowę. Powtórz gest, żeby ją wznowić. Dwukrotne stuknięcie trzema palcami włącza lub wyłącza mowę.
- **Sterowanie czytaniem**. Przesuń trzy palce w dół albo obróć dwa palce zgodnie z ruchem wskazówek zegara, żeby wybrać następną opcję sterowania czytaniem, na przykład **Słowa**, **Nagłówki** lub **Szybkość mowy**. Przesuń trzy palce w górę lub obróć dwa palce w przeciwną stronę, żeby wrócić do poprzedniej opcji. Potem przesuwaj jednym palcem w dół lub w górę, żeby użyć wybranej opcji. Zobacz [rozdział o czytaniu ekranu](#reading).
- **Menu SandFly** otworzysz stuknięciem trzema palcami. Możesz też przesunąć palec w górę, a potem w prawo, lub w dół, a potem w prawo. Zobacz [rozdział o poleceniach i menu](#commands).
- **Wstecz**. Przesuń palec w dół, a potem w lewo. Możesz też szybko przesunąć dwa palce tam i z powrotem.
- **Ekran główny**. Przesuń palec w górę, a potem w lewo, lub przesuń go w górę od dolnej krawędzi.
- **Ostatnie aplikacje** otworzysz ruchem w lewo, a potem w górę.
- **Powiadomienia** otworzysz ruchem w prawo, a potem w dół.
- Żeby wyszukać tekst na ekranie, przesuń palec w lewo, a potem w dół.

Więcej sposobów nawigacji znajdziesz w rozdziałach o [czytaniu ekranu](#reading) i [stronach internetowych](#web).

### Otwórz ustawienia SandFly {#getting-started-settings}

- Na ekranie aplikacji Androida otwórz **SandFly**. Ikona prowadzi do ustawień także przy wyłączonej usłudze SandFly. Możesz więc zmienić ustawienia przed jego włączeniem.
- W menu SandFly wybierz **Ustawienia SandFly**.
- W ustawieniach Androida wybierz **Ułatwienia dostępu** > **SandFly** > **Ustawienia**.

Na głównym ekranie są osobne strony dla ustawień mowy, szczegółowości, dźwięku i wibracji oraz wyjścia audio. Znajdziesz tam też dotyk, gesty, skróty klawiszowe, klawisze głośności, polecenia, sterowanie czytaniem, menu SandFly i własne menu.

Dalej możesz ustawić makra, polecenia głosowe, nawigację, pisanie, brajl, wyświetlanie, powiadomienia, tłumaczenie, opisy obrazów i ekranu, usługi AI, SandFly Pro, skrypty, etykiety niestandardowe, szybkie ustawienia oraz przegląd.

Na tym samym ekranie znajdziesz też wstrzymywanie i wyłączanie, profile ustawień, kopię zapasową i przywracanie, aktualizacje i instalację, Shizuku i root, pomoc oraz ustawienia programisty. Na zegarkach, telewizorach i urządzeniach bez wymaganego sprzętu część stron jest ukryta.

#### Znajdź ustawienie {#getting-started-search}

1. U góry głównego ekranu ustawień wybierz **Szukaj**.
2. Wpisz jedno lub kilka słów i rozpocznij wyszukiwanie.
3. Wybierz wynik.

Wynik musi mieć wszystkie wpisane słowa w nazwie lub opisie. Możesz wpisać też fragment słowa. Wielkość liter i znaki diakrytyczne nie wpływają na wynik. SandFly podaje nazwę wyniku i drogę do niego, na przykład **Mowa** > **Zatrzymywanie mowy**. Po wybraniu ustawienia fokus przechodzi na nie na właściwym ekranie. Wynik będący poleceniem otwiera się na ekranie **Polecenia**.

Ekrany **Gesty** i **Skróty klawiszowe** mają własną opcję **Szukaj**. Wyszukasz tam gest lub skrót po nazwie albo po poleceniu, które uruchamia.

### Podręcznik użytkownika {#getting-started-user-guide}

Otwórz **Pomoc i opinie** > **Podręcznik użytkownika**. Ze spisu treści na początku przejdziesz do wybranego rozdziału. Odsyłacze w tekście prowadzą do powiązanych sekcji. SandFly otwiera przewodnik w swoim języku. Jeśli brakuje tłumaczenia rozdziału, wyświetla wersję angielską.

### Przejdź z Backtalk Aaron na SandFly {#getting-started-backtalk}

SandFly zastępuje Backtalk Aaron, który nie otrzymuje już aktualizacji. Ostatnia wersja Backtalk Aaron proponuje przejście w powiadomieniu lub w oknie **Backtalk Aaron to teraz SandFly**.

1. Otwórz powiadomienie o aktualizacji lub wybierz **Sprawdź aktualizacje**, a następnie **Zainstaluj**.
2. Poczekaj na zakończenie instalacji SandFly obok Backtalk Aaron. Otworzą się ustawienia dostępności Androida.
3. Włącz SandFly. Aplikacja skopiuje ustawienia i własne etykiety. Backtalk Aaron wyłączy się sam.
4. Aby odinstalować poprzednią aplikację, otwórz powiadomienie **Ustawienia skopiowane z Backtalk Aaron**.

SandFly otrzymuje wydania stabilne. Jeśli chcesz także nowe kompilacje testowe, włącz **Aktualizacje i instalacja** > **Wersje deweloperskie**. Zobacz [opis kanałów aktualizacji](#updates-channels).

[Modele AI działające na urządzeniu](#descriptions) pobierz ponownie w SandFly. Jeśli używasz [klawiatury brajlowskiej](#braille), włącz ją ponownie dla SandFly.

Żeby SandFly przeniósł skróty dostępności i klawiaturę brajlowską z Backtalk Aaron, przyznaj mu uprawnienie do zmiany ustawień systemowych przed włączeniem SandFly. Wykonaj na komputerze polecenie `adb`.

```
adb shell pm grant io.github.aaron_gh.sandfly android.permission.WRITE_SECURE_SETTINGS
```

Bez tego uprawnienia ustaw skrót dla SandFly w ułatwieniach dostępu Androida. Jeżeli masz plik ustawień zapisany w Backtalk, zobacz [rozdział o profilach i kopii zapasowej](#profiles).
