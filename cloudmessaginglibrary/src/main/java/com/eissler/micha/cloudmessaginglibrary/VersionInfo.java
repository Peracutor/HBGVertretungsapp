package com.eissler.micha.cloudmessaginglibrary;

/**
 * Created by Micha.
 * 07.07.2017
 */
public interface VersionInfo {
    Integer getVersionNumber();

    String getVersionName();

    String getVersionDescription();

    String getApkUrl();
}
