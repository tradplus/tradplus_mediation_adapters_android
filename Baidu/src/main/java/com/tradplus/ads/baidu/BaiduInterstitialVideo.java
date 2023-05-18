package com.tradplus.ads.baidu;

import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.CACHE_FAILED;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;
import static com.tradplus.ads.base.common.TPError.SHOW_FAILED;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.baidu.mobads.sdk.api.AdSettings;
import com.baidu.mobads.sdk.api.RequestParameters;
import com.baidu.mobads.sdk.api.RewardVideoAd;
import com.baidu.mobads.sdk.api.RewardVideoAd.RewardVideoAdListener;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.reward.TPRewardAdapter;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.Map;

public class BaiduInterstitialVideo extends TPRewardAdapter {

    private String mPlacementId, userId, customData;
    private BaiduInterstitialCallbackRouter mCallbackRouter;
    private RewardVideoAd mRewardVideoAd;
    private boolean isC2SBidding;
    private boolean isBiddingLoaded;
    private OnC2STokenListener onC2STokenListener;
    private boolean hasGrantedReward = false;
    private boolean alwaysRewardUser;
    private static final String TAG = "BaiduInterstitialVideo";

    @Override
    public void loadCustomAd(final Context context, Map<String, Object> userParams, Map<String, String> tpParams) {
        if (mLoadAdapterListener == null && !isC2SBidding) {
            return;
        }

        if (extrasAreValid(tpParams)) {
            mPlacementId = tpParams.get(AppKeyManager.AD_PLACEMENT_ID);
            if (!TextUtils.isEmpty(tpParams.get(AppKeyManager.ALWAYS_REWARD))) {
                int rewardUser = Integer.parseInt(tpParams.get(AppKeyManager.ALWAYS_REWARD));
                alwaysRewardUser = (rewardUser == AppKeyManager.ENFORCE_REWARD);
            }
        } else {
            if (isC2SBidding) {
                if (onC2STokenListener != null) {
                    onC2STokenListener.onC2SBiddingFailed("",ADAPTER_CONFIGURATION_ERROR);
                }
            } else {
                mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_CONFIGURATION_ERROR));
            }
            return;
        }

        if (userParams != null && userParams.size() > 0) {
            if (userParams.containsKey(AppKeyManager.CUSTOM_USERID)) {
                userId = (String) userParams.get(AppKeyManager.CUSTOM_USERID);
            }

            if (userParams.containsKey(AppKeyManager.CUSTOM_DATA)) {
                customData = (String) userParams.get(AppKeyManager.CUSTOM_DATA);
            }
        }

        mCallbackRouter = BaiduInterstitialCallbackRouter.getInstance();
        mCallbackRouter.addListener(mPlacementId, mLoadAdapterListener);


        BaiduInitManager.getInstance().initSDK(context, userParams, tpParams, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                Log.i(TAG, "onSuccess: ");
                reqeusetInterstitialVideo(context);
            }

            @Override
            public void onFailed(String code, String msg) {

            }
        });


    }

    private void reqeusetInterstitialVideo(Context context) {
        //context : 需传入Activity的context
        //adPlaceId : 广告位
        //listener : 激励视频回调监听
        //useSurfaceView : 是否使用SurfaceView，默认使用TextureView
        if (isC2SBidding && isBiddingLoaded) {
            if (mCallbackRouter != null && mCallbackRouter.getListener(mPlacementId) != null) {
                setNetworkObjectAd(mRewardVideoAd);
                mCallbackRouter.getListener(mPlacementId).loadAdapterLoaded(null);
            }
            return;
        }

        if (mRewardVideoAd == null) {
            mRewardVideoAd = new RewardVideoAd(context, mPlacementId, mRewardVideoAdListener, false);
        }
        mRewardVideoAd.setDownloadAppConfirmPolicy(RequestParameters.DOWNLOAD_APP_CONFIRM_ALWAYS);

        if (!TextUtils.isEmpty(userId)) {
            mRewardVideoAd.setUserId(userId);
            Log.i(TAG, "userId: " + userId);
        }

        if (!TextUtils.isEmpty(customData)) {
            mRewardVideoAd.setExtraInfo(customData);
            Log.i(TAG, "ExtraInfo: " + customData);
        }
        mRewardVideoAd.load();
    }

    RewardVideoAdListener mRewardVideoAdListener = new RewardVideoAdListener() {
        @Override
        public void onAdShow() {
            Log.i(TAG, "onAdShow: ");
            if (mCallbackRouter.getShowListener(mPlacementId) != null) {
                mCallbackRouter.getShowListener(mPlacementId).onAdVideoStart();
            }

            if (mCallbackRouter.getShowListener(mPlacementId) != null) {
                mCallbackRouter.getShowListener(mPlacementId).onAdShown();
            }
        }

        @Override
        public void onAdClick() {
            Log.i(TAG, "onAdClick: ");
            if (mCallbackRouter.getShowListener(mPlacementId) != null) {
                mCallbackRouter.getShowListener(mPlacementId).onAdVideoClicked();
            }
        }

        @Override
        public void onAdClose(float v) {
            if (mCallbackRouter.getShowListener(mPlacementId) != null) {
                if (hasGrantedReward || alwaysRewardUser) {
                    mCallbackRouter.getShowListener(mPlacementId).onReward();
                }
                Log.i(TAG, "onAdClose: ");
                mCallbackRouter.getShowListener(mPlacementId).onAdClosed();
            }

        }

        @Override
        public void onAdFailed(String s) {
            Log.i(TAG, "onAdFailed: " + s);
            if (isC2SBidding) {
                if (onC2STokenListener != null) {
                    onC2STokenListener.onC2SBiddingFailed("",s);
                }
                return;
            }
            // 广告失败回调 原因：广告内容填充为空；网络原因请求广告超时
            // 建议：收到该回调之后，可以重新load下一条广告，最好限制load次数（4-5次即可）
            if (mCallbackRouter.getListener(mPlacementId) != null) {
                TPError tpError = new TPError(NETWORK_NO_FILL);
                tpError.setErrorMessage(s);
                mCallbackRouter.getListener(mPlacementId).loadAdapterLoadFailed(tpError);
            }
        }

        @Override
        public void onVideoDownloadSuccess() {
            Log.i(TAG, "onVideoDownloadSuccess: 视频缓存成功");
            if (isC2SBidding) {
                if (onC2STokenListener != null) {
                    String ecpmLevel = mRewardVideoAd.getECPMLevel();
                    Log.i(TAG, "激励视频 bid price: " + ecpmLevel);
                    if (TextUtils.isEmpty(ecpmLevel)) {
                        onC2STokenListener.onC2SBiddingFailed("","ecpmLevel is Empty");
                        return;
                    }
                    onC2STokenListener.onC2SBiddingResult(Double.parseDouble(ecpmLevel));
                }
                isBiddingLoaded = true;
                return;
            }
            // 说明：如果想一定走本地播放，那么收到该回调之后，可以调用show
            if (mCallbackRouter.getListener(mPlacementId) != null) {
                setNetworkObjectAd(mRewardVideoAd);
                mCallbackRouter.getListener(mPlacementId).loadAdapterLoaded(null);
            }
        }

        @Override
        public void onVideoDownloadFailed() {
            Log.i(TAG, "onVideoDownloadFailed: 视频缓存失败");
            if (isC2SBidding) {
                if (onC2STokenListener != null) {
                    onC2STokenListener.onC2SBiddingFailed("","视频缓存失败");
                }
                return;
            }
            // 视频缓存失败，如果想走本地播放，可以在这儿重新load下一条广告，最好限制load次数（4-5次即可）。
            if (mCallbackRouter.getListener(mPlacementId) != null) {
                mCallbackRouter.getListener(mPlacementId).loadAdapterLoadFailed(new TPError(CACHE_FAILED));
            }
        }

        @Override
        public void playCompletion() {
            Log.i(TAG, "playCompletion: 播放完成");
            if (mCallbackRouter.getShowListener(mPlacementId) != null) {
                mCallbackRouter.getShowListener(mPlacementId).onAdVideoEnd();
            }

        }

        @Override
        public void onAdSkip(float v) {
            Log.i(TAG, "onAdSkip: 用户点击跳过");
            alwaysRewardUser = false;
            if (mCallbackRouter.getShowListener(mPlacementId) != null) {
                mCallbackRouter.getShowListener(mPlacementId).onRewardSkip();
            }
        }

        @Override
        public void onRewardVerify(boolean rewardVerify) {
            Log.i(TAG, "onRewardVerify: " + rewardVerify);
            hasGrantedReward = rewardVerify;
        }

        @Override
        public void onAdLoaded() {
            Log.i(TAG, "onAdLoaded: 请求成功");
        }
    };

    @Override
    public void showAd() {
        if (mShowListener != null)
            mCallbackRouter.addShowListener(mPlacementId, mShowListener);

        if (mRewardVideoAd.isReady()) {
            mRewardVideoAd.show();
        } else {
            if (mCallbackRouter.getShowListener(mPlacementId) != null) {
                mCallbackRouter.getShowListener(mPlacementId).onAdVideoError(new TPError(SHOW_FAILED));
            }
        }
    }

    @Override
    public boolean isReady() {
       return mRewardVideoAd != null && mRewardVideoAd.isReady();
    }

    @Override
    public void clean() {
        super.clean();
        if (mRewardVideoAd != null) {
            mRewardVideoAd = null;
        }

        if (mRewardVideoAdListener != null) {
            mRewardVideoAdListener = null;
        }

        if (mPlacementId != null)
            mCallbackRouter.removeListeners(mPlacementId);
    }

    @Override
    public String getNetworkName() {
        return RequestUtils.getInstance().getCustomAs(TradPlusInterstitialConstants.NETWORK_BAIDU);
    }

    @Override
    public String getNetworkVersion() {
        return AdSettings.getSDKVersion();
    }

    private boolean extrasAreValid(final Map<String, String> serverExtras) {
        final String placementId = serverExtras.get(AppKeyManager.AD_PLACEMENT_ID);
        return (placementId != null && placementId.length() > 0);
    }

    @Override
    public void getC2SBidding(final Context context, final Map<String, Object> localParams, final Map<String, String> tpParams, final OnC2STokenListener onC2STokenListener) {
        this.onC2STokenListener = onC2STokenListener;
        isC2SBidding = true;
        loadCustomAd(context, localParams, tpParams);
    }

}