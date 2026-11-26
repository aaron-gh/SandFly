## Aanraking {#touch}

**Aanraking** in de instellingen voor SandFly stelt in hoe het aanraken van het scherm items focust en activeert, wanneer aanraking is vergrendeld, en welke apps aanraking direct krijgen. Gebaren worden beschreven in [](#gestures), en het scherm lezen door aanraking in [](#reading).

### Items focussen en activeren {#touch-activation}

Standaard focust het aanraken van een item dit en leest SandFly het voor, en activeert een dubbeltik ergens op het scherm het gefocuste item. De instellingen in deze sectie veranderen dit.

#### Focusvertraging {#touch-focus-delay}

**Aanraking** > **Focusvertraging** stelt in hoe lang de vinger op een item rust voordat SandFly het focust en voorleest, van 0,15 tot 2 seconden. De standaardwaarde is 0,15 seconden. Een kortere vertraging laat SandFly eerder spreken na het aanraken van het scherm. Heeft Android 13 of hoger nodig.

**Focusvertraging** is ook een leesinstelling, standaard uit; zie [](#commands).

#### Tik 1 keer om te activeren {#touch-single-tap}

Met **Aanraking** > **Tik 1 keer om te activeren** aan, activeert een enkele tik op het gefocuste item dit, zonder dubbeltikken. Dit staat standaard uit. Dit heeft verkennen via aanraking nodig, en staat niet op tv's.

#### Optillen om te activeren {#touch-lift-to-activate}

Met optillen om te activeren activeert het optillen van de vinger na het aanraken van een item dit, zoals het optillen van de vinger een toets van het schermtoetsenbord typt. **Aanraking** > **Optillen om te activeren** heeft drie keuzes:

- **Uitgeschakeld**: de standaard.
- **Alleen op de navigatiebalk**: alleen de knoppen Terug, Home en Overzicht.
- **Hele scherm**: elk item.

**Optillen om te activeren** is ook een leesinstelling, standaard uit, zodat je deze kunt veranderen met een swipe omhoog of omlaag; zie [](#commands).

#### Een activatie door optillen annuleren {#touch-lift-cancel}

Terwijl **Optillen om te activeren** aan staat, zijn er twee manieren om de vinger op te tillen zonder te activeren:

- Beweeg de vinger van de onderkant van het scherm af. Optillen in de onderste 3,5 mm van het scherm activeert niets, ook voor toetsen van het toetsenbord die typen wanneer je optilt. SandFly speelt een geluid en een trilling wanneer de vinger de onderrand bereikt.
- Houd de vinger op het item. **Aanraking** > **Vasthouden om te annuleren** stelt in hoe lang: **1 seconde**, **2 seconden**, **3 seconden** of **5 seconden**. Na die tijd op één item activeert het optillen van de vinger niets, en speelt SandFly hetzelfde geluid en dezelfde trilling. De standaardwaarde is **Uit**. Toetsen van het toetsenbord die typen wanneer je optilt, behouden hun eigen tikken en vasthouden.

#### Tikken om te activeren {#touch-tap-to-activate}

Met **Aanraking** > **Tikken om te activeren** aan, activeert een snelle tik die optilt voordat het item onder de vinger wordt gefocust, dat item. Terwijl de focus de laatste 3 seconden is bewogen, wacht een tik eerst op een tweede tik, zodat een dubbeltik het gefocuste item nog steeds activeert. Dit staat standaard uit.

#### Navigatiebalkknoppen {#touch-navigation-bar}

Met **Aanraking** > **Tikken om navigatiebalkknoppen te activeren** aan, gaan aanrakingen op de navigatiebalk rechtstreeks naar Android: een enkele tik drukt op een knop, en Home vasthouden houdt deze vast. Dit staat standaard uit en heeft Android 11 of hoger nodig. Terwijl dit aan staat, is **Optillen om te activeren** niet van toepassing op de navigatiebalk.

SandFly leest de knoppen niet voor terwijl je ze aanraakt, maar spreekt de knop uit waarop een tik heeft gedrukt, zoals "Terug" of "Home". Om dit uit te zetten, zet **Aanraking** > **Spreek navigatiebalkknoppen uit** uit, wat standaard aan staat.

#### Activeren met een tik {#touch-activate-with-tap}

Sommige bedieningselementen negeren de activatie die SandFly stuurt en reageren alleen op een vinger op het scherm. De opdracht **Activeren met een tik**, standaard niet toegewezen, tikt in plaats daarvan op het midden van het gefocuste item. Wijs dit toe aan een gebaar of sneltoets; zie [](#gestures). Een script kan hetzelfde doen voor gekozen items telkens wanneer ze worden geactiveerd; zie [](#scripts). Beide hebben Android 11 of hoger nodig; anders wordt het item als gewoonlijk geactiveerd.

Voor apps waarin het activeren van een item niets doet, zet je de app aan onder **Aanraking** > **Dubbeltikken tikt op het scherm**, of voer je in de app de opdracht **Dubbeltikken tikt op het scherm in deze app** uit, standaard niet toegewezen. Het activeren van een item in die app tikt het dan aan. De opdracht zegt "Dubbeltikken tikt op het scherm aan" of "Dubbeltikken tikt op het scherm uit", of "Geen app op de voorgrond" als er geen app op de voorgrond is. Dit heeft ook Android 11 of hoger nodig, en is niet beschikbaar op horloges of tv's.

#### Spraak terwijl je het scherm aanraakt {#touch-speech}

Standaard stopt het aanraken van het scherm de spraak. Om de spraak te laten doorgaan tot aanraking een item focust, zet **Spraak** > **Spraak stoppen bij aanraken van het scherm** uit. Zie [](#speech).

### Aanraking vergrendelen {#touch-lock}

Terwijl aanraking is vergrendeld, doen aanrakingen en gebaren op het scherm niets, en bereikt niets de app. Toetsen, opdrachten voor volumetoetsen, vingerafdrukgebaren, tikken op de achterkant en schudden werken nog wel. Opdrachten die het scherm aanraken, zoals **Activeren met een tik**, **Aanraking op het gefocuste item vasthouden** en aanrakingen in macro's, bereiken de app nog wel. Aanraking vergrendelen heeft Android 13 of hoger nodig en **Ontwikkelaarsinstellingen** > **Gebaren verwerken in SandFly**.

Om aanraking te vergrendelen of te ontgrendelen, gebruik de opdracht **Aanraking vergrendelen of ontgrendelen**. SandFly zegt "Aanraking vergrendeld" of "Aanraking ontgrendeld". Wijs de opdracht toe aan een volumetoetsdruk, een sneltoets of een tik op de achterkant, zodat je aanraking kunt ontgrendelen zonder het scherm; zie [](#buttons) en [](#gestures-back-tap). Beide volumetoetsen samen indrukken ontgrendelt aanraking altijd.

**Aanraking** > **Aanraking automatisch vergrendelen** vergrendelt aanraking zonder de opdracht:

- **Nooit**: de standaard.
- **Als het scherm aangaat**: elke keer dat het scherm aangaat. Ontgrendel met de opdracht of beide volumetoetsen.
- **Op het vergrendelscherm**: tot je de telefoon ontgrendelt.
- **Terwijl de nabijheidssensor is afgedekt**: terwijl het scherm aan staat en iets de afstandssensor afdekt, zoals een oor of een zak.

SandFly spreekt niets uit wanneer aanraking automatisch vergrendelt.

**Aanraking** > **Aanrakingen terwijl aanraking is vergrendeld** stelt in wat een aanraking op het vergrendelde scherm geeft: **Niets**, de standaard, **Geluid** of **Trilling**.

Scripts kunnen aanraking ook tot 10 seconden vergrendelen; zie [](#scripts).

### Directe aanraking {#touch-direct-touch}

Sommige apps, zoals audiogames, hebben aanrakingen nodig zoals ze worden gemaakt, maar verkennen via aanraking neemt de tikken en swipes eerst. Met directe aanraking geeft SandFly aanrakingen rechtstreeks door aan de apps die je kiest, terwijl SandFly aan blijft en blijft spreken. Directe aanraking heeft Android 11 of hoger nodig en staat niet op horloges of tv's.

#### Directe aanraking inschakelen voor een app {#touch-direct-touch-apps}

1. Ga naar **Aanraking** > **Directe aanraking**.
2. Zet onder **Apps** elke app aan die directe aanraking krijgt.

Wanneer een gekozen app naar de voorgrond komt, zegt SandFly "Directe aanraking aan", en wanneer de aanraking terugkeert naar SandFly, "Directe aanraking uit".

Het scherm **Directe aanraking** heeft:

- **Directe aanraking**: zet de functie aan of uit voor alle apps. Standaard aan.
- **Uitspreken wanneer directe aanraking verandert**: standaard aan.
- **Trillen wanneer directe aanraking verandert**: standaard uit.
- **Back-up maken van instellingen** en **Instellingen herstellen**: slaan de instellingen voor directe aanraking op in een bestand en laden ze, zoals op een ander apparaat.
- **Apps**: de apps en schermtoetsenborden, elk met een selectievakje.

De opdracht **Directe aanraking voor deze app aan- of uitzetten** voegt de app op de voorgrond toe aan de lijst of verwijdert deze, en zet **Directe aanraking** aan wanneer deze een app aanzet. Wijs dit toe aan een volumetoetsdruk, zodat een ziende helper een app kan gebruiken die niet werkt met SandFly terwijl SandFly blijft spreken, en jij directe aanraking kunt uitzetten zonder het scherm aan te raken. Met SandFly of het meldingenscherm op de voorgrond zegt SandFly "Geen app voor directe aanraking".

Om directe aanraking uit en weer aan te zetten zonder de app te verlaten, voeg de tegel **Directe aanraking** toe aan de snelle instellingen.

#### Wanneer SandFly aanraking teruggeeft {#touch-direct-touch-return}

Aanraking keert terug naar SandFly wanneer een dialoogvenster opent, een andere app erboven opent, het meldingenscherm of de snelle instellingen de helft of meer van het scherm bedekken, een tekstveld is gefocust, of het scherm uitgaat. Aanraking gaat terug naar de app wanneer deze weg zijn. Het schermtoetsenbord blijft altijd bij SandFly, zodat je het kunt verkennen terwijl de rest van het scherm directe aanraking heeft.

#### Direct typen {#touch-direct-typing}

Voor een app die zijn eigen toetsenbord tekent, zet direct typen aan: open in de lijst **Apps** de acties van de app en kies **Direct typen aanzetten**. Het toetsenbordgebied krijgt dan ook directe aanraking, en de app in de lijst toont "Direct typen aan". **Direct typen uitzetten** zet dit uit.

#### Toetsenborden die spreken {#touch-direct-touch-keyboards}

Een toetsenbord dat zelf spreekt, zoals een app met een brailletoetsenbord, heeft aanrakingen in elke app nodig. Zet dit aan in de lijst **Apps**. Terwijl dit wordt getoond, gaan aanrakingen erop rechtstreeks naar het toetsenbord in elke app, en blijft de rest van het scherm bij SandFly. Een toetsenbord dat de toetsen in een toegankelijkheidsoverlay tekent en een eigen toegankelijkheidsservice heeft die aan staat, behoudt het aanraakgebied dat het instelt, zonder in de lijst te staan.

#### Apps die kiezen waar ze directe aanraking krijgen {#touch-direct-touch-areas}

Een app kan aangeven dat deze directe aanraking nodig heeft. SandFly zet zo'n app de eerste keer aan wanneer deze naar de voorgrond komt; je kunt dit uitzetten in de lijst **Apps**. Een app kan ook directe aanraking alleen op delen van zijn scherm nemen, zoals een spelbord, terwijl verkennen via aanraking op de rest werkt, en kan directe aanraking voor zichzelf stoppen. Deze apps werken alleen terwijl ze zijn aangezet in de lijst **Apps**. Voor ontwikkelaars wordt de interface beschreven in [](#api-direct-touch).

### Vastgelopen aanraakverkenning repareren {#touch-stuck}

Op sommige telefoons kan Android, na een aanraking met meerdere vingers, verkennen via aanraking stoppen voor elke schermlezer tot de telefoon opnieuw start. Swipes en tikken werken nog wel, maar het bewegen van een vinger over het scherm leest niets voor.

**Aanraking** > **Vastgelopen aanraakverkenning repareren**, standaard aan, merkt dit op aan verkennende aanrakingen die geen item vinden. Om dit te repareren, heeft SandFly de machtiging DUMP nodig:

- Met Shizuku actief verleent SandFly de machtiging wanneer het start, en wanneer je de instelling aanzet; de eerste keer toont Shizuku een verzoek om SandFly toe te staan. Zie [](#updates).
- Zonder Shizuku, verleen deze eenmalig met adb. Terwijl de machtiging ontbreekt, toont de instelling het commando, en kopieert de actie **Opdracht kopiëren** dit:

```
adb shell pm grant io.github.aaron_gh.sandfly android.permission.DUMP
```

Met de machtiging zegt SandFly "Aanraakverkenning repareren", beëindigt de aanrakingen die Android ingedrukt liet, en zegt "Aanraakverkenning opgelost". Een aanraking tijdens de reparatie gaat rechtstreeks naar de app. Dit werkt op telefoons die deze aanrakingen vermelden in `dumpsys input`, zoals Samsung-telefoons.

Zonder de machtiging, of op andere telefoons, kan SandFly alleen zeggen dat de aanraakverkenning mogelijk is vastgelopen, en moet je de telefoon opnieuw starten. Het verkennen van een gebied zonder iets om voor te lezen, zoals een spel, vindt ook geen item, zodat de waarschuwing onterecht kan zijn, hoogstens eens per 10 minuten.

Als het scherm uitgaat tijdens een aanraking, kan Android de opgetilde vingers als ingedrukt blijven beschouwen, en dan werkt alleen aanraakverkenning: gebaren en dubbeltikken doen niets. SandFly detecteert dit als het scherm uitgaat tijdens een aanraking, of als twee aanrakingen achter elkaar in aanraakverkenning eindigen, en zet Verkennen via aanraking uit en weer aan. Een aanraking die eindigt doordat de volgende vinger neerkomt, zoals bij snel typen, telt niet mee. Dit hangt niet af van **Vastgelopen aanraakverkenning repareren** of de DUMP-machtiging.

### Alle instellingen voor vertraging resetten {#touch-reset-delays}

**Aanraking** > **Alle instellingen voor vertraging resetten** zet deze instellingen terug op hun standaardwaarden, na het dialoogvenster **Instellingen voor vertraging resetten?**:

- **Aanraking** > **Tik 1 keer om te activeren** en **Focusvertraging**.
- **Weergave** > **Animaties uitschakelen**.
- **Typen** > **Typemethode**, **Vertraging voor tikken en vasthouden tijdens het typen**, **Focusvertraging bij typen** en **Toetsenbordtoetsen sneller aankondigen**.
- **Navigatie** > **Items uitspreken voordat ze in beeld scrollen**.
- Of de leesinstellingen **Focusvertraging** en **Focusvertraging bij typen** worden getoond.

Zie [](#typing) voor de typinstellingen.
