# Setting up AniList sign-in

Shouze authenticates against AniList with **OAuth2 Implicit Grant** — the flow
the [AniList docs](https://docs.anilist.co/guide/auth/) recommend for mobile
apps (no client secret can be stored safely in an APK, and AniList does not
support PKCE). Tokens are long-lived (1 year) and are stored in
EncryptedSharedPreferences.

## 1. Create your AniList application

1. Go to <https://anilist.co/settings/developer>
2. Click **Create New Application**.
3. Fill in:
   - **Name**: anything, e.g. `Shouze`
   - **Redirect URL**: `shouze://anilist-auth`  ← must match exactly
4. Save. Copy the **Client ID** that is shown.

The client id is *not* a secret (the implicit grant has no secret — that's the
point). Never put your **client secret** into the repository.

## 2. Configure the client id

The id is public (the implicit grant has no secret — never add your client
*secret* anywhere). Pick whichever home fits how you build:

- **Global (recommended)** — add to `~/.gradle/gradle.properties`. Survives
  repo changes and fresh clones, and applies to every build on the machine
  (including on-device Gradle builds). Global properties win over project ones.

  ```properties
  ANILIST_CLIENT_ID=12345
  ```

- **Project-local** — `gradle.properties` is machine-local and gitignored.
  Start from the committed template, then set the id:

  ```sh
  cp gradle.properties.example gradle.properties
  ```

- **`local.properties`** — `ANILIST_CLIENT_ID=12345` works there too.

- **CI builds** — add the id as the repository secret `ANILIST_CLIENT_ID`
  (*Settings → Secrets and variables → Actions*). The Android CI workflow
  passes it into the build, so the debug APKs it publishes can sign in
  out of the box.

The value is compiled into `BuildConfig.ANILIST_CLIENT_ID`. If it's missing,
the login screen explains the setup instead of failing silently — and
**Paste token manually** (Profile → AniList Account) always works with any
valid AniList token, no client id needed.

## 3. Sign in

Profile → **AniList Account** → **Sign in with AniList**. A browser/Custom Tab
opens AniList's consent page; approving it redirects to `shouze://anilist-auth`
and the app completes login automatically, then pulls your library.

If your device/browser blocks the app redirect, use **Paste token manually**:
authorize via the *auth pin* flow (set your app's redirect URL to
`https://anilist.co/api/v2/oauth/pin`) and paste the shown token.

## Technical notes

| Topic | Detail |
| --- | --- |
| Authorize URL | `https://anilist.co/api/v2/oauth/authorize?client_id=…&redirect_uri=shouze://anilist-auth&response_type=token` |
| Token delivery | URL *fragment* of the redirect (`#access_token=…&expires=…`), parsed by `ImplicitRedirectParser` |
| Token lifetime | 1 year; no refresh tokens exist — on expiry the app asks you to sign in again |
| Storage | `androidx.security:security-crypto` EncryptedSharedPreferences (Keystore-backed); falls back to memory-only if the keystore is broken |
| Validation | After receiving a token the app runs a `Viewer` query before trusting it |
| Rate limits | All API calls share a client-side limiter (~28 req/min) and honor `Retry-After` |
