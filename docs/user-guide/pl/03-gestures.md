## Gesty {#gestures}

Do większości gestów możesz przypisać polecenie z listy. Żeby zmienić przypisanie, otwórz **Gesty** w ustawieniach SandFly.

Domyślne przypisania znajdziesz w [wykazie gestów](#appendix-gestures). O tym, co robią poszczególne polecenia, przeczytasz w [rozdziale o poleceniach i menu](#commands).

### Jak SandFly rozpoznaje gesty {#gestures-recognition}

Połóż palec na ekranie. Trzymaj go w miejscu albo przesuwaj powoli, a SandFly będzie odczytywał element pod palcem. To eksploracja dotykiem. Szybkie przesunięcie lub stuknięcie SandFly rozpoznaje jako gest. Więcej o eksploracji dotykiem znajdziesz w [rozdziale o poruszaniu się po ekranie](#reading).

Na urządzeniach z Androidem 13 lub nowszym gesty domyślnie rozpoznaje SandFly. Jeśli wyłączysz opcję **Używaj gestów w SandFly** w menu **Ustawienia programisty**, rozpoznawanie przejmie Android. Po zmianie wyłącz i ponownie włącz SandFly.

Gdy gesty rozpoznaje Android, ustawienia czasu na ekranie **Gesty** są niedostępne. Przestają też działać niektóre gesty SandFly.

- Pokrętło, gesty przy krawędziach i w rogach, szybki ruch dwoma palcami tam i z powrotem oraz zsuwanie i rozsuwanie palców.
- Przesunięcia z przytrzymaniem, trzyczęściowe i dwuetapowe, stuknięcie z przesunięciem oraz gesty łamane dwoma palcami.
- Trzy i cztery stuknięcia jednym palcem, cztery stuknięcia trzema palcami, dwukrotne stuknięcie dwoma palcami u góry i u dołu oraz stuknięcia czterema palcami na połowie ekranu.

Gesty wykonywane co najmniej dwoma palcami wymagają Androida 11 lub nowszego.

### Stuknięcia {#gestures-taps}

Stuknij dwa razy jednym palcem, aby aktywować element, na którym jest fokus. Jeśli chcesz wykonać długie naciśnięcie tego elementu, po drugim stuknięciu przytrzymaj palec na ekranie.

Na ekranie **Gesty** nie przypiszesz własnego polecenia do tych dwóch gestów ani do zwykłego pojedynczego stuknięcia jednym palcem. W kategorii **1 palec** znajdziesz też **Kliknij trzykrotnie** i **Kliknij czterokrotnie**. Oba gesty działają w dowolnym miejscu na ekranie i domyślnie nie mają przypisań. Na zegarku te gesty są niedostępne. [Stuknięcia w rogach ekranu](#gestures-corners) mają osobne przypisania.

Dwoma, trzema i czterema palcami możesz stuknąć raz, dwa lub trzy razy. Po drugim stuknięciu możesz też przytrzymać palce na ekranie. Dwoma lub trzema palcami możesz zakończyć w ten sposób również trzy stuknięcia. Trzema palcami da się ponadto stuknąć raz i przytrzymać albo stuknąć cztery razy.

Przy niektórych gestach możesz przypisać osobne polecenie w zależności od miejsca, w którym stukasz.

- Stuknij dwukrotnie dwoma palcami, jednym w górnej części ekranu, a drugim w dolnej. Każda z tych części zajmuje jedną trzecią wysokości. W ustawieniach ten gest to **Kliknij dwukrotnie 2 palcami, u góry i u dołu**. Gdy przypiszesz mu polecenie, ma ono pierwszeństwo przed zwykłym gestem **Kliknij dwukrotnie 2 palcami**. Liczy się bieżące ułożenie ekranu, również w trybie poziomym.
- Gesty **Kliknij 4 palcami w górnej połowie** i **Kliknij 4 palcami w dolnej połowie** możesz ustawić osobno. Przypisane w ten sposób polecenie ma na wybranej połowie pierwszeństwo przed gestem **Kliknij 4 palcami**.

Przy stuknięciu kilkoma palcami polecenie zwykle uruchamia się, gdy uniesiesz palce. SandFly poczeka, jeśli do tych samych stuknięć można jeszcze dodać kolejne stuknięcie, przytrzymanie albo przesunięcie i w ten sposób wykonać inny przypisany gest. Jeśli przesuniesz choćby jeden palec, SandFly przestanie traktować dotknięcie jako stuknięcie. Cztery stuknięcia trzema palcami zawsze uruchamiają polecenie od razu.

Po dwóch stuknięciach jednym palcem SandFly zwykle aktywuje element, gdy uniesiesz palec. Jeśli przypiszesz polecenie do **Kliknij trzykrotnie** lub **Kliknij czterokrotnie**, SandFly poczeka na następne stuknięcie. Jeśli go nie będzie, aktywuje element po upływie tego czasu. Po trzecim stuknięciu czeka na kolejne tylko wtedy, gdy **Kliknij czterokrotnie** ma przypisane polecenie. W rogu, w którym ustawiono gesty narożne, stuknięcia działają tak, jak opisano w [](#gestures-corners).

W opcji **Maksymalny odstęp między kliknięciami jednym palcem** ustawisz, ile czasu masz na następne stuknięcie. Zobacz [](#gestures-timing).

### Przesunięcia {#gestures-swipes}

Domyślnie szybkie przesunięcie jednym palcem w prawo przenosi fokus na następny element, a przesunięcie w lewo na poprzedni. Ruch w górę lub w dół wykonuje czynność wybraną w sterowaniu czytaniem. Możesz w ten sposób przechodzić między słowami albo zmieniać szybkość mowy. Więcej przykładów znajdziesz w [rozdziale o czytaniu ekranu](#reading).

Przesunięcie dwoma palcami przewija ekran albo uruchamia polecenie SandFly, zależnie od [ustawienia obsługi przesunięć dwoma palcami](#gestures-two-finger-swipes). Przesuwając trzy lub cztery palce, uruchamiasz polecenia SandFly.

#### Ruch tam i z powrotem oraz po linii łamanej {#gestures-two-direction}

W obu rodzajach gestów zmieniasz kierunek, cały czas dotykając ekranu jednym palcem.

- W geście tam i z powrotem po pierwszym ruchu zawracasz. Na przykład przesuń palec w górę, po czym cofnij go w dół. Gest **Przesuń w górę, a potem w dół** znajdziesz na ekranie **Gesty** w kategorii **1 palec, tam i z powrotem**.
- W geście po linii łamanej skręcasz pod kątem prostym. Na przykład przesuń palec w dół i skręć w prawo. Gest **Przesuń w dół, a potem w prawo** należy do kategorii **1 palec, linia łamana**.

Włącz **Obsługuj przesunięcia dwoma palcami w SandFly**, żeby korzystać z kategorii **2 palce, linia łamana**. Wśród ośmiu gestów jest na przykład **Przesuń w górę, a potem w prawo 2 palcami**. Polecenia do tych gestów wybierasz samodzielnie. Szybki [ruch dwoma palcami tam i z powrotem](#gestures-scrub-pinch-spread) ma osobne przypisanie.

Opcja **Rozpoznawaj przesunięcia przed uniesieniem palca** wyłącza i ukrywa gesty tam i z powrotem wykonywane jednym palcem oraz gesty łamane wykonywane jednym lub dwoma palcami. Zobacz [](#gestures-swipes-before-lift).

#### Przesunięcia trzyczęściowe {#gestures-three-part}

Przesuń palec w prawo, potem w górę i znów w prawo. Unieś go dopiero po ostatnim ruchu. To gest **Przesuń w prawo, w górę, a potem w prawo**. Możesz też zawracać, np. w geście **Przesuń w górę, w dół, a potem w górę**.

Jeśli SandFly nie rozpoznaje gestu, przesuń palec trochę dalej w każdym kierunku. SandFly stosuje ten sam próg długości co przy [geście po linii łamanej](#gestures-two-direction).

Na ekranie **Gesty** rozwiń kategorię **Przesunięcia trzyczęściowe**. Jest tam 36 kombinacji. Przypisz polecenie do tej, której chcesz używać. Włączenie opcji **Rozpoznawaj przesunięcia przed uniesieniem palca** wyłącza te gesty i ukrywa kategorię. Na zegarku przesunięcia trzyczęściowe są niedostępne.

#### Przesunięcia z przytrzymaniem {#gestures-swipe-and-hold}

Przesuń palec i zatrzymaj go na ekranie. Gdy przytrzymasz go wystarczająco długo, SandFly uruchomi polecenie, zanim uniesiesz palec. Jeżeli uniesiesz go za wcześnie, SandFly rozpozna zwykłe przesunięcie.

Przytrzymaniem możesz zakończyć prosty ruch, gest tam i z powrotem albo gest po linii łamanej. Na liście znajdziesz na przykład **Przesuń w prawo i przytrzymaj** oraz **Przesuń w lewo, a potem w górę i przytrzymaj**. Dłuższa pauza przed zmianą kierunku może już uruchomić polecenie przypisane do pierwszego ruchu z przytrzymaniem. Krótką pauzę SandFly potraktuje jako część całego gestu.

Tych 16 gestów znajdziesz na końcu kategorii **1 palec**, **1 palec, tam i z powrotem** oraz **1 palec, linia łamana**. Każdy z nich zacznie działać po przypisaniu polecenia. W opcji **Czas przytrzymania po przesunięciu** ustawisz, jak długo trzeba trzymać palec w miejscu. Zobacz [](#gestures-timing). Na zegarku przesunięcia z przytrzymaniem są niedostępne.

#### Przesunięcia dwuetapowe {#gestures-two-stroke}

Przesuń palec w prawo. Unieś go, po czym przesuń w górę. To przykład przesunięcia dwuetapowego. To gest **Przesuń w prawo, unieś palec, następnie przesuń w górę**. W 256 dostępnych kombinacjach możesz również powtórzyć ten sam ruch albo skręcić podczas pierwszego lub drugiego przesunięcia.

Polecenia do tych gestów przypisujesz samodzielnie. Na ekranie **Gesty** kategorie zależą od tego, w którym ruchu zmieniasz kierunek.

- W kategorii **Przesunięcia dwuetapowe** jest 16 kombinacji, w których żaden z dwóch ruchów nie zmienia kierunku.
- Jeśli zmieniasz kierunek dopiero w drugim ruchu, otwórz kategorię **Przesunięcia dwuetapowe zakończone gestem tam i z powrotem lub pod kątem**. Znajdziesz tam np. **Przesuń w górę, unieś palec, następnie przesuń w prawo, a potem w dół**.
- Jeśli zmieniasz kierunek już w pierwszym ruchu, szukaj kategorii z jego nazwą, na przykład **Przesunięcia dwuetapowe zaczynające się od „Przesuń w górę, a potem w lewo”**. Przykład z tej kategorii to **Przesuń w górę, a potem w lewo, unieś palec, następnie przesuń w prawo**.

Najpierw wybierz czas w opcji **Odstęp między przesunięciami dwuetapowymi**. Gdy ustawisz **Wyłączone**, kategorie znikną. SandFly liczy czas od chwili uniesienia palca po pierwszym ruchu. Zanim upłynie, zacznij drugi ruch. Dostępne wartości znajdziesz w [ustawieniach czasu](#gestures-timing).

SandFly czeka na drugi ruch tylko wtedy, gdy pierwszy rozpoczyna gest dwuetapowy z przypisanym poleceniem. Liczą się przypisania z aktualnego układu, profilu lub aplikacji. Pozostałe przesunięcia działają od razu. SandFly wykona polecenie pierwszego przesunięcia, gdy minie czas na drugi ruch albo gdy w tym czasie stukniesz, zaczniesz eksplorować ekran lub wykonasz gest kilkoma palcami.

Jeśli chcesz, żeby zwykłe przesunięcie w jednym kierunku działało od razu, wybierz kombinację, w której zawracasz lub skręcasz już w pierwszym ruchu. SandFly rozpozna ten ruch jako część kombinacji także wtedy, gdy sam w sobie nie ma przypisanego polecenia.

Opcja **Rozpoznawaj przesunięcia przed uniesieniem palca** ukrywa i wyłącza kombinacje, które w którymś z dwóch ruchów zmieniają kierunek. Na zegarku przesunięcia dwuetapowe są niedostępne.

### Pokrętło {#gestures-rotor}

Połóż dwa palce na ekranie i obróć je jak pokrętło. Jeśli obrócisz je zgodnie z ruchem wskazówek zegara, wybierzesz następną opcję sterowania czytaniem. Obróć je w drugą stronę, żeby wrócić do poprzedniej. Po wybraniu opcji przesuń jednym palcem w górę lub w dół, żeby jej użyć. Opcje sterowania czytaniem opisano w [rozdziale o poleceniach i menu](#commands).

Domyślnie jeden krok to 30 stopni, czyli jedna dwunasta obrotu. Obracaj dalej bez odrywania palców, żeby przejść o kilka pozycji. Możesz wybrać kąt od 15 do 60 stopni w opcji **Gesty** > **2 palce** > **Kąt obrotu na jeden krok pokrętła**. Zwiększ kąt, jeśli często mijasz potrzebną pozycję. Przewijanie dwoma palcami nie przestawia pokrętła.

Gestom **Obróć 2 palce zgodnie z ruchem wskazówek zegara** i **Obróć 2 palce przeciwnie do ruchu wskazówek zegara** możesz przypisać także inne polecenia.

### Ruch tam i z powrotem, zsuwanie i rozsuwanie palców {#gestures-scrub-pinch-spread}

Przesuń dwa palce szybko o około centymetr i cofnij je od razu do punktu wyjścia. Gest **Przesuń 2 palcami szybko tam i z powrotem** domyślnie cofa do poprzedniego ekranu. Gdy aplikacja obsługuje przesunięcia dwoma palcami, ruch w jednym kierunku przewija zawartość. Przy przewijaniu w bok SandFly może odczekać około pół sekundy, żeby sprawdzić, czy cofniesz palce.

W gestach **Zsuń 2 palce** i **Rozsuń 2 palce** porusz palcami o około pół centymetra w stronę środka lub na zewnątrz. Staraj się nie przesuwać obu palców razem ani ich mocno obracać. Te same gesty możesz wykonać trzema lub czterema palcami. Polecenia przypisujesz samodzielnie.

Po przypisaniu polecenia do zsunięcia lub rozsunięcia dwóch palców SandFly może chwilę wstrzymać przekazanie ruchu do aplikacji. Dzieje się tak, gdy palce bardziej się zbliżają lub oddalają, niż przesuwają razem. Jeśli aplikacja przestanie reagować na powiększanie palcami, ustaw **Zsuń 2 palce** i **Rozsuń 2 palce** na **Nieprzypisany**.

Na zegarku te gesty są niedostępne.

### Gesty od krawędzi i wzdłuż krawędzi {#gestures-edges}

Te gesty zaczynasz przy samym brzegu ekranu, na przykład na pasku stanu lub na pasku nawigacji. Żeby zmienić przypisania, przejdź do **Gesty** > **Krawędzie**. Gesty przy krawędziach są niedostępne na zegarku.

#### Przesuń palec od krawędzi {#gestures-edges-slide-in}

- Przesuń palec w górę od dolnej krawędzi. Po około centymetrze usłyszysz kliknięcie jak przy naciśnięciu przycisku ekranu głównego. Jeżeli wtedy uniesiesz palec, wrócisz na ekran główny. W ustawieniach ten gest to **Przesuń palcem w górę od dolnej krawędzi**. Jeżeli chcesz otworzyć ostatnio używane aplikacje, przesuń palec o około 4 cm dalej i unieś go po drugim kliknięciu. To gest **Przesuń palcem daleko w górę od dolnej krawędzi**.
- Otwórz powiadomienia gestem **Przesuń palcem w dół od lewego górnego rogu**, a szybkie ustawienia gestem **Przesuń palcem w dół od prawego górnego rogu**. W obu przypadkach unieś palec po pierwszym kliknięciu. Dłuższe warianty to **Przesuń palcem daleko w dół od lewego górnego rogu** oraz **Przesuń palcem daleko w dół od prawego górnego rogu**. Drugie kliknięcie usłyszysz w nich po przypisaniu własnego polecenia.
- Ruch od lewej krawędzi prowadź w prawo, a od prawej w lewo. Górna i dolna połowa każdej bocznej krawędzi mają po jednym krótkim i jednym długim geście. Do tej grupy należy na przykład gest **Przesuń palcem w prawo od lewej krawędzi, w górnej połowie**. Polecenia od bocznych krawędzi przypisujesz samodzielnie.

Pierwsze i drugie kliknięcie sygnalizują, że po uniesieniu palca SandFly wykona przypisane polecenie. Gdy do krótkiego gestu nic nie przypiszesz, SandFly traktuje ruch jak zwykłe przesunięcie aż do drugiego kliknięcia. Gdy pominiesz przypisanie do długiego gestu, drugiego kliknięcia nie usłyszysz. Zwykłe przesunięcie wykonasz także wtedy, gdy uniesiesz palec przed miejscem pierwszego kliknięcia.

Jeśli miniesz drugie kliknięcie, możesz wrócić palcem do miejsca pierwszego. Usłyszysz wtedy jedno krótkie stuknięcie. Jeśli cofniesz palec aż do krawędzi, usłyszysz dwa. Unieś tam palec, żeby anulować gest.

SandFly odtwarza coraz wyższe dźwięki przy dotknięciu krawędzi, przy pierwszym kliknięciu i przy drugim. Dźwięki i towarzyszące im wibracje możesz wyłączyć w **Dźwięk i wibracje** > **Poszczególne dźwięki i wibracje**. Więcej informacji znajdziesz w [rozdziale o dźwiękach i wibracjach](#sound).

Zacznij przy krawędzi i przesuń palec do miejsca pierwszego kliknięcia w ciągu około pół sekundy. Jeśli zaczniesz za daleko od brzegu albo wykonasz ruch zbyt wolno, SandFly rozpozna zwykłe przesunięcie lub rozpocznie eksplorację dotykiem. Eksploracja zaczyna się też wtedy, gdy przytrzymasz palec nieruchomo przy krawędzi przez 0,2 sekundy.

Jeśli zaczniesz w rogu, kierunek ruchu zdecyduje, który gest rozpozna SandFly. Gdy ruch pasuje do dwóch gestów, pierwszeństwo mają górna i dolna krawędź oraz ruch od krawędzi w głąb ekranu.

#### Przesuń palec wzdłuż krawędzi {#gestures-edges-slide-along}

Dotknij brzegu i prowadź palec wzdłuż niego. Na bocznej krawędzi przesuń palec w górę albo w dół, a na górnej lub dolnej w lewo albo w prawo. Tak wykonasz na przykład gest **Przesuń palcem w górę wzdłuż lewej krawędzi**.

Przed uniesieniem palca przesuń go przynajmniej tak daleko, jak do pierwszego kliknięcia w geście od krawędzi. Gestom wzdłuż brzegu nie towarzyszą dźwięki ani wibracje krawędzi. Każdy z tych gestów zacznie działać po przypisaniu polecenia.

#### Przewijaj wzdłuż krawędzi {#gestures-edges-scroll}

Aby przewijać ekran jednym palcem, otwórz **Gesty** > **Krawędzie** i włącz opcję dla krawędzi, której chcesz używać. Początkowo wszystkie cztery są wyłączone.

- **Przewijaj jednym palcem wzdłuż lewej krawędzi**
- **Przewijaj jednym palcem wzdłuż prawej krawędzi**
- **Przewijaj jednym palcem wzdłuż górnej krawędzi**
- **Przewijaj jednym palcem wzdłuż dolnej krawędzi**

Przesuwaj palec wzdłuż wybranej krawędzi. Po bokach przewijasz w pionie, a przy górnym i dolnym brzegu w poziomie. Zawartość przesuwa się za palcem tak jak przy przewijaniu dwoma palcami.

Przewijanie zastępuje przypisane gesty wzdłuż wybranej krawędzi. Polecenia wciąż możesz wywołać ruchem od krawędzi w głąb ekranu oraz przesunięciem w górę lub w dół od rogu.

#### Menu krawędzi {#gestures-edge-menus}

Na lewej i prawej krawędzi możesz umieścić własne menu poleceń. Każde mieści do 20 pozycji.

1. Otwórz **Gesty** > **Krawędzie** > **Menu lewej krawędzi** lub **Menu prawej krawędzi**.
2. Włącz **Używaj lewej krawędzi jako menu** lub **Używaj prawej krawędzi jako menu**.
3. Wybierz **Dodaj polecenie**.
4. Wybierz polecenie z listy. Są tu te same polecenia co w ustawieniach gestów.

Dla każdej pozycji dostępne są **Zmień polecenie**, **Przenieś w górę**, **Przenieś w dół** i **Usuń**.

Żeby wybrać polecenie, dotknij samej krawędzi i przesuwaj palec w górę albo w dół. SandFly rozkłada pozycje na równe części krawędzi, od góry do dołu. Za każdym razem trafisz na tę samą pozycję w tym samym miejscu. Gdy palec znajdzie się nad poleceniem, usłyszysz krótki dźwięk, a następnie jego nazwę. Unieś palec, żeby je wykonać.

Aby anulować wybór, odsuń palec od brzegu na odległość większą niż trzykrotność ustawionej szerokości krawędzi. Po komunikacie o anulowaniu unieś palec. Możesz też wrócić do brzegu bez odrywania palca i wybrać inne polecenie.

Gdy używasz krawędzi jako menu, ruch wzdłuż niej służy do wyboru polecenia. Menu zastępuje przewijanie i pozostałe gesty wzdłuż tej krawędzi. Gest od brzegu w głąb ekranu działa także przy włączonym menu. Jeśli zaczniesz ruch na klawiaturze ekranowej, menu się nie uaktywni. Klawiszy przy krawędzi możesz używać tak jak zwykle.

#### Ustawienia krawędzi {#gestures-edge-settings}

W kategorii **Krawędzie** na ekranie **Gesty** możesz dostosować odległości i zdecydować, co usłyszysz przy kliknięciach.

- Zmień **Szerokość krawędzi**, jeśli chcesz rozpoczynać gest dalej od brzegu. Możesz ustawić od 0,5 do 2 cm. Domyślnie jest to 0,5 cm.
- **Odległość do pierwszego kliknięcia** to droga, którą musi pokonać palec od krawędzi albo wzdłuż niej. Możesz ustawić od 0,5 do 2 cm. Domyślnie jest to 1 cm.
- **Odległość od pierwszego do drugiego kliknięcia** to droga, o którą przesuwasz palec dalej, żeby wykonać długi gest. Możesz ustawić od 1 do 5 cm. Domyślnie są to 4 cm.
- Włącz **Odczytuj polecenie przy każdym kliknięciu**, jeśli chcesz usłyszeć, jakie polecenie uruchomi się po uniesieniu palca. Opcja jest domyślnie wyłączona.

### Gesty w rogach ekranu {#gestures-corners}

Wybierz **Gesty** > **Rogi**, aby przypisać polecenia do stuknięć w rogach ekranu. W każdym rogu możesz stuknąć jednym palcem raz, dwa lub trzy razy. Po drugim albo trzecim stuknięciu możesz też przytrzymać palec. To pięć gestów na róg, w tym na przykład **Dwukrotne kliknięcie lewego górnego rogu**. SandFly domyślnie nie przypisuje im poleceń.

Jeśli nie przypiszesz polecenia do dwukrotnego stuknięcia w danym rogu, gest aktywuje element, na którym jest fokus. Po drugim stuknięciu SandFly czeka na następne tylko wtedy, gdy w tym rogu jest przypisane polecenie do trzech stuknięć.

Jeśli przesuniesz palec lub dołożysz drugi, SandFly obsłuży dotyk jak zwykle. Tak samo zareaguje, jeśli zatrzymasz palec na ekranie, a w tym rogu nie ma przypisania do gestu z przytrzymaniem. Z rogu możesz więc rozpocząć przesunięcie lub gest od krawędzi. SandFly rozpoznaje stuknięcia w rogach tylko poza obszarem klawiatury ekranowej.

- **Rozmiar rogu** określa długość boku kwadratu, w którym działają gesty. Do wyboru masz od 0,5 do 2 cm. Domyślna wartość to **Taki sam jak szerokość krawędzi**.
- **Limit czasu kliknięcia rogu** określa, po jakim czasie SandFly uzna dotknięcie za przytrzymanie. Możesz ustawić od 0,15 do 0,5 sekundy. Wartość domyślna to **Domyślne ustawienie systemu**.

Na zegarku te gesty są niedostępne.

### Stuknięcie, a potem przesunięcie {#gestures-tap-then-swipe}

Stuknij raz, dwa albo trzy razy dwoma, trzema lub czterema palcami. Następnie przyłóż tyle samo palców do ekranu i przesuń je. Przykładem jest gest **Kliknij dwukrotnie, a potem przesuń w górę 2 palcami**. Są też warianty, w których stukasz dwoma palcami, a przesuwasz tylko jeden. Do tej grupy należy **Kliknij 2 palcami, a potem przesuń w górę**. Aby zacząć ich używać, otwórz **Gesty** > **Kliknij, a potem przesuń** i przypisz polecenia.

Zacznij przesunięcie przy następnym dotknięciu ekranu. Masz na to 0,25 sekundy od ostatniego stuknięcia. Gdy kombinacja ma przypisane polecenie, SandFly czeka po stuknięciach, żeby sprawdzić, czy wykonasz też przesunięcie. Na zegarku te gesty są niedostępne.

### Stuknięcia w tył urządzenia {#gestures-back-tap}

Stuknij palcem w tył telefonu dwa, trzy lub cztery razy. Żeby ustawić, co ma się wtedy wydarzyć, przejdź do **Gesty** > **Stuknięcie w tył urządzenia**. Polecenia do tych trzech gestów przypisujesz samodzielnie.

- **Stuknij dwukrotnie w tył urządzenia**
- **Stuknij trzykrotnie w tył urządzenia**
- **Stuknij czterokrotnie w tył urządzenia**

SandFly wykrywa stuknięcia akcelerometrem. Wystarczy przypisać polecenie do jednego z tych gestów. Wykrywanie działa przy włączonym ekranie i wyłącza się po wstrzymaniu SandFly. Gdy stukasz w obudowę, nie dotykaj ekranu. Z tych gestów możesz korzystać także przy włączonej [blokadzie dotyku](#touch). Są niedostępne na zegarkach i telewizorach.

Opcja **Czułość stuknięcia w tył urządzenia** ma domyślną wartość **Średni**. Przy wartości **Niska** trzeba stukać najmocniej, a po wybraniu **Wysoka** wystarczą najlżejsze stuknięcia. Zwiększ czułość, jeśli SandFly pomija stuknięcia. Zmniejsz ją, jeśli polecenia uruchamiają się przy odkładaniu lub przenoszeniu telefonu.

Po dwukrotnym stuknięciu SandFly czeka na kolejne tylko wtedy, gdy przypiszesz polecenie do trzech lub czterech stuknięć. Jeśli korzystasz tylko z dwóch stuknięć, polecenie uruchomi się szybciej, gdy pozostałe gesty zostawisz bez przypisań. Po trzech stuknięciach SandFly czeka tylko, gdy przypiszesz gest z czterema.

### Potrząśnięcie urządzeniem {#gestures-shake}

Aby ustawić polecenie, przejdź do **Gesty** > **Potrząśnięcie** i wybierz gest **Potrząśnij**. Domyślnie jest nieprzypisany.

Szybko porusz telefonem tam i z powrotem. SandFly rozpozna potrząśnięcie, jeśli wykonasz przynajmniej dwa ruchy w każdą stronę w ciągu około dwóch trzecich sekundy. Dłuższe potrząsanie uruchomi polecenie tylko raz. Aby je powtórzyć, najpierw zatrzymaj telefon na chwilę.

Jeśli gest ma przypisane polecenie, SandFly wykrywa ruch akcelerometrem przy włączonym ekranie. Wstrzymanie SandFly wyłącza wykrywanie. Potrząśnięcie działa także przy blokadzie dotyku. Na zegarkach i telewizorach jest niedostępne.

Opcja **Czułość potrząśnięcia** ma domyślną wartość **Średni**. Ustawienie **Niska** wymaga najsilniejszego potrząśnięcia. Po wybraniu **Wysoka** SandFly rozpozna też lekki ruch.

### Gesty na czytniku linii papilarnych {#gestures-fingerprint}

Kategoria **Gesty związane z odciskiem palca** pojawia się wtedy, gdy telefon przekazuje SandFly przesunięcia po czytniku linii papilarnych. Znajdziesz w niej cztery gesty.

- **Przesuń palcem w górę po czytniku linii papilarnych**
- **Przesuń palcem w dół po czytniku linii papilarnych**
- **Przesuń palcem w lewo po czytniku linii papilarnych**
- **Przesuń palcem w prawo po czytniku linii papilarnych**

Przypisania tych gestów obowiązują we wszystkich profilach gestów.

### Ustawienia gestów {#gestures-screen}

W głównych ustawieniach SandFly wybierz **Gesty**. Opcje u góry ekranu są w tej kolejności.

- Wybierz **Szukaj**, żeby znaleźć gest po nazwie lub po przypisanym poleceniu. Po wybraniu wyniku zobaczysz listę poleceń dla tego gestu.
- **Ćwiczenie gestów** pozwala [wypróbować gesty](#gestures-practice).
- **Resetowanie ustawień gestów** [przywraca domyślne przypisania](#gestures-reset).
- Opcja **Układ gestów** pozwala [wybrać zestaw przypisań](#gestures-layout). Przełącznik **Obsługuj przesunięcia dwoma palcami w SandFly** [zmienia działanie przesunięć](#gestures-two-finger-swipes).
- Dalej są [ustawienia czasu](#gestures-timing) oraz opcja **Rozpoznawaj przesunięcia przed uniesieniem palca**. Zobacz [](#gestures-swipes-before-lift).
- **Gesty w aplikacjach**, **Profile gestów** i **Skróty aplikacji** otwierają ustawienia [dla poszczególnych aplikacji](#gestures-in-apps), [profili](#gestures-profiles) i [skrótów](#gestures-app-shortcuts).

Poniżej znajdziesz zwinięte kategorie. Rozwiń wybraną kategorię, aby zobaczyć gesty wraz z przypisanymi poleceniami.

- **1 palec**, **1 palec, tam i z powrotem**, **1 palec, linia łamana**, **Krawędzie** i **Rogi**.
- **2 palce**, **2 palce, linia łamana**, **3 palce**, **4 palce** i **Kliknij, a potem przesuń**.
- **Gesty związane z odciskiem palca**, **Stuknięcie w tył urządzenia**, **Potrząśnięcie** i **Przesunięcia trzyczęściowe**.
- **Przesunięcia dwuetapowe** i **Przesunięcia dwuetapowe zakończone gestem tam i z powrotem lub pod kątem**. Osobne kategorie grupują kombinacje według pierwszego ruchu. Zobacz [](#gestures-two-stroke).

Na zegarku znajdziesz też opcję **Zarezerwuj gesty dla Vibration Watch**. Szczegóły opisano w [rozdziale o zegarkach i telewizorach](#devices).

#### Układ gestów {#gestures-layout}

W ustawieniach **Gesty** wybierz **Układ gestów**. Dostępne są trzy zestawy przypisań.

- **Gesty SandFly** to układ domyślny.
- **Gesty VoiceOver** działają podobnie do gestów czytnika na iPhonie. Stuknięcie dwoma palcami wstrzymuje mowę. Stuknięcie dwa razy dwoma palcami uruchamia **Dyktuj, odtwórz lub wstrzymaj multimedia**. Przesuń trzema palcami, żeby przewinąć ekran. Gdy stukniesz trzema palcami, SandFly wykona polecenie **Odczytaj pozycję**. Gesty, których nie ma w VoiceOver, mają wartość **Nieprzypisany** i możesz ustawić je samodzielnie.
- **Gesty BlackBerry** działają podobnie do gestów BlackBerry 10 Screen Reader (BlackBerry 10 OS 10.3). Nie wszystkie gesty są takie same. Przesunięcia w prawo i w lewo przechodzą według ustawienia nawigacji, a przesunięcia w górę i w dół wybierają następne lub poprzednie ustawienie nawigacji, na przykład znaki, słowa lub wiersze. Przesunięcie w dół, a potem w lewo wstrzymuje mowę. Przesunięcie w dół, a potem w prawo uruchamia szybkie zadania. Przesunięcie w dół, a potem w górę odczytuje godzinę i datę, a przesunięcie w górę, a potem w dół odczytuje stan urządzenia. Stuknięcie dwoma palcami odczytuje tytuł ekranu. Stuknięcie dwa razy dwoma palcami uruchamia **Dyktuj, odtwórz lub wstrzymaj multimedia**, które odbiera i kończy połączenia, a stuknięcie dwa razy dwoma palcami z przytrzymaniem kończy połączenie. Przesunięcia dwoma palcami w górę i w dół przewijają wstecz i do przodu, a przesunięcie dwoma palcami w dół, a potem w lewo włącza lub wyłącza mowę. Przesunięcie trzema palcami w dół czyta od góry, a przesunięcia trzema palcami w prawo i w lewo przewijają w lewo i w prawo. **Przesuń palcem w dół od lewego górnego rogu** i **Przesuń palcem w dół od prawego górnego rogu** otwierają szybkie ustawienia, a długie przesunięcia otwierają powiadomienia. Gesty, których nie ma w BlackBerry 10 Screen Reader, mają wartość **Nieprzypisany** i możesz ustawić je samodzielnie. Zobacz [](#gestures-quick-tasks). Te gesty mogą być nieznane, jeśli nie używałeś BlackBerry 10, dlatego ich wybór otwiera okno **Używać gestów BlackBerry?**. Wybierz **Używaj**, aby potwierdzić. Tego układu nie ma w oknie **Wybierz gesty**.

Przy pierwszym uruchomieniu SandFly wybierasz układ w oknie **Wybierz gesty**. SandFly zapisuje zmiany osobno dla każdego układu. Jeśli później wrócisz do poprzedniego, odzyskasz jego przypisania. Gesty ustawione dla aplikacji i profile gestów działają we wszystkich układach.

Przełączenie układu zmienia też ustawienie **Obsługuj przesunięcia dwoma palcami w SandFly**. Układ **Gesty VoiceOver** włącza tę opcję, a **Gesty SandFly** ją wyłącza. W układach **Gesty VoiceOver** i **Gesty BlackBerry** polecenie **Ekran główny** na ekranie blokady wysyła przesunięcie dwoma palcami w górę. Odblokowuje to telefon albo wyświetla pole kodu PIN, wzoru lub hasła.

#### Szybkie zadania {#gestures-quick-tasks}

Polecenie **Szybkie zadania** działa podobnie do trybu Quick Tasks w BlackBerry 10 Screen Reader. SandFly odczytuje „Szybkie zadania”, a przez 3 sekundy poniższe gesty uruchamiają podane polecenia.

| Gest | Polecenie |
| --- | --- |
| Przesunięcie w górę, przesunięcie w dół | Zwiększ głośność, Zmniejsz głośność |
| Przesunięcie w prawo, przesunięcie w lewo | Zwiększ szybkość mowy, Zmniejsz szybkość mowy |
| Przesunięcie dwoma palcami w górę | Powtórz ostatnią wypowiedź |
| Przesunięcie dwoma palcami w dół | Przeliteruj ostatnią wypowiedź |
| Stuknięcie dwoma palcami | Ukryj lub pokaż ekran |

Każdy z tych gestów wydłuża czas o kolejne 3 sekundy. Inny gest kończy szybkie zadania i uruchamia własne polecenie. Po upływie czasu telefon wibruje. W układzie **Gesty BlackBerry** szybkie zadania uruchamia przesunięcie w dół, a potem w prawo. W innych układach przypisz polecenie na ekranie **Gesty**. Na ekranie **Polecenia** znajdziesz je w grupie **SandFly**.

#### Obsługa przesunięć dwoma palcami w SandFly {#gestures-two-finger-swipes}

Gdy wyłączysz **Obsługuj przesunięcia dwoma palcami w SandFly**, przesunięcia trafiają do aplikacji i służą do przewijania. Przy każdym z czterech kierunków na ekranie **Gesty** usłyszysz wtedy „nie można dostosować”.

Po włączeniu opcji przesunięcia uruchamiają przypisane polecenia SandFly. Na ekranie **Gesty** pojawia się wtedy także kategoria **2 palce, linia łamana**. Ta opcja wymaga Androida 11 lub nowszego.

#### Ustawienia czasu rozpoznawania gestów {#gestures-timing}

- **Maksymalny odstęp między kliknięciami jednym palcem** daje od 0,1 do 2 sekund na kolejne stuknięcie. Domyślna wartość to 0,25 sekundy. Skróć odstęp, jeśli chcesz, żeby SandFly szybciej reagował na dwukrotne stuknięcie. Jeśli stukniesz za wolno, SandFly rozpozna dwa osobne dotknięcia. Wydłuż odstęp, jeśli potrzebujesz więcej czasu na drugie stuknięcie. SandFly poczeka wtedy dłużej także po stuknięciu w róg lub pojedynczym stuknięciu kilkoma palcami. Przy odstępie dłuższym niż 0,3 sekundy pojedyncze stuknięcie przenosi fokus na element pod palcem 0,25 sekundy po uniesieniu palca, a drugie stuknięcie w tym czasie nadal aktywuje ten element. W gestach wykonywanych kilkoma palcami masz zawsze co najmniej 0,25 sekundy na kolejne stuknięcie.
- W opcji **Czas przytrzymania po przesunięciu** ustawiasz, jak długo trzeba trzymać palec w miejscu, by wykonać [gest z przytrzymaniem](#gestures-swipe-and-hold). Do wyboru masz od 0,1 do 0,5 sekundy. Domyślnie jest to 0,25 sekundy.
- **Czas rozpoczęcia gestu** wyznacza, ile czasu masz na przebycie pierwszego centymetra jednym palcem. Możesz ustawić od 0,15 do 2 sekund. Domyślna wartość to 0,15 sekundy. Jeśli palec w tym czasie nie przebył centymetra, SandFly zacznie odczytywać elementy pod nim. Wydłuż czas, jeśli zdarza się to przy celowo wolniejszych przesunięciach.
- **Odstęp między przesunięciami dwuetapowymi** daje 0,25, 0,5, 1, 1,5 lub 2 sekundy na rozpoczęcie drugiego ruchu. Czas biegnie od uniesienia palca po pierwszym ruchu. Przy domyślnej wartości **Wyłączone** SandFly nie rozpoznaje [gestów dwuetapowych](#gestures-two-stroke) i ukrywa ich kategorie.

Ustawienia czasu działają przy włączonej opcji **Używaj gestów w SandFly**. Zobacz [](#gestures-recognition). Na zegarku możesz zmienić tylko odstęp między stuknięciami.

#### Rozpoznawanie przesunięć przed uniesieniem palca {#gestures-swipes-before-lift}

W ustawieniach **Gesty** możesz włączyć **Rozpoznawaj przesunięcia przed uniesieniem palca**. Przy zwykłym przesunięciu jednym palcem SandFly uruchomi polecenie, gdy przesuniesz palec o centymetr. SandFly poczeka na uniesienie palca, jeśli w tym kierunku masz przypisany gest z przytrzymaniem albo jeśli ruch może się okazać gestem od krawędzi.

Po włączeniu opcji znikają kategorie **1 palec, tam i z powrotem**, **1 palec, linia łamana**, **Przesunięcia trzyczęściowe**, **2 palce, linia łamana** oraz kategorie przesunięć dwuetapowych ze zmianą kierunku. Przestają działać gesty z tych kategorii. Zaczynają się tak samo jak zwykłe przesunięcie, które SandFly od tej pory rozpoznaje wcześniej.

Opcja jest domyślnie wyłączona. W tym ustawieniu SandFly sprawdza, czy ruch może być początkiem przypisanego gestu z przytrzymaniem, tam i z powrotem, po linii łamanej lub trzyczęściowego. Jeśli pasuje tylko do prostego przesunięcia, SandFly może wykonać polecenie jeszcze przed uniesieniem palca. Układ **Gesty SandFly** ma domyślne polecenia dla gestów tam i z powrotem. W układzie **Gesty VoiceOver** powyższe kombinacje wymagają własnych przypisań.

Możesz włączyć lub wyłączyć tę opcję osobno dla każdego [profilu gestów](#gestures-profiles). Na zegarku opcja jest niedostępna.

### Zmień polecenie przypisane do gestu {#gestures-change}

1. Otwórz **Gesty**.
2. Rozwiń kategorię, na przykład **3 palce**.
3. Wybierz gest. Otworzy się lista poleceń.
4. Wybierz polecenie lub ustaw **Nieprzypisany**, żeby usunąć przypisanie.

Na początku listy poleceń znajdziesz **Szukaj** i **Nieprzypisany**. SandFly rozwija tylko kategorię z aktualnym poleceniem. Jeśli pamiętasz polecenie i chcesz ustalić, jaki gest je uruchamia, wybierz **Szukaj** na głównym ekranie **Gesty**.

Możesz też najpierw wybrać polecenie na ekranie **Polecenia**, a potem przypisać do niego gest. Zobacz [rozdział o poleceniach i menu](#commands).

### Przywróć domyślne przypisania gestów {#gestures-reset}

Wybierz **Gesty** > **Resetowanie ustawień gestów**. Gdy pojawi się pytanie **Zresetować gesty?**, wybierz **Resetuj**. SandFly przywróci domyślne przypisania wyłącznie w używanym układzie gestów.

### Gesty w aplikacjach {#gestures-in-apps}

Ten sam gest lub skrót klawiszowy może mieć inne działanie w wybranej aplikacji. SandFly korzysta z tego przypisania, gdy aplikacja jest na pierwszym planie. Na ekranie blokady, w panelu powiadomień i w szybkich ustawieniach SandFly używa przypisań interfejsu systemowego Androida.

1. Otwórz **Gesty** > **Gesty w aplikacjach**.
2. Wybierz **Dodaj aplikację**.
3. Wskaż aplikację z listy.
4. W sekcji **Gesty** wybierz **Dodaj gest**. Przy każdym geście zobaczysz aktualne polecenie.
5. Wskaż gest.
6. Otwórz kategorię poleceń.
7. Wybierz polecenie. Wartość **Nieprzypisany** wyłącza gest w tej aplikacji.

Na ekranie danej aplikacji przy każdym geście zobaczysz jego polecenie. Otwórz gest i użyj **Wybierz polecenie**, żeby je zmienić, albo **Usuń**, żeby usunąć przypisanie.

Aby dodać skrót dla aplikacji, wybierz **Skróty klawiszowe** > **Dodaj skrót klawiszowy** i naciśnij klawisze, których chcesz używać. Sposób przypisywania poleceń opisano w [rozdziale o klawiaturach fizycznych](#keyboards). Opcja **Usuń aplikację** kasuje wszystkie ustawione dla niej przypisania gestów i skrótów.

Polecenie **Gesty dla tej aplikacji** otwiera stronę przypisań dla aplikacji, z której właśnie korzystasz. Możesz tam dodać gest od razu.

Przypisania dla aplikacji zapiszesz przy użyciu **Eksportuj do pliku**, a wczytasz przy użyciu **Importuj z pliku**. Zobacz [dodatek o formacie plików](#api-item-files). Jeśli [aktywny profil gestów](#gestures-profiles) też ma przypisanie dla danego gestu, ma ono pierwszeństwo.

### Profile gestów {#gestures-profiles}

Profil gestów pozwala przełączyć cały zestaw przypisań naraz. Wybierz **Domyślny**, żeby wyłączyć aktywny profil.

1. Otwórz **Gesty** > **Profile gestów**.
2. Wybierz **Dodaj profil gestów**.
3. Wpisz nazwę lub zaakceptuj podpowiedź.
4. Otwórz utworzony profil.
5. Przypisz polecenia do gestów tak samo jak na ekranie **Gesty**.

W nowym profilu przy każdym geście jest ustawiona wartość **Domyślny**, pierwsza pozycja na liście poleceń. SandFly korzysta wtedy z przypisania na ekranie **Gesty**, ustawienia danej aplikacji lub skryptu. Polecenie ustawione w profilu ma pierwszeństwo przed ustawieniami aplikacji i skryptami.

Na ekranie profilu dostępne są **Zmień nazwę**, **Przenieś w górę**, **Przenieś w dół**, **Eksportuj do pliku** i **Usuń profil gestów**. Tutaj możesz też osobno ustawić **Rozpoznawaj przesunięcia przed uniesieniem palca**. Dopóki tego nie zrobisz, obowiązuje ustawienie z ekranu **Gesty**.

Profil możesz przełączać z ustawień, z menu lub przez przypisane polecenie.

- Otwórz **Gesty** > **Profile gestów** > **Używany profil gestów**.
- Po dodaniu pierwszego profilu opcja **Profil gestów** jest dostępna w sterowaniu czytaniem i w menu SandFly.
- Użyj polecenia **Wybierz profil gestów**, **Następny profil gestów** lub **Poprzedni profil gestów**. Każdy profil, w tym **Domyślny**, ma też własne polecenie, na przykład **Użyj profilu gestów Podróż**. Możesz przypisać te działania do gestów, również w samym profilu, albo do skrótów klawiszowych.

Pozycja **Domyślny** jest pierwsza w kolejności przełączania, a za nią są profile w kolejności z ekranu **Profile gestów**. Po zmianie SandFly odczytuje nazwę używanego profilu.

Profile gestów nie zmieniają przypisań na czytniku linii papilarnych ani skrótów klawiszowych. Zestawy innych ustawień opisano w [rozdziale o profilach ustawień i kopiach zapasowych](#profiles).

### Skróty aplikacji {#gestures-app-shortcuts}

Android może pokazać skróty po długim naciśnięciu ikony aplikacji na ekranie głównym. Przykładem jest skrót do napisania nowej wiadomości. SandFly wyświetla takie pozycje w kategorii poleceń **Skróty aplikacji**. Są tam też własne skróty dodane na ekranie o tej samej nazwie. Skróty tworzone przez aplikację w trakcie jej działania, na przykład ostatnie rozmowy, oraz skróty, które może otwierać tylko aplikacja ekranu głównego, pojawiają się na liście od Androida 12, gdy Shizuku jest uruchomione przez adb lub debugowanie bezprzewodowe albo z uprawnieniami roota. Lista poleceń odczytuje je przy każdym otwarciu.

Skróty z listy możesz uruchamiać gestem, poleceniem z menu krawędzi lub kombinacją klawiszy.

Możesz też dodać do SandFly skrót, który zwykle umieszczasz na ekranie głównym, na przykład do kontaktu lub makra w innej aplikacji.

1. Otwórz **Gesty** > **Skróty aplikacji**.
2. Wybierz **Dodaj skrót aplikacji**.
3. Wybierz rodzaj skrótu.
4. Uzupełnij dane na ekranie, który otworzy aplikacja.

Nowy skrót pojawi się na liście. Możesz tam zmienić jego nazwę lub go usunąć. Więcej o otwieraniu aplikacji i skrótów znajdziesz w [rozdziale o poleceniach i menu](#commands).

### Przećwicz gesty {#gestures-practice}

Wybierz **Ćwiczenie gestów** na ekranie **Gesty** lub użyj polecenia o tej samej nazwie. Próbuj dowolnych gestów. Po każdym SandFly odczyta tylko nazwę przypisanego polecenia.

### Trener gestów {#gestures-trainer}

**Pomoc i opinie** > **Trener gestów** mierzy gesty i porównuje je z limitami, których SandFly używa przy Twoich ustawieniach. Funkcja wymaga Androida 13 lub nowszego i jest niedostępna na zegarkach i telewizorach.

1. Wybierz grupę, na przykład **Jeden palec**, **Menu krawędzi** lub **Stuknięcie w tył urządzenia**.
2. Przeczytaj opis gestu i wybierz **Start**.
3. Wykonaj gest 3 razy. Po każdej próbie SandFly odczytuje wynik.

Podczas prób każde dotknięcie jest próbą, a gesty nie uruchamiają poleceń. Po ostatniej próbie ekran pokazuje pomiary każdej próby, wskazówki, co zmienić, oraz ustawienia wpływające na limity, każde z przyciskiem. **Powtórz** rozpoczyna próby od nowa, **Następne ćwiczenie** przechodzi do następnego gestu, a **Spis treści** pokazuje ćwiczenia z grupy.

Grupa pomija gesty wyłączone w Twoich ustawieniach i podaje, które ustawienie je wyłącza. Krawędź bez menu krawędzi ćwiczysz na przykładowych elementach, które nie uruchamiają poleceń.
