package com.qbump.gtr;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_PHONE_PERMISSION = 1;
    private static final int DURATION_THRESHOLD = 300;
    private static final int SILENT_DURATION = 30 * 60 * 1000; // 30 minutes

    private SharedPreferences preferences;
    private Map<String, Integer> persistentCallers;
    private Ringtone previousRingtone;

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != null && intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED")) {
                // Process SMS
                Bundle bundle = intent.getExtras();
                if (bundle != null) {
                }
            } else if (intent.getAction() != null && intent.getAction().equals(TelephonyManager.ACTION_PHONE_STATE_CHANGED)) {
                String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
                if (state != null && state.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
                    String phoneNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
                    if (!TextUtils.isEmpty(phoneNumber)) {
                        processCall(phoneNumber);
                    }
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        persistentCallers = new HashMap<>();

        // Request phone permissions if not granted
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE, Manifest.permission.READ_SMS}, REQUEST_PHONE_PERMISSION);
        } else {
            startMonitoring();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PHONE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startMonitoring();
            } else {
                Toast.makeText(this, "Permissions not granted. The app cannot function properly.", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
    }

    private void startMonitoring() {
        // Register receiver for SMS and phone state
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.provider.Telephony.SMS_RECEIVED");
        intentFilter.addAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED);
        registerReceiver(receiver, intentFilter);
        TelephonyManager telephonyManager = (Telephony
Manager) getSystemService(Context.TELEPHONY_SERVICE);
        if (telephonyManager != null) {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        }
    }

    private PhoneStateListener phoneStateListener = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int state, String phoneNumber) {
            super.onCallStateChanged(state, phoneNumber);

            switch (state) {
                case TelephonyManager.CALL_STATE_RINGING:
                    if (!TextUtils.isEmpty(phoneNumber)) {
                        processCall(phoneNumber);
                    }
                    break;
                case TelephonyManager.CALL_STATE_IDLE:
                    persistentCallers.remove(phoneNumber);
                    restoreRingtone();
                    break;
            }
        }
    };

    private void processCall(String phoneNumber) {
        int callCount = persistentCallers.containsKey(phoneNumber) ? persistentCallers.get(phoneNumber) + 1 : 1;
        persistentCallers.put(phoneNumber, callCount);

        if (callCount <= DURATION_THRESHOLD) {
            // Mute the ringtone
            muteRingtone();
        } else if (callCount == DURATION_THRESHOLD + 1) {
            playSilentRingtone();
        }
    }

    private void muteRingtone() {
        try {
            if (previousRingtone == null) {
                Uri ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
                previousRingtone = RingtoneManager.getRingtone(this, ringtoneUri);
            }
            if (previousRingtone != null && previousRingtone.isPlaying()) {
                previousRingtone.stop();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void playSilentRingtone() {
        try {
            if (previousRingtone == null) {
                Uri silentRingtoneUri = Uri.parse(Settings.System.DEFAULT_RINGTONE_URI.toString() + "-silent");
                previousRingtone = RingtoneManager.getRingtone(this, silentRingtoneUri);
            }
            if (previousRingtone != null) {
                previousRingtone.play();
            }
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    restoreRingtone();
                }
            }, SILENT_DURATION);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void restoreRingtone() {
        if (previousRingtone != null && !previousRingtone.isPlaying()) {
            previousRingtone.play();
        }
    }
}
