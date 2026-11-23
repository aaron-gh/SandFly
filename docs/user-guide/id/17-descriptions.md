## Deskripsi gambar dan layar {#descriptions}

SandFly dapat mendeskripsikan gambar yang difokuskan, seluruh layar, dan tampilan kamera belakang, serta menjawab pertanyaan tentangnya. Deskripsi berasal dari salah satu sumber berikut:

- Layanan AI, seperti Gemini, Claude, atau OpenAI, dengan kunci API Anda sendiri. Lihat [](#descriptions-ai-services).
- SandFly Pro, tanpa kunci API. Lihat [](#pro).
- Model yang berjalan di ponsel, tanpa ada data yang dikirim melalui jaringan. Lihat [](#descriptions-on-device).

Teks dalam gambar dikenali langsung di ponsel dan tidak memerlukan salah satu sumber di atas.

Setelan untuk deskripsi berada di dua layar: **Deskripsi gambar dan layar** untuk mengaktifkan deskripsi dan membuka Pengenalan langsung; **Layanan AI** untuk memilih layanan pembuat deskripsi. Deskripsi memerlukan ponsel atau tablet: fitur ini tidak tersedia di smartwatch dan TV.

### Menyiapkan deskripsi gambar dan layar {#descriptions-set-up}

1. Siapkan layanan AI, SandFly Pro, atau AI di perangkat. Lihat [](#descriptions-set-up-service), [](#pro), atau [](#descriptions-on-device-download).
2. Buka **Deskripsi gambar dan layar** lalu aktifkan **Deskripsi gambar mendetail**.
3. Baca dialog tentang data yang dikirim dan tujuannya, lalu pilih **Aktifkan**.

Jika Anda menjalankan **Deskripsikan gambar** saat **Deskripsi gambar mendetail** nonaktif, SandFly menampilkan dialog **Dapatkan deskripsi gambar mendetail?**. **Aktifkan** akan mengaktifkan setelan tersebut dan mendeskripsikan gambar; **Lain kali** membiarkannya tetap nonaktif, dan SandFly hanya membacakan teks dalam gambar.

Saat pertama kali Anda menjalankan **Deskripsikan layar**, SandFly menampilkan dialog **Deskripsi layar** mengenai screenshot yang dikirim. Pilih **Aktifkan** untuk mendeskripsikan layar.

### Setelan deskripsi gambar dan layar {#descriptions-settings}

**Deskripsi gambar dan layar** memiliki setelan berikut:

- **Deskripsi gambar mendetail** (nonaktif secara default): **Deskripsikan gambar** mengirim gambar yang difokuskan ke layanan AI yang sedang digunakan, atau ke model di perangkat, untuk mendapatkan deskripsi lengkap.
- **Deskripsi otomatis untuk teks dalam gambar**: SandFly membacakan teks dalam gambar saat fokus berpindah ke gambar tersebut. Pilih **Semua gambar**, **Hanya gambar tidak berlabel** (default), atau **Jangan deskripsikan gambar**. Teks dikenali di ponsel.
- **Pengenalan langsung**: membuka layar Pengenalan langsung. Lihat [](#descriptions-live-recognition). Hanya tersedia di perangkat dengan kamera.

### Mendeskripsikan gambar {#descriptions-describe-image}

1. Pindahkan fokus ke gambar.
2. Jalankan **Deskripsikan gambar**. Perintah ini ada di Menu SandFly, dan Anda dapat menetapkannya ke gestur atau tombol. Lihat [](#descriptions-commands).

SandFly membuka jendela **Deskripsi gambar** berisi deskripsi dan teks yang ditemukan dalam gambar. Di jendela tersebut Anda dapat menyalin deskripsi, memberi peringkat, dan mengajukan pertanyaan tentang gambar. Lihat [](#descriptions-questions).

Jika deskripsi mendetail tidak dapat dibuat, SandFly membacakan teks dalam gambar dan menjelaskan alasannya, misalnya "Tidak bisa mendapatkan deskripsi gambar yang lebih mendetail" beserta alasannya, atau "Hubungkan ke internet untuk mendapatkan deskripsi gambar yang lebih mendetail."

Agar deskripsi dibacakan lantang tanpa membuka jendela, jalankan **Ucapkan deskripsi gambar** sebagai gantinya.

### Mendeskripsikan layar {#descriptions-describe-screen}

Jalankan **Deskripsikan layar**. SandFly mengambil screenshot, mengirimkannya bersama item di layar, lalu membuka jendela **Deskripsi layar** berisi deskripsi layar. Anda dapat mengajukan pertanyaan tentang layar di jendela tersebut. Lihat [](#descriptions-questions).

Agar deskripsi dibacakan lantang tanpa membuka jendela, jalankan **Ucapkan deskripsi layar** sebagai gantinya.

**Deskripsikan layar** hanya tersedia dalam bahasa ponsel yang didukungnya. Screenshot jendela aman, seperti beberapa aplikasi perbankan, tidak dapat diambil.

### Mengajukan pertanyaan tentang gambar atau layar {#descriptions-questions}

Pertanyaan dikirim ke layanan AI atau model di perangkat yang sama dengan yang membuat deskripsi.

Di jendela **Deskripsi gambar** atau **Deskripsi layar**:

- **Tanyakan dengan suara** merekam pertanyaan menggunakan pengenal ucapan yang dipilih di **Mengetik** > **Dikte**, mengirimkannya, lalu membacakan jawabannya. **Berhenti mendengarkan** mengakhiri rekaman. Lihat [](#typing).
- **Ketikkan pertanyaan** membuka kolom teks. Ketik pertanyaan lalu pilih **Tanya**.

Setiap jawaban tetap berada di jendela tersebut, sehingga Anda dapat mengajukan pertanyaan lanjutan tentang gambar atau layar yang sama.

Untuk bertanya tanpa membuka deskripsi terlebih dahulu, jalankan **Tanyakan tentang gambar** atau **Tanyakan tentang layar**:

1. Pindahkan fokus ke gambar, untuk **Tanyakan tentang gambar**.
2. Jalankan perintah tersebut. SandFly mengambil screenshot gambar atau layar lalu mulai merekam.
3. Ucapkan pertanyaan Anda. Rekaman berhenti setelah jeda bicara, atau saat Anda menjalankan perintah itu lagi.

SandFly mengirim pertanyaan dan membacakan jawabannya. Jika rekaman tidak dapat dimulai, kolom teks akan terbuka sebagai gantinya: ketik pertanyaan lalu pilih **Tanya**. Sebelum Anda menyetujui pengiriman screenshot ke layanan AI, perintah ini akan menjalankan **Deskripsikan gambar** atau **Deskripsikan layar** yang menampilkan dialog persetujuan tersebut.

### Perintah untuk deskripsi {#descriptions-commands}

Perintah berikut berada dalam kategori **Deskripsi gambar dan layar** di **Perintah**, tempat Anda dapat menetapkannya ke gestur dan tombol serta menambahkannya ke Menu SandFly. Lihat [](#commands).

- **Ambil foto dan deskripsikan**: mengambil foto dengan kamera belakang dan mendeskripsikannya. Lihat [](#descriptions-take-picture).
- **Deskripsikan gambar**, **Ucapkan deskripsi gambar**, dan **Tanyakan tentang gambar**: mendeskripsikan gambar yang difokuskan dalam jendela, mendeskripsikannya secara lisan, atau menjawab pertanyaan lisan tentangnya.
- **Deskripsikan layar**, **Ucapkan deskripsi layar**, dan **Tanyakan tentang layar**: sama seperti di atas, untuk seluruh layar.
- **Laporkan respons AI**: membuka laporan masalah berisi teks terakhir yang diberikan model AI, seperti deskripsi atau jawaban. Lihat [](#help).

Dengan SandFly Pro, kategori ini juga memuat perintah pengenalan layar. Lihat [](#pro-screen-recognition).

Kategori **Pengenalan langsung** memiliki perintah untuk setiap mode Pengenalan langsung, seperti **Pengenalan langsung: Pemandangan**, serta **Hentikan Pengenalan langsung**. Lihat [](#descriptions-live-recognition).

Untuk gestur dan pintasan keyboard default, lihat [](#appendix-gestures) dan [](#appendix-keyboard-shortcuts).

#### Kontrol pembacaan Tindakan AI {#descriptions-ai-actions}

Kontrol pembacaan **Tindakan AI** menjalankan **Deskripsikan layar**, **Ucapkan deskripsi layar**, **Tanyakan tentang layar**, **Deskripsikan gambar**, **Ucapkan deskripsi gambar**, dan **Tanyakan tentang gambar**, serta dengan SandFly Pro, **Mulai atau hentikan tugas agen AI** dan **Ketik tugas agen AI**. Fitur ini memerlukan Android 11 atau versi lebih baru.

1. Pilih kontrol pembacaan **Tindakan AI**.
2. Geser ke atas atau ke bawah untuk memilih tindakan.
3. Ketuk dua kali untuk menjalankannya.

Ketuk dua kali akan menjalankan tindakan yang diumumkan kontrol pembacaan sampai fokus berpindah. **Tindakan AI yang disertakan**, di **Kontrol pembacaan**, menentukan tindakan mana saja yang ditawarkan kontrol pembacaan; semuanya disertakan secara default. Tindakan yang tidak tersedia tidak ditampilkan. Untuk kontrol pembacaan, lihat [](#commands).

### Mengambil foto dan mendeskripsikannya {#descriptions-take-picture}

**Ambil foto dan deskripsikan** membuka layar yang mengambil satu foto dengan kamera belakang lalu mendeskripsikannya seperti **Deskripsikan gambar**, lengkap dengan jendela yang sama untuk mengajukan pertanyaan.

1. Jalankan **Ambil foto dan deskripsikan**. SandFly mengucapkan "Tahan ponsel agar tetap stabil" lalu mengambil foto.
2. Baca deskripsi, dan ajukan pertanyaan tentang foto tersebut jika diperlukan.
3. Untuk mengambil foto lagi, pilih **Ambil foto lain**.

SandFly meminta izin kamera pada penggunaan pertama.

### Layanan AI {#descriptions-ai-services}

**Layanan AI**, di setelan utama SandFly, menentukan layanan yang mendeskripsikan gambar dan layar serta menjawab pertanyaan tentangnya. Layanan yang sama digunakan oleh Pengenalan langsung, terjemahan dengan opsi **Layanan AI**, dan fitur lain yang menggunakan AI.

Layar ini memiliki item berikut:

- **Layanan AI**: layanan yang sedang digunakan. Daftar ini hanya menampilkan layanan yang sudah diatur, serta **Tidak ada**.
- Item setelan untuk setiap layanan: **Pengaturan SandFly Pro**, **Setelan Gemini**, **Pengaturan Claude**, **Pengaturan OpenAI (ChatGPT)**, **Pengaturan Mistral**, **Pengaturan Groq**, **Pengaturan OpenRouter**, **Pengaturan Ollama Cloud**, **Pengaturan Qwen**, **Pengaturan Zhipu GLM**, **Pengaturan Baichuan**, **Pengaturan DeepSeek**, **Pengaturan Kimi**, **Pengaturan Hunyuan**, **Pengaturan SiliconFlow**, dan **Pengaturan Layanan lainnya**. Item untuk layanan yang sedang dipakai bertuliskan **Digunakan**; item untuk layanan yang belum diatur bertuliskan **Tidak diatur**. **Pengaturan SandFly Pro** tersedia pada build dengan SandFly Pro dan membuka layar SandFly Pro. Lihat [](#pro).
- **AI di perangkat**: lihat [](#descriptions-on-device).
- **Pertahankan model pengenalan dimuat** dan **Server unduhan model**: lihat [](#descriptions-model-settings).

#### Menyiapkan layanan {#descriptions-set-up-service}

1. Buka **Layanan AI** lalu buka setelan layanan tertentu, misalnya **Pengaturan Claude**.
2. Untuk **Layanan lainnya**, pilih **Alamat** lalu masukkan alamat layanan dengan API obrolan dan masukan gambar yang kompatibel dengan OpenAI. Alamat harus diawali dengan `https://`.
3. Pilih item kunci API, seperti **Kunci API Claude**. Pilih **Dapatkan kunci** untuk membuka situs web layanan tersebut dan membuat kunci, jika layanan menyediakannya. Tempel kunci lalu pilih **Simpan**.
4. Pilih item model, seperti **Model Claude**. SandFly mengambil daftar model untuk kunci Anda. Pilih salah satu model, atau pilih **Masukkan nama model** lalu ketik namanya. Jika daftar tidak dapat diambil, SandFly menjelaskan alasannya dan membuka kolom untuk nama model.

Suatu layanan dianggap sudah diatur setelah memiliki alamat, kunci API, dan model. Jika belum ada layanan lain yang digunakan, SandFly langsung memakai layanan tersebut begitu selesai diatur. Untuk menghapus kunci, kosongkan kolom lalu pilih **Simpan**. Jika layanan yang sedang digunakan kehilangan kunci atau modelnya, SandFly beralih ke layanan berikutnya yang sudah diatur, atau **Tidak ada**.

Kunci API tetap tersimpan di ponsel: setelan yang diekspor tidak menyertakannya. Lihat [](#profiles).

#### Memilih layanan AI {#descriptions-choose-service}

1. Buka **Layanan AI** > **Layanan AI**.
2. Pilih layanan, atau pilih **Tidak ada** untuk menonaktifkan deskripsi AI.

Jika belum ada layanan yang diatur, SandFly mengucapkan "Tidak ada layanan AI yang disiapkan. Atur satu dalam pengaturannya di bawah."

#### Setelan Gemini {#descriptions-gemini}

**Setelan Gemini** memiliki dua item:

- **Kunci API Gemini**: kunci dari Google AI Studio. **Dapatkan kunci** membuka halaman untuk membuatnya.
- **Model Gemini**: **Otomatis** (default) menggunakan Gemini Flash, lalu Gemini Flash-Lite saat Flash sibuk. Anda dapat memilih model lain dari daftar yang diberikan Google untuk kunci Anda; SandFly akan menggunakan Flash-Lite saat model tersebut sibuk atau terlalu lama menjawab.

Pada paket gratis Gemini API, Google dapat menyimpan data yang diterima dan menggunakannya untuk meningkatkan produknya.

#### Data yang dikirim {#descriptions-privacy}

Gambar, screenshot, dan pertanyaan yang Anda kirim akan diteruskan ke layanan yang sedang digunakan. Sebelum gambar pertama dikirim ke suatu layanan, SandFly menampilkan dialog berisi nama layanan tersebut; persyaratan dan kebijakan privasi layanan itu berlaku. Saat **Gunakan AI di perangkat** aktif, tidak ada data yang dikirim.

### AI di perangkat {#descriptions-on-device}

AI di perangkat mendeskripsikan gambar dan layar menggunakan model visi di ponsel, tanpa kunci API dan tanpa mengirim apa pun melalui jaringan setelah model didownload. Prosesnya lebih lambat daripada layanan AI dan menggunakan lebih banyak baterai. Fitur ini memerlukan ponsel dengan prosesor ARM 64-bit dan memori yang cukup untuk modelnya, serta tidak tersedia di smartwatch.

Saat **Gunakan AI di perangkat** aktif, **Deskripsikan gambar**, **Deskripsikan layar**, pertanyaan, Pengenalan langsung, dan terjemahan dengan opsi **Layanan AI** menggunakan model di ponsel alih-alih layanan AI. **Deskripsi gambar mendetail** juga harus aktif.

#### Mendownload model {#descriptions-on-device-download}

1. Buka **Layanan AI** > **AI di perangkat**.
2. Pilih **Model**, lalu pilih salah satu model. Daftar menampilkan ukuran setiap model, memori yang dibutuhkan, dan catatan singkat, serta menandai model yang **didownload** atau **eksperimental**. Daftar ini hanya menampilkan model yang sesuai dengan kapasitas memori ponsel dan model yang sudah didownload.
3. Pilih **Download model**. Jika menggunakan data seluler, SandFly akan bertanya terlebih dahulu.
4. Setelah download selesai, aktifkan **Gunakan AI di perangkat**.

**Gemma 4 E2B** adalah model yang disarankan, dan menjadi default di ponsel dengan memori yang mencukupi. Di ponsel lain, default-nya adalah model terbesar yang muat. Model eksperimental mungkin tidak bekerja dengan baik.

Selama model didownload, item tersebut menampilkan progres dan sisa waktu, lalu SandFly mengumumkan setiap seperempat progres download dan "Model didownload" di akhir. Sebuah notifikasi menampilkan progresnya. Ketuk item untuk menghentikan; download akan dilanjutkan dari titik terakhir saat Anda memulainya lagi. SandFly memeriksa file yang didownload dan menghapusnya jika tidak cocok dengan modelnya.

Untuk menggunakan file yang Anda download sendiri, pilih **Gunakan file model dari penyimpanan** lalu pilih file `.litertlm` Gemma 4. SandFly mengucapkan "Model diinstal" atau memberi tahu bahwa file tersebut bukan model yang didukung.

#### Setelan AI di perangkat {#descriptions-on-device-settings}

- **Gunakan AI di perangkat** (nonaktif secara default): membuat deskripsi dengan model di ponsel. Tersedia setelah model didownload.
- **Model**: model yang digunakan.
- **Download model**: mendownload model yang dipilih. Ditampilkan sampai model selesai didownload.
- **Kelola model**: menampilkan daftar model yang sudah didownload dan download yang belum selesai, beserta penyimpanan yang terpakai. Pilih salah satu lalu pilih **Hapus** untuk mengosongkan penyimpanannya.
- **Gunakan file model dari penyimpanan**: menginstal file model dari penyimpanan.
- **Batas waktu jawaban**: berapa lama SandFly menunggu jawaban, dari 1 hingga 10 menit. Default-nya adalah 3 menit. **Deskripsikan layar** memerlukan waktu lebih lama daripada **Deskripsikan gambar**.
- **Deskripsi layar yang lebih singkat** (nonaktif secara default): **Deskripsikan layar** meminta lebih sedikit detail agar jawaban datang lebih cepat.
- **Prosesor**: **CPU**, **GPU**, atau **NPU**. Hanya ditampilkan di ponsel yang mendukung model berjalan di GPU atau NPU. Dengan **NPU**, SandFly menggunakan model khusus untuk NPU tersebut, dan **Model** tidak dapat diubah.

Saat model dimuat dan bekerja, SandFly memutar suara yang berulang. Anda dapat menonaktifkan suara ini di **Suara dan getaran** > **Suara dan getaran satu per satu**. Lihat [](#sound).

#### Memori {#descriptions-on-device-memory}

Model dimuat pada permintaan pertama dan berjalan di prosesnya sendiri. Sebelum memuat model, SandFly memeriksa apakah ponsel memiliki memori kosong yang cukup; jika tidak, SandFly mengucapkan "Memori kosong tidak cukup" beserta nama modelnya. Jika Android menghentikan model karena kekurangan memori, SandFly akan mengabarkannya. Dalam kedua kasus tersebut, tutup beberapa aplikasi atau pilih model yang lebih kecil.

**Abaikan batas memori AI di perangkat**, di **Setelan developer**, menampilkan semua model, memungkinkan Anda memilih model apa pun yang sudah didownload, dan memuatnya tanpa memeriksa memori kosong.

### Setelan model {#descriptions-model-settings}

Dua setelan berikut terdapat di **Layanan AI** dan di **Mengetik** > **Dikte**, dan berlaku untuk model AI di perangkat maupun model pengenalan ucapan untuk dikte:

- **Pertahankan model pengenalan dimuat**: berapa lama model tetap berada di memori setelah digunakan: **Bongkar setelah digunakan** (default), **5 menit**, **15 menit**, **30 menit**, atau **Sampai SandFly berhenti**. Model AI di perangkat selalu tetap dimuat setidaknya 2 menit setelah sebuah permintaan. Dengan **Sampai SandFly berhenti**, model AI di perangkat dimuat saat SandFly mulai berjalan.
- **Server unduhan model**: **Hugging Face** (default) atau **hf-mirror.com**, untuk wilayah yang tidak dapat mengakses Hugging Face.

### Pengenalan langsung {#descriptions-live-recognition}

Layar **Pengenalan langsung** mendeskripsikan apa yang dilihat kamera belakang. Buka dari **Deskripsi gambar dan layar** > **Pengenalan langsung**, atau melalui kontrol pembacaan maupun perintah **Pengenalan langsung**. Fitur ini tidak tersedia di perangkat tanpa kamera, di smartwatch, atau di TV.

Setiap mode diaktifkan dan dinonaktifkan secara terpisah menggunakan kotak centang di layar:

- **Pemandangan**: mendeskripsikan apa yang dilihat kamera.
- **Teks**: membacakan teks dalam tampilan kamera. Teks dikenali di ponsel.
- **Tunjuk dan baca**: membacakan teks di ujung jari Anda dalam tampilan kamera. Pilih **Baca di atas ujung jari** (default) atau **Baca di bawah ujung jari**.
- **Deteksi orang**: mendeskripsikan orang dalam tampilan kamera.
- **Deteksi pintu**: mendeskripsikan pintu dalam tampilan kamera.
- **Tingkat cahaya**: membacakan tingkat cahaya dari sensor cahaya: **Gelap**, **Cahaya redup**, **Cahaya normal**, **Cahaya terang**, atau **Cahaya sangat terang**. Mode ini tidak menggunakan kamera.

Baris status menampilkan **Kamera aktif** atau **Kamera nonaktif**. SandFly meminta izin kamera saat pertama kali suatu mode membutuhkannya.

Deskripsi bisa saja salah. Jangan mengandalkannya untuk bergerak dengan aman. Orang dan pintu dideskripsikan hanya dari gambar, tanpa jarak.

#### Menggunakan Pengenalan langsung {#descriptions-live-recognition-use}

1. Pilih kontrol pembacaan **Pengenalan langsung**. Fitur ini memerlukan Android 11 atau versi lebih baru.
2. Geser ke atas atau ke bawah untuk memilih mode. SandFly menyebutkan mode dan apakah mode tersebut aktif.
3. Ketuk dua kali untuk mengaktifkan atau menonaktifkan mode tersebut. Layar Pengenalan langsung akan terbuka jika belum terbuka.

Saat layar terbuka:

- Ketuk dua jari: menghentikan ucapan dan membacakan hasil berikutnya.
- Gosok dua jari: menonaktifkan semua mode dan menutup layar.

Hasil dibacakan menggunakan suara SandFly, termasuk saat audio lain sedang diputar. Mikrofon tidak digunakan. Meninggalkan layar, mengunci ponsel, menjeda SandFly, atau menonaktifkan semua mode akan mematikan kamera.

#### Apa yang mendeskripsikan tampilan kamera {#descriptions-live-recognition-ai}

**Pemandangan**, **Tunjuk dan baca**, **Deteksi orang**, dan **Deteksi pintu** menggunakan sumber yang sama dengan **Deskripsikan gambar**:

- Jika **Gunakan AI di perangkat** aktif, digunakan model di ponsel. Tidak ada data yang keluar dari ponsel.
- Jika tidak, digunakan layanan AI yang sedang dipakai, seperti SandFly Pro atau Claude, yang menerima gambar kamera setiap beberapa detik.
- Jika Gemini yang menjadi layanan AI, digunakan Gemini Live API yang menerima gambar kamera setiap beberapa detik. SandFly membacakan teks jawabannya.

Sebelum gambar kamera pertama dikirim ke suatu layanan, SandFly meminta persetujuan melalui dialog **Kirim gambar kamera ke** diikuti nama layanan tersebut; pilih **Kirim gambar**. Kunci API yang ditolak, batas penggunaan yang habis, atau kegagalan berulang akan menonaktifkan mode yang menggunakan AI, dan SandFly akan memberitahukannya.

### Pesan error {#descriptions-errors}

Jika deskripsi atau jawaban gagal, SandFly memberi tahu penyebabnya, misalnya:

- Layanan tidak menerima kunci API. Periksa kunci di **Layanan AI**.
- Batas penggunaan layanan atau kunci API telah tercapai.
- Layanan sedang sibuk, atau membutuhkan waktu terlalu lama untuk menjawab.
- Tidak ada koneksi internet.
- "Tidak ada layanan AI yang digunakan. Siapkan satu atau pilih salah satu di Pengaturan layanan AI."

Untuk error lainnya, SandFly membacakan kode error dan kalimat pertama dari pesan layanan dalam bahasa Inggris.
