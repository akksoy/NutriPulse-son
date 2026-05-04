package com.nutripulse.app.ui.stock

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.nutripulse.app.R
import com.nutripulse.app.data.AppDatabase
import com.nutripulse.app.data.model.StockItem
import com.nutripulse.app.data.model.TransactionType
import com.nutripulse.app.databinding.FragmentStockTransactionBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

class StockTransactionFragment : Fragment() {

    private var _binding: FragmentStockTransactionBinding? = null
    private val binding get() = _binding!!

    private var stockItemId = -1
    private var initialType = TransactionType.PURCHASE
    private var currentType = TransactionType.PURCHASE
    private var stockItem: StockItem? = null

    companion object {
        fun newInstance(stockId: Int, type: String = TransactionType.PURCHASE) =
            StockTransactionFragment().apply {
                arguments = Bundle().apply {
                    putInt("stock_id", stockId)
                    putString("type", type)
                }
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStockTransactionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        stockItemId = arguments?.getInt("stock_id", -1) ?: -1
        initialType = arguments?.getString("type") ?: TransactionType.PURCHASE
        currentType = initialType

        loadStockItem()
        setupTypeButtons()
        setupAutoCalc()

        binding.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.btnSave.setOnClickListener { saveTransaction() }
    }

    private fun loadStockItem() {
        lifecycleScope.launch {
            val item = withContext(Dispatchers.IO) {
                AppDatabase.getDatabase(requireContext()).stockDao().getById(stockItemId)
            }
            item?.let {
                stockItem = it
                binding.tvTxFeedName.text = it.feedName
                binding.tvCurrentStockInfo.text = "${String.format("%.1f", it.currentStockKg)} kg"
                binding.tvCurrentValueInfo.text = "₺${String.format("%.0f", it.totalStockValue)}"

                // Varsayılan fiyat
                if (it.lastPurchasePrice > 0) {
                    binding.etPrice.setText(it.lastPurchasePrice.toString())
                }
                // Tedarikçi
                if (it.supplierName.isNotEmpty()) {
                    binding.etSupplierName.setText(it.supplierName)
                }

                selectType(currentType)
                updateAfterStock()
            }
        }
    }

    private fun setupTypeButtons() {
        binding.btnTypePurchase.setOnClickListener { selectType(TransactionType.PURCHASE) }
        binding.btnTypeUsage.setOnClickListener    { selectType(TransactionType.USAGE)    }
        binding.btnTypeAdjust.setOnClickListener   { selectType(TransactionType.ADJUSTMENT) }
        binding.btnTypeWaste.setOnClickListener    { selectType(TransactionType.WASTE)    }
    }

    private fun selectType(type: String) {
        currentType = type
        val color = TransactionType.color(type)

        // Tüm butonları sıfırla
        listOf(binding.btnTypePurchase, binding.btnTypeUsage,
               binding.btnTypeAdjust, binding.btnTypeWaste).forEach { btn ->
            (btn as LinearLayout).setBackgroundResource(R.drawable.bg_feed_item)
        }

        // Seçiliyi vurgula
        val selected = when (type) {
            TransactionType.PURCHASE   -> binding.btnTypePurchase
            TransactionType.USAGE      -> binding.btnTypeUsage
            TransactionType.ADJUSTMENT -> binding.btnTypeAdjust
            TransactionType.WASTE      -> binding.btnTypeWaste
            else -> binding.btnTypePurchase
        }
        selected.setBackgroundResource(R.drawable.bg_stat_green)

        // Başlık & miktar rengi
        binding.tvTxTitle.text = when (type) {
            TransactionType.PURCHASE   -> "📥 SATIN ALMA — Stok Artışı"
            TransactionType.USAGE      -> "📤 TÜKETİM — Stok Azalışı"
            TransactionType.ADJUSTMENT -> "🔧 STOK DÜZELTMESİ"
            TransactionType.WASTE      -> "🗑 FİRE / KAYIP"
            else -> "STOK HAREKETİ"
        }
        binding.tvTxTitle.setTextColor(Color.parseColor(color))
        binding.tvTotalAmount.setTextColor(Color.parseColor(color))

        // Fiyat alanı sadece satın almada gerekli
        binding.etPrice.isEnabled = type == TransactionType.PURCHASE

        updateAfterStock()
    }

    private fun setupAutoCalc() {
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateAfterStock()
                updateTotal()
            }
        }
        binding.etAmount.addTextChangedListener(watcher)
        binding.etPrice.addTextChangedListener(watcher)
    }

    private fun updateTotal() {
        val item = stockItem ?: return
        val amt = binding.etAmount.text.toString().toDoubleOrNull()
        val price = binding.etPrice.text.toString().toDoubleOrNull() ?: item.lastPurchasePrice
        val preview = StockTransactionCalculator.preview(item, currentType, amt, price)
        binding.tvTotalAmount.text = "₺${String.format("%.2f", preview.totalCost)}"
    }

    private fun updateAfterStock() {
        val item = stockItem ?: return
        val amt = binding.etAmount.text.toString().toDoubleOrNull()
        val price = binding.etPrice.text.toString().toDoubleOrNull() ?: item.lastPurchasePrice
        val preview = StockTransactionCalculator.preview(item, currentType, amt, price)

        binding.tvAfterStock.text = "${String.format("%.1f", preview.stockAfter)} kg"
        binding.tvAfterDays.text = if (preview.updatedDaysRemaining > 0) "${preview.updatedDaysRemaining} gün" else "—"

        val color = when {
            preview.stockAfter <= 0 -> "#FF5A5A"
            item.minStockKg > 0 && preview.stockAfter < item.minStockKg -> "#FF7A00"
            else -> "#00FF88"
        }
        binding.tvAfterStock.setTextColor(Color.parseColor(color))
    }

    private fun saveTransaction() {
        val item = stockItem ?: return
        val amt = binding.etAmount.text.toString().toDoubleOrNull()
        if (amt == null || amt <= 0) {
            Toast.makeText(requireContext(), "Miktar girin!", Toast.LENGTH_SHORT).show()
            return
        }

        val validationError = StockTransactionCalculator.validate(item, currentType, amt)
        if (validationError != null) {
            Toast.makeText(requireContext(), validationError, Toast.LENGTH_SHORT).show()
            return
        }

        val price = binding.etPrice.text.toString().toDoubleOrNull() ?: item.lastPurchasePrice
        val draft = StockTransactionCalculator.buildDraft(
            item = item,
            type = currentType,
            amountKg = amt,
            pricePerKg = price,
            referenceNo = binding.etRefNo.text.toString(),
            supplierName = binding.etSupplierName.text.toString(),
            notes = binding.etNotes.text.toString()
        )

        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(requireContext())
            withContext(Dispatchers.IO) {
                db.stockDao().insertTransaction(draft.transaction)
                db.stockDao().updateStock(
                    item.id,
                    draft.preview.stockAfter,
                    draft.preview.stockAfter * draft.preview.updatedAvgCostPerKg,
                    System.currentTimeMillis()
                )
                db.stockDao().updateUsageEstimate(item.id, item.dailyUsageKg, draft.preview.updatedDaysRemaining)
                if (currentType == TransactionType.PURCHASE && price > 0) {
                    db.feedDao().updatePrice(item.feedId, price)
                }
            }

            val msg = when (currentType) {
                TransactionType.PURCHASE   -> "✅ ${String.format("%.1f", amt)} kg giriş kaydedildi"
                TransactionType.USAGE      -> "📤 ${String.format("%.1f", amt)} kg tüketim kaydedildi"
                TransactionType.ADJUSTMENT -> "🔧 Stok ${String.format("%.1f", draft.preview.stockAfter)} kg olarak düzeltildi"
                TransactionType.WASTE      -> "🗑 ${String.format("%.1f", amt)} kg fire kaydedildi"
                else -> "Kayıt edildi"
            }
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}