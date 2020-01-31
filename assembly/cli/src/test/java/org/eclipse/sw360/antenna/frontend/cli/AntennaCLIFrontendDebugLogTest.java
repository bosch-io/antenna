/*
 * Copyright (c) Bosch.IO GmbH 2020.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.antenna.frontend.cli;

import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.WriterAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.eclipse.sw360.antenna.frontend.cli.teststeps.DummyLoggingAnalyzer;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Permission;
import java.util.Arrays;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * A test class to check special functionality of the Antenna CLI related to
 * logging and error handling. As these tests require a special setup, they
 * cannot be contained in the regular test class.
 */
public class AntennaCLIFrontendDebugLogTest {
    /**
     * Name of the permission to exit the current VM.
     */
    private static final String PERM_EXIT_VM = "exitVM";

    /**
     * Name of the test tool configuration file that gets executed.
     */
    private static final String TOOL_CONFIG = "/toolConfig.xml";

    /**
     * Special parameter that is replaced by the current POM file.
     */
    private static final String PARAM_CONFIG_FILE = "$POM";

    /**
     * The name of the appender to capture log output.
     */
    private static final String WRITER_APPENDER_NAME = "logCaptureAppender";

    /**
     * Stores the original SM, so that it can be restored later.
     */
    private static SecurityManager standardSecurityManager;

    /**
     * Stores the original output stream. Some tests change this stream to
     * capture the output generated by the CLI. In those cases, the original
     * stream has to be restored after the test.
     */
    private PrintStream originalSystemOut;

    @BeforeClass
    public static void setUpOnce() {
        standardSecurityManager = System.getSecurityManager();
        System.setSecurityManager(createSecurityManagerThatPreventsSystemExit());
    }

    @AfterClass
    public static void tearDownOnce() {
        System.setSecurityManager(standardSecurityManager);
    }

    @Before
    public void setUp() {
        originalSystemOut = System.out;
    }

    @After
    public void tearDown() {
        System.setOut(originalSystemOut);
        Configurator.reconfigure();  // set logging config back to defaults
    }

    /**
     * Returns a {@code SecurityManager} that prohibits exiting the VM. The
     * Antenna CLI application calls {@code System.exit()} under certain
     * circumstances, e.g. if invalid arguments are passed in. In order to
     * check such constellations, it has to be prevented that the VM is
     * actually stopped.
     *
     * @return a tweaked {@code SecurityManager}
     */
    private static SecurityManager createSecurityManagerThatPreventsSystemExit() {
        return new SecurityManager() {
            @Override
            public void checkPermission(Permission perm) {
                if (perm.getName().startsWith(PERM_EXIT_VM)) {
                    throw new SecurityException("Suppressed System.exit().");
                }
            }

            @Override
            public void checkPermission(Permission perm, Object context) {
                checkPermission(perm);
            }
        };
    }

    /**
     * Supports changing the logging configuration by invoking the action
     * specified on all exiting logger configurations.
     *
     * @param action the action to configure a logger
     */
    private static void configureLoggers(Consumer<LoggerConfig> action) {
        LoggerContext context = LoggerContext.getContext(false);
        Configuration configuration = context.getConfiguration();

        configuration.getLoggers().values().forEach(action);
        action.accept(configuration.getRootLogger());  // need to handle root explicitly
    }

    /**
     * Installs a special appender to cover all the log output that is
     * generated. The {@code StringWriter} that is returned can be used to
     * obtain the output that was written.
     *
     * @return a {@code StringWriter} to obtain the log output
     */
    private static StringWriter captureLogOutput() {
        StringWriter writer = new StringWriter();
        WriterAppender appender = WriterAppender.createAppender(PatternLayout.createDefaultLayout(), null,
                writer, WRITER_APPENDER_NAME, false, true);
        appender.start();

        configureLoggers(logConfig -> logConfig.addAppender(appender, null, null));
        return writer;
    }

    /**
     * Executes a test Antenna run with the given command line arguments. If
     * the arguments contain the special placeholder {@value #PARAM_CONFIG_FILE}, this
     * value is replaced by the path to the POM file.
     *
     * @param args the command line arguments
     */
    private static void runAntenna(String... args) {
        String[] antennaArgs = Arrays.stream(args)
                .map(AntennaCLIFrontendDebugLogTest::replaceConfigFilePlaceholder)
                .toArray(String[]::new);

        AntennaCLIFrontend.main(antennaArgs);
    }

    /**
     * Executes a test Antenna run with the given command line arguments and
     * returns a string with the logging output that has been captured.
     * Parameter processing works in the same way as for
     * {@link #runAntenna(String...)}.
     *
     * @param args the command line arguments
     * @return a string with the log output that was captured
     */
    private static String runAntennaAndCaptureLogOutput(String... args) {
        StringWriter logWriter = captureLogOutput();
        runAntenna(args);
        logWriter.flush();
        return logWriter.toString();
    }

    /**
     * Executes a test Antenna run with the given command line arguments, which
     * is expected to fail. The message printed to the console is returned.
     *
     * @param args the command line arguments
     * @return the output written to System.out
     */
    private static String runAntennaAndExpectFailure(String... args) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(bos);
        System.setOut(out);

        try {
            runAntenna(args);
            fail("Antenna run did not fail.");
        } catch (SecurityException e) {
            //expected
        }
        try {
            return bos.toString(StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            //cannot happen
            throw new AssertionError(e);
        }
    }

    /**
     * Replaces the special placeholder for the configuration file with the
     * actual path. All other parameters are returned without changes.
     *
     * @param param the parameter
     * @return the replaced parameter
     */
    private static String replaceConfigFilePlaceholder(String param) {
        if (PARAM_CONFIG_FILE.equals(param)) {
            try {
                Path configPath = Paths.get(AntennaCLIFrontendDebugLogTest.class.getResource(TOOL_CONFIG).toURI());
                return configPath.toAbsolutePath().toString();
            } catch (URISyntaxException e) {
                throw new AssertionError("Failed to load test configuration file " + TOOL_CONFIG);
            }
        }
        return param;
    }

    @Test
    public void testLogLevelIsInfoByDefault() {
        String output = runAntennaAndCaptureLogOutput(PARAM_CONFIG_FILE);

        assertThat(output).contains(DummyLoggingAnalyzer.INFO_LOG_MESSAGE);
        assertThat(output).doesNotContain(DummyLoggingAnalyzer.DEBUG_LOG_MESSAGE);
    }

    @Test
    public void testLogLevelCanBeSwitchedToDebug() {
        String output = runAntennaAndCaptureLogOutput(AntennaCLIOptions.SWITCH_DEBUG_SHORT, PARAM_CONFIG_FILE);

        assertThat(output).contains(DummyLoggingAnalyzer.INFO_LOG_MESSAGE);
        assertThat(output).contains(DummyLoggingAnalyzer.DEBUG_LOG_MESSAGE);
    }

    @Test
    public void testCommandLineIsValidated() {
        String output = runAntennaAndExpectFailure(PARAM_CONFIG_FILE, "--unsupported-argument");

        assertThat(output).contains(AntennaCLIOptions.helpMessage());
    }

    @Test
    public void testNonExistingConfigFileIsHandled() {
        String nonExistingPath = "/non/existing/config.xml";
        String output = runAntennaAndExpectFailure(nonExistingPath);

        assertThat(output).contains(Arrays.asList("Cannot find ", nonExistingPath));
    }
}
