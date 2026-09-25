"""
"First Light" — an original, calm piece for the Daily Light promo.

Composed and synthesised here from scratch: no samples, no loops, nothing
taken from anyone else, so it carries no third-party copyright. Released
with the project under CC0 — use it anywhere.

Four slow chords (Cmaj9, Am9, Fmaj7#11, Gsus2) under a sparse, soft
bell-piano melody, 24.5 s to match the video, fading in and out.
"""
import numpy as np, wave

SR = 44100
DUR = 24.5
t = np.arange(int(SR * DUR)) / SR
out = np.zeros_like(t)

def hz(n):  # MIDI note -> Hz
    return 440.0 * 2 ** ((n - 69) / 12)

def pad(note, start, length, gain):
    """Warm pad: detuned sines with a slow swell, so chords breathe in and out."""
    s, e = int(start * SR), min(int((start + length) * SR), len(t))
    tt = t[s:e] - start
    f = hz(note)
    tone = (np.sin(2*np.pi*f*tt) + 0.5*np.sin(2*np.pi*f*1.003*tt)
            + 0.5*np.sin(2*np.pi*f*0.997*tt) + 0.12*np.sin(2*np.pi*2*f*tt))
    env = np.minimum(1, tt / 1.8) * np.minimum(1, (length - tt) / 2.2)
    out[s:e] += gain * tone * np.clip(env, 0, 1)

def bell(note, start, gain):
    """Soft piano/bell: a few decaying harmonics."""
    length = 3.5
    s, e = int(start * SR), min(int((start + length) * SR), len(t))
    tt = t[s:e] - start
    f = hz(note)
    tone = (np.sin(2*np.pi*f*tt) * np.exp(-tt*1.3)
            + 0.35*np.sin(2*np.pi*2*f*tt) * np.exp(-tt*2.4)
            + 0.12*np.sin(2*np.pi*3.01*f*tt) * np.exp(-tt*3.5))
    attack = np.minimum(1, tt / 0.012)
    out[s:e] += gain * tone * attack

# Chords, 6 s each, overlapping a little so they cross-fade.
chords = [
    [48, 55, 59, 62, 64],   # Cmaj9
    [45, 52, 55, 59, 60],   # Am9
    [41, 48, 52, 57, 59],   # Fmaj7#11 (colour, not tension)
    [43, 50, 55, 57, 62],   # Gsus2
]
for i, ch in enumerate(chords):
    for n in ch:
        pad(n, i * 6.0, 7.0, 0.06)

# A sparse melody: few notes, lots of air.
melody = [
    (72, 1.0), (76, 2.5), (74, 4.0),
    (72, 7.0), (69, 8.5), (71, 10.0),
    (69, 13.0), (72, 14.5), (71, 16.0),
    (74, 19.0), (72, 20.5), (79, 22.0),
]
for n, st in melody:
    bell(n, st, 0.11)

# A little room: two short echoes.
room = np.copy(out)
for d, g in ((0.23, 0.25), (0.41, 0.14)):
    k = int(d * SR)
    room[k:] += out[:-k] * g
out = room

# Fade in 1.5 s, out 2.5 s; normalise gently.
fade = np.ones_like(t)
fade[: int(1.5*SR)] = np.linspace(0, 1, int(1.5*SR))
fade[-int(2.5*SR):] = np.linspace(1, 0, int(2.5*SR))
out *= fade
out = np.tanh(out * 1.2)
out *= 0.7 / np.max(np.abs(out))

stereo = np.stack([out, np.roll(out, int(0.012 * SR))], axis=1)  # a touch of width
pcm = (stereo * 32767).astype(np.int16)
with wave.open("first-light.wav", "wb") as w:
    w.setnchannels(2); w.setsampwidth(2); w.setframerate(SR)
    w.writeframes(pcm.tobytes())
print("wrote first-light.wav")
