/*
 * Designed and developed by 2022 skydoves (Jaewoong Eum)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.toandtpro.chatgpt.benchmark

import androidx.benchmark.macro.BaselineProfileMode.Disable
import androidx.benchmark.macro.BaselineProfileMode.Require
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupMode.COLD
import androidx.benchmark.macro.StartupMode.HOT
import androidx.benchmark.macro.StartupMode.WARM
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Run this benchmark from Studio to see startup measurements, and captured system traces
 * for investigating your app's performance from a cold state.
 */
@RunWith(AndroidJUnit4ClassRunner::class)
class ColdStartupBenchmark : AbstractStartupBenchmark(COLD)

/**
 * Run this benchmark from Studio to see startup measurements, and captured system traces
 * for investigating your app's performance from a warm state.
 */
@RunWith(AndroidJUnit4ClassRunner::class)
class WarmStartupBenchmark : AbstractStartupBenchmark(WARM)

/**
 * Run this benchmark from Studio to see startup measurements, and captured system traces
 * for investigating your app's performance from a hot state.
 */
@RunWith(AndroidJUnit4ClassRunner::class)
class HotStartupBenchmark : AbstractStartupBenchmark(HOT)

/**
 * Base class for benchmarks with different startup modes.
 * Enables app startups from various states of baseline profile or [CompilationMode]s.
 */
abstract class AbstractStartupBenchmark(private val startupMode: StartupMode) {
  @get:Rule
  val benchmarkRule = MacrobenchmarkRule()

  @Test
  fun startupNoCompilation() = startup(CompilationMode.None())

  @Test
  fun startupBaselineProfileDisabled() = startup(
    CompilationMode.Partial(baselineProfileMode = Disable, warmupIterations = 1)
  )

  @Test
  fun startupBaselineProfile() = startup(CompilationMode.Partial(baselineProfileMode = Require))

  @Test
  fun startupFullCompilation() = startup(CompilationMode.Full())

  private fun startup(compilationMode: CompilationMode) = benchmarkRule.measureRepeated(
    packageName = "com.toandtpro.chatgpt",
    metrics = listOf(StartupTimingMetric()),
    compilationMode = compilationMode,
    iterations = 5,
    startupMode = startupMode,
    setupBlock = {
      pressHome()
    }
  ) {
    startActivityAndWait()
  }
}
