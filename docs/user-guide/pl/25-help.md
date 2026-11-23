## Pomoc i opinie {#help}

Otwórz **Pomoc i opinie** w ustawieniach. Znajdziesz tam [podręcznik](#getting-started-user-guide), [samouczek i ćwiczenie gestów](#help-tutorial), [formularz opinii](#help-feedback) oraz [historię wersji i politykę prywatności](#help-version-history). Opcje diagnostyczne są na ekranie [**Ustawienia programisty**](#help-developer).

### Samouczek i ćwiczenie gestów {#help-tutorial}

**Samouczek SandFly** wraca do ostatniej lekcji. Na urządzeniu z ekranem dotykowym i podłączoną klawiaturą najpierw wybierasz **Samouczek dotyczący obsługi dotykowej** albo **Samouczek dotyczący klawiatury**. Zobacz [pierwsze kroki](#getting-started-tutorial).

Na ekranie **Ćwiczenie gestów** możesz sprawdzać gesty bez wykonywania poleceń. SandFly mówi nazwę gestu i przypisane działanie. Wybierz **Zakończ ćwiczenie**, żeby wyjść. Do tych ekranów prowadzą również polecenia **Otwórz samouczek** i **Ćwiczenie gestów**.

Zegarki i telewizory mają [własne samouczki](#devices). Jeśli telewizor nie ma samouczka, jego pozycja nazywa się **Pomoc**.

### Przesyłanie opinii {#help-feedback}

Wejdź w **Pomoc i opinie** > **Prześlij opinię** albo wybierz **Prześlij opinię** w menu SandFly. Zgłoszenie lub sugestię możesz wysłać bez konta. Odpowiedzi trafiają do SandFly.

Na ekranie **Prześlij opinię** rozpoczniesz nowe zgłoszenie. **Skrzynka odbiorcza** pokazuje przesłane zgłoszenia, ich stan i odpowiedzi, a **Szkice** pozwalają wrócić do niewysłanych.

#### Wysyłanie zgłoszenia {#help-feedback-send}

1. Otwórz **Pomoc i opinie** > **Prześlij opinię** i wybierz **Prześlij opinię**.
2. W **Rodzaj informacji zwrotnej** wskaż nieprawidłowe działanie, awarię, spowolnienie lub brak reakcji albo sugestię. Domyślnie wybrane jest **Nieprawidłowe działanie**.
3. Wybierz **Obszar**, na przykład **Mowa**, **Gesty** lub **Aktualizacje**. Jeśli problem dotyczy jednej aplikacji, wpisz jej nazwę.
4. Wypełnij **Tytuł** i **Opis**. Podaj, co się dzieje i czego się spodziewasz. Przy błędzie uzupełnij **Kroki do odtworzenia**.
5. Jeśli chcesz, wpisz **E-mail do odpowiedzi**. Odpowiedzi w SandFly pojawią się niezależnie od adresu. Sprawdź, czy chcesz [dołączyć dane urządzenia lub dziennik](#help-feedback-contents).
6. Wybierz **Wyślij**. SandFly potwierdzi i przeniesie zgłoszenie do skrzynki. Po nieudanej wysyłce opisze problem i zachowa szkic.

Tekst zostaje w szkicu do chwili wysłania. **Usuń wersję roboczą** usuwa szkic. Adres e-mail i ustawienia obu załączników przechodzą do następnego zgłoszenia.

#### Co znajdzie się w zgłoszeniu {#help-feedback-contents}

Oprócz wypełnionych pól zgłoszenie zawiera wersję i numer kompilacji SandFly. Domyślnie włączone **Dołącz informacje o urządzeniu** dodaje wersję Androida, producenta i model. Pod przełącznikiem można sprawdzić te dane.

**Dołącz dziennik SandFly** jest początkowo wyłączone. Dziennik może zawierać tekst z ekranu. Obejmuje ostatnie komunikaty czytnika, rozpoznane gesty, użycie klawiszy głośności, awarie i przerwy w działaniu, listę usług dostępności oraz zapisy innych procesów SandFly, takich jak lokalne AI.

Otwórz formularz zaraz po błędzie, żeby SandFly zachował bieżący fragment dziennika. Przy podzielonym ekranie odczyt następuje dopiero po **Wyślij**, więc możesz odtworzyć problem w drugiej aplikacji.

#### Stan i odpowiedzi {#help-feedback-inbox}

**Skrzynka odbiorcza** pokazuje stan zgłoszenia i liczbę nieprzeczytanych odpowiedzi. Zgłoszenie może być otwarte, zamknięte jako zakończone, nieplanowane lub powielone, albo usunięte z serwera.

Otwórz zgłoszenie. W części **Wiadomości** są odpowiedzi wraz z nadawcą i czasem. Aby odpowiedzieć, użyj **Twoja odpowiedź** i **Wyślij odpowiedź**.

Gdy SandFly działa, sprawdza skrzynkę co godzinę i po wejściu w **Prześlij opinię**. Odpowiedź lub zmiana stanu daje dźwięk i powiadomienie w kategorii **Odpowiedzi na opinie**. Zamknięte zgłoszenia są sprawdzane przez 30 dni. **Usuń ze skrzynki odbiorczej** usuwa pozycję z urządzenia i wyłącza sprawdzanie. Zgłoszenie pozostaje u autora.

#### Zgłoszenie odpowiedzi AI {#help-feedback-ai-response}

[**Zgłoś odpowiedź AI**](#commands) otwiera formularz z ostatnią odpowiedzią modelu. Możesz zgłosić na przykład błędny opis, wynik rozpoznawania na żywo albo działanie agenta. SandFly przechowuje tylko ostatnią odpowiedź do zakończenia pracy czytnika. Jeśli jeszcze jej nie ma, poinformuje o tym. Zobacz też [opisy i pytania](#descriptions).

### Historia wersji i polityka prywatności {#help-version-history}

**Historia wersji** otwiera [stronę wydań SandFly](https://github.com/aaron-gh/SandFly/releases). **Polityka prywatności** otwiera [politykę SandFly](https://sandfly.dev/privacy). Zegarek bez przeglądarki pokaże adres.

### Ustawienia programisty {#help-developer}

Część ustawień zapisuje zawartość ekranu do dziennika systemowego. Mogą ją odczytać aplikacje, które mają dostęp do dziennika. Włączaj szczegółowy zapis na czas diagnozy.

| Opcja | Działanie |
| --- | --- |
| **Kod wersji** | Podaje wersję i kod wersji. W Androidzie 11 i starszym informacja jest pod tytułem ekranu. |
| **Tryb diagnostyczny** | Stuknięcie czterema palcami zapisuje ekran w dzienniku. Tryb wymusza pokazywanie mowy, powtórzenie rozpoznanej mowy, zapis drzewa i nakładkę. Blokuje zmianę tych ustawień i poziomu rejestrowania. Wymaga potwierdzenia. |
| **Pokazuj odpowiedzi głosowe na ekranie** | Wyświetla to, co SandFly wypowiada. |
| **Używaj gestów w SandFly** | Rozpoznaje gesty w SandFly. Domyślnie włączone. Po zmianie wyłącz i włącz czytnik. Opcji nie ma na telewizorach. |
| **Ignoruj limity pamięci RAM dla AI na urządzeniu** | Pokazuje wszystkie modele i pozwala je ładować bez kontroli wolnego RAM. Zobacz [opisy lokalne](#descriptions). |
| **Powtórz rozpoznaną mowę** | Czyta tekst rozpoznany z [polecenia głosowego](#voice-commands). |
| **Włącz statystyki wydajności** | Zbiera czasy działania. **Drukuj statystyki wydajności** zapisuje je do dziennika. Zbieranie zwiększa zużycie energii i pamięci. Wymaga potwierdzenia. |
| **Czytanie dotykiem** | Domyślnie włączone. Wyłączenie zatrzymuje odczyt dotyku i gesty. Wymaga potwierdzenia. Opcji nie ma na telewizorach. |
| **Włącz debugowanie drzewa węzłów** | **Wydrukuj drzewo węzłów** zapisuje elementy ekranu do dziennika. SandFly proponuje gest trzykrotnego stuknięcia czterema palcami. Na telewizorze zapis uruchamia przycisk wyszukiwania. |
| **Włączyć nakładkę rejestrowania debugowania?** | Wyświetla dziennik na ekranie. |
| **Poziom rejestrowania** | Ustala szczegółowość zapisu. Domyślnie **Błąd**. Najdokładniejszy poziom wymaga potwierdzenia i może zapisać dane osobowe. |
| **Filtruj rejestrowanie zdarzeń ułatwiania dostępu** | Wybiera typy zapisywanych zdarzeń. |

Przełączniki diagnostyczne są domyślnie wyłączone. Aby skopiować dane pojedynczego elementu lub całe drzewo ekranu, zobacz [polecenia do sprawdzania elementów](#scripts-inspect) w rozdziale o [skryptach](#scripts).
