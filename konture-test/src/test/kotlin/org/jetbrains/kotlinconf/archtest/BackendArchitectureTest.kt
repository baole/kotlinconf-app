package org.jetbrains.kotlinconf.archtest

import io.github.baole.konture.*
import org.junit.jupiter.api.Test

class BackendArchitectureTest {

    @Test
    fun `naming and package convention rules for backend layers`() {
        // Routes must reside in .routes package and end with Routes
        Konture.classes()
            .that { 
                packageName.endsWith(".backend.routes") && 
                !name.endsWith("Kt") && 
                !name.contains("Dto") && 
                !name.endsWith("Info") &&
                Modifier.DATA !in modifiers
            }
            .should().haveNameEndingWith("Routes")
            .check()

        // Services must reside in .services package and end with Service
        Konture.classes()
            .that { packageName.endsWith(".backend.services") && !name.endsWith("Kt") }
            .should().haveNameEndingWith("Service")
            .check()

        // Repositories must reside in .repositories package and end with Repository
        Konture.classes()
            .that { packageName.endsWith(".backend.repositories") && !name.endsWith("Kt") }
            .should().haveNameEndingWith("Repository")
            .check()
    }

    @Test
    fun `routes must not directly depend on repositories or database schemas`() {
        // Forces files in routes package to delegate business logic to Services, not Repositories or schemas
        Konture.files()
            .that().resideInAPackage("org.jetbrains.kotlinconf.backend.routes")
            .should().satisfy { fileCtx, violations ->
                for (imp in fileCtx.declaration.imports) {
                    if (imp.contains(".backend.repositories") || imp.contains(".backend.schema")) {
                        violations.add("Route file ${fileCtx.declaration.name} directly imports database schema or repository component: $imp")
                    }
                }
            }
            .check()
    }

    @Test
    fun `repositories should be encapsulated and declared internal`() {
        // Enforce internal access modifier for all repository classes
        Konture.classes()
            .that { packageName.contains(".backend.repositories") && !name.endsWith("Kt") }
            .should().beInternal()
            .check()
    }
}
