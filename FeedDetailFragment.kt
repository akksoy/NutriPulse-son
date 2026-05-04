package com.nutripulse.app.ui.feed

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.nutripulse.app.R
import com.nutripulse.app.data.AppDatabase
import com.nutripulse.app.data.model.Feed
import com.nutripulse.app.data.model.FeedCategories
import com.nutripulse.app.databinding.FragmentFeedDetailBinding
import com.nutripulse.app.databinding.ItemNutrientRowBinding
import com.nutripulse.app.utils.PriceFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FeedDetailFragment : Fragment() {

    private var _binding: FragmentFeedDetailBinding? = null
    private val binding get() = _binding!!

    companion object {
        const val ARG_FEED_ID = "feed_id"
        fun newInstance(feedId: Int) = FeedDetailFragment().apply {
            arguments = Bundle().apply { putInt(ARG_FEED_ID, feedId) }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentFeedDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val feedId = arguments?.getInt(ARG_FEED_ID) ?: return

        binding.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }

        // DUZENLE butonu
        binding.btnEdit.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, AddEditFeedFragment.newInstance(feedId))
                .addToBackStack(null)
                .commit()
        }

        lifecycleScope.launch {
            val feed = withContext(Dispatchers.IO) {
                AppDatabase.getDatabase(requireContext()).feedDao().getFeedById(feedId)
            }
            feed?.let { displayFeed(it) }
        }
    }

    private fun displayFeed(feed: Feed) {
        binding.tvDetailName.text = feed.name
        binding.tvDetailCategory.text = FeedCategories.displayName(feed.category)
        binding.viewCategoryBar.setBackgroundColor(Color.parseColor(categoryColor(feed.category)))

        binding.tvDm.text = fmt(feed.dm)
        binding.tvMe.text = fmt(feed.me)
        binding.tvCp.text = fmt(feed.cp)

        setRow(binding.rowNel, "NEL", "MJ/kg", feed.nel)
        setRow(binding.rowNem, "NEM", "MJ/kg", feed.nem)
        setRow(binding.rowNeg, "NEG", "MJ/kg", feed.neg)
        setRow(binding.rowRdp, "RDP (Rumen Yikilan)", "%KM", feed.rdp)
        setRow(binding.rowRup, "RUP (Bypass Protein)", "%KM", feed.rup)
        setRow(binding.rowNdf, "NDF", "%KM", feed.ndf)
        setRow(binding.rowAdf, "ADF", "%KM", feed.adf)
        setRow(binding.rowAdl, "ADL (Lignin)", "%KM", feed.adl)
        setRow(binding.rowFat, "Ham Yag", "%KM", feed.fat)
        setRow(binding.rowStarch, "Nisasta", "%KM", feed.starch)
        setRow(binding.rowSugar, "Seker", "%KM", feed.sugar)
        setRow(binding.rowCa, "Kalsiyum (Ca)", "%KM", feed.ca)
        setRow(binding.rowP, "Fosfor (P)", "%KM", feed.p)
        setRow(binding.rowMg, "Magnezyum (Mg)", "%KM", feed.mg)
        setRow(binding.rowNa, "Sodyum (Na)", "%KM", feed.na)
        setRow(binding.rowK, "Potasyum (K)", "%KM", feed.k)
        setRow(binding.rowS, "Kukurt (S)", "%KM", feed.s)
        setRow(binding.rowLys, "Lizin (Lys)", "%KM", feed.lys)
        setRow(binding.rowMet, "Metiyonin (Met)", "%KM", feed.met)
        setRow(binding.rowThr, "Treonin (Thr)", "%KM", feed.thr)
        setRow(binding.rowTrp, "Triptofan (Trp)", "%KM", feed.trp)
        setRow(binding.rowMaxKg, "Maks Gunluk kg", "kg/gun", feed.maxDailyKg)
        setRow(binding.rowMaxPct, "Maks KM Orani", "%KM", feed.maxDmPct)
        setRow(binding.rowPrice, "Fiyat", "TL/kg", feed.pricePerKg)

        if (feed.notes.isNotEmpty()) {
            binding.layoutNotes.visibility = View.VISIBLE
            binding.tvNotes.text = feed.notes
        }
    }

    private fun setRow(row: ItemNutrientRowBinding, label: String, unit: String, value: Double) {
        row.tvNutrientLabel.text = label
        row.tvNutrientUnit.text = unit
        if (value == 0.0) {
            row.tvNutrientValue.text = "—"
            row.tvNutrientValue.setTextColor(Color.parseColor("#3A5A48"))
        } else {
            // Fiyat alanı özel formatlama
            if (label == "Fiyat") {
                row.tvNutrientValue.text = PriceFormatter.formatPrice(value)
            } else {
                row.tvNutrientValue.text = fmt(value)
            }
            row.tvNutrientValue.setTextColor(Color.parseColor("#00FF88"))
        }
    }

    private fun fmt(v: Double) =
        if (v == v.toLong().toDouble()) v.toLong().toString()
        else "%.2f".format(v).trimEnd('0').trimEnd('.')

    private fun categoryColor(cat: String) = when (cat) {
        FeedCategories.ROUGHAGE_WET -> "#00FF88"
        FeedCategories.ROUGHAGE_DRY -> "#00CC6A"
        FeedCategories.GRAIN        -> "#FFC400"
        FeedCategories.PROTEIN      -> "#00D4FF"
        FeedCategories.BYPRODUCT    -> "#FF7A00"
        FeedCategories.FAT          -> "#FF5FA0"
        FeedCategories.MINERAL      -> "#BF7FFF"
        FeedCategories.ADDITIVE     -> "#00EEFF"
        FeedCategories.AQUA         -> "#00BBFF"
        else -> "#00FF88"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}