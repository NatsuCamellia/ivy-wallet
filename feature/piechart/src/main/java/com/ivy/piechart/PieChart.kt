package com.ivy.piechart

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.ivy.base.legacy.toEpochMilli
import com.ivy.base.model.TransactionType
import com.ivy.data.model.Category
import com.ivy.data.model.CategoryId
import com.ivy.data.model.primitive.ColorInt
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.legacy.ui.WalletPreview
import com.ivy.legacy.ui.component.color
import com.ivy.legacy.ui.component.icon
import com.ivy.legacy.ui.component.label
import com.ivy.legacy.utils.timeNowUTC
import com.ivy.ui.R
import com.ivy.ui.color.Green
import com.ivy.ui.color.IvyDark
import com.ivy.ui.color.RedLight
import com.ivy.wallet.ui.theme.Gray
import com.ivy.wallet.ui.theme.toComposeColor
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import timber.log.Timber
import java.util.UUID
import kotlin.math.acos
import kotlin.math.sqrt

const val RADIUS_DP = 112f

/** The pie in its category colours with a hub in the middle: the type icon, or the selected category and its share. */
@Composable
fun PieChart(
    type: TransactionType,
    categoryAmounts: ImmutableList<CategoryAmount>,
    selectedCategory: SelectedCategory?,
    modifier: Modifier = Modifier,
    onCategoryClick: (Category?) -> Unit = {}
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            modifier = Modifier.size((2 * RADIUS_DP).dp),
            factory = {
                PieChartView(it)
            },
            update = { view ->
                view.display(
                    categoryAmounts = categoryAmounts.sortedByDescending { it.amount },
                    selectedCategory = selectedCategory,
                    onCategoryClicked = onCategoryClick
                )
            }
        )

        Hub(
            type = type,
            categoryAmounts = categoryAmounts,
            selectedCategory = selectedCategory,
        )
    }
}

