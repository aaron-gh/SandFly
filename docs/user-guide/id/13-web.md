## Halaman web dan elemen {#web}

Konten web adalah halaman di browser, seperti Chrome, atau tampilan web di dalam aplikasi. SandFly membaca konten web dengan gestur yang sama seperti layar lainnya, serta menambahkan cara untuk berpindah menurut elemen: menurut judul, link, penanda, kontrol formulir, daftar, atau tabel. Perpindahan elemen ini juga berfungsi di aplikasi.

Untuk berpindah per item, kontrol pembacaan, dan pembacaan berkelanjutan, lihat [](#reading). Pintasan keyboard untuk halaman web, termasuk pintasan satu tombol pada mode penjelajahan, dijelaskan di [](#keyboards).

### Membaca halaman web {#web-read}

- Geser ke kanan atau ke kiri untuk berpindah ke item berikutnya atau sebelumnya di halaman. Menggeser melewati item terakhir halaman akan berlanjut ke item setelah konten web, seperti toolbar browser.
- Sentuh layar untuk membaca item di bawah jari Anda.
- Untuk membaca seluruh halaman, gunakan **Baca dari atas** atau **Baca dari item yang difokuskan**. Lihat [](#reading-continuous).

SandFly mengambil item berikutnya dan sebelumnya dari item halaman sesuai urutan halaman, dan hanya meminta item berikutnya dari halaman bila diperlukan, sehingga item pada halaman yang baru dibuka tidak terlewat.

Saat Anda menggulir halaman sendiri, dengan dua jari atau dengan perintah gulir, dan item yang difokuskan tergulir keluar dari tampilan, SandFly memindahkan fokus ke item pertama yang terlihat, atau ke item terakhir yang terlihat saat Anda menggulir mundur. Saat halaman berubah atau dimuat ulang dan item yang difokuskan menghilang, SandFly memfokuskan item yang kini berada di posisinya.

Cara pembacaan beberapa elemen:

- Penanda, seperti navigasi atau utama, dibacakan berdasarkan jenis dan namanya, bukan seluruh isinya.
- Kotak centang dan tombol pilihan dibacakan bersama labelnya.
- Saat halaman memindahkan fokus ke elemen seperti postingan, seperti yang dilakukan Mastodon dengan tombol J dan K, SandFly membacakan teks link di dalamnya, seperti penulis, waktu, dan tagar, tetapi tidak tombol-tombolnya. Spasi di antara link tidak dibacakan.

Catatan: Beberapa mesin ucapan, seperti Gryphon, hanya berhenti di antara potongan teks yang diterimanya. Jika ucapan item berikutnya mulai terlambat setelah Anda menggeser menjauh dari paragraf panjang, aktifkan **Ucapan** > **Kirim teks panjang per kalimat**. Lihat [](#speech-pronunciation).

### Berpindah menurut elemen {#web-elements}

Untuk berpindah menurut satu jenis elemen, pilih kontrol pembacaannya, lalu geser ke bawah untuk elemen berikutnya dan geser ke atas untuk elemen sebelumnya. Jika tidak ada lagi elemen jenis tersebut, SandFly membacakan, misalnya, "Tidak ada judul berikutnya".

Kontrol pembacaan berikut aktif secara default:

- **Judul**: judul pada level mana pun.
- **Link**: link.
- **Kontrol**: item yang dapat Anda gunakan, seperti tombol, kotak centang, dan kolom teks.

Anda dapat menambahkan kontrol pembacaan berikut di layar **Kontrol pembacaan**, di bawah **Navigasi elemen** dan **Tingkat judul**:

- **Penanda**: penanda halaman, seperti "utama" atau "navigasi".
- **Tombol**
- **Kotak centang**
- **Tombol pilihan**
- **Edit kolom**
- **Kotak kombinasi**: kotak kombinasi, seperti menu drop-down.
- **Item yang dapat difokuskan**: item yang dapat menerima fokus input.
- **Gambar dan video**
- **Daftar**
- **Item daftar**
- **Tabel**
- **Link yang telah dikunjungi**: link ke halaman yang pernah Anda buka.
- **Link yang belum dikunjungi**: link ke halaman yang belum Anda buka.
- **Judul 1** hingga **Judul 6**: judul pada satu level tertentu.

Untuk menambahkan kontrol pembacaan, lihat [](#commands). Untuk memilih kontrol pembacaan, lihat [](#reading-controls).

Anda juga dapat berpindah menurut elemen tanpa mengubah kontrol pembacaan: tetapkan gestur ke perintah di kategori **Navigasi menurut elemen**, seperti **Judul berikutnya**, **Judul sebelumnya**, dan **Judul 1 berikutnya**. Lihat [](#gestures). Pada keyboard fisik, setiap elemen memiliki pintasan keyboard; lihat [](#keyboards).

Perintah **Navigasi**, yang dapat Anda tambahkan ke menu SandFly, mencantumkan jenis navigasi yang dapat dipilih, seperti karakter, judul, atau penanda. Saat perintah ini ada di menu SandFly, **Edit opsi navigasi**, di bawahnya pada layar **Menu SandFly**, memilih jenis navigasi apa saja yang ditampilkan. Lihat [](#commands).

#### Elemen di aplikasi {#web-elements-in-apps}

Setiap kontrol pembacaan elemen, gestur, dan pintasan keyboard juga berfungsi di aplikasi, sehingga satu set perintah dapat digunakan di halaman web maupun aplikasi. Di aplikasi, elemen-elemen tersebut adalah:

| Elemen | Di aplikasi |
| --- | --- |
| Judul, serta Judul 1 hingga Judul 6 | Item yang ditandai aplikasi sebagai judul. Aplikasi tidak melaporkan level judul, sehingga setiap level berpindah melalui semua judul. |
| Link | Link di dalam teks. |
| Kontrol | Tombol, kotak centang, sakelar, kolom teks, penggeser, dan item lain yang dapat Anda gunakan. |
| Penanda | Toolbar, bilah tab, bilah navigasi, dan bilah penelusuran, panel dengan judul, serta penampung dengan judul. |
| Tombol | Tombol, tombol gambar, dan tombol tindakan mengambang. |
| Kotak centang | Kotak centang, sakelar, dan tombol alih. |
| Tombol pilihan | Tombol pilihan. |
| Edit kolom | Kolom teks. |
| Kotak kombinasi | Daftar drop-down. |
| Item yang dapat difokuskan | Item yang dapat menerima fokus input. |
| Gambar dan video | Gambar, termasuk gambar yang dapat Anda aktifkan. |
| Daftar | Daftar dan koleksi yang bukan tabel. |
| Item daftar | Baris dari daftar yang memiliki fokus. Lihat [](#reading-lists-tables). |
| Tabel | Tabel dan kisi dengan baris serta kolom. |
| Link yang telah dikunjungi | Tidak ada. Aplikasi tidak melaporkan link yang telah dikunjungi, sehingga SandFly membacakan "Tidak ada link yang dikunjungi berikutnya". |
| Link yang belum dikunjungi | Semua link. |

Saat elemen berikutnya berada di luar layar, SandFly menggulir daftar tempat elemen tersebut berada, hingga 10 kali.

**Kontrol pembacaan** > **Tautan dan Kontrol hanya di konten web**, yang nonaktif secara default, mengecualikan kontrol pembacaan **Link** dan **Kontrol** dari daftar di luar konten web. Gestur dan pintasan keyboard tetap berfungsi di mana saja.

Saat pengenalan layar SandFly Pro menambahkan elemen pada aplikasi yang tidak melaporkannya, elemen tersebut dibaca, ditemukan lewat sentuhan, dan dinavigasi seperti item aplikasi lainnya. Lihat [](#pro).

### Tabel di halaman web {#web-tables}

Di halaman web, kontrol pembacaan **Tabel** berpindah dari satu tabel ke tabel lainnya. Di dalam tabel, kontrol pembacaan **Baris dan kolom**, **Baris**, dan **Kolom** berpindah antarsel, sama seperti di aplikasi. Lihat [](#reading-tables).

Keyboard fisik memiliki lebih banyak perintah tabel di kategori **Tabel**, seperti berpindah ke awal atau akhir baris, kolom, atau tabel, serta membacakan header baris atau kolom. Lihat [](#keyboards).

### Tindakan di halaman web {#web-actions}

Jika **Kontrol pembacaan** > **Ubah kontrol pembacaan secara otomatis** aktif, SandFly memilih kontrol pembacaan **Tindakan** saat fokus berpindah ke item yang memiliki tindakan. Di halaman web, SandFly hanya memilihnya pada item yang memiliki tindakannya sendiri, bukan pada teks atau link di dalam teks, sehingga kontrol pembacaan yang Anda pilih, seperti **Judul**, tetap terpilih saat Anda membaca. Pada item berikutnya yang tidak memiliki tindakan, SandFly kembali memilih kontrol pembacaan sebelumnya. Jika **Tindakan** dipilih secara otomatis dan tidak ada tindakan untuk item yang difokuskan, menggeser ke atas atau ke bawah akan menggunakan kontrol pembacaan yang digantikannya.

### Mode penjelajahan {#web-browse-mode}

Mode penjelajahan mengubah fungsi tombol keyboard fisik dan perintah layar braille. Saat mode penjelajahan aktif, satu tombol berpindah menurut elemen, seperti H untuk judul berikutnya, alih-alih dikirim ke halaman. Saat mode penjelajahan nonaktif, tombol dikirim ke halaman untuk mengetik di kolom teks dan untuk kontrol yang menggunakan tombol panah. SandFly membacakan "Mode penjelajahan aktif" atau "Mode penjelajahan nonaktif" dan memutar suara saat statusnya berubah.

Untuk mengaktifkan atau menonaktifkan mode penjelajahan, gunakan perintah **Aktifkan atau nonaktifkan mode penjelajahan** di kategori **Web**, yang dapat Anda tetapkan ke gestur. Setiap pemetaan tombol juga memiliki pintasan keyboard untuk perintah ini. Lihat [](#keyboards).

Setelan berikut berada di layar **Navigasi**:

- **Mode penjelajahan cerdas**: mengaktifkan atau menonaktifkan mode penjelajahan untuk item yang difokuskan. Aktif secara default. SandFly mengaktifkan mode penjelajahan saat fokus berpindah ke item di konten web, dan menonaktifkannya di kolom teks serta kontrol yang menggunakan tombol panah, seperti penggeser, kotak daftar, bilah menu, menu, tampilan pohon, kisi, kotak kombinasi, dan tombol putar. Jika perintah keyboard memindahkan fokus SandFly ke salah satu item tersebut, fokus input juga berpindah ke sana. Jika Anda menonaktifkan mode penjelajahan sendiri di konten web, memindahkan fokus di dalam konten web tidak akan mengaktifkannya kembali. Mode penjelajahan juga dinonaktifkan saat fokus input berpindah ke kolom teks atau penggeser di aplikasi.
- **Ingat mode jelajah di setiap aplikasi**: SandFly menyimpan status mode penjelajahan terakhir yang Anda aktifkan atau nonaktifkan di setiap aplikasi. Di aplikasi yang belum pernah Anda ubah statusnya, mode penjelajahan nonaktif, kecuali **Mode penjelajahan cerdas** mengaktifkannya di konten web. Nonaktif secara default.

### Daftar elemen dan alamat {#web-lists}

Dengan keyboard fisik, perintah berikut di kategori **Web** mencantumkan elemen di halaman:

- **Tampilkan daftar judul**
- **Tampilkan daftar penanda**
- **Tampilkan daftar link**
- **Tampilkan daftar kontrol**
- **Tampilkan daftar tabel**

Masing-masing membuka penelusuran layar dengan daftar satu jenis elemen. Pilih elemen untuk memindahkan fokus ke elemen tersebut. Saat keyboard fisik terhubung, penelusuran layar juga memiliki tombol **Semua**, **Judul**, **Penanda**, **Link**, **Kontrol**, dan **Tabel**, yang menampilkan satu jenis elemen. Lihat [](#reading-search).

**Ucapkan URL** membacakan alamat link yang difokuskan, dan **Ucapkan URL halaman** membacakan alamat halaman. Keduanya adalah perintah keyboard. Lihat [](#keyboards).

Untuk menemukan alamat web, alamat email, dan nomor telepon yang tertulis dalam teks suatu item, gunakan kontrol pembacaan **Alamat dan nomor**. Lihat [](#reading-addresses).

### Setelan untuk konten web {#web-settings}

- **Navigasi** > **Mode penjelajahan cerdas** dan **Ingat mode jelajah di setiap aplikasi**: Lihat [](#web-browse-mode).
- **Kontrol pembacaan** > **Tautan dan Kontrol hanya di konten web**: Lihat [](#web-elements-in-apps).
- **Kontrol pembacaan** > **Ubah kontrol pembacaan secara otomatis**: Lihat [](#web-actions).
- Profil setelan dapat digunakan secara otomatis saat fokus berada di konten web, misalnya untuk menyetel kecepatan bicara berbeda untuk halaman web. Lihat [](#profiles).
