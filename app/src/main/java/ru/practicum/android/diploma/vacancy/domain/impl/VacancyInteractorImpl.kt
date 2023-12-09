package ru.practicum.android.diploma.vacancy.domain.impl

import kotlinx.coroutines.flow.Flow
import ru.practicum.android.diploma.common.util.Result
import ru.practicum.android.diploma.vacancy.domain.api.VacancyInteractor
import ru.practicum.android.diploma.vacancy.domain.api.VacancyRepository
import ru.practicum.android.diploma.vacancy.domain.models.Vacancy

class VacancyInteractorImpl(private val vacancyRepository: VacancyRepository) : VacancyInteractor {
    override fun getVacancy(vacancyId: String): Flow<Result<Vacancy>> {
        return vacancyRepository.getVacancy(vacancyId)
    }
}
