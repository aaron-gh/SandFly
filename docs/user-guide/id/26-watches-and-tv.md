## Jam tangan dan TV {#devices}

SandFly berjalan di jam tangan Wear OS serta di Android TV dan Google TV. Bab ini menjelaskan perbedaan SandFly pada perangkat-perangkat tersebut dibandingkan di ponsel. Semua hal lain di panduan ini juga berlaku di sana, kecuali fitur yang memerlukan layar sentuh, ponsel, atau perangkat keras yang tidak dimiliki perangkat tersebut.

### Jam tangan Wear OS {#devices-watch}

Jam tangan memiliki build SandFly tersendiri, yaitu `sandfly-wear.apk`. Build ini memiliki setelan yang sama seperti di ponsel, kecuali setelan untuk fitur yang tidak didukung jam tangan.

#### Menginstal SandFly di jam tangan {#devices-watch-install}

Anda menginstal `sandfly-wear.apk` dari komputer menggunakan adb.

1. Unduh `sandfly-wear.apk` dari [rilis terbaru](https://github.com/aaron-gh/SandFly/releases/latest) ke komputer.
2. Di jam tangan, aktifkan opsi developer, lalu aktifkan **ADB debugging** dan debugging nirkabel. Langkahnya bergantung pada jam tangan. Pada sebagian besar jam tangan, ketuk nomor build di setelan **Tentang** tujuh kali untuk memunculkan opsi developer.
3. Di komputer, hubungkan ke jam tangan dengan `adb connect` diikuti alamat yang ditampilkan setelan debugging nirkabel jam tangan. Beberapa jam tangan perlu dipasangkan terlebih dahulu dengan `adb pair`.
4. Jalankan:

   ```
   adb install sandfly-wear.apk
   ```

5. Di jam tangan, aktifkan SandFly di setelan aksesibilitas.

Untuk membuka setelan SandFly di jam tangan, buka **SandFly** di daftar aplikasi jam tangan. Saat pertama kali SandFly diaktifkan, tutorial jam tangan akan terbuka.

#### Gerakan di jam tangan {#devices-watch-gestures}

Layar jam tangan hanya melacak maksimal dua jari, sehingga gerakan tiga atau empat jari tidak tersedia. Geseran dua jari diteruskan ke aplikasi untuk menggulir.

Jam tangan menggunakan gerakan default ponsel, kecuali:

| Gerakan | Perintah di jam tangan |
| --- | --- |
| Ketuk dua kali lalu tahan dengan 2 jari | Buka menu SandFly |
| Ketuk tiga kali lalu tahan dengan 2 jari | Aktifkan atau nonaktifkan ucapan |
| Geser kiri lalu atas | Ulangi frasa terakhir yang diucapkan |
| Geser kanan lalu atas | Naikkan volume |
| Geser kanan lalu bawah | Turunkan volume |
| Geser kiri lalu bawah | Tidak ditetapkan |

Untuk gerakan default ponsel, lihat [](#appendix-gestures). Untuk mengubah gerakan, buka **Gerakan**. Lihat [](#gestures).

#### Setelan dan fitur di jam tangan {#devices-watch-settings}

Di jam tangan:

- Kontrol pembacaan menyertakan **Gulir** dan **Volume aksesibilitas** secara default. Lihat [](#reading).
- Menu SandFly dapat digulir agar setiap item dapat dijangkau. **Deskripsikan layar** tidak ada di menu secara default. Item untuk menonaktifkan SandFly bernama **Nonaktifkan**. Lihat [](#commands).
- **Fitur-fitur baru di SandFly** bernama **Yang baru**.
- Fitur menjeda SandFly tidak tersedia, dan **Lanjutkan SandFly** tidak ditampilkan. Lihat [](#pause).
- **Berlatih gerakan** tidak ada di **Bantuan & masukan**. Gunakan **Berlatih gerakan** di bagian atas **Gerakan**, yang membuka latihan gerakan dari tutorial jam tangan.
- **Uji ketepatan gerakan** dan **Latihan keyboard** tidak ada di **Bantuan & masukan**.
- **Riwayat versi** dan **Kebijakan privasi** menampilkan alamat halaman jika jam tangan tidak memiliki browser.

Fitur-fitur berikut tidak tersedia di jam tangan, dan setelannya tidak ditampilkan: skrip, makro, menu kustom, kontrol pembacaan kustom, perintah suara, sentuhan langsung, ketuk belakang, goyang, setelan gerakan tepi dan sudut, keyboard braille, keyboard tulisan tangan, keyboard SandFly, AI di perangkat, **Gunakan SandFly sebagai wallpaper**, **Speaker saat jauh dari telinga**, **Setel ulang audio aksesibilitas setelah panggilan**, **Pintasan tombol volume saat layar mati**, dan **Tampilkan petunjuk untuk menonaktifkan SandFly**.

Untuk notifikasi di jam tangan Samsung, lihat [](#notifications).

#### Pembaruan di jam tangan {#devices-watch-updates}

Jam tangan memperbarui ke build baru `sandfly-wear.apk`, seperti dijelaskan di [](#updates). Jam tangan mengunduh pembaruan melalui Wi-Fi-nya sendiri jika Wi-Fi tersedia, bukan melalui ponsel, dan menampilkan progresnya di notifikasi. Jika dialog penginstalan tidak terbuka, misalnya karena layar mati selama pengunduhan, ketuk notifikasi **Pembaruan SandFly telah diunduh**.

### Android TV {#devices-tv}

File `sandfly.apk` yang sama dengan versi ponsel dapat diinstal di Android TV dan Google TV.

#### Menginstal SandFly di TV {#devices-tv-install}

1. Di TV, aktifkan opsi developer dan debugging jaringan. Langkahnya bergantung pada TV.
2. Di komputer pada jaringan yang sama, hubungkan ke TV dengan `adb connect` diikuti alamat IP TV.
3. Unduh `sandfly.apk` dari [rilis terbaru](https://github.com/aaron-gh/SandFly/releases/latest) lalu jalankan:

   ```
   adb install sandfly.apk
   ```

4. Di TV, aktifkan SandFly di setelan aksesibilitas. Pada sebagian besar TV, lokasinya di **Setelan** > **Preferensi Perangkat** > **Aksesibilitas**.

Pada banyak TV, menekan dan menahan tombol Kembali dan Bawah di remote secara bersamaan selama 3 detik akan mengaktifkan atau menonaktifkan SandFly. Ini adalah pintasan aksesibilitas TV, yang juga dapat Anda atur di setelan TV.

Untuk membuka setelan SandFly, pilih **Setelan SandFly** di daftar aplikasi pada layar utama TV.

#### Menggunakan remote {#devices-tv-remote}

Di TV, remote digunakan untuk memindahkan fokus:

- Tombol panah memindahkan fokus ke item berikutnya di atas, bawah, kiri, atau kanan.
- Tombol Pilih mengaktifkan item yang difokuskan. Jika item yang difokuskan tidak dapat diaktifkan, tombol Pilih diteruskan ke aplikasi.
- Tekan dan tahan tombol Pilih untuk menyentuh lama item yang difokuskan.
- Pada penggeser, tombol Pilih memulai mode penggeser, dan SandFly mengucapkan "Mode penggeser dimulai". Tombol panah kemudian mengubah nilainya. Tekan Pilih lagi untuk mengakhiri mode penggeser.
- Jika **Setelan developer** > **Aktifkan debug pohon node** aktif, tombol pencarian menuliskan pohon item di layar ke log sistem. Lihat [](#help-developer).

SandFly tidak menangani tombol panah dan tombol Pilih saat keyboard di layar sedang ditampilkan. Untuk keyboard fisik, lihat [](#keyboards).

#### Setelan dan fitur di TV {#devices-tv-settings}

Di TV:

- Setelan untuk fitur yang memerlukan layar sentuh atau ponsel tidak ditampilkan. Contohnya **Gerakan**, **Uji ketepatan gerakan**, **Sentuhan langsung**, **Pembacaan status**, **Gunakan ringkasan SandFly**, **Gunakan daftar notifikasi SandFly**, **Speaker saat jauh dari telinga**, dan **Deskripsi gambar dan layar**. Layar setelan **Menu SandFly** dan **Kontrol pembacaan** juga tidak ditampilkan.
- Fitur menjeda SandFly tidak tersedia, dan **Lanjutkan SandFly** tidak ditampilkan. Lihat [](#pause).
- Item pertama di **Bantuan & masukan** membuka tutorial TV jika TV memilikinya. Jika tidak, item tersebut bernama **Bantuan**.
- TV sering kali tidak menampilkan notifikasi, sehingga notifikasi pembaruan mungkin tidak muncul. Untuk memperbarui, buka **Update dan penginstalan** > **Periksa pembaruan**. Lihat [](#updates-check).
