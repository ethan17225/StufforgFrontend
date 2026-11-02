package stufforg.capstone.frontend.feature.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.LockReset
import androidx.compose.material.icons.rounded.Mail
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import stufforg.capstone.frontend.BuildConfig
import stufforg.capstone.frontend.R
import stufforg.capstone.frontend.core.designsystem.LuminaCard
import stufforg.capstone.frontend.core.designsystem.LuminaPrimaryButton
import stufforg.capstone.frontend.core.designsystem.LuminaTextField
import stufforg.capstone.frontend.core.designsystem.LuminaVioletDark
import stufforg.capstone.frontend.core.designsystem.LuminaVioletDeep
import stufforg.capstone.frontend.core.designsystem.Spacing
import stufforg.capstone.frontend.core.designsystem.StatusPillBadge
import stufforg.capstone.frontend.core.designsystem.StufforgFrontendTheme
import stufforg.capstone.frontend.core.util.GoogleAuthHelper

@Composable
fun AuthRouteScreen(
    initialCreateAccount: Boolean,
    onAuthenticated: () -> Unit,
    viewModel: AuthViewModel = viewModel(factory = AuthViewModel.factory(initialCreateAccount)),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    AuthScreen(
        uiState = uiState,
        onModeChange = viewModel::setCreateAccountMode,
        onIdentifierChange = viewModel::onIdentifierChange,
        onUsernameChange = viewModel::onUsernameChange,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onRepeatPasswordChange = viewModel::onRepeatPasswordChange,
        onRememberMeChange = viewModel::onRememberMeChange,
        onTogglePasswordVisibility = viewModel::onTogglePasswordVisibility,
        onSubmit = { viewModel.submitEmailAuth(onAuthenticated) },
        onGoogleClick = {
            scope.launch {
                val result = GoogleAuthHelper.requestGoogleIdToken(
                    context = context,
                    webClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID,
                )
                viewModel.submitGoogleIdToken(result, onAuthenticated)
            }
        },
    )
}

