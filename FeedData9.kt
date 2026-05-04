package com.nutripulse.app.data

import com.nutripulse.app.data.model.Feed
import com.nutripulse.app.data.model.FeedCategories

object FeedData9 {
    fun getFeeds(): List<Feed> {
        val list = mutableListOf<Feed>()

        // KATKI MADDELERI - ek
        list.add(Feed(name="Sodyum Sesquikarbonat",category=FeedCategories.ADDITIVE,dm=99.0,na=23.0,maxDailyKg=0.20, pricePerKg=20.00))
        list.add(Feed(name="Potasyum Bikarbonat",category=FeedCategories.ADDITIVE,dm=99.0,k=38.0,maxDailyKg=0.10, pricePerKg=20.00))
        list.add(Feed(name="Amonyum Bikarbonat",category=FeedCategories.ADDITIVE,dm=99.0,maxDailyKg=0.10, pricePerKg=20.00))
        list.add(Feed(name="Formaldehit Koruyucu",category=FeedCategories.ADDITIVE,dm=37.0,maxDailyKg=0.02,notes="Protein bypass icin", pricePerKg=80.00))
        list.add(Feed(name="Tanen Ekstrakti",category=FeedCategories.ADDITIVE,dm=95.0,maxDailyKg=0.02, pricePerKg=80.00))
        list.add(Feed(name="Etoksikin",category=FeedCategories.ADDITIVE,dm=99.0,maxDailyKg=0.001, pricePerKg=80.00))
        list.add(Feed(name="BHT Antioksidan",category=FeedCategories.ADDITIVE,dm=99.0,maxDailyKg=0.001, pricePerKg=80.00))
        list.add(Feed(name="Organoasit Karisim Pro",category=FeedCategories.ADDITIVE,dm=99.0,maxDailyKg=0.03, pricePerKg=40.00))
        list.add(Feed(name="Probiyotik Karisim",category=FeedCategories.ADDITIVE,dm=95.0,maxDailyKg=0.01, pricePerKg=80.00))
        list.add(Feed(name="Prebiyotik MOS",category=FeedCategories.ADDITIVE,dm=95.0,maxDailyKg=0.01, pricePerKg=80.00))
        list.add(Feed(name="Beta Glukan",category=FeedCategories.ADDITIVE,dm=95.0,maxDailyKg=0.01, pricePerKg=80.00))
        list.add(Feed(name="Levamizol",category=FeedCategories.ADDITIVE,dm=99.0,maxDailyKg=0.001,notes="Bagisiklik duzenleyici", pricePerKg=80.00))
        list.add(Feed(name="Oksitetrasiklin",category=FeedCategories.ADDITIVE,dm=99.0,maxDailyKg=0.001, pricePerKg=80.00))
        list.add(Feed(name="Koksidiyostat",category=FeedCategories.ADDITIVE,dm=99.0,maxDailyKg=0.001, pricePerKg=80.00))
        list.add(Feed(name="Avilamicin",category=FeedCategories.ADDITIVE,dm=99.0,maxDailyKg=0.001, pricePerKg=80.00))
        list.add(Feed(name="Korunan Kolin 25",category=FeedCategories.ADDITIVE,dm=25.0,maxDailyKg=0.05, pricePerKg=60.00))
        list.add(Feed(name="Nikotinik Asit",category=FeedCategories.ADDITIVE,dm=99.0,maxDailyKg=0.01, pricePerKg=80.00))
        list.add(Feed(name="Pantotenik Asit",category=FeedCategories.ADDITIVE,dm=99.0,maxDailyKg=0.005, pricePerKg=80.00))
        list.add(Feed(name="Riboflavin B2",category=FeedCategories.ADDITIVE,dm=99.0,maxDailyKg=0.002, pricePerKg=80.00))
        list.add(Feed(name="Piridoksin B6",category=FeedCategories.ADDITIVE,dm=99.0,maxDailyKg=0.002, pricePerKg=80.00))
        list.add(Feed(name="Tiamin B1",category=FeedCategories.ADDITIVE,dm=99.0,maxDailyKg=0.002, pricePerKg=80.00))
        list.add(Feed(name="Folik Asit",category=FeedCategories.ADDITIVE,dm=99.0,maxDailyKg=0.001, pricePerKg=400.00))
        list.add(Feed(name="K3 Vitamini",category=FeedCategories.ADDITIVE,dm=99.0,maxDailyKg=0.002, pricePerKg=80.00))

        // MINERAL - ek spesifik
        list.add(Feed(name="Organik Cinko",category=FeedCategories.MINERAL,dm=99.0,maxDailyKg=0.02, pricePerKg=60.00))
        list.add(Feed(name="Organik Mangan",category=FeedCategories.MINERAL,dm=99.0,maxDailyKg=0.02, pricePerKg=60.00))
        list.add(Feed(name="Organik Bakir",category=FeedCategories.MINERAL,dm=99.0,maxDailyKg=0.005, pricePerKg=120.00))
        list.add(Feed(name="Organik Selenyum",category=FeedCategories.MINERAL,dm=99.0,maxDailyKg=0.001, pricePerKg=400.00))
        list.add(Feed(name="Magnezyum Sulfat",category=FeedCategories.MINERAL,dm=99.0,mg=20.0,maxDailyKg=0.15, pricePerKg=10.00))
        list.add(Feed(name="Kalsiyum Propionat Mineral",category=FeedCategories.MINERAL,dm=99.0,ca=22.0,maxDailyKg=0.15, pricePerKg=10.00))
        list.add(Feed(name="Yem Kireci",category=FeedCategories.MINERAL,dm=99.0,ca=36.0,maxDmPct=2.0, pricePerKg=2.00))
        list.add(Feed(name="Deniz Yosunu Mineral",category=FeedCategories.MINERAL,dm=88.0,ca=3.50,mg=0.85,na=2.50,k=1.80,maxDmPct=2.0, pricePerKg=8.00))
        list.add(Feed(name="At Mineral Premiks",category=FeedCategories.MINERAL,dm=98.0,ca=14.0,p=6.0,mg=2.8,na=3.5,maxDailyKg=0.15, pricePerKg=80.00))
        list.add(Feed(name="Kuru Inck Premiksi",category=FeedCategories.MINERAL,dm=98.0,ca=16.0,p=7.0,mg=3.5,na=4.0,maxDailyKg=0.20, pricePerKg=80.00))
        list.add(Feed(name="Gecis Donemi Mineral",category=FeedCategories.MINERAL,dm=98.0,ca=14.0,p=6.5,mg=4.0,na=3.8,maxDailyKg=0.25, pricePerKg=90.00))
        list.add(Feed(name="Tampon Mineral Karisim",category=FeedCategories.MINERAL,dm=99.0,na=15.0,mg=5.0,maxDailyKg=0.20, pricePerKg=80.00))

        // YAN URUN - kalan
        list.add(Feed(name="Portakal Posasi Kuru",category=FeedCategories.BYPRODUCT,dm=91.0,me=11.5,nel=6.8,cp=6.5,ndf=24.0,fat=2.0,sugar=18.0,ca=1.95,p=0.10,maxDmPct=10.0, pricePerKg=7.00))
        list.add(Feed(name="Ananas Posasi Kuru",category=FeedCategories.BYPRODUCT,dm=90.0,me=9.5,nel=5.5,cp=5.5,ndf=38.0,fat=2.0,sugar=22.0,ca=0.32,p=0.10,maxDmPct=8.0, pricePerKg=7.00))
        list.add(Feed(name="Muz Kuru",category=FeedCategories.BYPRODUCT,dm=88.0,me=12.0,nel=7.2,cp=5.0,ndf=8.0,fat=1.5,sugar=60.0,ca=0.10,p=0.12,maxDmPct=8.0, pricePerKg=8.00))
        list.add(Feed(name="Hurma Posasi",category=FeedCategories.BYPRODUCT,dm=90.0,me=10.5,nel=6.2,cp=5.0,ndf=25.0,fat=2.5,sugar=50.0,ca=0.22,p=0.08,maxDmPct=8.0, pricePerKg=7.00))
        list.add(Feed(name="Misir Kocan Posasi",category=FeedCategories.BYPRODUCT,dm=90.0,me=8.5,nel=4.8,cp=4.5,ndf=72.0,adf=42.0,fat=0.8,ca=0.08,p=0.06, pricePerKg=4.00))
        list.add(Feed(name="Bugday Kesik Kepegi",category=FeedCategories.BYPRODUCT,dm=88.0,me=11.5,nel=6.8,cp=18.0,ndf=35.0,fat=5.0,starch=25.0,ca=0.12,p=1.30, pricePerKg=6.50))
        list.add(Feed(name="Havuc Posasi Kuru",category=FeedCategories.BYPRODUCT,dm=88.0,me=10.5,nel=6.2,cp=8.0,ndf=30.0,fat=2.5,sugar=20.0,ca=0.50,p=0.28,maxDmPct=8.0, pricePerKg=6.00))
        list.add(Feed(name="Ispanak Posasi",category=FeedCategories.BYPRODUCT,dm=15.0,me=9.0,nel=5.2,cp=18.0,ndf=22.0,fat=3.5,ca=0.80,p=0.42,maxDailyKg=2.0, pricePerKg=7.00))
        list.add(Feed(name="Patates Cipsi Kirigi",category=FeedCategories.BYPRODUCT,dm=95.0,me=13.5,nel=8.1,cp=7.0,ndf=6.0,fat=18.0,starch=48.0,na=0.80,maxDmPct=5.0, pricePerKg=6.00))
        list.add(Feed(name="Bisküvi Kirigi Kuru",category=FeedCategories.BYPRODUCT,dm=94.0,me=14.0,nel=8.4,cp=9.0,ndf=5.0,fat=14.0,starch=50.0,sugar=12.0,maxDmPct=10.0, pricePerKg=7.00))
        list.add(Feed(name="Makarna Kirigi",category=FeedCategories.BYPRODUCT,dm=87.0,me=13.5,nel=8.1,cp=12.5,ndf=5.0,fat=1.5,starch=68.0,ca=0.04,p=0.22,maxDmPct=10.0, pricePerKg=7.00))
        list.add(Feed(name="Pirinç Unu Kirigi",category=FeedCategories.BYPRODUCT,dm=87.0,me=13.8,nel=8.3,cp=8.0,ndf=4.0,fat=2.0,starch=72.0,ca=0.03,p=0.24,maxDmPct=15.0, pricePerKg=7.00))

        // PROTEIN - kanatli spesifik
        list.add(Feed(name="Misir+Soya Karmasi Bazik",category=FeedCategories.PROTEIN,dm=88.0,me=13.5,nel=8.1,cp=22.0,rdp=14.0,rup=8.0,ndf=12.0,fat=4.0,starch=45.0,ca=0.15,p=0.48,lys=1.40,met=0.50, pricePerKg=14.00))
        list.add(Feed(name="Soya+Ayçiçek Karisimi",category=FeedCategories.PROTEIN,dm=89.0,me=11.5,nel=6.8,cp=40.0,rdp=26.0,rup=14.0,ndf=24.0,fat=2.5,ca=0.34,p=0.86,lys=2.12,met=0.73, pricePerKg=14.00))
        list.add(Feed(name="Kanola+Soya Karisimi",category=FeedCategories.PROTEIN,dm=89.0,me=12.5,nel=7.5,cp=40.0,rdp=26.0,rup=14.0,ndf=20.0,fat=3.0,ca=0.47,p=0.85,lys=2.30,met=0.70, pricePerKg=14.00))

        return list
    }
}