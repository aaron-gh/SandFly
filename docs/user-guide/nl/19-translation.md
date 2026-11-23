## Vertaling {#translation}

SandFly kan vertalen wat het laatst heeft gezegd, de tekst van een tekstveld, en, met automatische vertaling, alle spraak en meldingen in andere talen. Vertalingen worden gemaakt op de telefoon of door een vertaalservice of AI-service die je kiest. Instellingen staan bij **Vertaling**.

### Vertaal de laatst uitgesproken zin {#translation-last-phrase}

**Laatst uitgesproken woordgroep vertalen** vertaalt wat SandFly het laatst zei en leest de vertaling voor. Met de focus in een tekstveld vertaalt dit in plaats daarvan de tekst van het veld, behalve in wachtwoordvelden.

De opdracht staat in de categorie **Lezen** in **Commando's** en heeft standaard geen gebaar. Wijs hem daar toe aan een gebaar of een toets. Zie [](#commands).

1. Verplaats de focus naar een item, of laat SandFly iets voorlezen.
2. Voer **Laatst uitgesproken woordgroep vertalen** uit.

SandFly leest de vertaling voor. Als de tekst al in de doeltaal is, zegt SandFly "Al in" en de taal. Als er niets te vertalen is, zegt SandFly "Niets te vertalen".

Als je de opdracht direct daarna opnieuw uitvoert, vertaalt SandFly opnieuw dezelfde oorspronkelijke tekst, niet de vertaling, zodat een taal die je ondertussen hebt gewijzigd, van toepassing is. Een vertaling blijft bewaard tot de tekst of de instellingen veranderen, zodat het opnieuw uitvoeren van de opdracht de tekst niet opnieuw naar de service stuurt.

De vertaling wordt toegevoegd aan de spraakgeschiedenis, zodat de opdrachten die de laatst uitgesproken zin herhalen, spellen en kopiëren, erop werken. Tekst langer dan 5000 tekens wordt afgekapt.

### Kies de vertaalservice {#translation-service}

1. Ga naar **Vertaling** > **Vertaalservice**.
2. Kies een service.

De services zijn:

