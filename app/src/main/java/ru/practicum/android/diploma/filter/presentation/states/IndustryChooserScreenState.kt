package ru.practicum.android.diploma.filter.presentation.states

import ru.practicum.android.diploma.filter.presentation.models.IndustryUi

sealed interface IndustryChooserScreenState {
    data class Content(
        val items: List<IndustryUi>,
    ) : IndustryChooserScreenState

    data object Error : IndustryChooserScreenState
    data object Loading : IndustryChooserScreenState
}
