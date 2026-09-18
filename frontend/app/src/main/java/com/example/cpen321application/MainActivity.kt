package com.example.cpen321application

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.cpen321application.ui.theme.CPEN321ApplicationTheme
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import kotlin.math.abs
import android.app.Activity
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.TextField
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CPEN321ApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AppNavHost(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun AppNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = "home",
        modifier = modifier
    ) {
        composable("home") { HomeScreen(navController) }
        composable("login") { LoginScreen() }
        composable("pixelart") { PixelArtScreen() }
        composable("timer") { TimerScreen() }
    }
}

@Composable
fun HomeScreen(navController: NavHostController) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Button(onClick = { navController.navigate("login") }, modifier = Modifier.fillMaxWidth()) {
            Text("Login + Server")
        }
        Button(onClick = { navController.navigate("pixelart") }, modifier = Modifier.fillMaxWidth()) {
            Text("Live Updates")
        }
        Button(onClick = { navController.navigate("timer") }, modifier = Modifier.fillMaxWidth()) {
            Text("Timer")
        }
    }
}

@Composable
fun LoginScreen() {

    // get from backend
    var serverIp by remember { mutableStateOf("")}
    var serverTime by remember { mutableStateOf("")}
    var myName by remember { mutableStateOf("")}

    var userName by remember { mutableStateOf("") }
    var statusText by remember { mutableStateOf("Not signed in yet") }

    // local values
    var clientTime by remember { mutableStateOf("") }
    var clientIp by remember { mutableStateOf("") }

    val coroutineScope = rememberCoroutineScope()

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = {
            coroutineScope.launch {
                statusText = "Signing in..."
                try {
                    val googleIdOption = GetGoogleIdOption.Builder()
                        .setServerClientId(BuildConfig.GOOGLE_CLIENT_ID)
                        .setFilterByAuthorizedAccounts(false)
                        .build()

                    val request = GetCredentialRequest.Builder()
                        .addCredentialOption(googleIdOption)
                        .build()

                    val credentialManager = CredentialManager.create(context)
                    val result = credentialManager.getCredential(
                        context = context as Activity,
                        request = request
                    )

                    val googleIdTokenCredential =
                        GoogleIdTokenCredential.createFrom(result.credential.data)
                    val givenName = googleIdTokenCredential.givenName ?: ""
                    val familyName = googleIdTokenCredential.familyName ?: ""
                    userName = "$givenName $familyName"

                    statusText = "Calling backend..."
                    val baseUrl = BuildConfig.API_BASE_URL.trimEnd('/')

                    serverIp = fetchJsonValue("$baseUrl/api/server-ip", "ip")
                    serverTime = fetchJsonValue("$baseUrl/api/server-time", "time")

                    val nameBody = withContext(Dispatchers.IO) {
                        val connection = URL("$baseUrl/api/my-name").openConnection() as HttpURLConnection
                        connection.connectTimeout = 5000
                        connection.readTimeout = 5000
                        connection.inputStream.bufferedReader().use { it.readText() }
                    }
                    val nameJson = JSONObject(nameBody)
                    myName = "${nameJson.getString("firstName")} ${nameJson.getString("lastName")}"

                    clientTime = getFormattedLocalTime()
                    clientIp = getLocalIpAddress()

                    statusText = "Done"
                } catch (e: Exception) {
                    statusText = "Error: ${e.message}"
                }
            }
        }) {
            Text("Sign in with Google")
        }

        if (statusText.isNotEmpty()) {
            Text(statusText)
        }
        // Display the all values
        if (userName.isNotEmpty()) {
            Text("Server IP address:", style = MaterialTheme.typography.labelMedium)
            Text(serverIp)

            Text("Client IP address:", style = MaterialTheme.typography.labelMedium)
            Text(clientIp)

            Text("Server local time:", style = MaterialTheme.typography.labelMedium)
            Text(serverTime)

            Text("Client local time:", style = MaterialTheme.typography.labelMedium)
            Text(clientTime)

            Text("Developer name:", style = MaterialTheme.typography.labelMedium)
            Text(myName)

            Text("Logged-in user:", style = MaterialTheme.typography.labelMedium)
            Text(userName)
        }
    }
}
private suspend fun fetchJsonValue(url: String, key: String): String =
    withContext(Dispatchers.IO) {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 5000
        connection.readTimeout = 5000
        val body = connection.inputStream.bufferedReader().use { it.readText() }
        JSONObject(body).getString(key)
    }

