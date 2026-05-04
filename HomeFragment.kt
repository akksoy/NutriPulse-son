package com.nutripulse.app

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.nutripulse.app.data.AppDatabase
import com.nutripulse.app.databinding.FragmentHomeBinding
import com.nutripulse.app.ui.animal.AnimalListFragment
import com.nutripulse.app.ui.feed.FeedFragment
import com.nutripulse.app.ui.ration.RationStudioFragment
import com.nutripulse.app.ui.report.ReportFragment
import com.nutripulse.app.ui.settings.FarmProfileFragment
import com.nutripulse.app.ui.settings.SettingsFragment
import com.nutripulse.app.ui.stock.StockFragment

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupModuleCards()
        setupToggles()
        startBlink()
        observeCounts()
    }

    private fun setupModuleCards() {
        binding.cardAnimal.setOnClickListener   { navigate(AnimalListFragment()) }
        binding.cardFeed.setOnClickListener     { navigate(FeedFragment()) }
        binding.cardRation.setOnClickListener   { navigate(RationStudioFragment()) }
        binding.cardReport.setOnClickListener   { navigate(ReportFragment()) }
        binding.cardStock.setOnClickListener    { navigate(StockFragment()) }
        binding.cardSettings.setOnClickListener { navigate(SettingsFragment()) }
        binding.cardFarmProfile.setOnClickListener { navigate(FarmProfileFragment()) }
        binding.btnAddFarmProfile.setOnClickListener { navigate(FarmProfileFragment()) }
    }

    private fun navigate(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null).commit()
    }

    private fun observeCounts() {
        val db = AppDatabase.getDatabase(requireContext())
        db.feedDao().getFeedCount().observe(viewLifecycleOwner) { count ->
            binding.tvRasyonCount.text = "$count"
        }
    }

    private fun setupToggles() {
        binding.headerSigir.setOnClickListener       { toggle(binding.subSigir,       binding.arrowSigir) }
        binding.headerManda.setOnClickListener       { toggle(binding.subManda,       binding.arrowManda) }
        binding.headerKoyun.setOnClickListener       { toggle(binding.subKoyun,       binding.arrowKoyun) }
        binding.headerKanatli.setOnClickListener     { toggle(binding.subKanatli,     binding.arrowKanatli) }
        binding.headerSuUrunleri.setOnClickListener  { toggle(binding.subSuUrunleri,  binding.arrowSuUrunleri) }
        binding.headerTekTirnakli.setOnClickListener { toggle(binding.subTekTirnakli, binding.arrowTekTirnakli) }
        binding.headerDiger.setOnClickListener       { toggle(binding.subDiger,       binding.arrowDiger) }
    }

    private fun toggle(sub: View, arrow: TextView) {
        if (sub.visibility == View.GONE) { sub.visibility = View.VISIBLE; arrow.text = " ▼" }
        else { sub.visibility = View.GONE; arrow.text = " ▶" }
    }

    private fun startBlink() {
        ObjectAnimator.ofFloat(binding.viewBlink, "alpha", 1f, 0.1f).apply {
            duration = 900; repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE; start()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
