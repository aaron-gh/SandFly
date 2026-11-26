## Profile ustawień i kopie zapasowe {#profiles}

Profil ustawień to nazwany zestaw ustawień. Gdy jest aktywny, SandFly stosuje zawarte w nim wartości zamiast Twoich zwykłych ustawień. Profil może działać wszędzie albo tylko w wybranych aplikacjach i sytuacjach. Opcja **Kopia zapasowa i przywracanie** zapisuje wszystkie ustawienia SandFly w pliku i pozwala je z niego odtworzyć. Za pomocą plików poszczególnych elementów możesz udostępniać na przykład same makra lub profile gestów albo przenosić je na inny telefon.

O profilach, które zmieniają tylko przypisania gestów, przeczytasz w [](#gestures). Profile głosów opisuje [](#speech).

### Co zawiera profil ustawień {#profiles-settings-profiles}

Profil przechowuje tylko te ustawienia, które w nim zmienisz. Wszystkie pozostałe zachowują wartości obowiązujące bez profili. Listę znajdziesz na ekranie **Profile ustawień** w ustawieniach SandFly. Aktywny profil ma oznaczenie **Używany**.

Profilu możesz używać na dwa sposoby.

- **Użyj teraz** włącza profil we wszystkich aplikacjach aż do jego wyłączenia. W ten sposób można używać tylko jednego profilu naraz. Zobacz [](#profiles-use).
- Opcje **Używaj automatycznie** i **Aplikacje** pozwalają włączyć profil podczas przeglądania treści internetowych, czytania ciągłego lub edycji tekstu, a także po otwarciu wybranej aplikacji lub klawiatury. Zobacz [](#profiles-automatic).

Profil może zawierać ustawienia główne SandFly oraz ustawienia klawiatury brajlowskiej i monitora brajlowskiego. Możesz w nim zapisać także gesty, skróty klawiszowe, profile gestów i wybór aktywnego profilu gestów. Poza profilem pozostają klucze API, licencja SandFly Pro, czas ostatniego sprawdzenia dostępności aktualizacji, zainstalowane skrypty i ich ustawienia oraz szkice motywów dźwiękowych. Profil nie przechowuje też informacji związanych z konkretnym telefonem, na przykład o wyświetlonych samouczkach i oknach dialogowych.

### Tryb BlackBerry {#profiles-blackberry-mode}

**Tryb BlackBerry** na dole ekranu **Profile ustawień** sprawia, że SandFly działa podobnie do BlackBerry 10 Screen Reader. Jego gesty, klawisze i dźwięki mogą być nieznane, jeśli nie używałeś BlackBerry 10. Włączenie otwiera okno **Włączyć tryb BlackBerry?**. Wybierz **Włącz**, aby potwierdzić. Za pierwszym razem SandFly tworzy profil ustawień **Tryb BlackBerry** i od razu go używa. Profil ustawia:

- **Gesty** > **Układ gestów** na **Gesty BlackBerry** z włączoną opcją **Obsługuj przesunięcia dwoma palcami w SandFly**. Zobacz [](#gestures-layout).
- Na telefonach z klawiaturą **Mapa klawiszy BlackBerry**. Zobacz [](#keyboards-phones).
- Motyw dźwiękowy **BlackBerry**, który instaluje SandFly. Motyw zawiera dźwięki SandFly, a nie dźwięki BlackBerry 10: brak dźwięku przy przeniesieniu fokusu oraz dźwięki zmiany ekranu, kliknięcia, przewijania, końca listy, błędów oraz pokazania i ukrycia klawiatury. **Dźwięki i wibracje kontrolek** są wyłączone. Zobacz [](#sound).
- Brak wskazówek, pozycji na listach i w siatkach, informacji o dostępnych czynnościach oraz komunikatów o pokazaniu i ukryciu klawiatury. Typy kontrolek są nadal odczytywane, a zmiany wysokości głosu są włączone.

Wszystkie pozostałe ustawienia zostają bez zmian. Aby zmienić to, co ustawia tryb BlackBerry, zmień ustawienia profilu **Tryb BlackBerry**. Zobacz [](#profiles-change-settings). Wyłączenie trybu BlackBerry kończy używanie profilu, a ponowne włączenie używa profilu z Twoimi zmianami. Aby zacząć od ustawień domyślnych, usuń profil. Polecenie **Włącz lub wyłącz tryb BlackBerry** z grupy **SandFly** na ekranie **Polecenia** robi to samo z gestu lub klawisza i przed włączeniem także otwiera okno potwierdzenia.

Tryb BlackBerry działa wszędzie, tak jak profil uruchomiony przez **Użyj teraz**, więc zastępuje inny profil używany w ten sposób.

### Dodaj profil ustawień {#profiles-add}

1. Otwórz **Profile ustawień** > **Nowy profil ustawień**.
2. Wpisz nazwę lub zostaw pole puste, aby użyć sugerowanej nazwy, np. „Profil 2”.
3. Wybierz **OK**.

Nowy profil jest pusty. Ustawienia dodasz według instrukcji w [](#profiles-change-settings).

### Zmień ustawienia profilu {#profiles-change-settings}

1. Otwórz ekran **Profile ustawień** i wybierz profil.
2. Wybierz **Zmień ustawienia**.

SandFly odczyta komunikat „Zmiany trafiają do profilu” z jego nazwą i otworzy ustawienia SandFly z zastosowanym profilem. Każde ustawienie, które teraz zmienisz, zostanie zapisane w tym profilu. Gdy wrócisz na ekran profilu, przejdziesz do ekranu **Profile ustawień** lub otworzysz inną aplikację, usłyszysz komunikat „Zmiany zapisano w profilu” z jego nazwą.

Jeśli profil ustawia już daną opcję, jej późniejsza zmiana podczas korzystania z profilu również zostanie w nim zapisana. Tak zadziała na przykład zmiana szybkości mowy za pomocą elementów sterujących czytaniem. Zmiany pozostałych opcji obowiązują także bez profilu.

Na ekranie profilu znajdziesz listę **Ustawienia w tym profilu**. Przy każdej pozycji podano nazwę ustawienia i jego miejsce w ustawieniach SandFly. Aby usunąć je z profilu, wybierz tę pozycję, a następnie **Usuń**. Opcja przyjmie wtedy wartość obowiązującą bez profilu.

### Włącz profil od razu {#profiles-use}

1. Otwórz ekran **Profile ustawień** i wybierz profil.
2. Wybierz **Użyj teraz**.

Profil obowiązuje wszędzie, również w ustawieniach SandFly. Aby go wyłączyć, wybierz **Przestań używać** na tym samym ekranie.

Poniższe polecenia z grupy **SandFly** służą do wyboru profilu, który ma obowiązywać wszędzie. Domyślnie nie mają przypisań. Zobacz [](#commands).

- **Wybierz profil ustawień** otwiera menu z pozycją **Brak profilu ustawień** i dostępnymi profilami.
- **Następny profil ustawień** i **Poprzedni profil ustawień** przełączają między profilami oraz pozycją **Brak profilu ustawień**.

SandFly odczytuje nazwę wybranego profilu, np. „Profil Czytanie”, albo komunikat „Brak profilu ustawień”.

### Włączaj profil automatycznie {#profiles-automatic}

Na ekranie wybranego profilu możesz ustawić, kiedy ma się włączać.

- **Używaj automatycznie** > **Treści internetowe** stosuje profil, gdy fokus znajduje się w treści internetowej.
- **Używaj automatycznie** > **Czytanie ciągłe** stosuje profil podczas czytania ciągłego, na przykład po uruchomieniu polecenia czytania od góry.
- **Używaj automatycznie** > **Edytowanie tekstu** stosuje profil, gdy fokus jest w polu edycji albo na klawiaturze służącej do wpisywania tekstu w tym polu.
- **Aplikacje** > **Dodaj aplikację** stosuje profil, gdy wyświetlana jest wybrana aplikacja. Aby usunąć aplikację z listy, wybierz ją, a następnie wybierz **Usuń**.
- **Klawiatury** > **Dodaj klawiaturę** stosuje profil, gdy wyświetlana jest wybrana klawiatura. Lista zawiera klawiatury włączone w ustawieniach Androida. Aby usunąć klawiaturę z listy, wybierz ją, a następnie wybierz **Usuń**.

Automatyczne profile są pomijane w ustawieniach SandFly, dzięki czemu widzisz tam zwykłe wartości. Są także nieaktywne po wyłączeniu SandFly.

#### Gdy kilka profili zmienia to samo ustawienie {#profiles-order}

Kilka profili może działać jednocześnie. Jeśli zmieniają tę samą opcję, pierwszeństwo ma profil włączony przez **Użyj teraz**, a następnie profil znajdujący się wyżej na liście **Profile ustawień**. Kolejność zmienisz przyciskami **Przenieś w górę** i **Przenieś w dół** na ekranie profilu.

### Zmień nazwę, kolejność lub usuń profil {#profiles-manage}

Na ekranie profilu są także następujące opcje.

- **Zmień nazwę** pozwala zmienić nazwę profilu.
- **Przenieś w górę** i **Przenieś w dół** zmieniają miejsce profilu na liście.
- **Eksportuj do pliku** zapisuje profil jako plik pojedynczego elementu. Zobacz [](#profiles-item-files).
- **Usuń** usuwa profil po potwierdzeniu.

### Kopia zapasowa i przywracanie ustawień {#profiles-backup}

Opcja **Kopia zapasowa i przywracanie** zapisuje wszystkie ustawienia i własną zawartość SandFly w pliku. Pozwala też zastąpić bieżące ustawienia i zawartość danymi z pliku, by na przykład tak samo skonfigurować drugi telefon. Opcje te są dostępne na telefonach.

#### Eksportuj ustawienia {#profiles-export}

1. Otwórz **Kopia zapasowa i przywracanie** > **Eksportuj ustawienia**.
2. Wybierz miejsce zapisu pliku. Sugerowana nazwa to `sandfly-settings.zip`.

SandFly odczytuje potwierdzenie eksportu i informuje, że w pliku nie ma kluczy API.

Archiwum ZIP zawiera wszystkie ustawienia SandFly oraz dodatkowe dane.

- Własne etykiety.
- Profile ustawień. Ustawienia główne zapisują się w takiej postaci, w jakiej obowiązują bez profili, a profile są zapisane osobno.
- Zainstalowane skrypty.
- Motywy dźwiękowe i dźwięki zmienione w motywie Backtalk.
- Obraz tapety.

Poniższe dane pozostają na urządzeniu.

- Klucze API, dzięki czemu możesz udostępniać lub synchronizować plik kopii zapasowej.
- Licencja SandFly Pro. Aby korzystać z niej na drugim telefonie, aktywuj SandFly Pro na nim jako na osobnym urządzeniu. Zobacz [](#pro).
- Czas ostatniego sprawdzenia dostępności aktualizacji.
- Pobrane modele AI i rozpoznawania mowy, dzienniki oraz zgłoszenia problemów.
- Szkice motywów dźwiękowych.

#### Importuj ustawienia {#profiles-import}

1. Otwórz **Kopia zapasowa i przywracanie** > **Importuj ustawienia**.
2. Wybierz plik ustawień.

Ustawienia z pliku zastępują bieżące, a SandFly odczytuje potwierdzenie importu. Klucze API na telefonie i ustawienia, których nie ma w pliku, zachowują swoje wartości. Aktywne profile zostają ponownie zastosowane do zaimportowanych ustawień.

Własne etykiety, profile ustawień, skrypty i motywy dźwiękowe z pliku zostają dodane do telefonu. Każdy z nich zastępuje odpowiadający mu element, na przykład o tej samej nazwie lub identyfikatorze. Pozostałe elementy zostają. Obraz tapety z pliku zastępuje obraz na telefonie.

Opcja **Importuj ustawienia** obsługuje również pliki z rozszerzeniem `.json` wyeksportowane przez starszą wersję SandFly oraz pliki ustawień z Backtalka. Ustawienia z pliku Backtalka zastępują odpowiadające im ustawienia SandFly. Opcje dostępne tylko w SandFly zachowują swoje wartości.

Jeśli usłyszysz „Ten plik nie jest plikiem ustawień SandFly ani Backtalk lub nie można go odczytać”, sprawdź format i rozmiar pliku. Plik może być uszkodzony lub mieć niewłaściwy format. Limit to 5 MB dla pliku `.json` i 300 MB po rozpakowaniu dla pliku `.zip`. W razie błędu ustawienia pozostają bez zmian.

### Pliki pojedynczych elementów {#profiles-item-files}

W takim pliku możesz przechować elementy jednego lub kilku rodzajów bez pozostałych ustawień SandFly. Możesz je wyeksportować, udostępnić, zmienić, napisać ręcznie lub za pomocą czatu AI i wczytać na dowolnym telefonie z SandFly. Pełną kopię ustawień wczytasz przez **Importuj ustawienia**, a plik pojedynczych elementów przez **Importuj z pliku**.

Na każdym z poniższych ekranów znajdziesz opcje **Eksportuj do pliku** i **Importuj z pliku**.

| Elementy | Ekran |
| --- | --- |
| Makra | **Makra** |
| Profile gestów | **Gesty** > **Profile gestów** |
| Gesty w aplikacjach | **Gesty** > **Gesty w aplikacjach** |
| Skróty klawiszowe w aplikacjach | **Skróty klawiszowe** > **Skróty klawiszowe w aplikacjach** |
| Mapy klawiszy | **Skróty klawiszowe**. Eksportowana jest aktualnie używana mapa. |
| Profile ustawień | **Profile ustawień** |
| Słownik mowy | **Mowa** > **Słownik mowy** |
| Słownik symboli | **Mowa** > **Słownik symboli** |
| Słownik brajlowski | **Brajl** > **Słownik brajlowski** |
| Własne znaki brajlowskie | **Brajl** > **Klawiatura brajlowska** > **Własne znaki brajlowskie** |
| Własne polecenia głosowe | **Polecenia głosowe** |

Pliki pojedynczych elementów są dostępne na telefonach.

#### Eksportuj elementy do pliku {#profiles-item-export}

1. Otwórz ekran z wybranymi elementami.
2. Wybierz **Eksportuj do pliku**.
3. Wybierz miejsce zapisu pliku.

Na ekranie listy **Eksportuj do pliku** zapisuje wszystkie pozycje. Na ekranie pojedynczego elementu, na przykład profilu ustawień, zapisuje tylko ten element. SandFly odczytuje „Wyeksportowano” lub „Nie można wyeksportować”.

#### Importuj elementy z pliku {#profiles-item-import}

1. Otwórz dowolny ekran wymieniony w tabeli w [](#profiles-item-files).
2. Wybierz **Importuj z pliku**.
3. Wybierz plik z elementami.

SandFly wczytuje wszystkie elementy zawarte w pliku bez względu na to, z którego ekranu uruchomisz import. Następnie podaje wynik, np. „Zaimportowano: 2 makra, 1 profil gestów”. Element zastępuje istniejącą pozycję tego samego rodzaju i o tej samej nazwie, bez względu na wielkość liter. Pozostałe pozycje zostają. Wpisy słownika zastępują wpisy wyszukujące ten sam tekst w tej samej aplikacji. Własne znaki brajlowskie zastępują pozycje o tym samym tekście w tej samej tablicy brajlowskiej. Własne polecenia głosowe zastępują polecenia o tym samym wyrażeniu. Zaimportowane mapy klawiszy są zawsze zapisywane jako mapy niestandardowe.

Po imporcie SandFly wymienia pominięte pozycje, na przykład nierozpoznane polecenia, gesty, klawisze, ustawienia i odrzucone wyrażenia regularne. Jeśli wybierzesz pełną kopię, usłyszysz „To jest plik ustawień. Zaimportuj go przez Kopia zapasowa i przywracanie > Importuj ustawienia”.

#### Przygotuj własny plik z elementami {#profiles-item-reference}

Opcja **Kopia zapasowa i przywracanie** > **Opis formatu pliku eksportu** zapisuje dokument `sandfly-file-format.md`. Zawiera opis plików poszczególnych elementów, a dalej listy gestów, poleceń i ustawień dostępnych na tym telefonie, z bieżącą wartością każdego ustawienia. Aby utworzyć elementy za pomocą czatu AI, udostępnij mu ten dokument i opisz, czego potrzebujesz. Zapisz odpowiedź jako plik i wczytaj ją przez **Importuj z pliku**.

Dokładny opis formatu znajdziesz w [](#api-item-files).
