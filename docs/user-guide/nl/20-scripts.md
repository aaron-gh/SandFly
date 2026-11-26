## Scripts en aangepaste labels {#scripts}

Een script verandert hoe SandFly werkt in één app of in elke app, of voegt opdrachten toe aan SandFly. Een script kan items herlabelen of verbergen, wijzigen wat SandFly zegt, acties, gebaren en sneltoetsen toevoegen, en stappen uitvoeren als er iets op het scherm gebeurt. Je kunt scripts installeren die andere mensen schrijven, of je eigen scripts maken in de scriptbouwer zonder code te schrijven.

Een aangepast label is een naam die je geeft aan een item dat geen naam heeft, of een verkeerde, zoals een ongelabelde knop.

Scripts werken alleen op telefoons en tablets. Op horloges en tv's zegt SandFly "Scripts zijn niet beschikbaar op dit apparaat".

Om scripts in Lua te schrijven, zie [](#api-scripting).

### Scripts {#scripts-overview}

**Scripts** in de instellingen van SandFly toont de geïnstalleerde scripts in twee groepen:

- **App-scripts**: scripts die werken in de apps die ze noemen.
- **Globale plug-ins**: scripts die in elke app werken.

Elk script toont of het aan staat, waar het werkt en zijn versie. Selecteer een script om zijn pagina te openen. Zie [](#scripts-manage).

Het scherm **Scripts** heeft ook:

- **Alle scripts uitschakelen**: stopt alle scripts. Scripts blijven geïnstalleerd en houden hun eigen schakelaars.
- **Nieuw script**: maakt een script in de scriptbouwer. Zie [](#scripts-builder).
- **Nieuw script uit code**: installeert een script dat je als code invoert. Zie [](#scripts-code).
- **Script bouwen met SandFly Pro**: SandFly Pro schrijft een script op basis van je beschrijving. Zie [](#scripts-builder-pro).
- **Script importeren**: installeert een scriptbestand. Zie [](#scripts-install).
- **Scriptbronnen**: toont de plekken om scripts te vinden. Zie [](#scripts-sources).
- **Scripts automatisch bijwerken** en **Controleren op updates**. Zie [](#scripts-updates).
- **Voeg het voorbeeldscript toe**: installeert een script dat de naam zegt van elke app waarnaar je overschakelt, als een script om mee te beginnen. Dit wordt getoond totdat je het installeert.

Scripts veranderen de schermen van SandFly niet, zoals de instellingen van SandFly. Scripts stoppen terwijl het scherm uit staat, behalve scripts die je toestaat te blijven werken, en terwijl SandFly is onderbroken.

### Installeer een script {#scripts-install}

Een script is een `.lua`-bestand van maximaal 1 MB, of een `.zip`-bestand van maximaal 5 MB met een `main.lua`-bestand erin.

Om een scriptbestand te installeren:

1. Ga naar **Scripts** > **Script importeren**.
2. Kies het `.lua`- of `.zip`-bestand.
3. Bekijk het installatiedialoogvenster, en selecteer dan **Installeren**. Zie [](#scripts-install-dialog).

Je kunt ook een scriptbestand openen of delen, of een `https`-link daarnaar, met **SandFly-script installeren**, bijvoorbeeld vanuit een berichten-app, een bestandsbeheerder of de downloads van een browser. Voor een link die een andere app stuurt, toont SandFly het adres en downloadt het script alleen nadat je **Downloaden** selecteert. Een link naar een hele repository werkt niet: open het script in de repository, en deel dan de link ervan.

#### Vind scripts in scriptbronnen {#scripts-sources}

Een scriptbron is een openbare repository van scripts op GitHub, GitLab, Codeberg, Gitea of Bitbucket. **Scripts** > **Scriptbronnen** toont de bronnen. De scriptrepository van SandFly staat standaard in de lijst.

Om een bron toe te voegen:

1. Ga naar **Scripts** > **Scriptbronnen** > **Bron toevoegen**.
2. Voer het adres in van de repository, of van een branch of map daarin, zoals `github.com/eigenaar/repository`.
3. Selecteer **Toevoegen**.

Selecteer een bron om zijn scripts te tonen. Selecteer een script om het te downloaden en het installatiedialoogvenster te openen. Als de lijst niet kan worden geladen, selecteer **Opnieuw proberen**. **Bron verwijderen** verwijdert de bron uit de lijst; scripts die ervan zijn geïnstalleerd, blijven geïnstalleerd.

#### Bekijk een script voor het installeren {#scripts-install-dialog}

Voordat SandFly een script installeert, toont het installatiedialoogvenster:

- Waar het script vandaan komt: een bestand, een link, een scriptbron, of **Meegeleverd met SandFly.**
- Of SandFly het script heeft ondertekend. SandFly ondertekent de scripts in zijn eigen repository. Een ondertekend script toont **Ondertekend door SandFly.**, waar het ook vandaan komt. Andere scripts tonen **Niet ondertekend door SandFly.** Een ondertekend script dat na het ondertekenen is gewijzigd, toont dat de handtekening niet overeenkomt. De auteur die in een script wordt genoemd, is wat de schrijver ervan daar heeft neergezet; alleen de handtekening toont dat een script van SandFly komt.
- **Het kan:** de machtigingen die het script nodig heeft. Machtigingen gemarkeerd "Alleen toestaan voor vertrouwde scripts" geven het script toegang tot gevoelige gegevens of tot heel SandFly. Zie [](#scripts-permissions).
- **Het voegt toe:** de opdrachten die het script toevoegt, met hun gebaren, toetsen, SandFly-menu-items en leesinstellingen.

Als het script is gemaakt voor een andere versie van SandFly, zegt het dialoogvenster dat delen mogelijk niet werken, en is de knop **Toch installeren**.

Het importeren van een script dat al is geïnstalleerd, werkt het bij en behoudt zijn gegevens en instellingen. Het dialoogvenster zegt welke versie het vervangt, toont nieuwe machtigingen, en waarschuwt als de nieuwe versie een andere auteur heeft.

### Werk scripts bij {#scripts-updates}

SandFly controleert de geïnstalleerde scripts dagelijks op nieuwere versies in hun bronnen. Om nu te controleren, ga naar **Scripts** > **Controleren op updates**.

Met **Scripts automatisch bijwerken** aan, installeert SandFly een nieuwere versie zonder te vragen als die dezelfde auteur heeft, geen nieuwe machtigingen nodig heeft, is gemaakt voor deze versie van SandFly, en van de bron van het script komt. Een nieuwere versie uit de repository van SandFly wordt alleen zonder te vragen geïnstalleerd als SandFly die heeft ondertekend. **Scripts automatisch bijwerken** staat standaard aan.

Andere nieuwere versies staan onder **Updates** op het scherm **Scripts**. Selecteer er een om het installatiedialoogvenster te openen.

### Beheer een geïnstalleerd script {#scripts-manage}

Ga naar **Scripts** en selecteer het script. Zijn pagina heeft:

- **Aanzetten**: zet het script aan of uit.
- **Blijven uitvoeren met scherm uit**: houdt het script actief terwijl het scherm uit staat, zoals voor een sprekende klok. Dit wordt alleen getoond voor scripts die het aanbieden, en staat uit als je het script installeert.
- De naam, beschrijving, versie en auteur van het script, en waar het werkt.
- **Code bekijken**: toont het bestand `main.lua` van het script, met het manifest. Zie [](#scripts-code).
- **Code bewerken**: wijzigt de code van het script. Dit wordt niet getoond voor scripts die in de scriptbouwer zijn gemaakt. Zie [](#scripts-code).
- **Bewerken in scriptbouwer**, voor een script gemaakt in de scriptbouwer, of **Kopiëren naar scriptbouwer** voor andere scripts. Zie [](#scripts-builder-manage).
- **Wijzigen met SandFly Pro**: getoond voor een script gemaakt in de scriptbouwer, met SandFly Pro. Zie [](#scripts-builder-pro).
- **Compatibiliteit**: getoond als het script is gemaakt voor een andere versie van SandFly.
- **Startpagina openen**: opent de webpagina van het script, nadat je het adres bevestigt.
- **Instellingen**: de instellingen van het script, als het die heeft.
- **Machtigingen**: een schakelaar voor elke machtiging die het script nodig heeft. Zie [](#scripts-permissions).
- **Commando's**: de commando's van het script en hun gebaren en toetsen. Zie [](#scripts-commands).
- **Eerder uitvoeren** en **Later uitvoeren**: de volgorde van scripts die dezelfde spraak wijzigen. App-scripts werken eerst.
- **Logboek**: de uitvoer en fouten van het script sinds SandFly is gestart. Zie [](#scripts-log).
- **Opgeslagen gegevens wissen**: verwijdert de gegevens die het script heeft opgeslagen.
- **Verwijderen**: verwijdert het script, met zijn code, gegevens en instellingen, nadat je bevestigt.

#### Bekijk en bewerk de code van een script {#scripts-code}

**Code bekijken**, op de pagina van een script, toont het bestand `main.lua`, met het manifest aan het begin, als tekst die je per regel, woord en teken kunt lezen en kunt selecteren.

**Code bewerken**, op de pagina van een script dat niet in de scriptbouwer is gemaakt, opent hetzelfde bestand in een tekstveld. **Opslaan** controleert de code zoals **Script importeren** een scriptbestand controleert, en vervangt de code van het geïnstalleerde script. Het script behoudt de schakelaar, machtigingen, gebaren, toetsen, instellingen en opgeslagen gegevens, en wordt opnieuw uitgevoerd met de nieuwe code. De `id` in het manifest moet gelijk blijven. Als de code machtigingen aanvraagt die het script niet had, toont het installatiedialoogvenster die eerst. Zie [](#scripts-install-dialog). De andere bestanden van een script uit een `.zip`-bestand blijven zoals ze zijn.

**Nieuw script uit code**, op het scherm **Scripts**, opent een leeg tekstveld. Voer een volledig bestand `main.lua` in, met `local manifest = { ... }` voor de code, en selecteer **Opslaan**. SandFly controleert de code zoals een scriptbestand en toont het installatiedialoogvenster. Een manifest met de `id` van een geïnstalleerd script werkt dat script bij.

Als de code niet kan worden geïnstalleerd, toont **Script niet opgeslagen** de reden, en blijft de code open om te wijzigen. Als je teruggaat met wijzigingen die niet zijn opgeslagen, opent **Wijzigingen verwijderen?**: kies **Verwijderen** of **Verder bewerken**.

Met **Scripts automatisch bijwerken** aan vervangt een nieuwere versie uit de bron van het script code die met **Code bewerken** is gewijzigd. Zie [](#scripts-updates).

Om scripts te schrijven, zie [](#api-scripting).

#### Scriptmachtigingen {#scripts-permissions}

Een script kan alleen de machtigingen gebruiken die erin staan. Op de pagina van een script, onder **Machtigingen**, kun je elke machtiging uitzetten. Het uitzetten van een machtiging stopt de delen van het script die haar nodig hebben.

De machtigingen zijn:

- **Het scherm lezen**
- **Klikken, scrollen, typen en de focus verplaatsen**
- **De spraak van SandFly ontvangen en wijzigen**
- **Wachtwoordvelden lezen**
- **Gebaren, toetsen, menu-items en leesinstellingen toevoegen en SandFly-commando's uitvoeren**
- **Internet gebruiken**
- **Het klembord lezen en wijzigen**
- **Teruggaan of naar het startscherm gaan, meldingen openen, een lijst met apps tonen, apps, instellingen en links openen, intents naar apps sturen, het volume, de belmodus en apparaatinstellingen zoals Wi-Fi en de zaklamp wijzigen, media bedienen, timers en wekkers instellen, en gesprekken en berichten starten**
- **Gesproken meldingen lezen en wijzigen**
- **Alle activiteit in elke app zien, inclusief typen**
- **Dialoogvensters boven elke app tonen**
- **Java-code uitvoeren met alle toegang van SandFly, in elke app**: geen andere machtiging beperkt een script met deze machtiging.

Sta de machtigingen voor internet, het klembord, wachtwoorden, systeemacties, meldingen, alle activiteit, dialoogvensters en Java-code alleen toe voor scripts van mensen die je vertrouwt.

#### Scriptcommando's {#scripts-commands}

Een script kan commando's toevoegen, die je kunt uitvoeren met een gebaar, sneltoets, het SandFly-menu of leesinstellingen. De commando's van geïnstalleerde scripts staan ook in de groep **Scripts** van **Commando's**, genoemd "Scriptnaam: commando", waar je ze kunt toewijzen zoals andere commando's. Zie [](#commands).

Om de gebaren en toetsen van een scriptcommando te wijzigen:

1. Ga naar **Scripts**, selecteer het script, en selecteer dan het commando onder **Commando's**.
2. Kies een van deze:
   - Een gebaar of toets in de lijst: **Uitzetten** behoudt het SandFly-commando van dat gebaar of die toets. **Verwijderen** verwijdert het uit het commando.
   - **Voer een gebaar uit of druk op toetsen**: wijst het volgende gebaar toe dat je maakt, of de toetsen die je indrukt met de SandFly-modificatietoets.
   - **Kies gebaar**: toont de gebaren.
   - **Toetsen typen**: voer toetsen in, zoals `sf+shift+n`, `sf+t`, `s` of `shift+n`.
   - **Standaardwaarden herstellen**: brengt de gebaren en toetsen van het script terug. Getoond nadat je ze hebt gewijzigd.

Terwijl een script werkt, gaan zijn gebaren en toetsen voor die van SandFly. Sommige scripts laden alleen om een commando uit te voeren; hun commando's staan alleen in **Commando's**. Als je een commando van een script uitvoert dat niet werkt op het huidige scherm, zegt SandFly "Script wordt niet uitgevoerd op dit scherm".

#### Scriptlogboek en fouten {#scripts-log}

**Logboek**, op de pagina van een script, toont de uitvoer en fouten van het script sinds SandFly is gestart, met de tijd van elke regel. **Logboek kopiëren** kopieert het, bijvoorbeeld om het naar de auteur van het script te sturen. **Logboek wissen** maakt het leeg.

Een script dat herhaaldelijk fouten geeft, wordt uitgezet, en SandFly zegt "<naam> is uitgezet na herhaalde fouten. Bekijk het logboek in Scripts."

### Zet scripts uit {#scripts-turn-off}

- Om één script uit te zetten, zet **Aanzetten** op zijn pagina uit.
- Om alle scripts uit te zetten, zet **Scripts** > **Alle scripts uitschakelen** aan. Elk script houdt zijn eigen schakelaar voor als je scripts weer aanzet.

De opdracht **Alle scripts in- of uitschakelen**, in de groep **SandFly** van commando's, doet hetzelfde als **Alle scripts uitschakelen**, en zegt "Scripts uitgeschakeld" of "Scripts aan". Gebruik dit als een script je verhindert een app te gebruiken. Zie [](#commands).

### Bouw een script {#scripts-builder}

De scriptbouwer maakt een script zonder code. Om er een te beginnen:

1. Ga naar **Scripts** > **Nieuw script**.
2. Voer een naam in, en selecteer dan **OK**.

De scriptbouwer opent. Elke wijziging wordt opgeslagen en direct toegepast. De onderdelen van het script staan in groepen op zijn scherm:

- **Naam**: de naam van het script.
- **Apps**: de apps waarin het script werkt. Zonder app werkt het in **Alle apps**. **App toevoegen** voegt een app toe; elke app kan worden beperkt met **Alleen in activiteit** of **Alleen in venster**.
- **Regels**: wijzigen items. Zie [](#scripts-builder-rules).
- **Leesinstellingen**: leesinstellingen die tussen items van een soort bewegen. **Leesinstelling toevoegen** voegt er een toe, met het item waarnaartoe die beweegt.
- **Commando's**: stappen die worden uitgevoerd via een gebaar, toetsen, het SandFly-menu of een leesinstelling. Zie [](#scripts-builder-commands).
- **Triggers**: stappen die zichzelf uitvoeren. Zie [](#scripts-builder-triggers).
- **Spraakwijzigingen**: wijzigen wat SandFly zegt. Zie [](#scripts-builder-speech).
- **Instellingen**, **Over** en **Code**. Zie [](#scripts-builder-settings).
- **Script**: **Script exporteren** en **Script dupliceren**. Zie [](#scripts-builder-manage).

**Scriptpagina** opent de pagina van het script, met zijn schakelaar, machtigingen en logboek. Een onderdeel dat niet klaar is, zegt wat het nodig heeft, zoals "Niet gebruikt: stel het item in", en wordt weggelaten uit het script totdat het klaar is.

#### Regels {#scripts-builder-rules}

Een regel noemt een item en wijzigt het. Selecteer **Regel toevoegen** om er een toe te voegen.

**Item** noemt de items waarop de regel van toepassing is, met dezelfde tests als een macro: tekst met **Zoeken in** en **Reguliere expressie**, itemtypen, **Exacte tekst**, **Resource-ID**, **Bovenliggende resource-ID**, **Klassenaam**, **Rol**, **Statussen**, **Binnen een item** en **Bevat een item**. Zie [](#macros). **Geïnspecteerd item gebruiken** vult deze in op basis van het item dat het laatst is getoond met **Gefocust item inspecteren** of **Itemdetails kopiëren**. Zie [](#scripts-inspect).

**Wijzigingen** stelt in wat de regel met het item doet:

- **Label**, **In plaats daarvan uitspreken**, **Gebruikstip**, **Status**, **Rol** en **Kop**.
- **Verbergen**: **Het item verbergen**, of **Het item en de items erin verbergen**.
- **Wijzigingen dempen**: stopt SandFly met het voorlezen van wijzigingen van het item, of ook van de items daarbinnen.
- **Lezen als één item**: leest het item en de items daarbinnen als één item voor.
- **Leesvolgorde**: **Lezen vóór een item** of **Lezen na een item**.
- **Dubbeltikken activeert** een ander item, **Optillen om te activeren**, **Lang indrukken na (ms)** en **Op scherm tikken om te activeren**.
- **Spraak wijzigen**: verwijdert, vervangt of herschikt wat SandFly voor het item zegt. Zie [](#scripts-builder-speech).
- **Indelen wat wordt gelezen**: kiest de delen die SandFly voor het item leest, in volgorde. **Een deel toevoegen** voegt een tekst van het item toe, zijn eigen tekst, of een item daarbinnen.

**Acties** voegt items toe aan het menu Acties van het item. **Actie toevoegen** kiest **Wat de actie doet**: **Op een item klikken**, **Lang op een item klikken**, **Focus naar een item verplaatsen**, **Een item vooruit scrollen**, **Een item terug scrollen**, **Tekst uitspreken** of **Tekst van een item uitspreken**, en of de actie voor of na de acties van de app komt. **Knoppen erin als acties** maakt de knoppen binnen het item zijn acties, met **Knoppen om weg te laten**.

Om een actie op te nemen:

1. Selecteer onder **Acties** **Een actie opnemen**. De app van het script opent.
2. Druk of druk lang op de items, met SandFly of door aanraking.
3. Open de melding **Actie wordt opgenomen**.
4. Selecteer **Opslaan**, of **Opnieuw opnemen**.

Als de regel het item noemt dat je hebt geïnspecteerd, heeft de groep **Geïnspecteerd item** **Lezen als één item, met de knoppen als acties** en **Kiezen wat wordt gelezen**, die de delen toont die SandFly voor het item leest: niet-aangevinkte delen worden verwijderd bij elk item zoals dit. **Regel voor** kiest of de regel **Elke rij zoals die waarin het staat** of **Elk item zoals het geïnspecteerde item** noemt.

**Regel dupliceren** kopieert de regel binnen hetzelfde script, **Regel naar script kopiëren** kopieert hem naar een ander gebouwd script, en **Regel verwijderen** verwijdert hem.

#### Commando's {#scripts-builder-commands}

**Commando toevoegen** voegt een commando toe met een titel. Op het scherm van een commando:

- **Stappen**: de stappen die het commando uitvoert, gemaakt in dezelfde editor als de stappen van een macro. Zie [](#macros).
- **Gebaren en sneltoets**: wijst het commando toe op deze telefoon. Zie [](#scripts-commands).
- **Standaardwaarden bij delen**: het gebaar, de toetsen, **In het SandFly-menu** en **Als leesinstelling** die het commando heeft waar het script is geïnstalleerd.
- **Naam wijzigen** en **Verwijderen**.

#### Triggers {#scripts-builder-triggers}

**Trigger toevoegen** voegt stappen toe die zichzelf uitvoeren:

- **Als de focus naar een item gaat**
- **Als de app wordt geopend** en **Als de app wordt gesloten**
- **Als een venster verandert**
- **Als een aankondiging een tekst bevat** en **Als een melding een tekst bevat**
- **Als tekst verandert**, **Als het scherm verandert**, **Als een lijst scrolt**, **Als de cursor beweegt**, **Als op een item wordt geklikt**, **Als lang op een item wordt geklikt** en **Als een bewerkingsveld invoerfocus krijgt**, voor elk item of één item
- **Elk aantal minuten** en **Elke dag op een tijdstip**
- **Als een knopinstelling wordt ingedrukt**

Getimede triggers werken alleen terwijl het script actief is.

#### Spraakwijzigingen {#scripts-builder-speech}

**Spraakwijziging toevoegen** wijzigt wat SandFly zegt. **Spraak die wordt gewijzigd** kiest **Spraak voor een item**, **Meldingen**, optioneel van één **App**, **Aankondigingen** of **Alle spraak**, met **Bevat tekst (leeg: alles)**.

**Bewerkingen** past deze wijzigingen toe in volgorde: **Tekst verwijderen**, **Tekst vervangen**, **Herhaalde delen verwijderen**, **Delen met een tekst verwijderen**, **Eerste delen behouden**, **In plaats daarvan zeggen**, **Ervoor zeggen**, **Erna zeggen** en **Niets zeggen**. Delen zijn de stukken tussen komma's. Bij **In plaats daarvan zeggen**, **Ervoor zeggen** en **Erna zeggen** is `{text}` wat SandFly zou zeggen. **Voorbeeld voor het geïnspecteerde item** toont het resultaat voor het item dat je het laatst hebt geïnspecteerd.

#### Instellingen, Over en Code {#scripts-builder-settings}

- **Instellingen** > **Instelling toevoegen**: voegt een **Schakelaar**, **Lijst**, **Tekst**, **Getal** of **Knop** toe aan de pagina van het script. Stappen gebruiken de waarde van een instelling, en een knop voert een trigger uit.
- **Over**: **Beschrijving**, **Versie**, **Startpagina**, **Alleen laden om een commando uit te voeren** en **Blijven uitvoeren met scherm uit**.
- **Code**: **Lua-code** toegevoegd aan het einde van het script, die SandFly op fouten controleert voordat het dit opslaat, en **Extra machtigingen** die de code nodig heeft.

#### Exporteer, dupliceer en kopieer scripts {#scripts-builder-manage}

- **Script exporteren**, in de scriptbouwer, slaat het script op als een `.zip`-bestand. **Script importeren** installeert het, en het kan nog steeds worden gewijzigd in de scriptbouwer.
- **Script dupliceren** maakt een kopie van het script.
- **Kopiëren naar scriptbouwer**, op de pagina van een script dat niet in de scriptbouwer is gemaakt, maakt een kopie die je kunt wijzigen in de scriptbouwer.
- **Bewerken in scriptbouwer**, op de pagina van een gebouwd script, opent het in de scriptbouwer.

#### Bouw een script met SandFly Pro {#scripts-builder-pro}

Met SandFly Pro schrijft **Script bouwen met SandFly Pro**, op het scherm **Scripts**, een script op basis van je beschrijving van wat het moet doen. **Wijzigen met SandFly Pro**, op de pagina van een gebouwd script en in de scriptbouwer, wijzigt een script. SandFly Pro leest het scherm op de voorgrond en test het script. In zijn melding toont SandFly Pro verzoeken om andere schermen te openen of delen van het script te proberen; selecteer daar **Klaar** of **Beantwoorden** als je dat hebt gedaan.

Het script opent dan in de scriptbouwer onder **Geschreven door SandFly Pro**, met een samenvatting, **Accepteren**, **Wijzigingen vragen** en **Verwijderen**. Tot je het accepteert, werkt het script zonder internet en Java-codemachtigingen. Het bouwen van een script gebruikt veel meer van de maandelijkse gebruikslimiet van SandFly Pro dan andere taken. Zie [](#pro-scripts).

### Inspecteer items {#scripts-inspect}

Deze opdrachten tonen de details die scripts en macro's gebruiken om items te benoemen. Ze staan in de groep **Ontwikkelaarsinstellingen** van commando's, standaard niet toegewezen, en kunnen worden toegevoegd aan het SandFly-menu. Zie [](#commands).

- **Gefocust item inspecteren**: opent het venster **Itemdetails** voor het item met focus, met **Teksten**, **Itemtypen**, **Items erin**, **Scriptdetails** en **App**. Dubbeltik op een regel om de waarde ervan te kopiëren. **Alles kopiëren** kopieert alle details. **Regel voor dit item toevoegen** voegt een regel voor het item toe aan een gebouwd script, of aan een nieuw script voor zijn app, en opent het.
- **Itemdetails kopiëren**: kopieert dezelfde details als tekst, en zegt "Itemdetails gekopieerd".
- **Schermboom kopiëren**: kopieert alle items op het scherm als tekst, één regel per item, en zegt hoeveel items het heeft gekopieerd.

SandFly zegt "Geen enkel item heeft focus" als er geen item is om te inspecteren.

### Aangepaste labels {#scripts-labels}

Een aangepast label vervangt de naam van een item, zoals een ongelabelde knop in een app. SandFly leest het label elke keer dat het het item voorleest. Je kunt items labelen die een resource-ID in de app hebben.

#### Voeg een label toe of bewerk het {#scripts-labels-add}

1. Verplaats de focus naar het item.
2. Voer de opdracht **Labels toevoegen of bewerken** uit, in de groep **Activeren** van commando's. Zie [](#commands).
3. Voer in het dialoogvenster **Label toevoegen** of **Label bewerken** het label in, en selecteer dan **OK**.

Om een label te verwijderen, voer **Labels toevoegen of bewerken** uit op het item, en selecteer dan **Label verwijderen**. SandFly zegt "Kan dit niet labelen" als het item niet gelabeld kan worden.

Om items te labelen vanuit het SandFly-menu, zet **Labels toevoegen of bewerken** aan bij **SandFly-menu**. Dit staat standaard uit. Het SandFly-menu heeft dan **Labels** voor items die gelabeld kunnen worden. Als de focus op een ongelabeld item staat, zegt SandFly dat labels beschikbaar zijn.

#### Beheer aangepaste labels {#scripts-labels-manage}

**Aangepaste labels**, in de instellingen van SandFly, toont de apps die aangepaste labels hebben. Selecteer een app om zijn labels te tonen, met de tijd waarop elk voor het laatst is gewijzigd. Selecteer een label om het te bewerken of te verwijderen.

#### Exporteer en importeer aangepaste labels {#scripts-labels-export}

**Back-up en herstel** > **Instellingen exporteren** neemt aangepaste labels mee. Zie [](#profiles-export). Om alleen de labels te delen, exporteer ze naar een bestand.

- **Aangepaste labels exporteren**: slaat alle aangepaste labels op in een bestand en opent het Android-deelscherm, waar je kiest waar je het naar toe stuurt of opslaat. SandFly zegt "Niets om te exporteren. Je hebt geen labels toegevoegd." als er geen zijn.
- **Aangepaste labels importeren**: leest een labelbestand. Kies in het dialoogvenster **Aangepaste labels importeren in SandFly** **Overschrijven** om je labels te vervangen door die in het bestand als beide hetzelfde item labelen, of **Overslaan** om die van jou te behouden.
- **Geïmporteerde labels terugzetten**: verwijdert de geïmporteerde labels en brengt de labels terug die het importeren verving. Dit wordt getoond na een import.
