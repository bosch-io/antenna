[![Build Status](https://dev.azure.com/deo1imb/playground/_apis/build/status/bsinno.antenna?branchName=master)](https://dev.azure.com/deo1imb/playground/_build/latest?definitionId=1&branchName=master)

# Antenna

Antenna scans artifacts of a project, downloads sources for dependencies, 
validates sources and licenses and creates:

* a third-party disclosure document that lists all dependencies with 
their licenses,
* a sources.zip containing all sources of the dependencies, and
* a processing report.

Learn more about Antenna in [What Antenna Does](antenna-documentation/src/site/markdown/index.md.vm).

### Install and build Antenna

Please note that some dependencies of SW360antenna are only available for Java 8. So you need to use Java 8 to build the project.

If you want to build Antenna on the command line, just use Maven like

    $ mvn install

By default, this will run tests. If you want to skip running tests use

    $ mvn install -DskipTests

#### Optional Profiles
You can activate the following optional profiles:
- `-P integration-test`: activates also the optional profile for integration testing in the sw360 module
- `-P site-tests`: which activates the site tests in `./documentation/`

#### Optional Modules
By default the p2-resolver in `./modules/p2/p2-resolver/`, which resolves OSGi sources via P2 repositories, is excluded from the build (since it complicates the build and is unnecessary in most cases).
To enable it, one can call the corresponding prepare script `./modules/p2/prepareDependenciesForP2.sh` (without Bash support one has to follow the steps in the script by hand).
To remove the P2 dependencies again you can use the script `./modules/p2/cleanupDependenciesForP2.sh`.

### Configure Antenna
Antenna can be used as a Maven plugin, with  Gradle or standalone executable.
As a maven plugin, Antenna's behaviour is configured by adding a `<plugin>` to your project's `pom.xml` file and adding settings to the `<configuration>` section.
Similarly, in Gradle, the same Maven files must be given and the `build.gradle` file needs to include the Antenna configuration.
As a standalone executable, Antenna is configured as an executable jar in the command line.
Find out how to configure Antenna by reading: [How to configure Antenna](antenna-documentation/src/site/markdown/how-to-configure.md.vm).

#### Configure Antenna for Java 9 or newer
Antenna can be used with Java versions 9 or newer.
However, it requires some additional configuration described in [Tool Configuration](antenna-documentation/src/site/markdown/tool-configuration.md.vm/#additional-configuration-for-java-9-or-newer).

 *To find answers in the most frequent questions/problems go to [Troubleshooting](antenna-documentation/src/site/markdown/troubleshooting.md.vm).*
