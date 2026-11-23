## Spraakopdrachten {#voice-commands}

Spraakopdrachten voeren SandFly-opdrachten uit via spraak: de focus bewegen, voorlezen, tekst bewerken, apps openen, items op het scherm activeren, een contact bellen, een bericht versturen, of het scherm beschrijven. Een spraakopdracht kan meerdere opdrachten achter elkaar bevatten. **Spraakopdrachten** in Instellingen voor SandFly zet AI aan voor opdrachten die SandFly niet begrijpt, en bevat de spraakopdrachten die je zelf maakt.

Zie [](#appendix-voice-commands) voor de volledige lijst met zinnen.

### Een spraakopdracht starten {#voice-commands-start}

1. Gebruik de opdracht **Spraakopdracht starten**. In de SandFly-gebarenindeling swipe je naar rechts en dan omhoog. Zie [](#appendix-gestures).
2. Zeg de opdracht.

De eerste keer toont SandFly **Gebruik spraakopdrachten om SandFly te bedienen** met voorbeelden van wat je kunt zeggen. Kies **Spraakopdrachten gebruiken** om de opname te starten. Schakel **Dit bericht altijd tonen** uit om te voorkomen dat het dialoogvenster weer wordt getoond.

Spraakopdrachten vereisen de microfoontoestemming. SandFly neemt op met de spraakherkenning die is gekozen in **Typen** > **Dicteren**. Zie [](#typing).

Spraakopdrachten zijn niet beschikbaar terwijl het scherm is vergrendeld of tijdens het instellen van het apparaat. Terwijl het scherm is vergrendeld, zegt SandFly dit en noemt het gebaar dat het SandFly-menu opent.

Om de opdrachten te tonen, zeg je "Help" of "Wat kan ik zeggen?". SandFly opent **Overzicht van spraakopdrachten**.

De opdracht **Spraakopdracht starten** staat ook in de categorie **Systeemacties**, zodat een sneltoets of een menu-item deze kan uitvoeren. Zie [](#commands-edit-menu) om deze aan het SandFly-menu toe te voegen.

### Wat je kunt zeggen {#voice-commands-phrases}

SandFly leest wat je zegt in deze volgorde:

1. Een aangepaste spraakopdracht, op zijn zin. Zie [](#voice-commands-custom).
2. De naam van elke opdracht, zoals SandFly die toont op het scherm **Commando's**, zoals "Lezen vanaf het begin", "Afbeelding beschrijven" of "Volgende kop". De namen van je macro's, aangepaste menu's en scriptopdrachten tellen ook. Zie [](#commands-list).
3. Een zinspatroon, zoals "open Chrome", "tap Settings", "call Sam" of "type hello". Zie [](#voice-commands-sandfly).
4. Een van de oorspronkelijke spraakopdrachten, zoals "Find save", "Words navigation", "Speak faster" of "Copy". Zie [](#appendix-voice-commands).
5. Met **Spraakopdrachten begrijpen met AI** aan, de AI-service. Zie [](#voice-commands-ai).

Hoofdletters en de interpunctie die de spraakherkenning toevoegt, maken niet uit. Woorden zoals "please", "can you", "could you", "I want to" en "now" aan het begin, en "please", "for me" en "now" aan het einde, worden weggelaten.

Als SandFly een opdracht niet begrijpt, zegt het "Deze opdracht wordt niet herkend" en hoe je de lijst met opdrachten kunt krijgen.

#### Apps, items en tekst {#voice-commands-sandfly}

- **Een app openen**: "open", "launch", "start" of "switch to", en de naam van de app. SandFly kiest de app met die hele naam, anders een app waarvan de naam ermee begint, anders een app met een woord ervan in de naam. Als meerdere apps evenveel overeenkomen, zegt SandFly "Geen app met de naam" en de naam.
- **Een item op het scherm activeren**: "tap", "double tap", "click", "press", "activate" of "select", en de tekst van het item. SandFly activeert het eerste item waarvan de hele tekst die tekst is, anders het eerste item waarvan de tekst die bevat. Als er geen is, zegt SandFly "Niet gevonden" en de tekst.
- **Tekst typen**: "type", "write" of "enter", en de tekst. SandFly typt die in het bewerkingsveld met focus.
- **Een opdracht uitvoeren**: "run" of "do" en de naam van een opdracht, of "run the" en de naam van een macro en "macro", zoals "run the morning macro".
- **Wachten**: "wait", "wait a second", of "wait" en een aantal seconden, tot 60. Gebruik dit tussen opdrachten in een keten.

#### Bellen en berichten {#voice-commands-calls}

- **Bellen**: "call", "phone", "ring" of "dial", en een contactnaam of telefoonnummer. SandFly zegt de naam, gevolgd door "bellen", en start het gesprek. De eerste keer vraagt SandFly om toestemming om te bellen; zeg de opdracht opnieuw nadat je dit hebt toegestaan. Zonder deze toestemming opent de telefoon-app met het nummer.
- **Een bericht versturen**: zoals "message Sam saying I'm on my way", "text Sam I'm on my way", "send a message to Sam on WhatsApp saying hello" of "tell Sam on Signal hello". Zonder de tekst zegt SandFly "Bericht aan" en de naam, en legt de tekst vast.

Voor contactnamen heeft SandFly toestemming nodig om contacten te lezen, en vraagt daar de eerste keer om. Als meerdere contacten die naam hebben, zegt SandFly "Meer dan één contact met de naam" en hun volledige namen; zeg de volledige naam.

Berichten gaan naar de standaard sms-app, tenzij je een app noemt. Met sms of WhatsApp opent SandFly het gesprek met de tekst, zegt "Versturen naar", de naam en de tekst, en legt het antwoord vast. Zeg "yes", "send", "OK" of "sure", en SandFly drukt op **Verzenden** en zegt "Verzenden ingedrukt". Elk ander antwoord, of geen antwoord, laat het bericht onverstuurd in de app, en SandFly zegt "Bericht niet verstuurd". Andere apps openen hun deelscherm met de tekst, waar je het gesprek kiest.

#### Beschrijvingen {#voice-commands-describe}

- **Het scherm beschrijven**: "describe the screen" of "what's on the screen".
- **De gefocuste afbeelding beschrijven**: "describe this image", "describe the photo" of "what's in this picture".
- **Kopiëren**: "copy it", "copy that" of "copy the description" kopieert de beschrijving van de vorige opdracht, of anders de laatste zin die SandFly heeft gezegd.
- **Verzenden**: "send it to Sam", "share it with Sam", of "send it to Sam on WhatsApp" verstuurt de beschrijving van de vorige opdracht als bericht. "Send the image description to Sam" en "send the screen description to Sam" beschrijven en versturen in één opdracht; SandFly vraagt "Versturen naar" en de naam voordat het op **Verzenden** drukt.

Deze opdrachten gebruiken de AI-diensten of beschrijvingen op het apparaat die **Afbeeldings- en schermbeschrijvingen** en **AI-diensten** instellen. Zie [](#descriptions).

### Opdrachten koppelen {#voice-commands-chains}

Een spraakopdracht kan meerdere opdrachten bevatten, verbonden door "then", "and then", "after that" of "afterwards", zoals "open Messages then call Sam". "And" verbindt ook opdrachten, als elk deel een opdracht is: "go home and open Chrome" is twee opdrachten, terwijl "tap Salt and pepper" er één is. SandFly voert de opdrachten na elkaar uit, met een korte pauze na elke opdracht die het scherm verandert.

Als een opdracht mislukt, zoals een item dat niet wordt gevonden, zegt SandFly waarom en stopt het. Een deel dat geen opdracht is, wordt gelezen als een van de oorspronkelijke spraakopdrachten; als het dat ook niet is, zegt SandFly "Niet begrepen" en het deel.

Om een spraakopdracht te stoppen terwijl die wordt uitgevoerd, start je een nieuwe spraakopdracht en zeg je "stop", "cancel", "quit" of "never mind". Het starten van een nieuwe spraakopdracht stopt ook de opdracht die wordt uitgevoerd.

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
