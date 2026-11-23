## Dźwięk, wibracje i wyjście audio {#sound}

Dźwięki i wibracje sygnalizują między innymi przeniesienie fokusu, aktywację elementu, przewijanie i otwarcie okna. Otwórz **Dźwięk i wibracje**, żeby je włączyć, dopasować lub zmienić temat. W **Wyjście audio** ustawisz, gdzie gra czytnik i jak jego odczyt współpracuje z innym dźwiękiem. Zobacz też [ustawienia mowy](#speech) i [klawisze głośności](#buttons).

### Włącz lub wyłącz dźwięki {#sound-feedback}

W części **Dźwięki** na ekranie **Dźwięk i wibracje** są trzy ustawienia.

- **Sygnał dźwiękowy** jest domyślnie włączony. Wyłącz go, jeśli chcesz korzystać tylko z wibracji.
- **Głośność sygnału dźwiękowego** ma zakres od 5 do 100 procent, co 5 punktów procentowych. Domyślnie jest to 50 procent.
- **Tematy dźwiękowe** otwierają [listę tematów i ustawienia dźwięków](#sound-themes).

Dźwięki przełączysz także skrótem **Włącz lub wyłącz sygnał dźwiękowy (ikony audio)**. Zobacz [skróty klawiszowe](#appendix-keyboard-shortcuts). Możesz też [wyłączyć wybrany dźwięk](#sound-individual).

### Zmień wibracje {#sound-vibration}

Sekcja **Wibracje** pojawia się na urządzeniach z silnikiem wibracji.

- **Sygnał wibracyjny** jest domyślnie włączony. Wyłącz go, jeśli chcesz korzystać tylko z dźwięku.
- **Siła wibracji** ma zakres od 5 do 200 procent. Domyślnie jest to 100 procent. Opcja pojawia się, jeśli urządzenie może zmieniać siłę wibracji.
- **Typ wibracji** pokazuje typy dostępne na urządzeniu. Jeśli jest tylko jeden, lista jest ukryta. Początkowo używany jest pierwszy dostępny typ.
  - **Efekty haptyczne** to złożone efekty, na przykład kliknięcia i tyknięcia. Wymagają Androida 11 i odpowiedniego silnika.
  - **Impulsy** mogą mieć różną siłę, jeśli obsługuje to silnik.
  - **Włączanie i wyłączanie** używa pełnej siły i działa na każdym silniku wibracji.

Jeśli **Efekty haptyczne** są zbyt słabe, możesz wybrać **Impulsy** i zwiększyć **Siła wibracji**. Polecenie **Sygnał wibracyjny** w kategorii **Mowa** pozwala [przełączać wibracje](#commands).

### Dźwięki i odpowiadające im wibracje {#sound-events}

Każdy dźwięk ma własną wibrację. Możesz więc rozpoznawać zdarzenia po samych wibracjach. Niektóre wibracje, na przykład przy komunikatach i przy pisaniu brajlem, działają bez dźwięku. Nazwy w tabeli odpowiadają opcjom **Poszczególne dźwięki i wibracje** i liście dźwięków tematu.

| Nazwa | Kiedy działa |
| --- | --- |
| **Zaznaczenie elementu** | Fokus przechodzi na element. |
| **Zaznaczenie elementu z dostępną czynnością** | Fokus przechodzi na element, który możesz aktywować. |
| **Dotknięcie pustego obszaru** | Pod palcem jest puste miejsce. |
| **Kliknięcie** | Aktywujesz element. |
| **Przytrzymanie** | Wykonujesz długie naciśnięcie. |
| **Przewijanie** | Lista się przewija. |
| **Wejście do listy** i **Wyjście z listy** | Fokus wchodzi na listę lub ją opuszcza. |
| **Działanie wykonane lub osiągnięto koniec** | Kończy się czynność lub fokus dochodzi do końca. |
| **Zmiana okna** | Otwiera się okno lub ekran. |
| **Następny element sterowania czytaniem** i **Poprzedni element sterowania czytaniem** | Wybierasz inną opcję sterowania czytaniem. |
| **Gest krawędzi na dole**, **Środkowy gest krawędzi**, **Góra gestu krawędzi** | Kolejne etapy gestu od krawędzi. Nie działają na zegarku. |
| **Punkt wstawiania został przeniesiony** | Kursor w polu przesuwa się po znaku, słowie, wierszu lub akapicie. |
| **Przejście do następnego wiersza** i **Przejście do poprzedniego wiersza** | Fokus przechodzi do wiersza niżej lub wyżej. |
| **Zadanie agenta rozpoczęte**, **Zadanie agenta ukończone**, **Pytanie agenta**, **Agent zatrzymany przy wrażliwej czynności**, **Zadanie agenta nieudane** | Sygnały agenta w [SandFly Pro](#pro). |
| **Błąd pisowni** | SandFly trafia na błędnie zapisany wyraz. |
| **Link** | W odczytywanym tekście jest link. |
| **Sformatowany tekst** | SandFly czyta sformatowany fragment, na przykład pogrubienie. |
| **Ekran wyłączony** i **Ekran włączony** | Ekran gaśnie lub się zapala. |
| **Ekran ukryty** i **Ekran pokazany** | Ekran [znika lub zostaje pokazany](#display). |
| **Wczytywanie** | SandFly czeka na opis lub inny wynik. |
| **Ładowanie modelu AI na urządzeniu** | Wczytuje się model lokalny. |
| **Tryb przeglądania włączony** i **Tryb przeglądania wyłączony** | Przełącza się [tryb przeglądania](#web). |
| **Klawiatura otwarta** i **Klawiatura zamknięta** | Pokazuje się lub znika klawiatura ekranowa. |
| **Powiadomienie SandFly** | SandFly wysyła powiadomienie przez kanał bez dźwięku. |
| **Pełne naładowanie** | Bateria jest pełna, gdy działa [powiadamianie o naładowaniu](#verbosity-announcements). |
| **Menu kołowe** | Osiem dźwięków pozycji menu. |
| **Monitor brajlowski połączony**, **Monitor brajlowski odłączony**, **Polecenie brajlowskie nie powiodło się**, **Automatyczne przewijanie brajla włączone**, **Automatyczne przewijanie brajla wyłączone**, **Klawiatura brajlowska skalibrowana** | Sygnały [monitora i klawiatury brajlowskiej](#braille). |
| **Dźwięk kontrolki: przycisk** i pozostałe dźwięki kontrolek | Różne [typy kontrolek](#sound-control-sounds). |

#### Dźwięki podczas nawigacji {#sound-navigation-sounds}

SandFly sygnalizuje zmianę elementu, opcji lub etapu gestu. Początek i koniec zwykłego gestu nie mają osobnego dźwięku.

- Dźwięk następnej lub poprzedniej opcji sterowania czytaniem usłyszysz przy zmianie gestem, menu kołowym, skrótem klawiszowym lub klawiaturą brajlowską.
- Przy [geście od krawędzi](#gestures) usłyszysz niski dźwięk przy dotknięciu brzegu, a następnie dźwięk pierwszego i drugiego etapu. Cofnięcie palca odtwarza dźwięk miejsca, do którego wracasz. Przy uniesieniu palca osobnego dźwięku nie ma.
- Dźwięk przejścia do następnego lub poprzedniego wiersza pojawia się, gdy fokus trafia do elementu niżej lub wyżej. Każda pozycja listy jest osobnym wierszem. Elementy obok siebie, na przykład ikony lub komórki tabeli, mogą dzielić wiersz. Dźwięk gra razem z dźwiękiem fokusu, a wibracja zastępuje wibrację fokusu. Przy przejściu między oknami tego sygnału nie usłyszysz.

#### Tyknięcia przy przewijaniu {#sound-scroll-ticks}

Podczas przewijania dwoma palcami usłyszysz lekkie tyknięcie przy każdym mijanym elemencie. Szybszy ruch daje częstsze tyknięcia. Jeśli lista nie podaje pozycji elementów, SandFly tyka co 48 jednostek `dp`. Pierwsze tyknięcie przypada na chwilę, gdy gest zaczyna działać w aplikacji.

Obok tych sygnałów może grać dźwięk przewijania. Odtwarza się najwyżej raz na 250 milisekund. Im bliżej końca listy, tym jest wyższy. Przewijanie wykonywane przez SandFly nie dodaje tyknięć.

### Wyłącz wybrane dźwięki lub wibracje {#sound-individual}

Otwórz **Dźwięk i wibracje** > **Poszczególne dźwięki i wibracje**. Osobne przełączniki znajdziesz pod **Dźwięki** i **Wibracje**. Początkowo wszystkie są włączone. Dźwiękiem i wibracją danego zdarzenia możesz sterować niezależnie. Główne przełączniki **Sygnał dźwiękowy** i **Sygnał wibracyjny** sterują całą grupą.

Żeby sprawdzić sygnał, ustaw fokus na przełączniku, otwórz czynności i wybierz **Podgląd**. Zmiana przełącznika nie odtwarza sygnału.

Na liście **Wibracje** są też sygnały bez dźwięku, między innymi **Komunikat**, **Gest krawędzi: przesunięcie wstecz** i wibracje klawiatury brajlowskiej. Wyłączenie wibracji obejmuje także wariant dostarczany przez temat.

### Tematy dźwiękowe {#sound-themes}

Temat zastępuje dźwięki i wibracje SandFly. Ma też własne ustawienia dźwięków kontrolek, przestrzenności i odczytu typu elementu. Wbudowany temat **SandFly** jest zawsze dostępny.

W **Dźwięk i wibracje** > **Tematy dźwiękowe** możesz [utworzyć nowy temat](#sound-themes-create) lub [zainstalować gotowy](#sound-themes-install). Aktywny temat jest oznaczony w sekcji **Zainstalowane tematy**. Pod jego ustawieniami znajdziesz [dźwięki kontrolek](#sound-control-sounds) i [dźwięki użyte w temacie](#sound-themes-replace).

#### Zainstaluj temat {#sound-themes-install}

Temat ma postać pliku ZIP. W sekcji **Zainstaluj temat** wybierz sposób instalacji.

- **Z pliku** otwiera systemowy wybór pliku.
- **Z linku** pozwala wkleić link HTTPS do pliku tematu lub repozytorium GitHub z tematem. Potwierdź przez **Zainstaluj**.
- **Ze źródła** pokazuje zapisane repozytoria GitHub, GitLab, Codeberg, Gitea lub Bitbucket. Użyj **Dodaj źródło** i wpisz adres, na przykład `github.com/owner/repository`. Otwórz źródło i wybierz plik. Na początku jest repozytorium tematów SandFly. **Usuń źródło** usuwa je z listy, a zainstalowane tematy są nadal dostępne.

Możesz też otworzyć plik w SandFly z menedżera plików lub pobranych plików przeglądarki. Z innej aplikacji udostępnij plik albo link do SandFly. Wybierz **Zainstaluj temat dźwiękowy**.

Przed instalacją SandFly podaje autora, opis, liczbę dźwięków i wibracji, licencję i pominięte pliki. **Zainstaluj i użyj** od razu włącza temat. **Zainstaluj** dodaje go do wyboru. Temat o tej samej nazwie zastąpi dotychczasowy. Na zegarku użyj linku. Instalacja ze źródła jest dostępna na telefonie.

#### Wybierz temat i zarządzaj nim {#sound-themes-manage}

Otwórz **Dźwięk i wibracje** > **Tematy dźwiękowe** i wybierz pozycję z sekcji **Zainstalowane tematy**.

- **Użyj tego tematu** zmienia aktywny temat razem z jego ustawieniami kontrolek.
- **Informacje o temacie** podają autora, opis, liczbę dźwięków i wibracji, licencję i stronę.
- **Zapisz jako plik tematu** zapisuje plik ZIP, który możesz udostępnić.
- **Usuń** usuwa temat. Wbudowany temat SandFly jest niedostępny do usunięcia.

#### Zastąp pojedynczy dźwięk {#sound-themes-replace}

Możesz zmienić dźwięk również w temacie **SandFly**.

1. Otwórz **Dźwięk i wibracje** > **Tematy dźwiękowe**.
2. W ustawieniach bieżącego tematu wybierz **Dźwięki**.
3. Wybierz dźwięk. W tytule okna jest jego nazwa i nazwa pliku, na przykład `tick` przy kliknięciu.
4. Wybierz **Wybierz plik dźwiękowy**, żeby wskazać plik WAV, OGG, MP3 lub FLAC do 5 MB. Możesz też odsłuchać sygnał, wybrać **Brak dźwięku** lub przywrócić **Użyj dźwięku SandFly**.

Podsumowania **Dźwięk SandFly**, **Dźwięk tematu** i **Brak dźwięku** wskazują, czego używa SandFly. Te same opcje są w czynnościach dźwięku. **Przywróć wszystkie dźwięki SandFly** zastępuje wszystkie dźwięki tematu wbudowanymi.

Lista zawiera też **Zmiana głośności** dla tonu przy zmianie głośności oraz dźwięki pisania brajlem. Zobacz [klawisze głośności](#buttons) i [ustawienia brajla](#braille).

#### Utwórz własny temat {#sound-themes-create}

1. Otwórz **Dźwięk i wibracje** > **Tematy dźwiękowe**.
2. Wybierz **Utwórz nowy temat**.
3. Wpisz **Nazwa**, której nie ma na liście. **Opis**, **Autor** i **Strona internetowa** są opcjonalne.
4. Wybierz **OK**. SandFly otworzy listę dźwięków.
5. Wybierz pliki dźwięków, które chcesz zastąpić. Ustaw też **Dźwięki i wibracje kontrolek**, **Dźwięk przestrzenny 3D** i **Nadal odczytuj typ kontrolki**.
6. Wybierz **Zapisz**, żeby użyć tematu, albo **Zapisz jako plik tematu**, żeby zapisać ZIP.

Aby wyjść bez zapisywania, cofnij się i wybierz **Odrzuć**. **Edytuj dalej** wraca do tematu. Jeśli temat ma dźwięki kontrolek, SandFly włączy je przy zapisie, o ile samodzielnie nie zmienisz odpowiedniej opcji.

#### Pliki tematu {#sound-themes-files}

Umieść `theme.json` i pliki dźwiękowe w jednym folderze w ZIP. Mogą być również w pojedynczym podfolderze, jak w ZIP pobranym z repozytorium GitHub. Archiwum samych dźwięków też można zainstalować. Temat otrzyma nazwę pliku.

- Nazwij dźwięk tak jak zastępowany plik, na przykład `focus.wav` lub `tick.ogg`. Nazwę sprawdzisz w oknie danego dźwięku. Wystarczy dołączyć zmieniane pliki.
- Temat obsługuje WAV, OGG, MP3, FLAC, M4A, AAC i Opus. Limit to 5 MB na plik i 50 MB na temat. SandFly pomija plik, którego Android nie może odtworzyć.
- W `theme.json` możesz podać nazwę, autora, opis, licencję, stronę, ustawienia kontrolek, wyciszone dźwięki i wibracje. Wibracja może mieć do trzech wariantów, po jednym na typ.
- SandFly zachowuje pliki licencji i opisu, na przykład `LICENSE.txt` i `README.md`. Pozostałe pliki pomija.

Pełny format i nazwy plików znajdziesz w [dokumentacji tematów](https://github.com/aaron-gh/SandFly/blob/master/themes.md).

### Dźwięki i wibracje kontrolek {#sound-control-sounds}

Gdy włączysz dźwięki kontrolek, po przeniesieniu fokusu usłyszysz dźwięk rodzaju elementu, na przykład przycisku, pola wyboru, przełącznika, suwaka lub pola tekstowego. Osobne sygnały mają także opcje, rozwijane listy, obrazy, zegary, karty, pozycje menu i listy, linki oraz elementy drzewa.

Wybierz **Dźwięk i wibracje** > **Tematy dźwiękowe**. W ustawieniach bieżącego tematu włącz **Dźwięki i wibracje kontrolek**. Domyślnie opcja jest wyłączona. **Nadal odczytuj typ kontrolki** dodaje nazwę typu po dźwięku. Domyślnie jest włączone dla tematu SandFly. Pozostałe tematy mogą zdecydować o tym osobno. [Dźwięk przestrzenny](#sound-3d-audio) zależy od położenia elementu.

Gdy fokus trafia na wiersz z kontrolką, SandFly używa sygnału pierwszej kontrolki, na przykład przełącznika. Jeśli wiersz nie ma kontrolki, odtwarza sygnał rodzaju elementu. Klawisze klawiatury ekranowej używają zwykłego dźwięku fokusu. Wibracja może działać przy wyłączonych dźwiękach. Gdy wyciszysz dany typ kontrolki w ustawieniach lub w temacie, SandFly odtwarza zwykły dźwięk fokusu i odczytuje typ.

Każdy temat zapisuje te ustawienia oddzielnie.

#### Dźwięk przestrzenny {#sound-3d-audio}

Dźwięk kontrolki może dochodzić z miejsca, w którym jest na ekranie, na przykład z lewej strony lub z góry. Tryb 3D wykorzystuje pomiary tego, jak dźwięk dociera do uszu z różnych stron. Bez niego dźwięk przesuwa się między lewym i prawym kanałem.

- **Podczas używania słuchawek** to ustawienie domyślne. Obejmuje słuchawki przewodowe i USB, zestawy Bluetooth i aparaty słuchowe. Od Androida 13 SandFly sprawdza, czy dźwięk czytnika rzeczywiście trafia do tego wyjścia. Na starszych Androidach wystarczy połączenie słuchawek.
- **Zawsze** używa 3D także na głośniku telefonu.
- **Nigdy, tylko lewy i prawy kanał** pozostawia wyłącznie kierunek poziomy.

### Wyjście audio {#sound-audio-output}

Na ekranie **Wyjście audio** znajdziesz [urządzenie odtwarzające](#sound-output-device), [głośnik telefonu dla wybranych urządzeń](#sound-speaker-devices) i [ustawienia rozmów](#sound-calls). Możesz ustawić [dźwięk podczas przesyłania ekranu](#sound-casting), [kanał głośności](#sound-speech-volume), [wyciszanie tła](#sound-ducking) i [niskie opóźnienie](#sound-low-latency).

### Wybierz urządzenie odtwarzające {#sound-output-device}

Otwórz **Wyjście audio** > **Urządzenie odtwarzające**. Domyślnie Android wybiera wyjście. Możesz wybrać **Głośnik telefonu** albo nazwę podłączonego urządzenia Bluetooth, przewodowego, USB lub aparatu słuchowego.

W przypadku Bluetooth o kierowaniu dźwięku decyduje Android. Czytnik nie wymusza trybu rozmowy. Jeżeli Android skieruje dźwięk na inne wyjście, będzie ono nadal używane. Na wyjście przewodowe, USB i aparaty słuchowe SandFly może wysyłać dźwięk bezpośrednio.

SandFly pamięta wybór po rozłączeniu sprzętu i po zmianie odtwarzanej aplikacji. Przy odłączonym urządzeniu podaje jego nazwę i stan, a do ponownego połączenia korzysta z wyjścia Androida. Android decyduje też podczas rozmów i gdy inna aplikacja używa mikrofonu do rozmowy lub obsługi asystenta. Wybór możesz zmieniać opcją [**Urządzenie odtwarzające**](#sound-reading-controls) w sterowaniu czytaniem.

### Przenieś mowę na głośnik telefonu przy wybranych połączeniach {#sound-speaker-devices}

Wybierz **Wyjście audio** > **Głośnik telefonu przy tych urządzeniach**. Na liście są urządzenia Bluetooth, przewodowe i USB, które są lub były podłączone przy działającym SandFly. Po podłączeniu zaznaczonego urządzenia mowa i dźwięki SandFly trafią na głośnik telefonu.

Zaznacz na przykład głośnik Bluetooth, jeśli chcesz, żeby muzyka grała na głośniku, a czytnik mówił z telefonu. Zostaw na liście niezaznaczone słuchawki, jeśli chcesz w nich słyszeć SandFly po ich podłączeniu.

Dwa urządzenia o tej samej nazwie mają wspólną pozycję. Lista jest pusta, dopóki nie podłączysz odpowiedniego sprzętu.

### Wybierz kanał głośności mowy {#sound-speech-volume}

Otwórz **Wyjście audio** > **Regulacja głośności mowy**.

- **Głośność dostępności** to ustawienie domyślne.
- **Głośność multimediów** używa tego samego kanału co multimedia.
- **Głośność dzwonka** odtwarza mowę jako dzwonek. Mowa może pozostać na telefonie podczas przesyłania multimediów albo po podłączeniu słuchawek. Milczy po wyciszeniu dzwonka.
- **Głośność połączenia, słuchawka** odtwarza mowę jak dźwięk rozmowy. Bez zestawu słuchawkowego używa słuchawki telefonu.

Jeżeli w ustawieniu urządzenia wymusisz głośnik telefonu, urządzenie przewodowe, USB lub aparat słuchowy, odczyt korzysta z głośności dostępności. Przy innym kanale Android odtwarza dźwięki dostępności innych aplikacji, na przykład sygnał dyktowania, z głośnością multimediów. Zobacz [sterowanie kanałami](#sound-reading-controls).

### Przycisz tło podczas mowy {#sound-ducking}

Opcja **Wyjście audio** > **Wyciszanie tła** przycisza inne dźwięki, na przykład muzykę, podczas odczytu. Domyślnie jest włączona. Możesz ją przełączyć poleceniem lub opcją sterowania czytaniem **Wyciszanie tła**.

### Dźwięk o niskim opóźnieniu {#sound-low-latency}

Opcja **Wyjście audio** > **Dźwięk o niskim opóźnieniu** pomaga szybciej rozpocząć odtwarzanie. Domyślnie jest włączona na telefonach i tabletach, a wyłączona na zegarkach. Wyłącz ją, jeśli mowa lub dźwięki zaczynają się rwać albo brzmią nieprawidłowo.

- Od Androida 11 szybki tor obejmuje mowę i dźwięki. W starszych wersjach obejmuje same dźwięki.
- SandFly korzysta ze zwykłego toru w trakcie rozmowy i krótko po jej rozpoczęciu lub zakończeniu, przy nagrywaniu z mikrofonu oraz z syntezatorami odtwarzającymi dźwięk samodzielnie.
- Wznowienie [w środku słowa](#speech-pause) wymaga niskiego opóźnienia. Jeśli wyłączysz tę opcję, [efekty głosu](#voice-effects) działają na urządzeniach z obsługą efektu systemowego.

### Zachowaj mowę na telefonie podczas przesyłania ekranu {#sound-casting}

Opcja **Wyjście audio** > **Odtwarzaj mowę tylko na telefonie podczas przesyłania ekranu** zapobiega przesyłaniu mowy i dźwięków SandFly do telewizora lub głośnika. Po włączeniu dźwięku czytnika nie będzie również w nagraniu ekranu. Domyślnie jest wyłączona i wymaga Androida 10 lub nowszego.

### Połączenia {#sound-calls}

Gdy włączysz **Wyjście audio** > **Włączaj tryb głośnomówiący po odsunięciu telefonu od ucha**, SandFly użyje czujnika zbliżeniowego. Rozmowa przejdzie na głośnik po odsunięciu telefonu i wróci do słuchawki po przyłożeniu go do ucha. Jeśli rozmowa rozpocznie się z telefonem odsuniętym, od razu zagra na głośniku. Podłączone zestawy przewodowe i Bluetooth mają własną obsługę. Funkcja wymaga Androida 12 i działającego, niewstrzymanego SandFly. Domyślnie jest wyłączona.

Funkcja wymaga uprawnienia do zarządzania połączeniami. Przy uruchomionym [Shizuku](#updates) SandFly przyznaje je po włączeniu opcji. Możesz też skopiować polecenie z czynności **Kopiuj polecenie**, wykonać je przez `adb` i wrócić do ekranu **Wyjście audio**. Bez Shizuku przełącznik staje się dostępny po przyznaniu uprawnienia.

Opcja **Resetuj dźwięk ułatwień dostępu po połączeniach** przywraca dźwięk czytnika, jeśli Android po rozmowie zostawi go w słuchawce. Dotyczy to też rozmów z komunikatorów. Funkcja jest domyślnie wyłączona. Wymaga Androida 13 lub nowszego oraz Shizuku lub roota. Przy Shizuku możesz zobaczyć prośbę o zezwolenie dla SandFly. W podsumowaniu można znaleźć polecenie do jednorazowego resetu przez `adb`. Użyj czynności **Kopiuj polecenie**.

### Opcje sterowania dźwiękiem i głośnością {#sound-reading-controls}

Dodaj potrzebne pozycje na ekranie [**Sterowanie czytaniem**](#commands). Zmienisz je gestem w górę lub w dół.

- **Urządzenie odtwarzające** zmienia wyjście, a **Głośność mowy** kanał używany przez mowę.
- **Wyciszanie tła** przełącza przyciszanie innych dźwięków.
- **Głośność dostępności**, **Głośność multimediów**, **Głośność połączenia**, **Głośność dzwonka**, **Głośność powiadomień** i **Głośność alarmu** zmieniają dany poziom o jeden krok i odczytują nową wartość.
- **Tryb dźwięku** przełącza dzwonek między dźwiękiem, wibracją i wyciszeniem. Zmiana głośności dzwonka lub powiadomień na wyciszoną albo z wyciszonej, a także przełączenie wyciszenia wymagają dostępu do trybu Nie przeszkadzać.
