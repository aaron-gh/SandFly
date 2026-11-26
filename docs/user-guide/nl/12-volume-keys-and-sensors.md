## Volumetoetsen en sensoren {#buttons}

**Volumetoetsen** in Instellingen voor SandFly stelt in wat de volumetoetsen doen terwijl SandFly aanstaat: schuifregelaars aanpassen, de cursor bewegen, opdrachten uitvoeren, en de feedback bij volumewijzigingen. Het laatste deel van dit hoofdstuk noemt de functies die de sensoren van de telefoon gebruiken.

### Volumetoetsen en spraakvolume {#buttons-speech-volume}

Om het spraakvolume vanaf elk scherm te wijzigen, houd je een vinger op het scherm en druk je op volume omhoog of volume omlaag. De volumetoetsen wijzigen ook het spraakvolume tijdens doorlopend lezen. Het spraakvolume en de andere volume-instellingen worden beschreven in [](#sound).

Beide volumetoetsen samen indrukken voert geen SandFly-opdracht uit, zodat de toegankelijkheidssnelkoppeling van Android blijft werken. Dit ontgrendelt ook de aanraking; zie [](#touch-lock).

### Schuifregelaars aanpassen en de cursor bewegen {#buttons-sliders-cursor}

- **Volumetoetsen passen schuifregelaars aan**: als een schuifregelaar focus heeft, wijzigen volume omhoog en volume omlaag de waarde ervan in plaats van het volume, en SandFly spreekt de nieuwe waarde uit. Houd een toets ingedrukt om de waarde te blijven wijzigen. Standaard uit.
- **Volumetoetsen verplaatsen de cursor**: als een tekstveld focus heeft, beweegt volume omhoog de cursor een teken terug en volume omlaag een teken verder, en SandFly spreekt het teken uit. Standaard uit.

Als een toets ook een opdracht heeft voor een van zijn drukken (zie [](#buttons-commands)), voeren de drukken hun opdrachten uit, en een druk zonder opdracht past de schuifregelaar aan of beweegt de cursor zodra hij eindigt, in plaats van het volume te wijzigen.

Als de focus op iets anders staat, of het scherm uit is, wijzigen de volumetoetsen het volume zoals gewoonlijk. Met een vinger op het scherm of tijdens doorlopend lezen wijzigen ze nog steeds het spraakvolume.

### Opdrachten uitvoeren met de volumetoetsen {#buttons-commands}

Elke volumetoets kan een opdracht uitvoeren voor twee keer indrukken, drie keer indrukken en lang indrukken: **Volume omhoog twee keer indrukken**, **Volume omhoog drie keer indrukken**, **Volume omhoog lang indrukken**, **Volume omlaag twee keer indrukken**, **Volume omlaag drie keer indrukken** en **Volume omlaag lang indrukken**. Standaard zijn ze allemaal niet toegewezen.

Een opdracht aan een druk toewijzen:

1. Ga naar **Volumetoetsen**.
2. Kies de druk, zoals **Volume omlaag twee keer indrukken**.
3. Kies een opdracht uit de lijst, dezelfde lijst als voor gebaren. Zie [](#commands).

Een toets zonder opdracht voor welke druk dan ook wijzigt direct het volume, zoals gewoonlijk. Als een toets een opdracht heeft voor twee of drie keer indrukken, wijzigt een enkele druk het volume 0,3 seconden na het loslaten van de toets, zodra er geen verdere druk volgt. Als een toets een opdracht heeft voor lang indrukken, voert vasthouden de opdracht uit in plaats van het volume herhaaldelijk te wijzigen.

**Duur van lang indrukken van volumetoets** stelt in hoe lang je een toets vasthoudt voor de lange druk: **Systeemstandaard**, of van 0,3 tot 1 seconde.

De drukken voeren geen opdrachten uit, en de toetsen wijzigen het volume zoals gewoonlijk, in deze gevallen:

- Terwijl het scherm uit is, tenzij het vergrendelscherm wordt getoond op een always-on-display, of **Sneltoetsen voor volumetoetsen met het scherm uit** aanstaat. Zie [](#buttons-screen-off).
- Tijdens een telefoongesprek, of terwijl een app gespreksaudio afspeelt.
- Terwijl een vinger op het scherm staat, of tijdens doorlopend lezen.
- Terwijl de app Shortcut Menu wordt getoond.

Terwijl een gesprek overgaat, voert alleen een volumetoets met de opdracht **Gesprek beantwoorden** of **Gesprek beëindigen** toegewezen aan een van zijn drukken, opdrachten uit.

### Volumetoetsdrukken van Android weghouden {#buttons-take-all}

- **SandFly verwerkt elke druk op een volumetoets**: terwijl het scherm aanstaat, gaat elke druk op een volumetoets naar SandFly. Drukken met een opdracht voeren die uit, en andere drukken, ook een toets vasthouden, wijzigen geen volume. Terwijl deze instelling aanstaat, verschijnen **Volume omhoog één keer indrukken** en **Volume omlaag één keer indrukken**, en voeren ze een opdracht uit voor een enkele druk. Standaard uit.
- **Volumepaneel verbergen**: terwijl het scherm aanstaat, wijzigen drukken zonder opdracht het volume zonder het volumepaneel van Android te tonen. Standaard uit. Met beide instellingen aan wijzigen drukken zonder opdracht geen volume.

Beide instellingen laten de toetsen aan Android terwijl het scherm uit is, tijdens een telefoongesprek of terwijl een app gespreksaudio afspeelt, terwijl een gesprek overgaat, terwijl SandFly is gepauzeerd, en als beide volumetoetsen worden vastgehouden. Schuifregelaars aanpassen, de cursor bewegen en het spraakvolume met een vinger op het scherm blijven werken.

### Sneltoetsen voor volumetoetsen met het scherm uit {#buttons-screen-off}

Met **Sneltoetsen voor volumetoetsen met het scherm uit** aan voeren de drukken op volumetoetsen met opdrachten die uit terwijl het scherm uit is. Drukken zonder opdracht wijzigen het volume. De instelling vereist Shizuku, en wordt niet getoond als SandFly root gebruikt in plaats van Shizuku, of op smartwatches; zie [](#updates-shizuku). De eerste keer dat je deze instelling aanzet, toont Shizuku een verzoek om SandFly toe te staan. Standaard uit.

### Dempknop {#buttons-mute-key}

Op telefoons met een dempknop, zoals de Zinwa Q25, heeft **Volumetoetsen** nog twee instellingen:

- **Dempknop ingedrukt houden om te dicteren in tekstvelden**: als een tekstveld invoerfocus heeft, een toetsenbord wordt getoond of is aangesloten, en er geen gesprek actief is, voert het vasthouden van de dempknop **Dicteren** uit: de opname begint als je op de knop drukt en stopt als je de knop loslaat, en SandFly typt de tekst. Een druk op de knop tijdens een dictee in een tekstveld stopt dat dictee. Standaard aan. Zie [](#typing-dictate).
- **Dempknop indrukken**: de opdracht die een druk op de dempknop overal elders uitvoert. De lijst is dezelfde als voor gebaren. Standaard niet toegewezen.

Als geen van beide instellingen een druk gebruikt, gaat de druk naar de keymap, waar de dempknop een eigen sneltoets kan zijn in de toetsenbordtelefoon-keymaps en in eigen keymaps (zie [](#keyboards-keymaps)), en daarna naar Android.

### Spraakvolume op Samsung-telefoons {#buttons-samsung}

Op Samsung-telefoons wijzigt One UI het spraakvolume als je op een volumetoets drukt terwijl SandFly spreekt, en tot een seconde daarna. SandFly houdt deze drukken weg van One UI en wijzigt het volume dat de toetsen wijzigen als SandFly stil is, zoals het mediavolume.

Om de volumetoetsen het spraakvolume te laten wijzigen terwijl SandFly spreekt, zet je **Volumetoetsen wijzigen spraakvolume tijdens spreken** aan. Deze instelling wordt alleen getoond op Samsung-telefoons, en staat standaard uit.

### Feedback bij volumewijziging {#buttons-feedback}

**Feedback bij volumewijziging** stelt in wat SandFly afspeelt als een volume wijzigt:

- **Uit**.
- **Toon**: een korte toon bij elke volumestap, op het volume dat wordt gewijzigd.
- **Spraak**: het volume dat is gewijzigd en het nieuwe niveau, zoals "Mediavolume ingesteld op 60 procent", na de laatste stap. Dit is de standaardinstelling.
- **Toon en spraak**.

Met een toon zet **Toon dempen tijdens afspelen van media** de toon uit terwijl een app media afspeelt. Met spraak zet **Spraak dempen tijdens afspelen van media** de aankondiging uit terwijl een app media afspeelt, en **Spraak onderbreken om het volume te melden** stopt de spraak die wordt afgespeeld om het volume aan te kondigen. De twee media-schakelaars staan standaard uit, en **Spraak onderbreken om het volume te melden** staat standaard aan.

Met spraak laat **Spraakvolume stil wijzigen tijdens spreken** de aankondiging weg wanneer de volumetoetsen het spraakvolume wijzigen terwijl SandFly spreekt of spraak in de wachtrij heeft, zodat spraak doorgaat op het nieuwe volume. Drukken die elkaar binnen een seconde opvolgen, tellen als één reeks, die stil blijft nadat de spraak eindigt. De toon speelt nog wel, indien gekozen. Drukken terwijl een vinger op het scherm staat, drukken terwijl SandFly stil is, en wijzigingen vanuit het volumepaneel of apps worden wel aangekondigd. Op Samsung-telefoons wijzigen de toetsen, buiten doorlopend lezen, het spraakvolume terwijl SandFly spreekt alleen met **Volumetoetsen wijzigen spraakvolume tijdens spreken** aan. Standaard uit.

Volumewijzigingen die SandFly maakt, zoals met een volumeopdracht, krijgen geen feedback. Volumewijzigingen die andere apps maken, krijgen die wel. Er is geen toon tijdens een gesprek of terwijl SandFly is gepauzeerd.

### Sensoren {#buttons-sensors}

Deze functies gebruiken de sensoren van de telefoon. Elke functie wordt ingesteld op het hier genoemde scherm.

- **Tikken op achterkant**: twee, drie of vier tikken op de achterkant van de telefoon voeren opdrachten uit. **Gebaren** > **Tikken op achterkant**; zie [](#gestures-back-tap).
- **Schudden**: de telefoon schudden voert een opdracht uit. **Gebaren** > **Schudden**; zie [](#gestures-shake).
- **Afstandssensor afdekken om spraak te stoppen**: de afstandssensor afdekken stopt de spraak. Standaard uit. **Spraak** > **Afstandssensor afdekken om spraak te stoppen**; zie [](#speech-stop).
- **Aanraking automatisch vergrendelen** > **Terwijl de nabijheidssensor is afgedekt**: de aanraking is vergrendeld terwijl iets de afstandssensor bedekt. Zie [](#touch-lock).
- **Schud om te stoppen** en **Zwaaien om te stoppen**: de telefoon schudden, of een hand over de afstandssensor bewegen, stopt het voorlezen van een melding. **Meldingen** > **Stop met lezen**; zie [](#notifications-stop).
- **Luidspreker wanneer de telefoon niet bij je oor is**: een gesprek gaat naar de luidspreker als je de telefoon van je oor afhaalt, en terug naar de oorspreker als je hem weer tegen je oor houdt. **Audio-uitvoer** > **Luidspreker wanneer de telefoon niet bij je oor is**. Dit vereist Android 12 of hoger. Zie [](#sound-calls).
