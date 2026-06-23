package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AttendeeNotification
import com.example.data.ChatMessage
import com.example.data.EventSession
import com.example.ui.EventTab
import com.example.ui.EventViewModel
import com.example.ui.LivePoll
import kotlinx.coroutines.launch

// Premium Dark Theme Color Tokens
private val DeepSpaceBlack = Color(0xFF0F121D)
private val CyberCardBlue = Color(0xFF171D2F)
private val NeonCyan = Color(0xFF00E5FF)
private val NeonPurple = Color(0xFFD500F9)
private val AmbientGradientStart = Color(0xFF101424)
private val AmbientGradientEnd = Color(0xFF080B13)
private val TextWhite = Color(0xFFF1F5F9)
private val TextMuted = Color(0xFF94A3B8)
private val RedBadge = Color(0xFFEF4444)
private val GreenActive = Color(0xFF10B981)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EventSpaceTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets.safeDrawing
                ) { innerPadding ->
                    EventSpaceApp(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun EventSpaceTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = NeonCyan,
            secondary = NeonPurple,
            background = DeepSpaceBlack,
            surface = CyberCardBlue,
            onPrimary = Color.Black,
            onSecondary = Color.White,
            onBackground = TextWhite,
            onSurface = TextWhite
        ),
        content = content
    )
}

