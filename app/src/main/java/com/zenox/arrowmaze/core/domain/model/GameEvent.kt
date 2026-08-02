package com.zenox.arrowmaze.core.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Discrete events emitted by the [com.zenox.arrowmaze.core.domain.engine.GameEngine] after
 * each player or hint action. The UI listens to a flow of these to drive animations, haptics
 * and analytics without coupling itself to the engine internals.
 *
 * Events are intentionally small value types — they carry only the data needed to render the
 * transition; full state lives on [GameSession].
 */
@Serializable
sealed interface GameEvent {

    /** An arrow cell was rotated. [newDirection] is the direction after the rotation. */
    @Serializable
    @SerialName("cellRotated")
    data class CellRotated(
        @SerialName("at")           val at: Position,
        @SerialName("newDirection") val newDirection: ArrowDirection
    ) : GameEvent

    /** The leading edge of the visible path advanced into [head]. */
    @Serializable
    @SerialName("pathAdvanced")
    data class PathAdvanced(
        @SerialName("head") val head: Position
    ) : GameEvent

    /** The visible path retreated away from [head] (e.g. because a downstream arrow was mis-rotated). */
    @Serializable
    @SerialName("pathRetreated")
    data class PathRetreated(
        @SerialName("head") val head: Position
    ) : GameEvent

    /** A hint was consumed and the hinted cell at [at] was auto-rotated. */
    @Serializable
    @SerialName("hintUsed")
    data class HintUsed(
        @SerialName("at") val at: Position
    ) : GameEvent

    /** The game was won. [path] is the full Start → Goal solution. */
    @Serializable
    @SerialName("won")
    data class Won(
        @SerialName("path") val path: List<Position>
    ) : GameEvent

    /** The game was lost. */
    @Serializable
    @SerialName("lost")
    data class Lost(
        @SerialName("reason") val reason: LoseReason
    ) : GameEvent
}
