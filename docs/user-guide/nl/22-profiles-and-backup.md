## Instellingenprofielen en backups {#profiles}

Een instellingenprofiel is een benoemde set instellingen die SandFly gebruikt in plaats van je instellingen terwijl het profiel in gebruik is, overal of alleen in bepaalde apps en situaties. **Back-up en herstel** slaat alle SandFly-instellingen op in een bestand en herstelt ze vanuit een bestand. Itembestanden bevatten losse soorten items, zoals macro's of gebarenprofielen, om ze te delen of naar een andere telefoon te verplaatsen.

Voor gebarenprofielen, die alleen gebaren wijzigen, zie [](#gestures). Voor spraakprofielen, zie [](#speech).

### Instellingenprofielen {#profiles-settings-profiles}

Een instellingenprofiel bevat alleen de instellingen die erin zijn gewijzigd. Elke andere instelling blijft zoals ze is zonder profielen. **Instellingenprofielen**, in Instellingen voor SandFly, toont de profielen, en markeert de profielen die in gebruik zijn met **In gebruik**.

Een profiel kan op twee manieren worden gebruikt:

- **Nu gebruiken** gebruikt het profiel overal, tot je het stopt. Op deze manier wordt steeds één profiel gebruikt. Zie [](#profiles-use).
- **Automatisch gebruiken** en **Apps** gebruiken het profiel in webcontent, tijdens doorlopend lezen, tijdens het bewerken van tekst, in de apps die je kiest, of terwijl een toetsenbord dat je kiest wordt getoond. Zie [](#profiles-automatic).

Een profiel kan de belangrijkste SandFly-instellingen bevatten, de instellingen van het brailletoetsenbord en de instellingen van de brailleleesregel, inclusief gebaren, sneltoetsen, gebarenprofielen en het gebarenprofiel dat in gebruik is. Een profiel bevat geen API-sleutels, de licentie voor SandFly Pro, het tijdstip van de laatste updatecontrole, geïnstalleerde scripts en hun instellingen, concepten van geluidsthema's, of de status van deze telefoon, zoals welke tutorials en dialoogvensters al zijn getoond.

### BlackBerry-modus {#profiles-blackberry-mode}

**BlackBerry-modus**, onder aan **Instellingenprofielen**, laat SandFly werken zoals de BlackBerry 10 Screen Reader. De gebaren, toetsen en geluiden zijn waarschijnlijk onbekend als je BlackBerry 10 niet hebt gebruikt. Bij het aanzetten opent het dialoogvenster **BlackBerry-modus aanzetten?**; selecteer **Aanzetten** om te bevestigen. De eerste keer maakt SandFly een instellingenprofiel, **BlackBerry-modus**, en gebruikt het meteen. Het profiel stelt in:

- **Gebaren** > **Gebarenindeling** op **BlackBerry-gebaren**, met **Swipes met twee vingers in SandFly verwerken** aan. Zie [](#gestures-layout).
- Op toetsenbordtelefoons de **BlackBerry-keymap**. Zie [](#keyboards-phones).
- Het geluidsthema **BlackBerry**, dat SandFly installeert. Het thema heeft SandFly-geluiden, geen BlackBerry 10-geluiden: geen geluid wanneer de focus beweegt, en geluiden voor schermwijzigingen, klikken, scrollen, het einde van een lijst, fouten en het tonen of verbergen van het toetsenbord. **Geluiden en trillingen van bedieningselementen** staan uit. Zie [](#sound).
- Geen gebruikstips, posities in lijsten en rasters, beschikbare acties of aankondigingen bij het tonen en verbergen van het toetsenbord. Soorten bedieningselementen worden nog uitgesproken, en toonhoogtewijzigingen staan aan.

Elke andere instelling blijft zoals ze is. Om te wijzigen wat BlackBerry-modus instelt, wijzig je de instellingen van het profiel **BlackBerry-modus**; zie [](#profiles-change-settings). BlackBerry-modus uitzetten stopt het gebruik van het profiel, en opnieuw aanzetten gebruikt het profiel met je wijzigingen. Om opnieuw met de standaardwaarden te beginnen, verwijder je het profiel. **BlackBerry-modus aan- of uitzetten**, onder **SandFly** op het scherm **Commando's**, doet hetzelfde vanaf een gebaar of toets, en opent ook het dialoogvenster voordat de modus aangaat.

BlackBerry-modus wordt overal gebruikt, zoals **Nu gebruiken** een profiel gebruikt, en vervangt dus een ander profiel dat zo wordt gebruikt.

### Voeg een instellingenprofiel toe {#profiles-add}

1. Ga naar **Instellingenprofielen** > **Nieuw instellingenprofiel**.
2. Voer een naam in, of laat het veld leeg om de voorgestelde naam te gebruiken, zoals "Profiel 2".
3. Selecteer **OK**.

Het nieuwe profiel stelt nog niets in. Om instellingen aan het profiel toe te voegen, zie [](#profiles-change-settings).

### Wijzig de instellingen van een profiel {#profiles-change-settings}

1. Ga naar **Instellingenprofielen** en selecteer het profiel.
2. Selecteer **Instellingen wijzigen**.

SandFly zegt "Wijzigingen gaan naar <naam>" en opent Instellingen voor SandFly met het profiel toegepast. Elke instelling die je wijzigt, gaat naar het profiel. Wanneer je teruggaat naar het scherm van het profiel of naar **Instellingenprofielen**, of een andere app opent, zegt SandFly "Wijzigingen opgeslagen in <naam>".

Een instelling die wordt gewijzigd terwijl een profiel in gebruik is dat deze instelt, zoals de spreeksnelheid gewijzigd met de leesinstellingen, wordt ook opgeslagen in dat profiel. Andere instellingen die je wijzigt, gelden zonder profielen.

Om de instellingen van een profiel te bekijken, ga je naar het scherm van het profiel. **Instellingen in dit profiel** toont elke instelling met de titel en de plaats ervan in Instellingen voor SandFly. Om een instelling uit het profiel te verwijderen, selecteer je deze en kies je **Verwijderen**. De instelling heeft dan de waarde die ze heeft zonder het profiel.

### Gebruik een profiel nu {#profiles-use}

1. Ga naar **Instellingenprofielen** en selecteer het profiel.
2. Selecteer **Nu gebruiken**.

Het profiel wordt overal gebruikt, ook in Instellingen voor SandFly. Om het gebruik te stoppen, selecteer je **Niet meer gebruiken** op hetzelfde scherm.

Deze opdrachten, in de groep opdrachten **SandFly**, kiezen het profiel dat overal wordt gebruikt. Ze zijn standaard niet toegewezen. Zie [](#commands).

- **Kies instellingenprofiel**: opent een menu met **Geen instellingenprofiel** en de profielen.
- **Volgend instellingenprofiel** en **Vorig instellingenprofiel**: gaan door de profielen en **Geen instellingenprofiel**.

SandFly zegt de naam van het profiel, zoals "Leesprofiel", of "Geen instellingenprofiel".

### Gebruik een profiel automatisch {#profiles-automatic}

Op het scherm van een profiel:

- **Automatisch gebruiken** > **Webinhoud**: gebruikt het profiel terwijl de focus in webcontent staat.
- **Automatisch gebruiken** > **Doorlopend lezen**: gebruikt het profiel tijdens doorlopend lezen, zoals Lees van boven.
- **Automatisch gebruiken** > **Tekstverwerking**: gebruikt het profiel terwijl de focus in een bewerkingsveld staat, of op het toetsenbord dat erin typt.
- **Apps** > **App toevoegen**: gebruikt het profiel terwijl de app die je kiest wordt getoond. Om een app te verwijderen, selecteer je deze en kies je **Verwijderen**.
- **Toetsenborden** > **Toetsenbord toevoegen**: gebruikt het profiel terwijl het toetsenbord dat je kiest wordt getoond. De lijst toont de toetsenborden die in Android-instellingen zijn aangezet. Om een toetsenbord te verwijderen, selecteer je dit en kies je **Verwijderen**.

Profielen die automatisch worden gebruikt, worden niet gebruikt in Instellingen voor SandFly, zodat Instellingen voor SandFly je instellingen zonder profielen toont. Ze worden ook niet gebruikt terwijl SandFly uit staat.

#### Wanneer profielen dezelfde instelling instellen {#profiles-order}

Er kunnen meerdere profielen tegelijk in gebruik zijn. Wanneer ze dezelfde instelling instellen, wint het profiel dat met **Nu gebruiken** wordt gebruikt, en daarna het profiel dat hoger staat in de lijst in **Instellingenprofielen**. Om de volgorde te wijzigen, gebruik je **Omhoog verplaatsen** en **Omlaag verplaatsen** op het scherm van een profiel.

### Hernoem, verplaats of verwijder een profiel {#profiles-manage}

Het scherm van een profiel heeft ook:

- **Hernoemen**: wijzigt de naam van het profiel.
- **Omhoog verplaatsen** en **Omlaag verplaatsen**: verplaatsen het profiel in de lijst.
- **Exporteren naar bestand**: slaat het profiel op in een itembestand. Zie [](#profiles-item-files).
- **Verwijderen**: verwijdert het profiel, nadat je dit bevestigt.

### Backup en herstel van instellingen {#profiles-backup}

**Back-up en herstel** slaat alle SandFly-instellingen en je eigen inhoud op in een bestand en vervangt ze door die uit een bestand, zoals om een andere telefoon op dezelfde manier in te stellen. Deze instellingen staan niet op horloges.

#### Exporteer instellingen {#profiles-export}

1. Ga naar **Back-up en herstel** > **Instellingen exporteren**.
2. Kies waar je het bestand wilt opslaan. De voorgestelde naam is `sandfly-settings.zip`.

SandFly zegt "Instellingen geëxporteerd. API-sleutels zijn niet inbegrepen".

Het .zip-bestand bevat elke SandFly-instelling, en:

- Aangepaste labels.
- Instellingenprofielen. De instellingen worden opgeslagen zoals ze zijn zonder profielen, en de profielen apart.
- Geïnstalleerde scripts.
- Geluidsthema's, en de geluiden die je hebt gewijzigd in het Backtalk-thema.
- De achtergrondafbeelding.

Het laat weg:

- API-sleutels, zodat je het bestand kunt delen of synchroniseren.
- De licentie voor SandFly Pro. Activeer SandFly Pro op een andere telefoon als een apart apparaat. Zie [](#pro).
- Het tijdstip van de laatste updatecontrole.
- Gedownloade AI- en spraakherkenningsmodellen, logboeken en probleemrapporten.
- Concepten van geluidsthema's.

#### Importeer instellingen {#profiles-import}

1. Ga naar **Back-up en herstel** > **Instellingen importeren**.
2. Kies het instellingenbestand.

De instellingen in het bestand vervangen je instellingen, en SandFly zegt "Instellingen geïmporteerd". Instellingen die niet in het bestand staan, en de API-sleutels op deze telefoon, blijven zoals ze zijn. De instellingenprofielen die in gebruik zijn, worden opnieuw toegepast over de geïmporteerde instellingen.

Aangepaste labels, instellingenprofielen, scripts en geluidsthema's in het bestand worden toegevoegd. Elk vervangt het item met dezelfde naam of ID op deze telefoon; de overige blijven bestaan. De achtergrondafbeelding in het bestand vervangt die van deze telefoon.

**Instellingen importeren** leest ook een instellingenbestand met de extensie .json, geëxporteerd door een eerdere versie van SandFly, en een instellingenbestand geëxporteerd vanuit Backtalk. De instellingen van een Backtalk-bestand vervangen die van SandFly, en instellingen die alleen SandFly heeft, blijven zoals ze zijn.

Als SandFly zegt "Dit bestand is geen instellingenbestand van SandFly of Backtalk, of kon niet worden gelezen", is het bestand geen instellingenbestand of is het beschadigd, is een .json-bestand groter dan 5 MB, of is een .zip-bestand na uitpakken groter dan 300 MB. Er wordt niets gewijzigd.

### Itembestanden {#profiles-item-files}

Een itembestand bevat SandFly-items van één of meer soorten, zonder andere instellingen. Je kunt items exporteren, delen, bewerken, met de hand of met een AI-chat schrijven, en importeren op elke telefoon met SandFly. Een itembestand is geen instellingenbestand: **Instellingen importeren** leest het niet, en **Importeren uit bestand** leest geen instellingenbestand.

Elk van deze schermen heeft **Exporteren naar bestand** en **Importeren uit bestand**:

| Items | Scherm |
| --- | --- |
| Macro's | **Macro's** |
| Gebarenprofielen | **Gebaren** > **Gebarenprofielen** |
| Gebaren in apps | **Gebaren** > **Gebaren in apps** |
| Sneltoetsen in apps | **Sneltoetsen** > **Sneltoetsen in apps** |
| Keymaps | **Sneltoetsen** (exporteert de keymap die in gebruik is) |
| Instellingenprofielen | **Instellingenprofielen** |
| Spraakwoordenboek | **Spraak** > **Spraakwoordenboek** |
| Symbolenwoordenboek | **Spraak** > **Symbolenwoordenboek** |
| Braillewoordenboek | **Braille** > **Braillewoordenboek** |
| Aangepaste brailletekens | **Braille** > **Brailletoetsenbord** > **Aangepaste brailletekens** |
| Aangepaste spraakopdrachten | **Spraakopdrachten** |

Itembestanden zijn niet beschikbaar op horloges.

#### Exporteer items naar een bestand {#profiles-item-export}

1. Ga naar het scherm van de items.
2. Selecteer **Exporteren naar bestand**.
3. Kies waar je het bestand wilt opslaan.

**Exporteren naar bestand** op een lijstscherm slaat elk item in de lijst op. Op het scherm van een los item, zoals een instellingenprofiel, slaat het dat item op. SandFly zegt "Geëxporteerd", of "Kon niet exporteren".

#### Importeer items uit een bestand {#profiles-item-import}

1. Ga naar een van de schermen in de tabel in [](#profiles-item-files).
2. Selecteer **Importeren uit bestand**.
3. Kies het itembestand.

SandFly importeert elk item in het bestand, vanaf welk scherm je het ook importeert, en zegt wat het heeft geïmporteerd, zoals "2 macro's, 1 gebarenprofiel geïmporteerd". Een geïmporteerd item vervangt het item van dezelfde soort met dezelfde naam, zonder onderscheid tussen hoofdletters en kleine letters. Andere items blijven bestaan. Items in woordenboeken vervangen items met dezelfde te zoeken tekst in dezelfde app. Aangepaste brailletekens vervangen die met dezelfde tekst in dezelfde brailletabel. Aangepaste spraakopdrachten vervangen die met dezelfde zin. Geïmporteerde keymaps zijn altijd aangepaste keymaps.

Na een import toont SandFly wat het heeft weggelaten, zoals onbekende opdrachten, gebaren, toetsen en instellingen, en reguliere expressies die het weigert. Als het bestand een instellingenbestand is, zegt SandFly "Dit is een instellingenbestand. Importeer het met Backup en herstel > Instellingen importeren".

#### Schrijf een itembestand {#profiles-item-reference}

**Back-up en herstel** > **Naslag voor exportbestandsindeling** slaat een document op, `sandfly-file-format.md`, dat itembestanden beschrijft, gevolgd door de gebaren, opdrachten en instellingen van deze telefoon, met de waarde die elke instelling nu heeft. Om een AI-chat items voor je te laten schrijven, geef je deze het bestand met een beschrijving van de items, sla je het antwoord op als bestand, en importeer je het met **Importeren uit bestand**.

De indeling wordt beschreven in [](#api-item-files).
