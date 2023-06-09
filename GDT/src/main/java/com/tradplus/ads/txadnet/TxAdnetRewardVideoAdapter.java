package com.tradplus.ads.txadnet;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.qq.e.ads.cfg.VideoOption;
import com.qq.e.ads.interstitial2.UnifiedInterstitialAD;
import com.qq.e.ads.interstitial2.UnifiedInterstitialADListener;
import com.qq.e.ads.rewardvideo.RewardVideoAD;
import com.qq.e.ads.rewardvideo.RewardVideoADListener;
import com.qq.e.ads.rewardvideo.ServerSideVerificationOptions;
import com.qq.e.comm.compliance.DownloadConfirmCallBack;
import com.qq.e.comm.compliance.DownloadConfirmListener;
import com.qq.e.comm.listeners.ADRewardListener;
import com.qq.e.comm.managers.GDTAdSdk;
import com.qq.e.comm.managers.status.SDKStatus;
import com.qq.e.comm.util.AdError;
import com.tradplus.ads.base.GlobalTradPlus;
import com.tradplus.ads.base.adapter.TPDownloadConfirmCallBack;
import com.tradplus.ads.base.adapter.TPDownloadConfirmListener;
import com.tradplus.ads.base.adapter.TPInitMediation;
import com.tradplus.ads.base.adapter.reward.TPRewardAdapter;
import com.tradplus.ads.common.DataKeys;
import com.tradplus.ads.base.common.TPError;
import com.tradplus.ads.base.util.AppKeyManager;
import com.tradplus.ads.base.util.TradPlusInterstitialConstants;
import com.tradplus.ads.pushcenter.utils.RequestUtils;

import java.util.Map;

import static com.tradplus.ads.base.common.TPError.ADAPTER_ACTIVITY_ERROR;
import static com.tradplus.ads.base.common.TPError.ADAPTER_CONFIGURATION_ERROR;
import static com.tradplus.ads.base.common.TPError.NETWORK_NO_FILL;
import static com.tradplus.ads.base.common.TPError.SHOW_FAILED;
import static com.tradplus.ads.base.common.TPError.UNSPECIFIED;

public class TxAdnetRewardVideoAdapter extends TPRewardAdapter {

    private RewardVideoAD rewardVideoAD;
    private static final long TIMEOUT_VALUE = 30 * 1000;
    private TxAdnetInterstitialCallbackRouter mGDTCbr;
    private String placementId, userId, customData;
    private boolean isVideoSoundEnable = true;
    private boolean hasGrantedReward = false;
    private boolean alwaysRewardUser;
    private Map<String, Object> mRewardMap;
    private int isRewardedInterstitialAd;
    private UnifiedInterstitialAD mUnifiedInterstitialAD;
    private static final String TAG = "GDTRewardedVideo";
    private ServerSideVerificationOptions serverSideVerificationOptions;
    private String payload;
    private String price;

    @Override
    public void loadCustomAd(final Context context,
                             final Map<String, Object> localExtras,
                             final Map<String, String> serverExtras) {
        if (mLoadAdapterListener == null) {
            return;
        }

        String mVideoMute;
        if (extrasAreValid(serverExtras)) {
            placementId = serverExtras.get(AppKeyManager.AD_PLACEMENT_ID);
            payload = serverExtras.get(DataKeys.BIDDING_PAYLOAD);
            price = serverExtras.get(DataKeys.BIDDING_PRICE);
            isRewardedInterstitialAd = Integer.parseInt(serverExtras.get(AppKeyManager.ADSOURCE_TYPE));
            mVideoMute = serverExtras.get(AppKeyManager.VIDEO_MUTE);

            if (!TextUtils.isEmpty(serverExtras.get(AppKeyManager.ALWAYS_REWARD))) {
                int rewardUser = Integer.parseInt(serverExtras.get(AppKeyManager.ALWAYS_REWARD));
                alwaysRewardUser = (rewardUser == AppKeyManager.ENFORCE_REWARD);
            }

            if (!TextUtils.isEmpty(mVideoMute)) {
                if (mVideoMute.equals(AppKeyManager.VIDEO_MUTE_YES)) {
                    isVideoSoundEnable = false;
                    Log.i(TAG, "videoMute: " + isVideoSoundEnable);
                }
            }
        } else {
            mLoadAdapterListener.loadAdapterLoadFailed(new TPError(ADAPTER_CONFIGURATION_ERROR));
            return;
        }

        if (localExtras != null && localExtras.size() > 0) {
            if (localExtras.containsKey(AppKeyManager.CUSTOM_USERID)) {
                userId = (String) localExtras.get(AppKeyManager.CUSTOM_USERID);
            }

            if (localExtras.containsKey(AppKeyManager.CUSTOM_DATA)) {
                customData = (String) localExtras.get(AppKeyManager.CUSTOM_DATA);
            }

            Log.i(TAG, "RewardData: userId : " + userId + " , customData : " + customData);
            ServerSideVerificationOptions.Builder builder = new ServerSideVerificationOptions.Builder();

            if (!TextUtils.isEmpty(userId)) {
                builder.setUserId(userId);
            }

            if (!TextUtils.isEmpty(customData)) {
                builder.setCustomData(customData);
            }

            if (!TextUtils.isEmpty(userId) || !TextUtils.isEmpty(customData)) {
                serverSideVerificationOptions = builder.build();
            }
        }

        mGDTCbr = TxAdnetInterstitialCallbackRouter.getInstance();
        mGDTCbr.addListener(placementId, mLoadAdapterListener);

        TencentInitManager.getInstance().initSDK(context, localExtras, serverExtras, new TPInitMediation.InitCallback() {
            @Override
            public void onSuccess() {
                initRewardVideo(context);
            }

            @Override
            public void onFailed(String code, String msg) {

            }
        });

    }

