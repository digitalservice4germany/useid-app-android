package de.digitalService.useID.ui.coordinators

import android.content.Context
import com.ramcosta.composedestinations.spec.Direction
import dagger.hilt.android.qualifiers.ApplicationContext
import de.digitalService.useID.getLogger
import de.digitalService.useID.idCardInterface.EIDInteractionEvent
import de.digitalService.useID.idCardInterface.IDCardManager
import de.digitalService.useID.ui.AppCoordinator
import de.digitalService.useID.ui.composables.screens.destinations.IdentificationAttributeConsentDestination
import de.digitalService.useID.ui.composables.screens.destinations.IdentificationPersonalPINDestination
import de.digitalService.useID.ui.composables.screens.destinations.IdentificationScanDestination
import de.digitalService.useID.ui.composables.screens.destinations.IdentificationSuccessDestination
import de.digitalService.useID.ui.composables.screens.identification.ScanEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IdentificationCoordinator @Inject constructor(
    @ApplicationContext private val context: Context,
    val appCoordinator: AppCoordinator,
    val idCardManager: IDCardManager
) {
    private val logger by getLogger()

    private val _scanEventFlow: MutableStateFlow<ScanEvent> = MutableStateFlow(ScanEvent.CardRequested)
    val scanEventFlow: StateFlow<ScanEvent>
        get() = _scanEventFlow

    private var requestAuthenticationEvent: EIDInteractionEvent.RequestAuthenticationRequestConfirmation? = null
    private var pinCallback: ((String) -> Unit)? = null

    fun startIdentificationProcess() {
        startIdentification()
    }

    fun confirmAttributesForIdentification() {
        val requestAuthenticationEvent = requestAuthenticationEvent ?: run {
            logger.error("Cannot confirm attributes because there isn't any authentication confirmation request event saved.")
            return
        }

        val requiredAttributes = requestAuthenticationEvent.request.readAttributes.filterValues { it }
        requestAuthenticationEvent.confirmationCallback(requiredAttributes)
    }

    fun onPINEntered(pin: String) {
        val pinCallback = pinCallback ?: run {
            logger.error("Cannot process PIN because there isn't any pin callback saved.")
            return
        }
        pinCallback(pin)
        this.pinCallback = null
    }

    private fun startIdentification() {
        val demoURL =
            "http://127.0.0.1:24727/eID-Client?tcTokenURL=https%3A%2F%2Ftest.governikus-eid.de%2FAutent-DemoApplication%2FRequestServlet%3Fprovider%3Ddemo_epa_20%26redirect%3Dtrue"

        CoroutineScope(Dispatchers.IO).launch {
            idCardManager.identify(context, demoURL).catch {
                logger.error("Error: $it")
            }.collect { event ->
                when (event) {
                    EIDInteractionEvent.AuthenticationStarted -> logger.debug("Authentication started")
                    is EIDInteractionEvent.RequestAuthenticationRequestConfirmation -> {
                        logger.debug(
                            "Requesting authentication confirmation:\n" +
                                "${event.request.subject}\n" +
                                "Read attributes: ${event.request.readAttributes.keys}"
                        )

                        requestAuthenticationEvent = event

                        navigateOnMain(IdentificationAttributeConsentDestination(event.request))
                    }
                    is EIDInteractionEvent.RequestPIN -> {
                        logger.debug("Requesting PIN")

                        pinCallback = event.pinCallback

                        navigateOnMain(IdentificationPersonalPINDestination(event.attempts, false))
                    }
                    EIDInteractionEvent.RequestCardInsertion -> {
                        logger.debug("Requesting ID card")
                        navigateOnMain(IdentificationScanDestination)
                    }
                    EIDInteractionEvent.CardRecognized -> {
                        logger.debug("Card recognized")
                        _scanEventFlow.emit(ScanEvent.CardAttached)
                    }
                    is EIDInteractionEvent.ProcessCompletedSuccessfully -> {
                        logger.debug("Process completed successfully")
                        _scanEventFlow.emit(ScanEvent.Finished)

                        // Handle refresh address here ...

                        requestAuthenticationEvent?.request?.subject?.let {
                            navigateOnMain(IdentificationSuccessDestination(it))
                        }
                    }
                    else -> logger.debug("Unhandled authentication event: $event")
                }
            }
        }
    }

    private fun navigateOnMain(direction: Direction) {
        CoroutineScope(Dispatchers.Main).launch { appCoordinator.navigate(direction) }
    }
}
