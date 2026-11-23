## Verbositeit {#verbosity}

Het scherm **Verbositeit** in de instellingen van SandFly bepaalt wat SandFly zegt over items, tekst en symbolen, tabellen en veranderingen op het scherm, en wat het weglaat voor apps die zelf spreken. Het heeft de groepen **Items**, **Tekst en symbolen**, **Tabellen**, **Aankondigingen** en **Apps met hun eigen spraak**.

Hoe tekst wordt uitgesproken, zoals fonetische letters en het spraakwoordenboek, staat op het scherm **Spraak**. Zie [](#speech). Toetsenbordecho staat op het scherm **Typen**. Zie [](#typing).

De opdracht **Verbositeit** opent dit scherm. Je kunt ook een item **Verbositeit** toevoegen aan het SandFly-menu. Zie [](#commands).

### Items {#verbosity-items}

De groep **Items** bepaalt wat SandFly over elk item zegt:

- **Gebruikstips uitspreken**: zegt, na het beschrijven van een item, hoe je het gebruikt, zoals "Dubbeltik om te activeren". Standaard aan.
- **Elementtype uitspreken**: zegt het type van een item, zoals "Knop" of "Keuzevakje". Standaard aan.
- **Itemtype uitspreken zonder pauze**: voegt de naam en het type van een item samen met een spatie in plaats van een pauze. Standaard uit.
- **Lijstinformatie uitspreken**: zegt lijstposities en lijstdetails. Standaard aan.
- **Raster- en tabelinformatie uitspreken**: zegt rijen en kolommen in rasters, tabellen en pagineringen die in rijen en kolommen zijn ingedeeld, zoals een map op een startscherm. Standaard aan. De instellingen van **Tabellen** gelden alleen terwijl dit aan staat. Zie [](#verbosity-tables).
- **Volgorde van itemgegevens**: de volgorde van de naam, het type en de status van een item. Zie [](#verbosity-item-order).
- **Element-ID's voorlezen**: zegt de ID van knoppen zonder label. Standaard uit.

#### Verander de volgorde van itemgegevens {#verbosity-item-order}

**Verbositeit** > **Volgorde van itemgegevens** toont **Status**, **Naam** en **Type** in de volgorde waarin SandFly ze uitspreekt. De standaardwaarde is **Status, naam, type**.

Om een detail te verplaatsen, doe je een van de volgende dingen:

- Tik op het detail en houd vast, en sleep het omhoog of omlaag. Met SandFly aan, dubbeltik je op het detail en houd je vast, en sleep je het.
- Gebruik de acties **Omhoog verplaatsen** en **Omlaag verplaatsen** van het detail.

SandFly kondigt de positie van het detail aan terwijl je het oppakt, verplaatst en neerzet.

De volgorde geldt ook voor rijen waarvan het keuzevakje of de schakelaar geen eigen tekst heeft, die SandFly als één bedieningselement voorleest met de tekst van de rij als naam, en voor knoppen en tabbladen waarvan het label een los item erbinnen is.

### Tekst en symbolen {#verbosity-text}

De groep **Tekst en symbolen** bepaalt hoe tekst, interpunctie, hoofdletters en emoji worden uitgesproken:

- **Interpunctie en symbolen uitspreken**: **Alle** zegt elk leesteken en symbool. **Meeste** zegt de meeste symbolen, zoals & en @, maar laat gewone interpunctie, zoals komma's en apostroffen, aan de spraakengine over. **Sommige**, de standaardwaarde, laat interpunctie en symbolen aan de spraakengine over. **Geen** laat interpunctie en symbolen weg uit spraak, behalve een teken dat los wordt voorgelezen, zoals bij het verplaatsen per teken. Apostroffen en koppeltekens binnen woorden, scheidingstekens binnen getallen en interpunctie die een woord afsluit, worden aan de spraakengine overgelaten. **Meer informatie**, in de lijst, opent een webpagina over interpunctieniveaus.
- **Hoofdletters**: wat SandFly doet bij een hoofdletter: **Niets doen**, **Zeg "hoofdletter"**, de standaardwaarde, **Toonhoogte wijzigen**, of **Geluid afspelen**. Met iets anders dan **Zeg "hoofdletter"** gaat een losse hoofdletter als kleine letter naar de spraakengine.
- **Emoji**, **Tel herhaalde emoji** en **"emoji" uitspreken na emojinamen**: zie [](#verbosity-emoji).
- **Aantal herhaalde symbolen**: zegt hoe vaak een symbool zich herhaalt wanneer dit 4 keer of vaker gebeurt. Standaard aan.
- **Tekstopmaak voorlezen** en **Opties voor tekstopmaak**: zie [](#verbosity-text-formatting).

De leesinstelling **Interpunctie en symbolen uitspreken** en de sneltoets **Interpunctieniveau wisselen** veranderen het interpunctieniveau. Om symbolen te vervangen door je eigen woorden, gebruik je **Spraak** > **Symbolenwoordenboek**. Zie [](#speech-symbol-dictionary).

#### Emoji {#verbosity-emoji}

Sommige spraakengines lezen emoji slecht voor, gebruiken eigen namen, of lezen ze niet voor. **Verbositeit** > **Emoji** bepaalt hoe SandFly ze uitspreekt:

- **Gelezen door spraakengine**: de standaardwaarde. De spraakengine krijgt de emoji zoals ze zijn.
- **Voorgelezen door SandFly**: vervangt elke emoji door zijn naam uit de Unicode CLDR, in de taal waarin de tekst wordt gesproken. Een huidskleur, een familie, een vlag of een keycap is één emoji met één naam. Emoji waarvoor SandFly geen naam heeft, worden aan de spraakengine overgelaten, en emoji zonder naam in de taal worden in het Engels uitgesproken.
- **Geen**: laat emoji weg, behalve in tekst die alleen uit emoji bestaat, zoals een reactieknop, die nog wel wordt benoemd.

Nog twee instellingen zijn afhankelijk van **Emoji**:

- **Tel herhaalde emoji**: met **Voorgelezen door SandFly** zegt een achter elkaar herhaalde emoji eenmaal met een aantal, zoals "3 grijnzend gezicht". Kies **Uit**, de standaardwaarde, of van **2 of meer** tot **6 of meer**.
- **"emoji" uitspreken na emojinamen**: zegt "emoji" na elke emoji. Standaard uit. Niet beschikbaar bij **Geen**.

Met **Voorgelezen door SandFly** noemt verplaatsen per teken een hele emoji in één keer, en stopt verplaatsen per woord bij emoji. Met **Geen** slaat verplaatsen per woord emoji over. Kopiëren of spellen van de laatst uitgesproken zin geeft de emoji, niet hun namen.

De leesinstelling **Emoji** en de opdracht **Emoji** veranderen deze instelling. De stem en snelheid van emoji worden ingesteld bij **Spraak**. Zie [](#speech-emoji).

#### Tekstopmaak {#verbosity-text-formatting}

**Verbositeit** > **Tekstopmaak voorlezen** kondigt veranderingen in tekstopmaak aan, zoals vet en cursief, terwijl SandFly tekst voorleest. Standaard uit.

**Verbositeit** > **Opties voor tekstopmaak** is beschikbaar terwijl **Tekstopmaak voorlezen** aan staat. Dit heeft:

- **Aankondigingsstijl**: **Spraak en geluid**, de standaardwaarde, **Alleen spraak**, of **Alleen geluid**. Met spraak zegt SandFly aankondigingen zoals "begin vet, kop, einde vet".
- **Aankondigingen over tekstopmaak**: de opmaak om aan te kondigen. **Vet**, **Cursief**, **Onderstreept**, **Doorgestreept**, **Naam van tekstkleur**, **Tekstgrootte** en **Lettertypefamilie** staan standaard aan. **Hexwaarde van tekstkleur** staat standaard uit.

De leesinstelling en opdracht **Tekstopmaak voorlezen** zetten de instelling aan of uit. De opdracht **Tekstopmaak beschrijven**, in de groep **Lezen** van de opdrachten, beschrijft de opmaak van de tekst met focus.

### Tabellen {#verbosity-tables}

De groep **Tabellen** geldt alleen terwijl **Raster- en tabelinformatie uitspreken** aan staat:

- **Rij- en kolomnummers uitspreken**: zegt rij- en kolomnummers voor tabelcellen. Standaard aan. Zet dit uit om alleen benoemde koppen te horen.
- **Kolomkoppen voorlezen**: **Na celgegevens**, de standaardwaarde, zegt de inhoud van een cel, daarna de rij- en kolomkoppen of -nummers. **Vóór celgegevens** zegt de koppen of nummers eerst, met "Kolomkop" of "Rijkop" voor een kopcel. Dit geldt ook voor rasters, zoals bij tabellen, dus een item in een rasters met foto's zegt eerst zijn rij en kolom. **Niet voorlezen** laat kolomkoppen weg, en zegt kolomnummers alleen terwijl **Rij- en kolomnummers uitspreken** aan staat.

### Aankondigingen {#verbosity-announcements}

De groep **Aankondigingen** bepaalt wat SandFly zegt wanneer er iets verandert:

- **Vensternamen**: wanneer SandFly de naam van een venster zegt. **Altijd** zegt deze wanneer een venster opent en wanneer focus er vanuit een ander venster naartoe gaat, voordat het eerste item wordt genoemd. **Altijd, onderbroken door het eerste item**, de standaardwaarde, doet hetzelfde, maar het eerste item met focus in een nieuw venster stopt de naam. **Alleen wanneer een venster opent** zegt de naam niet wanneer focus naar een venster gaat. **Nooit** zegt geen vensternamen.
- **Aantal lijstitems altijd uitspreken**: zegt welke items zichtbaar zijn na elke scroll. Standaard uit. Met dit uit, zegt SandFly geen lijstpositie wanneer een lijst scrolt.
- **Volledig opgeladen batterij melden**: zegt "Batterij volledig opgeladen" en speelt eenmaal het geluid **Volledig opgeladen** af wanneer de batterij vol is terwijl de oplader is aangesloten. Standaard uit.
- **Gespreksduur melden**: wanneer een telefoongesprek eindigt, zegt hoelang het duurde, vanaf het moment dat het gesprek werd opgenomen of gebeld. Gesprekken die niet zijn opgenomen worden niet aangekondigd. Standaard uit.
- **Melden wanneer het toetsenbord wordt getoond of verborgen**: standaard aan.
- **Scherm aan en uit**: wat SandFly zegt wanneer het scherm uit- of aangaat en wanneer de telefoon wordt ontgrendeld. Zie [](#verbosity-screen-on-off).
- **Tijdnotatie**: **Tijdnotatie van apparaat gebruiken**, de standaardwaarde, **12 uur** of **24 uur**, voor de tijd die SandFly zegt.
- **Seconden in de tijd opnemen**: standaard uit.
- **Seconden opnemen wanneer het scherm aangaat**: met **Seconden in de tijd opnemen** aan, neemt de seconden op in de tijd die wordt gezegd wanneer het scherm aangaat. Standaard aan.
- **Statusoverzicht**: de items van de opdracht **Status uitspreken**. Zie [](#verbosity-status-readout).

#### Scherm aan en uit {#verbosity-screen-on-off}

**Verbositeit** > **Scherm aan en uit** heeft drie groepen: **Wanneer het scherm uitgaat**, **Wanneer het scherm aangaat**, en **Wanneer de telefoon wordt ontgrendeld**. Op een flip-telefoon telt het wekken van het buitenscherm als het aangaan van het scherm, en het daarna direct openen van de telefoon zegt dit niet opnieuw. Elke groep heeft dezelfde items, behalve de eerste:

- **"Scherm uit" zeggen**: alleen in **Wanneer het scherm uitgaat**. Standaard aan.
- **Belmodus**: alleen in **Wanneer het scherm uitgaat**. Zegt wanneer de beltoon op trillen of stil staat. Standaard aan.
- **"Apparaat ontgrendeld" zeggen**: alleen in **Wanneer de telefoon wordt ontgrendeld**. Standaard aan.
- **Tijd**: standaard aan wanneer het scherm aangaat, uit voor de andere twee.
- **Aantal meldingen**: standaard aan wanneer het scherm aangaat, uit voor de andere twee.
- **Batterij**: **Uit**, de standaardwaarde, **Altijd**, of **Alleen tijdens opladen**.
- **Batterij laag**: zegt het batterijniveau wanneer dit onder een niveau komt: **Uit**, de standaardwaarde, of **Onder 10%**, **Onder 15%**, **Onder 20%**, **Onder 30%** of **Onder 50%**.
- **Scherm verborgen**: alleen wanneer het scherm is verborgen. Standaard uit. Zie [](#display).
- **Wifi**: netwerknaam en signaalsterkte. Standaard uit.
- **Mobiel netwerk**: provider, netwerktype, en signaalsterkte. Standaard uit.
- **Beltoon en niet storen**: alleen wanneer de beltoon niet op bellen staat, of niet storen aan staat. Standaard uit.
- **Vliegtuigmodus**: alleen wanneer vliegtuigmodus aan staat. Standaard uit.

#### Statusoverzicht {#verbosity-status-readout}

**Verbositeit** > **Statusoverzicht** toont de items die de opdracht **Status uitspreken** zegt, in volgorde: **Tijd**, **Batterij**, **Wifi**, **Mobiel netwerk**, **Beltoon en niet storen**, **Vliegtuigmodus** en **Scherm verborgen**. Allemaal standaard aan. Items in hun gewone staat, zoals de beltoon wanneer deze overgaat, worden weggelaten.

- Om een item uit of aan te zetten, kies je het.
- Om de volgorde te wijzigen, gebruik je de acties **Omhoog verplaatsen** en **Omlaag verplaatsen** van een item.

**Statusoverzicht** staat niet op tv's.

### Apps met hun eigen spraak {#verbosity-own-speech}

Sommige apps en toetsenborden spreken zelf, zoals een assistent die zijn antwoord uitspreekt terwijl hij toegankelijkheidsfocus naar elk nieuw stuk tekst verplaatst, of een toetsenbord dat zijn toetsen uitspreekt. De groep **Apps met hun eigen spraak** laat SandFly-spraak daarvoor weg, zodat dezelfde tekst niet twee keer wordt uitgesproken. Alleen spraak wordt weggelaten: focus verplaatst nog steeds, en geluiden en trillingen spelen nog steeds. SandFly-schermen worden altijd uitgesproken.

- **Alleen focus uitspreken die door jou is verplaatst**: **Uit**, de standaardwaarde, **In alle apps**, of **In gekozen apps**. Terwijl dit geldt, spreekt SandFly focus uit die je verplaatst met aanraking, gebaren, toetsen, een brailleleesregel of opdrachten. Het spreekt geen focus uit die de app verplaatst, aankondigingen, veranderingen in live-gebieden, vensternamen of aankondigingen van venstertwisselingen. Focus die SandFly verplaatst na een venstertwisseling, na een scroll, of om invoerfocus te volgen, telt als door de app verplaatst.
- **Gekozen apps**: de apps voor **In gekozen apps**. Alleen beschikbaar terwijl **Alleen focus uitspreken die door jou is verplaatst** op **In gekozen apps** staat. Kies **App toevoegen** om er een toe te voegen, en kies een app om deze te verwijderen.
- **Alleen focus uitspreken die door jou is verplaatst terwijl een assistent of gesprek audio afspeelt**: doet hetzelfde in alle apps terwijl een andere app audio afspeelt als assistent of gesprek. Focus die je verplaatst wordt nog steeds uitgesproken. Standaard uit.
- **Toetsenborden met hun eigen spraak**: terwijl een toetsenbord in deze lijst wordt getoond, spreekt SandFly zijn toetsen, de getypte of verwijderde tekst, of de cursor die in het bewerkbare veld verplaatst, niet uit. Gebaren en opdrachten werken nog steeds, en het verplaatsen door tekst met leesinstellingen wordt nog steeds uitgesproken. Kies **Toetsenbord toevoegen** om er een toe te voegen.

De leesinstelling **Alleen focus uitspreken die door jou is verplaatst** verandert de eerste instelling. De opdracht hiervan zet de instelling uit, of weer aan met **In gekozen apps** wanneer er gekozen apps zijn en anders **In alle apps**. De leesinstelling staat niet standaard in de leesinstellingen. Zie [](#commands).
