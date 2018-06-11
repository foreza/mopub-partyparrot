package com.mopub.mobileads;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.aerserv.sdk.AerServConfig;
import com.aerserv.sdk.AerServEvent;
import com.aerserv.sdk.AerServEventListener;
import com.aerserv.sdk.AerServInterstitial;
import com.aerserv.sdk.AerServSdk;
import com.aerserv.sdk.AerServVirtualCurrency;
import com.mopub.common.BaseLifecycleListener;
import com.mopub.common.LifecycleListener;
import com.mopub.common.MoPubReward;

import java.util.List;
import java.util.Map;
/*
* Implementation example of an adapter plug-in that will work with AerServMarket. It requires set-up
* on the server-side of MoPub to call the method `loadCustomEvent`
* from the class `com.mopub.mobileads.AerServCustomEventRewardedInterstitial`, which intiializes AerServ's
* SDK and sets it up with settings from a customized JSON object e.g. '{"placement", "Placement ID here"}'
* among other additional configuration changes, including timeout and keywords set up on MoPub's platform.
* This class is required to be part of the com.mopub.mobileads package in order to set up
* calls from MoPub's SDK to ours. This requires AerServPluginUtil to be in the same package too.
*
* In order to access AerServMarket, the publisher must set up a custom native network with
* MoPub, passing in our given PLC value from AerServ's platform, desired keywords if desired,
* and desired timeout in a JSON-formatted string e.g. '{"placement":"insert-12345-here', "timeoutMillis":"5000", "keywords":["arg1","arg2"]}'.
* By having their SDK call on this class through `loadCustomEvent`, the necessary listener events will be
* triggered by MoPub's SDK in order to load our content following the logic dictated in this file.
* In order to change the behavior of this plugin's response to AerServ's SDK, one can change the AerServCustomEventRewardedInterstitial
* class to change what each event's behavior will be.
*/

public class AerServCustomEventRewardedInterstitial extends CustomEventRewardedVideo {
    private String LOG_TAG = AerServCustomEventRewardedInterstitial.class.getSimpleName();

    /**
     * Some instances of acceptable properties that can be passed from the JSON-formatted string set up
     * on MoPub's end. The key-value's key are arbitrarily defined, but must be kept consistent.
     */
    private static final String KEYWORDS = "keywords";
    private static final String PLACEMENT = "placement";
    private static final String TIMEOUT_MILLIS = "timeoutMillis";
    private static final String SITE_ID = "siteId";
    private static final String REWARDED_VIDEO_CURRENCY_NAME_KEY = "Rewarded-Video-Currency-Name";
    private static final String REWARDED_VIDEO_CURRENCY_VALUE_KEY = "Rewarded-Video-Currency-Value-String";

    /**
     *  Required state to maintain this class. PlacementId cannot be null since AerServ requires it
     *  to identify ad unit. MoPub defaults to using its own virtual currency if set up on their network,
     *  else it takes the returned reward from this rewarded class.
     */
    private AerServInterstitial aerServInterstitial = null;
    private LifecycleListener aerServLifeCycleListener = null;
    private String placementId = null;
    private boolean isLoaded = false;
    private boolean isInitialized = false;

    /*  Deprecated in MoPub SDK v4.12. */
    private CustomEventRewardedVideoListener aerServListener = null;

    /* For internal consistency measures. */
    private static final Object lock = new Object();

    /**
     * MoPub's access to AerServ SDK's listener that needs to be callable for registering events.
     *
     * @return CustomEventRewardedVideoListener implements AerServListener and CustomEventRewardedVideoListener
     */
    @Nullable
    @Override
    protected CustomEventRewardedVideoListener getVideoListenerForSdk() {
        return aerServListener;
    }

    /**
     * AerServ supports pause/play capabilities that should occur when the view is out of sight.
     * For a better idea of what should be supported, see the life cycle of a view from Android's doc
     * @see <a href="https://developer.android.com/guide/components/activities/activity-lifecycle.html">
     * https://support.aerserv.com/hc/en-us/articles/204159160-Android-SDK-Integration-Version-2-40-4</a>
     *
     * @return LifecycleListener is a base lifecyclelistener defined by MoPub with calls for specific Aerserv features
     */
    @Nullable
    @Override
    protected LifecycleListener getLifecycleListener() {
        return aerServLifeCycleListener;
    }

