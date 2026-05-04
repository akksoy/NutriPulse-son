package com.nutripulse.app.data.model

object FeedCategories {
    const val ROUGHAGE_WET = "KABA_SULU"
    const val ROUGHAGE_DRY = "KABA_KURU"
    const val GRAIN        = "TAHIL"
    const val PROTEIN      = "PROTEIN"
    const val BYPRODUCT    = "YAN_URUN"
    const val FAT          = "YAG"
    const val MINERAL      = "MINERAL"
    const val VITAMIN      = "VITAMIN"
    const val PREMIKS      = "PREMIKS"
    const val ADDITIVE     = "KATKI"
    const val AQUA         = "SU_URUNLERI"

    val ALL = listOf(
        ROUGHAGE_WET, ROUGHAGE_DRY, GRAIN, PROTEIN,
        BYPRODUCT, FAT, MINERAL, VITAMIN, PREMIKS, ADDITIVE, AQUA
    )

    fun displayName(code: String): String = when (code) {
        ROUGHAGE_WET -> "Sulu Kaba Yemler"
        ROUGHAGE_DRY -> "Kuru Kaba Yemler"
        GRAIN        -> "Tahıllar & Kesif"
        PROTEIN      -> "Protein Kaynakları"
        BYPRODUCT    -> "Yan Ürünler"
        FAT          -> "Yağ Kaynakları"
        MINERAL      -> "Mineral Kaynakları"
        VITAMIN      -> "Vitamin Kaynakları"
        PREMIKS      -> "Premiks & Karma"
        ADDITIVE     -> "Katkı Maddeleri"
        AQUA         -> "Su Ürünleri Yemleri"
        else         -> code
    }

    fun color(code: String): String = when (code) {
        ROUGHAGE_WET -> "#00FF88"
        ROUGHAGE_DRY -> "#00CC6A"
        GRAIN        -> "#FFC400"
        PROTEIN      -> "#00D4FF"
        BYPRODUCT    -> "#FF7A00"
        FAT          -> "#FF5FA0"
        MINERAL      -> "#BF7FFF"
        VITAMIN      -> "#FFE066"
        PREMIKS      -> "#66EEFF"
        ADDITIVE     -> "#00EEFF"
        AQUA         -> "#00BBFF"
        else         -> "#00FF88"
    }

    fun shortName(code: String): String = when (code) {
        ROUGHAGE_WET -> "Sulu"
        ROUGHAGE_DRY -> "Kuru"
        GRAIN        -> "Tahıl"
        PROTEIN      -> "Protein"
        BYPRODUCT    -> "Yan Ürün"
        FAT          -> "Yağ"
        MINERAL      -> "Mineral"
        VITAMIN      -> "Vitamin"
        PREMIKS      -> "Premiks"
        ADDITIVE     -> "Katkı"
        AQUA         -> "Su Ürün"
        else         -> code
    }
}
