package ru.practicum.android.diploma.filter.presentation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ru.practicum.android.diploma.common.util.debounce
import ru.practicum.android.diploma.filter.domain.api.FilterInteractor
import ru.practicum.android.diploma.filter.domain.models.FilterParameters

class FilterViewModel(private val interactor: FilterInteractor) : ViewModel() {

    private val _state = MutableLiveData<FilterParameters>()
    val state: LiveData<FilterParameters> get() = _state

    private val salaryDebounced = debounce<String>(
        SALARY_UPDATE_DELAY_IN_MILLIS,
        viewModelScope,
        true
    ) { text ->
        val salary = if (text == "") {
            null
        } else {
            text.toInt()
        }
        val filter = state.value!!.copy(salary = salary)
        _state.postValue(filter)
    }

    fun clearWorkplace() {
        val filter = state.value!!.copy(area = null, country = null)
        _state.postValue(filter)
    }

    fun clearIndustry() {
        val filter = state.value!!.copy(industry = null)
        _state.postValue(filter)
    }

    fun clearAll() {
        val filter = FilterParameters()
        _state.postValue(filter)
    }

    fun updateSalary(text: String) {
        salaryDebounced(text)
    }

    fun onlyWithSalaryPressed(isChecked: Boolean) {
        val filter = state.value!!.copy(onlyWithSalary = isChecked)
        _state.postValue(filter)
    }

    fun saveFilterToLocalStorage() {
        val filter = state.value!!
        interactor.updateFilter(filter)
    }

    fun loadFilterFromLocalStorage() {
        val filter = interactor.getCurrentFilter()
        _state.postValue(filter)
    }

    companion object {
        private const val SALARY_UPDATE_DELAY_IN_MILLIS = 200L
    }
}
