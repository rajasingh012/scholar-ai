package com.druk.lmplayground.conversation

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.druk.lmplayground.MainActivity
import com.druk.lmplayground.R
import com.druk.lmplayground.theme.PlaygroundTheme
import kotlinx.coroutines.launch

class ConversationFragment : Fragment() {

    private val viewModel: ConversationViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(inflater.context).apply {
        layoutParams = LayoutParams(MATCH_PARENT, MATCH_PARENT)

        setContent {

            val messages = viewModel.uiState.messages
            val isGenerating by viewModel.isGenerating.observeAsState()
            val progress by viewModel.modelLoadingProgress.observeAsState(0f)
            val modelInfo by viewModel.loadedModel.observeAsState()
            val modelStatus by viewModel.loadedModelStatus.observeAsState()
            val supportsThinking by viewModel.supportsThinking.observeAsState(false)
            val thinkingEnabled by viewModel.thinkingEnabled.observeAsState(false)
            val isModelReady by viewModel.isModelReady.observeAsState(false)
            val models by viewModel.models.observeAsState(emptyList())
            val sessions by viewModel.sessions.observeAsState(emptyList())
            val currentSessionId by viewModel.currentSessionId.observeAsState()
            val generationParams by viewModel.generationParams.observeAsState(GenerationParams())
            val maxContextSize by viewModel.maxContextSize.observeAsState(4096)
            val sessionModelHint by viewModel.sessionModelHint.observeAsState()
            val systemPrompt by viewModel.systemPrompt.observeAsState("")
            val systemPromptId by viewModel.systemPromptId.observeAsState()
            val recentSystemPrompts by viewModel.recentSystemPrompts.observeAsState(emptyList())
            val userError by viewModel.userError.observeAsState()
            val pendingRamWarning by viewModel.pendingRamWarning.observeAsState()
            val modelLoadError by viewModel.modelLoadError.observeAsState()
            var showParamsSheet by remember { mutableStateOf(false) }
            val selectedClassIndex by viewModel.selectedClassIndex.observeAsState(0)
            val selectedModeIndex by viewModel.selectedModeIndex.observeAsState(0)

            // Surface transient ViewModel errors (e.g. message-too-large)
            // as Toasts. The ViewModel can't show UI directly, so we
            // observe a one-shot LiveData and clear it after consumption.
            val toastContext = LocalContext.current
            LaunchedEffect(userError) {
                val msg = userError ?: return@LaunchedEffect
                Toast.makeText(toastContext, msg, Toast.LENGTH_LONG).show()
                viewModel.consumeUserError()
            }

            PlaygroundTheme {

                val scrollState = rememberLazyListState()
                val scope = rememberCoroutineScope()
                val drawerState = rememberDrawerState(DrawerValue.Closed)

                val colorScheme = MaterialTheme.colorScheme

                // Drive toolbar container color directly from scroll position.
                val isScrolled by remember {
                    derivedStateOf {
                        scrollState.firstVisibleItemIndex > 0 ||
                                scrollState.firstVisibleItemScrollOffset > 0
                    }
                }
                val topBarColors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = if (isScrolled)
                        colorScheme.surfaceContainer
                    else
                        colorScheme.surface
                )
                val inputFocusRequester = remember { FocusRequester() }
                var modelReport by remember { mutableStateOf<String?>(null) }

                // When model finishes loading, jump to bottom and open keyboard
                LaunchedEffect(isModelReady) {
                    if (isModelReady) {
                        val lastIndex = scrollState.layoutInfo.totalItemsCount - 1
                        if (lastIndex >= 0) {
                            scrollState.animateScrollToItem(lastIndex)
                        }
                        inputFocusRequester.requestFocus()
                    }
                }

                pendingRamWarning?.let { warning ->
                    AlertDialog(
                        onDismissRequest = { viewModel.dismissRamWarning() },
                        title = { Text(stringResource(R.string.low_ram_warning_title)) },
                        text = {
                            Text(
                                stringResource(
                                    R.string.low_ram_warning_message,
                                    warning.neededRam,
                                    warning.totalRam,
                                )
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = { viewModel.confirmLoadDespiteRamWarning() }) {
                                Text(stringResource(R.string.load_anyway))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { viewModel.dismissRamWarning() }) {
                                Text(stringResource(R.string.cancel))
                            }
                        }
                    )
                }

                modelLoadError?.let { message ->
                    AlertDialog(
                        onDismissRequest = { viewModel.consumeModelLoadError() },
                        title = { Text(stringResource(R.string.model_load_failed_title)) },
                        text = { Text(message) },
                        confirmButton = {
                            TextButton(onClick = { viewModel.consumeModelLoadError() }) {
                                Text(stringResource(R.string.model_load_failed_dismiss))
                            }
                        }
                    )
                }

                if (showParamsSheet) {
                    GenerationParamsSheet(
                        params = generationParams,
                        maxContextSize = maxContextSize,
                        supportsThinking = supportsThinking,
                        systemPrompt = systemPrompt,
                        canUpdateLinkedPrompt = systemPromptId != null,
                        onParamsChanged = { viewModel.updateGenerationParams(it) },
                        onUpdateLinkedPrompt = { viewModel.updateLinkedSystemPrompt(it) },
                        onSaveAsNewPrompt = { viewModel.createAndApplySystemPrompt(it) },
                        onClearSystemPrompt = { viewModel.clearSystemPrompt() },
                        onDismiss = { showParamsSheet = false }
                    )
                }

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        SessionListDrawer(
                            sessions = sessions,
                            currentSessionId = currentSessionId,
                            onSessionSelected = { sessionId ->
                                viewModel.loadSession(sessionId)
                                scope.launch { drawerState.close() }
                            },
                            onDeleteSession = { sessionId ->
                                viewModel.deleteSession(sessionId)
                            },
                            onRenameSession = { sessionId, newTitle ->
                                viewModel.renameSession(sessionId, newTitle)
                            },
                            onPinSession = { sessionId, pinned ->
                                viewModel.pinSession(sessionId, pinned)
                            },
                            onSettingsClicked = {
                                scope.launch {
                                    drawerState.close()
                                    if (findNavController().currentDestination?.id == R.id.nav_home) {
                                        findNavController().navigate(R.id.action_home_to_settings)
                                    }
                                }
                            }
                        )
                    }
                ) {
                    Scaffold(
                        topBar = {
                            ConversationBar(
                                modelInfo = modelInfo,
                                modelStatus = modelStatus,
                                onNavIconPressed = {
                                    scope.launch { drawerState.open() }
                                },
                                colors = topBarColors,
                                onModelNamePressed = { },
                                onNewSessionPressed = {
                                    viewModel.newConversation()
                                }
                            )
                            if (modelReport != null) {
                                AlertDialog(
                                    onDismissRequest = {
                                        modelReport = null
                                    },
                                    title = {
                                        Text(text = stringResource(R.string.session_info))
                                    },
                                    text = {
                                        Text(
                                            text = modelReport!!,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    },
                                    confirmButton = {
                                        TextButton(onClick = { modelReport = null }) {
                                            Text(text = stringResource(R.string.close))
                                        }
                                    }
                                )
                            }
                        },
                        // Exclude ime and navigation bar padding so this can be added by the UserInput composable
                        contentWindowInsets = ScaffoldDefaults
                            .contentWindowInsets
                            .exclude(WindowInsets.navigationBars)
                            .exclude(WindowInsets.ime),
                        modifier = Modifier
                    ) { paddingValues ->
                        Column(
                            Modifier
                                .fillMaxSize()
                                .padding(paddingValues)
                                .drawBehind {
                                    val strokeWidth = 2.dp.toPx()
                                    val x = size.width * progress
                                    drawLine(
                                        colorScheme.primary,
                                        start = Offset(0f, 0f),
                                        end = Offset(x, 0f),
                                        strokeWidth = strokeWidth
                                    )
                                }) {
                            if (messages.isEmpty()) {
                                Box(
                                    modifier = Modifier.weight(1f).fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    HeroBanner()
                                }
                            } else {
                                Messages(
                                    messages = messages,
                                    modifier = Modifier.weight(1f),
                                    scrollState = scrollState,
                                    isGenerating = isGenerating == true,
                                    sessionModelHint = sessionModelHint,
                                    onSessionModelHintClick = { filename ->
                                        viewModel.loadModelByFilename(filename)
                                    },
                                    onSessionModelHintDismiss = {
                                        viewModel.dismissSessionModelHint()
                                    },
                                    onTokenCountClicked = {
                                        modelReport = viewModel.getReport()
                                    }
                                )
                            }
                            // Picker sits just above the composer. Visible only when:
                            //   - model is ready
                            //   - chat is empty
                            //   - library has entries
                            //   - the session has no prompt selected yet
                            // On pick: the picker row handles its own flight
                            // animation per-card, then fires onPick — which flips
                            // the visibility gate. Exit uses ExitTransition.None
                            // so the row disappears immediately after the card
                            // finishes flying (no double-animation).
                            // On clear: the row slides back up and fades in.
                            val pickerVisible = isModelReady &&
                                messages.isEmpty() &&
                                recentSystemPrompts.isNotEmpty() &&
                                systemPrompt.isEmpty()
                            androidx.compose.animation.AnimatedVisibility(
                                visible = pickerVisible,
                                enter = androidx.compose.animation.fadeIn(
                                    animationSpec = androidx.compose.animation.core.tween(220)
                                ) + androidx.compose.animation.slideInVertically(
                                    animationSpec = androidx.compose.animation.core.tween(220),
                                    initialOffsetY = { it / 2 }
                                ),
                                exit = androidx.compose.animation.ExitTransition.None
                            ) {
                                SystemPromptPickerRow(
                                    prompts = recentSystemPrompts,
                                    selectedText = systemPrompt,
                                    onPick = { viewModel.applySystemPrompt(it.id, it.text) },
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }
                            // NCERT Class & Mode selector above the input
                            ClassModeSelector(
                                selectedClassIndex = selectedClassIndex,
                                selectedModeIndex = selectedModeIndex,
                                onClassSelected = { viewModel.setSelectedClassIndex(it) },
                                onModeSelected = { viewModel.setSelectedModeIndex(it) },
                                modifier = Modifier.padding(bottom = 4.dp)
                            )

                            UserInput(
                                modifier = Modifier
                                    .navigationBarsPadding()
                                    .imePadding(),
                                focusRequester = inputFocusRequester,
                                status = if (modelInfo == null || !isModelReady)
                                    UserInputStatus.NOT_LOADED
                                else if (isGenerating == true)
                                    UserInputStatus.GENERATING
                                else
                                    UserInputStatus.IDLE,
                                supportsThinking = supportsThinking,
                                thinkingEnabled = thinkingEnabled,
                                onThinkingToggle = { viewModel.toggleThinking() },
                                onSwipeUp = {
                                    if (isModelReady) showParamsSheet = true
                                },
                                onMessageSent = { content ->
                                    viewModel.addMessage(
                                        Message("User", content)
                                    )
                                },
                                onCancelClicked = {
                                    viewModel.cancelGeneration()
                                },
                                // let this element handle the padding so that the elevation is shown behind the
                                // navigation bar
                                resetScroll = {
                                    scope.launch {
                                        val lastIndex = scrollState.layoutInfo.totalItemsCount - 1
                                        if (lastIndex >= 0) {
                                            scrollState.animateScrollToItem(lastIndex)
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
