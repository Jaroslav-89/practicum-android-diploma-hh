package ru.practicum.android.diploma.filter.ui

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import org.koin.androidx.viewmodel.ext.android.viewModel
import ru.practicum.android.diploma.R
import ru.practicum.android.diploma.common.util.debounce
import ru.practicum.android.diploma.databinding.FragmentCountryChooserBinding
import ru.practicum.android.diploma.filter.presentation.states.CountryChooserScreenState
import ru.practicum.android.diploma.filter.presentation.CountryViewModel
import ru.practicum.android.diploma.filter.ui.adapters.CountryAdapter

class CountryChooserFragment : Fragment(R.layout.fragment_country_chooser) {

    private var _binding: FragmentCountryChooserBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CountryViewModel by viewModel()
    private var isClickAllowed = true

    private val adapter = CountryAdapter {
        if (clickDebounce()) {
            viewModel.countrySelected(it)
            findNavController().navigateUp()
        }
    }

    private val onCountryClickDebounce = debounce<Boolean>(
        CLICK_DEBOUNCE_DELAY,
        lifecycleScope,
        false
    ) { param ->
        isClickAllowed = param
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentCountryChooserBinding.bind(view)

        binding.rvCountry.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding.rvCountry.adapter = adapter

        viewModel.state.observe(viewLifecycleOwner) { state ->
            renderState(state)
        }
    }

    private fun renderState(state: CountryChooserScreenState) {
        when (state) {
            is CountryChooserScreenState.Content -> {
                adapter.updateData(state.items)
                binding.rvCountry.isVisible = true
                binding.placeholderImage.isVisible = false
                binding.placeholderMessage.isVisible = false
            }

            is CountryChooserScreenState.Error -> {
                binding.rvCountry.isVisible = false
                binding.placeholderImage.isVisible = true
                binding.placeholderMessage.isVisible = true
            }
        }
    }

    private fun clickDebounce(): Boolean {
        val current = isClickAllowed
        if (isClickAllowed) {
            isClickAllowed = false
            onCountryClickDebounce(true)
        }
        return current
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val CLICK_DEBOUNCE_DELAY = 500L
    }
}
