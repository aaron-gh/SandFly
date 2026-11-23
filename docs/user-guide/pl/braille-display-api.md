# Monitor brajlowski udostępniany przez inną aplikację

Aplikacja może udostępnić monitor, którego SandFly nie obsługuje bezpośrednio. Mogą to być wbudowane komórki i klawisze notatnika brajlowskiego albo urządzenie obsługiwane przez aplikację producenta. Klawisze służą do poruszania się i pisania, a w komórkach pojawia się treść przygotowana przez SandFly, tak samo jak przy połączeniu Bluetooth lub USB.

## Zgoda użytkownika {#what-the-user-controls}

Monitor może odczytać wszystko, co SandFly pokazuje w brajlu, a jego klawisze mogą sterować urządzeniem. SandFly korzysta z aplikacji dopiero po wyświetleniu tej informacji i udzieleniu zgody.

Gdy SandFly znajdzie nową aplikację, wyświetli powiadomienie otwierające pytanie. Aplikacja również może je otworzyć. W ustawieniach monitora brajlowskiego jest lista monitorów z innych aplikacji, z której użytkownik może nadać lub cofnąć zgodę.

Gdy monitor aplikacji działa, SandFly wyświetla powiadomienie z jej nazwą. Po udzieleniu zgody SandFly nie ogranicza treści ani częstotliwości danych, które wysyła monitor.

## Manifest aplikacji {#your-apps-manifest}

Zadeklaruj uprawnienie do zgłoszenia monitora i usługę, z którą może połączyć się SandFly:

```xml
<uses-permission android:name="io.github.aaron_gh.sandfly.permission.BRAILLE_DISPLAY" />

<application>
    <service
        android:name=".DisplayService"
        android:exported="true"
        android:permission="io.github.aaron_gh.sandfly.permission.BIND_BRAILLE_DISPLAY">
        <intent-filter>
            <action android:name="fyi.quin.backtalk.action.BRAILLE_DISPLAY" />
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

Po uzyskaniu zgody SandFly łączy się z usługą i utrzymuje połączenie przez cały czas pracy. Po zatrzymaniu usługi może połączyć się ponownie. SandFly szuka monitorów przy starcie, po instalacji lub aktualizacji aplikacji oraz po nadaniu lub cofnięciu zgody.

Żeby od razu otworzyć ekran zgody, użyj:

```kotlin
context.startActivity(
  Intent("fyi.quin.backtalk.action.ASK_BRAILLE_DISPLAY")
    .setPackage("io.github.aaron_gh.sandfly")
    .putExtra("fyi.quin.backtalk.extra.PACKAGE", context.packageName)
    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
```

Jeśli zgoda już jest, nic się nie wyświetli. Jeżeli aplikacja chce ponowić wyszukiwanie, na przykład po wykryciu sprzętu, może wysłać:

```kotlin
context.sendBroadcast(
  Intent("fyi.quin.backtalk.action.BRAILLE_DISPLAYS_CHANGED").setPackage("io.github.aaron_gh.sandfly"))
```

## Interfejs {#the-interface}

Skopiuj pliki `IBrailleDisplay.aidl` i `IBrailleDisplayHost.aidl` z katalogu `braille/brailledisplay/src/phone/aidl/fyi/quin/backtalk/braille/`. Zachowaj pakiet `fyi.quin.backtalk.braille`. Usługa zwraca `IBrailleDisplay`. SandFly przekazuje jej `IBrailleDisplayHost` do wywołań w drugą stronę:

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
| `command(name, argument)` | Przypisuje innemu klawiszowi polecenie z `BrailleInputEvent` bez prefiksu `CMD_`, na przykład `NAV_ITEM_NEXT`, `NAV_PAN_DOWN`, `ACTIVATE_CURRENT` lub `GLOBAL_BACK`. W `ROUTE` argument to numer komórki od 0. W pozostałych poleceniach przekaż 0. |

`showCells(cells)` przekazuje jeden bajt na komórkę. Punkty mają te same bity co w `brailleKeys`. Wywołania są jednokierunkowe i wracają od razu.

## Pierwszeństwo monitorów {#one-display-at-a-time}

SandFly używa jednego monitora naraz. Połączenie Bluetooth lub USB ma pierwszeństwo. Gdy taki monitor się pojawi, SandFly go użyje, a po rozłączeniu wróci do monitora z aplikacji. Jeżeli kilka dopuszczonych aplikacji zgłosi sprzęt, pierwsza zachowa dostęp do czasu zakończenia pracy.
