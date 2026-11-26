## Suara, getaran, dan output audio {#sound}

SandFly memutar suara dan getaran untuk berbagai tindakan dan peristiwa, seperti memindahkan fokus, mengaktifkan item, menggulir, dan membuka jendela. **Suara dan getaran** di setelan SandFly mengaktifkan dan menonaktifkan suara serta getaran, mengatur volume dan kekuatannya, serta memuat tema suara dan suara kontrol. **Output audio** mengatur tempat ucapan dan suara diputar, pada volume mana, serta apa yang terjadi pada audio lain dan panggilan saat SandFly aktif.

Untuk mesin ucapan, suara, kecepatan bicara, volume ucapan, dan efek ucapan, lihat [](#speech). Untuk tombol volume dan **Umpan balik perubahan volume**, lihat [](#buttons).

### Mengaktifkan atau menonaktifkan suara {#sound-feedback}

Grup **Suara** di **Suara dan getaran** memiliki setelan berikut:

- **Respons suara**: memutar suara SandFly. Aktif secara default. Jika nonaktif, getaran tetap berfungsi.
- **Volume respons suara**: volume suara SandFly, dari 5 hingga 100 persen dengan kelipatan 5. Defaultnya adalah 50 persen.
- **Tema suara**: membuka tema suara terinstal, suara kontrol, dan audio 3D. Lihat [](#sound-themes).
- **Suara Tindakan tersedia**: kapan suara **Tindakan tersedia** diputar: **Nonaktif**, **Saat item difokuskan** (default), atau **Setelah item diucapkan**. Lihat [](#sound-actions-available).

Pintasan keyboard **Aktifkan atau nonaktifkan respons suara (earcon)** mengaktifkan atau menonaktifkan **Respons suara**. Lihat [](#appendix-keyboard-shortcuts).

Untuk menonaktifkan satu suara saja dan membiarkan sisanya aktif, lihat [](#sound-individual).

### Mengubah getaran {#sound-vibration}

Grup **Getaran** di **Suara dan getaran** memiliki setelan berikut. Pada perangkat tanpa motor getar, grup ini tidak ditampilkan.

- **Respons getar**: memutar getaran SandFly. Aktif secara default. Jika nonaktif, suara tetap diputar.
- **Kekuatan getaran**: seberapa kuat getarannya, dari 5 hingga 200 persen. Defaultnya adalah 100 persen. Setelan ini hanya ditampilkan pada perangkat yang motor getarnya dapat mengatur kekuatan.
- **Jenis getaran**: cara getaran diputar. Daftar hanya menampilkan jenis yang didukung motor getar perangkat, dan setelan disembunyikan jika hanya ada satu jenis yang tersedia. Hingga Anda memilih jenisnya, SandFly menggunakan jenis pertama dalam daftar.
  - **Efek haptik**: efek terkomposisi seperti klik dan ketukan halus, pada Android 11 dan lebih baru dengan motor getar yang mendukungnya.
  - **Denyut**: getaran dengan kekuatan bervariasi, pada motor getar yang dapat mengatur kekuatan.
  - **Hidup dan mati**: motor getar menyala dan mati pada kekuatan penuh. Setiap motor getar mendukung jenis ini.

Jika getaran terasa lemah dengan **Efek haptik**, pilih **Denyut** lalu naikkan **Kekuatan getaran**.

Perintah **Respons getar**, di grup perintah **Ucapan**, mengaktifkan atau menonaktifkan **Respons getar**. Lihat [](#commands).

### Suara dan getaran {#sound-events}

Setiap suara memiliki getarannya sendiri, yang diputar bersamaan dengan suara dan tetap bergetar meski **Respons suara** nonaktif. Anda dapat menonaktifkan semua suara dan tetap membedakan tindakan melalui getaran. Beberapa getaran, seperti getaran pengumuman dan getaran keyboard braille, bergetar tanpa suara.

Berikut daftar suara sesuai nama yang ditampilkan di **Suara dan getaran satu per satu** dan di daftar suara tema:

| Suara | Diputar saat |
| --- | --- |
| **Fokus pada item** | Fokus berpindah ke suatu item. |
| **Fokus pada item yang dapat diaktifkan** | Fokus berpindah ke item yang dapat diaktifkan. |
| **Tindakan tersedia** | Fokus berpindah ke item yang memiliki tindakan dari aplikasinya, seperti Balas pada pesan chat. Lihat [](#sound-actions-available). |
| **Menyentuh area kosong** | Jari Anda menyentuh bagian layar yang tidak memiliki item. |
| **Klik** | Suatu item diaktifkan. |
| **Sentuh lama** | Suatu item disentuh lama. |
| **Gulir** | Daftar digulir. |
| **Masuk ke daftar** dan **Keluar dari daftar** | Fokus masuk ke atau keluar dari daftar. |
| **Tindakan selesai atau sudah di akhir** | Tindakan selesai, atau fokus mencapai akhir daftar. |
| **Perubahan jendela** | Jendela atau layar baru terbuka. |
| **Kontrol pembacaan berikutnya** dan **Kontrol pembacaan sebelumnya** | Kontrol pembacaan lain dipilih. |
| **Gerakan tepi bawah**, **Gerakan tepi tengah**, dan **Gerakan tepi atas** | Tahap-tahap gerakan tepi. Tidak tersedia di jam tangan. |
| **Kursor dipindahkan** | Berpindah per karakter, kata, baris, atau paragraf memindahkan kursor di kolom teks yang sedang Anda edit. |
| **Pindah ke baris berikutnya** dan **Pindah ke baris sebelumnya** | Geseran memindahkan fokus ke item di baris bawah atau atasnya. |
| **Tugas agen dimulai**, **Tugas agen selesai**, **Pertanyaan agen**, **Agen berhenti pada tindakan sensitif**, dan **Tugas agen gagal** | Agen SandFly Pro. Hanya di versi dengan SandFly Pro. Lihat [](#pro). |
| **Salah eja** | Kata yang salah eja. |
| **Tautan** | Tautan di dalam teks. |
| **Teks berformat** | Teks berformat, seperti tebal. |
| **Emoji** | Setelah emoji, saat **Panjang respons** > **Setelah nama emoji** disetel ke **Putar suara**. Tidak ada getaran bawaan; tema suara dapat memberinya getaran. Lihat [](#verbosity-emoji). |
| **Layar mati** dan **Layar menyala** | Layar mati atau menyala. |
| **Layar disembunyikan** dan **Layar ditampilkan** | Layar disembunyikan, atau ditampilkan kembali. Lihat [](#display). |
| **Memuat** | Deskripsi atau hasil lain sedang dimuat. |
| **Model AI di perangkat sedang dimuat** | Model AI di perangkat sedang dimuat. |
| **Mode penjelajahan aktif** dan **Mode penjelajahan nonaktif** | Mode penjelajahan aktif atau nonaktif. Lihat [](#web). |
| **Keyboard dibuka** dan **Keyboard ditutup** | Keyboard di layar terbuka atau tertutup. |
| **Notifikasi SandFly** | SandFly mengirim notifikasi pada saluran notifikasi tanpa suara. |
| **Baterai penuh** | Baterai terisi penuh, saat **Umumkan saat baterai penuh** aktif. Lihat [](#verbosity-announcements). |
| **Menu lingkaran** | Item-item menu lingkaran, berupa delapan nada. |
| **Penampil braille terhubung**, **Penampil braille terputus**, **Perintah braille gagal**, **Gulir otomatis braille aktif**, **Gulir otomatis braille nonaktif**, dan **Keyboard braille dikalibrasi** | Penampil braille dan keyboard braille. Lihat [](#braille). |
| **Suara kontrol: tombol** hingga **Suara kontrol: item hierarki** | Suara kontrol, satu untuk setiap jenis kontrol. Lihat [](#sound-control-sounds). |

#### Suara navigasi {#sound-navigation-sounds}

SandFly tidak memutar suara saat gerakan dimulai atau berakhir. Sebagai gantinya, suara-suara berikut memberi umpan balik selama navigasi:

- **Kontrol pembacaan berikutnya** dan **Kontrol pembacaan sebelumnya** diputar saat Anda memilih kontrol pembacaan lain dengan gerakan, menu lingkaran, pintasan keyboard, atau keyboard braille.
- Suara gerakan tepi diputar saat jari menyentuh tepi tempat gerakan tepi dimulai (**Gerakan tepi bawah**, berupa ketukan rendah), saat gerakan mencapai tahap pertama (**Gerakan tepi tengah**), dan saat mencapai tahap kedua (**Gerakan tepi atas**). Menggeser kembali ke belakang memutar suara tahap yang dituju jari. Mengangkat jari tidak memutar suara apa pun. Untuk gerakan tepi, lihat [](#gestures).
- **Pindah ke baris berikutnya** diputar saat geseran maju memindahkan fokus ke item pada baris di bawah item yang difokuskan, dan **Pindah ke baris sebelumnya** saat geseran mundur memindahkan fokus ke item pada baris di atasnya. Setiap item daftar merupakan satu baris tersendiri. Item yang berdampingan, seperti ikon di layar utama atau sel dalam tabel, berada pada baris yang sama. Suara ini diputar bersamaan dengan suara fokus, dan getarannya menggantikan getaran fokus. Suara ini tidak diputar saat fokus berpindah ke jendela lain.

#### Suara Tindakan tersedia {#sound-actions-available}

Suara **Tindakan tersedia** diputar untuk item yang memiliki tindakan dari aplikasinya selain tindakan standar, seperti Balas dan Teruskan pada pesan chat di aplikasi perpesanan. Mengaktifkan, menyentuh lama, menggulir, meluaskan, dan tindakan standar lainnya tidak dihitung, sehingga tombol biasa tidak memutar suara **Tindakan tersedia**. Tombol keyboard di layar juga tidak dihitung. Tindakan tersebut ada di kontrol pembacaan **Tindakan** dan di menu SandFly.

Suara ini tidak memiliki getaran bawaan. Tema suara dapat memberinya getaran.

**Suara dan getaran** > **Suara Tindakan tersedia** mengatur kapan suara ini diputar:

- **Nonaktif**: suara tidak diputar.
- **Saat item difokuskan**: setelah suara fokus. Setelan default. Getaran yang diberikan tema untuk suara ini diputar menggantikan getaran fokus.
- **Setelah item diucapkan**: setelah deskripsi item selesai diucapkan sampai akhir. Jika Anda berpindah sebelum itu, suara tidak diputar.

Agar SandFly mengucapkan "Tindakan tersedia" setelah item seperti itu, lihat [](#verbosity-items).

#### Ketukan saat menggulir {#sound-scroll-ticks}

Saat Anda menggulir daftar dengan dua jari, SandFly memutar ketukan ringan untuk setiap item yang terlewati, sehingga seretan lambat berdetak pelan dan seretan cepat berdetak cepat. Di daftar yang tidak melaporkan itemnya, ketukan diputar setiap 48 dp pergeseran. Ketukan pertama diputar segera setelah seretan mencapai aplikasi. Suara gulir diputar bersamaan dengan ketukan, paling sering setiap 250 ms, dan nadanya makin tinggi dari atas daftar ke bawah. Saat SandFly sendiri yang menggulir, hanya suara gulir yang diputar.

### Menonaktifkan suara dan getaran satu per satu {#sound-individual}

**Suara dan getaran** > **Suara dan getaran satu per satu** memiliki tombol alih untuk setiap suara di bawah **Suara**, dan untuk setiap getaran di bawah **Getaran**. Semua tombol alih aktif secara default. Menonaktifkan suara tidak mematikan getarannya, dan menonaktifkan getaran tidak mematikan suaranya. **Respons suara** dan **Respons getar** tetap dapat menonaktifkan semua suara atau semua getaran sekaligus.

Untuk mendengarkan suara atau merasakan getaran sebelum mengubah tombol alihnya:

1. Pindahkan fokus ke tombol alih tersebut.
2. Buka menu tindakan.
3. Pilih **Pratinjau**.

Mengubah tombol alih tidak memutar suara atau getaran.

Daftar **Getaran** juga memiliki tombol alih untuk getaran tanpa suara: **Pengumuman**, **Gerakan tepi bergeser kembali**, serta getaran keyboard braille, seperti **Keyboard braille: mengetik karakter** dan **Keyboard braille: tidak ada yang bisa dihapus**. Tombol alih getaran juga menonaktifkan getaran pengganti yang disediakan oleh tema suara.

### Tema suara {#sound-themes}

Tema suara adalah sekumpulan suara dan getaran yang menggantikan bawaan SandFly. Setiap tema juga memiliki setelan **Suara dan getaran kontrol**, **Audio 3D**, dan **Tetap ucapkan jenis kontrol** sendiri. Suara bawaan disebut tema **SandFly**, yang tidak dapat dihapus.

**Suara dan getaran** > **Tema suara** memiliki bagian berikut:

- **Buat tema baru**: membuat tema baru. Lihat [](#sound-themes-create).
- **Tema terinstal**: setiap tema yang terinstal. Tema yang sedang dipakai menampilkan "Digunakan" pada ringkasannya.
- **Setelan** diikuti nama tema yang sedang digunakan: setelan suara kontrol dan daftar suara tema tersebut. Lihat [](#sound-control-sounds) dan [](#sound-themes-replace).
- **Instal tema**: **Dari file**, **Dari tautan**, dan **Dari sebuah sumber**. Lihat [](#sound-themes-install).

#### Menginstal tema suara {#sound-themes-install}

File tema berbentuk file ZIP. Untuk menginstalnya, gunakan salah satu cara berikut:

- **Dari file**: pilih file tema melalui pemilih file sistem.
- **Dari tautan**: masukkan atau tempel tautan ke file tema, atau ke repositori GitHub yang berisi tema, lalu pilih **Instal**. Tautan harus diawali `https`.
- **Dari sebuah sumber**: daftar repositori di GitHub, GitLab, Codeberg, Gitea, atau Bitbucket. Pilih **Tambahkan sumber** dan masukkan alamat repositori, seperti `github.com/owner/repository`. Pilih sumber untuk melihat daftar file temanya, lalu pilih file tema untuk menginstalnya. Daftar ini diawali dengan repositori tema SandFly. **Hapus sumber** menghapus sumber dari daftar. Tema yang sudah diinstal dari sumber itu tetap terinstal.
- Buka file tema dengan SandFly dari pengelola file atau unduhan browser, atau bagikan file tema atau tautan ke SandFly dari aplikasi lain, lalu pilih **Instal tema suara**.

Sebelum menginstal, SandFly menampilkan penulis, deskripsi, jumlah suara dan getaran, lisensi, serta file apa pun yang dilewati. Pilih **Instal dan gunakan** untuk menginstal sekaligus memakai tema tersebut, atau **Instal** untuk menginstalnya sambil tetap memakai tema saat ini. Tema dengan nama yang sama dengan tema terinstal akan menggantikannya.

Catatan: Jam tangan tidak memiliki pemilih file. Di jam tangan, instal tema melalui tautan. **Dari sebuah sumber** hanya tersedia di ponsel.

#### Memilih dan mengelola tema {#sound-themes-manage}

1. Buka **Suara dan getaran** > **Tema suara**.
2. Di bawah **Tema terinstal**, pilih sebuah tema.
3. Pilih salah satu opsi berikut:
   - **Gunakan tema ini**: beralih ke tema tersebut beserta setelan suara kontrolnya sendiri.
   - **Tentang tema ini**: menampilkan penulis, deskripsi, jumlah suara dan getaran, lisensi, dan situs web.
   - **Simpan sebagai file tema**: menyimpan tema sebagai file ZIP yang dapat Anda bagikan.
   - **Hapus**: menghapus tema. Tidak tersedia untuk tema **SandFly**.

#### Mengganti satu suara {#sound-themes-replace}

Anda dapat mengganti suara mana pun dari tema yang sedang digunakan dengan file suara lain. Untuk mengganti suara tanpa menginstal tema, gunakan tema **SandFly**.

1. Buka **Suara dan getaran** > **Tema suara**.
2. Di bawah setelan tema yang sedang digunakan, pilih **Suara**.
3. Pilih sebuah suara. Judul dialog menampilkan nama suara dan nama filenya, seperti "Klik (tick)".
4. Pilih salah satu opsi berikut:
   - **Pilih file suara**: membuka pemilih file. Ukuran file suara maksimal 5 MB, dalam format WAV, OGG, MP3, atau FLAC.
   - **Pilih dari folder**: menampilkan daftar file suara dan subfolder dari sebuah folder. Pertama kali, pemilih folder terbuka untuk memilih folder tersebut. Lihat [](#sound-themes-folder).
   - **Ganti folder**: membuka pemilih folder untuk memilih folder lain bagi **Pilih dari folder**. Ditampilkan setelah folder dipilih.
   - **Dengarkan**: memutar suara tersebut.
   - **Tanpa suara**: tidak memutar suara untuk peristiwa tersebut.
   - **Gunakan suara SandFly**: kembali ke suara bawaan.

Ringkasan setiap suara menampilkan **Suara SandFly**, **Suara tema**, atau **Tanpa suara**. Tindakan yang sama tersedia di menu tindakan setiap suara, yaitu **Dengarkan**, **Setel ke tanpa suara**, **Gunakan suara SandFly**, dan **Pilih file suara**. **Gunakan suara SandFly untuk semua** menghapus semua suara tema dan memakai suara bawaan.

Daftar suara ini juga memuat suara yang tidak ada di **Suara dan getaran satu per satu**: **Perubahan volume**, yang diputar pada setiap langkah perubahan volume saat **Umpan balik perubahan volume** disetel ke nada (lihat [](#buttons)), dan suara pengetikan keyboard braille, yang diputar saat **Suara pengetikan** aktif di setelan keyboard braille (lihat [](#braille)).

#### Memilih suara dari folder {#sound-themes-folder}

**Pilih dari folder** menampilkan isi satu folder di ponsel. Pilih sebuah suara, lalu **Pilih dari folder**. Pertama kali, pilih folder di pemilih folder.

Daftar menampilkan subfolder terlebih dahulu, lalu file suara, masing-masing diurutkan menurut nama. Hingga 2.000 entri ditampilkan per folder.

- File yang mendapat fokus aksesibilitas akan diputar. Memindahkan fokus dari file tersebut akan menghentikannya.
- Pilih sebuah file untuk menggunakannya sebagai suara tersebut.
- Pilih sebuah subfolder untuk menampilkan isinya. **Folder induk** atau Kembali membawa Anda ke folder di atasnya.

Jika folder dihapus atau SandFly tidak dapat lagi membacanya, pemilih folder akan terbuka kembali.

#### Membuat tema suara {#sound-themes-create}

1. Buka **Suara dan getaran** > **Tema suara**.
2. Pilih **Buat tema baru**.
3. Masukkan **Nama**. Nama harus berbeda dari nama tema yang sudah terinstal. **Deskripsi**, **Penulis**, dan **Situs web** bersifat opsional.
4. Pilih **Oke**. Tema terbuka dalam bentuk daftar suaranya.
5. Atur **Suara dan getaran kontrol**, **Audio 3D**, dan **Tetap ucapkan jenis kontrol**, lalu pilih file suara untuk setiap suara yang ingin Anda ganti.
6. Pilih **Simpan** untuk menyimpan tema dan langsung menggunakannya, atau **Simpan sebagai file tema** untuk menyimpannya sebagai file ZIP tanpa langsung menggunakannya.

Untuk keluar tanpa menyimpan, kembali lalu pilih **Buang**. **Lanjutkan mengedit** membawa Anda kembali ke tema. Jika Anda tidak mengubah **Suara dan getaran kontrol**, opsi tersebut akan otomatis diaktifkan bila tema yang disimpan berisi suara kontrol.

#### File tema {#sound-themes-files}

File tema adalah file ZIP yang berisi file `theme.json` dan file-file suara di dalam folder yang sama. Folder tersebut juga bisa berada di dalam satu folder di dalam file ZIP, seperti file ZIP yang dibuat GitHub dari sebuah repositori, sehingga repositori GitHub yang memiliki `theme.json` dan file suara di tingkat teratas langsung valid sebagai tema. File ZIP berisi suara tanpa `theme.json` juga dapat diinstal menggunakan nama filenya.

- Namai setiap file suara sesuai suara yang digantikannya, diikuti ekstensi formatnya, seperti `focus.wav` atau `tick.ogg`. Dialog setiap suara di **Suara** menampilkan nama filenya. Tema hanya perlu menyertakan suara yang ingin diubah.
- Format WAV, OGG, MP3, FLAC, M4A, AAC, dan Opus didukung, masing-masing hingga 5 MB dan maksimal 50 MB untuk keseluruhan tema. File yang tidak dapat diputar Android dilewati saat instalasi.
- `theme.json` dapat mengatur nama, penulis, deskripsi, lisensi, dan situs web tema, setelan suara kontrolnya, suara yang disetel tanpa suara, serta getaran. Satu getaran dapat memiliki hingga tiga bentuk untuk ketiga jenis getaran.
- File lisensi dan readme, seperti `LICENSE.txt` atau `README.md`, disimpan bersama tema. File lainnya dilewati.

Untuk format file lengkap beserta seluruh nama suara dan getaran, lihat [Tema suara](https://github.com/aaron-gh/SandFly/blob/master/themes.md).

### Suara dan getaran kontrol {#sound-control-sounds}

Saat suara kontrol aktif, memfokuskan sebuah kontrol akan memutar suara dan getaran sesuai jenis kontrolnya sebagai pengganti suara dan getaran fokus, dan SandFly tidak menyebutkan jenis kontrol tersebut, seperti "tombol". Tersedia suara untuk tombol, kotak centang (termasuk tombol alih dan tombol pengalih), tombol pilihan, kolom teks, daftar tarik-turun, penggeser, gambar, jam, tab, item menu, dan item daftar, serta, di halaman web, tautan dan item hierarki.

Untuk mengaktifkan suara kontrol:

1. Buka **Suara dan getaran** > **Tema suara**.
2. Di bawah setelan tema yang sedang digunakan, aktifkan **Suara dan getaran kontrol**. Setelan ini nonaktif secara default.

Setelan di bawah tema yang sedang digunakan:

- **Suara dan getaran kontrol**: memutar suara dan getaran kontrol.
- **Audio 3D**: kapan suara kontrol diputar sesuai posisi kontrol di layar. Lihat [](#sound-3d-audio).
- **Tetap ucapkan jenis kontrol**: tetap mengucapkan jenis kontrol setelah suaranya diputar. Aktif secara default untuk tema **SandFly**, dan nonaktif untuk tema lain kecuali ditentukan oleh tema tersebut.

Item yang bukan kontrol itu sendiri, seperti satu baris di Setelan, memutar suara dari kontrol pertama di dalamnya, seperti tombol alihnya, atau suara sesuai jenis itemnya, seperti item daftar. Tombol keyboard di layar tetap menggunakan suara fokus. Getaran kontrol tetap bergetar meski **Respons suara** nonaktif, sehingga Anda dapat membedakan kontrol lewat getaran. Jenis kontrol yang suaranya dinonaktifkan di **Suara dan getaran satu per satu**, atau disetel tanpa suara oleh tema, akan memutar suara fokus dan SandFly tetap mengucapkan jenis kontrol tersebut.

Setiap tema menyimpan setelan suara kontrolnya sendiri. Mengganti tema akan ikut mengganti setelan tersebut.

#### Audio 3D {#sound-3d-audio}

Suara kontrol diputar dari posisi kontrol di layar: dari sebelah kiri untuk kontrol di sisi kiri, dan lebih tinggi untuk kontrol di dekat bagian atas. Dengan audio 3D, suara memanfaatkan pengukuran cara kepala menerima suara dari berbagai arah. Tanpanya, suara hanya berpindah antara kiri dan kanan.

**Audio 3D** memiliki pilihan berikut:

- **Dengan headphone**: default. Audio 3D diputar saat audio SandFly dikirim ke headphone berkabel atau USB, headset atau headphone Bluetooth, atau alat bantu dengar. Di Android 13 dan lebih baru, SandFly memeriksa tujuan pemutaran audionya, sehingga speaker Bluetooth atau sistem mobil yang terhubung tetapi tidak memutar audio SandFly tidak dihitung. Di versi lebih lama, semua headphone yang terhubung dihitung.
- **Selalu**: audio 3D juga diputar di speaker ponsel.
- **Tidak pernah, hanya kiri dan kanan**: suara hanya berpindah antara kiri dan kanan.
- **Dengan headphone, tanpa headphone di tengah**: audio 3D saat memakai headphone, seperti **Dengan headphone**. Di speaker ponsel dan output lainnya, suara diputar dari tengah, tanpa berpindah antara kiri dan kanan.

### Setelan output audio {#sound-audio-output}

**Output audio** di setelan SandFly memiliki setelan berikut:

- **Perangkat output audio**: tempat ucapan dan suara diputar. Lihat [](#sound-output-device).
- **Speaker ponsel dengan perangkat ini**: perangkat output yang memindahkan audio SandFly ke speaker ponsel saat terhubung. Lihat [](#sound-speaker-devices).
- **Speaker saat jauh dari telinga**: mengalihkan panggilan antara lubang suara dan speaker. Lihat [](#sound-calls).
- **Pertahankan ucapan di ponsel saat mentransmisikan layar**: lihat [](#sound-casting).
- **Setel ulang audio aksesibilitas setelah panggilan**: lihat [](#sound-calls).
- **Kontrol volume untuk ucapan**: saluran volume yang digunakan oleh ucapan dan suara. Lihat [](#sound-speech-volume).
- **Pengecilan volume audio**: lihat [](#sound-ducking).
- **Audio latensi rendah**: lihat [](#sound-low-latency).

### Memilih perangkat output audio {#sound-output-device}

**Output audio** > **Perangkat output audio** menentukan tempat ucapan dan suara SandFly diputar:

- **Default sistem**: default. Android yang memilih outputnya.
- **Speaker ponsel**: speaker ponsel, termasuk saat headphone Bluetooth atau berkabel sedang terhubung.
- Perangkat terhubung berdasarkan namanya, seperti perangkat Bluetooth, berkabel, USB, atau alat bantu dengar. Daftar ini menampilkan perangkat yang sedang terhubung saat ini.

Perangkat Bluetooth yang Anda pilih menerima audio melalui perutean Android, sama seperti **Default sistem**, sehingga perangkat tidak beralih ke mode panggilan. Jika Android merutekan audio ke perangkat lain sementara perangkat Bluetooth pilihan Anda terhubung, audio tetap diputar di perangkat lain tersebut. Perangkat berkabel, USB, dan alat bantu dengar menerima audio secara langsung.

Pilihan tetap tersimpan saat perangkat tersambung atau terputus dan saat aplikasi lain mulai memutar audio. Saat perangkat pilihan tidak terhubung, setelan menampilkan namanya diikuti "(terputus)", dan audio mengikuti perutean Android hingga perangkat terhubung kembali. Selama panggilan telepon, dan saat aplikasi lain menggunakan mikrofon untuk panggilan atau asisten suara, Android yang memilih outputnya.

Kontrol pembacaan **Perangkat output audio** mengubah setelan ini dengan geser ke atas atau ke bawah. Lihat [](#sound-reading-controls).

### Memutar di speaker ponsel dengan perangkat tertentu {#sound-speaker-devices}

**Output audio** > **Speaker ponsel dengan perangkat ini** mencantumkan output Bluetooth, berkabel, dan USB yang sedang terhubung sekarang maupun yang pernah terhubung sebelumnya saat SandFly aktif. Saat perangkat yang Anda centang terhubung, SandFly memutar ucapan dan suara di speaker ponsel, apa pun pilihan pada **Perangkat output audio**. Misalnya, centang speaker Bluetooth agar ucapan tetap terdengar di ponsel sementara musik diputar di speaker, dan biarkan earbud tidak dicentang agar ucapan tetap masuk ke earbud.

Perangkat dicantumkan berdasarkan namanya, sehingga dua perangkat dengan nama yang sama menjadi satu pilihan. Sebelum ada perangkat yang terhubung, setelan menampilkan "Belum ada perangkat Bluetooth atau kabel yang terhubung".

### Memilih volume untuk ucapan {#sound-speech-volume}

**Output audio** > **Kontrol volume untuk ucapan** mengatur volume yang digunakan oleh ucapan dan suara SandFly:

- **Volume aksesibilitas**: default.
- **Volume media**.
- **Volume dering**: ucapan diputar sebagai nada dering. Ucapan tetap di ponsel saat media ditransmisikan ke perangkat lain, dapat diputar di speaker ponsel saat headphone terhubung, dan menjadi senyap saat pendering disetel senyap.
- **Volume panggilan, speaker telinga**: ucapan diputar seperti panggilan, melalui lubang suara saat tidak ada headset yang terhubung.

Saat **Perangkat output audio** mengirim audio ke speaker ponsel atau ke perangkat berkabel, USB, atau alat bantu dengar, ucapan menggunakan volume aksesibilitas.

Saat ucapan tidak menggunakan volume aksesibilitas, Android memutar suara aksesibilitas dari aplikasi lain, seperti nada pengetikan suara, menggunakan volume media.

Kontrol pembacaan **Volume ucapan** beralih di antara volume-volume ini, dan kontrol pembacaan **Volume aksesibilitas** mengubah volume aksesibilitas. Lihat [](#sound-reading-controls).

### Mengecilkan audio lain saat SandFly berbicara {#sound-ducking}

**Output audio** > **Pengecilan volume audio** mengecilkan volume audio lain, seperti musik, saat SandFly berbicara. Setelan ini aktif secara default. Kontrol pembacaan **Pengecilan volume audio** dan perintah **Pengecilan volume audio**, di grup perintah **Ucapan**, mengaktifkan dan menonaktifkannya.

### Audio latensi rendah {#sound-low-latency}

**Output audio** > **Audio latensi rendah** memutar suara dan ucapan SandFly melalui jalur audio tercepat di perangkat sehingga mulai terdengar lebih cepat. Setelan ini aktif secara default di ponsel dan tablet, serta nonaktif secara default di jam tangan. Jika ucapan atau suara terputus-putus atau terdengar tidak normal, nonaktifkan setelan ini.

- Ucapan menggunakan jalur cepat di Android 11 dan lebih baru. Di versi lebih lama, hanya suara yang menggunakannya.
- Ucapan diputar dengan cara biasa selama panggilan, sesaat setelah panggilan dimulai atau berakhir, saat aplikasi merekam dengan mikrofon, serta pada mesin ucapan yang memutar audionya sendiri.
- Melanjutkan ucapan dari tengah kata setelah jeda memerlukan **Audio latensi rendah**. Lihat [](#speech-pause). Jika nonaktif, efek ucapan hanya berfungsi pada perangkat yang mendukungnya sebagai efek audio. Lihat [](#voice-effects).

### Mempertahankan ucapan di ponsel saat mentransmisikan layar {#sound-casting}

**Output audio** > **Pertahankan ucapan di ponsel saat mentransmisikan layar** mencegah transmisi layar dan pencerminan layar mengirim ucapan serta suara SandFly ke TV atau speaker. Rekaman layar pun tidak menyertakan audio SandFly. Setelan ini nonaktif secara default. Memerlukan Android 10 atau lebih baru.

### Panggilan {#sound-calls}

**Output audio** memiliki dua setelan untuk panggilan.

**Speaker saat jauh dari telinga** mengalihkan panggilan ke speaker saat Anda menjauhkan ponsel dari telinga, dan kembali ke lubang suara saat Anda mendekatkannya lagi ke telinga. Setelan ini nonaktif secara default dan memerlukan Android 12 atau lebih baru. Fitur ini hanya bekerja saat SandFly aktif dan tidak dijeda, dan hanya memindahkan panggilan antara lubang suara dan speaker: headset Bluetooth dan berkabel tetap digunakan. Panggilan yang dimulai saat ponsel jauh dari telinga langsung dialihkan ke speaker.

Setelan ini memerlukan izin yang tidak diberikan Android kepada pembaca layar. Hingga izin diberikan, ringkasannya menampilkan perintah untuk memberikannya. Untuk memberikan izin, lakukan salah satu langkah berikut:

- Saat Shizuku berjalan, aktifkan setelan ini. SandFly memberikan izin melalui Shizuku. Lihat [](#updates).
- Jalankan perintah tersebut dengan adb. Untuk menyalin perintah, buka menu tindakan pada setelan ini lalu pilih **Salin perintah**. Setelah itu kembali ke **Output audio**. Tanpa Shizuku, tombol alih tidak tersedia hingga izin diberikan.

SandFly menggunakan sensor kedekatan untuk mendeteksi ponsel di dekat telinga Anda.

**Setel ulang audio aksesibilitas setelah panggilan** mengembalikan audio SandFly dari lubang suara saat Android membiarkan audio aksesibilitas tertinggal di lubang suara setelah panggilan berakhir, seperti panggilan di aplikasi perpesanan. Setelan ini nonaktif secara default, memerlukan Android 13 atau lebih baru, dan dijalankan melalui Shizuku atau root. Mengaktifkannya akan meminta Shizuku mengizinkan SandFly. Tanpa Shizuku, ringkasannya menampilkan perintah adb untuk menyetel ulang audio satu kali. Untuk menyalinnya, buka menu tindakan pada setelan ini, lalu pilih **Salin perintah**.

### Kontrol pembacaan untuk suara dan volume {#sound-reading-controls}

Kontrol pembacaan berikut mengubah setelan suara dan volume dengan geser ke atas atau ke bawah. Tambahkan melalui **Kontrol pembacaan**. Lihat [](#commands).

- **Perangkat output audio**: beralih di antara pilihan pada **Perangkat output audio**.
- **Volume ucapan**: beralih di antara pilihan volume pada **Kontrol volume untuk ucapan**.
- **Pengecilan volume audio**: mengaktifkan atau menonaktifkan **Pengecilan volume audio**.
- **Volume aksesibilitas**: menaikkan atau menurunkan volume aksesibilitas.
- **Volume media**, **Volume panggilan**, **Volume dering**, **Volume notifikasi**, dan **Volume alarm**: menaikkan atau menurunkan masing-masing volume satu langkah dan mengucapkan nilai volume barunya. Mengubah volume dering atau notifikasi ke atau dari mode senyap memerlukan **Akses Jangan Ganggu**.
- **Mode suara**: menyetel pendering ke suara, getar, atau bisu. Mengaktifkan atau menonaktifkan mode bisu memerlukan **Akses Jangan Ganggu**.
