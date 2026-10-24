/*
 * Copyright (C) 2026 The Lawnchair Open Source Project
 * Licensed under the Apache License, Version 2.0
 */
package com.android.launcher3.model;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.content.pm.ShortcutInfo;
import android.os.UserHandle;
import android.os.UserManager;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.android.launcher3.LauncherModel.ModelUpdateTask;
import com.android.launcher3.pm.UserCache;
import com.android.launcher3.shortcuts.ShortcutRequest;
import com.android.launcher3.logging.FileLog;
import app.lawnchair.util.PrivateSpaceUtils;

import java.util.List;
import java.util.Map;

/** Opens Lawnchair's normal add-item confirmation for the private project-link aliases. */
public class PrivateShareShortcutTask implements ModelUpdateTask {

    public static final String PUBLISHER_PACKAGE = "com.example.privatespaceprojects";
    public static final String ID_PREFIX = "shared-project-";
    public static final String EXTRA_VERSION = "app.lawnchair.private_share.version";
    public static final String EXTRA_PENDING = "app.lawnchair.private_share.pending";
    public static final String EXTRA_TITLE_REQUIRED = "app.lawnchair.private_share.title_required";
    public static final int VERSION = 1;
    public static final String PREFS = "private_share_shortcuts";
    public static final String EXTRA_MODE = "app.lawnchair.private_share.add_item";
    public static final String EXTRA_ID = "app.lawnchair.private_share.id";
    public static final String EXTRA_USER_SERIAL = "app.lawnchair.private_share.user_serial";
    private static final Object HANDLED_LOCK = new Object();

    private final List<ShortcutInfo> mChangedShortcuts;
    private final UserHandle mUser;

    public PrivateShareShortcutTask(List<ShortcutInfo> shortcuts, UserHandle user) {
        mChangedShortcuts = shortcuts;
        mUser = user;
    }

    @Override
    public void execute(@NonNull ModelTaskController taskController,
            @NonNull BgDataModel dataModel, @NonNull AllAppsList apps) {
        Context context = taskController.getApp().getContext();
        if (!isAllowed(context, mUser)) {
            return;
        }

        LauncherApps launcherApps = context.getSystemService(LauncherApps.class);
        SharedPreferences handled = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        for (ShortcutInfo changed : mChangedShortcuts) {
            String id = changed.getId();
            // LauncherApps callback objects may contain only key fields, without extras. Use the
            // opaque ID as a cheap filter, then verify the full published record below.
            if (!isPotentialCallback(changed) || handled.contains(id)) {
                continue;
            }

            ShortcutInfo current = findPendingShortcut(context, mUser, id);
            if (current == null) {
                continue;
            }
            LauncherActivityInfo alias = findAlias(launcherApps, mUser, current);
            if (alias == null) {
                continue;
            }

            Intent intent = new Intent(context,
                    com.android.launcher3.dragndrop.AddItemActivity.class)
                    .putExtra(EXTRA_MODE, true)
                    .putExtra(EXTRA_ID, id)
                    .putExtra(EXTRA_USER_SERIAL,
                            UserCache.INSTANCE.get(context).getSerialNumberForUser(mUser))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            synchronized (HANDLED_LOCK) {
                if (handled.contains(id) || !handled.edit()
                        .putLong(id, current.getLastChangedTimestamp()).commit()) {
                    continue;
                }
            }
            try {
                context.startActivity(intent);
                trimHandledIds(handled);
            } catch (RuntimeException e) {
                synchronized (HANDLED_LOCK) {
                    handled.edit().remove(id).commit();
                }
                // Do not include ShortcutInfo or its intent in diagnostics.
                FileLog.e("PrivateShareShortcutTask", "Could not open add-item confirmation", e);
            }
            return;
        }
    }

    private static void trimHandledIds(SharedPreferences preferences) {
        Map<String, ?> entries = preferences.getAll();
        while (entries.size() > 64) {
            String oldestId = null;
            long oldestTimestamp = Long.MAX_VALUE;
            for (Map.Entry<String, ?> entry : entries.entrySet()) {
                if (entry.getValue() instanceof Long
                        && (Long) entry.getValue() < oldestTimestamp) {
                    oldestTimestamp = (Long) entry.getValue();
                    oldestId = entry.getKey();
                }
            }
            if (oldestId == null) {
                return;
            }
            preferences.edit().remove(oldestId).apply();
            entries = preferences.getAll();
        }
    }

    public static boolean isAllowed(Context context, UserHandle user) {
        if (!PrivateSpaceUtils.isPinningAllowed(context)
                || !UserCache.INSTANCE.get(context).getUserInfo(user).isPrivate()) {
            return false;
        }
        UserManager userManager = context.getSystemService(UserManager.class);
        return userManager != null && !userManager.isQuietModeEnabled(user);
    }

    public static boolean isCandidate(ShortcutInfo shortcut) {
        if (shortcut == null || !PUBLISHER_PACKAGE.equals(shortcut.getPackage())
                || !shortcut.isDynamic() || shortcut.isPinned()
                || !shortcut.getId().startsWith(ID_PREFIX)) {
            return false;
        }
        android.os.PersistableBundle extras = shortcut.getExtras();
        return extras != null && extras.getInt(EXTRA_VERSION, -1) == VERSION
                && extras.getBoolean(EXTRA_PENDING, false);
    }

    private static boolean isPotentialCallback(ShortcutInfo shortcut) {
        return shortcut != null && PUBLISHER_PACKAGE.equals(shortcut.getPackage())
                && shortcut.getId() != null && shortcut.getId().startsWith(ID_PREFIX);
    }

    public static ShortcutInfo findPendingShortcut(Context context, UserHandle user, String id) {
        if (TextUtils.isEmpty(id) || !id.startsWith(ID_PREFIX)) {
            return null;
        }
        List<ShortcutInfo> found = new ShortcutRequest(context, user)
                .forPackage(PUBLISHER_PACKAGE, id)
                .query(ShortcutRequest.PUBLISHED);
        for (ShortcutInfo shortcut : found) {
            if (id.equals(shortcut.getId()) && isCandidate(shortcut)) {
                return shortcut;
            }
        }
        return null;
    }

    public static LauncherActivityInfo findAlias(LauncherApps launcherApps, UserHandle user,
            ShortcutInfo shortcut) {
        if (!PUBLISHER_PACKAGE.equals(shortcut.getPackage()) || shortcut.getActivity() == null) {
            return null;
        }
        for (LauncherActivityInfo activity : launcherApps.getActivityList(PUBLISHER_PACKAGE, user)) {
            if (activity.getComponentName().equals(shortcut.getActivity())
                    && activity.getApplicationInfo().enabled) {
                return activity;
            }
        }
        return null;
    }
}
