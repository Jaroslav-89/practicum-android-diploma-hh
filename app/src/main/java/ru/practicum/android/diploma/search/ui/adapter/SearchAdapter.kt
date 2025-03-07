package ru.practicum.android.diploma.search.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterInside
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import ru.practicum.android.diploma.R
import ru.practicum.android.diploma.common.util.Formatter
import ru.practicum.android.diploma.databinding.VacancyViewItemBinding
import ru.practicum.android.diploma.search.domain.model.VacancyItem

class SearchAdapter(private val clickListener: VacancyClickListener) :
    ListAdapter<VacancyItem, RecyclerView.ViewHolder>(VacancyDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInspector = LayoutInflater.from(parent.context)
        val binding = VacancyViewItemBinding.inflate(layoutInspector, parent, false)
        return VacancyViewHolder(
            binding = binding,
            clickListener = clickListener
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        (holder as VacancyViewHolder).bind(item)
    }

    class VacancyViewHolder(
        private val binding: VacancyViewItemBinding,
        private val clickListener: VacancyClickListener
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(vacancy: VacancyItem) {
            itemView.setOnClickListener { clickListener.onVacancyClick(vacancy) }
            with(binding) {
                vacancyNameAndCity.text =
                    itemView.resources.getString(R.string.vacancy_item_title, vacancy.vacancyName, vacancy.area)
                companyName.text = vacancy.employerName
                salary.text = Formatter.formatSalary(
                    context = itemView.context,
                    from = vacancy.salaryFrom,
                    to = vacancy.salaryTo,
                    currencyCode = vacancy.salaryCurrency
                )
                Glide.with(itemView)
                    .load(vacancy.employerLogoUrl)
                    .placeholder(R.drawable.vacancy_item_placeholder)
                    .transform(
                        CenterInside(),
                        RoundedCorners(
                            itemView.resources.getDimensionPixelSize(R.dimen.size_12)
                        ),
                    )
                    .into(employerLogoIv)
            }
        }
    }
}

fun interface VacancyClickListener {
    fun onVacancyClick(vacancy: VacancyItem)
}

private class VacancyDiffCallback : DiffUtil.ItemCallback<VacancyItem>() {

    override fun areItemsTheSame(oldItem: VacancyItem, newItem: VacancyItem): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: VacancyItem, newItem: VacancyItem): Boolean {
        return oldItem == newItem
    }
}
