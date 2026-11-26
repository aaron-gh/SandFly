## Afbeeldings- en schermbeschrijvingen {#descriptions}

SandFly kan de gefocuste afbeelding, het hele scherm en het beeld van de camera aan de achterkant beschrijven, en vragen daarover beantwoorden. Beschrijvingen komen van een van deze:

- Een AI-service, zoals Gemini, Claude of OpenAI, met je eigen API-sleutel. Zie [](#descriptions-ai-services).
- SandFly Pro, zonder API-sleutel. Zie [](#pro).
- Een model dat op de telefoon werkt, zonder dat er iets over het netwerk wordt verstuurd. Zie [](#descriptions-on-device).

Tekst in afbeeldingen wordt op de telefoon herkend en heeft niets van dit alles nodig.

Instellingen voor beschrijvingen staan op twee schermen: **Afbeeldings- en schermbeschrijvingen** zet beschrijvingen aan en opent Live herkenning; **AI-diensten** kiest wat de beschrijvingen maakt. Beschrijvingen vereisen een telefoon of tablet: ze zijn niet beschikbaar op horloges en tv's.

### Stel afbeeldings- en schermbeschrijvingen in {#descriptions-set-up}

1. Stel een AI-service, SandFly Pro of AI op het apparaat in. Zie [](#descriptions-set-up-service), [](#pro) of [](#descriptions-on-device-download).
2. Ga naar **Afbeeldings- en schermbeschrijvingen** en zet **Gedetailleerde afbeeldingsbeschrijvingen** aan.
3. Lees het dialoogvenster over wat wordt verstuurd en naar wie, en kies dan **Aanzetten**.

Als je **Afbeelding beschrijven** uitvoert terwijl **Gedetailleerde afbeeldingsbeschrijvingen** uit staat, toont SandFly het dialoogvenster **Gedetailleerde afbeeldingsbeschrijvingen ophalen?**. **Aanzetten** zet de instelling aan en beschrijft de afbeelding; **Nee, bedankt** laat hem uit staan, en SandFly leest alleen de tekst in de afbeelding voor.

De eerste keer dat je **Scherm beschrijven** uitvoert, toont SandFly het dialoogvenster **Schermbeschrijving** over de schermafbeelding die wordt verstuurd. Kies **Aanzetten** om het scherm te beschrijven.

### Instellingen voor afbeeldings- en schermbeschrijvingen {#descriptions-settings}

**Afbeeldings- en schermbeschrijvingen** heeft deze instellingen:

- **Gedetailleerde afbeeldingsbeschrijvingen** (standaard uit): **Afbeelding beschrijven** stuurt de gefocuste afbeelding naar de AI-service in gebruik, of naar het model op het apparaat, voor een volledige beschrijving.
- **Automatische beschrijvingen voor tekst in afbeeldingen**: SandFly leest de tekst in een afbeelding voor als de focus ernaartoe gaat. Kies **Alle afbeeldingen**, **Alleen niet-gelabelde afbeeldingen** (de standaardwaarde) of **Afbeeldingen niet beschrijven**. Tekst wordt op de telefoon herkend.
- **Herkenning van Cyrillische tekst**: downloadt en verwijdert de tekstherkenningsmodellen voor talen die in het Cyrillisch worden geschreven, zoals Russisch, Oekraïens en Servisch. Zie [](#descriptions-cyrillic-text). Niet op horloges.
- **Live herkenning**: opent het scherm Live herkenning. Zie [](#descriptions-live-recognition). Alleen op apparaten met een camera.

### Herkenning van Cyrillische tekst {#descriptions-cyrillic-text}

Tekstherkenning leest Cyrillische tekst met een model voor de taal, dat je eerst downloadt. SandFly gebruikt het model van de spraaktaal, of van de eerste taal van de telefoon, die in het Cyrillisch wordt geschreven. Zonder het model worden alleen Latijnse letters gelezen, en zegt SandFly één keer dat het model niet is gedownload.

1. Ga naar **Afbeeldings- en schermbeschrijvingen** > **Herkenning van Cyrillische tekst**.
2. Kies de taal. Het model van de taal, een paar megabyte, wordt gedownload, de eerste keer samen met het model voor Latijnse letters, zodat tekst in beide wordt gelezen. Bij mobiele data opent eerst een bevestigingsvenster.

Om een download te stoppen, kies je de taal opnieuw. Om een model te verwijderen, kies je een gedownloade taal en dan **Verwijderen**. Het model voor Latijnse letters wordt met de laatste taal verwijderd.

De modellen komen van het Tesseract OCR-project en worden op de telefoon door Tesseract gelezen. De herkenning is langzamer dan voor andere schriften.

### Beschrijf een afbeelding {#descriptions-describe-image}

1. Verplaats de focus naar een afbeelding.
2. Voer **Afbeelding beschrijven** uit. Dit staat in het SandFly-menu, en je kunt het toewijzen aan een gebaar of een toets. Zie [](#descriptions-commands).

SandFly opent het venster **Beschrijving van de afbeelding** met de beschrijving, en de tekst die in de afbeelding is gevonden. In het venster kun je de beschrijving kopiëren, beoordelen en vragen stellen over de afbeelding. Zie [](#descriptions-questions).

Als er geen gedetailleerde beschrijving kan worden gemaakt, leest SandFly de tekst in de afbeelding voor en zegt waarom, bijvoorbeeld "Kan geen gedetailleerdere afbeeldingsbeschrijving krijgen" met de reden, of "Maak verbinding met internet voor een gedetailleerdere afbeeldingsbeschrijving."

Om de beschrijving te laten voorlezen zonder het venster, voer in plaats daarvan **Afbeeldingsbeschrijving uitspreken** uit.

### Beschrijf het scherm {#descriptions-describe-screen}

Voer **Scherm beschrijven** uit. SandFly maakt een schermafbeelding, stuurt die met de items op het scherm, en opent het venster **Schermbeschrijving** met een beschrijving van het scherm. Je kunt vragen stellen over het scherm in het venster. Zie [](#descriptions-questions).

Om de beschrijving te laten voorlezen zonder het venster, voer in plaats daarvan **Schermbeschrijving uitspreken** uit.

**Scherm beschrijven** is alleen beschikbaar in telefoontalen die het ondersteunt. Schermafbeeldingen van beveiligde vensters, zoals sommige bank-apps, kunnen niet worden gemaakt.

### Stel vragen over een afbeelding of het scherm {#descriptions-questions}

Vragen gaan naar dezelfde AI-service of het model op het apparaat als de beschrijving.

In het venster **Beschrijving van de afbeelding** of **Schermbeschrijving**:

- **Vragen met spraak** neemt een vraag op met de spraakherkenner die is gekozen bij **Typen** > **Dicteren**, stuurt die en leest het antwoord voor. **Stop met luisteren** beëindigt de opname. Zie [](#typing).
- **Typ een vraag** opent een tekstveld. Typ de vraag en kies **Vragen**.

Elk antwoord blijft in het venster staan, zodat je vervolgvragen kunt stellen over dezelfde afbeelding of hetzelfde scherm.

Om te vragen zonder eerst een beschrijving te openen, voer **Vragen over afbeelding** of **Vragen over scherm** uit:

1. Verplaats de focus naar een afbeelding, voor **Vragen over afbeelding**.
2. Voer de opdracht uit. SandFly maakt een schermafbeelding van de afbeelding of het scherm en begint met opnemen.
3. Zeg de vraag. De opname stopt na een stilte, of als je de opdracht opnieuw uitvoert.

SandFly stuurt de vraag en leest het antwoord voor. Als de opname niet kan starten, opent in plaats daarvan een tekstveld: typ de vraag en kies **Vragen**. Tot je ermee hebt ingestemd om schermafbeeldingen naar de AI-service te sturen, voeren deze opdrachten in plaats daarvan **Afbeelding beschrijven** of **Scherm beschrijven** uit, die het dialoogvenster voor die toestemming tonen.

### Opdrachten voor beschrijvingen {#descriptions-commands}

Deze opdrachten staan in de categorie **Afbeeldings- en schermbeschrijvingen** in **Commando's**, waar je ze kunt toewijzen aan gebaren en toetsen en toevoegen aan het SandFly-menu. Zie [](#commands).

- **Foto maken en beschrijven**: maakt een foto met de camera aan de achterkant en beschrijft die. Zie [](#descriptions-take-picture).
- **Afbeelding beschrijven**, **Afbeeldingsbeschrijving uitspreken** en **Vragen over afbeelding**: beschrijven de gefocuste afbeelding in een venster, beschrijven haar voorlezend, of beantwoorden een gesproken vraag erover.
- **Scherm beschrijven**, **Schermbeschrijving uitspreken** en **Vragen over scherm**: hetzelfde voor het hele scherm.
- **Afbeelding van gefocust item delen** en **Screenshot delen**: maken een schermafbeelding van het gefocuste item of het hele scherm en openen daarmee het deelvenster van Android, om deze naar een andere app te sturen. Vereist Android 11 of hoger. Beveiligde vensters zijn zwart op de schermafbeelding.
- **AI-antwoord melden**: opent een probleemrapport met de laatste tekst die een AI-model gaf, zoals een beschrijving of een antwoord. Zie [](#help).

Met SandFly Pro heeft de categorie ook de opdrachten voor schermherkenning. Zie [](#pro-screen-recognition).

De categorie **Live herkenning** heeft een opdracht voor elke modus van Live herkenning, zoals **Live herkenning: Scènes**, en **Live herkenning stoppen**. Zie [](#descriptions-live-recognition).

Voor standaardgebaren en sneltoetsen, zie [](#appendix-gestures) en [](#appendix-keyboard-shortcuts).

#### Leesinstelling AI-acties {#descriptions-ai-actions}

De leesinstelling **AI-acties** voert **Scherm beschrijven**, **Schermbeschrijving uitspreken**, **Vragen over scherm**, **Afbeelding beschrijven**, **Afbeeldingsbeschrijving uitspreken**, **Vragen over afbeelding**, **Afbeelding van gefocust item delen** en **Screenshot delen** uit, en met SandFly Pro ook **AI-agenttaak starten of stoppen** en **AI-agenttaak typen**. Dit vereist Android 11 of hoger.

1. Kies de leesinstelling **AI-acties**.
2. Swipe omhoog of omlaag om een actie te kiezen.
3. Dubbeltik om die uit te voeren.

Dubbeltikken voert de actie uit die de leesinstelling aankondigde totdat de focus verplaatst. **Op te nemen AI-acties**, in **Leesinstellingen**, kiest welke acties de leesinstelling aanbiedt; standaard zijn ze allemaal opgenomen. Acties die niet beschikbaar zijn, worden weggelaten. Voor leesinstellingen, zie [](#commands).

### Maak een foto en beschrijf die {#descriptions-take-picture}

**Foto maken en beschrijven** opent een scherm dat één foto maakt met de camera aan de achterkant en die beschrijft zoals **Afbeelding beschrijven** doet, met hetzelfde venster voor vragen. Dit is alleen beschikbaar op apparaten met een camera.

1. Voer **Foto maken en beschrijven** uit. SandFly zegt "Houd de telefoon stil" en maakt de foto.
2. Lees de beschrijving, en stel zo nodig vragen over de foto.
3. Om een andere te maken, kies **Nieuwe foto maken**.

SandFly vraagt de eerste keer om de cameratoestemming.

### AI-diensten {#descriptions-ai-services}

**AI-diensten**, in de instellingen van SandFly, kiest wat afbeeldingen en schermen beschrijft en vragen daarover beantwoordt. Dezelfde service wordt gebruikt door Live herkenning, door vertaling met de optie **AI-service** en door andere functies die AI gebruiken.

Het scherm heeft deze items:

- **AI-service**: de service in gebruik. Dit toont alleen de services die zijn ingesteld, en **Geen**.
- Een instellingenitem voor elke service: **Instellingen voor SandFly Pro**, **Gemini-instellingen**, **Instellingen voor Claude**, **Instellingen voor OpenAI (ChatGPT)**, **Instellingen voor Mistral**, **Instellingen voor Groq**, **Instellingen voor OpenRouter**, **Instellingen voor Ollama Cloud**, **Instellingen voor Qwen**, **Instellingen voor Zhipu GLM**, **Instellingen voor Baichuan**, **Instellingen voor DeepSeek**, **Instellingen voor Kimi**, **Instellingen voor Hunyuan**, **Instellingen voor SiliconFlow** en **Instellingen voor Andere dienst**. Het item van de service in gebruik toont **In gebruik**; het item van een service die niet is ingesteld, toont **Niet ingesteld**. **Instellingen voor SandFly Pro** zit in builds met SandFly Pro en opent het scherm SandFly Pro. Zie [](#pro).
- **AI op het apparaat**: zie [](#descriptions-on-device).
- **Herkenningsmodellen geladen houden** en **Downloadserver voor modellen**: zie [](#descriptions-model-settings).

#### Stel een service in {#descriptions-set-up-service}

1. Ga naar **AI-diensten** en open de instellingen van een service, zoals **Instellingen voor Claude**.
2. Voor **Andere dienst**, kies **Adres** en voer het adres in van een service met een OpenAI-compatibele chat-API en beeldinvoer. Het adres moet beginnen met `https://`.
3. Kies het item voor de API-sleutel, zoals **Claude API-sleutel**. Kies **Sleutel ophalen** om de website van de service te openen en een sleutel te maken, als de service er een aanbiedt. Plak de sleutel en kies **Opslaan**.
4. Kies het modelitem, zoals **Claude-model**. SandFly haalt de lijst met modellen voor je sleutel op. Kies een model, of kies **Voer een modelnaam in** en typ hem. Als de lijst niet kan worden opgehaald, toont SandFly waarom en opent een veld voor de modelnaam.

Een service is ingesteld zodra ze een adres, een API-sleutel en een model heeft. Als geen andere service in gebruik is, gebruikt SandFly een service zodra die is ingesteld. Om een sleutel te verwijderen, maak het veld leeg en kies **Opslaan**. Als de service in gebruik zijn sleutel of model verliest, gebruikt SandFly de volgende service die is ingesteld, of **Geen**.

API-sleutels blijven op de telefoon: geëxporteerde instellingen bevatten ze niet. Zie [](#profiles).

#### Kies de AI-service {#descriptions-choose-service}

1. Ga naar **AI-diensten** > **AI-service**.
2. Kies een service, of **Geen** om AI-beschrijvingen uit te zetten.

Als geen service is ingesteld, zegt SandFly "Er is geen AI-service ingesteld. Stel er een in via de onderstaande instellingen."

De leesinstelling **AI-service**, standaard uit, wijzigt dezelfde instelling. Swipe omhoog of omlaag om te wisselen tussen de services die zijn ingesteld; SandFly zegt de naam van de service. **Geen** en SandFly Pro zonder licentie staan er niet in. Met minder dan twee ingestelde services zegt SandFly "Maar één AI-service ingesteld" of "Geen AI-service ingesteld" en blijft de instelling ongewijzigd. Voeg de leesinstelling toe bij **Leesinstellingen**; zie [](#commands). De opdrachten **AI-service: volgende waarde** en **AI-service: vorige waarde**, in **Instellingswaarden**, doen hetzelfde zonder de leesinstelling te selecteren.

#### Gemini-instellingen {#descriptions-gemini}

**Gemini-instellingen** heeft twee items:

- **Gemini API-sleutel**: de sleutel van Google AI Studio. **Sleutel ophalen** opent de pagina die er een maakt.
- **Gemini-model**: **Automatisch** (de standaardwaarde) gebruikt Gemini Flash, en Gemini Flash-Lite als Flash bezig is. Je kunt een ander model kiezen uit de lijst die Google voor je sleutel geeft; SandFly vraagt het aan Flash-Lite als dat model bezig is of te lang duurt.

Bij het gratis abonnement van de Gemini API kan Google bewaren wat het ontvangt en gebruiken om zijn producten te verbeteren.

#### Wat wordt verstuurd {#descriptions-privacy}

Afbeeldingen, schermafbeeldingen en vragen die je verstuurt, worden gestuurd naar de service in gebruik. Voordat de eerste afbeelding naar een service gaat, toont SandFly een dialoogvenster met het adres van de service, zoals api.anthropic.com; de eigen voorwaarden en het privacybeleid ervan zijn van toepassing. Met **AI op het apparaat gebruiken** aan wordt niets verstuurd.

### AI op het apparaat {#descriptions-on-device}

AI op het apparaat beschrijft afbeeldingen en schermen met een visueel model op de telefoon, zonder API-sleutel en zonder dat er na het downloaden iets over het netwerk wordt verstuurd. Dit is langzamer dan een AI-service en gebruikt meer batterij. Het vereist een telefoon met een 64-bits ARM-processor en genoeg geheugen voor het model, en is niet beschikbaar op horloges.

Terwijl **AI op het apparaat gebruiken** aan staat, gebruiken **Afbeelding beschrijven**, **Scherm beschrijven**, vragen, Live herkenning en vertaling met de optie **AI-service** het model op de telefoon in plaats van de AI-service. **Gedetailleerde afbeeldingsbeschrijvingen** moet ook aan staan.

#### Download een model {#descriptions-on-device-download}

1. Ga naar **AI-diensten** > **AI op het apparaat**.
2. Kies **Model**, en kies dan een model. De lijst toont de grootte van elk model, het geheugen dat het nodig heeft en een korte toelichting, en markeert modellen die **gedownload** of **experimenteel** zijn. Dit toont alleen de modellen waarvoor de telefoon genoeg geheugen heeft, en de modellen die al zijn gedownload.
3. Kies **Model downloaden**, dat de grootte van de download toont. Op mobiele data vraagt SandFly dit eerst.
4. Als het downloaden klaar is, zet **AI op het apparaat gebruiken** aan.

**Gemma 4 E2B** is het aanbevolen model, en de standaardwaarde op telefoons met genoeg geheugen daarvoor. Op andere telefoons is de standaardwaarde het grootste model dat past. Experimentele modellen werken mogelijk niet goed.

Terwijl het model wordt gedownload, toont het item hoe ver het is en hoeveel tijd er nog over is, en SandFly kondigt elk kwart van het downloaden aan en "Model gedownload" aan het einde. Een melding toont de voortgang. Tik op het item om te stoppen; het downloaden gaat verder waar het stopte als je het opnieuw start. SandFly controleert het gedownloade bestand en verwijdert het als het niet overeenkomt met het model.

Om een bestand te gebruiken dat je zelf hebt gedownload, kies **Gebruik een modelbestand uit de opslag** en kies een Gemma 4 `.litertlm`-bestand. SandFly zegt "Model geïnstalleerd" of dat het bestand geen ondersteund model is.

#### Instellingen voor AI op het apparaat {#descriptions-on-device-settings}

- **AI op het apparaat gebruiken** (standaard uit): beschrijf met het model op de telefoon. Beschikbaar zodra een model is gedownload.
- **Model**: het te gebruiken model.
- **Model downloaden**: downloadt het gekozen model. Getoond totdat het is gedownload.
- **Modellen beheren**: toont gedownloade modellen en onvoltooide downloads, met de opslagruimte die ze gebruiken. Kies een model en **Verwijderen** om zijn opslagruimte vrij te maken.
- **Gebruik een modelbestand uit de opslag**: installeert een modelbestand uit de opslag.
- **Tijdslimiet voor antwoorden**: hoe lang SandFly op een antwoord wacht, van 1 tot 10 minuten. De standaardwaarde is 3 minuten. **Scherm beschrijven** duurt langer dan **Afbeelding beschrijven**.
- **Kortere schermbeschrijvingen** (standaard uit): **Scherm beschrijven** vraagt om minder details, zodat het antwoord eerder komt.
- **Processor**: **CPU**, **GPU** of **NPU**. Alleen getoond op telefoons waar het model op de GPU of de NPU kan werken. Met **NPU** gebruikt SandFly het model dat voor die NPU is gebouwd, en kan **Model** niet worden gewijzigd.

Terwijl het model laadt en werkt, speelt SandFly een herhalend geluid af. Je kunt deze geluiden uitzetten bij **Geluid en trillen** > **Afzonderlijke geluiden en trillingen**. Zie [](#sound).

#### Geheugen {#descriptions-on-device-memory}

Het model laadt bij het eerste verzoek en werkt in zijn eigen proces. Voordat het een model laadt, controleert SandFly of de telefoon genoeg vrij geheugen heeft, en zo niet, zegt het "Niet genoeg vrij geheugen" met de naam van het model. Als Android het model stopt wegens geheugentekort, zegt SandFly dat. In beide gevallen, sluit een paar apps, of kies een kleiner model.

**AI-geheugenlimieten op het apparaat negeren**, in **Ontwikkelaarsinstellingen**, toont elk model, laat je elk gedownload model kiezen en laadt het zonder op vrij geheugen te controleren.

### Instellingen voor modellen {#descriptions-model-settings}

Deze twee instellingen staan in **AI-diensten** en in **Typen** > **Dicteren**, en gelden voor modellen voor AI op het apparaat en voor de spraakherkenningsmodellen van dictee:

- **Herkenningsmodellen geladen houden**: hoe lang modellen in het geheugen blijven na gebruik: **Na gebruik uitladen** (de standaardwaarde), **5 minuten**, **15 minuten**, **30 minuten** of **Totdat SandFly stopt**. Het model voor AI op het apparaat blijft na een verzoek altijd minstens 2 minuten geladen. Met **Totdat SandFly stopt** laadt het model voor AI op het apparaat als SandFly start.
- **Downloadserver voor modellen**: **Hugging Face** (de standaardwaarde) of **hf-mirror.com**, voor plekken waar Hugging Face niet bereikbaar is.

### Live herkenning {#descriptions-live-recognition}

Het scherm **Live herkenning** beschrijft wat de camera aan de achterkant ziet. Open het vanuit **Afbeeldings- en schermbeschrijvingen** > **Live herkenning**, of met de leesinstelling **Live herkenning** of de bijbehorende opdrachten. Dit is niet beschikbaar op apparaten zonder camera, op horloges of op tv's.

Elke modus gaat zelf aan en uit, met een selectievakje op het scherm:

- **Scènes**: beschrijft wat de camera ziet.
- **Tekst**: leest tekst in het camerabeeld voor. Tekst wordt op de telefoon herkend.
- **Aanwijzen en voorlezen**: leest de tekst bij je vingertop in het camerabeeld voor. Kies **Boven de vingertop lezen** (de standaardwaarde) of **Onder de vingertop lezen**.
- **Personen herkennen**: beschrijft personen in het camerabeeld.
- **Deuren herkennen**: beschrijft deuren in het camerabeeld.
- **Kleurherkenning**: noemt de kleuren in het midden van het camerabeeld, zoals "Donkerblauw", "Lichtgrijs" of "Rood en wit", of zegt "Gemengde kleuren" wanneer geen kleur er genoeg van bedekt. Een kleur wordt uitgesproken wanneer deze verandert en ongeveer een halve seconde gelijk blijft. Licht en donker hangen af van het licht en van de automatische belichting van de camera. Kleuren worden op de telefoon herkend.
- **Lichtniveau**: leest het lichtniveau van de lichtsensor voor: **Donker**, **Zwak licht**, **Normaal licht**, **Fel licht** of **Zeer fel licht**. Dit gebruikt de camera niet.
- **Lichtniveautoon**: speelt een doorlopende toon waarvan de toonhoogte stijgt met de helderheid van het beeld van de camera aan de achterkant, van 200 Hz in het donker tot 1600 Hz in zonlicht. De toon speelt op het volume van SandFly-geluiden, en stopt wanneer de modus wordt uitgezet of het scherm wordt verlaten.

De statusregel toont **Camera aan** of **Camera uit**. SandFly vraagt de eerste keer dat een modus dit nodig heeft om de cameratoestemming.

Beschrijvingen kunnen onjuist zijn. Vertrouw er niet op om je veilig te verplaatsen. Personen en deuren worden alleen aan de hand van de afbeelding beschreven, zonder afstanden.

#### Gebruik Live herkenning {#descriptions-live-recognition-use}

1. Kies de leesinstelling **Live herkenning**. Dit vereist Android 11 of hoger.
2. Swipe omhoog of omlaag om een modus te kiezen. SandFly zegt de modus en of die aan staat.
3. Dubbeltik om de modus aan of uit te zetten. Het scherm Live herkenning opent als het nog niet open is.

Terwijl het scherm open is:

- Met twee vingers tikken: stopt spraak en leest het volgende resultaat voor.
- Met twee vingers heen en weer wrijven: zet elke modus uit en sluit het scherm.

Resultaten worden voorgelezen met de stem van SandFly, ook terwijl andere audio speelt. De microfoon wordt niet gebruikt. Het scherm verlaten, de telefoon vergrendelen, SandFly onderbreken of elke modus uitzetten, zet de camera uit.

#### Wat het camerabeeld beschrijft {#descriptions-live-recognition-ai}

**Scènes**, **Aanwijzen en voorlezen**, **Personen herkennen** en **Deuren herkennen** gebruiken hetzelfde als **Afbeelding beschrijven**:

- Met **AI op het apparaat gebruiken** aan, het model op de telefoon. Er verlaat niets de telefoon.
- Anders de AI-service in gebruik, zoals SandFly Pro of Claude, die elke paar seconden een camerabeeld krijgt.
- Met Gemini als AI-service, de Gemini Live API, die elke paar seconden een camerabeeld krijgt. SandFly leest de tekst van het antwoord voor.

Voordat het eerste camerabeeld naar een service gaat, toont SandFly het dialoogvenster **Camerabeelden naar Gemini sturen?**, of voor andere services hetzelfde dialoogvenster met het adres van de service in plaats van Gemini, zoals api.anthropic.com. Kies **Beelden sturen**. Een afgewezen API-sleutel, een opgebruikte gebruikslimiet of herhaalde mislukkingen zetten de modi die AI gebruiken uit, en SandFly zegt dat.

### Foutmeldingen {#descriptions-errors}

Als een beschrijving of antwoord mislukt, zegt SandFly wat er mis ging, bijvoorbeeld:

- De service accepteerde de API-sleutel niet. Controleer de sleutel bij **AI-diensten**.
- De gebruikslimiet van de service of de API-sleutel is bereikt.
- De service is bezig, of deed er te lang over om te antwoorden.
- Er is geen internetverbinding.
- "Er is geen AI-service in gebruik. Stel er een in of kies er een in de instellingen voor AI-diensten."

Voor andere fouten zegt SandFly de foutcode en de eerste zin van het bericht van de service, in het Engels.
