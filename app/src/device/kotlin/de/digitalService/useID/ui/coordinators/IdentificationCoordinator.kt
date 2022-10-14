package de.digitalService.useID.ui.coordinators

import android.content.Context
import android.net.Uri
import com.ramcosta.composedestinations.spec.Direction
import dagger.hilt.android.qualifiers.ApplicationContext
import de.digitalService.useID.analytics.IssueTrackerManagerType
import de.digitalService.useID.analytics.TrackerManagerType
import de.digitalService.useID.getLogger
import de.digitalService.useID.idCardInterface.EIDInteractionEvent
import de.digitalService.useID.idCardInterface.IDCardInteractionException
import de.digitalService.useID.idCardInterface.IDCardManager
import de.digitalService.useID.models.ScanError
import de.digitalService.useID.ui.screens.destinations.IdentificationAttributeConsentDestination
import de.digitalService.useID.ui.screens.destinations.IdentificationPersonalPINDestination
import de.digitalService.useID.ui.screens.destinations.IdentificationScanDestination
import de.digitalService.useID.ui.screens.destinations.IdentificationSuccessDestination
import de.digitalService.useID.ui.screens.identification.FetchMetadataEvent
import de.digitalService.useID.ui.screens.identification.ScanEvent
import de.digitalService.useID.util.CoroutineContextProviderType
import io.sentry.Sentry
import io.sentry.SentryEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IdentificationCoordinator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appCoordinator: AppCoordinator,
    private val idCardManager: IDCardManager,
    private val trackerManager: TrackerManagerType,
    private val issueTrackerManager: IssueTrackerManagerType,
    private val coroutineContextProvider: CoroutineContextProviderType
) {
    private val logger by getLogger()

    private val _fetchMetadataEventFlow: MutableStateFlow<FetchMetadataEvent> = MutableStateFlow(FetchMetadataEvent.Started)
    val fetchMetadataEventFlow: StateFlow<FetchMetadataEvent>
        get() = _fetchMetadataEventFlow

    private val _scanEventFlow: MutableStateFlow<ScanEvent> = MutableStateFlow(ScanEvent.CardRequested)
    val scanEventFlow: Flow<ScanEvent>
        get() = _scanEventFlow

    private var requestAuthenticationEvent: EIDInteractionEvent.RequestAuthenticationRequestConfirmation? = null
    private var pinCallback: ((String) -> Unit)? = null

    private var reachedScanState = false

    fun startIdentificationProcess(tcTokenURL: String) {
        logger.debug("Start identification process.")
        idCardManager.cancelTask()
        reachedScanState = false
        CoroutineScope(coroutineContextProvider.IO).launch {
            _scanEventFlow.emit(ScanEvent.CardRequested)
        }
        startIdentification(tcTokenURL)
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
        logger.debug("Executing PIN callback.")
        pinCallback(pin)
        this.pinCallback = null
    }

    fun cancelIdentification() {
        logger.debug("Cancel identification process.")
        appCoordinator.popToRoot()
        idCardManager.cancelTask()
        reachedScanState = false
    }

    fun finishIdentification() {
        logger.debug("Finish identification process.")
        appCoordinator.setIsNotFirstTimeUser()
        appCoordinator.popToRoot()
        reachedScanState = false
        trackerManager.trackEvent(category = "identification", action = "buttonPressed", name = "continueToService")
    }

    private fun startIdentification(tcTokenURL: String) {
        val fullURL = Uri
            .Builder()
            .scheme("http")
            .encodedAuthority("127.0.0.1:24727")
            .appendPath("eID-Client")
            .appendQueryParameter("tcTokenURL", tcTokenURL)
            .build()
            .toString()

        CoroutineScope(coroutineContextProvider.IO).launch {
            _fetchMetadataEventFlow.emit(FetchMetadataEvent.Started)

            idCardManager.identify(context, fullURL).catch { error ->
                logger.error("Identification error: $error")

                when (error) {
                    IDCardInteractionException.CardDeactivated -> {
                        trackerManager.trackScreen("identification/cardDeactivated")
                        _scanEventFlow.emit(ScanEvent.Error(ScanError.CardDeactivated))
                    }
                    IDCardInteractionException.CardBlocked -> {
                        trackerManager.trackScreen("identification/cardUnreadable")
                        _scanEventFlow.emit(ScanEvent.Error(ScanError.CardBlocked))
                    }
                    is IDCardInteractionException.ProcessFailed -> {
                        _fetchMetadataEventFlow.emit(FetchMetadataEvent.Error)

                        val scanEvent = if (error.redirectUrl != null) {
                            ScanEvent.Error(ScanError.CardErrorWithRedirect(error.redirectUrl))
                        } else {
                            ScanEvent.Error(ScanError.CardErrorWithoutRedirect)
                        }

                        _scanEventFlow.emit(scanEvent)
                    }
                    else -> {
                        _fetchMetadataEventFlow.emit(FetchMetadataEvent.Error)
                        _scanEventFlow.emit(ScanEvent.Error(ScanError.Other(null)))

                        if (pinCallback == null && !reachedScanState) {
                            trackerManager.trackEvent(category = "identification", action = "loadingFailed", name = "attributes")

                            (error as? IDCardInteractionException)?.redacted?.let {
                                issueTrackerManager.capture(it)
                            }
                        }
                    }
                }
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

                        _fetchMetadataEventFlow.emit(FetchMetadataEvent.Finished)
                        navigateOnMain(IdentificationAttributeConsentDestination(event.request))
                    }
                    is EIDInteractionEvent.RequestPIN -> {
                        logger.debug("Requesting PIN")

                        pinCallback = event.pinCallback

                        if (event.attempts == null) {
                            logger.debug("PIN request without attempts")
                            navigateOnMain(IdentificationPersonalPINDestination)
                        } else {
                            logger.debug("PIN request with ${event.attempts} attempts")
                            _scanEventFlow.emit(ScanEvent.Error(ScanError.IncorrectPIN(attempts = event.attempts)))
                        }
                    }
                    is EIDInteractionEvent.RequestCAN -> {
                        logger.debug("Requesting CAN")
                        _scanEventFlow.emit(ScanEvent.Error(ScanError.PINSuspended))
                        trackerManager.trackScreen("identification/cardSuspended")
                        cancel()
                    }
                    is EIDInteractionEvent.RequestPINAndCAN -> {
                        logger.debug("Requesting PIN and CAN")
                        _scanEventFlow.emit(ScanEvent.Error(ScanError.PINSuspended))
                        trackerManager.trackScreen("identification/cardSuspended")
                        cancel()
                    }
                    is EIDInteractionEvent.RequestPUK -> {
                        logger.debug("Requesting PUK")
                        _scanEventFlow.emit(ScanEvent.Error(ScanError.PINBlocked))
                        trackerManager.trackScreen("identification/cardBlocked")
                        cancel()
                    }
                    EIDInteractionEvent.RequestCardInsertion -> {
                        logger.debug("Requesting ID card")
                        if (!reachedScanState) {
                            navigateOnMain(IdentificationScanDestination)
                        }
                    }
                    EIDInteractionEvent.CardRecognized -> {
                        logger.debug("Card recognized")
                        _scanEventFlow.emit(ScanEvent.CardAttached)
                        reachedScanState = true
                    }
                    is EIDInteractionEvent.ProcessCompletedSuccessfullyWithRedirect -> {
                        logger.debug("Process completed successfully")
                        _scanEventFlow.emit(ScanEvent.Finished)

                        requestAuthenticationEvent?.request?.subject?.let { subject ->
                            navigateOnMain(IdentificationSuccessDestination(subject, event.redirectURL))
                        }
                    }
                    else -> {
                        logger.debug("Unhandled authentication event: $event")
                        issueTrackerManager.capture(event.redacted)
                    }
                }
            }
        }
    }

    private fun navigateOnMain(direction: Direction) {
        CoroutineScope(Dispatchers.Main).launch { appCoordinator.navigate(direction) }
    }
}
