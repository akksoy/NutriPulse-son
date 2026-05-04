package com.nutripulse.app.data

import com.nutripulse.app.data.model.Feed
import com.nutripulse.app.data.model.FeedCategories

object FeedData7 {
    fun getFeeds(): List<Feed> {
        val list = mutableListOf<Feed>()

        // YAN URUN - Turkiye spesifik
        list.add(Feed(name="Findik Kabugu",category=FeedCategories.BYPRODUCT,dm=90.0,me=5.5,nel=2.8,cp=3.0,ndf=80.0,adf=58.0,fat=1.5,ca=0.18,p=0.05,maxDmPct=5.0,pricePerKg=4.00))
        list.add(Feed(name="Incir Posasi",category=FeedCategories.BYPRODUCT,dm=88.0,me=10.5,nel=6.2,cp=5.5,ndf=30.0,fat=2.5,sugar=45.0,ca=0.48,p=0.12,maxDmPct=8.0,pricePerKg=7.00))
        list.add(Feed(name="Kayisi Cekirdegi Kuspesi",category=FeedCategories.BYPRODUCT,dm=90.0,me=11.0,nel=6.5,cp=30.0,ndf=28.0,fat=6.0,ca=0.32,p=0.70,maxDmPct=8.0,pricePerKg=7.00))
        list.add(Feed(name="Antep Fistigi Kuspesi",category=FeedCategories.BYPRODUCT,dm=92.0,me=12.0,nel=7.2,cp=28.0,ndf=22.0,fat=18.0,ca=0.28,p=0.55,maxDmPct=6.0,pricePerKg=8.00))
        list.add(Feed(name="Cay Posasi Kuru",category=FeedCategories.BYPRODUCT,dm=90.0,me=7.5,nel=4.0,cp=22.0,ndf=48.0,fat=4.5,ca=0.45,p=0.35,maxDmPct=5.0,pricePerKg=7.00))
        list.add(Feed(name="Seker Pancari Posasi Melas",category=FeedCategories.BYPRODUCT,dm=88.0,me=13.0,nel=7.8,cp=10.0,ndf=38.0,fat=1.0,sugar=12.0,ca=0.65,p=0.10,maxDmPct=20.0,pricePerKg=7.00))
        list.add(Feed(name="Limon Kabugu Kuru",category=FeedCategories.BYPRODUCT,dm=91.0,me=10.5,nel=6.2,cp=7.0,ndf=22.0,fat=2.5,sugar=20.0,ca=2.20,p=0.12,maxDmPct=8.0,pricePerKg=7.00))
        list.add(Feed(name="Portakal Kabugu Kuru",category=FeedCategories.BYPRODUCT,dm=91.0,me=10.5,nel=6.2,cp=6.5,ndf=24.0,fat=2.0,sugar=18.0,ca=1.95,p=0.10,maxDmPct=8.0,pricePerKg=7.00))
        list.add(Feed(name="Misir Gluten Kek",category=FeedCategories.BYPRODUCT,dm=90.0,me=12.5,nel=7.5,cp=22.0,ndf=35.0,fat=4.0,starch=18.0,ca=0.10,p=0.82,maxDmPct=20.0,pricePerKg=7.00))
        list.add(Feed(name="Sarap Posasi Kuru",category=FeedCategories.BYPRODUCT,dm=92.0,me=7.5,nel=4.0,cp=12.0,ndf=55.0,adf=42.0,fat=4.5,ca=0.85,p=0.35,maxDmPct=5.0,pricePerKg=7.00))
        list.add(Feed(name="Zeytin Yapragi",category=FeedCategories.BYPRODUCT,dm=88.0,me=7.0,nel=3.8,cp=8.0,ndf=50.0,adf=35.0,fat=5.5,ca=1.20,p=0.08,maxDmPct=5.0,pricePerKg=7.00))
        list.add(Feed(name="Pamuk Tohumu Kabugu",category=FeedCategories.BYPRODUCT,dm=91.0,me=6.5,nel=3.2,cp=4.5,ndf=78.0,adf=62.0,fat=1.5,ca=0.12,p=0.08,maxDmPct=10.0,pricePerKg=4.00))
        list.add(Feed(name="Soya Okra",category=FeedCategories.BYPRODUCT,dm=90.0,me=11.5,nel=6.8,cp=44.0,ndf=10.0,fat=2.5,ca=0.30,p=0.65,maxDmPct=20.0,pricePerKg=8.00))
        list.add(Feed(name="Pirinc Kavuzu",category=FeedCategories.BYPRODUCT,dm=91.0,me=5.0,nel=2.5,cp=3.5,ndf=72.0,adf=52.0,fat=1.0,ca=0.10,p=0.07,maxDmPct=5.0,pricePerKg=4.00))
        list.add(Feed(name="Mantar Posasi",category=FeedCategories.BYPRODUCT,dm=15.0,me=9.5,nel=5.5,cp=18.0,ndf=38.0,fat=2.5,ca=0.30,p=0.45,maxDailyKg=3.0,pricePerKg=7.00))
        list.add(Feed(name="Tavuk Gubresi Islenmis Peleti",category=FeedCategories.BYPRODUCT,dm=88.0,me=7.5,nel=4.0,cp=28.0,ndf=22.0,fat=2.5,ca=3.20,p=2.10,maxDmPct=8.0,pricePerKg=4.00))
        list.add(Feed(name="Tuz Ruhu Kalsiyum",category=FeedCategories.BYPRODUCT,dm=35.0,me=9.5,nel=5.5,cp=8.0,sugar=55.0,ca=0.15,p=0.12,maxDailyKg=5.0,pricePerKg=4.00))

        // PROTEIN - Turkiye spesifik
        list.add(Feed(name="Yer Fistigi Tam",category=FeedCategories.PROTEIN,dm=93.0,me=18.0,nel=11.0,cp=28.0,ndf=14.0,fat=48.0,ca=0.10,p=0.45,lys=1.05,met=0.30,maxDmPct=6.0,pricePerKg=13.00))
        list.add(Feed(name="Aspir Kuspesi Dusuk Yag",category=FeedCategories.PROTEIN,dm=91.0,me=9.0,nel=5.2,cp=40.0,ndf=38.0,fat=1.5,ca=0.32,p=1.15,lys=1.50,met=0.70,maxDmPct=12.0,pricePerKg=12.00))
        list.add(Feed(name="Kolza Tam Tohum",category=FeedCategories.PROTEIN,dm=93.0,me=20.0,nel=12.2,cp=21.0,ndf=20.0,fat=42.0,ca=0.38,p=0.68,maxDmPct=6.0,pricePerKg=18.00))
        list.add(Feed(name="Soya Izolati",category=FeedCategories.PROTEIN,dm=92.0,me=14.5,nel=8.8,cp=88.0,rdp=22.0,rup=66.0,ndf=4.0,fat=0.5,ca=0.28,p=0.72,lys=5.80,met=1.20,maxDmPct=8.0,pricePerKg=16.00))
        list.add(Feed(name="Balik Cozunur Protein",category=FeedCategories.PROTEIN,dm=50.0,me=12.5,nel=7.5,cp=60.0,rdp=40.0,rup=20.0,fat=5.0,ca=1.20,p=1.00,lys=4.20,met=1.50,maxDailyKg=0.5,pricePerKg=40.00))
        list.add(Feed(name="Kanatli Yan Urun Unu",category=FeedCategories.PROTEIN,dm=93.0,me=12.0,nel=7.2,cp=60.0,rdp=15.0,rup=45.0,fat=12.0,ca=4.50,p=2.20,lys=3.20,met=1.10,maxDmPct=5.0,pricePerKg=18.00))

        // MINERAL & KATKI - Turkiye spesifik
        list.add(Feed(name="Deniz Suyu Minerali",category=FeedCategories.MINERAL,dm=95.0,na=30.0,mg=3.5,k=1.2,maxDailyKg=0.10,pricePerKg=8.00))
        list.add(Feed(name="Kayrak Tasi",category=FeedCategories.MINERAL,dm=99.0,ca=35.0,maxDmPct=1.5,pricePerKg=2.00))
        list.add(Feed(name="Kirectas Unu",category=FeedCategories.MINERAL,dm=99.0,ca=38.0,maxDmPct=2.0,pricePerKg=2.00))
        list.add(Feed(name="Istiridye Kabugu",category=FeedCategories.MINERAL,dm=99.0,ca=36.0,maxDmPct=2.0,pricePerKg=2.00))
        list.add(Feed(name="Fosfat Kaya",category=FeedCategories.MINERAL,dm=99.0,ca=32.0,p=14.0,maxDmPct=1.0,pricePerKg=10.00))
        list.add(Feed(name="Potas Klorur",category=FeedCategories.MINERAL,dm=99.0,k=52.0,maxDailyKg=0.08,pricePerKg=12.00))
        list.add(Feed(name="Amonyum Sulfat",category=FeedCategories.MINERAL,dm=99.0,na=0.0,s=24.0,maxDailyKg=0.05,pricePerKg=10.00))
        list.add(Feed(name="Sut Kuru Tam Yag",category=FeedCategories.MINERAL,dm=97.0,cp=26.0,fat=27.0,ca=0.90,p=0.72,maxDmPct=5.0,pricePerKg=30.00))
        list.add(Feed(name="Skim Milk Tozu",category=FeedCategories.MINERAL,dm=96.0,cp=35.0,fat=1.0,ca=1.30,p=1.05,na=0.55,maxDmPct=8.0,pricePerKg=30.00))
        list.add(Feed(name="Peynir Alti Suyu Kuru",category=FeedCategories.MINERAL,dm=93.0,cp=13.0,fat=1.0,sugar=72.0,ca=0.80,p=0.70,maxDmPct=8.0,pricePerKg=8.00))

        // SU URUNLERI YEMLERI - ek
        list.add(Feed(name="Ahtapot Unu",category=FeedCategories.AQUA,dm=92.0,me=13.0,nel=7.8,cp=72.0,fat=5.0,ca=1.50,p=1.20,lys=5.80,met=2.10,maxDmPct=5.0,pricePerKg=40.00))
        list.add(Feed(name="Murekkep Baligi Unu",category=FeedCategories.AQUA,dm=92.0,me=13.5,nel=8.1,cp=68.0,fat=6.0,ca=1.60,p=1.30,maxDmPct=5.0,pricePerKg=40.00))
        list.add(Feed(name="Denizyosunu Unu",category=FeedCategories.AQUA,dm=90.0,me=9.0,nel=5.2,cp=25.0,fat=4.0,ca=3.50,p=0.55,maxDmPct=5.0,pricePerKg=30.00))
        list.add(Feed(name="Kalamar Unu",category=FeedCategories.AQUA,dm=92.0,me=13.0,nel=7.8,cp=65.0,fat=7.0,ca=1.80,p=1.40,maxDmPct=4.0,pricePerKg=40.00))
        list.add(Feed(name="Sazan Buyutme Yemi",category=FeedCategories.AQUA,dm=91.0,me=14.0,cp=32.0,fat=8.0,ca=1.20,p=0.90,pricePerKg=30.00))
        list.add(Feed(name="Yayin Baligi Yemi",category=FeedCategories.AQUA,dm=92.0,me=15.5,cp=42.0,fat=10.0,ca=1.35,p=1.05,pricePerKg=30.00))
        list.add(Feed(name="Akdeniz Levrek Yemi",category=FeedCategories.AQUA,dm=93.0,me=17.5,cp=50.0,fat=15.0,ca=1.55,p=1.25,lys=3.50,met=1.35,pricePerKg=35.00))
        list.add(Feed(name="Goknar Alabalik Yemi",category=FeedCategories.AQUA,dm=93.0,me=17.0,cp=46.0,fat=17.0,ca=1.45,p=1.15,pricePerKg=35.00))

        return list
    }
}