@Composable
fun EventSpaceApp(
    modifier: Modifier = Modifier,
    viewModel: EventViewModel = viewModel()
) {
    val userReg by viewModel.userRegistration.collectAsStateWithLifecycle()
    val currentTabState = viewModel.currentTab.value
    
    Box(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    colors = listOf(AmbientGradientStart, AmbientGradientEnd)
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Elegant Technical Hub Banner Header
            EventHeaderBar(
                isRegistered = userReg != null,
                userName = userReg?.name ?: "",
                activeParticipants = viewModel.activeParticipants.value
            )

            if (userReg == null) {
                // Strictly enforces user profile/registration setup first
                RegistrationScreen(
                    onRegister = { name, email, job, targetInterests ->
                        viewModel.registerAttendee(name, email, job, targetInterests)
                    }
                )
            } else {
                // Tab Navigation layout for registered attendees
                Column(modifier = Modifier.fillMaxSize()) {
                    EventTabs(
                        selectedTab = currentTabState,
                        onTabSelected = { viewModel.currentTab.value = it }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        when (currentTabState) {
                            EventTab.REGISTRATION -> ProfileBadgeScreen(
                                name = userReg?.name ?: "",
                                email = userReg?.email ?: "",
                                jobTitle = userReg?.jobTitle ?: "",
                                interests = userReg?.interests ?: "",
                                onUpdateProfile = { n, j, i -> viewModel.updateProfile(n, j, i) },
                                onReset = { viewModel.unregister() }
                            )
                            EventTab.LIVE_STREAM -> LiveStreamScreen(viewModel = viewModel)
                            EventTab.ANALYTICS -> AnalyticsScreen(viewModel = viewModel)
                            EventTab.INBOX -> InboxScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EventHeaderBar(
    isRegistered: Boolean,
    userName: String,
    activeParticipants: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(containerColor = CyberCardBlue.copy(alpha = 0.85f)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Interactive star badge logo
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(NeonCyan, NeonPurple)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "EventSpace Logo",
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "EventSpace",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.5.sp,
                            color = NeonCyan
                        )
                    )
                    Text(
                        text = "VIRTUAL CON-2026",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = TextMuted
                        )
                    )
                }
            }

            if (isRegistered) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    // Blinking audience count indicator
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.5f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(GreenActive)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "$activeParticipants LIVE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = GreenActive
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EventTabs(
    selectedTab: EventTab,
    onTabSelected: (EventTab) -> Unit
) {
    ScrollableTabRow(
        selectedTabIndex = selectedTab.ordinal,
        containerColor = Color.Transparent,
        contentColor = NeonCyan,
        edgePadding = 8.dp,
        divider = {}
    ) {
        EventTab.values().forEach { tab ->
            val icon = when (tab) {
                EventTab.REGISTRATION -> Icons.Default.AccountBox
                EventTab.LIVE_STREAM -> Icons.Default.PlayArrow
                EventTab.ANALYTICS -> Icons.Default.Star
                EventTab.INBOX -> Icons.Default.Email
            }
            val title = when (tab) {
                EventTab.REGISTRATION -> "My Badge"
                EventTab.LIVE_STREAM -> "Streams & Chat"
                EventTab.ANALYTICS -> "Telemetry Panel"
                EventTab.INBOX -> "Email Inbox"
            }
            
            Tab(
                selected = selectedTab == tab,
                onClick = { onTabSelected(tab) },
                icon = { Icon(imageVector = icon, contentDescription = title) },
                text = {
                    Text(
                        text = title, 
                        fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal,
                        letterSpacing = 0.5.sp
                    )
                }
            )
        }
    }
}

@Composable
fun RegistrationScreen(
    onRegister: (String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var jobTitle by remember { mutableStateOf("") }
    
    // Preset suggest tags
    val availableInterests = listOf("Kotlin", "Jetpack Compose", "Gemini API", "Telemetry Tracking", "UX Engineering", "Serverless")
    val selectedInterests = remember { mutableStateListOf<String>() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Text(
                    text = "Welcome to EventSpace",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 24.sp,
                        color = NeonCyan,
                        textAlign = TextAlign.Center
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Register to unlock the interactive live streams, personalized schedules, attendee chats, and live speaker telemetry dashboard.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextMuted,
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CyberCardBlue),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.15f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "ATTENDEE PASS PROFILE",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = NeonPurple,
                            letterSpacing = 1.sp
                        )
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Full Name") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Name") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = TextMuted.copy(alpha = 0.3f),
                            focusedLabelColor = NeonCyan
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("registration_name_input"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = TextMuted.copy(alpha = 0.3f),
                            focusedLabelColor = NeonCyan
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("registration_email_input"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = jobTitle,
                        onValueChange = { jobTitle = it },
                        label = { Text("Professional Title") },
                        leadingIcon = { Icon(Icons.Default.Build, contentDescription = "Job Title") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = TextMuted.copy(alpha = 0.3f),
                            focusedLabelColor = NeonCyan
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("registration_job_input"),
                        singleLine = true
                    )

                    Text(
                        text = "Customize My Core Interests",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        ),
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    // Flows interactive interest cloud tags
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        availableInterests.forEach { interest ->
                            val isSelected = selectedInterests.contains(interest)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) NeonCyan.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.3f)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) NeonCyan else TextMuted.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        if (isSelected) {
                                            selectedInterests.remove(interest)
                                        } else {
                                            selectedInterests.add(interest)
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = NeonCyan,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = interest,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = if (isSelected) NeonCyan else TextWhite,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Button(
                onClick = {
                    if (name.isNotBlank() && email.isNotBlank()) {
                        val interestsStr = if (selectedInterests.isEmpty()) "General Technology" else selectedInterests.joinToString(", ")
                        onRegister(name, email, jobTitle, interestsStr)
                    }
                },
                enabled = name.isNotBlank() && email.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonCyan,
                    contentColor = Color.Black,
                    disabledContainerColor = TextMuted.copy(alpha = 0.3f),
                    disabledContentColor = TextMuted
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("submit_registration")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = "Scan")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "GENERATE PASS BADGE & INBOX",
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileBadgeScreen(
    name: String,
    email: String,
    jobTitle: String,
    interests: String,
    onUpdateProfile: (String, String, String) -> Unit,
    onReset: () -> Unit
) {
    var editName by remember { mutableStateOf(name) }
    var editJob by remember { mutableStateOf(jobTitle) }
    var editInterests by remember { mutableStateOf(interests) }
    var isEditing by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (!isEditing) {
            // Elegant Vector-like Conference Pass Badge Card
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(340.dp),
                colors = CardDefaults.cardColors(containerColor = CyberCardBlue),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawBehind {
                            // Subtly draw glowing radial gradient inside the pass card
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(NeonPurple.copy(alpha = 0.15f), Color.Transparent),
                                    center = Offset(size.width * 0.8f, size.height * 0.2f),
                                    radius = 200.dp.toPx()
                                )
                            )
                        }
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "OFFICIAL CON PASS",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan,
                                letterSpacing = 2.sp
                            )
                        )
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "NFC Enabled",
                            tint = NeonPurple,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Avatar circle placeholder
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.3f))
                            .border(2.dp, NeonCyan, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Premium Tier badge",
                            tint = NeonCyan,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            color = TextWhite
                        )
                    )
                    Text(
                        text = if (jobTitle.isNotBlank()) jobTitle else "Technical Professional",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = NeonPurple
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Custom horizontal separator
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                    ) {
                        drawLine(
                            color = TextMuted.copy(alpha = 0.25f),
                            start = Offset(0f, 0f),
                            end = Offset(size.width, 0f),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Curated Tracks: $interests",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextMuted,
                            textAlign = TextAlign.Center
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // Custom Barcode rendered natively via clean geometric lines inside Canvas!
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height(36.dp)
                    ) {
                        val barWidth = 4.dp.toPx()
                        val spacerWidth = 2.dp.toPx()
                        var currentX = 0f
                        val pattern = listOf(true, false, true, true, false, true, false, true, true, true, false, true, false, false, true, true, false, true, true, false, true)
                        
                        while (currentX < size.width) {
                            for (draw in pattern) {
                                if (currentX >= size.width) break
                                if (draw) {
                                    drawRect(
                                        color = NeonCyan,
                                        topLeft = Offset(currentX, 0f),
                                        size = Size(barWidth, size.height)
                                    )
                                }
                                currentX += barWidth + spacerWidth
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(0.9f),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { isEditing = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextWhite),
                    border = BorderStroke(1.dp, TextMuted.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Profile")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Edit Profile")
                }

                Button(
                    onClick = onReset,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RedBadge.copy(alpha = 0.2f),
                        contentColor = RedBadge
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Pass")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Delete Pass")
                }
            }
        } else {
            // Live Profile Editor Form
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CyberCardBlue),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, NeonPurple.copy(alpha = 0.25f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "UPDATE PASS DETAILS",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan,
                            letterSpacing = 1.sp
                        )
                    )

                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Display Name") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonCyan),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editJob,
                        onValueChange = { editJob = it },
                        label = { Text("Job Title") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonCyan),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editInterests,
                        onValueChange = { editInterests = it },
                        label = { Text("Manual Interests (split by comma)") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonCyan),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { isEditing = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }

                        Button(
                            onClick = {
                                onUpdateProfile(editName, editJob, editInterests)
                                isEditing = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black),
                            modifier = Modifier.weight(1.5f)
                        ) {
                            Text("Save Updates")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LiveStreamScreen(
    viewModel: EventViewModel
) {
    val sessions by viewModel.allSessions.collectAsStateWithLifecycle()
    val activeSession = viewModel.activeSession.value
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val isPlaying = viewModel.isPlaying.value
    val isUserFocused = viewModel.isUserFocused.value
    val activePoll = viewModel.activeSessionPoll.value

    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp > 600

    if (activeSession == null) {
        // Fallback UI to select a starting stream
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "No Stream",
                tint = TextMuted,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Select a Session to Stream",
                style = MaterialTheme.typography.titleLarge,
                color = TextWhite
            )
            Spacer(modifier = Modifier.height(24.dp))
            sessions.forEach { session ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { viewModel.selectSession(session) },
                    colors = CardDefaults.cardColors(containerColor = CyberCardBlue)
                ) {
                    ListItem(
                        headlineContent = { Text(session.title, fontWeight = FontWeight.Bold) },
                        supportingContent = { Text(session.speaker) },
                        trailingContent = { Icon(Icons.Default.PlayArrow, contentDescription = "Play") }
                    )
                }
            }
        }
    } else {
        // Primary Live stream player split layout
        if (isTablet) {
            // Adaptive horizontal pane for tables/chromebooks
            Row(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .weight(1.2f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp)
                ) {
                    LiveStreamPlayerSection(
                        activeSession = activeSession,
                        isPlaying = isPlaying,
                        currentSlideText = viewModel.currentSlideText.value,
                        isUserFocused = isUserFocused,
                        onTogglePlay = { viewModel.togglePlayback() },
                        onToggleFocus = { viewModel.isUserFocused.value = !viewModel.isUserFocused.value }
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    
                    StreamSessionSelector(
                        sessions = sessions,
                        currentSessionId = activeSession.sessionId,
                        onSessionSelected = { viewModel.selectSession(it) }
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(horizontal = 8.dp)
                ) {
                    ChatAndPollSection(
                        chatMessages = chatMessages,
                        activePoll = activePoll,
                        onSendMessage = { text, isQ -> viewModel.sendChatMessage(text, isQ) },
                        onVote = { index -> viewModel.submitPollResponse(index) }
                    )
                }
            }
        } else {
            // Classic stacked layout for standard mobile views
            Column(modifier = Modifier.fillMaxSize()) {
                // Video Player takes fixed premium viewport
                LiveStreamPlayerSection(
                    activeSession = activeSession,
                    isPlaying = isPlaying,
                    currentSlideText = viewModel.currentSlideText.value,
                    isUserFocused = isUserFocused,
                    onTogglePlay = { viewModel.togglePlayback() },
                    onToggleFocus = { viewModel.isUserFocused.value = !viewModel.isUserFocused.value }
                )

                // Navigation slides and other tracks collapsible
                var showTracksSelector by remember { mutableStateOf(false) }

                Column(modifier = Modifier.weight(1f)) {
                    if (showTracksSelector) {
                        Box(modifier = Modifier.fillMaxSize().background(DeepSpaceBlack)) {
                            StreamSessionSelector(
                                sessions = sessions,
                                currentSessionId = activeSession.sessionId,
                                onSessionSelected = {
                                    viewModel.selectSession(it)
                                    showTracksSelector = false
                                }
                            )
                        }
                    } else {
                        // Regular live interaction feed (Chat & Poll)
                        ChatAndPollSection(
                            chatMessages = chatMessages,
                            activePoll = activePoll,
                            onSendMessage = { text, isQ -> viewModel.sendChatMessage(text, isQ) },
                            onVote = { index -> viewModel.submitPollResponse(index) },
                            modifier = Modifier.weight(1f),
                            headerExtra = {
                                OutlinedButton(
                                    onClick = { showTracksSelector = true },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
                                    border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)),
                                    modifier = Modifier.height(32.dp).padding(horizontal = 4.dp)
                                ) {
                                    Icon(Icons.Default.List, contentDescription = "Tracks", modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Sessions", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LiveStreamPlayerSection(
    activeSession: EventSession,
    isPlaying: Boolean,
    currentSlideText: String,
    isUserFocused: Boolean,
    onTogglePlay: () -> Unit,
    onToggleFocus: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black),
        border = BorderStroke(1.dp, NeonPurple.copy(alpha = 0.25f))
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Color.Black)
            ) {
                // If live stream is simulated, render our high-quality event_banner asset!
                Image(
                    painter = painterResource(id = R.drawable.event_banner),
                    contentDescription = "Event Live Banner",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Deep color transparent overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                            )
                        )
                )

                // Telemetry status tags (Overlayed)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        // Live badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(RedBadge)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "LIVE STREAM",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                        }

                        // Telemetry focus tracker node
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isUserFocused) GreenActive.copy(alpha = 0.25f) else RedBadge.copy(alpha = 0.25f),
                            border = BorderStroke(1.dp, if (isUserFocused) GreenActive else RedBadge),
                            modifier = Modifier.clickable { onToggleFocus() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (isUserFocused) GreenActive else RedBadge)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isUserFocused) "Focused (Active)" else "Away (Lost Focus)",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isUserFocused) GreenActive else RedBadge
                                )
                            }
                        }
                    }

                    // Playback Status Indicator Overlay
                    if (!isPlaying) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.7f))
                                .clickable { onTogglePlay() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play Stream",
                                tint = NeonCyan,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    // Active Presenter details (Bottom-Left)
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(bottom = 4.dp)
                    ) {
                        Text(
                            text = activeSession.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Speaker",
                                tint = NeonCyan,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${activeSession.speaker} (${activeSession.speakerTitle})",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextWhite.copy(alpha = 0.8f)
                                )
                            )
                        }
                    }
                }
            }

            // Real-time Presentation slide synchronizer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "ACTIVE PRESENTATION SLIDE:",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = NeonPurple,
                                letterSpacing = 1.sp
                            )
                        )
                        Text(
                            text = currentSlideText,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextWhite
                            )
                        )
                    }

                    IconButton(onClick = onTogglePlay) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Refresh else Icons.Default.PlayArrow,
                            contentDescription = "Toggle streams",
                            tint = NeonCyan
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StreamSessionSelector(
    sessions: List<EventSession>,
    currentSessionId: String,
    onSessionSelected: (EventSession) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Text(
            text = "EVENT INTERACTIVE TRACKS",
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold,
                color = NeonCyan,
                letterSpacing = 1.sp
            ),
            modifier = Modifier.padding(bottom = 6.dp)
        )

        sessions.forEach { session ->
            val isActive = session.sessionId == currentSessionId
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .border(
                        width = 1.dp,
                        color = if (isActive) NeonCyan else Color.Transparent,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable { onSessionSelected(session) },
                colors = CardDefaults.cardColors(
                    containerColor = if (isActive) CyberCardBlue.copy(alpha = 0.9f) else CyberCardBlue.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (isActive) NeonCyan.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Event Status",
                            tint = if (isActive) NeonCyan else TextMuted
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = session.title,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isActive) NeonCyan else TextWhite
                            )
                        )
                        Text(
                            text = "${session.speaker} | ${session.time}",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatAndPollSection(
    chatMessages: List<ChatMessage>,
    activePoll: LivePoll?,
    onSendMessage: (String, Boolean) -> Unit,
    onVote: (Int) -> Unit,
    modifier: Modifier = Modifier,
    headerExtra: @Composable () -> Unit = {}
) {
    var textMessage by remember { mutableStateOf("") }
    var isQuestionCheck by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Auto-scroll chat to bottom when message list expands
    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            scope.launch {
                listState.animateScrollToItem(chatMessages.size - 1)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        // Tab Chat header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Send, contentDescription = "Attendee Feed", tint = NeonPurple)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "AUDIENCE INTERACTION",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextWhite,
                        letterSpacing = 1.sp
                    )
                )
            }
            headerExtra()
        }

        // Active Presentation live poll card if present
        if (activePoll != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
                    .padding(bottom = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.5f)),
                border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = "Active Poll", tint = NeonCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "LIVE SPEAKER POLL",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = NeonCyan,
                                    letterSpacing = 1.sp
                                )
                            )
                        }
                        if (activePoll.userSelectedOption != null) {
                            Text(
                                text = "VOTED",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = GreenActive
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = activePoll.question,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Vote options bars
                    val totalVotes = activePoll.votes.sum().coerceAtLeast(1)
                    activePoll.options.forEachIndexed { index, option ->
                        val hasVoted = activePoll.userSelectedOption != null
                        val percentage = (activePoll.votes[index].toFloat() / totalVotes * 100).toInt()

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.Black.copy(alpha = 0.3f))
                                .clickable(enabled = !hasVoted) { onVote(index) }
                        ) {
                            // Custom responsive fill percentage rendering natively!
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(if (hasVoted) (percentage.toFloat() / 100f) else 0f)
                                    .fillMaxHeight()
                                    .align(Alignment.CenterStart)
                                    .background(
                                        if (activePoll.userSelectedOption == index) NeonCyan.copy(alpha = 0.3f)
                                        else TextMuted.copy(alpha = 0.15f)
                                    )
                                    .matchParentSize()
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    if (activePoll.userSelectedOption == index) {
                                        Icon(Icons.Default.Check, contentDescription = "My Vote", tint = NeonCyan, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = option,
                                        fontSize = 11.sp,
                                        color = TextWhite,
                                        maxLines = 1
                                    )
                                }
                                if (hasVoted) {
                                    Text(
                                        text = "$percentage%",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (activePoll.userSelectedOption == index) NeonCyan else TextMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Live Chat list view
        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CyberCardBlue.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(chatMessages) { message ->
                    AttendeeChatMessageRow(message = message)
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Text input controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { isQuestionCheck = !isQuestionCheck },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (isQuestionCheck) NeonPurple.copy(alpha = 0.2f) else Color.Transparent)
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Ask Question Toggle",
                    tint = if (isQuestionCheck) NeonPurple else TextMuted
                )
            }

            OutlinedTextField(
                value = textMessage,
                onValueChange = { textMessage = it },
                placeholder = {
                    Text(
                        text = if (isQuestionCheck) "Submit question to presenter..." else "Type inside active chat room...",
                        fontSize = 12.sp
                    )
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (isQuestionCheck) NeonPurple else NeonCyan,
                    focusedLabelColor = if (isQuestionCheck) NeonPurple else NeonCyan
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_input_text")
            )

            Spacer(modifier = Modifier.width(6.dp))

            FloatingActionButton(
                onClick = {
                    if (textMessage.isNotBlank()) {
                        onSendMessage(textMessage, isQuestionCheck)
                        textMessage = ""
                        isQuestionCheck = false
                    }
                },
                containerColor = if (isQuestionCheck) NeonPurple else NeonCyan,
                contentColor = Color.Black,
                shape = CircleShape,
                modifier = Modifier
                    .size(44.dp)
                    .testTag("send_chat_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send Message",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun AttendeeChatMessageRow(message: ChatMessage) {
    val isSystem = message.userName == "System Broadcast" || message.userName == "System Moderator"
    val isOrganizer = message.userRole == "Organizer" || message.userRole == "Moderator"
    
    if (isSystem) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp, horizontal = 8.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(NeonPurple.copy(alpha = 0.1f))
                .border(0.5.dp, NeonPurple.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                .padding(8.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Notifications, contentDescription = "Broadcast", tint = NeonPurple, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "SESSION SYSTEM ANNOUNCEMENT",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = NeonPurple,
                        letterSpacing = 0.5.sp
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = message.text,
                    fontSize = 11.sp,
                    color = TextWhite,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    } else {
        val bubbleBg = when {
            isOrganizer -> NeonCyan.copy(alpha = 0.1f)
            message.userRole == "Speaker" -> NeonPurple.copy(alpha = 0.12f)
            message.isAI -> Color.Black.copy(alpha = 0.15f)
            else -> CyberCardBlue
        }
        val borderHighlight = when {
            isOrganizer -> BorderStroke(0.5.dp, NeonCyan.copy(alpha = 0.3f))
            message.userRole == "Speaker" -> BorderStroke(0.5.dp, NeonPurple.copy(alpha = 0.3f))
            else -> null
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            colors = CardDefaults.cardColors(containerColor = bubbleBg),
            border = borderHighlight,
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = message.userName,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (message.userRole == "Speaker") NeonPurple else TextWhite
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    when (message.userRole) {
                                        "Speaker" -> NeonPurple.copy(alpha = 0.2f)
                                        "Organizer", "Moderator" -> NeonCyan.copy(alpha = 0.2f)
                                        else -> Color.Black.copy(alpha = 0.3f)
                                    }
                                )
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = message.userRole.uppercase(),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (message.userRole == "Speaker") NeonPurple else NeonCyan
                            )
                        }
                    }

                    Text(
                        text = if (message.isAI) "🤖 SIMULATED" else "👥 USER",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (message.isAI) NeonPurple.copy(alpha = 0.7f) else TextMuted
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = if (message.userRole == "Speaker") TextWhite else TextWhite.copy(alpha = 0.9f)
                    )
                )
            }
        }
    }
}

