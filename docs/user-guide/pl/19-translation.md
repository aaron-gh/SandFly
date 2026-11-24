## Tłumaczenie {#translation}

SandFly może przetłumaczyć ostatnią wypowiedź lub zawartość pola tekstowego. Po włączeniu automatycznego tłumaczenia może też tłumaczyć wszystkie komunikaty i powiadomienia w innych językach. Tekst może być tłumaczony na telefonie albo przez wybraną usługę tłumaczenia lub usługę AI. Wszystkie opcje są na ekranie **Tłumaczenie**.

### Przetłumacz ostatnią wypowiedź {#translation-last-phrase}

Polecenie **Przetłumacz ostatnią wypowiedź** tłumaczy to, co SandFly przeczytał przed chwilą, i odczytuje wynik. Jeśli fokus jest w polu tekstowym, polecenie działa na jego zawartości. Pola haseł są pomijane.

Znajdziesz je na ekranie **Polecenia** w grupie **Czytanie**. Domyślnie nie ma przypisanego gestu. Na tym samym ekranie przypiszesz gest lub skrót klawiszowy. Zobacz [](#commands).

1. Przejdź do elementu lub zaczekaj, aż SandFly coś odczyta.
2. Użyj polecenia **Przetłumacz ostatnią wypowiedź**.

SandFly odczytuje tłumaczenie. Jeśli tekst jest już w języku docelowym, usłyszysz „Już w języku” i jego nazwę. Jeśli nie ma tekstu, usłyszysz „Brak tekstu do przetłumaczenia”.

Jeśli od razu ponowisz polecenie, SandFly jeszcze raz tłumaczy tekst źródłowy. Dzięki temu możesz w międzyczasie zmienić język. Dopóki tekst i ustawienia pozostają takie same, SandFly używa zapisanego wyniku i ponowne wywołanie nie wysyła tekstu do usługi.

Wynik pojawia się w historii mowy. Możesz go powtórzyć, przeliterować lub skopiować poleceniami dotyczącymi ostatniej wypowiedzi. Dłuższe teksty są obcinane do 5000 znaków.

### Wybierz usługę tłumaczenia {#translation-service}

1. Otwórz **Tłumaczenie** > **Usługa tłumaczenia**.
2. Wybierz usługę.

Masz do wyboru następujące usługi.

- **Na urządzeniu** korzysta z języków pobranych na telefon. To domyślna opcja na telefonach. Jest dostępna tylko na telefonach. Zobacz [](#translation-on-device).
- **Usługa AI** używa lokalnego modelu, jeśli włączono **Używaj AI na urządzeniu**. W przeciwnym razie korzysta z aktywnej usługi wybranej na ekranie **Usługi AI**, np. SandFly Pro, Gemini lub Claude. To domyślna opcja na zegarkach. Zobacz [](#descriptions-ai-services).
- **Google Cloud Translation**, **DeepL** i **Microsoft Translator** wymagają klucza API z wybranej usługi.
- **LibreTranslate** wymaga adresu serwera, np. `https://libretranslate.com` albo własnego serwera. Jeśli serwer tego wymaga, potrzebny jest również klucz API.

Po wybraniu usługi wymagającej klucza API lub adresu pole do wpisania danych otwiera się od razu. Na ekranie **Tłumaczenie** pojawia się też osobna grupa ustawień tej usługi, np. **Ustawienia DeepL**.

- W polu klucza API, np. **Klucz API DeepL**, wklej klucz i wybierz **Zapisz**. Aby go usunąć, wyczyść pole i wybierz **Zapisz**.
- W polu **Adres** wpisz adres serwera LibreTranslate. Musi zaczynać się od `https://`.
- W polu **Region** dla Microsoft Translator podaj region zasobu Translator w Azure, np. `westeurope`. Jeśli korzystasz z zasobu globalnego (**Globalny**), pozostaw pole puste.

Przy nazwie usługi, która nie jest gotowa do pracy, pojawia się „nie skonfigurowano”. Tekst do tłumaczenia trafia do wybranej usługi, chyba że ustawiono **Na urządzeniu** albo **Usługa AI** z włączonym AI na urządzeniu. Klucze API pozostają na telefonie i są pomijane przy eksportowaniu ustawień.

### Wybierz języki {#translation-languages}

- W opcji **Tłumacz z** wybierz język lub pozostaw domyślne **Wykryj język**.
- W opcji **Tłumacz na** wybierz język docelowy lub pozostaw domyślny język urządzenia.
- Opcja **Tekst w języku docelowym tłumacz na** pozwala wybrać drugi język dla tekstu, który jest już w języku docelowym. Jeśli znasz np. niemiecki i angielski, a zwykle tłumaczysz na angielski, możesz tu wybrać niemiecki. Domyślne ustawienie to **Nie tłumacz**. Opcja jest widoczna, gdy w polu **Tłumacz z** ustawiono **Wykryj język**.
- **Zamień języki** zamienia wartości w polach **Tłumacz z** i **Tłumacz na**. SandFly odczytuje nową parę. Opcja jest widoczna tylko wtedy, gdy w polu **Tłumacz z** wybrano konkretny język.

Po wybraniu usługi **Na urządzeniu** listy zawierają języki dostępne w tłumaczeniu lokalnym. Język jest wykrywany na telefonie. Przy pierwszym użyciu możesz usłyszeć „Trwa pobieranie wykrywania języka. Spróbuj ponownie za chwilę.”.

### Języki na urządzeniu {#translation-on-device}

Jeśli w ustawieniach tłumaczenia ręcznego lub automatycznego wybrano **Na urządzeniu**, pojawia się grupa ustawień tłumaczenia na urządzeniu z trzema opcjami.

- **Języki na urządzeniu** pokazują listę języków i informują, które są już pobrane. Wybierz niepobrany język, aby pobrać około 30 MB danych. Wybierz pobrany język, aby go usunąć.
- **Pobierz wszystkie języki** po wyświetleniu ich liczby i rozmiaru pobiera po kolei wszystkie brakujące języki. W trakcie pobierania pozycja pokazuje liczbę gotowych języków. Jeśli ekran **Tłumaczenie** jest otwarty, SandFly oznajmia ukończenie kolejnych 25 procent. Pobieranie trwa także po wyjściu z ekranu. Aby je zatrzymać, wybierz tę opcję ponownie.
- **Pobieraj języki tylko przez Wi-Fi** ogranicza pobieranie do Wi-Fi. Domyślnie opcja jest wyłączona.

Jeśli przy tłumaczeniu potrzebujesz języka, którego jeszcze nie ma na telefonie, SandFly zaczyna go pobierać razem z angielskim. Angielski służy jako język pośredni przy tłumaczeniu między innymi językami. SandFly odczytuje nazwy pobieranych języków.

### Automatyczne tłumaczenie {#translation-automatic}

W grupie **Automatyczne tłumaczenie** znajdziesz poniższe opcje.

- **Automatyczne tłumaczenie** tłumaczy mowę przed jej odczytaniem, także w powiadomieniach, jeśli tekst jest w innym języku niż ustawiony w opcji **Tłumacz na**. Domyślnie opcja jest wyłączona.
- **Tłumacz powiadomienia** tłumaczy tylko powiadomienia i działa, gdy **Automatyczne tłumaczenie** jest wyłączone. Domyślnie ta opcja też jest wyłączona.
- W opcji **Usługa automatycznego tłumaczenia** możesz wybrać **Na urządzeniu**, **Google Cloud Translation**, **DeepL**, **Microsoft Translator** lub **LibreTranslate**. Domyślnie używana jest usługa tłumaczenia ręcznego. Jeśli jest nią **Usługa AI**, tłumaczenie automatyczne korzysta domyślnie z usługi **Na urządzeniu**.
- Na liście **Języki bez tłumaczenia** możesz wskazać języki, które SandFly ma czytać w oryginale, np. języki, które rozumiesz. Domyślnie lista jest pusta.

Dwie ostatnie opcje są dostępne, gdy włączono **Automatyczne tłumaczenie** lub **Tłumacz powiadomienia**.

Aby przełączać automatyczne tłumaczenie bez wchodzenia w ustawienia, użyj elementu **Automatyczne tłumaczenie** w sterowaniu czytaniem. Domyślnie jest ukryty. Włącz go na ekranie **Sterowanie czytaniem**. Zobacz [](#commands).

#### Jak działa automatyczne tłumaczenie {#translation-automatic-details}

SandFly wykrywa język każdej wypowiedzi na telefonie. Tekst zostaje odczytany w oryginale i pozostaje na telefonie, jeśli jest już w języku docelowym albo w jednym z języków z listy **Języki bez tłumaczenia**. Gdy w opcji **Tłumacz z** wybierzesz konkretny język, tekst w każdym innym języku również pozostaje w oryginale. Wykrywanie języka pomija teksty złożone z mniej niż trzech liter.

Dźwięki i wibracje są odtwarzane od razu. Mowa zaczyna się, gdy tłumaczenie jest gotowe. Jeśli tłumaczenie trwa dłużej niż trzy sekundy albo się nie powiedzie, SandFly odczytuje oryginał. Ostatnie wyniki są zapamiętywane, więc powrót do tego samego elementu nie uruchamia ponownego tłumaczenia.

Jeśli wybrano **Na urządzeniu**, SandFly zacznie pobierać brakujący język, gdy tylko go rozpozna. W trakcie pobierania czyta oryginał. Gdy język będzie gotowy, ponownie przeczyta aktualny element, tym razem po przetłumaczeniu, o ile nadal na nim jesteś. Powiadomienie może czekać na pobranie języka do 30 sekund.

Powiadomienia są tłumaczone przed dodaniem do kolejki, dzięki czemu zachowują kolejność. Powiadomienie czytane z profilem głosu zachowuje głos z tego profilu. Powiadomienia prywatne są czytane w oryginale, a ich tekst pozostaje na telefonie. Zobacz [](#notifications).

### Jak SandFly odczytuje tłumaczenia {#translation-speech}

W grupie **Mowa** są trzy opcje dotyczące tłumaczeń.

- **Używaj głosu dla języka docelowego** przypisuje tłumaczeniu informację o języku, aby SandFly mógł dobrać głos tak jak przy innym tekście w tym języku. Zastosowanie mają ustawienia języka w menu **Mowa**. Opcja jest domyślnie włączona. Zobacz [](#speech).
- **Podawaj język oryginału** sprawia, że przed tłumaczeniem usłyszysz nazwę języka źródłowego, np. „Język oryginału: niemiecki”. Domyślnie opcja jest wyłączona.
- **Kopiuj tłumaczenia do schowka** zapisuje każde tłumaczenie do schowka. Domyślnie opcja jest wyłączona.

### Komunikaty o błędach tłumaczenia {#translation-errors}

Jeśli nie uda się przetłumaczyć tekstu, SandFly podaje przyczynę.

- Brakuje klucza API lub adresu LibreTranslate. Możesz np. usłyszeć „Wpisz klucz API usługi DeepL w ustawieniach tłumaczenia.”.
- Usługa nie zaakceptowała klucza API lub wyczerpano limit korzystania z usługi.
- Nie można połączyć się z usługą.
- Usługa nie obsługuje tego języka.
- Nie wykryto języka.
- Języki do tłumaczenia na urządzeniu jeszcze się pobierają albo nie udało się ich pobrać.

W pozostałych przypadkach SandFly informuje, że usługa nie przetłumaczyła tekstu, i odczytuje jej komunikat.
