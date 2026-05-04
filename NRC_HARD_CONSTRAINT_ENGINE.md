# NRC Hard Constraint Engine - Implementasyon Özeti

## Gözat Tarihi: 04 Nisan 2026

### 🎯 Amaç
NutriPulse rasyon optimizasyon motoruna **NRC standartlarına uygun HARD CONSTRAINT** sistemi entegre etmek. Tüm besin, fiziksel ve rumen sağlığı kısıtlamaları artık **zorunlu** (fail-on-violation) olarak uygulanmaktadır.

---

## 📋 Uygulanıp Zorunlu Kılınan Kısıtlamalar

### A) BESIN KISITLARı (Nutrient Constraints)

✅ **Enerji (NEL/ME)**
- NEL: Hedef ±6% band (hard minimum/maximum)
- ME: Hedef ±7% band (hard minimum/maximum)
- Violation: Rasyon CONSTRAINT_FAILED olarak işaretlenir

✅ **Ham Protein (CP)**
- CP Min: Hedef × 0.95
- CP Max: Hedef × 1.15
- Hard enforcement via `ConstraintEngine.checkViolations()`

✅ **RDP/RUP (Rumen Degradable/Undegradable Protein)**
- RDP Min: Hedef × 0.85
- RDP Max: Hedef × 1.25
- RUP Min: Hedef × 0.80

✅ **Fiber (NDF/ADF)**
- NDF Min: NRC standart (örn. SIGIR: 28-33%)
- NDF Max: NRC standart (örn. SIGIR: 45%)
- ADF Max: NDF Max - 4% (kalite kontrolü)

✅ **Mineraller (Ca, P, Mg, Na, K)**
- Kalsiyum (Ca): Hedef × (0.92 ~ 1.45)
- Fosfor (P): Hedef × (0.90 ~ 1.45)
- Magnezyum (Mg): Hedef × (0.90 ~ 1.50)
- Sodyum, Potasyum: Hedef ± 50%

✅ **Mineral Oranı (Ca:P)**
- Min Ratio: Tür bazlı (SIGIR: 1.8, KOY: 1.4, etc.)
- Max Ratio: Tür bazlı (SIGIR: 2.5, KOY: 2.6, etc.)
- Hard enforcement

✅ **Amino Asitler (Lizin, Metiyonin, Threonin)**
- NRC requirement tabanlı
- Warning seviyesi (şimdilik hard constraint değil, gelecek sürümde)

---

### B) FİZİKSEL KISITLAMALAR (Physical Constraints)

✅ **Kaba Yem Oranı (%)**
- Minimum: NRC standart (örn. SIGIR: 40%, AT: 55%)
- Maximum: Profil bazlı (örn. SIGIR wet roughage: 45%)
- Hard enforcement

✅ **DM (Kuru Madde) Tutarı**
- Min: NRC req × 0.93
- Max: NRC req × (1.0 + balance mode tol%)
- Hard enforcement

