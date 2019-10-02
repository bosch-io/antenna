/*
 * Copyright (c) Bosch Software Innovations GmbH 2017-2019.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.antenna.sw360;

import org.eclipse.sw360.antenna.api.exceptions.AntennaException;
import org.eclipse.sw360.antenna.model.artifact.Artifact;
import org.eclipse.sw360.antenna.model.license.License;
import org.eclipse.sw360.antenna.model.util.ArtifactLicenseUtils;
import org.eclipse.sw360.antenna.sw360.adapter.*;
import org.eclipse.sw360.antenna.sw360.rest.resource.components.SW360Component;
import org.eclipse.sw360.antenna.sw360.rest.resource.licenses.SW360License;
import org.eclipse.sw360.antenna.sw360.rest.resource.releases.SW360Release;
import org.eclipse.sw360.antenna.sw360.workflow.SW360ConnectionConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SW360MetaDataUpdater {
    private static final Logger LOGGER = LoggerFactory.getLogger(SW360MetaDataUpdater.class);

    // rest service adapters
    private SW360ProjectClientAdapter projectClientAdapter;
    private SW360LicenseClientAdapter licenseClientAdapter;
    private SW360ComponentClientAdapter componentClientAdapter;
    private SW360ReleaseClientAdapter releaseClientAdapter;
    private SW360ConnectionConfiguration sw360ConnectionConfiguration;

    private final boolean updateReleases;
    private final boolean uploadSources;

    public SW360MetaDataUpdater(SW360ConnectionConfiguration sw360ConnectionConfiguration, boolean updateReleases, boolean uploadSources) {
        projectClientAdapter = sw360ConnectionConfiguration.getSW360ProjectClientAdapter();
        licenseClientAdapter = sw360ConnectionConfiguration.getSW360LicenseClientAdapter();
        componentClientAdapter = sw360ConnectionConfiguration.getSW360ComponentClientAdapter();
        releaseClientAdapter = sw360ConnectionConfiguration.getSW360ReleaseClientAdapter();
        this.sw360ConnectionConfiguration = sw360ConnectionConfiguration;
        this.updateReleases = updateReleases;
        this.uploadSources = uploadSources;
    }

    public Set<String> getOrCreateLicenses(Artifact artifact) throws AntennaException {
        HttpHeaders header = sw360ConnectionConfiguration.getHttpHeaders();
        Set<String> licenseIds = new HashSet<>();

        List<License> licenses = flattenedLicenses(artifact);
        for (License license : licenses) {
            SW360License sw360License;
            if (!licenseClientAdapter.isLicenseOfArtifactAvailable(license, header)) {
                sw360License = licenseClientAdapter.addLicense(license, header);
            } else {
                LOGGER.debug("License [" + license.getLicenseId() + "] already exists in SW360.");
                sw360License = licenseClientAdapter.getSW360LicenseByAntennaLicense(license, header);
            }
            licenseIds.add(sw360License.getShortName());
        }
        return licenseIds;
    }

    public SW360Release getOrCreateRelease(Artifact artifact, Set<String> licenseIds, SW360Component component) throws AntennaException {
        HttpHeaders header = sw360ConnectionConfiguration.getHttpHeaders();

        if (!releaseClientAdapter.isArtifactAvailableAsRelease(artifact, component, header)) {
            return releaseClientAdapter.addRelease(artifact, component, licenseIds, uploadSources, header);
        } else {
            Optional<SW360Release> release = releaseClientAdapter.getReleaseByArtifact(component, artifact, header);
            if (release.isPresent()) {
                if(updateReleases) {
                    return releaseClientAdapter.updateRelease(release.get(), artifact, component, licenseIds, header);
                } else {
                    return release.get();
                }
            } else {
                throw new AntennaException("No release found for the artifact [" +
                        artifact + "]");
            }
        }
    }

    public SW360Component getOrCreateComponent(Artifact artifact) throws AntennaException {
        HttpHeaders header = sw360ConnectionConfiguration.getHttpHeaders();

        if (!componentClientAdapter.isArtifactAvailableAsComponent(artifact, header)) {
            return componentClientAdapter.addComponent(artifact, header);
        } else {
            Optional<SW360Component> component = componentClientAdapter.getComponentByArtifact(artifact, header);
            if (component.isPresent()) {
                return component.get();
            } else {
                throw new AntennaException("No component found for the artifact [" +
                        artifact + "]");
            }
        }
    }

    public void createProject(String projectName, String projectVersion, Collection<SW360Release> releases) throws AntennaException, IOException {
        HttpHeaders header = sw360ConnectionConfiguration.getHttpHeaders();

        Optional<String> projectId = projectClientAdapter.getProjectIdByNameAndVersion(projectName, projectVersion, header);

        String id;
        if (projectId.isPresent()) {
            // TODO: Needs endpoint on sw360 to update project on sw360
            LOGGER.debug("Could not update project " + projectId.get() + ", because the endpoint is not available.");
            id = projectId.get();
        } else {
            id = projectClientAdapter.addProject(projectName, projectVersion, header);
        }
        projectClientAdapter.addSW360ReleasesToSW360Project(id, releases, header);
    }

    private List<License> flattenedLicenses(Artifact artifact) {
        return Stream
                .of(ArtifactLicenseUtils.getFinalLicenses(artifact).getLicenses())
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .filter(Objects::nonNull)
                .filter(li -> ! li.isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }
}
