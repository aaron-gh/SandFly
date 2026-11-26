## Czytanie ekranu i nawigacja {#reading}

SandFly odczytuje element, na którym jest fokus. Możesz przenosić fokus dotykiem, przesunięciami palca, klawiaturą lub monitorem brajlowskim. Gdy fokus przejdzie na inny element, usłyszysz jego treść. W tym rozdziale znajdziesz sposoby poruszania się po ekranie, czytania fragmentami i bez przerw oraz przewijania. Na końcu opisano opcje z ekranu **Nawigacja**.

Gesty w tym rozdziale dotyczą domyślnego układu **Gesty SandFly**. Jeśli w **Gesty** > **Układ gestów** wybierzesz **Gesty VoiceOver**, niektóre gesty będą inne. Pełną listę domyślnych przypisań znajdziesz w [](#appendix-gestures). Sposób zmiany przypisań opisano w [](#gestures).

### Fokus SandFly {#reading-focus}

Fokus wskazuje, który element SandFly odczyta lub aktywuje. Na ekranie widać wokół niego ramkę. Jej wygląd możesz zmienić w [ustawieniach wyświetlania](#display).

Android osobno wskazuje element, do którego trafia tekst z klawiatury. Pole tekstowe lub element na stronie internetowej może przyjmować tekst także wtedy, gdy fokus SandFly jest gdzie indziej.

Elementem może być przycisk, wiersz tekstu, pozycja listy, obraz lub dowolna inna część ekranu. Podział zależy od aplikacji. Zazwyczaj SandFly czyta całą treść zagnieżdżoną w danym elemencie jako jedną pozycję. Żeby przechodzić osobno po fragmentach tekstu, włącz **Nawigacja** > **Zaznaczaj osobno tekst wewnątrz elementów**. Zobacz [](#reading-navigation-settings).

### Czytaj dotykiem {#reading-explore}

Przyłóż palec do ekranu i przesuń go. SandFly przenosi fokus na element pod palcem i go odczytuje. Gdy trafisz na puste miejsce, usłyszysz dźwięk.

Po dotknięciu ekranu SandFly chwilę czeka, żeby odróżnić dotyk od początku gestu. Domyślnie trwa to 0,15 sekundy. Czas ustawisz w **Dotyk** > **Opóźnienie zaznaczenia**. Zobacz [](#touch).

- Między ikonami na ekranie głównym SandFly nie czyta numeru strony, na przykład „Ekran główny 1 z 2”. Do tego komunikatu możesz dojść przesunięciem palca w lewo lub w prawo.
- Pływające okno, na przykład film odtwarzany nad ekranem głównym, jest jednym elementem. SandFly odczytuje nazwę aplikacji i informację, że to obraz w obrazie. Przyciski okna znajdziesz w czynnościach elementu. Możesz również dotknąć każdego z nich osobno.

### Przechodź między elementami {#reading-move}

- Przesuń palcem w prawo, żeby przejść do następnego elementu.
- Przesuń palcem w lewo, żeby wrócić do poprzedniego.

Kolejność odczytu ustala aplikacja. Zwykle SandFly przechodzi od lewego górnego rogu w stronę prawego dolnego. Jeśli następny element listy jest poza ekranem, SandFly przewinie listę. Zobacz [](#reading-scroll).

Do paska stanu i paska nawigacji możesz dojść dotykiem lub wybierając **Okna** w sterowaniu czytaniem. Z aplikacji nie przejdziesz do tych pasków zwykłym przesunięciem palca. Gest w prawo lub w lewo nie przeniesie Cię też z aplikacji na klawiaturę ekranową. Zobacz [](#typing).

Po przesunięciu za ostatni element domyślnie usłyszysz dźwięk końca, a fokus zostanie na tym elemencie. Powtórz gest, żeby przejść na początek. Zobacz [](#reading-wrap).

Do krańców ekranu prowadzą dwa polecenia.

- **Zaznacz pierwszy element na ekranie**
- **Zaznacz ostatni element na ekranie**

Domyślnie nie mają gestów. Możesz [przypisać gest lub skrót klawiszowy](#commands).

#### Przechodzenie między końcem a początkiem {#reading-wrap}

Przełącznik **Nawigacja** > **Zawijanie** jest domyślnie włączony. Pozwala przejść od ostatniego elementu do pierwszego i od pierwszego do ostatniego.

- Przy domyślnie włączonej opcji **Zatrzymuj na ostatnim elemencie przed zawinięciem** pierwsza próba przejścia za koniec lub przed początek zatrzyma fokus i odtworzy dźwięk. Dopiero następny gest przejdzie na drugi koniec. To ustawienie działa, gdy włączone jest **Zawijanie**.
- Gdy wyłączysz **Zawijanie**, przy każdej próbie przekroczenia końca lub początku usłyszysz ponownie dany element i dźwięk końca. Dotyczy to również przechodzenia po nagłówkach, linkach i elementach sterujących w aplikacji. Sama strona internetowa może nadal zawijać nawigację.

Możesz dodać **Zawijanie** na ekranie **Sterowanie czytaniem**, żeby przełączać je gestem w górę lub w dół. Zobacz [sterowanie czytaniem](#reading-controls).

Osobna opcja **Nawigacja** > **Zawijanie w sterowaniu czytaniem** dotyczy list wartości, na przykład w **Syntezator mowy**, **Urządzenie odtwarzające**, **Czynności**, **Adresy i numery** i **Tryb dźwięku**. Po wyłączeniu SandFly ponownie odczyta skrajną wartość i odegra dźwięk przy próbie przejścia dalej. Dopóki nie zmienisz tego ustawienia osobno, obowiązuje wartość z opcji **Zawijanie**.

### Aktywuj element {#reading-activate}

- Stuknij dwa razy w dowolnym miejscu. Polecenie **Aktywuj** zadziała na elemencie, na którym jest fokus.
- Aby wykonać długie naciśnięcie, stuknij dwa razy i po drugim stuknięciu przytrzymaj palec. To polecenie **Naciśnij i przytrzymaj**.

Jeśli wybierzesz czynność w opcji [**Czynności**](#reading-controls), dwukrotne stuknięcie wykona tę czynność.

Polecenie **Aktywuj dotknięciem** stuka w środek elementu. Przydaje się, gdy element reaguje tylko na dotknięcie ekranu. Domyślnie polecenie nie ma gestu. Wymaga Androida 11 lub nowszego. Na starszych urządzeniach aktywacja działa jak zwykle.

Aktywację jednym stuknięciem lub uniesieniem palca opisano w [rozdziale o dotyku](#touch).

### Sterowanie czytaniem {#reading-controls}

Opcja sterowania czytaniem określa, co zrobi przesunięcie jednym palcem w dół lub w górę. Wybierz na przykład **Słowa**, żeby przechodzić po słowach, **Nagłówki**, żeby przeskakiwać po sekcjach, albo **Szybkość mowy**, żeby zmieniać tempo.

- Aby wybrać następną opcję, przesuń palec w dół i z powrotem w górę. Możesz też przesunąć trzy palce w prawo lub w dół albo obrócić dwa palce zgodnie z ruchem wskazówek zegara.
- Aby wrócić do poprzedniej opcji, przesuń palec w górę i z powrotem w dół. Możesz też przesunąć trzy palce w lewo lub w górę albo obrócić dwa palce w przeciwną stronę.
- Jeśli chcesz przechodzić prosto do pierwszej opcji na używanej liście, przypisz gest lub skrót do **Pierwszy element sterowania czytaniem**. Domyślnie polecenie nie ma przypisania.
- Żeby użyć wybranej opcji, przesuń palec w dół, jeśli chcesz iść dalej, lub w górę, jeśli chcesz się cofnąć. Polecenia noszą nazwy **Przenieś element sterowania czytaniem w dół lub do przodu** i **Przenieś element sterowania czytaniem w górę lub do tyłu**.
- W opcjach, które zmieniają wartość, takich jak **Dostosuj suwak**, **Dostosuj wartość** i **Szybkość mowy**, kierunek przesunięcia określa zmianę, niezależnie od tego, które z dwóch poleceń uruchamia gest: przesunięcie w górę lub w prawo zwiększa wartość, a w dół lub w lewo ją zmniejsza. Dotyczy to prostych przesunięć od jednego do czterech palców. W języku zapisywanym od prawej do lewej przesunięcie w lewo zwiększa wartość, a w prawo ją zmniejsza. Przy innych gestach, skrótach klawiszowych i poleceniach monitora brajlowskiego **Przenieś element sterowania czytaniem w górę lub do tyłu** zwiększa wartość, a **Przenieś element sterowania czytaniem w dół lub do przodu** ją zmniejsza.

Po zmianie SandFly odczytuje nazwę wybranej opcji. Gesty w lewo i w prawo nadal przechodzą po zwykłych elementach. W tabeli możesz im nadać inne znaczenie przez **Wiersze i kolumny**.

Opcje takie jak **Czynności** i **Wiersze i kolumny** pojawiają się tylko wtedy, gdy pasują do danego elementu. Domyślnie SandFly sam je wybiera po przeniesieniu fokusu. Dotyczy to między innymi **Czynności**, **Dostosuj suwak**, **Dostosuj wartość** i **Korekta**. Po opuszczeniu elementu SandFly wróci do poprzedniej opcji. Aby samemu decydować o zmianie, wyłącz **Sterowanie czytaniem** > **Automatycznie zmieniaj opcję sterowania czytaniem**. W tabelach automatyczna zmiana na **Wiersze i kolumny** wymaga także **Nawigacja** > **W tabelach przełączaj na nawigację po tabeli**.

Na ekranie **Sterowanie czytaniem** wybierasz opcje i ich kolejność. Możesz tam przygotować osobne listy, dodać własne opcje i ustawić litery rozpoznawane przez polecenie **Napisz literę opcji sterowania czytaniem**. Możesz na przykład napisać na ekranie H, żeby wybrać **Nagłówki**. Szczegóły znajdziesz w [rozdziale o poleceniach](#commands).

Domyślnie na liście są **Znaki**, **Słowa**, **Wiersze**, **Akapity**, **Nagłówki**, **Linki**, **Kontrolki** oraz **Wyszukiwanie zawartości ekranu**. W tabeli pojawiają się też **Wiersze i kolumny**. Pozostałe pozycje możesz dodać w ustawieniach.

#### Czytaj tekst fragmentami {#reading-text}

Wybierz, jak duży fragment SandFly ma czytać po przesunięciu w górę lub w dół. Po dojściu do początku lub końca tekstu SandFly przejdzie do sąsiedniego elementu.

- **Znaki** pozwalają czytać po znaku.
- **Słowa** pozwalają czytać po słowie.
- **Wiersze** pozwalają czytać po wierszu.
- **Zdania** kończą się na znaku kończącym zdanie, w tym na znakach 。！？, albo na końcu wiersza. SandFly może przeczytać zdanie ze strony internetowej w kilku częściach, na przykład gdy w środku jest osobny link. Pozycję **Zdania** trzeba dodać do listy. Jest niedostępna w trybie zaznaczania.
- **Akapity** pozwalają czytać po akapicie.

Jeśli edytujesz pole tekstowe, gesty przenoszą kursor w obrębie pola. Zobacz [edycję tekstu](#typing). W ten sposób możesz też czytać niektóre teksty, po których sama aplikacja nie pozwala przechodzić fragmentami, na przykład wiadomości w Telegramie.

#### Przechodź po wybranym rodzaju elementów {#reading-kinds}

Przesuń palec w dół, żeby przejść do następnego elementu danego rodzaju, lub w górę, żeby wrócić do poprzedniego.

- **Domyślna nawigacja** przechodzi po kolei przez wszystkie elementy. Działa jak przesuwanie w prawo i w lewo. Początkowo nie ma tej opcji na liście.
- **Nagłówki**, **Linki** i **Kontrolki** służą do przechodzenia odpowiednio po nagłówkach, linkach oraz elementach takich jak przyciski i pola tekstowe. Działają w aplikacjach i na stronach internetowych.
- **Okna** prowadzą do innych okien, na przykład panelu głośności lub paska nawigacji. Tę opcję możesz dodać ręcznie.
- **Kontenery** pozwalają przejść do sąsiedniej grupy, na przykład listy, siatki, obszaru przewijania, paska narzędzi, kart, wyszukiwania, nawigacji lub panelu. Pasek wyszukiwania w pasku narzędzi liczy się razem z nim. Ruch wstecz prowadzi na początek poprzedniego kontenera, z wyjątkiem stron internetowych. Opcję trzeba dodać do listy.
- **Nawigacja pionowa** szuka elementu [poniżej lub powyżej](#reading-vertical).
- **Wyszukiwanie zawartości ekranu** pozwala przejść po wynikach [ostatniego wyszukiwania](#reading-search).
- W opcji **Przewijanie** ruch w dół przewija listę do przodu, a ruch w górę wstecz. Jeśli lista nie reaguje na polecenie przewijania, SandFly wysyła do niej przesunięcie. Domyślnie tej opcji nie ma na liście.

Na ekranie **Sterowanie czytaniem** w kategoriach **Nawigacja po elementach** i **Poziomy nagłówków** znajdziesz między innymi **Punkty orientacyjne**, **Przyciski**, **Pola edycji**, **Listy** i **Tabele**. Te opcje działają na [stronach internetowych](#web) i w aplikacjach.

Gdy włączysz **Sterowanie czytaniem** > **Łącza i elementy sterujące tylko w treści internetowej**, poza treścią WWW z listy znikną **Linki** i **Kontrolki**. Gesty i skróty prowadzące do następnego lub poprzedniego linku albo elementu sterującego nadal działają w aplikacjach. Opcja jest domyślnie wyłączona.

#### Przejdź po tekście lub elementach bez zmiany sterowania czytaniem {#reading-navigation-commands}

Wybierając polecenie dla [gestu](#gestures) lub skrótu, otwórz **Nawigacja według tekstu** albo **Nawigacja według elementu**. Możesz przechodzić do następnego lub poprzedniego znaku, słowa, wiersza, akapitu albo rodzaju elementu.

Do wyboru są między innymi nagłówki, także określonego poziomu, linki, linki odwiedzone i nieodwiedzone, elementy sterujące, punkty orientacyjne, przyciski, pola wyboru, przyciski opcji, pola edycji, pola kombi, elementy przyjmujące fokus, grafiki, listy, pozycje listy i tabele. Użycie polecenia zostawia wybraną opcję sterowania czytaniem.

### Czytanie ciągłe {#reading-continuous}

SandFly może odczytywać kolejne elementy i w razie potrzeby przewijać listę. Wybierz polecenie z kategorii **Czytanie**.

- **Czytaj od góry** zaczyna od pierwszego elementu na ekranie.
- **Czytaj od zaznaczonego elementu** zaczyna od elementu, na którym jest fokus. Domyślnie znajdziesz to polecenie w menu SandFly.
- **Czytaj od zaznaczonego elementu według sterowania czytaniem** zaczyna od elementu, na którym jest fokus. Potem czyta elementy wybranego rodzaju, na przykład wszystkie nagłówki albo wszystkie linki. Jeśli wybrana opcja sterowania czytaniem nie ogranicza nawigacji do jednego rodzaju elementów, SandFly odczyta je wszystkie.

Menu SandFly otworzysz stuknięciem trzema palcami. Polecenia czytania ciągłego domyślnie nie mają własnych gestów. Możesz [przypisać je do gestu lub skrótu](#commands).

Podczas odczytu możesz zmienić pozycję lub głośność.

- Przesuń palcem w prawo lub w lewo, żeby przejść do następnego lub poprzedniego elementu. Czytanie będzie kontynuowane od tego miejsca.
- Naciśnij klawisze głośności, żeby zmienić głośność mowy.
- Dotknij ekranu lub wykonaj inny gest, żeby zatrzymać odczyt.

Przy pierwszym uruchomieniu pojawi się komunikat **Jak działa czytanie ciągłe**. Wybierz **Zawsze pokazuj ten komunikat**, żeby widzieć go przy każdym uruchomieniu odczytu.

Ekran pozostaje włączony przez cały odczyt.

### Przewijanie {#reading-scroll}

Przy przechodzeniu w lewo lub w prawo SandFly przewija listę, jeśli następnego elementu nie widać. Możesz również przewijać samodzielnie.

- Przesuń palcem w prawo, a potem w lewo, żeby wykonać **Przewiń do przodu**.
- Przesuń palcem w lewo, a potem w prawo, żeby wykonać **Przewiń wstecz**. Na początku listy SandFly pociągnie ją w dół. W większości aplikacji odświeży to zawartość. Pociąganie wymaga Androida 13 lub nowszego.
- Przeciągnij dwoma palcami, tak jak jednym palcem przy wyłączonym SandFly, żeby aplikacja przewinęła zawartość bezpośrednio.

W domyślnym układzie gestów przesunięcie dwoma palcami trafia do aplikacji i przewija jej zawartość. Jeśli włączysz **Gesty** > **Obsługuj przesunięcia dwoma palcami w SandFly**, tak jak w układzie **Gesty VoiceOver**, przesunięcia będą uruchamiać polecenia. Domyślnie są to **Przewiń w górę**, **Przewiń w dół**, **Przewiń w lewo** i **Przewiń w prawo**. Przewijają w kierunku gestu.

Jeśli nic na ekranie nie daje się przewinąć w bok, **Przewiń w lewo** i **Przewiń w prawo** wykonują poziomy gest jednym palcem w obszarze, w którym znajduje się fokus. Może to przełączyć na przykład kartę folderu w Solid Explorer. Te cztery polecenia wymagają Androida 11 lub nowszego. Na zegarku są niedostępne.

W kategorii **Przewijanie** znajdziesz też polecenia bez domyślnych gestów.

- **Przewiń do góry** i **Przewiń w dół** przewijają listę, na której jest fokus, po jednej stronie aż do jej początku lub końca. Potem ustawiają fokus na pierwszym lub ostatnim elemencie. Dotknij ekranu, wykonaj gest lub naciśnij klawisz, żeby zatrzymać przewijanie. Polecenia są niedostępne na zegarkach.
- **Odśwież** przewija listę do góry i pociąga ją w dół. Polecenie wymaga Androida 13 lub nowszego.

Przy przewijaniu dwoma palcami usłyszysz dźwięk i poczujesz lekką wibrację przy każdej mijanej pozycji. Dźwięk jest coraz wyższy w miarę zbliżania się do końca listy. Samoczynne przewijanie do następnego elementu jest ciche. Zobacz [](#sound).

#### Przewijanie podczas przechodzenia między elementami {#reading-scroll-swipes}

Otwórz **Nawigacja**. Poniższe opcje dotyczą momentu, w którym następnego elementu jeszcze nie widać.

- **Przewijaj do następnego elementu z wyprzedzeniem** zaczyna przewijać, gdy SandFly czyta ostatni widoczny element listy. Fokus jest wciąż na tym elemencie, a przewijanie nie wydaje dźwięku. Po ruchu wstecz SandFly przewija w drugą stronę. Wyprzedzenie działa domyślnie. SandFly pomija je w polach tekstowych, w treści WWW i podczas czytania ciągłego. Zatrzymuje je po dotknięciu, naciśnięciu klawisza lub zmianie okna.
- **Odczytuj następny element podczas przewijania** przenosi fokus i zaczyna odczyt od razu, gdy kolejny element pojawi się na ekranie. Opcja jest domyślnie włączona. Wyłącz ją, żeby SandFly poczekał, aż lista zwolni.
- **Odczytuj elementy, zanim przewinięcie pokaże je na ekranie** pozwala przeczytać cały element od razu, nawet jeśli widać tylko jego część. Opcja jest domyślnie wyłączona. W niektórych aplikacjach odczyt może objąć też tekst pozostawiony przez aplikację tuż poza krawędzią elementu.

### Listy i tabele {#reading-lists-tables}

Po wejściu na listę lub jej opuszczeniu SandFly może odczytać informacje takie jak pozycja elementu. Zakres ustawisz w **Szczegółowość**. Sprawdź **Odczytywanie informacji o listach**, **Odczytywanie informacji o siatkach i tabelach** i **Zawsze odczytuj liczbę elementów z listy**. Więcej znajdziesz w [rozdziale o szczegółowości](#verbosity).

Opcja **Pozycje na liście** oraz odpowiednie gesty i skróty przechodzą po całych pozycjach listy. Pomijają przyciski wewnątrz pozycji, na przykład przycisk udostępniania wpisu. Gdy następnej pozycji jeszcze nie widać, SandFly przewinie listę. Po ostatniej pozycji przejdzie na następną listę. Jeśli fokus jest poza listą, szuka następnej pozycji listy na ekranie.

#### Poruszaj się po tabeli {#reading-tables}

W tabeli wybierz **Wiersze i kolumny**, aby przechodzić między komórkami.

- Przesuń w dół lub w górę, aby zmienić wiersz.
- Przesuń w prawo lub w lewo, aby zmienić kolumnę.

W tym trybie przesunięcia w bok zmieniają kolumnę także wtedy, gdy profil, aplikacja lub skrypt przypisuje im inne polecenie. Po wyjściu z tabeli SandFly wróci do poprzedniej opcji sterowania czytaniem.

Opcje **Wiersz** i **Kolumna** służą do ruchu tylko w jednym z tych kierunków. Użyj przesunięć w górę i w dół. Obie opcje trzeba dodać do listy. W kategorii poleceń **Tabele** możesz też wybrać **Następny wiersz**, **Poprzedni wiersz**, **Następna kolumna** lub **Poprzednia kolumna** i przypisać gesty.

Jeśli włączysz **Nawigacja** > **W tabelach przełączaj na nawigację po tabeli**, SandFly automatycznie wybierze **Wiersze i kolumny** po wejściu do tabeli. Ustawienie jest domyślnie wyłączone. Opcje obsługi tabel widać tylko w tabeli. SandFly traktuje też komórki Arkuszy Google jak tabelę. W czynnościach komórki znajdziesz między innymi **Edytuj komórkę** i **Wstaw formułę**.

W ustawieniach **Szczegółowość** możesz włączyć **Odczytuj numery wierszy i kolumn** oraz **Odczytuj nagłówki kolumn**. Zobacz [szczegółowość](#verbosity) i [skróty do odczytu nagłówków tabeli](#keyboards).

### Okna, kontenery i nawigacja pionowa {#reading-windows}

- Przesuń cztery palce w dół lub w górę, żeby przejść do następnego lub poprzedniego okna. Polecenia to **Następne okno** i **Poprzednie okno**.
- Przesuń cztery palce w prawo lub w lewo, żeby przejść do następnego lub poprzedniego kontenera. Polecenia **Następny kontener** i **Poprzedni kontener** zatrzymują się przy tych samych grupach co **Kontenery** w sterowaniu czytaniem.

Opcja **Nawigacja** > **Przechodź z paska stanu i paska nawigacji do innych okien** pozwala gestem wyjść z paska do aplikacji. Najpierw dotknij paska. Przesunięcie za jego pierwszy lub ostatni element przejdzie dalej. Przy domyślnie wyłączonej opcji gesty zawijają się w obrębie paska.

#### Nawigacja pionowa {#reading-vertical}

Wybierz **Nawigacja pionowa**. Przesunięcie w dół prowadzi do elementu pod tym, na którym jest fokus. Przesunięcie w górę prowadzi nad niego. W tym trybie SandFly pomija przyciski obok tytułu filmu czy nazwy pliku. Na klawiaturze numerycznej przechodzi do klawisza nad lub pod bieżącym, w tej samej kolumnie.

Na liście SandFly pozostaje w jej obrębie i w razie potrzeby przewija o stronę. Przy końcu listy lub poza nią szuka elementu wyżej lub niżej na ekranie. Jeśli niczego nie znajdzie, odtworzy dźwięk końca.

Polecenia **Przejdź do elementu poniżej** i **Przejdź do elementu powyżej** działają tak samo z gestu lub skrótu. **Przejdź do elementu po lewej** i **Przejdź do elementu po prawej** szukają najbliższego elementu we wskazanym kierunku. Domyślnie żadne z nich nie ma gestu.

### Fokus po zmianie ekranu {#reading-screen-changes}

Po otwarciu nowego okna lub powrocie do poprzedniego SandFly ustawia fokus w tym oknie. Może wrócić do ostatnio używanego elementu, przejść do pola z kursorem albo zacząć od pierwszego elementu. Dotyczy to też menu i okien dialogowych SandFly.

Jeśli otworzysz pozycję listy, na przykład folder, i wrócisz, SandFly ma dwie sekundy na odnalezienie tej pozycji. Gdy lista ją pokaże w tym czasie, fokus do niej wróci. Tak samo działa to w aplikacjach, które zmieniają listę bez otwierania osobnego okna.

Wyłącz **Nawigacja** > **Przenieś fokus na nowe okna**, jeśli chcesz usłyszeć przy otwarciu tylko nazwę okna. Pierwsze przesunięcie przejdzie wtedy na pierwszy element. SandFly pójdzie za fokusem klawiatury tylko wtedy, gdy przeniesiesz go klawiszem, na przykład przy użyciu Tab albo strzałki. Na telewizorze SandFly zawsze przenosi fokus do nowego okna.

Gdy element pod fokusem zniknie, na przykład po usunięciu wiersza listy, SandFly przejdzie do elementu, który zajął jego miejsce.

### Wyszukaj tekst na ekranie {#reading-search}

1. Przesuń palec w lewo, a potem w dół. To polecenie **Wyszukiwanie zawartości ekranu** z kategorii **SandFly**.
2. Wpisz szukane słowo. SandFly pokaże wyniki i odczyta ich liczbę.
3. Stuknij dwa razy na wyniku, żeby przenieść do niego fokus.

Przyciski **Następny ekran** i **Poprzedni ekran** przewijają zawartość pod oknem wyszukiwania. Użyjesz ich, żeby przeszukać dalszą część listy. Dostępne są też **Wyczyść wpisane dane** i **Zamknij wyszukiwanie**.

Po wyszukaniu wybierz **Wyszukiwanie zawartości ekranu** w sterowaniu czytaniem. W dół przejdziesz do następnego wyniku, a w górę do poprzedniego. Wyszukiwanie ekranu jest niedostępne na zegarku.

### Adresy i numery {#reading-addresses}

SandFly może znaleźć w odczytywanym elemencie adresy WWW i e-mail, numery telefonów oraz kody złożone z 4 do 8 cyfr. Działa to też w zwykłym tekście, na przykład w wiadomości z kodem jednorazowym.

1. Dodaj **Adresy i numery** na ekranie **Sterowanie czytaniem**. Domyślnie tej opcji nie ma na liście.
2. Ustaw fokus na elemencie i wybierz **Adresy i numery**.
3. Przesuwaj w górę lub w dół, aż usłyszysz szukany adres lub numer.
4. Stuknij dwa razy. Adres strony otworzy się w przeglądarce, a adres e-mail w nowej wiadomości. W przypadku numeru polecenie **Zadzwoń** otworzy numer w aplikacji telefonu. W przypadku kodu polecenie **Kopiuj** przeniesie go do schowka.

**Adresy i numery** są też w menu SandFly i w kategorii poleceń **Aktywacja**. Wejdź w **Sterowanie czytaniem** > **Uwzględniane adresy i numery**, żeby wybrać **Adresy internetowe**, **Adresy e-mail**, **Numery telefonów** i **Kody**.

Numery telefonów rozpoznaje mechanizm Androida, który wykrywa też daty, godziny, adresy uliczne i numery lotów. Cyfry w tych wyrażeniach, w nazwie użytkownika lub połączone z wyrazem, godziną czy kwotą nie trafiają do kodów. Inne liczby z 4 do 8 cyfr mogą się pojawić jako kody. Dotyczy to na przykład roku w tytule.

Sprawdzenie tekstu po geście lub otwarciu menu może zająć do sekundy. Na Androidzie 8 i wtedy, gdy mechanizm Androida nie odpowie w ciągu sekundy, lista zawiera tylko adresy WWW i e-mail.

### Skopiuj tekst z ekranu {#reading-copy-text}

Polecenie **Kopiuj tekst z ekranu** znajdziesz w kategorii **Edytowanie tekstu**. Domyślnie nie ma gestu. SandFly wyświetla tekst w oknie z polami wyboru.

Okno zawiera tekst z ekranu. Gdy na ekranie jest przewijana lista lub strona, przyciski **Przewiń w górę** nad listą i **Przewiń w dół** pod nią przewijają tę listę o stronę poza dotychczas pokazany tekst i dodają tekst tej strony. SandFly odczytuje „Dodano” i liczbę dodanych fragmentów. Zaznaczone fragmenty pozostają zaznaczone. Gdy lista jest na początku lub końcu, SandFly odczytuje „Nie ma więcej tekstu”. Po zamknięciu okna lista wraca do poprzedniego położenia.

1. Uruchom **Kopiuj tekst z ekranu**.
2. W sekcji **Szczegółowość** wybierz **Adresy i numery**, **Znaki**, **Słowa** lub **Akapity**. Początkowo wybrane są **Słowa**.
3. Zaznacz fragmenty. Jeśli chcesz zaznaczyć zakres, stuknij dwa razy i przytrzymaj przy pierwszym, a potem przy ostatnim fragmencie. Możesz też użyć czynności **Rozpocznij zaznaczanie** i **Zakończ zaznaczanie**. **Zaznacz wszystko** wybiera wszystkie fragmenty.
4. Wybierz **Kopiuj**. SandFly wstawia spacje między słowami, skleja znaki, rozdziela akapity pustym wierszem, a adresy i numery wpisuje w osobnych wierszach.

Gdy spod okna da się przewinąć listę lub stronę, użyj **Przewiń w górę** nad listą lub **Przewiń w dół** pod nią. SandFly przewinie o stronę i dołączy jej tekst. Usłyszysz komunikat o liczbie dodanych fragmentów. Zaznaczenia zostaną zachowane. Po dojściu do pierwszej lub ostatniej strony usłyszysz „Nie ma więcej tekstu”. Po zamknięciu okna przewinie listę z powrotem na miejsce, od którego zacząłeś.

Opcja **Adresy i numery** pojawia się, gdy na ekranie jest przynajmniej jeden adres lub numer. Wymienia wszystkie rodzaje niezależnie od ustawienia **Uwzględniane adresy i numery**. Możesz skopiować numer telefonu bez otwierania aplikacji do dzwonienia. Szukanie danych może wydłużyć otwieranie okna o sekundę.

Okno mieści do tysiąca fragmentów. Jeśli tekst jest dłuższy, SandFly odczyta komunikat „Lista skrócona do 1000 elementów”. Inne sposoby kopiowania opisano w [rozdziale o pisaniu](#typing).

### Ustawienia nawigacji {#reading-navigation-settings}

Na ekranie **Nawigacja** znajdziesz następujące ustawienia.

- **Zawijanie** pozwala przejść za ostatni element do pierwszego, a przed pierwszym do ostatniego. Opcja jest domyślnie włączona. Zobacz [](#reading-wrap).
- **Zatrzymuj na ostatnim elemencie przed zawinięciem** zatrzymuje fokus przy pierwszej próbie przejścia za koniec. Dopiero następny gest przenosi fokus na drugi koniec. Opcja jest domyślnie włączona.
- **Zawijanie w sterowaniu czytaniem** pozwala przejść z ostatniej wartości do pierwszej. Dopóki nie zmienisz tej opcji osobno, działa tak jak **Zawijanie**.
- **Przechodź z paska stanu i paska nawigacji do innych okien** pozwala przejść z paska do aplikacji przez przesunięcie palcem za skrajny element. Opcja jest domyślnie wyłączona. Zobacz [](#reading-windows).
- **Przenieś fokus na nowe okna** ustawia fokus w otwieranych oknach. Opcja jest domyślnie włączona. Zobacz [](#reading-screen-changes).
- **Odczytuj elementy, zanim przewinięcie pokaże je na ekranie** odczytuje częściowo widoczny element przed przewinięciem listy. Opcja jest domyślnie wyłączona. Zobacz [](#reading-scroll-swipes).
- **Przewijaj do następnego elementu z wyprzedzeniem** zaczyna przewijać listę podczas czytania ostatniego widocznego elementu. Opcja jest domyślnie włączona.
- **Odczytuj następny element podczas przewijania** zaczyna odczyt, gdy kolejny element pojawi się na ekranie. Opcja jest domyślnie włączona.
- **Zaznaczaj osobno tekst wewnątrz elementów** daje własny fokus poszczególnym fragmentom tekstu i ich stanom. Możesz do nich dojść dotykiem lub przesunięciem palca. Cały element zachowuje osobny fokus wtedy, gdy ma własny tekst, stan lub czynności. Gdy aktywujesz fragment tekstu, SandFly aktywuje zawierający go element. Opcja jest domyślnie wyłączona. Polecenie **Zaznaczaj osobno tekst wewnątrz elementów** w kategorii **Nawigacja** przełącza to ustawienie i odczytuje jego nowy stan.
- **W tabelach przełączaj na nawigację po tabeli** wybiera **Wiersze i kolumny**, gdy fokus trafia do tabeli. Opcja jest domyślnie wyłączona. Zobacz [](#reading-tables).
- **Inteligentny tryb przeglądania** i **Zapamiętaj tryb przeglądania w każdej aplikacji** opisano w [](#web).
- **Czytaj cytaty odpowiedzi w Telegramie razem z wiadomością** znajduje się w części **Aplikacje**. Zobacz [](#reading-telegram).

#### Telegram {#reading-telegram}

Te ustawienia dotyczą Telegrama oraz aplikacji opartych na nim, na przykład Nekograma.

- Opcja **Nawigacja** > **Czytaj cytaty odpowiedzi w Telegramie razem z wiadomością** jest domyślnie włączona. Po odczytaniu czasu wiadomości SandFly informuje, komu odpowiada nadawca, i czyta cytowany tekst. Cytat nie ma własnego fokusu. Żeby otworzyć cytowaną wiadomość, wybierz **Przejdź do cytowanej wiadomości** w czynnościach wiadomości. Jeśli wyłączysz tę opcję, cytat stanie się osobnym elementem, a czynność **Przejdź do cytowanej wiadomości** zniknie.
- Gdy czytasz wiadomości w kanale, SandFly pomija przycisk udostępniania przy przesunięciach i podczas eksploracji dotykiem. Znajdziesz go w czynnościach wiadomości pod nazwą nadaną przez Telegram, na przykład „Udostępnij”.
