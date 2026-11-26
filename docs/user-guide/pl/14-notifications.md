## Powiadomienia {#notifications}

SandFly odczytuje nowe powiadomienia, gdy przychodzą. Może też odczytać powiadomienia z panelu na żądanie lub o ustalonej porze. Na ekranie **Powiadomienia** w ustawieniach SandFly wybierasz, które powiadomienia mają być czytane, kiedy, w jakiej postaci i jakim głosem. Możesz tam również włączyć listę powiadomień SandFly w miejsce panelu Androida.

Połączenia przychodzące są odczytywane niezależnie od ustawień powiadomień.

### Dostęp do powiadomień {#notifications-access}

Niektóre funkcje wymagają przyznania SandFly dostępu do powiadomień w ustawieniach Androida.

- Odczytywanie powiadomień, których Android nie przekazuje czytnikom ekranu. Zobacz [](#notifications-not-announced).
- Odczytywanie całej treści prywatnych powiadomień przy zablokowanym telefonie.
- **Czytaj powiadomienia dozwolone w trybie Nie przeszkadzać**. Bez dostępu przy tym ustawieniu SandFly pomija wszystkie powiadomienia, gdy tryb jest włączony.
- Zatrzymanie odczytu powiadomienia po jego zamknięciu lub usunięciu.
- Podsumowania powiadomień z panelu.
- Lista powiadomień SandFly.

Aby przyznać dostęp, wybierz **Włącz dostęp do powiadomień** na ekranie **Kiedy czytać**, **Przestań czytać** albo **Podsumowanie i pora**. Na liście, która się otworzy, włącz SandFly. Opcja pojawia się, dopóki SandFly nie ma dostępu. SandFly korzysta z dostępu tylko wtedy, gdy jest włączony.

### Włącz lub wyłącz mowę powiadomień {#notifications-speech-on-off}

Opcja **Powiadomienia** > **Odczytuj powiadomienia** włącza lub wyłącza odczyt nowych powiadomień. Domyślnie jest włączona. Połączenia przychodzące nadal są ogłaszane po jej wyłączeniu.

Odczyt powiadomień możesz także przełączać na trzy inne sposoby.

- Kafelkiem **Czytanie powiadomień na głos** w szybkich ustawieniach.
- Poleceniem **Włączanie i wyłączanie czytania powiadomień**, które możesz przypisać do gestu lub klawiszy. SandFly odczyta informację, czy czytanie jest włączone.
- Aplikacją do automatyzacji. Zobacz [](#notifications-automation).

### Polecenia dotyczące powiadomień {#notifications-commands}

W grupie **Powiadomienia** na ekranie **Polecenia** znajdziesz następujące pozycje.

- **Czytaj powiadomienia** odczytuje powiadomienia z panelu według opcji **Podsumowanie i pora**. Zobacz [](#notifications-summary).
- **Przeczytaj jeszcze raz ostatnie powiadomienie** powtarza ostatnie odczytane powiadomienie. Jeśli SandFly nie przeczytał jeszcze żadnego, usłyszysz odpowiedni komunikat.
- **Zatrzymaj czytanie powiadomienia** zatrzymuje bieżący odczyt i pomija powiadomienia czekające w kolejce.
- **Włączanie i wyłączanie czytania powiadomień** przełącza odczyt.
- **Ogłaszanie godziny włączone lub wyłączone** ustawia **Podawaj godzinę co** na **Wyłączone** albo przywraca poprzedni odstęp. Jeśli odstępu wcześniej nie ustawiono, wybiera jedną godzinę. SandFly informuje, czy podawanie godziny jest włączone.

Polecenia możesz przypisać m.in. do gestów lub skrótów klawiszowych. Zobacz [](#commands).

Jeśli użyjesz gestu, skrótu albo klawisza w trakcie odczytu, odczyt się zatrzyma, a SandFly powie, co robi to działanie.

### Zmień sposób odczytywania {#notifications-reading}

W menu **Powiadomienia** > **Czytanie powiadomień na głos** określasz, co i w jaki sposób ma odczytywać SandFly. W grupie **Co jest czytane** są dwie opcje.

- **Format mowy** określa tekst wypowiadany przy każdym powiadomieniu. Możesz użyć znaczników `{app}`, `{title}`, `{text}`, `{content}` (pełna treść), `{bigtext}`, `{subtext}`, `{ticker}`, `{category}` i `{time}`. Domyślny format to `{category}, {title}, {text}`.
- **Odczytywanie prywatnych powiadomień** określa, co SandFly powie zamiast treści prywatnego powiadomienia. Możesz użyć `{app}`, `{category}` i `{time}`. Domyślnie jest to `{app}`.

W grupie **Tekst** znajdziesz poniższe ustawienia.

- **Pomiń emoji** pomija emoji w powiadomieniach. Domyślnie opcja jest wyłączona.
- **Czytaj wielkie litery jako słowa** odczytuje wyrazy zapisane wielkimi literami w całości. Niektóre syntezatory literują je znak po znaku. Domyślnie opcja jest wyłączona.
- **Czytaj długie liczby cyfra po cyfrze** pozwala usłyszeć osobno każdą cyfrę np. kodu lub numeru telefonu. Domyślnie opcja jest wyłączona. W polu **Minimalna liczba cyfr w długiej liczbie** ustaw próg od trzech do dziesięciu cyfr. Domyślna wartość to pięć.
- W opcji **Linki** wybierz **Czytaj cały link** (domyślnie), **Czytaj tylko nazwę witryny**, **Wypowiedz poniższy tekst** lub **Pomiń**. Tekst dla opcji **Wypowiedz poniższy tekst** podasz w polu **Tekst zamiast linku**.
- **Pomiń powiadomienia bez tekstu** wyłącza odczyt takich powiadomień. Domyślnie opcja jest wyłączona.

W grupie **Długość** ustawisz poniższe limity. Domyślnie wszystkie są wyłączone.

- **Maksymalna liczba słów** ogranicza odczyt do 5, 10, 15, 20, 30, 50 lub 100 słów.
- **Maksymalna liczba zdań** ogranicza odczyt do 1, 2, 3 lub 5 zdań.
- **Maksymalna liczba znaków tekstowych** ogranicza odczyt do 50, 100, 200, 300, 500 lub 1000 znaków.
- **Przestań czytać po** zatrzymuje odczyt po wybranym czasie od pięciu do 60 sekund.

W grupie **Czas** znajdziesz poniższe opcje.

- **Czekaj przed odczytaniem** opóźnia odczyt powiadomienia o maksymalnie dziesięć sekund. Domyślnie jest wyłączone.
- **Gdy czytane jest inne powiadomienie** określa, co zrobić, jeśli w trakcie odczytu pojawi się nowe powiadomienie. Wybierz **Czytaj po nim** (domyślnie), **Przerwij** albo **Nie czytaj nowego**.
- **Przeczytaj ponownie, aż ekran się włączy** powtarza co 1 do 30 minut powiadomienia, które przyszły przy wygaszonym ekranie. Powtarzanie ustaje po włączeniu ekranu. Domyślnie opcja jest wyłączona.

O wyborze głosu przeczytasz w [](#notifications-voice).

### Głos powiadomień {#notifications-voice}

W menu **Powiadomienia** > **Czytanie powiadomień na głos** > **Głos** znajdziesz poniższe opcje.

- **Głos** pozwala czytać powiadomienia przy użyciu jednego z Twoich profili głosowych wraz z jego ustawieniami, np. syntezatorem, szybkością i wysokością mowy. Domyślna opcja to **Głos SandFly**.
- **Profile głosowe** otwierają ekran tworzenia i edycji profili. Zobacz [](#speech-voice-profiles).
- **Czytaj podczas mowy SandFly innym głosem** włącza jednoczesny odczyt różnymi głosami. Szczegóły są poniżej. Domyślnie opcja jest wyłączona.
- **Wykryj język** czyta każde powiadomienie w języku, w którym je napisano. Wymaga **Mowa** > **Automatycznie przełączaj język**. Domyślnie opcja jest wyłączona. Zobacz [](#speech-languages).
- **Dźwięk przed powiadomieniami** odtwarza sygnał przed każdym powiadomieniem. Domyślnie opcja jest wyłączona.

Aplikacje i reguły mogą mieć własny głos. Zobacz [](#notifications-apps) i [](#notifications-rules).

Przy różnych głosach odczyt działa w następujący sposób.

- Jeśli powiadomienie używa tego samego profilu głosowego co SandFly, trafia do tej samej kolejki i jest odczytywane po bieżącej wypowiedzi.
- Powiadomienie z innym głosem również czeka na koniec wypowiedzi, najwyżej 45 sekund. Dzięki temu dwa głosy nie mówią jednocześnie. Po włączeniu **Czytaj podczas mowy SandFly innym głosem** powiadomienie zostanie odczytane od razu, równocześnie z bieżącą wypowiedzią.
- **Czytaj podczas mowy SandFly innym głosem** jest pomijane, gdy w aplikacji na pierwszym planie obowiązuje **Odczytuj tylko fokus przeniesiony przez użytkownika** albo trwa odtwarzanie dźwięku asystenta lub rozmowy. Zobacz [](#verbosity-own-speech).
- Gdy działa **Wyłączaj mowę podczas nagrywania** na ekranie **Mowa**, powiadomienia pozostają ciche. Zobacz [](#speech).
- Jeśli powiadomienia przeznaczone dla różnych głosów przyjdą jednocześnie, każde poczeka na poprzednie.

Jeśli syntezator profilu głosowego nie zadziała, SandFly odczyta powiadomienie swoim głosem.

### Aplikacje {#notifications-apps}

W menu **Powiadomienia** > **Aplikacje** wybierasz sposób odczytu dla poszczególnych aplikacji.

- **Inne aplikacje** określają odczyt w aplikacjach bez własnych ustawień. Dostępne są **Czytaj** (domyślnie), **Tylko nazwa aplikacji** i **Nie czytaj**.
- **Aplikacje** zawierają alfabetyczną listę aplikacji z ekranu głównego, z własnymi ustawieniami lub z historii. Wybierz pozycję, aby zobaczyć jej ustawienia.

Dla pojedynczej aplikacji dostępne są poniższe ustawienia.

- **Powiadomienia** mogą mieć ustawienie **Jak inne aplikacje** (domyślnie), **Czytaj**, **Tylko nazwa aplikacji**, **Najpierw przeczytaj** lub **Nie czytaj**. **Tylko nazwa aplikacji** pomija treść. **Najpierw przeczytaj** przerywa odczyt poprzedniego powiadomienia.
- **Wypowiadana nazwa** to nazwa, którą SandFly odczytuje dla tej aplikacji.
- **Czas pomiędzy powiadomieniami aplikacji** to najmniejszy odstęp między dwoma odczytanymi powiadomieniami tej aplikacji. **Jak inne aplikacje** stosuje ustawienie z grupy **Powtórzenia**.
- **Format mowy** pozwala ustawić osobny format dla danej aplikacji. Zobacz [](#notifications-reading).
- W polu **Głos** wybierz **Jak inne aplikacje**, **Głos SandFly** lub wybrany profil głosowy, np. z innym syntezatorem.
- **Zresetuj** usuwa własne ustawienia aplikacji. Opcja pojawia się, gdy aplikacja ma takie ustawienia.

### Słowa i zamienniki {#notifications-words}

W menu **Powiadomienia** > **Słowa i zamienniki** znajdziesz cztery listy. Każda pozycja może być wyrażeniem regularnym.

- **Czytaj tylko powiadomienia zawierające** ogranicza odczyt do powiadomień z co najmniej jednym wpisem z listy, jeśli lista nie jest pusta.
- **Nie czytaj powiadomień zawierających** pomija powiadomienia z dowolnym wpisem z listy.
- **Czytaj tylko nazwę aplikacji w powiadomieniach zawierających** odczytuje samą nazwę aplikacji, gdy w powiadomieniu pojawi się wpis z listy.
- **Inna wymowa** zastępuje odczytywany tekst, np. „lol” na „śmiech”. Zamiennik ma pola **Tekst**, **Wymawiaj jako**, **Tylko całe słowa** (domyślnie włączone) oraz **Wyrażenie regularne**.

Aby dodać słowo, wybierz **Dodaj słowo** pod odpowiednią listą, wpisz tekst i wybierz **OK**. Aby dodać zamiennik, wybierz **Dodaj zamiennik**. Po wybraniu pozycji możesz ją zmienić przez **Edytuj** lub usunąć przez **Usuń**.

### Rodzaje powiadomień {#notifications-kinds}

W menu **Powiadomienia** > **Rodzaje powiadomień** możesz wybrać, co pomijać.

- **Pomiń odtwarzacze multimedialne** jest domyślnie włączone.
- **Pomiń stałe powiadomienia**, np. od nawigacji i minutników.
- **Pomijaj powiadomienia o działających aplikacjach**, np. o VPN lub nagrywaniu.
- **Pomiń powiadomienia o postępie**.
- **Pomiń podsumowania grup**, np. „3 nowe wiadomości”.
- **Pomiń powiadomienia o niskim priorytecie**.
- **Pomiń powiadomienia systemowe**.

Poza **Pomiń odtwarzacze multimedialne** wszystkie powyższe opcje są domyślnie wyłączone.

### Kiedy czytać {#notifications-when}

W menu **Powiadomienia** > **Kiedy czytać** ustawisz, w jakich sytuacjach SandFly odczytuje powiadomienia.

- **Gdy ekran jest włączony** jest domyślnie włączone.
- **Gdy ekran jest wyłączony** jest domyślnie wyłączone.
- **Gdy telefon jest zablokowany** jest domyślnie włączone.
- W ustawieniu **Słuchawki** wybierz **Czytaj na głos ze słuchawkami lub bez** (domyślnie), **Tylko ze słuchawkami** lub **Tylko bez słuchawek**.
- W ustawieniu **Podczas odtwarzania multimediów** wybierz **Czytaj** (domyślnie), **Wstrzymaj multimedia podczas czytania** lub **Nie czytaj**.
- W ustawieniu **Nie przeszkadzać** wybierz **Nie czytaj żadnych**, **Czytaj powiadomienia dozwolone w trybie Nie przeszkadzać** (domyślnie) lub **Czytaj wszystkie**. Środkowa opcja wymaga dostępu do powiadomień.
- W ustawieniu **Podczas dotykania ekranu** wybierz **Czytaj od razu**, **Czytaj po zakończeniu dotyku** (domyślnie) lub **Nie czytaj**. Przy **Czytaj po zakończeniu dotyku** powiadomienia z czasu dotykania są odczytywane pół sekundy po uniesieniu palca, jednak najpóźniej dziesięć sekund od nadejścia pierwszego z nich.
- **Podczas rozmów** włącza odczyt w trakcie rozmowy. Domyślnie opcja jest wyłączona.
- **Nie w trybie cichym** jest domyślnie wyłączone.
- **Nie w trybie wibracji** jest domyślnie wyłączone.
- **Nie w trybie samochodowym** pomija odczyt np. w Android Auto, które samo czyta powiadomienia. Domyślnie opcja jest wyłączona.
- **Ciche godziny** wyłączają odczyt między godzinami **Początek** i **Koniec**. Domyślnie opcja jest wyłączona, a ustawione godziny to 22:00 i 07:00.

### Reguły {#notifications-rules}

W menu **Powiadomienia** > **Reguły** możesz zmienić odczyt powiadomień w określonych sytuacjach. Reguła składa się z warunków i czynności. Jeśli warunki są spełnione, SandFly stosuje do powiadomienia zapisane czynności. Reguła bez warunków dotyczy wszystkich powiadomień. Głos wybrany w regule zastępuje głos ustawiony dla aplikacji.

1. Wybierz **Dodaj regułę**, wpisz nazwę i wybierz **OK**. Możesz też użyć **Dodaj regułę z szablonu** i wybrać szablon.
2. W sekcji **Kiedy** wybierz **Dodaj warunek** i ustaw warunek.
3. W sekcji **Potem** wybierz **Dodaj akcję** i ustaw czynność.

Na ekranie reguły są następujące opcje.

- **Nazwa** pozwala zmienić nazwę reguły.
- **Włączona** przełącza regułę.
- W polu **Warunki** wybierz **Wszystkie warunki muszą być spełnione** lub **Musi być spełniony co najmniej jeden warunek**.
- **Kiedy** zawiera warunki. Wybierz warunek, a następnie **Edytuj**, **Odwróć warunek** lub **Usuń**. Odwrócenie sprawia, że warunek jest spełniony w sytuacji odwrotnej, np. gdy ekran jest wyłączony zamiast włączonego.
- **Potem** zawiera czynności. Możesz wybrać i usunąć każdą z nich.
- **Usuń regułę** usuwa całą regułę.

Warunki to **Urządzenie Bluetooth** z wyborem urządzenia lub **Dowolne urządzenie**, **Słuchawki przewodowe**, **Sieć Wi-Fi** według nazwy i **Pora dnia** z polami **Od**, **Do** i **Dni**. Możesz też wybrać **Ekran włączony**, **Telefon zablokowany**, **Powiadomienie z aplikacji**, **Aplikacja na pierwszym planie**, **Powiadomienie zawiera** z tekstem lub wyrażeniem regularnym, **Poziom baterii poniżej** z wartością procentową, **Ładowanie**, **Orientacja pozioma**, **Tryb cichy lub wibracyjny** albo **Odtwarzanie multimediów**. Na Androidzie 12 i nowszym **Urządzenie Bluetooth** wymaga dostępu do urządzeń w pobliżu. **Sieć Wi-Fi** wymaga dostępu do lokalizacji. Prośba o uprawnienie pojawia się przy dodaniu warunku.

Reguła może pominąć powiadomienie, odczytać samą nazwę aplikacji, odczytać całość także w przypadku prywatnego powiadomienia, użyć wybranego formatu mowy, ograniczyć liczbę słów lub pominąć limity długości. Może też zachować emoji lub nie rozdzielać cyfr, odczytać powiadomienie jako pierwsze, odczytać je mimo cichych godzin i innych ograniczeń odczytu albo użyć profilu głosowego.

Możesz skorzystać z szablonów **Tylko ze słuchawkami**, **W nocy cicho** (od 22:00 do 07:00), **Tylko przy wyłączonym ekranie**, **Tylko nazwa aplikacji, gdy telefon jest zablokowany** oraz **Krótko, gdy telefon się nie ładuje** (do 15 słów).

### Powtórzenia {#notifications-repeats}

W menu **Powiadomienia** > **Powtórzenia** możesz ustawić, jak SandFly obsługuje ponawiane i częste powiadomienia.

- **Pomijaj to samo powiadomienie przez** pomija powiadomienie, którego tekst jest taki sam jak tekst odczytany w wybranym czasie. Do wyboru są **Wyłączone** (domyślnie), odstęp od dziesięciu sekund do godziny i **Do ponownego uruchomienia SandFly**.
- **Liczby są traktowane jako takie same** uznaje za powtórzenie powiadomienia różniące się tylko liczbami, np. procentem pobierania. Domyślnie opcja jest wyłączona.
- **Czas pomiędzy powiadomieniami aplikacji** określa najkrótszy odstęp między dwoma odczytami z jednej aplikacji, od pięciu sekund do 15 minut. Domyślna wartość to **Wyłączone**. Możesz ustawić osobny odstęp dla każdej aplikacji. Zobacz [](#notifications-apps).
- **Pomijaj powtarzający się początek** pomija taki sam początek kolejnych powiadomień z aplikacji, np. nazwę grupy w komunikatorze. Domyślnie opcja jest wyłączona. W **Pamiętaj początek przez** ustaw, jak długo SandFly ma go pamiętać. Zakres wynosi od 30 sekund do 15 minut, a domyślna wartość to minuta.

Jeśli aplikacja ponownie wyśle w ciągu pięciu sekund powiadomienie z takim samym tytułem i tekstem albo wyśle ten sam tekst jako podsumowanie grupy, SandFly odczyta powiadomienie raz.

### Przerwij odczyt {#notifications-stop}

W menu **Powiadomienia** > **Przestań czytać** wybierzesz, co zatrzymuje odczyt powiadomienia.

- **Potrząśnij, aby zatrzymać** reaguje na potrząśnięcie telefonem. W ustawieniu **Intensywność wstrząsania** wybierz **Lekkie**, **Średni** (domyślnie) lub **Silny**.
- **Pomachaj, aby zatrzymać** reaguje na przesunięcie dłoni nad czujnikiem zbliżeniowym. Domyślnie włączony **Tryb kieszeni** zapobiega przerwaniu odczytu, jeśli przy rozpoczęciu czujnik był już zakryty, np. w kieszeni.
- **Naciśnięcie obu klawiszy głośności** zatrzymuje odczyt po naciśnięciu obu klawiszy naraz.
- **Zatrzymaj po odrzuceniu** zatrzymuje odczyt po odrzuceniu powiadomienia. Domyślnie włączona opcja **Również wtedy, gdy powiadomienie zostanie odrzucone na smartwatchu** stosuje tę regułę również przy odrzuceniu na połączonym zegarku.
- **Zatrzymaj, gdy aplikacja je usunie** zatrzymuje odczyt, gdy aplikacja usunie powiadomienie.

Poza **Tryb kieszeni** i **Również wtedy, gdy powiadomienie zostanie odrzucone na smartwatchu** opcje są domyślnie wyłączone. **Zatrzymaj po odrzuceniu** i **Zatrzymaj, gdy aplikacja je usunie** wymagają dostępu do powiadomień. Ustawienia działają również podczas odczytu podsumowania.

### Podsumowanie i pora {#notifications-summary}

W menu **Powiadomienia** > **Podsumowanie i pora** możesz odczytywać powiadomienia z panelu i ogłaszać godzinę w określonych odstępach. W grupie **Podsumowanie** znajdziesz poniższe opcje.

- **Włącz dostęp do powiadomień** przyznaje dostęp wymagany przez podsumowania. Opcja jest widoczna, dopóki SandFly nie ma tego dostępu.
- **Przeczytaj powiadomienia teraz** odczytuje powiadomienia z panelu. SandFly musi być włączony.
- **Czytaj powiadomienia codziennie** uruchamia odczyt każdego dnia o porze ustawionej w polu **Godzina**. Domyślnie jest to 08:00.
- **Najpierw najnowsze** rozpoczyna odczyt od najnowszego powiadomienia. Domyślnie opcja jest wyłączona.
- W polu **Twoje imię** wpisz imię używane w powitaniu, np. „Dzień dobry, Aniu”.
- W polu **Przerwa między powiadomieniami** ustaw czas do pięciu sekund lub zostaw domyślne **Wyłączone**.

Podsumowanie zaczyna się od powitania i liczby powiadomień, np. „Masz 5 powiadomień”. Potem SandFly odczytuje dla każdego aplikację, tytuł i tekst. Podsumowania grup i stałe powiadomienia są pomijane. Obowiązują ustawienia z ekranów **Aplikacje**, **Słowa i zamienniki**, **Rodzaje powiadomień** i **Reguły**. Pominięte zgodnie z nimi powiadomienia nie są wliczane do wyniku.

W grupie **Ogłaszanie godziny** znajdziesz następujące opcje.

- W polu **Podawaj godzinę co** wybierz **Wyłączone** (domyślnie), 15 minut, 30 minut, godzinę lub trzy godziny.
- **Włącz Alarmy i przypomnienia** otwiera ustawienie Androida, które pozwala ogłosić godzinę punktualnie. Bez tego Android może opóźnić odczyt. Pozycja jest widoczna, jeśli **Podawaj godzinę co** ma ustawiony odstęp, a uprawnienie jest wyłączone. Android otwiera to ustawienie również przy wyborze odstępu lub włączeniu **Czytaj powiadomienia codziennie**, jeśli uprawnienia jeszcze nie przyznano.
- **Format godziny** określa odczytywany tekst. Możesz użyć `{time}`, `{date}` i `{battery}` oznaczającego procent baterii. Domyślny format to „Jest `{time}`”.
- W polu **Głos** wybierz **Głos powiadomień** (domyślnie), **Głos SandFly** lub profil głosowy.

Podsumowania są odczytywane głosem powiadomień.

Aby ogłaszać poziom baterii co określony czas, skorzystaj z opcji **Szczegółowość** > **Poziom baterii** opisanej w [](#verbosity-announcements).

### Historia {#notifications-history}

Ekran **Powiadomienia** > **Historia powiadomień** pokazuje do 100 powiadomień od uruchomienia SandFly. Przy każdym znajdziesz godzinę nadejścia, informację o odczycie lub przyczynę pominięcia, np. tryb Nie przeszkadzać, oraz tekst odczytany przez SandFly. Historia jest przechowywana tylko w pamięci.

- **Zapisz historię powiadomień** jest domyślnie włączone.
- **Wyczyść historię** usuwa dotychczasowe wpisy.

Wybierz powiadomienie, aby wyświetlić czynności z nazwą aplikacji, np. Chat.

- Czynność „Nie czytaj Chat” ustawia dla aplikacji **Nie czytaj**.
- Czynność „Czytaj tylko nazwę Chat” ustawia **Tylko nazwa aplikacji**.
- Czynność „Czytaj Chat jako pierwszą” ustawia **Najpierw przeczytaj**.
- **Nie czytaj takich powiadomień** dodaje do listy **Nie czytaj powiadomień zawierających** wyrażenie regularne pasujące do podobnych powiadomień.
- **Kopiuj** kopiuje tytuł i treść powiadomienia.

### Sprawdź ustawienia {#notifications-test}

Opcja **Powiadomienia** > **Wyślij powiadomienie testowe** tworzy powiadomienie SandFly po pięciu sekundach. W tym czasie możesz wygasić ekran lub otworzyć inną aplikację. SandFly przeczyta je zgodnie z ustawieniami powiadomień. Potrzebuje uprawnienia do wyświetlania powiadomień.

### Automatyzacja {#notifications-automation}

Aplikacje do automatyzacji, takie jak Tasker i MacroDroid, mogą sterować odczytem przez komunikaty rozgłoszeniowe wysyłane do SandFly. Najpierw włącz **Powiadomienia** > **Zezwól aplikacjom do automatyzacji**. Domyślnie opcja jest wyłączona, ponieważ komunikat może wysłać dowolna aplikacja. W menu **Powiadomienia** > **Automatyzacja** znajdziesz listę działań i odpowiadające im akcje komunikatów. Każda akcja to nazwa pakietu SandFly i końcówka, np. `.action.READ_NOTIFICATIONS`. Wybierz działanie, aby skopiować jego akcję.

- **Włącz czytanie powiadomień na głos**.
- **Wyłącz czytanie powiadomień na głos**.
- **Włącz lub wyłącz czytanie powiadomień na głos**.
- **Zatrzymaj czytanie powiadomienia**.
- **Czytaj powiadomienia**.

### Lista powiadomień SandFly {#notifications-list}

Gdy włączysz **Powiadomienia** > **Używaj listy powiadomień SandFly**, polecenie **Powiadomienia** otworzy listę SandFly w miejsce panelu systemowego. Na niektórych telefonach, np. w urządzeniach Samsung z wieloma powiadomieniami, panel systemowy jest tak często odświeżany, że przechodzenie po nim jest powolne. Domyślnie opcja jest wyłączona. Lista wymaga dostępu do powiadomień.

Na liście możesz wykonywać poniższe czynności.

- Stuknij dwukrotnie powiadomienie, aby je otworzyć.
- Grupa stanowi jedną pozycję. Stuknij ją dwukrotnie, aby zobaczyć należące do niej powiadomienia.
- Każde powiadomienie zawiera swoje czynności. Jeśli aplikacja pozwala odpowiedzieć, jest też pole odpowiedzi z przyciskiem **Wyślij**.
- Powiadomienia o multimediach pokazują **Odtwarzanie** lub **Wstrzymano**. Udostępniają **Poprzedni utwór**, **Odtwórz** lub **Wstrzymaj** oraz **Następny utwór**.
- Powiadomienie bez tekstu, na przykład powiadomienie pogodowe z własnym układem, jest odczytywane z tekstem układu, a w przeciwnym razie z tekstem podrzędnym, tekstem informacyjnym lub tekstem podsumowania.
- Tekst dłuższy niż około 160 znaków jest skracany. **Pokaż wszystko** odczytuje całość. **Pokaż mniej** ponownie skraca tekst.
- **Zamknij** usuwa dane powiadomienie. **Wyczyść wszystko** usuwa wszystkie powiadomienia, które da się usunąć.
- **Szybkie ustawienia** otwierają panel szybkich ustawień.

Przy zablokowanym telefonie polecenie **Powiadomienia** otwiera panel systemowy. Android ukrywa w nim treści, które wybrano do ukrycia na ekranie blokady. Polecenie **Systemowy obszar powiadomień** zawsze otwiera panel systemowy. Lista SandFly otwiera się jako osobny ekran, więc aplikacja na pierwszym planie zostaje wstrzymana. Niektóre aplikacje kończą wtedy bieżącą czynność. Na przykład parowanie w debugowaniu bezprzewodowym nie powiedzie się, jeśli wpiszesz kod przy otwartej liście SandFly. W takich przypadkach użyj **Systemowy obszar powiadomień**.

Lista ma poniższe ustawienia.

- **Wyświetlaj powiadomienia od najnowszych, bez grup** pokazuje każde powiadomienie jako osobną pozycję, od najnowszego. SandFly odczytuje nazwę aplikacji, godzinę, tytuł i tekst. Domyślnie opcja jest wyłączona.
- **Pomijaj odtwarzacze multimediów na liście powiadomień** ukrywa powiadomienia odtwarzaczy. Są też pomijane przy podawaniu liczby powiadomień po włączeniu ekranu. Domyślnie opcja jest wyłączona.
- **Szczegóły powiadomienia** określają odczytywane części i ich kolejność. Do wyboru są **Tytuł**, **Tekst**, **Nazwa aplikacji**, **Godzina** oraz **Odtwarzanie lub wstrzymanie**. Każda część ma pole wyboru, domyślnie zaznaczone. Kolejność zmienisz przez **Przenieś w górę** i **Przenieś w dół**.

### Powiadomienia nieogłaszane przez Androida {#notifications-not-announced}

Android przekazuje czytnikom ekranu informacje o nowym powiadomieniu wtedy, gdy wywołuje ono alert. Może pominąć powiadomienie wyciszone przez tryb Nie przeszkadzać albo cichą aktualizację istniejącego powiadomienia. Tak się dzieje np. wtedy, gdy aplikacja dzwonka do drzwi podmienia ostatni alert o ruchu bez ponownego sygnału. Gdy SandFly ma dostęp do powiadomień, odbiera również takie powiadomienia i stosuje swoje ustawienia odczytu, w tym **Kiedy czytać** > **Nie przeszkadzać**. Powiadomienia o postępie i stałe powiadomienia są pomijane.

Przy zablokowanym telefonie Android przekazuje czytnikom tylko jawną wersję prywatnego powiadomienia. W wielu aplikacjach takiej wersji nie ma. Po przyznaniu dostępu SandFly może odczytać całe powiadomienie zgodnie z ustawieniami, m.in. **Odczytywanie prywatnych powiadomień** i **Tylko nazwa aplikacji, gdy telefon jest zablokowany**.

### Inne zachowania powiadomień {#notifications-other}

- Krótkie komunikaty wyświetlane u dołu ekranu nadal są czytane w trybie Nie przeszkadzać. Ustawienia tego trybu dotyczą tylko powiadomień.
- Na ekranie blokady i w panelu powiadomień SandFly pomija informację „zwinięte” przy każdym powiadomieniu. Po rozwinięciu powiadomienia odczytuje „rozwinięte”.
- Na telefonach, które pokazują panel przy powiadomieniu wyskakującym, np. niektórych urządzeniach Vivo, SandFly przez trzy sekundy od nadejścia powiadomienia pomija nazwę panelu. Odczyt wraca wcześniej po dotknięciu ekranu, naciśnięciu klawisza, użyciu monitora brajlowskiego lub wydaniu polecenia głosowego.
- Powiadomienia SandFly, np. o aktualizacji lub zmianie gestów, można odrzucić także na ekranie blokady. Powiadomienie o wstrzymaniu SandFly lub pobieraniu modelu na urządzenie pozostaje, dopóki trwa wstrzymanie lub pobieranie.
- Na zegarku Samsung SandFly odczytuje na karcie powiadomienia nazwę aplikacji i tytuł, potem wiadomość, a na końcu godzinę. Treść wiadomości jest czytana także wtedy, gdy brakuje jej w etykiecie karty. Zobacz [](#devices).
- O tłumaczeniu powiadomień przeczytasz w [](#translation).
