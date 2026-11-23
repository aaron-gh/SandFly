## Strony i elementy WWW {#web}

Strony w przeglądarce i treści WWW osadzone w aplikacjach obsługujesz tymi samymi [gestami czytania](#reading) co inne ekrany. Możesz też przechodzić od nagłówka do nagłówka, od linku do linku albo wybierać inne rodzaje elementów. Wiele z tych samych poleceń działa także w zwykłych widokach aplikacji. Jeśli korzystasz z klawiatury, zobacz [skróty klawiszowe](#keyboards).

### Czytanie strony {#web-read}

- Przesuń palcem w prawo lub w lewo, żeby przejść do następnego lub poprzedniego elementu. Po ostatnim elemencie strony kolejny ruch może przenieść Cię na przykład na pasek narzędzi przeglądarki.
- Dotknij ekranu, żeby odczytać to, co jest pod palcem.
- Użyj **Czytaj od góry** lub **Czytaj od zaznaczonego elementu**, żeby włączyć [czytanie ciągłe](#reading-continuous).

SandFly porusza się zgodnie z kolejnością elementów na stronie. Sięga po następny element przez mechanizm strony wtedy, gdy tego potrzebuje. Ma to zapobiegać omijaniu pozycji tuż po otwarciu strony.

Po przewinięciu dwoma palcami lub użyciu polecenia przewijania fokus może znaleźć się poza ekranem. SandFly szuka wtedy pierwszego widocznego elementu, a przy przewijaniu wstecz ostatniego. Jeśli element zniknie po zmianie lub przeładowaniu strony, czytnik próbuje ustawić fokus na pozycji, która zajęła jego miejsce.

Przy punkcie orientacyjnym, takim jak obszar nawigacji lub główna część strony, usłyszysz jego rodzaj oraz nazwę. Pola wyboru i przyciski opcji są odczytywane razem z etykietą. Gdy sama strona przenosi fokus na wpis, tak jak Mastodon przy użyciu J lub K, SandFly czyta tekst linków w obrębie wpisu, na przykład autora, godzinę i hasztagi. Przyciski i odstępy między linkami pomija w tym odczycie.

Niektóre syntezatory, w tym Gryphon, potrafią przerwać mowę dopiero między przekazanymi fragmentami. Jeśli po przejściu z długiego akapitu czekasz na następny odczyt, wypróbuj opcję **Mowa** > **Przekazuj długi tekst do syntezatora zdanie po zdaniu**. Zobacz [ustawienia wymowy](#speech-pronunciation).

### Przechodzenie po elementach wybranego rodzaju {#web-elements}

[Wybierz opcję sterowania czytaniem](#reading-controls). Przesuń palcem w dół, żeby znaleźć następny element danego rodzaju, albo w górę, żeby znaleźć poprzedni. Gdy SandFly nie znajdzie kolejnego nagłówka, usłyszysz komunikat o jego braku.

Od razu dostępne są **Nagłówki**, **Linki** oraz **Elementy sterujące**, czyli między innymi przyciski, pola wyboru i pola tekstowe. Otwórz **Sterowanie czytaniem** i zajrzyj do opcji **Nawigacja po elementach** oraz **Poziomy nagłówków**, żeby dodać:

- **Punkty orientacyjne**, na przykład obszar główny lub nawigację.
- **Przyciski**, **Pola wyboru**, **Opcje** i **Pola edycji**. Osobna opcja pól wyboru odpowiada listom rozwijanym.
- **Elementy możliwe do zaznaczenia**, czyli te, które mogą przyjąć fokus klawiatury.
- **Obrazy i filmy**, **Listy**, **Pozycje na liście** oraz **Tabele**.
- **Kliknięte linki** i **Nieotwarte linki**.
- Poziomy nagłówków od 1 do 6.

[Gesty](#gestures) i [skróty klawiszowe](#keyboards) pozwalają przejść do wybranego elementu także bez zmiany opcji sterowania czytaniem. Przy przypisywaniu gestu otwórz kategorię **Nawigacja według elementu** i wybierz na przykład **Następny nagłówek**, **Poprzedni nagłówek** albo **Następny nagłówek 1**.

Do [menu SandFly](#commands) możesz też dodać polecenie **Nawigacja**. Z jego listy wybierzesz między innymi znaki, nagłówki lub punkty orientacyjne. Po dodaniu polecenia otwórz **Menu SandFly** w ustawieniach i użyj widocznej pod nim opcji **Edytuj opcje nawigacji**.

#### Elementy w zwykłych widokach aplikacji {#web-elements-in-apps}

| Rodzaj elementu | Co SandFly wyszukuje w aplikacji |
| --- | --- |
| Nagłówki | Elementy oznaczone jako nagłówki. Aplikacja nie podaje ich poziomów, więc każdy z poziomów obejmuje wszystkie nagłówki. |
| Linki | Linki w tekście. |
| Elementy sterujące | Przyciski, pola wyboru, przełączniki, pola tekstowe, suwaki i podobne elementy. |
| Punkty orientacyjne | Paski narzędzi, kart, nawigacji lub wyszukiwania oraz panele i grupy z tytułem. |
| Przyciski | Zwykłe, obrazkowe i pływające przyciski. |
| Pola wyboru | Pola, przełączniki oraz przyciski zmieniające stan. |
| Opcje | Przyciski jednokrotnego wyboru. |
| Pola edycji | Pola tekstowe. |
| Listy rozwijane | Elementy, które po otwarciu pokazują listę opcji. |
| Elementy możliwe do zaznaczenia | Elementy, które mogą przyjąć fokus klawiatury. |
| Obrazy i filmy | Obrazy, także te, które możesz uaktywnić. |
| Listy | Listy i inne zbiory elementów poza tabelami. |
| Pozycje na liście | [Wiersze listy, na której jest fokus](#reading-lists-tables). |
| Tabele | Tabele oraz siatki z wierszami i kolumnami. |
| Kliknięte linki | Aplikacje nie podają informacji o odwiedzeniu linku, więc wyszukiwanie nie da wyniku. |
| Nieotwarte linki | Wszystkie linki. |

Jeżeli kolejny element jest poza ekranem, SandFly może przewinąć zawierającą go listę do dziesięciu razy. Włączenie **Sterowanie czytaniem** > **Łącza i elementy sterujące tylko w treści internetowej** ukrywa te dwie opcje na liście wyboru poza stronami WWW. Gesty i skróty nadal działają. Domyślnie przełącznik jest wyłączony.

Elementy dodane przez [rozpoznawanie ekranu SandFly Pro](#pro) możesz czytać, odnajdywać dotykiem i przeglądać tak samo jak pozostałe elementy aplikacji.

### Tabele na stronach {#web-tables}

Wybierz **Tabele**, żeby przechodzić między tabelami. Po wejściu do tabeli używaj opcji **Wiersze i kolumny**, **Wiersz** albo **Kolumna**, żeby [poruszać się po komórkach](#reading-tables).

Na [klawiaturze](#keyboards) w kategorii **Tabele** znajdziesz też polecenia przejścia na początek lub koniec wiersza, kolumny albo tabeli oraz odczyt nagłówków wiersza i kolumny.

### Czynności na stronach {#web-actions}

Włącz **Sterowanie czytaniem** > **Automatycznie zmieniaj opcję sterowania czytaniem**, żeby SandFly wybierał **Czynności** po przejściu na element, który je udostępnia. Na stronie WWW dotyczy to elementów z własnymi czynnościami. Przy zwykłym tekście i linkach w tekście pozostaje wybrana przez Ciebie opcja, na przykład **Nagłówki**.

Gdy przejdziesz z elementu z czynnościami na zwykły tekst, SandFly wróci do poprzedniej opcji. Jeśli automatycznie wybrane **Czynności** nie mają akurat nic do wyświetlenia, ruch palcem w górę lub w dół wykona polecenie poprzedniej opcji.

### Tryb przeglądania {#web-browse-mode}

W trybie przeglądania klawisze fizycznej klawiatury i polecenia monitora brajlowskiego służą do poruszania się po treści. Na przykład H przechodzi do następnego nagłówka. Po wyłączeniu trybu klawisze trafiają do strony. Możesz wtedy pisać w polach tekstowych lub używać strzałek w kontrolkach, które ich potrzebują.

Aby zmienić tryb, użyj **Włącz lub wyłącz tryb przeglądania** z kategorii **Strony internetowe**. Możesz przypisać to polecenie do gestu albo użyć skrótu z [wybranej mapy klawiatury](#keyboards). Zmianę usłyszysz w mowie i w dźwięku.

W ustawieniach **Nawigacja** są dwie dodatkowe opcje:

- **Inteligentny tryb przeglądania** działa domyślnie. Włącza tryb w treści WWW i wyłącza go w polach tekstowych i kontrolkach obsługiwanych strzałkami. Dotyczy to między innymi suwaków, list, menu, siatek, drzewa, list rozwijanych i pól zmiany wartości. Gdy skrót przeniesie Cię na taki element, klawiatura również otrzyma na nim fokus. Jeśli ręcznie wyłączysz tryb, samo przechodzenie po stronie go nie przywróci. W aplikacji tryb wyłącza się także po przejściu fokusu klawiatury do pola tekstowego lub suwaka.
- **Zapamiętaj tryb przeglądania w każdej aplikacji** zachowuje ustawienie, które ostatnio wybrano w danej aplikacji. Domyślnie jest wyłączona. Dopóki samodzielnie nie wybierzesz trybu w aplikacji, pozostaje wyłączony, chyba że włączy go inteligentny tryb w treści WWW.

### Listy elementów i adresy {#web-lists}

W kategorii skrótów klawiaturowych **Strony internetowe** są polecenia **Pokaż listę nagłówków**, **Pokaż listę punktów orientacyjnych**, **Pokaż listę linków**, **Pokaż listę elementów sterujących** oraz **Pokaż listę tabel**. Każde otwiera [wyszukiwanie na ekranie](#reading-search) zawężone do wybranego rodzaju. Wybierz wynik, żeby przejść do tego miejsca. Po podłączeniu klawiatury możesz też przełączać rodzaj elementów bezpośrednio w oknie wyszukiwania.

**Odczytaj adres URL** podaje adres linku, na którym jest fokus, a **URL strony do przeczytania na głos** podaje adres całej strony. To polecenia z [mapy klawiatury](#keyboards). Do adresów WWW, e-maili i numerów telefonu zapisanych w tekście użyj opcji [Adresy i numery](#reading-addresses).

### Ustawienia dotyczące stron WWW {#web-settings}

- Zachowanie i zapamiętywanie [trybu przeglądania](#web-browse-mode) znajdziesz w ustawieniach **Nawigacja**.
- Zakres [sterowania czytaniem w aplikacjach](#web-elements-in-apps) i wybór [czynności na stronie](#web-actions) ustawisz na ekranie **Sterowanie czytaniem**.
- [Profil ustawień](#profiles) może włączać się sam w treści WWW, na przykład po to, żeby zmienić tempo mowy tylko na stronach.
