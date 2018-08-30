/*
 * hub-artifactory
 *
 * Copyright (C) 2018 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.blackducksoftware.integration.hub.artifactory.scan

import com.blackducksoftware.integration.hub.api.generated.view.VersionBomPolicyStatusView
import com.blackducksoftware.integration.hub.artifactory.BlackDuckArtifactoryConfig
import com.blackducksoftware.integration.hub.artifactory.BlackDuckArtifactoryProperty
import com.blackducksoftware.integration.hub.exception.HubIntegrationException
import com.blackducksoftware.integration.hub.service.model.PolicyStatusDescription
import embedded.org.apache.commons.lang3.StringUtils
import groovy.transform.Field
import org.apache.commons.io.FileUtils
import org.artifactory.repo.RepoPath

// propertiesFilePathOverride allows you to specify an absolute path to the blackDuckScanForHub.properties file.
// If this is empty, we will default to ${ARTIFACTORY_HOME}/etc/plugins/lib/blackDuckScanForHub.properties
@Field String propertiesFilePathOverride = ""

@Field BlackDuckArtifactoryConfig blackDuckArtifactoryConfig
@Field RepositoryIdentificationService repositoryIdentificationService
@Field ScanPhoneHomeService scanPhoneHomeService
@Field ArtifactScanService artifactScanService
@Field ScanPluginManager scanPluginManager
@Field ArtifactoryScanPropertyService artifactoryScanPropertyService

initialize()

executions {
    /**
     * This will attempt to reload the properties file and initialize the scanner with the new values.
     *
     * This can be triggered with the following curl command:
     * curl -X POST -u admin:password "http://ARTIFACTORY_SERVER/artifactory/api/plugins/execute/blackDuckReloadScanner"
     */
    blackDuckReloadScanner(httpMethod: 'POST') { params ->
        log.info('Starting blackDuckReloadScanner REST request...')

        initialize()

        log.info('...completed blackDuckReloadScanner REST request.')
    }

    /**
     * This will search your artifactory ARTIFACTORY_REPOS_TO_SEARCH repositories for the filename patterns designated in ARTIFACT_NAME_PATTERNS_TO_SCAN.
     * For example:
     *
     * ARTIFACTORY_REPOS_TO_SEARCH="my-releases,my-snapshots"
     * ARTIFACT_NAME_PATTERNS_TO_SCAN="*.war,*.zip"
     *
     * then this REST call will search 'my-releases' and 'my-snapshots' for all .war (web archive) and .zip files, scan them, and publish the BOM to the provided Hub server.
     *
     * The scanning process will add several properties to your artifacts in artifactory. Namely:
     *
     * blackDuckScanResult - SUCCESS or FAILURE, depending on the result of the scan
     * blackDuckScanTime - the last time a SUCCESS scan was completed
     * blackDuckScanCodeLocationUrl - the url for the code location created in the Hub
     *
     * The same functionality is provided via the scanForHub cron job to enable scheduled scans to run consistently.
     *
     * This can be triggered with the following curl command:
     * curl -X GET -u admin:password "http://ARTIFACTORY_SERVER/artifactory/api/plugins/execute/blackDuckScan"
     */
    blackDuckScan(httpMethod: 'GET') { params ->
        log.info('Starting blackDuckScan REST request...')

        Set<RepoPath> repoPaths = repositoryIdentificationService.searchForRepoPaths()
        artifactScanService.scanArtifactPaths(repoPaths)

        log.info('...completed blackDuckScan REST request.')
    }

    /**
     * This will return a current status of the plugin's configuration to verify things are setup properly.
     *
     * This can be triggered with the following curl command:
     * curl -X GET -u admin:password "http://ARTIFACTORY_SERVER/artifactory/api/plugins/execute/blackDuckTestConfig"
     */
    blackDuckTestConfig(httpMethod: 'GET') { params ->
        log.info('Starting blackDuckTestConfig REST request...')

        message = buildStatusCheckMessage()

        log.info('...completed blackDuckTestConfig REST request.')
    }

    /**
     * This will delete, then recreate, the blackducksoftware directory which includes the cli, the cron job log, as well as all the cli logs.
     *
     * This can be triggered with the following curl command:
     * curl -X POST -u admin:password "http://ARTIFACTORY_SERVER/artifactory/api/plugins/execute/blackDuckReloadDirectory"
     */
    blackDuckReloadDirectory() { params ->
        log.info('Starting blackDuckReloadDirectory REST request...')

        FileUtils.deleteDirectory(blackDuckArtifactoryConfig.blackDuckDirectory)
        blackDuckArtifactoryConfig.blackDuckDirectory.mkdirs()

        log.info('...completed blackDuckReloadDirectory REST request.')
    }

    /**
     * This will search your artifactory ARTIFACTORY_REPOS_TO_SEARCH repositories for the filename patterns designated in ARTIFACT_NAME_PATTERNS_TO_SCAN.
     * For example:
     *
     * ARTIFACTORY_REPOS_TO_SEARCH="my-releases,my-snapshots"
     * ARTIFACT_NAME_PATTERNS_TO_SCAN="*.war,*.zip"
     *
     * then this REST call will search 'my-releases' and 'my-snapshots' for all .war (web archive) and .zip files and delete all the properties that the plugin sets.
     *
     * This can be triggered with the following curl command:
     * curl -X POST -u admin:password "http://ARTIFACTORY_SERVER/artifactory/api/plugins/execute/blackDuckDeleteScanProperties"
     */
    blackDuckDeleteScanProperties() { params ->
        log.info('Starting blackDuckDeleteScanProperties REST request...')

        Set<RepoPath> repoPaths = repositoryIdentificationService.searchForRepoPaths()
        repoPaths.each { artifactoryScanPropertyService.deleteAllBlackDuckProperties(it) }

        log.info('...completed blackDuckDeleteScanProperties REST request.')
    }

    /**
     * This will search your artifactory ARTIFACTORY_REPOS_TO_SEARCH repositories for the filename patterns designated in ARTIFACT_NAME_PATTERNS_TO_SCAN.
     * For example:
     *
     * ARTIFACTORY_REPOS_TO_SEARCH="my-releases,my-snapshots"
     * ARTIFACT_NAME_PATTERNS_TO_SCAN="*.war,*.zip"
     *
     * then this REST call will search 'my-releases' and 'my-snapshots' for all .war (web archive) and .zip files and checks for the 'blackduck.scanResult' property
     * if that property indicates a scan failure, it delete all the properties that the plugin set on it.
     *
     * This can be triggered with the following curl command:
     * curl -X POST -u admin:password "http://ARTIFACTORY_SERVER/artifactory/api/plugins/execute/blackDuckDeleteScanPropertiesFromFailures"
     */
    blackDuckDeleteScanPropertiesFromFailures() { params ->
        log.info('Starting blackDuckDeleteScanPropertiesFromFailures REST request...')

        Set<RepoPath> repoPaths = repositoryIdentificationService.searchForRepoPaths()
        repoPaths.each {
            if (repositories.getProperty(it, BlackDuckArtifactoryProperty.SCAN_RESULT.getName())?.equals('FAILURE')) {
                artifactoryScanPropertyService.deleteAllBlackDuckProperties(it)
            }
        }

        log.info('...completed blackDuckDeleteScanPropertiesFromFailures REST request.')
    }
}

