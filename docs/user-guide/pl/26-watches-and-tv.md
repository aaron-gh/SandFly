## Zegarki i telewizory {#devices}

SandFly działa na zegarkach z Wear OS oraz na telewizorach z Android TV i Google TV. W tym rozdziale opisano różnice w porównaniu z obsługą telefonu. Pozostałe instrukcje z podręcznika dotyczą także zegarków i telewizorów, o ile dana funkcja nie wymaga ekranu dotykowego, telefonu lub sprzętu, którego nie ma w urządzeniu.

### Zegarki z Wear OS {#devices-watch}

Zegarki mają osobną kompilację SandFly w pliku `sandfly-wear.apk`. Ustawienia są takie same jak na telefonie, z wyjątkiem opcji dotyczących funkcji niedostępnych na zegarkach.

#### Zainstaluj SandFly na zegarku {#devices-watch-install}

Do instalacji `sandfly-wear.apk` potrzebujesz komputera z adb.

1. Pobierz plik `sandfly-wear.apk` z [najnowszego wydania](https://github.com/aaron-gh/SandFly/releases/latest) na komputer.
2. Na zegarku włącz opcje programistyczne, a następnie debugowanie ADB i debugowanie bezprzewodowe. Kolejność czynności zależy od zegarka. W większości modeli opcje programistyczne pojawią się po siedmiokrotnym stuknięciu numeru kompilacji w ustawieniach **Informacje**.
3. Na komputerze połącz się z zegarkiem przez `adb connect` i adres wyświetlany w ustawieniu debugowania bezprzewodowego. Niektóre zegarki wymagają najpierw sparowania przez `adb pair`.
4. Uruchom poniższe polecenie.

   ```
   adb install sandfly-wear.apk
   ```

5. Na zegarku włącz SandFly w ustawieniach ułatwień dostępu.

Aby otworzyć ustawienia SandFly na zegarku, wybierz **SandFly** z listy aplikacji. Po pierwszym włączeniu SandFly otworzy samouczek dla zegarka.

#### Gesty na zegarkach {#devices-watch-gestures}

Ekran zegarka rozpoznaje najwyżej dwa palce jednocześnie. Możesz więc używać gestów wykonywanych jednym lub dwoma palcami. Przesunięcia dwoma palcami trafiają do aplikacji i służą do przewijania.

Domyślne przypisania są takie jak na telefonach, z poniższymi wyjątkami.

| Gest | Polecenie na zegarku |
| --- | --- |
| **Kliknij dwukrotnie i przytrzymaj 2 palcami** | **Otwórz menu SandFly** |
| **Kliknij trzykrotnie i przytrzymaj 2 palcami** | **Włącz lub wyłącz komunikaty głosowe** |
| **Przesuń w lewo, a potem w górę** | **Powtórz ostatnią wypowiedź** |
| **Przesuń w prawo, a potem w górę** | **Zwiększ głośność** |
| **Przesuń w prawo, a potem w dół** | **Zmniejsz głośność** |
| **Przesuń w lewo, a potem w dół** | **Nieprzypisany** |

Domyślne gesty na telefonach znajdziesz w [](#appendix-gestures). Przypisania zmienisz na ekranie **Gesty**. Zobacz [](#gestures).

#### Ustawienia i funkcje na zegarkach {#devices-watch-settings}

Na zegarku zwróć uwagę na te różnice.

- Sterowanie czytaniem domyślnie zawiera elementy **Przewijanie** i **Głośność dostępności**. Zobacz [](#reading).
- Menu SandFly można przewijać, aby dojść do każdej pozycji. Polecenie **Opisz ekran** jest domyślnie poza tym menu. Pozycja wyłączająca SandFly nazywa się **Wyłącz**. Zobacz [](#commands).
- Pozycja **Nowe funkcje w SandFly** w angielskim interfejsie zegarka nosi nazwę **What's new**.
- Wstrzymywanie SandFly i opcja **Wznawianie SandFly** są niedostępne. Zobacz [](#pause).
- Ćwiczenie gestów otworzysz po wybraniu **Ćwiczenie gestów** na początku ekranu **Gesty**. Otwiera się lekcja ćwiczeń z samouczka dla zegarka. Na zegarku tej pozycji nie ma w menu **Pomoc i opinie**.
- Pozycji **Trener gestów** i **Ćwiczenie klawiatury** nie ma w menu **Pomoc i opinie**.
- Jeśli na zegarku brakuje przeglądarki, opcje **Historia wersji** i **Polityka prywatności** wyświetlają adres odpowiedniej strony.

Na zegarkach nie ma poniższych funkcji ani odpowiadających im ustawień.

- Skrypty, makra, własne menu i własne elementy sterowania czytaniem.
- Polecenia głosowe, dotyk bezpośredni, stukanie w tył telefonu, potrząsanie oraz ustawienia gestów przy krawędziach i w rogach.
- Klawiatura brajlowska, klawiatura do pisania odręcznego i klawiatura SandFly.
- AI na urządzeniu i ustawienie **Użyj SandFly jako tapety**.
- **Włączaj tryb głośnomówiący po odsunięciu telefonu od ucha**, **Resetuj dźwięk ułatwień dostępu po połączeniach**, **Skróty przy wygaszonym ekranie** i **Pokazuj instrukcje wyłączania funkcji SandFly**.

O powiadomieniach na zegarkach Samsung przeczytasz w [](#notifications).

#### Aktualizacje na zegarku {#devices-watch-updates}

Na zegarku aktualizujesz kompilację `sandfly-wear.apk` według instrukcji w [](#updates). Gdy zegarek ma dostęp do Wi-Fi, pobiera aktualizację bezpośrednio przez własne Wi-Fi i pokazuje postęp w powiadomieniu. Jeśli okno instalacji się nie otworzy, np. po zgaśnięciu ekranu w trakcie pobierania, stuknij powiadomienie **Pobrano aktualizację SandFly**.

### Android TV {#devices-tv}

Na Android TV i Google TV instalujesz ten sam plik `sandfly.apk` co na telefonie.

#### Zainstaluj SandFly na telewizorze {#devices-tv-install}

1. Włącz opcje programistyczne i debugowanie sieciowe na telewizorze. Czynności różnią się w zależności od modelu.
2. Na komputerze podłączonym do tej samej sieci użyj `adb connect` z adresem IP telewizora.
3. Pobierz `sandfly.apk` z [najnowszego wydania](https://github.com/aaron-gh/SandFly/releases/latest) i uruchom poniższe polecenie.

   ```
   adb install sandfly.apk
   ```

4. Na telewizorze włącz SandFly w ustawieniach ułatwień dostępu. Na większości telewizorów znajdziesz je w menu **Ustawienia** > **Ustawienia urządzenia** > **Ułatwienia dostępu**.

Na wielu telewizorach jednoczesne przytrzymanie przycisków Wstecz i W dół na pilocie przez trzy sekundy włącza lub wyłącza SandFly. To skrót ułatwień dostępu telewizora. Możesz go też ustawić w jego ustawieniach.

Aby otworzyć ustawienia SandFly, wybierz **Ustawienia SandFly** na liście aplikacji na ekranie głównym telewizora.

#### Obsługuj telewizor pilotem {#devices-tv-remote}

Na telewizorze przenosisz fokus pilotem.

- Przyciski strzałek przenoszą fokus do następnego elementu w górę, w dół, w lewo lub w prawo.
- Przycisk wyboru aktywuje element, na którym jest fokus. Jeśli elementu nie da się aktywować, naciśnięcie trafia do aplikacji.
- Przytrzymaj przycisk wyboru, aby wykonać długie naciśnięcie na elemencie, na którym jest fokus.
- Gdy fokus jest na suwaku, naciśnij przycisk wyboru, aby włączyć tryb suwaka. Usłyszysz „Włączono Tryb suwaka”. Potem zmieniasz wartość strzałkami. Naciśnij przycisk wyboru ponownie, aby wyjść z trybu suwaka.
- Gdy włączysz **Ustawienia programisty** > **Włącz debugowanie drzewa węzłów**, przycisk wyszukiwania zapisuje drzewo elementów ekranu do dziennika systemowego. Zobacz [](#help-developer).

Gdy na ekranie jest klawiatura, strzałki i przycisk wyboru obsługuje klawiatura ekranowa. O klawiaturach fizycznych przeczytasz w [](#keyboards).

#### Ustawienia i funkcje na telewizorach {#devices-tv-settings}

Na telewizorach obowiązują poniższe różnice.

- Ustawienia funkcji zależnych od ekranu dotykowego albo telefonu są ukryte. Dotyczy to m.in. opcji **Gesty**, **Trener gestów**, **Dotyk bezpośredni**, **Odczytywanie stanu urządzenia**, **Używaj przeglądu SandFly**, **Używaj listy powiadomień SandFly**, **Włączaj tryb głośnomówiący po odsunięciu telefonu od ucha** oraz **Opisy obrazów i ekranu**. Ukryte są też ekrany ustawień **Menu SandFly** i **Sterowanie czytaniem**.
- Wstrzymywanie SandFly i opcja **Wznawianie SandFly** są niedostępne. Zobacz [](#pause).
- Pierwsza pozycja w menu **Pomoc i opinie** otwiera samouczek dla telewizora, jeśli jest dostępny. Na telewizorze bez samouczka ta pozycja nazywa się **Pomoc**.
- Na telewizorach często nie są wyświetlane powiadomienia, więc powiadomienie o aktualizacji może się nie pojawić. Aby zaktualizować SandFly, wybierz **Aktualizacje i instalacja** > **Sprawdź aktualizacje**. Zobacz [](#updates-check).
