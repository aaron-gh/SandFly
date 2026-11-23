## Tombol volume dan sensor {#buttons}

**Tombol volume** di setelan SandFly mengatur fungsi tombol volume saat SandFly aktif: menyesuaikan penggeser, menggerakkan kursor, menjalankan perintah, dan umpan balik saat volume berubah. Bagian terakhir bab ini mencantumkan fitur yang menggunakan sensor ponsel.

### Tombol volume dan volume ucapan {#buttons-speech-volume}

Untuk mengubah volume ucapan dari layar mana pun, tahan satu jari di layar lalu tekan tombol naikkan volume atau turunkan volume. Tombol volume juga mengubah volume ucapan selama pembacaan berkelanjutan. Volume ucapan dan setelan volume lainnya dijelaskan di [](#sound).

Menekan kedua tombol volume secara bersamaan tidak menjalankan perintah SandFly apa pun, sehingga pintasan aksesibilitas Android tetap berfungsi. Tindakan ini juga membuka kunci sentuhan; lihat [](#touch-lock).

### Menyesuaikan penggeser dan menggerakkan kursor {#buttons-sliders-cursor}

- **Tombol volume menyesuaikan penggeser**: saat penggeser memiliki fokus, tombol naikkan volume dan turunkan volume mengubah nilainya, bukan mengubah volume, dan SandFly mengucapkan nilai barunya. Tahan tombol untuk terus mengubah nilai. Nonaktif secara default.
- **Tombol volume menggerakkan kursor**: saat kolom teks memiliki fokus, tombol naikkan volume menggerakkan kursor mundur satu karakter dan tombol turunkan volume menggerakkannya maju satu karakter, dan SandFly mengucapkan karakter tersebut. Nonaktif secara default.

Saat fokus berada pada elemen lain, atau saat layar mati, tombol volume mengubah volume seperti biasa. Dengan satu jari di layar atau selama pembacaan berkelanjutan, tombol volume tetap mengubah volume ucapan.

### Menjalankan perintah dengan tombol volume {#buttons-commands}

Setiap tombol volume dapat menjalankan perintah untuk tekan dua kali, tekan tiga kali, dan tekan lama: **Tekan dua kali tombol naikkan volume**, **Tekan tiga kali tombol naikkan volume**, **Tekan lama tombol naikkan volume**, **Tekan dua kali tombol turunkan volume**, **Tekan tiga kali tombol turunkan volume**, dan **Tekan lama tombol turunkan volume**. Semuanya belum ditetapkan secara default.

Untuk menetapkan perintah ke suatu penekanan:

1. Buka **Tombol volume**.
2. Pilih jenis penekanan, seperti **Tekan dua kali tombol turunkan volume**.
3. Pilih perintah dari daftar, yang sama seperti daftar untuk gestur. Lihat [](#commands).

Tombol tanpa perintah untuk penekanan apa pun akan langsung mengubah volume seperti biasa. Saat tombol memiliki perintah tekan dua kali atau tiga kali, penekanan sekali akan mengubah volume 0,3 detik setelah Anda melepaskan tombol jika tidak ada penekanan lanjutan. Saat tombol memiliki perintah tekan lama, menahannya akan menjalankan perintah tersebut, bukan mengubah volume berulang kali.

**Durasi tekan lama tombol volume** mengatur berapa lama Anda menahan tombol untuk tekan lama: **Default sistem**, atau dari 0,3 hingga 1 detik.

Penekanan tidak menjalankan perintah, dan tombol mengubah volume seperti biasa, dalam kondisi berikut:

- Saat layar mati, kecuali layar kunci tampil di layar selalu aktif (always-on display), atau **Pintasan tombol volume saat layar mati** aktif. Lihat [](#buttons-screen-off).
- Selama panggilan telepon, atau saat aplikasi memutar audio panggilan.
- Saat ada jari di layar, atau selama pembacaan berkelanjutan.
- Saat tombol menyesuaikan penggeser atau menggerakkan kursor.

Saat panggilan berdering, hanya tombol volume yang memiliki perintah **Jawab panggilan** pada salah satu penekanannya yang menjalankan perintah.

### Mencegah penekanan tombol volume diteruskan ke Android {#buttons-take-all}

- **SandFly menangani setiap penekanan tombol volume**: saat layar menyala, setiap penekanan tombol volume masuk ke SandFly. Penekanan yang memiliki perintah akan menjalankannya, sedangkan penekanan lain, termasuk menahan tombol, tidak mengubah volume. Saat setelan ini aktif, **Tekan volume naik** dan **Tekan volume turun** muncul dan menjalankan perintah untuk sekali tekan. Nonaktif secara default.
- **Sembunyikan panel volume**: saat layar menyala, penekanan tanpa perintah akan mengubah volume tanpa menampilkan panel volume Android. Nonaktif secara default. Jika kedua setelan aktif, penekanan tanpa perintah tidak mengubah volume.

Kedua setelan tersebut tetap menyerahkan tombol ke Android saat layar mati, selama panggilan telepon atau saat aplikasi memutar audio panggilan, saat panggilan berdering, saat SandFly dijeda, dan saat kedua tombol volume ditahan. Penyesuaian penggeser, pergerakan kursor, dan volume ucapan dengan satu jari di layar tetap berfungsi.

### Pintasan tombol volume saat layar mati {#buttons-screen-off}

Jika **Pintasan tombol volume saat layar mati** aktif, penekanan tombol volume yang memiliki perintah tetap menjalankannya saat layar mati. Penekanan tanpa perintah mengubah volume. Setelan ini memerlukan Shizuku dan tidak ditampilkan jika SandFly menggunakan root alih-alih Shizuku atau di jam tangan; lihat [](#updates). Saat pertama kali mengaktifkannya, Shizuku menampilkan permintaan izin untuk SandFly. Nonaktif secara default.

### Volume ucapan di ponsel Samsung {#buttons-samsung}

Di ponsel Samsung, One UI mengubah volume ucapan saat Anda menekan tombol volume ketika SandFly berbicara, serta satu detik setelahnya. SandFly menahan penekanan ini dari One UI dan mengubah volume yang biasanya diubah tombol saat SandFly diam, seperti volume media.

Agar tombol volume mengubah volume ucapan saat SandFly berbicara, aktifkan **Tombol volume mengubah volume ucapan saat berbicara**. Setelan ini hanya ditampilkan di ponsel Samsung dan nonaktif secara default.

### Umpan balik perubahan volume {#buttons-feedback}

**Umpan balik perubahan volume** mengatur apa yang diputar SandFly saat volume berubah:

- **Nonaktif**.
- **Nada**: nada singkat pada setiap langkah perubahan volume, pada tingkat volume yang sedang diubah.
- **Ucapan**: jenis volume yang berubah dan tingkat barunya, seperti "Volume media disetel ke 60 persen", setelah langkah terakhir. Ini adalah setelan default.
- **Nada dan ucapan**.

Jika nada digunakan, **Bisukan nada saat media diputar** mematikan nada saat aplikasi memutar media. Jika ucapan digunakan, **Bisukan ucapan saat media diputar** mematikan pengumuman saat aplikasi memutar media, dan **Hentikan ucapan untuk mengumumkan volume** menghentikan ucapan yang sedang diputar untuk mengumumkan volume. Kedua tombol media tersebut nonaktif secara default, sedangkan **Hentikan ucapan untuk mengumumkan volume** aktif.

Perubahan volume yang dilakukan SandFly, seperti melalui perintah volume, tidak mendapatkan umpan balik. Perubahan volume yang dilakukan aplikasi lain tetap mendapatkan umpan balik. Tidak ada nada selama panggilan atau saat SandFly dijeda.

### Sensor {#buttons-sensors}

Fitur berikut menggunakan sensor ponsel. Masing-masing diatur di layar yang disebutkan di sini.

- **Ketuk belakang**: dua, tiga, atau empat ketukan di bagian belakang ponsel menjalankan perintah. **Gestur** > **Ketuk belakang**; lihat [](#gestures-back-tap).
- **Goyangkan**: menggoyangkan ponsel menjalankan perintah. **Gestur** > **Goyangkan**; lihat [](#gestures-shake).
- **Tutupi sensor kedekatan untuk menghentikan ucapan**: menutupi sensor kedekatan menghentikan ucapan. Nonaktif secara default. **Ucapan** > **Tutupi sensor kedekatan untuk menghentikan ucapan**; lihat [](#speech).
- **Kunci sentuhan secara otomatis** > **Saat sensor jarak tertutup**: sentuhan dikunci saat sesuatu menutupi sensor jarak. Lihat [](#touch-lock).
- **Kocok untuk berhenti** dan **Lambaikan tangan untuk berhenti**: menggoyangkan ponsel, atau menggerakkan tangan di atas sensor jarak, menghentikan pembacaan notifikasi. **Notifikasi** > **Berhenti membaca**; lihat [](#notifications).
- **Speaker saat jauh dari telinga**: panggilan berpindah ke speaker saat Anda menjauhkan ponsel dari telinga, dan kembali ke earpiece saat Anda mendekatkannya. **Output audio** > **Speaker saat jauh dari telinga**; lihat [](#sound).
