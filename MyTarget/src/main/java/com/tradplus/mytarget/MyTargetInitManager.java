package com.tradplus.mytarget;

import android.content.Context;
import android.util.Log;

import com.my.target.common.MyTargetPrivacy;
import com.my.target.common.MyTargetVersion;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.TradPlus;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.Map;

public class MyTargetInitManager extends TPInitMediation {


    private static MyTargetInitManager sInstance;

    public synchronized static MyTargetInitManager getInstance() {
        if (sInstance == null) {
            sInstance = new MyTargetInitManager();
        }
        return sInstance;
    }

    @Override
    public void initSDK(Context context, Map<String, Object> userParams, Map<String, String> tpParams, InitCallback initCallback) {
        // 该源没有初始化SDK的方法，写这个类仅为了实现开发者接入时可获取对应的VersionCode
        // 另外相关GDPR代码抽取到该类中

        String customAs = RequestUtils.getInstance().getCustomAs(TradPlusInterstitialConstants.NETWORK_MYTARGET);

        if (isInited(customAs)) {
            initCallback.onSuccess();
            return;
        }

        if (hasInit(customAs, initCallback)) {
            return;
        }

        suportGDPR(context, userParams);

        sendResult(customAs, true);
    }

    @Override
    public void suportGDPR(Context context, Map<String, Object> localExtras) {
        if (localExtras != null && localExtras.size() > 0) {
            if (localExtras.containsKey(AppKeyManager.GDPR_CONSENT) && localExtras.containsKey(AppKeyManager.IS_UE)) {
                boolean need_set_gdpr = false;
                int consent1 = (int) localExtras.get(AppKeyManager.GDPR_CONSENT);
                if (consent1 == TradPlus.PERSONALIZED) {
                    need_set_gdpr = true;
                }

                boolean isEu = (boolean) localExtras.get(AppKeyManager.IS_UE);
                Log.i("privacylaws", "suportGDPR: " + need_set_gdpr + ":isUe:" + isEu);
                //true:agree  false:deny
                MyTargetPrivacy.setUserConsent(need_set_gdpr);
            }

            if (localExtras.containsKey(AppKeyManager.KEY_COPPA)) {
                boolean coppa = (boolean) localExtras.get(AppKeyManager.KEY_COPPA);
                Log.i("privacylaws", "coppa: " + coppa);
                MyTargetPrivacy.setUserAgeRestricted(coppa);
            }

            if (localExtras.containsKey(AppKeyManager.KEY_CCPA)) {
                boolean ccpa = (boolean) localExtras.get(AppKeyManager.KEY_CCPA);
                Log.i("privacylaws", "ccpa: " + ccpa);
                MyTargetPrivacy.setCcpaUserConsent(ccpa);
            }
        }
    }

    @Override
    public String getNetworkVersionCode() {
        return MyTargetVersion.VERSION;
    }

    @Override
    public String getNetworkVersionName() {
        return "MyTarget";
    }
}