## Makro {#macros}

Makro adalah daftar langkah bernama yang dijalankan SandFly secara berurutan. Langkahnya dapat berupa perintah, ketukan pada item, teks yang diketik, jeda tunggu, atau langkah yang memeriksa layar lalu mengulangi atau melewati langkah lain. Setiap makro adalah sebuah perintah, sehingga gerakan, pintasan keyboard, item menu, perintah suara, atau pintasan layar utama dapat menjalankannya.

Anda membuat makro di **Makro** pada setelan SandFly, dengan menambahkan langkah atau dengan merekam tindakan Anda. Makro tidak tersedia di jam tangan. Skrip dapat melakukan lebih banyak hal daripada makro. Lihat [](#scripts).

### Membuat makro {#macros-add}

1. Di setelan SandFly, pilih **Makro**.
2. Pilih **Tambahkan makro** dan masukkan nama. SandFly membuka layar makro baru tersebut.
3. Pilih **Tambahkan langkah**, pilih jenis langkahnya, lalu isi apa yang harus dilakukan langkah itu. Lihat [](#macros-steps).
4. Ulangi langkah 3 untuk setiap langkah.
5. Untuk mencoba makro, pilih **Jalankan sekarang**.
6. Untuk menjalankannya dengan gerakan atau pintasan keyboard, pilih **Gerakan dan pintasan keyboard**. Lihat [](#commands-assign).

Untuk membuat langkah dengan cara melakukannya sendiri, lihat [](#macros-record).

### Layar Makro {#macros-screen}

Layar **Makro** memiliki:

- **Tambahkan makro**: membuat makro untuk semua aplikasi.
- **Tambahkan aplikasi**: memilih aplikasi dan membuka daftar makronya, tempat **Tambahkan makro** membuat makro yang hanya berjalan di aplikasi tersebut.
- **Impor dari file** dan **Ekspor ke file**: membaca makro dari file, atau menyimpan semua makro ke file. Pada daftar milik suatu aplikasi, **Ekspor ke file** menyimpan makro aplikasi tersebut. Lihat [](#api-item-files).
- **Semua aplikasi**: makro yang berjalan di semua aplikasi.
- **Aplikasi**: setiap aplikasi yang memiliki makro, beserta jumlah makronya. Pilih aplikasi untuk membuka daftarnya.

Makro juga disimpan dalam cadangan setelan. Lihat [](#profiles).

### Layar makro {#macros-macro-screen}

Layar sebuah makro memiliki:

- **Jalankan sekarang**: menjalankan makro. Saat SandFly nonaktif, item ini menampilkan "SandFly nonaktif".
- **Tambahkan langkah**: menambahkan langkah. Di bawahnya, SandFly menunjukkan posisi langkah baru: **Di akhir**, **Di awal**, atau setelah suatu langkah. Langkah baru masuk setelah langkah yang terakhir Anda tambahkan. Jadi, langkah yang ditambahkan setelah **Jika** atau perulangan masuk ke dalam bloknya.
- **Rekam langkah**: merekam langkah di posisi yang sama dengan tempat **Tambahkan langkah** menambahkan langkah. Lihat [](#macros-record).
- **Jalankan hanya jika**: kondisi yang harus terpenuhi agar makro berjalan. **Selalu** secara default. Jika kondisi tidak terpenuhi, makro tidak melakukan apa pun, dan **Jalankan sekarang** mengucapkan "Makro tidak dijalankan. Jalankan hanya jika: tidak terpenuhi". Lihat [](#macros-conditions).
- **Aplikasi**: aplikasi tempat makro berjalan, atau **Semua aplikasi** (default). Makro dengan aplikasi tertentu hanya berjalan saat aplikasi tersebut sedang tampil. Di tempat lain, **Jalankan sekarang** mengucapkan "Makro tidak dijalankan. Hanya berjalan di" diikuti nama aplikasi tersebut.
- **Tampilkan di Tindakan**: mencantumkan makro di menu **Tindakan** dan di kontrol pembacaan **Tindakan** untuk setiap item di aplikasinya, setelah tindakan milik aplikasi. Untuk makro **Semua aplikasi**, makro dicantumkan di setiap aplikasi. Memilih makro di sana akan menjalankannya. Nonaktif secara default.
- **Tampilkan di Tindakan hanya jika**: ditampilkan saat **Tampilkan di Tindakan** aktif. Ini adalah kondisi yang harus terpenuhi pada item yang difokuskan agar makro dicantumkan. Jika ada kondisi, makro dihitung sebagai tindakan item untuk **Ubah kontrol pembacaan secara otomatis**, sehingga kontrol pembacaan **Tindakan** dipilih pada item tersebut. Jika tidak ada kondisi, hal ini tidak terjadi.
- **Langkah**: daftar langkah secara berurutan. Langkah di dalam blok **Jika** atau perulangan ditampilkan menjorok ke dalam.
- **Gerakan dan pintasan keyboard**: gerakan, tombol panah, sel braille, dan pintasan keyboard yang menjalankan makro.
- **Tambahkan ke layar utama**: menambahkan pintasan layar utama yang menjalankan makro di layar mana pun Anda membukanya. Mengganti nama makro juga mengganti nama pintasannya. Menghapus makro akan menonaktifkan pintasannya. Tidak semua peluncur mendukung pintasan.
- **Ganti nama**, **Ekspor ke file**, dan **Hapus makro**.

Pilih langkah untuk tindakan berikut, yang juga tersedia sebagai tindakan aksesibilitas pada langkah tersebut: **Edit**, **Tambahkan langkah setelah**, **Pindahkan ke atas**, **Pindahkan ke bawah**, dan **Hapus**. Menghapus **Jika** atau perulangan juga menghapus langkah **Jika tidak, jika**, **Jika tidak**, dan langkah penutupnya, tetapi langkah di dalamnya tetap dipertahankan. Satu makro memiliki maksimal 200 langkah.

Jika blok makro tidak berpasangan, seperti **Jika** tanpa **Akhir jika**, layar menampilkan langkah dan masalahnya di bawah **Langkah**, dan makro tidak dijalankan.

### Langkah {#macros-steps}

**Tambahkan langkah** menyediakan langkah-langkah berikut:

- **Jalankan perintah**: menjalankan perintah apa pun selain makro. Pilih kategori, lalu perintahnya.
- **Ketuk item**: mengaktifkan item pertama di layar yang teks atau deskripsinya memuat teks tertentu, tanpa membedakan huruf besar/kecil. Item yang seluruh teksnya cocok didahulukan. SandFly menunggu item muncul selama waktu yang diatur di **Lama menunggu item (detik)**, 3 detik secara default.
- **Ketuk di posisi**: mengetuk layar pada koordinat X dan Y dalam piksel dari kiri atas. Memerlukan Android 11 ke atas.
- **Sentuh lama item**: menyentuh bagian tengah item selama **Detik untuk menahan**, 1 detik secara default, lalu mengangkatnya di tempat, atau menggeser ke atas, bawah, kiri, atau kanan sejauh jarak tertentu dalam dp sebelum mengangkatnya. Langkah ini berguna untuk tombol yang bekerja selama ditahan, seperti tombol pesan suara di aplikasi chat. Memerlukan Android 11 atau yang lebih baru.
- **Fokus ke item**: menemukan item dengan cara yang sama seperti **Ketuk item**, lalu memindahkan fokus aksesibilitas ke item itu sehingga item dibacakan.
- **Ucapkan item**: menemukan item dengan cara yang sama seperti **Ketuk item**, lalu membacakan namanya tanpa memindahkan fokus.
- **Sentuh lama atau gulir item**: menyentuh lama, menggulir maju, atau menggulir mundur suatu item beberapa kali. Jika item tidak mendukung tindakan itu, SandFly menggunakan item terdekat di sekitarnya yang mendukung, seperti bilah geser. Jika teks item dikosongkan, pengguliran dilakukan pada daftar terbesar di layar.
- **Ucapkan teks**: membacakan sebuah teks.
- **Ketik teks**: mengetikkan teks di kolom teks yang difokuskan, pada posisi kursor. Dengan **Ganti teks di kolom**, teks tersebut menggantikan seluruh teks di kolom.
- **Buka aplikasi**: membuka sebuah aplikasi.
- **Langkah lainnya**: langkah yang bekerja di luar layar. Lihat [](#macros-more-steps).
- **Tunggu**: menunggu sejumlah detik, hingga 600 detik.
- **Tunggu sampai**: menunggu hingga suatu kondisi terpenuhi. **Lama menunggu (detik)** adalah 10 detik secara default. Makro berhenti jika waktu habis.
- **Jika**, **Jika tidak, jika**, **Jika tidak**, dan **Akhir jika**: menjalankan langkah pada cabang pertama yang kondisinya terpenuhi. Lihat [](#macros-if).
- **Ulangi**, **Ulangi selama**, **Akhir ulangi**, dan **Keluar dari perulangan**: menjalankan langkah lebih dari sekali. Lihat [](#macros-repeat).
- **Hentikan makro**: menghentikan makro.

Jika suatu langkah gagal, SandFly mengucapkan "Makro dihentikan. Gagal:" diikuti nama langkahnya, lalu makro berhenti. Jika item suatu langkah tidak ditemukan, SandFly mengucapkan "Makro dihentikan. Tidak ditemukan:" diikuti teks item tersebut. **Tunggu sampai** yang kehabisan waktu mengucapkan "Makro dihentikan. Waktu habis:" diikuti nama langkahnya.

#### Menemukan item {#macros-find-items}

Langkah dan pengujian yang mencari item memiliki kolom berikut:

- **Teks item**: teks yang dicari.
- **Ekspresi reguler**: memperlakukan **Teks item** sebagai ekspresi reguler, yang dicocokkan di bagian mana pun dari teks tanpa membedakan huruf besar/kecil. Grup berulang tidak boleh berisi pengulangan atau `|`, seperti `(a+)+`.
- **Cocokkan di**: tempat pencarian. **Teks apa pun** (default), atau hanya **Teks**, **Deskripsi**, **Petunjuk**, **Status**, **Tooltip**, atau **ID sumber daya**. ID sumber daya cocok dengan atau tanpa nama paketnya, sehingga `send` cocok dengan `com.example:id/send`.
- **Jenis item**, kecuali di **Fokus ada di dalam item**: **Jenis apa pun** (default), atau hanya item yang **Dapat diklik**, **Dapat disentuh lama**, **Dapat diedit**, **Dapat digulir**, **Dapat disesuaikan**, **Dapat dicentang**, **Dicentang**, **Dipilih**, **Judul**, atau **Diaktifkan**. Dengan **Dapat diklik**, **Ketuk item** mengetuk tombol, bukan judul yang memiliki teks yang sama. Pada pengujian, jenis item ditanyakan setelah teks.

Untuk melihat isi setiap kolom suatu item dan jenis item yang dimilikinya, gunakan **Periksa item yang difokuskan**. Lihat [](#macros-inspect).

#### Langkah lainnya {#macros-more-steps}

- **Putar suara**: memutar salah satu suara SandFly.
- **Getar** dan **Putar nada**.
- **Salin teks**: menyalin teks ke papan klip.
- **Buka tautan**: membuka sebuah alamat.
- **Buka halaman setelan**: membuka halaman setelan Android, seperti Wi-Fi, Bluetooth, atau Baterai.
- **Bagikan teks**: membuka layar berbagi dengan sebuah teks.
- **Setel volume**: menyetel volume suatu aliran audio dalam persen, seperti media, dering, atau aksesibilitas.
- **Setel mode pendering**: normal, getar, atau senyap.
- **Setelan perangkat**: mengubah kontrol pada panel setelan cepat SandFly, seperti Wi-Fi, senter, Jangan Ganggu, atau kecerahan. Untuk sakelar, gunakan **Aktifkan**, **Nonaktifkan**, atau **Aktifkan atau nonaktifkan**. Untuk penggeser, gunakan angka, persentase seperti 50%, atau +1 atau -1. Untuk pilihan, gunakan salah satu opsi atau `next`. Lihat [](#display).
- **Tombol media**: **Putar atau jeda**, **Putar**, **Jeda**, **Berikutnya**, **Sebelumnya**, **Berhenti**, **Mundur**, atau **Maju cepat**.
- **Setel pengatur waktu**: jumlah detik, hingga 86.400, dengan label opsional.
- **Setel alarm**: waktu seperti 08:30, dengan label opsional.
- **Telepon**: menelepon nomor telepon.
- **Kirim pesan**: menulis pesan ke nomor telepon.
- **Buka kamera**: untuk foto atau video.
- **Tekan tombol**: menekan tombol keyboard, seperti `tab enter`. Memerlukan Shizuku. Lihat [](#updates).
- **Tampilkan pesan**: menampilkan dialog berisi teks, dan menunggu hingga Anda menutupnya.
- **Setel nilai**: menyimpan teks sebagai nilai dengan sebuah nama.
- **Ambil isi tautan**: mengambil teks dari alamat https dan menyimpannya sebagai nilai.
- **Terjemahkan teks**: menerjemahkan teks ke bahasa perangkat dan menyimpannya sebagai nilai. Lihat [](#translation).

Nama nilai menggunakan huruf, angka, titik, tanda hubung, dan garis bawah. Nilai tetap tersimpan dari satu kali jalan ke kali berikutnya, dan dipakai bersama oleh semua makro Anda.

Teks pada **Ucapkan teks**, **Ketik teks**, dan langkah-langkah di atas dapat memuat nilai sebagai `{name}`. Teks tersebut juga dapat memuat nilai berikut, yang tersedia setiap kali makro berjalan: `{item}` (teks item yang difokuskan), `{clipboard}`, `{time}`, `{date}`, `{battery}` (level baterai), serta `{device.wifi}` dan sejenisnya (status setelan perangkat).

### Kondisi {#macros-conditions}

Kondisi adalah daftar pengujian. **Jika**, **Jika tidak, jika**, **Ulangi selama**, **Tunggu sampai**, **Jalankan hanya jika**, dan **Tampilkan di Tindakan hanya jika** masing-masing memilikinya. Pilih langkah atau setelan tersebut untuk membuka kondisinya.

Layar kondisi memiliki **Tambahkan pengujian** dan **Pengujian**. Untuk **Tunggu sampai**, layar ini juga memiliki **Lama menunggu (detik)**. Kondisi tanpa pengujian selalu terpenuhi.

Jenis pengujian yang tersedia:

- **Item di layar**: ada item di layar dengan teks tertentu, dan dengan jenis item tertentu jika Anda memilihnya. Lihat [](#macros-find-items).
- **Item yang difokuskan**: item dengan fokus aksesibilitas, atau item di dalamnya, memiliki teks, jenis item, atau keduanya. Teks bersifat opsional.
- **Fokus ada di dalam item**: item yang difokuskan berada di dalam item dengan teks tertentu, atau merupakan item tersebut, seperti pemutar video yang dicari melalui ID sumber dayanya.
- **Aplikasi di depan**: suatu aplikasi sedang tampil di depan.
- **Keyboard ditampilkan**: keyboard di layar sedang ditampilkan.
- **Layar terkunci**: layar kunci sedang ditampilkan.
- **Nilai**: suatu nilai memuat teks tertentu. Jika **Berisi** kosong, pengujian ini memeriksa apakah nilai tersebut sudah disetel. Dengan **Ekspresi reguler**, teks tersebut adalah ekspresi reguler.

Saat kondisi sudah memiliki pengujian, **Tambahkan pengujian** terlebih dahulu menanyakan **Gabungkan dengan "dan"** atau **Gabungkan dengan "atau"**. Pilih pengujian untuk tindakan berikut:

- **Edit**.
- **Ubah ke**: membalik pengujian, seperti **Keyboard disembunyikan**, **Layar tidak terkunci**, **Tidak ada item di layar**, atau **Aplikasi tidak di depan**.
- **Gabungkan dengan "dan"** atau **Gabungkan dengan "atau"**, untuk pengujian setelah yang pertama.
- **Pindahkan ke atas**, **Pindahkan ke bawah**, dan **Hapus**.

"Dan" didahulukan daripada "atau". Artinya, kondisi terpenuhi jika semua pengujian dalam salah satu kelompok yang dipisahkan oleh "atau" terpenuhi. Satu kondisi memiliki maksimal 20 pengujian.

#### Jika {#macros-if}

**Tambahkan langkah** > **Jika** meminta sebuah pengujian lalu menambahkan **Jika** beserta **Akhir jika**. Langkah yang Anda tambahkan berikutnya masuk di antara keduanya. **Jika tidak, jika** menambahkan cabang lain dengan kondisi. **Jika tidak** menambahkan cabang yang dijalankan jika tidak ada kondisi sebelumnya yang terpenuhi. **Jika tidak, jika** atau **Jika tidak** yang ditambahkan tepat setelah **Akhir jika** dimasukkan sebelum **Akhir jika** tersebut.

#### Ulangi {#macros-repeat}

- **Ulangi**: menjalankan langkah hingga **Akhir ulangi** sejumlah kali, dari 1 hingga 1.000.
- **Ulangi selama**: menjalankan langkah hingga **Akhir ulangi** selama kondisi terpenuhi, maksimal 1.000 kali dalam satu kali makro dijalankan. Setelah 1.000 kali, SandFly mengucapkan "Makro dihentikan. Diulang 1000 kali:" diikuti nama langkahnya, lalu makro berhenti.
- **Keluar dari perulangan**: melanjutkan ke langkah setelah **Akhir ulangi** milik perulangan yang memuat langkah ini.

**Tambahkan langkah** menambahkan blok perulangan beserta **Akhir ulangi**, dan langkah yang Anda tambahkan berikutnya masuk ke dalamnya.

Untuk menggulir daftar hingga suatu item muncul lalu membukanya:

1. Tambahkan **Ulangi selama** dengan pengujian **Item di layar** dan teks item tersebut, lalu pilih **Ubah ke** agar pengujian menjadi **Tidak ada item di layar**.
2. Tambahkan **Sentuh lama atau gulir item** dengan pilihan **Gulir maju**, tanpa teks item.
3. Setelah **Akhir ulangi**, tambahkan **Ketuk item** dengan teks item tersebut.

### Merekam makro {#macros-record}

SandFly dapat merekam langkah makro sementara Anda melakukannya di aplikasi.

Untuk merekam makro baru:

1. Buka layar tempat makro dimulai.
2. Gunakan perintah **Rekam makro** di kategori **Makro**. Tetapkan ke gerakan terlebih dahulu, atau tambahkan ke menu SandFly. Lihat [](#commands-assign) dan [](#commands-edit-menu).
3. SandFly membuat makro bernama "Rekaman" diikuti tanggal dan waktu, lalu menampilkan notifikasi **Merekam langkah makro**.
4. Lakukan langkah-langkahnya.
5. Untuk berhenti, gunakan **Rekam makro** lagi, gunakan **Hentikan makro**, atau buka notifikasinya. SandFly membuka makro tersebut dan mencantumkan langkah yang terekam.
6. Pilih **Simpan** untuk menambahkan langkah, **Rekam lagi** untuk mengulang dari awal, atau **Batal**. **Batal** menghapus makro tersebut.

Untuk merekam langkah ke dalam makro yang sudah ada, pilih **Rekam langkah** di layarnya. SandFly kembali ke aplikasi yang terbuka sebelum setelan, atau membuka aplikasi milik makro tersebut, lalu mulai merekam. Langkah masuk ke posisi yang sama dengan tempat **Tambahkan langkah** menambahkan langkah.

SandFly merekam ketukan dan sentuhan lama pada item (langsung dengan jari atau melalui SandFly), pengguliran, teks yang diketik, aplikasi yang dibuka dari peluncur, serta perintah **Kembali**, **Layar utama**, **Ringkasan**, **Notifikasi**, **Setelan Cepat**, **Semua aplikasi**, **Pilih semua**, **Salin**, **Potong**, dan **Tempel**. Perintah notifikasi, Setelan Cepat, ringkasan, atau semua aplikasi yang Anda gunakan di akhir untuk menghentikan perekaman tidak ikut direkam. Teks yang diketik di kolom sandi dan tindakan di layar SandFly tidak direkam.

Langkah yang direkam juga menangkap maksud tindakan Anda:

- Pengguliran dalam daftar yang diikuti ketukan pada salah satu itemnya menjadi **Ulangi selama** yang menggulir hingga item ada di layar, lalu ketukan tersebut.
- Satu tindakan, atau rangkaian tindakan, yang dilakukan beberapa kali berturut-turut menjadi **Ulangi**. Jika item pertama yang ditekan sudah hilang setelah tekanan terakhir, tindakan itu menjadi **Ulangi selama** item tersebut ada di layar.
- Ketukan yang mengaktifkan sakelar atau kotak centang hanya dijalankan saat statusnya nonaktif. Ketukan yang menonaktifkannya hanya dijalankan saat statusnya aktif.
- Ketukan di peluncur yang membuka aplikasi menjadi **Buka aplikasi**.
- Jika tindakan pertama dilakukan di aplikasi yang sedang tampil saat perekaman dimulai, makro membuka aplikasi tersebut lebih dulu jika aplikasi itu belum tampil.

Langkah yang direkam mencari item berdasarkan seluruh teksnya, dalam bentuk ekspresi reguler. Setelah itu, Anda dapat mengedit langkahnya agar item ditemukan berdasarkan sebagian teks.

### Menjalankan makro {#macros-run}

- Gerakan, pintasan keyboard, tombol panah, atau sel braille yang ditetapkan padanya. Lihat [](#commands-assign).
- Item menu: tambahkan makro ke menu SandFly, menu kustom, atau kontrol pembacaan kustom dari kategori **Makro**. Lihat [](#commands).
- Menu atau kontrol pembacaan **Tindakan**, jika **Tampilkan di Tindakan** aktif.
- Pintasan layar utama, dengan **Tambahkan ke layar utama**.
- Perintah suara: ucapkan nama makro, atau "jalankan makro" diikuti namanya. Perintah suara kustom juga dapat menjalankannya. Lihat [](#voice-commands).
- **Jalankan sekarang** di layar makro.

Memulai makro akan menghentikan makro lain yang sedang berjalan. Perintah **Hentikan makro** menghentikan makro yang sedang berjalan. Saat makro dihapus, makro itu juga dihapus dari gerakan, tombol, dan menu yang menjalankannya.

### Memeriksa item untuk makro {#macros-inspect}

**Periksa item yang difokuskan**, perintah di kategori **Setelan developer**, membuka **Detail item** untuk item yang difokuskan. Jendela ini memiliki judul berikut:

- **Teks**: teks, deskripsi, petunjuk, dan status item, diberi nama seperti pada **Cocokkan di**.
- **Jenis item**: jenis item yang dimiliki item tersebut, seperti **Dapat diklik** atau **Judul**.
- **Item di dalamnya**: teks dari item-item yang ada di dalamnya.
- **Detail skrip**: ID sumber daya, peran, kelas, dan detail lain yang digunakan skrip.
- **Aplikasi**: aplikasi, aktivitas, dan jendela.

Aktifkan suatu baris untuk menyalin nilainya. **Salin semua** menyalin semua detail sebagai teks. Pada versi build dengan dukungan skrip, **Tambahkan aturan untuk item ini** membuka pembuat skrip dengan item tersebut. Lihat [](#scripts).

**Salin detail item** menyalin detail yang sama tanpa membuka jendela, dan mengucapkan "Detail item disalin". **Salin hierarki layar** menyalin semua item di layar sebagai teks, satu baris per item beserta kelas, ID, peran, status, dan teksnya. Hasilnya berguna untuk menulis skrip.

**Periksa item yang difokuskan**, **Salin detail item**, dan **Salin hierarki layar** dapat ditambahkan ke menu SandFly. Ketiganya nonaktif secara default. Untuk memeriksa item, pindahkan fokus ke item tersebut, buka menu SandFly, lalu pilih **Periksa item yang difokuskan**. Jendela akan menampilkan item yang difokuskan sebelum menu dibuka.
