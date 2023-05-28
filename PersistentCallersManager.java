package com.qbump.gtr

import java.util.HashMap;
import java.util.Map;

public class PersistentCallersManager {
    private Map<String, Integer> persistentCallers;

    public PersistentCallersManager() {
        persistentCallers = new HashMap<>();
    }

    public void processCall(String phoneNumber) {
        int callCount = persistentCallers.containsKey(phoneNumber) ? persistentCallers.get(phoneNumber) + 1 : 1;
        persistentCallers.put(phoneNumber, callCount);

        if (callCount <= DURATION_THRESHOLD) {
            // Mute the ringtone
            muteRingtone();
        } else if (callCount == DURATION_THRESHOLD + 1) {
            // Play silent ringtone after exceeding threshold
            playSilentRingtone();
        }
    }

    private void muteRingtone() {
        // Code for muting the ringtone
    }

    private void playSilentRingtone() {
        // Code for playing the silent ringtone
    }
}
