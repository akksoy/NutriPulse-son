package com.nutripulse.app.ui.animal

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.nutripulse.app.R
import com.nutripulse.app.data.AppDatabase
import com.nutripulse.app.data.NrcCalculator
import com.nutripulse.app.data.model.AnimalCategory
import com.nutripulse.app.data.model.AnimalProfile
import com.nutripulse.app.data.model.AnimalSpecies
import com.nutripulse.app.databinding.FragmentAnimalInputBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AnimalInputFragment : Fragment() {

    private var _binding: FragmentAnimalInputBinding? = null
    private val binding get() = _binding!!
    private var editProfileId = -1
    private var currentSpecies = AnimalSpecies.SIGIR
    private var currentCategory = AnimalCategory.SUT_INEGI_ORTA
    private var initialSpeciesArg: String? = null
    private var initialCategoryArg: String? = null
    private var userEditedIdentity = false
    private var applyingIdentityDefaults = false
    private var pendingCategorySelection: String? = null

    companion object {
        fun newInstance() = AnimalInputFragment()
        fun newInstance(profileId: Int) = AnimalInputFragment().apply {
            arguments = Bundle().apply { putInt("profile_id", profileId) }
        }

        fun newInstance(species: String, category: String? = null) = AnimalInputFragment().apply {
            arguments = Bundle().apply {
                putString("pref_species", species)
                if (!category.isNullOrBlank()) putString("pref_category", category)
            }
        }
    }

    // Tür → Kategoriler eşlemesi
    private val speciesCategories = mapOf(
        AnimalSpecies.SIGIR to listOf(
            "Sut Inegi - Erken Laktasyon" to AnimalCategory.SUT_INEGI_ERKEN,
            "Sut Inegi - Orta Laktasyon"  to AnimalCategory.SUT_INEGI_ORTA,
            "Sut Inegi - Gec Laktasyon"   to AnimalCategory.SUT_INEGI_GEC,
            "Kuru Inek - Uzak Kuru"       to AnimalCategory.KURU_INEK_UZAK,
            "Kuru Inek - Yakin Kuru"      to AnimalCategory.KURU_INEK_YAKIN,
            "Besi - Baslangic"            to AnimalCategory.BESI_BASLANGIC,
            "Besi - Buyutme"              to AnimalCategory.BESI_BUYUTME,
            "Besi - Bitirme"              to AnimalCategory.BESI_BITIRME,
            "Duve 0-6 Ay"                 to AnimalCategory.DUVE_0_6,
            "Duve 6-12 Ay"                to AnimalCategory.DUVE_6_12,
            "Duve 12-24 Ay"               to AnimalCategory.DUVE_12_24,
            "Buzagi - Emme Donemi"        to AnimalCategory.BUZAGI,
            "Dana - Sutten Kesim Sonrasi" to AnimalCategory.DANA
        ),
        AnimalSpecies.MANDA to listOf(
            "Manda - Sut" to AnimalCategory.MANDA_SUT,
            "Manda - Besi" to AnimalCategory.MANDA_BESI
        ),
        AnimalSpecies.KOYUN to listOf(
            "Koyun - Sut"             to AnimalCategory.KOYUN_SUT,
            "Koyun - Besi"            to AnimalCategory.KOYUN_BESI,
            "Koyun - Gebe"            to AnimalCategory.KOYUN_GEBE,
            "Koyun - Emziren"         to AnimalCategory.KOYUN_EMZIREN,
            "Kuzu - Besi Baslangic"   to AnimalCategory.KUZU_BESI_BASLANGIC,
            "Kuzu - Besi Bitis"       to AnimalCategory.KUZU_BESI_BITIS
        ),
        AnimalSpecies.KECI to listOf(
            "Keci - Sut"              to AnimalCategory.KECI_SUT,
            "Keci - Besi"             to AnimalCategory.KECI_BESI,
            "Keci - Ankara (Tiftik)"  to AnimalCategory.KECI_ANKARA,
            "Oglak - Besi"            to AnimalCategory.OGLAK_BESI
        ),
        AnimalSpecies.KANATLI to listOf(
            "Broiler - Baslangic"    to AnimalCategory.BROILER_BASLANGIC,
            "Broiler - Buyutme"      to AnimalCategory.BROILER_BUYUTME,
            "Broiler - Bitirme"      to AnimalCategory.BROILER_BITIRME,
            "Yumurtaaci Pilic"        to AnimalCategory.YUMURTACI_PILIC,
            "Yumurtaaci Yum"          to AnimalCategory.YUMURTACI_YUM,
            "Yumurtaaci Yasli"       to AnimalCategory.YUMURTACI_YASLI,
            "Hindi - Besi"            to AnimalCategory.HINDI_BESI,
            "Hindi - Buyutme"         to AnimalCategory.HINDI_BUYUTME,
            "Bldrcn - Besi"           to AnimalCategory.BILDIRCIN_BESI,
            "Bldrcn - Yum"            to AnimalCategory.BILDIRCIN_YUM,
            "Ordek - Besi"            to AnimalCategory.ORDEK_BESI,
            "Kaz - Besi"              to AnimalCategory.KAZ_BESI
        )
    )

    private val speciesList = listOf(
        "🐄 Sigir" to AnimalSpecies.SIGIR,
        "🐃 Manda" to AnimalSpecies.MANDA,
        "🐑 Koyun" to AnimalSpecies.KOYUN,
        "🐐 Keci"  to AnimalSpecies.KECI,
        "🐔 Kanatli" to AnimalSpecies.KANATLI
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAnimalInputBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        editProfileId = arguments?.getInt("profile_id", -1) ?: -1
        initialSpeciesArg = arguments?.getString("pref_species")
        initialCategoryArg = arguments?.getString("pref_category")
        pendingCategorySelection = initialCategoryArg

        setupSpeciesSpinner()
        setupIdentityWatchers()
        setupButtons()
        updateFieldVisibility()

        if (editProfileId != -1) {
            binding.tvTitle.text = "HAYVAN DUZENLE"
            binding.btnDelete.visibility = View.VISIBLE
            loadProfile(editProfileId)
        } else {
            applyInitialSelectionFromArgs()
        }
    }

    private fun setupSpeciesSpinner() {
        val labels = speciesList.map { it.first }
        val spAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, labels)
        spAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerSpecies.adapter = spAdapter

        if (speciesList.isNotEmpty()) {
            currentSpecies = speciesList.first().second
            setupCategorySpinner()
            updateFieldVisibility()
        }

        binding.spinnerSpecies.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                currentSpecies = speciesList[pos].second
                setupCategorySpinner()
                updateFieldVisibility()
                applySubtypeIdentityDefaultsIfNeeded()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupCategorySpinner() {
        val cats = speciesCategories[currentSpecies] ?: return
        val labels = cats.map { it.first }
        val spAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, labels)
        spAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCategory.adapter = spAdapter

        binding.spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                currentCategory = cats[pos].second
                updateFieldVisibility()
                applySubtypeIdentityDefaultsIfNeeded()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        val preferred = pendingCategorySelection
        val preferredIdx = if (preferred.isNullOrBlank()) -1 else cats.indexOfFirst { it.second == preferred }
        if (preferredIdx >= 0) {
            binding.spinnerCategory.setSelection(preferredIdx, false)
            currentCategory = cats[preferredIdx].second
            pendingCategorySelection = null
        } else {
            currentCategory = cats[0].second
            binding.spinnerCategory.setSelection(0, false)
        }

        updateFieldVisibility()
        applySubtypeIdentityDefaultsIfNeeded()
    }

    private fun setupIdentityWatchers() {
        binding.etProfileName.doAfterTextChanged {
            if (editProfileId == -1 && !applyingIdentityDefaults) userEditedIdentity = true
        }
        binding.etBreed.doAfterTextChanged {
            if (editProfileId == -1 && !applyingIdentityDefaults) userEditedIdentity = true
        }
    }

    private fun applySubtypeIdentityDefaultsIfNeeded() {
        // Yeni kayitta secilen tur/kategori neyse ad ve irk her zaman onunla senkron kalir.
        if (editProfileId != -1) return

        val categoryName = categoryDisplayName(currentCategory)
        val breedName = defaultBreedFor(currentCategory, currentSpecies)

        applyingIdentityDefaults = true
        binding.etProfileName.setText("$categoryName Grup 1")
        binding.etBreed.setText(breedName)
        applyingIdentityDefaults = false
    }

    private fun categoryDisplayName(categoryCode: String): String {
        return speciesCategories.values
            .flatten()
            .firstOrNull { it.second == categoryCode }
            ?.first
            ?: categoryCode
    }

    private fun defaultBreedFor(categoryCode: String, species: String): String {
        val byCategory = mapOf(
            // Sigir
            AnimalCategory.SUT_INEGI_ERKEN to "Holstein",
            AnimalCategory.SUT_INEGI_ORTA to "Holstein",
            AnimalCategory.SUT_INEGI_GEC to "Holstein",
            AnimalCategory.KURU_INEK_UZAK to "Holstein",
            AnimalCategory.KURU_INEK_YAKIN to "Holstein",
            AnimalCategory.BESI_BASLANGIC to "Simental",
            AnimalCategory.BESI_BUYUTME to "Simental",
            AnimalCategory.BESI_BITIRME to "Simental",
            AnimalCategory.DUVE_0_6 to "Holstein",
            AnimalCategory.DUVE_6_12 to "Holstein",
            AnimalCategory.DUVE_12_24 to "Holstein",
            AnimalCategory.BUZAGI to "Holstein",
            AnimalCategory.DANA to "Simental",

            // Manda
            AnimalCategory.MANDA_SUT to "Anadolu Mandasi",
            AnimalCategory.MANDA_BESI to "Anadolu Mandasi",

            // Koyun/Keci
            AnimalCategory.KOYUN_SUT to "Sakiz",
            AnimalCategory.KOYUN_BESI to "Akkaraman",
            AnimalCategory.KOYUN_GEBE to "Merinos",
            AnimalCategory.KOYUN_EMZIREN to "Merinos",
            AnimalCategory.KUZU_BESI_BASLANGIC to "Akkaraman",
            AnimalCategory.KUZU_BESI_BITIS to "Akkaraman",
            AnimalCategory.KECI_SUT to "Saanen",
            AnimalCategory.KECI_BESI to "Kilis Kecisi",
            AnimalCategory.KECI_ANKARA to "Ankara Kecisi",
            AnimalCategory.OGLAK_BESI to "Kilis Kecisi"
        )

        return byCategory[categoryCode] ?: when (species) {
            AnimalSpecies.SIGIR -> "Holstein"
            AnimalSpecies.MANDA -> "Anadolu Mandasi"
            AnimalSpecies.KOYUN -> "Merinos"
            AnimalSpecies.KECI -> "Saanen"
            // AnimalSpecies.KANATLI -> "Ross 308"
            // AnimalSpecies.SU -> "Balik"
            // AnimalSpecies.AT -> "Arap Ati"
            // AnimalSpecies.TAVSAN -> "Yeni Zelanda"
            else -> ""
        }
    }

    // Kategoriye gore hangi alanlar gosterilsin
    private fun updateFieldVisibility() {
        val dairyCattleCategories = setOf(
            AnimalCategory.SUT_INEGI_ERKEN,
            AnimalCategory.SUT_INEGI_ORTA,
            AnimalCategory.SUT_INEGI_GEC,
            AnimalCategory.MANDA_SUT
        )
        val smallRuminantMilkCategories = setOf(
            AnimalCategory.KOYUN_SUT,
            AnimalCategory.KOYUN_EMZIREN,
            AnimalCategory.KECI_SUT,
            // AnimalCategory.TAVSAN_EMZIREN
        )
        val eggCategories = setOf(
            AnimalCategory.YUMURTACI_PILIC,
            AnimalCategory.YUMURTACI_YUM,
            AnimalCategory.YUMURTACI_YASLI,
            AnimalCategory.BILDIRCIN_YUM
        )
        val pregnancyCategories = setOf(
            AnimalCategory.KOYUN_GEBE,
            AnimalCategory.KURU_INEK_YAKIN,
            // AnimalCategory.TAVSAN_GEBE
        )
        val gainCategories = setOf(
            AnimalCategory.BESI_BASLANGIC,
            AnimalCategory.BESI_BUYUTME,
            AnimalCategory.BESI_BITIRME,
            AnimalCategory.MANDA_BESI,
            AnimalCategory.KOYUN_BESI,
            AnimalCategory.KUZU_BESI,
            AnimalCategory.KUZU_BESI_BASLANGIC,
            AnimalCategory.KUZU_BESI_BITIS,
            AnimalCategory.KECI_BESI,
            AnimalCategory.OGLAK_BESI,
            AnimalCategory.BROILER_BASLANGIC,
            AnimalCategory.BROILER_BUYUTME,
            AnimalCategory.BROILER_BITIRME,
            AnimalCategory.HINDI_BUYUTME,
            AnimalCategory.HINDI_BESI,
            AnimalCategory.BILDIRCIN_BESI,
            AnimalCategory.ORDEK_BESI,
            AnimalCategory.KAZ_BESI,
            // AnimalCategory.ALABALIK_YAVRU,
            // AnimalCategory.ALABALIK_BUYUTME,
            // AnimalCategory.ALABALIK_PAZAR,
            // AnimalCategory.LEVREK_YAVRU,
            // AnimalCategory.LEVREK_BUYUTME,
            // AnimalCategory.CIPURA_YAVRU,
            // AnimalCategory.CIPURA_BUYUTME,
            // AnimalCategory.SAZAN_BUYUTME
        )

        val showMilkYield = currentCategory in dairyCattleCategories ||
            currentCategory in smallRuminantMilkCategories ||
            currentCategory in eggCategories
        val showMilkFat = currentCategory in dairyCattleCategories
        val showMilkProtein = currentCategory in dairyCattleCategories || currentCategory in smallRuminantMilkCategories
        val showLactWeek = currentCategory in dairyCattleCategories
        val showLactNum = currentCategory in dairyCattleCategories
        val showPregnancy = currentCategory in pregnancyCategories
        val showDailyGain = currentCategory in gainCategories
        val showAgeDays = false // currentSpecies == AnimalSpecies.KANATLI
        val showWaterTemp = false // currentSpecies == AnimalSpecies.SU
        val showBcs = currentSpecies == AnimalSpecies.SIGIR || currentSpecies == AnimalSpecies.MANDA

        binding.layoutMilk.visibility = if (showMilkYield) View.VISIBLE else View.GONE
        binding.layoutMilkFat.visibility = if (showMilkFat) View.VISIBLE else View.GONE
        binding.layoutMilkProtein.visibility = if (showMilkProtein) View.VISIBLE else View.GONE
        binding.layoutLactWeek.visibility = if (showLactWeek) View.VISIBLE else View.GONE
        binding.layoutLactNum.visibility = if (showLactNum) View.VISIBLE else View.GONE
        binding.layoutPregnancy.visibility = if (showPregnancy) View.VISIBLE else View.GONE
        binding.layoutDailyGain.visibility = if (showDailyGain) View.VISIBLE else View.GONE
        binding.layoutAgeDays.visibility = if (showAgeDays) View.VISIBLE else View.GONE
        binding.layoutWaterTemp.visibility = if (showWaterTemp) View.VISIBLE else View.GONE
        binding.layoutBcs.visibility = if (showBcs) View.VISIBLE else View.GONE

        if (currentCategory in eggCategories) {
            binding.tvMilkYieldLabel.text = "Yumurta Sayisi (adet/gun)"
            binding.etMilkYield.hint = "85"
        } else {
            binding.tvMilkYieldLabel.text = "Sut Verimi (L/gun)"
            binding.etMilkYield.hint = "25"
        }

        binding.tvDailyGainLabel.text = "Hedef GCAA (g/gun)"
        binding.etDailyGain.hint = "1200"
    }

    private fun setupButtons() {
        binding.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }

        binding.btnCalculate.setOnClickListener {
            val profile = buildProfile()
            val result = NrcCalculator.calculate(profile)
            showNrcResult(result)
        }

        binding.btnSave.setOnClickListener {
            val profile = buildProfileWithNrc()
            saveProfile(profile)
        }

        binding.btnDelete.setOnClickListener {
            deleteProfile()
        }
    }

    private fun readDoubleFrom(container: View, text: CharSequence, defaultVisible: Double = 0.0, hiddenValue: Double = 0.0): Double {
        return if (container.visibility == View.VISIBLE) text.toString().toDoubleOrNull() ?: defaultVisible else hiddenValue
    }

    private fun readIntFrom(container: View, text: CharSequence, defaultVisible: Int = 0, hiddenValue: Int = 0): Int {
        return if (container.visibility == View.VISIBLE) text.toString().toIntOrNull() ?: defaultVisible else hiddenValue
    }

    private fun buildProfile(): AnimalProfile {
        return AnimalProfile(
            id = if (editProfileId != -1) editProfileId else 0,
            profileName = binding.etProfileName.text.toString().trim(),
            species = currentSpecies,
            category = currentCategory,
            breed = binding.etBreed.text.toString().trim(),
            animalCount = binding.etAnimalCount.text.toString().toIntOrNull() ?: 1,
            bodyWeight = binding.etBodyWeight.text.toString().toDoubleOrNull() ?: 0.0,
            bcs = readDoubleFrom(binding.layoutBcs, binding.etBcs.text, defaultVisible = 3.0),
            milkYield = readDoubleFrom(binding.layoutMilk, binding.etMilkYield.text),
            milkFat = readDoubleFrom(binding.layoutMilkFat, binding.etMilkFat.text, defaultVisible = 3.5),
            milkProtein = readDoubleFrom(binding.layoutMilkProtein, binding.etMilkProtein.text, defaultVisible = 3.2),
            lactationWeek = readIntFrom(binding.layoutLactWeek, binding.etLactWeek.text),
            lactationNumber = readIntFrom(binding.layoutLactNum, binding.etLactNum.text, defaultVisible = 1),
            pregnancyMonth = readIntFrom(binding.layoutPregnancy, binding.etPregnancy.text),
            targetDailyGain = readDoubleFrom(binding.layoutDailyGain, binding.etDailyGain.text),
            ageDay = readIntFrom(binding.layoutAgeDays, binding.etAgeDays.text),
            waterTemp = readDoubleFrom(binding.layoutWaterTemp, binding.etWaterTemp.text, defaultVisible = 15.0, hiddenValue = 15.0),
            notes = binding.etNotes.text.toString()
        )
    }

    private fun buildProfileWithNrc(): AnimalProfile {
        val base = buildProfile()
        val nrc = NrcCalculator.calculate(base)
        return base.copy(
            reqDmKg = nrc.dmKg, reqMeMj = nrc.meMj, reqNelMj = nrc.nelMj,
            reqNemMj = nrc.nemMj, reqNegMj = nrc.negMj,
            reqCpG = nrc.cpG, reqRdpG = nrc.rdpG, reqRupG = nrc.rupG,
            reqNdfPct = nrc.ndfPct,
            reqCaG = nrc.caG, reqPG = nrc.pG, reqMgG = nrc.mgG,
            reqNaG = nrc.naG, reqKG = nrc.kG,
            reqLysG = nrc.lysG, reqMetG = nrc.metG,
            reqThrG = nrc.thrG, reqTrpG = nrc.trpG, reqSG = nrc.sG
        )
    }

    private fun showNrcResult(r: NrcCalculator.NrcResult, speciesForUnit: String = currentSpecies) {
        binding.layoutNrcResult.visibility = View.VISIBLE

        val isPoultry = false // speciesForUnit == AnimalSpecies.KANATLI
        val meUnit = if (isPoultry) "kcal/gun" else "MJ/gun"

        binding.tvNrcDm.text   = "${r.dmKg} kg/gun"
        binding.tvNrcMe.text   = "${r.meMj} $meUnit"
        binding.tvNrcNel.text  = if (r.nelMj > 0) "${r.nelMj} MJ" else "-"
        binding.tvNrcNem.text  = if (r.nemMj > 0) "${r.nemMj} MJ" else "-"
        binding.tvNrcNeg.text  = if (r.negMj > 0) "${r.negMj} MJ" else "-"
        binding.tvNrcCp.text   = "%.1f g/gun".format(r.cpG)
        binding.tvNrcRdp.text  = if (r.rdpG > 0) "%.1f g".format(r.rdpG) else "-"
        binding.tvNrcRup.text  = if (r.rupG > 0) "%.1f g".format(r.rupG) else "-"
        binding.tvNrcCa.text   = "%.2f g/gun".format(r.caG)
        binding.tvNrcP.text    = "%.2f g/gun".format(r.pG)
        binding.tvNrcMg.text   = "%.2f g/gun".format(r.mgG)
        binding.tvNrcNa.text   = "%.2f g/gun".format(r.naG)
        binding.tvNrcK.text    = "%.2f g/gun".format(r.kG)
        binding.tvNrcLys.text  = if (r.lysG > 0) "%.1f g".format(r.lysG) else "-"
        binding.tvNrcMet.text  = if (r.metG > 0) "%.1f g".format(r.metG) else "-"
        binding.tvNrcThr.text  = if (r.thrG > 0) "%.1f g".format(r.thrG) else "-"
        binding.tvNrcTrp.text  = if (r.trpG > 0) "%.1f g".format(r.trpG) else "-"
        binding.tvNrcS.text    = if (r.sG > 0) "%.1f g/gun".format(r.sG) else "-"
        binding.tvNrcSource.text = r.notes
    }

    private fun saveProfile(profile: AnimalProfile) {
        if (profile.bodyWeight <= 0) {
            Toast.makeText(requireContext(), "Canli agirlik girin!", Toast.LENGTH_SHORT).show()
            return
        }
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val db = AppDatabase.getDatabase(requireContext())
                if (editProfileId != -1) db.animalProfileDao().update(profile)
                else db.animalProfileDao().insert(profile)
            }
            Toast.makeText(requireContext(),
                if (editProfileId != -1) "Hayvan guncellendi!" else "Hayvan eklendi!",
                Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
        }
    }

    private fun deleteProfile() {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(requireContext())
            val profile = withContext(Dispatchers.IO) { db.animalProfileDao().getById(editProfileId) }
            profile?.let {
                withContext(Dispatchers.IO) { db.animalProfileDao().delete(it) }
                Toast.makeText(requireContext(), "Hayvan silindi!", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            }
        }
    }

    private fun loadProfile(id: Int) {
        lifecycleScope.launch {
            val p = withContext(Dispatchers.IO) {
                AppDatabase.getDatabase(requireContext()).animalProfileDao().getById(id)
            }
            p?.let { fillForm(it) }
        }
    }

    private fun fillForm(p: AnimalProfile) {
        userEditedIdentity = true
        pendingCategorySelection = p.category

        binding.etProfileName.setText(p.profileName)
        binding.etBreed.setText(p.breed)
        binding.etAnimalCount.setText(p.animalCount.toString())
        binding.etBodyWeight.setText(p.bodyWeight.toString())
        binding.etBcs.setText(if (p.bcs > 0) p.bcs.toString() else "")
        binding.etMilkYield.setText(if (p.milkYield > 0) p.milkYield.toString() else "")
        binding.etMilkFat.setText(if (p.milkFat > 0) p.milkFat.toString() else "")
        binding.etMilkProtein.setText(if (p.milkProtein > 0) p.milkProtein.toString() else "")
        binding.etLactWeek.setText(if (p.lactationWeek > 0) p.lactationWeek.toString() else "")
        binding.etLactNum.setText(if (p.lactationNumber > 0) p.lactationNumber.toString() else "")
        binding.etPregnancy.setText(if (p.pregnancyMonth > 0) p.pregnancyMonth.toString() else "")
        binding.etDailyGain.setText(if (p.targetDailyGain > 0) p.targetDailyGain.toString() else "")
        binding.etAgeDays.setText(if (p.ageDay > 0) p.ageDay.toString() else "")
        binding.etWaterTemp.setText(if (p.waterTemp > 0) p.waterTemp.toString() else "")
        binding.etNotes.setText(p.notes)

        // Spinner seçimlerini ayarla
        val spIdx = speciesList.indexOfFirst { it.second == p.species }
        if (spIdx >= 0) binding.spinnerSpecies.setSelection(spIdx)

        // setupCategorySpinner now consumes pendingCategorySelection deterministically.

        // Eğer eski profil yüklenirse ve Thr/Trp/S değerleri boşsa, otomatik recalculate et
        val profileToShow = p // if (p.reqDmKg > 0 && p.reqThrG == 0.0 && p.species == AnimalSpecies.KANATLI) { ... }

        if (profileToShow.reqDmKg > 0) showNrcResult(
            NrcCalculator.NrcResult(
                dmKg = profileToShow.reqDmKg,
                meMj = profileToShow.reqMeMj,
                nelMj = profileToShow.reqNelMj,
                nemMj = profileToShow.reqNemMj,
                negMj = profileToShow.reqNegMj,
                cpG = profileToShow.reqCpG,
                rdpG = profileToShow.reqRdpG,
                rupG = profileToShow.reqRupG,
                ndfPct = profileToShow.reqNdfPct,
                caG = profileToShow.reqCaG,
                pG = profileToShow.reqPG,
                mgG = profileToShow.reqMgG,
                naG = profileToShow.reqNaG,
                kG = profileToShow.reqKG,
                lysG = profileToShow.reqLysG,
                metG = profileToShow.reqMetG,
                thrG = profileToShow.reqThrG,
                trpG = profileToShow.reqTrpG,
                sG = profileToShow.reqSG
            ),
            speciesForUnit = p.species
        )
    }

    private fun applyInitialSelectionFromArgs() {
        val prefSpecies = initialSpeciesArg
        val prefCategory = initialCategoryArg
        pendingCategorySelection = prefCategory
        userEditedIdentity = false

        if (prefSpecies.isNullOrBlank()) {
            updateFieldVisibility()
            applySubtypeIdentityDefaultsIfNeeded()
            return
        }

        val spIdx = speciesList.indexOfFirst { it.second == prefSpecies }
        if (spIdx >= 0) {
            binding.spinnerSpecies.setSelection(spIdx)
        } else {
            updateFieldVisibility()
            applySubtypeIdentityDefaultsIfNeeded()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}