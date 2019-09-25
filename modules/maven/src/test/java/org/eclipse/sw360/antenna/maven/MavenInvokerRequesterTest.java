/*
 * Copyright (c) Bosch Software Innovations GmbH 2016-2019.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.antenna.maven;

import com.github.packageurl.PackageURL;
import com.github.packageurl.PackageURLBuilder;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.utils.cli.CommandLineException;
import org.eclipse.sw360.antenna.api.exceptions.AntennaException;
import org.eclipse.sw360.antenna.model.artifact.facts.ArtifactCoordinates;
import org.eclipse.sw360.antenna.model.util.ArtifactCoordinatesUtils;
import org.eclipse.sw360.antenna.testing.AntennaTestWithMockedContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.*;
import org.mockito.stubbing.Answer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class MavenInvokerRequesterTest extends AntennaTestWithMockedContext {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Mock
    private DefaultInvoker defaultInvokerMock = Mockito.mock(DefaultInvoker.class);
    @Captor
    private
    ArgumentCaptor<InvocationRequest> captor = ArgumentCaptor.forClass(InvocationRequest.class);

    private MavenInvokerRequester mir;

    private PackageURL mavenPurl;

    @Before
    public void before() throws Exception {
        mavenPurl = PackageURLBuilder.aPackageURL()
                .withType(PackageURL.StandardTypes.MAVEN)
                .withNamespace("groupId")
                .withName("artifactId")
                .withVersion("version")
                .build();

        System.setProperty("maven.home", temporaryFolder.newFolder("m2").toString());

        File basedir = temporaryFolder.newFile("projectBasedir");

        mir = new MavenInvokerRequester(basedir, defaultInvokerMock, Optional.empty());
    }

    @Test
    public void getPomFileSanityCheck() {
        File pomFile = mir.getPomFileFromContext();
        assertThat(pomFile.toString()).endsWith(MavenInvokerRequester.POM_FILENAME);
    }

    private InvocationResult getDummyInvocationResult(int returncode){
        return new InvocationResult() {
            @Override
            public CommandLineException getExecutionException() {
                return null;
            }

            @Override
            public int getExitCode() {
                return returncode;
            }
        };
    }

    @Test
    public void requestFileTestThatRequestIsComposedCorrectly() throws Exception {
        Path targetDirectory = temporaryFolder.newFolder("target").toPath();

        final String expectedJarBaseName = mir.getExpectedJarBaseName(mavenPurl, ClassifierInformation.DEFAULT_JAR);
        File expectedJarFile = new File(targetDirectory.toFile(), expectedJarBaseName);

        Mockito.when(defaultInvokerMock.execute(ArgumentMatchers.any(InvocationRequest.class)))
                .then((Answer<InvocationResult>) invocationOnMock -> {
                    // touch expected file
                    new FileOutputStream(expectedJarFile).close();

                    // return dummy result
                    return getDummyInvocationResult(0);
                });

        Optional<File> resultFile = mir.requestFile(mavenPurl, targetDirectory, ClassifierInformation.DEFAULT_JAR);

        Mockito.verify(defaultInvokerMock).execute(captor.capture());

        assertThat(resultFile.get()).isEqualTo(expectedJarFile);
        InvocationRequest invocationRequest = captor.getValue();
        Collection<String> goals = invocationRequest.getGoals();

        assertThat(goals).isNotEmpty();
        assertThat(goals).filteredOn(s -> s.contains(mavenPurl.getNamespace())).hasSize(1);
        assertThat(goals).filteredOn(s -> s.contains(mavenPurl.getName())).hasSize(1);
        assertThat(goals).filteredOn(s -> s.contains(mavenPurl.getVersion())).hasSize(1);
        assertThat(goals).filteredOn(s -> s.contains(targetDirectory.toString())).hasSize(1);
    }

    @Test
    public void requestFileRecognizesNonExistingArtifactReturningAnEmptyOptional() throws Exception {
        Path targetDirectory = temporaryFolder.newFolder("target").toPath();
        Mockito.when(defaultInvokerMock.execute(ArgumentMatchers.any(InvocationRequest.class)))
                .thenReturn(getDummyInvocationResult(0));
        Optional<File> requestResult = mir.requestFile(mavenPurl, targetDirectory, ClassifierInformation.DEFAULT_JAR);
        assertThat(requestResult).isEmpty();
    }

    @Test
    public void requestFileOnNonzeroReturnCodeReturnsEmptyOptional() throws Exception {
        Path targetDirectory = temporaryFolder.newFolder("target").toPath();

        final String expectedJarBaseName = mir.getExpectedJarBaseName(mavenPurl, ClassifierInformation.DEFAULT_JAR);
        File expectedJarFile = new File(targetDirectory.toFile(), expectedJarBaseName);

        Mockito.when(defaultInvokerMock.execute(ArgumentMatchers.any(InvocationRequest.class)))
                .then((Answer<InvocationResult>) invocationOnMock -> {
                    // touch expected file
                    new FileOutputStream(expectedJarFile).close();

                    // return dummy result
                    return getDummyInvocationResult(1);
                });
        Optional<File> requestResult = mir.requestFile(mavenPurl, targetDirectory, ClassifierInformation.DEFAULT_JAR);
        assertThat(requestResult).isEmpty();
    }
}