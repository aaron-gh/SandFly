## Tłumaczenie {#translation}

SandFly może przetłumaczyć ostatnią wypowiedź, zawartość pola tekstowego albo automatycznie tłumaczyć mowę i powiadomienia w innym języku. W ustawieniach **Tłumaczenie** wybierzesz języki oraz tłumaczenie na urządzeniu lub przez usługę.

### Tłumaczenie ostatniej wypowiedzi {#translation-last-phrase}

Użyj **Przetłumacz ostatnią wypowiedź**. Polecenie przeczyta przekład tego, co SandFly powiedział przed chwilą. Jeśli fokus jest w polu tekstowym, użyje zawartości pola. Pola haseł są pomijane.

Polecenie znajdziesz w kategorii **Czytanie**. Początkowo nie ma przypisanego gestu. Możesz [wybrać gest lub klawisz](#commands).

SandFly informuje, kiedy nie ma tekstu lub tekst jest już w języku docelowym. Ponowne użycie tłumaczy ten sam oryginał, więc możesz wcześniej zmienić język. Wynik jest zapamiętany, dopóki nie zmieni się tekst lub ustawienie. Dzięki temu kolejna próba nie wysyła ponownie tych samych danych.

Przekład trafia do historii mowy. Możesz go powtórzyć, przeliterować lub skopiować tak samo jak inną wypowiedź. Przy długim tekście tłumaczenie obejmuje do 5000 znaków.

### Wybór usługi {#translation-service}

Otwórz **Tłumaczenie** > **Usługa tłumaczenia**. Dostępne są:

- **Na urządzeniu**. Domyślny wybór na telefonie. Korzysta z [pobranych języków](#translation-on-device). Nie działa na zegarku.
- **Usługa AI**. Domyślny wybór na zegarku. Używa modelu lokalnego, jeśli jest włączony, lub [skonfigurowanej usługi AI](#descriptions-ai-services).
- **Google Cloud Translation**, **DeepL** i **Microsoft Translator**. Wymagają klucza API.
- **LibreTranslate**. Podaj adres serwera HTTPS i klucz, jeśli serwer go wymaga.

SandFly prosi o potrzebne dane po wybraniu usługi. Aby zmienić klucz, otwórz sekcję dostawcy, wklej klucz i wybierz **Zapisz**. Żeby go usunąć, wyczyść pole i zapisz. W LibreTranslate ustaw **Adres**, a w Microsoft Translator podaj **Region** zasobu Azure, na przykład `westeurope`. Przy zasobie globalnym zostaw region pusty.

SandFly oznacza niegotowe usługi w ustawieniach. Przy usługach sieciowych przekazuje tekst dostawcy. W trybie **Na urządzeniu** oraz przy **Usługa AI** z lokalnym modelem tekst pozostaje na urządzeniu. Klucze są zapisywane na telefonie i pomijane przy eksporcie ustawień.

### Język źródłowy i docelowy {#translation-languages}

- Opcja **Tłumacz z** pozwala wybrać język albo zostawić domyślne **Wykryj język**.
- Opcja **Tłumacz na** pozwala wybrać język przekładu. Domyślnie jest to język urządzenia.
- Przy automatycznym wykrywaniu pojawia się **Tekst w języku docelowym tłumacz na**. Możesz wybrać drugi język albo zostawić **Nie tłumacz** dla tekstu już zapisanego w języku docelowym.
- **Zamień języki** przełącza język źródłowy i docelowy. Opcja pojawia się, jeśli wskazujesz język źródłowy samodzielnie.

W trybie **Na urządzeniu** listy obejmują obsługiwane języki. Rozpoznawanie języka działa na telefonie. Przy pierwszym użyciu aplikacja może potrzebować chwili na pobranie modelu.

### Pobieranie języków {#translation-on-device}

Jeśli tłumaczenie na urządzeniu jest wybrane jako usługa zwykła albo automatyczna, otwórz **Języki na urządzeniu**. Wybierz język do pobrania lub usunięcia. Pobranie jednego języka zajmuje około 30 MB.

**Pobierz wszystkie języki** pokazuje liczbę i rozmiar, a po zatwierdzeniu pobiera je po kolei. SandFly pokazuje postęp i odczytuje kolejne ćwiartki, gdy ekran **Tłumaczenie** jest otwarty. Pobieranie trwa po wyjściu z ustawień. Możesz wrócić i wybrać tę opcję ponownie, żeby je zatrzymać.

**Pobieraj języki tylko przez Wi-Fi** jest domyślnie wyłączone. Jeśli potrzebnego języka jeszcze nie ma, SandFly pobierze go przy pierwszym użyciu i powie, co pobiera. Pobierze także angielski, przez który tłumaczone są pary pozostałych języków.

### Tłumaczenie automatyczne {#translation-automatic}

- **Automatyczne tłumaczenie** przekłada mowę w innych językach, także powiadomienia.
- **Tłumacz powiadomienia** tłumaczy same powiadomienia, jeśli tłumaczenie automatyczne pozostałej mowy jest wyłączone.

Obie opcje są domyślnie wyłączone. Po włączeniu którejś pojawiają się **Usługa automatycznego tłumaczenia** oraz **Języki bez tłumaczenia**. Usługa automatyczna może korzystać z urządzenia, Google Cloud Translation, DeepL, Microsoft Translator albo LibreTranslate. Bierze usługę wybraną do zwykłego tłumaczenia, a przy **Usługa AI** domyślnie przechodzi na **Na urządzeniu**. Na liście języków bez tłumaczenia możesz dodać na przykład te, które rozumiesz. Początkowo lista jest pusta.

Automatyczne tłumaczenie możesz też przełączać w trakcie czytania. Dodaj **Automatyczne tłumaczenie** na ekranie [Sterowanie czytaniem](#commands) i wybierz tę opcję podczas nawigacji.

#### Co dzieje się przed odczytem {#translation-automatic-details}

SandFly rozpoznaje język wypowiedzi na urządzeniu. Czyta oryginał, jeśli jest w języku docelowym lub na liście pomijanych. Jeśli wskazano język źródłowy, tłumaczy tylko z tego języka. Tekst krótszy niż trzy litery pomija przy rozpoznawaniu.

Dźwięk i wibracje pojawiają się od razu. Mowa czeka na przekład do 3 sekund. Przy błędzie lub przekroczeniu czasu zabrzmi oryginał. SandFly zapamiętuje ostatnie przekłady, dzięki czemu może od razu odczytać ten sam element ponownie.

Jeśli w trybie lokalnym brakuje języka, SandFly zacznie go pobierać po potwierdzeniu rozpoznania. W trakcie pobierania czyta oryginał. Po zakończeniu odczyta tłumaczenie elementu, jeśli wciąż na nim jesteś. Powiadomienie może czekać na pobranie do 30 sekund.

[Powiadomienia](#notifications) są tłumaczone przed dodaniem do kolejki mowy, zachowują więc swoją kolejność. Jeśli mają przypisany profil głosu, zostanie użyty. Treść prywatnych powiadomień nie jest tłumaczona ani wysyłana do usługi.

### Odczyt tłumaczenia {#translation-speech}

W **Tłumaczenie** > **Mowa** znajdziesz:

- **Używaj głosu dla języka docelowego**. Domyślnie włączone. Oznacza przekład językiem dla [ustawień mowy](#speech).
- **Podawaj język oryginału**. Przed przekładem podaje, z jakiego języka pochodzi tekst. Domyślnie wyłączone.
- **Kopiuj tłumaczenia do schowka**. Kopiuje każdy przekład. Domyślnie wyłączone.

### Gdy tłumaczenie się nie udaje {#translation-errors}

SandFly może poprosić o klucz albo o adres LibreTranslate. Powie również, jeśli dostawca odrzucił klucz, wyczerpał się limit, nie można połączyć się z usługą, język nie jest obsługiwany albo nie udało się go wykryć. Przy tłumaczeniu lokalnym poinformuje o trwającym lub przerwanym pobieraniu języka. Przy innych błędach odczyta komunikat dostawcy.
