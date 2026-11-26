## Horloges en tv's {#devices}

SandFly draait op Wear OS-horloges en op Android TV en Google TV. Dit hoofdstuk beschrijft hoe SandFly op deze apparaten verschilt van telefoons. Al het andere in deze handleiding geldt ook voor deze apparaten, behalve functies die een aanraakscherm, een telefoon, of hardware nodig hebben die het apparaat niet heeft.

### Wear OS-horloges {#devices-watch}

Horloges hebben een eigen SandFly-build, `sandfly-wear.apk`. Deze heeft dezelfde instellingen als telefoons, behalve instellingen voor functies die horloges niet ondersteunen.

#### Installeer SandFly op een horloge {#devices-watch-install}

Je installeert `sandfly-wear.apk` vanaf een computer met adb.

1. Download `sandfly-wear.apk` van de [laatste release](https://github.com/aaron-gh/SandFly/releases/latest) naar de computer.
2. Zet op het horloge de ontwikkelaarsopties aan, en zet daarna **ADB-debuggen** en draadloos debuggen aan. De stappen hangen af van het horloge. Op de meeste horloges tik je zeven keer op het buildnummer in de instellingen **Info** om de ontwikkelaarsopties te tonen.
3. Verbind op de computer met het horloge met `adb connect` en het adres dat de instelling voor draadloos debuggen van het horloge toont. Sommige horloges moeten eerst worden gekoppeld met `adb pair`.
4. Voer uit:

   ```
   adb install sandfly-wear.apk
   ```

5. Zet op het horloge SandFly aan in de toegankelijkheidsinstellingen.

Om Instellingen voor SandFly op een horloge te openen, open je **SandFly** in de lijst met apps van het horloge. De eerste keer dat SandFly wordt aangezet, opent het de tutorial voor horloges.

#### Gebaren op horloges {#devices-watch-gestures}

Horlogeschermen volgen maximaal twee vingers, dus gebaren met drie of vier vingers zijn niet beschikbaar. Swipes met 2 vingers gaan naar de app, waar ze scrollen.

Horloges gebruiken de standaardgebaren van telefoons, behalve:

| Gebaar | Opdracht op horloges |
| --- | --- |
| Dubbeltikken en vasthouden met 2 vingers | SandFly-menu openen |
| 3 keer tikken en vasthouden met 2 vingers | Spraak aan- of uitzetten |
| Naar links en omhoog swipen | Laatst uitgesproken woordgroep herhalen |
| Naar rechts en omhoog swipen | Volume verhogen |
| Naar rechts en omlaag swipen | Volume verlagen |
| Naar links en omlaag swipen | Niet toegewezen |

Voor de standaardgebaren van telefoons, zie [](#appendix-gestures). Om een gebaar te wijzigen, ga je naar **Gebaren**. Zie [](#gestures).

#### Instellingen en functies op horloges {#devices-watch-settings}

Op horloges:

- Bevatten de leesinstellingen standaard **Scrollen** en **Toegankelijkheidsvolume**. Zie [](#reading).
- Scrolt het SandFly-menu, zodat elk item bereikt kan worden. **Scherm beschrijven** staat standaard niet in het menu. Het item dat SandFly uitzet, heet **Uitzetten**. Zie [](#commands).
- Heet **Nieuwe functies in SandFly** **Wat is er nieuw**.
- Is SandFly pauzeren niet beschikbaar, en wordt **SandFly hervatten** niet getoond. Zie [](#pause).
- Staat **Gebaren oefenen** niet in **Hulp en feedback**. Gebruik **Gebaren oefenen** boven aan **Gebaren**, dat de gebaaroefening van de tutorial voor horloges opent.
- Staan **Gebarentrainer** en **Toetsenbordoefening** niet in **Hulp en feedback**.
- Tonen **Versiegeschiedenis** en **Privacybeleid** het adres van de pagina wanneer het horloge geen browser heeft.

Deze functies zijn niet beschikbaar op horloges, en hun instellingen worden niet getoond: scripts, macro's, aangepaste menu's, aangepaste leesinstellingen, spraakopdrachten, directe aanraking, tikken op de achterkant, schudden, instellingen voor rand- en hoekgebaren, het brailletoetsenbord, het handschrifttoetsenbord, het SandFly-toetsenbord, AI op het apparaat, **SandFly als achtergrond gebruiken**, **Luidspreker wanneer de telefoon niet bij je oor is**, **Toegankelijkheidsaudio resetten na gesprekken**, **Sneltoetsen voor volumetoetsen met het scherm uit**, en **Instructies tonen om SandFly uit te zetten**.

Voor meldingen op Samsung-horloges, zie [](#notifications).

#### Updates op horloges {#devices-watch-updates}

Een horloge update naar nieuwe builds van `sandfly-wear.apk`, zoals beschreven in [](#updates). Het horloge downloadt de update via zijn eigen Wi-Fi wanneer Wi-Fi beschikbaar is, niet via de telefoon, en toont de voortgang in een melding. Als de installatiemelding niet opent, zoals wanneer het scherm uitging tijdens de download, tik je op de melding **SandFly-update gedownload**.

### Android TV {#devices-tv}

Dezelfde `sandfly.apk` als voor telefoons installeert op Android TV en Google TV.

#### Installeer SandFly op een tv {#devices-tv-install}

1. Zet op de tv de ontwikkelaarsopties en netwerkdebuggen aan. De stappen hangen af van de tv.
2. Verbind op een computer in hetzelfde netwerk met de tv met `adb connect` en het IP-adres van de tv.
3. Download `sandfly.apk` van de [laatste release](https://github.com/aaron-gh/SandFly/releases/latest) en voer uit:

   ```
   adb install sandfly.apk
   ```

4. Zet op de tv SandFly aan in de toegankelijkheidsinstellingen. Op de meeste tv's staan deze in **Instellingen** > **Apparaatvoorkeuren** > **Toegankelijkheid**.

Op veel tv's zet je SandFly aan of uit door Terug en Omlaag op de afstandsbediening samen 3 seconden ingedrukt te houden. Dit is de snelkoppeling voor toegankelijkheid van de tv, die je ook kunt instellen in de instellingen van de tv.

Om Instellingen voor SandFly te openen, kies je **Instellingen voor SandFly** in de apps op het startscherm van de tv.

#### Gebruik de afstandsbediening {#devices-tv-remote}

Op een tv verplaatst de afstandsbediening de focus:

- De pijlknoppen verplaatsen de focus naar het volgende item omhoog, omlaag, links of rechts.
- Selecteren activeert het gefocuste item. Wanneer het gefocuste item niet kan worden geactiveerd, gaat Selecteren naar de app.
- Houd Selecteren ingedrukt om het gefocuste item te tikken en vast te houden.
- Op een schuifregelaar start Selecteren de schuifregelaarmodus, en zegt SandFly "Schuifregelaarmodus gestart". De pijlknoppen wijzigen dan de waarde. Druk opnieuw op Selecteren om de schuifregelaarmodus te beëindigen.
- Met **Ontwikkelaarsinstellingen** > **Foutopsporing voor knooppuntstructuren aanzetten** aan schrijft de zoekknop de boomstructuur van items op het scherm naar het systeemlogboek. Zie [](#help-developer).

SandFly behandelt de pijl- en selectieknoppen niet terwijl het schermtoetsenbord wordt getoond. Voor fysieke toetsenborden, zie [](#keyboards).

#### Instellingen en functies op tv's {#devices-tv-settings}

Op tv's:

- Worden instellingen voor functies die een aanraakscherm of een telefoon nodig hebben niet getoond, zoals **Gebaren**, **Gebarentrainer**, **Directe aanraking**, **Status voorlezen**, **Overzicht van SandFly gebruiken**, **Meldingenlijst van SandFly gebruiken**, **Luidspreker wanneer de telefoon niet bij je oor is** en **Afbeeldings- en schermbeschrijvingen**. De instellingenschermen **SandFly-menu** en **Leesinstellingen** worden ook niet getoond.
- Is SandFly pauzeren niet beschikbaar, en wordt **SandFly hervatten** niet getoond. Zie [](#pause).
- Opent het eerste item in **Hulp en feedback** de tutorial van de tv waar de tv er een heeft. Anders heet het **Hulp**.
- Tonen tv's vaak geen meldingen, dus de updatemelding verschijnt mogelijk niet. Om te updaten, ga je naar **Updates en installatie** > **Controleren op updates**. Zie [](#updates-check).