    private void initRewardVideo(Context context) {
        if (isRewardedInterstitialAd == AppKeyManager.INTERACTION_TYPE) {
            loadFullVideoInterstitial();
        } else {
            loadRewardVideo(context);

        }
    }

    private void loadRewardVideo(Context context) {
        if (TextUtils.isEmpty(payload)) {
            rewardVideoAD = new RewardVideoAD(context, placementId, mRwardVideoADListener, isVideoSoundEnable);
        } else {
            rewardVideoAD = new RewardVideoAD(context, placementId, mRwardVideoADListener, isVideoSoundEnable, payload);
        }

        if (serverSideVerificationOptions != null) {
            rewardVideoAD.setServerSideVerificationOptions(serverSideVerificationOptions);
        }
        rewardVideoAD.loadAD();
    }

    private void loadFullVideoInterstitial() {
        Activity activity = GlobalTradPlus.getInstance().getActivity();
        if (activity == null) {
            if (mGDTCbr.getListener(placementId) != null) {
                mGDTCbr.getListener(placementId).loadAdapterLoadFailed(new TPError(ADAPTER_ACTIVITY_ERROR));
            }
            return;
        }

        if (TextUtils.isEmpty(payload)) {
            mUnifiedInterstitialAD = new UnifiedInterstitialAD(activity, placementId, mUnifiedInterstitialADListener);
        } else {
            mUnifiedInterstitialAD = new UnifiedInterstitialAD(activity, placementId, mUnifiedInterstitialADListener, null, payload);
        }

        if (serverSideVerificationOptions != null) {
            mUnifiedInterstitialAD.setServerSideVerificationOptions(serverSideVerificationOptions);
        }

        VideoOption.Builder builder = new VideoOption.Builder();
        Log.i(TAG, "PlacementId: " + placementId + "， videoMute: " + isVideoSoundEnable);
        VideoOption option = builder.setAutoPlayMuted(!isVideoSoundEnable)
                .setDetailPageMuted(!isVideoSoundEnable)
                .setAutoPlayPolicy(VideoOption.AutoPlayPolicy.ALWAYS)
                .build();
        mUnifiedInterstitialAD.setVideoOption(option);
        mUnifiedInterstitialAD.loadFullScreenAD();
    }

