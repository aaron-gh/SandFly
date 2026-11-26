## Tekst typen en bewerken {#typing}

SandFly leest het schermtoetsenbord voor terwijl je het verkent, typt de toets onder je vinger, en leest wat je typt. Je kunt tekst ook dicteren, met de hand op het scherm schrijven, of in braille typen. Dit hoofdstuk beschrijft de instellingen van **Typen**, typen op het schermtoetsenbord, de cursor verplaatsen, tekst selecteren en kopiëren, de klembordgeschiedenis, gedetailleerd bewerken, dicteren en het handschrifttoetsenbord.

Het brailletoetsenbord wordt beschreven in [](#braille), en fysieke toetsenborden en de SandFly-toetsenbordinvoermethode in [](#keyboards).

### Typinstellingen {#typing-settings}

Om de typinstellingen te openen, ga je naar **Typen** in de instellingen van SandFly. Het scherm heeft:

- **Toetsenbordecho**: wat SandFly voorleest terwijl je typt met elk type toetsenbord. Zie [](#typing-keyboard-echo).
- **Dubbeltikken verplaatst cursor naar begin of einde**. Zie [](#typing-double-tap-cursor).
- **Schermtoetsenbord**: **Typemethode**, **Vertraging voor tikken en vasthouden tijdens het typen**, **Toetsen ingedrukt houden na de vertraging voor tikken en vasthouden**, **Focusvertraging bij typen** en **Toetsenbordtoetsen sneller aankondigen**. Zie [](#typing-on-screen-keyboard).
- **Fysiek toetsenbord**: alleen getoond terwijl een fysiek toetsenbord is verbonden. Zie [](#typing-physical-keyboard).
- **Andere manieren om te typen**: **Dicteren**, **Handschrifttoetsenbord** en **Wachttijd na schrijven**. Zie [](#typing-dictation) en [](#typing-handwriting).

### Typ op het schermtoetsenbord {#typing-on-screen-keyboard}

Raak het toetsenbord aan en sleep je vinger over de toetsen. SandFly leest elke toets onder je vinger voor. Hoe een toets wordt getypt, hangt af van de typemethode.

#### Kies een typemethode {#typing-method}

1. Ga naar **Typen** > **Typemethode**.
2. Kies een van de methoden:
   - **Houd je vinger op een toets om deze te selecteren en dubbeltik**: til je vinger op, dubbeltik dan overal om de toets te typen die SandFly het laatst voorlas.
   - **Houd je vinger op een lettertoets om deze te selecteren en haal dan je vinger van het scherm. Dubbeltik voor andere toetsen.**: het optillen van je vinger typt een lettertoets. Andere toetsen, zoals Delete of Enter, vereisen een dubbeltik. Dit is de standaardwaarde.
   - **Houd je vinger op een toets om te selecteren en haal dan je vinger van het scherm**: het optillen van je vinger typt elke toets. Deze methode heeft de laagste vertraging.
   - **Houd je vinger op een toets om die te selecteren en til je vinger dan op. Dubbeltik voor Enter, Klaar of Verzenden.**: het optillen van je vinger typt elke toets behalve de toets die verzendt of indient, zoals Enter, Klaar, Verzenden, Zoeken of Start, waarvoor een dubbeltik nodig is. SandFly herkent deze toets in Gboard. In andere toetsenborden typt elke toets wanneer je je vinger optilt.
   - **Tik op een toets om die te typen, of houd je vinger op een toets om die te selecteren en til je vinger dan op**: een snelle tik op een toets typt deze meteen, zodat snelle dubbele letters twee keer worden getypt. Het vasthouden van een vinger op het toetsenbord verkent de toetsen, en optillen typt de toets onder de vinger. Deze methode is beschikbaar op Android 13 en later.

Bij elke methode kun je ook split-tap gebruiken: houd één vinger op een toets en tik met een andere vinger op het scherm om deze te typen.

Op Android 13 en later zet het kiezen van een van de laatste drie methoden **Focusvertraging bij typen** op 0,15 seconden. Zie [](#typing-focus-delay).

#### Tik een toets aan en houd vast {#typing-touch-and-hold}

**Typen** > **Vertraging voor tikken en vasthouden tijdens het typen** bepaalt hoe lang je je vinger op een toets houdt voordat SandFly er lang op drukt, bijvoorbeeld om de alternatieve tekens van een toets te openen. De keuzes zijn 0,5, 1, 2, 3 en 5 seconden; de standaardwaarde is 3 seconden. De instelling is niet beschikbaar met de typemethode **Houd je vinger op een toets om deze te selecteren en dubbeltik**.

Met **Typen** > **Toetsen ingedrukt houden na de vertraging voor tikken en vasthouden** aan, drukt SandFly de toets aan het einde van de vertraging in en geeft de rest van de aanraking door aan het toetsenbord, zoals een dubbeltik en vasthouden doet. Het toetsenbord behandelt de aanraking dan als een eigen lange druk, zoals een Delete-toets die blijft herhalen terwijl deze wordt vastgehouden. Dit geldt bij de til-om-te-typen-methoden, op alle toetsenborden behalve Gboard en Pixelboard: daar wordt lang op de toetsen gedrukt, wat de pop-up met alternatieve tekens opent. Standaard uit.

#### Alternatieve tekens in Gboard {#typing-gboard-alternatives}

Wanneer je een toets in Gboard vasthoudt, drukt SandFly er na de vertraging voor tikken en vasthouden lang op, en opent Gboard een pop-up met alternatieve tekens. SandFly leest het geselecteerde teken, zoals "Komma", zodra de pop-up opent. Sleep je vinger naar de andere tekens om ze te horen, en til je vinger op om het teken te typen waar je op staat.

Gboard 18.8 en later krijgen aanrakingen via de aanraakverkenning van Android, zoals andere toetsenborden. Bij eerdere versies van Gboard, en van Pixelboard, dat op Gboard is gebaseerd, verkent SandFly aanrakingen op het toetsenbord zelf, op Android 11 en later. Terwijl de pop-up van deze versies open is, selecteert je vinger een van de tekens erin, waar je vinger ook is, het dichtstbijzijnde teken wanneer je vinger buiten de pop-up is, en optillen typt het en sluit de pop-up. Wanneer je vinger het toetsenbord verlaat zonder geopende pop-up, neemt aanraakverkenning het over voor de rest van het scherm, en optillen van je vinger daar typt niets. Dit geldt niet met de typemethode **Houd je vinger op een toets om deze te selecteren en dubbeltik**.

#### Trilling van Gboard-toetsen {#typing-gboard-vibration}

Gboard 18.8 en later spelen de toetsfeedback af die in de Gboard-instellingen is ingesteld, zoals **Trillen bij toetsaanslag**.

Gboard en Pixelboard in versies vóór 18.8 spelen toetsfeedback alleen af wanneer een toets hover-gebeurtenissen ontvangt, die een aanraking die SandFly zelf verkent niet verstuurt. Wanneer Shizuku of root voor SandFly is ingesteld, stuurt SandFly deze hover-gebeurtenissen naar het toetsenbord, zodat het de feedback afspeelt die in zijn eigen instellingen is ingesteld. Zonder Shizuku of root spelen deze versies geen toetsfeedback terwijl SandFly het toetsenbord verkent. Hiervoor is geen instelling. Om Shizuku of root in te stellen, zie [](#updates).

#### Verander de focusvertraging bij typen {#typing-focus-delay}

**Typen** > **Focusvertraging bij typen** bepaalt hoe snel SandFly een toets voorleest nadat je het toetsenbord aanraakt: 0,15, 0,2, 0,25 of 0,3 seconden. De standaardwaarde is 0,3 seconden. Een kortere vertraging leest toetsen sneller voor. De instelling is beschikbaar op Android 13 en later.

Je kunt de vertraging ook wijzigen terwijl je typt met de leesinstelling **Focusvertraging bij typen**, in de groep **Aanraking** van **Leesinstellingen**. Zie [](#commands).

#### Toetsenbordtoetsen sneller aankondigen {#typing-faster-keys}

**Typen** > **Toetsenbordtoetsen sneller aankondigen** bereidt de spraak van de toetsenbordtoetsen van tevoren voor, om de vertraging voor het voorlezen van een toets te verminderen. Standaard uit, en alleen beschikbaar wanneer Google Text-to-Speech de spraakengine is.

#### Swipes en het toetsenbord {#typing-swipes-keyboard}

Swipes vanuit een app, de statusbalk of de navigatiebalk verplaatsen focus nooit naar het toetsenbord. Bij elke typemethode behalve **Houd je vinger op een toets om deze te selecteren en dubbeltik**, gaat een swipe terwijl focus op een toets staat verder in de app, vanuit het tekstveld waarin je typt. Bij de dubbeltikmethode verplaatsen swipes door de toetsen. Swipes vanuit het toetsenbord kunnen het nog steeds verlaten.

### Toetsenbordecho {#typing-keyboard-echo}

Toetsenbordecho bepaalt wat SandFly voorleest terwijl je typt. Elk type toetsenbord heeft zijn eigen instelling.

1. Ga naar **Typen** > **Toetsenbordecho**.
2. Kies een toetsenbord: **Schermtoetsenbord**, **Fysiek toetsenbord**, **Brailletoetsenbord**, **Brailleleesregel** of **Handschrifttoetsenbord**.
3. Kies **Geen**, **Tekens**, **Woorden** of **Tekens en woorden**. De standaardwaarde voor elk is **Tekens en woorden**.

Woordecho leest het hele woord voor zoals het in het veld staat, terug tot de laatste spatie. Woorden met apostroffen of koppeltekens, zoals "don't", zijn één woord, en een web- of e-mailadres is één woord. Wanneer een toetsenbord een woord, de interpunctie erbij en een spatie in één keer toevoegt, zoals het brailletoetsenbord doet in kortschrift, leest SandFly het woord voor.

Elke echo-instelling is ook een leesinstelling, een item van het SandFly-menu en een opdracht in de groep **Spraak** van de opdrachten: **Toetsenbordecho op het scherm**, **Echo van fysiek toetsenbord**, **Echo van brailletoetsenbord**, **Echo van brailleleesregel** en **Echo van handschrifttoetsenbord**. De leesinstellingen en menu-items staan standaard uit. Om ze toe te voegen, ga je naar **Leesinstellingen** of **SandFly-menu** en zet je ze aan in de groep **Toetsenbordecho**. Zie [](#commands).

De sneltoets **Typecho doorlopen** zet **Fysiek toetsenbord** op de volgende keuze. Zie [](#appendix-keyboard-shortcuts).

### Verplaats de cursor {#typing-move-cursor}

Wanneer focus op een tekstveld staat, verplaatsen de leesinstellingen **Tekens**, **Woorden**, **Regels**, **Zinnen** en **Alinea's** de cursor door de tekst, en leest SandFly de tekst voor die hij passeert. Met de standaardgebaren:

1. Swipe omhoog dan omlaag, of omlaag dan omhoog, om een leesinstelling te kiezen. Je kunt ook met drie vingers omhoog of omlaag swipen.
2. Swipe omlaag om de cursor vooruit te verplaatsen, of swipe omhoog om hem terug te verplaatsen.

De opdrachten **Cursor naar begin verplaatsen** en **Cursor naar einde verplaatsen** in de groep **Tekstverwerking** van **Commando's** verplaatsen de cursor naar het begin of einde van de tekst. Ze staan ook in de acties van een tekstveld. Zie [](#typing-editing-actions).

Voor de standaardgebaren, zie [](#appendix-gestures). Voor leesinstellingen, zie [](#commands).

#### Dubbeltik om de cursor naar begin of einde te verplaatsen {#typing-double-tap-cursor}

Met **Typen** > **Dubbeltikken verplaatst cursor naar begin of einde** aan, verplaatst een dubbeltik op het tekstveld waarin je typt de cursor:

- Wanneer de cursor niet aan het begin staat, verplaatst een dubbeltik hem naar het begin van de tekst, en kondigt SandFly "Begin van veld" aan.
- Wanneer de cursor aan het begin staat, verplaatst een dubbeltik hem naar het einde, en kondigt SandFly "Einde van veld" aan.

De opdracht **Activeren** doet hetzelfde wanneer deze wordt uitgevoerd vanuit een ander gebaar, een spraakopdracht, een braillecommando of een script.

In elk ander bedieningselement, of in een tekstveld zonder invoerfocus met een getoond toetsenbord, activeert een dubbeltik het item zoals gewoonlijk. De instelling staat standaard uit, en is beschikbaar op Android 13 en later.

### Selecteer, kopieer, knip en plak {#typing-select-copy-paste}

#### Selecteer tekst {#typing-select-text}

1. Verplaats de cursor naar waar de selectie begint.
2. Voer **Selectiemodus starten of beëindigen** uit. SandFly kondigt "Selectiemodus aan" aan.
3. Verplaats de cursor met een leesinstelling, zoals **Tekens** of **Woorden**. De tekst die de cursor passeert, wordt geselecteerd.
4. Voer **Selectiemodus starten of beëindigen** opnieuw uit om de selectiemodus te beëindigen.

In de NVDA-keymap is **Selectiemodus starten of beëindigen** SandFly-toets + F9. In de SandFly-keymap is dit SandFly-toets + Shift + S.

**Alles selecteren** selecteert alle tekst in het veld, en **Selectie wissen** verwijdert de selectie.

Selectiemodus werkt ook in tekst die niet in een tekstveld staat, zoals een bericht of webpagina, en over items heen. SandFly houdt deze selectie zelf bij: alleen de tekst die de cursor passeert, wordt geselecteerd, en verplaatsen naar een ander item begint daar een nieuw bereik. **Kopiëren** kopieert dan alle bereiken, elk bereik op een eigen regel.

#### Kopieer, knip en plak {#typing-copy-cut-paste}

- **Kopiëren** kopieert de geselecteerde tekst, of de tekst van het item met focus.
- **Toevoegen aan klembord** voegt de geselecteerde tekst, of de tekst van het item met focus, toe aan het einde van de tekst op het klembord, op een nieuwe regel.
- **Knippen** knipt de geselecteerde tekst in een tekstveld.
- **Plakken** plakt het klembord in het tekstveld bij de cursor.

Standaard voert een drievingerdubbeltik en vasthouden **Knippen** uit, en een drievingertripletik en vasthouden **Plakken**. Je kunt elk van deze opdrachten een gebaar geven. Zie [](#appendix-gestures) en [](#commands).

#### Bewerkingsacties en het bewerkingsmenu {#typing-editing-actions}

Wanneer focus op een tekstveld staat, bevat de leesinstelling **Acties** en het item **Acties** van het SandFly-menu bewerkingsacties: **Cursor naar begin verplaatsen**, **Cursor naar einde verplaatsen**, **Selectiemodus starten of beëindigen**, **Knippen**, **Kopiëren**, **Plakken**, **Alles selecteren** en **Gedetailleerde bewerking**. **Gedetailleerde bewerking** wordt niet aangeboden in wachtwoordvelden.

De opdracht **Bewerkingsmenu** opent een menu met **Klembordgeschiedenis**, **Kopiëren**, **Toevoegen aan klembord**, **Knippen**, **Plakken**, **Tekst wissen**, **Alles selecteren** en **Selectiemodus starten of beëindigen**.

#### Andere opdrachten voor tekstbewerking {#typing-editing-commands}

De groep **Tekstverwerking** van **Commando's** heeft ook:

- **Tekst sturen**: drukt de toets die de tekst in het toetsenbord verzendt of indient, zoals Verzenden, Zoeken of Klaar.
- **Nieuwe regel**: doet wat Enter doet. In een tekstveld met meerdere regels typt dit een nieuwe regel bij de cursor en zegt SandFly "Nieuwe regel". In een tekstveld met één regel voert dit de actie van het veld uit, zoals Zoeken of Volgende, of doet anders wat **Tekst sturen** doet. Met spraak ook "nieuwe alinea" of "druk op enter".
- **Tekst wissen**: verwijdert alle tekst in het tekstveld.
- **Aantal tekens**: leest de lengte van de tekst voor, zoals "12 tekens, 3 woorden". In een wachtwoordveld worden alleen de tekens geteld.
- **Tekstveld voorlezen**: leest de hele tekst van het tekstveld voor, of "Leeg" en de hint wanneer het geen tekst heeft.
- **Spelling-/grammaticasuggesties**: opent de suggesties voor de typefout bij de cursor. De leesinstelling **Proeflezen** verplaatst tussen typefouten.
- **Dicteren**: zie [](#typing-dictate).
- **Spraakbewerking starten**: neemt spraakopdrachten voor het tekstveld na elkaar op. Zie [](#voice-commands-editing).
- **Toetsenbord wisselen**: verandert naar het volgende toetsenbord. Beschikbaar op Android 11 en later.
- **Emoji zoeken**, **Symbool kiezen**, **Bewerkingsmodus aan- of uitzetten**, **Wachtwoordveld spellen**, **Volgende woordsuggestie**, **Vorige woordsuggestie** en **Woordsuggestie typen**: opdrachten van het SandFly-toetsenbord. Zie [](#keyboards).

Geen van deze opdrachten heeft standaard een gebaar. Om er een gebaar aan te geven, zie [](#commands).

### Klembordgeschiedenis {#typing-clipboard-history}

SandFly houdt een geschiedenis bij van tot 50 gekopieerde teksten, nieuwste eerst. Elke tekst wordt bewaard tot 200.000 tekens, en de geschiedenis tot 1.000.000 tekens in totaal: oudere teksten daarboven worden verwijderd. **Toevoegen aan klembord** stopt ook bij 200.000 tekens. De geschiedenis bevat de teksten die SandFly kopieert, en de teksten die andere apps kopiëren terwijl een venster van SandFly focus heeft. Android laat apps de inhoud van het klembord niet op de achtergrond lezen, dus met Shizuku of root voegt de schakelaar **Kopieën uit andere apps toevoegen** boven aan de geschiedenis elke tekst toe die andere apps kopiëren. De geschiedenis wordt los van de instellingen bewaard en is niet opgenomen in een backup van de instellingen.

Om de geschiedenis te openen, voer je **Klembordgeschiedenis** uit, vanuit de opdrachten **Tekstverwerking** of het **Bewerkingsmenu**. De geschiedenis opent over het huidige scherm.

- Dubbeltik op een tekst om deze te plakken in het tekstveld waaruit de geschiedenis is geopend. Als de geschiedenis zonder tekstveld is geopend, opent dubbeltikken de acties van de tekst.
- Dubbeltik en houd een tekst vast om de acties erbij te openen: **Plakken**, **Kopiëren** opnieuw, **Toevoegen aan klembord**, **Bewerken** van de tekst, **Exporteren naar bestand**, of **Verwijderen**. De acties staan ook bij **Acties** van de tekst.
- **Selecteren** toont de teksten als selectievakjes. Dubbeltik op teksten om ze aan te vinken, en kies dan **Verwijderen** of **Exporteren naar bestand** voor de aangevinkte teksten.
- **Exporteren naar bestand** bewaart de hele geschiedenis als een tekstbestand. Meerdere teksten worden gescheiden door een regel van drie koppeltekens tussen lege regels.
- **Geschiedenis wissen** verwijdert alle teksten.

Om te plakken zonder de geschiedenis te openen, voeg je de leesinstelling **Klembordgeschiedenis** toe, in de groep **Acties voor huidig item** van **Leesinstellingen**. Swipe in een tekstveld omhoog of omlaag om door de teksten in de geschiedenis te gaan, en dubbeltik om de tekst te plakken die SandFly het laatst voorlas. De leesinstelling staat standaard uit. Zie [](#commands).

### Kopieer tekst van het scherm {#typing-copy-from-screen}

Deze opdrachten staan in de groep **Tekstverwerking** van **Commando's**:

- **Laatst uitgesproken woordgroep kopiëren**: kopieert de laatste tekst die SandFly voorlas. Standaard voert een drievingertripletik dit uit.
- **Laatste gesproken zin woord voor woord selecteren**: opent een lijst van de woorden van de laatste zin, om de woorden te controleren die je wilt kopiëren.
- **Tekst van scherm kopiëren**: opent een lijst van de tekst op het scherm, opgesplitst in adressen en getallen, tekens, woorden of alinea's, om de delen aan te vinken die je wilt kopiëren. Zie [](#reading-copy-text).
- **Doorlopend kopiëren**: verplaatst focus van het item met focus door elk item daarna, zonder spraak, waarbij lijsten en pagina's onderweg scrollen, en verzamelt de tekst van elk item. Raak het scherm aan, druk op een toets of voer een andere opdracht uit om te stoppen. Aan het einde worden de teksten getoond met elk een keuzevakje; vink de teksten aan die je wilt kopiëren en kies **Opslaan**. Doorlopend kopiëren stopt na 2000 items of 10 minuten.

In deze lijsten vinken de acties **Selectie starten** en **Selectie beëindigen** van een item elk item tussen twee items aan. Dubbeltikken en vasthouden van een item doet hetzelfde.

### Gedetailleerde bewerking {#typing-granular-editing}

Gedetailleerde bewerking opent de tekst van het tekstveld met focus, of van elk item met focus dat tekst heeft, in een eigen scherm, opgesplitst in stukken. Om dit te openen, kies je **Gedetailleerde bewerking** in de acties van een tekstveld, of voer je de opdracht **Gedetailleerde bewerking** uit vanuit de groep **Tekstverwerking** van **Commando's**. Wachtwoordvelden kunnen niet worden geopend.

- **Splitsen op** kiest de stukken: **Tekens**, **Woorden**, **Zinnen**, **Regels** of **Alinea's**. De standaardwaarde is **Woorden**, en SandFly houdt je laatste keuze aan.
- Dubbeltik op een stuk om het te bewerken. Het leeg maken ervan verwijdert het.
- De acties van een stuk zijn **Selecteren**, **Invoegen voor**, **Invoegen na** en **Verwijderen**. Dubbeltik en houd een stuk vast om ze in een dialoogvenster te openen. **Selectie starten** en **Selectie beëindigen** selecteren elk stuk tussen twee stukken.
- Spaties, tabs en regeleinden tussen stukken zijn eigen stukken, zoals "2 spaties" of "Nieuwe regel", en een lege regel wordt voorgelezen als "Lege regel".
- **Kopiëren** kopieert de geselecteerde stukken, of alle tekst wanneer geen stuk is geselecteerd.
- **Meer** heeft **Ongedaan maken**, **Opnieuw**, **Zoeken**, **Volgende zoeken**, **Vorige zoeken**, **Vervangen**, **Tekst toevoegen aan het einde**, **Alles selecteren**, **Selectie wissen**, **Geselecteerde tekst knippen** en **Geselecteerde verwijderen**. **Vervangen** vervangt alle overeenkomsten, zonder onderscheid tussen hoofd- en kleine letters.
- **Opslaan** schrijft de tekst terug naar het tekstveld en sluit het scherm. Als het tekstveld er niet meer is, wordt de tekst in plaats daarvan gekopieerd.
- Terug opent een dialoogvenster om de wijzigingen op te slaan of te verwijderen.

Tekst die niet in een tekstveld staat, zoals een bericht, kan worden bewerkt en gekopieerd maar niet opgeslagen.

### Dicteren {#typing-dictation}

#### Dicteer in een tekstveld {#typing-dictate}

De opdracht **Dicteren** typt wat je zegt in het tekstveld met focus, bij de cursor.

1. Verplaats focus naar een tekstveld.
2. Voer **Dicteren** uit. Een geluidssignaal speelt, en de opname begint. Spraak wordt gedempt terwijl SandFly opneemt.
3. Spreek. De opname gaat door tijdens pauzes.
4. Voer **Dicteren** opnieuw uit. Een tweede geluidssignaal speelt, en SandFly typt de tekst.

De opname stopt zelf na 15 minuten, of na een stiltetijd die je instelt. Zie [](#typing-dictation-settings).

Standaard voert een tweevingerdubbeltik **Dicteren, media afspelen of pauzeren** uit. Dit start **Dicteren** wanneer een tekstveld invoerfocus heeft, een toetsenbord wordt getoond of is verbonden, en er geen gesprek actief is; het stopt een dictatie die opneemt; anders speelt het media af of pauzeert ze. Voor de standaardgebaren, zie [](#appendix-gestures).

Op telefoons met een dempknop, zoals de Zinwa Q25, houd je de dempknop ingedrukt in een tekstveld om te dicteren: de opname begint wanneer je op de knop drukt en stopt wanneer je deze loslaat. De instelling is **Volumetoetsen** > **Dempknop ingedrukt houden om te dicteren in tekstvelden**, standaard aan. Zie [](#buttons-mute-key).

#### Kies hoe spraak wordt herkend {#typing-speech-recognition}

Ga naar **Typen** > **Dicteren** > **Spraakherkenning**, en kies een van de volgende:

- **Automatisch**: de standaardwaarde. SandFly gebruikt Google-spraakherkenning op het apparaat wanneer deze is geïnstalleerd en de taal van de telefoon heeft; anders Rambler, wanneer er een Gemini API-sleutel is en Rambler de taal ondersteunt; anders Whisper, wanneer een model is gedownload; anders Android-spraakherkenning. Het scherm toont welke er wordt gebruikt, zoals "Automatisch: Rambler".
- **Google-spraakherkenning op het apparaat**: de herkenner van de app Spraakherkenning en synthese, met zijn offline taalpakketten, in de taal van de telefoon. Op Android 13 en later voegt deze interpunctie en hoofdletters toe terwijl **Automatische interpunctie** aan staat. Wanneer de taal van de telefoon geen taalpakket heeft, opent de rij **Downloaden** (taal) **voor herkenning op het apparaat** het downloadscherm van Spraakherkenning en synthese.
- **Rambler (Gemini API)**: het model Gemini 3.5 Transcribe, via de Gemini API-sleutel die is ingesteld in AI-services. Dit verwijdert stopwoorden, lost zelfcorrecties op, en formatteert interpunctie, getallen en lijsten. Zie [](#descriptions) voor de Gemini API-sleutel.
- **Whisper**: herkenning op de telefoon, met een gedownload Whisper-model. Zie [](#typing-whisper-models).
- **Android-spraakherkenning**: de spraakherkenningsservice die is gekozen in de Android-instellingen.

Een keuze die niet kan worden gebruikt, valt terug op dezelfde volgorde als **Automatisch**. Google-spraakherkenning op het apparaat en Android-spraakherkenning starten na elke pauze opnieuw totdat je dicteren stopt. Als Rambler niet reageert, wordt de opname getranscribeerd met Whisper wanneer een model is gedownload.

**Dicteren** herkent de taal van het toetsenbord, zoals de taal die in Gboard is gekozen, wanneer die verschilt van de taal van de telefoon. Google-spraakherkenning op het apparaat heeft dan een taalpakket nodig voor de taal van het toetsenbord; zonder taalpakket wordt de volgende manier van spraakherkenning in de volgorde van **Automatisch** gebruikt. Spraakopdrachten en andere spraakinvoer gebruiken de taal van de telefoon.

Met **Live transcriptie van Rambler** aan, streamt SandFly de opname terwijl je spreekt, en ontvangt de tekst van elke zin wanneer je pauzeert, zodat de tekst sneller wordt getypt na het stoppen. Als de stream mislukt, stuurt SandFly de hele opname wanneer je stopt. Standaard aan, en alleen getoond wanneer er een Gemini API-sleutel is.

Opmerking: De opname wordt naar Google gestuurd met Rambler. Met een Gemini API-sleutel van het gratis niveau kan Google de audio en tekst gebruiken om zijn producten te verbeteren; met een betaald niveau niet.

#### Whisper-modellen {#typing-whisper-models}

Whisper herkent spraak op de telefoon, zodat de opname op de telefoon blijft. **Typen** > **Dicteren** > **Whisper-modellen** toont de modellen om te downloaden:

- **Whisper Tiny** (32 MB), **Whisper Base** (60 MB), **Whisper Small** (190 MB) en **Whisper Large v3 Turbo** (574 MB): meertalige modellen. Een groter model is nauwkeuriger en trager.
- **FUTO Tiny**, **FUTO Base** en **FUTO Small**, en Engelstalige versies van elk: modellen van FUTO.

Om een model te gebruiken:

1. Kies een model. SandFly downloadt het, na een bevestiging bij mobiele data.
2. Om naar een ander gedownload model te wisselen, kies je dit, en kies je daarna **Gebruiken**. Om een model te verwijderen, kies je het, en kies je daarna **Verwijderen**.

Het eerste model dat je downloadt, wordt gebruikt. Whisper herkent de taal die is beschreven in [](#typing-speech-recognition).

**Modelbestand** voegt een eigen Whisper-model toe in het whisper.cpp-formaat, zoals een model dat is getraind voor een taal, uit de bestanden op de telefoon, tot 4 GB. Eenmaal toegevoegd, kun je het gebruiken, vervangen met **Ander bestand kiezen**, of verwijderen. Een bestand dat geen whisper.cpp-model is, wordt geweigerd.

**Processor**, alleen getoond op telefoons met een Adreno-GPU en een OpenCL-driver, kiest of Whisper draait op de **CPU** of de **GPU**. Als Whisper niet op de GPU kan draaien, draait het op de CPU.

#### Corrigeer dicteren met AI {#typing-dictation-correction}

Met **Typen** > **Dicteren** > **Dicteren corrigeren met AI** aan, stuurt SandFly de tekst van Google-spraakherkenning op het apparaat, Whisper of Android-spraakherkenning naar een AI-service voordat deze wordt getypt, met tot 300 tekens van de tekst voor de cursor. De service verwijdert stopwoorden, stotteren en valse starts, lost zelfcorrecties op ("twee, nee, drie" wordt "drie"), voegt interpunctie en hoofdletters toe, corrigeert verkeerd gehoorde woorden, en formatteert getallen, datums en lijsten. Standaard aan. Tekst van Rambler wordt niet opnieuw gestuurd, en dicteren in een wachtwoordveld wordt niet gestuurd.

- **AI-service voor dicteren**: **Zelfde als AI-diensten** (de standaardwaarde), elke AI-service die is ingesteld, of **AI op het apparaat** wanneer zijn model is gedownload. Met AI op het apparaat blijft al het dicteren op de telefoon. Zie [](#descriptions).
- **Correctieprompt**: de instructies die de AI-service ontvangt, zoals een taal of stijl. Het veld begint met de ingebouwde prompt. Het ongewijzigd of leeg opslaan houdt de ingebouwde prompt aan.

Als de service niet binnen 20 seconden reageert (60 seconden voor AI op het apparaat), of tekst teruggeeft die veel langer is dan de spraak, typt SandFly de tekst zoals herkend. Het uitvoeren van **Dicteren** voordat de service reageert, doet meteen hetzelfde.

#### Andere instellingen voor dicteren {#typing-dictation-settings}

In **Typen** > **Dicteren**:

- **Automatische interpunctie**: Google-spraakherkenning voegt interpunctie en hoofdletters toe. Standaard aan. Getoond op Android 13 of later met Google-spraakherkenning geïnstalleerd.
- **Aanstootgevende woorden blokkeren**: Google-spraakherkenning maskeert aanstootgevende woorden. Standaard aan. Getoond bij **Automatische interpunctie**.
- **Automatisch stoppen na stilte**: stopt de opname na 1, 2, 3, 5, 10, 15 of 30 seconden, of 1 minuut, zonder spraak. De standaardwaarde is **Uit**.
- **Gedicteerde tekst uitspreken**: leest de tekst voor nadat SandFly deze typt, behalve in wachtwoordvelden. Standaard uit.
- **Herkenningsmodellen geladen houden**: hoelang een Whisper-model en het AI-model op het apparaat in het geheugen blijven na gebruik: **Na gebruik uitladen** (de standaardwaarde), 5, 15 of 30 minuten, of **Totdat SandFly stopt**. Een geladen model start sneller en gebruikt geheugen.
- **Downloadserver voor modellen**: waar modellen worden gedownload vandaan, **Hugging Face** (de standaardwaarde) of **hf-mirror.com**.

### Handschrifttoetsenbord {#typing-handwriting}

SandFly-handschrift is een toetsenbord dat typt wat je met één vinger op het scherm schrijft. Dit is beschikbaar op telefoons en tablets met Android 11 of later. Het toetsenbord vult het scherm, en terwijl SandFly aan staat, gaan aanrakingen erop rechtstreeks erheen in plaats van via aanraakverkenning.

#### Zet het handschrifttoetsenbord aan {#typing-handwriting-turn-on}

1. Ga naar **Typen** > **Handschrifttoetsenbord**. De toetsenbordinstellingen van Android openen.
2. Zet **SandFly handschrift** aan.
3. Kies in een tekstveld **SandFly handschrift** als toetsenbord, bijvoorbeeld met de opdracht **Toetsenbord wisselen** of de toetsenbordwisselaar van Android.

De eerste keer dat het toetsenbord opent, doorloopt een handleiding het schrijven van een letter, een woord en elk gebaar, en gaat verder wanneer je elke stap doet. Er wordt niets in het veld getypt tijdens de handleiding. Om deze vroeg te beëindigen, swipe je met drie vingers omlaag. Om de handleiding opnieuw te openen, tik je met drie vingers.

Handschrift wordt op de telefoon herkend door Google ML Kit, in de taal van de telefoon. De eerste keer toont SandFly een dialoogvenster om het herkenningsmodel te downloaden, ongeveer 20 MB, en kondigt aan wanneer het is gedownload.

#### Schrijf met het handschrifttoetsenbord {#typing-handwriting-write}

Schrijf een letter, meerdere letters of een heel woord ergens op het scherm. Streken die binnen de wachttijd op elkaar volgen, zijn één stuk schrift, dat daarna wordt getypt en voorgelezen. Puntjes, zoals op een i, tellen als streken.

**Typen** > **Wachttijd na schrijven** bepaalt de wachttijd, van 0,2 tot 1 seconde; de standaardwaarde is 0,6 seconden. Een kortere wachttijd typt sneller, maar elke streek van een letter met meerdere streken, zoals H, moet erbinnen beginnen. De instelling geldt ook voor de opdrachten **Letter van een leesinstelling schrijven** en **Schrijf de naam van een app om deze te openen**. Zie [](#commands).

| Gebaar | Actie |
| --- | --- |
| Tweevingerswipe naar rechts | Typ een spatie |
| Tweevingerswipe naar links | Verwijder het teken voor de cursor |
| Tweevingerswipe omlaag | Nieuwe regel, of de actie van het veld, zoals Verzenden, Zoeken of Klaar |
| Tweevingerswipe omhoog | Vervang het laatste geschreven teken door de volgende lezing van dezelfde lengte, zoals "h" voor "H" |
| Tweevingertik | Lees de tekst voor de cursor voor |
| Drievingerswipe omlaag | Verberg het toetsenbord |
| Drievingerswipe naar rechts of links | Verander naar het volgende of vorige toetsenbord (Android 9 en later) |
| Drievingertik | Open de handleiding |

**Typen** > **Toetsenbordecho** > **Handschrifttoetsenbord** bepaalt wat het toetsenbord voorleest terwijl je typt: elk stuk schrift, elk woord wanneer een spatie of nieuwe regel het afsluit, beide, of niets. Verwijderde tekens en de lezingen die het laatste geschreven stuk vervangen, worden altijd voorgelezen, wat de echo ook is. In tekstvelden van SandFly wordt de tekst voor de cursor niet voorgelezen, en een verwijdering wordt alleen voorgelezen als "Verwijderd".

Terwijl het handschrifttoetsenbord wordt getoond, werken alleen opdrachten die geen venster openen, zoals navigatie en leesinstellingen.

### Fysieke toetsenborden {#typing-physical-keyboard}

Terwijl een fysiek toetsenbord is verbonden, heeft de groep **Fysiek toetsenbord** van **Typen** **Lettertoetsen drukken op knoppen van het numerieke toetsenblok**, **Enter verstuurt berichten**, **SandFly-toetsenbord**, **Invoer bij ingedrukte toets**, **Tekens bij twee keer drukken**, **Twee keer spatie typt een punt**, **Autocorrectie**, **Woordsuggesties**, **Suggestie uitspreken**, **Enter typt de uitgesproken suggestie**, **Taal van de woordenlijst**, **Geleerde woorden verwijderen**, **Wachtwoordtekens uitspreken** en **Android-instellingen voor fysiek toetsenbord**. Deze instellingen worden beschreven in [](#keyboards).
