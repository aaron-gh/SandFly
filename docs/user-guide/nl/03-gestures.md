## Gebaren {#gestures}

SandFly voert een opdracht uit voor elk gebaar dat je op het aanraakscherm maakt: swipes, tikken, bewegingen vanaf de randen van het scherm, draaien met twee vingers en meer. De meeste gebaren kunnen elke opdracht uitvoeren, en je kunt ze veranderen in **Gebaren**. Dit hoofdstuk beschrijft de soorten gebaren, de instellingen op het scherm **Gebaren**, gebaren in afzonderlijke apps en gebarenprofielen.

De opdrachten die elk gebaar standaard uitvoert, staan vermeld in [](#appendix-gestures). De opdrachten zelf worden beschreven in [](#commands).

### Hoe SandFly gebaren herkent {#gestures-recognition}

Een aanraking die op het scherm rust of langzaam beweegt, is verkennen via aanraking: SandFly leest het item onder de vinger voor. Een aanraking die snel beweegt, of een tik, is een gebaar. Zie [](#reading) voor verkennen via aanraking.

Op Android 13 en hoger herkent SandFly gebaren zelf. Veel gebaren in dit hoofdstuk hebben dit nodig: de rotor, randgebaren, hoekgebaren, de schrobbeweging, knijpen en spreiden, swipe en vasthouden, swipes in drie delen, swipes in twee halen, tikken dan swipen, hoekgebaren met twee vingers, de viervoudige tik met drie vingers, de dubbele tik met twee vingers aan de boven- en onderkant van het scherm en de tikken met vier vingers op de bovenste of onderste helft van het scherm. De tijdsinstellingen op het scherm **Gebaren** hebben dit ook nodig. Gebaren met twee of meer vingers hebben Android 11 of hoger nodig.

Herkenning door SandFly wordt bepaald door **Ontwikkelaarsinstellingen** > **Gebaren verwerken in SandFly**, wat standaard aan staat. Wanneer je dit uitzet, herkent Android gebaren, en zijn de gebaren en instellingen hierboven niet beschikbaar. Zet SandFly uit en weer aan na het veranderen van deze instelling.

### Tikken {#gestures-taps}

- **Eén vinger**: dubbeltikken activeert het gefocuste item, en dubbeltikken en vasthouden tikt het item en houdt het vast. Deze gebaren kunnen niet worden veranderd.
- **Twee, drie en vier vingers**: een tik, dubbeltik en 3 keer tikken met elk aantal vingers, en dubbeltikken en vasthouden. Twee en drie vingers hebben ook 3 keer tikken en vasthouden, en drie vingers hebben tikken en vasthouden en een viervoudige tik.
- **Twee vingers boven en onder**: **Dubbeltikken met 2 vingers, boven en onder** is een afzonderlijk gebaar. Terwijl het een opdracht heeft, voert een dubbele tik met twee vingers, met één vinger in het bovenste derde deel van het scherm en de andere in het onderste derde deel, deze opdracht uit in plaats van **Dubbeltikken met 2 vingers**. Boven en onder volgen het scherm zoals getoond, ook in liggende stand.
- **Vier vingers op helft van het scherm**: **Tikken met 4 vingers op bovenste helft** en **Tikken met 4 vingers op onderste helft** zijn afzonderlijke gebaren. Terwijl een van deze een opdracht heeft, voert een tik met vier vingers op die helft die opdracht uit in plaats van **Tikken met 4 vingers**.

Een tik met twee, drie of vier vingers voert zijn opdracht uit zodra de vingers worden opgetild, tenzij een langer gebaar dat met dezelfde tikken begint een opdracht heeft, zoals een verdere tik, een tik en vasthouden, of tikken dan swipen. Dan wacht SandFly op de volgende tik. Als een vinger beweegt tijdens een tik met meerdere vingers, is de aanraking geen tik meer. Een viervoudige tik met drie vingers wordt altijd meteen uitgevoerd.

**Tijd tussen tikken met één vinger** stelt in hoe lang SandFly wacht op een verdere tik met 1 vinger. Zie [](#gestures-timing).

### Swipes {#gestures-swipes}

Een swipe beweegt een of meer vingers snel over het scherm in één richting. Swipes met 1 vinger naar links en rechts bewegen naar het vorige en volgende item, en swipes omhoog en omlaag veranderen de waarde van de huidige leesinstelling. Zie [](#reading).

Swipes met twee vingers scrollen de app of voeren SandFly-opdrachten uit; zie [](#gestures-two-finger-swipes). Swipes met drie en vier vingers voeren SandFly-opdrachten uit.

#### Heen-en-weergebaren en hoekgebaren {#gestures-two-direction}

Een heen-en-weergebaar swipet de ene kant op en terug zonder de vinger op te tillen, zoals **Omhoog, dan omlaag swipen**. Een hoekgebaar swipet de ene kant op en draait dan haaks af, zoals **Omlaag en naar rechts swipen**. Deze staan in de categorieën **Heen en weer met 1 vinger** en **Hoekgebaar met 1 vinger** van het scherm **Gebaren**.

**Hoekgebaar met 2 vingers** heeft acht hoekgebaren met twee vingers, zoals **Met 2 vingers omhoog en naar rechts swipen**. Deze doen standaard niets, werken alleen terwijl **Swipes met twee vingers in SandFly verwerken** aan staat, en zijn verborgen terwijl dit uit staat. Twee vingers die de ene kant op bewegen en terug zijn de schrobbeweging, dus er zijn geen heen-en-weergebaren met twee vingers.

**Veegbewegingen herkennen voor het loslaten** zet de heen-en-weergebaren en hoekgebaren met 1 vinger en de hoekgebaren met 2 vingers uit en verbergt ze. Zie [](#gestures-swipes-before-lift).

#### Swipes in drie delen {#gestures-three-part}

Een swipe in drie delen beweegt één vinger in drie richtingen zonder deze op te tillen, waarbij elk deel afbuigt van het vorige, zoals **Naar rechts, omhoog, dan naar rechts swipen** of **Omhoog, omlaag, dan omhoog swipen**. De 36 gebaren staan in de categorie **Swipes in drie delen** van het scherm **Gebaren**. Ze doen standaard niets, en een swipe in drie delen zonder opdracht wordt niet herkend. Elk deel heeft de lengte van een deel van een hoekgebaar nodig. **Veegbewegingen herkennen voor het loslaten** zet ze uit en verbergt ze. Niet op horloges.

#### Swipe en vasthouden {#gestures-swipe-and-hold}

Elke swipe met 1 vinger, heen-en-weerswipe en hoekswipe heeft een versie die eindigt met de vinger rustend op het scherm, zoals **Naar rechts swipen en vasthouden** of **Naar links en omhoog swipen en vasthouden**. Swipe, en houd de vinger dan stil: de opdracht wordt uitgevoerd wanneer de vasthoudtijd afloopt, voordat je de vinger optilt. Eerder optillen maakt er de gewone swipe van.

Een pauze die korter is dan de vasthoudtijd op het punt waar een hoek- of heen-en-weerswipe afbuigt, blijft deel van die swipe. Een langere pauze is de swipe en vasthouden van de eerste richting, wanneer die een opdracht heeft.

Deze 16 gebaren staan aan het einde van de categorieën **1 vinger**, **Heen en weer met 1 vinger** en **Hoekgebaar met 1 vinger** en doen standaard niets. Een swipe en vasthouden zonder opdracht wordt niet herkend, zodat swipes werken zoals voorheen tot je er een toewijst. **Vasthoudtijd na een swipe** stelt de rest in; zie [](#gestures-timing). Niet op horloges.

#### Swipes in twee halen {#gestures-two-stroke}

Een swipe in twee halen is twee swipes met 1 vinger met de vinger opgetild ertussen, zoals **Naar rechts swipen, loslaten, dan omhoog swipen**. Elke swipe kan een swipe in één richting, een heen-en-weerswipe of een hoekswipe zijn, zoals **Omhoog en naar links swipen, loslaten, dan naar rechts swipen** of **Omhoog swipen, loslaten, dan naar rechts en omlaag swipen**, wat 256 gebaren maakt, inclusief dezelfde swipe twee keer. Ze doen standaard niets. Op het scherm **Gebaren** staan de 16 die zijn opgebouwd uit swipes in één richting in de categorie **Swipes in twee halen**, die beginnen met een swipe in één richting en eindigen met een heen-en-weer- of hoekswipe staan in **Swipes in twee halen die eindigen met heen en weer of hoekgebaar**, en de andere staan in een categorie per eerste swipe, zoals **Swipes in twee halen, beginnend met Omhoog en naar links swipen**.

Ze werken alleen terwijl **Tijd tussen swipes in twee halen** is ingesteld; zie [](#gestures-timing). Terwijl dit **Uit** staat, zijn de categorieën voor swipes in twee halen verborgen. De tweede swipe moet binnen die tijd na het optillen van de eerste beginnen. Na een swipe wacht SandFly alleen op een tweede swipe wanneer een swipe in twee halen die met die swipe begint een opdracht heeft, in de gebarenindeling, het gebarenprofiel in gebruik of de gebaren van de app; andere swipes worden meteen uitgevoerd. Wanneer de tijd verstrijkt, of de volgende aanraking is een tik, verkennen via aanraking of een gebaar met meer vingers, wordt de eerste swipe uitgevoerd als een gewone swipe.

Een swipe in twee halen die begint met een heen-en-weer- of hoekswipe laat geen swipe in één richting wachten op een tweede swipe, zodat opdrachten op swipes in twee halen kunnen staan terwijl swipes in één richting meteen worden uitgevoerd. Een heen-en-weer- of hoekswipe in een swipe in twee halen met een opdracht wordt herkend, ook wanneer deze zelf geen opdracht heeft. **Veegbewegingen herkennen voor het loslaten** zet de swipes in twee halen met een heen-en-weer- of hoekswipe uit en verbergt ze. Niet op horloges.

### Rotor {#gestures-rotor}

Draai twee vingers op het scherm als een wijzerplaat om een leesinstelling te kiezen: met de klok mee voor de volgende leesinstelling en tegen de klok in voor de vorige. Swipe dan omhoog of omlaag om de waarde ervan te veranderen. Leesinstellingen worden beschreven in [](#commands).

Elke stap van de draai beweegt één leesinstelling, zodat je kunt blijven draaien om verder te bewegen. **Gebaren** > **Rotordraaiing per stap**, onder **2 vingers**, stelt de stap in van 15 tot 60 graden. De standaardwaarde is 30 graden, een twaalfde van een omwenteling. Kies een grotere stap als je vaak voorbij de gewenste leesinstelling beweegt. Scrollen met twee vingers draait de rotor niet.

Je kunt andere opdrachten toewijzen aan **Draai met 2 vingers met de klok mee** en **Draai met 2 vingers tegen de klok in**.

### Schrobben, knijpen en spreiden {#gestures-scrub-pinch-spread}

Om te schrobben, beweeg je twee vingers snel de ene kant op over het scherm en terug, elke streek ongeveer een centimeter lang. Standaard gaat de schrobbeweging terug. Twee vingers die slechts één kant op bewegen, scrollen nog steeds. Terwijl een schrobbeweging nog mogelijk is, kan een zijwaartse scroll met twee vingers tot ongeveer een halve seconde wachten voordat deze begint.

**Knijp met 2 vingers**, **Uiteenspreiden met 2 vingers** en dezelfde gebaren met 3 en 4 vingers doen standaard niets. Een knijp- of spreidbeweging telt zodra de vingers ongeveer een halve centimeter naar of van hun midden af zijn bewogen, zonder dat ze samen bewegen of veel draaien. Terwijl een knijp- of spreidbeweging met twee vingers een opdracht heeft, wacht een beweging met twee vingers die meer knijpt dan beweegt voordat deze de app bereikt, zodat apps die met twee vingers inzoomen misschien niet inzoomen tot beide op **Niet toegewezen** staan.

De schrobbeweging, knijpen en spreiden staan niet op horloges.

### Randgebaren {#gestures-edges}

Randgebaren beginnen precies aan de rand van het scherm, zoals op de navigatiebalk met gebaren of de statusbalk. Ze staan in de categorie **Randen** van het scherm **Gebaren** en staan niet op horloges.

#### Van een rand naar binnen bewegen {#gestures-edges-slide-in}

Beweeg één vinger vanaf een rand naar binnen:

- **Veeg omhoog vanaf de onderrand**: na ongeveer 1 cm speelt SandFly een klik zoals een thuisknop. Til daar je vinger op om naar het startscherm te gaan. Blijf nog ongeveer 4 cm verder bewegen tot een tweede klik, en til daar je vinger op voor **Veeg lang omhoog vanaf de onderrand**, wat de recente apps opent.
- **Veeg naar beneden vanaf linksboven** opent meldingen en **Veeg naar beneden vanaf rechtsboven** opent snelle instellingen, na dezelfde eerste klik. **Veeg lang naar beneden vanaf linksboven** en **Veeg lang naar beneden vanaf rechtsboven**, bij de tweede klik, doen standaard niets.
- Naar rechts vanaf de linkerrand en naar links vanaf de rechterrand. De bovenste en onderste helft van elke zijrand zijn afzonderlijke gebaren, elk met een korte en een lange swipe, zoals **Veeg naar rechts vanaf de linkerrand, bovenste helft**. Deze doen standaard niets.

Alleen bewegingen met een opdracht klikken. Een beweging zonder opdracht bij de eerste klik blijft een gewone swipe tot de tweede klik; een lange swipe zonder opdracht heeft geen tweede klik. Optillen voor de eerste klik maakt een gewone swipe.

Teruggaan van de tweede klik naar de eerste speelt één korte tik, en teruggaan naar de rand speelt er twee; optillen bij de rand annuleert het gebaar. Het aanraken van de rand, de eerste klik en de tweede klik spelen elk een geluid dat bij elke fase stijgt. De geluiden en trillingen kunnen worden uitgezet in **Afzonderlijke geluiden en trillingen**; zie [](#sound).

Een beweging die weg van de rand begint, of die langer dan ongeveer een halve seconde duurt om de eerste klik te bereiken, is een gewone swipe of verkennen via aanraking. Een vinger die 0,2 seconden bij de rand rust, start verkennen via aanraking.

In een hoek bepaalt de richting van de beweging welk gebaar het is. Een beweging die bij twee gebaren past, gebruikt de boven- of onderrand, en een beweging naar binnen gaat voor een beweging langs een rand.

#### Langs een rand bewegen {#gestures-edges-slide-along}

Je kunt ook één vinger langs een rand bewegen: omhoog of omlaag langs de linker- of rechterrand, of naar links of rechts langs de boven- of onderrand, zoals **Veeg omhoog langs de linkerrand**. Begin precies aan de rand en houd je vinger er dicht bij. De opdracht wordt uitgevoerd wanneer je optilt na het bewegen over de afstand tot de eerste klik. Deze gebaren doen standaard niets en spelen geen randgeluiden of trillingen.

#### Langs een rand scrollen {#gestures-edges-scroll}

Om apps met één vinger langs een rand te scrollen, zet **Scrollen met één vinger langs de linkerrand**, **Scrollen met één vinger langs de rechterrand**, **Scrollen met één vinger langs de bovenrand** of **Scrollen met één vinger langs de onderrand** aan onder **Randen**. Deze staan allemaal standaard uit. Een beweging langs die rand verplaatst dan de inhoud van de app met de vinger mee, zoals een swipe met twee vingers doet: omhoog en omlaag langs de linker- en rechterrand, naar links en rechts langs de boven- en onderrand.

Terwijl een rand scrolt, worden de gebaren die er langs bewegen niet gebruikt. Bewegingen vanaf de rand naar binnen blijven gebaren, en dat geldt ook voor bewegingen omhoog en omlaag vanaf de hoeken.

#### Randmenu's {#gestures-edge-menus}

De linker- en rechterrand kunnen elk een menu van opdrachten zijn. Zo stel je een randmenu in:

1. Ga naar **Gebaren** > **Randen** > **Menu linkerrand** of **Menu rechterrand**.
2. Zet **Linkerrand als menu gebruiken** of **Rechterrand als menu gebruiken** aan.
3. Kies **Commando toevoegen**, en kies een opdracht uit dezelfde lijst als voor gebaren. Een menu heeft maximaal 20 opdrachten.

Elke opdracht heeft **Commando wijzigen**, **Omhoog verplaatsen**, **Omlaag verplaatsen** en **Verwijderen**.

Om het menu te gebruiken, begin precies aan de rand en beweeg omhoog of omlaag langs de rand. De opdrachten verdelen de lengte van de rand gelijk, vanaf boven, zodat elke opdracht altijd op dezelfde plek staat. SandFly spreekt de naam van elke opdracht uit met een tikgeluid wanneer de vinger deze bereikt, en optillen voert de opdracht onder de vinger uit. Meer dan drie keer de randbreedte naar binnen bewegen zegt "Annuleren", en daar optillen voert niets uit; teruggaan naar de rand keert terug naar de opdrachten.

Terwijl een rand een menu is, worden de gebaren die er langs bewegen en het scrollen er langs niet gebruikt. Bewegingen vanaf die rand naar binnen blijven gebaren. Een beweging die op het toetsenbord begint, is geen menu, zodat de toetsen aan de rand als gewoonlijk typen.

#### Randinstellingen {#gestures-edge-settings}

Onder **Randen** op het scherm **Gebaren**:

- **Randbreedte**: hoe ver van de rand een aanraking kan beginnen, van 0,5 tot 2 cm. De standaardwaarde is 0,5 cm.
- **Afstand tot eerste klik**: hoe ver je vanaf de rand, of er langs, naar binnen beweegt tot de eerste klik, van 0,5 tot 2 cm. De standaardwaarde is 1 cm.
- **Afstand van de eerste tot de tweede klik**: hoe ver je verder beweegt tot de tweede klik, van 1 tot 5 cm. De standaardwaarde is 4 cm.
- **Opdracht uitspreken bij elke klik**: bij elke klik van een beweging vanaf een rand naar binnen spreekt SandFly de opdracht uit die optillen daar uitvoert, zoals "Terug". Standaard uit.

### Hoekgebaren {#gestures-corners}

Elke hoek van het scherm heeft vijf gebaren met 1 vinger, onder **Hoeken**: tikken, dubbeltikken, 3 keer tikken, dubbeltikken en vasthouden, en 3 keer tikken en vasthouden, zoals **Dubbeltikken in de hoek linksboven**. Deze doen standaard niets.

Een hoek zonder toegewezen opdracht werkt als gewoonlijk. In een hoek met een toegewezen opdracht zijn tikken die SandFly niet gebruikt gewone tikken: een dubbeltik activeert nog steeds. Een dubbeltik wacht alleen op een derde tik terwijl de hoek een opdracht voor 3 keer tikken heeft. Een aanraking die beweegt, een vinger toevoegt, of rust zonder vasthoudgebaar is een gewone aanraking, zodat swipes en randgebaren vanuit de hoek nog steeds werken. Hoekgebaren beginnen niet op het toetsenbord.

- **Hoekgrootte**: de grootte van het vierkant bij elke hoek, van 0,5 tot 2 cm. De standaardwaarde is **Zelfde als randbreedte**.
- **Maximale tiktijd in hoek**: hoe lang een tik in een hoek ingedrukt kan blijven voordat deze als vasthouden telt, van 0,15 tot 0,5 seconden. De standaardwaarde is **Systeemstandaard**.

Niet op horloges.

### Tikken, dan swipen {#gestures-tap-then-swipe}

De categorie **Tikken, dan swipen** heeft gebaren die één, twee of drie keer tikken met twee, drie of vier vingers en dan swipen met net zoveel vingers, zoals **Dubbeltikken, dan omhoog swipen met 2 vingers**. Deze heeft ook gebaren die tikken met twee vingers en dan swipen met één vinger, zoals **Tikken met 2 vingers, dan omhoog swipen**. Deze doen standaard niets.

De swipe is de volgende aanraking na de laatste tik en moet binnen 0,25 seconden daarna beginnen. Terwijl een van deze gebaren een opdracht heeft, wachten de tikken waarmee het begint op de swipe. Niet op horloges.

### Tikken op achterkant {#gestures-back-tap}

Tik twee, drie of vier keer op de achterkant van de telefoon met een vinger om **Dubbeltikken op achterkant**, **3 keer tikken op achterkant** of **4 keer tikken op achterkant** uit te voeren, onder **Tikken op achterkant** op het scherm **Gebaren**. Deze doen standaard niets. SandFly herkent de tikken met de versnellingsmeter, alleen terwijl het scherm aan staat, SandFly niet is gepauzeerd en een tik op de achterkant een opdracht heeft. Tikken terwijl je het scherm aanraakt, tellen niet.

**Gevoeligheid van tikken op achterkant** stelt in hoe hard een tik moet zijn: **Laag** heeft de hardste tikken nodig, **Hoog** de lichtste. De standaardwaarde is **Gemiddeld**. Verhoog dit als tikken worden gemist, en verlaag het als tikken op de achterkant worden uitgevoerd wanneer je de telefoon neerlegt of draagt.

Tikken wachten alleen op een volgende tik terwijl een gebaar met meer tikken een opdracht heeft: een dubbele tik wordt sneller uitgevoerd wanneer **3 keer tikken op achterkant** en **4 keer tikken op achterkant** geen opdracht hebben. Tikken op de achterkant staan niet op horloges of tv's. Tikken op de achterkant werken nog terwijl aanraking is vergrendeld; zie [](#touch).

### Schudden {#gestures-shake}

Schud de telefoon snel heen en weer om **Schudden** uit te voeren, onder **Schudden** op het scherm **Gebaren**. Dit doet standaard niets. Een schudbeweging is minstens twee slagen in elke richting binnen ongeveer twee derde van een seconde. Schudden dat doorgaat, voert **Schudden** één keer uit; de volgende schudbeweging telt nadat de telefoon een moment stil heeft gelegen. SandFly herkent schudbewegingen met de versnellingsmeter, alleen terwijl het scherm aan staat, SandFly niet is gepauzeerd en **Schudden** een opdracht heeft.

**Gevoeligheid van schudden** stelt in hoe hard de telefoon moet worden geschud: **Laag** heeft de hardste schudbewegingen nodig, **Hoog** de lichtste. De standaardwaarde is **Gemiddeld**.

Schudden staat niet op horloges of tv's, en werkt terwijl aanraking is vergrendeld.

### Vingerafdrukgebaren {#gestures-fingerprint}

Op telefoons waarvan de vingerafdruksensor swipes meldt, heeft de categorie **Vingerafdrukgebaren** **Omhoog swipen over vingerafdruksensor**, **Omlaag swipen over vingerafdruksensor**, **Links swipen over vingerafdruksensor** en **Rechts swipen over vingerafdruksensor**. De categorie is verborgen op andere telefoons. Gebarenprofielen veranderen vingerafdrukgebaren niet.

### Het scherm Gebaren {#gestures-screen}

**Gebaren** in de instellingen voor SandFly heeft, van boven naar onder:

- **Zoeken**: vindt een gebaar op naam of op de opdracht die het uitvoert, en opent de lijst met opdrachten daarvan.
- **Gebaren oefenen**: opent een pagina waar SandFly zegt wat elk gebaar dat je maakt doet. Zie [](#gestures-practice).
- **Instellingen voor gebaren resetten**: zie [](#gestures-reset).
- **Gebarenindeling** en **Swipes met twee vingers in SandFly verwerken**: zie [](#gestures-layout) en [](#gestures-two-finger-swipes).
- De tijdsinstellingen: zie [](#gestures-timing).
- **Veegbewegingen herkennen voor het loslaten**: zie [](#gestures-swipes-before-lift).
- **Gebaren in apps**, **Gebarenprofielen** en **App-snelkoppelingen**: zie [](#gestures-in-apps), [](#gestures-profiles) en [](#gestures-app-shortcuts).
- De gebarencategorieën: **1 vinger**, **Heen en weer met 1 vinger**, **Hoekgebaar met 1 vinger**, **Randen**, **Hoeken**, **2 vingers**, **Hoekgebaar met 2 vingers**, **3 vingers**, **4 vingers**, **Tikken, dan swipen**, **Vingerafdrukgebaren**, **Tikken op achterkant**, **Schudden**, **Swipes in drie delen**, **Swipes in twee halen**, **Swipes in twee halen die eindigen met heen en weer of hoekgebaar** en een categorie **Swipes in twee halen, beginnend met** voor elke heen-en-weer- en hoekswipe.

De categorieën zijn ingeklapt. Activeer een categorie om deze uit te vouwen. Elk gebaar in een categorie toont de opdracht die het uitvoert.

Op een horloge heeft het scherm ook **Gebaren reserveren voor Vibration Watch**; zie [](#devices).

#### Gebarenindeling {#gestures-layout}

**Gebarenindeling** kiest de gebaren waarvan het scherm **Gebaren** uitgaat:

- **SandFly-gebaren**: de standaardgebaren van SandFly.
- **VoiceOver-gebaren**: de gebaren van VoiceOver op iPhone, zoals een tik met twee vingers om spraak te pauzeren, een dubbeltik met twee vingers voor **Dicteren, media afspelen of pauzeren**, swipes met drie vingers om te scrollen en een tik met drie vingers voor **Positie uitspreken**. Gebaren die VoiceOver niet heeft, staan op **Niet toegewezen**, en je kunt ze toewijzen.

De eerste keer dat SandFly start, opent het dialoogvenster **Gebaren kiezen**, waar je een indeling kiest. Elke indeling behoudt zijn eigen veranderingen, zodat terugschakelen de gebaren herstelt zoals jij ze hebt ingesteld. Het kiezen van een indeling zet **Swipes met twee vingers in SandFly verwerken** aan voor **VoiceOver-gebaren** en uit voor **SandFly-gebaren**. Gebaren in apps en gebarenprofielen zijn van toepassing op beide indelingen.

#### Swipes met twee vingers in SandFly verwerken {#gestures-two-finger-swipes}

Met **Swipes met twee vingers in SandFly verwerken** uit, gaan swipes met twee vingers naar de app, die er mee scrolt, en tonen de vier swipes met twee vingers op het scherm **Gebaren** "niet aan te passen". Met dit aan voeren swipes met twee vingers de opdrachten uit die erop zijn toegewezen, en zijn de gebaren **Hoekgebaar met 2 vingers** beschikbaar. Deze instelling heeft Android 11 of hoger nodig.

#### Tijdsinstellingen {#gestures-timing}

- **Tijd tussen tikken met één vinger**: hoe lang SandFly wacht op een verdere tik voordat het besluit dat een gebaar met tikken met 1 vinger klaar is, van 0,1 tot 2 seconden. De standaardwaarde is 0,25 seconden. Een kortere tijd maakt dubbeltikken met 1 vinger sneller, maar een trage dubbeltik kan dan als twee tikken tellen. Met een langere tijd activeren twee tikken op een item binnen die tijd het item, en wachten hoektikken en tikken met meerdere vingers langer. Tikken met twee of meer vingers laten altijd minstens 0,25 seconden tussen tikken toe.
- **Vasthoudtijd na een swipe**: hoe lang de vinger rust na een swipe voor een swipe en vasthouden-gebaar, van 0,1 tot 0,5 seconden. De standaardwaarde is 0,25 seconden. Zie [](#gestures-swipe-and-hold).
- **Starttijd van gebaar**: hoe lang een swipe met 1 vinger kan duren om de eerste centimeter te bewegen voordat de aanraking verkennen via aanraking wordt, van 0,15 tot 0,3 seconden. De standaardwaarde is 0,15 seconden. Kies een langere tijd als trage swipes verkennen via aanraking starten.
- **Tijd tussen swipes in twee halen**: hoe lang na een swipe de tweede swipe van een swipe in twee halen kan beginnen: **Uit**, 0,25, 0,5, 1, 1,5 of 2 seconden. De standaardwaarde is **Uit**, wat swipes in twee halen uitzet en de categorieën ervan verbergt. Zie [](#gestures-two-stroke).

Deze instellingen hebben gebaarherkenning door SandFly nodig; zie [](#gestures-recognition). **Vasthoudtijd na een swipe**, **Starttijd van gebaar** en **Tijd tussen swipes in twee halen** staan niet op horloges.

#### Veegbewegingen herkennen voor het loslaten {#gestures-swipes-before-lift}

Met **Veegbewegingen herkennen voor het loslaten** aan, voert een swipe met 1 vinger zijn opdracht uit zodra de vinger 1 cm heeft bewogen, voordat je deze optilt. Heen-en-weer- en hoekgebaren beginnen als een enkele swipe, zodat deze instelling de categorieën **Heen en weer met 1 vinger**, **Hoekgebaar met 1 vinger**, **Swipes in drie delen** en **Hoekgebaar met 2 vingers** en de categorieën voor swipes in twee halen met heen-en-weer- en hoekswipes uitzet en verbergt. Een swipe wacht nog steeds op het optillen wanneer de swipe en vasthouden van zijn richting een opdracht heeft, of wanneer een randgebaar nog mogelijk is. Dit staat standaard uit.

Met dit uit, wordt een swipe ook voor het optillen uitgevoerd wanneer geen gebaar dat in die richting begint een opdracht heeft: geen heen-en-weer- of hoekgebaar, geen swipe en vasthouden van beide, en geen swipe in drie delen. Met **SandFly-gebaren** hebben de heen-en-weergebaren opdrachten; met **VoiceOver-gebaren** heeft geen van deze gebaren een opdracht.

Elk gebarenprofiel kan deze instelling voor zichzelf aan of uit zetten; zie [](#gestures-profiles). Niet op horloges.

### Wat een gebaar doet veranderen {#gestures-change}

1. Ga naar **Gebaren**.
2. Activeer de categorie van het gebaar, zoals **3 vingers**, om deze uit te vouwen.
3. Activeer het gebaar. Een lijst met opdrachten opent.
4. Kies een opdracht. **Niet toegewezen** laat het gebaar niets doen.

De lijst begint met **Zoeken** en **Niet toegewezen**, gevolgd door de categorieën van opdrachten. Elke categorie is ingeklapt, behalve die met de opdracht die het gebaar nu uitvoert. Om een gebaar te vinden aan de hand van de opdracht die het uitvoert, gebruik **Zoeken** boven aan het scherm **Gebaren**.

Je kunt een opdracht ook een gebaar geven vanaf het scherm **Commando's**; zie [](#commands).

### Gebaren resetten {#gestures-reset}

**Gebaren** > **Instellingen voor gebaren resetten** zet elk gebaar van de gebarenindeling in gebruik terug op de standaardopdracht, na het dialoogvenster **Gebaren resetten?**. Kies **Resetten** om te bevestigen. Gebaren in apps, gebarenprofielen en de andere instellingen op het scherm veranderen niet.

### Gebaren in apps {#gestures-in-apps}

Een gebaar of sneltoets kan een andere opdracht uitvoeren in één app, terwijl die app op de voorgrond staat. In het meldingenscherm, de snelle instellingen en het vergrendelscherm zijn de gebaren van de app System UI van toepassing.

Zo voeg je gebaren voor een app toe:

1. Ga naar **Gebaren** > **Gebaren in apps**.
2. Kies **App toevoegen**, en kies de app.
3. Kies onder **Gebaren** **Gebaar toevoegen**. De lijst toont elk gebaar met de opdracht ervan.
4. Kies een gebaar, dan een categorie van opdrachten, dan een opdracht. **Niet toegewezen** laat het gebaar niets doen in de app.

Elk gebaar op het scherm van de app toont de opdracht ervan in de app. Activeer het voor **Opdracht kiezen** en **Verwijderen**. Onder **Sneltoetsen** registreert **Sneltoets toevoegen** toetsen voor de app op dezelfde manier; zie [](#keyboards). **App verwijderen** verwijdert alle gebaren en sneltoetsen van de app.

De opdracht **Gebaren voor deze app** opent het scherm van de app op de voorgrond, zodat je een gebaar kunt toevoegen terwijl je de app gebruikt.

**Exporteren naar bestand** en **Importeren uit bestand** slaan de gebaren van apps op in een bestand en lezen ze terug; zie [](#api-item-files). Een gebarenprofiel komt voor gebaren in apps; zie [](#gestures-profiles).

### Gebarenprofielen {#gestures-profiles}

Een gebarenprofiel is een set gebaren die andere opdrachten uitvoeren terwijl het profiel in gebruik is. **Standaard** is geen profiel.

Zo voeg je een gebarenprofiel toe:

1. Ga naar **Gebaren** > **Gebarenprofielen**.
2. Kies **Gebarenprofiel toevoegen**, en typ een naam, of laat de voorgestelde naam staan.
3. Open het profiel, en verander de gebaren die je wilt, zoals op het scherm **Gebaren**.

Elk gebaar in een profiel staat eerst op **Standaard**, en voert uit wat het zonder het profiel uitvoert: de opdracht ervan op het scherm **Gebaren**, in **Gebaren in apps** of in een script. Een gebaar dat op een opdracht staat, voert die opdracht uit, voor gebaren in apps en scripts. De lijst met opdrachten heeft **Standaard** als eerste.

Het scherm van een profiel heeft ook **Naam wijzigen**, **Omhoog verplaatsen**, **Omlaag verplaatsen**, **Exporteren naar bestand** en **Gebarenprofiel verwijderen**, en zijn eigen schakelaar **Veegbewegingen herkennen voor het loslaten**, die het scherm **Gebaren** volgt tot je dit verandert.

Zo verander je het profiel in gebruik:

- **Gebaren** > **Gebarenprofielen** > **Gebarenprofiel in gebruik**.
- De leesinstelling **Gebarenprofiel** en het item **Gebarenprofiel** in het SandFly-menu, getoond zodra er een profiel bestaat.
- De opdrachten **Gebarenprofiel kiezen**, **Volgend gebarenprofiel** en **Vorig gebarenprofiel**, en **Gebarenprofiel gebruiken** voor elk profiel en **Standaard**. Je kunt deze toewijzen aan gebaren, ook gebaren in een profiel, en aan sneltoetsen.

SandFly spreekt de naam van het profiel uit wanneer het verandert. Volgend en vorig gaan door **Standaard** en dan de profielen in de volgorde van het scherm **Gebarenprofielen**. Profielen veranderen vingerafdrukgebaren of sneltoetsen niet. Instellingenprofielen, die andere instellingen veranderen, worden beschreven in [](#profiles).

### App-snelkoppelingen {#gestures-app-shortcuts}

De categorie opdrachten **App-snelkoppelingen** vermeldt de snelkoppelingen die het aanraken en vasthouden van een app-pictogram op het startscherm toont, zoals een nieuw bericht, en de snelkoppelingen die je toevoegt op het scherm **App-snelkoppelingen**. Snelkoppelingen die een app toevoegt terwijl deze draait, zoals recente gesprekken, en snelkoppelingen die alleen de startscherm-app mag openen, worden alleen vermeld vanaf Android 12, via Shizuku dat is gestart met adb of draadloos debuggen, of via root. Een lijst met opdrachten leest ze elke keer dat deze opent. Je kunt ze toewijzen aan gebaren, randmenu's en sneltoetsen.

Zo voeg je een snelkoppeling toe die een app maakt voor het startscherm, zoals een contact of een macro van een andere app:

1. Ga naar **Gebaren** > **App-snelkoppelingen**.
2. Kies **App-snelkoppeling toevoegen**, en kies het soort snelkoppeling.
3. Vul het scherm in dat de app opent.

De snelkoppeling wordt toegevoegd aan de lijst, waar je deze kunt hernoemen of verwijderen. Zie [](#commands) voor de opdrachten die apps en snelkoppelingen openen.

### Gebaren oefenen {#gestures-practice}

**Gebaren** > **Gebaren oefenen** opent een pagina waar je elk gebaar kunt proberen. SandFly zegt wat het gebaar doet in plaats van het uit te voeren. De opdracht **Gebaren oefenen** opent dezelfde pagina.
