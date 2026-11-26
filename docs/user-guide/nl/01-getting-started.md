## Aan de slag {#getting-started}

SandFly is een schermlezer voor Android. Het leest voor wat er op het scherm staat met tekst-naar-spraak, speelt geluiden en trillingen af, en verandert hoe aanraakgebaren werken, zodat je het apparaat kunt gebruiken zonder het scherm te zien. SandFly kan het scherm ook tonen op een brailleleesregel en invoer ontvangen van een brailletoetsenbord op het scherm of een fysiek toetsenbord.

### Apparaten en Android-versies {#getting-started-devices}

SandFly werkt op:

- Telefoons en tablets met Android 8 of hoger.
- Wear OS-horloges. Zie [](#devices-watch).
- Android TV en Google TV. Zie [](#devices-tv).

Sommige functies hebben een nieuwere versie van Android nodig, en sommige hebben Shizuku of root nodig, waarmee SandFly rechten krijgt die Android alleen via adb verleent. De secties die deze functies beschrijven, vermelden dit. Zie [](#updates-shizuku). SandFly Pro is een abonnement dat AI-functies toevoegt. Zie [](#pro).

### SandFly installeren {#getting-started-install}

SandFly wordt gepubliceerd als een APK-bestand op de [SandFly-releasepagina](https://github.com/aaron-gh/SandFly/releases/latest). `sandfly.apk` is voor telefoons, tablets en tv's, en `sandfly-wear.apk` is voor horloges.

Zo installeer je SandFly op een telefoon of tablet:

1. Open op de telefoon de [nieuwste release](https://github.com/aaron-gh/SandFly/releases/latest) in een browser.
2. Download `sandfly.apk`.
3. Open het gedownloade bestand.
4. Als Android dit vraagt, geef de browser of bestandsbeheerder toestemming om apps te installeren, ga dan terug en open het bestand opnieuw.
5. Kies **Installeren**.

SandFly wordt geïnstalleerd als een afzonderlijke app, met eigen instellingen, naast elke andere schermlezer op het apparaat. Daarna installeert SandFly zijn eigen updates. Zie [](#updates).

Opmerking: Op Android 13 en hoger kan Android toegankelijkheidsdiensten blokkeren die vanuit een browser of bestandsbeheerder zijn geïnstalleerd. Als SandFly niet kan worden ingeschakeld in de toegankelijkheidsinstellingen, open dan de app-info van SandFly in de Android-instellingen, open het menu **Meer** en kies **Beperkte instellingen toestaan**. De namen van deze onderdelen kunnen op sommige telefoons anders zijn.

### SandFly inschakelen {#getting-started-turn-on}

Schakel elke andere schermlezer uit voordat je SandFly inschakelt. Twee schermlezers die tegelijk aan staan, lezen allebei het scherm voor en verwerken allebei gebaren.

1. Open de Android-app **Instellingen**.
2. Ga naar **Toegankelijkheid**. Op sommige telefoons staat SandFly in een subsectie zoals **Geïnstalleerde apps** of **Gedownloade apps**.
3. Kies **SandFly**.
4. Schakel de schakelaar in en bevestig.

SandFly zegt "SandFly aan". De eerste keer opent SandFly het dialoogvenster voor de gebarenindeling en de tutorial. Zie [](#getting-started-first-start).

#### SandFly in- en uitschakelen met de snelkoppeling voor toegankelijkheid {#getting-started-shortcut}

Android kan SandFly in- en uitschakelen met een snelkoppeling voor toegankelijkheid, zoals beide volumetoetsen een paar seconden ingedrukt houden, of de toegankelijkheidsknop. Om er een in te stellen, ga naar **Instellingen** > **Toegankelijkheid** > **SandFly** in de Android-instellingen en schakel de snelkoppeling voor SandFly in. Welke snelkoppelingen beschikbaar zijn, hangt af van de versie van Android en de telefoon.

### Wat er de eerste keer gebeurt {#getting-started-first-start}

De eerste keer dat SandFly wordt ingeschakeld, worden deze schermen in deze volgorde geopend:

1. Het dialoogvenster **Gebaren kiezen**, op een apparaat met een aanraakscherm. Zie [](#getting-started-gesture-layout).
2. De SandFly-tutorial. Zie [](#getting-started-tutorial).

Als je een eerdere versie van SandFly gebruikte, opent SandFly in plaats van de tutorial de pagina's over de nieuwe functies van de update. Zie [](#getting-started-new-features).

Als Backtalk Aaron is geïnstalleerd, kopieert SandFly eerst de instellingen en aangepaste labels daarvan. Zie [](#getting-started-backtalk).

#### Een gebarenindeling kiezen {#getting-started-gesture-layout}

Het dialoogvenster **Gebaren kiezen** heeft twee knoppen:

- **SandFly-gebaren**: de standaardgebaren van SandFly. Swipes met twee vingers gaan naar de app, waar ze scrollen.
- **VoiceOver-gebaren**: gebaren die overeenkomen met VoiceOver op iPhone. Swipes met twee vingers voeren SandFly-opdrachten uit, zoals voorlezen vanaf het begin van het scherm.

Als je het dialoogvenster sluit zonder een keuze te maken, worden SandFly-gebaren geselecteerd. Als je het dialoogvenster verlaat met Home, wordt het de volgende keer dat SandFly start opnieuw geopend. De tutorial opent na de keuze en leert de gebaren van de gekozen indeling.

Om de indeling later te veranderen, ga naar **Gebaren** > **Gebarenindeling**. Zie [](#gestures).

#### De SandFly-tutorial {#getting-started-tutorial}

De SandFly-tutorial leert de basisgebaren met lessen die je op het scherm oefent. Deze opent de eerste keer dat SandFly wordt ingeschakeld. Om hem later te openen, ga naar **Hulp en feedback** > **SandFly-tutorial**, of gebruik de opdracht **Tutorial openen**.

De tutorial heeft 16 lessen in vijf hoofdstukken:

- **Basis**: Welkom, Verkennen via aanraking, Tussen items navigeren, Items activeren, Spraak pauzeren.
- **Lezen**: Leesinstellingen, Koppen, Doorlopend lezen.
- **Bedieningselementen en tekst**: Lijsten scrollen, Schuifregelaars, Acties, Typen.
- **Navigeren**: Terug, startscherm en meldingen, SandFly-menu, Handige commando's.
- **Oefenen**: Gebaren oefenen.

Elke les geeft een instructie, wacht tot je deze met je eigen gebaren hebt uitgevoerd, en geeft dan de volgende. De instructie is het eerste onderdeel na de naam van de les. SandFly spreekt elke instructie uit, ook terwijl andere audio speelt, en herhaalt deze na 25 seconden zonder gebaar of aanraking, tot drie keer. De instructies noemen de gebaren die in je gebareninstellingen aan elke actie zijn toegewezen. Als een actie geen gebaar heeft, meldt de stap dit, en gaat **Stap overslaan** verder zonder dat gebaar.

Als een les is afgerond, zegt SandFly welk gebaar de volgende les start en verplaatst de focus naar **Volgende les**, gevolgd door de naam van de les. De laatste les heeft **Tutorial afronden**.

In een les leiden gebaren die de tutorial zouden verlaten, zoals Terug, Home, recente apps en meldingen, er niet toe dat je de tutorial verlaat. SandFly zegt wat het gebaar zou hebben gedaan. Spraak uitschakelen en SandFly pauzeren werken op dezelfde manier. Terug sluit nog steeds het toetsenbord en menu's. Om een andere les te kiezen of de tutorial te verlaten, gebruik je **Inhoud**, die de lessen vermeldt, zegt welke zijn afgerond, en een van de lessen opent. **Tutorial sluiten** in **Inhoud** sluit de tutorial.

De tutorial opent opnieuw bij de les waar je was, of bij de volgende les als die al is afgerond.

In **Gebaren oefenen** zegt elk gebaar zijn naam en de opdracht die erop is toegewezen, zonder de opdracht uit te voeren. Naar het volgende en vorige item bewegen en activeren werken nog wel, zodat je **Oefenen beëindigen** kunt bereiken. Om deze les direct te openen, gebruik **Hulp en feedback** > **Gebaren oefenen**, of **Gebaren oefenen** boven aan het scherm **Gebaren**.

Opmerking: De tutorial neemt alleen gebaren aan die SandFly herkent. Als **Gebaren verwerken in SandFly** uit staat in **Ontwikkelaarsinstellingen**, herkent Android de gebaren, en kunnen de navigatiegebaren van Android de tutorial verlaten. De lessen over schuifregelaars en acties hebben **Leesinstellingen** > **Leesinstelling automatisch wijzigen** nodig, die standaard aan staat.

#### Toetsenbordtutorial {#getting-started-keyboard-tutorial}

Op een apparaat met een fysiek toetsenbord en zonder aanraakscherm opent SandFly de eerste keer in plaats daarvan de **SandFly-toetsenbordtutorial**. Deze leert de sneltoetsen van je toetsenbordindeling in 15 lessen, waaronder **Lijst met sneltoetsen**, **Browsemodus** voor webpagina's, en **Toetsenbord oefenen**. Als een apparaat zowel een aanraakscherm als een fysiek toetsenbord heeft, opent **SandFly-tutorial** een dialoogvenster met **Tutorial over aanraakbediening** en **Tutorial over toetsenbordbediening**. Zie [](#keyboards).

Horloges en tv's hebben hun eigen tutorials. Zie [](#devices).

#### Pagina's met nieuwe functies {#getting-started-new-features}

Na een update die functies toevoegt, opent SandFly de volgende keer dat het start pagina's die deze beschrijven. Om ze opnieuw te lezen, kies **Nieuwe functies in SandFly** boven aan het hoofdscherm van de instellingen voor SandFly.

### Basisgebaren {#getting-started-basics}

Dit zijn de gebaren van de SandFly-indeling. De VoiceOver-indeling wijst sommige van deze gebaren anders toe. Voor elk standaardgebaar, zie [](#appendix-gestures). Om een gebaar te veranderen, zie [](#gestures).

- **Verkennen via aanraking**: Beweeg één vinger over het scherm. SandFly leest het item onder je vinger voor.
- **Naar het volgende of vorige item gaan**: Swipe naar rechts of naar links.
- **Het item activeren**: Dubbeltik ergens op het scherm. Dubbeltik en houd vast om het item te tikken en vast te houden.
- **Scrollen**: Swipe met twee vingers omhoog of omlaag. SandFly scrolt een lijst ook wanneer je voorbij het laatste item op het scherm swipet.
- **Spraak stoppen**: Tik met twee vingers. Tik opnieuw met twee vingers om door te gaan. Dubbeltikken met drie vingers schakelt spraak uit en aan.
- **Leesinstellingen**: Swipe met drie vingers omlaag, of draai twee vingers met de klok mee op het scherm, om de volgende leesinstelling te kiezen, zoals **Woorden**, **Koppen** of **Spreeksnelheid**. Swipe met drie vingers omhoog, of draai tegen de klok in, voor de vorige. Swipe dan omlaag of omhoog om door het gekozen item te bewegen of de gekozen instelling te wijzigen. Zie [](#reading).
- **SandFly-menu**: Tik met drie vingers, swipe omhoog en dan naar rechts, of swipe omlaag en dan naar rechts. Zie [](#commands).
- **Terug**: Swipe omlaag en dan naar links, of schrob heen en weer met twee vingers.
- **Home**: Swipe omhoog en dan naar links, of swipe omhoog vanaf de onderrand.
- **Recente apps**: Swipe naar links en dan omhoog.
- **Meldingen**: Swipe naar rechts en dan omlaag.
- **Het scherm doorzoeken**: Swipe naar links en dan omlaag.

Voor hoe SandFly voorleest en hoe je door tekst, lijsten en webpagina's beweegt, zie [](#reading) en [](#web).

### Instellingen voor SandFly openen {#getting-started-settings}

Om de instellingen voor SandFly te openen, doe een van de volgende dingen:

- Open **SandFly** in de apps-lijst van het apparaat. Het app-icoon opent de instellingen of SandFly nu aan of uit staat, zodat je instellingen kunt wijzigen voordat je SandFly inschakelt.
- Kies **Instellingen voor SandFly** in het SandFly-menu.
- Ga in de Android-instellingen naar **Toegankelijkheid** > **SandFly** > **Instellingen**.

Het hoofdscherm van de instellingen vermeldt de instellingenschermen: Spraak, Uitgebreidheidsniveau, Geluid en trillen, Audio-uitvoer, Aanraking, Gebaren, Sneltoetsen, Volumetoetsen, Commando's, Leesinstellingen, SandFly-menu, Aangepaste menu's, Macro's, Spraakopdrachten, Navigatie, Typen, Braille, Weergave, Meldingen, Vertaling, Afbeeldings- en schermbeschrijvingen, AI-diensten, SandFly Pro, Scripts, Aangepaste labels, Snelle instellingen, Overzicht, Pauzeren en uitzetten, Instellingenprofielen, Back-up en herstel, Updates en installatie, Shizuku en root, Hulp en feedback en Ontwikkelaarsinstellingen. Sommige schermen worden niet getoond op horloges en tv's, of op apparaten die de hardware voor een functie missen.

#### De instellingen doorzoeken {#getting-started-search}

**Zoeken**, boven aan het hoofdscherm van de instellingen, vindt instellingen en opdrachten op elk instellingenscherm.

1. Kies **Zoeken**.
2. Typ een of meer woorden en zoek dan.
3. Kies een resultaat.

Een resultaat heeft elk woord dat je hebt getypt in zijn titel of beschrijving, ook binnen een woord. Hoofdletters en accenten worden genegeerd. Elk resultaat toont zijn titel en de schermen en categorieën die ernaartoe leiden, zoals **Spraak** > **Spraak stoppen**. Het kiezen van een instelling opent het scherm ervan met de instelling gefocust. Het kiezen van een opdracht opent deze in **Commando's**.

De schermen **Gebaren** en **Sneltoetsen** hebben hun eigen **Zoeken**, die een gebaar of sneltoets vindt op naam of op de opdracht die erop is toegewezen.

### De gebruikershandleiding {#getting-started-user-guide}

Deze handleiding opent vanuit **Hulp en feedback** > **Gebruikershandleiding** in de instellingen voor SandFly. Deze begint met een inhoudsopgave, en links in de tekst gaan naar de secties die ze noemen. De handleiding wordt getoond in de taal van SandFly. Hoofdstukken die niet naar die taal zijn vertaald, worden in het Engels getoond.

De meeste instellingenschermen van SandFly hebben **Hulp** in de app-balk, na de titel van het scherm. **Hulp** opent de sectie van deze handleiding over dat scherm. Links in de sectie naar andere secties openen die secties, en **Terug naar inhoud** opent de hele handleiding.

### Overstappen van Backtalk Aaron {#getting-started-backtalk}

SandFly vervangt Backtalk Aaron, dat geen updates meer krijgt. De laatste build van Backtalk Aaron biedt SandFly aan als update, met de melding of het dialoogvenster **Backtalk Aaron is nu SandFly**.

1. Tik in Backtalk Aaron op de updatemelding, of kies **Controleren op updates**, en dan **Installeren**.
2. SandFly wordt naast Backtalk Aaron geïnstalleerd, en de Android-toegankelijkheidsinstellingen openen.
3. Schakel SandFly in. SandFly kopieert de instellingen en aangepaste labels van Backtalk Aaron, en Backtalk Aaron schakelt zichzelf uit.
4. Tik op de melding **Instellingen gekopieerd van Backtalk Aaron** om Backtalk Aaron te verwijderen.

SandFly krijgt stabiele releases. Om ook ontwikkelversies te krijgen, schakel **Updates en installatie** > **Ontwikkelversies** in. Zie [](#updates-channels).

AI-modellen op het apparaat worden niet gekopieerd. Download ze opnieuw in SandFly. Zie [](#descriptions). Als je het brailletoetsenbord gebruikte, schakel dit opnieuw in voor SandFly. Zie [](#braille).

Als je een snelkoppeling voor toegankelijkheid gebruikt, geef SandFly dan toestemming om beveiligde instellingen te wijzigen voordat je het inschakelt. SandFly verplaatst dan de snelkoppelingen voor toegankelijkheid, en het brailletoetsenbord, van Backtalk Aaron naar zichzelf. Voer vanaf een computer uit:

```
adb shell pm grant io.github.aaron_gh.sandfly android.permission.WRITE_SECURE_SETTINGS
```

Zonder deze toestemming stel je de snelkoppeling opnieuw in voor SandFly in de Android-toegankelijkheidsinstellingen.

Zie [](#profiles) om een instellingenbestand te gebruiken dat is geëxporteerd vanuit Backtalk.
