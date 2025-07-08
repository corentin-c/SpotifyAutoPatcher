/*
 *  Copyright (C) 2022 github.com/REAndroid
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.reandroid.apkeditor.merge;

import static com.reandroid.apkeditor.merge.LogUtil.logMessage;

import android.content.Context;
import android.net.Uri;

import com.github.corentinc.SpotifyAutoPatcher.R;
import com.reandroid.apk.ApkBundle;
import com.reandroid.apk.ApkModule;
import com.reandroid.apkeditor.common.AndroidManifestHelper;
import com.reandroid.app.AndroidManifest;
import com.reandroid.archive.ZipEntryMap;
import com.reandroid.arsc.chunk.TableBlock;
import com.reandroid.arsc.chunk.xml.AndroidManifestBlock;
import com.reandroid.arsc.chunk.xml.ResXmlAttribute;
import com.reandroid.arsc.chunk.xml.ResXmlElement;
import com.reandroid.arsc.container.SpecTypePair;
import com.reandroid.arsc.model.ResourceEntry;
import com.reandroid.arsc.value.Entry;
import com.reandroid.arsc.value.ResValue;
import com.reandroid.arsc.value.ValueType;
import com.starry.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class Merger {

    public interface LogListener {
        void onLog(CharSequence log);

        void onLog(String log);

        void onLog(int resID);
    }

    public static void run(ApkBundle bundle, File dir, Uri out, Context context) throws IOException, InterruptedException {
        logMessage("Found modules: " + bundle.getApkModuleList().size());
        for (File split : dir.listFiles()) {
            String splitName = split.getName();
            String arch = null;
            String var = "x86";
            if (splitName.contains(var)) arch = var;
            else if (splitName.contains(var = "x86_64") || splitName.contains("x86-64") || splitName.contains("x64"))
                arch = var;
            else if (splitName.contains("arm64")) arch = "arm64-v8a";
            else if (splitName.contains("v7a") || splitName.contains("arm7")) arch = "armeabi-v7a";
            if (arch != null) try (ApkModule zf = ApkModule.loadApkFile(split, splitName)) {
                if (zf.containsFile("lib" + File.separator + arch + File.separator + "libpairipcore.so")) {
                    LogUtil.logMessage(context.getString(R.string.pairip_warning));
                    break;
                }
            }
        }
        try (ApkModule mergedModule = bundle.mergeModules()) {
            if (mergedModule.hasAndroidManifest()) {
                AndroidManifestBlock manifest = mergedModule.getAndroidManifest();
                logMessage(context.getString(R.string.sanitizing_manifest));
                int ID_requiredSplitTypes = 0x0101064e;
                int ID_splitTypes = 0x0101064f;

                AndroidManifestHelper.removeAttributeFromManifestById(manifest,
                        ID_requiredSplitTypes);
                AndroidManifestHelper.removeAttributeFromManifestById(manifest,
                        ID_splitTypes);
                AndroidManifestHelper.removeAttributeFromManifestByName(manifest,
                        AndroidManifest.NAME_splitTypes);

                AndroidManifestHelper.removeAttributeFromManifestByName(manifest,
                        AndroidManifest.NAME_requiredSplitTypes);
                AndroidManifestHelper.removeAttributeFromManifestByName(manifest,
                        AndroidManifest.NAME_splitTypes);
                AndroidManifestHelper.removeAttributeFromManifestAndApplication(manifest,
                        AndroidManifest.ID_extractNativeLibs
                );
                AndroidManifestHelper.removeAttributeFromManifestAndApplication(manifest,
                        AndroidManifest.ID_isSplitRequired
                );

                ResXmlElement application = manifest.getApplicationElement();
                List<ResXmlElement> splitMetaDataElements =
                        AndroidManifestHelper.listSplitRequired(application);
                boolean splits_removed = false;
                for (ResXmlElement meta : splitMetaDataElements) {
                    if (!splits_removed) {
                        boolean result = false;
                        ResXmlAttribute nameAttribute = meta.searchAttributeByResourceId(AndroidManifest.ID_name);
                        if (nameAttribute != null) {
                            if ("com.android.vending.splits".equals(nameAttribute.getValueAsString())) {
                                ResXmlAttribute valueAttribute = meta.searchAttributeByResourceId(
                                        AndroidManifest.ID_value);
                                if (valueAttribute == null) {
                                    valueAttribute = meta.searchAttributeByResourceId(
                                            AndroidManifest.ID_resource);
                                }
                                if (valueAttribute != null
                                        && valueAttribute.getValueType() == ValueType.REFERENCE) {
                                    if (mergedModule.hasTableBlock()) {
                                        TableBlock tableBlock = mergedModule.getTableBlock();
                                        ResourceEntry resourceEntry = tableBlock.getResource(valueAttribute.getData());
                                        if (resourceEntry != null) {
                                            ZipEntryMap zipEntryMap = mergedModule.getZipEntryMap();
                                            for (Entry entry : resourceEntry) {
                                                if (entry == null) {
                                                    continue;
                                                }
                                                ResValue resValue = entry.getResValue();
                                                if (resValue == null) {
                                                    continue;
                                                }
                                                String path = resValue.getValueAsString();
                                                logMessage(context.getString(R.string.removed_table_entry) + " " + path);
                                                //Remove file entry
                                                zipEntryMap.remove(path);
                                                // It's not safe to destroy entry, resource id might be used in dex code.
                                                // Better replace it with boolean value.
                                                entry.setNull(true);
                                                SpecTypePair specTypePair = entry.getTypeBlock()
                                                        .getParentSpecTypePair();
                                                specTypePair.removeNullEntries(entry.getId());
                                            }
                                            result = true;
                                        }
                                    }
                                }
                            }
                        }
                        splits_removed = result;
                    }
                    logMessage("Removed-element : <" + meta.getName() + "> name=\""
                            + AndroidManifestHelper.getNamedValue(meta) + "\"");
                    application.remove(meta);
                }
                manifest.refresh();
            }
            logMessage(context.getString(R.string.saving));
            mergedModule.writeApk(FileUtils.getOutputStream(out, context));
        }
    }

}