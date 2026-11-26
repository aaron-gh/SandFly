## Gerakan {#gestures}

SandFly menjalankan perintah untuk setiap gerakan yang Anda buat di layar sentuh: geseran, ketukan, geseran dari tepi layar, putaran dua jari, dan lainnya. Sebagian besar gerakan dapat menjalankan perintah apa pun, dan Anda dapat mengubahnya di **Gerakan**. Bab ini menjelaskan berbagai jenis gerakan, setelan pada layar **Gerakan**, gerakan di aplikasi tertentu, dan profil gerakan.

Perintah yang dijalankan setiap gerakan secara default tercantum di [](#appendix-gestures). Perintah itu sendiri dijelaskan di [](#commands).

### Cara SandFly mengenali gerakan {#gestures-recognition}

Sentuhan yang diam di layar atau bergerak perlahan adalah eksplorasi sentuhan: SandFly membacakan item di bawah jari. Sentuhan yang bergerak cepat, atau ketukan, adalah gerakan. Lihat [](#reading) untuk eksplorasi sentuhan.

Di Android 13 dan yang lebih baru, SandFly mengenali gerakan sendiri. Banyak gerakan dalam bab ini memerlukannya: rotor, gerakan tepi, gerakan sudut, usap bolak-balik, jepit dan rentangkan, geser dan tahan, geseran tiga bagian, geseran dua tarikan, ketuk lalu geser, gerakan berbentuk sudut dua jari, ketuk tiga kali dan ketuk empat kali dengan satu jari, ketuk empat kali dengan tiga jari, ketuk dua kali dengan dua jari di bagian atas dan bawah layar, serta ketukan empat jari di separuh atas atau bawah layar. Setelan waktu pada layar **Gerakan** juga memerlukannya. Gerakan dengan dua jari atau lebih memerlukan Android 11 atau yang lebih baru.

Pengenalan oleh SandFly dikendalikan oleh **Setelan developer** > **Kelola gerakan di SandFly**, yang aktif secara default. Saat Anda menonaktifkannya, Android yang mengenali gerakan, sehingga gerakan dan setelan di atas tidak tersedia. Nonaktifkan lalu aktifkan kembali SandFly setelah Anda mengubah setelan ini.

### Ketukan {#gestures-taps}

- **Satu jari**: ketuk dua kali mengaktifkan item yang difokuskan, dan ketuk dua kali lalu tahan menyentuh lama item tersebut. Gerakan ini tidak dapat diubah. **Ketuk tiga kali** dan **Ketuk empat kali**, dalam kategori **1 jari**, tidak melakukan apa pun secara default dan berfungsi di mana saja di layar. Keduanya tidak tersedia di jam tangan.
- **Dua, tiga, dan empat jari**: ketuk, ketuk dua kali, dan ketuk tiga kali dengan masing-masing jumlah jari, serta ketuk dua kali lalu tahan. Dua dan tiga jari juga memiliki ketuk tiga kali lalu tahan, sedangkan tiga jari juga memiliki ketuk lalu tahan serta ketuk empat kali.
- **Dua jari di atas dan bawah**: **Ketuk dua kali dengan 2 jari, atas dan bawah** adalah gerakan terpisah. Selama gerakan ini memiliki perintah, ketukan dua kali dengan dua jari, satu jari di sepertiga atas layar dan jari lainnya di sepertiga bawah, menjalankan gerakan ini, bukan **Ketuk dua kali dengan 2 jari**. Atas dan bawah mengikuti layar sebagaimana ditampilkan, juga dalam mode lanskap.
- **Empat jari di separuh layar**: **Ketuk dengan 4 jari di separuh atas** dan **Ketuk dengan 4 jari di separuh bawah** adalah gerakan terpisah. Selama salah satunya memiliki perintah, ketukan empat jari di separuh layar tersebut menjalankan gerakan itu, bukan **Ketuk dengan 4 jari**.

Ketukan dengan dua, tiga, atau empat jari menjalankan perintahnya segera setelah jari diangkat, kecuali jika gerakan yang lebih panjang dengan awalan ketukan yang sama memiliki perintah, seperti ketukan lanjutan, ketuk lalu tahan, atau ketuk lalu geser. Jika demikian, SandFly menunggu ketukan berikutnya. Jika ada jari yang bergerak selama ketukan beberapa jari, sentuhan tersebut tidak lagi dihitung sebagai ketukan. Ketukan empat kali dengan tiga jari selalu langsung dijalankan.

Ketuk dua kali dengan satu jari dijalankan segera setelah jari diangkat, kecuali jika **Ketuk tiga kali** atau **Ketuk empat kali** memiliki perintah. Jika demikian, SandFly menunggu ketukan lanjutan, dan ketuk dua kali baru mengaktifkan item setelah jeda tersebut. Ketuk tiga kali hanya menunggu selama **Ketuk empat kali** memiliki perintah. Di sudut yang memiliki gerakan sudut, ketukan dihitung sebagai ketukan sudut. Lihat [](#gestures-corners).

**Waktu antara ketukan satu jari** mengatur berapa lama SandFly menunggu ketukan satu jari berikutnya. Lihat [](#gestures-timing).

### Geseran {#gestures-swipes}

Geseran menggerakkan satu jari atau lebih secara cepat melintasi layar ke satu arah. Geseran satu jari ke kiri dan ke kanan berpindah ke item sebelumnya dan berikutnya, sedangkan geseran ke atas dan ke bawah mengubah nilai kontrol pembacaan saat ini. Lihat [](#reading).

Geseran dua jari dapat menggulir aplikasi atau menjalankan perintah SandFly. Lihat [](#gestures-two-finger-swipes). Geseran tiga dan empat jari menjalankan perintah SandFly.

#### Gerakan bolak-balik dan berbentuk sudut {#gestures-two-direction}

Gerakan bolak-balik menggeser ke satu arah lalu kembali tanpa mengangkat jari, seperti **Geser ke atas lalu ke bawah**. Gerakan berbentuk sudut menggeser ke satu arah lalu berbelok tegak lurus, seperti **Geser ke bawah lalu ke kanan**. Keduanya berada di kategori **1 jari bolak-balik** dan **Gerakan berbentuk sudut dengan 1 jari** pada layar **Gerakan**.

**Gerakan berbentuk sudut dengan 2 jari** memiliki delapan gerakan sudut dengan dua jari, seperti **Geser ke atas lalu ke kanan dengan 2 jari**. Secara default tidak melakukan apa pun, hanya berfungsi saat **SandFly menangani geser dua jari** aktif, dan disembunyikan saat setelan itu nonaktif. Dua jari yang digerakkan ke satu arah lalu kembali adalah gerakan usap bolak-balik, sehingga tidak ada gerakan bolak-balik dua jari.

**Kenali geseran sebelum jari diangkat** menonaktifkan dan menyembunyikan gerakan satu jari bolak-balik dan sudut serta gerakan sudut dua jari. Lihat [](#gestures-swipes-before-lift).

#### Geseran tiga bagian {#gestures-three-part}

Geseran tiga bagian menggerakkan satu jari ke tiga arah tanpa mengangkatnya, setiap bagian berbelok dari arah sebelumnya, seperti **Geser ke kanan, ke atas, lalu ke kanan** atau **Geser ke atas, ke bawah, lalu ke atas**. Ke-36 gerakan ini berada di kategori **Geseran tiga bagian** pada layar **Gerakan**. Secara default tidak melakukan apa pun, dan geseran tiga bagian tanpa perintah tidak dikenali. Setiap bagian harus sepanjang satu bagian gerakan sudut. **Kenali geseran sebelum jari diangkat** menonaktifkan dan menyembunyikannya. Tidak tersedia di jam tangan.

#### Geser dan tahan {#gestures-swipe-and-hold}

Setiap geseran satu jari, geseran bolak-balik, dan geseran sudut memiliki versi yang diakhiri dengan jari diam di layar, seperti **Geser ke kanan dan tahan** atau **Geser ke kiri lalu ke atas dan tahan**. Geser, lalu diamkan jari Anda: perintah dijalankan saat waktu tahan berakhir, sebelum Anda mengangkat jari. Mengangkat jari lebih cepat menjadikannya geseran biasa.

Jeda yang lebih singkat daripada waktu tahan saat gerakan sudut atau bolak-balik berbelok tetap menjadi bagian dari geseran tersebut. Jeda yang lebih lama menjadi geser dan tahan pada arah pertama, jika arah tersebut memiliki perintah.

Ke-16 gerakan ini berada di bagian akhir kategori **1 jari**, **1 jari bolak-balik**, dan **Gerakan berbentuk sudut dengan 1 jari**, dan secara default tidak melakukan apa pun. Geser dan tahan tanpa perintah tidak dikenali, sehingga geseran bekerja seperti biasa hingga Anda menetapkan perintah padanya. **Waktu tahan setelah geser** mengatur lama jari diam. Lihat [](#gestures-timing). Tidak tersedia di jam tangan.

#### Geseran dua tarikan {#gestures-two-stroke}

Geseran dua tarikan adalah dua geseran satu jari dengan jari diangkat di antara keduanya, seperti **Geser ke kanan, angkat, lalu geser ke atas**. Masing-masing geseran dapat berupa geseran satu arah, bolak-balik, atau sudut, seperti **Geser ke atas lalu ke kiri, angkat, lalu geser ke kanan** atau **Geser ke atas, angkat, lalu geser ke kanan lalu ke bawah**, sehingga berjumlah 256 gerakan, termasuk geseran yang sama dua kali. Secara default tidak melakukan apa pun. Di layar **Gerakan**, 16 gerakan yang terdiri dari geseran satu arah berada di kategori **Geseran dua tarikan**, yang diawali geseran satu arah dan diakhiri geseran bolak-balik atau sudut berada di **Geseran dua tarikan yang diakhiri bolak-balik atau sudut**, dan sisanya berada di kategori tersendiri untuk setiap geseran pertama, seperti **Geseran dua tarikan diawali Geser ke atas lalu ke kiri**.

Gerakan ini hanya berfungsi saat **Waktu antara geseran dua tarikan** diatur. Lihat [](#gestures-timing). Saat bernilai **Nonaktif**, kategori geseran dua tarikan disembunyikan. Geseran kedua harus dimulai dalam batas waktu tersebut setelah geseran pertama diangkat. Setelah suatu geseran, SandFly hanya menunggu geseran kedua jika ada geseran dua tarikan yang diawali geseran tersebut dan memiliki perintah, baik di tata letak gerakan, profil gerakan yang digunakan, maupun gerakan aplikasi. Geseran lain langsung dijalankan. Jika waktunya habis, atau sentuhan berikutnya adalah ketukan, eksplorasi sentuhan, atau gerakan dengan lebih banyak jari, geseran pertama dijalankan sebagai geseran biasa.

Geseran dua tarikan yang diawali geseran bolak-balik atau sudut tidak membuat geseran satu arah menunggu geseran kedua, sehingga perintah dapat ditempatkan pada geseran dua tarikan sementara geseran satu arah tetap langsung dijalankan. Geseran bolak-balik atau sudut di dalam geseran dua tarikan yang memiliki perintah tetap dikenali meskipun tidak memiliki perintahnya sendiri. **Kenali geseran sebelum jari diangkat** menonaktifkan dan menyembunyikan geseran dua tarikan yang menggunakan geseran bolak-balik atau sudut. Tidak tersedia di jam tangan.

### Rotor {#gestures-rotor}

Putar dua jari di layar seperti memutar tombol putar untuk memilih kontrol pembacaan: searah jarum jam untuk kontrol pembacaan berikutnya dan berlawanan arah jarum jam untuk kontrol pembacaan sebelumnya. Lalu geser ke atas atau ke bawah untuk mengubah nilainya. Kontrol pembacaan dijelaskan di [](#commands).

Setiap langkah putaran memindahkan satu kontrol pembacaan, sehingga Anda dapat terus memutar untuk berpindah lebih jauh. **Gerakan** > **Putaran rotor per langkah**, di bawah **2 jari**, mengatur sudut tiap langkah dari 15 hingga 60 derajat. Nilai default-nya adalah 30 derajat, seperdua belas putaran. Pilih langkah yang lebih besar jika Anda sering melewati kontrol pembacaan yang diinginkan. Menggulir dengan dua jari tidak memutar rotor.

Anda dapat menetapkan perintah lain untuk **Putar searah jarum jam dengan 2 jari** dan **Putar berlawanan arah jarum jam dengan 2 jari**.

### Usap bolak-balik, jepit, dan rentangkan {#gestures-scrub-pinch-spread}

Untuk mengusap bolak-balik, gerakkan dua jari dengan cepat ke satu arah melintasi layar lalu kembali, masing-masing tarikan sekitar satu sentimeter. Secara default, usap bolak-balik melakukan Kembali. Dua jari yang digerakkan ke satu arah saja tetap menggulir. Selama usap bolak-balik masih mungkin terjadi, pengguliran menyamping dengan dua jari dapat menunggu hingga sekitar setengah detik sebelum dimulai.

**Jepit dengan 2 jari**, **Rentangkan dengan 2 jari**, serta gerakan yang sama dengan 3 dan 4 jari tidak melakukan apa pun secara default. Jepit atau rentangkan dihitung setelah jari bergerak sekitar setengah sentimeter mendekati atau menjauhi titik tengahnya, tanpa bergeser bersama-sama atau banyak berputar. Selama jepit atau rentangkan dua jari memiliki perintah, gerakan dua jari yang lebih banyak menjepit daripada bergeser akan menunggu sebelum diteruskan ke aplikasi, sehingga aplikasi yang melakukan zoom dengan dua jari mungkin tidak dapat di-zoom hingga keduanya diatur ke **Tidak ditetapkan**.

Gerakan usap bolak-balik, jepit, dan rentangkan tidak tersedia di jam tangan.

### Gerakan tepi {#gestures-edges}

Gerakan tepi dimulai tepat dari tepi layar, seperti pada bilah navigasi gerakan atau bilah status. Gerakan ini berada di kategori **Tepi** pada layar **Gerakan** dan tidak tersedia di jam tangan.

#### Menggeser masuk dari tepi {#gestures-edges-slide-in}

Geser satu jari masuk dari tepi layar:

- **Geser ke atas dari tepi bawah**: setelah sekitar 1 cm, SandFly memutar bunyi klik seperti tombol layar utama. Angkat di sana untuk membuka layar utama. Terus geser sekitar 4 cm lagi hingga klik kedua, lalu angkat di sana untuk **Geser panjang ke atas dari tepi bawah**, yang membuka aplikasi terbaru.
- **Geser ke bawah dari kiri atas** membuka notifikasi dan **Geser ke bawah dari kanan atas** membuka setelan cepat, setelah klik pertama yang sama. **Geser panjang ke bawah dari kiri atas** dan **Geser panjang ke bawah dari kanan atas**, pada klik kedua, secara default tidak melakukan apa pun.
- Ke kanan dari tepi kiri dan ke kiri dari tepi kanan. Separuh atas dan separuh bawah masing-masing tepi samping adalah gerakan terpisah, masing-masing memiliki geseran pendek dan panjang, seperti **Geser ke kanan dari tepi kiri, separuh atas**. Secara default tidak melakukan apa pun.

Hanya geseran yang memiliki perintah yang mengeluarkan bunyi klik. Geseran tanpa perintah di klik pertama tetap menjadi geseran biasa hingga mencapai klik kedua. Geseran panjang tanpa perintah tidak memiliki klik kedua. Mengangkat jari sebelum klik pertama menjadikannya geseran biasa.

Menggeser kembali dari klik kedua ke klik pertama memutar satu bunyi ketukan pendek, dan menggeser kembali ke tepi memutar dua bunyi ketukan. Mengangkat jari di tepi membatalkan gerakan. Menyentuh tepi, klik pertama, dan klik kedua masing-masing memutar suara yang nadanya naik di setiap tahap. Suara dan getarannya dapat dinonaktifkan di **Suara dan getaran satu per satu**. Lihat [](#sound).

Geseran yang dimulai jauh dari tepi, atau memerlukan waktu lebih dari sekitar setengah detik untuk mencapai klik pertama, menjadi geseran biasa atau eksplorasi sentuhan. Jari yang diam di tepi selama 0,2 detik memulai eksplorasi sentuhan.

Di sudut layar, arah geseran menentukan gerakannya. Geseran yang cocok dengan dua gerakan menggunakan tepi atas atau bawah, dan geseran masuk diprioritaskan sebelum geseran menyusuri tepi.

#### Menggeser menyusuri tepi {#gestures-edges-slide-along}

Anda juga dapat menggeser satu jari menyusuri tepi: ke atas atau ke bawah di tepi kiri atau kanan, atau ke kiri atau ke kanan di tepi atas atau bawah, seperti **Geser ke atas di sepanjang tepi kiri**. Mulai tepat dari tepi dan jaga jari tetap dekat tepi. Perintah dijalankan saat Anda mengangkat jari setelah menggeser sejauh jarak ke klik pertama. Gerakan ini secara default tidak melakukan apa pun dan tidak memutar suara atau getaran tepi.

#### Menggulir menyusuri tepi {#gestures-edges-scroll}

Untuk menggulir aplikasi dengan satu jari menyusuri tepi, aktifkan **Gulir dengan satu jari di sepanjang tepi kiri**, **Gulir dengan satu jari di sepanjang tepi kanan**, **Gulir dengan satu jari di sepanjang tepi atas**, atau **Gulir dengan satu jari di sepanjang tepi bawah** di bawah **Tepi**. Semuanya nonaktif secara default. Geseran menyusuri tepi tersebut kemudian menggerakkan konten aplikasi mengikuti jari, seperti geseran dua jari: ke atas dan ke bawah di tepi kiri dan kanan, ke kiri dan ke kanan di tepi atas dan bawah.

Saat suatu tepi digunakan untuk menggulir, gerakan yang menggeser menyusuri tepi tersebut tidak digunakan. Geseran masuk dari tepi tetap berfungsi sebagai gerakan, begitu pula geseran ke atas dan ke bawah dari sudut.

#### Menu tepi {#gestures-edge-menus}

Tepi kiri dan tepi kanan masing-masing dapat dijadikan menu perintah. Untuk menyiapkan menu tepi:

1. Buka **Gerakan** > **Tepi** > **Menu tepi kiri** atau **Menu tepi kanan**.
2. Aktifkan **Gunakan tepi kiri sebagai menu** atau **Gunakan tepi kanan sebagai menu**.
3. Pilih **Tambahkan perintah**, lalu pilih perintah dari daftar yang sama seperti untuk gerakan. Satu menu menampung hingga 20 perintah.

Setiap perintah memiliki **Ubah perintah**, **Pindahkan ke atas**, **Pindahkan ke bawah**, dan **Hapus**.

Untuk menggunakan menu, mulai tepat dari tepi lalu geser ke atas atau ke bawah menyusurinya. Perintah membagi panjang tepi secara merata dari atas, sehingga setiap perintah selalu berada di posisi yang sama. Saat jari mencapai suatu perintah, SandFly mengucapkan namanya disertai bunyi klik ringan. Mengangkat jari menjalankan perintah di bawah jari. Jika Anda menggerakkan jari menjauh dari tepi lebih dari tiga kali lebar tepi, SandFly mengucapkan "Batal", dan mengangkat jari di sana tidak menjalankan apa pun. Kembali ke tepi mengembalikan Anda ke daftar perintah.

Saat suatu tepi dijadikan menu, gerakan yang menggeser menyusuri tepi itu dan pengguliran menyusuri tepi itu tidak digunakan. Geseran masuk dari tepi tersebut tetap menjadi gerakan. Geseran yang dimulai di keyboard tidak membuka menu, sehingga tombol di tepi keyboard tetap mengetik seperti biasa.

#### Setelan tepi {#gestures-edge-settings}

Di bawah **Tepi** pada layar **Gerakan**:

- **Lebar tepi**: seberapa jauh dari tepi sentuhan dapat dimulai, dari 0,5 hingga 2 cm. Default-nya adalah 0,5 cm.
- **Jarak ke klik pertama**: seberapa jauh Anda menggeser masuk dari tepi, atau menyusurinya, menuju klik pertama, dari 0,5 hingga 2 cm. Default-nya adalah 1 cm.
- **Jarak dari klik pertama ke kedua**: seberapa jauh Anda terus menggeser menuju klik kedua, dari 1 hingga 5 cm. Default-nya adalah 4 cm.
- **Ucapkan perintah di setiap klik**: pada setiap klik saat menggeser masuk dari tepi, SandFly mengucapkan perintah yang akan dijalankan jika jari diangkat di sana, seperti "Layar utama". Nonaktif secara default.

### Gerakan sudut {#gestures-corners}

Setiap sudut layar memiliki lima gerakan satu jari, di bawah **Sudut**: ketuk, ketuk dua kali, ketuk tiga kali, ketuk dua kali lalu tahan, serta ketuk tiga kali lalu tahan, seperti **Ketuk dua kali sudut kiri atas**. Secara default tidak melakukan apa pun.

Sudut tanpa perintah yang ditetapkan bekerja seperti biasa. Di sudut yang memiliki perintah, ketukan yang tidak dipakai SandFly tetap menjadi ketukan biasa: ketuk dua kali tetap mengaktifkan. Ketuk dua kali hanya menunggu ketukan ketiga jika sudut tersebut memiliki perintah ketuk tiga kali. Sentuhan yang bergerak, menambah jari, atau diam tanpa gerakan tahan adalah sentuhan biasa, sehingga geseran dan gerakan tepi dari sudut tetap berfungsi. Gerakan sudut tidak dimulai di atas keyboard.

- **Ukuran sudut**: ukuran persegi di setiap sudut, dari 0,5 hingga 2 cm. Default-nya adalah **Sama dengan lebar tepi**.
- **Batas waktu ketukan sudut**: berapa lama ketukan di sudut boleh tetap menempel sebelum dihitung sebagai tahan, dari 0,15 hingga 0,5 detik. Default-nya adalah **Default sistem**.

Tidak tersedia di jam tangan.

### Ketuk, lalu geser {#gestures-tap-then-swipe}

Kategori **Ketuk, lalu geser** memuat gerakan yang mengetuk satu, dua, atau tiga kali dengan dua, tiga, atau empat jari lalu menggeser dengan jumlah jari yang sama, seperti **Ketuk dua kali, lalu geser ke atas dengan 2 jari**. Kategori ini juga memuat gerakan yang mengetuk dengan dua jari lalu menggeser dengan satu jari, seperti **Ketuk dengan 2 jari, lalu geser ke atas**. Secara default tidak melakukan apa pun.

Geseran adalah sentuhan berikutnya setelah ketukan terakhir dan harus dimulai dalam 0,25 detik setelahnya. Selama salah satu gerakan ini memiliki perintah, ketukan awalannya akan menunggu geseran. Tidak tersedia di jam tangan.

### Ketuk belakang {#gestures-back-tap}

Ketuk bagian belakang ponsel dua, tiga, atau empat kali dengan jari untuk menjalankan **Ketuk dua kali di bagian belakang**, **Ketuk tiga kali di bagian belakang**, atau **Ketuk empat kali di bagian belakang**, di bawah **Ketuk belakang** pada layar **Gerakan**. Secara default tidak melakukan apa pun. SandFly mendeteksi ketukan menggunakan akselerometer, hanya saat layar menyala, SandFly tidak dijeda, dan ketuk belakang memiliki perintah. Ketukan saat Anda menyentuh layar tidak dihitung.

**Sensitivitas ketuk belakang** mengatur seberapa keras ketukan harus dilakukan: **Pendek** memerlukan ketukan paling keras, **Panjang** paling ringan. Default-nya adalah **Sedang**. Naikkan jika ketukan sering terlewat, dan turunkan jika ketuk belakang terpicu saat Anda meletakkan atau membawa ponsel.

Ketukan hanya menunggu ketukan lanjutan jika gerakan dengan lebih banyak ketukan memiliki perintah. Jadi, ketuk dua kali dijalankan lebih cepat jika **Ketuk tiga kali di bagian belakang** dan **Ketuk empat kali di bagian belakang** tidak memiliki perintah. Ketuk belakang tidak tersedia di jam tangan atau TV. Ketuk belakang tetap berfungsi saat sentuhan dikunci. Lihat [](#touch).

### Goyangkan {#gestures-shake}

Goyangkan ponsel dengan cepat bolak-balik untuk menjalankan **Goyangkan**, di bawah **Goyangkan** pada layar **Gerakan**. Secara default tidak melakukan apa pun. Satu goyangan terdiri dari setidaknya dua ayunan ke masing-masing arah dalam sekitar dua pertiga detik. Goyangan yang terus berlanjut hanya menjalankan **Goyangkan** sekali. Goyangan berikutnya baru dihitung setelah ponsel diam sejenak. SandFly mendeteksi goyangan dengan akselerometer, hanya saat layar menyala, SandFly tidak dijeda, dan **Goyangkan** memiliki perintah.

**Sensitivitas goyangan** mengatur seberapa kuat ponsel harus digoyangkan: **Pendek** memerlukan goyangan paling kuat, **Panjang** paling ringan. Default-nya adalah **Sedang**.

Goyangkan tidak tersedia di jam tangan atau TV, dan tetap berfungsi saat sentuhan dikunci.

### Gerakan sidik jari {#gestures-fingerprint}

Di ponsel yang sensor sidik jarinya melaporkan geseran, kategori **Gerakan sidik jari** memiliki **Geser ke atas pada sensor sidik jari**, **Geser ke bawah pada sensor sidik jari**, **Geser ke kiri pada sensor sidik jari**, dan **Geser ke kanan pada sensor sidik jari**. Kategori ini disembunyikan di ponsel lain. Profil gerakan tidak mengubah gerakan sidik jari.

### Layar Gerakan {#gestures-screen}

**Gerakan** di setelan SandFly berisi, dari atas:

- **Telusuri**: mencari gerakan berdasarkan namanya atau perintah yang dijalankannya, lalu membuka daftar perintahnya.
- **Berlatih gerakan**: membuka halaman tempat SandFly menyebutkan fungsi setiap gerakan yang Anda buat. Lihat [](#gestures-practice).
- **Reset setelan gerakan**: lihat [](#gestures-reset).
- **Tata letak gerakan** dan **SandFly menangani geser dua jari**: lihat [](#gestures-layout) dan [](#gestures-two-finger-swipes).
- Setelan waktu: lihat [](#gestures-timing).
- **Kenali geseran sebelum jari diangkat**: lihat [](#gestures-swipes-before-lift).
- **Gerakan di aplikasi**, **Profil gerakan**, dan **Pintasan aplikasi**: lihat [](#gestures-in-apps), [](#gestures-profiles), dan [](#gestures-app-shortcuts).
- Kategori gerakan: **1 jari**, **1 jari bolak-balik**, **Gerakan berbentuk sudut dengan 1 jari**, **Tepi**, **Sudut**, **2 jari**, **Gerakan berbentuk sudut dengan 2 jari**, **3 jari**, **4 jari**, **Ketuk, lalu geser**, **Gerakan sidik jari**, **Ketuk belakang**, **Goyangkan**, **Geseran tiga bagian**, **Geseran dua tarikan**, **Geseran dua tarikan yang diakhiri bolak-balik atau sudut**, serta kategori **Geseran dua tarikan diawali** untuk setiap geseran bolak-balik dan sudut.

Kategori-kategori tersebut dalam keadaan diciutkan. Aktifkan kategori untuk meluaskannya. Setiap gerakan dalam kategori menampilkan perintah yang dijalankannya.

Di jam tangan, layar ini juga memiliki **Cadangkan gerakan untuk Vibration Watch**. Lihat [](#devices).

#### Tata letak gerakan {#gestures-layout}

**Tata letak gerakan** memilih gerakan awal yang digunakan layar **Gerakan**:

- **Gerakan SandFly**: gerakan default SandFly.
- **Gerakan VoiceOver**: gerakan VoiceOver di iPhone, seperti ketuk dua jari untuk menjeda ucapan, ketuk dua kali dua jari untuk **Dikte, putar, atau jeda media**, geseran tiga jari untuk menggulir, dan ketuk tiga jari untuk **Ucapkan posisi**. Gerakan yang tidak dimiliki VoiceOver bernilai **Tidak ditetapkan**, dan Anda dapat menetapkannya sendiri.
- **Gerakan BlackBerry**: gerakan yang mirip dengan gerakan BlackBerry 10 Screen Reader (BlackBerry 10 OS 10.3). Tidak semua gerakan sama. Geser ke kanan dan ke kiri berpindah menurut setelan navigasi, sedangkan geser ke atas dan ke bawah memilih setelan navigasi berikutnya dan sebelumnya, seperti karakter, kata, atau baris. Geser ke bawah lalu ke kiri menjeda ucapan, geser ke bawah lalu ke kanan memulai Tugas cepat, geser ke bawah lalu ke atas mengucapkan waktu dan tanggal, dan geser ke atas lalu ke bawah mengucapkan status. Ketuk dua jari mengucapkan judul layar, ketuk dua kali dua jari adalah **Dikte, putar, atau jeda media**, yang menjawab dan mengakhiri panggilan, dan ketuk dua kali lalu tahan dengan dua jari mengakhiri panggilan. Geser dua jari ke atas dan ke bawah menggulir mundur dan maju, dan geser dua jari ke bawah lalu ke kiri mengaktifkan atau menonaktifkan ucapan. Geser tiga jari ke bawah membaca dari atas, dan geser tiga jari ke kanan dan ke kiri menggulir ke kiri dan ke kanan. **Geser ke bawah dari kiri atas** dan **Geser ke bawah dari kanan atas** membuka setelan cepat, dan geseran panjangnya membuka notifikasi. Gerakan yang tidak dimiliki BlackBerry 10 Screen Reader bernilai **Tidak ditetapkan**, dan Anda dapat menetapkannya sendiri. Lihat [](#gestures-quick-tasks). Gerakan ini kemungkinan terasa asing jika Anda belum pernah menggunakan BlackBerry 10. Karena itu, memilihnya akan membuka dialog **Gunakan gerakan BlackBerry?**. Pilih **Gunakan** untuk mengonfirmasi. Gerakan ini tidak ada di dialog **Pilih gerakan**.

Saat pertama kali SandFly dimulai, dialog **Pilih gerakan** terbuka agar Anda dapat memilih **Gerakan SandFly** atau **Gerakan VoiceOver**. Setiap tata letak menyimpan perubahannya sendiri, sehingga beralih kembali akan memulihkan gerakan seperti yang Anda atur. Memilih tata letak mengaktifkan **SandFly menangani geser dua jari** untuk **Gerakan VoiceOver** dan **Gerakan BlackBerry**, dan menonaktifkannya untuk **Gerakan SandFly**. Dengan **Gerakan VoiceOver** dan **Gerakan BlackBerry**, **Layar utama** di layar kunci mengirim geseran dua jari ke atas, yang membuka kunci ponsel atau menampilkan PIN, pola, atau sandi. Gerakan di aplikasi dan profil gerakan berlaku untuk setiap tata letak.

#### Tugas cepat {#gestures-quick-tasks}

**Tugas Cepat** mirip dengan mode Quick Tasks pada BlackBerry 10 Screen Reader. SandFly mengucapkan "Tugas cepat", dan selama 3 detik gerakan berikut menjalankan perintah berikut:

| Gerakan | Perintah |
| --- | --- |
| Geser ke atas, geser ke bawah | Naikkan volume, turunkan volume |
| Geser ke kanan, geser ke kiri | Tambah kecepatan bicara, kurangi kecepatan bicara |
| Geser dua jari ke atas | Ulangi frasa yang terakhir diucapkan |
| Geser dua jari ke bawah | Eja frasa yang terakhir diucapkan |
| Ketuk dua jari | Sembunyikan atau tampilkan layar |

Setiap gerakan ini memberi tambahan 3 detik. Gerakan lain mengakhiri Tugas cepat dan menjalankan perintahnya sendiri. Saat waktunya habis, ponsel bergetar. Di tata letak BlackBerry, Tugas cepat adalah geser ke bawah lalu ke kanan. Di tata letak lain, tetapkan gerakannya di layar **Gerakan**. Perintah ini ada di bawah **SandFly** pada layar **Perintah**.

#### Tangani geseran dua jari di SandFly {#gestures-two-finger-swipes}

Saat **SandFly menangani geser dua jari** nonaktif, geseran dua jari diteruskan ke aplikasi, yang menggunakannya untuk menggulir, dan keempat geseran dua jari di layar **Gerakan** menampilkan "tidak dapat disesuaikan". Saat aktif, geseran dua jari menjalankan perintah yang ditetapkan padanya, dan gerakan **Gerakan berbentuk sudut dengan 2 jari** tersedia. Di layar kunci dan layar utama, gerakan untuk **Gulir ke atas** mengirim geseran ke atas sebagai gantinya, yang membuka kunci ponsel atau menampilkan PIN, pola, atau sandi, atau membuka daftar aplikasi. Setelan ini memerlukan Android 11 atau yang lebih baru.

#### Setelan waktu {#gestures-timing}

- **Waktu antara ketukan satu jari**: berapa lama SandFly menunggu ketukan lanjutan sebelum memutuskan bahwa gerakan ketukan satu jari selesai, dari 0,1 hingga 2 detik. Default-nya adalah 0,25 detik. Waktu lebih singkat membuat ketukan dua kali satu jari merespons lebih cepat, tetapi ketukan dua kali yang lambat dapat terhitung sebagai dua ketukan terpisah. Dengan waktu lebih lama, dua ketukan pada item dalam rentang itu akan mengaktifkannya, dan ketukan sudut serta ketukan tunggal multijari menunggu lebih lama. Dengan waktu lebih dari 0,3 detik, ketukan tunggal memfokuskan item di bawah jari 0,25 detik setelah jari diangkat, dan ketukan kedua dalam rentang waktu itu tetap mengaktifkan item tersebut. Ketukan dengan dua jari atau lebih selalu memberi waktu setidaknya 0,25 detik antar-ketukan.
- **Waktu tahan setelah geser**: berapa lama jari diam setelah geseran untuk gerakan geser dan tahan, dari 0,1 hingga 0,5 detik. Default-nya adalah 0,25 detik. Lihat [](#gestures-swipe-and-hold).
- **Waktu mulai gerakan**: berapa lama geseran satu jari boleh bergerak pada sentimeter pertamanya sebelum sentuhan berubah menjadi eksplorasi sentuhan, dari 0,15 hingga 2 detik. Default-nya adalah 0,15 detik. Pilih waktu lebih lama jika geseran yang lambat sering memicu eksplorasi sentuhan.
- **Waktu antara geseran dua tarikan**: berapa lama setelah satu geseran, geseran kedua dari geseran dua tarikan dapat dimulai: **Nonaktif**, 0,25, 0,5, 1, 1,5, atau 2 detik. Default-nya adalah **Nonaktif**, yang menonaktifkan geseran dua tarikan dan menyembunyikan kategorinya. Lihat [](#gestures-two-stroke).

Setelan ini memerlukan pengenalan gerakan oleh SandFly. Lihat [](#gestures-recognition). **Waktu tahan setelah geser**, **Waktu mulai gerakan**, dan **Waktu antara geseran dua tarikan** tidak tersedia di jam tangan.

#### Kenali geseran sebelum jari diangkat {#gestures-swipes-before-lift}

Dengan **Kenali geseran sebelum jari diangkat** aktif, geseran satu jari menjalankan perintahnya begitu jari bergerak sejauh 1 cm, sebelum Anda mengangkatnya. Karena gerakan bolak-balik dan sudut dimulai seperti geseran tunggal, setelan ini menonaktifkan dan menyembunyikan kategori **1 jari bolak-balik**, **Gerakan berbentuk sudut dengan 1 jari**, **Geseran tiga bagian**, dan **Gerakan berbentuk sudut dengan 2 jari** serta kategori geseran dua tarikan yang memakai geseran bolak-balik dan sudut. Geseran tetap menunggu jari diangkat jika geser dan tahan pada arah tersebut memiliki perintah, atau jika gerakan tepi masih mungkin terjadi. Nonaktif secara default.

Saat setelan ini nonaktif, geseran juga dijalankan sebelum jari diangkat apabila tidak ada gerakan yang dimulai ke arah tersebut yang memiliki perintah: tidak ada gerakan bolak-balik atau sudut, tidak ada geser dan tahan dari keduanya, dan tidak ada geseran tiga bagian. Pada **Gerakan SandFly**, gerakan bolak-balik memiliki perintah, sedangkan pada **Gerakan VoiceOver**, tidak satu pun gerakan tersebut memiliki perintah.

Setiap profil gerakan dapat mengaktifkan atau menonaktifkan setelan ini untuk dirinya sendiri. Lihat [](#gestures-profiles). Tidak tersedia di jam tangan.

### Mengubah fungsi gerakan {#gestures-change}

1. Buka **Gerakan**.
2. Aktifkan kategori gerakan tersebut, misalnya **3 jari**, untuk meluaskannya.
3. Aktifkan gerakan yang diinginkan. Daftar perintah akan terbuka.
4. Pilih perintah. **Tidak ditetapkan** membuat gerakan tidak melakukan apa pun.

Daftar diawali **Telusuri** dan **Tidak ditetapkan**, diikuti kategori perintah. Setiap kategori diciutkan, kecuali kategori yang memuat perintah yang saat ini dijalankan gerakan tersebut. Untuk mencari gerakan berdasarkan perintah yang dijalankannya, gunakan **Telusuri** di bagian atas layar **Gerakan**.

Anda juga dapat memberi gerakan pada suatu perintah dari layar **Perintah**. Lihat [](#commands).

### Mereset gerakan {#gestures-reset}

**Gerakan** > **Reset setelan gerakan** mengembalikan setiap gerakan pada tata letak gerakan yang sedang digunakan ke perintah default-nya, setelah dialog **Reset gerakan?**. Pilih **Setel ulang** untuk mengonfirmasi. Gerakan di aplikasi, profil gerakan, dan setelan lain di layar tidak berubah.

### Gerakan di aplikasi {#gestures-in-apps}

Suatu gerakan atau pintasan keyboard dapat menjalankan perintah berbeda di satu aplikasi selama aplikasi tersebut berada di depan. Di panel notifikasi, setelan cepat, dan layar kunci, gerakan milik aplikasi System UI yang berlaku.

Untuk menambahkan gerakan bagi suatu aplikasi:

1. Buka **Gerakan** > **Gerakan di aplikasi**.
2. Pilih **Tambahkan aplikasi**, lalu pilih aplikasinya.
3. Di bawah **Gerakan**, pilih **Tambahkan gerakan**. Daftar menampilkan setiap gerakan beserta perintahnya.
4. Pilih gerakan, lalu kategori perintah, lalu perintahnya. **Tidak ditetapkan** membuat gerakan tidak melakukan apa pun di aplikasi tersebut.

Setiap gerakan pada layar aplikasi menampilkan perintahnya di aplikasi itu. Aktifkan untuk memilih **Pilih perintah** dan **Hapus**. Di bawah **Pintasan keyboard**, **Tambahkan pintasan keyboard** merekam tombol untuk aplikasi tersebut dengan cara yang sama. Lihat [](#keyboards). **Hapus aplikasi** menghapus semua gerakan dan pintasan keyboard untuk aplikasi tersebut.

Perintah **Gerakan untuk aplikasi ini** membuka layar untuk aplikasi yang sedang berada di depan, sehingga Anda dapat menambahkan gerakan sambil menggunakan aplikasi.

**Ekspor ke file** dan **Impor dari file** menyimpan gerakan aplikasi ke file dan membacanya kembali. Lihat [](#api-item-files). Profil gerakan diprioritaskan sebelum gerakan di aplikasi. Lihat [](#gestures-profiles).

### Profil gerakan {#gestures-profiles}

Profil gerakan adalah kumpulan gerakan yang menjalankan perintah lain selama profil tersebut digunakan. **Default** berarti tidak menggunakan profil.

Untuk menambahkan profil gerakan:

1. Buka **Gerakan** > **Profil gerakan**.
2. Pilih **Tambahkan profil gerakan**, lalu ketik nama, atau biarkan nama yang disarankan.
3. Buka profil tersebut, lalu ubah gerakan yang Anda inginkan seperti di layar **Gerakan**.

Setiap gerakan dalam profil awalnya bernilai **Default** dan menjalankan perintah yang sama seperti tanpa profil: perintahnya di layar **Gerakan**, di **Gerakan di aplikasi**, atau di skrip. Gerakan yang ditetapkan ke suatu perintah akan menjalankan perintah itu, mendahului gerakan di aplikasi dan skrip. Daftar perintah menampilkan **Default** di urutan pertama.

Layar profil juga memiliki **Ganti nama**, **Pindahkan ke atas**, **Pindahkan ke bawah**, **Ekspor ke file**, dan **Hapus profil gerakan**, serta pengalih **Kenali geseran sebelum jari diangkat** tersendiri yang mengikuti layar **Gerakan** hingga Anda mengubahnya.

Untuk mengubah profil yang sedang digunakan:

- **Gerakan** > **Profil gerakan** > **Profil gerakan yang digunakan**.
- Kontrol pembacaan **Profil gerakan** dan item **Profil gerakan** di menu SandFly, yang ditampilkan setelah ada profil.
- Perintah **Pilih profil gerakan**, **Profil gerakan berikutnya**, dan **Profil gerakan sebelumnya**, serta **Gunakan profil gerakan** untuk setiap profil dan **Default**. Anda dapat menetapkannya ke gerakan, termasuk gerakan di dalam profil, dan ke pintasan keyboard.

SandFly mengucapkan nama profil saat berganti. Berikutnya dan sebelumnya menelusuri **Default** lalu profil-profil sesuai urutannya di layar **Profil gerakan**. Profil tidak mengubah gerakan sidik jari atau pintasan keyboard. Profil setelan, yang mengubah setelan lainnya, dijelaskan di [](#profiles).

### Pintasan aplikasi {#gestures-app-shortcuts}

Kategori perintah **Pintasan aplikasi** mencantumkan pintasan yang muncul saat Anda menyentuh lama ikon aplikasi di layar utama, seperti pesan baru, serta pintasan yang Anda tambahkan di layar **Pintasan aplikasi**. Pintasan yang ditambahkan aplikasi saat berjalan, seperti percakapan terbaru, dan pintasan yang hanya boleh dibuka oleh aplikasi layar utama baru dicantumkan mulai Android 12, dengan Shizuku yang dijalankan lewat adb atau debugging nirkabel, atau dengan root. Daftar perintah membacanya setiap kali dibuka. Anda dapat menetapkannya ke gerakan, menu tepi, dan pintasan keyboard.

Untuk menambahkan pintasan yang dibuat aplikasi bagi layar utama, seperti kontak atau makro dari aplikasi lain:

1. Buka **Gerakan** > **Pintasan aplikasi**.
2. Pilih **Tambahkan pintasan aplikasi**, lalu pilih jenis pintasannya.
3. Selesaikan layar yang dibuka oleh aplikasi tersebut.

Pintasan ditambahkan ke daftar, tempat Anda dapat mengganti nama atau menghapusnya. Lihat [](#commands) untuk perintah yang membuka aplikasi dan pintasan.

### Berlatih gerakan {#gestures-practice}

**Gerakan** > **Berlatih gerakan** membuka halaman tempat Anda dapat mencoba gerakan apa pun. SandFly menyebutkan fungsi gerakan tersebut tanpa menjalankannya. Perintah **Berlatih gerakan** membuka halaman yang sama.

### Uji ketepatan gerakan {#gestures-trainer}

**Bantuan & masukan** > **Uji ketepatan gerakan** mengukur gerakan Anda berdasarkan batas yang dipakai SandFly dengan setelan Anda. Fitur ini memerlukan Android 13 atau yang lebih baru, dan tidak tersedia di jam tangan atau TV.

1. Pilih grup, misalnya **Satu jari**, **Menu tepi**, atau **Ketuk belakang**.
2. Baca penjelasan gerakannya, lalu pilih **Mulai**.
3. Lakukan gerakan itu 3 kali. Hasil setiap percobaan diucapkan.

Selama percobaan berlangsung, setiap sentuhan dihitung sebagai percobaan, dan gerakan tidak menjalankan perintah apa pun. Setelah percobaan terakhir, layar mencantumkan ukuran setiap percobaan, apa yang perlu diubah, dan setelan yang mengubah batasnya, masing-masing dengan tombol. **Ulangi** mengulang percobaan, **Latihan berikutnya** berpindah ke gerakan berikutnya, dan **Daftar isi** mencantumkan latihan dalam grup tersebut.

Grup tidak menyertakan gerakan yang dinonaktifkan oleh setelan Anda, dan menyebutkan setelan mana yang menonaktifkannya. Tepi yang tidak memiliki menu tepi dilatih dengan item contoh, yang tidak menjalankan perintah apa pun.
