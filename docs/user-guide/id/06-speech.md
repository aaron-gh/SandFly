## Ucapan {#speech}

Layar **Ucapan** di setelan SandFly mengatur mesin ucapan, suara, kecepatan, tinggi nada, dan volume, profil suara, bahasa, pengucapan, serta hal yang menghentikan ucapan. Layar ini memiliki grup **Suara**, **Bahasa**, **Pengucapan**, **Menghentikan ucapan**, dan **Aplikasi lain**.

Tempat diputarnya ucapan, volume pemutarannya, pengecilan volume audio, dan **Audio latensi rendah** berada di layar **Output audio**. Lihat [](#sound). Apa yang diucapkan SandFly tentang setiap item, seperti petunjuk penggunaan, tanda baca, dan emoji, berada di layar **Panjang respons**. Lihat [](#verbosity).

### Memilih mesin ucapan {#speech-engine}

SandFly menggunakan setelan mesin ucapannya sendiri, terpisah dari mesin default Android.

1. Buka **Ucapan** > **Mesin text-to-speech**.
2. Pilih mesin yang terinstal, atau **Default sistem** untuk menggunakan mesin yang disetel di Android.

**Ucapan** > **Setelan text-to-speech sistem** membuka setelan text-to-speech Android, tempat Anda dapat menginstal suara dan menyetel mesin default sistem. Opsi ini tidak ditampilkan di TV.

Kontrol pembacaan **Mesin ucapan** beralih di antara mesin yang terinstal saat Anda menggeser ke atas atau ke bawah. Kontrol ini mengubah mesin **Default SandFly**, sehingga tidak tersedia saat profil suara sedang digunakan. Kontrol ini tidak ada di kontrol pembacaan secara default. Untuk menambahkannya, buka **Kontrol pembacaan**. Lihat [](#commands).

Beberapa mesin memutar audionya sendiri, seperti mesin yang meneruskan teks ke mesin lain. SandFly mendeteksinya dan memutar ucapannya dengan cara biasa, bukan melalui **Audio latensi rendah**.

### Mengubah kecepatan bicara, tinggi nada, dan volume {#speech-rate}

Penggeser di grup **Suara** mengatur ucapan **Default SandFly**. Penggeser ini hanya tersedia saat SandFly aktif.

- **Kecepatan bicara**: dari 10% hingga 600%. Defaultnya adalah 100%.
- **Tinggi nada ucapan**: dari 20% hingga 200%. Defaultnya adalah 100%.
- **Volume ucapan**: dari 25 hingga 100. Defaultnya adalah 100.

**Besar perubahan kecepatan bicara**, di bawah **Kecepatan bicara**, mengatur seberapa besar kecepatan berubah setiap kali Anda mengubahnya dengan penggeser **Kecepatan bicara** di layar ini dan di profil suara, kontrol pembacaan **Kecepatan bicara**, serta perintah kecepatan bicara. **10% dari kecepatan**, yang merupakan default, mengubah kecepatan yang sedang digunakan sebesar sepersepuluhnya. **1%**, **2%**, **5%**, **10%**, dan **20%** berpindah ke kelipatan berikutnya dari langkah tersebut, misalnya dari 133% ke 135% dengan **5%**.

Saat profil suara sedang digunakan, item **Profil suara** menampilkan "Digunakan:" diikuti nama profilnya, dan penggeser ini tidak mengubah suara profil tersebut. Dalam keadaan ini, mengubah kecepatan atau tinggi nada melalui perintah atau kontrol pembacaan akan mengubah profil yang sedang digunakan. Lihat [](#speech-voice-profiles).

Anda juga dapat mengubah ucapan tanpa membuka setelan:

- Kontrol pembacaan **Kecepatan bicara**, yang ada di kontrol pembacaan secara default: geser ke atas atau ke bawah untuk mengubah kecepatan.
- Perintah **Tambah kecepatan bicara**, **Kurangi kecepatan bicara**, **Naikkan tinggi nada ucapan**, dan **Turunkan tinggi nada ucapan**, untuk gerakan, pintasan keyboard, dan menu.
- Pintasan keyboard **Naikkan volume ucapan**, **Turunkan volume ucapan**, dan **Kecepatan bicara dan tinggi nada disetel ulang**. Lihat [](#appendix-keyboard-shortcuts).

Untuk memilih apakah ucapan diputar dengan volume aksesibilitas, media, dering, atau panggilan, lihat [](#sound-speech-volume). Untuk mengubah volume ucapan dengan tombol volume, lihat [](#buttons-speech-volume).

### Mengulangi frasa terakhir lebih lambat {#speech-repeat-slower}

Perintah **Ulangi frasa yang terakhir diucapkan lebih lambat**, di grup perintah **Membaca**, mengucapkan kembali frasa terakhir satu kali lebih lambat dengan suara yang sama. Kecepatan bicara tidak berubah, sehingga item berikutnya tetap diucapkan dengan kecepatan biasa.

**Ucapan** > **Ulangi lebih lambat sebesar** menentukan seberapa banyak lebih lambat: 5%, 10%, 15%, 20%, 25%, 30%, 40%, atau 50%. Defaultnya adalah 15%.

Untuk menggunakan perintah ini, tetapkan ke gerakan atau pintasan keyboard, atau tambahkan ke menu. Lihat [](#commands).

### Profil suara {#speech-voice-profiles}

Profil suara adalah sekumpulan setelan ucapan yang dapat langsung Anda aktifkan sekaligus: mesin ucapan, bahasa, suara, volume, kecepatan, tinggi nada, efek ucapan, dan **Kirim teks panjang per kalimat**. Misalnya, satu profil dapat menggunakan mesin lain untuk membaca teks panjang sementara **Default SandFly** tetap cepat.

**Default SandFly** selalu ada dalam daftar. Profil ini menggunakan setelan di layar **Ucapan**. Saat profil suara digunakan, semua ucapan menggunakan mesin dan suara profil tersebut, sehingga SandFly tidak mengganti bahasa atau dialek, kecuali untuk bahasa yang memiliki suaranya sendiri di **Suara menurut bahasa**. Lihat [](#speech-voices-by-language).

Profil suara juga digunakan untuk emoji dan untuk bahasa: lihat [](#speech-emoji) dan [](#speech-voices-by-language).

#### Menambahkan profil suara {#speech-add-voice-profile}

1. Buka **Ucapan** > **Profil suara**.
2. Pilih **Tambahkan profil suara**.
3. Masukkan nama, atau biarkan kosong untuk menggunakan nama yang disarankan, seperti "Profil suara 1". Pilih **Oke**.

SandFly membuka setelan profil baru tersebut. Profil baru dimulai dengan volume ucapan, kecepatan, tinggi nada, efek ucapan, dan **Kirim teks panjang per kalimat** milik **Default SandFly**.

#### Setelan profil suara {#speech-voice-profile-settings}

Untuk mengubah profil, buka **Ucapan** > **Profil suara** dan pilih profilnya.

- **Nama**: mengganti nama profil.
- **Mesin text-to-speech**: mesin untuk profil tersebut. Mesin yang tidak lagi terinstal akan ditampilkan dengan keterangan "tidak terinstal".
- **Bahasa**: **Default**, atau salah satu bahasa dari mesin tersebut.
- **Suara**: **Default**, atau salah satu suara dari mesin dalam bahasa yang dipilih.
- **Volume ucapan**, **Kecepatan bicara**, dan **Tinggi nada ucapan**: sama seperti di layar **Ucapan**, khusus untuk profil ini.
- **Putar sampel**: mengucapkan kalimat contoh dengan mesin, suara, volume, kecepatan, tinggi nada, dan efek ucapan yang diatur di sini, dalam bahasa profil jika SandFly memiliki kalimat tersebut dalam bahasa itu. "Sampel tidak diputar" berarti mesin tidak dapat dimulai atau mengembalikan error.
- **Efek ucapan**: efek ucapan untuk profil ini. Lihat [](#voice-effects). Tidak tersedia di jam tangan.
- **Kirim teks panjang per kalimat**: sama seperti di layar **Ucapan**, khusus untuk profil ini. Lihat [](#speech-pronunciation).
- **Biarkan mesin ucapan tetap berjalan**: menjaga mesin profil ini tetap berjalan saat mesin lain digunakan dan layar menyala, sehingga saat beralih ke profil ini tidak perlu menunggu mesin dimulai. Setiap mesin yang dibiarkan berjalan menggunakan prosesnya sendiri. Nonaktif secara default.
- **Pindahkan ke atas** dan **Pindahkan ke bawah**: hanya di jam tangan. Di ponsel, Anda memindahkan profil langsung di daftarnya.
- **Hapus profil suara**: menghapus profil setelah Anda mengonfirmasi.

#### Beralih profil suara {#speech-switch-voice-profile}

- **Ucapan** > **Profil suara** > **Profil suara yang digunakan**: pilih **Default SandFly** atau sebuah profil.
- Kontrol pembacaan **Profil suara**: geser ke atas atau ke bawah untuk berpindah antara **Default SandFly** dan profil lainnya, sesuai urutan di layar **Profil suara**. Kontrol ini tidak ada di kontrol pembacaan secara default. Untuk menambahkannya, buka **Kontrol pembacaan**.
- Perintah **Pilih profil suara**, **Profil suara berikutnya**, dan **Profil suara sebelumnya**, di grup perintah **Ucapan**.
- Setelah Anda memiliki profil, grup perintah **Profil suara** memiliki perintah **Gunakan profil suara** untuk **Default SandFly** dan untuk setiap profil, yang dapat digunakan pada gerakan, pintasan keyboard, dan menu.

Anda juga dapat menambahkan item **Profil suara** ke menu SandFly. Lihat [](#commands).

#### Mengurutkan, mengganti nama, dan menghapus profil suara {#speech-manage-voice-profiles}

Di layar **Profil suara**:

- Untuk memindahkan profil, sentuh lama profil lalu seret ke atas atau ke bawah, atau gunakan tindakan **Pindahkan ke atas** dan **Pindahkan ke bawah**. SandFly mengumumkan di atas atau di bawah profil mana posisi barunya.
- Untuk mengganti nama atau menghapus profil, gunakan tindakan **Ganti nama** dan **Hapus**, atau sentuh lama profil untuk membuka menu yang berisi tindakan tersebut.

**Biarkan mesin ucapan profil suara sebelumnya tetap berjalan**, di layar **Profil suara**, menjaga mesin dari profil yang digunakan sebelum profil saat ini tetap berjalan saat layar menyala, sehingga beralih kembali tidak perlu menunggu mesin dimulai. Setelan ini nonaktif secara default.

### Suara dan kecepatan emoji {#speech-emoji}

Dua setelan di grup **Suara** mengubah cara emoji terdengar:

- **Suara emoji**: **Sama dengan ucapan** (default), atau profil suara tertentu untuk mengucapkan emoji.
- **Kecepatan bicara emoji**: kecepatan pengucapan emoji sebagai persentase dari kecepatan bicara, dari 50% hingga 300%. Defaultnya adalah 100%.

Keduanya berlaku baik saat mesin ucapan membaca emoji secara langsung maupun saat SandFly menggantinya dengan namanya. Apakah emoji dibaca dan bagaimana caranya diatur di **Panjang respons** > **Emoji**. Lihat [](#verbosity-emoji).

### Efek ucapan {#voice-effects}

**Ucapan** > **Efek ucapan** menyetel equalizer 11 pita dan efek kompresi untuk **Default SandFly**. **Efek ucapan** di layar profil suara menyetelnya untuk profil tersebut. Profil baru dimulai dengan efek ucapan milik **Default SandFly**. Efek ucapan tidak tersedia di jam tangan.

Layar **Efek ucapan** berisi:

- **Gunakan efek ucapan**: mengaktifkan atau menonaktifkan equalizer dan efek tanpa mengubahnya. Aktif secara default.
- **Preset**: **Datar**, **Kejernihan**, **Penguat bass**, **Kurangi bass**, **Penguat treble**, **Kurangi treble**, **Hangat**, **Kurangi kekasaran**, **Kurangi desis**, **Speaker kecil**, atau **Bass dalam**, diikuti preset tersimpan Anda. Preset menyetel pita dan efek. Pita dimulai dalam posisi datar dan efek dalam keadaan nonaktif. Mengubah satu pita atau satu efek akan menyetel **Preset** ke **Kustom**.
- **Simpan sebagai preset**: menyimpan pita dan efek sebagai preset dengan nama tertentu. Preset tersimpan ditampilkan untuk **Default SandFly** dan untuk setiap profil suara. Anda dapat menyimpan hingga 50 preset.
- **Preset tersimpan**: mengganti nama atau menghapus preset tersimpan. Menghapus preset tidak mengubah efek ucapan yang disetel ke preset tersebut.
- **Kompresi bass**, **Kompresi vokal**, dan **Kompresi treble**: **Nonaktif**, **Rendah**, **Sedang**, atau **Tinggi**. Masing-masing mengompresi ucapan dalam rentangnya, di bawah 200 Hz, 200 Hz hingga 4 kHz, atau di atas 4 kHz, dan menaikkan levelnya, sehingga bagian yang pelan dalam rentang tersebut menjadi lebih keras. **Bass dalam** menggunakan **Kompresi bass** pada **Sedang**.
- **Equalizer**: penggeser untuk setiap pita: 64 Hz, 125 Hz, 250 Hz, 500 Hz, 1 kHz, 2 kHz, 3 kHz, 4 kHz, 6 kHz, 8 kHz, dan 16 kHz, masing-masing dari -12 dB hingga +12 dB.

Saat sebuah pita dinaikkan atau suatu efek aktif, pembatas mencegah suara pecah (clipping).

Efek ucapan berlaku pada ucapan yang diputar SandFly melalui **Output audio** > **Audio latensi rendah**. Jika **Audio latensi rendah** nonaktif, efek ucapan berlaku sebagai efek audio di Android 9 dan lebih baru pada perangkat yang mendukungnya. Jika tidak, item **Efek ucapan** akan menampilkan "Memerlukan Output audio > Audio latensi rendah". Mesin yang meneruskan teks ke mesin lain hanya menerapkan efek ini jika juga meneruskan sesi audionya. Lihat [](#sound).

Kontrol pembacaan: **Efek ucapan** mengaktifkan atau menonaktifkan efek ucapan dari suara yang sedang digunakan, dan setiap pita equalizer memiliki kontrol pembacaannya sendiri, seperti **Equalizer 1 kHz**. Geser ke atas atau ke bawah untuk mengubah pita sebesar 1 dB. Kontrol ini tidak ada di kontrol pembacaan secara default. Untuk menambahkannya, buka **Kontrol pembacaan**. Perintah **Gunakan efek ucapan**, di grup perintah **Ucapan**, mengaktifkan atau menonaktifkan efek ucapan.

### Bahasa {#speech-languages}

Saat aplikasi menandai teks dengan bahasa lain, SandFly mengganti suara ke bahasa tersebut. Grup **Bahasa** mengatur caranya:

- **Ganti bahasa secara otomatis**: beralih ke suara bahasa lain untuk teks dalam bahasa tersebut. Aktif secara default.
- **Ganti dialek secara otomatis**: beralih ke ragam regional lain dari bahasa suara tersebut, seperti bahasa Inggris Britania untuk teks yang ditandai sebagai bahasa Inggris Britania saat suara menggunakan bahasa Inggris AS. Aktif secara default.
- **Suara menurut bahasa**: bahasa-bahasa yang Anda baca, masing-masing dengan suara tertentu. Lihat [](#speech-voices-by-language).
- **Kenali bahasa dari kata**: membedakan bahasa yang ditulis dalam aksara yang sama berdasarkan kata-katanya, untuk teks yang tidak ditandai aplikasi. Nonaktif secara default.

Jika pergantian dinonaktifkan, SandFly membaca teks dengan suara yang sedang digunakan tanpa jeda di sekitarnya.

Dengan **Ganti bahasa secara otomatis** aktif, SandFly juga mendeteksi bahasa teks yang tidak ditandai aplikasi berdasarkan aksaranya, seperti huruf Arab atau Yunani dalam teks bahasa Inggris, serta berdasarkan huruf yang hanya digunakan oleh salah satu bahasa Anda. **Kenali bahasa dari kata** menggunakan pengenalan bahasa di perangkat dari layanan Google Play untuk potongan teks yang berisi sedikitnya 12 huruf.

Perintah **Ganti bahasa secara otomatis** dan **Kenali bahasa dari kata**, di grup perintah **Ucapan**, mengaktifkan atau menonaktifkan setelan ini.

#### Suara menurut bahasa {#speech-voices-by-language}

**Ucapan** > **Suara menurut bahasa** mencantumkan bahasa yang Anda tambahkan. Setiap bahasa diucapkan dengan **Default SandFly**, menggunakan suara dari mesin untuk bahasa tersebut, atau dengan profil suara tertentu. Gunakan profil suara jika mesin default memiliki suara untuk bahasa itu yang tidak ingin Anda pakai.

Untuk menambahkan bahasa:

1. Buka **Ucapan** > **Suara menurut bahasa**.
2. Pilih **Tambahkan bahasa**. SandFly mencantumkan bahasa dari semua mesin yang terinstal.
3. Pilih bahasanya. SandFly menambahkannya dengan **Default SandFly**.
4. Untuk menggunakan profil suara, pilih bahasa tersebut dalam daftar, lalu pilih profilnya.

Untuk menghapus bahasa, pilih bahasa tersebut lalu pilih **Hapus bahasa**.

Jika dua atau lebih bahasa yang tercantum, atau satu bahasa yang tercantum dan bahasa suara utama, menggunakan aksara yang sama, seperti bahasa Inggris dan bahasa Prancis dalam aksara Latin, layar ini menampilkan daftar untuk aksara tersebut, seperti **Aksara Latin**. Daftar ini menentukan bahasa untuk teks dalam aksara tersebut jika tidak ada hal lain yang mengenalinya.

#### Mengubah bahasa lisan {#speech-spoken-language}

Kontrol pembacaan **Bahasa lisan** dan perintah **Ubah bahasa lisan** mengubah bahasa ucapan default SandFly. Saat profil suara sedang digunakan, profil tersebut yang menentukan bahasa, dan kontrol pembacaan ini tidak memiliki nilai. Kontrol pembacaan ini tidak ada di kontrol pembacaan secara default.

### Pengucapan {#speech-pronunciation}

Grup **Pengucapan** berisi:

- **Gunakan perubahan tinggi nada**: mengucapkan huruf yang dihapus dengan nada suara lebih tinggi. Aktif secara default.
- **Ucapkan huruf secara fonetik saat meninjau**: untuk huruf yang Anda telusuri per karakter dan teks yang dieja.
- **Ucapkan huruf secara fonetik saat mengetik**: untuk tombol keyboard dan huruf yang Anda ketik atau hapus.
- **Kirim teks panjang per kalimat**: untuk mesin ucapan yang lambat berhenti, seperti Gryphon, agar geseran langsung menghentikan pembacaan teks panjang. Banyak mesin memberi jeda lebih lama di antara kalimat yang dikirim terpisah, jadi biarkan nonaktif jika mesin Anda berhenti dengan cepat. Nonaktif secara default.
- **Baca pemisah baris sebagai spasi**: membaca teks tanpa jeda pada pergantian barisnya. Nonaktif secara default.
- **Kamus ucapan** dan **Kamus simbol**: lihat [](#speech-dictionary).

Kedua setelan huruf fonetik masing-masing memiliki empat pilihan:

- **Nonaktif**: hanya mengucapkan hurufnya.
- **Setelah huruf, dengan jeda, seperti "h, hotel"**: default. Saat Anda berpindah per karakter, katanya diucapkan setelah jeda singkat. Jika Anda berpindah lagi sebelum jeda selesai, kata itu dilewati.
- **Setelah huruf, tanpa jeda, seperti "h hotel"**.
- **Sebagai pengganti huruf, seperti "hotel"**.

### Kamus ucapan {#speech-dictionary}

**Ucapan** > **Kamus ucapan** mengubah cara teks diucapkan pada semua ucapan, seperti suatu kata diucapkan sebagai kata lain, atau suatu simbol diucapkan sebagai spasi. Entri diterapkan secara berurutan, masing-masing terhadap teks hasil entri sebelumnya. Layar setelan SandFly tetap dibaca sebagaimana tertulis.

Untuk menambahkan entri:

1. Buka **Ucapan** > **Kamus ucapan**.
2. Pilih **Tambahkan pengganti**.
3. Di **Teks**, masukkan teks yang ingin dicari. Di **Ucapkan sebagai**, masukkan teks penggantinya. Kosongkan **Ucapkan sebagai** untuk menghilangkan teks tersebut dari ucapan.
4. Pilih **Hanya kata utuh** (aktif secara default) agar hanya cocok dengan kata utuh, dan **Ekspresi reguler** untuk mencocokkan ekspresi reguler.
5. Pilih **Oke**.

Pilih sebuah entri untuk **Edit**, **Pilih aplikasi**, dan **Hapus**. **Pilih aplikasi** membatasi entri hanya untuk satu aplikasi, atau **Semua aplikasi**. Entri untuk satu aplikasi berlaku saat jendela aplikasi tersebut adalah jendela terakhir yang ditampilkan.

**Impor dari file** dan **Ekspor ke file** memuat dan menyimpan entri sebagai file. Lihat [](#api-item-files).

Kamus braille, untuk penampil braille, ada di setelan braille. Lihat [](#braille).

#### Kamus simbol {#speech-symbol-dictionary}

**Ucapan** > **Kamus simbol** mengganti simbol dan rangkaian simbol dalam ucapan serta di penampil braille. Entri mencocokkan teks persisnya di mana saja, termasuk tanda titik langsung setelah kata, dan tidak memiliki opsi kata utuh atau ekspresi reguler. Entri diterapkan secara berurutan, setelah kamus ucapan atau kamus braille. Anda dapat menambahkan hingga 500 entri.

Pilih **Tambahkan pengganti**, masukkan **Simbol** dan teks pada **Ganti dengan**, lalu pilih **Oke**. Entri memiliki opsi **Edit**, **Pilih aplikasi**, **Hapus**, **Impor dari file**, dan **Ekspor ke file** yang sama dengan kamus ucapan.

Dalam ucapan, entri untuk tanda baca yang tidak dibacakan oleh mesin ucapan, seperti titik atau koma, tidak berlaku jika **Panjang respons** > **Ucapkan tanda baca dan simbol** mengecualikan tanda baca tersebut. Jika disetel ke **Tidak ada**, tidak ada entri untuk satu simbol tunggal yang berlaku dalam teks, kecuali jika **Ucapkan simbol yang mengubah makna** mempertahankan simbol tersebut. Lihat [](#verbosity-text).

### Menjeda dan melanjutkan ucapan {#speech-pause}

Perintah **Jeda atau lanjutkan ucapan** menjeda ucapan dan melanjutkannya kembali. Secara default, perintah ini menggunakan ketukan dua jari. Lihat [](#appendix-gestures).

Saat ucapan dilanjutkan:

- Dengan **Output audio** > **Audio latensi rendah** aktif, ucapan berhenti di tengah kata dan berlanjut dari titik yang sama. SandFly menyimpan hingga 10 detik ucapan yang dijeda.
- Jika **Audio latensi rendah** nonaktif, pada mesin yang melaporkan posisi kata, ucapan berlanjut dari kata tempat ucapan berhenti.
- Pada mesin yang tidak melaporkan posisi kata, seperti RHVoice dan Gryphon, SandFly memperkirakan sejauh mana mesin telah membaca berdasarkan kecepatan bicara item sebelumnya, lalu melanjutkan dari awal kalimat atau frasa tersebut. Ucapan bisa mengulang beberapa kata, tetapi tidak ada yang terlewat.

SandFly tidak memutar ulang suara dan getaran item saat ucapan dilanjutkan.

Perintah **Aktifkan atau nonaktifkan ucapan** menonaktifkan dan mengaktifkan semua ucapan.

Pada keyboard fisik, menekan Ctrl saja akan menjeda ucapan. Lihat [](#keyboards).

### Menghentikan ucapan {#speech-stop}

Grup **Menghentikan ucapan** mengatur hal yang menghentikan ucapan:

- **Hentikan ucapan saat menyentuh layar**: aktif secara default.
- **Hentikan ucapan saat mengetik**: kapan karakter yang diketik pada keyboard fisik menghentikan ucapan. **Selalu** (default), **Di kolom teks**, atau **Pada gema keyboard**, yaitu hanya saat gema keyboard mengucapkan karakter tersebut atau kata yang diakhirinya. Tidak tersedia di jam tangan.
- **Hentikan ucapan saat menekan Enter**: menekan Enter pada keyboard fisik menghentikan ucapan. Aktif secara default. Tidak tersedia di jam tangan.
- **Tutupi sensor kedekatan untuk menghentikan ucapan**: menutupi sensor kedekatan akan menghentikan ucapan. Nonaktif secara default. Tidak ditampilkan di TV atau perangkat tanpa sensor kedekatan.
- **Senyap saat merekam**: saat sebuah aplikasi merekam tanpa headphone terhubung, SandFly hanya berbicara dalam 2 detik setelah sentuhan, gerakan, penekanan tombol, atau input braille, dan diam untuk ucapan lain seperti notifikasi agar tidak ikut terekam. Nonaktif secara default.

Pada keyboard fisik, setiap tombol kecuali tombol pengubah, tombol pengunci (lock), dan tombol volume akan menghentikan ucapan, demikian juga semua pintasan dengan Ctrl, Alt, atau Meta. Karakter yang diketik dan tombol Enter mengikuti setelan di atas. Spasi di luar kolom teks selalu menghentikan ucapan.

Saat aplikasi lain merekam dari mikrofon dan selama panggilan berlangsung, SandFly tetap mengucapkan apa yang Anda minta dengan gerakan atau tombol, seperti waktu, tingkat baterai, atau judul jendela.

### Histori ucapan {#speech-history}

Perintah **Histori ucapan**, di grup perintah **Membaca**, membuka daftar 100 teks terakhir yang diucapkan SandFly, dimulai dari yang terbaru.

- Pilih sebuah teks untuk **Salin**, dan untuk **Tampilkan semua** jika teks dipendekkan dalam daftar.
- **Hapus histori**, di bagian akhir daftar, menghapus semua teks.

Histori hanya disimpan di memori, tidak pernah disimpan ke penyimpanan atau dicatat dalam log, dan dihapus saat SandFly dinonaktifkan. Histori tidak menyertakan ucapan saat kolom sandi sedang difokuskan atau ucapan saat daftar histori sedang terbuka. Setiap teks disimpan hingga 2.000 karakter.

Untuk menggunakan perintah ini, tetapkan ke gerakan atau pintasan keyboard, atau tambahkan ke menu. Lihat [](#commands).

### Mengizinkan aplikasi lain berbicara {#speech-other-apps}

Aplikasi dan skrip dapat mengirim teks ke SandFly untuk diucapkan menggunakan suara dan setelan SandFly Anda. **Ucapan** > **Izinkan aplikasi lain berbicara** mengatur aplikasi mana saja yang diizinkan:

- **Izinkan aplikasi lain berbicara**: mengaktifkan atau menonaktifkan ucapan dari aplikasi lain. Nonaktif secara default.
- **Putar suara sebelum aplikasi lain berbicara**: memutar suara sebelum setiap teks dari aplikasi lain agar tidak tertukar dengan ucapan SandFly. Nonaktif secara default.
- **Aplikasi**: setiap aplikasi yang pernah terhubung, dengan kotak centang untuk mengizinkannya. Aplikasi yang dapat berbicara saat perangkat terkunci juga memiliki kotak centang dengan namanya diikuti "saat perangkat terkunci". "Belum ada aplikasi yang meminta izin untuk berbicara." berarti daftar masih kosong.

Saat aplikasi pertama kali mencoba berbicara, SandFly menampilkan notifikasi "Izinkan [nama aplikasi] berbicara melalui SandFly?", dengan pilihan **Izinkan** dan **Jangan izinkan**. Tidak ada teks dari aplikasi yang diucapkan hingga Anda mengizinkannya. Tidak ada teks dari aplikasi lain yang diucapkan saat SandFly nonaktif atau dijeda, dan aplikasi lain tidak dapat menghentikan atau mengganti ucapan SandFly.

Keyboard yang sedang digunakan dapat berbicara tanpa harus ada di daftar ini, dan dapat menonaktifkan gema keyboard SandFly untuk pengetikannya sendiri.

Untuk developer, lihat [](#api-speech).

### Perintah ucapan dan kontrol pembacaan {#speech-commands}

Grup perintah **Ucapan**, antara lain, berisi: **Jeda atau lanjutkan ucapan**, **Aktifkan atau nonaktifkan ucapan**, **Tambah kecepatan bicara**, **Kurangi kecepatan bicara**, **Naikkan tinggi nada ucapan**, **Turunkan tinggi nada ucapan**, **Ubah bahasa lisan**, **Ganti bahasa secara otomatis**, **Kenali bahasa dari kata**, **Pilih profil suara**, **Profil suara berikutnya**, **Profil suara sebelumnya**, dan **Gunakan efek ucapan**. Pintasan keyboard menambahkan **Naikkan volume ucapan**, **Turunkan volume ucapan**, **Kecepatan bicara dan tinggi nada disetel ulang**, dan **Buka setelan text-to-speech**.

Grup **Membaca** berisi **Ulangi frasa yang terakhir diucapkan**, **Ulangi frasa yang terakhir diucapkan lebih lambat**, **Eja frasa yang terakhir diucapkan**, dan **Histori ucapan**.

Kontrol pembacaan ucapan mencakup **Kecepatan bicara**, **Bahasa lisan**, **Mesin ucapan**, **Profil suara**, **Efek ucapan**, dan pita-pita equalizer. Hanya **Kecepatan bicara** yang ada di kontrol pembacaan secara default. Kontrol pembacaan **Volume ucapan** dan **Perangkat output audio** dijelaskan di [](#sound-reading-controls). Grup **Ucapan** di **Kontrol pembacaan** juga berisi **Ucapkan tanda baca dan simbol**, **Emoji**, dan **Ucapkan pemformatan teks** (lihat [](#verbosity-text)), **Terjemahan otomatis** (lihat [](#translation)), serta **Ucapkan hanya fokus yang dipindahkan pengguna** (lihat [](#verbosity-own-speech)).

Untuk menetapkan perintah ke gerakan dan pintasan keyboard, serta menambahkan kontrol pembacaan, lihat [](#commands).
