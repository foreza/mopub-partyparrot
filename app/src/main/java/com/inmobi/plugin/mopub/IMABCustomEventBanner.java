package com.inmobi.plugin.mopub;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.View;

import com.aerserv.sdk.AerServBanner;
import com.aerserv.sdk.AerServEvent;
import com.aerserv.sdk.AerServEventListener;
import com.aerserv.sdk.AerServTransactionInformation;
import com.inmobi.ads.core.AdTypes;
import com.mopub.mobileads.CustomEventBanner;
import com.mopub.mobileads.MoPubErrorCode;
import com.mopub.mobileads.MoPubView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;

public class IMABCustomEventBanner extends CustomEventBanner {

    @NonNull
    static final Map<String, AerServBannerListener> listenerMap = new HashMap<>();

    private AerServBanner aerServBanner = null;
    private View.OnAttachStateChangeListener attachStateChangeListener = null;

    @Override
    protected void loadBanner(final Context context,
                              final CustomEventBannerListener customEventBannerListener,
                              final Map<String, Object> localExtras,
                              final Map<String, String> serverExtras) {
        if (customEventBannerListener == null) {
            throw new IllegalArgumentException("CustomEventBannerListener cannot be null");
        }

        // Error checking
        if(context == null) {
            customEventBannerListener.onBannerFailed(MoPubErrorCode.NETWORK_INVALID_STATE);
            return;
        }

        if (localExtras == null) {
            customEventBannerListener.onBannerFailed(MoPubErrorCode.NETWORK_INVALID_STATE);
            return;
        }

        Object placement = localExtras.get(IMAudienceBidder.AD_KEY);
        if (!(placement instanceof String)) {
            customEventBannerListener.onBannerFailed(MoPubErrorCode.NETWORK_INVALID_STATE);
            return;
        }

        AerServBannerListener adListener = listenerMap.get(placement);
        if (adListener == null) {
            customEventBannerListener.onBannerFailed(MoPubErrorCode.NETWORK_INVALID_STATE);
            return;
        }
        adListener.setCustomEventBannerListener(customEventBannerListener);
        aerServBanner = adListener.getAerServBanner();
        listenerMap.remove(placement);

        MoPubView moPubView = adListener.getMoPubView();
        String keywords = IMAudienceBidder.removeIMKeywords(moPubView.getKeywords());
        moPubView.setKeywords(keywords);

        if (aerServBanner != null) {
            attachStateChangeListener =  new View.OnAttachStateChangeListener() {
                @Override
                public void onViewAttachedToWindow(View v) {
                    if (attachStateChangeListener != null) {
                        aerServBanner.removeOnAttachStateChangeListener(attachStateChangeListener);
                    }
                    attachStateChangeListener = null;
                    aerServBanner.show();
                }

                @Override
                public void onViewDetachedFromWindow(View v) {
                    if (attachStateChangeListener != null) {
                        aerServBanner.removeOnAttachStateChangeListener(attachStateChangeListener);
                    }
                    attachStateChangeListener = null;
                }
            };
            aerServBanner.addOnAttachStateChangeListener(attachStateChangeListener);
            if (adListener.hasFiredLoaded) {
                customEventBannerListener.onBannerLoaded(aerServBanner);
            }
            adListener.hasFiredLoaded = true;
        } else {
            customEventBannerListener.onBannerFailed(MoPubErrorCode.NETWORK_NO_FILL);
        }
    }

    @Override
    protected void onInvalidate() {
        if (aerServBanner != null) {
            aerServBanner.kill();
            aerServBanner = null;
        }
    }

    public static class AerServBannerListener implements AerServEventListener {

        private final static Handler handler = new Handler(Looper.getMainLooper());

        private AerServBanner mAerServBanner = null;
        private CustomEventBannerListener mBannerListener = null;

        boolean hasFiredLoaded = false;

