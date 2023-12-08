package ru.practicum.android.diploma.search.domain.impl

import kotlinx.coroutines.flow.Flow
import ru.practicum.android.diploma.common.util.Result
import ru.practicum.android.diploma.filter.domain.models.FilterParameters
import ru.practicum.android.diploma.search.domain.api.SearchInteractor
import ru.practicum.android.diploma.search.domain.api.SearchRepository
import ru.practicum.android.diploma.search.domain.model.VacancyListData

class SearchInteractorImpl(private val repository: SearchRepository) : SearchInteractor {
    override fun searchVacancies(text: String, options: FilterParameters): Flow<Result<VacancyListData>> {
        return repository.searchVacancies(text, options)
    }
}
