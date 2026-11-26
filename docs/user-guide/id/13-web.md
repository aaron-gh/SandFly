## Halaman web dan elemen {#web}

Konten web adalah halaman di browser, seperti Chrome, atau tampilan web di dalam aplikasi. SandFly membaca konten web dengan gerakan yang sama seperti di layar lainnya. SandFly juga menambahkan cara untuk berpindah menurut elemen, yaitu menurut judul, tautan, penunjuk, kontrol formulir, daftar, atau tabel. Perpindahan menurut elemen ini juga berfungsi di aplikasi.

Untuk berpindah per item, kontrol pembacaan, dan pembacaan berkelanjutan, lihat [](#reading). Pintasan keyboard untuk halaman web, termasuk pintasan satu tombol dalam mode penjelajahan, dijelaskan di [](#keyboards).

### Membaca halaman web {#web-read}

- Geser ke kanan atau ke kiri untuk berpindah ke item berikutnya atau sebelumnya di halaman. Jika Anda menggeser melewati item terakhir halaman, fokus berlanjut ke item setelah konten web, seperti toolbar browser.
- Sentuh layar untuk membaca item di bawah jari Anda.
- Untuk membaca seluruh halaman, gunakan **Baca dari atas** atau **Baca dari item yang difokuskan**. Lihat [](#reading-continuous).

SandFly menentukan item berikutnya dan sebelumnya dari item-item halaman, sesuai urutan di halaman. SandFly hanya meminta item berikutnya dari halaman bila diperlukan. Dengan begitu, item pada halaman yang baru dibuka tidak terlewat.

Anda mungkin menggulir halaman sendiri, dengan dua jari atau dengan perintah gulir, sampai item yang difokuskan keluar dari tampilan. Dalam hal ini, SandFly memindahkan fokus ke item pertama yang terlihat, atau ke item terakhir yang terlihat jika Anda menggulir mundur. Jika halaman berubah atau dimuat ulang dan item yang difokuskan menghilang, SandFly memfokuskan item yang kini berada di posisi tersebut.

Cara pembacaan beberapa elemen:

- Penunjuk, seperti navigasi atau utama, dibacakan berdasarkan jenis dan namanya, bukan seluruh isinya.
- Kotak centang dan tombol pilihan dibacakan bersama labelnya.
- Beberapa halaman memindahkan fokus ke elemen seperti postingan, misalnya Mastodon dengan tombol J dan K. Dalam hal ini, SandFly membacakan teks tautan di dalam elemen itu, seperti penulis, waktu, dan tagar, tetapi tidak membacakan tombol-tombolnya. Spasi di antara tautan tidak dibacakan.

Catatan: Beberapa mesin ucapan, seperti Gryphon, hanya dapat berhenti di antara potongan teks yang diterimanya. Jika ucapan item berikutnya terlambat dimulai setelah Anda menggeser dari paragraf panjang, aktifkan **Ucapan** > **Kirim teks panjang per kalimat**. Lihat [](#speech-pronunciation).

### Berpindah menurut elemen {#web-elements}

Untuk berpindah menurut satu jenis elemen, pilih kontrol pembacaannya, lalu geser ke bawah untuk elemen berikutnya dan geser ke atas untuk elemen sebelumnya. Jika tidak ada lagi elemen jenis tersebut, SandFly membacakan pesan seperti "Tidak ada judul berikutnya".

Kontrol pembacaan berikut aktif secara default:

- **Judul**: judul pada level mana pun.
- **Tautan**: tautan.
- **Kontrol**: item yang dapat Anda gunakan, seperti tombol, kotak centang, dan kolom teks.

Anda dapat menambahkan kontrol pembacaan berikut di layar **Kontrol pembacaan**, di bawah **Navigasi elemen** dan **Tingkat judul**:

- **Penunjuk**: penunjuk halaman, seperti "utama" atau "navigasi".
- **Tombol**
- **Kotak centang**
- **Tombol pilihan**
- **Kolom teks**
- **Kotak kombinasi**: kotak kombinasi, seperti menu drop-down.
- **Item yang dapat difokuskan**: item yang dapat menerima fokus input.
- **Gambar dan video**
- **Daftar**
- **Item daftar**
- **Tabel**
- **Tautan yang telah dikunjungi**: tautan ke halaman yang pernah Anda buka.
- **Tautan yang belum dikunjungi**: tautan ke halaman yang belum pernah Anda buka.
- **Judul 1** hingga **Judul 6**: judul pada satu level tertentu.

Untuk menambahkan kontrol pembacaan, lihat [](#commands). Untuk memilih kontrol pembacaan, lihat [](#reading-controls).

Anda juga dapat berpindah menurut elemen tanpa mengubah kontrol pembacaan. Caranya, tetapkan gerakan ke perintah di kategori **Navigasi menurut elemen**, seperti **Judul berikutnya**, **Judul sebelumnya**, dan **Judul 1 berikutnya**. Lihat [](#gestures). Pada keyboard fisik, setiap elemen memiliki pintasan keyboard. Lihat [](#keyboards).

Perintah **Navigasi**, yang dapat Anda tambahkan ke menu SandFly, mencantumkan jenis navigasi yang dapat dipilih, seperti karakter, judul, atau penunjuk. Saat perintah ini ada di menu SandFly, **Edit opsi navigasi** muncul tepat di bawahnya pada layar **Menu SandFly**. Gunakan item itu untuk memilih jenis navigasi yang ditampilkan. Lihat [](#commands).

#### Elemen di aplikasi {#web-elements-in-apps}

Setiap kontrol pembacaan elemen, gerakan, dan pintasan keyboard juga berfungsi di aplikasi. Dengan begitu, satu set perintah dapat digunakan di halaman web maupun di aplikasi. Di aplikasi, elemen-elemen tersebut adalah:

| Elemen | Di aplikasi |
| --- | --- |
| Judul, serta Judul 1 hingga Judul 6 | Item yang ditandai aplikasi sebagai judul. Aplikasi tidak melaporkan level judul, sehingga setiap level berpindah melalui semua judul. |
| Tautan | Tautan di dalam teks. |
| Kontrol | Tombol, kotak centang, sakelar, kolom teks, penggeser, dan item lain yang dapat Anda gunakan. |
| Penunjuk | Toolbar, bilah tab, bilah navigasi, bilah penelusuran, panel dengan judul, dan penampung dengan judul. |
| Tombol | Tombol, tombol gambar, dan tombol tindakan mengambang. |
| Kotak centang | Kotak centang, sakelar, dan tombol alih. |
| Tombol pilihan | Tombol pilihan. |
| Kolom teks | Kolom teks. |
| Kotak kombinasi | Daftar drop-down. |
| Item yang dapat difokuskan | Item yang dapat menerima fokus input. |
| Gambar dan video | Gambar, termasuk gambar yang dapat Anda aktifkan. |
| Daftar | Daftar dan koleksi yang bukan tabel. |
| Item daftar | Baris dari daftar yang memiliki fokus. Lihat [](#reading-lists-tables). |
| Tabel | Tabel dan kisi dengan baris serta kolom. |
| Tautan yang telah dikunjungi | Tidak ada. Aplikasi tidak melaporkan tautan yang telah dikunjungi, sehingga SandFly membacakan "Tidak ada tautan yang dikunjungi berikutnya". |
| Tautan yang belum dikunjungi | Semua tautan. |

Jika elemen berikutnya berada di luar layar, SandFly menggulir daftar yang memuat elemen tersebut, hingga 10 kali.

**Kontrol pembacaan** > **Tautan dan Kontrol hanya di konten web**, yang nonaktif secara default, menghapus kontrol pembacaan **Tautan** dan **Kontrol** dari daftar saat Anda berada di luar konten web. Gerakan dan pintasan keyboard tetap berfungsi di mana saja.

Beberapa aplikasi tidak melaporkan elemennya. Jika pengenalan layar SandFly Pro menambahkan elemen tersebut, SandFly membacanya, menemukannya lewat sentuhan, dan menavigasinya seperti item aplikasi lainnya. Lihat [](#pro).

### Tabel di halaman web {#web-tables}

Di halaman web, kontrol pembacaan **Tabel** berpindah dari satu tabel ke tabel lainnya. Di dalam tabel, kontrol pembacaan **Baris dan kolom**, **Baris**, dan **Kolom** berpindah antarsel, sama seperti di aplikasi. Lihat [](#reading-tables).

Keyboard fisik memiliki lebih banyak perintah tabel di kategori **Tabel**, seperti berpindah ke awal atau akhir baris, kolom, atau tabel, dan membacakan header baris atau kolom. Lihat [](#keyboards).

### Tindakan di halaman web {#web-actions}

Jika **Kontrol pembacaan** > **Ubah kontrol pembacaan secara otomatis** aktif, SandFly memilih kontrol pembacaan **Tindakan** saat fokus berpindah ke item yang memiliki tindakan. Di halaman web, SandFly hanya memilihnya pada item yang memiliki tindakannya sendiri, bukan pada teks atau tautan di dalam teks. Dengan begitu, kontrol pembacaan yang Anda pilih, seperti **Judul**, tetap terpilih saat Anda membaca. Pada item berikutnya yang tidak memiliki tindakan, SandFly kembali memilih kontrol pembacaan sebelumnya. Jika **Tindakan** dipilih secara otomatis tetapi item yang difokuskan tidak memiliki tindakan, geser ke atas atau ke bawah memakai kontrol pembacaan yang tadi digantikan.

### Mode penjelajahan {#web-browse-mode}

Mode penjelajahan mengubah fungsi tombol keyboard fisik dan perintah penampil braille. Saat mode penjelajahan aktif, tombol tunggal berpindah menurut elemen, seperti H untuk judul berikutnya, dan tidak dikirim ke halaman. Saat mode penjelajahan nonaktif, tombol dikirim ke halaman untuk mengetik di kolom teks dan untuk kontrol yang menggunakan tombol panah. Saat statusnya berubah, SandFly membacakan "Mode penjelajahan aktif" atau "Mode penjelajahan nonaktif" dan memutar suara.

Untuk mengaktifkan atau menonaktifkan mode penjelajahan, gunakan perintah **Aktifkan atau nonaktifkan mode penjelajahan** di kategori **Web**, yang dapat Anda tetapkan ke gerakan. Setiap peta tombol juga memiliki pintasan keyboard untuk perintah ini. Lihat [](#keyboards).

Dalam mode penjelajahan, tombol penelusuran cepat bersama huruf atau angka berpindah ke item berikutnya yang diawali huruf atau angka tersebut. **Pintasan keyboard** > **Penelusuran cepat** > **Tombol penelusuran cepat** memilih tombolnya; nonaktif secara default. Lihat [](#keyboards-fast-search).

Setelan berikut berada di layar **Navigasi**:

- **Mode penjelajahan cerdas**: mengaktifkan atau menonaktifkan mode penjelajahan untuk item yang difokuskan. Aktif secara default. SandFly mengaktifkan mode penjelajahan saat fokus berpindah ke item di konten web. SandFly menonaktifkannya di kolom teks dan di kontrol yang menggunakan tombol panah, seperti penggeser, kotak daftar, bilah menu, menu, tampilan pohon, kisi, kotak kombinasi, dan tombol putar. Jika perintah keyboard memindahkan fokus SandFly ke salah satu item tersebut, fokus input juga berpindah ke sana. Jika Anda menonaktifkan mode penjelajahan sendiri di konten web, memindahkan fokus di dalam konten web tidak akan mengaktifkannya kembali. Mode penjelajahan juga dinonaktifkan saat fokus input berpindah ke kolom teks atau penggeser di aplikasi.
- **Ingat mode penjelajahan di setiap aplikasi**: SandFly mengingat status mode penjelajahan terakhir yang Anda atur di setiap aplikasi, aktif atau nonaktif. Di aplikasi yang belum pernah Anda ubah statusnya, mode penjelajahan nonaktif, kecuali **Mode penjelajahan cerdas** mengaktifkannya di konten web. Nonaktif secara default.

### Daftar elemen dan alamat {#web-lists}

Dengan keyboard fisik, perintah berikut di kategori **Web** mencantumkan elemen di halaman:

- **Tampilkan daftar judul**
- **Tampilkan daftar penunjuk**
- **Tampilkan daftar tautan**
- **Tampilkan daftar kontrol**
- **Tampilkan daftar tabel**

Setiap perintah membuka penelusuran layar dengan daftar satu jenis elemen. Pilih elemen untuk memindahkan fokus ke elemen tersebut. Saat keyboard fisik terhubung, penelusuran layar juga memiliki tombol **Semua**, **Judul**, **Penunjuk**, **Tautan**, **Kontrol**, dan **Tabel**. Setiap tombol menampilkan satu jenis elemen. Lihat [](#reading-search).

**Ucapkan URL** membacakan alamat tautan yang difokuskan, dan **Ucapkan URL halaman** membacakan alamat halaman. Keduanya adalah perintah keyboard. Lihat [](#keyboards).

Untuk menemukan alamat web, alamat email, dan nomor telepon yang tertulis dalam teks suatu item, gunakan kontrol pembacaan **Alamat dan nomor**. Lihat [](#reading-addresses).

### Setelan untuk konten web {#web-settings}

- **Navigasi** > **Mode penjelajahan cerdas** dan **Ingat mode penjelajahan di setiap aplikasi**: lihat [](#web-browse-mode).
- **Kontrol pembacaan** > **Tautan dan Kontrol hanya di konten web**: lihat [](#web-elements-in-apps).
- **Kontrol pembacaan** > **Ubah kontrol pembacaan secara otomatis**: lihat [](#web-actions).
- Profil setelan dapat digunakan secara otomatis saat fokus berada di konten web, misalnya untuk menyetel kecepatan bicara berbeda untuk halaman web. Lihat [](#profiles).