    /**
     * The key-value required for our end in order to access an advertisement.
     *
     * @return String a required key for this ad network
     */
    @NonNull
    @Override
    protected String getAdNetworkId() { return placementId; }

    /**
     * Required for clean-up on any instances of MoPub's rewarded video.
     */
    @Override
    protected void onInvalidate() {
        if (aerServInterstitial != null) {
            aerServInterstitial.kill();
            aerServInterstitial = null;
        }
    }

    /**
     * Check to make sure that an instance has not already been instantiated. If it has not, then create
     * a life cycle listener and assign the static instance to instance.
     *
     * @param activity the current activity to run the displayed ad
     * @param localExtras necessary key-value pairs listed in JSON-format for set-up
     * @param serverExtras desired key-value pairs
     *
     * @return boolean whether or not the sdk needs to setup life cycle listeners for this SDK
     */
    @Override
    protected boolean checkAndInitializeSdk(@NonNull Activity activity,
                                            @NonNull Map<String, Object> localExtras,
                                            @NonNull Map<String, String> serverExtras) throws Exception {
        synchronized (lock) {
            if (isInitialized) {
                return false;
            }

            aerServLifeCycleListener = createLifeCycleListener();
            isInitialized = true;
        }
        return true;
    }

    /**
     * Method to re-configure the existing instance of AerServ's SDK for each usage to get an ad. If any
     * properties need to be changed, then the json object must be changed on MoPub's network.
     *
     * @param activity the existing activity that will display the ad
     * @param localExtras the necessary key-value pairs required for this SDK
     * @param serverExtras key-value pairs needed for this SDK
     */
    @Override
    protected void loadWithSdkInitialized(@NonNull Activity activity,
                                          @NonNull Map<String, Object> localExtras,
                                          @NonNull Map<String, String> serverExtras) throws Exception {
        synchronized(lock) {
            isLoaded = false;
            if (activity == null) {
                throw new IllegalArgumentException("Activity cannot be null.");
            }
            placementId = AerServPluginUtil.getString(PLACEMENT,
                    localExtras, serverExtras);

            if (placementId == null) {
                throw new IllegalArgumentException("Placement ID cannot be null.");
            }
            Log.d(LOG_TAG, "Placement ID is: " + placementId);

            AerServConfig aerServConfig = new AerServConfig(activity, placementId);
            Integer timeoutMillis = AerServPluginUtil.getInteger(TIMEOUT_MILLIS,
                    localExtras, serverExtras);

            if (timeoutMillis != null) {
                Log.d(LOG_TAG, "Timeout is set to " + timeoutMillis + " ms.");
                aerServConfig.setTimeout(timeoutMillis);
            }

            List<String> keywords = AerServPluginUtil.getStringList(KEYWORDS,
                    localExtras,
                    serverExtras);
            if (keywords != null) {
                Log.d(LOG_TAG, "Keywords are set to " + keywords.toString());
                aerServConfig.setKeywords(keywords);
            }

            aerServListener = createAerServCustomEventRewardedInterstitialListener();
            aerServConfig
                    .setEventListener((AerServEventListener) aerServListener)
                    .setPreload(true);
            aerServInterstitial = new AerServInterstitial(aerServConfig);
        }
    }

    /**
     * Indicates to MoPub's SDK whether AerServ's SDK has prepared a video yet.
     *
     * @return boolean whether or not the rewardedInterstitial is ready
     */
    @Override
    protected boolean hasVideoAvailable() {
        return aerServInterstitial != null && isLoaded;
    }

    /**
     * Call to AerServ's SDK to display the currently loaded advertisement.
     */
    @Override
    protected void showVideo() {
        if (aerServInterstitial != null && isLoaded) {
            aerServInterstitial.show();
        }
    }

    /**
     * Creates a lifecycle listener for changes to different stages in the activity's life.
     *
     * @return LifecycleListener a listener to any events involving an activity's lifecycle
     */
    private LifecycleListener createLifeCycleListener() {
        return new BaseLifecycleListener() {

            @Override
            public void onPause(@NonNull Activity activity) {
                super.onPause(activity);
                if (aerServInterstitial != null) {
                    aerServInterstitial.pause();
                }
            }

            @Override
            public void onResume(@NonNull Activity activity) {
                super.onResume(activity);
                if (aerServInterstitial != null) {
                    aerServInterstitial.play();
                }
            }
        };
    }

