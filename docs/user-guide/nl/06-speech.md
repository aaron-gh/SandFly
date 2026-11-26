## Spraak {#speech}

Het scherm **Spraak** in de instellingen van SandFly bepaalt de spraakengine, stem, snelheid, toonhoogte en volume, stemprofielen, talen, uitspraak en wat spraak stopt. Het heeft de groepen **Stem**, **Talen**, **Uitspraak**, **Spraak stoppen** en **Andere apps**.

Waar spraak wordt afgespeeld, het volume waarop, het verlagen van ander geluid en **Audio met lage latentie** staan op het scherm **Audio-uitvoer**. Zie [](#sound). Wat SandFly over elk item zegt, zoals gebruikstips, interpunctie en emoji, staat op het scherm **Uitgebreidheidsniveau**. Zie [](#verbosity).

### Kies een spraakengine {#speech-engine}

SandFly gebruikt zijn eigen instelling voor de spraakengine, los van de standaardengine van Android.

1. Ga naar **Spraak** > **Tekst-naar-spraak-engine**.
2. Kies een geïnstalleerde engine, of **Systeemstandaard** om de engine te gebruiken die in Android is ingesteld.

**Spraak** > **Tekst-naar-spraak-instellingen van systeem** opent de tekst-naar-spraak-instellingen van Android, waar je stemmen kunt installeren en de standaardengine van het systeem kunt instellen. Deze staat niet op tv's.

De leesinstelling **Spraakmotor** wisselt tussen de geïnstalleerde engines wanneer je omhoog of omlaag swipet. Deze wijzigt de engine van **SandFly-standaard**, dus is deze niet beschikbaar terwijl een stemprofiel in gebruik is. Deze staat niet standaard in de leesinstellingen. Om hem toe te voegen, ga je naar **Leesinstellingen**. Zie [](#commands).

Sommige engines spelen hun audio zelf af, zoals engines die tekst doorgeven aan andere engines. SandFly herkent deze en speelt hun spraak op de gewone manier af in plaats van via **Audio met lage latentie**.

### Verander de spreeksnelheid, toonhoogte en volume {#speech-rate}

De schuifregelaars in de groep **Stem** bepalen de spraak van **SandFly-standaard**. Ze zijn alleen beschikbaar terwijl SandFly aan staat.

- **Spreeksnelheid**: van 10% tot 600%. De standaardwaarde is 100%.
- **Toonhoogte van spraak**: van 20% tot 200%. De standaardwaarde is 100%.
- **Spraakvolume**: van 25 tot 100. De standaardwaarde is 100.

**Stapgrootte spreeksnelheid**, onder **Spreeksnelheid**, stelt in hoeveel de snelheid per keer verandert met de schuifregelaars **Spreeksnelheid** van dit scherm en van stemprofielen, de leesinstelling **Spreeksnelheid** en de opdrachten voor de spreeksnelheid. **10% van de snelheid**, de standaardwaarde, verandert de gebruikte snelheid met een tiende ervan. **1%**, **2%**, **5%**, **10%** en **20%** gaan naar het volgende veelvoud van die stap, zoals van 133% naar 135% met **5%**.

Terwijl een stemprofiel in gebruik is, toont het item **Stemprofielen** "In gebruik:" en de naam van het profiel, en veranderen deze schuifregelaars niet hoe het klinkt. Het wijzigen van de snelheid of toonhoogte met opdrachten of leesinstellingen wijzigt dan het profiel dat in gebruik is. Zie [](#speech-voice-profiles).

Je kunt spraak ook wijzigen zonder de instellingen te openen:

- De leesinstelling **Spreeksnelheid**, die standaard in de leesinstellingen staat: swipe omhoog of omlaag om de snelheid te wijzigen.
- De opdrachten **Spreeksnelheid verhogen**, **Spreeksnelheid verlagen**, **Spreektoonhoogte verhogen** en **Spreektoonhoogte verlagen**, voor gebaren, sneltoetsen en menu's.
- De sneltoetsen **Spraakvolume verhogen**, **Spraakvolume verlagen** en **Spreeksnelheid en toonhoogte resetten**. Zie [](#appendix-keyboard-shortcuts).

Om te kiezen of spraak wordt afgespeeld op het toegankelijkheids-, media-, bel- of gespreksvolume, zie [](#sound-speech-volume). Om het spraakvolume te wijzigen met de volumetoetsen, zie [](#buttons-speech-volume).

### Herhaal de laatste zin langzamer {#speech-repeat-slower}

De opdracht **Laatst uitgesproken woordgroep langzamer herhalen**, in de groep **Lezen** van de opdrachten, zegt de laatst uitgesproken zin nog eens, langzamer, met dezelfde stem. De spreeksnelheid verandert niet, dus het volgende item wordt op de gewone snelheid uitgesproken.

**Spraak** > **Langzamer herhalen met** bepaalt hoeveel langzamer: 5%, 10%, 15%, 20%, 25%, 30%, 40% of 50%. De standaardwaarde is 15%.

Om de opdracht te gebruiken, wijs je hem toe aan een gebaar of sneltoets, of voeg je hem toe aan een menu. Zie [](#commands).

### Stemprofielen {#speech-voice-profiles}

Een stemprofiel is een set spraakinstellingen die je in één keer kunt wisselen: een spraakengine, taal, stem, volume, snelheid, toonhoogte, stemeffecten en **Stuur lange tekst zin voor zin**. Zo kan het ene profiel een andere engine gebruiken om lange tekst voor te lezen, terwijl **SandFly-standaard** snel blijft.

**SandFly-standaard** staat altijd in de lijst. Het gebruikt de instellingen van het scherm **Spraak**. Terwijl een profiel in gebruik is, gebruikt alle spraak de engine en stem van het profiel, dus SandFly wisselt dan niet van taal of dialect, behalve voor talen die een eigen stem hebben in **Stemmen per taal**. Zie [](#speech-voices-by-language).

Stemprofielen worden ook gebruikt voor emoji en voor talen: zie [](#speech-emoji) en [](#speech-voices-by-language).

#### Voeg een stemprofiel toe {#speech-add-voice-profile}

1. Ga naar **Spraak** > **Stemprofielen**.
2. Kies **Stemprofiel toevoegen**.
3. Voer een naam in, of laat het veld leeg om de voorgestelde naam te gebruiken, zoals "Stemprofiel 1". Kies **OK**.

SandFly opent de instellingen van het nieuwe profiel. Een nieuw profiel begint met het spraakvolume, de snelheid, de toonhoogte, de stemeffecten en **Stuur lange tekst zin voor zin** van **SandFly-standaard**.

#### Instellingen van stemprofiel {#speech-voice-profile-settings}

Om een profiel te wijzigen, ga je naar **Spraak** > **Stemprofielen** en kies je het profiel.

- **Naam**: hernoemt het profiel.
- **Tekst-naar-spraak-engine**: de engine van het profiel. Een engine die niet meer is geïnstalleerd, staat vermeld met "niet geïnstalleerd".
- **Taal**: **Standaard**, of een van de talen van de engine.
- **Stem**: **Standaard**, of een van de stemmen van de engine in de gekozen taal.
- **Spraakvolume**, **Spreeksnelheid** en **Toonhoogte van spraak**: zoals op het scherm **Spraak**, voor dit profiel.
- **Voorbeeld afspelen**: spreekt een voorbeeldzin uit met de engine, stem, het volume, de snelheid, toonhoogte en stemeffecten die hier zijn ingesteld, in de taal van het profiel als SandFly de zin in die taal heeft. "Voorbeeld niet afgespeeld" betekent dat de engine niet is gestart of een fout heeft teruggegeven.
- **Stemeffecten**: de stemeffecten van dit profiel. Zie [](#voice-effects). Niet op horloges.
- **Stuur lange tekst zin voor zin**: zoals op het scherm **Spraak**, voor dit profiel. Zie [](#speech-pronunciation).
- **Engine gestart houden**: houdt de engine van dit profiel actief terwijl een andere engine in gebruik is en het scherm aan staat, zodat wisselen naar het profiel niet wacht tot de engine start. Elke engine die gestart blijft, draait als een eigen proces. Standaard uit.
- **Omhoog verplaatsen** en **Omlaag verplaatsen**: alleen op horloges. Op telefoons verplaats je profielen in de lijst.
- **Stemprofiel verwijderen**: verwijdert het profiel na bevestiging.

#### Wissel stemprofiel {#speech-switch-voice-profile}

- **Spraak** > **Stemprofielen** > **Stemprofiel in gebruik**: kies **SandFly-standaard** of een profiel.
- De leesinstelling **Stemprofiel**: swipe omhoog of omlaag om door **SandFly-standaard** en de profielen te gaan, in de volgorde van het scherm **Stemprofielen**. Deze staat niet standaard in de leesinstellingen. Om hem toe te voegen, ga je naar **Leesinstellingen**.
- De opdrachten **Kies stemprofiel**, **Volgende stemprofiel** en **Vorig stemprofiel**, in de groep **Spraak** van de opdrachten.
- Zodra je een profiel hebt, heeft de groep **Stemprofielen** van de opdrachten een opdracht **Stemprofiel gebruiken** voor **SandFly-standaard** en voor elk profiel, voor gebaren, sneltoetsen en menu's.

Je kunt ook een item **Stemprofiel** toevoegen aan het SandFly-menu. Zie [](#commands).

#### Volgorde, hernoemen en verwijderen van stemprofielen {#speech-manage-voice-profiles}

Op het scherm **Stemprofielen**:

- Om een profiel te verplaatsen, tik je erop en houd je vast, en sleep je het omhoog of omlaag, of gebruik je de acties **Omhoog verplaatsen** en **Omlaag verplaatsen**. SandFly kondigt aan boven of onder welk profiel het is verplaatst.
- Om een profiel te hernoemen of te verwijderen, gebruik je de acties **Hernoemen** en **Verwijderen**, of tik je op het profiel en houd je vast om een menu met deze opties te openen.

**Engine van het vorige stemprofiel gestart houden**, op het scherm **Stemprofielen**, houdt de engine van het profiel dat vóór het huidige werd gebruikt actief terwijl het scherm aan staat, zodat terugwisselen niet wacht tot de engine start. Standaard uit.

### Emojistem en -snelheid {#speech-emoji}

Twee instellingen in de groep **Stem** veranderen hoe emoji klinken:

- **Emoji-stem**: **Zelfde als spraak**, de standaardwaarde, of een stemprofiel om emoji mee uit te spreken.
- **Spraaksnelheid voor emoji**: de snelheid van emoji als percentage van de spreeksnelheid, van 50% tot 300%. De standaardwaarde is 100%.

Ze gelden zowel wanneer de spraakengine de emoji voorleest als wanneer SandFly ze vervangt door hun namen. Of emoji worden voorgelezen, en hoe, wordt ingesteld bij **Uitgebreidheidsniveau** > **Emoji**. Zie [](#verbosity-emoji).

### Stemeffecten {#voice-effects}

**Spraak** > **Stemeffecten** stelt een equalizer met 11 banden en compressie-effecten in voor **SandFly-standaard**. **Stemeffecten** op het scherm van een stemprofiel stelt deze in voor dat profiel. Een nieuw profiel begint met de stemeffecten van **SandFly-standaard**. Stemeffecten zijn niet beschikbaar op horloges.

Het scherm **Stemeffecten** heeft:

- **Stemeffecten gebruiken**: zet de equalizer en de effecten aan of uit zonder ze te wijzigen. Standaard aan.
- **Voorinstelling**: **Vlak**, **Helderheid**, **Meer bas**, **Minder bas**, **Meer hoge tonen**, **Minder hoge tonen**, **Warme klank**, **Minder scherpte**, **Minder sisklanken**, **Kleine luidspreker** of **Diepe bas**, daarna je opgeslagen voorinstellingen. Een voorinstelling stelt de banden en de effecten in. De banden beginnen vlak en de effecten uit. Het wijzigen van een band of een effect zet **Voorinstelling** op **Aangepast**.
- **Opslaan als voorinstelling**: slaat de banden en effecten op als een voorinstelling met een naam. Opgeslagen voorinstellingen worden vermeld voor **SandFly-standaard** en voor elk stemprofiel. Je kunt tot 50 voorinstellingen opslaan.
- **Opgeslagen voorinstellingen**: hernoemt of verwijdert een opgeslagen voorinstelling. Het verwijderen van een voorinstelling verandert niets aan de stemeffecten die erop zijn ingesteld.
- **Bascompressie**, **Stemcompressie** en **Hogetonencompressie**: **Uit**, **Laag**, **Gemiddeld** of **Hoog**. Elk comprimeert spraak in zijn bereik, onder 200 Hz, 200 Hz tot 4 kHz, of boven 4 kHz, en verhoogt het niveau ervan, zodat stille delen van het bereik luider worden. **Diepe bas** gebruikt **Bascompressie** op **Gemiddeld**.
- **Equalizer**: een schuifregelaar voor elke band: 64 Hz, 125 Hz, 250 Hz, 500 Hz, 1 kHz, 2 kHz, 3 kHz, 4 kHz, 6 kHz, 8 kHz en 16 kHz, elk van -12 dB tot +12 dB.

Wanneer een band versterkt of een effect aan staat, voorkomt een limiter dat spraak vervormt.

De stemeffecten gelden voor spraak die SandFly afspeelt via **Audio-uitvoer** > **Audio met lage latentie**. Met **Audio met lage latentie** uit, gelden ze als audio-effect op Android 9 en later, waar het apparaat dit ondersteunt. Anders toont het item **Stemeffecten** "Vereist Audio-uitvoer > Audio met lage latentie". Engines die tekst doorgeven aan andere engines passen het effect alleen toe als ze ook de audiosessie doorgeven. Zie [](#sound).

Leesinstellingen: **Stemeffecten** zet de stemeffecten van de stem in gebruik aan of uit, en elke equalizerband heeft een leesinstelling, zoals **Equalizer 1 kHz**. Swipe omhoog of omlaag om de band met 1 dB te wijzigen. Ze staan niet standaard in de leesinstellingen. Om ze toe te voegen, ga je naar **Leesinstellingen**. De opdracht **Stemeffecten gebruiken**, in de groep **Spraak** van de opdrachten, zet de stemeffecten aan of uit.

### Talen {#speech-languages}

Wanneer een app tekst als een andere taal markeert, wisselt SandFly naar de stem van die taal. De groep **Talen** bepaalt hoe:

- **Automatisch van taal wisselen**: wisselt naar de stem van een andere taal voor tekst in die taal. Standaard aan.
- **Automatisch van dialect wisselen**: wisselt naar een andere regionale vorm van de taal van de stem, zoals Brits-Engels voor tekst die als Brits-Engels is gemarkeerd terwijl de stem Amerikaans-Engels spreekt. Standaard aan.
- **Stemmen per taal**: de talen die je leest, elk met een stem. Zie [](#speech-voices-by-language).
- **Talen herkennen aan woorden**: onderscheidt talen die in hetzelfde schrift zijn geschreven aan hun woorden, voor tekst die apps niet markeren. Standaard uit.

Met een wijziging uit, leest SandFly de tekst voor met de stem die in gebruik is, zonder pauzes erom heen.

Met **Automatisch van taal wisselen** aan, vindt SandFly ook de taal van tekst die apps niet markeren aan het schrift, zoals Arabische of Griekse letters in Engelse tekst, en aan letters die maar één van je talen gebruikt. **Talen herkennen aan woorden** gebruikt taalherkenning op het apparaat van Google Play-services, voor stukken tekst van minstens 12 letters.

De opdrachten **Automatisch van taal wisselen** en **Talen herkennen aan woorden**, in de groep **Spraak** van de opdrachten, zetten deze instellingen aan of uit.

#### Stemmen per taal {#speech-voices-by-language}

**Spraak** > **Stemmen per taal** toont de talen die je toevoegt. Elke taal wordt uitgesproken met **SandFly-standaard**, met de stem van de engine voor die taal, of met een stemprofiel. Gebruik een profiel wanneer de standaardengine een stem voor de taal heeft die je niet wilt gebruiken.

Om een taal toe te voegen:

1. Ga naar **Spraak** > **Stemmen per taal**.
2. Kies **Taal toevoegen**. SandFly toont de talen van alle geïnstalleerde engines.
3. Kies de taal. SandFly voegt deze toe met **SandFly-standaard**.
4. Om een stemprofiel te gebruiken, kies je de taal in de lijst en daarna het profiel.

Om een taal te verwijderen, kies je deze en kies je **Taal verwijderen**.

Wanneer twee of meer van de vermelde talen, of een vermelde taal en de taal van de stem, een schrift delen, zoals Engels en Frans in het Latijnse schrift, toont het scherm een lijst voor dat schrift, zoals **Latijns schrift**. Deze bepaalt de taal van tekst in dat schrift wanneer niets anders deze herkent.

#### Verander de gesproken taal {#speech-spoken-language}

De leesinstelling **Gesproken taal** en de opdracht **Gesproken taal wijzigen** veranderen de taal van de spraak van SandFly-standaard. Terwijl een stemprofiel in gebruik is, bepaalt het profiel de taal, en heeft de leesinstelling geen waarde. De leesinstelling staat niet standaard in de leesinstellingen.

### Uitspraak {#speech-pronunciation}

De groep **Uitspraak** heeft:

- **Verschillende toonhoogten gebruiken**: spreekt verwijderde letters uit met een hogere stem. Standaard aan.
- **Letters fonetisch uitspreken bij doorlopen**: voor letters waar je per teken overheen gaat en gespelde tekst.
- **Letters fonetisch uitspreken bij typen**: voor toetsenbordtoetsen en letters die je typt of verwijdert.
- **Stuur lange tekst zin voor zin**: voor spraakengines die traag stoppen, zoals Gryphon, zodat een swipe lange tekst meteen stopt. Veel engines pauzeren langer tussen apart verstuurde zinnen, dus laat dit uit staan als jouw engine snel stopt. Standaard uit.
- **Regeleinden als spaties voorlezen**: leest tekst voor zonder de pauzes bij regeleinden. Standaard uit.
- **Spraakwoordenboek** en **Symbolenwoordenboek**: zie [](#speech-dictionary).

De twee instellingen voor fonetische letters hebben elk vier keuzes:

- **Uit**: zegt alleen de letter.
- **Na de letter, met pauze, zoals ‘h, Hendrik’**: de standaardwaarde. Wanneer je per teken verplaatst, volgt het woord na een korte pauze, en als je eerder verder gaat, wordt het overgeslagen.
- **Na de letter, zonder pauze, zoals ‘h Hendrik’**.
- **In plaats van de letter, zoals ‘Hendrik’**.

### Spraakwoordenboek {#speech-dictionary}

**Spraak** > **Spraakwoordenboek** verandert hoe tekst wordt uitgesproken in alle spraak, zoals een woord dat als een ander woord wordt gezegd, of een symbool dat als spatie wordt gezegd. Items worden in volgorde toegepast, elk op de tekst die het vorige item overliet. Instellingenschermen van SandFly worden voorgelezen zoals ze er staan.

Om een item toe te voegen:

1. Ga naar **Spraak** > **Spraakwoordenboek**.
2. Kies **Vervanging toevoegen**.
3. Voer in **Tekst** de tekst in om te zoeken. Voer in **Uitspreken als** in wat er in plaats daarvan moet worden gezegd. Laat **Uitspreken als** leeg om de tekst weg te laten.
4. Kies **Alleen hele woorden**, standaard aan, om alleen hele woorden te matchen, en **Reguliere expressie** om een reguliere expressie te matchen.
5. Kies **OK**.

Kies een item voor **Bewerken**, **Kies een app** en **Verwijderen**. **Kies een app** beperkt het item tot één app, of **Alle apps**. Een item voor één app geldt terwijl een venster van die app het laatst getoonde venster is.

**Importeren uit bestand** en **Exporteren naar bestand** laden en bewaren de items als een bestand. Zie [](#api-item-files).

Het brailewoordenboek, voor brailleleesregels, staat in de brailleinstellingen. Zie [](#braille).

#### Symbolenwoordenboek {#speech-symbol-dictionary}

**Spraak** > **Symbolenwoordenboek** vervangt symbolen en reeksen symbolen, in spraak en op een brailleleesregel. Een item matcht zijn exacte tekst overal, ook een punt direct na een woord, en heeft geen optie voor hele woorden of reguliere expressie. Items worden in volgorde toegepast, na het spraakwoordenboek of braillewoordenboek. Je kunt tot 500 items toevoegen.

Kies **Vervanging toevoegen**, voer het **Symbool** in en wat je wilt **Vervangen door**, en kies **OK**. Items hebben dezelfde opties **Bewerken**, **Kies een app**, **Verwijderen**, **Importeren uit bestand** en **Exporteren naar bestand** als het spraakwoordenboek.

In spraak gelden items voor interpunctie die spraakengines niet voorlezen, zoals een punt of komma, niet terwijl **Uitgebreidheidsniveau** > **Interpunctie en symbolen uitspreken** die interpunctie weglaat. Met **Geen** geldt geen item voor een los symbool in tekst, behalve waar **Symbolen uitspreken die de betekenis veranderen** het symbool laat staan. Zie [](#verbosity-text).

### Pauzeer en hervat spraak {#speech-pause}

De opdracht **Spraak pauzeren of hervatten** pauzeert spraak en hervat deze. Standaard is dit een tweevingertik. Zie [](#appendix-gestures).

Wanneer spraak hervat:

- Met **Audio-uitvoer** > **Audio met lage latentie** aan, stopt spraak midden in een woord en hervat vanaf dezelfde plek. SandFly bewaart tot 10 seconden gepauzeerde spraak.
- Anders, met engines die woordposities doorgeven, hervat spraak vanaf het woord waar het stopte.
- Met engines die geen woordposities doorgeven, zoals RHVoice en Gryphon, schat SandFly hoe ver de engine was aan de hand van hoe snel eerdere items werden gesproken, en hervat vanaf het begin van die zin of woordgroep. Het kan een paar woorden herhalen, maar slaat er geen over.

SandFly speelt het geluid en de trilling van het item niet opnieuw af wanneer spraak hervat.

De opdracht **Spraak aan- of uitzetten** zet alle spraak uit en aan.

Op een fysiek toetsenbord pauzeert alleen Ctrl indrukken spraak. Zie [](#keyboards).

### Stop spraak {#speech-stop}

De groep **Spraak stoppen** bepaalt wat spraak stopt:

- **Spraak stoppen bij aanraken van het scherm**: standaard aan.
- **Spraak onderbreken tijdens typen**: wanneer een teken dat op een fysiek toetsenbord wordt getypt spraak stopt. **Altijd**, de standaardwaarde, **In bewerkingsvelden**, of **Bij toetsenbordecho**, alleen wanneer de toetsenbordecho het teken of het woord dat het afsluit uitspreekt. Niet op horloges.
- **Spraak onderbreken bij Enter**: Enter op een fysiek toetsenbord stopt spraak. Standaard aan. Niet op horloges.
- **Afstandssensor afdekken om spraak te stoppen**: het afdekken van de afstandssensor stopt spraak. Standaard uit. Niet op tv's en niet op apparaten zonder afstandssensor.
- **Stil tijdens het opnemen**: terwijl een app opneemt zonder dat er een koptelefoon is aangesloten, spreekt SandFly alleen binnen 2 seconden na een aanraking, gebaar, toets of brailleinvoer, en is stil voor andere spraak, zoals meldingen, zodat deze niet wordt opgenomen. Standaard uit.

Op een fysiek toetsenbord stopt elke toets behalve de controletoetsen, vergrendeltoetsen en de volumetoetsen spraak, en zo ook alle sneltoetsen met Ctrl, Alt of Meta. Getypte tekens en Enter volgen de instellingen hierboven. Spatie buiten een bewerkbaar veld stopt spraak altijd.

Terwijl een andere app opneemt vanaf de microfoon en tijdens gesprekken, spreekt SandFly nog steeds wat je met een gebaar of toets opvraagt, zoals de tijd, het batterijniveau of de venstertitel.

### Spraakgeschiedenis {#speech-history}

De opdracht **Spraakgeschiedenis**, in de groep **Lezen** van de opdrachten, opent een lijst van de laatste 100 teksten die SandFly heeft uitgesproken, nieuwste eerst.

- Kies een tekst voor **Kopiëren**, en voor **Toon alles** wanneer de lijst de tekst inkort.
- **Geschiedenis wissen**, aan het einde van de lijst, verwijdert elke tekst.

De geschiedenis wordt alleen in het geheugen bewaard, nooit opgeslagen of gelogd, en wordt gewist wanneer SandFly wordt uitgezet. Deze bevat geen spraak terwijl een wachtwoordveld focus heeft, of spraak terwijl de lijst open is. Elke tekst wordt tot 2000 tekens bewaard.

Om de opdracht te gebruiken, wijs je hem toe aan een gebaar of sneltoets, of voeg je hem toe aan een menu. Zie [](#commands).

### Laat andere apps spreken {#speech-other-apps}

Apps en scripts kunnen tekst naar SandFly sturen om voor te lezen in jouw SandFly-stem en -instellingen. **Spraak** > **Andere apps laten spreken** bepaalt welke apps dit mogen:

- **Andere apps laten spreken**: zet spraak van andere apps aan of uit. Standaard uit.
- **Speel een geluid af voordat andere apps spreken**: speelt een geluid af voor elke tekst van een andere app, zodat deze niet voor SandFly-spraak wordt aangezien. Standaard uit.
- **Apps**: elke app die verbinding heeft gemaakt, met een vakje om deze toe te staan. Een app die mag spreken terwijl het apparaat is vergrendeld heeft ook een vakje met zijn naam en "terwijl het apparaat is vergrendeld". "Nog geen app heeft verbinding gemaakt." betekent dat de lijst leeg is.

De eerste keer dat een app proberen te spreken, toont SandFly een melding, "Toestaan dat" (app) "spreekt via SandFly?", met **Toestaan** en **Niet toestaan**. Niets van de app wordt uitgesproken tot je dit toestaat. Niets van andere apps wordt uitgesproken terwijl SandFly uit of gepauzeerd is, en apps kunnen SandFly-spraak niet stoppen of vervangen.

Het toetsenbord dat in gebruik is, kan spreken zonder in de lijst te staan, en kan de SandFly-toetsenbordecho van zijn eigen typen zelf uitzetten.

Voor ontwikkelaars, zie [](#api-speech).

### Spraakopdrachten en leesinstellingen {#speech-commands}

De groep **Spraak** van de opdrachten heeft onder meer: **Spraak pauzeren of hervatten**, **Spraak aan- of uitzetten**, **Spreeksnelheid verhogen**, **Spreeksnelheid verlagen**, **Spreektoonhoogte verhogen**, **Spreektoonhoogte verlagen**, **Gesproken taal wijzigen**, **Automatisch van taal wisselen**, **Talen herkennen aan woorden**, **Kies stemprofiel**, **Volgende stemprofiel**, **Vorig stemprofiel** en **Stemeffecten gebruiken**. Sneltoetsen voegen **Spraakvolume verhogen**, **Spraakvolume verlagen**, **Spreeksnelheid en toonhoogte resetten** en **Instellingen voor tekst-naar-spraak openen** toe.

De groep **Lezen** heeft **Laatst uitgesproken woordgroep herhalen**, **Laatst uitgesproken woordgroep langzamer herhalen**, **Laatst uitgesproken woordgroep spellen** en **Spraakgeschiedenis**.

De spraakleesinstellingen zijn **Spreeksnelheid**, **Gesproken taal**, **Spraakmotor**, **Stemprofiel**, **Stemeffecten** en de equalizerbanden. Alleen **Spreeksnelheid** staat standaard in de leesinstellingen. De leesinstellingen **Spraakvolume** en **Audio-uitvoerapparaat** worden beschreven in [](#sound-reading-controls). De groep **Spraak** van **Leesinstellingen** heeft ook **Interpunctie en symbolen uitspreken**, **Emoji** en **Tekstopmaak voorlezen** (zie [](#verbosity-text)), **Automatische vertaling** (zie [](#translation)) en **Alleen door de gebruiker verplaatste focus uitspreken** (zie [](#verbosity-own-speech)).

Om opdrachten toe te wijzen aan gebaren en sneltoetsen, en om leesinstellingen toe te voegen, zie [](#commands).
