## Skrip dan label khusus {#scripts}

Skrip mengubah cara kerja SandFly di satu aplikasi atau di semua aplikasi, atau menambahkan perintah ke SandFly. Skrip dapat mengganti label atau menyembunyikan item, mengubah ucapan SandFly, menambahkan tindakan, gestur, dan pintasan keyboard, serta menjalankan langkah-langkah saat sesuatu terjadi di layar. Anda dapat menginstal skrip buatan orang lain, atau membuatnya sendiri di pembuat skrip tanpa menulis kode.

Label khusus adalah nama yang Anda berikan pada item yang belum memiliki nama atau memiliki nama yang salah, seperti tombol tanpa label.

Skrip hanya berjalan di ponsel dan tablet. Di smartwatch dan TV, SandFly mengucapkan "Skrip tidak tersedia di perangkat ini".

Untuk menulis skrip dalam bahasa Lua, lihat [](#api-scripting).

### Skrip {#scripts-overview}

**Skrip** di setelan SandFly menampilkan daftar skrip terinstal dalam dua kelompok:

- **Skrip aplikasi**: skrip yang berjalan di aplikasi yang ditentukannya.
- **Plugin global**: skrip yang berjalan di semua aplikasi.

Setiap skrip menampilkan status aktifnya, tempat berjalannya, dan versinya. Pilih sebuah skrip untuk membuka halamannya. Lihat [](#scripts-manage).

Layar **Skrip** juga memiliki:

- **Nonaktifkan semua skrip**: menghentikan semua skrip. Skrip tetap terinstal dan mempertahankan sakelar masing-masing.
- **Skrip baru**: membuat skrip di pembuat skrip. Lihat [](#scripts-builder).
- **Buat skrip dengan SandFly Pro**: SandFly Pro menulis skrip berdasarkan deskripsi Anda. Lihat [](#scripts-builder-pro).
- **Impor skrip**: menginstal file skrip. Lihat [](#scripts-install).
- **Sumber skrip**: menampilkan daftar tempat untuk menemukan skrip. Lihat [](#scripts-sources).
- **Perbarui skrip secara otomatis** dan **Periksa pembaruan**. Lihat [](#scripts-updates).
- **Tambahkan contoh skrip**: menginstal skrip yang mengucapkan nama setiap aplikasi yang Anda buka, sebagai skrip awal untuk dipelajari. Opsi ini ditampilkan hingga Anda menginstalnya.

Skrip tidak mengubah layar milik SandFly, seperti setelan SandFly. Skrip berhenti saat layar mati—kecuali skrip yang Anda izinkan untuk tetap berjalan—serta saat SandFly dijeda.

### Menginstal skrip {#scripts-install}

Skrip berupa file `.lua` berukuran hingga 1 MB, atau file `.zip` berukuran hingga 5 MB yang berisi file `main.lua` di dalamnya.

Untuk menginstal file skrip:

1. Buka **Skrip** > **Impor skrip**.
2. Pilih file `.lua` atau `.zip`.
3. Tinjau dialog instalasi, lalu pilih **Instal**. Lihat [](#scripts-install-dialog).

Anda juga dapat membuka atau membagikan file skrip, atau tautan `https` ke file skrip, dengan **Instal skrip SandFly**, misalnya dari aplikasi perpesanan, pengelola file, atau daftar unduhan browser. Untuk tautan yang dikirim aplikasi lain, SandFly menampilkan alamatnya dan baru mendownload skrip setelah Anda memilih **Download**. Tautan ke seluruh repositori tidak dapat digunakan: buka skrip yang diinginkan di repositori, lalu bagikan tautannya.

#### Menemukan skrip di sumber skrip {#scripts-sources}

Sumber skrip adalah repositori skrip publik di GitHub, GitLab, Codeberg, Gitea, atau Bitbucket. **Skrip** > **Sumber skrip** menampilkan daftar sumber tersebut. Repositori skrip SandFly sudah tersedia dalam daftar secara default.

Untuk menambahkan sumber:

1. Buka **Skrip** > **Sumber skrip** > **Tambahkan sumber**.
2. Masukkan alamat repositori, atau cabang maupun folder di dalamnya, seperti `github.com/owner/repository`.
3. Pilih **Tambahkan**.

Pilih sebuah sumber untuk melihat daftar skripnya. Pilih skrip untuk mendownloadnya dan membuka dialog instalasi. Jika daftar gagal dimuat, pilih **Coba lagi**. **Hapus sumber** menghapus sumber dari daftar; skrip yang sudah diinstal dari sumber tersebut tetap terinstal.

#### Meninjau skrip sebelum menginstal {#scripts-install-dialog}

Sebelum SandFly menginstal skrip, dialog instalasi menampilkan:

- Asal skrip: dari file, tautan, sumber skrip, atau **Disertakan dengan SandFly.**
- Apakah SandFly menandatangani skrip tersebut. SandFly menandatangani skrip di repositorinya sendiri. Skrip yang ditandatangani menampilkan **Ditandatangani oleh SandFly.** dari mana pun asalnya. Skrip lain menampilkan **Tidak ditandatangani oleh SandFly.** Skrip bertanda tangan yang diubah setelah ditandatangani menampilkan peringatan bahwa tanda tangannya tidak cocok. Nama penulis di dalam skrip adalah apa pun yang ditulis pembuatnya; hanya tanda tangan yang membuktikan bahwa skrip berasal dari SandFly.
- **Skrip ini dapat:** izin yang dibutuhkan skrip. Izin bertanda "Izinkan hanya untuk skrip tepercaya" memberi skrip akses ke data sensitif atau ke seluruh SandFly. Lihat [](#scripts-permissions).
- **Skrip ini menambahkan:** perintah yang ditambahkan skrip, beserta gestur, tombol, item Menu SandFly, dan kontrol pembacaannya.

Jika skrip dibuat untuk versi SandFly yang berbeda, dialog memberi tahu bahwa sebagian fitur mungkin tidak berfungsi, dan tombolnya menjadi **Tetap instal**.

Mengimpor skrip yang sudah terinstal akan memperbaruinya, sambil mempertahankan data dan setelannya. Dialog menampilkan versi yang digantikan, mencantumkan izin baru, dan memberi peringatan jika versi baru memiliki penulis yang berbeda.

### Memperbarui skrip {#scripts-updates}

SandFly memeriksa versi lebih baru dari skrip yang terinstal di sumbernya masing-masing sekali sehari. Untuk memeriksa sekarang, buka **Skrip** > **Periksa pembaruan**.

Saat **Perbarui skrip secara otomatis** aktif, SandFly menginstal versi lebih baru tanpa bertanya jika penulisnya sama, tidak memerlukan izin baru, dibuat untuk versi SandFly ini, dan berasal dari sumber skrip tersebut. Versi lebih baru dari repositori SandFly hanya diinstal otomatis jika ditandatangani oleh SandFly. **Perbarui skrip secara otomatis** aktif secara default.

Versi lebih baru lainnya dicantumkan di bawah **Pembaruan** pada layar **Skrip**. Pilih salah satunya untuk membuka dialog instalasi.

### Mengelola skrip yang terinstal {#scripts-manage}

Buka **Skrip** lalu pilih skripnya. Halamannya berisi:

- **Aktifkan**: mengaktifkan atau menonaktifkan skrip.
- **Tetap berjalan saat layar mati**: membuat skrip tetap berjalan saat layar mati, seperti untuk jam bicara. Hanya ditampilkan untuk skrip yang menyediakannya, dan nonaktif saat skrip pertama kali diinstal.
- Nama, deskripsi, versi, dan penulis skrip, serta tempat skrip berjalan.
- **Edit di pembuat skrip**, untuk skrip yang dibuat di pembuat skrip, atau **Salin ke pembuat skrip** untuk skrip lain. Lihat [](#scripts-builder-manage).
- **Kompatibilitas**: ditampilkan jika skrip dibuat untuk versi SandFly yang berbeda.
- **Buka halaman beranda**: membuka halaman web skrip setelah Anda mengonfirmasi alamatnya.
- **Setelan**: setelan milik skrip, jika ada.
- **Izin**: sakelar untuk setiap izin yang dibutuhkan skrip. Lihat [](#scripts-permissions).
- **Perintah**: perintah milik skrip beserta gestur dan tombolnya. Lihat [](#scripts-commands).
- **Jalankan lebih awal** dan **Jalankan lebih akhir**: urutan skrip yang mengubah ucapan yang sama. Skrip aplikasi selalu dijalankan lebih dulu.
- **Log**: output dan error skrip sejak SandFly mulai berjalan. Lihat [](#scripts-log).
- **Hapus data tersimpan**: menghapus data yang disimpan oleh skrip.
- **Hapus**: menghapus skrip beserta kode, data, dan setelannya setelah Anda mengonfirmasi.

#### Izin skrip {#scripts-permissions}

Skrip hanya dapat menggunakan izin yang tercantum di dalamnya. Di halaman skrip, di bawah **Izin**, Anda dapat menonaktifkan masing-masing izin. Menonaktifkan suatu izin akan menghentikan bagian skrip yang membutuhkannya.

Izin yang ada meliputi:

- **Membaca layar**
- **Mengklik, men-scroll, mengetik, dan memindahkan fokus**
- **Menerima dan mengubah ucapan SandFly**
- **Membaca kolom sandi**
- **Menambahkan gestur, tombol, item menu, dan kontrol pembacaan, serta menjalankan perintah SandFly**
- **Menggunakan internet**
- **Membaca dan mengubah papan klip**
- **Kembali atau ke layar utama, membuka notifikasi, menampilkan daftar aplikasi, membuka aplikasi, setelan, dan link, mengirim intent ke aplikasi, mengubah volume, mode pendering, dan setelan perangkat seperti Wi-Fi dan senter, mengontrol media, menyetel pengatur waktu dan alarm, serta memulai panggilan dan pesan**
- **Membaca dan mengubah notifikasi yang diucapkan**
- **Melihat semua aktivitas di setiap aplikasi, termasuk pengetikan**
- **Menampilkan dialog di atas aplikasi apa pun**
- **Menjalankan kode Java dengan semua akses SandFly, di setiap aplikasi**: tidak ada izin lain yang membatasi skrip yang memiliki izin ini.

Izinkan akses internet, papan klip, sandi, tindakan sistem, notifikasi, semua aktivitas, dialog, dan kode Java hanya untuk skrip dari orang yang Anda percaya.

#### Perintah skrip {#scripts-commands}

Skrip dapat menambahkan perintah yang bisa Anda jalankan dengan gestur, pintasan keyboard, Menu SandFly, atau kontrol pembacaan. Perintah dari skrip terinstal juga terdapat dalam kelompok **Skrip** di **Perintah**, dengan nama "Nama skrip: perintah", tempat Anda dapat menetapkannya seperti perintah lainnya. Lihat [](#commands).

Untuk mengubah gestur dan tombol dari perintah skrip:

1. Buka **Skrip**, pilih skripnya, lalu pilih perintah di bawah **Perintah**.
2. Pilih salah satu opsi berikut:
   - Gestur atau tombol dalam daftar: **Nonaktifkan** mempertahankan perintah SandFly pada gestur atau tombol tersebut. **Hapus** menghapusnya dari perintah.
   - **Lakukan gestur atau tekan tombol**: menetapkan gestur berikutnya yang Anda lakukan, atau tombol yang Anda tekan bersama tombol pengubah SandFly.
   - **Pilih gestur**: menampilkan daftar gestur.
   - **Ketik tombol**: masukkan tombol seperti `sf+shift+n`, `sf+t`, `s`, atau `shift+n`.
   - **Pulihkan default**: mengembalikan gestur dan tombol bawaan skrip. Ditampilkan setelah Anda mengubahnya.

Saat skrip berjalan, gestur dan tombolnya didahulukan dari milik SandFly. Beberapa skrip hanya dimuat untuk menjalankan perintah; perintahnya hanya ada di **Perintah**. Jika Anda menjalankan perintah dari skrip yang tidak sedang berjalan di layar saat ini, SandFly mengucapkan "Skrip tidak berjalan di layar ini".

#### Log dan error skrip {#scripts-log}

**Log**, di halaman skrip, mencantumkan output dan error skrip sejak SandFly mulai berjalan, lengkap dengan waktu setiap entri. **Salin log** menyalinnya, misalnya untuk dikirim ke penulis skrip. **Hapus log** mengosongkannya.

Skrip yang mengalami error berulang akan dinonaktifkan secara otomatis, dan SandFly mengucapkan "<nama> dinonaktifkan setelah error berulang. Lihat log-nya di Skrip."

### Menonaktifkan skrip {#scripts-turn-off}

- Untuk menonaktifkan satu skrip, matikan **Aktifkan** di halamannya.
- Untuk menonaktifkan semua skrip, aktifkan **Skrip** > **Nonaktifkan semua skrip**. Setiap skrip mempertahankan sakelarnya masing-masing untuk saat Anda mengaktifkan kembali skrip.

Perintah **Aktifkan atau nonaktifkan semua skrip**, di kelompok perintah **SandFly**, berfungsi sama dengan **Nonaktifkan semua skrip**, dan mengucapkan "Skrip nonaktif" atau "Skrip aktif". Gunakan perintah ini jika ada skrip yang membuat Anda tidak bisa menggunakan aplikasi. Lihat [](#commands).

### Membuat skrip {#scripts-builder}

Pembuat skrip membuat skrip tanpa kode. Untuk memulainya:

1. Buka **Skrip** > **Skrip baru**.
2. Masukkan nama, lalu pilih **Oke**.

Pembuat skrip akan terbuka. Setiap perubahan langsung disimpan dan berlaku. Bagian-bagian skrip dikelompokkan di layarnya:

- **Nama**: nama skrip.
- **Aplikasi**: aplikasi tempat skrip berjalan. Jika kosong, skrip berjalan di **Semua aplikasi**. **Tambahkan aplikasi** menambahkan aplikasi; setiap aplikasi dapat dibatasi dengan **Hanya di aktivitas** atau **Hanya di jendela**.
- **Aturan**: mengubah item. Lihat [](#scripts-builder-rules).
- **Kontrol pembacaan**: kontrol pembacaan yang berpindah antar-item sejenis. **Tambahkan kontrol pembacaan** menambahkannya beserta item tujuannya.
- **Perintah**: langkah-langkah yang dijalankan melalui gestur, tombol, Menu SandFly, atau kontrol pembacaan. Lihat [](#scripts-builder-commands).
- **Pemicu**: langkah-langkah yang berjalan otomatis. Lihat [](#scripts-builder-triggers).
- **Perubahan ucapan**: mengubah ucapan SandFly. Lihat [](#scripts-builder-speech).
- **Setelan**, **Tentang**, dan **Kode**. Lihat [](#scripts-builder-settings).
- **Skrip**: **Ekspor skrip** dan **Duplikat skrip**. Lihat [](#scripts-builder-manage).

**Halaman skrip** membuka halaman skrip berisi sakelar, izin, dan log-nya. Bagian yang belum selesai menampilkan apa yang masih diperlukan, misalnya "Tidak digunakan: tetapkan item", dan tidak disertakan dalam skrip hingga selesai diatur.

#### Aturan {#scripts-builder-rules}

Aturan menentukan suatu item dan mengubahnya. Pilih **Tambahkan aturan** untuk menambahkannya.

**Item** menentukan item yang dikenai aturan, dengan pengujian yang sama seperti makro: teks dengan **Cocokkan di** dan **Ekspresi reguler**, jenis item, **Teks persis**, **ID sumber daya**, **ID sumber daya induk**, **Nama kelas**, **Peran**, **Status**, **Di dalam item**, dan **Berisi item di dalamnya**. Lihat [](#macros). **Gunakan item yang diperiksa** mengisi kolom-kolom ini dari item yang terakhir ditampilkan dengan **Periksa item yang difokuskan** atau **Salin detail item**. Lihat [](#scripts-inspect).

**Perubahan** menetapkan apa yang dilakukan aturan pada item:

- **Label**, **Ucapkan sebagai gantinya**, **Petunjuk penggunaan**, **Status**, **Peran**, dan **Judul**.
- **Sembunyikan**: **Sembunyikan item**, atau **Sembunyikan item dan item di dalamnya**.
- **Bisukan perubahan**: menghentikan SandFly membacakan perubahan pada item tersebut, atau termasuk item di dalamnya juga.
- **Baca sebagai satu item**: membacakan item beserta item di dalamnya sebagai satu kesatuan.
- **Urutan pembacaan**: **Baca sebelum item** atau **Baca setelah item**.
- **Ketuk dua kali mengaktifkan** item lain, **Angkat untuk mengaktifkan**, **Tekan lama setelah (ms)**, dan **Ketuk di layar untuk mengaktifkan**.
- **Ubah ucapan**: menghapus, mengganti, atau mengubah urutan ucapan SandFly untuk item tersebut. Lihat [](#scripts-builder-speech).
- **Atur yang dibaca**: memilih bagian yang dibacakan SandFly untuk item, sesuai urutan. **Tambahkan bagian** menambahkan teks item, teksnya sendiri, atau item di dalamnya.

**Tindakan** menambahkan entri ke menu Tindakan milik item. **Tambahkan tindakan** memilih **Fungsi**: **Klik item**, **Klik lama item**, **Pindahkan fokus ke item**, **Scroll maju item**, **Scroll mundur item**, **Ucapkan teks**, atau **Ucapkan teks item**, serta apakah tampil sebelum atau sesudah tindakan aplikasi. **Tombol di dalam sebagai tindakan** menjadikan tombol di dalam item sebagai tindakannya, dengan opsi **Tombol yang dikecualikan**.

Untuk merekam tindakan:

1. Di bawah **Tindakan**, pilih **Rekam tindakan**. Aplikasi tujuan skrip akan terbuka.
2. Tekan atau tekan lama item yang diinginkan, menggunakan SandFly atau sentuhan langsung.
3. Buka notifikasi **Merekam tindakan**.
4. Pilih **Simpan**, atau **Rekam lagi**.

Jika aturan merujuk pada item yang baru Anda periksa, kelompok **Item yang diperiksa** menyediakan **Baca sebagai satu item, dengan tombolnya sebagai tindakan** dan **Pilih yang dibaca**, yang mencantumkan bagian-bagian yang dibacakan SandFly untuk item tersebut: bagian yang tidak dicentang akan dihapus dari setiap item serupa. **Aturan untuk** menentukan apakah aturan berlaku bagi **Setiap baris seperti baris tempatnya berada** atau **Setiap item seperti item yang diperiksa**.

**Duplikat aturan** menyalin aturan di skrip yang sama, **Salin aturan ke skrip** menyalinnya ke skrip buatan lain, dan **Hapus aturan** menghapusnya.

#### Perintah {#scripts-builder-commands}

**Tambahkan perintah** menambahkan perintah baru beserta judulnya. Di layar perintah:

- **Langkah**: langkah-langkah yang dijalankan perintah, dibuat di editor yang sama dengan langkah makro. Lihat [](#macros).
- **Gestur dan pintasan keyboard**: menetapkan perintah di ponsel ini. Lihat [](#scripts-commands).
- **Default saat dibagikan**: gestur, tombol, **Di menu SandFly**, dan **Sebagai kontrol pembacaan** yang dimiliki perintah saat skrip diinstal di perangkat lain.
- **Ganti nama** dan **Hapus**.

#### Pemicu {#scripts-builder-triggers}

**Tambahkan pemicu** menambahkan langkah yang berjalan secara otomatis:

- **Saat fokus berpindah ke item**
- **Saat aplikasi dibuka** dan **Saat aplikasi ditutup**
- **Saat jendela berubah**
- **Saat pengumuman berisi teks** dan **Saat notifikasi berisi teks**
- **Saat teks berubah**, **Saat layar berubah**, **Saat daftar di-scroll**, **Saat kursor berpindah**, **Saat item diklik**, **Saat item diklik lama**, dan **Saat kolom edit mendapat fokus input**, untuk item apa pun atau satu item tertentu
- **Setiap sejumlah menit** dan **Setiap hari pada waktu tertentu**
- **Saat setelan tombol ditekan**

Pemicu berwaktu hanya berjalan saat skrip sedang berjalan.

#### Perubahan ucapan {#scripts-builder-speech}

**Tambahkan perubahan ucapan** mengubah apa yang diucapkan SandFly. **Ucapan yang diubah** memilih **Ucapan untuk item**, **Notifikasi** (secara opsional dari satu **Aplikasi**), **Pengumuman**, atau **Semua ucapan**, dengan **Berisi teks (kosong: apa pun)**.

**Suntingan** menerapkan perubahan berikut secara berurutan: **Hapus teks**, **Ganti teks**, **Hapus bagian yang berulang**, **Hapus bagian yang berisi teks**, **Pertahankan bagian pertama**, **Ucapkan sebagai gantinya**, **Ucapkan sebelumnya**, **Ucapkan setelahnya**, dan **Jangan ucapkan apa pun**. Bagian adalah potongan teks di antara tanda koma. Di **Ucapkan sebagai gantinya**, **Ucapkan sebelumnya**, dan **Ucapkan setelahnya**, `{text}` mewakili apa yang seharusnya diucapkan SandFly. **Pratinjau pada item yang diperiksa** menampilkan hasilnya untuk item yang terakhir Anda periksa.

#### Setelan, Tentang, dan Kode {#scripts-builder-settings}

- **Setelan** > **Tambahkan setelan**: menambahkan **Sakelar**, **Daftar**, **Teks**, **Angka**, atau **Tombol** ke halaman skrip. Langkah-langkah dapat membaca nilai setelan, dan tombol menjalankan pemicu.
- **Tentang**: **Deskripsi**, **Versi**, **Halaman beranda**, **Muat hanya untuk menjalankan perintah**, dan **Tetap berjalan saat layar mati**.
- **Kode**: **Kode Lua** yang ditambahkan di akhir skrip—yang diperiksa error-nya oleh SandFly sebelum disimpan—serta **Izin tambahan** yang dibutuhkan kode tersebut.

#### Mengekspor, menduplikat, dan menyalin skrip {#scripts-builder-manage}

- **Ekspor skrip**, di pembuat skrip, menyimpan skrip sebagai file `.zip`. **Impor skrip** menginstalnya kembali, dan skrip tetap dapat diubah di pembuat skrip.
- **Duplikat skrip** membuat salinan skrip.
- **Salin ke pembuat skrip**, di halaman skrip yang tidak dibuat di pembuat skrip, membuat salinan yang dapat Anda ubah di pembuat skrip.
- **Edit di pembuat skrip**, di halaman skrip buatan pembuat skrip, membukanya kembali di pembuat skrip.

#### Membuat skrip dengan SandFly Pro {#scripts-builder-pro}

Dengan SandFly Pro, **Buat skrip dengan SandFly Pro** di layar **Skrip** menulis skrip berdasarkan deskripsi Anda mengenai apa yang harus dilakukannya. **Ubah dengan SandFly Pro**, di halaman skrip buatan dan di pembuat skrip, mengubah skrip yang sudah ada. SandFly Pro membaca layar yang sedang tampil dan menguji skrip tersebut. Di notifikasinya, SandFly Pro menampilkan permintaan untuk membuka layar lain atau mencoba bagian skrip; pilih **Selesai** atau **Balas** di sana setelah Anda melakukannya.

Skrip kemudian terbuka di pembuat skrip di bawah **Ditulis oleh SandFly Pro**, beserta ringkasan, **Terima**, **Minta perubahan**, dan **Buang**. Sebelum Anda menerimanya, skrip berjalan tanpa izin internet dan kode Java. Membuat skrip memakai batas penggunaan bulanan SandFly Pro jauh lebih banyak dibandingkan tugas lainnya. Lihat [](#pro).

### Memeriksa item {#scripts-inspect}

Perintah berikut menampilkan detail yang digunakan skrip dan makro untuk mengenali item. Perintah ini ada di kelompok **Setelan developer** dalam perintah, belum ditetapkan secara default, dan dapat ditambahkan ke Menu SandFly. Lihat [](#commands).

- **Periksa item yang difokuskan**: membuka jendela **Detail item** untuk item yang sedang difokuskan, berisi **Teks**, **Jenis item**, **Item di dalamnya**, **Detail skrip**, dan **Aplikasi**. Ketuk dua kali sebuah baris untuk menyalin nilainya. **Salin semua** menyalin semua detail. **Tambahkan aturan untuk item ini** menambahkan aturan untuk item tersebut ke skrip buatan yang ada atau ke skrip baru untuk aplikasinya, lalu membukanya.
- **Salin detail item**: menyalin detail yang sama sebagai teks, lalu mengucapkan "Detail item disalin".
- **Salin hierarki layar**: menyalin semua item di layar sebagai teks, satu baris untuk setiap item, lalu menyebutkan jumlah item yang disalin.

SandFly mengucapkan "Tidak ada item yang difokuskan" jika tidak ada item yang dapat diperiksa.

### Label khusus {#scripts-labels}

Label khusus menggantikan nama suatu item, seperti tombol tanpa label di sebuah aplikasi. SandFly membacakan label tersebut setiap kali membacakan itemnya. Anda dapat memberi label pada item yang memiliki ID sumber daya di dalam aplikasi.

#### Menambahkan atau mengedit label {#scripts-labels-add}

1. Pindahkan fokus ke item tersebut.
2. Jalankan perintah **Tambahkan atau edit label**, di kelompok perintah **Aktivasi**. Lihat [](#commands).
3. Di dialog **Tambahkan label** atau **Edit label**, masukkan label, lalu pilih **Oke**.

Untuk menghapus label, jalankan **Tambahkan atau edit label** pada item tersebut, lalu pilih **Hapus label**. SandFly mengucapkan "Tidak dapat memberi label" jika item tidak dapat diberi label.

Untuk memberi label item dari Menu SandFly, aktifkan **Tambahkan atau edit label** di **Menu SandFly**. Opsi ini nonaktif secara default. Menu SandFly kemudian menampilkan **Label** untuk item yang dapat diberi label. Saat fokus berada di item tanpa label, SandFly memberi tahu bahwa label tersedia.

#### Mengelola label khusus {#scripts-labels-manage}

**Label khusus**, di setelan SandFly, mencantumkan aplikasi yang memiliki label khusus. Pilih aplikasi untuk melihat daftar labelnya beserta waktu terakhir diubah. Pilih label untuk mengedit atau menghapusnya.

#### Mengekspor dan mengimpor label khusus {#scripts-labels-export}

Label khusus tidak disertakan dalam setelan yang diekspor. Untuk memindahkannya ke ponsel lain, ekspor ke sebuah file.

- **Ekspor label khusus**: menyimpan semua label khusus ke sebuah file dan membuka lembar berbagi Android, tempat Anda memilih tujuan pengiriman atau penyimpanannya. SandFly mengucapkan "Tidak ada yang diekspor. Anda belum menambahkan label apa pun" jika belum ada label.
- **Impor label khusus**: membaca file label. Di dialog **Impor label khusus ke SandFly**, pilih **Ganti** untuk mengganti label Anda dengan label dari file jika keduanya memberi label pada item yang sama, atau **Lewati** untuk mempertahankan label Anda.
- **Kembalikan label yang diimpor**: menghapus label yang baru diimpor dan mengembalikan label yang tergantikan oleh impor tersebut. Ditampilkan setelah proses impor.
