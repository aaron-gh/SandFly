## Mowa {#speech}

Otwórz **Mowa**, żeby wybrać syntezator, ustawić głos, języki i wymowę albo zdecydować, co przerywa odczyt. Ustawienia są w grupach **Głos**, **Języki**, **Wymowa**, **Zatrzymywanie mowy** i **Inne aplikacje**.

Urządzenie odtwarzające, wyciszanie tła i dźwięk o niskim opóźnieniu ustawisz na ekranie [**Wyjście audio**](#sound). Informacje, które czytnik podaje przy elemencie, wybierzesz w [**Szczegółowość**](#verbosity).

### Wybierz syntezator mowy {#speech-engine}

1. Otwórz **Mowa** > **Syntezator mowy**.
2. Wybierz zainstalowany syntezator. Jeśli chcesz korzystać z ustawienia Androida, wybierz **Domyślny syntezator systemowy**.

Na ekranie **Mowa** pozycja **Systemowe ustawienia zamiany tekstu na mowę** otwiera ustawienia Androida. Możesz tam pobrać głosy albo zmienić domyślny syntezator systemu. Tej pozycji nie ma na telewizorach.

Możesz dodać **Syntezator mowy** na ekranie [**Sterowanie czytaniem**](#commands). Przesunięcie w górę lub w dół wybierze wtedy inny zainstalowany syntezator dla **Domyślny profil głosowy SandFly**. Ta opcja jest niedostępna, gdy korzystasz z własnego profilu głosowego.

Niektóre syntezatory odtwarzają dźwięk samodzielnie, na przykład gdy przekazują tekst innemu silnikowi. SandFly rozpoznaje je i korzysta ze zwykłego odtwarzania, z pominięciem toru **Dźwięk o niskim opóźnieniu**.

### Zmień szybkość, wysokość i głośność mowy {#speech-rate}

Gdy SandFly działa, w części **Głos** są trzy suwaki dla domyślnego profilu.

- **Szybkość mowy** ma zakres od 10 do 600 procent. Domyślnie jest to 100 procent.
- **Wysokość głosu** ma zakres od 20 do 200 procent. Domyślnie jest to 100 procent.
- **Głośność mowy** ma zakres od 25 do 100. Domyślnie jest to 100.

Jeżeli używasz innego [profilu głosowego](#speech-voice-profiles), jego nazwę znajdziesz przy **Profile głosowe**. Szybkość i ton bieżącego profilu możesz zmieniać z poleceń i sterowania czytaniem. Suwaki na ekranie **Mowa** dotyczą domyślnego profilu.

Wybierz **Szybkość mowy** w sterowaniu czytaniem i przesuń palec w górę lub w dół, żeby zmienić tempo. Ta opcja jest domyślnie na liście. Możesz też użyć **Zwiększ szybkość mowy**, **Zmniejsz szybkość mowy**, **Zwiększ ton głosu** albo **Zmniejsz ton głosu**. Do klawiatury fizycznej są też **Zwiększ głośność mowy**, **Zmniejsz głośność mowy** i **Zresetuj szybkość mowy i ton**. Zobacz [wykaz skrótów](#appendix-keyboard-shortcuts).

Możesz wybrać [kanał głośności mowy](#sound-speech-volume), na przykład dostępności lub multimediów. Zobacz też [regulację klawiszami głośności](#buttons-speech-volume).

### Powtórz ostatnią wypowiedź wolniej {#speech-repeat-slower}

W kategorii **Czytanie** wybierz **Powtórz wolniej ostatnią wypowiedź**. SandFly powtórzy tekst raz, tym samym głosem i wolniej. Następny element odczyta w zwykłym tempie.

W **Mowa** > **Powtarzaj wolniej o** możesz wybrać 5, 10, 15, 20, 25, 30, 40 lub 50 procent. Domyślnie jest to 15 procent. Polecenie możesz [dodać do menu albo przypisać do gestu](#commands).

### Profile głosowe {#speech-voice-profiles}

Profil pozwala jednym poleceniem zmienić syntezator, język, głos, głośność, tempo, wysokość głosu i efekty. W profilu możesz też ustawić przekazywanie długiego tekstu zdanie po zdaniu. Możesz na przykład przygotować osobny głos do długich tekstów.

Pozycja **Domyślny profil głosowy SandFly** zawsze jest na liście i korzysta z ekranu **Mowa**. Po włączeniu własnego profilu SandFly używa jego syntezatora i głosu. Czytnik nie przełącza wtedy automatycznie języka ani dialektu, chyba że dla danego języka ustawiono [osobny głos](#speech-voices-by-language). Możesz też wybrać profil do [odczytywania emoji](#speech-emoji).

#### Dodaj profil głosowy {#speech-add-voice-profile}

1. Otwórz **Mowa** > **Profile głosowe**.
2. Wybierz **Dodaj profil głosowy**.
3. Wpisz nazwę albo zostaw puste pole, żeby przyjąć proponowaną nazwę.
4. Wybierz **OK**.

Otworzą się ustawienia profilu. Początkowa głośność, szybkość, wysokość i efekty pochodzą z domyślnego profilu. Dotyczy to też **Przekazuj długi tekst do syntezatora zdanie po zdaniu**.

#### Ustawienia profilu głosowego {#speech-voice-profile-settings}

W **Mowa** > **Profile głosowe** wybierz profil, który chcesz zmienić.

- **Nazwa** pozwala zmienić nazwę.
- **Syntezator mowy** wybiera silnik. SandFly informuje przy pozycji, jeśli tego syntezatora już nie ma.
- **Język** i **Głos** pozwalają użyć wartości domyślnej lub wybrać język i dostępny dla niego głos.
- **Głośność mowy**, **Szybkość mowy** i **Wysokość głosu** dotyczą tego profilu.
- **Efekty głosu** otwierają [korektor i kompresję](#voice-effects). Są niedostępne na zegarkach.
- **Przekazuj długi tekst do syntezatora zdanie po zdaniu** działa tak samo jak na ekranie [wymowy](#speech-pronunciation), ale tylko dla tego profilu.
- **Utrzymuj uruchomiony syntezator mowy** skraca czas przejścia do profilu. Jego syntezator pozostaje uruchomiony przy włączonym ekranie, nawet gdy używasz innego. Każdy utrzymywany silnik ma własny proces. Domyślnie opcja jest wyłączona.
- **Przenieś w górę** i **Przenieś w dół** możesz wybrać na zegarku. Na telefonie zmień kolejność na liście profili.
- **Usuń profil głosowy** kasuje profil po potwierdzeniu.

#### Przełącz profil głosowy {#speech-switch-voice-profile}

- Otwórz **Mowa** > **Profile głosowe** > **Używany profil głosowy** i wybierz profil.
- W sterowaniu czytaniem dodaj **Profil głosowy**. Przesuwaj w górę lub w dół, żeby przechodzić przez profil domyślny i pozostałe profile w kolejności z ekranu **Profile głosowe**.
- W kategorii **Mowa** użyj **Wybierz profil głosowy**, **Następny profil głosowy** albo **Poprzedni profil głosowy**.
- Po dodaniu profilu w kategorii **Profile głosowe** pojawią się polecenia „Użyj profilu głosowego” z nazwą każdego profilu, w tym domyślnego. Możesz dodać je do gestu, skrótu lub menu.

Pozycję **Profil głosowy** możesz też [dodać do menu SandFly](#commands).

#### Zmień kolejność, nazwę lub usuń profil {#speech-manage-voice-profiles}

Na ekranie **Profile głosowe** przytrzymaj profil i przeciągnij go w górę lub w dół. Możesz też użyć **Przenieś w górę** albo **Przenieś w dół**. SandFly powie, przed którym lub za którym profilem znajdzie się przenoszona pozycja.

Przytrzymaj profil, żeby otworzyć menu, lub wybierz czynność **Zmień nazwę** albo **Usuń**. Opcja **Utrzymuj uruchomiony syntezator mowy poprzedniego profilu głosowego** zostawia przy włączonym ekranie uruchomiony poprzedni silnik. Dzięki temu możesz do niego wrócić bez czekania na start. Domyślnie jest wyłączona.

### Głos i tempo odczytu emoji {#speech-emoji}

W części **Głos** na ekranie **Mowa** wybierz **Głos emoji**. Możesz zostawić **Taki sam jak mowa** lub wybrać profil głosowy. Suwak **Szybkość mowy emoji** zmienia tempo od 50 do 300 procent zwykłej szybkości mowy. Domyślnie jest to 100 procent.

Ustawienia działają zarówno przy bezpośrednim odczycie emoji, jak i wtedy, gdy SandFly zastępuje je nazwami. To, czy emoji są odczytywane, ustawisz w [**Szczegółowość** > **Emoji**](#verbosity-emoji).

### Efekty głosu {#voice-effects}

Otwórz **Mowa** > **Efekty głosu**, żeby zmienić jedenastopasmowy korektor i kompresję domyślnego profilu. Możesz też ustawić te efekty w wybranym profilu. Nowy profil przejmuje ustawienia domyślnego. Efekty są niedostępne na zegarkach.

- **Używaj efektów głosu** włącza lub wyłącza efekty z zachowaniem zapisanych wartości. Domyślnie przełącznik jest włączony.
- W **Ustawienie wstępne** są gotowe ustawienia, między innymi **Płaski**, **Wyrazistość**, **Wzmocnienie basów**, **Redukcja basów**, **Wzmocnienie tonów wysokich**, **Redukcja tonów wysokich**, **Ciepły**, **Redukcja ostrości**, **Redukcja sybilantów**, **Mały głośnik** i **Głęboki bas**. Są tam również zapisane przez Ciebie ustawienia. Początkowo pasma są płaskie, a kompresja wyłączona. Ręczna zmiana ustawia wariant niestandardowy.
- Przez **Zapisz jako ustawienie wstępne** możesz zapisać do 50 własnych zestawów. Są dostępne we wszystkich profilach. Na ekranie **Zapisane ustawienia wstępne** zmienisz nazwę lub usuniesz zestaw. Samo usunięcie zestawu nie wyłącza używanych efektów.
- **Kompresja basów**, **Kompresja głosu** i **Kompresja wysokich tonów** dotyczą odpowiednio częstotliwości poniżej 200 Hz, od 200 Hz do 4 kHz i powyżej 4 kHz. Kompresja wzmacnia cichsze fragmenty danego zakresu. Możesz ją wyłączyć albo ustawić małą, średnią lub dużą kompresję. **Głęboki bas** używa średniej kompresji basów.
- **Korektor** ma suwaki dla 64, 125, 250 i 500 Hz oraz 1, 2, 3, 4, 6, 8 i 16 kHz. Każdy możesz ustawić od minus 12 do plus 12 dB.

Jeśli wzmocnisz pasmo lub włączysz kompresję, ogranicznik chroni mowę przed przesterowaniem.

Efekty działają w torze [dźwięku o niskim opóźnieniu](#sound). Jeśli go wyłączysz, na obsługiwanym urządzeniu z Androidem 9 lub nowszym SandFly użyje efektu systemowego. Na pozostałych urządzeniach czytnik poda przy **Efekty głosu**, że potrzebuje dźwięku o niskim opóźnieniu. Gdy syntezator przekazuje tekst innemu silnikowi, efekt zadziała tylko wtedy, gdy przekaże również sesję audio.

Możesz dodać **Efekty głosu** i wybrane pasma, na przykład **Korektor 1 kHz**, do sterowania czytaniem. Przesunięcie zmienia poziom pasma o 1 dB. Polecenie **Używaj efektów głosu** przełącza efekty w aktualnym profilu.

### Języki {#speech-languages}

Jeśli aplikacja oznaczy tekst innym językiem, SandFly może dobrać właściwy głos. W grupie **Języki** znajdziesz te opcje.

- **Automatycznie przełączaj język** wybiera głos dla języka tekstu. Opcja jest domyślnie włączona. SandFly rozpoznaje też nieoznaczony tekst po alfabecie, na przykład greckim lub arabskim, oraz po literach występujących tylko w jednym z używanych języków.
- **Automatycznie przełączaj dialekt** uwzględnia odmianę regionalną, na przykład angielski brytyjski zamiast amerykańskiego. Opcja jest domyślnie włączona.
- **Głosy według języka** pozwalają określić [głosy dla wybranych języków](#speech-voices-by-language).
- **Rozpoznawaj języki na podstawie słów** rozróżnia języki korzystające z tego samego alfabetu, gdy aplikacja ich nie oznacza. Usługi Google Play rozpoznają język na urządzeniu dla fragmentów zawierających co najmniej 12 liter. Opcja jest domyślnie wyłączona.

Jeżeli wyłączysz przełączanie, SandFly przeczyta tekst bieżącym głosem bez dodatkowych pauz. Polecenia **Automatycznie przełączaj język** i **Rozpoznawaj języki na podstawie słów** znajdziesz w kategorii **Mowa**.

#### Dobierz głos do języka {#speech-voices-by-language}

1. Otwórz **Mowa** > **Głosy według języka**.
2. Wybierz **Dodaj język**. SandFly pokaże języki zainstalowanych syntezatorów.
3. Wybierz język. SandFly przypisze mu **Domyślny profil głosowy SandFly**.
4. Aby ustawić inny profil, otwórz język na liście i go wybierz.

Domyślny profil korzysta z głosu syntezatora dostępnego dla danego języka. Jeśli wolisz inny głos, przypisz przygotowany profil. Aby usunąć wpis, wybierz język, a potem **Usuń język**.

Gdy kilka języków korzysta z tego samego alfabetu, na przykład angielski i francuski z łacińskiego, SandFly pokaże dodatkową listę dla alfabetu. Możesz na niej wybrać język używany wtedy, gdy inne wskazówki nie wystarczają.

#### Zmień język wypowiedzi {#speech-spoken-language}

Użyj **Zmień język wypowiedzi** lub dodaj opcję **Język wypowiedzi** do sterowania czytaniem. Zmienisz wtedy język domyślnego głosu SandFly. Podczas korzystania z własnego profilu decyduje język z profilu.

### Wymowa {#speech-pronunciation}

- **Zmieniaj ton** odczytuje usuwane litery wyższym głosem. Opcja jest domyślnie włączona.
- **Odczytuj litery fonetycznie podczas przeglądania** dotyczy czytania po znakach i literowania tekstu.
- **Odczytuj litery fonetycznie podczas pisania** dotyczy klawiszy oraz wpisywanych i usuwanych znaków.
- **Przekazuj długi tekst do syntezatora zdanie po zdaniu** pomaga szybciej przerwać długi odczyt w syntezatorach takich jak Gryphon. W niektórych silnikach osobne zdania mają dłuższe pauzy. Jeśli Twój syntezator od razu przerywa mowę, możesz zostawić opcję wyłączoną. Tak jest ustawiona domyślnie.
- **Odczytuj podziały wierszy jako spacje** pozwala czytać bez przerw na końcu wiersza. Domyślnie jest wyłączone.
- [**Słownik mowy** i **Słownik symboli**](#speech-dictionary) pozwalają zmienić wymowę.

Przy odczytywaniu fonetycznym możesz wybrać samą literę, zastąpić ją słowem albo usłyszeć literę i słowo razem. Domyślnie działa **Po literze, z pauzą, np. „h, Henryk”**. Przy przechodzeniu po znakach możesz od razu przejść dalej, żeby pominąć słowo. Dostępne są też **Po literze, bez pauzy, np. „h Henryk”** i **Zamiast litery, np. „Henryk”**.

### Słownik mowy {#speech-dictionary}

W **Mowa** > **Słownik mowy** możesz zastąpić tekst inną wymową. SandFly stosuje wpisy po kolei. Każdy wpis działa na wyniku poprzedniego. Ustawienia czytnika są czytane zgodnie z tym, co jest napisane na ekranie.

1. Wybierz **Dodaj zamiennik**.
2. W polu **Tekst** wpisz szukany fragment.
3. W polu **Wymawiaj jako** wpisz odczyt. Jeśli zostawisz je puste, SandFly pominie ten tekst.
4. Zostaw **Tylko całe słowa**, jeśli szukasz całych słów. Domyślnie opcja jest włączona. Aby użyć wzorca, wybierz **Wyrażenie regularne**.
5. Wybierz **OK**.

Po otwarciu wpisu dostępne są **Edytuj**, **Wybierz aplikację** i **Usuń**. Aby używać wpisu dla jednej aplikacji, wybierz ją zamiast **Wszystkie aplikacje**. Zamiennik działa, gdy okno tej aplikacji jest ostatnim wyświetlonym oknem.

**Importuj z pliku** i **Eksportuj do pliku** pozwalają przenosić wpisy. Zobacz [formaty plików](#api-item-files). Osobny słownik dla monitora znajdziesz w [ustawieniach brajla](#braille).

#### Słownik symboli {#speech-symbol-dictionary}

W **Mowa** > **Słownik symboli** zastąpisz symbole i ich sekwencje w mowie i brajlu. SandFly szuka dokładnie wpisanego tekstu, również obok innych znaków, na przykład kropki zaraz po słowie. Wpisy stosuje w kolejności po słowniku mowy lub brajla. Słownik mieści do 500 wpisów.

Wybierz **Dodaj zamiennik**, wpisz **Symbol** i tekst zastępczy, po czym wybierz **OK**. Możesz edytować, ograniczać do aplikacji, usuwać, importować i eksportować wpisy tak jak w słowniku mowy.

Jeśli [ustawienia interpunkcji](#verbosity-text) pomijają znak, którego syntezator nie czyta sam, jego zamiennik również jest pomijany. Przy poziomie bez odczytu interpunkcji zamienniki pojedynczych symboli działają wtedy, gdy znak zachowała opcja **Odczytuj symbole zmieniające znaczenie**.

### Wstrzymaj i wznów mowę {#speech-pause}

Stuknij dwoma palcami, żeby wstrzymać lub wznowić mowę. To domyślne przypisanie polecenia **Wstrzymaj lub wznów komunikaty głosowe**. Zobacz [wykaz gestów](#appendix-gestures).

- Przy włączonym **Wyjście audio** > **Dźwięk o niskim opóźnieniu** SandFly zatrzymuje i wznawia dźwięk dokładnie w tym samym miejscu, także w środku słowa. Przechowuje do 10 sekund wstrzymanego odczytu.
- Przy zwykłym odtwarzaniu, jeśli syntezator podaje pozycję słowa, odczyt wraca do tego słowa.
- Jeśli syntezator, na przykład RHVoice lub Gryphon, nie podaje pozycji, SandFly szacuje ją na podstawie wcześniejszego tempa. Wraca do początku zdania lub frazy. Możesz usłyszeć kilka słów ponownie.

Po wznowieniu nie ma ponownego dźwięku ani wibracji elementu. Polecenie **Włącz lub wyłącz komunikaty głosowe** przełącza całą mowę. Na [klawiaturze fizycznej](#keyboards) sam Ctrl wstrzymuje mowę.

### Zatrzymywanie mowy {#speech-stop}

- **Zatrzymuj mowę po dotknięciu ekranu** jest domyślnie włączone.
- **Przerywaj mowę podczas pisania** dotyczy znaków z klawiatury fizycznej. Możesz wybrać **Zawsze**, **W polach edycji** lub **Gdy echo klawiatury odczytuje wpisany znak lub słowo**. Domyślnie wybrane jest **Zawsze**. Opcji nie ma na zegarkach.
- **Przerywaj mowę po naciśnięciu klawisza Enter** jest domyślnie włączone. Opcji nie ma na zegarkach.
- **Zakryj czujnik zbliżeniowy, by zatrzymać wypowiedź** działa, gdy urządzenie ma czujnik i nie jest telewizorem. Domyślnie opcja jest wyłączona.
- **Wyłączaj mowę podczas nagrywania** ogranicza odczyt, gdy inna aplikacja nagrywa bez podłączonych słuchawek. SandFly mówi wtedy tylko w ciągu dwóch sekund od dotknięcia, gestu, klawisza lub wpisu brajlowskiego. Domyślnie opcja jest wyłączona.

Na klawiaturze fizycznej większość klawiszy i skróty z Ctrl, Alt lub Meta przerywają mowę. Modyfikatory, klawisze blokady i głośności są wyjątkami. Znaki i Enter podlegają opisanym wyżej ustawieniom. Spacja poza polem tekstowym zawsze zatrzymuje odczyt.

W trakcie nagrania z mikrofonu lub rozmowy SandFly nadal odpowiada na żądania wykonane gestem lub klawiszem, na przykład odczyta godzinę, tytuł okna albo stan baterii.

### Historia mowy {#speech-history}

W kategorii **Czytanie** wybierz **Historia mowy**. Zobaczysz do 100 ostatnich wypowiedzi, od najnowszej. Wybierz wypowiedź, żeby użyć **Kopiuj** albo **Pokaż wszystko**, jeśli na liście widać skrót. Opcja **Wyczyść historię** jest na końcu listy.

SandFly przechowuje do 2000 znaków każdej wypowiedzi w pamięci. Historia znika po wyłączeniu czytnika. SandFly pomija odczyt podczas pracy z polem hasła oraz wypowiedzi z chwili, gdy historia jest otwarta. Polecenie możesz [dodać do menu lub przypisać do gestu](#commands).

### Zezwól innym aplikacjom na odczyt {#speech-other-apps}

Na ekranie **Mowa** otwórz **Zezwalaj innym aplikacjom na odczytywanie tekstu przez SandFly**. Główny przełącznik jest domyślnie wyłączony. Możesz też włączyć **Odtwarzaj dźwięk przed odczytaniem tekstu przekazanego przez inną aplikację**, żeby rozpoznać takie wypowiedzi.

W sekcji **Aplikacje** możesz pozwolić na odczyt przez poszczególne aplikacje i, jeśli jest to obsługiwane, osobno przy zablokowanym urządzeniu. Przy pierwszej próbie SandFly pokaże powiadomienie z nazwą aplikacji oraz przyciskami **Zezwól** i **Nie zezwalaj**. Do czasu zgody czytnik niczego z tej aplikacji nie odczyta.

Kiedy SandFly jest wyłączony lub wstrzymany, odczyt z aplikacji też jest wstrzymany. Aplikacja nie może przerwać ani zastąpić wypowiedzi czytnika. Bieżąca klawiatura może mówić bez dodawania jej do listy i wyłączyć echo SandFly dla własnego pisania. Zobacz [interfejs mowy dla programistów](#api-speech).

### Polecenia i sterowanie mową {#speech-commands}

W kategorii **Mowa** możesz przypisać wstrzymanie i wznowienie, włączenie i wyłączenie mowy, zmianę tempa, tonu, języka i profilu oraz przełączanie wykrywania języka i efektów. Skróty klawiszowe obejmują również głośność, reset tempa i tonu oraz otwarcie ustawień zamiany tekstu na mowę.

Kategoria **Czytanie** zawiera **Powtórz ostatnią wypowiedź**, **Powtórz wolniej ostatnią wypowiedź**, **Przeliteruj ostatnią wypowiedź** i **Historia mowy**.

W sterowaniu czytaniem możesz dodać **Język wypowiedzi**, **Syntezator mowy**, **Profil głosowy**, **Efekty głosu** i pasma korektora. Z tej grupy domyślnie jest już dodana **Szybkość mowy**. Zobacz opcje [głośności i urządzenia odtwarzającego](#sound-reading-controls), [interpunkcji, emoji i formatowania](#verbosity-text), [automatycznego tłumaczenia](#translation) oraz [odczytu własnych ruchów fokusu](#verbosity-own-speech). Przypisania zmienisz w [ustawieniach poleceń](#commands).
