/*
 * Copyright 2019 the original author or authors.
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

package org.gradle.performance.results.report

import org.gradle.performance.ResultSpecification
import org.gradle.performance.results.PerformanceReportScenario
import org.gradle.performance.results.PerformanceReportScenarioHistoryExecution
import org.gradle.performance.results.PerformanceTestExecutionResult

class DefaultPerformanceExecutionDataProviderTest extends ResultSpecification {

    private static final String COMMIT = 'commit-under-test'

    def 'identifies this pipeline\'s executions by build id (CI) or commit (local) and derives the measured verdict from them'() {
        given:
        // The result JSON no longer carries a build id - only the scenario identity (+ status, used by cross-build).
        def teamCityExecution = new PerformanceTestExecutionResult(scenarioName: 'x', scenarioClass: 'org.example.C', testProject: 'p', status: 'SUCCESS')
        // The DB has a regressed row produced by this pipeline's bucket, plus a row from an unrelated build/commit.
        def pipelineRow = regressedExecution('114134082', COMMIT)
        def foreignRow = regressedExecution('114123152', 'other-commit')

        when:
        def scenario = new PerformanceReportScenario([teamCityExecution], [pipelineRow, foreignRow], false, pipelineBuildIds as Set, currentCommit)

        then:
        scenario.currentExecutions*.teamCityBuildId == expectedCurrentBuildIds
        scenario.regressedByMeasurement == expectedRegressed

        where:
        desc                  | pipelineBuildIds | currentCommit | expectedCurrentBuildIds | expectedRegressed
        'CI, fresh run'       | ['114134082']    | 'ignored'     | ['114134082']           | true
        'CI, build-cache hit' | ['999999']       | 'ignored'     | []                      | false
        'local, by commit'    | []               | COMMIT        | ['114134082']           | true
    }

    def 'cross-version verdict comes from the DB, while cross-build still uses the recorded status'() {
        given:
        def failed = new PerformanceTestExecutionResult(scenarioName: 'x', scenarioClass: 'org.example.C', testProject: 'p', status: 'FAILURE')
        def row = regressedExecution('build-1', COMMIT)

        when:
        def scenario = new PerformanceReportScenario([failed], [row], crossBuild, ['build-1'] as Set, COMMIT)

        then:
        scenario.regressed == statusBasedRegressed              // status-based signal (used by the cross-build report)
        scenario.regressedByMeasurement == measurementRegressed // DB confidence (used by the cross-version report)

        where:
        crossBuild | statusBasedRegressed | measurementRegressed
        false      | true                 | true   // cross-version: both agree here
        true       | true                 | false  // cross-build: only the status-based signal fires (DB model is guarded off)
    }

    private PerformanceReportScenarioHistoryExecution regressedExecution(String teamCityBuildId, String commitId) {
        // current markedly slower than baseline with high confidence -> confidentToSayWorse() == true
        return new PerformanceReportScenarioHistoryExecution(new Date().getTime(), teamCityBuildId, commitId, measuredOperationList([1, 1, 1]), measuredOperationList([2, 2, 2]))
    }
}
