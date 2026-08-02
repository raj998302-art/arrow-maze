package com.zenox.arrowmaze.core.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The four cardinal arrow directions used by every [ArrowCell] on the board.
 *
 * The enum is ordered so that `ordinal` increases clockwise (UP → RIGHT → DOWN → LEFT),
 * which makes 90° clockwise rotation a trivial `+1 mod 4` operation.
 *
 * Angles follow the convention used by the UI layer: 0° = pointing up, growing clockwise.
 */
@Serializable
enum class ArrowDirection {

    @SerialName("UP") UP,
    @SerialName("RIGHT") RIGHT,
    @SerialName("DOWN") DOWN,
    @SerialName("LEFT") LEFT;

    /** Rotates this direction 90° clockwise. */
    fun rotateCW(): ArrowDirection = entries[(ordinal + 1) % entries.size]

    /** Rotates this direction 90° counter-clockwise. */
    fun rotateCCW(): ArrowDirection = entries[(ordinal + entries.size - 1) % entries.size]

    /** Returns the direction pointing the opposite way (180°). */
    fun opposite(): ArrowDirection = entries[(ordinal + 2) % entries.size]

    /**
     * The (row, col) delta to move one cell in this direction.
     * Row grows downward, col grows rightward — standard grid convention.
     */
    fun delta(): Pair<Int, Int> = when (this) {
        UP    -> -1 to 0
        RIGHT -> 0 to 1
        DOWN  -> 1 to 0
        LEFT  -> 0 to -1
    }

    /** Angle in degrees, 0 = up, growing clockwise. Useful for rotating a Compose canvas arrow asset. */
    val angleDegrees: Float
        get() = when (this) {
            UP    -> 0f
            RIGHT -> 90f
            DOWN  -> 180f
            LEFT  -> 270f
        }

    companion object {
        /** Random direction using the supplied [kotlin.random.Random]. */
        fun random(rng: kotlin.random.Random): ArrowDirection =
            entries[rng.nextInt(entries.size)]
    }
}