    private final UnifiedInterstitialADListener mUnifiedInterstitialADListener = new UnifiedInterstitialADListener() {
        @Override
        public void onADReceive() {
            mUnifiedInterstitialAD.setRewardListener(new ADRewardListener() {
                @Override
                public void onReward(Map<String, Object> map) {
                    Log.i(TAG, "onReward: " + map.toString());
                    hasGrantedReward = true;
                    mRewardMap = map;
                }
            });
            if (mGDTCbr.getListener(placementId) != null) {
                Log.i(TAG, "onADReceive: " + mUnifiedInterstitialAD.getAdPatternType());
                setNetworkObjectAd(mUnifiedInterstitialAD);
                mGDTCbr.getListener(placementId).loadAdapterLoaded(null);
            }
        }

        @Override
        public void onVideoCached() {
        }

        @Override
        public void onNoAD(AdError adError) {
            Log.i(TAG, "onNoAD, errorcode :" + adError.getErrorCode() + ", errormessage :" + adError.getErrorMsg());
            if (mGDTCbr.getListener(placementId) != null) {
                mGDTCbr.getListener(placementId).loadAdapterLoadFailed(TxAdnetErrorUtil.getTradPlusErrorCode(adError));
            }
        }

        @Override
        public void onADOpened() {
            Log.i(TAG, "onADOpened: ");
            if (mGDTCbr.getShowListener(placementId) != null) {
                mGDTCbr.getShowListener(placementId).onAdShown();
                mGDTCbr.getShowListener(placementId).onAdVideoStart();
            }
        }

        @Override
        public void onADExposure() {
            Log.i(TAG, "onADExposure: ");
        }

        @Override
        public void onADClicked() {
            Log.i(TAG, "onADClicked: ");
            if (mGDTCbr.getShowListener(placementId) != null) {
                mGDTCbr.getShowListener(placementId).onAdVideoClicked();
            }
        }

        @Override
        public void onADLeftApplication() {
            Log.i(TAG, "onADLeftApplication: ");
        }

        @Override
        public void onADClosed() {
            if (mGDTCbr.getShowListener(placementId) == null) {
                return;
            }
            mGDTCbr.getShowListener(placementId).onAdVideoEnd();
            if (hasGrantedReward || alwaysRewardUser) {
                mGDTCbr.getShowListener(placementId).onReward(mRewardMap);
            }

            Log.i(TAG, "onADClosed: ");
            mGDTCbr.getShowListener(placementId).onAdClosed();

        }

        @Override
        public void onRenderSuccess() {

        }

        @Override
        public void onRenderFail() {
            if (mGDTCbr.getListener(placementId) != null) {
                mGDTCbr.getListener(placementId).loadAdapterLoadFailed(new TPError(NETWORK_NO_FILL));
            }
        }
    };


    private final RewardVideoADListener mRwardVideoADListener = new RewardVideoADListener() {
        @Override
        public void onADLoad() {
            Log.i(TAG, "onADLoad: ");

        }

        @Override
        public void onVideoCached() {
            Log.i(TAG, "onVideoCached: ");
            if (mGDTCbr.getListener(placementId) != null) {
                setNetworkObjectAd(rewardVideoAD);
                mGDTCbr.getListener(placementId).loadAdapterLoaded(null);
            }
        }

        @Override
        public void onADShow() {
            Log.i(TAG, "onADShow: ");
            if (mGDTCbr.getShowListener(placementId) != null) {
                mGDTCbr.getShowListener(placementId).onAdVideoStart();
            }
        }

        @Override
        public void onADExpose() {
            Log.i(TAG, "onADExpose: ");
            if (mGDTCbr.getShowListener(placementId) != null) {
                mGDTCbr.getShowListener(placementId).onAdShown();
            }
        }

        @Override
        public void onReward(Map<String, Object> map) {
            Log.i(TAG, "onReward: ");
            hasGrantedReward = true;
            mRewardMap = map;
        }

        @Override
        public void onADClick() {
            Log.i(TAG, "onADClick: ");
            if (mGDTCbr.getShowListener(placementId) != null)
                mGDTCbr.getShowListener(placementId).onAdVideoClicked();
        }

        @Override
        public void onVideoComplete() {
            Log.i(TAG, "onVideoComplete: ");
            if (mGDTCbr.getShowListener(placementId) != null)
                mGDTCbr.getShowListener(placementId).onAdVideoEnd();
        }

        @Override
        public void onADClose() {
            if (mGDTCbr.getShowListener(placementId) == null) {
                return;
            }

            Log.i(TAG, "onADClose: ");
            if (hasGrantedReward || alwaysRewardUser) {
                mGDTCbr.getShowListener(placementId).onReward(mRewardMap);
            }
            mGDTCbr.getShowListener(placementId).onAdClosed();

        }

        @Override
        public void onError(AdError adError) {
            Log.i(TAG, "onError , errorcode :" + adError.getErrorCode() + ", errormessage :" + adError.getErrorMsg());
            if (mGDTCbr.getListener(placementId) != null)
                mGDTCbr.getListener(placementId).loadAdapterLoadFailed(TxAdnetErrorUtil.getTradPlusErrorCode(adError));
        }
    };

