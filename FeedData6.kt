package com.nutripulse.app.data

import com.nutripulse.app.data.model.Feed
import com.nutripulse.app.data.model.FeedCategories

object FeedData6 {
    fun getFeeds(): List<Feed> {
        val list = mutableListOf<Feed>()

        // SULU KABA - eksikler
        list.add(Feed(name="Ayicegi Silaji",category=FeedCategories.ROUGHAGE_WET,dm=22.0,me=9.0,nel=5.2,cp=12.0,ndf=42.0,adf=28.0,fat=3.5,ca=0.38,p=0.22,pricePerKg=3.00))
        list.add(Feed(name="Bezelye Silaji",category=FeedCategories.ROUGHAGE_WET,dm=25.0,me=10.5,nel=6.3,cp=16.0,ndf=40.0,adf=26.0,fat=2.8,ca=0.65,p=0.32,pricePerKg=3.50))
        list.add(Feed(name="Fig+Arpa Karisik Silaj",category=FeedCategories.ROUGHAGE_WET,dm=28.0,me=10.2,nel=6.1,cp=14.0,ndf=46.0,adf=28.0,fat=2.8,ca=0.42,p=0.26,pricePerKg=3.50))
        list.add(Feed(name="Taze Misir Bitkisi",category=FeedCategories.ROUGHAGE_WET,dm=20.0,me=10.0,nel=6.0,cp=8.5,ndf=50.0,adf=30.0,fat=2.5,starch=15.0,ca=0.22,p=0.18,pricePerKg=2.80))
        list.add(Feed(name="Taze Yonca",category=FeedCategories.ROUGHAGE_WET,dm=22.0,me=10.8,nel=6.5,cp=20.0,rdp=15.0,rup=5.0,ndf=36.0,adf=26.0,fat=3.0,ca=1.55,p=0.30,mg=0.28,k=2.50,pricePerKg=4.20))
        list.add(Feed(name="Taze Italyan Cadiri",category=FeedCategories.ROUGHAGE_WET,dm=18.0,me=10.5,nel=6.2,cp=14.0,ndf=42.0,adf=26.0,fat=3.2,ca=0.45,p=0.28,pricePerKg=3.50))
        list.add(Feed(name="Kolza Silaji",category=FeedCategories.ROUGHAGE_WET,dm=18.0,me=10.0,nel=6.0,cp=18.0,ndf=38.0,adf=24.0,fat=3.5,ca=0.75,p=0.35,pricePerKg=3.50))
        list.add(Feed(name="Taze Cadir Otu",category=FeedCategories.ROUGHAGE_WET,dm=20.0,me=10.2,nel=6.1,cp=13.0,ndf=44.0,adf=27.0,fat=3.0,ca=0.42,p=0.26,pricePerKg=3.50))

        // KURU KABA - eksikler
        list.add(Feed(name="Kuru Bezelye Otu",category=FeedCategories.ROUGHAGE_DRY,dm=87.0,me=9.8,nel=5.7,cp=16.0,ndf=40.0,adf=28.0,fat=2.5,ca=1.30,p=0.28,pricePerKg=3.50))
        list.add(Feed(name="Kolza Otu",category=FeedCategories.ROUGHAGE_DRY,dm=87.0,me=9.2,nel=5.4,cp=14.5,ndf=42.0,adf=30.0,fat=3.0,ca=1.10,p=0.30,pricePerKg=3.50))
        list.add(Feed(name="Nohut Otu",category=FeedCategories.ROUGHAGE_DRY,dm=88.0,me=9.5,nel=5.5,cp=15.0,ndf=42.0,adf=30.0,fat=2.2,ca=1.20,p=0.26,pricePerKg=3.50))
        list.add(Feed(name="Mercimek Otu",category=FeedCategories.ROUGHAGE_DRY,dm=87.0,me=9.8,nel=5.7,cp=17.0,ndf=40.0,adf=28.0,fat=2.5,ca=1.35,p=0.28,pricePerKg=3.50))
        list.add(Feed(name="Bakla Otu",category=FeedCategories.ROUGHAGE_DRY,dm=87.0,me=10.0,nel=5.9,cp=18.0,ndf=38.0,adf=26.0,fat=2.8,ca=1.40,p=0.30,pricePerKg=3.50))
        list.add(Feed(name="Macar Figi Otu",category=FeedCategories.ROUGHAGE_DRY,dm=87.0,me=9.8,nel=5.7,cp=16.5,ndf=42.0,adf=30.0,fat=2.2,ca=1.15,p=0.27,pricePerKg=3.50))
        list.add(Feed(name="Sorgum Kuru Otu",category=FeedCategories.ROUGHAGE_DRY,dm=88.0,me=8.5,nel=4.8,cp=7.5,ndf=60.0,adf=38.0,fat=2.0,ca=0.28,p=0.18,pricePerKg=3.00))
        list.add(Feed(name="Misir Gluten Yemi Silaji",category=FeedCategories.ROUGHAGE_DRY,dm=40.0,me=12.0,nel=7.2,cp=22.0,ndf=35.0,fat=3.5,ca=0.10,p=0.85,pricePerKg=4.00))
        list.add(Feed(name="Pamuk Tozu Kaba",category=FeedCategories.ROUGHAGE_DRY,dm=90.0,me=7.5,nel=4.0,cp=5.0,ndf=75.0,adf=55.0,fat=1.5,ca=0.22,p=0.06,pricePerKg=3.00))

        // TAHIL - eksikler
        list.add(Feed(name="Bezelye Dane",category=FeedCategories.GRAIN,dm=87.0,me=13.0,nel=7.8,cp=24.0,rdp=17.0,rup=7.0,ndf=15.0,adf=7.0,fat=1.5,starch=48.0,ca=0.10,p=0.45,lys=1.68,met=0.22,pricePerKg=8.00))
        list.add(Feed(name="Bakla Dane",category=FeedCategories.GRAIN,dm=87.0,me=13.0,nel=7.8,cp=28.0,rdp=20.0,rup=8.0,ndf=14.0,fat=1.5,starch=44.0,ca=0.12,p=0.48,lys=1.90,met=0.20,pricePerKg=8.00))
        list.add(Feed(name="Nohut Dane",category=FeedCategories.GRAIN,dm=87.0,me=13.0,nel=7.8,cp=22.0,ndf=16.0,fat=5.0,starch=42.0,ca=0.14,p=0.42,lys=1.52,met=0.28,pricePerKg=8.00))
        list.add(Feed(name="Mercimek Dane",category=FeedCategories.GRAIN,dm=87.0,me=13.0,nel=7.8,cp=25.0,ndf=12.0,fat=1.5,starch=48.0,ca=0.08,p=0.42,lys=1.75,met=0.22,pricePerKg=8.00))
        list.add(Feed(name="Lupen Dane",category=FeedCategories.GRAIN,dm=87.0,me=13.5,nel=8.1,cp=35.0,rdp=22.0,rup=13.0,ndf=18.0,fat=7.0,starch=8.0,ca=0.22,p=0.42,lys=1.82,met=0.22,pricePerKg=8.00))
        list.add(Feed(name="Keten Tohumu Tam",category=FeedCategories.GRAIN,dm=91.0,me=20.0,nel=12.0,cp=22.0,ndf=28.0,fat=38.0,ca=0.22,p=0.62,lys=0.95,met=0.48,maxDmPct=8.0,pricePerKg=12.00))
        list.add(Feed(name="Ayicegi Tam Tohum",category=FeedCategories.GRAIN,dm=93.0,me=18.5,nel=11.2,cp=17.0,ndf=30.0,fat=42.0,ca=0.28,p=0.68,maxDmPct=6.0,pricePerKg=12.00))
        list.add(Feed(name="Susam Tohumu",category=FeedCategories.GRAIN,dm=93.0,me=19.0,nel=11.5,cp=20.0,ndf=18.0,fat=48.0,ca=1.20,p=0.68,met=0.88,maxDmPct=5.0,pricePerKg=13.00))
        list.add(Feed(name="Kabak Cekirdegi Kuspesi",category=FeedCategories.GRAIN,dm=90.0,me=12.5,nel=7.5,cp=55.0,ndf=22.0,fat=8.0,ca=0.22,p=1.10,maxDmPct=8.0,pricePerKg=13.00))
        list.add(Feed(name="Pirinc Kepegi Peleti",category=FeedCategories.GRAIN,dm=91.0,me=11.5,nel=6.8,cp=14.0,ndf=25.0,fat=14.0,starch=28.0,ca=0.08,p=1.60,maxDmPct=10.0,pricePerKg=7.00))
        list.add(Feed(name="Bugday Unu 1Kalite",category=FeedCategories.GRAIN,dm=87.0,me=14.0,nel=8.4,cp=12.0,ndf=4.0,fat=1.5,starch=72.0,ca=0.04,p=0.30,pricePerKg=7.00))
        list.add(Feed(name="Misir Unu",category=FeedCategories.GRAIN,dm=87.0,me=14.0,nel=8.4,cp=9.0,ndf=8.0,fat=3.5,starch=70.0,ca=0.03,p=0.28,pricePerKg=7.00))

        return list
    }
}