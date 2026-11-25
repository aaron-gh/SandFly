## Polecenia głosowe {#voice-commands}

Poleceniami głosowymi możesz przenosić fokus, czytać i edytować tekst, otwierać aplikacje, aktywować elementy ekranu, dzwonić, wysyłać wiadomości i opisywać ekran. Możesz też w jednej wypowiedzi podać kilka poleceń do wykonania po kolei. Na ekranie **Polecenia głosowe** dodasz własne zwroty i wybierzesz, czy niezrozumiałe polecenia ma obsługiwać AI. Tego ekranu nie ma na zegarku.

Pełny wykaz zwrotów znajdziesz w [](#appendix-voice-commands).

### Użyj polecenia głosowego {#voice-commands-start}

1. Użyj polecenia **Użyj polecenia głosowego**. W układzie gestów SandFly przesuń palec w prawo, a potem w górę. Zobacz [](#appendix-gestures).
2. Powiedz polecenie.

W oknie **Używanie poleceń głosowych podczas korzystania z SandFly** pojawiają się przykładowe zwroty. Wybierz **Używaj poleceń głosowych**, aby rozpocząć nagrywanie. Jeśli chcesz pominąć to okno przy kolejnych użyciach, odznacz **Zawsze pokazuj ten komunikat**.

Polecenia głosowe wymagają dostępu do mikrofonu. SandFly używa mechanizmu rozpoznawania mowy wybranego w menu **Pisanie** > **Dyktowanie**. Zobacz [](#typing).

Polecenia głosowe są niedostępne podczas konfiguracji urządzenia i przy zablokowanym ekranie. Jeśli ekran jest zablokowany, SandFly informuje o tym i podaje gest otwierający menu SandFly.

Aby wyświetlić listę, powiedz „Pomoc” lub „Co mogę powiedzieć?”. Otworzy się **Przegląd poleceń głosowych**.

Polecenie **Użyj polecenia głosowego** jest też w grupie **Czynności systemowe**. Możesz uruchamiać je skrótem klawiszowym albo pozycją menu. O dodawaniu pozycji do menu SandFly przeczytasz w [](#commands-edit-menu).

### Co możesz powiedzieć {#voice-commands-phrases}

SandFly sprawdza to, co powiesz, w następującej kolejności.

1. Szuka zwrotu zapisanego we własnych poleceniach głosowych. Zobacz [](#voice-commands-custom).
2. Szuka nazwy polecenia takiej jak na ekranie **Polecenia**, np. **Czytaj od góry**, **Opisz obraz** albo **Następny nagłówek**. Możesz też powiedzieć nazwę swojego makra, menu lub polecenia skryptu. Zobacz [](#commands-list).
3. Próbuje dopasować wzorzec wypowiedzi, np. „otwórz Chrome”, „wybierz Ustawienia”, „zadzwoń do Alex” lub „wpisz cześć”. Zobacz [](#voice-commands-sandfly).
4. Sprawdza starsze polecenia głosowe, takie jak „Znajdź zapisz”, „Nawigacja typu słowa”, „Mów szybciej” lub „Kopiuj”. Zobacz [](#appendix-voice-commands).
5. Jeśli włączono **Rozumiej polecenia głosowe za pomocą AI**, przekazuje nierozpoznaną wypowiedź do usługi AI. Zobacz [](#voice-commands-ai).

Wielkość liter i znaki przestankowe dodane przy rozpoznawaniu mowy nie wpływają na dopasowanie. SandFly pomija też m.in. „proszę”, „czy możesz”, „możesz”, „chcę” i „teraz” na początku wypowiedzi oraz „proszę”, „dla mnie” i „teraz” na końcu.

Jeśli SandFly nie rozpozna polecenia, odczyta odpowiedni komunikat i podpowie, jak otworzyć listę poleceń.

#### Aplikacje, elementy i tekst {#voice-commands-sandfly}

- Aby otworzyć aplikację, powiedz np. „otwórz”, „uruchom”, „włącz” lub „przełącz na”, a potem jej nazwę. SandFly najpierw szuka dokładnej nazwy. Jeśli nie znajdzie dokładnego dopasowania, szuka nazwy zaczynającej się od tego tekstu, a na końcu nazwy zawierającej jedno z jego słów. Jeśli brakuje dopasowania albo pasuje kilka aplikacji w równym stopniu, SandFly spróbuje uruchomić polecenie o tej nazwie, np. „otwórz powiadomienia”. Jeśli polecenia też nie ma, zwrot pozostaje nierozpoznany.
- Aby aktywować element ekranu, powiedz np. „stuknij”, „dotknij dwukrotnie”, „kliknij”, „naciśnij”, „aktywuj” lub „wybierz” i podaj jego tekst. SandFly aktywuje pierwszy element o dokładnie takim tekście. Jeśli go nie znajdzie, użyje pierwszego elementu zawierającego podany tekst. Gdy żaden nie pasuje, usłyszysz „Nie znaleziono” i szukany tekst.
- Aby użyć funkcji **Wpisz tekst**, powiedz „wpisz”, „napisz” lub „wprowadź”, a potem podyktuj treść. SandFly wpisze ją do pola edycji, na którym jest fokus.
- Aby uruchomić polecenie lub makro, powiedz np. „wykonaj” i nazwę polecenia albo „uruchom makro” i nazwę makra. Przykładowy zwrot to „uruchom makro poranek”.
- Polecenie **Czekaj** wstawia przerwę między poleceniami. Powiedz „poczekaj”, „poczekaj sekundę” lub „poczekaj” i podaj liczbę sekund. Możesz poczekać maksymalnie 60 sekund.

#### Połączenia i wiadomości {#voice-commands-calls}

- Aby użyć funkcji **Zadzwoń**, powiedz np. „zadzwoń do”, „zadzwoń na”, „dzwoń do” lub „połącz z” i podaj nazwę kontaktu albo numer telefonu. Usłyszysz „Dzwonię do” i nazwę, a SandFly rozpocznie połączenie. Przy pierwszym użyciu pojawi się prośba o uprawnienie do wykonywania połączeń. Po przyznaniu uprawnienia powiedz polecenie ponownie. Bez uprawnienia aplikacja Telefon otworzy się z wpisanym numerem.
- Aby wysłać wiadomość, powiedz np. „napisz do Alex już idę”, „wyślij SMS do Alex o treści już idę”, „wyślij wiadomość do Alex w WhatsApp o treści cześć” albo „napisz do Alex w Signal cześć”. Jeśli pominiesz treść, usłyszysz „Wiadomość do” i nazwę kontaktu, a następnie możesz podyktować wiadomość.

Wyszukiwanie po nazwach kontaktów wymaga dostępu do kontaktów. Prośba o to uprawnienie pojawi się przy pierwszym użyciu. Jeśli kilka kontaktów ma tę samą nazwę, SandFly wymieni pełne nazwy. Powiedz wtedy pełną nazwę właściwego kontaktu. Jeśli kontaktu nie ma, SandFly odczyta komunikat „Brak kontaktu o nazwie” i podaną nazwę.

Jeśli nie wskażesz aplikacji, SandFly użyje domyślnej aplikacji SMS. Przy wiadomości SMS lub WhatsApp otworzy rozmowę z wpisanym tekstem, odczyta odbiorcę i treść, a potem nagra odpowiedź. Powiedz „tak”, „wyślij”, „OK” lub „jasne”, aby SandFly nacisnął **Wyślij** i odczytał „Naciśnięto Wyślij”. Przy innej odpowiedzi lub braku odpowiedzi tekst pozostanie w aplikacji, a SandFly odczyta „Wiadomość nie została wysłana”. W pozostałych aplikacjach otworzy się ekran udostępniania z tekstem. Wybierz tam rozmowę.

#### Opisy {#voice-commands-describe}

- Aby opisać ekran, powiedz „opisz ekran” lub „co jest na ekranie”.
- Aby opisać obraz, na którym jest fokus, powiedz np. „opisz ten obraz”, „opisz zdjęcie” albo „co jest na tym zdjęciu”.
- **Kopiuj**. Zwrot „skopiuj to” lub „skopiuj opis” kopiuje opis zwrócony przez poprzednie polecenie. Jeśli poprzednie polecenie nie zwróciło opisu, kopiuje ostatnią wypowiedź SandFly.
- Zwrot „wyślij to do Alex”, „udostępnij to Alex” lub „wyślij to do Alex w WhatsApp” wysyła poprzedni opis albo, jeśli go nie ma, ostatnią wypowiedź. Możesz też powiedzieć np. „wyślij opis obrazu do Alex” lub „wyślij opis ekranu do Alex”, aby od razu opisać i wysłać. Przed naciśnięciem **Wyślij** SandFly odczytuje nazwę odbiorcy i prośbę o potwierdzenie.

Te polecenia korzystają z usług AI lub opisów na urządzeniu określonych na ekranach **Opisy obrazów i ekranu** i **Usługi AI**. Zobacz [](#descriptions).

### Połącz kilka poleceń {#voice-commands-chains}

Jedna wypowiedź może zawierać kilka poleceń. Połącz je słowami „potem”, „a następnie”, „następnie”, „a potem” lub „po tym”. Możesz powiedzieć np. „otwórz Wiadomości, potem zadzwoń do Alex”. Słowo „i” też oddziela polecenia, gdy obie części są poleceniami. „Przejdź do ekranu głównego i otwórz Chrome” zawiera dwa polecenia, a „wybierz Sól i pieprz” jedno. SandFly wykonuje je kolejno, z krótką przerwą po każdym poleceniu zmieniającym ekran.

Jeśli polecenie się nie powiedzie, np. brakuje elementu, SandFly poda przyczynę i zatrzyma wykonywanie. Gdy część wypowiedzi nie pasuje do polecenia, SandFly spróbuje potraktować ją jako jedno ze starszych poleceń głosowych. Jeśli i to się nie uda, odczyta „Nie zrozumiano” oraz nierozpoznaną część.

Aby zatrzymać wykonywanie, zacznij kolejne polecenie głosowe i powiedz „stop”, „anuluj”, „przerwij” lub „nieważne”. Rozpoczęcie nowej wypowiedzi również zatrzymuje poprzednią.

### Rozpoznawanie poleceń za pomocą AI {#voice-commands-ai}

Opcja **Polecenia głosowe** > **Rozumiej polecenia głosowe za pomocą AI** przekazuje do bieżącej usługi AI wypowiedzi, których SandFly nie rozpoznał, w tym sekwencje rozpoznane tylko częściowo. Usługa zamienia je na polecenia. SandFly wysyła treść wypowiedzi, nazwy poleceń i teksty elementów widocznych na ekranie. Domyślnie opcja jest wyłączona.

Najpierw skonfiguruj usługę na ekranie **Usługi AI**. Może to być AI na urządzeniu, SandFly Pro lub inna usługa z kluczem. Zobacz [](#descriptions) i [](#pro). Odpowiedź AI może uruchomić polecenia, aktywować elementy, otworzyć aplikacje, wpisać tekst, poczekać, wykonać połączenie, wysłać wiadomość, opisać ekran lub obraz, skopiować tekst albo odczytać krótką odpowiedź.

### Własne polecenia głosowe {#voice-commands-custom}

Własne polecenie głosowe to zwrot, który uruchamia wybrane polecenie. SandFly sprawdza własne zwroty przed innymi poleceniami głosowymi.

1. W ustawieniach SandFly wybierz **Polecenia głosowe**.
2. W sekcji **Własne polecenia głosowe** wybierz **Dodaj polecenie głosowe**.
3. Wpisz zwrot o długości do 200 znaków.
4. Wybierz grupę, a w niej polecenie.

Wybierz zapisane polecenie, aby użyć opcji **Zmień frazę**, **Zmień polecenie** lub **Usuń**. Każde własne polecenie musi mieć inny zwrot.

Opcja **Eksportuj do pliku** zapisuje własne polecenia, a **Importuj z pliku** dodaje je z pliku. Zobacz [](#api-item-files).

Własny zwrot może uruchomić dowolne polecenie, również makro lub własne menu. Jeśli chcesz wykonać kilka kroków za pomocą jednego zwrotu, utwórz makro i przypisz do niego zwrot. Zobacz [](#macros).

### Języki {#voice-commands-languages}

Rozpoznawanie mowy używa języka urządzenia. Starsze zwroty, wzorce wypowiedzi i słowa łączące polecenia są tłumaczone razem z SandFly. Nazwy poleceń rozpoznawane są w języku, w którym SandFly je wyświetla. W [](#appendix-voice-commands) znajdziesz zwroty rozpoznawane przez polską wersję.
