package com.nutripulse.app.data

import com.nutripulse.app.data.model.Feed
import com.nutripulse.app.data.model.FeedCategories

object FeedData10 {
    fun getFeeds(): List<Feed> {
        val list = mutableListOf<Feed>()

        // KANATLI AMINO ASIT ODAKLI PROTEIN KAYNAKLARI
        list.add(Feed(name="Soya Kuspesi Kanatli 48",category=FeedCategories.PROTEIN,
            dm=89.0,me=13.5,nel=8.2,cp=48.0,rdp=30.0,rup=18.0,ndf=10.0,fat=1.5,
            ca=0.30,p=0.68,lys=3.10,met=0.66,thr=1.88,trp=0.62,maxDmPct=30.0,
            pricePerKg=13.50))

        list.add(Feed(name="Kanola Kuspesi Kanatli",category=FeedCategories.PROTEIN,
            dm=90.0,me=12.0,nel=7.2,cp=38.0,rdp=24.0,rup=14.0,ndf=28.0,fat=3.5,
            ca=0.65,p=1.05,lys=2.10,met=0.72,thr=1.68,trp=0.45,maxDmPct=15.0,
            pricePerKg=10.00))

        // KANATLI SPESIFIK YEM HAMMADDELERI
        list.add(Feed(name="Misir Sarı Dane Kanatli",category=FeedCategories.GRAIN,
            dm=87.0,me=14.2,nel=8.5,cp=9.0,rdp=6.5,rup=2.5,ndf=10.0,fat=3.8,
            starch=68.0,ca=0.03,p=0.30,lys=0.25,met=0.18,thr=0.32,trp=0.07,maxDmPct=65.0,
            pricePerKg=7.00))

        list.add(Feed(name="Sorgum Kanatli",category=FeedCategories.GRAIN,
            dm=87.0,me=13.2,nel=7.9,cp=10.0,ndf=13.0,fat=3.0,starch=62.0,
            ca=0.04,p=0.32,lys=0.22,met=0.15,thr=0.30,trp=0.08,maxDmPct=30.0,
            pricePerKg=7.00))

        list.add(Feed(name="Arpa Kanatli",category=FeedCategories.GRAIN,
            dm=87.0,me=11.5,nel=6.8,cp=12.0,ndf=17.0,fat=2.2,starch=57.0,
            ca=0.05,p=0.35,lys=0.40,met=0.18,thr=0.38,trp=0.12,maxDmPct=20.0,
            pricePerKg=6.50))

        list.add(Feed(name="Bugday Kanatli",category=FeedCategories.GRAIN,
            dm=87.0,me=13.5,nel=8.1,cp=13.0,ndf=15.0,fat=2.0,starch=63.0,
            ca=0.05,p=0.38,lys=0.38,met=0.22,thr=0.38,trp=0.15,maxDmPct=30.0,
            pricePerKg=6.80))

        list.add(Feed(name="Pirinc Kirigi Kanatli",category=FeedCategories.GRAIN,
            dm=87.0,me=13.8,nel=8.3,cp=8.5,ndf=6.0,fat=2.0,starch=73.0,
            ca=0.04,p=0.28,lys=0.32,met=0.18,thr=0.28,trp=0.10,maxDmPct=30.0,
            pricePerKg=7.00))

        list.add(Feed(name="Tapioka Kanatli",category=FeedCategories.GRAIN,
            dm=87.0,me=13.5,nel=8.1,cp=2.5,ndf=8.0,fat=0.5,starch=78.0,
            ca=0.12,p=0.10,lys=0.05,met=0.02,thr=0.05,trp=0.02,maxDmPct=20.0,
            pricePerKg=8.00))

        // KANATLI MINERAL & VITAMIN
        list.add(Feed(name="Broiler Mineral Premiks",category=FeedCategories.MINERAL,
            dm=98.0,ca=10.0,p=5.0,mg=1.5,na=1.5,maxDailyKg=0.05,
            pricePerKg=80.00))

        list.add(Feed(name="Yumurtaci Mineral Premiks",category=FeedCategories.MINERAL,
            dm=98.0,ca=12.0,p=4.5,mg=1.5,na=1.5,maxDailyKg=0.05,
            pricePerKg=80.00))

        list.add(Feed(name="Hindi Mineral Premiks",category=FeedCategories.MINERAL,
            dm=98.0,ca=11.0,p=5.0,mg=1.8,na=1.8,maxDailyKg=0.05,
            pricePerKg=80.00))

        list.add(Feed(name="Bildircin Mineral Premiks",category=FeedCategories.MINERAL,
            dm=98.0,ca=9.0,p=4.0,mg=1.2,na=1.2,maxDailyKg=0.03,
            pricePerKg=80.00))

        list.add(Feed(name="Kalsiyum Karbonat Kanatli",category=FeedCategories.MINERAL,
            dm=99.0,ca=38.0,maxDmPct=8.0,
            pricePerKg=2.00))

        list.add(Feed(name="Istiridye Kabuğu Kanatli",category=FeedCategories.MINERAL,
            dm=99.0,ca=36.0,maxDmPct=10.0,
            pricePerKg=2.00))

        list.add(Feed(name="Monofosfat Kanatli",category=FeedCategories.MINERAL,
            dm=99.0,ca=16.0,p=22.0,maxDmPct=2.0,
            pricePerKg=15.00))

        list.add(Feed(name="Sodyum Klorur Kanatli",category=FeedCategories.MINERAL,
            dm=99.0,na=39.0,maxDmPct=0.5,
            pricePerKg=5.00))

        // KANATLI KABA/YAN URUN
        list.add(Feed(name="Lucern Unu Kanatli",category=FeedCategories.ROUGHAGE_DRY,
            dm=90.0,me=7.5,nel=4.2,cp=18.0,ndf=38.0,fat=2.5,ca=1.50,p=0.26,
            lys=0.90,met=0.28,maxDmPct=5.0,
            pricePerKg=4.50))

        list.add(Feed(name="Citrus Posasi Kanatli",category=FeedCategories.BYPRODUCT,
            dm=91.0,me=10.5,nel=6.2,cp=6.5,ndf=20.0,fat=3.0,sugar=18.0,
            ca=2.00,p=0.10,maxDmPct=5.0,
            pricePerKg=7.00))

        list.add(Feed(name="Bira Posasi Kuru Kanatli",category=FeedCategories.BYPRODUCT,
            dm=92.0,me=10.5,nel=6.2,cp=27.0,ndf=44.0,fat=7.0,
            ca=0.28,p=0.52,lys=0.82,met=0.45,thr=0.88,trp=0.30,maxDmPct=8.0,
            pricePerKg=7.00))

        list.add(Feed(name="Misir DDG Kanatli",category=FeedCategories.BYPRODUCT,
            dm=90.0,me=12.0,nel=7.2,cp=28.0,rdp=12.0,rup=16.0,ndf=36.0,fat=10.0,
            ca=0.08,p=0.72,lys=0.72,met=0.52,thr=0.92,trp=0.20,maxDmPct=15.0))

        list.add(Feed(name="Pamuk Kuspesi Kanatli",category=FeedCategories.PROTEIN,
            dm=90.0,me=9.5,nel=5.5,cp=41.0,ndf=26.0,fat=2.5,
            ca=0.20,p=1.10,lys=1.68,met=0.57,thr=1.38,trp=0.48,maxDmPct=8.0))

        // KANATLI HAZIR YEM KATKI
        list.add(Feed(name="Broiler Vitamin Premiks",category=FeedCategories.ADDITIVE,
            dm=98.0,maxDailyKg=0.10))

        list.add(Feed(name="Yumurtaci Vitamin Premiks",category=FeedCategories.ADDITIVE,
            dm=98.0,maxDailyKg=0.10))

        list.add(Feed(name="Koksidiyostat Salinomisin",category=FeedCategories.ADDITIVE,
            dm=99.0,maxDailyKg=0.001))

        list.add(Feed(name="Koksidiyostat Narasin",category=FeedCategories.ADDITIVE,
            dm=99.0,maxDailyKg=0.001))

        list.add(Feed(name="Fitaz 5000 Kanatli",category=FeedCategories.ADDITIVE,
            dm=92.0,maxDailyKg=0.005))

        list.add(Feed(name="Ksilanaz Kanatli",category=FeedCategories.ADDITIVE,
            dm=92.0,maxDailyKg=0.005))

        list.add(Feed(name="Beta Glukanaز Kanatli",category=FeedCategories.ADDITIVE,
            dm=92.0,maxDailyKg=0.005))

        list.add(Feed(name="Organik Asit Kanatli",category=FeedCategories.ADDITIVE,
            dm=99.0,maxDailyKg=0.03))

        list.add(Feed(name="Probiyotik Kanatli",category=FeedCategories.ADDITIVE,
            dm=95.0,maxDailyKg=0.01))

        list.add(Feed(name="Zeatin Mayas Kanatli",category=FeedCategories.ADDITIVE,
            dm=93.0,maxDailyKg=0.05))

        list.add(Feed(name="Kolin Klorur 60 Kanatli",category=FeedCategories.ADDITIVE,
            dm=60.0,maxDailyKg=0.05))

        list.add(Feed(name="Betain Kanatli",category=FeedCategories.ADDITIVE,
            dm=96.0,maxDailyKg=0.02,notes="Osmoprotektan, met tasarrufu"))

        return list
    }
}