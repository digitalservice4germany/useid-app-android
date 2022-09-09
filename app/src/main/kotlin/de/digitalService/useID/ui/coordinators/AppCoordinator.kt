package de.digitalService.useID.ui.coordinators

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.navigation.NavController
import com.ramcosta.composedestinations.navigation.navigate
import com.ramcosta.composedestinations.spec.Direction
import de.digitalService.useID.StorageManagerType
import de.digitalService.useID.models.NfcAvailability
import de.digitalService.useID.ui.screens.destinations.HomeScreenDestination
import de.digitalService.useID.ui.screens.destinations.IdentificationFetchMetadataDestination
import de.digitalService.useID.ui.screens.destinations.SetupIntroDestination
import javax.inject.Inject
import javax.inject.Singleton

interface AppCoordinatorType {
    val nfcAvailability: State<NfcAvailability>

    fun setNavController(navController: NavController)
    fun navigate(route: Direction)
    fun pop()
    fun popToRoot()
    fun startIdSetup(tcTokenURL: String?)
    fun startIdentification(tcTokenURL: String)
    fun homeScreenLaunched()
    fun setNfcAvailability(availability: NfcAvailability)
    fun setIsNotFirstTimeUser()
}

@Singleton
class AppCoordinator @Inject constructor(
    private val storageManager: StorageManagerType
) : AppCoordinatorType {
    private lateinit var navController: NavController

    var tcTokenURL: String? = null

    private var coldLaunch: Boolean = true

    override val nfcAvailability: MutableState<NfcAvailability> = mutableStateOf(NfcAvailability.Available)

    override fun setNavController(navController: NavController) {
        this.navController = navController
    }

    override fun navigate(route: Direction) = navController.navigate(route)

    override fun pop() {
        navController.popBackStack()
    }

    override fun popToRoot() {
        navController.popBackStack(route = HomeScreenDestination.route, inclusive = false)
    }

    override fun startIdSetup(tcTokenURL: String?) {
        popToRoot()
        navigate(SetupIntroDestination(tcTokenURL))
    }

    override fun startIdentification(tcTokenURL: String) = navController.navigate(IdentificationFetchMetadataDestination(tcTokenURL))

    override fun homeScreenLaunched() {
        if (storageManager.getIsFirstTimeUser()) {
            if (coldLaunch || tcTokenURL != null) {
                startIdSetup(tcTokenURL)
            }
        } else {
            tcTokenURL?.let { startIdentification(it) }
        }

        coldLaunch = false
        tcTokenURL = null
    }

    override fun setNfcAvailability(availability: NfcAvailability) {
        nfcAvailability.value = availability
    }

    override fun setIsNotFirstTimeUser() {
        storageManager.setIsNotFirstTimeUser()
    }
}
