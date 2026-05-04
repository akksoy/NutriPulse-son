package com.nutripulse.app.data

import com.nutripulse.app.data.model.Feed
import com.nutripulse.app.data.model.FeedCategories

object FeedData1 {
    fun getFeeds(): List<Feed> {
        val list = mutableListOf<Feed>()

        // ══ SULU KABA YEMLER — tam degerler (fiyatlar TL/kg taze) ══
        list.add(Feed(name="Misir Silaji",category=FeedCategories.ROUGHAGE_WET,
            dm=33.0,me=10.5,nel=6.3,nem=5.8,neg=3.5,
            cp=8.5,rdp=6.0,rup=2.5,ndf=44.0,adf=27.0,adl=4.5,
            fat=3.2,starch=28.0,ash=4.5,
            ca=0.25,p=0.22,mg=0.18,na=0.01,k=1.10,s=0.12,maxDmPct=60.0,
            pricePerKg=2.80))

        list.add(Feed(name="Yonca Silaji",category=FeedCategories.ROUGHAGE_WET,
            dm=35.0,me=9.8,nel=5.8,nem=5.2,neg=3.0,
            cp=18.0,rdp=13.0,rup=5.0,ndf=42.0,adf=32.0,adl=8.0,
            fat=2.8,ash=10.0,ca=1.50,p=0.28,mg=0.30,na=0.04,k=2.20,s=0.28,
            pricePerKg=4.20))

        list.add(Feed(name="Sorgum Silaji",category=FeedCategories.ROUGHAGE_WET,
            dm=30.0,me=9.8,nel=5.7,nem=5.1,neg=3.0,
            cp=8.0,rdp=5.5,rup=2.5,ndf=55.0,adf=34.0,adl=5.0,
            fat=2.5,starch=12.0,ca=0.30,p=0.20,mg=0.17,k=1.20,s=0.10,
            pricePerKg=3.50))

        list.add(Feed(name="Tritikale Silaji",category=FeedCategories.ROUGHAGE_WET,
            dm=32.0,me=10.0,nel=6.0,nem=5.4,neg=3.2,
            cp=10.5,rdp=7.5,rup=3.0,ndf=50.0,adf=30.0,adl=4.0,
            fat=2.8,starch=15.0,ca=0.30,p=0.25,k=1.50,s=0.12,
            pricePerKg=3.80))

        list.add(Feed(name="Arpa Silaji",category=FeedCategories.ROUGHAGE_WET,
            dm=30.0,me=9.9,nel=5.9,nem=5.3,neg=3.1,
            cp=10.0,rdp=7.0,rup=3.0,ndf=52.0,adf=32.0,adl=4.2,
            fat=2.6,starch=10.0,ca=0.28,p=0.24,k=1.40,s=0.11,
            pricePerKg=3.80))

        list.add(Feed(name="Bugday Silaji",category=FeedCategories.ROUGHAGE_WET,
            dm=30.0,me=9.7,nel=5.6,nem=5.0,neg=2.9,
            cp=11.0,rdp=7.8,rup=3.2,ndf=53.0,adf=33.0,adl=4.5,
            fat=2.5,starch=8.0,ca=0.27,p=0.23,k=1.35,s=0.11,
            pricePerKg=3.80))

        list.add(Feed(name="Cayir Merasi Silaji Taze",category=FeedCategories.ROUGHAGE_WET,
            dm=20.0,me=10.5,nel=6.2,nem=5.6,neg=3.4,
            cp=15.0,rdp=11.0,rup=4.0,ndf=45.0,adf=28.0,adl=5.0,
            fat=3.5,ca=0.55,p=0.30,mg=0.22,na=0.03,k=2.50,s=0.22,
            pricePerKg=3.50))

        list.add(Feed(name="Cayir Merasi Soguk Mevsim",category=FeedCategories.ROUGHAGE_WET,
            dm=20.0,me=11.0,nel=6.8,nem=6.2,neg=3.8,
            cp=18.0,rdp=13.5,rup=4.5,ndf=40.0,adf=25.0,adl=4.0,
            fat=4.0,ca=0.60,p=0.35,mg=0.25,na=0.04,k=2.80,s=0.25,
            pricePerKg=3.50))

        list.add(Feed(name="Cayir Merasi Sicak Mevsim",category=FeedCategories.ROUGHAGE_WET,
            dm=22.0,me=9.8,nel=5.8,nem=5.2,neg=3.0,
            cp=13.0,rdp=9.5,rup=3.5,ndf=55.0,adf=35.0,adl=6.0,
            fat=3.0,ca=0.45,p=0.28,mg=0.20,k=2.20,s=0.18,
            pricePerKg=3.50))

        list.add(Feed(name="Misir Hasili",category=FeedCategories.ROUGHAGE_WET,
            dm=30.0,me=8.5,nel=4.8,nem=4.2,neg=2.4,
            cp=6.0,rdp=4.5,rup=1.5,ndf=60.0,adf=38.0,adl=6.0,
            fat=1.8,ca=0.30,p=0.15,k=0.90,s=0.08,
            pricePerKg=2.80))

        list.add(Feed(name="Seker Pancari Yapragi Silaji",category=FeedCategories.ROUGHAGE_WET,
            dm=18.0,me=9.5,nel=5.5,nem=4.9,neg=2.8,
            cp=15.0,rdp=11.0,rup=4.0,ndf=35.0,adf=22.0,fat=2.5,
            ca=1.20,p=0.25,mg=0.30,na=0.80,k=3.50,s=0.20,maxDailyKg=15.0,
            pricePerKg=3.00))

        list.add(Feed(name="Pancar Posasi Silaji",category=FeedCategories.ROUGHAGE_WET,
            dm=22.0,me=11.0,nel=6.8,nem=6.2,neg=3.8,
            cp=9.5,rdp=7.0,rup=2.5,ndf=43.0,adf=22.0,fat=1.2,sugar=5.0,
            ca=0.60,p=0.08,mg=0.15,k=0.80,s=0.12,
            pricePerKg=3.00))

        // ══ KURU KABA YEMLER — tam degerler ══
        list.add(Feed(name="Yonca Kuru Otu Kalite1",category=FeedCategories.ROUGHAGE_DRY,
            dm=88.0,me=10.5,nel=6.2,nem=5.6,neg=3.3,
            cp=20.0,rdp=14.0,rup=6.0,ndf=38.0,adf=28.0,adl=6.5,
            fat=2.5,ash=10.5,ca=1.60,p=0.28,mg=0.32,na=0.04,k=2.40,s=0.28,
            pricePerKg=4.50))

        list.add(Feed(name="Yonca Kuru Otu Kalite2",category=FeedCategories.ROUGHAGE_DRY,
            dm=88.0,me=9.8,nel=5.8,nem=5.2,neg=3.0,
            cp=18.0,rdp=13.0,rup=5.0,ndf=42.0,adf=32.0,adl=7.5,
            fat=2.2,ca=1.45,p=0.25,mg=0.28,k=2.20,s=0.25,
            pricePerKg=4.00))

        list.add(Feed(name="Yonca Kuru Otu Kalite3",category=FeedCategories.ROUGHAGE_DRY,
            dm=88.0,me=8.8,nel=5.0,nem=4.4,neg=2.5,
            cp=15.0,rdp=11.0,rup=4.0,ndf=48.0,adf=38.0,adl=9.0,
            fat=2.0,ca=1.30,p=0.22,mg=0.24,k=1.90,s=0.22,
            pricePerKg=3.50))

        list.add(Feed(name="Cayir Kuru Otu",category=FeedCategories.ROUGHAGE_DRY,
            dm=87.0,me=8.5,nel=4.8,nem=4.2,neg=2.4,
            cp=10.0,rdp=7.0,rup=3.0,ndf=58.0,adf=36.0,adl=7.0,
            fat=2.0,ca=0.50,p=0.20,mg=0.15,k=1.80,s=0.15,
            pricePerKg=3.50))

        list.add(Feed(name="Bugday Saman",category=FeedCategories.ROUGHAGE_DRY,
            dm=90.0,me=6.0,nel=3.0,nem=2.8,neg=1.2,
            cp=3.8,rdp=2.8,rup=1.0,ndf=74.0,adf=50.0,adl=7.5,
            fat=1.5,ca=0.20,p=0.07,mg=0.08,k=1.20,s=0.06,maxDmPct=20.0,
            pricePerKg=3.00))

        list.add(Feed(name="Arpa Saman",category=FeedCategories.ROUGHAGE_DRY,
            dm=90.0,me=6.5,nel=3.3,nem=3.0,neg=1.4,
            cp=4.2,rdp=3.0,rup=1.2,ndf=72.0,adf=48.0,adl=7.0,
            fat=1.8,ca=0.25,p=0.08,mg=0.10,k=1.50,s=0.07,maxDmPct=20.0,
            pricePerKg=3.00))

        list.add(Feed(name="Misir Kocan Kuru",category=FeedCategories.ROUGHAGE_DRY,
            dm=88.0,me=7.0,nel=3.8,nem=3.4,neg=1.8,
            cp=3.5,rdp=2.5,rup=1.0,ndf=80.0,adf=45.0,adl=6.0,
            fat=0.8,ca=0.12,p=0.06,k=0.60,s=0.05,
            pricePerKg=3.00))

        list.add(Feed(name="Tritikale Kuru Otu",category=FeedCategories.ROUGHAGE_DRY,
            dm=88.0,me=9.5,nel=5.5,nem=4.9,neg=2.8,
            cp=11.0,rdp=7.8,rup=3.2,ndf=52.0,adf=32.0,adl=5.0,
            fat=2.5,starch=12.0,ca=0.30,p=0.25,k=1.60,s=0.12,
            pricePerKg=3.50))

        list.add(Feed(name="Yulaf Kuru Otu",category=FeedCategories.ROUGHAGE_DRY,
            dm=87.0,me=9.0,nel=5.2,nem=4.6,neg=2.6,
            cp=9.5,rdp=6.8,rup=2.7,ndf=56.0,adf=34.0,adl=5.5,
            fat=2.8,ca=0.28,p=0.22,k=1.70,s=0.12,
            pricePerKg=3.50))

        list.add(Feed(name="Fig Otu",category=FeedCategories.ROUGHAGE_DRY,
            dm=87.0,me=9.8,nel=5.7,nem=5.1,neg=3.0,
            cp=17.0,rdp=12.0,rup=5.0,ndf=42.0,adf=30.0,adl=7.0,
            fat=2.5,ca=1.10,p=0.28,mg=0.25,k=2.00,s=0.22,
            pricePerKg=3.80))

        list.add(Feed(name="Korunga Otu",category=FeedCategories.ROUGHAGE_DRY,
            dm=87.0,me=9.5,nel=5.5,nem=4.9,neg=2.8,
            cp=16.0,rdp=11.5,rup=4.5,ndf=43.0,adf=31.0,adl=7.5,
            fat=2.2,ca=1.30,p=0.26,mg=0.22,k=1.90,s=0.20,
            pricePerKg=3.80))

        list.add(Feed(name="Bugday Samani Amonyakli",category=FeedCategories.ROUGHAGE_DRY,
            dm=88.0,me=7.8,nel=4.2,nem=3.8,neg=2.0,
            cp=8.0,rdp=6.5,rup=1.5,ndf=70.0,adf=46.0,adl=7.0,
            fat=1.5,ca=0.22,p=0.08,k=1.30,s=0.07,
            pricePerKg=3.20))

        // ══ TAHILLAR — tam degerler ══
        list.add(Feed(name="Arpa",category=FeedCategories.GRAIN,
            dm=87.0,me=13.0,nel=7.8,nem=7.2,neg=4.8,
            cp=12.0,rdp=9.0,rup=3.0,ndf=17.0,adf=6.0,adl=1.5,
            fat=2.2,starch=57.0,sugar=2.5,ash=2.4,
            ca=0.05,p=0.35,mg=0.14,na=0.02,k=0.52,s=0.15,
            lys=0.40,met=0.18,thr=0.38,trp=0.12,maxDmPct=40.0,
            pricePerKg=6.50))

        list.add(Feed(name="Misir Dane",category=FeedCategories.GRAIN,
            dm=87.0,me=14.2,nel=8.5,nem=7.9,neg=5.3,
            cp=9.0,rdp=6.5,rup=2.5,ndf=10.0,adf=3.5,adl=0.8,
            fat=3.8,starch=68.0,sugar=2.0,ash=1.4,
            ca=0.03,p=0.30,mg=0.12,na=0.01,k=0.40,s=0.12,
            lys=0.25,met=0.18,thr=0.32,trp=0.07,maxDmPct=50.0,
            pricePerKg=7.00))

        list.add(Feed(name="Bugday",category=FeedCategories.GRAIN,
            dm=87.0,me=13.5,nel=8.1,nem=7.5,neg=5.0,
            cp=13.0,rdp=9.5,rup=3.5,ndf=15.0,adf=4.0,adl=0.9,
            fat=2.0,starch=63.0,ash=1.8,
            ca=0.05,p=0.38,mg=0.15,na=0.01,k=0.48,s=0.16,
            lys=0.38,met=0.22,thr=0.38,trp=0.15,maxDmPct=30.0,
            pricePerKg=6.80))

        list.add(Feed(name="Yulaf",category=FeedCategories.GRAIN,
            dm=87.0,me=11.5,nel=6.8,nem=6.2,neg=4.0,
            cp=11.5,rdp=8.5,rup=3.0,ndf=28.0,adf=14.0,adl=2.5,
            fat=4.5,starch=42.0,ash=3.0,
            ca=0.08,p=0.32,mg=0.14,k=0.45,s=0.22,
            lys=0.42,met=0.18,thr=0.38,trp=0.14))

        list.add(Feed(name="Sorgum Dane",category=FeedCategories.GRAIN,
            dm=87.0,me=13.2,nel=7.9,nem=7.3,neg=4.9,
            cp=10.0,rdp=7.5,rup=2.5,ndf=13.0,adf=5.5,adl=1.0,
            fat=3.0,starch=62.0,ash=1.6,
            ca=0.04,p=0.32,mg=0.15,k=0.38,s=0.12,
            lys=0.22,met=0.15,thr=0.30,trp=0.08))

        list.add(Feed(name="Tritikale Dane",category=FeedCategories.GRAIN,
            dm=87.0,me=13.0,nel=7.7,nem=7.1,neg=4.7,
            cp=13.5,rdp=9.8,rup=3.7,ndf=14.0,adf=5.0,adl=1.0,
            fat=2.2,starch=58.0,ash=2.0,
            ca=0.05,p=0.36,mg=0.15,k=0.50,s=0.16,
            lys=0.42,met=0.22,thr=0.40,trp=0.14))

        list.add(Feed(name="Cavdar",category=FeedCategories.GRAIN,
            dm=87.0,me=12.8,nel=7.5,nem=6.9,neg=4.6,
            cp=11.0,rdp=8.0,rup=3.0,ndf=18.0,adf=6.5,adl=1.5,
            fat=1.8,starch=55.0,ash=1.8,
            ca=0.06,p=0.32,mg=0.12,k=0.52,s=0.14,
            lys=0.35,met=0.18,thr=0.35,trp=0.12))

        list.add(Feed(name="Misir Gluten Yemi",category=FeedCategories.GRAIN,
            dm=88.0,me=12.0,nel=7.2,nem=6.6,neg=4.3,
            cp=22.0,rdp=14.0,rup=8.0,ndf=38.0,adf=12.0,adl=2.0,
            fat=3.5,starch=22.0,ca=0.10,p=0.85,mg=0.25,k=0.75,s=0.32,
            lys=0.68,met=0.42,thr=0.82,trp=0.15,maxDmPct=25.0,
            pricePerKg=8.00))

        list.add(Feed(name="Bugday Kepegi",category=FeedCategories.GRAIN,
            dm=88.0,me=10.5,nel=6.2,nem=5.6,neg=3.5,
            cp=16.5,rdp=11.5,rup=5.0,ndf=42.0,adf=13.0,adl=3.0,
            fat=4.0,starch=20.0,ash=5.5,
            ca=0.12,p=1.20,mg=0.55,na=0.04,k=1.10,s=0.22,
            lys=0.62,met=0.22,thr=0.52,trp=0.22,maxDmPct=20.0,
            pricePerKg=6.50))

        list.add(Feed(name="Pirinc Kepegi",category=FeedCategories.GRAIN,
            dm=90.0,me=11.5,nel=6.8,nem=6.2,neg=4.0,
            cp=14.0,rdp=10.0,rup=4.0,ndf=26.0,adf=10.0,adl=2.5,
            fat=14.0,starch=28.0,ash=10.0,
            ca=0.08,p=1.60,mg=0.85,k=1.40,s=0.18,
            lys=0.52,met=0.28,thr=0.48,trp=0.12,maxDmPct=10.0))

        list.add(Feed(name="Ekmek Artigi Kuru",category=FeedCategories.GRAIN,
            dm=92.0,me=13.5,nel=8.0,nem=7.4,neg=4.9,
            cp=11.0,rdp=7.5,rup=3.5,ndf=10.0,fat=8.0,starch=42.0,sugar=8.0,
            ca=0.20,p=0.22,na=0.60,k=0.28,s=0.12,
            lys=0.32,met=0.18,thr=0.32,trp=0.12,maxDmPct=15.0))

        return list
    }
}