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

    def 'sorts regressed scenarios first and scenarios without a current-pipeline measurement last'() {
        when:
        List<PerformanceReportScenario> scenarios = [
            improved('b-improved'),
            unknownScenario('c-unknown'),
            regressed('a-regressed')
        ]
        scenarios.sort(DefaultPerformanceExecutionDataProvider.SCENARIO_COMPARATOR)

        then:
        scenarios*.scenarioName == ['a-regressed', 'b-improved', 'c-unknown']
    }

    def 'identifies this pipeline\'s executions by build id (CI) or commit (local) and derives the verdict from them'() {
        given:
        // The result JSON no longer carries a build id or status - it is only the scenario identity.
        def teamCityExecution = new PerformanceTestExecutionResult(scenarioName: 'x', scenarioClass: 'org.example.C', testProject: 'p')
        // The DB has a regressed row produced by this pipeline's bucket, plus a row from an unrelated build/commit.
        def pipelineRow = regressedExecution('114134082', COMMIT)
        def foreignRow = regressedExecution('114123152', 'other-commit')

        when:
        def scenario = new PerformanceReportScenario([teamCityExecution], [pipelineRow, foreignRow], false, pipelineBuildIds as Set, currentCommit)

        then:
        scenario.currentExecutions*.teamCityBuildId == expectedCurrentBuildIds
        scenario.regressed == expectedRegressed
        scenario.unknown == expectedUnknown

        where:
        desc                | pipelineBuildIds | currentCommit  | expectedCurrentBuildIds | expectedRegressed | expectedUnknown
        'CI, fresh run'     | ['114134082']    | 'ignored'      | ['114134082']           | true              | false
        'CI, build-cache hit' | ['999999']     | 'ignored'      | []                      | false             | true
        'local, by commit'  | []               | COMMIT         | ['114134082']           | true              | false
    }

    private PerformanceReportScenario regressed(String name) {
        return scenario(name, [1, 1, 1], [2, 2, 2])
    }

    private PerformanceReportScenario improved(String name) {
        return scenario(name, [2, 2, 2], [1, 1, 1])
    }

    private PerformanceReportScenario scenario(String name, List<Integer> baseVersion, List<Integer> currentVersion) {
        def execution = new PerformanceTestExecutionResult(scenarioName: name, scenarioClass: 'org.example.C', testProject: 'p')
        def history = new PerformanceReportScenarioHistoryExecution(new Date().getTime(), 'build-1', COMMIT, measuredOperationList(baseVersion), measuredOperationList(currentVersion))
        return new PerformanceReportScenario([execution], [history], false, ['build-1'] as Set, COMMIT)
    }

    private PerformanceReportScenario unknownScenario(String name) {
        def execution = new PerformanceTestExecutionResult(scenarioName: name, scenarioClass: 'org.example.C', testProject: 'p')
        return new PerformanceReportScenario([execution], [], false, ['build-1'] as Set, COMMIT)
    }

    private PerformanceReportScenarioHistoryExecution regressedExecution(String teamCityBuildId, String commitId) {
        // current markedly slower than baseline with high confidence -> confidentToSayWorse() == true
        return new PerformanceReportScenarioHistoryExecution(new Date().getTime(), teamCityBuildId, commitId, measuredOperationList([1, 1, 1]), measuredOperationList([2, 2, 2]))
    }
}
