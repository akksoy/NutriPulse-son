package com.nutripulse.app.data

import com.nutripulse.app.data.model.Feed
import com.nutripulse.app.data.model.FeedCategories

object FeedData2 {
    fun getFeeds(): List<Feed> {
        val list = mutableListOf<Feed>()

        // ══ PROTEİN KAYNAKLARI — tam değerler ══
        list.add(Feed(name="Soya Kuspesi 44",category=FeedCategories.PROTEIN,
            dm=89.0,me=13.2,nel=8.0,nem=7.4,neg=4.9,
            cp=44.0,rdp=28.0,rup=16.0,ndf=15.0,adf=10.0,adl=1.5,
            fat=2.5,ash=6.5,ca=0.30,p=0.65,mg=0.28,k=2.20,s=0.38,
            lys=2.90,met=0.62,thr=1.75,trp=0.58,maxDmPct=25.0,
            pricePerKg=13.00))

        list.add(Feed(name="Soya Kuspesi 48",category=FeedCategories.PROTEIN,
            dm=89.0,me=13.5,nel=8.2,nem=7.6,neg=5.1,
            cp=48.0,rdp=30.0,rup=18.0,ndf=10.0,adf=7.0,adl=1.0,
            fat=1.5,ash=6.5,ca=0.30,p=0.68,mg=0.28,k=2.30,s=0.40,
            lys=3.10,met=0.66,thr=1.88,trp=0.62,
            pricePerKg=13.50))

        list.add(Feed(name="Soya Tam Yag",category=FeedCategories.PROTEIN,
            dm=90.0,me=16.5,nel=10.0,nem=9.2,neg=6.1,
            cp=38.0,rdp=22.0,rup=16.0,ndf=12.0,fat=18.0,
            ca=0.27,p=0.60,mg=0.22,k=1.80,s=0.32,
            lys=2.50,met=0.53,thr=1.60,trp=0.52,maxDmPct=15.0,
            pricePerKg=15.00))

        list.add(Feed(name="Aycicek Kuspesi 36",category=FeedCategories.PROTEIN,
            dm=89.0,me=10.0,nel=5.8,nem=5.2,neg=3.2,
            cp=36.0,rdp=24.0,rup=12.0,ndf=38.0,adf=22.0,adl=5.0,
            fat=2.5,ash=6.8,ca=0.38,p=1.10,mg=0.42,k=1.22,s=0.48,
            lys=1.35,met=0.85,thr=1.32,trp=0.42,maxDmPct=20.0,
            pricePerKg=9.00))

        list.add(Feed(name="Aycicek Kuspesi 28",category=FeedCategories.PROTEIN,
            dm=89.0,me=8.5,nel=4.8,nem=4.2,neg=2.5,
            cp=28.0,rdp=18.0,rup=10.0,ndf=48.0,adf=30.0,adl=8.0,
            fat=2.0,ca=0.30,p=0.90,mg=0.35,k=1.00,s=0.38,
            lys=1.05,met=0.68,thr=1.02,trp=0.32,maxDmPct=15.0,
            pricePerKg=8.00))

        list.add(Feed(name="Pamuk Tohumu Kuspesi",category=FeedCategories.PROTEIN,
            dm=90.0,me=10.5,nel=6.2,nem=5.6,neg=3.5,
            cp=41.0,rdp=24.0,rup=17.0,ndf=28.0,adf=18.0,adl=4.0,
            fat=2.5,ca=0.20,p=1.10,mg=0.48,k=1.35,s=0.42,
            lys=1.68,met=0.57,thr=1.42,trp=0.48,maxDmPct=15.0,
            pricePerKg=10.00))

        list.add(Feed(name="Pamuk Tohumu Tam",category=FeedCategories.PROTEIN,
            dm=92.0,me=13.5,nel=8.0,nem=7.4,neg=4.9,
            cp=24.0,rdp=10.0,rup=14.0,ndf=45.0,adf=32.0,fat=18.0,
            ca=0.15,p=0.60,mg=0.32,k=1.05,s=0.30,
            lys=1.02,met=0.38,thr=0.88,trp=0.28,maxDmPct=12.0,
            pricePerKg=11.00))

        list.add(Feed(name="Kanola Kuspesi",category=FeedCategories.PROTEIN,
            dm=90.0,me=12.0,nel=7.2,nem=6.6,neg=4.3,
            cp=38.0,rdp=24.0,rup=14.0,ndf=28.0,adf=18.0,adl=5.0,
            fat=3.5,ca=0.65,p=1.05,mg=0.48,k=1.28,s=0.68,
            lys=2.10,met=0.72,thr=1.68,trp=0.45,maxDmPct=20.0,
            pricePerKg=10.00))

        list.add(Feed(name="Misir Gluteni 60",category=FeedCategories.PROTEIN,
            dm=90.0,me=14.5,nel=8.8,nem=8.2,neg=5.5,
            cp=62.0,rdp=18.0,rup=44.0,ndf=8.0,fat=2.5,
            ca=0.05,p=0.45,mg=0.12,k=0.22,s=0.48,
            lys=1.00,met=1.95,thr=2.20,trp=0.28,maxDmPct=10.0,
            pricePerKg=16.00))

        list.add(Feed(name="Balik Unu 65",category=FeedCategories.PROTEIN,
            dm=92.0,me=14.0,nel=8.5,nem=7.9,neg=5.3,
            cp=65.0,rdp=18.0,rup=47.0,ndf=0.0,fat=9.0,
            ca=3.50,p=2.50,mg=0.22,na=0.80,k=0.85,s=0.58,
            lys=5.10,met=1.85,thr=2.70,trp=0.72,
            maxDailyKg=1.0,maxDmPct=5.0,pricePerKg=40.00))

        list.add(Feed(name="Balik Unu 72",category=FeedCategories.PROTEIN,
            dm=92.0,me=15.0,nel=9.0,nem=8.4,neg=5.6,
            cp=72.0,rdp=16.0,rup=56.0,fat=10.0,
            ca=4.00,p=2.80,na=0.85,k=0.88,s=0.62,
            lys=5.60,met=2.10,thr=2.90,trp=0.80,
            maxDailyKg=0.8,maxDmPct=4.0,pricePerKg=45.00))

        list.add(Feed(name="Kan Unu",category=FeedCategories.PROTEIN,
            dm=92.0,me=13.5,nel=8.0,cp=88.0,rdp=20.0,rup=68.0,fat=1.5,
            ca=0.30,p=0.25,na=0.42,k=0.22,s=0.72,
            lys=8.50,met=1.10,thr=3.90,trp=1.20,
            maxDailyKg=0.5,maxDmPct=3.0,pricePerKg=25.00))

        list.add(Feed(name="Et Kemik Unu",category=FeedCategories.PROTEIN,
            dm=92.0,me=11.0,nel=6.5,cp=50.0,rdp=22.0,rup=28.0,fat=10.0,
            ca=9.50,p=4.80,na=0.70,k=0.42,s=0.48,
            lys=3.20,met=0.65,thr=1.80,trp=0.28,maxDmPct=5.0,pricePerKg=18.00))

        list.add(Feed(name="Tuy Unu Hidrolize",category=FeedCategories.PROTEIN,
            dm=93.0,me=12.5,nel=7.5,cp=80.0,rdp=15.0,rup=65.0,fat=5.0,
            ca=0.30,p=0.70,s=1.20,
            lys=1.80,met=0.55,thr=3.80,trp=0.50,maxDmPct=5.0,pricePerKg=12.00))

        list.add(Feed(name="Besleyici Maya",category=FeedCategories.PROTEIN,
            dm=93.0,me=12.0,nel=7.2,cp=48.0,rdp=35.0,rup=13.0,fat=2.0,
            ca=0.90,p=1.50,mg=0.22,k=1.80,s=0.42,
            lys=3.40,met=0.72,thr=2.50,trp=0.55,maxDmPct=3.0,pricePerKg=18.00))

        // ══ YAN ÜRÜNLER — tam değerler ══
        list.add(Feed(name="Bira Posasi Kuru",category=FeedCategories.BYPRODUCT,
            dm=92.0,me=11.0,nel=6.6,nem=6.0,neg=3.8,
            cp=28.0,rdp=15.0,rup=13.0,ndf=46.0,adf=22.0,adl=4.5,
            fat=7.0,ca=0.28,p=0.52,mg=0.18,k=0.08,s=0.38,
            lys=0.82,met=0.45,thr=0.88,trp=0.30,maxDmPct=20.0,
            pricePerKg=7.00))

        list.add(Feed(name="Bira Posasi Yas",category=FeedCategories.BYPRODUCT,
            dm=22.0,me=11.0,nel=6.6,nem=6.0,neg=3.8,
            cp=28.0,rdp=15.0,rup=13.0,ndf=46.0,fat=7.0,
            ca=0.28,p=0.52,mg=0.18,k=0.08,s=0.38,maxDailyKg=10.0,
            pricePerKg=2.00))

        list.add(Feed(name="Melas Seker Pancari",category=FeedCategories.BYPRODUCT,
            dm=75.0,me=12.8,nel=7.5,nem=6.8,neg=4.5,
            cp=6.8,rdp=6.0,ndf=0.0,fat=0.2,sugar=48.0,
            ca=0.18,p=0.04,mg=0.32,na=1.20,k=5.50,s=0.35,
            maxDailyKg=2.0,maxDmPct=8.0,pricePerKg=7.00))

        list.add(Feed(name="Melas Seker Kamisi",category=FeedCategories.BYPRODUCT,
            dm=75.0,me=12.5,nel=7.3,nem=6.6,neg=4.3,
            cp=4.5,sugar=48.0,ca=0.80,p=0.10,mg=0.38,na=0.18,k=3.80,s=0.42,
            maxDailyKg=2.0,pricePerKg=7.00))

        list.add(Feed(name="Seker Pancari Posasi Kuru",category=FeedCategories.BYPRODUCT,
            dm=91.0,me=12.5,nel=7.5,nem=6.8,neg=4.5,
            cp=9.5,rdp=7.0,rup=2.5,ndf=43.0,adf=22.0,adl=3.0,
            fat=1.0,sugar=4.0,ca=0.60,p=0.10,mg=0.15,k=0.80,s=0.20,
            maxDmPct=25.0,pricePerKg=6.50))

        list.add(Feed(name="Seker Pancari Posasi Yas",category=FeedCategories.BYPRODUCT,
            dm=12.0,me=12.5,nel=7.5,cp=9.5,ndf=43.0,fat=1.0,
            ca=0.60,p=0.10,mg=0.15,k=0.80,maxDailyKg=20.0,pricePerKg=1.80))

        list.add(Feed(name="Domates Posasi Kuru",category=FeedCategories.BYPRODUCT,
            dm=92.0,me=9.5,nel=5.5,nem=4.9,neg=2.8,
            cp=17.5,rdp=11.0,rup=6.5,ndf=52.0,adf=38.0,adl=12.0,
            fat=9.5,ca=0.42,p=0.32,mg=0.18,k=0.82,s=0.22,maxDmPct=10.0))

        list.add(Feed(name="Elma Posasi Kuru",category=FeedCategories.BYPRODUCT,
            dm=90.0,me=10.0,nel=5.8,nem=5.2,neg=3.0,
            cp=6.0,rdp=4.5,rup=1.5,ndf=40.0,adf=28.0,adl=8.0,
            fat=4.5,sugar=8.0,ca=0.12,p=0.12,k=0.52,s=0.12,maxDmPct=10.0,
            pricePerKg=6.00))

        list.add(Feed(name="Zeytin Posasi",category=FeedCategories.BYPRODUCT,
            dm=88.0,me=8.0,nel=4.5,cp=5.5,rdp=3.5,rup=2.0,
            ndf=50.0,adf=35.0,adl=15.0,fat=8.0,
            ca=0.45,p=0.06,mg=0.05,k=0.35,s=0.08,maxDmPct=8.0,
            pricePerKg=5.50))

        list.add(Feed(name="Citrus Posasi Kuru",category=FeedCategories.BYPRODUCT,
            dm=91.0,me=12.0,nel=7.2,nem=6.5,neg=4.2,
            cp=7.0,rdp=5.0,rup=2.0,ndf=25.0,adf=18.0,adl=2.5,
            fat=3.5,sugar=16.0,ca=1.80,p=0.12,mg=0.12,k=0.85,s=0.12,maxDmPct=15.0,
            pricePerKg=6.50))

        list.add(Feed(name="Misir DDG",category=FeedCategories.BYPRODUCT,
            dm=90.0,me=13.5,nel=8.0,nem=7.4,neg=4.9,
            cp=29.0,rdp=12.0,rup=17.0,ndf=38.0,adf=12.0,adl=3.0,
            fat=9.5,ca=0.10,p=0.75,mg=0.28,k=0.92,s=0.42,
            lys=0.72,met=0.52,thr=0.92,trp=0.20,maxDmPct=20.0,
            pricePerKg=8.50))

        list.add(Feed(name="Patates Posasi",category=FeedCategories.BYPRODUCT,
            dm=12.0,me=12.0,nel=7.0,cp=6.0,rdp=4.5,rup=1.5,
            ndf=20.0,fat=0.5,starch=50.0,ca=0.06,p=0.14,k=1.80,s=0.08,
            maxDailyKg=15.0,pricePerKg=2.50))

        list.add(Feed(name="Ekmek Mayasi",category=FeedCategories.BYPRODUCT,
            dm=93.0,me=12.5,nel=7.5,cp=50.0,rdp=38.0,fat=2.0,
            ca=0.55,p=1.45,mg=0.22,k=1.80,s=0.38,
            lys=3.20,met=0.68,thr=2.40,trp=0.52,maxDmPct=3.0,pricePerKg=8.00))

        list.add(Feed(name="Peyniraltı Suyu Tozu",category=FeedCategories.BYPRODUCT,
            dm=93.0,me=14.0,nel=8.5,cp=13.0,rdp=10.0,fat=1.0,sugar=72.0,
            ca=0.80,p=0.70,na=0.50,k=1.60,s=0.22,maxDmPct=10.0,pricePerKg=8.00))

        // Türkiye Yem Fabrikası - Süt İneği Rasyonu İçin Yaygın Kullanılan Hammaddeler
        list.add(Feed(name="DDGS Mısır",category=FeedCategories.BYPRODUCT,
            dm=90.0,me=13.8,nel=8.2,nem=7.6,neg=5.0,
            cp=27.0,rdp=11.0,rup=16.0,ndf=40.0,adf=11.0,adl=2.8,
            fat=10.0,starch=5.0,ca=0.08,p=0.72,mg=0.25,k=0.85,s=0.40,
            lys=0.65,met=0.48,thr=0.85,trp=0.18,maxDmPct=20.0,
            notes="Mısır Distillers Dried Grains with Solubles - Nişasta fabrikası yan ürünü"))

        list.add(Feed(name="Mısır Gluten Yemi",category=FeedCategories.BYPRODUCT,
            dm=90.0,me=11.5,nel=6.8,nem=6.2,neg=4.0,
            cp=21.0,rdp=9.0,rup=12.0,ndf=45.0,adf=10.0,adl=2.5,
            fat=3.5,starch=15.0,ca=0.12,p=0.65,mg=0.22,k=0.75,s=0.35,
            lys=0.55,met=0.42,thr=0.72,trp=0.15,maxDmPct=25.0,
            notes="Corn Gluten Feed - Nişasta fabrikası yan ürünü, yüksek NDF"))

        list.add(Feed(name="Mısır Gluteni",category=FeedCategories.BYPRODUCT,
            dm=90.0,me=14.0,nel=8.5,nem=7.8,neg=5.2,
            cp=60.0,rdp=15.0,rup=45.0,ndf=18.0,adf=5.0,adl=1.0,
            fat=4.0,starch=12.0,ca=0.08,p=0.45,mg=0.18,k=0.55,s=0.32,
            lys=1.80,met=1.50,thr=2.20,trp=0.55,maxDmPct=15.0,
            notes="Corn Gluten Meal - Yüksek proteinli mısır nişasta yan ürünü"))

        list.add(Feed(name="Soya Kabuğu",category=FeedCategories.BYPRODUCT,
            dm=91.0,me=9.5,nel=5.5,nem=4.9,neg=3.0,
            cp=12.0,rdp=6.0,rup=6.0,ndf=68.0,adf=45.0,adl=8.0,
            fat=2.0,ca=0.45,p=0.18,mg=0.28,k=1.10,s=0.22,
            maxDmPct=15.0,
            notes="Soybean Hulls - Soya işleme yan ürünü, yüksek lif",
            pricePerKg=7.50))

        list.add(Feed(name="Yüksek Nişastalı Buğday Kepeği",category=FeedCategories.BYPRODUCT,
            dm=88.0,me=11.8,nel=7.0,nem=6.4,neg=4.2,
            cp=14.0,rdp=8.0,rup=6.0,ndf=35.0,adf=12.0,adl=3.5,
            fat=4.5,starch=28.0,ca=0.15,p=0.95,mg=0.45,k=1.30,s=0.35,
            maxDmPct=20.0,
            notes="High Starch Wheat Bran - Buğday öğütme yan ürünü, yüksek nişasta",
            pricePerKg=7.00))

        return list
    }
}