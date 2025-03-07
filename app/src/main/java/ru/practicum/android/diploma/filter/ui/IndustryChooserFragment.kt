package ru.practicum.android.diploma.filter.ui

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import org.koin.androidx.viewmodel.ext.android.viewModel
import ru.practicum.android.diploma.R
import ru.practicum.android.diploma.databinding.FragmentIndustryChooserBinding
import ru.practicum.android.diploma.filter.presentation.states.IndustryChooserScreenState
import ru.practicum.android.diploma.filter.presentation.IndustryViewModel
import ru.practicum.android.diploma.filter.presentation.models.IndustryUi
import ru.practicum.android.diploma.filter.ui.adapters.IndustryAdapter

class IndustryChooserFragment : Fragment(R.layout.fragment_industry_chooser) {

    private var _binding: FragmentIndustryChooserBinding? = null
    private val binding get() = _binding!!

    private val viewModel: IndustryViewModel by viewModel()

    private val adapter = IndustryAdapter { item ->
        viewModel.industrySelected(item)
        hideSoftKeyboard()
        scrollListUp = false
    }
    private var scrollListUp = true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentIndustryChooserBinding.bind(view)

        binding.rvIndustry.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding.rvIndustry.adapter = adapter
        binding.rvIndustry.itemAnimator = null

        viewModel.state.observe(viewLifecycleOwner) { state ->
            renderState(state)
        }

        setListeners()
    }

    private fun setListeners() {
        binding.btApply.setOnClickListener {
            viewModel.saveFilterToLocalStorage()
            findNavController().navigateUp()
        }

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
            scrollListUp = true
            viewModel.applySearchResults(text.toString())
        }

        binding.searchLayoutField.setEndIconOnClickListener {
            if (binding.searchLayoutField.tag == R.drawable.ic_clear) {
                binding.searchEditText.text?.clear()
                binding.searchEditText.requestFocus()
                scrollListUp = true
            }
        }
    }

    private fun renderState(state: IndustryChooserScreenState) {
        when (state) {
            is IndustryChooserScreenState.Content -> showContent(state.items)
            is IndustryChooserScreenState.Error -> showError()
            is IndustryChooserScreenState.Loading -> showLoading()
        }
    }

    private fun showLoading() {
        updateScreenViews(
            isRvIndustry = false,
            isRecyclerViewProgressBar = true,
            isPlaceholderImage = false,
            isPlaceholderMessage = false,
            isBtApply = false
        )
    }

    private fun showError() {
        updateScreenViews(
            isRvIndustry = false,
            isRecyclerViewProgressBar = false,
            isPlaceholderImage = true,
            isPlaceholderMessage = true,
            isBtApply = false
        )
    }

    private fun showContent(items: List<IndustryUi>) {
        adapter.updateData(items)
        if (scrollListUp) {
            binding.rvIndustry.scrollToPosition(0)
        }
        updateScreenViews(
            isRvIndustry = true,
            isRecyclerViewProgressBar = false,
            isPlaceholderImage = false,
            isPlaceholderMessage = false,
            isBtApply = items.any { it.isChecked }
        )
    }

    private fun updateScreenViews(
        isRvIndustry: Boolean,
        isRecyclerViewProgressBar: Boolean,
        isPlaceholderImage: Boolean,
        isPlaceholderMessage: Boolean,
        isBtApply: Boolean
    ) {
        with(binding) {
            rvIndustry.isVisible = isRvIndustry
            recyclerViewProgressBar.isVisible = isRecyclerViewProgressBar
            placeholderImage.isVisible = isPlaceholderImage
            placeholderMessage.isVisible = isPlaceholderMessage
            btApply.isVisible = isBtApply
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun hideSoftKeyboard() {
        val inputMethodManager =
            requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        inputMethodManager?.hideSoftInputFromWindow(binding.searchEditText.windowToken, 0)
        binding.searchEditText.isEnabled = true
    }
}
