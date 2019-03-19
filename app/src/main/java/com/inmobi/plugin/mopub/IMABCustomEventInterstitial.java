package com.inmobi.plugin.mopub;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.aerserv.sdk.AerServEvent;
import com.aerserv.sdk.AerServEventListener;
import com.aerserv.sdk.AerServInterstitial;
import com.aerserv.sdk.AerServTransactionInformation;
import com.mopub.mobileads.CustomEventInterstitial;
import com.mopub.mobileads.MoPubErrorCode;
import com.mopub.mobileads.MoPubInterstitial;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IMABCustomEventInterstitial extends CustomEventInterstitial {

    private AerServInterstitial aerServInterstitial = null;

    @Override
    protected void loadInterstitial(final Context context,
                                    final CustomEventInterstitialListener customEventInterstitialListener,
                                    final Map<String, Object> localExtras,
                                    final Map<String, String> serverExtras) {
        // Error checking
        if (customEventInterstitialListener == null) {
            throw new IllegalArgumentException("CustomEventInterstitialListener cannot be null");
        }

        if (context == null) {
            customEventInterstitialListener.onInterstitialFailed(MoPubErrorCode.NETWORK_INVALID_STATE);
            return;
        }

        if (localExtras == null) {
            customEventInterstitialListener.onInterstitialFailed(MoPubErrorCode.NETWORK_INVALID_STATE);
            return;
        }

        if (!localExtras.containsKey(IMAudienceBidder.LISTENER_KEY)) {
            customEventInterstitialListener.onInterstitialFailed(MoPubErrorCode.NETWORK_INVALID_STATE);
            return;
        }
        Object adListener = localExtras.get(IMAudienceBidder.LISTENER_KEY);
        if (!(adListener instanceof AerServInterstitialListener)) {
            customEventInterstitialListener.onInterstitialFailed(MoPubErrorCode.NETWORK_INVALID_STATE);
        } else {
            ((AerServInterstitialListener) adListener).setCustomEventInterstitialListener(customEventInterstitialListener);
        }

        if (!localExtras.containsKey(IMAudienceBidder.AD_KEY)) {
            customEventInterstitialListener.onInterstitialFailed(MoPubErrorCode.NETWORK_NO_FILL);
        }
        Object adObect = localExtras.get(IMAudienceBidder.AD_KEY);
        if (adObect instanceof AerServInterstitial) {
            aerServInterstitial = (AerServInterstitial) adObect;
            customEventInterstitialListener.onInterstitialLoaded();
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

        @NonNull
        private final MoPubInterstitial mMoPubInterstitial;
        @NonNull
        private final IMAudienceBidder.IMAudienceBidderInterstitialListener mAudienceBidderListener;

        AerServInterstitialListener(@NonNull MoPubInterstitial moPubInterstitial,
                                    @NonNull IMAudienceBidder.IMAudienceBidderInterstitialListener listener) {
            mMoPubInterstitial = moPubInterstitial;
            mAudienceBidderListener = listener;
        }

        void setCustomEventInterstitialListener(@NonNull CustomEventInterstitialListener customEventInterstitialListener) {
            mInterstitialListener = customEventInterstitialListener;
        }

        void setAerServInterstitial(@NonNull AerServInterstitial aerServInterstitial) {
            mAerServInterstitial = aerServInterstitial;
        }

        @Override
        public void onAerServEvent(AerServEvent aerServEvent, final List<Object> list) {
            switch (aerServEvent) {
                case VC_READY:
                    break;
                case PRELOAD_READY:
                    break;
                case AD_LOADED:
                    break;
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
                            localExtras.remove(IMAudienceBidder.LISTENER_KEY);
                            mMoPubInterstitial.setLocalExtras(localExtras);
                            String keywords = mMoPubInterstitial.getKeywords();
                            mMoPubInterstitial.setKeywords(IMAudienceBidder.removeIMKeyword(keywords));
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
                            Map<String, Object> localExtras = new HashMap<>(mMoPubInterstitial.getLocalExtras());
                            localExtras.remove(IMAudienceBidder.AD_KEY);
                            localExtras.remove(IMAudienceBidder.LISTENER_KEY);
                            mMoPubInterstitial.setLocalExtras(localExtras);
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
                            if (mAerServInterstitial != null) {
                                Map<String, Object> localExtras = new HashMap<>(mMoPubInterstitial.getLocalExtras());
                                localExtras.put(IMAudienceBidder.AD_KEY, mAerServInterstitial);
                                localExtras.put(IMAudienceBidder.LISTENER_KEY, AerServInterstitialListener.this);
                                mMoPubInterstitial.setLocalExtras(localExtras);

                                AerServTransactionInformation transactionInformation = null;
                                if (list.get(0) instanceof AerServTransactionInformation) {
                                    transactionInformation = (AerServTransactionInformation) list.get(0);
                                }

                                if (transactionInformation != null) {
                                    String keyword = IMAudienceBidder.getBidKeyword(transactionInformation);
                                    String oldKeyword = mMoPubInterstitial.getKeywords();
                                    if (!TextUtils.isEmpty(oldKeyword)) {
                                        mMoPubInterstitial.setKeywords(String.format("%s,%s", oldKeyword, keyword));
                                    } else {
                                        mMoPubInterstitial.setKeywords(keyword);
                                    }
                                }
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        mAudienceBidderListener.onBidRecieved(mMoPubInterstitial);
                                    }
                                });
                            } else {
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        Error error=  new Error("Something when wrong with getting the bid.");
                                        mAudienceBidderListener.onBidFailed(mMoPubInterstitial, error);
                                    }
                                });
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
            }
        }
    }
}
