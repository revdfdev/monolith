package com.rehan.monolith;

import com.rehan.monolith.OpResult;

interface IShizukuShellService {
    void destroy() = 16777114;
    int getUid() = 2;
    OpResult setSuspended(in String[] pkgs, boolean suspended) = 3;
    OpResult setEnabled(in String[] pkgs, boolean enabled) = 4;
    OpResult forceStop(String pkg) = 5;
    OpResult putSetting(String namespace, String key, String value) = 6;
    OpResult getSetting(String namespace, String key) = 7;
    OpResult uninstall(String pkg) = 8;
    OpResult install(in ParcelFileDescriptor apk, long size, in String[] flags) = 9;
    OpResult listDisabledPackages() = 10;
    OpResult trimCaches() = 11;
    OpResult setDnd(String mode) = 12;
    OpResult allowUsageStats(String selfPkg) = 13;
    OpResult listUsers() = 14;
    OpResult switchUser(int userId) = 15;
    OpResult createUser(String name) = 16;
    OpResult installExisting(String pkg, int userId) = 17;
}
