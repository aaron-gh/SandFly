## Sentuhan {#touch}

**Sentuhan** di setelan SandFly mengatur cara sentuhan di layar memfokuskan dan mengaktifkan item, kapan sentuhan dikunci, serta aplikasi mana yang menerima sentuhan secara langsung. Gerakan dijelaskan di [](#gestures), dan membaca layar dengan sentuhan di [](#reading).

### Memfokuskan dan mengaktifkan item {#touch-activation}

Secara default, menyentuh suatu item akan memfokuskannya dan SandFly membacakannya, lalu ketukan dua kali di mana saja di layar akan mengaktifkan item yang difokuskan. Setelan di bagian ini mengubah perilaku tersebut.

#### Penundaan fokus {#touch-focus-delay}

**Sentuhan** > **Penundaan fokus** mengatur berapa lama jari diam di atas suatu item sebelum SandFly memfokuskan dan membacakannya, dari 0,15 hingga 2 detik. Default-nya adalah 0,15 detik. Penundaan yang lebih singkat membuat SandFly berbicara lebih cepat setelah Anda menyentuh layar. Memerlukan Android 13 atau yang lebih baru.

**Penundaan fokus** juga merupakan kontrol pembacaan, yang nonaktif secara default. Lihat [](#commands).

#### Satu ketukan untuk mengaktifkan {#touch-single-tap}

Dengan **Sentuhan** > **Satu ketukan untuk mengaktifkan** aktif, satu ketukan pada item yang difokuskan langsung mengaktifkannya tanpa perlu ketuk dua kali. Setelan ini nonaktif secara default. Fitur ini memerlukan jelajahi dengan sentuhan, dan tidak tersedia di TV.

#### Angkat untuk mengaktifkan {#touch-lift-to-activate}

Dengan angkat untuk mengaktifkan, mengangkat jari setelah menyentuh item akan mengaktifkannya, seperti saat mengangkat jari mengetik tombol pada keyboard di layar. **Sentuhan** > **Angkat untuk mengaktifkan** memiliki tiga pilihan:

- **Nonaktif**: setelan default.
- **Hanya di bilah navigasi**: hanya tombol Kembali, Layar utama, dan Ringkasan.
- **Seluruh layar**: setiap item.

**Angkat untuk mengaktifkan** juga merupakan kontrol pembacaan, yang nonaktif secara default, sehingga Anda dapat mengubahnya dengan geseran ke atas atau ke bawah. Lihat [](#commands).

#### Membatalkan aktivasi angkat {#touch-lift-cancel}

Saat **Angkat untuk mengaktifkan** aktif, ada dua cara mengangkat jari tanpa mengaktifkan item:

- Geser jari keluar ke tepi bawah layar. Mengangkat jari di area 3,5 mm paling bawah layar tidak mengaktifkan apa pun, termasuk tombol keyboard yang mengetik saat jari diangkat. SandFly memutar suara dan getaran saat jari mencapai tepi bawah.
- Tahan jari pada item. **Sentuhan** > **Tahan untuk membatalkan** mengatur durasinya: **1 detik**, **2 detik**, **3 detik**, atau **5 detik**. Setelah waktu tersebut pada satu item, mengangkat jari tidak mengaktifkan apa pun, dan SandFly memutar suara serta getaran yang sama. Default-nya adalah **Nonaktif**. Tombol keyboard yang mengetik saat jari diangkat tetap mempertahankan fungsi sentuh lamanya sendiri.

#### Ketuk untuk mengaktifkan {#touch-tap-to-activate}

Dengan **Sentuhan** > **Ketuk untuk mengaktifkan** aktif, ketukan cepat yang diangkat sebelum item di bawah jari sempat difokuskan akan langsung mengaktifkan item tersebut. Jika fokus baru berpindah dalam 3 detik terakhir, ketukan akan menunggu ketukan kedua terlebih dahulu, sehingga ketuk dua kali tetap mengaktifkan item yang difokuskan. Setelan ini nonaktif secara default.

#### Tombol bilah navigasi {#touch-navigation-bar}

Dengan **Sentuhan** > **Ketuk untuk mengaktifkan tombol bilah navigasi** aktif, sentuhan di bilah navigasi langsung diteruskan ke Android: satu ketukan menekan tombol, dan menahan Layar utama akan menahannya. Setelan ini nonaktif secara default dan memerlukan Android 11 atau yang lebih baru. Saat aktif, **Angkat untuk mengaktifkan** tidak berlaku pada bilah navigasi.

SandFly tidak membacakan tombol saat Anda menyentuhnya, tetapi mengucapkan tombol yang ditekan oleh ketukan, seperti "Kembali" atau "Layar utama". Untuk menonaktifkannya, nonaktifkan **Sentuhan** > **Ucapkan tombol bilah navigasi**, yang aktif secara default.

#### Aktifkan dengan ketukan {#touch-activate-with-tap}

Beberapa kontrol mengabaikan aktivasi yang dikirim SandFly dan hanya bereaksi terhadap jari di layar. Perintah **Aktifkan dengan ketukan**, yang tidak ditetapkan secara default, mengetuk bagian tengah item yang difokuskan sebagai gantinya. Tetapkan perintah ini ke gerakan atau pintasan keyboard. Lihat [](#gestures). Skrip juga dapat melakukan hal yang sama untuk item pilihan setiap kali item itu diaktifkan. Lihat [](#scripts). Keduanya memerlukan Android 11 atau yang lebih baru. Di versi yang lebih lama, item diaktifkan seperti biasa.

Untuk aplikasi yang tidak bereaksi saat item diaktifkan, aktifkan aplikasi tersebut di **Sentuhan** > **Ketuk dua kali mengetuk layar**, atau jalankan perintah **Ketuk dua kali mengetuk layar di aplikasi ini**, yang tidak ditetapkan secara default, di aplikasi tersebut. Mengaktifkan item di aplikasi itu kemudian mengetuk item tersebut. Perintah ini mengucapkan "Ketuk dua kali mengetuk layar aktif" atau "Ketuk dua kali mengetuk layar nonaktif", atau "Tidak ada aplikasi di depan" jika tidak ada aplikasi di depan. Fitur ini juga memerlukan Android 11 atau yang lebih baru, dan tidak tersedia di jam tangan atau TV.

#### Ucapan saat Anda menyentuh layar {#touch-speech}

Secara default, menyentuh layar menghentikan ucapan. Agar ucapan tetap berlanjut hingga sentuhan memfokuskan suatu item, nonaktifkan **Ucapan** > **Hentikan ucapan saat menyentuh layar**. Lihat [](#speech).

### Mengunci sentuhan {#touch-lock}

Saat sentuhan dikunci, sentuhan dan gerakan di layar tidak melakukan apa pun, dan tidak ada yang diteruskan ke aplikasi. Tombol fisik, perintah tombol volume, gerakan sidik jari, ketuk belakang, dan goyangkan tetap berfungsi. Perintah yang menyentuh layar, seperti **Aktifkan dengan ketukan**, **Tahan sentuhan pada item yang difokuskan**, dan sentuhan di makro, tetap diteruskan ke aplikasi. Kunci sentuhan memerlukan Android 13 atau yang lebih baru serta **Setelan developer** > **Kelola gerakan di SandFly**.

Untuk mengunci atau membuka kunci sentuhan, gunakan perintah **Kunci atau buka kunci sentuhan**. SandFly mengucapkan "Sentuhan dikunci" atau "Kunci sentuhan dibuka". Tetapkan perintah ini ke penekanan tombol volume, pintasan keyboard, atau ketuk belakang, agar Anda dapat membuka kunci sentuhan tanpa menggunakan layar. Lihat [](#buttons) dan [](#gestures-back-tap). Menekan kedua tombol volume bersamaan selalu membuka kunci sentuhan.

**Sentuhan** > **Kunci sentuhan secara otomatis** mengunci sentuhan tanpa menjalankan perintah:

- **Tidak pernah**: setelan default.
- **Saat layar menyala**: setiap kali layar menyala. Buka kunci dengan perintah atau kedua tombol volume.
- **Di layar kunci**: hingga Anda membuka kunci ponsel.
- **Saat sensor jarak tertutup**: saat layar menyala dan sesuatu menutupi sensor jarak, seperti telinga atau saku.

SandFly tidak mengucapkan apa pun saat sentuhan dikunci secara otomatis.

**Sentuhan** > **Sentuhan saat sentuhan dikunci** mengatur respons ketika layar yang terkunci disentuh: **Tidak ada** (default), **Suara**, atau **Getaran**.

Skrip juga dapat mengunci sentuhan hingga 10 detik. Lihat [](#scripts).

### Sentuhan langsung {#touch-direct-touch}

Beberapa aplikasi, seperti gim audio, perlu menerima sentuhan persis seperti yang Anda lakukan, tetapi jelajahi dengan sentuhan menangkap ketukan dan geseran lebih dulu. Dengan sentuhan langsung, SandFly meneruskan sentuhan langsung ke aplikasi yang Anda pilih, sementara SandFly tetap aktif dan tetap berbicara. Sentuhan langsung memerlukan Android 11 atau yang lebih baru serta tidak tersedia di jam tangan atau TV.

#### Mengaktifkan sentuhan langsung untuk aplikasi {#touch-direct-touch-apps}

1. Buka **Sentuhan** > **Sentuhan langsung**.
2. Di bawah **Aplikasi**, aktifkan setiap aplikasi yang akan menerima sentuhan langsung.

Saat aplikasi yang Anda pilih berada di depan, SandFly mengucapkan "Sentuhan langsung aktif", dan saat sentuhan kembali ke SandFly, "Sentuhan langsung nonaktif".

Layar **Sentuhan langsung** memiliki:

- **Sentuhan langsung**: mengaktifkan atau menonaktifkan fitur untuk semua aplikasi. Aktif secara default.
- **Ucapkan saat sentuhan langsung berubah**: aktif secara default.
- **Bergetar saat sentuhan langsung berubah**: nonaktif secara default.
- **Cadangkan setelan** dan **Pulihkan setelan**: menyimpan setelan sentuhan langsung ke file dan memuatnya kembali, misalnya di perangkat lain.
- **Aplikasi**: daftar aplikasi dan keyboard di layar, masing-masing dengan kotak centang.

Perintah **Aktifkan atau nonaktifkan sentuhan langsung untuk aplikasi ini** menambahkan aplikasi yang sedang di depan ke daftar atau menghapusnya, serta mengaktifkan **Sentuhan langsung** saat mengaktifkan suatu aplikasi. Tetapkan perintah ini ke penekanan tombol volume agar pendamping yang melihat dapat menggunakan aplikasi yang tidak bekerja dengan SandFly sementara SandFly tetap berbicara, dan Anda dapat menonaktifkan sentuhan langsung tanpa menyentuh layar. Jika SandFly atau panel notifikasi sedang berada di depan, SandFly mengucapkan "Tidak ada aplikasi untuk sentuhan langsung".

Untuk menonaktifkan dan mengaktifkan kembali sentuhan langsung tanpa meninggalkan aplikasi, tambahkan ubin **Sentuhan langsung** ke setelan cepat.

#### Saat SandFly mengambil alih sentuhan kembali {#touch-direct-touch-return}

Sentuhan kembali ke SandFly saat dialog terbuka, aplikasi lain terbuka di atasnya, panel notifikasi atau setelan cepat menutupi separuh layar atau lebih, kolom teks difokuskan, atau layar mati. Sentuhan kembali ke aplikasi setelah semua itu hilang. Keyboard di layar selalu tetap ditangani SandFly, sehingga Anda dapat menjelajahinya sementara bagian layar lainnya memakai sentuhan langsung.

#### Pengetikan langsung {#touch-direct-typing}

Untuk aplikasi yang menggambar keyboard-nya sendiri, aktifkan pengetikan langsung: di daftar **Aplikasi**, buka tindakan pada aplikasi tersebut lalu pilih **Aktifkan pengetikan langsung**. Area keyboard lalu ikut menerima sentuhan langsung, dan aplikasi di daftar menampilkan "Pengetikan langsung aktif". **Nonaktifkan pengetikan langsung** menonaktifkannya.

#### Keyboard yang berbicara {#touch-direct-touch-keyboards}

Keyboard yang berbicara sendiri, seperti aplikasi keyboard braille, membutuhkan sentuhan di setiap aplikasi. Aktifkan di daftar **Aplikasi**. Saat keyboard tersebut tampil, sentuhan di atasnya langsung diteruskan ke keyboard di aplikasi mana pun, sementara bagian layar lainnya tetap ditangani SandFly. Keyboard yang menggambar tombolnya di overlay aksesibilitas dan memiliki layanan aksesibilitas sendiri yang aktif tetap mendapatkan area sentuh yang ditetapkannya, tanpa harus ada di daftar.

#### Aplikasi yang memilih area sentuhan langsungnya sendiri {#touch-direct-touch-areas}

Aplikasi dapat menyatakan bahwa dirinya memerlukan sentuhan langsung. SandFly mengaktifkan aplikasi seperti itu saat pertama kali tampil di depan. Anda dapat menonaktifkannya di daftar **Aplikasi**. Aplikasi juga dapat mengambil sentuhan langsung hanya pada bagian tertentu layarnya, seperti papan permainan, sementara jelajahi dengan sentuhan tetap bekerja di bagian lain, dan dapat menghentikan sentuhan langsung untuk dirinya sendiri. Aplikasi-aplikasi ini hanya bekerja selama diaktifkan di daftar **Aplikasi**. Bagi developer, antarmukanya dijelaskan di [](#api-direct-touch).

### Memperbaiki eksplorasi sentuhan yang macet {#touch-stuck}

Di beberapa ponsel, setelah sentuhan dengan beberapa jari, Android dapat menghentikan eksplorasi sentuhan untuk semua pembaca layar hingga ponsel dimulai ulang. Geseran dan ketukan tetap berfungsi, tetapi menggerakkan jari di layar tidak membacakan apa pun.

**Sentuhan** > **Perbaiki eksplorasi sentuhan yang macet**, yang aktif secara default, mendeteksi hal ini dari sentuhan eksplorasi yang tidak menemukan item. Untuk memperbaikinya, SandFly memerlukan izin DUMP:

- Saat Shizuku berjalan, SandFly memberikan izin tersebut saat dimulai dan saat Anda mengaktifkan setelan ini. Pada saat pertama kali, Shizuku menampilkan permintaan untuk mengizinkan SandFly. Lihat [](#updates).
- Tanpa Shizuku, berikan izin tersebut sekali melalui adb. Selama izin belum diberikan, setelan ini menampilkan perintahnya, dan tindakan **Salin perintah** akan menyalinnya:

```
adb shell pm grant io.github.aaron_gh.sandfly android.permission.DUMP
```

Dengan izin tersebut, SandFly mengucapkan "Memperbaiki eksplorasi sentuhan", mengakhiri sentuhan yang ditinggalkan Android dalam keadaan menempel, lalu mengucapkan "Eksplorasi sentuh diperbaiki". Sentuhan selama proses perbaikan langsung diteruskan ke aplikasi. Cara ini berfungsi pada ponsel yang mencantumkan sentuhan tersebut di `dumpsys input`, seperti ponsel Samsung.

Tanpa izin tersebut, atau di ponsel lain, SandFly hanya dapat memberi tahu bahwa eksplorasi sentuhan mungkin macet dan Anda perlu memulai ulang ponsel. Menjelajahi area tanpa item yang bisa dibaca, seperti gim, juga tidak menemukan item, sehingga peringatan ini dapat muncul secara keliru paling sering sekali setiap 10 menit.

Jika layar mati selama sentuhan, Android dapat tetap menganggap jari yang sudah diangkat masih menempel, sehingga hanya eksplorasi sentuhan yang berfungsi: gerakan dan ketuk dua kali tidak melakukan apa pun. SandFly mendeteksi hal ini saat layar mati selama sentuhan, atau saat dua sentuhan berturut-turut berakhir sebagai eksplorasi sentuhan, lalu menonaktifkan dan mengaktifkan kembali eksplorasi sentuhan. Sentuhan yang berakhir karena jari berikutnya menyentuh layar, seperti saat mengetik cepat, tidak dihitung. Hal ini tidak bergantung pada **Perbaiki eksplorasi sentuhan yang macet** atau izin DUMP.

### Mereset semua setelan penundaan {#touch-reset-delays}

**Sentuhan** > **Reset semua setelan penundaan** mengembalikan setelan berikut ke nilai default-nya, setelah dialog **Reset setelan penundaan?**:

- **Sentuhan** > **Satu ketukan untuk mengaktifkan** dan **Penundaan fokus**.
- **Tampilan** > **Matikan animasi**.
- **Mengetik** > **Metode pengetikan**, **Penundaan sentuh lama saat mengetik**, **Penundaan fokus pengetikan**, dan **Bacakan tombol keyboard lebih cepat**.
- **Navigasi** > **Ucapkan item sebelum digulir hingga terlihat**.
- Apakah kontrol pembacaan **Penundaan fokus** dan **Penundaan fokus pengetikan** ditampilkan.

Lihat [](#typing) untuk setelan pengetikan.
