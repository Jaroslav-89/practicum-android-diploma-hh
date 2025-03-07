package ru.practicum.android.diploma.filter.presentation.states

import ru.practicum.android.diploma.filter.domain.models.Country

sealed interface CountryChooserScreenState {
    data class Content(
        val items: List<Country>,
    ) : CountryChooserScreenState

    data object Error : CountryChooserScreenState
}
