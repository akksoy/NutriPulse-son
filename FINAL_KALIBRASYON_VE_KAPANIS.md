# Final Kalibrasyon ve Zorunlu Kapanis

Bu belge, NutriPulse rasyon motoru icin final kalibrasyon paketini ve canliya gecis kapanis kriterlerini tanimlar.

## 1) Final Kalibrasyon Paketi (Kilitleme)

- Paket surumu: `v1.0`
- Paket tarihi: `2026-04-04`
- Kaynak dosya: `app/src/main/java/com/nutripulse/app/data/solver/RationStandards.kt`

### Kilitli kapsam

- Tur bazli kisit profilleri:
  - SIGIR/MANDA
  - KOYUN/KECI
  - AT
  - TAVSAN
- Mode bazli tuning:
  - `LOW_COST`
  - `BALANCED`
  - `STRICT`
- Kullanilan ana kisitlar:
  - NDF min/max
  - Kaba yem min
  - CP max
  - Nisasta, seker, yag max
  - NFC max
  - DCAD proxy min
  - peNDF proxy min

## 2) Zorunlu Kapanis Kontrolleri

Aşağıdaki kontroller gecmeden paket canliya alinmamalidir.

- Derleme:
  - `:app:compileDebugKotlin` basarili
- Testler:
  - `RationStandardsTest`
  - `BrillSelectionComplianceTest`
  - `RationRegressionSmokeTest`
  - `BrillQualityGateTest`
  - `BrillCalibrationRegressionTest`
- Uygulama ici dogrulama:
  - Rasyon ekraninda mode degisince preset degerleri aninda guncelleniyor
  - Preset ozeti ve kalibrasyon surumu gorunuyor

## 3) Gercekten Tamam (Acceptance)

Asagidaki 4 kosul birlikte saglanirsa "gercekten tamam" kabul edilir:

1. Tanimli testlerin tamami yesil.
2. Brill referans vakalarinda skor/coverage kabul bandinda.
3. Saha UAT (en az 5 vaka) kritik sapma olmadan tamam.
4. `RationStandards.kt` degerleri freeze edilmis ve surum/tarih notu guncel.

## 4) Sonraki Degisiklik Kurali

Final sonrasinda yalnizca su tip degisikliklere izin verilir:

- Kritik bugfix
- Veri giris duzeltmesi
- Dokumantasyon

Kisit degerlerinde degisiklik yapilacaksa yeni paket surumu acilmalidir (or: `v1.1`).
