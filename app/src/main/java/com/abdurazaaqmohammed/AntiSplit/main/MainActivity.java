package com.abdurazaaqmohammed.AntiSplit.main;

import static android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION;
import static android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS;
import static com.abdurazaaqmohammed.AntiSplit.main.LegacyUtils.aboveSdk20;
import static com.reandroid.apkeditor.merge.LogUtil.logEnabled;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.widget.NestedScrollView;

import com.abdurazaaqmohammed.AntiSplit.R;
import com.fom.storage.media.AndroidXI;
import com.github.paul035.LocaleHelper;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textview.MaterialTextView;
import com.reandroid.apk.ApkBundle;
import com.reandroid.apkeditor.merge.LogUtil;
import com.reandroid.apkeditor.merge.Merger;
import com.starry.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.nio.channels.ClosedByInterruptException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** @noinspection deprecation*/
public class MainActivity extends AppCompatActivity implements Merger.LogListener {
    private static boolean ask = true;
    private static boolean showDialog;
    private static boolean signApk;
    private static boolean selectSplitsForDevice;
    private Uri splitAPKUri;
    private ArrayList<Uri> uris;
    private boolean urisAreSplitApks = true;
    public static boolean errorOccurred;
    public static String lang;
    public static int theme;
    public static int sortMode;
    public DeviceSpecsUtil DeviceSpecsUtil;
    private String pkgName;
    private static String suffix;
    private boolean systemTheme;

    public Handler getHandler() {
        return handler;
    }

    /** @noinspection AssignmentUsedAsCondition*/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DynamicColors.applyToActivitiesIfAvailable(getApplication());
        handler = new Handler(Looper.getMainLooper());

        deleteDir(getCacheDir());
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        SharedPreferences settings = getSharedPreferences("set", Context.MODE_PRIVATE);
        boolean dark = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        setTheme(theme = settings.getInt("theme", dark
                    ? com.google.android.material.R.style.Theme_Material3_Dark_NoActionBar : com.google.android.material.R.style.Theme_Material3_Light_NoActionBar));

        DeviceSpecsUtil = new DeviceSpecsUtil(this);

        setContentView(R.layout.activity_main);
        if(theme == R.style.Theme_MyApp_Black) findViewById(R.id.main).setBackgroundColor(Color.BLACK);
        scrollView = findViewById(R.id.scrollView);
        logField = findViewById(R.id.logField);
        suffix = settings.getString("suffix", "_antisplit");
        lang = settings.getString("lang", "en");
        if(lang.equals(Locale.getDefault().getLanguage())) rss = getResources();
        else updateLang(LocaleHelper.setLocale(MainActivity.this, lang).getResources(), null);
        if(aboveSdk20) {
            getWindow().addFlags(FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            int transparent = rss.getColor(android.R.color.transparent);
            getWindow().setNavigationBarColor(transparent);
            getWindow().setStatusBarColor(transparent);
        }

        if (!LegacyUtils.supportsWriteExternalStorage) {
            EdgeToEdge.enable(this);
            getWindow().setStatusBarContrastEnforced(true);
            getWindow().setNavigationBarContrastEnforced(true);
        }
        // Fetch settings from SharedPreferences
        signApk = settings.getBoolean("signApk", true);
        showDialog = settings.getBoolean("showDialog", false);
        selectSplitsForDevice = settings.getBoolean("selectSplitsForDevice", false);
        logEnabled = settings.getBoolean("logEnabled", true);
        ask = settings.getBoolean("ask", true);
        systemTheme = settings.getBoolean("systemTheme", true);
        sortMode = settings.getInt("sortMode", 0);
        LogUtil.setLogListener(this);

        View selectFromInstalledApps = findViewById(R.id.fromAppsButton);
        if(aboveSdk20) selectFromInstalledApps.setOnClickListener(v3 -> {
            AlertDialog ad = new MaterialAlertDialogBuilder(MainActivity.this).setNegativeButton(rss.getString(R.string.cancel), null).create();
            PackageManager pm = getPackageManager();
            List<PackageInfo> packageInfoList = pm.getInstalledPackages(0);

            List<AppInfo> appInfoList = new ArrayList<>();

            for (PackageInfo packageInfo : packageInfoList) {
                try {
                    String packageName = packageInfo.packageName;
                    ApplicationInfo ai = pm.getApplicationInfo(packageName, 0);
                    if (ai.splitSourceDirs != null) {
                        appInfoList.add(new AppInfo(
                                (String) pm.getApplicationLabel(ai),
                                pm.getApplicationIcon(ai),
                                packageName,
                                packageInfo.lastUpdateTime,
                                packageInfo.firstInstallTime));
                    }
                } catch (PackageManager.NameNotFoundException ignored) {}
            }
            if(sortMode == 0) Collections.sort(appInfoList, Comparator.comparing((AppInfo p) -> p.name.toLowerCase(Locale.ROOT)));
            else Collections.sort(appInfoList, Comparator.comparing((AppInfo p) -> sortMode == 1 ? p.lastUpdated : p.firstInstall).reversed());

            LayoutInflater layoutInflater = LayoutInflater.from(MainActivity.this);
            View dialogView = layoutInflater.inflate(R.layout.dialog_search, null);

            ListView listView = dialogView.findViewById(R.id.list_view);
            final AppListArrayAdapter adapter = new AppListArrayAdapter(MainActivity.this, appInfoList, true);
            listView.setAdapter(adapter);

            listView.setOnItemClickListener((parent, view, position, id) -> {
                ad.dismiss();
                boolean realAskValue = ask;
                ask = true;
                pkgName = adapter.filteredAppInfoList.get(position).packageName;
                selectDirToSaveAPKOrSaveNow();
                ask = realAskValue;
            });
            EditText searchBar = dialogView.findViewById(R.id.search_bar);

            View clearButton = dialogView.findViewById(R.id.clear_button);
    //            if(theme == R.style.Theme_MyApp_Black)
            clearButton.setOnClickListener(v -> searchBar.setText(""));
    //            else dialogView.<TextInputLayout>findViewById(R.id.tilly).setEndIconMode(TextInputLayout.END_ICON_CLEAR_TEXT);

            searchBar.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    // No action needed here
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    clearButton.setVisibility(TextUtils.isEmpty(s) ? View.GONE : View.VISIBLE);
                    adapter.getFilter().filter(s);
                }

                @Override
                public void afterTextChanged(Editable s) {
                    // No action needed here
                }
            });
            dialogView.findViewById(R.id.filter_button).setOnClickListener(v -> {
                PopupMenu popupMenu = new PopupMenu(this, v);
                popupMenu.getMenuInflater().inflate(R.menu.sort_menu, popupMenu.getMenu());

                popupMenu.setOnMenuItemClickListener(item -> {
                    int itemId = item.getItemId();
                    if (itemId == R.id.sort_name) {
                        sortMode = 0;
                        Collections.sort(appInfoList, Comparator.comparing((AppInfo p) -> p.name.toLowerCase(Locale.ROOT)));
                    } else if (itemId == R.id.date_updated) {
                        sortMode = 1;
                        Collections.sort(appInfoList, Comparator.comparing((AppInfo p) ->  p.lastUpdated).reversed());
                    } else {
                        Collections.sort(appInfoList, Comparator.comparing((AppInfo p) ->  p.firstInstall).reversed());
                        sortMode = 2;
                    }
                    listView.setAdapter(new AppListArrayAdapter(MainActivity.this, appInfoList, true));
                    return true;
                });

                popupMenu.show();
            });

