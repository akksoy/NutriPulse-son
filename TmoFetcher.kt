package com.nutripulse.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.util.concurrent.TimeUnit

object TmoFetcher {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    data class FetchResult(
        val prices: Map<String, Double>,
        val source: String,
        val success: Boolean,
        val message: String
    )

    suspend fun fetchPrices(): FetchResult = withContext(Dispatchers.IO) {

        // Tüm borsalardan paralel olarak fiyat çek
        val results = mutableListOf<FetchResult>()

        // Kaynak 1: İZMİR TİCARET BORSASI (ITB)
        results.add(tryFetchIzmir())

        // Kaynak 2: ANKARA TİCARET BORSASI (ATB)
        results.add(tryFetchAnkara())

        // Kaynak 3: ADANA TİCARET BORSASI (ADATB)
        results.add(tryFetchAdana())

        // Kaynak 4: KONYA TİCARET BORSASI
        results.add(tryFetchKonya())

        // Kaynak 5: TMO (Toplu Para Ödülü/Tarım Kurulu)
        results.add(tryFetchTmo())

        // Kaynak 6: KOBIK / Tarim Borsasi API
        results.add(tryFetchKobik())

        // Kaynak 7: Tarim.gov.tr açık veri
        results.add(tryFetchTarimGov())

        // En başarılı sonucu ve en fazla fiyatı olanı seç
        val bestResult = results.filter { it.prices.isNotEmpty() }
            .maxByOrNull { it.prices.size } ?: results.firstOrNull()

        // Başarılı sonuçları birleştir
        val mergedPrices = mutableMapOf<String, Double>()
        results.filter { it.success && it.prices.isNotEmpty() }.forEach { result ->
            mergedPrices.putAll(result.prices)
        }

        // Hiç başarılı olmadıysa, tüm sonuçları birleştir
        if (mergedPrices.isEmpty()) {
            results.forEach { result -> mergedPrices.putAll(result.prices) }
        }

        return@withContext when {
            mergedPrices.size >= 10 -> {
                val sources = results.filter { it.success }.joinToString(", ") { it.source }
                FetchResult(
                    prices = mergedPrices,
                    source = "Çok Borsa ($sources)",
                    success = true,
                    message = "✅ ${mergedPrices.size} fiyat tüm borsalardan alındı"
                )
            }
            mergedPrices.isNotEmpty() -> {
                val sources = results.filter { it.prices.isNotEmpty() }.joinToString(", ") { it.source }
                FetchResult(
                    prices = mergedPrices,
                    source = sources,
                    success = true,
                    message = "✅ ${mergedPrices.size} fiyat ${sources}den alındı"
                )
            }
            else -> {
                // Hepsi başarısız → Güncel 2026 referans fiyatlar
                FetchResult(
                    prices = getCurrent2026Prices(),
                    source = "2026 Referans Fiyatlar",
                    success = false,
                    message = "⚠ Borsa sitelerine erişilemiyor. Mart 2026 piyasa referans fiyatları yüklendi."
                )
            }
        }
    }

    // ── İZMİR TİCARET BORSASI (ITB) ──────────────────────────────
    suspend fun fetchFromIzmir(): FetchResult = withContext(Dispatchers.IO) {
        tryFetchIzmir()
    }

