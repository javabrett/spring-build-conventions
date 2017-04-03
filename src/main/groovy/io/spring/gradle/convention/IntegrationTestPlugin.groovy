/*
 * Copyright 2002-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package io.spring.gradle.convention

import org.gradle.api.plugins.GroovyPlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.bundling.Zip
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.testing.Test
import org.gradle.plugins.ide.eclipse.EclipsePlugin

/**
 *
 * Adds support for integration tests to java projects.
 *
 * <ul>
 * <li>Adds integrationTestCompile and integrationTestRuntime configurations</li>
 * <li>A new source test folder of src/integration-test/java has been added</li>
 * <li>A task to run integration tests named integrationTest is added</li>
 * <li>If Groovy plugin is added a new source test folder src/integration-test/groovy is added</li>
 * </ul>
 *
 * @author Rob Winch
 */
public class IntegrationTestPlugin implements Plugin<Project> {

	@Override
	public void apply(Project project) {
		project.plugins.withType(JavaPlugin.class) {
			applyJava(project)
		}
	}

	private applyJava(Project project) {
		project.configurations {
			integrationTestCompile {
				extendsFrom testCompile, optional, provided
			}
			integrationTestRuntime {
				extendsFrom integrationTestCompile, testRuntime
			}
		}

		project.sourceSets {
			integrationTest {
				java.srcDir project.file('src/integration-test/java')
				resources.srcDir project.file('src/integration-test/resources')
				compileClasspath = project.sourceSets.main.output + project.sourceSets.test.output + project.configurations.integrationTestCompile
				runtimeClasspath = output + compileClasspath + project.configurations.integrationTestRuntime
			}
		}

		Task integrationTestTask = project.tasks.create("integrationTest", Test) {
			group = 'Verification'
			description = 'Runs the integration tests.'
			dependsOn 'jar'
			testClassesDir = project.sourceSets.integrationTest.output.classesDir
			classpath = project.sourceSets.integrationTest.runtimeClasspath
			reports {
				html.destination = project.file("$project.buildDir/reports/integration-tests/")
				junitXml.destination = project.file("$project.buildDir/integration-test-results/")
			}
			shouldRunAfter project.tasks.test
		}
		project.tasks.check.dependsOn integrationTestTask

		project.plugins.withType(GroovyPlugin) {
			project.sourceSets {
				integrationTest {
					groovy.srcDirs project.file('src/integration-test/groovy')
				}
			}
		}

		project.plugins.withType(EclipsePlugin) {
			project.eclipse.classpath {
				plusConfigurations += [ project.configurations.integrationTestCompile ]
			}
		}
	}
}
