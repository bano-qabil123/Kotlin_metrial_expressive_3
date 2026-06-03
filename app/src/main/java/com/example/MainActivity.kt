package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.foundation.border
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import com.example.data.local.ActivityEntity
import com.example.data.local.PostEntity
import com.example.data.local.ProfileEntity
import com.example.data.model.Comment
import com.example.data.model.JsonParser
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.SocialViewModel
import kotlinx.coroutines.launch

import androidx.compose.foundation.text.ClickableText
import androidx.compose.ui.zIndex
import androidx.compose.foundation.rememberScrollState

class MainActivity : ComponentActivity() {
    private val viewModel: SocialViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                SafeMainContent(viewModel = viewModel)
            }
        }
    }
}

// --- GLOBAL UNBREAKABLE INTERFACE RESILIERY & WELCOME STATE MACHINE ---
@Composable
fun SafeMainContent(viewModel: SocialViewModel) {
    var fatalError by remember { mutableStateOf<Throwable?>(null) }
    
    if (fatalError != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Rounded.Warning,
                        contentDescription = "Error Logo",
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Pulse Safe Mode",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "A critical interface exception was handled gracefully: ${fatalError?.localizedMessage}. Tap below to reset.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            fatalError = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onErrorContainer)
                    ) {
                        Text("Restart App", color = MaterialTheme.colorScheme.errorContainer)
                    }
                }
            }
        }
    } else {
        var currentAppState by remember { mutableStateOf("splash") } // "splash", "auth", "loaded"
        val isLoggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()

        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(800)
            if (isLoggedIn) {
                currentAppState = "loaded"
            } else {
                currentAppState = "auth"
            }
        }

        // Sync appState if login state toggles later
        LaunchedEffect(isLoggedIn) {
            if (currentAppState != "splash") {
                currentAppState = if (isLoggedIn) "loaded" else "auth"
            }
        }

        when (currentAppState) {
            "splash" -> {
                var startAnimation by remember { mutableStateOf(false) }
                val scale by animateFloatAsState(
                    targetValue = if (startAnimation) 1.0f else 0.6f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "logo_scale"
                )
                val alpha by animateFloatAsState(
                    targetValue = if (startAnimation) 1.0f else 0.0f,
                    animationSpec = tween(900),
                    label = "logo_alpha"
                )

                // Indefinite pulse animation for the glowing ring
                val infiniteTransition = rememberInfiniteTransition(label = "pulse_ring")
                val pulseScale by infiniteTransition.animateFloat(
                    initialValue = 1.0f,
                    targetValue = 1.8f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1700, easing = LinearOutSlowInEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "pulse_scale"
                )
                val pulseAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.6f,
                    targetValue = 0.0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1700, easing = LinearOutSlowInEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "pulse_alpha"
                )

                LaunchedEffect(Unit) {
                    startAnimation = true
                }

                val isDark = isSystemInDarkTheme()
                val splashBg = if (isDark) {
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF231A3E), Color(0xFF100E17)),
                        radius = 1200f
                    )
                } else {
                    Brush.radialGradient(
                        colors = listOf(Color(0xFFF0E7FF), Color(0xFFFEF7FF)),
                        radius = 1200f
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(splashBg),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            // Pulsating glowing circle in background
                            Box(
                                modifier = Modifier
                                    .size(140.dp)
                                    .graphicsLayer {
                                        scaleX = pulseScale
                                        scaleY = pulseScale
                                        this.alpha = pulseAlpha
                                    }
                                    .background(
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                        shape = CircleShape
                                    )
                            )
                            
                            // App logo with soft spring scale
                            Image(
                                painter = painterResource(id = R.drawable.ic_pulse_app_logo_1780347729282),
                                contentDescription = "Pulse Official Logo",
                                modifier = Modifier
                                    .size(130.dp)
                                    .graphicsLayer {
                                        scaleX = scale
                                        scaleY = scale
                                        this.alpha = alpha
                                    }
                                    .clip(RoundedCornerShape(32.dp))
                                    .border(
                                        width = 2.dp,
                                        brush = Brush.linearGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.primary,
                                                MaterialTheme.colorScheme.secondary
                                            )
                                        ),
                                        shape = RoundedCornerShape(32.dp)
                                    ),
                                contentScale = ContentScale.Crop
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        // Animated Name
                        AnimatedVisibility(
                            visible = startAnimation,
                            enter = fadeIn(animationSpec = tween(800, delayMillis = 200)) + 
                                    expandVertically(animationSpec = tween(800, delayMillis = 200))
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "PULSE",
                                    style = MaterialTheme.typography.displaySmall.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 6.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "A space for genuine passions.",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                    ),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(48.dp))
                        
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.secondary,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
            "auth" -> {
                AuthScreen(viewModel = viewModel)
            }
            else -> {
                MainApp(viewModel = viewModel)
            }
        }
    }
}

// --- FLOATING AMBIENT GLOW BACKDROP FOR PREMIUM SCREENS ---
@Composable
fun AmbientGlowBackground(content: @Composable () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "ambient_glow")
    val animOffset1 by infiniteTransition.animateFloat(
        initialValue = -50f,
        targetValue = 120f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offset1"
    )
    val animOffset2 by infiniteTransition.animateFloat(
        initialValue = 120f,
        targetValue = -50f,
        animationSpec = infiniteRepeatable(
            animation = tween(14000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offset2"
    )

    val isDark = isSystemInDarkTheme()
    val bgColors = if (isDark) {
        listOf(Color(0xFF0F0E13), Color(0xFF141218))
    } else {
        listOf(Color(0xFFFCF9FF), Color(0xFFF3EDF7))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(bgColors))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            if (width > 0f && height > 0f) {
                if (isDark) {
                    val r1 = maxOf(1f, width * 0.6f)
                    val r2 = maxOf(1f, width * 0.7f)
                    val r3 = maxOf(1f, width * 0.5f)

                    // Indigo/Purple Glow
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFFFFB74D).copy(alpha = 0.08f), Color.Transparent),
                            center = Offset(width * 0.1f + animOffset1, height * 0.2f),
                            radius = r1
                        ),
                        center = Offset(width * 0.1f + animOffset1, height * 0.2f),
                        radius = r1
                    )
                    // Violet Glow
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFFD0BCFF).copy(alpha = 0.18f), Color.Transparent),
                            center = Offset(width * 0.8f + animOffset2, height * 0.4f),
                            radius = r2
                        ),
                        center = Offset(width * 0.8f + animOffset2, height * 0.4f),
                        radius = r2
                    )
                    // Pinkish Core Glow
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFFF48FB1).copy(alpha = 0.12f), Color.Transparent),
                            center = Offset(width * 0.5f, height * 0.75f - animOffset1),
                            radius = r3
                        ),
                        center = Offset(width * 0.5f, height * 0.75f - animOffset1),
                        radius = r3
                    )
                } else {
                    val r1 = maxOf(1f, width * 0.5f)
                    val r2 = maxOf(1f, width * 0.6f)

                    // Light Violet Glow
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFFEADDFF).copy(alpha = 0.35f), Color.Transparent),
                            center = Offset(width * 0.2f + animOffset1, height * 0.25f),
                            radius = r1
                        ),
                        center = Offset(width * 0.2f + animOffset1, height * 0.25f),
                        radius = r1
                    )
                    // Light Pink Glow
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFFFFD8E4).copy(alpha = 0.3f), Color.Transparent),
                            center = Offset(width * 0.8f - animOffset2, height * 0.65f),
                            radius = r2
                        ),
                        center = Offset(width * 0.8f - animOffset2, height * 0.65f),
                        radius = r2
                    )
                }
            }
        }
        content()
    }
}

