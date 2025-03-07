package ru.practicum.android.diploma.filter.ui

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import org.koin.androidx.viewmodel.ext.android.viewModel
import ru.practicum.android.diploma.R
import ru.practicum.android.diploma.common.util.debounce
import ru.practicum.android.diploma.databinding.FragmentAreaChooserBinding
import ru.practicum.android.diploma.filter.domain.models.Area
import ru.practicum.android.diploma.filter.presentation.states.AreaChooserScreenState
import ru.practicum.android.diploma.filter.presentation.AreaViewModel
import ru.practicum.android.diploma.filter.ui.adapters.AreaAdapter

class AreaChooserFragment : Fragment(R.layout.fragment_area_chooser) {

    private var _binding: FragmentAreaChooserBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AreaViewModel by viewModel()
    private var isClickAllowed = true

    private val adapter = AreaAdapter {
        if (clickDebounce()) {
            viewModel.areaSelected(it)
        }
    }

    private val onAreaClickDebounce = debounce<Boolean>(
        CLICK_DEBOUNCE_DELAY,
        lifecycleScope,
        false
    ) { param ->
        isClickAllowed = param
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAreaChooserBinding.bind(view)

        setRv()
        setListeners()
        setObservables()
    }

    private fun setRv() {
        binding.rvArea.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding.rvArea.adapter = adapter
    }

    private fun setListeners() {
        binding.searchEditText.doOnTextChanged { text, _, _, _ ->
            if (text.isNullOrEmpty()) {
                binding.searchLayoutField.apply {
                    setEndIconDrawable(R.drawable.ic_search)
                    tag = R.drawable.ic_search
                }
            } else {
                binding.searchLayoutField.apply {
                    setEndIconDrawable(R.drawable.ic_clear)
                    tag = R.drawable.ic_clear
                }
            }
            viewModel.searchDebounce(text.toString())
        }

        binding.searchLayoutField.setEndIconOnClickListener {
            if (binding.searchLayoutField.tag == R.drawable.ic_clear) {
                binding.searchEditText.setText("")
            }
        }
    }

    private fun setObservables() {
        viewModel.state.observe(viewLifecycleOwner) { state ->
            renderState(state)
        }

        viewModel.closeEvent.observe(viewLifecycleOwner) { close ->
            if (close) {
                findNavController().navigateUp()
            }
        }
    }

    private fun renderState(state: AreaChooserScreenState) {
        when (state) {
            is AreaChooserScreenState.Loading -> {
                showLoading()
            }

            is AreaChooserScreenState.Empty -> {
                showEmpty()
            }

            is AreaChooserScreenState.Content -> {
                showFoundAreas(state.items)
            }

            is AreaChooserScreenState.Error -> {
                showError()
            }
        }
    }

    private fun showLoading() {
        binding.rvArea.isVisible = false
        binding.placeholderImage.isVisible = false
        binding.placeholderMessage.isVisible = false
        binding.progressBar.isVisible = true
    }

    private fun showEmpty() {
        binding.placeholderImage.setImageResource(R.drawable.search_fail_placeholder)
        binding.placeholderMessage.text = getString(R.string.nothing_found_choosing_area_tv)
        binding.placeholderImage.contentDescription = getString(R.string.nothing_found_choosing_area_tv)
        binding.rvArea.isVisible = false
        binding.placeholderImage.isVisible = true
        binding.placeholderMessage.isVisible = true
        binding.progressBar.isVisible = false
    }

    private fun showFoundAreas(areas: List<Area>) {
        adapter.updateData(areas)
        binding.rvArea.scrollToPosition(0)
        binding.rvArea.isVisible = true
        binding.placeholderImage.isVisible = false
        binding.placeholderMessage.isVisible = false
        binding.progressBar.isVisible = false
    }

    private fun showError() {
        binding.placeholderImage.setImageResource(R.drawable.region_search_fail_placeholder)
        binding.placeholderMessage.text = getString(R.string.search_throwable_choosing_area_tv)
        binding.placeholderImage.contentDescription = getString(R.string.search_throwable_choosing_area_tv)
        binding.rvArea.isVisible = false
        binding.placeholderImage.isVisible = true
        binding.placeholderMessage.isVisible = true
        binding.progressBar.isVisible = false
    }

    private fun clickDebounce(): Boolean {
        val current = isClickAllowed
        if (isClickAllowed) {
            isClickAllowed = false
            onAreaClickDebounce(true)
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

