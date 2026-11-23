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
- **Insert**: op de meeste toetsenborden, in de groep van zes toetsen boven de pijltoetsen.
- **Numpad Insert**: de 0-toets van het numerieke toetsenblok, met Num Lock uit.
- **Sym**, **Right Alt**, **Right Shift** en **Fn**: toetsen van toetsenbordtelefoons. Zie [](#keyboards-phones).

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
- **Toetsenbordtelefoon-keymap** en **Toetsenbordtelefoon enkele-toets-keymap**: voor telefoons met een ingebouwd toetsenbord zonder Ctrl, Meta of pijltoetsen. Zie [](#keyboards-phones).
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

- **Keymap**: **Een keymap kiezen**, **SandFly-toets kiezen**, **De keymap resetten**, **Sneltoetsen van het systeem bekijken**, **Nieuwe keymap**, **Exporteren naar bestand**, **Importeren uit bestand**, en voor keymaps die je zelf hebt gemaakt, **Keymap hernoemen** en **Keymap verwijderen**. **Sneltoetsen van het systeem bekijken** opent de lijst met de sneltoetsen van Android en van de app die in gebruik is.
- **Sneltoetsen in apps**: sneltoetsen die andere opdrachten uitvoeren in één app. Zie [](#keyboards-app-shortcuts).
- **Zoeken**: vindt een opdracht of een pijltoets op naam of op toetsen, en opent de sneltoets ervan.
- **Navigatie met pijltoetsen**: in keymaps die dit hebben. Zie [](#keyboards-arrow-keys).
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
- Ctrl + Backspace stelt de standaardsneltoets weer in.
- Escape, of **Annuleren**, sluit het dialoogvenster zonder wijziging.

Als een andere opdracht deze toetsen al heeft, toont SandFly "Op dit moment toegewezen aan" en de naam van die opdracht. Als je opslaat, vraagt het dialoogvenster **Toets is al in gebruik** of je de toetsen van die opdracht wilt overnemen; selecteer **OK** om ze toe te wijzen aan de opdracht die je wijzigt.

De sneltoets voor **Browsemodus aan- of uitzetten** kan ook de SandFly-toets twee keer indrukken zijn: druk er twee keer binnen een halve seconde op. Ctrl + Alt en Ctrl + Meta kunnen niet twee keer worden ingedrukt.

#### Sneltoetsen van twee toetsen achter elkaar {#keyboards-sequences}

In de SandFly-keymap en de VoiceOver-keymap, en in keymaps die daarvan gekopieerd zijn, kan een sneltoets de SandFly-toets + R of de SandFly-toets + O zijn, gevolgd door een derde toets, zoals SandFly-toets + O, dan H. Druk de derde toets binnen 3 seconden in, met of zonder de SandFly-toets. De derde toets kan geen Escape, Backspace, Enter of aanpassingstoets zijn.

Om zo'n sneltoets vast te leggen, druk je op SandFly-toets + R of SandFly-toets + O, en dan de derde toets.

#### Sneltoetsen die alleen tijdens bewerken werken {#keyboards-while-editing}

Selecteer **Alleen tijdens bewerken** in het sneltoetsdialoogvenster voor een sneltoets die alleen werkt terwijl een tekstveld wordt bewerkt. Zo'n sneltoets wordt zonder de SandFly-toets ingedrukt, en SandFly neemt de toetsen over van het tekstveld. Buiten tekstvelden werken de toetsen zoals ze zonder de sneltoets zouden werken. De sneltoets wordt getoond met "tijdens bewerken" na de toetsen, zoals "Alt + spatie tijdens bewerken".

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
3. Selecteer **Sneltoets toevoegen** onder **Sneltoetsen** en druk op de toetsen.
4. Kies een opdracht.

Hetzelfde scherm heeft de gebaren van elke app. Zie [](#gestures) voor gebaren in apps.

### Een sneltoets vinden {#keyboards-shortcut-list}

**Lijst met sneltoetsen tonen** opent een doorzoekbare lijst met elke opdracht en de bijbehorende sneltoets. De standaardsneltoets is SandFly-toets + Shift + / in de SandFly-keymap, en SandFly-toets + Q in de toetsenbordtelefoon-keymap.

**Sneltoetsen** > **Zoeken** vindt een opdracht op naam of op toetsen in de instellingen. Zie [](#appendix-keyboard-shortcuts) voor de standaardsneltoetsen van elke keymap.

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

SandFly speelt een geluid af of zegt "Browsemodus aan" of "Browsemodus uit".

In de toetsenbordtelefoon-keymaps werken de opdrachten die in andere keymaps de browsemodus nodig hebben, ook zonder die modus.

Twee instellingen in **Navigatie** veranderen als de browsemodus aanstaat:

- **Navigatie** > **Slimme browsemodus**: zet de browsemodus uit als de focus naar een tekstveld in webinhoud gaat, zodat je kunt typen, en weer aan als de focus naar een item gaat dat geen tekstveld is. Standaard aan.
- **Navigatie** > **Browsemodus per app onthouden**: behoudt de browsemodus zoals je die voor het laatst in elke app hebt aan- of uitgezet. In een app waarin je de browsemodus niet hebt aan- of uitgezet, staat die uit, tenzij **Slimme browsemodus** hem aanzet in webinhoud. Standaard uit.

Zie [](#web) voor navigatie op webelementen.

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

Elke keymap heeft zijn eigen opdrachten voor pijltoetsen en zijn eigen schakelaar. De NVDA-, JAWS- en Orca-keymaps hebben geen navigatie met pijltoetsen, omdat hun pijltoetsen de SandFly-focus bewegen; daar zegt de opdracht "Geen navigatie met pijltoetsen in deze keymap".

### Desktopnavigatie {#keyboards-desktop}

In de NVDA-, JAWS- en Orca-keymaps, en in keymaps die daarvan gekopieerd zijn:

- Pijl-omlaag en Pijl-omhoog gaan naar het volgende en vorige item. Pijl-links en Pijl-rechts bewegen per teken, en Ctrl ermee per woord. Ctrl + Home en Ctrl + End gaan naar het eerste en laatste item.
- Tab en Shift + Tab bewegen de invoerfocus en de SandFly-focus naar het volgende en vorige item dat invoerfocus kan krijgen.
- Enter klikt op het item met SandFly-focus, tenzij het invoerfocus heeft.
- Escape is Terug.
- In een tekstveld dat wordt bewerkt, gaan deze toetsen naar het tekstveld.

In de browsemodus bewegen losse letters per element, zoals navigatie met losse letters, snelnavigatietoetsen en structurele navigatie dat doen in NVDA, JAWS en Orca.

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

- **Doorgeven aan systeem**: de volgende toets, of samen ingedrukte toetsen, die je na de sneltoets indrukt, gaan naar de app in plaats van naar SandFly. SandFly zegt "Volgende toetsaanslag wordt genegeerd". De standaardsneltoets is SandFly-toets + Shift + Escape in de SandFly-keymap, en SandFly-toets + P in de toetsenbordtelefoon-keymap.
- **Toetsen zonder SandFly-toets doorgeven aan deze app**: de weergegeven app krijgt elke toets die zonder de SandFly-toets wordt ingedrukt, zoals de pijltoetsen en de letters van de browsemodus, tot je de opdracht opnieuw uitvoert in die app. SandFly zegt "Toetsen worden doorgegeven aan" of "Toetsen worden niet doorgegeven aan" en de naam van de app, en bewaart de keuze voor elke app. Terwijl de leescursor aanstaat, neemt die nog steeds zijn toetsen over. De standaardsneltoets is SandFly-toets + Shift + spatie in de NVDA-keymap, en SandFly-toets + Shift + Q in de toetsenbordtelefoon-keymap.
- **SandFly-toetsenbordinvoer aan- of uitzetten**: elke toets gaat naar Android, ook sneltoetsen met de SandFly-toets, tot je de sneltoets van deze opdracht opnieuw indrukt. SandFly zegt "SandFly-toetsenbordinvoer uit" of "SandFly-toetsenbordinvoer aan". Toetsenbordinvoer staat weer aan zodra SandFly opnieuw start. De standaardsneltoets is SandFly-toets + Shift + K in de SandFly-, NVDA- en JAWS-keymap, en SandFly-toets + Shift + P in de toetsenbordtelefoon-keymap.

Deze opdrachten staan in de categorie **Activeren**. Een script kan SandFly ook elke toets aan zijn app laten doorgeven; SandFly-toetsenbordinvoer weer aanzetten beëindigt dat. Zie [](#scripts).

### Toetsenbordtelefoons {#keyboards-phones}

Toetsenbordtelefoons zijn telefoons met een ingebouwd toetsenbord met letters, Shift, Alt en vaak Sym, maar zonder Ctrl, Meta of pijltoetsen: de Unihertz Titan-telefoons, de BlackBerry KEYone, KEY2 en KEY2 LE, en Zinwa-telefoons zoals de Q25. SandFly herkent deze telefoons en gebruikt daarop de **Toetsenbordtelefoon-keymap**. Je kunt op elke telefoon beide toetsenbordtelefoon-keymaps kiezen.

- **Toetsenbordtelefoon-keymap**: de meest gebruikte sneltoetsen zijn het indrukken van toetsen die niet voor typen worden gebruikt, en werken overal, ook in tekstvelden. De los ingedrukte SandFly-toets gaat naar het volgende item, en Shift + de SandFly-toets naar het vorige item. Enter activeert het item waar de SandFly-focus naartoe is gegaan. Alt twee keer indrukken speelt media af of pauzeert ze, en beantwoordt gesprekken. Alt + Shift vasthouden selecteert de volgende leesinstelling. De andere sneltoetsen zijn de SandFly-toets met een letter, zoals SandFly-toets + J, I en K voor leesinstellingen, en T en B voor de bovenkant en onderkant van het scherm.
- **Toetsenbordtelefoon enkele-toets-keymap**: losse toetsen voeren opdrachten uit, zoals spatie voor het volgende item en Shift + spatie voor het vorige item, H voor de volgende kop, en J, L, I en K voor leesinstellingen. Losse toetsen werken buiten tekstvelden die worden bewerkt. De SandFly-toets met een van deze toetsen doet overal hetzelfde, ook in tekstvelden, zodat SandFly-toets + spatie uit een tekstveld gaat. Shift + Z vergrendelt of ontgrendelt aanraking.

In beide keymaps:

- Enter, ingedrukt na het verplaatsen van de SandFly-focus naar een item, activeert dat item. Na het typen gaat Enter naar de app, zodat deze een bericht verstuurt. Als de SandFly-focus op het tekstveld staat dat wordt bewerkt, gaat Enter naar de app.
- De opdrachten die in andere keymaps de browsemodus nodig hebben, werken ook hier zonder die modus.
- Navigatie met pijltoetsen staat standaard aan. Een druk op een trackpad die Enter verstuurt, zoals op de Zinwa Q25, activeert het item.
- Met Alt als SandFly-toets typt Alt met een letter het cijfer of symbool ervan in een tekstveld dat wordt bewerkt.
- Sneltoetsen met Sym, Fn of Ctrl werken ook in tekstvelden.
- SandFly-toets + L vergrendelt of ontgrendelt aanraking in de toetsenbordtelefoon-keymap. Zie [](#touch).

Zie [](#appendix-keyboard-shortcuts) voor de volledige lijst met sneltoetsen.

#### De SandFly-toets op toetsenbordtelefoons {#keyboards-phone-sandfly-key}

De toetsenbordtelefoon-keymaps bieden Sym, Ctrl, Right Shift, Fn, Right Alt, Alt en Meta aan als SandFly-toets. Standaard kiest SandFly een toets die niet nodig is om op de telefoon te typen:

- Ctrl, op Unihertz-telefoons waarvan de toetsenbordinstellingen de Fn-toets laten werken als Ctrl, behalve de Titan 2 Elite.
- Right Shift, op de Unihertz Titan 2 Elite, de Zinwa Q25 en BlackBerry-telefoons met een rechter Shift-toets.
- Sym, op andere telefoons met een Sym-toets.
- Alt, op telefoons zonder Sym-toets, zoals de eerste Unihertz Titan.

Met Sym als SandFly-toets is de symboollaag van het toetsenbord niet beschikbaar via Sym. Op de Zinwa Q25 verstuurt de toets met het label Sym Right Alt. Op de BlackBerry KEY2 is Fn de Speed-toets.

#### Instellingen voor fysiek toetsenbord in Typen {#keyboards-typing-settings}

**Typen** > **Fysiek toetsenbord** wordt getoond terwijl een fysiek toetsenbord is aangesloten:

- **Lettertoetsen drukken op knoppen van het numerieke toetsenblok**: op toetsenborden waarvan de letters cijfers bevatten, zoals op BlackBerry-, Unihertz- en Zinwa-telefoons waar W, E en R 1, 2 en 3 zijn, drukken lettertoetsen op de knoppen van het pincodescherm van het vergrendelscherm, het beltoetsenblok, waar ze de toetsenbloktonen afspelen, en rekenmachines, waar de Alt-tekens van letters, zoals + en -, op de rekenknoppen drukken. Een letter op het vergrendelscherm voordat het pincodescherm wordt getoond, opent dit scherm. Toetsen met een aanpassingstoets, cijfertoetsen en toetsen die in een tekstveld worden getypt, worden niet overgenomen. Standaard aan, en alleen getoond voor zulke toetsenborden.
- **Enter verstuurt berichten**: in het berichtenveld van een berichten-app, zoals Messages, WhatsApp, Telegram, Signal of Slack, met tekst erin, drukt Enter op de knop Verzenden van de app. Shift + Enter en Alt + Enter gaan naar de app, die een nieuwe regel typt. Dit werkt met elk fysiek toetsenbord, en staat standaard uit.
- **SandFly-toetsenbord**: opent de lijst met toetsenborden in de Android-instellingen, waar je het SandFly-toetsenbord aanzet. Zie [](#keyboards-sandfly-keyboard).
- **Invoer bij ingedrukte toets**: wat een vastgehouden toets typt met het SandFly-toetsenbord.
- **Tekens bij twee keer indrukken**: wat een twee keer ingedrukte toets typt met het SandFly-toetsenbord. Zie [](#keyboards-double-press).
- **Dubbele spatie typt een punt**: met het SandFly-toetsenbord typt spatie die twee keer na een woord wordt ingedrukt een punt en een spatie. Standaard uit.
- **Android-instellingen voor fysiek toetsenbord**: opent de instellingen voor het fysieke toetsenbord van Android, zoals de toetsenbordindeling.

### Het SandFly-toetsenbord {#keyboards-sandfly-keyboard}

Het SandFly-toetsenbord is een invoermethode voor fysieke toetsenborden. Het toont geen toetsen op het scherm. Toetsen typen zoals ze dat zonder het SandFly-toetsenbord doen; het SandFly-toetsenbord voegt toe:

- Precieze informatie over of een tekstveld overneemt wat je typt, zodat sneltoetsen met losse toetsen en Enter in de toetsenbordtelefoon-keymaps op de juiste plekken werken.
- Vastgehouden toetsen die het Alt-teken van de toets typen, of de hoofdletter ervan.
- Twee keer indrukken dat andere tekens typt, zoals G G voor ğ, en een punt voor spatie die twee keer wordt ingedrukt.
- Emoji zoeken en een symbolenlijst.
- Bewerkingsmodus, waarin losse toetsen de cursor bewegen en selecteren, kopiëren, knippen en plakken.

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

**Typen** > **Fysiek toetsenbord** > **Tekens bij twee keer indrukken** toont toetsen en wat elke toets typt als die twee keer wordt ingedrukt met het SandFly-toetsenbord, zoals ğ voor G, ö voor O of ä voor E. **Een toets toevoegen** voegt er een toe: voer het teken van de toets in bij **Toets** en de tekens bij **Typt bij twee keer indrukken**. Elke toets heeft **Bewerken** en **Verwijderen**. De lijst is standaard leeg.

De eerste druk typt het teken van de toets. Een tweede druk op dezelfde toets binnen 350 milliseconden, zonder dat er een andere toets of cursorbeweging tussen zit, zet de tekens voor twee keer indrukken in de plaats. Na een hoofdletter, zoals met Shift, typt de tweede druk de hoofdletterversie, zoals Ğ. Toetsen die niet in de lijst staan, typen zoals gewoonlijk.

Met **Dubbele spatie typt een punt** aan zet spatie die twee keer wordt ingedrukt na een letter of cijfer een punt en een spatie in de plaats van de eerste spatie.

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
- Shift met een beweging selecteert. S begint met selecteren, en SandFly zegt "Selecteren aan"; druk opnieuw op S om te stoppen.
- A selecteert alles, C kopieert, X knipt, V plakt.
- Z maakt ongedaan, Shift + Z herstelt.
- R leest de selectie voor, of de regel als er niets is geselecteerd.
- Verwijdertoetsen verwijderen.
- Enter, Escape of Terug zet de bewerkingsmodus uit.

Andere toetsen die typen doen niets in de bewerkingsmodus. De bewerkingsmodus vereist het SandFly-toetsenbord. In de toetsenbordtelefoon-keymaps is de sneltoets SandFly-toets + C.

### Overige toetsenbordinstellingen {#keyboards-other-settings}

- **Typen** > **Toetsenbordecho** > **Fysiek toetsenbord** stelt in wat SandFly zegt terwijl je op een fysiek toetsenbord typt. Zie [](#typing).
- Elke toets op een fysiek toetsenbord stopt de spraak, behalve aanpassingstoetsen zoals Shift. Ctrl los ingedrukt onderbreekt de spraak. **Spraak onderbreken tijdens typen**, in **Spraak**, stelt in wanneer getypte tekens de spraak stoppen. Zie [](#speech).
- Als er de eerste keer dat SandFly start een fysiek toetsenbord is aangesloten, wordt de toetsenbordtutorial geopend in plaats van de aanraaktutorial. Als je de tutorial later opent met een toetsenbord aangesloten op een apparaat met een aanraakscherm, biedt SandFly **Tutorial over aanraakbediening** en **Tutorial over toetsenbordbediening** aan. Zie [](#getting-started).
