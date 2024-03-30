package app.revanced.patches.youtube.utils.returnyoutubedislike.rollingnumber.fingerprints

import app.revanced.util.fingerprint.ReferenceFingerprint
import com.android.tools.smali.dexlib2.Opcode

/**
 * Compatible with YouTube v18.30.xx to v18.49.xx
 */
object RollingNumberMeasureAnimatedTextFingerprint : ReferenceFingerprint(
    opcodes = listOf(
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT,
        Opcode.ADD_FLOAT_2ADDR, // measuredTextWidth
        Opcode.ADD_INT_LIT8,
        Opcode.GOTO
    ),
    reference = { "Landroid/text/TextPaint;->measureText([CII)F" }
)