/**
 * blackduck-artifactory-common
 *
 * Copyright (c) 2020 Synopsys, Inc.
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
package com.synopsys.integration.blackduck.artifactory.modules.scan.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.artifactory.fs.ItemInfo;
import org.artifactory.repo.RepoPath;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.blackduck.artifactory.ArtifactoryPAPIService;
import com.synopsys.integration.blackduck.artifactory.ArtifactoryPropertyService;
import com.synopsys.integration.blackduck.artifactory.BlackDuckArtifactoryProperty;
import com.synopsys.integration.blackduck.artifactory.DateTimeManager;
import com.synopsys.integration.blackduck.artifactory.modules.scan.ScanModule;
import com.synopsys.integration.blackduck.artifactory.modules.scan.ScanModuleConfig;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.log.Slf4jIntLogger;

public class RepositoryIdentificationService {
    private final IntLogger logger = new Slf4jIntLogger(LoggerFactory.getLogger(RepositoryIdentificationService.class));

    private final ScanModuleConfig scanModuleConfig;
    private final DateTimeManager dateTimeManager;
    private final ArtifactoryPropertyService artifactoryPropertyService;
    private final ArtifactoryPAPIService artifactoryPAPIService;

    public RepositoryIdentificationService(final ScanModuleConfig scanModuleConfig, final DateTimeManager dateTimeManager, final ArtifactoryPropertyService artifactoryPropertyService, final ArtifactoryPAPIService artifactoryPAPIService) {
        this.scanModuleConfig = scanModuleConfig;
        this.artifactoryPropertyService = artifactoryPropertyService;
        this.artifactoryPAPIService = artifactoryPAPIService;
        this.dateTimeManager = dateTimeManager;
    }

    public Set<RepoPath> searchForRepoPaths() {
        final List<String> patternsToScan = scanModuleConfig.getNamePatterns();
        final List<String> repoKeysToScan = scanModuleConfig.getRepos();
        final List<RepoPath> repoPaths = new ArrayList<>();

        if (!repoKeysToScan.isEmpty()) {
            repoKeysToScan.stream()
                .map(repoKey -> artifactoryPAPIService.searchForArtifactsByPatterns(repoKey, patternsToScan))
                .forEach(repoPaths::addAll);
        } else {
            logger.info(String.format("Please specify valid repos to scan or disable the %s", ScanModule.class.getSimpleName()));
        }

        logger.debug(String.format("patternsToScan: %d", patternsToScan.size()));
        logger.debug(String.format("repoKeysToScan: %d", repoKeysToScan.size()));
        logger.debug(String.format("repoPaths: %d", repoPaths.size()));
        return new HashSet<>(repoPaths);
    }

    /**
     * If artifact's last modified time is newer than the scan time, or we have no record of the scan time, we should scan now, unless, if the cutoff date is set, only scan if the modified date is greater than or equal to the cutoff.
     */
    boolean shouldRepoPathBeScannedNow(final RepoPath repoPath) {
        final ItemInfo itemInfo = artifactoryPAPIService.getItemInfo(repoPath);
        final long lastModifiedTime = itemInfo.getLastModified();
        final String artifactCutoffDate = scanModuleConfig.getArtifactCutoffDate();

        boolean shouldCutoffPreventScanning = false;
        if (StringUtils.isNotBlank(artifactCutoffDate)) {
            try {
                final long cutoffTime = dateTimeManager.getTimeFromString(artifactCutoffDate);
                shouldCutoffPreventScanning = lastModifiedTime < cutoffTime;
            } catch (final Exception e) {
                logger.error(String.format("The pattern: %s does not match the date string: %s", dateTimeManager.getDateTimePattern(), artifactCutoffDate), e);
                shouldCutoffPreventScanning = false;
            }
        }

        if (shouldCutoffPreventScanning) {
            logger.warn(String.format("%s was not scanned because the cutoff was set and the artifact is too old", itemInfo.getName()));
            return false;
        }

        final Optional<String> blackDuckScanTimeProperty = artifactoryPropertyService.getProperty(repoPath, BlackDuckArtifactoryProperty.SCAN_TIME);
        if (!blackDuckScanTimeProperty.isPresent()) {
            return true;
        }

        try {
            final long blackDuckScanTime = dateTimeManager.getTimeFromString(blackDuckScanTimeProperty.get());
            return lastModifiedTime >= blackDuckScanTime;
        } catch (final Exception e) {
            //if the date format changes, the old format won't parse, so just cleanup the property by returning true and re-scanning
            logger.error("Exception parsing the scan date (most likely the format changed)", e);
        }

        return true;
    }
}
