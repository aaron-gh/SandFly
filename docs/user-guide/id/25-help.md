## Bantuan dan masukan {#help}

**Bantuan & masukan** di setelan SandFly berisi:

- **Panduan pengguna**: panduan ini. Lihat [](#getting-started-user-guide).
- **Tutorial SandFly**: tutorial interaktif. Lihat [](#help-tutorial).
- **Berlatih gestur**: pelajaran latihan gestur dari tutorial. Lihat [](#help-tutorial).
- **Kirim masukan**: laporan masalah dan saran kepada pengembang SandFly, beserta balasannya. Lihat [](#help-feedback).
- **Riwayat versi**: catatan rilis dari setiap rilis SandFly. Lihat [](#help-version-history).
- **Kebijakan privasi**: kebijakan privasi SandFly. Lihat [](#help-version-history).

Setelan untuk developer dan untuk mencari masalah ada di **Setelan developer**. Lihat [](#help-developer).

### Tutorial dan latihan gestur {#help-tutorial}

**Bantuan & masukan** > **Tutorial SandFly** membuka tutorial SandFly pada pelajaran terakhir yang Anda buka. Pada perangkat dengan layar sentuh dan keyboard fisik, pilihan **Tutorial gestur sentuh** atau **Tutorial keyboard** akan muncul terlebih dahulu. Untuk daftar pelajaran dan cara kerja tutorial, lihat [](#getting-started-tutorial).

**Bantuan & masukan** > **Berlatih gestur** membuka **Latihan gestur**, di mana setiap gestur menyebutkan namanya dan perintah yang ditetapkan tanpa menjalankan perintah tersebut. Untuk keluar, pindah ke **Akhiri latihan** lalu aktifkan.

Perintah **Buka tutorial** dan **Berlatih gestur** membuka pelajaran yang sama. **Berlatih gestur** tidak ditampilkan di jam tangan dan TV, yang memiliki tutorial tersendiri. Di TV yang tidak memiliki tutorial, **Tutorial SandFly** bernama **Bantuan**. Lihat [](#devices).

### Kirim masukan {#help-feedback}

**Kirim masukan** mengirimkan laporan masalah atau saran kepada pengembang SandFly tanpa memerlukan akun. Pengembang dapat membalas, dan balasannya masuk langsung ke SandFly. Untuk membukanya, buka **Bantuan & masukan** > **Kirim masukan**, atau pilih **Kirim masukan** di menu SandFly.

Layar **Kirim masukan** memiliki:

- **Kirim masukan**: membuat laporan baru.
- **Kotak masuk**: masukan yang telah Anda kirim, beserta status setiap laporan dan balasannya.
- **Draf**: laporan yang sudah Anda mulai tetapi belum dikirim.

#### Mengirim laporan {#help-feedback-send}

1. Buka **Bantuan & masukan** > **Kirim masukan**.
2. Pilih **Kirim masukan**.
3. Isi formulir:
   - **Jenis umpan balik**: **Perilaku tidak semestinya**, **Kerusakan**, **Lambat atau tidak responsif**, atau **Saran**. Pilihan default adalah **Perilaku tidak semestinya**.
   - **Bagian**: bagian dari SandFly, seperti **Ucapan**, **Gestur**, **Penampil braille**, atau **Pembaruan**.
   - **Judul**: ringkasan singkat. Wajib diisi.
   - **Aplikasi tempat hal itu terjadi**: nama aplikasi, jika masalah hanya terjadi di satu aplikasi.
   - **Deskripsi**: apa yang terjadi dan apa yang Anda harapkan. Wajib diisi.
   - **Langkah untuk mereproduksi**: langkah yang dilakukan agar masalah muncul. Tidak ditampilkan untuk saran.
   - **Email untuk balasan**: opsional. Balasan tetap masuk ke SandFly baik Anda memasukkan alamat email maupun tidak.
   - **Sertakan informasi perangkat**: aktif secara default. Lihat [](#help-feedback-contents).
   - **Sertakan log SandFly**: nonaktif secara default. Lihat [](#help-feedback-contents).
4. Pilih **Kirim**.

SandFly akan mengucapkan "Masukan terkirim." dan laporan berpindah ke **Kotak masuk**. Jika laporan tidak dapat dikirim, SandFly menyebutkan alasannya, seperti "Masukan tidak terkirim. Periksa koneksi internet.", dan laporan tetap tersimpan sebagai draf.

Apa yang Anda ketik disimpan sebagai draf sampai Anda mengirimnya. **Hapus draf** menghapus draf tersebut. Alamat email dan kedua sakelar **Sertakan** tetap disimpan untuk laporan berikutnya.

#### Isi laporan {#help-feedback-contents}

Laporan selalu menyertakan kolom yang Anda isi serta versi dan nomor build SandFly.

**Sertakan informasi perangkat** menambahkan versi Android serta produsen dan model perangkat. Sakelar ini menampilkan informasi yang akan ditambahkan.

**Sertakan log SandFly** menambahkan log terbaru SandFly yang membantu pengembang menemukan penyebab masalah. Log ini dapat berisi teks dari layar. Isinya mencakup:

- Log SandFly sejak saat Anda membuka **Kirim masukan**, agar masalah yang ingin Anda laporkan masih tercatat di dalamnya. Dalam layar terpisah, log dibaca saat Anda memilih **Kirim**, sehingga Anda dapat memicu masalah di aplikasi satunya terlebih dahulu.
- Gestur terakhir yang dikenali SandFly.
- Crash terakhir dan waktu terakhir SandFly nonaktif, serta saat-saat SandFly sempat berhenti merespons.
- Layanan aksesibilitas yang aktif dan penekanan tombol volume terakhir.
- Log dari proses SandFly lain, seperti AI di perangkat.

Untuk melaporkan masalah segera setelah terjadi, buka **Kirim masukan** sebelum melakukan hal lain agar log masih menyimpannya.

#### Balasan dan status {#help-feedback-inbox}

**Kotak masuk** mencantumkan laporan yang telah Anda kirim, lengkap dengan status masing-masing dan jumlah balasan yang belum dibaca. Statusnya berupa salah satu dari:

- **Buka**.
- **Ditutup, selesai**.
- **Ditutup, tidak direncanakan**.
- **Tertutup, duplikat**.
- **Dihapus**: laporan sudah tidak ada lagi di server laporan.

Pilih laporan untuk membukanya. Laporan menampilkan **Status**, **Tanggapan Anda**, dan **Pesan**, yaitu balasan dari pengembang dan balasan Anda sendiri, masing-masing disertai pengirim dan waktunya. Untuk membalas:

1. Pilih **Balasan Anda** lalu ketik balasan.
2. Pilih **Kirim balasan**.

Selama SandFly aktif, SandFly memeriksa balasan dan perubahan status setiap jam, serta setiap kali Anda membuka **Kirim masukan**. Balasan atau perubahan status menampilkan notifikasi bersuara yang langsung membuka laporan tersebut. Laporan yang ditutup tetap diperiksa selama 30 hari. Notifikasi balasan berada di kategori notifikasi **Balasan umpan balik** milik SandFly.

**Hapus dari kotak masuk** menghapus laporan dari perangkat dan menghentikan pemeriksaannya. Pengembang tetap menyimpan laporan tersebut.

#### Melaporkan respons AI {#help-feedback-ai-response}

Perintah **Laporkan respons AI** membuka laporan baru yang sudah terisi teks terakhir dari model AI, seperti deskripsi gambar atau layar, hasil pengenalan langsung, atau hasil tugas agen. Gunakan perintah ini untuk melaporkan respons yang salah atau menyinggung. SandFly hanya menyimpan respons terakhir sampai SandFly berhenti. Jika belum ada respons AI, SandFly akan memberi tahu. Tetapkan perintah ini ke gestur atau tombol di **Perintah**. Lihat [](#commands) dan [](#descriptions).

### Riwayat versi dan kebijakan privasi {#help-version-history}

**Riwayat versi** membuka [halaman rilis SandFly](https://github.com/aaron-gh/SandFly/releases) di browser. Halaman ini memuat catatan rilis dari setiap rilis. **Kebijakan privasi** membuka [kebijakan privasi SandFly](https://sandfly.dev/privacy). Di jam tangan tanpa browser, SandFly menampilkan alamat situsnya.

### Setelan developer {#help-developer}

**Setelan developer** berisi setelan untuk menemukan masalah dan untuk pengembangan. Sebagian setelan menuliskan konten layar ke log sistem yang dapat dibaca aplikasi lain yang memiliki akses log. Biarkan nonaktif kecuali Anda membutuhkannya.

- **Kode Versi**: versi SandFly beserta kode versinya. Pada Android 11 dan lebih lama, versi ditampilkan di bawah judul layar.
- **Mode diagnosis**: nonaktif secara default. Ketukan empat jari menuliskan konten layar ke log sistem. Selama mode ini aktif, **Tampilkan output ucapan**, **Bacakan ucapan yang dikenali berulang-ulang**, **Aktifkan debug pohon node**, dan **Aktifkan overlay log debug** bekerja seolah aktif, serta keempatnya dan **Tingkat output log** tidak dapat diubah. SandFly meminta konfirmasi Anda.
- **Tampilkan output ucapan**: nonaktif secara default. Menampilkan teks yang diucapkan SandFly di layar.
- **Kelola gestur di SandFly**: aktif secara default. SandFly mengenali gestur sendiri alih-alih menyerahkannya ke Android. Banyak fitur gestur SandFly memerlukan opsi ini. Nonaktifkan lalu aktifkan kembali SandFly setelah mengubahnya. Tidak ditampilkan di TV.
- **Abaikan batas memori AI di perangkat**: nonaktif secara default. Menampilkan semua model AI di perangkat, termasuk model yang memerlukan memori lebih besar dari kapasitas perangkat, dan memuat model tanpa memeriksa sisa memori. Lihat [](#descriptions).
- **Bacakan ucapan yang dikenali berulang-ulang**: nonaktif secara default. SandFly mengucapkan teks yang dikenali dari perintah suara. Lihat [](#voice-commands).
- **Aktifkan statistik performa**: nonaktif secara default. SandFly mengumpulkan statistik waktu, yang ditulis ke log sistem oleh perintah **Cetak statistik performa**. Menggunakan lebih banyak memori dan daya. SandFly meminta konfirmasi Anda.
- **Jelajahi dengan sentuhan**: aktif secara default. Menonaktifkannya membuat SandFly berhenti membaca item yang Anda sentuh dan berhenti menangani gestur. SandFly meminta konfirmasi Anda. Tidak ditampilkan di TV.
- **Aktifkan debug pohon node**: nonaktif secara default. Perintah **Cetak pohon node** menuliskan pohon item di layar ke log sistem. Saat Anda mengaktifkannya, SandFly menawarkan untuk menetapkan **Cetak pohon node** ke ketuk tiga kali dengan empat jari. Di TV, tombol pencarian di remote mencetak pohon tersebut.
- **Aktifkan overlay log debug**: nonaktif secara default. Menampilkan log SandFly di layar.
- **Tingkat output log**: seberapa banyak informasi yang ditulis SandFly ke log sistem, dari **Tidak ada** hingga **VERBOSE**. Nilai defaultnya adalah **Error**. SandFly meminta konfirmasi Anda untuk pencatatan verbose, yang dapat memasukkan informasi pribadi ke dalam log.
- **Filter pencatatan log aktivitas aksesibilitas**: jenis aktivitas aksesibilitas yang ditulis SandFly ke log sistem.

Untuk memeriksa satu item, serta menyalin detail item atau pohon layar, gunakan perintah yang dijelaskan di [](#scripts).
