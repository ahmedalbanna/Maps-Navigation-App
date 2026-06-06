package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    composeTestRule.setContent { MyApplicationTheme { Greeting("Maps & Navigation") } }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}

@androidx.compose.runtime.Composable
fun Greeting(name: String) {
  androidx.compose.foundation.layout.Box(
    modifier = androidx.compose.ui.Modifier
      .fillMaxSize()
      .background(androidx.compose.ui.graphics.Color(0xFF0C101B)),
    contentAlignment = androidx.compose.ui.Alignment.Center
  ) {
    androidx.compose.material3.Text(
      text = "مرحباً بك في تطبيق الخرائط والملاحة! ($name)",
      color = androidx.compose.ui.graphics.Color.White,
      fontSize = 16.sp,
      fontWeight = FontWeight.Bold
    )
  }
}
