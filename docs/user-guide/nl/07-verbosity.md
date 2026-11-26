## Uitgebreidheidsniveau {#verbosity}

Het scherm **Uitgebreidheidsniveau** in de instellingen van SandFly bepaalt wat SandFly zegt over items, tekst en symbolen, tabellen en veranderingen op het scherm, en wat het weglaat voor apps die zelf spreken. Het heeft de groepen **Items**, **Tekst en symbolen**, **Tabellen**, **Aankondigingen** en **Apps met eigen spraak**.

Hoe tekst wordt uitgesproken, zoals fonetische letters en het spraakwoordenboek, staat op het scherm **Spraak**. Zie [](#speech). Toetsenbordecho staat op het scherm **Typen**. Zie [](#typing).

De opdracht **Uitgebreidheidsniveau** opent dit scherm. Je kunt ook een item **Uitgebreidheidsniveau** toevoegen aan het SandFly-menu. Zie [](#commands).

### Items {#verbosity-items}

De groep **Items** bepaalt wat SandFly over elk item zegt:

- **Gebruikstips uitspreken**: zegt, na het beschrijven van een item, hoe je het gebruikt, zoals "Dubbeltik om te activeren". Standaard aan.
- **Elementtype uitspreken**: zegt het type van een item, zoals "Knop" of "Keuzevakje". Standaard aan.
- **Itemtype uitspreken zonder pauze**: voegt de naam en het type van een item samen met een spatie in plaats van een pauze. Standaard uit.
- **Acties beschikbaar uitspreken**: zegt "Acties beschikbaar" na een item met acties van de app, zoals Beantwoorden bij een chatbericht. Werkt met **Gebruikstips uitspreken** aan of uit; met gebruikstips aan geeft de tip dan alleen het gebaar dat de acties toont, zoals "Gebruik tik met 2 vingers om acties te bekijken". Standaard uit. Voor de items waarvoor dit geldt en het geluid **Acties beschikbaar**, zie [](#sound-actions-available).
- **Lijstinformatie uitspreken**: zegt lijstposities en lijstdetails. Standaard aan.
- **Raster- en tabelinformatie uitspreken**: zegt rijen en kolommen in rasters, tabellen en pagineringen die in rijen en kolommen zijn ingedeeld, zoals een map op een startscherm. Standaard aan. De instellingen van **Tabellen** gelden alleen terwijl dit aan staat. Zie [](#verbosity-tables).
- **Volgorde van itemgegevens**: de volgorde van de naam, het type en de status van een item. Zie [](#verbosity-item-order).
- **Element-ID's voorlezen**: zegt de ID van knoppen zonder label. Standaard uit.

#### Verander de volgorde van itemgegevens {#verbosity-item-order}

**Uitgebreidheidsniveau** > **Volgorde van itemgegevens** toont **Status**, **Naam** en **Type** in de volgorde waarin SandFly ze uitspreekt. De standaardwaarde is **Status, naam, type**.

Om een detail te verplaatsen, doe je een van de volgende dingen:

- Tik op het detail en houd vast, en sleep het omhoog of omlaag. Met SandFly aan, dubbeltik je op het detail en houd je vast, en sleep je het.
- Gebruik de acties **Omhoog verplaatsen** en **Omlaag verplaatsen** van het detail.

SandFly kondigt de positie van het detail aan terwijl je het oppakt, verplaatst en neerzet.

De volgorde geldt ook voor rijen waarvan het keuzevakje of de schakelaar geen eigen tekst heeft, die SandFly als één bedieningselement voorleest met de tekst van de rij als naam, en voor knoppen en tabbladen waarvan het label een los item erbinnen is.

### Tekst en symbolen {#verbosity-text}

De groep **Tekst en symbolen** bepaalt hoe tekst, interpunctie, hoofdletters en emoji worden uitgesproken:

- **Interpunctie en symbolen uitspreken**: **Alle** zegt elk leesteken en symbool. **De meeste** zegt de meeste symbolen, zoals & en @, maar laat gewone interpunctie, zoals komma's en apostroffen, aan de spraakengine over. **Sommige**, de standaardwaarde, laat interpunctie en symbolen aan de spraakengine over. **Geen** laat interpunctie en symbolen weg uit spraak, behalve een teken dat los wordt voorgelezen, zoals bij het verplaatsen per teken. Apostroffen en koppeltekens binnen woorden, scheidingstekens binnen getallen en interpunctie die een woord afsluit, worden aan de spraakengine overgelaten. **Meer informatie**, in de lijst, opent een webpagina over interpunctieniveaus.
- **Symbolen uitspreken die de betekenis veranderen**, getoond terwijl **Interpunctie en symbolen uitspreken** op **Geen** staat: laat deze aan de spraakengine over: een plus- of minteken direct voor een getal, graden-, procent-, promille- en valutatekens, en + − × ÷ = < > tussen getallen. Standaard uit.
- **Hoofdletters**: wat SandFly doet bij een hoofdletter: **Niets doen**, **Zeg 'hoofdletter'**, de standaardwaarde, **Toonhoogte wijzigen**, of **Geluid afspelen**. Met een andere keuze dan **Zeg 'hoofdletter'** stuurt SandFly een losse hoofdletter als kleine letter naar de spraakengine.
- **Emoji**, **Tel herhaalde emoji** en **‘Emoji’ uitspreken na emojinamen**: zie [](#verbosity-emoji).
- **Aantal herhaalde symbolen**: zegt hoe vaak een symbool zich herhaalt wanneer dit 4 keer of vaker gebeurt. Standaard aan.
- **Tekstopmaak voorlezen** en **Opties voor tekstopmaak**: zie [](#verbosity-text-formatting).

De leesinstelling **Interpunctie en symbolen uitspreken** en de sneltoets **Interpunctieniveau doorlopen** veranderen het interpunctieniveau. Om symbolen te vervangen door je eigen woorden, gebruik je **Spraak** > **Symbolenwoordenboek**. Zie [](#speech-symbol-dictionary).

#### Emoji {#verbosity-emoji}

Sommige spraakengines lezen emoji slecht voor, gebruiken eigen namen, of lezen ze niet voor. **Uitgebreidheidsniveau** > **Emoji** bepaalt hoe SandFly ze uitspreekt:

- **Gelezen door spraakengine**: de standaardwaarde. De spraakengine krijgt de emoji zoals ze zijn.
- **Voorgelezen door SandFly**: vervangt elke emoji door zijn naam uit de Unicode CLDR, in de taal waarin de tekst wordt gesproken. Een huidskleur, een familie, een vlag of een keycap is één emoji met één naam. Emoji waarvoor SandFly geen naam heeft, worden aan de spraakengine overgelaten, en emoji zonder naam in de taal worden in het Engels uitgesproken.
- **Geen**: laat emoji weg, behalve in tekst die alleen uit emoji bestaat, zoals een reactieknop, die nog wel wordt benoemd.

Nog twee instellingen zijn afhankelijk van **Emoji**:

- **Tel herhaalde emoji**: met **Voorgelezen door SandFly** zegt een achter elkaar herhaalde emoji eenmaal met een aantal, zoals "3 grijnzend gezicht". Kies **Uit**, de standaardwaarde, of van **2 of meer** tot **6 of meer**.
- **Na emojinamen**: wat na elke emoji volgt. Niet beschikbaar bij **Geen**.
  - **Niets**: de standaardwaarde.
  - **"emoji" zeggen**: zegt "emoji" na elke emoji, zoals "grijnzend gezicht emoji".
  - **Een geluid afspelen**: speelt het geluid **Emoji** af na elke emoji, of eenmaal na emoji die naast elkaar staan. Emoji worden dan als afzonderlijke uitingen uitgesproken, los van de tekst eromheen. Een geluidsthema kan het geluid vervangen, en **Afzonderlijke geluiden en trillingen** kan het uitzetten. Zie [](#sound-events).

Met **Voorgelezen door SandFly** noemt verplaatsen per teken een hele emoji in één keer, en stopt verplaatsen per woord bij emoji. Met **Geen** slaat verplaatsen per woord emoji over. Kopiëren of spellen van de laatst uitgesproken zin geeft de emoji, niet hun namen.

De leesinstelling **Emoji** en de opdracht **Emoji** veranderen deze instelling. De stem en snelheid van emoji worden ingesteld bij **Spraak**. Zie [](#speech-emoji).

#### Tekstopmaak {#verbosity-text-formatting}

**Uitgebreidheidsniveau** > **Tekstopmaak voorlezen** kondigt veranderingen in tekstopmaak aan, zoals vet en cursief, terwijl SandFly tekst voorleest. Standaard uit.

**Uitgebreidheidsniveau** > **Opties voor tekstopmaak** is beschikbaar terwijl **Tekstopmaak voorlezen** aan staat. Dit heeft:

- **Aankondigingsstijl**: **Spraak en geluid**, de standaardwaarde, **Alleen spraak**, of **Alleen geluid**. Met spraak zegt SandFly aankondigingen zoals "begin vet, kop, einde vet".
- **Aankondigingen over tekstopmaak**: de opmaak om aan te kondigen. **Vet**, **Cursief**, **Onderstrepen**, **Doorhalen**, **Naam tekstkleur**, **Tekstgrootte** en **Lettertypefamilie** staan standaard aan. **Hexadecimale waarde van tekstkleur** staat standaard uit.

De leesinstelling en opdracht **Tekstopmaak voorlezen** zetten de instelling aan of uit. De opdracht **Tekstopmaak beschrijven**, in de groep **Lezen** van de opdrachten, beschrijft de opmaak van de tekst met focus.

### Tabellen {#verbosity-tables}

De groep **Tabellen** geldt alleen terwijl **Raster- en tabelinformatie uitspreken** aan staat:

- **Rij- en kolomnummers uitspreken**: zegt rij- en kolomnummers voor tabelcellen. Standaard aan. Zet dit uit om alleen benoemde koppen te horen.
- **Kolomkoppen voorlezen**: **Na celgegevens**, de standaardwaarde, zegt de inhoud van een cel, daarna de rij- en kolomkoppen of -nummers. **Vóór celgegevens** zegt de koppen of nummers eerst, met "Kolomkop" of "Rijkop" voor een kopcel. Dit geldt ook voor rasters, zoals bij tabellen, dus een item in een rasters met foto's zegt eerst zijn rij en kolom. **Niet voorlezen** laat kolomkoppen weg, en zegt kolomnummers alleen terwijl **Rij- en kolomnummers uitspreken** aan staat.

### Aankondigingen {#verbosity-announcements}

De groep **Aankondigingen** bepaalt wat SandFly zegt wanneer er iets verandert:

- **Vensternamen**: wanneer SandFly de naam van een venster zegt. **Altijd** zegt deze wanneer een venster opent en wanneer focus er vanuit een ander venster naartoe gaat, voordat het eerste item wordt genoemd. **Altijd, onderbroken door het eerste item**, de standaardwaarde, doet hetzelfde, maar het eerste item met focus in een nieuw venster stopt de naam. **Alleen als er een venster opengaat** zegt de naam niet wanneer focus naar een venster gaat. **Nooit** zegt geen vensternamen.
- **Aantal lijstitems altijd uitspreken**: zegt welke items zichtbaar zijn na elke scroll. Standaard uit. Met dit uit, zegt SandFly geen lijstpositie wanneer een lijst scrolt.
- **Volledig opgeladen batterij melden**: zegt "Batterij volledig opgeladen" en speelt eenmaal het geluid **Volledig opgeladen** af wanneer de batterij vol is terwijl de oplader is aangesloten. Standaard uit.
- **Batterijniveau**: zegt het batterijniveau, of de batterij nu oplaadt of niet, elke keer dat het een veelvoud van 5% of 10% bereikt, of elke 15 minuten, 30 minuten of uur. Er wordt niets gezegd terwijl het scherm uit is: een niveau dat is bereikt of een interval dat is verstreken terwijl het scherm uit was, wordt gezegd wanneer het scherm aangaat. Standaard uit.
- **Stem voor batterijniveau**: de stem voor **Batterijniveau**: **Stem voor meldingen**, **SandFly's stem**, de standaardwaarde, of een stemprofiel. Zie [](#notifications-voice).
- **Gespreksduur melden**: wanneer een telefoongesprek eindigt, zegt hoelang het duurde, vanaf het moment dat het gesprek werd opgenomen of gebeld. Gesprekken die niet zijn opgenomen worden niet aangekondigd. Standaard uit.
- **Melden wanneer het toetsenbord wordt getoond of verborgen**: standaard aan.
- **Scherm aan en uit**: wat SandFly zegt wanneer het scherm uit- of aangaat en wanneer de telefoon wordt ontgrendeld. Zie [](#verbosity-screen-on-off).
- **Tijdnotatie**: **Tijdsindeling van apparaat gebruiken**, de standaardwaarde, **12-uurs** of **24-uurs**, voor de tijd die SandFly zegt.
- **Seconden in de tijd opnemen**: standaard uit.
- **Seconden opnemen als het scherm aangaat**: met **Seconden in de tijd opnemen** aan, neemt de seconden op in de tijd die wordt gezegd wanneer het scherm aangaat. Standaard aan.
- **Status voorlezen**: de items van de opdracht **Status uitspreken**. Zie [](#verbosity-status-readout).

#### Scherm aan en uit {#verbosity-screen-on-off}

**Uitgebreidheidsniveau** > **Scherm aan en uit** heeft drie groepen: **Wanneer het scherm wordt uitgeschakeld**, **Wanneer het scherm wordt ingeschakeld**, en **Wanneer de telefoon wordt ontgrendeld**. Op een flip-telefoon telt het wekken van het buitenscherm als het aangaan van het scherm, en het daarna direct openen van de telefoon zegt dit niet opnieuw. Elke groep heeft dezelfde items, behalve de eerste items:

- **'Scherm uit' uitspreken**: alleen in **Wanneer het scherm wordt uitgeschakeld**. Standaard aan.
- **Belmodus**: alleen in **Wanneer het scherm wordt uitgeschakeld**. Zegt wanneer de beltoon op trillen of stil staat. Standaard aan.
- **'Apparaat ontgrendeld' uitspreken**: alleen in **Wanneer de telefoon wordt ontgrendeld**. Standaard aan.
- **Tijd**: standaard aan wanneer het scherm aangaat, uit voor de andere twee.
- **Aantal meldingen**: standaard aan wanneer het scherm aangaat, uit voor de andere twee.
- **Batterij**: **Uit**, de standaardwaarde, **Altijd**, of **Alleen tijdens opladen**.
- **Batterij bijna leeg**: zegt het batterijniveau wanneer dit onder een niveau komt: **Uit**, de standaardwaarde, of **Onder 10%**, **Onder 15%**, **Onder 20%**, **Onder 30%** of **Onder 50%**.
- **Verborgen scherm**: alleen wanneer het scherm is verborgen. Standaard uit. Zie [](#display).
- **Wi-Fi**: netwerknaam en signaalsterkte. Standaard uit.
- **Mobiel netwerk**: provider, netwerktype, en signaalsterkte. Standaard uit.
- **Belsignaal en Niet storen**: alleen wanneer de beltoon niet op bellen staat, of niet storen aan staat. Standaard uit.
- **Vliegtuigmodus**: alleen wanneer vliegtuigmodus aan staat. Standaard uit.

**Uitspreken tijdens het afspelen van media**, boven de groepen, spreekt de aankondigingen bij scherm uit en ontgrendelen uit terwijl media worden afgespeeld. Standaard uit. De aankondiging bij scherm aan wordt altijd uitgesproken tijdens het afspelen van media.

#### Status voorlezen {#verbosity-status-readout}

**Uitgebreidheidsniveau** > **Status voorlezen** toont de items die de opdracht **Status uitspreken** zegt, in volgorde: **Tijd**, **Batterij**, **Wi-Fi**, **Mobiel netwerk**, **Belsignaal en Niet storen**, **Vliegtuigmodus** en **Verborgen scherm**. Allemaal standaard aan. Items in hun gewone staat, zoals de beltoon wanneer deze overgaat, worden weggelaten.

- Om een item uit of aan te zetten, kies je het.
- Om de volgorde te wijzigen, gebruik je de acties **Omhoog verplaatsen** en **Omlaag verplaatsen** van een item.

**Status voorlezen** staat niet op tv's.

### Apps met eigen spraak {#verbosity-own-speech}

Sommige apps en toetsenborden spreken zelf, zoals een assistent die zijn antwoord uitspreekt terwijl hij toegankelijkheidsfocus naar elk nieuw stuk tekst verplaatst, of een toetsenbord dat zijn toetsen uitspreekt. De groep **Apps met eigen spraak** laat SandFly-spraak daarvoor weg, zodat dezelfde tekst niet twee keer wordt uitgesproken. Alleen spraak wordt weggelaten: focus verplaatst nog steeds, en geluiden en trillingen spelen nog steeds. SandFly-schermen worden altijd uitgesproken.

- **Alleen door de gebruiker verplaatste focus uitspreken**: **Uit**, de standaardwaarde, **In alle apps**, of **In gekozen apps**. Terwijl dit geldt, spreekt SandFly focus uit die je verplaatst met aanraking, gebaren, toetsen, een brailleleesregel of opdrachten. Het spreekt geen focus uit die de app verplaatst, aankondigingen, veranderingen in live-gebieden, vensternamen of aankondigingen van venstertwisselingen. Focus die SandFly verplaatst na een venstertwisseling, na een scroll, of om invoerfocus te volgen, telt als door de app verplaatst.
- **Gekozen apps**: de apps voor **In gekozen apps**. Alleen beschikbaar terwijl **Alleen door de gebruiker verplaatste focus uitspreken** op **In gekozen apps** staat. Kies **App toevoegen** om er een toe te voegen, en kies een app om deze te verwijderen.
- **Alleen door de gebruiker verplaatste focus uitspreken terwijl een assistent of gesprek audio afspeelt**: doet hetzelfde in alle apps terwijl een andere app audio afspeelt als assistent of gesprek, en terwijl een assistentvenster wordt getoond, zoals Gemini boven een app of het vergrendelscherm. De meldingen bij scherm aan en ontgrendelen worden dan ook weggelaten, en spraak stopt wanneer een assistentvenster opent. Focus die je verplaatst wordt nog steeds uitgesproken. Standaard uit.
- **Toetsenborden met eigen spraak**: terwijl een toetsenbord in deze lijst wordt getoond, spreekt SandFly zijn toetsen, de getypte of verwijderde tekst, of de cursor die in het bewerkbare veld verplaatst, niet uit. Gebaren en opdrachten werken nog steeds, en het verplaatsen door tekst met leesinstellingen wordt nog steeds uitgesproken. Kies **Toetsenbord toevoegen** om er een toe te voegen.

SandFly spreekt nooit de aankondigingen, veranderingen in live-gebieden, venstertwisselingen, toasts of meldingen uit van com.oplus.viewtalk, een toegankelijkheidsservice op realme- en OPPO-telefoons die afbeeldingsbeschrijvingen stuurt zoals "Afbeelding kan … bevatten".

De leesinstelling **Alleen door de gebruiker verplaatste focus uitspreken** verandert de eerste instelling. De opdracht hiervan zet de instelling uit, of weer aan met **In gekozen apps** wanneer er gekozen apps zijn en anders **In alle apps**. De leesinstelling staat niet standaard in de leesinstellingen. Zie [](#commands).
