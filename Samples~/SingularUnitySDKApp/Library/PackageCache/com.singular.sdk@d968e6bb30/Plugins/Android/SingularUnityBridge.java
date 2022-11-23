package com.singular.unitybridge;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.singular.sdk.DeferredDeepLinkHandler;
import com.singular.sdk.Singular;
import com.singular.sdk.SingularConfig;
import com.singular.sdk.SingularLinkHandler;
import com.singular.sdk.SingularLinkParams;
import com.unity3d.player.UnityPlayer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import com.singular.sdk.ShortLinkHandler;


public class SingularUnityBridge {

    public SingularUnityBridge() {
    }

    static SingularConfig config;
    static int currentIntentHash;

    public static void onNewIntent(final Intent intent) {
        UnityPlayer.currentActivity.runOnUiThread(new Runnable() {
            public void run() {

                // We save the intent hash code to make sure that the intent we get is a new one to avoid resolving an old deeplink.
                if (config == null || intent.hashCode() == currentIntentHash) {
                    return;
                }

                currentIntentHash = intent.hashCode();

                if (intent != null && intent.getData() != null &&
                        Intent.ACTION_VIEW.equals(intent.getAction())) {

                    config.withSingularLink(intent, config.linkCallback, config.shortlinkTimeoutSec);
                    Singular.init(UnityPlayer.currentActivity.getApplicationContext(), config);
                }
            }
        });
    }

    public static void init(String configString) {
        try {
            JSONObject configJson = new JSONObject(configString);
            String apikey = configJson.optString("apiKey", null);
            String secret = configJson.optString("secret", null);

            if (apikey == null || secret == null){
                return;
            }

            Context context = UnityPlayer.currentActivity.getApplicationContext();
            SingularConfig singularConfig = new SingularConfig(apikey, secret);

            String facebookAppId = configJson.optString("facebookAppId", null);
            String openUri = configJson.optString("openUri", null);

            if (!TextUtils.isEmpty(facebookAppId)) {
                singularConfig.withFacebookAppId(facebookAppId);
            }

            if (!TextUtils.isEmpty(openUri)) {
                Uri uri = Uri.parse(openUri);
                singularConfig.withOpenURI(uri);
            }

            boolean ddlEnable = configJson.optBoolean("enableDeferredDeepLinks", false);

            if (ddlEnable) {
                singularConfig.withDDLHandler(new DeferredDeepLinkHandler() {
                    @Override
                    public void handleLink(final String link) {

                        UnityPlayer.currentActivity.runOnUiThread(new Runnable() {
                            public void run() {
                                if (link == null) {
                                    UnityPlayer.UnitySendMessage("SingularSDKObject", "DeepLinkHandler", "");
                                } else {
                                    UnityPlayer.UnitySendMessage("SingularSDKObject", "DeepLinkHandler", link);
                                }
                            }
                        });
                    }
                });

                long ddlTimeoutSec = configJson.optLong("ddlTimeoutSec", 0);

                if (ddlTimeoutSec > 0) {
                    singularConfig.withDDLTimeoutInSec(ddlTimeoutSec);
                }
            }

            List<String> domains = new ArrayList<>();

            JSONArray supportedDomains = configJson.optJSONArray("supportedDomains");

            if (supportedDomains != null) {
                for (int i = 0; i < supportedDomains.length(); i++) {
                    domains.add(supportedDomains.getString(i));
                }
            }

            SingularLinkHandler handler = new SingularLinkHandler() {
                @Override
                public void onResolved(final SingularLinkParams singularLinkParams) {
                    UnityPlayer.currentActivity.runOnUiThread(new Runnable() {
                        public void run() {
                            JSONObject json = new JSONObject();
                            try {
                                json.put("deeplink", singularLinkParams.getDeeplink());
                                json.put("passthrough", singularLinkParams.getPassthrough());
                                json.put("is_deferred", singularLinkParams.isDeferred());
                            } catch (JSONException e) {
                                Log.d("SINGULAR_UNITY", "Unable to create json object with error:" + e.getMessage());
                            }
                            UnityPlayer.UnitySendMessage("SingularSDKObject", "SingularLinkHandlerResolved", json.toString());
                        }
                    });
                }
            };

            Intent intent = UnityPlayer.currentActivity.getIntent();
            currentIntentHash = intent.hashCode();

            long shortlinkResolveTimeout = configJson.optLong("shortlinkResolveTimeout", 0);

            if (shortlinkResolveTimeout > 0) {
                singularConfig.withSingularLink(intent, handler, shortlinkResolveTimeout, domains);
            } else {
                singularConfig.withSingularLink(intent, handler, domains);
            }

            boolean enableLogging = configJson.optBoolean("enableLogging", false);

            if (enableLogging) {
                singularConfig.withLoggingEnabled();
            }

            int logLevel = configJson.optInt("logLevel",-1);
            if (logLevel >= 0) {
                 singularConfig.withLogLevel(logLevel);
            }

            String fcmDeviceToken = configJson.optString("fcmDeviceToken", null);
            if (fcmDeviceToken != null) {
                 singularConfig.withFCMDeviceToken(fcmDeviceToken);
            }


            long sessionTimeoutSec = configJson.optLong("sessionTimeoutSec", 0);

            if (sessionTimeoutSec > 0) {
                singularConfig.withSessionTimeoutInSec(sessionTimeoutSec);
            }

            String customUserId = configJson.optString("customUserId", null);

            if (customUserId != null) {
                singularConfig.withCustomUserId(customUserId);
            }

            String imei = configJson.optString("imei", null);

            if (imei != null) {
                singularConfig.withIMEI(imei);
            }

            boolean collectOAID = configJson.optBoolean("collectOAID", false);

            if (collectOAID) {
                singularConfig.withOAIDCollection();
            }

            JSONObject globalProperties = configJson.optJSONObject("globalProperties");

            // Adding all of the global properties to the singular config
            if (globalProperties != null) {
                Iterator<String> iter = globalProperties.keys();
                while (iter.hasNext()) {
                    String key = iter.next();
                    JSONObject property = globalProperties.getJSONObject(key);

                    String propertyKey = property.optString("Key", "");

                    if (propertyKey != null && !propertyKey.trim().equals("")) {
                        singularConfig.withGlobalProperty(propertyKey,
                                property.optString("Value", ""),
                                property.optBoolean("OverrideExisting", false));
                    }
                }
            }

            // The config is saved so it can be used later when the app is resumed using a deeplink
            config = singularConfig;

            Singular.init(context, singularConfig);

        } catch (Exception e) {
            Log.d("SINGULAR_UNITY", e.getMessage());
        }
    }

