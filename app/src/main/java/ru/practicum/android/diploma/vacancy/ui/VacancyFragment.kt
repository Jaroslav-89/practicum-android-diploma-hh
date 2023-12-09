package ru.practicum.android.diploma.vacancy.ui

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.core.text.HtmlCompat
import androidx.core.text.HtmlCompat.FROM_HTML_MODE_LEGACY
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import ru.practicum.android.diploma.R
import ru.practicum.android.diploma.common.util.Formatter
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
    private var phonesAdapter: PhonesAdapter? = null
    private lateinit var onPhoneClickDebounce: (Phone) -> Unit
    private val viewmodel by viewModel<VacancyViewModel> {
        parametersOf(args.vacancyId)
    }

    private var isFavorite = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentVacancyBinding.bind(view)

//        Toast.makeText(requireContext(), "vacancyId= ${args.vacancyId}", Toast.LENGTH_SHORT).show()
        viewmodel.getVacancy(args.vacancyId)

        binding.gotoSimilarJobsFragmentBtn.setOnClickListener {
            val direction =
                VacancyFragmentDirections.actionVacancyFragmentToSimilarVacanciesFragment(viewmodel.getVacancyId())
            findNavController().navigate(direction)
        }

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
            is VacancyScreenState.InternetThrowable -> showError()
            is VacancyScreenState.Content -> showContent(state.vacancy)
        }
    }

    private fun hideAllView() {
        binding.apply {
            svVacancy.isVisible = false
            tvContacts.isVisible = false
            tvContactName.isVisible = false
            contactName.isVisible = false
            tvEmail.isVisible = false
            email.isVisible = false
            tvPhone.isVisible = false
            tvPhone.isVisible = false
            phoneList.isVisible = false
            tvComment.isVisible = false
            comment.isVisible = false
        }
//        TODO("отправить сюда вьюшки ошибок и загрузки")
    }

    private fun showLoading() {
        Log.d("DEBUG", "Loading...")
        hideAllView()
//        TODO("should be implemented in another task")
    }

    private fun showError() {
        Log.d("DEBUG", "ERROR")
//        TODO("should be implemented in another task")
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

            val radiusForLogo = binding.root.resources.getDimensionPixelSize(R.dimen.size_12)

            Glide.with(requireContext())
                .load(vacancy.employerLogoUrl)
                .placeholder(R.drawable.vacancy_item_placeholder)
                .centerCrop()
                .transform(RoundedCorners(radiusForLogo))
                .into(binding.image)

            companyName.text = vacancy.employerName

            if (vacancy.address.isNullOrEmpty()) {
                city.text = vacancy.area
            } else {
                city.text = vacancy.address.toString()
            }

            experience.text = vacancy.experience

            val employmentAndSchedueString = StringBuilder()
                .append(vacancy.employment)

            if (vacancy.employment != null && vacancy.schedule != null) {
                employmentAndSchedueString
                    .append(", ")
            }
            employmentAndSchedueString
                .append(vacancy.schedule)
            schedule.text = employmentAndSchedueString.toString()

            description.text = HtmlCompat.fromHtml(
                vacancy.description,
                FROM_HTML_MODE_LEGACY
            )

            if (vacancy.keySkills.isNullOrEmpty()) {
                tvKeySkill.isVisible = false
                keySkill.isVisible = false
            } else {
                val keySkillsString = buildString {
                    append("<ul>")
                    vacancy.keySkills.forEach { keySkill ->
                        append("<li>${keySkill}</li>")
                    }
                    append("</ul>")
                }
                keySkill.text = HtmlCompat.fromHtml(
                    keySkillsString,
                    FROM_HTML_MODE_LEGACY
                )
            }
            viewContactsSection(vacancy)
        }
    }

    private fun viewContactsSection(vacancy: Vacancy) {
        binding.apply {
            if (
                vacancy.contacts?.name?.isNotEmpty() == true ||
                vacancy.contacts?.email?.isNotEmpty() == true ||
                vacancy.contacts?.phones?.toString()?.isNotEmpty() == true
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
                tvPhone.isVisible = true
                phonesAdapter = vacancy.contacts.phones.let {
                    PhonesAdapter(it) { phone ->
                        onPhoneClickDebounce(phone)
                    }
                }
                onPhoneClickDebounce = { phone ->
                    viewmodel.makeCall(phone)
                }
                phoneList.adapter = phonesAdapter
                tvPhone.isVisible = true
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
                        Toast.makeText(requireContext(), R.string.share_vacancy, Toast.LENGTH_SHORT).show()
                        true
                    }

                    R.id.action_toggle_favorite -> {
                        viewmodel.toggleFavorite()
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
}
