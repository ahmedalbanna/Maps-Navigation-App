package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.viewmodel.MapViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("الخرائط والملاحة", appName)
  }

  @Test
  fun `verify MapViewModel initializes with presets`() {
    val application = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = MapViewModel(application)

    // Check default starting camera is Riyadh
    assertEquals(24.7136, viewModel.centerLat.value, 0.001)
    assertEquals(46.6753, viewModel.centerLon.value, 0.001)

    // Check search functionality
    viewModel.updateSearchQuery("الرياض")
    val suggestions = viewModel.suggestions.value
    assertTrue(suggestions.isNotEmpty())

    val matchesRiyadh = suggestions.any {
      when (it) {
        is MapViewModel.SearchResult.CityResult -> it.city.name == "الرياض"
        else -> false
      }
    }
    assertTrue(matchesRiyadh)
  }
}
