package com.mopub.mobileads;

import android.content.Context;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.aerserv.sdk.AerServBanner;
import com.aerserv.sdk.AerServConfig;
import com.aerserv.sdk.AerServEvent;
import com.aerserv.sdk.AerServEventListener;
import com.mopub.common.util.Views;

import java.util.List;
import java.util.Map;

public class AerServCustomEventBanner extends CustomEventBanner {

    private static final String LOG_TAG = AerServCustomEventBanner.class.getSimpleName();
    private static final String PLACEMENT = "placement";

    private AerServBanner aerServBanner = null;
    private CustomEventBannerListener mBannerListener = null;

    private View.OnAttachStateChangeListener attachStateChangeListener = null;
    private String placement;

    @Override
    protected void loadBanner(final Context context,
                              final CustomEventBannerListener customEventBannerListener,
                              final Map<String, Object> localExtras,
                              final Map<String, String> serverExtras) {
        // Error checking
        if(context == null) {
            throw new IllegalArgumentException("Context cannot be null");
        }
        if (customEventBannerListener == null) {
            throw new IllegalArgumentException("CustomEventBannerListener cannot be null");
        }

        // Get placement
        mBannerListener = customEventBannerListener;
        placement = AerServPluginUtil.getString(PLACEMENT, localExtras, serverExtras);
        if (placement == null) {
            Log.w(LOG_TAG, "Cannot load AerServ ad because placement is missing");
            customEventBannerListener.onBannerFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            return;
        }
        Log.d(LOG_TAG, "Placement is: " + placement);

        AerServBidder aerServBidder = AerServBidder.getInstance();

        aerServBanner = (AerServBanner) aerServBidder.getAdForPlacement(placement);
        if( aerServBanner != null){
            Log.d(LOG_TAG,"Aerserv banner ad already loaded, skipping the loading part");
            aerServBidder.setListenerForPlacement(placement, mBannerListener);
            mBannerListener.onBannerLoaded(aerServBanner);
            aerServBanner.show();
        }
        else{
            aerServBanner = new AerServBanner(context);

            // Read and configure optional properties
            AerServConfig aerServConfig = new AerServConfig(context, placement)
                    .setPreload(true);

            aerServConfig.setRefreshInterval(0);

            // Map AerServ ad events to MoPub ad events
            aerServConfig.setEventListener(new AerServEventListener() {
                @Override
                public void onAerServEvent(final AerServEvent aerServEvent, List<Object> list) {
                    Handler handler = new Handler(context.getMainLooper());
                    Runnable runnable = new Runnable() {
                        @Override
                        public void run() {
                            switch (aerServEvent) {
                                case PRELOAD_READY:
                                    Log.d(LOG_TAG, "Banner ad loaded");
                                    mBannerListener.onBannerLoaded(aerServBanner);
                                    break;
                                case AD_FAILED:
                                    Log.d(LOG_TAG, "Failed to load banner ad");
                                    mBannerListener.onBannerFailed(MoPubErrorCode.NETWORK_NO_FILL);
                                    break;
                                case AD_CLICKED:
                                    Log.d(LOG_TAG, "Banner ad clicked");
                                    mBannerListener.onBannerClicked();
                                    break;
                                default:
                                    Log.d(LOG_TAG, "The following AerServ banner ad event cannot be mapped ");
                                    break;
                            }
                        }
                    };
                    handler.post(runnable);
                }
            });

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

            aerServBanner.configure(aerServConfig);
            Log.d(LOG_TAG, "Loading banner ad");
        }

    }

    @Override
    protected void onInvalidate() {
        if (aerServBanner != null) {
            if (aerServBanner.getParent() != null) {
                Views.removeFromParent(aerServBanner);
            }

            Object savedInstance = AerServBidder.getInstance().getAdForPlacement(placement);

            if (!TextUtils.isEmpty(placement) && savedInstance instanceof AerServBanner &&
                    savedInstance.hashCode() == aerServBanner.hashCode()) {
                AerServBidder.getInstance().removeAdForPlacement(placement);
                AerServBidder.getInstance().removeListener(placement);
            }

            if (attachStateChangeListener != null) {
                aerServBanner.removeOnAttachStateChangeListener(attachStateChangeListener);
            }

            aerServBanner.kill();
            aerServBanner = null;
        }
    }
}