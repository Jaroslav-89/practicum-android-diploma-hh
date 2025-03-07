package ru.practicum.android.diploma.vacancy.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.text.HtmlCompat
import androidx.core.text.HtmlCompat.FROM_HTML_MODE_LEGACY
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterInside
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import ru.practicum.android.diploma.R
import ru.practicum.android.diploma.common.util.Formatter
import ru.practicum.android.diploma.common.util.debounce
import ru.practicum.android.diploma.databinding.FragmentVacancyBinding
import ru.practicum.android.diploma.vacancy.domain.models.Phone
import ru.practicum.android.diploma.vacancy.domain.models.Vacancy
import ru.practicum.android.diploma.vacancy.presentation.VacancyScreenState
import ru.practicum.android.diploma.vacancy.presentation.VacancyViewModel
import ru.practicum.android.diploma.vacancy.ui.adapter.PhonesAdapter

class VacancyFragment : Fragment(R.layout.fragment_vacancy) {

    private var _binding: FragmentVacancyBinding? = null
    private val binding get() = _binding!!

    private val args: VacancyFragmentArgs by navArgs()
    private val menuHost: MenuHost get() = requireActivity()
    private val phonesAdapter = PhonesAdapter(object : PhonesAdapter.PhoneClickListener {
        override fun onPhoneClick(phone: Phone) {
            if (clickDebounce()) {
                viewmodel.makeCall(phone)
            }
        }
    })

    private val viewmodel by viewModel<VacancyViewModel> {
        parametersOf(args.vacancyId)
    }

    private var isFavorite = false
    private var isClickAllowed = true

    private val onTrackClickDebounce = debounce<Boolean>(
        CLICK_DEBOUNCE_DELAY,
        lifecycleScope,
        false
    ) { param ->
        isClickAllowed = param
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentVacancyBinding.bind(view)

        viewmodel.getVacancy(args.vacancyId)

        setListeners()
        setObservables()
        prepareToolbarMenu()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null
    }

    private fun render(state: VacancyScreenState) {
        when (state) {
            is VacancyScreenState.Loading -> showLoading()
            is VacancyScreenState.Error -> showError()
            is VacancyScreenState.InternetThrowable -> showInternetThrowable()
            is VacancyScreenState.Content -> showContent(state.vacancy)
        }
    }

    private fun hideAllView() {
        binding.apply {
            clProgressBar.isVisible = false
            clErrorPlaceholder.isVisible = false
            svVacancy.isVisible = false
            tvContacts.isVisible = false
            tvContactName.isVisible = false
            contactName.isVisible = false
            tvEmail.isVisible = false
            email.isVisible = false
            phoneList.isVisible = false
        }
    }

    private fun showLoading() {
        hideAllView()
        binding.clProgressBar.isVisible = true
    }

    private fun showInternetThrowable() {
        hideAllView()
        binding.errorPlaceholderImage.setImageResource(R.drawable.placeholder_no_internet)
        binding.errorPlaceholderText.setText(R.string.internet_throwable_tv)
        binding.clErrorPlaceholder.isVisible = true
    }

    private fun showError() {
        hideAllView()
        binding.errorPlaceholderImage.setImageResource(R.drawable.placeholder_server_error_cat)
        binding.errorPlaceholderText.setText(R.string.server_throwable_tv)
        binding.clErrorPlaceholder.isVisible = true
    }

    private fun showContent(vacancy: Vacancy) {
        hideAllView()
        binding.apply {
            svVacancy.isVisible = true
            nameVacancy.text = vacancy.vacancyName
            salary.text = Formatter.formatSalary(
                requireContext(),
                vacancy.salaryFrom,
                vacancy.salaryTo,
                vacancy.salaryCurrency
            )

            fillCompanySection(vacancy)

            experience.text = vacancy.experience

            val employmentAndScheduleString = StringBuilder()
                .append(vacancy.employment)

            if (vacancy.employment != null && vacancy.schedule != null) {
                employmentAndScheduleString
                    .append(", ")
            }
            employmentAndScheduleString
                .append(vacancy.schedule)
            schedule.text = employmentAndScheduleString.toString()

            description.text = HtmlCompat.fromHtml(
                vacancy.description,
                FROM_HTML_MODE_LEGACY
            )

            fillKeySkills(vacancy)
            fillContactsSection(vacancy)
        }
    }

