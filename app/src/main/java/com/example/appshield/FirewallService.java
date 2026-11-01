package com.example.appshield;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.net.VpnService;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.FileInputStream;
import java.util.ArrayList;

public class FirewallService extends VpnService {

    public static final String ACTION_START =
            "com.example.appshield.START_FIREWALL";

    public static final String ACTION_STOP =
            "com.example.appshield.STOP_FIREWALL";

    public static final String EXTRA_BLOCKED_APPS = "blocked_apps";

    private static final String TAG = "AppShield";
    private static final String CHANNEL_ID = "appshield_firewall";
    private static final int NOTIFICATION_ID = 4101;

    private ParcelFileDescriptor vpnInterface;
    private Thread packetThread;
    private volatile boolean running = false;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent == null || ACTION_STOP.equals(intent.getAction())) {
            stopFirewall();
            return START_NOT_STICKY;
        }

        if (ACTION_START.equals(intent.getAction())) {

            ArrayList<String> apps =
                    intent.getStringArrayListExtra(EXTRA_BLOCKED_APPS);

            if (apps == null || apps.isEmpty()) {
                stopFirewall();
                return START_NOT_STICKY;
            }

            if (!running) {
                startFirewall(apps);
            }
        }

        return START_NOT_STICKY;
    }

    private void startFirewall(ArrayList<String> apps) {

        try {
            Builder builder = new Builder()
                    .setSession("AppShield Firewall")
                    .setMtu(1500)
                    .addAddress("10.77.0.1", 32)
                    .addRoute("0.0.0.0", 0)
                    .addAddress("fd77:77::1", 128)
                    .addRoute("::", 0);

            int added = 0;

            for (String packageName : apps) {
                try {
                    builder.addAllowedApplication(packageName);
                    added++;
                } catch (Exception e) {
                    Log.w(TAG, "Could not add app: " + packageName, e);
                }
            }

            if (added == 0) {
                stopSelf();
                return;
            }

            // Establish the VPN before requesting the VPN-specific
            // systemExempted foreground-service type.
            vpnInterface = builder.establish();

            if (vpnInterface == null) {
                throw new IllegalStateException(
                        "Android could not establish the VPN interface");
            }

            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(
                        NOTIFICATION_ID,
                        buildNotification(),
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED
                );
            } else {
                startForeground(NOTIFICATION_ID, buildNotification());
            }

            running = true;

            packetThread = new Thread(
                    this::discardPackets,
                    "AppShield-PacketDrop"
            );

            packetThread.start();

            Log.i(TAG, "Firewall started for " + added + " selected apps");

        } catch (Exception e) {
            Log.e(TAG, "Firewall failed to start", e);
            stopFirewall();
        }
    }

    private void discardPackets() {

        ParcelFileDescriptor descriptor = vpnInterface;

        if (descriptor == null) {
            return;
        }

        byte[] buffer = new byte[32767];

        try (FileInputStream input =
                     new FileInputStream(descriptor.getFileDescriptor())) {

            while (running) {
                int length = input.read(buffer);

                if (length < 0) {
                    break;
                }

                // Intentionally discard selected apps' packets.
            }

        } catch (Exception e) {
            if (running) {
                Log.e(TAG, "Packet reader stopped", e);
            }
        } finally {
            if (running) {
                running = false;
                stopForeground(true);
                stopSelf();
            }
        }
    }

    private Notification buildNotification() {

        Intent openApp = new Intent(this, MainActivity.class);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                openApp,
                PendingIntent.FLAG_UPDATE_CURRENT |
                        (Build.VERSION.SDK_INT >= 23
                                ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        Notification.Builder builder;

        if (Build.VERSION.SDK_INT >= 26) {
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }

        return builder
                .setContentTitle("AppShield is active")
                .setContentText(
                        "Blocking network traffic for selected apps"
                )
                .setSmallIcon(android.R.drawable.ic_lock_lock)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }

    private void createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= 26) {

            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "AppShield Firewall",
                    NotificationManager.IMPORTANCE_LOW
            );

            channel.setDescription(
                    "Status of AppShield network protection"
            );

            NotificationManager manager =
                    getSystemService(NotificationManager.class);

            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void stopFirewall() {

        running = false;

        if (vpnInterface != null) {
            try {
                vpnInterface.close();
            } catch (Exception e) {
                Log.w(TAG, "Error closing VPN interface", e);
            }

            vpnInterface = null;
        }

        if (Build.VERSION.SDK_INT >= 24) {
            stopForeground(STOP_FOREGROUND_REMOVE);
        } else {
            stopForeground(true);
        }

        stopSelf();
    }

    @Override
    public void onRevoke() {
        stopFirewall();
        super.onRevoke();
    }

    @Override
    public void onDestroy() {
        stopFirewall();
        super.onDestroy();
    }
}
