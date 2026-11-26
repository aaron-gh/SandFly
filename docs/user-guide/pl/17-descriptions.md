## Opisy obrazów i ekranu {#descriptions}

SandFly może opisać obraz, na którym jest fokus, cały ekran albo widok z tylnego aparatu. Może też odpowiadać na pytania o to, co jest na obrazie. Masz do wyboru trzy źródła opisów.

- Usługa AI, taka jak Gemini, Claude lub OpenAI, z własnym kluczem API. Zobacz [](#descriptions-ai-services).
- SandFly Pro, bez własnego klucza API. Zobacz [](#pro).
- Model uruchamiany w telefonie, który nie przesyła materiałów przez sieć. Zobacz [](#descriptions-on-device).

Tekst z obrazów jest rozpoznawany na telefonie niezależnie od źródła opisów.

Na ekranie **Opisy obrazów i ekranu** możesz włączyć szczegółowe opisy i otworzyć rozpoznawanie na żywo. W **Usługi AI** wybierasz, co tworzy opisy. Te funkcje działają na telefonach i tabletach. Na zegarkach i telewizorach są niedostępne.

### Włącz opisy obrazów i ekranu {#descriptions-set-up}

1. Skonfiguruj usługę AI, SandFly Pro lub model na urządzeniu. Zobacz [](#descriptions-set-up-service), [](#pro) lub [](#descriptions-on-device-download).
2. Otwórz **Opisy obrazów i ekranu** i włącz **Szczegółowe opisy obrazów**.
3. Przeczytaj informację o tym, jakie dane i do kogo trafią. Wybierz **Włącz**.

Jeśli użyjesz **Opisz obraz** przy wyłączonej opcji **Szczegółowe opisy obrazów**, pojawi się okno **Włączyć szczegółowe opisy obrazów?**. Wybierz **Włącz**, żeby włączyć ustawienie i opisać obraz. Jeśli wybierzesz **Nie, dziękuję**, SandFly odczyta tylko rozpoznany tekst, a ustawienie pozostanie wyłączone.

Przy pierwszym użyciu **Opisz ekran** pojawi się okno **Opis ekranu** z informacją o przesyłaniu zrzutu. Wybierz **Włącz**, żeby uzyskać opis.

### Ustawienia opisów {#descriptions-settings}

- **Szczegółowe opisy obrazów** pozwalają opisać obraz za pomocą wybranej usługi lub modelu. Domyślnie są wyłączone.
- **Automatyczne opisy tekstu na obrazach** odczytują tekst po przejściu na obraz. Wybierz **Wszystkie obrazy**, **Tylko obrazy bez etykiet** (domyślnie) lub **Nie opisuj obrazów**. Rozpoznawanie tekstu odbywa się na urządzeniu.
- **Rozpoznawanie tekstu cyrylicą** pobiera i usuwa modele rozpoznawania tekstu dla języków zapisywanych cyrylicą, takich jak rosyjski, ukraiński i serbski. Zobacz [](#descriptions-cyrillic-text). Opcja jest niedostępna na zegarkach.
- **Rozpoznawanie na żywo** otwiera [ekran rozpoznawania](#descriptions-live-recognition). Opcja jest dostępna na urządzeniach z aparatem.

### Rozpoznawanie tekstu cyrylicą {#descriptions-cyrillic-text}

Rozpoznawanie tekstu odczytuje tekst zapisany cyrylicą za pomocą modelu dla danego języka, który najpierw trzeba pobrać. SandFly używa modelu języka mowy lub pierwszego języka telefonu zapisywanego cyrylicą. Bez modelu odczytywane są tylko litery łacińskie, a SandFly jednorazowo informuje, że model nie jest pobrany.

1. Otwórz **Opisy obrazów i ekranu** > **Rozpoznawanie tekstu cyrylicą**.
2. Wybierz język. Model o rozmiarze kilku megabajtów zostanie pobrany, za pierwszym razem razem z modelem liter łacińskich, aby odczytywać tekst w obu alfabetach. Przy komórkowej transmisji danych najpierw otwiera się okno potwierdzenia.

Aby zatrzymać pobieranie, ponownie wybierz język. Aby usunąć model, wybierz pobrany język, a potem **Usuń**. Model liter łacińskich jest usuwany razem z ostatnim językiem.

### Opisz obraz {#descriptions-describe-image}

1. Przenieś fokus na obraz.
2. Wybierz **Opisz obraz** w menu SandFly lub użyj przypisanego gestu albo klawisza. Zobacz [](#descriptions-commands).

W oknie **Opis obrazu** znajdziesz opis i tekst rozpoznany na obrazie. Możesz skopiować opis, ocenić go lub zadać pytanie. Zobacz [](#descriptions-questions).

Jeśli szczegółowy opis jest niedostępny, SandFly odczyta sam tekst z obrazu i poda powód. Na przykład może odczytać „Nie można uzyskać bardziej szczegółowego opisu obrazu” i wyjaśnienie albo poinformować, że opis wymaga połączenia z internetem.

Żeby usłyszeć opis bez otwierania okna, uruchom **Odczytaj opis obrazu**.

### Opisz ekran {#descriptions-describe-screen}

Uruchom **Opisz ekran**. SandFly zrobi zrzut, prześle go razem z elementami ekranu i otworzy okno **Opis ekranu**. W tym samym oknie możesz zadać pytania o ekran. Zobacz [](#descriptions-questions).

Żeby usłyszeć wynik bez otwierania okna, użyj **Odczytaj opis ekranu**.

**Opisz ekran** działa w obsługiwanych językach telefonu. Android blokuje zrzuty zabezpieczonych okien, na przykład niektórych ekranów aplikacji bankowych.

### Zadaj pytanie o obraz lub ekran {#descriptions-questions}

Pytania trafiają do tej samej usługi AI albo do tego samego modelu co opis. W oknie **Opis obrazu** lub **Opis ekranu** są dwie możliwości.

- **Zapytaj głosem** nagrywa pytanie przy użyciu mechanizmu rozpoznawania mowy wybranego w **Pisanie** > **Dyktowanie**. SandFly wyśle pytanie, a następnie odczyta odpowiedź. Żeby zakończyć nagrywanie, wybierz **Przestań słuchać**. Zobacz [](#typing).
- **Wpisz pytanie** otwiera pole tekstowe. Wpisz pytanie i wybierz **Zapytaj**.

Odpowiedzi zostają w oknie. Możesz zadawać kolejne pytania o ten sam obraz lub ekran.

Możesz też od razu użyć polecenia **Zapytaj o obraz** lub **Zapytaj o ekran**.

1. Jeśli pytasz o obraz, przenieś na niego fokus.
2. Uruchom odpowiednie polecenie. SandFly zrobi zrzut obrazu lub ekranu i zacznie nagrywanie.
3. Wypowiedz pytanie. Nagrywanie zakończy się po przerwie w mówieniu lub przy ponownym użyciu polecenia.

SandFly wyśle pytanie i odczyta odpowiedź. Gdy nagrywania nie da się rozpocząć, otworzy się pole tekstowe. Wpisz pytanie i wybierz **Zapytaj**. Jeśli jeszcze nie wyraziłeś zgody na przesyłanie zrzutów do usługi AI, polecenie otworzy odpowiednio **Opisz obraz** lub **Opisz ekran**, gdzie znajdziesz okno zgody.

### Polecenia dotyczące opisów {#descriptions-commands}

W ustawieniach **Polecenia** otwórz kategorię **Opisy obrazów i ekranu**. Możesz tam przypisać polecenia do gestów i klawiszy lub dodać je do menu SandFly. Zobacz [](#commands).

- **Zrób zdjęcie i opisz** robi zdjęcie tylnym aparatem i je opisuje. Zobacz [](#descriptions-take-picture).
- **Opisz obraz** wyświetla opis obrazu, na którym jest fokus. **Odczytaj opis obrazu** czyta go na głos, a **Zapytaj o obraz** odpowiada na wypowiedziane pytanie.
- **Opisz ekran**, **Odczytaj opis ekranu** i **Zapytaj o ekran** robią to samo dla całego ekranu.
- **Udostępnij obraz zaznaczonego elementu** i **Udostępnij zrzut ekranu** robią zrzut elementu lub całego ekranu i otwierają ekran udostępniania Androida. W ten sposób możesz wysłać zrzut do innej aplikacji. Polecenia wymagają Androida 11 lub nowszego. Zabezpieczone okna na zrzucie są czarne.
- **Zgłoś odpowiedź AI** otwiera zgłoszenie problemu z ostatnim tekstem zwróconym przez model, na przykład opisem lub odpowiedzią. Zobacz [](#help).

Jeśli masz SandFly Pro, w tej kategorii pojawiają się też polecenia rozpoznawania ekranu. Zobacz [](#pro-screen-recognition).

Kategoria **Rozpoznawanie na żywo** ma polecenie dla każdego trybu, na przykład **Rozpoznawanie na żywo: Sceny**, oraz polecenie **Zatrzymaj rozpoznawanie na żywo**. Zobacz [](#descriptions-live-recognition).

Domyślne gesty i skróty znajdziesz w [](#appendix-gestures) i [](#appendix-keyboard-shortcuts).

#### Czynności AI podczas czytania {#descriptions-ai-actions}

Opcja **Czynności AI** pozwala wybrać polecenie bez otwierania menu. Wymaga Androida 11 lub nowszego. Obejmuje **Opisz ekran**, **Odczytaj opis ekranu**, **Zapytaj o ekran**, **Opisz obraz**, **Odczytaj opis obrazu**, **Zapytaj o obraz**, **Udostępnij obraz zaznaczonego elementu** i **Udostępnij zrzut ekranu**. W SandFly Pro dostępne są też **Uruchom lub zatrzymaj zadanie agenta AI** i **Wpisz zadanie dla agenta AI**.

1. Wybierz **Czynności AI** w sterowaniu czytaniem.
2. Przesuń palcem w górę lub w dół, żeby wybrać czynność.
3. Stuknij dwa razy, żeby ją wykonać.

Dwukrotne stuknięcie uruchamia ostatnio odczytaną czynność, dopóki fokus pozostaje na tym samym elemencie. W **Sterowanie czytaniem** > **Uwzględniane czynności AI** możesz wybrać, co znajdzie się na liście. Domyślnie włączone są wszystkie czynności. SandFly pomija te, które są w danej chwili niedostępne. Zobacz [](#commands).

### Zrób zdjęcie z opisem {#descriptions-take-picture}

**Zrób zdjęcie i opisz** robi jedno zdjęcie tylnym aparatem i opisuje je tak samo jak **Opisz obraz**. W oknie opisu możesz też zadawać pytania o zdjęcie. Polecenie jest dostępne na urządzeniach z aparatem.

1. Uruchom **Zrób zdjęcie i opisz**. Usłyszysz „Trzymaj telefon nieruchomo”, po czym SandFly zrobi zdjęcie.
2. Przeczytaj opis. Jeśli potrzebujesz szczegółów, zadaj pytanie o zdjęcie.
3. Żeby zrobić następne zdjęcie, wybierz **Zrób kolejne zdjęcie**.

Przy pierwszym użyciu pojawi się systemowa prośba o dostęp do aparatu.

### Usługi AI {#descriptions-ai-services}

Na głównym ekranie ustawień otwórz **Usługi AI**. Tu wybierasz źródło opisów obrazów i ekranów oraz odpowiedzi na pytania o nie. Ta sama usługa obsługuje rozpoznawanie na żywo, tłumaczenia w trybie **Usługa AI** i inne funkcje korzystające z AI.

- **Usługa AI** pokazuje używaną usługę. Możesz wybrać jedną ze skonfigurowanych usług albo **Brak**.
- Każda usługa ma własny ekran. Są to **Ustawienia Gemini**, **Ustawienia Claude**, **Ustawienia OpenAI (ChatGPT)**, **Ustawienia Mistral**, **Ustawienia Groq**, **Ustawienia OpenRouter**, **Ustawienia Ollama Cloud**, **Ustawienia Qwen**, **Ustawienia Zhipu GLM**, **Ustawienia Baichuan**, **Ustawienia DeepSeek**, **Ustawienia Kimi**, **Ustawienia Hunyuan**, **Ustawienia SiliconFlow** i **Ustawienia Inna usługa**. Pozycja aktywnej usługi ma opis **W użyciu**, a pozycja bez konfiguracji **Nie skonfigurowano**. W wydaniach z SandFly Pro pozycja **Ustawienia SandFly Pro** otwiera główny ekran Pro. Zobacz [](#pro).
- W **AI na urządzeniu** możesz pobrać i skonfigurować model na telefonie. Zobacz [](#descriptions-on-device).
- **Utrzymuj modele rozpoznawania w pamięci** i **Serwer pobierania modeli** dotyczą lokalnych modeli. Zobacz [](#descriptions-model-settings).

#### Skonfiguruj usługę {#descriptions-set-up-service}

1. Otwórz **Usługi AI** i ustawienia usługi, na przykład **Ustawienia Claude**.
2. W przypadku **Inna usługa** wybierz **Adres**. Wpisz adres zaczynający się od `https://`. Usługa musi udostępniać interfejs czatu zgodny z OpenAI i przyjmować obrazy.
3. Otwórz pozycję klucza API, na przykład **Klucz API Claude**. Jeśli dostawca udostępnia stronę tworzenia klucza, przejdziesz do niej przyciskiem **Uzyskaj klucz**. Wklej klucz i wybierz **Zapisz**.
4. Otwórz pozycję modelu, na przykład **Model Claude**. SandFly pobierze listę modeli dostępnych dla Twojego klucza. Wybierz model z listy lub użyj **Wprowadź nazwę modelu** i wpisz ją ręcznie. Jeśli pobranie listy się nie powiedzie, SandFly poda przyczynę i otworzy pole nazwy.

Usługa jest gotowa, gdy ma adres, klucz API i model. Jeżeli nie używasz innej usługi, SandFly od razu zacznie korzystać z nowo skonfigurowanej. Żeby usunąć klucz, wyczyść jego pole i wybierz **Zapisz**. Gdy używana usługa straci klucz lub model, SandFly przejdzie na następną skonfigurowaną usługę albo wybierze **Brak**.

Klucze API pozostają w telefonie. Eksport ustawień ich nie obejmuje. Zobacz [](#profiles).

#### Wybierz usługę AI {#descriptions-choose-service}

1. Otwórz **Usługi AI** > **Usługa AI**.
2. Wybierz usługę lub **Brak**, żeby wyłączyć opisy AI.

Jeśli żadna usługa nie jest skonfigurowana, pojawi się komunikat „Nie skonfigurowano żadnej usługi AI. Skonfiguruj jedną w jej ustawieniach poniżej”.

#### Ustawienia Gemini {#descriptions-gemini}

W **Ustawienia Gemini** są dwie pozycje.

- **Klucz API Gemini** to klucz z Google AI Studio. Przycisk **Uzyskaj klucz** otwiera stronę, na której możesz go utworzyć.
- W ustawieniu **Model Gemini** domyślna opcja **Automatyczny** używa Gemini Flash. Gdy Flash jest zajęty, SandFly przechodzi na Flash-Lite. Możesz też wybrać inny model z listy dostępnej dla Twojego klucza. Gdy wybrany model jest zajęty lub zbyt długo nie odpowiada, SandFly używa Flash-Lite.

W bezpłatnym wariancie Gemini API Google może zachowywać otrzymane materiały i wykorzystywać je do ulepszania swoich produktów.

#### Dane przekazywane do usługi {#descriptions-privacy}

Wybrana usługa otrzymuje obrazy, zrzuty oraz pytania, które jej przekazujesz. Przed pierwszą wysyłką SandFly pokazuje adres usługi. Obowiązują zasady prywatności danego dostawcy. Z włączoną opcją **Używaj AI na urządzeniu** opisy nie wychodzą z telefonu.

### AI działające na urządzeniu {#descriptions-on-device}

Możesz pobrać model działający na telefonie. Nie potrzebujesz klucza API ani połączenia do tworzenia opisów po pobraniu. Lokalny model zwykle działa wolniej i zużywa więcej baterii. Wymaga procesora ARM 64-bit i odpowiedniej ilości pamięci. Nie działa na zegarkach.

Po włączeniu **Używaj AI na urządzeniu** model obsługuje opisy obrazów i ekranów, pytania, rozpoznawanie na żywo oraz tłumaczenie w trybie **Usługa AI**. Do szczegółowych opisów potrzebny jest także przełącznik **Szczegółowe opisy obrazów**.

#### Pobierz model {#descriptions-on-device-download}

1. Otwórz **Usługi AI** > **AI na urządzeniu**.
2. Wybierz **Model**, a następnie model z listy. Przy każdym jest rozmiar, ilość potrzebnej pamięci i krótki opis. Pozycje mogą mieć oznaczenia **pobrany** lub **eksperymentalny**. Lista pokazuje modele, na które telefon ma wystarczająco dużo pamięci, oraz modele już pobrane.
3. Wybierz **Pobierz model**. Przycisk podaje rozmiar pobierania. Jeśli korzystasz z danych mobilnych, pojawi się okno potwierdzenia.
4. Po zakończeniu pobierania włącz **Używaj AI na urządzeniu**.

Zalecany model to Gemma 4 E2B. Jeśli telefon ma wystarczająco dużo pamięci, jest to wybór domyślny. Na innych telefonach domyślny jest największy model, który się mieści. Modele eksperymentalne mogą działać gorzej.

Podczas pobierania pozycja pokazuje postęp i pozostały czas. SandFly odczytuje postęp po każdej ćwiartce, a na końcu mówi „Model pobrany”. Postęp znajdziesz też w powiadomieniu. Wybierz pozycję pobierania, żeby je zatrzymać. Jeśli rozpoczniesz ponownie, pobieranie ruszy od tego samego miejsca. SandFly sprawdzi plik i usunie go, jeśli nie odpowiada wybranemu modelowi.

Jeśli samodzielnie pobrałeś model Gemma 4, wybierz **Użyj pliku modelu z pamięci urządzenia** i wskaż plik `.litertlm`. SandFly odczyta „Model zainstalowany” albo poinformuje, że plik jest nieobsługiwany.

#### Ustawienia modelu na urządzeniu {#descriptions-on-device-settings}

- **Używaj AI na urządzeniu** włącza opisy za pomocą modelu na telefonie. Jest domyślnie wyłączone. Można je włączyć po pobraniu modelu.
- **Model** pozwala wybrać model, z którego chcesz korzystać.
- **Pobierz model** pobiera wybrany model. Pozycja jest widoczna, dopóki model nie jest pobrany.
- **Zarządzaj modelami** pokazuje pobrane modele, niedokończone pobrania oraz miejsce, które zajmują. Żeby zwolnić miejsce, wybierz model i użyj **Usuń**.
- **Użyj pliku modelu z pamięci urządzenia** instaluje model ze wskazanego pliku.
- **Limit czasu oczekiwania na odpowiedź** pozwala ustawić od 1 do 10 minut. Domyślnie są to 3 minuty. **Opisz ekran** potrzebuje więcej czasu niż **Opisz obraz**.
- **Krótsze opisy ekranu** ogranicza zakres odpowiedzi polecenia **Opisz ekran**, dzięki czemu wynik pojawia się wcześniej. Opcja jest domyślnie wyłączona.
- W **Procesor** możesz wybrać **CPU**, **GPU** lub **NPU**. Ta pozycja pojawia się tylko na telefonie, na którym model może działać na GPU lub NPU. Po wybraniu **NPU** SandFly używa modelu przygotowanego dla danego układu. Ustawienia **Model** nie można wtedy zmienić.

Podczas ładowania i działania modelu SandFly odtwarza powtarzające się dźwięki. Możesz je wyłączyć w **Dźwięk i wibracje** > **Poszczególne dźwięki i wibracje**. Zobacz [](#sound).

#### Pamięć modelu {#descriptions-on-device-memory}

Model startuje przy pierwszym żądaniu i działa w osobnym procesie. Jeśli brakuje wolnej pamięci lub Android zatrzyma model z jej braku, SandFly poda przyczynę. Zamknij część aplikacji albo wybierz mniejszy model.

Opcja **Ignoruj limity pamięci RAM dla AI na urządzeniu** w **Ustawienia programisty** pokazuje wszystkie modele i pozwala uruchomić pobrany model bez sprawdzania wolnego miejsca w RAM.

### Ustawienia wspólne dla modeli {#descriptions-model-settings}

Te ustawienia znajdziesz w **Usługi AI** oraz w **Pisanie** > **Dyktowanie**. Obejmują lokalne modele AI i modele rozpoznawania mowy do dyktowania.

- **Utrzymuj modele rozpoznawania w pamięci** określa, jak długo modele pozostają w pamięci po użyciu. Możesz wybrać domyślne **Zwolnij po użyciu**, **5 minut**, **15 minut**, **30 minut** lub **Do zatrzymania SandFly**. Lokalny model AI pozostaje w pamięci przez co najmniej 2 minuty po każdym żądaniu. Jeśli wybierzesz **Do zatrzymania SandFly**, model AI ładuje się już przy uruchomieniu SandFly.
- W **Serwer pobierania modeli** wybierz domyślne **Hugging Face** lub **hf-mirror.com**, jeśli Hugging Face jest niedostępne.

### Rozpoznawanie na żywo {#descriptions-live-recognition}

Ekran **Rozpoznawanie na żywo** opisuje to, co widzi tylny aparat. Otwórz **Opisy obrazów i ekranu** > **Rozpoznawanie na żywo** albo użyj opcji sterowania czytaniem lub poleceń **Rozpoznawanie na żywo**. Funkcja jest niedostępna na urządzeniach bez aparatu i na zegarkach oraz telewizorach.

Każdy tryb możesz włączyć lub wyłączyć osobnym polem wyboru.

- **Sceny** opisują to, co widzi aparat.
- **Tekst** odczytuje napisy widoczne w kadrze. SandFly rozpoznaje tekst na telefonie.
- **Wskaż i czytaj** odczytuje tekst wskazany palcem w kadrze. Wybierz domyślne **Czytaj nad opuszkiem palca** lub **Czytaj pod opuszkiem palca**.
- **Wykrywanie osób** opisuje osoby widoczne w kadrze.
- **Wykrywanie drzwi** opisuje drzwi widoczne w kadrze.
- **Wykrywanie kolorów** podaje kolory w środku kadru, np. „Ciemnoniebieski”, „Jasnoszary” lub „Czerwony i biały”, albo odczytuje „Mieszane kolory”, gdy żaden kolor nie zajmuje wystarczającej części kadru. Kolor jest odczytywany, gdy się zmieni i utrzyma przez około pół sekundy. Jasność i ciemność zależą od oświetlenia i automatycznej ekspozycji aparatu. SandFly rozpoznaje kolory na telefonie.
- **Poziom światła** odczytuje poziom z czujnika, od **Ciemno** przez **Słabe światło**, **Normalne światło** i **Jasne światło** po **Bardzo jasne światło**. Ten tryb nie używa aparatu.
- **Ton poziomu światła** odtwarza ciągły dźwięk, którego wysokość rośnie z jasnością obrazu z tylnego aparatu, od 200 Hz w ciemności do 1600 Hz w pełnym słońcu. Dźwięk ma głośność dźwięków SandFly i cichnie po wyłączeniu trybu lub opuszczeniu ekranu.

W wierszu stanu zobaczysz **Aparat włączony** lub **Aparat wyłączony**. Gdy po raz pierwszy uruchomisz tryb korzystający z aparatu, pojawi się systemowa prośba o uprawnienie.

Opisy mogą zawierać błędy. Nie polegaj na nich podczas przemieszczania się. Opisy osób i drzwi powstają tylko na podstawie obrazu i nie zawierają informacji o odległości.

#### Steruj rozpoznawaniem na żywo {#descriptions-live-recognition-use}

1. Wybierz **Rozpoznawanie na żywo** w sterowaniu czytaniem. Ta opcja wymaga Androida 11 lub nowszego.
2. Przesuń palcem w górę albo w dół. Usłyszysz nazwę trybu i informację, czy jest włączony.
3. Stuknij dwa razy, żeby zmienić stan trybu. Jeśli ekran rozpoznawania jest zamknięty, otworzy się przy tym geście.

Gdy ekran rozpoznawania jest otwarty, możesz wykonać dwa gesty.

- Stuknij dwoma palcami, żeby przerwać mowę i odczytać następny wynik.
- [Przesuń szybko dwa palce tam i z powrotem](#gestures-scrub-pinch-spread), żeby wyłączyć wszystkie tryby i zamknąć ekran.

SandFly odczytuje wyniki własnym głosem, również podczas odtwarzania innego dźwięku. Rozpoznawanie nie używa mikrofonu. Aparat wyłączy się po opuszczeniu ekranu, zablokowaniu telefonu, wstrzymaniu SandFly lub wyłączeniu wszystkich trybów.

#### Skąd pochodzą opisy z aparatu {#descriptions-live-recognition-ai}

**Sceny**, **Wskaż i czytaj**, **Wykrywanie osób** i **Wykrywanie drzwi** korzystają z tego samego źródła opisów co **Opisz obraz**.

- Przy włączonej opcji **Używaj AI na urządzeniu** kadry przetwarza model w telefonie. Dane nie opuszczają telefonu.
- W pozostałych przypadkach bieżąca usługa AI, na przykład SandFly Pro lub Claude, otrzymuje obraz z aparatu co kilka sekund.
- Jeśli wybraną usługą jest Gemini, obrazy trafiają co kilka sekund do Gemini Live API. SandFly odczytuje otrzymany tekst.

Przed wysłaniem pierwszego obrazu pojawi się okno **Wysyłać obrazy z aparatu do Gemini?**. Przy innej usłudze okno pokaże jej adres, na przykład api.anthropic.com. Żeby wyrazić zgodę, wybierz **Wyślij obrazy**. Jeśli usługa odrzuci klucz API, skończy się limit lub błędy się powtarzają, tryby korzystające z AI zostaną wyłączone. SandFly poinformuje o tym głosem.

### Komunikaty o błędach {#descriptions-errors}

Jeśli nie uda się uzyskać opisu lub odpowiedzi, SandFly odczyta przyczynę.

- Usługa odrzuciła klucz API. Sprawdź klucz na ekranie **Usługi AI**.
- Wykorzystano limit usługi lub klucza API.
- Usługa jest zajęta albo nie odpowiedziała na czas.
- Brakuje połączenia z internetem.
- „Nie jest używana żadna usługa AI. Skonfiguruj lub wybierz jedną w sekcji „Usługi AI”.”

W przypadku innych błędów SandFly odczyta kod i pierwsze zdanie komunikatu usługi po angielsku.