    public static void createReferrerShortLink(String baseLink,
                                        String referrerName,
                                        String referrerId,
                                        String args){

        JSONObject params = null;
        try{
            params = new JSONObject(args);
        }catch (JSONException e){
            e.printStackTrace();
        }

        Singular.createReferrerShortLink(baseLink,
                referrerName,
                referrerId,
                params,
                new ShortLinkHandler() {
                    @Override
                    public void onSuccess(final String link) {
                        UnityPlayer.currentActivity.runOnUiThread(new Runnable() {
                            public void run() {
                                JSONObject json = new JSONObject();
                                try {
                                    json.put("data", link);
                                    json.put("error", null);
                                } catch (JSONException e) {
                                    Log.d("SINGULAR_UNITY", "Unable to create json object with error:" + e.getMessage());
                                }
                                UnityPlayer.UnitySendMessage("SingularSDKObject", "ShortLinkResolved", json.toString());
                            }
                        });
                    }

                    @Override
                    public void onError(final String error) {
                                JSONObject json = new JSONObject();
                                try {
                                    json.put("data", null);
                                    json.put("error", error);
                                } catch (JSONException e) {
                                    Log.d("SINGULAR_UNITY", "Unable to create json object with error:" + e.getMessage());
                                }
                                UnityPlayer.UnitySendMessage("SingularSDKObject", "ShortLinkResolved", json.toString());
                    }
                });


    }
}
