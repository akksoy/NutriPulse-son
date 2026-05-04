package com.nutripulse.app.ui.ration

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nutripulse.app.data.AppDatabase
import com.nutripulse.app.data.model.Ration
import com.nutripulse.app.databinding.FragmentRationCompareBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RationCompareFragment : Fragment() {

    private var _binding: FragmentRationCompareBinding? = null
    private val binding get() = _binding!!

    private var selectedA: Ration? = null
    private var selectedB: Ration? = null
    private lateinit var listAdapter: RationListAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRationCompareBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
        setupList()
        loadRations()
    }

    private fun setupList() {
        listAdapter = RationListAdapter(emptyList()) { ration ->
            when {
                selectedA == null -> { selectedA = ration; updateCompare() }
                selectedB == null -> { selectedB = ration; updateCompare() }
                else -> { selectedA = ration; selectedB = null; updateCompare() }
            }
        }
        binding.recyclerRations.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerRations.adapter = listAdapter
    }

    private fun loadRations() {
        AppDatabase.getDatabase(requireContext()).rationDao().getAll()
            .observe(viewLifecycleOwner) { list ->
                listAdapter.update(list)
            }
    }

    private fun updateCompare() {
        val a = selectedA ?: return
        val b = selectedB ?: return
        binding.layoutCompareTable.visibility = View.VISIBLE

        binding.tvCompareA.text = a.name.take(10)
        binding.tvCompareB.text = b.name.take(10)

        binding.tvCostA.text = "₺${String.format("%.2f", a.totalCostPerDay)}"
        binding.tvCostB.text = "₺${String.format("%.2f", b.totalCostPerDay)}"

        binding.tvProfitA.text = "₺${String.format("%.2f", a.netProfitPerDay)}"
        binding.tvProfitB.text = "₺${String.format("%.2f", b.netProfitPerDay)}"

        binding.tvCoverageA.text = "%${a.coveragePct}"
        binding.tvCoverageB.text = "%${b.coveragePct}"

        binding.tvYieldA.text = "${a.estimatedYield} L"
        binding.tvYieldB.text = "${b.estimatedYield} L"

        binding.tvLimitA.text = a.limitingNutrient.ifEmpty { "—" }
        binding.tvLimitB.text = b.limitingNutrient.ifEmpty { "—" }

        // Öneri
        val bestNet = if (a.netProfitPerDay >= b.netProfitPerDay) a else b
        binding.tvCompareRecommend.text = "✅ Önerilen: ${bestNet.name} (Net Kar ₺${String.format("%.2f", bestNet.netProfitPerDay)}/gün)"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// ── Rasyon listesi adapter ──────────────────────────────────────
class RationListAdapter(
    private var list: List<Ration>,
    private val onSelect: (Ration) -> Unit
) : RecyclerView.Adapter<RationListAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvName: TextView   = v.findViewById(android.R.id.text1)
        val tvDetail: TextView = v.findViewById(android.R.id.text2)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_2, parent, false)
        v.setPadding(32, 16, 32, 16)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val r = list[position]
        holder.tvName.text = r.name
        holder.tvName.setTextColor(Color.parseColor("#00FF88"))
        holder.tvName.textSize = 13f
        holder.tvDetail.text =
            "₺${String.format("%.2f", r.totalCostPerDay)}/gün  " +
            "Kar:₺${String.format("%.2f", r.netProfitPerDay)}  " +
            "%${r.coveragePct} karşılama"
        holder.tvDetail.setTextColor(Color.parseColor("#7ABFA0"))
        holder.tvDetail.textSize = 10f
        holder.itemView.setOnClickListener { onSelect(r) }
    }

    override fun getItemCount() = list.size
    fun update(l: List<Ration>) { list = l; notifyDataSetChanged() }
}