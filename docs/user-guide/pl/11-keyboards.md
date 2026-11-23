## Klawiatury fizyczne {#keyboards}

SandFly obsługuje klawiaturę Bluetooth, USB i klawiaturę wbudowaną w telefon. Możesz używać skrótów, poruszać się strzałkami lub literami i przekazywać klawisze aplikacji. Zobacz [fabryczne skróty](#appendix-keyboard-shortcuts) oraz [samouczek](#getting-started).

### Klawisz SandFly {#keyboards-sandfly-key}

Większość skrótów łączy klawisz SandFly z innym klawiszem. Na przykład klawisz SandFly ze strzałką w prawo może przejść do następnego elementu. Używane możliwości zależą od mapy.

- **Meta** to klawisz Windows lub Command.
- Możliwe są także **Alt**, **Ctrl**, **Ctrl + Alt** i **Ctrl + Meta**. Przy kombinacji przytrzymaj oba klawisze.
- **Klawisz Insert** zwykle znajduje się nad strzałkami. **Klawisz Insert na klawiaturze numerycznej** to zero przy wyłączonym Num Lock.
- Na [telefonach z klawiaturą](#keyboards-phones) możesz użyć **Sym**, **Prawy Alt**, **Prawy Shift** lub **Fn**.

SandFly przechwytuje przytrzymany Insert, także numeryczny, oraz Sym, prawy Alt, prawy Shift i Fn. Gdy klawiszem SandFly jest Meta, pojedyncze naciśnięcie otwiera ekran wszystkich aplikacji. Podwójne naciśnięcie może przełączać [tryb przeglądania](#keyboards-browse-mode), zależnie od mapy. Zobacz [wybór klawisza](#keyboards-choose-sandfly-key).

### Mapy klawiszy {#keyboards-keymaps}

Mapa klawiszy to zestaw przypisań.

- **Mapa klawiszy SandFly** domyślnie używa Meta. Możesz wybrać Insert lub Insert numeryczny.
- **Mapa klawiszy VoiceOver** odwzorowuje sterowanie iPhone'a. Domyślnie używa Ctrl + Alt, do wyboru jest też Insert lub Insert numeryczny. Meta zastępuje Command. Litery w trybie przeglądania zachowują się jak szybka nawigacja VoiceOver.
- **Mapa klawiszy NVDA** odpowiada układowi laptopowemu. **Mapa klawiszy JAWS** używa skrótów komputerowych i laptopowych klawiszy kursora oraz klikania. Obie domyślnie używają Insert. Możesz wybrać Insert numeryczny lub Meta.
- **Mapa klawiszy Orca** używa układu laptopowego i domyślnego Insert. Możesz wybrać również Insert numeryczny, Meta lub Alt.
- **Mapa klawiszy telefonu z klawiaturą** i **Mapa pojedynczych klawiszy telefonu** są przeznaczone do [telefonów z klawiszami](#keyboards-phones).
- Możesz też [utworzyć własną mapę](#keyboards-custom-keymaps).

W NVDA, JAWS i Orca strzałki oraz Tab działają [jak na komputerze](#keyboards-desktop), bez klawisza SandFly. Przy pierwszym uruchomieniu na rozpoznanym telefonie z klawiaturą SandFly wybiera mapę telefonu.

#### Wybierz mapę {#keyboards-choose-keymap}

1. Otwórz **Skróty klawiszowe**.
2. Wybierz **Wybierz mapę klawiszy**.
3. Wybierz mapę. SandFly odczyta jej nazwę.

Każda mapa zapamiętuje własne skróty, klawisz SandFly i ustawienia strzałek.

#### Wybierz klawisz SandFly {#keyboards-choose-sandfly-key}

Otwórz **Skróty klawiszowe** > **Wybierz klawisz SandFly**. W mapach wbudowanych zobaczysz klawisze, z którymi działają wszystkie domyślne skróty. Własna mapa pozwala wybrać każdy. W domyślnych skrótach SandFly podmieni modyfikator. Zmiany Twoich skrótów są zapamiętywane osobno dla każdego klawisza SandFly.

#### Utwórz własną mapę {#keyboards-custom-keymaps}

1. [Wybierz mapę](#keyboards-choose-keymap), którą chcesz skopiować.
2. Otwórz **Skróty klawiszowe** i wybierz **Nowa mapa klawiszy**.
3. Wpisz nazwę lub zostaw proponowaną.
4. Wybierz **OK**. Kopia od razu będzie używana.

Kopia zachowuje skróty, modyfikator i sposób nawigacji wyjściowej mapy. Możesz też ustawiać skróty bez klawisza SandFly. Są aktywne poza edytowanymi polami. W kopiach SandFly i VoiceOver skróty z Ctrl lub Alt działają także podczas edycji. Sam modyfikator trafia do aplikacji.

Gdy korzystasz z własnej mapy, w tych ustawieniach są **Zmień nazwę mapy klawiszy** i **Usuń mapę klawiszy**. Po potwierdzeniu usunięcia SandFly włączy mapę domyślną.

#### Przenieś mapę w pliku {#keyboards-keymap-files}

**Eksportuj do pliku** zapisuje aktywną mapę wraz ze skrótami i klawiszem SandFly. **Importuj z pliku** dodaje mapę. Opcje są niedostępne na zegarku. Zobacz [format pliku](#api-item-files).

#### Przywróć domyślne skróty mapy {#keyboards-reset-keymap}

Wybierz **Zresetuj mapę klawiszy** i potwierdź. SandFly odtworzy fabryczne skróty i polecenia strzałek używanej mapy. Tej operacji nie da się cofnąć.

### Ekran skrótów klawiszowych {#keyboards-settings}

- **Szukaj** znajduje polecenie lub strzałkę po nazwie lub klawiszach.
- Ekran **Mapa klawiszy** pozwala wybrać mapę i klawisz SandFly. Tu też zmienisz, zapiszesz lub przywrócisz mapę. **Wyświetl systemowe skróty klawiszowe** pokazuje skróty Androida i bieżącej aplikacji.
- **Skróty klawiszowe w aplikacjach** służą do [ustawiania osobnych przypisań](#keyboards-app-shortcuts).
- **Nawigacja strzałkami** pojawia się w mapach, które ją [obsługują](#keyboards-arrow-keys).
- Rozwiń kategorię, żeby zobaczyć polecenia i skróty. Przy skrócie działającym wyłącznie w trybie przeglądania SandFly podaje to w nazwie.

Żeby zmieniać skróty i resetować mapę, włącz czytnik. Możesz też otworzyć skrót na własnym ekranie [polecenia](#commands).

### Zmień skrót klawiszowy {#keyboards-change-shortcut}

1. Otwórz **Skróty klawiszowe**.
2. Otwórz kategorię lub użyj **Szukaj**.
3. Wybierz polecenie i naciśnij nowe klawisze.
4. Naciśnij Enter lub wybierz **Zapisz**.

Backspace albo **Wyczyść** usuwa skrót. W mapach SandFly, VoiceOver i ich kopiach Ctrl + Backspace odtwarza skrót domyślny. Escape lub **Anuluj** zamyka okno bez zapisu. Jeśli kombinacja jest zajęta, SandFly poda poprzednie polecenie. Potwierdź w oknie **Klucz jest już używany**, żeby przenieść skrót.

Dla **Włącz lub wyłącz tryb przeglądania** możesz zarejestrować dwa naciśnięcia klawisza SandFly w pół sekundy. Nie dotyczy to Ctrl + Alt i Ctrl + Meta.

#### Skróty w dwóch krokach {#keyboards-sequences}

Mapy SandFly, VoiceOver i ich kopie pozwalają nacisnąć klawisz SandFly z R albo O, a potem kolejny klawisz. Na drugi krok masz trzy sekundy. Możesz wtedy puścić klawisz SandFly. Ostatnim klawiszem nie może być Escape, Backspace, Enter ani modyfikator. Tę samą sekwencję naciśnij w oknie nagrywania skrótu.

#### Skróty używane tylko podczas pisania {#keyboards-while-editing}

Zaznacz **Tylko podczas edycji** przy ustawianiu skrótu. Naciśnij jego klawisze bez klawisza SandFly. Czytnik przejmie je tylko w edytowanym polu. Poza nim trafią do aplikacji. Przy kombinacji na liście zobaczysz adnotację o edycji.

#### Dodatkowe naciśnięcia w mapach telefonu {#keyboards-presses}

Możesz przypisać jeden modyfikator, na przykład Sym, dwa szybkie naciśnięcia lub przytrzymanie. Jeśli nagrywasz dwa naciśnięcia, pierwsze trafia do aplikacji, a drugie wykonuje przypisane polecenie. Modyfikatory możesz przytrzymać razem przez pół sekundy. Możesz też przytrzymać zwykły klawisz, na przykład K. W edytowanym polu przytrzymania nadal mogą służyć do pisania.

Nagraj naciśnięcia tak, jak później ich użyjesz. W tych mapach pojedynczy Enter po chwili zapisuje, a podwójny zapisuje sekwencję dwóch naciśnięć Enter. Backspace czyści przypisanie.

### Skróty w wybranej aplikacji {#keyboards-app-shortcuts}

Skrót aplikacji ma pierwszeństwo przed mapą. Jeśli wybierzesz **Nieprzypisany**, klawisze trafią do aplikacji.

1. Otwórz **Skróty klawiszowe** > **Skróty klawiszowe w aplikacjach**.
2. Wybierz **Dodaj aplikację** i wskaż aplikację.
3. W sekcji **Skróty klawiszowe** wybierz **Dodaj skrót klawiszowy**. Naciśnij klawisze i zapisz.
4. Wybierz kategorię i polecenie lub wartość bez przypisania.

SandFly musi być włączony, żeby dodać skrót. Po wybraniu skrótu użyj **Wybierz polecenie** lub **Usuń**. **Usuń aplikację** kasuje jej przypisania. Na tym samym ekranie ustawisz też [gesty aplikacji](#gestures).

### Znajdź skrót {#keyboards-shortcut-list}

**Pokaż listę skrótów klawiszowych** otwiera listę z wyszukiwaniem. W mapach SandFly, VoiceOver i Orca domyślnie użyj klawisza SandFly + Shift + ukośnik. W mapie telefonu użyj klawisza SandFly + Q, a w mapie pojedynczych klawiszy samego Q. W ustawieniach znajdziesz też osobne **Szukaj**. Zobacz [wykaz fabryczny](#appendix-keyboard-shortcuts).

### Tryb przeglądania {#keyboards-browse-mode}

W tym trybie litery mogą przenosić fokus w aplikacjach i na stronach. W mapie SandFly H przechodzi do następnego nagłówka, K do linku, B do przycisku, D do punktu orientacyjnego, a F do elementu sterującego. Dodaj Shift, żeby cofnąć się w danej kategorii. Strzałki przenoszą po wierszach i znakach, a Ctrl ze strzałkami w bok po słowach.

| Mapa | Domyślny skrót trybu przeglądania |
| --- | --- |
| SandFly | Dwa naciśnięcia klawisza SandFly |
| VoiceOver | Klawisz SandFly + Q |
| NVDA | Klawisz SandFly + spacja |
| JAWS | Klawisz SandFly + Z |
| Orca | Klawisz SandFly + A |

SandFly podaje zmianę trybu dźwiękiem lub mową. W mapach telefonu odpowiednie polecenia działają bez przełączania. Domyślnie **Nawigacja** > **Inteligentny tryb przeglądania** zmienia tryb zależnie od tego, czy jesteś w polu edycji treści WWW. Domyślnie wyłączone **Zapamiętaj tryb przeglądania w każdej aplikacji** zapamiętuje Twój wybór osobno dla aplikacji. Zobacz [pełne zasady trybu](#web).

### Nawigacja strzałkami {#keyboards-arrow-keys}

Gdy jest włączona, same strzałki lub ich połączenia z Ctrl i Alt uruchamiają polecenia.

| Klawisze | Polecenie domyślne |
| --- | --- |
| Prawo lub lewo | Następny lub poprzedni element. |
| Dół lub góra | Użyj wybranej opcji sterowania czytaniem. |
| Lewo i prawo razem | Przełącz nawigację strzałkami. |
| Góra i dół razem | Aktywuj element. |
| Góra i prawo lub góra i lewo | Następna lub poprzednia opcja sterowania czytaniem. |
| Ctrl + góra lub Ctrl + dół | Pierwszy lub ostatni element. |
| Alt + góra lub Alt + dół | Przewiń wstecz lub do przodu. |
| Alt + lewo lub Alt + prawo | Przewiń w prawo lub w lewo. |

Dół z prawą lub lewą strzałką oraz Ctrl ze strzałką w lewo lub prawo domyślnie nie mają przypisania. Nawigację przełączysz lewą i prawą strzałką razem lub w **Skróty klawiszowe** > **Nawigacja strzałkami** > **Nawigacja strzałkami**. Dostępne jest też polecenie **Włącz lub wyłącz nawigację strzałkami**. Domyślnie funkcja działa w mapach telefonu, między innymi ze strzałkami wysyłanymi przez gładzik Zinwa Q25.

Żeby zmienić przypisanie, wybierz kombinację na ekranie **Nawigacja strzałkami** i wybierz polecenie. Pojedyncza strzałka z kombinacji działa po zwolnieniu, jeśli wcześniej nie dołożysz drugiej. W polu edycji zwykła strzałka przenosi kursor. Kombinacje nadal działają, a lewo i prawo razem wyłączają nawigację po 0,4 sekundy. Każda mapa ma własne ustawienia. NVDA, JAWS i Orca używają własnej nawigacji zamiast tej funkcji.

### Nawigacja jak na komputerze {#keyboards-desktop}

W mapach NVDA, JAWS i Orca oraz ich kopiach strzałki w pionie przechodzą po elementach, a w poziomie po znakach. Ctrl ze strzałką w bok przechodzi po słowach. Ctrl + Home i Ctrl + End prowadzą na początek lub koniec. Tab i Shift + Tab przenoszą fokus czytnika i klawiatury po elementach przyjmujących klawisze. Enter aktywuje element czytnika, chyba że już przyjmuje klawisze. Escape cofa. W edytowanym polu klawisze trafiają do pola. W trybie przeglądania litery prowadzą po rodzajach elementów.

#### Nawigacja obiektowa {#keyboards-object-navigation}

W kategorii **Nawigacja obiektowa** znajdziesz **Przejdź do obiektu nadrzędnego**, **Przejdź do pierwszego obiektu podrzędnego**, **Przejdź do następnego obiektu** i **Przejdź do poprzedniego obiektu**. Polecenia poruszają się po strukturze aplikacji, także po zwykle pomijanych grupach. SandFly podaje „Grupa” i jej liczebność przy kontenerze bez etykiety.

#### Kursor przeglądu {#keyboards-review-cursor}

Kursor przeglądu pozwala czytać tekst w kolejności jego ułożenia na ekranie. Nie przenosi fokusu czytnika. Pozycje obok siebie mogą tworzyć jeden wiersz. Jeśli kursor jest wyłączony, polecenia zaczynają od fokusu SandFly.

W kategorii **Przegląd ekranu** znajdziesz polecenia bieżącego, następnego i poprzedniego wiersza, słowa lub znaku, a także przejścia na początek i koniec wiersza albo listy wierszy. Możesz kliknąć lub przytrzymać w miejscu kursora, przenieść fokus do kursora lub odwrotnie, włączyć i wyłączyć kursor. **Rozpoznaj tekst na ekranie** od Androida 11 rozpoznaje też tekst ze zrzutu ekranu.

Przy włączonym kursorze lub rozpoznanym tekście strzałki w pionie przenoszą po wierszach, strzałki w bok po znakach, a Ctrl ze strzałką w bok po słowach. Home i End prowadzą na krańce wiersza. Ctrl + Home, Ctrl + End oraz Page Up i Page Down prowadzą do skrajnego wiersza. Enter klika, Shift + Enter przytrzymuje, a Escape zamyka rozpoznany tekst.

W JAWS działa to jak kursor JAWS, a w Orca jak płaski przegląd. Skróty są w mapach NVDA, JAWS i Orca. W innych możesz je ustawić na ekranie **Skróty klawiszowe**.

### Przekaż klawisze aplikacji {#keyboards-pass-keys}

- **Przekaż do systemu** oddaje następne naciśnięcie lub kombinację aplikacji.
- **Przekazuj tej aplikacji klawisze bez klawisza SandFly** zapamiętuje wybór dla aplikacji. Dotyczy między innymi strzałek i liter trybu przeglądania. Ponowne polecenie cofa wybór. Aktywny kursor przeglądu nadal przejmuje swoje klawisze.
- **Włącz lub wyłącz obsługę klawiatury przez SandFly** oddaje Androidowi również skróty z klawiszem SandFly. Naciśnij skrót tego polecenia ponownie, żeby wrócić. Ponowne uruchomienie czytnika włącza obsługę.

| Mapa | Następne naciśnięcie | Klawisze aplikacji | Obsługa klawiatury |
| --- | --- | --- | --- |
| SandFly | Klawisz SandFly + Shift + Escape | Brak | Klawisz SandFly + Shift + K |
| VoiceOver | Brak | Brak | Klawisz SandFly + Shift + średnik |
| NVDA | Klawisz SandFly + F2 | Klawisz SandFly + Shift + spacja | Klawisz SandFly + Shift + K |
| JAWS | Klawisz SandFly + 3 | Klawisz SandFly + Shift + Z | Klawisz SandFly + Shift + K |
| Orca | Klawisz SandFly + Backspace | Klawisz SandFly + Shift + A | Klawisz SandFly + Shift + średnik |
| Telefon z klawiaturą | Klawisz SandFly + P | Klawisz SandFly + Shift + Q | Klawisz SandFly + Shift + P |
| Pojedyncze klawisze telefonu | Klawisz SandFly + P | Shift + Q | Klawisz SandFly + Shift + P |

Polecenia znajdziesz w kategorii **Aktywacja**. [Skrypt](#scripts) może oddać aplikacji wszystkie klawisze. Ponowne włączenie obsługi przez SandFly kończy to działanie.

### Telefony z klawiaturą {#keyboards-phones}

SandFly rozpoznaje między innymi Unihertz Titan, BlackBerry KEYone, KEY2 i KEY2 LE oraz Zinwa Q25. Te telefony mają litery i modyfikatory takie jak Shift i Alt, ale zwykle brak strzałek, Ctrl i Meta. Mapa telefonu jest wybierana automatycznie przy pierwszym użyciu. Obie mapy możesz wybrać też ręcznie.

Jeśli wybierzesz **Mapa klawiszy telefonu z klawiaturą**, sam klawisz SandFly przechodzi do następnego elementu, a z Shift do poprzedniego. Enter uaktywnia element, na którym jest fokus. Podwójny Alt steruje multimediami i odbiera rozmowę. Przytrzymanie Alt + Shift wybiera następną opcję sterowania czytaniem. Klawisz SandFly z J, I lub K steruje czytaniem, a z T lub B prowadzi na początek lub koniec. Klawisz SandFly z L przełącza [blokadę dotyku](#touch).

Po wybraniu **Mapa pojedynczych klawiszy telefonu** spacja i Shift + spacja przechodzą do następnego lub poprzedniego elementu. H przechodzi do nagłówka, a J, L, I i K sterują czytaniem. Te klawisze działają poza edytowanym polem. Dodaj klawisz SandFly, żeby użyć ich także w polu. Shift + Z przełącza blokadę dotyku.

W obu mapach Enter aktywuje element po przejściu fokusu. W używanym polu i po wpisaniu tekstu trafia do aplikacji. Klawisze działają bez trybu przeglądania. Nawigacja strzałkami jest domyślnie włączona. Gładzik wysyłający Enter może aktywować element. Jeśli klawiszem SandFly jest Alt, Alt z literą w polu wpisuje przypisaną cyfrę lub symbol. Skróty z Sym, Fn i Ctrl działają także w polach. Zobacz [wykaz skrótów](#appendix-keyboard-shortcuts).

#### Domyślny klawisz SandFly na telefonie {#keyboards-phone-sandfly-key}

Możesz wybrać Sym, Ctrl, prawy Shift, Fn, prawy Alt, Alt albo Meta. Domyślnie SandFly wybiera klawisz, który nie przeszkadza w pisaniu.

- Ctrl na telefonach Unihertz, gdy Fn działa jako Ctrl, z wyjątkiem Titan 2 Elite.
- Prawy Shift na Titan 2 Elite, Zinwa Q25 i BlackBerry z prawym Shift.
- Sym na pozostałych telefonach z klawiszem Sym.
- Alt bez klawisza Sym, na przykład w pierwszym modelu Titan.

Gdy klawiszem czytnika jest Sym, nie otwiera dotychczasowej warstwy symboli. W Zinwa Q25 klawisz Sym wysyła prawy Alt. W KEY2 Fn odpowiada klawiszowi Speed.

#### Ustawienia pisania klawiaturą fizyczną {#keyboards-typing-settings}

Po podłączeniu klawiatury otwórz **Pisanie** > **Klawiatura fizyczna**.

- **Klawisze liter naciskają przyciski klawiatury numerycznej** pojawia się na telefonach, gdzie litery mają dodatkowe cyfry. W, E i R mogą odpowiadać 1, 2 i 3. Działa na ekranie PIN, klawiaturze połączeń i w kalkulatorze. Kalkulator używa też operatorów dostępnych z Alt. Na ekranie blokady litera otwiera klawiaturę PIN. Zmodyfikowane klawisze, cyfry i pisanie w polu zachowują swoje działanie. Opcja jest domyślnie włączona.
- **Enter wysyła wiadomości** naciska przycisk wysyłania, gdy w polu komunikatora jest tekst. Shift + Enter i Alt + Enter trafiają do aplikacji jako nowy wiersz. Działa z dowolną klawiaturą fizyczną. Domyślnie jest wyłączone.
- **Klawiatura SandFly** otwiera [konfigurację metody wprowadzania](#keyboards-sandfly-keyboard). Możesz ustawić działanie przytrzymania, [podwójnego naciśnięcia](#keyboards-double-press) i [dwóch spacji](#keyboards-double-press).
- **Ustawienia klawiatury fizycznej Androida** otwierają ustawienia systemowe, między innymi układ językowy.

### Klawiatura SandFly {#keyboards-sandfly-keyboard}

To opcjonalna metoda wprowadzania dla klawiatur fizycznych. Dzięki niej SandFly wie, kiedy pole rzeczywiście przyjmuje tekst, i może w odpowiednim momencie zmienić działanie liter i Enter. Dodaje wpisywanie przez przytrzymanie i podwójne naciśnięcie, wyszukiwanie emoji, wybór symbolu i tryb edycji. Zwykłe klawisze nadal wpisują znaki. Metoda jest niedostępna na zegarkach i telewizorach.

#### Włącz Klawiaturę SandFly {#keyboards-turn-on-sandfly-keyboard}

1. Podłącz klawiaturę lub użyj wbudowanej.
2. Otwórz **Pisanie** > **Klawiatura fizyczna** > **Klawiatura SandFly**.
3. Włącz **Klawiatura SandFly** na systemowej liście metod wprowadzania.
4. W polu tekstowym wybierz ją przełącznikiem Androida lub poleceniem **Przełącz klawiaturę**. Polecenie wymaga Androida 11 i przechodzi do następnej włączonej klawiatury.

Po wybraniu tej metody klawiatura dotykowa jest ukryta.

#### Przytrzymywanie klawiszy {#keyboards-held-keys}

Otwórz **Pisanie** > **Klawiatura fizyczna** > **Wpisywanie po przytrzymaniu klawisza**. Domyślnie SandFly wpisuje znak dostępny z Alt, a jeśli go nie ma, wielką literę. Możesz też wybrać samą wielką literę lub powtarzanie znaku. Najpierw pojawia się zwykły znak klawisza. Gdy Android zacznie powtarzać naciśnięcie, SandFly zastępuje go wynikiem przytrzymania.

#### Podwójne naciśnięcie {#keyboards-double-press}

Otwórz **Pisanie** > **Klawiatura fizyczna** > **Znaki podwójnego naciśnięcia**. Użyj **Dodaj klawisz**. Wpisz znak w **Klawisz** i od 1 do 16 znaków w **Podwójne naciśnięcie wpisuje**. Wpis możesz edytować lub usunąć. Lista jest początkowo pusta.

Pierwsze naciśnięcie wpisuje zwykły znak. Drugie w ciągu 350 ms zastępuje go nowym, jeśli między nimi nie naciśniesz innego klawisza ani nie przesuniesz kursora. Dla wielkiej litery SandFly używa wielkiego zamiennika. Możesz na przykład wpisać ğ dwoma naciśnięciami G. Domyślnie wyłączone **Podwójna spacja wstawia kropkę** dodaje kropkę i spację po literze lub cyfrze. Te funkcje są pomijane w polach hasła.

#### Wyszukaj emoji lub wybierz symbol {#keyboards-emoji}

1. W edytowanym polu z Klawiaturą SandFly uruchom **Wyszukaj emotikony** albo **Wybierz symbol**. Ostatnio używane znaki są pierwsze.
2. Wpisz początek jednego lub kilku słów nazwy. Nazwy emoji pojawiają się w języku telefonu.
3. Przejdź po wynikach strzałkami lub Tab i Shift + Tab. SandFly podaje pozycję.
4. Naciśnij Enter, żeby wstawić znak.

Backspace usuwa literę, a przy pustym wyszukiwaniu zamyka listę. Escape lub Wstecz również ją zamykają. W mapach telefonu emoji otworzysz klawiszem SandFly + O, a symbole przez Alt + spacja podczas edycji. Alt + spacja działa też na Zinwa Q25. W innej mapie ustaw skrót w kategorii **Edytowanie tekstu**.

#### Tryb edycji {#keyboards-editing-mode}

Uruchom **Włącz lub wyłącz tryb edycji** w polu z Klawiaturą SandFly. W mapach telefonu użyj klawisza SandFly + C. W tym trybie litery służą do edycji.

- J i L lub lewa i prawa strzałka przechodzą po znakach. I i K lub strzałki pionowe przechodzą po wierszach. U i O przechodzą po słowach.
- Alt + J i Alt + L przechodzą na początek i koniec wiersza. Alt + I i Alt + K idą na początek i koniec tekstu. T i B też prowadzą na krańce tekstu. Home i End dotyczą wiersza, a z Ctrl całego tekstu.
- Shift z ruchem zaznacza. S zaczyna lub kończy zaznaczanie. A zaznacza wszystko, C kopiuje, X wycina, a V wkleja.
- Z cofa, Shift + Z ponawia, R czyta zaznaczenie albo wiersz. Klawisze usuwania usuwają tekst.
- Enter, Escape lub Wstecz kończą tryb. Inne klawisze piszące są wtedy pomijane.

### Pozostałe ustawienia klawiatury {#keyboards-other-settings}

Otwórz **Pisanie** > **Echo klawiatury** > **Klawiatura fizyczna**, żeby ustawić [odczyt podczas pisania](#typing). Większość klawiszy przerywa mowę. Klawisze blokad, modyfikatory i głośność są wyjątkami. Znaki i Enter podlegają [ustawieniom mowy](#speech-stop), a sam Ctrl wstrzymuje lub wznawia odczyt.

Jeśli przy pierwszym uruchomieniu jest podłączona klawiatura, SandFly otwiera samouczek klawiszy. Gdy masz też ekran dotykowy, możesz później wybrać [samouczek dotykowy lub klawiaturowy](#getting-started).
