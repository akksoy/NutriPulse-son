package com.nutripulse.app.ui.animal

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nutripulse.app.data.model.AnimalProfile
import com.nutripulse.app.data.model.AnimalSpecies
import com.nutripulse.app.databinding.ItemAnimalCardBinding

class AnimalAdapter(
    private var profiles: List<AnimalProfile>,
    private val onEdit: (AnimalProfile) -> Unit,
    private val onRation: (AnimalProfile) -> Unit
) : RecyclerView.Adapter<AnimalAdapter.AnimalVH>() {

    inner class AnimalVH(val b: ItemAnimalCardBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AnimalVH {
        val b = ItemAnimalCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AnimalVH(b)
    }

    override fun onBindViewHolder(holder: AnimalVH, position: Int) {
        val p = profiles[position]
        val b = holder.b

        // Emoji + isim
        b.tvAnimalEmoji.text = speciesEmoji(p.species)
        b.tvProfileName.text = p.profileName.ifEmpty { categoryDisplayName(p.category) }
        b.tvCategory.text = categoryDisplayName(p.category)
        b.tvAnimalCount2.text = "${p.animalCount} baş"
        b.tvBodyWeight.text = "${p.bodyWeight} kg"

        // NRC ihtiyaçları
        b.tvReqDm.text = if (p.reqDmKg > 0) "${p.reqDmKg} kg" else "Hesapla"
        b.tvReqNel.text = if (p.reqNelMj > 0) "${p.reqNelMj} MJ" else "-"
        b.tvReqCp.text = if (p.reqCpG > 0) "${p.reqCpG.toInt()} g" else "-"
        b.tvReqCa.text = if (p.reqCaG > 0) "${p.reqCaG.toInt()} g" else "-"

        // Renk
        val color = speciesColor(p.species)
        try {
            b.tvCategory.setTextColor(Color.parseColor(color))
        } catch (_: Exception) {}

        b.root.setOnClickListener { onEdit(p) }
        b.btnCreateRation.setOnClickListener { onRation(p) }
    }

    override fun getItemCount() = profiles.size

    fun updateProfiles(list: List<AnimalProfile>) {
        profiles = list
        notifyDataSetChanged()
    }

    private fun speciesEmoji(s: String) = when (s) {
        AnimalSpecies.SIGIR   -> "🐄"
        AnimalSpecies.MANDA   -> "🐃"
        AnimalSpecies.KOYUN   -> "🐑"
        AnimalSpecies.KECI    -> "🐐"
        AnimalSpecies.KANATLI -> "🐔"
        else -> "🐄"
    }

    private fun speciesColor(s: String) = when (s) {
        AnimalSpecies.SIGIR   -> "#00FF88"
        AnimalSpecies.MANDA   -> "#00D4FF"
        AnimalSpecies.KOYUN   -> "#FFC400"
        AnimalSpecies.KECI    -> "#FF7A00"
        AnimalSpecies.KANATLI -> "#FF6B6B"
        else -> "#00FF88"
    }

    private fun categoryDisplayName(cat: String): String = when (cat) {
        "SUT_INEGI_ERKEN" -> "Sut Inegi - Erken Laktasyon"
        "SUT_INEGI_ORTA"  -> "Sut Inegi - Orta Laktasyon"
        "SUT_INEGI_GEC"   -> "Sut Inegi - Gec Laktasyon"
        "KURU_INEK_UZAK"  -> "Kuru Inek - Uzak Kuru"
        "KURU_INEK_YAKIN" -> "Kuru Inek - Yakin Kuru (Gecis)"
        "BESI_BASLANGIC"  -> "Besi - Baslangic"
        "BESI_BUYUTME"    -> "Besi - Buyutme"
        "BESI_BITIRME"    -> "Besi - Bitirme"
        "DUVE_0_6"        -> "Duve 0-6 Ay"
        "DUVE_6_12"       -> "Duve 6-12 Ay"
        "DUVE_12_24"      -> "Duve 12-24 Ay"
        "BUZAGI"          -> "Buzagi - Emme Donemi"
        "DANA"            -> "Dana - Sutten Kesim Sonrasi"

        "MANDA_SUT"       -> "Manda - Sut"
        "MANDA_BESI"      -> "Manda - Besi"

        "KOYUN_SUT"       -> "Koyun - Sut"
        "KOYUN_BESI"      -> "Koyun - Besi"
        "KOYUN_GEBE"      -> "Koyun - Gebe"
        "KOYUN_EMZIREN"   -> "Koyun - Emziren"
        "KUZU_BESI"       -> "Kuzu - Besi"
        "KUZU_BESI_BASLANGIC" -> "Kuzu - Besi Baslangic"
        "KUZU_BESI_BITIS" -> "Kuzu - Besi Bitis"
        "KECI_SUT"        -> "Keci - Sut"
        "KECI_BESI"       -> "Keci - Besi"
        "KECI_ANKARA"     -> "Keci - Ankara (Tiftik)"
        "OGLAK_BESI"      -> "Oglak - Besi"

        "BROILER_BASLANGIC" -> "Etlik Pilic - Baslatma"
        "BROILER_BUYUTME" -> "Etlik Pilic - Buyutme"
        "BROILER_BITIRME" -> "Etlik Pilic - Bitirme"
        "YUMURTACI_PILIC" -> "Yumurta Tavugu - Pilic"
        "YUMURTACI_YUM"   -> "Yumurta Tavugu - Yumurtlama"
        "YUMURTACI_YASLI" -> "Yumurta Tavugu - Yasli"
        "HINDI_BUYUTME"   -> "Hindi - Buyutme"
        "HINDI_BESI"      -> "Hindi - Besi"
        "BILDIRCIN_YUM"   -> "Bildircin - Yumurta"
        "BILDIRCIN_BESI"  -> "Bildircin - Besi"
        "ORDEK_BESI"      -> "Ordek - Besi"
        "KAZ_BESI"        -> "Kaz - Besi"

        "ALABALIK_YAVRU"   -> "Alabalik - Yavru"
        "ALABALIK_BUYUTME" -> "Alabalik - Buyutme"
        "ALABALIK_PAZAR"   -> "Alabalik - Pazar Boyu"
        "LEVREK_YAVRU"     -> "Levrek - Yavru"
        "LEVREK_BUYUTME"   -> "Levrek - Buyutme"
        "CIPURA_YAVRU"     -> "Cipura - Yavru"
        "CIPURA_BUYUTME"   -> "Cipura - Buyutme"
        "SAZAN_BUYUTME"    -> "Sazan - Buyutme"

        "AT_HAFIF"        -> "At - Hafif Calisma"
        "AT_ORTA"         -> "At - Orta Calisma"
        "AT_AGIR"         -> "At - Agir Calisma"
        "ESEK_KATIR_CALISMA" -> "Esek/Katir - Calisma"
        "ESEK_KATIR_DINLENME" -> "Esek/Katir - Dinlenme"

        "TAVSAN_BUYUTME"  -> "Tavsan - Buyutme"
        "TAVSAN_GEBE"     -> "Tavsan - Gebe"
        "TAVSAN_EMZIREN"  -> "Tavsan - Emziren"
        else -> cat
    }
}