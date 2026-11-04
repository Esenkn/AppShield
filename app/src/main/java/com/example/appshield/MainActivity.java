package com.example.appshield;

import com.example.appshield.security.AppIntegrityChecker;
import com.example.appshield.security.IntegrityResult;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.VpnService;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends Activity {

    private static final int VPN_REQUEST_CODE = 1001;

    private static final int BG = Color.rgb(11, 16, 32);
    private static final int PANEL = Color.rgb(21, 29, 50);
    private static final int BLUE = Color.rgb(79, 140, 255);
    private static final int WHITE = Color.WHITE;
    private static final int MUTED = Color.rgb(165, 177, 202);
    private static final int GREEN = Color.rgb(32, 201, 151);

    private final Set<String> selectedApps = new HashSet<>();
    private final List<ApplicationInfo> installedApps = new ArrayList<>();
    private final List<LinearLayout> appRows = new ArrayList<>();

    private LinearLayout appList;
    private EditText searchInput;

    private TextView statusText;
    private TextView statusDescription;
    private TextView packetValue;
    private TextView byteValue;
    private TextView uptimeValue;
    private TextView selectionCount;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private final Runnable statsUpdater = new Runnable() {
        @Override
        public void run() {
            updateStatistics();
            handler.postDelayed(this, 1000);
        }
    };

    private SharedPreferences statsPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        statsPreferences = getSharedPreferences(
                FirewallService.PREFS,
                MODE_PRIVATE
        );

        createInterface();
        loadInstalledApps();
        updateStatistics();
    }

    private void createInterface() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(BG);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(20), dp(18), dp(30));

        scrollView.addView(root);

        TextView title = new TextView(this);
        title.setText("🛡 AppShield");
        title.setTextColor(WHITE);
        title.setTextSize(29);
        title.setTypeface(null, Typeface.BOLD);
        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Network firewall • On-device control");
        subtitle.setTextColor(MUTED);
        subtitle.setTextSize(13);
        addSpace(root, 5);
        root.addView(subtitle);

        addSpace(root, 22);

        LinearLayout statusCard = createCard(root);

        TextView statusHeading = makeText(
                "PROTECTION STATUS", 12, MUTED, true
        );
        statusCard.addView(statusHeading);

        statusText = makeText("Checking status...", 21, WHITE, true);
        addSpace(statusCard, 10);
        statusCard.addView(statusText);

        statusDescription = makeText(
                "Selected applications are routed through AppShield's local VPN.",
                13, MUTED, false
        );
        addSpace(statusCard, 7);
        statusCard.addView(statusDescription);

        addSpace(root, 14);

        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.HORIZONTAL);

        Button startButton = makeButton("Start protection", BLUE);
        Button stopButton = makeButton("Stop", Color.rgb(190, 65, 85));

        controls.addView(startButton, new LinearLayout.LayoutParams(
                0, dp(48), 1
        ));

        View buttonGap = new View(this);
        controls.addView(buttonGap, new LinearLayout.LayoutParams(dp(10), 1));

        controls.addView(stopButton, new LinearLayout.LayoutParams(
                0, dp(48), 1
        ));

        root.addView(controls);

        startButton.setOnClickListener(v -> requestProtection());
        stopButton.setOnClickListener(v -> stopProtection());

        addSpace(root, 22);

        LinearLayout statsCard = createCard(root);

        TextView statsHeading = makeText(
                "LIVE FIREWALL STATISTICS", 14, WHITE, true
        );
        statsCard.addView(statsHeading);

        addSpace(statsCard, 17);

        packetValue = makeText("0", 25, GREEN, true);
        statsCard.addView(packetValue);

        TextView packetLabel = makeText(
                "Packets read and discarded", 12, MUTED, false
        );
        addSpace(statsCard, 3);
        statsCard.addView(packetLabel);

        addSpace(statsCard, 17);

        byteValue = makeText("0 B", 25, BLUE, true);
        statsCard.addView(byteValue);

        TextView byteLabel = makeText(
                "Total data discarded", 12, MUTED, false
        );
        addSpace(statsCard, 3);
        statsCard.addView(byteLabel);

        addSpace(statsCard, 17);

        uptimeValue = makeText("00:00:00", 25, WHITE, true);
        statsCard.addView(uptimeValue);

        TextView uptimeLabel = makeText(
                "Firewall uptime", 12, MUTED, false
        );
        addSpace(statsCard, 3);
        statsCard.addView(uptimeLabel);

        addSpace(statsCard, 16);

        Button resetButton = makeButton(
                "Reset statistics", Color.rgb(55, 68, 94)
        );
        statsCard.addView(resetButton);

        resetButton.setOnClickListener(v -> resetStatistics());

        addSpace(root, 24);

        // AppShield security integrity scanner
        LinearLayout securityCard = createCard(root);

        TextView securityHeading = makeText(
                "SECURITY SCAN", 16, WHITE, true
        );
        securityCard.addView(securityHeading);

        addSpace(securityCard, 7);

        TextView securityDescription = makeText(
                "Check AppShield's package identity and signing certificate.",
                12, MUTED, false
        );
        securityCard.addView(securityDescription);

        addSpace(securityCard, 12);

        Button scanButton = makeButton(
                "Run security scan", BLUE
        );
        securityCard.addView(scanButton);

        scanButton.setOnClickListener(v -> {
            scanButton.setEnabled(false);
            scanButton.setText("Scanning...");

            new Thread(() -> {
                IntegrityResult result =
                        new AppIntegrityChecker(getApplicationContext()).check();

                runOnUiThread(() -> {
                    scanButton.setEnabled(true);
                    scanButton.setText("Run security scan");
                    showIntegrityReport(result);
                });
            }).start();
        });

        addSpace(root, 24);

        TextView appsHeading = makeText(
                "APPLICATIONS", 17, WHITE, true
        );
        root.addView(appsHeading);

        TextView appsDescription = makeText(
                "Choose the apps whose traffic AppShield should block.",
                12, MUTED, false
        );
        addSpace(root, 5);
        root.addView(appsDescription);

        addSpace(root, 13);

        searchInput = new EditText(this);
        searchInput.setSingleLine(true);
        searchInput.setHint("Search installed apps...");
        searchInput.setHintTextColor(MUTED);
        searchInput.setTextColor(WHITE);
        searchInput.setTextSize(14);
        searchInput.setPadding(dp(14), dp(10), dp(14), dp(10));
        android.graphics.drawable.GradientDrawable searchBackground =
                new android.graphics.drawable.GradientDrawable();
        searchBackground.setColor(Color.rgb(24, 35, 59));
        searchBackground.setCornerRadius(dp(15));
        searchBackground.setStroke(dp(1), Color.rgb(66, 101, 157));
        searchInput.setBackground(searchBackground);
        root.addView(searchInput, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(48)
        ));

        searchInput.addTextChangedListener(
                new android.text.TextWatcher() {
                    @Override
                    public void beforeTextChanged(
                            CharSequence s, int start, int count, int after) {
                    }

                    @Override
                    public void onTextChanged(
                            CharSequence s, int start, int before, int count) {
                        filterApps(s.toString());
                    }

                    @Override
                    public void afterTextChanged(
                            android.text.Editable s) {
                    }
                }
        );

        addSpace(root, 10);

        LinearLayout selectionControls = new LinearLayout(this);
        selectionControls.setOrientation(LinearLayout.HORIZONTAL);

        Button selectAllButton = makeButton(
                "Select all shown", Color.rgb(40, 105, 165)
        );

        Button clearButton = makeButton(
                "Clear selection", Color.rgb(55, 68, 94)
        );

        selectionControls.addView(selectAllButton,
                new LinearLayout.LayoutParams(0, dp(43), 1));

        selectionControls.addView(new View(this),
                new LinearLayout.LayoutParams(dp(8), 1));

        selectionControls.addView(clearButton,
                new LinearLayout.LayoutParams(0, dp(43), 1));

        root.addView(selectionControls);

        selectAllButton.setOnClickListener(v -> {
            for (ApplicationInfo app : getVisibleApps()) {
                selectedApps.add(app.packageName);
            }
            filterApps(searchInput.getText().toString());
        });

        clearButton.setOnClickListener(v -> {
            selectedApps.clear();
            filterApps(searchInput.getText().toString());
        });

        addSpace(root, 10);

        selectionCount = makeText(
                "0 applications selected", 12, MUTED, true
        );
        root.addView(selectionCount);

        addSpace(root, 8);

        appList = new LinearLayout(this);
        appList.setOrientation(LinearLayout.VERTICAL);
        root.addView(appList);

        setContentView(scrollView);
    }

    private void loadInstalledApps() {
        installedApps.clear();

        PackageManager pm = getPackageManager();

        List<ApplicationInfo> apps =
                pm.getInstalledApplications(PackageManager.GET_META_DATA);

        for (ApplicationInfo app : apps) {
            if (!getPackageName().equals(app.packageName)) {
                installedApps.add(app);
            }
        }

        Collections.sort(installedApps,
                new Comparator<ApplicationInfo>() {
                    @Override
                    public int compare(ApplicationInfo a, ApplicationInfo b) {
                        return pm.getApplicationLabel(a).toString()
                                .compareToIgnoreCase(
                                        pm.getApplicationLabel(b).toString()
                                );
                    }
                });

        filterApps("");
    }

    private List<ApplicationInfo> getVisibleApps() {
        List<ApplicationInfo> visible = new ArrayList<>();

        String query = searchInput == null
                ? "" : searchInput.getText().toString()
                .trim().toLowerCase(Locale.ROOT);

        PackageManager pm = getPackageManager();

        for (ApplicationInfo app : installedApps) {
            String label = pm.getApplicationLabel(app)
                    .toString().toLowerCase(Locale.ROOT);

            String packageName = app.packageName.toLowerCase(Locale.ROOT);

            if (label.contains(query) || packageName.contains(query)) {
                visible.add(app);
            }
        }

        return visible;
    }

    private void filterApps(String query) {
        if (appList == null) return;

        appList.removeAllViews();
        appRows.clear();

        PackageManager pm = getPackageManager();

        String search = query.trim().toLowerCase(Locale.ROOT);

        for (ApplicationInfo app : installedApps) {
            String label = pm.getApplicationLabel(app).toString();
            String packageName = app.packageName;

            if (!label.toLowerCase(Locale.ROOT).contains(search)
                    && !packageName.toLowerCase(Locale.ROOT).contains(search)) {
                continue;
            }

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(10), dp(9), dp(10), dp(9));
            android.graphics.drawable.GradientDrawable rowBackground =
                    new android.graphics.drawable.GradientDrawable();
            rowBackground.setColor(Color.rgb(24, 35, 59));
            rowBackground.setCornerRadius(dp(15));
            rowBackground.setStroke(dp(1), Color.rgb(43, 59, 88));
            row.setBackground(rowBackground);
            row.setElevation(dp(2));

            LinearLayout.LayoutParams rowParams =
                    new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    );
            rowParams.bottomMargin = dp(5);

            ImageView icon = new ImageView(this);

            try {
                Drawable drawable = pm.getApplicationIcon(app);
                icon.setImageDrawable(drawable);
            } catch (Exception ignored) {
            }

            row.addView(icon, new LinearLayout.LayoutParams(
                    dp(40), dp(40)
            ));

            LinearLayout details = new LinearLayout(this);
            details.setOrientation(LinearLayout.VERTICAL);
            details.setPadding(dp(10), 0, dp(5), 0);

            TextView appName = makeText(label, 14, WHITE, true);
            TextView packageLabel = makeText(
                    packageName, 10, MUTED, false
            );

            details.addView(appName);
            addSpace(details, 3);
            details.addView(packageLabel);

            row.addView(details, new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1
            ));

            CheckBox checkBox = new CheckBox(this);
            checkBox.setButtonTintList(
                    android.content.res.ColorStateList.valueOf(BLUE)
            );
            checkBox.setChecked(selectedApps.contains(packageName));

            checkBox.setOnCheckedChangeListener((button, checked) -> {
                if (checked) {
                    selectedApps.add(packageName);
                } else {
                    selectedApps.remove(packageName);
                }
                updateSelectionCount();
            });

            row.addView(checkBox);

            row.setOnClickListener(v ->
                    checkBox.setChecked(!checkBox.isChecked())
            );

            appList.addView(row, rowParams);
            appRows.add(row);
        }

        updateSelectionCount();
    }

    private void requestProtection() {
        if (selectedApps.isEmpty()) {
            showMessage(
                    "No applications selected",
                    "Search and select at least one application first."
            );
            return;
        }

        Intent prepareIntent = VpnService.prepare(this);

        if (prepareIntent != null) {
            startActivityForResult(
                    prepareIntent,
                    VPN_REQUEST_CODE
            );
        } else {
            startFirewallService();
        }
    }

    @Override
    protected void onActivityResult(
            int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == VPN_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                startFirewallService();
            } else {
                showMessage(
                        "VPN permission required",
                        "Allow the VPN connection to enable AppShield."
                );
            }
        }
    }

    private void startFirewallService() {
        Intent intent = new Intent(this, FirewallService.class);
        intent.setAction(FirewallService.ACTION_START);

        intent.putStringArrayListExtra(
                FirewallService.EXTRA_BLOCKED_APPS,
                new ArrayList<>(selectedApps)
        );

        if (Build.VERSION.SDK_INT >= 26) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }

        statusText.setText("Protection requested");
        statusDescription.setText(
                "Android is starting the local VPN firewall."
        );
    }

    private void stopProtection() {
        Intent intent = new Intent(this, FirewallService.class);
        intent.setAction(FirewallService.ACTION_STOP);
        startService(intent);

        statusText.setText("Protection is OFF");
        statusDescription.setText("Firewall stopped.");
    }

    private void updateStatistics() {
        if (statsPreferences == null) return;

        long packets = statsPreferences.getLong(
                FirewallService.KEY_PACKETS, 0
        );

        long bytes = statsPreferences.getLong(
                FirewallService.KEY_BYTES, 0
        );

        long started = statsPreferences.getLong(
                FirewallService.KEY_STARTED, 0
        );

        boolean active = statsPreferences.getBoolean(
                FirewallService.KEY_ACTIVE, false
        );

        packetValue.setText(String.format(Locale.US, "%,d", packets));
        byteValue.setText(formatBytes(bytes));

        if (active && started > 0) {
            uptimeValue.setText(
                    formatDuration(System.currentTimeMillis() - started)
            );

            statusText.setText("Protection is ACTIVE");
            statusText.setTextColor(GREEN);
            statusDescription.setText(
                    "Selected applications are routed through the local VPN."
            );
        } else {
            uptimeValue.setText("00:00:00");
            statusText.setText("Protection is OFF");
            statusText.setTextColor(WHITE);
            statusDescription.setText(
                    "Select applications and start protection."
            );
        }
    }

    private void showIntegrityReport(IntegrityResult result) {
        StringBuilder report = new StringBuilder();

        report.append(result.isHealthy()
                ? "STATUS: No integrity warnings detected"
                : "STATUS: Integrity warnings detected");

        report.append("\\n\\nPackage: ")
                .append(result.getPackageName());

        report.append("\\nDebug build: ")
                .append(result.isDebuggable() ? "Yes" : "No");

        report.append("\\nSigning certificate available: ")
                .append(result.getSigningCertificateAvailable() ? "Yes" : "No");

        report.append("\\nTrusted release certificate: ")
                .append(result.getReleaseCertificateMatches() ? "MATCH" : "NO MATCH");

        if (!result.getCertificateSha256().isEmpty()) {
            report.append("\\n\\nCertificate SHA-256:\\n")
                    .append(result.getCertificateSha256());
        }

        if (result.getApkSha256() != null) {
            report.append("\\n\\nAPK SHA-256:\\n")
                    .append(result.getApkSha256());
        }

        if (!result.getFindings().isEmpty()) {
            report.append("\\n\\nWARNINGS:");
            for (String finding : result.getFindings()) {
                report.append("\\n• ").append(finding);
            }
        }

        if (!result.getInformation().isEmpty()) {
            report.append("\\n\\nINFORMATION:");
            for (String item : result.getInformation()) {
                report.append("\\n• ").append(item);
            }
        }

        report.append("\\n\\nNote: This scan checks app identity and signing. It is not a complete malware scan.");

        new AlertDialog.Builder(this)
                .setTitle("AppShield Security Report")
                .setMessage(report.toString())
                .setPositiveButton("OK", null)
                .show();
    }

    private void resetStatistics() {
        new AlertDialog.Builder(this)
                .setTitle("Reset statistics?")
                .setMessage(
                        "This will clear the packet and data counters."
                )
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Reset", (dialog, which) -> {
                    if (statsPreferences.getBoolean(
                            FirewallService.KEY_ACTIVE, false)) {

                        Intent intent = new Intent(
                                this, FirewallService.class
                        );
                        intent.setAction(
                                FirewallService.ACTION_RESET_STATS
                        );

                        startService(intent);

                    } else {
                        statsPreferences.edit()
                                .putLong(FirewallService.KEY_PACKETS, 0)
                                .putLong(FirewallService.KEY_BYTES, 0)
                                .apply();
                    }

                    updateStatistics();
                })
                .show();
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }

        if (bytes < 1024L * 1024L) {
            return String.format(
                    Locale.US, "%.2f KB", bytes / 1024.0
            );
        }

        if (bytes < 1024L * 1024L * 1024L) {
            return String.format(
                    Locale.US, "%.2f MB",
                    bytes / (1024.0 * 1024.0)
            );
        }

        return String.format(
                Locale.US, "%.2f GB",
                bytes / (1024.0 * 1024.0 * 1024.0)
        );
    }

    private String formatDuration(long milliseconds) {
        long seconds = Math.max(0, milliseconds / 1000);

        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long remainingSeconds = seconds % 60;

        return String.format(
                Locale.US,
                "%02d:%02d:%02d",
                hours, minutes, remainingSeconds
        );
    }

    private LinearLayout createCard(LinearLayout parent) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(17), dp(17), dp(17), dp(17));

        android.graphics.drawable.GradientDrawable background =
                new android.graphics.drawable.GradientDrawable(
                        android.graphics.drawable.GradientDrawable.Orientation.TL_BR,
                        new int[] {
                                Color.rgb(31, 44, 76),
                                Color.rgb(18, 27, 49)
                        }
                );

        background.setCornerRadius(dp(22));
        background.setStroke(dp(1), Color.rgb(62, 91, 145));
        card.setBackground(background);
        card.setElevation(dp(8));

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        params.bottomMargin = dp(3);
        parent.addView(card, params);
        return card;
    }

    private TextView makeText(
            String value, float size, int color, boolean bold) {

        TextView text = new TextView(this);
        text.setText(value);
        text.setTextSize(size);
        text.setTextColor(color);

        if (bold) {
            text.setTypeface(null, Typeface.BOLD);
        }

        return text;
    }

    private Button makeButton(String label, int color) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextColor(WHITE);
        button.setTextSize(13);
        button.setTypeface(null, Typeface.BOLD);
        button.setAllCaps(false);
        button.setPadding(dp(12), dp(8), dp(12), dp(8));

        int dark = Color.rgb(
                Math.max(0, Color.red(color) - 35),
                Math.max(0, Color.green(color) - 35),
                Math.max(0, Color.blue(color) - 35)
        );

        android.graphics.drawable.GradientDrawable normal =
                new android.graphics.drawable.GradientDrawable(
                        android.graphics.drawable.GradientDrawable.Orientation.TL_BR,
                        new int[] { color, dark }
                );
        normal.setCornerRadius(dp(16));
        normal.setStroke(dp(1), Color.argb(110, 180, 210, 255));

        android.graphics.drawable.GradientDrawable pressed =
                new android.graphics.drawable.GradientDrawable(
                        android.graphics.drawable.GradientDrawable.Orientation.TL_BR,
                        new int[] { dark, color }
                );
        pressed.setCornerRadius(dp(16));
        pressed.setStroke(dp(1), Color.rgb(120, 160, 230));

        android.graphics.drawable.StateListDrawable states =
                new android.graphics.drawable.StateListDrawable();

        states.addState(
                new int[] { android.R.attr.state_pressed },
                pressed
        );
        states.addState(
                new int[] { android.R.attr.state_enabled },
                normal
        );
        states.addState(new int[] {}, normal);

        button.setBackground(states);
        button.setElevation(dp(7));

        button.setOnTouchListener((view, event) -> {
            if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                view.animate()
                        .scaleX(0.96f)
                        .scaleY(0.96f)
                        .setDuration(90)
                        .start();
            } else if (event.getAction() == android.view.MotionEvent.ACTION_UP
                    || event.getAction() == android.view.MotionEvent.ACTION_CANCEL) {
                view.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(130)
                        .start();
            }
            return false;
        });

        return button;
    }

    private void addSpace(LinearLayout parent, int heightDp) {
        View space = new View(this);
        parent.addView(space, new LinearLayout.LayoutParams(
                1, dp(heightDp)
        ));
    }

    private int dp(float value) {
        return (int) (value * getResources()
                .getDisplayMetrics().density + 0.5f);
    }

    private void updateSelectionCount() {
        if (selectionCount != null) {
            selectionCount.setText(
                    selectedApps.size() + " applications selected"
            );
        }
    }

    private void showMessage(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.removeCallbacks(statsUpdater);
        handler.post(statsUpdater);
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(statsUpdater);
        super.onPause();
    }
}
