package com.inmobi.plugin.mopub;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.aerserv.sdk.AerServBanner;
import com.aerserv.sdk.AerServConfig;
import com.aerserv.sdk.AerServInterstitial;
import com.aerserv.sdk.AerServTransactionInformation;
import com.mopub.common.MediationSettings;
import com.mopub.common.logging.MoPubLog;
import com.mopub.mobileads.MoPubInterstitial;
import com.mopub.mobileads.MoPubView;

import java.util.Formatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IMAudienceBidder {

    static final String AD_KEY = "IMABAd";
    static final String LISTENER_KEY = "IMABAdListener";

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

    private IMAudienceBidder() {
        // Do nothing.
    }

    public void updateBid(@NonNull Context context, String plc,
                          @NonNull MoPubView moPubView,
                          @NonNull IMAudienceBidderBannerListener audienceBidderListener) {
        if (TextUtils.isEmpty(plc)) {
            Error error = new Error("Cannot update MoPubView keywords with a empty or null placement.");
            audienceBidderListener.onBidFailed(moPubView, error);
            return;
        }

        String keywords = moPubView.getKeywords();
        moPubView.setKeywords(removeIMKeyword(keywords));

        MoPubLog.d("AudienceBidder: Attempting to add bid to MoPubView for placement: " + plc);

        IMABCustomEventBanner.AerServBannerListener aerServEventListener = new IMABCustomEventBanner.AerServBannerListener(moPubView, audienceBidderListener);

        AerServBanner aerServBanner = new AerServBanner(context);
        if (moPubView.getLayoutParams() != null) {
            aerServBanner.setLayoutParams(moPubView.getLayoutParams());
        }

        aerServEventListener.setAerServBanner(aerServBanner);
        AerServConfig aerServConfig = new AerServConfig(context, plc)
                .setPreload(true)
                .setEventListener(aerServEventListener)
                .setRefreshInterval(0);
        aerServBanner.configure(aerServConfig);
    }

    public void updateBid(@NonNull Context context, String plc,
                          @NonNull MoPubInterstitial moPubInterstitial,
                          @NonNull IMAudienceBidderInterstitialListener audienceBidderListener) {
        if (TextUtils.isEmpty(plc)) {
            Error error = new Error("Cannot update MoPubInterstitial keywords with a empty or null placement.");
            audienceBidderListener.onBidFailed(moPubInterstitial, error);
            return;
        }

        MoPubLog.d("AudienceBidder: Attempting to add bid to MoPubInterstitial for placement: " + plc);

        String keywords = moPubInterstitial.getKeywords();
        moPubInterstitial.setKeywords(removeIMKeyword(keywords));

        IMABCustomEventInterstitial.AerServInterstitialListener aerServEventListener = new IMABCustomEventInterstitial.AerServInterstitialListener(moPubInterstitial, audienceBidderListener);

        AerServConfig aerServConfig = new AerServConfig(context, plc)
                .setPreload(true)
                .setEventListener(aerServEventListener);
        AerServInterstitial aerServInterstitial = new AerServInterstitial(aerServConfig);
        aerServEventListener.setAerServInterstitial(aerServInterstitial);
    }

    public void updateBid(@NonNull Context context, String plc,
                          @NonNull String userId,
                          @NonNull IMAudienceBidderRewardedListener audienceBidderListener) {
        if (TextUtils.isEmpty(plc)) {
            Error error = new Error("Cannot update RequestParameters keywords with a empty or null placement.");
            IMABRewardedWrapper wrapper = new IMABRewardedWrapper(null, null);
            audienceBidderListener.onBidFailed(wrapper, error);
            return;
        }

        MoPubLog.d("AudienceBidder: Attempting to add bid to RequestParameters for placement: " + plc);

        IMABCustomEventRewarded.AerServRewardedListener aerServEventListener = new IMABCustomEventRewarded.AerServRewardedListener(plc, audienceBidderListener);
        AerServConfig aerServConfig = new AerServConfig(context, plc)
                .setPreload(true)
                .setEventListener(aerServEventListener)
                .setUserId(userId);
        AerServInterstitial aerServInterstitial = new AerServInterstitial(aerServConfig);
        aerServEventListener.setAerServInterstitial(aerServInterstitial);
    }

    static String getBidKeyword(@NonNull AerServTransactionInformation aerServTransactionInformation) {
        double bidPrice;
        double buyerPrice = aerServTransactionInformation.getBuyerPrice().doubleValue();

        if (buyerPrice <= 0.00) {
            bidPrice = 0.00;
        } else if (buyerPrice > 0.00 && buyerPrice < 0.05) {
            bidPrice = 0.01;
        } else if (buyerPrice >= 0.05 && buyerPrice < 0.10) {
            bidPrice = 0.05;
        } else if (buyerPrice >= 0.10 && buyerPrice < 2.00) {
            int tmpPrice =  (int) (buyerPrice * 100);
            bidPrice = (tmpPrice - (tmpPrice % 10))/100.0;
        } else if (buyerPrice >= 2.00 && buyerPrice < 4.00) {
            int tmpPrice =  (int) (buyerPrice * 100);
            bidPrice = (tmpPrice - (tmpPrice % 50))/100.0;
        } else if (buyerPrice >= 4.00 && buyerPrice < 30.00) {
            bidPrice = (int) buyerPrice;
        } else {
            bidPrice = 30.00;
        }

        Formatter formatter = new Formatter();
        String bidString = formatter.format("%.2f", bidPrice).toString();
        formatter.close();
        return String.format("IMAB:%s", bidString);
    }

    @Nullable
    static String removeIMKeyword(@Nullable String keyword) {
        Matcher matcher;

        if (!TextUtils.isEmpty(keyword)) {
            if ((matcher = Pattern.compile(",(IMAB:[0-9]+.[0-9]{2},)+").matcher(keyword)).find()) {
                keyword = matcher.replaceAll(",");
            }

            if ((matcher = Pattern.compile(",?IMAB:[0-9]+.[0-9]{2},?").matcher(keyword)).find()) {
                keyword = matcher.replaceAll("");
            }
        }

        return keyword;
    }
}
