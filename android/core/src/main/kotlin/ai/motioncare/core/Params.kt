package ai.motioncare.core

/** Mirrors shared/params.json (STEP 2). */
data class Params(
    val sampleHz: Double = 1.0,
    val speedEmaAlpha: Double = 1.0,
    val minSpeedForHeading: Double = 2.0,
    val bandHighPassHz: Double = 0.0315,
    val bandHighPassQ: Double = 0.7071067811865476,
    val bandLowPassHz: Double = 0.27,
    val bandLowPassQ: Double = 0.95,
    val bandLowPassZero: Double = 0.35,
    val indexWindowS: Double = 60.0,
    val wX: Double = 1.0,
    val wY: Double = 1.0,
    val thetaCaution: Double = 0.9,
    val thetaHigh: Double = 1.5,
    val exitRatio: Double = 0.8,
    val enterHoldS: Double = 10.0,
    val ttsCooldownS: Double = 300.0,
    val highHoldForRestStopS: Double = 180.0,
    val cabinT0: Double = 22.0,
    val gammaT: Double = 0.04,
    val gammaV: Double = 0.15,
    val recircSaturationS: Double = 3600.0,
    val cabinFactorFloor: Double = 0.6,
    val tau0: Double = 30.0,
    val tauPriorN: Double = 5.0,
    val tauSlope: Double = 5.0,
    val cueSayS: Double = 2.0,
    val cuePrepareS: Double = 3.0,
) {
    companion object {
        private val number = Regex("\"(\\w+)\"\\s*:\\s*(-?[0-9.]+(?:[eE][-+]?\\d+)?)")

        /** Reads the flat numeric fields of params.json; unknown keys are ignored, missing keys keep defaults. */
        fun fromJson(json: String): Params {
            val v = number.findAll(json).associate { it.groupValues[1] to it.groupValues[2].toDouble() }
            val d = Params()
            return Params(
                sampleHz = v["sampleHz"] ?: d.sampleHz,
                speedEmaAlpha = v["speedEmaAlpha"] ?: d.speedEmaAlpha,
                minSpeedForHeading = v["minSpeedForHeading"] ?: d.minSpeedForHeading,
                bandHighPassHz = v["bandHighPassHz"] ?: d.bandHighPassHz,
                bandHighPassQ = v["bandHighPassQ"] ?: d.bandHighPassQ,
                bandLowPassHz = v["bandLowPassHz"] ?: d.bandLowPassHz,
                bandLowPassQ = v["bandLowPassQ"] ?: d.bandLowPassQ,
                bandLowPassZero = v["bandLowPassZero"] ?: d.bandLowPassZero,
                indexWindowS = v["indexWindowS"] ?: d.indexWindowS,
                wX = v["wX"] ?: d.wX,
                wY = v["wY"] ?: d.wY,
                thetaCaution = v["thetaCaution"] ?: d.thetaCaution,
                thetaHigh = v["thetaHigh"] ?: d.thetaHigh,
                exitRatio = v["exitRatio"] ?: d.exitRatio,
                enterHoldS = v["enterHoldS"] ?: d.enterHoldS,
                ttsCooldownS = v["ttsCooldownS"] ?: d.ttsCooldownS,
                highHoldForRestStopS = v["highHoldForRestStopS"] ?: d.highHoldForRestStopS,
                cabinT0 = v["cabinT0"] ?: d.cabinT0,
                gammaT = v["gammaT"] ?: d.gammaT,
                gammaV = v["gammaV"] ?: d.gammaV,
                recircSaturationS = v["recircSaturationS"] ?: d.recircSaturationS,
                cabinFactorFloor = v["cabinFactorFloor"] ?: d.cabinFactorFloor,
                tau0 = v["tau0"] ?: d.tau0,
                tauPriorN = v["tauPriorN"] ?: d.tauPriorN,
                tauSlope = v["tauSlope"] ?: d.tauSlope,
                cueSayS = v["cueSayS"] ?: d.cueSayS,
                cuePrepareS = v["cuePrepareS"] ?: d.cuePrepareS,
            )
        }
    }
}
