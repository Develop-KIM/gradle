/*
 * Copyright 2020 the original author or authors.
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

package gradlebuild.performance

import groovy.transform.CompileStatic
import groovy.transform.MapConstructor

// Modify this class with care, see class org.gradle.performance.results.PerformanceTestExecutionResult
//
// This is the output of the (cacheable) PerformanceTest task, so it must contain only data that is a pure function of
// the task inputs: the identity of the scenarios that were exercised. The producing build's teamCityBuildId, the web
// URL derived from it, and the pass/fail status/testFailure are deliberately NOT stored here - they are build-specific
// and would be replayed stale onto an unrelated build on a build-cache hit. The report derives the TeamCity build from
// the `org.gradle.performance.dependencyBuildIds` system property and the verdict from the performance database.
@MapConstructor
@CompileStatic
class ScenarioBuildResultData {
    String scenarioName
    String scenarioClass
    String testProject
    String agentName
    String agentUrl
}
