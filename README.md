# 🤖 Friday AI - Ağıllı Səs və Sistem Köməkçisi (Android)

<p align="center">
  <img src="app/src/main/res/drawable/friday_ai_logo_1787859726049.png" width="120" height="120" alt="Friday AI Logo" style="border-radius: 50%;">
</p>

<p align="center">
  <b>Android üçün güclü, tam funksional və intuitiv şəxsi AI səs köməkçisi.</b><br>
  <i>"Hey Friday" oyanış əmri, Gemini AI inteqrasiyası, sistem idarəetməsi, səsli qeydlər, zəng və SMS modulları ilə.</i>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-green.svg" alt="Platform">
  <img src="https://img.shields.io/badge/Kotlin-Jetpack%20Compose-blue.svg" alt="Compose">
  <img src="https://img.shields.io/badge/AI-Google%20Gemini-orange.svg" alt="Gemini AI">
  <img src="https://img.shields.io/badge/Build-GitHub%20Actions-brightgreen.svg" alt="GitHub Actions">
</p>

---

## 📱 APK-nı Birbaşa Telefona Necə Yükləmək Olar?

Kompüterə və ya Android Studio-ya ehtiyac olmadan APK faylını birbaşa telefonunuza yükləyə bilərsiniz:

### Metod 1: GitHub Releases Bölməsindən (Ən Asan Yol)
1. Telefonunuzun brauzerində bu GitHub səhifəsinə daxil olun.
2. Sağ tərəfdəki (və ya aşağıdakı) **"Releases"** bölməsinə keçin.
3. Ən son versiyadakı **`Friday-AI-Assistant.apk`** faylını birbaşa telefonunuza endirin.
4. Endirilən fayla toxunub **"Quraşdır" (Install)** seçin.

### Metod 2: GitHub Actions Bölməsindən
1. Yuxarı menyudan **Actions** bölməsinə keçin.
2. Ən son uğurlu işə salınmış **"Build and Release Friday AI APK"** işinin üzərinə vurun.
3. Aşağıdakı **Artifacts** hissəsindən **`Friday-AI-Assistant-APK`** faylını yükləyin.

---

## 🌟 Əsas Funksiyalar və İmkalar

### 1. 🎙️ AI Səs Köməkçisi ("Hey Friday")
- **"Hey Friday" Oyanış Rejimi**: Səsli olaraq aktivləşir və sizi dinləməyə başlayır.
- **Canlı Holoqrafik Voice Orb**: Danışıq və cavab zamanı reaktiv dalğa animasiyası.
- **Gemini AI & TTS**: Sual və əmrlərə anında Azərbaycan dilində səsli və mətnli cavab.

### 2. 🎛️ Telefon Ayarları və Sistem İdarəetməsi
- **Wi-Fi & Bluetooth**: *"Wi-Fi aç / söndür"*, *"Bluetooth yandır / söndür"* səsli əmrləri və ya tək toxunuşla idarəetmə.
- **Fənər (Flashlight)**: *"Fənəri yandır"*, *"İşığı söndür"* əmrləri ilə flaşın yandırılıb-söndürülməsi.
- **Səs Səviyyələri**: Media, Zəng və Siqnal səslərini faizlə tənzimləmə (*"Səsi 80% et"*, *"Səsi bağla"*).
- **Ekran Parlaqlığı**: Parlaqlıq səviyyəsinin tənzimlənməsi (*"Parlaqlığı artır"*).
- **Rejimlər & Batareya**: Səssiz / Titrəmə rejimləri və batareya faizinin monitorinqi.

### 3. 📂 Səsli Qeydlər və Daxili Fayl Meneceri
- **Audio Qeydiyyat**: Canlı səs dalğası ilə qeydlərin aparılması, tətbiqdaxili pleyerdə dinlənməsi və silinməsi.
- **Mətn Faylları**: `.txt`, `.json`, `.md` formatında sənədlərin yaradılması və daxili redaktorla redaktə olunması.

### 4. 📞 Zəng və SMS İdarəsi
- **Səsli Zəng**: Səsli əmrlə nömrə yığımı (*"0501234567 nömrəsinə zəng et"*).
- **Səsli SMS**: Səsli əmrlə mesaj tərtibatı və göndərilməsi (*"SMS göndər: Salam, haradasan?"*).
- **Səsli Elan**: Gələn zəng və SMS-ləri Friday tərəfindən səsləndirilməsi və səsli əmrlə idarəsi.

---

## 🛠️ Kompüterdə Yığmaq (Android Studio ilə)

Əgər layihəni öz kompüterinizdə inkişaf etdirmək və ya yığmaq istəyirsinizsə:

1. Layihəni klonlayın:
   ```bash
   git clone https://github.com/YOUR_USERNAME/YOUR_REPO.git
   ```
2. **Android Studio** (Ladybug və ya daha yeni) proqramında açın.
3. Gradle sinxronizasiyasını gözləyin.
4. Terminaldan və ya Android Studio-dan yığın:
   ```bash
   gradle assembleDebug
   ```
5. Yaranan APK faylı burada olacaq: `app/build/outputs/apk/debug/app-debug.apk`.

---

## 🔒 Tələb Olunan İcazələr (Permissions)

Tətbiq tam funksional işləməsi üçün aşağıdakı sistem icazələrini istifadə edir:
- `RECORD_AUDIO` - Səsli əmrlər və səs qeydləri üçün.
- `CALL_PHONE` - Səsli zəng funksiyası üçün.
- `SEND_SMS` & `RECEIVE_SMS` - Səsli SMS idarəetməsi üçün.
- `READ_PHONE_STATE` - Gələn zənglərin tanınması və səsləndirilməsi üçün.
- `POST_NOTIFICATIONS` - Sistem bildirişləri üçün.

---

## 📄 Lisenziya
Bu layihə açıq mənbəli olaraq təqdim olunur.
