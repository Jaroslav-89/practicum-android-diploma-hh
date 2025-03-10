package ru.practicum.android.diploma.search.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import ru.practicum.android.diploma.common.data.network.NetworkClient
import ru.practicum.android.diploma.common.data.network.dto.Request
import ru.practicum.android.diploma.common.data.storage.FilterStorage
import ru.practicum.android.diploma.common.mappers.FilterMapper
import ru.practicum.android.diploma.common.util.NetworkResult
import ru.practicum.android.diploma.filter.domain.models.FilterParameters
import ru.practicum.android.diploma.search.data.dto.VacancySearchResponse
import ru.practicum.android.diploma.search.data.mapper.VacancyResponseMapper
import ru.practicum.android.diploma.search.domain.api.SearchRepository
import ru.practicum.android.diploma.search.domain.model.SearchQuery
import ru.practicum.android.diploma.search.domain.model.VacancyListData

class SearchRepositoryImpl(
    private val networkClient: NetworkClient,
    private val vacancyMapper: VacancyResponseMapper,
    private val filterStorage: FilterStorage,
    private val filterMapper: FilterMapper
) : SearchRepository {

    override fun searchVacanciesPaged(
        searchQuery: SearchQuery
    ): Flow<NetworkResult<VacancyListData>> = flow {
        val filter = filterMapper.mapToDomain(filterStorage.getFilterParameters())
        val request = Request.VacancySearchRequest(prepareSearchQueryMap(searchQuery, filter))

        when (val result = networkClient.doRequest(request)) {
            is NetworkResult.Success -> {
                val data = vacancyMapper.mapDtoToModel(result.data as VacancySearchResponse)
                emit(NetworkResult.Success(data))
            }

            is NetworkResult.Error -> {
                emit(NetworkResult.Error(result.errorType!!))
            }
        }
    }

    override fun getSimilarVacanciesPaged(
        vacancyId: String,
        paging: SearchQuery
    ): Flow<NetworkResult<VacancyListData>> = flow {
        val filter = filterMapper.mapToDomain(filterStorage.getFilterParameters())
        val request = Request.SimilarVacancyRequest(vacancyId, prepareSearchQueryMap(paging, filter))

        when (val result = networkClient.doRequest(request)) {
            is NetworkResult.Success -> {
                val data = vacancyMapper.mapDtoToModel(result.data as VacancySearchResponse)
                emit(NetworkResult.Success(data))
            }

            is NetworkResult.Error -> {
                emit(NetworkResult.Error(result.errorType!!))
            }
        }
    }

    override fun isFilterActive(): Boolean {
        return filterStorage.isFilterActive()
    }

    private fun prepareSearchQueryMap(
        searchQuery: SearchQuery,
        filter: FilterParameters
    ): Map<String, String> {
        val map: HashMap<String, String> = HashMap()
        if (searchQuery.text != null) {
            map["text"] = searchQuery.text!!
        }
        map["page"] = searchQuery.page.toString()
        map["per_page"] = searchQuery.perPage.toString()

        if (filter.area != null) {
            map["area"] = filter.area.id
        } else {
            if (filter.country != null) {
                map["area"] = filter.country.id
            }
        }
        if (filter.industry != null) {
            map["industry"] = filter.industry.id
        }
        if (filter.salary != null) {
            map["salary"] = filter.salary.toString()
        }
        if (filter.onlyWithSalary) {
            map["only_with_salary"] = "true"
        }
        return map
    }
}
