package com.tradplus.joomob;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.View;

import com.joomob.sdk.common.ads.AdError;
import com.joomob.sdk.common.ads.JMADManager;
import com.joomob.sdk.common.ads.JMView;
import com.joomob.sdk.common.ads.JmAdSlot;
import com.joomob.sdk.common.ads.listener.JmDrawVideoListener;
import com.joomob.sdk.common.proxy.IDrawVideoAd;
import com.joomob.sdk.common.proxy.JMAD;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.nativead.TPNativeAdapter;
import com.tradplus.ads.base.bean.TPBaseAd;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TPContextUtils;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;

public class JoomobDrawVideoList extends TPNativeAdapter {
    public static final String TAG = "JoomobDrawVideoList";
    private String mAppId;
    private String mSlotId;
    private JMAD mJmad;
    private IDrawVideoAd mDrawVideoAd;
    private JoomobNativeAd mJoomobNativeAd;

    @Override
    public void loadCustomAd(Context context,
                             Map<String, Object> localExtras, Map<String, String> serverExtras) {
        if (mLoadAdapterListener == null) {
            return;
        }
        Log.i(TAG, "loadAdView: ");
        final Context _ct = TPContextUtils.getInstance(context).compareContext(context);
        if (_ct == null) {
            mLoadAdapterListener.loadAdapterLoadFailed(new TPError(TPError.ADAPTER_ACTIVITY_ERROR));
            return;
        }
        if (serverExtras != null && serverExtras.size() > 0) {
            mAppId = serverExtras.get(AppKeyManager.APP_ID);
            mSlotId = serverExtras.get(AppKeyManager.AD_PLACEMENT_ID);
        }
//        mAppId = "2001920";
//        mSlotId = "3001784620";
        if (!AppKeyManager.getInstance().isInited(mAppId, AppKeyManager.AdType.SHARE)) {
            JoomobInitManager.getInstance().initSDK(context, localExtras, serverExtras, new TPInitMediation.InitCallback() {
                @Override
                public void onSuccess() {
                    Log.i(TAG, "onSuccess: ");
                    requestDrawList(context);
                }

                @Override
                public void onFailed(String code, String msg) {

                }
            });
        } else {
            requestDrawList(context);
        }

    }

    private void requestDrawList(Context context) {
        mJmad = JMADManager.getInstance().create();
        int width = (int) UIUtils.getScreenWidthDp(context);
        int height = (int) UIUtils.getHeight((Activity) context);
        Log.i(TAG, "loadAdView width: " + width + ":height:" + height);
        final JmAdSlot jmAdSlot = new JmAdSlot.Builder()
                .setWidth(width)
                .setHeight(height)
                .setSlotId(mSlotId)
                .build();

        mJmad.loadDrawVideoAd((Activity) context, jmAdSlot, jmDrawVideoListener);
    }

    private JmDrawVideoListener jmDrawVideoListener = new JmDrawVideoListener() {
        @Override
        public void onDisplayAd() {
            Log.i(TAG, "onDisplayAd: ");
            if (mJoomobNativeAd != null) {
                mJoomobNativeAd.adShown();
            }
        }

        @Override
        public void onClickAd() {
            Log.i(TAG, "onClickAd: ");
            if (mJoomobNativeAd != null) {
                mJoomobNativeAd.adClicked();
            }
        }

        @Override
        public void onAdError(AdError adError) {
            Log.i(TAG, "onAdError msg: " + adError.getErrorMsg() + ":code:" + adError.getErrorCode());
            if (mLoadAdapterListener != null) {
                TPError tpError = new TPError(NETWORK_NO_FILL);
                tpError.setErrorCode(adError.getErrorCode()+"");
                tpError.setErrorMessage(adError.getErrorMsg());
                mLoadAdapterListener.loadAdapterLoadFailed(tpError);
            }
        }

        @Override
        public void onRequestSuccess(JMView jmView, IDrawVideoAd iDrawVideoAd) {
            Log.i(TAG, "onRequestSuccess: ");
            mDrawVideoAd = iDrawVideoAd;
            if (jmView != null) {
                List<View> jmViews = new ArrayList<>();
                jmViews.add(jmView);
                mJoomobNativeAd = new JoomobNativeAd(jmViews, TPBaseAd.AD_TYPE_NATIVE_LIST);
                if (mLoadAdapterListener != null) {
                    mLoadAdapterListener.loadAdapterLoaded(mJoomobNativeAd);
                }
            }
        }

        @Override
        public void onPlayError(AdError adError) {
            Log.i(TAG, "onPlayError: ");
        }

        @Override
        public void onVideoAdPaused() {
            Log.i(TAG, "onVideoAdPaused: ");
        }

        @Override
        public void onProgressUpdate(long l, long l1) {
            Log.i(TAG, "onProgressUpdate: ");
        }

        @Override
        public void onVideoAdComplete() {
            Log.i(TAG, "onVideoAdComplete: ");
        }

        @Override
        public void onClickRetry() {
            Log.i(TAG, "onClickRetry: ");
        }

        @Override
        public void onVideoAdStartPlay() {
            Log.i(TAG, "onVideoAdStartPlay: ");
        }

        @Override
        public void onVideoLoad() {
            Log.i(TAG, "onVideoLoad: ");
        }

        @Override
        public void onRenderSuccess() {
            Log.i(TAG, "onRenderSuccess: ");
            if (mJoomobNativeAd != null) {
                mJoomobNativeAd.adShown();
            }
        }
    };

    @Override
    public void clean() {
        Log.i(TAG, "onInvalidate: ");
        if (mDrawVideoAd != null) {
            mDrawVideoAd.destroy();
        }
    }

    @Override
    public String getNetworkName() {
        return RequestUtils.getInstance().getCustomAs(TradPlusInterstitialConstants.NETWORK_JULIANG);
    }

    @Override
    public String getNetworkVersion() {
        return BuildConfig.VERSION_NAME;
    }
}
