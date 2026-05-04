package com.nutripulse.app.data

import com.nutripulse.app.data.model.Feed
import com.nutripulse.app.data.model.FeedCategories

object FeedData8 {
    fun getFeeds(): List<Feed> {
        val list = mutableListOf<Feed>()

        // ENERJI KAYNAKLARI (Yag kategorisinde)
        list.add(Feed(name="Misir Yagi",category=FeedCategories.FAT,dm=99.0,me=34.0,nel=21.0,fat=99.0,maxDmPct=3.0,maxDailyKg=0.6,pricePerKg=36.00))
        list.add(Feed(name="Soya Yagi",category=FeedCategories.FAT,dm=99.0,me=34.0,nel=21.0,fat=99.0,maxDmPct=3.0,maxDailyKg=0.6,pricePerKg=36.00))
        list.add(Feed(name="Kolza Yagi",category=FeedCategories.FAT,dm=99.0,me=34.0,nel=21.0,fat=99.0,maxDmPct=3.0,maxDailyKg=0.5,pricePerKg=35.00))
        list.add(Feed(name="Pamuk Yagi",category=FeedCategories.FAT,dm=99.0,me=33.5,nel=20.5,fat=99.0,maxDmPct=3.0,pricePerKg=32.00))
        list.add(Feed(name="Zeytinyagi Ham",category=FeedCategories.FAT,dm=99.0,me=34.0,nel=21.0,fat=99.0,maxDmPct=2.0,pricePerKg=36.00))
        list.add(Feed(name="Tam Yag Soya Extruded",category=FeedCategories.FAT,dm=92.0,me=17.0,nel=10.2,cp=37.0,rdp=20.0,rup=17.0,ndf=10.0,fat=19.0,ca=0.28,p=0.60,lys=2.45,met=0.52,maxDmPct=15.0,pricePerKg=18.00))

        // MISIR URUNLERI (Tahıl alt grubu)
        list.add(Feed(name="Misir Gluten Yemi 18",category=FeedCategories.GRAIN,dm=89.0,me=11.0,nel=6.5,cp=18.0,rdp=12.0,rup=6.0,ndf=42.0,fat=3.0,starch=20.0,ca=0.10,p=0.72,maxDmPct=25.0,pricePerKg=8.00))
        list.add(Feed(name="Misir Malt Posasi",category=FeedCategories.GRAIN,dm=92.0,me=12.0,nel=7.2,cp=24.0,ndf=38.0,fat=6.0,starch=15.0,ca=0.12,p=0.58,maxDmPct=15.0,pricePerKg=8.00))
        list.add(Feed(name="Misir Germ Yagi Cikarilmis",category=FeedCategories.GRAIN,dm=90.0,me=11.5,nel=6.8,cp=20.0,ndf=34.0,fat=4.5,starch=22.0,ca=0.08,p=0.62,pricePerKg=8.00))
        list.add(Feed(name="Bugday Kepeği Ham",category=FeedCategories.GRAIN,dm=87.0,me=10.0,nel=5.9,cp=15.5,ndf=45.0,fat=4.2,starch=18.0,ca=0.12,p=1.15,pricePerKg=6.50))
        list.add(Feed(name="Bugday Ruşeymi",category=FeedCategories.GRAIN,dm=88.0,me=14.5,nel=8.7,cp=26.0,ndf=16.0,fat=9.0,starch=25.0,ca=0.08,p=1.10,lys=1.80,met=0.55,pricePerKg=8.00))
        list.add(Feed(name="Tritikale Kepegi",category=FeedCategories.GRAIN,dm=88.0,me=10.2,nel=6.0,cp=15.0,ndf=42.0,fat=3.8,ca=0.11,p=0.88,pricePerKg=6.50))
        list.add(Feed(name="Sorgum Kepegi",category=FeedCategories.GRAIN,dm=88.0,me=10.5,nel=6.2,cp=12.0,ndf=36.0,fat=4.5,starch=18.0,ca=0.06,p=0.72,pricePerKg=6.50))

        // KARMA YEM KONSANTRELER
        list.add(Feed(name="Sut Inegi Konsantre 36",category=FeedCategories.GRAIN,dm=88.0,me=12.0,nel=7.2,cp=36.0,rdp=22.0,rup=14.0,ndf=18.0,fat=4.5,ca=1.20,p=0.80,lys=2.20,met=0.68,pricePerKg=12.00))
        list.add(Feed(name="Sut Inegi Konsantre 32",category=FeedCategories.GRAIN,dm=88.0,me=12.0,nel=7.2,cp=32.0,rdp=20.0,rup=12.0,ndf=20.0,fat=4.0,ca=1.10,p=0.75,pricePerKg=11.00))
        list.add(Feed(name="Besi Dana Konsantre 38",category=FeedCategories.GRAIN,dm=88.0,me=12.5,nel=7.5,cp=38.0,ndf=16.0,fat=4.5,ca=1.30,p=0.85,pricePerKg=12.00))
        list.add(Feed(name="Koyun Keci Konsantre 32",category=FeedCategories.GRAIN,dm=88.0,me=12.0,nel=7.2,cp=32.0,ndf=18.0,fat=4.0,ca=1.20,p=0.75,pricePerKg=11.00))
        list.add(Feed(name="Kanatli Konsantre 40",category=FeedCategories.GRAIN,dm=88.0,me=12.5,nel=7.5,cp=40.0,ndf=10.0,fat=5.0,ca=1.50,p=0.95,lys=2.80,met=1.10,pricePerKg=13.00))
        list.add(Feed(name="Yumurtaci Konsantre 38",category=FeedCategories.GRAIN,dm=88.0,me=12.0,nel=7.2,cp=38.0,ndf=10.0,fat=4.5,ca=3.50,p=0.85,lys=2.60,met=1.00,pricePerKg=13.00))
        list.add(Feed(name="Broiler Baslangic Konsantre",category=FeedCategories.GRAIN,dm=88.0,me=13.0,nel=7.8,cp=42.0,ndf=8.0,fat=6.0,ca=1.60,p=1.10,lys=3.20,met=1.30,pricePerKg=14.00))
        list.add(Feed(name="Hindi Konsantre",category=FeedCategories.GRAIN,dm=88.0,me=12.5,nel=7.5,cp=40.0,ndf=10.0,fat=5.0,ca=1.55,p=1.00,lys=3.00,met=1.15,pricePerKg=13.00))

        return list
    }
}