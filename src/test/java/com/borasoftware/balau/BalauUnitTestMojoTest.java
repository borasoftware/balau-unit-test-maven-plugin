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

import org.apache.maven.plugin.testing.AbstractMojoTestCase;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class BalauUnitTestMojoTest extends AbstractMojoTestCase {
	static final String testProjectLocation = "src/test/test-project";

	class TestFiles {
		final File pom;
		final File targetDirectory;
		final File cmakeTargetDirectory;
		final File reportDirectory;
		final List<File> reportFiles;

		TestFiles(File pom,
		          File targetDirectory,
		          File cmakeTargetDirectory,
		          File reportDirectory,
		          List<File> reportFiles) {
			this.pom = pom;
			this.targetDirectory = targetDirectory;
			this.cmakeTargetDirectory = cmakeTargetDirectory;
			this.reportDirectory = reportDirectory;
			this.reportFiles = reportFiles;
		}
	}

	private TestFiles testFiles;

	protected void setUp() throws Exception {
		super.setUp();
		testFiles = locateTestFiles();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void test() throws Exception {
		assertNotNull(testFiles.pom);
		assertTrue(testFiles.pom.exists());

		final BalauUnitTestMojo mojo = (BalauUnitTestMojo) lookupMojo("test", testFiles.pom);
		assertNotNull(mojo);
		mojo.execute();

		final Path appExecutable = testFiles.cmakeTargetDirectory.toPath().resolve("bin").resolve("Tests");
		assertTrue(Files.exists(appExecutable));

		assertTrue(Files.exists(testFiles.reportDirectory.toPath()));

		for (File reportFile : testFiles.reportFiles) {
			assertTrue(Files.exists(reportFile.toPath()));
		}
	}

	private TestFiles locateTestFiles() {
		final File pom = AbstractMojoTestCase.getTestFile(testProjectLocation + "/pom.xml");
		final File targetDirectory = new File(pom.getParentFile(), "target");
		final File cmakeTargetDirectory = new File(targetDirectory, "cmake");
		final File reportDirectory = new File(cmakeTargetDirectory, "unitTestReports");

		final List<File> reportFiles = new ArrayList<File>() {{
			new File(reportDirectory, "LibTest.xml");
		}};

		return new TestFiles(
			  pom
			, targetDirectory
			, cmakeTargetDirectory
			, reportDirectory
			, reportFiles
		);
	}
}
