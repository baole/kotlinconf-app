package org.jetbrains.kotlinconf.archtest

import io.github.baole.konture.*
import org.junit.jupiter.api.Test

class ModuleBoundaryTest {

    @Test
    fun `no circular dependencies allowed in the entire project`() {
        // Enforce a strict directed acyclic graph (DAG) on our modules
        Konture.assertNoCycles()
    }

    @Test
    fun `core module must remain a pure leaf dependency`() {
        // Core module contains only schemas/types shared between backend & app
        Konture.modules()
            .that { path == ":core" }
            .should {
                for (dep in dependencies) {
                    addViolation("Core module should not depend on any other subprojects, but depends on: ${dep.targetPath}")
                }
            }
            .check()
    }

    @Test
    fun `client app modules must not depend on backend implementation`() {
        Konture.modules()
            .that { path.startsWith(":app:") }
            .should().notDependOnModule(":backend")
            .check()
    }

    @Test
    fun `backend must not depend on front-end client-specific modules`() {
        Konture.modules()
            .that { path == ":backend" }
            .should {
                for (dep in dependencies) {
                    if (dep.targetPath.startsWith(":app:") && dep.targetPath != ":app:adminApp") {
                        addViolation("Backend should never import client-facing app modules, but depends on: ${dep.targetPath}")
                    }
                }
            }
            .check()
    }

    @Test
    fun `core module classes must not import backend or app packages`() {
        // Enforce boundary separation on a class import level
        Konture.classes()
            .that { filePath.contains("/core/src/") && !name.endsWith("Kt") }
            .should {
                for (imp in imports) {
                    if (imp.contains("org.jetbrains.kotlinconf.backend") || imp.contains("org.jetbrains.kotlinconf.screens") || imp.contains("org.jetbrains.kotlinconf.navigation")) {
                        addViolation("Core class $fqName must not import backend or app components: $imp")
                    }
                }
            }
            .check()
    }
}
