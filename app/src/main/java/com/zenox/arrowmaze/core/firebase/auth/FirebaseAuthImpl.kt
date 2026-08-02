package com.zenox.arrowmaze.core.firebase.auth

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.zenox.arrowmaze.core.common.AppConstants
import com.zenox.arrowmaze.core.common.AppError
import com.zenox.arrowmaze.core.common.Result
import com.zenox.arrowmaze.core.common.Result.Failure
import com.zenox.arrowmaze.core.common.Result.Success
import com.zenox.arrowmaze.core.common.onFailure
import com.zenox.arrowmaze.core.common.onSuccess
import com.zenox.arrowmaze.core.common.resultOf
import com.zenox.arrowmaze.core.data.dto.ProfileDto
import com.zenox.arrowmaze.core.data.mapper.ProfileMapper.toDto
import com.zenox.arrowmaze.core.data.repository.ProfileRepository
import com.zenox.arrowmaze.core.data.repository.SessionRepository
import com.zenox.arrowmaze.core.di.IoDispatcher
import com.zenox.arrowmaze.core.domain.model.Profile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Default [ArrowMazeAuth] implementation. Wraps [FirebaseAuth] and translates
 * every Firebase `Task` into the app's [Result] type, normalising exceptions
 * into [AppError.Auth] via [mapAuthException].
 *
 * Auth-state observation is exposed as a hot [callbackFlow] wrapping
 * [FirebaseAuth.AuthStateListener]; the listener is unregistered when the
 * flow's collector cancels.
 *
 * Guest-merge strategy (called from [signInWithEmail] / [signUpWithEmail] /
 * [signInWithGoogle]):
 *  1. If `firebaseAuth.currentUser?.isAnonymous == true`, attempt
 *     `linkWithCredential(credential)` so the guest's uid (and therefore its
 *     Firestore profile document) is preserved.
 *  2. If `linkWithCredential` throws [FirebaseAuthInvalidCredentialsException]
 *     (credential already in use by another account), sign in fresh instead.
 *     The new account has a different uid, so we copy the guest's economy +
 *     progression fields onto the new account via
 *     [ProfileRepository.mergeGuestIntoAccount].
 *  3. In both cases, after a successful sign-in we clear the local guest
 *     session state via [SessionRepository.clearGuest].
 */
