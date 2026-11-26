## Webpagina's en elementen {#web}

Webinhoud is een pagina in een browser, zoals Chrome, of een webweergave in een app. SandFly leest webinhoud voor met dezelfde gebaren als andere schermen, en voegt manieren toe om per element te bewegen: per kop, link, herkenningspunt, formulierveld, lijst of tabel. Deze elementbewegingen werken ook in apps.

Zie [](#reading) voor bewegen per item, leesinstellingen en doorlopend lezen. Sneltoetsen voor webpagina's, inclusief de losse-toetssneltoetsen van de browsemodus, worden beschreven in [](#keyboards).

### Een webpagina lezen {#web-read}

- Swipe naar rechts of links om naar het volgende of vorige item van de pagina te gaan. Een swipe voorbij het laatste item van de pagina gaat door naar de items na de webinhoud, zoals de werkbalk van de browser.
- Raak het scherm aan om het item onder je vinger voor te lezen.
- Gebruik **Lezen vanaf het begin** of **Lezen vanaf item waarop is gefocust** om de hele pagina te lezen. Zie [](#reading-continuous).

SandFly haalt het volgende en vorige item uit de items van de pagina, in paginavolgorde, en vraagt het volgende item alleen op van de pagina als dat nodig is, zodat items van een pagina die net is geopend niet worden overgeslagen.

Als je zelf door een pagina scrolt, met twee vingers of met een scrollopdracht, en het item met focus scrolt buiten beeld, verplaatst SandFly de focus naar het eerste item in beeld, of naar het laatste item in beeld als je terugscrolt. Als een pagina verandert of opnieuw laadt en het item met focus verdwijnt, plaatst SandFly de focus op het item dat nu op die plek staat.

Hoe sommige elementen worden voorgelezen:

- Een herkenningspunt, zoals navigatie of hoofd, wordt voorgelezen met het type en de naam, niet met de hele inhoud.
- Selectievakjes en keuzerondjes worden voorgelezen met hun labels.
- Als een pagina de focus naar een element zoals een bericht verplaatst, zoals Mastodon doet met de toetsen J en K, leest SandFly de tekst van de links erin voor, zoals de auteur, de tijd en de hashtags, maar niet de knoppen ervan. Spaties tussen links worden niet voorgelezen.

Opmerking: Sommige spraakengines, zoals Gryphon, stoppen alleen tussen de tekstdelen die ze ontvangen. Als de spraak van het volgende item laat begint nadat je van een lange alinea bent weggeswipet, zet je **Spraak** > **Stuur lange tekst zin voor zin** aan. Zie [](#speech-pronunciation).

### Per element bewegen {#web-elements}

Om per één soort element te bewegen, selecteer je de leesinstelling ervan en swipe je dan omlaag voor het volgende en omhoog voor het vorige. Als er geen verder element van dat soort is, leest SandFly bijvoorbeeld "Geen volgende kop" voor.

Deze leesinstellingen staan standaard aan:

- **Koppen**: koppen van elk niveau.
- **Links**: links.
- **Bedieningselementen**: items die je kunt gebruiken, zoals knoppen, selectievakjes en tekstvelden.

Je kunt deze leesinstellingen toevoegen op het scherm **Leesinstellingen**, onder **Elementnavigatie** en **Kopniveaus**:

- **Herkenningspunten**: herkenningspunten, zoals "hoofd" of "navigatie".
- **Knoppen**
- **Selectievakjes**
- **Keuzerondjes**
- **Bewerkingsvelden**
- **Keuzelijsten met invoervak**: keuzelijsten met invoervak, zoals vervolgkeuzemenu's.
- **Focusbare items**: items die invoerfocus kunnen krijgen.
- **Afbeeldingen en video's**
- **Lijsten**
- **Lijstitems**
- **Tabellen**
- **Bezochte links**: links naar pagina's die je hebt geopend.
- **Niet-bezochte links**: links naar pagina's die je niet hebt geopend.
- **Kop 1** tot en met **Kop 6**: koppen van één niveau.

Zie [](#commands) om een leesinstelling toe te voegen. Zie [](#reading-controls) om leesinstellingen te selecteren.

Je kunt ook per element bewegen zonder de leesinstelling te wijzigen: wijs gebaren toe aan de opdrachten in de categorie **Navigeren op element**, zoals **Volgende kop**, **Vorige kop** en **Volgende kop 1**. Zie [](#gestures). Op een fysiek toetsenbord heeft elk element een sneltoets; zie [](#keyboards).

De opdracht **Navigatie**, die je aan het SandFly-menu kunt toevoegen, toont soorten navigatie om uit te kiezen, zoals tekens, koppen of herkenningspunten. Als die in het SandFly-menu staat, kiest **Navigatie-opties bewerken**, daaronder op het scherm **SandFly-menu**, welke soorten worden getoond. Zie [](#commands).

#### Elementen in apps {#web-elements-in-apps}

Elke leesinstelling, elk gebaar en elke sneltoets voor elementen werkt ook in apps, zodat één verzameling opdrachten door webpagina's en apps beweegt. In apps zijn de elementen:

| Element | In apps |
| --- | --- |
| Koppen, en Kop 1 tot en met Kop 6 | Items die de app als koppen markeert. Apps geven geen kopniveaus door, dus elk niveau beweegt door alle koppen. |
| Links | Links in tekst. |
| Bedieningselementen | Knoppen, selectievakjes, schakelaars, tekstvelden, schuifregelaars en andere items die je kunt gebruiken. |
| Herkenningspunten | Werkbalken, tabbladbalken, navigatiebalken en zoekbalken, panelen met een titel, en containers met een titel. |
| Knoppen | Knoppen, afbeeldingsknoppen en zwevende actieknoppen. |
| Selectievakjes | Selectievakjes, schakelaars en keuzeknoppen. |
| Keuzerondjes | Keuzerondjes. |
| Bewerkingsvelden | Tekstvelden. |
| Keuzelijsten met invoervak | Vervolgkeuzelijsten. |
| Focusbare items | Items die invoerfocus kunnen krijgen. |
| Afbeeldingen en video's | Afbeeldingen, inclusief afbeeldingen die je kunt activeren. |
| Lijsten | Lijsten en verzamelingen die geen tabellen zijn. |
| Lijstitems | Rijen van de lijst die focus heeft. Zie [](#reading-lists-tables). |
| Tabellen | Tabellen en roosters met rijen en kolommen. |
| Bezochte links | Geen. Apps geven geen bezochte links door, dus leest SandFly "Geen volgende bezochte link" voor. |
| Niet-bezochte links | Alle links. |

Als het volgende element buiten beeld is, scrollt SandFly de lijst waarin het staat, tot 10 keer.

**Leesinstellingen** > **Links en Bedieningselementen alleen in webinhoud**, standaard uit, laat de leesinstellingen **Links** en **Bedieningselementen** buiten webinhoud weg uit de lijst. De gebaren en sneltoetsen werken nog steeds overal.

Als SandFly Pro-schermherkenning de elementen toevoegt van een app die ze niet doorgeeft, worden die elementen voorgelezen, met aanraking gevonden en genavigeerd zoals de items van de app. Zie [](#pro).

### Tabellen op webpagina's {#web-tables}

Op een webpagina beweegt de leesinstelling **Tabellen** van tabel naar tabel. Binnen een tabel bewegen de leesinstellingen **Rijen en kolommen**, **Rij** en **Kolom** tussen cellen, zoals in apps. Zie [](#reading-tables).

Een fysiek toetsenbord heeft meer tabelopdrachten, in de categorie **Tabellen**, zoals bewegen naar het begin of einde van een rij, een kolom of een tabel, en het voorlezen van de rij- of kolomkop. Zie [](#keyboards).

### Acties op webpagina's {#web-actions}

Met **Leesinstellingen** > **Leesinstelling automatisch wijzigen** aan selecteert SandFly de leesinstelling **Acties** als de focus naar een item gaat dat acties heeft. Op webpagina's selecteert SandFly deze alleen op items die eigen acties hebben, niet op tekst of op links binnen tekst, zodat een leesinstelling die je hebt geselecteerd, zoals **Koppen**, geselecteerd blijft terwijl je leest. Bij het volgende item zonder acties selecteert SandFly de eerdere leesinstelling weer. Als **Acties** automatisch is geselecteerd en niets heeft voor het item met focus, gebruikt omhoog of omlaag swipen de leesinstelling die deze heeft vervangen.

### Browsemodus {#web-browse-mode}

De browsemodus verandert wat de toetsen van een fysiek toetsenbord en de opdrachten van een brailleleesregel doen. Met de browsemodus aan bewegen losse toetsen per element, zoals H voor de volgende kop, in plaats van naar de pagina te gaan. Met de browsemodus uit gaan toetsen naar de pagina, om te typen in tekstvelden en voor bedieningselementen die de pijltoetsen gebruiken. SandFly zegt "Browsemodus aan" of "Browsemodus uit" en speelt een geluid af als deze verandert.

Gebruik de opdracht **Browsemodus aan- of uitzetten**, in de categorie **Web**, om de browsemodus aan of uit te zetten; je kunt deze aan een gebaar toewijzen. Elke keymap heeft hiervoor ook een sneltoets. Zie [](#keyboards).

In de browsemodus gaat de toets voor snel zoeken met een letter of cijfer naar het volgende item dat met die letter of dat cijfer begint. **Sneltoetsen** > **Snel zoeken** > **Toets voor snel zoeken** kiest de toets; deze staat standaard uit. Zie [](#keyboards-fast-search).

Deze instellingen staan op het scherm **Navigatie**:

- **Slimme browsemodus**: zet de browsemodus aan of uit voor het gefocuste item. Standaard aan. SandFly zet de browsemodus aan als de focus naar een item in webinhoud gaat, en uit bij tekstvelden en bij bedieningselementen die de pijltoetsen gebruiken, zoals schuifregelaars, keuzelijsten, menubalken, menu's, boomstructuren, roosters, keuzelijsten met invoervak en spinbuttons. Als een toetsenbordopdracht de SandFly-focus naar een van deze items verplaatst, gaat de invoerfocus daar ook naartoe. Als je de browsemodus zelf uitzet in webinhoud, zet het verplaatsen van de focus binnen die webinhoud hem niet weer aan. De browsemodus gaat ook uit als de invoerfocus naar een tekstveld of schuifregelaar in een app gaat.
- **Browsemodus per app onthouden**: SandFly behoudt de browsemodus zoals je die voor het laatst in elke app hebt aan- of uitgezet. In een app waarin je de browsemodus niet hebt aan- of uitgezet, staat die uit, tenzij **Slimme browsemodus** hem aanzet in webinhoud. Standaard uit.

### Lijsten met elementen en adressen {#web-lists}

Met een fysiek toetsenbord tonen deze opdrachten, in de categorie **Web**, de elementen van de pagina:

- **Lijst met koppen tonen**
- **Lijst met oriëntatiepunten tonen**
- **Lijst met links tonen**
- **Lijst met bedieningselementen tonen**
- **Lijst met tabellen tonen**

Elke opdracht opent schermzoeken met de lijst van één soort element. Selecteer een element om de focus daarnaartoe te verplaatsen. Met een fysiek toetsenbord aangesloten heeft schermzoeken ook de knoppen **Alles**, **Koppen**, **Herkenningspunten**, **Links**, **Bedieningselementen** en **Tabellen**, die één soort element tonen. Zie [](#reading-search).

**URL uitspreken** leest het adres van de link met focus voor, en **URL van pagina uitspreken** leest het adres van de pagina voor. Beide zijn toetsenbordopdrachten. Zie [](#keyboards).

Gebruik de leesinstelling **Adressen en nummers** om de webadressen, e-mailadressen en telefoonnummers te vinden die in de tekst van een item staan. Zie [](#reading-addresses).

### Instellingen voor webinhoud {#web-settings}

- **Navigatie** > **Slimme browsemodus** en **Browsemodus per app onthouden**: zie [](#web-browse-mode).
- **Leesinstellingen** > **Links en Bedieningselementen alleen in webinhoud**: zie [](#web-elements-in-apps).
- **Leesinstellingen** > **Leesinstelling automatisch wijzigen**: zie [](#web-actions).
- Een instellingenprofiel kan automatisch worden gebruikt terwijl de focus in webinhoud staat, bijvoorbeeld om een andere spreeksnelheid voor webpagina's in te stellen. Zie [](#profiles).
