## Tampilan, ringkasan, dan setelan cepat {#display}

**Tampilan** di setelan SandFly mengatur indikator fokus, animasi layar, dan wallpaper SandFly. Bab ini juga membahas cara menyembunyikan layar, kecerahan layar, ringkasan aplikasi terbaru SandFly (**Ringkasan**), layar terpisah, dan panel setelan cepat SandFly (**Setelan cepat**).

### Mengubah indikator fokus {#display-focus-indicator}

Indikator fokus adalah bingkai yang digambar SandFly di sekeliling item yang memiliki fokus aksesibilitas. Setelannya berada di **Tampilan** > **Indikator fokus**, pada Android 12 dan versi lebih baru:

- **Tampilkan indikator fokus**: menggambar bingkai. Aktif secara default.
- **Batas tebal**: menggambar bingkai yang lebih lebar.
- **Warna**: warna bingkai: **Default**, **Merah**, **Oranye**, **Kuning**, **Hijau**, **Biru**, atau **Abu-abu**.

Perintah **Tampilkan indikator fokus**, di kelompok perintah **SandFly**, mengaktifkan atau menonaktifkan indikator fokus. Lihat [](#commands).

### Mematikan animasi {#display-animations}

**Tampilan** > **Matikan animasi** mematikan animasi jendela di seluruh ponsel, sehingga SandFly dapat membacakan layar baru lebih cepat. Setelan ini aktif secara default. SandFly mematikan animasi di Android 13 dan versi lebih baru, lalu mengembalikannya seperti semula saat Anda menonaktifkan setelan ini atau menonaktifkan SandFly.

### Menyembunyikan layar {#display-hide-screen}

Fitur sembunyikan layar membuat tampilan layar menjadi hitam, termasuk bilah navigasi, agar orang di sekitar Anda tidak dapat melihat isinya. SandFly dan aplikasi tetap berjalan seperti biasa.

Untuk menyembunyikan layar:

1. Buka Menu SandFly. Lihat [](#commands).
2. Pilih **Sembunyikan atau tampilkan layar**.
3. Di dialog **Sembunyikan layar?**, pilih **Oke**.

SandFly mengucapkan "Layar disembunyikan" dan memutar suara. Untuk menampilkan layar kembali, pilih **Sembunyikan atau tampilkan layar** di Menu SandFly lagi. SandFly mengucapkan "Menampilkan layar".

Anda juga dapat menyembunyikan dan menampilkan layar dengan:

- Perintah **Sembunyikan atau tampilkan layar**, di kelompok perintah **SandFly**, yang dapat Anda tetapkan ke gerakan atau pintasan keyboard. Lihat [](#commands).
- Kontrol pembacaan **Sembunyikan layar**, nonaktif secara default. Aktifkan di **Kontrol pembacaan**, lalu geser ke atas atau ke bawah untuk menyembunyikan atau menampilkan layar.

Agar dialog tidak muncul setiap kali, matikan **Selalu tampilkan peringatan ini** di dialog tersebut. SandFly kemudian hanya mengucapkan "Layar disembunyikan", tanpa petunjuk cara menampilkan layar kembali.

Dengan izin **Ubah setelan sistem**, SandFly juga menurunkan kecerahan layar ke tingkat minimum saat layar disembunyikan, dan mengembalikannya saat layar ditampilkan kembali. Saat pertama kali Anda menyembunyikan layar tanpa izin tersebut, SandFly membuka layar Android untuk mengizinkannya.

Catatan: Layar tetap tersembunyi setelah SandFly dimulai ulang. Fitur sembunyikan layar tidak tersedia saat ponsel terkunci. Sebelum Android 14, deskripsi gambar tidak tersedia saat layar disembunyikan.

Untuk mengubah suara saat menyembunyikan dan menampilkan layar, lihat [](#sound). Agar SandFly mengucapkan "Layar disembunyikan" saat layar menyala, lihat [](#verbosity).

### Mengubah kecerahan layar {#display-brightness}

Kontrol pembacaan **Kecerahan** mengubah kecerahan layar sistem, bahkan saat layar sedang disembunyikan. Kontrol ini aktif secara default di **Kontrol pembacaan**.

1. Geser ke atas atau ke bawah untuk memilih kontrol pembacaan hingga SandFly mengucapkan "Kecerahan". Lihat [](#reading).
2. Geser ke atas untuk membuat layar lebih terang, atau geser ke bawah untuk membuatnya lebih redup.

Setiap langkah mengubah kecerahan sebesar 10 persen dari penggeser kecerahan sistem, dan SandFly menyebutkan tingkat yang baru, misalnya "Kecerahan 40 persen." SandFly memerlukan izin **Ubah setelan sistem** untuk mengubah kecerahan, dan akan membuka layar Android untuk mengizinkannya pada kali pertama. Saat kecerahan adaptif aktif, SandFly mengucapkan "Nonaktifkan kecerahan adaptif untuk mengubah kecerahan."

### Menggunakan SandFly sebagai wallpaper {#display-wallpaper}

Beberapa ponsel, seperti ponsel Vivo, menghentikan SandFly di latar belakang meskipun sudah dikecualikan dari pengoptimalan baterai. Android menjaga wallpaper yang sedang aktif agar tetap berjalan, dan memulainya ulang jika berhenti. SandFly memiliki wallpaper animasi yang berjalan di dalam SandFly sendiri, sehingga di ponsel-ponsel tersebut SandFly tetap berjalan selama menjadi wallpaper.

1. Buka **Tampilan** > **Gunakan SandFly sebagai wallpaper**.
2. Pada layar wallpaper Android yang terbuka, tetapkan sebagai wallpaper.

Selama SandFly menjadi wallpaper, setelan tersebut menampilkan "SandFly adalah wallpaper". Wallpaper tersebut berwarna hitam. Untuk menampilkan gambar sebagai gantinya, buka **Tampilan** > **Gambar wallpaper** lalu pilih gambar. SandFly mengucapkan "Gambar wallpaper disetel", atau "Tidak dapat menggunakan gambar ini".

Setelan ini hanya tersedia di ponsel dan tablet.

### Ringkasan SandFly {#display-overview}

Ringkasan SandFly menampilkan daftar aplikasi terbaru dalam satu kolom atau kisi yang dibacakan SandFly secara berurutan. Ringkasan ini terbuka menggantikan ringkasan sistem saat Anda mengaktifkan **Ringkasan** > **Gunakan ringkasan SandFly**. Setelan ini nonaktif secara default.

Ringkasan SandFly memerlukan Shizuku atau root untuk membaca dan membuka aplikasi terbaru. Lihat [](#updates). Tanpa Shizuku, ringkasan sistem yang terbuka, dan SandFly mengucapkan "Shizuku tidak berjalan" atau "Shizuku tidak mengizinkan SandFly". Di layar kunci, ringkasan sistem yang terbuka.

Setelan lainnya di **Ringkasan** adalah:

- **Aplikasi terbaru di bagian bawah**: menempatkan aplikasi terbaru di bagian paling akhir, dan membuka daftar yang sudah digulir ke paling bawah. Nonaktif secara default.
- **Kolom**: 1 menampilkan daftar. 2, 3, atau 4 menampilkan kisi dengan jumlah kolom tersebut. Default-nya adalah 1.

Setelan ini tidak tersedia di TV.

#### Menggunakan ringkasan {#display-overview-use}

Saat **Gunakan ringkasan SandFly** aktif, perintah **Ringkasan**, di **Tindakan sistem**, membuka ringkasan SandFly alih-alih ringkasan sistem. Ringkasan SandFly juga terbuka di atas ringkasan sistem saat Anda membuka ringkasan sistem melalui gerakan navigasi atau tombol Aplikasi Terbaru.

Di dalam ringkasan:

- Ketuk dua kali sebuah aplikasi untuk beralih ke aplikasi tersebut.
- Buka menu Tindakan pada sebuah aplikasi untuk **Tutup**, **Layar terpisah**, dan **Info aplikasi**. **Layar terpisah** menampilkan aplikasi tersebut dalam layar terpisah di samping aplikasi asal saat ringkasan dibuka. **Info aplikasi** membuka halaman Info aplikasi Android dari aplikasi tersebut.
- Pada keyboard fisik, tekan Delete untuk menutup aplikasi yang difokuskan.
- **Tutup semua**, di kiri bawah, menutup semua aplikasi terbaru.
- **Ringkasan sistem**, di kanan bawah, membuka ringkasan sistem.

Jalankan perintah **Ringkasan** lagi saat ringkasan sedang terbuka untuk beralih ke aplikasi sebelumnya. Perintah **Ringkasan sistem** membuka ringkasan sistem meskipun **Gunakan ringkasan SandFly** aktif.

#### Beralih antar-aplikasi terbaru {#display-switch-apps}

Perintah berikut berada di **Tindakan sistem**, belum ditetapkan secara default. Lihat [](#commands).

- **Aplikasi sebelumnya**: beralih ke aplikasi yang digunakan sebelum aplikasi saat ini. SandFly mengucapkan "Tidak ada aplikasi sebelumnya" jika tidak ada. Tanpa Shizuku, SandFly membuka ringkasan sistem dan beralih ke aplikasi sebelumnya dari sana.
- **Aplikasi lebih lama**: membuka aplikasi berikutnya yang lebih lama di daftar aplikasi terbaru. Jalankan lagi untuk mundur lebih jauh. SandFly menyebutkan nama setiap aplikasi, dan "Tidak ada aplikasi lebih lama" di akhir daftar. Tanpa Shizuku, Aplikasi lebih lama berfungsi sama dengan Aplikasi sebelumnya.
- **Aplikasi lebih baru**: kembali menuju aplikasi paling baru setelah menggunakan **Aplikasi lebih lama**. SandFly mengucapkan "Tidak ada aplikasi lebih baru" saat sudah di aplikasi paling baru. Aplikasi lebih baru memerlukan Shizuku.

Saat **Gunakan ringkasan SandFly** aktif dan Shizuku berjalan, Alt+Tab pada keyboard fisik dapat beralih aplikasi tanpa membuka jendela:

1. Tahan Alt lalu tekan Tab. SandFly menyebutkan nama aplikasi sebelum aplikasi saat ini.
2. Tekan Tab lagi untuk berpindah ke aplikasi yang lebih lama, atau Shift+Tab untuk mundur kembali.
3. Lepaskan Alt untuk beralih ke aplikasi yang disebutkan SandFly. Untuk tetap di aplikasi saat ini, tekan Escape sebelum Anda melepaskan Alt.

Alt+Tab tidak tersedia jika Alt ditetapkan sebagai tombol SandFly. Lihat [](#keyboards).

#### Layar terpisah {#display-split-screen}

Untuk membuka dua aplikasi dalam layar terpisah, gunakan **Layar terpisah** di menu Tindakan milik aplikasi pada ringkasan SandFly. Lihat [](#display-overview-use).

Saat layar terpisah ditampilkan, Menu SandFly memiliki item **Layar terpisah**, berisi **Pindah ke pemisah** serta tindakan pemisah seperti ukurannya dan menukar posisi aplikasi. Perintah **Layar terpisah**, di **Tindakan sistem**, membuka menu yang sama, dan mengucapkan "Tidak dalam layar terpisah" saat layar terpisah tidak aktif. Dalam layar terpisah, tombol Kembali dan tombol yang diketikkan dikirim ke aplikasi tempat terakhir kali Anda memindahkan fokus.

### Panel setelan cepat SandFly {#display-quick-settings}

Panel setelan cepat SandFly adalah kisi berisi sakelar, penggeser, dan tombol yang dapat Anda pilih dan atur susunannya. Panel ini terbuka menggantikan setelan cepat sistem saat Anda mengaktifkan **Setelan cepat** > **Gunakan setelan cepat SandFly**. Setelan ini nonaktif secara default.

#### Menggunakan panel setelan cepat {#display-quick-settings-use}

Perintah **Setelan Cepat**, di **Tindakan sistem**, membuka panel ini. Tombol **Setelan cepat** di daftar notifikasi SandFly juga membukanya. Lihat [](#notifications). Di layar kunci, setelan cepat sistem yang terbuka sebagai gantinya.

Di dalam panel:

- SandFly menyebutkan baris dan kolom setiap item, namanya, dan statusnya, seperti jaringan Wi-Fi yang terhubung.
- Ketuk dua kali sakelar untuk mengaktifkan atau menonaktifkannya. Ketuk dua kali item dengan beberapa pilihan nilai, seperti **Mode suara** atau **Batas waktu layar**, untuk berpindah ke nilai berikutnya.
- Geser ke atas atau ke bawah pada penggeser, seperti **Kecerahan** atau **Volume media**, untuk menggerakkannya satu langkah.
- Ketuk dua kali lalu tahan sebuah item, atau gunakan tindakan **Buka setelan**, untuk membuka setelan Android yang terkait.
- **Edit** membuka layar untuk memilih item. Lihat [](#display-quick-settings-edit).
- **Setelan cepat sistem** membuka setelan cepat sistem.

Perintah **Setelan cepat sistem** membuka setelan cepat sistem meskipun **Gunakan setelan cepat SandFly** aktif.

Android hanya mengizinkan sistem dan shell mengubah beberapa setelan tertentu. Saat Shizuku berjalan, SandFly dapat mengaktifkan dan menonaktifkan sendiri Wi-Fi, Bluetooth, data seluler, hotspot, mode pesawat, lokasi, NFC, penghemat baterai, tema gelap, penghemat data, serta akses mikrofon dan kamera, dan memberi dirinya sendiri izin yang diperlukan untuk kontrol lainnya. Tanpa Shizuku, sakelar-sakelar ini akan membuka halaman setelan Android terkait. Lihat [](#updates).

SandFly mengaktifkan kotak aplikasi dan kotak sistem di setelan cepat sistem: SandFly membuka setelan cepat sistem, mengaktifkan kotak tersebut, menutupnya tanpa membacakannya, lalu menyebutkan status baru kotak itu. Tanpa Shizuku, kotak hanya berfungsi jika ada di setelan cepat sistem, dan SandFly mengucapkan "<nama> tidak ada dalam setelan cepat sistem" jika tidak dapat menemukan kotaknya. Saat Shizuku berjalan, SandFly terlebih dahulu menambahkan kotak aplikasi ke setelan cepat sistem jika belum ada di sana, dan mengaktifkannya melalui Shizuku jika kotak tersebut tidak ditemukan di sana.

#### Memilih item yang ditampilkan di panel {#display-quick-settings-edit}

Buka **Setelan cepat** > **Panel setelan cepat**, atau pilih **Edit** di dalam panel. Layar ini mencantumkan item-item di panel, masing-masing beserta baris dan kolomnya.

- **Tambahkan kontrol**: mencantumkan kontrol SandFly yang belum ada di panel: Wi-Fi, Bluetooth, data seluler, hotspot, mode pesawat, penghemat data, Jangan Ganggu, modus suara, senter, kecerahan senter, lokasi, putar otomatis, penghemat baterai, tema gelap, Cahaya Malam, Ekstra redup, inversi warna, koreksi warna, NFC, akses mikrofon, akses kamera, kecerahan, kecerahan adaptif, batas waktu layar, ukuran font, ukuran tampilan, media, output media, volume media, volume panggilan, volume dering, volume notifikasi, volume alarm, volume aksesibilitas, volume sistem, alarm berikutnya, baterai, Setelan, dan menu daya. Kontrol yang tidak dimiliki ponsel tidak dicantumkan.
- **Tambahkan kotak aplikasi**: mencantumkan kotak setelan cepat yang disediakan aplikasi terinstal.
- **Tambahkan kotak sistem**: mencantumkan kotak dari setelan cepat sistem. Kotak sistem yang sudah memiliki kontrol SandFly, seperti Hotspot, ditambahkan sebagai kontrol tersebut. Jika Shizuku berjalan, SandFly memberi dirinya sendiri akses DUMP dan akses penggunaan, lalu membaca daftarnya dari sistem. Jika tidak, SandFly mencarinya di setelan cepat sistem, yang memerlukan beberapa detik.
- **Jumlah kolom**: jumlah kolom dalam kisi, dari 1 hingga 4. Default-nya adalah 2.

Untuk memindahkan atau menghapus item, ketuk dua kali item tersebut, atau gunakan tindakannya:

- **Pindahkan**: mencantumkan sel-sel kisi. Item akan menempati sel yang Anda pilih, dan item di antaranya bergeser satu sel untuk memberi ruang. SandFly menyebutkan berapa banyak item yang berpindah.
- **Hapus**: menghapus item dari panel.

Secara default, panel berisi Wi-Fi, Bluetooth, data seluler, mode pesawat, Jangan Ganggu, modus suara, senter, lokasi, putar otomatis, penghemat baterai, kecerahan, volume media, volume dering, volume aksesibilitas, media, dan baterai.
