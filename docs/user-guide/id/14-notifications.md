## Notifikasi {#notifications}

SandFly membacakan notifikasi baru saat masuk. SandFly juga dapat membacakan notifikasi di panel notifikasi saat diminta atau pada waktu yang ditetapkan. **Notifikasi** di setelan SandFly mengatur notifikasi mana yang dibacakan SandFly, apa yang diucapkan, dengan suara apa, dan kapan. Layar ini juga mengaktifkan daftar notifikasi SandFly, yang dapat menggantikan panel notifikasi sistem.

Panggilan masuk selalu dibacakan, apa pun setelan notifikasinya.

### Akses notifikasi {#notifications-access}

Beberapa fitur memerlukan akses notifikasi untuk SandFly. Akses ini diberikan melalui setelan Android. Fitur tersebut adalah:

- Membacakan notifikasi yang tidak diumumkan Android ke pembaca layar. Lihat [](#notifications-not-announced).
- Membacakan notifikasi pribadi secara penuh saat ponsel terkunci.
- **Baca notifikasi yang diizinkan oleh mode Jangan Ganggu**. Tanpa akses notifikasi, SandFly tidak membacakan notifikasi apa pun saat mode Jangan Ganggu aktif dengan pilihan ini.
- Menghentikan pembacaan notifikasi saat notifikasi itu ditutup atau dihapus.
- Ringkasan notifikasi di panel notifikasi.
- Daftar notifikasi SandFly.
- Mengakhiri panggilan yang sedang berlangsung di aplikasi seperti WhatsApp dengan perintah **Akhiri panggilan**. Lihat [](#commands-calls).

Untuk memberikan akses notifikasi, pilih **Aktifkan akses notifikasi** di layar **Kapan dibaca**, **Berhenti membaca**, atau **Ringkasan dan waktu**, lalu aktifkan SandFly di daftar tersebut. Item ini hanya ditampilkan saat SandFly tidak memiliki akses notifikasi. SandFly hanya terhubung ke notifikasi saat SandFly aktif.

### Mengaktifkan atau menonaktifkan pembacaan notifikasi {#notifications-speech-on-off}

**Notifikasi** > **Ucapkan notifikasi** mengaktifkan atau menonaktifkan pembacaan notifikasi baru. Setelan ini aktif secara default. Panggilan masuk tetap dibacakan meskipun setelan ini nonaktif.

Anda juga dapat mengaktifkan atau menonaktifkan pembacaan notifikasi dengan:

- Kartu setelan cepat **Pembacaan notifikasi**.
- Perintah **Pembacaan notifikasi aktif atau nonaktif**, yang dapat Anda tetapkan ke gerakan atau pintasan keyboard. SandFly mengucapkan "Pembacaan notifikasi diaktifkan" atau "Pembacaan notifikasi dinonaktifkan".
- Aplikasi otomatisasi. Lihat [](#notifications-automation).

### Perintah untuk notifikasi {#notifications-commands}

Kategori **Notifikasi** di **Perintah** berisi:

- **Baca notifikasi**: membacakan notifikasi di panel notifikasi, sesuai setelan **Ringkasan dan waktu**. Lihat [](#notifications-summary).
- **Baca lagi notifikasi terakhir**: mengucapkan kembali notifikasi terakhir yang dibacakan SandFly. SandFly mengucapkan "Tidak ada notifikasi yang dibacakan" jika belum ada.
- **Hentikan pembacaan notifikasi**: menghentikan notifikasi yang sedang dibacakan serta notifikasi yang masih mengantre untuk dibacakan.
- **Pembacaan notifikasi aktif atau nonaktif**
- **Pengumuman waktu aktif atau nonaktif**: menyetel **Ucapkan waktunya setiap** ke **Nonaktif**, atau mengembalikannya ke interval sebelumnya. Jika sebelumnya belum ada interval, perintah ini menyetelnya ke 1 jam. SandFly mengucapkan "Pengumuman waktu aktif" atau "Pengumuman waktu nonaktif".

Anda dapat menetapkan perintah-perintah ini ke gerakan, pintasan keyboard, dan input lainnya. Lihat [](#commands).

Jika Anda memakai gerakan, pintasan keyboard, atau tombol saat notifikasi dibacakan, pembacaan berhenti. SandFly lalu mengucapkan tindakan yang dilakukan gerakan atau tombol tersebut.

### Mengubah apa yang diucapkan {#notifications-reading}

**Notifikasi** > **Pembacaan notifikasi** mengatur apa yang diucapkan SandFly untuk notifikasi dan bagaimana cara membacakannya. Di bawah **Apa yang dikatakan**:

- **Format ucapan**: teks yang diucapkan SandFly untuk setiap notifikasi. Teks ini tersusun dari placeholder: `{app}`, `{title}`, `{text}`, `{content}` (keseluruhan teks), `{bigtext}`, `{subtext}`, `{ticker}`, `{category}`, dan `{time}`. Default-nya adalah `{category}, {title}, {text}`.
- **Pembacaan notifikasi pribadi**: teks yang diucapkan SandFly sebagai pengganti isi notifikasi pribadi, dengan placeholder `{app}`, `{category}`, dan `{time}`. Default-nya adalah `{app}`.

Di bawah **Teks**:

- **Lewati emoji**: emoji dalam notifikasi tidak dibacakan. Nonaktif secara default.
- **Baca kata berhuruf kapital sebagai kata biasa**: kata yang ditulis dengan huruf kapital semua dibacakan sebagai kata. Tanpa setelan ini, beberapa mesin ucapan mengejanya huruf demi huruf. Nonaktif secara default.
- **Bacakan angka panjang per digit**: angka seperti kode dan nomor telepon dibacakan digit demi digit. Nonaktif secara default. **Jumlah digit minimum dalam angka panjang** mengatur berapa digit minimal sebuah angka agar dibacakan dengan cara ini, dari 3 hingga 10. Default-nya 5.
- **Tautan**: **Bacakan seluruh tautan** (default), **Bacakan situsnya saja**, **Ucapkan teks pengganti tautan**, atau **Lewati**. **Teks pengganti tautan** adalah teks untuk pilihan **Ucapkan teks pengganti tautan**.
- **Lewati notifikasi tanpa teks**: notifikasi tanpa teks tidak dibacakan. Nonaktif secara default.

Di bawah **Panjang**, semuanya **Nonaktif** secara default:

- **Jumlah kata maksimum**: membacakan hingga 5, 10, 15, 20, 30, 50, atau 100 kata.
- **Jumlah kalimat maksimum**: membacakan hingga 1, 2, 3, atau 5 kalimat.
- **Jumlah karakter maksimum**: membacakan hingga 50, 100, 200, 300, 500, atau 1000 karakter.
- **Berhenti membaca setelah**: menghentikan notifikasi setelah 5 hingga 60 detik.

Di bawah **Waktu pembacaan**:

- **Tunggu sebelum membaca**: menunggu hingga 10 detik sebelum membacakan notifikasi. **Nonaktif** secara default.
- **Saat notifikasi lain dibaca**: mengatur apa yang terjadi jika notifikasi masuk saat notifikasi lain sedang dibacakan. Pilihannya **Baca setelahnya** (default), **Sela notifikasi itu**, atau **Jangan bacakan yang baru**.
- **Baca lagi sampai layar menyala**: notifikasi yang masuk saat layar mati diucapkan lagi setiap 1 hingga 30 menit, sampai layar menyala. **Nonaktif** secara default.

Untuk setelan suara di layar ini, lihat [](#notifications-voice).

### Suara notifikasi {#notifications-voice}

Di bawah **Notifikasi** > **Pembacaan notifikasi** > **Suara**:

- **Suara**: membacakan notifikasi dengan salah satu profil suara Anda beserta semua setelannya, seperti mesin ucapan lain, kecepatan, dan nada suara. Default-nya adalah **Suara SandFly**.
- **Profil suara**: membuka daftar profil suara, tempat Anda membuat dan mengubah profil. Lihat [](#speech-voice-profiles).
- **Bacakan bersamaan dengan ucapan SandFly, dengan suara berbeda**: lihat di bawah. Nonaktif secara default.
- **Deteksi bahasa**: mengucapkan setiap notifikasi dalam bahasa penulisannya. Setelan ini memerlukan **Ucapan** > **Ganti bahasa secara otomatis**. Lihat [](#speech-languages). Nonaktif secara default.
- **Suara sebelum notifikasi**: memutar suara sebelum setiap notifikasi. Nonaktif secara default.

Aplikasi dan aturan dapat memiliki suaranya sendiri. Lihat [](#notifications-apps) dan [](#notifications-rules).

Cara notifikasi dengan suara berbeda dibacakan:

- Notifikasi yang memakai profil suara yang sama dengan suara SandFly saat ini dibacakan setelah ucapan yang sedang berlangsung, dalam antrean ucapan yang sama.
- Notifikasi dengan suara berbeda juga dibacakan setelah ucapan yang sedang berlangsung, agar dua suara tidak berbicara bersamaan. Notifikasi itu menunggu paling lama 45 detik. Jika **Bacakan bersamaan dengan ucapan SandFly, dengan suara berbeda** aktif, notifikasi langsung dibacakan, menimpa ucapan yang sedang berlangsung.
- **Bacakan bersamaan dengan ucapan SandFly, dengan suara berbeda** tidak berlaku saat **Ucapkan hanya fokus yang dipindahkan pengguna** berlaku pada aplikasi yang sedang tampil, atau saat asisten atau panggilan memutar audio. Lihat [](#verbosity-own-speech).
- Saat **Senyap saat merekam**, di **Ucapan**, berlaku, tidak ada notifikasi yang dibacakan. Lihat [](#speech).
- Jika beberapa notifikasi dengan suara berbeda masuk bersamaan, setiap notifikasi menunggu notifikasi sebelumnya selesai.

Jika mesin ucapan suatu profil suara gagal, SandFly membacakan notifikasi dengan suaranya sendiri.

### Aplikasi {#notifications-apps}

**Notifikasi** > **Aplikasi** mengatur cara pembacaan notifikasi setiap aplikasi.

- **Aplikasi lain**: cara SandFly membacakan notifikasi dari aplikasi yang tidak memiliki setelan khusus: **Baca** (default), **Hanya nama aplikasi**, atau **Jangan baca**.
- **Aplikasi**: aplikasi di peluncur, aplikasi yang memiliki setelan, dan aplikasi di riwayat, diurutkan menurut nama. Setiap aplikasi menampilkan setelannya.

Untuk satu aplikasi:

- **Notifikasi**: **Seperti aplikasi lainnya** (default), **Baca**, **Hanya nama aplikasi**, **Baca dulu**, atau **Jangan baca**. **Hanya nama aplikasi** tidak pernah membacakan isi notifikasi. Notifikasi dari aplikasi dengan setelan **Baca dulu** memotong notifikasi yang sedang dibacakan.
- **Nama yang diucapkan**: nama yang diucapkan SandFly untuk aplikasi tersebut.
- **Jeda antara notifikasi dari aplikasi yang sama**: jeda tersingkat antara dua notifikasi aplikasi ini yang dibacakan. **Seperti aplikasi lainnya** menggunakan setelan di **Pengulangan**.
- **Format ucapan**: format ucapan khusus untuk aplikasi tersebut. Lihat [](#notifications-reading).
- **Suara**: **Seperti aplikasi lainnya**, **Suara SandFly**, atau profil suara, seperti profil dengan mesin ucapan lain.
- **Setel ulang**: menghapus setelan khusus aplikasi tersebut. Hanya ditampilkan untuk aplikasi yang memiliki setelan.

### Kata dan pengganti {#notifications-words}

**Notifikasi** > **Kata dan pengganti** memiliki empat daftar. Setiap item dapat berupa ekspresi reguler.

- **Hanya bacakan notifikasi yang berisi**: jika daftar ini berisi kata, hanya notifikasi yang memuat salah satu kata itu yang dibacakan.
- **Jangan bacakan notifikasi yang berisi**: notifikasi yang memuat salah satu kata ini tidak dibacakan.
- **Bacakan hanya nama aplikasi untuk notifikasi yang berisi**: untuk notifikasi yang memuat salah satu kata ini, hanya nama aplikasi yang diucapkan.
- **Ucapkan secara berbeda**: teks yang diucapkan SandFly secara berbeda, seperti "lol" sebagai "tertawa". Setiap pengganti memiliki **Teks**, **Ucapkan sebagai**, **Hanya kata utuh** (aktif secara default), dan **Ekspresi reguler**.

Untuk menambahkan kata, pilih **Tambahkan kata** di bawah daftarnya, masukkan teks, lalu pilih **Oke**. Untuk menambahkan pengganti, pilih **Tambahkan pengganti**. Pilih item untuk **Edit** atau **Hapus**.

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
- **Saat ponsel terkunci**: aktif secara default.
- **Headphone**: **Baca dengan atau tanpa headphone** (default), **Hanya dengan headphone**, atau **Hanya tanpa headphone**.
- **Saat media diputar**: **Baca** (default), **Jeda media saat membaca**, atau **Jangan baca**.
- **Jangan Ganggu**: **Jangan baca apa pun**, **Baca notifikasi yang diizinkan oleh mode Jangan Ganggu** (default), atau **Baca semuanya**. **Baca notifikasi yang diizinkan oleh mode Jangan Ganggu** memerlukan akses notifikasi.
- **Saat layar disentuh**: **Baca segera**, **Baca ketika sentuhan berakhir** (default), atau **Jangan baca**. Dengan **Baca ketika sentuhan berakhir**, notifikasi yang masuk saat Anda menyentuh layar dibacakan setengah detik setelah sentuhan berakhir. Paling lambat, notifikasi dibacakan 10 detik setelah notifikasi pertama masuk.
- **Selama panggilan**: membacakan notifikasi selama panggilan. Nonaktif secara default.
- **Tidak dalam mode senyap**: nonaktif secara default.
- **Tidak dalam mode getar**: nonaktif secara default.
- **Tidak dalam mode mobil**: misalnya saat memakai Android Auto, yang membacakan notifikasi sendiri. Nonaktif secara default.
- **Jam tenang**: tidak ada notifikasi yang dibacakan antara **Mulai** dan **Akhir**. Nonaktif secara default. Waktu default-nya adalah 22:00 dan 07:00.

### Aturan {#notifications-rules}

**Notifikasi** > **Aturan** mengubah cara pembacaan notifikasi pada kondisi tertentu. Sebuah aturan memiliki kondisi dan tindakan. Saat kondisinya terpenuhi, tindakannya diterapkan ke notifikasi tersebut. Aturan tanpa kondisi berlaku untuk setiap notifikasi. Suara suatu aturan menggantikan suara aplikasi.

Untuk menambahkan aturan:

1. Pilih **Tambahkan aturan**, masukkan nama, lalu pilih **Oke**. Atau pilih **Tambahkan aturan dari templat** dan pilih sebuah templat.
2. Di bawah **Ketika**, pilih **Tambahkan kondisi** lalu pilih kondisi.
3. Di bawah **Lalu**, pilih **Tambahkan tindakan** lalu pilih tindakan.

Sebuah aturan memiliki:

- **Nama**
- **Diaktifkan**
- **Kondisi**: **Semua kondisi harus dipenuhi**, atau **Setidaknya satu kondisi harus dipenuhi**.
- **Ketika**: daftar kondisi. Pilih kondisi untuk **Edit**, **Balik kondisi**, atau **Hapus**. **Balik kondisi** membuat kondisi terpenuhi saat kebalikannya terjadi, misalnya "Layar mati" sebagai ganti "Layar menyala".
- **Lalu**: daftar tindakan. Pilih tindakan untuk menghapusnya.
- **Hapus aturan**

Kondisinya adalah **Perangkat Bluetooth** (perangkat yang Anda pilih atau **Perangkat apa pun**), **Headphone berkabel**, **Jaringan Wi-Fi** (berdasarkan nama), **Waktu dalam sehari** (dengan **Dari**, **Sampai**, dan **Hari**), **Layar menyala**, **Ponsel terkunci**, **Notifikasi dari aplikasi**, **Aplikasi di latar depan**, **Notifikasi berisi** (teks atau ekspresi reguler), **Level baterai di bawah** (persentase), **Mengisi daya**, **Lanskap**, **Mode senyap atau getar**, dan **Pemutaran media**. **Perangkat Bluetooth** memerlukan izin Perangkat di sekitar pada Android 12 ke atas, dan **Jaringan Wi-Fi** memerlukan izin lokasi. SandFly meminta izin tersebut saat Anda menambahkan kondisinya.

Tindakannya adalah: jangan bacakan, bacakan nama aplikasi saja, bacakan seluruhnya meskipun pribadi, gunakan format ucapan, bacakan hingga sejumlah kata, bacakan tanpa batas panjang, pertahankan emoji, satukan angka, bacakan lebih dulu, bacakan bahkan saat jam tenang dan waktu lain ketika notifikasi tidak dibacakan, dan bacakan dengan profil suara.

Templat yang tersedia adalah **Hanya dengan headphone**, **Senyap di malam hari** (22:00 hingga 07:00), **Hanya dengan layar mati**, **Hanya nama aplikasi saat ponsel terkunci**, dan **Singkat saat tidak mengisi daya** (hingga 15 kata).

### Pengulangan {#notifications-repeats}

**Notifikasi** > **Pengulangan** menangani notifikasi yang muncul berulang dan aplikasi yang mengirim banyak notifikasi:

- **Lewati notifikasi yang sama selama**: jika teks notifikasi sama dengan notifikasi yang sudah dibacakan dalam rentang waktu ini, notifikasi itu tidak dibacakan lagi. Pilihannya adalah **Nonaktif** (default), dari 10 detik hingga 1 jam, dan **Hingga SandFly dimulai ulang**.
- **Angka dianggap sama**: notifikasi yang hanya berbeda pada angkanya, seperti persentase unduhan, dianggap sama. Nonaktif secara default.
- **Jeda antara notifikasi dari aplikasi yang sama**: jeda tersingkat antara dua notifikasi dari aplikasi yang sama yang dibacakan, dari 5 detik hingga 15 menit. **Nonaktif** secara default. Setiap aplikasi dapat memiliki setelannya sendiri. Lihat [](#notifications-apps).
- **Hilangkan awal yang berulang**: menghilangkan awalan yang berulang pada notifikasi suatu aplikasi, seperti nama grup dalam chat. Nonaktif secara default. **Ingat awal selama** mengatur berapa lama SandFly mengingat awalan tersebut, dari 30 detik hingga 15 menit. Default-nya 1 menit.

SandFly juga hanya membacakan notifikasi satu kali jika aplikasi mengirimnya lagi dengan judul dan teks yang sama dalam 5 detik, atau mengirim teks yang sama sebagai ringkasan grup.

### Berhenti membaca {#notifications-stop}

**Notifikasi** > **Berhenti membaca** mengatur hal yang menghentikan pembacaan notifikasi:

- **Kocok untuk berhenti**: goyangkan ponsel. **Kekuatan guncangan**: **Lembut**, **Sedang** (default), atau **Kuat**.
- **Lambaikan tangan untuk berhenti**: gerakkan tangan di atas sensor jarak. **Mode saku** aktif secara default. Jika sensor sudah tertutup saat pembacaan dimulai, misalnya karena ponsel ada di dalam saku, modus ini mencegah sensor menghentikan pembacaan.
- **Tekan kedua tombol volume**
- **Berhenti saat ditutup**: menghentikan notifikasi saat Anda menutupnya. **Juga ketika notifikasi ditutup di jam tangan** aktif secara default, dan juga menghentikan notifikasi saat Anda menutupnya di jam tangan yang terhubung.
- **Berhenti saat aplikasi menghapusnya**

Semua kecuali **Mode saku** dan **Juga ketika notifikasi ditutup di jam tangan** nonaktif secara default. **Berhenti saat ditutup** dan **Berhenti saat aplikasi menghapusnya** memerlukan akses notifikasi. Setelan ini juga berlaku saat ringkasan sedang dibacakan.

### Ringkasan dan waktu {#notifications-summary}

**Notifikasi** > **Ringkasan dan waktu** membacakan notifikasi di panel notifikasi dan mengucapkan waktu secara berkala. Di bawah **Ringkasan**:

- **Aktifkan akses notifikasi**: ringkasan memerlukan akses ini. Hanya ditampilkan saat SandFly tidak memiliki akses notifikasi.
- **Baca notifikasi sekarang**: membacakan notifikasi di panel notifikasi. SandFly harus aktif.
- **Baca notifikasi setiap hari**: membacakannya setiap hari pada **Waktu**, 08:00 secara default.
- **Yang terbaru dulu**: membacakan notifikasi terbaru lebih dulu. Nonaktif secara default.
- **Nama Anda**: nama untuk sapaan di awal, seperti "Selamat pagi. Sam."
- **Jeda di antara notifikasi**: dari **Nonaktif** (default) hingga 5 detik.

Ringkasan dimulai dengan sapaan dan jumlah notifikasi, seperti "Anda memiliki 5 notifikasi.". Setelah itu, setiap notifikasi dibacakan dengan urutan aplikasi, judul, dan teks. Ringkasan grup dan notifikasi yang sedang berlangsung tidak disertakan. Setelan **Aplikasi**, **Kata dan pengganti**, **Jenis notifikasi**, dan **Aturan** juga berlaku. Notifikasi yang dikecualikan oleh setelan tersebut tidak dihitung.

Di bawah **Pengumuman waktu**:

- **Ucapkan waktunya setiap**: **Nonaktif** (default), 15 menit, 30 menit, 1 jam, atau 3 jam.
- **Aktifkan Alarm dan pengingat**: membuka setelan Android yang memungkinkan SandFly mengucapkan waktu tepat pada waktunya. Tanpa izin ini, Android dapat menunda pengumuman. Item ini hanya ditampilkan saat **Ucapkan waktunya setiap** tidak disetel ke **Nonaktif** dan setelan Android tersebut nonaktif. SandFly juga membuka setelan Android ini saat Anda memilih interval untuk **Ucapkan waktunya setiap** atau mengaktifkan **Baca notifikasi setiap hari**, kecuali jika setelan tersebut sudah aktif.
- **Format waktu**: teks yang diucapkan SandFly, dengan placeholder `{time}`, `{date}`, dan `{battery}` (persentase baterai). Default-nya adalah "Sekarang pukul {time}".
- **Suara**: **Suara notifikasi** (default), **Suara SandFly**, atau profil suara.

Ringkasan dibacakan dengan suara notifikasi.

Agar tingkat baterai diucapkan pada interval tertentu, lihat **Panjang respons** > **Tingkat baterai** di [](#verbosity-announcements).

### Riwayat notifikasi {#notifications-history}

**Notifikasi** > **Riwayat notifikasi** mencantumkan 100 notifikasi terakhir yang masuk sejak SandFly dimulai. Untuk setiap notifikasi, daftar ini menampilkan waktu masuknya, apakah notifikasi dibacakan, alasan jika tidak dibacakan (seperti "Tidak dibacakan: Jangan Ganggu"), dan apa yang diucapkan SandFly. Riwayat hanya disimpan di memori.

- **Simpan riwayat notifikasi**: aktif secara default.
- **Hapus riwayat**

Pilih notifikasi untuk tindakan berikut. Tindakan ini ditampilkan dengan nama aplikasinya, seperti Chat:

- **Jangan baca Chat**: menyetel aplikasi ke **Jangan baca**.
- **Baca hanya nama Chat**: menyetel aplikasi ke **Hanya nama aplikasi**.
- **Baca Chat terlebih dahulu**: menyetel aplikasi ke **Baca dulu**.
- **Jangan bacakan notifikasi seperti ini**: menambahkan ekspresi reguler yang cocok dengan notifikasi serupa ke **Jangan bacakan notifikasi yang berisi**.
- **Salin**: menyalin judul dan teksnya.

### Menguji setelan {#notifications-test}

**Notifikasi** > **Kirim notifikasi uji** menampilkan notifikasi uji SandFly setelah 5 detik. Dengan begitu, Anda sempat mematikan layar atau membuka aplikasi lain terlebih dahulu. SandFly membacakannya sesuai setelan notifikasi Anda. SandFly memerlukan izin untuk menampilkan notifikasi.

### Otomatisasi {#notifications-automation}

Aplikasi otomatisasi, seperti Tasker atau MacroDroid, dapat mengontrol pembacaan notifikasi melalui siaran (broadcast) ke SandFly. Aktifkan **Notifikasi** > **Izinkan aplikasi otomatisasi** terlebih dahulu. Setelan ini nonaktif secara default karena aplikasi mana pun dapat mengirim siaran. **Notifikasi** > **Otomatisasi** mencantumkan tindakan yang tersedia, masing-masing dengan tindakan siarannya. Tindakan siaran terdiri dari nama paket SandFly diikuti tindakan seperti `.action.READ_NOTIFICATIONS`. Pilih tindakan untuk menyalin tindakan siarannya:

- **Aktifkan pembacaan notifikasi**
- **Nonaktifkan pembacaan notifikasi**
- **Aktifkan atau nonaktifkan pembacaan notifikasi**
- **Hentikan pembacaan notifikasi**
- **Baca notifikasi**

### Daftar notifikasi SandFly {#notifications-list}

Jika **Notifikasi** > **Gunakan daftar notifikasi SandFly** aktif, perintah **Notifikasi** membuka daftar milik SandFly alih-alih panel notifikasi sistem. Di beberapa ponsel, seperti ponsel Samsung yang memiliki banyak notifikasi, panel sistem terlalu sering digambar ulang sehingga navigasi di dalamnya lambat. Setelan ini nonaktif secara default dan memerlukan akses notifikasi.

Di dalam daftar:

- Ketuk dua kali notifikasi untuk membukanya.
- Grup ditampilkan sebagai satu item. Ketuk dua kali grup untuk menampilkan notifikasi di dalamnya.
- Setiap notifikasi memiliki tindakannya sendiri. Jika aplikasi menyediakannya, ada juga kolom balas dengan tombol **Kirim**.
- Notifikasi media menampilkan **Diputar** atau **Dijeda**, dengan **Sebelumnya**, **Putar** atau **Jeda**, dan **Berikutnya**.
- Notifikasi tanpa teks, seperti notifikasi cuaca dengan tata letak sendiri, dibacakan dengan teks dari tata letaknya, atau jika tidak ada, dengan teks sub, teks info, atau teks ringkasannya.
- Teks yang lebih panjang dari sekitar 160 karakter dipotong. Tindakan **Tampilkan semua** membacakan seluruh teks, dan **Tampilkan lebih sedikit** memotongnya kembali.
- **Tutup** menghapus satu notifikasi, dan **Hapus semua** menghapus semua notifikasi yang dapat dihapus.
- **Setelan cepat** membuka setelan cepat.

Saat ponsel terkunci, perintah **Notifikasi** membuka panel sistem. Panel sistem menyembunyikan hal-hal yang Anda pilih untuk disembunyikan di layar kunci. Perintah **Panel notifikasi sistem** selalu membuka panel sistem. Daftar SandFly terbuka sebagai layar tersendiri, sehingga aplikasi yang sedang tampil dijeda. Beberapa aplikasi menghentikan proses yang sedang berjalan saat dijeda. Contohnya, penyandingan debug nirkabel di Setelan gagal jika kode penyandingan dimasukkan dari daftar SandFly. Gunakan **Panel notifikasi sistem** untuk keperluan tersebut.

Setelan untuk daftar ini:

- **Urutkan notifikasi dari yang terbaru, tanpa grup**: setiap notifikasi menjadi item tersendiri, yang terbaru lebih dulu, dibacakan sebagai aplikasi, waktu, judul, dan teks. Nonaktif secara default.
- **Kecualikan pemutar media dari daftar notifikasi**: mengecualikan notifikasi pemutar media. Notifikasi ini juga tidak dihitung dalam jumlah notifikasi yang dibacakan saat layar menyala. Nonaktif secara default.
- **Detail notifikasi**: bagian notifikasi yang dibacakan di daftar, beserta urutannya: **Judul**, **Teks**, **Nama aplikasi**, **Waktu**, dan **Diputar atau dijeda**. Setiap bagian berupa kotak centang yang dicentang secara default, dan memiliki tindakan **Pindahkan ke atas** dan **Pindahkan ke bawah**.

### Notifikasi yang tidak diumumkan Android {#notifications-not-announced}

Android hanya memberi tahu pembaca layar tentang notifikasi baru jika notifikasi tersebut berbunyi atau memberi peringatan. Notifikasi berikut tidak diumumkan: notifikasi yang ditahan mode Jangan Ganggu, dan pembaruan yang dikirim aplikasi tanpa memberi peringatan ulang, seperti aplikasi bel pintu yang mengganti peringatan gerakan terakhirnya. Jika SandFly memiliki akses notifikasi, SandFly tetap menerima notifikasi tersebut dan membacakannya sesuai setelan notifikasi. Jadi, **Kapan dibaca** > **Jangan Ganggu** juga berlaku untuk notifikasi ini. Notifikasi progres dan notifikasi yang sedang berlangsung dikecualikan.

Saat ponsel terkunci, Android hanya memberikan versi publik dari notifikasi pribadi kepada pembaca layar, padahal banyak aplikasi tidak menyediakan versi publik. Dengan akses notifikasi, SandFly membacakan seluruh notifikasi sesuai setelan notifikasi Anda, seperti **Pembacaan notifikasi pribadi** dan **Hanya nama aplikasi saat ponsel terkunci**.

### Perilaku notifikasi lainnya {#notifications-other}

- Toast, yaitu pesan singkat yang ditampilkan aplikasi di bagian bawah layar, tetap dibacakan saat mode Jangan Ganggu aktif. Setelan Jangan Ganggu hanya berlaku untuk notifikasi.
- Di layar kunci dan panel notifikasi, SandFly tidak mengucapkan "diciutkan" untuk setiap notifikasi. SandFly mengucapkan "diluaskan" saat Anda meluaskan notifikasi.
- Di ponsel yang menampilkan panel notifikasi saat notifikasi mengambang (heads-up) masuk, seperti ponsel Vivo, SandFly tidak mengucapkan nama panel dalam 3 detik setelah notifikasi masuk. Pengecualiannya, jika dalam waktu itu Anda menyentuh layar, menekan tombol, menggunakan penampil braille, atau memberikan perintah suara.
- Anda dapat menutup notifikasi SandFly, seperti notifikasi tentang pembaruan atau perubahan gerakan, bahkan di layar kunci. Notifikasi saat SandFly dijeda dan saat mengunduh model di perangkat tetap ada selama proses tersebut berlangsung.
- Di jam tangan Samsung, SandFly membacakan kartu notifikasi dengan urutan aplikasi dan judul, lalu pesan, lalu waktu. Ini juga berlaku jika label kartu tidak menyertakan pesannya. Lihat [](#devices).
- Untuk menerjemahkan notifikasi, lihat [](#translation).
