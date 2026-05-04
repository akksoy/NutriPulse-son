package com.nutripulse.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * PriceFetcherService - Tüm ticaret borsalarından fiyat çekme servisi
 * 
 * Desteklenen Borsalar:
 * 1. İzmir Ticaret Borsası (ITB)
 * 2. Ankara Ticaret Borsası (ATB)
 * 3. Adana Ticaret Borsası (ADATB)
 * 4. Konya Ticaret Borsası
 * 5. TMO (Toprak Mahsülleri Ofisi)
 * 6. KOBIK (Petrol Borsası)
 * 7. Tarım ve Orman Bakanlığı - TEPGE Veri Tabanı
 */
object PriceFetcherService {

    /**
     * Tüm borsalardan paralel olarak fiyat çek ve en iyi sonucu döndür
     * @return FetchResult - başarılı/başarısız sonuç
     */
    suspend fun fetchAllExchangePrices(): TmoFetcher.FetchResult = withContext(Dispatchers.IO) {
        TmoFetcher.fetchPrices()
    }

    /**
     * Belirli bir borsa tarafından fiyat çek
     * @param exchangeName - Borsa adı (izmir, ankara, adana, konya, tmo, kobik, tarim)
     * @return FetchResult
     */
    suspend fun fetchByExchange(exchangeName: String): TmoFetcher.FetchResult = withContext(Dispatchers.IO) {
        when (exchangeName.lowercase()) {
            "izmir" -> TmoFetcher.fetchFromIzmir()
            "ankara" -> TmoFetcher.fetchFromAnkara()
            "adana" -> TmoFetcher.fetchFromAdana()
            "konya" -> TmoFetcher.fetchFromKonya()
            "tmo" -> TmoFetcher.fetchFromTmo()
            "kobik" -> TmoFetcher.fetchFromKobik()
            "tarim" -> TmoFetcher.fetchFromTarimGov()
            else -> TmoFetcher.FetchResult(
                emptyMap(),
                "Bilinmiyor",
                false,
                "Geçersiz borsa adı: $exchangeName"
            )
        }
    }

    /**
     * Mevcut borsa listesini döndür
     */
    fun getAvailableExchanges(): List<String> = listOf(
        "İzmir Ticaret Borsası (ITB)",
        "Ankara Ticaret Borsası (ATB)",
        "Adana Ticaret Borsası (ADATB)",
        "Konya Ticaret Borsası",
        "TMO (Toprak Mahsülleri Ofisi)",
        "KOBIK (Piyasa Fiyatlandırması)",
        "Tarım Bakanlığı TEPGE"
    )

    /**
     * Fiyat güncelleme durumunu açıklayan bilgilendirme mesajı
     */
    fun getPriceUpdateInfo(): String {
        return """
            🌐 FİYAT GÜNCELLEME KAYNAKLARI
            
            ✓ İzmir Ticaret Borsası (ITB)
              Sebze, meyve ve tarım ürünleri fiyatları
            
            ✓ Ankara Ticaret Borsası (ATB)
              Tahıl ve yem ürünleri fiyatları
            
            ✓ Adana Ticaret Borsası (ADATB)
              Pamuk ve tarım yan ürünleri
            
            ✓ Konya Ticaret Borsası
              Bölge yem ve tahıl fiyatları
            
            ✓ TMO (Toprak Mahsülleri Ofisi)
              Resmi alım fiyatları
            
            ✓ KOBIK (Piyasa Fiyatları)
              Güncel piyasa fiyatlandırması
            
            ✓ Tarım ve Orman Bakanlığı
              TEPGE Araştırma Merkezi veri tabanı
            
            💡 Otomatik Mod: Tüm 7 kaynaktan paralel fiyat çeker
            📝 Manuel Mod: İstediğiniz fiyatları girebilirsiniz
        """.trimIndent()
    }
}
