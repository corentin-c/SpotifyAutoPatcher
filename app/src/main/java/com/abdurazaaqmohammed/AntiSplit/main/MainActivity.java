package com.abdurazaaqmohammed.AntiSplit.main;

import static android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION;
import static com.reandroid.apkeditor.merge.LogUtil.logEnabled;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import android.support.v4.content.FileProvider;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.widget.NestedScrollView;

import com.abdurazaaqmohammed.AntiSplit.R;
import com.corentinc.patcher.CPatcher;
import com.fom.storage.media.AndroidXI;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.reandroid.apk.ApkBundle;
import com.reandroid.apkeditor.merge.LogUtil;
import com.reandroid.apkeditor.merge.Merger;
import com.starry.FileUtils;

import java.io.File;
import java.lang.ref.WeakReference;
import java.nio.channels.ClosedByInterruptException;
import java.util.Objects;

public class MainActivity extends AppCompatActivity implements Merger.LogListener {
    public static boolean errorOccurred;
    public static String lang;
    public static int theme;
    public DeviceSpecsUtil DeviceSpecsUtil;
    private String pkgName;

    public Handler getHandler() {
        return handler;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handler = new Handler(Looper.getMainLooper());

        deleteDir(getCacheDir());
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        DeviceSpecsUtil = new DeviceSpecsUtil(this);
        setContentView(R.layout.activity_main);
        scrollView = findViewById(R.id.scrollView);
        logField = findViewById(R.id.logField);
        LogUtil.setLogListener(this);
        logEnabled = true;
        pkgName = "com.spotify.music";
        process(Uri.fromFile(new File(getApplicationContext().getCacheDir().getPath() + "spotify.apk")));
    }

    public void styleAlertDialog(AlertDialog ad) {
        Window w = ad.getWindow();
        if (w != null) {
            GradientDrawable border = new GradientDrawable();
            border.setColor(theme == com.google.android.material.R.style.Theme_Material3_Light_NoActionBar ? Color.WHITE : Color.BLACK); // Background color
            TypedValue typedValue = new TypedValue();
            getTheme().resolveAttribute(android.R.attr.colorPrimary, typedValue, true);
            border.setStroke(5, typedValue.data); // Border width and color
            border.setCornerRadius(24);
            w.setBackgroundDrawable(border);
            double m = 0.8;
            DisplayMetrics displayMetrics = this.getResources().getDisplayMetrics();
            int height = (int) (displayMetrics.heightPixels * m);
            int width = (int) (displayMetrics.widthPixels * m);
            w.setLayout(width, height);
        }
        runOnUiThread(ad::show);
    }

