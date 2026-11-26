## Het scherm verkennen en voorlezen {#reading}

SandFly leest het item voor dat SandFly-focus heeft. Je verplaatst de focus door het scherm aan te raken, door te swipen, met een toetsenbord of een brailleleesregel, en SandFly leest elk item voor waar het naartoe beweegt. Dit hoofdstuk beschrijft hoe je door het scherm beweegt, tekst in stukken leest, doorlopend leest en scrolt, en vermeldt de instellingen op het scherm **Navigatie**.

De gebaren in dit hoofdstuk zijn die van de standaardgebarenindeling, **SandFly-gebaren**. Met **Gebaren** > **Gebarenindeling** ingesteld op **VoiceOver-gebaren** verschillen sommige gebaren. Voor elk standaardgebaar, zie [](#appendix-gestures). Om gebaren te veranderen, zie [](#gestures).

### SandFly-focus {#reading-focus}

SandFly-focus is het item dat SandFly voorleest en waarop het handelt. Een rechthoek op het scherm toont waar deze zich bevindt; om te veranderen hoe die eruitziet, zie [](#display).

SandFly-focus staat los van invoerfocus, het item dat invoer ontvangt van een toetsenbord. Tekstvelden, en sommige webinhoud, kunnen invoerfocus hebben terwijl SandFly-focus elders is.

Een item kan een knop zijn, een regel tekst, een lijstrij, een afbeelding of iets anders dat een app toont. Apps bepalen hoe hun schermen in items zijn verdeeld. Wanneer een item andere tekst bevat, leest SandFly dit allemaal als één item, tenzij **Navigatie** > **Tekst in items afzonderlijk focussen** aan staat. Zie [](#reading-navigation-settings).

### Verkennen via aanraking {#reading-explore}

Raak het scherm aan en beweeg je vinger. SandFly focust en leest het item onder je vinger voor. Wanneer je vinger over een leeg deel van het scherm beweegt, speelt SandFly een geluid.

SandFly wacht een korte tijd na het aanraken van het scherm, om een gebaar van een aanraking te onderscheiden, voordat het het item onder je vinger focust. Om deze wachttijd te veranderen, ga naar **Aanraking** > **Focusvertraging**. De standaardwaarde is 0,15 seconden. Aanraakinstellingen worden beschreven in [](#touch).

- Tussen de pictogrammen van het startscherm aanraken leest de pagina niet voor, zoals "Startscherm 1 van 2". Swipen bereikt dit nog wel.
- Een scherm-in-scherm-venster, zoals een video die over het startscherm speelt, is één item, genoemd naar de app en "scherm in scherm". De bedieningselementen in het venster zijn acties van dat item. Aanraking bereikt elke bediening nog wel.

### Tussen items bewegen {#reading-move}

- Om naar het volgende item te gaan, swipe naar rechts.
- Om naar het vorige item te gaan, swipe naar links.

Items worden voorgelezen in de volgorde die de app geeft, meestal van linksboven naar rechtsonder. Wanneer het volgende item buiten beeld is in een lijst, scrolt SandFly de lijst. Zie [](#reading-scroll).

Swipes bewegen niet naar de statusbalk of de navigatiebalk. Je bereikt deze door aanraking, of met de leesinstelling **Vensters**. Swipes vanuit de app bewegen niet naar het schermtoetsenbord; zie [](#typing).

Aan het einde van het scherm stopt een swipe voorbij het laatste item daar en speelt een geluid. De volgende swipe gaat naar het eerste item. Om dit te veranderen, zie [](#reading-wrap).

Deze opdrachten bewegen naar de uiteinden van het scherm. Ze hebben standaard geen gebaar:

- **Focus op eerste item op het scherm plaatsen**
- **Focus op laatste item op het scherm plaatsen**

Om een gebaar of sneltoets aan een opdracht toe te wijzen, zie [](#commands).

#### Rondgaan {#reading-wrap}

Met **Navigatie** > **Rondgaan** aan, wat de standaard is, gaat een swipe voorbij het laatste item naar het eerste item, en een swipe voorbij het eerste item naar het laatste.

- **Navigatie** > **Stoppen bij het laatste item voor het rondgaan**: De eerste swipe voorbij het laatste of eerste item stopt daar met het eindgeluid, en alleen de volgende swipe gaat rond. Standaard aan. Dit heeft geen effect terwijl **Rondgaan** uit staat.
- Met **Rondgaan** uit, leest elke swipe of toetsaanslag voorbij het eerste of laatste item dat item opnieuw voor met het eindgeluid. Bewegen op kop, link of bediening in apps stopt ook bij de uiteinden. Webpagina's kunnen zelf nog wel rondgaan.

**Rondgaan** is ook een leesinstelling, die je kunt toevoegen op het scherm **Leesinstellingen** om rondgaan aan of uit te zetten met een swipe omhoog of omlaag. Zie [](#reading-controls).

**Navigatie** > **Rondgaan in leesinstellingen** stelt in of leesinstellingen die door een lijst van waarden gaan, zoals **Spraakmotor**, **Audio-uitvoerapparaat**, **Acties**, **Adressen en nummers** of **Geluidsmodus**, rondgaan van de laatste waarde naar de eerste. Wanneer dit uit staat, leest swipen voorbij de eerste of laatste waarde die waarde opnieuw voor met het eindgeluid. Tot je dit verandert, volgt het **Rondgaan**, zodat items en waarden eenmaal ingesteld anders kunnen rondgaan.

### Een item activeren {#reading-activate}

- Om het gefocuste item te activeren, dubbeltik ergens op het scherm. Dit is de opdracht **Activeren**.
- Om het gefocuste item te tikken en vast te houden, zoals een lange druk doet, dubbeltik en houd vast. Dit is de opdracht **Tikken en vasthouden**.

Wanneer de leesinstelling **Acties** is geselecteerd, voert een dubbeltik de daarin gekozen actie uit. Zie [](#reading-controls).

Met **Typen** > **Dubbeltikken verplaatst cursor naar begin of einde** aan verplaatst een dubbeltik op het tekstveld waarin je typt de cursor naar het begin of einde van de tekst. Zie [](#typing-double-tap-cursor).

**Activeren** doet in al deze gevallen wat een dubbeltik doet, welk gebaar, welke spraakopdracht, welk braillecommando of welk script de opdracht ook uitvoert.

**Activeren met een tik**, een opdracht zonder gebaar standaard, tikt op het midden van het gefocuste item op het scherm, voor items die alleen reageren op een vinger op het scherm. Dit heeft Android 11 of hoger nodig; anders wordt het item als gewoonlijk geactiveerd.

Om items te activeren door je vinger op te tillen of met een enkele tik, zie [](#touch).

### Leesinstellingen {#reading-controls}

Een leesinstelling bepaalt wat een swipe omhoog of omlaag doet. De meeste leesinstellingen bewegen op een soort item of stuk tekst, zoals **Koppen** of **Woorden**. Andere veranderen een instelling, zoals **Spreeksnelheid**.

- Om de volgende leesinstelling te selecteren, swipe omlaag en dan omhoog, swipe met drie vingers naar rechts of omlaag, of draai met twee vingers met de klok mee.
- Om de vorige leesinstelling te selecteren, swipe omhoog en dan omlaag, swipe met drie vingers naar links of omhoog, of draai tegen de klok in.
- Om de eerste leesinstelling van de gebruikte lijst te selecteren, wijs je een gebaar of sneltoets toe aan **Eerste leesinstelling**. Deze heeft er standaard geen.
- Om op de geselecteerde leesinstelling te bewegen, swipe omlaag om vooruit te gaan en omhoog om terug te gaan. Dit zijn de opdrachten **Leesinstellingen lager of vooruit verplaatsen** en **Leesinstellingen hoger of achteruit verplaatsen**.
- Bij leesinstellingen die een waarde wijzigen, zoals **Schuifregelaar aanpassen**, **Waarde aanpassen** en **Spreeksnelheid**, bepaalt de richting van de swipe de wijziging, welke van de twee opdrachten de swipe ook uitvoert: een swipe omhoog of naar rechts verhoogt de waarde, een swipe omlaag of naar links verlaagt deze. Dit geldt voor rechte swipes met één tot vier vingers. Bij een taal die van rechts naar links wordt geschreven, verhoogt een swipe naar links de waarde en verlaagt een swipe naar rechts deze. Bij andere gebaren, sneltoetsen en opdrachten van een brailleleesregel verhoogt **Leesinstellingen hoger of achteruit verplaatsen** de waarde en verlaagt **Leesinstellingen lager of vooruit verplaatsen** deze.

SandFly spreekt de naam van elke leesinstelling uit terwijl je deze selecteert. Naar rechts en links swipen beweegt per item, welke leesinstelling ook is geselecteerd, behalve met **Rijen en kolommen** in een tabel.

Leesinstellingen die alleen bij sommige items passen, zoals **Acties** bij een item met acties of **Rijen en kolommen** in een tabel, verschijnen in de lijst alleen waar ze van toepassing zijn. **Leesinstellingen** > **Leesinstelling automatisch wijzigen**, standaard aan, selecteert zo'n leesinstelling wanneer de focus naar een item beweegt dat deze gebruikt, zoals **Acties**, **Schuifregelaar aanpassen**, **Waarde aanpassen** of **Proeflezen**, en selecteert de eerdere leesinstelling opnieuw wanneer de focus weg beweegt. Zet dit uit om de door jou geselecteerde leesinstelling te behouden. In tabellen wordt **Rijen en kolommen** op deze manier alleen geselecteerd met **Navigatie** > **Overschakelen naar tabelnavigatie in tabellen** aan.

Het scherm **Leesinstellingen** stelt in welke leesinstellingen in de lijst staan en in welke volgorde. Het heeft ook lijsten met leesinstellingen, aangepaste leesinstellingen en de letters voor **Letter van een leesinstelling schrijven**, een opdracht die een leesinstelling selecteert op de letter die je op het scherm schrijft, zoals K voor **Koppen**. Deze worden beschreven in [](#commands).

Standaard heeft de lijst deze navigatieleesinstellingen: **Tekens**, **Woorden**, **Regels**, **Alinea's**, **Koppen**, **Links**, **Bedieningselementen**, **Schermzoeken** en, in tabellen, **Rijen en kolommen**. Je kunt de andere toevoegen op het scherm **Leesinstellingen**.

#### Tekst in stukken lezen {#reading-text}

Deze leesinstellingen bewegen door de tekst van het gefocuste item, en dan door naar het volgende of vorige item:

- **Tekens**: één teken tegelijk.
- **Woorden**: één woord tegelijk.
- **Regels**: één regel tegelijk.
- **Zinnen**: één zin tegelijk. Een zin eindigt bij een zinsleesteken, waaronder Chinese 。！？, en aan het einde van een regel. Op webpagina's wordt een zin die doorloopt in een ander item, zoals een link daarbinnen, in delen voorgelezen. Niet standaard in de lijst, en niet beschikbaar in selectiemodus. De opdrachten **Volgende zin** en **Vorige zin**, in de categorie **Navigeren op tekst**, bewegen per zin zonder de leesinstelling.
- **Alinea's**: één alinea tegelijk.

In een tekstveld dat wordt bewerkt, bewegen deze leesinstellingen de cursor, en blijven swipes in het veld. Voor het bewerken van tekst, zie [](#typing).

SandFly beweegt ook door tekst die een app zelf tekent en niet door schermlezers laat bewegen, zoals berichten in Telegram.

#### Op type item bewegen {#reading-kinds}

Deze leesinstellingen bewegen naar het volgende of vorige item van één soort. Swipe omlaag voor het volgende en omhoog voor het vorige.

- **Standaardnavigatie**: één item tegelijk, zoals naar rechts en links swipen doet. Niet standaard in de lijst.
- **Koppen**, **Links** en **Bedieningselementen**: koppen, links, en items die je kunt gebruiken, zoals knoppen en tekstvelden. Deze werken in apps en op webpagina's.
- **Vensters**: andere vensters op het scherm, zoals de volumeregeling of de navigatiebalk. Niet standaard in de lijst.
- **Containers**: uit de huidige container naar de volgende of vorige. Containers zijn lijsten, rasters, scrollweergaven, werkbalken, tabbladbalken, navigatiebalken, zoekbalken en vensterdelen. Een balk binnen een balk, zoals een zoekbalk in een werkbalk, telt als één. Teruggaan beweegt naar het begin van de vorige container, behalve op webpagina's. Niet standaard in de lijst.
- **Verticale navigatie**: het item onder of boven. Zie [](#reading-vertical).
- **Schermzoeken**: de resultaten van de laatste schermzoekopdracht. Zie [](#reading-search).
- **Scrollen**: scrolt de lijst vooruit wanneer je omlaag swipet en terug wanneer je omhoog swipet. Wanneer een lijst niet op scrollen reageert, stuurt SandFly in plaats daarvan een swipe naar de lijst. Niet standaard in de lijst.

De leesinstellingen onder **Elementnavigatie** en **Kopniveaus** op het scherm **Leesinstellingen**, zoals **Herkenningspunten**, **Knoppen**, **Bewerkingsvelden**, **Lijsten** en **Tabellen**, bewegen op de elementen van webpagina's, en werken ook in apps. Zie [](#web).

**Leesinstellingen** > **Links en Bedieningselementen alleen in webinhoud**, standaard uit, laat **Links** en **Bedieningselementen** buiten de lijst buiten webinhoud. Gebaren en sneltoetsen voor de volgende of vorige link of bediening werken nog overal.

#### Op tekst of element bewegen zonder leesinstelling {#reading-navigation-commands}

Je kunt een gebaar of sneltoets toewijzen om naar het volgende of vorige teken, woord, regel, alinea, kop, link, bedieningselement, herkenningspunt, knop, selectievakje, keuzerondje, invoerveld, keuzelijst, focusbaar item, afbeelding, lijst, lijstitem, tabel, bezochte link, niet-bezochte link, of kop van een opgegeven niveau te gaan. Deze bewegingen veranderen de geselecteerde leesinstelling niet. In de lijst met opdrachten voor een gebaar staan ze onder **Navigeren op tekst** en **Navigeren op element**. Zie [](#gestures).

### Doorlopend lezen {#reading-continuous}

SandFly kan elk item na elkaar voorlezen, en lijsten onderweg scrollen:

- **Lezen vanaf het begin**: leest vanaf het eerste item op het scherm.
- **Lezen vanaf item waarop is gefocust**: leest vanaf het gefocuste item. Dit staat standaard in het SandFly-menu.
- **Voorlezen vanaf gefocust item per leesinstelling**: leest vanaf het gefocuste item, en dan alleen de items van de geselecteerde leesinstelling, zoals elke kop of elke link. Een leesinstelling die niet tussen items van één soort beweegt, leest elk item voor.

Deze opdrachten staan in de categorie **Lezen** van opdrachten en hebben standaard geen gebaar. Je kunt ze uitvoeren vanuit het SandFly-menu, dat een tik met drie vingers opent, of ze toewijzen aan een gebaar of sneltoets. Zie [](#commands).

Terwijl SandFly doorlopend leest:

- Swipe naar rechts of links om naar het volgende item te springen of naar het vorige terug te gaan. Het lezen gaat vanaf daar door.
- Druk op de volumetoetsen om het spraakvolume te veranderen.
- Om te stoppen met lezen, raak het scherm aan of gebruik een ander gebaar.

De eerste keer dat je doorlopend lezen start, toont SandFly **Hoe doorlopend lezen werkt**. Om dit elke keer te tonen, selecteer **Dit bericht altijd tonen**.

SandFly houdt het scherm aan terwijl het leest.

### Scrollen {#reading-scroll}

Naar rechts en links swipen scrolt lijsten zelf wanneer het volgende item buiten beeld is. Je kunt ook zelf scrollen:

- Om vooruit te scrollen, swipe naar rechts en dan naar links. Dit is **Vooruit scrollen**.
- Om terug te scrollen, swipe naar links en dan naar rechts. Dit is **Terug scrollen**. Boven aan een lijst trekt **Terug scrollen** de lijst omlaag, wat deze in de meeste apps vernieuwt. Omlaag trekken heeft Android 13 of hoger nodig.
- Om de app direct te scrollen, sleep twee vingers over het scherm, zoals je zonder SandFly één vinger zou slepen.

In de standaardgebarenindeling gaan swipes met twee vingers naar de app en scrollen deze. Met **Gebaren** > **Swipes met twee vingers in SandFly verwerken** aan, zoals in de VoiceOver-gebarenindeling, voeren swipes met twee vingers in plaats daarvan opdrachten uit: standaard **Omhoog scrollen**, **Omlaag scrollen**, **Naar links scrollen** en **Naar rechts scrollen**, die de inhoud in de richting van de swipe bewegen. Als geen enkel item op het scherm zijwaarts scrolt, sturen **Naar links scrollen** en **Naar rechts scrollen** een swipe met één vinger naar de pager rond het gefocuste item, zoals de maptabbladen in Solid Explorer. Deze vier opdrachten hebben Android 11 of hoger nodig, en staan niet op horloges.

Als niets op het scherm in die richting kan scrollen, zoals aan het einde van een lijst, spelen **Vooruit scrollen**, **Terug scrollen** en deze vier opdrachten het geluid **Actie voltooid of einde bereikt** af. Boven aan een lijst trekken **Terug scrollen** en **Omlaag scrollen** de lijst in plaats daarvan omlaag, zonder geluid. Vanaf een brailleleesregel spelen **Vooruit scrollen** en **Terug scrollen** in plaats daarvan het geluid **Braille-opdracht mislukt** af.

Deze opdrachten, in de categorie **Scrollen**, hebben standaard geen gebaar:

- **Naar boven scrollen** en **Naar beneden scrollen** scrollen de lijst die focus heeft naar het begin of einde, een pagina per keer, en focussen dan het eerste of laatste item. Een aanraking, gebaar of toets stopt ze. Niet op horloges.
- **Vernieuwen** scrolt de lijst naar boven en trekt deze omlaag. Dit heeft Android 13 of hoger nodig.

Terwijl je met twee vingers scrolt, speelt SandFly het scrollgeluid en een lichte trilling voor elk item dat voorbij scrolt. De toonhoogte van het scrollgeluid stijgt van boven naar onder in de lijst. Scrolls die SandFly maakt om het volgende item te bereiken, maken geen geluid. Voor geluids- en trillingsinstellingen, zie [](#sound).

#### Scrollen tijdens swipes {#reading-scroll-swipes}

Deze instellingen op het scherm **Navigatie** veranderen hoe swipes lijsten scrollen:

- **Alvast scrollen naar het volgende item**: Wanneer het gefocuste item het laatste item van een lijst op het scherm is, scrolt SandFly de lijst vooruit terwijl het het item voorleest, tot het volgende item op het scherm staat, zodat de volgende swipe niet op een scroll moet wachten. De focus blijft op het item, en de scroll maakt geen geluid. Na een beweging terug scrolt SandFly in plaats daarvan terug. Het scrolt niet vooruit in tekstvelden, in webinhoud of tijdens doorlopend lezen, en stopt wanneer je het scherm aanraakt, op een toets drukt of een venster verandert. Standaard aan.
- **Volgende item uitspreken tijdens scrollen**: Wanneer een swipe een lijst moet scrollen om het volgende item te bereiken, focust en leest SandFly het item zodra het op het scherm komt, terwijl de lijst nog beweegt. Zet dit uit om te wachten tot de lijst tot stilstand komt. Standaard aan.
- **Items uitspreken voordat ze in beeld scrollen**: Wanneer een swipe naar een item beweegt dat slechts deels op het scherm staat, leest SandFly dit meteen voor, inclusief de delen die nog buiten de rand van de lijst vallen, in plaats van te wachten tot de lijst scrolt. Standaard uit, omdat sommige apps verborgen tekst buiten een item bewaren in plaats van die te verbergen, en die tekst dan wordt voorgelezen.

### Lijsten en tabellen {#reading-lists-tables}

Wanneer de focus een lijst binnengaat of verlaat, leest SandFly lijstinformatie voor, zoals de positie van het item. **Uitgebreidheidsniveau** > **Lijstinformatie uitspreken**, **Raster- en tabelinformatie uitspreken** en **Aantal lijstitems altijd uitspreken** stellen in wat wordt voorgelezen. Zie [](#verbosity).

De leesinstelling **Lijstitems** en de gebaren en sneltoetsen voor lijstitems bewegen naar het volgende of vorige item van de lijst die focus heeft. Ze gaan voorbij de knoppen en andere delen binnen elk item, zoals de Deel-knop van een bericht, en scrollen de lijst wanneer het volgende item buiten beeld is. Na het laatste item gaan ze door naar de volgende lijst. Buiten een lijst bewegen ze naar het volgende lijstitem op het scherm.

#### Door tabellen navigeren {#reading-tables}

In een tabel beweegt de leesinstelling **Rijen en kolommen** per cel:

- Swipe omlaag of omhoog om naar de volgende of vorige rij te gaan.
- Swipe naar rechts of links om naar de volgende of vorige kolom te gaan.

Terwijl **Rijen en kolommen** is geselecteerd, bewegen naar rechts en naar links swipen altijd per kolom, wat een gebaarprofiel, een appgebaar of een script er ook aan toewijst. Wanneer de focus de tabel verlaat, selecteert SandFly de eerdere leesinstelling opnieuw.

De leesinstellingen **Rij** en **Kolom** bewegen alleen per rij of alleen per kolom met swipes omlaag en omhoog. Ze staan niet standaard in de lijst. **Volgende rij**, **Vorige rij**, **Volgende kolom** en **Vorige kolom** zijn ook opdrachten, in de categorie **Tabellen**, die je aan gebaren kunt toewijzen.

**Navigatie** > **Overschakelen naar tabelnavigatie in tabellen**, standaard uit, selecteert **Rijen en kolommen** wanneer de focus een tabel binnengaat.

De leesinstellingen voor tabellen verschijnen in de lijst alleen in tabellen. In Google Spreadsheets behandelt SandFly het raster van cellen als een tabel, zodat de leesinstellingen en opdrachten voor tabellen tussen cellen bewegen, en de acties van een cel **Cel bewerken** en **Formule invoegen** omvatten.

Of SandFly rij- en kolomnummers en kolomkoppen voorleest bij elke cel, wordt ingesteld door **Uitgebreidheidsniveau** > **Rij- en kolomnummers uitspreken** en **Kolomkoppen voorlezen**. Zie [](#verbosity). Toetsenbordopdrachten die de rij- en kolomkoppen voorlezen, worden beschreven in [](#keyboards).

### Vensters, containers en items daaronder {#reading-windows}

- Om naar het volgende venster te gaan, swipe met vier vingers omlaag. Om naar het vorige venster te gaan, swipe met vier vingers omhoog. Dit zijn **Volgend venster** en **Vorig venster**.
- Om naar de volgende container te gaan, swipe met vier vingers naar rechts. Om naar de vorige container te gaan, swipe met vier vingers naar links. Dit zijn **Volgende container** en **Vorige container**, die bij dezelfde containers stoppen als de leesinstelling **Containers**.

**Navigatie** > **Van de statusbalk en navigatiebalk naar andere vensters gaan**, standaard uit: Wanneer de focus in de statusbalk of de navigatiebalk staat, zoals na het aanraken ervan, beweegt swipen voorbij het eerste of laatste item van de balk naar de app. Wanneer dit uit staat, gaan swipes om de balk heen.

#### Verticale navigatie {#reading-vertical}

De leesinstelling **Verticale navigatie** beweegt naar het item onder wanneer je omlaag swipet en het item boven wanneer je omhoog swipet. Deze gaat voorbij de andere items van dezelfde rij, zoals de knoppen naast de titel van een video of de naam van een bestand, en blijft in de kolom van het gefocuste item: vanaf een toets op een toetsenblok beweegt deze naar de toets onder of boven. In een lijst blijft deze in de lijst, en scrolt de lijst een pagina wanneer er geen rij onder of boven op het scherm staat. Aan het einde van de lijst, of buiten een lijst, beweegt deze naar het item onder of boven op het scherm, en speelt het eindgeluid wanneer er geen is.

De opdrachten **Naar item onder gaan** en **Naar item boven gaan** maken dezelfde bewegingen vanuit een gebaar of sneltoets, zonder de leesinstelling. **Naar item links gaan** en **Naar item rechts gaan** bewegen naar het dichtstbijzijnde item op het scherm in die richting. Geen van deze heeft standaard een gebaar.

### Focus wanneer het scherm verandert {#reading-screen-changes}

Wanneer een venster opent, of je teruggaat naar een scherm, beweegt SandFly de focus daarin: naar het item waar je was, naar het tekstveld met de cursor, of naar het eerste item. Dit geldt ook voor SandFly-menu's en dialoogvensters.

Nadat je een item in een lijst activeert, zoals een map in een bestandsbeheerder of een pagina in Instellingen, en teruggaat, keert de focus terug naar dat item wanneer de lijst dit binnen twee seconden weer toont. Dit werkt ook in apps die de lijst veranderen zonder een nieuw venster te openen.

Om de focus te laten staan waar deze is, zet **Navigatie** > **Verplaats de focus naar nieuwe vensters** uit. SandFly leest dan alleen de naam van het venster voor, en de volgende swipe beweegt naar het eerste item. Focus volgt invoerfocus alleen wanneer je invoerfocus verplaatst met een toets, zoals een pijltoets of Tab. Op tv's beweegt de focus altijd naar nieuwe vensters.

Wanneer het gefocuste item verdwijnt, zoals een advertentie die inklapt of een lijstrij die wordt verwijderd, focust SandFly het item dat nu op die plaats staat, en gaat het voorlezen vanaf daar door.

### Het scherm doorzoeken {#reading-search}

Schermzoeken vindt items op het scherm aan de hand van hun tekst.

1. Swipe naar links en dan omlaag. Dit is de opdracht **Schermzoeken**, in de categorie **SandFly**.
2. Typ een woord in het zoekveld. SandFly vermeldt de overeenkomende items en leest het aantal overeenkomsten voor.
3. Dubbeltik op een resultaat om de focus daarnaar te verplaatsen.

Het zoekvenster heeft ook **Volgend scherm** en **Vorig scherm**, die het scherm erachter scrollen om meer van een lijst te doorzoeken, **Invoer wissen** en **Zoekopdracht afsluiten**.

Na een zoekopdracht beweegt de leesinstelling **Schermzoeken** tussen de resultaten: swipe omlaag voor het volgende resultaat en omhoog voor het vorige. Schermzoeken staat niet op horloges.

### Adressen en nummers {#reading-addresses}

De leesinstelling **Adressen en nummers** vermeldt de webadressen, e-mailadressen, telefoonnummers en codes van 4 tot 8 cijfers in de tekst van het gefocuste item, ook wanneer de app er geen links van maakt, zoals in een chatbericht of in een melding met een eenmalige code.

1. Voeg op het scherm **Leesinstellingen** **Adressen en nummers** toe. Dit staat standaard uit.
2. Focus het item, en selecteer **Adressen en nummers**.
3. Swipe omhoog of omlaag tot SandFly het adres of nummer voorleest dat je wilt.
4. Dubbeltik. Een webadres opent, een e-mailadres opent een nieuwe e-mail, een telefoonnummer opent in de telefoon-app (**Bellen** en het nummer), en een code wordt gekopieerd (**Kopiëren** en de code).

**Adressen en nummers** is ook een item van het SandFly-menu en een opdracht, in de categorie **Activeren**. **Leesinstellingen** > **Op te nemen adressen en nummers** zet elke soort aan of uit: **Webadressen**, **E-mailadressen**, **Telefoonnummers** en **Cijfercodes**.

Telefoonnummers worden gevonden door de tekstclassificatie van Android, die ook datums, tijden, straatadressen en vluchtnummers vindt. Cijfers in een van deze, en cijfers verbonden aan een woord, een gebruikersnaam, een tijd of een bedrag, worden niet als codes vermeld. Andere nummers van 4 tot 8 cijfers, zoals een jaar in een kop, worden als codes vermeld. De tekst wordt gelezen wanneer je de leesinstelling swipet of het SandFly-menu opent, wat tot een seconde kan duren. Op Android 8, en wanneer de tekstclassificatie niet binnen een seconde antwoordt, worden alleen web- en e-mailadressen vermeld.

### Tekst van het scherm kopiëren {#reading-copy-text}

**Tekst van scherm kopiëren**, een opdracht in de categorie **Tekstverwerking** zonder gebaar standaard, opent een venster met de tekst op het scherm als een lijst met selectievakjes.

Het venster toont de tekst op het scherm. Als het scherm een lijst of pagina heeft die scrolt, scrollen **Omhoog scrollen** boven de lijst en **Omlaag scrollen** eronder die lijst een pagina verder dan de tot dan toe vermelde tekst, en wordt de tekst van die pagina toegevoegd. SandFly zegt hoeveel stukken er zijn toegevoegd, zoals "5 items toegevoegd". Aangevinkte stukken blijven aangevinkt. Als de lijst aan het begin of einde staat, zegt SandFly "Geen tekst meer". De lijst scrollt terug naar waar hij stond toen het venster sluit.

1. Voer **Tekst van scherm kopiëren** uit.
2. Kies onder **Granulariteit** hoe de tekst wordt verdeeld: **Adressen en nummers**, **Tekens**, **Woorden** of **Alinea's**. **Woorden** is als eerste geselecteerd.
3. Vink de stukken aan die je wilt. Om een reeks aan te vinken, dubbeltik en houd het eerste stuk vast en dan het laatste, of gebruik de acties **Selectie starten** en **Selectie beëindigen**. **Alles selecteren** vinkt elk stuk aan.
4. Selecteer **Kopiëren**. De aangevinkte stukken worden op volgorde gekopieerd: woorden met een spatie ertussen, tekens zonder iets ertussen, alinea's met een lege regel ertussen, en adressen en nummers één per regel.

**Adressen en nummers** vermeldt elk webadres, e-mailadres, telefoonnummer en code op het scherm zoals geschreven, zodat je een telefoonnummer kunt kopiëren in plaats van het te bellen. Dit verschijnt alleen wanneer het scherm die heeft, en vermeldt elke soort, wat **Op te nemen adressen en nummers** ook is ingesteld. Ze vinden kan het venster tot een seconde langer laten openen.

Het venster bevat maximaal 1000 stukken; wanneer de tekst langer is, leest het "Lijst afgekapt bij" en het aantal stukken. Voor andere manieren om tekst te kopiëren, zie [](#typing).

### Navigatie-instellingen {#reading-navigation-settings}

Het scherm **Navigatie** in de instellingen voor SandFly heeft deze instellingen:

- **Rondgaan**: Voorbij het laatste item swipen gaat naar het eerste, en voorbij het eerste naar het laatste. Standaard aan. Zie [](#reading-wrap).
- **Stoppen bij het laatste item voor het rondgaan**: De eerste swipe voorbij het einde stopt daar; de volgende gaat rond. Standaard aan.
- **Rondgaan in leesinstellingen**: Waarden van leesinstellingen gaan rond van de laatste naar de eerste. Volgt **Rondgaan** tot je dit verandert.
- **Van de statusbalk en navigatiebalk naar andere vensters gaan**: Swipes voorbij het einde van een systeembalk bewegen naar de app. Standaard uit. Zie [](#reading-windows).
- **Verplaats de focus naar nieuwe vensters**: De focus beweegt naar vensters die openen. Standaard aan. Zie [](#reading-screen-changes).
- **Items uitspreken voordat ze in beeld scrollen**: Items die deels buiten beeld staan, worden voorgelezen voordat de lijst scrolt. Standaard uit. Zie [](#reading-scroll-swipes).
- **Alvast scrollen naar het volgende item**: Lijsten scrollen alvast terwijl het laatste item op het scherm wordt voorgelezen. Standaard aan.
- **Volgende item uitspreken tijdens scrollen**: Het volgende item wordt voorgelezen zodra het op het scherm komt. Standaard aan.
- **Tekst in items afzonderlijk focussen**: Elk stuk tekst en elke status binnen een item krijgt zijn eigen focus, zodat je het kunt bereiken door te swipen en door aanraking. Het item zelf houdt alleen focus wanneer het eigen tekst, een eigen status of eigen acties heeft; het activeren van een stuk tekst daarbinnen activeert het item. Standaard uit. De opdracht **Tekst in items afzonderlijk focussen**, in de categorie **Navigatie**, zet dit aan of uit en leest "Tekst binnen items afzonderlijk focussen aan" of "Tekst binnen items afzonderlijk focussen uit" voor.
- **Overschakelen naar tabelnavigatie in tabellen**: **Rijen en kolommen** wordt geselecteerd wanneer de focus een tabel binnengaat. Standaard uit. Zie [](#reading-tables).
- **Slimme browsemodus** en **Browsemodus per app onthouden**: Zie [](#web).
- **Antwoordcitaten in Telegram voorlezen met het bericht**, onder **Apps**: Zie [](#reading-telegram).

#### Telegram {#reading-telegram}

In Telegram en apps die erop zijn gebaseerd, zoals Nekogram:

- Met **Navigatie** > **Antwoordcitaten in Telegram voorlezen met het bericht** aan, wat de standaard is, leest SandFly het citaat van het bericht waarop een bericht reageert samen met het bericht voor, na de tijd, als "in antwoord op" de verzender en de tekst. Het citaat krijgt geen eigen focus; **Naar geciteerd bericht**, in de acties van het bericht, toont het geciteerde bericht. Zet de instelling uit om het citaat als eigen item te hebben; **Naar geciteerd bericht** wordt dan niet aangeboden.
- De Deel-knop naast elk bericht in een kanaal krijgt geen focus wanneer je swipet of het scherm aanraakt. Deze staat in de acties van het bericht, onder de naam die Telegram ervoor geeft, zoals **Delen**.
