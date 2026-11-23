## Macro's {#macros}

Een macro is een benoemde lijst met stappen die SandFly na elkaar uitvoert: commando's, tikken op items, getypte tekst, wachttijden, en stappen die het scherm controleren en andere stappen herhalen of overslaan. Elke macro is een opdracht, dus een gebaar, een sneltoets, een menu-item, een spraakopdracht of een startscherm-snelkoppeling kan hem uitvoeren.

Je maakt macro's in **Macro's**, in Instellingen voor SandFly, door stappen toe te voegen of door op te nemen wat je doet. Macro's zijn niet beschikbaar op horloges. Voor scripts, die meer kunnen dan macro's, zie [](#scripts).

### Maak een macro {#macros-add}

1. Ga in Instellingen voor SandFly naar **Macro's**.
2. Kies **Macro toevoegen** en voer een naam in. SandFly opent het scherm van de nieuwe macro.
3. Kies **Stap toevoegen**, dan het soort stap, en vul in wat die doet. Zie [](#macros-steps).
4. Herhaal stap 3 voor elke stap.
5. Om de macro te proberen, kies **Nu uitvoeren**.
6. Om hem uit te voeren met een gebaar of sneltoets, kies **Gebaren en sneltoets**. Zie [](#commands-assign).

Om de stappen in plaats daarvan te maken door ze uit te voeren, zie [](#macros-record).

### Het scherm Macro's {#macros-screen}

Het scherm **Macro's** heeft:

- **Macro toevoegen**: maakt een macro voor alle apps.
- **App toevoegen**: kiest een app en opent de lijst met zijn macro's, waar **Macro toevoegen** een macro maakt die alleen in die app werkt.
- **Importeren uit bestand** en **Exporteren naar bestand**: lezen macro's uit een bestand, of slaan alle macro's op in één bestand. In de lijst van een app slaat **Exporteren naar bestand** de macro's van die app op. Zie [](#api-item-files).
- **Alle apps**: de macro's die in elke app werken.
- **Apps**: elke app die macro's heeft, met het aantal macro's. Kies een app om zijn lijst te openen.

Macro's worden ook opgeslagen in backups van instellingen. Zie [](#profiles).

### Het macroscherm {#macros-macro-screen}

Het scherm van een macro heeft:

- **Nu uitvoeren**: voert de macro uit. Als SandFly uit staat, toont dit "SandFly is uit".
- **Stap toevoegen**: voegt een stap toe. Daaronder toont SandFly waar de stap komt: **Aan het einde**, **Aan het begin**, of na een stap. Een nieuwe stap komt na de stap die je het laatst hebt toegevoegd, zodat stappen die je na **Als** of een herhaling toevoegt, in dat blok terechtkomen.
- **Stappen opnemen**: neemt stappen op waar **Stap toevoegen** ze toevoegt. Zie [](#macros-record).
- **Alleen uitvoeren als**: een voorwaarde waaraan moet worden voldaan om de macro uit te voeren. Standaard **Altijd**. Als niet aan de voorwaarde wordt voldaan, doet de macro niets, en **Nu uitvoeren** toont "Macro niet uitgevoerd. Alleen uitvoeren als: niet voldaan". Zie [](#macros-conditions).
- **App**: de app waarin de macro werkt, of **Alle apps**, de standaardwaarde. Een macro met een app werkt alleen terwijl die app op de voorgrond staat; elders zegt **Nu uitvoeren** "Macro niet uitgevoerd. Werkt alleen in" en de naam van de app.
- **Tonen in Acties**: toont de macro in het menu **Acties** en de leesinstelling **Acties** van elk item in zijn app, of in elke app bij **Alle apps**, na de acties van de app. Door hem daar te kiezen, voer je de macro uit. Standaard uit.
- **Alleen tonen in Acties als**: getoond terwijl **Tonen in Acties** aan staat. Een voorwaarde waaraan het gefocuste item moet voldoen om de macro in zijn menu Acties te tonen. Met een voorwaarde geldt de macro als een actie van het item voor **Leesinstelling automatisch wijzigen**, zodat de leesinstelling **Acties** erop is geselecteerd; zonder voorwaarde niet.
- **Stappen**: de stappen in volgorde. Stappen binnen een **Als** of een herhaling zijn ingesprongen.
- **Gebaren en sneltoets**: de gebaren, pijltoetsen, braillecellen en sneltoets die de macro uitvoeren.
- **Toevoegen aan startscherm**: voegt een startscherm-snelkoppeling toe die de macro uitvoert op het scherm van waaruit je hem toevoegt. Het hernoemen van de macro hernoemt de snelkoppeling; het verwijderen van de macro schakelt haar uit. Niet elke launcher ondersteunt snelkoppelingen.
- **Hernoemen**, **Exporteren naar bestand** en **Macro verwijderen**.

Kies een stap voor deze acties, die ook toegankelijkheidsacties op de stap zijn: **Bewerken**, **Stap toevoegen na**, **Omhoog verplaatsen**, **Omlaag verplaatsen** en **Verwijderen**. Het verwijderen van een **Als** of een herhaling verwijdert ook de bijbehorende stappen **Anders als**, **Anders** en de eindstap, en behoudt de stappen daarbinnen. Een macro heeft maximaal 200 stappen.

Als de blokken van een macro niet overeenkomen, zoals een **Als** zonder **Einde als**, toont het scherm de stap en het probleem onder **Stappen**, en wordt de macro niet uitgevoerd.

### Stappen {#macros-steps}

**Stap toevoegen** biedt deze stappen:

- **Commando uitvoeren**: voert elk commando uit behalve een macro. Kies een categorie, dan het commando.
- **Item aantikken**: activeert het eerste item op het scherm waarvan de tekst of beschrijving een tekst bevat, ongeacht hoofd- of kleine letters. Een item waarvan de hele tekst de tekst is, komt als eerste. SandFly wacht op het item, standaard 3 seconden, ingesteld met **Seconden wachten op het item**.
- **Tikken op positie**: tikt op het scherm op een X en een Y in pixels vanaf linksboven. Vereist Android 11 of hoger.
- **Item aanraken en vasthouden**: raakt het midden van een item aan voor **Seconden vasthouden**, standaard 1 seconde, en tilt dan op dezelfde plek op of schuift omhoog, omlaag, naar links of naar rechts over een afstand in dp voordat hij optilt. Voor knoppen die reageren terwijl ze worden vastgehouden, zoals de knop voor spraakberichten van een chatapp. Vereist Android 11 of hoger.
- **Item focussen**: zoekt een item zoals **Item aantikken** doet en verplaatst de toegankelijkheidsfocus ernaartoe, waardoor het wordt gelezen.
- **Item uitspreken**: zoekt een item zoals **Item aantikken** doet en leest zijn naam voor zonder de focus te verplaatsen.
- **Een item lang indrukken of scrollen**: drukt lang op een item, scrollt het vooruit of scrollt het terug, een aantal keer. Voor een item dat dit niet kan, gebruikt SandFly het dichtstbijzijnde item dat het wel kan, zoals een schuifregelaar. Een scroll zonder itemtekst scrollt de grootste lijst op het scherm.
- **Tekst uitspreken**: leest een tekst voor.
- **Tekst typen**: typt een tekst in het gefocuste bewerkingsveld, bij de cursor. Met **Tekst in veld vervangen** vervangt de tekst de tekst van het veld.
- **App openen**: opent een app.
- **Meer stappen**: stappen die buiten het scherm werken. Zie [](#macros-more-steps).
- **Wachten**: wacht een aantal seconden, tot 600.
- **Wachten tot**: wacht tot aan een voorwaarde is voldaan. **Seconden wachten** is standaard 10; de macro stopt als de tijd verstreken is.
- **Als**, **Anders als**, **Anders** en **Einde als**: voeren de stappen uit van de eerste branch waaraan is voldaan. Zie [](#macros-if).
- **Herhalen**, **Herhalen zolang**, **Einde herhalen** en **Lus verlaten**: voeren stappen meer dan één keer uit. Zie [](#macros-repeat).
- **Macro stoppen**: stopt de macro.

Als een stap mislukt, zegt SandFly "Macro gestopt. Mislukt:" gevolgd door de stap, en de macro stopt. Als het item van een stap niet wordt gevonden, zegt SandFly "Macro gestopt. Niet gevonden:" en de tekst van het item. Een **Wachten tot** waarvan de tijd verstrijkt, zegt "Macro gestopt. Time-out:" en de stap.

#### Zoek items {#macros-find-items}

De stappen en tests die een item zoeken, hebben deze velden:

- **Itemtekst**: de tekst om naar te zoeken.
- **Reguliere expressie**: leest **Itemtekst** als een reguliere expressie, overal in de tekst gezocht en ongeacht hoofd- of kleine letters. Herhaalde groepen mogen geen herhaling of `|` bevatten, zoals `(a+)+`.
- **Zoeken in**: waar te zoeken. **Willekeurige tekst**, de standaardwaarde, of alleen **Tekst**, **Beschrijving**, **Hint**, **Status**, **Tooltip** of **Resource-ID**. Een resource-ID komt overeen met of zonder zijn pakket, dus `send` komt overeen met `com.example:id/send`.
- **Itemtype**, behalve in **Focus is binnen een item**: **Elk type**, de standaardwaarde, of alleen items die **Aanklikbaar**, **Lang aanklikbaar**, **Bewerkbaar**, **Scrollbaar**, **Aanpasbaar**, **Aanvinkbaar**, **Aangevinkt**, **Geselecteerd**, **Kop** of **Aangezet** zijn. Met **Aanklikbaar** tikt **Item aantikken** op een knop en niet op een kop met dezelfde tekst. Tests vragen om het itemtype na de tekst.

Om te zien wat een item in elk veld heeft en welke itemtypen het is, gebruik **Gefocust item inspecteren**. Zie [](#macros-inspect).

#### Meer stappen {#macros-more-steps}

- **Geluid afspelen**: speelt een van de geluiden van SandFly af.
- **Trillen** en **Toon afspelen**.
- **Tekst kopiëren**: kopieert een tekst naar het klembord.
- **Link openen**: opent een adres.
- **Instellingenpagina openen**: opent een pagina van de Android-instellingen, zoals Wi-Fi, Bluetooth of Batterij.
- **Tekst delen**: opent het deelscherm met een tekst.
- **Volume instellen**: stelt het volume van een stream in procenten in, zoals media, bel of toegankelijkheid.
- **Belmodus instellen**: normaal, trillen of stil.
- **Apparaatinstelling**: wijzigt een bediening van het snelle-instellingenpaneel van SandFly, zoals Wi-Fi, de zaklamp, Niet storen of helderheid: **Aanzetten**, **Uitzetten** of **Aan- of uitzetten** voor een schakelaar; een getal, een percentage zoals 50%, of +1 of -1 voor een schuifregelaar; een optie, of next, voor een keuze. Zie [](#display).
- **Mediatoets**: **Afspelen of onderbreken**, **Afspelen**, **Onderbreken**, **Volgende**, **Vorige**, **Stoppen**, **Terugspoelen** of **Vooruitspoelen**.
- **Timer instellen**: een aantal seconden, tot 86.400, met een optioneel label.
- **Wekker instellen**: een tijd zoals 08:30, met een optioneel label.
- **Bellen**: belt een telefoonnummer.
- **Bericht sturen**: schrijft een bericht naar een telefoonnummer.
- **Camera openen**: voor een foto of een video.
- **Toetsen indrukken**: drukt toetsen in, zoals `tab enter`. Vereist Shizuku. Zie [](#updates).
- **Bericht tonen**: toont een dialoogvenster met een tekst, en wacht tot je het sluit.
- **Waarde instellen**: bewaart een tekst als een waarde met een naam.
- **Link ophalen**: bewaart de tekst op een https-adres als een waarde.
- **Tekst vertalen**: vertaalt een tekst naar de taal van het apparaat en bewaart die als een waarde. Zie [](#translation).

Namen van waarden gebruiken letters, cijfers, punten, streepjes en underscores. Waarden worden tussen uitvoeringen bewaard, voor al je macro's samen.

De teksten van **Tekst uitspreken**, **Tekst typen** en de stappen hierboven kunnen waarden bevatten als `{naam}`, en deze waarden heeft elke uitvoering: `{item}`, de tekst van het gefocuste item; `{clipboard}`; `{time}`; `{date}`; `{battery}`, het batterijniveau; en `{device.wifi}` en dergelijke, de status van een apparaatinstelling.

### Voorwaarden {#macros-conditions}

Een voorwaarde is een lijst met tests. **Als**, **Anders als**, **Herhalen zolang**, **Wachten tot**, **Alleen uitvoeren als** en **Alleen tonen in Acties als** hebben er elk een. Kies de stap, of de instelling, om de voorwaarde te openen.

Het voorwaardescherm heeft **Voorwaarde toevoegen** en **Voorwaarden**, en voor **Wachten tot**, **Seconden wachten**. Een voorwaarde zonder tests is altijd voldaan.

De tests zijn:

- **Item op scherm**: een item op het scherm met een tekst, en van een itemtype als je er een kiest. Zie [](#macros-find-items).
- **Item met focus**: het item met toegankelijkheidsfocus, of de items daarbinnen, hebben een tekst, een itemtype, of beide. De tekst is optioneel.
- **Focus is binnen een item**: het gefocuste item staat binnen een item met een tekst, of is er zelf een, zoals een videospeler gevonden via zijn resource-ID.
- **App op de voorgrond**: een app staat op de voorgrond.
- **Toetsenbord getoond**: het schermtoetsenbord wordt getoond.
- **Scherm vergrendeld**: het vergrendelscherm wordt getoond.
- **Waarde**: een waarde bevat een tekst, of, met **Bevat** leeg, is ingesteld. Met **Reguliere expressie** is de tekst een reguliere expressie.

Als een voorwaarde tests heeft, vraagt **Voorwaarde toevoegen** eerst om **Verbinden met en** of **Verbinden met of**. Kies een test voor deze acties:

- **Bewerken**.
- **Wijzigen in**: keert de test om, zoals **Toetsenbord verborgen**, **Scherm ontgrendeld**, **Geen item op scherm** of **App niet op de voorgrond**.
- **Verbinden met en** of **Verbinden met of**, voor tests na de eerste.
- **Omhoog verplaatsen**, **Omlaag verplaatsen** en **Verwijderen**.

"En" gaat voor "of": aan de voorwaarde is voldaan als aan alle tests van een van de groepen die "of" scheidt, is voldaan. Een voorwaarde heeft maximaal 20 tests.

#### Als {#macros-if}

**Stap toevoegen** > **Als** vraagt om een test en voegt **Als** toe met zijn **Einde als**. De stappen die je daarna toevoegt, komen daartussen. **Anders als** voegt een andere branch toe met een voorwaarde, en **Anders** een branch voor als aan geen voorwaarde ervoor is voldaan. **Anders als** of **Anders** die direct na **Einde als** worden toegevoegd, komen daarvoor.

#### Herhalen {#macros-repeat}

- **Herhalen**: voert de stappen tot **Einde herhalen** een aantal keer uit, van 1 tot 1000.
- **Herhalen zolang**: voert de stappen tot **Einde herhalen** uit zolang aan een voorwaarde is voldaan, tot 1000 keer in één uitvoering van de macro. Na 1000 keer zegt SandFly "Macro gestopt. 1000 keer herhaald:" en de stap, en de macro stopt.
- **Lus verlaten**: gaat verder na de **Einde herhalen** van de herhaling waarin hij staat.

**Stap toevoegen** voegt een herhaling toe met zijn **Einde herhalen**, en de stappen die je daarna toevoegt, komen daarbinnen.

Om een lijst te scrollen tot een item verschijnt en het dan te openen:

1. Voeg **Herhalen zolang** toe met de test **Item op scherm**, de tekst van het item, en **Wijzigen in** zodat die **Geen item op scherm** wordt.
2. Voeg **Een item lang indrukken of scrollen** toe, **Vooruit scrollen**, zonder itemtekst.
3. Voeg na **Einde herhalen** **Item aantikken** toe met de tekst van het item.

### Neem een macro op {#macros-record}

SandFly kan de stappen van een macro opnemen terwijl je ze in apps uitvoert.

Om een nieuwe macro op te nemen:

1. Ga naar het scherm waar de macro begint.
2. Gebruik de opdracht **Een macro opnemen**, in de categorie **Macro's**. Wijs hem eerst toe aan een gebaar, of voeg hem toe aan het SandFly-menu. Zie [](#commands-assign) en [](#commands-edit-menu).
3. SandFly maakt een macro met de naam "Opgenomen" en de datum en tijd, en toont de melding **Macrostappen opnemen**.
4. Voer de stappen uit.
5. Om te stoppen, gebruik **Een macro opnemen** opnieuw, gebruik **Macro stoppen**, of open de melding. SandFly opent de macro en toont de opgenomen stappen.
6. Kies **Opslaan** om de stappen toe te voegen, **Opnieuw opnemen** om opnieuw te beginnen, of **Annuleren**. **Annuleren** verwijdert de macro.

Om stappen op te nemen in een bestaande macro, kies **Stappen opnemen** op het scherm ervan. SandFly gaat terug naar de app die open was voor de instellingen, of opent de app van de macro, en neemt op. De stappen komen waar **Stap toevoegen** een stap toevoegt.

SandFly neemt op: aanraken en lang aanraken van items, via aanraking of met SandFly, scrolls, getypte tekst, apps geopend vanuit de launcher, en de commando's **Terug**, **Home**, **Overzicht**, **Meldingen**, **Snelle instellingen**, **Alle apps**, **Alles selecteren**, **Kopiëren**, **Knippen** en **Plakken**. Een commando voor meldingen, snelle instellingen, overzicht of alle apps dat aan het einde wordt gebruikt om de opname te stoppen, wordt weggelaten. Tekst getypt in wachtwoordvelden en de schermen van SandFly worden niet opgenomen.

De opgenomen stappen bewaren ook wat de acties impliceren:

- Scrolls in een lijst gevolgd door een druk op een item daarin worden een **Herhalen zolang** die scrollt tot het item op het scherm staat, en dan de druk.
- Een actie, of een reeks acties, die achter elkaar meerdere keren wordt uitgevoerd, wordt een **Herhalen**, of een **Herhalen zolang** als het eerst gedrukte item op het scherm staat terwijl het na de laatste keer weg was.
- Een druk die een schakelaar of selectievakje aan- of uitzet, werkt alleen als die uit of aan staat.
- Een druk in de launcher die een app opende, wordt **App openen**.
- Als de eerste actie in de app staat die bij het begin op de voorgrond stond, opent de macro die app eerst als die niet op de voorgrond staat.

Opgenomen stappen zoeken items op hun hele tekst, als een reguliere expressie. Bewerk de stappen achteraf om ze items te laten zoeken op een deel van de tekst.

### Voer een macro uit {#macros-run}

- Een gebaar, sneltoets, pijltoets of braillecel die eraan is toegewezen. Zie [](#commands-assign).
- Een menu-item: voeg de macro toe aan het SandFly-menu, een aangepast menu of een aangepaste leesinstelling vanuit de categorie **Macro's**. Zie [](#commands).
- Het menu **Acties** of de leesinstelling **Acties**, met **Tonen in Acties** aan.
- Een startscherm-snelkoppeling, met **Toevoegen aan startscherm**.
- Een spraakopdracht: zeg de naam van de macro, of "voer de macro" naam "uit". Een aangepaste spraakopdracht kan hem ook uitvoeren. Zie [](#voice-commands).
- **Nu uitvoeren** op het macroscherm.

Het starten van een macro stopt de macro die loopt. De opdracht **Macro stoppen** stopt de lopende macro. Het verwijderen van een macro verwijdert hem uit de gebaren, toetsen en menu's die hem uitvoeren.

### Inspecteer items voor macro's {#macros-inspect}

**Gefocust item inspecteren**, een opdracht in de categorie **Ontwikkelaarsinstellingen**, opent **Itemdetails** voor het gefocuste item. Het venster heeft deze kopjes:

- **Teksten**: de tekst, beschrijving, hint en status van het item, benoemd zoals **Zoeken in** ze benoemt.
- **Itemtypen**: de itemtypen die het item is, zoals **Aanklikbaar** of **Kop**.
- **Items erin**: de teksten van de items daarbinnen.
- **Scriptdetails**: de resource-ID, rol, klasse en andere details die scripts gebruiken.
- **App**: de app, activiteit en het venster.

Activeer een regel om de waarde ervan te kopiëren. **Alles kopiëren** kopieert alle details als tekst. In builds met scripts opent **Regel voor dit item toevoegen** de scriptbouwer met het item. Zie [](#scripts).

**Itemdetails kopiëren** kopieert dezelfde details zonder het venster te openen, en zegt "Itemdetails gekopieerd". **Schermboom kopiëren** kopieert alle items op het scherm als tekst, één regel per item met zijn klasse, ID, rol, statussen en teksten, voor het schrijven van scripts.

**Gefocust item inspecteren**, **Itemdetails kopiëren** en **Schermboom kopiëren** kunnen worden toegevoegd aan het SandFly-menu; ze staan standaard uit. Om een item te inspecteren, verplaats de focus ernaartoe, open dan het SandFly-menu en kies **Gefocust item inspecteren**: het venster toont het item dat voor het menu focus had.
