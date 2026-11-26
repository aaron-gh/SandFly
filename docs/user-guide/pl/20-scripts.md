## Skrypty i własne etykiety {#scripts}

Skrypt może zmienić działanie SandFly w jednej aplikacji lub we wszystkich, a także dodać nowe polecenia. Za pomocą skryptu możesz zmieniać etykiety i treść odczytywanych komunikatów, ukrywać elementy, dodawać czynności, gesty i skróty klawiszowe oraz uruchamiać kroki po określonym zdarzeniu na ekranie. Możesz instalować skrypty innych osób albo stworzyć własny w kreatorze bez pisania kodu.

Własna etykieta to nadana przez Ciebie nazwa elementu, takiego jak przycisk, który nie ma nazwy albo ma niewłaściwą.

Skrypty działają na telefonach i tabletach. Na zegarkach i telewizorach SandFly informuje, że skrypty są niedostępne na tym urządzeniu.

O pisaniu skryptów w Lua przeczytasz w [](#api-scripting).

### Skrypty {#scripts-overview}

Ekran **Skrypty** w ustawieniach SandFly dzieli zainstalowane skrypty na dwie grupy.

- **Skrypty aplikacji** działają tylko w aplikacjach wskazanych przez skrypt.
- **Wtyczki globalne** działają we wszystkich aplikacjach.

Przy każdym skrypcie znajdziesz informację, czy jest włączony, gdzie działa i jaką ma wersję. Wybierz skrypt, aby otworzyć jego ekran. Zobacz [](#scripts-manage).

Na ekranie **Skrypty** znajdziesz także te opcje.

- **Wyłącz wszystkie skrypty** zatrzymuje wszystkie skrypty. Zachowuje zainstalowane skrypty i ustawienia ich przełączników.
- **Nowy skrypt** otwiera kreator skryptów. Zobacz [](#scripts-builder).
- **Nowy skrypt z kodu** instaluje skrypt wpisany jako kod. Zobacz [](#scripts-code).
- **Utwórz skrypt za pomocą SandFly Pro** tworzy skrypt na podstawie Twojego opisu. Zobacz [](#scripts-builder-pro).
- **Importuj skrypt** pozwala zainstalować skrypt z pliku. Zobacz [](#scripts-install).
- **Źródła skryptów** pokazują miejsca, w których możesz znaleźć skrypty. Zobacz [](#scripts-sources).
- **Aktualizuj skrypty automatycznie** i **Sprawdź aktualizacje** służą do aktualizacji skryptów. Zobacz [](#scripts-updates).
- **Dodaj przykładowy skrypt** instaluje skrypt, który odczytuje nazwę aplikacji po przejściu do niej. Możesz użyć go jako punktu wyjścia. Opcja znika po zainstalowaniu przykładu.

Skrypty pomijają ekrany SandFly, np. ustawienia. Przestają działać, gdy wstrzymasz SandFly lub ekran zgaśnie. Przy wygaszonym ekranie nadal mogą działać skrypty, którym na to zezwolisz.

### Zainstaluj skrypt {#scripts-install}

Skrypt to plik `.lua` o rozmiarze do 1 MB albo plik `.zip` o rozmiarze do 5 MB, który zawiera `main.lua`.

1. Otwórz **Skrypty** > **Importuj skrypt**.
2. Wybierz plik `.lua` lub `.zip`.
3. Sprawdź informacje w oknie instalacji, a potem wybierz **Zainstaluj**. Zobacz [](#scripts-install-dialog).

Możesz też otworzyć lub udostępnić plik skryptu albo prowadzący do niego link `https` za pomocą opcji **Zainstaluj skrypt SandFly**. Opcja jest dostępna m.in. w komunikatorach, menedżerach plików i na liście pobranych plików przeglądarki. Przy linku z innej aplikacji SandFly pokazuje adres i zaczyna pobierać plik po wybraniu **Pobierz**. Aby udostępnić skrypt z repozytorium, otwórz sam skrypt i udostępnij jego adres. Link do całego repozytorium tu nie zadziała.

#### Znajdź skrypty w źródłach {#scripts-sources}

Źródło skryptów to publiczne repozytorium skryptów w serwisie GitHub, GitLab, Codeberg, Gitea lub Bitbucket. Listę znajdziesz w menu **Skrypty** > **Źródła skryptów**. Repozytorium skryptów SandFly jest dodane domyślnie.

1. Otwórz **Skrypty** > **Źródła skryptów** > **Dodaj źródło**.
2. Wpisz adres repozytorium, jego gałęzi lub folderu, np. `github.com/owner/repository`.
3. Wybierz **Dodaj**.

Wybierz źródło, aby wyświetlić zawarte w nim skrypty. Wybierz skrypt, aby go pobrać i otworzyć okno instalacji. Jeśli lista się nie wczyta, wybierz **Spróbuj ponownie**. **Usuń źródło** usuwa je z listy. Skrypty już zainstalowane z tego źródła zostają na urządzeniu.

#### Sprawdź skrypt przed instalacją {#scripts-install-dialog}

W oknie instalacji SandFly pokazuje poniższe informacje.

- Pochodzenie skryptu. Może to być plik, link lub źródło skryptów. Przy skrypcie dostarczonym z SandFly widnieje **Dołączony do SandFly.**
- Podpis skryptu. SandFly podpisuje skrypty z własnego repozytorium. Przy podpisanym skrypcie z dowolnego źródła widnieje **Podpisany przez SandFly.**. Przy innych pojawia się **Niepodpisany przez SandFly.**. Jeśli ktoś zmienił skrypt po podpisaniu, SandFly informuje, że podpis się nie zgadza. Nazwę autora może wpisać dowolna osoba tworząca skrypt. To podpis potwierdza jego pochodzenie z SandFly.
- **Możliwości skryptu:** lista potrzebnych uprawnień. Uprawnienia oznaczone jako przeznaczone tylko dla zaufanych skryptów dają dostęp do wrażliwych danych lub do wszystkich funkcji SandFly. Zobacz [](#scripts-permissions).
- **Dodawane polecenia:** polecenia skryptu wraz z gestami, klawiszami, pozycjami menu SandFly i sterowaniem czytaniem.

Jeśli skrypt napisano dla innej wersji SandFly, w oknie pojawi się informacja, że niektóre części mogą nie działać. Przycisk instalacji zmieni się na **Zainstaluj mimo to**.

Import zainstalowanego już skryptu aktualizuje go i zachowuje jego dane oraz ustawienia. W oknie instalacji znajdziesz numer zastępowanej wersji i nowe uprawnienia. Jeśli nowa wersja ma innego autora, pojawi się ostrzeżenie.

### Aktualizuj skrypty {#scripts-updates}

Raz na dobę SandFly sprawdza w źródłach, czy są nowsze wersje zainstalowanych skryptów. Aby sprawdzić teraz, wybierz **Skrypty** > **Sprawdź aktualizacje**.

Przy włączonej opcji **Aktualizuj skrypty automatycznie** SandFly samodzielnie instaluje wersję, która pochodzi ze źródła skryptu, ma tego samego autora, nie wymaga nowych uprawnień i pasuje do wersji SandFly. Nowsza wersja z repozytorium SandFly instaluje się automatycznie tylko wtedy, gdy ma podpis SandFly. Opcja **Aktualizuj skrypty automatycznie** jest domyślnie włączona.

Pozostałe nowsze wersje są wymienione w sekcji **Aktualizacje** na ekranie **Skrypty**. Wybierz wersję, aby otworzyć okno instalacji.

### Zarządzaj zainstalowanym skryptem {#scripts-manage}

Otwórz **Skrypty** i wybierz skrypt. Na jego ekranie znajdziesz poniższe opcje i informacje.

- **Włącz** przełącza działanie skryptu.
- **Działaj przy wyłączonym ekranie** utrzymuje działanie skryptu przy wygaszonym ekranie, np. gdy skrypt odczytuje godzinę. Opcja jest widoczna tylko w skryptach, które ją udostępniają, i jest wyłączona po instalacji.
- Nazwa, opis, wersja, autor i informacja o tym, gdzie skrypt działa.
- **Wyświetl kod** pokazuje plik `main.lua` skryptu razem z manifestem. Zobacz [](#scripts-code).
- **Edytuj kod** zmienia kod skryptu. Opcja nie jest widoczna w skryptach utworzonych w kreatorze. Zobacz [](#scripts-code).
- **Edytuj w kreatorze skryptów** dla skryptów utworzonych w kreatorze albo **Kopiuj do kreatora skryptów** dla pozostałych. Zobacz [](#scripts-builder-manage).
- **Zmień za pomocą SandFly Pro** jest widoczne dla skryptu utworzonego w kreatorze, gdy masz SandFly Pro. Zobacz [](#scripts-builder-pro).
- **Zgodność** jest widoczna, gdy skrypt napisano dla innej wersji SandFly.
- **Otwórz stronę domową** otwiera stronę skryptu po potwierdzeniu adresu.
- **Ustawienia** otwierają ustawienia skryptu, jeśli je udostępnia.
- **Uprawnienia** zawierają przełącznik dla każdego uprawnienia wymaganego przez skrypt. Zobacz [](#scripts-permissions).
- **Polecenia** pokazują polecenia skryptu oraz ich gesty i klawisze. Zobacz [](#scripts-commands).
- **Uruchamiaj wcześniej** i **Uruchamiaj później** zmieniają kolejność skryptów, które modyfikują ten sam komunikat. Skrypty aplikacji działają jako pierwsze.
- **Dziennik** pokazuje dane wyjściowe i błędy skryptu od uruchomienia SandFly. Zobacz [](#scripts-log).
- **Wyczyść zapisane dane** usuwa dane zapisane przez skrypt.
- **Usuń** usuwa skrypt wraz z jego kodem, danymi i ustawieniami po potwierdzeniu.

#### Wyświetl i edytuj kod skryptu {#scripts-code}

**Wyświetl kod** na ekranie skryptu pokazuje jego plik `main.lua` z manifestem na początku jako tekst, który możesz czytać po wierszach, słowach i znakach oraz zaznaczać.

**Edytuj kod** na ekranie skryptu, który nie powstał w kreatorze, otwiera ten sam plik w polu tekstowym. **Zapisz** sprawdza kod tak jak **Importuj skrypt** sprawdza plik skryptu i zastępuje kod zainstalowanego skryptu. Skrypt zachowuje przełącznik, uprawnienia, gesty, klawisze, ustawienia i zapisane dane, a potem działa z nowym kodem. Wartość `id` w manifeście musi pozostać taka sama. Jeśli kod wymaga uprawnień, których skrypt wcześniej nie miał, najpierw wyświetli je okno instalacji. Zobacz [](#scripts-install-dialog). Pozostałe pliki skryptu z pliku `.zip` pozostają bez zmian.

**Nowy skrypt z kodu** na ekranie **Skrypty** otwiera puste pole tekstowe. Wpisz cały plik `main.lua` z `local manifest = { ... }` przed kodem i wybierz **Zapisz**. SandFly sprawdza kod tak jak plik skryptu i wyświetla okno instalacji. Manifest z `id` zainstalowanego skryptu aktualizuje ten skrypt.

Jeśli kodu nie można zainstalować, **Nie zapisano skryptu** podaje przyczynę, a kod pozostaje otwarty do zmiany. Jeśli wrócisz z niezapisanymi zmianami, otworzy się okno **Odrzucić zmiany?**. Wybierz **Odrzuć** lub **Edytuj dalej**.

Gdy opcja **Aktualizuj skrypty automatycznie** jest włączona, nowsza wersja ze źródła skryptu zastępuje kod zmieniony przez **Edytuj kod**. Zobacz [](#scripts-updates).

Pisanie skryptów opisuje [](#api-scripting).

#### Uprawnienia skryptu {#scripts-permissions}

Skrypt może korzystać tylko z uprawnień, które wymienia. Na jego ekranie w sekcji **Uprawnienia** możesz wyłączyć każde z nich. Wyłączenie zatrzymuje te części skryptu, które go potrzebują.

Skrypt może prosić o następujące uprawnienia.

- **Odczytywanie ekranu**.
- **Klikanie, przewijanie, wpisywanie tekstu i przenoszenie fokusu**.
- **Dostęp do wypowiedzi SandFly i ich zmienianie**.
- **Odczytywanie pól haseł**.
- **Dodawanie gestów, skrótów klawiszowych, pozycji menu i opcji sterowania czytaniem oraz uruchamianie poleceń SandFly**.
- **Korzystanie z internetu**.
- **Odczytywanie i zmienianie zawartości schowka**.
- **Cofanie, przechodzenie do ekranu głównego, otwieranie powiadomień, wyświetlanie listy aplikacji, otwieranie aplikacji, ustawień i linków, wysyłanie intencji do aplikacji, zmiana głośności, trybu dzwonka i ustawień urządzenia, takich jak Wi-Fi i latarka, sterowanie multimediami, ustawianie timerów i alarmów oraz rozpoczynanie połączeń i wiadomości**.
- **Odczytywanie i zmienianie treści odczytywanych powiadomień**.
- **Śledzenie wszystkich zdarzeń w każdej aplikacji, w tym wpisywania tekstu**.
- **Wyświetlanie okien dialogowych nad dowolną aplikacją**.
- **Uruchamianie kodu Java z pełnym dostępem SandFly, w każdej aplikacji**. Inne wyłączone uprawnienia nie ograniczają skryptu, który może uruchamiać kod Java.

Zezwalaj na dostęp do internetu, schowka i haseł, czynności systemowe, odczyt powiadomień, śledzenie zdarzeń, otwieranie okien oraz uruchamianie kodu Java tylko skryptom z zaufanego źródła.

#### Polecenia skryptów {#scripts-commands}

Skrypt może dodawać polecenia, które uruchomisz gestem, skrótem klawiszowym, z menu SandFly lub za pomocą sterowania czytaniem. Polecenia zainstalowanych skryptów są też w grupie **Skrypty** na ekranie **Polecenia**. W nazwie polecenia najpierw widnieje nazwa skryptu. Przypiszesz je tak samo jak pozostałe polecenia. Zobacz [](#commands).

Aby zmienić gesty lub klawisze polecenia skryptu, wykonaj poniższe czynności.

1. Otwórz **Skrypty**, wybierz skrypt, a pod nagłówkiem **Polecenia** wybierz polecenie.
2. Skorzystaj z jednej z dostępnych opcji.
   - Wybierz gest lub klawisz z listy. **Wyłącz** pozostawia dla niego polecenie SandFly. **Usuń** usuwa przypisanie do polecenia skryptu.
   - **Wykonaj gest lub naciśnij skrót** zapisuje następny wykonany gest albo klawisze naciśnięte z klawiszem SandFly.
   - **Wybierz gest** otwiera listę gestów.
   - **Wpisz skrót klawiszowy** pozwala wpisać klawisze, np. `sf+shift+n`, `sf+t`, `s` lub `shift+n`.
   - **Przywróć domyślne przypisania** przywraca gesty i klawisze skonfigurowane w skrypcie. Opcja pojawia się po ich zmianie.

Podczas działania skryptu jego gesty i klawisze mają pierwszeństwo przed przypisaniami SandFly. Niektóre skrypty są wczytywane tylko w celu wykonania polecenia. Ich polecenia znajdziesz wyłącznie na ekranie **Polecenia**. Jeśli spróbujesz wywołać polecenie skryptu, który nie działa na bieżącym ekranie, SandFly odczyta komunikat o tym.

#### Dziennik i błędy skryptu {#scripts-log}

Opcja **Dziennik** na ekranie skryptu pokazuje wyniki i błędy od uruchomienia SandFly. Przy każdym wpisie jest czas. **Kopiuj dziennik** kopiuje zawartość, aby można ją było np. wysłać autorowi skryptu. **Wyczyść dziennik** usuwa wpisy.

Jeśli błędy się powtarzają, SandFly wyłącza skrypt, podaje jego nazwę i odsyła do dziennika na ekranie **Skrypty**.

### Wyłącz skrypty {#scripts-turn-off}

- Aby wyłączyć jeden skrypt, wyłącz przełącznik **Włącz** na ekranie tego skryptu.
- Aby wyłączyć wszystkie skrypty, włącz **Skrypty** > **Wyłącz wszystkie skrypty**. Poszczególne przełączniki zostają zapamiętane i zadziałają po ponownym włączeniu skryptów.

Polecenie **Włącz lub wyłącz wszystkie skrypty** z grupy **SandFly** przełącza to samo ustawienie co **Wyłącz wszystkie skrypty** i odczytuje aktualny stan. Skorzystaj z niego, jeśli skrypt uniemożliwia obsługę aplikacji. Zobacz [](#commands).

### Utwórz skrypt w kreatorze {#scripts-builder}

W kreatorze możesz utworzyć skrypt bez pisania kodu.

1. Otwórz **Skrypty** > **Nowy skrypt**.
2. Wpisz nazwę i wybierz **OK**.

Otworzy się kreator. Każda zmiana jest od razu zapisywana i stosowana. Części skryptu są podzielone na grupy.

- **Nazwa** to nazwa skryptu.
- **Aplikacje** określają, gdzie działa skrypt. Gdy lista jest pusta, obowiązuje ustawienie **Wszystkie aplikacje**. Aby dodać aplikację, wybierz **Dodaj aplikację**. W obrębie aplikacji możesz ograniczyć skrypt przez **Tylko w aktywności** lub **Tylko w oknie**.
- **Reguły** zmieniają elementy. Zobacz [](#scripts-builder-rules).
- **Sterowanie czytaniem** zawiera pozycje przechodzące po określonym rodzaju elementów. Wybierz **Dodaj opcję sterowania czytaniem** i wskaż element, na który ma przechodzić.
- **Polecenia** zawierają kroki uruchamiane gestem, klawiszami, z menu SandFly lub za pomocą sterowania czytaniem. Zobacz [](#scripts-builder-commands).
- **Wyzwalacze** zawierają kroki, które uruchamiają się automatycznie. Zobacz [](#scripts-builder-triggers).
- **Zmiany mowy** określają, co odczytuje SandFly. Zobacz [](#scripts-builder-speech).
- **Ustawienia**, **Informacje** i **Kod** dotyczą konfiguracji i treści skryptu. Zobacz [](#scripts-builder-settings).
- W grupie **Skrypt** znajdziesz **Eksportuj skrypt** i **Duplikuj skrypt**. Zobacz [](#scripts-builder-manage).

Opcja **Strona skryptu** otwiera ekran skryptu z przełącznikiem, uprawnieniami i dziennikiem. Przy niedokończonej części pojawia się informacja o brakujących danych, np. o konieczności wskazania elementu. Dopóki jej nie uzupełnisz, ta część jest pomijana.

#### Reguły {#scripts-builder-rules}

Reguła wybiera element i określa, jak go zmienić. Wybierz **Dodaj regułę**, aby utworzyć nową.

W sekcji **Element** określasz, do których elementów stosować regułę. Dostępne są te same warunki co w makrze. Możesz dopasować tekst przez **Dopasuj w** i **Wyrażenie regularne**, a także użyć typu elementu oraz pól **Dokładny tekst**, **Identyfikator zasobu**, **Identyfikator zasobu elementu nadrzędnego**, **Nazwa klasy**, **Rola**, **Stany**, **Wewnątrz elementu** i **Zawiera element**. Zobacz [](#macros). Możesz też wybrać **Użyj sprawdzonego elementu**. Wtedy pola zostaną uzupełnione danymi elementu ostatnio sprawdzonego poleceniem **Sprawdź element z fokusem** lub **Kopiuj szczegóły elementu**. Zobacz [](#scripts-inspect).

W sekcji **Zmiany** wybierasz działanie reguły.

- **Etykieta**, **Odczytuj zamiast tego**, **Wskazówka użytkowania**, **Stan**, **Rola** i **Nagłówek** zmieniają właściwości elementu.
- W polu **Ukryj** możesz wybrać **Ukryj element** lub **Ukryj element i elementy w nim**.
- **Wycisz zmiany** pomija odczyt zmian elementu, a w razie potrzeby również jego zawartości.
- **Czytaj jako jeden element** odczytuje element wraz z zawartością jako jedną pozycję.
- W polu **Kolejność czytania** użyj **Czytaj przed elementem** lub **Czytaj po elemencie**.
- **Kliknięcie dwukrotne aktywuje** pozwala wskazać inny element do aktywowania. Są tu też opcje **Aktywowanie po puszczeniu**, **Przytrzymanie po (ms)** i **Aktywuj dotknięciem ekranu**.
- **Zmień mowę** usuwa, zastępuje lub przestawia części wypowiedzi, którą SandFly odczytuje przy elemencie. Zobacz [](#scripts-builder-speech).
- **Ustal, co jest czytane** określa części wypowiedzi i ich kolejność. Za pomocą **Dodaj część** dodasz jeden z tekstów elementu, własny tekst lub element wewnętrzny.

Sekcja **Czynności** dodaje pozycje do menu czynności elementu. Wybierz **Dodaj czynność**. W polu **Działanie** możesz wybrać **Kliknij element**, **Kliknij element z przytrzymaniem**, **Przenieś fokus na element**, **Przewiń element do przodu**, **Przewiń element do tyłu**, **Odczytaj tekst** lub **Odczytaj tekst elementu**. Ustal też, czy pozycja ma być przed czynnościami aplikacji, czy za nimi. Opcja **Przyciski w środku jako czynności** zamienia przyciski wewnątrz elementu na czynności. W polu **Przyciski do pominięcia** wybierzesz wyjątki.

Możesz też nagrać czynność.

1. W sekcji **Czynności** wybierz **Nagraj czynność**. Otworzy się aplikacja skryptu.
2. Naciśnij lub naciśnij i przytrzymaj wybrane elementy. Możesz to zrobić za pomocą SandFly albo dotykiem.
3. Otwórz powiadomienie o nagrywaniu czynności.
4. Wybierz **Zapisz** lub **Nagraj ponownie**.

Jeśli reguła wskazuje sprawdzony element, sekcja **Sprawdzony element** udostępnia **Czytaj jako jeden element, z jego przyciskami jako czynnościami** oraz **Wybierz, co jest czytane**. Druga opcja pokazuje części treści odczytywane przez SandFly. Odznacz fragmenty, które mają być pomijane w każdym podobnym elemencie. W polu **Reguła dla** wybierz **Każdy wiersz podobny do tego, w którym jest** lub **Każdy element podobny do sprawdzonego elementu**.

**Duplikuj regułę** tworzy kopię w tym samym skrypcie. **Kopiuj regułę do skryptu** przenosi kopię do innego skryptu utworzonego w kreatorze. **Usuń regułę** usuwa regułę.

#### Polecenia {#scripts-builder-commands}

Opcja **Dodaj polecenie** tworzy polecenie o podanej nazwie. Na jego ekranie znajdziesz poniższe ustawienia.

- **Kroki** zawierają czynności polecenia. Edytuje się je tak samo jak kroki makra. Zobacz [](#macros).
- **Gesty i skrót klawiszowy** służą do ustawienia przypisań na tym telefonie. Zobacz [](#scripts-commands).
- **Domyślne przy udostępnianiu** określa przypisania nadawane poleceniu przy instalowaniu skryptu. Możesz ustawić gest, klawisze, **W menu SandFly** i **Jako opcja sterowania czytaniem**.
- **Zmień nazwę** i **Usuń** pozwalają zmienić nazwę lub skasować polecenie.

#### Wyzwalacze {#scripts-builder-triggers}

Opcja **Dodaj wyzwalacz** tworzy kroki wykonywane samoczynnie. Wybierz zdarzenie, które ma je uruchamiać.

- **Gdy fokus przejdzie na element**.
- **Gdy aplikacja się otworzy** lub **Gdy aplikacja się zamknie**.
- **Gdy zmieni się okno**.
- **Gdy komunikat zawiera tekst** lub **Gdy powiadomienie zawiera tekst**.
- **Gdy zmieni się tekst**, **Gdy zmieni się ekran**, **Gdy lista się przewinie**, **Gdy kursor się przesunie**, **Gdy element zostanie kliknięty**, **Gdy element zostanie kliknięty z przytrzymaniem** lub **Gdy pole edycji otrzyma fokus wprowadzania**. Możesz wybrać dowolny albo konkretny element.
- **Co określoną liczbę minut** lub **Codziennie o określonej godzinie**.
- **Gdy zostanie naciśnięty przycisk ustawienia**.

Wyzwalacze czasowe działają tylko wtedy, gdy skrypt jest uruchomiony.

#### Zmiany mowy {#scripts-builder-speech}

Opcja **Dodaj zmianę mowy** pozwala zmienić to, co odczytuje SandFly. W polu **Zmieniana mowa** wybierz **Mowa dla elementu**, **Powiadomienia** (ewentualnie z wybraną pozycją **Aplikacja**), **Komunikaty** lub **Cała mowa**. Pole **Zawiera tekst (puste: dowolny)** zawęża dopasowanie do podanego tekstu, jeśli je uzupełnisz.

W sekcji **Edycje** zmiany wykonują się w kolejności na liście. Dostępne są **Usuń tekst**, **Zastąp tekst**, **Usuń powtarzające się części**, **Usuń części zawierające tekst**, **Zachowaj pierwsze części**, **Powiedz zamiast tego**, **Powiedz przed**, **Powiedz po** i **Nic nie mów**. Części wypowiedzi są oddzielone przecinkami. W ustawieniach **Powiedz zamiast tego**, **Powiedz przed** i **Powiedz po** zapis `{text}` oznacza tekst, który SandFly normalnie by wypowiedział. Użyj **Podgląd na sprawdzonym elemencie**, aby zobaczyć wynik na ostatnim sprawdzonym elemencie.

#### Ustawienia, informacje i kod {#scripts-builder-settings}

- **Ustawienia** > **Dodaj ustawienie** dodaje na ekranie skryptu element typu **Przełącznik**, **Lista**, **Tekst**, **Liczba** albo **Przycisk**. Kroki mogą korzystać z wartości ustawienia. Przycisk uruchamia wyzwalacz.
- **Informacje** zawierają **Opis**, **Wersja**, **Strona główna**, **Wczytuj tylko do uruchomienia polecenia** oraz **Działaj przy wyłączonym ekranie**.
- **Kod** zawiera pole **Kod Lua** i pozycję **Dodatkowe uprawnienia**, wymagane przez ten kod. Kod jest dołączany do końca skryptu. SandFly sprawdza, czy nie ma w nim błędów, przed zapisem.

#### Eksportuj, duplikuj i kopiuj skrypty {#scripts-builder-manage}

- **Eksportuj skrypt** w kreatorze zapisuje skrypt jako plik `.zip`. Zainstaluj go przez **Importuj skrypt**. Po imporcie nadal można go zmieniać w kreatorze.
- **Duplikuj skrypt** tworzy kopię.
- **Kopiuj do kreatora skryptów** na ekranie skryptu, który powstał poza kreatorem, tworzy kopię do edycji w kreatorze.
- **Edytuj w kreatorze skryptów** na ekranie skryptu utworzonego w kreatorze otwiera go do edycji.

#### Utwórz skrypt za pomocą SandFly Pro {#scripts-builder-pro}

Jeśli korzystasz z SandFly Pro, wybierz **Utwórz skrypt za pomocą SandFly Pro** na ekranie **Skrypty** i opisz, co skrypt ma robić. Opcja **Zmień za pomocą SandFly Pro** na ekranie gotowego skryptu i w kreatorze pozwala zmienić istniejący skrypt. SandFly Pro odczytuje bieżący ekran i testuje skrypt. W powiadomieniu mogą się pojawić instrukcje otwarcia kolejnych ekranów lub wypróbowania części skryptu. Po wykonaniu instrukcji wybierz **Ukończono** lub **Odpowiedz** w powiadomieniu.

Skrypt otworzy się w kreatorze w sekcji **Napisany przez SandFly Pro**. Zobaczysz podsumowanie i przyciski **Akceptuj**, **Poproś o zmiany** i **Odrzuć**. Do chwili akceptacji skrypt działa bez uprawnień do korzystania z internetu i uruchamiania kodu Java. Utworzenie skryptu zużywa znacznie więcej miesięcznego limitu SandFly Pro niż pozostałe zadania. Zobacz [](#pro-scripts).

### Sprawdź elementy ekranu {#scripts-inspect}

Poniższe polecenia pokazują szczegóły, po których skrypty i makra rozpoznają elementy. Znajdziesz je w grupie **Ustawienia programisty** na ekranie poleceń. Domyślnie nie mają przypisań. Możesz dodać je do menu SandFly. Zobacz [](#commands).

- **Sprawdź element z fokusem** otwiera okno **Szczegóły elementu** z sekcjami **Teksty**, **Typy elementu**, **Elementy wewnątrz**, **Szczegóły dla skryptów** i **Aplikacja**. Stuknij dwukrotnie wiersz, aby skopiować jego wartość. **Kopiuj wszystko** kopiuje wszystkie szczegóły. **Dodaj regułę dla tego elementu** dodaje regułę do skryptu utworzonego w kreatorze albo do nowego skryptu dla aplikacji i otwiera ją do edycji.
- **Kopiuj szczegóły elementu** kopiuje te same dane jako tekst i odczytuje potwierdzenie.
- **Kopiuj drzewo ekranu** kopiuje wszystkie elementy ekranu jako tekst, po jednym w wierszu, i ogłasza liczbę skopiowanych elementów.

Jeśli żaden element nie ma fokusu, SandFly odczyta odpowiedni komunikat.

### Etykiety niestandardowe {#scripts-labels}

Własna etykieta zastępuje nazwę elementu, np. przycisku bez etykiety. SandFly odczytuje etykietę za każdym razem, gdy czyta ten element. Etykiety możesz dodawać do elementów mających w aplikacji identyfikator zasobu.

#### Dodaj lub zmień etykietę {#scripts-labels-add}

1. Przenieś fokus na element.
2. Użyj polecenia **Dodawanie i edytowanie etykiet** z grupy **Aktywacja**. Zobacz [](#commands).
3. W oknie **Dodaj etykietę** lub **Edytuj etykietę** wpisz etykietę i wybierz **OK**.

Aby usunąć etykietę, uruchom **Dodawanie i edytowanie etykiet** dla odpowiedniego elementu, a następnie wybierz **Usuń etykietę**. Jeśli elementu nie da się oznaczyć, SandFly odczyta odpowiedni komunikat.

Aby dodawać etykiety z menu SandFly, dodaj **Dodawanie i edytowanie etykiet** na ekranie **Menu SandFly**. Domyślnie polecenia tam nie ma. Menu będzie odtąd zawierało pozycję **Etykiety** dla elementów, którym można nadać etykietę. Gdy fokus jest na elemencie bez etykiety, SandFly informuje, że można ją dodać.

#### Zarządzaj własnymi etykietami {#scripts-labels-manage}

Na ekranie **Etykiety niestandardowe** w ustawieniach SandFly znajdziesz aplikacje z własnymi etykietami. Wybierz aplikację, aby zobaczyć etykiety i czas ostatniej zmiany każdej z nich. Wybierz etykietę, aby ją zmienić lub usunąć.

#### Eksportuj i importuj własne etykiety {#scripts-labels-export}

Opcja **Kopia zapasowa i przywracanie** > **Eksportuj ustawienia** zapisuje także własne etykiety. Zobacz [](#profiles-export). Jeśli chcesz udostępnić same etykiety, wyeksportuj je do osobnego pliku.

- **Eksportuj niestandardowe etykiety** zapisuje wszystkie własne etykiety w pliku i otwiera panel udostępniania Androida. Wybierzesz w nim miejsce wysłania lub zapisu pliku. Jeśli nie ma żadnych własnych etykiet, SandFly informuje, że nie ma czego wyeksportować.
- **Importuj niestandardowe etykiety** odczytuje plik etykiet. Jeśli etykieta z pliku i etykieta na telefonie dotyczą tego samego elementu, w oknie **Zaimportuj niestandardowe etykiety do SandFly** wybierz **Zastąp**, aby użyć etykiety z pliku, albo **Pomiń**, aby zostawić własną.
- **Wycofaj zaimportowane etykiety** usuwa etykiety z importu i przywraca etykiety, które import zastąpił. Opcja pojawia się po imporcie.
