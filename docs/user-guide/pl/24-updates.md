## Aktualizacje, Shizuku i root {#updates}

SandFly pobiera aktualizacje ze strony wydań SandFly w serwisie GitHub. Opcje aktualizacji znajdziesz na ekranie **Aktualizacje i instalacja**.

Niektóre funkcje wymagają uprawnień, które Android przyznaje tylko przez adb, czyli Android Debug Bridge. SandFly może je uzyskać za pomocą Shizuku lub przez dostęp root na urządzeniu z rootem. Opcja dotycząca dostępu root jest na ekranie **Shizuku i root**.

### Wydania stabilne i wersje deweloperskie {#updates-channels}

SandFly jest dostępny w dwóch rodzajach kompilacji.

- Wydania stabilne mają numer wersji, np. SandFly 0.6.0. Są dostępne na [stronie wydań SandFly](https://github.com/aaron-gh/SandFly/releases).
- Wersje deweloperskie powstają po każdej zmianie w SandFly. Zmiany trafiają do nich wcześniej, a kompilacje przechodzą mniej testów.

Numer kompilacji rośnie po każdej zmianie. Wybierz **Aktualizacje i instalacja** > **Sprawdź aktualizacje**, aby sprawdzić numer zainstalowanej wersji i kompilacji, np. „Wersja 0.6.1, kompilacja 1234”.

Domyślnie SandFly proponuje tylko wydania stabilne. Aby pobierać również kompilacje deweloperskie, włącz **Aktualizacje i instalacja** > **Wersje deweloperskie**. Od tej pory SandFly proponuje najnowszą dostępną kompilację, stabilną lub deweloperską, i sprawdza aktualizacje mniej więcej co godzinę zamiast raz na dobę.

### Sprawdź dostępność aktualizacji {#updates-check}

1. Otwórz **Aktualizacje i instalacja**.
2. Wybierz **Sprawdź aktualizacje**.

Najpierw pojawi się komunikat „Sprawdzanie aktualizacji”, a potem jeden z poniższych wyników.

- „SandFly jest aktualny”.
- Okno z numerem dostępnej kompilacji i opisem zmian. W przypadku wydania stabilnego są to informacje o wydaniu, a w przypadku wersji deweloperskiej zmiany od zainstalowanej kompilacji. Wybierz **Zainstaluj**, aby ją zainstalować, lub **Anuluj**.
- „Nie udało się sprawdzić aktualizacji”. Ten komunikat pojawia się, gdy nie można połączyć się z GitHubem. Spróbuj ponownie później.

Jeśli właśnie trwa publikowanie nowej kompilacji deweloperskiej, SandFly czeka i ponawia sprawdzanie przez maksymalnie minutę.

### Automatyczne sprawdzanie aktualizacji {#updates-automatic}

Opcja **Aktualizacje i instalacja** > **Automatycznie sprawdzaj aktualizacje** jest domyślnie włączona. SandFly sprawdza dostępność nowej kompilacji 30 sekund od uruchomienia, a potem raz na dobę. Po włączeniu opcji **Wersje deweloperskie** sprawdza co godzinę. O nowej kompilacji informuje powiadomienie **Dostępna aktualizacja SandFly** z opisem zmian. Każda kompilacja jest ogłaszana raz.

Aby powiadomienie się pojawiło, SandFly musi mieć uprawnienie do wyświetlania powiadomień. Jeśli powiadomienia dla SandFly są wyłączone, informacja o kompilacji pojawi się po ich włączeniu.

Jeśli wolisz samodzielnie sprawdzać aktualizacje, wyłącz **Automatycznie sprawdzaj aktualizacje** i używaj opcji **Sprawdź aktualizacje**.

### Zainstaluj aktualizację {#updates-install}

1. Stuknij powiadomienie **Dostępna aktualizacja SandFly** albo wybierz **Zainstaluj** w oknie otwartym przez **Sprawdź aktualizacje**.
2. Przy pierwszej aktualizacji Android otworzy ustawienie zezwalające SandFly na instalowanie aplikacji. Usłyszysz „Zezwól SandFly na instalowanie aplikacji, a następnie ponów aktualizację”. Włącz zezwolenie, wróć i powtórz pierwszy krok.
3. SandFly pokaże „Pobieranie aktualizacji” oraz powiadomienie o postępie. Zobaczysz procent ukończenia, a po kilku sekundach także przybliżony pozostały czas, np. „40%, pozostała 1 minuta i 20 sekund”.
4. Po pobraniu otworzy się systemowe okno instalacji. Wybierz **Zaktualizuj** lub **Zainstaluj**.

SandFly uruchomi się ponownie w nowej wersji, z dotychczasowymi ustawieniami. Jeśli okno instalacji się nie otworzy, np. gdy ekran zgaśnie podczas pobierania, stuknij powiadomienie **Pobrano aktualizację SandFly**. Powiadomienie znika po dziesięciu minutach.

SandFly instaluje tylko własne kompilacje podpisane kluczem SandFly. Jeśli pobrano inny plik, pojawi się komunikat „Pobrany plik nie jest kompilacją tej aplikacji, więc nie został zainstalowany”. Po utracie połączenia SandFly do pięciu razy próbuje wznowić pobieranie od miejsca, w którym zostało przerwane. Jeśli mimo ponowień pobieranie się nie powiedzie, usłyszysz „Nie udało się pobrać aktualizacji”. Ponowne uruchomienie instalacji tej samej kompilacji przed ponownym uruchomieniem SandFly wznawia pobieranie.

Samodzielnie zbudowana wersja SandFly ma inny podpis. Jeśli chcesz zamienić ją na wydanie SandFly lub odwrotnie, najpierw odinstaluj dotychczasową wersję, a potem zainstaluj wybraną.

O aktualizacjach na zegarku przeczytasz w [](#devices-watch-updates).

### Pokaż SandFly jako aplikację z Google Play {#updates-play-installer}

Niektóre aplikacje, np. bankowe, wyłączają usługi ułatwień dostępu, jeżeli Android wskazuje dla nich źródło instalacji inne niż Google Play. Opcja **Aktualizacje i instalacja** > **Pokazuj jako zainstalowane z Google Play** powoduje, że Android zapisuje Google Play jako instalator SandFly. Dzięki temu te aplikacje mogą pozostawić SandFly włączony. Opcja jest domyślnie wyłączona i wymaga Shizuku lub dostępu root.

Po włączeniu tej opcji SandFly ponownie instaluje swój pakiet APK przez Shizuku, wskazując Google Play jako instalator. W trakcie instalacji SandFly uruchamia się ponownie, więc mowa na chwilę się zatrzymuje. Aplikacja i ustawienia zostają takie same. Zmienia się informacja o instalatorze udostępniana innym aplikacjom. Aplikacja może ustalić w inny sposób, skąd faktycznie zainstalowano SandFly.

Przy włączonej opcji aktualizacje działają następująco.

- SandFly instaluje aktualizacje przez Shizuku, aby Google Play pozostał zapisany jako instalator. Jeśli się to nie uda, aktualizacja odbywa się przez systemowe okno instalacji.
- Jeśli Android zapisze inny instalator, np. po aktualizacji bez uruchomionego Shizuku, SandFly ponownie zapisze Google Play po uzyskaniu dostępu do Shizuku. Do tego czasu przy opcji zobaczysz „Włączone, ale Google Play nie jest jeszcze zarejestrowany jako instalator. SandFly ustawia to, gdy Shizuku jest dostępne.”.

Po wyłączeniu opcji SandFly przywraca nazwę poprzedniego instalatora.

Jeśli zmiana instalatora z aplikacji jest niedostępna, użyj czynności **Kopiuj polecenie**. Skopiuje polecenie adb wykonujące tę samą operację. Uruchom je na komputerze z folderu, w którym znajduje się plik `sandfly.apk`.

```
adb install -r -i com.android.vending sandfly.apk
```

Aby przywrócić zwykły zapis instalacji, uruchom `adb install -r sandfly.apk`.

### Shizuku {#updates-shizuku}

[Shizuku](https://shizuku.rikka.app) uruchamia polecenia innych aplikacji z uprawnieniami adb. Gdy Shizuku działa i ma dostęp do niego SandFly, SandFly może otrzymać uprawnienia przyznawane tylko przez adb i wykonać polecenia dostępne w adb. Potrzebujesz Shizuku w wersji 11 lub nowszej.

#### Skonfiguruj Shizuku {#updates-shizuku-setup}

1. Zainstaluj Shizuku i uruchom je zgodnie z instrukcjami w tej aplikacji. Na Androidzie 11 i nowszym możesz je uruchomić przez debugowanie bezprzewodowe, bez komputera. Na urządzeniu z rootem możesz też użyć dostępu root.
2. Włącz SandFly. Jeśli jest już włączony, wyłącz go i włącz ponownie.
3. Gdy SandFly uruchomi się przy działającym Shizuku, pojawi się okno wyboru uprawnień. Zezwól SandFly na dostęp.

SandFly samoczynnie wyświetla prośbę o dostęp do Shizuku tylko raz. Później prośba pojawia się przy włączaniu funkcji, która potrzebuje dostępu. Jeśli wybierzesz odmowę bez ponownego wyświetlania pytania, zmień uprawnienie SandFly w aplikacji Shizuku.

Shizuku uruchomione przez debugowanie bezprzewodowe lub adb zatrzymuje się przy ponownym uruchomieniu urządzenia. Uruchom je wtedy ponownie w aplikacji Shizuku.

#### Funkcje korzystające z Shizuku lub roota {#updates-shizuku-features}

Bez Shizuku lub dostępu root poniższe funkcje są niedostępne albo działają w ograniczonym zakresie.

- **Dotyk** > **Napraw zablokowaną eksplorację dotykową** jest domyślnie włączone. Potrzebuje uprawnienia DUMP, aby wznowić eksplorację, gdy Android pozostawi dotknięcie w stanie aktywnym. Bez uprawnienia SandFly tylko informuje, że eksploracja dotykowa mogła się zablokować. Zobacz [](#touch).
- **Wyjście audio** > **Włączaj tryb głośnomówiący po odsunięciu telefonu od ucha** wymaga uprawnienia do zarządzania połączeniami. Działa od Androida 12. Zobacz [](#sound).
- **Wyjście audio** > **Resetuj dźwięk ułatwień dostępu po połączeniach** działa przez Shizuku lub dostęp root, od Androida 13. Zobacz [](#sound).
- **Klawisze głośności** > **Skróty przy wygaszonym ekranie** działają wyłącznie przez Shizuku. Zobacz [](#buttons).
- **Przegląd** > **Używaj przeglądu SandFly** odczytuje i otwiera ostatnie aplikacje z pomocą Shizuku lub dostępu root. Bez nich otwiera się systemowy przegląd. Shizuku lub roota wymagają też Alt+Tab, otwieranie aplikacji na podzielonym ekranie oraz polecenia **Starsza aplikacja** i **Nowsza aplikacja**. Zobacz [](#display).
- Panel szybkich ustawień SandFly korzysta z Shizuku lub roota do obsługi m.in. Wi-Fi, Bluetooth, danych komórkowych i trybu samolotowego. Dotyczy to też jasności, automatycznego obracania, podświetlenia nocnego, dodatkowego przyciemnienia, kolorów i rozmiaru elementów na ekranie. Bez tych uprawnień niektóre przełączniki otwierają odpowiednie ustawienia Androida. Zobacz [](#display).
- Jeśli w historii schowka włączysz **Dodawaj kopie z innych aplikacji**, dołączenie tekstu skopiowanego poza SandFly wymaga Shizuku lub roota. Zobacz [](#typing).
- Gboard odtwarza własne sygnały klawiszy, np. z opcji **Wibracja przy naciśnięciu klawisza**, podczas eksplorowania klawiszy tylko z Shizuku lub dostępem root. Zobacz [](#typing).
- Polecenia uruchamiające skrót ułatwień dostępu innej usługi wymagają Shizuku lub roota. Zobacz [](#commands).
- Polecenia z grup **Kafelki aplikacji** i **Kafelki systemowe** oraz **Dodaj kafelek systemowy** w ustawieniach panelu szybkich ustawień. Bez Shizuku lub roota kafelek działa tylko wtedy, gdy znajduje się w szybkich ustawieniach systemu, a **Dodaj kafelek systemowy** wyszukuje kafelki w szybkich ustawieniach systemu. Zobacz [](#commands-categories) i [](#display-quick-settings).
- Skróty dodawane przez aplikację podczas działania oraz skróty, które może otworzyć tylko aplikacja ekranu głównego, są widoczne na liście tylko z Shizuku lub dostępem root. Zobacz [](#gestures-app-shortcuts).
- Tych uprawnień wymagają też skrypty i makra naciskające klawisze lub wpisujące tekst poza polem edycji oraz kroki makr zmieniające wymienione wyżej opcje w szybkich ustawieniach. Zobacz [](#scripts) i [](#macros).
- **Aktualizacje i instalacja** > **Pokazuj jako zainstalowane z Google Play** wymaga dostępu opisanego w [](#updates-play-installer).

Jeśli przy uruchomieniu SandFly ma dostęp do Shizuku lub roota, przyznaje sobie uprawnienia wymagane przez włączone funkcje.

#### Przyznaj uprawnienia przez adb {#updates-adb}

Jeśli nie korzystasz z Shizuku, przy ustawieniach wymagających dodatkowego uprawnienia znajdziesz w opisie polecenie adb. Czynność **Kopiuj polecenie** zapisze je w schowku. Podłącz urządzenie do komputera z adb, włącz debugowanie przez USB lub debugowanie bezprzewodowe, uruchom polecenie i wróć do ustawienia.

Polecenie zależy od funkcji.

- Aby użyć opcji **Napraw zablokowaną eksplorację dotykową**, uruchom poniższe polecenie.

  ```
  adb shell pm grant io.github.aaron_gh.sandfly android.permission.DUMP
  ```

- Aby użyć opcji **Włączaj tryb głośnomówiący po odsunięciu telefonu od ucha**, uruchom poniższe polecenie.

  ```
  adb shell appops set io.github.aaron_gh.sandfly MANAGE_ONGOING_CALLS allow
  ```

- Aby przenieść skróty ułatwień dostępu z Backtalk Aaron, uruchom poniższe polecenie. Zobacz [](#getting-started-backtalk).

  ```
  adb shell pm grant io.github.aaron_gh.sandfly android.permission.WRITE_SECURE_SETTINGS
  ```

Dla opcji **Resetuj dźwięk ułatwień dostępu po połączeniach** adb nie udostępnia uprawnienia. Bez Shizuku przy tym ustawieniu pojawia się polecenie, które jednorazowo resetuje wyjście audio ułatwień dostępu. Przydaje się, gdy po rozmowie mowa nadal jest odtwarzana przez słuchawkę telefonu. Polecenie wskazuje zainstalowany pakiet APK, więc skopiuj je ponownie po aktualizacji SandFly.

### Użyj dostępu root zamiast Shizuku {#updates-root}

Na urządzeniu z rootem SandFly może wykonywać polecenia za pomocą dostępu root. Włącz **Shizuku i root** > **Używaj roota zamiast Shizuku**. Opcja jest domyślnie wyłączona.

SandFly uruchamia wtedy polecenia przez `su` jako użytkownik `shell`, z którego korzystają też adb i Shizuku. Menedżer dostępu root, np. Magisk lub KernelSU, wyświetli prośbę o przyznanie dostępu SandFly. Po włączeniu opcji funkcje wymagające Shizuku używają dostępu root. Wyjątkiem jest opcja **Skróty przy wygaszonym ekranie**, ukryta w tym trybie. Jeśli `su` nie przyzna dostępu root, w przeglądzie aplikacji i na podzielonym ekranie usłyszysz „Brak dostępu do roota”.

Uwaga: Jeśli `su` na Twoim urządzeniu nie może uruchomić polecenia jako użytkownik `shell`, SandFly wykonuje je jako użytkownik root. Dodawanie kopii z innych aplikacji do historii schowka i przełącznik hotspotu są wtedy niedostępne.
