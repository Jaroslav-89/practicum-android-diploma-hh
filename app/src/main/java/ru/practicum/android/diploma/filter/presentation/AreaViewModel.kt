package ru.practicum.android.diploma.filter.presentation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.practicum.android.diploma.common.util.NetworkResult
import ru.practicum.android.diploma.common.util.SingleLiveEvent
import ru.practicum.android.diploma.common.util.debounce
import ru.practicum.android.diploma.filter.domain.api.FilterInteractor
import ru.practicum.android.diploma.filter.domain.models.Area
import ru.practicum.android.diploma.filter.domain.models.Country
import ru.practicum.android.diploma.filter.domain.models.FilterParameters
import ru.practicum.android.diploma.filter.presentation.states.AreaChooserScreenState

class AreaViewModel(private val interactor: FilterInteractor) : ViewModel() {

    private var areaList = emptyList<Area>()
    private var loadedFilter = FilterParameters()
    private var countriesList = emptyList<Country>()

    val searchDebounce = debounce<String>(
        SEARCH_DEBOUNCE_DELAY_IN_MILLIS,
        viewModelScope,
        true
    ) { changedText ->
        filterList(changedText)
    }

    private val _state = MutableLiveData<AreaChooserScreenState>(AreaChooserScreenState.Loading)
    val state: LiveData<AreaChooserScreenState> get() = _state

    private val _closeEvent = SingleLiveEvent<Boolean>()
    val closeEvent: LiveData<Boolean> get() = _closeEvent

    init {
        loadedFilter = interactor.getCurrentFilter()

        viewModelScope.launch {
            if (loadedFilter.country != null) {
                interactor.getAreas(loadedFilter.country?.id!!).collect {
                    loadAreaProcessResult(result = it)
                }
            } else {
                interactor.getAreas().collect {
                    loadAreaProcessResult(result = it)
                }
                interactor.getCountries().collect {
                    loadCountriesProcessResult(result = it)
                }
            }
        }
    }

    fun areaSelected(area: Area) {
        viewModelScope.launch {
            if (area != loadedFilter.area) {
                _state.postValue(AreaChooserScreenState.Loading)
                val filter: FilterParameters = if (loadedFilter.country == null) {
                    val country = getCountryForArea(area)
                    loadedFilter.copy(area = area, country = country)
                } else {
                    loadedFilter.copy(area = area)
                }
                interactor.updateFilter(filter)
            }
            _closeEvent.postValue(true)
        }
    }

    private fun filterList(text: String) =
        viewModelScope.launch(Dispatchers.Default) {
            if (text.isNotBlank()) {
                val newList = areaList.filter { area ->
                    area.name.contains(text, true)
                }
                if (newList.isEmpty()) {
                    _state.postValue(AreaChooserScreenState.Empty)
                } else {
                    _state.postValue(AreaChooserScreenState.Content(newList))
                }
            } else {
                _state.postValue(AreaChooserScreenState.Content(areaList))
            }
        }

    private fun loadAreaProcessResult(result: NetworkResult<List<Area>>) {
        when (result) {
            is NetworkResult.Success -> {
                val data = result.data!!
                if (data.isEmpty()) {
                    _state.postValue(AreaChooserScreenState.Empty)
                } else {
                    areaList = data.sortedBy { it.id.toInt() }
                    _state.postValue(AreaChooserScreenState.Content(areaList))
                }
            }

            is NetworkResult.Error -> {
                _state.postValue(AreaChooserScreenState.Error)
            }
        }
    }

    private fun loadCountriesProcessResult(result: NetworkResult<List<Country>>) {
        when (result) {
            is NetworkResult.Success -> {
                countriesList = result.data!!
            }

            is NetworkResult.Error -> {
                _state.postValue(AreaChooserScreenState.Error)
            }
        }
    }

    private fun getCountryForArea(selectedArea: Area): Country {
        val parentId = getParentCountryId(selectedArea)
        return getCountryById(parentId)
    }

    private fun getParentCountryId(selectedArea: Area): String {
        val countriesId = countriesList.map { it.id }
        var parentId = selectedArea.parentId
        while (!countriesId.contains(parentId)) {
            parentId = areaList.firstOrNull { it.id == parentId }?.parentId ?: ""
        }
        return parentId
    }

    private fun getCountryById(parentId: String): Country {
        return countriesList.first { it.id == parentId }
    }

    companion object {
        private const val SEARCH_DEBOUNCE_DELAY_IN_MILLIS = 500L
    }
}
