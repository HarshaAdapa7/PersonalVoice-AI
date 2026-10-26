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

### 4. Audio Playback & Export Test
1. Click the **Play** button on the player card. Verify audio waveform animation and time counter progression.
2. Drag the progress slider to seek within the audio track.
3. Click **Download / Share Audio File (.wav)**.
4. Verify the toast confirms the WAV file was created in app storage and the Android Share Sheet opens.

### 5. Gemini API Key Configuration
1. Tap the **Key** icon in the top app bar.
2. Enter your Gemini API key, or keep the default to test with the **Device Preview** engine offline.
