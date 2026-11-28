## Makra {#macros}

Makro to nazwana lista kroków, które SandFly wykonuje po kolei. Kroki mogą uruchamiać polecenia, stukać w elementy, wpisywać tekst, czekać, sprawdzać ekran oraz powtarzać lub pomijać inne kroki. Każde makro jest poleceniem. Możesz je uruchomić gestem, skrótem klawiszowym, pozycją menu, poleceniem głosowym lub skrótem na ekranie głównym.

Makra tworzysz na ekranie **Makra** w ustawieniach SandFly. Możesz dodawać kroki albo nagrywać swoje czynności. Na zegarkach makra są niedostępne. Więcej możliwości dają skrypty, opisane w [](#scripts).

### Utwórz makro {#macros-add}

1. W ustawieniach SandFly wybierz **Makra**.
2. Wybierz **Dodaj makro** i wpisz nazwę. SandFly otworzy ekran nowego makra.
3. Wybierz **Dodaj krok**, a potem rodzaj kroku. Uzupełnij pola określające, co ma się wykonać. Zobacz [](#macros-steps).
4. Powtarzaj krok trzeci dla kolejnych czynności.
5. Aby wypróbować makro, wybierz **Uruchom teraz**.
6. Aby uruchamiać makro gestem lub skrótem, wybierz **Gesty i skrót klawiszowy**. Zobacz [](#commands-assign).

Jeśli chcesz nagrać kroki, wykonując je samodzielnie, zobacz [](#macros-record).

### Ekran Makra {#macros-screen}

Na ekranie **Makra** znajdziesz poniższe pozycje.

- **Dodaj makro** tworzy makro dostępne we wszystkich aplikacjach.
- **Dodaj aplikację** pozwala wybrać aplikację i otworzyć listę jej makr. Wybierz tam **Dodaj makro**, aby utworzyć makro działające tylko w tej aplikacji.
- **Importuj z pliku** wczytuje makra z pliku. **Eksportuj do pliku** zapisuje wszystkie makra. Na liście makr aplikacji zapisuje tylko makra tej aplikacji. Zobacz [](#api-item-files).
- **Wszystkie aplikacje** pokazują makra działające w każdej aplikacji.
- **Aplikacje** pokazują aplikacje mające makra wraz z ich liczbą. Wybierz aplikację, aby otworzyć jej listę makr.

Makra są też zapisywane w kopiach zapasowych ustawień. Zobacz [](#profiles).

### Ekran makra {#macros-macro-screen}

Na ekranie konkretnego makra znajdziesz te opcje.

- **Uruchom teraz** wykonuje makro. Gdy SandFly jest wyłączony, pojawia się komunikat o jego wyłączeniu.
- **Dodaj krok** dodaje czynność. Poniżej pojawia się miejsce wstawienia, np. **Na końcu**, **Na początku** albo za wybranym krokiem. Nowy krok trafia po ostatnio dodanym. Dzięki temu kroki dodane po **Jeśli** lub po początku powtarzania trafiają do jego wnętrza.
- **Nagraj kroki** dodaje nagrane działania w tym samym miejscu co **Dodaj krok**. Zobacz [](#macros-record).
- **Uruchamiaj tylko wtedy, gdy** pozwala ustawić warunek uruchomienia makra. Domyślnie wybrano **Zawsze**. Jeśli warunek jest niespełniony, makro się nie wykona, a **Uruchom teraz** odczyta informację o niespełnionym warunku. Zobacz [](#macros-conditions).
- **Aplikacja** wskazuje aplikację, w której makro ma działać. Domyślna wartość to **Wszystkie aplikacje**. Makro przypisane do aplikacji działa tylko na jej pierwszym planie. Jeśli wybierzesz **Uruchom teraz** poza nią, SandFly poinformuje, w której aplikacji działa makro.
- **Pokazuj w czynnościach** dodaje makro do menu **Czynności** i do elementu **Czynności** w sterowaniu czytaniem. Pojawia się po czynnościach aplikacji dla każdego jej elementu lub w każdej aplikacji przy ustawieniu **Wszystkie aplikacje**. Wybierz makro na liście, aby je uruchomić. Domyślnie opcja jest wyłączona.
- **Pokazuj w czynnościach tylko, gdy** pojawia się po włączeniu **Pokazuj w czynnościach**. Pozwala określić warunek, który musi spełnić element, aby pojawiło się makro. Jeśli ustawisz warunek, makro liczy się jako czynność elementu dla opcji **Automatycznie zmieniaj opcję sterowania czytaniem**. SandFly wybierze wtedy **Czynności** przy tym elemencie. Bez warunku makro nie wywołuje tej zmiany.
- **Kroki** pokazują czynności w kolejności wykonania. Kroki w warunku **Jeśli** lub w pętli są wcięte.
- **Gesty i skrót klawiszowy** pokazują gesty, strzałki, komórki brajlowskie oraz skrót uruchamiające makro.
- **Dodaj do ekranu głównego** tworzy skrót uruchamiający makro na ekranie, z którego je otworzysz. Zmiana nazwy makra zmienia nazwę skrótu, a usunięcie makra go wyłącza. Skróty zależą od obsługi w aplikacji ekranu głównego.
- **Zmień nazwę**, **Eksportuj do pliku** i **Usuń makro** pozwalają zmienić nazwę, zapisać lub usunąć makro.

Po wybraniu kroku możesz użyć **Edytuj**, **Dodaj krok po tym kroku**, **Przenieś w górę**, **Przenieś w dół** i **Usuń**. Te same pozycje znajdziesz w czynnościach ułatwień dostępu kroku. Usunięcie **Jeśli** lub początku pętli usuwa powiązane kroki **W przeciwnym razie, jeśli**, **W przeciwnym razie** i krok kończący. Kroki znajdujące się w środku zostają. Makro może mieć najwyżej 200 kroków.

Jeśli w makrze brakuje pasującego kroku kończącego, np. po **Jeśli** nie ma **Koniec warunku**, SandFly wskazuje krok i problem pod nagłówkiem **Kroki**. Makra z takim błędem nie można uruchomić.

### Kroki {#macros-steps}

Pod **Dodaj krok** znajdziesz poniższe rodzaje kroków.

- **Uruchom polecenie** wykonuje dowolne polecenie poza innym makrem. Wybierz grupę i polecenie.
- **Kliknij element** aktywuje pierwszy element ekranu, którego tekst lub opis zawiera podany fragment, niezależnie od wielkości liter. Pierwszeństwo ma element pasujący w całości. SandFly czeka, aż element się pojawi. Czas ustawisz w polu **Czas oczekiwania na element (sekundy)**. Domyślnie są to trzy sekundy.
- **Kliknij w podanym miejscu** stuka w ekran na współrzędnych X i Y, podanych w pikselach od lewego górnego rogu. Wymaga Androida 11 lub nowszego.
- **Dotknij i przytrzymaj element** przytrzymuje środek elementu przez **Czas przytrzymania (sekundy)**, domyślnie jedną sekundę. Potem unosi palec w miejscu albo przesuwa go w górę, w dół, w lewo lub w prawo o odległość podaną w dp. Przydaje się np. do przycisku nagrywania wiadomości głosowej, który działa w trakcie przytrzymania. Wymaga Androida 11 lub nowszego.
- **Przenieś fokus na element** szuka go tak jak **Kliknij element**. Przenosi na niego fokus ułatwień dostępu, więc SandFly go odczytuje.
- **Odczytaj element** szuka go tak jak **Kliknij element** i odczytuje jego nazwę bez przenoszenia fokusu.
- **Wykonaj długie kliknięcie lub przewiń** przytrzymuje albo przewija element do przodu lub do tyłu określoną liczbę razy. Jeśli sam element nie obsługuje czynności, SandFly korzysta z najbliższego elementu, który ją obsługuje, np. suwaka. Przewinięcie bez tekstu elementu przewija największą listę na ekranie.
- **Odczytaj tekst** odczytuje podany tekst.
- **Wpisz tekst** wpisuje tekst w miejscu kursora w polu edycji, na którym jest fokus. Po włączeniu **Zastąp tekst w polu** nowy tekst zastępuje zawartość pola.
- **Otwórz aplikację** uruchamia wybraną aplikację.
- **Więcej kroków** pokazuje kroki działające poza ekranem. Zobacz [](#macros-more-steps).
- **Czekaj** wstrzymuje wykonanie na podaną liczbę sekund, najwyżej 600.
- **Czekaj, aż** czeka na spełnienie warunku. W polu **Czas oczekiwania (sekundy)** domyślnie ustawiono dziesięć sekund. Gdy czas minie, makro się zatrzymuje.
- **Jeśli**, **W przeciwnym razie, jeśli**, **W przeciwnym razie** i **Koniec warunku** wykonują kroki pierwszej gałęzi z pasującym warunkiem. Zobacz [](#macros-if).
- **Powtarzaj**, **Powtarzaj, dopóki**, **Koniec powtarzania** i **Wyjdź z pętli** służą do ponownego wykonywania kroków. Zobacz [](#macros-repeat).
- **Zatrzymaj makro** kończy wykonywanie makra.

Jeśli krok się nie powiedzie, SandFly ogłasza zatrzymanie makra i podaje krok, który się nie udał. Jeśli zabraknie szukanego elementu, odczytuje też jego tekst. Gdy przekroczysz czas ustawiony w kroku **Czekaj, aż**, usłyszysz informację o przekroczeniu czasu i nazwę kroku.

#### Znajdź element {#macros-find-items}

Kroki i warunki, które szukają elementu, mają poniższe pola.

- **Tekst elementu** określa szukany tekst.
- **Wyrażenie regularne** interpretuje **Tekst elementu** jako wzorzec, który może pasować w dowolnym miejscu tekstu, niezależnie od wielkości liter. Powtarzana grupa nie może zawierać innego powtórzenia ani operatora `|`. Przykład niedozwolonego wzorca to `(a+)+`.
- **Szukaj w** określa, gdzie szukać. Dostępne są **Dowolny tekst** (domyślnie), **Tekst**, **Opis**, **Podpowiedź**, **Stan**, podpowiedź wyświetlana przy elemencie oraz **Identyfikator zasobu**. Identyfikator pasuje z nazwą pakietu lub bez niej, np. `send` pasuje do `com.example:id/send`.
- **Typ elementu** jest dostępny poza warunkiem **Fokus w elemencie lub jego zawartości**. Do wyboru są **Dowolny typ** (domyślnie), **Klikalny**, **Klikalny z przytrzymaniem**, **Edytowalny**, **Przewijalny**, **Regulowany**, **Z możliwością zaznaczenia**, **Zaznaczony**, **Wybrany**, **Nagłówek** i **Włączony**. Jeśli ustawisz **Klikalny**, krok **Kliknij element** wybierze przycisk spośród przycisku i nagłówka z tym samym tekstem. Przy ustawianiu warunku wybór typu pojawia się po wpisaniu tekstu.

Polecenie **Sprawdź element z fokusem** pokazuje wartości poszczególnych pól i typy elementu. Zobacz [](#macros-inspect).

#### Więcej kroków {#macros-more-steps}

- **Odtwórz dźwięk** odtwarza jeden z dźwięków SandFly.
- **Uruchom wibrację** wywołuje wibrację, a **Odtwórz ton** odtwarza ton.
- **Kopiuj tekst** zapisuje podany tekst w schowku.
- **Otwórz link** otwiera adres.
- **Otwórz ekran ustawień Androida** otwiera stronę ustawień Androida, np. Wi-Fi, Bluetooth lub baterii.
- **Udostępnij tekst** otwiera panel udostępniania z podanym tekstem.
- **Ustaw głośność** ustawia procentową głośność wybranego rodzaju dźwięku, np. multimediów, dzwonka lub ułatwień dostępu.
- **Ustaw tryb dzwonka** ustawia tryb zwykły, wibracje lub tryb cichy.
- **Ustawienie urządzenia** zmienia jedną z opcji w szybkich ustawieniach SandFly, takich jak Wi-Fi, latarka, Nie przeszkadzać lub jasność. Dla przełącznika wybierz **Włącz**, **Wyłącz** albo **Włącz lub wyłącz**. Dla suwaka podaj liczbę, wartość procentową (np. 50%) albo +1 lub -1. Przy wyborze z listy podaj opcję albo wybierz następną. Zobacz [](#display).
- **Klawisz multimediów** wykonuje **Odtwórz lub wstrzymaj**, **Odtwórz**, **Wstrzymaj**, **Następny utwór**, **Poprzedni utwór**, **Zatrzymaj**, **Przewiń do tyłu** lub **Przewiń do przodu**.
- **Ustaw minutnik** ustawia odliczanie w sekundach, do 86 400, z opcjonalną etykietą.
- **Ustaw budzik** ustawia godzinę, np. 08:30, z opcjonalną etykietą.
- **Zadzwoń** wybiera numer telefonu.
- **Przygotuj wiadomość SMS** przygotowuje wiadomość do podanego numeru.
- **Otwórz aparat** otwiera aparat do zrobienia zdjęcia lub nagrania filmu.
- **Naciśnij klawisze** wykonuje naciśnięcia takie jak `tab enter`. Wymaga Shizuku. Zobacz [](#updates).
- **Pokaż komunikat** wyświetla okno z tekstem i czeka na jego zamknięcie.
- **Ustaw wartość zmiennej** zapisuje podany tekst pod wybraną nazwą.
- **Pobierz tekst z adresu** zapisuje tekst z adresu HTTPS jako wartość.
- **Przetłumacz tekst** tłumaczy podany tekst na język urządzenia i zapisuje go jako wartość. Zobacz [](#translation).

Nazwy wartości mogą zawierać litery, cyfry, kropki, łączniki i znaki podkreślenia. Wartości są wspólne dla wszystkich makr i pozostają zapisane między uruchomieniami.

W treści kroków **Odczytaj tekst**, **Wpisz tekst** i wymienionych powyżej możesz użyć zapisanej wartości jako `{name}`. Każde uruchomienie ma też dostęp do gotowych wartości. `{item}` zawiera tekst elementu, na którym jest fokus, `{clipboard}` zawartość schowka, `{time}` godzinę, `{date}` datę, a `{battery}` poziom baterii. Wartości takie jak `{device.wifi}` podają stan wybranego ustawienia urządzenia.

### Warunki {#macros-conditions}

Warunek składa się z jednej lub kilku kontroli. Mają go kroki **Jeśli**, **W przeciwnym razie, jeśli**, **Powtarzaj, dopóki** i **Czekaj, aż** oraz ustawienia **Uruchamiaj tylko wtedy, gdy** i **Pokazuj w czynnościach tylko, gdy**. Wybierz krok lub ustawienie, aby otworzyć ekran warunku.

Na ekranie znajdziesz **Dodaj warunek** i listę **Warunki**. Dla **Czekaj, aż** dostępne jest też pole **Czas oczekiwania (sekundy)**. Warunek z pustą listą jest zawsze spełniony.

Możesz użyć następujących kontroli.

- **Element na ekranie** sprawdza, czy na ekranie jest element o podanym tekście i opcjonalnie wybranym typie. Zobacz [](#macros-find-items).
- **Element z fokusem** sprawdza, czy element, na którym jest fokus ułatwień dostępu, lub któryś z elementów w jego środku ma podany tekst, typ lub obie cechy. Tekst możesz zostawić pusty.
- **Fokus w elemencie lub jego zawartości** sprawdza, czy fokus jest na wskazanym elemencie lub w jego wnętrzu. Możesz np. wskazać odtwarzacz wideo według identyfikatora zasobu.
- **Aplikacja na pierwszym planie** sprawdza, czy wybrana aplikacja jest na pierwszym planie.
- **Klawiatura widoczna** sprawdza, czy widać klawiaturę ekranową.
- **Ekran zablokowany** sprawdza, czy widać ekran blokady.
- **Wartość zmiennej** sprawdza, czy zapisana wartość zawiera tekst z pola **Zawiera**. Jeśli pole zostawisz puste, sprawdza, czy wartość jest ustawiona. Po włączeniu **Wyrażenie regularne** wpisany tekst staje się wzorcem.

Jeśli na liście jest już kontrola, po wybraniu **Dodaj warunek** wybierasz **Połącz spójnikiem „i”** lub **Połącz spójnikiem „lub”**. Wybierz kontrolę na liście, aby zobaczyć poniższe opcje.

- **Edytuj** pozwala zmienić jej ustawienia.
- **Zmień na** odwraca kontrolę. Zmienia np. klawiaturę widoczną na **Klawiatura ukryta**, a zablokowany ekran na **Ekran odblokowany**. W ten sam sposób ustawisz **Brak elementu na ekranie** albo **Aplikacja nie na pierwszym planie**.
- **Połącz spójnikiem „i”** lub **Połącz spójnikiem „lub”** zmienia połączenie dla każdej kontroli poza pierwszą.
- **Przenieś w górę**, **Przenieś w dół** i **Usuń** pozwalają zmienić kolejność lub usunąć pozycję.

Spójnik „i” ma pierwszeństwo przed „lub”. Wystarczy, że będą spełnione wszystkie kontrole w jednej z grup oddzielonych przez „lub”. Lista może mieć najwyżej 20 kontroli.

#### Jeśli {#macros-if}

Wybierz **Dodaj krok** > **Jeśli** i ustaw warunek. SandFly wstawi **Jeśli** wraz z **Koniec warunku**. Kolejne dodawane kroki trafią między nie. **W przeciwnym razie, jeśli** dodaje osobną gałąź z warunkiem. **W przeciwnym razie** dodaje gałąź wykonywaną, gdy żaden wcześniejszy warunek nie pasuje. Jeśli dodasz jeden z tych kroków tuż po **Koniec warunku**, SandFly wstawi go przed krokiem kończącym.

#### Powtarzanie {#macros-repeat}

- **Powtarzaj** wykonuje kroki do **Koniec powtarzania** określoną liczbę razy, od 1 do 1000.
- **Powtarzaj, dopóki** wykonuje kroki do **Koniec powtarzania**, póki warunek jest spełniony. Przy jednym uruchomieniu makra może to zrobić do 1000 razy. Po osiągnięciu limitu SandFly odczyta informację o liczbie powtórzeń i krok, po czym zatrzyma makro.
- **Wyjdź z pętli** przechodzi do pierwszego kroku za **Koniec powtarzania** bieżącej pętli.

Po wstawieniu pętli przez **Dodaj krok** SandFly dodaje od razu **Koniec powtarzania**. Następne dodawane kroki trafią do wnętrza pętli.

Aby przewijać listę do znalezienia elementu, a potem go otworzyć, wykonaj te kroki.

1. Dodaj **Powtarzaj, dopóki**. Wybierz **Element na ekranie**, podaj tekst elementu i użyj **Zmień na**, aby ustawić **Brak elementu na ekranie**.
2. Dodaj **Wykonaj długie kliknięcie lub przewiń** z działaniem **Przewiń do przodu**. Zostaw tekst elementu pusty.
3. Za krokiem **Koniec powtarzania** dodaj **Kliknij element** z tekstem szukanego elementu.

### Nagraj makro {#macros-record}

SandFly może zapisać czynności wykonywane przez Ciebie w aplikacjach jako kroki makra.

1. Otwórz ekran, na którym makro ma się zaczynać.
2. Użyj polecenia **Nagraj makro** z grupy **Makra**. Najpierw przypisz je do gestu lub dodaj do menu SandFly. Zobacz [](#commands-assign) i [](#commands-edit-menu).
3. SandFly utworzy makro o nazwie „Nagranie” z datą i godziną oraz pokaże powiadomienie **Nagrywanie kroków makra**.
4. Wykonaj czynności, które chcesz nagrać.
5. Aby zakończyć, użyj ponownie **Nagraj makro**, użyj **Zatrzymaj makro** albo otwórz powiadomienie. SandFly otworzy ekran makra i pokaże nagrane kroki.
6. Wybierz **Zapisz**, aby dodać kroki, **Nagraj ponownie**, aby zacząć od nowa, albo **Anuluj**, aby usunąć makro.

Jeśli chcesz nagrać kroki do istniejącego makra, wybierz **Nagraj kroki** na jego ekranie. SandFly wróci do aplikacji otwartej przed ustawieniami albo otworzy aplikację przypisaną do makra, a następnie zacznie nagrywać. Kroki trafią do tego samego miejsca co przy użyciu **Dodaj krok**.

SandFly nagrywa naciśnięcia i przytrzymania elementów dotykiem lub za pomocą SandFly, przewijanie, wpisywany tekst i otwieranie aplikacji z ekranu głównego. Zapisuje też polecenia **Wstecz**, **Ekran główny**, **Przegląd SandFly**, **Powiadomienia**, **Szybkie ustawienia**, **Wszystkie aplikacje**, **Zaznacz wszystko**, **Kopiuj**, **Wytnij** i **Wklej**. Jeśli użyjesz powiadomień, szybkich ustawień, przeglądu lub listy wszystkich aplikacji tylko po to, żeby zakończyć nagrywanie, polecenie zostanie pominięte. SandFly pomija też tekst wpisywany w polach haseł i czynności na własnych ekranach.

Przy tworzeniu kroków SandFly uwzględnia przebieg nagrania.

- Seria przewinięć listy zakończona naciśnięciem elementu zamienia się w **Powtarzaj, dopóki**. Pętla przewija listę do chwili pojawienia się elementu. Po niej następuje naciśnięcie.
- Czynność lub sekwencja powtórzona kilka razy staje się krokiem **Powtarzaj**. Jeśli po ostatnim powtórzeniu zniknął pierwszy naciskany element, SandFly wstawia **Powtarzaj, dopóki** i sprawdza, czy ten element jeszcze jest na ekranie.
- Naciśnięcie zmieniające stan przełącznika lub pola wyboru wykonuje się, gdy ma ono stan przeciwny do ustawionego w nagraniu.
- Naciśnięcie na ekranie głównym, które otworzyło aplikację, staje się krokiem **Otwórz aplikację**.
- Jeśli pierwsza czynność odbyła się w aplikacji widocznej na początku, makro ją najpierw otworzy, gdy nie jest już na pierwszym planie.

Nagrane kroki szukają pełnego tekstu elementów za pomocą wyrażeń regularnych. Po nagraniu możesz je edytować, aby szukały tylko fragmentu tekstu.

### Uruchom makro {#macros-run}

- Użyj przypisanego gestu, skrótu klawiszowego, strzałki lub komórki brajlowskiej. Zobacz [](#commands-assign).
- Wybierz makro w menu. Dodasz je z grupy **Makra** do menu SandFly, własnego menu lub własnego sterowania czytaniem. Zobacz [](#commands).
- Wybierz makro w menu **Czynności** lub w pozycji **Czynności** sterowania czytaniem, jeśli włączono **Pokazuj w czynnościach**.
- Użyj skrótu na ekranie głównym dodanego przez **Dodaj do ekranu głównego**.
- Użyj polecenia głosowego. Powiedz nazwę makra albo „uruchom makro” i jego nazwę. Możesz też przypisać własny zwrot. Zobacz [](#voice-commands).
- Wybierz **Uruchom teraz** na ekranie makra.

Uruchomienie kolejnego makra zatrzymuje wykonywane makro. Możesz też użyć **Zatrzymaj makro**. Usunięcie makra usuwa jego przypisania do gestów, klawiszy i menu.

### Sprawdzaj elementy na potrzeby makr {#macros-inspect}

Polecenie **Sprawdź element z fokusem** z grupy **Ustawienia programisty** otwiera okno **Szczegóły elementu**. W oknie znajdziesz poniższe sekcje.

- **Teksty** pokazują tekst, opis, podpowiedź i stan elementu pod tymi samymi nazwami co pole **Szukaj w**.
- **Typy elementu** podają pasujące typy, np. **Klikalny** lub **Nagłówek**.
- **Elementy wewnątrz** pokazują teksty elementów zawartych w bieżącym elemencie.
- **Szczegóły dla skryptów** pokazują identyfikator zasobu, rolę, klasę i inne dane używane przez skrypty.
- **Aplikacja** pokazuje aplikację, aktywność i okno.

Aktywuj wiersz, aby skopiować jego wartość. **Kopiuj wszystko** kopiuje wszystkie dane w postaci tekstu. W kompilacjach ze skryptami **Dodaj regułę dla tego elementu** otwiera w kreatorze regułę dla tego elementu. Zobacz [](#scripts).

**Kopiuj szczegóły elementu** kopiuje te same dane bez otwierania okna i odczytuje potwierdzenie. **Kopiuj drzewo ekranu** kopiuje wszystkie elementy jako tekst, po jednym w wierszu. Każdy wiersz zawiera klasę, identyfikator, rolę, stany i teksty przydatne przy pisaniu skryptów.

Polecenia **Sprawdź element z fokusem**, **Kopiuj szczegóły elementu** i **Kopiuj drzewo ekranu** możesz dodać do menu SandFly. Domyślnie nie są tam dostępne. Aby sprawdzić element, przenieś na niego fokus, otwórz menu SandFly i wybierz **Sprawdź element z fokusem**. Okno pokaże element, na którym był fokus przed otwarciem menu.
