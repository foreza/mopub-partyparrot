package com.inmobi.plugin.mopub;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.view.View;

import com.aerserv.sdk.AerServBanner;
import com.aerserv.sdk.AerServEvent;
import com.aerserv.sdk.AerServEventListener;
import com.aerserv.sdk.AerServTransactionInformation;
import com.mopub.mobileads.CustomEventBanner;
import com.mopub.mobileads.MoPubErrorCode;
import com.mopub.mobileads.MoPubView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IMABCustomEventBanner extends CustomEventBanner {

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

        if (!localExtras.containsKey(IMAudienceBidder.LISTENER_KEY)) {
            customEventBannerListener.onBannerFailed(MoPubErrorCode.NETWORK_INVALID_STATE);
            return;
        }
        Object adListener = localExtras.get(IMAudienceBidder.LISTENER_KEY);
        if (!(adListener instanceof AerServBannerListener)) {
            customEventBannerListener.onBannerFailed(MoPubErrorCode.NETWORK_INVALID_STATE);
        } else {
            ((AerServBannerListener) adListener).setCustomEventBannerListener(customEventBannerListener);
        }

        if (!localExtras.containsKey(IMAudienceBidder.AD_KEY)) {
            customEventBannerListener.onBannerFailed(MoPubErrorCode.NETWORK_NO_FILL);
        }
        Object adObect = localExtras.get(IMAudienceBidder.AD_KEY);
        if (adObect instanceof AerServBanner) {
            aerServBanner = (AerServBanner) adObect;
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
            customEventBannerListener.onBannerLoaded(aerServBanner);
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

        @NonNull
        private final MoPubView mAdView;
        @NonNull
        private final IMAudienceBidder.IMAudienceBidderBannerListener mAudienceBidderListener;

        AerServBannerListener(@NonNull MoPubView moPubView,
                              @NonNull IMAudienceBidder.IMAudienceBidderBannerListener audienceBidderListener) {
            mAdView = moPubView;
            mAudienceBidderListener = audienceBidderListener;
        }

        void setCustomEventBannerListener(@NonNull CustomEventBannerListener customEventBannerListener) {
            mBannerListener = customEventBannerListener;
        }

        void setAerServBanner(@NonNull AerServBanner aerServBanner) {
            mAerServBanner = aerServBanner;
        }

        @Override
        public void onAerServEvent(AerServEvent aerServEvent, final List<Object> list) {
            switch(aerServEvent) {
                case VC_READY:
                    break;
                case PRELOAD_READY:
                    break;
                case AD_LOADED:
                    break;
                case AD_EXPANDED:
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (mBannerListener != null) {
                                mBannerListener.onBannerExpanded();
                            }
                        }
                    });
                    break;
                case AD_COLLAPSED:
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (mBannerListener != null) {
                                mBannerListener.onBannerCollapsed();
                            }
                        }
                    });
                    break;
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
                case AD_IMPRESSION:
                    Map<String, Object> localExtras = new HashMap<>(mAdView.getLocalExtras());
                    localExtras.remove(IMAudienceBidder.AD_KEY);
                    localExtras.remove(IMAudienceBidder.LISTENER_KEY);
                    mAdView.setLocalExtras(localExtras);
                    String keywords = mAdView.getKeywords();
                    mAdView.setKeywords(IMAudienceBidder.removeIMKeyword(keywords));
                    break;
                case AD_FAILED:
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            Map<String, Object> localExtras = new HashMap<>(mAdView.getLocalExtras());
                            localExtras.remove(IMAudienceBidder.AD_KEY);
                            localExtras.remove(IMAudienceBidder.LISTENER_KEY);
                            mAdView.setLocalExtras(localExtras);
                            if (mBannerListener != null) {
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
                            if (mAerServBanner != null) {
                                Map<String, Object> localExtras = new HashMap<>(mAdView.getLocalExtras());
                                localExtras.put(IMAudienceBidder.AD_KEY, mAerServBanner);
                                localExtras.put(IMAudienceBidder.LISTENER_KEY, AerServBannerListener.this);
                                mAdView.setLocalExtras(localExtras);

                                AerServTransactionInformation transactionInformation = null;
                                if (list.get(0) instanceof AerServTransactionInformation) {
                                    transactionInformation = (AerServTransactionInformation) list.get(0);
                                }

                                if (transactionInformation != null) {
                                    String keyword = IMAudienceBidder.getBidKeyword(transactionInformation);
                                    String oldKeyword = mAdView.getKeywords();
                                    if (!TextUtils.isEmpty(oldKeyword)) {
                                        mAdView.setKeywords(String.format("%s,%s", oldKeyword, keyword));
                                    } else {
                                        mAdView.setKeywords(keyword);
                                    }
                                }
                                mAudienceBidderListener.onBidRecieved(mAdView);
                            } else {
                                Error error=  new Error("Something when wrong with getting the bid.");
                                mAudienceBidderListener.onBidFailed(mAdView, error);
                            }
                        }
                    });
                    break;
                case SHOW_TRANSACTION:
                    break;
            }
        }
    }
}