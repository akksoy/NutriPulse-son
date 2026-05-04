package com.nutripulse.app.ui.feed

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.nutripulse.app.R
import com.nutripulse.app.data.AppDatabase
import com.nutripulse.app.data.model.Feed
import com.nutripulse.app.data.model.FeedCategories
import com.nutripulse.app.databinding.FragmentAddFeedBinding
import com.nutripulse.app.databinding.ItemInputRowBinding
import com.nutripulse.app.databinding.ItemSpinnerRowBinding
import com.nutripulse.app.utils.PriceFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AddEditFeedFragment : Fragment() {

    private var _binding: FragmentAddFeedBinding? = null
    private val binding get() = _binding!!

    private var editFeedId: Int = -1
    private var existingFeed: Feed? = null

    companion object {
        const val ARG_FEED_ID = "edit_feed_id"

        // Yeni yem eklemek icin
        fun newInstance() = AddEditFeedFragment()

        // Var olan yemi duzenlemek icin
        fun newInstance(feedId: Int) = AddEditFeedFragment().apply {
            arguments = Bundle().apply { putInt(ARG_FEED_ID, feedId) }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddFeedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        editFeedId = arguments?.getInt(ARG_FEED_ID, -1) ?: -1

        setupLabels()
        setupCategorySpinner()
        setupButtons()

        if (editFeedId != -1) {
            // Duzenle modu
            binding.tvTitle.text = "YEM DUZENLE"
            binding.btnDelete.visibility = View.VISIBLE
            loadFeedForEdit(editFeedId)
        }

        // Fiyat tarihi goster
        val today = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date())
        binding.tvPriceDate.text = today
    }

    private fun setupLabels() {
        // Temel
        setInputLabel(binding.inputName, "Yem Adi", "")
        setInputLabel(binding.inputDm, "Kuru Madde (KM)", "%")

        // Fiyat
        setInputLabel(binding.inputPrice, "Fiyat", "TL/kg")

        // Enerji
        setInputLabel(binding.inputMe, "ME", "MJ/kg")
        setInputLabel(binding.inputNel, "NEL", "MJ/kg")
        setInputLabel(binding.inputNem, "NEM", "MJ/kg")
        setInputLabel(binding.inputNeg, "NEG", "MJ/kg")

        // Protein
        setInputLabel(binding.inputCp, "Ham Protein (HP)", "%KM")
        setInputLabel(binding.inputRdp, "RDP", "%KM")
        setInputLabel(binding.inputRup, "RUP", "%KM")

        // Lif
        setInputLabel(binding.inputNdf, "NDF", "%KM")
        setInputLabel(binding.inputAdf, "ADF", "%KM")
        setInputLabel(binding.inputFat, "Ham Yag", "%KM")
        setInputLabel(binding.inputStarch, "Nisasta", "%KM")

        // Mineral
        setInputLabel(binding.inputCa, "Kalsiyum (Ca)", "%KM")
        setInputLabel(binding.inputP, "Fosfor (P)", "%KM")
        setInputLabel(binding.inputMg, "Magnezyum (Mg)", "%KM")
        setInputLabel(binding.inputNa, "Sodyum (Na)", "%KM")

        // Amino asit
        setInputLabel(binding.inputLys, "Lizin (Lys)", "%KM")
        setInputLabel(binding.inputMet, "Metiyonin (Met)", "%KM")
        setInputLabel(binding.inputThr, "Treonin (Thr)", "%KM")
        setInputLabel(binding.inputTrp, "Triptofan (Trp)", "%KM")

        // Kisit
        setInputLabel(binding.inputMaxKg, "Maks Gunluk", "kg/gun")
        setInputLabel(binding.inputMaxPct, "Maks KM Orani", "%KM")

        // Name input klavye tipi
        binding.inputName.etInputValue.inputType = android.text.InputType.TYPE_CLASS_TEXT
        binding.inputName.etInputValue.hint = "Yem adini giriniz"
    }

    private fun setInputLabel(row: ItemInputRowBinding, label: String, unit: String) {
        row.tvInputLabel.text = label
        row.tvInputUnit.text = unit
    }

    private fun setupCategorySpinner() {
        val categories = listOf(
            "Sulu Kaba Yemler",
            "Kuru Kaba Yemler",
            "Tahillar & Kesif",
            "Protein Kaynaklari",
            "Yan Urunler",
            "Yag Kaynaklari",
            "Mineral & Vitamin",
            "Katki Maddeleri",
            "Su Urunleri Yemleri"
        )
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            categories
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        binding.spinnerCategory.tvSpinnerLabel.text = "Kategori"
        binding.spinnerCategory.spinnerValue.adapter = adapter
    }

    private fun setupButtons() {
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.btnSave.setOnClickListener {
            saveFeed()
        }

        binding.btnDelete.setOnClickListener {
            deleteFeed()
        }
    }

    private fun loadFeedForEdit(feedId: Int) {
        lifecycleScope.launch {
            val feed = withContext(Dispatchers.IO) {
                AppDatabase.getDatabase(requireContext()).feedDao().getFeedById(feedId)
            }
            feed?.let {
                existingFeed = it
                fillForm(it)
            }
        }
    }

    private fun fillForm(feed: Feed) {
        binding.inputName.etInputValue.setText(feed.name)
        setVal(binding.inputDm, feed.dm)
        setVal(binding.inputPrice, feed.pricePerKg)
        setVal(binding.inputMe, feed.me)
        setVal(binding.inputNel, feed.nel)
        setVal(binding.inputNem, feed.nem)
        setVal(binding.inputNeg, feed.neg)
        setVal(binding.inputCp, feed.cp)
        setVal(binding.inputRdp, feed.rdp)
        setVal(binding.inputRup, feed.rup)
        setVal(binding.inputNdf, feed.ndf)
        setVal(binding.inputAdf, feed.adf)
        setVal(binding.inputFat, feed.fat)
        setVal(binding.inputStarch, feed.starch)
        setVal(binding.inputCa, feed.ca)
        setVal(binding.inputP, feed.p)
        setVal(binding.inputMg, feed.mg)
        setVal(binding.inputNa, feed.na)
        setVal(binding.inputLys, feed.lys)
        setVal(binding.inputMet, feed.met)
        setVal(binding.inputThr, feed.thr)
        setVal(binding.inputTrp, feed.trp)
        setVal(binding.inputMaxKg, feed.maxDailyKg)
        setVal(binding.inputMaxPct, feed.maxDmPct)
        if (feed.notes.isNotEmpty()) binding.etNotes.setText(feed.notes)

        // Kategori spinner'i ayarla
        val catIndex = when (feed.category) {
            FeedCategories.ROUGHAGE_WET -> 0
            FeedCategories.ROUGHAGE_DRY -> 1
            FeedCategories.GRAIN        -> 2
            FeedCategories.PROTEIN      -> 3
            FeedCategories.BYPRODUCT    -> 4
            FeedCategories.FAT          -> 5
            FeedCategories.MINERAL      -> 6
            FeedCategories.ADDITIVE     -> 7
            FeedCategories.AQUA         -> 8
            else -> 0
        }
        binding.spinnerCategory.spinnerValue.setSelection(catIndex)
    }

    private fun setVal(row: ItemInputRowBinding, value: Double) {
        if (value != 0.0) {
            // Fiyat alanı formatlı göster
            if (row == binding.inputPrice) {
                row.etInputValue.setText(PriceFormatter.formatPrice(value))
            } else {
                row.etInputValue.setText(value.toString())
            }
        }
    }

    private fun saveFeed() {
        val name = binding.inputName.etInputValue.text.toString().trim()
        if (name.isEmpty()) {
            Toast.makeText(requireContext(), "Yem adi bos olamaz!", Toast.LENGTH_SHORT).show()
            return
        }

        val catIndex = binding.spinnerCategory.spinnerValue.selectedItemPosition
        val category = listOf(
            FeedCategories.ROUGHAGE_WET, FeedCategories.ROUGHAGE_DRY,
            FeedCategories.GRAIN, FeedCategories.PROTEIN, FeedCategories.BYPRODUCT,
            FeedCategories.FAT, FeedCategories.MINERAL, FeedCategories.ADDITIVE,
            FeedCategories.AQUA
        )[catIndex]

        val feed = Feed(
            id = if (editFeedId != -1) editFeedId else 0,
            name = name,
            category = category,
            isCustom = true,
            dm       = getDouble(binding.inputDm),
            pricePerKg = getPrice(binding.inputPrice),  // Fiyat özel işleme
            me       = getDouble(binding.inputMe),
            nel      = getDouble(binding.inputNel),
            nem      = getDouble(binding.inputNem),
            neg      = getDouble(binding.inputNeg),
            cp       = getDouble(binding.inputCp),
            rdp      = getDouble(binding.inputRdp),
            rup      = getDouble(binding.inputRup),
            ndf      = getDouble(binding.inputNdf),
            adf      = getDouble(binding.inputAdf),
            fat      = getDouble(binding.inputFat),
            starch   = getDouble(binding.inputStarch),
            ca       = getDouble(binding.inputCa),
            p        = getDouble(binding.inputP),
            mg       = getDouble(binding.inputMg),
            na       = getDouble(binding.inputNa),
            lys      = getDouble(binding.inputLys),
            met      = getDouble(binding.inputMet),
            thr      = getDouble(binding.inputThr),
            trp      = getDouble(binding.inputTrp),
            maxDailyKg = getDouble(binding.inputMaxKg),
            maxDmPct   = getDouble(binding.inputMaxPct),
            notes    = binding.etNotes.text.toString()
        )

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val db = AppDatabase.getDatabase(requireContext())
                if (editFeedId != -1) db.feedDao().updateFeed(feed)
                else db.feedDao().insertFeed(feed)
            }
            Toast.makeText(requireContext(),
                if (editFeedId != -1) "Yem guncellendi!" else "Yem eklendi!",
                Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
        }
    }

    private fun deleteFeed() {
        existingFeed?.let { feed ->
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    AppDatabase.getDatabase(requireContext()).feedDao().deleteFeed(feed)
                }
                Toast.makeText(requireContext(), "Yem silindi!", Toast.LENGTH_SHORT).show()
                // 2 kez geri git (detay -> liste)
                parentFragmentManager.popBackStack()
                parentFragmentManager.popBackStack()
            }
        }
    }

    private fun getDouble(row: ItemInputRowBinding): Double {
        return row.etInputValue.text.toString().toDoubleOrNull() ?: 0.0
    }

    private fun getPrice(row: ItemInputRowBinding): Double {
        // Fiyat girişini parse et (virgül veya nokta kabul et)
        val price = PriceFormatter.parsePrice(row.etInputValue.text.toString())
        // 1 ondalık basamağa yuvarla
        return if (price > 0) kotlin.math.round(price * 10) / 10 else 0.0
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}