## Memulai {#getting-started}

SandFly adalah pembaca layar untuk Android. SandFly membacakan apa yang ada di layar dengan text-to-speech, memutar suara dan getaran, serta mengubah cara kerja gestur sentuh agar Anda dapat menggunakan perangkat tanpa melihat layar. SandFly juga dapat menampilkan layar pada layar braille serta menerima masukan dari keyboard braille di layar atau keyboard fisik.

### Perangkat dan versi Android {#getting-started-devices}

SandFly berjalan di:

- Ponsel dan tablet dengan Android 8 atau yang lebih baru.
- Jam tangan Wear OS. Lihat [](#devices-watch).
- Android TV dan Google TV. Lihat [](#devices-tv).

Beberapa fitur memerlukan versi Android yang lebih baru, dan beberapa memerlukan Shizuku atau root, yang memberi SandFly izin yang hanya diberikan Android melalui adb. Bagian yang menjelaskan fitur tersebut akan menyebutkannya. Lihat [](#updates-shizuku). SandFly Pro adalah langganan yang menambahkan fitur AI. Lihat [](#pro).

### Menginstal SandFly {#getting-started-install}

SandFly diterbitkan sebagai file APK di [halaman rilis SandFly](https://github.com/aaron-gh/SandFly/releases/latest). `sandfly.apk` untuk ponsel, tablet, dan TV, sedangkan `sandfly-wear.apk` untuk jam tangan.

Untuk menginstal SandFly di ponsel atau tablet:

1. Di ponsel, buka [rilis terbaru](https://github.com/aaron-gh/SandFly/releases/latest) di browser.
2. Unduh `sandfly.apk`.
3. Buka file yang telah diunduh.
4. Jika Android meminta izin, izinkan browser atau pengelola file untuk menginstal aplikasi, lalu kembali dan buka file itu lagi.
5. Pilih **Instal**.

SandFly terinstal sebagai aplikasi terpisah dengan setelannya sendiri, berdampingan dengan pembaca layar lain yang ada di perangkat. Setelah itu, SandFly menginstal pembaruannya sendiri. Lihat [](#updates).

Catatan: Di Android 13 dan yang lebih baru, Android dapat memblokir layanan aksesibilitas yang diinstal dari browser atau pengelola file. Jika SandFly tidak dapat diaktifkan di setelan aksesibilitas, buka info aplikasi untuk SandFly di setelan Android, buka menu **Lainnya**, lalu pilih **Izinkan setelan yang dibatasi**. Nama item ini bisa berbeda di beberapa ponsel.

### Mengaktifkan SandFly {#getting-started-turn-on}

Nonaktifkan pembaca layar lain sebelum Anda mengaktifkan SandFly. Dua pembaca layar yang aktif bersamaan akan sama-sama membacakan layar dan sama-sama menangani gestur.

1. Buka aplikasi **Setelan** Android.
2. Buka **Aksesibilitas**. Di beberapa ponsel, SandFly berada di subbagian seperti **Aplikasi terinstal** atau **Aplikasi yang didownload**.
3. Pilih **SandFly**.
4. Aktifkan tombol pengalihnya, lalu konfirmasi.

SandFly mengucapkan "SandFly aktif". Pada saat pertama kali, SandFly membuka dialog tata letak gestur dan tutorial. Lihat [](#getting-started-first-start).

#### Mengaktifkan dan menonaktifkan SandFly dengan pintasan aksesibilitas {#getting-started-shortcut}

Android dapat mengaktifkan dan menonaktifkan SandFly dengan pintasan aksesibilitas, seperti menahan kedua tombol volume selama beberapa detik, atau tombol aksesibilitas. Untuk mengaturnya, buka **Setelan** > **Aksesibilitas** > **SandFly** di setelan Android dan aktifkan pintasan untuk SandFly. Pintasan yang tersedia bergantung pada versi Android dan ponsel.

### Yang terjadi pada saat pertama kali {#getting-started-first-start}

Saat pertama kali SandFly diaktifkan, layar berikut terbuka secara berurutan:

1. Dialog **Pilih gestur**, di perangkat dengan layar sentuh. Lihat [](#getting-started-gesture-layout).
2. Tutorial SandFly. Lihat [](#getting-started-tutorial).

Jika Anda sebelumnya menggunakan versi SandFly yang lebih lama, SandFly akan membuka halaman tentang fitur baru dari pembaruan tersebut, bukan tutorial. Lihat [](#getting-started-new-features).

Jika Backtalk Aaron terinstal, SandFly terlebih dahulu menyalin setelan dan label khususnya. Lihat [](#getting-started-backtalk).

#### Memilih tata letak gestur {#getting-started-gesture-layout}

Dialog **Pilih gestur** memiliki dua tombol:

- **Gestur SandFly**: gestur default SandFly. Geseran dua jari diteruskan ke aplikasi untuk men-scroll.
- **Gestur VoiceOver**: gestur yang menyerupai VoiceOver di iPhone. Geseran dua jari menjalankan perintah SandFly, seperti membaca dari bagian atas layar.

Menutup dialog tanpa memilih akan memilih Gestur SandFly. Jika Anda meninggalkan dialog dengan Layar utama, dialog akan terbuka lagi saat SandFly dimulai berikutnya. Tutorial terbuka setelah pilihan dibuat dan mengajarkan gestur dari tata letak yang Anda pilih.

Untuk mengubah tata letak nanti, buka **Gestur** > **Tata letak gestur**. Lihat [](#gestures).

#### Tutorial SandFly {#getting-started-tutorial}

Tutorial SandFly mengajarkan gestur dasar melalui pelajaran yang Anda praktikkan langsung di layar. Tutorial ini terbuka saat pertama kali SandFly diaktifkan. Untuk membukanya lagi nanti, buka **Bantuan & masukan** > **Tutorial SandFly**, atau gunakan perintah **Buka tutorial**.

Tutorial ini memiliki 16 pelajaran dalam lima bab:

- **Dasar-dasar**: Selamat datang, Jelajahi dengan sentuhan, Berpindah antar-item, Mengaktifkan item, Menjeda ucapan.
- **Membaca**: Kontrol pembacaan, Judul, Membaca terus-menerus.
- **Kontrol dan teks**: Men-scroll daftar, Penggeser, Tindakan, Mengetik.
- **Berpindah-pindah**: Kembali, Layar utama, dan notifikasi; Menu SandFly; Perintah yang berguna.
- **Latihan**: Latihan gestur.

Setiap pelajaran memberikan instruksi, menunggu hingga Anda melakukannya dengan gestur Anda sendiri, lalu memberikan instruksi berikutnya. Instruksi adalah item pertama setelah nama pelajaran. SandFly mengucapkan setiap instruksi, termasuk saat audio lain sedang diputar, dan mengulanginya setelah 25 detik tanpa gestur atau sentuhan, hingga tiga kali. Instruksi menyebutkan gestur yang ditetapkan untuk setiap tindakan di setelan gestur Anda. Jika suatu tindakan tidak memiliki gestur, langkah tersebut akan menyatakannya, dan **Lewati langkah** melanjutkan tanpa langkah itu.

Saat suatu pelajaran selesai, SandFly menyebutkan gestur mana yang memulai pelajaran berikutnya dan memindahkan fokus ke **Pelajaran berikutnya**, diikuti nama pelajaran tersebut. Pelajaran terakhir memiliki **Selesaikan tutorial**.

Di dalam pelajaran, gestur yang biasanya keluar dari tutorial, seperti Kembali, Layar utama, aplikasi terbaru, dan notifikasi, tidak keluar dari tutorial. SandFly mengucapkan apa yang seharusnya dilakukan gestur tersebut. Menonaktifkan ucapan dan menjeda SandFly bekerja dengan cara yang sama. Kembali tetap menutup keyboard dan menu. Untuk memilih pelajaran lain atau keluar dari tutorial, gunakan **Daftar isi**, yang mencantumkan semua pelajaran, menunjukkan mana yang sudah selesai, dan membuka pelajaran mana pun. **Tutup tutorial** di dalam **Daftar isi** menutup tutorial.

Tutorial terbuka kembali pada pelajaran yang sedang Anda ikuti, atau pada pelajaran berikutnya jika pelajaran tersebut sudah selesai.

Di **Latihan gestur**, setiap gestur mengucapkan nama gestur dan perintah yang ditetapkan padanya, tanpa menjalankan perintah tersebut. Berpindah ke item berikutnya dan sebelumnya serta mengaktifkan tetap berfungsi agar Anda dapat mencapai **Akhiri latihan**. Untuk membuka pelajaran ini secara langsung, gunakan **Bantuan & masukan** > **Berlatih gestur**, atau **Berlatih gestur** di bagian atas layar **Gestur**.

Catatan: Tutorial hanya menerima gestur yang dikenali oleh SandFly. Jika **Kelola gestur di SandFly** nonaktif di **Setelan developer**, Android yang mengenali gestur, dan gestur navigasi Android dapat keluar dari tutorial. Pelajaran tentang penggeser dan tindakan memerlukan **Kontrol pembacaan** > **Ubah kontrol pembacaan secara otomatis**, yang aktif secara default.

#### Tutorial keyboard {#getting-started-keyboard-tutorial}

Jika keyboard fisik tersambung saat SandFly dimulai untuk pertama kali, SandFly membuka **Tutorial keyboard SandFly** sebagai gantinya. Tutorial ini mengajarkan pintasan keyboard dari peta tombol Anda dalam 15 pelajaran, termasuk **Daftar pintasan keyboard**, **Mode penjelajahan** untuk halaman web, dan **Latihan keyboard**. Saat perangkat memiliki layar sentuh sekaligus keyboard fisik, **Tutorial SandFly** membuka dialog berisi **Tutorial gestur sentuh** dan **Tutorial keyboard**. Lihat [](#keyboards).

Jam tangan dan TV memiliki tutorialnya sendiri. Lihat [](#devices).

#### Halaman fitur baru {#getting-started-new-features}

Setelah pembaruan yang menambahkan fitur, SandFly membuka halaman yang menjelaskan fitur-fitur tersebut pada saat berikutnya SandFly dimulai. Untuk membacanya lagi, pilih **Fitur-fitur baru di SandFly** di bagian atas layar utama setelan SandFly.

### Gestur dasar {#getting-started-basics}

Berikut adalah gestur dari tata letak SandFly. Tata letak VoiceOver menetapkan sebagian gestur secara berbeda. Untuk melihat semua gestur default, lihat [](#appendix-gestures). Untuk mengubah gestur, lihat [](#gestures).

- **Jelajahi dengan sentuhan**: Gerakkan satu jari di layar. SandFly membacakan item di bawah jari Anda.
- **Pindah ke item berikutnya atau sebelumnya**: Geser ke kanan atau geser ke kiri.
- **Aktifkan item**: Ketuk dua kali di mana saja di layar. Ketuk dua kali dan tahan untuk menyentuh lama item tersebut.
- **Scroll**: Geser ke atas atau ke bawah dengan dua jari. SandFly juga men-scroll daftar saat Anda menggeser melewati item terakhir di layar.
- **Hentikan ucapan**: Ketuk dengan dua jari. Ketuk dengan dua jari lagi untuk melanjutkan. Ketuk dua kali dengan tiga jari menonaktifkan dan mengaktifkan ucapan.
- **Kontrol pembacaan**: Geser ke bawah dengan tiga jari, atau putar dua jari searah jarum jam di layar, untuk memilih kontrol pembacaan berikutnya, seperti **Kata**, **Judul**, atau **Kecepatan bicara**. Geser ke atas dengan tiga jari, atau putar dua jari berlawanan arah jarum jam, untuk kontrol pembacaan sebelumnya. Lalu geser ke bawah atau ke atas untuk berpindah menurut item yang dipilih atau mengubah setelan yang dipilih. Lihat [](#reading).
- **Menu SandFly**: Ketuk dengan tiga jari, geser ke atas lalu ke kanan, atau geser ke bawah lalu ke kanan. Lihat [](#commands).
- **Kembali**: Geser ke bawah lalu ke kiri, atau usap bolak-balik dengan dua jari.
- **Layar utama**: Geser ke atas lalu ke kiri, atau geser ke atas dari tepi bawah.
- **Aplikasi terbaru**: Geser ke kiri lalu ke atas.
- **Notifikasi**: Geser ke kanan lalu ke bawah.
- **Telusuri layar**: Geser ke kiri lalu ke bawah.

Untuk cara SandFly membaca dan cara berpindah melalui teks, daftar, dan halaman web, lihat [](#reading) dan [](#web).

### Membuka setelan SandFly {#getting-started-settings}

Untuk membuka setelan SandFly, lakukan salah satu langkah berikut:

- Buka **SandFly** di daftar aplikasi perangkat. Ikon aplikasi membuka setelan baik saat SandFly aktif maupun nonaktif, sehingga Anda dapat mengubah setelan sebelum mengaktifkan SandFly.
- Pilih **Setelan SandFly** di menu SandFly.
- Di setelan Android, buka **Aksesibilitas** > **SandFly** > **Setelan**.

Layar setelan utama mencantumkan layar setelan berikut: Ucapan, Panjang respons, Suara dan getaran, Output audio, Sentuhan, Gestur, Pintasan keyboard, Tombol volume, Perintah, Kontrol pembacaan, Menu SandFly, Menu kustom, Makro, Perintah suara, Navigasi, Mengetik, Braille, Tampilan, Notifikasi, Terjemahan, Deskripsi gambar dan layar, Layanan AI, SandFly Pro, Skrip, Label khusus, Setelan cepat, Ringkasan, Jeda dan nonaktifkan, Profil setelan, Cadangkan dan pulihkan, Update dan penginstalan, Shizuku dan root, Bantuan & masukan, dan Setelan developer. Beberapa layar tidak ditampilkan di jam tangan dan TV, atau di perangkat yang tidak memiliki perangkat keras yang diperlukan fitur tersebut.

#### Menelusuri setelan {#getting-started-search}

**Telusuri**, di bagian atas layar setelan utama, mencari setelan dan perintah di setiap layar setelan.

1. Pilih **Telusuri**.
2. Ketik satu kata atau lebih, lalu cari.
3. Pilih salah satu hasil.

Setiap hasil memuat semua kata yang Anda ketik di judul atau deskripsinya, termasuk di dalam sebuah kata. Huruf besar/kecil dan aksen diabaikan. Setiap hasil menampilkan judulnya serta layar dan kategori yang mengarah ke sana, seperti **Ucapan** > **Menghentikan ucapan**. Memilih suatu setelan akan membuka layarnya dengan setelan tersebut difokuskan. Memilih suatu perintah akan membukanya di **Perintah**.

Layar **Gestur** dan **Pintasan keyboard** memiliki **Telusuri** sendiri, yang mencari gestur atau pintasan berdasarkan namanya atau perintah yang ditetapkan padanya.

### Panduan pengguna {#getting-started-user-guide}

Panduan ini dibuka dari **Bantuan & masukan** > **Panduan pengguna** di setelan SandFly. Panduan diawali daftar isi, dan tautan di dalam teks mengarah ke bagian yang disebutkannya. Panduan ditampilkan dalam bahasa SandFly. Bab yang belum diterjemahkan ke bahasa tersebut ditampilkan dalam bahasa Inggris.

### Pindah dari Backtalk Aaron {#getting-started-backtalk}

SandFly menggantikan Backtalk Aaron, yang tidak lagi menerima pembaruan. Build terakhir Backtalk Aaron menawarkan SandFly sebagai pembaruan, dengan notifikasi atau dialog **Backtalk Aaron sekarang menjadi SandFly**.

1. Di Backtalk Aaron, ketuk notifikasi pembaruan, atau pilih **Periksa pembaruan**, lalu **Instal**.
2. SandFly terinstal berdampingan dengan Backtalk Aaron, dan setelan aksesibilitas Android terbuka.
3. Aktifkan SandFly. SandFly menyalin setelan dan label khusus dari Backtalk Aaron, lalu Backtalk Aaron menonaktifkan dirinya sendiri.
4. Ketuk notifikasi **Setelan disalin dari Backtalk Aaron** untuk mencopot pemasangan Backtalk Aaron.

SandFly menerima rilis stabil. Untuk mendapatkan build pengembangan juga, aktifkan **Update dan penginstalan** > **Build pengembangan**. Lihat [](#updates-channels).

Model AI di perangkat tidak ikut disalin. Unduh kembali model tersebut di SandFly. Lihat [](#descriptions). Jika Anda menggunakan keyboard braille, aktifkan kembali untuk SandFly. Lihat [](#braille).

Jika Anda menggunakan pintasan aksesibilitas, berikan SandFly izin untuk mengubah setelan aman sebelum Anda mengaktifkannya. SandFly kemudian memindahkan pintasan aksesibilitas dan keyboard braille dari Backtalk Aaron ke dirinya sendiri. Dari komputer, jalankan:

```
adb shell pm grant io.github.aaron_gh.sandfly android.permission.WRITE_SECURE_SETTINGS
```

Tanpa izin ini, atur kembali pintasan untuk SandFly di setelan aksesibilitas Android.

Untuk menggunakan file setelan yang diekspor dari Backtalk, lihat [](#profiles).
