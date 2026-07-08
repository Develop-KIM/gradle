/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.performance.results

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

/**
 * Represents a result of single performance test execution, deserialized from JSON result.
 *
 * @see gradlebuild.performance.ScenarioBuildResultData
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class PerformanceTestExecutionResult {
    public static final int FLAKINESS_DETECTION_THRESHOLD = 99
    // Only the scenario identity is carried in the (cacheable) bucket output. The producing build id, web URL and
    // pass/fail status are intentionally absent - the report derives the TeamCity build from the
    // `org.gradle.performance.dependencyBuildIds` system property and the verdict from the performance database.
    String scenarioName
    String scenarioClass
    String testProject

    PerformanceExperiment getPerformanceExperiment() {
        new PerformanceExperiment(getTestProject(), new PerformanceScenario(getScenarioClass(), getScenarioName()))
    }
}
