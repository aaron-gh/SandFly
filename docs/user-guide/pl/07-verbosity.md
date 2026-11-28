## Szczegółowość {#verbosity}

Na ekranie **Szczegółowość** w ustawieniach SandFly możesz wybrać, co SandFly czyta o elementach, tekście, symbolach, tabelach i zmianach na ekranie. Tutaj ustawisz też, kiedy ograniczać odczyt w aplikacjach z własną mową. Ustawienia są podzielone na **Elementy**, **Tekst i symbole**, **Tabele**, **Komunikaty** i **Aplikacje z własną mową**.

Ustawienia wymowy, w tym odczyt fonetyczny i słownik mowy, znajdziesz na ekranie **Mowa**. Zobacz [](#speech). Echo klawiatury ustawisz na ekranie **Pisanie**. Zobacz [](#typing).

Ten ekran otwiera polecenie **Szczegółowość**. Możesz je też dodać do menu SandFly. Zobacz [](#commands).

### Elementy {#verbosity-items}

W grupie **Elementy** wybierzesz, jakie informacje SandFly podaje o każdym elemencie.

- **Odczytuj wskazówki użytkowania** odczytuje po opisie elementu podpowiedź obsługi, np. „Kliknij dwukrotnie, aby aktywować”. Domyślnie opcja jest włączona.
- **Odczytuj typ elementu** podaje jego typ, np. „Przycisk” lub „Pole wyboru”. Domyślnie opcja jest włączona.
- **Odczytywanie typu elementu bez pauzy** odczytuje nazwę i typ ze spacją między nimi, bez dodatkowej pauzy. Domyślnie opcja jest wyłączona.
- **Odczytuj „Dostępne czynności”** odczytuje „Dostępne czynności” przy elemencie z czynnościami aplikacji, np. z odpowiedzią na wiadomość. Działa niezależnie od ustawienia **Odczytuj wskazówki użytkowania**. Jeśli wskazówki są włączone, podają tylko gest otwierający czynności, np. stuknięcie dwoma palcami. Domyślnie opcja jest wyłączona. O obsługiwanych elementach i dźwięku **Dostępne czynności** przeczytasz w [](#sound-actions-available).
- **Odczytywanie informacji o listach** podaje pozycję na liście i szczegóły listy. Domyślnie opcja jest włączona.
- **Odczytywanie informacji o siatkach i tabelach** podaje wiersze i kolumny w siatkach, tabelach i widokach stron ułożonych w wierszach i kolumnach, np. w folderze na ekranie głównym. Domyślnie opcja jest włączona. Tylko wtedy działają ustawienia z grupy **Tabele**. Zobacz [](#verbosity-tables).
- **Kolejność szczegółów pozycji** ustawia kolejność odczytywania nazwy, typu i stanu elementu. Zobacz [](#verbosity-item-order).
- **Odczytuj identyfikatory elementów** podaje identyfikatory przycisków bez etykiet. Domyślnie opcja jest wyłączona.

#### Zmień kolejność szczegółów pozycji {#verbosity-item-order}

Ekran **Szczegółowość** > **Kolejność szczegółów pozycji** pokazuje **Stan**, **Nazwa** i **Typ** w kolejności odczytu. Domyślny porządek to **Stan, nazwa, typ**.

Kolejność możesz zmienić na dwa sposoby.

- Naciśnij i przytrzymaj pozycję, po czym przeciągnij ją w górę lub w dół. Przy włączonym SandFly stuknij dwa razy, przytrzymaj i przeciągnij.
- Użyj czynności **Przenieś w górę** lub **Przenieś w dół**.

SandFly podaje pozycję podczas podnoszenia, przesuwania i odkładania szczegółu.

Ta kolejność dotyczy także wierszy z polem wyboru lub przełącznikiem bez własnego tekstu. SandFly odczytuje taki wiersz jako jeden element z tekstem wiersza w roli nazwy. Ustawienie obejmuje też przyciski i karty, których etykieta jest osobnym elementem w środku.

### Tekst i symbole {#verbosity-text}

W grupie **Tekst i symbole** ustawisz czytanie tekstu, interpunkcji, wielkich liter i emoji.

- **Odczytuj znaki interpunkcyjne i symbole** ma cztery poziomy. **Wszystkie** czyta każdy znak przestankowy i symbol. **Większość** czyta większość symboli, np. & i @, a zwykłą interpunkcję, taką jak przecinki i apostrofy, zostawia syntezatorowi. **Niektóre** to opcja domyślna. Syntezator decyduje wtedy, jak czytać interpunkcję i symbole. **Brak** pomija te znaki w mowie, z wyjątkiem znaku czytanego osobno, np. przy nawigacji znak po znaku. Apostrofy i łączniki wewnątrz słów, separatory wewnątrz liczb i znaki przestankowe na końcu słów pozostają do odczytu przez syntezator. **Więcej informacji** na liście otwiera stronę o poziomach interpunkcji.
- Opcja **Odczytuj symbole zmieniające znaczenie** pojawia się przy poziomie **Brak**. Pozostawia syntezatorowi plus lub minus bezpośrednio przed liczbą, znaki stopnia, procentu, promila i walut, a także + − × ÷ = < > między liczbami. Domyślnie opcja jest wyłączona.
- W opcji **Wielkie litery** wybierz **Nic nie rób**, **Powiedz „wielkie”** (domyślnie), **Zmień wysokość głosu** lub **Odtwórz dźwięk**. Jeśli wybierzesz coś innego niż **Powiedz „wielkie”**, pojedynczą wielką literę SandFly przekaże syntezatorowi jako małą.
- **Emoji**, **Podawaj liczbę powtórzonych emoji** i **Odczytuj „emoji” po nazwach emoji** są opisane w [](#verbosity-emoji).
- **Podawaj liczbę powtórzonych symboli** podaje liczbę wystąpień symbolu, jeśli powtarza się co najmniej cztery razy. Domyślnie opcja jest włączona.
- **Informuj o formatowaniu tekstu** i **Opcje formatowania tekstu** są opisane w [](#verbosity-text-formatting).

Poziom interpunkcji zmienisz pozycją **Odczytuj znaki interpunkcyjne i symbole** w sterowaniu czytaniem lub skrótem **Przełącz poziom interpunkcji**. Jeśli chcesz zastępować symbole wybranymi słowami, użyj **Mowa** > **Słownik symboli**. Zobacz [](#speech-symbol-dictionary).

#### Emoji {#verbosity-emoji}

Niektóre syntezatory odczytują emoji niewyraźnie, nadają im własne nazwy lub je pomijają. Wybierz **Szczegółowość** > **Emoji**, aby ustawić sposób odczytu.

- **Odczytywane przez syntezator mowy** to opcja domyślna. SandFly przekazuje emoji do syntezatora w oryginalnej postaci.
- **Odczytywane przez SandFly** zastępuje emoji nazwami z Unicode CLDR w języku odczytywanego tekstu. Odcień skóry, rodzina, flaga i klawisz są odczytywane jako jedno emoji z jedną nazwą. Emoji bez nazwy SandFly przekazuje syntezatorowi. Gdy nazwy brakuje tylko w danym języku, używa angielskiej.
- **Nie odczytuj** pomija emoji w tekście. Jeśli tekst składa się wyłącznie z emoji, np. przycisk reakcji, SandFly odczytuje nazwę.

Od opcji **Emoji** zależą jeszcze dwa ustawienia.

- **Podawaj liczbę powtórzonych emoji** działa przy **Odczytywane przez SandFly**. Powtarzające się obok siebie emoji czyta raz z podaniem liczby, np. „3 uśmiechnięte buźki”. Do wyboru są **Wyłączone** (domyślnie) oraz progi od **2 lub więcej** do **6 lub więcej**.
- **Odczytuj „emoji” po nazwach emoji** dodaje słowo „emoji” po każdej nazwie. Domyślnie opcja jest wyłączona. Po wybraniu **Nie odczytuj** jest niedostępna.

Przy **Odczytywane przez SandFly** nawigacja po znakach podaje nazwę całego emoji raz, a nawigacja po słowach zatrzymuje się na emoji. Przy **Nie odczytuj** nawigacja po słowach je pomija. Kopiowanie lub literowanie ostatniej wypowiedzi daje same emoji w oryginalnej postaci.

Ustawienie możesz zmieniać poleceniem **Emoji** i pozycją **Emoji** w sterowaniu czytaniem. Głos i szybkość odczytu emoji ustawisz na ekranie **Mowa**. Zobacz [](#speech-emoji).

#### Formatowanie tekstu {#verbosity-text-formatting}

Opcja **Szczegółowość** > **Informuj o formatowaniu tekstu** oznajmia podczas czytania zmiany formatowania, np. początek pogrubienia lub kursywy. Domyślnie jest wyłączona.

Po włączeniu **Informuj o formatowaniu tekstu** pojawia się **Szczegółowość** > **Opcje formatowania tekstu** z poniższymi ustawieniami.

- W opcji **Styl powiadomienia** wybierz **Mowa i dźwięk** (domyślnie), **Tylko mowa** lub **Tylko dźwięk**. Przy mowie usłyszysz np. „początek pogrubienia, tytuł, koniec pogrubienia”.
- W sekcji **Powiadomienia o formatowaniu tekstu** wybierz zgłaszane zmiany. Domyślnie włączone są **Pogrubienie**, **Kursywa**, **Podkreślenie**, **Przekreślenie**, **Nazwa koloru tekstu**, **Rozmiar tekstu** i **Rodzina czcionek**. **Wartość szesnastkowa koloru tekstu** jest domyślnie wyłączona.

Odczyt formatowania możesz włączać i wyłączać także poleceniem **Informuj o formatowaniu tekstu** lub pozycją o tej nazwie w sterowaniu czytaniem. W grupie **Czytanie** polecenie **Opisz formatowanie tekstu** podaje formatowanie tekstu, na którym jest fokus.

### Tabele {#verbosity-tables}

Ustawienia z grupy **Tabele** działają po włączeniu **Odczytywanie informacji o siatkach i tabelach**.

- **Odczytuj numery wierszy i kolumn** podaje numery przy komórkach tabeli. Domyślnie opcja jest włączona. Wyłącz ją, jeśli chcesz słyszeć same nazwane nagłówki.
- W opcji **Odczytuj nagłówki kolumn** wybierz **Po zawartości komórki** (domyślnie), aby najpierw słyszeć treść, a potem nagłówki lub numery wiersza i kolumny. **Przed zawartością komórki** podaje je wcześniej i na komórce nagłówkowej odczytuje „Nagłówek kolumny” lub „Nagłówek wiersza”. Dotyczy to także siatek. Na przykład w siatce zdjęć usłyszysz najpierw numer wiersza i kolumny. **Nie odczytuj** pomija nagłówki kolumn. Numery kolumn nadal są czytane, jeśli włączono **Odczytuj numery wierszy i kolumn**.

### Komunikaty {#verbosity-announcements}

W grupie **Komunikaty** wybierzesz, co SandFly mówi, gdy na urządzeniu coś się zmienia.

- W opcji **Nazwy okien** wartość **Zawsze** podaje nazwę przy otwarciu okna i po przeniesieniu fokusu z innego okna, przed odczytaniem pierwszego elementu. Domyślne **Zawsze, do rozpoczęcia odczytu pierwszego elementu** działa tak samo, ale odczyt pierwszego elementu nowego okna przerywa nazwę. **Tylko wtedy, gdy okno się otworzy** podaje nazwę przy otwarciu. **Nigdy** pomija nazwy.
- **Zawsze odczytuj liczbę elementów z listy** podaje widoczne pozycje po każdym przewinięciu. Domyślnie opcja jest wyłączona. Wtedy SandFly nie podaje pozycji listy podczas przewijania.
- **Informuj o pełnym naładowaniu** raz mówi, że bateria jest pełna, i odtwarza dźwięk **Pełne naładowanie**, jeśli ładowarka jest podłączona. Domyślnie opcja jest wyłączona.
- **Poziom baterii** odczytuje stan baterii podczas ładowania i poza nim. Możesz wybrać odczyt przy osiągnięciu wielokrotności 5% lub 10% albo co 15 minut, 30 minut lub godzinę. Przy wygaszonym ekranie odczyt jest pomijany. Domyślnie opcja jest wyłączona.
- W opcji **Głos ogłaszania poziomu baterii** wybierz **Głos powiadomień**, **Głos SandFly** (domyślnie) lub jeden z profili głosowych. Zobacz [](#notifications-voice).
- **Odczytuj czas trwania połączenia** podaje długość połączenia po jego zakończeniu, liczoną od odebrania lub wybrania numeru. Połączenia nieodebrane są pomijane. Domyślnie opcja jest wyłączona.
- **Informowanie o pokazaniu lub ukryciu klawiatury** jest domyślnie włączone.
- **Włączanie i wyłączanie ekranu** określa komunikaty przy wygaszaniu, wybudzaniu i odblokowaniu. Zobacz [](#verbosity-screen-on-off).
- W opcji **Format godziny** wybierz **Użyj formatu godziny urządzenia** (domyślnie), **12-godzinny** lub **24-godzinny**.
- **Odczytuj godzinę z sekundami** jest domyślnie wyłączone.
- **Odczytuj sekundy także po włączeniu ekranu** pozwala podać sekundy przy wybudzeniu urządzenia, gdy włączono **Odczytuj godzinę z sekundami**. Domyślnie opcja jest włączona.
- **Odczytywanie stanu urządzenia** ustala zawartość polecenia **Odczytaj stan urządzenia**. Zobacz [](#verbosity-status-readout).

#### Włączanie i wyłączanie ekranu {#verbosity-screen-on-off}

W menu **Szczegółowość** > **Włączanie i wyłączanie ekranu** są trzy grupy. To **Przy wyłączaniu ekranu**, **Przy włączaniu ekranu** i **Przy odblokowywaniu telefonu**. W składanym telefonie wybudzenie zewnętrznego ekranu liczy się jako włączenie ekranu. Jeśli od razu otworzysz telefon, komunikat nie będzie powtórzony. Grupy mają te same ustawienia poza pierwszymi trzema.

- **Odczytuj „Ekran wyłączony”** jest dostępne tylko w grupie **Przy wyłączaniu ekranu**. Domyślnie opcja jest włączona.
- **Tryb dzwonka** jest dostępny tylko w grupie **Przy wyłączaniu ekranu**. Podaje, czy włączono wibracje lub tryb cichy. Domyślnie opcja jest włączona.
- **Odczytuj „Urządzenie zostało odblokowane”** jest dostępne tylko w grupie **Przy odblokowywaniu telefonu**. Domyślnie opcja jest włączona.
- **Godzina** jest domyślnie włączona przy włączaniu ekranu i wyłączona w dwóch pozostałych grupach.
- **Liczba powiadomień** jest domyślnie włączona przy włączaniu ekranu i wyłączona w dwóch pozostałych grupach.
- W ustawieniu **Bateria** wybierz **Wyłączone** (domyślnie), **Zawsze** albo **Tylko podczas ładowania**.
- **Niski poziom baterii** podaje poziom po spadku poniżej wybranego progu. Dostępne są **Wyłączone** (domyślnie), **Poniżej 10%**, **Poniżej 15%**, **Poniżej 20%**, **Poniżej 30%** i **Poniżej 50%**.
- **Ukrycie ekranu** podaje informację tylko przy ukrytym ekranie. Domyślnie opcja jest wyłączona. Zobacz [](#display).
- **Wi-Fi** podaje nazwę sieci i siłę sygnału. Domyślnie opcja jest wyłączona.
- **Sieć komórkowa** podaje operatora, rodzaj sieci i siłę sygnału. Domyślnie opcja jest wyłączona.
- **Dzwonek i tryb Nie przeszkadzać** odczytuje informację tylko wtedy, gdy dzwonek jest wyciszony lub działa tryb Nie przeszkadzać. Domyślnie opcja jest wyłączona.
- **Tryb samolotowy** odczytuje informację tylko przy włączonym trybie samolotowym. Domyślnie opcja jest wyłączona.

Powyżej grup znajdziesz **Odczytuj podczas odtwarzania multimediów**. Pozwala odczytywać komunikat o wyłączeniu ekranu i odblokowaniu w trakcie odtwarzania. Domyślnie opcja jest wyłączona. Komunikat o włączeniu ekranu słychać podczas odtwarzania bez względu na to ustawienie.

#### Odczytywanie stanu urządzenia {#verbosity-status-readout}

Ekran **Szczegółowość** > **Odczytywanie stanu urządzenia** pokazuje informacje podawane przez polecenie **Odczytaj stan urządzenia** w kolejności odczytu. To **Godzina**, **Bateria**, **Wi-Fi**, **Sieć komórkowa**, **Dzwonek i tryb Nie przeszkadzać**, **Tryb samolotowy** i **Ukrycie ekranu**. Domyślnie wszystkie są włączone. Zwykły stan, np. to, że dzwonek jest włączony, jest pomijany.

- Wybierz pozycję, żeby ją włączyć lub wyłączyć.
- Użyj czynności **Przenieś w górę** lub **Przenieś w dół**, żeby zmienić kolejność.

Ekran **Odczytywanie stanu urządzenia** jest niedostępny na telewizorach.

### Aplikacje z własną mową {#verbosity-own-speech}

Niektóre aplikacje i klawiatury mają własną mowę. Na przykład asystent czyta odpowiedź w trakcie przenoszenia fokusu ułatwień dostępu na kolejne fragmenty, a klawiatura może sama czytać klawisze. W grupie **Aplikacje z własną mową** możesz ograniczyć mowę SandFly, żeby nie słyszeć tej samej treści dwa razy. Nadal działa przenoszenie fokusu oraz odtwarzanie dźwięków i wibracji. SandFly zawsze czyta swoje ekrany.

- W opcji **Odczytuj elementy tylko po przeniesieniu fokusu przez użytkownika** wybierz **Wyłączone** (domyślnie), **We wszystkich aplikacjach** lub **W wybranych aplikacjach**. Gdy opcja działa, SandFly czyta elementy, na które przenosisz fokus dotykiem, gestem, klawiszami, monitorem brajlowskim lub poleceniem. Pomija fokus przeniesiony przez aplikację, komunikaty, zmiany obszarów dynamicznych, nazwy okien i komunikaty o zmianie okna. Dotyczy to też fokusu przeniesionego przez SandFly po zmianie okna, przewinięciu lub w ślad za fokusem wprowadzania.
- **Wybrane aplikacje** to lista dostępna po ustawieniu wartości **W wybranych aplikacjach**. Wybierz **Dodaj aplikację**, żeby dodać pozycję, lub wybierz już dodaną aplikację, żeby ją usunąć.
- **Odczytuj elementy tylko po przeniesieniu fokusu przez użytkownika podczas odtwarzania dźwięku asystenta lub rozmowy** stosuje tę zasadę we wszystkich aplikacjach w trakcie odtwarzania dźwięku asystenta lub rozmowy przez inną aplikację. Fokus przenoszony przez Ciebie nadal jest odczytywany. Domyślnie opcja jest wyłączona.
- Jeśli wyświetlana klawiatura jest na liście **Klawiatury z własną mową**, SandFly pomija jej klawisze, wpisywany i usuwany tekst oraz ruch kursora w polu edycji. Gesty i polecenia działają dalej. Nadal słyszysz tekst podczas nawigacji za pomocą sterowania czytaniem. Aby dodać klawiaturę, wybierz **Dodaj klawiaturę**.

SandFly pomija komunikaty, zmiany obszarów dynamicznych i zmiany okien usługi `com.oplus.viewtalk` na telefonach realme i OPPO. To usługa ułatwień dostępu, która przekazuje opisy obrazów, np. „Obraz może zawierać…”.

Pozycja **Odczytuj elementy tylko po przeniesieniu fokusu przez użytkownika** w sterowaniu czytaniem zmienia pierwsze ustawienie z tej grupy. Polecenie o tej samej nazwie wyłącza je. Przy ponownym włączeniu wybiera **W wybranych aplikacjach**, jeśli lista zawiera aplikacje, albo **We wszystkich aplikacjach**, jeśli jest pusta. Pozycja nie należy do domyślnego sterowania czytaniem. Zobacz [](#commands).
