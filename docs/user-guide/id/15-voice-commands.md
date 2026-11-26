## Perintah suara {#voice-commands}

Perintah suara menjalankan perintah SandFly dari ucapan: memindahkan fokus, membaca, mengedit teks, membuka aplikasi, mengaktifkan item di layar, menelepon kontak, mengirim pesan, atau mendeskripsikan layar. Satu perintah suara dapat berisi beberapa perintah secara berurutan. Di setelan SandFly, layar **Perintah suara** mengaktifkan AI untuk perintah yang tidak dipahami SandFly, mengatur batas waktu pengeditan suara, dan menyimpan perintah suara yang Anda buat. Layar **Perintah suara** tidak ditampilkan di jam tangan.

Untuk daftar lengkap frasa, lihat [](#appendix-voice-commands).

### Memulai perintah suara {#voice-commands-start}

1. Gunakan perintah **Mulai perintah suara**. Pada tata letak gerakan SandFly, geser ke kanan lalu ke atas. Lihat [](#appendix-gestures).
2. Ucapkan perintah.

SandFly menampilkan dialog **Gunakan perintah suara untuk mengontrol SandFly** yang berisi contoh hal yang dapat diucapkan. Pilih **Gunakan perintah suara** untuk mulai merekam. Agar dialog ini tidak ditampilkan lagi, hapus centang **Selalu tampilkan pesan ini**.

Perintah suara memerlukan izin mikrofon. SandFly merekam menggunakan pengenal ucapan yang dipilih di **Mengetik** > **Dikte**. Lihat [](#typing).

Perintah suara tidak tersedia saat layar terkunci atau selama penyiapan perangkat. Jika layar terkunci, SandFly memberi tahu hal itu dan menyebutkan gerakan untuk membuka menu SandFly.

Untuk melihat daftar perintah, ucapkan "Bantuan" atau "Apa yang bisa saya ucapkan". SandFly membuka **Ringkasan perintah suara**.

Perintah **Mulai perintah suara** juga ada di kategori **Tindakan sistem**, sehingga pintasan keyboard atau item menu dapat menjalankannya. Untuk menambahkannya ke menu SandFly, lihat [](#commands-edit-menu).

### Apa yang dapat Anda ucapkan {#voice-commands-phrases}

SandFly mencocokkan ucapan Anda dengan urutan berikut:

1. Perintah suara kustom, berdasarkan frasanya. Lihat [](#voice-commands-custom).
2. Nama perintah apa pun, sebagaimana ditampilkan SandFly di layar **Perintah**, seperti "Baca dari atas", "Deskripsikan gambar", atau "Judul berikutnya". Nama makro, menu kustom, dan perintah skrip Anda juga termasuk. Nama aplikasi, seperti "Gmail", membuka aplikasi tersebut, kecuali jika ada perintah dengan nama itu. Lihat [](#commands-list).
3. Pola frasa, seperti "buka Chrome", "ketuk Setelan", "telepon Sam", atau "ketik halo". Lihat [](#voice-commands-sandfly).
4. Salah satu perintah suara bawaan, seperti "Cari simpan", "Navigasi kata", "Bicara lebih cepat", atau "Salin". Lihat [](#appendix-voice-commands).
5. Layanan AI, jika **Gunakan AI untuk memahami perintah suara** aktif. Lihat [](#voice-commands-ai).

Huruf besar-kecil dan tanda baca yang ditambahkan pengenal ucapan tidak berpengaruh. Kata-kata seperti "tolong", "bisakah kamu", "bisakah Anda", "maukah kamu", "maukah Anda", "saya ingin", "saya mau", dan "sekarang" di awal, serta "tolong", "untuk saya", dan "sekarang" di akhir, diabaikan.

Jika SandFly tidak memahami perintah, SandFly mengucapkan "Tidak dapat mengenali perintah ini" beserta cara melihat daftar perintah.

#### Aplikasi, item, dan teks {#voice-commands-sandfly}

- **Membuka aplikasi**: "buka", "jalankan", "mulai", atau "beralih ke", diikuti nama aplikasi. SandFly memilih aplikasi yang namanya sama persis. Jika tidak ada, SandFly memilih aplikasi yang namanya diawali teks itu, lalu aplikasi yang namanya memuat salah satu kata dari teks itu. Jika tidak ada aplikasi yang cocok, atau beberapa aplikasi sama cocoknya, SandFly menjalankan perintah dengan nama tersebut, seperti "buka notifikasi". Jika perintah seperti itu juga tidak ada, perintah suara tidak dipahami.
- **Mengaktifkan item di layar**: "ketuk", "ketuk dua kali", "klik", "tekan", "aktifkan", atau "pilih", diikuti teks item. SandFly mengaktifkan item pertama yang seluruh teksnya sama. Jika tidak ada, SandFly mengaktifkan item pertama yang teksnya memuat teks tersebut. Jika item seperti itu juga tidak ada, SandFly mengucapkan "Tidak ditemukan" diikuti teks tersebut. "Tekan enter" menjalankan **Baris baru** sebagai gantinya. "Pilih" diikuti teks yang ada di kolom teks yang difokuskan memilih teks tersebut sebagai gantinya. Lihat [](#voice-commands-text).
- **Ketik teks**: "ketik", "tulis", atau "masukkan", diikuti teksnya. SandFly mengetikkannya di kolom teks yang difokuskan dan mengucapkan teks yang diketik, kecuali di kolom sandi.
- **Menjalankan perintah**: "jalankan" atau "lakukan" diikuti nama perintah, atau "jalankan makro" diikuti nama makro, seperti "jalankan makro pagi".
- **Tunggu**: "tunggu", "tunggu sebentar", atau "tunggu" diikuti jumlah detik dan "detik", hingga 60 detik. Gunakan di antara perintah dalam satu rangkaian.

#### Teks di kolom teks {#voice-commands-text}

Frasa berikut berlaku pada teks di kolom teks yang memiliki fokus input.

- **Memindahkan kursor**: angka dan "karakter", "huruf", "kata", "kalimat", "baris", atau "paragraf", diikuti "berikutnya", "selanjutnya", "sebelumnya", "terakhir", "ke kanan", atau "ke kiri", atau didahului "maju" atau "mundur", seperti "3 kata berikutnya", "lima kata ke kiri", atau "mundur dua baris". "Pindahkan kursor ke", "pindah ke", atau "ke" dapat ditambahkan di depannya. SandFly memindahkan kursor dan mengucapkan teks yang dilewati.
- **Awal dan akhir**: "awal" atau "akhir", diikuti "kata", "kalimat", "baris", "paragraf", atau "teks", seperti "akhir baris" atau "ke awal kalimat". "Ke awal" dan "ke akhir" adalah awal dan akhir teks. SandFly memindahkan kursor ke sana dan mengucapkan teks yang dilewati. Jika kursor sudah berada di awal atau akhir, kursor berpindah ke awal unit sebelumnya atau ke akhir unit berikutnya.
- **Memilih, menyalin, memotong, atau menghapus**: "pilih", "sorot", "blok", "salin", "copy", "potong", "cut", "hapus", atau "buang", diikuti salah satu frasa di atas, seperti "hapus lima kata berikutnya", "salin 2 kalimat sebelumnya", "pilih baris berikutnya", "hapus kata terakhir", atau "hapus ke akhir baris". SandFly mengucapkan "Teks yang dipilih:" diikuti teks yang dipilih, teks yang disalin diikuti "disalin", teks yang dipotong diikuti "dipotong", atau teks yang dihapus diikuti "dihapus". Menyalin tidak memindahkan kursor.
- **Memilih, menyalin, memotong, atau menghapus teks tertentu**: "pilih", "salin", "potong", atau "hapus", diikuti teks di kolom, seperti "pilih buaya". SandFly mencari teks yang paling dekat dengan kursor, dengan mengabaikan huruf besar-kecil dan tanda baca di antara katanya, lalu menjalankan tindakan pada teks tersebut. Jika kolom tidak berisi teks itu, SandFly mengucapkan "Tidak ditemukan" diikuti teks tersebut.

Angkanya 1 hingga 20 dalam bentuk kata, atau berupa digit. Tanpa angka, frasa berlaku untuk satu unit, seperti "pilih kata sebelumnya". Kata, kalimat, baris, atau paragraf tempat kursor berada dihitung sebagai yang pertama, mulai dari kursor. Kata dihitung tanpa tanda baca. Baris berakhir pada jeda baris. Menghapus seluruh kata, kalimat, baris, atau paragraf juga menghapus spasi atau jeda baris yang tersisa. Jika tidak ada lagi yang dapat dilewati, SandFly mengucapkan "Awal kolom" atau "Akhir kolom".

Nama perintah didahulukan: "Kata berikutnya" dan "Karakter sebelumnya" menjalankan perintah tersebut. Lihat [](#voice-commands-phrases).

Jika tidak ada kolom teks yang memiliki fokus input, "pilih" diikuti teks mengaktifkan item dengan teks tersebut, dan frasa lainnya dibaca sebagai perintah suara bawaan. "Pilih" diikuti teks juga mengaktifkan item jika kolom teks yang difokuskan adalah kolom sandi atau tidak berisi teks tersebut. Untuk frasa lainnya, di kolom sandi, SandFly mengucapkan "Tidak tersedia di kolom sandi".

#### Panggilan dan pesan {#voice-commands-calls}

- **Telepon**: "telepon", "hubungi", "panggil", "telepon ke", atau "lakukan panggilan ke", diikuti nama kontak atau nomor telepon. SandFly mengucapkan "Menelepon" diikuti nama tersebut, lalu memulai panggilan. Pada penggunaan pertama, SandFly meminta izin untuk melakukan panggilan telepon. Ucapkan perintah lagi setelah Anda mengizinkannya. Tanpa izin tersebut, aplikasi telepon terbuka dengan nomor tersebut.
- **Mengirim pesan**: seperti "kirim pesan ke Sam yang berbunyi saya sedang di jalan", "SMS Sam saya sedang di jalan", "kirim pesan ke Sam lewat WhatsApp yang berbunyi halo", atau "beri tahu Sam lewat Signal halo". Tanpa isi teks, SandFly mengucapkan "Pesan untuk" diikuti nama tersebut, lalu merekam teksnya.

Untuk nama kontak, SandFly memerlukan izin membaca kontak dan memintanya pada penggunaan pertama. Jika ada beberapa kontak dengan nama tersebut, SandFly mengucapkan "Ada lebih dari satu kontak bernama" diikuti nama lengkap mereka. Ucapkan nama lengkap kontak yang Anda maksud. Jika tidak ada kontak dengan nama tersebut, SandFly mengucapkan "Tidak ada kontak bernama" diikuti nama tersebut.

Pesan dikirim melalui aplikasi SMS default kecuali Anda menyebutkan nama aplikasi. Untuk SMS atau WhatsApp, SandFly membuka percakapan berisi teks tersebut, mengucapkan "Kirim ke" diikuti nama dan teksnya, lalu merekam jawaban Anda. Jika Anda mengucapkan "ya", "kirim", "oke", atau "tentu", SandFly menekan **Kirim** lalu mengucapkan "Kirim ditekan". Jika jawaban Anda lain, atau Anda tidak menjawab, pesan tetap belum terkirim di aplikasi, dan SandFly mengucapkan "Pesan tidak dikirim". Untuk aplikasi lain, layar berbagi aplikasi terbuka dengan teks tersebut, lalu Anda memilih percakapannya.

#### Deskripsi {#voice-commands-describe}

- **Mendeskripsikan layar**: "jelaskan layar", "deskripsikan layar", atau "apa yang ada di layar".
- **Mendeskripsikan gambar yang difokuskan**: "jelaskan gambar ini", "deskripsikan foto ini", atau "apa yang ada di gambar ini".
- **Salin**: "salin itu", "salin", atau "salin deskripsinya" menyalin deskripsi dari perintah sebelumnya. Jika tidak ada deskripsi, frasa terakhir yang diucapkan SandFly yang disalin.
- **Kirim**: "kirim ke Sam", "bagikan ke Sam", atau "kirim ke Sam lewat WhatsApp" mengirim deskripsi dari perintah sebelumnya sebagai pesan. Jika tidak ada deskripsi, frasa terakhir yang diucapkan SandFly yang dikirim. "Kirim deskripsi gambar ke Sam" dan "kirim deskripsi layar ke Sam" mendeskripsikan lalu mengirim dalam satu perintah. SandFly mengucapkan "Kirim ke" diikuti nama tersebut sebelum menekan **Kirim**.

Perintah ini menggunakan layanan AI atau deskripsi di perangkat yang diatur di **Deskripsi gambar dan layar** dan **Layanan AI**. Lihat [](#descriptions).

### Merangkai perintah {#voice-commands-chains}

Satu perintah suara dapat berisi beberapa perintah yang dihubungkan dengan "lalu", "setelah itu", "kemudian", atau "sesudah itu", seperti "buka Pesan lalu telepon Sam". Kata "dan" juga menghubungkan perintah jika setiap bagiannya merupakan perintah. Contohnya, "layar utama dan buka Chrome" adalah dua perintah, sedangkan "ketuk Garam dan merica" adalah satu perintah. SandFly menjalankan perintah satu per satu, dengan jeda singkat setelah setiap perintah yang mengubah layar.

Jika satu perintah gagal, seperti item yang tidak ditemukan, SandFly menyebutkan alasannya lalu berhenti. Bagian yang bukan pola perintah dicocokkan dengan perintah suara bawaan. Jika tetap tidak cocok, SandFly mengucapkan "Tidak dipahami" diikuti bagian tersebut.

Untuk menghentikan perintah suara yang sedang berjalan, mulai perintah suara lain lalu ucapkan "berhenti", "batal", "batalkan", atau "sudahlah". Memulai perintah suara lain saja juga sudah menghentikan perintah yang sedang berjalan.

### Pengeditan suara {#voice-commands-editing}

**Mulai pengeditan suara**, di kategori **Pengeditan teks**, merekam perintah suara untuk kolom teks yang difokuskan secara berturut-turut, tanpa perlu gerakan untuk setiap perintah. Perintah ini tidak memiliki gerakan secara default. Lihat [](#commands-list).

1. Pindahkan fokus ke kolom teks.
2. Jalankan **Mulai pengeditan suara**. SandFly mengucapkan "Pengeditan suara aktif", dan bunyi lonceng diputar saat perekaman dimulai.
3. Ucapkan perintah. SandFly menjalankannya, mengucapkan hasilnya, lalu mulai merekam lagi setelah selesai berbicara. Bunyi lonceng diputar lagi saat perekaman dimulai setelah sebuah perintah.

Pengeditan suara hanya menjalankan perintah berikut:

- Perintah dari kategori **Pengeditan teks** dan **Navigasi menurut teks**, berdasarkan namanya, seperti "Pilih semua", "Salin", "Tempel", "Hapus teks", "Gerakkan kursor ke akhir", "Kata berikutnya", atau "Karakter sebelumnya". **Dikte** dan **Mulai pengeditan suara** tidak dijalankan. Lihat [](#typing-editing-commands).
- "Ketik", "tulis", atau "masukkan", diikuti teks, yang diketikkan SandFly di posisi kursor lalu diucapkan.
- "Baris baru", "paragraf baru", atau "tekan enter", yang menjalankan **Baris baru**.
- Frasa untuk teks di kolom teks, seperti "hapus lima kata berikutnya", "akhir baris", atau "pilih buaya". Di sini, "pilih" diikuti teks selalu berlaku pada teks di kolom. Lihat [](#voice-commands-text).
- Frasa pengeditan teks dari perintah suara bawaan, seperti "Pilih", "Selesai memilih", "Hapus", "Potong", atau "Eja TEKS". Di sini frasa tersebut dicocokkan sebagai keseluruhan perintah. Lihat [](#appendix-voice-commands-text-editing).
- Perintah suara kustom yang menjalankan salah satu perintah ini atau makro. Lihat [](#voice-commands-custom).
- Rangkaian dari perintah-perintah ini, seperti "pilih semua lalu potong". Lihat [](#voice-commands-chains).

Ucapan lain tidak dijalankan. SandFly mengucapkan "Tidak dipahami" diikuti ucapan Anda, lalu merekam lagi. Teks yang bukan perintah tidak diketikkan. Untuk mengetiknya, ucapkan "ketik" sebelum teks tersebut.

Pengeditan suara berakhir, dan SandFly mengucapkan "Pengeditan suara nonaktif", saat:

- Anda menjalankan **Mulai pengeditan suara** lagi.
- Anda mengucapkan "berhenti mengedit", "hentikan pengeditan suara", "selesai mengedit", "akhiri pengeditan suara", "pengeditan suara nonaktif", "mulai pengeditan suara", "berhenti", "batal", "batalkan", atau "sudahlah".
- Fokus meninggalkan kolom teks.
- Waktu yang diatur di **Perintah suara** > **Batas waktu pengeditan suara** sudah habis sejak pengeditan suara dimulai. Pilihannya 5, 10, 20, atau 30 menit, dengan 10 menit sebagai default.
- Pengenalan ucapan gagal tiga kali berturut-turut, atau izin mikrofon tidak ada.

Ucapan SandFly dibisukan selama perekaman, agar tidak ikut terekam. Jika tidak ada yang diucapkan, perekaman dimulai lagi tanpa bunyi lonceng. Pengeditan suara merekam dengan pengenal ucapan yang dipilih di **Mengetik** > **Dikte**, tanpa koreksi AI. **Dikte**, **Dikte, putar, atau jeda media**, dan **Mulai perintah suara** mengakhiri pengeditan suara tanpa mengucapkan "Pengeditan suara nonaktif".

### Memahami perintah suara dengan AI {#voice-commands-ai}

**Perintah suara** > **Gunakan AI untuk memahami perintah suara** mengirimkan perintah suara yang tidak dipahami SandFly, atau rangkaian yang tidak sepenuhnya dipahami, ke layanan AI yang sedang digunakan. Layanan AI mengubahnya menjadi perintah. SandFly mengirimkan ucapan Anda, nama-nama perintah, dan teks item di layar. Nonaktif secara default.

Setelan ini memerlukan layanan AI yang diatur di **Layanan AI**: AI di perangkat, SandFly Pro, atau layanan lain dengan kunci API. Lihat [](#descriptions) dan [](#pro). Jawaban AI dapat menjalankan perintah, mengaktifkan item, membuka aplikasi, mengetik teks, menunggu, menelepon, mengirim pesan, mendeskripsikan layar atau gambar, menyalin, atau mengucapkan jawaban singkat.

### Perintah suara kustom {#voice-commands-custom}

Perintah suara kustom adalah frasa yang menjalankan suatu perintah. Perintah suara kustom diprioritaskan di atas semua perintah suara lainnya.

1. Di setelan SandFly, pilih **Perintah suara**.
2. Di bawah **Perintah suara kustom**, pilih **Tambahkan perintah suara**.
3. Masukkan frasa, hingga 200 karakter.
4. Pilih kategori, lalu perintahnya.

Pilih perintah suara kustom untuk **Ubah frasa**, **Ubah perintah**, dan **Hapus**. Dua perintah suara kustom tidak boleh memiliki frasa yang sama.

**Ekspor ke file** menyimpan perintah suara kustom ke file, dan **Impor dari file** menambahkannya dari file. Lihat [](#api-item-files).

Perintah suara kustom dapat menjalankan perintah apa pun, termasuk makro atau menu kustom. Untuk menjalankan beberapa langkah dari satu frasa, buat makro lalu beri makro tersebut sebuah frasa. Lihat [](#macros).

### Bahasa {#voice-commands-languages}

Perintah suara menggunakan bahasa perangkat. Frasa perintah suara bawaan, pola frasa, dan kata penghubungnya ikut diterjemahkan bersama SandFly. Nama perintah dikenali dalam bahasa yang ditampilkan SandFly. Frasa di [](#appendix-voice-commands) adalah frasa dalam bahasa Inggris.
