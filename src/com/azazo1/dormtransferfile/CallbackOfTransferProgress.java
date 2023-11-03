package com.azazo1.dormtransferfile;

public interface CallbackOfTransferProgress {
    void callback(long now, long total);
}