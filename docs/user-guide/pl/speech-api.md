# Mowa SandFly w innych aplikacjach

Aplikacja może poprosić SandFly o wypowiedzenie tekstu głosem użytkownika, z wybranym syntezatorem, tempem, wysokością i głośnością. Z tego interfejsu mogą korzystać na przykład gry, Tasker i narzędzia dostępu zdalnego bez własnego syntezatora.

## Zgody użytkownika {#what-the-user-controls}

1. Użytkownik włącza **Mowa** > **Zezwalaj innym aplikacjom na odczytywanie tekstu przez SandFly**. Ta opcja jest początkowo wyłączona.
2. Przy pierwszej próbie SandFly wyświetla powiadomienie z pytaniem o dostęp. Do czasu wybrania **Zezwól** SandFly odrzuca żądania i nie odczyta ich później.
3. Po wywołaniu usługi lub wysłaniu komunikatu aplikacja pojawia się na liście pod tym ustawieniem. Użytkownik może ją dopuścić lub zablokować w dowolnym momencie.

Po udzieleniu zgody nadal obowiązują następujące zasady:

- Jeśli użytkownik włączył dźwięk zapowiadający mowę innych aplikacji, SandFly odtwarza go przed każdą wypowiedzią.
- Odczyt działa, dopóki SandFly jest włączony i aktywny.
- Na ekranie blokady potrzebne jest osobne [zezwolenie](#speaking-while-the-device-is-locked).
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

To uprawnienie służy do poproszenia o osobną zgodę. Gdy aplikacja po raz pierwszy zażąda mowy przy blokadzie, SandFly pokaże drugie powiadomienie. Żeby nacisnąć **Zezwól**, trzeba odblokować urządzenie. Do czasu uzyskania zgody żądania na ekranie blokady są odrzucane.

W ustawieniach mowy przy aplikacji jest oddzielny przełącznik dla zablokowanego ekranu. Pojawia się tylko wtedy, gdy aplikacja deklaruje uprawnienie. Obowiązują te same limity i ustawienia dźwięku. Starsze wydania SandFly, które nie mają tego uprawnienia, nie przekazują mowy innych aplikacji na ekranie blokady.

## Połączenie z usługą {#binding-to-the-service}

To najbardziej niezawodna metoda i działa we wszystkich wersjach Androida obsługiwanych przez SandFly. Skopiuj plik `talkback/src/main/aidl/fyi/quin/backtalk/speech/ISpeechControl.aidl` do aplikacji, zachowując pakiet `fyi.quin.backtalk.speech`. Połącz się z usługą:

```kotlin
val intent =
  Intent("fyi.quin.backtalk.action.SPEECH_CONTROL").setPackage("io.github.aaron_gh.sandfly")
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

Bieżąca klawiatura systemowa używa usługi tak jak echo klawiatury SandFly. Działa bez zgody, bez limitu i sygnału, także przy zablokowanym ekranie. `interrupt` może przerwać również mowę samego SandFly. Bez tej opcji nowy odczyt poczeka. Komunikaty rozgłoszeniowe od klawiatury podlegają zwykłym zasadom dla aplikacji.

Klawiatura z własnym echem wywołuje `setTypingSpoken(true)` przy otwarciu i `setTypingSpoken(false)`, kiedy kończy odczyt. Dzięki temu SandFly nie powtarza zmian wykonanych klawiaturą ekranową, w tym wpisywania, kasowania, zastępowania, ruchu kursora i zaznaczenia. Nadal czyta własne polecenia edycji oraz zmiany z klawiatury fizycznej i monitora brajlowskiego. Po przełączeniu na inną klawiaturę SandFly wraca do echa. Wywołaj `setTypingSpoken(true)` ponownie, gdy twoja klawiatura stanie się bieżącą.

## Komunikat rozgłoszeniowy {#sending-a-broadcast}

Aplikacja do automatyzacji może użyć komunikatu, żeby uniknąć łączenia z usługą. Ta metoda działa od Androida 14, jeśli nadawca ujawnia swoją tożsamość:

```kotlin
val intent =
  Intent("fyi.quin.backtalk.action.SPEAK")
    .setPackage("io.github.aaron_gh.sandfly")
    .putExtra("text", "Download finished")
    .putExtra("interrupt", false)
val options = BroadcastOptions.makeBasic().setShareIdentityEnabled(true)
context.sendBroadcast(intent, null, options.toBundle())
```

| Akcja | Dodatkowe pola |
| --- | --- |
| `fyi.quin.backtalk.action.SPEAK` | `text` jest wymaganym ciągiem znaków. `interrupt` jest wartością logiczną. `rate`, `pitch` i `volume` przyjmują liczby tak samo jak w `speak`. |
| `fyi.quin.backtalk.action.STOP_SPEECH` | Bez dodatkowych pól. |

SandFly pomija komunikaty bez informacji o nadawcy.

## Skrypty {#scripts}

Skrypt Lua może użyć bezpośrednio `sf.speak`. Zobacz [API skryptów](scripting.md).
