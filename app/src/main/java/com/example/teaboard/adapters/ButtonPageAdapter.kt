package com.example.teaboard.adapters

import android.content.res.Configuration
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.teaboard.R
import com.example.teaboard.models.ButtonConfig

/**
 * Adapter for ViewPager2 that displays pages of buttons
 * Each page contains up to 6 buttons in a 2x3 or 3x2 grid
 */
class ButtonPageAdapter(
    private var allButtons: List<ButtonConfig> = emptyList(),
    private var isEditMode: Boolean = false,
    private val onButtonClick: (ButtonConfig) -> Unit,
    private val onDeleteClick: (ButtonConfig) -> Unit
) : RecyclerView.Adapter<ButtonPageAdapter.PageViewHolder>() {

    companion object {
        const val BUTTONS_PER_PAGE = 6
    }

    private val pages: List<List<ButtonConfig>>
        get() = allButtons.chunked(BUTTONS_PER_PAGE)

    inner class PageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val recyclerView: RecyclerView = itemView.findViewById(R.id.recyclerViewPage)
        private var adapter: ButtonCardAdapter? = null

        fun bind(buttonsForPage: List<ButtonConfig>) {
            if (adapter == null) {
                adapter = ButtonCardAdapter(
                    buttonConfigs = buttonsForPage,
                    isEditMode = isEditMode,
                    onButtonClick = onButtonClick,
                    onDeleteClick = onDeleteClick
                )
                recyclerView.adapter = adapter

                // Use 4 columns in landscape, 2 in portrait
                val isLandscape = itemView.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
                val spanCount = if (isLandscape) 4 else 2
                recyclerView.layoutManager = GridLayoutManager(itemView.context, spanCount)
            } else {
                adapter?.updateData(buttonsForPage)
                adapter?.setEditMode(isEditMode)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.page_buttons, parent, false)
        return PageViewHolder(view)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        val pageButtons = if (position < pages.size) pages[position] else emptyList()
        holder.bind(pageButtons)
    }

    override fun getItemCount(): Int = if (allButtons.isEmpty()) 1 else pages.size

    fun updateData(newButtons: List<ButtonConfig>) {
        allButtons = newButtons
        notifyDataSetChanged()
    }

    fun setEditMode(editMode: Boolean) {
        isEditMode = editMode
        notifyDataSetChanged()
    }
}
