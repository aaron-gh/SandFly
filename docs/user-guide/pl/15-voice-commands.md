## Polecenia głosowe {#voice-commands}

Głosem możesz przejść do innej pozycji, odczytać ekran, edytować tekst, otworzyć aplikację albo wywołać inne [polecenie SandFly](#commands-list). Możesz też wybrać element ekranu po jego nazwie, zadzwonić do kontaktu i wysłać wiadomość. Ustawienia **Polecenia głosowe** pozwalają dodać własne frazy i włączyć rozumienie poleceń przez AI. Tego ekranu nie ma na zegarkach.

### Jak powiedzieć polecenie {#voice-commands-start}

1. Uruchom **Użyj polecenia głosowego**. W domyślnym układzie SandFly przesuń palec w prawo, a potem w górę. Zobacz [domyślne gesty](#appendix-gestures).
2. Przy pierwszym użyciu wybierz **Używaj poleceń głosowych**. Jeśli okno z przykładami ma się już nie pojawiać, odznacz **Zawsze pokazuj ten komunikat**.
3. Powiedz, co chcesz zrobić.

SandFly potrzebuje zgody na używanie mikrofonu. Rozpoznaje głos w usłudze wybranej w **Pisanie** > **Dyktowanie**. Więcej znajdziesz w [rozdziale o pisaniu](#typing).

Polecenia głosowe nie działają na zablokowanym ekranie ani podczas konfiguracji urządzenia. Na ekranie blokady SandFly wyjaśni przyczynę i poda gest otwierający jego menu.

Powiedz „pomoc”, żeby otworzyć **Przegląd poleceń głosowych**. Możesz też [dodać polecenie](#commands-edit-menu) **Użyj polecenia głosowego** z kategorii **Czynności systemowe** do menu SandFly lub przypisać je do skrótu klawiszowego.

### Co możesz powiedzieć {#voice-commands-phrases}

SandFly sprawdza w tej kolejności:

1. [Twoje własne frazy](#voice-commands-custom).
2. Nazwy takie jak na ekranie **Polecenia**, na przykład „Czytaj od góry” i „Następny nagłówek”. Dotyczy to również nazw Twoich makr, menu i poleceń skryptu.
3. [Podstawowe wzorce poleceń](#voice-commands-sandfly), na przykład „otwórz Chrome”, „kliknij Ustawienia” i „wpisz dzień dobry”.
4. [Pozostałe polecenia głosowe](#appendix-voice-commands), na przykład „znajdź zapisz” i „mów szybciej”.
5. [Usługę AI](#voice-commands-ai), jeśli została włączona.

Wielkość liter i interpunkcja dodana przez rozpoznawanie mowy nie mają znaczenia. Możesz zacząć od „proszę”, „czy możesz” lub „chcę” albo zakończyć „proszę”, „dla mnie” lub „teraz”. SandFly pomija te zwroty przy szukaniu polecenia. Jeśli go nie rozpozna, podpowie, jak otworzyć listę.

#### Otwieranie aplikacji, klikanie i pisanie {#voice-commands-sandfly}

- **Otwórz aplikację**. Powiedz „otwórz Chrome”, „uruchom Chrome” lub „przełącz na Chrome”. SandFly szuka pełnej nazwy, potem początku nazwy, a na koniec wspólnego słowa. Gdy nie ma wyniku lub kilka wyników pasuje tak samo, spróbuje wykonać polecenie o tej nazwie.
- Żeby wybrać element na ekranie, powiedz na przykład „kliknij Ustawienia”, „stuknij Ustawienia” lub „wybierz Ustawienia”. SandFly najpierw szuka dokładnego tekstu, a potem pierwszej pozycji, która go zawiera.
- **Wpisz tekst**. Powiedz „wpisz” lub „wprowadź”, a potem treść. SandFly wpisze ją do pola tekstowego, na którym jest fokus.
- Żeby uruchomić działanie, powiedz „wykonaj” i nazwę działania albo „uruchom makro” i nazwę makra.
- Żeby zrobić przerwę, powiedz „poczekaj”, „poczekaj sekundę” albo podaj przerwę, na przykład „poczekaj pięć sekund”. Najdłuższa przerwa trwa 60 sekund.

#### Połączenia i wiadomości {#voice-commands-calls}

Powiedz „zadzwoń do” i nazwę kontaktu albo „zadzwoń na” i numer. SandFly poda wybraną osobę i rozpocznie połączenie. Po pierwszej prośbie o zgodę na połączenia powtórz polecenie. Jeśli jej nie udzielisz, otworzy się aplikacja telefonu z wpisanym numerem.

Aby wysłać wiadomość, powiedz na przykład „napisz do” i nazwę kontaktu, a potem treść. Możesz podać komunikator, na przykład „wyślij wiadomość do” i nazwę kontaktu, „przez Signal o treści” i wiadomość. Bez treści SandFly dopyta i rozpocznie nagrywanie.

Przy pierwszym użyciu nazwy kontaktu SandFly poprosi o dostęp do kontaktów. Jeśli znajdzie kilka takich samych imion, poda pełne nazwy. Powtórz polecenie z pełną nazwą odczytaną przez SandFly.

Domyślnym wyborem jest aplikacja SMS. W przypadku SMS lub WhatsApp SandFly wstawi tekst do rozmowy, przeczyta odbiorcę i treść oraz poprosi o potwierdzenie. Powiedz „tak” lub „wyślij”, żeby nacisnął **Wyślij**. Bez potwierdzenia treść pozostanie w aplikacji. W innych komunikatorach może się otworzyć ekran udostępniania. Wybierz tam rozmowę.

#### Opisy ekranu i obrazów {#voice-commands-describe}

Powiedz „opisz ekran”, „opisz ten obraz” lub „opisz to zdjęcie”. SandFly używa [opisów lokalnych lub wybranej usługi AI](#descriptions). „Skopiuj opis” bierze treść poprzedniego opisu. Gdy opisu nie ma, kopiuje ostatnią wypowiedź SandFly.

„Wyślij opis do” i nazwa kontaktu podaje tę treść jako wiadomość. Możesz też podać komunikator, mówiąc „wyślij to do” i nazwę kontaktu, potem „przez” i nazwę aplikacji. „Wyślij opis ekranu do” lub „wyślij opis obrazu do” najpierw tworzy opis i zaczyna wysyłanie. SandFly podaje odbiorcę przed użyciem przycisku **Wyślij**.

### Kilka poleceń pod rząd {#voice-commands-chains}

Oddziel polecenia słowem „potem”, „następnie” lub „a potem”. Na przykład „otwórz Ustawienia, a potem kliknij Dostępność”. Możesz użyć też „i”, jeśli po obu stronach są osobne polecenia. Dzięki temu zdanie „kliknij Sól i pieprz” nadal może oznaczać jeden element o tej nazwie.

SandFly wykonuje kroki po kolei. Po zmianie ekranu robi krótką przerwę. Gdy elementu nie da się znaleźć albo polecenie się nie powiedzie, poda przyczynę i zakończy sekwencję. Jeśli części zdania nie uda się zinterpretować, spróbuje rozpoznać ją jako jedno ze starszych poleceń.

Aby przerwać wykonywanie, rozpocznij nowe polecenie głosowe. Możesz też powiedzieć „stop”, „anuluj” lub „przerwij”.

### Rozumienie poleceń przez AI {#voice-commands-ai}

Włącz **Polecenia głosowe** > **Rozumiej polecenia głosowe za pomocą AI**, żeby użyć AI, gdy SandFly nie rozumie wypowiedzi lub części sekwencji. Do wybranej usługi trafiają Twoje słowa, nazwy dostępnych poleceń i tekst elementów na ekranie. Opcja jest domyślnie wyłączona.

Najpierw skonfiguruj [Usługi AI](#descriptions), na przykład model na urządzeniu, [SandFly Pro](#pro) albo usługę z własnym kluczem. Odpowiedź może uruchomić polecenie, wybrać element, otworzyć aplikację, wpisać tekst, poczekać, zadzwonić, wysłać wiadomość, opisać obraz lub ekran, skopiować wynik albo wypowiedzieć krótką odpowiedź.

### Własne polecenia głosowe {#voice-commands-custom}

1. Otwórz **Polecenia głosowe** w ustawieniach.
2. W sekcji **Własne polecenia głosowe** wybierz **Dodaj polecenie głosowe**.
3. Wpisz frazę do 200 znaków.
4. Wybierz kategorię i działanie.

Własna fraza ma pierwszeństwo przy rozpoznawaniu. Każda musi mieć inną treść. Wybierz zapisane polecenie, żeby użyć **Zmień frazę**, **Zmień polecenie** lub **Usuń**.

**Eksportuj do pliku** i **Importuj z pliku** pozwalają zapisać i dodać [zestaw poleceń](#api-item-files). Fraza może uruchamiać własne menu lub [makro](#macros). Jeśli kilka czynności ma zaczynać się od krótkiego hasła, umieść je w jednym makrze.

### Język poleceń {#voice-commands-languages}

Rozpoznawanie używa języka urządzenia. Frazy i słowa łączące zależą od tłumaczenia programu, a nazwy poleceń wypowiadasz tak, jak są wyświetlane przez SandFly. W [dodatku o poleceniach](#appendix-voice-commands) znajdziesz także starsze formuły i przykłady.
