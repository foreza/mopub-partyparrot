package com.inmobi.plugin.mopub;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.aerserv.sdk.AerServBanner;
import com.aerserv.sdk.AerServConfig;
import com.aerserv.sdk.AerServInterstitial;
import com.aerserv.sdk.utils.ApsUtil;
import com.amazon.device.ads.DTBAdResponse;
import com.inmobi.ads.core.AdTypes;
import com.mopub.common.MediationSettings;
import com.mopub.common.logging.MoPubLog;
import com.mopub.mobileads.MoPubInterstitial;
import com.mopub.mobileads.MoPubView;

import java.util.Collections;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IMAudienceBidder {

    static final String AD_KEY = "IMABPlacement";

    public static class IMABRewardedWrapper {
        @Nullable
        private final String mKeyword;
        @Nullable
        private final MediationSettings mMediationSettings;

        IMABRewardedWrapper(@Nullable String keyword,
                            @Nullable MediationSettings mediationSettings) {
            mKeyword = keyword;
            mMediationSettings = mediationSettings;
        }

        @Nullable
        public String getKeyword() {
            return mKeyword;
        }

        @Nullable
        public MediationSettings getMediationSettings() {
            return mMediationSettings;
        }
    }

    static class IMAudienceBidderMediationSettings implements MediationSettings {

        @NonNull
        private final AerServInterstitial mAerServInterstitial;
        @NonNull
        private final IMABCustomEventRewarded.AerServRewardedListener mRewardedListener;
        @NonNull
        private final String mPlacement;

        IMAudienceBidderMediationSettings(@NonNull String placement,
                                          @NonNull AerServInterstitial aerServInterstitial,
                                          @NonNull IMABCustomEventRewarded.AerServRewardedListener rewardedListener) {
            mAerServInterstitial = aerServInterstitial;
            mPlacement = placement;
            mRewardedListener = rewardedListener;
        }

        @NonNull
        AerServInterstitial getAerServInterstitial() {
            return mAerServInterstitial;
        }

        @NonNull
        String getPlacement() {
            return mPlacement;
        }

        @NonNull
        IMABCustomEventRewarded.AerServRewardedListener getRewardedListener() {
            return mRewardedListener;
        }
    }

    public interface IMAudienceBidderInterstitialListener {
        void onBidRecieved(@NonNull MoPubInterstitial moPubInterstitial);
        void onBidFailed(@NonNull MoPubInterstitial moPubInterstitial, @NonNull Error error);
    }

    public interface IMAudienceBidderBannerListener {
        void onBidRecieved(@NonNull MoPubView moPubAdView);
        void onBidFailed(@NonNull MoPubView moPubAdView, @NonNull Error error);
    }

    public interface IMAudienceBidderRewardedListener {
        void onBidRecieved(@NonNull IMABRewardedWrapper wrapper);
        void onBidFailed(@NonNull IMABRewardedWrapper wrapper, @NonNull Error error);
    }

    private static class IMAudienceBidderLoader {
        static final IMAudienceBidder INSTANCE = new IMAudienceBidder();
    }

    public static IMAudienceBidder getInstance() {
        return IMAudienceBidderLoader.INSTANCE;
    }


    private final Handler handler;

    private IMAudienceBidder() {
        handler = new Handler(Looper.getMainLooper());
    }

    public interface BidToken {
        void refreshBid(@NonNull Context context, @Nullable DTBAdResponse dtbAdResponse, long timeoutMillis);
    }

    private static class InterstitialBidToken implements BidToken {

        private final MoPubInterstitial moPubInterstitial;
        private final String placement;
        private final IMAudienceBidderInterstitialListener bidderInterstitialListener;

        private InterstitialBidToken(@NonNull String placement, @NonNull MoPubInterstitial moPubInterstitial,
                                     @NonNull IMAudienceBidderInterstitialListener bidderInterstitialListener) {
            this.placement = placement;
            this.moPubInterstitial = moPubInterstitial;
            this.bidderInterstitialListener = bidderInterstitialListener;
        }

        /**
         * Object used to get an bid.
         * @param context Current context. Cannot be null.
         * @param dtbAdResponse Amazon's ad object with bid information. Can be null
         *         if APS did not return with an ad.
         * @param timeoutMillis Maximum time the listener has to return.
         */
        @Override
        public void refreshBid(@NonNull Context context, @Nullable final DTBAdResponse dtbAdResponse, long timeoutMillis) {

            if (TextUtils.isEmpty(placement)) {
                Error error = new Error("Cannot update MoPubInterstitial keywords with a empty or null placement.");
                bidderInterstitialListener.onBidFailed(moPubInterstitial, error);
                return;
            }

            Map<String, Object> tmpMap = new HashMap<>(moPubInterstitial.getLocalExtras());
            tmpMap.put(AD_KEY, placement);
            moPubInterstitial.setLocalExtras(tmpMap);

            String staleKeywordRemoved = removeIMKeywords(moPubInterstitial.getKeywords());
            moPubInterstitial.setKeywords(staleKeywordRemoved);

            boolean hasValidAps = false;

            final Double price = dtbAdResponse == null ? null : ApsUtil.getPrice(placement, dtbAdResponse);
            if (price != null) {
                String bidKeyword = getBidKeyword(price, AdTypes.INT);
                String granularBidKeywork = getGranularBidKeyword(price, AdTypes.INT);
                if (TextUtils.isEmpty(staleKeywordRemoved)) {
                    moPubInterstitial.setKeywords(bidKeyword.equals(granularBidKeywork) ? String.format("%s", bidKeyword) : String.format("%s,%s", bidKeyword, granularBidKeywork));
                } else {
                    moPubInterstitial.setKeywords(bidKeyword.equals(granularBidKeywork) ? String.format("%s,%s", staleKeywordRemoved, bidKeyword) : String.format("%s,%s,%s", staleKeywordRemoved, bidKeyword, granularBidKeywork));
                }
                hasValidAps = true;
            }

            final IMAudienceBidderInterstitialListener wrapper = new IMAudienceBidderInterstitialListener() {

                private IMAudienceBidderInterstitialListener bidderInterstitialListenerWrapper = InterstitialBidToken.this.bidderInterstitialListener;
                private boolean hasCallbackFired = false;

                @Override
                public void onBidRecieved(@NonNull MoPubInterstitial moPubInterstitial) {
                    if (!hasCallbackFired) {
                        hasCallbackFired = true;
                        bidderInterstitialListenerWrapper.onBidRecieved(moPubInterstitial);
                    }
                }

                @Override
                public void onBidFailed(@NonNull MoPubInterstitial moPubInterstitial, @NonNull Error error) {
                    if (!hasCallbackFired) {
                        hasCallbackFired = true;
                        bidderInterstitialListenerWrapper.onBidFailed(moPubInterstitial, error);
                    }
                }
            };

            Timer timer = null;
            if (timeoutMillis > 0) {
                timer = new Timer();
                TimerTask task = new TimerTask() {

                    @Override
                    public void run() {
                        IMAudienceBidder.getInstance().handler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (price != null) {
                                    wrapper.onBidRecieved(moPubInterstitial);
                                } else {
                                    Error error = new Error("No ad filled with IMAudienceBidder.");
                                    wrapper.onBidFailed(moPubInterstitial, error);
                                }
                            }
                        });
                    }
                };
                timer.schedule(task, timeoutMillis);
            } else if (timeoutMillis < 0) {
                IMAudienceBidder.getInstance().handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (price != null) {
                            wrapper.onBidRecieved(moPubInterstitial);
                        } else {
                            Error error = new Error("No ad filled with IMAudienceBidder.");
                            wrapper.onBidFailed(moPubInterstitial, error);
                        }
                    }
                });
                return;
            }

            MoPubLog.d("AudienceBidder: Attempting to add bid to MoPubInterstitial for placement: " + placement);

            IMABCustomEventInterstitial.AerServInterstitialListener aerServEventListener = new IMABCustomEventInterstitial.AerServInterstitialListener(moPubInterstitial, placement, wrapper, timer);
            IMABCustomEventInterstitial.listenerMap.put(placement, aerServEventListener);

            AerServConfig aerServConfig = new AerServConfig(context, placement)
                    .setPreload(true)
                    .setEventListener(aerServEventListener)
                    .setUseHeaderBidding(true);

            if (hasValidAps) {
                aerServConfig.setAPSAdResponses(Collections.singletonList(dtbAdResponse));
            }

            AerServInterstitial aerServInterstitial = new AerServInterstitial(aerServConfig);
            aerServEventListener.setAerServInterstitial(aerServInterstitial);
        }
    }

    private static class BannerBidToken implements BidToken {

        private final MoPubView moPubView;
        private final String placement;
        private final IMAudienceBidderBannerListener bidderBannerListener;

        private BannerBidToken(String placement, MoPubView moPubView, IMAudienceBidderBannerListener bidderBannerListener) {
            this.placement = placement;
            this.moPubView = moPubView;
            this.bidderBannerListener = bidderBannerListener;
        }

        /**
         * Object used to get an bid.
         * @param context Current context. Cannot be null.
         * @param dtbAdResponse Amazon's ad object with bid information. Can be null
         *         if APS did not return with an ad.
         * @param timeoutMillis Maximum time the listener has to return.
         */
        @Override
        public void refreshBid(@NonNull Context context, @Nullable final DTBAdResponse dtbAdResponse, long timeoutMillis) {
            if (TextUtils.isEmpty(placement)) {
                Error error = new Error("Cannot update MoPubView keywords with a empty or null placement.");
                bidderBannerListener.onBidFailed(moPubView, error);
                return;
            }

            Map<String, Object> tmpMap = new HashMap<>(moPubView.getLocalExtras());
            tmpMap.put(AD_KEY, placement);
            moPubView.setLocalExtras(tmpMap);

            String staleKeywordRemoved = removeIMKeywords(moPubView.getKeywords());
            moPubView.setKeywords(staleKeywordRemoved);

            boolean hasValidAps = false;

            final Double price = dtbAdResponse == null ? null : ApsUtil.getPrice(placement, dtbAdResponse);
            if (price != null) {
                String bidKeyword = getBidKeyword(price, AdTypes.BANNER);
                String granularBidKeywork = getGranularBidKeyword(price, AdTypes.BANNER);
                if (TextUtils.isEmpty(staleKeywordRemoved)) {
                    moPubView.setKeywords(bidKeyword.equals(granularBidKeywork) ? String.format("%s", bidKeyword) : String.format("%s,%s", bidKeyword, granularBidKeywork));
                } else {
                    moPubView.setKeywords(bidKeyword.equals(granularBidKeywork) ? String.format("%s,%s", staleKeywordRemoved, bidKeyword) : String.format("%s,%s,%s", staleKeywordRemoved, bidKeyword, granularBidKeywork));
                }
                hasValidAps = true;
            }

            final IMAudienceBidderBannerListener wrapper = new IMAudienceBidderBannerListener() {

                private IMAudienceBidderBannerListener bidderBannerListenerWrapper = BannerBidToken.this.bidderBannerListener;
                private boolean hasCallbackFired = false;

                @Override
                public void onBidRecieved(@NonNull MoPubView moPubView) {
                    if (!hasCallbackFired) {
                        hasCallbackFired = true;
                        bidderBannerListenerWrapper.onBidRecieved(moPubView);
                    }
                }

                @Override
                public void onBidFailed(@NonNull MoPubView moPubView, @NonNull Error error) {
                    if (!hasCallbackFired) {
                        hasCallbackFired = true;
                        bidderBannerListenerWrapper.onBidFailed(moPubView, error);
                    }
                }
            };

            Timer timer = null;
            if (timeoutMillis > 0) {
                timer = new Timer();
                TimerTask task = new TimerTask() {
                    @Override
                    public void run() {
                        IMAudienceBidder.getInstance().handler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (price != null) {
                                    wrapper.onBidRecieved(moPubView);
                                } else {
                                    Error error = new Error("No ad filled with IMAudienceBidder.");
                                    wrapper.onBidFailed(moPubView, error);
                                }
                            }
                        });
                    }
                };
                timer.schedule(task, timeoutMillis);
            } else if (timeoutMillis < 0) {
                IMAudienceBidder.getInstance().handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (price != null) {
                            wrapper.onBidRecieved(moPubView);
                        } else {
                            Error error = new Error("No ad filled with IMAudienceBidder.");
                            wrapper.onBidFailed(moPubView, error);
                        }
                    }
                });
                return;
            }

            MoPubLog.d("AudienceBidder: Attempting to add bid to MoPubView for placement: " + placement);

            IMABCustomEventBanner.AerServBannerListener aerServEventListener = new IMABCustomEventBanner.AerServBannerListener(moPubView, placement, wrapper, timer);

            AerServBanner aerServBanner = new AerServBanner(context);
            if (moPubView.getLayoutParams() != null) {
                aerServBanner.setLayoutParams(moPubView.getLayoutParams());
            }

            aerServEventListener.setAerServBanner(aerServBanner);
            IMABCustomEventBanner.listenerMap.put(placement, aerServEventListener);

            AerServConfig aerServConfig = new AerServConfig(context, placement)
                    .setPreload(true)
                    .setEventListener(aerServEventListener)
                    .setRefreshInterval(0);

            if (hasValidAps) {
                aerServConfig.setAPSAdResponses(Collections.singletonList(dtbAdResponse));
            }

            aerServBanner.configure(aerServConfig);
        }
    }

    private static final class RewardedBidToken implements BidToken {

        private final String userId;
        private final String placement;
        private final IMAudienceBidderRewardedListener bidderRewardedListener;

        private RewardedBidToken(@NonNull String placement, @Nullable String userId,
                                 @NonNull IMAudienceBidderRewardedListener bidderRewardedListener) {
            this.placement = placement;
            this.userId = userId;
            this.bidderRewardedListener = bidderRewardedListener;
        }

        /**
         * Object used to get an bid.
         * @param context Current context. Cannot be null.
         * @param dtbAdResponse Amazon's ad object with bid information. Can be null
         *         if APS did not return with an ad.
         * @param timeoutMillis Maximum time the listener has to return.
         */
        @Override
        public void refreshBid(@NonNull Context context, @Nullable final DTBAdResponse dtbAdResponse, long timeoutMillis) {
            if (TextUtils.isEmpty(placement)) {
                Error error = new Error("Cannot update RequestParameters keywords with a empty or null placement.");
                IMABRewardedWrapper wrapper = new IMABRewardedWrapper(null, null);
                bidderRewardedListener.onBidFailed(wrapper, error);
                return;
            }

            final IMAudienceBidderRewardedListener wrapper = new IMAudienceBidderRewardedListener() {

                private IMAudienceBidderRewardedListener bidderRewardedListenerWrapper = RewardedBidToken.this.bidderRewardedListener;
                private boolean hasCallbackFired = false;

                @Override
                public void onBidRecieved(@NonNull IMABRewardedWrapper rewardedWrapper) {
                    if (!hasCallbackFired) {
                        hasCallbackFired = true;
                        bidderRewardedListenerWrapper.onBidRecieved(rewardedWrapper);
                    }
                }

                @Override
                public void onBidFailed(@NonNull IMABRewardedWrapper rewardedWrapper, @NonNull Error error) {
                    if (!hasCallbackFired) {
                        hasCallbackFired = true;
                        bidderRewardedListenerWrapper.onBidFailed(rewardedWrapper, error);
                    }
                }
            };

            MoPubLog.d("AudienceBidder: Attempting to add bid to RequestParameters for placement: " + placement);

            final IMABCustomEventRewarded.AerServRewardedListener aerServEventListener = new IMABCustomEventRewarded.AerServRewardedListener(placement, wrapper);
            AerServConfig aerServConfig = new AerServConfig(context, placement)
                    .setPreload(true)
                    .setEventListener(aerServEventListener)
                    .setUseHeaderBidding(true);

            if (!TextUtils.isEmpty(userId)) {
                aerServConfig.setUserId(userId);
            }

            if (dtbAdResponse != null) {
                Double price = ApsUtil.getPrice(placement, dtbAdResponse);
                if (price != null) {
                    aerServConfig.setAPSAdResponses(Collections.singletonList(dtbAdResponse));
                }
            }

            final AerServInterstitial aerServInterstitial = new AerServInterstitial(aerServConfig);

            if (timeoutMillis > 0) {
                Timer timer = new Timer();
                TimerTask task = new TimerTask() {

                    @Override
                    public void run() {
                        IMAudienceBidder.getInstance().handler.post(new Runnable() {
                            @Override
                            public void run() {
                                IMABRewardedWrapper rewardedWrapper;
                                if (dtbAdResponse != null) {
                                    Double price = ApsUtil.getPrice(placement, dtbAdResponse);
                                    String bidKeyword, grandularBidPriceKeyword;
                                    if (price != null) {
                                        bidKeyword = getBidKeyword(price, AdTypes.INT);
                                        grandularBidPriceKeyword = getGranularBidKeyword(price, AdTypes.INT);
                                    } else {
                                        bidKeyword = "";
                                        grandularBidPriceKeyword = "";
                                    }
                                    String keywords = bidKeyword.equals(grandularBidPriceKeyword) ? String.format("%s", bidKeyword) : String.format("%s,%s", bidKeyword, grandularBidPriceKeyword);
                                    IMAudienceBidderMediationSettings settings = new IMAudienceBidderMediationSettings(placement, aerServInterstitial, aerServEventListener);
                                    rewardedWrapper = new IMABRewardedWrapper(keywords , settings);

                                    wrapper.onBidRecieved(rewardedWrapper);
                                } else {
                                    rewardedWrapper = new IMABRewardedWrapper(null, null);
                                    Error error = new Error("No ad to show with IMAudienceBidder.");
                                    wrapper.onBidFailed(rewardedWrapper, error);
                                }
                            }
                        });
                    }
                };
                timer.schedule(task, timeoutMillis);
                aerServEventListener.setTimer(timer);
            } else if (timeoutMillis < 0) {
                IMABRewardedWrapper rewardedWrapper = new IMABRewardedWrapper(null, null);
                Error error = new Error("No ad filled with IMAudienceBidder.");
                wrapper.onBidFailed(rewardedWrapper, error);
                return;
            }
            aerServEventListener.setAerServInterstitial(aerServInterstitial);
        }
    }

    /**
     * Creates a bidToken and updates the MoPub Banner object with the keywords
     * corresponding to the bid. Any keywords in the format of "IMAB:X.XX" will
     * be removed on this call.
     * @param context Current context. Cannot be null.
     * @param placement AerServ's placement id. Cannot be null or empty.
     * @param moPubView MoPub banner object that the keyword will be injected
     *         into. Cannot be null.
     * @param audienceBidderListener AudienceBidder listener. Cannot be null.
     */
    @NonNull
    public BidToken updateBid(@NonNull Context context, String placement, @NonNull MoPubView moPubView,
                              @NonNull IMAudienceBidderBannerListener audienceBidderListener) {

        return updateBid(context, placement, moPubView, null, audienceBidderListener, 0);
    }

    /**
     * Creates a bidToken and updates the MoPub Banner object with the keywords
     * corresponding to the bid. Any keywords in the format of "IMAB:X.XX" will
     * be removed on this call.
     * @param context Current context. Cannot be null.
     * @param placement AerServ's placement id. Cannot be null or empty.
     * @param dtbAdResponse Amazon's ad object with bid information. Can be null
     *         if APS did no require with an ad.
     * @param moPubView MoPub banner object that the keyword will be injected
     *         into. Cannot be null.
     * @param audienceBidderListener AudienceBidder listener. Cannot be null.
     * @param timeoutMillis Maximum time the listener has to return.
     */
    @NonNull
    public BidToken updateBid(@NonNull Context context, String placement, @NonNull MoPubView moPubView,
                              @Nullable DTBAdResponse dtbAdResponse,
                              @NonNull IMAudienceBidderBannerListener audienceBidderListener,
                              long timeoutMillis) {

        BidToken bidToken = new BannerBidToken(placement, moPubView, audienceBidderListener);
        bidToken.refreshBid(context, dtbAdResponse, timeoutMillis);

        return bidToken;
    }

    /**
     * Creates a bidToken and updates the MoPub Banner object with the keywords
     * corresponding to the bid. Any keywords in the format of "IMAB:X.XX" will
     * be removed on this call.
     * @param context Current context. Cannot be null.
     * @param placement AerServ's placement id. Cannot be null or empty.
     * @param moPubInterstitial MoPub interstitial object that the keyword will be injected
     *         into. Cannot be null.
     * @param audienceBidderListener AudienceBidder listener. Cannot be null.
     */
    @NonNull
    public BidToken updateBid(@NonNull Context context, String placement, @NonNull MoPubInterstitial moPubInterstitial,
                              @NonNull IMAudienceBidderInterstitialListener audienceBidderListener) {

        return updateBid(context, placement, moPubInterstitial, null, audienceBidderListener, 0);
    }

    /**
     * Creates a bidToken and updates the MoPub Banner object with the keywords
     * corresponding to the bid. Any keywords in the format of "IMAB:X.XX" will
     * be removed on this call.
     * @param context Current context. Cannot be null.
     * @param placement AerServ's placement id. Cannot be null or empty.
     * @param dtbAdResponse Amazon's ad object with bid information. Can be null
     *         if APS did no require with an ad.
     * @param moPubInterstitial MoPub interstitial object that the keyword will be injected
     *         into. Cannot be null.
     * @param audienceBidderListener AudienceBidder listener. Cannot be null.
     * @param timeoutMillis Maximum time the listener has to return.
     */
    @NonNull
    public BidToken updateBid(@NonNull Context context, String placement, @NonNull MoPubInterstitial moPubInterstitial,
                              @Nullable DTBAdResponse dtbAdResponse,
                              @NonNull IMAudienceBidderInterstitialListener audienceBidderListener,
                              long timeoutMillis) {

        BidToken bidToken = new InterstitialBidToken(placement, moPubInterstitial, audienceBidderListener);
        bidToken.refreshBid(context, dtbAdResponse, timeoutMillis);

        return bidToken;
    }

    /**
     * Creates a bidToken and updates the MoPub Banner object with the keywords
     * corresponding to the bid. Any keywords in the format of "IMAB:X.XX" will
     * be removed on this call.
     * @param context Current context. Cannot be null.
     * @param placement AerServ's placement id. Cannot be null or empty.
     * @param userId User's userId within publisher system. This is meant to be used for frequency
     *               cap.
     * @param audienceBidderListener AudienceBidder listener. Cannot be null.
     */
    @NonNull
    public BidToken updateBid(@NonNull Context context, String placement, @Nullable String userId,
                              @NonNull IMAudienceBidderRewardedListener audienceBidderListener) {

        return updateBid(context, placement, userId, null, audienceBidderListener, 0);
    }

    /**
     * Creates a bidToken and updates the MoPub Banner object with the keywords
     * corresponding to the bid. Any keywords in the format of "IMAB:X.XX" will
     * be removed on this call.
     * @param context Current context. Cannot be null.
     * @param placement AerServ's placement id. Cannot be null or empty.
     * @param dtbAdResponse Amazon's ad object with bid information. Can be null
     *         if APS did no require with an ad.
     * @param userId User's userId within publisher system. This is meant to be used for frequency
     *               cap.
     * @param audienceBidderListener AudienceBidder listener. Cannot be null.
     * @param timeoutMillis Maximum time the listener has to return.
     */
    @NonNull
    public BidToken updateBid(@NonNull Context context, String placement, @Nullable String userId,
                              @Nullable DTBAdResponse dtbAdResponse,
                              @NonNull IMAudienceBidderRewardedListener audienceBidderListener,
                              long timeoutMillis) {

        BidToken bidToken = new RewardedBidToken(placement, userId, audienceBidderListener);
        bidToken.refreshBid(context, dtbAdResponse, timeoutMillis);

        return bidToken;
    }

    @NonNull
    static String getBidKeyword(double buyerPrice, @AdTypes String adType) {
        double bidPrice;

        if (AdTypes.INT.equals(adType)) {
            if (buyerPrice < 0.01) {
                bidPrice = 0.00;
            } else if (buyerPrice >= 0.01 && buyerPrice < 0.05) {
                bidPrice = 0.01;
            } else if (buyerPrice >= 0.05 && buyerPrice < 0.10) {
                bidPrice = 0.05;
            } else if (buyerPrice >= 0.10 && buyerPrice < 2.00) {
                int tmpPrice = (int) (buyerPrice * 100);
                bidPrice = (tmpPrice - (tmpPrice % 10)) / 100.0;
            } else if (buyerPrice >= 2.00 && buyerPrice < 4.00) {
                int tmpPrice = (int) (buyerPrice * 100);
                bidPrice = (tmpPrice - (tmpPrice % 50)) / 100.0;
            } else if (buyerPrice >= 4.00 && buyerPrice < 75.00) {
                bidPrice = (int) buyerPrice;
            } else {
                bidPrice = 75.00;
            }
        } else {
            if (buyerPrice < 0.01) {
                bidPrice = 0.00;
            } else if (buyerPrice >= 0.01 && buyerPrice < 0.05) {
                bidPrice = 0.01;
            } else if (buyerPrice >= 0.05 && buyerPrice < 0.10) {
                bidPrice = 0.05;
            } else if (buyerPrice >= 0.10 && buyerPrice < 2.00) {
                int tmpPrice = (int) (buyerPrice * 100);
                bidPrice = (tmpPrice - (tmpPrice % 10)) / 100.0;
            } else if (buyerPrice >= 2.00 && buyerPrice < 4.00) {
                int tmpPrice = (int) (buyerPrice * 100);
                bidPrice = (tmpPrice - (tmpPrice % 50)) / 100.0;
            } else if (buyerPrice >= 4.00 && buyerPrice < 30.00) {
                bidPrice = (int) buyerPrice;
            } else {
                bidPrice = 30.00;
            }
        }

        Formatter formatter = new Formatter();
        String bidString = formatter.format("%.2f", bidPrice).toString();
        formatter.close();
        return String.format("IMAB:%s", bidString);
    }

    @NonNull
    static String getGranularBidKeyword(double buyerPrice, @AdTypes String adType) {
        double bidPrice;

        if (AdTypes.INT.equals(adType)) {
            if (buyerPrice < 0.01) {
                bidPrice = 0.00;
            } else if (buyerPrice >= 0.01 && buyerPrice < 0.05) {
                bidPrice = 0.01;
            } else if (buyerPrice >= 0.05 && buyerPrice < 0.10) {
                bidPrice = 0.05;
            } else if (buyerPrice < 75) {
                int tmpPrice = (int) (buyerPrice * 100);
                bidPrice = (tmpPrice - (tmpPrice % 10)) / 100.0;
            } else {
                bidPrice = 75.00;
            }
        } else {
            if (buyerPrice < 0.01) {
                bidPrice = 0.00;
            } else if (buyerPrice >= 0.01 && buyerPrice < 0.05) {
                bidPrice = 0.01;
            } else if (buyerPrice >= 0.05 && buyerPrice < 0.10) {
                bidPrice = 0.05;
            } else if (buyerPrice < 30.00) {
                int tmpPrice = (int) (buyerPrice * 100);
                bidPrice = (tmpPrice - (tmpPrice % 10)) / 100.0;
            } else {
                bidPrice = 30.00;
            }
        }

        Formatter formatter = new Formatter();
        String bidString = formatter.format("%.2f", bidPrice).toString();
        formatter.close();
        return String.format("IMAB:%s", bidString);
    }

    @Nullable
    static String removeIMKeywords(@Nullable String keyword) {
        Matcher matcher;

        if (!TextUtils.isEmpty(keyword)) {
            if ((matcher = Pattern.compile(",(IMAB:[0-9]+.[0-9]{2},)+,").matcher(keyword)).find()) {
                keyword = matcher.replaceAll(",");
            }

            if ((matcher = Pattern.compile(",?IMAB:[0-9]+.[0-9]{2},?").matcher(keyword)).find()) {
                keyword = matcher.replaceAll("");
            }
        }

        return keyword;
    }
}
