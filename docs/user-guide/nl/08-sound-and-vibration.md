## Geluid, trilling en audio-uitvoer {#sound}

SandFly speelt een geluid en een trilling af voor acties en gebeurtenissen, zoals focus verplaatsen, een item activeren, scrollen en een venster openen. **Geluid en trillen** in de instellingen van SandFly zet geluiden en trillingen aan en uit, bepaalt hun volume en sterkte, en bevat geluidsthema's en bedieningsgeluiden. **Audio-uitvoer** bepaalt waar spraak en geluiden worden afgespeeld, op welk volume, en wat er gebeurt met andere audio en met gesprekken terwijl SandFly aan staat.

Voor de spraakengine, stemmen, spreeksnelheid, spraakvolume en de stemeffecten, zie [](#speech). Voor de volumetoetsen en **Feedback bij volumewijziging**, zie [](#buttons).

### Zet geluiden aan of uit {#sound-feedback}

De groep **Geluiden** van **Geluid en trillen** heeft deze instellingen:

- **Geluidsfeedback**: speelt de SandFly-geluiden af. Standaard aan. Met dit uit, spelen trillingen nog steeds.
- **Volume geluidsfeedback**: het volume van de geluiden, van 5 tot 100 procent in stappen van 5. De standaardwaarde is 50 procent.
- **Geluidsthema's**: opent de geïnstalleerde geluidsthema's, bedieningsgeluiden en 3D-audio. Zie [](#sound-themes).
- **Geluid voor Acties beschikbaar**: wanneer het geluid **Acties beschikbaar** speelt: **Uit**, **Als het item focus krijgt**, de standaardwaarde, of **Nadat het item is uitgesproken**. Zie [](#sound-actions-available).

De sneltoets **Geluidsfeedback (earcons) aan- of uitzetten** zet **Geluidsfeedback** aan of uit. Zie [](#appendix-keyboard-shortcuts).

Om een los geluid uit te zetten en de rest te houden, zie [](#sound-individual).

### Verander trilling {#sound-vibration}

De groep **Trillen** van **Geluid en trillen** heeft deze instellingen. Op apparaten zonder trilmotor wordt de groep niet getoond.

- **Trilfeedback**: speelt de SandFly-trillingen af. Standaard aan. Met dit uit, spelen geluiden nog steeds.
- **Trillingssterkte**: hoe sterk trillingen zijn, van 5 tot 200 procent. De standaardwaarde is 100 procent. De instelling wordt alleen getoond op apparaten waarvan de trilmotor de sterkte kan variëren.
- **Type trilling**: hoe trillingen worden afgespeeld. De lijst toont alleen de typen die de trilmotor kan afspelen, en de instelling is verborgen wanneer er maar één type beschikbaar is. Tot je een type kiest, gebruikt SandFly het eerste in de lijst.
  - **Haptische effecten**: samengestelde effecten zoals klikken en tikken, op Android 11 en later met een trilmotor die ze ondersteunt.
  - **Pulsen**: trillingen met wisselende sterkte, op trilmotoren die hun sterkte kunnen variëren.
  - **Aan en uit**: de trilmotor gaat aan en uit op volle sterkte. Elke trilmotor kan dit type afspelen.

Als trillingen zwak aanvoelen met **Haptische effecten**, kies je **Pulsen** en verhoog je **Trillingssterkte**.

De opdracht **Trilfeedback**, in de groep **Spraak** van de opdrachten, zet **Trilfeedback** aan of uit. Zie [](#commands).

### Geluiden en trillingen {#sound-events}

Elk geluid heeft een eigen trilling, die met het geluid afspeelt en ook afspeelt met **Geluidsfeedback** uit. Je kunt alle geluiden uitzetten en acties nog steeds aan hun trilling onderscheiden. Een paar trillingen, zoals de aankondigingstrilling en de trillingen van het brailletoetsenbord, spelen zonder geluid.

De geluiden, bij de namen die worden getoond in **Afzonderlijke geluiden en trillingen** en in de geluidslijst van een thema:

| Geluid | Speelt bij |
| --- | --- |
| **Focus op een item** | Focus verplaatst naar een item. |
| **Focus op een activeerbaar item** | Focus verplaatst naar een item dat je kunt activeren. |
| **Acties beschikbaar** | Focus verplaatst naar een item met acties van de app, zoals Beantwoorden bij een chatbericht. Zie [](#sound-actions-available). |
| **Een leeg gebied aanraken** | Je vinger raakt een deel van het scherm zonder item aan. |
| **Klik** | Een item wordt geactiveerd. |
| **Lang indrukken** | Een item wordt aangeraakt en vastgehouden. |
| **Scrollen** | Een lijst scrolt. |
| **Een lijst binnengaan** en **Een lijst verlaten** | Focus verplaatst een lijst in of uit. |
| **Actie voltooid of einde bereikt** | Een actie wordt voltooid, of focus bereikt het einde van een lijst. |
| **Vensterwijziging** | Een nieuw venster of scherm opent. |
| **Volgende leesinstelling** en **Vorige leesinstelling** | Een andere leesinstelling wordt gekozen. |
| **Randgebaar onderaan**, **Randgebaar midden** en **Randgebaar bovenaan** | De fasen van een randgebaar. Niet op horloges. |
| **Invoegpunt verplaatst** | Verplaatsen per teken, woord, regel of alinea verplaatst de cursor in een tekstveld dat je bewerkt. |
| **Naar volgende rij** en **Naar vorige rij** | Een swipe verplaatst focus naar een item op een rij onder of boven de huidige. |
| **Agenttaak gestart**, **Agenttaak voltooid**, **Vraag van agent**, **Agent gestopt bij gevoelige actie** en **Agenttaak mislukt** | De SandFly Pro-agent. Alleen in builds met SandFly Pro. Zie [](#pro). |
| **Spelfout** | Een verkeerd gespeld woord. |
| **Link** | Een link in tekst. |
| **Opgemaakte tekst** | Opgemaakte tekst, zoals vet. |
| **Emoji** | Na een emoji, met **Uitgebreidheidsniveau** > **Na emojinamen** ingesteld op **Een geluid afspelen**. Geen ingebouwde trilling; een geluidsthema kan het er een geven. Zie [](#verbosity-emoji). |
| **Scherm uit** en **Scherm aan** | Het scherm gaat uit of aan. |
| **Scherm verborgen** en **Scherm weergegeven** | Het scherm is verborgen, of weer getoond. Zie [](#display). |
| **Laden** | Een beschrijving of ander resultaat wordt geladen. |
| **AI-model op het apparaat wordt geladen** | Het AI-model op het apparaat wordt geladen. |
| **Browsemodus aan** en **Browsemodus uit** | Browsemodus gaat aan of uit. Zie [](#web). |
| **Toetsenbord geopend** en **Toetsenbord gesloten** | Een schermtoetsenbord opent of sluit. |
| **SandFly-melding** | SandFly plaatst een melding op een meldingskanaal zonder geluid. |
| **Volledig opgeladen** | De batterij is volledig opgeladen, met **Volledig opgeladen batterij melden** aan. Zie [](#verbosity-announcements). |
| **Cirkelmenu** | De items van het cirkelmenu, acht noten. |
| **Brailleleesregel verbonden**, **Brailleleesregel losgekoppeld**, **Braille-opdracht mislukt**, **Automatisch braille scrollen aan**, **Automatisch braille scrollen uit** en **Brailletoetsenbord gekalibreerd** | Brailleleesregels en het brailletoetsenbord. Zie [](#braille). |
| **Bedieningselementgeluid: knop** tot **Bedieningselementgeluid: boomitem** | Bedieningsgeluiden, één voor elk type bedieningselement. Zie [](#sound-control-sounds). |

#### Navigatiegeluiden {#sound-navigation-sounds}

SandFly speelt geen geluid wanneer een gebaar begint of eindigt. Deze geluiden geven in plaats daarvan feedback tijdens navigatie:

- **Volgende leesinstelling** en **Vorige leesinstelling** spelen wanneer je met een gebaar, het cirkelmenu, een sneltoets of het brailletoetsenbord een andere leesinstelling kiest.
- De randgebaargeluiden spelen wanneer een vinger een rand aanraakt waar een randgebaar begint (**Randgebaar onderaan**, een lage tik), wanneer het gebaar zijn eerste fase bereikt (**Randgebaar midden**), en wanneer het de tweede fase bereikt (**Randgebaar bovenaan**). Teruggleiden speelt het geluid van de fase waar de vinger naar teruggaat. Het optillen van de vinger speelt niets. Voor randgebaren, zie [](#gestures).
- **Naar volgende rij** speelt wanneer een swipe vooruit focus verplaatst naar een item op een rij onder het item met focus, en **Naar vorige rij** wanneer een swipe terug focus verplaatst naar een item op een rij erboven. Elk lijstitem is een eigen rij. Items naast elkaar, zoals pictogrammen op een startscherm of cellen van een tabel, delen een rij. Het geluid speelt met het focusgeluid, en zijn trilling vervangt de focustrilling. Het speelt niet wanneer focus naar een ander venster verplaatst.

#### Geluid voor Acties beschikbaar {#sound-actions-available}

Het geluid **Acties beschikbaar** speelt voor een item met acties van de app naast de standaardacties, zoals Beantwoorden en Doorsturen bij een chatbericht in een berichten-app. Activeren, lang indrukken, scrollen, uitvouwen en andere standaardacties tellen niet mee, dus een gewone knop speelt geen geluid **Acties beschikbaar**. Toetsen van schermtoetsenborden tellen ook niet mee. De acties staan in de leesinstelling **Acties** en in het SandFly-menu.

Het geluid heeft geen ingebouwde trilling; een geluidsthema kan het er een geven.

**Geluid en trillen** > **Geluid voor Acties beschikbaar** stelt in wanneer het geluid speelt:

- **Uit**: het geluid speelt niet.
- **Als het item focus krijgt**: na het focusgeluid. De standaardwaarde. Een trilling die het thema het geluid geeft, speelt in plaats van de focustrilling.
- **Nadat het item is uitgesproken**: zodra de beschrijving van het item tot het einde is uitgesproken. Als je eerder verder gaat, speelt het niet.

Om SandFly "Acties beschikbaar" te laten zeggen na zulke items, zie [](#verbosity-items).

#### Tikjes tijdens scrollen {#sound-scroll-ticks}

Terwijl je met twee vingers een lijst scrolt, speelt SandFly een licht tikje voor elk item dat voorbijscrolt, zodat een trage sleepbeweging langzaam tikt en een snelle sleepbeweging snel tikt. In lijsten die hun items niet doorgeven, speelt een tikje voor elke 48 dp scrollen. Het eerste tikje speelt zodra de sleepbeweging de app bereikt. Het scrollgeluid speelt met de tikjes, hoogstens elke 250 ms, en zijn toonhoogte stijgt van de top van de lijst naar de onderkant. Scrolls die SandFly zelf maakt, spelen alleen het scrollgeluid.

### Zet losse geluiden en trillingen uit {#sound-individual}

**Geluid en trillen** > **Afzonderlijke geluiden en trillingen** heeft een schakelaar voor elk geluid, onder **Geluiden**, en voor elke trilling, onder **Trillingen**. Alle schakelaars staan standaard aan. Het uitzetten van een geluid houdt de trilling, en het uitzetten van een trilling houdt het geluid. **Geluidsfeedback** en **Trilfeedback** zetten nog steeds alle geluiden of alle trillingen in één keer uit.

Om een geluid of trilling af te spelen voordat je de schakelaar wijzigt:

1. Verplaats focus naar de schakelaar.
2. Open het actiemenu.
3. Kies **Beluisteren**.

Het wijzigen van een schakelaar speelt niets af.

De lijst **Trillingen** heeft ook schakelaars voor trillingen zonder geluid: **Melding**, **Randgebaar: terugschuiven**, en de trillingen van het brailletoetsenbord, zoals **Brailletoetsenbord: een teken typen** en **Brailletoetsenbord: niets te verwijderen**. De schakelaar van een trilling zet ook de trilling uit die een geluidsthema erin plaatst.

### Geluidsthema's {#sound-themes}

Een geluidsthema is een set geluiden en trillingen die de ingebouwde vervangen. Elk thema heeft ook eigen instellingen voor **Geluiden en trillingen van bedieningselementen**, **3D-audio** en **Toch het type bedieningselement uitspreken**. De ingebouwde geluiden zijn het thema **SandFly**, dat niet kan worden verwijderd.

**Geluid en trillen** > **Geluidsthema's** heeft deze delen:

- **Nieuw thema maken**: maakt een nieuw thema. Zie [](#sound-themes-create).
- **Geïnstalleerde thema's**: elk geïnstalleerd thema. Het thema dat in gebruik is toont "In gebruik" in zijn samenvatting.
- **Instellingen van** en de naam van het thema dat in gebruik is: de instellingen voor bedieningsgeluiden en de geluidslijst van het thema. Zie [](#sound-control-sounds) en [](#sound-themes-replace).
- **Thema installeren**: **Uit een bestand**, **Van een koppeling** en **Van een bron**. Zie [](#sound-themes-install).

#### Installeer een geluidsthema {#sound-themes-install}

Een themabestand is een ZIP-bestand. Om een thema te installeren, gebruik je een van de volgende:

- **Uit een bestand**: kies een themabestand met de systeembestandskiezer.
- **Van een koppeling**: voer een koppeling naar een themabestand in of plak deze, of naar een GitHub-repository met een thema erin, en kies **Installeren**. Koppelingen moeten beginnen met `https`.
- **Van een bron**: een lijst van repositories op GitHub, GitLab, Codeberg, Gitea of Bitbucket. Kies **Bron toevoegen** en voer het adres van een repository in, zoals `github.com/owner/repository`. Kies een bron om zijn themabestanden te tonen, en kies een themabestand om het te installeren. De lijst begint met de SandFly-themarepository. **Bron verwijderen** verwijdert een bron uit de lijst; thema's die daaruit zijn geïnstalleerd, blijven geïnstalleerd.
- Open een themabestand met SandFly vanuit een bestandsbeheerder of de downloads van een browser, of deel een themabestand of koppeling met SandFly vanuit een andere app, en kies **Geluidsthema installeren**.

Voordat het wordt geïnstalleerd, toont SandFly de auteur, de beschrijving, het aantal geluiden en trillingen, de licentie en bestanden die zijn weggelaten. Kies **Installeren en gebruiken** om het thema te installeren en te gebruiken, of **Installeren** om het te installeren en het huidige thema te houden. Een thema met dezelfde naam als een geïnstalleerd thema vervangt dit.

Opmerking: Horloges hebben geen bestandskiezer. Installeer thema's op een horloge via een koppeling. **Van een bron** is alleen beschikbaar op telefoons.

#### Kies en beheer thema's {#sound-themes-manage}

1. Ga naar **Geluid en trillen** > **Geluidsthema's**.
2. Kies onder **Geïnstalleerde thema's** een thema.
3. Kies een van de volgende:
   - **Dit thema gebruiken**: wisselt naar het thema, met zijn eigen instellingen voor bedieningsgeluiden.
   - **Over dit thema**: toont de auteur, de beschrijving, het aantal geluiden en trillingen, de licentie en de website.
   - **Opslaan als themabestand**: slaat het thema op als een ZIP-bestand dat je kunt delen.
   - **Verwijderen**: verwijdert het thema. Niet beschikbaar voor het thema **SandFly**.

#### Vervang losse geluiden {#sound-themes-replace}

Je kunt elk geluid van het thema dat in gebruik is vervangen door een geluidsbestand. Om geluiden te vervangen zonder een thema te installeren, gebruik je het thema **SandFly**.

1. Ga naar **Geluid en trillen** > **Geluidsthema's**.
2. Kies onder de instellingen van het thema dat in gebruik is **Geluiden**.
3. Kies een geluid. De titel van het dialoogvenster toont het geluid en zijn bestandsnaam, zoals "Klik (tick)".
4. Kies een van de volgende:
   - **Een geluidsbestand kiezen**: opent de bestandskiezer. Een geluid kan tot 5 MB groot zijn, in WAV-, OGG-, MP3- of FLAC-formaat.
   - **Kiezen uit map**: toont de geluidsbestanden en submappen van een map. De eerste keer opent de mapkiezer om de map te kiezen. Zie [](#sound-themes-folder).
   - **Andere map**: opent de mapkiezer om een andere map te kiezen voor **Kiezen uit map**. Getoond nadat een map is gekozen.
   - **Beluisteren**: speelt het geluid af.
   - **Geen geluid**: speelt geen geluid af voor de gebeurtenis.
   - **Geluid van SandFly gebruiken**: gaat terug naar het ingebouwde geluid.

De samenvatting van elk geluid toont **Geluid van SandFly**, **Themageluid** of **Geen geluid**. Dezelfde acties staan in het actiemenu van elk geluid, als **Beluisteren**, **Instellen op geen geluid**, **Geluid van SandFly gebruiken** en **Een geluidsbestand kiezen**. **De geluiden van SandFly voor alles gebruiken** verwijdert elk geluid van het thema en gebruikt de ingebouwde geluiden.

De geluidslijst heeft ook geluiden die **Afzonderlijke geluiden en trillingen** niet heeft: **Volumewijziging**, dat speelt voor elke volumestap wanneer **Feedback bij volumewijziging** is ingesteld op een toon (zie [](#buttons)), en de typegeluiden van het brailletoetsenbord, die spelen met **Typegeluiden** aan in de instellingen van het brailletoetsenbord (zie [](#braille)).

#### Kies geluiden uit een map {#sound-themes-folder}

**Kiezen uit map** toont één map op de telefoon. Kies een geluid, en dan **Kiezen uit map**. Kies de eerste keer de map in de mapkiezer.

De lijst toont eerst submappen, dan geluidsbestanden, elk gesorteerd op naam. Per map worden maximaal 2.000 items getoond.

- Het bestand met toegankelijkheidsfocus speelt af. Als je de focus van het bestand af verplaatst, stopt het.
- Kies een bestand om het voor het geluid te gebruiken.
- Kies een submap om de inhoud te tonen. **Bovenliggende map** of Terug gaat terug naar de map erboven.

Als de map is verwijderd of SandFly de map niet meer kan lezen, opent de mapkiezer opnieuw.

#### Maak een geluidsthema {#sound-themes-create}

1. Ga naar **Geluid en trillen** > **Geluidsthema's**.
2. Kies **Nieuw thema maken**.
3. Voer een **Naam** in. Een naam moet verschillen van de namen van geïnstalleerde thema's. **Beschrijving**, **Auteur** en **Website** zijn optioneel.
4. Kies **OK**. Het thema opent in een lijst van zijn geluiden.
5. Stel **Geluiden en trillingen van bedieningselementen**, **3D-audio** en **Toch het type bedieningselement uitspreken** in, en kies een geluidsbestand voor elk geluid dat je wilt vervangen.
6. Kies **Opslaan** om het thema op te slaan en te gebruiken, of **Opslaan als themabestand** om het op te slaan als ZIP-bestand zonder het te gebruiken.

Om te stoppen zonder op te slaan, ga je terug en kies je **Verwijderen**. **Doorgaan met bewerken** gaat terug naar het thema. Als je **Geluiden en trillingen van bedieningselementen** ongewijzigd laat, wordt dit aangezet wanneer het opgeslagen thema bedieningsgeluiden heeft.

#### Themabestanden {#sound-themes-files}

Een themabestand is een ZIP-bestand met een `theme.json`-bestand en de geluidsbestanden, in dezelfde map. De map kan ook in één map van het ZIP-bestand staan, zoals in een ZIP-bestand dat GitHub van een repository maakt, dus een GitHub-repository met `theme.json` en de geluiden boven in is een thema. Een ZIP-bestand met geluiden zonder `theme.json` wordt ook geïnstalleerd, met de naam van het bestand.

- Noem elk geluidsbestand naar het geluid dat het vervangt, met de extensie van zijn formaat, zoals `focus.wav` of `tick.ogg`. Het dialoogvenster van elk geluid in **Geluiden** toont zijn bestandsnaam. Een thema heeft alleen de geluiden nodig die het verandert.
- WAV-, OGG-, MP3-, FLAC-, M4A-, AAC- en Opus-bestanden werken, tot 5 MB elk en 50 MB voor het thema. Een bestand dat Android niet kan afspelen, wordt bij installatie weggelaten.
- `theme.json` kan de naam, auteur, beschrijving, licentie en website van het thema instellen, zijn instellingen voor bedieningsgeluiden, geluiden die op geen geluid zijn gezet, en trillingen. Een trilling kan tot drie vormen hebben, voor de drie trillingstypen.
- Licentie- en leesmij-bestanden, zoals `LICENSE.txt` of `README.md`, worden bij het thema gehouden. Andere bestanden worden weggelaten.

Voor het volledige bestandsformaat, met elk geluid en elke trillingsnaam, zie [Sound themes](https://github.com/aaron-gh/SandFly/blob/master/themes.md).

### Geluiden en trillingen van bedieningselementen {#sound-control-sounds}

Met bedieningsgeluiden aan, speelt focus op een bedieningselement een geluid en trilling voor zijn type bedieningselement in plaats van het focusgeluid en de focustrilling, en zegt SandFly het type bedieningselement niet, zoals "knop". Er zijn geluiden voor knoppen, keuzevakjes (ook schakelaars en keuzeknoppen), keuzerondjes, bewerkbare velden, keuzelijsten, schuifregelaars, afbeeldingen, klokken, tabbladen, menu-items en lijstitems, en op webpagina's, links en boomstructuuritems.

Om bedieningsgeluiden aan te zetten:

1. Ga naar **Geluid en trillen** > **Geluidsthema's**.
2. Zet onder de instellingen van het thema dat in gebruik is **Geluiden en trillingen van bedieningselementen** aan. Standaard uit.

De instellingen onder het thema dat in gebruik is:

- **Geluiden en trillingen van bedieningselementen**: speelt bedieningsgeluiden en -trillingen af.
- **3D-audio**: wanneer bedieningsgeluiden spelen vanaf de positie van het bedieningselement. Zie [](#sound-3d-audio).
- **Toch het type bedieningselement uitspreken**: zegt het type bedieningselement na zijn geluid. Standaard aan voor het thema **SandFly**, uit voor andere thema's tenzij het thema dit instelt.

Een item dat zelf geen bedieningselement is, zoals een rij in Instellingen, speelt het geluid van het eerste bedieningselement erin, zoals zijn schakelaar, en anders het geluid van zijn type item, zoals een lijstitem. Toetsen van het schermtoetsenbord houden het focusgeluid. Een trilling van een bedieningselement speelt met **Geluidsfeedback** uit, zodat je bedieningselementen aan hun trilling kunt onderscheiden. Een type bedieningselement waarvan het geluid is uitgezet in **Afzonderlijke geluiden en trillingen**, of door het thema op geen geluid is gezet, speelt het focusgeluid, en SandFly zegt het type bedieningselement.

Elk thema houdt zijn eigen instellingen voor bedieningsgeluiden. Wisselen van thema wisselt deze mee.

#### 3D-audio {#sound-3d-audio}

Bedieningsgeluiden spelen vanaf de positie van het bedieningselement op het scherm: van links voor een bedieningselement aan de linkerkant, en van hogerop voor een bedieningselement bovenin. Met 3D-audio gebruiken de geluiden metingen van hoe een hoofd geluid uit elke richting ontvangt. Zonder dit bewegen ze alleen tussen links en rechts.

**3D-audio** heeft deze opties:

- **Met koptelefoon**: de standaardwaarde. 3D-audio speelt wanneer SandFly-audio naar een bedrade of USB-koptelefoon, een Bluetooth-headset of -koptelefoon, of hoorapparaten gaat. Op Android 13 en later controleert SandFly waar zijn audio speelt, dus een verbonden Bluetooth-luidspreker of auto die geen SandFly-audio afspeelt, telt niet mee. Op eerdere versies telt elke verbonden koptelefoon mee.
- **Altijd**: 3D-audio ook op de telefoonluidspreker.
- **Nooit, alleen links en rechts**: de geluiden bewegen alleen tussen links en rechts.
- **Met koptelefoon, anders in het midden**: 3D-audio met koptelefoon, zoals **Met koptelefoon**. Op de telefoonluidspreker en andere uitvoer spelen de geluiden vanuit het midden, zonder tussen links en rechts te bewegen.

### Instellingen voor audio-uitvoer {#sound-audio-output}

**Audio-uitvoer** in de instellingen van SandFly heeft deze instellingen:

- **Audio-uitvoerapparaat**: waar spraak en geluiden worden afgespeeld. Zie [](#sound-output-device).
- **Telefoonluidspreker bij deze apparaten**: uitvoer die SandFly-audio naar de telefoonluidspreker verplaatst terwijl verbonden. Zie [](#sound-speaker-devices).
- **Luidspreker wanneer de telefoon niet bij je oor is**: wisselt gesprekken tussen de oorspeaker en de luidspreker. Zie [](#sound-calls).
- **Spraak op de telefoon houden tijdens casten**: zie [](#sound-casting).
- **Toegankelijkheidsaudio resetten na gesprekken**: zie [](#sound-calls).
- **Volumeregeling voor spraak**: het volume dat spraak en geluiden gebruiken. Zie [](#sound-speech-volume).
- **Audiovolume verlagen**: zie [](#sound-ducking).
- **Audio met lage latentie**: zie [](#sound-low-latency).

### Kies het audio-uitvoerapparaat {#sound-output-device}

**Audio-uitvoer** > **Audio-uitvoerapparaat** bepaalt waar SandFly-spraak en -geluiden worden afgespeeld:

- **Systeemstandaard**: de standaardwaarde. Android kiest de uitvoer.
- **Telefoonluidspreker**: de telefoonluidspreker, ook terwijl Bluetooth- of bedrade koptelefoons zijn verbonden.
- Een verbonden apparaat op naam, zoals een Bluetooth-, bedraad, USB- of hoorapparaat. De lijst toont de nu verbonden apparaten.

Een Bluetooth-apparaat dat je kiest, ontvangt audio via de routering van Android, net als bij **Systeemstandaard**, zodat het apparaat niet naar gespreksmodus wisselt. Als Android audio naar een ander apparaat routeert terwijl het gekozen Bluetooth-apparaat is verbonden, blijft de audio op het andere apparaat. Bedrade, USB- en hoorapparaten ontvangen audio direct.

De keuze blijft wanneer apparaten verbinden of de verbinding verbreken en wanneer andere apps audio beginnen af te spelen. Terwijl het gekozen apparaat niet is verbonden, toont de instelling zijn naam met "(niet verbonden)", en volgt audio de routering van Android tot het apparaat weer verbindt. Tijdens telefoongesprekken, en terwijl een andere app de microfoon gebruikt voor een gesprek of spraakassistent, kiest Android de uitvoer.

De leesinstelling **Audio-uitvoerapparaat** verandert de instelling met een swipe omhoog of omlaag. Zie [](#sound-reading-controls).

### Speel op de telefoonluidspreker met sommige apparaten {#sound-speaker-devices}

**Audio-uitvoer** > **Telefoonluidspreker bij deze apparaten** toont de nu verbonden Bluetooth-, bedrade en USB-uitvoer, en de uitvoer die eerder was verbonden terwijl SandFly aan stond. Terwijl een apparaat dat je aanvinkt is verbonden, speelt SandFly spraak en geluiden op de telefoonluidspreker, wat **Audio-uitvoerapparaat** ook is ingesteld. Vink bijvoorbeeld een Bluetooth-luidspreker aan om spraak op de telefoon te hebben terwijl muziek op de luidspreker speelt, en laat je oordopjes niet aangevinkt om spraak erin te hebben.

Apparaten worden op naam vermeld, dus twee apparaten met dezelfde naam zijn één keuze. Tot een apparaat is verbonden, toont de instelling "Nog geen Bluetooth- of bedrade apparaten verbonden".

### Kies het volume voor spraak {#sound-speech-volume}

**Audio-uitvoer** > **Volumeregeling voor spraak** bepaalt het volume waarop spraak en SandFly-geluiden worden afgespeeld:

- **Toegankelijkheidsvolume**: de standaardwaarde.
- **Mediavolume**.
- **Belvolume**: spraak speelt als een beltoon. Dit blijft op de telefoon terwijl media naar een ander apparaat wordt gecast, kan op de telefoonluidspreker spelen terwijl een koptelefoon is verbonden, en is stil terwijl de beltoon stil staat.
- **Gespreksvolume, oorspeaker**: spraak speelt als een gesprek, op de oorspeaker wanneer er geen headset is verbonden.

Terwijl **Audio-uitvoerapparaat** audio naar de telefoonluidspreker stuurt, of naar een bedraad, USB- of hoorapparaat, gebruikt spraak het toegankelijkheidsvolume.

Terwijl spraak het toegankelijkheidsvolume niet gebruikt, speelt Android de toegankelijkheidsgeluiden van andere apps, zoals de tonen van spraakinvoer, op het mediavolume.

De leesinstelling **Spraakvolume** wisselt tussen deze volumes, en de leesinstelling **Toegankelijkheidsvolume** verandert het toegankelijkheidsvolume. Zie [](#sound-reading-controls).

### Verlaag ander geluid terwijl SandFly spreekt {#sound-ducking}

**Audio-uitvoer** > **Audiovolume verlagen** verlaagt het volume van andere audio, zoals muziek, terwijl SandFly spreekt. Standaard aan. De leesinstelling **Audiovolume verlagen** en de opdracht **Audiovolume verlagen**, in de groep **Spraak** van de opdrachten, zetten dit aan en uit.

### Audio met lage latentie {#sound-low-latency}

**Audio-uitvoer** > **Audio met lage latentie** speelt SandFly-geluiden en -spraak af via het snelste audiopad van het apparaat, zodat ze sneller beginnen. Standaard aan op telefoons en tablets en standaard uit op horloges. Als spraak of geluiden stotteren of verkeerd klinken, zet je dit uit.

- Spraak gebruikt het snelle pad op Android 11 en later. Op eerdere versies gebruiken alleen geluiden dit.
- Spraak speelt op de gewone manier tijdens gesprekken, kort na het begin of einde van een gesprek, terwijl een app met de microfoon opneemt, en met spraakengines die hun eigen audio afspelen.
- Spraak midden in een woord hervatten na een pauze vereist **Audio met lage latentie**. Zie [](#speech-pause). Met dit uit, werken de stemeffecten alleen waar het apparaat ze als audio-effect ondersteunt. Zie [](#voice-effects).

### Houd spraak op de telefoon tijdens casten {#sound-casting}

**Audio-uitvoer** > **Spraak op de telefoon houden tijdens casten** stopt het casten en spiegelen van het scherm om SandFly-spraak en -geluiden naar de tv of luidspreker te sturen. Schermopnames hebben dan ook geen SandFly-audio. Standaard uit. Vereist Android 10 of later.

### Gesprekken {#sound-calls}

**Audio-uitvoer** heeft twee instellingen voor gesprekken.

**Luidspreker wanneer de telefoon niet bij je oor is** wisselt een gesprek naar de luidspreker wanneer je de telefoon van je oor weghaalt, en terug naar de oorspeaker wanneer je hem weer tegen je oor houdt. Standaard uit en vereist Android 12 of later. Dit werkt alleen terwijl SandFly aan staat en niet gepauzeerd is, en verplaatst het gesprek alleen tussen de oorspeaker en de luidspreker: Bluetooth- en bedrade headsets blijven in gebruik. Een gesprek dat begint met de telefoon weg van je oor gaat naar de luidspreker.

De instelling heeft een rechtenmachtiging nodig die Android niet aan schermlezers geeft. Tot de machtiging is verleend, toont de samenvatting de opdracht die deze verleent. Om de machtiging te verlenen, doe je een van de volgende:

- Met Shizuku actief, zet je de instelling aan. SandFly verleent de machtiging via Shizuku. Zie [](#updates).
- Voer de opdracht uit met adb. Om de opdracht te kopiëren, open je het actiemenu op de instelling en kies je **Opdracht kopiëren**. Ga daarna terug naar **Audio-uitvoer**. Zonder Shizuku is de schakelaar niet beschikbaar tot de machtiging is verleend.

SandFly gebruikt de afstandssensor om te detecteren dat de telefoon bij je oor is.

**Toegankelijkheidsaudio resetten na gesprekken** verplaatst SandFly-audio terug van de oorspeaker wanneer Android toegankelijkheidsaudio op de oorspeaker laat staan na een gesprek, zoals een gesprek in een berichtenapp. Standaard uit, vereist Android 13 of later, en werkt via Shizuku of root. Bij het aanzetten toont Shizuku een dialoogvenster om SandFly toe te staan. Zonder Shizuku toont de samenvatting een adb-opdracht die de audio eenmaal reset; open het actiemenu op de instelling en kies **Opdracht kopiëren** om deze te kopiëren.

### Leesinstellingen voor geluid en volume {#sound-reading-controls}

Deze leesinstellingen veranderen instellingen voor geluid en volume met een swipe omhoog of omlaag. Voeg ze toe in **Leesinstellingen**. Zie [](#commands).

- **Audio-uitvoerapparaat**: wisselt tussen de keuzes van **Audio-uitvoerapparaat**.
- **Spraakvolume**: wisselt tussen de volumes van **Volumeregeling voor spraak**.
- **Audiovolume verlagen**: zet **Audiovolume verlagen** aan of uit.
- **Toegankelijkheidsvolume**: verhoogt of verlaagt het toegankelijkheidsvolume.
- **Mediavolume**, **Gespreksvolume**, **Belvolume**, **Meldingsvolume** en **Alarmvolume**: verhogen of verlagen elk volume met één stap en zeggen het nieuwe volume. Het bel- of meldingsvolume wijzigen naar of van stil vereist **Toegang tot Niet storen**.
- **Geluidsmodus**: zet de beltoon op geluid, trillen of stil. Stil aan- of uitzetten vereist **Toegang tot Niet storen**.
