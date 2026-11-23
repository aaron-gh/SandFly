## Szczegółowość odczytu {#verbosity}

W **Szczegółowość** decydujesz, ile SandFly mówi o elementach, tekście, tabelach i zmianach na ekranie. Możesz otworzyć ten ekran poleceniem o tej samej nazwie albo [dodać je do menu SandFly](#commands). Wymowę ustawisz na ekranie [**Mowa**](#speech), a echo klawiatury w [**Pisanie**](#typing).

### Informacje o elementach {#verbosity-items}

Ustawienia znajdziesz w części **Elementy**.

- **Odczytuj wskazówki użytkowania** podpowiada, jak obsłużyć element, na przykład że dwa stuknięcia go aktywują. Domyślnie jest włączone.
- **Odczytuj typ elementu** podaje informacje takie jak „przycisk” albo „pole wyboru”. Domyślnie jest włączone.
- **Odczytywanie typu elementu bez pauzy** łączy nazwę i typ ze spacją. Domyślnie jest wyłączone.
- **Odczytywanie informacji o listach** podaje pozycję elementu i informacje o liście. Domyślnie jest włączone.
- **Odczytywanie informacji o siatkach i tabelach** podaje wiersze i kolumny, także na ekranach z ikonami ułożonymi w siatkę. Domyślnie jest włączone. Od tej opcji zależą [ustawienia tabel](#verbosity-tables).
- **Kolejność szczegółów pozycji** ustala [kolejność odczytu](#verbosity-item-order).
- **Odczytuj identyfikatory elementów** odczytuje identyfikatory przycisków bez etykiety. Domyślnie jest wyłączone.

#### Zmień kolejność informacji o elemencie {#verbosity-item-order}

Otwórz **Szczegółowość** > **Kolejność szczegółów pozycji**. Domyślna kolejność to **Stan**, **Nazwa**, **Typ**.

Aby przesunąć pozycję, stuknij dwa razy, przytrzymaj i przeciągnij w górę lub w dół. Przy wyłączonym czytniku wystarczy przytrzymać i przeciągnąć. Możesz też użyć **Przenieś w górę** albo **Przenieś w dół**. SandFly podaje pozycję przy rozpoczęciu, w trakcie przenoszenia i po upuszczeniu.

Ta kolejność obowiązuje również wtedy, gdy SandFly używa tekstu całego wiersza jako nazwy przełącznika lub pola wyboru. Dotyczy też przycisków i kart z etykietą w osobnym elemencie wewnątrz kontrolki.

### Tekst i symbole {#verbosity-text}

Opcja **Odczytuj znaki interpunkcyjne i symbole** ma cztery poziomy.

- **Wszystkie** odczytuje wszystkie znaki interpunkcyjne i symbole.
- **Większość** odczytuje między innymi `&` i `@`. Przecinki, apostrofy i inne częste znaki pozostawia syntezatorowi.
- **Niektóre** to ustawienie domyślne. Syntezator decyduje, które znaki odczyta.
- **Brak** pomija interpunkcję i symbole, chyba że czytasz pojedynczy znak. Apostrofy i łączniki wewnątrz słów, separatory liczb i końcowa interpunkcja pozostają do wymówienia przez syntezator.

Po wybraniu **Brak** pojawia się **Odczytuj symbole zmieniające znaczenie**. Domyślnie opcja jest wyłączona. Po włączeniu SandFly przekazuje syntezatorowi między innymi plus i minus przed liczbą, znaki stopni, procentów, promili i walut oraz znaki działań i porównań między liczbami. Wybierz **Więcej informacji**, żeby otworzyć opis poziomów w przeglądarce. Poziom zmienisz też w sterowaniu czytaniem albo skrótem **Przełącz poziom interpunkcji**. Własną wymowę ustawisz w [słowniku symboli](#speech-symbol-dictionary).

W opcji **Wielkie litery** możesz wybrać **Nic nie rób**, **Powiedz „wielkie”**, **Zmień wysokość głosu** albo **Odtwórz dźwięk**. Domyślnie SandFly mówi „wielkie”. Przy pozostałych ustawieniach przekazuje pojedynczą literę do syntezatora małą literą.

**Podawaj liczbę powtórzonych symboli** działa domyślnie. SandFly podaje liczbę, gdy znak powtarza się co najmniej cztery razy. Na tym ekranie są też ustawienia [emoji](#verbosity-emoji) i [formatowania](#verbosity-text-formatting).

#### Emoji {#verbosity-emoji}

Wybierz **Szczegółowość** > **Emoji**.

- **Odczytywane przez syntezator mowy** przekazuje znaki bez zmian. To ustawienie domyślne.
- **Odczytywane przez SandFly** zastępuje znaki nazwami Unicode CLDR w języku odczytywanego tekstu. Emoji z odcieniem skóry, rodzina, flaga lub znak przypominający klawisz mają po jednej nazwie. Jeśli brakuje nazwy w danym języku, SandFly używa angielskiej. Jeśli w ogóle nie zna nazwy, przekazuje znak do syntezatora.
- Możesz też wyłączyć odczyt emoji. SandFly nadal nazywa tekst złożony wyłącznie z emoji, na przykład przycisk reakcji.

Przy odczytywaniu przez SandFly opcja **Podawaj liczbę powtórzonych emoji** odczytuje powtarzający się znak raz i podaje liczbę powtórzeń. Domyślnie jest **Wyłączone**. Możesz wybrać próg od **2 lub więcej** do **6 lub więcej**. **Odczytuj „emoji” po nazwach emoji** dodaje słowo „emoji” po każdej nazwie. Domyślnie jest wyłączone. Opcja znika, gdy wyłączysz odczyt emoji.

Jeśli emoji czyta SandFly, nawigacja po znakach odczytuje całe emoji jako jeden znak, a nawigacja po słowach zatrzymuje się na emoji. Jeśli odczyt wyłączysz, nawigacja po słowach je pomija. Kopiowanie i literowanie ostatniej wypowiedzi zachowują znak emoji. Głos i tempo ustawisz w [ustawieniach mowy](#speech-emoji). Sposób odczytu zmienisz też przez **Emoji** w sterowaniu czytaniem lub poleceniach.

#### Formatowanie tekstu {#verbosity-text-formatting}

Włącz **Szczegółowość** > **Informuj o formatowaniu tekstu**, żeby podczas odczytu słyszeć zmiany takie jak początek i koniec pogrubienia. Domyślnie jest wyłączone. Gdy je włączysz, pojawią się **Opcje formatowania tekstu**.

- W opcji **Styl powiadomienia** wybierz **Mowa i dźwięk**, **Tylko mowa** albo **Tylko dźwięk**. Domyślnie oba sygnały są włączone.
- Otwórz **Powiadomienia o formatowaniu tekstu** i wybierz rodzaje zmian. Domyślnie SandFly informuje o pogrubieniu, kursywie, podkreśleniu, przekreśleniu, nazwie koloru, rozmiarze i rodzaju czcionki. Odczyt wartości szesnastkowej koloru możesz włączyć osobno.

Opcję przełączysz poleceniem lub sterowaniem czytaniem **Informuj o formatowaniu tekstu**. Aby sprawdzić formatowanie tekstu, na którym jest fokus, wybierz **Opisz formatowanie tekstu** w kategorii **Czytanie**.

### Tabele {#verbosity-tables}

Opcje w **Tabele** działają przy włączonym **Odczytywanie informacji o siatkach i tabelach**.

- **Odczytuj numery wierszy i kolumn** jest domyślnie włączone. Wyłącz je, aby słyszeć tylko nagłówki mające nazwę.
- W **Odczytuj nagłówki kolumn** domyślne **Po zawartości komórki** podaje najpierw treść, a potem nagłówki lub numery. **Przed zawartością komórki** odwraca kolejność. Przy komórce nagłówkowej SandFly podaje też, że to nagłówek kolumny albo wiersza. Dotyczy to również siatek, na przykład zdjęć. **Nie odczytuj** pomija nagłówki kolumn. Numery są nadal dostępne po włączeniu odczytu numerów.

### Komunikaty {#verbosity-announcements}

- W opcji **Nazwy okien** wybierz **Zawsze**, żeby słyszeć nazwę po otwarciu okna i po przeniesieniu fokusu do niego, jeszcze przed pierwszym elementem. Domyślne **Zawsze ucinane przy pierwszym elemencie** pozwala odczytowi pierwszego elementu przerwać nazwę nowego okna. **Tylko wtedy, gdy okno się otworzy** ogranicza odczyt do otwierania, a **Nigdy** wyłącza nazwy.
- **Zawsze odczytuj liczbę elementów z listy** po przewinięciu podaje, które pozycje są widoczne. Domyślnie jest wyłączone.
- **Informuj o pełnym naładowaniu** odczytuje komunikat i gra **Pełne naładowanie** raz po naładowaniu baterii przy podłączonej ładowarce. Domyślnie jest wyłączone.
- **Poziom baterii** odczytuje poziom po osiągnięciu wielokrotności 5 lub 10 procent albo co 15, 30 lub 60 minut. Działa przy włączonym ekranie niezależnie od ładowania. Domyślnie jest wyłączone. W opcji **Głos poziomu baterii** wybierz [**Głos powiadomień**](#notifications-voice), domyślny **Głos SandFly** albo profil głosowy.
- **Informuj o czasie trwania połączenia** po zakończeniu podaje czas od wybrania numeru lub odebrania rozmowy. Pomija połączenia nieodebrane. Domyślnie jest wyłączone.
- **Informowanie o pokazaniu lub ukryciu klawiatury** jest domyślnie włączone.
- **Włączanie i wyłączanie ekranu** pozwala ustawić [komunikaty o ekranie i odblokowaniu](#verbosity-screen-on-off).
- **Format godziny** ustala odczyt **12-godzinny**, **24-godzinny** albo domyślne **Użyj formatu godziny urządzenia**.
- **Podawaj sekundy w godzinie** jest domyślnie wyłączone. Jeśli je włączysz, zależna od niego opcja **Podawaj sekundy po włączeniu ekranu** uwzględni sekundy także po wybudzeniu. Ta druga opcja jest domyślnie włączona.
- **Odczytywanie stanu urządzenia** zmienia [zawartość komunikatu o stanie](#verbosity-status-readout).

#### Zmiany stanu ekranu {#verbosity-screen-on-off}

Otwórz **Szczegółowość** > **Włączanie i wyłączanie ekranu**. Ustawienia są osobne dla **Przy wyłączaniu ekranu**, **Przy włączaniu ekranu** i **Przy odblokowywaniu telefonu**. Wybudzenie zewnętrznego ekranu składanego telefonu liczy się jako włączenie ekranu. Jeśli od razu rozłożysz telefon, SandFly nie powtórzy komunikatu.

- Przy wyłączaniu ekranu domyślnie działają **Odczytuj „Ekran wyłączony”** i **Tryb dzwonka**. Ten drugi mówi, gdy dzwonek jest wyciszony lub ustawiony na wibrację.
- Przy odblokowywaniu domyślnie działa **Odczytuj „Urządzenie zostało odblokowane”**.
- **Godzina** i **Liczba powiadomień** są domyślnie włączone przy włączaniu ekranu, a wyłączone w pozostałych grupach.
- W części **Bateria** możesz wybrać odczyt **Zawsze** lub **Tylko podczas ładowania**, albo zostawić domyślne wyłączenie. **Niski poziom baterii** pozwala odczytać poziom poniżej wybranego progu od 10 do 50 procent. Domyślnie jest wyłączone.
- **Ukryty ekran** podaje stan tylko wtedy, gdy ekran [jest ukryty](#display). **Wi-Fi** podaje nazwę sieci i siłę sygnału, a **Sieć komórkowa** operatora, rodzaj sieci i sygnał. Domyślnie wszystkie trzy opcje są wyłączone.
- **Dzwonek i tryb Nie przeszkadzać** informuje tylko o wibracji, wyciszeniu lub włączonym trybie Nie przeszkadzać. **Tryb samolotowy** odzywa się tylko przy włączonym trybie. Obie opcje są domyślnie wyłączone.

#### Odczytywanie stanu urządzenia {#verbosity-status-readout}

Otwórz **Szczegółowość** > **Odczytywanie stanu urządzenia**. Kolejność domyślna to **Godzina**, **Bateria**, **Wi-Fi**, **Sieć komórkowa**, **Dzwonek i tryb Nie przeszkadzać**, **Tryb samolotowy** i **Ukryty ekran**. Wszystkie pozycje są włączone. SandFly pomija stany zwykłe, na przykład działający dzwonek.

Wybierz pozycję, żeby ją włączyć lub wyłączyć. Do zmiany kolejności użyj **Przenieś w górę** lub **Przenieś w dół**. Ustawienia nie są dostępne na telewizorach.

### Aplikacje z własną mową {#verbosity-own-speech}

Jeśli aplikacja lub klawiatura sama odczytuje treść, możesz ograniczyć mowę SandFly w części **Aplikacje z własną mową**. Fokus, dźwięki i wibracje nadal działają. SandFly zawsze czyta własne ekrany.

- **Odczytuj tylko fokus przeniesiony przez użytkownika** domyślnie jest wyłączone. Możesz wybrać **We wszystkich aplikacjach** lub **W wybranych aplikacjach**. SandFly czyta wtedy zmiany po dotyku, gestach, klawiszach, brajlu i poleceniach. Pomija między innymi fokus przeniesiony przez aplikację, komunikaty, obszary zmieniające się samoczynnie i nazwy okien. Za ruch aplikacji uznaje także przeniesienie po zmianie okna, przewinięciu lub za kursorem edycji.
- **Wybrane aplikacje** pojawia się dla wariantu **W wybranych aplikacjach**. Użyj **Dodaj aplikację**, żeby dodać pozycję, lub wybierz istniejącą, żeby ją usunąć.
- **Odczytuj tylko fokus przeniesiony przez użytkownika, gdy asystent lub połączenie odtwarza dźwięk** stosuje to ograniczenie wszędzie podczas dźwięku asystenta lub połączenia. Ruchy wykonane przez Ciebie są nadal odczytywane. Domyślnie opcja jest wyłączona.
- W **Klawiatury z własną mową** dodaj klawiaturę przez **Dodaj klawiaturę**. Gdy wybrana klawiatura jest widoczna, SandFly pomija odczyt jej klawiszy, wpisanych i usuniętych znaków oraz ruchów kursora w polu tekstowym. Gesty i polecenia działają, a ruch po tekście przez sterowanie czytaniem nadal jest odczytywany.

Opcję **Odczytuj tylko fokus przeniesiony przez użytkownika** możesz dodać do [sterowania czytaniem](#commands). Polecenie o tej samej nazwie przełącza ograniczenie. Po włączeniu używa wybranych aplikacji, jeśli są na liście, a w przeciwnym razie wszystkich.
