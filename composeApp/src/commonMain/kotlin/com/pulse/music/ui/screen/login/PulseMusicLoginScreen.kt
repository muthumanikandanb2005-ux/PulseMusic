package com.pulse.music.ui.screen.login

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.domain.repository.SupabaseAuthRepository
import com.maxrave.simpmusic.ui.component.RippleIconButton
import com.maxrave.simpmusic.ui.icon.ArrowBackIosNew
import com.maxrave.simpmusic.ui.icon.Check
import com.maxrave.simpmusic.ui.icon.Close
import com.maxrave.simpmusic.ui.icon.KeyboardArrowDown
import com.maxrave.simpmusic.ui.icon.Subtitles
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.theme.typo
import com.pulse.music.ui.screen.profile.GENDER_OPTIONS
import com.pulse.music.ui.screen.profile.INDIAN_LANGUAGES
import com.pulse.music.ui.screen.profile.IndianLanguageInfo
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

private val BitChordNeonGreen = Color(0xFF00E676)
private val BitChordOledBlack = Color(0xFF000000)
private val BitChordFieldSurface = Color(0xFF101014)
private val BitChordCardSurface = Color(0xFF0C130E)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PulseMusicLoginScreen(
    innerPadding: PaddingValues,
    navController: NavController,
    hideBottomNavigation: () -> Unit = {},
    showBottomNavigation: () -> Unit = {},
    supabaseAuthRepository: SupabaseAuthRepository = koinInject(),
    dataStoreManager: DataStoreManager = koinInject(),
) {
    val coroutineScope = rememberCoroutineScope()
    val isLoggedIn by supabaseAuthRepository.isLoggedIn.collectAsStateWithLifecycle(false)
    val userEmail by supabaseAuthRepository.currentUserEmail.collectAsStateWithLifecycle(null)

    val savedProfileName by dataStoreManager.profileName.collectAsStateWithLifecycle("")
    val savedProfileAge by dataStoreManager.profileAge.collectAsStateWithLifecycle("")
    val savedProfileGender by dataStoreManager.profileGender.collectAsStateWithLifecycle("")
    val savedProfileLanguage by dataStoreManager.profileLanguagePreference.collectAsStateWithLifecycle("")

    var isSignUpMode by remember { mutableStateOf(false) }

    // User Profile Form State
    var name by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("Male") }
    var languagePreference by remember { mutableStateOf("Tamil (தமிழ்)") }

    var showLanguagePicker by remember { mutableStateOf(false) }

    // Auth Form State
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    // Populate profile inputs once loaded
    LaunchedEffect(savedProfileName, savedProfileAge, savedProfileGender, savedProfileLanguage) {
        if (name.isEmpty() && savedProfileName.isNotEmpty()) name = savedProfileName
        if (age.isEmpty() && savedProfileAge.isNotEmpty()) age = savedProfileAge
        if (savedProfileGender.isNotEmpty()) gender = savedProfileGender
        if (savedProfileLanguage.isNotEmpty()) languagePreference = savedProfileLanguage
    }

    LaunchedEffect(Unit) {
        hideBottomNavigation()
    }

    DisposableEffect(Unit) {
        onDispose {
            showBottomNavigation()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isLoggedIn) "Pulse Music Profile & Account" else "Account & Profile Setup",
                        style = typo().titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                },
                navigationIcon = {
                    Box(Modifier.padding(horizontal = 5.dp)) {
                        RippleIconButton(
                            SimpIcons.ArrowBackIosNew,
                            Modifier.size(32.dp),
                            true,
                        ) {
                            coroutineScope.launch {
                                dataStoreManager.setHasSeenLoginPrompt(true)
                            }
                            navController.navigateUp()
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BitChordOledBlack,
                ),
            )
        },
        containerColor = BitChordOledBlack,
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top,
            ) {
                Spacer(modifier = Modifier.height(12.dp))

                if (isLoggedIn && !userEmail.isNullOrBlank()) {
                    // ==========================================
                    // 1. INTEGRATED LOGGED-IN PROFILE HUB
                    // ==========================================
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00E676).copy(alpha = 0.15f))
                            .border(2.dp, BitChordNeonGreen, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (name.isNotBlank()) name.take(1).uppercase() else userEmail?.firstOrNull()?.uppercase() ?: "P",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = BitChordNeonGreen,
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = if (name.isNotBlank()) name else "Pulse Music Listener",
                        style = typo().titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )

                    Text(
                        text = userEmail ?: "",
                        style = typo().bodySmall,
                        color = Color.White.copy(alpha = 0.7f),
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Editable Profile Information Card
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(BitChordCardSurface)
                            .border(1.dp, BitChordNeonGreen.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Text(
                            text = "Profile Information",
                            style = typo().titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = BitChordNeonGreen,
                        )

                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Profile Name", color = BitChordNeonGreen) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BitChordNeonGreen,
                                unfocusedBorderColor = Color(0xFF2E2E36),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = BitChordNeonGreen,
                                focusedContainerColor = BitChordFieldSurface,
                                unfocusedContainerColor = BitChordFieldSurface,
                            ),
                        )

                        OutlinedTextField(
                            value = age,
                            onValueChange = { age = it.filter { char -> char.isDigit() } },
                            label = { Text("Age", color = BitChordNeonGreen) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BitChordNeonGreen,
                                unfocusedBorderColor = Color(0xFF2E2E36),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = BitChordNeonGreen,
                                focusedContainerColor = BitChordFieldSurface,
                                unfocusedContainerColor = BitChordFieldSurface,
                            ),
                        )

                        // Gender Selector Chips
                        Column {
                            Text(
                                text = "Gender",
                                style = typo().labelMedium,
                                color = Color.White.copy(alpha = 0.8f),
                            )
                            Spacer(Modifier.height(8.dp))
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                GENDER_OPTIONS.forEach { opt ->
                                    val isSelected = gender == opt
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(if (isSelected) BitChordNeonGreen else BitChordFieldSurface)
                                            .border(
                                                1.dp,
                                                if (isSelected) BitChordNeonGreen else Color(0xFF2E2E36),
                                                RoundedCornerShape(20.dp),
                                            )
                                            .clickable { gender = opt }
                                            .padding(horizontal = 14.dp, vertical = 8.dp),
                                    ) {
                                        Text(
                                            text = opt,
                                            style = typo().labelMedium,
                                            color = if (isSelected) Color.Black else Color.White,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        )
                                    }
                                }
                            }
                        }

                        // Indian Language Preference Selector Button
                        Column {
                            Text(
                                text = "Language Preference",
                                style = typo().labelMedium,
                                color = Color.White.copy(alpha = 0.8f),
                            )
                            Spacer(Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(BitChordFieldSurface)
                                    .border(1.dp, BitChordNeonGreen.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                    .clickable { showLanguagePicker = true }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    ) {
                                        Icon(
                                            imageVector = SimpIcons.Subtitles,
                                            contentDescription = "Language",
                                            tint = BitChordNeonGreen,
                                            modifier = Modifier.size(20.dp),
                                        )
                                        Text(
                                            text = if (languagePreference.isNotBlank()) languagePreference else "Select Indian Language",
                                            style = typo().bodyMedium,
                                            color = Color.White,
                                            fontWeight = FontWeight.Medium,
                                        )
                                    }
                                    Icon(
                                        imageVector = SimpIcons.KeyboardArrowDown,
                                        contentDescription = "Select",
                                        tint = BitChordNeonGreen,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                        }

                        // Save Profile Button
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    dataStoreManager.setProfile(
                                        name = name.trim(),
                                        age = age.trim(),
                                        gender = gender,
                                        languagePreference = languagePreference,
                                    )
                                    statusMessage = "Profile updated successfully!"
                                    isError = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BitChordNeonGreen,
                                contentColor = Color.Black,
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(46.dp),
                        ) {
                            Text("Save Profile Changes", fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Cloud Sync Active Card
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(BitChordCardSurface)
                            .border(1.dp, Color(0xFF1E2822), RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "Pulse Music Cloud Sync Active",
                            style = typo().labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = BitChordNeonGreen,
                        )
                        Text(
                            text = "• Liked songs synced securely in real time\n• Custom playlists safely backed up\n• Listening history and analytics preserved\n• Author: Manikandan • BitChord Engine",
                            style = typo().bodySmall,
                            color = Color.White.copy(alpha = 0.75f),
                        )
                    }

                    AnimatedVisibility(visible = !statusMessage.isNullOrBlank()) {
                        Text(
                            text = statusMessage.orEmpty(),
                            color = if (isError) MaterialTheme.colorScheme.error else BitChordNeonGreen,
                            style = typo().bodySmall,
                            modifier = Modifier.padding(top = 10.dp),
                            textAlign = TextAlign.Center,
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                supabaseAuthRepository.signOut()
                                statusMessage = "Signed out"
                                isError = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                    ) {
                        Text("Sign Out")
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                } else {
                    // ==========================================
                    // 2. INTEGRATED AUTH & SIGN UP PROFILE FLOW
                    // ==========================================

                    // Segmented Mode Toggle: [ Sign In ] vs [ Sign Up ]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(BitChordCardSurface)
                            .border(1.dp, Color(0xFF26332A), RoundedCornerShape(14.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (!isSignUpMode) BitChordNeonGreen else Color.Transparent)
                                .clickable {
                                    isSignUpMode = false
                                    statusMessage = null
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "Sign In",
                                style = typo().labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (!isSignUpMode) Color.Black else Color.White,
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSignUpMode) BitChordNeonGreen else Color.Transparent)
                                .clickable {
                                    isSignUpMode = true
                                    statusMessage = null
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "Sign Up / Register",
                                style = typo().labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (isSignUpMode) Color.Black else Color.White,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = if (isSignUpMode) "Create Pulse Music Account & Profile" else "Welcome to Pulse Music",
                        style = typo().titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = Color.White,
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (isSignUpMode) {
                            "Fill your name, age, and Indian language preference below to set up your profile and cloud backup simultaneously."
                        } else {
                            "Sign in with your email to access your synchronized library, or continue as guest."
                        },
                        style = typo().bodySmall,
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // PROFILE FIELDS REQUIRED ON SIGN UP
                    if (isSignUpMode) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(BitChordCardSurface)
                                .border(1.dp, BitChordNeonGreen.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                text = "Profile Details",
                                style = typo().labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = BitChordNeonGreen,
                            )

                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = { Text("Full Name *", color = if (name.isNotEmpty()) BitChordNeonGreen else Color.White.copy(alpha = 0.6f)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = BitChordNeonGreen,
                                    unfocusedBorderColor = Color(0xFF2E2E36),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    cursorColor = BitChordNeonGreen,
                                    focusedContainerColor = BitChordFieldSurface,
                                    unfocusedContainerColor = BitChordFieldSurface,
                                ),
                            )

                            OutlinedTextField(
                                value = age,
                                onValueChange = { age = it.filter { char -> char.isDigit() } },
                                label = { Text("Age *", color = if (age.isNotEmpty()) BitChordNeonGreen else Color.White.copy(alpha = 0.6f)) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = BitChordNeonGreen,
                                    unfocusedBorderColor = Color(0xFF2E2E36),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    cursorColor = BitChordNeonGreen,
                                    focusedContainerColor = BitChordFieldSurface,
                                    unfocusedContainerColor = BitChordFieldSurface,
                                ),
                            )

                            // Gender selector chips
                            Column {
                                Text(
                                    text = "Gender",
                                    style = typo().labelMedium,
                                    color = Color.White.copy(alpha = 0.8f),
                                )
                                Spacer(Modifier.height(6.dp))
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    GENDER_OPTIONS.forEach { opt ->
                                        val isSelected = gender == opt
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(20.dp))
                                                .background(if (isSelected) BitChordNeonGreen else BitChordFieldSurface)
                                                .border(
                                                    1.dp,
                                                    if (isSelected) BitChordNeonGreen else Color(0xFF2E2E36),
                                                    RoundedCornerShape(20.dp),
                                                )
                                                .clickable { gender = opt }
                                                .padding(horizontal = 12.dp, vertical = 6.dp),
                                        ) {
                                            Text(
                                                text = opt,
                                                style = typo().labelSmall,
                                                color = if (isSelected) Color.Black else Color.White,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            )
                                        }
                                    }
                                }
                            }

                            // Indian Language Picker Field
                            Column {
                                Text(
                                    text = "Language Preference (Indian Languages)",
                                    style = typo().labelMedium,
                                    color = Color.White.copy(alpha = 0.8f),
                                )
                                Spacer(Modifier.height(6.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(BitChordFieldSurface)
                                        .border(1.dp, BitChordNeonGreen.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                                        .clickable { showLanguagePicker = true }
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        ) {
                                            Icon(
                                                imageVector = SimpIcons.Subtitles,
                                                contentDescription = "Language",
                                                tint = BitChordNeonGreen,
                                                modifier = Modifier.size(18.dp),
                                            )
                                            Text(
                                                text = if (languagePreference.isNotBlank()) languagePreference else "Select Indian Language",
                                                style = typo().bodyMedium,
                                                color = Color.White,
                                                fontWeight = FontWeight.Medium,
                                            )
                                        }
                                        Icon(
                                            imageVector = SimpIcons.KeyboardArrowDown,
                                            contentDescription = "Expand",
                                            tint = BitChordNeonGreen,
                                            modifier = Modifier.size(20.dp),
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    // CREDENTIALS INPUT
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email", color = if (email.isNotEmpty()) BitChordNeonGreen else Color.White.copy(alpha = 0.6f)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BitChordNeonGreen,
                            unfocusedBorderColor = Color(0xFF2E2E36),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = BitChordNeonGreen,
                            focusedContainerColor = BitChordFieldSurface,
                            unfocusedContainerColor = BitChordFieldSurface,
                        ),
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password", color = if (password.isNotEmpty()) BitChordNeonGreen else Color.White.copy(alpha = 0.6f)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BitChordNeonGreen,
                            unfocusedBorderColor = Color(0xFF2E2E36),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = BitChordNeonGreen,
                            focusedContainerColor = BitChordFieldSurface,
                            unfocusedContainerColor = BitChordFieldSurface,
                        ),
                    )

                    AnimatedVisibility(visible = !statusMessage.isNullOrBlank()) {
                        Text(
                            text = statusMessage.orEmpty(),
                            color = if (isError) MaterialTheme.colorScheme.error else BitChordNeonGreen,
                            style = typo().bodySmall,
                            modifier = Modifier.padding(top = 10.dp),
                            textAlign = TextAlign.Center,
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // PRIMARY SUBMIT BUTTON
                    Button(
                        onClick = {
                            if (isSignUpMode) {
                                if (name.isBlank()) {
                                    statusMessage = "Please enter your name"
                                    isError = true
                                    return@Button
                                }
                                if (age.isBlank()) {
                                    statusMessage = "Please enter your age"
                                    isError = true
                                    return@Button
                                }
                            }
                            if (email.isBlank() || password.isBlank()) {
                                statusMessage = "Please enter email and password"
                                isError = true
                                return@Button
                            }

                            coroutineScope.launch {
                                isLoading = true
                                statusMessage = null

                                // If sign up, save profile first
                                if (isSignUpMode) {
                                    dataStoreManager.setProfile(
                                        name = name.trim(),
                                        age = age.trim(),
                                        gender = gender,
                                        languagePreference = languagePreference,
                                    )
                                }

                                val result = if (isSignUpMode) {
                                    supabaseAuthRepository.signUp(email.trim(), password)
                                } else {
                                    supabaseAuthRepository.signIn(email.trim(), password)
                                }

                                isLoading = false
                                result.fold(
                                    onSuccess = {
                                        dataStoreManager.setHasSeenLoginPrompt(true)
                                        statusMessage = if (isSignUpMode) "Profile and account created successfully!" else "Signed in successfully!"
                                        isError = false
                                        navController.navigateUp()
                                    },
                                    onFailure = { err ->
                                        statusMessage = err.message ?: "Authentication failed"
                                        isError = true
                                    }
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BitChordNeonGreen,
                            contentColor = Color.Black,
                        ),
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isLoading,
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.Black,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text(
                                text = if (isSignUpMode) "Sign Up & Save Profile" else "Sign In",
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // GUEST CONTINUE / LOCAL PROFILE OPTION
                    TextButton(
                        onClick = {
                            coroutineScope.launch {
                                if (name.isNotBlank() || age.isNotBlank() || languagePreference.isNotBlank()) {
                                    dataStoreManager.setProfile(
                                        name = name.trim(),
                                        age = age.trim(),
                                        gender = gender,
                                        languagePreference = languagePreference,
                                    )
                                }
                                dataStoreManager.setHasSeenLoginPrompt(true)
                                navController.navigateUp()
                            }
                        }
                    ) {
                        Text(
                            text = "Save & Continue as Guest",
                            color = Color.White.copy(alpha = 0.75f),
                            style = typo().labelLarge,
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    // ==========================================
    // 3. INDIAN LANGUAGES SELECTION DIALOG
    // ==========================================
    if (showLanguagePicker) {
        AlertDialog(
            onDismissRequest = { showLanguagePicker = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Select Indian Language",
                        style = typo().titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    IconButton(
                        onClick = { showLanguagePicker = false },
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            imageVector = SimpIcons.Close,
                            contentDescription = "Close",
                            tint = Color.White.copy(alpha = 0.7f),
                        )
                    }
                }
            },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(INDIAN_LANGUAGES) { lang ->
                        val formattedLang = "${lang.englishName} (${lang.nativeScript})"
                        val isSelected = languagePreference.startsWith(lang.englishName)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) Color(0xFF00E676).copy(alpha = 0.15f) else BitChordFieldSurface)
                                .border(
                                    1.dp,
                                    if (isSelected) BitChordNeonGreen else Color(0xFF26262E),
                                    RoundedCornerShape(12.dp),
                                )
                                .clickable {
                                    languagePreference = formattedLang
                                    showLanguagePicker = false
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(
                                    text = lang.englishName,
                                    style = typo().bodyLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) BitChordNeonGreen else Color.White,
                                )
                                Text(
                                    text = lang.nativeScript,
                                    style = typo().bodySmall,
                                    color = if (isSelected) BitChordNeonGreen.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.6f),
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = SimpIcons.Check,
                                    contentDescription = "Selected",
                                    tint = BitChordNeonGreen,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            containerColor = Color(0xFF0D140F),
            shape = RoundedCornerShape(20.dp),
        )
    }
}
