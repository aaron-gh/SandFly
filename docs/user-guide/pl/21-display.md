## Wyświetlanie, ostatnie aplikacje i szybkie ustawienia {#display}

Na ekranie **Wyświetlanie** w ustawieniach SandFly możesz zmienić obramowanie fokusu, animacje okien i tapetę. W tym rozdziale opisano też ukrywanie ekranu, zmianę jasności, listę ostatnich aplikacji w **Przegląd**, podzielony ekran i panel **Szybkie ustawienia** SandFly.

### Zmień obramowanie fokusu {#display-focus-indicator}

Wskaźnik zaznaczenia to ramka, którą SandFly rysuje wokół elementu, na którym jest fokus. Od Androida 12 znajdziesz jej ustawienia w **Wyświetlanie** > **Wskaźnik zaznaczenia**.

- **Pokazuj wskaźnik zaznaczenia** włącza ramkę. Opcja jest domyślnie włączona.
- **Grube obramowanie** poszerza ramkę.
- W ustawieniu **Kolor** wybierz **Domyślny**, **Czerwony**, **Pomarańczowy**, **Żółty**, **Zielony**, **Niebieski** lub **Szary**.

Polecenie **Pokazuj wskaźnik zaznaczenia** w kategorii **SandFly** włącza lub wyłącza ramkę. Zobacz [](#commands).

### Wyłącz animacje {#display-animations}

Opcja **Wyświetlanie** > **Wyłącz animacje** wyłącza animacje okien w całym telefonie. Dzięki temu SandFly szybciej odczytuje nowe ekrany. Opcja jest domyślnie włączona i działa od Androida 13. Gdy ją wyłączysz lub wyłączysz SandFly, wróci poprzednie ustawienie animacji.

### Ukryj ekran {#display-hide-screen}

Ukrywanie ekranu zasłania go na czarno, łącznie z paskiem nawigacji. Osoby obok nie widzą jego zawartości, a Ty nadal możesz normalnie korzystać z SandFly i aplikacji.

1. Otwórz [menu SandFly](#commands).
2. Wybierz **Ukryj lub pokaż ekran**.
3. W oknie **Ukryć ekran?** wybierz **OK**.

SandFly odczyta „Ekran ukryty” i odtworzy dźwięk. Żeby odsłonić ekran, jeszcze raz wybierz **Ukryj lub pokaż ekran**. Usłyszysz „Ekran jest widoczny”.

Możesz też ukrywać i odsłaniać ekran na dwa inne sposoby.

- Przypisz gest lub skrót do polecenia **Ukryj lub pokaż ekran** w grupie **SandFly**. Zobacz [](#commands).
- Włącz **Ukrywanie ekranu** na ekranie **Sterowanie czytaniem**. Opcja jest domyślnie wyłączona. Po jej wybraniu przesuń palcem w górę lub w dół, żeby ukryć lub odsłonić ekran.

W oknie potwierdzenia możesz wyłączyć **Zawsze pokazuj to ostrzeżenie**. Po ukryciu ekranu usłyszysz wtedy sam komunikat o ukryciu, bez instrukcji odsłonięcia.

Jeśli przyznasz uprawnienie **Zmienianie ustawień systemowych**, SandFly przy ukrywaniu ekranu zmniejszy też jasność do minimum. Przy odsłonięciu przywróci poprzednią jasność. Jeśli uprawnienia jeszcze nie ma, przy pierwszym ukryciu SandFly otworzy odpowiedni ekran ustawień Androida.

Uwaga: Ekran pozostaje ukryty po ponownym uruchomieniu SandFly. Nie możesz go ukryć, gdy telefon jest zablokowany. Przed Androidem 14 opisy obrazów są niedostępne przy ukrytym ekranie.

Sygnały ukrycia i odsłonięcia możesz zmienić według instrukcji w [](#sound). Komunikat o ukryciu przy wybudzeniu opisano w [](#verbosity).

### Zmień jasność ekranu {#display-brightness}

Opcja **Jasność** w sterowaniu czytaniem zmienia jasność systemową, również przy ukrytym ekranie. Jest domyślnie dostępna na ekranie **Sterowanie czytaniem**.

1. Wybierz opcję sterowania czytaniem **Jasność**. Jak wybierać opcje, opisano w [](#reading-controls).
2. Przesuń palcem w górę, żeby rozjaśnić ekran, lub w dół, żeby go przyciemnić.

Każde przesunięcie zmienia jasność o 10 procent skali systemowego suwaka. SandFly podaje nowy poziom, na przykład „Jasność 40 procent”. Przy pierwszej zmianie SandFly otworzy ustawienia Androida, w których możesz przyznać uprawnienie **Zmienianie ustawień systemowych**. Przy włączonej jasności adaptacyjnej usłyszysz „Wyłącz jasność adaptacyjną, aby ustawić ją ręcznie”.

### Ustaw tapetę SandFly {#display-wallpaper}

Niektóre telefony, między innymi Vivo, mogą zatrzymać SandFly w tle mimo wyłączenia optymalizacji baterii. Android utrzymuje działanie aktywnej tapety i wznawia ją po zatrzymaniu. Tapeta SandFly działa w tym samym procesie co usługa dostępności. Na tych telefonach jej ustawienie pozwala utrzymać działanie SandFly.

1. Otwórz **Wyświetlanie** > **Użyj SandFly jako tapety**.
2. Ustaw tapetę na otwartym ekranie Androida.

Po ustawieniu opcji jako tapety pozycja w ustawieniach informuje, że SandFly jest bieżącą tapetą. Domyślnie tapeta jest czarna. Żeby ustawić własną, wybierz **Wyświetlanie** > **Obraz tapety** i wskaż obraz. Usłyszysz „Ustawiono obraz tapety” lub „Nie można użyć tego obrazu”.

Te ustawienia są dostępne na telefonach i tabletach.

### Przegląd ostatnich aplikacji SandFly {#display-overview}

Przegląd SandFly pokazuje ostatnie aplikacje na liście lub w siatce. SandFly odczytuje je po kolei. Żeby otwierać ten przegląd zamiast systemowego, włącz **Przegląd** > **Używaj przeglądu SandFly**. Opcja jest domyślnie wyłączona.

SandFly używa Shizuku lub roota do pobierania i otwierania ostatnich aplikacji. Zobacz [](#updates). Jeśli Shizuku nie działa lub SandFly nie ma dostępu, usłyszysz „Shizuku nie działa” albo „Shizuku nie zezwala SandFly” i otworzy się przegląd systemowy. Na ekranie blokady otwiera się przegląd systemowy.

W **Przegląd** znajdziesz także dwie opcje.

- **Najnowsza aplikacja na dole** umieszcza ostatnio używaną aplikację na końcu listy i od razu tam przewija. Opcja jest domyślnie wyłączona.
- **Kolumny** ustawia układ. Jedna kolumna pokazuje listę. Dwie, trzy lub cztery pokazują siatkę o wybranej szerokości. Domyślnie ustawiona jest jedna kolumna.

Tych ustawień nie ma na telewizorach.

#### Korzystaj z przeglądu {#display-overview-use}

Przy włączonym **Używaj przeglądu SandFly** polecenie **Przegląd** w kategorii **Czynności systemowe** otworzy listę SandFly. Przegląd SandFly pojawi się też nad systemowym po użyciu gestu nawigacji Androida lub przycisku ostatnich aplikacji.

- Stuknij dwukrotnie na aplikacji, żeby do niej przejść.
- Otwórz menu **Czynności** danej aplikacji, żeby wybrać **Zamknij**, **Podziel ekran** lub **Informacje o aplikacji**. **Podziel ekran** otwiera wybraną aplikację obok tej, z której otwarto przegląd. **Informacje o aplikacji** otwiera odpowiednią stronę ustawień Androida.
- Naciśnij Delete na klawiaturze fizycznej, żeby zamknąć aplikację, na której jest fokus.
- U dołu po lewej wybierz **Zamknij wszystkie**, żeby zamknąć wszystkie ostatnie aplikacje.
- U dołu po prawej wybierz **Przegląd systemowy**, żeby otworzyć systemową listę.

Gdy przegląd jest otwarty, ponownie uruchom **Przegląd**, żeby przejść do poprzedniej aplikacji. Osobne polecenie **Przegląd systemowy** otwiera listę Androida także wtedy, gdy **Używaj przeglądu SandFly** jest włączone.

#### Przełączaj się między aplikacjami {#display-switch-apps}

W kategorii **Czynności systemowe** znajdziesz trzy polecenia. Domyślnie nie mają przypisań. Zobacz [](#commands).

- **Poprzednia aplikacja** otwiera aplikację używaną bezpośrednio przed bieżącą. Jeśli nie ma dokąd przejść, usłyszysz „Brak poprzedniej aplikacji”. Bez Shizuku SandFly otworzy przegląd systemowy i wybierze w nim poprzednią aplikację.
- **Starsza aplikacja** przechodzi do kolejnej starszej pozycji na liście. Wykonaj polecenie ponownie, żeby cofnąć się dalej. SandFly podaje nazwę każdej aplikacji, a na końcu listy „Brak starszej aplikacji”. Bez Shizuku polecenie działa jak **Poprzednia aplikacja**.
- **Nowsza aplikacja** przechodzi w stronę najnowszej pozycji po użyciu **Starsza aplikacja**. Przy ostatniej pozycji usłyszysz „Brak nowszej aplikacji”. Polecenie wymaga Shizuku.

Gdy **Używaj przeglądu SandFly** jest włączone i Shizuku działa, możesz przełączać aplikacje z klawiatury fizycznej bez otwierania przeglądu.

1. Przytrzymaj Alt i naciśnij Tab. SandFly odczyta nazwę poprzedniej aplikacji.
2. Naciśnij Tab ponownie, żeby przejść do starszych aplikacji, lub Shift + Tab, żeby wrócić w stronę nowszych.
3. Puść Alt, żeby otworzyć ostatnio odczytaną aplikację. Żeby zostać w bieżącej aplikacji, naciśnij Escape przed puszczeniem Alt.

Skrót Alt + Tab jest niedostępny, jeśli Alt jest [klawiszem SandFly](#keyboards).

#### Podziel ekran {#display-split-screen}

Żeby wyświetlić dwie aplikacje obok siebie, użyj **Podziel ekran** w menu **Czynności** aplikacji w [przeglądzie SandFly](#display-overview-use).

Przy podzielonym ekranie w menu SandFly znajdziesz **Podziel ekran**. Otwiera **Przejdź do separatora** i czynności separatora, między innymi zmianę proporcji i zamianę aplikacji miejscami. To samo menu otwiera polecenie **Podziel ekran** z kategorii **Czynności systemowe**. Jeśli okna nie są podzielone, usłyszysz „Ekran nie jest podzielony”. W układzie dzielonym Wstecz i klawisze służące do wpisywania trafiają do aplikacji, w której ostatnio przenosiłeś fokus.

### Panel szybkich ustawień SandFly {#display-quick-settings}

W panelu SandFly możesz ułożyć własną siatkę przełączników, suwaków i przycisków. Żeby otwierać ją zamiast panelu systemowego, włącz **Szybkie ustawienia** > **Używaj szybkich ustawień SandFly**. Opcja jest domyślnie wyłączona.

#### Korzystaj z panelu {#display-quick-settings-use}

Panel otwiera polecenie **Szybkie ustawienia** z kategorii **Czynności systemowe** lub przycisk **Szybkie ustawienia** na [liście powiadomień](#notifications) SandFly. Na ekranie blokady otwierają się szybkie ustawienia systemowe.

- SandFly podaje wiersz i kolumnę elementu, jego nazwę i stan, na przykład nazwę połączonej sieci Wi-Fi.
- Stuknij dwa razy, żeby zmienić stan przełącznika. W opcjach z kilkoma wartościami, takich jak **Tryb dźwięku** lub **Limit czasu ekranu**, dwukrotne stuknięcie wybiera następną wartość.
- Na suwaku, takim jak **Jasność** lub **Głośność multimediów**, przesuń palcem w górę albo w dół, żeby zmienić wartość o jeden krok.
- Stuknij dwa razy i przytrzymaj po drugim stuknięciu lub użyj czynności **Otwórz ustawienia**, żeby przejść do ustawień Androida dla danego elementu.
- **Edytuj** otwiera ekran wyboru elementów. Zobacz [](#display-quick-settings-edit).
- **Szybkie ustawienia systemu** otwiera panel systemowy.

Polecenie **Szybkie ustawienia systemu** otwiera panel Androida także przy włączonej opcji **Używaj szybkich ustawień SandFly**.

Android pozwala zmieniać niektóre ustawienia tylko systemowi lub procesowi z uprawnieniami powłoki. Przy działającym Shizuku SandFly może bezpośrednio włączać lub wyłączać Wi-Fi, Bluetooth, dane mobilne, hotspot, tryb samolotowy, lokalizację, NFC, oszczędzanie baterii i danych, ciemny motyw oraz dostęp do mikrofonu i kamery. Nadaje sobie też uprawnienia potrzebne do pozostałych elementów panelu. Bez Shizuku te przełączniki otwierają odpowiednie ustawienia Androida. Zobacz [](#updates).

SandFly aktywuje kafelki aplikacji i kafelki systemowe w panelu Androida. Otwiera panel systemowy, aktywuje kafelek, zamyka panel bez odczytu i podaje nowy stan kafelka. Bez Shizuku kafelek działa tylko wtedy, gdy znajduje się w panelu Androida. Jeśli SandFly nie znajdzie kafelka, usłyszysz jego nazwę i komunikat „nie znajduje się w szybkich ustawieniach systemu”. Gdy Shizuku działa, SandFly najpierw dodaje kafelek aplikacji do panelu Androida, jeśli go tam nie ma, a gdy go tam nie znajdzie, aktywuje go przez Shizuku.

#### Wybierz zawartość panelu {#display-quick-settings-edit}

Otwórz **Szybkie ustawienia** > **Panel szybkich ustawień** lub wybierz **Edytuj** w samym panelu. Przy każdym elemencie na liście zobaczysz numer wiersza i kolumny.

- **Dodaj element sterujący** wyświetla elementy SandFly, których jeszcze nie ma w panelu. Na liście są tylko funkcje dostępne na urządzeniu.
  - Połączenia i dane obejmują **Wi-Fi**, **Bluetooth**, **Dane mobilne**, **Hotspot**, **Tryb samolotowy**, **Oszczędzanie danych**, **Lokalizacja** i **NFC**.
  - Dźwięk i zasilanie obejmują **Nie przeszkadzać**, **Tryb dźwięku**, **Oszczędzanie baterii**, **Głośność multimediów**, **Głośność połączenia**, **Głośność dzwonka**, **Głośność powiadomień**, **Głośność alarmu**, **Głośność dostępności**, **Głośność dźwięku systemowego**, **Następny alarm** i **Bateria**.
  - Obraz i oświetlenie obejmują **Latarka**, **Jasność latarki**, **Automatyczne obracanie**, **Ciemny motyw**, **Nocne światło**, **Bardzo przyciemnione**, **Inwersja kolorów**, **Korekta koloru**, **Jasność**, **Jasność adaptacyjna**, **Limit czasu ekranu**, **Rozmiar czcionki** i **Rozmiar wyświetlacza**.
  - Pozostałe elementy to **Dostęp do mikrofonu**, **Dostęp do kamery**, **Media**, **Wyjście multimedialne**, **Ustawienia** i **Menu zasilania**.
- **Dodaj kafelek aplikacji** wyświetla kafelki szybkich ustawień oferowane przez zainstalowane aplikacje.
- **Dodaj kafelek systemowy** wyświetla kafelki z szybkich ustawień Androida. Jeśli kafelek ma odpowiednik w SandFly, zostanie dodany jako element SandFly. Tak działa na przykład **Hotspot**. Gdy Shizuku działa, SandFly przyznaje sobie uprawnienie DUMP i dostęp do danych o użyciu, a potem odczytuje listę z systemu. W przeciwnym razie przegląda szybkie ustawienia systemu, co trwa kilka sekund.
- **Liczba kolumn** pozwala ustawić od 1 do 4 kolumn siatki. Domyślnie są dwie.

Żeby zmienić pozycję elementu lub go usunąć, stuknij na nim dwa razy albo otwórz jego czynności.

- **Przenieś** wyświetla komórki siatki. Element zajmie wybraną komórkę. Pozostałe pozycje pomiędzy starym a nowym miejscem przesuną się o jedną komórkę. SandFly poda, ile z nich zostało przesuniętych.
- **Usuń** usuwa element z panelu.

Domyślnie panel zawiera **Wi-Fi**, **Bluetooth**, **Dane mobilne**, **Tryb samolotowy**, **Nie przeszkadzać**, **Tryb dźwięku**, **Latarka**, **Lokalizacja**, **Automatyczne obracanie**, **Oszczędzanie baterii**, **Jasność**, **Głośność multimediów**, **Głośność dzwonka**, **Głośność dostępności**, **Media** i **Bateria**.