@Singleton
class FirebaseAuthImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val io: CoroutineDispatcher,
    private val firestore: FirebaseFirestore,
    private val profileRepository: ProfileRepository,
    private val sessionRepository: SessionRepository,
) : ArrowMazeAuth {

    private val firebaseAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    override val currentUser: Flow<AuthUser?> = callbackFlow {
        val listener = object : FirebaseAuth.AuthStateListener {
            override fun onAuthStateChanged(auth: FirebaseAuth) {
                trySend(auth.currentUser?.toAuthUser())
            }
        }
        firebaseAuth.addAuthStateListener(listener)
        // Prime the flow with the current state so collectors don't have to
        // wait for the first auth-state change.
        trySend(firebaseAuth.currentUser?.toAuthUser())
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    override suspend fun signInWithEmail(email: String, password: String): Result<AuthUser> =
        withContext(io) {
            val guest = currentGuest()
            val result = if (guest != null) {
                // Try to upgrade the guest to an email account (same uid).
                val credential = com.google.firebase.auth.EmailAuthProvider
                    .getCredential(email, password)
                runCatching {
                    firebaseAuth.currentUser!!
                        .linkWithCredential(credential)
                        .await()
                        .user!!
                }.getOrElse { linkError ->
                    if (linkError is FirebaseAuthInvalidCredentialsException) {
                        // Email already in use by another account — sign in fresh
                        // and merge the guest's progress onto the new uid.
                        Timber.i(linkError, "Guest link failed; signing in fresh + merging.")
                        firebaseAuth.signInWithEmailAndPassword(email, password)
                            .await()
                            .user!!
                    } else {
                        throw linkError
                    }
                }
            } else {
                firebaseAuth.signInWithEmailAndPassword(email, password)
                    .await()
                    .user!!
            }

            finishSignIn(
                user = result,
                isGuest = false,
                email = email,
                displayName = result.displayName ?: email.substringBefore('@'),
            )
        }

    override suspend fun signUpWithEmail(
        email: String,
        password: String,
        displayName: String,
    ): Result<AuthUser> = withContext(io) {
        val guest = currentGuest()
        val result = if (guest != null) {
            val credential = com.google.firebase.auth.EmailAuthProvider
                .getCredential(email, password)
            runCatching {
                firebaseAuth.currentUser!!
                    .linkWithCredential(credential)
                    .await()
                    .user!!
            }.getOrElse { linkError ->
                if (linkError is FirebaseAuthInvalidCredentialsException) {
                    Timber.i(linkError, "Guest link failed on sign-up; creating fresh account + merging.")
                    firebaseAuth.createUserWithEmailAndPassword(email, password)
                        .await()
                        .user!!
                } else {
                    throw linkError
                }
            }
        } else {
            firebaseAuth.createUserWithEmailAndPassword(email, password)
                .await()
                .user!!
        }

        // Persist the chosen display name on the Firebase user profile so
        // subsequent reloads see it. Best-effort — don't fail sign-up if the
        // profile update fails.
        runCatching {
            val profileChange = UserProfileChangeRequest.Builder()
                .setDisplayName(displayName)
                .build()
            result.updateProfile(profileChange).await()
            result.reload().await()
        }.onFailure { Timber.w(it, "Failed to set display name on Firebase user.") }

        finishSignIn(
            user = result,
            isGuest = false,
            email = email,
            displayName = displayName,
        )
    }

    override suspend fun signInWithGoogle(idToken: String): Result<AuthUser> =
        withContext(io) {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val guest = currentGuest()
            val result = if (guest != null) {
                runCatching {
                    firebaseAuth.currentUser!!
                        .linkWithCredential(credential)
                        .await()
                        .user!!
                }.getOrElse { linkError ->
                    if (linkError is FirebaseAuthInvalidCredentialsException) {
                        Timber.i(linkError, "Guest link failed for Google; signing in fresh + merging.")
                        firebaseAuth.signInWithCredential(credential)
                            .await()
                            .user!!
                    } else {
                        throw linkError
                    }
                }
            } else {
                firebaseAuth.signInWithCredential(credential)
                    .await()
                    .user!!
            }

            finishSignIn(
                user = result,
                isGuest = false,
                email = result.email,
                displayName = result.displayName ?: result.email?.substringBefore('@') ?: "Player",
            )
        }

    override suspend fun signInAsGuest(): Result<AuthUser> = withContext(io) {
        val result = firebaseAuth.signInAnonymously().await().user!!
        val authUser = result.toAuthUser()
        // Seed a default Profile document so the very first profile read isn't null.
        createProfileIfMissing(
            uid = authUser.uid,
            isGuest = true,
            email = null,
            displayName = "Guest",
        )
        Success(authUser)
    }

    override suspend fun sendPasswordReset(email: String): Result<Unit> =
        withContext(io) {
            resultOf {
                firebaseAuth.sendPasswordResetEmail(email).await()
                Timber.i("Password reset email sent to %s", email)
            }.mapAuthError()
        }

    override suspend fun sendEmailVerification(): Result<Unit> =
        withContext(io) {
            val user = firebaseAuth.currentUser
                ?: return@withContext Failure(AppError.Auth("No signed-in user", code = "NO_USER"))
            resultOf {
                user.sendEmailVerification().await()
                Timber.i("Verification email sent to %s", user.email ?: "(no email)")
            }.mapAuthError()
        }

    override suspend fun signOut(): Result<Unit> = withContext(io) {
        resultOf {
            firebaseAuth.signOut()
            Timber.i("User signed out.")
        }
    }

    override suspend fun deleteAccount(): Result<Unit> = withContext(io) {
        val user = firebaseAuth.currentUser
            ?: return@withContext Failure(AppError.Auth("No signed-in user", code = "NO_USER"))
        val uid = user.uid
        resultOf {
            user.delete().await()
            // Best-effort: delete the Firestore profile document too.
            runCatching {
                firestore.collection(AppConstants.FS_USERS).document(uid).delete().await()
            }.onFailure { Timber.w(it, "Failed to delete Firestore profile for uid=%s", uid) }
            Timber.i("Account deleted: uid=%s", uid)
        }.mapAuthError()
    }

    override suspend fun reloadUser(): Result<Unit> = withContext(io) {
        val user = firebaseAuth.currentUser
            ?: return@withContext Failure(AppError.Auth("No signed-in user", code = "NO_USER"))
        resultOf {
            user.reload().await()
            Unit
        }.mapAuthError()
    }

    override suspend fun createProfileIfMissing(
        uid: String,
        isGuest: Boolean,
        email: String?,
        displayName: String,
    ): Result<Unit> = withContext(io) {
        resultOf {
            val ref = firestore.collection(AppConstants.FS_USERS).document(uid)
            val snapshot = ref.get().await()
            if (snapshot.exists()) {
                Timber.d("Profile already exists for uid=%s — skipping seed.", uid)
                return@resultOf
            }
            val now = System.currentTimeMillis()
            val defaultProfile = Profile(
                uid = uid,
                isGuest = isGuest,
                email = email,
                displayName = displayName,
                playerName = displayName,
                avatarUrl = null,
                country = "US",
                joinDateEpochMs = now,
                level = 1,
                xp = 0,
                coins = AppConstants.STARTING_COINS,
                hints = AppConstants.STARTING_HINTS,
                lives = AppConstants.STARTING_LIVES,
                lastLifeRegenEpochMs = now,
                gamesPlayed = 0,
                gamesWon = 0,
                bestStreak = 0,
                currentStreak = 0,
                averageSolveTimeMs = 0L,
                highestLevel = 0,
                currentThemeId = "dark",
                currentArrowSkinId = "default",
                currentTrailFxId = "default",
                ownedItems = emptyList(),
                unlockedAchievements = emptyList(),
                isPremium = false,
                isVip = false,
            )
            val dto: ProfileDto = defaultProfile.toDto()
            // Use the DTO's @SerialName fields by encoding to JSON then
            // re-parsing as a Map so Firestore sees snake_case keys.
            val json = kotlinx.serialization.json.Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
            }
            val jsonString = json.encodeToString(ProfileDto.serializer(), dto)
            val parsed = json.parseToJsonElement(jsonString)
                as kotlinx.serialization.json.JsonObject
            val data: Map<String, Any?> = parsed.entries.associate { (k, v) ->
                k to jsonToFirestoreValue(v)
            }
            ref.set(data).await()
            Timber.i("Seeded default profile for uid=%s guest=%s", uid, isGuest)
        }.mapAuthError()
    }

    // ---------- internals ----------

    /**
     * Common post-sign-in housekeeping: seeds a profile if missing, merges a
     * guest profile onto the new account when the previous guest couldn't be
     * linked, and clears the guest session state.
     *
     * Returns the freshly-authorised [AuthUser] wrapped in [Success], or
     * [Failure] with an [AppError.Auth] for any unrecoverable error.
     */
    private suspend fun finishSignIn(
        user: FirebaseUser,
        isGuest: Boolean,
        email: String?,
        displayName: String,
    ): Result<AuthUser> {
        return try {
            // 1. Seed the profile doc if this is a brand-new account.
            createProfileIfMissing(
                uid = user.uid,
                isGuest = isGuest,
                email = email,
                displayName = displayName,
            )

            // 2. If the previous session was a guest (and the new uid is
            //    different from the guest uid), merge the guest's economy +
            //    progression onto the new account.
            val guestProfile = sessionRepository.guestProfileFlow.first()
            val guestUid = sessionRepository.guestUidFlow.first()
            if (guestProfile != null && guestUid != null && guestUid != user.uid) {
                Timber.i("Merging guest=%s into new account=%s", guestUid, user.uid)
                profileRepository.mergeGuestIntoAccount(guestProfile, user.uid)
                    .onFailure { Timber.w(it.asException(), "Profile merge failed (continuing).") }
            }

            // 3. Always clear local guest state once we have a real account.
            sessionRepository.clearGuest()

            Success(user.toAuthUser())
        } catch (t: Throwable) {
            Failure(mapAuthException(t))
        }
    }

    /** Returns the current anonymous guest [FirebaseUser], or null. */
    private fun currentGuest(): FirebaseUser? {
        val current = firebaseAuth.currentUser ?: return null
        return if (current.isAnonymous) current else null
    }

    /** Maps a [Throwable] into an [AppError.Auth] with a stable error code. */
    private fun mapAuthException(t: Throwable): AppError.Auth {
        val code = when (t) {
            is FirebaseAuthInvalidUserException -> "INVALID_USER"
            is FirebaseAuthInvalidCredentialsException -> "INVALID_CREDENTIALS"
            else -> "AUTH_ERROR"
        }
        val msg = t.message ?: "Authentication failed"
        return AppError.Auth(msg, code = code)
    }

    /** Convenience: wraps a [resultOf] block's failure in [AppError.Auth]. */
    private fun <T> Result<T>.mapAuthError(): Result<T> = when (this) {
        is Failure -> Failure(mapAuthException(this.error.asException()))
        else -> this
    }

    /** Converts a [FirebaseUser] to the domain [AuthUser]. */
    private fun FirebaseUser.toAuthUser(): AuthUser = AuthUser(
        uid = uid,
        email = email,
        displayName = displayName,
        isEmailVerified = isEmailVerified,
        isAnonymous = isAnonymous,
        photoUrl = photoUrl?.toString(),
    )

    /**
     * Converts a [kotlinx.serialization.json.JsonElement] into a value
     * Firestore's `set()` accepts (String / Number / Boolean / List / Map).
     * Used by [createProfileIfMissing] to build the seed document.
     */
    private fun jsonToFirestoreValue(
        element: kotlinx.serialization.json.JsonElement,
    ): Any? = when (element) {
        is kotlinx.serialization.json.JsonNull -> null
        is kotlinx.serialization.json.JsonPrimitive -> {
            val content = element.content
            when {
                element.isString -> content
                content == "true" -> true
                content == "false" -> false
                content.contains('.') || content.contains('e') || content.contains('E') ->
                    content.toDoubleOrNull() ?: content
                else -> content.toLongOrNull() ?: content
            }
        }
        is kotlinx.serialization.json.JsonArray -> element.map { jsonToFirestoreValue(it) }
        is kotlinx.serialization.json.JsonObject -> element.entries.associate { (k, v) ->
            k to (jsonToFirestoreValue(v) ?: "")
        }
    }
}