jobs {
    /**
     * This will search your artifactory ARTIFACTORY_REPOS_TO_SEARCH repositories for the filename patterns designated in ARTIFACT_NAME_PATTERNS_TO_SCAN.
     * For example:
     *
     * ARTIFACTORY_REPOS_TO_SEARCH="my-releases,my-snapshots"
     * ARTIFACT_NAME_PATTERNS_TO_SCAN="*.war,*.zip"
     *
     * then this cron job will search 'my-releases' and 'my-snapshots' for all .war (web archive) and .zip files, scan them, and publish the BOM to the provided Hub server.
     *
     * The scanning process will add several properties to your artifacts in artifactory. Namely:
     *
     * blackduck.scanResult - SUCCESS or FAILURE, depending on the result of the scan
     * blackduck.scanTime - the last time a SUCCESS scan was completed
     * blackduck.uiUrl - the url directly to the scanned BOM in the Hub
     * blackduck.apiUrl - the api url for your project which is needed for further Hub REST calls.
     *
     * The same functionality is provided via the scanForHub execution to enable a one-time scan triggered via a REST call.
     */
    blackDuckScan(cron: artifactoryScanPropertyService.blackDuckScanCron) {
        log.info('Starting blackDuckScan cron job...')

        Set<RepoPath> repoPaths = repositoryIdentificationService.searchForRepoPaths()
        artifactScanService.scanArtifactPaths(repoPaths)

        log.info('...completed blackDuckScan cron job.')
    }

    blackDuckAddPolicyStatus(cron: artifactoryScanPropertyService.blackDuckAddPolicyStatusCron) {
        log.info('Starting blackDuckAddPolicyStatus cron job...')

        Set<RepoPath> repoPaths = repositoryIdentificationService.searchForRepoPaths()
        populatePolicyStatuses(repoPaths)

        log.info('...completed blackDuckAddPolicyStatus cron job.')
    }
}

//####################################################
//PLEASE MAKE NO EDITS BELOW THIS LINE - NO TOUCHY!!!
//####################################################