✅ **Konsantre Maksimum (%)**
- Hesaplama: 100 - kaba_min_pct
- Uygulanıyor (kategori cap'leri + global feed share)

✅ **Yem Bazında Üst Sınır**
- Kategori max'ları (GRAIN, PROTEIN, etc.)
- Feed-specific maxDailyKg ve maxDmPct
- Hard enforcement

---

### C) RUMEN SAĞLIĞI KISITLARı (Rumen Health Constraints)

✅ **NDF (Nötral Deterjan Lif)**
- Minimum: NRC standart (zorunlu, rumen mekanik uyarımı)
- Maximum: NRC standart (rumen fermentasyonu)

✅ **NFC (Non-Fiber Carbs)**
- Maximum: Standart (SIGIR: 40%, KANATLI: formüle göre)
- Hard enforcement

✅ **Nişasta (Starch)**
- Maximum: Standart (SIGIR: 28%, KOY: 32%, etc.)
- Hard enforcement

✅ **Şeker (Sugar)**
- Maximum: Standart (SIGIR: 8%, KOY: 10%, etc.)
- Hard enforcement

✅ **Yağ (Fat)**
- Maximum: Standart (SIGIR: 6.5%, AT: 8%, etc.)
- Hard enforcement

✅ **DCAD (Dietary Cation-Anion Difference)**
- Proxy: Na × 434.98 + K × 255.74 - S × 624.15
- Minimum: Standart (rumen pH riski, NEG süt inekleri için)
- Hard enforcement

✅ **peNDF (Physically Effective NDF)**
- Minimum: Standart (rumen mukoza uyarımı)
- Hesaplama: NDF × kategori faktörü (roughage: 1.0, grain: 0.25, etc.)
- Hard enforcement

✅ **ADF (Acid Deterjan Lif)**
- Maximum: Kalite kontrolü (NDF Max - 4%)
- Hard enforcement

---

## 🏗️ Implementasyon Mimarisi

### Yeni Dosya: `ConstraintEngine.kt`

**Sınıflar:**
1. `NutrientBounds` - Tüm hard constraint'lerin lower/upper bound'larını tutar
2. `ConstraintEngine` (object) - Two static methods:
   - `generateBoundsForAnimal()` - NRC + profile bazlı bound'lar oluştur
   - `checkViolations()` - Rasyon vs bound'ları karşılaştır, ihlal listesi dön

**Mantık:**
- NRC requirement'ler hayvan profilinden okunur
- SpeciesOptimizationProfile'dan tür bazlı standardlar eklenir
- BalanceMode (LOW_COST, BALANCED, STRICT) toleransları modüle eder
- Tüm besinler 15+ farklı constraint'e karşı kontrol edilir

### Güncellenen Dosyalar:

1. **RationOptimizer.kt**
   - `optimize()` fonksiyonunun sonuna hard constraint check eklendi
   - Status: "CONSTRAINT_FAILED" eğer ihlal varsa
   - `evaluateManual()` da aynı şekilde kontrol
   - Import: `ConstraintEngine`

2. **BrillQualityGate.kt**
   - `assess()` - Hard constraint status kontrol edilir
   - `computeScore()` - isHardConstraintFailed parametresi (+50 puan penalti)

3. **RationResultFragment.kt**
   - CONSTRAINT_FAILED, MANUAL_CONSTRAINT_FAILED statüsleri eklendi
   - Renk: KIRMIZI (#FF0000) hard constraint failure için

4. **RationFragment.kt**
   - `runOptimization()` loop'ta CONSTRAINT_FAILED check eklendi
   - Hard constraint fail = fallback da kabul edilmez

---

## 📊 Sonuç Analizi

### Status Kodları:

```
✅ OPTIMAL
   - Brill quality gates passed
   - Tüm NRC hard constraints geçti
   - Uyarı yok
   
⚠️ APPROXIMATE
   - Brill quality gates passed
   - Tüm hard constraints geçti
   - Minör uyarılar (soft constraint) var
   
❌ CONSTRAINT_FAILED
   - **Yeni** - Bir veya daha fazla NRC hard constraint ihlal edildi
   - Sonuç gösterilmez, kullanıcıya "yem havuzunu genişletin" mesajı
   
❌ INFEASIBLE
   - Çözüm hiç bulunamadı
   
✅ MANUAL_OK
   - Manuel rasyon hard constraints geçti
   
⚠️ MANUAL_GAP
   - Manuel rasyon hard constraints geçti ama <95% coverage
   
❌ MANUAL_CONSTRAINT_FAILED
   - **Yeni** - Manuel rasyon hard constraint ihlal
```

---

## 🔬 Test Senaryosu

### Test 1: Protein/Mineral Aşırılığı Testi
**Amaç:** CP > cpMax, Ca > caMax, P > pMax durumunda CONSTRAINT_FAILED döner

**Adımlar:**
1. Bir hayvan profili seç (SUT_INEGI_ORTA)
2. Aday yemde protein/mineral yoğun yemleri ağırlıklandır
3. Optimize çalıştır
4. **Beklenen:** CONSTRAINT_FAILED + "CP fazla", "Ca fazla" gibi ihlal listesi

### Test 2: Rumen Sağlığı Testi
**Amaç:** NDF < min veya NFC > max durumunda CONSTRAINT_FAILED döner

**Adımlar:**
1. Hayvan: AT (horse) - yüksek NDF isterse
2. Aday yem: Çoğunlukla tahıl (NDF düşük, NFC yüksek)
3. Optimize çalıştır
4. **Beklenen:** CONSTRAINT_FAILED + "NDF yetersiz", "NFC fazla"

### Test 3: Ca:P Oranı Testi
**Amaç:** Ca:P oranı bound'lar dışında ise CONSTRAINT_FAILED döner

**Adımlar:**
1. Manuel rasyon: Fosfor ağır bir yem seç (örn. monokalsiyum fosfat)
2. Ca:P ratio'yu 1.0:1 'e göre ayarla
3. Hesapla
4. **Beklenen:** MANUAL_CONSTRAINT_FAILED + "Ca:P oranı düşük"

### Test 4: Hard Constraint + Brill Gate Kombinasyon
**Amaç:** Hard constraint'ler geçse ama Brill kalitesi kötü olmalı

**Adımlar:**
1. Çok az yem sayısı (< 8) ama hard constraint geçen rasyon
2. Optimize çalıştır
3. **Beklenen:** APPROXIMATE + Brill uyarıları ama CONSTRAINT_FAILED değil

---

## 🚀 Derleme ve Çalıştırma

```bash
cd /storage/internal_new/project/NutriPulse

# Derleme kontrol
./gradlew :app:compileDebugKotlin

# Unit test (varsa)
./gradlew :app:testDebugUnitTest

# Full build
./gradlew :app:assembleDebug
```

---

## 🔄 Gelecek Adımlar (Roadmap)

1. **Amino Asit Hard Constraint** - Şimdi warning, hard'a çevir
2. **DCAD / pH Risk** - Daha hassas sınırlar (pre-partum vs laktasyon)
3. **Fiber Kalitesi** - ADF:NDF ratio constraint'i
4. **Mikotoksin Sınırları** - Yem bazında kontaminasyon
5. **Operator Panel** - Constraint'leri kullanıcı UI'dan override etme seçeneği

---

## 📝 Notlar

- Tüm constraint'ler **NRC 2001 (Dairy), 2016 (Beef), 2007 (Small Ruminant/Horse)** tabanlı
- Balance mode (LOW_COST vs STRICT) toleransları %5-12 aralığında
- Hard constraint fail ise fallback kabul edilmez (user experience iyileştirme)
- Manuel rasyon da aynı constraint'lere tabi (ek kalite güvence)

---

**Son Güncelleme:** 04 Nisan 2026  
**Versiyon:** v1.1.0 (Hard Constraint Engine)  
**Durum:** Üretim Hazır ✅
