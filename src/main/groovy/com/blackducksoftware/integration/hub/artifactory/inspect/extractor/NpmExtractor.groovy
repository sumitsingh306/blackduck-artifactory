package com.blackducksoftware.integration.hub.artifactory.inspect.extractor

import java.nio.charset.StandardCharsets
import java.util.zip.GZIPInputStream

import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.io.IOUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import com.blackducksoftware.integration.hub.artifactory.ArtifactoryDownloader
import com.blackducksoftware.integration.hub.bdio.simple.model.BdioComponent
import com.blackducksoftware.integration.hub.bdio.simple.model.BdioExternalIdentifier

import groovy.json.JsonSlurper

@Component
class NpmExtractor extends Extractor {
    private final Logger logger = LoggerFactory.getLogger(NpmExtractor.class)

    @Autowired
    ArtifactoryDownloader artifactoryDownloader

    boolean shouldAttemptExtract(String artifactName, Map jsonObject) {
        def extension = getExtension(artifactName)
        'tgz' == extension || 'tar.gz' == extension
    }

    BdioComponent extract(String artifactName, Map jsonObject) {
        def tgzFile = artifactoryDownloader.download(jsonObject, artifactName)

        def tarArchiveInputStream = new TarArchiveInputStream(new GZIPInputStream(new FileInputStream(tgzFile)))
        BdioComponent bdioComponent = null
        try {
            def tarArchiveEntry
            while (null != (tarArchiveEntry = tarArchiveInputStream.getNextTarEntry())) {
                if ('package/package.json' == tarArchiveEntry.name) {
                    byte[] entryBuffer = decompressTarContents(logger, 'package/package.json', artifactName, tarArchiveInputStream, tarArchiveEntry)
                    String entryContent = new String(entryBuffer, StandardCharsets.UTF_8)

                    def npmPackageJson = new JsonSlurper().parseText(entryContent)
                    def packageName = npmPackageJson.name
                    def version = npmPackageJson.version

                    String bdioId = bdioPropertyHelper.createBdioId(packageName, version)
                    BdioExternalIdentifier bdioExternalIdentifier = bdioPropertyHelper.createNpmExternalIdentifier(packageName, version)
                    bdioComponent = bdioNodeFactory.createComponent(packageName, version, bdioId, bdioExternalIdentifier)
                    return bdioComponent
                }
            }
        } finally {
            IOUtils.closeQuietly(tarArchiveInputStream)
        }
    }
}