@Composable
fun AnalyticsScreen(
    viewModel: EventViewModel
) {
    val activeSession = viewModel.activeSession.value
    val allMetrics by viewModel.allMetrics.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val isUserFocused = viewModel.isUserFocused.value
    
    // Sum all metric parameters to render on dashboard
    val totalWatched = allMetrics.sumOf { it.watchedSeconds }
    val totalFocused = allMetrics.sumOf { it.activeFocusSeconds }
    val totalChats = allMetrics.sumOf { it.chatMessagesSent }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "ORGANIZER TELEMETRY PANEL",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = NeonCyan,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                )
                Text(
                    text = "Real-Time Attendee Engagement Analytics",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                )
            }
        }

        // Live focus quotient and telemetry counter
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CyberCardBlue),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "LIVE INTERACTION COUNTERS (TOTAL)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = NeonPurple,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TelemetryCounterItem(
                            title = "Accumulated Watch",
                            value = "${totalWatched}s",
                            icon = Icons.Default.Star,
                            color = NeonCyan
                        )
                        TelemetryCounterItem(
                            title = "Engaged Focus",
                            value = "${totalFocused}s",
                            icon = Icons.Default.Search,
                            color = GreenActive
                        )
                        TelemetryCounterItem(
                            title = "Chat Contribution",
                            value = "$totalChats msgs",
                            icon = Icons.Default.Send,
                            color = NeonPurple
                        )
                    }
                }
            }
        }

        // Visual Custom Canvas Telemetry Chart Card!
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CyberCardBlue),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, NeonPurple.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ATTENDEE RETENTION LINE GRAPH",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = NeonCyan,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(NeonCyan.copy(alpha = 0.1f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("Continuous", fontSize = 8.sp, color = NeonCyan, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Premium Custom Canvas drawing details
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .background(Color.Black.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                    ) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp)
                        ) {
                            // Draw grid lines
                            val gridCount = 4
                            for (i in 1..gridCount) {
                                val y = size.height / (gridCount + 1) * i
                                drawLine(
                                    color = TextMuted.copy(alpha = 0.1f),
                                    start = Offset(0f, y),
                                    end = Offset(size.width, y),
                                    strokeWidth = 1.dp.toPx()
                                )
                            }

                            // Dynamic Retention Line path mapping
                            val points = listOf(
                                Offset(0f, size.height * 0.1f),
                                Offset(size.width * 0.2f, size.height * 0.2f),
                                Offset(size.width * 0.4f, size.height * 0.35f),
                                Offset(size.width * 0.6f, size.height * 0.45f),
                                Offset(size.width * 0.8f, size.height * 0.22f), // recovers at Slide 5 / Q&A interaction
                                Offset(size.width, size.height * 0.15f)
                            )

                            val path = Path().apply {
                                moveTo(points[0].x, points[0].y)
                                for (index in 1 until points.size) {
                                    val prev = points[index - 1]
                                    val curr = points[index]
                                    quadraticBezierTo(
                                        prev.x + (curr.x - prev.x) / 2f, prev.y,
                                        curr.x, curr.y
                                    )
                                }
                            }

                            // Draw gradient under line
                            val gradientPath = Path().apply {
                                addPath(path)
                                lineTo(size.width, size.height)
                                lineTo(0f, size.height)
                                close()
                            }
                            drawPath(
                                path = gradientPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(NeonCyan.copy(alpha = 0.3f), Color.Transparent)
                                )
                            )

                            // Draw primary retention line stroke
                            drawPath(
                                path = path,
                                color = NeonCyan,
                                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                            )

                            // Highlight active spot
                            val spotIndex = 4
                            drawCircle(
                                color = NeonPurple,
                                radius = 6.dp.toPx(),
                                center = points[spotIndex]
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Slide 1 (Start)", fontSize = 9.sp, color = TextMuted)
                        Text("Slide 3 (Core)", fontSize = 9.sp, color = TextMuted)
                        Text("Slide 5 (Q&A)", fontSize = 9.sp, color = NeonPurple, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Live AI Critique Consultant (Continuous Gemini evaluation based on user interaction!)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CyberCardBlue),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, NeonPurple.copy(alpha = 0.25f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "AI Critique",
                            tint = NeonPurple,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "GEMINI CO-PILOT CRITIQUE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = NeonPurple,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = viewModel.aiCritique.value,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextWhite,
                                lineHeight = 18.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Critique updates automatically every 15s when live stream playback is active.",
                        fontSize = 9.sp,
                        color = TextMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun TelemetryCounterItem(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = title, tint = color, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = value, style = MaterialTheme.typography.titleMedium.copy(color = color))
        Text(text = title, fontSize = 9.sp, color = TextMuted)
    }
}

