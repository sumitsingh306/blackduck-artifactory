/**
 * blackduck-artifactory-common
 *
 * Copyright (c) 2019 Synopsys, Inc.
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
package com.synopsys.integration.blackduck.artifactory

import com.google.common.collect.HashMultimap
import com.google.common.collect.SetMultimap
import com.synopsys.integration.log.IntLogger
import com.synopsys.integration.util.NameVersion
import org.apache.commons.lang3.StringUtils
import org.artifactory.repo.RepoPath
import java.util.*

class ArtifactoryPropertyService(private val artifactoryPAPIService: ArtifactoryPAPIService, private val dateTimeManager: DateTimeManager) {

    fun hasProperty(repoPath: RepoPath, property: BlackDuckArtifactoryProperty): Boolean {
        return artifactoryPAPIService.hasProperty(repoPath, property.propertyName)
    }

    fun getProperty(repoPath: RepoPath, property: BlackDuckArtifactoryProperty): Optional<String> {
        return getProperty(repoPath, property.propertyName)
    }

    private fun getProperty(repoPath: RepoPath, propertyKey: String): Optional<String> {
        val propertyValue = StringUtils.stripToNull(artifactoryPAPIService.getProperty(repoPath, propertyKey))

        return Optional.ofNullable(propertyValue)
    }

    fun getPropertyAsInteger(repoPath: RepoPath, property: BlackDuckArtifactoryProperty): Optional<Int> {
        return getProperty(repoPath, property).map { it.toInt() }
    }

    fun getDateFromProperty(repoPath: RepoPath, property: BlackDuckArtifactoryProperty): Optional<Date> {
        val dateTimeAsString = getProperty(repoPath, property)

        return dateTimeAsString.map { dateTimeManager.getDateFromString(it) }
    }

    fun setProperty(repoPath: RepoPath, property: BlackDuckArtifactoryProperty, value: String, logger: IntLogger) {
        setProperty(repoPath, property.propertyName, value, logger)
    }

    private fun setProperty(repoPath: RepoPath, property: String, value: String, logger: IntLogger) {
        artifactoryPAPIService.setProperty(repoPath, property, value)
        logger.debug("Set property $property to $value on ${repoPath.toPath()}")
    }

    fun setPropertyFromDate(repoPath: RepoPath, property: BlackDuckArtifactoryProperty, date: Date, logger: IntLogger) {
        val dateTimeAsString = dateTimeManager.getStringFromDate(date)
        setProperty(repoPath, property, dateTimeAsString, logger)

        val dateTimeAsStringConverted = dateTimeManager.geStringFromDateWithTimeZone(date)
        dateTimeAsStringConverted.ifPresent { converted -> setProperty(repoPath, property.timeName, converted, logger) }
    }

    fun deleteProperty(repoPath: RepoPath, property: BlackDuckArtifactoryProperty, logger: IntLogger) {
        deleteProperty(repoPath, property.propertyName, logger)
        deleteProperty(repoPath, property.timeName, logger)
    }

    private fun deleteProperty(repoPath: RepoPath, propertyName: String, logger: IntLogger) {
        if (artifactoryPAPIService.hasProperty(repoPath, propertyName)) {
            artifactoryPAPIService.deleteProperty(repoPath, propertyName)
            logger.debug("Removed property $propertyName from ${repoPath.toPath()}")
        }
    }

    fun deleteAllBlackDuckPropertiesFromRepo(repoKey: String, params: Map<String, List<String>>, logger: IntLogger) {
        BlackDuckArtifactoryProperty.values()
                .flatMap { artifactoryProperty -> getItemsContainingAnyProperties(repoKey, artifactoryProperty) }
                .forEach { repoPath -> deleteAllBlackDuckPropertiesFromRepoPath(repoPath, params, logger) }
    }

    private fun getItemsContainingAnyProperties(repoKey: String, vararg properties: BlackDuckArtifactoryProperty): List<RepoPath> {
        return properties
                .flatMap { getItemsContainingAnyProperties(repoKey, it) }
    }

    fun deleteAllBlackDuckPropertiesFromRepoPath(repoPath: RepoPath, params: Map<String, List<String>>, logger: IntLogger) {
        BlackDuckArtifactoryProperty.values()
                .filterNot { isPropertyInParams(it, params) }
                .forEach { property -> deleteProperty(repoPath, property, logger) }
    }

    private fun isPropertyInParams(blackDuckArtifactoryProperty: BlackDuckArtifactoryProperty, params: Map<String, List<String>>): Boolean {
        return params["properties"]?.any { it == blackDuckArtifactoryProperty.propertyName } ?: false
    }

    fun getItemsContainingProperties(repoKey: String, vararg properties: BlackDuckArtifactoryProperty): List<RepoPath> {
        val setMultimap = HashMultimap.create<String, String>()
        properties.forEach { setMultimap.put(it.propertyName, "*") }

        return getItemsContainingPropertiesAndValues(setMultimap, repoKey)
    }

    fun getItemsContainingPropertiesAndValues(setMultimap: SetMultimap<String, String>, vararg repoKeys: String): List<RepoPath> {
        return artifactoryPAPIService.itemsByProperties(setMultimap, *repoKeys)
    }

    fun getProjectNameVersion(repoPath: RepoPath): Optional<NameVersion> {
        val projectName = getProperty(repoPath, BlackDuckArtifactoryProperty.BLACKDUCK_PROJECT_NAME)
        val projectVersionName = getProperty(repoPath, BlackDuckArtifactoryProperty.BLACKDUCK_PROJECT_VERSION_NAME)
        var nameVersion: NameVersion? = null

        if (projectName.isPresent && projectVersionName.isPresent) {
            nameVersion = NameVersion(projectName.get(), projectVersionName.get())
        }

        return Optional.ofNullable(nameVersion)
    }
}