    @Override
    public void showAd() {
        if (mGDTCbr != null && mShowListener != null) {
            mGDTCbr.addShowListener(placementId, mShowListener);
        }

        Activity activity = GlobalTradPlus.getInstance().getActivity();
        if (activity == null) {
            if (mShowListener != null) {
                mShowListener.onAdVideoError(new TPError(TPError.ADAPTER_ACTIVITY_ERROR));
            }
            return;
        }

        if (isRewardedInterstitialAd == AppKeyManager.INTERACTION_TYPE) {
            if (mUnifiedInterstitialAD == null) {
                if (mGDTCbr.getShowListener(placementId) != null) {
                    mGDTCbr.getShowListener(placementId).onAdVideoError(new TPError(UNSPECIFIED));
                }
                return;
            }

            setBidEcpm();
            mUnifiedInterstitialAD.showFullScreenAD(activity);
        } else {

            if (rewardVideoAD == null) {
                if (mGDTCbr.getShowListener(placementId) != null) {
                    mGDTCbr.getShowListener(placementId).onAdVideoError(new TPError(UNSPECIFIED));
                }
                return;
            }

            if (rewardVideoAD.hasShown()) {
                if (mGDTCbr.getShowListener(placementId) != null) {
                    TPError tpError = new TPError(SHOW_FAILED);
                    tpError.setErrorMessage("该条广告已经展示过一次");
                    mGDTCbr.getShowListener(placementId).onAdVideoError(tpError);
                }
                return;
            }

            setBidEcpm();
            rewardVideoAD.showAD(activity);

        }

    }

    @Override
    public void clean() {
        super.clean();
        if (rewardVideoAD != null) {
            rewardVideoAD = null;
        }

        if (mUnifiedInterstitialAD != null) {
            mUnifiedInterstitialAD.setRewardListener(null);
            mUnifiedInterstitialAD.destroy();
            mUnifiedInterstitialAD = null;
        }

        if (placementId != null) {
            mGDTCbr.removeListeners(placementId);
        }

    }

    @Override
    public boolean isReady() {
        if (rewardVideoAD != null) {
            return rewardVideoAD.isValid() && !isAdsTimeOut();
        }

        if (mUnifiedInterstitialAD != null) {
            return mUnifiedInterstitialAD.isValid() && !isAdsTimeOut();
        }
        return false;
    }

    private boolean extrasAreValid(final Map<String, String> serverExtras) {
        return serverExtras.containsKey(AppKeyManager.APP_ID);
    }

    @Override
    public String getNetworkName() {
        return RequestUtils.getInstance().getCustomAs(TradPlusInterstitialConstants.NETWORK_TENCENT);
    }

    @Override
    public String getNetworkVersion() {
        return SDKStatus.getIntegrationSDKVersion();
    }

    @Override
    public void setNetworkExtObj(Object obj) {
        if (obj instanceof DownloadConfirmListener) {
            if (rewardVideoAD != null) {
                rewardVideoAD.setDownloadConfirmListener((DownloadConfirmListener) obj);
            }
        }

        if (obj instanceof TPDownloadConfirmListener) {
            mTPDownloadConfirmListener = (TPDownloadConfirmListener) obj;
            if (rewardVideoAD != null) {
                rewardVideoAD.setDownloadConfirmListener(DOWNLOAD_CONFIRM_LISTENER);
            }
        }


    }

    private TPDownloadConfirmListener mTPDownloadConfirmListener;
    private final DownloadConfirmListener DOWNLOAD_CONFIRM_LISTENER = new DownloadConfirmListener() {

        @Override
        public void onDownloadConfirm(Activity context, int scenes, String infoUrl,
                                      final DownloadConfirmCallBack callBack) {
            Log.i("onDownloadConfirm", "scenes:" + scenes + " infourl:" + infoUrl);
            mTPDownloadConfirmListener.onDownloadConfirm(context, scenes, infoUrl, new TPDownloadConfirmCallBack() {
                @Override
                public void onConfirm() {
                    callBack.onConfirm();
                }

                @Override
                public void onCancel() {
                    callBack.onCancel();
                }
            });
        }
    };

    private void setBidEcpm() {
        try {
            float temp = Float.parseFloat(price);
            int price = (int) temp;
            Log.i(TAG, "setBidEcpm: " + price);
            if (isRewardedInterstitialAd == AppKeyManager.INTERACTION_TYPE) {
                mUnifiedInterstitialAD.setBidECPM(price);
            } else {
                rewardVideoAD.setBidECPM(price);
            }
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    @Override
    public String getBiddingToken(Context context, Map<String, String> tpParams) {
        if (tpParams == null) {
            return "";
        }
        String appId = tpParams.get(AppKeyManager.APP_ID);
        if (!TencentInitManager.isInited(appId)) {
            GDTAdSdk.init(context, appId);
        }
        return GDTAdSdk.getGDTAdManger().getBuyerId(null);
    }

    @Override
    public String getBiddingNetworkInfo(Context context, Map<String, String> tpParams) {
        if (tpParams == null) {
            return "";
        }

        if (tpParams.containsKey(AppKeyManager.AD_PLACEMENT_ID)) {
            String placementId = tpParams.get(AppKeyManager.AD_PLACEMENT_ID);
            try {
                return GDTAdSdk.getGDTAdManger().getSDKInfo(placementId);
            } catch (Throwable throwable) {
                throwable.printStackTrace();
                return "";
            }
        }
        return "";

    }
}

