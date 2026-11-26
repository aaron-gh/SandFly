## Panjang respons {#verbosity}

Layar **Panjang respons** di setelan SandFly menentukan apa yang diucapkan SandFly tentang item, teks dan simbol, tabel, dan perubahan di layar, serta apa yang dilewati untuk aplikasi yang memiliki ucapannya sendiri. Layar ini memiliki grup **Item**, **Teks dan simbol**, **Tabel**, **Pengumuman**, dan **Aplikasi dengan ucapannya sendiri**.

Cara teks diucapkan, seperti huruf fonetik dan kamus ucapan, berada di layar **Ucapan**. Lihat [](#speech). Gema keyboard berada di layar **Mengetik**. Lihat [](#typing).

Perintah **Panjang respons** membuka layar ini. Anda juga dapat menambahkan item **Panjang respons** ke menu SandFly. Lihat [](#commands).

### Item {#verbosity-items}

Grup **Item** menentukan apa yang diucapkan SandFly tentang setiap item:

- **Ucapkan petunjuk penggunaan**: setelah item dideskripsikan, mengucapkan cara menggunakannya, seperti "Ketuk dua kali untuk mengaktifkan". Aktif secara default.
- **Ucapkan jenis elemen**: mengucapkan jenis item, seperti "Tombol" atau "Kotak centang". Aktif secara default.
- **Ucapkan jenis item tanpa jeda**: menggabungkan nama dan jenis item dengan spasi alih-alih jeda. Nonaktif secara default.
- **Ucapkan Tindakan tersedia**: mengucapkan "Tindakan tersedia" setelah item yang memiliki tindakan dari aplikasinya, seperti Balas pada pesan chat. Berfungsi baik saat **Ucapkan petunjuk penggunaan** aktif maupun nonaktif. Saat petunjuk penggunaan aktif, petunjuk tersebut hanya menyebutkan gerakan yang menampilkan tindakan, seperti "Gunakan ketuk dengan 2 jari untuk melihat tindakan". Nonaktif secara default. Untuk item yang dicakupnya dan suara **Tindakan tersedia**, lihat [](#sound-actions-available).
- **Ucapkan informasi daftar**: mengucapkan posisi daftar dan detail daftar. Aktif secara default.
- **Ucapkan informasi kisi dan tabel**: mengucapkan baris dan kolom pada kisi, tabel, dan penampil halaman yang ditata dalam baris dan kolom, seperti folder di layar utama. Aktif secara default. Setelan **Tabel** hanya berlaku saat opsi ini aktif. Lihat [](#verbosity-tables).
- **Urutan detail item**: urutan pengucapan nama, jenis, dan status suatu item. Lihat [](#verbosity-item-order).
- **Ucapkan ID elemen**: mengucapkan ID tombol yang tidak memiliki label. Nonaktif secara default.

#### Mengubah urutan detail item {#verbosity-item-order}

**Panjang respons** > **Urutan detail item** mencantumkan **Status**, **Nama**, dan **Jenis** sesuai urutan pengucapannya oleh SandFly. Defaultnya adalah **Status, nama, jenis**.

Untuk memindahkan satu detail, lakukan salah satu langkah berikut:

- Sentuh lama detail tersebut, lalu seret ke atas atau ke bawah. Saat SandFly aktif, ketuk dua kali lalu tahan detail tersebut, kemudian seret.
- Gunakan tindakan **Pindahkan ke atas** dan **Pindahkan ke bawah** pada detail tersebut.

SandFly mengumumkan posisi detail saat Anda mengambil, memindahkan, dan melepasnya.

Urutan ini juga berlaku untuk baris yang kotak centang atau tombol alihnya tidak memiliki teks sendiri. SandFly membaca baris seperti itu sebagai satu kontrol, dengan teks baris sebagai namanya. Urutan ini juga berlaku untuk tombol dan tab yang labelnya merupakan item terpisah di dalamnya.

### Teks dan simbol {#verbosity-text}

Grup **Teks dan simbol** menentukan cara teks, tanda baca, huruf kapital, dan emoji diucapkan:

- **Ucapkan tanda baca dan simbol**: **Semua** mengucapkan setiap tanda baca dan simbol. **Sebagian besar** mengucapkan sebagian besar simbol, seperti & dan @, tetapi menyerahkan tanda baca umum, seperti koma dan apostrof, kepada mesin ucapan. **Beberapa** (default) menyerahkan tanda baca dan simbol kepada mesin ucapan. **Tidak ada** menghilangkan tanda baca dan simbol dari ucapan, kecuali karakter yang dibaca tersendiri, seperti saat berpindah per karakter. Apostrof dan tanda hubung di dalam kata, pemisah di dalam angka, serta tanda baca yang mengakhiri kata tetap diserahkan kepada mesin ucapan. **Pelajari lebih lanjut** di daftar ini membuka halaman web tentang tingkat tanda baca.
- **Ucapkan simbol yang mengubah makna**, ditampilkan saat **Ucapkan tanda baca dan simbol** disetel ke **Tidak ada**: menyerahkan hal berikut kepada mesin ucapan: tanda plus atau minus tepat sebelum angka, tanda derajat, persen, per mil, dan mata uang, serta + − × ÷ = < > di antara angka. Nonaktif secara default.
- **Huruf kapital**: apa yang dilakukan SandFly untuk huruf kapital: **Jangan lakukan apa pun**, **Ucapkan "huruf kapital"** (default), **Ubah tinggi nada**, atau **Putar suara**. Dengan pilihan apa pun selain **Ucapkan "huruf kapital"**, SandFly mengirim huruf kapital yang berdiri sendiri ke mesin ucapan dalam huruf kecil.
- **Emoji**, **Hitung emoji berulang**, dan **Ucapkan "emoji" setelah nama emoji**: lihat [](#verbosity-emoji).
- **Hitung simbol berulang**: mengucapkan berapa kali simbol berulang jika muncul 4 kali atau lebih secara berurutan. Aktif secara default.
- **Ucapkan pemformatan teks** dan **Opsi pemformatan teks**: lihat [](#verbosity-text-formatting).

Kontrol pembacaan **Ucapkan tanda baca dan simbol** dan pintasan keyboard **Ganti tingkat tanda baca** mengubah tingkat tanda baca. Untuk mengganti simbol dengan kata-kata Anda sendiri, gunakan **Ucapan** > **Kamus simbol**. Lihat [](#speech-symbol-dictionary).

#### Emoji {#verbosity-emoji}

Sebagian mesin ucapan membaca emoji dengan kurang baik, memakai nama sendiri, atau tidak membacanya sama sekali. **Panjang respons** > **Emoji** memilih cara SandFly mengucapkannya:

- **Dibacakan oleh mesin ucapan**: default. Mesin ucapan menerima emoji apa adanya.
- **Dibacakan oleh SandFly**: mengganti setiap emoji dengan namanya dari Unicode CLDR dalam bahasa pengucapan teks tersebut. Warna kulit, keluarga, bendera, atau tombol angka dianggap satu emoji dengan satu nama. Emoji yang tidak memiliki nama di SandFly diserahkan kepada mesin ucapan, dan emoji yang tidak memiliki nama dalam bahasa tersebut diucapkan dalam bahasa Inggris.
- **Tidak ada**: menghilangkan emoji, kecuali pada teks yang hanya berisi emoji, seperti tombol reaksi, yang tetap disebutkan namanya.

Dua setelan lain bergantung pada **Emoji**:

- **Hitung emoji berulang**: dengan **Dibacakan oleh SandFly**, mengucapkan emoji yang berulang berurutan satu kali disertai jumlahnya, seperti "3 wajah menyeringai". Pilih **Nonaktif** (default), atau dari **2 atau lebih** hingga **6 atau lebih**.
- **Setelah nama emoji**: yang mengikuti setiap emoji. Tidak tersedia jika **Emoji** disetel ke **Tidak ada**.
  - **Tidak ada**: default.
  - **Ucapkan "emoji"**: mengucapkan "emoji" setelah setiap emoji, seperti "wajah menyeringai emoji".
  - **Putar suara**: memutar suara **Emoji** setelah setiap emoji, atau satu kali setelah emoji yang berdampingan. Emoji kemudian diucapkan sebagai ucapan terpisah dari teks di sekitarnya. Tema suara dapat mengganti suara ini, dan **Suara dan getaran satu per satu** dapat menonaktifkannya. Lihat [](#sound-events).

Dengan **Dibacakan oleh SandFly**, berpindah per karakter menyebutkan seluruh emoji satu kali, dan berpindah per kata berhenti di emoji. Dengan **Tidak ada**, berpindah per kata melewati emoji. Menyalin atau mengeja frasa yang terakhir diucapkan tetap menghasilkan emojinya, bukan namanya.

Kontrol pembacaan **Emoji** dan perintah **Emoji** mengubah setelan ini. Suara dan kecepatan emoji diatur di **Ucapan**. Lihat [](#speech-emoji).

#### Pemformatan teks {#verbosity-text-formatting}

**Panjang respons** > **Ucapkan pemformatan teks** mengumumkan perubahan pemformatan teks, seperti tebal dan miring, saat SandFly membaca teks. Setelan ini nonaktif secara default.

**Panjang respons** > **Opsi pemformatan teks** tersedia saat **Ucapkan pemformatan teks** aktif. Layar ini berisi:

- **Gaya pengumuman**: **Ucapan dan suara** (default), **Hanya ucapan**, atau **Hanya suara**. Dengan ucapan, SandFly mengucapkan pengumuman seperti "mulai tebal, tajuk utama, akhir tebal".
- **Pengumuman pemformatan teks**: jenis pemformatan yang diumumkan. **Tebal**, **Miring**, **Garis bawah**, **Coret**, **Nama warna teks**, **Ukuran teks**, dan **Jenis font** aktif secara default. **Nilai hex warna teks** nonaktif secara default.

Kontrol pembacaan dan perintah **Ucapkan pemformatan teks** mengaktifkan atau menonaktifkan setelan ini. Perintah **Jelaskan pemformatan teks**, di grup perintah **Membaca**, mendeskripsikan pemformatan teks yang sedang difokuskan.

### Tabel {#verbosity-tables}

Grup **Tabel** hanya berlaku saat **Ucapkan informasi kisi dan tabel** aktif:

- **Ucapkan nomor baris dan kolom**: mengucapkan nomor baris dan kolom untuk sel tabel. Aktif secara default. Nonaktifkan untuk hanya mendengar header yang memiliki nama.
- **Baca header kolom**: **Setelah data sel** (default) mengucapkan isi sel terlebih dahulu, lalu header atau nomor baris dan kolomnya. **Sebelum data sel** mengucapkan header atau nomornya terlebih dahulu, dengan "Judul kolom" atau "Judul baris" untuk sel header. Setelan ini berlaku untuk kisi maupun tabel, sehingga item di kisi foto mengucapkan baris dan kolomnya terlebih dahulu. **Jangan baca** menghilangkan header kolom, dan hanya mengucapkan nomor kolom jika **Ucapkan nomor baris dan kolom** aktif.

### Pengumuman {#verbosity-announcements}

Grup **Pengumuman** menentukan apa yang diucapkan SandFly saat terjadi perubahan:

- **Nama jendela**: kapan SandFly mengucapkan nama jendela. **Selalu** mengucapkannya saat jendela terbuka dan saat fokus berpindah ke jendela itu dari jendela lain, sebelum item pertama. **Selalu, dihentikan oleh item pertama** (default) melakukan hal yang sama, tetapi item pertama yang difokuskan di jendela baru akan menghentikan pengucapan nama jendela. **Hanya ketika sebuah jendela terbuka** tidak mengucapkannya saat fokus berpindah ke jendela. **Tidak pernah** tidak mengucapkan nama jendela sama sekali.
- **Selalu ucapkan jumlah item daftar**: mengucapkan item mana saja yang sedang tampil setelah setiap pengguliran. Nonaktif secara default. Jika nonaktif, SandFly tidak mengucapkan posisi daftar saat daftar digulir.
- **Umumkan saat baterai penuh**: mengucapkan "Baterai terisi penuh" dan memutar suara **Baterai penuh** satu kali saat baterai penuh sementara pengisi daya tersambung. Nonaktif secara default.
- **Tingkat baterai**: mengucapkan tingkat baterai, baik saat mengisi daya maupun tidak, setiap kali mencapai kelipatan 5% atau 10%, atau setiap 15 menit, 30 menit, atau satu jam. Tidak ada yang diucapkan saat layar mati: tingkat yang tercapai atau interval yang berakhir selama layar mati diucapkan saat layar menyala. Nonaktif secara default.
- **Suara tingkat baterai**: suara untuk **Tingkat baterai**: **Suara notifikasi**, **Suara SandFly** (default), atau profil suara. Lihat [](#notifications-voice).
- **Umumkan durasi panggilan**: saat panggilan telepon berakhir, mengucapkan berapa lama panggilan berlangsung sejak dijawab atau dihubungi. Panggilan yang tidak dijawab tidak diumumkan. Nonaktif secara default.
- **Umumkan saat keyboard muncul atau disembunyikan**: aktif secara default.
- **Layar aktif dan mati**: apa yang diucapkan SandFly saat layar mati atau menyala dan saat kunci ponsel dibuka. Lihat [](#verbosity-screen-on-off).
- **Format waktu**: **Gunakan format waktu perangkat** (default), **12 jam**, atau **24 jam**, untuk waktu yang diucapkan SandFly.
- **Sertakan detik dalam waktu**: nonaktif secara default.
- **Sertakan detik saat layar menyala**: saat **Sertakan detik dalam waktu** aktif, menyertakan detik pada waktu yang diucapkan ketika layar menyala. Aktif secara default.
- **Pembacaan status**: item yang dibacakan perintah **Ucapkan status**. Lihat [](#verbosity-status-readout).

#### Layar aktif dan mati {#verbosity-screen-on-off}

**Panjang respons** > **Layar aktif dan mati** memiliki tiga grup: **Saat layar mati**, **Saat layar menyala**, dan **Saat kunci ponsel dibuka**. Di ponsel lipat, menyalakan layar luar dihitung sebagai layar menyala, dan membuka ponsel segera setelahnya tidak mengucapkannya lagi. Setiap grup memiliki item yang sama, kecuali beberapa item pertama:

- **Ucapkan "Layar mati"**: hanya di **Saat layar mati**. Aktif secara default.
- **Mode pendering**: hanya di **Saat layar mati**. Mengucapkan jika pendering disetel ke getar atau senyap. Aktif secara default.
- **Ucapkan "Perangkat dibuka kuncinya"**: hanya di **Saat kunci ponsel dibuka**. Aktif secara default.
- **Waktu**: aktif secara default saat layar menyala, nonaktif untuk dua grup lainnya.
- **Jumlah notifikasi**: aktif secara default saat layar menyala, nonaktif untuk dua grup lainnya.
- **Baterai**: **Nonaktif** (default), **Selalu**, atau **Hanya saat mengisi daya**.
- **Baterai lemah**: mengucapkan tingkat baterai saat berada di bawah batas tertentu: **Nonaktif** (default), atau **Di bawah 10%**, **Di bawah 15%**, **Di bawah 20%**, **Di bawah 30%**, atau **Di bawah 50%**.
- **Layar tersembunyi**: hanya saat layar disembunyikan. Nonaktif secara default. Lihat [](#display).
- **Wi-Fi**: nama jaringan dan kekuatan sinyal. Nonaktif secara default.
- **Jaringan seluler**: operator, jenis jaringan, dan kekuatan sinyal. Nonaktif secara default.
- **Dering dan Jangan Ganggu**: hanya saat pendering tidak disetel ke dering, atau Jangan Ganggu aktif. Nonaktif secara default.
- **Mode pesawat**: hanya saat mode pesawat aktif. Nonaktif secara default.

**Ucapkan saat media diputar**, di atas grup-grup tersebut, mengucapkan pengumuman layar mati dan buka kunci saat media sedang diputar. Nonaktif secara default. Pengumuman layar menyala selalu diucapkan saat media diputar.

#### Pembacaan status {#verbosity-status-readout}

**Panjang respons** > **Pembacaan status** mencantumkan item yang diucapkan perintah **Ucapkan status**, secara berurutan: **Waktu**, **Baterai**, **Wi-Fi**, **Jaringan seluler**, **Dering dan Jangan Ganggu**, **Mode pesawat**, dan **Layar tersembunyi**. Semuanya aktif secara default. Item dalam keadaan normal, seperti pendering saat berdering, dilewati.

- Untuk menonaktifkan atau mengaktifkan item, pilih item tersebut.
- Untuk mengubah urutannya, gunakan tindakan **Pindahkan ke atas** dan **Pindahkan ke bawah** pada item tersebut.

**Pembacaan status** tidak ditampilkan di TV.

### Aplikasi dengan ucapannya sendiri {#verbosity-own-speech}

Beberapa aplikasi dan keyboard berbicara sendiri, seperti asisten yang mengucapkan jawabannya sambil memindahkan fokus aksesibilitas ke setiap potongan teks baru, atau keyboard yang mengucapkan tombolnya sendiri. Grup **Aplikasi dengan ucapannya sendiri** menghilangkan ucapan SandFly untuk aplikasi dan keyboard tersebut agar teks yang sama tidak diucapkan dua kali. Hanya ucapan yang dihilangkan: fokus tetap berpindah, dan suara serta getaran tetap diputar. Layar SandFly selalu diucapkan.

- **Ucapkan hanya fokus yang dipindahkan pengguna**: **Nonaktif** (default), **Di semua aplikasi**, atau **Di aplikasi yang dipilih**. Saat berlaku, SandFly hanya mengucapkan fokus yang Anda pindahkan melalui sentuhan, gerakan, tombol, penampil braille, atau perintah. SandFly tidak mengucapkan fokus yang dipindahkan aplikasi, pengumuman, perubahan wilayah langsung (live region), nama jendela, atau pengumuman pergantian jendela. Fokus yang dipindahkan SandFly setelah jendela berganti, setelah pengguliran, atau untuk mengikuti fokus input dianggap dipindahkan oleh aplikasi.
- **Aplikasi yang dipilih**: daftar aplikasi untuk **Di aplikasi yang dipilih**. Hanya tersedia saat **Ucapkan hanya fokus yang dipindahkan pengguna** disetel ke **Di aplikasi yang dipilih**. Pilih **Tambahkan aplikasi** untuk menambahkan aplikasi, dan pilih aplikasi yang ada untuk menghapusnya.
- **Ucapkan hanya fokus yang dipindahkan pengguna saat asisten atau panggilan memutar audio**: melakukan hal yang sama di semua aplikasi saat aplikasi lain memutar audio sebagai asisten atau panggilan, dan saat jendela asisten, seperti Gemini di atas aplikasi atau layar kunci, ditampilkan. Pengumuman layar menyala dan buka kunci juga tidak diucapkan saat itu, dan ucapan berhenti saat jendela asisten terbuka. Fokus yang Anda pindahkan sendiri tetap diucapkan. Nonaktif secara default.
- **Keyboard dengan ucapannya sendiri**: saat keyboard di daftar ini ditampilkan, SandFly tidak mengucapkan tombolnya, teks yang diketik atau dihapus, maupun perpindahan kursor di kolom teks. Gerakan dan perintah tetap berfungsi, dan perpindahan melalui teks dengan kontrol pembacaan tetap diucapkan. Pilih **Tambahkan keyboard** untuk menambahkan keyboard.

SandFly tidak pernah mengucapkan pengumuman, perubahan wilayah langsung, perubahan jendela, toast, atau notifikasi dari com.oplus.viewtalk, layanan aksesibilitas di ponsel realme dan OPPO yang mengirimkan deskripsi gambar seperti "Gambar mungkin berisi…".

Kontrol pembacaan **Ucapkan hanya fokus yang dipindahkan pengguna** mengubah setelan pertama. Perintahnya menonaktifkan setelan tersebut, atau mengaktifkannya kembali ke **Di aplikasi yang dipilih** jika ada aplikasi yang dipilih, dan ke **Di semua aplikasi** jika tidak ada. Kontrol pembacaan ini tidak ada di kontrol pembacaan secara default. Lihat [](#commands).
