package com.tradplus.ads.adcolony;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.adcolony.sdk.AdColony;
import com.adcolony.sdk.AdColonyAppOptions;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.TradPlus;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TestDeviceUtil;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;

import java.util.Map;


public class AdColonyInitManager extends TPInitMediation {

    private static final String TAG = "AdColony";
    private static AdColonyInitManager sInstance;
    private AdColonyAppOptions appOptions;
    private String userId, appId, zoneIds;

    public synchronized static AdColonyInitManager getInstance() {
        if (sInstance == null) {
            sInstance = new AdColonyInitManager();
        }
        return sInstance;
    }

    @Override
    public void initSDK(Context context, Map<String, Object> userParams, Map<String, String> tpParams, final InitCallback initCallback) {

        if (tpParams != null && tpParams.size() > 0) {
            appId = tpParams.get(AppKeyManager.APP_ID);
            zoneIds = tpParams.get(AdColonyConstant.ADCOLONY_ZONE_ID);
        }

        if (userParams != null && userParams.size() > 0) {
            userId = (String) userParams.get(AppKeyManager.CUSTOM_USERID);
            if (TextUtils.isEmpty(userId)) {
                userId = "";
            }
        }

        if (isInited(appId)) {
            initCallback.onSuccess();
            return;
        }

        if (hasInit(appId, initCallback)) {
            return;
        }

        String[] zoneIdsArr = zoneIds.split(",");

        // Construct optional app options object to be sent with configure
        if (appOptions == null) {
            appOptions = new AdColonyAppOptions();
        }

        //Used to enable test ads for your application without changing dashboard settings.
        if (!TextUtils.isEmpty(userId)) {
            Log.i(TAG, "RewardData: userId : " + userId);
            appOptions.setUserID(userId);
        }
        //Used to enable test ads for your application without changing dashboard settings.
        appOptions.setTestModeEnabled(TestDeviceUtil.getInstance().isNeedTestDevice());

        suportGDPR(context, userParams);
        Log.d(TradPlusInterstitialConstants.INIT_TAG, "initSDK: appId :" + appId);

        if (!(context instanceof Activity)) {
            if (initCallback != null) {
                initCallback.onFailed("", "Context is not Activity");
            }
            return;
        }

        boolean configure = AdColony.configure((Activity) context, appOptions, appId, zoneIdsArr);
        Log.i(TAG, "configure: " + configure);
        if (configure) {
            sendResult(appId, true);
        } else {
            sendResult(appId, false, "", TPError.UNSPECIFIED);
        }

    }

    @Override
    public void suportGDPR(Context context, Map<String, Object> userParams) {
        if (userParams != null && userParams.size() > 0) {
            if (userParams.containsKey(AppKeyManager.GDPR_CONSENT) && userParams.containsKey(AppKeyManager.IS_UE)) {
                boolean need_set_gdpr = true;
                int consent = (int) userParams.get(AppKeyManager.GDPR_CONSENT);
                if (consent == TradPlus.NONPERSONALIZED || consent == TradPlus.UNKNOWN) {
                    need_set_gdpr = false;
                }

                boolean isEu = (boolean) userParams.get(AppKeyManager.IS_UE);
                Log.i("privacylaws", "GDPR: " + need_set_gdpr + ":isUe:" + isEu);
                appOptions.setPrivacyFrameworkRequired(AdColonyAppOptions.GDPR, isEu);
                appOptions.setPrivacyConsentString(AdColonyAppOptions.GDPR, need_set_gdpr ? "1" : "0");
            }

            if (userParams.containsKey(AppKeyManager.KEY_CCPA)) {
                boolean ccpa = (boolean) userParams.get(AppKeyManager.KEY_CCPA);
                Log.i("privacylaws", "ccpa: " + ccpa);
                appOptions.setPrivacyFrameworkRequired(AdColonyAppOptions.CCPA, true);
                appOptions.setPrivacyConsentString(AdColonyAppOptions.CCPA, ccpa ? "1" : "0");
            } else {
                appOptions.setPrivacyFrameworkRequired(AdColonyAppOptions.CCPA, false);
            }

            if (userParams.containsKey(AppKeyManager.KEY_COPPA)) {
                boolean coppa = (boolean) userParams.get(AppKeyManager.KEY_COPPA);
                Log.i("privacylaws", "coppa:" + coppa);
                appOptions.setPrivacyFrameworkRequired(AdColonyAppOptions.COPPA, coppa);
            }

            if (userParams.containsKey(AppKeyManager.DFF)) {
                boolean dff = (boolean) userParams.get(AppKeyManager.DFF);
                Log.i("privacylaws", "dff:" + dff);
                appOptions.setIsChildDirectedApp(dff);
            }
        }
    }

    @Override
    public String getNetworkVersionCode() {
        // NOTE: AdColony returns an empty string if attempting to retrieve if not initialized
        return AdColony.getSDKVersion();
    }

    @Override
    public String getNetworkVersionName() {
        return "AdColony";
    }
}
