## Skrypty i własne etykiety {#scripts}

Skrypt może zmienić działanie SandFly w wybranych aplikacjach albo w całym systemie. Może na przykład nazwać przycisk, ukryć element, zmienić wypowiedź, dodać polecenie lub zareagować na zmianę ekranu. Możesz instalować gotowe skrypty, tworzyć je w kreatorze albo [pisać w Lua](#api-scripting).

Jeśli potrzebujesz tylko nadać nazwę elementowi, użyj [etykiety niestandardowej](#scripts-labels). Skrypty działają na telefonach i tabletach. Zegarki i telewizory ich nie obsługują.

### Ekran Skrypty {#scripts-overview}

Otwórz **Skrypty** w ustawieniach. **Skrypty aplikacji** działają w wybranych aplikacjach, a **Wtyczki globalne** w całym systemie. Przy każdym skrypcie zobaczysz stan, wersję i miejsce działania. Wybierz go, żeby [otworzyć jego ustawienia](#scripts-manage).

Z ekranu głównego możesz [wyłączyć wszystkie skrypty](#scripts-turn-off), [utworzyć nowy](#scripts-builder), [użyć SandFly Pro](#scripts-builder-pro), [importować](#scripts-install) lub [poszukać gotowych skryptów](#scripts-sources). Są tu też [opcje aktualizacji](#scripts-updates). **Dodaj przykładowy skrypt** instaluje przykład, który podaje nazwę aplikacji po przełączeniu. Przycisk znika po instalacji.

Skrypty nie zmieniają ekranów SandFly. Zatrzymują się po wstrzymaniu czytnika. Po wyłączeniu ekranu mogą działać dalej tylko skrypty, którym na to zezwoliłeś.

### Instalowanie skryptu {#scripts-install}

SandFly przyjmuje pliki `.lua` do 1 MB i archiwa `.zip` do 5 MB zawierające `main.lua`.

1. Otwórz **Skrypty** > **Importuj skrypt**.
2. Wybierz plik.
3. [Przejrzyj dane](#scripts-install-dialog) i wybierz **Zainstaluj**.

W menedżerze plików, przeglądarce lub komunikatorze możesz otworzyć lub udostępnić plik albo link HTTPS przez **Zainstaluj skrypt SandFly**. Przy linku SandFly wyświetli adres i pobierze plik dopiero po wybraniu **Pobierz**. Udostępnij link do konkretnego skryptu. Adres całego repozytorium dodaje się jako źródło.

#### Źródła skryptów {#scripts-sources}

Źródło to publiczne repozytorium w GitHubie, GitLabie, Codebergu, Gitei lub Bitbuckecie. Oficjalne repozytorium SandFly jest już na liście.

1. Otwórz **Skrypty** > **Źródła skryptów** > **Dodaj źródło**.
2. Podaj adres repozytorium, gałęzi lub katalogu, na przykład `github.com/owner/repository`.
3. Wybierz **Dodaj**, potem otwórz źródło i wybierz skrypt.

Jeśli lista się nie otwiera, użyj **Spróbuj ponownie**. **Usuń źródło** usuwa adres z listy. Skrypty zainstalowane wcześniej pozostają na urządzeniu.

#### Informacje przed instalacją {#scripts-install-dialog}

Okno pokazuje źródło, podpis, wymagane uprawnienia oraz polecenia, gesty i skróty, które skrypt doda. Nazwę autora wpisuje właściciel skryptu. Jeżeli chcesz potwierdzić pochodzenie z SandFly, sprawdź komunikat **Podpisany przez SandFly**. Czytnik podaje osobno brak podpisu lub niezgodność podpisu po zmianie pliku.

Przeczytaj **Możliwości skryptu** i [uprawnienia](#scripts-permissions). Przy wersji przeznaczonej do innego wydania możesz użyć **Zainstaluj mimo to**. Niektóre funkcje mogą wtedy nie działać.

Import tej samej pozycji aktualizuje skrypt i zachowuje dane oraz ustawienia. Okno podaje zastępowaną wersję i nowe uprawnienia. SandFly informuje też o zmianie autora.

### Aktualizowanie skryptów {#scripts-updates}

SandFly sprawdza źródła raz dziennie. Aby zrobić to od razu, otwórz **Skrypty** > **Sprawdź aktualizacje**.

Domyślnie działa **Aktualizuj skrypty automatycznie**. SandFly instaluje nową wersję, jeśli pochodzi z tego samego źródła, ma tego samego autora, jest zgodna z wydaniem SandFly i nie wymaga nowych uprawnień. Przy oficjalnym repozytorium potrzebny jest także podpis SandFly. Pozostałe aktualizacje wybierasz samodzielnie w sekcji **Aktualizacje**.

### Ustawienia zainstalowanego skryptu {#scripts-manage}

Otwórz **Skrypty** i wybierz nazwę. Na stronie skryptu znajdziesz:

- **Włącz** i, jeśli skrypt to przewiduje, **Działaj przy wyłączonym ekranie**. Drugi przełącznik jest początkowo wyłączony.
- Opis, wersję, autora, zakres działania, informacje o **Zgodność** i **Otwórz stronę domową**. Przed otwarciem strony zatwierdzasz adres.
- **Edytuj w kreatorze skryptów** lub **Kopiuj do kreatora skryptów**. Zobacz [edycję i eksport](#scripts-builder-manage).
- **Ustawienia** skryptu, [**Uprawnienia**](#scripts-permissions), [**Polecenia**](#scripts-commands) i [**Dziennik**](#scripts-log).
- **Uruchamiaj wcześniej** i **Uruchamiaj później**, żeby wybrać kolejność skryptów zmieniających tę samą wypowiedź. Skrypty aplikacji działają przed globalnymi.
- **Wyczyść zapisane dane** usuwa dane skryptu. **Usuń** po potwierdzeniu usuwa też kod i ustawienia.

#### Uprawnienia skryptu {#scripts-permissions}

Skrypt może użyć zadeklarowanych uprawnień. W sekcji **Uprawnienia** możesz każde z nich wyłączyć. Części skryptu, które go potrzebują, przestaną działać.

Uprawnienia obejmują czytanie ekranu, klikanie i przewijanie, wpisywanie oraz przenoszenie fokusu, dostęp do mowy, odczyt haseł, dodawanie poleceń i skrótów, internet, schowek, akcje systemowe, powiadomienia, śledzenie zdarzeń w aplikacjach, okna dialogowe i kod Java.

Działania systemowe obejmują między innymi nawigację, otwieranie aplikacji i adresów, wysyłanie komunikatów do aplikacji, sterowanie głośnością, Wi-Fi i latarką, multimedia, timery, alarmy, połączenia i wiadomości. Kod Java daje pełny dostęp SandFly w każdej aplikacji. Pozostałe przełączniki nie ograniczają skryptu, który ma to uprawnienie. Dostęp do haseł, schowka, sieci, działań systemowych, powiadomień, wszystkich zdarzeń, okien i Javy przyznawaj tylko zaufanym skryptom.

#### Polecenia skryptów {#scripts-commands}

W [Polecenia](#commands) otwórz kategorię **Skrypty**. Każda pozycja zawiera nazwę skryptu i nazwę jego polecenia. Możesz [przypisać](#commands-assign) je do gestu, klawisza, menu i sterowania czytaniem.

Aby zmienić przypisania na stronie skryptu, otwórz **Polecenia** i wybierz nazwę. Użyj **Wykonaj gest lub naciśnij skrót**, **Wybierz gest** lub **Wpisz skrót klawiszowy**. Przykładowa składnia to `sf+shift+n`, `sf+t`, `s` i `shift+n`. Przy zapisanym geście **Wyłącz** przywraca działanie SandFly dla tego gestu, a **Usuń** usuwa zapis ze skryptu. **Przywróć domyślne przypisania** pojawia się po zmianie.

Kiedy skrypt działa, jego skróty mają pierwszeństwo. Polecenia skryptów ładowanych tylko na żądanie znajdziesz w głównym ekranie **Polecenia**. Jeśli skrypt nie może działać na bieżącym ekranie, SandFly o tym poinformuje.

#### Dziennik skryptu {#scripts-log}

**Dziennik** pokazuje wyniki i błędy od uruchomienia SandFly, wraz z godziną. **Kopiuj dziennik** ułatwia przekazanie informacji autorowi. **Wyczyść dziennik** usuwa wpisy. SandFly wyłącza skrypt po powtarzających się błędach i kieruje do dziennika.

### Wyłączanie skryptów {#scripts-turn-off}

Pojedynczy skrypt wyłączysz na jego stronie przełącznikiem **Włącz**. **Skrypty** > **Wyłącz wszystkie skrypty** wstrzymuje wszystkie i zapamiętuje ich indywidualne stany. To samo robi polecenie **Włącz lub wyłącz wszystkie skrypty** w kategorii **SandFly**. Przydaje się, gdy skrypt przeszkadza w obsłudze aplikacji.

### Kreator skryptów {#scripts-builder}

Otwórz **Skrypty** > **Nowy skrypt**, wpisz nazwę i wybierz **OK**. Zmiany są zapisywane i stosowane od razu.

- W sekcji **Aplikacje** użyj **Dodaj aplikację**. Możesz dodatkowo ograniczyć działanie przez **Tylko w aktywności** lub **Tylko w oknie**. Bez wybranej aplikacji skrypt jest globalny.
- Sekcja [**Reguły**](#scripts-builder-rules) zmienia elementy. **Sterowanie czytaniem** pozwala dodać sposób przechodzenia między dopasowanymi elementami.
- W sekcji [**Polecenia**](#scripts-builder-commands) dodajesz działania dla użytkownika. [**Wyzwalacze**](#scripts-builder-triggers) reagują na zdarzenia, a **Zmiany mowy** zmieniają treść odczytu.
- Pozostałe części dotyczą [ustawień i kodu](#scripts-builder-settings) oraz [eksportu i kopiowania](#scripts-builder-manage).

**Strona skryptu** otwiera ekran z przełącznikiem, uprawnieniami i dziennikiem. Niedokończona część podaje, czego brakuje, i zacznie działać po uzupełnieniu.

#### Reguły {#scripts-builder-rules}

Użyj **Dodaj regułę**. W części **Element** określ, co ma pasować. Dostępne są tekst, pola tekstu, wyrażenie regularne i typy znane z [makr](#macros), a także **Dokładny tekst**, ID zasobu i jego rodzica, nazwa klasy, rola, stan oraz element zawierający albo zawarty. **Użyj sprawdzonego elementu** uzupełnia dane z [ostatniej inspekcji](#scripts-inspect).

W części **Zmiany** możesz zmienić etykietę, odczytywaną treść, podpowiedź, stan, rolę lub nagłówek. Możesz ukryć sam element albo razem z zawartością, wyciszyć aktualizacje, czytać go jako jedną pozycję lub zmienić kolejność czytania. Możesz wskazać element uaktywniany dwukrotnym stuknięciem, ustawić aktywację po uniesieniu, czas przytrzymania lub aktywację dotknięciem. **Zmień mowę** korzysta z [edycji wypowiedzi](#scripts-builder-speech). **Ustal, co jest czytane** pozwala wybierać części elementu i kolejność. Użyj **Dodaj część**, żeby dodać jego tekst lub zawartość.

W **Czynności** użyj **Dodaj czynność**. Może kliknąć element, przytrzymać, przenieść fokus, przewinąć lub przeczytać tekst. Ustal, czy ma się pojawiać przed czynnościami aplikacji, czy po nich. **Przyciski w środku jako czynności** przenosi przyciski elementu na tę listę. **Przyciski do pominięcia** pozwala wybrać wyjątki.

Do nagrania czynności użyj **Nagraj czynność**. W aplikacji wykonaj kliknięcia lub przytrzymania, otwórz powiadomienie o nagrywaniu i wybierz **Zapisz** lub **Nagraj ponownie**.

Gdy reguła dotyczy sprawdzonego elementu, możesz grupować wiersz wraz z przyciskami i wybrać czytane części. Ustawienie **Reguła dla** pozwala wybrać podobne wiersze albo podobne elementy. **Duplikuj regułę**, **Kopiuj regułę do skryptu** i **Usuń regułę** pozwalają zarządzać utworzoną regułą.

#### Własne polecenia {#scripts-builder-commands}

Wybierz **Dodaj polecenie** i wpisz tytuł. W części **Kroki** użyjesz edytora [makr](#macros). **Gesty i skrót klawiszowy** ustawia przypisania na bieżącym urządzeniu. **Domyślne przy udostępnianiu** pozwala podać gest, klawisze i ustawienia **W menu SandFly** oraz **Jako opcja sterowania czytaniem** używane po zainstalowaniu skryptu. Możesz też zmienić nazwę lub usunąć polecenie.

#### Wyzwalacze {#scripts-builder-triggers}

Użyj **Dodaj wyzwalacz**. Wybierz zdarzenie, a potem kroki, które mają się wykonać. Wyzwalaczem może być:

- przejście fokusu na element, otwarcie lub zamknięcie aplikacji i zmiana okna,
- komunikat lub powiadomienie z pasującym tekstem,
- zmiana tekstu lub ekranu, przewinięcie listy, ruch kursora, kliknięcie, przytrzymanie i przejście fokusu klawiatury do pola tekstowego. Możesz wskazać konkretny element,
- upływ określonego czasu, dana godzina w ciągu dnia lub użycie przycisku skryptu.

Wyzwalacze czasowe działają, dopóki działa skrypt.

#### Zmiany mowy {#scripts-builder-speech}

Użyj **Dodaj zmianę mowy**. W **Zmieniana mowa** wskaż mowę elementu, powiadomienia (również z wybranej aplikacji), komunikaty albo całą mowę. Możesz dodać warunek **Zawiera tekst (puste: dowolny)**.

W części **Edycje** określ kolejno, który tekst usunąć lub zastąpić. Możesz usunąć powtarzające się części, pominąć części z określonym tekstem, zostawić pierwsze części, coś powiedzieć zamiast oryginału, przed nim lub po nim albo wyciszyć wypowiedź. Części to fragmenty rozdzielone przecinkami. W polach **Powiedz zamiast tego**, **Powiedz przed** i **Powiedz po** zmienna `{text}` wstawia pierwotną wypowiedź SandFly.

**Podgląd na sprawdzonym elemencie** stosuje edycję do elementu z ostatniej inspekcji i pokazuje wynik.

#### Ustawienia, informacje i kod {#scripts-builder-settings}

**Ustawienia** > **Dodaj ustawienie** dodaje na stronie skryptu przełącznik, listę, tekst, liczbę lub przycisk. Kroki mogą odczytywać wartość, a przycisk uruchamia wyzwalacz.

Na ekranie **Informacje** podasz opis, wersję i stronę główną. Ustawisz też ładowanie na żądanie i dostępność przy wygaszonym ekranie. Na ekranie **Kod** możesz dopisać własny **Kod Lua** i wskazać **Dodatkowe uprawnienia**. SandFly sprawdza kod przed zapisem.

#### Eksport i kopiowanie {#scripts-builder-manage}

**Eksportuj skrypt** zapisuje `.zip`. Po ponownym imporcie przez **Importuj skrypt** możesz go dalej edytować w kreatorze. **Duplikuj skrypt** tworzy kopię. **Kopiuj do kreatora skryptów** kopiuje skrypt spoza kreatora, a **Edytuj w kreatorze skryptów** pozwala wrócić do już utworzonego projektu.

#### Tworzenie z SandFly Pro {#scripts-builder-pro}

Użyj **Utwórz skrypt za pomocą SandFly Pro** na ekranie **Skrypty** albo **Zmień za pomocą SandFly Pro** na stronie istniejącego skryptu. Agent napisze skrypt, sprawdzi go i poprosi o ekrany lub próby, których potrzebuje. Po skończeniu wybierz **Akceptuj**, **Poproś o zmiany** albo **Odrzuć**. Przed akceptacją skrypt nie używa internetu ani Javy. Zobacz [pracę z agentem i limity](#pro-scripts).

### Sprawdzanie elementów {#scripts-inspect}

W kategorii **Ustawienia programisty** znajdziesz **Sprawdź element z fokusem**, **Kopiuj szczegóły elementu** i **Kopiuj drzewo ekranu**. Możesz [dodać je do menu SandFly](#commands).

Przenieś fokus na element i wybierz pierwsze polecenie. Okno **Szczegóły elementu** pokazuje teksty, typy, zawartość, dane do skryptów i aplikację. Stuknij dwukrotnie na wierszu, żeby skopiować jego wartość, lub użyj **Kopiuj wszystko**. **Dodaj regułę dla tego elementu** otwiera kreator z uzupełnioną regułą w istniejącym albo nowym skrypcie dla aplikacji.

**Kopiuj szczegóły elementu** robi to bez okna. **Kopiuj drzewo ekranu** kopiuje wszystkie elementy i podaje ich liczbę. Jeśli nie ma elementu do sprawdzenia, SandFly o tym powie.

### Etykiety niestandardowe {#scripts-labels}

Etykieta nadaje własną nazwę elementowi, na przykład przyciskowi bez opisu. SandFly będzie ją odczytywać przy każdym przejściu na element. Element musi mieć identyfikator zasobu w aplikacji.

#### Dodanie, edycja lub usunięcie etykiety {#scripts-labels-add}

1. Przenieś fokus na element.
2. Użyj **Dodawanie i edytowanie etykiet** z kategorii **Aktywacja**.
3. W **Dodaj etykietę** lub **Edytuj etykietę** wpisz nazwę i wybierz **OK**.

Aby usunąć nazwę, użyj tego samego polecenia i wybierz **Usuń etykietę**. SandFly powie, jeśli elementu nie da się nazwać. W ustawieniach **Menu SandFly** możesz dodać **Dodawanie i edytowanie etykiet**. Menu pokaże wtedy **Etykiety**, gdy bieżący element da się nazwać.

#### Zarządzanie etykietami {#scripts-labels-manage}

Otwórz **Etykiety niestandardowe** w ustawieniach. Wybierz aplikację, żeby zobaczyć jej etykiety i daty zmian. Wybierz etykietę, żeby ją zmienić lub usunąć.

#### Import i eksport etykiet {#scripts-labels-export}

Etykiety trafiają do [kopii ustawień](#profiles-export). Możesz też zachować je osobno:

- **Eksportuj niestandardowe etykiety** zapisuje je do pliku i otwiera udostępnianie Androida. Jeśli nie ma etykiet, SandFly o tym powie.
- **Importuj niestandardowe etykiety** otwiera plik. Gdy plik i telefon mają etykietę dla tego samego elementu, **Zastąp** użyje nowej, a **Pomiń** zostawi Twoją.
- **Wycofaj zaimportowane etykiety** pojawia się po imporcie. Usuwa dodane etykiety i przywraca zastąpione.
