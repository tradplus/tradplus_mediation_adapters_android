package com.tradplus.ads.toutiao;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;


import com.bytedance.sdk.openadsdk.AdSlot;
import com.bytedance.sdk.openadsdk.CSJAdError;
import com.bytedance.sdk.openadsdk.CSJSplashAd;
import com.bytedance.sdk.openadsdk.TTAdConstant;
import com.bytedance.sdk.openadsdk.TTAdManager;
import com.bytedance.sdk.openadsdk.TTAdNative;
import com.bytedance.sdk.openadsdk.TTAdSdk;
import com.bytedance.sdk.openadsdk.TTAppDownloadListener;
import com.tradplus.ads.base.GlobalTradPlus;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.splash.TPSplashAdapter;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.lang.ref.SoftReference;
import java.util.Map;

import static com.tradplus.ads.base.common.TPError.ADAPTER_ACTIVITY_ERROR;
import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.INIT_FAILED;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;
import static com.tradplus.ads.base.common.TPError.SHOW_FAILED;
import static com.tradplus.ads.base.common.TPError.UNSPECIFIED;

public class ToutiaoSplashAd extends TPSplashAdapter {

    private String mPlacementId;
    private static final String TAG = "ToutiaoSplash";
    //开屏广告加载超时时间,建议大于3000,这里为了冷启动第一次加载到广告并且展示,示例设置了3000ms
    private int timeout = 3000;
    private TTAdManager adManager;
    private TTAdNative mAdNative;
    private int mIsTemplateRending;
    private int mZoomOut;
    private SplashClickEyeManager mSplashClickEyeManager;
    private CSJSplashAd.SplashClickEyeListener mSplashClickEyeListener;
    private boolean mIsSplashClickEye = false;//是否是开屏点睛
    private boolean isSupportClickEye;
    private CSJSplashAd mCSJSplashAd;
    private boolean isC2SBidding;
    private boolean isBiddingLoaded;
    private double ecpmLevel;
    private OnC2STokenListener onC2STokenListener;