            ad.setView(dialogView);
            styleAlertDialog(ad);
        });
        else selectFromInstalledApps.setVisibility(View.GONE);

        findViewById(R.id.settingsButton).setOnClickListener(v -> {
            ScrollView settingsDialog = (ScrollView) LayoutInflater.from(this).inflate(R.layout.setty, null);

            ((TextView) settingsDialog.findViewById(R.id.langPicker)).setText(rss.getString(R.string.lang));
            ((TextView) settingsDialog.findViewById(R.id.logToggle)).setText(rss.getString(R.string.enable_logs));
            ((TextView) settingsDialog.findViewById(R.id.ask)).setText(rss.getString(R.string.ask));
            ((TextView) settingsDialog.findViewById(R.id.showDialogToggle)).setText(rss.getString(R.string.show_dialog));
            ((TextView) settingsDialog.findViewById(R.id.signToggle)).setText(rss.getString(R.string.sign_apk));
            ((TextInputLayout) settingsDialog.findViewById(R.id.suffixLayout)).setHint(rss.getString(R.string.suffix));
            TextInputEditText suffixInput = settingsDialog.findViewById(R.id.suffixInput);
            suffixInput.setText(suffix);
            suffixInput.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    suffix = s.toString();
                }

                @Override
                public void afterTextChanged(Editable s) {

                }
            });
            MaterialButtonToggleGroup themeButtons = settingsDialog.findViewById(R.id.themeToggleGroup);
            themeButtons.check(
                    systemTheme ? R.id.systemThemeButton :
                    theme == com.google.android.material.R.style.Theme_Material3_Light_NoActionBar ? R.id.lightThemeButton :
                    theme == com.google.android.material.R.style.Theme_Material3_Dark_NoActionBar ? R.id.darkThemeButton :
                    R.id.blackThemeButton
            );
            themeButtons.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
                if (isChecked) {
                    systemTheme = false;
                    if (checkedId == R.id.lightThemeButton) {
                        themeButtons.check(R.id.lightThemeButton);
                        theme = com.google.android.material.R.style.Theme_Material3_Light_NoActionBar;
                    } else if (checkedId == R.id.darkThemeButton) {
                        themeButtons.findViewById(R.id.darkThemeButton);
                        theme = com.google.android.material.R.style.Theme_Material3_Dark_NoActionBar;
                    } else if (checkedId == R.id.blackThemeButton) {
                        themeButtons.check(R.id.blackThemeButton);
                        theme = R.style.Theme_MyApp_Black;
                    } else {
                        systemTheme = true;
                        themeButtons.check(R.id.systemThemeButton);
                        theme = ((rss.getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES) ?
                                com.google.android.material.R.style.Theme_Material3_Dark_NoActionBar : com.google.android.material.R.style.Theme_Material3_Light_NoActionBar;
                    }

                    settings.edit().putInt("theme", theme).apply();
                    setTheme(theme);
                    recreate();
                }
            });

            CompoundButton logSwitch = settingsDialog.findViewById(R.id.logToggle);
            logSwitch.setChecked(logEnabled);
            logSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> logEnabled = isChecked);

            CompoundButton signToggle = settingsDialog.findViewById(R.id.signToggle);
            signToggle.setChecked(signApk);
            signToggle.setOnCheckedChangeListener((buttonView, isChecked) -> signApk = isChecked);

            CompoundButton selectSplitsAutomaticallySwitch = settingsDialog.findViewById(R.id.selectSplitsForDeviceToggle);
            CompoundButton showDialogSwitch = settingsDialog.findViewById(R.id.showDialogToggle);

            showDialogSwitch.setChecked(showDialog);
            showDialogSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if(showDialog = isChecked) selectSplitsAutomaticallySwitch.setChecked(selectSplitsForDevice = false);
            });

            selectSplitsAutomaticallySwitch.setChecked(selectSplitsForDevice);
            selectSplitsAutomaticallySwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if(selectSplitsForDevice = isChecked) showDialogSwitch.setChecked(showDialog = false);
            });

            CompoundButton askSwitch = settingsDialog.findViewById(R.id.ask);
            askSwitch.setChecked(ask);
            askSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if(!(ask = isChecked)) checkStoragePerm();
            });

            settingsDialog.findViewById(R.id.langPicker).setOnClickListener(v2 -> {
                String[] langs = rss.getStringArray(R.array.langs);
                String[] display = rss.getStringArray(R.array.langs_display);

                AlertDialog ad = new MaterialAlertDialogBuilder(this).setSingleChoiceItems(display, -1, (dialog, which) -> {
                    updateLang(LocaleHelper.setLocale(this, lang = langs[which]).getResources(), settingsDialog);
                    dialog.dismiss();
                }).create();
                styleAlertDialog(ad);
                ad.getListView().setAdapter(new ArrayAdapter<>(this, android.R.layout.select_dialog_singlechoice, display));
                for (int i = 0; i < langs.length; i++) {
                    if (Objects.equals(lang, langs[i])) {
                        ad.getListView().setItemChecked(i, true);
                        break;
                    }
                }
            });
            MaterialTextView title = new MaterialTextView(this);
            title.setText(rss.getString(R.string.settings));
            int size = 20;
            title.setPadding(size,size,size,size);
            title.setTextSize(size);
            title.setGravity(Gravity.CENTER);
            styleAlertDialog(new MaterialAlertDialogBuilder(this).setCustomTitle(title).setView(settingsDialog)
                    .setPositiveButton(rss.getString(R.string.close), (dialog, which) -> dialog.dismiss()).create());
        });

        findViewById(R.id.decodeButton).setOnClickListener(v -> startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT)
                        .addCategory(Intent.CATEGORY_OPENABLE)
                        .setType("*/*")
                        .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                        .putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/zip", "application/vnd.android.package-archive", "application/octet-stream"})
                , 1) // XAPK is octet-stream
        );

        // Check if user shared or opened file with the app.
        final Intent openIntent = getIntent();
        final String action = openIntent.getAction();
        if (Intent.ACTION_SEND.equals(action)) {
            splitAPKUri = openIntent.getParcelableExtra(Intent.EXTRA_STREAM);
        } else if (Intent.ACTION_VIEW.equals(action)) {
            splitAPKUri = openIntent.getData();
        } else if (Intent.ACTION_SEND_MULTIPLE.equals(action) && openIntent.hasExtra(Intent.EXTRA_STREAM)) {
            uris = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) ? openIntent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri.class) : openIntent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
            if (uris != null) {
                final Uri uri = uris.get(0);
                String path = uri.getPath();
                if(urisAreSplitApks = TextUtils.isEmpty(path) || !path.endsWith(".apk")) splitAPKUri = uri;
                else selectDirToSaveAPKOrSaveNow();
            }
        }
        if (splitAPKUri != null) {
            if(showDialog) showApkSelectionDialog();
            else selectDirToSaveAPKOrSaveNow();
        }
    }

    public void styleAlertDialog(AlertDialog ad) {
        Window w = ad.getWindow();
        if(w != null) {
            GradientDrawable border = new GradientDrawable();
            border.setColor(theme == com.google.android.material.R.style.Theme_Material3_Light_NoActionBar ? Color.WHITE : Color.BLACK); // Background color
            TypedValue typedValue = new TypedValue();
            getTheme().resolveAttribute(android.R.attr.colorPrimary, typedValue, true);
            border.setStroke(5, typedValue.data); // Border width and color
            border.setCornerRadius(24);
            w.setBackgroundDrawable(border);
            double m = 0.8;
            DisplayMetrics displayMetrics = rss.getDisplayMetrics();
            int height = (int) (displayMetrics.heightPixels * m);
            int width = (int) (displayMetrics.widthPixels * m);
            w.setLayout(width, height);
        }
        runOnUiThread(ad::show);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if(systemTheme) {
            int currentNightMode = newConfig.uiMode & Configuration.UI_MODE_NIGHT_MASK;
            if (currentNightMode == Configuration.UI_MODE_NIGHT_YES) {
                setTheme(theme = com.google.android.material.R.style.Theme_Material3_Dark_NoActionBar);
                recreate();
            } else if (currentNightMode == Configuration.UI_MODE_NIGHT_NO) {
                setTheme(theme = com.google.android.material.R.style.Theme_Material3_Light_NoActionBar);
                recreate();
            }
        }
    }

    public static Resources rss;

    private void updateLang(Resources res, ScrollView settingsDialog) {
        rss = res;
        this.<TextView>findViewById(R.id.decodeButton).setText(res.getString(R.string.merge));
        this.<TextView>findViewById(R.id.fromAppsButton).setText(res.getString(R.string.select_from_installed_apps));
        findViewById(R.id.settingsButton).setContentDescription(res.getString(R.string.settings));
        findViewById(R.id.installButton).setContentDescription(res.getString(R.string.install));
        findViewById(R.id.cancelButton).setContentDescription(res.getString(R.string.cancel));
        findViewById(R.id.copyButton).setContentDescription(res.getString(R.string.copy_log));

        if(settingsDialog != null) {
            ((TextView) settingsDialog.findViewById(R.id.langPicker)).setText(res.getString(R.string.lang));
            ((TextView) settingsDialog.findViewById(R.id.logToggle)).setText(res.getString(R.string.enable_logs));
            ((TextView) settingsDialog.findViewById(R.id.ask)).setText(res.getString(R.string.ask));
            ((TextView) settingsDialog.findViewById(R.id.showDialogToggle)).setText(res.getString(R.string.show_dialog));
            ((TextView) settingsDialog.findViewById(R.id.signToggle)).setText(res.getString(R.string.sign_apk));
            ((TextView) settingsDialog.findViewById(R.id.selectSplitsForDeviceToggle)).setText(res.getString(R.string.automatically_select));
            ((TextInputLayout) settingsDialog.findViewById(R.id.suffixLayout)).setHint(rss.getString(R.string.suffix));
        }
    }

    final File getAntisplitMFolder() {
        final File antisplitMFolder = new File(Environment.getExternalStorageDirectory(), "AntiSplit-M");
        return antisplitMFolder.exists() || antisplitMFolder.mkdir() ? antisplitMFolder : new File(Environment.getExternalStorageDirectory(), "Download");
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void checkStoragePerm() {
        if(doesNotHaveStoragePerm(this)) {
            Toast.makeText(this, rss.getString(R.string.grant_storage), Toast.LENGTH_LONG).show();
            if(LegacyUtils.supportsWriteExternalStorage) requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
            else startActivityForResult(new Intent(ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:" + getPackageName())), 0);
        }
    }

    public static boolean doesNotHaveStoragePerm(Context context) {
        return Build.VERSION.SDK_INT > 22 && (LegacyUtils.supportsWriteExternalStorage ?
            context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED :
            !Environment.isExternalStorageManager());
    }

    @Override
    protected void onPause() {
        getSharedPreferences("set", Context.MODE_PRIVATE).edit()
                .putBoolean("logEnabled", logEnabled)
                .putBoolean("ask", ask)
                .putBoolean("showDialog", showDialog)
                .putBoolean("signApk", signApk)
                .putBoolean("systemTheme", systemTheme)
                .putBoolean("selectSplitsForDevice", selectSplitsForDevice)
                .putInt("theme", theme)
                .putInt("sortMode", sortMode)
                .putString("lang", lang)
                .putString("suffix", suffix)
                .apply();
        super.onPause();
    }

    TextView logField;
    NestedScrollView scrollView;

    @Override
        public void onLog(CharSequence msg) {
        runOnUiThread(() -> {
            logField.append(new StringBuilder(msg).append('\n'));
            scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
        });
    }

    @Override
    public void onLog(int resID) {
        onLog(rss.getString(resID));
    }

    private Handler handler;

    /** @noinspection ResultOfMethodCallIgnored, DataFlowIssue */
    public static void deleteDir(File dir) {
        // There should never be folders in here.
        for (String child : dir.list()) new File(dir, child).delete();
    }

    /** @noinspection ResultOfMethodCallIgnored*/
    private void cleanupAppFolder() {
        if(doesNotHaveStoragePerm(this)) return;
        File appFolder = new File(Environment.getExternalStorageDirectory(), "AntiSplit-M");
        if(!appFolder.exists()) return;
        File[] children = appFolder.listFiles();
        if(children == null) return;
        if (children.length == 0) appFolder.delete();
        else {
            for (File child : children) if (child.isFile() && child.length() == 0) child.delete();
            if (appFolder.listFiles().length == 0) appFolder.delete();
        }
    }

    @Override
    protected void onDestroy() {
        deleteDir(getCacheDir());
        cleanupAppFolder();
        super.onDestroy();
    }

    private List<String> splitsToUse = null;

    private static class ProcessTask extends AsyncTask<Uri, Void, Void> {
        private final WeakReference<MainActivity> activityReference;
        private final DeviceSpecsUtil DeviceSpecsUtil;
        private final String packageNameFromAppList;
        // only retain a weak reference to the activity
        ProcessTask(MainActivity context, com.abdurazaaqmohammed.AntiSplit.main.DeviceSpecsUtil deviceSpecsUtil, String fromAppList) {
            activityReference = new WeakReference<>(context);
            DeviceSpecsUtil = deviceSpecsUtil;
            this.packageNameFromAppList = fromAppList;
            toggleAnimation(context, true);
        }

        @Override
        protected Void doInBackground(Uri... uris) {
            MainActivity activity = activityReference.get();
            if (activity == null) return null;

            final File cacheDir = activity.getCacheDir();
            if (cacheDir != null && activity.urisAreSplitApks) deleteDir(cacheDir);
            try {
                if(TextUtils.isEmpty(packageNameFromAppList)) {
                    List<String> splits = activity.splitsToUse;
                    if (activity.urisAreSplitApks) {
                        if (selectSplitsForDevice) {
                            splits = DeviceSpecsUtil.getListOfSplits(activity.splitAPKUri);
                            switch (splits.size()) {
                                case 4:
                                case 3:
                                case 2:
                                    splits.clear();
                                    break;
                                default:
                                    List<String> copy = List.copyOf(splits);
                                    List<String> toRemove = new ArrayList<>();
                                    boolean splitApkContainsArch = false;
                                    for (int i = 0; i < splits.size(); i++) {
                                        final String thisSplit = splits.get(i);
                                        if (!splitApkContainsArch && com.abdurazaaqmohammed.AntiSplit.main.DeviceSpecsUtil.isArch(thisSplit)) {
                                            splitApkContainsArch = true;
                                        }
                                        if (DeviceSpecsUtil.shouldIncludeSplit(thisSplit))
                                            toRemove.add(thisSplit);
                                    }
                                    if (splitApkContainsArch) {
                                        boolean selectedSplitsContainsArch = false;
                                        for (int i = 0; i < copy.size(); i++) {
                                            final String thisSplit = copy.get(i);
                                            if (com.abdurazaaqmohammed.AntiSplit.main.DeviceSpecsUtil.isArch(thisSplit) && toRemove.contains(thisSplit)) {
                                                selectedSplitsContainsArch = true;
                                                break;
                                            }
                                        }
                                        if (!selectedSplitsContainsArch) {
                                            LogUtil.logMessage("Could not find device architecture, selecting all architectures");
                                            for (int i = 0; i < splits.size(); i++) {
                                                final String thisSplit = splits.get(i);
                                                if (com.abdurazaaqmohammed.AntiSplit.main.DeviceSpecsUtil.isArch(thisSplit))
                                                    toRemove.add(thisSplit); // select all to be sure
                                            }
                                        }
                                    }

                                    boolean didNotFindDpi = true;
                                    for (int i = 0; i < copy.size(); i++) {
                                        String thisSplit = copy.get(i);
                                        if (thisSplit.contains("dpi") && toRemove.contains(thisSplit)) {
                                            didNotFindDpi = false;
                                            break;
                                        }
                                    }
                                    if (didNotFindDpi) {
                                        for (int i = 0; i < splits.size(); i++) {
                                            String thisSplit = splits.get(i);
                                            if (thisSplit.contains("hdpi")) toRemove.add(thisSplit);
                                        }
                                    }
                                    splits.removeAll(toRemove);
                            }
                        }
                    } else {
                        // These are the splits from inside the APK, just copy the splits to cache folder
                        for (Uri uri : activity.uris) {
                            try (InputStream is = FileUtils.getInputStream(uri, activity)) {
                                FileUtils.copyFile(is, new File(cacheDir, getOriginalFileName(activity, uri)));
                            }
                        }
                    }
                    Merger.run(
                        activity.urisAreSplitApks ? activity.splitAPKUri : null,
                        cacheDir,
                        uris[0],
                        activity,
                        splits,
                        signApk);
                } else try(ApkBundle bundle = new ApkBundle()) {
                    bundle.loadApkDirectory(new File(activity.getPackageManager().getPackageInfo(packageNameFromAppList, 0).applicationInfo.sourceDir).getParentFile(), false, activity);
                    Merger.run(bundle, cacheDir, uris[0], activity, signApk);
                }
            } catch (Exception e) {
                activity.showError(e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            MainActivity activity = activityReference.get();
            toggleAnimation(activity, false);
            activity.pkgName = null;
            if(activity.urisAreSplitApks) activity.getHandler().post(() -> {
                try {
                    activity.uris.remove(0);
                    activity.splitAPKUri = activity.uris.get(0);
                    if(showDialog) activity.showApkSelectionDialog();
                    else activity.selectDirToSaveAPKOrSaveNow();
                } catch (IndexOutOfBoundsException | NullPointerException ignored) {
                    // End of list, I don't know why but isEmpty is not working
                    activity.showSuccess();
                }
            });
            else activity.showSuccess();
        }
    }

    public static void toggleAnimation(MainActivity context, boolean on) {
        LinearProgressIndicator loadingImage = context.findViewById(R.id.progressIndicator);
        context.runOnUiThread(() -> {
            if(on) {
                loadingImage.setVisibility(View.VISIBLE);
                //loadingImage.startAnimation(AnimationUtils.loadAnimation(context, R.anim.loading));
            }
            else {
                loadingImage.setVisibility(View.GONE);
              //  loadingImage.clearAnimation();
            }
        });
    }

    private void processOneSplitApkUri(Uri uri) {
        splitAPKUri = uri;
        if (showDialog) showApkSelectionDialog();
        else selectDirToSaveAPKOrSaveNow();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) switch (requestCode) {
            case 0:
                checkStoragePerm();
                break;
            case 1:
                // opened through button in the app

                ClipData clipData = data.getClipData();
                if (clipData == null) processOneSplitApkUri(data.getData());
                else {
                    //multiple files selected
                    Uri first = clipData.getItemAt(0).getUri();
                    String path = first.getPath();
                    if (path != null && path.endsWith(".apk")) {
                        urisAreSplitApks = false;
                        uris = new ArrayList<>();
                        uris.add(first);
                    } else processOneSplitApkUri(first);

                    for (int i = 1; i < clipData.getItemCount(); i++) {
                        Uri uri = clipData.getItemAt(i).getUri();
                        if (urisAreSplitApks) processOneSplitApkUri(uri);
                        else uris.add(uri);
                    }
                    if (!urisAreSplitApks) selectDirToSaveAPKOrSaveNow();
                }
                break;
            case 2:
                // going to process and save a file now
                logField.setText("");
                process(data.getData());
                break;
        }
    }
    private final ActivityResultLauncher<IntentSenderRequest> launcher = registerForActivityResult(
            new ActivityResultContracts.StartIntentSenderForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) LogUtil.logMessage("Deleted ");
            });

    private void process(Uri outputUri) {
        findViewById(R.id.installButton).setVisibility(View.GONE);
        ProcessTask processTask = new ProcessTask(this, DeviceSpecsUtil, pkgName);
        processTask.execute(outputUri);
        LinearLayout fabs = findViewById(R.id.fabs);
        fabs.setAlpha(0.5f);
        View cancelButton = findViewById(R.id.cancelButton);
        cancelButton.setVisibility(View.VISIBLE);
        cancelButton.setOnClickListener(v -> {
            try {
                if(doesNotHaveStoragePerm(this)) AndroidXI.getInstance().with(this).delete(launcher, outputUri);
                else if(new File(FileUtils.getPath(outputUri, this)).delete()) LogUtil.logMessage("Cleaned output file " + getOriginalFileName(this, outputUri));
            } catch (Exception ignored) {}
            Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
            if (intent == null) {
                processTask.cancel(true);
                intent = getIntent();
                finish();
                startActivity(intent);
            } else {
                startActivity(Intent.makeRestartActivityTask(intent.getComponent()));
                Runtime.getRuntime().exit(0);
            }
        });

        View copyButton = findViewById(R.id.copyButton);
        copyButton.setVisibility(View.VISIBLE);
        copyButton.setOnClickListener(v -> copyText(new StringBuilder().append(logField.getText()).append('\n').append(((TextView) findViewById(R.id.errorField)).getText())));
    }

    public void showApkSelectionDialog() {
        try {
            List<String> splits = DeviceSpecsUtil.getListOfSplits(splitAPKUri);
            Collections.sort(splits, Comparator.comparing(splitName -> splitName.toLowerCase(Locale.ROOT)));
            final int initialSize = splits.size();
            String[] apkNames = new String[initialSize + 5];
            boolean[] checkedItems = new boolean[initialSize + 5];

            apkNames[0] = rss.getString(R.string.all);
            apkNames[1] = rss.getString(R.string.for_device);
            apkNames[2] = rss.getString(R.string.arch_for_device);
            apkNames[3] = rss.getString(R.string.dpi_for_device);
            apkNames[4] = rss.getString(R.string.lang_for_device);
            for (int i = 5; i < initialSize + 5; i++) {
                apkNames[i] = splits.get(i - 5);
                checkedItems[i] = false;
            }

            AlertDialog ad = new MaterialAlertDialogBuilder(this).setTitle(rss.getString(R.string.select_splits)).setMultiChoiceItems(apkNames, checkedItems, (dialog, which, isChecked) -> {
                switch (which) {
                    case 0:
                        // "Select All" option
                        for (int i = 5; i < checkedItems.length; i++)
                            ((AlertDialog) dialog).getListView().setItemChecked(i, checkedItems[i] = isChecked);
                        break;
                    case 1:
                        // device specs option
                        for (int i = 5; i < checkedItems.length; i++) {
                            ((AlertDialog) dialog).getListView().setItemChecked(i, checkedItems[i] = (isChecked && DeviceSpecsUtil.shouldIncludeSplit(apkNames[i])));
                        }
                        boolean didNotFindAppropriateDpi = true;
                        for (int i = 5; i < checkedItems.length; i++) {
                            if (checkedItems[i] && apkNames[i].contains("dpi")) {
                                didNotFindAppropriateDpi = false;
                                break;
                            }
                        }
                        if (didNotFindAppropriateDpi) {
                            for (int i = 5; i < checkedItems.length; i++) {
                                if (apkNames[i].contains("hdpi")) {
                                    ((AlertDialog) dialog).getListView().setItemChecked(i, checkedItems[i] = isChecked);
                                    break;
                                }
                            }
                        }
                        break;
                    case 2:
                        //arch for device
                        for (int i = 5; i < checkedItems.length; i++) {
                            if (DeviceSpecsUtil.shouldIncludeArch(apkNames[i]))
                                ((AlertDialog) dialog).getListView().setItemChecked(i, checkedItems[i] = isChecked);
                        }
                        break;
                    case 3:
                        //dpi for device
                        for (int i = 5; i < checkedItems.length; i++) {
                            if (DeviceSpecsUtil.shouldIncludeDpi(apkNames[i]))
                                ((AlertDialog) dialog).getListView().setItemChecked(i, checkedItems[i] = isChecked);
                        }
                        boolean didNotFoundAppropriateDpi = true;
                        for (int i = 5; i < checkedItems.length; i++) {
                            if (checkedItems[i] && apkNames[i].contains("dpi")) {
                                didNotFoundAppropriateDpi = false;
                                break;
                            }
                        }
                        if (didNotFoundAppropriateDpi) {
                            for (int i = 5; i < checkedItems.length; i++) {
                                if (apkNames[i].contains("hdpi")) {
                                    ((AlertDialog) dialog).getListView().setItemChecked(i, checkedItems[i] = isChecked);
                                    break;
                                }
                            }
                        }
                        break;
                    case 4:
                        //lang for device
                        for (int i = 5; i < checkedItems.length; i++) {
                            if (DeviceSpecsUtil.shouldIncludeLang(apkNames[i]))
                                ((AlertDialog) dialog).getListView().setItemChecked(i, checkedItems[i] = isChecked);
                        }
                        break;
                    default:
                        ListView listView = ((AlertDialog) dialog).getListView();
                        if (!isChecked)
                            listView.setItemChecked(0, checkedItems[0] = false); // Uncheck "Select All" if any individual item is unchecked
                        for (int i = 1; i <= 4; i++) {
                            if (checkedItems[i] && !DeviceSpecsUtil.shouldIncludeSplit(apkNames[which])) {
                                listView.setItemChecked(i, checkedItems[i] = false); // uncheck device arch if non device arch selected
                            }
                        }
                        break;
                }
            }).setPositiveButton("OK", (dialog, which) -> {
                for (int i = 1; i < checkedItems.length; i++) {
                    if (checkedItems[i]) splits.remove(apkNames[i]); // ?????
                }

                if (splits.size() == initialSize) {
                    urisAreSplitApks = true; // reset
                    showError(rss.getString(R.string.nothing));
                } else {
                    splitsToUse = splits;
                    selectDirToSaveAPKOrSaveNow();
                }
            }).setNegativeButton("Cancel", null).create();
            styleAlertDialog(ad);
            for (int i = 5; i < apkNames.length; i++) {
                if (com.abdurazaaqmohammed.AntiSplit.main.DeviceSpecsUtil.isBaseApk(apkNames[i])) {
                    ad.getListView().setItemChecked(i, true);
                    break;
                }
            }
        } catch (IOException e) {
            showError(e);
        }
    }

    private void showSuccess() {
        findViewById(R.id.cancelButton).setVisibility(View.GONE);
        View installButton = findViewById(R.id.installButton);
        if(errorOccurred) installButton.setVisibility(View.GONE);
        else {
            final String success = rss.getString(R.string.success_saved);
            LogUtil.logMessage(success);
            runOnUiThread(() -> Toast.makeText(this, success, Toast.LENGTH_SHORT).show());
            if(signApk && Merger.signedApk != null) {
                installButton.setVisibility(View.VISIBLE);
                installButton.setOnClickListener(v ->          //if (supportsFileChannel && !getPackageManager().canRequestPackageInstalls()) startActivityForResult(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).setData(Uri.parse(String.format("package:%s", getPackageName()))), 1234);
                        startActivity(new Intent(Intent.ACTION_INSTALL_PACKAGE)
                                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                .setData(Merger.signedApk)));
            } else installButton.setVisibility(View.GONE);
        }
    }

    private void copyText(CharSequence text) {
        ((ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("log", text));
        Toast.makeText(this, rss.getString(R.string.copied_log), Toast.LENGTH_SHORT).show();
    }

    private void showError(Exception e) {
        cleanupAppFolder();
        if (!(e instanceof ClosedByInterruptException)) {
            final String mainErr = e.toString();
            errorOccurred = !mainErr.equals(rss.getString(R.string.sign_failed));
            toggleAnimation(this, false);

            StringBuilder stackTrace = new StringBuilder(mainErr);

            for (StackTraceElement line : e.getStackTrace()) stackTrace.append(line).append('\n');
            StringBuilder fullLog = new StringBuilder(stackTrace).append('\n')
                    .append("SDK ").append(Build.VERSION.SDK_INT).append('\n')
                    .append(rss.getString(R.string.app_name)).append(' ');
            String currentVer;
            try {
                currentVer = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            } catch (Exception ex) {
                currentVer = "2.1.1";
            }
            fullLog.append(currentVer).append('\n').append("Storage permission granted: ").append(!doesNotHaveStoragePerm(this))
                    .append('\n').append(logField.getText());

            getHandler().post(() -> runOnUiThread(() -> {
                View dialogView = getLayoutInflater().inflate(R.layout.dialog_button_layout, null);

                ((TextView) dialogView.findViewById(R.id.errorD)).setText(stackTrace);

                styleAlertDialog(new MaterialAlertDialogBuilder(this)
                        .setTitle(mainErr)
                        .setView(dialogView)
                        .setPositiveButton(rss.getString(R.string.copy_log), (dialog, which) -> {
                            copyText(fullLog);
                            dialog.dismiss();
                        })
                        .setNegativeButton(rss.getString(R.string.create_issue), (dialog, which) -> {
                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/AbdurazaaqMohammed/AntiSplit-M/issues/new?title=Crash%20Report&body=" + fullLog)));
                            dialog.dismiss();
                        })
                        .setNeutralButton(rss.getString(R.string.cancel), (dialog, which) -> dialog.dismiss())
                        .create());
                ScrollView scrollView = dialogView.findViewById(R.id.errorView);

                ViewGroup.LayoutParams params = scrollView.getLayoutParams();
                params.height = (int) (rss.getDisplayMetrics().heightPixels * 0.5);
                scrollView.setLayoutParams(params);
            }));
        }
    }

    private void showError(String err) {
        toggleAnimation(this, false);
        runOnUiThread(() -> {
            TextView errorBox = findViewById(R.id.errorField);
            errorBox.setVisibility(View.VISIBLE);
            errorBox.setText(err);
            Toast.makeText(this, err, Toast.LENGTH_SHORT).show();
        });
    }

    public static String getOriginalFileName(Context context, Uri uri) {
        String result = null;
        try {
            if (Objects.equals(uri.getScheme(), "content")) {
                try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                    if (cursor != null && cursor.moveToFirst()) {
                        result = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME));
                    }
                }
            }
            if (result == null) {
                result = uri.getPath();
                int cut = Objects.requireNonNull(result).lastIndexOf('/'); // Ensure it throw the NullPointerException here to be caught
                if (cut != -1) result = result.substring(cut + 1);
            }
            LogUtil.logMessage(result);
            return result.replaceFirst("\\.(?:xapk|aspk|apk[sm])", suffix + ".apk");
        } catch (Exception ignored) {
            return "filename_not_found";
        }
    }

    @SuppressLint("InlinedApi")
    private void selectDirToSaveAPKOrSaveNow() {
        if (ask) {
            String filename;
            if(TextUtils.isEmpty(pkgName)) filename = (urisAreSplitApks ? getOriginalFileName(this, splitAPKUri) : getNameFromNonSplitApks());
            else {
                String versionName;
                try {
                    versionName = " v" + getPackageManager().getPackageInfo(pkgName, 0).versionName;
                } catch (Exception ignored) {
                    versionName = "";
                }
                filename = pkgName + versionName + suffix;
            }

            startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT)
                    .addCategory(Intent.CATEGORY_OPENABLE)
                    .setType("application/vnd.android.package-archive")
                    .putExtra(Intent.EXTRA_TITLE, filename), 2);
        } else {
            checkStoragePerm();
            try {
                if(splitAPKUri == null) process(Uri.fromFile(new File(getAntisplitMFolder(), "output.apk")));
                else {
                    String originalFilePath;
                    if(urisAreSplitApks) originalFilePath = FileUtils.getPath(splitAPKUri, this);
                    else {
                        String path = FileUtils.getPath(uris.get(0), this);
                        originalFilePath = (TextUtils.isEmpty(path) ? getAntisplitMFolder() : path.substring(0, path.lastIndexOf(File.separator))) + File.separator + getNameFromNonSplitApks();
                    }
                    File f;
                    String newFilePath = TextUtils.isEmpty(originalFilePath) ?
                            getAntisplitMFolder() + File.separator + getOriginalFileName(this, splitAPKUri) // If originalFilePath is null urisAreSplitApks must be true because getNameFromNonSplitApks will always return something
                            : originalFilePath.replaceFirst("\\.(?:xapk|aspk|apk[sm])", suffix + ".apk");
                    if(TextUtils.isEmpty(newFilePath) ||
                            newFilePath.startsWith("/data/")
                        // || !(f = new File(newFilePath)).createNewFile() || f.canWrite()
                    ) {
                        f = new File(getAntisplitMFolder(), newFilePath.substring(newFilePath.lastIndexOf(File.separator) + 1));
                        showError(rss.getString(R.string.no_filepath) + newFilePath);
                    } else f = new File(newFilePath);
                    logField.setText("");
                    while(f.exists() && f.length() > 99) f = new File(f.getPath().replace(".apk", "_1.apk"));
                    LogUtil.logMessage(rss.getString(R.string.output) + f);
                    process(Uri.fromFile(f));
                }

            } catch (IOException e) {
                showError(e);
            }
        }
    }

    private String getNameFromNonSplitApks() {
        String realName = null;
        for(Uri uri : uris) {
            final String fileName = getOriginalFileName(this, uri);
            if (fileName.equals("base.apk")) {
                try {
                    realName = Objects.requireNonNull(getPackageManager().getPackageArchiveInfo(Objects.requireNonNull(FileUtils.getPath(uri, this)), 0)).packageName;
                    break;
                } catch (NullPointerException | IOException ignored) {}
            } else if (!fileName.startsWith("config") && !fileName.startsWith("split")) {
                // this should hopefully be base.apk renamed to the package name
                realName = fileName;
                break;
            }
        }
        return (TextUtils.isEmpty(realName) ? "unknown" : realName.replace(".apk", "")) + suffix + ".apk";
    }
}