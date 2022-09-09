package de.digitalService.useID.ui.coordinators

import de.digitalService.useID.SecureStorageManagerInterface
import de.digitalService.useID.ui.screens.destinations.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SetupCoordinator @Inject constructor(
    private val appCoordinator: AppCoordinator,
    private val secureStorageManager: SecureStorageManagerInterface
) {
    private var tcTokenURL: String? = null

    fun setTCTokenURL(tcTokenURL: String) {
        this.tcTokenURL = tcTokenURL
    }

    fun identificationPending(): Boolean {
        return this.tcTokenURL != null
    }

    fun startSetupIDCard() {
        secureStorageManager.clearStorage()
        appCoordinator.navigate(SetupPINLetterDestination)
    }

    fun setupWithPINLetter() {
        appCoordinator.navigate(SetupTransportPINDestination)
    }

    fun setupWithoutPINLetter() {
        appCoordinator.navigate(SetupResetPersonalPINDestination)
    }

    fun onTransportPINEntered() {
        appCoordinator.navigate(SetupPersonalPINIntroDestination)
    }

    fun onPersonalPINIntroFinished() {
        appCoordinator.navigate(SetupPersonalPINDestination)
    }

    fun onPersonalPINEntered() {
        appCoordinator.navigate(SetupScanDestination)
    }

    fun onSettingPINSucceeded() {
        appCoordinator.setIsNotFirstTimeUser()
        appCoordinator.navigate(SetupFinishDestination)
    }

    fun onSetupFinished() {
        handleSetupEnded()
    }

    fun onBackToHome() {
        appCoordinator.popToRoot()
    }

    fun onBackTapped() {
        appCoordinator.pop()
    }

    fun onSkipSetup() {
        handleSetupEnded()
    }

    fun cancelSetup() {
        secureStorageManager.clearStorage()
        appCoordinator.popToRoot()
        tcTokenURL = null
    }

    private fun handleSetupEnded() {
        secureStorageManager.clearStorage()

        tcTokenURL?.let {
            appCoordinator.startIdentification(it)
            tcTokenURL = null
        } ?: run {
            appCoordinator.popToRoot()
        }
    }
}