    @Override
    public void loadCustomAd(final Context context, Map<String, Object> userParams, Map<String, String> tpParams) {
        if (mLoadAdapterListener == null && !isC2SBidding) {
            return;
        }

        if (tpParams != null && tpParams.size() > 0) {
            mPlacementId = tpParams.get(AppKeyManager.AD_PLACEMENT_ID);
            String template = tpParams.get(AppKeyManager.IS_TEMPLATE_RENDERING);
            mZoomOut = Integer.parseInt(tpParams.get(ToutiaoConstant.ZOOM_OUT));

            if (!TextUtils.isEmpty(template)) {
                mIsTemplateRending = Integer.parseInt(template);
            }
        } else {
            if (isC2SBidding) {
                if (onC2STokenListener != null) {
                    onC2STokenListener.onC2SBiddingFailed("", ADAPTER_CONFIGURATION_ERROR);
                }
            } else {
                mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_CONFIGURATION_ERROR));
            }
            return;
        }

        if (userParams != null && userParams.size() > 0) {
            if (userParams.containsKey(AppKeyManager.TIME_DELTA)) {
                int localTimeOut = (int) userParams.get(AppKeyManager.TIME_DELTA);

                // App Open ad timeout recommended >=3000ms.
                if (localTimeOut >= 1000) {
                    timeout = localTimeOut;
                    Log.i(TAG, "timeout: " + timeout);
                }
            }
        }

        adManager = TTAdSdk.getAdManager();
        mAdNative = adManager.createAdNative(context);

        ToutiaoInitManager.getInstance().initSDK(context, userParams, tpParams, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                loadSplashAd(context);
            }

            @Override
            public void onFailed(String code, String msg) {
                if (mLoadAdapterListener != null) {
                    TPError tpError = new TPError(INIT_FAILED);
                    tpError.setErrorCode(code);
                    tpError.setErrorMessage(msg);
                    mLoadAdapterListener.loadAdapterLoadFailed(tpError);
                }

                if (onC2STokenListener != null) {
                    onC2STokenListener.onC2SBiddingFailed(code + "", msg);
                }
            }
        });

    }


    private void loadSplashAd(final Context context) {
        if (isC2SBidding && isBiddingLoaded) {
            if (mLoadAdapterListener != null) {
                setNetworkObjectAd(mCSJSplashAd);
                // 竞价成功时的上报接⼝（必传），单位是分
                mCSJSplashAd.win(ecpmLevel);
                mLoadAdapterListener.loadAdapterLoaded(null);
            }
            return;
        }

        int width = UIUtils.getScreenWidthInPx(context);
        int height = UIUtils.getScreenHeightInPx(context);

        // V4.2.5.2开始不区分渲染方式，
        // 要求开发者同时设置setImageAcceptedSize（单位：px）和setExpressViewAcceptedSize（单位：dp ）接口，不同时设置可能会导致展示异常。
        final AdSlot.Builder builder = new AdSlot.Builder().setCodeId(mPlacementId).setSupportDeepLink(true).setImageAcceptedSize(width, height).setExpressViewAcceptedSize((float) width, (float) height);

        AdSlot adSlot = builder.build();
        //step4:请求广告，调用开屏广告异步请求接口，对请求回调的广告作渲染处理
        mAdNative.loadSplashAd(adSlot, new TTAdNative.CSJSplashAdListener() {
            @Override
            public void onSplashLoadSuccess() {
                // 广告物料、素材加载成功回调
                Log.i(TAG, "onSplashLoadSuccess: ");
            }

            @Override
            public void onSplashLoadFail(CSJAdError csjAdError) {
                LoadSplashFail(csjAdError, null);
            }

            // 广告渲染回调，接入方可以在这个回调中，调用ad.showSplashView(splashContainerView)进行渲染
            @Override
            public void onSplashRenderSuccess(CSJSplashAd csjSplashAd) {
                if (csjSplashAd == null) {
                    if (mLoadAdapterListener != null) {
                        mLoadAdapterListener.loadAdapterLoadFailed(new TPError(UNSPECIFIED));
                    }
                    Log.i(TAG, "onSplashRenderSuccess ,but csjSplashAd == null");
                    return;
                }
                mCSJSplashAd = csjSplashAd;
                Log.i(TAG, "onSplashRenderSuccess: ");

                if (mZoomOut == 1) {
                    //获取SplashView
                    View splashView = csjSplashAd.getSplashView();
                    //初始化开屏点睛相关数据
                    initSplashClickEyeData(context, csjSplashAd, splashView);
                }

                //设置SplashView的交互监听器
                csjSplashAd.setSplashAdListener(splashAdListener);

                if (csjSplashAd.getInteractionType() == TTAdConstant.INTERACTION_TYPE_DOWNLOAD) {
                    csjSplashAd.setDownloadListener(downloadListener);
                }

                if (isC2SBidding) {
                    if (onC2STokenListener != null) {
                        Map<String, Object> mediaExtraInfo = csjSplashAd.getMediaExtraInfo();
                        Integer price = (Integer)mediaExtraInfo.get("price");
                        Log.i(TAG, "price: "  + price);
                        if (price == null) {
                            onC2STokenListener.onC2SBiddingFailed("","price == null");
                            return;
                        }
                        ecpmLevel = price.doubleValue();
                        onC2STokenListener.onC2SBiddingResult(ecpmLevel);
                    }
                    isBiddingLoaded = true;
                    return;
                }

                if (mLoadAdapterListener != null) {
                    setNetworkObjectAd(csjSplashAd);
                    mLoadAdapterListener.loadAdapterLoaded(null);
                }

            }

            @Override
            public void onSplashRenderFail(CSJSplashAd csjSplashAd, CSJAdError csjAdError) {
                LoadSplashFail(csjAdError, csjSplashAd);
            }
        }, timeout);

    }


    /**
     * 竞价失败时的上报接⼝（必传）
     * auctionPrice 胜出者的第⼀名价格（不想上报价格传时null），单位是分
     * lossReason 竞价失败的原因（不想上报原因时传null），可参考枚举值或者媒体⾃定义回传
     * winBidder 胜出者（不想上报胜出者时传null），可参考枚举值或者媒体⾃定义回传
     * 102 bid价格低于最高价
     */
    @Override
    public void setLossNotifications(String auctionPrice, String lossReason) {
        if (mCSJSplashAd != null) {
            try {
                mCSJSplashAd.loss(Double.valueOf(auctionPrice), "102", null);
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }
    }

    private void LoadSplashFail(CSJAdError csjAdError, CSJSplashAd csjSplashAd) {
        if (mLoadAdapterListener == null) return;

        if (csjSplashAd != null) {
            Log.i(TAG, "onSplashRender: ");
        }

        if (isC2SBidding) {
            if (onC2STokenListener != null) {
                onC2STokenListener.onC2SBiddingFailed(csjAdError.getCode() + "", csjAdError.getMsg());
            }
            return;
        }

        if (csjAdError != null) {
            String msg = csjAdError.getMsg();
            int code = csjAdError.getCode();
            Log.i(TAG, "onSplashFail: code:" + code + "， msg：" + msg);
            mLoadAdapterListener.loadAdapterLoadFailed(ToutiaoErrorUtil.getTradPlusErrorCode(code, msg));
        } else {
            Log.i(TAG, "onSplashFail: ");
            mLoadAdapterListener.loadAdapterLoadFailed(new TPError(NETWORK_NO_FILL));
        }
    }

    private final CSJSplashAd.SplashAdListener splashAdListener = new CSJSplashAd.SplashAdListener() {
        @Override
        public void onSplashAdShow(CSJSplashAd csjSplashAd) {
            Log.i(TAG, "onSplashAdShow: ");
            if (mShowListener != null) {
                mShowListener.onAdShown();
            }
        }

        @Override
        public void onSplashAdClick(CSJSplashAd csjSplashAd) {
            Log.i(TAG, "onSplashAdClick");
            if (mShowListener != null) {
                mShowListener.onAdClicked();
            }
        }

        @Override
        public void onSplashAdClose(CSJSplashAd csjSplashAd, int i) {
            Log.i(TAG, "onSplashAdClose: ");
            if (mZoomOut != 1 || !isSupportClickEye) {
                close();
            }
        }
    };

    private final TTAppDownloadListener downloadListener = new TTAppDownloadListener() {
        boolean hasShow = false;

        @Override
        public void onIdle() {
            Log.i(TAG, "onIdle: ");
        }

        @Override
        public void onDownloadActive(long l, long l1, String s, String s1) {
            if (mDownloadListener != null && !isDownLoadStart) {
                isDownLoadStart = true;
                mDownloadListener.onDownloadStart(l, l1, s, s1);
            }
            Log.i(TAG, "onDownloadActive: " + l + " " + l1);
            if (mDownloadListener != null) mDownloadListener.onDownloadUpdate(l, l1, s, s1, 0);
        }

        @Override
        public void onDownloadPaused(long l, long l1, String s, String s1) {
            Log.i(TAG, "onDownloadPaused: " + l + " " + l1);
            if (mDownloadListener != null) mDownloadListener.onDownloadPause(l, l1, s, s1);
        }

        @Override
        public void onDownloadFailed(long l, long l1, String s, String s1) {
            Log.i(TAG, "onDownloadFailed: " + l + " " + l1);
            if (mDownloadListener != null) mDownloadListener.onDownloadFail(l, l1, s, s1);
        }

        @Override
        public void onDownloadFinished(long l, String s, String s1) {
            Log.i(TAG, "onDownloadFinished: " + l + " " + s1);
            if (mDownloadListener != null) mDownloadListener.onDownloadFinish(l, l, s, s1);
        }

        @Override
        public void onInstalled(String s, String s1) {
            Log.i(TAG, "onInstalled: " + s + " " + s1);
            if (mDownloadListener != null) mDownloadListener.onInstalled(0, 0, s, s1);
        }
    };

    private boolean isDownLoadStart;
    private boolean isClose;

    private void close() {
        if (mShowListener != null && !isClose) {
            isClose = true;
            mShowListener.onAdClosed();
        }
    }

    private void initSplashClickEyeData(Context context, CSJSplashAd splashAd, View splashView) {
        if (splashAd == null || splashView == null) {
            return;
        }

//        mIsSplashClickEye = SplashClickEyeManager.getInstance(mCxt).isSupportSplashClickEye();
        Activity activity = GlobalTradPlus.getInstance().getActivity();
        if (activity == null) {
            if (mLoadAdapterListener != null) {
                mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_ACTIVITY_ERROR));
            }
            return;
        }

        if (mAdContainerView != null) {
            Log.i(TAG, "initSplashClickEyeData: ");
            mSplashClickEyeListener = new SplashClickEyeListener(context, activity, splashAd, mAdContainerView);

            splashAd.setSplashClickEyeListener(mSplashClickEyeListener);

            mSplashClickEyeManager = SplashClickEyeManager.getInstance(context);

            mSplashClickEyeManager.setSplashInfo(splashAd, splashView, activity.getWindow().getDecorView());
        }
    }

    public class SplashClickEyeListener implements CSJSplashAd.SplashClickEyeListener {
        private SoftReference<Activity> mActivity;
        private CSJSplashAd mSplashAd;
        private ViewGroup mSplashContainer;
        private Context mContext;

        public SplashClickEyeListener(Context context, Activity activity, CSJSplashAd splashAd, ViewGroup splashContainer) {
            mContext = context;
            mActivity = new SoftReference<>(activity);
            mSplashAd = splashAd;
            mSplashContainer = splashContainer;
        }

        @Override
        public void onSplashClickEyeReadyToShow(CSJSplashAd csjSplashAd) {
            Log.i(TAG, "onSplashClickEyeReadyToShow: ");
            //开始执行开屏点睛动画
            startSplashAnimationStart();
            if (mShowListener != null) {
                mShowListener.onZoomOutStart();
            }
        }

        @Override
        public void onSplashClickEyeClick() {
            isSupportClickEye = true;
            // 广告loaded成功，即使不是开屏点睛广告，也应该正常展示
            SplashClickEyeManager splashClickEyeManager = SplashClickEyeManager.getInstance(mContext);
            splashClickEyeManager.setSupportSplashClickEye(true);
        }

        @Override
        public void onSplashClickEyeClose() {
            Log.i(TAG, "onSplashClickEyeClose: ");
            //sdk关闭了了点睛悬浮窗
            SplashClickEyeManager splashClickEyeManager = SplashClickEyeManager.getInstance(mContext);
            splashClickEyeManager.clearSplashStaticData();
            if (mShowListener != null) {
                mShowListener.onZoomOutEnd();
            }
            if (mShowListener != null && !isClose) {
                isClose = true;
                mShowListener.onAdClosed();
            }
        }

        private void startSplashAnimationStart() {
            Log.i(TAG, "startSplashAnimationStart: ");
            if (mActivity.get() == null || mSplashAd == null || mSplashContainer == null) {
                return;
            }

            Log.i(TAG, "onSplashClickEyeAnimationStart: ");
            SplashClickEyeManager splashClickEyeManager = SplashClickEyeManager.getInstance(mContext);
            ViewGroup content = mActivity.get().findViewById(android.R.id.content);
            splashClickEyeManager.startSplashClickEyeAnimation(mSplashContainer, content, content, new SplashClickEyeManager.AnimationCallBack() {
                @Override
                public void animationStart(int animationTime) {
                    Log.i(TAG, "animationStart: ");
                    if (mSplashAd != null) {
                        mSplashAd.startClickEye();
                    }
                }

                @Override
                public void animationEnd() {
                    Log.i(TAG, "animationEnd: ");
                    if (mSplashAd != null) {
                        mSplashAd.showSplashClickEyeView(mSplashContainer);
                    }

                }
            });
        }
    }

    @Override
    public void showAd() {
        Log.i(TAG, "showAd: ");
        if (mShowListener == null) return;
        if (mCSJSplashAd == null) {
            mShowListener.onAdVideoError(new TPError(SHOW_FAILED));
            return;
        }
        //设置SplashView的交互监听器
        View splashView = mCSJSplashAd.getSplashView();
        if (splashView != null && mAdContainerView != null) {
            mAdContainerView.removeAllViews();
            mAdContainerView.addView(splashView);
        } else {
            mShowListener.onAdVideoError(new TPError(SHOW_FAILED));
        }
    }

    @Override
    public void clean() {
        if (mCSJSplashAd != null) {
            mCSJSplashAd.setSplashAdListener(null);
            mCSJSplashAd.setSplashClickEyeListener(null);
            mCSJSplashAd.setDownloadListener(null);
            mCSJSplashAd = null;
        }

    }

    @Override
    public boolean isReady() {
        return !isAdsTimeOut();
    }

    @Override
    public String getNetworkName() {
        return RequestUtils.getInstance().getCustomAs(TradPlusInterstitialConstants.NETWORK_PANGLECN);
    }

    @Override
    public String getNetworkVersion() {
        if (adManager != null) {
            return adManager.getSDKVersion();
        }
        return null;
    }

    @Override
    public void getC2SBidding(final Context context, final Map<String, Object> localParams, final Map<String, String> tpParams, final OnC2STokenListener onC2STokenListener) {
        this.onC2STokenListener = onC2STokenListener;
        isC2SBidding = true;
        loadCustomAd(context, localParams, tpParams);
    }

}