@Composable
fun AuthScreen(
    uiState: AuthUiState,
    onModeChange: (Boolean) -> Unit,
    onIdentifierChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onRepeatPasswordChange: (String) -> Unit,
    onRememberMeChange: (Boolean) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onSubmit: () -> Unit,
    onGoogleClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.lg, vertical = Spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(MaterialTheme.shapes.large)
                    .background(
                        Brush.linearGradient(listOf(LuminaVioletDeep, LuminaVioletDark)),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Inventory2,
                    contentDescription = stringResource(R.string.cd_app_logo),
                    tint = Color.White,
                    modifier = Modifier.size(36.dp),
                )
            }
            Spacer(Modifier.height(Spacing.md))
            Text(
                text = if (uiState.isCreateAccount) "Create Your Account" else "StuffOrg",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = "Your physical world, effortlessly organized.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(Spacing.xl))

            LuminaCard(modifier = Modifier.fillMaxWidth()) {
                AuthModeToggle(
                    isCreateAccount = uiState.isCreateAccount,
                    onModeChange = onModeChange,
                )
                Spacer(Modifier.height(Spacing.lg))

                OutlinedButton(
                    onClick = onGoogleClick,
                    enabled = !uiState.isLoading,
                    shape = CircleShape,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 50.dp),
                ) {
                    Text(
                        text = "G",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(Spacing.sm))
                    Text(
                        text = "Continue with Google",
                        style = MaterialTheme.typography.labelLarge,
                    )
                }

                Spacer(Modifier.height(Spacing.lg))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f))
                    Text(
                        text = if (uiState.isCreateAccount) "  OR REGISTER WITH EMAIL  " else "  OR CONTINUE WITH EMAIL  ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(Spacing.lg))

                if (uiState.errorMessage != null) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = Spacing.md),
                    ) {
                        Text(
                            text = uiState.errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(Spacing.md),
                        )
                    }
                }

                val passwordTransformation = if (uiState.isPasswordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                }
                val visibilityIcon: @Composable () -> Unit = {
                    IconButton(onClick = onTogglePasswordVisibility) {
                        Icon(
                            imageVector = if (uiState.isPasswordVisible) {
                                Icons.Rounded.Visibility
                            } else {
                                Icons.Rounded.VisibilityOff
                            },
                            contentDescription = stringResource(R.string.cd_toggle_password_visibility),
                        )
                    }
                }

                if (uiState.isCreateAccount) {
                    LuminaTextField(
                        value = uiState.username,
                        onValueChange = onUsernameChange,
                        label = "Username",
                        placeholder = "alexmorgan",
                        leadingIcon = Icons.Rounded.Badge,
                        trailingContent = if (uiState.username.trim().length >= 2) {
                            {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                )
                            }
                        } else {
                            null
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    )
                    Spacer(Modifier.height(Spacing.md))
                    LuminaTextField(
                        value = uiState.email,
                        onValueChange = onEmailChange,
                        label = "Email Address",
                        placeholder = "alex.morgan@workspace.io",
                        leadingIcon = Icons.Rounded.AlternateEmail,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next,
                        ),
                    )
                    Spacer(Modifier.height(Spacing.md))
                    LuminaTextField(
                        value = uiState.password,
                        onValueChange = onPasswordChange,
                        label = "Password",
                        placeholder = "Create a strong password",
                        leadingIcon = Icons.Rounded.Key,
                        trailingContent = visibilityIcon,
                        visualTransformation = passwordTransformation,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Next,
                        ),
                    )
                    if (uiState.password.isNotEmpty()) {
                        Spacer(Modifier.height(Spacing.sm))
                        PasswordStrengthBar(score = uiState.passwordStrengthScore)
                    }
                    Spacer(Modifier.height(Spacing.md))
                    LuminaTextField(
                        value = uiState.repeatPassword,
                        onValueChange = onRepeatPasswordChange,
                        label = "Confirm Password",
                        placeholder = "Repeat your password",
                        leadingIcon = Icons.Rounded.LockReset,
                        trailingContent = if (uiState.passwordsMatch) {
                            {
                                StatusPillBadge(
                                    text = "Matched",
                                    icon = Icons.Rounded.CheckCircle,
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.padding(end = Spacing.sm),
                                )
                            }
                        } else {
                            null
                        },
                        visualTransformation = passwordTransformation,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done,
                        ),
                    )
                } else {
                    LuminaTextField(
                        value = uiState.identifier,
                        onValueChange = onIdentifierChange,
                        label = "Email or Username",
                        placeholder = "alex.morgan@workspace.io",
                        leadingIcon = Icons.Rounded.Mail,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next,
                        ),
                    )
                    Spacer(Modifier.height(Spacing.md))
                    LuminaTextField(
                        value = uiState.password,
                        onValueChange = onPasswordChange,
                        label = "Password",
                        placeholder = "Enter your password",
                        leadingIcon = Icons.Rounded.Key,
                        trailingContent = visibilityIcon,
                        visualTransformation = passwordTransformation,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done,
                        ),
                    )
                    Spacer(Modifier.height(Spacing.md))
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Keep Me Signed In",
                                    style = MaterialTheme.typography.titleSmall,
                                )
                                Text(
                                    text = "Persist refresh token on this device",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(
                                checked = uiState.rememberMe,
                                onCheckedChange = onRememberMeChange,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(Spacing.xl))
                LuminaPrimaryButton(
                    text = if (uiState.isCreateAccount) {
                        "Create Account & Get Started"
                    } else {
                        "Sign In"
                    },
                    onClick = onSubmit,
                    loading = uiState.isLoading,
                    trailingIcon = Icons.AutoMirrored.Rounded.ArrowForward,
                )
            }
        }
    }
}

@Composable
private fun AuthModeToggle(
    isCreateAccount: Boolean,
    onModeChange: (Boolean) -> Unit,
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            SegmentedPillOption(
                selected = !isCreateAccount,
                text = "Sign In",
                icon = Icons.AutoMirrored.Rounded.Login,
                onClick = { onModeChange(false) },
                modifier = Modifier.weight(1f),
            )
            SegmentedPillOption(
                selected = isCreateAccount,
                text = "Create Account",
                icon = Icons.Rounded.PersonAdd,
                onClick = { onModeChange(true) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SegmentedPillOption(
    selected: Boolean,
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        contentColor = if (selected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        modifier = modifier.heightIn(min = 44.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(Spacing.xs))
            Text(text = text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun PasswordStrengthBar(score: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        for (i in 1..4) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(
                        if (i <= score) {
                            MaterialTheme.colorScheme.tertiary
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHighest
                        },
                    ),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AuthScreenSignInPreview() {
    StufforgFrontendTheme {
        AuthScreen(
            uiState = AuthUiState(identifier = "alex.morgan@workspace.io"),
            onModeChange = {},
            onIdentifierChange = {},
            onUsernameChange = {},
            onEmailChange = {},
            onPasswordChange = {},
            onRepeatPasswordChange = {},
            onRememberMeChange = {},
            onTogglePasswordVisibility = {},
            onSubmit = {},
            onGoogleClick = {},
        )
    }
}

