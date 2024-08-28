/*
 * Copyright (C) 2017 The ORT Project Authors (see <https://github.com/oss-review-toolkit/ort/blob/main/NOTICE>)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * License-Filename: LICENSE
 */

package org.ossreviewtoolkit.plugins.packagemanagers.maven

import java.io.File

import org.apache.maven.model.io.xpp3.MavenXpp3Reader
import org.apache.maven.project.DefaultProjectBuildingRequest
import org.apache.maven.project.MavenProject
import org.apache.maven.repository.RepositorySystem
import org.apache.maven.repository.internal.MavenRepositorySystemUtils
import org.apache.maven.shared.dependency.graph.internal.DefaultDependencyCollectorBuilder
import org.eclipse.aether.internal.impl.EnhancedLocalRepositoryManagerFactory
import org.eclipse.aether.repository.LocalRepository

import org.eclipse.aether.supplier.RepositorySystemSupplier

import org.ossreviewtoolkit.analyzer.AbstractPackageManagerFactory
import org.ossreviewtoolkit.analyzer.PackageManager
import org.ossreviewtoolkit.model.Project
import org.ossreviewtoolkit.model.ProjectAnalyzerResult
import org.ossreviewtoolkit.model.config.AnalyzerConfiguration
import org.ossreviewtoolkit.model.config.RepositoryConfiguration

/**
 * The [Maven](https://maven.apache.org/) package manager for Java.
 */
class Maven(
    name: String,
    analysisRoot: File,
    analyzerConfig: AnalyzerConfiguration,
    repoConfig: RepositoryConfiguration
) : PackageManager(name, analysisRoot, analyzerConfig, repoConfig) {
    class Factory : AbstractPackageManagerFactory<Maven>("Maven") {
        override val globsForDefinitionFiles = listOf("pom.xml")

        override fun create(
            analysisRoot: File,
            analyzerConfig: AnalyzerConfiguration,
            repoConfig: RepositoryConfiguration
        ) = Maven(type, analysisRoot, analyzerConfig, repoConfig)
    }

    override fun resolveDependencies(definitionFile: File, labels: Map<String, String>): List<ProjectAnalyzerResult> {
        val model = definitionFile.inputStream()
            .use { MavenXpp3Reader().read(it) }
            .apply { pomFile = definitionFile }

        val repositorySystem = RepositorySystemSupplier().get()

        //EnhancedLocalRepositoryManagerFactory().new
        val session = MavenRepositorySystemUtils.newSession().apply {
            localRepositoryManager = repositorySystem.newLocalRepositoryManager(this, LocalRepository(RepositorySystem.defaultUserLocalRepository))
        }

        val buildingRequest = DefaultProjectBuildingRequest().apply {
            project = MavenProject(model)
            repositorySession = session
        }


        val dependencyCollectorBuilder = DefaultDependencyCollectorBuilder(repositorySystem)
        val rootNode = dependencyCollectorBuilder.collectDependencyGraph(buildingRequest, /* filter = */ null)

        println(rootNode)

        repositorySystem.shutdown()

        return listOf(ProjectAnalyzerResult(Project.EMPTY, packages = emptySet()))
    }

}
