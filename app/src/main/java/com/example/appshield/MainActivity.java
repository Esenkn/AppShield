package com.example.appshield;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.VpnService;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends Activity {
    private static final int VPN_REQUEST = 1001;
    private final Set<String> selectedApps = new HashSet<>();
    private final List<ApplicationInfo> installedApps = new ArrayList<>();
    private LinearLayout appList;
    private EditText searchBox;
    private TextView statusText;
    private TextView countText;
    private boolean protectionActive = false;

    private int bg = Color.rgb(11, 16, 32);
    private int panel = Color.rgb(21, 29, 50);
    private int blue = Color.rgb(79, 140, 255);
    private int white = Color.WHITE;
    private int muted = Color.rgb(164, 177, 204);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildScreen();
        loadApps();
        renderApps("");
    }

    private GradientDrawable rounded(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radius);
        return d;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private LinearLayout vertical() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    private LinearLayout.LayoutParams params(int width, int height) {
        return new LinearLayout.LayoutParams(width, height);
    }

    private void buildScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(bg);

        LinearLayout root = vertical();
        root.setPadding(20, 24, 20, 24);
        scroll.addView(root);

        TextView logo = text("🛡  AppShield", 28, white, true);
        root.addView(logo);

        TextView subtitle = text("Network firewall • On-device control", 14, muted, false);
        LinearLayout.LayoutParams subParams = params(-1, -2);
        subParams.topMargin = 5;
        root.addView(subtitle, subParams);

        LinearLayout statusCard = vertical();
        statusCard.setPadding(16, 16, 16, 16);
        statusCard.setBackground(rounded(panel, 22));
        LinearLayout.LayoutParams cardParams = params(-1, -2);
        cardParams.topMargin = 24;
        root.addView(statusCard, cardParams);

        statusText = text("● Protection is OFF", 18, muted, true);
        statusCard.addView(statusText);

        TextView warning = text(
            "Selected apps will be routed into a local VPN and their network packets dropped. Android will ask for VPN permission.",
            13, muted, false);
        LinearLayout.LayoutParams warningParams = params(-1, -2);
        warningParams.topMargin = 10;
        statusCard.addView(warning, warningParams);

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams buttonsParams = params(-1, -2);
        buttonsParams.topMargin = 16;
        statusCard.addView(buttons, buttonsParams);

        Button start = new Button(this);
        start.setText("Start protection");
        start.setTextColor(white);
        start.setBackground(rounded(blue, 14));
        buttons.addView(start, new LinearLayout.LayoutParams(0, 52, 1));

        Button stop = new Button(this);
        stop.setText("Stop");
        stop.setTextColor(white);
        stop.setBackground(rounded(Color.rgb(65, 76, 99), 14));
        LinearLayout.LayoutParams stopParams = new LinearLayout.LayoutParams(0, 52, 1);
        stopParams.leftMargin = 10;
        buttons.addView(stop, stopParams);

        start.setOnClickListener(v -> startProtection());
        stop.setOnClickListener(v -> stopProtection());

        TextView appsHeading = text("Choose apps to block", 20, white, true);
        LinearLayout.LayoutParams headingParams = params(-1, -2);
        headingParams.topMargin = 26;
        root.addView(appsHeading, headingParams);

        countText = text("Loading installed apps…", 13, muted, false);
        LinearLayout.LayoutParams countParams = params(-1, -2);
        countParams.topMargin = 6;
        root.addView(countText, countParams);

        searchBox = new EditText(this);
        searchBox.setSingleLine(true);
        searchBox.setTextColor(white);
        searchBox.setHintTextColor(muted);
        searchBox.setHint("Search installed apps");
        searchBox.setPadding(16, 6, 16, 6);
        searchBox.setBackground(rounded(panel, 14));
        LinearLayout.LayoutParams searchParams = params(-1, 52);
        searchParams.topMargin = 14;
        root.addView(searchBox, searchParams);

        LinearLayout selectionButtons = new LinearLayout(this);
        selectionButtons.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams selectParams = params(-1, -2);
        selectParams.topMargin = 8;
        root.addView(selectionButtons, selectParams);

        Button selectAll = new Button(this);
        selectAll.setText("Select all shown");
        selectAll.setOnClickListener(v -> {
            String query = searchBox.getText().toString().trim().toLowerCase();
            for (ApplicationInfo info : installedApps) {
                String name = getAppName(info).toLowerCase();
                if (query.isEmpty() || name.contains(query) ||
                    info.packageName.toLowerCase().contains(query)) {
                    selectedApps.add(info.packageName);
                }
            }
            renderApps(query);
        });
        selectionButtons.addView(selectAll, new LinearLayout.LayoutParams(0, 48, 1));

        Button clear = new Button(this);
        clear.setText("Clear selection");
        clear.setOnClickListener(v -> {
            selectedApps.clear();
            renderApps(searchBox.getText().toString().trim().toLowerCase());
        });
        LinearLayout.LayoutParams clearParams = new LinearLayout.LayoutParams(0, 48, 1);
        clearParams.leftMargin = 8;
        selectionButtons.addView(clear, clearParams);

        appList = vertical();
        LinearLayout.LayoutParams listParams = params(-1, -2);
        listParams.topMargin = 8;
        root.addView(appList, listParams);

        setContentView(scroll);

        searchBox.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                renderApps(s.toString().trim().toLowerCase());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private String getAppName(ApplicationInfo info) {
        CharSequence name = info.loadLabel(getPackageManager());
        return name == null ? info.packageName : name.toString();
    }

    private void loadApps() {
        try {
            List<ApplicationInfo> apps = getPackageManager().getInstalledApplications(
                PackageManager.GET_META_DATA);
            for (ApplicationInfo info : apps) {
                if (!info.packageName.equals(getPackageName())) {
                    installedApps.add(info);
                }
            }
            Collections.sort(installedApps, new Comparator<ApplicationInfo>() {
                @Override public int compare(ApplicationInfo a, ApplicationInfo b) {
                    return getAppName(a).compareToIgnoreCase(getAppName(b));
                }
            });
        } catch (Exception e) {
            Toast.makeText(this, "Could not load installed apps", Toast.LENGTH_LONG).show();
        }
    }

    private void renderApps(String query) {
        if (appList == null) return;
        appList.removeAllViews();
        int shown = 0;

        for (ApplicationInfo info : installedApps) {
            String name = getAppName(info);
            if (!query.isEmpty()
                && !name.toLowerCase().contains(query)
                && !info.packageName.toLowerCase().contains(query)) {
                continue;
            }

            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(12, 8, 12, 8);
            row.setBackground(rounded(panel, 14));
            LinearLayout.LayoutParams rowParams = params(-1, -2);
            rowParams.topMargin = 6;
            appList.addView(row, rowParams);

            android.widget.ImageView icon = new android.widget.ImageView(this);
            try {
                icon.setImageDrawable(info.loadIcon(getPackageManager()));
            } catch (Exception ignored) {}
            row.addView(icon, params(42, 42));

            LinearLayout labels = vertical();
            LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(0, -2, 1);
            labelParams.leftMargin = 12;
            row.addView(labels, labelParams);

            labels.addView(text(name, 15, white, true));
            TextView packageLabel = text(info.packageName, 10, muted, false);
            packageLabel.setMaxLines(1);
            packageLabel.setSingleLine(true);
            labels.addView(packageLabel);

            CheckBox check = new CheckBox(this);
            check.setButtonTintList(android.content.res.ColorStateList.valueOf(blue));
            check.setChecked(selectedApps.contains(info.packageName));
            check.setOnCheckedChangeListener((button, checked) -> {
                if (checked) selectedApps.add(info.packageName);
                else selectedApps.remove(info.packageName);
                updateCount();
            });
            row.addView(check, params(48, 48));
            row.setOnClickListener(v -> check.setChecked(!check.isChecked()));
            shown++;
        }

        countText.setText(shown + " apps shown • " + selectedApps.size() + " selected");
        if (shown == 0) {
            TextView empty = text("No matching apps found.", 14, muted, false);
            empty.setPadding(12, 20, 12, 20);
            appList.addView(empty);
        }
    }

    private void updateCount() {
        countText.setText(countText.getText().toString().split(" • ")[0]
            + " • " + selectedApps.size() + " selected");
    }

    private void startProtection() {
        if (selectedApps.isEmpty()) {
            new AlertDialog.Builder(this)
                .setTitle("No apps selected")
                .setMessage("Select at least one app to block.")
                .setPositiveButton("OK", null)
                .show();
            return;
        }
        Intent intent = VpnService.prepare(this);
        if (intent != null) {
            startActivityForResult(intent, VPN_REQUEST);
        } else {
            onActivityResult(VPN_REQUEST, RESULT_OK, null);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == VPN_REQUEST && resultCode == RESULT_OK) {
            Intent service = new Intent(this, FirewallService.class);
            service.setAction(FirewallService.ACTION_START);
            service.putStringArrayListExtra(FirewallService.EXTRA_BLOCKED_APPS,
                new ArrayList<>(selectedApps));
            if (android.os.Build.VERSION.SDK_INT >= 26) startForegroundService(service);
            else startService(service);
            protectionActive = true;
            statusText.setText("● Protection requested");
            statusText.setTextColor(Color.rgb(32, 201, 151));
            Toast.makeText(this, "Starting AppShield protection", Toast.LENGTH_SHORT).show();
        } else if (requestCode == VPN_REQUEST) {
            Toast.makeText(this, "VPN permission was not granted", Toast.LENGTH_LONG).show();
        }
    }

    private void stopProtection() {
        Intent service = new Intent(this, FirewallService.class);
        service.setAction(FirewallService.ACTION_STOP);
        startService(service);
        protectionActive = false;
        statusText.setText("● Protection is OFF");
        statusText.setTextColor(muted);
        Toast.makeText(this, "Protection stopped", Toast.LENGTH_SHORT).show();
    }
}
