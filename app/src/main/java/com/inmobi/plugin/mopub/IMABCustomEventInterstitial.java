package com.inmobi.plugin.mopub;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.aerserv.sdk.AerServEvent;
import com.aerserv.sdk.AerServEventListener;
import com.aerserv.sdk.AerServInterstitial;
import com.aerserv.sdk.AerServTransactionInformation;
import com.inmobi.ads.core.AdTypes;
import com.mopub.mobileads.CustomEventInterstitial;
import com.mopub.mobileads.MoPubErrorCode;
import com.mopub.mobileads.MoPubInterstitial;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;

public class IMABCustomEventInterstitial extends CustomEventInterstitial {

    @NonNull
    static final Map<String, AerServInterstitialListener> listenerMap = new HashMap<>();

    private AerServInterstitial aerServInterstitial = null;

    @Override
    protected void loadInterstitial(final Context context,
                                    final CustomEventInterstitialListener customEventInterstitialListener,
                                    final Map<String, Object> localExtras,
                                    final Map<String, String> serverExtras) {
        // Error checking
        if(context == null) {
            customEventInterstitialListener.onInterstitialFailed(MoPubErrorCode.NETWORK_INVALID_STATE);
            return;
        }

        if (localExtras == null) {
            customEventInterstitialListener.onInterstitialFailed(MoPubErrorCode.NETWORK_INVALID_STATE);
            return;
        }

        Object placement = localExtras.get(IMAudienceBidder.AD_KEY);
        if (!(placement instanceof String)) {
            customEventInterstitialListener.onInterstitialFailed(MoPubErrorCode.NETWORK_INVALID_STATE);
            return;
        }

        AerServInterstitialListener adListener = listenerMap.get(placement);
        if (adListener == null) {
            customEventInterstitialListener.onInterstitialFailed(MoPubErrorCode.NETWORK_INVALID_STATE);
            return;
        }
        adListener.setCustomEventInterstitialListener(customEventInterstitialListener);
        aerServInterstitial = adListener.getAerservInterstitial();
        listenerMap.remove(placement);

        if (aerServInterstitial != null) {
            if (adListener.hasFiredLoaded) {
                customEventInterstitialListener.onInterstitialLoaded();
            }
            adListener.hasFiredLoaded = true;
        } else {
            customEventInterstitialListener.onInterstitialFailed(MoPubErrorCode.NETWORK_NO_FILL);
        }

    }

    @Override
    protected void onInvalidate() {
        if(aerServInterstitial != null) {
            aerServInterstitial.kill();
            aerServInterstitial = null;
        }
    }

    @Override
    protected void showInterstitial() {
        if (aerServInterstitial != null) {
            aerServInterstitial.show();
        }
    }

    public static class AerServInterstitialListener implements AerServEventListener {

        private static final Handler handler = new Handler(Looper.getMainLooper());

        private AerServInterstitial mAerServInterstitial = null;
        private CustomEventInterstitialListener mInterstitialListener = null;

        boolean hasFiredLoaded = false;

        @NonNull
        private final String mPlacement;
        @NonNull
        private final MoPubInterstitial mMoPubInterstitial;
        @NonNull
        private final IMAudienceBidder.IMAudienceBidderInterstitialListener mAudienceBidderListener;
        @Nullable
        private final Timer mTask;

        AerServInterstitialListener(@NonNull MoPubInterstitial moPubInterstitial, @NonNull String placement,
                                    @NonNull IMAudienceBidder.IMAudienceBidderInterstitialListener listener, @Nullable Timer timeoutTimer) {
            mMoPubInterstitial = moPubInterstitial;
            mPlacement = placement;
            mAudienceBidderListener = listener;
            mTask = timeoutTimer;
        }

        void setCustomEventInterstitialListener(@NonNull CustomEventInterstitialListener customEventInterstitialListener) {
            mInterstitialListener = customEventInterstitialListener;
        }

        void setAerServInterstitial(@NonNull AerServInterstitial aerServInterstitial) {
            mAerServInterstitial = aerServInterstitial;
        }

        @Nullable
        AerServInterstitial getAerservInterstitial() {
            return mAerServInterstitial;
        }

        @Override
        public void onAerServEvent(AerServEvent aerServEvent, final List<Object> list) {
            switch (aerServEvent) {
                case AD_CLICKED:
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (mInterstitialListener != null) {
                                mInterstitialListener.onInterstitialClicked();
                            }
                        }
                    });
                    break;
                case AD_DISMISSED:
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (mInterstitialListener != null) {
                                mInterstitialListener.onInterstitialDismissed();
                            }
                        }
                    });
                    break;
                case AD_IMPRESSION:
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            Map<String, Object> localExtras = new HashMap<>(mMoPubInterstitial.getLocalExtras());
                            localExtras.remove(IMAudienceBidder.AD_KEY);
                            mMoPubInterstitial.setLocalExtras(localExtras);
                            String keywords = mMoPubInterstitial.getKeywords();
                            mMoPubInterstitial.setKeywords(IMAudienceBidder.removeIMKeywords(keywords));
                            if (mInterstitialListener != null) {
                                mInterstitialListener.onInterstitialImpression();
                            }

                        }
                    });
                    break;
                case AD_FAILED:
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (mTask != null) {
                                mTask.cancel();
                            }

                            listenerMap.remove(mPlacement);

                            if (mInterstitialListener != null) {
                                mInterstitialListener.onInterstitialFailed(MoPubErrorCode.NETWORK_NO_FILL);
                            } else {
                                Error error = new Error("Failed to load MoPub and the corresponding bid price");
                                mAudienceBidderListener.onBidFailed(mMoPubInterstitial, error);
                            }
                        }
                    });
                    break;
                case LOAD_TRANSACTION:
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (mTask != null) {
                                mTask.cancel();
                            }

                            String oldkeyword = mMoPubInterstitial.getKeywords();

                            if (mAerServInterstitial != null) {
                                listenerMap.put(mPlacement, AerServInterstitialListener.this);

                                AerServTransactionInformation transactionInformation = null;
                                if (list.get(0) instanceof AerServTransactionInformation) {
                                    transactionInformation = (AerServTransactionInformation) list.get(0);
                                }

                                if (transactionInformation != null) {
                                    String keyword = IMAudienceBidder.getBidKeyword(transactionInformation.getBuyerPrice().doubleValue(), AdTypes.INT);
                                    String granularKeyword = IMAudienceBidder.getGranularBidKeyword(transactionInformation.getBuyerPrice().doubleValue(), AdTypes.INT);
                                    if (TextUtils.isEmpty(oldkeyword)) {
                                        mMoPubInterstitial.setKeywords(keyword.equals(granularKeyword) ? String.format("%s", keyword) : String.format("%s,%s", keyword, granularKeyword));
                                    } else {
                                        mMoPubInterstitial.setKeywords(keyword.equals(granularKeyword) ? String.format("%s,%s", oldkeyword, keyword) : String.format("%s,%s,%s", oldkeyword, keyword, granularKeyword));
                                    }
                                }

                                mAudienceBidderListener.onBidRecieved(mMoPubInterstitial);
                                if (mInterstitialListener != null) {
                                    mInterstitialListener.onInterstitialLoaded();
                                }
                                hasFiredLoaded = true;
                            } else {
                                Error error = new Error("Something when wrong with getting the bid.");
                                mAudienceBidderListener.onBidFailed(mMoPubInterstitial, error);
                            }
                        }
                    });
                    break;
                case SHOW_TRANSACTION:
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (mInterstitialListener != null) {
                                mInterstitialListener.onInterstitialShown();
                            }
                        }
                    });
                    break;
            }
        }
    }
}
