## Meldingen {#notifications}

SandFly leest nieuwe meldingen voor zodra ze binnenkomen, en kan de meldingen in het meldingenpaneel op verzoek of op een vast tijdstip voorlezen. **Meldingen** in Instellingen voor SandFly stelt in welke meldingen SandFly voorleest, wat het zegt, in welke stem, en wanneer. Het zet ook de meldingenlijst van SandFly aan, die de plaats van het meldingenpaneel kan innemen.

Inkomende gesprekken worden altijd voorgelezen, wat de meldingsinstellingen ook zijn.

### Toegang tot meldingen {#notifications-access}

Sommige functies vereisen toegang tot meldingen voor SandFly, die de Android-instellingen geven:

- Het voorlezen van meldingen die Android niet aankondigt aan schermlezers. Zie [](#notifications-not-announced).
- Het volledig voorlezen van privémeldingen terwijl de telefoon is vergrendeld.
- **Lees meldingen toegestaan door de modus Niet storen**. Zonder toegang tot meldingen leest SandFly geen meldingen voor terwijl Niet storen aanstaat met deze keuze.
- Het stoppen van een melding als die wordt gesloten of verwijderd.
- Samenvattingen van de meldingen in het meldingenpaneel.
- De meldingenlijst van SandFly.
- Het beëindigen van het lopende gesprek van een app, zoals WhatsApp, met **Gesprek beëindigen**. Zie [](#commands-calls).

Om toegang tot meldingen te geven, selecteer je **Toegang tot meldingen aanzetten** op het scherm **Wanneer voorlezen**, **Stop met lezen** of **Samenvatting en tijd**, en zet je SandFly aan in de lijst. Het item wordt alleen getoond zolang SandFly geen toegang tot meldingen heeft. SandFly maakt alleen verbinding met meldingen terwijl SandFly aanstaat.

### Het voorlezen van meldingen aan- of uitzetten {#notifications-speech-on-off}

**Meldingen** > **Meldingen uitspreken** zet het voorlezen van nieuwe meldingen aan of uit. Standaard aan. Inkomende gesprekken worden voorgelezen terwijl deze instelling uitstaat.

Je kunt het voorlezen van meldingen ook aan of uit zetten met:

- De snelle instellingentegel **Meldingen voorlezen**.
- De opdracht **Meldingen voorlezen aan of uit**, die je aan een gebaar of sneltoets kunt toewijzen. SandFly zegt "Meldingen voorlezen ingeschakeld" of "Meldingen voorlezen uitgeschakeld".
- Een automatiseringsapp. Zie [](#notifications-automation).

### Opdrachten voor meldingen {#notifications-commands}

De categorie **Meldingen** in **Commando's** heeft:

- **Meldingen lezen**: leest de meldingen in het meldingenpaneel voor, zoals **Samenvatting en tijd** instelt. Zie [](#notifications-summary).
- **Lees de laatste melding nog eens**: zegt de laatste melding die SandFly heeft voorgelezen opnieuw. SandFly zegt "Er is geen melding gelezen" als er geen is.
- **Voorlezen van de melding stoppen**: stopt de melding die wordt voorgelezen, en die nog wachten om voorgelezen te worden.
- **Meldingen voorlezen aan of uit**
- **Tijdaankondigingen aan of uit**: zet **Zeg de tijd elke** op **Uit**, of terug naar het interval dat het eerder had, of op 1 uur als er geen was ingesteld. SandFly zegt "Tijdaankondigingen aan" of "Tijdaankondigingen uit".

Je kunt deze toewijzen aan gebaren, sneltoetsen en andere invoer. Zie [](#commands).

Een gebaar, sneltoets of toets die je gebruikt terwijl een melding wordt voorgelezen, stopt die melding, en SandFly zegt wat het gebaar of de toets doet.

### Wijzig wat wordt voorgelezen {#notifications-reading}

**Meldingen** > **Meldingen voorlezen** stelt in wat SandFly zegt voor een melding en hoe. Onder **Wat wordt voorgelezen**:

- **Spraakformaat**: de tekst die SandFly zegt voor elke melding, opgebouwd uit tijdelijke aanduidingen: `{app}`, `{title}`, `{text}`, `{content}` (de hele tekst), `{bigtext}`, `{subtext}`, `{ticker}`, `{category}` en `{time}`. De standaardinstelling is `{category}, {title}, {text}`.
- **Voorlezen van privémeldingen**: wat SandFly zegt in plaats van de inhoud van een privémelding, met de tijdelijke aanduidingen `{app}`, `{category}` en `{time}`. De standaardinstelling is `{app}`.

Onder **Tekst**:

- **Emoji weglaten**: emoji in meldingen worden niet voorgelezen. Standaard uit.
- **Lees hoofdletters als woorden**: woorden in hoofdletters worden als woorden voorgelezen. Sommige spraakengines spellen ze letter voor letter. Standaard uit.
- **Lees lange getallen cijfer voor cijfer**: getallen zoals codes en telefoonnummers worden één cijfer per keer voorgelezen. Standaard uit. **Minimaal aantal cijfers in een lang getal** stelt in hoeveel cijfers een getal moet hebben om zo voorgelezen te worden, van 3 tot 10; de standaardinstelling is 5.
- **Koppelingen**: **Volledige link voorlezen**, de standaardinstelling, **Alleen de site voorlezen**, **Onderstaande tekst uitspreken**, of **Weglaten**. **Tekst in plaats van een link** is de tekst voor **Onderstaande tekst uitspreken**.
- **Meldingen zonder tekst overslaan**: meldingen zonder tekst worden niet voorgelezen. Standaard uit.

Onder **Lengte** staat elke instelling standaard op **Uit**:

- **Maximaal aantal woorden**: leest tot 5, 10, 15, 20, 30, 50 of 100 woorden voor.
- **Maximaal aantal zinnen**: leest tot 1, 2, 3 of 5 zinnen voor.
- **Maximaal aantal teksttekens**: leest tot 50, 100, 200, 300, 500 of 1000 tekens voor.
- **Stoppen met lezen na**: stopt een melding na 5 tot 60 seconden.

Onder **Tijdstip**:

- **Wachten voor voorlezen**: wacht tot 10 seconden voordat een melding wordt voorgelezen. Standaard **Uit**.
- **Wanneer een andere melding wordt voorgelezen**: wat er gebeurt als een melding binnenkomt terwijl een andere wordt voorgelezen: **Erna voorlezen**, de standaardinstelling, **Onderbreken**, of **Nieuwe niet voorlezen**.
- **Opnieuw voorlezen tot het scherm aangaat**: zegt de meldingen die binnenkwamen terwijl het scherm uit was, elke 1 tot 30 minuten opnieuw, tot het scherm aangaat. Standaard **Uit**.

Zie [](#notifications-voice) voor stem-instellingen op dit scherm.

### Stem voor meldingen {#notifications-voice}

Onder **Meldingen** > **Meldingen voorlezen** > **Stem**:

- **Stem**: leest meldingen voor met een van je stemprofielen, met alle instellingen daarvan, zoals een andere spraakengine, tempo en toonhoogte. De standaardinstelling is **SandFly's stem**.
- **Stemprofielen**: opent de stemprofielen, waar je ze maakt en wijzigt. Zie [](#speech-voice-profiles).
- **Door de spraak van SandFly heen voorlezen met een andere stem**: zie hieronder. Standaard uit.
- **Taal detecteren**: zegt elke melding in de taal waarin die is geschreven. Vereist **Spraak** > **Automatisch van taal wisselen**. Zie [](#speech-languages). Standaard uit.
- **Geluid vóór meldingen**: speelt een geluid af vóór elke melding. Standaard uit.

Apps en regels kunnen een eigen stem hebben. Zie [](#notifications-apps) en [](#notifications-rules).

Hoe meldingen in verschillende stemmen worden voorgelezen:

- Een melding in het stemprofiel waarmee SandFly spreekt, wordt voorgelezen na de huidige spraak, in dezelfde spraakwachtrij.
- Een melding in een andere stem wordt ook voorgelezen na de huidige spraak, zodat niet twee stemmen tegelijk spreken. Deze wacht tot 45 seconden. Met **Door de spraak van SandFly heen voorlezen met een andere stem** aan wordt de melding meteen voorgelezen, over de huidige spraak heen.
- **Door de spraak van SandFly heen voorlezen met een andere stem** geldt niet terwijl **Alleen door de gebruiker verplaatste focus uitspreken** van toepassing is op de app op de voorgrond, of terwijl een assistent of een gesprek audio afspeelt. Zie [](#verbosity-own-speech).
- Terwijl **Stil tijdens het opnemen**, in **Spraak**, van toepassing is, wordt er geen melding voorgelezen. Zie [](#speech).
- Als meldingen voor verschillende stemmen samen binnenkomen, wacht elke op de melding ervoor.

Als de engine van een stemprofiel mislukt, leest SandFly de melding voor in zijn eigen stem.

### Apps {#notifications-apps}

**Meldingen** > **Apps** stelt in hoe de meldingen van elke app worden voorgelezen.

- **Andere apps**: hoe SandFly de meldingen voorleest van apps zonder eigen instellingen: **Lezen**, de standaardinstelling, **Alleen app-naam**, of **Niet lezen**.
- **Apps**: de apps in de launcher, de apps met instellingen, en de apps in de geschiedenis, op naam. Elke app toont zijn instellingen.

Voor één app:

- **Meldingen**: **Net als andere apps**, de standaardinstelling, **Lezen**, **Alleen app-naam**, **Lees eerst** of **Niet lezen**. **Alleen app-naam** zegt nooit wat de melding zegt. Meldingen met **Lees eerst** onderbreken een melding die wordt voorgelezen.
- **Gesproken naam**: de naam die SandFly zegt voor de app.
- **Tijd tussen de meldingen van een app**: de kortste tijd tussen twee meldingen van de app die worden voorgelezen. **Net als andere apps** gebruikt de instelling in **Herhalingen**.
- **Spraakformaat**: een spraakformaat voor de app. Zie [](#notifications-reading).
- **Stem**: **Net als andere apps**, **SandFly's stem**, of een stemprofiel, zoals een met een andere spraakengine.
- **Resetten**: verwijdert de instellingen van de app. Alleen getoond voor een app met instellingen.

### Woorden en vervangingen {#notifications-words}

**Meldingen** > **Woorden en vervangingen** heeft vier lijsten. Elk item kan een reguliere expressie zijn.

- **Alleen meldingen voorlezen met**: als de lijst woorden heeft, worden alleen meldingen voorgelezen die er een van bevatten.
- **Geen meldingen voorlezen met**: meldingen die een van deze woorden bevatten, worden niet voorgelezen.
- **Alleen de app-naam voorlezen bij meldingen met**: voor meldingen die een van deze woorden bevatten, wordt alleen de naam van de app gezegd.
- **Andere uitspraak**: tekst die SandFly anders uitspreekt, zoals "lol" als "lachen". Elke vervanging heeft **Tekst**, **Uitspreken als**, **Alleen hele woorden**, standaard aan, en **Reguliere expressie**.

Om een woord toe te voegen, selecteer je **Woord toevoegen** onder de bijbehorende lijst, voer je de tekst in en selecteer je **OK**. Om een vervanging toe te voegen, selecteer je **Vervanging toevoegen**. Selecteer een item om het te **Bewerken** of te **Verwijderen**.

### Soorten meldingen {#notifications-kinds}

**Meldingen** > **Soorten meldingen** laat soorten meldingen weg:

- **Mediaspelers overslaan**: standaard aan.
- **Lopende meldingen overslaan**: zoals navigatie en timers.
- **Meldingen van actieve apps overslaan**: zoals een VPN of een audiorecorder.
- **Voortgangsmeldingen overslaan**
- **Groepssamenvattingen overslaan**: zoals "3 nieuwe berichten".
- **Meldingen met lage prioriteit overslaan**
- **Systeemmeldingen overslaan**

Alle instellingen staan standaard uit, behalve **Mediaspelers overslaan**.

### Wanneer voorlezen {#notifications-when}

**Meldingen** > **Wanneer voorlezen** stelt de voorwaarden in waaronder meldingen worden voorgelezen:

- **Terwijl het scherm aanstaat**: standaard aan.
- **Terwijl het scherm uit is**: standaard uit.
- **Terwijl de telefoon is vergrendeld**: standaard aan.
- **Koptelefoon**: **Voorlezen met of zonder koptelefoon**, de standaardinstelling, **Alleen met koptelefoon**, of **Alleen zonder koptelefoon**.
- **Tijdens het afspelen van media**: **Lezen**, de standaardinstelling, **Media pauzeren tijdens voorlezen**, of **Niet lezen**.
- **Niet storen**: **Niets lezen**, **Lees meldingen toegestaan door de modus Niet storen**, de standaardinstelling, of **Lees alles**. **Lees meldingen toegestaan door de modus Niet storen** vereist toegang tot meldingen.
- **Terwijl het scherm wordt aangeraakt**: **Lees meteen**, **Voorlezen wanneer de aanraking eindigt**, de standaardinstelling, of **Niet lezen**. Met **Voorlezen wanneer de aanraking eindigt** worden meldingen die binnenkomen terwijl je het scherm aanraakt, een halve seconde na het einde van de aanraking voorgelezen, en uiterlijk 10 seconden nadat de eerste ervan binnenkwam.
- **Tijdens gesprekken**: leest meldingen voor tijdens gesprekken. Standaard uit.
- **Niet in stille modus**: standaard uit.
- **Niet in trilmodus**: standaard uit.
- **Niet in automodus**: zoals met Android Auto, dat meldingen zelf voorleest. Standaard uit.
- **Rustige uren**: er worden geen meldingen voorgelezen tussen **Begin** en **Einde**. Standaard uit; de standaardtijden zijn 22:00 en 07:00.

### Regels {#notifications-rules}

**Meldingen** > **Regels** verandert hoe meldingen in bepaalde omstandigheden worden voorgelezen. Een regel heeft voorwaarden en acties: als aan de voorwaarden is voldaan, zijn de acties van toepassing op de melding. Een regel zonder voorwaarden is van toepassing op elke melding. De stem van een regel neemt de plaats in van de stem van de app.

Een regel toevoegen:

1. Selecteer **Regel toevoegen**, voer een naam in en selecteer **OK**. Of selecteer **Voeg een regel toe vanuit een sjabloon** en kies een sjabloon.
2. Selecteer onder **Wanneer** de optie **Voorwaarde toevoegen** en kies een voorwaarde.
3. Selecteer onder **Dan** de optie **Actie toevoegen** en kies een actie.

Een regel heeft:

- **Naam**
- **Ingeschakeld**
- **Voorwaarden**: **Er moet aan alle voorwaarden zijn voldaan**, of **Er moet aan minstens één voorwaarde worden voldaan**.
- **Wanneer**: de voorwaarden. Selecteer een voorwaarde om die te **Bewerken**, **Keer de voorwaarde om** om die juist te laten gelden als er niet aan is voldaan, zoals "Het scherm is uitgeschakeld" in plaats van "Het scherm staat aan", of **Verwijderen**.
- **Dan**: de acties. Selecteer een actie om die te verwijderen.
- **Regel verwijderen**

De voorwaarden zijn **Bluetooth-apparaat**, een apparaat dat je kiest of **Elk apparaat**, **Bedrade hoofdtelefoon**, **Wifi-netwerk**, op naam, **Tijd van de dag**, met **Van**, **Tot** en **Dagen**, **Scherm aan**, **Telefoon vergrendeld**, **Melding van een app**, **App op de voorgrond**, **Melding bevat**, een tekst of reguliere expressie, **Batterijniveau lager dan**, een percentage, **Opladen**, **Liggend**, **Stille modus of trilmodus**, en **Media afspelen**. **Bluetooth-apparaat** vereist de toestemming Apparaten in de buurt op Android 12 en hoger, en **Wifi-netwerk** vereist de locatietoestemming; SandFly vraagt hier om als je de voorwaarde toevoegt.

De acties zijn: niet voorlezen, alleen de app-naam voorlezen, volledig voorlezen, ook als het privé is, een spraakformaat gebruiken, tot een aantal woorden voorlezen, zonder lengtelimiet voorlezen, emoji behouden, getallen bij elkaar houden, eerst voorlezen, ook voorlezen tijdens rustige uren en andere momenten waarop meldingen niet worden voorgelezen, en met een stemprofiel voorlezen.

De sjablonen zijn **Alleen met koptelefoon**, **Stil 's nachts** (22:00 tot 07:00), **Alleen als het scherm uit staat**, **Alleen de app-naam wanneer de telefoon is vergrendeld** en **Kort wanneer niet aan het opladen** (tot 15 woorden).

### Herhalingen {#notifications-repeats}

**Meldingen** > **Herhalingen** behandelt meldingen die opnieuw komen en apps die er veel plaatsen:

- **Dezelfde melding overslaan gedurende**: een melding met dezelfde tekst als een melding die binnen deze tijd is voorgelezen, wordt niet voorgelezen. De keuzes zijn **Uit**, de standaardinstelling, van 10 seconden tot 1 uur, en **Totdat SandFly opnieuw start**.
- **Getallen tellen als gelijk**: meldingen die alleen in getallen verschillen, zoals het percentage van een download, tellen als gelijk. Standaard uit.
- **Tijd tussen de meldingen van een app**: de kortste tijd tussen twee meldingen van een app die worden voorgelezen, van 5 seconden tot 15 minuten. Standaard **Uit**. Elke app kan zijn eigen instelling hebben. Zie [](#notifications-apps).
- **Herhaald begin weglaten**: laat een begin weg dat zich herhaalt in de meldingen van een app, zoals de naam van een groep in een chat. Standaard uit. **Begin onthouden gedurende** stelt in hoe lang SandFly het begin bewaart: 30 seconden tot 15 minuten, standaard 1 minuut.

SandFly leest een melding ook maar één keer voor als een app die binnen 5 seconden opnieuw plaatst met dezelfde titel en tekst, of dezelfde tekst als groepssamenvatting plaatst.

### Stop met lezen {#notifications-stop}

**Meldingen** > **Stop met lezen** stelt in wat het voorlezen van een melding stopt:

- **Schud om te stoppen**: schud de telefoon. **Intensiteit van schudden**: **Zacht**, **Gemiddeld**, de standaardinstelling, of **Sterk**.
- **Zwaaien om te stoppen**: beweeg een hand over de afstandssensor. **Zakmodus**, standaard aan, voorkomt dat een sensor die al bedekt is wanneer het voorlezen begint, zoals in een zak, het voorlezen stopt.
- **Beide volumetoetsen indrukken**
- **Stoppen bij sluiten**: stopt de melding als je die sluit. **Ook wanneer de melding op een smartwatch wordt afgewezen**, standaard aan, stopt de melding ook als je die afwijst op een verbonden smartwatch.
- **Stoppen wanneer de app de melding verwijdert**

Alle instellingen staan standaard uit, behalve **Zakmodus** en **Ook wanneer de melding op een smartwatch wordt afgewezen**. **Stoppen bij sluiten** en **Stoppen wanneer de app de melding verwijdert** vereisen toegang tot meldingen. Deze instellingen zijn ook van toepassing terwijl een samenvatting wordt voorgelezen.

### Samenvatting en tijd {#notifications-summary}

**Meldingen** > **Samenvatting en tijd** leest de meldingen in het meldingenpaneel voor, en zegt de tijd op een interval. Onder **Samenvatting**:

- **Toegang tot meldingen aanzetten**: samenvattingen vereisen dit. Alleen getoond zolang SandFly geen toegang tot meldingen heeft.
- **Meldingen nu voorlezen**: leest de meldingen in het meldingenpaneel voor. Vereist dat SandFly aanstaat.
- **Meldingen elke dag voorlezen**: leest ze elke dag voor op **Tijdstip**, standaard 08:00.
- **Nieuwste eerst**: leest de nieuwste melding eerst voor. Standaard uit.
- **Jouw naam**: een naam voor de begroeting aan het begin, zoals "Goedemorgen. Sam."
- **Pauze tussen meldingen**: van **Uit**, de standaardinstelling, tot 5 seconden.

Een samenvatting begint met een begroeting en het aantal meldingen, zoals "Je hebt 5 meldingen.", en leest elke melding voor als app, titel en tekst. Groepssamenvattingen en doorlopende meldingen worden weggelaten. **Apps**, **Woorden en vervangingen**, **Soorten meldingen** en **Regels** zijn van toepassing, en de meldingen die ze weglaten, worden niet meegeteld.

Onder **Tijdaankondigingen**:

- **Zeg de tijd elke**: **Uit**, de standaardinstelling, 15 minuten, 30 minuten, 1 uur of 3 uur.
- **Wekkers en herinneringen inschakelen**: opent de Android-instelling waarmee SandFly de tijd precies op tijd kan zeggen. Zonder deze instelling kan Android de aankondigingen uitstellen. Alleen getoond zolang **Zeg de tijd elke** niet op **Uit** staat en de instelling uit staat. SandFly opent deze Android-instelling ook als je een interval kiest voor **Zeg de tijd elke** of **Meldingen elke dag voorlezen** aanzet, tenzij die al aan staat.
- **Tijdnotatie**: wat SandFly zegt, met de tijdelijke aanduidingen `{time}`, `{date}` en `{battery}`, het batterijpercentage. De standaardinstelling is "Het is {time}".
- **Stem**: **Stem voor meldingen**, de standaardinstelling, **SandFly's stem**, of een stemprofiel.

Samenvattingen worden voorgelezen in de stem voor meldingen.

Om het batterijniveau op een interval te laten zeggen, zie **Uitgebreidheidsniveau** > **Batterijniveau** in [](#verbosity-announcements).

### Meldingsgeschiedenis {#notifications-history}

**Meldingen** > **Meldingsgeschiedenis** toont de laatste 100 meldingen die zijn binnengekomen sinds SandFly is gestart, met de tijd waarop elke melding binnenkwam, of die is voorgelezen, waarom niet, zoals "Niet voorgelezen: Niet storen", en wat SandFly heeft gezegd. De geschiedenis wordt alleen in het geheugen bewaard.

- **Meldingsgeschiedenis bewaren**: standaard aan.
- **Geschiedenis wissen**

Selecteer een melding voor deze acties, getoond met de naam van de app, zoals Chat:

- **Chat niet lezen**: zet de app op **Niet lezen**.
- **Lees alleen de naam van Chat**: zet de app op **Alleen app-naam**.
- **Lees eerst Chat**: zet de app op **Lees eerst**.
- **Lees dit soort meldingen niet**: voegt een reguliere expressie toe die overeenkomt met meldingen zoals deze, aan **Geen meldingen voorlezen met**.
- **Kopiëren**: kopieert de titel en tekst.

### Test de instellingen {#notifications-test}

**Meldingen** > **Stuur een testmelding** plaatst na 5 seconden een SandFly-testmelding, zodat je eerst het scherm kunt uitzetten of een andere app kunt openen. SandFly leest die voor met je meldingsinstellingen. SandFly vereist toestemming om meldingen te tonen.

### Automatisering {#notifications-automation}

Automatiseringsapps, zoals Tasker of MacroDroid, kunnen het voorlezen van meldingen bedienen met broadcasts naar SandFly. Zet eerst **Meldingen** > **Automatiseringsapps toestaan** aan; deze staat standaard uit, omdat elke app ze kan versturen. **Meldingen** > **Automatisering** toont de acties, elk met zijn broadcastactie: de pakketnaam van SandFly gevolgd door een actie zoals `.action.READ_NOTIFICATIONS`. Selecteer een actie om de broadcastactie te kopiëren:

- **Schakel het voorlezen van meldingen in**
- **Schakel het voorlezen van meldingen uit**
- **Schakel het voorlezen van meldingen in of uit**
- **Voorlezen van de melding stoppen**
- **Meldingen lezen**

### Meldingenlijst van SandFly {#notifications-list}

Met **Meldingen** > **Meldingenlijst van SandFly gebruiken** aan opent de opdracht **Meldingen** een SandFly-lijst in plaats van het systeemmeldingenpaneel. Op sommige telefoons, zoals Samsung-telefoons met veel meldingen, wordt het systeempaneel zo vaak opnieuw getekend dat erdoorheen bewegen traag is. Standaard uit, en vereist toegang tot meldingen.

In de lijst:

- Dubbeltik op een melding om die te openen.
- Een groep is één item. Dubbeltik erop om de meldingen ervan te tonen.
- Elke melding heeft zijn acties en, waar de app dit aanbiedt, een antwoordveld met **Verzenden**.
- Mediameldingen tonen **Wordt afgespeeld** of **Gepauzeerd**, met **Vorige**, **Afspelen** of **Onderbreken**, en **Volgende**.
- Een melding zonder tekst, zoals een weermelding met een eigen opmaak, wordt voorgelezen met de tekst van de opmaak, of anders met de subtekst, infotekst of samenvattingstekst.
- Tekst langer dan ongeveer 160 tekens wordt ingekort. De actie **Toon alles** leest de hele tekst voor, en **Minder tonen** kort die weer in.
- **Sluiten** verwijdert een melding, en **Alles wissen** verwijdert alle meldingen die verwijderd kunnen worden.
- **Snelle instellingen** opent de snelle instellingen.

Terwijl de telefoon is vergrendeld, opent de opdracht **Meldingen** het systeempaneel, dat verbergt wat je hebt gekozen om te verbergen op het vergrendelscherm. De opdracht **Meldingenpaneel van systeem** opent altijd het systeempaneel. De lijst van SandFly opent als een scherm op zichzelf, zodat de app op de voorgrond pauzeert. Sommige apps beëindigen wat ze doen als ze pauzeren: koppelen via draadloos debuggen in Instellingen mislukt als de koppelingscode wordt ingevoerd vanuit de lijst van SandFly. Gebruik daarvoor **Meldingenpaneel van systeem**.

Instellingen voor de lijst:

- **Meldingen nieuwste eerst weergeven, zonder groepen**: elke melding is een item op zichzelf, nieuwste eerst, voorgelezen als app, tijd, titel en tekst. Standaard uit.
- **Mediaspelers weglaten uit de meldingenlijst**: laat meldingen van mediaspelers weg. Het aantal meldingen dat wordt voorgelezen als het scherm aangaat, laat die ook weg. Standaard uit.
- **Meldingsdetails**: welke delen van een melding de lijst voorleest, en in welke volgorde: **Titel**, **Tekst**, **App-naam**, **Tijdstip** en **Afspelen of pauzeren**. Elk deel is een selectievakje, standaard allemaal aangevinkt, en heeft de acties **Omhoog verplaatsen** en **Omlaag verplaatsen**.

### Meldingen die Android niet aankondigt {#notifications-not-announced}

Android meldt schermlezers alleen over een nieuwe melding als die een melding laat afgaan. Een melding die Niet storen tegenhoudt, of een update die een app plaatst zonder opnieuw te laten afgaan, zoals een deurbel-app die zijn laatste bewegingsmelding vervangt, wordt niet aangekondigd. Als SandFly toegang tot meldingen heeft, ontvangt het deze meldingen ook, en leest ze voor zoals de meldingsinstellingen zeggen, zodat **Wanneer voorlezen** > **Niet storen** ook daarop van toepassing is. Voortgangsmeldingen en lopende meldingen worden weggelaten.

Terwijl de telefoon is vergrendeld, geeft Android schermlezers alleen de openbare versie van een privémelding, die veel apps niet hebben. Met toegang tot meldingen leest SandFly in plaats daarvan de hele melding voor, zoals de meldingsinstellingen zeggen, zoals **Voorlezen van privémeldingen** en **Alleen de app-naam wanneer de telefoon is vergrendeld**.

### Ander gedrag bij meldingen {#notifications-other}

- Toasts, de korte berichten die apps onderaan het scherm tonen, worden voorgelezen terwijl Niet storen aanstaat. De instellingen van Niet storen zijn alleen van toepassing op meldingen.
- Op het vergrendelscherm en in het meldingenpaneel zegt SandFly niet "collapsed" voor elke melding. Het zegt "expanded" als je een melding uitvouwt.
- Op telefoons die het paneel van het meldingenpaneel tonen als er een pop-upmelding binnenkomt, zoals Vivo-telefoons, zegt SandFly de naam van het paneel niet binnen 3 seconden nadat een melding binnenkomt, tenzij je daarna het scherm hebt aangeraakt, op een toets hebt gedrukt, een brailleleesregel hebt gebruikt of een spraakopdracht hebt gegeven.
- Je kunt mededelingen van SandFly, zoals over een update of gewijzigde gebaren, ook op het vergrendelscherm sluiten. De meldingen voor het pauzeren van SandFly en voor het downloaden van een model op het apparaat blijven staan terwijl die doorgaan.
- Op Samsung-smartwatches leest SandFly een meldingskaart voor als app en titel, dan het bericht, dan de tijd, ook als het label van de kaart het bericht weglaat. Zie [](#devices).
- Zie [](#translation) om meldingen te laten vertalen.