@Composable
private fun Hub(
    type: TransactionType,
    categoryAmounts: ImmutableList<CategoryAmount>,
    selectedCategory: SelectedCategory?,
    modifier: Modifier = Modifier,
) {
    val selected = selectedCategory?.let { selection ->
        categoryAmounts.firstOrNull { it.category == selection.category }
    }
    val total = categoryAmounts.sumOf { it.amount }

    Column(
        modifier = modifier
            .size(RADIUS_DP.dp)
            .background(MaterialTheme.colorScheme.surface, CircleShape)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (selected == null) {
            Icon(
                imageVector = type.icon(),
                contentDescription = type.label(),
                tint = type.color(),
            )
        } else {
            Text(
                text = selected.category?.name?.value ?: stringResource(R.string.unspecified),
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = percentLabel(selected.amount, total),
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

private class PieChartView(context: Context) : View(context) {
    private var categoryAmounts = emptyList<CategoryAmount>()
    private var paints = mapOf<Category?, Paint>()
    private var totalAmount = 0.0

    val rectangle = RectF(
        0f,
        0f,
        com.ivy.legacy.utils.convertDpToPixel(context, 2 * RADIUS_DP),
        com.ivy.legacy.utils.convertDpToPixel(context, 2 * RADIUS_DP)
    )

    var onCategoryClicked: (Category?) -> Unit = {}

    fun display(
        categoryAmounts: List<CategoryAmount>,
        selectedCategory: SelectedCategory?,
        onCategoryClicked: (Category?) -> Unit
    ) {
        this.onCategoryClicked = onCategoryClicked

        this.categoryAmounts = categoryAmounts
        this.totalAmount = categoryAmounts.sumOf { it.amount }

        this.paints = categoryAmounts
            .map {
                val category = it.category
                val categoryColor = category?.color?.value?.toComposeColor() ?: Gray
                val color = if (selectedCategory == null) {
                    categoryColor
                } else {
                    if (selectedCategory.category == category) {
                        categoryColor
                    } else {
                        categoryColor.copy(
                            alpha = 0.15f
                        )
                    }
                }

                category to paintFor(
                    color = color
                )
            }
            .toMap()

        invalidate()
    }

    private fun paintFor(color: Color): Paint {
        return Paint().apply {
            this.color = color.toArgb()
            this.strokeWidth = com.ivy.legacy.utils.convertDpToPixel(context, 2f)
            this.strokeCap = Paint.Cap.ROUND
            this.strokeJoin = Paint.Join.ROUND
            this.isAntiAlias = true
        }
    }

    private val zones = mutableListOf<Zone>()

    override fun onDraw(canvas: Canvas) {
        var startAngle = -90.0

        zones.clear()

        for (categoryAmount in categoryAmounts) {
            val paint = paints[categoryAmount.category] ?: continue
            val amount = categoryAmount.amount

            if (amount == 0.0) continue

            val percent = amount / totalAmount
            val sweepAngle = 360 * percent

            zones.add(
                Zone(
                    startAngle = startAngle,
                    endAngle = startAngle + sweepAngle,
                    category = categoryAmount.category
                )
            )

            canvas.drawArc(
                rectangle,
                startAngle.toFloat(),
                sweepAngle.toFloat(),
                true,
                paint
            ) // draw

            startAngle += sweepAngle
        }
    }

    private var startClickTime = 0L

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                startClickTime = timeNowUTC().toEpochMilli()
            }

            MotionEvent.ACTION_UP -> {
                val clickDuration: Long =
                    timeNowUTC().toEpochMilli() - startClickTime
                if (clickDuration < MAX_CLICK_DURATION) {
                    val touchX = event.x
                    val touchY = event.y

                    val centerX = width / 2f
                    val centerY = height / 2f
                    Timber.d("click: x = $touchX, y = $touchY (width = $width, height = $height)")

                    val angle = getAngle(
                        touchX = touchX,
                        touchY = touchY,
                        centerX = centerX,
                        centerY = centerY
                    )

                    Timber.d("degrees = $angle")

                    val clickedCategory = zones
                        .firstOrNull { zone ->
                            zone.contains(angle = angle)
                        }?.category
                    Timber.i("clicked category = ${clickedCategory?.name}")

                    onCategoryClicked(clickedCategory)
                }
            }
        }
        return true
    }

    private fun getAngle(
        touchX: Float,
        touchY: Float,
        centerX: Float,
        centerY: Float
    ): Double {
        val angle: Double
        val x2 = touchX - centerX
        val y2 = touchY - centerY
        val d1 = sqrt((centerY * centerY).toDouble())
        val d2 = sqrt((x2 * x2 + y2 * y2).toDouble())
        angle = if (touchX >= centerX) {
            Math.toDegrees(acos((-centerY * y2) / (d1 * d2)))
        } else {
            360 - Math.toDegrees(acos((-centerY * y2) / (d1 * d2)))
        }
        return angle - 90.0
    }

    companion object {
        private const val MAX_CLICK_DURATION = 200
    }

    private data class Zone(
        val startAngle: Double,
        val endAngle: Double,
        val category: Category?
    ) {
        fun contains(angle: Double): Boolean =
            angle > startAngle && angle < endAngle
    }
}

@Preview
@Composable
private fun Preview() {
    val categories = listOf("Bills" to Green, "Food & Drink" to IvyDark, "Gifts" to RedLight).map { (name, color) ->
        Category(
            name = NotBlankTrimmedString.unsafe(name),
            color = ColorInt(color.toArgb()),
            icon = null,
            id = CategoryId(UUID.randomUUID()),
            orderNum = 0.0,
        )
    }
    WalletPreview {
        PieChart(
            type = TransactionType.EXPENSE,
            categoryAmounts = persistentListOf(
                CategoryAmount(category = categories[0], amount = 791.0),
                CategoryAmount(category = categories[1], amount = 260.03),
                CategoryAmount(category = categories[2], amount = 160.0),
                CategoryAmount(category = null, amount = 497.0),
            ),
            selectedCategory = null
        )
    }
}
