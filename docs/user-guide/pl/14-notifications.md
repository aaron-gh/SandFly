## Powiadomienia {#notifications}

SandFly może odczytywać powiadomienia w chwili nadejścia, na żądanie lub o wybranej porze. Otwórz **Powiadomienia** w ustawieniach, żeby wybrać, co i kiedy ma mówić. Na tym samym ekranie możesz włączyć listę powiadomień SandFly. Informacja o połączeniu przychodzącym jest odczytywana niezależnie od tych ustawień.

### Dostęp do powiadomień {#notifications-access}

Niektóre funkcje wymagają zgody Androida. Należą do nich [odczyt powiadomień, których Android nie ogłasza](#notifications-not-announced), dostęp do prywatnych treści na zablokowanym ekranie, podsumowania, lista SandFly i zatrzymywanie mowy po usunięciu powiadomienia. Zgody potrzebujesz również do opcji **Czytaj powiadomienia dozwolone w trybie Nie przeszkadzać**. Bez niej ta opcja przy włączonym trybie wycisza odczyt wszystkich powiadomień.

Otwórz **Kiedy czytać**, **Przestań czytać** lub **Podsumowanie i pora** i wybierz **Włącz dostęp do powiadomień**. Na liście systemowej włącz SandFly. Przycisk pojawia się wtedy, gdy brakuje zgody. SandFly łączy się z usługą powiadomień tylko wtedy, gdy działa czytnik.

### Włączanie i wyłączanie odczytu {#notifications-speech-on-off}

Opcja **Powiadomienia** > **Odczytuj powiadomienia** jest domyślnie włączona. Odczyt możesz też przełączać kafelkiem **Czytanie powiadomień na głos**, poleceniem **Włączanie i wyłączanie czytania powiadomień** lub z [aplikacji do automatyzacji](#notifications-automation). Po wyłączeniu odczytu SandFly nadal informuje o połączeniach przychodzących.

### Polecenia dotyczące powiadomień {#notifications-commands}

W kategorii **Powiadomienia** znajdziesz polecenia:

- **Czytaj powiadomienia**. Odczytuje [podsumowanie](#notifications-summary) z obszaru powiadomień.
- **Przeczytaj jeszcze raz ostatnie powiadomienie**. Powtarza to, które SandFly wcześniej odczytał. Jeśli odczytów jeszcze nie było, usłyszysz o tym.
- **Zatrzymaj czytanie powiadomienia**. Przerywa bieżący odczyt i usuwa oczekujące powiadomienia z kolejki mowy.
- **Włączanie i wyłączanie czytania powiadomień**.
- **Ogłaszanie godziny włączone lub wyłączone**. Wyłącza ustawienie **Podawaj godzinę co** lub przywraca poprzedni odstęp. Jeśli nie był wcześniej zapisany, SandFly będzie podawał godzinę co 60 minut.

[Polecenia](#commands) możesz przypisać do gestów, klawiatury i innych skrótów. Użycie gestu lub klawisza podczas odczytu powiadomienia przerywa odczyt. Usłyszysz wynik użytego polecenia.

### Treść i długość odczytu {#notifications-reading}

Otwórz **Powiadomienia** > **Czytanie powiadomień na głos**. W części **Co jest czytane** możesz zmienić kolejność i zakres informacji.

- **Format mowy** zawiera tekst i pola do wypełnienia: `{app}` (aplikacja), `{title}` (tytuł), `{text}` (tekst), `{content}` (cała treść), `{bigtext}` (rozwinięty tekst), `{subtext}` (tekst dodatkowy), `{ticker}` (krótki komunikat), `{category}` (kategoria) i `{time}` (godzina). Domyślnie SandFly używa `{category}, {title}, {text}`.
- **Odczytywanie prywatnych powiadomień** pozwala podać, co powiedzieć zamiast prywatnej treści. Dostępne pola to `{app}`, `{category}` i `{time}`. Domyślnie usłyszysz nazwę aplikacji (`{app}`).

W części **Tekst** są opcje **Pomiń emoji**, **Czytaj wielkie litery jako słowa**, **Czytaj długie liczby cyfra po cyfrze** i **Pomiń powiadomienia bez tekstu**. Wszystkie są domyślnie wyłączone. Przy odczycie cyfr ustaw **Minimalna liczba cyfr w długiej liczbie** od 3 do 10. Początkowy próg to 5 cyfr.

W opcji **Linki** możesz zostawić **Czytaj cały link**, ograniczyć odczyt do **Czytaj tylko nazwę witryny** lub wybrać **Pomiń**. Aby zastąpić link własnym komunikatem, użyj **Wypowiedz poniższy tekst** i wpisz go w polu **Tekst zamiast linku**.

W części **Długość** ustawisz **Maksymalna liczba słów** (od 5 do 100), **Maksymalna liczba zdań** (1, 2, 3 lub 5), **Maksymalna liczba znaków tekstowych** (od 50 do 1000) i **Przestań czytać po** (od 5 do 60 sekund). Domyślnie odczyt nie ma tych ograniczeń.

W części **Czas** możesz:

- ustawić **Czekaj przed odczytaniem** na maksymalnie 10 sekund,
- wybrać **Gdy czytane jest inne powiadomienie** > **Czytaj po nim**, **Przerwij** lub **Nie czytaj nowego**,
- włączyć **Przeczytaj ponownie, aż ekran się włączy** i ustawić powtarzanie od 1 do 30 minut dla powiadomień otrzymanych przy wygaszonym ekranie.

Domyślnie SandFly od razu czyta powiadomienia, ustawia je w kolejce po trwającym odczycie i nie powtarza ich w ustalonych odstępach.

[Głos powiadomień](#notifications-voice) możesz ustawić osobno.

### Głos powiadomień {#notifications-voice}

Otwórz **Powiadomienia** > **Czytanie powiadomień na głos** > **Głos**. Domyślnie odzywa się **Głos SandFly**. Możesz wybrać inny [profil głosowy](#speech-voice-profiles) ze swoim syntezatorem, tempem i wysokością głosu. Przycisk **Profile głosowe** pozwala tworzyć i zmieniać profile.

Opcje **Dźwięk przed powiadomieniami** i **Wykryj język** są domyślnie wyłączone. Wykrywanie języka wymaga włączenia **Automatycznie przełączaj język** w [ustawieniach mowy](#speech-languages). Oddzielny głos możesz też wybrać dla [aplikacji](#notifications-apps) lub [reguły](#notifications-rules).

Powiadomienie w tym samym głosie czeka w zwykłej kolejce mowy. W innym głosie też zaczeka na zakończenie odczytu, najwyżej 45 sekund. Jeśli włączysz **Czytaj podczas mowy SandFly innym głosem**, drugi głos może odezwać się w trakcie trwającego odczytu. Powiadomienia czekają wtedy na siebie nawzajem.

Równoległy odczyt nie działa przy dźwięku asystenta lub rozmowy ani w aplikacji, w której obowiązuje [Odczytuj tylko fokus przeniesiony przez użytkownika](#verbosity-own-speech). Kiedy działa [**Wyłączaj mowę podczas nagrywania**](#speech), powiadomienia nie są czytane. Jeśli wybrany syntezator zawiedzie, SandFly użyje własnego głosu.

### Ustawienia dla poszczególnych aplikacji {#notifications-apps}

Otwórz **Powiadomienia** > **Aplikacje**. W pozycji **Inne aplikacje** wybierz wspólne ustawienie dla aplikacji bez własnych reguł. SandFly może odczytywać ich powiadomienia, podawać tylko nazwę aplikacji albo milczeć. Początkowo odczytuje treść.

W sekcji **Aplikacje** znajdziesz aplikacje z ekranu aplikacji, pozycje z własnymi ustawieniami i nadawców z historii. Wybierz nazwę, żeby ustawić:

- **Powiadomienia**. Możesz zostawić **Jak inne aplikacje** albo nakazać odczyt, odczyt samej nazwy aplikacji, odczyt w pierwszej kolejności lub pomijanie. Powiadomienie z pierwszeństwem przerywa trwający odczyt.
- **Wypowiadana nazwa**, **Czas pomiędzy powiadomieniami aplikacji** i [**Format mowy**](#notifications-reading). Odstęp **Jak inne aplikacje** pochodzi z sekcji **Powtórzenia**.
- **Głos**. Wybierz **Jak inne aplikacje**, **Głos SandFly** lub profil głosowy.
- Ustawienia aplikacji możesz przywrócić do wartości wspólnych. Polecenie resetowania pojawia się po zmianie co najmniej jednej opcji.

### Słowa i zamienniki {#notifications-words}

Na ekranie **Powiadomienia** > **Słowa i zamienniki** są cztery listy:

- **Czytaj tylko powiadomienia zawierające**. Gdy lista jest uzupełniona, SandFly czyta powiadomienie, jeśli zawiera przynajmniej jeden wpis.
- **Nie czytaj powiadomień zawierających**. SandFly pomija takie powiadomienia.
- **Czytaj tylko nazwę aplikacji w powiadomieniach zawierających**. SandFly podaje wyłącznie nazwę nadawcy.
- **Inna wymowa**. Możesz na przykład zamienić skrót na pełną nazwę. Wypełnij **Tekst** i **Wymawiaj jako**. **Tylko całe słowa** jest początkowo włączone.

Użyj **Dodaj słowo** lub **Dodaj zamiennik**. Dla każdego wpisu możesz włączyć **Wyrażenie regularne**. Po wybraniu wpisu dostępne są polecenia **Edytuj** i **Usuń**.

### Pomijanie rodzajów powiadomień {#notifications-kinds}

W sekcji **Rodzaje powiadomień** jest domyślnie włączona opcja **Pomiń odtwarzacze multimedialne**. Możesz też pominąć stałe powiadomienia, na przykład nawigację lub minutnik, powiadomienia o działających aplikacjach, o postępie, podsumowania grup, powiadomienia o niskim priorytecie i systemowe. Te opcje są początkowo wyłączone.

### Kiedy SandFly ma czytać {#notifications-when}

Otwórz **Powiadomienia** > **Kiedy czytać**.

| Ustawienie | Domyślne działanie i dostępne zmiany |
| --- | --- |
| **Gdy ekran jest włączony**, **Gdy ekran jest wyłączony**, **Gdy telefon jest zablokowany** | SandFly czyta przy włączonym ekranie, również na ekranie blokady. Odczyt przy wyłączonym ekranie jest początkowo wyłączony. |
| **Słuchawki** | Odczyt działa ze słuchawkami i bez nich. Możesz wybrać jeden z tych przypadków. |
| **Podczas odtwarzania multimediów** | SandFly czyta. Możesz wstrzymywać multimedia na czas czytania lub wyłączyć odczyt. |
| **Nie przeszkadzać** | Domyślnie czyta dozwolone powiadomienia. Ta opcja wymaga dostępu do powiadomień. Możesz też czytać wszystkie albo żadne. |
| **Podczas dotykania ekranu** | Domyślnie odczyt zaczyna się pół sekundy po zakończeniu dotyku, najpóźniej 10 sekund od pierwszego oczekującego powiadomienia. Możesz czytać od razu albo pominąć odczyt w trakcie dotyku. |
| **Podczas rozmów** | Przełącznik włącza odczyt w czasie rozmowy. Domyślnie jest wyłączony. |
| **Nie w trybie cichym**, **Nie w trybie wibracji**, **Nie w trybie samochodowym** | Każda z opcji może wstrzymać odczyt w tym trybie. Początkowo są wyłączone. |
| **Ciche godziny** | SandFly może nie czytać między godzinami z pól **Początek** i **Koniec**. Domyślnie wyłączone, a początkowe godziny to 22:00 i 07:00. |

### Reguły {#notifications-rules}

Reguła łączy warunek z działaniem, na przykład podaje samą nazwę aplikacji po zablokowaniu telefonu. Jeśli reguła ma własny głos, przejmuje on odczyt. Reguła bez warunków obejmuje wszystkie powiadomienia.

1. Otwórz **Powiadomienia** > **Reguły**. Użyj **Dodaj regułę**, podaj nazwę i zatwierdź. Możesz też wybrać **Dodaj regułę z szablonu**.
2. W części **Kiedy** wybierz **Dodaj warunek**. Przy kilku warunkach określ, czy mają być spełnione wszystkie, czy przynajmniej jeden.
3. W części **Potem** wybierz **Dodaj akcję**. Zaznacz ją ponownie, żeby ją usunąć.

Warunek możesz edytować, odwrócić lub usunąć. Odwrócenie warunku **Ekran włączony** sprawia, że reguła działa przy wyłączonym ekranie. Regułę można wyłączyć lub usunąć.

Warunkiem może być wybrane lub dowolne urządzenie Bluetooth, podłączenie słuchawek przewodowych, nazwa sieci Wi-Fi, pora i dni, stan ekranu lub blokady, aplikacja powiadomienia lub aplikacja na pierwszym planie, tekst lub wyrażenie regularne, poziom baterii, ładowanie, orientacja pozioma, tryb cichy lub wibracyjny oraz odtwarzanie multimediów. Sprawdzenie Bluetooth w Androidzie 12 i nowszym wymaga dostępu do urządzeń w pobliżu. Sprawdzenie sieci Wi-Fi wymaga dostępu do lokalizacji. SandFly prosi o odpowiednią zgodę przy dodawaniu warunku.

Akcja może pominąć powiadomienie, odczytać nazwę aplikacji albo całą treść, także prywatną. Możesz użyć innego formatu lub głosu, zmienić limit słów, zdjąć ograniczenia długości, pozostawić emoji lub liczby w oryginalnej formie, nadać pierwszeństwo i dopuścić odczyt w cichych godzinach lub przy innych warunkach wyciszenia.

Gotowe szablony to **Tylko ze słuchawkami**, **W nocy cicho** (22:00–07:00), **Tylko przy wyłączonym ekranie**, **Tylko nazwa aplikacji, gdy telefon jest zablokowany** oraz **Krótko, gdy telefon się nie ładuje** (do 15 słów).

### Powtarzające się powiadomienia {#notifications-repeats}

W sekcji **Powtórzenia** możesz ustawić:

- **Pomijaj to samo powiadomienie przez**. Do wyboru masz odstęp od 10 sekund do godziny albo **Do ponownego uruchomienia SandFly**. **Liczby są traktowane jako takie same** obejmuje też powiadomienia różniące się tylko liczbą, na przykład procentem przesyłania.
- **Czas pomiędzy powiadomieniami aplikacji**. Ustaw od 5 sekund do 15 minut. [Każda aplikacja może mieć własny odstęp](#notifications-apps).
- **Pomijaj powtarzający się początek**. Pomija na przykład nazwę grupy powtarzaną w kolejnych wiadomościach. Opcja **Pamiętaj początek przez** ma zakres od 30 sekund do 15 minut i domyślnie wynosi minutę.

Te mechanizmy można włączać niezależnie i są początkowo wyłączone. SandFly i tak czyta powiadomienie tylko raz, jeśli aplikacja ponowi je z tym samym tytułem i tekstem w ciągu 5 sekund albo opublikuje tę samą treść jako podsumowanie grupy.

### Przerywanie odczytu powiadomień {#notifications-stop}

Otwórz **Powiadomienia** > **Przestań czytać**.

- **Potrząśnij, aby zatrzymać** reaguje na potrząśnięcie telefonem. W opcji **Intensywność wstrząsania** wybierz jeden z trzech progów. Domyślny jest średni.
- **Pomachaj, aby zatrzymać** reaguje na ruch dłoni przy czujniku zbliżeniowym. Domyślnie włączony **Tryb kieszeni** pozwala czytać także wtedy, gdy czujnik był zasłonięty już na początku odczytu.
- **Naciśnięcie obu klawiszy głośności** pozwala przerwać odczyt oboma klawiszami naraz.
- **Zatrzymaj po odrzuceniu** przerywa odczyt odrzuconego powiadomienia. Podopcja **Również wtedy, gdy powiadomienie zostanie odrzucone na smartwatchu** jest domyślnie włączona.
- **Zatrzymaj, gdy aplikacja je usunie** przerywa odczyt, gdy powiadomienie znika za sprawą aplikacji.

Główne przełączniki są domyślnie wyłączone. Odrzucanie i usuwanie wymagają dostępu do powiadomień. Opcje działają także podczas czytania podsumowania.

### Podsumowanie powiadomień i ogłaszanie godziny {#notifications-summary}

W części **Powiadomienia** > **Podsumowanie i pora** > **Podsumowanie** włącz dostęp do powiadomień, jeśli SandFly go jeszcze nie ma. Na tym ekranie możesz:

- użyć **Przeczytaj powiadomienia teraz**, gdy SandFly jest włączony,
- włączyć **Czytaj powiadomienia codziennie** i ustawić **Godzina**. Początkowo jest to 08:00,
- wybrać **Najpierw najnowsze**, wpisać **Twoje imię** do powitania lub ustawić **Przerwa między powiadomieniami** do 5 sekund.

SandFly najpierw się przywita i poda liczbę powiadomień, a następnie w głosie powiadomień poda aplikację, tytuł i tekst. Pomija stałe powiadomienia i podsumowania grup. Ustawienia aplikacji, słowa, rodzaje i reguły obowiązują również tutaj. Pominięte pozycje nie wliczają się do podsumowania.

W części **Ogłaszanie godziny** wybierz **Podawaj godzinę co**. Do wyboru jest 15 lub 30 minut, godzina albo trzy godziny. Domyślnie odczyt okresowy jest wyłączony. W polu **Format godziny** możesz użyć `{time}`, `{date}` oraz `{battery}`, czyli godziny, daty i procentu baterii. W opcji **Głos** wybierz **Głos powiadomień** (ustawienie domyślne), **Głos SandFly** albo profil.

Do punktualnego odczytu Android może wymagać zgody **Alarmy i przypomnienia**. SandFly otwiera ustawienie przy wyborze odstępu lub włączeniu codziennego podsumowania. Bez zgody system może opóźniać odczyt. Oddzielnie możesz włączyć [okresowe podawanie stanu baterii](#verbosity-announcements).

### Historia powiadomień {#notifications-history}

Na ekranie **Historia powiadomień** zobaczysz do 100 pozycji od uruchomienia SandFly. Każda zawiera godzinę nadejścia, wynik lub powód pominięcia oraz to, co zostało powiedziane. Historia jest przechowywana w pamięci. Możesz wyłączyć **Zapisz historię powiadomień** albo użyć **Wyczyść historię**.

Wybierz wpis, żeby ustawić zasady dla jego aplikacji (pomiń, czytaj samą nazwę lub czytaj najpierw). **Nie czytaj takich powiadomień** dodaje pasujące wyrażenie regularne do listy pomijania. **Kopiuj** kopiuje tytuł i treść.

### Powiadomienie testowe {#notifications-test}

Użyj **Powiadomienia** > **Wyślij powiadomienie testowe**. SandFly wyświetli własne powiadomienie po 5 sekundach. Możesz w tym czasie wygasić ekran lub otworzyć aplikację, żeby sprawdzić swoje reguły. Android musi zezwalać SandFly na wyświetlanie powiadomień.

### Automatyzacja {#notifications-automation}

Tasker, MacroDroid i inne aplikacje mogą sterować odczytem za pomocą komunikatów Androida. Najpierw włącz **Powiadomienia** > **Zezwól aplikacjom do automatyzacji**. Domyślnie zgoda jest wyłączona, bo taki komunikat może wysłać dowolna aplikacja.

W sekcji **Automatyzacja** wybierz działanie, żeby skopiować jego pełny identyfikator. Składa się z nazwy pakietu SandFly i końcówki, na przykład `.action.READ_NOTIFICATIONS`. Możesz włączyć, wyłączyć lub przełączyć odczyt, zatrzymać powiadomienie albo odczytać powiadomienia.

### Lista powiadomień SandFly {#notifications-list}

Włącz **Powiadomienia** > **Używaj listy powiadomień SandFly**, jeśli chcesz, żeby polecenie **Powiadomienia** otwierało listę czytnika. Przydaje się to na telefonach, na których systemowa lista często się odświeża, na przykład na Samsungach przy wielu powiadomieniach. Funkcja jest domyślnie wyłączona i wymaga dostępu do powiadomień.

- Stuknij dwukrotnie po ustawieniu fokusu na powiadomieniu, żeby je otworzyć. W przypadku grupy najpierw zobaczysz jej poszczególne powiadomienia.
- Dostępne są czynności aplikacji i, jeśli aplikacja go udostępnia, pole odpowiedzi z przyciskiem **Wyślij**.
- Przy powiadomieniu odtwarzacza znajdziesz stan odtwarzania i przyciski zmiany utworu, odtwarzania lub pauzy.
- Tekst dłuższy niż około 160 znaków jest skracany. Użyj **Pokaż wszystko**, a potem w razie potrzeby **Pokaż mniej**.
- **Zamknij** usuwa powiadomienie. **Wyczyść wszystko** usuwa wszystkie, które można odrzucić. **Szybkie ustawienia** otwierają panel systemowy.

Przy zablokowanym telefonie polecenie **Powiadomienia** otwiera obszar systemowy i respektuje ustawienia widoczności z ekranu blokady. Polecenie **Systemowy obszar powiadomień** otwiera go zawsze. Przydaje się też, jeśli aplikacja musi pozostać na pierwszym planie. Lista SandFly to osobny ekran i aplikacja pod nim przechodzi w tło. Na przykład okno kodu do parowania debugowania bezprzewodowego znika w takiej sytuacji.

Możesz włączyć **Wyświetlaj powiadomienia od najnowszych, bez grup**. Każde powiadomienie będzie wtedy własną pozycją, odczytywaną jako aplikacja, godzina, tytuł i tekst. **Pomijaj odtwarzacze multimediów na liście powiadomień** usuwa je też z liczby podawanej przy wybudzeniu. Obie opcje są początkowo wyłączone.

Otwórz **Szczegóły powiadomienia** i wybierz, co SandFly ma czytać. Początkowo są to **Tytuł**, **Tekst**, **Nazwa aplikacji**, **Godzina** oraz **Odtwarzanie lub wstrzymanie**. Kolejność zmienisz przez **Przenieś w górę** i **Przenieś w dół**.

### Powiadomienia, których Android nie ogłasza {#notifications-not-announced}

Android przekazuje czytnikowi zdarzenie, gdy powiadomienie daje sygnał. Może go nie przekazać w trybie Nie przeszkadzać albo przy cichej aktualizacji poprzedniego wpisu, na przykład alertu o ruchu z dzwonka. Dostęp do powiadomień pozwala SandFly odbierać te wpisy osobną drogą i stosować do nich reguły odczytu. Stałe powiadomienia i powiadomienia o postępie są pomijane w tej ścieżce.

Po zablokowaniu telefonu Android przekazuje czytnikowi jedynie publiczną wersję prywatnego powiadomienia, jeśli aplikacja ją przygotowała. Po udzieleniu dostępu SandFly może odczytać pełną treść zgodnie z ustawieniami prywatności i regułami.

### Pozostałe zachowania {#notifications-other}

- Krótkie komunikaty aplikacji wyświetlane u dołu ekranu są czytane również w trybie Nie przeszkadzać. Reguły trybu dotyczą powiadomień.
- SandFly mówi o rozwinięciu powiadomienia na ekranie blokady lub w obszarze powiadomień. Nie powtarza stanu zwinięcia przy każdym wpisie.
- Na telefonach, które po nadejściu alertu pokazują panel powiadomienia (na przykład niektórych Vivo), SandFly pomija nazwę panelu przez pierwsze 3 sekundy. Odczyta ją, jeśli w tym czasie użyjesz dotyku, klawisza, monitora brajlowskiego lub polecenia głosowego.
- Powiadomienia SandFly o aktualizacji lub zmianie gestów można odrzucić również z ekranu blokady. Stan wstrzymania czytnika i pobieranie modelu lokalnego pozostają widoczne, dopóki trwają.
- Na [zegarkach Samsung](#devices) SandFly czyta z karty nazwę aplikacji i tytuł, wiadomość, a następnie godzinę. Może odczytać treść także wtedy, gdy etykieta karty jej nie zawiera.
- Możesz też włączyć [tłumaczenie powiadomień](#translation).
