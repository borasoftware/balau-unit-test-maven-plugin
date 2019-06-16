/*
 * Copyright (C) 2019 Bora Software (contact@borasoftware.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.borasoftware.balau;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Execute the Balau unit test application as specified in the plugin's configuration.
 *
 * @author Nicholas Smethurst
 */
@Mojo(name = "test", threadSafe = true, defaultPhase = LifecyclePhase.CLEAN)
public class BalauUnitTestMojo extends AbstractMojo {
	@Parameter(defaultValue = "${project.build.directory}", readonly = true, required = true)
	private File projectBuildDirectory;

	// CMake build directory relative to project base directory.
	@Parameter(defaultValue = "${project.build.directory}/cmake", readonly = true)
	private File cmakeBinaryDirectory;

	// Directory relative to project base directory into which unit test reports will be written.
	@Parameter(defaultValue = "${project.build.directory}/cmake/unitTestReports", readonly = true)
	private File reportDirectory;

	// Test application path relative to cmakeDirectory (default: bin/Tests then Tests).
	@Parameter
	private String appPath;

	// Balau test framework execution model.
	@Parameter(defaultValue = "SingleThreaded", readonly = true)
	private String executionModel;

	// Test class names to include/exclude (default = *IT::*).
	@Parameter
	private List<String> patterns;

	// Custom LD_LIBRARY_PATH to use when running the unit test application (default is empty).
	@Parameter
	private String ldLibraryPath;

	public void execute() throws MojoExecutionException {
		final Log log = getLog();
		final Path cmakeDirectory = getCMakeBinaryDirectory(projectBuildDirectory, cmakeBinaryDirectory);
		final Path appDirectory = getAppDirectory(cmakeDirectory);
		final String appName = getAppName();
		final String execModel = getExecutionModel();

		log.debug("cmakeBinaryDirectory = " + cmakeDirectory);
		log.debug("appDirectory         = " + appDirectory);
		log.debug("appName              = " + appName);
		log.debug("executionModel       = " + execModel);

		final List<String> commandLine = new ArrayList<>();

		commandLine.add(appDirectory.resolve(appName).toString());
		commandLine.add("-e");
		commandLine.add(execModel);

		if (reportDirectory != null) {
			commandLine.add("-r");
			commandLine.add(reportDirectory.getAbsolutePath());
		}

		if (patterns != null && !patterns.isEmpty()) {
			for (String pattern : patterns) {
				commandLine.add(pattern.trim());
			}
		} else {
			commandLine.add("*Test::*");
		}

		try {
			final ProcessBuilder builder = new ProcessBuilder(commandLine);

			builder.directory(appDirectory.toFile());
			builder.redirectErrorStream(true);

			if (ldLibraryPath != null && !ldLibraryPath.isEmpty()) {
				builder.environment().put("LD_LIBRARY_PATH", ldLibraryPath);
			}

			final Process process = builder.start();

			final BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line;

			while ((line = reader.readLine()) != null) {
				log.info(line);
			}

			final int exitStatus = process.waitFor();

			if (exitStatus != 0) {
				throw new MojoExecutionException("The unit test application process failed with exist status " + exitStatus);
			}
		} catch (IOException e) {
			throw new MojoExecutionException("The unit test application process failed.");
		} catch (InterruptedException e) {
			throw new MojoExecutionException("The unit test application process was interrupted.");
		}
	}

	private static Path getCMakeBinaryDirectory(File projectBuildDirectory, File cmakeBinaryDirectory) throws MojoExecutionException {
		if (cmakeBinaryDirectory != null) {
			return cmakeBinaryDirectory.toPath();
		}

		if (projectBuildDirectory == null) {
			throw new MojoExecutionException("${project.build.directory} was not supplied to plugin.");
		}

		return projectBuildDirectory.toPath().resolve("cmake");
	}

	private Path getAppDirectory(Path cmakeDirectory) throws MojoExecutionException {
		if (appPath == null || appPath.isEmpty()) {
			// Check for "bin/Tests" then "Tests".

			final Path bin = cmakeDirectory.resolve("bin");
			Path testBinary = bin.resolve("Tests");

			if (Files.exists(testBinary) && Files.isReadable(testBinary) && Files.isRegularFile(testBinary)) {
				return bin;
			}

			testBinary = cmakeDirectory.resolve("Tests");

			if (Files.exists(testBinary) && Files.isReadable(testBinary) && Files.isRegularFile(testBinary)) {
				return cmakeDirectory.toAbsolutePath();
			}

			throw new MojoExecutionException("No Tests executable found in either " + bin + " or " + cmakeDirectory + " directories.");
		}

		final String[] components = getAppNameComponents();

		if (components.length == 1) {
			return cmakeDirectory.toAbsolutePath();
		}

		Path testAppParentDirectory = cmakeDirectory;

		for (int m = 0; m < components.length - 1; m++) {
			testAppParentDirectory = testAppParentDirectory.resolve(components[m]);
		}

		if (!Files.exists(testAppParentDirectory)
			|| !Files.isReadable(testAppParentDirectory)
			|| !Files.isDirectory(testAppParentDirectory)) {
			throw new MojoExecutionException("No Tests app parent directory found at " + testAppParentDirectory);
		}

		final Path testAppPath = testAppParentDirectory.resolve(components[components.length - 1]);

		if (!Files.exists(testAppPath)
			|| !Files.isReadable(testAppPath)
			|| !Files.isRegularFile(testAppPath)) {
			throw new MojoExecutionException("No Tests executable found at " + testAppPath);
		}

		return testAppParentDirectory.toAbsolutePath();
	}

	private String getAppName() {
		if (appPath != null && !appPath.isEmpty()) {
			final String[] components = getAppNameComponents();
			return components[components.length - 1];
		}

		return "Tests";
	}

	private String[] getAppNameComponents() {
		return appPath.trim().split("/|\\\\");
	}

	private String getExecutionModel() {
		return executionModel == null || executionModel.isEmpty() ? "SingleThreaded" : executionModel.trim();
	}
}
