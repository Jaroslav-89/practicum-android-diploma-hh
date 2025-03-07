package ru.practicum.android.diploma.filter.presentation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import ru.practicum.android.diploma.common.util.NetworkResult
import ru.practicum.android.diploma.filter.domain.api.FilterInteractor
import ru.practicum.android.diploma.filter.domain.models.Industry
import ru.practicum.android.diploma.filter.presentation.models.IndustryUi
import ru.practicum.android.diploma.filter.presentation.states.IndustryChooserScreenState

class IndustryViewModel(private val interactor: FilterInteractor) : ViewModel() {

    private val _state = MutableLiveData<IndustryChooserScreenState>(IndustryChooserScreenState.Loading)
    val state: LiveData<IndustryChooserScreenState> get() = _state

    private var industriesFromApi = listOf<IndustryUi>()

    init {
        getInitialData()
    }

    private fun getInitialData() {
        renderState(IndustryChooserScreenState.Loading)
        viewModelScope.launch {
            interactor.getIndustries().collect {
                processResult(result = it)
            }
        }
    }

    private fun processResult(result: NetworkResult<List<Industry>>) {
        when (result) {
            is NetworkResult.Success -> {
                val filter = interactor.getCurrentFilter()
                val data = result.data!!.map {
                    IndustryUi(
                        id = it.id,
                        name = it.name,
                        isChecked = it.id == filter.industry?.id
                    )
                }

                industriesFromApi = data
                renderState(IndustryChooserScreenState.Content(data))
            }

            is NetworkResult.Error -> {
                renderState(IndustryChooserScreenState.Error)
            }
        }
    }

    private fun renderState(state: IndustryChooserScreenState) {
        _state.postValue(state)
    }

    fun saveFilterToLocalStorage() {
        val industry = (state.value as IndustryChooserScreenState.Content).items.first { it.isChecked }
        val filter = interactor.getCurrentFilter().copy(
            industry = Industry(
                id = industry.id,
                name = industry.name
            )
        )
        interactor.updateFilter(filter)
    }

    fun industrySelected(industry: IndustryUi) {
        val currentList = state.value as IndustryChooserScreenState.Content
        val updatedList = currentList.items.map {
            IndustryUi(
                id = it.id,
                name = it.name,
                isChecked = it.id == industry.id
            )
        }
        renderState(IndustryChooserScreenState.Content(updatedList))
    }

    fun applySearchResults(search: String?) {
        if (industriesFromApi.isEmpty()) {
            getInitialData()
        } else {
            if (search.isNullOrEmpty()) {
                renderState(IndustryChooserScreenState.Content(industriesFromApi))
            } else {
                val updatedList = industriesFromApi.filter {
                    it.name.contains(search, true)
                }
                renderState(IndustryChooserScreenState.Content(updatedList))
            }
        }
    }
}
