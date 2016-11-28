/*
 * BSD 2-Clause License
 *
 * Copyright (c) 2015, Jochen Seeber
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.sda.gradle

import groovy.transform.TypeChecked

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet

@TypeChecked
public class WsimportPlugin implements Plugin<Project> {
    void apply(Project project) {
        project.with {
            plugins.apply(JavaPlugin)

            configurations.create("wsimport").with {
                description = "The JAX-WS libraries used for the wsimport task."
                visible = false
                transitive = true
                extendsFrom(configurations.getAt("compile"))
            }

            dependencies.add("wsimport", [group: "com.sun.xml.ws", name: "jaxws-tools", version: "2.2.10"])

            JavaPluginConvention java = project.convention.getPlugin(JavaPluginConvention)

            Task eclipseClasspath = tasks.findByName("eclipseClasspath")

            java.sourceSets.all { SourceSet sourceSet ->
                String taskName = sourceSet.getTaskName("wsimport", "")
                File wsdlDir = file("src/${sourceSet.name}/wsdl")

                if(wsdlDir.directory) {
                    String infix = sourceSet.name == "main" ? "" : "-${sourceSet.name}"
                    File javaDir = file("src/${infix}/java")

                    WsimportTask wsimportTask = tasks.create(taskName, WsimportTask)
                    wsimportTask.with {
                        description = "Generate JAX-WS code from WSDL for source set ${sourceSet.name}."
                        inputDir = wsdlDir
                        outputDir = javaDir
                        group = "generated"
                    }

                    sourceSet.java.srcDir(javaDir)
                    sourceSet.resources.srcDir(wsdlDir)

                    tasks[sourceSet.compileJavaTaskName].dependsOn(wsimportTask)

                    if(eclipseClasspath != null) {
                        eclipseClasspath.dependsOn(wsimportTask)
                    }
                }
            }
        }
    }
}