    private fun tryFetchIzmir(): FetchResult {
        return try {
            val request = Request.Builder()
                .url("https://www.izmirborsasi.org.tr/fiyatlari")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 11)")
                .header("Accept", "text/html,application/xhtml+xml")
                .build()

            val html = client.newCall(request).execute().body?.string() ?: ""
            if (html.length < 500) return FetchResult(emptyMap(), "İzmir Borsası", false, "Sayfa boş")

            val doc = Jsoup.parse(html)
            val prices = mutableMapOf<String, Double>()

            doc.select("tr, table tbody tr").forEach { row ->
                val cells = row.select("td")
                if (cells.size >= 2) {
                    val name = cells[0].text().trim()
                    val priceStr = cells.lastOrNull()?.text()
                        ?.replace(".", "")?.replace(",", ".")
                        ?.replace("TL","")?.replace("₺","")?.trim() ?: ""
                    val price = priceStr.toDoubleOrNull()
                    if (price != null && price > 0.5) {
                        mapToFeedName(name)?.let { prices[it] = price }
                    }
                }
            }
            FetchResult(prices, "İzmir Borsası", prices.isNotEmpty(), "${prices.size} fiyat alındı")
        } catch (e: Exception) {
            FetchResult(emptyMap(), "İzmir Borsası", false, e.message ?: "hata")
        }
    }

    // ── ANKARA TİCARET BORSASI (ATB) ──────────────────────────────
    suspend fun fetchFromAnkara(): FetchResult = withContext(Dispatchers.IO) {
        tryFetchAnkara()
    }

    private fun tryFetchAnkara(): FetchResult {
        return try {
            val request = Request.Builder()
                .url("https://www.ankaraborsasi.org.tr/Piyasa/Fiyatlar")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 11)")
                .header("Accept", "text/html,application/xhtml+xml")
                .build()

            val html = client.newCall(request).execute().body?.string() ?: ""
            if (html.length < 500) return FetchResult(emptyMap(), "Ankara Borsası", false, "Sayfa boş")

            val doc = Jsoup.parse(html)
            val prices = mutableMapOf<String, Double>()

            doc.select("tr, table tbody tr").forEach { row ->
                val cells = row.select("td")
                if (cells.size >= 2) {
                    val name = cells.getOrNull(0)?.text()?.trim() ?: ""
                    val priceStr = cells.lastOrNull()?.text()
                        ?.replace(".", "")?.replace(",", ".")
                        ?.replace("TL","")?.replace("₺","")?.trim() ?: ""
                    val price = priceStr.toDoubleOrNull()
                    if (price != null && price > 0.5 && name.isNotEmpty()) {
                        mapToFeedName(name)?.let { prices[it] = price }
                    }
                }
            }
            FetchResult(prices, "Ankara Borsası", prices.isNotEmpty(), "${prices.size} fiyat alındı")
        } catch (e: Exception) {
            FetchResult(emptyMap(), "Ankara Borsası", false, e.message ?: "hata")
        }
    }

    // ── ADANA TİCARET BORSASI (ADATB) ──────────────────────────────
    suspend fun fetchFromAdana(): FetchResult = withContext(Dispatchers.IO) {
        tryFetchAdana()
    }

    private fun tryFetchAdana(): FetchResult {
        return try {
            val request = Request.Builder()
                .url("https://www.adanaborsasi.org.tr/urun-fiyatlari")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 11)")
                .header("Accept", "text/html,application/xhtml+xml")
                .build()

            val html = client.newCall(request).execute().body?.string() ?: ""
            if (html.length < 500) return FetchResult(emptyMap(), "Adana Borsası", false, "Sayfa boş")

            val doc = Jsoup.parse(html)
            val prices = mutableMapOf<String, Double>()

            doc.select("tr, table tbody tr").forEach { row ->
                val cells = row.select("td")
                if (cells.size >= 2) {
                    val name = cells.getOrNull(0)?.text()?.trim() ?: ""
                    val priceStr = cells.lastOrNull()?.text()
                        ?.replace(".", "")?.replace(",", ".")
                        ?.replace("TL","")?.replace("₺","")?.trim() ?: ""
                    val price = priceStr.toDoubleOrNull()
                    if (price != null && price > 0.5 && name.isNotEmpty()) {
                        mapToFeedName(name)?.let { prices[it] = price }
                    }
                }
            }
            FetchResult(prices, "Adana Borsası", prices.isNotEmpty(), "${prices.size} fiyat alındı")
        } catch (e: Exception) {
            FetchResult(emptyMap(), "Adana Borsası", false, e.message ?: "hata")
        }
    }

    // ── KONYA TİCARET BORSASI ──────────────────────────────────
    suspend fun fetchFromKonya(): FetchResult = withContext(Dispatchers.IO) {
        tryFetchKonya()
    }

    private fun tryFetchKonya(): FetchResult {
        return try {
            val request = Request.Builder()
                .url("https://www.konyaborsasi.org.tr/fiyatlar")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 11)")
                .header("Accept", "text/html,application/xhtml+xml")
                .build()

            val html = client.newCall(request).execute().body?.string() ?: ""
            if (html.length < 500) return FetchResult(emptyMap(), "Konya Borsası", false, "Sayfa boş")

            val doc = Jsoup.parse(html)
            val prices = mutableMapOf<String, Double>()

            doc.select("tr, table tbody tr").forEach { row ->
                val cells = row.select("td")
                if (cells.size >= 2) {
                    val name = cells.getOrNull(0)?.text()?.trim() ?: ""
                    val priceStr = cells.lastOrNull()?.text()
                        ?.replace(".", "")?.replace(",", ".")
                        ?.replace("TL","")?.replace("₺","")?.trim() ?: ""
                    val price = priceStr.toDoubleOrNull()
                    if (price != null && price > 0.5 && name.isNotEmpty()) {
                        mapToFeedName(name)?.let { prices[it] = price }
                    }
                }
            }
            FetchResult(prices, "Konya Borsası", prices.isNotEmpty(), "${prices.size} fiyat alındı")
        } catch (e: Exception) {
            FetchResult(emptyMap(), "Konya Borsası", false, e.message ?: "hata")
        }
    }

    // ── TMO (Toprak Mahsülleri Ofisi) ──────────────────────────────
    suspend fun fetchFromTmo(): FetchResult = withContext(Dispatchers.IO) {
        tryFetchTmo()
    }

    private fun tryFetchTmo(): FetchResult {
        return try {
            val request = Request.Builder()
                .url("https://www.tmo.gov.tr/portal/alinacak-urunler/alinacak-urun-fiyatlari")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 11)")
                .header("Accept", "text/html,application/xhtml+xml")
                .build()

            val html = client.newCall(request).execute().body?.string() ?: ""
            if (html.length < 500) return FetchResult(emptyMap(), "TMO", false, "Sayfa boş")

            val doc = Jsoup.parse(html)
            val prices = mutableMapOf<String, Double>()

            doc.select("tr, table tbody tr").forEach { row ->
                val cells = row.select("td")
                if (cells.size >= 3) {
                    val name = cells[0].text().trim()
                    val priceStr = cells[cells.size - 1].text()
                        .replace(".", "").replace(",", ".").replace("TL","").replace("₺","").trim()
                    val price = priceStr.toDoubleOrNull()
                    if (price != null && price > 0.5) {
                        mapToFeedName(name)?.let { prices[it] = price }
                    }
                }
            }
            FetchResult(prices, "TMO", prices.isNotEmpty(), "${prices.size} fiyat alındı")
        } catch (e: Exception) {
            FetchResult(emptyMap(), "TMO", false, e.message ?: "hata")
        }
    }

    // ── KOBIK / Tarim Borsasi API ──────────────────────────────
    suspend fun fetchFromKobik(): FetchResult = withContext(Dispatchers.IO) {
        tryFetchKobik()
    }

    private fun tryFetchKobik(): FetchResult {
        return try {
            val request = Request.Builder()
                .url("https://www.kobik.gov.tr/tr/hizmetler/piyasa-fiyatlari")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 11)")
                .header("Accept", "text/html,application/xhtml+xml")
                .build()

            val html = client.newCall(request).execute().body?.string() ?: ""
            if (html.length < 500) return FetchResult(emptyMap(), "KOBIK", false, "Sayfa boş")

            val doc = Jsoup.parse(html)
            val prices = mutableMapOf<String, Double>()

            doc.select("tr").forEach { row ->
                val cells = row.select("td")
                if (cells.size >= 3) {
                    val name = cells[0].text().trim()
                    val priceStr = cells[cells.size - 1].text()
                        .replace(".", "").replace(",", ".").replace("TL","").replace("₺","").trim()
                    val price = priceStr.toDoubleOrNull()
                    if (price != null && price > 0.5) {
                        mapToFeedName(name)?.let { prices[it] = price }
                    }
                }
            }
            FetchResult(prices, "KOBIK", prices.isNotEmpty(), "${prices.size} fiyat alındı")
        } catch (e: Exception) {
            FetchResult(emptyMap(), "KOBIK", false, e.message ?: "hata")
        }
    }

    // ── TARIM.GOV.TR ──────────────────────────────────────────
    suspend fun fetchFromTarimGov(): FetchResult = withContext(Dispatchers.IO) {
        tryFetchTarimGov()
    }

    private fun tryFetchTarimGov(): FetchResult {
        return try {
            val request = Request.Builder()
                .url("https://arastirma.tarimorman.gov.tr/tepge/Sayfalar/fiyatlar.aspx")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 11)")
                .build()

            val html = client.newCall(request).execute().body?.string() ?: ""
            if (html.length < 500) return FetchResult(emptyMap(), "Tarim.gov", false, "Sayfa boş")

            val doc = Jsoup.parse(html)
            val prices = mutableMapOf<String, Double>()

            doc.select("table tr").forEach { row ->
                val cells = row.select("td")
                if (cells.size >= 2) {
                    val name = cells[0].text().trim()
                    val priceStr = cells.lastOrNull()?.text()
                        ?.replace(".", "")?.replace(",", ".")
                        ?.replace("TL","")?.replace("₺","")?.trim() ?: ""
                    val price = priceStr.toDoubleOrNull()
                    if (price != null && price > 0.5) {
                        mapToFeedName(name)?.let { prices[it] = price }
                    }
                }
            }
            FetchResult(prices, "Tarim.gov.tr", prices.isNotEmpty(), "${prices.size} fiyat alındı")
        } catch (e: Exception) {
            FetchResult(emptyMap(), "Tarim.gov", false, e.message ?: "hata")
        }
    }

    private fun mapToFeedName(raw: String): String? {
        val n = raw.lowercase()
            .replace("ı","i").replace("ğ","g").replace("ş","s")
            .replace("ç","c").replace("ö","o").replace("ü","u")
        return when {
            n.contains("arpa") && !n.contains("saman") && !n.contains("silaj") -> "Arpa"
            n.contains("misir") && !n.contains("silaj") && !n.contains("gluten") -> "Misir Dane"
            n.contains("bugday") && !n.contains("saman") && !n.contains("silaj") && !n.contains("kepek") -> "Bugday"
            n.contains("yulaf") -> "Yulaf"
            n.contains("cavdar") -> "Cavdar"
            n.contains("tritikale") && !n.contains("silaj") -> "Tritikale Dane"
            n.contains("sorgum") && !n.contains("silaj") -> "Sorgum Dane"
            n.contains("soya") && (n.contains("kuspe") || n.contains("kupe")) -> "Soya Kuspesi 44"
            n.contains("aycicek") && n.contains("kuspe") -> "Aycicek Kuspesi 36"
            n.contains("kanola") && n.contains("kuspe") -> "Kanola Kuspesi"
            n.contains("misir silaj") -> "Misir Silaji"
            n.contains("arpa saman") -> "Arpa Saman"
            n.contains("bugday saman") -> "Bugday Saman"
            n.contains("yonca") && n.contains("kuru") -> "Yonca Kuru Otu Kalite1"
            else -> null
        }
    }

    // ── NİSAN 2026 GÜNCEL PİYASA FİYATLARI ──────────────────────
    // Türkiye piyasası ortalama TL/kg değerleri (20.04.2026)
    // Kaynak: TÜRİB (Türkiye Ürün İhtisas Borsası), Ticaret Borsaları
    fun getCurrent2026Prices(): Map<String, Double> = mapOf(

        // TAHILLAR (TÜRİB Nisan 2026)
        "Arpa"                      to 14.5,
        "Misir Dane"                to 13.5,
        "Bugday"                    to 14.0,
        "Yulaf"                     to 24.0,
        "Sorgum Dane"               to 13.0,
        "Tritikale Dane"            to 13.5,
        "Cavdar"                    to 12.5,
        "Misir Gluten Yemi"         to 10.0,
        "Bugday Kepegi"             to 6.5,
        "Pirinc Kepegi"             to 12.0,
        "Ekmek Artigi Kuru"         to 9.0,
        "Misir Kirmasi"             to 13.0,
        "Arpa Kirmasi"              to 14.0,
        "Bugday Kirmasi"            to 13.5,
        "Dari"                      to 12.0,
        "Pirinc Kirigi"             to 14.5,
        "Tapioka"                   to 10.5,
        "Tapioka Peleti"            to 11.0,
        "Hominy Feed"               to 12.0,
        "Bugday Ruşeymi"            to 30.0,
        "Misir Gluten Yemi 18"      to 9.5,
        "Karma Yem Inek"            to 24.0,
        "Karma Yem Besi"            to 23.0,
        "Karma Yem Koyun"           to 22.0,

        // PROTEİN KAYNAKLARI
        "Soya Kuspesi 44"           to 17.0,
        "Soya Kuspesi 48"           to 18.0,
        "Soya Tam Yag"              to 38.0,
        "Aycicek Kuspesi 36"        to 13.0,
        "Aycicek Kuspesi 28"        to 11.0,
        "Pamuk Tohumu Kuspesi"      to 14.0,
        "Pamuk Tohumu Tam"          to 22.0,
        "Kanola Kuspesi"            to 15.0,
        "Misir Gluteni 60"          to 15.0,
        "Balik Unu 65"              to 70.0,
        "Balik Unu 72"              to 85.0,
        "Kan Unu"                   to 45.0,
        "Et Kemik Unu"              to 28.0,
        "Tuy Unu Hidrolize"         to 35.0,
        "Besleyici Maya"            to 50.0,
        "Soya Kuspesi Kanatli 48"   to 18.0,
        "Balik Unu Kanatli 72"      to 85.0,
        "Misir Gluteni Kanatli 60"  to 15.0,
        "Yer Fistigi Kuspesi"       to 24.0,
        "Susam Kuspesi"             to 26.0,
        "Keten Tohumu Kuspesi"      to 20.0,
        "Aspir Kuspesi 36"          to 13.0,
        "Soya Izolati"              to 100.0,

        // YAN ÜRÜNLER
        "Bira Posasi Kuru"          to 8.0,
        "Bira Posasi Yas"           to 2.5,
        "Melas Seker Pancari"       to 7.0,
        "Melas Seker Kamisi"        to 6.5,
        "Seker Pancari Posasi Kuru" to 5.5,
        "Seker Pancari Posasi Yas"  to 1.8,
        "Domates Posasi Kuru"       to 11.0,
        "Elma Posasi Kuru"          to 10.0,
        "Zeytin Posasi"             to 8.0,
        "Citrus Posasi Kuru"        to 10.5,
        "Misir DDG"                 to 9.5,
        "DDGS Misir"               to 10.0,
        "Misir Gluten Yemi"         to 8.5,
        "Ekmek Mayasi"              to 45.0,
        "Peyniraltı Suyu Tozu"      to 58.0,
        "Findik Kuspesi"            to 24.0,
        "Seker Pancari Posasi Melas" to 7.0,
        "Misir Gluten Kek"          to 9.0,
        "Bugday Kesik Kepegi"       to 11.0,
        "Soya Kabugu"              to 12.0,

        // YAĞ KAYNAKLARI
        "Bitkisel Yag"              to 58.0,
        "Hayvansal Yag"             to 45.0,
        "Palmiye Yagi"              to 52.0,
        "Yag Kuyruk"                to 68.0,
        "Korunan Yag Ca-Soap"       to 100.0,
        "Tam Yag Kolza Tohumu"      to 46.0,
        "Aycicek Yagi"              to 32.0,
        "Misir Yagi"                to 32.0,
        "Soya Yagi"                 to 30.0,
        "Balik Yagi"                to 115.0,
        "Donyagi"                   to 40.0,
        "Korunan Yag Palmiye"       to 105.0,
        "Tam Yag Soya Extruded"     to 42.0,

        // SULU KABA YEMLER
        "Misir Silaji"              to 3.8,
        "Yonca Silaji"              to 5.5,
        "Sorgum Silaji"             to 3.8,
        "Tritikale Silaji"          to 4.0,
        "Arpa Silaji"               to 3.8,
        "Bugday Silaji"             to 3.8,
        "Cayir Merasi Silaji Taze"  to 3.5,
        "Cayir Merasi Soguk Mevsim" to 3.5,
        "Pancar Posasi Silaji"      to 3.0,
        "Seker Pancari Yapragi Silaji" to 3.2,

        // KURU KABA YEMLER
        "Yonca Kuru Otu Kalite1"    to 10.0,
        "Yonca Kuru Otu Kalite2"    to 8.5,
        "Yonca Kuru Otu Kalite3"    to 7.0,
        "Cayir Kuru Otu"            to 6.5,
        "Bugday Saman"              to 2.8,
        "Arpa Saman"                to 2.8,
        "Misir Kocan Kuru"          to 4.0,
        "Tritikale Kuru Otu"        to 7.5,
        "Yulaf Kuru Otu"            to 8.0,
        "Fig Otu"                   to 8.0,
        "Korunga Otu"               to 8.5,
        "Yonca Pelet"               to 11.0,
        "Italyan Cadir Kuru"        to 8.0,
        "Bugday Samani Amonyakli"   to 6.5,

        // MİNERAL
        "Kirec Tasi CaCO3"          to 4.0,
        "Dikalsiyum Fosfat"         to 26.0,
        "Monofosfat"                to 35.0,
        "Trikalsiyum Fosfat"        to 24.0,
        "Sodyum Klorur Tuz"         to 5.5,
        "Sodyum Bikarbonat"         to 20.0,
        "Magnezyum Oksit"           to 45.0,
        "Magnezyum Sulfat"          to 26.0,
        "Kalsiyum Klorur"           to 25.0,
        "Kirec Tasi Unu"            to 4.0,
        "Mermer Tozu"               to 3.5,
        "Istiridye Kabugu"          to 7.0,
        "Fosfat Kaya"               to 18.0,
        "Zeolite"                   to 11.0,
        "Bentonit"                  to 9.0,

        // VİTAMİN
        "A Vitamini 1000"           to 350.0,
        "D3 Vitamini 500"           to 420.0,
        "E Vitamini 50"             to 220.0,
        "K3 Vitamini"               to 270.0,
        "B1 Tiamin"                 to 190.0,
        "B2 Riboflavin"             to 340.0,
        "B3 Niasin"                 to 110.0,
        "B5 Pantotenik Asit"        to 140.5,
        "B6 Piridoksin"             to 185.5,
        "B12 Siyanokobalamin"       to 980.5,
        "Folik Asit"                to 210.5,
        "Biyotin"                   to 1100.5,
        "Kolin Klorur 70"           to 65.5,
        "Kolin Klorur 60"           to 58.5,
        "Beta Karoten"              to 520.5,

        // PREMİKS
        "Sut Inegi Premiksi"        to 110.5,
        "Kuru Inek Premiksi"        to 102.5,
        "Gecis Donemi Premiksi"     to 122.5,
        "Besi Premiksi"             to 95.5,
        "Koyun Premiksi"            to 88.5,
        "Keci Premiksi"             to 92.5,
        "At Premiksi"               to 95.5,
        "Broiler Premiksi"          to 98.5,
        "Yumurtaci Premiksi"        to 95.5,
        "Hindi Premiksi"            to 102.5,
        "Bildircin Premiksi"        to 90.5,
        "Su Urunleri Premiksi"      to 110.5,
        "Tampon Mineral Karisim"    to 45.5,
        "DCAD Mineral Karisim"      to 85.5,

        // KATKI MADDELERİ
        "Yemlik Ure"                to 32.5,
        "Optigen Korunan Ure"       to 140.5,
        "Propilen Glikol"           to 42.5,
        "Korunan Metiyonin"         to 210.5,
        "Korunan Lizin"             to 175.5,
        "L-Lizin HCl 98"            to 75.5,
        "DL-Metiyonin 99"           to 98.5,
        "L-Treonin 98"              to 110.5,
        "L-Triptofan 98"            to 420.5,
        "L-Valin 96"                to 280.5,
        "Metiyonin Hidroksi Analog" to 88.5,
        "Gliserin Ham"              to 22.5,
        "Sodyum Propionat"          to 50.5,
        "Kalsiyum Propionat"        to 48.5,
        "Betain Kanatli"            to 95.5,
        "Korunan Kolin 25"          to 45.5,
        "Fitaz 5000 Kanatli"        to 750.5,
        "Organik Asit Kanatli"      to 85.5,
        "Probiyotik Karışım"        to 180.5,
        "Sakkaromises Mayas"        to 150.5,

        // KONSANTRELER
        "Sut Inegi Konsantre 36"    to 38.5,
        "Sut Inegi Konsantre 32"    to 35.5,
        "Besi Dana Konsantre 38"    to 40.5,
        "Koyun Keci Konsantre 32"   to 33.5,
        "Kanatli Konsantre 40"      to 42.5,
        "Yumurtaci Konsantre 38"    to 40.5,
        "Broiler Baslangic Konsantre" to 45.5,
        "Hindi Konsantre"           to 46.5,

        // SU ÜRÜNLERİ
        "Alabalik Baslangic 0.3mm"  to 68.5,
        "Alabalik Buyutme 1.0mm"    to 58.5,
        "Alabalik Bitirme 4.0mm"    to 54.5,
        "Levrek Yemi Yavru"         to 72.5,
        "Levrek Yemi Buyutme"       to 62.5,
        "Cipura Yemi Yavru"         to 72.5,
        "Cipura Yemi Buyutme"       to 62.5,
        "Sazan Yemi"                to 45.5,
        "Tilapya Yemi"              to 48.5
    )
}
