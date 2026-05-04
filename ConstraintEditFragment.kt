package com.nutripulse.app.ui.ration

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nutripulse.app.R
import com.nutripulse.app.databinding.FragmentConstraintEditBinding
import com.nutripulse.app.databinding.ItemConstraintRowBinding

/**
 * NutriPulse — Kısıtlama Düzenleme Ekranı
 * Kullanıcı her besin için min/max değer girebilir.
 * Sonuçlar RationFragment'e callback ile iletilir.
 */
class ConstraintEditFragment : Fragment() {

    private var _binding: FragmentConstraintEditBinding? = null
    private val binding get() = _binding!!

    // Geri döndürülecek kısıtlar
    var onConstraintsSaved: ((Map<String, Pair<Double, Double>>) -> Unit)? = null

    data class ConstraintRow(
        val code: String,
        val name: String,
        val unit: String,
        var min: Double = 0.0,
        var max: Double = -1.0,
        var active: Boolean = true
    )

    private val defaultConstraints = listOf(
        ConstraintRow("NEL",  "NEL (Net Enerji Laktasyon)", "MJ/kg KM"),
        ConstraintRow("ME",   "ME (Metabolik Enerji)",      "MJ/kg KM"),
        ConstraintRow("CP",   "Ham Protein",                "%KM"),
        ConstraintRow("RDP",  "RDP (Rumen Yıkılan Protein)","g/kg KM"),
        ConstraintRow("RUP",  "RUP (Bypass Protein)",       "g/kg KM"),
        ConstraintRow("NDF",  "NDF (Nötr Deterjan Lif)",    "%KM", min = 28.0, max = 45.0),
        ConstraintRow("CA",   "Kalsiyum (Ca)",              "g/kg KM"),
        ConstraintRow("P",    "Fosfor (P)",                 "g/kg KM"),
        ConstraintRow("MG",   "Magnezyum (Mg)",             "g/kg KM"),
        ConstraintRow("NA",   "Sodyum (Na)",                "g/kg KM"),
        ConstraintRow("K",    "Potasyum (K)",               "g/kg KM"),
        ConstraintRow("LYS",  "Lizin (Lys)",                "g/kg KM"),
        ConstraintRow("MET",  "Metiyonin (Met)",            "g/kg KM")
    )

    private lateinit var adapter: ConstraintAdapter

    companion object {
        fun newInstance(existing: Map<String, Pair<Double, Double>>? = null) =
            ConstraintEditFragment().apply {
                // Mevcut kısıtları argümana koy
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConstraintEditBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ConstraintAdapter(defaultConstraints.toMutableList())
        binding.recyclerConstraints.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerConstraints.adapter = adapter

        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.btnApply.setOnClickListener {
            val result = adapter.getActiveConstraints()
            onConstraintsSaved?.invoke(result)
            Toast.makeText(requireContext(), "${result.size} kısıt uygulandı", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
        }

        binding.btnResetAll.setOnClickListener {
            adapter.resetAll()
            Toast.makeText(requireContext(), "Kısıtlar sıfırlandı", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// ── Adapter ──────────────────────────────────────────────────────
class ConstraintAdapter(
    private val rows: MutableList<ConstraintEditFragment.ConstraintRow>
) : RecyclerView.Adapter<ConstraintAdapter.CVH>() {

    inner class CVH(val b: ItemConstraintRowBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CVH {
        val b = ItemConstraintRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CVH(b)
    }

    override fun onBindViewHolder(holder: CVH, position: Int) {
        val row = rows[position]
        val b = holder.b

        b.tvConstraintName.text = row.name
        b.tvConstraintUnit.text = row.unit
        b.switchActive.isChecked = row.active

        if (row.min > 0) b.etConstraintMin.setText(row.min.toString())
        if (row.max > 0) b.etConstraintMax.setText(row.max.toString())

        b.switchActive.setOnCheckedChangeListener { _, checked ->
            rows[position] = row.copy(active = checked)
            b.etConstraintMin.isEnabled = checked
            b.etConstraintMax.isEnabled = checked
        }

        b.etConstraintMin.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val v = b.etConstraintMin.text.toString().toDoubleOrNull() ?: 0.0
                rows[position] = rows[position].copy(min = v)
            }
        }

        b.etConstraintMax.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val v = b.etConstraintMax.text.toString().toDoubleOrNull() ?: -1.0
                rows[position] = rows[position].copy(max = v)
            }
        }
    }

    override fun getItemCount() = rows.size

    fun getActiveConstraints(): Map<String, Pair<Double, Double>> {
        return rows
            .filter { it.active && (it.min > 0 || it.max > 0) }
            .associate { it.code to Pair(it.min, it.max) }
    }

    fun resetAll() {
        rows.forEach { row ->
            rows[rows.indexOf(row)] = row.copy(min = 0.0, max = -1.0)
        }
        notifyDataSetChanged()
    }
}