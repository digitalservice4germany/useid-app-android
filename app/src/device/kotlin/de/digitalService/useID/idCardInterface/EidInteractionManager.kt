package de.digitalService.useID.idCardInterface

import android.content.Context
import android.net.Uri
import android.nfc.Tag
import de.digitalService.useID.getLogger
import de.digitalService.useID.util.CoroutineContextProvider
import de.governikus.ausweisapp2.sdkwrapper.SDKWrapper.workflowController
import de.governikus.ausweisapp2.sdkwrapper.card.core.AccessRights
import de.governikus.ausweisapp2.sdkwrapper.card.core.ApiLevel
import de.governikus.ausweisapp2.sdkwrapper.card.core.AuthResult
import de.governikus.ausweisapp2.sdkwrapper.card.core.CertificateDescription
import de.governikus.ausweisapp2.sdkwrapper.card.core.ChangePinResult
import de.governikus.ausweisapp2.sdkwrapper.card.core.Reader
import de.governikus.ausweisapp2.sdkwrapper.card.core.VersionInfo
import de.governikus.ausweisapp2.sdkwrapper.card.core.WorkflowCallbacks
import de.governikus.ausweisapp2.sdkwrapper.card.core.WorkflowProgress
import de.governikus.ausweisapp2.sdkwrapper.card.core.WrapperError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EidInteractionManager @Inject constructor(
    private val coroutineContextProvider: CoroutineContextProvider
) {
    private val logger by getLogger()

    private val _eidFlow: MutableStateFlow<EidInteractionEvent> = MutableStateFlow(EidInteractionEvent.Idle)
    val eidFlow: Flow<EidInteractionEvent>
        get() = _eidFlow

    private val workflowControllerStarted: MutableStateFlow<Boolean> = MutableStateFlow(false)

    private val workflowCallbacks = object : WorkflowCallbacks {
        override fun onAccessRights(error: String?, accessRights: AccessRights?) {
            logger.trace("onAccessRights called")

            error?.let { logger.error(it) }
            if (accessRights == null) {
                logger.error("Access rights missing.")
                _eidFlow.value = EidInteractionEvent.Error(IdCardInteractionException.FrameworkError("Access rights missing. ${error ?: "n/a"}"))
                return
            }

            if (accessRights.effectiveRights == accessRights.requiredRights) {
                val authenticationRequest = AuthenticationRequest(accessRights.requiredRights.map { EidAttribute.fromAccessRight(it) }, accessRights.transactionInfo)
                _eidFlow.value = EidInteractionEvent.AuthenticationRequestConfirmationRequested(authenticationRequest)
            } else {
                workflowController.setAccessRights(listOf())
            }
        }

        override fun onApiLevel(error: String?, apiLevel: ApiLevel?) {
            TODO("Not yet implemented")
        }

        override fun onAuthenticationCompleted(authResult: AuthResult) {
            logger.debug("Authentication completed")
            logger.error(authResult.result?.major)
            logger.error(authResult.result?.minor)
            logger.error(authResult.result?.reason)

            authResult.result?.let { result ->
                val majorCode = result.major.split("#").last()
                val minorCode = result.minor?.split("#")?.last()

                val redirectUrl = authResult.url?.let { url ->
                    val uriBuilder = url.buildUpon()
                    majorCode.let { uriBuilder.appendQueryParameter("ResultMajor", it) }
                    minorCode?.let { uriBuilder.appendQueryParameter("ResultMinor", it) }
                    result.reason?.let { uriBuilder.appendQueryParameter("ResultMessage", it) }
                    uriBuilder.build().toString()
                }

                if (majorCode != "error") {
                    redirectUrl?.let { _eidFlow.value = EidInteractionEvent.AuthenticationSucceededWithRedirect(it) }
                } else {
                    _eidFlow.value = EidInteractionEvent.Error(IdCardInteractionException.ProcessFailed(redirectUrl))
                }
            } ?: run { _eidFlow.value = EidInteractionEvent.Error(IdCardInteractionException.ProcessFailed()) }
        }

        override fun onAuthenticationStartFailed(error: String) {
            _eidFlow.value = EidInteractionEvent.Error(IdCardInteractionException.FrameworkError(error))
        }

        override fun onAuthenticationStarted() {
            _eidFlow.value = EidInteractionEvent.AuthenticationStarted
        }

        override fun onBadState(error: String) {
            _eidFlow.value = EidInteractionEvent.Error(IdCardInteractionException.FrameworkError("Bad state: $error"))
        }

        override fun onCertificate(certificateDescription: CertificateDescription) {
            _eidFlow.value = EidInteractionEvent.CertificateDescriptionReceived(
                CertificateDescription(
                    certificateDescription.issuerName,
                    certificateDescription.issuerUrl?.toString(),
                    certificateDescription.purpose,
                    certificateDescription.subjectName,
                    certificateDescription.subjectUrl?.toString(),
                    certificateDescription.termsOfUsage,
                )
            )
        }

        override fun onChangePinCompleted(changePinResult: ChangePinResult) {
            if (changePinResult.success) {
                logger.debug("New PIN has been set successfully.")
                _eidFlow.value = EidInteractionEvent.PinChangeSucceeded
            } else {
                logger.error("Changing PIN failed.")
                _eidFlow.value = EidInteractionEvent.Error(IdCardInteractionException.ChangingPinFailed)
            }
        }

        override fun onChangePinStarted() {
            _eidFlow.value = EidInteractionEvent.PinChangeStarted
        }

        override fun onEnterCan(error: String?, reader: Reader) {
            error?.let { logger.error(it) }
            _eidFlow.value = EidInteractionEvent.CanRequested()
        }

        override fun onEnterNewPin(error: String?, reader: Reader) {
            error?.let { logger.error(it) }
            _eidFlow.value = EidInteractionEvent.NewPinRequested
        }

        override fun onEnterPin(error: String?, reader: Reader) {
            error?.let { logger.error(it) }
            logger.debug("pin retry counter: ${reader.card?.pinRetryCounter}")
            reader.card?.let {
                _eidFlow.value = EidInteractionEvent.PinRequested(it.pinRetryCounter)
            } ?: run {
                _eidFlow.value = EidInteractionEvent.Error(IdCardInteractionException.FrameworkError("Framework requests PIN without card"))
            }
        }

        override fun onEnterPuk(error: String?, reader: Reader) {
            error?.let { logger.error(it) }
            _eidFlow.value = EidInteractionEvent.PukRequested
        }

        override fun onInfo(versionInfo: VersionInfo) {
            logger.trace("on info: $versionInfo")
        }

        override fun onInsertCard(error: String?) {
            error?.let { logger.error(it) }
            _eidFlow.value = EidInteractionEvent.CardInsertionRequested
        }

        override fun onInternalError(error: String) {
            logger.error(error)
            _eidFlow.value = EidInteractionEvent.Error(IdCardInteractionException.FrameworkError(error))
        }

        override fun onReader(reader: Reader?) {
            logger.trace("onReader")
            if (reader == null) {
                logger.error("Unknown reader.")
                _eidFlow.value = EidInteractionEvent.Error(IdCardInteractionException.UnknownReader)
                return
            }

            if (reader.card == null) {
                _eidFlow.value = EidInteractionEvent.CardRemoved
            } else {
                _eidFlow.value = EidInteractionEvent.CardRecognized
            }
        }

        override fun onReaderList(readers: List<Reader>?) {
            logger.trace("onReaderList")
        }

        override fun onStarted() {
            logger.trace("onStarted")
            workflowControllerStarted.value = true
        }

        override fun onStatus(workflowProgress: WorkflowProgress) {
            logger.trace("onStatus with state ${workflowProgress.state} and progress ${workflowProgress.progress}")
        }

        override fun onWrapperError(error: WrapperError) {
            logger.error("${error.error} - ${error.msg}")
            _eidFlow.value = EidInteractionEvent.Error(IdCardInteractionException.FrameworkError(error.msg))
        }
    }

    fun handleNfcTag(tag: Tag) {
        if (!workflowController.isStarted) {
            logger.error("No task running.")
            return
        }

        workflowController.onNfcTagDetected(tag)
    }

    fun identify(context: Context, tcTokenUrl: Uri) {
        logger.debug("Starting workflow controller.")
        workflowController.registerCallbacks(workflowCallbacks)

        CoroutineScope(coroutineContextProvider.Default).launch {
            workflowControllerStarted.collect { started ->
                if (started) {
                    logger.debug("Start authentication")
                    workflowController.startAuthentication(tcTokenUrl)
                    cancel()
                }
            }
        }

        workflowController.start(context)
    }

    fun changePin(context: Context) {
        logger.debug("Starting workflow controller.")
        workflowController.registerCallbacks(workflowCallbacks)

        CoroutineScope(coroutineContextProvider.Default).launch {
            workflowControllerStarted.collect { started ->
                if (started) {
                    logger.debug("Start PIN management")
                    workflowController.startChangePin()
                    cancel()
                }
            }
        }

        workflowController.start(context)
    }

    fun cancelTask() {
        logger.debug("Stopping workflow controller.")
        workflowController.unregisterCallbacks(workflowCallbacks)
        workflowController.stop()
        workflowControllerStarted.value = false
        _eidFlow.value = EidInteractionEvent.Idle
    }

    fun providePin(pin: String) {
        if (!workflowController.isStarted) {
            logger.error("No task running.")
            return
        }

        workflowController.setPin(pin)
    }

    fun provideNewPin(newPin: String) {
        if (!workflowController.isStarted) {
            logger.error("No task running.")
            return
        }

        workflowController.setNewPin(newPin)
    }

    fun provideCan(can: String) {
        if (!workflowController.isStarted) {
            logger.error("No task running.")
            return
        }

        workflowController.setCan(can)
    }

    fun getCertificate() {
        if (!workflowController.isStarted) {
            logger.error("No task running.")
            return
        }

        workflowController.getCertificate()
    }

    fun acceptAccessRights() {
        if (!workflowController.isStarted) {
            logger.error("No task running.")
            return
        }

        workflowController.accept()
    }
}