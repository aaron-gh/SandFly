## Deskripsi gambar dan layar {#descriptions}

SandFly dapat mendeskripsikan gambar yang difokuskan, seluruh layar, dan apa yang terlihat oleh kamera belakang. SandFly juga dapat menjawab pertanyaan tentang semua itu. Deskripsi berasal dari salah satu sumber berikut:

- Layanan AI, seperti Gemini, Claude, atau OpenAI, dengan kunci API Anda sendiri. Lihat [](#descriptions-ai-services).
- SandFly Pro, tanpa kunci API. Lihat [](#pro).
- Model yang berjalan di ponsel, tanpa mengirim data apa pun melalui jaringan. Lihat [](#descriptions-on-device).

Teks dalam gambar dikenali langsung di ponsel dan tidak memerlukan sumber mana pun di atas.

Setelan untuk deskripsi berada di dua layar. **Deskripsi gambar dan layar** mengaktifkan deskripsi dan membuka Pengenalan langsung. **Layanan AI** memilih layanan pembuat deskripsi. Deskripsi memerlukan ponsel atau tablet, dan tidak tersedia di jam tangan dan TV.

### Menyiapkan deskripsi gambar dan layar {#descriptions-set-up}

1. Siapkan layanan AI, SandFly Pro, atau AI di perangkat. Lihat [](#descriptions-set-up-service), [](#pro), atau [](#descriptions-on-device-download).
2. Buka **Deskripsi gambar dan layar** lalu aktifkan **Deskripsi gambar mendetail**.
3. Baca dialog tentang data yang dikirim dan tujuannya, lalu pilih **Aktifkan**.

Jika Anda menjalankan **Deskripsikan gambar** saat **Deskripsi gambar mendetail** nonaktif, SandFly menampilkan dialog **Dapatkan deskripsi gambar mendetail?**. **Aktifkan** mengaktifkan setelan tersebut lalu mendeskripsikan gambar. **Lain kali** membiarkan setelan tetap nonaktif, dan SandFly hanya membacakan teks dalam gambar.

Saat pertama kali Anda menjalankan **Deskripsikan layar**, SandFly menampilkan dialog **Deskripsi layar** yang menjelaskan bahwa screenshot akan dikirim. Pilih **Aktifkan** untuk mendeskripsikan layar.

### Setelan deskripsi gambar dan layar {#descriptions-settings}

**Deskripsi gambar dan layar** memiliki setelan berikut:

- **Deskripsi gambar mendetail** (nonaktif secara default): **Deskripsikan gambar** mengirim gambar yang difokuskan ke layanan AI yang sedang digunakan, atau ke model di perangkat, untuk mendapatkan deskripsi lengkap.
- **Deskripsi otomatis untuk teks dalam gambar**: SandFly membacakan teks dalam gambar saat fokus berpindah ke gambar tersebut. Pilih **Semua gambar**, **Hanya gambar tidak berlabel** (default), atau **Jangan deskripsikan gambar**. Teks dikenali di ponsel.
- **Pengenalan teks Sirilik**: mengunduh dan menghapus model pengenalan teks untuk bahasa yang ditulis dalam aksara Sirilik, seperti Rusia, Ukraina, dan Serbia. Lihat [](#descriptions-cyrillic-text). Tidak tersedia di jam tangan.
- **Pengenalan langsung**: membuka layar Pengenalan langsung. Lihat [](#descriptions-live-recognition). Hanya tersedia di perangkat dengan kamera.

### Pengenalan teks Sirilik {#descriptions-cyrillic-text}

Pengenalan teks membaca teks Sirilik dengan model untuk bahasa tersebut, yang perlu Anda unduh terlebih dahulu. SandFly menggunakan model bahasa ucapan, atau model bahasa ponsel pertama, yang ditulis dalam aksara Sirilik. Tanpa model, hanya huruf Latin yang dibaca, dan SandFly mengucapkan satu kali bahwa model belum diunduh.

1. Buka **Deskripsi gambar dan layar** > **Pengenalan teks Sirilik**.
2. Pilih bahasanya. Model bahasa tersebut, berukuran beberapa megabyte, diunduh bersama model huruf Latin pada kali pertama, sehingga teks dalam kedua aksara dapat dibaca. Jika menggunakan data seluler, dialog konfirmasi terbuka terlebih dahulu.

Untuk menghentikan unduhan, pilih bahasa itu lagi. Untuk menghapus model, pilih bahasa yang sudah diunduh, lalu **Hapus**. Model huruf Latin dihapus bersama bahasa terakhir.

Model berasal dari proyek Tesseract OCR dan dibaca oleh Tesseract di ponsel. Pengenalan lebih lambat daripada untuk aksara lain.

### Mendeskripsikan gambar {#descriptions-describe-image}

1. Pindahkan fokus ke gambar.
2. Jalankan **Deskripsikan gambar**. Perintah ini ada di Menu SandFly, dan Anda dapat menetapkannya ke gerakan atau tombol. Lihat [](#descriptions-commands).

SandFly membuka jendela **Deskripsi gambar** yang berisi deskripsi dan teks yang ditemukan dalam gambar. Di jendela tersebut Anda dapat menyalin deskripsi, memberi peringkat, dan mengajukan pertanyaan tentang gambar. Lihat [](#descriptions-questions).

Jika deskripsi mendetail tidak dapat dibuat, SandFly membacakan teks dalam gambar dan menyebutkan alasannya, misalnya "Tidak bisa mendapatkan deskripsi gambar yang lebih mendetail" beserta alasannya, atau "Hubungkan ke internet untuk mendapatkan deskripsi gambar yang lebih mendetail."

Agar deskripsi dibacakan lantang tanpa membuka jendela, jalankan **Ucapkan deskripsi gambar** sebagai gantinya.

### Mendeskripsikan layar {#descriptions-describe-screen}

Jalankan **Deskripsikan layar**. SandFly mengambil screenshot, mengirimkannya bersama item di layar, lalu membuka jendela **Deskripsi layar** berisi deskripsi layar. Anda dapat mengajukan pertanyaan tentang layar di jendela tersebut. Lihat [](#descriptions-questions).

Agar deskripsi dibacakan lantang tanpa membuka jendela, jalankan **Ucapkan deskripsi layar** sebagai gantinya.

**Deskripsikan layar** hanya tersedia jika bahasa ponsel Anda didukung. Screenshot jendela aman, seperti di beberapa aplikasi perbankan, tidak dapat diambil.

### Mengajukan pertanyaan tentang gambar atau layar {#descriptions-questions}

Pertanyaan dikirim ke layanan AI atau model di perangkat yang membuat deskripsi.

Di jendela **Deskripsi gambar** atau **Deskripsi layar**:

- **Tanyakan dengan suara** merekam pertanyaan menggunakan pengenal ucapan yang dipilih di **Mengetik** > **Dikte**, mengirimkannya, lalu membacakan jawabannya. **Berhenti mendengarkan** mengakhiri rekaman. Lihat [](#typing).
- **Ketikkan pertanyaan** membuka kolom teks. Ketik pertanyaan lalu pilih **Tanya**.

Setiap jawaban tetap berada di jendela tersebut, sehingga Anda dapat mengajukan pertanyaan lanjutan tentang gambar atau layar yang sama.

Untuk bertanya tanpa membuka deskripsi terlebih dahulu, jalankan **Tanyakan tentang gambar** atau **Tanyakan tentang layar**:

1. Untuk **Tanyakan tentang gambar**, pindahkan fokus ke gambar.
2. Jalankan perintah tersebut. SandFly mengambil screenshot gambar atau layar lalu mulai merekam.
3. Ucapkan pertanyaan Anda. Rekaman berhenti setelah jeda bicara, atau saat Anda menjalankan perintah itu lagi.

SandFly mengirim pertanyaan dan membacakan jawabannya. Jika rekaman tidak dapat dimulai, kolom teks terbuka sebagai gantinya. Ketik pertanyaan lalu pilih **Tanya**. Selama Anda belum menyetujui pengiriman screenshot ke layanan AI, kedua perintah ini menjalankan **Deskripsikan gambar** atau **Deskripsikan layar**. Perintah itu menampilkan dialog persetujuannya.

### Perintah untuk deskripsi {#descriptions-commands}

Perintah berikut berada dalam kategori **Deskripsi gambar dan layar** di **Perintah**, tempat Anda dapat menetapkannya ke gerakan dan tombol serta menambahkannya ke Menu SandFly. Lihat [](#commands).

- **Ambil foto dan deskripsikan**: mengambil foto dengan kamera belakang dan mendeskripsikannya. Lihat [](#descriptions-take-picture).
- **Deskripsikan gambar**, **Ucapkan deskripsi gambar**, dan **Tanyakan tentang gambar**: mendeskripsikan gambar yang difokuskan dalam jendela, mendeskripsikannya secara lisan, atau menjawab pertanyaan lisan tentangnya.
- **Deskripsikan layar**, **Ucapkan deskripsi layar**, dan **Tanyakan tentang layar**: sama seperti di atas, untuk seluruh layar.
- **Bagikan gambar item yang difokuskan** dan **Bagikan screenshot**: mengambil screenshot item yang difokuskan atau seluruh layar, lalu membuka sheet berbagi Android agar screenshot itu dapat dikirim ke aplikasi lain. Memerlukan Android 11 atau yang lebih baru. Jendela aman tampil hitam di screenshot.
- **Laporkan respons AI**: membuka laporan masalah berisi teks terakhir yang diberikan model AI, seperti deskripsi atau jawaban. Lihat [](#help).

Dengan SandFly Pro, kategori ini juga memuat perintah pengenalan layar. Lihat [](#pro-screen-recognition).

Kategori **Pengenalan langsung** memiliki perintah untuk setiap mode Pengenalan langsung, seperti **Pengenalan langsung: Pemandangan**, serta **Hentikan Pengenalan langsung**. Lihat [](#descriptions-live-recognition).

Untuk gerakan dan pintasan keyboard default, lihat [](#appendix-gestures) dan [](#appendix-keyboard-shortcuts).

#### Kontrol pembacaan Tindakan AI {#descriptions-ai-actions}

Kontrol pembacaan **Tindakan AI** menjalankan **Deskripsikan layar**, **Ucapkan deskripsi layar**, **Tanyakan tentang layar**, **Deskripsikan gambar**, **Ucapkan deskripsi gambar**, **Tanyakan tentang gambar**, **Bagikan gambar item yang difokuskan**, dan **Bagikan screenshot**. Dengan SandFly Pro, kontrol ini juga menjalankan **Mulai atau hentikan tugas agen AI** dan **Ketik tugas agen AI**. Fitur ini memerlukan Android 11 atau versi lebih baru.

1. Pilih kontrol pembacaan **Tindakan AI**.
2. Geser ke atas atau ke bawah untuk memilih tindakan.
3. Ketuk dua kali untuk menjalankannya.

Selama fokus belum berpindah, ketuk dua kali menjalankan tindakan yang terakhir diumumkan kontrol pembacaan. **Tindakan AI yang disertakan**, di **Kontrol pembacaan**, menentukan tindakan yang ditawarkan kontrol pembacaan. Secara default, semua tindakan disertakan. Tindakan yang tidak tersedia tidak ditampilkan. Untuk kontrol pembacaan, lihat [](#commands).

### Mengambil foto dan mendeskripsikannya {#descriptions-take-picture}

**Ambil foto dan deskripsikan** membuka layar yang mengambil satu foto dengan kamera belakang. Foto itu lalu dideskripsikan seperti pada **Deskripsikan gambar**, lengkap dengan jendela yang sama untuk mengajukan pertanyaan. Fitur ini hanya tersedia di perangkat yang memiliki kamera.

1. Jalankan **Ambil foto dan deskripsikan**. SandFly mengucapkan "Tahan ponsel agar tetap stabil" lalu mengambil foto.
2. Baca deskripsi, dan ajukan pertanyaan tentang foto tersebut jika diperlukan.
3. Untuk mengambil foto lagi, pilih **Ambil foto lain**.

SandFly meminta izin kamera pada penggunaan pertama.

### Layanan AI {#descriptions-ai-services}

**Layanan AI**, di setelan utama SandFly, menentukan layanan yang mendeskripsikan gambar dan layar serta menjawab pertanyaan tentang keduanya. Layanan yang sama digunakan oleh Pengenalan langsung, terjemahan dengan opsi **Layanan AI**, dan fitur lain yang menggunakan AI.

Layar ini memiliki item berikut:

- **Layanan AI**: layanan yang sedang digunakan. Daftar ini hanya menampilkan layanan yang sudah diatur, serta **Tidak ada**.
- Item setelan untuk setiap layanan: **Setelan SandFly Pro**, **Setelan Gemini**, **Setelan Claude**, **Setelan OpenAI (ChatGPT)**, **Setelan Mistral**, **Setelan Groq**, **Setelan OpenRouter**, **Setelan Ollama Cloud**, **Setelan Qwen**, **Setelan Zhipu GLM**, **Setelan Baichuan**, **Setelan DeepSeek**, **Setelan Kimi**, **Setelan Hunyuan**, **Setelan SiliconFlow**, dan **Setelan Layanan lainnya**. Item untuk layanan yang sedang dipakai bertuliskan **Sedang digunakan**, sedangkan item untuk layanan yang belum diatur bertuliskan **Belum disiapkan**. **Setelan SandFly Pro** tersedia pada build dengan SandFly Pro dan membuka layar SandFly Pro. Lihat [](#pro).
- **AI di perangkat**: lihat [](#descriptions-on-device).
- **Biarkan model pengenalan tetap dimuat** dan **Server unduhan model**: lihat [](#descriptions-model-settings).

#### Menyiapkan layanan {#descriptions-set-up-service}

1. Buka **Layanan AI** lalu buka setelan layanan tertentu, misalnya **Setelan Claude**.
2. Untuk **Layanan lainnya**, pilih **Alamat** lalu masukkan alamat layanan yang memiliki API obrolan kompatibel OpenAI dan menerima input gambar. Alamat harus diawali dengan `https://`.
3. Pilih item kunci API, seperti **Kunci API Claude**. Jika layanan menyediakannya, pilih **Dapatkan kunci** untuk membuka situs web layanan tersebut dan membuat kunci. Tempel kunci lalu pilih **Simpan**.
4. Pilih item model, seperti **Model Claude**. SandFly mengambil daftar model untuk kunci Anda. Pilih salah satu model, atau pilih **Masukkan nama model** lalu ketik namanya. Jika daftar tidak dapat diambil, SandFly menjelaskan alasannya dan membuka kolom untuk nama model.

Suatu layanan dianggap sudah diatur setelah memiliki alamat, kunci API, dan model. Jika belum ada layanan lain yang digunakan, SandFly langsung memakai layanan tersebut begitu selesai diatur. Untuk menghapus kunci, kosongkan kolom lalu pilih **Simpan**. Jika layanan yang sedang digunakan kehilangan kunci atau modelnya, SandFly beralih ke layanan berikutnya yang sudah diatur. Jika tidak ada, SandFly beralih ke **Tidak ada**.

Kunci API hanya tersimpan di ponsel. Setelan yang diekspor tidak menyertakannya. Lihat [](#profiles).

#### Memilih layanan AI {#descriptions-choose-service}

1. Buka **Layanan AI** > **Layanan AI**.
2. Pilih layanan, atau pilih **Tidak ada** untuk menonaktifkan deskripsi AI.

Jika belum ada layanan yang diatur, SandFly mengucapkan "Tidak ada layanan AI yang disiapkan. Atur satu dalam setelannya di bawah."

Kontrol pembacaan **Layanan AI**, yang nonaktif secara default, mengubah setelan yang sama. Geser ke atas atau ke bawah untuk beralih di antara layanan yang sudah disiapkan. SandFly mengucapkan nama layanannya. **Tidak ada** dan SandFly Pro tanpa lisensi tidak termasuk dalam pilihan ini. Jika layanan yang disiapkan kurang dari dua, SandFly mengucapkan "Hanya satu layanan AI yang disiapkan" atau "Tidak ada layanan AI yang disiapkan", dan setelannya tidak berubah. Tambahkan kontrol ini di **Kontrol pembacaan**. Lihat [](#commands). Perintah **Layanan AI: nilai berikutnya** dan **Layanan AI: nilai sebelumnya**, di **Nilai setelan**, melakukan hal yang sama tanpa memilih kontrol pembacaan.

#### Setelan Gemini {#descriptions-gemini}

**Setelan Gemini** memiliki dua item:

- **Kunci API Gemini**: kunci dari Google AI Studio. **Dapatkan kunci** membuka halaman untuk membuatnya.
- **Model Gemini**: **Otomatis** (default) menggunakan Gemini Flash, lalu Gemini Flash-Lite saat Flash sibuk. Anda dapat memilih model lain dari daftar yang diberikan Google untuk kunci Anda. SandFly akan beralih ke Flash-Lite saat model tersebut sibuk atau terlalu lama menjawab.

Pada paket gratis Gemini API, Google dapat menyimpan data yang diterima dan menggunakannya untuk meningkatkan produknya.

#### Data yang dikirim {#descriptions-privacy}

Gambar, screenshot, dan pertanyaan yang Anda kirim akan diteruskan ke layanan yang sedang digunakan. Sebelum gambar pertama dikirim ke suatu layanan, SandFly menampilkan dialog berisi alamat layanan tersebut, seperti api.anthropic.com. Persyaratan dan kebijakan privasi layanan itu sendiri yang berlaku. Saat **Gunakan AI di perangkat** aktif, tidak ada data yang dikirim.

### AI di perangkat {#descriptions-on-device}

AI di perangkat mendeskripsikan gambar dan layar menggunakan model visi di ponsel. Fitur ini tidak memerlukan kunci API dan tidak mengirim apa pun melalui jaringan setelah model diunduh. Prosesnya lebih lambat daripada layanan AI dan menggunakan lebih banyak baterai. Fitur ini memerlukan ponsel dengan prosesor ARM 64-bit dan memori yang cukup untuk modelnya, serta tidak tersedia di jam tangan.

Saat **Gunakan AI di perangkat** aktif, **Deskripsikan gambar**, **Deskripsikan layar**, pertanyaan, Pengenalan langsung, dan terjemahan dengan opsi **Layanan AI** menggunakan model di ponsel alih-alih layanan AI. **Deskripsi gambar mendetail** juga harus aktif.

#### Mengunduh model {#descriptions-on-device-download}

1. Buka **Layanan AI** > **AI di perangkat**.
2. Pilih **Model**, lalu pilih salah satu model. Daftar menampilkan ukuran setiap model, memori yang dibutuhkan, dan catatan singkat. Daftar juga menandai model yang **diunduh** atau **eksperimental**. Daftar ini hanya menampilkan model yang muat di memori ponsel dan model yang sudah diunduh.
3. Pilih **Unduh model**, yang menampilkan ukuran unduhan. Jika Anda memakai data seluler, SandFly bertanya terlebih dahulu.
4. Setelah unduhan selesai, aktifkan **Gunakan AI di perangkat**.

**Gemma 4 E2B** adalah model yang disarankan, dan menjadi default di ponsel yang memorinya mencukupi. Di ponsel lain, default-nya adalah model terbesar yang muat. Model eksperimental mungkin tidak bekerja dengan baik.

Selama model diunduh, item tersebut menampilkan progres dan sisa waktu. SandFly mengumumkan setiap kali unduhan mencapai seperempat bagian, lalu mengucapkan "Model diunduh" di akhir. Sebuah notifikasi juga menampilkan progresnya. Ketuk item untuk menghentikan unduhan. Saat Anda memulainya lagi, unduhan dilanjutkan dari titik terakhir. SandFly memeriksa file yang diunduh dan menghapusnya jika tidak cocok dengan modelnya.

Untuk menggunakan file yang Anda unduh sendiri, pilih **Gunakan file model dari penyimpanan** lalu pilih file `.litertlm` Gemma 4. SandFly mengucapkan "Model diinstal" atau memberi tahu bahwa file tersebut bukan model yang didukung.

#### Setelan AI di perangkat {#descriptions-on-device-settings}

- **Gunakan AI di perangkat** (nonaktif secara default): membuat deskripsi dengan model di ponsel. Tersedia setelah model diunduh.
- **Model**: model yang digunakan.
- **Unduh model**: mengunduh model yang dipilih. Ditampilkan sampai model selesai diunduh.
- **Kelola model**: menampilkan daftar model yang sudah diunduh dan unduhan yang belum selesai, beserta penyimpanan yang terpakai. Pilih salah satu lalu pilih **Hapus** untuk mengosongkan penyimpanannya.
- **Gunakan file model dari penyimpanan**: menginstal file model dari penyimpanan.
- **Batas waktu jawaban**: berapa lama SandFly menunggu jawaban, dari 1 hingga 10 menit. Default-nya adalah 3 menit. **Deskripsikan layar** memerlukan waktu lebih lama daripada **Deskripsikan gambar**.
- **Deskripsi layar yang lebih singkat** (nonaktif secara default): **Deskripsikan layar** meminta lebih sedikit detail agar jawaban lebih cepat datang.
- **Prosesor**: **CPU**, **GPU**, atau **NPU**. Hanya ditampilkan di ponsel yang dapat menjalankan model di GPU atau NPU. Dengan **NPU**, SandFly menggunakan model khusus untuk NPU tersebut, dan **Model** tidak dapat diubah.

Saat model dimuat dan bekerja, SandFly memutar suara secara berulang. Anda dapat menonaktifkan suara ini di **Suara dan getaran** > **Suara dan getaran satu per satu**. Lihat [](#sound).

#### Memori {#descriptions-on-device-memory}

Model dimuat pada permintaan pertama dan berjalan di prosesnya sendiri. Sebelum memuat model, SandFly memeriksa apakah ponsel memiliki memori kosong yang cukup. Jika tidak cukup, SandFly mengucapkan "Memori kosong tidak cukup" beserta nama modelnya. Jika Android menghentikan model karena kekurangan memori, SandFly memberi tahu Anda. Dalam kedua kasus tersebut, tutup beberapa aplikasi atau pilih model yang lebih kecil.

**Abaikan batas memori AI di perangkat**, di **Setelan developer**, menampilkan semua model, memungkinkan Anda memilih model apa pun yang sudah diunduh, dan memuatnya tanpa memeriksa memori kosong.

### Setelan model {#descriptions-model-settings}

Dua setelan berikut terdapat di **Layanan AI** dan di **Mengetik** > **Dikte**, dan berlaku untuk model AI di perangkat maupun model pengenalan ucapan untuk dikte:

- **Biarkan model pengenalan tetap dimuat**: mengatur berapa lama model tetap berada di memori setelah digunakan: **Keluarkan dari memori setelah digunakan** (default), **5 menit**, **15 menit**, **30 menit**, atau **Sampai SandFly berhenti**. Model AI di perangkat selalu tetap dimuat setidaknya 2 menit setelah sebuah permintaan. Dengan **Sampai SandFly berhenti**, model AI di perangkat dimuat saat SandFly mulai berjalan.
- **Server unduhan model**: **Hugging Face** (default) atau **hf-mirror.com**, untuk wilayah yang tidak dapat mengakses Hugging Face.

### Pengenalan langsung {#descriptions-live-recognition}

Layar **Pengenalan langsung** mendeskripsikan apa yang dilihat kamera belakang. Buka layar ini dari **Deskripsi gambar dan layar** > **Pengenalan langsung**, atau melalui kontrol pembacaan atau perintah **Pengenalan langsung**. Fitur ini tidak tersedia di perangkat tanpa kamera, di jam tangan, atau di TV.

Setiap mode diaktifkan dan dinonaktifkan secara terpisah dengan kotak centang di layar:

- **Pemandangan**: mendeskripsikan apa yang dilihat kamera.
- **Teks**: membacakan teks dalam tampilan kamera. Teks dikenali di ponsel.
- **Tunjuk dan Baca**: membacakan teks di ujung jari Anda dalam tampilan kamera. Pilih **Baca di atas ujung jari** (default) atau **Baca di bawah ujung jari**.
- **Deteksi orang**: mendeskripsikan orang dalam tampilan kamera.
- **Deteksi pintu**: mendeskripsikan pintu dalam tampilan kamera.
- **Deteksi warna**: menyebutkan warna di bagian tengah tampilan kamera, seperti "Biru tua", "Abu-abu muda", atau "Merah dan putih", atau mengucapkan "Warna campuran" jika tidak ada warna yang cukup menutupi bagian tersebut. Warna diucapkan saat berubah dan sudah bertahan sekitar setengah detik. Terang dan gelapnya bergantung pada cahaya dan eksposur otomatis kamera. Warna dikenali di ponsel.
- **Tingkat cahaya**: membacakan tingkat cahaya dari sensor cahaya: **Gelap**, **Cahaya redup**, **Cahaya normal**, **Cahaya terang**, atau **Cahaya sangat terang**. Mode ini tidak menggunakan kamera.
- **Nada tingkat cahaya**: memutar nada terus-menerus yang tinggi nadanya naik sesuai kecerahan tampilan kamera belakang, dari 200 Hz dalam gelap hingga 1600 Hz di bawah sinar matahari. Nada diputar pada volume suara SandFly, dan berhenti saat mode dinonaktifkan atau layar ditinggalkan.

Baris status menampilkan **Kamera aktif** atau **Kamera nonaktif**. SandFly meminta izin kamera saat pertama kali suatu mode membutuhkannya.

Deskripsi bisa saja salah. Jangan mengandalkannya agar dapat bergerak dengan aman. Orang dan pintu dideskripsikan hanya dari gambar, tanpa informasi jarak.

#### Menggunakan Pengenalan langsung {#descriptions-live-recognition-use}

1. Pilih kontrol pembacaan **Pengenalan langsung**. Fitur ini memerlukan Android 11 atau versi lebih baru.
2. Geser ke atas atau ke bawah untuk memilih mode. SandFly menyebutkan mode dan apakah mode tersebut aktif.
3. Ketuk dua kali untuk mengaktifkan atau menonaktifkan mode tersebut. Jika layar Pengenalan langsung belum terbuka, layar itu akan terbuka.

Saat layar terbuka:

- Ketuk dengan dua jari: menghentikan ucapan dan membacakan hasil berikutnya.
- Gosok dengan dua jari: menonaktifkan semua mode dan menutup layar.

Hasil dibacakan dengan suara SandFly, termasuk saat audio lain sedang diputar. Mikrofon tidak digunakan. Meninggalkan layar, mengunci ponsel, menjeda SandFly, atau menonaktifkan semua mode akan mematikan kamera.

#### Apa yang mendeskripsikan tampilan kamera {#descriptions-live-recognition-ai}

**Pemandangan**, **Tunjuk dan Baca**, **Deteksi orang**, dan **Deteksi pintu** menggunakan sumber yang sama dengan **Deskripsikan gambar**:

- Jika **Gunakan AI di perangkat** aktif, SandFly memakai model di ponsel. Tidak ada data yang keluar dari ponsel.
- Jika tidak, SandFly memakai layanan AI yang sedang digunakan, seperti SandFly Pro atau Claude. Layanan ini menerima gambar kamera setiap beberapa detik.
- Jika Gemini menjadi layanan AI, SandFly memakai Gemini Live API, yang menerima gambar kamera setiap beberapa detik. SandFly membacakan teks jawabannya.

Sebelum gambar kamera pertama dikirim ke suatu layanan, SandFly menampilkan dialog **Kirim gambar kamera ke Gemini?**, atau untuk layanan lain **Kirim gambar kamera ke** diikuti alamat layanan tersebut, seperti api.anthropic.com. Pilih **Kirim gambar**. Jika kunci API ditolak, batas penggunaan habis, atau terjadi kegagalan berulang, mode yang menggunakan AI dinonaktifkan, dan SandFly memberi tahu Anda.

### Pesan error {#descriptions-errors}

Jika deskripsi atau jawaban gagal, SandFly memberi tahu penyebabnya, misalnya:

- Layanan tidak menerima kunci API. Periksa kunci di **Layanan AI**.
- Batas penggunaan layanan atau kunci API telah tercapai.
- Layanan sedang sibuk, atau membutuhkan waktu terlalu lama untuk menjawab.
- Tidak ada koneksi internet.
- "Tidak ada layanan AI yang digunakan. Siapkan satu atau pilih salah satu di Setelan layanan AI."

Untuk error lainnya, SandFly membacakan kode error dan kalimat pertama dari pesan layanan, dalam bahasa Inggris.
