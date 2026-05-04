package com.nutripulse.app.ui.stock

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.nutripulse.app.R
import com.nutripulse.app.data.AppDatabase
import com.nutripulse.app.data.model.FeedCategories
import com.nutripulse.app.data.model.StockItem
import com.nutripulse.app.data.model.StockTransaction
import com.nutripulse.app.data.model.TransactionType
import com.nutripulse.app.databinding.FragmentStockAddBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StockAddFragment : Fragment() {

    private var _binding: FragmentStockAddBinding? = null
    private val binding get() = _binding!!

    private var editStockId = -1
    private var selectedFeedId = -1
    private var selectedFeedName = ""
    private var selectedFeedCategory = ""
    private var currentStock: StockItem? = null
    private lateinit var transactionAdapter: StockTransactionAdapter

    companion object {
        fun newInstance() = StockAddFragment()
        fun newInstance(stockId: Int) = StockAddFragment().apply {
            arguments = Bundle().apply { putInt("stock_id", stockId) }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStockAddBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        editStockId = arguments?.getInt("stock_id", -1) ?: -1

        transactionAdapter = StockTransactionAdapter(emptyList())
        binding.recyclerTransactions.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerTransactions.adapter = transactionAdapter
        binding.layoutTransactionHistory.visibility = if (editStockId > 0) View.VISIBLE else View.GONE

        if (editStockId > 0) {
            binding.tvTitle.text = "STOK DÜZENLE"
            binding.btnDelete.visibility = View.VISIBLE
            loadStockItem(editStockId)
        }

        setupAutoCalc()
        setupButtons()
    }

    private fun setupAutoCalc() {
        // Stok değeri ve kalan gün otomatik hesapla
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { updateCalc() }
        }
        binding.etCurrentStock.addTextChangedListener(watcher)
        binding.etPurchasePrice.addTextChangedListener(watcher)
        binding.etDailyUsage.addTextChangedListener(watcher)
    }

    private fun updateCalc() {
        val stock = binding.etCurrentStock.text.toString().toDoubleOrNull() ?: 0.0
        val price = binding.etPurchasePrice.text.toString().toDoubleOrNull() ?: 0.0
        val usage = binding.etDailyUsage.text.toString().toDoubleOrNull() ?: 0.0

        if (stock > 0 || price > 0) {
            binding.layoutCalc.visibility = View.VISIBLE
            val value = stock * price
            binding.tvCalcValue.text = "₺${String.format("%.2f", value)}"
            val days = if (usage > 0) (stock / usage).toInt() else 0
            binding.tvCalcDays.text = if (days > 0) "$days gün" else "—"
        } else {
            binding.layoutCalc.visibility = View.GONE
        }
    }

    private fun setupButtons() {
        binding.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }

        binding.btnSelectFeed.setOnClickListener {
            // Yem seçici — LiveData ile yem listesini göster
            showFeedSelector()
        }

        binding.btnSave.setOnClickListener { saveStockItem() }
        binding.btnDelete.setOnClickListener { deleteStockItem() }
    }

    private fun showFeedSelector() {
        lifecycleScope.launch {
            val feeds = withContext(Dispatchers.IO) {
                AppDatabase.getDatabase(requireContext()).feedDao().getAllFeedsSnapshot()
            }

            // Basit dialog yerine alt sheet benzeri seçici
            val names = feeds.map { "${it.name} (${FeedCategories.displayName(it.category)})" }
            val dialog = android.app.AlertDialog.Builder(requireContext())
                .setTitle("Yem Seç")
                .setItems(names.toTypedArray()) { _, which ->
                    val feed = feeds[which]
                    selectedFeedId = feed.id
                    selectedFeedName = feed.name
                    selectedFeedCategory = feed.category
                    binding.tvSelectedFeed.text = feed.name
                    binding.tvSelectedFeed.setTextColor(
                        android.graphics.Color.parseColor("#00FF88"))
                    binding.tvFeedCategory.text = FeedCategories.displayName(feed.category)

                    // Mevcut fiyatı otomatik doldur
                    if (feed.pricePerKg > 0 &&
                        binding.etPurchasePrice.text.toString().isEmpty()) {
                        binding.etPurchasePrice.setText(feed.pricePerKg.toString())
                    }
                }
                .create()
            dialog.show()
        }
    }

    private fun loadStockItem(id: Int) {
        lifecycleScope.launch {
            val item = withContext(Dispatchers.IO) {
                AppDatabase.getDatabase(requireContext()).stockDao().getById(id)
            }
            item?.let {
                currentStock = it
                selectedFeedId = it.feedId
                selectedFeedName = it.feedName
                selectedFeedCategory = it.feedCategory

                binding.tvSelectedFeed.text = it.feedName
                binding.tvSelectedFeed.setTextColor(
                    android.graphics.Color.parseColor("#00FF88"))
                binding.tvFeedCategory.text = FeedCategories.displayName(it.feedCategory)

                if (it.currentStockKg > 0) binding.etCurrentStock.setText(it.currentStockKg.toString())
                if (it.minStockKg > 0)     binding.etMinStock.setText(it.minStockKg.toString())
                if (it.maxStockKg > 0)     binding.etMaxStock.setText(it.maxStockKg.toString())
                if (it.lastPurchasePrice > 0) binding.etPurchasePrice.setText(it.lastPurchasePrice.toString())
                if (it.dailyUsageKg > 0)   binding.etDailyUsage.setText(it.dailyUsageKg.toString())
                if (it.supplierName.isNotEmpty()) binding.etSupplier.setText(it.supplierName)
                if (it.supplierPhone.isNotEmpty()) binding.etSupplierPhone.setText(it.supplierPhone)
                if (it.location.isNotEmpty()) binding.etLocation.setText(it.location)
                if (it.batchNo.isNotEmpty()) binding.etBatchNo.setText(it.batchNo)
                if (it.notes.isNotEmpty()) binding.etNotes.setText(it.notes)

                updateCalc()

                // Hareket geçmişi
                showTransactionHistory(id)
            }
        }
    }

    private fun showTransactionHistory(stockId: Int) {
        AppDatabase.getDatabase(requireContext()).stockDao()
            .getTransactions(stockId)
            .observe(viewLifecycleOwner) { txList ->
                transactionAdapter.update(txList)
                binding.layoutTransactionHistory.visibility = View.VISIBLE
                binding.tvTransactionTitle.text = "HAREKET GEÇMİŞİ (${txList.size})"
                binding.recyclerTransactions.visibility = if (txList.isEmpty()) View.GONE else View.VISIBLE
                binding.tvTransactionEmpty.visibility = if (txList.isEmpty()) View.VISIBLE else View.GONE
            }
    }

    private fun saveStockItem() {
        if (selectedFeedId < 0) {
            Toast.makeText(requireContext(), "Yem seçin!", Toast.LENGTH_SHORT).show()
            return
        }

        val stock   = binding.etCurrentStock.text.toString().toDoubleOrNull() ?: 0.0
        val minStk  = binding.etMinStock.text.toString().toDoubleOrNull() ?: 0.0
        val maxStk  = binding.etMaxStock.text.toString().toDoubleOrNull() ?: 0.0
        val price   = binding.etPurchasePrice.text.toString().toDoubleOrNull() ?: 0.0
        val usage   = binding.etDailyUsage.text.toString().toDoubleOrNull() ?: 0.0
        val days    = if (usage > 0) (stock / usage).toInt() else 0

        val item = StockItem(
            id = if (editStockId > 0) editStockId else 0,
            feedId           = selectedFeedId,
            feedName         = selectedFeedName,
            feedCategory     = selectedFeedCategory,
            currentStockKg   = stock,
            minStockKg       = minStk,
            maxStockKg       = maxStk,
            avgCostPerKg     = price,
            lastPurchasePrice= price,
            totalStockValue  = stock * price,
            dailyUsageKg     = usage,
            daysRemaining    = days,
            supplierName     = binding.etSupplier.text.toString(),
            supplierPhone    = binding.etSupplierPhone.text.toString(),
            location         = binding.etLocation.text.toString(),
            batchNo          = binding.etBatchNo.text.toString(),
            notes            = binding.etNotes.text.toString(),
            lastReceiveDate  = System.currentTimeMillis()
        )

        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(requireContext())
            withContext(Dispatchers.IO) {
                if (editStockId > 0) {
                    db.stockDao().update(item)
                } else {
                    val newId = db.stockDao().insert(item)
                    // İlk giriş hareketi kaydet
                    if (stock > 0) {
                        val tx = StockTransaction(
                            stockItemId  = newId.toInt(),
                            feedName     = selectedFeedName,
                            feedCategory = selectedFeedCategory,
                            type         = TransactionType.PURCHASE,
                            amountKg     = stock,
                            pricePerKg   = price,
                            totalCost    = stock * price,
                            stockAfter   = stock,
                            supplierName = binding.etSupplier.text.toString(),
                            notes        = "İlk stok girişi"
                        )
                        db.stockDao().insertTransaction(tx)
                        // Feed tablosundaki fiyatı da güncelle
                        if (price > 0) db.feedDao().updatePrice(selectedFeedId, price)
                    }
                }
            }
            Toast.makeText(requireContext(),
                if (editStockId > 0) "Stok güncellendi!" else "Stok eklendi!",
                Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
        }
    }

    private fun deleteStockItem() {
        lifecycleScope.launch {
            currentStock?.let { item ->
                withContext(Dispatchers.IO) {
                    AppDatabase.getDatabase(requireContext()).stockDao()
                        .deactivate(item.id, System.currentTimeMillis())
                }
                Toast.makeText(requireContext(), "Stok kaydı pasife alındı", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}