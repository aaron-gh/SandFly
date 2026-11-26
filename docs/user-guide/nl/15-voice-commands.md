## Spraakopdrachten {#voice-commands}

Spraakopdrachten voeren SandFly-opdrachten uit via spraak: de focus bewegen, voorlezen, tekst bewerken, apps openen, items op het scherm activeren, een contact bellen, een bericht versturen, of het scherm beschrijven. Een spraakopdracht kan meerdere opdrachten achter elkaar bevatten. **Spraakopdrachten** in Instellingen voor SandFly zet AI aan voor opdrachten die SandFly niet begrijpt, stelt de tijdslimiet van spraakbewerking in, en bevat de spraakopdrachten die je zelf maakt. Het scherm **Spraakopdrachten** wordt niet getoond op horloges.

Zie [](#appendix-voice-commands) voor de volledige lijst met zinnen.

### Een spraakopdracht starten {#voice-commands-start}

1. Gebruik de opdracht **Spraakopdracht starten**. In de SandFly-gebarenindeling swipe je naar rechts en dan omhoog. Zie [](#appendix-gestures).
2. Zeg de opdracht.

SandFly toont **Bedien SandFly met spraakopdrachten** met voorbeelden van wat je kunt zeggen. Kies **Spraakopdrachten gebruiken** om de opname te starten. Schakel **Dit bericht altijd tonen** uit om te voorkomen dat het dialoogvenster weer wordt getoond.

Spraakopdrachten vereisen de microfoontoestemming. SandFly neemt op met de spraakherkenning die is gekozen in **Typen** > **Dicteren**. Zie [](#typing).

Spraakopdrachten zijn niet beschikbaar terwijl het scherm is vergrendeld of tijdens het instellen van het apparaat. Terwijl het scherm is vergrendeld, zegt SandFly dit en noemt het gebaar dat het SandFly-menu opent.

Om de opdrachten te tonen, zeg je "Hulp" of "Wat kan ik zeggen?". SandFly opent **Overzicht van spraakopdrachten**.

De opdracht **Spraakopdracht starten** staat ook in de categorie **Systeemacties**, zodat een sneltoets of een menu-item deze kan uitvoeren. Zie [](#commands-edit-menu) om deze aan het SandFly-menu toe te voegen.

### Wat je kunt zeggen {#voice-commands-phrases}

SandFly leest wat je zegt in deze volgorde:

1. Een aangepaste spraakopdracht, op zijn zin. Zie [](#voice-commands-custom).
2. De naam van elke opdracht, zoals SandFly die toont op het scherm **Commando's**, zoals "Lezen vanaf het begin", "Afbeelding beschrijven" of "Volgende kop". De namen van je macro's, aangepaste menu's en scriptopdrachten tellen ook. De naam van een app, zoals "Gmail", opent de app, tenzij een opdracht die naam heeft. Zie [](#commands-list).
3. Een zinspatroon, zoals "open Chrome", "tik op Instellingen", "bel Sam" of "typ hallo". Zie [](#voice-commands-sandfly).
4. Een van de oorspronkelijke spraakopdrachten, zoals "Zoeken opslaan", "Navigeren per Woorden", "sneller praten" of "kopiëren". Zie [](#appendix-voice-commands).
5. Met **Spraakopdrachten begrijpen met AI** aan, de AI-service. Zie [](#voice-commands-ai).

Hoofdletters en de interpunctie die de spraakherkenning toevoegt, maken niet uit. Woorden zoals "alsjeblieft", "kun je", "kan je", "zou je", "ik wil", "ik zou graag" en "nu" aan het begin, en "alsjeblieft", "graag", "voor mij" en "nu" aan het einde, worden weggelaten.

Als SandFly een opdracht niet begrijpt, zegt het "Deze opdracht wordt niet herkend" en hoe je de lijst met opdrachten kunt krijgen.

#### Apps, items en tekst {#voice-commands-sandfly}

- **Een app openen**: "open", "start", "ga naar" of "schakel over naar", en de naam van de app, of de naam van de app en "openen". SandFly kiest de app met die hele naam, anders een app waarvan de naam ermee begint, anders een app met een woord ervan in de naam. Als geen app overeenkomt, of meerdere apps evenveel overeenkomen, voert SandFly de opdracht met die naam uit, zoals "open meldingen"; als die er niet is, wordt de spraakopdracht niet begrepen.
- **Een item op het scherm activeren**: "tik (op)", "dubbeltik (op)", "tik dubbel (op)", "klik (op)", "druk op", "activeer" of "selecteer", en de tekst van het item. SandFly activeert het eerste item waarvan de hele tekst die tekst is, anders het eerste item waarvan de tekst die bevat. Als er geen is, zegt SandFly "Niet gevonden" en de tekst. "Druk op enter" voert in plaats daarvan **Nieuwe regel** uit. "Selecteer" en een tekst die in het gefocuste tekstveld staat, selecteert in plaats daarvan die tekst. Zie [](#voice-commands-text).
- **Tekst typen**: "typ" of "schrijf" en de tekst, of "voer" of "tik", de tekst en "in". SandFly typt die in het bewerkingsveld met focus en zegt de getypte tekst, behalve in een wachtwoordveld.
- **Een opdracht uitvoeren**: "voer" en de naam van een opdracht en "uit", "start" of "doe" en de naam van een opdracht, of "voer de macro" en de naam van een macro en "uit", zoals "voer de macro ochtend uit".
- **Wachten**: "wacht", "wacht een seconde", of "wacht" en een aantal seconden, tot 60. Gebruik dit tussen opdrachten in een keten.

#### Tekst in een tekstveld {#voice-commands-text}

Deze zinnen werken op de tekst van het tekstveld met invoerfocus.

- **De cursor verplaatsen**: "volgende", "vorige", "rechts", "links", "vooruit" of "terug", een aantal, en "tekens", "woorden", "zinnen", "regels" of "alinea's", zoals "volgende 3 woorden", "verplaats de cursor links vijf woorden" of "ga terug twee regels". "Verplaats de cursor" of "ga" kan ervoor staan. SandFly verplaatst de cursor en zegt de tekst die de cursor is gepasseerd.
- **Begin en einde**: "begin van" of "einde van", of "naar het begin van (de)" of "naar het einde van (de)", en "woord", "zin", "regel", "alinea" of "tekst", zoals "einde van regel" of "ga naar het begin van de zin". "Naar het begin" en "naar het einde" zijn het begin en einde van de tekst. SandFly verplaatst de cursor daarheen en zegt de tekst die de cursor is gepasseerd. Als de cursor al aan het begin of einde staat, gaat de cursor naar het begin van de vorige of het einde van de volgende.
- **Selecteren, kopiëren, knippen of verwijderen**: "selecteer", "kopieer", "knip" of "verwijder", en een van de zinnen hierboven, zoals "verwijder volgende vijf woorden", "kopieer vorige 2 zinnen", "selecteer volgende regel", "verwijder laatste woord" of "verwijder naar het einde van de regel". SandFly zegt "Geselecteerde tekst" en de geselecteerde tekst, "gekopieerd" en de gekopieerde tekst, "geknipt" en de geknipte tekst, of de verwijderde tekst en "verwijderd". Kopiëren laat de cursor staan.
- **Een tekst selecteren, kopiëren, knippen of verwijderen**: "selecteer", "kopieer", "knip" of "verwijder", en een tekst in het veld, zoals "selecteer krokodil". SandFly zoekt de tekst die het dichtst bij de cursor staat, zonder op hoofdletters en de interpunctie tussen de woorden te letten, en werkt op die tekst. Als het veld de tekst niet bevat, zegt SandFly "Niet gevonden" en de tekst.

Het aantal is 1 tot 20 in woorden, of cijfers; zonder aantal werkt de zin op één, zoals in "selecteer vorige woord". Het woord, de zin, de regel of de alinea waarin de cursor staat, telt als de eerste, vanaf de cursor. Woorden worden geteld zonder interpunctie. Een regel eindigt bij een regeleinde. Het verwijderen van hele woorden, zinnen, regels of alinea's verwijdert ook de spatie of het regeleinde dat overblijft. Als er niets meer is om over te bewegen, zegt SandFly "Begin van het veld" of "Einde van het veld".

De namen van opdrachten gaan voor: "Volgend woord" en "Vorig teken" voeren die opdrachten uit. Zie [](#voice-commands-phrases).

Als geen tekstveld invoerfocus heeft, activeert "selecteer" en een tekst het item met die tekst, en worden de andere zinnen gelezen als de oorspronkelijke spraakopdrachten. "Selecteer" en een tekst activeert ook het item als het gefocuste tekstveld een wachtwoordveld is of de tekst niet bevat. Bij de andere zinnen zegt SandFly in een wachtwoordveld "Niet beschikbaar in wachtwoordvelden".

#### Bellen en berichten {#voice-commands-calls}

- **Bellen**: "bel", "bel naar", "telefoneer met" of "kies", en een contactnaam of telefoonnummer, of "bel", de naam en "op". SandFly zegt de naam, gevolgd door "bellen", en start het gesprek. De eerste keer vraagt SandFly om toestemming om te bellen; zeg de opdracht opnieuw nadat je dit hebt toegestaan. Zonder deze toestemming opent de telefoon-app met het nummer.
- **Een bericht versturen**: zoals "bericht Sam ik ben onderweg", "sms Sam ik ben onderweg", "stuur een bericht naar Sam via WhatsApp met de tekst hallo" of "zeg tegen Sam via Signal hallo". Zonder de tekst zegt SandFly "Bericht aan" en de naam, en legt de tekst vast.

Voor contactnamen heeft SandFly toestemming nodig om contacten te lezen, en vraagt daar de eerste keer om. Als meerdere contacten die naam hebben, zegt SandFly "Meer dan één contact met de naam" en hun volledige namen; zeg de volledige naam. Als geen contact die naam heeft, zegt SandFly "Geen contact met de naam" en de naam.

Berichten gaan naar de standaard sms-app, tenzij je een app noemt. Met sms of WhatsApp opent SandFly het gesprek met de tekst, zegt "Versturen naar", de naam en de tekst, en legt het antwoord vast. Zeg "ja", "verstuur", "ok" of "prima", en SandFly drukt op **Verzenden** en zegt "Verzenden ingedrukt". Elk ander antwoord, of geen antwoord, laat het bericht onverstuurd in de app, en SandFly zegt "Bericht niet verstuurd". Andere apps openen hun deelscherm met de tekst, waar je het gesprek kiest.

#### Beschrijvingen {#voice-commands-describe}

- **Het scherm beschrijven**: "beschrijf het scherm" of "wat staat er op het scherm".
- **De gefocuste afbeelding beschrijven**: "beschrijf deze afbeelding", "beschrijf de foto" of "wat staat er op deze foto".
- **Kopiëren**: "kopieer het", "kopieer dat" of "kopieer de beschrijving" kopieert de beschrijving van de vorige opdracht, of anders de laatste zin die SandFly heeft gezegd.
- **Verzenden**: "stuur het naar Sam", "deel het met Sam", of "stuur het naar Sam via WhatsApp" verstuurt de beschrijving van de vorige opdracht als bericht, of anders de laatste zin die SandFly heeft uitgesproken. "Stuur de afbeeldingsbeschrijving naar Sam" en "stuur de schermbeschrijving naar Sam" beschrijven en versturen in één opdracht; SandFly zegt "Versturen naar" en de naam voordat het op **Verzenden** drukt.

Deze opdrachten gebruiken de AI-diensten of beschrijvingen op het apparaat die **Afbeeldings- en schermbeschrijvingen** en **AI-diensten** instellen. Zie [](#descriptions).

### Opdrachten koppelen {#voice-commands-chains}

Een spraakopdracht kan meerdere opdrachten bevatten, verbonden door "dan", "en dan", "daarna" of "vervolgens", zoals "open Berichten dan bel Sam". "En" verbindt ook opdrachten, als elk deel een opdracht is: "ga naar home en open Chrome" is twee opdrachten, terwijl "tik op Peper en zout" er één is. SandFly voert de opdrachten na elkaar uit, met een korte pauze na elke opdracht die het scherm verandert.

Als een opdracht mislukt, zoals een item dat niet wordt gevonden, zegt SandFly waarom en stopt het. Een deel dat geen opdracht is, wordt gelezen als een van de oorspronkelijke spraakopdrachten; als het dat ook niet is, zegt SandFly "Niet begrepen" en het deel.

Om een spraakopdracht te stoppen terwijl die wordt uitgevoerd, start je een nieuwe spraakopdracht en zeg je "stop", "annuleer", "laat maar" of "hou op". Het starten van een nieuwe spraakopdracht stopt ook de opdracht die wordt uitgevoerd.

### Spraakbewerking {#voice-commands-editing}

**Spraakbewerking starten**, in de categorie **Tekstverwerking**, neemt spraakopdrachten voor het gefocuste tekstveld na elkaar op, zonder een gebaar voor elke opdracht. Er is standaard geen gebaar voor. Zie [](#commands-list).

1. Verplaats de focus naar een tekstveld.
2. Voer **Spraakbewerking starten** uit. SandFly zegt "Spraakbewerking aan", en een geluidssignaal speelt als de opname begint.
3. Zeg een opdracht. SandFly voert de opdracht uit, spreekt het resultaat uit, en neemt opnieuw op zodra de spraak eindigt. Het geluidssignaal speelt opnieuw als de opname na een opdracht begint.

Spraakbewerking voert alleen deze opdrachten uit:

- De opdrachten van de categorieën **Tekstverwerking** en **Navigeren op tekst**, op naam, zoals "Alles selecteren", "Kopiëren", "Plakken", "Tekst wissen", "Cursor naar einde verplaatsen", "Volgend woord" of "Vorig teken". **Dicteren** en **Spraakbewerking starten** worden niet uitgevoerd. Zie [](#typing-editing-commands).
- "Typ" of "schrijf" en een tekst, of "voer" of "tik", een tekst en "in", die SandFly bij de cursor typt en uitspreekt.
- "Nieuwe regel", "nieuwe alinea" of "druk op enter", die **Nieuwe regel** uitvoeren.
- De zinnen voor tekst in een tekstveld, zoals "verwijder volgende vijf woorden", "einde van regel" of "selecteer krokodil". Hier werkt "selecteer" en een tekst altijd op de tekst van het veld. Zie [](#voice-commands-text).
- De tekstbewerkingszinnen van de oorspronkelijke spraakopdrachten, zoals "Selecteren", "Selecteren voltooien", "Verwijderen", "Knippen" of "Spel TEKST". Hier moeten ze overeenkomen met de hele opdracht. Zie [](#appendix-voice-commands-text-editing).
- Aangepaste spraakopdrachten die een van deze opdrachten of een macro uitvoeren. Zie [](#voice-commands-custom).
- Ketens van deze opdrachten, zoals "alles selecteren dan knippen". Zie [](#voice-commands-chains).

Iets anders wordt niet uitgevoerd: SandFly zegt "Niet begrepen" en wat je hebt gezegd, en neemt opnieuw op. Tekst die geen opdracht is, wordt niet getypt; zeg er "typ" voor.

Spraakbewerking eindigt, en SandFly zegt "Spraakbewerking uit", als:

- Je **Spraakbewerking starten** opnieuw uitvoert.
- Je "stop bewerken", "stop spraakbewerking", "bewerken beëindigen", "spraakbewerking beëindigen", "spraakbewerking uit", "spraakbewerking starten", "stop", "stoppen", "annuleer", "annuleren", "laat maar" of "hou op" zegt.
- De focus het tekstveld verlaat.
- **Spraakopdrachten** > **Tijdslimiet spraakbewerking** is verstreken sinds spraakbewerking begon: 5, 10, 20 of 30 minuten, standaard 10.
- Spraakherkenning drie keer achter elkaar mislukt, of de microfoontoestemming ontbreekt.

Spraak is stil terwijl SandFly opneemt, zodat de spraak niet wordt opgenomen. Als er niets wordt gezegd, begint de opname opnieuw zonder geluidssignaal. Spraakbewerking neemt op met de spraakherkenning die is gekozen in **Typen** > **Dicteren**, zonder AI-correctie. **Dicteren**, **Dicteren, media afspelen of pauzeren** en **Spraakopdracht starten** beëindigen spraakbewerking zonder "Spraakbewerking uit".

### Spraakopdrachten begrijpen met AI {#voice-commands-ai}

**Spraakopdrachten** > **Spraakopdrachten begrijpen met AI** stuurt een spraakopdracht die SandFly niet begrijpt, of een keten die niet volledig wordt begrepen, naar de AI-service die in gebruik is, die deze omzet in opdrachten. SandFly stuurt wat je hebt gezegd, de namen van de opdrachten en de teksten van de items op het scherm. Standaard uit.

De schakelaar vereist een AI-service die is ingesteld in **AI-diensten**: AI op het apparaat, SandFly Pro, of een andere service met een sleutel. Zie [](#descriptions) en [](#pro). Het antwoord van de AI kan opdrachten uitvoeren, items activeren, apps openen, tekst typen, wachten, bellen, berichten versturen, het scherm of een afbeelding beschrijven, kopiëren, of een kort antwoord uitspreken.

### Aangepaste spraakopdrachten {#voice-commands-custom}

Een aangepaste spraakopdracht is een zin die een opdracht uitvoert. Aangepaste spraakopdrachten gaan voor elke andere spraakopdracht.

1. Kies **Spraakopdrachten** in Instellingen voor SandFly.
2. Kies **Spraakopdracht toevoegen** onder **Aangepaste spraakopdrachten**.
3. Voer de zin in, tot 200 tekens.
4. Kies een categorie, en dan de opdracht.

Kies een aangepaste spraakopdracht voor **Zin wijzigen**, **Commando wijzigen** en **Verwijderen**. Twee aangepaste spraakopdrachten kunnen niet dezelfde zin hebben.

**Exporteren naar bestand** slaat de aangepaste spraakopdrachten op in een bestand, en **Importeren uit bestand** voegt ze toe vanuit een bestand. Zie [](#api-item-files).

Een aangepaste spraakopdracht kan elke opdracht uitvoeren, inclusief een macro of een aangepast menu. Om meerdere stappen vanuit één zin uit te voeren, maak je een macro en geef je de macro een zin. Zie [](#macros).

### Talen {#voice-commands-languages}

Spraakopdrachten gebruiken de taal van het apparaat. De zinnen van de oorspronkelijke spraakopdrachten, de zinspatronen en de verbindingswoorden worden met SandFly vertaald, en opdrachtnamen worden gelezen in de taal waarin SandFly ze toont. De zinnen in [](#appendix-voice-commands) zijn de Engelse.
