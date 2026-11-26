## Dotyk {#touch}

Na ekranie **Dotyk** możesz zdecydować, kiedy SandFly ustawia fokus i aktywuje element. Są tu też opcje blokady dotyku i przekazywania dotknięć do wybranych aplikacji. Zobacz również [gesty](#gestures) oraz [czytanie ekranu dotykiem](#reading).

### Ustawianie fokusu i aktywowanie elementów {#touch-activation}

Domyślnie, gdy dotkniesz elementu, SandFly ustawia na nim fokus i go odczytuje. Stuknij potem dwa razy w dowolnym miejscu, żeby aktywować ten element. Możesz zmienić sposób aktywacji w ustawieniach **Dotyk**.

#### Opóźnienie zaznaczenia {#touch-focus-delay}

Wybierz **Dotyk** > **Opóźnienie zaznaczenia**, aby ustawić, jak długo SandFly czeka po dotknięciu, zanim przeniesie fokus i odczyta element. Do wyboru masz od 0,15 do 2 sekund. Domyślnie SandFly czeka 0,15 sekundy. Krótszy czas oznacza szybszą reakcję na dotyk. Opcja wymaga Androida 13 lub nowszego.

Możesz dodać **Opóźnienie zaznaczenia** do [sterowania czytaniem](#commands). Domyślnie tej opcji nie ma na liście.

#### Aktywowanie jednym stuknięciem {#touch-single-tap}

Włącz **Dotyk** > **Kliknij raz, aby uaktywnić**, żeby aktywować pojedynczym stuknięciem element, na którym jest już fokus. Opcja jest domyślnie wyłączona. Wymaga eksploracji dotykiem. Jest niedostępna na telewizorach.

#### Aktywowanie po uniesieniu palca {#touch-lift-to-activate}

Jeśli włączysz tę funkcję, po dotknięciu elementu wystarczy unieść palec, by go aktywować. Działa to podobnie do wpisywania liter na klawiaturze ekranowej. Zasięg działania wybierzesz w menu **Dotyk** > **Aktywowanie po puszczeniu**.

- **Wyłączone** to ustawienie domyślne.
- **Tylko na pasku nawigacji** uaktywnia w ten sposób przyciski Wstecz, Ekran główny i Przegląd.
- **Na całym ekranie** obejmuje wszystkie elementy.

Opcję **Aktywowanie po puszczeniu** możesz też dodać do [sterowania czytaniem](#commands), żeby zmieniać ją przesunięciem w górę lub w dół. Domyślnie nie ma jej na liście.

#### Anuluj aktywowanie po uniesieniu palca {#touch-lift-cancel}

Gdy używasz **Aktywowanie po puszczeniu**, możesz w dwóch sytuacjach unieść palec bez aktywowania elementu.

- Zsuń palec za dolną krawędź ekranu. SandFly da znać dźwiękiem i wibracją, gdy tam dotrzesz. Unieś palec w obrębie dolnych 3,5 mm ekranu, żeby anulować. Działa to również wtedy, gdy klawiatura wpisuje znaki po uniesieniu palca.
- Przytrzymaj palec na elemencie. Najpierw ustaw czas w **Dotyk** > **Przytrzymaj, aby anulować**. Dostępne wartości to **1 sekunda**, **2 sekundy**, **3 sekundy** i **5 sekund**. Gdy minie wybrany czas, usłyszysz ten sam dźwięk i poczujesz wibrację. Możesz wtedy unieść palec. Domyślnie ustawiona jest wartość **Wyłączony**. Na klawiaturze, która wpisuje po uniesieniu palca, przytrzymanie klawisza nadal działa w sposób przewidziany przez klawiaturę.

#### Aktywuj szybkim stuknięciem {#touch-tap-to-activate}

Włącz **Dotyk** > **Kliknij, aby uaktywnić**, jeśli chcesz aktywować element krótkim stuknięciem, zanim SandFly ustawi na nim fokus. Jeśli fokus zmienił się w ciągu ostatnich trzech sekund, SandFly poczeka na drugie stuknięcie. Możesz wtedy nadal aktywować element, na którym jest fokus, dwukrotnym stuknięciem. Opcja jest domyślnie wyłączona.

#### Przyciski paska nawigacji {#touch-navigation-bar}

Opcja **Dotyk** > **Aktywowanie przycisków paska nawigacji kliknięciem** przekazuje dotknięcia paska bezpośrednio do Androida. Stuknięcie naciska przycisk, a przytrzymanie działa tak jak w systemie, na przykład na przycisku ekranu głównego. Funkcja jest domyślnie wyłączona i wymaga Androida 11. Gdy ją włączysz, **Aktywowanie po puszczeniu** przestaje obejmować pasek nawigacji.

SandFly odczytuje nazwę dopiero po naciśnięciu przycisku, na przykład „Wstecz”. Żeby wyciszyć te nazwy, wyłącz **Dotyk** > **Odczytuj nazwy przycisków paska nawigacji**. Domyślnie opcja jest włączona.

#### Aktywuj element bezpośrednim dotknięciem {#touch-activate-with-tap}

Niektóre elementy reagują tylko na dotknięcie ekranu w miejscu, w którym się znajdują. Gdy zwykła aktywacja nic nie robi, użyj **Aktywuj dotknięciem**. SandFly wykona pojedyncze stuknięcie w środku elementu, na którym jest fokus. Polecenie jest domyślnie nieprzypisane. Możesz dodać [gest](#gestures) lub skrót klawiszowy. [Skrypt](#scripts) może stosować ten sposób przy każdej aktywacji wybranych elementów. Obie metody wymagają Androida 11 lub nowszego. Na wcześniejszej wersji aktywacja działa jak zwykle.

Aby używać dotknięcia przy każdej aktywacji w danej aplikacji, zaznacz ją w **Dotyk** > **Dwukrotne kliknięcie stuka w ekran**. Możesz również uruchomić w tej aplikacji polecenie **Dwukrotne kliknięcie stuka w ekran w tej aplikacji**. Domyślnie nie ma ono gestu. SandFly ogłosi, czy włączono stuknięcie ekranu przy dwukrotnym kliknięciu, czy je wyłączono. Gdy nie ma aplikacji na pierwszym planie, usłyszysz komunikat o jej braku. Ta funkcja wymaga Androida 11 i jest niedostępna na zegarkach i telewizorach.

#### Mowa podczas dotykania ekranu {#touch-speech}

Domyślnie dotknięcie ekranu zatrzymuje mowę. Jeśli chcesz, żeby SandFly czytał do chwili przeniesienia fokusu, wyłącz **Mowa** > **Zatrzymuj mowę po dotknięciu ekranu**. Zobacz [ustawienia mowy](#speech).

### Zablokuj dotyk {#touch-lock}

Po zablokowaniu dotyku SandFly nie wykonuje gestów ani nie przekazuje dotknięć do aplikacji. Nadal możesz używać klawiszy, poleceń klawiszy głośności, gestów na czytniku linii papilarnych, stuknięć w tył urządzenia i potrząśnięcia. Polecenia, które dotykają ekranu, na przykład **Aktywuj dotknięciem**, **Przytrzymaj dotyk na elemencie z fokusem** i dotknięcia w makrach, nadal trafiają do aplikacji. Blokada wymaga Androida 13 lub nowszego oraz włączonej opcji **Ustawienia programisty** > **Używaj gestów w SandFly**.

Użyj polecenia **Zablokuj lub odblokuj dotyk**. Usłyszysz „Dotyk zablokowany” lub „Dotyk odblokowany”. Przypisz polecenie do [klawisza głośności](#buttons), skrótu klawiszowego lub [stuknięcia w tył urządzenia](#gestures-back-tap), żeby odblokować ekran bez dotykania. Jednoczesne naciśnięcie obu klawiszy głośności zawsze odblokowuje dotyk.

W menu **Dotyk** > **Automatycznie blokuj dotyk** wybierzesz, kiedy ma się włączać blokada.

- **Nigdy** to ustawienie domyślne.
- **Po włączeniu ekranu** włącza blokadę za każdym razem, gdy ekran się zapala. Użyj polecenia lub obu klawiszy głośności, żeby odblokować dotyk.
- **Na ekranie blokady** włącza blokadę do chwili odblokowania telefonu.
- **Gdy czujnik zbliżeniowy jest zakryty** blokuje dotyk przy włączonym ekranie, gdy czujnik przysłania na przykład ucho lub kieszeń.

SandFly nie odczytuje komunikatu przy automatycznej blokadzie. Reakcję na dotknięcie wybierzesz w menu **Dotyk** > **Dotknięcia przy zablokowanym dotyku**. Do wyboru masz **Nic**, **Dźwięk** i **Wibracja**. Domyślnie ustawiona jest wartość **Nic**. [Skrypty](#scripts) mogą blokować dotyk na najwyżej 10 sekund.

### Dotyk bezpośredni {#touch-direct-touch}

Niektóre aplikacje, na przykład gry dźwiękowe, potrzebują bezpośredniego dostępu do dotknięć i przesunięć. Przy zwykłym czytaniu dotykiem te gesty przechwytuje najpierw SandFly. Włącz dla nich dotyk bezpośredni. SandFly pozostanie włączony i będzie czytał, a dotknięcia trafią od razu do aplikacji. Funkcja wymaga Androida 11 lub nowszego. Jest niedostępna na zegarkach i telewizorach.

#### Włącz dotyk bezpośredni w wybranej aplikacji {#touch-direct-touch-apps}

1. Otwórz **Dotyk** > **Dotyk bezpośredni**.
2. W sekcji **Aplikacje** zaznacz te, do których dotknięcia mają trafiać bezpośrednio.

Kiedy przejdziesz do zaznaczonej aplikacji, SandFly powie „Dotyk bezpośredni włączony”. Kiedy SandFly znowu przejmie dotyk, usłyszysz „Dotyk bezpośredni wyłączony”.

Na ekranie **Dotyk bezpośredni** znajdziesz następujące opcje.

- **Dotyk bezpośredni** włącza lub wyłącza funkcję dla wszystkich zaznaczonych aplikacji. Domyślnie jest włączony.
- **Odczytuj zmiany stanu dotyku bezpośredniego** jest domyślnie włączone.
- **Wibruj przy zmianie stanu dotyku bezpośredniego** jest domyślnie wyłączone.
- **Utwórz kopię zapasową ustawień** zapisuje ustawienia dotyku bezpośredniego do pliku. **Przywróć ustawienia** je wczytuje, np. na innym urządzeniu.
- **Aplikacje** zawierają aplikacje i klawiatury ekranowe. Przy każdej pozycji jest pole wyboru.

Polecenie **Włącz lub wyłącz dotyk bezpośredni dla tej aplikacji** dodaje aplikację z pierwszego planu do listy albo ją z niej usuwa. Przy dodawaniu włącza również główny przełącznik **Dotyk bezpośredni**. Możesz przypisać je do naciśnięcia klawisza głośności. Dzięki temu osoba widząca może obsłużyć aplikację, która nie współpracuje z SandFly, a Ty nadal słyszysz mowę. Tą samą drogą wyłączysz dotyk bezpośredni bez użycia ekranu. Kiedy na pierwszym planie są ustawienia SandFly lub panel powiadomień, usłyszysz „Brak aplikacji dla dotyku bezpośredniego”.

Jeśli chcesz przełączać funkcję bez zamykania aplikacji, dodaj kafelek **Dotyk bezpośredni** do szybkich ustawień Androida.

#### Kiedy SandFly ponownie przejmuje dotyk {#touch-direct-touch-return}

SandFly przejmuje dotyk, gdy otworzy się okno dialogowe, na wierzchu pojawi się inna aplikacja albo panel powiadomień lub szybkie ustawienia zajmą co najmniej połowę ekranu. Dzieje się tak również wtedy, gdy fokus przejdzie do pola tekstowego lub ekran zgaśnie. Po ustąpieniu przyczyny dotknięcia znów trafiają do aplikacji. Klawiaturę ekranową nadal możesz przeglądać dotykiem, nawet gdy reszta ekranu działa bezpośrednio.

#### Pisanie w aplikacji z własną klawiaturą {#touch-direct-typing}

Jeśli aplikacja wyświetla własną klawiaturę, otwórz jej czynności na liście **Aplikacje** i wybierz **Włącz pisanie bezpośrednie**. Dotknięcia klawiszy również trafią wtedy bezpośrednio do aplikacji. Na liście przy tej aplikacji pojawi się informacja o włączonym pisaniu bezpośrednim. Żeby je wyłączyć, użyj **Wyłącz pisanie bezpośrednie**.

#### Klawiatury z własną mową {#touch-direct-touch-keyboards}

Klawiatura, która mówi sama, na przykład osobna klawiatura brajlowska, może potrzebować bezpośredniego dotyku w każdej aplikacji. Zaznacz ją na liście **Aplikacje**. Gdy klawiatura jest widoczna, dostaje dotknięcia w swoim obszarze. Pozostałą część ekranu obsługuje SandFly. Klawiatura, która rysuje klawisze w nakładce ułatwień dostępu i ma włączoną własną usługę ułatwień dostępu, zachowuje ustawiony przez siebie obszar dotyku, także bez zaznaczenia na liście.

#### Aplikacje wybierające obszar dotyku bezpośredniego {#touch-direct-touch-areas}

Aplikacja może zadeklarować, że potrzebuje dotyku bezpośredniego. SandFly włączy go, gdy aplikacja po raz pierwszy znajdzie się na pierwszym planie. Możesz ją odznaczyć na liście **Aplikacje**.

Niektóre aplikacje przejmują tylko część ekranu, na przykład planszę gry. Na pozostałym obszarze możesz korzystać z eksploracji dotykiem. Aplikacja może też sama zakończyć przejmowanie dotyku. Funkcje te działają, gdy aplikacja jest zaznaczona na liście **Aplikacje**. Zobacz [interfejs dotyku bezpośredniego dla programistów](#api-direct-touch).

### Przywróć odczyt podczas przesuwania palca {#touch-stuck}

Na niektórych telefonach Android może po dotknięciu kilkoma palcami zablokować eksplorację dotykiem aż do ponownego uruchomienia urządzenia. Gesty i stuknięcia działają, ale podczas przesuwania palca SandFly niczego nie odczytuje. Dotyczy to wszystkich czytników na urządzeniu.

Opcja **Dotyk** > **Napraw zablokowaną eksplorację dotykową** jest domyślnie włączona. SandFly rozpoznaje problem, gdy podczas eksploracji nie znajduje elementów. Do naprawy potrzebuje uprawnienia `DUMP`.

- Jeśli działa [Shizuku](#updates), SandFly przyznaje sobie uprawnienie przy starcie i po włączeniu tej opcji. Za pierwszym razem Shizuku wyświetli prośbę o zgodę.
- Możesz też nadać uprawnienie raz przez `adb`. Dopóki go nie ma, SandFly pokazuje polecenie. Użyj **Kopiuj polecenie**, żeby skopiować je do schowka.

```
adb shell pm grant io.github.aaron_gh.sandfly android.permission.DUMP
```

Jeśli SandFly ma uprawnienie, mówi „Naprawianie eksploracji dotykowej”, kończy dotknięcia, które Android nadal traktował jako aktywne, i mówi „Naprawiono eksplorację dotykową”. Dotknięcie podczas naprawy trafia bezpośrednio do aplikacji. Naprawa działa na telefonach, które pokazują te dotknięcia w `dumpsys input`, na przykład na telefonach Samsunga.

Gdy brakuje uprawnienia lub telefon nie udostępnia danych, SandFly może tylko ostrzec o możliwej blokadzie. W takiej sytuacji uruchom telefon ponownie. Ostrzeżenie może się też pojawić, gdy przyłożysz palec do miejsca bez elementów do odczytania, na przykład w grze. SandFly podaje je najwyżej raz na 10 minut.

Jeżeli ekran zgaśnie podczas dotykania, Android może potraktować uniesione palce tak, jakby dalej dotykały ekranu. Wtedy działa wyłącznie eksploracja, a gesty i dwukrotne stuknięcia przestają reagować. SandFly wykrywa zgaśnięcie ekranu w trakcie dotyku lub dwa kolejne dotknięcia zakończone eksploracją. Nie liczy się dotknięcie, które kończy się, bo następny palec dotyka ekranu, jak przy szybkim pisaniu. Wyłącza wtedy eksplorację i zaraz włącza ją ponownie. Ta naprawa działa niezależnie od opcji **Napraw zablokowaną eksplorację dotykową** i uprawnienia `DUMP`.

### Przywróć domyślne czasy reakcji {#touch-reset-delays}

Wybierz **Dotyk** > **Zresetuj wszystkie ustawienia opóźnienia**. Potwierdź w oknie **Zresetować ustawienia opóźnienia?**.

SandFly przywróci wartości domyślne tych opcji.

- Na ekranie **Dotyk** przywróci **Kliknij raz, aby uaktywnić** i **Opóźnienie zaznaczenia**.
- Na ekranie **Wyświetlanie** przywróci **Wyłącz animacje**.
- Na ekranie **Pisanie** przywróci **Metoda pisania**, **Opóźnienie podczas naciskania i przytrzymywania w czasie pisania**, **Opóźnienie zaznaczenia podczas pisania** oraz **Szybciej odczytuj przyciski klawiatury ekranowej**. Zobacz [ustawienia pisania](#typing).
- Na ekranie **Nawigacja** przywróci **Odczytuj elementy, zanim przewinięcie pokaże je na ekranie**.
- Przywróci też domyślną widoczność opcji sterowania czytaniem **Opóźnienie zaznaczenia** i **Opóźnienie zaznaczenia podczas pisania**.
