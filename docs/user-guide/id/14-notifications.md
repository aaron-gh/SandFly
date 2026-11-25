## Notifikasi {#notifications}

SandFly membacakan notifikasi baru saat masuk, dan dapat membacakan notifikasi di panel notifikasi saat diminta atau pada waktu yang ditetapkan. **Notifikasi** di setelan SandFly mengatur notifikasi mana yang dibacakan SandFly, apa yang diucapkan, dengan suara apa, dan kapan. Menu ini juga mengaktifkan daftar notifikasi SandFly, yang dapat menggantikan panel notifikasi sistem.

Panggilan masuk selalu dibacakan, apa pun setelan notifikasinya.

### Akses notifikasi {#notifications-access}

Beberapa fitur memerlukan akses notifikasi untuk SandFly, yang diberikan melalui setelan Android:

- Membacakan notifikasi yang tidak diumumkan Android ke pembaca layar. Lihat [](#notifications-not-announced).
- Membacakan notifikasi pribadi secara penuh saat ponsel terkunci.
- **Baca notifikasi yang diizinkan oleh mode Jangan Ganggu**. Tanpa akses notifikasi, SandFly tidak membacakan notifikasi apa pun saat mode Jangan Ganggu aktif dengan pilihan ini.
- Menghentikan pembacaan notifikasi saat notifikasi ditutup atau dihapus.
- Ringkasan notifikasi di panel notifikasi.
- Daftar notifikasi SandFly.

Untuk memberikan akses notifikasi, pilih **Aktifkan akses notifikasi** di layar **Kapan dibaca**, **Berhenti membaca**, atau **Ringkasan dan waktu**, lalu aktifkan SandFly di daftar tersebut. Item ini hanya ditampilkan saat SandFly tidak memiliki akses notifikasi. SandFly hanya terhubung ke notifikasi saat SandFly aktif.

### Mengaktifkan atau menonaktifkan pembacaan notifikasi {#notifications-speech-on-off}

**Notifikasi** > **Ucapkan notifikasi** mengaktifkan atau menonaktifkan pembacaan notifikasi baru. Setelan ini aktif secara default. Panggilan masuk tetap dibacakan meskipun setelan ini nonaktif.

Anda juga dapat mengaktifkan atau menonaktifkan pembacaan notifikasi dengan:

- Kartu setelan cepat **Pembacaan notifikasi**.
- Perintah **Pembacaan notifikasi aktif atau nonaktif**, yang dapat Anda tetapkan ke gestur atau pintasan keyboard. SandFly mengucapkan "Pembacaan notifikasi diaktifkan" atau "Pembacaan notifikasi dinonaktifkan".
- Aplikasi otomatisasi. Lihat [](#notifications-automation).

### Perintah untuk notifikasi {#notifications-commands}

Kategori **Notifikasi** di **Perintah** berisi:

- **Baca notifikasi**: membacakan notifikasi di panel notifikasi, sesuai setelan **Ringkasan dan waktu**. Lihat [](#notifications-summary).
- **Baca lagi notifikasi terakhir**: mengucapkan kembali notifikasi terakhir yang dibacakan SandFly. SandFly mengucapkan "Tidak ada notifikasi yang dibacakan" jika belum ada.
- **Hentikan pembacaan notifikasi**: menghentikan notifikasi yang sedang dibacakan serta notifikasi yang sedang mengantre untuk dibacakan.
- **Pembacaan notifikasi aktif atau nonaktif**
- **Pengumuman waktu aktif atau nonaktif**: menyetel **Ucapkan waktunya setiap** ke **Nonaktif**, atau mengembalikannya ke interval sebelumnya, atau ke 1 jam jika sebelumnya belum disetel. SandFly mengucapkan "Pengumuman waktu aktif" atau "Pengumuman waktu nonaktif".

Anda dapat menetapkan perintah-perintah ini ke gestur, pintasan keyboard, dan input lainnya. Lihat [](#commands).

Gestur, pintasan keyboard, atau tombol yang Anda gunakan saat notifikasi dibacakan akan menghentikannya, dan SandFly mengucapkan tindakan yang dilakukan gestur atau tombol tersebut.

### Mengubah apa yang diucapkan {#notifications-reading}

**Notifikasi** > **Pembacaan notifikasi** mengatur apa yang diucapkan SandFly untuk notifikasi dan cara membacakannya. Di bawah **Apa yang dikatakan**:

- **Format ucapan**: teks yang diucapkan SandFly untuk setiap notifikasi, tersusun dari placeholder: `{app}`, `{title}`, `{text}`, `{content}` (keseluruhan teks), `{bigtext}`, `{subtext}`, `{ticker}`, `{category}`, dan `{time}`. Default-nya adalah `{category}, {title}, {text}`.
- **Pembacaan notifikasi pribadi**: teks yang diucapkan SandFly sebagai pengganti isi notifikasi pribadi, dengan placeholder `{app}`, `{category}`, dan `{time}`. Default-nya adalah `{app}`.

Di bawah **Teks**:

- **Lewati emoji**: emoji dalam notifikasi tidak dibacakan. Nonaktif secara default.
- **Baca huruf kapital sebagai kata-kata**: kata dalam huruf kapital dibacakan sebagai kata. Beberapa mesin ucapan mengejanya huruf demi huruf. Nonaktif secara default.
- **Bacakan angka panjang per digit**: angka seperti kode dan nomor telepon dibacakan satu digit pada satu waktu. Nonaktif secara default. **Jumlah digit minimum dalam angka panjang** mengatur berapa digit yang harus dimiliki angka agar dibacakan dengan cara ini, dari 3 hingga 10; default-nya 5.
- **Link**: **Bacakan seluruh link** (default), **Bacakan situsnya saja**, **Ucapkan teks di bawah**, atau **Lewati**. **Teks pengganti link** adalah teks untuk pilihan **Ucapkan teks di bawah**.
- **Lewati notifikasi tanpa teks**: notifikasi tanpa teks tidak dibacakan. Nonaktif secara default.

Di bawah **Panjang**, semuanya **Nonaktif** secara default:

- **Jumlah kata maksimum**: membacakan hingga 5, 10, 15, 20, 30, 50, atau 100 kata.
- **Jumlah kalimat maksimum**: membacakan hingga 1, 2, 3, atau 5 kalimat.
- **Jumlah maksimum karakter teks**: membacakan hingga 50, 100, 200, 300, 500, atau 1000 karakter.
- **Berhenti membaca setelah**: menghentikan notifikasi setelah 5 hingga 60 detik.

Di bawah **Waktu**:

- **Tunggu sebelum membaca**: menunggu hingga 10 detik sebelum membacakan notifikasi. **Nonaktif** secara default.
- **Saat notifikasi lain dibaca**: apa yang terjadi saat notifikasi masuk ketika notifikasi lain sedang dibacakan: **Baca setelahnya** (default), **Interupsi**, atau **Jangan bacakan yang baru**.
- **Baca lagi sampai layar menyala**: mengucapkan kembali setiap 1 hingga 30 menit notifikasi yang masuk saat layar mati, hingga layar menyala. **Nonaktif** secara default.

Untuk setelan suara di layar ini, lihat [](#notifications-voice).

### Suara notifikasi {#notifications-voice}

Di bawah **Notifikasi** > **Pembacaan notifikasi** > **Suara**:

- **Suara**: membacakan notifikasi dengan salah satu profil suara Anda beserta semua setelannya, seperti mesin ucapan, kecepatan, dan nada suara lain. Default-nya adalah **Suara SandFly**.
- **Profil suara**: membuka profil suara, tempat Anda membuatnya dan mengubahnya. Lihat [](#speech-voice-profiles).
- **Baca menimpa ucapan SandFly dengan suara berbeda**: lihat di bawah. Nonaktif secara default.
- **Deteksi bahasa**: mengucapkan setiap notifikasi dalam bahasa penulisannya. Setelan ini memerlukan **Ucapan** > **Ganti bahasa secara otomatis**. Lihat [](#speech-languages). Nonaktif secara default.
- **Suara sebelum notifikasi**: memutar suara sebelum setiap notifikasi. Nonaktif secara default.

Aplikasi dan aturan dapat memiliki suaranya sendiri. Lihat [](#notifications-apps) dan [](#notifications-rules).

Cara notifikasi dengan suara berbeda dibacakan:

- Notifikasi dengan profil suara yang sama dengan suara SandFly dibacakan setelah ucapan saat ini, di antrean ucapan yang sama.
- Notifikasi dengan suara berbeda juga dibacakan setelah ucapan saat ini, agar dua suara tidak berbicara bersamaan. Notifikasi menunggu hingga 45 detik. Jika **Baca menimpa ucapan SandFly dengan suara berbeda** aktif, notifikasi langsung dibacakan menimpa ucapan saat ini.
- **Baca menimpa ucapan SandFly dengan suara berbeda** tidak berlaku saat **Ucapkan hanya fokus yang dipindahkan pengguna** berlaku pada aplikasi di depan, atau saat asisten atau panggilan memutar audio. Lihat [](#verbosity-own-speech).
- Saat **Senyap saat merekam**, di **Ucapan**, berlaku, tidak ada notifikasi yang dibacakan. Lihat [](#speech).
- Saat notifikasi dengan suara berbeda masuk bersamaan, masing-masing menunggu notifikasi sebelumnya selesai.

Jika mesin ucapan suatu profil suara gagal, SandFly membacakan notifikasi dengan suaranya sendiri.

### Aplikasi {#notifications-apps}

**Notifikasi** > **Aplikasi** mengatur cara pembacaan notifikasi setiap aplikasi.

- **Aplikasi lain**: cara SandFly membacakan notifikasi dari aplikasi yang tidak memiliki setelan khusus: **Baca** (default), **Hanya nama aplikasi**, atau **Jangan baca**.
- **Aplikasi**: aplikasi di peluncur, aplikasi yang memiliki setelan, dan aplikasi di riwayat, menurut nama. Masing-masing menampilkan setelannya.

Untuk satu aplikasi:

- **Notifikasi**: **Seperti aplikasi lainnya** (default), **Baca**, **Hanya nama aplikasi**, **Baca dulu**, atau **Jangan baca**. **Hanya nama aplikasi** tidak pernah membacakan isi notifikasi. Notifikasi **Baca dulu** memotong notifikasi yang sedang dibacakan.
- **Nama yang diucapkan**: nama yang diucapkan SandFly untuk aplikasi tersebut.
- **Waktu antara notifikasi aplikasi**: jeda tersingkat antara dua notifikasi aplikasi yang dibacakan. **Seperti aplikasi lainnya** menggunakan setelan di **Pengulangan**.
- **Format ucapan**: format ucapan khusus untuk aplikasi tersebut. Lihat [](#notifications-reading).
- **Suara**: **Seperti aplikasi lainnya**, **Suara SandFly**, atau profil suara, seperti profil dengan mesin ucapan lain.
- **Setel ulang**: menghapus setelan khusus aplikasi tersebut. Hanya ditampilkan untuk aplikasi yang memiliki setelan.

### Kata dan pengganti {#notifications-words}

**Notifikasi** > **Kata dan pengganti** memiliki empat daftar. Setiap item dapat berupa ekspresi reguler.

- **Hanya bacakan notifikasi yang berisi**: jika daftar ini berisi kata, hanya notifikasi yang memuat salah satunya yang dibacakan.
- **Jangan bacakan notifikasi yang berisi**: notifikasi yang memuat salah satu kata ini tidak dibacakan.
- **Bacakan hanya nama aplikasi untuk notifikasi yang berisi**: untuk notifikasi yang memuat salah satu kata ini, hanya nama aplikasi yang diucapkan.
- **Ucapan berbeda**: teks yang diucapkan SandFly secara berbeda, seperti "lol" sebagai "tertawa". Setiap pengganti memiliki **Teks**, **Ucapkan sebagai**, **Hanya kata utuh** (aktif secara default), dan **Ekspresi reguler**.

Untuk menambahkan kata, pilih **Tambahkan sebuah kata** di bawah daftarnya, masukkan teks, lalu pilih **Oke**. Untuk menambahkan pengganti, pilih **Tambahkan pengganti**. Pilih item untuk **Sunting** atau **Hapus**.

### Jenis notifikasi {#notifications-kinds}

**Notifikasi** > **Jenis notifikasi** mengecualikan jenis notifikasi tertentu:

- **Lewati pemutar media**: aktif secara default.
- **Lewati notifikasi yang sedang berlangsung**: seperti navigasi dan pengatur waktu.
- **Lewati notifikasi aplikasi yang berjalan**: seperti VPN atau perekam audio.
- **Lewati notifikasi progres**
- **Lewati ringkasan grup**: seperti "3 pesan baru".
- **Lewati notifikasi prioritas rendah**
- **Lewati notifikasi sistem**

Semua kecuali **Lewati pemutar media** nonaktif secara default.

### Kapan dibaca {#notifications-when}

**Notifikasi** > **Kapan dibaca** mengatur kondisi saat notifikasi dibacakan:

- **Saat layar menyala**: aktif secara default.
- **Saat layar mati**: nonaktif secara default.
- **Saat telepon terkunci**: aktif secara default.
- **Headphone**: **Membaca dengan lantang dengan atau tanpa headphone** (default), **Hanya dengan headphone**, atau **Hanya tanpa headphone**.
- **Saat media diputar**: **Baca** (default), **Jeda media saat membaca**, atau **Jangan baca**.
- **Jangan Ganggu**: **Jangan baca apa pun**, **Baca notifikasi yang diizinkan oleh mode Jangan Ganggu** (default), atau **Baca semuanya**. **Baca notifikasi yang diizinkan oleh mode Jangan Ganggu** memerlukan akses notifikasi.
- **Saat layar disentuh**: **Baca segera**, **Baca ketika sentuhan berakhir** (default), atau **Jangan baca**. Dengan **Baca ketika sentuhan berakhir**, notifikasi yang masuk saat Anda menyentuh layar akan dibacakan setengah detik setelah sentuhan berakhir, dan paling lambat 10 detik setelah notifikasi pertama masuk.
- **Selama panggilan**: membacakan notifikasi selama panggilan. Nonaktif secara default.
- **Tidak dalam mode senyap**: nonaktif secara default.
- **Tidak dalam mode getar**: nonaktif secara default.
- **Tidak dalam mode mobil**: seperti dengan Android Auto, yang membacakan notifikasi sendiri. Nonaktif secara default.
- **Jam tenang**: tidak ada notifikasi yang dibacakan antara **Mulai** dan **Akhir**. Nonaktif secara default; waktu default-nya adalah 22:00 dan 07:00.

### Aturan {#notifications-rules}

**Notifikasi** > **Aturan** mengubah cara pembacaan notifikasi pada kondisi tertentu. Sebuah aturan memiliki kondisi dan tindakan: saat kondisinya terpenuhi, tindakannya diterapkan ke notifikasi tersebut. Aturan tanpa kondisi berlaku untuk setiap notifikasi. Suara suatu aturan menggantikan suara aplikasi.

Untuk menambahkan aturan:

1. Pilih **Tambahkan aturan**, masukkan nama, lalu pilih **Oke**. Atau pilih **Tambahkan aturan dari templat** dan pilih sebuah templat.
2. Di bawah **Ketika**, pilih **Tambahkan kondisi** lalu pilih kondisi.
3. Di bawah **Lalu**, pilih **Tambahkan tindakan** lalu pilih tindakan.

Sebuah aturan memiliki:

- **Nama**
- **Diaktifkan**
- **Kondisi**: **Semua kondisi harus dipenuhi**, atau **Setidaknya satu syarat harus dipenuhi**.
- **Ketika**: daftar kondisi. Pilih kondisi untuk **Sunting**, **Balik kondisi** agar terpenuhi saat kondisi sebaliknya terjadi seperti "Layar mati" alih-alih "Layar menyala", atau **Hapus**.
- **Lalu**: daftar tindakan. Pilih tindakan untuk menghapusnya.
- **Hapus aturan**

Kondisinya adalah **Perangkat Bluetooth** (perangkat yang Anda pilih atau **Perangkat apa pun**), **Headphone berkabel**, **Jaringan Wi-Fi** (berdasarkan nama), **Waktu dalam sehari** (dengan **Dari**, **Sampai**, dan **Hari**), **Layar menyala**, **Telepon terkunci**, **Notifikasi dari aplikasi**, **Aplikasi di latar depan**, **Notifikasi berisi** (teks atau ekspresi reguler), **Level baterai di bawah** (persentase), **Mengisi daya**, **Lanskap**, **Mode senyap atau getar**, dan **Pemutaran media**. **Perangkat Bluetooth** memerlukan izin Perangkat di sekitar pada Android 12 ke atas, dan **Jaringan Wi-Fi** memerlukan izin lokasi; SandFly memintanya saat Anda menambahkan kondisi tersebut.

Tindakannya adalah: jangan bacakan, bacakan nama aplikasi saja, bacakan seluruhnya meskipun pribadi, gunakan format ucapan, bacakan hingga sejumlah kata, bacakan tanpa batas panjang, pertahankan emoji, satukan angka, bacakan lebih dulu, bacakan bahkan saat jam tenang dan waktu lain ketika notifikasi tidak dibacakan, dan bacakan dengan profil suara.

Templat yang tersedia adalah **Hanya dengan headphone**, **Senyap di malam hari** (22:00 hingga 07:00), **Hanya dengan layar mati**, **Hanya nama aplikasi saat ponsel terkunci**, dan **Singkat saat tidak mengisi daya** (hingga 15 kata).

### Pengulangan {#notifications-repeats}

**Notifikasi** > **Pengulangan** menangani notifikasi yang muncul berulang dan aplikasi yang mengirim banyak notifikasi:

- **Lewati notifikasi yang sama selama**: notifikasi dengan teks yang sama dengan yang sudah dibacakan dalam rentang waktu ini tidak dibacakan lagi. Pilihannya adalah **Nonaktif** (default), dari 10 detik hingga 1 jam, dan **Hingga SandFly dimulai ulang**.
- **Angka dianggap sama**: notifikasi yang hanya berbeda pada angkanya, seperti persentase unduhan, dianggap sama. Nonaktif secara default.
- **Waktu antara notifikasi aplikasi**: jeda tersingkat antara dua notifikasi suatu aplikasi yang dibacakan, dari 5 detik hingga 15 menit. **Nonaktif** secara default. Setiap aplikasi dapat memiliki setelannya sendiri. Lihat [](#notifications-apps).
- **Hilangkan awal yang berulang**: menghilangkan awalan yang berulang pada notifikasi suatu aplikasi, seperti nama grup dalam chat. Nonaktif secara default. **Ingat awal selama** mengatur berapa lama SandFly mengingat awalan tersebut: 30 detik hingga 15 menit, 1 menit secara default.

SandFly juga hanya membacakan notifikasi satu kali saat aplikasi mengirimnya kembali dengan judul dan teks yang sama dalam 5 detik, atau mengirim teks yang sama sebagai ringkasan grup.

### Berhenti membaca {#notifications-stop}

**Notifikasi** > **Berhenti membaca** mengatur hal yang menghentikan pembacaan notifikasi:

- **Kocok untuk berhenti**: goyangkan ponsel. **Kekuatan guncangan**: **Lembut**, **Sedang** (default), atau **Kuat**.
- **Lambaikan tangan untuk berhenti**: gerakkan tangan di atas sensor jarak. **Modus saku**, aktif secara default, mencegah sensor yang sudah tertutup saat pembacaan dimulai, misalnya di dalam saku, menghentikan pembacaan.
- **Tekan kedua tombol volume**
- **Berhenti saat ditutup**: menghentikan notifikasi saat Anda menutupnya. **Juga ketika notifikasi ditutup di jam tangan pintar**, aktif secara default, juga menghentikannya saat Anda menutupnya di jam tangan yang terhubung.
- **Berhenti saat aplikasi menghapusnya**

Semua kecuali **Modus saku** dan **Juga ketika notifikasi ditutup di jam tangan pintar** nonaktif secara default. **Berhenti saat ditutup** dan **Berhenti saat aplikasi menghapusnya** memerlukan akses notifikasi. Setelan ini juga berlaku saat ringkasan sedang dibacakan.

### Ringkasan dan waktu {#notifications-summary}

**Notifikasi** > **Ringkasan dan waktu** membacakan notifikasi di panel notifikasi, dan mengucapkan waktu secara berkala. Di bawah **Ringkasan**:

- **Aktifkan akses notifikasi**: ringkasan memerlukan akses ini. Hanya ditampilkan saat SandFly tidak memiliki akses notifikasi.
- **Baca notifikasi sekarang**: membacakan notifikasi di panel notifikasi. SandFly harus aktif.
- **Baca notifikasi setiap hari**: membacakannya setiap hari pada **Waktu**, 08:00 secara default.
- **Yang terbaru dulu**: membacakan notifikasi terbaru lebih dulu. Nonaktif secara default.
- **Nama Anda**: nama untuk sapaan di awal, seperti "Selamat pagi. Sam."
- **Jeda di antara notifikasi**: dari **Nonaktif** (default) hingga 5 detik.

Ringkasan dimulai dengan sapaan dan jumlah notifikasi, seperti "Anda memiliki 5 notifikasi.", lalu membacakan setiap notifikasi sebagai aplikasi, judul, dan teks. Ringkasan grup dan notifikasi yang sedang berlangsung tidak disertakan. **Aplikasi**, **Kata dan pengganti**, **Jenis notifikasi**, dan **Aturan** berlaku, dan notifikasi yang dikecualikan olehnya tidak dihitung.

Di bawah **Pengumuman waktu**:

- **Ucapkan waktunya setiap**: **Nonaktif** (default), 15 menit, 30 menit, 1 jam, atau 3 jam.
- **Aktifkan Alarm dan pengingat**: membuka setelan Android yang memungkinkan SandFly mengucapkan waktu tepat pada waktunya. Tanpa izin ini, Android dapat menunda pengumuman. Hanya ditampilkan saat **Ucapkan waktunya setiap** tidak disetel ke **Nonaktif** dan setelan tersebut nonaktif. SandFly juga membuka setelan Android ini saat Anda memilih interval untuk **Ucapkan waktunya setiap** atau mengaktifkan **Baca notifikasi setiap hari**, kecuali jika setelan tersebut sudah aktif.
- **Format waktu**: teks yang diucapkan SandFly, dengan placeholder `{time}`, `{date}`, dan `{battery}` (persentase baterai). Default-nya adalah "Sekarang pukul {time}".
- **Suara**: **Suara notifikasi** (default), **Suara SandFly**, atau profil suara.

Ringkasan dibacakan dengan suara notifikasi.

Agar tingkat baterai diucapkan pada interval tertentu, lihat **Panjang respons** > **Tingkat baterai** di [](#verbosity-announcements).

### Riwayat pemberitahuan {#notifications-history}

**Notifikasi** > **Riwayat pemberitahuan** mencantumkan 100 notifikasi terakhir yang masuk sejak SandFly dimulai, beserta waktu masuknya, apakah dibacakan, alasannya jika tidak dibacakan seperti "Tidak dibacakan: Jangan Ganggu", dan apa yang diucapkan SandFly. Riwayat hanya disimpan di memori.

- **Simpan riwayat pemberitahuan**: aktif secara default.
- **Hapus riwayat**

Pilih notifikasi untuk tindakan berikut, yang ditampilkan dengan nama aplikasinya, seperti Chat:

- **Jangan baca Chat**: menyetel aplikasi ke **Jangan baca**.
- **Baca hanya nama Chat**: menyetel aplikasi ke **Hanya nama aplikasi**.
- **Baca Chat terlebih dahulu**: menyetel aplikasi ke **Baca dulu**.
- **Jangan membaca notifikasi seperti ini**: menambahkan ekspresi reguler yang cocok dengan notifikasi serupa ke **Jangan bacakan notifikasi yang berisi**.
- **Salin**: menyalin judul dan teksnya.

### Menguji setelan {#notifications-test}

**Notifikasi** > **Kirim notifikasi uji** menampilkan notifikasi uji SandFly setelah 5 detik, sehingga Anda dapat mematikan layar atau membuka aplikasi lain terlebih dahulu. SandFly membacakannya sesuai setelan notifikasi Anda. SandFly memerlukan izin untuk menampilkan notifikasi.

### Otomatisasi {#notifications-automation}

Aplikasi otomatisasi, seperti Tasker atau MacroDroid, dapat mengontrol pembacaan notifikasi melalui siaran (broadcast) ke SandFly. Aktifkan **Notifikasi** > **Izinkan aplikasi otomatisasi** terlebih dahulu; setelan ini nonaktif secara default karena aplikasi mana pun dapat mengirimkannya. **Notifikasi** > **Otomatisasi** mencantumkan tindakannya, masing-masing dengan tindakan siarannya: nama paket SandFly diikuti tindakan seperti `.action.READ_NOTIFICATIONS`. Pilih tindakan untuk menyalin tindakan siarannya:

- **Aktifkan pembacaan notifikasi**
- **Nonaktifkan pembacaan notifikasi**
- **Aktifkan atau nonaktifkan pembacaan notifikasi**
- **Hentikan pembacaan notifikasi**
- **Baca notifikasi**

### Daftar notifikasi SandFly {#notifications-list}

Jika **Notifikasi** > **Gunakan daftar notifikasi SandFly** aktif, perintah **Notifikasi** membuka daftar milik SandFly alih-alih panel notifikasi sistem. Di beberapa ponsel, seperti ponsel Samsung dengan banyak notifikasi, panel sistem digambar ulang terlalu sering sehingga navigasi di dalamnya lambat. Setelan ini nonaktif secara default dan memerlukan akses notifikasi.

Di dalam daftar:

- Ketuk dua kali notifikasi untuk membukanya.
- Grup ditampilkan sebagai satu item. Ketuk dua kali untuk menampilkan notifikasi di dalamnya.
- Setiap notifikasi memiliki tindakannya dan, jika disediakan aplikasi, kolom balas dengan **Kirim**.
- Notifikasi media menampilkan **Diputar** atau **Dijeda**, dengan **Sebelumnya**, **Putar** atau **Jeda**, dan **Berikutnya**.
- Teks yang lebih panjang dari sekitar 160 karakter dipotong. Tindakan **Tunjukkan semuanya** membacakan seluruh teks, dan **Tampilkan lebih sedikit** memotongnya kembali.
- **Tutup** menghapus satu notifikasi, dan **Hapus semua** menghapus semua notifikasi yang dapat dihapus.
- **Setelan cepat** membuka setelan cepat.

Saat ponsel terkunci, perintah **Notifikasi** membuka panel sistem, yang menyembunyikan hal-hal yang Anda pilih untuk disembunyikan di layar kunci. Perintah **Panel notifikasi sistem** selalu membuka panel sistem. Daftar SandFly terbuka sebagai layarnya sendiri, sehingga aplikasi di depan dijeda. Beberapa aplikasi mengakhiri proses yang sedang dilakukan saat dijeda: penyandingan proses debug nirkabel di Setelan gagal jika kode penyandingan dimasukkan dari daftar SandFly. Gunakan **Panel notifikasi sistem** untuk keperluan tersebut.

Setelan untuk daftar ini:

- **Urutkan notifikasi dari yang terbaru, tanpa grup**: setiap notifikasi menjadi item tersendiri, yang terbaru lebih dulu, dibacakan sebagai aplikasi, waktu, judul, dan teks. Nonaktif secara default.
- **Kecualikan pemutar media dari daftar notifikasi**: mengecualikan notifikasi pemutar media. Jumlah notifikasi yang dibacakan saat layar menyala juga mengecualikannya. Nonaktif secara default.
- **Detail notifikasi**: bagian notifikasi yang dibacakan daftar beserta urutannya: **Judul**, **Teks**, **Nama aplikasi**, **Waktu**, dan **Diputar atau dijeda**. Setiap bagian berupa kotak centang, semuanya dicentang secara default, dan memiliki tindakan **Pindahkan ke atas** dan **Pindahkan ke bawah**.

### Notifikasi yang tidak diumumkan Android {#notifications-not-announced}

Android hanya memberi tahu pembaca layar tentang notifikasi baru saat notifikasi tersebut berbunyi atau memberi peringatan. Notifikasi yang ditahan oleh mode Jangan Ganggu, atau pembaruan yang dikirim aplikasi tanpa memberi peringatan ulang, seperti aplikasi bel pintu yang mengganti peringatan gerakan terakhirnya, tidak diumumkan. Jika SandFly memiliki akses notifikasi, SandFly juga menerima notifikasi tersebut dan membacakannya sesuai setelan notifikasi, sehingga **Kapan dibaca** > **Jangan Ganggu** berlaku juga untuk notifikasi ini. Notifikasi progres dan notifikasi yang sedang berlangsung dikecualikan.

Saat ponsel terkunci, Android hanya memberikan versi publik dari notifikasi pribadi ke pembaca layar, yang sering tidak dimiliki banyak aplikasi. Dengan akses notifikasi, SandFly membacakan seluruh notifikasi sesuai setelan notifikasi Anda, seperti **Pembacaan notifikasi pribadi** dan **Hanya nama aplikasi saat ponsel terkunci**.

### Perilaku notifikasi lainnya {#notifications-other}

- Toast, yaitu pesan singkat yang ditampilkan aplikasi di bagian bawah layar, tetap dibacakan saat mode Jangan Ganggu aktif. Setelan Jangan Ganggu hanya berlaku untuk notifikasi.
- Di layar kunci dan panel notifikasi, SandFly tidak mengucapkan "diciutkan" untuk setiap notifikasi. SandFly mengucapkan "diluaskan" saat Anda meluaskan notifikasi.
- Di ponsel yang menampilkan panel notifikasi saat notifikasi mengambang (heads-up) masuk, seperti ponsel Vivo, SandFly tidak mengucapkan nama panel dalam 3 detik setelah notifikasi masuk, kecuali Anda menyentuh layar, menekan tombol, menggunakan layar braille, atau memberikan perintah suara setelahnya.
- Anda dapat menutup pemberitahuan SandFly, seperti pemberitahuan pembaruan atau perubahan gestur, juga di layar kunci. Notifikasi untuk menjeda SandFly dan untuk mengunduh model di perangkat tetap ada selama proses tersebut berlangsung.
- Di jam tangan Samsung, SandFly membacakan kartu notifikasi sebagai aplikasi dan judul, lalu pesan, lalu waktu, termasuk ketika label kartu tidak menyertakan pesannya. Lihat [](#devices).
- Untuk menerjemahkan notifikasi, lihat [](#translation).
