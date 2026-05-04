package com.nutripulse.app.ui.animal

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nutripulse.app.R
import com.nutripulse.app.data.AppDatabase
import com.nutripulse.app.data.model.AnimalCategory
import com.nutripulse.app.data.model.AnimalProfile
import com.nutripulse.app.data.model.AnimalSpecies
import com.nutripulse.app.databinding.FragmentAnimalListBinding
import com.nutripulse.app.ui.ration.RationFragment

class AnimalListFragment : Fragment() {

    companion object {
        private const val ARG_SELECTION_MODE = "selection_mode"

        fun newInstance(selectionMode: Boolean = false) = AnimalListFragment().apply {
            arguments = Bundle().apply { putBoolean(ARG_SELECTION_MODE, selectionMode) }
        }
    }

    private var _binding: FragmentAnimalListBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: AnimalAdapter
    private lateinit var quickAddAdapter: QuickTemplateAdapter
    private var allProfiles: List<AnimalProfile> = emptyList()
    private var activeSpecies = "ALL"
    private val filterViews = mutableListOf<TextView>()
    private var selectionMode = false

    private val quickTemplates = listOf(
        // Sigir (13)
        Triple("Sut Inegi - Erken Laktasyon", AnimalSpecies.SIGIR, AnimalCategory.SUT_INEGI_ERKEN),
        Triple("Sut Inegi - Orta Laktasyon", AnimalSpecies.SIGIR, AnimalCategory.SUT_INEGI_ORTA),
        Triple("Sut Inegi - Gec Laktasyon", AnimalSpecies.SIGIR, AnimalCategory.SUT_INEGI_GEC),
        Triple("Kuru Inek - Uzak Kuru", AnimalSpecies.SIGIR, AnimalCategory.KURU_INEK_UZAK),
        Triple("Kuru Inek - Yakin Kuru", AnimalSpecies.SIGIR, AnimalCategory.KURU_INEK_YAKIN),
        Triple("Besi - Baslangic", AnimalSpecies.SIGIR, AnimalCategory.BESI_BASLANGIC),
        Triple("Besi - Buyutme", AnimalSpecies.SIGIR, AnimalCategory.BESI_BUYUTME),
        Triple("Besi - Bitirme", AnimalSpecies.SIGIR, AnimalCategory.BESI_BITIRME),
        Triple("Duve 0-6 Ay", AnimalSpecies.SIGIR, AnimalCategory.DUVE_0_6),
        Triple("Duve 6-12 Ay", AnimalSpecies.SIGIR, AnimalCategory.DUVE_6_12),
        Triple("Duve 12-24 Ay", AnimalSpecies.SIGIR, AnimalCategory.DUVE_12_24),
        Triple("Buzagi - Emme Donemi", AnimalSpecies.SIGIR, AnimalCategory.BUZAGI),
        Triple("Dana - Sutten Kesim Sonrasi", AnimalSpecies.SIGIR, AnimalCategory.DANA),

        // Manda (2)
        Triple("Manda - Sut", AnimalSpecies.MANDA, AnimalCategory.MANDA_SUT),
        Triple("Manda - Besi", AnimalSpecies.MANDA, AnimalCategory.MANDA_BESI),

        // Koyun & Keci (10)
        Triple("Koyun - Sut", AnimalSpecies.KOYUN, AnimalCategory.KOYUN_SUT),
        Triple("Koyun - Besi", AnimalSpecies.KOYUN, AnimalCategory.KOYUN_BESI),
        Triple("Koyun - Gebe", AnimalSpecies.KOYUN, AnimalCategory.KOYUN_GEBE),
        Triple("Koyun - Emziren", AnimalSpecies.KOYUN, AnimalCategory.KOYUN_EMZIREN),
        Triple("Kuzu - Besi Baslangic", AnimalSpecies.KOYUN, AnimalCategory.KUZU_BESI_BASLANGIC),
        Triple("Kuzu - Besi Bitis", AnimalSpecies.KOYUN, AnimalCategory.KUZU_BESI_BITIS),
        Triple("Keci - Sut", AnimalSpecies.KECI, AnimalCategory.KECI_SUT),
        Triple("Keci - Besi", AnimalSpecies.KECI, AnimalCategory.KECI_BESI),
        Triple("Keci - Ankara (Tiftik)", AnimalSpecies.KECI, AnimalCategory.KECI_ANKARA),
        Triple("Oglak - Besi", AnimalSpecies.KECI, AnimalCategory.OGLAK_BESI),

        // Kanatlı (12)
        Triple("Broiler - Baslangic", AnimalSpecies.KANATLI, AnimalCategory.BROILER_BASLANGIC),
        Triple("Broiler - Buyutme", AnimalSpecies.KANATLI, AnimalCategory.BROILER_BUYUTME),
        Triple("Broiler - Bitirme", AnimalSpecies.KANATLI, AnimalCategory.BROILER_BITIRME),
        Triple("Yumurtaaci Pilic", AnimalSpecies.KANATLI, AnimalCategory.YUMURTACI_PILIC),
        Triple("Yumurtaaci Yum", AnimalSpecies.KANATLI, AnimalCategory.YUMURTACI_YUM),
        Triple("Yumurtaaci Yasli", AnimalSpecies.KANATLI, AnimalCategory.YUMURTACI_YASLI),
        Triple("Hindi - Besi", AnimalSpecies.KANATLI, AnimalCategory.HINDI_BESI),
        Triple("Hindi - Buyutme", AnimalSpecies.KANATLI, AnimalCategory.HINDI_BUYUTME),
        Triple("Bldrcn - Besi", AnimalSpecies.KANATLI, AnimalCategory.BILDIRCIN_BESI),
        Triple("Bldrcn - Yum", AnimalSpecies.KANATLI, AnimalCategory.BILDIRCIN_YUM),
        Triple("Ordek - Besi", AnimalSpecies.KANATLI, AnimalCategory.ORDEK_BESI),
        Triple("Kaz - Besi", AnimalSpecies.KANATLI, AnimalCategory.KAZ_BESI)
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAnimalListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        selectionMode = arguments?.getBoolean(ARG_SELECTION_MODE, false) ?: false

        setupRecycler()
        setupQuickTemplateGrid()
        buildFilters()
        observeProfiles()

        binding.btnAddAnimal.setOnClickListener {
            if (activeSpecies == "ALL") openInput()
            else openInput(activeSpecies)
        }
    }

