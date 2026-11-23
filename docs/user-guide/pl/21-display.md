## Wyświetlanie, ostatnie aplikacje i szybkie ustawienia {#display}

W ustawieniach **Wyświetlanie** możesz zmienić obramowanie fokusu, animacje i tapetę. Z tego rozdziału dowiesz się też, jak ukryć ekran, zmienić jasność oraz korzystać z przeglądu aplikacji i panelu szybkich ustawień SandFly.

### Obramowanie fokusu {#display-focus-indicator}

W Androidzie 12 i nowszym otwórz **Wyświetlanie** > **Wskaźnik zaznaczenia**. SandFly może obrysować element, na którym jest fokus dostępności.

- **Pokazuj wskaźnik zaznaczenia** jest domyślnie włączone.
- **Grube obramowanie** poszerza obrys.
- W ustawieniu koloru możesz użyć domyślnego albo wybrać czerwony, pomarańczowy, żółty, zielony, niebieski lub szary.

[Polecenie](#commands) **Pokazuj wskaźnik zaznaczenia** w kategorii **SandFly** przełącza obrys bez wchodzenia w ustawienia.

### Animacje {#display-animations}

Domyślnie włączone **Wyświetlanie** > **Wyłącz animacje** usuwa animacje okien w całym telefonie, żeby szybciej czytać nowe ekrany. Działa od Androida 13. Po wyłączeniu tej opcji albo samego SandFly wracają poprzednie ustawienia animacji.

### Ukrywanie ekranu {#display-hide-screen}

Ukrycie zasłania ekran na czarno, wraz z paskiem nawigacji. SandFly i aplikacje działają dalej.

1. Otwórz [menu SandFly](#commands) i wybierz **Ukryj lub pokaż ekran**.
2. W oknie **Ukryć ekran?** wybierz **OK**.
3. Po komunikacie ekran jest ukryty. Użyj tego samego polecenia, żeby go odsłonić.

Do polecenia możesz przypisać gest lub klawisz. Możesz też włączyć **Ukrywanie ekranu** na ekranie **Sterowanie czytaniem** i użyć ruchu w górę lub w dół. Jeśli nie chcesz za każdym razem potwierdzać ukrycia, odznacz **Zawsze pokazuj to ostrzeżenie**.

Po udzieleniu zgody na zmianę ustawień systemowych SandFly ogranicza też jasność do minimum i przywraca ją po odsłonięciu. Jeżeli zgody nie ma, otworzy ustawienia Androida przy pierwszym użyciu. Ekran zostaje ukryty po ponownym uruchomieniu czytnika. Na ekranie blokady nie można go ukryć. W wersjach starszych niż Android 14 ukrycie wyłącza możliwość opisywania obrazów.

Zobacz [sygnały ukrycia i pokazania](#sound) oraz [komunikaty po wybudzeniu](#verbosity).

### Jasność {#display-brightness}

Wybierz **Jasność** na liście [opcji sterowania czytaniem](#reading). Przesuń palec w górę, żeby rozjaśnić ekran, lub w dół, żeby go przyciemnić. Każdy ruch zmienia wartość o 10% skali systemowej. SandFly podaje wynik. Możesz to zrobić również przy ukrytym ekranie.

Opcja jest domyślnie dostępna. Przy pierwszym użyciu przyznaj zgodę na zmianę ustawień systemowych. Jeśli działa automatyczna jasność, SandFly poprosi o jej wyłączenie.

### Tapeta SandFly {#display-wallpaper}

Niektóre telefony, między innymi Vivo, potrafią zatrzymać czytnik w tle mimo wyłączenia optymalizacji baterii. Android utrzymuje aktywną tapetę i uruchamia ją ponownie w razie zatrzymania. Tapeta SandFly działa w jego procesie, więc może pomóc zachować działanie czytnika.

Otwórz **Wyświetlanie** > **Użyj SandFly jako tapety**. Ustaw tapetę na otwartym ekranie Androida. Domyślnie jest czarna. Jeśli wolisz własny obraz, wybierz **Wyświetlanie** > **Obraz tapety**. Te opcje są dostępne na telefonach i tabletach.

### Przegląd ostatnich aplikacji SandFly {#display-overview}

Otwórz **Przegląd** > **Używaj przeglądu SandFly**, żeby zamiast systemowej listy otwierać listę lub siatkę SandFly. Domyślnie opcja jest wyłączona.

Do odczytu i otwierania ostatnich aplikacji potrzeba [Shizuku albo roota](#updates). Jeśli Shizuku nie działa lub nie zezwala na dostęp, SandFly poda przyczynę i otworzy przegląd systemowy. Na ekranie blokady działa przegląd systemu.

**Najnowsza aplikacja na dole** otwiera listę przewiniętą na koniec. Początkowo jest wyłączone. Wybierz 1 kolumnę dla listy albo 2, 3 lub 4 dla siatki. Domyślnie jest jedna. Tych ustawień nie ma na telewizorach.

#### Korzystanie z przeglądu {#display-overview-use}

Jeśli włączysz przegląd SandFly, otworzy się po poleceniu **Przegląd**, po systemowym geście nawigacji i po użyciu przycisku ostatnich aplikacji.

- Stuknij dwukrotnie na aplikacji, żeby do niej przejść.
- W menu **Czynności** aplikacji użyj **Zamknij**, **Podziel ekran** lub **Informacje o aplikacji**. Podział otwiera aplikację obok tej, z której przyszedłeś. Informacje otwierają ustawienia Androida.
- Delete na klawiaturze zamyka aplikację, na której jest fokus.
- **Zamknij wszystkie** u dołu po lewej zamyka wszystkie ostatnie aplikacje. **Przegląd systemowy** u dołu po prawej otwiera listę Androida.

Ponowne użycie **Przegląd** z otwartej listy przełącza do poprzedniej aplikacji. Polecenie **Przegląd systemowy** zawsze otwiera listę Androida.

#### Przełączanie między aplikacjami {#display-switch-apps}

W kategorii [Czynności systemowe](#commands) są trzy polecenia bez domyślnych skrótów:

- **Poprzednia aplikacja** otwiera ostatnią używaną przed bieżącą. Bez Shizuku SandFly robi to przez przegląd systemowy.
- **Starsza aplikacja** przechodzi dalej wstecz po liście, za każdym razem podając nazwę lub koniec listy. Bez Shizuku działa jak poprzednie polecenie.
- **Nowsza aplikacja** wraca w stronę najnowszej. Wymaga Shizuku.

Jeśli działa przegląd SandFly i Shizuku, przytrzymaj Alt i naciśnij Tab. SandFly poda poprzednią aplikację bez otwierania listy. Kolejne Tab przechodzą wstecz, a Shift + Tab w przód. Puść Alt, aby przełączyć się na odczytaną aplikację. Użyj Escape przed puszczeniem, żeby pozostać w bieżącej. Alt + Tab nie działa w ten sposób, jeśli [klawiszem SandFly](#keyboards) jest Alt.

#### Podzielony ekran {#display-split-screen}

Użyj **Podziel ekran** z menu czynności aplikacji w [przeglądzie](#display-overview-use). Przy podzielonym ekranie w menu SandFly pojawi się **Podziel ekran** z pozycją **Przejdź do separatora** i jego czynnościami, na przykład zmianą rozmiaru i zamianą aplikacji. To samo menu otwiera polecenie **Podziel ekran** z **Czynności systemowe**. Wstecz i klawisze wpisywania trafiają do aplikacji, w której ostatnio poruszałeś fokusem.

### Panel szybkich ustawień SandFly {#display-quick-settings}

SandFly może wyświetlić własną siatkę przełączników, suwaków i przycisków. Włącz **Szybkie ustawienia** > **Używaj szybkich ustawień SandFly**. Domyślnie włącza się panel Androida.

#### Korzystanie z panelu {#display-quick-settings-use}

Użyj polecenia **Szybkie ustawienia** lub przycisku **Szybkie ustawienia** na [liście powiadomień](#notifications). Przy zablokowanym ekranie otworzy się panel systemowy. Polecenie **Szybkie ustawienia systemu** otwiera go zawsze.

- SandFly podaje wiersz, kolumnę, nazwę i stan, na przykład nazwę sieci Wi-Fi.
- Stuknij dwukrotnie, żeby przełączyć opcję lub wybrać następną wartość, na przykład w **Tryb dźwięku** lub **Limit czasu ekranu**.
- Na suwaku, takim jak **Jasność** lub **Głośność multimediów**, przesuń palec w górę albo w dół, żeby zmienić wartość o krok.
- Stuknij dwukrotnie i przytrzymaj albo wybierz czynność **Otwórz ustawienia**, żeby otworzyć ustawienia Androida dla elementu.
- **Edytuj** [zmienia zawartość panelu](#display-quick-settings-edit).

[Shizuku](#updates-shizuku) pozwala w panelu zmieniać między innymi Wi-Fi, Bluetooth, dane mobilne, hotspot, samolot, lokalizację, NFC, oszczędzanie baterii i danych, ciemny motyw oraz dostęp do mikrofonu i aparatu. Bez Shizuku takie przełączniki otwierają ustawienia Androida.

Kafelki systemowe i kafelki aplikacji używane bez Shizuku muszą być dodane w systemowych szybkich ustawieniach. SandFly otwiera panel Androida, po cichu używa kafelka, zamyka panel i mówi o nowym stanie. Jeśli kafelka nie ma, podaje nazwę brakującej pozycji.

#### Zawartość i układ panelu {#display-quick-settings-edit}

Użyj **Edytuj** w panelu albo otwórz **Szybkie ustawienia** > **Panel szybkich ustawień**. Lista podaje wiersz i kolumnę każdej pozycji.

- **Dodaj element sterujący** wybiera spośród dostępnych opcji urządzenia. Należą do nich połączenia, dane, tryb samolotowy, Nie przeszkadzać, dźwięk, latarka i jej jasność, lokalizacja, obrót, oszczędzanie, motyw i kolory, NFC, mikrofon i aparat, jasność i jej automatyka, wygaszanie, rozmiar czcionki i ekranu, multimedia i ich wyjście, poszczególne kanały głośności, najbliższy alarm, bateria, ustawienia i menu zasilania.
- **Dodaj kafelek aplikacji** i **Dodaj kafelek systemowy** wyświetlają odpowiednie listy. Jeśli SandFly ma własny element o tym samym działaniu, doda swój, na przykład hotspot.
- Liczba kolumn wynosi od 1 do 4. Domyślny układ ma 2 kolumny.

Stuknij dwukrotnie na pozycji lub użyj jej menu czynności. **Przenieś** pozwala wybrać nową komórkę. Pozostałe pozycje przesuną się o jedno miejsce. **Usuń** usuwa pozycję z panelu.

Domyślny zestaw to Wi-Fi, Bluetooth, dane mobilne, tryb samolotowy, Nie przeszkadzać, tryb dźwięku, latarka, lokalizacja, obrót, oszczędzanie baterii, jasność, głośność multimediów, dzwonka i dostępności, multimedia oraz bateria.
