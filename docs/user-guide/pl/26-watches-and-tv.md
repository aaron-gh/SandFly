## Zegarki i telewizory {#devices}

SandFly działa na zegarkach Wear OS, na Android TV i na Google TV. Ten rozdział podaje instalację oraz najważniejsze różnice w obsłudze. Pozostałe rozdziały mają zastosowanie, jeśli urządzenie obsługuje daną funkcję.

### Zegarki Wear OS {#devices-watch}

Na zegarku użyj osobnego wydania `sandfly-wear.apk`. Ustawienia są podobne do telefonu. Pozycje zależne od funkcji niedostępnych na zegarku nie są wyświetlane.

#### Instalacja na zegarku {#devices-watch-install}

1. Pobierz `sandfly-wear.apk` z [najnowszego wydania](https://github.com/aaron-gh/SandFly/releases/latest) na komputer z ADB.
2. Włącz opcje programisty na zegarku. Zwykle trzeba siedem razy stuknąć numer kompilacji na ekranie **Informacje**. Następnie włącz debugowanie ADB i debugowanie bezprzewodowe.
3. Połącz się z adresem i portem z ekranu debugowania przez `adb connect`. Jeśli zegarek wymaga parowania, użyj wcześniej `adb pair` z portem i kodem z okna parowania.
4. Z katalogu z pobranym pakietem uruchom `adb install sandfly-wear.apk`.
5. Włącz SandFly w ustawieniach dostępności zegarka.

Aby otworzyć ustawienia czytnika, wybierz **SandFly** na liście aplikacji. Pierwsze uruchomienie otwiera samouczek zegarka.

#### Gesty na zegarku {#devices-watch-gestures}

Zegarek rozpoznaje do dwóch palców. Przesunięcia dwoma palcami trafiają do aplikacji i służą do przewijania. Poniższe gesty różnią się od [domyślnego układu na telefonie](#appendix-gestures).

| Gest | Domyślne działanie |
| --- | --- |
| Stuknij dwa razy dwoma palcami i po drugim stuknięciu przytrzymaj. | Otwiera menu SandFly. |
| Stuknij trzy razy dwoma palcami i po trzecim stuknięciu przytrzymaj. | Włącza lub wyłącza mowę. |
| Przesuń palec w lewo, potem w górę. | Powtarza ostatnią wypowiedź. |
| Przesuń palec w prawo, potem w górę. | Zwiększa głośność. |
| Przesuń palec w prawo, potem w dół. | Zmniejsza głośność. |
| Przesuń palec w lewo, potem w dół. | Brak przypisania. |

Skróty zmienisz w ustawieniach [Gesty](#gestures).

#### Ustawienia i funkcje na zegarku {#devices-watch-settings}

- W [sterowaniu czytaniem](#reading) od razu są **Przewijanie** i **Głośność dostępności**.
- [Menu SandFly](#commands) można przewijać. Początkowo nie zawiera **Opisz ekran**. Pozycja wyłączenia nazywa się **Wyłącz**. Informacje o nowościach mogą się wyświetlać jako krótsza pozycja.
- **Ćwiczenie gestów** znajdziesz u góry ekranu **Gesty**. Otwiera odpowiednią lekcję samouczka. Zegarek bez przeglądarki poda adres historii wersji lub polityki prywatności.
- [Wstrzymanie i automatyczne wznowienie](#pause) są dostępne na telefonach i tabletach.
- Na zegarku nie ma skryptów, makr, własnych menu i opcji sterowania, poleceń głosowych, dotyku bezpośredniego, stuknięć w tył, potrząśnięcia ani gestów krawędzi i rogów. Niedostępne są również klawiatura brajlowska, odręczna i SandFly, lokalny model AI, tapeta SandFly, opcje audio rozmów z telefonu, skróty klawiszy głośności przy wyłączonym ekranie i komunikat awaryjnego wyłączenia.

Zobacz [ustawienia powiadomień](#notifications) i [odczyt kart na zegarkach Samsung](#notifications-other).

#### Aktualizacje na zegarku {#devices-watch-updates}

Zegarek pobiera kolejną wersję `sandfly-wear.apk` w taki sam sposób jak [inne wydania SandFly](#updates). Gdy ma dostęp do własnej sieci Wi-Fi, pobiera bezpośrednio przez nią. Postęp widać w powiadomieniu. Jeśli ekran zgaśnie i instalator się nie otworzy, wybierz **Pobrano aktualizację SandFly**.

### Android TV i Google TV {#devices-tv}

Na telewizorze użyj pakietu `sandfly.apk`, tak jak na telefonie.

#### Instalacja na telewizorze {#devices-tv-install}

1. Włącz opcje programisty i debugowanie sieciowe w telewizorze.
2. Z komputera w tej samej sieci połącz się przez `adb connect` z adresem telewizora.
3. Pobierz `sandfly.apk` z [najnowszego wydania](https://github.com/aaron-gh/SandFly/releases/latest) i uruchom `adb install sandfly.apk`.
4. Włącz SandFly w ustawieniach dostępności. Zwykle znajdziesz je w ustawieniach urządzenia.

Na wielu telewizorach przytrzymanie Wstecz i strzałki w dół przez 3 sekundy przełącza usługę przypisaną do skrótu dostępności. Skrót możesz ustawić w telewizorze. Aby otworzyć opcje czytnika, wybierz **Ustawienia SandFly** wśród aplikacji na ekranie głównym.

#### Obsługa pilotem {#devices-tv-remote}

Strzałki przenoszą fokus w wybranym kierunku. Przycisk wyboru uaktywnia element, na którym jest fokus. Jeśli elementu nie da się uaktywnić, naciśnięcie trafia do aplikacji. Przytrzymaj przycisk, by wykonać długie naciśnięcie na elemencie.

Na suwaku przycisk wyboru przełącza tryb regulacji. Po komunikacie zmieniaj wartość strzałkami. Naciśnij przycisk drugi raz, żeby wrócić do nawigacji.

Przy włączonej opcji [**Włącz debugowanie drzewa węzłów**](#help-developer) przycisk wyszukiwania zapisuje drzewo ekranu. Kiedy jest widoczna klawiatura ekranowa, SandFly pozostawia jej strzałki i przycisk wyboru. Zobacz [obsługę klawiatury fizycznej](#keyboards).

#### Ustawienia i funkcje na telewizorze {#devices-tv-settings}

Telewizor ukrywa ustawienia zależne od dotyku lub funkcji telefonu. Dotyczy to między innymi gestów, dotyku bezpośredniego, odczytu stanu urządzenia, przeglądu, własnej listy powiadomień, trybu głośnomówiącego i opisów. Nie pokazuje też ekranów ustawień menu i sterowania czytaniem ani [wstrzymania czytnika](#pause).

Pierwsza pozycja w **Pomoc i opinie** otwiera samouczek, jeśli jest dostępny, albo **Pomoc**. Ponieważ telewizor może nie wyświetlać powiadomienia o aktualizacji, użyj [**Aktualizacje i instalacja** > **Sprawdź aktualizacje**](#updates-check).