@SuppressLint("DefaultLocale")
private fun getFormattedLocalTime(): String {
    val now = java.util.Calendar.getInstance()

    // Calendar gives you each piece of the time separately
    val hours = String.format("%02d", now.get(java.util.Calendar.HOUR_OF_DAY))
    val minutes = String.format("%02d", now.get(java.util.Calendar.MINUTE))
    val seconds = String.format("%02d", now.get(java.util.Calendar.SECOND))

    // ZONE_OFFSET is the base timezone offset in milliseconds (e.g., PST = -8 hours)
    // DST_OFFSET is the daylight saving adjustment (0 or +1 hour)
    // Adding them gives the total offset from UTC
    val offsetMs = now.get(java.util.Calendar.ZONE_OFFSET) + now.get(java.util.Calendar.DST_OFFSET)
    val offsetMin = offsetMs / 60000  // convert ms to minutes
    val sign = if (offsetMin >= 0) "+" else "-"
    val absOffset = abs(offsetMin)
    val offHours = String.format("%02d", absOffset / 60)
    val offMins = String.format("%02d", absOffset % 60)

    return "$hours:$minutes:$seconds GMT$sign$offHours:$offMins"
}

private fun getLocalIpAddress(): String {
    try {
        val interfaces = java.net.NetworkInterface.getNetworkInterfaces()

        while (interfaces.hasMoreElements()) {
            val networkInterface = interfaces.nextElement()
            val addresses = networkInterface.inetAddresses

            while (addresses.hasMoreElements()) {
                val address = addresses.nextElement()

                if (!address.isLoopbackAddress && address is java.net.Inet4Address) {
                    return address.hostAddress ?: "Unknown"
                }
            }
        }
    } catch (e: Exception) {
        return "Error: ${e.message}"
    }
    return "Unknown"
}

@Composable
fun PixelArtScreen() {

    val grid = remember {
        Array(16) { Array(16) { mutableStateOf(Color.White) } }
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val client = OkHttpClient()
            val request = Request.Builder()
                .url("ws://10.0.2.2:3000")
                .build()

            client.newWebSocket(request, object : WebSocketListener() {
                override fun onMessage(webSocket: okhttp3.WebSocket, text: String) {
                    try {
                        val json = JSONObject(text)
                        val x = json.getInt("x")
                        val y = json.getInt("y")
                        val color = json.getString("color")

                        grid[y][x].value = Color(android.graphics.Color.parseColor(color))
                    } catch (_: Exception) {
                        // skip any bad msgs
                    }
                }
            })
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Pixel Art:", style = MaterialTheme.typography.titleMedium)

        Spacer(modifier = Modifier.height(16.dp))

        // rows
        for (y in 0 until 16) {
            Row {
                // columns
                for (x in 0 until 16) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(grid[y][x].value)
                    )
                }
            }
        }
    }
}

