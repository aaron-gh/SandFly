## Aktualizacje, Shizuku i root {#updates}

SandFly może pobierać wydania z GitHuba. Otwórz **Aktualizacje i instalacja**, żeby sprawdzić wersję i zmienić sposób aktualizowania. Niektóre funkcje Androida wymagają uprawnień ADB. SandFly może uzyskać je przez [Shizuku](#updates-shizuku) lub na zrootowanym urządzeniu przez [roota](#updates-root).

### Wydania stabilne i deweloperskie {#updates-channels}

[Strona wydań SandFly](https://github.com/aaron-gh/SandFly/releases) zawiera wersje stabilne, oznaczane na przykład jako 0.6.0. Kompilacje deweloperskie powstają po kolejnych zmianach. Dostają poprawki wcześniej i przechodzą mniej testów.

Każda kompilacja ma rosnący numer. **Aktualizacje i instalacja** > **Sprawdź aktualizacje** podaje wersję i bieżący numer. Domyślnie SandFly proponuje stabilne wydania. Włącz **Wersje deweloperskie**, żeby dostawać najnowszą z obu możliwości. SandFly będzie wtedy sprawdzać aktualizacje mniej więcej co godzinę.

### Sprawdzanie aktualizacji {#updates-check}

Otwórz **Aktualizacje i instalacja** > **Sprawdź aktualizacje**. SandFly powie, czy wersja jest bieżąca. Jeśli znajdzie aktualizację, pokaże numer i opis zmian. Przy wydaniu stabilnym są to informacje o wydaniu, a przy deweloperskim zmiany od Twojej wersji. Wybierz **Zainstaluj** albo **Anuluj**.

Jeśli GitHub jest niedostępny, spróbuj później. Gdy trwa publikacja kompilacji deweloperskiej, SandFly może ponawiać sprawdzenie przez minutę.

### Automatyczne sprawdzanie {#updates-automatic}

**Automatycznie sprawdzaj aktualizacje** jest domyślnie włączone. Pierwsze sprawdzenie następuje po około 30 sekundach od uruchomienia SandFly, a potem raz dziennie lub co godzinę przy wersjach deweloperskich. **Dostępna aktualizacja SandFly** zawiera zmiany i pojawia się raz na kompilację. Do wyświetlenia powiadomienia potrzebna jest zgoda Androida. Po udzieleniu zgody SandFly może pokazać oczekującą aktualizację.

Wyłącz tę opcję, jeśli chcesz sprawdzać wersję samodzielnie.

### Instalacja aktualizacji {#updates-install}

1. Otwórz **Dostępna aktualizacja SandFly** albo wybierz **Zainstaluj** po ręcznym sprawdzeniu.
2. Jeśli Android poprosi o zgodę na instalowanie aplikacji przez SandFly, włącz ją i powtórz pierwszy krok.
3. Poczekaj na pobranie. SandFly pokazuje procent i po chwili szacowany czas.
4. W instalatorze Androida użyj **Zaktualizuj** lub **Zainstaluj**.

SandFly uruchomi się ponownie z zachowanymi ustawieniami. Gdy instalator się nie otworzy, na przykład przez wyłączony ekran, wybierz **Pobrano aktualizację SandFly**. Powiadomienie jest dostępne przez 10 minut.

SandFly przyjmuje pliki podpisane swoim kluczem. Jeśli połączenie się zerwie, podejmie do pięciu prób i wznowi od przerwanego miejsca. Ponowne rozpoczęcie instalacji tej samej kompilacji przed ponownym uruchomieniem też może wznowić pobieranie.

Samodzielnie zbudowana kompilacja ma inny podpis. Android nie pozwoli zastąpić nią oficjalnego wydania w tym samym pakiecie. [Zapisz ustawienia](#profiles-export) przed zmianą wydania, jeśli wymaga odinstalowania. Na zegarku zobacz [aktualizacje Wear OS](#devices-watch-updates).

### Google Play jako źródło instalacji {#updates-play-installer}

Niektóre aplikacje wyłączają usługi dostępności zainstalowane spoza Google Play. Domyślnie wyłączona opcja **Pokazuj jako zainstalowane z Google Play** zmienia źródło instalacji zapisane przez Androida. Wymaga Shizuku albo roota.

SandFly instaluje ponownie własny pakiet i przez chwilę milknie. Kod i ustawienia pozostają takie same. Inne aplikacje mogą także sprawdzać pochodzenie w inny sposób.

Przy kolejnych aktualizacjach SandFly próbuje zachować źródło przez Shizuku. Po niepowodzeniu otworzy instalator Androida. Jeśli źródło się zmieniło, SandFly ustawi je ponownie po uzyskaniu dostępu do Shizuku. Do tego czasu informuje o bieżącym stanie. Wyłączenie funkcji przywraca poprzednie źródło.

Opcja **Kopiuj polecenie** udostępnia odpowiednik dla ADB. Uruchom z katalogu zawierającego `sandfly.apk`:

```
adb install -r -i com.android.vending sandfly.apk
```

Aby przywrócić zwykłą instalację, użyj `adb install -r sandfly.apk`.

### Shizuku {#updates-shizuku}

[Shizuku](https://shizuku.rikka.app) wykonuje dla innych aplikacji polecenia z uprawnieniami ADB. SandFly wymaga Shizuku 11 lub nowszego.

#### Uruchamianie Shizuku {#updates-shizuku-setup}

1. Zainstaluj Shizuku i uruchom zgodnie z instrukcjami aplikacji. W Androidzie 11 i nowszym możesz użyć debugowania bezprzewodowego bez komputera. Na zrootowanym telefonie dostępny jest start z rootem.
2. Włącz SandFly. Jeśli już działa, wyłącz i włącz czytnik.
3. Gdy Shizuku zapyta, zezwól na dostęp SandFly.

SandFly prosi sam przy pierwszym uruchomieniu. Później prośba pojawia się przy włączaniu funkcji, która go potrzebuje. Jeśli wybrano odmowę bez ponownego pytania, zmień zgodę w aplikacji Shizuku.

Shizuku uruchomione przez debugowanie wyłącza się po ponownym uruchomieniu urządzenia. Włącz je wtedy w aplikacji.

#### Funkcje korzystające z Shizuku lub roota {#updates-shizuku-features}

| Funkcja | Do czego potrzebuje dodatkowych uprawnień |
| --- | --- |
| [Napraw zablokowaną eksplorację dotykową](#touch) | Uprawnienie DUMP pozwala naprawić dotyk pozostawiony przez Androida w stanie przytrzymania. Bez niego SandFly sygnalizuje problem. |
| [Włączaj tryb głośnomówiący po odsunięciu telefonu od ucha](#sound) | Zarządzanie trwającym połączeniem, od Androida 12. |
| [Resetuj dźwięk ułatwień dostępu po połączeniach](#sound) | Reset wyjścia po rozmowie, od Androida 13. |
| [Skróty przy wygaszonym ekranie](#buttons) | Odbieranie klawiszy głośności przy wygaszonym ekranie. Działa przez Shizuku. |
| [Przegląd SandFly](#display) | Ostatnie aplikacje, Alt + Tab, otwieranie w podziale ekranu i nawigacja po ostatnich aplikacjach. Bez uprawnień dostępny jest przegląd systemowy. |
| [Panel szybkich ustawień](#display) | Zmiana ustawień systemu. Bez uprawnień część kafelków otwiera odpowiednie ustawienia. |
| [Pisanie i schowek](#typing) | Dodawanie kopii z innych aplikacji do historii schowka i odtwarzanie dźwięków lub wibracji Gboard przy eksploracji klawiszy. |
| [Polecenia dostępności](#commands) | Uruchamianie skrótu innej usługi dostępności. |
| [Skrypty](#scripts) i [makra](#macros) | Naciskanie klawiszy, pisanie bez pola tekstu i zmienianie wskazanych szybkich ustawień. |
| [Źródło instalacji](#updates-play-installer) | Zapis Google Play jako instalatora. |

Po uruchomieniu z dostępem do Shizuku lub roota SandFly nadaje sobie uprawnienia potrzebne włączonym funkcjom.

#### Nadawanie zgód przez ADB {#updates-adb}

Jeśli do działania wystarczy jedno uprawnienie ADB, bez Shizuku SandFly pokazuje polecenie w opisie opcji i udostępnia **Kopiuj polecenie**. Podłącz urządzenie do komputera z ADB przez USB lub debugowanie bezprzewodowe, wykonaj polecenie i wróć do ustawienia.

Dla **Napraw zablokowaną eksplorację dotykową**:

```
adb shell pm grant io.github.aaron_gh.sandfly android.permission.DUMP
```

Dla **Włączaj tryb głośnomówiący po odsunięciu telefonu od ucha**:

```
adb shell appops set io.github.aaron_gh.sandfly MANAGE_ONGOING_CALLS allow
```

Dla [przenoszenia skrótów dostępności z Backtalka Aaron](#getting-started-backtalk):

```
adb shell pm grant io.github.aaron_gh.sandfly android.permission.WRITE_SECURE_SETTINGS
```

Przy opcji **Resetuj dźwięk ułatwień dostępu po połączeniach** ADB może jednorazowo zmienić wyjście. Skopiuj polecenie z SandFly. Zawiera ścieżkę do zainstalowanego pakietu, więc po aktualizacji trzeba skopiować je ponownie.

### Root zamiast Shizuku {#updates-root}

Na zrootowanym urządzeniu możesz włączyć **Shizuku i root** > **Używaj roota zamiast Shizuku**. Domyślnie ta opcja jest wyłączona. Menedżer roota, na przykład Magisk lub KernelSU, poprosi o zgodę.

SandFly używa `su` i próbuje wykonywać polecenia jako użytkownik `shell`, tak jak przez ADB lub Shizuku. W tym trybie skróty klawiszy głośności przy wygaszonym ekranie nie są dostępne. Gdy zgody na root nie ma, przegląd i podział ekranu podadzą tę przyczynę.

Jeśli `su` nie może wykonać polecenia jako `shell`, SandFly użyje roota. W tej sytuacji nie działa dodawanie kopii z innych aplikacji do historii schowka ani przełącznik hotspotu.