- **Op het apparaat**: vertaalt op de telefoon met gedownloade talen. De standaardwaarde op telefoons. Niet op horloges. Zie [](#translation-on-device).
- **AI-service**: het model voor AI op het apparaat als **AI op het apparaat gebruiken** aan staat, anders de AI-service in gebruik bij **AI-diensten**, zoals SandFly Pro, Gemini of Claude. De standaardwaarde op horloges. Zie [](#descriptions-ai-services).
- **Google Cloud Translation**, **DeepL** en **Microsoft Translator**: vereisen je eigen API-sleutel van de service.
- **LibreTranslate**: vereist het adres van een LibreTranslate-server, zoals `https://libretranslate.com` of je eigen server, en een API-sleutel als de server die nodig heeft.

Als je een service kiest die een API-sleutel of een adres vereist, vraagt SandFly er direct om. De instellingen van de service staan dan in een eigen categorie op het scherm **Vertaling**, zoals **Instellingen voor DeepL**:

- Het item voor de API-sleutel, zoals **DeepL API-sleutel**: plak de sleutel en kies **Opslaan**. Om hem te verwijderen, maak het veld leeg en kies **Opslaan**.
- **Adres**, voor LibreTranslate: het adres van de server. Dit moet beginnen met `https://`.
- **Regio**, voor Microsoft Translator: de Azure-regio van je Translator-resource, zoals `westeurope`. Laat dit leeg voor een globale resource (**Globaal**).

Een service die nog niet gereed is, toont "niet ingesteld" na zijn naam. Tekst die je vertaalt, wordt naar de service gestuurd, behalve met **Op het apparaat**, of met **AI-service** terwijl AI op het apparaat aan staat. API-sleutels blijven op de telefoon: geëxporteerde instellingen bevatten ze niet.

### Kies de talen {#translation-languages}

- **Vertalen vanuit**: **Taal detecteren** (de standaardwaarde) of een taal.
- **Vertalen naar**: **Apparaattaal** (de standaardwaarde) of een taal.
- **Tekst in de doeltaal vertalen naar**: een tweede taal voor tekst die al in de doeltaal is, of **Niet vertalen** (de standaardwaarde). Bijvoorbeeld, een Engelstalige die Duits en Engels leest, kan hier Duits kiezen. Alleen getoond terwijl **Vertalen vanuit** **Taal detecteren** is.
- **Talen omwisselen**: wisselt de talen van **Vertalen vanuit** en **Vertalen naar** om, en SandFly zegt het nieuwe paar. Alleen getoond terwijl **Vertalen vanuit** een taal is.

Met **Op het apparaat** tonen de lijsten de talen die vertaling op het apparaat ondersteunt. Taaldetectie werkt op de telefoon. De eerste keer zegt SandFly mogelijk "Taaldetectie wordt gedownload. Probeer het zo opnieuw."

### Talen op het apparaat {#translation-on-device}

Met **Op het apparaat** als de vertaalservice of de service voor automatische vertaling heeft de categorie **Instellingen voor Op het apparaat** deze items:

- **Talen op het apparaat**: toont de talen en welke zijn gedownload. Kies een taal die niet is gedownload om hem te downloaden, ongeveer 30 MB, of een gedownloade taal om hem te verwijderen.
- **Alle talen downloaden**: downloadt elke taal die niet is gedownload, één voor één, na een dialoogvenster met hun aantal en grootte. Terwijl ze downloaden, toont het item hoeveel er klaar zijn, en SandFly kondigt elk kwart aan terwijl het scherm **Vertaling** open is. De downloads gaan door nadat je het scherm verlaat. Kies het item opnieuw om ze te stoppen.
- **Talen alleen via Wi-Fi downloaden** (standaard uit): talen downloaden alleen via Wi-Fi.

Een taal die nodig is en niet is gedownload, wordt de eerste keer dat hij nodig is gedownload, samen met Engels, waarmee andere taalparen worden vertaald. SandFly zegt welke talen worden gedownload.

### Automatische vertaling {#translation-automatic}

De categorie **Automatische vertaling** heeft deze instellingen:

- **Automatische vertaling** (standaard uit): spraak in een andere taal dan **Vertalen naar** wordt vertaald voordat SandFly die voorleest, meldingen inbegrepen.
- **Meldingen vertalen** (standaard uit): vertaalt alleen meldingen, terwijl **Automatische vertaling** uit staat.
- **Service voor automatische vertaling**: **Op het apparaat**, **Google Cloud Translation**, **DeepL**, **Microsoft Translator** of **LibreTranslate**. **AI-service** wordt niet aangeboden. De standaardwaarde is de vertaalservice, of **Op het apparaat** als de vertaalservice **AI-service** is.
- **Niet-vertaalde talen**: talen die SandFly voorleest zoals ze zijn, zoals talen die je begrijpt. Standaard geen.

De laatste twee instellingen zijn beschikbaar terwijl **Automatische vertaling** of **Meldingen vertalen** aan staat.

Om automatische vertaling aan en uit te zetten zonder instellingen te openen, gebruik de leesinstelling **Automatische vertaling**. Die staat standaard uit; zet hem aan bij **Leesinstellingen**. Zie [](#commands).

#### Hoe automatische vertaling werkt {#translation-automatic-details}

SandFly herkent de taal van elke uitspraak op de telefoon. Tekst die in de doeltaal is, in een van **Niet-vertaalde talen**, of, als **Vertalen vanuit** een taal is, in een andere taal, wordt voorgelezen zoals hij is en niet naar een service gestuurd. Tekst met minder dan drie letters wordt niet gecontroleerd.

De geluiden en trillingen van een uitspraak spelen meteen, en de woorden volgen als de vertaling klaar is. Een vertaling die langer duurt dan 3 seconden, of mislukt, wordt vervangen door de oorspronkelijke tekst. SandFly bewaart recente vertalingen, zodat teruggaan naar een item het niet opnieuw vertaalt.

Met **Op het apparaat** begint een taal die niet is gedownload, te downloaden zodra SandFly er zeker van is. Terwijl die downloadt, wordt spraak onvertaald voorgelezen, en als hij is gedownload, leest SandFly het item waarop je staat opnieuw voor, vertaald, tenzij je bent doorgegaan. Een melding wacht tot 30 seconden op het downloaden.

Meldingen worden vertaald voordat ze in de wachtrij komen, zodat ze hun plaats behouden. Een melding die met een spraakprofiel wordt voorgelezen, houdt de stem van dat profiel. Privémeldingen worden niet vertaald, zodat hun tekst nooit naar een service wordt gestuurd. Zie [](#notifications).

### Hoe vertalingen worden voorgelezen {#translation-speech}

De categorie **Spraak** heeft deze instellingen:

- **Stem voor de doeltaal gebruiken** (standaard aan): de vertaling wordt gemarkeerd met zijn taal, zodat SandFly de stem wijzigt zoals voor andere tekst in die taal, volgens de taalinstellingen bij **Spraak**. Zie [](#speech).
- **Oorspronkelijke taal uitspreken** (standaard uit): SandFly zegt de taal van de oorspronkelijke tekst voor de vertaling, zoals "Uit het Duits:".
- **Vertalingen naar het klembord kopiëren** (standaard uit): elke vertaling wordt gekopieerd naar het klembord.

### Foutmeldingen bij vertaling {#translation-errors}

Als een vertaling mislukt, zegt SandFly waarom, bijvoorbeeld:

- De API-sleutel of het adres van LibreTranslate ontbreekt: "Voer de API-sleutel van DeepL in bij Vertaling."
- De service accepteerde de API-sleutel niet, of zijn gebruikslimiet is bereikt.
- De service is niet bereikbaar.
- De service vertaalt de taal niet.
- "Taal niet gedetecteerd".
- Talen voor vertaling op het apparaat worden nog gedownload, of konden niet worden gedownload.

Voor andere mislukkingen zegt SandFly dat de service de tekst niet kon vertalen, met het bericht van de service.
