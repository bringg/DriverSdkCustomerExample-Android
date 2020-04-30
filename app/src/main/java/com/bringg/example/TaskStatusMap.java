package com.bringg.example;

import android.util.SparseArray;

import androidx.annotation.NonNull;

import static driver_sdk.models.Task.Status.STATUS_ACCEPTED;
import static driver_sdk.models.Task.Status.STATUS_ASSIGNED;
import static driver_sdk.models.Task.Status.STATUS_CANCELED;
import static driver_sdk.models.Task.Status.STATUS_CHECKED_IN;
import static driver_sdk.models.Task.Status.STATUS_DONE;
import static driver_sdk.models.Task.Status.STATUS_FREE;
import static driver_sdk.models.Task.Status.STATUS_REJECTED;
import static driver_sdk.models.Task.Status.STATUS_STARTED;

public class TaskStatusMap {

    @NonNull
    private static final SparseArray<String> mTranslationTable;

    static {
        mTranslationTable = new SparseArray<String>() {{
            put(STATUS_FREE, "Free");
            put(STATUS_ASSIGNED, "Assigned");
            put(STATUS_STARTED,"Started");
            put(STATUS_CHECKED_IN,"Checked-In");
            put(STATUS_DONE,"Done");
            put(STATUS_ACCEPTED,"Accepted");
            put(STATUS_CANCELED,"Canceled");
            put(STATUS_REJECTED,"Rejected");
        }};
    }

    public static String getUserStatus(int taskStatus) {
        return mTranslationTable.get(taskStatus);
    }
}
