/*
 * Copyright (C) 2026 The Lawnchair Open Source Project
 * Licensed under the Apache License, Version 2.0
 */
package com.android.launcher3.util;

import static com.google.common.truth.Truth.assertThat;

import android.content.ComponentName;
import android.os.UserHandle;

import androidx.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ComponentKeyTest {

    @Test
    public void persistedUserIdRoundTripsForOwnerAndPrivateProfile() {
        ComponentName component = new ComponentName("example.package", "example.package.Main");

        ComponentKey owner = ComponentKey.fromString(
                new ComponentKey(component, UserHandle.of(0)).toString());
        ComponentKey privateProfile = ComponentKey.fromString(
                new ComponentKey(component, UserHandle.of(10)).toString());

        assertThat(owner.componentName).isEqualTo(component);
        assertThat(owner.user).isEqualTo(UserHandle.of(0));
        assertThat(privateProfile.componentName).isEqualTo(component);
        assertThat(privateProfile.user).isEqualTo(UserHandle.of(10));
    }

    @Test
    public void persistedComponentKeyRejectsInvalidUserId() {
        assertThat(ComponentKey.fromString("example.package/example.package.Main#-2"))
                .isNull();
        assertThat(ComponentKey.fromString("example.package/example.package.Main#bad"))
                .isNull();
    }
}
