package ru.practicum.android.diploma.filter.presentation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import ru.practicum.android.diploma.common.util.NetworkResult
import ru.practicum.android.diploma.filter.domain.api.FilterInteractor
import ru.practicum.android.diploma.filter.domain.models.Country
import ru.practicum.android.diploma.filter.domain.models.FilterParameters
import ru.practicum.android.diploma.filter.presentation.states.CountryChooserScreenState

class CountryViewModel(private val interactor: FilterInteractor) : ViewModel() {

    private val loadedFilter: FilterParameters = interactor.getCurrentFilter()

    private val _state = MutableLiveData<CountryChooserScreenState>()
    val state: LiveData<CountryChooserScreenState> get() = _state

    init {
        viewModelScope.launch {
            interactor.getCountries().collect {
                processResult(result = it)
            }
        }
    }

    private fun processResult(result: NetworkResult<List<Country>>) {
        when (result) {
            is NetworkResult.Success -> {
                val data = sorting(result.data!!)
                _state.postValue(CountryChooserScreenState.Content(data))
            }

            is NetworkResult.Error -> {
                _state.postValue(CountryChooserScreenState.Error)
            }
        }
    }

    fun countrySelected(country: Country) {
        if (country != loadedFilter.country) {
            interactor.updateFilter(loadedFilter.copy(country = country, area = null))
        }
    }

    private fun sorting(countries: List<Country>): List<Country> {
        val newList = countries.toMutableList()
        val others = countries.maxWith(Comparator.comparingInt { it.id.length })
        newList.remove(others)
        newList.add(others)
        return newList
    }
}
