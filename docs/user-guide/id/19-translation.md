## Terjemahan {#translation}

SandFly dapat menerjemahkan frasa terakhir yang diucapkannya dan teks di dalam kolom teks. Dengan terjemahan otomatis, SandFly juga menerjemahkan semua ucapan dan notifikasi dalam bahasa lain. Terjemahan dibuat di ponsel, atau oleh layanan terjemahan atau layanan AI yang Anda pilih. Setelannya berada di **Terjemahan**.

### Menerjemahkan frasa terakhir yang diucapkan {#translation-last-phrase}

**Terjemahkan frasa terakhir yang diucapkan** menerjemahkan apa yang terakhir diucapkan SandFly lalu membacakan terjemahannya. Jika fokus berada di kolom teks, perintah ini menerjemahkan isi kolom teks tersebut, kecuali pada kolom sandi.

Perintah ini ada di kategori **Membaca** di **Perintah** dan tidak memiliki gerakan secara default. Tetapkan perintah ini ke gerakan atau tombol di sana. Lihat [](#commands).

1. Pindahkan fokus ke suatu item, atau biarkan SandFly membacakan sesuatu.
2. Jalankan **Terjemahkan frasa terakhir yang diucapkan**.

SandFly membacakan terjemahannya. Jika teks sudah berada dalam bahasa target, SandFly mengucapkan "Sudah dalam bahasa" diikuti nama bahasanya. Jika tidak ada yang bisa diterjemahkan, SandFly mengucapkan "Tidak ada yang perlu diterjemahkan".

Jika Anda menjalankan perintah ini lagi tepat setelahnya, SandFly menerjemahkan kembali teks asli yang sama, bukan hasil terjemahannya, sehingga perubahan bahasa yang Anda lakukan di antaranya langsung berlaku. Suatu terjemahan disimpan hingga teks atau setelan berubah, sehingga menjalankan perintah lagi tidak mengirim teks ulang ke layanan.

Hasil terjemahan ditambahkan ke riwayat ucapan, sehingga perintah untuk mengulang, mengeja, dan menyalin frasa terakhir yang diucapkan dapat bekerja padanya. Teks yang lebih dari 5.000 karakter akan dipotong.

### Memilih layanan terjemahan {#translation-service}

1. Buka **Terjemahan** > **Layanan terjemahan**.
2. Pilih salah satu layanan.

Layanan yang tersedia adalah:

- **Di perangkat**: menerjemahkan di ponsel menggunakan bahasa yang telah diunduh. Default di ponsel. Tidak tersedia di jam tangan. Lihat [](#translation-on-device).
- **Layanan AI**: menggunakan model AI di perangkat jika **Gunakan AI di perangkat** aktif. Jika tidak, layanan ini menggunakan layanan AI yang sedang dipakai di **Layanan AI**, seperti SandFly Pro, Gemini, atau Claude. Default di jam tangan. Lihat [](#descriptions-ai-services).
- **Google Cloud Translation**, **DeepL**, dan **Microsoft Translator**: memerlukan kunci API Anda sendiri dari masing-masing layanan.
- **LibreTranslate**: memerlukan alamat server LibreTranslate, seperti `https://libretranslate.com` atau server Anda sendiri, serta kunci API jika server membutuhkannya.

Saat Anda memilih layanan yang memerlukan kunci API atau alamat, SandFly langsung memintanya. Setelan layanan tersebut kemudian muncul dalam kategorinya sendiri di layar **Terjemahan**, misalnya **Setelan DeepL**:

- Item kunci API, seperti **Kunci API DeepL**: tempel kunci lalu pilih **Simpan**. Untuk menghapusnya, kosongkan kolom lalu pilih **Simpan**.
- **Alamat**, untuk LibreTranslate: alamat server. Harus diawali dengan `https://`.
- **Wilayah**, untuk Microsoft Translator: region Azure dari resource Translator Anda, seperti `westeurope`. Biarkan kosong untuk resource global (**Global**).

Layanan yang belum siap menampilkan "belum disiapkan" setelah namanya. Teks yang Anda terjemahkan akan dikirim ke layanan tersebut, kecuali saat menggunakan **Di perangkat**, atau saat menggunakan **Layanan AI** dengan AI di perangkat aktif. Kunci API tetap tersimpan di ponsel: setelan yang diekspor tidak menyertakannya.

### Memilih bahasa {#translation-languages}

- **Terjemahkan dari**: **Deteksi bahasa** (default) atau bahasa tertentu.
- **Terjemahkan ke**: **Bahasa perangkat** (default) atau bahasa tertentu.
- **Terjemahkan teks dalam bahasa target ke**: bahasa kedua untuk teks yang sudah dalam bahasa target, atau **Jangan terjemahkan** (default). Misalnya, penutur bahasa Inggris yang membaca bahasa Jerman dan Inggris dapat memilih bahasa Jerman di sini. Hanya ditampilkan saat **Terjemahkan dari** disetel ke **Deteksi bahasa**.
- **Tukar bahasa**: menukar bahasa pada **Terjemahkan dari** dan **Terjemahkan ke**, lalu SandFly menyebutkan pasangan bahasa yang baru. Hanya ditampilkan saat **Terjemahkan dari** disetel ke bahasa tertentu.

Dengan **Di perangkat**, daftar hanya menampilkan bahasa yang didukung oleh terjemahan di perangkat. Deteksi bahasa berjalan di ponsel. Pada kali pertama, SandFly mungkin mengucapkan "Deteksi bahasa sedang diunduh. Coba lagi sebentar lagi."

### Bahasa di perangkat {#translation-on-device}

Jika **Di perangkat** dipilih sebagai layanan terjemahan atau layanan terjemahan otomatis, kategori **Setelan Di perangkat** memiliki item berikut:

- **Bahasa di perangkat**: menampilkan daftar bahasa dan bahasa mana yang sudah diunduh. Pilih bahasa yang belum diunduh untuk mengunduhnya (sekitar 30 MB), atau pilih bahasa yang sudah diunduh untuk menghapusnya.
- **Unduh semua bahasa**: mengunduh setiap bahasa yang belum diunduh satu per satu, setelah menampilkan dialog berisi jumlah dan ukurannya. Selama pengunduhan, item ini menampilkan jumlah yang sudah selesai, dan SandFly mengumumkan setiap seperempat progres selama layar **Terjemahan** terbuka. Pengunduhan tetap berlanjut setelah Anda meninggalkan layar. Pilih item ini lagi untuk menghentikannya.
- **Unduh bahasa hanya melalui Wi-Fi** (nonaktif secara default): bahasa hanya diunduh saat terhubung ke Wi-Fi.

Bahasa yang belum diunduh akan diunduh saat pertama kali diperlukan, bersama bahasa Inggris. Bahasa Inggris menjadi perantara untuk menerjemahkan pasangan bahasa lain. SandFly menyebutkan bahasa yang sedang diunduh.

### Terjemahan otomatis {#translation-automatic}

Kategori **Terjemahan otomatis** memiliki setelan berikut:

- **Terjemahan otomatis** (nonaktif secara default): ucapan dalam bahasa selain **Terjemahkan ke** diterjemahkan sebelum SandFly membacakannya, termasuk notifikasi.
- **Terjemahkan notifikasi** (nonaktif secara default): hanya menerjemahkan notifikasi saat **Terjemahan otomatis** nonaktif.
- **Layanan terjemahan otomatis**: **Di perangkat**, **Google Cloud Translation**, **DeepL**, **Microsoft Translator**, atau **LibreTranslate**. **Layanan AI** tidak ditawarkan. Default-nya adalah layanan terjemahan yang dipilih, atau **Di perangkat** jika layanan terjemahan adalah **Layanan AI**.
- **Bahasa yang tidak diterjemahkan**: bahasa yang dibacakan SandFly apa adanya, seperti bahasa yang Anda pahami. Kosong secara default.

Dua setelan terakhir tersedia saat **Terjemahan otomatis** atau **Terjemahkan notifikasi** aktif.

Untuk mengaktifkan dan menonaktifkan terjemahan otomatis tanpa membuka setelan, gunakan kontrol pembacaan **Terjemahan otomatis**. Kontrol ini nonaktif secara default. Aktifkan di **Kontrol pembacaan**. Lihat [](#commands).

#### Cara kerja terjemahan otomatis {#translation-automatic-details}

SandFly mengenali bahasa setiap ucapan di ponsel. Teks berikut dibacakan apa adanya dan tidak dikirim ke layanan: teks dalam bahasa target, teks dalam salah satu **Bahasa yang tidak diterjemahkan**, dan, saat **Terjemahkan dari** disetel ke bahasa tertentu, teks dalam bahasa lain mana pun. Teks kurang dari tiga huruf tidak diperiksa.

Suara dan getaran untuk sebuah ucapan langsung diputar, lalu kata-katanya menyusul setelah terjemahan siap. Terjemahan yang memakan waktu lebih dari 3 detik atau gagal akan digantikan oleh teks asli. SandFly menyimpan terjemahan terbaru sehingga kembali ke item sebelumnya tidak menerjemahkannya ulang.

Dengan **Di perangkat**, bahasa yang belum diunduh mulai diunduh begitu SandFly yakin akan bahasanya. Selama diunduh, ucapan dibacakan tanpa diterjemahkan. Setelah selesai diunduh, SandFly membacakan ulang item tempat Anda berada dalam bentuk terjemahan, kecuali Anda sudah berpindah. Notifikasi menunggu pengunduhan hingga 30 detik.

Notifikasi diterjemahkan sebelum masuk antrean, sehingga urutannya tetap terjaga. Notifikasi yang dibacakan dengan profil suara tetap memakai suara profil tersebut. Notifikasi pribadi tidak diterjemahkan, sehingga teksnya tidak pernah dikirim ke layanan mana pun. Lihat [](#notifications).

### Cara terjemahan dibacakan {#translation-speech}

Kategori **Ucapan** memiliki setelan berikut:

- **Gunakan suara untuk bahasa target** (aktif secara default): hasil terjemahan ditandai dengan bahasanya, sehingga SandFly mengganti suara seperti pada teks lain dalam bahasa tersebut sesuai setelan bahasa di **Ucapan**. Lihat [](#speech).
- **Ucapkan bahasa asli** (nonaktif secara default): SandFly menyebutkan bahasa teks asli sebelum terjemahannya, misalnya "Dari Jerman:".
- **Salin terjemahan ke papan klip** (nonaktif secara default): setiap hasil terjemahan disalin ke papan klip.

### Pesan error terjemahan {#translation-errors}

Jika terjemahan gagal, SandFly menjelaskan alasannya, misalnya:

- Kunci API atau alamat LibreTranslate belum diisi: "Masukkan kunci API DeepL di setelan Terjemahan."
- Layanan tidak menerima kunci API, atau batas penggunaannya telah tercapai.
- Layanan tidak dapat dihubungi.
- Layanan tidak mendukung bahasa tersebut.
- "Bahasa tidak terdeteksi".
- Bahasa untuk terjemahan di perangkat masih diunduh, atau tidak dapat diunduh.

Untuk kegagalan lainnya, SandFly memberi tahu bahwa layanan tidak dapat menerjemahkan teks, disertai pesan dari layanan tersebut.