@Composable
fun TimerScreen() {

    var minutesInput by remember { mutableStateOf("") }
    var secondsInput by remember { mutableStateOf("") }
    var timeLeft by remember { mutableStateOf<Int?>(null) }
    var isRunning by remember { mutableStateOf(false) }

    var showSurprise by remember { mutableStateOf(false) }
    var tickerInput by remember { mutableStateOf("") }
    var stockName by remember { mutableStateOf("") }
    var stockPrice by remember { mutableStateOf("") }
    var stockChange by remember { mutableStateOf("") }
    var stockError by remember { mutableStateOf<String?>(null) }
    var isSearching by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(isRunning, timeLeft) {
        if (isRunning && timeLeft != null && timeLeft!! > 0) {
            delay(1000L)
            timeLeft = timeLeft!! - 1
        } else if (isRunning && timeLeft == 0) {
            isRunning = false
            showSurprise = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Set Time", style = MaterialTheme.typography.titleMedium)

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = minutesInput,
                onValueChange = { minutesInput = it.filter { ch -> ch.isDigit() } },
                label = { Text("Min") },
                modifier = Modifier.width(80.dp)
            )

            Text(":")

            TextField(
                value = secondsInput,
                onValueChange = { secondsInput = it.filter { ch -> ch.isDigit() } },
                label = { Text("Sec") },
                modifier = Modifier.width(80.dp)
            )
        }


        Button(
            onClick = {
                val mins = minutesInput.toIntOrNull() ?: 0
                val secs = secondsInput.toIntOrNull() ?: 0
                val total = mins * 60 + secs
                if (total > 0) {
                    timeLeft = total
                    isRunning = true
                }
            },
            enabled = !isRunning
        ) {
            Text("Start")
        }

        if (timeLeft != null) {
            val displayMin = timeLeft!! / 60
            val displaySec = timeLeft!! % 60
            Text(
                text = String.format("%02d:%02d", displayMin, displaySec),
                style = MaterialTheme.typography.displayMedium
            )
        }

        if (showSurprise) {
            Text("Stock Lookup (e.g. APPL, MSFT, etc.)", style = MaterialTheme.typography.titleMedium)

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = tickerInput,
                    onValueChange = { tickerInput = it.uppercase() },
                    label = { Text("Ticker") },
                    modifier = Modifier.width(140.dp)
                )

                Button(
                    onClick = {
                        if (tickerInput.isNotBlank()) {
                            isSearching = true
                            stockError = null
                            coroutineScope.launch {
                                try {
                                    val result = fetchStockQuote(tickerInput.trim())
                                    stockName = result.first
                                    stockPrice = result.second
                                    stockChange = result.third
                                } catch (e: Exception) {
                                    stockError = "Error: ${e.javaClass.simpleName}: ${e.message}"
                                    stockName = ""
                                    stockPrice = ""
                                    stockChange = ""
                                } finally {
                                    isSearching = false
                                }
                            }
                        }
                    },
                    enabled = !isSearching
                ){
                    Text(if (isSearching) "..." else "Search")
                }
            }
            if (stockError != null) {
                Text(stockError!!, color = MaterialTheme.colorScheme.error)
            }

            if (stockName.isNotEmpty()) {
                Text(stockName, style = MaterialTheme.typography.titleLarge)
                Text(stockPrice, style = MaterialTheme.typography.displaySmall)
                Text(
                    stockChange,
                    color = if (stockChange.startsWith("-")) Color.Red else Color(0xFF4CAF50)
                )
            }
        }
    }
}

private suspend fun fetchStockQuote(ticker: String): Triple<String, String, String> =
    withContext(Dispatchers.IO) {
        val url = "https://financialmodelingprep.com/stable/quote?symbol=$ticker&apikey=${BuildConfig.FMP_API_KEY}"
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
        val body = connection.inputStream.bufferedReader().use { it.readText() }

        val array = org.json.JSONArray(body)
        if (array.length() == 0) throw Exception("No data")

        val stock = array.getJSONObject(0)
        val name = stock.getString("name")
        val price = String.format("$%.2f", stock.getDouble("previousClose"))
        val changePct = stock.getDouble("changePercentage")
        val changeStr = String.format("%+.2f%%", changePct)

        Triple(name, price, changeStr)
    }

private suspend fun fetchHealthStatus(apiBaseUrl: String): String = withContext(Dispatchers.IO) {
    val healthUrl = "${apiBaseUrl.trimEnd('/')}/health"
    try {
        val connection = (URL(healthUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 5_000
            readTimeout = 5_000
        }
        when (val code = connection.responseCode) {
            HttpURLConnection.HTTP_OK -> {
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                "Backend healthy ($healthUrl): $body"
            }
            else -> {
                val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() }
                "Backend error ($healthUrl): HTTP $code${errorBody?.let { " — $it" } ?: ""}"
            }
        }
    } catch (e: Exception) {
        "Backend unreachable ($healthUrl): ${e.message ?: e.javaClass.simpleName}"
    }
}