@Composable
fun AuthScreen(viewModel: SocialViewModel) {
    var isSignUpMode by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var avatarUrl by remember { mutableStateOf("") }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    
    val context = LocalContext.current

    AmbientGlowBackground {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .systemBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 450.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                        shape = RoundedCornerShape(32.dp)
                    )
                    .border(
                        width = 1.5.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
                            )
                        ),
                        shape = RoundedCornerShape(32.dp)
                    )
                    .padding(horizontal = 24.dp, vertical = 32.dp)
                    .verticalScroll(androidx.compose.foundation.rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Pulsing Logo Ring
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)
                                )
                            ),
                            shape = CircleShape
                        )
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_pulse_app_logo_1780347729282),
                        contentDescription = "Pulse Official Logo",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = if (isSignUpMode) "Join Pulse" else "Welcome Back",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isSignUpMode) "Share your genuine passions brain-rot free" else "Sign in to connect, free of AI slop",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(28.dp))

                if (isSignUpMode) {
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Username") },
                        placeholder = { Text("e.g. creative_genius") },
                        leadingIcon = { Icon(Icons.Rounded.AlternateEmail, contentDescription = null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)) },
                        modifier = Modifier.fillMaxWidth().testTag("auth_username_input"),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        ),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("Full Name") },
                        placeholder = { Text("Jordan Sparks") },
                        leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)) },
                        modifier = Modifier.fillMaxWidth().testTag("auth_fullname_input"),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        ),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = bio,
                        onValueChange = { bio = it },
                        label = { Text("Short Bio") },
                        placeholder = { Text("Art, game dev, coffee, etc.") },
                        leadingIcon = { Icon(Icons.Rounded.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)) },
                        modifier = Modifier.fillMaxWidth().testTag("auth_bio_input"),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = avatarUrl,
                        onValueChange = { avatarUrl = it },
                        label = { Text("Avatar URL (Optional)") },
                        placeholder = { Text("https://example.com/me.png") },
                        leadingIcon = { Icon(Icons.Rounded.Link, contentDescription = null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)) },
                        modifier = Modifier.fillMaxWidth().testTag("auth_avatar_input"),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        ),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    placeholder = { Text("jordan@example.com") },
                    leadingIcon = { Icon(Icons.Rounded.Email, contentDescription = null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)) },
                    modifier = Modifier.fillMaxWidth().testTag("auth_email_input"),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    ),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    placeholder = { Text("Minimum 4 characters") },
                    leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)) },
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth().testTag("auth_password_input"),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    ),
                    singleLine = true
                )
                
                if (!isSignUpMode) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = { showForgotPasswordDialog = true },
                            modifier = Modifier.testTag("forgot_password_button")
                        ) {
                            Text(
                                "Forgot Password?",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(24.dp))
                }

                Button(
                    onClick = {
                        if (isSignUpMode) {
                            if (username.isBlank() || email.isBlank() || fullName.isBlank()) {
                                Toast.makeText(context, "Please fill in all details", Toast.LENGTH_SHORT).show()
                            } else if (!email.contains("@")) {
                                Toast.makeText(context, "Please enter a valid email", Toast.LENGTH_SHORT).show()
                            } else if (password.length < 4) {
                                Toast.makeText(context, "Password must be at least 4 characters", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.signUp(
                                    username = username.trim(),
                                    email = email.trim(),
                                    fullName = fullName.trim(),
                                    bio = bio,
                                    avatarUrl = avatarUrl.trim()
                                )
                                Toast.makeText(context, "Account Created Successfully!", Toast.LENGTH_LONG).show()
                            }
                        } else {
                            if (email.isBlank() || password.isBlank()) {
                                Toast.makeText(context, "Please enter email and password", Toast.LENGTH_SHORT).show()
                            } else if (viewModel.logIn(email.trim(), password)) {
                                Toast.makeText(context, "Welcome back to Pulse!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Login failed. Password must be >= 4 chars, email must contain @", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("auth_submit_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.secondary
                                    )
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isSignUpMode) "Create Account" else "Sign In",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                TextButton(
                    onClick = { isSignUpMode = !isSignUpMode },
                    modifier = Modifier.testTag("toggle_auth_mode")
                ) {
                    Text(
                        text = if (isSignUpMode) "Already have an account? Sign In" else "New to Pulse? Create an Account",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        }
    }

    if (showForgotPasswordDialog) {
        var forgotEmail by remember { mutableStateOf("") }
        Dialog(onDismissRequest = { showForgotPasswordDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Rounded.LockReset,
                        contentDescription = "Forgot Password",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Reset Password",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Enter your email and we'll securely trigger a Supabase-encrypted password recovery flow.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = forgotEmail,
                        onValueChange = { forgotEmail = it },
                        placeholder = { Text("Email Address") },
                        modifier = Modifier.fillMaxWidth().testTag("forgot_email_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showForgotPasswordDialog = false }) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (forgotEmail.contains("@")) {
                                    Toast.makeText(context, "A secure recovery link has been dispatched to $forgotEmail", Toast.LENGTH_LONG).show()
                                    showForgotPasswordDialog = false
                                } else {
                                    Toast.makeText(context, "Please enter a valid email", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Reset")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CreatorProfileDialog(
    profile: ProfileEntity,
    onDismiss: () -> Unit,
    viewModel: SocialViewModel
) {
    val allProfiles by viewModel.allProfiles.collectAsStateWithLifecycle()
    val currentProfile = remember(allProfiles, profile) {
        allProfiles.find { it.id == profile.id } ?: profile
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(28.dp))
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Cover Banner + Overlapping Avatar Block
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(175.dp)
                ) {
                    // Cover Banner Block with close button overlay
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .align(Alignment.TopCenter)
                    ) {
                        AsyncImage(
                            model = if (currentProfile.bannerUrl.isNotBlank()) currentProfile.bannerUrl else "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=800",
                            contentDescription = "Creator Banner",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                                        )
                                    )
                                )
                        )
                        IconButton(
                            onClick = onDismiss,
                            colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                                .size(36.dp)
                        ) {
                            Icon(Icons.Rounded.Close, contentDescription = "Close details", modifier = Modifier.size(18.dp))
                        }
                    }

                    // Overlapping avatar aligned at the bottom center of the parent 175dp Box (making half of it sit over the 130dp banner)
                    AsyncImage(
                        model = currentProfile.avatarUrl.ifEmpty { "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=100" },
                        contentDescription = "Creator Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(4.dp)
                            .clip(CircleShape)
                            .align(Alignment.BottomCenter)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        currentProfile.fullName,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                        textAlign = TextAlign.Center
                    )
                    if (currentProfile.isVerified) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Rounded.Verified,
                            contentDescription = "Verified Creator",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Text(
                    "@${currentProfile.username}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    currentProfile.bio.ifEmpty { "No passion description yet." },
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text("${currentProfile.followersCount}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black))
                        Text("Followers", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text("${currentProfile.followingCount}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black))
                        Text("Following", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        viewModel.toggleFollow(currentProfile.id)
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (currentProfile.isFollowedByMe) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary,
                        contentColor = if (currentProfile.isFollowedByMe) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(
                        imageVector = if (currentProfile.isFollowedByMe) Icons.Rounded.Check else Icons.Rounded.Add,
                        contentDescription = "Follow State",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (currentProfile.isFollowedByMe) "Following" else "Follow Passions",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

@Composable
fun FollowListDialog(
    type: String, // "Followers" or "Following"
    onDismiss: () -> Unit,
    viewModel: SocialViewModel,
    onCreatorClick: (ProfileEntity) -> Unit
) {
    val allProfiles by viewModel.allProfiles.collectAsStateWithLifecycle()
    val profilesToRender = remember(allProfiles, type) {
        if (type == "Following") {
            allProfiles.filter { it.isFollowedByMe }
        } else {
            allProfiles.filter { !it.isMe }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$type List",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close dialogue")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (profilesToRender.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Nobody listed in this category yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(profilesToRender, key = { it.id }) { creator ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { 
                                        onDismiss()
                                        onCreatorClick(creator)
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = creator.avatarUrl,
                                    contentDescription = "User Avatar",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            creator.fullName,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                        if (creator.isVerified) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                Icons.Rounded.Verified,
                                                contentDescription = "Verified Creator",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        "@${creator.username}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                }
                                
                                Button(
                                    onClick = { viewModel.toggleFollow(creator.id) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (creator.isFollowedByMe) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary,
                                        contentColor = if (creator.isFollowedByMe) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimary
                                    ),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text(
                                        if (creator.isFollowedByMe) "Following" else "Follow",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PassionRichText(
    text: String,
    onHashtagClick: (String) -> Unit,
    onMentionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val annotatedString = buildAnnotatedString {
        val words = text.split(Regex("(?=\\s)|(?<=\\s)"))
        words.forEach { word ->
            if (word.startsWith("#") && word.length > 1) {
                pushStringAnnotation(tag = "HASHTAG", annotation = word)
                withStyle(style = SpanStyle(color = Color(0xFF00B0FF), fontWeight = FontWeight.Bold)) {
                    append(word)
                }
                pop()
            } else if (word.startsWith("@") && word.length > 1) {
                val cleanWord = word.trim().replace(Regex("[^a-zA-Z0-9_@]"), "")
                pushStringAnnotation(tag = "MENTION", annotation = cleanWord)
                withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)) {
                    append(word)
                }
                pop()
            } else {
                append(word)
            }
        }
    }

    ClickableText(
        text = annotatedString,
        style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
        modifier = modifier,
        onClick = { offset ->
            annotatedString.getStringAnnotations(tag = "HASHTAG", start = offset, end = offset)
                .firstOrNull()?.let { annotation ->
                    onHashtagClick(annotation.item)
                }
            annotatedString.getStringAnnotations(tag = "MENTION", start = offset, end = offset)
                .firstOrNull()?.let { annotation ->
                    onMentionClick(annotation.item)
                }
        }
    )
}

// --- Dynamic Navigation Destinations ---
enum class Screen(val title: String) {
    EXPLORE("Explore"),
    SEARCH("Search"),
    ACTIVITY("Activity"),
    PROFILE("Profile"),
    CREATE_POST("Create Post"),
    EDIT_PROFILE("Update Profile"),
    POST_DETAIL("Post Detail")
}

@Composable
fun InAppPushNotificationBanner(
    notification: ActivityEntity,
    onDismiss: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 480.dp)
            .padding(12.dp)
            .clickable(onClick = onClick)
            .testTag("in_app_push_banner"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.inverseOnSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.BottomEnd, modifier = Modifier.size(42.dp)) {
                if (!notification.actorAvatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = notification.actorAvatarUrl,
                        contentDescription = "Notification profile",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                } else {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Rounded.Person, 
                                contentDescription = "Actor", 
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
                
                // Nest status badge matches action type
                val badgeIcon = when (notification.type) {
                    "like" -> Icons.Rounded.Favorite
                    "comment" -> Icons.Rounded.ChatBubble
                    "follow" -> Icons.Rounded.PersonAdd
                    else -> Icons.Rounded.Notifications
                }
                val badgeBg = when (notification.type) {
                    "like" -> Color(0xFFE91E63)
                    "comment" -> Color(0xFF00B074)
                    "follow" -> Color(0xFF2196F3)
                    else -> MaterialTheme.colorScheme.primary
                }
                Surface(
                    shape = CircleShape,
                    color = badgeBg,
                    modifier = Modifier
                        .size(16.dp)
                        .offset(x = 2.dp, y = 2.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            badgeIcon, 
                            contentDescription = null, 
                            tint = Color.White, 
                            modifier = Modifier.size(9.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "@${notification.actorUsername}",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.inverseSurface
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "• Now",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.5f)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = notification.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.85f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            IconButton(
                onClick = onDismiss, 
                modifier = Modifier
                    .size(28.dp)
                    .background(MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.08f), CircleShape)
            ) {
                Icon(
                    Icons.Rounded.Close, 
                    contentDescription = "Close push banner", 
                    tint = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.6f), 
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(viewModel: SocialViewModel) {
    val posts by viewModel.posts.collectAsStateWithLifecycle()
    val activities by viewModel.activities.collectAsStateWithLifecycle()
    val myProfile by viewModel.myProfile.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val supabaseConfigured by viewModel.supabaseConfigured.collectAsStateWithLifecycle()
    val allProfiles by viewModel.allProfiles.collectAsStateWithLifecycle()

    var activeScreen by remember { mutableStateOf(Screen.EXPLORE) }
    var selectedPostId by remember { mutableStateOf<String?>(null) }
    var composeImagePrefill by remember { mutableStateOf<String?>(null) }
    var activeUserDetailProfile by remember { mutableStateOf<ProfileEntity?>(null) }
    
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Request native runtime permission for system notifications on modern Android 13+ devices
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = { isGranted -> }
        )
        LaunchedEffect(Unit) {
            launcher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Detect width to show responsive Layout: bottom bar for Mobile, navigation rail for Desktop/Web
    val configuration = LocalConfiguration.current
    val isWideScreen = configuration.screenWidthDp >= 600

    // Connect feedback
    LaunchedEffect(Unit) {
        viewModel.syncSuccess.collect { success ->
            val message = if (success) "Synced successfully with Supabase Cloud!" else "Sync error or database not configured."
            snackbarHostState.showSnackbar(message)
        }
    }

    val currentPushNotification by viewModel.currentPushNotification.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            floatingActionButton = {
                if (activeScreen == Screen.EXPLORE || activeScreen == Screen.SEARCH) {
                    ExtendedFloatingActionButton(
                        onClick = { activeScreen = Screen.CREATE_POST },
                        shape = RoundedCornerShape(28.dp),
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                        modifier = Modifier.testTag("create_post_fab")
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Publish Post")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Express", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (!isWideScreen && activeScreen in listOf(Screen.EXPLORE, Screen.SEARCH, Screen.ACTIVITY, Screen.PROFILE)) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp
                    ) {
                        NavigationBarItem(
                            selected = activeScreen == Screen.EXPLORE,
                            onClick = { activeScreen = Screen.EXPLORE },
                            icon = { Icon(if (activeScreen == Screen.EXPLORE) Icons.Filled.Home else Icons.Outlined.Home, contentDescription = "Feed") },
                            label = { Text("Feed") },
                            modifier = Modifier.testTag("nav_explore")
                        )
                        NavigationBarItem(
                            selected = activeScreen == Screen.SEARCH,
                            onClick = { activeScreen = Screen.SEARCH },
                            icon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
                            label = { Text("Search") },
                            modifier = Modifier.testTag("nav_search")
                        )
                        NavigationBarItem(
                            selected = activeScreen == Screen.ACTIVITY,
                            onClick = { activeScreen = Screen.ACTIVITY },
                            icon = {
                                BadgedBox(badge = {
                                    val unread = activities.size
                                    if (unread > 0) {
                                        Badge { Text("$unread") }
                                    }
                                }) {
                                    Icon(if (activeScreen == Screen.ACTIVITY) Icons.Filled.Notifications else Icons.Outlined.Notifications, contentDescription = "Notifications")
                                }
                            },
                            label = { Text("Activity") },
                            modifier = Modifier.testTag("nav_activity")
                        )
                        NavigationBarItem(
                            selected = activeScreen == Screen.PROFILE,
                            onClick = { activeScreen = Screen.PROFILE },
                            icon = { Icon(if (activeScreen == Screen.PROFILE) Icons.Filled.Person else Icons.Outlined.Person, contentDescription = "Profile") },
                            label = { Text("Profile") },
                            modifier = Modifier.testTag("nav_profile")
                        )
                    }
                }
            }
        ) { innerPadding ->
        val isDark = isSystemInDarkTheme()
        val bgColor = MaterialTheme.colorScheme.background
        val appScreenBg = remember(isDark, bgColor) {
            androidx.compose.ui.graphics.Brush.verticalGradient(
                colors = if (isDark) {
                    listOf(androidx.compose.ui.graphics.Color(0xFF160F25), bgColor)
                } else {
                    listOf(androidx.compose.ui.graphics.Color(0xFFFAF2FF), bgColor)
                },
                startY = 0f,
                endY = 1200f
            )
        }
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(appScreenBg)
        ) {
            // Tablet & Web Left Navigation Rail
            if (isWideScreen && activeScreen in listOf(Screen.EXPLORE, Screen.SEARCH, Screen.ACTIVITY, Screen.PROFILE)) {
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surface,
                    header = {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(vertical = 16.dp)
                        ) {
                            Text(
                                "Pulse",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.W900,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                            if (isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            } else if (supabaseConfigured) {
                                Icon(
                                    Icons.Rounded.CloudDone,
                                    contentDescription = "Database Connected",
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxHeight(),
                    windowInsets = WindowInsets.safeDrawing
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    NavigationRailItem(
                        selected = activeScreen == Screen.EXPLORE,
                        onClick = { activeScreen = Screen.EXPLORE },
                        icon = { Icon(if (activeScreen == Screen.EXPLORE) Icons.Filled.Home else Icons.Outlined.Home, contentDescription = "Feed") },
                        label = { Text("Feed") }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    NavigationRailItem(
                        selected = activeScreen == Screen.SEARCH,
                        onClick = { activeScreen = Screen.SEARCH },
                        icon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
                        label = { Text("Search") }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    NavigationRailItem(
                        selected = activeScreen == Screen.ACTIVITY,
                        onClick = { activeScreen = Screen.ACTIVITY },
                        icon = {
                            BadgedBox(badge = {
                                val unread = activities.size
                                if (unread > 0) {
                                    Badge { Text("$unread") }
                                }
                            }) {
                                Icon(if (activeScreen == Screen.ACTIVITY) Icons.Filled.Notifications else Icons.Outlined.Notifications, contentDescription = "Notifications")
                            }
                        },
                        label = { Text("Activity") }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    NavigationRailItem(
                        selected = activeScreen == Screen.PROFILE,
                        onClick = { activeScreen = Screen.PROFILE },
                        icon = { Icon(if (activeScreen == Screen.PROFILE) Icons.Filled.Person else Icons.Outlined.Person, contentDescription = "Profile") },
                        label = { Text("Profile") }
                    )
                    Spacer(modifier = Modifier.weight(1f))
                }
            }

            // Main Content Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                AnimatedContent(
                    targetState = activeScreen,
                    transitionSpec = {
                        slideInVertically(animationSpec = spring()) { it } + fadeIn() togetherWith
                        slideOutVertically(animationSpec = spring()) { -it } + fadeOut()
                    },
                    label = "screen_transition"
                ) { targetScreen ->
                    when (targetScreen) {
                        Screen.EXPLORE -> {
                            ExploreScreen(
                                posts = posts,
                                allProfiles = allProfiles,
                                isSyncing = isSyncing,
                                supabaseConfigured = supabaseConfigured,
                                onLike = { viewModel.toggleLike(it) },
                                onAddComment = { pid, txt -> viewModel.addComment(pid, txt) },
                                onDeletePost = { viewModel.deletePost(it) },
                                myProfile = myProfile,
                                onTriggerSync = { viewModel.syncWithSupabase() },
                                onCreatorClick = { activeUserDetailProfile = it },
                                onHashtagClick = { tag ->
                                    viewModel.updateSearchQuery(tag)
                                    activeScreen = Screen.SEARCH
                                },
                                onMentionClick = { mention ->
                                    val cleanHandle = mention.replace("@", "").trim()
                                    val foundProfile = allProfiles.find { it.username.equals(cleanHandle, ignoreCase = true) }
                                    if (foundProfile != null) {
                                        activeUserDetailProfile = foundProfile
                                    } else {
                                        Toast.makeText(context, "No user @$cleanHandle exists on Pulse.", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onPostClick = { postId ->
                                    selectedPostId = postId
                                    activeScreen = Screen.POST_DETAIL
                                },
                                onDeleteComment = { pid, cid -> viewModel.deleteComment(pid, cid) }
                            )
                        }
                        Screen.SEARCH -> {
                            SearchScreen(
                                searchQuery = searchQuery,
                                onQueryChange = { viewModel.updateSearchQuery(it) },
                                posts = posts,
                                allProfiles = allProfiles,
                                onLike = { viewModel.toggleLike(it) },
                                onAddComment = { pid, txt -> viewModel.addComment(pid, txt) },
                                onDeletePost = { viewModel.deletePost(it) },
                                myProfile = myProfile,
                                onCreatorClick = { activeUserDetailProfile = it },
                                onHashtagClick = { tag ->
                                    viewModel.updateSearchQuery(tag)
                                    activeScreen = Screen.SEARCH
                                },
                                onMentionClick = { mention ->
                                    val cleanHandle = mention.replace("@", "").trim()
                                    val foundProfile = allProfiles.find { it.username.equals(cleanHandle, ignoreCase = true) }
                                    if (foundProfile != null) {
                                        activeUserDetailProfile = foundProfile
                                    } else {
                                        Toast.makeText(context, "No user @$cleanHandle exists on Pulse.", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onPostClick = { postId ->
                                    selectedPostId = postId
                                    activeScreen = Screen.POST_DETAIL
                                },
                                onDeleteComment = { pid, cid -> viewModel.deleteComment(pid, cid) }
                            )
                        }
                        Screen.ACTIVITY -> {
                            ActivityScreen(
                                activities = activities,
                                onMarkAllRead = { viewModel.markAllNotificationsRead() },
                                onUseImage = { imageUrl ->
                                    composeImagePrefill = imageUrl
                                    activeScreen = Screen.CREATE_POST
                                    scope.launch {
                                        snackbarHostState.showSnackbar("📷 Loaded attachment into Express composer!")
                                    }
                                },
                                onTriggerSim = { viewModel.triggerRandomSimNotification() }
                            )
                        }
                        Screen.PROFILE -> {
                            ProfileScreen(
                                myProfile = myProfile,
                                posts = posts,
                                onUpdateProfile = { name, handle, bio, url, bannerUrl ->
                                    viewModel.updateMyProfile(handle, name, bio, url, bannerUrl)
                                },
                                viewModel = viewModel,
                                onCreatorClick = { activeUserDetailProfile = it },
                                onHashtagClick = { tag ->
                                    viewModel.updateSearchQuery(tag)
                                    activeScreen = Screen.SEARCH
                                },
                                onMentionClick = { mention ->
                                    val cleanHandle = mention.replace("@", "").trim()
                                    val foundProfile = allProfiles.find { it.username.equals(cleanHandle, ignoreCase = true) }
                                    if (foundProfile != null) {
                                        activeUserDetailProfile = foundProfile
                                    } else {
                                        Toast.makeText(context, "No user @$cleanHandle exists on Pulse.", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onEditProfileClick = {
                                    activeScreen = Screen.EDIT_PROFILE
                                },
                                onPostClick = { postId ->
                                    selectedPostId = postId
                                    activeScreen = Screen.POST_DETAIL
                                },
                                onDeleteComment = { pid, cid -> viewModel.deleteComment(pid, cid) }
                            )
                        }
                        Screen.CREATE_POST -> {
                            CreatePostScreen(
                                prefilledImageUrl = composeImagePrefill,
                                onDismiss = {
                                    activeScreen = Screen.EXPLORE
                                    composeImagePrefill = null
                                },
                                onPublish = { text, image ->
                                    viewModel.createPost(text, image)
                                    activeScreen = Screen.EXPLORE
                                    composeImagePrefill = null
                                    scope.launch {
                                        snackbarHostState.showSnackbar("🚀 Passion thread successfully published!")
                                    }
                                }
                            )
                        }
                        Screen.EDIT_PROFILE -> {
                            if (myProfile != null) {
                                EditProfileScreen(
                                    profile = myProfile!!,
                                    onDismiss = { activeScreen = Screen.PROFILE },
                                    onSave = { name, handle, bio, avatar, banner ->
                                        viewModel.updateMyProfile(handle, name, bio, avatar, banner)
                                        activeScreen = Screen.PROFILE
                                        scope.launch {
                                            snackbarHostState.showSnackbar("✨ Personal style and profile sync card saved!")
                                        }
                                    }
                                )
                            }
                        }
                        Screen.POST_DETAIL -> {
                            val detailPost = posts.find { it.id == selectedPostId }
                            if (detailPost != null) {
                                PostDetailScreen(
                                    post = detailPost,
                                    allProfiles = allProfiles,
                                    onBack = { activeScreen = Screen.EXPLORE },
                                    onLike = { viewModel.toggleLike(detailPost.id) },
                                    onAddComment = { viewModel.addComment(detailPost.id, it) },
                                    onDeleteComment = { viewModel.deleteComment(detailPost.id, it) },
                                    myProfile = myProfile
                                )
                            } else {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Selected passion thread was deleted or is not cached.", style = MaterialTheme.typography.bodyLarge)
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Button(onClick = { activeScreen = Screen.EXPLORE }) {
                                            Text("Return to Stream")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }



        activeUserDetailProfile?.let { profile ->
            CreatorProfileDialog(
                profile = profile,
                onDismiss = { activeUserDetailProfile = null },
                viewModel = viewModel
            )
        }

        // In-App dynamic sliding overlay heads-up push banner
        currentPushNotification?.let { notification ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .zIndex(10000f)
            ) {
                InAppPushNotificationBanner(
                    notification = notification,
                    onDismiss = { viewModel.dismissPushNotification() },
                    onClick = {
                        activeScreen = Screen.ACTIVITY
                        viewModel.dismissPushNotification()
                    }
                )
            }
        }
    }
}

// ==========================================
// 1. EXPLORE SCREEN & CHILE HERO
// ==========================================
@Composable
fun ExploreScreen(
    posts: List<PostEntity>,
    allProfiles: List<ProfileEntity>,
    isSyncing: Boolean,
    supabaseConfigured: Boolean,
    onLike: (String) -> Unit,
    onAddComment: (String, String) -> Unit,
    onDeletePost: (String) -> Unit,
    myProfile: ProfileEntity?,
    onTriggerSync: () -> Unit,
    onCreatorClick: (ProfileEntity) -> Unit,
    onHashtagClick: (String) -> Unit,
    onMentionClick: (String) -> Unit,
    onPostClick: (String) -> Unit,
    onDeleteComment: ((String, String) -> Unit)? = null
) {
    var selectedFeedTab by remember { mutableStateOf(0) }
    var isRefreshingSimulated by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val filteredPosts = remember(posts, selectedFeedTab, allProfiles) {
        val baseList = if (selectedFeedTab == 1) {
            posts.filter { post ->
                val creator = allProfiles.find { it.id == post.userId || it.username == post.userUsername }
                creator?.isFollowedByMe == true
            }
        } else {
            posts
        }
        baseList.distinctBy { it.id }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("explore_feed_list"),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        // App header with beautiful logo & sync indicator
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.img_official_logo_1780348631601),
                        contentDescription = "Pulse Official Logo",
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(14.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            "Pulse",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-1.0).sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                        Text(
                            "Expressive Social Feed",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Simulated Refresh button
                    IconButton(
                        onClick = {
                            if (!isRefreshingSimulated) {
                                isRefreshingSimulated = true
                                scope.launch {
                                    onTriggerSync()
                                    kotlinx.coroutines.delay(1200)
                                    isRefreshingSimulated = false
                                    Toast.makeText(context, "Passions Feed updated!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.testTag("feed_refresh_button")
                    ) {
                        Icon(
                            Icons.Rounded.Refresh,
                            contentDescription = "Simulated Refresh",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        // Pull to refresh animated spinner indicator
        if (isRefreshingSimulated) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "Fetching fresh passions...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Chile Trending Hero banner
        item {
            ChileHeroSection()
        }

        // All vs Following Feed filter tabs
        item {
            TabRow(
                selectedTabIndex = selectedFeedTab,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                Tab(
                    selected = selectedFeedTab == 0,
                    onClick = { selectedFeedTab = 0 },
                    modifier = Modifier.testTag("feed_tab_all")
                ) {
                    Box(modifier = Modifier.padding(vertical = 12.dp)) {
                        Text("All Passions", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }
                }
                Tab(
                    selected = selectedFeedTab == 1,
                    onClick = { selectedFeedTab = 1 },
                    modifier = Modifier.testTag("feed_tab_following")
                ) {
                    Box(modifier = Modifier.padding(vertical = 12.dp)) {
                        Text("Following Only", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }

        // Section divider label
        item {
            Text(
                text = if (selectedFeedTab == 1) "Your Following Expressions" else "Recent Expressions",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.W900),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
            )
        }

        if (filteredPosts.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .testTag("empty_feed_card"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.CloudOff,
                                contentDescription = "Empty feed",
                                modifier = Modifier.size(36.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        Text(
                            if (selectedFeedTab == 1) "Chamber is silent..." else "A blank canvas awaits!",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            if (selectedFeedTab == 1) "Creators you follow haven't voiced their expressions yet. Try exploring the public stream!" else "Be the first to spark a trending conversation. Share what makes you passionate today.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    isRefreshingSimulated = true
                                    scope.launch {
                                        onTriggerSync()
                                        kotlinx.coroutines.delay(1200)
                                        isRefreshingSimulated = false
                                    }
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sync Feed")
                            }
                            
                            Button(
                                onClick = {
                                    // opens Compose modal Dialog
                                    // we can trigger the floating menu flow
                                    Toast.makeText(context, "Click the '+' Floating Icon below to voice your thoughts!", Toast.LENGTH_LONG).show()
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Start Thread")
                            }
                        }
                    }
                }
            }
        } else {
            items(filteredPosts, key = { it.id }) { post ->
                PostCard(
                    post = post,
                    allProfiles = allProfiles,
                    onLike = { onLike(post.id) },
                    onAddComment = { txt -> onAddComment(post.id, txt) },
                    onDelete = { onDeletePost(post.id) },
                    myProfile = myProfile,
                    onCreatorClick = onCreatorClick,
                    onHashtagClick = onHashtagClick,
                    onMentionClick = onMentionClick,
                    onPostClick = { onPostClick(post.id) },
                    onDeleteComment = { commentId -> onDeleteComment?.invoke(post.id, commentId) }
                )
            }
        }
    }
}

@Composable
fun ChileHeroSection() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(340.dp)
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .testTag("chile_hero_card"),
        shape = RoundedCornerShape(40.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE91E63)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Full bleed visual
            AsyncImage(
                model = "https://images.unsplash.com/photo-1476514525535-07fb3b4ae5f1?w=800",
                contentDescription = "Street Art Chile",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // High-vibrancy Linear Gradient blend Overlay (Orange to Pink)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFF27D26).copy(alpha = 0.85f),
                                Color(0xFFE91E63).copy(alpha = 0.90f)
                            )
                        )
                    )
            )

            // Abstract circular decorative patterns (white overlay with custom alpha)
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                // Top-right large circle border
                drawCircle(
                    color = Color.White,
                    radius = 160.dp.toPx(),
                    center = center.copy(x = size.width * 1.05f, y = size.height * -0.05f),
                    alpha = 0.15f,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 24.dp.toPx())
                )
                // Bottom-left small solid circle
                drawCircle(
                    color = Color.White,
                    radius = 40.dp.toPx(),
                    center = center.copy(x = size.width * 0.15f, y = size.height * 0.75f),
                    alpha = 0.12f
                )
            }

            // Overlap content
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(28.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(100.dp),
                    color = Color.White.copy(alpha = 0.2f),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Text(
                        "TRENDING NOW",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp,
                            color = Color.White
                        ),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }

                Text(
                    "STREET\nART",
                    style = MaterialTheme.typography.displayLarge.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        fontSize = 42.sp,
                        lineHeight = 40.sp,
                        letterSpacing = (-1.5).sp
                    ),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Beautiful accent action button inside hero
                Button(
                    onClick = { /* Action */ },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFFE91E63)
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        "Explore Collection",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

// ==========================================
// 1.B POST CARD COMPONENT
// ==========================================
@Composable
fun PostCard(
    post: PostEntity,
    allProfiles: List<ProfileEntity>,
    onLike: () -> Unit,
    onAddComment: (String) -> Unit,
    onDelete: () -> Unit,
    myProfile: ProfileEntity?,
    onCreatorClick: (ProfileEntity) -> Unit,
    onHashtagClick: (String) -> Unit,
    onMentionClick: (String) -> Unit,
    onPostClick: (() -> Unit)? = null,
    onDeleteComment: ((String) -> Unit)? = null
) {
    var expandedComments by remember { mutableStateOf(false) }
    var newCommentText by remember { mutableStateOf("") }
    val comments = remember(post.commentsJson) { JsonParser.jsonToComments(post.commentsJson) }

    val author = remember(allProfiles, post) {
        allProfiles.find { it.id == post.userId || it.username == post.userUsername }
    }
    val isVerified = author?.isVerified == true

    val isDark = isSystemInDarkTheme()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .clickable { onPostClick?.invoke() }
            .testTag("post_card_${post.id}"),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) MaterialTheme.colorScheme.surface.copy(alpha = 0.65f) else MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (post.isLikedByMe) MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header: User Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = post.userAvatarUrl.ifEmpty { "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=100" },
                    contentDescription = "User Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable {
                            val creator = author ?: ProfileEntity(
                                id = post.userId,
                                username = post.userUsername,
                                fullName = post.userFullName,
                                avatarUrl = post.userAvatarUrl,
                                bio = ""
                            )
                            onCreatorClick(creator)
                        }
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            val creator = author ?: ProfileEntity(
                                id = post.userId,
                                username = post.userUsername,
                                fullName = post.userFullName,
                                avatarUrl = post.userAvatarUrl,
                                bio = ""
                            )
                            onCreatorClick(creator)
                        }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            post.userFullName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        if (isVerified) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                Icons.Rounded.Verified,
                                contentDescription = "Verified Creator",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Text(
                        "@${post.userUsername} • ${formatRelativeTime(post.createdAt)}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    )
                }

                if (myProfile != null && post.userId == myProfile.id) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Rounded.DeleteOutline,
                            contentDescription = "Delete Expression",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Text Content with click hashtag & mention parser
            PassionRichText(
                text = post.content,
                onHashtagClick = onHashtagClick,
                onMentionClick = onMentionClick,
                modifier = Modifier.fillMaxWidth()
            )

            // Post Visual Attachment if present
            if (!post.imageUrl.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                Card(
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                ) {
                    AsyncImage(
                        model = post.imageUrl,
                        contentDescription = "Attachment Image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Likes and Comments count buttons row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Like pill
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (post.isLikedByMe) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .clickable { onLike() }
                        .testTag("like_button_${post.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (post.isLikedByMe) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = "Like",
                            tint = if (post.isLikedByMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "${post.likesCount}",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Comments expand pill
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (expandedComments) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.clickable { expandedComments = !expandedComments }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.ChatBubbleOutline,
                            contentDescription = "Comments",
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "${comments.size}",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            // Animated nested comments block (Image 3 layout details)
            AnimatedVisibility(
                visible = expandedComments,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(12.dp))

                    if (comments.isEmpty()) {
                        Text(
                            "No thoughts shared yet. Be the first!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            comments.forEach { comment ->
                                val commentAuthor = remember(allProfiles, comment) {
                                    allProfiles.find { it.username == comment.username }
                                }
                                val isCommentAuthorVerified = commentAuthor?.isVerified == true

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val creator = commentAuthor ?: ProfileEntity(
                                                id = comment.username,
                                                username = comment.username,
                                                fullName = comment.fullName,
                                                avatarUrl = comment.avatarUrl,
                                                bio = ""
                                            )
                                            onCreatorClick(creator)
                                        },
                                    verticalAlignment = Alignment.Top
                                ) {
                                    if (comment.content.trim().startsWith("@")) {
                                        Icon(
                                            imageVector = Icons.Rounded.KeyboardArrowRight,
                                            contentDescription = "Reply alignment",
                                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                            modifier = Modifier.padding(end = 4.dp, top = 2.dp).size(16.dp)
                                        )
                                    }

                                    AsyncImage(
                                        model = comment.avatarUrl.ifEmpty { "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=100" },
                                        contentDescription = "Commenter Avatar",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                comment.fullName,
                                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold)
                                            )
                                            if (isCommentAuthorVerified) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Icon(
                                                    Icons.Rounded.Verified,
                                                    contentDescription = "Verified Creator",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                "Reply",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                modifier = Modifier.clickable {
                                                    newCommentText = "@${comment.username} "
                                                }
                                            )
                                        }
                                        Text(
                                            comment.content,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }

                                    val canDeleteComment = myProfile != null && (comment.userId == myProfile.id || comment.username == myProfile.username || post.userId == myProfile.id)
                                    if (canDeleteComment && onDeleteComment != null) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        IconButton(
                                            onClick = { onDeleteComment(comment.id) },
                                            modifier = Modifier.size(28.dp).testTag("delete_comment_${comment.id}")
                                        ) {
                                            Icon(
                                                Icons.Rounded.DeleteOutline,
                                                contentDescription = "Delete comment",
                                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Post a new comment field
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newCommentText,
                            onValueChange = { newCommentText = it },
                            placeholder = { Text("Write a friendly comment...") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("comment_input_${post.id}"),
                            shape = RoundedCornerShape(16.dp),
                            textStyle = MaterialTheme.typography.bodyMedium,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (newCommentText.isNotBlank()) {
                                    onAddComment(newCommentText)
                                    newCommentText = ""
                                }
                            },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.testTag("send_comment_${post.id}")
                        ) {
                            Icon(Icons.Rounded.Send, contentDescription = "Publish Comment", modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 2. SEARCH SCREEN
// ==========================================
@Composable
fun SearchScreen(
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    posts: List<PostEntity>,
    allProfiles: List<ProfileEntity>,
    onLike: (String) -> Unit,
    onAddComment: (String, String) -> Unit,
    onDeletePost: (String) -> Unit,
    myProfile: ProfileEntity?,
    onCreatorClick: (ProfileEntity) -> Unit,
    onHashtagClick: (String) -> Unit,
    onMentionClick: (String) -> Unit,
    onPostClick: (String) -> Unit,
    onDeleteComment: ((String, String) -> Unit)? = null
) {
    val presetTags = listOf("#M3Design", "#Adventure", "#Brutalist", "#Berlin", "#Travel")

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            "Search Explore",
            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.W900),
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 8.dp)
        )

        val isDark = isSystemInDarkTheme()
        // Custom search field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onQueryChange,
            placeholder = { Text("Search comments, topics, creators...") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .testTag("search_text_input"),
            shape = RoundedCornerShape(24.dp),
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search Icon") },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Rounded.Clear, contentDescription = "Clear search")
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
            )
        )

        // Preset topic tags scroll
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            presetTags.forEach { tag ->
                val cleanedTag = tag.trim().lowercase()
                val isSelected = searchQuery.lowercase().contains(cleanedTag)
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        if (isSelected) onQueryChange("") else onQueryChange(tag)
                    },
                    label = { Text(tag) },
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            val results = posts.filter {
                searchQuery.isBlank() ||
                it.content.contains(searchQuery, ignoreCase = true) ||
                it.userUsername.contains(searchQuery, ignoreCase = true) ||
                it.userFullName.contains(searchQuery, ignoreCase = true)
            }.distinctBy { it.id }

            if (results.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Rounded.SearchOff,
                                contentDescription = "Empty results",
                                modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "No matches found for '$searchQuery'",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            } else {
                items(results, key = { it.id }) { post ->
                    PostCard(
                        post = post,
                        allProfiles = allProfiles,
                        onLike = { onLike(post.id) },
                        onAddComment = { txt -> onAddComment(post.id, txt) },
                        onDelete = { onDeletePost(post.id) },
                        myProfile = myProfile,
                        onCreatorClick = onCreatorClick,
                        onHashtagClick = onHashtagClick,
                        onMentionClick = onMentionClick,
                        onPostClick = { onPostClick(post.id) },
                        onDeleteComment = { commentId -> onDeleteComment?.invoke(post.id, commentId) }
                    )
                }
            }
        }
    }
}

// ==========================================
// 3. ACTIVITY SCREEN (Image 3 Style notifications)
// ==========================================
@Composable
fun ActivityScreen(
    activities: List<ActivityEntity>,
    onMarkAllRead: () -> Unit,
    onUseImage: (String) -> Unit,
    onTriggerSim: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf("All Workspace") }
    
    // Inline comment states & interaction simulation lists to matches the UI perfectly
    val bComments = remember { 
        mutableStateListOf(
            Pair("https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=100", "@Benjamin here is the link supaui.com/download 👍")
        ) 
    }
    var bInputText by remember { mutableStateOf("") }

    val gComments = remember { 
        mutableStateListOf(
            Pair("https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=100", "@Grace That would be awesome 🔥")
        ) 
    }
    var gInputText by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // High fidelity Top Bar header matching image right device
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { /* Back option */ },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Activity",
                    style = MaterialTheme.typography.headlineLarge.copy(fontSize = 32.sp, fontWeight = FontWeight.Black)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Bolt Sim Trigger Button
                IconButton(
                    onClick = onTriggerSim,
                    modifier = Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
                        .testTag("simulation_trigger")
                ) {
                    Icon(
                        Icons.Rounded.Bolt,
                        contentDescription = "Simulate Activity Event",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                
                Spacer(modifier = Modifier.width(8.dp))

                // Filter icon option
                IconButton(
                    onClick = { /* Option click */ },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Rounded.Tune,
                        contentDescription = "Filters",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }
                
                if (activities.any { !it.isRead }) {
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = onMarkAllRead,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Rounded.DoneAll,
                            contentDescription = "Mark all read",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Horizontal Category Filter bar with specific squircle styling
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val tabs = listOf("All Workspace", "Personal", "Team", "Community")
            tabs.forEach { tab ->
                val isSelected = selectedTab == tab
                val chipBg = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                val chipText = if (isSelected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant
                
                Surface(
                    modifier = Modifier
                        .clickable { selectedTab = tab }
                        .testTag("filter_chip_$tab"),
                    shape = RoundedCornerShape(100.dp),
                    color = chipBg
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = tab,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                            fontSize = 12.sp,
                            color = chipText
                        )
                        
                        if (tab == "Team") {
                            Spacer(modifier = Modifier.width(6.dp))
                            // Vibrant Badge indicator matching the red 3 badge
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFE91E63),
                                modifier = Modifier.size(16.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        "3",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("activity_notification_list"),
            contentPadding = PaddingValues(bottom = 110.dp)
        ) {
            // Screen Section: HIGH FIDELITY ITEMS
            
            // --- ITEM 1: Benjamin Comment (Personal / All Workspace) ---
            if (selectedTab == "All Workspace" || selectedTab == "Personal") {
                item {
                    ActivityContainer(
                        iconBadge = {
                            StatusBadge(
                                icon = Icons.Rounded.ChatBubble,
                                bgColor = Color(0xFFE2F9F3),
                                iconColor = Color(0xFF00B074)
                            )
                        },
                        avatarUrl = "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=100",
                        headerText = buildAnnotatedString {
                            withStyle(style = SpanStyle(fontWeight = FontWeight.ExtraBold)) {
                                append("Benjamin")
                            }
                            append(" commented on ")
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)) {
                                append("Sora UI Kit")
                            }
                        },
                        timeText = "1h.",
                        modifier = Modifier.testTag("mock_benjamin")
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                "What a good design! I like how you dealt with the spacing. Where can I get this file?",
                                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            // Loop replies list
                            bComments.forEach { reply ->
                                ReplyItemRow(avatarUrl = reply.first, content = reply.second)
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Action Inline Comment Input
                            CommentInputBar(
                                value = bInputText,
                                onValueChange = { bInputText = it },
                                onSend = {
                                    bComments.add(Pair("https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=100", bInputText))
                                    bInputText = ""
                                    Toast.makeText(context, "Comment sent successfully!", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            }

            // --- ITEM 2: Jacob Midjourney Generation (Team / All Workspace) ---
            if (selectedTab == "All Workspace" || selectedTab == "Team") {
                item {
                    ActivityContainer(
                        iconBadge = {
                            StatusBadge(
                                icon = Icons.Rounded.AttachFile,
                                bgColor = Color(0xFFE3F2FD),
                                iconColor = Color(0xFF2196F3)
                            )
                        },
                        avatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=100",
                        headerText = buildAnnotatedString {
                            withStyle(style = SpanStyle(fontWeight = FontWeight.ExtraBold)) {
                                append("Jacob")
                            }
                            append(" generated a new images on ")
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)) {
                                append("Midjourney")
                            }
                        },
                        timeText = "8h",
                        modifier = Modifier.testTag("mock_jacob")
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // High detail attachment design
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(240.dp)
                                    .padding(bottom = 12.dp),
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    AsyncImage(
                                        model = "https://images.unsplash.com/photo-1600585154340-be6161a56a0c?w=800",
                                        contentDescription = "Beach House Midjourney",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }

                            // Dynamic Dropdown & Button row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    var showMenu by remember { mutableStateOf(false) }
                                    Box {
                                        Button(
                                            onClick = { showMenu = true },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFF2196F3),
                                                contentColor = Color.White
                                            ),
                                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("Download", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Dropdown", modifier = Modifier.size(16.dp))
                                            }
                                        }

                                        DropdownMenu(
                                            expanded = showMenu,
                                            onDismissRequest = { showMenu = false }
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("Original PNG (Ultra)") },
                                                onClick = {
                                                    showMenu = false
                                                    Toast.makeText(context, "📥 Downloaded High Resolution Original!", Toast.LENGTH_SHORT).show()
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Standard WebP") },
                                                onClick = {
                                                    showMenu = false
                                                    Toast.makeText(context, "📥 Downloaded Standard Image Preview!", Toast.LENGTH_SHORT).show()
                                                }
                                            )
                                        }
                                    }

                                    OutlinedButton(
                                        onClick = { 
                                            onUseImage("https://images.unsplash.com/photo-1600585154340-be6161a56a0c?w=800") 
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                                    ) {
                                        Text("Use image", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }

                                IconButton(onClick = { /* Options Menu */ }) {
                                    Icon(
                                        Icons.Rounded.HorizontalRule,
                                        contentDescription = "Options",
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // --- ITEM 3: Likes Aggregation (Community / All Workspace) ---
            if (selectedTab == "All Workspace" || selectedTab == "Community") {
                item {
                    ActivityContainer(
                        iconBadge = {
                            StatusBadge(
                                icon = Icons.Rounded.Favorite,
                                bgColor = Color(0xFFFFEBEE),
                                iconColor = Color(0xFFE91E63)
                            )
                        },
                        avatarUrl = null, // Custom layered avatar avatar row on the body
                        headerText = buildAnnotatedString {
                            withStyle(style = SpanStyle(fontWeight = FontWeight.ExtraBold)) {
                                append("Benjamin and 8 others")
                            }
                            append(" liked your article")
                        },
                        timeText = "12h",
                        modifier = Modifier.testTag("mock_likes")
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Left overlapping list of avatars as shown in Image 3
                            Row(
                                modifier = Modifier.padding(bottom = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy((-10).dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val listAvatars = listOf(
                                    "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=100",
                                    "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=100",
                                    "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=100",
                                    "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=100",
                                    "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=100"
                                )
                                listAvatars.forEach { url ->
                                    AsyncImage(
                                        model = url,
                                        contentDescription = "overlapping pile avatar",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .border(2.dp, MaterialTheme.colorScheme.background, CircleShape)
                                    )
                                }
                            }

                            // Embedded Article beautiful preview card
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "Understand the Design Language",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            "Regardless of the platform, it's crucial you understand its design language. This include...",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    AsyncImage(
                                        model = "https://images.unsplash.com/photo-1513542789411-b6a5d4f31634?w=150",
                                        contentDescription = "article visual block",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // --- ITEM 4: Grace Comment on Fleet (Personal / All Workspace) ---
            if (selectedTab == "All Workspace" || selectedTab == "Personal") {
                item {
                    ActivityContainer(
                        iconBadge = {
                            StatusBadge(
                                icon = Icons.Rounded.ChatBubble,
                                bgColor = Color(0xFFE2F9F3),
                                iconColor = Color(0xFF00B074)
                            )
                        },
                        avatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=100",
                        headerText = buildAnnotatedString {
                            withStyle(style = SpanStyle(fontWeight = FontWeight.ExtraBold)) {
                                append("Grace")
                            }
                            append(" commented on ")
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)) {
                                append("Fleet UI Kit")
                            }
                        },
                        timeText = "1d",
                        modifier = Modifier.testTag("mock_grace")
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                "Having some fun with these activity components. It has the potential to be a freebie. Stay tuned to get the updates!",
                                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            // Replies
                            gComments.forEach { reply ->
                                ReplyItemRow(avatarUrl = reply.first, content = reply.second)
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Action Inline comment bar
                            CommentInputBar(
                                value = gInputText,
                                onValueChange = { gInputText = it },
                                onSend = {
                                    gComments.add(Pair("https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=100", gInputText))
                                    gInputText = ""
                                    Toast.makeText(context, "Comment sent successfully!", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            }

            // --- USER'S REAL DATABASE NOTIFICATIONS (Integrated on All Workspace table) ---
            if (activities.isNotEmpty()) {
                item {
                    Text(
                        "Workspace Updates",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        modifier = Modifier.padding(start = 24.dp, top = 24.dp, bottom = 12.dp)
                    )
                }

                items(activities) { act ->
                    // Skip mock items since we loaded high-detail ones above, or render standard Database Items
                    ActivityItemCard(
                        act = act,
                        onDownload = {
                            Toast.makeText(context, "📥 Downloaded database attachment!", Toast.LENGTH_SHORT).show()
                        },
                        onUseImage = { onUseImage(it) }
                    )
                }
            }
        }
    }
}

// Custom timeline node item container
@Composable
fun ActivityContainer(
    iconBadge: @Composable () -> Unit,
    avatarUrl: String?,
    headerText: androidx.compose.ui.text.AnnotatedString,
    timeText: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Timeline or Visual Badge Column on the left
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(42.dp)
        ) {
            iconBadge()
            // Spacer/Dotted connection line
            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Right side details column
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    if (avatarUrl != null) {
                        AsyncImage(
                            model = avatarUrl,
                            contentDescription = "Avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color.LightGray)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                    }
                    Text(
                        text = headerText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = timeText,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            // Central body
            content()
            
            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f), thickness = 1.dp)
        }
    }
}

// Indicator badge icon for events
@Composable
fun StatusBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    bgColor: Color,
    iconColor: Color
) {
    Surface(
        shape = CircleShape,
        color = bgColor,
        modifier = Modifier.size(34.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// Nested Comment row representation
@Composable
fun ReplyItemRow(avatarUrl: String, content: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = avatarUrl,
            contentDescription = "sub avatar",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(Color.LightGray)
        )
        Spacer(modifier = Modifier.width(10.dp))
        
        // Highlight link/mentions
        val styledReply = buildAnnotatedString {
            if (content.startsWith("@")) {
                val endOfAt = content.indexOf(" ").takeIf { it != -1 } ?: content.length
                val mention = content.substring(0, endOfAt)
                val rest = content.substring(endOfAt)
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)) {
                    append(mention)
                }
                append(rest)
            } else {
                append(content)
            }
        }

        Text(
            text = styledReply,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
        )
    }
}

// Comment input entry bar
@Composable
fun CommentInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text("Write a comment...", fontSize = 13.sp) },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(100.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = Color.Transparent,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            ),
            trailingIcon = {
                Icon(
                    Icons.Rounded.SentimentSatisfied,
                    contentDescription = "Smiley emoji selector",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp)
                )
            },
            singleLine = true
        )
        Spacer(modifier = Modifier.width(6.dp))
        TextButton(
            onClick = {
                if (value.isNotBlank()) {
                    onSend()
                }
            }
        ) {
            Text("Send", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
        }
    }
}

@Composable
fun ActivityItemCard(
    act: ActivityEntity,
    onDownload: () -> Unit,
    onUseImage: (String) -> Unit
) {
    // Generate dummy preview photo URL matching specific activity triggers
    val previewPhotoUrl = when (act.postId) {
        "post_1" -> "https://images.unsplash.com/photo-1476514525535-07fb3b4ae5f1?w=800"
        "post_2" -> "https://images.unsplash.com/photo-1600585154340-be6161a56a0c?w=800"
        else -> null
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .testTag("activity_item_${act.id}"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (act.isRead) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                AsyncImage(
                    model = act.actorAvatarUrl.ifEmpty { "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=100" },
                    contentDescription = "Notification Actor Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = act.actorUsername,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = act.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }

                // Action Icon indicators
                val actionIcon = when (act.type) {
                    "like" -> Icons.Rounded.Favorite to Color.Red
                    "comment" -> Icons.Rounded.ChatBubble to MaterialTheme.colorScheme.primary
                    else -> Icons.Rounded.AlternateEmail to MaterialTheme.colorScheme.secondary
                }
                Icon(
                    actionIcon.first,
                    contentDescription = act.type,
                    tint = actionIcon.second,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Image Preview box with action buttons like Image 3
            if (previewPhotoUrl != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    AsyncImage(
                        model = previewPhotoUrl,
                        contentDescription = "Attachment preview",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDownload) {
                        Icon(Icons.Rounded.Download, contentDescription = "Download", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Download", style = MaterialTheme.typography.labelLarge)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onUseImage(previewPhotoUrl) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        )
                    ) {
                        Icon(Icons.Rounded.Brush, contentDescription = "Use", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Use Image", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

// ==========================================
// 4. PROFILE SCREEN & SUPABASE Cloud Settings
// ==========================================
@Composable
fun ProfileScreen(
    myProfile: ProfileEntity?,
    posts: List<PostEntity>,
    onUpdateProfile: (String, String, String, String, String) -> Unit,
    viewModel: SocialViewModel,
    onCreatorClick: (ProfileEntity) -> Unit,
    onHashtagClick: (String) -> Unit,
    onMentionClick: (String) -> Unit,
    onEditProfileClick: () -> Unit,
    onPostClick: (String) -> Unit,
    onDeleteComment: ((String, String) -> Unit)? = null
) {
    var showCloudSettings by remember { mutableStateOf(false) }
    var showFollowListCategory by remember { mutableStateOf<String?>(null) } // "Followers" or "Following" or null
    var activeTab by remember { mutableStateOf("Expressions") } // "Expressions" or "Liked Ledger"

    val allProfiles by viewModel.allProfiles.collectAsStateWithLifecycle()
    
    // Filter list representing user own posts
    val myPosts = remember(posts, myProfile) {
        if (myProfile == null) emptyList() else posts.filter { it.userId == myProfile.id }
    }
    
    // Filter list representing posts Jordan has liked
    val myLikedPosts = remember(posts, myProfile) {
        if (myProfile == null) emptyList() else posts.filter { it.isLikedByMe }
    }

    if (myProfile == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("profile_view_list"),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        // High fidelity cover & avatar block with depth offset
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                // Background cover premium image
                AsyncImage(
                    model = if (myProfile.bannerUrl.isNotBlank()) myProfile.bannerUrl else "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=800",
                    contentDescription = "Cover Image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // Gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.background.copy(alpha = 0.5f)
                                )
                            )
                        )
                )
            }
        }

        // Overlapping Avatar & details column
        item {
            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .offset(y = (-50).dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    AsyncImage(
                        model = myProfile.avatarUrl,
                        contentDescription = "My Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(4.dp)
                            .clip(CircleShape)
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Button(
                            onClick = { onEditProfileClick() },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.testTag("edit_profile_button")
                        ) {
                            Icon(Icons.Rounded.Edit, contentDescription = "Edit Profile", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Edit profile")
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        // Beautiful logout action button icon
                        IconButton(
                            onClick = { viewModel.logOut() },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            ),
                            modifier = Modifier.testTag("logout_button")
                        ) {
                            Icon(Icons.Rounded.Logout, contentDescription = "Log Out")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    myProfile.fullName,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.W900)
                )
                Text(
                    "@${myProfile.username}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    myProfile.bio,
                    style = MaterialTheme.typography.bodyLarge,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Interactive live tracking counts row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    ProfileCountPill(label = "Expressions", count = myPosts.size)
                    Spacer(modifier = Modifier.width(12.dp))
                    ProfileCountPill(
                        label = "Followers", 
                        count = myProfile.followersCount,
                        modifier = Modifier.clickable { showFollowListCategory = "Followers" }
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    ProfileCountPill(
                        label = "Following", 
                        count = myProfile.followingCount,
                        modifier = Modifier.clickable { showFollowListCategory = "Following" }
                    )
                }
            }
        }

        // Section Tabs Switcher representing complete management sub-system
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .offset(y = (-30).dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val profileTabs = listOf("Expressions", "Liked Ledger")
                profileTabs.forEach { tab ->
                    val isSelected = activeTab == tab
                    val buttonBg = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    val textColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { activeTab = tab }
                            .testTag("profile_tab_$tab"),
                        shape = RoundedCornerShape(12.dp),
                        color = buttonBg
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 12.dp)) {
                            Text(
                                text = tab, 
                                fontWeight = FontWeight.Bold, 
                                color = textColor, 
                                fontSize = 13.sp,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                    }
                }
            }
        }

        val listToRender = if (activeTab == "Expressions") myPosts else myLikedPosts

        if (listToRender.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(48.dp)
                        .offset(y = (-30).dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (activeTab == "Expressions") "You haven't expressed anything yet. Share your first thought!" else "Your activity ledger has no liked posts yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        } else {
            items(listToRender, key = { it.id }) { post ->
                Box(modifier = Modifier.offset(y = (-30).dp)) {
                    PostCard(
                        post = post,
                        allProfiles = allProfiles,
                        onLike = { viewModel.toggleLike(post.id) },
                        onAddComment = { txt -> viewModel.addComment(post.id, txt) },
                        onDelete = { viewModel.deletePost(post.id) },
                        myProfile = myProfile,
                        onCreatorClick = onCreatorClick,
                        onHashtagClick = onHashtagClick,
                        onMentionClick = onMentionClick,
                        onPostClick = { onPostClick(post.id) },
                        onDeleteComment = { commentId -> onDeleteComment?.invoke(post.id, commentId) }
                    )
                }
            }
        }
    }

    // Followers / Following dialog list popup
    showFollowListCategory?.let { category ->
        FollowListDialog(
            type = category,
            onDismiss = { showFollowListCategory = null },
            viewModel = viewModel,
            onCreatorClick = onCreatorClick
        )
    }



    // Supabase Cloud settings sheet Dialog
    if (showCloudSettings) {
        SupabaseSettingsDialog(
            viewModel = viewModel,
            onDismiss = { showCloudSettings = false }
        )
    }
}

@Composable
fun ProfileCountPill(label: String, count: Int, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
            Text(
                "$count",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.W900)
            )
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

// ==========================================
// 5. CREATE POST DIALOG
// ==========================================
@Composable
fun CreatePostDialog(
    prefilledImageUrl: String?,
    onDismiss: () -> Unit,
    onPublish: (String, String?) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val draftPrefs = remember { context.getSharedPreferences("pulse_draft_prefs", android.content.Context.MODE_PRIVATE) }
    val draftContent = remember { draftPrefs.getString("draft_content", "") ?: "" }
    val draftImageUrl = remember { draftPrefs.getString("draft_image_url", "") ?: "" }

    var contentText by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf(prefilledImageUrl ?: "") }
    var showDraftPill by remember { mutableStateOf(draftContent.isNotEmpty()) }

    // Real-time draft autosave
    androidx.compose.runtime.LaunchedEffect(contentText, imageUrl) {
        if (contentText.isNotEmpty() || imageUrl.isNotEmpty()) {
            draftPrefs.edit()
                .putString("draft_content", contentText)
                .putString("draft_image_url", imageUrl)
                .apply()
        }
    }

    val stockImages = listOf(
        "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=500" to "Beach Sunrise",
        "https://images.unsplash.com/photo-1454496522488-7a8e488e8606?w=500" to "Snow Peak",
        "https://images.unsplash.com/photo-1447752875215-b2761acb3c5d?w=500" to "Forest",
        "https://images.unsplash.com/photo-1470071459604-3b5ec3a7fe05?w=500" to "Foggy Meadow"
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .testTag("create_post_dialog_content"),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // LIVE POST FEED PREVIEW HEADER
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .padding(16.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // Mock Header
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    modifier = Modifier.size(36.dp),
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("J", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Jordan Vance", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                    Text("@jordan • Just now", style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray))
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            
                            // Content
                            Text(
                                text = if (contentText.isBlank()) "Expression preview appears here..." else contentText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (contentText.isBlank()) Color.Gray else MaterialTheme.colorScheme.onSurface,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                            
                            // Visual attachment preview
                            if (imageUrl.isNotBlank()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Card(
                                    modifier = Modifier.fillMaxWidth().height(100.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    AsyncImage(
                                        model = imageUrl,
                                        contentDescription = "Attachment preview",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }
                }

                // SCROLLABLE FORM CONTROLS
                Column(
                    modifier = Modifier
                        .heightIn(max = 350.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp)
                ) {
                    Text(
                        text = "Compose Expression",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.W900),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    if (showDraftPill && draftContent.isNotEmpty()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Rounded.Drafts,
                                        contentDescription = "Draft Detected",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Unsaved draft found!",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    TextButton(
                                        onClick = {
                                            contentText = draftContent
                                            if (draftImageUrl.isNotEmpty()) {
                                                imageUrl = draftImageUrl
                                            }
                                            showDraftPill = false
                                        },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text("Restore", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold))
                                    }
                                    IconButton(
                                        onClick = {
                                            draftPrefs.edit().clear().apply()
                                            showDraftPill = false
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Rounded.DeleteOutline,
                                            contentDescription = "Clear Draft",
                                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Input Content
                    OutlinedTextField(
                        value = contentText,
                        onValueChange = { contentText = it },
                        placeholder = { Text("What perspective are you sharing Jordan? Share your thoughts...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .testTag("post_content_input"),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.05f)
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Stock travel themes selector
                    Text(
                        text = "Or choose a beautiful template backdrop:",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        stockImages.forEach { pair ->
                            val isSelected = imageUrl == pair.first
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clickable { imageUrl = pair.first }
                                    .border(
                                        width = if (isSelected) 3.dp else 0.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    AsyncImage(
                                        model = pair.first,
                                        contentDescription = pair.second,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.4f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            pair.second.split(" ").first(),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = Color.White,
                                                fontWeight = FontWeight.Black
                                            ),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Image URL input - Collapsible Advanced Control
                    var showCustomImageOption by remember { mutableStateOf(false) }
                    TextButton(onClick = { showCustomImageOption = !showCustomImageOption }) {
                        Text(if (showCustomImageOption) "Hide custom photo input" else "Or provide a custom image URL...")
                    }

                    if (showCustomImageOption) {
                        OutlinedTextField(
                            value = imageUrl,
                            onValueChange = { imageUrl = it },
                            placeholder = { Text("https://example.com/artwork.jpg") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("post_image_url_input"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
                            )
                        )
                    }
                }

                // BOTTOM ACTION BAR
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            if (contentText.isNotBlank()) {
                                draftPrefs.edit().clear().apply()
                                onPublish(contentText, imageUrl.ifBlank { null })
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        enabled = contentText.isNotBlank(),
                        modifier = Modifier.testTag("post_publish_button")
                    ) {
                        Text("Share")
                    }
                }
            }
        }
    }
}

// ==========================================
// 6. EDIT PROFILE DIALOG
// ==========================================
@Composable
fun EditProfileDialog(
    profile: ProfileEntity,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(profile.fullName) }
    var username by remember { mutableStateOf(profile.username) }
    var bio by remember { mutableStateOf(profile.bio) }
    var avatar by remember { mutableStateOf(profile.avatarUrl) }
    var banner by remember { mutableStateOf(profile.bannerUrl) }

    val scenicBanners = listOf(
        "https://images.unsplash.com/photo-1506318137071-a8e063b4bec0?w=600" to "Cosmic",
        "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=600" to "Misty",
        "https://images.unsplash.com/photo-1600585154340-be6161a56a0c?w=600" to "Brutalist",
        "https://images.unsplash.com/photo-1515621061946-eff1c2a352bd?w=600" to "Cyber"
    )
    val profileAvatars = listOf(
        "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150" to "Creative",
        "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150" to "Slick",
        "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150" to "Active",
        "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=150" to "Gradient"
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .testTag("edit_profile_dialog_content"),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                // LIVE PROFILE CARD RUNTIME PREVIEW
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                ) {
                    AsyncImage(
                        model = banner.ifBlank { "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=800" },
                        contentDescription = "Cover Image live preview",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    // Dimming overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Black.copy(alpha = 0.2f), Color.Black.copy(alpha = 0.75f))
                                )
                            )
                    )
                    // Overlay details preview
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        AsyncImage(
                            model = avatar.ifBlank { "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=100" },
                            contentDescription = "Avatar Live Preview",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .border(2.dp, Color.White, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = name.ifBlank { "Jordan Vance" },
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    fontSize = 15.sp
                                )
                            )
                            Text(
                                text = "@${username.ifBlank { "jordan" }}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        }
                    }
                }

                // SCROLLABLE FORM SECTION
                Column(
                    modifier = Modifier
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 20.dp)
                ) {
                    Text(
                        text = "Customize Profile Card",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.W900),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Display Name Field
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Display Name") },
                        leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_profile_name"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Username Handle Field
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Username Handle") },
                        leadingIcon = { Icon(Icons.Rounded.AlternateEmail, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_profile_username"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Biography Field
                    OutlinedTextField(
                        value = bio,
                        onValueChange = { bio = it },
                        label = { Text("Biography / Short Status") },
                        leadingIcon = { Icon(Icons.Rounded.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .testTag("edit_profile_bio"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // BANNERS PRESETS SELECTOR
                    Text(
                        text = "Scenic Cover Presets:",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        scenicBanners.forEach { pair ->
                            val isSelected = banner == pair.first
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clickable { banner = pair.first }
                                    .border(
                                        width = if (isSelected) 3.dp else 0.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    AsyncImage(
                                        model = pair.first,
                                        contentDescription = pair.second,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.4f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = pair.second,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = Color.White,
                                                fontWeight = FontWeight.Black
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // AVATAR PRESETS SELECTOR
                    Text(
                        text = "Creative Avatar Presets:",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        profileAvatars.forEach { pair ->
                            val isSelected = avatar == pair.first
                            AsyncImage(
                                model = pair.first,
                                contentDescription = pair.second,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(45.dp)
                                    .clip(CircleShape)
                                    .clickable { avatar = pair.first }
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                        shape = CircleShape
                                    )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // ADVANCED MANUAL VALUES (COLLAPSIBLE / OPTIONAL IN TEXT FIELD)
                    var showUrls by remember { mutableStateOf(false) }
                    TextButton(
                        onClick = { showUrls = !showUrls },
                        modifier = Modifier.align(Alignment.Start)
                    ) {
                        Text(if (showUrls) "Hide original URL inputs" else "Advanced: custom photo URLs")
                    }

                    if (showUrls) {
                        OutlinedTextField(
                            value = avatar,
                            onValueChange = { avatar = it },
                            label = { Text("Avatar Photo URL") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("edit_profile_avatar"),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = banner,
                            onValueChange = { banner = it },
                            label = { Text("Cover Banner URL") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("edit_profile_banner"),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                // BOTTOM ACTION BAR
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Discard")
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank() && username.isNotBlank()) {
                                onSave(name, username, bio, avatar, banner)
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        enabled = name.isNotBlank() && username.isNotBlank(),
                        modifier = Modifier.testTag("edit_profile_save_button")
                    ) {
                        Text("Save Card")
                    }
                }
            }
        }
    }
}

// ==========================================
// 7. SUPABASE CLOUD CONNECTION SETTINGS DIALOG
// ==========================================
@Composable
fun SupabaseSettingsDialog(
    viewModel: SocialViewModel,
    onDismiss: () -> Unit
) {
    var urlInput by remember { mutableStateOf(viewModel.supabase.url) }
    var keyInput by remember { mutableStateOf(viewModel.supabase.key) }
    var enabledInput by remember { mutableStateOf(viewModel.supabase.isEnabled) }

    var testStatus by remember { mutableStateOf<String?>(null) }
    var testingConnection by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .testTag("supabase_setup_details"),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Supabase Config",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.W900)
                    )

                    Switch(
                        checked = enabledInput,
                        onCheckedChange = { enabledInput = it },
                        modifier = Modifier.testTag("supabase_toggle_switch")
                    )
                }

                Text(
                    "Interface directly with your remote database. Toggle online connection or save local offline cache seamlessly.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // DB Endpoint
                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    label = { Text("Supabase URL Endpoint") },
                    placeholder = { Text("https://xxxx.supabase.co") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("supabase_url"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    enabled = enabledInput
                )

                Spacer(modifier = Modifier.height(12.dp))

                // DB Anon Key
                OutlinedTextField(
                    value = keyInput,
                    onValueChange = { keyInput = it },
                    label = { Text("Supabase API Anon Key") },
                    placeholder = { Text("eyJhbGciOiJIUzI1NiIsInR5...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("supabase_key"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    enabled = enabledInput
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Test connection trigger
                if (enabledInput && urlInput.isNotBlank() && keyInput.isNotBlank()) {
                    Button(
                        onClick = {
                            testingConnection = true
                            testStatus = "Checking server nodes..."
                            scope.launch {
                                // temporarily apply config values and test
                                val originalUrl = viewModel.supabase.url
                                val originalKey = viewModel.supabase.key
                                viewModel.supabase.url = urlInput
                                viewModel.supabase.key = keyInput
                                
                                val responseSuccess = viewModel.supabase.testConnection()
                                
                                testStatus = if (responseSuccess) {
                                    "Connected! Database Schema verified."
                                } else {
                                    "Connection refused. Verify endpoint credentials."
                                }
                                
                                // restore
                                viewModel.supabase.url = originalUrl
                                viewModel.supabase.key = originalKey
                                testingConnection = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (testingConnection) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Test connection", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                testStatus?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (it.startsWith("Connected")) Color(0xFF00C853) else Color.Red
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Save actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Dismiss")
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            viewModel.saveConfig(urlInput, keyInput, enabledInput)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.testTag("supabase_save_button")
                    ) {
                        Text("Save & Sync")
                    }
                }
            }
        }
    }
}

// Global Relative Time Formatter
fun formatRelativeTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 60000L -> "just now"
        diff < 3600000L -> "${diff / 60000L}m ago"
        diff < 86400000L -> "${diff / 3600000L}h ago"
        diff < 604800000L -> "${diff / 86400000L}d ago"
        else -> {
            val date = java.util.Date(timestamp)
            val sdf = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
            sdf.format(date)
        }
    }
}

// ==========================================
// NEW FULL SCREENS (REDESIGNED EXQUISITELY)
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostScreen(
    prefilledImageUrl: String?,
    onDismiss: () -> Unit,
    onPublish: (String, String?) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val draftPrefs = remember { context.getSharedPreferences("pulse_draft_prefs", android.content.Context.MODE_PRIVATE) }
    val draftContent = remember { draftPrefs.getString("draft_content", "") ?: "" }
    val draftImageUrl = remember { draftPrefs.getString("draft_image_url", "") ?: "" }

    var contentText by remember { mutableStateOf(draftContent) }
    var imageUrl by remember { mutableStateOf(prefilledImageUrl ?: draftImageUrl) }
    var showDraftPill by remember { mutableStateOf(draftContent.isNotEmpty()) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            imageUrl = uri.toString()
        }
    }

    LaunchedEffect(contentText, imageUrl) {
        draftPrefs.edit()
            .putString("draft_content", contentText)
            .putString("draft_image_url", imageUrl)
            .apply()
    }

    val stockImages = listOf(
        "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=500" to "Beach Sunrise",
        "https://images.unsplash.com/photo-1454496522488-7a8e488e8606?w=500" to "Snow Peak",
        "https://images.unsplash.com/photo-1447752875215-b2761acb3c5d?w=500" to "Forest",
        "https://images.unsplash.com/photo-1470071459604-3b5ec3a7fe05?w=500" to "Foggy Meadow"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Compose Passion", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            if (contentText.isNotBlank() || imageUrl.isNotBlank()) {
                                draftPrefs.edit().clear().apply()
                                onPublish(contentText, imageUrl.ifBlank { null })
                            } else {
                                Toast.makeText(context, "Please voice some words or attach an image!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.testTag("publish_post_button_top")
                    ) {
                        Text("Publish", fontWeight = FontWeight.ExtraBold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            if (showDraftPill) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.EditNote, contentDescription = "Drafts", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Unsent draft loaded from local storage.", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        TextButton(onClick = {
                            contentText = ""
                            imageUrl = ""
                            draftPrefs.edit().clear().apply()
                            showDraftPill = false
                        }) {
                            Text("Clear", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            OutlinedTextField(
                value = contentText,
                onValueChange = { if (it.length <= 280) contentText = it },
                placeholder = { Text("What are you passionate about? Write here...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .testTag("create_post_input"),
                textStyle = MaterialTheme.typography.bodyLarge,
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
            )

            Text(
                text = "${contentText.length}/280 Characters",
                style = MaterialTheme.typography.labelSmall,
                color = if (contentText.length >= 280) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 6.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (imageUrl.isNotBlank()) {
                Text("Attached Visual preview:", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .clip(RoundedCornerShape(24.dp))
                ) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = "Attachment preview",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    IconButton(
                        onClick = { imageUrl = "" },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = "Remove", tint = Color.White)
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            Text("Attach Creative Media", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(80.dp)
                        .clickable { galleryLauncher.launch("image/*") }
                        .testTag("gallery_picker_btn"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Rounded.PhotoLibrary, contentDescription = "Gallery", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Gallery Photo", style = MaterialTheme.typography.labelMedium)
                    }
                }

                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(80.dp)
                        .clickable {
                            val nextStock = stockImages.random()
                            imageUrl = nextStock.first
                            Toast.makeText(context, "Selected preset color: ${nextStock.second}", Toast.LENGTH_SHORT).show()
                        },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Rounded.ColorLens, contentDescription = "Presets", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Stock Presets", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = {
                    if (contentText.isNotBlank() || imageUrl.isNotBlank()) {
                        draftPrefs.edit().clear().apply()
                        onPublish(contentText, imageUrl.ifBlank { null })
                    } else {
                        Toast.makeText(context, "Please voice some words or attach an image!", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("publish_post_button_bottom"),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(Icons.Rounded.Send, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Publish Passion Thread", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    profile: ProfileEntity,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(profile.fullName) }
    var username by remember { mutableStateOf(profile.username) }
    var bio by remember { mutableStateOf(profile.bio) }
    var avatar by remember { mutableStateOf(profile.avatarUrl) }
    var banner by remember { mutableStateOf(profile.bannerUrl) }

    val context = androidx.compose.ui.platform.LocalContext.current

    val avatarLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            avatar = uri.toString()
        }
    }

    val bannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            banner = uri.toString()
        }
    }

    val scenicBanners = listOf(
        "https://images.unsplash.com/photo-1506318137071-a8e063b4bec0?w=600" to "Cosmic",
        "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=600" to "Misty",
        "https://images.unsplash.com/photo-1600585154340-be6161a56a0c?w=600" to "Brutalist",
        "https://images.unsplash.com/photo-1515621061946-eff1c2a352bd?w=600" to "Cyber"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Update My Persona", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            if (name.isBlank() || username.isBlank()) {
                                Toast.makeText(context, "Full name and username cannot be blank!", Toast.LENGTH_SHORT).show()
                            } else {
                                onSave(name, username, bio, avatar, banner)
                            }
                        },
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.testTag("save_profile_button_top")
                    ) {
                        Text("Save Card", fontWeight = FontWeight.ExtraBold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                AsyncImage(
                    model = banner.ifBlank { "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=800" },
                    contentDescription = "Cover preview",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                            )
                        )
                )

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    AsyncImage(
                        model = avatar.ifBlank { "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150" },
                        contentDescription = "Avatar preview",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .border(3.dp, Color.White, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.padding(bottom = 4.dp)) {
                        Text(
                            text = name.ifBlank { "Jordan Sparks" },
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black, color = Color.White)
                        )
                        Text(
                            text = "@${username.ifBlank { "creative_mind" }}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primaryContainer)
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(24.dp)) {
                Text("Aesthetic Media Pickers", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { avatarLauncher.launch("image/*") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.Portrait, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pick Avatar")
                    }

                    OutlinedButton(
                        onClick = { bannerLauncher.launch("image/*") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.Landscape, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pick Cover")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text("Scenic Banners Presets", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    scenicBanners.forEach { pair ->
                        val isSelected = banner == pair.first
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clickable { banner = pair.first }
                                .border(
                                    width = if (isSelected) 3.dp else 0.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                AsyncImage(
                                    model = pair.first,
                                    contentDescription = pair.second,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.4f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = pair.second,
                                        style = MaterialTheme.typography.labelSmall.copy(color = Color.White, fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        }
                    }
                }

                Text("Profile Credentials", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Display Name") },
                    leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth().testTag("edit_profile_name"),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username Handle") },
                    leadingIcon = { Icon(Icons.Rounded.AlternateEmail, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth().testTag("edit_profile_username"),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("Biography") },
                    leadingIcon = { Icon(Icons.Rounded.EditNote, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .testTag("edit_profile_bio"),
                    shape = RoundedCornerShape(16.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        if (name.isBlank() || username.isBlank()) {
                            Toast.makeText(context, "Full name and username cannot be blank!", Toast.LENGTH_SHORT).show()
                        } else {
                            onSave(name, username, bio, avatar, banner)
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("save_profile_button_bottom")
                ) {
                    Icon(Icons.Rounded.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save & Update Card Info", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    post: PostEntity,
    allProfiles: List<ProfileEntity>,
    onBack: () -> Unit,
    onLike: () -> Unit,
    onAddComment: (String) -> Unit,
    onDeleteComment: (String) -> Unit,
    myProfile: ProfileEntity?
) {
    var newCommentText by remember { mutableStateOf("") }
    val comments = remember(post.commentsJson) { JsonParser.jsonToComments(post.commentsJson) }
    val author = remember(allProfiles, post) {
        allProfiles.find { it.id == post.userId || it.username == post.userUsername }
    }
    val isVerified = author?.isVerified == true

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Passion Thread", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(24.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = post.userAvatarUrl.ifEmpty { "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=100" },
                            contentDescription = "User Avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    post.userFullName,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                                )
                                if (isVerified) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        Icons.Rounded.Verified,
                                        contentDescription = "Verified Creator",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Text(
                                "@${post.userUsername}",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        post.content,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Normal,
                            lineHeight = 28.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (post.imageUrl != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 300.dp),
                            shape = RoundedCornerShape(24.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            AsyncImage(
                                model = post.imageUrl,
                                contentDescription = "Post Expression Visual",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        formatRelativeTime(post.createdAt).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${post.likesCount}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Likes",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${comments.size}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Comments",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        IconButton(onClick = onLike) {
                            Icon(
                                imageVector = if (post.isLikedByMe) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                contentDescription = "Like icon",
                                tint = if (post.isLikedByMe) Color(0xFFE91E63) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(onClick = {}) {
                            Icon(
                                Icons.Rounded.ChatBubbleOutline,
                                contentDescription = "Comments",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (comments.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "No comments yet",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                            Text(
                                "Be the first to reply and start the dialogue!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                        }
                    }
                } else {
                    items(comments, key = { it.id }) { comment ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            ) {
                                AsyncImage(
                                    model = comment.avatarUrl.ifEmpty { "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=100" },
                                    contentDescription = "Commenter Avatar",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            comment.fullName,
                                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            "@${comment.username}",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                            )
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        comment.content,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                if (myProfile != null && comment.userId == myProfile.id) {
                                    IconButton(
                                        onClick = { onDeleteComment(comment.id) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Rounded.DeleteOutline,
                                            contentDescription = "Delete comment",
                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.06f))
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 4.dp,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newCommentText,
                        onValueChange = { newCommentText = it },
                        placeholder = { Text("Post your Passion reply...") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("comment_input_feed"),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    IconButton(
                        onClick = {
                            if (newCommentText.isNotBlank()) {
                                onAddComment(newCommentText)
                                newCommentText = ""
                            }
                        },
                        enabled = newCommentText.isNotBlank()
                    ) {
                        Icon(
                            Icons.Rounded.Send,
                            contentDescription = "Send Reply",
                            tint = if (newCommentText.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                        )
                    }
                }
            }
        }
    }
}

