package com.zenox.arrowmaze.core.firebase.auth

import com.zenox.arrowmaze.core.common.Result
import kotlinx.coroutines.flow.Flow

/**
 * Authenticated Firebase user projected into the domain layer. Carries just
 * the fields the app needs; the raw `FirebaseUser` is kept inside
 * [FirebaseAuthImpl] so the rest of the app never imports the Firebase SDK
 * directly.
 *
 * @property uid             Stable Firebase uid (anonymous users get one too).
 * @property email           Email address (null for guests / unverified).
 * @property displayName     Display name (null until set by the user).
 * @property isEmailVerified Whether the user has clicked the verification link.
 * @property isAnonymous     True for `signInAnonymously()` users (guests).
 * @property photoUrl        Photo URL (set by Google Sign-In or profile editor).
 */
data class AuthUser(
    val uid: String,
    val email: String?,
    val displayName: String?,
    val isEmailVerified: Boolean,
    val isAnonymous: Boolean,
    val photoUrl: String?,
)

/**
 * Single façade for all Firebase Authentication operations. The default
 * implementation, [FirebaseAuthImpl], is bound by
 * [com.zenox.arrowmaze.core.di.FirebaseModule]; tests can replace it with a
 * fake that emits deterministic [AuthUser] values.
 *
 * Every method returns [Result] so callers don't have to wrap try/catch —
 * Firebase `Task` exceptions are normalised into [com.zenox.arrowmaze.core.common.AppError.Auth].
 *
 * Guest-merge contract:
 *  - When a guest (`isAnonymous == true`) signs in with email or Google,
 *    [FirebaseAuthImpl] first tries `linkWithCredential` so the guest uid is
 *    preserved. If that fails because the credential is already in use by
 *    another account, it falls back to a fresh sign-in and then asks the
 *    [com.zenox.arrowmaze.core.data.repository.ProfileRepository] to copy the
 *    guest's coins/hints/progress onto the new account.
 */
interface ArrowMazeAuth {

    /** Hot flow of the current auth state; emits `null` when signed out. */
    val currentUser: Flow<AuthUser?>

    /** Sign in with an email + password pair. */
    suspend fun signInWithEmail(email: String, password: String): Result<AuthUser>

    /**
     * Create a new email/password account. [displayName] is written to the
     * Firebase user profile (best-effort) and also stored on the default
     * Profile document created via [createProfileIfMissing].
     */
    suspend fun signUpWithEmail(
        email: String,
        password: String,
        displayName: String,
    ): Result<AuthUser>

    /**
     * Sign in with a Google ID token returned by the Credential Manager. If a
     * guest account is currently active, it is linked (or merged) per the
     * guest-merge contract above.
     */
    suspend fun signInWithGoogle(idToken: String): Result<AuthUser>

    /** Create / sign-in an anonymous guest account. */
    suspend fun signInAsGuest(): Result<AuthUser>

    /** Send a password-reset email to [email]. */
    suspend fun sendPasswordReset(email: String): Result<Unit>

    /** Send an email-verification link to the current user. */
    suspend fun sendEmailVerification(): Result<Unit>

    /** Sign out the current user (no-op when already signed out). */
    suspend fun signOut(): Result<Unit>

    /** Permanently delete the current user's Firebase account. */
    suspend fun deleteAccount(): Result<Unit>

    /** Re-fetch the current user profile from Firebase (refreshes verification flags, etc.). */
    suspend fun reloadUser(): Result<Unit>

    /**
     * Creates a default Profile document at `FS_USERS/{uid}` if (and only if)
     * one does not already exist. Called from the auth impl right after a
     * sign-up / guest creation so that the very first profile read by the
     * [com.zenox.arrowmaze.core.data.repository.ProfileRepository] doesn't
     * return `null`.
     *
     * Keeping this on the auth interface (rather than on ProfileRepository)
     * avoids a circular dependency: the auth impl already has Firestore +
     * Firebase injected, so it can write the seed document directly without
     * going through the repository layer.
     */
    suspend fun createProfileIfMissing(
        uid: String,
        isGuest: Boolean,
        email: String?,
        displayName: String,
    ): Result<Unit>
}
