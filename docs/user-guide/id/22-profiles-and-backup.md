## Profil setelan dan pencadangan {#profiles}

Profil setelan adalah sekumpulan setelan bernama yang digunakan SandFly sebagai pengganti setelan Anda saat profil tersebut aktif, baik di semua tempat maupun hanya di beberapa aplikasi dan situasi tertentu. **Cadangkan dan pulihkan** menyimpan semua setelan SandFly ke sebuah file dan memulihkannya dari file. File item memuat satu atau beberapa jenis item tertentu, seperti makro atau profil gestur, untuk dibagikan atau dipindahkan ke ponsel lain.

Untuk profil gestur, yang hanya mengubah gestur, lihat [](#gestures). Untuk profil suara, lihat [](#speech).

### Profil setelan {#profiles-settings-profiles}

Profil setelan hanya menyimpan setelan yang diubah di dalamnya. Setiap setelan lain tetap seperti semula tanpa profil. **Profil setelan**, di setelan SandFly, mencantumkan daftar profil dan menandai profil yang sedang aktif dengan **Digunakan**.

Profil dapat digunakan dengan dua cara:

- **Gunakan sekarang** menggunakan profil di semua tempat, sampai Anda menghentikannya. Hanya satu profil dalam satu waktu yang digunakan dengan cara ini. Lihat [](#profiles-use).
- **Gunakan otomatis** dan **Aplikasi** menggunakan profil di konten web, selama pembacaan berkelanjutan, saat mengedit teks, di aplikasi yang Anda pilih, atau saat keyboard yang Anda pilih ditampilkan. Lihat [](#profiles-automatic).

Profil dapat menyimpan setelan utama SandFly, setelan keyboard braille, dan setelan layar braille, termasuk gestur, pintasan keyboard, profil gestur, dan profil gestur yang sedang digunakan. Profil tidak menyimpan kunci API, lisensi SandFly Pro, waktu pemeriksaan pembaruan terakhir, skrip yang terinstal beserta setelannya, draf tema suara, atau status ponsel ini, seperti tutorial dan dialog mana saja yang sudah pernah ditampilkan.

### Menambahkan profil setelan {#profiles-add}

1. Buka **Profil setelan** > **Profil setelan baru**.
2. Masukkan nama, atau biarkan kolom kosong untuk menggunakan nama yang disarankan, seperti "Profil 2".
3. Pilih **Oke**.

Profil baru belum mengatur setelan apa pun. Untuk menambahkan setelan ke dalamnya, lihat [](#profiles-change-settings).

### Mengubah setelan profil {#profiles-change-settings}

1. Buka **Profil setelan** lalu pilih profilnya.
2. Pilih **Ubah setelan**.

SandFly akan mengucapkan "Perubahan disimpan ke <nama>" dan membuka setelan SandFly dengan profil tersebut diterapkan. Setiap setelan yang Anda ubah akan masuk ke profil ini. Saat Anda kembali ke layar profil atau ke **Profil setelan**, atau membuka aplikasi lain, SandFly akan mengucapkan "Perubahan disimpan ke <nama>".

Setelan yang diubah saat profil yang aktif mengaturnya, seperti kecepatan bicara yang diubah dengan kontrol pembacaan, juga disimpan ke profil tersebut. Setelan lain yang Anda ubah berlaku tanpa profil.

Untuk melihat setelan dalam sebuah profil, buka layar profil tersebut. **Setelan dalam profil ini** mencantumkan setiap setelan berdasarkan judul dan letaknya di setelan SandFly. Untuk menghapus setelan dari profil, pilih setelan tersebut, lalu pilih **Hapus**. Setelan itu kemudian kembali ke nilainya tanpa profil.

### Menggunakan profil sekarang {#profiles-use}

1. Buka **Profil setelan** lalu pilih profilnya.
2. Pilih **Gunakan sekarang**.

Profil digunakan di semua tempat, termasuk di setelan SandFly. Untuk berhenti menggunakannya, pilih **Berhenti menggunakan** di layar yang sama.

Perintah-perintah berikut, di grup perintah **SandFly**, memilih profil yang digunakan di semua tempat. Secara default, perintah ini belum ditetapkan. Lihat [](#commands).

- **Pilih profil setelan**: membuka menu berisi **Tidak ada profil setelan** dan daftar profil.
- **Profil setelan berikutnya** dan **Profil setelan sebelumnya**: berpindah melalui daftar profil dan **Tidak ada profil setelan**.

SandFly akan mengucapkan nama profil, seperti "Profil membaca", atau "Tidak ada profil setelan".

### Menggunakan profil secara otomatis {#profiles-automatic}

Di layar profil:

- **Gunakan otomatis** > **Konten web**: menggunakan profil saat fokus berada di konten web.
- **Gunakan otomatis** > **Pembacaan berkelanjutan**: menggunakan profil selama pembacaan berkelanjutan, seperti Baca dari atas.
- **Gunakan otomatis** > **Pengeditan teks**: menggunakan profil saat fokus berada di kolom edit, atau di keyboard yang mengetik di dalamnya.
- **Aplikasi** > **Tambahkan aplikasi**: menggunakan profil saat aplikasi yang Anda pilih ditampilkan. Untuk menghapus aplikasi, pilih aplikasinya, lalu pilih **Hapus**.
- **Keyboard** > **Tambahkan keyboard**: menggunakan profil saat keyboard yang Anda pilih ditampilkan. Daftarnya memuat keyboard yang diaktifkan di setelan Android. Untuk menghapus keyboard, pilih keyboardnya, lalu pilih **Hapus**.

Profil yang digunakan secara otomatis tidak digunakan di setelan SandFly, agar setelan SandFly menampilkan setelan Anda tanpa profil tersebut. Profil juga tidak digunakan saat SandFly nonaktif.

#### Saat beberapa profil mengatur setelan yang sama {#profiles-order}

Beberapa profil dapat aktif bersamaan. Jika profil-profil tersebut mengatur setelan yang sama, profil yang diaktifkan dengan **Gunakan sekarang** yang diprioritaskan, lalu profil yang posisinya lebih tinggi di daftar **Profil setelan**. Untuk mengubah urutannya, gunakan **Pindahkan ke atas** dan **Pindahkan ke bawah** di layar profil.

### Mengganti nama, memindahkan, atau menghapus profil {#profiles-manage}

Layar profil juga memiliki:

- **Ganti nama**: mengubah nama profil.
- **Pindahkan ke atas** dan **Pindahkan ke bawah**: memindahkan urutan profil di daftar.
- **Ekspor ke file**: menyimpan profil ke file item. Lihat [](#profiles-item-files).
- **Hapus**: menghapus profil, setelah Anda mengonfirmasi.

### Mencadangkan dan memulihkan setelan {#profiles-backup}

**Cadangkan dan pulihkan** menyimpan semua setelan SandFly dan konten Anda sendiri ke sebuah file serta menggantinya dengan isi dari sebuah file, misalnya untuk menyiapkan ponsel lain dengan cara yang sama. Setelan ini tidak tersedia di jam tangan.

#### Mengekspor setelan {#profiles-export}

1. Buka **Cadangkan dan pulihkan** > **Ekspor pengaturan**.
2. Pilih lokasi untuk menyimpan file. Nama yang disarankan adalah `sandfly-settings.zip`.

SandFly akan mengucapkan "Pengaturan diekspor. Kunci API tidak disertakan".

File .zip tersebut menyimpan setiap setelan SandFly, serta:

- Label kustom.
- Profil setelan. Setelan disimpan sebagaimana adanya tanpa profil, dan profil disimpan secara terpisah.
- Skrip yang terinstal.
- Tema suara, dan suara yang Anda ubah di tema Backtalk.
- Gambar wallpaper.

File ini tidak menyertakan:

- Kunci API, agar Anda dapat membagikan atau menyinkronkan file tersebut.
- Lisensi SandFly Pro. Di ponsel lain, aktifkan SandFly Pro sebagai perangkat tersendiri. Lihat [](#pro).
- Waktu pemeriksaan pembaruan terakhir.
- Model AI dan pengenalan ucapan yang diunduh, log, dan laporan masalah.
- Draf tema suara.

#### Mengimpor setelan {#profiles-import}

1. Buka **Cadangkan dan pulihkan** > **Impor pengaturan**.
2. Pilih file setelan.

Setelan di dalam file akan menggantikan setelan Anda, dan SandFly akan mengucapkan "Pengaturan diimpor". Setelan yang tidak ada di dalam file, serta kunci API di ponsel ini, tetap seperti semula. Profil setelan yang sedang aktif diterapkan kembali di atas setelan yang diimpor.

Label kustom, profil setelan, skrip, dan tema suara di dalam file akan ditambahkan. Masing-masing menggantikan yang ada di ponsel ini jika memiliki item, nama, atau ID yang sama; yang lainnya tetap ada. Gambar wallpaper di dalam file menggantikan wallpaper ponsel ini.

**Impor pengaturan** juga membaca file setelan berekstensi .json yang diekspor oleh versi SandFly sebelumnya, serta file setelan yang diekspor dari Backtalk. Setelan dari file Backtalk menggantikan setelan SandFly, dan setelan yang hanya dimiliki SandFly tetap seperti semula.

Jika SandFly mengucapkan "File ini bukan file setelan SandFly atau Backtalk, atau tidak dapat dibaca", berarti file tersebut bukan file setelan atau rusak, ukuran file .json lebih dari 5 MB, atau ukuran file .zip melebihi 300 MB setelah diekstrak. Tidak ada yang diubah.

### File item {#profiles-item-files}

File item menyimpan item SandFly dari satu atau beberapa jenis, tanpa setelan lainnya. Anda dapat mengekspor item, membagikannya, mengeditnya, menulisnya secara manual atau dengan obrolan AI, lalu mengimpornya di ponsel mana saja yang memiliki SandFly. File item bukan file setelan: **Impor pengaturan** tidak membacanya, dan **Impor dari file** tidak membaca file setelan.

Setiap layar berikut memiliki **Ekspor ke file** dan **Impor dari file**:

| Item | Layar |
| --- | --- |
| Makro | **Makro** |
| Profil gestur | **Gestur** > **Profil gestur** |
| Gestur di aplikasi | **Gestur** > **Gestur di aplikasi** |
| Pintasan keyboard di aplikasi | **Pintasan keyboard** > **Pintasan keyboard di aplikasi** |
| Peta tombol | **Pintasan keyboard** (mengekspor peta tombol yang sedang digunakan) |
| Profil setelan | **Profil setelan** |
| Kamus ucapan | **Ucapan** > **Kamus ucapan** |
| Kamus simbol | **Ucapan** > **Kamus simbol** |
| Kamus braille | **Braille** > **Kamus braille** |
| Karakter braille kustom | **Braille** > **Keyboard braille** > **Karakter braille kustom** |
| Perintah suara kustom | **Perintah suara** |

File item tidak tersedia di jam tangan.

#### Mengekspor item ke file {#profiles-item-export}

1. Buka layar item tersebut.
2. Pilih **Ekspor ke file**.
3. Pilih lokasi untuk menyimpan file.

**Ekspor ke file** di layar daftar menyimpan setiap item dalam daftar. Di layar satu item, seperti profil setelan, opsi ini menyimpan item tersebut saja. SandFly akan mengucapkan "Diekspor", atau "Tidak dapat mengekspor".

#### Mengimpor item dari file {#profiles-item-import}

1. Buka salah satu layar pada tabel di [](#profiles-item-files).
2. Pilih **Impor dari file**.
3. Pilih file item.

SandFly mengimpor semua item dalam file, dari layar mana pun Anda mengimpornya, dan menyebutkan apa yang diimpor, seperti "Mengimpor 2 makro, 1 profil gestur". Item yang diimpor menggantikan item sejenis dengan nama yang sama, tanpa membedakan huruf besar/kecil. Item lainnya tetap ada. Entri kamus menggantikan entri dengan teks pencarian yang sama di aplikasi yang sama. Karakter braille kustom menggantikan karakter dengan teks yang sama di tabel braille yang sama. Perintah suara kustom menggantikan perintah dengan frasa yang sama. Peta tombol yang diimpor selalu menjadi peta tombol kustom.

Setelah impor selesai, SandFly mencantumkan apa yang dilewati, seperti perintah, gestur, tombol, dan setelan yang tidak dikenal, serta ekspresi reguler yang ditolak. Jika file tersebut adalah file setelan, SandFly akan mengucapkan "Ini adalah file setelan. Impor melalui Cadangkan dan pulihkan > Impor pengaturan".

#### Menulis file item {#profiles-item-reference}

**Cadangkan dan pulihkan** > **Referensi format file ekspor** menyimpan dokumen `sandfly-file-format.md` yang menjelaskan file item, diikuti daftar gestur, perintah, dan setelan ponsel ini beserta nilai masing-masing setelan saat ini. Agar obrolan AI menulis item untuk Anda, berikan file ini bersama deskripsi item yang diinginkan, simpan balasannya sebagai file, lalu impor dengan **Impor dari file**.

Formatnya dijelaskan di [](#api-item-files).
