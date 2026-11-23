## Pisanie i edycja tekstu {#typing}

SandFly odczytuje klawisz pod palcem i może mówić, co wpisałeś. Możesz też dyktować lub pisać odręcznie. Osobne rozdziały opisują [klawiaturę brajlowską](#braille) i [klawiatury fizyczne](#keyboards), w tym metodę wprowadzania Klawiatura SandFly.

### Ustawienia pisania {#typing-settings}

Otwórz **Pisanie** w ustawieniach SandFly. Wybierz [**Echo klawiatury**](#typing-keyboard-echo), żeby ustawić, co usłyszysz podczas wpisywania. Na tym ekranie są też [ruch kursora dwukrotnym stuknięciem](#typing-double-tap-cursor), [ustawienia klawiatury ekranowej](#typing-on-screen-keyboard) oraz, po podłączeniu sprzętu, [ustawienia klawiatury fizycznej](#typing-physical-keyboard). W sekcji **Inne sposoby pisania** znajdziesz [dyktowanie](#typing-dictation) i [pismo odręczne](#typing-handwriting).

### Pisz na klawiaturze ekranowej {#typing-on-screen-keyboard}

Przesuwaj palcem po klawiaturze. SandFly odczytuje klawisz pod palcem. To, jak go naciskasz, zależy od wybranej metody pisania.

#### Wybierz metodę pisania {#typing-method}

Otwórz **Pisanie** > **Metoda pisania**. Opcje odpowiadają następującym sposobom.

- **Przytrzymaj palec, aby wybrać przycisk klawiatury, a następnie kliknij dwukrotnie**. Unieś palec i stuknij dwa razy w dowolnym miejscu. Wpiszesz ostatni odczytany klawisz.
- **Przytrzymaj palec, aby wybrać przycisk z literą, a następnie podnieś palec. Pozostałe przyciski wymagają dwukrotnego kliknięcia.** Literę wpiszesz po uniesieniu palca. Do innych klawiszy, na przykład usuwania lub Enter, użyj dwukrotnego stuknięcia. To ustawienie domyślne.
- **Przytrzymaj palec, aby wybrać przycisk klawiatury, a następnie podnieś palec**. Każdy klawisz działa po uniesieniu palca. Ta metoda ma najmniejsze opóźnienie.
- **Przytrzymaj palec, aby wybrać przycisk klawiatury, a następnie podnieś palec. Przyciski Enter, Gotowe lub Wyślij wymagają dwukrotnego kliknięcia.** W Gboard trzeba stuknąć dwa razy na klawiszu zatwierdzania, na przykład Enter, Gotowe, Wyślij lub Szukaj. Na pozostałych klawiszach i w innych klawiaturach wystarcza uniesienie palca.
- **Stuknij dowolny klawisz, aby go wpisać, lub przytrzymaj palec, aby wybrać dowolny klawisz, a następnie podnieś**. Krótkie stuknięcie wpisuje znak od razu. Dwa szybkie stuknięcia wpisują go dwa razy. Możesz też przesuwać palec po klawiszach i wpisać wybrany po uniesieniu. Metoda jest dostępna od Androida 13.

W każdej metodzie możesz przytrzymać jeden palec na klawiszu i stuknąć drugim palcem, żeby go wpisać. Na Androidzie 13 lub nowszym trzy ostatnie metody ustawiają [opóźnienie zaznaczenia podczas pisania](#typing-focus-delay) na 0,15 sekundy.

#### Przytrzymaj klawisz {#typing-touch-and-hold}

W **Pisanie** > **Opóźnienie podczas naciskania i przytrzymywania w czasie pisania** wybierz 0,5, 1, 2, 3 lub 5 sekund. Domyślnie SandFly czeka trzy sekundy, zanim wykona długie naciśnięcie, na przykład otworzy znaki alternatywne. Ustawienia nie ma przy metodzie, w której każdy klawisz wpisujesz dwukrotnym stuknięciem.

Włączenie **Przytrzymuj klawisze po upływie opóźnienia naciśnięcia i przytrzymania** przekazuje klawiaturze dalszy dotyk po upływie ustawionego czasu. Klawiatura może wtedy na przykład powtarzać usuwanie podczas trzymania klawisza. Opcja jest domyślnie wyłączona. Działa przy metodach pisania po uniesieniu palca, z wyjątkiem Gboard i Pixelboard w wersjach wcześniejszych niż 18.8.

#### Znaki alternatywne w Gboard {#typing-gboard-alternatives}

Przytrzymaj klawisz. Po ustawionym czasie Gboard pokaże znaki alternatywne, a SandFly odczyta wybrany znak. Przesuń palec do innego znaku, żeby go usłyszeć. Unieś palec, żeby go wpisać.

Od wersji 18.8 Gboard korzysta z eksploracji Androida. W starszym Gboard i Pixelboard od Androida 11 SandFly sam obsługuje dotyk na klawiaturze. Gdy okno znaków jest otwarte, palec poza nim wskazuje najbliższy znak. Uniesienie palca wpisuje znak i zamyka okno. Jeśli okno nie jest otwarte, wyjście palcem poza klawiaturę przełącza na zwykłą eksplorację ekranu. Uniesienie palca poza klawiaturą nic nie wpisuje. Ta obsługa starszych klawiatur nie dotyczy metody, w której wszystkie klawisze wpisujesz dwukrotnym stuknięciem.

#### Wibracja klawiszy Gboard {#typing-gboard-vibration}

Gboard od wersji 18.8 stosuje własne ustawienia sygnałów klawiszy. Starszy Gboard i Pixelboard potrzebują informacji o przesunięciu nad klawiszem. Jeśli masz skonfigurowane [Shizuku lub root](#updates), SandFly przekazuje tę informację do klawiatury. Bez niej w starszych wersjach sygnały klawiatury nie działają podczas eksploracji obsługiwanej przez SandFly. Nie ma osobnego przełącznika.

#### Ustaw czas odczytu klawisza po dotknięciu {#typing-focus-delay}

Wybierz **Pisanie** > **Opóźnienie zaznaczenia podczas pisania**. Możesz ustawić 0,15, 0,2, 0,25 lub 0,3 sekundy. Domyślne 0,3 sekundy dają najwięcej czasu przed odczytem. Opcja wymaga Androida 13 lub nowszego.

Możesz zmieniać ten sam czas podczas pisania, jeśli dodasz **Opóźnienie zaznaczenia podczas pisania** w grupie **Dotyk** na ekranie [**Sterowanie czytaniem**](#commands).

#### Szybszy odczyt klawiszy {#typing-faster-keys}

**Pisanie** > **Szybciej odczytuj przyciski klawiatury ekranowej** przygotowuje wcześniej dźwięk nazw klawiszy. Dzięki temu SandFly może szybciej je odczytać. Opcja jest domyślnie wyłączona i działa z syntezatorem Google.

#### Przesunięcia a klawiatura {#typing-swipes-keyboard}

Żeby wejść na klawiaturę z aplikacji lub paska systemowego, dotknij jej. Zwykłe przesunięcie nie przejdzie na klawiaturę. Jeśli fokus jest na klawiszu, przesunięcie zwykle wraca do aplikacji i kontynuuje od pola tekstowego. W metodzie z dwukrotnym stuknięciem każdego klawisza przesunięcia przechodzą po klawiszach. Możesz nimi również wyjść z klawiatury.

### Echo klawiatury {#typing-keyboard-echo}

1. Otwórz **Pisanie** > **Echo klawiatury**.
2. Wybierz **Klawiatura ekranowa**, **Klawiatura fizyczna**, **Klawiatura brajlowska**, **Monitor brajlowski** albo **Klawiatura pisma ręcznego**.
3. Wybierz **Znaki**, **Słowa**, **Znaki i słowa** lub wyłącz echo. Domyślnie każda klawiatura używa opcji **Znaki i słowa**.

Echo słów czyta wyraz w postaci wpisanej w polu, do poprzedniej spacji. Apostrof lub łącznik w słowie go nie dzieli. Adres WWW lub e-mail też jest jednym słowem. Jeśli klawiatura od razu wstawi wyraz, interpunkcję i spację, usłyszysz wyraz.

Echo każdej klawiatury ma własną pozycję na ekranie [**Sterowanie czytaniem**](#commands), w **Menu SandFly** i w kategorii poleceń **Mowa**. Pozycje menu i sterowania czytaniem dodajesz samodzielnie w grupie **Echo klawiatury**. Skrót **Włącz lub wyłącz echo klawiatury podczas pisania** przełącza następną opcję klawiatury fizycznej. Zobacz [wykaz skrótów](#appendix-keyboard-shortcuts).

### Przenieś kursor {#typing-move-cursor}

W polu tekstowym opcje **Znaki**, **Słowa**, **Wiersze**, **Zdania** i **Akapity** przenoszą kursor. SandFly czyta mijany tekst. Używając [domyślnych gestów](#appendix-gestures), najpierw wybierz [opcję sterowania czytaniem](#commands) ruchem w górę i w dół, w dół i w górę albo przesunięciem trzech palców w pionie. Potem przesuń jeden palec w dół, żeby przenieść kursor w przód, albo w górę, żeby się cofnąć.

**Przenieś kursor na początek** i **Przenieś kursor na koniec** są w kategorii **Edytowanie tekstu** oraz w [czynnościach pola tekstowego](#typing-editing-actions).

#### Przenieś kursor dwukrotnym stuknięciem {#typing-double-tap-cursor}

Włącz **Pisanie** > **Dwukrotne kliknięcie przenosi kursor na początek lub koniec**. Gdy edytujesz pole i masz aktywną klawiaturę, stuknięcie dwa razy przeniesie kursor na początek. Jeśli kursor już jest na początku, przeniesie go na koniec. SandFly podaje, na którym końcu jest kursor.

W pozostałych elementach oraz w polach, których nie edytujesz, dwukrotne stuknięcie zachowuje zwykłe działanie. Opcja wymaga Androida 13 i jest domyślnie wyłączona.

### Zaznaczanie, kopiowanie, wycinanie i wklejanie {#typing-select-copy-paste}

#### Zaznacz tekst {#typing-select-text}

1. Przenieś kursor na początek zakresu.
2. Użyj **Włącz lub wyłącz tryb zaznaczania**. SandFly potwierdzi włączenie.
3. Przenieś kursor, na przykład po znakach lub słowach. Zaznaczysz mijany tekst.
4. Użyj ponownie tego samego polecenia, żeby zakończyć tryb.

Możesz też użyć **Zaznacz wszystko** lub usunąć zaznaczenie. Tryb działa również w wiadomościach, na stronach i między elementami. Po wejściu do innego elementu SandFly rozpoczyna tam osobny zakres. Polecenie **Kopiuj** zapisuje wtedy każdy zakres w osobnym wierszu.

#### Skopiuj, wytnij lub wklej {#typing-copy-cut-paste}

- **Kopiuj** kopiuje zaznaczony tekst albo element, na którym jest fokus.
- **Dodaj do schowka** dopisuje tekst do bieżącej zawartości schowka w nowym wierszu.
- Wycinanie usuwa zaznaczony tekst z pola i kopiuje go do schowka.
- **Wklej** wstawia tekst ze schowka przy kursorze.

Domyślnie dwukrotne stuknięcie trzema palcami z przytrzymaniem wycina. Trzykrotne stuknięcie trzema palcami z przytrzymaniem wkleja. Zobacz [wykaz gestów](#appendix-gestures) i [ustawienia poleceń](#commands).

#### Czynności pola i menu edycji {#typing-editing-actions}

Gdy fokus jest w polu tekstowym, opcja **Czynności** w menu SandFly i w sterowaniu czytaniem pozwala ustawić kursor, zaznaczać, wycinać, kopiować i wklejać. Możesz też uruchomić **Szczegółowa edycja**. Tej czynności nie ma w polu hasła.

Polecenie **Menu edycji** otwiera historię schowka, kopiowanie, dodawanie do schowka, wycinanie, wklejanie, czyszczenie tekstu, zaznaczanie wszystkiego i przełączanie trybu zaznaczania.

#### Pozostałe polecenia edycji {#typing-editing-commands}

- **Prześlij tekst** naciska klawisz zatwierdzający klawiatury, na przykład Wyślij, Szukaj lub Gotowe.
- **Wyczyść tekst** usuwa zawartość pola.
- **Liczba znaków** liczy znaki i słowa. W polu hasła podaje tylko znaki.
- **Czytaj pole tekstowe** czyta całą zawartość lub informuje o pustym polu i odczytuje wskazówkę.
- **Podpowiedzi ortograficzne/gramatyczne** otwierają podpowiedzi dla błędu pod kursorem. Między błędami przechodzisz przez **Korekta**.
- **Dyktuj** [wstawia tekst z mowy](#typing-dictate).
- **Przełącz klawiaturę** wybiera następną klawiaturę. Wymaga Androida 11.
- **Wyszukaj emotikony**, **Wybierz symbol** i **Włącz lub wyłącz tryb edycji** działają z [klawiaturą SandFly](#keyboards).

Domyślnie te polecenia nie mają gestów. Możesz je ustawić na ekranie [**Polecenia**](#commands).

### Historia schowka {#typing-clipboard-history}

SandFly pamięta do 50 tekstów, od najnowszego. Zapisuje kopie wykonane w SandFly i kopie z innych aplikacji, gdy okno czytnika ma fokus. Aby zbierać też kopie w tle, użyj Shizuku lub roota i włącz **Dodawaj kopie z innych aplikacji** na górze historii. Kopia zapasowa ustawień pomija historię schowka.

Otwórz **Historia schowka** przez **Menu edycji** lub kategorię **Edytowanie tekstu**.

- Stuknij dwa razy na wpisie, żeby wkleić go do pola, z którego otworzyłeś historię. Jeśli wcześniej nie było pola, otworzą się czynności wpisu.
- Stuknij dwa razy i przytrzymaj, żeby wkleić, skopiować ponownie, dodać do schowka, edytować, eksportować lub usunąć wpis. Te same możliwości są w **Czynności**.
- Włącz zaznaczanie i zaznacz kilka wpisów, żeby je razem usunąć lub eksportować.
- **Eksportuj do pliku** zapisuje całą historię. Między tekstami są trzy łączniki w osobnym wierszu, oddzielone pustymi wierszami.
- **Wyczyść historię** usuwa wszystkie wpisy.

Jeśli chcesz wklejać bez otwierania okna historii, dodaj **Historia schowka** w **Sterowanie czytaniem** > **Czynności dla bieżącego elementu**. W polu tekstowym przesuwaj w górę lub w dół, aby wybrać wpis. Stuknij dwa razy, by wkleić ostatni odczytany wpis. Zobacz [ustawienia sterowania czytaniem](#commands).

### Kopiowanie treści ekranu {#typing-copy-from-screen}

- **Skopiuj ostatnią wypowiedź** kopiuje ostatni tekst. Domyślnie uruchomisz je trzykrotnym stuknięciem trzema palcami.
- **Wybierz ostatnią wypowiedź słowo po słowie** otwiera listę słów do zaznaczenia.
- **Kopiuj tekst z ekranu** pozwala [wybrać fragmenty tekstu](#reading-copy-text), na przykład akapity lub adresy.
- **Kopiowanie ciągłe** zbiera tekst kolejnych elementów. Zaczyna od miejsca, w którym jest fokus, i przewija bez mowy. Zatrzymasz je dotknięciem, klawiszem lub innym poleceniem. Po zakończeniu zaznacz potrzebne fragmenty i wybierz **Zapisz**. Limit to 2000 elementów lub dziesięć minut.

W oknach z listą możesz zaznaczyć zakres od jednego wpisu do drugiego przez czynności rozpoczęcia i zakończenia zaznaczania albo dwukrotne stuknięcie z przytrzymaniem na obu końcach.

### Szczegółowa edycja {#typing-granular-editing}

Otwórz **Szczegółowa edycja** w czynnościach pola albo użyj polecenia z kategorii **Edytowanie tekstu**. SandFly otworzy tekst w osobnym oknie, podzielony na fragmenty. Pola haseł są niedostępne.

- W opcji **Podziel według** wybierz **Znaki**, **Słowa**, **Zdania**, **Wiersze** lub **Akapity**. Początkowo są **Słowa**. SandFly zapamiętuje ostatni wybór.
- Stuknij dwa razy na fragmencie, żeby go zmienić. Puste pole usuwa fragment. Możesz też zaznaczyć go, wstawić tekst przed lub po nim, albo go usunąć. Stuknięcie z przytrzymaniem po drugim dotknięciu otwiera czynności fragmentu.
- Spacje, tabulatory, nowe i puste wiersze mają osobne pozycje.
- **Kopiuj** kopiuje zaznaczone fragmenty lub cały tekst, jeśli nic nie zaznaczysz.
- Menu **Więcej** pozwala cofać i ponawiać zmiany, znajdować tekst i kolejne wyniki, zastępować tekst, dopisać tekst na końcu, zaznaczać, usuwać zaznaczenie, wycinać i usuwać wybrane wpisy. Zamiana działa na wszystkich pasujących fragmentach bez względu na wielkość liter.
- **Zapisz** przenosi zmiany do pola i zamyka okno. Jeśli pole zniknęło, SandFly kopiuje tekst. Cofnięcie otwiera wybór zapisu albo odrzucenia zmian.

Jeżeli edytujesz tekst spoza pola, na przykład odebraną wiadomość, możesz go poprawić i skopiować. Nie można zapisać zmian w wiadomości źródłowej.

### Dyktowanie {#typing-dictation}

#### Podyktuj tekst do pola {#typing-dictate}

1. Ustaw fokus w polu tekstowym.
2. Uruchom **Dyktuj**. Po sygnale rozpocznie się nagrywanie. SandFly wycisza odczyt na czas nagrania.
3. Mów. Przerwy nie kończą domyślnie nagrywania.
4. Uruchom **Dyktuj** ponownie. Usłyszysz drugi sygnał, a SandFly wstawi wynik przy kursorze.

Nagrywanie kończy się po 15 minutach lub po [ustawionym czasie ciszy](#typing-dictation-settings). Domyślnie dwa stuknięcia dwoma palcami uruchamiają **Dyktuj, odtwórz lub wstrzymaj multimedia**. Gest zaczyna dyktowanie przy aktywnym polu i pokazanej lub podłączonej klawiaturze, poza rozmową. Jeśli nagrywanie już trwa, gest je kończy. W pozostałych sytuacjach steruje odtwarzaniem. Zobacz [domyślne gesty](#appendix-gestures).

#### Wybierz rozpoznawanie mowy {#typing-speech-recognition}

Otwórz **Pisanie** > **Dyktowanie** > **Rozpoznawanie mowy**. Przy wyborze automatycznym SandFly próbuje kolejno rozpoznawania Google na urządzeniu z pakietem języka telefonu, Rambler z kluczem Gemini API i obsługiwanym językiem, pobranego modelu Whisper i rozpoznawania Androida. Ekran podaje używaną metodę. Ten sam wybór zastępczy działa, gdy wskazana ręcznie metoda jest niedostępna.

- **Rozpoznawanie mowy Google na urządzeniu** używa pakietu języka telefonu. Od Androida 13 może dodawać interpunkcję i wielkie litery. Jeśli brakuje pakietu, użyj przycisku pobierania w ustawieniach.
- **Rambler (Gemini API)** używa modelu Gemini 3.5 Transcribe przez klucz z [ustawień usług AI](#descriptions). Usuwa słowa wypełniające, uwzględnia poprawki w mowie i formatuje interpunkcję, liczby i listy.
- **Whisper** działa na telefonie po [pobraniu modelu](#typing-whisper-models).
- **Rozpoznawanie mowy Androida** używa usługi wybranej w systemie.

Rozpoznawanie Google i Androida wznawia pracę po pauzach aż do końca dyktowania. Przy braku odpowiedzi Rambler SandFly może użyć pobranego modelu Whisper. Domyślna **Transkrypcja na żywo Rambler** przesyła wypowiedź w trakcie mówienia. Jeśli przesyłanie nie działa, SandFly wyśle całe nagranie po zatrzymaniu. Przełącznik pojawia się po podaniu klucza Gemini API.

Rambler przesyła nagranie do Google. Przy bezpłatnym kluczu Gemini API usługa może używać dźwięku i tekstu do doskonalenia produktów. Przy płatnym kluczu te dane nie są w tym celu używane.

#### Modele Whisper {#typing-whisper-models}

W **Pisanie** > **Dyktowanie** > **Modele Whisper** wybierz model. Nagranie jest rozpoznawane na urządzeniu. Modele wielojęzyczne to **Whisper Tiny** 32 MB, **Whisper Base** 60 MB, **Whisper Small** 190 MB i **Whisper Large v3 Turbo** 574 MB. Większe modele są zwykle dokładniejsze i wolniejsze. Są też modele **FUTO Tiny**, **FUTO Base** i **FUTO Small** oraz ich wersje tylko do angielskiego.

Wybierz model do pobrania. Przy danych komórkowych SandFly poprosi o potwierdzenie. Pierwszy pobrany model będzie używany od razu. Potem możesz wybrać inny z listy i użyć **Używaj** albo **Usuń**. Jeśli model zna język telefonu, używa go. W innym przypadku rozpoznaje język nagrania.

Przez **Plik modelu** możesz dodać własny plik whisper.cpp do 4 GB. Po dodaniu możesz go użyć, zastąpić przez **Wybierz inny plik** lub usunąć. Inny format nie zostanie przyjęty. Na telefonie z GPU Adreno i sterownikiem OpenCL możesz wybrać **CPU** albo **GPU** w **Procesor**. Jeśli GPU nie zadziała, model uruchomi się na procesorze.

#### Popraw dyktowanie za pomocą AI {#typing-dictation-correction}

Domyślne ustawienie **Pisanie** > **Dyktowanie** > **Poprawiaj dyktowanie za pomocą AI** przekazuje transkrypcję z Google, Whisper lub Androida do wybranej usługi przed wpisaniem. Dołącza do 300 znaków sprzed kursora. Usługa może poprawić błędnie usłyszane słowa, dodać interpunkcję, usunąć zawahania i uwzględnić poprawki w mowie. Wynik Rambler nie jest wysyłany ponownie. Tekst dyktowany w polu hasła nie trafia do usługi.

W opcji **Usługa AI do dyktowania** możesz wybrać domyślne **Jak w sekcji „Usługi AI”**, inną skonfigurowaną usługę lub **AI na urządzeniu** po pobraniu modelu. Użyj rozpoznawania i korekty lokalnej, żeby dyktowanie pozostało na telefonie. Zobacz [ustawienia usług AI](#descriptions). W polu **Instrukcje poprawiania** możesz określić język i styl. Puste lub niezmienione pole używa wbudowanych instrukcji.

Po 20 sekundach bez odpowiedzi, po 60 przy modelu lokalnym, albo gdy odpowiedź jest znacznie dłuższa od wypowiedzi, SandFly wstawi rozpoznany tekst bez korekty. Ponowne **Dyktuj** podczas oczekiwania od razu wstawi ten tekst.

#### Pozostałe ustawienia dyktowania {#typing-dictation-settings}

- **Automatyczna interpunkcja** włącza interpunkcję i wielkie litery w rozpoznawaniu Google. Jest domyślnie włączona i wymaga odpowiedniej usługi oraz Androida 13.
- **Blokuj obraźliwe słowa** maskuje takie słowa w rozpoznawaniu Google. Jest domyślnie włączone. Pojawia się z opcją interpunkcji.
- **Automatyczne zatrzymanie po ciszy** może kończyć nagranie po 1, 2, 3, 5, 10, 15 lub 30 sekundach albo minucie. Domyślnie jest wyłączone.
- **Odczytuj podyktowany tekst** czyta wpisany wynik z wyjątkiem pól haseł. Domyślnie jest wyłączone.
- **Utrzymuj modele rozpoznawania w pamięci** pozwala wybrać **Zwolnij po użyciu**, 5, 15 lub 30 minut albo **Do zatrzymania SandFly**. Pozostawiony model uruchamia się szybciej i zajmuje pamięć. Domyślnie SandFly zwalnia model po użyciu.
- **Serwer pobierania modeli** pozwala wybrać domyślne **Hugging Face** lub **hf-mirror.com**.

### Klawiatura pisma odręcznego {#typing-handwriting}

Pismo odręczne SandFly to klawiatura dla telefonów i tabletów z Androidem 11 lub nowszym. Piszesz palcem na całym ekranie. Gdy klawiatura jest otwarta, dotknięcia trafiają bezpośrednio do niej.

#### Włącz klawiaturę pisma odręcznego {#typing-handwriting-turn-on}

1. Otwórz **Pisanie** > **Klawiatura pisma ręcznego**.
2. W ustawieniach Androida włącz **Pismo odręczne SandFly**.
3. W polu tekstowym wybierz tę klawiaturę przez **Przełącz klawiaturę** lub przełącznik Androida.

Przy pierwszym otwarciu samouczek przeprowadza przez literę, słowo i gesty, bez wpisywania do pola. Możesz go zakończyć przesunięciem trzech palców w dół i później otworzyć stuknięciem trzema palcami. Rozpoznawanie odbywa się na urządzeniu w języku telefonu. Za pierwszym razem SandFly poprosi o pobranie modelu Google ML Kit, który ma około 20 MB.

#### Pisz palcem {#typing-handwriting-write}

Napisz literę, kilka liter albo słowo w dowolnym miejscu. Ruchy rozpoczęte przed upływem czasu oczekiwania są odczytywane razem. Kropka nad literą również liczy się jako ruch.

W **Pisanie** > **Oczekiwanie po napisaniu** ustaw od 0,2 do 1 sekundy. Domyślnie jest to 0,6 sekundy. Przy krótszym czasie SandFly szybciej wpisuje wynik. Musisz jednak rozpocząć każdą kreskę wieloczęściowej litery w tym czasie. Dotyczy to też poleceń [pisania litery lub nazwy aplikacji](#commands).

| Gest | Działanie |
| --- | --- |
| Dwa palce w prawo | Wstaw spację. |
| Dwa palce w lewo | Usuń znak przed kursorem. |
| Dwa palce w dół | Wstaw nowy wiersz lub wykonaj działanie pola, na przykład Wyślij. |
| Dwa palce w górę | Wybierz następną wersję odczytu tej samej długości. |
| Stuknięcie dwoma palcami | Przeczytaj tekst przed kursorem. |
| Trzy palce w dół | Schowaj klawiaturę. |
| Trzy palce w prawo lub w lewo | Wybierz następną lub poprzednią klawiaturę od Androida 9. |
| Stuknięcie trzema palcami | Otwórz samouczek. |

W **Pisanie** > **Echo klawiatury** > **Klawiatura pisma ręcznego** wybierz odczyt fragmentu, wyrazu zakończonego spacją lub nowym wierszem, obu albo żadnego. Usunięty znak i następna wersja odczytu są odczytywane niezależnie od echa. W polach SandFly tekst przed kursorem jest pomijany, a przy usuwaniu usłyszysz tylko informację o usunięciu. Gdy klawiatura jest otwarta, działają polecenia, które nie otwierają nowego okna.

### Klawiatura fizyczna {#typing-physical-keyboard}

Po podłączeniu klawiatury znajdziesz w **Pisanie** sekcję **Klawiatura fizyczna**. Obejmuje ustawienia przycisków numerycznych, wysyłania klawiszem Enter, Klawiatury SandFly, przytrzymań, podwójnych naciśnięć, kropki po dwóch spacjach i układu Androida. Szczegóły są w [rozdziale o klawiaturach](#keyboards).