    /**
     * Creates the AerServ event listener that will be called by MoPub's SDK to events involving this adunit.
     * Deprecated in MoPub SDK v4.12
     *
     * @return CustomEventRewardedVideoListener implements AerServListener and CustomEventRewardedVideoListener
     */
    private CustomEventRewardedVideoListener createAerServCustomEventRewardedInterstitialListener() {
        return new AerServCustomEventRewardedInterstitialListener();
    }

    /**
     * Implementation of a listener that will be called by MoPub's SDK in any events for a mediated adunit. This example
     * is sufficient for handling AerServ events, but may be customized to see any events for the publisher's needs i.e.
     * VIDEO_LOADED, VIDEO_25 and VIDEO_100 etc. CustomEventRewardVideoListener was deprecated in MoPub SDK v4.12
     *
     * See more:
     * @see <a href="https://support.aerserv.com/hc/en-us/articles/204159160-Android-SDK-Integration-Version-2-40-4">
     *     https://support.aerserv.com/hc/en-us/articles/204159160-Android-SDK-Integration-Version-2-40-4</a>
     */
    private class AerServCustomEventRewardedInterstitialListener implements AerServEventListener, CustomEventRewardedVideoListener {

        @Override
        public void onAerServEvent(AerServEvent aerServEvent, List<Object> args) {
            AerServVirtualCurrency vc;
            switch(aerServEvent) {
                case PRELOAD_READY:
                    Log.d(LOG_TAG, "Rewarded interstitial ad loaded.");
                    MoPubRewardedVideoManager.onRewardedVideoLoadSuccess(
                            AerServCustomEventRewardedInterstitial.class,
                            placementId);
                    isLoaded = true;
                    break;
                case VC_READY:
                    Log.d(LOG_TAG, "Rewarded Interstitial ad vc ready.");
                    vc = (AerServVirtualCurrency) args.get(0);
                    Log.d(LOG_TAG, "Virtual Currency PLC is ready: name="
                            + vc.getName()
                            + ", amount=" + vc.getAmount()
                            + ", buyerName=" + vc.getBuyerName()
                            + ", buyerPrice=" + vc.getBuyerPrice() + ".");
                    break;
                case AD_FAILED:
                    Log.d(LOG_TAG, "Failed to load rewarded interstitial ad");
                    MoPubRewardedVideoManager.onRewardedVideoLoadFailure(
                            AerServCustomEventRewardedInterstitial.class,
                            placementId
                            , MoPubErrorCode.NETWORK_NO_FILL);
                    break;
                case VIDEO_START:
                    Log.d(LOG_TAG, "Rewarded interstitial ad shown.");
                    MoPubRewardedVideoManager.onRewardedVideoStarted(
                            AerServCustomEventRewardedInterstitial.class
                            , placementId);
                    break;
                case AD_CLICKED:
                    Log.d(LOG_TAG, "Rewarded interstitial ad clicked.");
                    MoPubRewardedVideoManager.onRewardedVideoClicked(
                            AerServCustomEventRewardedInterstitial.class, placementId);
                    break;
                case AD_DISMISSED:
                    Log.d(LOG_TAG, "Rewarded interstitial ad dismissed.");
                    MoPubRewardedVideoManager.onRewardedVideoClosed(
                            AerServCustomEventRewardedInterstitial.class, placementId);
                    isLoaded = false;
                    break;
                case VC_REWARDED:
                    try {
                        vc = (AerServVirtualCurrency) args.get(0);
                        Log.d(LOG_TAG, "Virtual Currency PLC has been rewarded: name="
                                + vc.getName()
                                + ", amount=" + vc.getAmount()
                                + ", buyerName=" + vc.getBuyerName()
                                + ", buyerPrice=" + vc.getBuyerPrice() + ".");

                        MoPubRewardedVideoManager.onRewardedVideoCompleted(
                                AerServCustomEventRewardedInterstitial.class
                                , placementId
                                , MoPubReward.success(vc.getName(), vc.getAmount().intValueExact()));
                    } catch (ArithmeticException e) {
                        Log.e(LOG_TAG, "Caught an arithmetic exception.", e);
                        MoPubRewardedVideoManager.onRewardedVideoCompleted(
                                AerServCustomEventRewardedInterstitial.class
                                , placementId
                                , MoPubReward.success("", 0));
                    }

                    break;
                default:
                    Log.d(LOG_TAG, "The following AerServ interstitial ad event cannot be "
                            + "mapped and can be safely ignored: " + aerServEvent.name() + ".");
                    break;
            }
        }
    }
}