    private void checkStoragePerm() {
        if (doesNotHaveStoragePerm(this)) {
            Toast.makeText(this, this.getString(R.string.grant_storage), Toast.LENGTH_LONG).show();
            if (LegacyUtils.supportsWriteExternalStorage)
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
            else
                startActivityForResult(new Intent(ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:" + getPackageName())), 0);
        }
    }

    public static boolean doesNotHaveStoragePerm(Context context) {
        return (LegacyUtils.supportsWriteExternalStorage ?
                context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED :
                !Environment.isExternalStorageManager());
    }


    TextView logField;
    NestedScrollView scrollView;

    @Override
    public void onLog(CharSequence msg) {
        onLog(msg.toString());
    }

    @Override
    public void onLog(String log) {
        Log.i("", log);
        runOnUiThread(() -> {
            logField.append(new StringBuilder(log).append('\n'));
            scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
        });
    }

    @Override
    public void onLog(int resID) {
        onLog(this.getString(resID));
    }

    private Handler handler;

    /**
     * @noinspection ResultOfMethodCallIgnored, DataFlowIssue
     */
    public static void deleteDir(File dir) {
        // There should never be folders in here.
        for (String child : dir.list()) new File(dir, child).delete();
    }

    @Override
    protected void onDestroy() {
        deleteDir(getCacheDir());
        super.onDestroy();
    }

    private static class ProcessTask extends AsyncTask<Uri, Void, Void> {
        private final WeakReference<MainActivity> activityReference;
        private final String packageNameFromAppList;

        // only retain a weak reference to the activity
        ProcessTask(MainActivity context, String fromAppList) {
            activityReference = new WeakReference<>(context);
            this.packageNameFromAppList = fromAppList;
        }

        @Override
        protected Void doInBackground(Uri... uris) {
            MainActivity activity = activityReference.get();
            if (activity == null) return null;

            final File cacheDir = activity.getCacheDir();
            deleteDir(cacheDir);
            try {
                ApkBundle bundle = new ApkBundle();
                bundle.loadApkDirectory(new File(activity.getPackageManager().getPackageInfo(packageNameFromAppList, 0).applicationInfo.sourceDir).getParentFile(), false, activity);
                Merger.run(bundle, cacheDir, uris[0], activity, true);
            } catch (Exception e) {
                activity.showError(e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            MainActivity activity = activityReference.get();
            activity.pkgName = null;
            activity.showSuccess();
        }
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
            String suffix = "_antisplit";
            return result.replaceFirst("\\.(?:xapk|aspk|apk[sm])", suffix + ".apk");
        } catch (Exception ignored) {
            return "filename_not_found";
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null && requestCode == 0) {
            checkStoragePerm();
        }
    }

    private final ActivityResultLauncher<IntentSenderRequest> launcher = registerForActivityResult(
            new ActivityResultContracts.StartIntentSenderForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) LogUtil.logMessage("Deleted ");
            });

    private void process(Uri outputUri) {
        findViewById(R.id.installButton).setVisibility(View.GONE);
        ProcessTask processTask = new ProcessTask(this, pkgName);
        processTask.execute(outputUri);
        LinearLayout fabs = findViewById(R.id.fabs);
        fabs.setAlpha(0.5f);
        View cancelButton = findViewById(R.id.cancelButton);
        cancelButton.setVisibility(View.VISIBLE);
        cancelButton.setOnClickListener(v -> {
            try {
                if (doesNotHaveStoragePerm(this))
                    AndroidXI.getInstance().with(this).delete(launcher, outputUri);
                else if (new File(FileUtils.getPath(outputUri, this)).delete())
                    LogUtil.logMessage("Cleaned output file " + getOriginalFileName(this, outputUri));
            } catch (Exception ignored) {
            }
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

    private void showSuccess() {
        //patch here
        findViewById(R.id.cancelButton).setVisibility(View.GONE);

        View installButton = findViewById(R.id.installButton);
        if (errorOccurred) installButton.setVisibility(View.GONE);
        else {
            final String success = this.getString(R.string.success_saved);
            LogUtil.logMessage(success);
            runOnUiThread(() -> Toast.makeText(this, success, Toast.LENGTH_SHORT).show());
            if (Merger.signedApk != null) {
                File patchedApk = CPatcher.INSTANCE.patch(getApplicationContext(), Merger.signedApk, this);
                installButton.setVisibility(View.VISIBLE);
                installButton.setOnClickListener(v ->
                        startActivity(new Intent(Intent.ACTION_INSTALL_PACKAGE)
                                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                .setData(FileProvider.getUriForFile(getApplicationContext(), "com.abdurazaaqmohammed.AntiSplit.provider", patchedApk))));
            } else installButton.setVisibility(View.GONE);
        }
    }

    private void copyText(CharSequence text) {
        ((ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("log", text));
        Toast.makeText(this, this.getString(R.string.copied_log), Toast.LENGTH_SHORT).show();
    }

    private void showError(Exception e) {
        if (!(e instanceof ClosedByInterruptException)) {
            final String mainErr = e.toString();
            errorOccurred = !mainErr.equals(this.getString(R.string.sign_failed));

            StringBuilder stackTrace = new StringBuilder(mainErr);

            for (StackTraceElement line : e.getStackTrace()) stackTrace.append(line).append('\n');
            StringBuilder fullLog = new StringBuilder(stackTrace).append('\n')
                    .append("SDK ").append(Build.VERSION.SDK_INT).append('\n')
                    .append(this.getString(R.string.app_name)).append(' ');
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
                        .setPositiveButton(this.getString(R.string.copy_log), (dialog, which) -> {
                            copyText(fullLog);
                            dialog.dismiss();
                        })
                        .setNegativeButton(this.getString(R.string.create_issue), (dialog, which) -> {
                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/AbdurazaaqMohammed/AntiSplit-M/issues/new?title=Crash%20Report&body=" + fullLog)));
                            dialog.dismiss();
                        })
                        .setNeutralButton(this.getString(R.string.cancel), (dialog, which) -> dialog.dismiss())
                        .create());
                ScrollView scrollView = dialogView.findViewById(R.id.errorView);

                ViewGroup.LayoutParams params = scrollView.getLayoutParams();
                params.height = (int) (this.getResources().getDisplayMetrics().heightPixels * 0.5);
                scrollView.setLayoutParams(params);
            }));
        }
    }
}