package com.floattime.app;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

import java.util.Random;

public class SoundUtil {

    public static void play(int type) {
        new Thread(() -> playSync(type)).start();
    }

    private static void playSync(int type) {
        int sampleRate = 44100;
        double[][] notes;
        switch (type) {
            case 0:  // 长按：清泉击石，单滴
                notes = new double[][]{{0, 120, 0.5, 0}};
                break;
            case 1:  // 开始：两声清脆 do-sol
                notes = new double[][]{{1, 100, 0.5, 0}, {1, 140, 0.5, 120}};
                break;
            case 2:  // 取消：轻柔短促
                notes = new double[][]{{2, 80, 0.35, 0}};
                break;
            default:
                notes = new double[][]{{0, 100, 0.4, 0}};
        }

        int totalMs = 0;
        for (double[] n : notes) {
            int delay = n.length > 3 ? (int) n[3] : 0;
            totalMs = Math.max(totalMs, delay + (int) n[1]);
        }
        int totalSamples = sampleRate * (totalMs + 80) / 1000;
        byte[] pcm = new byte[totalSamples * 2];

        for (double[] n : notes) {
            int kind = (int) n[0];
            int durMs = (int) n[1];
            double vol = n[2];
            int delayMs = n.length > 3 ? (int) n[3] : 0;
            int startSample = sampleRate * delayMs / 1000;
            int num = sampleRate * durMs / 1000;

            for (int i = 0; i < num; i++) {
                double t = i / (double) sampleRate;
                double progress = (double) i / num;
                double sample;
                if (kind == 0) {
                    // 清泉击石：高频正弦 + 泛音 + 极快衰减
                    double fundamental = Math.sin(2 * Math.PI * 5200 * t);
                    double overtone = 0.35 * Math.sin(2 * Math.PI * 8400 * t);
                    double sub = 0.15 * Math.sin(2 * Math.PI * 2600 * t);
                    // 指数衰减（水滴的快速消逝）
                    double env = Math.exp(-12.0 * progress);
                    sample = (fundamental + overtone + sub) * env * vol;
                } else if (kind == 1) {
                    // 开始：清脆音 do(523) sol(784)，正弦+轻微泛音
                    double freq = progress < 0.5 ? 523 : 784;
                    double fundamental = Math.sin(2 * Math.PI * freq * t);
                    double overtone = 0.2 * Math.sin(2 * Math.PI * freq * 2 * t);
                    double env = Math.exp(-6.0 * progress);
                    sample = (fundamental + overtone) * env * vol * 0.6;
                } else {
                    // 取消：轻柔短促，中频
                    double fundamental = Math.sin(2 * Math.PI * 880 * t);
                    double env = Math.exp(-8.0 * progress);
                    sample = fundamental * env * vol * 0.5;
                }
                short val = (short) (sample * 0.8 * Short.MAX_VALUE);
                int idx = (startSample + i) * 2;
                if (idx + 1 < pcm.length) {
                    short existing = (short) ((pcm[idx] & 0xFF) | ((pcm[idx + 1] & 0xFF) << 8));
                    int mixed = existing + val;
                    if (mixed > Short.MAX_VALUE) mixed = Short.MAX_VALUE;
                    if (mixed < Short.MIN_VALUE) mixed = Short.MIN_VALUE;
                    val = (short) mixed;
                    pcm[idx] = (byte) (val & 0xFF);
                    pcm[idx + 1] = (byte) ((val >> 8) & 0xFF);
                }
            }
        }

        int minBuf = AudioTrack.getMinBufferSize(sampleRate,
                AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
        int bufSize = Math.max(minBuf * 2, pcm.length);

        AudioTrack track = null;
        try {
            track = new AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufSize,
                    AudioTrack.MODE_STREAM);
            track.play();
            track.write(pcm, 0, pcm.length);
            Thread.sleep(totalMs + 150);
        } catch (Exception ignored) {
        } finally {
            if (track != null) {
                try { track.stop(); } catch (Exception ignored) {}
                try { track.release(); } catch (Exception ignored) {}
            }
        }
    }
}
