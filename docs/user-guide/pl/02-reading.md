## Czytanie ekranu i nawigacja {#reading}

SandFly odczytuje element, na którym jest fokus. Możesz przenosić fokus dotykiem, gestami, klawiaturą lub monitorem brajlowskim. Poniższe gesty dotyczą układu **Gesty SandFly**. Jeśli korzystasz z układu **Gesty VoiceOver**, sprawdź [domyślne przypisania](#appendix-gestures) i [ustawienia gestów](#gestures).

### Fokus czytnika {#reading-focus}

Fokus wskazuje, który element SandFly odczyta lub aktywuje. Na ekranie widać wokół niego ramkę. Jej wygląd możesz zmienić w [ustawieniach wyświetlania](#display).

Android osobno wskazuje miejsce, do którego trafia tekst z klawiatury. Możesz nadal pisać w polu tekstowym, gdy SandFly odczytuje element w innej części ekranu. Podobnie działają niektóre strony internetowe.

Elementem może być przycisk, tekst, wiersz listy lub obraz. Podział zależy od aplikacji. SandFly zazwyczaj czyta zawartość elementu razem. Aby przechodzić osobno do znajdujących się w nim fragmentów tekstu, włącz **Nawigacja** > **Zaznaczaj osobno tekst wewnątrz elementów**. Szczegóły opisano w [ustawieniach nawigacji](#reading-navigation-settings).

### Czytaj dotykiem {#reading-explore}

Przyłóż palec do ekranu i przesuwaj go powoli. SandFly przenosi fokus na element pod palcem i go odczytuje. Gdy trafisz na puste miejsce, usłyszysz dźwięk.

Po dotknięciu SandFly czeka krótko, żeby odróżnić eksplorację od gestu. Domyślnie trwa to 0,15 sekundy. Czas ustawisz w **Dotyk** > **Opóźnienie zaznaczenia**. Zobacz [ustawienia dotyku](#touch).

Między ikonami na ekranie głównym SandFly nie czyta numeru strony, na przykład „Ekran główny 1 z 2”. Do tego komunikatu możesz dojść gestem w lewo lub w prawo.

Film w małym oknie nad inną aplikacją to jeden element. SandFly odczytuje nazwę aplikacji i informację o obrazie w obrazie. Przyciski okna znajdziesz w czynnościach elementu. Możesz też dotknąć ich bezpośrednio.

### Przechodź między elementami {#reading-move}

- Przesuń palcem w prawo, żeby przejść do następnego elementu.
- Przesuń palcem w lewo, żeby wrócić do poprzedniego.

Aplikacja ustala kolejność. Zwykle SandFly przechodzi od lewego górnego rogu w stronę prawego dolnego. Jeśli następny element listy jest poza ekranem, [SandFly przewinie listę](#reading-scroll).

Do paska stanu i paska nawigacji możesz dojść dotykiem lub opcją sterowania czytaniem **Okna**. Z aplikacji nie przejdziesz do tych pasków zwykłym przesunięciem palcem. W podobny sposób z aplikacji nie wejdziesz na [klawiaturę ekranową](#typing).

Po przesunięciu za ostatni element domyślnie usłyszysz dźwięk końca. Powtórz gest, żeby przejść na początek. Zobacz [ustawienia zawijania](#reading-wrap).

Polecenia **Zaznacz pierwszy element na ekranie** i **Zaznacz ostatni element na ekranie** przenoszą fokus od razu na początek lub koniec. Domyślnie nie mają gestów. Możesz [przypisać gest lub skrót klawiszowy](#commands).

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

Po zmianie SandFly odczytuje nazwę wybranej opcji. Gesty w lewo i w prawo nadal przechodzą po zwykłych elementach. W tabeli możesz im nadać inne znaczenie przez **Wiersze i kolumny**.

Opcje takie jak **Czynności** i **Wiersze i kolumny** pojawiają się tylko wtedy, gdy pasują do danego elementu. Domyślnie SandFly sam je wybiera po przeniesieniu fokusu. Dotyczy to między innymi **Czynności**, **Dostosuj suwak**, **Dostosuj wartość** i **Korekta**. Po opuszczeniu elementu SandFly wróci do poprzedniej opcji. Aby samemu decydować o zmianie, wyłącz **Sterowanie czytaniem** > **Automatycznie zmieniaj opcję sterowania czytaniem**. W tabelach automatyczna zmiana na **Wiersze i kolumny** wymaga także **Nawigacja** > **W tabelach przełączaj na nawigację po tabeli**.

Na ekranie **Sterowanie czytaniem** wybierasz opcje i ich kolejność. Możesz tam przygotować osobne listy, dodać własne opcje i ustawić litery rozpoznawane przez polecenie **Napisz literę opcji sterowania czytaniem**. Możesz na przykład napisać na ekranie H, żeby wybrać **Nagłówki**. Szczegóły znajdziesz w [rozdziale o poleceniach](#commands).

Domyślnie na liście są **Znaki**, **Słowa**, **Wiersze**, **Akapity**, **Nagłówki**, **Linki**, elementy sterujące oraz **Wyszukiwanie zawartości ekranu**. W tabeli pojawiają się też **Wiersze i kolumny**. Pozostałe pozycje możesz dodać w ustawieniach.

#### Czytaj tekst fragmentami {#reading-text}

Wybierz, jak duży fragment SandFly ma czytać po przesunięciu w górę lub w dół. Po dojściu do początku lub końca tekstu czytnik przejdzie do sąsiedniego elementu.

- **Znaki** pozwalają czytać po znaku.
- **Słowa** pozwalają czytać po słowie.
- **Wiersze** pozwalają czytać po wierszu.
- **Zdania** kończą się na znaku kończącym zdanie, w tym na znakach 。！？, albo na końcu wiersza. SandFly może przeczytać zdanie ze strony internetowej w kilku częściach, na przykład gdy w środku jest osobny link. Pozycję **Zdania** trzeba dodać do listy. Jest niedostępna w trybie zaznaczania.
- **Akapity** pozwalają czytać po akapicie.

Jeśli edytujesz pole tekstowe, gesty przenoszą kursor w obrębie pola. Zobacz [edycję tekstu](#typing). W ten sposób możesz też czytać niektóre teksty, po których sama aplikacja nie pozwala przechodzić fragmentami, na przykład wiadomości w Telegramie.

#### Przechodź po wybranym rodzaju elementów {#reading-kinds}

Przesuń palec w dół, żeby przejść do następnego elementu danego rodzaju, lub w górę, żeby wrócić do poprzedniego.

- **Domyślna nawigacja** przechodzi po kolei przez wszystkie elementy. Działa jak przesuwanie w prawo i w lewo. Początkowo nie ma tej opcji na liście.
- **Nagłówki**, **Linki** i elementy sterujące służą do przechodzenia odpowiednio po nagłówkach, linkach oraz elementach takich jak przyciski i pola tekstowe. Działają w aplikacjach i na stronach internetowych.
- **Okna** prowadzą do innych okien, na przykład panelu głośności lub paska nawigacji. Tę opcję możesz dodać ręcznie.
- **Kontenery** pozwalają przejść do sąsiedniej grupy, na przykład listy, siatki, obszaru przewijania, paska narzędzi, kart, wyszukiwania, nawigacji lub panelu. Pasek wyszukiwania w pasku narzędzi liczy się razem z nim. Ruch wstecz prowadzi na początek poprzedniego kontenera, z wyjątkiem stron internetowych. Opcję trzeba dodać do listy.
- **Nawigacja pionowa** szuka elementu [poniżej lub powyżej](#reading-vertical).
- **Wyszukiwanie zawartości ekranu** pozwala przejść po wynikach [ostatniego wyszukiwania](#reading-search).
- W opcji **Przewijanie** ruch w dół przewija listę do przodu, a ruch w górę wstecz. Jeśli lista nie reaguje na polecenie przewijania, SandFly wysyła do niej przesunięcie. Domyślnie tej opcji nie ma na liście.

Na ekranie **Sterowanie czytaniem** w kategoriach **Nawigacja po elementach** i **Poziomy nagłówków** znajdziesz między innymi **Punkty orientacyjne**, **Przyciski**, **Pola edycji**, **Listy** i **Tabele**. Te opcje działają na [stronach internetowych](#web) i w aplikacjach.

Gdy włączysz **Sterowanie czytaniem** > **Łącza i elementy sterujące tylko w treści internetowej**, poza treścią WWW z listy znikną opcje chodzenia po linkach i elementach sterujących. Gesty i skróty prowadzące do następnego lub poprzedniego linku albo elementu sterującego nadal działają w aplikacjach. Opcja jest domyślnie wyłączona.

#### Przejdź po tekście lub elementach bez zmiany sterowania czytaniem {#reading-navigation-commands}

Wybierając polecenie dla [gestu](#gestures) lub skrótu, otwórz **Nawigacja według tekstu** albo **Nawigacja według elementu**. Możesz przechodzić do następnego lub poprzedniego znaku, słowa, wiersza, akapitu albo rodzaju elementu.

Do wyboru są między innymi nagłówki, także określonego poziomu, linki, linki odwiedzone i nieodwiedzone, elementy sterujące, punkty orientacyjne, przyciski, pola wyboru, przyciski opcji, pola edycji, pola kombi, elementy przyjmujące fokus, grafiki, listy, pozycje listy i tabele. Użycie polecenia zostawia wybraną opcję sterowania czytaniem.

### Czytanie ciągłe {#reading-continuous}

SandFly może czytać kolejne elementy i przewijać listy podczas odczytu. Wybierz polecenie z kategorii **Czytanie**.

- **Czytaj od góry** zaczyna od pierwszego elementu ekranu.
- **Czytaj od zaznaczonego elementu** zaczyna od miejsca, w którym jest fokus. Domyślnie znajdziesz to polecenie w menu SandFly.
- **Czytaj od zaznaczonego elementu według sterowania czytaniem** zaczyna od elementu, na którym jest fokus. Potem czyta tylko wybrane elementy, na przykład nagłówki albo linki. Jeśli wybrana opcja nie służy do przechodzenia po jednym rodzaju elementów, SandFly odczyta je wszystkie.

Menu SandFly otworzysz stuknięciem trzema palcami. Polecenia czytania ciągłego domyślnie nie mają gestów. Możesz je [przypisać do gestu lub skrótu](#commands).

W trakcie odczytu możesz przesunąć palcem w lewo lub w prawo. SandFly ruszy dalej od poprzedniego lub następnego elementu. Klawiszami głośności zmienisz głośność mowy. Żeby zatrzymać odczyt, dotknij ekranu lub użyj innego gestu. Ekran pozostaje włączony przez cały odczyt.

Przy pierwszym odczycie SandFly pokazuje **Jak działa czytanie ciągłe**. Wybierz **Zawsze pokazuj ten komunikat**, aby widzieć go za każdym razem.

### Przewijanie {#reading-scroll}

Gdy przesuwasz fokus w prawo lub w lewo, SandFly przewija listę po dotarciu do elementu poza ekranem. Możesz też przewinąć listę samodzielnie.

- Przesuń palec w prawo, a potem w lewo, żeby wykonać **Przewiń do przodu**.
- Przesuń palec w lewo, a potem w prawo, żeby wykonać **Przewiń wstecz**. Na początku listy czytnik pociągnie ją w dół. W wielu aplikacjach to odświeża zawartość.
- Przesuń po ekranie dwa palce, żeby aplikacja przewinęła zawartość bezpośrednio.

Jeśli włączysz **Gesty** > **Obsługuj przesunięcia dwoma palcami w SandFly**, ruchy przejmie czytnik. To ustawienie układu VoiceOver. Domyślne polecenia to **Przewiń w górę**, **Przewiń w dół**, **Przewiń w lewo** i **Przewiń w prawo**. Przewijają w kierunku gestu. Gdy nic na ekranie nie daje się przewinąć w bok, SandFly wysyła poziome przesunięcie jednym palcem do obszaru stron wokół elementu, na którym jest fokus. Tak może przełączyć na przykład kartę folderu w Solid Explorer. Cztery polecenia wymagają Androida 11 lub nowszego i są niedostępne na zegarkach.

W kategorii **Przewijanie** możesz też przypisać gest do przewijania na sam początek lub koniec listy. SandFly przechodzi stronami i ustawia fokus na pierwszym lub ostatnim elemencie. Dotknięcie, gest lub klawisz zatrzymują przewijanie. To rozwiązanie jest niedostępne na zegarkach. Polecenie **Odśwież** przewija listę do początku i pociąga ją w dół. Wymaga Androida 11 lub nowszego. Te polecenia domyślnie nie mają gestów.

Przy przewijaniu dwoma palcami słyszysz dźwięk i czujesz lekką wibrację przy mijaniu elementów. Dźwięk jest coraz wyższy w miarę zbliżania się do końca listy. Samoczynne przewijanie do następnego elementu jest ciche. Szczegóły znajdziesz w [ustawieniach dźwięków i wibracji](#sound).

#### Przewijanie podczas przechodzenia między elementami {#reading-scroll-swipes}

Otwórz **Nawigacja**. Poniższe opcje dotyczą momentu, w którym następnego elementu jeszcze nie widać.

- **Przewijaj do następnego elementu z wyprzedzeniem** zaczyna przewijać, gdy SandFly czyta ostatni widoczny element listy. Fokus jest wciąż na tym elemencie, a przewijanie nie wydaje dźwięku. Po ruchu wstecz czytnik przewija w drugą stronę. Wyprzedzenie działa domyślnie. SandFly pomija je w polach tekstowych, w treści WWW i podczas czytania ciągłego. Zatrzymuje je po dotknięciu, naciśnięciu klawisza lub zmianie okna.
- **Odczytuj następny element podczas przewijania** przenosi fokus i zaczyna odczyt od razu, gdy kolejny element pojawi się na ekranie. Opcja jest domyślnie włączona. Wyłącz ją, jeśli czytnik ma poczekać, aż lista zwolni.
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
4. Stuknij dwa razy, żeby otworzyć stronę, utworzyć wiadomość e-mail, otworzyć numer w aplikacji telefonu albo skopiować kod.

**Adresy i numery** są też w menu SandFly i w kategorii poleceń **Aktywacja**. Wejdź w **Sterowanie czytaniem** > **Uwzględniane adresy i numery**, żeby wybrać **Adresy internetowe**, **Adresy e-mail**, **Numery telefonów** i **Kody**.

Numery telefonów rozpoznaje mechanizm Androida, który wykrywa też daty, godziny, adresy uliczne i numery lotów. Cyfry w tych wyrażeniach, w nazwie użytkownika lub połączone z wyrazem, godziną czy kwotą nie trafiają do kodów. Inne liczby z 4 do 8 cyfr mogą się pojawić jako kody. Dotyczy to na przykład roku w tytule.

Sprawdzenie tekstu po geście lub otwarciu menu może zająć do sekundy. Na Androidzie 8 i wtedy, gdy mechanizm Androida nie odpowie w ciągu sekundy, lista zawiera tylko adresy WWW i e-mail.

### Skopiuj tekst z ekranu {#reading-copy-text}

Polecenie **Kopiuj tekst z ekranu** znajdziesz w kategorii **Edytowanie tekstu**. Domyślnie nie ma gestu. SandFly wyświetla tekst w oknie z polami wyboru.

1. Uruchom **Kopiuj tekst z ekranu**.
2. W sekcji **Szczegółowość** wybierz **Adresy i numery**, **Znaki**, **Słowa** lub **Akapity**. Początkowo wybrane są **Słowa**.
3. Zaznacz fragmenty. Jeśli chcesz zaznaczyć zakres, stuknij dwa razy i przytrzymaj przy pierwszym, a potem przy ostatnim fragmencie. Możesz też użyć czynności rozpoczęcia zaznaczania i **Zakończ zaznaczanie**. **Zaznacz wszystko** wybiera wszystkie fragmenty.
4. Wybierz **Kopiuj**. SandFly wstawia spacje między słowami, skleja znaki, rozdziela akapity pustym wierszem, a adresy i numery wpisuje w osobnych wierszach.

Gdy spod okna da się przewinąć listę lub stronę, użyj **Przewiń w górę** nad listą lub **Przewiń w dół** pod nią. SandFly przewinie o stronę i dołączy jej tekst. Odczyta liczbę dodanych fragmentów, zachowa zaznaczenia, a przy końcu lub początku powie, że nie ma dalszego tekstu. Po zamknięciu okna przewinie listę z powrotem na miejsce, od którego zacząłeś.

Opcja **Adresy i numery** pojawia się, gdy na ekranie jest przynajmniej jeden adres lub numer. Wymienia wszystkie rodzaje niezależnie od ustawienia **Uwzględniane adresy i numery**. Możesz skopiować numer telefonu bez otwierania aplikacji do dzwonienia. Szukanie danych może wydłużyć otwieranie okna o sekundę.

Okno mieści do tysiąca fragmentów. Jeśli tekst jest dłuższy, SandFly poda, na ilu fragmentach uciął listę. Inne sposoby kopiowania opisano w [rozdziale o pisaniu](#typing).

### Ustawienia nawigacji {#reading-navigation-settings}

- **Zawijanie**, **Zatrzymuj na ostatnim elemencie przed zawinięciem** i **Zawijanie w sterowaniu czytaniem** opisano w [ustawieniach zawijania](#reading-wrap).
- **Przechodź z paska stanu i paska nawigacji do innych okien** dotyczy [wychodzenia z paska do aplikacji](#reading-windows).
- **Przenieś fokus na nowe okna** opisano w [sekcji o zmianach ekranu](#reading-screen-changes).
- **Odczytuj elementy, zanim przewinięcie pokaże je na ekranie**, **Przewijaj do następnego elementu z wyprzedzeniem** i **Odczytuj następny element podczas przewijania** dotyczą [przewijania przy zmianie fokusu](#reading-scroll-swipes).
- **Zaznaczaj osobno tekst wewnątrz elementów** daje osobny fokus poszczególnym fragmentom tekstu i ich stanom. Możesz do nich dojść dotykiem lub przesunięciem. Cały element zachowuje własny fokus, jeśli ma własny tekst, stan lub czynności. Gdy aktywujesz fragment wewnątrz, SandFly aktywuje jego element. Opcja jest domyślnie wyłączona. Możesz ją przełączyć poleceniem **Zaznaczaj osobno tekst wewnątrz elementów** w kategorii **Nawigacja**. SandFly informuje o włączeniu lub wyłączeniu.
- **W tabelach przełączaj na nawigację po tabeli** opisano w [obsłudze tabel](#reading-tables).
- **Inteligentny tryb przeglądania** i **Zapamiętaj tryb przeglądania w każdej aplikacji** opisano w [rozdziale o treści WWW](#web).
- **Czytaj cytaty odpowiedzi w Telegramie razem z wiadomością** znajdziesz w części **Aplikacje**. Zobacz [Telegram](#reading-telegram).

#### Telegram {#reading-telegram}

Domyślnie SandFly odczytuje w Telegramie i aplikacjach opartych na nim, takich jak Nekogram, cytat zaraz po czasie wiadomości, a następnie nazwę nadawcy cytatu i jego treść. Fokus zostaje na samej wiadomości. Żeby otworzyć cytowaną wiadomość, wybierz czynność **Przejdź do cytowanej wiadomości**.

Jeśli chcesz przechodzić do cytatu osobno, wyłącz **Nawigacja** > **Czytaj cytaty odpowiedzi w Telegramie razem z wiadomością**. Czynność **Przejdź do cytowanej wiadomości** będzie wtedy niedostępna.

Przycisk udostępniania przy wiadomości z kanału znajdziesz w jej czynnościach pod nazwą nadaną przez Telegram. SandFly pomija go przy przesuwaniu i eksploracji dotykiem.
