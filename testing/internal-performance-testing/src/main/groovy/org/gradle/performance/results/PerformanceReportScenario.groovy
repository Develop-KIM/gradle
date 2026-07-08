/*
 * Copyright 2021 the original author or authors.
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

/**
 * Represents a row in performance report, i.e. a specific scenario.
 */
class PerformanceReportScenario {
    final PerformanceExperiment performanceExperiment
    /**
     * The scenario identities read from the (cacheable) TeamCity-build-generated result JSONs. These carry only the
     * scenario name/class/project - no build id, status or timings - so they tell us *which* scenarios were exercised,
     * not their outcome.
     */
    final List<PerformanceTestExecutionResult> teamCityExecutions

    /**
     * The executions read from the performance database that were produced by this pipeline (see constructor).
     */
    final List<PerformanceReportScenarioHistoryExecution> currentExecutions

    /**
     * All executions read from the performance database (current + prior history).
     */
    final List<PerformanceReportScenarioHistoryExecution> historyExecutions

    final boolean crossBuild

    PerformanceReportScenario(
        List<PerformanceTestExecutionResult> teamCityExecutions,
        List<PerformanceReportScenarioHistoryExecution> historyExecutions,
        boolean crossBuild,
        Set<String> pipelineBuildIds,
        String currentCommit
    ) {
        if (teamCityExecutions.empty) {
            throw new IllegalArgumentException("teamCity executions must not be empty!")
        }
        this.performanceExperiment = teamCityExecutions[0].performanceExperiment
        this.teamCityExecutions = teamCityExecutions
        this.crossBuild = crossBuild

        // "Current" executions are the ones this pipeline actually produced. On CI we identify them by matching each DB
        // row's own teamCityBuildId (written accurately by the run that measured it) against the authoritative bucket
        // build IDs of this pipeline. A build-cache hit produces no DB row at all, so cached results never appear here -
        // which is why we no longer rely on the (cacheable) result JSON's build id or status. Locally, where the
        // authoritative set is unknown, we fall back to matching the commit under test.
        this.currentExecutions = pipelineBuildIds.isEmpty()
            ? historyExecutions.findAll { it.commitId == currentCommit }
            : historyExecutions.findAll { pipelineBuildIds.contains(it.teamCityBuildId) }
        this.historyExecutions = historyExecutions
    }

    String getName() {
        return "$scenarioName | $testProject | ${scenarioClass.substring(scenarioClass.lastIndexOf(".") + 1)}"
    }

    String getScenarioName() {
        return performanceExperiment.scenario.testName
    }

    String getScenarioClass() {
        return performanceExperiment.scenario.className
    }

    String getTestProject() {
        return performanceExperiment.testProject
    }

    boolean isCrossVersion() {
        return !crossBuild
    }

    /**
     * No measurement produced by this pipeline is available for this scenario (e.g. the bucket result was served from
     * the build cache, or the scenario did not run), so there is nothing to evaluate.
     */
    boolean isUnknown() {
        return currentExecutions.empty
    }

    boolean isImproved() {
        return !crossBuild && !currentExecutions.empty && currentExecutions.every { it.confidentToSayBetter() }
    }

    /**
     * Whether this pipeline's own measurements (from the DB, not any status baked into the cacheable result JSON) show
     * a confident regression for this scenario. This is the signal used to fail the build.
     */
    boolean isRegressed() {
        return !crossBuild && currentExecutions.any { it.confidentToSayWorse() }
    }

    boolean isSuccessful() {
        return !currentExecutions.empty && !isRegressed()
    }

    boolean isAboutToRegress() {
        return isRegressed()
    }

    double getDifferenceSortKey() {
        if (currentExecutions.empty) {
            return Double.NEGATIVE_INFINITY
        }
        def firstExecution = currentExecutions[0]
        double signum = Math.signum(firstExecution.differencePercentage)
        if (signum == 0.0d) {
            signum = -1.0
        }
        return firstExecution.confidencePercentage * signum
    }

    double getDifferencePercentage() {
        return currentExecutions.empty ? Double.NEGATIVE_INFINITY : currentExecutions[0].getDifferencePercentage()
    }
}
