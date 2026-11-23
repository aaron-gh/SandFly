## Perintah suara {#voice-commands}

Perintah suara menjalankan perintah SandFly dari ucapan: memindahkan fokus, membaca, mengedit teks, membuka aplikasi, mengaktifkan item di layar, menelepon kontak, mengirim pesan, atau mendeskripsikan layar. Satu perintah suara dapat berisi beberapa perintah secara berurutan. **Perintah suara** di setelan SandFly mengaktifkan AI untuk perintah yang tidak dipahami SandFly, serta menyimpan perintah suara yang Anda buat.

Untuk daftar lengkap frasa, lihat [](#appendix-voice-commands).

### Memulai perintah suara {#voice-commands-start}

1. Gunakan perintah **Mulai perintah suara**. Pada tata letak gestur SandFly, geser ke kanan lalu ke atas. Lihat [](#appendix-gestures).
2. Ucapkan perintah.

Pada penggunaan pertama, SandFly menampilkan **Menggunakan perintah suara untuk mengontrol SandFly** berisi contoh hal yang dapat diucapkan. Pilih **Gunakan perintah suara** untuk mulai merekam. Agar dialog ini tidak ditampilkan lagi, hapus centang **Selalu tampilkan pesan ini**.

Perintah suara memerlukan izin mikrofon. SandFly merekam menggunakan pengenal ucapan yang dipilih di **Mengetik** > **Dikte**. Lihat [](#typing).

Perintah suara tidak tersedia saat layar terkunci atau selama penyiapan perangkat. Saat layar terkunci, SandFly mengumumkannya dan menyebutkan gestur untuk membuka menu SandFly.

Untuk melihat daftar perintah, ucapkan "Bantuan" atau "Apa yang bisa saya ucapkan". SandFly membuka **Ringkasan perintah suara**.

Perintah **Mulai perintah suara** juga ada di kategori **Tindakan sistem**, sehingga pintasan keyboard atau item menu dapat menjalankannya. Untuk menambahkannya ke menu SandFly, lihat [](#commands-edit-menu).

### Apa yang dapat Anda ucapkan {#voice-commands-phrases}

SandFly membaca ucapan Anda dalam urutan berikut:

1. Perintah suara kustom, berdasarkan frasanya. Lihat [](#voice-commands-custom).
2. Nama perintah apa pun, sebagaimana ditampilkan SandFly di layar **Perintah**, seperti "Baca dari atas", "Deskripsikan gambar", atau "Judul berikutnya". Nama makro, menu kustom, dan perintah skrip Anda juga termasuk. Lihat [](#commands-list).
3. Pola frasa, seperti "buka Chrome", "ketuk Setelan", "telepon Sam", atau "ketik halo". Lihat [](#voice-commands-sandfly).
4. Salah satu perintah suara bawaan, seperti "Cari simpan", "Navigasi kata", "Bicara lebih cepat", atau "Salin". Lihat [](#appendix-voice-commands).
5. Jika **Pahami perintah suara dengan AI** aktif, layanan AI. Lihat [](#voice-commands-ai).

Huruf besar/kecil dan tanda baca yang ditambahkan pengenal ucapan tidak berpengaruh. Kata-kata seperti "tolong", "bisakah kamu", "bisakah Anda", "maukah kamu", "maukah Anda", "saya ingin", "saya mau", dan "sekarang" di awal, serta "tolong", "untuk saya", dan "sekarang" di akhir, diabaikan.

Jika SandFly tidak memahami perintah, SandFly mengucapkan "Tidak dapat mengenali perintah ini" beserta cara melihat daftar perintah.

#### Aplikasi, item, dan teks {#voice-commands-sandfly}

- **Membuka aplikasi**: "buka", "jalankan", "mulai", atau "beralih ke", diikuti nama aplikasi. SandFly memilih aplikasi dengan nama utuh tersebut, jika tidak ada maka yang namanya diawali teks itu, atau yang memuat salah satu katanya. Jika beberapa aplikasi cocok secara setara, SandFly mengucapkan "Tidak ada aplikasi bernama" diikuti nama tersebut.
- **Mengaktifkan item di layar**: "ketuk", "ketuk dua kali", "klik", "tekan", "aktifkan", atau "pilih", diikuti teks item. SandFly mengaktifkan item pertama yang seluruh teksnya sama, atau jika tidak ada maka item pertama yang teksnya memuat teks tersebut. Jika tidak ada, SandFly mengucapkan "Tidak ditemukan" diikuti teks tersebut.
- **Ketik teks**: "ketik", "tulis", atau "masukkan", diikuti teksnya. SandFly mengetikkannya di kolom edit yang difokuskan.
- **Menjalankan perintah**: "jalankan" atau "lakukan" diikuti nama perintah, atau "jalankan makro" diikuti nama makro, seperti "jalankan makro pagi".
- **Tunggu**: "tunggu", "tunggu sebentar", atau "tunggu" diikuti jumlah detik dan "detik", hingga 60 detik. Gunakan di antara perintah dalam satu rangkaian.

#### Panggilan dan pesan {#voice-commands-calls}

- **Telepon**: "telepon", "hubungi", "panggil", "telepon ke", atau "lakukan panggilan ke", diikuti nama kontak atau nomor telepon. SandFly mengucapkan "Menelepon" diikuti nama tersebut lalu memulai panggilan. Pada penggunaan pertama, SandFly meminta izin untuk melakukan panggilan telepon; ucapkan perintah lagi setelah Anda mengizinkannya. Tanpa izin tersebut, aplikasi telepon terbuka dengan nomor tersebut.
- **Mengirim pesan**: seperti "kirim pesan ke Sam yang berbunyi saya sedang di jalan", "SMS Sam saya sedang di jalan", "kirim pesan ke Sam lewat WhatsApp yang berbunyi halo", atau "beri tahu Sam lewat Signal halo". Tanpa isi teks, SandFly mengucapkan "Pesan untuk" diikuti nama tersebut, lalu merekam teksnya.

Untuk nama kontak, SandFly memerlukan izin membaca kontak dan memintanya pada penggunaan pertama. Jika ada beberapa kontak dengan nama tersebut, SandFly mengucapkan "Ada lebih dari satu kontak bernama" diikuti nama lengkap mereka; ucapkan nama lengkapnya.

Pesan dikirim melalui aplikasi SMS default kecuali Anda menyebutkan nama aplikasi. Untuk SMS atau WhatsApp, SandFly membuka percakapan dengan teks tersebut, mengucapkan "Kirim ke" diikuti nama dan teksnya, lalu merekam jawaban Anda. Ucapkan "ya", "kirim", "oke", atau "tentu", dan SandFly akan menekan **Kirim** lalu mengucapkan "Kirim ditekan". Jawaban lain, atau tanpa jawaban, membiarkan pesan belum terkirim di aplikasi, dan SandFly mengucapkan "Pesan tidak dikirim". Aplikasi lain membuka layar berbagi dengan teks tersebut, tempat Anda memilih percakapan.

#### Deskripsi {#voice-commands-describe}

- **Mendeskripsikan layar**: "jelaskan layar", "deskripsikan layar", atau "apa yang ada di layar".
- **Mendeskripsikan gambar yang difokuskan**: "jelaskan gambar ini", "deskripsikan foto ini", atau "apa yang ada di gambar ini".
- **Salin**: "salin itu", "salin", atau "salin deskripsinya" menyalin deskripsi dari perintah sebelumnya, atau jika tidak ada maka frasa terakhir yang diucapkan SandFly.
- **Kirim**: "kirim ke Sam", "bagikan ke Sam", atau "kirim ke Sam lewat WhatsApp" mengirim deskripsi dari perintah sebelumnya sebagai pesan. "Kirim deskripsi gambar ke Sam" dan "kirim deskripsi layar ke Sam" mendeskripsikan lalu mengirim dalam satu perintah; SandFly menanyakan "Kirim ke" diikuti nama tersebut sebelum menekan **Kirim**.

Perintah ini menggunakan layanan AI atau deskripsi di perangkat yang diatur di **Deskripsi gambar dan layar** dan **Layanan AI**. Lihat [](#descriptions).

### Merangkai perintah {#voice-commands-chains}

Satu perintah suara dapat berisi beberapa perintah yang dihubungkan dengan "lalu", "setelah itu", "kemudian", atau "sesudah itu", seperti "buka Pesan lalu telepon Sam". "Dan" juga menghubungkan perintah jika setiap bagiannya merupakan perintah: "layar utama dan buka Chrome" adalah dua perintah, sedangkan "ketuk Garam dan merica" adalah satu perintah. SandFly menjalankan perintah satu per satu, dengan jeda singkat setelah setiap perintah yang mengubah layar.

Jika satu perintah gagal, seperti item yang tidak ditemukan, SandFly menyebutkan alasannya lalu berhenti. Bagian yang bukan merupakan pola perintah dibaca sebagai salah satu perintah suara bawaan; jika bukan juga, SandFly mengucapkan "Tidak dipahami" diikuti bagian tersebut.

Untuk menghentikan perintah suara yang sedang berjalan, mulai perintah suara lain lalu ucapkan "berhenti", "batal", "batalkan", atau "sudahlah". Memulai perintah suara lain juga menghentikan perintah yang sedang berjalan.

### Memahami perintah suara dengan AI {#voice-commands-ai}

**Perintah suara** > **Pahami perintah suara dengan AI** mengirimkan perintah suara yang tidak dipahami SandFly, atau rangkaian yang tidak sepenuhnya dipahami, ke layanan AI yang digunakan untuk diubah menjadi perintah. SandFly mengirimkan ucapan Anda, nama-nama perintah, dan teks item di layar. Nonaktif secara default.

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

Perintah suara menggunakan bahasa perangkat. Frasa perintah suara bawaan, pola frasa, dan kata penghubungnya diterjemahkan bersama SandFly, dan nama perintah dibaca dalam bahasa yang ditampilkan SandFly. Frasa di [](#appendix-voice-commands) adalah frasa dalam bahasa Inggris.
