package app.revanced.patches.music.utils.settings

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.replaceInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.music.utils.integrations.Constants.INTEGRATIONS_PATH
import app.revanced.patches.music.utils.integrations.IntegrationsPatch
import app.revanced.patches.music.utils.mainactivity.MainActivityResolvePatch
import app.revanced.patches.music.utils.mainactivity.MainActivityResolvePatch.injectInit
import app.revanced.patches.music.utils.settings.fingerprints.GoogleApiActivityFingerprint
import app.revanced.patches.music.utils.settings.fingerprints.PreferenceFingerprint
import app.revanced.patches.music.utils.settings.fingerprints.SettingsHeadersFragmentFingerprint
import app.revanced.patches.shared.settings.fingerprints.SharedSettingFingerprint
import app.revanced.util.exception
import app.revanced.util.getTargetIndex
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Patch(
    dependencies = [
        IntegrationsPatch::class,
        MainActivityResolvePatch::class
    ],
    requiresIntegrations = true
)
object SettingsBytecodePatch : BytecodePatch(
    setOf(
        GoogleApiActivityFingerprint,
        PreferenceFingerprint,
        SharedSettingFingerprint,
        SettingsHeadersFragmentFingerprint
    )
) {
    private const val INTEGRATIONS_ACTIVITY_CLASS_DESCRIPTOR =
        "$INTEGRATIONS_PATH/settings/ActivityHook;"
    private const val INTEGRATIONS_FRAGMENT_CLASS_DESCRIPTOR =
        "$INTEGRATIONS_PATH/settings/preference/ReVancedPreferenceFragment;"

    override fun execute(context: BytecodeContext) {

        /**
         * Set SharedPrefCategory
         */
        SharedSettingFingerprint.result?.let {
            it.mutableMethod.apply {
                val stringIndex = getTargetIndex(Opcode.CONST_STRING)
                val stringRegister = getInstruction<OneRegisterInstruction>(stringIndex).registerA

                replaceInstruction(
                    stringIndex,
                    "const-string v$stringRegister, \"youtube\""
                )
            }
        } ?: throw SharedSettingFingerprint.exception

        /**
         * Inject settings Activity
         */
        SettingsHeadersFragmentFingerprint.result?.let {
            it.mutableMethod.apply {
                val targetIndex = it.scanResult.patternScanResult!!.endIndex
                val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                addInstruction(
                    targetIndex + 1,
                    "invoke-static {v$targetRegister}, $INTEGRATIONS_ACTIVITY_CLASS_DESCRIPTOR->setActivity(Ljava/lang/Object;)V"
                )
            }
        } ?: throw SettingsHeadersFragmentFingerprint.exception

        /**
         * Values are loaded when preferences change
         */
        PreferenceFingerprint.result?.let {
            it.mutableMethod.apply {
                val targetIndex = it.scanResult.patternScanResult!!.endIndex
                val keyRegister = getInstruction<FiveRegisterInstruction>(targetIndex).registerD
                val valueRegister = getInstruction<FiveRegisterInstruction>(targetIndex).registerE

                addInstruction(
                    targetIndex,
                    "invoke-static {v$keyRegister, v$valueRegister}, $INTEGRATIONS_FRAGMENT_CLASS_DESCRIPTOR->onPreferenceChanged(Ljava/lang/String;Z)V"
                )
            }
        } ?: throw PreferenceFingerprint.exception

        /**
         * Inject dummy Activity for intent
         */
        GoogleApiActivityFingerprint.result?.let {
            it.mutableMethod.apply {
                addInstructionsWithLabels(
                    1, """
                        invoke-static {p0}, $INTEGRATIONS_ACTIVITY_CLASS_DESCRIPTOR->initialize(Landroid/app/Activity;)Z
                        move-result v0
                        if-eqz v0, :show
                        return-void
                        """, ExternalLabel("show", getInstruction(1))
                )
            }
        } ?: throw GoogleApiActivityFingerprint.exception

        injectInit("InitializationPatch", "setDeviceInformation")
        injectInit("InitializationPatch", "onCreate")

    }
}
