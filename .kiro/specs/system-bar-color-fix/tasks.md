# Implementation Plan

- [ ] 1. Create SystemBarConfigurator utility class
  - Create new file `app/shared/src/androidMain/kotlin/ui/SystemBarConfigurator.kt`
  - Implement Color to Android Color Int conversion function
  - Implement SystemBarStyle creation functions for status bar and navigation bar
  - Add documentation for each function
  - _Requirements: 4.1, 4.2_

- [ ]* 1.1 Write property test for Color conversion
  - **Property 1: Theme-to-SystemBar Color Consistency**
  - **Validates: Requirements 1.1, 1.2, 1.3, 1.5**

- [ ]* 1.2 Write unit tests for SystemBarConfigurator
  - Test color conversion with various color values (transparent, opaque, semi-transparent)
  - Test SystemBarStyle creation for dark and light modes
  - Test edge cases (null colors, invalid values)
  - _Requirements: 4.1, 4.2_

- [ ] 2. Create SystemBarColors data class
  - Create data class in `app/shared/src/androidMain/kotlin/ui/SystemBarColors.kt`
  - Implement factory method to create from ColorScheme
  - Add companion object with default values
  - _Requirements: 4.1_

- [ ]* 2.1 Write unit tests for SystemBarColors
  - Test factory method creates correct colors from ColorScheme
  - Test dark and light mode handling
  - Test default values
  - _Requirements: 4.1_

- [ ] 3. Update ConfigureSystemBars to use SystemBarConfigurator
  - Modify `app/shared/src/androidMain/kotlin/ui/SystemBars.android.kt`
  - Replace inline color conversion with SystemBarConfigurator.toAndroidColor()
  - Replace inline SystemBarStyle creation with SystemBarConfigurator functions
  - Simplify the DisposableEffect logic
  - Ensure navigation bar always uses transparent color
  - _Requirements: 1.4, 4.2_

- [ ]* 3.1 Write property test for navigation bar transparency
  - **Property 2: Navigation Bar Transparency Invariant**
  - **Validates: Requirements 1.4**

- [ ] 4. Simplify MainActivity system bar configuration
  - Modify `app/android/src/main/kotlin/com/xmvisio/app/MainActivity.kt`
  - Remove `configureSystemBars()` function
  - Update `onCreate()` to only call `enableEdgeToEdge()` with default parameters
  - Remove system bar configuration from `onResume()`
  - Add comment explaining that system bar colors are managed by App.kt
  - _Requirements: 2.2, 4.2_

- [ ]* 4.1 Write property test for lifecycle color consistency
  - **Property 3: Lifecycle Color Consistency**
  - **Validates: Requirements 2.1, 2.4**

- [ ] 5. Verify App.kt system bar configuration
  - Review `app/shared/src/commonMain/kotlin/App.kt`
  - Ensure ConfigureSystemBars is called with correct parameters
  - Verify statusBarColor uses MaterialTheme.colorScheme.surfaceContainerLowest
  - Verify navigationBarColor uses Color.Transparent
  - Ensure isDark parameter correctly reflects theme mode
  - _Requirements: 1.1, 1.2, 1.3, 1.4_

- [ ]* 5.1 Write property test for mode switch completeness
  - **Property 4: Mode Switch Completeness**
  - **Validates: Requirements 3.2**

- [ ]* 5.2 Write property test for dynamic theme propagation
  - **Property 5: Dynamic Theme Propagation**
  - **Validates: Requirements 3.3**

- [ ]* 5.3 Write property test for seed color propagation
  - **Property 6: Seed Color Propagation**
  - **Validates: Requirements 3.4**

- [ ] 6. Add error handling for theme loading failures
  - Modify theme loading logic to catch exceptions
  - Implement fallback to default theme settings
  - Add logging for debugging
  - Create getDefaultThemeSettings() helper function
  - _Requirements: 5.4_

- [ ]* 6.1 Write unit test for theme loading error handling
  - Test that theme loading failures fall back to defaults
  - Test that default theme settings are sensible
  - Verify error logging occurs
  - _Requirements: 5.4_

- [ ] 7. Add null safety for Activity context
  - Review ConfigureSystemBars implementation
  - Ensure graceful handling when Activity context is unavailable
  - Add null checks and early returns
  - _Requirements: 4.2_

- [ ]* 7.1 Write property test for ColorScheme calculation consistency
  - **Property 7: ColorScheme Calculation Consistency**
  - **Validates: Requirements 4.3**

- [ ]* 7.2 Write property test for theme change propagation
  - **Property 8: Theme Change Propagation**
  - **Validates: Requirements 4.4**

- [ ]* 7.3 Write property test for Intent recreation consistency
  - **Property 9: Intent Recreation Color Consistency**
  - **Validates: Requirements 5.3**

- [ ] 8. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 9. Integration testing
  - Test app startup with different theme configurations
  - Test theme switching (light/dark, dynamic theme, seed color)
  - Test lifecycle events (background/foreground, rotation)
  - Test special scenarios (notification launch, crash recovery)
  - _Requirements: 1.1, 2.1, 3.2, 3.3, 3.4, 5.1, 5.2, 5.3_

- [ ]* 9.1 Write integration tests for app startup
  - Test system bar colors on first launch
  - Test with different saved theme preferences
  - _Requirements: 1.1_

- [ ]* 9.2 Write integration tests for theme switching
  - Test all theme setting combinations
  - Verify system bar updates correctly
  - _Requirements: 3.2, 3.3, 3.4_

- [ ]* 9.3 Write integration tests for lifecycle events
  - Simulate app going to background and returning
  - Verify no color flickering or inconsistency
  - _Requirements: 2.1_

- [ ] 10. Manual testing on different Android versions
  - Test on Android 12+ with dynamic theme support
  - Test on Android 11 and below without dynamic theme
  - Test on different device manufacturers (Samsung, Pixel, etc.)
  - Verify no regressions in system bar behavior
  - _Requirements: 1.5, 2.1, 3.3_

- [ ] 11. Final checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.