private void populatePolicyStatuses(Set<RepoPath> repoPaths) {
    boolean problemRetrievingPolicyStatus = false
    repoPaths.each {
        try {
            String projectVersionUrl = repositories.
            getProperty(it, BlackDuckArtifactoryProperty.PROJECT_VERSION_URL.getName())
            if (StringUtils.isNotBlank(projectVersionUrl)) {
                projectVersionUrl = artifactoryScanPropertyService.updateUrlPropertyToCurrentHubServer(projectVersionUrl)
                repositories.setProperty(it, BlackDuckArtifactoryProperty.PROJECT_VERSION_URL.getName(), projectVersionUrl)
                try {
                    VersionBomPolicyStatusView versionBomPolicyStatusView = scanPluginManager.hubConnectionService.getPolicyStatusOfProjectVersion(projectVersionUrl);
                    log.info("policy status json: " + versionBomPolicyStatusView.json);
                    PolicyStatusDescription policyStatusDescription = new PolicyStatusDescription(versionBomPolicyStatusView);
                    repositories.setProperty(it, BlackDuckArtifactoryProperty.POLICY_STATUS.getName(), policyStatusDescription.policyStatusMessage)
                    repositories.setProperty(it, BlackDuckArtifactoryProperty.OVERALL_POLICY_STATUS.getName(), versionBomPolicyStatusView.overallStatus.toString())
                    log.info("Added policy status to ${it.name}")
                    repositories.setProperty(it, BlackDuckArtifactoryProperty.UPDATE_STATUS.getName(), 'UP TO DATE')
                    repositories.setProperty(it, BlackDuckArtifactoryProperty.LAST_UPDATE.getName(), scanPluginManager.dateTimeManager.getStringFromDate(new Date()))
                    scanPhoneHomeService.phoneHome()
                } catch (HubIntegrationException e) {
                    problemRetrievingPolicyStatus = true
                    def policyStatus = repositories.getProperty(it, BlackDuckArtifactoryProperty.POLICY_STATUS.getName())
                    def overallPolicyStatus = repositories.getProperty(it, BlackDuckArtifactoryProperty.OVERALL_POLICY_STATUS.getName())
                    if (StringUtils.isNotBlank(policyStatus) || StringUtils.isNotBlank(overallPolicyStatus)) {
                        repositories.setProperty(it, BlackDuckArtifactoryProperty.UPDATE_STATUS.getName(), 'OUT OF DATE')
                    }
                }
            }
        } catch (Exception e) {
            log.error("There was a problem trying to access repository ${it.name}: ", e)
            problemRetrievingPolicyStatus = true
        }
    }
    if (problemRetrievingPolicyStatus) {
        log.warn('There was a problem retrieving policy status for one or more repos. This is expected if you do not have policy management.')
    }
}

private String buildStatusCheckMessage() {
    def connectMessage = 'OK'
    try {
        if (scanPluginManager.hubConnectionService == null) {
            connectMessage = 'Could not create the connection to the Hub - you will have to check the artifactory logs.'
        }
    } catch (Exception e) {
        connectMessage = e.message
    }

    Set<RepoPath> repoPaths = repositoryIdentificationService.searchForRepoPaths()

    def cutoffMessage = 'The date cutoff is not specified so all artifacts that are found will be scanned.'
    if (StringUtils.isNotBlank(scanPluginManager.getArtifactCutoffDate())) {
        try {
            scanPluginManager.dateTimeManager.getTimeFromString(scanPluginManager.getArtifactCutoffDate())
            cutoffMessage = 'The date cutoff is specified correctly.'
        } catch (Exception e) {
            cutoffMessage = "The pattern: ${scanPluginManager.dateTimeManager.dateTimePattern} does not match the date string: ${scanPluginManager.getArtifactCutoffDate()}: ${e.message}"
        }
    }

    return """canConnectToHub: ${connectMessage}
artifactsFound: ${repoPaths.size()}
dateCutoffStatus: ${cutoffMessage}
"""
}

private void initialize() {
    blackDuckArtifactoryConfig = new BlackDuckArtifactoryConfig()
    blackDuckArtifactoryConfig.setEtcDirectory(ctx.artifactoryHome.etcDir.toString())
    blackDuckArtifactoryConfig.setHomeDirectory(ctx.artifactoryHome.homeDir.toString())
    blackDuckArtifactoryConfig.setPluginsDirectory(ctx.artifactoryHome.pluginsDir.toString())
    blackDuckArtifactoryConfig.setThirdPartyVersion(ctx?.versionProvider?.running?.versionName?.toString())

    // The ScanPluginManager must be created first
    scanPluginManager = new ScanPluginManager(blackDuckArtifactoryConfig);
    scanPluginManager.setUpBlackDuckDirectory()

    final String defaultPropertiesFileName = "${this.getClass().getSimpleName()}.properties"
    artifactoryScanPropertyService = new ArtifactoryScanPropertyService(blackDuckArtifactoryConfig, scanPluginManager, repositories, searches, propertiesFilePathOverride, defaultPropertiesFileName)
    repositoryIdentificationService = new RepositoryIdentificationService(blackDuckArtifactoryConfig, scanPluginManager, repositories, searches)
    scanPhoneHomeService = new ScanPhoneHomeService(blackDuckArtifactoryConfig, scanPluginManager.getHubConnectionService())
    artifactScanService = new ArtifactScanService(blackDuckArtifactoryConfig, repositoryIdentificationService, scanPluginManager, scanPhoneHomeService, artifactoryScanPropertyService, repositories)
}
