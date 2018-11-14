package com.iterable.iterableapi;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;

import android.graphics.Color;

import android.graphics.Point;
import android.graphics.Rect;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by David Truong dt@iterable.com.
 *
 * The IterableInAppManager handles creating and rendering different types of InApp Notifications received from the IterableApi
 */
public class IterableInAppManager {
    static final String TAG = "IterableInAppManager";

    private final IterableInAppStorage storage = new IterableInAppMemoryStorage();
    private final IterableInAppHandler handler;

    IterableInAppManager(IterableInAppHandler handler) {
        this.handler = handler;
    }

    public List<IterableInAppMessage> getAllMessages() {
        return storage.getMessages();
    }

    public void syncInApp() {
        IterableApi.getInstance().getInAppMessages(10, new IterableHelper.IterableActionHandler() {
            @Override
            public void execute(String payload) {
                if (payload != null && !payload.isEmpty()) {
                    try {
                        List<IterableInAppMessage> messages = new ArrayList<>();
                        JSONObject mainObject = new JSONObject(payload);
                        JSONArray jsonArray = mainObject.optJSONArray(IterableConstants.ITERABLE_IN_APP_MESSAGE);
                        if (jsonArray != null) {
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject messageJson = jsonArray.optJSONObject(i);
                                IterableInAppMessage message = IterableInAppMessage.fromJSON(messageJson);
                                if (message != null) {
                                    messages.add(message);
                                }
                            }
                        }
                        syncWithRemoteQueue(messages);
                    } catch (JSONException e) {
                        IterableLogger.e(TAG, e.toString());
                    }
                }
            }
        });
    }

    private void syncWithRemoteQueue(List<IterableInAppMessage> remoteQueue) {
        storage.putMessages(remoteQueue);
        handler.onNewBatch(getAllMessages());
    }

    public void showMessage(IterableInAppMessage message) {
        Activity currentActivity = IterableActivityMonitor.getCurrentActivity();
        if (currentActivity != null) {
            if (IterableInAppManager.showIterableNotificationHTML(currentActivity, message.getContent().html, message.getMessageId(), new IterableHelper.IterableActionHandler() {
                @Override
                public void execute(String data) {
                    if (data != null && data.startsWith("itbl://")) {
                        IterableActionRunner.executeAction(IterableApi.getInstance().getMainActivityContext(), IterableAction.actionCustomAction(data), IterableActionSource.IN_APP);
                    } else {
                        IterableActionRunner.executeAction(IterableApi.getInstance().getMainActivityContext(), IterableAction.actionOpenUrl(data), IterableActionSource.IN_APP);
                    }
                }
            }, 0.0, message.getContent().padding)) {
                IterableApi.sharedInstance.inAppConsume(message.getMessageId());
            }
        }
    }

    /**
     * Displays an html rendered InApp Notification
     * @param context
     * @param htmlString
     * @param messageId
     * @param clickCallback
     * @param backgroundAlpha
     * @param padding
     */
    public static boolean showIterableNotificationHTML(Context context, String htmlString, String messageId, IterableHelper.IterableActionHandler clickCallback, double backgroundAlpha, Rect padding) {
        if (context instanceof Activity) {
            Activity currentActivity = (Activity) context;
            if (htmlString != null) {
                if (IterableInAppHTMLNotification.getInstance() != null) {
                    IterableLogger.w(TAG, "Skipping the in-app notification: another notification is already being displayed");
                    return false;
                }

                IterableInAppHTMLNotification notification = IterableInAppHTMLNotification.createInstance(context, htmlString);
                notification.setTrackParams(messageId);
                notification.setCallback(clickCallback);
                notification.setBackgroundAlpha(backgroundAlpha);
                notification.setPadding(padding);
                notification.setOwnerActivity(currentActivity);
                notification.show();
                return true;
            }
        } else {
            IterableLogger.w(TAG, "To display in-app notifications, the context must be of an instance of: Activity");
        }
        return false;
    }

    /**
     * Gets the next message from the payload
     * @param payload
     * @return
     */
    public static JSONObject getNextMessageFromPayload(String payload) {
        JSONObject returnObject = null;
        if (payload != null && payload != "") {
            try {
                JSONObject mainObject = new JSONObject(payload);
                JSONArray jsonArray = mainObject.optJSONArray(IterableConstants.ITERABLE_IN_APP_MESSAGE);
                if (jsonArray != null) {
                    returnObject = jsonArray.optJSONObject(0);
                }
            } catch (JSONException e) {
                IterableLogger.e(TAG, e.toString());
            }
        }
        return returnObject;
    }

    /**
     * Returns a Rect containing the paddingOptions
     * @param paddingOptions
     * @return
     */
    public static Rect getPaddingFromPayload(JSONObject paddingOptions) {
        Rect rect = new Rect();
        rect.top = decodePadding(paddingOptions.optJSONObject("top"));
        rect.left = decodePadding(paddingOptions.optJSONObject("left"));
        rect.bottom = decodePadding(paddingOptions.optJSONObject("bottom"));
        rect.right = decodePadding(paddingOptions.optJSONObject("right"));

        return rect;
    }

    /**
     * Retrieves the padding percentage
     * @discussion -1 is returned when the padding percentage should be auto-sized
     * @param jsonObject
     * @return
     */
    static int decodePadding(JSONObject jsonObject) {
        int returnPadding = 0;
        if (jsonObject != null) {
            if ("AutoExpand".equalsIgnoreCase(jsonObject.optString("displayOption"))) {
                returnPadding = -1;
            } else {
                returnPadding = jsonObject.optInt("percentage", 0);
            }
        }
        return returnPadding;
    }

}