@Composable
fun InboxScreen(
    viewModel: EventViewModel
) {
    val inbox by viewModel.allNotifications.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "TRANSACTIONAL MAIL SINK",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = NeonCyan,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                    )
                    Text(
                        text = "Automated Email Notification Logs",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                    )
                }

                IconButton(
                    onClick = { viewModel.clearNotifications() },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(RedBadge.copy(alpha = 0.15f))
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Clear Mailbox", tint = RedBadge)
                }
            }
        }

        if (inbox.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    colors = CardDefaults.cardColors(containerColor = CyberCardBlue.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(imageVector = Icons.Default.Email, contentDescription = "Mailbox Empty", tint = TextMuted, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Mailbox is completely empty",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = TextWhite)
                        )
                        Text(
                            text = "Register or trigger profile modifications to fire transactional emails.",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, textAlign = TextAlign.Center)
                        )
                    }
                }
            }
        } else {
            items(inbox) { email ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CyberCardBlue),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.15f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(NeonCyan.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = Icons.Default.Email, contentDescription = "Custom Email", tint = NeonCyan, modifier = Modifier.size(16.dp))
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = email.recipientEmail,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = TextWhite
                                        )
                                    )
                                    Text(
                                        text = "To: Attendee Inbox",
                                        fontSize = 10.sp,
                                        color = TextMuted
                                    )
                                }
                            }

                            // Dynamic simulated send tag
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(GreenActive.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "SENT SUCCESSFULLY",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = GreenActive,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = email.subject,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = email.body,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextWhite.copy(alpha = 0.85f),
                                lineHeight = 16.sp
                            )
                        )
                    }
                }
            }
        }
    }
}
