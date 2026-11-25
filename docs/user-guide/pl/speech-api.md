# Mowa SandFly w innych aplikacjach

Aplikacja może przekazać SandFly tekst do odczytania. SandFly użyje głosu, syntezatora, tempa, wysokości i głośności wybranych przez użytkownika. To rozwiązanie sprawdzi się między innymi w grach, aplikacjach do automatyzacji, takich jak Tasker, i narzędziach dostępu zdalnego, które odczytują komunikaty osobom niewidomym bez własnego syntezatora.

## Zgody użytkownika {#what-the-user-controls}

Żaden tekst z Twojej aplikacji nie zostanie odczytany przed uzyskaniem zgody użytkownika.

1. Użytkownik włącza **Mowa** > **Zezwalaj innym aplikacjom na odczytywanie tekstu przez SandFly**. Ta opcja jest początkowo wyłączona.
2. Po pierwszym żądaniu odczytu pojawi się powiadomienie z prośbą o zgodę. Dopóki użytkownik nie wybierze **Zezwól**, SandFly odrzuca żądania. Nie przechowuje ich do późniejszego odczytu.
3. Po wywołaniu usługi lub wysłaniu komunikatu aplikacja pojawi się w **Mowa** > **Zezwalaj innym aplikacjom na odczytywanie tekstu przez SandFly**. Stanie się tak również wtedy, gdy powiadomienie się nie pojawi. Użytkownik może tam zezwolić aplikacji na odczyt lub zablokować go w dowolnym momencie.

Po udzieleniu zgody nadal obowiązują następujące zasady.

