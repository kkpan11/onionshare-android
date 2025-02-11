package org.onionshare.android.ui.share

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.annotation.StringRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily.Companion.Monospace
import androidx.compose.ui.text.font.FontStyle.Companion.Italic
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.onionshare.android.R
import org.onionshare.android.ui.CopyButton
import org.onionshare.android.ui.StyledLegacyText
import org.onionshare.android.ui.theme.Error
import org.onionshare.android.ui.theme.IndicatorReady
import org.onionshare.android.ui.theme.IndicatorSharing
import org.onionshare.android.ui.theme.IndicatorStarting
import org.onionshare.android.ui.theme.OnionRed
import org.onionshare.android.ui.theme.OnionshareTheme
import kotlin.random.Random

private data class BottomSheetUi(
    val indicatorIcon: ImageVector = Icons.Filled.Circle,
    val indicatorColor: Color,
    @StringRes val stateText: Int,
    @StringRes val buttonText: Int,
)

private fun getBottomSheetUi(state: ShareUiState) = when (state) {
    is ShareUiState.AddingFiles -> BottomSheetUi(
        indicatorColor = IndicatorReady,
        stateText = R.string.share_state_ready,
        buttonText = R.string.share_button_start,
    )

    is ShareUiState.Starting -> BottomSheetUi(
        indicatorColor = IndicatorStarting,
        stateText = R.string.share_state_starting,
        buttonText = R.string.share_button_starting,
    )
    is ShareUiState.Sharing -> BottomSheetUi(
        indicatorColor = IndicatorSharing,
        stateText = R.string.share_state_sharing,
        buttonText = R.string.share_button_stop,
    )
    is ShareUiState.Complete -> BottomSheetUi(
        indicatorIcon = Icons.Filled.CheckCircle,
        indicatorColor = IndicatorSharing,
        stateText = R.string.share_state_transfer_complete,
        buttonText = R.string.share_button_complete,
    )
    is ShareUiState.Stopping -> BottomSheetUi(
        indicatorColor = IndicatorStarting,
        stateText = R.string.share_state_stopping,
        buttonText = R.string.share_button_stopping,
    )
    is ShareUiState.ErrorAddingFile -> BottomSheetUi(
        indicatorColor = IndicatorReady,
        stateText = R.string.share_state_ready,
        buttonText = R.string.share_button_start,
    )

    is ShareUiState.ErrorStarting -> BottomSheetUi(
        indicatorColor = Error,
        stateText = R.string.share_state_error,
        buttonText = R.string.share_button_error,
    )
}

@Composable
fun BottomSheet(state: ShareUiState, onSheetButtonClicked: () -> Unit) {
    val sheetUi = getBottomSheetUi(state)
    val bottomPadding = with(LocalDensity.current) {
        WindowInsets.safeDrawing.getBottom(this).toDp()
    }
    Column(modifier = Modifier.padding(bottom = bottomPadding)) {
        Row(
            verticalAlignment = CenterVertically,
            modifier = Modifier.padding(16.dp),
        ) {
            Icon(
                imageVector = sheetUi.indicatorIcon,
                tint = sheetUi.indicatorColor,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = stringResource(sheetUi.stateText),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(start = 16.dp),
            )
        }
        ProgressDivider(state)
        val colorControlNormal = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
        @Suppress("CascadeIf") // not in the mood for 'when' right now
        if (state is ShareUiState.Starting) {
            Text(
                text = stringResource(R.string.share_state_starting_text),
                modifier = Modifier.padding(16.dp),
            )
            HorizontalDivider(thickness = 2.dp)
        } else if (state is ShareUiState.Sharing) {
            StyledLegacyText(
                id = R.string.share_onion_intro,
                modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
            )
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = CenterVertically,
                horizontalArrangement = spacedBy(8.dp),
            ) {
                // Box is workaround for https://issuetracker.google.com/issues/372053402
                Box(modifier = Modifier.weight(1f, fill = false)) {
                    SelectionContainer {
                        Text(state.onionAddress, fontFamily = Monospace)
                    }
                }
                CopyButton(
                    toCopy = state.onionAddress,
                    clipBoardLabel = stringResource(R.string.clipboard_onion_service_label),
                )
            }
            HorizontalDivider(thickness = 2.dp)
        } else if (state is ShareUiState.ErrorStarting) {
            val textRes =
                if (state.torFailedToConnect) R.string.share_state_error_tor_text else R.string.share_state_error_text
            Text(
                text = if (state.errorMsg == null) {
                    stringResource(textRes)
                } else {
                    stringResource(textRes) + "\n\n${state.errorMsg}"
                },
                modifier = Modifier.padding(16.dp),
            )
            HorizontalDivider(thickness = 2.dp)
        }
        var buttonEnabled by remember(state) { mutableStateOf(state !is ShareUiState.Stopping) }
        Button(
            onClick = {
                buttonEnabled = false
                onSheetButtonClicked()
            },
            colors = if (state is ShareUiState.Sharing) {
                ButtonDefaults.buttonColors(
                    contentColor = MaterialTheme.colorScheme.OnionRed,
                    containerColor = MaterialTheme.colorScheme.surface
                )
            } else ButtonDefaults.buttonColors(),
            border = if (state is ShareUiState.Sharing) {
                BorderStroke(1.dp, colorControlNormal)
            } else null,
            shape = RoundedCornerShape(32.dp),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 0.dp,
            ),
            enabled = buttonEnabled,
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
        ) {
            Text(
                text = stringResource(sheetUi.buttonText),
                fontSize = 16.sp,
                fontStyle = if (state is ShareUiState.Starting) Italic else null,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}

@Composable
fun ProgressDivider(state: ShareUiState) {
    if (state is ShareUiState.Starting) {
        val animatedProgress by animateFloatAsState(
            targetValue = state.totalProgress,
            animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec
        )
        LinearProgressIndicator(
            progress = { animatedProgress },
            color = MaterialTheme.colorScheme.primary,
            gapSize = 0.dp,
            drawStopIndicator = {},
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
        )
    } else {
        HorizontalDivider(thickness = 2.dp)
    }
}

@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
fun ShareBottomSheetReadyPreview() {
    OnionshareTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            BottomSheet(
                state = ShareUiState.AddingFiles,
                onSheetButtonClicked = {},
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ShareBottomSheetStartingPreview() {
    OnionshareTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            BottomSheet(
                state = ShareUiState.Starting(25, 50),
                onSheetButtonClicked = {},
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ShareBottomSheetSharingPreview() {
    OnionshareTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            BottomSheet(
                state = ShareUiState.Sharing(
                    "http://openpravyvc6spbd4flzn4g2iqu4sxzsizbtb5aqec25t76dnoo5w7yd.onion/eW91IGFyZSBhIG5lcmQ7KQ",
                ),
                onSheetButtonClicked = {},
            )
        }
    }
}

@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
fun ShareBottomSheetSharingPreviewNight() {
    ShareBottomSheetSharingPreview()
}

@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
fun ShareBottomSheetCompletePreview() {
    OnionshareTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            BottomSheet(
                state = ShareUiState.Complete,
                onSheetButtonClicked = {},
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ShareBottomSheetStoppingPreview() {
    OnionshareTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            BottomSheet(
                state = ShareUiState.Stopping,
                onSheetButtonClicked = {},
            )
        }
    }
}

@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
fun ShareBottomSheetErrorPreview() {
    OnionshareTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            BottomSheet(
                state = ShareUiState.ErrorStarting(Random.nextBoolean()),
                onSheetButtonClicked = {},
            )
        }
    }
}
