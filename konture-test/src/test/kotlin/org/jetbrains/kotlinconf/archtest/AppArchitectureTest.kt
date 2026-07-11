package org.jetbrains.kotlinconf.archtest

import io.github.baole.konture.*
import org.junit.jupiter.api.Test

class AppArchitectureTest {

    @Test
    fun `viewmodels must extend androidx lifecycle ViewModel and reside in correct packages`() {
        Konture.classes()
            .that { name.endsWith("ViewModel") && !name.contains("Test") && visibility != Visibility.PRIVATE }
            .should {
                // Enforce superclass
                if (supertypes.none { it.endsWith("ViewModel") }) {
                    addViolation("Class $fqName ends with 'ViewModel' but does not extend androidx.lifecycle.ViewModel")
                }
                // Enforce directory location
                if (!packageName.contains(".screens")) {
                    addViolation("ViewModel $fqName should reside in screen-scoped package directory (.screens)")
                }
            }
            .check()
    }

    @Test
    fun `viewmodels must be annotated for Metro DI injection`() {
        // Views / Screens resolve ViewModels via default params (e.g., viewModel = metroViewModel())
        // ViewModels must have Metro metadata annotations to avoid runtime compilation injection errors
        Konture.classes()
            .that { name.endsWith("ViewModel") && !name.contains("Test") && visibility != Visibility.PRIVATE }
            .should {
                val hasContributes = annotations.any { it.name == "ContributesIntoMap" || it.fqName == "dev.zacsweers.metro.ContributesIntoMap" }
                val hasViewModelKey = annotations.any { it.name == "ViewModelKey" || it.fqName == "dev.zacsweers.metrox.viewmodel.ViewModelKey" }
                val isAssisted = annotations.any { it.name == "AssistedInject" || it.fqName == "dev.zacsweers.metro.AssistedInject" }
                
                if (!(hasContributes && hasViewModelKey) && !isAssisted) {
                    addViolation("ViewModel $fqName must be annotated with @ContributesIntoMap and @ViewModelKey, or be annotated with @AssistedInject")
                }
            }
            .check()
    }

    @Test
    fun `viewmodels must be platform agnostic and not reference Android views or contexts`() {
        Konture.classes()
            .that { name.endsWith("ViewModel") && !name.contains("Test") }
            .should {
                val forbiddenImports = listOf("android.content", "android.view", "android.widget", "android.app")
                for (imp in imports) {
                    if (forbiddenImports.any { imp.startsWith(it) }) {
                        addViolation("KMP ViewModel $fqName should be platform-agnostic, but references Android package: $imp")
                    }
                }
            }
            .check()
    }

    @Test
    fun `composables must not accept repository or service parameters directly`() {
        // Views must depend on their matching ViewModels, not data layers
        Konture.functions()
            .that { hasAnnotation("Composable") && modulePath.startsWith(":app:") }
            .should {
                noneParameterMatches("accepts raw repository or database references") { param ->
                    param.type.endsWith("Repository") || param.type.endsWith("Database") || param.type.endsWith("Service")
                }
            }
            .check()
    }

    @Test
    fun `app layered architecture dependencies must flow in one direction`() {
        // Enforce clean layered architecture on the client side:
        // 1. Data, Storage and Network layers must NOT depend on Presentation/UI layers.
        // 2. Core Business logic / Service layers must NOT depend on Presentation/UI layers.
        // 3. UI screens and navigation classes must NOT import DI graph/wiring components directly.
        Konture.classes()
            .that { 
                packageName.startsWith("org.jetbrains.kotlinconf") && 
                !packageName.startsWith("org.jetbrains.kotlinconf.backend") && 
                !name.endsWith("Kt") 
            }
            .should {
                val isNetworkOrData = packageName.contains(".network") || packageName.contains(".storage")
                val isServiceOrLogic = fqName.startsWith("org.jetbrains.kotlinconf.ConferenceService") || packageName.contains(".flags")
                val isPresentation = packageName.contains(".screens") || packageName.contains(".navigation")
                
                if (isNetworkOrData || isServiceOrLogic) {
                    for (imp in imports) {
                        if (imp.contains(".screens.") || imp.contains(".navigation.")) {
                            addViolation("Non-presentation class $fqName must not import presentation components: $imp")
                        }
                    }
                }
                
                if (isPresentation) {
                    for (imp in imports) {
                        if (imp.contains(".di.")) {
                            addViolation("Presentation/UI class $fqName must not directly import DI components: $imp")
                        }
                    }
                }
            }
            .check()
    }

    @Test
    fun `global viewmodel inheritance constraint`() {
        // Enforce that any class in the workspace extending ViewModel must reside in :app:shared .screens and end with ViewModel
        Konture.classes()
            .that { supertypes.any { it.endsWith("ViewModel") } && !name.endsWith("Kt") && visibility != Visibility.PRIVATE }
            .should {
                val isSharedAppModule = filePath.contains("/app/shared/") || filePath.contains("/shared/src/")
                if (!isSharedAppModule || !packageName.contains(".screens") || !name.endsWith("ViewModel")) {
                    addViolation("Class $fqName extends ViewModel but is illegally defined or located (must reside in :app:shared screen packages, and end with 'ViewModel', filePath: $filePath)")
                }
            }
            .check()
    }
}
