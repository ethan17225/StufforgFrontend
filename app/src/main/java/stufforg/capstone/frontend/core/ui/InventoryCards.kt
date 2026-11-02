package stufforg.capstone.frontend.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Cable
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Checkroom
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Kitchen
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Park
import androidx.compose.material.icons.rounded.Power
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import stufforg.capstone.frontend.R
import stufforg.capstone.frontend.core.designsystem.LuminaCard
import stufforg.capstone.frontend.core.designsystem.QuantityStepper
import stufforg.capstone.frontend.core.designsystem.Spacing
import stufforg.capstone.frontend.core.designsystem.StatusPillBadge
import stufforg.capstone.frontend.core.designsystem.parseBoxColor
import stufforg.capstone.frontend.core.model.BoxOut
import stufforg.capstone.frontend.core.model.ItemOut

fun categoryIconFor(category: String?): ImageVector {
    val normalized = category?.lowercase().orEmpty()
    return when {
        "cable" in normalized || "cord" in normalized || "wire" in normalized -> Icons.Rounded.Cable
        "audio" in normalized || "music" in normalized || "mic" in normalized -> Icons.Rounded.Headphones
        "adapter" in normalized || "power" in normalized || "charger" in normalized -> Icons.Rounded.Power
        "tool" in normalized || "hardware" in normalized -> Icons.Rounded.Build
        "tech" in normalized || "electr" in normalized || "camera" in normalized -> Icons.Rounded.Devices
        "camp" in normalized || "outdoor" in normalized -> Icons.Rounded.Park
        "kitchen" in normalized || "cook" in normalized -> Icons.Rounded.Kitchen
        "cloth" in normalized || "apparel" in normalized -> Icons.Rounded.Checkroom
        "doc" in normalized || "book" in normalized || "paper" in normalized -> Icons.Rounded.Description
        else -> Icons.Rounded.Category
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BoxSummaryCard(
    box: BoxOut,
    onClick: () -> Unit,
    onQrClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = parseBoxColor(box.colorCode)
    val accentSoft = accent.copy(alpha = 0.14f)

    LuminaCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (box.hasUploadedImage) {
                AsyncImage(
                    model = box.imageUrl,
                    contentDescription = box.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(accentSoft),
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(accentSoft),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Inventory2,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(26.dp),
                    )
                }
            }
            Spacer(Modifier.width(Spacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    StatusPillBadge(
                        text = box.boxTag,
                        containerColor = accentSoft,
                        contentColor = accent,
                    )
                    Text(
                        text = box.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(Spacing.xs))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    if (!box.location.isNullOrBlank()) {
                        Icon(
                            imageVector = Icons.Rounded.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = "${box.location} • ",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        text = "${box.items.size} items (${box.totalUnits} qty)",
                        style = MaterialTheme.typography.labelMedium,
                        color = accent,
                    )
                }
            }
            Surface(
                shape = CircleShape,
                color = accentSoft,
            ) {
                IconButton(
                    onClick = onQrClick,
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.QrCode2,
                        contentDescription = stringResource(R.string.cd_print_box_qr),
                        tint = accent,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }

        if (box.items.isNotEmpty()) {
            Spacer(Modifier.height(Spacing.md))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                box.items.take(3).forEach { item ->
                    StatusPillBadge(
                        text = item.name,
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (box.items.size > 3) {
                    StatusPillBadge(
                        text = "+${box.items.size - 3} more",
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        contentColor = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
fun ItemQuantityCard(
    item: ItemOut,
    onClick: () -> Unit,
    onQuantityChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    boxSubtitle: String? = null,
    selectionMode: Boolean = false,
    selected: Boolean = false,
    onToggleSelect: (() -> Unit)? = null,
) {
    LuminaCard(
        onClick = if (selectionMode && onToggleSelect != null) onToggleSelect else onClick,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (selectionMode && onToggleSelect != null) {
                Checkbox(
                    checked = selected,
                    onCheckedChange = { onToggleSelect() },
                )
                Spacer(Modifier.width(Spacing.xs))
            }
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = categoryIconFor(item.category),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(Spacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!item.description.isNullOrBlank()) {
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    modifier = Modifier.padding(top = Spacing.xs),
                ) {
                    if (!item.category.isNullOrBlank()) {
                        StatusPillBadge(
                            text = item.category,
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            contentColor = MaterialTheme.colorScheme.primary,
                        )
                    }
                    if (!boxSubtitle.isNullOrBlank()) {
                        Text(
                            text = boxSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            if (!selectionMode) {
                Spacer(Modifier.width(Spacing.sm))
                QuantityStepper(
                    quantity = item.amount,
                    onDecrement = { if (item.amount > 1) onQuantityChange(item.amount - 1) },
                    onIncrement = { onQuantityChange(item.amount + 1) },
                )
            }
        }
    }
}

