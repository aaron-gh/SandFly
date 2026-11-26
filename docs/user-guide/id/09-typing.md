## Mengetik dan mengedit teks {#typing}

SandFly membacakan keyboard di layar saat Anda menjelajahinya, mengetikkan tombol di bawah jari Anda, dan membacakan apa yang Anda ketik. Anda juga dapat mendikte teks, menulisnya dengan tangan di layar, atau mengetik dalam braille. Bab ini menjelaskan setelan **Mengetik**, mengetik di keyboard di layar, memindahkan kursor, memilih dan menyalin teks, histori papan klip, pengeditan terperinci, dikte, dan keyboard tulisan tangan.

Keyboard braille dijelaskan di [](#braille), sedangkan keyboard fisik dan metode input Keyboard SandFly dijelaskan di [](#keyboards).

### Setelan mengetik {#typing-settings}

Untuk membuka setelan mengetik, buka **Mengetik** di setelan SandFly. Layar ini berisi:

- **Gema keyboard**: apa yang dibacakan SandFly saat Anda mengetik dengan setiap jenis keyboard. Lihat [](#typing-keyboard-echo).
- **Ketuk dua kali memindahkan kursor ke awal atau akhir**. Lihat [](#typing-double-tap-cursor).
- **Keyboard virtual**: **Metode pengetikan**, **Penundaan sentuh lama saat mengetik**, **Tahan tombol tetap ditekan setelah jeda sentuh lama**, **Penundaan fokus pengetikan**, dan **Bacakan tombol keyboard lebih cepat**. Lihat [](#typing-on-screen-keyboard).
- **Keyboard fisik**: hanya ditampilkan saat keyboard fisik terhubung. Lihat [](#typing-physical-keyboard).
- **Cara lain untuk mengetik**: **Dikte**, **Keyboard tulisan tangan**, dan **Tunggu setelah menulis**. Lihat [](#typing-dictation) dan [](#typing-handwriting).

### Mengetik di keyboard di layar {#typing-on-screen-keyboard}

Sentuh keyboard dan geser jari Anda di atas tombol-tombolnya. SandFly membacakan setiap tombol di bawah jari Anda. Cara tombol diketik bergantung pada metode pengetikan.

#### Memilih metode pengetikan {#typing-method}

1. Buka **Mengetik** > **Metode pengetikan**.
2. Pilih salah satu metode berikut:
   - **Tahan jari untuk memilih tombol, lalu ketuk dua kali**: angkat jari Anda, lalu ketuk dua kali di mana saja untuk mengetik tombol yang terakhir dibacakan SandFly.
   - **Tahan jari untuk memilih tombol huruf, lalu angkat. Ketuk dua kali untuk tombol lain.**: mengangkat jari Anda mengetikkan tombol huruf. Tombol lain, seperti Delete atau Enter, memerlukan ketuk dua kali. Ini adalah setelan default.
   - **Tahan jari untuk memilih tombol, lalu angkat**: mengangkat jari Anda mengetikkan tombol apa saja. Metode ini memiliki jeda paling rendah.
   - **Tahan jari untuk memilih tombol mana saja, lalu angkat. Ketuk dua kali untuk Enter, Selesai, atau Kirim.**: mengangkat jari Anda mengetikkan tombol apa saja kecuali tombol yang mengirim atau menyerahkan, seperti Enter, Selesai, Kirim, Cari, atau Buka, yang memerlukan ketuk dua kali. SandFly mengenali tombol ini di Gboard. Di keyboard lain, setiap tombol diketik saat Anda mengangkat jari.
   - **Ketuk tombol apa saja untuk mengetiknya, atau tahan jari untuk memilih tombol apa saja, lalu angkat**: ketukan cepat pada tombol langsung mengetiknya, sehingga huruf ganda yang diketik cepat langsung terketik dua kali. Menahan jari di keyboard menjelajahi tombol-tombol, dan mengangkatnya mengetikkan tombol di bawah jari. Metode ini tersedia di Android 13 dan yang lebih baru.

Dengan metode mana pun, Anda juga dapat menggunakan ketuk terpisah (split-tap): tahan satu jari pada tombol, lalu ketuk layar dengan jari lain untuk mengetiknya.

Di Android 13 dan yang lebih baru, memilih salah satu dari tiga metode terakhir menetapkan **Penundaan fokus pengetikan** ke 0,15 detik. Lihat [](#typing-focus-delay).

#### Menyentuh lama tombol {#typing-touch-and-hold}

**Mengetik** > **Penundaan sentuh lama saat mengetik** mengatur berapa lama Anda menahan jari pada sebuah tombol sebelum SandFly menekannya lama, misalnya untuk membuka karakter alternatif dari tombol tersebut. Pilihannya adalah 0,5, 1, 2, 3, dan 5 detik. Defaultnya adalah 3 detik. Setelan ini tidak tersedia dengan metode pengetikan **Tahan jari untuk memilih tombol, lalu ketuk dua kali**.

Saat **Mengetik** > **Tahan tombol tetap ditekan setelah jeda sentuh lama** aktif, SandFly menekan tombol ke bawah pada akhir jeda dan meneruskan sisa sentuhan ke keyboard, seperti yang dilakukan ketuk dua kali lalu tahan. Keyboard kemudian menangani sentuhan itu sebagai sentuhan lama miliknya sendiri, seperti tombol Delete yang berulang saat ditahan. Ini berlaku untuk metode angkat untuk mengetik, pada semua keyboard kecuali Gboard dan Pixelboard. Tombol Gboard dan Pixelboard disentuh lama, sehingga membuka pop-up karakter alternatif. Setelan ini nonaktif secara default.

#### Karakter alternatif di Gboard {#typing-gboard-alternatives}

Saat Anda menahan tombol di Gboard, SandFly menekannya lama setelah jeda sentuh lama, dan Gboard membuka jendela pop-up karakter alternatif. SandFly membacakan karakter yang dipilih, seperti "Koma", segera setelah pop-up terbuka. Geser jari Anda ke karakter lain untuk mendengarnya, lalu angkat jari untuk mengetik karakter yang sedang dipilih.

Gboard 18.8 dan yang lebih baru menerima sentuhan melalui jelajah sentuh Android, seperti keyboard lainnya. Dengan Gboard versi sebelumnya, dan Pixelboard yang dibangun dari Gboard, SandFly menjelajahi sentuhan di keyboard secara langsung di Android 11 dan yang lebih baru. Saat pop-up versi ini terbuka, jari Anda selalu memilih salah satu karakternya, di mana pun posisi jari. Jika jari berada di luar pop-up, karakter terdekat yang dipilih. Mengangkat jari mengetik karakter itu lalu menutup pop-up. Saat jari Anda meninggalkan keyboard tanpa pop-up yang terbuka, jelajah sentuh mengambil alih untuk bagian layar lainnya, dan mengangkat jari di sana tidak mengetik apa pun. Ini tidak berlaku dengan metode pengetikan **Tahan jari untuk memilih tombol, lalu ketuk dua kali**.

#### Getaran tombol Gboard {#typing-gboard-vibration}

Gboard 18.8 dan yang lebih baru memutar umpan balik tombol yang diatur di setelan Gboard, seperti **Bergetar saat tombol ditekan**.

Gboard dan Pixelboard versi sebelum 18.8 memutar umpan balik tombol hanya saat tombol menerima peristiwa arahkan kursor (hover), yang tidak dikirim oleh sentuhan yang dijelajahi SandFly secara langsung. Saat Shizuku atau root disiapkan untuk SandFly, SandFly mengirimkan peristiwa hover ini ke keyboard, sehingga keyboard memutar umpan balik yang diatur di setelannya sendiri. Tanpa Shizuku atau root, versi ini tidak memutar umpan balik tombol saat SandFly menjelajahi keyboard. Tidak ada setelan untuk ini. Untuk menyiapkan Shizuku atau root, lihat [](#updates).

#### Mengubah penundaan fokus pengetikan {#typing-focus-delay}

**Mengetik** > **Penundaan fokus pengetikan** mengatur seberapa cepat SandFly membacakan tombol setelah Anda menyentuh keyboard: 0,15, 0,2, 0,25, atau 0,3 detik. Defaultnya adalah 0,3 detik. Jeda yang lebih singkat membacakan tombol lebih cepat. Setelan ini tersedia di Android 13 dan yang lebih baru.

Anda juga dapat mengubah jeda ini saat mengetik menggunakan kontrol pembacaan **Penundaan fokus pengetikan**, di grup **Sentuhan** pada **Kontrol pembacaan**. Lihat [](#commands).

#### Mengumumkan tombol keyboard lebih cepat {#typing-faster-keys}

**Mengetik** > **Bacakan tombol keyboard lebih cepat** menyiapkan ucapan tombol keyboard sebelumnya, untuk mengurangi jeda sebelum tombol dibacakan. Setelan ini nonaktif secara default, dan hanya tersedia saat Google Text-to-Speech digunakan sebagai mesin ucapan.

#### Gerakan geser dan keyboard {#typing-swipes-keyboard}

Gerakan geser dari aplikasi, bilah status, atau bilah navigasi tidak pernah memindahkan fokus ke dalam keyboard. Dengan metode pengetikan apa pun kecuali **Tahan jari untuk memilih tombol, lalu ketuk dua kali**, gerakan geser saat fokus berada di tombol akan berlanjut di dalam aplikasi, mulai dari kolom teks yang sedang Anda ketik. Dengan metode ketuk dua kali, gerakan geser berpindah antar tombol. Gerakan geser dari dalam keyboard tetap dapat keluar dari keyboard.

### Gema keyboard {#typing-keyboard-echo}

Gema keyboard mengatur apa yang dibacakan SandFly saat Anda mengetik. Setiap jenis keyboard memiliki setelannya sendiri.

1. Buka **Mengetik** > **Gema keyboard**.
2. Pilih keyboard: **Keyboard virtual**, **Keyboard fisik**, **Keyboard braille**, **Penampil braille**, atau **Keyboard tulisan tangan**.
3. Pilih **Tidak ada**, **Karakter**, **Kata**, atau **Karakter dan kata**. Default untuk masing-masing adalah **Karakter dan kata**.

Gema kata membacakan seluruh kata sebagaimana tertulis di kolom, kembali hingga spasi terakhir. Kata dengan apostrof atau tanda hubung, seperti "don't", dihitung sebagai satu kata, dan alamat web atau email adalah satu kata. Saat keyboard menambahkan sebuah kata, tanda bacanya, dan spasi sekaligus, seperti yang dilakukan keyboard braille dalam braille singkat, SandFly membacakan kata tersebut.

Setiap setelan gema juga merupakan kontrol pembacaan, item Menu SandFly, dan perintah di grup perintah **Ucapan**: **Gema keyboard di layar**, **Gema keyboard fisik**, **Gema keyboard braille**, **Gema penampil braille**, dan **Gema keyboard tulisan tangan**. Kontrol pembacaan dan item menu tersebut nonaktif secara default. Untuk menambahkannya, buka **Kontrol pembacaan** atau **Menu SandFly** lalu aktifkan di grup **Gema keyboard**. Lihat [](#commands).

Pintasan keyboard **Ganti mode gema pengetikan** mengubah **Keyboard fisik** ke pilihan berikutnya. Lihat [](#appendix-keyboard-shortcuts).

### Memindahkan kursor {#typing-move-cursor}

Saat fokus berada di kolom teks, kontrol pembacaan **Karakter**, **Kata**, **Baris**, **Kalimat**, dan **Paragraf** memindahkan kursor melewati teks, dan SandFly membacakan teks yang dilewati. Dengan gerakan default:

1. Geser ke atas lalu ke bawah, atau ke bawah lalu ke atas, untuk memilih kontrol pembacaan. Anda juga dapat menggeser ke atas atau ke bawah dengan tiga jari.
2. Geser ke bawah untuk memindahkan kursor maju, atau geser ke atas untuk memindahkannya mundur.

Perintah **Pindahkan kursor ke awal** dan **Pindahkan kursor ke akhir** di grup **Pengeditan teks** pada **Perintah** memindahkan kursor ke awal atau akhir teks. Keduanya juga ada di tindakan kolom teks. Lihat [](#typing-editing-actions).

Untuk gerakan default, lihat [](#appendix-gestures). Untuk kontrol pembacaan, lihat [](#commands).

#### Ketuk dua kali untuk memindahkan kursor ke awal atau akhir {#typing-double-tap-cursor}

Saat **Mengetik** > **Ketuk dua kali memindahkan kursor ke awal atau akhir** aktif, ketuk dua kali pada kolom teks yang sedang Anda ketik akan memindahkan kursor:

- Jika kursor tidak berada di awal, ketuk dua kali memindahkannya ke awal teks, dan SandFly mengumumkan "Awal kolom".
- Jika kursor berada di awal, ketuk dua kali memindahkannya ke akhir, dan SandFly mengumumkan "Akhir kolom".

Perintah **Aktifkan** melakukan hal yang sama saat dijalankan dari gerakan lain, perintah suara, perintah braille, atau skrip.

Pada kontrol lainnya, atau pada kolom teks tanpa fokus input dan keyboard yang tampil, ketuk dua kali mengaktifkan item seperti biasa. Setelan ini nonaktif secara default, dan tersedia di Android 13 dan yang lebih baru.

### Memilih, menyalin, memotong, dan menempel {#typing-select-copy-paste}

#### Memilih teks {#typing-select-text}

1. Pindahkan kursor ke tempat pemilihan dimulai.
2. Jalankan **Mulai atau akhiri mode pemilihan**. SandFly mengumumkan "Mode pemilihan aktif".
3. Pindahkan kursor dengan kontrol pembacaan, seperti **Karakter** atau **Kata**. Teks yang dilewati kursor akan dipilih.
4. Jalankan **Mulai atau akhiri mode pemilihan** lagi untuk mengakhiri mode pemilihan.

Pada Peta tombol NVDA, **Mulai atau akhiri mode pemilihan** adalah Tombol SandFly + F9. Pada Peta tombol SandFly, pintasannya Tombol SandFly + Shift + S.

**Pilih semua** memilih seluruh teks dalam kolom, dan **Hapus pilihan** menghapus pilihan tersebut.

Mode pemilihan juga berfungsi pada teks yang bukan di dalam kolom teks, seperti pesan atau halaman web, serta lintas item. SandFly menyimpan pilihan ini sendiri: hanya teks yang dilewati kursor yang dipilih, dan berpindah ke item lain memulai rentang baru di sana. **Salin** kemudian menyalin semua rentang tersebut, satu rentang per baris.

#### Menyalin, memotong, dan menempel {#typing-copy-cut-paste}

- **Salin** menyalin teks yang dipilih, atau teks dari item yang difokuskan.
- **Tambahkan ke papan klip** menambahkan teks yang dipilih, atau teks dari item yang difokuskan, ke akhir teks di papan klip, pada baris baru.
- **Potong** memotong teks yang dipilih di dalam kolom teks.
- **Tempel** menempelkan isi papan klip ke kolom teks di posisi kursor.

Secara default, ketuk dua kali lalu tahan dengan tiga jari menjalankan **Potong**, dan ketuk tiga kali lalu tahan dengan tiga jari menjalankan **Tempel**. Anda dapat menetapkan gerakan untuk perintah mana pun ini. Lihat [](#appendix-gestures) dan [](#commands).

#### Tindakan pengeditan dan menu pengeditan {#typing-editing-actions}

Saat fokus berada di kolom teks, kontrol pembacaan **Tindakan** dan item **Tindakan** di Menu SandFly menyertakan tindakan pengeditan: **Pindahkan kursor ke awal**, **Pindahkan kursor ke akhir**, **Mulai atau akhiri mode pemilihan**, **Potong**, **Salin**, **Tempel**, **Pilih semua**, dan **Pengeditan terperinci**. **Pengeditan terperinci** tidak ditawarkan di kolom sandi.

Perintah **Menu pengeditan** membuka menu berisi **Histori papan klip**, **Salin**, **Tambahkan ke papan klip**, **Potong**, **Tempel**, **Hapus teks**, **Pilih semua**, dan **Mulai atau akhiri mode pemilihan**.

#### Perintah pengeditan teks lainnya {#typing-editing-commands}

Grup **Pengeditan teks** pada **Perintah** juga memiliki:

- **Kirim teks**: menekan tombol yang mengirim atau menyerahkan teks di keyboard, seperti Kirim, Cari, atau Selesai.
- **Baris baru**: melakukan hal yang sama seperti Enter. Di kolom teks dengan beberapa baris, perintah ini mengetikkan baris baru di posisi kursor, dan SandFly mengucapkan "Baris baru". Di kolom teks satu baris, perintah ini menjalankan tindakan kolom, seperti Cari atau Berikutnya, atau jika tidak ada, melakukan hal yang sama seperti **Kirim teks**. Melalui suara, juga "paragraf baru" atau "tekan enter".
- **Hapus teks**: menghapus semua teks di kolom teks.
- **Jumlah karakter**: membacakan panjang teks, seperti "12 karakter, 3 kata". Di kolom sandi, hanya karakter yang dihitung.
- **Baca kolom teks**: membacakan seluruh teks dalam kolom teks, atau "Kosong" beserta petunjuknya jika tidak ada teks.
- **Saran ejaan/tata bahasa**: membuka saran untuk kesalahan ketik di posisi kursor. Kontrol pembacaan **Periksa tata bahasa** berpindah di antara kesalahan ketik.
- **Dikte**: lihat [](#typing-dictate).
- **Mulai pengeditan suara**: merekam perintah suara untuk kolom teks satu per satu. Lihat [](#voice-commands-editing).
- **Ganti keyboard**: beralih ke keyboard berikutnya. Tersedia di Android 11 dan yang lebih baru.
- **Telusuri emoji**, **Pilih simbol**, **Aktifkan atau nonaktifkan mode pengeditan**, **Eja kolom sandi**, **Saran kata berikutnya**, **Saran kata sebelumnya**, dan **Ketik saran kata**: perintah dari Keyboard SandFly. Lihat [](#keyboards).

Tidak satu pun perintah ini memiliki gerakan secara default. Untuk menetapkan gerakan, lihat [](#commands).

### Histori papan klip {#typing-clipboard-history}

SandFly menyimpan histori hingga 50 teks yang disalin, dari yang terbaru. Setiap teks disimpan hingga 200.000 karakter, dan histori secara keseluruhan hingga 1.000.000 karakter: teks lama yang melebihi batas itu dihapus. **Tambahkan ke papan klip** juga berhenti pada 200.000 karakter. Histori ini menyimpan teks yang disalin SandFly, dan teks yang disalin aplikasi lain saat jendela SandFly memiliki fokus. Android tidak mengizinkan aplikasi membaca papan klip di latar belakang, jadi dengan Shizuku atau root, tombol alih **Tambahkan salinan dari aplikasi lain** di bagian atas histori akan menambahkan setiap teks yang disalin oleh aplikasi lain. Histori disimpan terpisah dari setelan dan tidak disertakan dalam cadangan setelan.

Untuk membuka histori, jalankan **Histori papan klip**, dari perintah **Pengeditan teks** atau **Menu pengeditan**. Histori terbuka di atas layar saat ini.

- Ketuk dua kali teks untuk menempelkannya ke kolom teks tempat histori dibuka. Jika histori dibuka tanpa kolom teks, ketuk dua kali akan membuka tindakan teks tersebut.
- Ketuk dua kali lalu tahan teks untuk membuka tindakannya: **Tempel**, **Salin** lagi, **Tambahkan ke papan klip**, **Edit** teksnya, **Ekspor ke file**, atau **Hapus**. Tindakan ini juga ada di **Tindakan** setiap teks.
- **Pilih** menampilkan teks sebagai kotak centang. Ketuk dua kali teks untuk mencentangnya, lalu pilih **Hapus** atau **Ekspor ke file** untuk teks yang dicentang.
- **Ekspor ke file** menyimpan seluruh histori sebagai file teks. Beberapa teks dipisahkan oleh baris berisi tiga tanda hubung di antara baris kosong.
- **Hapus histori** menghapus semua teks.

Untuk menempel tanpa membuka histori, tambahkan kontrol pembacaan **Histori papan klip**, di grup **Tindakan untuk item saat ini** pada **Kontrol pembacaan**. Di kolom teks, geser ke atas atau ke bawah untuk menelusuri teks dalam histori, lalu ketuk dua kali untuk menempelkan teks yang terakhir dibacakan SandFly. Kontrol pembacaan ini nonaktif secara default. Lihat [](#commands).

### Menyalin teks dari layar {#typing-copy-from-screen}

Perintah-perintah ini berada di grup **Pengeditan teks** pada **Perintah**:

- **Salin frasa yang terakhir diucapkan**: menyalin teks terakhir yang dibacakan SandFly. Secara default, ketuk tiga kali dengan tiga jari menjalankannya.
- **Pilih frasa yang terakhir diucapkan kata demi kata**: membuka daftar kata dari frasa terakhir, agar Anda dapat mencentang kata yang ingin disalin.
- **Salin teks dari layar**: membuka daftar teks di layar, dibagi menjadi alamat dan nomor, karakter, kata, atau paragraf, agar Anda dapat mencentang bagian yang ingin disalin. Lihat [](#reading-copy-text).
- **Salin berkelanjutan**: memindahkan fokus dari item yang difokuskan melewati setiap item setelahnya, tanpa ucapan, menggulir daftar dan halaman di sepanjang jalan, dan mengumpulkan teks setiap item. Sentuh layar, tekan tombol, atau jalankan perintah lain untuk berhenti. Di akhir, setiap teks ditampilkan dengan kotak centang. Centang teks yang ingin disalin, lalu pilih **Simpan**. Salin berkelanjutan berhenti setelah 2.000 item atau 10 menit.

Dalam daftar-daftar ini, tindakan **Mulai pemilihan** dan **Akhiri pemilihan** pada sebuah item mencentang setiap item di antara dua item. Ketuk dua kali lalu tahan pada sebuah item melakukan hal yang sama.

### Pengeditan terperinci {#typing-granular-editing}

Pengeditan terperinci membuka teks dari kolom teks yang difokuskan, atau dari item berfokus mana pun yang memiliki teks, di layarnya sendiri yang dibagi menjadi beberapa bagian. Untuk membukanya, pilih **Pengeditan terperinci** pada tindakan kolom teks, atau jalankan perintah **Pengeditan terperinci** dari grup **Pengeditan teks** pada **Perintah**. Kolom sandi tidak dapat dibuka.

- **Bagi berdasarkan** memilih bagiannya: **Karakter**, **Kata**, **Kalimat**, **Baris**, atau **Paragraf**. Defaultnya adalah **Kata**, dan SandFly menyimpan pilihan terakhir Anda.
- Ketuk dua kali sebuah bagian untuk mengeditnya. Mengosongkannya akan menghapusnya.
- Tindakan untuk sebuah bagian adalah **Pilih**, **Sisipkan sebelum**, **Sisipkan setelah**, dan **Hapus**. Ketuk dua kali lalu tahan sebuah bagian untuk membukanya dalam dialog. **Mulai pemilihan** dan **Akhiri pemilihan** memilih setiap bagian di antara dua bagian.
- Spasi, tab, dan jeda baris di antara bagian menjadi bagian tersendiri, seperti "2 spasi" atau "Baris baru", dan baris kosong dibacakan sebagai "Baris kosong".
- **Salin** menyalin bagian yang dipilih, atau seluruh teks saat tidak ada bagian yang dipilih.
- **Lainnya** berisi **Urungkan**, **Ulangi**, **Cari**, **Temukan selanjutnya**, **Temukan sebelumnya**, **Ganti**, **Tambahkan teks di akhir**, **Pilih semua**, **Hapus pilihan**, **Potong teks yang dipilih**, dan **Hapus yang dipilih**. **Ganti** mengganti semua kecocokan, tanpa membedakan huruf besar/kecil.
- **Simpan** menuliskan teks kembali ke kolom teks dan menutup layar. Jika kolom teks sudah tidak ada, teks akan disalin sebagai gantinya.
- Tombol Kembali membuka dialog untuk menyimpan atau membuang perubahan.

Teks yang tidak berada di dalam kolom teks, seperti pesan, dapat diedit dan disalin, tetapi tidak dapat disimpan.

### Dikte {#typing-dictation}

#### Mendikte ke kolom teks {#typing-dictate}

Perintah **Dikte** mengetikkan apa yang Anda ucapkan ke kolom teks yang difokuskan, di posisi kursor.

1. Pindahkan fokus ke kolom teks.
2. Jalankan **Dikte**. Bunyi lonceng diputar, dan perekaman dimulai. Ucapan dibisukan saat SandFly merekam.
3. Berbicaralah. Perekaman tetap berlangsung saat Anda berhenti sejenak.
4. Jalankan **Dikte** lagi. Bunyi lonceng kedua diputar, dan SandFly mengetikkan teksnya.

Perekaman berhenti dengan sendirinya setelah 15 menit, atau setelah durasi hening yang Anda atur. Lihat [](#typing-dictation-settings).

Secara default, ketuk dua kali dengan dua jari menjalankan **Dikte, putar, atau jeda media**. Perintah ini memulai **Dikte** saat kolom teks memiliki fokus input, keyboard tampil atau terhubung, dan tidak ada panggilan aktif. Jika dikte sedang merekam, perintah ini menghentikannya. Dalam kondisi lain, perintah ini memutar atau menjeda media. Untuk gerakan default, lihat [](#appendix-gestures).

Di ponsel yang memiliki tombol bisukan, seperti Zinwa Q25, tahan tombol bisukan di kolom teks untuk mendikte: perekaman dimulai saat Anda menekan tombol dan berhenti saat Anda melepaskannya. Setelannya adalah **Tombol volume** > **Tahan tombol bisukan untuk mendikte di kolom teks**, yang aktif secara default. Lihat [](#buttons-mute-key).

#### Memilih cara ucapan dikenali {#typing-speech-recognition}

Buka **Mengetik** > **Dikte** > **Pengenalan ucapan**, lalu pilih salah satu opsi berikut:

- **Otomatis**: setelan default. SandFly menggunakan pengenalan ucapan Google di perangkat jika terinstal dan mendukung bahasa ponsel. Jika tidak, SandFly menggunakan Rambler jika ada kunci API Gemini dan Rambler mendukung bahasanya. Jika tidak, SandFly menggunakan Whisper jika model sudah diunduh. Jika semuanya tidak tersedia, SandFly menggunakan pengenalan ucapan Android. Layar menampilkan mana yang sedang digunakan, seperti "Otomatis: Rambler".
- **Pengenalan ucapan Google di perangkat**: pengenal dari aplikasi Speech Recognition & Synthesis, dengan paket bahasa luringnya, dalam bahasa ponsel. Di Android 13 dan yang lebih baru, ini menambahkan tanda baca dan huruf kapital saat **Tanda baca otomatis** aktif. Jika bahasa ponsel tidak memiliki paket bahasa, baris **Unduh** (bahasa) **untuk pengenalan di perangkat** membuka layar unduhan Speech Recognition & Synthesis.
- **Rambler (Gemini API)**: model Gemini 3.5 Transcribe, melalui kunci API Gemini yang diatur di layanan AI. Model ini menghapus kata pengisi, menyelesaikan koreksi diri, serta memformat tanda baca, angka, dan daftar. Lihat [](#descriptions) untuk kunci API Gemini.
- **Whisper**: pengenalan di ponsel, menggunakan model Whisper yang diunduh. Lihat [](#typing-whisper-models).
- **Pengenalan ucapan Android**: layanan pengenalan ucapan yang dipilih di setelan Android.

Jika pilihan Anda tidak dapat digunakan, SandFly beralih ke pilihan berikutnya dengan urutan yang sama seperti **Otomatis**. Pengenalan ucapan Google di perangkat dan Android mulai lagi setelah setiap jeda hingga Anda menghentikan dikte. Jika Rambler tidak merespons, rekaman akan ditranskripsikan dengan Whisper jika ada model yang diunduh.

**Dikte** mengenali bahasa keyboard, seperti bahasa yang dipilih di Gboard, jika berbeda dari bahasa ponsel. Pengenalan ucapan Google di perangkat kemudian memerlukan paket bahasa untuk bahasa keyboard; tanpa paket itu, cara pengenalan ucapan berikutnya dalam urutan **Otomatis** yang digunakan. Perintah suara dan input suara lainnya menggunakan bahasa ponsel.

Saat **Transkripsi langsung Rambler** aktif, SandFly mengalirkan rekaman saat Anda berbicara dan menerima teks setiap frasa saat Anda jeda, sehingga teks diketik lebih cepat setelah Anda berhenti. Jika aliran gagal, SandFly mengirimkan seluruh rekaman saat Anda berhenti. Setelan ini aktif secara default, dan hanya ditampilkan jika ada kunci API Gemini.

Catatan: Rekaman dikirim ke Google saat menggunakan Rambler. Dengan kunci API Gemini tingkat gratis, Google dapat menggunakan audio dan teks tersebut untuk meningkatkan produknya. Dengan kunci tingkat berbayar, Google tidak menggunakannya.

#### Model Whisper {#typing-whisper-models}

Whisper mengenali ucapan di ponsel, sehingga rekaman tetap berada di ponsel. **Mengetik** > **Dikte** > **Model Whisper** mencantumkan model yang dapat diunduh:

- **Whisper Tiny** (32 MB), **Whisper Base** (60 MB), **Whisper Small** (190 MB), dan **Whisper Large v3 Turbo** (574 MB): model multibahasa. Model yang lebih besar lebih akurat tetapi lebih lambat.
- **FUTO Tiny**, **FUTO Base**, dan **FUTO Small**, serta versi khusus bahasa Inggris dari masing-masing: model dari FUTO.

Untuk menggunakan model:

1. Pilih sebuah model. SandFly akan mengunduhnya, setelah konfirmasi jika menggunakan data seluler.
2. Untuk beralih ke model lain yang sudah diunduh, pilih modelnya, lalu pilih **Gunakan**. Untuk menghapus model, pilih modelnya, lalu pilih **Hapus**.

Model pertama yang Anda unduh akan langsung digunakan. Whisper mengenali bahasa seperti yang dijelaskan di [](#typing-speech-recognition).

**File model** menambahkan model Whisper milik Anda sendiri dalam format whisper.cpp, seperti model yang dilatih untuk bahasa tertentu, dari file di ponsel, hingga 4 GB. Setelah ditambahkan, Anda dapat menggunakannya, menggantinya dengan **Pilih file lain**, atau menghapusnya. File yang bukan model whisper.cpp akan ditolak.

**Prosesor**, hanya ditampilkan pada ponsel dengan GPU Adreno dan driver OpenCL, memilih apakah Whisper berjalan di **CPU** atau **GPU**. Jika Whisper tidak dapat berjalan di GPU, Whisper berjalan di CPU.

#### Mengoreksi dikte dengan AI {#typing-dictation-correction}

Saat **Mengetik** > **Dikte** > **Koreksi dikte dengan AI** aktif, SandFly mengirimkan teks dari pengenalan ucapan Google di perangkat, Whisper, atau Android ke layanan AI sebelum mengetiknya, beserta hingga 300 karakter dari teks sebelum kursor. Layanan tersebut menghapus kata pengisi, gagap, dan awal yang salah, menyelesaikan koreksi diri ("dua, bukan, tiga" menjadi "tiga"), menambahkan tanda baca dan huruf kapital, memperbaiki kata yang salah dengar, serta memformat angka, tanggal, dan daftar. Setelan ini aktif secara default. Teks dari Rambler tidak dikirim lagi, dan dikte ke kolom sandi tidak dikirim.

- **Layanan AI untuk dikte**: **Sama dengan Layanan AI** (default), layanan AI mana pun yang sudah disiapkan, atau **AI di perangkat** jika modelnya sudah diunduh. Dengan AI di perangkat, seluruh proses dikte tetap berada di ponsel. Lihat [](#descriptions).
- **Instruksi koreksi**: instruksi yang diterima layanan AI, seperti bahasa atau gaya. Kolom ini dimulai dengan perintah bawaan. Menyimpannya tanpa perubahan atau kosong akan tetap menggunakan perintah bawaan.

Jika layanan tidak merespons dalam 20 detik (60 detik untuk AI di perangkat), atau mengembalikan teks yang jauh lebih panjang dari ucapannya, SandFly mengetikkan teks sesuai hasil pengenalan aslinya. Menjalankan **Dikte** sebelum layanan merespons akan langsung melakukan hal yang sama.

#### Setelan dikte lainnya {#typing-dictation-settings}

Di **Mengetik** > **Dikte**:

- **Tanda baca otomatis**: pengenalan ucapan Google menambahkan tanda baca dan huruf kapital. Aktif secara default. Ditampilkan di Android 13 atau yang lebih baru jika pengenalan ucapan Google terinstal.
- **Blokir kata-kata kasar**: pengenalan ucapan Google menyensor kata-kata kasar. Aktif secara default. Ditampilkan bersama **Tanda baca otomatis**.
- **Berhenti otomatis setelah hening**: menghentikan perekaman setelah 1, 2, 3, 5, 10, 15, atau 30 detik, atau 1 menit tanpa ucapan. Defaultnya adalah **Nonaktif**.
- **Ucapkan teks yang didiktekan**: membacakan teks setelah SandFly mengetiknya, kecuali di kolom sandi. Nonaktif secara default.
- **Biarkan model pengenalan tetap dimuat**: berapa lama model Whisper dan model AI di perangkat tetap berada di memori setelah digunakan: **Keluarkan dari memori setelah digunakan** (default), 5, 15, atau 30 menit, atau **Sampai SandFly berhenti**. Model yang tetap dimuat mulai lebih cepat tetapi menggunakan memori.
- **Server unduhan model**: tempat model diunduh, **Hugging Face** (default) atau **hf-mirror.com**.

### Keyboard tulisan tangan {#typing-handwriting}

SandFly tulisan tangan adalah keyboard yang mengetikkan apa yang Anda tulis di layar dengan satu jari. Fitur ini tersedia di ponsel dan tablet dengan Android 11 atau yang lebih baru. Keyboard memenuhi layar, dan saat SandFly aktif, sentuhan di atasnya langsung masuk ke keyboard alih-alih melalui jelajah sentuh.

#### Mengaktifkan keyboard tulisan tangan {#typing-handwriting-turn-on}

1. Buka **Mengetik** > **Keyboard tulisan tangan**. Setelan keyboard Android akan terbuka.
2. Aktifkan **Tulisan tangan SandFly**.
3. Di kolom teks, pilih **Tulisan tangan SandFly** sebagai keyboard, misalnya dengan perintah **Ganti keyboard** atau pengalih keyboard Android.

Saat keyboard pertama kali dibuka, tutorial akan memandu Anda menulis huruf, kata, dan setiap gerakan, lalu berlanjut saat Anda melakukan setiap langkah. Tidak ada yang diketik ke kolom selama tutorial. Untuk mengakhirinya lebih awal, geser ke bawah dengan tiga jari. Untuk membukanya lagi, ketuk dengan tiga jari.

Tulisan tangan dikenali di ponsel oleh Google ML Kit, dalam bahasa ponsel. Pada kali pertama, SandFly menampilkan dialog untuk mengunduh model pengenalan, sekitar 20 MB, dan mengumumkan saat unduhan selesai.

#### Menulis dengan keyboard tulisan tangan {#typing-handwriting-write}

Tulis satu huruf, beberapa huruf, atau satu kata utuh di mana saja di layar. Goresan yang berurutan dalam waktu tunggu merupakan satu tulisan, yang kemudian diketik dan dibacakan. Titik, seperti pada huruf i, dihitung sebagai goresan.

**Mengetik** > **Tunggu setelah menulis** mengatur waktu tunggu, dari 0,2 hingga 1 detik. Defaultnya adalah 0,6 detik. Waktu tunggu yang lebih singkat mengetik lebih cepat, tetapi setiap goresan dari huruf bergoresan ganda, seperti H, harus dimulai dalam batas waktu tersebut. Setelan ini juga berlaku untuk perintah **Tulis huruf kontrol pembacaan** dan **Tulis nama aplikasi untuk membukanya**. Lihat [](#commands).

| Gerakan | Tindakan |
| --- | --- |
| Geser ke kanan dengan dua jari | Mengetik spasi |
| Geser ke kiri dengan dua jari | Menghapus karakter sebelum kursor |
| Geser ke bawah dengan dua jari | Baris baru, atau tindakan kolom, seperti Kirim, Cari, atau Selesai |
| Geser ke atas dengan dua jari | Mengganti tulisan terakhir dengan hasil bacaan berikutnya dengan panjang yang sama, seperti "h" untuk "H" |
| Ketuk dengan dua jari | Membacakan teks sebelum kursor |
| Geser ke bawah dengan tiga jari | Menyembunyikan keyboard |
| Geser ke kanan atau ke kiri dengan tiga jari | Beralih ke keyboard berikutnya atau sebelumnya (Android 9 dan yang lebih baru) |
| Ketuk dengan tiga jari | Membuka tutorial |

**Mengetik** > **Gema keyboard** > **Keyboard tulisan tangan** mengatur apa yang dibacakan keyboard saat Anda mengetik: setiap tulisan, setiap kata saat diakhiri spasi atau baris baru, keduanya, atau tidak sama sekali. Karakter yang dihapus dan hasil bacaan yang mengganti tulisan terakhir selalu dibacakan apa pun setelan gemanya. Di kolom teks SandFly, teks sebelum kursor tidak dibacakan, dan penghapusan hanya dibacakan sebagai "Dihapus".

Saat keyboard tulisan tangan ditampilkan, hanya perintah yang tidak membuka jendela yang dapat dijalankan, seperti navigasi dan kontrol pembacaan.

### Keyboard fisik {#typing-physical-keyboard}

Saat keyboard fisik terhubung, grup **Keyboard fisik** di **Mengetik** berisi **Tombol huruf menekan tombol keypad**, **Enter mengirim pesan**, **Keyboard SandFly**, **Ketikan tombol yang ditahan**, **Karakter saat tombol ditekan dua kali**, **Spasi dua kali mengetikkan titik**, **Koreksi otomatis**, **Saran kata**, **Ucapkan saran**, **Enter mengetik saran yang diucapkan**, **Bahasa daftar kata**, **Hapus kata yang dipelajari**, **Ucapkan karakter sandi**, dan **Setelan keyboard fisik Android**. Setelan-setelan ini dijelaskan di [](#keyboards).
