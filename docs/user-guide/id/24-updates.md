## Pembaruan, Shizuku, dan root {#updates}

SandFly memperbarui dirinya sendiri dari rilis SandFly di GitHub. Setelan untuk pembaruan ada di **Update dan penginstalan**.

Beberapa fitur memerlukan izin yang hanya diberikan Android melalui adb (Android Debug Bridge). SandFly dapat memperolehnya melalui Shizuku, atau melalui root pada perangkat yang di-root. Setelan untuk root ada di **Shizuku dan root**.

### Rilis stabil dan build pengembangan {#updates-channels}

SandFly diterbitkan dalam dua jenis build:

- Rilis stabil, dinamai berdasarkan versi, seperti SandFly 0.6.0. Rilis ini tersedia di [halaman rilis SandFly](https://github.com/aaron-gh/SandFly/releases).
- Build pengembangan, dibuat dari setiap perubahan pada SandFly. Build ini mendapatkan perubahan lebih cepat daripada rilis stabil, tetapi pengujiannya lebih sedikit.

Setiap build memiliki nomor build yang meningkat di setiap perubahan. **Update dan penginstalan** > **Periksa pembaruan** menampilkan versi dan nomor build SandFly yang terinstal, seperti "Versi 0.6.1, build 1234".

Secara default, SandFly hanya menawarkan rilis stabil. Untuk mendapatkan build pengembangan juga, aktifkan **Update dan penginstalan** > **Build pengembangan**. SandFly kemudian akan menawarkan build mana pun yang lebih baru, baik build pengembangan maupun rilis stabil, dan memeriksa pembaruan sekitar sekali setiap jam alih-alih sekali sehari.

### Memeriksa pembaruan {#updates-check}

1. Buka **Update dan penginstalan**.
2. Pilih **Periksa pembaruan**.

SandFly menampilkan "Memeriksa pembaruan", lalu salah satu dari berikut ini:

- "SandFly sudah versi terbaru".
- Dialog **SandFly build** *nomor* **tersedia**, berisi perubahan di build baru. Untuk rilis stabil, perubahannya berupa catatan rilis. Untuk build pengembangan, berupa perubahan sejak build yang terinstal. Pilih **Instal** untuk menginstalnya, atau **Batal**.
- "Tidak dapat memeriksa pembaruan", saat GitHub tidak dapat dijangkau. Coba lagi nanti.

Saat build pengembangan baru sedang dipublikasikan, pemeriksaan menunggunya dan mencoba lagi, hingga satu menit.

### Pemeriksaan pembaruan otomatis {#updates-automatic}

**Update dan penginstalan** > **Periksa pembaruan secara otomatis** aktif secara default. Saat SandFly menyala, SandFly memeriksa build baru 30 detik setelah dimulai, lalu sekali sehari, atau sekali setiap jam jika **Build pengembangan** aktif. Jika build baru tersedia, SandFly menampilkan notifikasi **Pembaruan SandFly tersedia** beserta daftar perubahannya. Setiap build diumumkan satu kali.

Notifikasi ini memerlukan izin agar SandFly dapat menampilkan notifikasi. Jika notifikasi dimatikan untuk SandFly, build akan diumumkan saat notifikasi diaktifkan kembali.

Nonaktifkan **Periksa pembaruan secara otomatis** untuk hanya memeriksa dengan **Periksa pembaruan**.

### Menginstal pembaruan {#updates-install}

1. Ketuk notifikasi **Pembaruan SandFly tersedia**, atau pilih **Instal** pada dialog **Periksa pembaruan**.
2. Pada kali pertama, Android membuka setelan yang mengizinkan SandFly menginstal aplikasi, dan SandFly menampilkan "Izinkan SandFly menginstal aplikasi, lalu coba perbarui lagi". Aktifkan setelan tersebut, kembali, dan ulangi langkah 1.
3. SandFly menampilkan "Mengunduh pembaruan" dan progres unduhan di notifikasi: persentase, serta sisa waktu setelah beberapa detik, seperti "40%, 1 menit 20 detik tersisa".
4. Setelah unduhan selesai, dialog penginstalan Android terbuka. Pilih **Perbarui** atau **Instal**.

SandFly akan dimulai ulang dengan build baru dan setelan yang sama. Jika dialog penginstalan tidak terbuka, misalnya karena layar mati saat mengunduh, ketuk notifikasi **Pembaruan SandFly telah diunduh**. Notifikasi ini hilang setelah 10 menit.

SandFly hanya menginstal build SandFly yang ditandatangani dengan kunci SandFly. Jika file yang diunduh bukan build resmi, SandFly menampilkan "Unduhan bukan build dari aplikasi ini, sehingga tidak diinstal". Jika koneksi terputus, SandFly mencoba ulang hingga lima kali dan melanjutkan unduhan dari titik berhenti. Jika unduhan tetap gagal, SandFly menampilkan "Tidak dapat mengunduh pembaruan". Menginstal ulang build yang sama sebelum SandFly dimulai ulang akan melanjutkan unduhan tersebut.

Build SandFly yang Anda kompilasi sendiri ditandatangani dengan kunci yang berbeda. Android tidak dapat menginstal rilis SandFly di atas build tersebut, begitu pula sebaliknya. Hapus instalan salah satunya sebelum menginstal yang lain.

Di jam tangan, lihat [](#devices-watch-updates).

### Tampak terinstal dari Google Play {#updates-play-installer}

Beberapa aplikasi, seperti sebagian aplikasi perbankan, menonaktifkan layanan aksesibilitas yang dicatat Android sebagai terinstal dari sumber selain Google Play. **Update dan penginstalan** > **Tampak terinstal dari Google Play** membuat Android mencatat Google Play sebagai penginstal SandFly, agar aplikasi-aplikasi tersebut tetap membiarkan SandFly aktif. Setelan ini nonaktif secara default dan memerlukan Shizuku atau root.

Saat Anda mengaktifkan setelan ini, SandFly menginstal ulang APK-nya sendiri melalui Shizuku dengan menetapkan Google Play sebagai penginstalnya. SandFly akan dimulai ulang selama proses instalasi, sehingga ucapan berhenti sejenak. Aplikasi dan setelannya tetap sama. Hanya informasi penginstal yang dilaporkan ke aplikasi lain yang berubah. Namun, aplikasi tetap bisa mengetahui dengan cara lain bahwa SandFly tidak diinstal dari Google Play.

Selama setelan ini aktif:

- SandFly menginstal pembaruan melalui Shizuku agar Google Play tetap tercatat sebagai penginstal. Jika gagal, SandFly menginstal pembaruan menggunakan dialog penginstalan Android.
- Jika ada sesuatu yang mencatat penginstal lain, misalnya pembaruan yang diinstal saat Shizuku tidak berjalan, SandFly mencatat ulang Google Play saat Shizuku kembali mengizinkan SandFly. Sampai saat itu, setelan menampilkan "Aktif, tetapi Google Play belum tercatat sebagai penginstal. SandFly akan mengaturnya saat Shizuku tersedia."

Menonaktifkan setelan ini mengembalikan catatan penginstal ke nilai sebelumnya.

Tanpa Shizuku, setelan tidak dapat mengubah penginstal. Tindakan **Salin perintah** pada setelan ini menyalin perintah adb yang melakukan hal yang sama dari komputer. Jalankan perintah itu di folder yang berisi `sandfly.apk`:

```
adb install -r -i com.android.vending sandfly.apk
```

Untuk mengembalikan catatan penginstal sebagai instalasi biasa, jalankan `adb install -r sandfly.apk`.

### Shizuku {#updates-shizuku}

[Shizuku](https://shizuku.rikka.app) adalah aplikasi yang menjalankan perintah untuk aplikasi lain dengan izin adb. Saat Shizuku berjalan dan mengizinkan SandFly, SandFly dapat memberikan izin kepada dirinya sendiri yang biasanya hanya diberikan Android melalui adb, serta menjalankan perintah yang hanya dapat dijalankan adb. SandFly memerlukan Shizuku versi 11 atau lebih baru.

#### Menyiapkan Shizuku {#updates-shizuku-setup}

1. Instal Shizuku dan jalankan, sesuai petunjuk di aplikasi Shizuku. Pada Android 11 dan lebih baru, Shizuku dapat dijalankan dengan debugging nirkabel tanpa komputer. Pada perangkat yang di-root, Shizuku juga dapat dijalankan dengan root.
2. Aktifkan SandFly, atau nonaktifkan lalu aktifkan kembali jika sudah aktif.
3. Saat SandFly dimulai ketika Shizuku sedang berjalan, Shizuku menampilkan dialog untuk mengizinkan SandFly. Pilih izinkan.

SandFly hanya meminta izin ke Shizuku satu kali secara otomatis. Setelah itu, dialog Shizuku terbuka saat Anda mengaktifkan fitur yang memerlukan Shizuku. Jika Anda sempat menolak SandFly dan memilih untuk tidak ditanya lagi, izinkan SandFly melalui aplikasi Shizuku.

Shizuku yang dijalankan lewat debugging nirkabel atau adb akan berhenti saat perangkat dimulai ulang. Jalankan kembali di aplikasi Shizuku.

#### Fitur yang menggunakan Shizuku atau root {#updates-shizuku-features}

Tanpa Shizuku atau root, fitur-fitur berikut tidak tersedia atau bekerja secara terbatas, sebagaimana dijelaskan di bab masing-masing:

- **Sentuhan** > **Perbaiki eksplorasi sentuhan yang macet**, aktif secara default, memerlukan izin DUMP untuk memperbaiki eksplorasi sentuhan ketika Android membiarkan sentuhan tertahan. Tanpa izin ini, SandFly hanya memberi tahu bahwa eksplorasi sentuhan mungkin macet. Lihat [](#touch).
- **Output audio** > **Speaker saat jauh dari telinga** memerlukan izin untuk mengelola panggilan. Memerlukan Android 12 atau lebih baru. Lihat [](#sound).
- **Output audio** > **Setel ulang audio aksesibilitas setelah panggilan** hanya bekerja melalui Shizuku atau root. Memerlukan Android 13 atau lebih baru. Lihat [](#sound).
- **Tombol volume** > **Pintasan tombol volume saat layar mati** hanya bekerja melalui Shizuku, bukan root. Lihat [](#buttons).
- **Ringkasan** > **Gunakan ringkasan SandFly** membaca dan membuka aplikasi terbaru melalui Shizuku atau root. Tanpa keduanya, ringkasan sistem yang akan terbuka. Alt+Tab, membuka aplikasi di layar terpisah, serta perintah **Aplikasi lebih lama** dan **Aplikasi lebih baru** juga memerlukan Shizuku atau root. Lihat [](#display).
- Sakelar pada panel setelan cepat SandFly, seperti Wi-Fi, Bluetooth, data seluler, dan mode pesawat, serta kontrol kecerahan, putar otomatis, cahaya malam, redup ekstra, warna, dan ukuran tampilan. Tanpa Shizuku atau root, beberapa sakelar akan membuka setelan sistem. Lihat [](#display).
- Riwayat papan klip hanya dapat menambahkan teks yang disalin di aplikasi lain melalui Shizuku atau root, dengan **Tambahkan salinan dari aplikasi lain** di riwayat. Lihat [](#typing).
- Gboard memutar umpan balik tombolnya sendiri, seperti **Bergetar saat tombol ditekan**, saat Anda menjelajahi tombolnya. Fitur ini hanya bekerja melalui Shizuku atau root. Lihat [](#typing).
- Perintah yang menjalankan pintasan aksesibilitas milik layanan aksesibilitas lain. Lihat [](#commands).
- Perintah **Kotak aplikasi** dan **Kotak sistem**, serta **Tambahkan kotak sistem** di setelan panel setelan cepat. Tanpa Shizuku atau root, kotak hanya berfungsi jika ada di setelan cepat sistem, dan **Tambahkan kotak sistem** mencari kotak di setelan cepat sistem. Lihat [](#commands-categories) dan [](#display-quick-settings).
- Pintasan aplikasi yang ditambahkan aplikasi saat berjalan, dan pintasan yang hanya boleh dibuka oleh aplikasi layar utama, hanya dicantumkan melalui Shizuku atau root. Lihat [](#gestures-app-shortcuts).
- Skrip dan makro yang menekan tombol atau mengetik teks saat tidak ada kolom teks, serta langkah makro yang mengubah kontrol setelan cepat di atas. Lihat [](#scripts) dan [](#macros).
- **Update dan penginstalan** > **Tampak terinstal dari Google Play**. Lihat [](#updates-play-installer).

Saat SandFly dimulai dengan Shizuku atau root diizinkan, SandFly memberikan kepada dirinya sendiri izin yang dibutuhkan oleh fitur-fitur yang telah Anda aktifkan.

#### Memberikan izin dengan adb {#updates-adb}

Tanpa Shizuku, setelan yang memerlukan izin adb menampilkan perintah adb di deskripsinya, dan tindakan **Salin perintah** menyalin perintah tersebut ke papan klip. Jalankan perintah dari komputer dengan adb, saat perangkat terhubung dan debugging USB atau debugging nirkabel aktif, lalu kembali ke setelan tersebut.

Perintah-perintah tersebut adalah:

- Untuk **Perbaiki eksplorasi sentuhan yang macet**:

  ```
  adb shell pm grant io.github.aaron_gh.sandfly android.permission.DUMP
  ```

- Untuk **Speaker saat jauh dari telinga**:

  ```
  adb shell appops set io.github.aaron_gh.sandfly MANAGE_ONGOING_CALLS allow
  ```

- Untuk memindahkan pintasan aksesibilitas dari Backtalk Aaron. Lihat [](#getting-started-backtalk).

  ```
  adb shell pm grant io.github.aaron_gh.sandfly android.permission.WRITE_SECURE_SETTINGS
  ```

**Setel ulang audio aksesibilitas setelah panggilan** tidak memiliki izin yang dapat diberikan lewat adb. Tanpa Shizuku, setelan ini menampilkan perintah adb yang menyetel ulang output audio aksesibilitas sekali saja, untuk saat ucapan terdengar dari lubang suara ponsel setelah panggilan. Perintah tersebut menyebut APK yang terinstal, jadi salin perintah itu lagi setelah SandFly diperbarui.

### Menggunakan root sebagai pengganti Shizuku {#updates-root}

Pada perangkat yang di-root, SandFly dapat menjalankan perintahnya dengan root alih-alih Shizuku. Aktifkan **Shizuku dan root** > **Gunakan root sebagai pengganti Shizuku**. Setelan ini nonaktif secara default.

SandFly kemudian menjalankan perintah melalui `su` sebagai pengguna shell, yaitu pengguna yang dipakai adb dan Shizuku. Pengelola root di perangkat, seperti Magisk atau KernelSU, menampilkan dialog untuk memberikan akses root ke SandFly. Fitur yang memerlukan Shizuku akan menggunakan root saat setelan ini aktif, kecuali **Pintasan tombol volume saat layar mati**, yang tidak ditampilkan dalam mode root. Jika `su` tidak memberikan akses root, ringkasan dan layar terpisah akan mengucapkan "Tidak ada akses root".

Catatan: Jika `su` pada perangkat tidak dapat menjalankan perintah sebagai pengguna shell, SandFly menjalankannya sebagai root. Menambahkan salinan dari aplikasi lain ke riwayat papan klip dan sakelar hotspot tidak akan berfungsi dalam kondisi tersebut.
