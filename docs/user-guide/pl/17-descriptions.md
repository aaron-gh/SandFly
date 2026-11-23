## Opisy obrazów i ekranu {#descriptions}

SandFly może opisać obraz, ekran lub widok z tylnego aparatu i odpowiedzieć na pytania o to, co na nich jest. Do opisu wykorzystuje [usługę AI z własnym kluczem](#descriptions-ai-services), [SandFly Pro](#pro) albo [model na urządzeniu](#descriptions-on-device). Sam tekst z obrazów rozpoznaje na urządzeniu, niezależnie od tych usług.

Otwórz **Opisy obrazów i ekranu**, żeby włączyć opisy lub rozpoznawanie na żywo. Źródło wybierzesz na ekranie **Usługi AI**. Te funkcje są dostępne na telefonach i tabletach.

### Pierwsze uruchomienie opisów {#descriptions-set-up}

1. Skonfiguruj [usługę AI](#descriptions-set-up-service), [SandFly Pro](#pro) lub [model na urządzeniu](#descriptions-on-device-download).
2. W ustawieniach otwórz **Opisy obrazów i ekranu** i włącz **Szczegółowe opisy obrazów**.
3. Przeczytaj informację o tym, jakie dane i do kogo trafią. Wybierz **Włącz**.

Polecenie **Opisz obraz** przy wyłączonych szczegółowych opisach też otwiera to pytanie. **Włącz** uruchamia opis, a **Nie, dziękuję** pozostawia odczyt samego tekstu. Przy pierwszym użyciu **Opisz ekran** SandFly osobno zapyta o zrzut ekranu.

### Ustawienia opisów {#descriptions-settings}

- **Szczegółowe opisy obrazów** pozwalają opisać obraz za pomocą wybranej usługi lub modelu. Domyślnie są wyłączone.
- **Automatyczne opisy tekstu na obrazach** odczytują tekst po przejściu na obraz. Wybierz **Wszystkie obrazy**, **Tylko obrazy bez etykiet** (domyślnie) lub **Nie opisuj obrazów**. Rozpoznawanie tekstu odbywa się na urządzeniu.
- **Rozpoznawanie na żywo** otwiera [ekran rozpoznawania](#descriptions-live-recognition). Opcja jest dostępna na urządzeniach z aparatem.

### Opisywanie obrazu {#descriptions-describe-image}

Przenieś fokus na obraz i użyj [**Opisz obraz**](#descriptions-commands) z menu SandFly. Otworzy się **Opis obrazu** z opisem i rozpoznanym tekstem. Możesz skopiować wynik, ocenić go lub [zadać pytanie](#descriptions-questions).

Użyj **Odczytaj opis obrazu**, jeśli chcesz usłyszeć wynik bez otwierania okna. Jeśli szczegółowy opis się nie powiedzie, SandFly spróbuje odczytać tekst z obrazu i poda przyczynę.

### Opisywanie ekranu {#descriptions-describe-screen}

Użyj **Opisz ekran**, żeby zrobić zrzut i uzyskać opis w oknie **Opis ekranu**. Do wybranego modelu trafia zrzut wraz z listą elementów. Jeśli wystarczy Ci wynik głosowy, użyj **Odczytaj opis ekranu**.

Polecenie działa w obsługiwanych językach urządzenia. Android blokuje zrzuty zabezpieczonych okien, na przykład niektórych ekranów aplikacji bankowych.

### Pytania o obraz lub ekran {#descriptions-questions}

W oknie opisu użyj **Zapytaj głosem** lub **Wpisz pytanie**. Nagrywanie używa usługi z ustawień [**Pisanie** > **Dyktowanie**](#typing). Możesz zakończyć nagranie przyciskiem **Przestań słuchać**. Przy wpisywaniu użyj **Zapytaj**. Odpowiedzi zostają w oknie, więc możesz zadawać kolejne pytania o ten sam obraz lub ekran.

Żeby od razu zadać pytanie, użyj **Zapytaj o obraz** albo **Zapytaj o ekran**. W pierwszym przypadku najpierw przenieś fokus na obraz. SandFly zrobi zrzut i włączy nagrywanie. Po przerwie w mówieniu lub ponownym użyciu polecenia zakończy nagrywanie i odczyta odpowiedź. Jeśli mikrofon się nie uruchomi, pojawi się pole tekstowe.

Pytania trafiają do tej samej usługi lub modelu co opis. Przed pierwszą zgodą na przesyłanie zrzutów polecenia otwierają zwykły ekran opisu, w którym możesz wyrazić zgodę.

### Polecenia dotyczące opisów {#descriptions-commands}

W kategorii **Opisy obrazów i ekranu** możesz [przypisać do gestu lub klawisza](#commands) polecenia opisu, odczytu i zadawania pytań. Znajdziesz tam również:

- **Zrób zdjęcie i opisz**. [Robi zdjęcie tylnym aparatem](#descriptions-take-picture).
- **Udostępnij obraz zaznaczonego elementu** i **Udostępnij zrzut ekranu**. Otwierają systemowy ekran udostępniania. Wymagają Androida 11. Zabezpieczone okna są czarne na zrzucie.
- **Zgłoś odpowiedź AI**. Otwiera [zgłoszenie problemu](#help) z ostatnią odpowiedzią lub opisem modelu.
- Polecenia [rozpoznawania ekranu](#pro-screen-recognition), jeśli masz SandFly Pro.

Kategoria **Rozpoznawanie na żywo** ma polecenia dla poszczególnych trybów oraz **Zatrzymaj rozpoznawanie na żywo**. Zobacz także [gesty](#appendix-gestures) i [skróty klawiszowe](#appendix-keyboard-shortcuts).

#### Czynności AI podczas czytania {#descriptions-ai-actions}

W Androidzie 11 i nowszym możesz wybrać opcję **Czynności AI**. Przesuń palec w górę lub w dół, żeby usłyszeć działanie. Stuknij dwukrotnie, żeby je wykonać. Dopóki fokus się nie zmieni, dwukrotne stuknięcie wykona ostatnio odczytaną czynność.

Dostępne są opisy, odczyt opisu, pytania i udostępnianie, a w SandFly Pro także rozpoczęcie lub zatrzymanie zadania agenta i wpisanie zadania. Otwórz **Sterowanie czytaniem** > **Uwzględniane czynności AI**, żeby ukryć te, których nie używasz. Domyślnie lista obejmuje wszystkie dostępne czynności.

### Zdjęcie z opisem {#descriptions-take-picture}

Użyj **Zrób zdjęcie i opisz** i przytrzymaj telefon nieruchomo po komunikacie. SandFly użyje tylnego aparatu i otworzy opis, w którym możesz zadać pytania. Aby powtórzyć, wybierz **Zrób kolejne zdjęcie**. Przy pierwszym użyciu potrzebna jest zgoda na dostęp do aparatu.

### Usługi AI {#descriptions-ai-services}

Otwórz **Usługi AI** w ustawieniach. Wybrana usługa obsługuje opisy i pytania, rozpoznawanie na żywo, tłumaczenia w trybie **Usługa AI** i inne funkcje korzystające z AI.

Lista **Usługa AI** pokazuje skonfigurowane pozycje i pozwala wyłączyć usługę. Osobne ekrany ustawień mają między innymi Gemini, Claude, OpenAI (ChatGPT), Mistral, Groq, OpenRouter, Ollama Cloud, Qwen, Zhipu GLM, Baichuan, DeepSeek, Kimi, Hunyuan, SiliconFlow i **Inna usługa**. W obsługiwanych wydaniach jest też **SandFly Pro**. SandFly wskazuje usługę używaną i pozycje, które wciąż trzeba skonfigurować.

Na tym samym ekranie znajdują się [AI na urządzeniu](#descriptions-on-device) i [ustawienia modeli](#descriptions-model-settings).

#### Konfigurowanie usługi {#descriptions-set-up-service}

1. Otwórz **Usługi AI** i wybierz ustawienia dostawcy.
2. Jeśli wybierasz **Inna usługa**, podaj **Adres** zaczynający się od `https://`. Usługa musi udostępniać API zgodne z OpenAI Chat i przyjmować obrazy.
3. Wybierz pole klucza API. **Uzyskaj klucz** prowadzi do strony dostawcy, jeśli udostępnia taką możliwość. Wklej klucz i wybierz **Zapisz**.
4. Otwórz wybór modelu. Użyj listy pobranej dla klucza albo **Wprowadź nazwę modelu**. Jeśli pobranie listy się nie powiedzie, SandFly poda przyczynę i pozwoli wpisać model.

Do działania potrzebny jest adres, klucz i model. Jeśli jeszcze nie używasz żadnej usługi, SandFly od razu wybierze skonfigurowaną. Żeby usunąć klucz, wyczyść jego pole i zapisz. Jeśli używana usługa utraci klucz lub model, SandFly przejdzie na następną skonfigurowaną albo wyłączy opisy AI.

Klucze zostają na telefonie. SandFly nie umieszcza ich w [eksportowanych ustawieniach](#profiles).

#### Wybór i wyłączanie usługi {#descriptions-choose-service}

Otwórz **Usługi AI** > **Usługa AI** i wybierz jedną z gotowych usług. Aby wyłączyć usługi sieciowe, wybierz **Brak**. Jeśli lista jest pusta, najpierw skonfiguruj wybraną usługę w ustawieniach poniżej.

#### Ustawienia Gemini {#descriptions-gemini}

Otwórz **Ustawienia Gemini** i podaj **Klucz API Gemini** z Google AI Studio. Możesz dotrzeć do niego przez **Uzyskaj klucz**.

Domyślny wybór w **Model Gemini** używa Gemini Flash i przechodzi na Flash-Lite, gdy Flash jest zajęty. Możesz też wybrać model z listy dostępnej dla klucza. Jeśli wybrany model jest zajęty lub zbyt wolno odpowiada, SandFly spróbuje Flash-Lite.

W bezpłatnym wariancie Gemini Google może zachowywać otrzymane materiały i wykorzystywać je do doskonalenia swoich produktów.

#### Dane przekazywane do usługi {#descriptions-privacy}

Wybrana usługa otrzymuje obrazy, zrzuty oraz pytania, które jej przekazujesz. Przed pierwszą wysyłką SandFly pokazuje adres usługi. Obowiązują zasady prywatności danego dostawcy. Z włączoną opcją **Używaj AI na urządzeniu** opisy nie wychodzą z telefonu.

### AI działające na urządzeniu {#descriptions-on-device}

Możesz pobrać model działający na telefonie. Nie potrzebujesz klucza API ani połączenia do tworzenia opisów po pobraniu. Lokalny model zwykle działa wolniej i zużywa więcej baterii. Wymaga procesora ARM 64-bit i odpowiedniej ilości pamięci. Nie działa na zegarkach.

Po włączeniu **Używaj AI na urządzeniu** model obsługuje opisy obrazów i ekranów, pytania, rozpoznawanie na żywo oraz tłumaczenie w trybie **Usługa AI**. Do szczegółowych opisów potrzebny jest także przełącznik **Szczegółowe opisy obrazów**.

#### Pobranie modelu {#descriptions-on-device-download}

1. Otwórz **Usługi AI** > **AI na urządzeniu** > **Model**.
2. Wybierz z listy. Każdy wpis podaje rozmiar, wymagania pamięci i krótki opis. Lista obejmuje pasujące i już pobrane modele. SandFly oznacza modele eksperymentalne.
3. Wybierz **Pobierz model**. SandFly poda rozmiar i przy danych mobilnych zapyta o zgodę.
4. Po pobraniu włącz **Używaj AI na urządzeniu**.

Jeśli telefon ma dość pamięci, domyślnym wyborem jest Gemma 4 E2B. Na pozostałych telefonach SandFly proponuje największy model, który się zmieści. Modele eksperymentalne mogą działać gorzej.

SandFly pokazuje postęp i pozostały czas. Czyta kolejne ćwiartki i informuje o zakończeniu. Możesz zatrzymać pobieranie i je wznowić. SandFly sprawdzi pobrany plik i usunie go, jeśli nie zgadza się z wybranym modelem.

Jeśli masz plik Gemma 4 z rozszerzeniem `.litertlm`, użyj **Użyj pliku modelu z pamięci urządzenia**. SandFly potwierdzi instalację lub zgłosi nieobsługiwany plik.

#### Ustawienia modelu lokalnego {#descriptions-on-device-settings}

- **Używaj AI na urządzeniu** jest domyślnie wyłączone i pojawia się po pobraniu modelu.
- **Model**, **Pobierz model** i **Użyj pliku modelu z pamięci urządzenia** pozwalają wybrać lub dodać model.
- **Zarządzaj modelami** pokazuje zajęte miejsce, także przez nieukończone pobrania. Użyj **Usuń**, żeby je odzyskać.
- **Limit czasu oczekiwania na odpowiedź** może wynosić od minuty do 10 minut. Domyślnie są to 3 minuty. Opis ekranu zwykle trwa dłużej niż opis obrazu.
- **Krótsze opisy ekranu** przyspieszają odpowiedź przez ograniczenie jej zakresu. Domyślnie są wyłączone.
- **Procesor** pozwala wybrać **CPU**, **GPU** lub **NPU**, jeśli urządzenie obsługuje przyspieszenie. Przy NPU SandFly używa modelu przeznaczonego dla tego układu i blokuje zmianę modelu.

SandFly powtarza dźwięk podczas ładowania i pracy modelu. Możesz go wyłączyć w **Dźwięk i wibracje** > **Poszczególne dźwięki i wibracje**. Zobacz [ustawienia dźwięku](#sound).

#### Pamięć modelu {#descriptions-on-device-memory}

Model startuje przy pierwszym żądaniu i działa w osobnym procesie. Jeśli brakuje wolnej pamięci lub Android zatrzyma model z jej braku, SandFly poda przyczynę. Zamknij część aplikacji albo wybierz mniejszy model.

Opcja **Ignoruj limity pamięci RAM dla AI na urządzeniu** w **Ustawienia programisty** pokazuje wszystkie modele i pozwala uruchomić pobrany model bez sprawdzania wolnego miejsca w RAM.

### Ustawienia wspólne dla modeli {#descriptions-model-settings}

Te dwie opcje znajdziesz w **Usługi AI** oraz **Pisanie** > **Dyktowanie**. Dotyczą zarówno lokalnych opisów AI, jak i modeli rozpoznawania mowy:

- **Utrzymuj modele rozpoznawania w pamięci** pozwala wybrać **Zwolnij po użyciu**, 5, 15 lub 30 minut albo **Do zatrzymania SandFly**. Lokalny model AI i tak pozostaje w pamięci co najmniej 2 minuty. Jeśli wybierzesz ostatnią opcję, ładuje się przy uruchomieniu SandFly.
- **Serwer pobierania modeli** pozwala wybrać domyślny **Hugging Face** lub **hf-mirror.com**, jeśli główny serwer jest niedostępny.

### Rozpoznawanie na żywo {#descriptions-live-recognition}

Otwórz **Opisy obrazów i ekranu** > **Rozpoznawanie na żywo** lub użyj polecenia albo opcji sterowania czytaniem o tej nazwie. Funkcja jest dostępna na telefonach i tabletach z aparatem.

Każdy tryb ma osobny przełącznik:

- **Sceny** opisują to, co widzi aparat.
- **Tekst** czyta napisy rozpoznawane na urządzeniu.
- **Wskaż i czytaj** odczytuje tekst przy opuszku w obrazie z aparatu. Wybierz **Czytaj nad opuszkiem palca** albo **Czytaj pod opuszkiem palca**. Domyślna jest pierwsza opcja.
- **Wykrywanie osób** i **Wykrywanie drzwi** opisują osoby i drzwi widoczne w kadrze.
- **Poziom światła** podaje wskazanie czujnika od **Ciemno** do **Bardzo jasne światło**. Nie włącza aparatu.

Stan **Aparat włączony** lub **Aparat wyłączony** powie, czy aparat działa. Przy pierwszym użyciu trybu z aparatem potrzebna jest zgoda. Opisy mogą zawierać błędy. Nie używaj ich do bezpiecznego poruszania się. SandFly nie podaje odległości od osób ani drzwi.

#### Sterowanie rozpoznawaniem na żywo {#descriptions-live-recognition-use}

1. Wybierz **Rozpoznawanie na żywo** w sterowaniu czytaniem (Android 11 lub nowszy).
2. Przesuń palec w górę albo w dół, żeby wybrać tryb i sprawdzić jego stan.
3. Stuknij dwukrotnie, żeby zmienić stan. SandFly otworzy ekran rozpoznawania, jeśli jeszcze jest zamknięty.

Na tym ekranie stuknięcie dwoma palcami przerywa mowę i czyta następny wynik. Żeby wyłączyć wszystkie tryby i zamknąć ekran, [przesuń szybko dwa palce tam i z powrotem](#gestures-scrub-pinch-spread).

Wyniki czyta głos SandFly także przy innym dźwięku. Rozpoznawanie nie używa mikrofonu. Aparat wyłącza się po opuszczeniu ekranu, zablokowaniu telefonu, wstrzymaniu SandFly lub wyłączeniu wszystkich trybów.

#### Skąd pochodzą opisy z aparatu {#descriptions-live-recognition-ai}

**Sceny**, **Wskaż i czytaj**, **Wykrywanie osób** i **Wykrywanie drzwi** korzystają z tego samego wyboru AI co **Opisz obraz**. Model lokalny dostaje obraz na urządzeniu. Przy usłudze sieciowej obraz jest wysyłany co kilka sekund. Jeżeli wybierzesz Gemini, SandFly użyje Gemini Live API i przeczyta tekst odpowiedzi.

Przed pierwszą wysyłką SandFly pokaże nazwę lub adres usługi. Zatwierdź przez **Wyślij obrazy**. Odrzucony klucz, wykorzystany limit lub powtarzające się błędy wyłączają tryby z AI. SandFly poinformuje o tym głosem.

### Komunikaty o błędach {#descriptions-errors}

SandFly podaje przyczynę, gdy opis lub pytanie się nie powiedzie. Najczęściej jest to problem z kluczem, wykorzystany limit, brak połączenia, przeciążenie usługi albo zbyt długie oczekiwanie. Jeśli żadna usługa nie działa, wybierz ją lub skonfiguruj w **Usługi AI**. Przy innych błędach usłyszysz kod i pierwsze zdanie odpowiedzi dostawcy po angielsku.