    private fun setupRecycler() {
        val selectAnimal: (AnimalProfile) -> Unit = { profile ->
            parentFragmentManager.setFragmentResult(
                RationFragment.REQUEST_SELECT_ANIMAL,
                Bundle().apply { putInt(RationFragment.KEY_ANIMAL_ID, profile.id) }
            )
            parentFragmentManager.popBackStack()
        }

        adapter = AnimalAdapter(
            emptyList(),
            onEdit = { profile ->
                if (selectionMode) {
                    selectAnimal(profile)
                } else {
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, AnimalInputFragment.newInstance(profile.id))
                        .addToBackStack(null)
                        .commit()
                }
            },
            onRation = selectAnimal
        )
        binding.recyclerAnimals.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerAnimals.adapter = adapter
    }

    private fun setupQuickTemplateGrid() {
        quickAddAdapter = QuickTemplateAdapter { species, category ->
            openInput(species, category)
        }

        val spanCount = if (resources.configuration.screenWidthDp >= 600) 3 else 2
        binding.recyclerQuickAdd.layoutManager = GridLayoutManager(requireContext(), spanCount)
        binding.recyclerQuickAdd.setHasFixedSize(true)
        binding.recyclerQuickAdd.adapter = quickAddAdapter
    }

    private fun buildFilters() {
        val items = listOf(
            "TÜMÜ" to "ALL",
            "🐄 Sığır" to AnimalSpecies.SIGIR,
            "🐃 Manda" to AnimalSpecies.MANDA,
            "🐑 Koyun" to AnimalSpecies.KOYUN,
            "🐐 Keçi" to AnimalSpecies.KECI,
            "🐔 Kanatlı" to AnimalSpecies.KANATLI
        )
        items.forEachIndexed { idx, (label, code) ->
            val tv = TextView(requireContext()).apply {
                text = label; textSize = 10f
                val lp = ViewGroup.MarginLayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                lp.marginEnd = 6; layoutParams = lp; setPadding(20, 12, 20, 12)
                if (idx == 0) { setBackgroundResource(R.drawable.bg_filter_active); setTextColor(Color.parseColor("#05080A")) }
                else { setBackgroundResource(R.drawable.bg_filter_inactive); setTextColor(Color.parseColor("#00FF88")) }
                setOnClickListener {
                    activeSpecies = code
                    filterViews.forEach { v -> v.setBackgroundResource(R.drawable.bg_filter_inactive); v.setTextColor(Color.parseColor("#00FF88")) }
                    setBackgroundResource(R.drawable.bg_filter_active); setTextColor(Color.parseColor("#05080A"))
                    filterProfiles()
                }
            }
            filterViews.add(tv)
            binding.speciesFilterBar.addView(tv)
        }
    }

    private fun observeProfiles() {
        AppDatabase.getDatabase(requireContext()).animalProfileDao().getAll()
            .observe(viewLifecycleOwner) { profiles ->
                allProfiles = profiles
                binding.tvAnimalCount.text = "${profiles.size} hayvan grubu kayıtlı"
                filterProfiles()
            }
    }

    private fun filterProfiles() {
        val filtered = if (activeSpecies == "ALL") allProfiles
                       else allProfiles.filter { it.species == activeSpecies }
        adapter.updateProfiles(filtered)

        val isEmpty = filtered.isEmpty()
        binding.recyclerAnimals.visibility = if (isEmpty) View.GONE else View.VISIBLE
        binding.layoutEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE

        // Her zaman quick templates'i render et - sayacı guncellemek icin
        renderQuickTemplates()
        
        if (isEmpty) {
            updateEmptyTexts()
        }
    }

    private fun updateEmptyTexts() {
        if (activeSpecies == "ALL") {
            binding.tvEmptyTitle.text = "Henuz hayvan grubu eklenmemis"
            binding.tvEmptySubtitle.text = "Hayvan ekleyerek NRC ihtiyaclarini\nhesaplayabilirsiniz"
        } else {
            binding.tvEmptyTitle.text = "Bu tur icin kayitli grup bulunamadi"
            binding.tvEmptySubtitle.text = "Asagidaki hizli secimlerden birini\nsecip hemen grup ekleyin"
        }
    }

    private fun renderQuickTemplates() {
        val templates = if (activeSpecies == "ALL") quickTemplates
                        else quickTemplates.filter { it.second == activeSpecies }

        // Her kategori için mevcut profil sayısını bul
        val categoryCountMap = allProfiles.groupBy { it.category }
            .mapValues { it.value.size }
        
        val templatesWithCount = templates.map { (name, species, category) ->
            Triple(name, species, category) to (categoryCountMap[category] ?: 0)
        }
        
        quickAddAdapter.submitTemplates(templatesWithCount)
    }

    private fun openInput(species: String? = null, category: String? = null) {
        val fragment = if (species.isNullOrBlank()) {
            AnimalInputFragment.newInstance()
        } else {
            AnimalInputFragment.newInstance(species, category)
        }

        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private class QuickTemplateAdapter(
        private val onItemClick: (String, String) -> Unit
    ) : RecyclerView.Adapter<QuickTemplateAdapter.QuickTemplateViewHolder>() {

        private val items = mutableListOf<Pair<Triple<String, String, String>, Int>>()

        fun submitTemplates(newItems: List<Pair<Triple<String, String, String>, Int>>) {
            items.clear()
            items.addAll(newItems)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuickTemplateViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_quick_template, parent, false)
            return QuickTemplateViewHolder(view)
        }

        override fun onBindViewHolder(holder: QuickTemplateViewHolder, position: Int) {
            val item = items[position]
            val (triple, existingCount) = item
            val (label, species, category) = triple
            val suffix = if (existingCount > 0) " ✅($existingCount)" else " ➕"
            holder.bind(label + suffix)
            holder.itemView.setOnClickListener { onItemClick(species, category) }
            holder.itemView.setOnLongClickListener {
                // Long press = her zaman yeni profil ekle
                onItemClick(species, category)
                true
            }
        }

        override fun getItemCount() = items.size

        class QuickTemplateViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val textView: TextView = itemView.findViewById(R.id.tvQuickTemplate)

            fun bind(label: String) {
                textView.text = label
            }
        }
    }
}