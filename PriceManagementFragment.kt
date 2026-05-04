package com.nutripulse.app.ui.feed

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nutripulse.app.R
import com.nutripulse.app.data.PriceScenarioManager
import com.nutripulse.app.data.PriceScenarioManager.AlertType
import com.nutripulse.app.data.PriceScenarioManager.PriceAlert
import com.nutripulse.app.data.PriceScenarioManager.PriceScenario
import com.nutripulse.app.data.TmoFetcher
import com.nutripulse.app.databinding.FragmentPriceManagementBinding
import com.nutripulse.app.databinding.ItemPriceAlertBinding
import com.nutripulse.app.databinding.ItemPriceScenarioBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PriceManagementFragment : Fragment() {

    private var _binding: FragmentPriceManagementBinding? = null
    private val binding get() = _binding!!

    private lateinit var scenarioAdapter: ScenarioAdapter
    private lateinit var alertAdapter: AlertAdapter

    private val scenarios = mutableListOf<PriceScenario>()
    private val alerts = mutableListOf<PriceAlert>()

    private val csvPicker = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                importPricesFromFile(uri)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPriceManagementBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        loadData()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }

        scenarioAdapter = ScenarioAdapter(
            scenarios = scenarios,
            onApplyClick = { scenario -> applyScenario(scenario.id) },
            onDeleteClick = { scenario -> deleteScenario(scenario) }
        )
        binding.rvScenarios.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = scenarioAdapter
        }

        alertAdapter = AlertAdapter(alerts) { alert ->
            alerts.remove(alert)
            alertAdapter.notifyDataSetChanged()
            saveAlerts()
        }
        binding.rvAlerts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = alertAdapter
        }

        binding.btnImportCsv.setOnClickListener { openFilePicker() }
        binding.btnAddAlert.setOnClickListener { addNewAlert() }
        binding.btnSyncPrices.setOnClickListener { syncPrices() }
    }

    private fun loadData() {
        lifecycleScope.launch {
            scenarios.clear()
            scenarios.addAll(PriceScenarioManager.loadAllScenarios(requireContext()))
            scenarioAdapter.notifyDataSetChanged()
            binding.tvScenarioCount.text = "${scenarios.size} senaryo"

            alerts.clear()
            alerts.addAll(PriceScenarioManager.loadAlerts(requireContext()))
            alertAdapter.notifyDataSetChanged()
            binding.tvAlertCount.text = "${alerts.size} uyarı"

            val prices = TmoFetcher.getCurrent2026Prices()
            binding.tvCurrentPrices.text = "${prices.size} referans fiyat yüklü"
            binding.tvCurrentPrices.setTextColor(Color.parseColor("#00FF88"))
        }
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(
                "text/csv", "text/comma-separated-values", "text/plain"
            ))
        }
        csvPicker.launch(intent)
    }

    private fun importPricesFromFile(uri: Uri) {
        lifecycleScope.launch {
            binding.btnImportCsv.isEnabled = false
            binding.tvImportStatus.text = "İçe aktarılıyor..."
            binding.tvImportStatus.visibility = View.VISIBLE

            try {
                val filePath = copyToInternalStorage(uri)
                if (filePath != null) {
                    val result = PriceScenarioManager.importPricesFromCSV(requireContext(), filePath)
                    val statusText = "${result.imported} fiyat içe aktarıldı" + 
                        if (result.skipped > 0) " (${result.skipped} atlandı)" else ""
                    binding.tvImportStatus.text = statusText
                    loadData()
                }
            } catch (e: Exception) {
                android.util.Log.e("PriceManagementFragment", "Import failed", e)
                val msg = e.message ?: e.toString()
                binding.tvImportStatus.text = "Hata: $msg"
            } finally {
                binding.btnImportCsv.isEnabled = true
            }
        }
    }

    private fun copyToInternalStorage(uri: Uri): String? {
        return try {
            val inputStream = requireContext().contentResolver.openInputStream(uri) ?: return null
            val fileName = "import_prices_${System.currentTimeMillis()}.csv"
            val file = java.io.File(requireContext().filesDir, fileName)
            file.outputStream().use { output ->
                inputStream.copyTo(output)
            }
            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    private fun applyScenario(scenarioId: String) {
        lifecycleScope.launch {
            val count = PriceScenarioManager.applyScenario(requireContext(), scenarioId)
            if (count > 0) {
                Toast.makeText(requireContext(), "$count yem fiyatı güncellendi", Toast.LENGTH_SHORT).show()
                syncPrices()
            }
            loadData()
        }
    }

    private fun deleteScenario(scenario: PriceScenario) {
        scenarios.remove(scenario)
        val all = PriceScenarioManager.loadAllScenarios(requireContext()).toMutableList()
        all.removeAll { it.id == scenario.id }
        all.forEach { PriceScenarioManager.saveScenario(requireContext(), it) }
        scenarioAdapter.notifyDataSetChanged()
        binding.tvScenarioCount.text = "${scenarios.size} senaryo"
    }

    private fun addNewAlert() {
        val feedName = binding.etAlertFeed.text.toString()
        val thresholdStr = binding.etAlertThreshold.text.toString()
        
        if (feedName.isBlank() || thresholdStr.isBlank()) {
            Toast.makeText(requireContext(), "Lütfen tüm alanları doldurun", Toast.LENGTH_SHORT).show()
            return
        }

        val threshold = thresholdStr.toDoubleOrNull()
        if (threshold == null || threshold <= 0) {
            Toast.makeText(requireContext(), "Geçerli bir fiyat girin", Toast.LENGTH_SHORT).show()
            return
        }

        val alertType = when (binding.spAlertType.selectedItemPosition) {
            0 -> AlertType.PRICE_ABOVE_THRESHOLD
            1 -> AlertType.PRICE_BELOW_THRESHOLD
            else -> AlertType.PRICE_ABOVE_THRESHOLD
        }

        val alert = PriceAlert(
            feedName = feedName,
            currentPrice = 0.0,
            thresholdPrice = threshold,
            alertType = alertType,
            message = "$feedName için fiyat uyarısı: $threshold TL/kg"
        )

        alerts.add(alert)
        alertAdapter.notifyDataSetChanged()
        saveAlerts()
        binding.tvAlertCount.text = "${alerts.size} uyarı"

        binding.etAlertFeed.text?.clear()
        binding.etAlertThreshold.text?.clear()
        Toast.makeText(requireContext(), "Uyarı eklendi", Toast.LENGTH_SHORT).show()
    }

    private fun saveAlerts() {
        PriceScenarioManager.saveAlerts(requireContext(), alerts)
    }

    private fun syncPrices() {
        lifecycleScope.launch {
            binding.btnSyncPrices.isEnabled = false
            binding.tvSyncStatus.text = "Fiyatlar güncelleniyor..."
            binding.tvSyncStatus.visibility = View.VISIBLE

            val result = withContext(Dispatchers.IO) {
                com.nutripulse.app.data.PriceSyncManager.syncAllFeedPrices(requireContext())
            }

            binding.tvSyncStatus.text = "${result.updatedFeeds} / ${result.totalFeeds} yem güncellendi"
            binding.btnSyncPrices.isEnabled = true
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    inner class ScenarioAdapter(
        private val scenarios: MutableList<PriceScenario>,
        private val onApplyClick: (PriceScenario) -> Unit,
        private val onDeleteClick: (PriceScenario) -> Unit
    ) : RecyclerView.Adapter<ScenarioAdapter.ViewHolder>() {

        inner class ViewHolder(val binding: ItemPriceScenarioBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemPriceScenarioBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val scenario = scenarios[position]
            holder.binding.tvScenarioName.text = scenario.name
            holder.binding.tvScenarioDesc.text = scenario.description
            holder.binding.tvScenarioPriceCount.text = "${scenario.prices.size} fiyat"

            holder.binding.btnApply.isEnabled = !scenario.isActive
            holder.binding.btnApply.setOnClickListener { onApplyClick(scenario) }
            holder.binding.btnDelete.setOnClickListener { onDeleteClick(scenario) }

            holder.binding.cardScenario.setCardBackgroundColor(
                if (scenario.isActive) Color.parseColor("#1A00FF88") else Color.WHITE
            )
        }

        override fun getItemCount() = scenarios.size
    }

    inner class AlertAdapter(
        private val alerts: MutableList<PriceAlert>,
        private val onDeleteClick: (PriceAlert) -> Unit
    ) : RecyclerView.Adapter<AlertAdapter.ViewHolder>() {

        inner class ViewHolder(val binding: ItemPriceAlertBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemPriceAlertBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val alert = alerts[position]
            holder.binding.tvAlertFeed.text = alert.feedName
            holder.binding.tvAlertThreshold.text = "Eşik: ${alert.thresholdPrice} TL/kg"
            
            val typeText = when (alert.alertType) {
                AlertType.PRICE_ABOVE_THRESHOLD -> "Fiyat yüksek"
                AlertType.PRICE_BELOW_THRESHOLD -> "Fiyat düşük"
                AlertType.PRICE_CHANGE_HIGH -> "Fiyat değişimi"
                AlertType.STOCK_LOW -> "Stok düşük"
            }
            holder.binding.tvAlertType.text = typeText
            holder.binding.btnDeleteAlert.setOnClickListener { onDeleteClick(alert) }
        }

        override fun getItemCount() = alerts.size
    }
}