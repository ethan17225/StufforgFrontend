package stufforg.capstone.frontend.feature.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import stufforg.capstone.frontend.core.designsystem.LuminaCard
import stufforg.capstone.frontend.core.designsystem.LuminaPrimaryButton
import stufforg.capstone.frontend.core.designsystem.LuminaTonalButton
import stufforg.capstone.frontend.core.designsystem.LuminaViolet
import stufforg.capstone.frontend.core.designsystem.LuminaVioletDark
import stufforg.capstone.frontend.core.designsystem.LuminaVioletDeep
import stufforg.capstone.frontend.core.designsystem.Spacing
import stufforg.capstone.frontend.core.designsystem.StatusPillBadge
import stufforg.capstone.frontend.core.designsystem.StuffOrgBrandBadge
import stufforg.capstone.frontend.core.designsystem.StufforgFrontendTheme

@Composable
fun OnboardingScreen(
    onFinish: (createAccount: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()

    BackHandler(enabled = pagerState.currentPage > 0) {
        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        ) {
            OnboardingHeader(
                currentStep = pagerState.currentPage + 1,
                onSkip = { onFinish(false) },
            )
            Spacer(Modifier.height(Spacing.lg))

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
            ) { page ->
                when (page) {
                    0 -> OnboardingStepOne(
                        onNext = { scope.launch { pagerState.animateScrollToPage(1) } },
                    )
                    1 -> OnboardingStepTwo(
                        onNext = { scope.launch { pagerState.animateScrollToPage(2) } },
                    )
                    else -> OnboardingStepThree(
                        onCreateAccount = { onFinish(true) },
                        onSignIn = { onFinish(false) },
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingHeader(
    currentStep: Int,
    onSkip: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            StuffOrgBrandBadge()
            FilledTonalButton(
                onClick = onSkip,
                shape = CircleShape,
            ) {
                Text(
                    text = if (currentStep < 3) "Skip to App" else "Finish Setup",
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
        Spacer(Modifier.height(Spacing.lg))
        val stepTitle = when (currentStep) {
            1 -> "STEP 1 OF 3 • QUICK SETUP"
            2 -> "STEP 2 OF 3 • SMART VISION"
            else -> "STEP 3 OF 3 • READY TO GO"
        }
        val stepPct = when (currentStep) {
            1 -> "33%"
            2 -> "66%"
            else -> "100% Ready"
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stepTitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            StatusPillBadge(text = stepPct)
        }
        Spacer(Modifier.height(Spacing.sm))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            for (step in 1..3) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(
                            if (step <= currentStep) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHighest
                            },
                        ),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OnboardingStepOne(onNext: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
            LuminaCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                    ) {
                        StatusPillBadge(text = "Studio Gear")
                        StatusPillBadge(
                            text = "Winter Camping",
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                    }
                    Spacer(Modifier.height(Spacing.lg))
                    Box(
                        modifier = Modifier
                            .size(128.dp)
                            .graphicsLayer { rotationZ = -6f }
                            .clip(MaterialTheme.shapes.large)
                            .background(
                                Brush.linearGradient(
                                    listOf(LuminaVioletDeep, LuminaVioletDark),
                                ),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Rounded.Inventory2,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(48.dp),
                            )
                            Spacer(Modifier.height(Spacing.xs))
                            Text(
                                text = "#BOX-04",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White,
                            )
                        }
                    }
                    Spacer(Modifier.height(Spacing.md))
                    StatusPillBadge(
                        text = "AI-assisted box indexing",
                        icon = Icons.Rounded.AutoAwesome,
                    )
                    Spacer(Modifier.height(Spacing.lg))
                    Text(
                        text = "Organize your physical world in seconds.",
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(Spacing.sm))
                    Text(
                        text = "Snap a photo of any storage box or shelf. AI detects items, generates printable QR labels, and lets you locate everything fast.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            FeatureHighlightRow(
                icon = Icons.Rounded.PhotoCamera,
                title = "Snap & Tag Magic",
                subtitle = "Take or upload a photo of box contents — YOLO object detection pre-fills items and quantities automatically.",
            )
        }

        Column(modifier = Modifier.padding(top = Spacing.xl)) {
            LuminaPrimaryButton(
                text = "Get Started with AI Scan",
                onClick = onNext,
                trailingIcon = Icons.AutoMirrored.Rounded.ArrowForward,
            )
        }
    }
}

@Composable
private fun OnboardingStepTwo(onNext: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
            LuminaCard(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .padding(Spacing.lg),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        StatusPillBadge(
                            text = "AI Object Detection",
                            icon = Icons.Rounded.AutoAwesome,
                        )
                        DetectionPreviewTag("Camera Body", "98% match")
                        DetectionPreviewTag("Wireless Microphone", "99% match")
                        DetectionPreviewTag("Braided USB-C Cable", "95% match")
                    }
                }
                Spacer(Modifier.height(Spacing.lg))
                Text(
                    text = "Instant Multi-Item Tagging",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    text = "Capture items spread out before packing. StuffOrg detects multiple objects at once and builds your searchable box manifest.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            FeatureHighlightRow(
                icon = Icons.Rounded.QrCodeScanner,
                title = "Zero-Permission QR Lookup",
                subtitle = "Scan any physical box sticker with Google Code Scanner to view contents without opening the lid.",
            )
            FeatureHighlightRow(
                icon = Icons.Rounded.Print,
                title = "Printable QR Stickers",
                subtitle = "Print or share scannable 2×2\" box labels straight from your phone.",
            )
        }

        Column(modifier = Modifier.padding(top = Spacing.xl)) {
            LuminaPrimaryButton(
                text = "Continue",
                onClick = onNext,
                trailingIcon = Icons.AutoMirrored.Rounded.ArrowForward,
            )
        }
    }
}

@Composable
private fun OnboardingStepThree(
    onCreateAccount: () -> Unit,
    onSignIn: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                color = LuminaViolet,
            ) {
                Column(
                    modifier = Modifier
                        .background(
                            Brush.linearGradient(
                                listOf(LuminaVioletDeep, LuminaViolet, LuminaVioletDark),
                            ),
                        )
                        .padding(Spacing.xl),
                ) {
                    StatusPillBadge(
                        text = "Inventory Ready",
                        icon = Icons.Rounded.CheckCircle,
                        containerColor = Color.White.copy(alpha = 0.18f),
                        contentColor = Color.White,
                    )
                    Spacer(Modifier.height(Spacing.lg))
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = Color.White.copy(alpha = 0.14f),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(Spacing.lg),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color.White),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Inventory2,
                                    contentDescription = null,
                                    tint = LuminaViolet,
                                )
                            }
                            Spacer(Modifier.width(Spacing.md))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Box #01 • Home & Studio",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                )
                                Text(
                                    text = "AI Scan & QR Labels Enabled",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.8f),
                                )
                            }
                            Icon(
                                imageVector = Icons.Rounded.QrCode2,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp),
                            )
                        }
                    }
                    Spacer(Modifier.height(Spacing.lg))
                    Text(
                        text = "You're All Set to Organize Your World!",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                    )
                    Spacer(Modifier.height(Spacing.sm))
                    Text(
                        text = "Create an account or sign in to start cataloging boxes, scanning items with AI, and printing QR labels.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.88f),
                    )
                }
            }
        }

        Column(
            modifier = Modifier.padding(top = Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            LuminaPrimaryButton(
                text = "Create Account & Start Organizing",
                onClick = onCreateAccount,
                trailingIcon = Icons.AutoMirrored.Rounded.ArrowForward,
            )
            LuminaTonalButton(
                text = "I Already Have an Account — Sign In",
                onClick = onSignIn,
            )
        }
    }
}

@Composable
private fun DetectionPreviewTag(name: String, confidence: String) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(16.dp),
            )
            Text(text = name, style = MaterialTheme.typography.labelLarge)
            Text(
                text = confidence,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun FeatureHighlightRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
) {
    LuminaCard(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(Spacing.md))
            Column {
                Text(text = title, style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OnboardingScreenPreview() {
    StufforgFrontendTheme {
        OnboardingScreen(onFinish = {})
    }
}

