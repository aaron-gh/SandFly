## Klawisze głośności i czujniki {#buttons}

Otwórz **Klawisze głośności** w ustawieniach SandFly. Możesz tam przypisać polecenia do klawiszy, włączyć zmianę wartości suwaków i przesuwanie kursora oraz ustawić sygnał zmiany głośności.

### Zmiana głośności mowy {#buttons-speech-volume}

Przytrzymaj palec na ekranie i naciśnij klawisz głośności. W ten sposób zmienisz głośność mowy na dowolnym ekranie. Klawisze regulują mowę także w trakcie czytania ciągłego. Pozostałe sposoby zmiany głośności znajdziesz w rozdziale o [dźwięku](#sound).

Naciśnięcie obu klawiszy naraz pozostawia działanie skrótu dostępności Androida. Pozwala też [odblokować dotyk](#touch-lock).

### Obsługa suwaków i kursora {#buttons-sliders-cursor}

- **Klawisze głośności regulują suwaki**. Ustaw fokus na suwaku i naciskaj klawisze głośności, żeby zmienić jego wartość. SandFly poda nową wartość. Przytrzymaj klawisz, żeby zmieniać ją dalej.
- **Klawisze głośności przesuwają kursor**. W polu tekstowym klawisz zwiększania głośności cofa kursor o znak, a klawisz zmniejszania przesuwa go w przód. SandFly odczytuje znak.

Obie opcje są domyślnie wyłączone. Na pozostałych elementach klawisze regulują głośność. Z palcem na ekranie i podczas czytania ciągłego regulują mowę.

### Przypisywanie poleceń do klawiszy {#buttons-commands}

Każdy klawisz ma trzy osobne skróty. Możesz nacisnąć go dwa razy, trzy razy lub przytrzymać. Początkowo żaden z tych skrótów nie ma przypisanego polecenia.

1. Otwórz **Klawisze głośności**.
2. Wybierz skrót, na przykład **Podwójne naciśnięcie przycisku zmniejszania głośności**.
3. Wybierz działanie. Do dyspozycji masz tę samą [listę poleceń](#commands) co przy przypisywaniu gestów.

Bez przypisania klawisz od razu zmienia głośność. Po przypisaniu podwójnego lub potrójnego naciśnięcia SandFly czeka 0,3 sekundy od puszczenia klawisza. Jeśli w tym czasie nie naciśniesz go ponownie, pojedyncze naciśnięcie zmieni głośność. Jeśli przypiszesz działanie do przytrzymania, klawisz je wykona po upływie czasu z opcji **Czas przytrzymania klawisza głośności**. Możesz użyć czasu systemowego albo ustawić od 0,3 do 1 sekundy.

Klawisze nadal regulują głośność w następujących sytuacjach:

- Ekran jest wygaszony. Wyjątki to ekran blokady widoczny na stałe i włączone [skróty przy wygaszonym ekranie](#buttons-screen-off).
- Trwa połączenie telefoniczne, aplikacja odtwarza dźwięk rozmowy albo otwarte jest Menu skrótów.
- Trzymasz palec na ekranie albo trwa czytanie ciągłe. Regulujesz wtedy głośność mowy.

Polecenia nie uruchamiają się również przy regulowaniu suwaka i przesuwaniu kursora. Gdy dzwoni telefon, skróty działają tylko na tym klawiszu, do którego przypisano **Odbierz połączenie**.

### Pojedyncze naciśnięcia i panel głośności {#buttons-take-all}

- **SandFly obsługuje każde naciśnięcie klawisza głośności**. Przy włączonym ekranie wszystkie naciśnięcia trafiają do SandFly. Pojawiają się wówczas opcje **Naciśnięcie klawisza zwiększania głośności** i **Naciśnięcie klawisza zmniejszania głośności**. Naciśnięcia i przytrzymania bez przypisania nie zmieniają głośności.
- **Ukryj panel głośności**. Przy włączonym ekranie klawisz bez przypisania zmienia głośność bez wyświetlania panelu Androida. Po włączeniu obu opcji takie naciśnięcie nie zmieni głośności.

Obie opcje są domyślnie wyłączone. Android nadal obsługuje klawisze przy wygaszonym ekranie, podczas rozmowy lub odtwarzania dźwięku rozmowy, przy połączeniu przychodzącym, po wstrzymaniu SandFly i po naciśnięciu obu klawiszy naraz. Klawiszami nadal możesz regulować suwaki, przesuwać kursor i zmieniać głośność mowy z palcem na ekranie.

### Skróty przy wygaszonym ekranie {#buttons-screen-off}

Włącz **Skróty przy wygaszonym ekranie**, żeby używać przypisanych poleceń również po wyłączeniu wyświetlacza. Naciśnięcia bez przypisania nadal zmieniają głośność.

Opcja jest początkowo wyłączona. Wymaga [Shizuku](#updates-shizuku), które przy pierwszym włączeniu poprosi o zgodę dla SandFly. Opcja nie pojawia się na zegarkach ani wtedy, gdy SandFly korzysta z roota zamiast Shizuku.

### Głośność mowy na telefonach Samsung {#buttons-samsung}

One UI domyślnie kieruje klawisze do regulacji głośności mowy, gdy czytnik mówi i jeszcze przez sekundę po zakończeniu wypowiedzi. SandFly przechwytuje wtedy klawisze, by regulowały ten sam kanał co w ciszy, na przykład głośność multimediów.

Jeśli wolisz w tej sytuacji regulować mowę, włącz **Klawisze głośności zmieniają głośność mowy podczas mówienia**. Przełącznik pojawia się tylko na telefonach Samsung i jest domyślnie wyłączony.

### Sygnał zmiany głośności {#buttons-feedback}

W opcji **Informacja zwrotna o zmianie głośności** wybierasz, co usłyszysz po użyciu klawiszy:

- Możesz wyłączyć sygnalizację.
- **Sygnał** odtwarza krótki dźwięk przy każdym kroku, z właśnie ustawianą głośnością.
- **Mowa** po ostatnim kroku podaje kanał i poziom głośności. To ustawienie domyślne.
- **Sygnał i mowa** łączy oba sposoby.

Za pomocą opcji **Wyłącz sygnał podczas odtwarzania multimediów** i **Wyłącz mowę podczas odtwarzania multimediów** możesz wyciszyć wybrany rodzaj sygnału w trakcie odtwarzania. Obie są początkowo wyłączone. Domyślnie włączona opcja **Przerywaj mowę, aby ogłosić głośność** pozwala od razu odczytać nową wartość.

Jeśli chcesz regulować trwający odczyt bez przerywania go komunikatem, włącz **Zmieniaj głośność mowy bez komunikatu podczas mówienia**. Dotyczy to także wypowiedzi czekających w kolejce. Kolejne naciśnięcia w odstępach do sekundy należą do tej samej serii i po ustaniu odczytu pozostają bez komunikatu. Sygnał dźwiękowy nadal może się odezwać. Zmiany z palcem na ekranie, w ciszy, z panelu głośności i z innych aplikacji nadal są ogłaszane. Na Samsungu podczas zwykłego odczytu klawisze regulują mowę tylko po włączeniu [odpowiedniej opcji](#buttons-samsung). Czytanie ciągłe działa pod tym względem tak samo jak na pozostałych telefonach.

Zmiany głośności wykonane poleceniem SandFly nie uruchamiają sygnalizacji. Zmiany z innych aplikacji mogą ją uruchomić. Sygnał dźwiękowy nie gra w trakcie rozmowy ani po wstrzymaniu czytnika.

### Czujniki telefonu {#buttons-sensors}

- **Gesty** > **Stuknięcie w tył urządzenia**. Przypisz polecenie do [dwóch, trzech lub czterech stuknięć](#gestures-back-tap) w tył telefonu.
- **Gesty** > **Potrząśnięcie**. Przypisz [polecenie do potrząśnięcia](#gestures-shake).
- **Mowa** > **Zakryj czujnik zbliżeniowy, by zatrzymać wypowiedź**. Ta opcja jest domyślnie wyłączona. Zobacz [przerywanie odczytu](#speech-stop).
- **Automatycznie blokuj dotyk** > **Gdy czujnik zbliżeniowy jest zakryty**. [Blokada dotyku](#touch-lock) działa, dopóki coś zasłania czujnik.
- **Powiadomienia** > **Przestań czytać**. Włącz **Potrząśnij, aby zatrzymać** lub **Pomachaj, aby zatrzymać**, żeby [przerwać odczyt powiadomienia](#notifications-stop).
- **Wyjście audio** > **Włączaj tryb głośnomówiący po odsunięciu telefonu od ucha**. Podczas rozmowy dźwięk przechodzi na głośnik po odsunięciu telefonu i wraca do słuchawki po przyłożeniu go do ucha. [Opcja wymaga Androida 12](#sound-calls).
