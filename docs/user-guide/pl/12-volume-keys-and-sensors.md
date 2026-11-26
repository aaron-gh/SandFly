## Klawisze głośności i czujniki {#buttons}

Na ekranie **Klawisze głośności** w ustawieniach SandFly wybierzesz, co robią przyciski głośności, gdy SandFly jest włączony. Mogą regulować suwaki, przesuwać kursor i uruchamiać polecenia. Tutaj ustawisz też sygnały zmiany głośności. Na końcu rozdziału znajdziesz funkcje korzystające z czujników telefonu.

### Klawisze głośności i głośność mowy {#buttons-speech-volume}

Aby zmienić głośność mowy na dowolnym ekranie, przytrzymaj jeden palec na ekranie i naciśnij przycisk zwiększania lub zmniejszania głośności. Przyciski zmieniają też głośność mowy w trakcie czytania ciągłego. O głośności mowy i innych ustawieniach dźwięku przeczytasz w [](#sound).

Naciśnięcie obu przycisków jednocześnie nie uruchamia polecenia SandFly, dzięki czemu działa skrót ułatwień dostępu Androida. Ta kombinacja odblokowuje również dotyk. Zobacz [](#touch-lock).

### Reguluj suwaki i przesuwaj kursor {#buttons-sliders-cursor}

- **Klawisze głośności regulują suwaki** zmienia działanie przycisków, gdy fokus jest na suwaku. Zamiast głośności zmieniają wtedy wartość suwaka, a SandFly odczytuje nową wartość. Przytrzymaj przycisk, aby zmieniać ją dalej. Domyślnie opcja jest wyłączona.
- **Klawisze głośności przesuwają kursor** działa, gdy fokus jest w polu tekstowym. Przycisk zwiększania głośności cofa kursor o znak, a przycisk zmniejszania głośności przesuwa kursor o znak do przodu. SandFly odczytuje znak. Domyślnie opcja jest wyłączona.

Gdy przycisk ma też polecenie przypisane do jednego z naciśnięć (zobacz [](#buttons-commands)), jego naciśnięcia uruchamiają polecenia, a naciśnięcie bez polecenia po zakończeniu reguluje suwak albo przesuwa kursor zamiast zmieniać głośność.

Gdy fokus jest gdzie indziej albo ekran jest wygaszony, przyciski zwykle zmieniają głośność. Po przyłożeniu palca do ekranu oraz w trakcie czytania ciągłego nadal służą do regulacji głośności mowy.

### Uruchamiaj polecenia klawiszami głośności {#buttons-commands}

Dla każdego przycisku możesz przypisać polecenie do podwójnego, potrójnego lub długiego naciśnięcia. Odpowiadają za to ustawienia **Podwójne naciśnięcie przycisku zwiększania głośności**, **Potrójne naciśnięcie przycisku zwiększania głośności**, **Długie naciśnięcie przycisku zwiększania głośności**, **Podwójne naciśnięcie przycisku zmniejszania głośności**, **Potrójne naciśnięcie przycisku zmniejszania głośności** i **Długie naciśnięcie przycisku zmniejszania głośności**. Domyślnie żadne z tych naciśnięć nie ma przypisania.

1. Otwórz **Klawisze głośności**.
2. Wybierz rodzaj naciśnięcia, np. **Podwójne naciśnięcie przycisku zmniejszania głośności**.
3. Wybierz polecenie. Lista jest taka sama jak przy przypisywaniu gestów. Zobacz [](#commands).

Przycisk bez przypisanych poleceń zmienia głośność od razu. Gdy przypiszesz polecenie do podwójnego lub potrójnego naciśnięcia, pojedyncze naciśnięcie zmieni głośność po 0,3 sekundy od zwolnienia przycisku, jeśli nie nastąpi następne naciśnięcie. Gdy przypiszesz polecenie do długiego naciśnięcia, przytrzymanie uruchomi to polecenie zamiast wielokrotnie zmieniać głośność.

**Czas przytrzymania klawisza głośności** określa, kiedy SandFly uzna naciśnięcie za długie. Wybierz **Domyślne ustawienie systemu** albo wartość od 0,3 do 1 sekundy.

W poniższych sytuacjach przyciski zmieniają głośność tak jak zwykle i nie uruchamiają przypisanych poleceń.

- Ekran jest wygaszony. Wyjątkiem jest widoczny ekran blokady w trybie Always On Display lub włączona opcja **Skróty przy wygaszonym ekranie**. Zobacz [](#buttons-screen-off).
- Trwa rozmowa telefoniczna lub aplikacja odtwarza dźwięk rozmowy.
- Trzymasz palec na ekranie lub trwa czytanie ciągłe.
- Na ekranie jest aplikacja Menu skrótów.

Gdy telefon dzwoni, polecenia może uruchomić tylko przycisk, do którego jednego z naciśnięć przypisano **Odbierz połączenie**.

### Obsługa naciśnięć i panel głośności {#buttons-take-all}

- **SandFly obsługuje każde naciśnięcie klawisza głośności** przekazuje do SandFly wszystkie naciśnięcia przycisków przy włączonym ekranie. Naciśnięcia z przypisaniami uruchamiają polecenia. Pozostałe, w tym przytrzymanie, nie zmieniają głośności. Przy włączonej opcji pojawiają się ustawienia **Naciśnięcie klawisza zwiększania głośności** i **Naciśnięcie klawisza zmniejszania głośności**, które pozwalają przypisać polecenie do pojedynczego naciśnięcia. Domyślnie opcja jest wyłączona.
- **Ukryj panel głośności** przy włączonym ekranie zmienia głośność naciśnięciami bez polecenia, ale bez pokazywania panelu Androida. Domyślnie opcja jest wyłączona. Po włączeniu obu opcji naciśnięcia bez przypisań nie zmieniają głośności.

Obie opcje zostawiają obsługę przycisków Androidowi, gdy ekran jest wygaszony, telefon dzwoni, trwa rozmowa, aplikacja odtwarza dźwięk rozmowy, SandFly jest wstrzymany lub trzymasz oba przyciski głośności naraz. Nadal działa regulacja suwaka, przesuwanie kursora i zmiana głośności mowy z palcem na ekranie.

### Skróty przy wygaszonym ekranie {#buttons-screen-off}

Po włączeniu **Skróty przy wygaszonym ekranie** przypisane naciśnięcia wykonują polecenia również wtedy, gdy ekran jest wyłączony. Naciśnięcia bez polecenia zmieniają głośność. Opcja wymaga Shizuku i jest ukryta, gdy SandFly używa zamiast niego dostępu root, a także na zegarkach. Zobacz [](#updates-shizuku). Gdy włączysz ją po raz pierwszy, pojawi się prośba o przyznanie SandFly dostępu do Shizuku. Domyślnie opcja jest wyłączona.

### Klawisz wyciszenia {#buttons-mute-key}

Na telefonach z klawiszem wyciszenia, takich jak Zinwa Q25, ekran **Klawisze głośności** ma dwie dodatkowe opcje.

- **Przytrzymaj klawisz wyciszenia, aby dyktować w polach tekstowych**: gdy pole tekstowe ma fokus wprowadzania, klawiatura jest widoczna lub podłączona i nie trwa rozmowa, przytrzymanie klawisza wyciszenia uruchamia **Dyktuj**. Nagrywanie zaczyna się po naciśnięciu klawisza i kończy po jego zwolnieniu, a SandFly wpisuje tekst. Naciśnięcie klawisza podczas dyktowania do pola tekstowego zatrzymuje dyktowanie. Domyślnie opcja jest włączona. Zobacz [](#typing-dictate).
- **Naciśnięcie klawisza wyciszenia**: polecenie, które naciśnięcie klawisza wyciszenia uruchamia w pozostałych sytuacjach. Lista jest taka sama jak przy gestach. Domyślnie bez przypisania.

Gdy żadna z tych opcji nie obsłuży naciśnięcia, trafia ono do mapy klawiszy. W mapach telefonu z klawiaturą i we własnych mapach klawisz wyciszenia może mieć własny skrót klawiszowy (zobacz [](#keyboards-keymaps)). W przeciwnym razie naciśnięcie trafia do Androida.

### Głośność mowy na telefonach Samsung {#buttons-samsung}

Na telefonach Samsung system One UI zmienia głośność mowy, gdy naciśniesz przycisk głośności w trakcie wypowiedzi SandFly albo przez sekundę po jej zakończeniu. SandFly przejmuje te naciśnięcia i zmienia głośność tego samego rodzaju dźwięku, co przy milczącej mowie, np. głośność multimediów.

Aby przyciski regulowały głośność mowy w trakcie wypowiedzi, włącz **Klawisze głośności zmieniają głośność mowy podczas mówienia**. Opcja jest dostępna tylko na telefonach Samsung i domyślnie jest wyłączona.

### Informacja zwrotna o zmianie głośności {#buttons-feedback}

W opcji **Informacja zwrotna o zmianie głośności** możesz wybrać, co SandFly odtwarza przy zmianie głośności.

- **Wyłączona** wycisza sygnał i komunikat.
- **Sygnał** odtwarza krótki dźwięk przy każdym kroku regulacji, z aktualnie ustawianą głośnością.
- **Mowa** po ostatnim kroku odczytuje, jakiego rodzaju głośność się zmieniła i jaki jest nowy poziom, np. „Głośność multimediów ustawiona na 60 procent”. To opcja domyślna.
- **Sygnał i mowa** odtwarza dźwięk i odczytuje komunikat.

Jeśli używasz sygnału, **Wyłącz sygnał podczas odtwarzania multimediów** wycisza go, gdy aplikacja odtwarza multimedia. Jeśli używasz komunikatów, **Wyłącz mowę podczas odtwarzania multimediów** pomija komunikat w trakcie odtwarzania, a **Przerywaj mowę, aby ogłosić głośność** zatrzymuje bieżącą wypowiedź, aby odczytać nowy poziom. Obie opcje dotyczące multimediów są domyślnie wyłączone. **Przerywaj mowę, aby ogłosić głośność** jest domyślnie włączone.

Opcja **Zmieniaj głośność mowy bez komunikatu podczas mówienia** pozwala regulować głośność mowy klawiszami, gdy SandFly mówi lub ma tekst w kolejce. Komunikat o zmianie zostaje pominięty, a SandFly kontynuuje wypowiedź z nową głośnością. Naciśnięcia w odstępie do sekundy tworzą jedną serię. Seria jest cicha także wtedy, gdy mowa się skończy przed ostatnim naciśnięciem. Wybrany sygnał dźwiękowy nadal się odtwarza. SandFly ogłasza zmiany przy naciśnięciu z palcem na ekranie albo gdy milczy, a także zmiany w panelu głośności i w aplikacjach. Na telefonach Samsung poza czytaniem ciągłym przyciski zmieniają głośność w trakcie mowy tylko po włączeniu **Klawisze głośności zmieniają głośność mowy podczas mówienia**. Opcja cichej zmiany jest domyślnie wyłączona.

Zmiany wykonane przez sam SandFly, np. poleceniem regulacji głośności, nie wywołują sygnału ani komunikatu. Zmiany z innych aplikacji wywołują wybraną informację zwrotną. Sygnał nie odtwarza się podczas rozmowy ani po wstrzymaniu SandFly.

### Czujniki {#buttons-sensors}

Poniższe funkcje korzystają z czujników telefonu.

- **Stuknięcie w tył urządzenia** uruchamia polecenia po dwóch, trzech lub czterech stuknięciach w tył telefonu. Znajdziesz je w menu **Gesty** > **Stuknięcie w tył urządzenia**. Zobacz [](#gestures-back-tap).
- **Potrząśnięcie** uruchamia polecenie po potrząśnięciu telefonem. Ustawisz je w menu **Gesty** > **Potrząśnięcie**. Zobacz [](#gestures-shake).
- **Zakryj czujnik zbliżeniowy, by zatrzymać wypowiedź** zatrzymuje mowę po zasłonięciu czujnika. Opcja jest domyślnie wyłączona. Znajdziesz ją w menu **Mowa** > **Zakryj czujnik zbliżeniowy, by zatrzymać wypowiedź**. Zobacz [](#speech-stop).
- **Automatycznie blokuj dotyk** > **Gdy czujnik zbliżeniowy jest zakryty** blokuje dotyk na czas zasłonięcia czujnika. Zobacz [](#touch-lock).
- **Potrząśnij, aby zatrzymać** oraz **Pomachaj, aby zatrzymać** zatrzymują odczyt powiadomienia po potrząśnięciu telefonem lub przesunięciu dłoni nad czujnikiem zbliżeniowym. Znajdziesz je w menu **Powiadomienia** > **Przestań czytać**. Zobacz [](#notifications-stop).
- **Włączaj tryb głośnomówiący po odsunięciu telefonu od ucha** przełącza rozmowę na głośnik po odsunięciu telefonu i z powrotem na słuchawkę po przyłożeniu do ucha. Opcja jest dostępna od Androida 12 w menu **Wyjście audio** > **Włączaj tryb głośnomówiący po odsunięciu telefonu od ucha**. Zobacz [](#sound-calls).
