## Makra {#macros}

Makro wykonuje po kolei zapisane czynności. Może użyć polecenia SandFly, kliknąć element, wprowadzić tekst albo poczekać na zmianę na ekranie. Może też powtarzać lub pomijać kroki zależnie od tego, co znajdzie.

Makra tworzysz na ekranie **Makra** w ustawieniach. Możesz dodać kroki samodzielnie lub nagrać je podczas korzystania z aplikacji. Potem uruchomisz makro gestem, skrótem klawiszowym, z menu, głosem albo z ekranu głównego. Makra nie działają na zegarku. Do zadań wymagających kodu służą [skrypty](#scripts).

### Tworzenie makra {#macros-add}

1. Otwórz **Makra** w ustawieniach SandFly.
2. Wybierz **Dodaj makro** i wpisz nazwę.
3. Użyj **Dodaj krok**, wybierz [rodzaj kroku](#macros-steps) i uzupełnij jego ustawienia. W ten sam sposób dodaj następne kroki.
4. Wybierz **Uruchom teraz**, żeby sprawdzić wynik.
5. Otwórz **Gesty i skrót klawiszowy**, żeby [przypisać makro](#commands-assign).

Jeśli łatwiej Ci wykonać czynności w aplikacji, możesz zacząć od [nagrania](#macros-record).

### Ekran Makra {#macros-screen}

**Dodaj makro** tworzy makro wspólne dla wszystkich aplikacji. **Dodaj aplikację** otwiera listę makr przypisanych do wybranej aplikacji. Na tej liście **Dodaj makro** tworzy makro tylko dla niej.

W części **Wszystkie aplikacje** znajdziesz makra wspólne, a w części **Aplikacje** wejdziesz do makr poszczególnych aplikacji. SandFly podaje ich liczbę przy nazwie.

Opcje **Importuj z pliku** i **Eksportuj do pliku** pozwalają przenosić [zestawy makr](#api-item-files). Gdy eksportujesz z listy danej aplikacji, zapisujesz jej makra. Makra trafiają również do [kopii ustawień](#profiles).

### Ekran pojedynczego makra {#macros-macro-screen}

- **Uruchom teraz** wykonuje makro, jeśli SandFly działa i są spełnione warunki.
- **Dodaj krok** i **Nagraj kroki** wstawiają kroki w miejscu podanym pod przyciskiem. Domyślnie następny krok pojawia się po ostatnio dodanym. Po dodaniu warunku lub powtórzenia następny krok trafi do jego wnętrza.
- **Uruchamiaj tylko, gdy** wymaga spełnienia [warunku](#macros-conditions). Domyślnie wybrane jest **Zawsze**. Jeśli warunek nie zachodzi, **Uruchom teraz** poda przyczynę.
- **Aplikacja** pozwala ograniczyć uruchamianie do aplikacji na pierwszym planie. Domyślna wartość to **Wszystkie aplikacje**.
- **Pokazuj w menu Czynności** dodaje makro po czynnościach aplikacji, zarówno do menu, jak i do opcji sterowania czytaniem **Czynności**. Początkowo jest wyłączone. Z **Pokazuj w menu Czynności tylko, gdy** możesz wybrać, przy których elementach pojawi się makro. Jeśli ustawisz warunek, makro liczy się jako czynność elementu na potrzeby opcji **Automatycznie zmieniaj opcję sterowania czytaniem**.
- **Kroki** pokazują kolejność wykonania. Kroki wewnątrz warunku lub powtórzenia są wcięte.
- **Gesty i skrót klawiszowy** pokazuje gesty, strzałki, znaki brajlowskie i skrót używane do uruchamiania.
- **Dodaj do ekranu głównego** tworzy skrót, o ile ekran główny go obsługuje. Zmiana nazwy makra zmienia nazwę skrótu. Usunięcie makra go wyłącza.
- Pozostałe opcje to **Zmień nazwę**, **Eksportuj do pliku** i **Usuń makro**.

Wybierz krok, żeby użyć **Edytuj**, **Dodaj krok po**, **Przenieś w górę**, **Przenieś w dół** lub **Usuń**. Te same polecenia są dostępne jako czynności na kroku. Usunięcie początku warunku lub powtórzenia usuwa należące do niego kroki sterujące, w tym koniec i ewentualne gałęzie. Zawartość zostaje. Makro mieści do 200 kroków.

Jeśli brakuje na przykład **Koniec warunku**, SandFly wskaże miejsce pod **Kroki**. Makro nie uruchomi się do czasu naprawienia układu.

### Kroki makra {#macros-steps}

| Krok | Działanie |
| --- | --- |
| **Uruchom polecenie** | Wykonuje działanie wybrane z kategorii SandFly. Za pomocą tego kroku nie można uruchomić innego makra. |
| **Kliknij element** | SandFly najpierw szuka pełnego tekstu. Jeśli go nie znajdzie, kliknie pierwszy element zawierający ten tekst. Czeka domyślnie 3 sekundy. Zmienisz to w opcji **Sekundy oczekiwania na element**. |
| **Kliknij w pozycji** | Stuka w punkt X, Y mierzony w pikselach od lewego górnego rogu. Wymaga Androida 11. |
| **Naciśnij i przytrzymaj element** | Przytrzymuje środek elementu. Ustaw **Czas przytrzymania w sekundach**, domyślnie sekundę. Przed uniesieniem można przesunąć dotyk o odległość podaną w dp. Przydaje się przy przyciskach działających podczas trzymania, na przykład przy nagrywaniu wiadomości. Wymaga Androida 11. |
| **Zaznacz element** | Wyszukuje element i przenosi na niego fokus dostępności. SandFly go odczyta. |
| **Odczytaj element** | Wyszukuje i czyta nazwę bez przenoszenia fokusu. |
| **Przytrzymaj lub przewiń element** | Przytrzymuje element albo przewija w wybranym kierunku i określoną liczbę razy. Gdy element sam tego nie obsługuje, SandFly szuka najbliższego, który może wykonać działanie. Bez tekstu elementu przewija największą listę na ekranie. |
| **Odczytaj tekst** | Czyta podaną treść. |
| **Wpisz tekst** | Wstawia tekst w miejscu kursora. **Zastąp tekst w polu** zmienia całą zawartość pola. |
| **Otwórz aplikację** | Otwiera wybraną aplikację. |
| **Więcej kroków** | Zawiera [pozostałe działania](#macros-more-steps). |
| **Czekaj** | Robi przerwę do 600 sekund. |
| **Czekaj, aż** | Czeka na warunek. Domyślnie **Sekundy oczekiwania** wynoszą 10. Po upływie czasu makro się zatrzyma. |
| **Jeśli**, **W przeciwnym razie, jeśli**, **W przeciwnym razie**, **Koniec warunku** | Wykonują pierwszą [pasującą gałąź](#macros-if). |
| **Powtarzaj**, **Powtarzaj, dopóki**, **Koniec powtarzania**, **Wyjdź z pętli** | Ustawiają [powtarzanie kroków](#macros-repeat). |
| **Zatrzymaj makro** | Kończy działanie makra. |

Jeśli krok zawiedzie, SandFly przerwie makro i odczyta, na którym kroku wystąpił problem. Poda też, jeśli zabrakło szukanego elementu lub upłynął czas oczekiwania.

#### Wyszukiwanie elementów {#macros-find-items}

W polu **Tekst elementu** wpisz nazwę lub jej fragment. Przy zwykłym wyszukiwaniu wielkość liter nie ma znaczenia. **Wyrażenie regularne** włącza wyszukiwanie według wzorca. Ono również pomija wielkość liter i może znaleźć dopasowanie w środku tekstu. Powtarzana grupa nie może zawierać ani powtórzenia, ani alternatywy `|`. Na przykład `(a+)+` jest niedozwolone.

**Dopasuj w** pozwala określić, gdzie szukać. Początkowo jest to **Dowolny tekst**. Możesz ograniczyć pole do tekstu, opisu, stanu, podpowiedzi albo identyfikatora zasobu. Identyfikator zadziała w całości lub z samą końcówką, na przykład `send` dla `com.example:id/send`.

**Typ elementu** zawęża wyszukiwanie na przykład do klikalnego, edytowalnego, przewijalnego lub regulowanego elementu, pola wyboru, zaznaczonej pozycji, nagłówka lub elementu włączonego. Domyślnie wybrany jest **Dowolny typ**. Wybór **Klikalny** przydaje się, jeśli ten sam napis widnieje na przycisku i nagłówku. Warunek **Zaznaczenie wewnątrz elementu** nie ma tego ustawienia. W pozostałych warunkach typ wybierasz po podaniu tekstu.

Jeśli trudno ustalić, czego szukać, użyj polecenia [Sprawdź element z fokusem](#macros-inspect).

#### Pozostałe kroki {#macros-more-steps}

- **Odtwórz dźwięk**, **Wibruj**, **Odtwórz ton**, **Kopiuj tekst** i **Udostępnij tekst**.
- **Otwórz link** otwiera adres. **Otwórz stronę ustawień** przechodzi na ekran systemowy, na przykład Wi-Fi, Bluetooth lub baterię.
- **Ustaw głośność** podaje procent dla wybranego kanału, na przykład multimediów, dzwonka albo dostępności. **Ustaw tryb dzwonka** przełącza tryb normalny, wibracje lub ciszę.
- **Ustawienie urządzenia** używa elementu [szybkich ustawień SandFly](#display). Dla przełącznika wybierz włączanie, wyłączanie lub zmianę stanu. Dla suwaka podaj liczbę, procent albo różnicę, na przykład `+1`. Przy liście możesz wskazać opcję lub wybrać następną.
- **Klawisz multimediów** pozwala między innymi odtwarzać, wstrzymać, zmienić utwór i przewijać.
- **Ustaw timer** przyjmuje czas do 86 400 sekund. **Ustaw alarm** przyjmuje godzinę, na przykład 08:30. W obu możesz dodać etykietę.
- **Zadzwoń**, **Wyślij wiadomość** i **Otwórz aparat** zaczynają odpowiednio połączenie, wiadomość na numer lub robienie zdjęcia bądź filmu.
- **Naciśnij klawisze** wykonuje sekwencję, na przykład `tab enter`. Wymaga [Shizuku](#updates).
- **Pokaż wiadomość** wyświetla tekst i czeka na zamknięcie okna.
- **Ustaw wartość** zapisuje tekst pod nazwą. **Pobierz z linku** odczytuje tekst z adresu HTTPS, a **Przetłumacz tekst** [tłumaczy go](#translation) na język urządzenia. Wynik obu kroków można zapisać pod nazwą.

W nazwach wartości używaj liter, cyfr, kropek, myślników i podkreśleń. Makra współdzielą wartości i zachowują je między uruchomieniami.

W tekście kroku, na przykład w **Odczytaj tekst** lub **Wpisz tekst**, użyj `{nazwa}`, żeby pobrać zapisaną wartość. Dostępne są również `{item}` (tekst elementu, na którym jest fokus), `{clipboard}`, `{time}`, `{date}`, `{battery}` oraz wartości ustawień, na przykład `{device.wifi}`.

### Warunki {#macros-conditions}

Warunek może należeć do kroku **Jeśli**, **W przeciwnym razie, jeśli**, **Powtarzaj, dopóki** lub **Czekaj, aż**. Mają go również opcje **Uruchamiaj tylko, gdy** i **Pokazuj w menu Czynności tylko, gdy**. Otwórz wybraną pozycję i użyj **Dodaj warunek**. Pusta lista warunków jest zawsze spełniona. Przy **Czekaj, aż** ustaw też czas oczekiwania.

Możesz sprawdzić, czy:

- **Element na ekranie** występuje i ma [szukany tekst lub typ](#macros-find-items).
- **Zaznaczony element** lub coś w jego wnętrzu ma określony tekst lub typ. Tekst można pominąć.
- **Zaznaczenie wewnątrz elementu** znajduje się na określonym elemencie lub w jego wnętrzu, na przykład w odtwarzaczu o wskazanym identyfikatorze.
- Działa wybrana aplikacja, widać klawiaturę albo ekran jest zablokowany. Wybierz **Aplikacja na pierwszym planie**, **Klawiatura widoczna** lub **Ekran zablokowany**.
- **Wartość** zawiera tekst albo pasuje do wyrażenia regularnego. Z pustym polem treści wystarczy, żeby wartość była ustawiona.

Kolejne warunki dodasz przez **Połącz spójnikiem „i”** lub **Połącz spójnikiem „lub”**. Każdy możesz edytować, odwrócić, przenieść lub usunąć. Po pierwszym warunku możesz też zmienić spójnik przy następnych. „I” wiąże mocniej niż „lub”. SandFly uzna cały warunek za spełniony, jeśli zgadza się przynajmniej jedna grupa połączona słowem „i”. Maksymalnie możesz dodać 20 warunków.

#### Jeśli {#macros-if}

Po dodaniu **Jeśli** SandFly prosi o warunek i wstawia **Koniec warunku**. Dodaj działania pomiędzy nimi. Aby sprawdzić inną możliwość, dodaj **W przeciwnym razie, jeśli**. **W przeciwnym razie** obejmie pozostałe przypadki. Gdy dodasz jedną z tych gałęzi tuż po końcu warunku, SandFly wstawi ją przed końcem.

#### Powtarzanie i przewijanie aż do wyniku {#macros-repeat}

**Powtarzaj** wykona kroki od 1 do 1000 razy. **Powtarzaj, dopóki** sprawdza warunek i powtarza kroki, póki jest spełniony. Po 1000 obiegach zatrzyma całe makro. **Wyjdź z pętli** przechodzi od razu do kroków po najbliższym **Koniec powtarzania**.

Jeśli makro ma przewinąć listę, aż zobaczy element, i go otworzyć:

1. Dodaj **Powtarzaj, dopóki**. Wybierz **Element na ekranie** i wpisz charakterystyczny fragment nazwy. Otwórz dodany warunek i użyj **Zmień na**, żeby wskazywał brak tego elementu na ekranie.
2. W środku powtórzenia dodaj **Przytrzymaj lub przewiń element** > **Przewiń do przodu**. Zostaw tekst pusty, żeby przewijać największą listę. Jeśli na ekranie jest kilka list, możesz wskazać właściwą.
3. Dodaj **Kliknij element** za krokiem **Koniec powtarzania**. Wpisz ten sam fragment nazwy. Sprawdź, czy regularne wyrażenie i typ elementu są dobrane tak samo w obu miejscach.

### Nagrywanie makra {#macros-record}

1. Otwórz ekran, od którego ma zaczynać się makro.
2. Użyj **Nagraj makro** z kategorii **Makra**. Najpierw możesz [przypisać gest](#commands-assign) albo [dodać polecenie do menu](#commands-edit-menu).
3. Wykonaj czynności. SandFly nada nagraniu nazwę z datą i godziną oraz wyświetli powiadomienie o nagrywaniu.
4. Użyj ponownie **Nagraj makro**, wybierz **Zatrzymaj makro** albo otwórz powiadomienie.
5. Przejrzyj zarejestrowane kroki. Użyj **Zapisz**, **Nagraj ponownie** lub **Anuluj**. Anulowanie usuwa tworzone makro.

**Nagraj kroki** w istniejącym makrze wraca do ostatniej aplikacji lub otwiera aplikację makra. Wstawia nagranie w tym samym miejscu co **Dodaj krok**.

SandFly nagrywa dotknięcia, przytrzymania, przewijanie, wpisywanie tekstu, otwieranie aplikacji z ekranu głównego i podstawowe polecenia, takie jak **Wstecz**, **Ekran główny**, **Powiadomienia**, **Szybkie ustawienia**, zaznaczanie, kopiowanie, wycinanie i wklejanie. Pomija ekrany SandFly i pola haseł. Polecenia otwierające powiadomienia, szybkie ustawienia, ostatnie aplikacje lub listę aplikacji mogą zostać pominięte na końcu, jeśli posłużyły tylko do zakończenia rejestracji.

Nagranie potrafi zamienić przewijanie zakończone kliknięciem na pętlę szukającą elementu. Powtarzające się działania stają się powtórzeniem. Zmiana przełącznika lub pola wyboru trafia do makra z warunkiem jego stanu. Otwieranie aplikacji z ekranu głównego zamienia się w **Otwórz aplikację**. SandFly może także dodać otwarcie aplikacji, od której zaczęło się nagranie.

Nagrane kroki szukają pełnego tekstu za pomocą wyrażenia regularnego. Jeśli nazwa może się zmieniać, edytuj krok i dopasuj tylko jej charakterystyczny fragment.

### Uruchamianie makra {#macros-run}

Makro możesz uruchomić z jego ekranu, [skrótem](#commands-assign), z [menu lub opcji sterowania czytaniem](#commands), z **Czynności** albo ze skrótu na ekranie głównym. Przy [poleceniu głosowym](#voice-commands) powiedz nazwę lub zacznij od „uruchom makro”. Możesz też przypisać własną frazę.

Rozpoczęcie nowego makra zatrzymuje poprzednie. W dowolnym momencie możesz użyć **Zatrzymaj makro**. Usunięcie makra usuwa jego powiązania z gestami, klawiszami i menu.

### Sprawdzanie elementu przed dodaniem kroku {#macros-inspect}

Przenieś fokus na interesujący Cię element. Wybierz **Sprawdź element z fokusem** z kategorii **Ustawienia programisty**. W oknie **Szczegóły elementu** znajdziesz jego teksty, typy, zawartość, dane dla skryptów oraz aplikację, aktywność i okno. Wybierz wiersz, żeby skopiować wartość, albo użyj **Kopiuj wszystko**. W wydaniach ze [skryptami](#scripts) dostępne jest też **Dodaj regułę dla tego elementu**.

**Kopiuj szczegóły elementu** robi to samo bez otwierania okna. **Kopiuj drzewo ekranu** kopiuje wszystkie elementy w osobnych wierszach wraz z klasą, identyfikatorem, stanem i tekstami. Te trzy polecenia są początkowo ukryte w menu SandFly. Jeśli je dodasz, po otwarciu menu sprawdzą element, na którym był fokus przed jego wyświetleniem.
