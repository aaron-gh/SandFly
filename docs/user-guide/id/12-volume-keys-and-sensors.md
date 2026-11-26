## Tombol volume dan sensor {#buttons}

**Tombol volume** di setelan SandFly mengatur fungsi tombol volume saat SandFly aktif, yaitu menyesuaikan penggeser, menggerakkan kursor, dan menjalankan perintah. Layar ini juga mengatur umpan balik saat volume berubah. Bagian terakhir bab ini mencantumkan fitur yang menggunakan sensor ponsel.

### Tombol volume dan volume ucapan {#buttons-speech-volume}

Untuk mengubah volume ucapan dari layar mana pun, tahan satu jari di layar lalu tekan tombol naikkan volume atau turunkan volume. Tombol volume juga mengubah volume ucapan selama pembacaan berkelanjutan. Volume ucapan dan setelan volume lainnya dijelaskan di [](#sound).

Menekan kedua tombol volume secara bersamaan tidak menjalankan perintah SandFly apa pun, sehingga pintasan aksesibilitas Android tetap berfungsi. Tindakan ini juga membuka kunci sentuhan. Lihat [](#touch-lock).

### Menyesuaikan penggeser dan menggerakkan kursor {#buttons-sliders-cursor}

- **Tombol volume menyesuaikan penggeser**: saat penggeser memiliki fokus, tombol naikkan volume dan turunkan volume mengubah nilainya, bukan mengubah volume, dan SandFly mengucapkan nilai barunya. Tahan tombol untuk terus mengubah nilai. Nonaktif secara default.
- **Tombol volume menggerakkan kursor**: saat kolom teks memiliki fokus, tombol naikkan volume menggerakkan kursor mundur satu karakter, sedangkan tombol turunkan volume menggerakkannya maju satu karakter. SandFly mengucapkan karakter tersebut. Nonaktif secara default.

Jika tombol juga memiliki perintah untuk salah satu penekanannya (lihat [](#buttons-commands)), penekanannya menjalankan perintah tersebut, dan penekanan tanpa perintah menyesuaikan penggeser atau menggerakkan kursor setelah selesai, bukan mengubah volume.

Saat fokus berada pada item lain, atau saat layar mati, tombol volume mengubah volume seperti biasa. Dengan satu jari di layar atau selama pembacaan berkelanjutan, tombol volume tetap mengubah volume ucapan.

### Menjalankan perintah dengan tombol volume {#buttons-commands}

Setiap tombol volume dapat menjalankan perintah untuk tekan dua kali, tekan tiga kali, dan tekan lama: **Tekan dua kali tombol naikkan volume**, **Tekan tiga kali tombol naikkan volume**, **Tekan lama tombol naikkan volume**, **Tekan dua kali tombol turunkan volume**, **Tekan tiga kali tombol turunkan volume**, dan **Tekan lama tombol turunkan volume**. Semuanya belum ditetapkan secara default.

Untuk menetapkan perintah ke suatu penekanan:

1. Buka **Tombol volume**.
2. Pilih jenis penekanan, seperti **Tekan dua kali tombol turunkan volume**.
3. Pilih perintah dari daftar, yang sama seperti daftar untuk gerakan. Lihat [](#commands).

Tombol yang tidak memiliki perintah sama sekali langsung mengubah volume seperti biasa. Jika tombol memiliki perintah tekan dua kali atau tiga kali, penekanan sekali baru mengubah volume 0,3 detik setelah Anda melepaskan tombol, asalkan tidak ada penekanan berikutnya. Saat tombol memiliki perintah tekan lama, menahannya akan menjalankan perintah tersebut, bukan mengubah volume berulang kali.

**Durasi tekan lama tombol volume** mengatur berapa lama Anda harus menahan tombol agar dihitung sebagai tekan lama: **Default sistem**, atau dari 0,3 hingga 1 detik.

Dalam kondisi berikut, penekanan tidak menjalankan perintah, dan tombol mengubah volume seperti biasa:

- Saat layar mati, kecuali jika layar kunci tampil di layar selalu aktif (always-on display), atau jika **Pintasan tombol volume saat layar mati** aktif. Lihat [](#buttons-screen-off).
- Selama panggilan telepon, atau saat aplikasi memutar audio panggilan.
- Saat ada jari di layar, atau selama pembacaan berkelanjutan.
- Saat aplikasi Shortcut Menu ditampilkan.

Saat panggilan telepon berdering, hanya tombol volume yang memiliki perintah **Jawab panggilan** atau **Akhiri panggilan** pada salah satu penekanannya yang menjalankan perintah.

### Mencegah penekanan tombol volume diteruskan ke Android {#buttons-take-all}

- **SandFly menangani setiap penekanan tombol volume**: saat layar menyala, setiap penekanan tombol volume masuk ke SandFly. Penekanan yang memiliki perintah menjalankan perintah itu, sedangkan penekanan lain, termasuk menahan tombol, tidak mengubah volume. Saat setelan ini aktif, setelan **Tekan volume naik** dan **Tekan volume turun** muncul. Keduanya menetapkan perintah untuk sekali tekan. Nonaktif secara default.
- **Sembunyikan panel volume**: saat layar menyala, penekanan tanpa perintah akan mengubah volume tanpa menampilkan panel volume Android. Nonaktif secara default. Jika kedua setelan aktif, penekanan tanpa perintah tidak mengubah volume.

Kedua setelan tersebut tetap membiarkan tombol ditangani Android saat layar mati, selama panggilan telepon atau saat aplikasi memutar audio panggilan, saat panggilan berdering, saat SandFly dijeda, dan saat kedua tombol volume ditahan. Penyesuaian penggeser, pergerakan kursor, dan volume ucapan dengan satu jari di layar tetap berfungsi.

### Pintasan tombol volume saat layar mati {#buttons-screen-off}

Jika **Pintasan tombol volume saat layar mati** aktif, penekanan tombol volume yang memiliki perintah tetap menjalankannya saat layar mati. Penekanan tanpa perintah mengubah volume. Setelan ini memerlukan Shizuku. Setelan ini tidak ditampilkan jika SandFly menggunakan root, bukan Shizuku, dan tidak ditampilkan di jam tangan. Lihat [](#updates-shizuku). Saat Anda pertama kali mengaktifkannya, Shizuku meminta Anda mengizinkan SandFly. Nonaktif secara default.

### Tombol bisukan {#buttons-mute-key}

Di ponsel yang memiliki tombol bisukan, seperti Zinwa Q25, **Tombol volume** memiliki dua setelan tambahan:

- **Tahan tombol bisukan untuk mendikte di kolom teks**: saat kolom teks memiliki fokus input, keyboard tampil atau terhubung, dan tidak ada panggilan aktif, menahan tombol bisukan menjalankan **Dikte**. Perekaman dimulai saat Anda menekan tombol dan berhenti saat Anda melepaskannya, lalu SandFly mengetikkan teksnya. Menekan tombol ini selama dikte ke kolom teks berlangsung akan menghentikan dikte tersebut. Aktif secara default. Lihat [](#typing-dictate).
- **Saat tombol bisukan ditekan**: perintah yang dijalankan saat Anda menekan tombol bisukan di tempat lain. Daftarnya sama seperti untuk gerakan. Tidak ditetapkan secara default.

Jika tidak ada setelan yang mengambil penekanan tersebut, penekanan diteruskan ke peta tombol, lalu ke Android. Di peta tombol ponsel berkeyboard dan di peta tombol kustom, tombol bisukan dapat menjadi pintasan keyboard tersendiri. Lihat [](#keyboards-keymaps).

### Volume ucapan di ponsel Samsung {#buttons-samsung}

Di ponsel Samsung, One UI mengubah volume ucapan jika Anda menekan tombol volume saat SandFly berbicara atau dalam satu detik setelahnya. SandFly mencegat penekanan ini sebelum sampai ke One UI. SandFly lalu mengubah volume yang biasanya diubah tombol saat SandFly diam, seperti volume media.

Agar tombol volume mengubah volume ucapan saat SandFly berbicara, aktifkan **Tombol volume mengubah volume ucapan saat SandFly berbicara**. Setelan ini hanya ditampilkan di ponsel Samsung dan nonaktif secara default.

### Umpan balik perubahan volume {#buttons-feedback}

**Umpan balik perubahan volume** mengatur apa yang diputar SandFly saat sebuah volume berubah:

- **Nonaktif**.
- **Nada**: nada singkat pada setiap langkah perubahan, diputar dengan volume yang sedang diubah.
- **Ucapan**: setelah langkah terakhir, SandFly mengucapkan jenis volume yang berubah dan tingkat barunya, seperti "Volume media disetel ke 60 persen". Ini adalah setelan default.
- **Nada dan ucapan**.

Jika nada digunakan, **Bisukan nada saat media diputar** mematikan nada saat aplikasi memutar media. Jika ucapan digunakan, **Bisukan ucapan saat media diputar** mematikan pengumuman saat aplikasi memutar media, dan **Hentikan ucapan untuk mengumumkan volume** menghentikan ucapan yang sedang berlangsung agar volume dapat diumumkan. Kedua setelan untuk media tersebut nonaktif secara default, sedangkan **Hentikan ucapan untuk mengumumkan volume** aktif.

Jika ucapan digunakan, **Ubah volume ucapan tanpa pengumuman saat SandFly berbicara** menghilangkan pengumuman saat tombol volume mengubah volume ucapan selama SandFly berbicara atau masih memiliki ucapan dalam antrean. Dengan begitu, ucapan berlanjut dengan volume baru. Penekanan yang berturut-turut dalam satu detik dihitung sebagai satu rangkaian, dan rangkaian itu tetap tidak diumumkan meskipun ucapan sudah selesai. Nada tetap diputar jika dipilih. Penekanan saat ada jari di layar, penekanan saat SandFly tidak berbicara, dan perubahan dari panel volume atau aplikasi tetap diumumkan. Di ponsel Samsung, di luar pembacaan berkelanjutan, tombol volume mengubah volume ucapan saat SandFly berbicara hanya jika **Tombol volume mengubah volume ucapan saat SandFly berbicara** aktif. Nonaktif secara default.

Perubahan volume yang dilakukan SandFly sendiri, seperti melalui perintah volume, tidak diberi umpan balik. Perubahan volume yang dilakukan aplikasi lain tetap diberi umpan balik. Tidak ada nada selama panggilan atau saat SandFly dijeda.

### Sensor {#buttons-sensors}

Fitur berikut menggunakan sensor ponsel. Masing-masing diatur di layar yang disebutkan di sini.

- **Ketuk belakang**: dua, tiga, atau empat ketukan di bagian belakang ponsel menjalankan perintah. Atur di **Gerakan** > **Ketuk belakang**. Lihat [](#gestures-back-tap).
- **Goyangkan**: menggoyangkan ponsel menjalankan perintah. Atur di **Gerakan** > **Goyangkan**. Lihat [](#gestures-shake).
- **Tutupi sensor kedekatan untuk menghentikan ucapan**: menutupi sensor kedekatan menghentikan ucapan. Nonaktif secara default. Atur di **Ucapan** > **Tutupi sensor kedekatan untuk menghentikan ucapan**. Lihat [](#speech-stop).
- **Kunci sentuhan secara otomatis** > **Saat sensor jarak tertutup**: sentuhan dikunci saat sesuatu menutupi sensor jarak. Lihat [](#touch-lock).
- **Kocok untuk berhenti** dan **Lambaikan tangan untuk berhenti**: menggoyangkan ponsel, atau menggerakkan tangan di atas sensor jarak, menghentikan pembacaan notifikasi. Atur di **Notifikasi** > **Berhenti membaca**. Lihat [](#notifications-stop).
- **Speaker saat jauh dari telinga**: panggilan berpindah ke speaker saat Anda menjauhkan ponsel dari telinga, dan kembali ke earpiece saat Anda mendekatkannya ke telinga. Atur di **Output audio** > **Speaker saat jauh dari telinga**. Memerlukan Android 12 atau yang lebih baru. Lihat [](#sound-calls).
