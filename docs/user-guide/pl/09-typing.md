## Pisanie i edycja tekstu {#typing}

SandFly odczytuje klawisze ekranowe pod palcem oraz tekst, który wpisujesz. Możesz też dyktować, pisać odręcznie na ekranie lub wprowadzać tekst brajlem. Ten rozdział opisuje ekran **Pisanie**, klawiaturę ekranową, przesuwanie kursora, zaznaczanie i kopiowanie, historię schowka, szczegółową edycję, dyktowanie i klawiaturę pisma ręcznego.

Klawiaturę brajlowską opisuje [](#braille), a klawiatury fizyczne i metodę wprowadzania tekstu Klawiatura SandFly opisuje [](#keyboards).

### Ustawienia pisania {#typing-settings}

W ustawieniach SandFly otwórz ekran **Pisanie**. Znajdziesz tam poniższe opcje.

- **Echo klawiatury** określa odczyt podczas pisania na poszczególnych klawiaturach. Zobacz [](#typing-keyboard-echo).
- **Dwukrotne kliknięcie przenosi kursor na początek lub koniec** zmienia działanie stuknięcia w edytowanym polu. Zobacz [](#typing-double-tap-cursor).
- W grupie **Klawiatura ekranowa** znajdziesz **Metoda pisania**, **Opóźnienie podczas naciskania i przytrzymywania w czasie pisania**, **Przytrzymuj klawisze po upływie opóźnienia naciśnięcia i przytrzymania**, **Opóźnienie zaznaczenia podczas pisania** i **Szybciej odczytuj przyciski klawiatury ekranowej**. Zobacz [](#typing-on-screen-keyboard).
- **Klawiatura fizyczna** pojawia się po podłączeniu klawiatury. Zobacz [](#typing-physical-keyboard).
- W grupie **Inne sposoby pisania** znajdziesz **Dyktowanie**, **Klawiatura pisma ręcznego** oraz **Oczekiwanie po napisaniu**. Zobacz [](#typing-dictation) i [](#typing-handwriting).

### Pisz na klawiaturze ekranowej {#typing-on-screen-keyboard}

Dotknij klawiatury i przesuwaj palec po klawiszach. SandFly odczytuje każdy klawisz pod palcem. To, w jaki sposób go wpiszesz, zależy od metody pisania.

#### Wybierz metodę pisania {#typing-method}

1. Otwórz **Pisanie** > **Metoda pisania**.
2. Wybierz jedną z poniższych metod.
   - **Przytrzymaj palec, aby wybrać przycisk klawiatury, a następnie kliknij dwukrotnie**. Unieś palec, a potem stuknij dwukrotnie w dowolnym miejscu, żeby wpisać ostatni odczytany klawisz.
   - **Przytrzymaj palec, aby wybrać przycisk z literą, a następnie podnieś palec. Pozostałe przyciski wymagają dwukrotnego kliknięcia.** Unieś palec, aby wpisać literę. Pozostałe klawisze, np. Usuń lub Enter, aktywujesz dwukrotnym stuknięciem. To ustawienie domyślne.
   - **Przytrzymaj palec, aby wybrać przycisk klawiatury, a następnie podnieś palec**. Uniesienie palca wpisuje dowolny klawisz. Ta metoda ma najmniejsze opóźnienie.
   - **Przytrzymaj palec, aby wybrać przycisk klawiatury, a następnie podnieś palec. Przyciski Enter, Gotowe lub Wyślij wymagają dwukrotnego kliknięcia.** Uniesienie palca wpisuje każdy klawisz poza klawiszem zatwierdzającym, takim jak Enter, Gotowe, Wyślij, Szukaj lub Przejdź. Ten klawisz wymaga dwóch stuknięć. SandFly rozpoznaje go na Gboardzie. W innych klawiaturach uniesienie palca wpisuje każdy klawisz.
   - **Stuknij dowolny klawisz, aby go wpisać, lub przytrzymaj palec, aby wybrać dowolny klawisz, a następnie podnieś**. Szybkie stuknięcie od razu wpisuje znak, a dwa szybkie stuknięcia wpisują go dwa razy. Przytrzymanie palca pozwala czytać klawisze pod palcem. Uniesienie wpisuje bieżący klawisz. Metoda jest dostępna od Androida 13.

W każdej metodzie możesz też przytrzymać klawisz jednym palcem i stuknąć ekran drugim, aby go wpisać.

Na Androidzie 13 i nowszym wybranie jednej z trzech ostatnich metod ustawia **Opóźnienie zaznaczenia podczas pisania** na 0,15 sekundy. Zobacz [](#typing-focus-delay).

#### Naciśnij i przytrzymaj klawisz {#typing-touch-and-hold}

**Pisanie** > **Opóźnienie podczas naciskania i przytrzymywania w czasie pisania** określa, po jakim czasie przytrzymanie palca na klawiszu wykonuje długie naciśnięcie. Możesz tak np. wyświetlić dodatkowe znaki klawisza. Do wyboru są 0,5, 1, 2, 3 i 5 sekund. Domyślnie ustawiono trzy sekundy. Opcja jest niedostępna przy metodzie **Przytrzymaj palec, aby wybrać przycisk klawiatury, a następnie kliknij dwukrotnie**.

Po włączeniu **Pisanie** > **Przytrzymuj klawisze po upływie opóźnienia naciśnięcia i przytrzymania** SandFly naciska klawisz po upływie opóźnienia i przekazuje resztę dotknięcia klawiaturze. Działa to podobnie do dwukrotnego stuknięcia i przytrzymania. Klawiatura obsługuje wtedy długie naciśnięcie po swojemu, np. klawisz kasowania powtarza się w trakcie trzymania. Opcja dotyczy metod pisania z unoszeniem palca. Wyjątkiem są Gboard i Pixelboard, gdzie długie naciśnięcie otwiera okno dodatkowych znaków. Domyślnie opcja jest wyłączona.

#### Dodatkowe znaki w Gboardzie {#typing-gboard-alternatives}

Gdy przytrzymasz klawisz w Gboardzie, po ustawionym czasie SandFly wykona długie naciśnięcie, a Gboard otworzy okno dodatkowych znaków. Po otwarciu SandFly od razu czyta wybrany znak, np. „przecinek”. Przesuń palec, aby usłyszeć inne znaki. Unieś go, aby wpisać bieżący.

Gboard 18.8 i nowszy otrzymuje dotknięcia przez eksplorację dotykową Androida, tak jak inne klawiatury. Przy starszych wersjach Gboarda i opartym na nim Pixelboardzie na Androidzie 11 lub nowszym SandFly samodzielnie obsługuje dotykanie klawiatury. Gdy otworzysz okno dodatkowych znaków, położenie palca wybiera jeden z nich. Gdy palec jest poza oknem, wybierany jest najbliższy znak. Uniesienie palca wpisuje znak i zamyka okno. Jeśli przesuniesz palec poza klawiaturę bez otwartego okna znaków, włączy się zwykłe czytanie dotykiem. Uniesienie palca poza klawiaturą wtedy niczego nie wpisze. Zasada ta nie dotyczy metody **Przytrzymaj palec, aby wybrać przycisk klawiatury, a następnie kliknij dwukrotnie**.

#### Wibracje klawiszy Gboarda {#typing-gboard-vibration}

Gboard 18.8 i nowszy odtwarza sygnały zgodnie z jego własnymi ustawieniami, np. **Wibracja przy naciśnięciu klawisza**.

Starsze wersje Gboarda i Pixelboarda odtwarzają sygnały tylko wtedy, gdy klawisz otrzymuje zdarzenia przesuwania palca nad klawiszami. Nie powstają one, gdy SandFly samodzielnie eksploruje klawiaturę. Jeśli skonfigurujesz Shizuku lub dostęp root, SandFly przekaże te zdarzenia, a klawiatura odtworzy własne sygnały. Bez tych uprawnień starsze wersje nie odtwarzają sygnałów w trakcie eksploracji przez SandFly. Działanie jest automatyczne i nie ma osobnego przełącznika. Zobacz [](#updates).

#### Zmień opóźnienie zaznaczenia podczas pisania {#typing-focus-delay}

W menu **Pisanie** > **Opóźnienie zaznaczenia podczas pisania** wybierzesz, po jakim czasie od dotknięcia klawiatury SandFly odczytuje klawisz. Możesz ustawić 0,15, 0,2, 0,25 lub 0,3 sekundy. Domyślnie ustawiono 0,3 sekundy. Krótszy czas przyspiesza odczyt. Opcja działa od Androida 13.

Opóźnienie możesz zmieniać także w trakcie pisania. Dodaj **Opóźnienie zaznaczenia podczas pisania** z grupy **Dotyk** na ekranie **Sterowanie czytaniem**. Zobacz [](#commands).

#### Przyspiesz odczyt klawiszy {#typing-faster-keys}

Opcja **Pisanie** > **Szybciej odczytuj przyciski klawiatury ekranowej** przygotowuje mowę klawiszy z wyprzedzeniem. Dzięki temu krócej czekasz na ich odczyt. Domyślnie opcja jest wyłączona. Jest dostępna tylko z syntezatorem Google.

#### Przesunięcia palcem a klawiatura {#typing-swipes-keyboard}

Przesunięcia z aplikacji, paska stanu lub paska nawigacji nie przenoszą fokusu na klawiaturę. Przy większości metod, jeśli fokus jest na klawiszu, przesunięcie palcem kontynuuje nawigację w aplikacji od pola, w którym piszesz. Wyjątkiem jest metoda **Przytrzymaj palec, aby wybrać przycisk klawiatury, a następnie kliknij dwukrotnie**, przy której przesunięcie przechodzi po klawiszach. Przesunięciem z klawiatury nadal możesz z niej wyjść.

### Echo klawiatury {#typing-keyboard-echo}

Echo klawiatury określa odczyt podczas pisania. Dla każdego rodzaju klawiatury ustawisz je osobno.

1. Otwórz **Pisanie** > **Echo klawiatury**.
2. Wybierz **Klawiatura ekranowa**, **Klawiatura fizyczna**, **Klawiatura brajlowska**, **Monitor brajlowski** lub **Klawiatura pisma ręcznego**.
3. Wybierz **Brak**, **Znaki**, **Słowa** lub **Znaki i słowa**. We wszystkich rodzajach opcja domyślna to **Znaki i słowa**.

Echo słów odczytuje całe słowo zapisane w polu, cofając się do ostatniej spacji. Słowo z apostrofem lub łącznikiem, np. „don't”, jest traktowane jako całość. Tak samo jest z adresem strony lub e-mail. Jeśli klawiatura dodaje razem słowo, znak przestankowy i spację, jak klawiatura brajlowska przy skrótach brajlowskich, SandFly odczytuje słowo.

Każde ustawienie echa jest też pozycją sterowania czytaniem, pozycją menu SandFly i poleceniem z grupy **Mowa**. To **Echo klawiatury ekranowej**, **Echo klawiatury fizycznej**, **Echo klawiatury brajlowskiej**, **Echo monitora brajlowskiego** i **Echo klawiatury pisma ręcznego**. Domyślnie pozycje są ukryte w sterowaniu i menu. Aby je dodać, otwórz **Sterowanie czytaniem** lub **Menu SandFly** i wybierz je w grupie **Echo klawiatury**. Zobacz [](#commands).

Skrót **Włącz lub wyłącz echo klawiatury podczas pisania** wybiera następną wartość echa dla **Klawiatura fizyczna**. Zobacz [](#appendix-keyboard-shortcuts).

### Przesuwaj kursor {#typing-move-cursor}

Gdy fokus jest w polu tekstowym, pozycje **Znaki**, **Słowa**, **Wiersze**, **Zdania** i **Akapity** przesuwają kursor, a SandFly odczytuje mijany tekst. Domyślnymi gestami zrobisz to tak.

1. Przesuń palec w górę, a potem w dół, lub w dół, a potem w górę, aby wybrać pozycję sterowania czytaniem. Możesz też przesunąć trzy palce w górę lub w dół.
2. Przesuń palec w dół, żeby przesunąć kursor do przodu, albo w górę, żeby go cofnąć.

Polecenia **Przenieś kursor na początek** i **Przenieś kursor na koniec** znajdziesz na ekranie **Polecenia** w grupie **Edytowanie tekstu**. Przenoszą kursor na początek lub koniec tekstu. Są też dostępne w czynnościach pola tekstowego. Zobacz [](#typing-editing-actions).

Domyślne gesty opisuje [](#appendix-gestures), a sterowanie czytaniem [](#commands).

#### Przenoś kursor dwukrotnym stuknięciem {#typing-double-tap-cursor}

Po włączeniu **Pisanie** > **Dwukrotne kliknięcie przenosi kursor na początek lub koniec** dwukrotne stuknięcie przenosi kursor w bieżącym polu tekstowym.

- Gdy kursor jest poza początkiem, dwa stuknięcia przenoszą go na początek tekstu. SandFly ogłasza początek pola.
- Gdy kursor jest na początku, dwa stuknięcia przenoszą go na koniec. SandFly ogłasza koniec pola.

Na innych elementach oraz w polu bez fokusu wprowadzania i widocznej klawiatury dwukrotne stuknięcie aktywuje element jak zwykle. Opcja jest domyślnie wyłączona. Działa od Androida 13.

### Zaznaczaj, kopiuj, wycinaj i wklejaj {#typing-select-copy-paste}

#### Zaznacz tekst {#typing-select-text}

1. Przenieś kursor do miejsca, od którego chcesz zacząć zaznaczanie.
2. Użyj **Włącz lub wyłącz tryb zaznaczania**. SandFly potwierdzi włączenie trybu.
3. Przesuwaj kursor za pomocą pozycji sterowania czytaniem, np. **Znaki** lub **Słowa**. Zaznaczysz tekst, przez który przechodzi kursor.
4. Ponownie użyj **Włącz lub wyłącz tryb zaznaczania**, aby zakończyć zaznaczanie.

**Zaznacz wszystko** zaznacza cały tekst pola. **Odznacz cały tekst** usuwa zaznaczenie.

Tryb zaznaczania działa również poza polami tekstowymi, np. w wiadomościach lub na stronach, także przy przechodzeniu między elementami. SandFly przechowuje wtedy zaznaczenie samodzielnie. Zaznacza tylko tekst mijany przez kursor. Po przejściu do innego elementu zaczyna osobne zaznaczenie. **Kopiuj** kopiuje wszystkie zaznaczone fragmenty, po jednym na wiersz.

#### Kopiuj, wycinaj i wklejaj {#typing-copy-cut-paste}

- **Kopiuj** kopiuje zaznaczony tekst lub tekst elementu, na którym jest fokus.
- **Dodaj do schowka** dopisuje zaznaczony tekst lub tekst elementu na końcu bieżącej zawartości schowka, w nowym wierszu.
- **Wytnij** wycina zaznaczony tekst z pola.
- **Wklej** wstawia zawartość schowka w polu tekstowym w miejscu kursora.

Domyślnie **Wytnij** to dwukrotne stuknięcie trzema palcami z przytrzymaniem, a **Wklej** to trzykrotne stuknięcie trzema palcami z przytrzymaniem. Każdemu z tych poleceń możesz przypisać gest. Zobacz [](#appendix-gestures) i [](#commands).

#### Czynności edycji i menu edycji {#typing-editing-actions}

Gdy fokus jest na polu tekstowym, pozycja **Czynności** w sterowaniu czytaniem i pozycja **Czynności** w menu SandFly zawierają **Przenieś kursor na początek**, **Przenieś kursor na koniec**, **Włącz lub wyłącz tryb zaznaczania**, **Wytnij**, **Kopiuj**, **Wklej**, **Zaznacz wszystko** i **Szczegółowa edycja**. Ostatnia opcja jest niedostępna w polach haseł.

Polecenie **Menu edycji** otwiera menu z pozycjami **Historia schowka**, **Kopiuj**, **Dodaj do schowka**, **Wytnij**, **Wklej**, **Wyczyść tekst**, **Zaznacz wszystko** i **Włącz lub wyłącz tryb zaznaczania**.

#### Inne polecenia edycji tekstu {#typing-editing-commands}

W grupie **Edytowanie tekstu** na ekranie **Polecenia** znajdziesz też poniższe polecenia.

- **Prześlij tekst** naciska klawisz wysyłający lub zatwierdzający tekst, np. Wyślij, Szukaj lub Gotowe.
- **Wyczyść tekst** usuwa całą zawartość pola.
- **Liczba znaków** podaje długość tekstu, np. „12 znaków, 3 słowa”. W polach haseł zlicza tylko znaki.
- **Czytaj pole tekstowe** odczytuje cały tekst pola. W pustym polu ogłasza, że jest puste, i czyta podpowiedź.
- **Podpowiedzi ortograficzne/gramatyczne** otwierają sugestie dla błędu w miejscu kursora. Pozycja **Korekta** w sterowaniu czytaniem przechodzi między błędami.
- Działanie **Dyktuj** opisuje [](#typing-dictate).
- **Przełącz klawiaturę** przełącza na następną klawiaturę. Polecenie jest dostępne od Androida 11.
- **Wyszukaj emotikony**, **Wybierz symbol** i **Włącz lub wyłącz tryb edycji** to polecenia klawiatury SandFly. Zobacz [](#keyboards).

Domyślnie polecenia z tej listy nie mają gestów. O przypisywaniu przeczytasz w [](#commands).

### Historia schowka {#typing-clipboard-history}

SandFly zachowuje do 50 skopiowanych tekstów, od najnowszego. Obejmuje to tekst skopiowany przez SandFly oraz tekst kopiowany w innych aplikacjach, gdy fokus znajduje się w oknie SandFly. Android ogranicza dostęp do schowka w tle. Dzięki Shizuku lub dostępowi root przełącznik **Dodawaj kopie z innych aplikacji** u góry historii może zapisywać wszystkie teksty kopiowane przez inne aplikacje. Historia jest przechowywana osobno od ustawień, poza kopią zapasową.

Aby otworzyć historię nad bieżącym ekranem, wybierz **Historia schowka** w grupie poleceń **Edytowanie tekstu** lub w menu **Menu edycji**.

- Stuknij tekst dwukrotnie, aby wkleić go do pola, z którego otwarto historię. Jeśli nie otwarto jej z pola, dwa stuknięcia otworzą czynności tekstu.
- Stuknij tekst dwukrotnie i przytrzymaj, aby otworzyć czynności. Możesz wybrać **Wklej**, ponownie **Kopiuj**, **Dodaj do schowka**, **Edytuj**, **Eksportuj do pliku** lub **Usuń**. Te same opcje są w menu **Czynności** danego wpisu.
- **Zaznacz** pokazuje wpisy jako pola wyboru. Stuknij wybrane wpisy dwa razy, a potem użyj **Usuń** lub **Eksportuj do pliku**.
- **Eksportuj do pliku** zapisuje całą historię w pliku tekstowym. Poszczególne teksty oddziela wiersz z trzema łącznikami i pustym wierszem z każdej strony.
- **Wyczyść historię** usuwa wszystkie wpisy.

Aby wklejać bez otwierania historii, dodaj **Historia schowka** z grupy **Czynności dla bieżącego elementu** na ekranie **Sterowanie czytaniem**. W polu tekstowym przesuwaj palec w górę lub w dół po wpisach historii. Stuknij dwukrotnie, aby wkleić ostatnio odczytany wpis. Domyślnie pozycji nie ma na liście. Zobacz [](#commands).

### Kopiuj tekst z ekranu {#typing-copy-from-screen}

W grupie **Edytowanie tekstu** na ekranie **Polecenia** są poniższe polecenia.

- **Skopiuj ostatnią wypowiedź** kopiuje ostatni odczytany tekst. Domyślnie uruchamia się po trzykrotnym stuknięciu trzema palcami.
- **Wybierz ostatnią wypowiedź słowo po słowie** otwiera listę słów ostatniej wypowiedzi. Zaznacz słowa, które chcesz skopiować.
- **Kopiuj tekst z ekranu** otwiera tekst ekranu podzielony na adresy i numery, znaki, słowa lub akapity. Zaznacz fragmenty do skopiowania. Zobacz [](#reading-copy-text).
- **Kopiowanie ciągłe** przenosi fokus z bieżącego elementu przez kolejne, bez mowy. Przewija po drodze listy i strony oraz zbiera tekst z każdego elementu. Dotknij ekranu, naciśnij klawisz lub użyj innego polecenia, żeby zakończyć. Na końcu przy każdym tekście jest pole wyboru. Zaznacz teksty do skopiowania i wybierz **Zapisz**. Kopiowanie kończy się najpóźniej po 2000 elementów lub dziesięciu minutach.

Na tych listach użyj **Rozpocznij zaznaczanie** i **Zakończ zaznaczanie**, aby zaznaczyć wszystkie pozycje między dwiema wybranymi. Tak samo działa dwukrotne stuknięcie pozycji z przytrzymaniem.

### Szczegółowa edycja {#typing-granular-editing}

Szczegółowa edycja pokazuje tekst pola lub innego elementu, na którym jest fokus, na osobnym ekranie podzielonym na fragmenty. Wybierz **Szczegółowa edycja** w czynnościach pola albo użyj polecenia o tej nazwie na ekranie **Polecenia**, w grupie **Edytowanie tekstu**. Pola haseł nie otwierają się w tym trybie.

- W opcji **Podziel według** wybierz **Znaki**, **Słowa**, **Zdania**, **Wiersze** lub **Akapity**. Domyślnie wybrane są **Słowa**. SandFly zapamiętuje ostatni wybór.
- Stuknij fragment dwa razy, aby go zmienić. Pozostawienie pustego pola usuwa fragment.
- Przy każdym fragmencie są działania **Zaznacz**, **Wstaw przed**, **Wstaw po** i **Usuń**. Stuknij go dwukrotnie i przytrzymaj, aby otworzyć je w oknie. **Rozpocznij zaznaczanie** i **Zakończ zaznaczanie** pozwalają zaznaczyć wszystkie pośrednie fragmenty.
- Spacje, tabulatory i podziały wierszy między fragmentami są osobnymi pozycjami, np. „2 spacje” lub „Nowy wiersz”. Pusty wiersz jest odczytywany jako „Pusty wiersz”.
- **Kopiuj** kopiuje zaznaczone fragmenty. Jeśli nic nie zaznaczysz, skopiuje cały tekst.
- W menu **Więcej** są **Cofnij**, **Ponów**, **Znajdź**, **Znajdź następny**, **Znajdź poprzedni**, **Zastąp**, **Dodaj tekst na końcu**, **Zaznacz wszystko**, **Odznacz cały tekst**, **Wytnij zaznaczony tekst** i **Usuń wybrane**. **Zastąp** zmienia wszystkie dopasowania, niezależnie od wielkości liter.
- **Zapisz** wstawia tekst z powrotem do pola i zamyka ekran. Jeśli pola już nie ma, tekst trafia do schowka.
- Użycie Wstecz otwiera okno z wyborem zapisu lub odrzucenia zmian.

Tekst spoza pola, np. otrzymaną wiadomość, możesz edytować i skopiować. Zmian nie da się zapisać w oryginale.

### Dyktowanie {#typing-dictation}

#### Dyktuj tekst do pola {#typing-dictate}

Polecenie **Dyktuj** wpisuje to, co powiesz, w miejscu kursora w polu tekstowym, na którym jest fokus.

1. Przenieś fokus do pola tekstowego.
2. Użyj **Dyktuj**. Usłyszysz sygnał i zacznie się nagrywanie. Mowa SandFly jest wtedy wyciszona.
3. Mów. Przerwy w mówieniu nie kończą nagrania.
4. Użyj ponownie **Dyktuj**. Usłyszysz drugi sygnał, a SandFly wpisze tekst.

Nagrywanie zatrzymuje się po 15 minutach lub po ustawionym przez Ciebie czasie ciszy. Zobacz [](#typing-dictation-settings).

Domyślnie dwukrotne stuknięcie dwoma palcami uruchamia **Dyktuj, odtwórz lub wstrzymaj multimedia**. Gdy pole tekstowe ma fokus wprowadzania, klawiatura jest widoczna lub podłączona i nie trwa rozmowa, polecenie uruchamia **Dyktuj**. Jeśli dyktowanie jest w toku, zatrzymuje je. W pozostałych sytuacjach odtwarza lub wstrzymuje multimedia. Domyślne gesty znajdziesz w [](#appendix-gestures).

Na telefonach z klawiszem wyciszenia, takich jak Zinwa Q25, przytrzymaj klawisz wyciszenia w polu tekstowym, aby dyktować. Nagrywanie zaczyna się po naciśnięciu klawisza i kończy po jego zwolnieniu. Odpowiada za to opcja **Klawisze głośności** > **Przytrzymaj klawisz wyciszenia, aby dyktować w polach tekstowych**, domyślnie włączona. Zobacz [](#buttons-mute-key).

#### Wybierz sposób rozpoznawania mowy {#typing-speech-recognition}

Otwórz **Pisanie** > **Dyktowanie** > **Rozpoznawanie mowy** i wybierz jedną z poniższych opcji.

- **Automatycznie** to ustawienie domyślne. SandFly wybiera rozpoznawanie Google na urządzeniu, jeśli jest zainstalowane i obsługuje język telefonu. W przeciwnym razie próbuje Ramblera, jeśli masz klucz Gemini API i Rambler obsługuje język. Następnie sprawdza, czy pobrano model Whisper, a na końcu korzysta z rozpoznawania mowy Androida. Na ekranie widać aktualny wybór, np. „Automatycznie: Rambler”.
- **Rozpoznawanie mowy Google na urządzeniu** używa aplikacji Rozpoznawanie i synteza mowy Google oraz pobranych pakietów języka telefonu. Na Androidzie 13 i nowszym po włączeniu **Automatyczna interpunkcja** dodaje znaki przestankowe i wielkie litery. Gdy brakuje pakietu, pozycja **Pobierz** z nazwą języka otwiera ekran pobrania w aplikacji Google.
- **Rambler (Gemini API)** korzysta z modelu Gemini 3.5 Transcribe przez klucz Gemini API z ustawień usług AI. Usuwa zbędne wtrącenia, uwzględnia poprawki w trakcie wypowiedzi oraz formatuje interpunkcję, liczby i listy. Konfigurację klucza Gemini API opisuje [](#descriptions).
- **Whisper** rozpoznaje mowę na telefonie z użyciem pobranego modelu. Zobacz [](#typing-whisper-models).
- **Rozpoznawanie mowy Androida** korzysta z usługi wybranej w ustawieniach Androida.

Gdy wybrana metoda jest niedostępna, SandFly stosuje kolejność opcji **Automatycznie**. Rozpoznawanie Google na urządzeniu oraz rozpoznawanie Androida zaczynają kolejny fragment po każdej pauzie, dopóki nie zakończysz dyktowania. Jeśli Rambler nie odpowie, SandFly rozpozna nagranie w Whisperze, o ile masz pobrany model.

Przy włączonej opcji **Transkrypcja na żywo Rambler** SandFly przesyła nagranie w trakcie mówienia i odbiera tekst kolejnych fragmentów po pauzach. Dzięki temu tekst szybciej pojawia się po zatrzymaniu. Jeśli przesyłanie się nie uda, SandFly wyśle całe nagranie po zakończeniu. Domyślnie opcja jest włączona i pojawia się po dodaniu klucza Gemini API.

Uwaga: Rambler wysyła nagranie do Google. Przy kluczu Gemini API w darmowym planie Google może użyć dźwięku i tekstu do ulepszania swoich produktów. Przy kluczu w płatnym planie dane nie są używane w tym celu.

#### Modele Whisper {#typing-whisper-models}

Whisper rozpoznaje mowę na telefonie, więc nagranie zostaje na urządzeniu. W menu **Pisanie** > **Dyktowanie** > **Modele Whisper** znajdziesz poniższe modele.

- **Whisper Tiny** (32 MB), **Whisper Base** (60 MB), **Whisper Small** (190 MB) i **Whisper Large v3 Turbo** (574 MB) obsługują wiele języków. Większy model jest dokładniejszy i wolniejszy.
- **FUTO Tiny**, **FUTO Base** i **FUTO Small** oraz ich wersje tylko dla języka angielskiego pochodzą od FUTO.

Aby skorzystać z modelu, wykonaj poniższe czynności.

1. Wybierz model. SandFly go pobierze. Przy danych komórkowych najpierw pojawi się okno potwierdzenia.
2. Aby przełączyć się na inny pobrany model, wybierz go, a następnie **Używaj**. Aby usunąć model, wybierz go, a następnie **Usuń**.

SandFly używa pierwszego pobranego modelu. Whisper korzysta z języka telefonu, gdy jest znany. W przeciwnym razie rozpoznaje język nagrania.

Opcja **Plik modelu** umożliwia dodanie własnego modelu w formacie whisper.cpp, np. wytrenowanego dla konkretnego języka. Wybierz plik z telefonu, o rozmiarze do 4 GB. Po dodaniu możesz użyć modelu, podmienić go przez **Wybierz inny plik** lub go usunąć. Pliki w innym formacie są odrzucane.

Ustawienie **Procesor** jest widoczne tylko na telefonach z układem graficznym Adreno i sterownikiem OpenCL. Wybierz **CPU** lub **GPU**. Jeśli Whisper nie uruchomi się na GPU, użyje CPU.

#### Poprawiaj dyktowanie za pomocą AI {#typing-dictation-correction}

Po włączeniu **Pisanie** > **Dyktowanie** > **Poprawiaj dyktowanie za pomocą AI** SandFly przesyła do usługi AI tekst rozpoznany przez Google na urządzeniu, Whisper lub rozpoznawanie Androida. Dołącza do 300 znaków sprzed kursora. Usługa usuwa zbędne wtrącenia, zająknięcia i urwane początki, uwzględnia poprawki w mowie, dodaje interpunkcję i wielkie litery, poprawia błędnie rozpoznane słowa i formatuje liczby, daty i listy. Jeśli powiesz „dwa, nie, trzy”, wynikiem jest „trzy”. Domyślnie opcja jest włączona. Tekst z Ramblera nie jest wysyłany ponownie, a tekst dyktowany do pola hasła nie jest wysyłany.

- W opcji **Usługa AI do dyktowania** wybierz **Jak w sekcji „Usługi AI”** (domyślnie), dowolną skonfigurowaną usługę lub **AI na urządzeniu**, jeśli model jest pobrany. W ostatnim przypadku całe dyktowanie pozostaje na telefonie. Zobacz [](#descriptions).
- W polu **Instrukcje poprawiania** możesz podać język, styl lub inne wskazówki dla modelu. Początkowo pole zawiera wbudowaną instrukcję. Zapis bez zmian lub z pustym polem zachowuje wbudowaną instrukcję.

Gdy odpowiedź nie nadejdzie w ciągu 20 sekund (60 sekund dla AI na urządzeniu) albo wynik jest znacznie dłuższy od wypowiedzi, SandFly wpisze tekst w postaci rozpoznanej przez mechanizm rozpoznawania mowy. Możesz to zrobić od razu, używając **Dyktuj** przed nadejściem odpowiedzi.

#### Pozostałe ustawienia dyktowania {#typing-dictation-settings}

W menu **Pisanie** > **Dyktowanie** znajdziesz poniższe opcje.

- **Automatyczna interpunkcja** pozwala mechanizmowi Google wstawiać znaki przestankowe i wielkie litery. Domyślnie opcja jest włączona. Jest widoczna na Androidzie 13 lub nowszym z zainstalowanym rozpoznawaniem mowy Google.
- **Blokuj obraźliwe słowa** pozwala mechanizmowi Google ukrywać obraźliwe słowa. Domyślnie opcja jest włączona. Jest widoczna razem z **Automatyczna interpunkcja**.
- **Automatyczne zatrzymanie po ciszy** kończy nagrywanie po 1, 2, 3, 5, 10, 15 lub 30 sekundach albo po minucie bez mowy. Domyślnie ustawione jest **Wyłączone**.
- **Odczytuj podyktowany tekst** czyta wpisany tekst poza polami haseł. Domyślnie opcja jest wyłączona.
- **Utrzymuj modele rozpoznawania w pamięci** ustala, jak długo pozostają w pamięci modele Whisper i AI na urządzeniu. Możesz wybrać **Zwolnij po użyciu** (domyślnie), 5, 15 lub 30 minut albo **Do zatrzymania SandFly**. Wczytany model zaczyna pracę szybciej, ale zajmuje pamięć.
- **Serwer pobierania modeli** pozwala wybrać **Hugging Face** (domyślnie) lub **hf-mirror.com**.

### Klawiatura pisma ręcznego {#typing-handwriting}

Pismo odręczne SandFly to klawiatura, która wpisuje to, co napiszesz palcem na ekranie. Jest dostępna na telefonach i tabletach od Androida 11. Zajmuje cały ekran. Przy włączonym SandFly dotknięcia klawiatury trafiają do niej bezpośrednio.

#### Włącz klawiaturę pisma ręcznego {#typing-handwriting-turn-on}

1. Otwórz **Pisanie** > **Klawiatura pisma ręcznego**. Otworzą się ustawienia klawiatur Androida.
2. Włącz **Pismo odręczne SandFly**.
3. W polu tekstowym wybierz **Pismo odręczne SandFly** za pomocą **Przełącz klawiaturę** lub systemowego przełącznika klawiatur.

Przy pierwszym otwarciu samouczek pokazuje pisanie litery, słowa i każdy gest. Przechodzi dalej, gdy wykonasz dany krok. W trakcie samouczka do pola nic nie jest wpisywane. Aby skończyć wcześniej, przesuń trzy palce w dół. Aby otworzyć samouczek ponownie, stuknij trzema palcami.

Pismo jest rozpoznawane na telefonie przez Google ML Kit w języku telefonu. Przy pierwszym użyciu SandFly pokazuje okno pobrania modelu rozpoznawania, około 20 MB, i informuje, gdy pobieranie się zakończy.

#### Pisz odręcznie {#typing-handwriting-write}

Napisz literę, kilka liter lub całe słowo w dowolnym miejscu ekranu. Ruchy wykonane w ustawionym odstępie są traktowane jako jeden zapis, który SandFly wpisuje i odczytuje. Kropka, np. nad i, również jest osobnym ruchem.

**Pisanie** > **Oczekiwanie po napisaniu** określa czas od 0,2 do 1 sekundy. Domyślna wartość to 0,6 sekundy. Krótszy czas szybciej wpisuje tekst, ale każdy kolejny ruch przy pisaniu litery, np. H, trzeba zacząć przed upływem ustawionego czasu. Ustawienie dotyczy również poleceń **Napisz literę opcji sterowania czytaniem** i **Napisz nazwę aplikacji, aby ją otworzyć**. Zobacz [](#commands).

| Gest | Działanie |
| --- | --- |
| Przesunięcie dwóch palców w prawo | Wpisuje spację. |
| Przesunięcie dwóch palców w lewo | Usuwa znak przed kursorem. |
| Przesunięcie dwóch palców w dół | Wstawia nowy wiersz albo wykonuje czynność pola, np. Wyślij, Szukaj lub Gotowe. |
| Przesunięcie dwóch palców w górę | Zastępuje ostatni zapis kolejnym wynikiem tej samej długości, np. H przez h. |
| Stuknięcie dwoma palcami | Odczytuje tekst przed kursorem. |
| Przesunięcie trzech palców w dół | Ukrywa klawiaturę. |
| Przesunięcie trzech palców w prawo lub w lewo | Przełącza na następną lub poprzednią klawiaturę (Android 9 i nowsze). |
| Stuknięcie trzema palcami | Otwiera samouczek. |

W menu **Pisanie** > **Echo klawiatury** > **Klawiatura pisma ręcznego** wybierzesz, co słyszysz w trakcie pisania. Może to być każdy zapis, całe słowo zakończone spacją lub nowym wierszem, oba albo cisza. Usunięte znaki i wyniki zastępujące ostatni zapis są odczytywane niezależnie od echa. W polach tekstowych SandFly tekst przed kursorem jest pomijany, a przy usunięciu usłyszysz tylko „Usunięto”.

Gdy klawiatura pisma ręcznego jest widoczna, działają tylko polecenia bez otwierania okna, np. nawigacja i sterowanie czytaniem.

### Klawiatury fizyczne {#typing-physical-keyboard}

Przy podłączonej klawiaturze fizycznej w menu **Pisanie** > **Klawiatura fizyczna** znajdziesz **Klawisze liter naciskają przyciski klawiatury numerycznej**, **Enter wysyła wiadomości**, **Klawiatura SandFly**, **Wpisywanie po przytrzymaniu klawisza**, **Znaki podwójnego naciśnięcia**, **Podwójna spacja wstawia kropkę**, **Autokorekta**, **Podpowiedzi słów**, **Wypowiadaj podpowiedź**, **Enter wpisuje wypowiedzianą podpowiedź**, **Język listy słów**, **Usuń zapamiętane słowa**, **Odczytuj znaki hasła** i **Ustawienia klawiatury fizycznej Androida**. Wszystkie opisuje [](#keyboards).
