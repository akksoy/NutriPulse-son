package com.nutripulse.app.data

import com.nutripulse.app.data.model.Feed
import com.nutripulse.app.data.model.FeedCategories

object FeedData5 {
    fun getFeeds(): List<Feed> {
        val list = mutableListOf<Feed>()

        // EK PROTEIN KAYNAKLARI
        list.add(Feed(name="Soya Proteini Konsantresi",category=FeedCategories.PROTEIN,dm=90.0,me=14.0,nel=8.5,cp=65.0,rdp=25.0,rup=40.0,ndf=8.0,fat=1.0,ca=0.35,p=0.72,lys=4.20,met=0.90,maxDmPct=15.0,pricePerKg=16.00))
        list.add(Feed(name="Yer Fistigi Kuspesi",category=FeedCategories.PROTEIN,dm=92.0,me=12.0,nel=7.2,cp=50.0,rdp=32.0,rup=18.0,ndf=18.0,fat=6.0,ca=0.20,p=0.60,lys=1.80,met=0.48,maxDmPct=15.0,pricePerKg=13.00))
        list.add(Feed(name="Susam Kuspesi",category=FeedCategories.PROTEIN,dm=92.0,me=11.5,nel=6.8,cp=45.0,ndf=20.0,fat=5.0,ca=2.20,p=1.35,lys=1.30,met=1.30,maxDmPct=10.0,pricePerKg=13.00))
        list.add(Feed(name="Ketencik Kuspesi",category=FeedCategories.PROTEIN,dm=90.0,me=11.0,nel=6.5,cp=35.0,ndf=32.0,fat=3.5,ca=0.40,p=0.85,maxDmPct=15.0,pricePerKg=12.00))
        list.add(Feed(name="Keten Tohumu Kuspesi",category=FeedCategories.PROTEIN,dm=90.0,me=11.5,nel=6.8,cp=36.0,rdp=24.0,rup=12.0,ndf=30.0,fat=3.0,ca=0.40,p=0.85,lys=1.60,met=0.58,maxDmPct=12.0,pricePerKg=12.00))
        list.add(Feed(name="Aspir Kuspesi Yuksek Protein",category=FeedCategories.PROTEIN,dm=91.0,me=9.5,nel=5.5,cp=42.0,ndf=35.0,fat=2.5,ca=0.28,p=1.10,maxDmPct=10.0,pricePerKg=12.00))
        list.add(Feed(name="Misir Germ Kuspesi",category=FeedCategories.PROTEIN,dm=90.0,me=12.0,nel=7.2,cp=22.0,ndf=32.0,fat=8.0,ca=0.08,p=0.65,maxDmPct=15.0,pricePerKg=10.00))
        list.add(Feed(name="Soya Küspesi Islak",category=FeedCategories.PROTEIN,dm=25.0,me=13.0,nel=7.8,cp=44.0,rdp=28.0,rup=16.0,ndf=15.0,fat=2.5,ca=0.30,p=0.65,maxDailyKg=5.0,pricePerKg=4.00))

        // EK YAN URUNLER
        list.add(Feed(name="Findik Kuspesi",category=FeedCategories.BYPRODUCT,dm=91.0,me=10.5,nel=6.2,cp=28.0,ndf=35.0,fat=12.0,ca=0.32,p=0.55,maxDmPct=10.0,pricePerKg=8.00))
        list.add(Feed(name="Limon Posasi Kuru",category=FeedCategories.BYPRODUCT,dm=91.0,me=11.5,nel=6.8,cp=7.5,ndf=28.0,fat=3.0,sugar=14.0,ca=2.10,p=0.10,maxDmPct=12.0,pricePerKg=7.00))
        list.add(Feed(name="Nar Posasi Kuru",category=FeedCategories.BYPRODUCT,dm=90.0,me=9.0,nel=5.2,cp=12.0,ndf=44.0,fat=5.5,ca=0.80,p=0.18,maxDmPct=8.0,pricePerKg=7.00))
        list.add(Feed(name="Üzüm Posasi Kuru",category=FeedCategories.BYPRODUCT,dm=90.0,me=8.5,nel=4.8,cp=10.0,ndf=48.0,fat=5.0,ca=0.72,p=0.30,maxDmPct=8.0,pricePerKg=7.00))
        list.add(Feed(name="Zeytinyagi Tortulari",category=FeedCategories.BYPRODUCT,dm=45.0,me=9.0,nel=5.2,cp=6.0,ndf=45.0,fat=12.0,ca=0.30,p=0.08,maxDmPct=5.0,pricePerKg=6.00))
        list.add(Feed(name="Kesif Yem Fabrika Atigi",category=FeedCategories.BYPRODUCT,dm=90.0,me=12.0,nel=7.2,cp=16.0,ndf=25.0,fat=5.0,ca=0.60,p=0.45,pricePerKg=7.00))
        list.add(Feed(name="Misir Nisastasi Artigi",category=FeedCategories.BYPRODUCT,dm=88.0,me=13.0,nel=7.8,cp=12.0,ndf=18.0,fat=4.5,starch=45.0,ca=0.08,p=0.35,pricePerKg=7.00))

        // EK MINERAL & VITAMIN
        list.add(Feed(name="Kalsiyum Klorur",category=FeedCategories.MINERAL,dm=99.0,ca=36.0,maxDailyKg=0.10,pricePerKg=10.00))
        list.add(Feed(name="Cinkosulfat",category=FeedCategories.MINERAL,dm=99.0,maxDailyKg=0.02,pricePerKg=60.00))
        list.add(Feed(name="Mangan Sulfat",category=FeedCategories.MINERAL,dm=99.0,maxDailyKg=0.02,pricePerKg=60.00))
        list.add(Feed(name="Bakir Sulfat",category=FeedCategories.MINERAL,dm=99.0,maxDailyKg=0.005,pricePerKg=120.00))
        list.add(Feed(name="Demir Sulfat",category=FeedCategories.MINERAL,dm=99.0,maxDailyKg=0.02,pricePerKg=40.00))
        list.add(Feed(name="Selenyum Kaynagi",category=FeedCategories.MINERAL,dm=99.0,maxDailyKg=0.001,pricePerKg=400.00))
        list.add(Feed(name="Kobalt Kaynagi",category=FeedCategories.MINERAL,dm=99.0,maxDailyKg=0.001,pricePerKg=80.00))
        list.add(Feed(name="Iyot Kaynagi",category=FeedCategories.MINERAL,dm=99.0,maxDailyKg=0.001,pricePerKg=80.00))
        list.add(Feed(name="A Vitamini",category=FeedCategories.MINERAL,dm=99.0,maxDailyKg=0.005,pricePerKg=400.00))
        list.add(Feed(name="D3 Vitamini",category=FeedCategories.MINERAL,dm=99.0,maxDailyKg=0.003,pricePerKg=350.00))
        list.add(Feed(name="E Vitamini",category=FeedCategories.MINERAL,dm=99.0,maxDailyKg=0.01,pricePerKg=120.00))
        list.add(Feed(name="B12 Vitamini",category=FeedCategories.MINERAL,dm=99.0,maxDailyKg=0.001,pricePerKg=400.00))
        list.add(Feed(name="Biyotin",category=FeedCategories.MINERAL,dm=99.0,maxDailyKg=0.001,pricePerKg=400.00))
        list.add(Feed(name="Niasin Nikotin Asit",category=FeedCategories.MINERAL,dm=99.0,maxDailyKg=0.01,pricePerKg=80.00))
        list.add(Feed(name="Kolin Klorur",category=FeedCategories.MINERAL,dm=70.0,maxDailyKg=0.05,pricePerKg=60.00))
        list.add(Feed(name="Lizin HCl Sentetik",category=FeedCategories.MINERAL,dm=99.0,lys=78.0,maxDailyKg=0.02,pricePerKg=80.00))
        list.add(Feed(name="DL-Metiyonin Sentetik",category=FeedCategories.MINERAL,dm=99.0,met=99.0,maxDailyKg=0.01,pricePerKg=80.00))
        list.add(Feed(name="Treonin Sentetik",category=FeedCategories.MINERAL,dm=99.0,thr=98.0,maxDailyKg=0.01,pricePerKg=80.00))
        list.add(Feed(name="At Premiksi",category=FeedCategories.MINERAL,dm=98.0,ca=12.0,p=5.0,mg=2.5,maxDailyKg=0.15,pricePerKg=80.00))
        list.add(Feed(name="Tavsan Premiksi",category=FeedCategories.MINERAL,dm=98.0,ca=10.0,p=4.5,mg=2.0,maxDailyKg=0.05,pricePerKg=80.00))

        // EK KATKI MADDELERI
        list.add(Feed(name="Monensin",category=FeedCategories.ADDITIVE,dm=99.0,maxDailyKg=0.001,notes="Iyonofor antibiyotik",pricePerKg=40.00))
        list.add(Feed(name="Lasalosid",category=FeedCategories.ADDITIVE,dm=99.0,maxDailyKg=0.001,pricePerKg=40.00))
        list.add(Feed(name="Virginiamisin",category=FeedCategories.ADDITIVE,dm=99.0,maxDailyKg=0.001,pricePerKg=40.00))
        list.add(Feed(name="Flavomisin",category=FeedCategories.ADDITIVE,dm=99.0,maxDailyKg=0.001,pricePerKg=40.00))
        list.add(Feed(name="Korunan Kolin",category=FeedCategories.ADDITIVE,dm=50.0,maxDailyKg=0.05,pricePerKg=60.00))
        list.add(Feed(name="Gliserin Ham",category=FeedCategories.ADDITIVE,dm=80.0,me=14.5,nel=8.8,maxDailyKg=0.30,maxDmPct=5.0,pricePerKg=20.00))
        list.add(Feed(name="Sodyum Propionat",category=FeedCategories.ADDITIVE,dm=99.0,na=18.0,maxDailyKg=0.15,pricePerKg=20.00))
        list.add(Feed(name="Kalsiyum Propionat",category=FeedCategories.ADDITIVE,dm=99.0,ca=22.0,maxDailyKg=0.15,pricePerKg=20.00))
        list.add(Feed(name="Fitaz Enzim",category=FeedCategories.ADDITIVE,dm=92.0,maxDailyKg=0.005,pricePerKg=80.00))
        list.add(Feed(name="Selulaz Enzim",category=FeedCategories.ADDITIVE,dm=92.0,maxDailyKg=0.005,pricePerKg=80.00))
        list.add(Feed(name="Proteaz Enzim",category=FeedCategories.ADDITIVE,dm=92.0,maxDailyKg=0.005,pricePerKg=80.00))
        list.add(Feed(name="Organik Asit Karisimi",category=FeedCategories.ADDITIVE,dm=99.0,maxDailyKg=0.02,pricePerKg=40.00))
        list.add(Feed(name="Esansiyel Yag Karisimi",category=FeedCategories.ADDITIVE,dm=99.0,maxDailyKg=0.01,pricePerKg=80.00))
        list.add(Feed(name="Beta Karoten",category=FeedCategories.ADDITIVE,dm=99.0,maxDailyKg=0.005,pricePerKg=400.00))
        list.add(Feed(name="Zeatin Mayas",category=FeedCategories.ADDITIVE,dm=93.0,maxDailyKg=0.05,pricePerKg=80.00))

        // EK SU URUNLERI
        list.add(Feed(name="Somon Yemi Yavru",category=FeedCategories.AQUA,dm=93.0,me=17.0,cp=50.0,fat=14.0,ca=1.50,p=1.20,pricePerKg=35.00))
        list.add(Feed(name="Somon Yemi Buyutme",category=FeedCategories.AQUA,dm=93.0,me=18.0,cp=45.0,fat=18.0,ca=1.40,p=1.10,pricePerKg=35.00))
        list.add(Feed(name="Tilapya Yemi",category=FeedCategories.AQUA,dm=91.0,me=13.5,cp=32.0,fat=7.0,ca=1.20,p=0.88,pricePerKg=30.00))
        list.add(Feed(name="Yayın Yemi",category=FeedCategories.AQUA,dm=91.0,me=14.0,cp=35.0,fat=8.0,ca=1.30,p=0.95,pricePerKg=30.00))
        list.add(Feed(name="Karides Yemi",category=FeedCategories.AQUA,dm=92.0,me=15.0,cp=38.0,fat=7.0,ca=1.50,p=1.00,pricePerKg=35.00))
        list.add(Feed(name="Midye Yemi",category=FeedCategories.AQUA,dm=90.0,me=13.0,cp=28.0,fat=5.0,ca=1.80,p=0.85,pricePerKg=30.00))
        list.add(Feed(name="Yılan Baligi Yemi",category=FeedCategories.AQUA,dm=93.0,me=17.5,cp=48.0,fat=15.0,ca=1.40,p=1.15,pricePerKg=35.00))
        list.add(Feed(name="Turna Yemi",category=FeedCategories.AQUA,dm=92.0,me=15.5,cp=40.0,fat=10.0,ca=1.35,p=1.00,pricePerKg=35.00))

        return list
    }
}