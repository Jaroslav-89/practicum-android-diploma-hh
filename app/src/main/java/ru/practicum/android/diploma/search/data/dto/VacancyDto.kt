package ru.practicum.android.diploma.search.data.dto

data class VacancyDto(
    val id: String,
    val name: String,
    val area: VacancyAreaDto,
    val salary: SalaryDto?,
    val employer: EmployerDto
)
