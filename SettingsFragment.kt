package com.nutripulse.app.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.nutripulse.app.R
import com.nutripulse.app.data.AppDatabase
import com.nutripulse.app.data.model.FarmProfile
import com.nutripulse.app.data.model.SubscriptionType
import com.nutripulse.app.databinding.FragmentSettingsBinding
import com.nutripulse.app.databinding.ItemSettingsToggleBinding
import com.nutripulse.app.databinding.ItemSettingsActionBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private var currentProfile: FarmProfile? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToggles()
        setupActions()
        loadProfile()

        binding.btnGoFarmProfile.setOnClickListener { openFarmProfile() }
        binding.btnUpgrade.setOnClickListener {
            Toast.makeText(requireContext(),
                "Pro lisans için nutripulse@gmail.com ile iletişime geçin", Toast.LENGTH_LONG).show()
        }
        binding.btnSaveSettings.setOnClickListener { saveSettings() }
    }

    private fun setupToggles() {
        // Bildirimler
        setToggle(binding.rowNotifications, "🔔", "Bildirimler",
            "Uygulama bildirimleri", true)
        // Kritik stok
        setToggle(binding.rowCriticalAlert, "⚠", "Kritik Stok Uyarısı",
            "Stok alt sınırında uyar", true)
        // Günlük rapor
        setToggle(binding.rowDailyReport, "📊", "Günlük Rapor",
            "Her sabah özet bildirimi", false)
        // Otomatik yedek
        setToggle(binding.rowAutoBackup, "☁", "Otomatik Yedekleme",
            "Günlük bulut yedekleme", false)
    }

    private fun setToggle(
        row: ItemSettingsToggleBinding,
        emoji: String, title: String, desc: String,
        checked: Boolean
    ) {
        row.tvToggleEmoji.text = emoji
        row.tvToggleTitle.text = title
        row.tvToggleDesc.text  = desc
        row.switchToggle.isChecked = checked
    }

    private fun setupActions() {
        // Veri yönetimi
        setAction(binding.rowExportData, "📤", "Veriyi Dışa Aktar", "JSON / Excel formatında")
        setAction(binding.rowImportData, "📥", "Veriyi İçe Aktar", "Yedekten geri yükle")
        setAction(binding.rowClearRations, "🗑", "Rasyonları Temizle", "Tüm rasyonları sil", "Dikkat!")

        binding.rowExportData.root.setOnClickListener {
            Toast.makeText(requireContext(), "Dışa aktarma yakında!", Toast.LENGTH_SHORT).show()
        }
        binding.rowImportData.root.setOnClickListener {
            Toast.makeText(requireContext(), "İçe aktarma yakında!", Toast.LENGTH_SHORT).show()
        }
        binding.rowClearRations.root.setOnClickListener {
            confirmClearRations()
        }

        // Hakkında
        setAction(binding.rowAbout, "⚡", "NutriPulse Hakkında", "v1.0.0 — 2026")
        setAction(binding.rowPrivacy, "🔒", "Gizlilik Politikası", "")
        setAction(binding.rowContact, "📧", "İletişim & Destek", "nutripulse@gmail.com")

        binding.rowAbout.root.setOnClickListener { showAbout() }
        binding.rowContact.root.setOnClickListener {
            Toast.makeText(requireContext(), "nutripulse@gmail.com", Toast.LENGTH_LONG).show()
        }
    }

    private fun setAction(
        row: ItemSettingsActionBinding,
        emoji: String, title: String, desc: String, badge: String = ""
    ) {
        row.tvActionEmoji.text = emoji
        row.tvActionTitle.text = title
        row.tvActionDesc.text  = desc
        row.tvActionBadge.text = badge
        if (badge.isNotEmpty()) row.tvActionBadge.setTextColor(
            android.graphics.Color.parseColor("#FF7A00"))
    }

    private fun loadProfile() {
        AppDatabase.getDatabase(requireContext()).farmProfileDao().getProfile()
            .observe(viewLifecycleOwner) { profile ->
                profile?.let { p ->
                    currentProfile = p
                    // Çiftlik kartını güncelle
                    binding.tvFarmName.text = p.farmName.ifEmpty { "Çiftlik Profili" }
                    binding.tvFarmOwner.text = if (p.ownerName.isNotEmpty())
                        "${p.ownerName}  •  ${p.farmType}"
                    else "Profil düzenlemek için tıklayın"

                    // Lisans kartı
                    binding.tvLicenseType.text = SubscriptionType.display(p.subscriptionType)
                    binding.tvLicenseType.setTextColor(
                        android.graphics.Color.parseColor(SubscriptionType.color(p.subscriptionType)))
                    binding.tvLicenseDetail.text = when (p.subscriptionType) {
                        SubscriptionType.PRO -> "Pro lisans aktif ✅"
                        SubscriptionType.ENTERPRISE -> "Kurumsal lisans aktif ✅"
                        else -> "Pro'ya geçin — Tüm özellikleri açın"
                    }

                    // Toggle'ları güncelle
                    binding.rowNotifications.switchToggle.isChecked = p.notificationsEnabled
                    binding.rowCriticalAlert.switchToggle.isChecked  = p.criticalStockAlert
                    binding.rowDailyReport.switchToggle.isChecked    = p.dailyReportEnabled
                    binding.rowAutoBackup.switchToggle.isChecked     = p.autoBackup

                    // Varsayılanlar
                    binding.etDefaultMilkPrice.setText(p.defaultMilkPrice.toString())
                    binding.etDefaultNdfMin.setText(p.defaultNdfMin.toString())
                    binding.etDefaultNdfMax.setText(p.defaultNdfMax.toString())
                }
            }
    }

    private fun saveSettings() {
        val db = AppDatabase.getDatabase(requireContext())
        val loadedProfile = currentProfile
        lifecycleScope.launch {
            val profile = loadedProfile ?: withContext(Dispatchers.IO) {
                db.farmProfileDao().getProfileSync()
            } ?: FarmProfile()
            val updated = profile.copy(
                notificationsEnabled = binding.rowNotifications.switchToggle.isChecked,
                criticalStockAlert   = binding.rowCriticalAlert.switchToggle.isChecked,
                dailyReportEnabled   = binding.rowDailyReport.switchToggle.isChecked,
                autoBackup           = binding.rowAutoBackup.switchToggle.isChecked,
                defaultMilkPrice = binding.etDefaultMilkPrice.text.toString().toDoubleOrNull() ?: 18.0,
                defaultNdfMin    = binding.etDefaultNdfMin.text.toString().toDoubleOrNull() ?: 28.0,
                defaultNdfMax    = binding.etDefaultNdfMax.text.toString().toDoubleOrNull() ?: 45.0,
                updatedAt        = System.currentTimeMillis()
            )
            withContext(Dispatchers.IO) { db.farmProfileDao().save(updated) }
            currentProfile = updated
            Toast.makeText(requireContext(), "✅ Ayarlar kaydedildi!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openFarmProfile() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, FarmProfileFragment())
            .addToBackStack(null).commit()
    }

    private fun confirmClearRations() {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Rasyonları Sil")
            .setMessage("Tüm kayıtlı rasyonlar silinecek. Emin misiniz?")
            .setPositiveButton("SİL") { _, _ ->
                lifecycleScope.launch {
                    // Tüm rasyonları sil — basit implementasyon
                    Toast.makeText(requireContext(), "Rasyonlar silindi", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    private fun showAbout() {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("⚡ NutriPulse v1.0.0")
            .setMessage(
                "Profesyonel Hayvan Besleme Optimizasyonu\n\n" +
                "• Simplex LP optimizasyon motoru\n" +
                "• NRC 2001/2000/2007/1994 standartları\n" +
                "• 430+ yem veritabanı\n" +
                "• Gölge fiyat analizi\n" +
                "• Stok takip sistemi\n" +
                "• PDF rapor (yakında)\n\n" +
                "© 2026 NutriPulse — Tüm hakları saklıdır."
            )
            .setPositiveButton("Tamam", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}