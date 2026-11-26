## Hulp en feedback {#help}

**Hulp en feedback** in Instellingen voor SandFly heeft:

- **Gebruikershandleiding**: deze handleiding. Zie [](#getting-started-user-guide).
- **SandFly-tutorial**: de interactieve tutorial. Zie [](#help-tutorial).
- **Gebaren oefenen**: de gebaaroefenles van de tutorial. Zie [](#help-tutorial).
- **Toetsenbordoefening**: zet toetsenbordoefening aan. SandFly zegt elke ingedrukte toets en de opdracht ervan, en voert niets uit. Beschikbaar terwijl SandFly aan staat; niet getoond op horloges. Zie [](#keyboards-practice).
- **Gebarentrainer**: oefeningen die gebaren meten aan de grenzen die SandFly gebruikt. Niet getoond op horloges en tv's. Zie [](#gestures-trainer).
- **Feedback sturen**: probleemrapporten en suggesties aan de ontwikkelaar van SandFly, en de antwoorden. Zie [](#help-feedback).
- **Versiegeschiedenis**: de releasenotities van elke SandFly-release. Zie [](#help-version-history).
- **Privacybeleid**: het privacybeleid van SandFly. Zie [](#help-version-history).

De instellingen voor ontwikkelaars en voor het vinden van problemen staan in **Ontwikkelaarsinstellingen**. Zie [](#help-developer).

### Tutorial en gebaren oefenen {#help-tutorial}

**Hulp en feedback** > **SandFly-tutorial** opent de SandFly-tutorial bij de les waar je was. Op een apparaat met een aanraakscherm en een fysiek toetsenbord biedt SandFly eerst **Tutorial over aanraakbediening** en **Tutorial over toetsenbordbediening** aan. Voor de lessen en hoe de tutorial werkt, zie [](#getting-started-tutorial).

**Hulp en feedback** > **Gebaren oefenen** opent **Gebaren oefenen**, waar elk gebaar de naam en de opdracht noemt die erin is toegewezen, zonder de opdracht uit te voeren. Om deze te verlaten, ga je naar **Oefenen beëindigen** en activeer je dit.

De opdrachten **Tutorial openen** en **Gebaren oefenen** openen dezelfde lessen. **Gebaren oefenen** wordt niet getoond op horloges en tv's, die hun eigen tutorials hebben. Op een tv zonder tutorial heet **SandFly-tutorial** **Hulp**. Zie [](#devices).

### Stuur feedback {#help-feedback}

**Feedback sturen** stuurt een probleemrapport of een suggestie naar de ontwikkelaar van SandFly, zonder account. De ontwikkelaar kan antwoorden, en de antwoorden komen naar SandFly. Om dit te openen, ga je naar **Hulp en feedback** > **Feedback sturen**, of kies je **Feedback sturen** in het SandFly-menu.

Het scherm **Feedback sturen** heeft:

- **Nieuwe feedback**: een nieuw rapport.
- **Postvak IN**: de feedback die je hebt gestuurd, met de status van elk rapport en de antwoorden.
- **Concepten**: rapporten die je bent begonnen en nog niet hebt gestuurd.

#### Stuur een rapport {#help-feedback-send}

1. Ga naar **Hulp en feedback** > **Feedback sturen**.
2. Kies **Nieuwe feedback**.
3. Vul het formulier in:
   - **Type feedback**: **Onjuist gedrag**, **Crash**, **Traag of reageert niet**, of **Suggestie**. De standaardwaarde is **Onjuist gedrag**.
   - **Gebied**: het onderdeel van SandFly, zoals **Spraak**, **Gebaren**, **Brailleleesregel** of **Updates**.
   - **Titel**: een korte samenvatting. Verplicht.
   - **App waar het gebeurt**: de naam van de app, als het probleem in één app optreedt.
   - **Beschrijving**: wat er gebeurt en wat je verwachtte. Verplicht.
   - **Stappen om te reproduceren**: wat je moet doen om het probleem te laten optreden. Wordt niet getoond voor suggesties.
   - **E-mail voor antwoorden**: optioneel. Antwoorden komen naar SandFly, of je nu een adres invoert of niet.
   - **Apparaatinformatie toevoegen**: standaard aan. Zie [](#help-feedback-contents).
   - **SandFly-logboek toevoegen**: standaard uit. Zie [](#help-feedback-contents).
4. Kies **Verzenden**.

SandFly zegt "Feedback verzonden." en het rapport verplaatst naar **Postvak IN**. Als het rapport niet kan worden verstuurd, zegt SandFly waarom, zoals "Feedback niet verzonden. Controleer de internetverbinding.", en blijft het rapport een concept.

Wat je typt, wordt als concept bewaard tot je het verstuurt. **Concept verwijderen** verwijdert het. Het e-mailadres en de twee schakelaars voor **toevoegen** worden bewaard voor het volgende rapport.

#### Wat een rapport bevat {#help-feedback-contents}

Een rapport bevat altijd de velden die je hebt ingevuld en de versie en het buildnummer van SandFly.

**Apparaatinformatie toevoegen** voegt de Android-versie en de fabrikant en het model van het apparaat toe. De schakelaar toont de informatie die wordt toegevoegd.

**SandFly-logboek toevoegen** voegt het recente logboek van SandFly toe, wat de ontwikkelaar helpt de oorzaak van een probleem te vinden. Het logboek kan tekst van het scherm bevatten. Het bevat:

- Het logboek van SandFly vanaf het moment dat je **Feedback sturen** hebt geopend, zodat het probleem dat je wilt rapporteren er nog in staat. In gesplitst scherm wordt het logboek gelezen wanneer je **Verzenden** kiest, zodat je het probleem eerst in de andere app kunt laten optreden.
- De laatste gebaren die SandFly heeft herkend.
- De laatste crash en het laatste moment dat SandFly uitging, en tijden waarop SandFly even niet reageerde.
- De toegankelijkheidsservices die aan staan, en de laatste keren dat op de volumetoetsen en de Enter-toets is gedrukt.
- De laatste aankondigingen, meldingen, toasts en vensternamen die apps hebben gestuurd, met de app die elk ervan stuurde en het begin van de tekst.
- Waar de focus na Terug naartoe ging, de tijd die het controleren van de voorwaarden in **Alleen tonen in Acties als** van macro's kost, wat audio afspeelt en opneemt, en typen vanuit de Gboard-pop-up met alternatieve tekens.
- De logboeken van de andere processen van SandFly, zoals AI op het apparaat.

Om een probleem snel na het optreden te rapporteren, open je **Feedback sturen** voordat je iets anders doet, zodat het logboek het nog bevat.

#### Antwoorden en status {#help-feedback-inbox}

**Postvak IN** toont de rapporten die je hebt gestuurd, met de status van elk en het aantal antwoorden dat je nog niet hebt gelezen. De status is een van:

- **Open**.
- **Gesloten, voltooid**.
- **Gesloten, niet gepland**.
- **Gesloten, duplicaat**.
- **Verwijderd**: het rapport staat niet meer op de rapportserver.

Kies een rapport om het te openen. Het toont **Status**, **Jouw feedback** en **Berichten**, de antwoorden van de ontwikkelaar en je eigen berichten, elk met wie het heeft gestuurd en wanneer. Om te antwoorden:

1. Kies **Jouw antwoord** en typ het antwoord.
2. Kies **Antwoord verzenden**.

Terwijl SandFly aan staat, controleert het elk uur op antwoorden en statuswijzigingen, en elke keer dat je **Feedback sturen** opent. Een antwoord of een statuswijziging toont een melding met geluid, die het rapport opent. Gesloten rapporten worden nog 30 dagen gecontroleerd. Meldingen voor antwoorden staan in de meldingscategorie **Feedback-antwoorden** van SandFly.

**Verwijderen uit Postvak IN** verwijdert een rapport van het apparaat en stopt de controles. De ontwikkelaar houdt het rapport.

#### Rapporteer een AI-antwoord {#help-feedback-ai-response}

De opdracht **AI-antwoord melden** opent een nieuw rapport met de laatste tekst die een AI-model heeft gegeven, zoals een afbeeldings- of schermbeschrijving, een resultaat van live herkenning of het resultaat van een agenttaak. Gebruik deze om een antwoord te rapporteren dat onjuist of kwetsend is. SandFly bewaart alleen het laatste antwoord, tot SandFly stopt. Als er geen AI-antwoord is geweest, zegt SandFly dit. Wijs de opdracht toe aan een gebaar of toets in **Commando's**. Zie [](#commands) en [](#descriptions).

### Versiegeschiedenis en privacybeleid {#help-version-history}

**Versiegeschiedenis** opent de [SandFly-releasepagina](https://github.com/aaron-gh/SandFly/releases) in de browser. Deze bevat de releasenotities van elke release. **Privacybeleid** opent het [privacybeleid van SandFly](https://sandfly.dev/privacy). Op een horloge zonder browser toont SandFly in plaats daarvan het adres.

### Ontwikkelaarsinstellingen {#help-developer}

**Ontwikkelaarsinstellingen** heeft instellingen voor het vinden van problemen en voor ontwikkeling. Sommige hiervan schrijven de inhoud van het scherm naar het systeemlogboek, dat andere apps met toegang tot logboeken kunnen lezen. Laat ze uit, behalve als je ze nodig hebt.

- **Versiecode**: de versie van SandFly en de versiecode. Op Android 11 en eerder wordt de versie in plaats daarvan onder de titel van het scherm getoond.
- **Diagnosemodus**: standaard uit. Een tik met 4 vingers schrijft de inhoud van het scherm naar het systeemlogboek. Terwijl de modus aan staat, werken **Spraakuitvoer tonen**, **Herkende spraak herhalen**, **Foutopsporing voor knooppuntstructuren aanzetten** en **Overlay met foutopsporingslogboek aanzetten** alsof ze aan staan, en kunnen deze en **Niveau van logoutput** niet worden gewijzigd. SandFly toont een bevestigingsdialoogvenster. SandFly vraagt je om dit te bevestigen.
- **Spraakuitvoer tonen**: standaard uit. Toont de tekst die SandFly uitspreekt op het scherm.
- **Gebaren verwerken in SandFly**: standaard aan. SandFly herkent gebaren zelf in plaats van dit aan Android over te laten. Veel gebaarfuncties van SandFly hebben dit nodig. Zet SandFly uit en weer aan nadat je dit wijzigt. Wordt niet getoond op tv's.
- **AI-geheugenlimieten op het apparaat negeren**: standaard uit. Toont elk AI-model op het apparaat, ook modellen die meer geheugen nodig hebben dan het apparaat heeft, en laadt een model zonder op vrij geheugen te controleren. Zie [](#descriptions).
- **Herkende spraak herhalen**: standaard uit. SandFly spreekt de tekst uit die het heeft herkend van een spraakopdracht. Zie [](#voice-commands).
- **Prestatiestatistieken aanzetten**: standaard uit. SandFly verzamelt tijdstatistieken, die de opdracht **Prestatiestatistieken afdrukken** naar het systeemlogboek schrijft. Gebruikt meer geheugen en stroom. SandFly toont een bevestigingsdialoogvenster.
- **Verkennen via aanraking**: standaard aan. Dit uitzetten laat SandFly stoppen met het voorlezen van items die je aanraakt en het behandelen van gebaren. SandFly toont een bevestigingsdialoogvenster. Wordt niet getoond op tv's.
- **Foutopsporing voor knooppuntstructuren aanzetten**: standaard uit. De opdracht **Knooppuntstructuur afdrukken** schrijft de boomstructuur van items op het scherm naar het systeemlogboek. Wanneer je dit aanzet, biedt een dialoogvenster aan om **Knooppuntstructuur afdrukken** toe te wijzen aan een drietik met 4 vingers. Op een tv schrijft de zoekknop van de afstandsbediening de boomstructuur.
- **Overlay met foutopsporingslogboek aanzetten**: standaard uit. Toont het logboek van SandFly op het scherm.
- **Niveau van logoutput**: hoeveel SandFly naar het systeemlogboek schrijft, van **NONE** tot **VERBOSE**. De standaardwaarde is **ERROR**. Verbose-logging, wat persoonlijke informatie in het logboek kan plaatsen, moet worden bevestigd.
- **Gebeurtenisregistratie voor toegankelijkheid filteren**: de soorten toegankelijkheidsgebeurtenissen die SandFly naar het systeemlogboek schrijft.

Om een los item te inspecteren, en om de details van een item of de boomstructuur van het scherm te kopiëren, gebruik je de opdrachten die worden beschreven in [](#scripts).
