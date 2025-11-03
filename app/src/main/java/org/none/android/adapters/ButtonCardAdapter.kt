package org.none.android.adapters

import android.animation.ValueAnimator
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.GradientDrawable
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import org.none.android.R
import org.none.android.models.ButtonConfig
import java.io.File

class ButtonCardAdapter(
    private var buttonConfigs: List<ButtonConfig>,
    private var isEditMode: Boolean = false,
    private val onButtonClick: (ButtonConfig) -> Unit,
    private val onDeleteClick: (ButtonConfig) -> Unit
) : RecyclerView.Adapter<ButtonCardAdapter.ButtonViewHolder>() {

    private val cardAnimators = mutableMapOf<Int, ValueAnimator>()

    // Constants from MainActivity
    companion object {
        private const val BUTTON_PRESS_SCALE = 0.92f
        private const val BUTTON_PRESS_DURATION_MS = 100L
        private const val EDIT_MODE_BORDER_WIDTH_DP = 4
    }

    class ButtonViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val card: FrameLayout = view.findViewById(R.id.cardButton)
        val cardContent: FrameLayout = view.findViewById(R.id.cardContent)
        val image: ImageView = view.findViewById(R.id.imgButton)
        val progress: ProgressBar? = view.findViewById(R.id.progressButton)
        val deleteIcon: ImageView = view.findViewById(R.id.imgDelete)
        val label: Button = view.findViewById(R.id.btnLabel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ButtonViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_button_card, parent, false)
        return ButtonViewHolder(view)
    }

    override fun onBindViewHolder(holder: ButtonViewHolder, position: Int) {
        val config = buttonConfigs[position]

        // Set card background color with rounded corners
        try {
            val color = Color.parseColor(config.backgroundColor)
            val drawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(color)
                cornerRadius = 12f * holder.cardContent.context.resources.displayMetrics.density
            }
            holder.cardContent.background = drawable
        } catch (e: Exception) {
            // Fallback to default color if parsing fails
            val drawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(Color.parseColor("#6B4CE6"))
                cornerRadius = 12f * holder.cardContent.context.resources.displayMetrics.density
            }
            holder.cardContent.background = drawable
        }

        // Set label text
        holder.label.text = config.label

        // Load image if exists
        val imageFile = if (config.imagePath.isNotEmpty()) File(config.imagePath) else null
        if (imageFile != null && imageFile.exists()) {
            // Check if this is a demo image (contains timestamp pattern or not)
            val isDemoImage = !config.imagePath.contains("_\\d{13,}".toRegex())

            if (isDemoImage) {
                // Demo images: use fitCenter with padding for a smaller look
                val density = holder.image.context.resources.displayMetrics.density
                val paddingSidesDp = 48
                val paddingTopDp = 32
                val paddingBottomDp = 64

                val paddingSidesPx = (paddingSidesDp * density).toInt()
                val paddingTopPx = (paddingTopDp * density).toInt()
                val paddingBottomPx = (paddingBottomDp * density).toInt()

                holder.image.setPadding(paddingSidesPx, paddingTopPx, paddingSidesPx, paddingBottomPx)
                holder.image.scaleType = ImageView.ScaleType.FIT_CENTER

                Glide.with(holder.image.context)
                    .load(imageFile)
                    .fitCenter()
                    .into(holder.image)
            } else {
                // User uploaded images: use full space
                holder.image.setPadding(0, 0, 0, 0)
                holder.image.scaleType = ImageView.ScaleType.CENTER_CROP

                Glide.with(holder.image.context)
                    .load(imageFile)
                    .centerCrop()
                    .into(holder.image)
            }
        } else {
            holder.image.setImageDrawable(null)
            holder.image.setPadding(0, 0, 0, 0)
        }

        // Show/hide delete icon based on edit mode
        holder.deleteIcon.visibility = if (isEditMode) View.VISIBLE else View.GONE

        // Set up delete click listener
        holder.deleteIcon.setOnClickListener {
            onDeleteClick(config)
        }

        // Set up card click listener
        holder.card.setOnClickListener {
            onButtonClick(config)

            // Haptic feedback
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

            // Press animation
            holder.card.animate()
                .scaleX(BUTTON_PRESS_SCALE)
                .scaleY(BUTTON_PRESS_SCALE)
                .setDuration(BUTTON_PRESS_DURATION_MS)
                .withEndAction {
                    holder.card.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(BUTTON_PRESS_DURATION_MS)
                        .start()
                }
                .start()
        }

        // Apply edit mode visual effects (pulsing border)
        updateEditModeVisuals(holder, config.buttonId)
    }

    override fun getItemCount(): Int = buttonConfigs.size

    fun updateData(newConfigs: List<ButtonConfig>) {
        buttonConfigs = newConfigs
        notifyDataSetChanged()
    }

    fun setEditMode(editMode: Boolean) {
        isEditMode = editMode
        notifyDataSetChanged()
    }

    private fun updateEditModeVisuals(holder: ButtonViewHolder, buttonId: Int) {
        // Cancel existing animator for this button
        cardAnimators[buttonId]?.cancel()
        cardAnimators.remove(buttonId)

        val density = holder.card.context.resources.displayMetrics.density

        if (isEditMode) {
            // Enable edit mode: orange border with pulsing animation, orange overlay

            // Orange overlay on image
            holder.image.setColorFilter(
                Color.parseColor("#66FF6600"),
                PorterDuff.Mode.SRC_ATOP
            )

            // Pulsing border animation - animate the outer frame's background color
            val startColor = Color.parseColor("#FF6600")  // Orange
            val endColor = Color.parseColor("#FFAA00")    // Lighter orange

            val animator = ValueAnimator.ofArgb(startColor, endColor).apply {
                duration = 1000
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.REVERSE
                addUpdateListener { animation ->
                    val drawable = GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        setColor(animation.animatedValue as Int)
                        cornerRadius = 16f * density
                    }
                    holder.card.background = drawable
                }
                start()
            }

            cardAnimators[buttonId] = animator
        } else {
            // Disable edit mode: restore black border, remove overlay
            val drawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(Color.parseColor("#000000"))
                cornerRadius = 16f * density
            }
            holder.card.background = drawable
            holder.image.clearColorFilter()
        }
    }

    fun cancelAllAnimations() {
        cardAnimators.values.forEach { it.cancel() }
        cardAnimators.clear()
    }

    override fun onViewRecycled(holder: ButtonViewHolder) {
        super.onViewRecycled(holder)
        // Clean up Glide to prevent memory leaks
        Glide.with(holder.image.context).clear(holder.image)
    }
}
