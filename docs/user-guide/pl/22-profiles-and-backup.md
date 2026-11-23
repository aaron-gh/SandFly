## Profile ustawień i kopie zapasowe {#profiles}

Profil zmienia wybrane ustawienia SandFly. Możesz włączyć go wszędzie albo tylko w określonych aplikacjach i sytuacjach. Kopia zapasowa służy do zapisania i przeniesienia ustawień czytnika. Osobne pliki mogą zawierać na przykład same makra albo profile gestów.

[Profile gestów](#gestures) zmieniają przypisania gestów, a [profile głosowe](#speech) ustawienia głosu.

### Co zawiera profil ustawień {#profiles-settings-profiles}

Profil zawiera ustawienia, które w nim zmienisz. W pozostałych sprawach nadal obowiązują ustawienia ogólne. Na liście **Profile ustawień** SandFly oznacza pozycje używane w tej chwili.

[**Użyj teraz**](#profiles-use) włącza jeden profil ręcznie. [**Używaj automatycznie**](#profiles-automatic) pozwala włączać profile między innymi na stronach, podczas czytania ciągłego i podczas edycji. Możesz też wybrać aplikacje lub klawiatury.

W profilu można zmienić ustawienia SandFly, klawiatury i monitora brajlowskiego, gesty, skróty, profile gestów oraz wybór aktywnego profilu gestów. Profil nie obejmuje kluczy API, licencji Pro, czasu ostatniego sprawdzenia aktualizacji, skryptów i ich ustawień, roboczych motywów dźwiękowych ani stanu urządzenia, na przykład informacji o wyświetlonych samouczkach.

### Nowy profil {#profiles-add}

Otwórz **Profile ustawień** > **Nowy profil ustawień**. Wpisz nazwę albo pozostaw pustą, by przyjąć podpowiedź SandFly. Wybierz **OK**. Na początku profil jest pusty. Możesz teraz [dodać do niego ustawienia](#profiles-change-settings).

### Zmiana ustawień profilu {#profiles-change-settings}

1. Wybierz profil i użyj **Zmień ustawienia**.
2. SandFly otworzy ustawienia z włączonym profilem i poda jego nazwę. Wszystkie wprowadzane zmiany trafią do niego.
3. Wróć na ekran profilu lub do listy albo otwórz inną aplikację. SandFly potwierdzi zapis.

Gdy zmienisz ustawienie w trakcie używania profilu, na przykład tempo mowy podczas czytania, zmiana trafi do profilu, jeśli to on ustawia tempo. Pozostałe zmiany trafiają do ustawień ogólnych.

W części **Ustawienia w tym profilu** zobaczysz nazwy zmienionych opcji i ich miejsce w menu. Żeby znów korzystać z ustawienia ogólnego, wybierz wpis w profilu i użyj **Usuń**.

### Ręczne włączanie profilu {#profiles-use}

Wybierz profil i użyj **Użyj teraz**. Zadziała wszędzie, również w ustawieniach SandFly. **Przestań używać** na tym samym ekranie wyłącza go.

W kategorii [SandFly](#commands) znajdziesz też **Wybierz profil ustawień**, **Następny profil ustawień** i **Poprzedni profil ustawień**. Początkowo nie mają przypisanych gestów. Obejmują opcję **Brak profilu ustawień**, a przy wyborze czytnik podaje nowy profil.

### Automatyczny wybór profilu {#profiles-automatic}

Na ekranie profilu w opcji **Używaj automatycznie** możesz wskazać treści WWW, **Czytanie ciągłe** lub **Edytowanie tekstu**. W ostatnim przypadku profil działa w polu edycji i na wpisującej do niego klawiaturze.

**Aplikacje** > **Dodaj aplikację** stosuje profil, gdy wybrana aplikacja jest na ekranie. **Klawiatury** > **Dodaj klawiaturę** robi to samo dla klawiatury ekranowej. Lista pokazuje klawiatury włączone w Androidzie. W obu przypadkach wybierz wpis i **Usuń**, żeby wyłączyć powiązanie.

Profile automatyczne nie obowiązują w ustawieniach SandFly ani przy wyłączonym czytniku.

#### Kolejność profili {#profiles-order}

Może działać kilka profili jednocześnie. Gdy ustawiają tę samą opcję, pierwszeństwo ma profil włączony przez **Użyj teraz**. Potem liczy się kolejność na liście **Profile ustawień**. Użyj **Przenieś w górę** lub **Przenieś w dół**, aby ją zmienić.

### Zmiana nazwy, kolejności i usuwanie {#profiles-manage}

Na ekranie profilu użyj **Zmień nazwę**, opcji przenoszenia lub **Usuń**. Przed usunięciem SandFly poprosi o potwierdzenie. **Eksportuj do pliku** zapisuje [sam profil](#profiles-item-files).

### Kopia zapasowa i przywracanie {#profiles-backup}

Na ekranie **Kopia zapasowa i przywracanie** możesz zapisać ustawienia i własną zawartość SandFly albo wczytać je z innego urządzenia. Te opcje nie są dostępne na zegarkach.

#### Eksport ustawień {#profiles-export}

Otwórz **Kopia zapasowa i przywracanie** > **Eksportuj ustawienia** i wybierz miejsce zapisu. Sugerowana nazwa to `sandfly-settings.zip`.

Archiwum zawiera ustawienia główne, własne etykiety, osobno zapisane profile, zainstalowane skrypty, motywy dźwiękowe i zmiany w motywie Backtalk oraz obraz tapety. Ustawienia główne są zapisane w takiej postaci, w jakiej obowiązują bez profili.

Klucze API pozostają na urządzeniu. Kopia pomija też licencję [SandFly Pro](#pro), czas ostatniego sprawdzenia aktualizacji, pobrane modele AI i rozpoznawania mowy, dzienniki, zgłoszenia oraz robocze motywy dźwiękowe.

#### Import ustawień {#profiles-import}

Otwórz **Kopia zapasowa i przywracanie** > **Importuj ustawienia** i wybierz plik. Ustawienia z pliku zastąpią odpowiadające im ustawienia telefonu. Te, których plik nie zawiera, i klucze API zostają. Aktywne profile zostaną ponownie zastosowane.

Importowane etykiety, profile, skrypty i motywy zastępują pozycje o tej samej nazwie, identyfikatorze lub przypisaniu. Pozostałe pozycje zostają. Obraz tapety z pliku zastępuje obecny.

SandFly przyjmuje również starszy plik `.json` oraz kopię z Backtalka. Własne ustawienia SandFly, których Backtalk nie ma, pozostają. Jeśli plik jest uszkodzony, ma zły format, przekracza 5 MB dla JSON lub 300 MB po rozpakowaniu ZIP, SandFly zgłosi błąd bez wprowadzania zmian.

### Pliki pojedynczych elementów {#profiles-item-files}

Do udostępniania wybranej zawartości służą **Eksportuj do pliku** i **Importuj z pliku**. Taki plik możesz zmienić lub napisać samodzielnie. Pełną kopię ustawień otwiera się przez **Importuj ustawienia**.

| Zawartość | Gdzie jej szukać |
| --- | --- |
| Makra | **Makra** |
| Profile gestów | **Gesty** > **Profile gestów** |
| Gesty w aplikacjach | **Gesty** > **Gesty w aplikacjach** |
| Skróty w aplikacjach | **Skróty klawiszowe** > **Skróty klawiszowe w aplikacjach** |
| Mapa klawiatury | **Skróty klawiszowe**. Eksport dotyczy bieżącej mapy. |
| Profile ustawień | **Profile ustawień** |
| Słownik mowy | **Mowa** > **Słownik mowy** |
| Słownik symboli | **Mowa** > **Słownik symboli** |
| Słownik brajlowski | **Brajl** > **Słownik brajlowski** |
| Własne znaki brajlowskie | **Brajl** > **Klawiatura brajlowska** > **Własne znaki brajlowskie** |
| Własne polecenia głosowe | **Polecenia głosowe** |

Plików pojedynczych elementów nie obsługują zegarki.

#### Eksport wybranych pozycji {#profiles-item-export}

Otwórz odpowiedni ekran, wybierz **Eksportuj do pliku** i wskaż miejsce zapisu. Na ekranie listy zapiszesz całą listę. Z ekranu pojedynczej pozycji, na przykład profilu ustawień, zapiszesz tylko tę pozycję. SandFly poda wynik.

#### Import wybranych pozycji {#profiles-item-import}

Na dowolnym [ekranie z tabeli](#profiles-item-files) użyj **Importuj z pliku**. SandFly doda całą zawartość, również jeśli plik zawiera inny rodzaj pozycji niż otwarty ekran. Potem poda, co dodał.

Nowy wpis zastępuje pozycję tego samego rodzaju o tej samej nazwie bez względu na wielkość liter. W słowniku liczy się szukany tekst i aplikacja, dla znaków brajlowskich tekst i tabela, a dla poleceń głosowych fraza. Mapy klawiatury trafiają do własnych map.

Po imporcie SandFly podaje pominięte wpisy, na przykład nieznane polecenia, ustawienia, gesty, skróty lub nieobsługiwane wyrażenia regularne. Jeśli wybrano kopię ustawień, skieruje do **Importuj ustawienia**.

#### Jak przygotować własny plik {#profiles-item-reference}

**Kopia zapasowa i przywracanie** > **Opis formatu pliku eksportu** zapisuje `sandfly-file-format.md`. Dokument opisuje format i zawiera listę gestów, poleceń i ustawień dostępnych na urządzeniu wraz z bieżącymi wartościami. Jeżeli tworzysz plik z pomocą czatu AI, podaj mu ten opis oraz listę potrzebnych pozycji. Zapisz wynik i otwórz go przez **Importuj z pliku**.

Zobacz także [specyfikację plików](#api-item-files).
