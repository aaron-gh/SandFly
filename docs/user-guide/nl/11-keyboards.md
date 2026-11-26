## Fysieke toetsenborden {#keyboards}

Je kunt SandFly gebruiken met een Bluetooth- of USB-toetsenbord, of met het ingebouwde toetsenbord van een toetsenbordtelefoon. SandFly voert opdrachten uit via sneltoetsen, beweegt met de pijltoetsen en losse letters waar je keymap die gebruikt, en kan toetsen aan de app die je gebruikt laten. Dit hoofdstuk beschrijft **Sneltoetsen** in Instellingen voor SandFly en de instellingen voor het fysieke toetsenbord in **Typen**. Zie [](#appendix-keyboard-shortcuts) voor de standaardsneltoets van elke opdracht.

Wil je de sneltoetsen stap voor stap leren, gebruik dan de toetsenbordtutorial. Zie [](#getting-started).

### De SandFly-toets {#keyboards-sandfly-key}

De meeste sneltoetsen gebruiken de SandFly-toets samen met andere toetsen, zoals SandFly-toets + Pijl-rechts om naar het volgende item te gaan. De SandFly-toets is een toets, of twee toetsen die je samen ingedrukt houdt, die je voor elke keymap kiest. Dit kan een van deze toetsen zijn, afhankelijk van de keymap:

- **Meta**: de Windows-toets, naast de linker Alt-toets. Op Apple-toetsenborden de Command-toets.
- **Alt**
- **Ctrl + Alt**: beide toetsen samen ingedrukt houden.
- **Ctrl + Meta**: beide toetsen samen ingedrukt houden.
- **Ctrl**
- **Invoegen**: op de meeste toetsenborden, in de groep van zes toetsen boven de pijltoetsen.
- **Insert op numeriek toetsenblok**: de 0-toets van het numerieke toetsenblok, met Num Lock uit.
- **Sym**, **Rechter Alt**, **Rechter Shift** en **Fn**: toetsen van toetsenbordtelefoons. Zie [](#keyboards-phones).

Insert, Numpad Insert, Sym, Right Alt, Right Shift en Fn zijn toetsen die Android niet als aanpassingstoetsen behandelt. SandFly neemt ze over zolang ze ingedrukt zijn, en ze bereiken de app niet.

Met Meta als SandFly-toets opent eenmaal op Meta drukken de lijst met alle apps, zoals Android dat doet. Twee keer snel op de SandFly-toets drukken zet de browsemodus aan of uit in keymaps waar dat de sneltoets daarvoor is. Zie [](#keyboards-browse-mode).

Zie [](#keyboards-choose-sandfly-key) om de SandFly-toets te kiezen.

### Keymaps {#keyboards-keymaps}

Een keymap is een verzameling sneltoetsen. SandFly heeft deze keymaps:

- **SandFly-keymap**: de standaardkeymap. De SandFly-toets is Meta, Insert of Numpad Insert, standaard Meta.
- **VoiceOver-keymap**: de toetsenbordopdrachten van VoiceOver op iPhone, met de SandFly-toets als VO-toets. De SandFly-toets is Ctrl + Alt, Insert of Numpad Insert, standaard Ctrl + Alt. Meta neemt de plaats van Command in. In de browsemodus bewegen losse toetsen zoals Quick Nav met één toets dat doet in VoiceOver.
- **NVDA-keymap**: de laptopindeling van NVDA, waarvoor geen numeriek toetsenblok nodig is, met de SandFly-toets als NVDA-toets. De SandFly-toets is Insert, Numpad Insert of Meta, standaard Insert.
- **JAWS-keymap**: de desktoptoetsaanslagen van JAWS, met de laptoptoetsen van JAWS voor klikken en cursors, met de SandFly-toets als JAWS-toets. De SandFly-toets is Insert, Numpad Insert of Meta, standaard Insert.
- **Orca-keymap**: de laptopindeling van Orca, met de SandFly-toets als Orca-aanpassingstoets. De SandFly-toets is Insert, Numpad Insert, Meta of Alt, standaard Insert.
- **Keymap voor toetsenbordtelefoons** en **Enkeltoets-keymap voor toetsenbordtelefoons**: voor telefoons met een ingebouwd toetsenbord zonder Ctrl, Meta of pijltoetsen. Zie [](#keyboards-phones).
- **BlackBerry-keymap**: toetsen die lijken op die van de BlackBerry 10 Screen Reader, voor toetsenbordtelefoons. De BlackBerry-modus kiest deze keymap op toetsenbordtelefoons. Zie [](#keyboards-phones) en [](#profiles-blackberry-mode).
- Keymaps die je zelf maakt. Zie [](#keyboards-custom-keymaps).

In de NVDA-, JAWS- en Orca-keymaps bewegen de pijltoetsen en Tab zonder de SandFly-toets, zoals op een computer. Zie [](#keyboards-desktop).

Op een toetsenbordtelefoon is de toetsenbordtelefoon-keymap de standaardkeymap. SandFly schakelt een toetsenbordtelefoon eenmalig van de SandFly-keymap naar de toetsenbordtelefoon-keymap, de eerste keer dat het erop start.

#### Een keymap kiezen {#keyboards-choose-keymap}

1. Open **Sneltoetsen** in Instellingen voor SandFly.
2. Selecteer **Een keymap kiezen**.
3. Kies een keymap.

SandFly kondigt de keymap aan die nu actief is. Elke keymap behoudt zijn eigen sneltoetsen, zijn eigen SandFly-toets en zijn eigen instellingen voor navigatie met pijltoetsen, zodat het wijzigen van een keymap geen andere keymap verandert.

#### De SandFly-toets kiezen {#keyboards-choose-sandfly-key}

1. Open **Sneltoetsen** in Instellingen voor SandFly.
2. Selecteer **SandFly-toets kiezen**.
3. Kies een toets.

**SandFly-toets kiezen** stelt de SandFly-toets in van de keymap die in gebruik is. De lijst toont alleen de toetsen die elke standaardsneltoets van de keymap behouden; een keymap die je zelf maakt biedt elke SandFly-toets aan. Elke standaardsneltoets behoudt zijn toetsen, met de nieuwe SandFly-toets in plaats van de oude. Sneltoetsen die je hebt gewijzigd, worden afzonderlijk bewaard voor elke SandFly-toets van een keymap.

#### Eigen keymaps {#keyboards-custom-keymaps}

Je kunt eigen keymaps maken. Een nieuwe keymap begint als een kopie van de keymap die in gebruik is, met zijn sneltoetsen, zijn SandFly-toets en de manier waarop zijn toetsen werken, zoals pijltoetsen die bewegen zoals in de NVDA-keymap.

Een keymap maken:

1. Kies de keymap die je wilt kopiëren. Zie [](#keyboards-choose-keymap).
2. Selecteer **Nieuwe keymap** in **Sneltoetsen**.
3. Voer een naam in, of laat het veld leeg om de weergegeven naam te gebruiken, zoals "Kopie van NVDA-keymap".
4. Selecteer **OK**.

De nieuwe keymap is dan in gebruik. In een keymap die je zelf maakt, kunnen sneltoetsen elke toets gebruiken, met of zonder de SandFly-toets. Sneltoetsen zonder de SandFly-toets werken overal, behalve in een tekstveld dat wordt bewerkt, waar de toetsen naar het tekstveld gaan. In keymaps die gekopieerd zijn van de SandFly- of VoiceOver-keymap werken zulke sneltoetsen met Ctrl of Alt ook in tekstvelden. Aanpassingstoetsen die los worden ingedrukt, gaan altijd naar de app.

Terwijl een keymap die je zelf hebt gemaakt in gebruik is, heeft **Sneltoetsen** ook:

- **Keymap hernoemen**: wijzigt de naam.
- **Keymap verwijderen**: verwijdert de keymap, nadat je dit bevestigt. De standaardkeymap is dan in gebruik.

#### Keymaps exporteren en importeren {#keyboards-keymap-files}

- **Exporteren naar bestand**: slaat de keymap die in gebruik is, met zijn sneltoetsen en SandFly-toets, op in een bestand dat je kiest.
- **Importeren uit bestand**: voegt een keymap toe vanuit een bestand.

Deze zijn niet beschikbaar op smartwatches. Zie [](#api-item-files) voor de bestandsindeling.

#### Een keymap resetten {#keyboards-reset-keymap}

**De keymap resetten** geeft elke sneltoets van de keymap die in gebruik is weer zijn standaardtoetsen, en elke pijltoets zijn standaardopdracht. SandFly vraagt je eerst om dit te bevestigen. Je kunt een reset niet ongedaan maken.

### Het scherm Sneltoetsen {#keyboards-settings}

**Sneltoetsen** in Instellingen voor SandFly heeft:

- **Zoeken**: vindt een opdracht of een pijltoets op naam of op toetsen, en opent de sneltoets ervan.
- **Keymap**: **Een keymap kiezen**, **SandFly-toets kiezen**, **De keymap resetten**, **Sneltoetsen van het systeem bekijken**, **Nieuwe keymap**, **Exporteren naar bestand**, **Importeren uit bestand**, en voor keymaps die je zelf hebt gemaakt, **Keymap hernoemen** en **Keymap verwijderen**. **Sneltoetsen van het systeem bekijken** opent de lijst met de sneltoetsen van Android en van de app die in gebruik is.
- **Sneltoetsen in apps**: sneltoetsen die andere opdrachten uitvoeren in één app. Zie [](#keyboards-app-shortcuts).
- **Toetsenbordoefening**: zet de toetsenbordoefening aan. Zie [](#keyboards-practice).
- **Navigatie met pijltoetsen**: in keymaps die dit hebben. Zie [](#keyboards-arrow-keys).
- **Snel zoeken**: **Toets voor snel zoeken** en **Rondgaan**. Zie [](#keyboards-fast-search).
- De opdrachten, per categorie, elk met de bijbehorende sneltoets. Elke categorie gaat open en dicht. Een opdracht waarvan de sneltoets alleen in de browsemodus werkt, heeft "in de browsemodus" na de naam, zoals "Naar volgende kop gaan in de browsemodus".

Je kunt sneltoetsen alleen wijzigen en de keymap alleen resetten terwijl SandFly aanstaat.

Je kunt de sneltoets van een opdracht ook bekijken en wijzigen op het scherm van de opdracht in **Commando's**. Zie [](#commands).

### Een sneltoets wijzigen {#keyboards-change-shortcut}

1. Open **Sneltoetsen** in Instellingen voor SandFly.
2. Open de categorie van de opdracht, of selecteer **Zoeken** en typ de naam.
3. Selecteer de opdracht.
4. Druk op de nieuwe toetsen.
5. Druk op Enter, of selecteer **Opslaan**.

In het dialoogvenster:

- Backspace wist de sneltoets, zodat de opdracht er geen heeft. **Wissen** doet hetzelfde.
- Ctrl + Backspace stelt de standaardsneltoets weer in, in de SandFly- en VoiceOver-keymap en in keymaps die daarvan gekopieerd zijn.
- Escape, of **Annuleren**, sluit het dialoogvenster zonder wijziging.

Als een andere opdracht deze toetsen al heeft, toont SandFly "Op dit moment toegewezen aan" en de naam van die opdracht. Als je opslaat, vraagt het dialoogvenster **Toets is al in gebruik** of je de toetsen van die opdracht wilt overnemen; selecteer **OK** om ze toe te wijzen aan de opdracht die je wijzigt.

De sneltoets voor **Browsemodus aan- of uitzetten** kan ook de SandFly-toets twee keer indrukken zijn: druk er twee keer binnen een halve seconde op. Ctrl + Alt en Ctrl + Meta kunnen niet twee keer worden ingedrukt.

#### Sneltoetsen van twee toetsen achter elkaar {#keyboards-sequences}

In de SandFly-keymap en de VoiceOver-keymap, en in keymaps die daarvan gekopieerd zijn, kan een sneltoets de SandFly-toets + R of de SandFly-toets + O zijn, gevolgd door een derde toets, zoals SandFly-toets + O, dan H. Druk de derde toets binnen 3 seconden in, met of zonder de SandFly-toets. De derde toets kan geen Escape, Backspace, Enter of aanpassingstoets zijn.

Om zo'n sneltoets vast te leggen, druk je op SandFly-toets + R of SandFly-toets + O, en dan de derde toets.

#### Sneltoetsen die alleen tijdens bewerken werken {#keyboards-while-editing}

Selecteer **Alleen tijdens bewerken** in het sneltoetsdialoogvenster voor een sneltoets die alleen werkt terwijl een tekstveld wordt bewerkt. Zo'n sneltoets wordt zonder de SandFly-toets ingedrukt, en SandFly neemt de toetsen over van het tekstveld. Buiten tekstvelden werken de toetsen zoals ze zonder de sneltoets zouden werken. De sneltoets wordt getoond met "tijdens bewerken" na de toetsen, zoals "Alt + spatie tijdens bewerken".

#### Sneltoetsen die alleen in de browsemodus werken {#keyboards-in-browse-mode}

Selecteer in de toetsenbordtelefoon-keymaps **Alleen in browsemodus** in het sneltoetsdialoogvenster voor een sneltoets die alleen in de browsemodus werkt. Zo'n sneltoets wordt zonder de SandFly-toets ingedrukt. In de browsemodus gaat deze sneltoets voor een sneltoets met dezelfde toetsen zonder **Alleen in browsemodus**, die dan met de SandFly-toets en die toetsen wordt uitgevoerd. Buiten de browsemodus werken de toetsen zoals ze zonder de sneltoets zouden werken. De sneltoets wordt getoond met "in de browsemodus" na de toetsen, zoals "B in de browsemodus".

#### Drukken in toetsenbordtelefoon-keymaps {#keyboards-presses}

In de toetsenbordtelefoon-keymaps kan een sneltoets ook zijn:

- Een aanpassingstoets die los wordt ingedrukt, zoals Sym alleen of Shift + Sym. Zo'n druk bereikt de app, tenzij er een sneltoets voor is.
- Een toets, of aanpassingstoetsen, die snel twee keer worden ingedrukt, zoals Alt twee keer. De eerste druk gaat zoals gewoonlijk naar de app; de tweede voert de sneltoets uit.
- Aanpassingstoetsen die los een halve seconde worden vastgehouden, zoals Alt + Shift vasthouden.
- Een andere toets die wordt vastgehouden, zoals K vasthouden. Terwijl je typt, typen vastgehouden toetsen zoals gewoonlijk.

Om deze vast te leggen, druk je de toetsen op dezelfde manier in het sneltoetsdialoogvenster. In deze keymaps slaat eenmaal op Enter drukken de sneltoets na een moment op; twee keer op Enter drukken legt Enter twee keer vast. Backspace wist de sneltoets.

### Sneltoetsen in apps {#keyboards-app-shortcuts}

**Sneltoetsen** > **Sneltoetsen in apps** stelt sneltoetsen in voor één app. Deze gaan voor de sneltoetsen van de keymap terwijl de app wordt weergegeven. Een sneltoets die in een app is ingesteld op **Niet toegewezen** laat de toetsen aan de app.

1. Selecteer **Sneltoetsen in apps** in **Sneltoetsen**.
2. Selecteer **App toevoegen** en kies de app.
3. Selecteer **Sneltoets toevoegen** onder **Sneltoetsen**, druk op de toetsen en selecteer **Opslaan**.
4. Kies een categorie en dan een opdracht, of kies **Niet toegewezen**.

**Sneltoets toevoegen** is alleen beschikbaar terwijl SandFly aanstaat. Selecteer een sneltoets voor **Opdracht kiezen** of **Verwijderen**. **App verwijderen** verwijdert de gebaren en sneltoetsen van de app.

Hetzelfde scherm heeft de gebaren van elke app. Zie [](#gestures) voor gebaren in apps.

### Een sneltoets vinden {#keyboards-shortcut-list}

**Lijst met sneltoetsen tonen** opent een doorzoekbare lijst met elke opdracht en de bijbehorende sneltoets. De standaardsneltoets is SandFly-toets + Shift + / in de SandFly-, VoiceOver-, NVDA-, JAWS- en Orca-keymap, SandFly-toets + Q in de keymap voor toetsenbordtelefoons en de BlackBerry-keymap, en Q in de enkeltoets-keymap voor toetsenbordtelefoons.

**Sneltoetsen** > **Zoeken** vindt een opdracht op naam of op toetsen in de instellingen. Zie [](#appendix-keyboard-shortcuts) voor de standaardsneltoetsen van elke keymap.

### Toetsenbordoefening {#keyboards-practice}

Terwijl de toetsenbordoefening aanstaat, zegt SandFly elke toets die je indrukt en de opdracht die de toets uitvoert, en voert niets uit. Toetsen bereiken de apps niet. Gebruik dit om de toetsen van een toetsenbord en de sneltoetsen van je keymap te leren.

- Toetsen worden genoemd zoals de lijst met sneltoetsen ze noemt, gevolgd door de opdracht, zoals "Right Shift + W, Venstertitel voorlezen". Toetsen zonder opdracht worden alleen genoemd.
- Een aanpassingstoets wordt genoemd als je deze indrukt, met de opdracht van de los ingedrukte aanpassingstoetsen. De los ingedrukte SandFly-toets wordt ook genoemd als SandFly-toets, zoals "Right Shift, SandFly-toets, Volgend item".
- Twee keer ingedrukte toetsen, vastgehouden toetsen en de tweede toets van een sneltoets van twee toetsen achter elkaar worden met de opdracht genoemd, zoals "Alt twee keer" of "Alt + Shift ingedrukt gehouden".
- Terwijl navigatie met pijltoetsen aanstaat, worden de pijltoetsen genoemd met de opdrachten voor navigatie met pijltoetsen.
- Een vastgehouden toets wordt één keer genoemd.

Om de toetsenbordoefening aan te zetten, selecteer je **Sneltoetsen** > **Toetsenbordoefening** of **Hulp en feedback** > **Toetsenbordoefening**, of druk je op de sneltoets van **Toetsenbordoefening aan- of uitzetten**: SandFly-toets + K in de VoiceOver-keymap, SandFly-toets + 1 in de NVDA- en JAWS-keymap, SandFly-toets + H in de Orca-keymap en SandFly-toets + Shift + K in de toetsenbordtelefoon-keymaps. De SandFly-keymap heeft geen standaardsneltoets. SandFly zegt "Toetsenbordoefening aan".

Om de toetsenbordoefening uit te zetten, druk je opnieuw op de sneltoets, op Terug of op Escape. SandFly zegt "Toetsenbordoefening uit". De toetsenbordoefening gaat ook uit als SandFly opnieuw start of wordt gepauzeerd.

**Toetsenbordoefening aan- of uitzetten** staat in de categorie **SandFly**, zodat je deze opdracht ook aan een gebaar kunt toewijzen of aan het SandFly-menu kunt toevoegen. Volumetoetsen die zonder de SandFly-toets worden ingedrukt, wijzigen het volume zoals gewoonlijk. De les **Toetsenbord oefenen** van de toetsenbordtutorial is anders: die noemt alleen sneltoetsen, op het tutorialscherm. Om die les te openen, kies je **Inhoud** in de toetsenbordtutorial, of voer je **Sneltoetsen oefenen** uit, SandFly-toets + 0 in de SandFly-keymap.

### Browsemodus {#keyboards-browse-mode}

In de browsemodus bewegen losse toetsen zonder de SandFly-toets de SandFly-focus, zoals op een computerschermlezer: in de SandFly-keymap gaat H naar de volgende kop, K naar de volgende link, B naar de volgende knop, D naar het volgende herkenningspunt en F naar het volgende bedieningselement, en Shift met de toets gaat terug. De pijltoetsen bewegen per regel en teken, en Ctrl met Pijl-links of Pijl-rechts per woord. De browsemodus werkt in webinhoud en in apps.

De standaardsneltoets van **Browsemodus aan- of uitzetten** is:

| Keymap | Sneltoets |
| --- | --- |
| SandFly-keymap | SandFly-toets twee keer indrukken |
| VoiceOver-keymap | SandFly-toets + Q |
| NVDA-keymap | SandFly-toets + spatie |
| JAWS-keymap | SandFly-toets + Z |
| Orca-keymap | SandFly-toets + A |
| Enkeltoets-keymap voor toetsenbordtelefoons | SandFly-toets twee keer indrukken; SandFly-toets + Q met Alt als SandFly-toets |

SandFly speelt een geluid af of zegt "Browsemodus aan" of "Browsemodus uit".

In de toetsenbordtelefoon-keymaps werken de opdrachten die in andere keymaps de browsemodus nodig hebben, ook zonder die modus. In de enkeltoets-keymap voor toetsenbordtelefoons verandert de browsemodus wat letters doen: B, C, D, I, K, L, R, T, U, V, X en de cijfertoetsen 1 tot 6 bewegen zoals in de browsemodus van de SandFly-keymap, en H, F, E en G bewegen per kop, bedieningselement, bewerkingsveld en afbeelding, in en buiten de browsemodus. De SandFly-toets met een van deze letters voert de sneltoets uit die de letter buiten de browsemodus heeft, zoals SandFly-toets + B voor de onderkant van het scherm en SandFly-toets + K om op de leesinstelling te bewegen. K en Shift + K in de browsemodus zijn **Naar volgende link gaan, tweede toets** en **Naar vorige link gaan, tweede toets** in **Sneltoetsen**. Zie [](#keyboards-phones).

Twee instellingen in **Navigatie** veranderen als de browsemodus aanstaat:

- **Navigatie** > **Slimme browsemodus**: zet de browsemodus uit als de focus naar een tekstveld in webinhoud gaat, zodat je kunt typen, en weer aan als de focus naar een item gaat dat geen tekstveld is. Standaard aan. Terwijl navigatie met pijltoetsen aanstaat, en in de toetsenbordtelefoon-keymaps, krijgt een tekstveld waar je naartoe gaat geen invoerfocus, zodat de browsemodus aan blijft tot je het veld activeert.
- **Navigatie** > **Browsemodus per app onthouden**: behoudt de browsemodus zoals je die voor het laatst in elke app hebt aan- of uitgezet. In een app waarin je de browsemodus niet hebt aan- of uitgezet, staat die uit, tenzij **Slimme browsemodus** hem aanzet in webinhoud. Standaard uit.

Zie [](#web) voor navigatie op webelementen.

#### Snel zoeken {#keyboards-fast-search}

In de browsemodus beweegt de toets voor snel zoeken met een letter of cijfer de SandFly-focus naar het volgende item dat met die letter of dat cijfer begint. Shift met beide toetsen gaat naar het vorige item. Hoofdletters en accenten maken niet uit: E vindt "Email" en "Écran".

Om de toets te kiezen, ga je naar **Sneltoetsen** > **Snel zoeken** > **Toets voor snel zoeken**. Standaard is dit **Uit**. De lijst heeft Alt, Ctrl en Meta, elk links of rechts, Sym en Fn, behalve toetsen die deel zijn van de SandFly-toets van de keymap. Als je de SandFly-toets wijzigt in een toets die de toets voor snel zoeken bevat, staat snel zoeken uit voor die keymap.

Snel zoeken werkt niet in een tekstveld waarin je typt. Terwijl de toets voor snel zoeken wordt vastgehouden, voeren de letters en cijfers geen andere sneltoetsen uit.

**Sneltoetsen** > **Snel zoeken** > **Rondgaan**: als geen item na de SandFly-focus met de letter begint, gaat het zoeken verder vanaf de bovenkant van het scherm, of vanaf de onderkant bij teruggaan. Standaard aan. Als er geen item wordt gevonden, zegt SandFly "Niet gevonden".

### Navigatie met pijltoetsen {#keyboards-arrow-keys}

Terwijl navigatie met pijltoetsen aanstaat, voeren de pijltoetsen opdrachten uit zonder de SandFly-toets: los, twee samen ingedrukt, of met Ctrl of Alt. De standaardopdrachten zijn die van de pijltoetsen in VoiceOver op iPhone:

| Toetsen | Opdracht |
| --- | --- |
| Pijl-rechts, Pijl-links | Volgend item, vorig item |
| Pijl-omlaag, Pijl-omhoog | Leesinstelling omlaag, omhoog aanpassen |
| Pijl-links + Pijl-rechts samen | **Navigatie met pijltoetsen aan- of uitzetten** |
| Pijl-omhoog + Pijl-omlaag samen | Item activeren |
| Pijl-omhoog + Pijl-rechts, Pijl-omhoog + Pijl-links samen | Volgende leesinstelling, vorige leesinstelling selecteren |
| Ctrl + Pijl-omhoog, Ctrl + Pijl-omlaag | Eerste item, laatste item op scherm |
| Alt + Pijl-omhoog, Alt + Pijl-omlaag | Terugscrollen, verderscrollen |
| Alt + Pijl-links, Alt + Pijl-rechts | Naar rechts scrollen, naar links scrollen |

Pijl-omlaag + Pijl-rechts, Pijl-omlaag + Pijl-links, Ctrl + Pijl-links en Ctrl + Pijl-rechts hebben standaard geen opdracht.

Om navigatie met pijltoetsen aan of uit te zetten, druk je Pijl-links en Pijl-rechts samen in, of zet je **Sneltoetsen** > **Navigatie met pijltoetsen** > **Navigatie met pijltoetsen** aan of uit. **Navigatie met pijltoetsen aan- of uitzetten** is ook een opdracht die je aan een gebaar kunt toewijzen. Navigatie met pijltoetsen staat standaard uit, behalve in de toetsenbordtelefoon-keymaps, waar hij aanstaat voor de pijltoetsen die een trackpad verstuurt, zoals de trackpad van de Zinwa Q25.

De opdracht van een pijltoets wijzigen:

1. Open **Navigatie met pijltoetsen** in **Sneltoetsen**.
2. Selecteer de pijltoetsen, zoals "Pijl-omhoog + Pijl-omlaag".
3. Kies een opdracht uit de lijst, dezelfde lijst als voor een gebaar.

Een toets die deel is van twee samen ingedrukte toetsen voert zijn opdracht uit als je hem loslaat, tenzij je eerst de tweede toets indrukt. Terwijl een tekstveld wordt bewerkt, bewegen losse pijltoetsen de cursor; samen ingedrukte toetsen voeren nog steeds hun opdrachten uit, en Pijl-links + Pijl-rechts zet navigatie met pijltoetsen uit zodra beide 0,4 seconden worden vastgehouden.

Terwijl navigatie met pijltoetsen aanstaat, wordt een tekstveld waar je naartoe gaat voorgelezen zonder dat het veld invoerfocus krijgt, en bewegen de pijltoetsen verder langs het veld, ook waar de app het veld al invoerfocus had gegeven. Het veld wordt bewerkt zodra je het activeert, zoals met Pijl-omhoog + Pijl-omlaag, of erin typt. In de toetsenbordtelefoon-keymaps activeert Enter het veld ook. Een nieuw scherm dat opent met de SandFly-focus op een tekstveld met invoerfocus, begint met het veld in bewerking. In de toetsenbordtelefoon-keymaps typen letters op een nieuw scherm waar een ander item de SandFly-focus heeft, in het tekstveld met invoerfocus, en gaan de pijltoetsen naar dat veld zodra je hebt getypt.

Elke keymap heeft zijn eigen opdrachten voor pijltoetsen en zijn eigen schakelaar. De NVDA-, JAWS- en Orca-keymaps hebben geen navigatie met pijltoetsen, omdat hun pijltoetsen de SandFly-focus bewegen; daar zegt de opdracht "Geen navigatie met pijltoetsen in deze keymap".

### Desktopnavigatie {#keyboards-desktop}

In de NVDA-, JAWS- en Orca-keymaps, en in keymaps die daarvan gekopieerd zijn:

- Pijl-omlaag en Pijl-omhoog gaan naar het volgende en vorige item. Pijl-links en Pijl-rechts bewegen per teken, en Ctrl ermee per woord. Ctrl + Home en Ctrl + End gaan naar het eerste en laatste item.
- Tab en Shift + Tab bewegen de invoerfocus en de SandFly-focus naar het volgende en vorige item dat invoerfocus kan krijgen.
- Enter klikt op het item met SandFly-focus, tenzij het invoerfocus heeft.
- Escape is Terug.
- In een tekstveld dat wordt bewerkt, gaan deze toetsen naar het tekstveld.

In de browsemodus bewegen losse letters per element, zoals navigatie met losse letters, snelnavigatietoetsen en structurele navigatie dat doen in NVDA, JAWS en Orca.

Ctrl + Pijl-omlaag en Ctrl + Pijl-omhoog gaan naar de volgende en vorige alinea. In de NVDA-keymap doen P en Shift + P dat ook, in de browsemodus. Dit zijn **Naar volgende alinea gaan, tweede toets** en **Naar vorige alinea gaan, tweede toets** in **Sneltoetsen**, waar je ze kunt wijzigen of in andere keymaps kunt toewijzen.

#### Objectnavigatie {#keyboards-object-navigation}

De opdrachten van **Objectnavigatie** bewegen de SandFly-focus door de items zoals de app ze ordent, inclusief containers die de SandFly-focus anders overslaat:

- **Naar bovenliggend object gaan**
- **Naar eerste onderliggend object gaan**
- **Naar volgend object gaan**
- **Naar vorig object gaan**

Een container zonder label wordt voorgelezen als "Groep" met het aantal items.

#### Leescursor {#keyboards-review-cursor}

De leescursor leest de tekst op het scherm per regel, woord en teken zoals die is weergegeven, van boven naar onder en van links naar rechts, zonder de SandFly-focus te bewegen. Items die naast elkaar op het scherm staan, worden als één regel voorgelezen. De opdrachten van **Schermoverzicht** zijn:

- **Vorige regel lezen**, **Huidige regel lezen**, **Volgende regel lezen**
- **Vorig woord lezen**, **Huidig woord lezen**, **Volgend woord lezen**
- **Vorig teken lezen**, **Huidig teken lezen**, **Volgend teken lezen**
- **Bovenste regel lezen**, **Onderste regel lezen**, **Begin van regel lezen**, **Einde van regel lezen**
- **Klikken bij leescursor**, **Lang klikken bij leescursor**: klikt op het item bij de leescursor, of tikt daar op het scherm.
- **Leescursor naar SandFly-focus verplaatsen**, **SandFly-focus naar leescursor verplaatsen**
- **Leescursor aanzetten**, **Leescursor uitzetten**
- **Tekst op scherm herkennen**: herkent de tekst in een schermafbeelding van het scherm en toont die om te lezen, inclusief tekst die apps niet doorgeven. Vereist Android 11 of hoger.

Leesopdrachten beginnen waar de SandFly-focus is, tenzij de leescursor aanstaat. Terwijl de leescursor aanstaat, en terwijl herkende tekst wordt getoond, bewegen en klikken deze toetsen de leescursor in plaats van de app te bereiken:

- Pijl-omhoog en Pijl-omlaag: vorige en volgende regel.
- Pijl-links en Pijl-rechts: vorig en volgend teken. Met Ctrl: vorig en volgend woord.
- Home en End: begin en einde van de regel. Met Ctrl: bovenste en onderste regel.
- Page Up en Page Down: bovenste en onderste regel.
- Enter: klikken. Shift + Enter: lang klikken.
- Escape: sluit herkende tekst.

In de JAWS-keymap werkt de leescursor als de JAWS-cursor; in de Orca-keymap als flat review. Deze opdrachten hebben sneltoetsen in de NVDA-, JAWS- en Orca-keymaps. In andere keymaps kun je ze toewijzen in **Sneltoetsen**.

### Toetsen aan apps laten {#keyboards-pass-keys}

Sommige apps gebruiken toetsen die SandFly ook gebruikt. Deze opdrachten laten toetsen aan de app:

- **Doorgeven aan systeem**: de volgende toets, of samen ingedrukte toetsen, die je na de sneltoets indrukt, gaan naar de app in plaats van naar SandFly. SandFly zegt "Volgende toetsaanslag wordt genegeerd".
- **Toetsen zonder SandFly-toets doorgeven aan deze app**: de weergegeven app krijgt elke toets die zonder de SandFly-toets wordt ingedrukt, zoals de pijltoetsen en de letters van de browsemodus, tot je de opdracht opnieuw uitvoert in die app. SandFly zegt "Toetsen worden doorgegeven aan" of "Toetsen worden niet doorgegeven aan" en de naam van de app, en bewaart de keuze voor elke app. Terwijl de leescursor aanstaat, neemt die nog steeds zijn toetsen over.
- **SandFly-toetsenbordinvoer aan- of uitzetten**: elke toets gaat naar Android, ook sneltoetsen met de SandFly-toets, tot je de sneltoets van deze opdracht opnieuw indrukt. SandFly zegt "SandFly-toetsenbordinvoer uit" of "SandFly-toetsenbordinvoer aan". Toetsenbordinvoer staat weer aan zodra SandFly opnieuw start.

De standaardsneltoetsen zijn:

| Keymap | Doorgeven aan systeem | Toetsen zonder SandFly-toets doorgeven aan deze app | SandFly-toetsenbordinvoer aan- of uitzetten |
| --- | --- | --- | --- |
| SandFly-keymap | SandFly-toets + Shift + Escape | Geen | SandFly-toets + Shift + K |
| VoiceOver-keymap | Geen | Geen | SandFly-toets + Shift + ; |
| NVDA-keymap | SandFly-toets + F2 | SandFly-toets + Shift + spatie | SandFly-toets + Shift + K |
| JAWS-keymap | SandFly-toets + 3 | SandFly-toets + Shift + Z | SandFly-toets + Shift + K |
| Orca-keymap | SandFly-toets + Backspace | SandFly-toets + Shift + A | SandFly-toets + Shift + ; |
| Keymap voor toetsenbordtelefoons | SandFly-toets + P | SandFly-toets + Shift + Q | SandFly-toets + Shift + P |
| Enkeltoets-keymap voor toetsenbordtelefoons | SandFly-toets + P | Shift + Q | SandFly-toets + Shift + P |

Deze opdrachten staan in de categorie **Activeren**. Een script kan SandFly ook elke toets aan zijn app laten doorgeven; SandFly-toetsenbordinvoer weer aanzetten beëindigt dat. Zie [](#scripts).

### Een aanraking vasthouden {#keyboards-touch-hold}

**Aanraking op het gefocuste item vasthouden** zet een aanraking op het midden van het gefocuste item, zoals een vinger dat doet, en houdt die aanraking zo lang vast als je kiest. Gebruik dit voor knoppen die werken zolang er een vinger op blijft, zoals de knop voor spraakberichten in Telegram: het opnemen loopt terwijl de aanraking duurt, en het bericht wordt verstuurd als de aanraking wordt losgelaten. **Tikken en vasthouden** doet dit niet.

- Houd de toetsen ingedrukt: de aanraking begint en blijft duren terwijl je de toetsen vasthoudt. Het loslaten van de laatste toets van de sneltoets, zoals V, laat de aanraking los; de SandFly-toets mag je eerst loslaten. SandFly zegt niets.
- Druk de toetsen in en laat ze binnen een halve seconde los: de aanraking blijft duren zonder de toetsen. SandFly zegt "Aanraking vastgehouden". Om de aanraking los te laten, druk je opnieuw op de sneltoets, op Terug of op Escape. SandFly zegt "Aanraking losgelaten".

De standaardsneltoets is SandFly-toets + V in de toetsenbordtelefoon-keymaps; andere keymaps hebben er geen. Terwijl een aanraking wordt vastgehouden, werken andere toetsen zoals gewoonlijk, en wordt aanraking over het hele scherm aan de app doorgegeven. De aanraking wordt losgelaten na 10 minuten, als het item niet meer op het scherm staat, en als SandFly wordt gepauzeerd. Een andere aanraking die naar het scherm wordt gestuurd, zoals **Activeren met een tik**, annuleert de vastgehouden aanraking.

De opdracht staat in de categorie **Activeren**, zodat je deze ook aan een gebaar kunt toewijzen of aan het SandFly-menu kunt toevoegen; zo uitgevoerd blijft de aanraking duren tot je de opdracht opnieuw uitvoert of op Terug of Escape drukt. Dit vereist Android 11 of hoger; op oudere versies, en als het item niet op het scherm staat, zegt SandFly "Niet beschikbaar". Als geen enkel item focus heeft, zegt SandFly "Geen enkel item heeft focus".

### Toetsenbordtelefoons {#keyboards-phones}

Toetsenbordtelefoons zijn telefoons met een ingebouwd toetsenbord met letters, Shift, Alt en vaak Sym, maar zonder Ctrl, Meta of pijltoetsen: de Unihertz Titan-telefoons, de BlackBerry KEYone, KEY2 en KEY2 LE, en Zinwa-telefoons zoals de Q25. SandFly herkent deze telefoons en gebruikt daarop de **Keymap voor toetsenbordtelefoons**. Je kunt op elke telefoon beide toetsenbordtelefoon-keymaps kiezen.

- **Keymap voor toetsenbordtelefoons**: de meest gebruikte sneltoetsen zijn het indrukken van toetsen die niet voor typen worden gebruikt, en werken overal, ook in tekstvelden. De los ingedrukte SandFly-toets gaat naar het volgende item, en Shift + de SandFly-toets naar het vorige item. Enter activeert het item waar de SandFly-focus naartoe is gegaan. Alt twee keer indrukken speelt media af of pauzeert ze, en beantwoordt gesprekken. Alt + Shift vasthouden selecteert de volgende leesinstelling. De andere sneltoetsen zijn de SandFly-toets met een letter, zoals SandFly-toets + J, I en K voor leesinstellingen, en T en B voor de bovenkant en onderkant van het scherm.
- **BlackBerry-keymap**: de keymap voor toetsenbordtelefoons met toetsen die lijken op die van de BlackBerry 10 Screen Reader voor een extern toetsenbord, met de SandFly-toets in plaats van Ctrl. SandFly-toets + A leest vanaf het begin, en SandFly-toets + Shift + A vanaf het gefocuste item. SandFly-toets + Enter leest het gefocuste item voor. SandFly-toets + S pauzeert de spraak, en SandFly-toets + Shift + S verbergt of toont het scherm. SandFly-toets + . zegt de titel van het scherm. SandFly-toets + Shift + T start Snelle taken; zie [](#gestures-quick-tasks). De pijltoetsen, zoals die van de trackpad van de Zinwa Q25, bewegen met Rechts en Links per navigatie-instelling, en kiezen met Omhoog en Omlaag de volgende en vorige navigatie-instelling. Sneltoetsen van de keymap voor toetsenbordtelefoons op die toetsen liggen hier op andere toetsen: lang indrukken op SandFly-toets + Shift + Enter, **Zoeken op scherm** op SandFly-toets + Shift + spatie, **Instellingen voor SandFly openen** op SandFly-toets + Shift + O, en **Afbeelding beschrijven** op SandFly-toets + Z.
- **Enkeltoets-keymap voor toetsenbordtelefoons**: losse toetsen voeren opdrachten uit, zoals spatie voor het volgende item en Shift + spatie voor het vorige item, H voor de volgende kop, en J, L, I en K voor leesinstellingen. Losse toetsen werken buiten tekstvelden die worden bewerkt. De SandFly-toets met een van deze toetsen doet overal hetzelfde, ook in tekstvelden, zodat SandFly-toets + spatie uit een tekstveld gaat. Shift + Z vergrendelt of ontgrendelt aanraking. De twee keer ingedrukte SandFly-toets zet de browsemodus aan of uit. In de browsemodus bewegen letters per element zoals in de SandFly-keymap, zoals B naar de volgende knop en K naar de volgende link, en voert de SandFly-toets met een letter de sneltoets uit die de letter buiten de browsemodus heeft, zoals SandFly-toets + T voor de bovenkant van het scherm. Zie [](#keyboards-browse-mode).

In alle drie keymaps:

- Enter, ingedrukt na het verplaatsen van de SandFly-focus naar een item, activeert dat item. Na het typen gaat Enter naar de app, zodat deze een bericht verstuurt. Als de SandFly-focus op het tekstveld staat dat wordt bewerkt, gaat Enter naar de app. Op een tekstveld waar de SandFly-focus naartoe is gegaan, activeert Enter het veld, ook waar het veld invoerfocus heeft.
- De opdrachten die in andere keymaps de browsemodus nodig hebben, werken ook hier zonder die modus.
- Navigatie met pijltoetsen staat standaard aan. Een druk op een trackpad die Enter verstuurt, zoals op de Zinwa Q25, activeert het item.
- Met Alt als SandFly-toets typt Alt met een letter het cijfer of symbool ervan in een tekstveld dat wordt bewerkt.
- Sneltoetsen met Sym, Fn of Ctrl werken ook in tekstvelden.
- SandFly-toets + L vergrendelt of ontgrendelt aanraking in de toetsenbordtelefoon-keymap en de BlackBerry-keymap, en Shift + Z in de enkeltoets-keymap voor toetsenbordtelefoons. Zie [](#touch).
- SandFly-toets + V houdt een aanraking vast op het gefocuste item, zoals de knop voor spraakberichten in een berichten-app. Zie [](#keyboards-touch-hold).
- SandFly-toets + Y spelt het wachtwoordveld dat wordt bewerkt. Zie [](#keyboards-passwords).

Zie [](#appendix-keyboard-shortcuts) voor de volledige lijst met sneltoetsen.

#### De SandFly-toets op toetsenbordtelefoons {#keyboards-phone-sandfly-key}

De toetsenbordtelefoon-keymaps en de BlackBerry-keymap bieden Sym, Ctrl, Right Shift, Fn, Right Alt, Alt en Meta aan als SandFly-toets. Standaard kiest SandFly een toets die niet nodig is om op de telefoon te typen:

- Ctrl, op Unihertz-telefoons waarvan de toetsenbordinstellingen de Fn-toets laten werken als Ctrl, behalve de Titan 2 Elite.
- Right Shift, op de Unihertz Titan 2 Elite, de Zinwa Q25 en BlackBerry-telefoons met een rechter Shift-toets.
- Sym, op andere telefoons met een Sym-toets.
- Alt, op telefoons zonder Sym-toets, zoals de eerste Unihertz Titan.

Met Sym als SandFly-toets is de symboollaag van het toetsenbord niet beschikbaar via Sym. Op de Zinwa Q25 verstuurt de toets met het label Sym Right Alt. Op de BlackBerry KEY2 is Fn de Speed-toets.

#### Instellingen voor fysiek toetsenbord in Typen {#keyboards-typing-settings}

**Typen** > **Fysiek toetsenbord** wordt getoond terwijl een fysiek toetsenbord is aangesloten:

- **Lettertoetsen drukken op knoppen van het numerieke toetsenblok**: op toetsenborden waarvan de letters cijfers bevatten, zoals op BlackBerry-, Unihertz- en Zinwa-telefoons waar W, E en R 1, 2 en 3 zijn, drukken lettertoetsen op de knoppen van het pincodescherm van het vergrendelscherm, het beltoetsenblok, waar ze de toetsenbloktonen afspelen, en rekenmachines, waar de Alt-tekens van letters, zoals + en -, op de rekenknoppen drukken. Op het vergrendelscherm drukken ook cijfertoetsen op de knoppen van het pincodescherm, en drukken Enter en Backspace op de knoppen Enter en Verwijderen ervan. Een letter of cijfer die wordt ingedrukt voordat het pincodescherm wordt getoond, opent dit scherm en typt het cijfer, of ontgrendelt het apparaat waar geen pincode nodig is, zoals terwijl Extend Unlock het ontgrendeld houdt. Toetsen met een aanpassingstoets, cijfertoetsen buiten het vergrendelscherm en toetsen die in een tekstveld worden getypt, zoals een wachtwoordveld op het vergrendelscherm, worden niet overgenomen. Standaard aan, en alleen getoond voor zulke toetsenborden.
- **Enter verstuurt berichten**: in het berichtenveld van een berichten-app, zoals Messages, WhatsApp, Telegram, Signal of Slack, met tekst erin, drukt Enter op de knop Verzenden van de app. Shift + Enter en Alt + Enter gaan naar de app, die een nieuwe regel typt. Terwijl navigatie met pijltoetsen aanstaat, en in de toetsenbordtelefoon-keymaps, verstuurt Enter op een berichtenveld waar de SandFly-focus naartoe is gegaan pas iets als het veld is geactiveerd of als erin is getypt. Dit werkt met elk fysiek toetsenbord, en staat standaard uit. De verzendtoets van een toetsenbordtelefoon, zoals de groene beltoets van de Zinwa Q25, drukt in dezelfde berichtenvelden op de knop Verzenden, of deze instelling nu aan of uit staat. In die apps drukt de verzendtoets ook op een knop Verzenden die buiten het veld wordt getoond, zoals die voor een spraakbericht dat wordt opgenomen; elders gaat de toets zoals gewoonlijk naar de telefoon.
- **SandFly-toetsenbord**: opent de lijst met toetsenborden in de Android-instellingen, waar je het SandFly-toetsenbord aanzet. Zie [](#keyboards-sandfly-keyboard).
- **Invoer bij ingedrukte toets**: wat een vastgehouden toets typt met het SandFly-toetsenbord.
- **Tekens bij twee keer drukken**: wat een twee keer ingedrukte toets typt met het SandFly-toetsenbord. Zie [](#keyboards-double-press).
- **Twee keer spatie typt een punt**: met het SandFly-toetsenbord typt spatie die twee keer na een woord wordt ingedrukt een punt en een spatie. Standaard uit.
- **Autocorrectie**, **Woordsuggesties**, **Suggestie uitspreken**, **Enter typt de uitgesproken suggestie**, **Taal van de woordenlijst** en **Geleerde woorden verwijderen**: correcties en suggesties voor het woord dat met het SandFly-toetsenbord wordt getypt. **Autocorrectie** en **Woordsuggesties** staan standaard uit. Zie [](#keyboards-word-suggestions).
- **Wachtwoordtekens uitspreken**: met het SandFly-toetsenbord zegt SandFly in wachtwoordvelden elk getypt, verwijderd of gepasseerd teken in plaats van "opsommingsteken". Standaard uit. Zie [](#keyboards-passwords).
- **Android-instellingen voor fysiek toetsenbord**: opent de instellingen voor het fysieke toetsenbord van Android, zoals de toetsenbordindeling.

### Het SandFly-toetsenbord {#keyboards-sandfly-keyboard}

Het SandFly-toetsenbord is een invoermethode voor fysieke toetsenborden. Het toont geen toetsen op het scherm. Toetsen typen zoals ze dat zonder het SandFly-toetsenbord doen; het SandFly-toetsenbord voegt toe:

- Precieze informatie over of een tekstveld overneemt wat je typt, zodat sneltoetsen met losse toetsen en Enter in de toetsenbordtelefoon-keymaps op de juiste plekken werken.
- Vastgehouden toetsen die het Alt-teken van de toets typen, of de hoofdletter ervan.
- Shift die voor een toets wordt ingedrukt en losgelaten, wat die toets met Shift typt, zoals op BlackBerry-toetsenborden. Shift opnieuw indrukken en loslaten voor de toets annuleert dit.
- Twee keer indrukken dat andere tekens typt, zoals G G voor ğ, en een punt voor spatie die twee keer wordt ingedrukt.
- Emoji zoeken en een symbolenlijst.
- Bewerkingsmodus, waarin losse toetsen de cursor bewegen en selecteren, kopiëren, knippen en plakken.
- Autocorrectie en woordsuggesties.
- De tekens van wachtwoordvelden, die Android aan schermlezers alleen als opsommingstekens geeft.

Het SandFly-toetsenbord is niet beschikbaar op smartwatches en tv's.

#### Het SandFly-toetsenbord aanzetten {#keyboards-turn-on-sandfly-keyboard}

1. Sluit het toetsenbord aan, of gebruik het ingebouwde toetsenbord van een toetsenbordtelefoon.
2. Open **Typen** > **Fysiek toetsenbord** > **SandFly-toetsenbord** in Instellingen voor SandFly.
3. Zet **SandFly-toetsenbord** aan in de lijst met toetsenborden.
4. Schakel in een tekstveld over naar het SandFly-toetsenbord met de toetsenbordschakelaar van Android, of met de opdracht **Toetsenbord wisselen**, die overschakelt naar het volgende toetsenbord dat aanstaat. **Toetsenbord wisselen** vereist Android 11 of hoger.

Terwijl het SandFly-toetsenbord in gebruik is, wordt er geen schermtoetsenbord getoond.

#### Vastgehouden toetsen {#keyboards-held-keys}

**Typen** > **Fysiek toetsenbord** > **Invoer bij ingedrukte toets** stelt in wat een vastgehouden teken-toets typt met het SandFly-toetsenbord:

- **Alt-teken**: het Alt-teken van de toets, zoals op BlackBerry-toetsenborden, of de hoofdletter ervan als die er geen heeft. Dit is de standaardinstelling.
- **Hoofdletter**: de hoofdletter ervan.
- **Herhaald teken**: het teken, herhaald, zoals zonder het SandFly-toetsenbord.

De toets typt eerst zijn eigen teken; als Android het begint te herhalen, zet het SandFly-toetsenbord het teken voor vasthouden in de plaats.

#### Twee keer indrukken {#keyboards-double-press}

**Typen** > **Fysiek toetsenbord** > **Tekens bij twee keer drukken** toont toetsen en wat elke toets typt als die twee keer wordt ingedrukt met het SandFly-toetsenbord, zoals ğ voor G, ö voor O of ä voor E. **Een toets toevoegen** voegt er een toe: voer het teken van de toets in bij **Toets**, en 1 tot 16 tekens bij **Twee keer drukken typt**. Selecteer een toets voor **Bewerken** of **Verwijderen**. De lijst is standaard leeg.

De eerste druk typt het teken van de toets. Een tweede druk op dezelfde toets binnen 350 milliseconden, zonder dat er een andere toets of cursorbeweging tussen zit, zet de tekens voor twee keer indrukken in de plaats. Na een hoofdletter, zoals met Shift, typt de tweede druk de hoofdletterversie, zoals Ğ. Toetsen die niet in de lijst staan, typen zoals gewoonlijk.

Met **Twee keer spatie typt een punt** aan zet spatie die twee keer wordt ingedrukt na een letter of cijfer een punt en een spatie in de plaats van de eerste spatie.

Twee keer indrukken werkt niet in wachtwoordvelden.

#### Emoji zoeken en symbolen kiezen {#keyboards-emoji}

**Emoji zoeken** en **Symbool kiezen** openen een lijst in het tekstveld dat wordt bewerkt:

1. Voer **Emoji zoeken** of **Symbool kiezen** uit. SandFly zegt "Emoji zoeken" of "Symbolen". De tekens die je het meest recent hebt gekozen, staan voorop.
2. Typ woorden van de naam van het teken, zoals "thumbs up". Elk getypt woord moet een woord van de naam laten beginnen. Emojinamen zijn in de taal van je telefoon.
3. Druk op de pijltoetsen, of op Tab en Shift + Tab, om door de overeenkomsten te bewegen. SandFly leest elke overeenkomst voor met zijn positie, zoals "1 van 5".
4. Druk op Enter om het teken te typen.

Backspace verwijdert de laatst getypte letter, en sluit de lijst als er niets is getypt. Terug of Escape sluit de lijst. Als het SandFly-toetsenbord niet in gebruik is in een tekstveld, zeggen de opdrachten "Vereist het SandFly-toetsenbord in een tekstveld".

In de toetsenbordtelefoon-keymaps is **Emoji zoeken** SandFly-toets + O, en is **Symbool kiezen** Alt + spatie tijdens bewerken. Op de Zinwa Q25 opent Alt + spatie ook de symbolenlijst. In andere keymaps kun je ze toewijzen in **Sneltoetsen**, in de categorie **Tekstverwerking**.

#### Bewerkingsmodus {#keyboards-editing-mode}

**Bewerkingsmodus aan- of uitzetten** zet de bewerkingsmodus aan in het tekstveld dat wordt bewerkt. SandFly zegt "Bewerkingsmodus aan". In de bewerkingsmodus bewegen en bewerken losse toetsen in plaats van te typen, en SandFly leest de bewegingen voor:

- J en L, of Pijl-links en Pijl-rechts: vorig en volgend teken.
- I en K, of Pijl-omhoog en Pijl-omlaag: vorige en volgende regel.
- U en O: vorig en volgend woord.
- Alt + J en Alt + L: begin en einde van de regel. Alt + I en Alt + K: begin en einde van de tekst.
- T en B: begin en einde van de tekst.
- Home en End: begin en einde van de regel. Met Ctrl: begin en einde van de tekst.
- Shift met een beweging selecteert. S begint met selecteren, en SandFly zegt "Selecteren aan"; druk opnieuw op S om te stoppen.
- A selecteert alles, C kopieert, X knipt, V plakt.
- Z maakt ongedaan, Shift + Z herstelt.
- R leest de selectie voor, of de regel als er niets is geselecteerd.
- Verwijdertoetsen verwijderen.
- Enter, Escape of Terug zet de bewerkingsmodus uit.

Andere toetsen die typen doen niets in de bewerkingsmodus. In een wachtwoordveld zegt R "Wordt niet voorgelezen in wachtwoordvelden", en kopieert C zonder de tekst uit te spreken. De bewerkingsmodus vereist het SandFly-toetsenbord. In de toetsenbordtelefoon-keymaps is de sneltoets SandFly-toets + C.

#### Autocorrectie en woordsuggesties {#keyboards-word-suggestions}

Het SandFly-toetsenbord kan het woord dat je typt corrigeren en er woorden voor voorstellen. Beide staan standaard uit, in **Typen** > **Fysiek toetsenbord**:

- **Autocorrectie**: als je na een verkeerd gespeld woord spatie, een leesteken of Enter typt, komt de correctie in de plaats van het woord, en zegt SandFly de correctie, zoals "receive, gecorrigeerd". Woorden die midden in een zin met een hoofdletter beginnen, woorden met hoofdletters na de eerste letter en woorden die in hoofdletters zijn getypt, blijven zoals getypt. Engelse i, im en dont worden I, I'm en don't.
- **Woordsuggesties**: terwijl je een woord typt, gaan Pijl-omhoog en Pijl-omlaag door correcties en langere woorden die beginnen met wat je hebt getypt, zoals "keyboard, 1 van 8" voor "keyb". Op de Zinwa Q25 verstuurt de trackpad deze toetsen. Enter, of een klik op de trackpad, typt de suggestie met een spatie; spatie of een leesteken typt de suggestie gevolgd door dat teken. Terug of Escape sluit de lijst en behoudt het woord zoals getypt. Elke andere toets sluit de lijst en doet wat die toets doet, zodat Backspace een letter verwijdert en een letter wordt getypt. Terwijl er geen woord wordt getypt, na spatie, een leesteken of een cursorbeweging, bewegen Pijl-omhoog en Pijl-omlaag zoals eerder in de tekst.

**Suggestie uitspreken**: terwijl je een woord typt, zegt SandFly met een lagere toonhoogte de correctie die spatie zou typen, zoals "receive, correctie", of anders de eerste woordsuggestie, zoals "keyboard" voor "keyboa". Typ verder om het woord te behouden, druk op Pijl-omlaag voor andere suggesties, of op spatie om de correctie te typen. De volgende toets stopt de spraak. Kies wanneer de suggestie wordt uitgesproken:

- **Direct**: na elke toets, direct na de toetsenbordecho van de toets, of meteen als toetsenbordecho uit staat.
- 0,25 tot 3 seconden: als je binnen het woord zo lang stopt met typen. De standaardwaarde is 1 seconde.
- **Uit**: nooit.

Dit geldt terwijl **Autocorrectie** of **Woordsuggesties** aanstaat.

**Enter typt de uitgesproken suggestie**, standaard uit: nadat een correctie of suggestie is uitgesproken, typt Enter deze met een spatie, tot je verder typt of de cursor beweegt. Op de Zinwa Q25 verstuurt een klik op de trackpad Enter, zodat een klik de suggestie typt. Een tweede Enter gaat naar de app, zoals om een bericht te versturen. Pijl-omlaag opent de lijst dan bij de suggestie na de uitgesproken suggestie. Met de schakelaar uit gaat Enter zoals eerder naar de app, en opent de eerste Pijl-omlaag de lijst bij de eerste suggestie, de eerste Pijl-omhoog bij de laatste.

Een swipe op de trackpad verstuurt twee of meer pijltoetsaanslagen; in de lijst bewegen aanslagen in dezelfde richting die minder dan 0,1 seconde na elkaar komen één keer.

Om een correctie ongedaan te maken, druk je direct daarna op Backspace. SandFly zegt het woord zoals je het typte, zoals "recieve, correctie ongedaan gemaakt", en het woord wordt nooit meer gecorrigeerd.

Woorden die je typt en kiest, komen hoger in de suggesties. Deze geleerde woorden, en de woorden waarvan je de correctie ongedaan hebt gemaakt, worden alleen op de telefoon bewaard, per taal. **Geleerde woorden verwijderen** verwijdert ze. Er wordt niets geleerd in wachtwoordvelden of in velden die leren uitzetten, zoals bij privé browsen.

**Taal van de woordenlijst** stelt de taal van de woorden in: **Systeemtaal**, de standaardwaarde, of een van Engels (VS), Engels (VK), Duits, Spaans, Frans, Portugees (Brazilië), Portugees (Portugal), Hongaars, Turks, Russisch, Oekraïens en Arabisch, elk vermeld met de naam in die taal, zoals Deutsch. Zonder woordenlijst voor de systeemtaal wordt er niets gecorrigeerd of voorgesteld tot je een taal kiest. Correcties houden rekening met de toetsen van je toetsenbordindeling: een letter die is getypt voor de toets ernaast, wordt als eerste gecorrigeerd. Een letter die zonder accent is getypt, zoals fur voor für, wordt ook gecorrigeerd.

Autocorrectie en woordsuggesties worden niet gebruikt in velden voor wachtwoorden, e-mailadressen, webadressen en getallen, en ook niet in velden die zijn gemarkeerd voor geen suggesties.

Zonder pijltoetsen gebruik je de opdrachten **Volgende woordsuggestie**, **Vorige woordsuggestie** en **Woordsuggestie typen**, in de categorie **Tekstverwerking**. Ze hebben geen standaardsneltoetsen: wijs ze toe in **Sneltoetsen**, waar **Alleen tijdens bewerken** de toetsen elders voor typen vrijhoudt, of wijs ze toe aan gebaren. **Woordsuggestie typen** typt de laatst uitgesproken suggestie, of de eerste suggestie voor het getypte woord. Met aanraking voeg je de leesinstelling **Woordsuggesties** toe, in de groep **Acties voor huidig item** van **Leesinstellingen**: terwijl een woord wordt getypt, swipe je omhoog of omlaag om door de suggesties te gaan, en dubbeltik je om de suggestie te typen die SandFly heeft uitgesproken. Deze leesinstelling staat standaard uit en vereist Android 11 of hoger.

#### Wachtwoordvelden {#keyboards-passwords}

Android geeft schermlezers voor de tekst van een wachtwoordveld alleen opsommingstekens, dus SandFly zegt "opsommingsteken" voor elk teken. Het SandFly-toetsenbord leest de tekst van het veld waarin het typt, en SandFly kan de tekens op twee manieren uitspreken.

**Typen** > **Fysiek toetsenbord** > **Wachtwoordtekens uitspreken**, standaard uit: in een wachtwoordveld dat met het SandFly-toetsenbord wordt bewerkt, zegt SandFly:

- Elk getypt teken, zoals "hoofdletter P", "apenstaartje" of "1". Tekens worden benoemd zoals in andere tekstvelden, en hoofdletters volgen **Uitgebreidheidsniveau** > **Hoofdletters**.
- Elk teken dat met Backspace of Delete wordt verwijderd, zoals "a verwijderd".
- Het teken waar de cursor overheen gaat met Pijl-links of Pijl-rechts, de trackpad of de bewerkingsmodus.
- Voor een toets die wordt vastgehouden voor het Alt-teken: het teken en het teken dat werd vervangen, zoals "apenstaartje heeft a vervangen".

Geplakte of ingevulde tekst, een gewist veld, een selectie en bewegingen over meer dan één teken worden zoals eerder uitgesproken, met opsommingstekens.

**Wachtwoordveld spellen**, in de categorie **Tekstverwerking** van de opdrachten, spelt de hele tekst van het wachtwoordveld dat wordt bewerkt, elk teken met de positie, zoals "1 hoofdletter P, 2 a, 3 apenstaartje, 4 1". Hoofdletters worden altijd uitgesproken als "hoofdletter". Dit werkt of **Wachtwoordtekens uitspreken** nu aan of uit staat. In een tekstveld dat geen wachtwoordveld is, zegt SandFly "Geen wachtwoordveld"; zonder het SandFly-toetsenbord in een tekstveld "Vereist het SandFly-toetsenbord in een tekstveld". In de toetsenbordtelefoon-keymaps is de sneltoets SandFly-toets + Y. In andere keymaps wijs je de opdracht toe in **Sneltoetsen**. Je kunt de opdracht ook aan een gebaar toewijzen of aan het SandFly-menu toevoegen.

Wachtwoordtekens worden alleen op deze twee manieren uitgesproken. Ze worden niet bewaard voor **Laatst uitgesproken woordgroep herhalen**, **Laatst uitgesproken woordgroep spellen**, **Laatst uitgesproken woordgroep kopiëren** of **Spraakgeschiedenis**, en SandFly logt alleen hoeveel tekens een veld heeft.

Standaard Android-tekstvelden geven het SandFly-toetsenbord de tekst, en de meeste apps ook. Waar een app geen tekst geeft, zegt SandFly zoals eerder "opsommingsteken", en zegt **Wachtwoordveld spellen** "Tekst van wachtwoordveld niet beschikbaar".

### Overige toetsenbordinstellingen {#keyboards-other-settings}

- **Typen** > **Toetsenbordecho** > **Fysiek toetsenbord** stelt in wat SandFly zegt terwijl je op een fysiek toetsenbord typt. Zie [](#typing).
- Elke toets op een fysiek toetsenbord stopt de spraak, behalve aanpassingstoetsen zoals Shift, Caps Lock, Num Lock, Scroll Lock en de volumetoetsen. **Spraak** > **Spraak onderbreken tijdens typen** stelt in wanneer getypte tekens de spraak stoppen, en **Spraak** > **Spraak onderbreken bij Enter** of Enter dat doet. Ctrl los ingedrukt pauzeert of hervat de spraak. Zie [](#speech-stop).
- Als er de eerste keer dat SandFly start een fysiek toetsenbord is aangesloten, wordt de toetsenbordtutorial geopend in plaats van de aanraaktutorial. Als je de tutorial later opent met een toetsenbord aangesloten op een apparaat met een aanraakscherm, biedt SandFly **Tutorial over aanraakbediening** en **Tutorial over toetsenbordbediening** aan. Zie [](#getting-started).
