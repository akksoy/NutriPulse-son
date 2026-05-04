package com.nutripulse.app.data

import com.nutripulse.app.data.model.Feed
import com.nutripulse.app.data.model.FeedCategories

object FeedData3 {
    fun getFeeds(): List<Feed> {
        val list = mutableListOf<Feed>()

        // ══════════════════════════════════════════
        // YAG KAYNAKLARI — tam degerler
        // ══════════════════════════════════════════
        list.add(Feed(name="Bitkisel Yag",category=FeedCategories.FAT,dm=99.0,me=34.0,nel=21.0,fat=99.0,maxDmPct=5.0,maxDailyKg=0.8, pricePerKg=35.00))
        list.add(Feed(name="Hayvansal Yag",category=FeedCategories.FAT,dm=99.0,me=33.0,nel=20.0,fat=99.0,maxDmPct=4.0,maxDailyKg=0.7, pricePerKg=32.00))
        list.add(Feed(name="Yag Kuyruk",category=FeedCategories.FAT,dm=95.0,me=30.0,nel=18.5,fat=92.0,maxDmPct=6.0,maxDailyKg=1.0, pricePerKg=28.00))
        list.add(Feed(name="Korunan Yag Ca-Soap",category=FeedCategories.FAT,dm=96.0,me=26.0,nel=16.0,fat=84.0,ca=9.0,maxDmPct=3.0,maxDailyKg=0.5, pricePerKg=38.00))
        list.add(Feed(name="Tam Yag Kolza Tohumu",category=FeedCategories.FAT,dm=93.0,me=19.0,nel=11.5,cp=22.0,fat=40.0,ca=0.38,p=0.72,maxDmPct=8.0, pricePerKg=18.00))
        list.add(Feed(name="Aycicek Yagi",category=FeedCategories.FAT,dm=99.0,me=34.0,nel=21.0,fat=99.0,maxDmPct=3.0, pricePerKg=36.00))
        list.add(Feed(name="Misir Yagi",category=FeedCategories.FAT,dm=99.0,me=34.0,nel=21.0,fat=99.0,maxDmPct=3.0,maxDailyKg=0.6, pricePerKg=36.00))
        list.add(Feed(name="Soya Yagi",category=FeedCategories.FAT,dm=99.0,me=34.0,nel=21.0,fat=99.0,maxDmPct=3.0,maxDailyKg=0.6, pricePerKg=36.00))
        list.add(Feed(name="Korunan Yag Palmiye",category=FeedCategories.FAT,dm=96.0,me=25.0,nel=15.5,fat=82.0,ca=9.5,maxDmPct=3.0,maxDailyKg=0.5, pricePerKg=34.00))
        list.add(Feed(name="Tam Yag Soya Extruded",category=FeedCategories.FAT,dm=92.0,me=17.0,nel=10.2,cp=37.0,rdp=20.0,rup=17.0,ndf=10.0,fat=19.0,ca=0.28,p=0.60,lys=2.45,met=0.52,maxDmPct=15.0, pricePerKg=18.00))

        // ══════════════════════════════════════════
        // MINERAL KAYNAKLARI (saf mineral tuzlari)
        // ══════════════════════════════════════════
        list.add(Feed(name="Kirec Tasi CaCO3",category=FeedCategories.MINERAL,dm=99.0,ca=38.0,maxDmPct=2.0, pricePerKg=2.00))
        list.add(Feed(name="Dikalsiyum Fosfat",category=FeedCategories.MINERAL,dm=99.0,ca=22.0,p=18.5,maxDmPct=1.5, pricePerKg=15.00))
        list.add(Feed(name="Monofosfat",category=FeedCategories.MINERAL,dm=99.0,ca=16.0,p=22.0,maxDmPct=1.0, pricePerKg=15.00))
        list.add(Feed(name="Trikalsiyum Fosfat",category=FeedCategories.MINERAL,dm=99.0,ca=32.0,p=15.0,maxDmPct=1.5, pricePerKg=15.00))
        list.add(Feed(name="Sodyum Klorur Tuz",category=FeedCategories.MINERAL,dm=99.0,na=39.0,maxDailyKg=0.15,maxDmPct=0.5, pricePerKg=5.00))
        list.add(Feed(name="Sodyum Bikarbonat",category=FeedCategories.MINERAL,dm=99.0,na=27.0,maxDailyKg=0.25,maxDmPct=0.8, pricePerKg=12.00))
        list.add(Feed(name="Magnezyum Oksit",category=FeedCategories.MINERAL,dm=99.0,mg=54.0,maxDailyKg=0.10,maxDmPct=0.3, pricePerKg=12.00))
        list.add(Feed(name="Magnezyum Sulfat",category=FeedCategories.MINERAL,dm=99.0,mg=20.0,s=13.0,maxDailyKg=0.15, pricePerKg=10.00))
        list.add(Feed(name="Kalsiyum Klorur",category=FeedCategories.MINERAL,dm=99.0,ca=36.0,maxDailyKg=0.10, pricePerKg=10.00))
        list.add(Feed(name="Potasyum Klorur",category=FeedCategories.MINERAL,dm=99.0,k=52.0,maxDailyKg=0.08, pricePerKg=12.00))
        list.add(Feed(name="Amonyum Sulfat",category=FeedCategories.MINERAL,dm=99.0,s=24.0,maxDailyKg=0.05, pricePerKg=10.00))
        list.add(Feed(name="Zeolite",category=FeedCategories.MINERAL,dm=95.0,ca=4.0,maxDailyKg=0.20, pricePerKg=8.00))
        list.add(Feed(name="Bentonit",category=FeedCategories.MINERAL,dm=92.0,ca=2.5,maxDailyKg=0.30, pricePerKg=8.00))
        list.add(Feed(name="Kirec Tasi Unu",category=FeedCategories.MINERAL,dm=99.0,ca=38.0,maxDmPct=2.0, pricePerKg=2.00))
        list.add(Feed(name="Istiridye Kabugu",category=FeedCategories.MINERAL,dm=99.0,ca=36.0,maxDmPct=2.0, pricePerKg=2.00))
        list.add(Feed(name="Fosfat Kaya",category=FeedCategories.MINERAL,dm=99.0,ca=32.0,p=14.0,maxDmPct=1.0, pricePerKg=10.00))
        // Türkiye Yem Fabrikası - Süt İneği Mineral Kaynakları
        list.add(Feed(name="Mermer Tozu",category=FeedCategories.MINERAL,dm=99.0,ca=36.0,maxDmPct=2.0,
            notes="Kalsiyum kaynağı, yem fabrikalarında yaygın kullanılır", pricePerKg=2.00))
        // Eser mineraller
        list.add(Feed(name="Cinkosulfat",category=FeedCategories.MINERAL,dm=99.0,maxDailyKg=0.02, pricePerKg=60.00))
        list.add(Feed(name="Mangan Sulfat",category=FeedCategories.MINERAL,dm=99.0,maxDailyKg=0.02, pricePerKg=60.00))
        list.add(Feed(name="Bakir Sulfat",category=FeedCategories.MINERAL,dm=99.0,maxDailyKg=0.005, pricePerKg=120.00))
        list.add(Feed(name="Demir Sulfat",category=FeedCategories.MINERAL,dm=99.0,maxDailyKg=0.02, pricePerKg=40.00))
        list.add(Feed(name="Selenyum Kaynagi",category=FeedCategories.MINERAL,dm=99.0,maxDailyKg=0.001, pricePerKg=400.00))
        list.add(Feed(name="Organik Cinko",category=FeedCategories.MINERAL,dm=99.0,maxDailyKg=0.02))
        list.add(Feed(name="Organik Selenyum",category=FeedCategories.MINERAL,dm=99.0,maxDailyKg=0.001))

        // ══════════════════════════════════════════
        // VITAMIN KAYNAKLARI (saf vitaminler)
        // ══════════════════════════════════════════
        list.add(Feed(name="A Vitamini 1000",category=FeedCategories.VITAMIN,dm=99.0,maxDailyKg=0.005,notes="1 mIU/g potens", pricePerKg=400.00))
        list.add(Feed(name="D3 Vitamini 500",category=FeedCategories.VITAMIN,dm=99.0,maxDailyKg=0.003,notes="500.000 IU/g", pricePerKg=350.00))
        list.add(Feed(name="E Vitamini 50",category=FeedCategories.VITAMIN,dm=99.0,maxDailyKg=0.01,notes="50% dl-alfa tokoferil asetat", pricePerKg=120.00))
        list.add(Feed(name="K3 Vitamini",category=FeedCategories.VITAMIN,dm=99.0,maxDailyKg=0.002, pricePerKg=80.00))
        list.add(Feed(name="B1 Tiamin",category=FeedCategories.VITAMIN,dm=99.0,maxDailyKg=0.002, pricePerKg=80.00))
        list.add(Feed(name="B2 Riboflavin",category=FeedCategories.VITAMIN,dm=99.0,maxDailyKg=0.002, pricePerKg=80.00))
        list.add(Feed(name="B3 Niasin",category=FeedCategories.VITAMIN,dm=99.0,maxDailyKg=0.01, pricePerKg=80.00))
        list.add(Feed(name="B5 Pantotenik Asit",category=FeedCategories.VITAMIN,dm=99.0,maxDailyKg=0.005, pricePerKg=80.00))
        list.add(Feed(name="B6 Piridoksin",category=FeedCategories.VITAMIN,dm=99.0,maxDailyKg=0.002, pricePerKg=80.00))
        list.add(Feed(name="B12 Siyanokobalamin",category=FeedCategories.VITAMIN,dm=99.0,maxDailyKg=0.001, pricePerKg=400.00))
        list.add(Feed(name="Folik Asit",category=FeedCategories.VITAMIN,dm=99.0,maxDailyKg=0.001, pricePerKg=400.00))
        list.add(Feed(name="Biyotin",category=FeedCategories.VITAMIN,dm=99.0,maxDailyKg=0.001, pricePerKg=400.00))
        list.add(Feed(name="Kolin Klorur 70",category=FeedCategories.VITAMIN,dm=70.0,maxDailyKg=0.05, pricePerKg=60.00))
        list.add(Feed(name="Kolin Klorur 60",category=FeedCategories.VITAMIN,dm=60.0,maxDailyKg=0.05, pricePerKg=60.00))
        list.add(Feed(name="Beta Karoten",category=FeedCategories.VITAMIN,dm=99.0,maxDailyKg=0.005, pricePerKg=400.00))

        // ══════════════════════════════════════════
        // PREMİKS & KARMA (hazir karisimlar)
        // ══════════════════════════════════════════
        list.add(Feed(name="Sut Inegi Premiksi",category=FeedCategories.PREMIKS,dm=98.0,ca=12.0,p=6.0,mg=3.0,na=4.0,maxDailyKg=0.20,notes="Mineral+Vitamin komple premiks", pricePerKg=80.00))
        list.add(Feed(name="Kuru Inek Premiksi",category=FeedCategories.PREMIKS,dm=98.0,ca=16.0,p=7.0,mg=3.5,na=4.0,maxDailyKg=0.20, pricePerKg=80.00))
        list.add(Feed(name="Gecis Donemi Premiksi",category=FeedCategories.PREMIKS,dm=98.0,ca=14.0,p=6.5,mg=4.0,na=3.8,maxDailyKg=0.25,notes="Doğumdan 3 hafta once/sonra", pricePerKg=90.00))
        list.add(Feed(name="Besi Premiksi",category=FeedCategories.PREMIKS,dm=98.0,ca=15.0,p=6.5,mg=2.5,maxDailyKg=0.15, pricePerKg=80.00))
        list.add(Feed(name="Koyun Premiksi",category=FeedCategories.PREMIKS,dm=98.0,ca=14.0,p=5.5,mg=2.0,maxDailyKg=0.10, pricePerKg=80.00))
        list.add(Feed(name="Keci Premiksi",category=FeedCategories.PREMIKS,dm=98.0,ca=13.0,p=5.0,mg=2.0,maxDailyKg=0.08, pricePerKg=80.00))
        // list.add(Feed(name="At Premiksi",category=FeedCategories.PREMIKS,dm=98.0,ca=12.0,p=5.0,mg=2.5,maxDailyKg=0.15))
        // list.add(Feed(name="Tavsan Premiksi",category=FeedCategories.PREMIKS,dm=98.0,ca=10.0,p=4.5,mg=2.0,maxDailyKg=0.05))
        // list.add(Feed(name="Broiler Premiksi",category=FeedCategories.PREMIKS,dm=98.0,ca=10.0,p=5.0,maxDailyKg=0.05))
        // list.add(Feed(name="Yumurtaci Premiksi",category=FeedCategories.PREMIKS,dm=98.0,ca=12.0,p=4.5,maxDailyKg=0.05))
        // list.add(Feed(name="Hindi Premiksi",category=FeedCategories.PREMIKS,dm=98.0,ca=11.0,p=5.0,maxDailyKg=0.05))
        // list.add(Feed(name="Bildircin Premiksi",category=FeedCategories.PREMIKS,dm=98.0,ca=9.0,p=4.0,maxDailyKg=0.03))
        // list.add(Feed(name="Su Urunleri Premiksi",category=FeedCategories.PREMIKS,dm=98.0,ca=8.0,p=4.5,maxDmPct=1.0))
        list.add(Feed(name="Sut Inegi Mineral Blogu",category=FeedCategories.PREMIKS,dm=97.0,ca=18.0,p=5.0,mg=4.0,na=8.0,k=2.0,maxDailyKg=0.15,notes="Yalama tasi sekli", pricePerKg=80.00))
        list.add(Feed(name="Tampon Mineral Karisim",category=FeedCategories.PREMIKS,dm=99.0,na=15.0,mg=5.0,maxDailyKg=0.20, pricePerKg=80.00))
        list.add(Feed(name="DCAD Mineral Karisim",category=FeedCategories.PREMIKS,dm=98.0,ca=12.0,mg=4.0,na=1.0,k=0.5,maxDailyKg=0.20,notes="Anyonik diyet - dogum oncesi", pricePerKg=90.00))

        // ══════════════════════════════════════════
        // SU ÜRÜNLERİ YEMLERİ
        // list.add(Feed(name="Alabalik Baslangic 0.3mm",category=FeedCategories.AQUA,dm=93.0,me=16.0,cp=50.0,fat=12.0,ca=1.50,p=1.20,lys=3.20,met=1.20))
        // list.add(Feed(name="Alabalik Buyutme 1.0mm",category=FeedCategories.AQUA,dm=93.0,me=17.0,cp=46.0,fat=16.0,ca=1.40,p=1.10,lys=3.00,met=1.10))
        // list.add(Feed(name="Alabalik Bitirme 4.0mm",category=FeedCategories.AQUA,dm=93.0,me=17.5,cp=42.0,fat=18.0,ca=1.30,p=1.00,lys=2.80,met=1.00))
        // list.add(Feed(name="Levrek Yemi Yavru",category=FeedCategories.AQUA,dm=93.0,me=16.5,cp=52.0,fat=14.0,ca=1.60,p=1.30,lys=3.40,met=1.30))
        // list.add(Feed(name="Levrek Yemi Buyutme",category=FeedCategories.AQUA,dm=93.0,me=17.0,cp=48.0,fat=16.0,ca=1.50,p=1.20))
        // list.add(Feed(name="Cipura Yemi Yavru",category=FeedCategories.AQUA,dm=93.0,me=16.5,cp=52.0,fat=14.0,ca=1.60,p=1.30))
        // list.add(Feed(name="Cipura Yemi Buyutme",category=FeedCategories.AQUA,dm=93.0,me=17.0,cp=48.0,fat=16.0,ca=1.50,p=1.20))
        // list.add(Feed(name="Sazan Yemi",category=FeedCategories.AQUA,dm=91.0,me=14.0,cp=32.0,fat=8.0,ca=1.20,p=0.90))

        return list
    }
}