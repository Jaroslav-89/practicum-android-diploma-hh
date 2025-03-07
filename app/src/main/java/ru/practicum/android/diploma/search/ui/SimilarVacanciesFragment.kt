package ru.practicum.android.diploma.search.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import ru.practicum.android.diploma.R
import ru.practicum.android.diploma.common.util.ErrorType
import ru.practicum.android.diploma.common.util.debounce
import ru.practicum.android.diploma.databinding.FragmentSimilarVacanciesBinding
import ru.practicum.android.diploma.search.domain.model.VacancyListData
import ru.practicum.android.diploma.search.presentation.SimilarVacanciesScreenState
import ru.practicum.android.diploma.search.presentation.SimilarVacanciesViewModel
import ru.practicum.android.diploma.search.ui.adapter.SearchAdapter

class SimilarVacanciesFragment : Fragment(R.layout.fragment_similar_vacancies) {

    private var _binding: FragmentSimilarVacanciesBinding? = null
    private val binding get() = _binding!!

    private var isClickAllowed = true

    private val args: SimilarVacanciesFragmentArgs by navArgs()
    private val adapter = SearchAdapter {
        if (clickDebounce()) {
            val action = SimilarVacanciesFragmentDirections.actionSimilarVacanciesFragmentToVacancyFragment(it.id)
            findNavController().navigate(action)
        }
    }

    private val viewModel by viewModel<SimilarVacanciesViewModel> {
        parametersOf(args.vacancyId)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSimilarVacanciesBinding.bind(view)

        setListeners()
        addRvAdapter()
        setObservables()
    }

    private fun setListeners() {
        binding.rvSimilar.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy > 0) {
                    val pos = (binding.rvSimilar.layoutManager as LinearLayoutManager).findLastVisibleItemPosition()
                    val itemsCount = adapter.itemCount
                    if (pos >= itemsCount - 1) {
                        viewModel.onLastItemReached()
                    }
                }
            }
        })
    }

    private fun setObservables() {
        viewModel.state.observe(viewLifecycleOwner) { state ->
            render(state)
        }

        viewModel.toastEvent.observe(viewLifecycleOwner) { error ->
            when (error) {
                ErrorType.NO_INTERNET -> showToast(getString(R.string.toast_internet_throwable))
                else -> showToast(getString(R.string.toast_unknown_error))
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    private fun addRvAdapter() {
        binding.rvSimilar.adapter = adapter
        binding.rvSimilar.setHasFixedSize(false)
    }

    private fun render(state: SimilarVacanciesScreenState) {
        when (state) {
            is SimilarVacanciesScreenState.Content -> showContent(state.vacancyData, state.isLoading)
            is SimilarVacanciesScreenState.Loading -> showLoading()
            is SimilarVacanciesScreenState.Empty -> showEmpty()
            is SimilarVacanciesScreenState.Error -> showError(state.error)
        }
    }

    private fun showError(error: ErrorType) {
        if (error == ErrorType.NO_INTERNET) {
            binding.placeholderImage.setImageResource(R.drawable.placeholder_no_internet)
            binding.placeholderMessage.text = getString(R.string.internet_throwable_tv)
        } else {
            binding.placeholderImage.setImageResource(R.drawable.placeholder_error_server)
            binding.placeholderMessage.text = getString(R.string.server_throwable_tv)
        }
        updateScreenViews(
            isMainProgressVisible = false,
            isSimilarRvVisible = false,
            isPlaceholderVisible = true,
            isNextPageLoadingVisible = false
        )
    }

    private fun showEmpty() = with(binding) {
        binding.placeholderImage.setImageResource(R.drawable.placeholder_empty_result)
        binding.placeholderMessage.text = getString(R.string.load_vacancy_throwable_tv)
        updateScreenViews(
            isSimilarRvVisible = false,
            isMainProgressVisible = false,
            isPlaceholderVisible = true,
            isNextPageLoadingVisible = false
        )
    }

    private fun showLoading() {
        updateScreenViews(
            isSimilarRvVisible = false,
            isMainProgressVisible = true,
            isPlaceholderVisible = false,
            isNextPageLoadingVisible = false
        )
    }

    private fun showContent(foundVacancyData: VacancyListData, isPageLoading: Boolean) {
        adapter.submitList(foundVacancyData.items)
        if (foundVacancyData.page == 0) {
            binding.rvSimilar.scrollToPosition(0)
        }
        updateScreenViews(
            isMainProgressVisible = false,
            isSimilarRvVisible = true,
            isPlaceholderVisible = false,
            isNextPageLoadingVisible = isPageLoading
        )
        if (isPageLoading) {
            binding.rvSimilar.scrollToPosition(adapter.itemCount - 1)
        }
    }

    private fun updateScreenViews(
        isSimilarRvVisible: Boolean,
        isMainProgressVisible: Boolean,
        isPlaceholderVisible: Boolean,
        isNextPageLoadingVisible: Boolean
    ) {
        with(binding) {
            mainProgressBar.isVisible = isMainProgressVisible
            placeholderImage.isVisible = isPlaceholderVisible
            placeholderMessage.isVisible = isPlaceholderVisible
            rvSimilar.isVisible = isSimilarRvVisible
            recyclerViewProgressBar.isVisible = isNextPageLoadingVisible
        }
    }

    private val onClickDebounce = debounce<Boolean>(
        CLICK_DEBOUNCE_DELAY,
        lifecycleScope,
        false
    ) { param ->
        isClickAllowed = param
    }

    private fun clickDebounce(): Boolean {
        val current = isClickAllowed
        if (isClickAllowed) {
            isClickAllowed = false
            onClickDebounce(true)
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
