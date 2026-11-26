## Updates, Shizuku en root {#updates}

SandFly update zichzelf vanuit de SandFly-releases op GitHub. De instellingen voor updates staan in **Updates en installatie**.

Sommige functies hebben machtigingen nodig die Android alleen verleent via adb, de Android Debug Bridge. SandFly kan deze krijgen via Shizuku, of via root op een apparaat met root. De instelling voor root staat in **Shizuku en root**.

### Stabiele releases en ontwikkelversies {#updates-channels}

SandFly wordt in twee soorten builds gepubliceerd:

- Stabiele releases, genoemd naar versie, zoals SandFly 0.6.0. Deze staan op de [SandFly-releasepagina](https://github.com/aaron-gh/SandFly/releases).
- Ontwikkelversies, gebouwd vanuit elke wijziging aan SandFly. Deze krijgen wijzigingen eerder dan stabiele releases, en worden minder getest.

Elke build heeft een buildnummer, dat omhoog gaat bij elke wijziging. **Updates en installatie** > **Controleren op updates** toont de versie en het buildnummer van de geïnstalleerde SandFly, zoals "Versie 0.6.1, build 1234".

Standaard biedt SandFly alleen stabiele releases aan. Om ook ontwikkelversies te krijgen, zet je **Updates en installatie** > **Ontwikkelversies** aan. SandFly biedt dan de build aan die nieuwer is, een ontwikkelversie of een stabiele release, en controleert ongeveer één keer per uur op updates in plaats van één keer per dag.

### Controleer op updates {#updates-check}

1. Ga naar **Updates en installatie**.
2. Kies **Controleren op updates**.

SandFly toont "Controleren op updates", en daarna een van deze:

- "SandFly is up-to-date".
- Een dialoogvenster **SandFly build** *nummer* **is beschikbaar**, met de wijzigingen in de nieuwe build. Voor een stabiele release zijn de wijzigingen de releasenotities. Voor een ontwikkelversie zijn het de wijzigingen sinds de geïnstalleerde build. Kies **Installeren** om deze te installeren, of **Annuleren**.
- "Kon niet controleren op updates", wanneer GitHub niet bereikt kan worden. Probeer het later opnieuw.

Terwijl een nieuwe ontwikkelversie wordt gepubliceerd, wacht de controle daarop en probeert het opnieuw, tot een minuut lang.

### Automatisch controleren op updates {#updates-automatic}

**Updates en installatie** > **Automatisch controleren op updates** staat standaard aan. Terwijl SandFly aan staat, controleert het 30 seconden na het starten op een nieuwe build, en daarna één keer per dag, of één keer per uur met **Ontwikkelversies** aan. Wanneer een nieuwe build beschikbaar is, toont SandFly de melding **SandFly-update beschikbaar**, met de wijzigingen. Elke build wordt één keer aangekondigd.

De melding heeft machtiging nodig voor SandFly om meldingen te tonen. Als meldingen voor SandFly uit staan, wordt de build aangekondigd zodra ze worden aangezet.

Zet **Automatisch controleren op updates** uit om alleen te controleren met **Controleren op updates**.

### Installeer een update {#updates-install}

1. Tik op de melding **SandFly-update beschikbaar**, of kies **Installeren** in het dialoogvenster van **Controleren op updates**.
2. De eerste keer opent Android de instelling die toestaat dat SandFly apps installeert, en toont SandFly "Geef SandFly toestemming om apps te installeren en probeer de update vervolgens opnieuw". Zet de instelling aan, ga terug, en herhaal stap 1.
3. SandFly toont "Update downloaden" en de voortgang van de download in een melding: het percentage, en na een paar seconden de resterende tijd, zoals "40%, nog 1 minuut en 20 seconden".
4. Wanneer de download voltooid is, opent de Android-installatiemelding. Kies **Update** of **Installeren**.

SandFly start opnieuw op met de nieuwe build, met dezelfde instellingen. Als de installatiemelding niet opent, zoals wanneer het scherm uitging tijdens de download, tik je op de melding **SandFly-update gedownload**. De melding verdwijnt na 10 minuten.

SandFly installeert alleen builds van SandFly die zijn ondertekend met de SandFly-sleutel. Als de download iets anders is, toont SandFly "De download is geen build van deze app, dus is deze niet geïnstalleerd". Wanneer de verbinding wegvalt, probeert SandFly het tot vijf keer opnieuw, en vervolgt de download waar deze stopte. Als de download nog steeds mislukt, toont SandFly "Kan de update niet downloaden". Dezelfde build opnieuw installeren, voordat SandFly opnieuw start, vervolgt de download.

Een build van SandFly die je zelf bouwt, is ondertekend met een andere sleutel. Android installeert geen SandFly-release over deze heen, of andersom. Verwijder de ene voordat je de andere installeert.

Op een horloge, zie [](#devices-watch-updates).

### Laat SandFly lijken geïnstalleerd vanuit Google Play {#updates-play-installer}

Sommige apps, zoals sommige bank-apps, zetten toegankelijkheidsservices uit die Android registreert als geïnstalleerd vanaf een andere plek dan Google Play. **Updates en installatie** > **Weergeven als geïnstalleerd via Google Play** laat Android Google Play registreren als de installer van SandFly, zodat die apps SandFly aan laten staan. Deze instelling staat standaard uit en heeft Shizuku of root nodig.

Wanneer je de instelling aanzet, installeert SandFly zijn eigen APK opnieuw via Shizuku, waarbij Google Play als installer wordt genoemd. SandFly start opnieuw tijdens de installatie, dus spraak stopt even. De app en de instellingen blijven hetzelfde. Alleen wat andere apps over de installer wordt verteld, verandert. Een app kan nog op andere manieren ontdekken dat SandFly niet is geïnstalleerd vanuit Google Play.

Terwijl de instelling aan staat:

- Installeert SandFly updates via Shizuku, zodat Google Play de geregistreerde installer blijft. Als dat mislukt, installeert SandFly de update met de Android-installatiemelding.
- Als iets anders de installer registreert, zoals een update die is geïnstalleerd terwijl Shizuku niet actief was, registreert SandFly Google Play opnieuw de volgende keer dat Shizuku SandFly toestaat. Tot die tijd toont de instelling "Aan, maar Google Play is nog niet geregistreerd als installer. SandFly stelt dit in zodra Shizuku beschikbaar is."

De instelling uitzetten registreert de installer die SandFly ervoor had.

Zonder Shizuku kan de instelling de installer niet wijzigen. De actie **Opdracht kopiëren** kopieert een adb-opdracht die vanaf een computer hetzelfde doet, in de map met `sandfly.apk`:

```
adb install -r -i com.android.vending sandfly.apk
```

Om de installer weer als een gewone installatie te registreren, voer je `adb install -r sandfly.apk` uit.

### Shizuku {#updates-shizuku}

[Shizuku](https://shizuku.rikka.app) is een app die opdrachten uitvoert voor andere apps met de machtigingen van adb. Met Shizuku actief en SandFly toegestaan, geeft SandFly zichzelf machtigingen die Android alleen via adb verleent, en voert het opdrachten uit die alleen adb kan uitvoeren. SandFly heeft Shizuku versie 11 of hoger nodig.

#### Stel Shizuku in {#updates-shizuku-setup}

1. Installeer Shizuku en start het, zoals de Shizuku-app beschrijft. Op Android 11 en hoger kan Shizuku starten met draadloos debuggen, zonder computer. Op een apparaat met root kan Shizuku ook starten met root.
2. Zet SandFly aan, of zet het uit en weer aan als het al aan staat.
3. Wanneer SandFly start terwijl Shizuku actief is, toont Shizuku een dialoogvenster om SandFly toe te staan. Kies om dit toe te staan.

SandFly vraagt maar één keer zelf om Shizuku-toegang. Daarna opent het Shizuku-dialoogvenster wanneer je een functie aanzet die Shizuku nodig heeft. Als je hebt gekozen om SandFly te weigeren en niet meer te vragen, sta je SandFly toe in de Shizuku-app.

Shizuku dat is gestart met draadloos debuggen of adb stopt wanneer het apparaat opnieuw start. Start het opnieuw in de Shizuku-app.

#### Functies die Shizuku of root gebruiken {#updates-shizuku-features}

Zonder Shizuku of root zijn deze functies niet beschikbaar of werken ze beperkt, zoals beschreven in hun hoofdstukken:

- **Aanraking** > **Vastgelopen aanraakverkenning repareren**, standaard aan, heeft de machtiging DUMP nodig om aanraakverkenning te repareren wanneer Android een aanraking actief laat staan. Zonder deze machtiging zegt SandFly alleen dat aanraakverkenning mogelijk is vastgelopen. Zie [](#touch).
- **Audio-uitvoer** > **Luidspreker wanneer de telefoon niet bij je oor is** heeft machtiging nodig om gesprekken te beheren. Vereist Android 12 of hoger. Zie [](#sound).
- **Audio-uitvoer** > **Toegankelijkheidsaudio resetten na gesprekken** werkt alleen via Shizuku of root. Vereist Android 13 of hoger. Zie [](#sound).
- **Volumetoetsen** > **Sneltoetsen voor volumetoetsen met het scherm uit** werkt alleen via Shizuku, niet via root. Zie [](#buttons).
- **Overzicht** > **Overzicht van SandFly gebruiken** leest en opent de recente apps via Shizuku of root. Zonder deze opent in plaats daarvan het overzicht van het systeem. Alt+Tab, apps openen in gesplitst scherm, en de opdrachten **Oudere app** en **Nieuwere app** hebben ook Shizuku of root nodig. Zie [](#display).
- De schakelaars van het paneel voor snelle instellingen van SandFly, zoals Wi-Fi, Bluetooth, mobiele data en vliegtuigmodus, en de bedieningselementen voor helderheid, automatisch draaien, nachtverlichting, extra dimmen, kleur en weergavegrootte. Zonder Shizuku of root openen sommige schakelaars in plaats daarvan de instellingen van het systeem. Zie [](#display).
- De geschiedenis van het klembord voegt tekst die in andere apps is gekopieerd alleen toe via Shizuku of root, met **Kopieën uit andere apps toevoegen** in de geschiedenis. Zie [](#typing).
- Gboard speelt zijn eigen toetsfeedback, zoals **Trillen bij toetsaanslag**, terwijl je de toetsen verkent, alleen af via Shizuku of root. Zie [](#typing).
- Opdrachten die de snelkoppeling voor toegankelijkheid van een andere toegankelijkheidsservice uitvoeren. Zie [](#commands).
- De opdrachten **App-tegels** en **Systeemtegels**, en **Systeemtegel toevoegen** in de instellingen van het paneel voor snelle instellingen. Zonder Shizuku of root werkt een tegel alleen als deze in de systeeminstellingen voor snelle instellingen staat, en zoekt **Systeemtegel toevoegen** door de systeeminstellingen voor snelle instellingen om de tegels te vinden. Zie [](#commands-categories) en [](#display-quick-settings).
- App-snelkoppelingen die een app toevoegt terwijl deze actief is, en snelkoppelingen die alleen de startscherm-app mag openen, worden alleen via Shizuku of root getoond. Zie [](#gestures-app-shortcuts).
- Scripts en macro's die toetsen indrukken, of tekst typen waar geen tekstveld is, en macrostappen die de hierboven genoemde bedieningselementen van snelle instellingen wijzigen. Zie [](#scripts) en [](#macros).
- **Updates en installatie** > **Weergeven als geïnstalleerd via Google Play**. Zie [](#updates-play-installer).

Wanneer SandFly start met Shizuku of root toegestaan, geeft het zichzelf de machtigingen die de functies die je hebt aangezet nodig hebben.

#### Verleen machtigingen met adb {#updates-adb}

Zonder Shizuku toont een instelling die een adb-machtiging nodig heeft, de adb-opdracht in de beschrijving, en kopieert de actie **Opdracht kopiëren** de opdracht naar het klembord. Voer de opdracht uit vanaf een computer met adb, met het apparaat verbonden en USB-debuggen of draadloos debuggen aan, en ga daarna terug naar de instelling.

De opdrachten zijn:

- Voor **Vastgelopen aanraakverkenning repareren**:

  ```
  adb shell pm grant io.github.aaron_gh.sandfly android.permission.DUMP
  ```

- Voor **Luidspreker wanneer de telefoon niet bij je oor is**:

  ```
  adb shell appops set io.github.aaron_gh.sandfly MANAGE_ONGOING_CALLS allow
  ```

- Voor het verplaatsen van snelkoppelingen voor toegankelijkheid vanuit Backtalk Aaron. Zie [](#getting-started-backtalk).

  ```
  adb shell pm grant io.github.aaron_gh.sandfly android.permission.WRITE_SECURE_SETTINGS
  ```

**Toegankelijkheidsaudio resetten na gesprekken** heeft geen machtiging die adb kan verlenen. Zonder Shizuku toont de instelling een adb-opdracht die de audio-uitvoer voor toegankelijkheid eenmalig reset, voor wanneer spraak na een gesprek via de oorluidspreker speelt. De opdracht noemt de geïnstalleerde APK, dus kopieer deze opnieuw nadat SandFly is bijgewerkt.

### Gebruik root in plaats van Shizuku {#updates-root}

Op een apparaat met root kan SandFly zijn opdrachten uitvoeren met root in plaats van Shizuku. Zet **Shizuku en root** > **Root gebruiken in plaats van Shizuku** aan. Deze instelling staat standaard uit.

SandFly voert de opdrachten dan uit via `su`, als de shell-gebruiker, de gebruiker die adb en Shizuku gebruiken. De rootmanager van het apparaat, zoals Magisk of KernelSU, toont een dialoogvenster om root aan SandFly te verlenen. Functies die Shizuku nodig hebben, gebruiken root terwijl deze instelling aan staat, behalve **Sneltoetsen voor volumetoetsen met het scherm uit**, die niet wordt getoond in rootmodus. Wanneer `su` geen root verleent, zeggen het overzicht en gesplitst scherm "Geen roottoegang".

Opmerking: Als `su` op het apparaat geen opdrachten kan uitvoeren als de shell-gebruiker, voert SandFly ze uit als root. Kopieën uit andere apps toevoegen aan de klembordgeschiedenis en de schakelaar voor de hotspot werken dan niet.
