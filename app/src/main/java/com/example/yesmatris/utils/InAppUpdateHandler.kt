package com.example.yesmatris.utils

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability

class InAppUpdateHandler {
    internal fun checkForInAppUpdate(
        appUpdateManager: AppUpdateManager,
        appUpdateType: Int,
        listener: InstallStateUpdatedListener,
        activityResultLauncher: ActivityResultLauncher<IntentSenderRequest>,
        setUpdateMust: () -> Unit,
    ) {
        if (appUpdateType == AppUpdateType.FLEXIBLE) {
            appUpdateManager.unregisterListener(listener)
        }
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo
        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            val isUpdateAvailable =
                appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
            if (isUpdateAvailable && appUpdateInfo.isUpdateTypeAllowed(appUpdateType)) {
                startUpdateFlow(
                    appUpdateManager,
                    appUpdateInfo,
                    appUpdateType,
                    listener,
                    activityResultLauncher,
                    setUpdateMust,
                )
            }
        }
    }

    internal fun checkIfAnUpdateRunning(
        appUpdateManager: AppUpdateManager,
        listener: InstallStateUpdatedListener,
        activityResultLauncher: ActivityResultLauncher<IntentSenderRequest>,
        setUpdateMust: () -> Unit,
    ) {
        appUpdateManager
            .appUpdateInfo
            .addOnSuccessListener { appUpdateInfo ->
                when {
                    // If an in-app update is already running, resume the update.
                    appUpdateInfo.updateAvailability()
                            == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> {
                        startUpdateFlow(
                            appUpdateManager,
                            appUpdateInfo,
                            AppUpdateType.IMMEDIATE,
                            listener,
                            activityResultLauncher,
                            setUpdateMust,
                        )
                    }
                }
            }
    }

    private fun startUpdateFlow(
        appUpdateManager: AppUpdateManager,
        appUpdateInfo: AppUpdateInfo,
        appUpdateType: Int,
        listener: InstallStateUpdatedListener,
        activityResultLauncher: ActivityResultLauncher<IntentSenderRequest>,
        setUpdateMust: () -> Unit = {},
    ) {
        if (appUpdateType == AppUpdateType.FLEXIBLE) {
            appUpdateManager.registerListener(listener)
            setUpdateMust()
        }
        appUpdateManager.startUpdateFlowForResult(
            appUpdateInfo,
            activityResultLauncher,
            AppUpdateOptions.newBuilder(appUpdateType).build(),
        )
    }
}