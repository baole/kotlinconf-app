package org.jetbrains.kotlinconf.archtest

import io.github.baole.konture.Konture
import io.github.baole.konture.files
import io.github.baole.konture.that
import io.github.baole.konture.should
import org.junit.jupiter.api.Test

class PressureArchitectureTest {

    @Test
    fun `backend route files should not import persistence or dependency injection details directly`() {
        Konture.files()
            .that {
                modulePath == ":backend" &&
                    declaration.packageName.startsWith("org.jetbrains.kotlinconf.backend.routes")
            }
            .should {
                val forbiddenImports = imports.filter {
                    it.startsWith("org.jetbrains.kotlinconf.backend.repositories") ||
                        it.startsWith("org.jetbrains.kotlinconf.backend.schema") ||
                        it.startsWith("org.koin")
                }

                if (forbiddenImports.isNotEmpty()) {
                    addViolation(
                        "Backend route file $filePath imports implementation details: " +
                            forbiddenImports.joinToString(),
                    )
                }
            }
            .check()
    }

    @Test
    fun `backend services should not depend on Ktor framework APIs`() {
        Konture.files()
            .that {
                modulePath == ":backend" &&
                    declaration.packageName.startsWith("org.jetbrains.kotlinconf.backend.services")
            }
            .should {
                val ktorImports = imports.filter { it.startsWith("io.ktor") }

                if (ktorImports.isNotEmpty()) {
                    addViolation(
                        "Backend service file $filePath imports Ktor APIs: " +
                            ktorImports.joinToString(),
                    )
                }
            }
            .check()
    }

    @Test
    fun `shared common screens should not import storage or network implementation packages`() {
        Konture.files()
            .that {
                modulePath == ":app:shared" &&
                    declaration.filePath.contains("/src/commonMain/") &&
                    declaration.packageName.startsWith("org.jetbrains.kotlinconf.screens")
            }
            .should {
                val forbiddenImports = imports.filter {
                    it.startsWith("org.jetbrains.kotlinconf.network") ||
                        it.startsWith("org.jetbrains.kotlinconf.storage")
                }

                if (forbiddenImports.isNotEmpty()) {
                    addViolation(
                        "Common screen file $filePath imports data implementation packages: " +
                            forbiddenImports.joinToString(),
                    )
                }
            }
            .check()
    }

    @Test
    fun `shared common root package should not contain Compose UI dependencies`() {
        Konture.files()
            .that {
                modulePath == ":app:shared" &&
                    declaration.filePath.contains("/src/commonMain/") &&
                    declaration.packageName == "org.jetbrains.kotlinconf"
            }
            .should {
                val composeImports = imports.filter { it.startsWith("androidx.compose") }

                if (composeImports.isNotEmpty()) {
                    addViolation(
                        "Shared root file $filePath imports Compose UI APIs: " +
                            composeImports.joinToString(),
                    )
                }
            }
            .check()
    }

    @Test
    fun `core shared contract module should stay free of app backend and framework imports`() {
        Konture.files()
            .that { modulePath == ":core" }
            .should {
                val forbiddenImports = imports.filter {
                    it.startsWith("io.ktor") ||
                        it.startsWith("androidx.compose") ||
                        it.startsWith("android.") ||
                        it.startsWith("org.jetbrains.kotlinconf.backend") ||
                        it.startsWith("org.jetbrains.kotlinconf.screens") ||
                        it.startsWith("org.jetbrains.kotlinconf.network") ||
                        it.startsWith("org.jetbrains.kotlinconf.storage") ||
                        it.startsWith("org.jetbrains.kotlinconf.ui")
                }

                if (forbiddenImports.isNotEmpty()) {
                    addViolation(
                        "Core contract file $filePath imports framework or app/backend details: " +
                            forbiddenImports.joinToString(),
                    )
                }
            }
            .check()
    }
}
