## Dźwięk, wibracje i wyjście audio {#sound}

SandFly sygnalizuje dźwiękiem i wibracją takie zdarzenia jak przeniesienie fokusu, aktywowanie elementu, przewinięcie listy czy otwarcie okna. Na ekranie **Dźwięk i wibracje** możesz włączać lub wyłączać te sygnały, zmieniać ich głośność i siłę oraz ustawiać motywy dźwiękowe i dźwięki kontrolek. Ekran **Wyjście audio** pozwala wybrać urządzenie odtwarzające, ustawić głośność i zdecydować, co dzieje się z pozostałym dźwiękiem i rozmowami, gdy SandFly jest włączony.

Syntezator, głosy, szybkość i głośność mowy oraz efekty głosu opisuje [](#speech). O przyciskach głośności i opcji **Informacja zwrotna o zmianie głośności** przeczytasz w [](#buttons).

### Włącz lub wyłącz dźwięki {#sound-feedback}

W grupie **Dźwięki** na ekranie **Dźwięk i wibracje** znajdziesz poniższe ustawienia.

- **Sygnał dźwiękowy** włącza dźwięki SandFly. Jest domyślnie włączony. Po wyłączeniu nadal działają wibracje.
- **Głośność sygnału dźwiękowego** pozwala ustawić od 5% do 100%, co pięć punktów procentowych. Domyślna wartość to 50%.
- **Tematy dźwiękowe** otwierają listę zainstalowanych motywów, ustawienia dźwięków kontrolek i dźwięku przestrzennego. Zobacz [](#sound-themes).
- **Dźwięk „Dostępne czynności”** określa, kiedy słychać dźwięk **Dostępne czynności**. Możesz wybrać **Wyłączony**, **Gdy element zostanie zaznaczony** (domyślnie) lub **Po odczytaniu elementu**. Zobacz [](#sound-actions-available).

Skrót klawiszowy **Włącz lub wyłącz sygnał dźwiękowy (ikony audio)** przełącza opcję **Sygnał dźwiękowy**. Zobacz [](#appendix-keyboard-shortcuts).

Jeśli chcesz wyłączyć pojedynczy dźwięk, zobacz [](#sound-individual).

### Zmień wibracje {#sound-vibration}

Poniższe ustawienia znajdziesz w grupie **Wibracje** na ekranie **Dźwięk i wibracje**. Na urządzeniu bez silnika wibracji ta grupa jest ukryta.

- **Sygnał wibracyjny** włącza wibracje SandFly. Domyślnie jest włączony. Po wyłączeniu nadal działają dźwięki.
- **Siła wibracji** ustawia natężenie od 5% do 200%. Domyślnie jest to 100%. Opcja jest widoczna, jeśli urządzenie pozwala zmieniać siłę wibracji.
- **Typ wibracji** wybiera sposób odtwarzania. Na liście są tylko typy obsługiwane przez urządzenie. Gdy dostępny jest jeden typ, opcja jest ukryta. Dopóki nic nie wybierzesz, SandFly używa pierwszego typu z listy.
  - **Efekty haptyczne** to złożone wzory, np. kliknięcia lub krótkie stuknięcia. Są dostępne od Androida 11, jeśli silnik wibracji je obsługuje.
  - **Impulsy** to wibracje o zmiennej sile. Wymagają silnika z regulacją siły.
  - **Włączanie i wyłączanie** uruchamia i zatrzymuje silnik przy pełnej sile. Ten typ obsługuje każdy silnik wibracji.

Jeśli przy ustawieniu **Efekty haptyczne** wibracje są za słabe, wybierz **Impulsy** i zwiększ wartość **Siła wibracji**.

Polecenie **Sygnał wibracyjny** z grupy **Mowa** włącza i wyłącza wibracje. Zobacz [](#commands).

### Dźwięki i wibracje {#sound-events}

Każdy dźwięk ma własną wibrację, którą poczujesz również po wyłączeniu **Sygnał dźwiękowy**. Dzięki temu możesz rozpoznawać zdarzenia przy wyciszonym dźwięku. Niektóre wibracje, np. dla komunikatów i klawiatury brajlowskiej, występują samodzielnie.

Poniższe nazwy znajdziesz w ustawieniach **Poszczególne dźwięki i wibracje** oraz na liście dźwięków motywu.

| Dźwięk | Kiedy go słychać |
| --- | --- |
| **Zaznaczenie elementu** | Fokus przechodzi na element. |
| **Zaznaczenie elementu z dostępną czynnością** | Fokus przechodzi na element, który możesz aktywować. |
| **Dostępne czynności** | Fokus przechodzi na element z czynnościami aplikacji, np. z odpowiedzią na wiadomość. Zobacz [](#sound-actions-available). |
| **Dotknięcie pustego obszaru** | Dotykasz miejsca bez elementu. |
| **Kliknięcie** | Aktywujesz element. |
| **Przytrzymanie** | Naciskasz i przytrzymujesz element. |
| **Przewijanie** | Lista się przewija. |
| **Wejście do listy** i **Wyjście z listy** | Fokus wchodzi na listę lub z niej wychodzi. |
| **Działanie wykonane lub osiągnięto koniec** | Czynność się kończy lub fokus dociera do końca listy. |
| **Zmiana okna** | Otwiera się nowe okno lub ekran. |
| **Następny element sterowania czytaniem** i **Poprzedni element sterowania czytaniem** | Wybierasz inną pozycję sterowania czytaniem. |
| **Gest krawędzi na dole**, **Środkowy gest krawędzi** i **Góra gestu krawędzi** | Kolejne etapy gestu krawędziowego. Niedostępne na zegarkach. |
| **Punkt wstawiania został przeniesiony** | Nawigacja po znakach, słowach, wierszach lub akapitach przesuwa kursor w edytowanym polu. |
| **Przejście do następnego wiersza** i **Przejście do poprzedniego wiersza** | Przesunięcie palca przenosi fokus do wiersza niżej lub wyżej. |
| **Zadanie agenta rozpoczęte**, **Zadanie agenta ukończone**, **Pytanie agenta**, **Agent zatrzymany przy wrażliwej czynności** i **Zadanie agenta nieudane** | Zdarzenia agenta w kompilacjach z SandFly Pro. Zobacz [](#pro). |
| **Błąd pisowni** | W tekście jest źle napisane słowo. |
| **Link** | W tekście jest link. |
| **Sformatowany tekst** | Tekst ma formatowanie, np. pogrubienie. |
| **Emoji** | Po emoji, gdy w opcji **Szczegółowość** > **Po nazwach emoji** wybrano **Odtwórz dźwięk**. Brak wbudowanej wibracji. Motyw dźwiękowy może ją dodać. Zobacz [](#verbosity-emoji). |
| **Ekran wyłączony** i **Ekran włączony** | Ekran się wyłącza lub włącza. |
| **Ekran ukryty** i **Ekran pokazany** | Ekran zostaje ukryty lub ponownie pokazany. Zobacz [](#display). |
| **Wczytywanie** | Trwa wczytywanie opisu lub innego wyniku. |
| **Ładowanie modelu AI na urządzeniu** | Trwa wczytywanie lokalnego modelu AI. |
| **Tryb przeglądania włączony** i **Tryb przeglądania wyłączony** | Włączasz lub wyłączasz tryb przeglądania. Zobacz [](#web). |
| **Klawiatura otwarta** i **Klawiatura zamknięta** | Klawiatura ekranowa się otwiera lub zamyka. |
| **Powiadomienie SandFly** | SandFly publikuje powiadomienie w kategorii bez przypisanego dźwięku. |
| **Pełne naładowanie** | Bateria jest w pełni naładowana i włączono **Informuj o pełnym naładowaniu**. Zobacz [](#verbosity-announcements). |
| **Menu kołowe** | Wybierasz jedną z pozycji menu kołowego, sygnalizowanych ośmioma dźwiękami. |
| **Monitor brajlowski połączony**, **Monitor brajlowski odłączony**, **Polecenie brajlowskie nie powiodło się**, **Automatyczne przewijanie brajla włączone**, **Automatyczne przewijanie brajla wyłączone** i **Klawiatura brajlowska skalibrowana** | Zdarzenia monitora i klawiatury brajlowskiej. Zobacz [](#braille). |
| Od **Dźwięk kontrolki: przycisk** po **Dźwięk kontrolki: element drzewa** | Dźwięki poszczególnych typów kontrolek. Zobacz [](#sound-control-sounds). |

#### Dźwięki nawigacji {#sound-navigation-sounds}

SandFly nie odtwarza dźwięku przy rozpoczęciu ani zakończeniu gestu. W trakcie nawigacji możesz natomiast usłyszeć następujące sygnały.

- **Następny element sterowania czytaniem** i **Poprzedni element sterowania czytaniem** słychać po zmianie pozycji gestem, w menu kołowym, skrótem klawiszowym albo klawiaturą brajlowską.
- Dźwięk gestu krawędziowego słychać po dotknięciu miejsca, od którego zaczyna się gest (**Gest krawędzi na dole**, niski dźwięk), po osiągnięciu pierwszego etapu (**Środkowy gest krawędzi**) oraz po osiągnięciu drugiego (**Góra gestu krawędzi**). Cofnięcie palca odtwarza dźwięk etapu, do którego wracasz. Po uniesieniu palca nic nie słychać. Zobacz [](#gestures).
- **Przejście do następnego wiersza** słychać, gdy przesunięcie do następnego elementu przenosi fokus do niższego wiersza. **Przejście do poprzedniego wiersza** słychać, gdy przesunięcie wstecz przenosi go wyżej. Każda pozycja listy stanowi osobny wiersz. Elementy ułożone obok siebie, np. ikony ekranu głównego lub komórki tabeli, są w jednym wierszu. Sygnał gra razem z dźwiękiem fokusu, a jego wibracja zastępuje wibrację fokusu. Sygnał jest pomijany przy przejściu do innego okna.

#### Dźwięk „Dostępne czynności” {#sound-actions-available}

Dźwięk **Dostępne czynności** sygnalizuje, że poza zwykłą obsługą element ma dodatkowe czynności aplikacji. Przykładem są odpowiedź i przekazanie wiadomości w komunikatorze. Zwykłe czynności, takie jak aktywowanie, przytrzymanie, przewijanie i rozwijanie, nie wywołują tego sygnału. Nie słychać go więc na zwykłym przycisku ani na klawiszu klawiatury ekranowej. Dodatkowe czynności znajdziesz w pozycji **Czynności** w sterowaniu czytaniem i w menu SandFly.

SandFly domyślnie nie wibruje przy tym dźwięku. Motyw dźwiękowy może dodać wibrację.

Opcja **Dźwięk i wibracje** > **Dźwięk „Dostępne czynności”** określa, kiedy odtworzyć dźwięk.

- **Wyłączony** pomija ten dźwięk.
- **Gdy element zostanie zaznaczony** odtwarza sygnał po dźwięku fokusu. To ustawienie domyślne. Jeśli motyw nadaje mu wibrację, zastępuje ona wibrację fokusu.
- **Po odczytaniu elementu** odtwarza sygnał po przeczytaniu pełnego opisu. Jeśli przejdziesz dalej wcześniej, sygnału nie będzie.

Aby SandFly odczytywał słowa „Dostępne czynności”, zobacz [](#verbosity-items).

#### Krótkie sygnały podczas przewijania {#sound-scroll-ticks}

Przewijając listę dwoma palcami, usłyszysz krótki dźwięk przy każdym mijanym elemencie. Im szybciej przesuwasz palce, tym częściej słychać sygnały. Na liście, która nie udostępnia pozycji, dźwięk słychać co 48 dp przesunięcia. Pierwszy sygnał pojawia się, gdy tylko przesunięcie dotrze do aplikacji. Razem z krótkimi sygnałami gra dźwięk przewijania, najwyżej co 250 ms. Jego ton rośnie od początku do końca listy. Gdy SandFly przewija listę sam, odtwarza tylko dźwięk przewijania.

### Wyłącz pojedyncze dźwięki lub wibracje {#sound-individual}

Na ekranie **Dźwięk i wibracje** > **Poszczególne dźwięki i wibracje** w grupie **Dźwięki** jest przełącznik każdego dźwięku, a w grupie **Wibracje** przełącznik każdej wibracji. Domyślnie wszystkie są włączone. Wyłączenie dźwięku zachowuje odpowiadającą mu wibrację, a wyłączenie wibracji zachowuje dźwięk. **Sygnał dźwiękowy** i **Sygnał wibracyjny** nadal pozwalają wyłączyć odpowiednio wszystkie dźwięki lub wszystkie wibracje naraz.

Aby odtworzyć sygnał przed zmianą ustawienia, wykonaj te kroki.

1. Przenieś fokus na przełącznik.
2. Otwórz menu czynności.
3. Wybierz **Odsłuchaj**.

Zmiana ustawienia przełącznika nie odtwarza sygnału.

Na liście **Wibracje** znajdziesz też przełączniki wibracji bez dźwięku, np. **Komunikat**, **Gest krawędzi: przesunięcie wstecz** oraz wibracje klawiatury brajlowskiej, takie jak **Klawiatura brajlowska: wpisywanie znaku** i **Klawiatura brajlowska: brak tekstu do usunięcia**. Wyłączenie wibracji wyłącza również wibrację podstawioną w jej miejsce przez motyw.

### Motywy dźwiękowe {#sound-themes}

Motyw dźwiękowy to zestaw dźwięków i wibracji zastępujących wbudowane sygnały. Każdy motyw ma też własne ustawienia **Dźwięki i wibracje kontrolek**, **Dźwięk przestrzenny 3D** i **Nadal odczytuj typ kontrolki**. Wbudowane sygnały należą do motywu **SandFly**, którego nie można usunąć.

Ekran **Dźwięk i wibracje** > **Tematy dźwiękowe** składa się z poniższych części.

- **Utwórz nowy temat** tworzy motyw. Zobacz [](#sound-themes-create).
- **Zainstalowane tematy** pokazują wszystkie motywy. Używany motyw ma odpowiednie oznaczenie w podsumowaniu.
- Grupa **Ustawienia tematu** wraz z nazwą aktywnego motywu zawiera ustawienia dźwięków kontrolek oraz listę dźwięków. Zobacz [](#sound-control-sounds) i [](#sound-themes-replace).
- **Zainstaluj temat** pozwala wybrać **Z pliku**, **Z linku** lub **Ze źródła**. Zobacz [](#sound-themes-install).

#### Zainstaluj motyw dźwiękowy {#sound-themes-install}

Motyw jest zapisany w pliku ZIP. Zainstalujesz go na jeden z poniższych sposobów.

- **Z pliku** otwiera systemowy wybór plików. Wskaż plik motywu.
- **Z linku** pozwala wpisać lub wkleić link do pliku motywu albo repozytorium GitHub zawierającego motyw. Link musi zaczynać się od `https`. Wybierz **Zainstaluj**.
- **Ze źródła** pokazuje listę repozytoriów z GitHuba, GitLaba, Codeberga, Gitei lub Bitbucket. Wybierz **Dodaj źródło** i podaj adres repozytorium, np. `github.com/owner/repository`. Wybierz źródło, aby zobaczyć pliki motywów, a następnie wybierz plik, aby go zainstalować. Na liście jest początkowo repozytorium motywów SandFly. **Usuń źródło** usuwa je z listy i zachowuje motywy z niego zainstalowane.
- Otwórz plik motywu za pomocą SandFly z menedżera plików lub z pobranych plików przeglądarki. Możesz też udostępnić plik lub link do SandFly z innej aplikacji. Wybierz **Zainstaluj temat dźwiękowy**.

Przed instalacją zobaczysz autora, opis, liczbę dźwięków i wibracji, licencję oraz pominięte pliki. Wybierz **Zainstaluj i użyj**, aby od razu przełączyć motyw, lub **Zainstaluj**, aby pozostać przy bieżącym. Motyw o tej samej nazwie zastąpi poprzedni.

Uwaga: Zegarki nie mają menedżera wyboru plików. Instaluj na nich motywy z linku. Opcja **Ze źródła** jest dostępna tylko na telefonach.

#### Wybierz motyw lub zmień jego ustawienia {#sound-themes-manage}

1. Otwórz **Dźwięk i wibracje** > **Tematy dźwiękowe**.
2. W sekcji **Zainstalowane tematy** wybierz motyw.
3. Wybierz jedną z opcji.
   - **Użyj tego tematu** włącza motyw wraz z jego ustawieniami dźwięków kontrolek.
   - **Informacje o temacie** pokazują autora, opis, liczbę dźwięków i wibracji, licencję i stronę internetową.
   - **Zapisz jako plik tematu** zapisuje motyw w pliku ZIP do udostępnienia.
   - **Usuń** usuwa motyw. Ta opcja jest niedostępna dla motywu **SandFly**.

#### Zastąp wybrane dźwięki {#sound-themes-replace}

Każdy dźwięk używanego motywu możesz zastąpić plikiem. Jeśli chcesz zmienić dźwięki bez instalowania motywu, użyj motywu **SandFly**.

1. Otwórz **Dźwięk i wibracje** > **Tematy dźwiękowe**.
2. W ustawieniach używanego motywu wybierz **Dźwięki**.
3. Wybierz dźwięk. Tytuł okna podaje nazwę dźwięku i nazwę jego pliku, np. „Kliknięcie (tick)”.
4. Wybierz jedną z opcji.
   - **Wybierz plik dźwiękowy** otwiera systemowy wybór plików. Plik może mieć do 5 MB i być w formacie WAV, OGG, MP3 lub FLAC.
   - **Wybierz z folderu** pokazuje pliki dźwiękowe i podfoldery wybranego folderu. Za pierwszym razem otwiera się systemowy wybór folderu. Zobacz [](#sound-themes-folder).
   - **Zmień folder** otwiera systemowy wybór folderu, w którym wskażesz inny folder dla **Wybierz z folderu**. Opcja jest widoczna po wybraniu folderu.
   - **Odsłuchaj** odtwarza dźwięk.
   - **Brak dźwięku** wycisza to zdarzenie.
   - **Użyj dźwięku SandFly** przywraca wbudowany dźwięk.

W podsumowaniu każdego dźwięku jest **Dźwięk SandFly**, **Dźwięk tematu** lub **Brak dźwięku**. Te same działania są w menu czynności dźwięku pod nazwami **Odsłuchaj**, **Ustaw brak dźwięku**, **Użyj dźwięku SandFly** i **Wybierz plik dźwiękowy**. Opcja **Przywróć wszystkie dźwięki SandFly** usuwa zmiany dźwięków motywu i przywraca wbudowane.

Na tej liście znajdziesz również dźwięki spoza ekranu **Poszczególne dźwięki i wibracje**. **Zmiana głośności** jest odtwarzana przy każdym kroku regulacji, gdy w opcji **Informacja zwrotna o zmianie głośności** wybrano sygnał. Zobacz [](#buttons). Dźwięki klawiatury brajlowskiej słychać po włączeniu **Dźwięki pisania** w jej ustawieniach. Zobacz [](#braille).

#### Wybierz dźwięki z folderu {#sound-themes-folder}

**Wybierz z folderu** pokazuje zawartość jednego folderu w telefonie. Wybierz dźwięk, a potem **Wybierz z folderu**. Za pierwszym razem wskaż folder w systemowym wyborze folderu.

Lista pokazuje najpierw podfoldery, a potem pliki dźwiękowe, w obu grupach według nazwy. Z jednego folderu wyświetla się do 2000 pozycji.

- Plik, na którym jest fokus ułatwień dostępu, jest odtwarzany. Przeniesienie fokusu z pliku zatrzymuje odtwarzanie.
- Wybierz plik, aby użyć go dla dźwięku.
- Wybierz podfolder, aby wyświetlić jego zawartość. **Folder nadrzędny** lub Wstecz wraca do folderu wyżej.

Jeśli folder zostanie usunięty albo SandFly nie może go już odczytać, systemowy wybór folderu otworzy się ponownie.

#### Utwórz motyw dźwiękowy {#sound-themes-create}

1. Otwórz **Dźwięk i wibracje** > **Tematy dźwiękowe**.
2. Wybierz **Utwórz nowy temat**.
3. W polu **Nazwa** wpisz nazwę inną niż nazwy zainstalowanych motywów. Pola **Opis**, **Autor** i **Strona internetowa** są opcjonalne.
4. Wybierz **OK**, aby otworzyć listę dźwięków motywu.
5. Ustaw **Dźwięki i wibracje kontrolek**, **Dźwięk przestrzenny 3D** i **Nadal odczytuj typ kontrolki**. Wybierz plik dla każdego dźwięku, który chcesz zastąpić.
6. Wybierz **Zapisz**, aby zapisać i włączyć motyw, albo **Zapisz jako plik tematu**, aby zapisać go jako ZIP bez włączania.

Aby wyjść bez zapisu, wróć i wybierz **Odrzuć**. Przycisk **Edytuj dalej** wraca do motywu. Jeśli zostawisz **Dźwięki i wibracje kontrolek** bez zmian, SandFly włączy je podczas zapisu motywu zawierającego dźwięki kontrolek.

#### Pliki motywów {#sound-themes-files}

Plik motywu to archiwum ZIP z plikiem `theme.json` i plikami dźwiękowymi w tym samym katalogu. W archiwum może to być też jeden katalog niżej, jak w plikach ZIP eksportowanych przez GitHuba. Repozytorium z plikiem `theme.json` i dźwiękami w katalogu głównym jest więc gotowym motywem. Możesz też zainstalować ZIP z samymi dźwiękami. Motyw otrzyma wtedy nazwę pliku.

- Nazwij każdy plik tak jak zastępowany dźwięk. Użyj rozszerzenia jego formatu, np. `focus.wav` lub `tick.ogg`. Nazwę pliku zobaczysz w oknie dźwięku na ekranie **Dźwięki**. Motyw może zawierać tylko dźwięki, które zmienia.
- Obsługiwane są WAV, OGG, MP3, FLAC, M4A, AAC i Opus. Limit to 5 MB na plik i 50 MB na motyw. Pliki, których Android nie może odtworzyć, są pomijane przy instalacji.
- W `theme.json` można określić nazwę, autora, opis, licencję i stronę motywu. Można także ustawić dźwięki kontrolek i wibracje oraz wskazać, które zdarzenia mają być bez dźwięku. Każda wibracja może mieć do trzech postaci dla różnych typów wibracji.
- Pliki licencji i opisu, np. `LICENSE.txt` lub `README.md`, zostają w motywie. Pozostałe pliki są pomijane.

Pełny format wraz z nazwami wszystkich dźwięków i wibracji opisuje [dokumentacja motywów dźwiękowych](https://github.com/aaron-gh/SandFly/blob/master/themes.md).

### Dźwięki i wibracje kontrolek {#sound-control-sounds}

Gdy dźwięki kontrolek są włączone, po przeniesieniu fokusu na kontrolkę usłyszysz dźwięk i poczujesz wibrację jej typu. Zastępują one zwykłe sygnały fokusu. SandFly może wtedy pominąć czytanie typu, np. słowa „przycisk”. Dostępne są dźwięki dla przycisków, pól wyboru (także przełączników), przycisków opcji, pól edycji, list rozwijanych, suwaków, obrazów, zegarów, kart, pozycji menu i listy, a na stronach także linków i elementów drzewa.

1. Otwórz **Dźwięk i wibracje** > **Tematy dźwiękowe**.
2. W ustawieniach używanego motywu włącz **Dźwięki i wibracje kontrolek**. Opcja jest domyślnie wyłączona.

W ustawieniach bieżącego motywu znajdziesz trzy związane opcje.

- **Dźwięki i wibracje kontrolek** włączają odpowiednie sygnały.
- **Dźwięk przestrzenny 3D** określa, kiedy odtwarzać dźwięk z położenia kontrolki. Zobacz [](#sound-3d-audio).
- **Nadal odczytuj typ kontrolki** dodaje nazwę jej typu po dźwięku. Domyślnie jest włączona w motywie **SandFly**, a w innych wyłączona, chyba że motyw ustawia ją inaczej.

Jeśli element sam nie jest kontrolką, np. wiersz ustawień, SandFly odtwarza dźwięk pierwszej kontrolki w środku, takiej jak przełącznik. Gdy nie ma kontrolki, używa dźwięku typu elementu, np. pozycji listy. Klawisze klawiatury ekranowej zachowują zwykły dźwięk fokusu. Wibracja kontrolki działa również po wyłączeniu **Sygnał dźwiękowy**. Jeśli dźwięk danego typu wyłączysz w **Poszczególne dźwięki i wibracje** lub motyw ustawia go jako **Brak dźwięku**, SandFly odtworzy zwykły dźwięk fokusu i odczyta typ kontrolki.

Każdy motyw zapamiętuje własne ustawienia dźwięków kontrolek. Przy zmianie motywu zmieniają się też te ustawienia.

#### Dźwięk przestrzenny 3D {#sound-3d-audio}

Dźwięk może dochodzić z miejsca, w którym znajduje się kontrolka. Jeśli kontrolka jest po lewej, słychać ją z lewej strony, a jeśli jest wysoko, dźwięk dochodzi z góry. Tryb 3D wykorzystuje pomiary tego, jak dźwięk z różnych kierunków dociera do głowy. Bez niego położenie jest oddawane tylko w poziomie.

W opcji **Dźwięk przestrzenny 3D** wybierz jedno z poniższych ustawień.

- **Podczas używania słuchawek** to opcja domyślna. Dźwięk 3D działa, gdy SandFly odtwarza przez słuchawki przewodowe lub USB, zestaw lub słuchawki Bluetooth albo aparaty słuchowe. Od Androida 13 SandFly sprawdza, gdzie faktycznie odtwarza. Podłączony głośnik Bluetooth lub zestaw samochodowy, przez który nie gra dźwięk SandFly, się nie liczy. W starszym Androidzie liczą się dowolne podłączone słuchawki.
- **Zawsze** włącza dźwięk 3D również w głośniku telefonu.
- **Nigdy, tylko lewy i prawy kanał** zachowuje położenie w lewo i w prawo.

### Ustawienia wyjścia audio {#sound-audio-output}

Na ekranie **Wyjście audio** w ustawieniach SandFly znajdziesz poniższe opcje.

- **Urządzenie odtwarzające** określa, gdzie słychać mowę i dźwięki. Zobacz [](#sound-output-device).
- **Głośnik telefonu przy tych urządzeniach** wybiera urządzenia, których podłączenie przenosi dźwięk SandFly na głośnik telefonu. Zobacz [](#sound-speaker-devices).
- **Włączaj tryb głośnomówiący po odsunięciu telefonu od ucha** przełącza rozmowy między słuchawką telefonu a jego głośnikiem. Zobacz [](#sound-calls).
- **Odtwarzaj mowę tylko na telefonie podczas przesyłania ekranu** pozostawia dźwięk SandFly na telefonie. Zobacz [](#sound-casting).
- **Resetuj dźwięk ułatwień dostępu po połączeniach** przywraca wyjście dźwięku po rozmowie. Zobacz [](#sound-calls).
- **Regulacja głośności mowy** ustala, której głośności używają mowa i dźwięki. Zobacz [](#sound-speech-volume).
- **Wyciszanie tła** ścisza inne dźwięki na czas mowy SandFly. Zobacz [](#sound-ducking).
- **Dźwięk o niskim opóźnieniu** przyspiesza rozpoczęcie odtwarzania. Zobacz [](#sound-low-latency).

### Wybierz urządzenie odtwarzające {#sound-output-device}

W menu **Wyjście audio** > **Urządzenie odtwarzające** wybierasz, gdzie słychać mowę i dźwięki SandFly.

- **Domyślne urządzenie systemowe** pozostawia wybór Androidowi. To wartość domyślna.
- **Głośnik telefonu** odtwarza dźwięk na głośniku telefonu również przy podłączonych słuchawkach Bluetooth lub przewodowych.
- Możesz wybrać po nazwie podłączone urządzenie, np. Bluetooth, przewodowe, USB lub aparat słuchowy. Lista pokazuje aktualnie podłączone urządzenia.

Po wybraniu urządzenia Bluetooth dźwięk jest kierowany przez Androida, tak jak przy **Domyślne urządzenie systemowe**. Dzięki temu urządzenie nie przechodzi w tryb rozmowy. Jeśli Android skieruje dźwięk na inne urządzenie mimo połączenia z wybranym, dźwięk pozostanie na drugim urządzeniu. Urządzenia przewodowe, USB i aparaty słuchowe otrzymują dźwięk bezpośrednio.

Wybór zostaje zapisany po podłączeniu lub odłączeniu urządzenia oraz po rozpoczęciu odtwarzania w innej aplikacji. Jeśli wybrane urządzenie jest odłączone, przy jego nazwie pojawia się informacja o odłączeniu. Do chwili ponownego połączenia dźwięk kieruje Android. Android wybiera też wyjście podczas rozmów i gdy inna aplikacja używa mikrofonu na potrzeby rozmowy lub asystenta głosowego.

Pozycja **Urządzenie odtwarzające** w sterowaniu czytaniem zmienia wyjście po przesunięciu palcem w górę lub w dół. Zobacz [](#sound-reading-controls).

### Odtwarzaj przez głośnik telefonu przy wybranych urządzeniach {#sound-speaker-devices}

Ekran **Wyjście audio** > **Głośnik telefonu przy tych urządzeniach** pokazuje aktualnie podłączone urządzenia Bluetooth, przewodowe i USB oraz urządzenia podłączane wcześniej, gdy SandFly był włączony. Gdy zaznaczone urządzenie się połączy, mowa i dźwięki SandFly przechodzą na głośnik telefonu niezależnie od opcji **Urządzenie odtwarzające**. Możesz np. zaznaczyć głośnik Bluetooth, aby odtwarzał muzykę, a mowę słyszeć z telefonu. Słuchawki douszne pozostaw odznaczone, aby słyszeć w nich mowę.

Urządzenia są zapisywane według nazw. Dwa o tej samej nazwie mają jedną pozycję na liście. Jeśli niczego jeszcze nie podłączono, zobaczysz „Nie podłączono jeszcze żadnych urządzeń Bluetooth ani przewodowych”.

### Wybierz głośność mowy {#sound-speech-volume}

W menu **Wyjście audio** > **Regulacja głośności mowy** wybierz, która głośność ma regulować mowę i dźwięki SandFly.

- **Głośność dostępności** to wartość domyślna.
- **Głośność multimediów** używa głośności multimediów w Androidzie.
- **Głośność dzwonka** odtwarza mowę tak jak dzwonek. Pozostaje ona na telefonie przy przesyłaniu multimediów na inne urządzenie, może grać w głośniku przy podłączonych słuchawkach i milknie w trybie cichym.
- **Głośność połączenia, słuchawka** odtwarza mowę tak jak rozmowę, przez słuchawkę przy uchu, jeśli nie podłączono zestawu słuchawkowego.

Gdy **Urządzenie odtwarzające** kieruje dźwięk do głośnika telefonu, urządzenia przewodowego, USB lub aparatu słuchowego, mowa korzysta z głośności dostępności.

Gdy mowa nie korzysta z głośności dostępności, Android odtwarza dźwięki ułatwień dostępu innych aplikacji, np. sygnały dyktowania, z głośnością multimediów.

Pozycja **Głośność mowy** w sterowaniu czytaniem przełącza wybór rodzaju głośności. Pozycja **Głośność dostępności** podnosi lub obniża tę głośność. Zobacz [](#sound-reading-controls).

### Ścisz inne dźwięki podczas mowy {#sound-ducking}

**Wyjście audio** > **Wyciszanie tła** ścisza pozostałe dźwięki, np. muzykę, gdy SandFly mówi. Domyślnie opcja jest włączona. Możesz ją przełączać za pomocą pozycji **Wyciszanie tła** w sterowaniu czytaniem lub polecenia **Wyciszanie tła** w grupie **Mowa**.

### Dźwięk o niskim opóźnieniu {#sound-low-latency}

**Wyjście audio** > **Dźwięk o niskim opóźnieniu** korzysta z najszybszego toru audio urządzenia, aby szybciej zacząć odtwarzanie sygnałów i mowy. Opcja jest domyślnie włączona na telefonach i tabletach, a wyłączona na zegarkach. Wyłącz ją, jeśli mowa lub sygnały się rwą albo brzmią niewłaściwie.

- Od Androida 11 szybki tor działa dla mowy. We wcześniejszych wersjach korzystają z niego tylko dźwięki.
- Mowa działa zwykłym torem podczas rozmowy i krótko po jej rozpoczęciu lub zakończeniu, podczas nagrywania przez mikrofon oraz z syntezatorami odtwarzającymi dźwięk samodzielnie.
- Wznowienie mowy w środku słowa po pauzie wymaga opcji **Dźwięk o niskim opóźnieniu**. Zobacz [](#speech-pause). Po jej wyłączeniu efekty głosu działają tylko wtedy, gdy urządzenie je obsługuje jako efekt audio. Zobacz [](#voice-effects).

### Pozostaw mowę na telefonie podczas przesyłania ekranu {#sound-casting}

**Wyjście audio** > **Odtwarzaj mowę tylko na telefonie podczas przesyłania ekranu** pozostawia mowę i dźwięki SandFly na telefonie przy przesyłaniu lub powielaniu obrazu na telewizorze albo głośniku. Gdy opcja jest włączona, dźwięku SandFly nie ma też w nagraniach ekranu. Domyślnie opcja jest wyłączona. Wymaga Androida 10 lub nowszego.

### Połączenia {#sound-calls}

Na ekranie **Wyjście audio** są dwie opcje dotyczące rozmów.

**Włączaj tryb głośnomówiący po odsunięciu telefonu od ucha** przenosi rozmowę na głośnik po odsunięciu telefonu i z powrotem do słuchawki po przyłożeniu go do ucha. Domyślnie opcja jest wyłączona. Wymaga Androida 12 lub nowszego. Działa, gdy SandFly jest włączony i niewstrzymany. Zmienia wyjście tylko między słuchawką telefonu a jego głośnikiem. Zestawy słuchawkowe Bluetooth i przewodowe pozostają w użyciu. Rozmowa rozpoczęta z telefonem odsuniętym od ucha od razu trafia na głośnik.

Do działania potrzebne jest uprawnienie, którego Android nie przyznaje czytnikom ekranu. Dopóki go brakuje, podsumowanie pokazuje polecenie, które je przyznaje. Możesz skorzystać z jednego z poniższych sposobów.

- Uruchom Shizuku i włącz opcję. SandFly uzyska uprawnienie przez Shizuku. Zobacz [](#updates).
- Uruchom polecenie przez adb. Aby je skopiować, otwórz menu czynności ustawienia i wybierz **Kopiuj polecenie**. Potem wróć na ekran **Wyjście audio**. Bez Shizuku przełącznik stanie się dostępny po przyznaniu uprawnienia.

SandFly sprawdza, czy trzymasz telefon przy uchu, za pomocą czujnika zbliżeniowego.

**Resetuj dźwięk ułatwień dostępu po połączeniach** przywraca wyjście audio, gdy po rozmowie np. w komunikatorze Android pozostawi dźwięk ułatwień dostępu w słuchawce telefonu. Domyślnie opcja jest wyłączona. Wymaga Androida 13 lub nowszego oraz Shizuku albo dostępu root. Przy włączaniu może pojawić się prośba o zezwolenie na dostęp do Shizuku. Bez Shizuku podsumowanie pokazuje polecenie adb do jednorazowego resetu dźwięku. Otwórz menu czynności ustawienia i wybierz **Kopiuj polecenie**, żeby je skopiować.

### Sterowanie dźwiękiem i głośnością podczas czytania {#sound-reading-controls}

Przesunięcie palcem w górę lub w dół zmienia wartość wybranej pozycji. Poniższe pozycje możesz dodać na ekranie **Sterowanie czytaniem**. Zobacz [](#commands).

- **Urządzenie odtwarzające** przełącza wartości ustawienia **Urządzenie odtwarzające**.
- **Głośność mowy** przełącza rodzaj głośności ustawiany w opcji **Regulacja głośności mowy**.
- **Wyciszanie tła** włącza lub wyłącza **Wyciszanie tła**.
- **Głośność dostępności** podnosi lub obniża głośność dostępności.
- **Głośność multimediów**, **Głośność połączenia**, **Głośność dzwonka**, **Głośność powiadomień** i **Głośność alarmu** zmieniają odpowiednią głośność o jeden krok i podają nową wartość. Zmiana głośności dzwonka lub powiadomień z wyciszonej na słyszalną lub odwrotnie wymaga dostępu do trybu Nie przeszkadzać.
- **Tryb dźwięku** ustawia dźwięk, wibracje lub wyciszenie. Przełączanie wyciszenia wymaga dostępu do trybu Nie przeszkadzać.
