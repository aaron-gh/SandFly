## Menjelajahi dan membaca layar {#reading}

SandFly membacakan item yang sedang menerima fokus SandFly. Anda memindahkan fokus dengan menyentuh layar, menggeser, atau menggunakan keyboard atau penampil braille, lalu SandFly membacakan setiap item yang dituju. Bab ini menjelaskan cara berpindah di layar, membaca teks per bagian, membaca terus-menerus, dan menggulir, serta mencantumkan setelan pada layar **Navigasi**.

Gerakan dalam bab ini berasal dari tata letak gerakan default, yaitu **Gerakan SandFly**. Jika **Gerakan** > **Tata letak gerakan** diatur ke **Gerakan VoiceOver**, beberapa gerakan akan berbeda. Untuk melihat semua gerakan default, lihat [](#appendix-gestures). Untuk mengubah gerakan, lihat [](#gestures).

### Fokus SandFly {#reading-focus}

Fokus SandFly adalah item yang dibacakan dan ditindaklanjuti oleh SandFly. Sebuah kotak persegi panjang di layar menunjukkan posisinya. Untuk mengubah tampilannya, lihat [](#display).

Fokus SandFly terpisah dari fokus masukan, yaitu item yang menerima ketikan dari keyboard. Kolom teks, dan sebagian konten web, dapat memiliki fokus masukan sementara fokus SandFly berada di tempat lain.

Sebuah item dapat berupa tombol, baris teks, baris daftar, gambar, atau apa pun yang ditampilkan aplikasi. Aplikasi menentukan bagaimana layarnya dibagi menjadi item. Saat sebuah item berisi teks lain di dalamnya, SandFly membacakan semuanya sebagai satu item, kecuali jika **Navigasi** > **Fokuskan teks di dalam item secara terpisah** aktif. Lihat [](#reading-navigation-settings).

### Jelajahi dengan sentuhan {#reading-explore}

Sentuh layar dan gerakkan jari Anda. SandFly memfokuskan dan membacakan item di bawah jari Anda. Saat jari Anda bergerak di atas bagian layar yang kosong, SandFly memutar suara.

SandFly menunggu sebentar setelah Anda menyentuh layar, untuk membedakan gerakan dari sentuhan, sebelum memfokuskan item di bawah jari Anda. Untuk mengubah waktu tunggu ini, buka **Sentuhan** > **Penundaan fokus**. Nilai default-nya adalah 0,15 detik. Setelan sentuhan dijelaskan di [](#touch).

- Menyentuh area di antara ikon-ikon layar utama tidak membacakan halaman, seperti "Layar utama 1 dari 2". Menggeser tetap dapat mencapainya.
- Jendela picture-in-picture, seperti video yang diputar di atas layar utama, diperlakukan sebagai satu item yang dinamai sesuai aplikasinya diikuti "picture in picture". Kontrol di dalam jendela tersebut menjadi tindakan pada item itu. Sentuhan tetap dapat mencapai setiap kontrol.

### Berpindah antar-item {#reading-move}

- Untuk pindah ke item berikutnya, geser ke kanan.
- Untuk pindah ke item sebelumnya, geser ke kiri.

Item dibacakan sesuai urutan yang diberikan aplikasi, biasanya dari kiri atas ke kanan bawah. Saat item berikutnya berada di luar layar dalam sebuah daftar, SandFly menggulir daftar tersebut. Lihat [](#reading-scroll).

Geseran tidak berpindah ke bilah status atau bilah navigasi. Anda mencapainya dengan sentuhan atau dengan kontrol pembacaan **Jendela**. Geseran dari aplikasi juga tidak berpindah ke keyboard di layar. Lihat [](#typing).

Di ujung layar, geseran melewati item terakhir berhenti di sana dan memutar suara. Geseran berikutnya pindah ke item pertama. Untuk mengubah perilaku ini, lihat [](#reading-wrap).

Perintah berikut memindahkan fokus ke ujung layar. Keduanya tidak memiliki gerakan secara default:

- **Fokus pada item pertama di layar**
- **Fokus pada item terakhir di layar**

Untuk menetapkan gerakan atau pintasan keyboard ke suatu perintah, lihat [](#commands).

#### Kembali ke awal {#reading-wrap}

Dengan **Navigasi** > **Lanjutkan dari ujung lain** aktif (setelan default), geseran melewati item terakhir akan pindah ke item pertama, dan geseran melewati item pertama akan pindah ke item terakhir.

- **Navigasi** > **Berhenti di item terakhir sebelum kembali ke awal**: Geseran pertama melewati item terakhir atau pertama berhenti di sana dengan suara ujung, dan baru geseran berikutnya yang kembali ke awal atau akhir. Aktif secara default. Setelan ini tidak berpengaruh saat **Lanjutkan dari ujung lain** nonaktif.
- Saat **Lanjutkan dari ujung lain** nonaktif, setiap geseran atau penekanan tombol melewati item pertama atau terakhir membacakan ulang item tersebut disertai suara ujung. Berpindah menurut judul, tautan, atau kontrol di aplikasi juga berhenti di ujung. Halaman web mungkin masih kembali ke awal dengan sendirinya.

**Lanjutkan dari ujung lain** juga merupakan kontrol pembacaan yang dapat Anda tambahkan di layar **Kontrol pembacaan** untuk mengaktifkan atau menonaktifkan kembali ke awal dengan geseran ke atas atau ke bawah. Lihat [](#reading-controls).

**Navigasi** > **Kembali ke awal di kontrol pembacaan** mengatur apakah kontrol pembacaan yang berisi daftar nilai berputar dari nilai terakhir kembali ke nilai pertama. Contohnya **Mesin ucapan**, **Perangkat output audio**, **Tindakan**, **Alamat dan nomor**, dan **Mode suara**. Saat nonaktif, menggeser melewati nilai pertama atau terakhir membacakan ulang nilai tersebut dengan suara ujung. Selama belum Anda ubah, setelan ini mengikuti **Lanjutkan dari ujung lain**. Setelah Anda mengaturnya, item dan nilai dapat berperilaku berbeda.

### Mengaktifkan item {#reading-activate}

- Untuk mengaktifkan item yang difokuskan, ketuk dua kali di mana saja di layar. Ini adalah perintah **Aktifkan**.
- Untuk menyentuh lama item yang difokuskan, sama seperti sentuh lama tanpa SandFly, ketuk dua kali lalu tahan. Ini adalah perintah **Sentuh lama**.

Saat kontrol pembacaan **Tindakan** dipilih, ketukan dua kali menjalankan tindakan yang dipilih di dalamnya. Lihat [](#reading-controls).

Saat **Mengetik** > **Ketuk dua kali memindahkan kursor ke awal atau akhir** aktif, ketuk dua kali pada kolom teks yang sedang Anda ketik memindahkan kursor ke awal atau akhir teks. Lihat [](#typing-double-tap-cursor).

**Aktifkan** melakukan hal yang sama seperti ketuk dua kali dalam semua kasus ini, apa pun gerakan, perintah suara, perintah braille, atau skrip yang menjalankannya.

**Aktifkan dengan ketukan**, perintah tanpa gerakan secara default, mengetuk bagian tengah item yang difokuskan di layar, berguna untuk item yang hanya merespons jari di layar. Perintah ini memerlukan Android 11 atau yang lebih baru. Di versi yang lebih lama, item diaktifkan seperti biasa.

Untuk mengaktifkan item dengan mengangkat jari atau dengan satu ketukan, lihat [](#touch).

### Kontrol pembacaan {#reading-controls}

Kontrol pembacaan menentukan apa yang terjadi saat Anda menggeser ke atas atau ke bawah. Sebagian besar kontrol pembacaan berpindah berdasarkan jenis item atau bagian teks, seperti **Judul** atau **Kata**. Lainnya mengubah suatu setelan, seperti **Kecepatan bicara**.

- Untuk memilih kontrol pembacaan berikutnya, geser ke bawah lalu ke atas, geser ke kanan atau ke bawah dengan tiga jari, atau putar searah jarum jam dengan dua jari.
- Untuk memilih kontrol pembacaan sebelumnya, geser ke atas lalu ke bawah, geser ke kiri atau ke atas dengan tiga jari, atau putar berlawanan arah jarum jam dengan dua jari.
- Untuk memilih kontrol pembacaan pertama dari daftar yang digunakan, tetapkan gerakan atau pintasan keyboard ke **Kontrol pembacaan pertama**. Secara default, perintah ini tidak memiliki gerakan atau pintasan.
- Untuk berpindah menurut kontrol pembacaan yang dipilih, geser ke bawah untuk maju dan geser ke atas untuk mundur. Keduanya adalah perintah **Turunkan atau majukan kontrol pembacaan** dan **Naikkan atau mundurkan kontrol pembacaan**.
- Pada kontrol pembacaan yang mengubah nilai, seperti **Sesuaikan penggeser**, **Sesuaikan nilai**, dan **Kecepatan bicara**, arah geseran menentukan perubahannya, apa pun perintah yang dijalankan geseran tersebut: geseran ke atas atau ke kanan menaikkan nilai, sedangkan geseran ke bawah atau ke kiri menurunkannya. Ini berlaku untuk geseran lurus dengan satu hingga empat jari. Dengan bahasa yang ditulis dari kanan ke kiri, geseran ke kiri menaikkan nilai dan geseran ke kanan menurunkannya. Dengan gerakan lain, pintasan keyboard, dan perintah penampil braille, **Naikkan atau mundurkan kontrol pembacaan** menaikkan nilai dan **Turunkan atau majukan kontrol pembacaan** menurunkannya.

SandFly membacakan nama setiap kontrol pembacaan saat Anda memilihnya. Menggeser ke kanan dan ke kiri tetap berpindah antar-item apa pun kontrol pembacaan yang dipilih, kecuali saat menggunakan **Baris dan kolom** di dalam tabel.

Kontrol pembacaan yang hanya cocok untuk item tertentu, seperti **Tindakan** pada item yang memiliki tindakan atau **Baris dan kolom** di dalam tabel, hanya muncul di daftar pada tempat yang sesuai. **Kontrol pembacaan** > **Ubah kontrol pembacaan secara otomatis**, yang aktif secara default, otomatis memilih kontrol pembacaan tersebut saat fokus berpindah ke item yang menggunakannya, seperti **Tindakan**, **Sesuaikan penggeser**, **Sesuaikan nilai**, atau **Periksa tata bahasa**. Saat fokus meninggalkan item itu, SandFly kembali memilih kontrol pembacaan sebelumnya. Nonaktifkan agar kontrol pembacaan yang Anda pilih tetap dipertahankan. Di dalam tabel, **Baris dan kolom** dipilih secara otomatis seperti ini hanya jika **Navigasi** > **Beralih ke navigasi tabel dalam tabel** aktif.

Layar **Kontrol pembacaan** mengatur kontrol pembacaan mana saja yang ada dalam daftar dan urutannya. Layar ini juga memuat daftar kontrol pembacaan, kontrol pembacaan kustom, dan huruf untuk **Tulis huruf kontrol pembacaan**, yaitu perintah untuk memilih kontrol pembacaan melalui huruf yang Anda tulis di layar, misalnya H untuk **Judul**. Semua ini dijelaskan di [](#commands).

Secara default, daftar memuat kontrol pembacaan navigasi berikut: **Karakter**, **Kata**, **Baris**, **Paragraf**, **Judul**, **Tautan**, **Kontrol**, **Penelusuran layar**, dan, di dalam tabel, **Baris dan kolom**. Anda dapat menambahkan yang lain di layar **Kontrol pembacaan**.

#### Membaca teks per bagian {#reading-text}

Kontrol pembacaan berikut berpindah menyusuri teks pada item yang difokuskan, lalu berlanjut ke item berikutnya atau sebelumnya:

- **Karakter**: per karakter.
- **Kata**: per kata.
- **Baris**: per baris.
- **Kalimat**: per kalimat. Kalimat berakhir pada tanda baca akhir kalimat, termasuk tanda baca bahasa Tionghoa 。！？, dan pada akhir baris. Di halaman web, kalimat yang berlanjut ke item lain, seperti tautan di dalamnya, dibacakan per bagian. Tidak ada dalam daftar secara default, dan tidak tersedia dalam mode pemilihan. Perintah **Kalimat berikutnya** dan **Kalimat sebelumnya**, di kategori **Navigasi menurut teks**, berpindah per kalimat tanpa kontrol pembacaan.
- **Paragraf**: per paragraf.

Di kolom teks yang sedang diedit, kontrol pembacaan ini menggerakkan kursor, dan geseran tetap berada di dalam kolom. Untuk mengedit teks, lihat [](#typing).

SandFly juga dapat berpindah menyusuri teks yang digambar sendiri oleh aplikasi dan tidak dapat dijelajahi pembaca layar lain, seperti pesan di Telegram.

#### Berpindah berdasarkan jenis item {#reading-kinds}

Kontrol pembacaan berikut berpindah ke item berikutnya atau sebelumnya dari satu jenis tertentu. Geser ke bawah untuk berikutnya dan geser ke atas untuk sebelumnya.

- **Navigasi default**: per item, sama seperti menggeser ke kanan dan ke kiri. Tidak ada dalam daftar secara default.
- **Judul**, **Tautan**, dan **Kontrol**: judul, tautan, dan item yang dapat digunakan, seperti tombol dan kolom teks. Ketiganya berfungsi di aplikasi maupun halaman web.
- **Jendela**: jendela lain di layar, seperti kontrol volume atau bilah navigasi. Tidak ada dalam daftar secara default.
- **Penampung**: keluar dari penampung saat ini ke penampung berikutnya atau sebelumnya. Penampung mencakup daftar, kisi, tampilan gulir, bilah alat, bilah tab, bilah navigasi, bilah penelusuran, dan panel. Bilah di dalam bilah, seperti bilah penelusuran di dalam bilah alat, dihitung sebagai satu. Mundur memindahkan fokus ke awal penampung sebelumnya, kecuali di halaman web. Tidak ada dalam daftar secara default.
- **Navigasi vertikal**: item di bawah atau di atas. Lihat [](#reading-vertical).
- **Penelusuran layar**: hasil dari penelusuran layar terakhir. Lihat [](#reading-search).
- **Gulir**: menggulir daftar maju saat Anda menggeser ke bawah dan mundur saat Anda menggeser ke atas. Jika daftar tidak merespons pengguliran, SandFly mengirim geseran ke daftar tersebut. Tidak ada dalam daftar secara default.

Kontrol pembacaan di bawah **Navigasi elemen** dan **Tingkat judul** pada layar **Kontrol pembacaan**, seperti **Penunjuk**, **Tombol**, **Kolom teks**, **Daftar**, dan **Tabel**, berpindah berdasarkan elemen halaman web, dan juga berfungsi di aplikasi. Lihat [](#web).

**Kontrol pembacaan** > **Tautan dan Kontrol hanya di konten web**, yang nonaktif secara default, menyembunyikan **Tautan** dan **Kontrol** dari daftar saat berada di luar konten web. Gerakan dan pintasan keyboard untuk tautan atau kontrol berikutnya maupun sebelumnya tetap berfungsi di mana saja.

#### Berpindah menurut teks atau elemen tanpa kontrol pembacaan {#reading-navigation-commands}

Anda dapat menetapkan gerakan atau pintasan keyboard untuk berpindah ke karakter, kata, baris, paragraf, judul, tautan, kontrol, penunjuk, tombol, kotak centang, tombol radio, kolom teks, kotak kombo, item yang dapat difokuskan, grafik, daftar, item daftar, tabel, tautan yang sudah dikunjungi, tautan yang belum dikunjungi, atau judul pada tingkat tertentu berikutnya atau sebelumnya. Perpindahan ini tidak mengubah kontrol pembacaan yang dipilih. Di daftar perintah untuk gerakan, semuanya berada di bawah **Navigasi menurut teks** dan **Navigasi menurut elemen**. Lihat [](#gestures).

### Membaca terus-menerus {#reading-continuous}

SandFly dapat membacakan setiap item satu per satu dan menggulir daftar sambil membaca:

- **Baca dari atas**: membaca mulai dari item pertama di layar.
- **Baca dari item yang difokuskan**: membaca mulai dari item yang difokuskan. Perintah ini ada di menu SandFly secara default.
- **Baca dari item yang difokuskan menurut kontrol pembacaan**: membaca mulai dari item yang difokuskan, lalu hanya membacakan item yang sesuai dengan kontrol pembacaan yang dipilih, misalnya setiap judul atau setiap tautan. Kontrol pembacaan yang tidak berpindah antar-item sejenis akan membacakan semua item.

Perintah-perintah ini berada di kategori perintah **Membaca** dan tidak memiliki gerakan secara default. Anda dapat menjalankannya dari menu SandFly yang dibuka dengan ketukan tiga jari, atau menetapkannya ke gerakan maupun pintasan keyboard. Lihat [](#commands).

Saat SandFly membaca terus-menerus:

- Geser ke kanan atau ke kiri untuk melompat ke item berikutnya atau kembali ke item sebelumnya. Pembacaan berlanjut dari sana.
- Tekan tombol volume untuk mengubah volume ucapan.
- Untuk menghentikan pembacaan, sentuh layar atau gunakan gerakan lain.

Saat pertama kali Anda memulai pembacaan terus-menerus, SandFly menampilkan **Cara kerja pembacaan berkelanjutan**. Untuk menampilkannya setiap kali, pilih **Selalu tampilkan pesan ini**.

SandFly menjaga layar tetap menyala saat membaca.

### Menggulir {#reading-scroll}

Saat item berikutnya berada di luar layar, geseran ke kanan dan ke kiri menggulir daftar secara otomatis. Anda juga dapat menggulir sendiri:

- Untuk menggulir maju, geser ke kanan lalu ke kiri. Ini adalah **Gulir maju**.
- Untuk menggulir mundur, geser ke kiri lalu ke kanan. Ini adalah **Gulir mundur**. Di bagian atas daftar, **Gulir mundur** menarik daftar ke bawah. Di sebagian besar aplikasi, tarikan ini menyegarkan daftar. Tarikan memerlukan Android 13 atau yang lebih baru.
- Untuk menggulir aplikasi secara langsung, seret dua jari di layar, seperti saat Anda menyeret satu jari tanpa SandFly.

Di tata letak gerakan default, geseran dua jari diteruskan ke aplikasi, lalu aplikasi menggulir isinya. Jika **Gerakan** > **SandFly menangani geser dua jari** aktif, seperti pada tata letak gerakan VoiceOver, geseran dua jari menjalankan perintah sebagai gantinya: secara default **Gulir ke atas**, **Gulir ke bawah**, **Gulir ke kiri**, dan **Gulir ke kanan**, yang menggerakkan konten searah geseran. Jika tidak ada item di layar yang dapat digulir ke samping, **Gulir ke kiri** dan **Gulir ke kanan** mengirim geseran satu jari ke pager di sekitar item yang difokuskan, seperti tab folder di Solid Explorer. Keempat perintah ini memerlukan Android 11 atau yang lebih baru, dan tidak tersedia di jam tangan.

Jika tidak ada yang dapat digulir ke arah tersebut di layar, seperti di akhir daftar, **Gulir maju**, **Gulir mundur**, dan keempat perintah ini memutar suara **Tindakan selesai atau sudah di akhir**. Di bagian atas daftar, **Gulir mundur** dan **Gulir ke bawah** menarik daftar ke bawah sebagai gantinya, tanpa suara. Dari penampil braille, **Gulir maju** dan **Gulir mundur** memutar suara **Perintah braille gagal** sebagai gantinya.

Perintah berikut, di kategori **Gulir**, tidak memiliki gerakan secara default:

- **Gulir ke paling atas** dan **Gulir ke paling bawah** menggulir daftar yang difokuskan ke awal atau akhirnya, halaman demi halaman, lalu memfokuskan item pertama atau terakhirnya. Sentuhan, gerakan, atau penekanan tombol akan menghentikannya. Tidak tersedia di jam tangan.
- **Segarkan** menggulir daftar ke bagian atas lalu menariknya ke bawah. Memerlukan Android 13 atau yang lebih baru.

Saat Anda menggulir dengan dua jari, SandFly memutar suara gulir dan getaran ringan untuk setiap item yang terlewati. Nada suara gulir semakin tinggi dari bagian atas daftar ke bagian bawah. Saat SandFly sendiri menggulir untuk mencapai item berikutnya, tidak ada suara. Untuk setelan suara dan getaran, lihat [](#sound).

#### Menggulir saat menggeser {#reading-scroll-swipes}

Setelan berikut pada layar **Navigasi** mengubah cara geseran menggulir daftar:

- **Gulir lebih awal ke item berikutnya**: Saat item yang difokuskan adalah item terakhir dari daftar di layar, SandFly menggulir daftar maju selagi membacakan item tersebut, sampai item berikutnya tampak di layar. Dengan begitu, geseran berikutnya tidak perlu menunggu pengguliran. Fokus tetap berada di item tersebut, dan pengguliran ini tidak bersuara. Setelah Anda berpindah mundur, SandFly menggulir mundur. SandFly tidak menggulir lebih awal di kolom teks, konten web, atau selama pembacaan terus-menerus, dan berhenti saat Anda menyentuh layar, menekan tombol, atau jendela berubah. Aktif secara default.
- **Ucapkan item berikutnya saat menggulir**: Saat geseran harus menggulir daftar untuk mencapai item berikutnya, SandFly memfokuskan dan membacakan item tersebut segera setelah muncul di layar, selagi daftar masih bergerak. Nonaktifkan untuk menunggu hingga daftar melambat. Aktif secara default.
- **Ucapkan item sebelum digulir hingga terlihat**: Saat geseran pindah ke item yang hanya terlihat sebagian di layar, SandFly langsung membacakannya, termasuk bagian yang masih berada di luar tepi daftar, alih-alih menunggu daftar digulir. Nonaktif secara default, karena beberapa aplikasi meletakkan teks tersembunyi di luar item alih-alih benar-benar menyembunyikannya, dan teks itu akan ikut dibacakan.

### Daftar dan tabel {#reading-lists-tables}

Saat fokus masuk atau keluar dari daftar, SandFly membacakan informasi daftar, seperti posisi item. **Panjang respons** > **Ucapkan informasi daftar**, **Ucapkan informasi kisi dan tabel**, dan **Selalu ucapkan jumlah item daftar** mengatur apa yang dibacakan. Lihat [](#verbosity).

Kontrol pembacaan **Item daftar** serta gerakan dan pintasan keyboard item daftar berpindah ke item berikutnya atau sebelumnya dalam daftar yang difokuskan. Semuanya melewati tombol dan bagian lain di dalam setiap item, seperti tombol Bagikan pada sebuah postingan, dan menggulir daftar saat item berikutnya berada di luar layar. Setelah item terakhir, fokus berlanjut ke daftar berikutnya. Di luar daftar, semuanya berpindah ke item daftar berikutnya di layar.

#### Menavigasi tabel {#reading-tables}

Di dalam tabel, kontrol pembacaan **Baris dan kolom** berpindah per sel:

- Geser ke bawah atau ke atas untuk pindah ke baris berikutnya atau sebelumnya.
- Geser ke kanan atau ke kiri untuk pindah ke kolom berikutnya atau sebelumnya.

Saat **Baris dan kolom** dipilih, geser ke kanan dan geser ke kiri selalu berpindah per kolom, apa pun perintah yang ditetapkan oleh profil gerakan, gerakan di aplikasi, atau skrip pada kedua gerakan tersebut. Saat fokus keluar dari tabel, SandFly kembali memilih kontrol pembacaan sebelumnya.

Kontrol pembacaan **Baris** dan **Kolom** hanya berpindah per baris atau hanya per kolom dengan geseran ke bawah dan ke atas. Keduanya tidak ada dalam daftar secara default. **Baris berikutnya**, **Baris sebelumnya**, **Kolom berikutnya**, dan **Kolom sebelumnya** juga merupakan perintah di kategori **Tabel** yang dapat Anda tetapkan ke gerakan.

**Navigasi** > **Beralih ke navigasi tabel dalam tabel**, yang nonaktif secara default, memilih **Baris dan kolom** saat fokus memasuki tabel.

Kontrol pembacaan tabel hanya muncul dalam daftar saat berada di dalam tabel. Di Google Sheets, SandFly memperlakukan kisi sel sebagai tabel, sehingga kontrol pembacaan dan perintah tabel berpindah antar-sel, dan tindakan pada sebuah sel mencakup **Edit sel** dan **Sisipkan rumus**.

Apakah SandFly membacakan nomor baris dan kolom serta header kolom bersama setiap sel diatur oleh **Panjang respons** > **Ucapkan nomor baris dan kolom** dan **Baca header kolom**. Lihat [](#verbosity). Perintah keyboard yang membacakan header baris dan kolom dijelaskan di [](#keyboards).

### Jendela, penampung, dan item di bawah {#reading-windows}

- Untuk pindah ke jendela berikutnya, geser ke bawah dengan empat jari. Untuk pindah ke jendela sebelumnya, geser ke atas dengan empat jari. Ini adalah **Jendela berikutnya** dan **Jendela sebelumnya**.
- Untuk pindah ke penampung berikutnya, geser ke kanan dengan empat jari. Untuk pindah ke penampung sebelumnya, geser ke kiri dengan empat jari. Ini adalah **Penampung berikutnya** dan **Penampung sebelumnya**, yang berhenti di penampung yang sama seperti kontrol pembacaan **Penampung**.

**Navigasi** > **Pindah dari bilah status dan bilah navigasi ke jendela lain**, yang nonaktif secara default: Saat fokus berada di bilah status atau bilah navigasi, misalnya setelah Anda menyentuhnya, menggeser melewati item pertama atau terakhir di bilah tersebut akan berpindah ke aplikasi. Saat nonaktif, geseran berputar di dalam bilah saja.

#### Navigasi vertikal {#reading-vertical}

Kontrol pembacaan **Navigasi vertikal** berpindah ke item di bawah saat Anda menggeser ke bawah dan ke item di atas saat Anda menggeser ke atas. Kontrol ini melewati item lain pada baris yang sama, seperti tombol di samping judul video atau nama file, dan tetap berada pada kolom item yang difokuskan: dari sebuah tombol pada papan tombol angka, fokus pindah ke tombol tepat di bawah atau di atasnya. Di dalam daftar, fokus tetap berada di dalam daftar. Jika tidak ada baris di bawah atau di atas di layar, SandFly menggulir daftar satu halaman. Di ujung daftar, atau di luar daftar, fokus pindah ke item di bawah atau di atas di layar, dan memutar suara ujung jika tidak ada lagi.

Perintah **Pindah ke item di bawah** dan **Pindah ke item di atas** melakukan perpindahan yang sama dari gerakan atau pintasan keyboard, tanpa kontrol pembacaan. **Pindah ke item di sebelah kiri** dan **Pindah ke item di sebelah kanan** pindah ke item terdekat di layar ke arah tersebut. Tidak satu pun memiliki gerakan secara default.

### Fokus saat layar berubah {#reading-screen-changes}

Saat jendela baru terbuka, atau saat Anda kembali ke layar sebelumnya, SandFly memindahkan fokus ke dalamnya: ke item tempat Anda berada sebelumnya, ke kolom teks dengan kursor, atau ke item pertama. Ini juga berlaku untuk menu dan dialog SandFly.

Setelah Anda mengaktifkan item dalam daftar, seperti folder di pengelola file atau halaman di Setelan, lalu kembali, fokus kembali ke item tersebut jika daftar menampilkannya lagi dalam waktu dua detik. Ini juga berfungsi di aplikasi yang mengubah isi daftar tanpa membuka jendela baru.

Untuk membiarkan fokus tetap di tempatnya, nonaktifkan **Navigasi** > **Pindahkan fokus ke jendela baru**. SandFly kemudian hanya membacakan nama jendela, dan geseran berikutnya pindah ke item pertama. Fokus hanya mengikuti fokus masukan saat Anda memindahkan fokus masukan dengan tombol, seperti tombol panah atau Tab. Di TV, fokus selalu berpindah ke jendela baru.

Saat item yang difokuskan menghilang, seperti iklan yang menciut atau baris daftar yang dihapus, SandFly memfokuskan item yang kini berada di posisinya. Pembacaan berlanjut dari sana.

### Menelusuri layar {#reading-search}

Penelusuran layar mencari item di layar berdasarkan teksnya.

1. Geser ke kiri lalu ke bawah. Ini adalah perintah **Penelusuran layar**, di kategori **SandFly**.
2. Ketik kata di kolom penelusuran. SandFly mencantumkan item yang cocok dan membacakan jumlah kecocokan.
3. Ketuk dua kali hasil untuk memindahkan fokus ke item tersebut.

Jendela penelusuran juga memiliki **Layar berikutnya** dan **Layar sebelumnya**, yang menggulir layar di belakangnya untuk menelusuri lebih banyak isi daftar, **Hapus masukan**, dan **Tutup penelusuran**.

Setelah penelusuran, kontrol pembacaan **Penelusuran layar** berpindah di antara hasil: geser ke bawah untuk hasil berikutnya dan geser ke atas untuk hasil sebelumnya. Penelusuran layar tidak tersedia di jam tangan.

### Alamat dan nomor {#reading-addresses}

Kontrol pembacaan **Alamat dan nomor** mencantumkan alamat web, alamat email, nomor telepon, dan kode 4 hingga 8 digit dalam teks item yang difokuskan, termasuk saat aplikasi tidak menjadikannya tautan, seperti pada pesan obrolan atau notifikasi berisi kode sekali pakai.

1. Di layar **Kontrol pembacaan**, tambahkan **Alamat dan nomor**. Setelan ini nonaktif secara default.
2. Fokuskan item, lalu pilih **Alamat dan nomor**.
3. Geser ke atas atau ke bawah hingga SandFly membacakan alamat atau nomor yang Anda inginkan.
4. Ketuk dua kali. Alamat web akan terbuka, alamat email membuka email baru, nomor telepon terbuka di aplikasi telepon (**Telepon** diikuti nomornya), dan kode akan disalin (**Salin** diikuti kodenya).

**Alamat dan nomor** juga merupakan item di menu SandFly dan perintah di kategori **Aktivasi**. **Kontrol pembacaan** > **Alamat dan nomor yang disertakan** mengaktifkan atau menonaktifkan setiap jenis: **Alamat web**, **Alamat email**, **Nomor telepon**, dan **Kode**.

Nomor telepon ditemukan oleh pengklasifikasi teks Android, yang juga mendeteksi tanggal, waktu, alamat jalan, dan nomor penerbangan. Digit dalam semua itu, serta digit yang menempel pada kata, nama pengguna, waktu, atau jumlah, tidak dicantumkan sebagai kode. Angka lain sepanjang 4 hingga 8 digit, seperti tahun dalam judul berita, dicantumkan sebagai kode. Teks dipindai saat Anda menggeser kontrol pembacaan atau membuka menu SandFly, yang dapat memakan waktu hingga satu detik. Di Android 8, dan saat pengklasifikasi teks tidak merespons dalam satu detik, hanya alamat web dan email yang dicantumkan.

### Menyalin teks dari layar {#reading-copy-text}

**Salin teks dari layar**, perintah di kategori **Pengeditan teks** tanpa gerakan secara default, membuka jendela yang menampilkan teks di layar sebagai daftar kotak centang.

Jendela ini mencantumkan teks di layar. Jika layar memiliki daftar atau halaman yang dapat digulir, jendela ini memiliki **Gulir ke atas** di atas daftar dan **Gulir ke bawah** di bawahnya. Keduanya menggulir daftar di layar satu halaman melewati teks yang sudah dicantumkan, lalu menambahkan teks halaman itu. SandFly mengucapkan jumlah bagian yang ditambahkan, seperti "5 item ditambahkan". Bagian yang dicentang tetap dicentang. Saat daftar berada di awal atau akhirnya, SandFly mengucapkan "Tidak ada teks lagi". Saat jendela ditutup, daftar digulir kembali ke posisi semula.

1. Jalankan **Salin teks dari layar**.
2. Di bawah **Granularitas**, pilih cara teks dibagi: **Alamat dan nomor**, **Karakter**, **Kata**, atau **Paragraf**. **Kata** dipilih terlebih dahulu.
3. Centang bagian yang Anda inginkan. Untuk mencentang suatu rentang, ketuk dua kali lalu tahan bagian pertama, lalu bagian terakhir, atau gunakan tindakan **Mulai pemilihan** dan **Akhiri pemilihan**. **Pilih semua** mencentang setiap bagian.
4. Pilih **Salin**. Bagian yang dicentang disalin secara berurutan: kata dengan spasi di antaranya, karakter tanpa pemisah, paragraf dengan baris kosong di antaranya, serta alamat dan nomor satu per baris.

**Alamat dan nomor** mencantumkan setiap alamat web, alamat email, nomor telepon, dan kode di layar persis seperti tertulis, sehingga Anda dapat menyalin nomor telepon alih-alih meneleponnya. Opsi ini hanya muncul jika layar memilikinya, dan mencantumkan semua jenis, apa pun setelan **Alamat dan nomor yang disertakan**. Pencariannya dapat membuat jendela terbuka hingga satu detik lebih lama.

Jendela ini menampung paling banyak 1.000 bagian. Jika teks lebih panjang, SandFly membacakan "Daftar dipotong pada" diikuti jumlah bagiannya. Untuk cara lain menyalin teks, lihat [](#typing).

### Setelan navigasi {#reading-navigation-settings}

Layar **Navigasi** di setelan SandFly memiliki setelan berikut:

- **Lanjutkan dari ujung lain**: Menggeser melewati item terakhir pindah ke item pertama, dan melewati item pertama pindah ke item terakhir. Aktif secara default. Lihat [](#reading-wrap).
- **Berhenti di item terakhir sebelum kembali ke awal**: Geseran pertama melewati ujung berhenti di sana. Geseran berikutnya baru kembali ke awal atau akhir. Aktif secara default.
- **Kembali ke awal di kontrol pembacaan**: Nilai kontrol pembacaan berputar dari nilai terakhir kembali ke nilai pertama. Mengikuti **Lanjutkan dari ujung lain** hingga Anda mengubahnya.
- **Pindah dari bilah status dan bilah navigasi ke jendela lain**: Geseran melewati ujung bilah sistem berpindah ke aplikasi. Nonaktif secara default. Lihat [](#reading-windows).
- **Pindahkan fokus ke jendela baru**: Fokus pindah ke jendela yang terbuka. Aktif secara default. Lihat [](#reading-screen-changes).
- **Ucapkan item sebelum digulir hingga terlihat**: Item yang sebagian berada di luar layar dibacakan sebelum daftar digulir. Nonaktif secara default. Lihat [](#reading-scroll-swipes).
- **Gulir lebih awal ke item berikutnya**: Daftar digulir lebih awal selagi item terakhir di layar dibacakan. Aktif secara default.
- **Ucapkan item berikutnya saat menggulir**: Item berikutnya dibacakan segera setelah muncul di layar. Aktif secara default.
- **Fokuskan teks di dalam item secara terpisah**: Setiap potongan teks dan setiap status di dalam sebuah item mendapat fokus sendiri, sehingga Anda dapat mencapainya dengan geseran dan sentuhan. Item itu sendiri hanya mempertahankan fokus jika memiliki teks, status, atau tindakannya sendiri. Mengaktifkan potongan teks di dalamnya akan mengaktifkan item tersebut. Nonaktif secara default. Perintah **Fokuskan teks di dalam item secara terpisah**, di kategori **Navigasi**, mengaktifkan atau menonaktifkannya dan membacakan "Fokuskan teks di dalam item secara terpisah aktif" atau "Fokuskan teks di dalam item secara terpisah nonaktif".
- **Beralih ke navigasi tabel dalam tabel**: **Baris dan kolom** dipilih saat fokus memasuki tabel. Nonaktif secara default. Lihat [](#reading-tables).
- **Mode penjelajahan cerdas** dan **Ingat mode penjelajahan di setiap aplikasi**: Lihat [](#web).
- **Bacakan kutipan balasan Telegram bersama pesannya**, di bawah **Aplikasi**: Lihat [](#reading-telegram).

#### Telegram {#reading-telegram}

Di Telegram dan aplikasi turunannya, seperti Nekogram:

- Dengan **Navigasi** > **Bacakan kutipan balasan Telegram bersama pesannya** aktif (setelan default), SandFly membacakan kutipan pesan yang dibalas bersama pesan balasannya, setelah waktu, sebagai "membalas" diikuti pengirim dan teksnya. Kutipan tidak mendapat fokus tersendiri. Untuk melihat pesan yang dikutip, gunakan **Buka pesan yang dikutip** di tindakan pesan. Nonaktifkan setelan ini agar kutipan menjadi item tersendiri. Saat setelan ini nonaktif, **Buka pesan yang dikutip** tidak tersedia.
- Tombol Bagikan di samping setiap pesan dalam saluran tidak difokuskan saat Anda menggeser atau menyentuh layar. Tombol ini berada di tindakan pesan, dengan nama yang diberikan Telegram, seperti **Bagikan**.
