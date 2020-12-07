package com.pushwoosh.reactnativeplugin;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.Intent;

import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.notification.NotificationServiceExtension;
import com.pushwoosh.notification.PushMessage;

public class PushwooshNotificationServiceExtension extends NotificationServiceExtension {

	private boolean showForegroundPush;

	private boolean silentPushBroadcastEnabled;
	private String silentPushBroadcastAction;
	private String silentPushBroadcastParameter;

	private static final String SILENT_PUSH_BROADCAST_ACTION_DEFAULT = "MESSAGE";
	private static final String SILENT_PUSH_BROADCAST_PARAMETER_DEFAULT = "message";

	public PushwooshNotificationServiceExtension() {
		try {
			String packageName = getApplicationContext().getPackageName();
			ApplicationInfo ai = getApplicationContext().getPackageManager().getApplicationInfo(packageName, PackageManager.GET_META_DATA);

			if (ai.metaData != null) {
				showForegroundPush = ai.metaData.getBoolean("PW_BROADCAST_PUSH", false) || ai.metaData.getBoolean("com.pushwoosh.foreground_push", false);
				silentPushBroadcastEnabled = ai.metaData.getBoolean("com.pushwoosh.silent_push_broadcast_enabled", false);
				if (silentPushBroadcastEnabled) {
					silentPushBroadcastAction = ai.metaData.getString("com.pushwoosh.silent_push_broadcast_action", SILENT_PUSH_BROADCAST_ACTION_DEFAULT);
					silentPushBroadcastParameter = ai.metaData.getString("com.pushwoosh.silent_push_broadcast_parameter", SILENT_PUSH_BROADCAST_PARAMETER_DEFAULT);
				}
			}
		} catch (Exception e) {
			PWLog.exception(e);
		}

		PWLog.debug(PushwooshPlugin.TAG, "showForegroundPush = " + showForegroundPush);
	}

	@Override
	protected boolean onMessageReceived(final PushMessage pushMessage) {
		if (silentPushBroadcastEnabled && pushMessage != null && pushMessage.isSilent()) {
			sendBroadcastMessage(pushMessage.getCustomData());
		}
		PushwooshPlugin.messageReceived(pushMessage.toJson().toString());
		return (!showForegroundPush && isAppOnForeground()) || super.onMessageReceived(pushMessage);
	}


	protected void sendBroadcastMessage(String data) {
		if (data != null && data.length() > 0) {
			PWLog.debug(PushwooshPlugin.TAG, "Broadcast silent push message: " + data);

			// send broadcast event
			Intent broadcastIntent = new Intent();
			String packageName = getApplicationContext().getPackageName();
			broadcastIntent.setAction(packageName + "." + silentPushBroadcastAction);
			// send broadcast intent only for current application (explicit broadcast)
			broadcastIntent.setPackage(packageName);
			broadcastIntent.putExtra(silentPushBroadcastParameter, data);

			getApplicationContext().sendBroadcast(broadcastIntent);
		}
	}

	@Override
	protected void startActivityForPushMessage(final PushMessage pushMessage) {
		super.startActivityForPushMessage(pushMessage);
 	}

	@Override
	protected void onMessageOpened(final PushMessage pushMessage) {
		PushwooshPlugin.openPush(pushMessage.toJson().toString());
	}
}
