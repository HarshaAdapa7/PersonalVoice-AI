# PersonalVoice AI — Testing Guide

## Phase 1: Voice-Generation MVP Verification

### 1. Engine Selection & Cloning Disclosure Test
1. Launch the app in the streaming emulator.
2. In the **Speech Engine** card, observe the three engine chips:
   - **Gemini TTS**: Displays the honesty badge: `"Gemini Prebuilt • Style Control (No Cloning)"`.
   - **External Cloning**: Displays: `"External Voice Cloning (F5-TTS/CosyVoice)"`.
   - **Device Preview**: Displays: `"Device Acoustic Synth (Preview Mode)"`.
3. Verify that the UI never misleads the user regarding voice cloning capabilities.

### 2. Sentence-Aware Chunking Test
1. In the **Speech Script** text field, paste a long multi-sentence paragraph (>400 characters).
2. Notice the live character count and chunk calculation (e.g., `450 chars • ~2 chunks`).
3. Press **Synthesize Speech**.
4. Observe the progress message displaying chunk-by-chunk synthesis and concatenation.
5. In the resulting player card, verify the joined chunk count (e.g., `Chunks Joined: 2`).

### 3. Delivery Controls Test
1. Change **Speaking Pace** from `1.00x` to `1.25x` or `0.80x` using the slider or quick presets.
2. Select a different **Vocal Tone** (e.g., `Warm & Friendly`, `Professional`, `Energetic`).
3. Click **Synthesize Speech**. Verify delivery parameters are clamped to engine capabilities and applied.

---

## Phase 2: Accounts, Profiles, Consent, and Deletion Verification

### 1. Account Session & Ownership Isolation Test
1. Tap the **Account** tab in the bottom navigation.
2. Verify that the initial session is initialized as verified account `Authorized Owner` (`creator@personalvoice.ai`).
3. Tap **Create Account** tab, enter a new email and password, and create an account.
4. Tap **Switch Account** to quickly toggle to `Colleague B` (`colleague@personalvoice.ai`).
5. Notice that when switched to Colleague B, any voice profiles created by the primary owner are completely hidden, verifying cross-user isolation and ownership checks.

### 2. Voice Enrollment & Random Consent Verification Test
1. Tap the **Profiles** tab in the bottom navigation.
2. Click **Enroll Voice** to open the `ConsentFlowDialog`.
3. Observe the randomized read-aloud verification sentence (e.g., `"I hereby verify that this is my own voice... Verification code 7821. Consented by Authorized Owner on..."`).
4. Tap the refresh icon next to the statement to confirm a new randomized code and statement is generated.
5. Notice the **Authorize & Enroll** button is disabled until both the consent checkbox is checked and a valid voice recording passes quality checks.

### 3. Audio Recorder & Quality Gate Test
1. Tap **Record Voice Sample** inside the enrollment dialog.
2. Observe the live amplitude audio bars responding in real time.
3. Tap **Stop Recording**.
4. Check the **Audio Quality Validated** banner (reports RMS dBFS level, peak amplitude, and confirms lack of clipping/silence).
5. Check the explicit consent checkbox:
   `"I declare this audio sample is my own voice and explicitly grant PersonalVoice AI permission to create this profile."`
6. Tap **Authorize & Enroll**. The new profile is saved to SQLite Room database with status `active`.

### 4. Studio Profile Integration Test
1. On the **Profiles** screen, tap **Use in Studio** on your newly enrolled profile.
2. Return to the **Studio** tab. Observe the active profile banner:
   `"Active Authorized Profile: [Your Profile Name] • Enrolled with verified consent"`.
3. Press **Synthesize Speech**. Synthesis runs and logs an immutable audit event for the profile.

### 5. Immediate Access Revocation & Deletion Queue Test
1. Go back to the **Profiles** tab.
2. Click the **Delete** (trash) icon on an active profile.
3. Review the confirmation dialog explaining that access will be revoked immediately and artifacts will be permanently purged via an enqueued deletion job.
4. Tap **Revoke & Delete**.
5. Observe the profile is immediately removed from the active studio selection.
6. Open the **Audit** tab in the bottom navigation:
   - In **Audit Log**: Verify `PROFILE_DELETION_ENQUEUED` and `PROFILE_PURGED` events are logged with timestamps.
   - In **Deletion Queue**: Verify the deletion job ID, target resource, and status `COMPLETED`.
   - In **Consent Records**: Review the immutable record of the signed statement text.
