package com.tradplus.ads.inmobix;

import android.content.Context;
import android.util.Log;


import com.inmobi.sdk.InMobiSdk;
import com.inmobi.sdk.SdkInitializationListener;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.TradPlus;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TestDeviceUtil;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class InmobiInitManager extends TPInitMediation {

    private static final String TAG = "Inmobi";
    private String mAccountId;
    private static InmobiInitManager sInstance;
    boolean need_set_gdpr = false;
    private HashMap<String, String> mParameters;

    public synchronized static InmobiInitManager getInstance() {
        if (sInstance == null) {
            sInstance = new InmobiInitManager();
        }
        return sInstance;
    }

    private boolean availableParams(Map<String, String> tpParams) {
        mAccountId = tpParams.get(AppKeyManager.ACCOUNT_ID);
        return (mAccountId != null && mAccountId.length() > 0);
    }

    @Override
    public void initSDK(Context context, Map<String, Object> userParams, Map<String, String> tpParams, final InitCallback initCallback) {

        if (!availableParams(tpParams)) {
            // 初始化SDK参数有问题返回失败
            sendResult(TAG, false, "", TPError.EMPTY_INIT_CONFIGURATION);
            return;
        }

        if (isInited(mAccountId)) {
            initCallback.onSuccess();
            return;
        }

        if (hasInit(mAccountId, initCallback)) {
            return;
        }

        suportGDPR(context, userParams);

        if (TestDeviceUtil.getInstance().isNeedTestDevice()) {
            InMobiSdk.setLogLevel(InMobiSdk.LogLevel.DEBUG);
        }

        //Initialize Inmobi SDK before any API call.
        JSONObject consent = new JSONObject();
        try {
            // Provide correct consent value to sdk which is obtained by User
            consent.put(InMobiSdk.IM_GDPR_CONSENT_AVAILABLE, need_set_gdpr);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.d(TradPlusInterstitialConstants.INIT_TAG, "initSDK: accountId :" + mAccountId);
        InMobiSdk.init(context, mAccountId, consent, new SdkInitializationListener() {
            @Override
            public void onInitializationComplete(Error error) {
                if (error == null) {
                    Log.i(TAG, "onInitializationComplete: ");
                    sendResult(mAccountId, true);
                } else {
                    String message = error.getMessage();
                    Log.i(TAG, "onInitialization Failed : " + message);
                    sendResult(mAccountId, false, "", message);
                }
            }
        });


    }

    @Override
    public void suportGDPR(Context context, Map<String, Object> userParams) {
        //GDPR
        if (userParams != null && userParams.size() > 0) {
            if (userParams.containsKey(AppKeyManager.GDPR_CONSENT) && userParams.containsKey(AppKeyManager.IS_UE)) {
                int consent = (int) userParams.get(AppKeyManager.GDPR_CONSENT);
                if (consent == TradPlus.PERSONALIZED) {
                    need_set_gdpr = true;
                }
                boolean isEu = (boolean) userParams.get(AppKeyManager.IS_UE);
                Log.i("privacylaws", "suportGDPR: " + need_set_gdpr + ":isUe:" + isEu);

                JSONObject consentObject = new JSONObject();
                try {
                    // Provide correct consent value to sdk which is obtained by User
                    consentObject.put(InMobiSdk.IM_GDPR_CONSENT_AVAILABLE, isEu);
                    // Provide 0 if GDPR is not applicable and 1 if applicable
                    consentObject.put(InMobiSdk.IM_GDPR_CONSENT_GDPR_APPLIES, need_set_gdpr ? 1 : 0);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                InMobiSdk.updateGDPRConsent(consentObject);
            }

            if (userParams.containsKey(AppKeyManager.KEY_COPPA) || userParams.containsKey(AppKeyManager.DFF)) {
                boolean isChildDirected = false;
                // COPPA隐私标志设置为true，以限制传输android广告标识符后才能设置DFF
                if (userParams.containsKey(AppKeyManager.KEY_COPPA)) {
                    isChildDirected = (boolean) userParams.get(AppKeyManager.KEY_COPPA);
                    Log.i("privacylaws", "coppa: " + isChildDirected);
                } else {
                    isChildDirected = (boolean) userParams.get(AppKeyManager.DFF);
                    Log.i("privacylaws", "dff:" + isChildDirected);
                }
                InMobiSdk.setIsAgeRestricted(isChildDirected);
            }


            if (userParams.containsKey(AppKeyManager.KEY_CCPA)) {
                boolean ccpa = (boolean) userParams.get(AppKeyManager.KEY_CCPA);
                // 如果您不想退出基于兴趣的广告，请将不出售标志设置为 false
                // 如果您想选择退出基于兴趣的广告，请将不出售标志设置为 true
                // TP ccpa true表示同意出售，此时do_not_sell标志要返回false,String传“0”
                mParameters = new HashMap<>();
                mParameters.put("do_not_sell", ccpa ? "0" : "1");
                Log.i("privacylaws", "ccpa:" + ccpa);
            }
        }

    }

    public Map<String, String> getParameters() {
        Log.i("privacylaws", "Parameters:" + mParameters);
        return mParameters != null ? mParameters : null;
    }

    @Override
    public String getNetworkVersionCode() {
        return InMobiSdk.getVersion();
    }

    @Override
    public String getNetworkVersionName() {
        return "InMobi";
    }
}
