package com.skobbler.ngx.sdktools.download;

/**
 * Listener for download component
 * Must be implemented by anyone who uses download sdk tools component
 * Created by CatalinM on 11/13/2014.
 */
public interface SKToolsDownloadListener {

    void onDownloadProgress(SKToolsDownloadItem currentDownloadItem);

    void onDownloadCancelled(String currentDownloadItemCode);

    void onDownloadPaused(SKToolsDownloadItem currentDownloadItem);

    void onInternetConnectionFailed(SKToolsDownloadItem currentDownloadItem, boolean responseReceivedFromServer);

    void onAllDownloadsCancelled();

    void onNotEnoughMemoryOnCurrentStorage(SKToolsDownloadItem currentDownloadItem);

    void onInstallStarted(SKToolsDownloadItem currentInstallingItem);

    void onInstallFinished(SKToolsDownloadItem currentInstallingItem);
}