        @NonNull
        private final String mPlacement;
        @NonNull
        private final MoPubView mAdView;
        @NonNull
        private final IMAudienceBidder.IMAudienceBidderBannerListener mAudienceBidderListener;
        @Nullable
        private final Timer mTimer;


        AerServBannerListener(@NonNull MoPubView moPubView, @NonNull String placement,
                              @NonNull IMAudienceBidder.IMAudienceBidderBannerListener audienceBidderListener,
                              @Nullable Timer timeoutTimer) {
            mAdView = moPubView;
            mAudienceBidderListener = audienceBidderListener;
            mPlacement = placement;
            mTimer = timeoutTimer;
        }

        void setCustomEventBannerListener(@NonNull CustomEventBannerListener customEventBannerListener) {
            mBannerListener = customEventBannerListener;
        }

        void setAerServBanner(@NonNull AerServBanner aerServBanner) {
            mAerServBanner = aerServBanner;
        }

        @NonNull
        MoPubView getMoPubView() {
            return mAdView;
        }

        @Nullable
        AerServBanner getAerServBanner() {
            return mAerServBanner;
        }

        @Override
        public void onAerServEvent(final AerServEvent aerServEvent, final List<Object> list) {
            switch (aerServEvent) {
                case AD_CLICKED:
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (mBannerListener != null) {
                                mBannerListener.onBannerClicked();
                            }
                        }
                    });
                    break;
                case AD_DISMISSED:
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (mBannerListener != null) {
                                mBannerListener.onBannerCollapsed();
                            }
                        }
                    });
                    break;
                case AD_FAILED:
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (mTimer != null) {
                                mTimer.cancel();
                            }

                            listenerMap.remove(mPlacement);

                            String oldKeyword = mAdView.getKeywords();
                            mAdView.setKeywords(IMAudienceBidder.removeIMKeywords(oldKeyword));

                            if (mBannerListener != null && !hasFiredLoaded) {
                                mBannerListener.onBannerFailed(MoPubErrorCode.NETWORK_NO_FILL);
                            } else {
                                Error error = new Error("Failed to load MoPub and the corresponding bid price");
                                mAudienceBidderListener.onBidFailed(mAdView, error);
                            }
                        }
                    });
                    break;
                case LOAD_TRANSACTION:
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (mTimer != null) {
                                mTimer.cancel();
                            }

                            String oldkeyword = mAdView.getKeywords();

                            if (mAerServBanner != null) {
                                AerServTransactionInformation transactionInformation = null;
                                if (list.get(0) instanceof AerServTransactionInformation) {
                                    transactionInformation = (AerServTransactionInformation) list.get(0);
                                }

                                if (transactionInformation != null) {
                                    String keyword = IMAudienceBidder.getBidKeyword(transactionInformation.getBuyerPrice().doubleValue(), AdTypes.BANNER);
                                    String granularKeyword = IMAudienceBidder.getGranularBidKeyword(transactionInformation.getBuyerPrice().doubleValue(), AdTypes.BANNER);
                                    if (TextUtils.isEmpty(oldkeyword)) {
                                        mAdView.setKeywords(keyword.equals(granularKeyword) ? String.format("%s", keyword) : String.format("%s,%s", keyword, granularKeyword));
                                    } else {
                                        mAdView.setKeywords(keyword.equals(granularKeyword) ? String.format("%s,%s", oldkeyword, keyword) : String.format("%s,%s,%s", oldkeyword, keyword, granularKeyword));
                                    }
                                }

                                mAudienceBidderListener.onBidRecieved(mAdView);
                                if (mBannerListener != null) {
                                    mBannerListener.onBannerLoaded(mAerServBanner);
                                }
                                hasFiredLoaded = true;
                            } else {
                                Error error = new Error("Something when wrong with getting the bid.");
                                mAudienceBidderListener.onBidFailed(mAdView, error);
                            }
                        }
                    });
                    break;
            }
        }
    }
}