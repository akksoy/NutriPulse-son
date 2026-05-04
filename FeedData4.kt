package com.nutripulse.app.data

import com.nutripulse.app.data.model.Feed
import com.nutripulse.app.data.model.FeedCategories

object FeedData4 {
    fun getFeeds(): List<Feed> {
        val list = mutableListOf<Feed>()

        // EK SULU KABA YEMLER
        list.add(Feed(name="Italyan Cadir Silaji",category=FeedCategories.ROUGHAGE_WET,dm=28.0,me=10.2,nel=6.1,cp=14.0,ndf=48.0,adf=28.0,fat=3.0,ca=0.45,p=0.28,pricePerKg=3.50))
        list.add(Feed(name="Sudanotu Silaji",category=FeedCategories.ROUGHAGE_WET,dm=28.0,me=9.5,nel=5.5,cp=9.0,ndf=58.0,adf=36.0,fat=2.2,ca=0.28,p=0.18,pricePerKg=3.50))
        list.add(Feed(name="Misir Kocan Silaji",category=FeedCategories.ROUGHAGE_WET,dm=35.0,me=9.0,nel=5.2,cp=5.5,ndf=65.0,adf=38.0,fat=1.5,ca=0.15,p=0.12,pricePerKg=2.80))
        list.add(Feed(name="Sorgum-Sudan Melezi Silaj",category=FeedCategories.ROUGHAGE_WET,dm=28.0,me=9.8,nel=5.7,cp=9.5,ndf=56.0,adf=34.0,fat=2.3,ca=0.30,p=0.20,pricePerKg=3.50))
        list.add(Feed(name="Yem Bicagi Silaji",category=FeedCategories.ROUGHAGE_WET,dm=18.0,me=10.8,nel=6.5,cp=16.0,ndf=38.0,adf=24.0,fat=3.8,ca=0.52,p=0.32,pricePerKg=3.80))
        list.add(Feed(name="Hayvan Pancari",category=FeedCategories.ROUGHAGE_WET,dm=12.0,me=12.5,nel=7.5,cp=8.5,ndf=15.0,fat=0.8,sugar=60.0,ca=0.12,p=0.18,maxDailyKg=20.0,pricePerKg=2.80))
        list.add(Feed(name="Seker Pancari",category=FeedCategories.ROUGHAGE_WET,dm=23.0,me=13.0,nel=7.8,cp=6.5,ndf=10.0,fat=0.5,sugar=70.0,ca=0.10,p=0.16,maxDailyKg=10.0,pricePerKg=2.80))
        list.add(Feed(name="Patates",category=FeedCategories.ROUGHAGE_WET,dm=22.0,me=13.0,nel=7.8,cp=9.5,ndf=8.0,fat=0.5,starch=65.0,ca=0.04,p=0.20,maxDailyKg=8.0,pricePerKg=2.80))
        list.add(Feed(name="Havuc",category=FeedCategories.ROUGHAGE_WET,dm=12.0,me=12.0,nel=7.0,cp=8.5,ndf=12.0,fat=1.5,sugar=30.0,ca=0.35,p=0.28,maxDailyKg=5.0,pricePerKg=2.80))

        // EK KURU KABA YEMLER
        list.add(Feed(name="Misir Sapi Kuru",category=FeedCategories.ROUGHAGE_DRY,dm=86.0,me=7.2,nel=3.9,cp=4.5,ndf=76.0,adf=48.0,fat=1.2,ca=0.22,p=0.08,pricePerKg=3.00))
        list.add(Feed(name="Pamuk Sapi",category=FeedCategories.ROUGHAGE_DRY,dm=90.0,me=6.8,nel=3.5,cp=4.0,ndf=72.0,adf=52.0,fat=1.0,ca=0.35,p=0.06,pricePerKg=3.00))
        list.add(Feed(name="Susam Posasi",category=FeedCategories.ROUGHAGE_DRY,dm=90.0,me=9.0,nel=5.2,cp=40.0,ndf=22.0,fat=8.0,ca=2.50,p=1.30,pricePerKg=4.00))
        list.add(Feed(name="Aspir Kuspesi",category=FeedCategories.ROUGHAGE_DRY,dm=91.0,me=8.5,nel=4.8,cp=24.0,ndf=45.0,fat=2.5,ca=0.28,p=0.85,pricePerKg=4.00))
        list.add(Feed(name="Italyan Cadir Kuru",category=FeedCategories.ROUGHAGE_DRY,dm=87.0,me=9.5,nel=5.5,cp=13.0,ndf=50.0,adf=30.0,fat=2.8,ca=0.40,p=0.26,pricePerKg=3.50))
        list.add(Feed(name="Yonca Pelet",category=FeedCategories.ROUGHAGE_DRY,dm=90.0,me=10.0,nel=5.9,cp=18.0,rdp=13.0,rup=5.0,ndf=40.0,adf=30.0,fat=2.2,ca=1.55,p=0.26,pricePerKg=4.50))

        // EK TAHILLAR
        list.add(Feed(name="Misir Irmagi",category=FeedCategories.GRAIN,dm=88.0,me=14.0,nel=8.4,cp=8.5,ndf=8.0,fat=3.5,starch=70.0,ca=0.03,p=0.28,pricePerKg=7.00))
        list.add(Feed(name="Bugday Kepegi Peleti",category=FeedCategories.GRAIN,dm=88.0,me=10.5,nel=6.2,cp=16.5,ndf=40.0,fat=4.0,ca=0.12,p=1.20,mg=0.55,pricePerKg=6.50))
        list.add(Feed(name="Arpa Kepegi",category=FeedCategories.GRAIN,dm=88.0,me=10.0,nel=5.8,cp=14.0,ndf=38.0,fat=3.5,ca=0.10,p=0.80,pricePerKg=6.50))
        list.add(Feed(name="Yulaf Kepegi",category=FeedCategories.GRAIN,dm=90.0,me=10.5,nel=6.2,cp=15.0,ndf=35.0,fat=5.0,ca=0.10,p=0.75,pricePerKg=6.50))
        list.add(Feed(name="Cavdar Kepegi",category=FeedCategories.GRAIN,dm=88.0,me=10.0,nel=5.8,cp=15.5,ndf=40.0,fat=3.8,ca=0.10,p=0.80,pricePerKg=6.50))
        list.add(Feed(name="Misir Oz Yagi Posasi",category=FeedCategories.GRAIN,dm=90.0,me=13.0,nel=7.8,cp=25.0,ndf=32.0,fat=8.0,ca=0.08,p=0.70,pricePerKg=7.00))
        list.add(Feed(name="Bugday Oz",category=FeedCategories.GRAIN,dm=88.0,me=15.0,nel=9.0,cp=26.0,ndf=18.0,fat=8.5,ca=0.08,p=1.10,pricePerKg=7.00))
        list.add(Feed(name="Pirinc Kirigi",category=FeedCategories.GRAIN,dm=87.0,me=13.8,nel=8.3,cp=8.5,ndf=6.0,fat=2.0,starch=73.0,ca=0.04,p=0.28,pricePerKg=7.00))
        list.add(Feed(name="Dari",category=FeedCategories.GRAIN,dm=87.0,me=13.0,nel=7.8,cp=11.0,ndf=14.0,fat=3.8,starch=60.0,ca=0.04,p=0.32,pricePerKg=7.00))
        list.add(Feed(name="Kinoa",category=FeedCategories.GRAIN,dm=87.0,me=13.5,nel=8.1,cp=14.0,ndf=12.0,fat=5.5,starch=52.0,ca=0.08,p=0.40,pricePerKg=8.00))
        list.add(Feed(name="Bugday Kirmasi",category=FeedCategories.GRAIN,dm=87.0,me=13.2,nel=7.9,cp=13.0,ndf=14.0,fat=2.0,starch=62.0,ca=0.05,p=0.36,pricePerKg=7.00))
        list.add(Feed(name="Arpa Kirmasi",category=FeedCategories.GRAIN,dm=87.0,me=12.8,nel=7.6,cp=12.0,ndf=16.0,fat=2.2,starch=55.0,ca=0.05,p=0.33,pricePerKg=7.00))
        list.add(Feed(name="Misir Kirmasi",category=FeedCategories.GRAIN,dm=87.0,me=14.0,nel=8.4,cp=9.0,ndf=10.0,fat=3.8,starch=67.0,ca=0.03,p=0.30,pricePerKg=7.00))
        list.add(Feed(name="Karma Yem Inek",category=FeedCategories.GRAIN,dm=88.0,me=12.5,nel=7.5,cp=18.0,ndf=25.0,fat=4.0,ca=0.80,p=0.55,pricePerKg=8.00))
        list.add(Feed(name="Karma Yem Besi",category=FeedCategories.GRAIN,dm=88.0,me=13.0,nel=7.8,cp=16.0,ndf=22.0,fat=4.0,ca=0.70,p=0.50,pricePerKg=8.00))
        list.add(Feed(name="Karma Yem Koyun",category=FeedCategories.GRAIN,dm=88.0,me=12.0,nel=7.2,cp=17.0,ndf=24.0,fat=3.5,ca=0.75,p=0.50,pricePerKg=8.00))

        return list
    }
}