# Monitor brajlowski udostępniany przez inną aplikację

Aplikacja może udostępnić monitor, którego SandFly nie obsługuje bezpośrednio. Mogą to być wbudowane komórki i klawisze notatnika brajlowskiego albo urządzenie obsługiwane przez aplikację producenta. Klawisze służą do poruszania się i pisania, a w komórkach pojawia się treść przygotowana przez SandFly, tak samo jak przy połączeniu Bluetooth lub USB.

## Zgoda użytkownika {#what-the-user-controls}

Monitor może odczytać wszystko, co SandFly pokazuje w brajlu, a jego klawisze mogą sterować urządzeniem. SandFly korzysta z aplikacji dopiero po wyświetleniu tej informacji i udzieleniu zgody.

- Jeśli SandFly znajdzie aplikację, która może działać jako monitor, a użytkownik jeszcze nie zdecydował o dostępie, pojawi się powiadomienie. Otwiera ono ostrzeżenie opisujące zakres dostępu.
- Aplikacja może też od razu otworzyć ostrzeżenie. Zobacz [Manifest aplikacji](#your-apps-manifest).
- W ustawieniach **Brajl** > **Monitor brajlowski** > **Wyświetlacze z innych aplikacji** znajduje się lista wszystkich aplikacji, które mogą działać jako monitor. Użytkownik może tam nadać lub cofnąć zgodę dla każdej z nich. Przy udzielaniu zgody również pojawi się ostrzeżenie.

Gdy monitor aplikacji działa, SandFly wyświetla powiadomienie z jej nazwą. Po udzieleniu zgody SandFly nie ogranicza treści ani częstotliwości danych, które wysyła monitor.

## Manifest aplikacji {#your-apps-manifest}

Zadeklaruj uprawnienie, które umożliwia zgłoszenie aplikacji jako monitora. Udostępnij też usługę, z którą może się połączyć wyłącznie SandFly.

```xml
<uses-permission android:name="io.github.aaron_gh.sandfly.permission.BRAILLE_DISPLAY" />

<application>
    <service
        android:name=".DisplayService"
        android:exported="true"
        android:permission="io.github.aaron_gh.sandfly.permission.BIND_BRAILLE_DISPLAY">
        <intent-filter>
            <action android:name="io.github.aaron_gh.sandfly.action.BRAILLE_DISPLAY" />
        </intent-filter>
    </service>
</application>
```

Od Androida 11 dodaj widoczność pakietu:

```xml
<queries>
    <package android:name="io.github.aaron_gh.sandfly" />
</queries>
```

Po uzyskaniu zgody SandFly łączy się z usługą. Utrzymuje połączenie przez cały czas działania, więc aplikacja działa razem z SandFly. Po zatrzymaniu usługi SandFly łączy się ponownie. Szuka monitorów przy starcie, po instalacji lub aktualizacji aplikacji oraz po nadaniu lub cofnięciu zgody.

Żeby od razu otworzyć ekran zgody, użyj:

```kotlin
context.startActivity(
  Intent("io.github.aaron_gh.sandfly.action.ASK_BRAILLE_DISPLAY")
    .setPackage("io.github.aaron_gh.sandfly")
    .putExtra("io.github.aaron_gh.sandfly.extra.PACKAGE", context.packageName)
    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
```

Jeśli zgoda już jest, nic się nie wyświetli. Jeżeli aplikacja chce ponowić wyszukiwanie, na przykład po wykryciu sprzętu, może wysłać:

```kotlin
context.sendBroadcast(
  Intent("io.github.aaron_gh.sandfly.action.BRAILLE_DISPLAYS_CHANGED").setPackage("io.github.aaron_gh.sandfly"))
```

## Interfejs {#the-interface}

Skopiuj pliki `IBrailleDisplay.aidl` i `IBrailleDisplayHost.aidl` z katalogu `braille/brailledisplay/src/phone/aidl/io/github/aaron_gh/sandfly/braille/`. Zachowaj pakiet `io.github.aaron_gh.sandfly.braille`. Usługa zwraca `IBrailleDisplay`. SandFly przekazuje jej `IBrailleDisplayHost` do wywołań w drugą stronę:

```kotlin
class DisplayService : Service() {
  private var host: IBrailleDisplayHost? = null

  private val display =
    object : IBrailleDisplay.Stub() {
      override fun onHostConnected(host: IBrailleDisplayHost) {
        this@DisplayService.host = host
        // When your hardware is ready: its name and number of text cells.
        host.start("Example display", 40)
      }

      override fun showCells(cells: ByteArray) {
        // Show cells on the hardware.
      }
    }

  override fun onBind(intent: Intent): IBinder = display
}
```

| Metoda `IBrailleDisplayHost` | Działanie |
| --- | --- |
| `start(name, cellCount)` | Zgłasza gotowy monitor, jego nazwę i liczbę komórek. Użyj 0 dla urządzenia z samymi klawiszami. Wywołaj ponownie po zmianie nazwy lub liczby. |
| `stop()` | Zgłasza niedostępność monitora, na przykład po rozłączeniu. Kiedy wróci, wywołaj `start`. |
| `brailleKeys(dots, space)` | Przekazuje punkty naciśnięte razem. Bit 0 oznacza punkt 1, a bit 7 punkt 8. `space` oznacza spację. Kombinacja wykonuje przypisane polecenie lub wpisuje znak. Dla samej spacji przekaż `brailleKeys(0, true)`. |
| `command(name, argument)` | Uruchamia dla innego klawisza polecenie z `BrailleInputEvent` bez prefiksu `CMD_`, na przykład `NAV_ITEM_NEXT`, `NAV_PAN_DOWN`, `ACTIVATE_CURRENT` lub `GLOBAL_BACK`. W `ROUTE` argument to numer komórki od 0. W pozostałych poleceniach przekaż 0. |

`showCells(cells)` przekazuje jeden bajt na komórkę. Punkty mają te same bity co w `brailleKeys`. Wywołania są jednokierunkowe i wracają od razu.

## Pierwszeństwo monitorów {#one-display-at-a-time}

SandFly używa jednego monitora naraz. Monitor Bluetooth lub USB ma pierwszeństwo. Gdy jest podłączony, monitory udostępniane przez aplikacje czekają. Jeśli jeden z nich był używany wcześniej, po rozłączeniu monitora Bluetooth lub USB SandFly do niego wróci. Gdy kilka aplikacji z udzieloną zgodą uruchomi swoje monitory, SandFly użyje pierwszego. Pozostałe czekają, aż pierwszy zakończy pracę.

## Przestarzałe nazwy {#deprecated-names}

Te nazwy, pochodzące z Backtalk, są przestarzałe:

| Przestarzała nazwa | Nowa nazwa |
| --- | --- |
| `fyi.quin.backtalk.action.BRAILLE_DISPLAY` | `io.github.aaron_gh.sandfly.action.BRAILLE_DISPLAY` |
| `fyi.quin.backtalk.braille.IBrailleDisplay` | `io.github.aaron_gh.sandfly.braille.IBrailleDisplay` |
| `fyi.quin.backtalk.braille.IBrailleDisplayHost` | `io.github.aaron_gh.sandfly.braille.IBrailleDisplayHost` |
| `fyi.quin.backtalk.action.ASK_BRAILLE_DISPLAY` | `io.github.aaron_gh.sandfly.action.ASK_BRAILLE_DISPLAY` |
| `fyi.quin.backtalk.extra.PACKAGE` | `io.github.aaron_gh.sandfly.extra.PACKAGE` |
| `fyi.quin.backtalk.action.BRAILLE_DISPLAYS_CHANGED` | `io.github.aaron_gh.sandfly.action.BRAILLE_DISPLAYS_CHANGED` |

Z usługą zadeklarowaną z przestarzałą akcją SandFly łączy się przez przestarzałe interfejsy, więc zmień akcję i oba interfejsy jednocześnie. Usługa z obiema akcjami jest łączona przez nowe interfejsy. W przypadku `BRAILLE_DISPLAYS_CHANGED` ostrzeżenie podaje nazwę aplikacji tylko w Androidzie 14 i nowszym, gdy komunikat rozgłoszeniowy udostępnia tożsamość nadawcy.

SandFly 0.8.0 i nowsze nadal przyjmują te nazwy od aplikacji, które z nich korzystają. Gdy aplikacja użyje jednej z nich po raz pierwszy od uruchomienia SandFly, SandFly zapisuje w logu ostrzeżenie z tagiem `SandFlyApi`. Aby wyświetlić ostrzeżenia:

```sh
adb logcat -s SandFlyApi
```

Gdy aplikacja jest kompilacją z możliwością debugowania, SandFly wyświetla też powiadomienie z listą użytych przez nią przestarzałych nazw.

Przestarzałe nazwy zostaną usunięte w jednej z kolejnych wersji. SandFly 0.7.0 i starsze przyjmują tylko przestarzałe nazwy. Aby obsługiwać także te wersje, zadeklaruj obie akcje, a w `onBind` zwróć interfejs pasujący do akcji intencji.
