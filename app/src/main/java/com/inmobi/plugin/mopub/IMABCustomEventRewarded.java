package com.inmobi.plugin.mopub;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.aerserv.sdk.AerServEvent;
import com.aerserv.sdk.AerServEventListener;
import com.aerserv.sdk.AerServInterstitial;
import com.aerserv.sdk.AerServTransactionInformation;
import com.aerserv.sdk.AerServVirtualCurrency;
import com.mopub.common.LifecycleListener;
import com.mopub.common.MoPubReward;
import com.mopub.mobileads.CustomEventRewardedVideo;
import com.mopub.mobileads.MoPubErrorCode;
import com.mopub.mobileads.MoPubRewardedVideoManager;

import java.util.List;
import java.util.Map;

public class IMABCustomEventRewarded extends CustomEventRewardedVideo {

    private static final String AD_UNIT_KEY = "com_mopub_ad_unit_id";

    private IMAudienceBidder.IMAudienceBidderMediationSettings mMediationSettings = null;

    @NonNull
    @Override
    protected String getAdNetworkId() { return mMediationSettings.getPlacement(); }

    @Override
    protected void onInvalidate() {
        mMediationSettings = null;
    }

    @Nullable
    @Override
    protected LifecycleListener getLifecycleListener() {
        return null;
    }

    @Override
    protected boolean checkAndInitializeSdk(@NonNull Activity activity,
                                            @NonNull Map<String, Object> localExtras,
                                            @NonNull Map<String, String> serverExtras) throws Exception {
        return true;
    }

    @Override
    protected void loadWithSdkInitialized(@NonNull Activity activity,
                                          @NonNull Map<String, Object> localExtras,
                                          @NonNull Map<String, String> serverExtras)  {
        if (!localExtras.containsKey(AD_UNIT_KEY)) {
            MoPubRewardedVideoManager.onRewardedVideoLoadFailure(this.getClass(), null, MoPubErrorCode.NETWORK_INVALID_STATE);
        }

        Object localExtraObject = localExtras.get(AD_UNIT_KEY);
        if (localExtraObject instanceof String) {
            String adUnit = (String) localExtraObject;
            if (TextUtils.isEmpty(adUnit)) {
                MoPubRewardedVideoManager.onRewardedVideoLoadFailure(this.getClass(), null, MoPubErrorCode.NETWORK_INVALID_STATE);
                return;
            }

            IMAudienceBidder.IMAudienceBidderMediationSettings mediationSettings = MoPubRewardedVideoManager.getInstanceMediationSettings(IMAudienceBidder.IMAudienceBidderMediationSettings.class, adUnit);
            if (mediationSettings != null) {
                mMediationSettings = mediationSettings;
                String placement = mediationSettings.getPlacement();
                MoPubRewardedVideoManager.onRewardedVideoLoadSuccess(this.getClass(), placement);
            } else {
                MoPubRewardedVideoManager.onRewardedVideoLoadFailure(this.getClass(), null, MoPubErrorCode.NETWORK_INVALID_STATE);
            }
        } else {
            MoPubRewardedVideoManager.onRewardedVideoLoadFailure(this.getClass(), null, MoPubErrorCode.NETWORK_INVALID_STATE);
        }
    }

    @Override
    protected boolean hasVideoAvailable() {
        return mMediationSettings != null;
    }

    @Override
    protected void showVideo() {
        if (mMediationSettings != null) {
            mMediationSettings.getAerServInterstitial().show();
        }
    }

    public static class AerServRewardedListener implements AerServEventListener {

        public static final Handler handler = new Handler(Looper.getMainLooper());

        private AerServInterstitial mAerServInterstitial = null;
        private String mAdUnit = null;

        @NonNull
        private final String mPlacement;
        @NonNull
        private final IMAudienceBidder.IMAudienceBidderRewardedListener mAudienceBidderListener;

        AerServRewardedListener(@NonNull String placement,
                                @NonNull IMAudienceBidder.IMAudienceBidderRewardedListener audienceBidderListener) {
            mPlacement = placement;
            mAudienceBidderListener = audienceBidderListener;
        }

        void setAerServInterstitial(@NonNull AerServInterstitial aerServInterstitial) {
            mAerServInterstitial = aerServInterstitial;
        }

        @Override
        public void onAerServEvent(AerServEvent aerServEvent, final List<Object> args) {
            switch(aerServEvent) {
                case VC_READY:
                    break;
                case PRELOAD_READY:
                    break;
                case AD_FAILED:
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (mAdUnit != null) {
                                MoPubRewardedVideoManager.onRewardedVideoLoadFailure(IMABCustomEventRewarded.class, mPlacement, MoPubErrorCode.NETWORK_INVALID_STATE);
                            } else {
                                Error error = new Error("Failed to load MoPub and the corresponding bid price");
                                IMAudienceBidder.IMABRewardedWrapper bidObject = new IMAudienceBidder.IMABRewardedWrapper(null, null);
                                mAudienceBidderListener.onBidFailed(bidObject, error);
                            }
                        }
                    });
                    break;
                case AD_IMPRESSION:
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            MoPubRewardedVideoManager.onRewardedVideoStarted(IMABCustomEventRewarded.class, mPlacement);
                        }
                    });
                    break;
                case AD_CLICKED:
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            MoPubRewardedVideoManager.onRewardedVideoClicked(IMABCustomEventRewarded.class, mPlacement);
                        }
                    });
                    break;
                case AD_DISMISSED:
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            MoPubRewardedVideoManager.onRewardedVideoClosed(IMABCustomEventRewarded.class, mPlacement);
                        }
                    });
                    break;
                case LOAD_TRANSACTION:
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (args.get(0) instanceof AerServTransactionInformation) {
                                AerServTransactionInformation transactionInformation = (AerServTransactionInformation) args.get(0);
                                String bidKeyword = IMAudienceBidder.getBidKeyword(transactionInformation);

                                IMAudienceBidder.IMAudienceBidderMediationSettings mediationSettings = new IMAudienceBidder.IMAudienceBidderMediationSettings(mPlacement, mAerServInterstitial, AerServRewardedListener.this);
                                IMAudienceBidder.IMABRewardedWrapper bidObject = new IMAudienceBidder.IMABRewardedWrapper(bidKeyword, mediationSettings);
                                mAudienceBidderListener.onBidRecieved(bidObject);
                            } else {
                                Error error = new Error("Something when wrong with getting the bid.");
                                mAudienceBidderListener.onBidFailed(new IMAudienceBidder.IMABRewardedWrapper(null, null), error);
                            }
                        }
                    });
                    break;
                case VC_REWARDED:
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (args.get(0) instanceof AerServVirtualCurrency) {
                                final AerServVirtualCurrency vc = (AerServVirtualCurrency) args.get(0);
                                if (vc.getAmount() != null) {
                                    MoPubRewardedVideoManager.onRewardedVideoCompleted(IMABCustomEventRewarded.class,
                                            mPlacement, MoPubReward.success(vc.getName(), vc.getAmount().intValueExact()));
                                }
                            }
                        }
                    });
                    break;
            }
        }
    }
}