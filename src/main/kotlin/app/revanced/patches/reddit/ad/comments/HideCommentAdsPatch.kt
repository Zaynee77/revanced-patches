package app.revanced.patches.reddit.ad.comments

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.reddit.ad.comments.fingerprints.HideCommentAdsFingerprint
import app.revanced.patches.reddit.utils.integrations.Constants.PATCHES_PATH
import app.revanced.util.exception
import app.revanced.util.getWalkerMethod

object HideCommentAdsPatch : BytecodePatch(
    setOf(HideCommentAdsFingerprint)
) {
    private const val INTEGRATION_METHOD_DESCRIPTOR =
        "$PATCHES_PATH/GeneralAdsPatch;->hideCommentAds()Z"

    override fun execute(context: BytecodeContext) {
        HideCommentAdsFingerprint.result?.apply {
            val walkerMethod = getWalkerMethod(context, scanResult.patternScanResult!!.startIndex)
            walkerMethod.apply {
                addInstructionsWithLabels(
                    0, """
                        invoke-static {}, $INTEGRATION_METHOD_DESCRIPTOR
                        move-result v0
                        if-eqz v0, :show
                        new-instance v0, Ljava/lang/Object;
                        invoke-direct {v0}, Ljava/lang/Object;-><init>()V
                        return-object v0
                        """, ExternalLabel("show", getInstruction(0))
                )
            }
        } ?: throw HideCommentAdsFingerprint.exception

    }
}
