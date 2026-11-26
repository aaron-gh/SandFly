## Bantuan dan masukan {#help}

**Bantuan & masukan** di setelan SandFly berisi:

- **Panduan pengguna**: panduan ini. Lihat [](#getting-started-user-guide).
- **Tutorial SandFly**: tutorial interaktif. Lihat [](#help-tutorial).
- **Berlatih gerakan**: pelajaran latihan gerakan dari tutorial. Lihat [](#help-tutorial).
- **Latihan keyboard**: mengaktifkan latihan keyboard. SandFly mengucapkan setiap tombol yang ditekan beserta perintahnya, tanpa menjalankan apa pun. Tersedia saat SandFly aktif; tidak ditampilkan di jam tangan. Lihat [](#keyboards-practice).
- **Uji ketepatan gerakan**: latihan yang mengukur gerakan Anda berdasarkan batas yang dipakai SandFly. Tidak ditampilkan di jam tangan dan TV. Lihat [](#gestures-trainer).
- **Kirim masukan**: laporan masalah dan saran kepada pengembang SandFly, beserta balasannya. Lihat [](#help-feedback).
- **Riwayat versi**: catatan rilis dari setiap rilis SandFly. Lihat [](#help-version-history).
- **Kebijakan privasi**: kebijakan privasi SandFly. Lihat [](#help-version-history).

Setelan untuk developer dan untuk mencari masalah ada di **Setelan developer**. Lihat [](#help-developer).

### Tutorial dan latihan gerakan {#help-tutorial}

**Bantuan & masukan** > **Tutorial SandFly** membuka tutorial SandFly pada pelajaran terakhir yang Anda buka. Pada perangkat dengan layar sentuh dan keyboard fisik, pilihan **Tutorial gerakan sentuh** dan **Tutorial keyboard** ditampilkan terlebih dahulu. Untuk daftar pelajaran dan cara kerja tutorial, lihat [](#getting-started-tutorial).

**Bantuan & masukan** > **Berlatih gerakan** membuka **Latihan gerakan**. Di sana, SandFly menyebutkan nama setiap gerakan yang Anda lakukan dan perintah yang ditetapkan padanya, tanpa menjalankan perintah tersebut. Untuk keluar, pindah ke **Akhiri latihan** lalu aktifkan.

Perintah **Buka tutorial** dan **Berlatih gerakan** membuka pelajaran yang sama. **Berlatih gerakan** tidak ditampilkan di jam tangan dan TV, yang memiliki tutorial tersendiri. Di TV yang tidak memiliki tutorial, **Tutorial SandFly** bernama **Bantuan**. Lihat [](#devices).

### Kirim masukan {#help-feedback}

**Kirim masukan** mengirimkan laporan masalah atau saran kepada pengembang SandFly tanpa memerlukan akun. Pengembang dapat membalas, dan balasannya masuk langsung ke SandFly. Untuk membukanya, buka **Bantuan & masukan** > **Kirim masukan**, atau pilih **Kirim masukan** di menu SandFly.

Layar **Kirim masukan** memiliki:

- **Masukan baru**: membuat laporan baru.
- **Kotak masuk**: masukan yang telah Anda kirim, beserta status setiap laporan dan balasannya.
- **Draf**: laporan yang sudah Anda mulai tetapi belum dikirim.

#### Mengirim laporan {#help-feedback-send}

1. Buka **Bantuan & masukan** > **Kirim masukan**.
2. Pilih **Masukan baru**.
3. Isi formulir:
   - **Jenis masukan**: **Perilaku salah**, **Error tiba-tiba**, **Lambat atau tidak merespons**, atau **Saran**. Pilihan default adalah **Perilaku salah**.
   - **Bagian**: bagian dari SandFly, seperti **Ucapan**, **Gerakan**, **Penampil braille**, atau **Pembaruan**.
   - **Judul**: ringkasan singkat. Wajib diisi.
   - **Aplikasi tempat masalah terjadi**: nama aplikasi, jika masalah hanya terjadi di satu aplikasi.
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
- Gerakan terakhir yang dikenali SandFly.
- Crash terakhir dan waktu terakhir SandFly nonaktif, serta saat-saat SandFly sempat berhenti merespons.
- Layanan aksesibilitas yang aktif serta penekanan tombol volume dan tombol Enter terakhir.
- Pengumuman, notifikasi, toast, dan nama jendela terakhir yang dikirim aplikasi, masing-masing dengan aplikasi pengirimnya dan awal teksnya.
- Ke mana fokus berpindah setelah Kembali, waktu yang diperlukan untuk memeriksa kondisi **Tampilkan di Tindakan hanya jika** pada makro, apa yang memutar dan merekam audio, serta pengetikan dari pop-up karakter alternatif Gboard.
- Log dari proses SandFly lain, seperti AI di perangkat.

Untuk melaporkan masalah segera setelah terjadi, buka **Kirim masukan** sebelum melakukan hal lain agar log masih menyimpannya.

#### Balasan dan status {#help-feedback-inbox}

**Kotak masuk** mencantumkan laporan yang telah Anda kirim, lengkap dengan status masing-masing dan jumlah balasan yang belum dibaca. Statusnya berupa salah satu dari:

- **Buka**.
- **Ditutup, selesai**.
- **Ditutup, tidak direncanakan**.
- **Ditutup, duplikat**.
- **Dihapus**: laporan sudah tidak ada lagi di server laporan.

Pilih laporan untuk membukanya. Laporan menampilkan **Status**, **Masukan Anda**, dan **Pesan**. **Pesan** berisi balasan dari pengembang dan balasan Anda sendiri, masing-masing disertai pengirim dan waktunya. Untuk membalas:

1. Pilih **Balasan Anda** lalu ketik balasan.
2. Pilih **Kirim balasan**.

Selama SandFly aktif, SandFly memeriksa balasan dan perubahan status setiap jam, serta setiap kali Anda membuka **Kirim masukan**. Balasan atau perubahan status menampilkan notifikasi bersuara yang langsung membuka laporan tersebut. Laporan yang ditutup tetap diperiksa selama 30 hari. Notifikasi balasan berada di kategori notifikasi **Balasan masukan** milik SandFly.

**Hapus dari kotak masuk** menghapus laporan dari perangkat dan menghentikan pemeriksaannya. Pengembang tetap menyimpan laporan tersebut.

#### Melaporkan respons AI {#help-feedback-ai-response}

Perintah **Laporkan respons AI** membuka laporan baru yang sudah terisi teks terakhir dari model AI, seperti deskripsi gambar atau layar, hasil pengenalan langsung, atau hasil tugas agen. Gunakan perintah ini untuk melaporkan respons yang salah atau menyinggung. SandFly hanya menyimpan respons terakhir sampai SandFly berhenti. Jika belum ada respons AI, SandFly akan memberi tahu. Tetapkan perintah ini ke gerakan atau tombol di **Perintah**. Lihat [](#commands) dan [](#descriptions).

### Riwayat versi dan kebijakan privasi {#help-version-history}

**Riwayat versi** membuka [halaman rilis SandFly](https://github.com/aaron-gh/SandFly/releases) di browser. Halaman ini memuat catatan rilis dari setiap rilis. **Kebijakan privasi** membuka [kebijakan privasi SandFly](https://sandfly.dev/privacy). Di jam tangan tanpa browser, SandFly menampilkan alamat situsnya.

### Setelan developer {#help-developer}

**Setelan developer** berisi setelan untuk menemukan masalah dan untuk pengembangan. Sebagian setelan menuliskan konten layar ke log sistem yang dapat dibaca aplikasi lain yang memiliki akses log. Biarkan nonaktif kecuali Anda membutuhkannya.

- **Kode versi**: versi SandFly beserta kode versinya. Pada Android 11 dan lebih lama, versi ditampilkan di bawah judul layar sebagai gantinya.
- **Mode diagnosis**: nonaktif secara default. Ketukan empat jari menuliskan konten layar ke log sistem. Selama mode ini aktif, **Tampilkan output ucapan**, **Ulangi ucapan yang dikenali**, **Aktifkan debug pohon node**, dan **Aktifkan overlay log debug** bekerja seolah aktif, serta keempatnya dan **Tingkat output log** tidak dapat diubah. SandFly menampilkan dialog konfirmasi.
- **Tampilkan output ucapan**: nonaktif secara default. Menampilkan teks yang diucapkan SandFly di layar.
- **Kelola gerakan di SandFly**: aktif secara default. SandFly mengenali gerakan sendiri alih-alih menyerahkannya ke Android. Banyak fitur gerakan SandFly memerlukan opsi ini. Nonaktifkan lalu aktifkan kembali SandFly setelah mengubahnya. Tidak ditampilkan di TV.
- **Abaikan batas memori AI di perangkat**: nonaktif secara default. Menampilkan semua model AI di perangkat, termasuk model yang memerlukan memori lebih besar dari kapasitas perangkat, dan memuat model tanpa memeriksa sisa memori. Lihat [](#descriptions).
- **Ulangi ucapan yang dikenali**: nonaktif secara default. SandFly mengucapkan teks yang dikenali dari perintah suara. Lihat [](#voice-commands).
- **Aktifkan statistik performa**: nonaktif secara default. SandFly mengumpulkan statistik waktu, yang ditulis ke log sistem oleh perintah **Cetak statistik performa**. Menggunakan lebih banyak memori dan daya. SandFly menampilkan dialog konfirmasi.
- **Jelajahi dengan sentuhan**: aktif secara default. Menonaktifkannya membuat SandFly berhenti membaca item yang Anda sentuh dan berhenti menangani gerakan. SandFly menampilkan dialog konfirmasi. Tidak ditampilkan di TV.
- **Aktifkan debug pohon node**: nonaktif secara default. Perintah **Cetak pohon node** menuliskan pohon item di layar ke log sistem. Saat Anda mengaktifkannya, dialog menawarkan untuk menetapkan **Cetak pohon node** ke ketuk tiga kali dengan empat jari. Di TV, tombol pencarian di remote mencetak pohon tersebut.
- **Aktifkan overlay log debug**: nonaktif secara default. Menampilkan log SandFly di layar.
- **Tingkat output log**: seberapa banyak informasi yang ditulis SandFly ke log sistem, dari **Tidak ada** hingga **VERBOSE**. Nilai defaultnya adalah **Error**. Pencatatan verbose, yang dapat memasukkan informasi pribadi ke dalam log, memerlukan konfirmasi.
- **Filter pencatatan log peristiwa aksesibilitas**: jenis aktivitas aksesibilitas yang ditulis SandFly ke log sistem.

Untuk memeriksa satu item, serta menyalin detail item atau pohon layar, gunakan perintah yang dijelaskan di [](#scripts).
