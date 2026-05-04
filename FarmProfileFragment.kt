package com.nutripulse.app.ui.settings

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.nutripulse.app.data.AppDatabase
import com.nutripulse.app.data.model.FarmProfile
import com.nutripulse.app.data.model.FarmType
import com.nutripulse.app.databinding.FragmentFarmProfileBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FarmProfileFragment : Fragment() {

    private var _binding: FragmentFarmProfileBinding? = null
    private val binding get() = _binding!!
    private var selectedFarmType = FarmType.BUYUKBAS
    private var existing: FarmProfile? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFarmProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupTypeButtons()
        loadProfile()

        binding.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.btnSave.setOnClickListener { saveProfile() }
        binding.btnActivate.setOnClickListener { activateLicense() }
    }

    private fun setupTypeButtons() {
        val buttons = listOf(
            binding.btnTypeBuyukbas to FarmType.BUYUKBAS,
            binding.btnTypeKucukbas to FarmType.KUCUKBAS,
            binding.btnTypeKanatli  to FarmType.KANATLI,
            binding.btnTypeKarma    to FarmType.KARMA
        )
        buttons.forEach { (btn, type) ->
            btn.setOnClickListener {
                selectedFarmType = type
                buttons.forEach { (b, _) ->
                    b.setBackgroundResource(com.nutripulse.app.R.drawable.bg_feed_item)
                    (b.getChildAt(1) as? TextView)?.setTextColor(Color.parseColor("#C0E8D0"))
                }
                btn.setBackgroundResource(com.nutripulse.app.R.drawable.bg_stat_green)
                (btn.getChildAt(1) as? TextView)?.setTextColor(Color.parseColor("#00FF88"))
            }
        }
        // Varsayılan seçim
        selectType(FarmType.BUYUKBAS)
    }

    private fun selectType(type: String) {
        selectedFarmType = type
        val map = mapOf(
            FarmType.BUYUKBAS to binding.btnTypeBuyukbas,
            FarmType.KUCUKBAS to binding.btnTypeKucukbas,
            FarmType.KANATLI  to binding.btnTypeKanatli,
            FarmType.KARMA    to binding.btnTypeKarma
        )
        map.forEach { (t, btn) ->
            if (t == type) {
                btn.setBackgroundResource(com.nutripulse.app.R.drawable.bg_stat_green)
                (btn.getChildAt(1) as? TextView)?.setTextColor(Color.parseColor("#00FF88"))
            } else {
                btn.setBackgroundResource(com.nutripulse.app.R.drawable.bg_feed_item)
                (btn.getChildAt(1) as? TextView)?.setTextColor(Color.parseColor("#C0E8D0"))
            }
        }
    }

    private fun loadProfile() {
        lifecycleScope.launch {
            val profile = withContext(Dispatchers.IO) {
                AppDatabase.getDatabase(requireContext()).farmProfileDao().getProfileSync()
            }
            profile?.let {
                existing = it
                fillForm(it)
            }
        }
    }

    private fun fillForm(p: FarmProfile) {
        binding.etFarmName.setText(p.farmName)
        binding.etOwnerName.setText(p.ownerName)
        binding.etPhone.setText(p.phone)
        binding.etEmail.setText(p.email)
        binding.etCity.setText(p.city)
        binding.etDistrict.setText(p.district)
        binding.etAddress.setText(p.address)
        if (p.totalAnimals > 0) binding.etTotalAnimals.setText(p.totalAnimals.toString())
        if (p.milkingCows > 0)  binding.etMilkingCows.setText(p.milkingCows.toString())
        if (p.dryCoows > 0)     binding.etDryCows.setText(p.dryCoows.toString())
        if (p.heifers > 0)      binding.etHeifers.setText(p.heifers.toString())
        if (p.calves > 0)       binding.etCalves.setText(p.calves.toString())
        if (p.dailyMilkKg > 0)  binding.etDailyMilk.setText(p.dailyMilkKg.toString())
        binding.etMilkPrice.setText(p.milkPriceTl.toString())
        binding.etMilkFat.setText(p.milkFatTarget.toString())
        binding.etMilkProtein.setText(p.milkProteinTarget.toString())
        if (p.licenseKey.isNotEmpty()) binding.etLicenseKey.setText(p.licenseKey)
        if (p.notes.isNotEmpty()) binding.etNotes.setText(p.notes)
        selectType(p.farmType.ifEmpty { FarmType.BUYUKBAS })
    }

    private fun saveProfile() {
        val farmName = binding.etFarmName.text.toString().trim()
        val ownerName = binding.etOwnerName.text.toString().trim()

        if (farmName.isEmpty()) {
            Toast.makeText(requireContext(), "Çiftlik adı zorunlu!", Toast.LENGTH_SHORT).show()
            return
        }

        val phone = binding.etPhone.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val city = binding.etCity.text.toString().trim()
        val district = binding.etDistrict.text.toString().trim()
        val address = binding.etAddress.text.toString().trim()
        val totalAnimals = binding.etTotalAnimals.text.toString().toIntOrNull() ?: 0
        val milkingCows = binding.etMilkingCows.text.toString().toIntOrNull() ?: 0
        val dryCows = binding.etDryCows.text.toString().toIntOrNull() ?: 0
        val heifers = binding.etHeifers.text.toString().toIntOrNull() ?: 0
        val calves = binding.etCalves.text.toString().toIntOrNull() ?: 0
        val dailyMilkKg = binding.etDailyMilk.text.toString().toDoubleOrNull() ?: 0.0
        val milkPriceTl = binding.etMilkPrice.text.toString().toDoubleOrNull() ?: 18.0
        val milkFatTarget = binding.etMilkFat.text.toString().toDoubleOrNull() ?: 3.5
        val milkProteinTarget = binding.etMilkProtein.text.toString().toDoubleOrNull() ?: 3.2
        val licenseKey = binding.etLicenseKey.text.toString().trim()
        val notes = binding.etNotes.text.toString()

        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(requireContext())
            val base = withContext(Dispatchers.IO) { db.farmProfileDao().getProfileSync() } ?: existing ?: FarmProfile()
            val profile = base.copy(
                farmName = farmName,
                ownerName = ownerName,
                phone = phone,
                email = email,
                city = city,
                district = district,
                address = address,
                farmType = selectedFarmType,
                totalAnimals = totalAnimals,
                milkingCows = milkingCows,
                dryCoows = dryCows,
                heifers = heifers,
                calves = calves,
                dailyMilkKg = dailyMilkKg,
                milkPriceTl = milkPriceTl,
                milkFatTarget = milkFatTarget,
                milkProteinTarget = milkProteinTarget,
                licenseKey = licenseKey,
                notes = notes,
                updatedAt = System.currentTimeMillis()
            )

            withContext(Dispatchers.IO) {
                db.farmProfileDao().save(profile)
            }
            existing = profile
            Toast.makeText(requireContext(), "✅ Çiftlik profili kaydedildi!", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
        }
    }

    private fun activateLicense() {
        val key = binding.etLicenseKey.text.toString().trim()
        if (key.isEmpty()) {
            Toast.makeText(requireContext(), "Lisans anahtarı girin!", Toast.LENGTH_SHORT).show()
            return
        }
        // Gerçek uygulamada sunucu doğrulaması yapılır
        if (key.startsWith("NP-PRO-")) {
            Toast.makeText(requireContext(), "✅ Pro lisans aktive edildi!", Toast.LENGTH_LONG).show()
        } else if (key.startsWith("NP-ENT-")) {
            Toast.makeText(requireContext(), "✅ Kurumsal lisans aktive edildi!", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(requireContext(), "❌ Geçersiz lisans anahtarı", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}