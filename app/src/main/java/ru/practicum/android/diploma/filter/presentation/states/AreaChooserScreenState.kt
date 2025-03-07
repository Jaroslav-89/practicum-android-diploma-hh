package ru.practicum.android.diploma.filter.presentation.states

import ru.practicum.android.diploma.filter.domain.models.Area

sealed interface AreaChooserScreenState {
    data class Content(
        val items: List<Area>,
    ) : AreaChooserScreenState

    data object Empty : AreaChooserScreenState
    data object Loading : AreaChooserScreenState
    data object Error : AreaChooserScreenState
}