    private fun fillCompanySection(vacancy: Vacancy) {
        binding.apply {
            Glide.with(requireContext())
                .load(vacancy.employerLogoUrl)
                .placeholder(R.drawable.vacancy_item_placeholder)
                .transform(
                    CenterInside(),
                    RoundedCorners(
                        resources.getDimensionPixelSize(R.dimen.size_12)
                    ),
                )
                .into(binding.image)

            companyName.text = vacancy.employerName

            if (vacancy.address.isNullOrEmpty()) {
                city.text = vacancy.area
            } else {
                city.text = vacancy.address.toString()
            }
        }
    }

    private fun fillKeySkills(vacancy: Vacancy) {
        binding.apply {
            if (vacancy.keySkills.isNullOrEmpty()) {
                tvKeySkill.isVisible = false
                keySkill.isVisible = false
            } else {
                val keySkillsString = buildString {
                    append("<ul>")
                    vacancy.keySkills.forEach { keySkill ->
                        append("<li>$keySkill</li>")
                    }
                    append("</ul>")
                }
                keySkill.text = HtmlCompat.fromHtml(
                    keySkillsString,
                    FROM_HTML_MODE_LEGACY
                )
            }
        }
    }

    private fun fillContactsSection(vacancy: Vacancy) {
        binding.apply {
            if (
                vacancy.contacts?.name?.isNotEmpty() == true ||
                vacancy.contacts?.email?.isNotEmpty() == true ||
                vacancy.contacts?.phones?.isNotEmpty() == true
            ) {
                tvContacts.isVisible = true
            }

            if (vacancy.contacts?.name?.isNotEmpty() == true) {
                contactName.text = vacancy.contacts.name
                tvContactName.isVisible = true
                contactName.isVisible = true
            }

            if (vacancy.contacts?.email?.isNotEmpty() == true) {
                email.text = vacancy.contacts.email
                tvEmail.isVisible = true
                email.isVisible = true
            }

            if (vacancy.contacts?.phones?.isNotEmpty() == true) {
                vacancy.contacts.phones.let {
                    phonesAdapter.updatePhones(it)
                }
                phoneList.adapter = phonesAdapter
                phoneList.isVisible = true
            }
        }
    }

    private fun prepareToolbarMenu() {
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onPrepareMenu(menu: Menu) {
                super.onPrepareMenu(menu)
                val item = menu.findItem(R.id.action_toggle_favorite)
                val icon = if (isFavorite) {
                    R.drawable.ic_favorite_active
                } else {
                    R.drawable.ic_favorite_inactive
                }
                item.setIcon(icon)
            }

            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.vacancy_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_share -> {
                        val vacancy = viewmodel.currentVacancy.value
                        if (clickDebounce() && vacancy?.alternateUrl != null) {
                            viewmodel.shareVacancy(vacancy.alternateUrl)
                        }
                        true
                    }

                    R.id.action_toggle_favorite -> {
                        val vacancy = viewmodel.currentVacancy.value
                        if (vacancy != null) {
                            viewmodel.toggleFavorite(vacancy)
                        }
                        true
                    }

                    else -> false
                }
            }
        }, viewLifecycleOwner)
    }

    private fun setObservables() {
        viewmodel.observeFavoriteState().observe(viewLifecycleOwner) {
            isFavorite = it
            // нужно дернуть тулбар в Activity, чтобы он перерисовался
            requireActivity().invalidateOptionsMenu()

        }
        viewmodel.observeVacancyState().observe(viewLifecycleOwner) {
            render(it)
        }
    }

    private fun setListeners() {
        binding.gotoSimilarJobsFragmentBtn.setOnClickListener {
            val direction =
                VacancyFragmentDirections.actionVacancyFragmentToSimilarVacanciesFragment(viewmodel.getVacancyId())
            findNavController().navigate(direction)
        }

        binding.email.setOnClickListener {
            val vacancy = viewmodel.currentVacancy.value
            if (clickDebounce() && vacancy?.contacts?.email != null) {
                val subject = StringBuilder()
                    .append(requireContext().getString(R.string.vacancy_detail_title_tv))
                    .append(": ")
                    .append(vacancy.vacancyName)
                    .append(", ")
                    .append(vacancy.area)

                viewmodel.sendEmail(vacancy.contacts.email, subject.toString())
            }
        }
    }

    private fun clickDebounce(): Boolean {
        val current = isClickAllowed
        if (isClickAllowed) {
            isClickAllowed = false
            onTrackClickDebounce(true)
        }
        return current
    }

    companion object {
        private const val CLICK_DEBOUNCE_DELAY = 500L
    }
}