- Jeśli użytkownik włączył **Odtwarzaj dźwięk przed odczytaniem tekstu przekazanego przez inną aplikację**, przed każdą wypowiedzią pojawi się krótki sygnał. Dzięki niemu można odróżnić mowę aplikacji od własnych wypowiedzi SandFly.
- Odczyt działa, dopóki SandFly jest włączony i aktywny.
- Na ekranie blokady odczyt działa tylko wtedy, gdy aplikacja deklaruje tę możliwość i użytkownik osobno wyrazi na nią zgodę. Zobacz [Mowa przy zablokowanym urządzeniu](#speaking-while-the-device-is-locked).
- Każda aplikacja może wysłać od razu kilka żądań, a potem mniej więcej jedno na sekundę. Nadmiarowe żądania są odrzucane.
- Tekst jest odczytywany bez formatowania.
- Żądania przerwania dotyczą mowy z innych aplikacji. Wypowiedzi samego SandFly trwają dalej. Bieżąca [klawiatura](#keyboards) działa według osobnych zasad.

Usługa nie informuje aplikacji, czy użytkownik wyraził zgodę ani czy tekst został odczytany.

## Deklaracja uprawnienia {#permission}

Dodaj uprawnienie SandFly do manifestu:

```xml
<uses-permission android:name="io.github.aaron_gh.sandfly.permission.SPEAK" />
```

Od Androida 11 zadbaj o widoczność pakietu:

```xml
<queries>
    <package android:name="io.github.aaron_gh.sandfly" />
</queries>
```

## Mowa przy zablokowanym urządzeniu {#speaking-while-the-device-is-locked}

Jeżeli aplikacja ma czytać także po zablokowaniu, na przykład ogłaszać nowe wiadomości przy wygaszonym ekranie, zadeklaruj dodatkowe uprawnienie:

```xml
<uses-permission android:name="io.github.aaron_gh.sandfly.permission.SPEAK_WHEN_LOCKED" />
```

Sama deklaracja nie włącza odczytu po zablokowaniu ekranu. Zgoda jest oddzielna.

1. Zgoda na mowę aplikacji nie obejmuje odczytu przy zablokowanym urządzeniu.
2. Gdy aplikacja po raz pierwszy zażąda odczytu przy blokadzie, pojawi się drugie powiadomienie. Żeby wybrać **Zezwól**, trzeba najpierw odblokować urządzenie. Do tego czasu żądania wysłane przy blokadzie są odrzucane.
3. Użytkownik może w każdej chwili włączyć lub wyłączyć przełącznik opisany nazwą aplikacji i dopiskiem „gdy urządzenie jest zablokowane”. Znajdzie go w **Mowa** > **Zezwalaj innym aplikacjom na odczytywanie tekstu przez SandFly**. Przełącznik jest widoczny tylko dla aplikacji, które deklarują uprawnienie.

Aplikacje bez tej deklaracji nie mogą odczytywać tekstu na ekranie blokady. Limit żądań i sygnał dźwiękowy obowiązują jak zwykle. Starsze wydania SandFly bez tego uprawnienia nie przekazują mowy innych aplikacji podczas blokady.

## Połączenie z usługą {#binding-to-the-service}

To najbardziej niezawodna metoda i działa we wszystkich wersjach Androida obsługiwanych przez SandFly. Skopiuj plik `talkback/src/main/aidl/io/github/aaron_gh/sandfly/speech/ISpeechControl.aidl` do aplikacji, zachowując pakiet `io.github.aaron_gh.sandfly.speech`. Połącz się z usługą:

```kotlin
val intent =
  Intent("io.github.aaron_gh.sandfly.action.SPEECH_CONTROL").setPackage("io.github.aaron_gh.sandfly")
context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
```

Po nawiązaniu połączenia pobierz interfejs:

```kotlin
val speech = ISpeechControl.Stub.asInterface(binder)
// Text, interrupt, rate, pitch, volume.
speech.speak("Level 3", false, 1f, 1f, 1f)
speech.stop()
```

| Metoda | Działanie |
| --- | --- |
| `speak(text, interrupt, rate, pitch, volume)` | Czyta `text`. `rate` i `pitch` mnożą ustawienia użytkownika przez wartość od 0,25 do 4. `volume` ma zakres od 0 do 1. `interrupt` zastępuje trwającą mowę innych aplikacji bez czekania. |
| `stop()` | Zatrzymuje mowę innych aplikacji. |
| `setTypingSpoken(spoken)` | Pozwala klawiaturze przejąć echo wpisywania. |

Wywołania są jednokierunkowe i wracają od razu.

## Klawiatury {#keyboards}

Aplikacja wybrana jako bieżąca klawiatura korzysta z usługi tak jak echo klawiatury SandFly. Nie potrzebuje zgody użytkownika i nie pojawia się w **Zezwalaj innym aplikacjom na odczytywanie tekstu przez SandFly**. Nie obowiązuje jej limit żądań ani dźwięk przed wypowiedzią. Może odczytywać tekst także przy zablokowanym urządzeniu. `interrupt` zatrzymuje całą bieżącą mowę, również wypowiedzi SandFly. Bez tej opcji nowy odczyt poczeka na zakończenie bieżącej wypowiedzi. Te zasady dotyczą wywołań usługi. Komunikaty rozgłoszeniowe od klawiatury są obsługiwane tak jak komunikaty innych aplikacji.

Klawiatura z własnym echem wywołuje `setTypingSpoken(true)` przy otwarciu i `setTypingSpoken(false)`, kiedy kończy odczyt. Dzięki temu SandFly nie powtarza zmian wykonanych klawiaturą ekranową, w tym wpisywania, kasowania, zastępowania, ruchu kursora i zaznaczenia. Nadal czyta własne polecenia edycji oraz wpisywanie i ruch kursora z klawiatury fizycznej lub monitora brajlowskiego. Po przełączeniu na inną klawiaturę SandFly wraca do echa. Wywołaj `setTypingSpoken(true)` ponownie, gdy Twoja klawiatura znów będzie aktywna.

## Komunikat rozgłoszeniowy {#sending-a-broadcast}

Aplikacja do automatyzacji może użyć komunikatu, żeby uniknąć łączenia z usługą. Ta metoda działa od Androida 14, jeśli nadawca ujawnia swoją tożsamość:

```kotlin
val intent =
  Intent("io.github.aaron_gh.sandfly.action.SPEAK")
    .setPackage("io.github.aaron_gh.sandfly")
    .putExtra("text", "Download finished")
    .putExtra("interrupt", false)
val options = BroadcastOptions.makeBasic().setShareIdentityEnabled(true)
context.sendBroadcast(intent, null, options.toBundle())
```

| Akcja | Dodatkowe pola |
| --- | --- |
| `io.github.aaron_gh.sandfly.action.SPEAK` | `text` jest wymaganym ciągiem znaków. `interrupt` jest wartością logiczną. `rate`, `pitch` i `volume` przyjmują liczby tak samo jak w `speak`. |
| `io.github.aaron_gh.sandfly.action.STOP_SPEECH` | Bez dodatkowych pól. |

SandFly pomija komunikaty bez informacji o nadawcy.

## Skrypty {#scripts}

Skrypt Lua może użyć bezpośrednio `sf.speak`. Zobacz [API skryptów](scripting.md).

## Przestarzałe nazwy {#deprecated-names}

Te nazwy, pochodzące z Backtalk, są przestarzałe:

| Przestarzała nazwa | Nowa nazwa |
| --- | --- |
| `fyi.quin.backtalk.action.SPEECH_CONTROL` | `io.github.aaron_gh.sandfly.action.SPEECH_CONTROL` |
| `fyi.quin.backtalk.speech.ISpeechControl` | `io.github.aaron_gh.sandfly.speech.ISpeechControl` |
| `fyi.quin.backtalk.action.SPEAK` | `io.github.aaron_gh.sandfly.action.SPEAK` |
| `fyi.quin.backtalk.action.STOP_SPEECH` | `io.github.aaron_gh.sandfly.action.STOP_SPEECH` |

Przestarzała akcja łączy się z przestarzałym interfejsem, więc zmień obie nazwy jednocześnie.

SandFly 27.0 i nowsze nadal przyjmują te nazwy od aplikacji, które z nich korzystają. Gdy aplikacja użyje jednej z nich po raz pierwszy od uruchomienia SandFly, SandFly zapisuje w logu ostrzeżenie z tagiem `SandFlyApi`. Aby wyświetlić ostrzeżenia:

```sh
adb logcat -s SandFlyApi
```

Gdy aplikacja jest kompilacją z możliwością debugowania, SandFly wyświetla też powiadomienie z listą użytych przez nią przestarzałych nazw.

Przestarzałe nazwy zostaną usunięte w jednej z kolejnych wersji. SandFly 0.7.0 i starsze przyjmują tylko przestarzałe nazwy. Aby obsługiwać także te wersje, połącz się z usługą najpierw przez nową akcję, a gdy `bindService` zwróci false, przez przestarzałą.
