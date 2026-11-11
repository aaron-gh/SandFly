/*
 * Copyright 2026 SandFly contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.android.accessibility.utils.output

/**
 * The letters each language is written with, in lower case, from the standard exemplar sets of
 * Unicode CLDR as Android 13's ICU has them, for the languages of every script written by more
 * than one language except Chinese characters. Android's ICU doesn't make these sets public.
 *
 * Each line is a language tag, the script it is written in, and its letters.
 */
internal object ExemplarLetters {

  private val byTag: Map<String, Pair<String, Set<Int>>> by lazy {
    DATA.lineSequence()
      .filter { it.isNotBlank() }
      .associate { line ->
        val (tag, script, letters) = line.split(' ', limit = 3)
        tag to (script to letters.codePoints().toArray().toSet())
      }
  }

  /**
   * The letters [language] is written with in [script], or null if they aren't known. A language
   * written in several scripts, such as Serbian, has a set for each.
   */
  fun of(language: String, script: String): Set<Int>? {
    byTag["$language-$script"]?.let {
      return it.second
    }
    val entry = byTag[language] ?: return null
    return if (entry.first == script) entry.second else null
  }

  private const val DATA =
    """
af Latn abcdefghijklmnopqrstuvwxyzáâèéêëîïôöû
agq Latn abcdefghiklmnopstuvwyzàâèêìîòôùûāēěīŋōūǎǐǒǔɔɛɨʉʔ
ak Latn abdefghiklmnoprstuwyɔɛ
am Ethi ሀሁሂሃሄህሆለሉሊላሌልሎሏሐሑሒሓሔሕሖሗመሙሚማሜምሞሟሠሡሢሣሤሥሦሧረሩሪራሬርሮሯሰሱሲሳሴስሶሷሸሹሺሻሼሽሾሿቀቁቂቃቄቅቆቈቊቋቌቍበቡቢባቤብቦቧቨቩቪቫቬቭቮቯተቱቲታቴትቶቷቸቹቺቻቼችቾቿኀኁኂኃኄኅኆኈኊኋኌኍነኑኒናኔንኖኗኘኙኚኛኜኝኞኟአኡኢኣኤእኦኧከኩኪካኬክኮኰኲኳኴኵኸኹኺኻኼኽኾወዉዊዋዌውዎዐዑዒዓዔዕዖዘዙዚዛዜዝዞዟዠዡዢዣዤዥዦዧየዩዪያዬይዮደዱዲዳዴድዶዷጀጁጂጃጄጅጆጇገጉጊጋጌግጎጐጒጓጔጕጠጡጢጣጤጥጦጧጨጩጪጫጬጭጮጯጰጱጲጳጴጵጶጷጸጹጺጻጼጽጾጿፀፁፂፃፄፅፆፈፉፊፋፌፍፎፏፐፑፒፓፔፕፖፗ
ar Arab ءآأؤإئابةتثجحخدذرزسشصضطظعغفقكلمنهوىي
as Beng অআইঈউঊঋএঐওঔকখগঘঙচছজঝঞটঠডঢণতথদধনপফবভমযলশষসহৰৱ
asa Latn abcdefghijklmnoprstuvwyz
ast Latn abcdefghilmnopqrstuvxyzáéíñóúüḥḷ
az Latn abcdefghijklmnopqrstuvxyzçöüğışə
az-Cyrl Cyrl абвгдежзийклмнопрстуфхчшыјғҝүҹһәө
az-Latn Latn abcdefghijklmnopqrstuvxyzçöüğışə
bas Latn abcdefghijklmnoprstuvwyzàáâèéêìíîòóôùúûāēěīńŋōūǎǐǒǔǹɓɔɛ
be Cyrl абвгдежзйклмнопрстуфхцчшыьэюяёіў
bem Latn abcefgijklmnopstuwy
bez Latn abcdefghijklmnopqrstuvwyz
bg Cyrl абвгдежзийклмнопрстуфхцчшщъьюя
bm Latn abcdefghijklmnoprstuwyzŋɔɛɲ
bn Beng অআইঈউঊঋঌএঐওঔকখগঘঙচছজঝঞটঠডঢণতথদধনপফবভমযরলশষসহঽৎৠৡ
bo Tibt ཀཁགངཅཆཇཉཊཋཌཎཏཐདནཔཕབམཙཚཛཝཞཟའཡརལཤཥསཧཨཪ
br Latn abdefghijklmnoprstuvwxyzêñù
brx Deva अआइईउऊऍएऐऑओऔकखगघचछजझञटठडढणतथदधनपफबभमयरलळवशषसह
bs Latn abcdefghijklmnoprstuvzćčđšž
bs-Cyrl Cyrl абвгдежзиклмнопрстуфхцчшђјљњћџ
bs-Latn Latn abcdefghijklmnoprstuvzćčđšž
ca Latn abcdefghijklmnopqrstuvwxyzàçèéíïòóúü
ce Cyrl абвгдежзийклмнопрстуфхцчшщъыьэюяё
ceb Latn abdeghiklmnoprstuwy
cgg Latn abcdefghijklmnopqrstuvwxyz
ckb Arab ئابتجحخدرزسشعغفقلمنوپچڕژڤکگڵھۆیێە
cs Latn abcdefghijklmnopqrstuvwxyzáéíóúýčďěňřšťůž
cy Latn abcdefghijlmnoprstuwyàáâäèéêëìíîïòóôöùúûüýÿŵŷẁẃẅỳ
da Latn abcdefghijklmnopqrstuvwxyzåæø
dav Latn abcdefghijklmnoprstuvwyz
de Latn abcdefghijklmnopqrstuvwxyzßäöü
dje Latn abcdefghijklmnopqrstuwxyzãõŋšžɲẽ
doi Deva अआइईउऊऋऌएऐओऔकखगघङचछजझञटठडढणतथदधनपफबभमयरलळवशषसहऽॐॠॡ
dsb Latn abcdefghijklmnopqrstuvwxyzóćčěłńŕśšźž
dua Latn abcdefgijklmnoprstuwyáéíóúŋūɓɔɗɛ
dyo Latn abcdefghijklmnopqrstuvwxyáéíñóúŋ
dz Tibt ཀཁགངཅཆཇཉཏཐདནཔཕབམཙཚཛཝཞཟའཡརལཤསཧཨ
ebu Latn abcdefghijklmnopqrstuvwxyzĩũ
ee Latn abdefghiklmnoprstuvwxyzàáãèéìíòóõùúĩŋũƒɔɖɛɣʋẽ
en Latn abcdefghijklmnopqrstuvwxyz
eo Latn abcdefghijklmnoprstuvzĉĝĥĵŝŭ
es Latn abcdefghijklmnopqrstuvwxyzáéíñóúü
et Latn abcdefghijklmnopqrstuvwxyzäõöüšž
eu Latn abcdefghijklmnopqrstuvwxyzçñ
ewo Latn abdefghiklmnoprstuvwyzàáâèéêìíîòóôùúûěńŋǎǐǒǔǹɔəɛ
fa Arab ءآأؤئابةتثجحخدذرزسشصضطظعغفقلمنهوپچژکگی
ff Latn abcdefghijklmnoprstuwyñŋƴɓɗ
ff-Latn Latn abcdefghijklmnoprstuwyñŋƴɓɗ
fi Latn abcdefghijklmnopqrstuvwxyzäåöšž
fil Latn abcdefghijklmnopqrstuvwxyzñ
fo Latn abdefghijklmnoprstuvyáæíðóøúý
fr Latn abcdefghijklmnopqrstuvwxyzàâæçèéêëîïôùûüÿœ
fur Latn abcdefghijklmnopqrstuvwxyzàâçèêìîòôùû
fy Latn abcdefghijklmnoprstuvwyzàáâäèéêëíïóôöúûüý
ga Latn abcdefghilmnoprstuáéíóú
gd Latn abcdefghilmnoprstuàèìòù
gl Latn abcdefghijklmnopqrstuvwxyzáéíïñóúü
gsw Latn abcdefghijklmnopqrstuvwxyzäöü
guz Latn abcdefghijklmnoprstuvwyz
gv Latn abcdefghijklmnopqrstuvwxyzç
ha Latn abcdefghijklmnorstuwyzƙƴɓɗʼ
haw Latn aehiklmnopuwāēīōūʻ
he Hebr אבגדהוזחטיךכלםמןנסעףפץצקרשת
hi Deva अआइईउऊऋऌऍएऐऑओऔकखगघङचछजझञटठडढणतथदधनपफबभमयरलळवशषसहऽॐ
hr Latn abcdefghijklmnoprstuvzćčđšž
hsb Latn abcdefghijklmnopqrstuvwxyzóćčěłńřšž
hu Latn abcdefghijklmnoprstuvzáéíóöúüőű
ia Latn abcdefghijklmnopqrstuvwxyz
id Latn abcdefghijklmnopqrstuvwxyz
ig Latn abdefghijklmnoprstuvwyzṅẹịọụ
is Latn abdefghijklmnoprstuvxyáæéíðóöúýþ
it Latn abcdefghijklmnopqrstuvwxyzàèéìòóù
jgo Latn abcdfghijklmnpstuvwyzáâíîúûńŋǎǐǔǹɔɛʉḿẅꞌ
jmc Latn abcdefghijklmnoprstuvwyz
jv Latn abcdeghijklmnoprstuwyâåèéêìòù
kab Latn abcdefghijklmnpqrstuwxyzčǧɛɣḍḥṛṣṭẓ
kam Latn abcdefghijklmnopqrstuvwyzĩũ
kde Latn abcdefghijklmnopqrstuvwxyz
kea Latn abdefghijklmnoprstuvxyzñ
khq Latn abcdefghijklmnopqrstuwxyzãõŋšžɲẽ
ki Latn abcdeghijkmnortuwyĩũ
kk Cyrl абвгдежзийклмнопрстуфхцчшщъыьэюяёіғқңүұһәө
kkj Latn abcdefghijklmnoprstuvwyàáâèéêìíîòóôùúûŋǌɓɔɗɛ
kl Latn abcdefghijklmnopqrstuvwxyzåæø
kln Latn abcdeghijklmnoprstuwy
kok Deva अआइईउऊऋऌऍएऐऑओऔकखगघङचछजझञटठडढणतथदधनपफबभमयरलळवशषसहऽॐ
ks Arab ؠءآأؤابتثجحخدذرزسشصضطظعغفقلمنوٲٹپچڈڑژکگںھہۄۆیۍے
ks-Arab Arab ؠءآأؤابتثجحخدذرزسشصضطظعغفقلمنوٲٹپچڈڑژکگںھہۄۆیۍے
ksb Latn abcdefghijklmnopstuvwyz
ksf Latn abcdefghijklmnoprstuvwyzáéíóúŋǝɔɛ
ksh Latn abcdefghijklmnopqrstuvwxyzßäåæëöüėœů
ku Latn abcdefghijklmnopqrstuvwxyzçêîûş
kw Latn abcdefghijklmnopqrstuvwxyz
ky Cyrl абгдежзийклмнопрстухчшъыэюяёңүө
lag Latn abcdefghijklmnopqrstuvwxyzáéíóúɨʉ
lb Latn abcdefghijklmnopqrstuvwxyzäéë
lg Latn abcdefgijklmnoprstuvwyzŋ
lkt Latn abeghiklmnopstuwyzáéíóúčŋšžǧȟʼ
ln Latn abcdefghiklmnoprstuvwyzáâéêíîóôúěǎǐǒɔɛ
lrc Arab آأؤئابتثجحخدذرزسشصضطظعغؽفقلمنوپچژڤکگھۉۊیە
lt Latn abcdefghijklmnoprstuvyząčėęįšūųž
lu Latn abcdefhijklmnopqstuvwyzàáèéìíòóùúɔɛ
luo Latn abcdefghijklmnoprstuvwy
luy Latn abcdefghijklmnopqrstuvwxyz
lv Latn abcdefghijklmnoprstuvzāčēģīķļņšūž
mai Deva कखगघचछजझञटठडढणतथदधनपफबभमयरलवशषसह
mas Latn abcdeghijklmnoprstuwyàáâèéêìíîòóôùúûāēīŋōūɔɛɨʉ
mer Latn abcdefghijklmnopqrstuvwxyzĩũ
mfe Latn abcdefghijklmnoprstuvwxyz
mg Latn abdefghijklmnoprstvyzàâèéêëìîïñô
mgh Latn abcdefghijklmnoprstuvwyz
mgo Latn abdefgijkmnoprstuwyzàèìòùŋɔəʼ
mi Latn aehikmnoprtuwāēīōū
mk Cyrl абвгдежзиклмнопрстуфхцчшѓѕјљњќџ
mn Cyrl абвгдежзийклмнопрстуфхцчшщъыьэюяёүө
mni Beng অআইঈউঊঋএঐওঔকখগঘঙচছজঝঞটঠডঢণতথদধনপফবভমযরলশষসহৱ
mni-Beng Beng অআইঈউঊঋএঐওঔকখগঘঙচছজঝঞটঠডঢণতথদধনপফবভমযরলশষসহৱ
mr Deva अआइईउऊऋऌऍएऐऑओऔकखगघङचछजझञटठडढणतथदधनपफबभमयरऱलळवशषसहऽॐ
ms Latn abcdefghijklmnopqrstuvwxyz
mt Latn abdefghijklmnopqrstuvwxzàèìòùċġħż
mua Latn abcdefghijklmnoprstuvwyzãëõĩŋǝɓɗṽ
mzn Arab ءآأؤئابةتثجحخدذرزسشصضطظعغفقلمنهوپچژکگی
naq Latn abcdefghikmnopqrstuwxyzâîôûǀǁǂǃ
nb Latn abcdefghijklmnopqrstuvwxyzàåæéòóôø
nd Latn abcdefghijklmnopqstuvwxyz
ne Deva अआइईउऊऋऌऍएऐऑओऔकखगघङचछजझञटठडढणतथदधनपफबभमयरलळवशषसहऽॐ
nl Latn abcdefghijklmnopqrstuvwxyzáäéëíïóöúü
nmg Latn abcdefghijklmnoprstuvwyáâäéêíîïóôöúûāēěīńŋōŕūǎǐǒǔǝɓɔɛ
nn Latn abcdefghijklmnopqrstuvwxyzàåæéòóôø
nnh Latn abcdefghijklmnopstuvwyzàáâèéêìíòóôùúûÿěńŋǎǒǔɔɛʉʼḿẅ
no Latn abcdefghijklmnopqrstuvwxyzàåæéòóôø
nus Latn abcdefghijklmnopqrstuvwxyzäëïöŋɔɛɣ
nyn Latn abcdefghijklmnopqrstuvwxyz
om Latn abcdefghijklmnopqrstuvwxyz
os Cyrl абвгдежзийклмнопрстуфхцчшщъыьэюяёӕ
pa Guru ਅਆਇਈਉਊਏਐਓਔਕਖਗਘਙਚਛਜਝਞਟਠਡਢਣਤਥਦਧਨਪਫਬਭਮਯਰਲਵਸਹੜੲੳੴ
pa-Arab Arab ءآؤئابتثجحخدذرزسشصضطظعغفقلمنهوٹپچڈڑژکگںھہیے
pa-Guru Guru ਅਆਇਈਉਊਏਐਓਔਕਖਗਘਙਚਛਜਝਞਟਠਡਢਣਤਥਦਧਨਪਫਬਭਮਯਰਲਵਸਹੜੲੳੴ
pcm Latn abdefghijklmnoprstuvwyzáéíóúẹọ
pl Latn abcdefghijklmnoprstuwyzóąćęłńśźż
ps Arab ءآأؤئابةتثجحخدذرزسشصضطظعغفقلمنهويټپځڅچډړږژښکګگڼیۍې
pt Latn abcdefghijklmnopqrstuvwxyzàáâãçéêíòóôõú
qu Latn ahiklmnpqstuwyñ
rm Latn abcdefghijklmnopqrstuvwxyzàèéìòù
rn Latn abcdefghijklmnopqrstuvwxyz
ro Latn abcdefghijklmnoprstuvwxyzâîășț
rof Latn abcdefghijklmnoprstuvwyz
ru Cyrl абвгдежзийклмнопрстуфхцчшщъыьэюяё
rw Latn abcdefghijklmnopqrstuvwxyz
rwk Latn abcdefghijklmnoprstuvwyz
sa Deva अआइईउऊऋऌएऐओऔकखगघङचछजझञटठडढणतथदधनपफबभमयरलळवशषसहऽॐॠॡ
sah Cyrl абгдийклмнопрстухчыэҕҥүһө
saq Latn abcdeghijklmnoprstuvwy
sat Olck ᱚᱛᱜᱝᱞᱟᱠᱡᱢᱣᱤᱥᱦᱧᱨᱩᱪᱫᱬᱭᱮᱯᱰᱱᱲᱳᱴᱵᱶᱷᱸᱹᱺᱻᱼᱽ
sat-Olck Olck ᱚᱛᱜᱝᱞᱟᱠᱡᱢᱣᱤᱥᱦᱧᱨᱩᱪᱫᱬᱭᱮᱯᱰᱱᱲᱳᱴᱵᱶᱷᱸᱹᱺᱻᱼᱽ
sbp Latn abcdefghijklmnopstuvwy
sc Latn abcdefghijlmnoprstuvzàèìòù
sd Arab ءآابتثجحخدذرزسشصضطظعغفقلمنهويٺٻٽپٿڀڃڄچڇڊڌڍڏڙڦکڪگڱڳڻھ
sd-Arab Arab ءآابتثجحخدذرزسشصضطظعغفقلمنهويٺٻٽپٿڀڃڄچڇڊڌڍڏڙڦکڪگڱڳڻھ
sd-Deva Deva अआइईउऊएऐओऔकखगघङचछजझञटठडढणतथदधनपफबभमयरलवशषसहॻॼॾॿ
se Latn abcdefghijklmnoprstuvzáčđŋšŧž
seh Latn abcdefghijklmnopqrstuvwxyzàáâãçéêíòóôõú
ses Latn abcdefghijklmnopqrstuwxyzãõŋšžɲẽ
sg Latn abdefghijklmnoprstuvwyzâäêëîïôöùûü
shi Tfng ⴰⴱⴳⴷⴹⴻⴼⴽⵀⵃⵄⵅⵇⵉⵊⵍⵎⵏⵓⵔⵕⵖⵙⵚⵛⵜⵟⵡⵢⵣⵥ
shi-Latn Latn abcdefghijklmnqrstuwxyzɛɣḍḥṛṣṭ
shi-Tfng Tfng ⴰⴱⴳⴷⴹⴻⴼⴽⵀⵃⵄⵅⵇⵉⵊⵍⵎⵏⵓⵔⵕⵖⵙⵚⵛⵜⵟⵡⵢⵣⵥ
sk Latn abcdefghijklmnopqrstuvwxyzáäéíóôúýčďĺľňŕšťž
sl Latn abcdefghijklmnoprstuvzčšž
smn Latn abcdefghijklmnoprstuvyzáâäčđŋšž
sn Latn abcdefghijklmnoprstuvwyz
so Latn bcdfghjklmnqrstwxy
sq Latn abcdefghijklmnopqrstuvxyzçë
sr Cyrl абвгдежзиклмнопрстуфхцчшђјљњћџ
sr-Cyrl Cyrl абвгдежзиклмнопрстуфхцчшђјљњћџ
sr-Latn Latn abcdefghijklmnoprstuvzćčđšž
su Latn abcdefghijklmnopqrstuvwxyzé
su-Latn Latn abcdefghijklmnopqrstuvwxyzé
sv Latn abcdefghijklmnopqrstuvwxyzàäåéö
sw Latn abdefghijklmnoprstuvwyz
teo Latn abcdeghijklmnoprstuvwxy
tg Cyrl абвгдежзийклмнопрстуфхчшъэюяёғқҳҷӣӯ
ti Ethi ሀሁሂሃሄህሆለሉሊላሌልሎሏሐሑሒሓሔሕሖሗመሙሚማሜምሞሟሠሡሢሣሤሥሦሧረሩሪራሬርሮሯሰሱሲሳሴስሶሷሸሹሺሻሼሽሾሿቀቁቂቃቄቅቆቈቊቋቌቍቐቑቒቓቔቕቖቘቚቛቜቝበቡቢባቤብቦቧቨቩቪቫቬቭቮቯተቱቲታቴትቶቷቸቹቺቻቼችቾቿኀኁኂኃኄኅኆኈኊኋኌኍነኑኒናኔንኖኗኘኙኚኛኜኝኞኟአኡኢኣኤእኦኧከኩኪካኬክኮኰኲኳኴኵኸኹኺኻኼኽኾዀዂዃዄዅወዉዊዋዌውዎዐዑዒዓዔዕዖዘዙዚዛዜዝዞዟዠዡዢዣዤዥዦዧየዩዪያዬይዮደዱዲዳዴድዶዷጀጁጂጃጄጅጆጇገጉጊጋጌግጎጐጒጓጔጕጠጡጢጣጤጥጦጧጨጩጪጫጬጭጮጯጰጱጲጳጴጵጶጷጸጹጺጻጼጽጾጿፀፁፂፃፄፅፆፇፈፉፊፋፌፍፎፏፐፑፒፓፔፕፖፗ
tk Latn abdefghijklmnoprstuwyzäçöüýňşž
to Latn aefhiklmnopstuváéíóúāēīōūʻ
tr Latn abcdefghijklmnoprstuvyzçöüğış
tt Cyrl абвгдежзийклмнопрстуфхцчшщъыьэюяёҗңүһәө
twq Latn abcdefghijklmnopqrstuwxyzãõŋšžɲẽ
tzm Latn abcdefghijklmnqrstuwxyzɛɣḍḥṛṣṭ
ug Arab ابتجخدرزسشغفقكلمنوىيپچژڭگھۆۇۈۋېە
uk Cyrl ʼабвгдежзийклмнопрстуфхцчшщьюяєіїґ
ur Arab ءابتثجحخدذرزسشصضطظعغفقلمنوٹپچڈڑژکگھہیے
uz Latn abdefghijklmnopqrstuvxyzʼ
uz-Arab Arab ءآأؤئابةتثجحخدذرزسشصضطظعغفقلمنهوپچژکگۇۉی
uz-Cyrl Cyrl абвгдежзийклмнопрстуфхчшъэюяёўғқҳ
uz-Latn Latn abdefghijklmnopqrstuvxyzʼ
vai Vaii ꔀꔁꔂꔃꔄꔅꔆꔇꔈꔉꔊꔋꔌꔍꔎꔏꔐꔑꔒꔓꔔꔕꔖꔗꔘꔙꔚꔛꔜꔝꔞꔟꔠꔡꔢꔣꔤꔥꔦꔧꔨꔩꔪꔫꔬꔭꔮꔯꔰꔱꔲꔳꔴꔵꔶꔷꔸꔹꔺꔻꔼꔽꔾꔿꕀꕁꕂꕃꕄꕅꕆꕇꕈꕉꕊꕋꕌꕍꕎꕏꕐꕑꕒꕓꕔꕕꕖꕗꕘꕙꕚꕛꕜꕝꕞꕟꕠꕡꕢꕣꕤꕥꕦꕧꕨꕩꕪꕫꕬꕭꕮꕯꕰꕱꕲꕳꕴꕵꕶꕷꕸꕹꕺꕻꕼꕽꕾꕿꖀꖁꖂꖃꖄꖅꖆꖇꖈꖉꖊꖋꖌꖍꖎꖏꖐꖑꖒꖓꖔꖕꖖꖗꖘꖙꖚꖛꖜꖝꖞꖟꖠꖡꖢꖣꖤꖥꖦꖧꖨꖩꖪꖫꖬꖭꖮꖯꖰꖱꖲꖳꖴꖵꖶꖷꖸꖹꖺꖻꖼꖽꖾꖿꗀꗁꗂꗃꗄꗅꗆꗇꗈꗉꗊꗋꗌꗍꗎꗏꗐꗑꗒꗓꗔꗕꗖꗗꗘꗙꗚꗛꗜꗝꗞꗟꗠꗡꗢꗣꗤꗥꗦꗧꗨꗩꗪꗫꗬꗭꗮꗯꗰꗱꗲꗳꗴꗵꗶꗷꗸꗹꗺꗻꗼꗽꗾꗿꘀꘁꘂꘃꘄꘅꘆꘇꘈꘉꘊꘋꘌꘐꘑꘒꘪꘫ
vai-Latn Latn abcdefghijklmnopqrstuvwxyzáãéíóõúĩŋũɓɔɗɛẽ
vai-Vaii Vaii ꔀꔁꔂꔃꔄꔅꔆꔇꔈꔉꔊꔋꔌꔍꔎꔏꔐꔑꔒꔓꔔꔕꔖꔗꔘꔙꔚꔛꔜꔝꔞꔟꔠꔡꔢꔣꔤꔥꔦꔧꔨꔩꔪꔫꔬꔭꔮꔯꔰꔱꔲꔳꔴꔵꔶꔷꔸꔹꔺꔻꔼꔽꔾꔿꕀꕁꕂꕃꕄꕅꕆꕇꕈꕉꕊꕋꕌꕍꕎꕏꕐꕑꕒꕓꕔꕕꕖꕗꕘꕙꕚꕛꕜꕝꕞꕟꕠꕡꕢꕣꕤꕥꕦꕧꕨꕩꕪꕫꕬꕭꕮꕯꕰꕱꕲꕳꕴꕵꕶꕷꕸꕹꕺꕻꕼꕽꕾꕿꖀꖁꖂꖃꖄꖅꖆꖇꖈꖉꖊꖋꖌꖍꖎꖏꖐꖑꖒꖓꖔꖕꖖꖗꖘꖙꖚꖛꖜꖝꖞꖟꖠꖡꖢꖣꖤꖥꖦꖧꖨꖩꖪꖫꖬꖭꖮꖯꖰꖱꖲꖳꖴꖵꖶꖷꖸꖹꖺꖻꖼꖽꖾꖿꗀꗁꗂꗃꗄꗅꗆꗇꗈꗉꗊꗋꗌꗍꗎꗏꗐꗑꗒꗓꗔꗕꗖꗗꗘꗙꗚꗛꗜꗝꗞꗟꗠꗡꗢꗣꗤꗥꗦꗧꗨꗩꗪꗫꗬꗭꗮꗯꗰꗱꗲꗳꗴꗵꗶꗷꗸꗹꗺꗻꗼꗽꗾꗿꘀꘁꘂꘃꘄꘅꘆꘇꘈꘉꘊꘋꘌꘐꘑꘒꘪꘫ
vi Latn abcdeghiklmnopqrstuvxyàáâãèéêìíòóôõùúýăđĩũơưạảấầẩẫậắằẳẵặẹẻẽếềểễệỉịọỏốồổỗộớờởỡợụủứừửữựỳỵỷỹ
vun Latn abcdefghijklmnoprstuvwyz
wae Latn abcdefghijklmnopqrstuvwxyzáãäéíóõöúüčšũ
wo Latn abcdefgijklmnopqrstuwxyàéëñóŋ
xh Latn abcdefghijklmnopqrstuvwxyz
xog Latn abcdefghijklmnopqrstuvwxyz
yav Latn abcdefhiklmnopstuvwyàáâèéìíîòóôùúûāīŋōūǎǒǔɔɛ
yi Hebr אבגדהוזחטיךכלםמןנסעףץצקרשת
yo Latn abdefghijklmnoprstuwyàáèéìíòóùúńǹḿṣẹọ
zgh Tfng ⴰⴱⴳⴷⴹⴻⴼⴽⵀⵃⵄⵅⵇⵉⵊⵍⵎⵏⵓⵔⵕⵖⵙⵚⵛⵜⵟⵡⵢⵣⵥ
zu Latn abcdefghijklmnopqrstuvwxyz
"""
}
