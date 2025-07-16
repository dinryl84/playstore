package com.bnhs.quizigsig

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlin.system.exitProcess

class MainActivity : ComponentActivity() {
    private var backgroundRunCount = 0
    private var webViewReference: WebView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            QuizApp(onWebViewCreated = { webView -> webViewReference = webView })
        }
    }

    override fun onPause() {
        super.onPause()
        backgroundRunCount++

        if (backgroundRunCount == 1) {
            showFirstWarningDialog(this)
        } else if (backgroundRunCount in 2..4) {
            showMidWarningDialog(this, backgroundRunCount)
        } else if (backgroundRunCount == 5) {
            autoSubmitAndExit()
        }
    }

    private fun showFirstWarningDialog(context: Context) {
        AlertDialog.Builder(context)
            .setTitle("Warning")
            .setMessage("Switching apps may cause you to lose your answers. If you do it again, your answers will be cleared but you can continue. You are allowed up to 5 switches.")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showMidWarningDialog(context: Context, count: Int) {
        val left = 5 - count
        AlertDialog.Builder(context)
            .setTitle("Warning")
            .setMessage("You have switched apps $count time(s). You only have $left more before your form will auto-submit.")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun autoSubmitAndExit() {
        webViewReference?.evaluateJavascript(
            """
            (function() {
                function simulateClick(el) {
                    var evt = new MouseEvent('click', {
                        view: window,
                        bubbles: true,
                        cancelable: true
                    });
                    el.dispatchEvent(evt);
                }

                function findAndClickSubmit() {
                    var spans = document.querySelectorAll('span');
                    for (var i = 0; i < spans.length; i++) {
                        if (spans[i].innerText.trim().toLowerCase() === "submit") {
                            var parent = spans[i];
                            while (parent && parent.tagName !== 'BODY') {
                                if (parent.getAttribute('role') === 'button') {
                                    simulateClick(parent);
                                    return true;
                                }
                                parent = parent.parentElement;
                            }
                        }
                    }
                    return false;
                }

                function findAndClickViewScore() {
                    var spans = document.querySelectorAll('span');
                    for (var i = 0; i < spans.length; i++) {
                        if (spans[i].innerText.trim().toLowerCase().includes("view score")) {
                            var parent = spans[i];
                            while (parent && parent.tagName !== 'BODY') {
                                if (parent.getAttribute('role') === 'button') {
                                    simulateClick(parent);
                                    return true;
                                }
                                parent = parent.parentElement;
                            }
                        }
                    }
                    return false;
                }

                // Submit first
                if (findAndClickSubmit()) {
                    setTimeout(function() {
                        findAndClickViewScore();
                    }, 2000);
                }

                return "✅ Submit and View Score attempted.";
            })();
            """.trimIndent()
        ) { result ->
            println("Result: $result")

            Handler(Looper.getMainLooper()).postDelayed({
                android.os.Process.killProcess(android.os.Process.myPid())
                exitProcess(0)
            }, 10000) // wait 6 seconds to finish actions before exit
        }
    }
}

@Composable
fun QuizApp(onWebViewCreated: (WebView) -> Unit = {}) {
    var quizUrl by remember { mutableStateOf("") }
    var showWebView by remember { mutableStateOf(false) }
    var selectedTime by remember { mutableStateOf(0) }
    var showLinkGroup by remember { mutableStateOf(false) }
    var currentLinkGroup by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var currentGroupTitle by remember { mutableStateOf("") }
    val context = LocalContext.current

    val linkGroups = mapOf(
        2 to mapOf(
            "Video 1" to "https://docs.google.com/forms/d/e/1FAIpQLSczFNGoke5LfgeqmAZ5BSxZLibDRwDIsFfTavl6BNYPvPQt1A/viewform?usp=sharing",
            "Video 2" to "https://docs.google.com/forms/d/e/1FAIpQLSdFp6_Ob7wegyXOQVn39KwB4TDeN3hyr3LLf0pkrl5a8EvPuQ/viewform?usp=sharing",
            "Video 3" to "https://docs.google.com/forms/d/e/1FAIpQLSfLa2lWqUjFP2bO7Lyn3-SBC9YYaaOH3tTFW8KC16rUOOKEzQ/viewform?usp=sharing",
            "Video 4" to "https://docs.google.com/forms/d/e/1FAIpQLSf0notJNktu_sUm4VTDmmYrkC0C_JueyGlrMjNvQZum8LOPwg/viewform?usp=sharing",
            "Video 5" to "https://docs.google.com/forms/d/e/1FAIpQLSegYTCpKGndz_5_ufE0uEJtpX-cDqYLGlKsr3eWwnOTj_tSfA/viewform?usp=sharing",
            "Video 6" to "https://docs.google.com/forms/d/e/1FAIpQLScFGB99TUU1_t9TX1H9BVUJVechcPajMMcFftC9MEQ45T3xog/viewform?usp=sharing",
            "Video 7" to "https://docs.google.com/forms/d/e/1FAIpQLSf0ynq1Z3lInS1yGGrL8OoP-4-kvKyx33yHaVVpgzbVTzGzQQ/viewform?usp=sharing",
            "Video 8" to "https://docs.google.com/forms/d/e/1FAIpQLSdmdkUHwl14AWPIsXk7EE-_ygLzgjXAn9kHXhgoKvAS_Wcy6A/viewform?usp=sharing",
            "Video 9" to "https://docs.google.com/forms/d/e/1FAIpQLSfR_1oRevRsC6ledanV1yD3GQNxjw9aXpeq5LfNXUn60oVv0Q/viewform?usp=sharing",
            "Video 10" to "https://docs.google.com/forms/d/e/1FAIpQLSfUrLHgTNQwAbpfPePC7zMwl5wcs4ol0vLw1eIx6KOQm7bViA/viewform?usp=sharing",
            "Video 11" to "https://docs.google.com/forms/d/e/1FAIpQLSdpjgkvfrKsGkMGEPv7t6BTHCQGNn8MVCqOGBjhPlYMN4Mr8g/viewform?usp=sharing",
            "Video 12" to "https://docs.google.com/forms/d/e/1FAIpQLSfDWhleOpwaBoQdySQqOW-XlKQQwkqBHgYqucOnCHzbdFXvFw/viewform?usp=sharing",
            "Video 13" to "https://docs.google.com/forms/d/e/1FAIpQLSegOD2CQQhfJJheWdRyjZHM1pbVBwF97T6vmbHnzYykUi6W7w/viewform?usp=sharing"

        ),
        10 to mapOf(
            "Video 1" to "https://docs.google.com/forms/d/e/1FAIpQLSfoV0JuzybaPPp5YAKSW4sAkIVp70RpCmEw3yrhP-t0pFuelg/viewform?usp=sharing",
            "Video 2" to "https://docs.google.com/forms/d/e/1FAIpQLScY_XJ2SYTC5AQLwnW-ApiEG73C1FVEpmUgVje_5pEvZhFJ5A/viewform?usp=sharing",
            "Video 3" to "https://docs.google.com/forms/d/e/1FAIpQLSff_vhsx2if8-KC5ZVwhSD78obNR1H4Vh85lFtgfK_QdeBL5Q/viewform?usp=sharing",
            "Video 4" to "https://docs.google.com/forms/d/e/1FAIpQLSezB3JNFPqGnFcB3MqW3mwuotqQGFpninv7AXgkwU2H7xZAsw/viewform?usp=sharing",
            "Video 5" to "https://docs.google.com/forms/d/e/1FAIpQLSeAvM5WWgO6ZW9R64iGTPG7Rhikx9XOYt0jB_BUnEkegSMRPQ/viewform?usp=sharing",
            "Video 6" to "https://docs.google.com/forms/d/e/1FAIpQLSfvPlqzhLWFB7dFDB89ujgnPY-BKUlfvMwMPRx-22st1bi1ZQ/viewform?usp=sharing",
            "Video 7" to "https://docs.google.com/forms/d/e/1FAIpQLSctyWB4yh2lcKYBMVCfRvhz1V1p5FTmthZFzZkEcmF7UP8fQA/viewform?usp=sharing",
            "Video 8" to "https://docs.google.com/forms/d/e/1FAIpQLSefDqe8ttTIiIcAkhfmyDfwzI2LoOKYMiDxZs4sLvoz4f8YOQ/viewform?usp=sharing",
            "Video 9" to "https://docs.google.com/forms/d/e/1FAIpQLSem8jWz5Sy_sjRn386k9ZZkudmwAwNnO1ZZs9xJIso-VFFh0g/viewform?usp=sharing",
            "Video 10" to "https://docs.google.com/forms/d/e/1FAIpQLSdKuk6KP3qFwG2XmH1GDxcctXT4JxB0tgVWnHKfckNEzVo3_Q/viewform?usp=sharing",
            "Video 11" to "https://docs.google.com/forms/d/e/1FAIpQLSd1jd4AqyLJ2D3rW_nRHPdcuR4HlUOt09GEgbl1BftiNn8Wgg/viewform?usp=sharing",
            "Video 12" to "https://docs.google.com/forms/d/e/1FAIpQLSfYx00SIOomehsmZaWbUf3BWHvpGypOPeOm5iw60-cHJF4fGQ/viewform?usp=sharing",
            "Video 13" to "https://docs.google.com/forms/d/e/1FAIpQLSc3dvVpFEIs28_4cqG7A0VvPANFVRwNDf7GGwiAWERRn86Sng/viewform?usp=sharing"
        ),
        20 to mapOf(
            "Video 1" to "https://docs.google.com/forms/d/e/1FAIpQLSdvpWYB675LLAX5DSfQVFURnMMkI9LHjbIvIkmamWWqBvXIVQ/viewform?usp=sharing",
            "Video 2" to "https://docs.google.com/forms/d/e/1FAIpQLSezyCMpsVfxYXQzkgd270Y6x6tFfT-NgPcLojInzThU4dh_xA/viewform?usp=sharing",
            "Video 3" to "https://docs.google.com/forms/d/e/1FAIpQLSfYGJ2dyR254AZM1Zb029KbfM3rdkhL_zbLA6MSy_d5o3lQQA/viewform?usp=sharing",
            "Video 4" to "https://docs.google.com/forms/d/e/1FAIpQLSf7wAXiP1F5Q4_YZxWpZSVzZg_EtdWMxLl9NQu_kHOES1w_kQ/viewform?usp=sharing",
            "Video 5" to "https://docs.google.com/forms/d/e/1FAIpQLSdM0f8HH5kjVXZxgzSAmWPhswLgeyqjqK14wq_K9XBXItZU_A/viewform?usp=sharing",
            "Video 6" to "https://docs.google.com/forms/d/e/1FAIpQLSf-jIGvmqd3hZ24vBpGSc6luzNICChl-TGY6NJa8GhHKoBnTg/viewform?usp=sharing",
            "Video 7" to "https://docs.google.com/forms/d/e/1FAIpQLScnDM4-2fuDLB_mkGMCxLvKqBkHqLEIxu-n9FQxek3LOb8AZQ/viewform?usp=sharing",
            "Video 8" to "https://docs.google.com/forms/d/e/1FAIpQLSeNyrQDWw3QSTP850n_eElOm8ad7zEHGhqR1AFn4Ioio1sF3A/viewform?usp=sharing",
            "Video 9" to "https://docs.google.com/forms/d/e/1FAIpQLSe40w-KyhUMvrkSxNzjfWKYlN2rqPO2hkZYJgoGpfl0BpALxw/viewform?usp=sharing",
            "Video 10" to "https://docs.google.com/forms/d/e/1FAIpQLSei1kVdFKj7oqt4ODjbBVSCIhZkdtT4A69Rwy6cgagJzf_Nig/viewform?usp=sharing",
            "Video 11" to "https://docs.google.com/forms/d/e/1FAIpQLScm9PjglmE5FZJ_ABpBgrNy6nFLabb8QjycC8UV0CGREcCang/viewform?usp=sharing",
            "Video 12" to "https://docs.google.com/forms/d/e/1FAIpQLSdFnClgDMx7-9rhXNgKnaWFPT4vjZvG-a2d_f3liz8T-bZ8Qw/viewform?usp=sharing",
            "Video 13" to "https://docs.google.com/forms/d/e/1FAIpQLSdRfZ64L6jKV6k04TfyN-sKelYHQgwg2ls4oWU-0tn8ZjG9Aw/viewform?usp=sharing",
            "Video 14" to "https://docs.google.com/forms/d/e/1FAIpQLSett7LSSBzyg-Ar5ac-nkGP5Cbjen2DrfPjQX_lCV2e-IGYpw/viewform?usp=sharing"
        ),
        50 to mapOf(
            "Video 1" to "https://docs.google.com/forms/d/e/1FAIpQLSfLAOTbaNhEpnpWsu-D87Mvu3-BHHriYQ-NVS-xCzFQY2aGeA/viewform?usp=sharing",
            "Video 2" to "https://docs.google.com/forms/d/e/1FAIpQLSd4YnFQ5PNaqSapcLRZrL7VisfPQU9WuLyF69QVKRB0HgUeYQ/viewform?usp=sharing",
            "Video 3" to "https://docs.google.com/forms/d/e/1FAIpQLSdXHjgfRo1t9eUWMmTbxsO58QJlozZWvVdhHO-IZlczz5onbQ/viewform?usp=sharing",
            "Video 4" to "https://docs.google.com/forms/d/e/1FAIpQLSeAZfg1-Hhe73-DBfFQaYitR642uakKYo95M8MdrDOC-Ljqag/viewform?usp=sharing",
            "Video 5" to "https://docs.google.com/forms/d/e/1FAIpQLSfcMj3lXDz4pHWu2iBkO8Fmy3A1td77hzD5GlzBuxqV-TgGJw/viewform?usp=sharing",
            "Video 6" to "https://docs.google.com/forms/d/e/1FAIpQLSf3d3O3lf9MXvfNdpw2FxdAowNxTL_4AFHSRonjB7jkkyHRQQ/viewform?usp=sharing",
            "Video 7" to "https://docs.google.com/forms/d/e/1FAIpQLSfHf2eAmB-w0isTVckWOzElFFLnvOryUsu0j8S9LKL9PH3QDg/viewform?usp=sharing",
            "Video 8" to "https://docs.google.com/forms/d/e/1FAIpQLSeo5_FC4nrsLLn1SmtPbMnCtELR2pLYX2m0Kpb7XLamxzhU5A/viewform?usp=sharing",
            "Video 9" to "https://docs.google.com/forms/d/e/1FAIpQLScW_shqY49YRzXeiUQmhbkoM5oJ1tYJs02MFUtXbEqLKwGaPA/viewform?usp=sharing",
            "Video 10" to "https://docs.google.com/forms/d/e/1FAIpQLSfYA0uKoMpRAWhneiDndx9yd01h7Uh8blsO39j0luzAGSH_rw/viewform?usp=sharing",
            "Video 11" to "https://docs.google.com/forms/d/e/1FAIpQLScF_RfdJD4BjWTzqoSPHT97Z8htLILY9CQtzuvKHYvdReYVGA/viewform?usp=sharing",
            "Video 12" to "https://docs.google.com/forms/d/e/1FAIpQLSfjVPL7bd6HT3IBtC_vYXQyVl9PmLo3Fp-mRXR6fxo2Ww5X7Q/viewform?usp=sharing",
            "Video 13" to "https://docs.google.com/forms/d/e/1FAIpQLSdIMXUPvUmJJLQeD0u-L59MG-a1liH4SgACmEzXQTwzebtymg/viewform?usp=sharing"

        )
    )

    val allQuarters = mapOf(
        "1st Quarter" to mapOf(
            "1st Summative (Q1)" to "https://docs.google.com/forms/d/e/1FAIpQLSdSN_smMADh1JZK25S00ogXF8nmMwJAMPGk4r1EhHvVZSOTHw/viewform?usp=sharing",
            "2nd Summative (Q1)" to "https://docs.google.com/forms/d/e/1FAIpQLSdi0vOrHGesrW8qS8p7E4a6IUm3GjtgzYxQEelpn_ieZxVH3Q/viewform?usp=sharing",
            "3rd Summative (Q1)" to "https://docs.google.com/forms/d/e/1FAIpQLSeEVhas5G1kzHvHBPbQ-_PnRwV_wadh7422AjdiwL4RERujpw/viewform?usp=sharing",
            "4th Summative (Q1)" to "https://docs.google.com/forms/d/e/1FAIpQLSegOD2CQQhfJJheWdRyjZHM1pbVBwF97T6vmbHnzYykUi6W7w/viewform?usp=sharing"
        ),
        "2nd Quarter" to mapOf(
            "1st Summative (Q2)" to "https://docs.google.com/forms/d/e/1FAIpQLSdFmxAT2DPefteKqmVvkRt4_04Bl8HYUhfLwPXHZceQxvzOTA/viewform?usp=sharing",
            "2nd Summative (Q2)" to "https://docs.google.com/forms/d/e/1FAIpQLScuG5CpwcjOLbjYRZQoJCyhSkb25r6YHlKxMiVNWkUNT-gpoA/viewform?usp=sharing",
            "3rd Summative (Q2)" to "https://docs.google.com/forms/d/e/1FAIpQLSdlKp7brg0hCWz5k02byCZP7hc63FrfJacFH_O-cBCE6-gJQQ/viewform?usp=sharing",
            "4th Summative (Q2)" to "https://docs.google.com/forms/d/e/1FAIpQLSc3dvVpFEIs28_4cqG7A0VvPANFVRwNDf7GGwiAWERRn86Sng/viewform?usp=sharing"
        ),
        "3rd Quarter" to mapOf(
            "1st Summative (Q3)" to "https://docs.google.com/forms/d/e/1FAIpQLScDXcDTakIJ8HRPMvzMLRmkcU4gul-4F1aG3AfM9i2RU2wePA/viewform?usp=sharing",
            "2nd Summative (Q3)" to "https://docs.google.com/forms/d/e/1FAIpQLScv1rWi1D9AvI09LRmf6zLffabwHUPRgWLVAJv1sjyRtS9JWw/viewform?usp=sharing",
            "3rd Summative (Q3)" to "https://docs.google.com/forms/d/e/1FAIpQLSfphHxprn5JGmG3cX8jhXGut6P8FnJdufkN25cb-BcAWzFchA/viewform?usp=sharing",
            "4th Summative (Q3)" to "https://docs.google.com/forms/d/e/1FAIpQLSett7LSSBzyg-Ar5ac-nkGP5Cbjen2DrfPjQX_lCV2e-IGYpw/viewform?usp=sharing"
        ),
        "4th Quarter" to mapOf(
            "1st Summative (Q4)" to "https://docs.google.com/forms/d/e/1FAIpQLSdnlGmG4CuX50kYmfN6nHVfSKzEALZhNB3JxhqmpHgIPJn9dw/viewform?usp=sharing",
            "2nd Summative (Q4)" to "https://docs.google.com/forms/d/e/1FAIpQLScxxhP1OxkOazRrXDNzUFZ9oLaNyPeZERcagkut24c1uAhF2A/viewform?usp=sharing",
            "3rd Summative (Q4)" to "https://docs.google.com/forms/d/e/1FAIpQLSf7_O83b2NXVH-MH-AmjtDB_edDxAEmNKNLF_Zgz_wuE9N_Ig/viewform?usp=sharing",
            "4th Summative (Q4)" to "https://docs.google.com/forms/d/e/1FAIpQLSdIMXUPvUmJJLQeD0u-L59MG-a1liH4SgACmEzXQTwzebtymg/viewform?usp=sharing"
        )
    )

    // Root Column: Not scrollable. Holds the fixed title and the dynamic content area.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Part 1: Fixed Title and Subtitle
        Text("quiZigsig", fontSize = 24.sp, modifier = Modifier.padding(bottom = 4.dp))
        Text("Coded by AI compiled by Dinryl P. Basigsig", fontSize = 16.sp)
        Spacer(modifier = Modifier.height(18.dp))

        // Part 2: Dynamic Content Area
        if (showWebView) {
            // Display the WebView, making it fill the remaining available space
            WebViewScreen(
                url = quizUrl,
                timeLimit = selectedTime,
                onQuizCompleted = {
                    showWebView = false
                    showLinkGroup = false
                },
                onWebViewCreated = onWebViewCreated,
                modifier = Modifier.weight(1f) // Use weight to fill space
            )
        } else {
            // Display the selection buttons inside a new scrollable column
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!showLinkGroup) {
                    Text("Choose a quarter:", fontSize = 18.sp, modifier = Modifier.padding(bottom = 8.dp))

                    val timeLimits = listOf(2, 10, 20, 50, 60)
                    val buttonLabels = mapOf(
                        2 to "Q1_F", 10 to "Q2_F", 20 to "Q3_F", 50 to "Q4_F", 60 to "Summative"
                    )

                    timeLimits.chunked(3).forEach { row ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            row.forEach { time ->
                                Button(
                                    onClick = {
                                        selectedTime = time
                                        if (time == 60) {
                                            showLinkGroup = true
                                            currentLinkGroup = allQuarters.values.fold(emptyMap()) { acc, map -> acc + map }
                                            currentGroupTitle = "Summative Quiz Selection"
                                        } else {
                                            showLinkGroup = true
                                            currentLinkGroup = linkGroups[time] ?: emptyMap()
                                            currentGroupTitle = "Quiz Options (${buttonLabels[time]})"
                                        }
                                    },
                                    modifier = Modifier.padding(4.dp)
                                ) {
                                    Text(buttonLabels[time] ?: "$time min")
                                }
                            }
                        }
                    }
                } else {
                    Text(currentGroupTitle, fontSize = 20.sp, modifier = Modifier.padding(vertical = 8.dp))

                    currentLinkGroup.forEach { (name, link) ->
                        Button(
                            onClick = {
                                quizUrl = link
                                showWebView = true
                                showLinkGroup = false
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp)
                        ) {
                            Text(name)
                        }
                    }

                    Button(
                        onClick = { showLinkGroup = false },
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text("Back")
                    }
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewScreen(
    url: String,
    timeLimit: Int,
    onQuizCompleted: () -> Unit,
    onWebViewCreated: (WebView) -> Unit = {},
    modifier: Modifier = Modifier // Add modifier parameter
) {
    val context = LocalContext.current
    val totalTimeMillis = timeLimit * 60 * 1000L
    var startTime by rememberSaveable { mutableStateOf(System.currentTimeMillis()) }
    var remainingTime by remember { mutableStateOf((totalTimeMillis / 1000).toInt()) }

    if (timeLimit == 60) {
        LaunchedEffect(key1 = startTime) {
            while (true) {
                val elapsed = System.currentTimeMillis() - startTime
                val timeLeftMillis = totalTimeMillis - elapsed
                remainingTime = (timeLeftMillis / 1000).toInt()

                if (remainingTime <= 60 && remainingTime > 59) {
                    showOneMinuteLeftDialog(context)
                }

                if (remainingTime <= 0) {
                    showTimeUpDialog(context)
                    break
                }

                kotlinx.coroutines.delay(1000)
            }
        }
    }

    // Apply the passed-in modifier to this Column
    Column(modifier = modifier.fillMaxWidth()) {
        if (timeLimit == 60) {
            Text(
                "Time Remaining: ${remainingTime / 60}m ${remainingTime % 60}s",
                fontSize = 18.sp,
                modifier = Modifier.padding(8.dp)
            )
        }

        AndroidView(factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.loadsImagesAutomatically = true
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true

                CookieManager.getInstance().setAcceptCookie(true)
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView,
                        request: WebResourceRequest
                    ): Boolean {
                        return false
                    }
                }
                loadUrl(url)
                onWebViewCreated(this)
            }
        }, modifier = Modifier.fillMaxSize())
    }
}

fun showOneMinuteLeftDialog(context: Context) {
    AlertDialog.Builder(context)
        .setTitle("Time Alert")
        .setMessage("Only 1 minute remaining!")
        .setPositiveButton("OK", null)
        .show()
}

fun showTimeUpDialog(context: Context) {
    AlertDialog.Builder(context)
        .setTitle("Time's Up")
        .setMessage("The time for the quiz has expired.")
        .setPositiveButton("OK", null)
        .show()
}