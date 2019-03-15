package com.mopub.mobileads;

import android.content.Context;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.aerserv.sdk.AerServAd;
import com.aerserv.sdk.AerServBanner;
import com.aerserv.sdk.AerServConfig;
import com.aerserv.sdk.AerServEvent;
import com.aerserv.sdk.AerServEventListener;
import com.aerserv.sdk.AerServInterstitial;
import com.aerserv.sdk.AerServTransactionInformation;
import com.aerserv.sdk.AerServVirtualCurrency;
import com.mopub.common.MoPubReward;
import com.mopub.common.logging.MoPubLog;
import com.mopub.mobileads.MoPubRewardedVideoManager.RequestParameters;

import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AerServBidder {

    private static final Object LOCK = new Object();

    private static AerServBidder instance = null;

    private Map<String, AerServAd> aerservPlacementAdMap = new HashMap<>();
    private Map<String, Object> aerservPlacementListenerMap = new HashMap<>();

    private String rewardedKeyword = null;

    private AerServBidder() {
    }

    // Only one thread can execute this at a time
    public static AerServBidder getInstance() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new AerServBidder();
                }
            }
        }
        return instance;
    }

    public void updateBidForInterstitial(@NonNull String plc, @NonNull Context context,
                                         @NonNull MoPubInterstitial moPubInterstitial,
                                         AerServBidListener aerServBidListener) {
        if (TextUtils.isEmpty(plc)) {
            Error error = new Error("Cannot update MoPubInterstitial keywords with a empty AerServ placement.");
            if (aerServBidListener != null) {
                aerServBidListener.onBidFailed(moPubInterstitial, error);

            }
            return;
        }

        MoPubLog.d("AerServSDK: Attempting to add bid to MoPubInterstitial for AerServ placement: " + plc);
        AerServConfig aerServConfig = getAerservConfig(context, plc, aerServBidListener,
                moPubInterstitial);
        AerServInterstitial aerServInterstitial = new AerServInterstitial(aerServConfig);
        aerservPlacementAdMap.put(plc, aerServInterstitial);
    }

    public void updateBidForRewarded(@NonNull String plc, @NonNull Context context,
                                     @NonNull RequestParameters mopubRequestParam,
                                     AerServBidListener aerServBidListener) {
        if (TextUtils.isEmpty(plc)) {
            Error error = new Error("Cannot update RequestParameters keywords with a empty " +
                    "or null AerServ placement.");
            if (aerServBidListener != null) {
                aerServBidListener.onBidFailed(mopubRequestParam, error);

            }
            return;
        }

        MoPubLog.d("AerServSDK: Attempting to add bid to RequestParameters for AerServ placement: " + plc);
        AerServConfig aerServConfig = getAerservConfig(context, plc, aerServBidListener,
                mopubRequestParam);
        aerServConfig.setUserId(mopubRequestParam.mCustomerId);
        AerServInterstitial aerServInterstitial = new AerServInterstitial(aerServConfig);

        aerservPlacementAdMap.put(plc, aerServInterstitial);
    }

    public void updateBidForBanner(@NonNull String plc, @NonNull Context context,
                                   @NonNull MoPubView moPubView,
                                   AerServBidListener aerServBidListener) {
        if (TextUtils.isEmpty(plc)) {
            Error error = new Error("Cannot update MoPubView keywords with a empty AerServ placement.");
            if (aerServBidListener != null) {
                aerServBidListener.onBidFailed(moPubView, error);

            }
            return;
        }

        MoPubLog.d("AerServSDK: Attempting to add bid to MoPubView for AerServ placement: " + plc);

        AerServBanner aerServBanner = new AerServBanner(context);
        AerServConfig aerServConfig = getAerservConfig(context, plc, aerServBidListener,
                moPubView);
        aerServConfig.setRefreshInterval(0);
        aerServBanner.configure(aerServConfig);
        aerservPlacementAdMap.put(plc, aerServBanner);
    }

    AerServAd getAdForPlacement(String placement) {
        return aerservPlacementAdMap.get(placement);
    }

    void removeAdForPlacement(String placement) {
        aerservPlacementAdMap.remove(placement);
    }

    void setListenerForPlacement(String placement, Object mopubListener) {
        aerservPlacementListenerMap.put(placement,mopubListener);
    }

    void removeListener(String placement) {
        aerservPlacementListenerMap.remove(placement);
    }

    private AerServConfig getAerservConfig(@NonNull final Context context,
                                           @NonNull final String placement,
                                           final AerServBidListener aerServBidListener,
                                           @NonNull final Object mopubObject) {
        final AerServConfig aerServConfig = new AerServConfig(context, placement);
        aerServConfig.setPreload(true);
        aerServConfig.setEventListener(new AerServEventListener() {
            @Override
            public void onAerServEvent(final AerServEvent aerServEvent, final List<Object> list) {
                Handler handler = new Handler(context.getMainLooper());

                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        CustomEventInterstitial.CustomEventInterstitialListener customEventInterstitialListener = null;
                        CustomEventBanner.CustomEventBannerListener customEventBannerListener = null;
                        String rewarded_video_tag = null;
                        if (aerservPlacementListenerMap.get(placement) != null) {
                            if (aerservPlacementListenerMap.get(placement) instanceof CustomEventInterstitial.CustomEventInterstitialListener) {
                                customEventInterstitialListener = (CustomEventInterstitial.CustomEventInterstitialListener) aerservPlacementListenerMap.get(placement);

                            } else if (aerservPlacementListenerMap.get(placement) instanceof CustomEventBanner.CustomEventBannerListener) {
                                customEventBannerListener = (CustomEventBanner.CustomEventBannerListener) aerservPlacementListenerMap.get(placement);

                            } else if (aerservPlacementListenerMap.get(placement) instanceof String) {
                                rewarded_video_tag = (String) aerservPlacementListenerMap.get(placement);

                            }

                        }

                        switch(aerServEvent) {
                            case VC_READY:
                                MoPubLog.d("AerServSDK: Aerserv Rewarded Ad Virtual Currency ready");
                                if (mopubObject instanceof RequestParameters) {

                                    AerServVirtualCurrency vc = (AerServVirtualCurrency) list.get(0);
                                    String bidKeyword = getBidKeyword(vc.getAerServTransactionInformation(), placement, mopubObject);
                                    RequestParameters requestParameters = (RequestParameters) mopubObject;

                                    Matcher matcher;

                                    String oldKeyword = requestParameters.mKeywords;
                                    if (!TextUtils.isEmpty(oldKeyword)) {
                                        if ((matcher = Pattern.compile(",(AS_[0-9]+:[0-9]+.[0-9]{2},)+").matcher(oldKeyword)).find()) {
                                            oldKeyword = matcher.replaceAll(",");
                                        }

                                        if ((matcher = Pattern.compile(",?AS_[0-9]+:[0-9]+.[0-9]{2},?").matcher(oldKeyword)).find()) {
                                            oldKeyword = matcher.replaceAll("");
                                        }
                                    }

                                    String keywords;

                                    if (TextUtils.isEmpty(rewardedKeyword)) {
                                        rewardedKeyword = bidKeyword;

                                    } else if ((matcher = Pattern.compile("AS_" + placement + ":[0-9]+.[0-9]{2}").matcher(rewardedKeyword)).find()) {
                                        rewardedKeyword = matcher.replaceAll(bidKeyword);

                                    } else {
                                        rewardedKeyword = rewardedKeyword + "," + bidKeyword;
                                    }

                                    if (TextUtils.isEmpty(oldKeyword)) {
                                        keywords = rewardedKeyword;

                                    } else {
                                        keywords = rewardedKeyword + "," + oldKeyword;

                                    }

                                    RequestParameters newRequestParameters = new RequestParameters(
                                            keywords,
                                            requestParameters.mUserDataKeywords,
                                            requestParameters.mLocation,
                                            requestParameters.mCustomerId);

                                    if (aerServBidListener != null) {
                                        aerServBidListener.onBidRecieved(newRequestParameters);
                                    }
                                }
                                break;

                            case VC_REWARDED:
                                AerServVirtualCurrency vc = (AerServVirtualCurrency) list.get(0);
                                if (mopubObject instanceof RequestParameters) {
                                    MoPubReward reward;
                                    if (!TextUtils.isEmpty(vc.getBuyerName()) &&
                                            vc.getAmount().doubleValue() >= 0) {
                                        reward = MoPubReward.success(vc.getBuyerName(),
                                                vc.getAmount().intValue());

                                    } else {
                                        reward = MoPubReward.failure();

                                    }

                                    MoPubRewardedVideoManager.onRewardedVideoCompleted(
                                            AerServCustomEventRewardedInterstitial.class, placement,
                                            reward);
                                }
                                break;

                            case AD_IMPRESSION:
                                if (customEventInterstitialListener != null) {
                                    MoPubLog.d("AerServSDK: Aerserv Interstitial ad shown");

                                    customEventInterstitialListener.onInterstitialShown();

                                    if (mopubObject instanceof MoPubInterstitial) {
                                        String oldKeyword;

                                        MoPubInterstitial moPubInterstitial = (MoPubInterstitial) mopubObject;

                                        if (!TextUtils.isEmpty(oldKeyword = moPubInterstitial.getKeywords())) {
                                            String keyword;
                                            Matcher matcher;

                                            if ((matcher = Pattern.compile(",AS_" + placement + ":[0-9]+.[0-9]{2},").matcher(oldKeyword)).find()) {
                                                keyword = matcher.replaceAll(",");
                                            } else if ((matcher = Pattern.compile(",?AS_" + placement + ":[0-9]+.[0-9]{2},?").matcher(oldKeyword)).find()) {
                                                keyword = matcher.replaceAll("");
                                            } else {
                                                keyword = oldKeyword;
                                            }
                                            moPubInterstitial.setKeywords(keyword);
                                        }
                                    }

                                } else if (mopubObject instanceof MoPubView) {
                                    String oldKeyword;

                                    MoPubView moPubView = (MoPubView) mopubObject;

                                    if (!TextUtils.isEmpty(oldKeyword = moPubView.getKeywords())) {
                                        String keyword;
                                        Matcher matcher;

                                        if ((matcher = Pattern.compile(",AS_" + placement + ":[0-9]+.[0-9]{2},").matcher(oldKeyword)).find()) {
                                            keyword = matcher.replaceAll(",");
                                        } else if ((matcher = Pattern.compile(",?AS_" + placement + ":[0-9]+.[0-9]{2},?").matcher(oldKeyword)).find()) {
                                            keyword = matcher.replaceAll("");
                                        } else {
                                            keyword = oldKeyword;
                                        }
                                        moPubView.setKeywords(keyword);
                                    }

                                } else if (mopubObject instanceof RequestParameters) {
                                    MoPubLog.d("AerServSDK: Aerserv Rewarded interstitial ad shown.");
                                    MoPubRewardedVideoManager.onRewardedVideoStarted(
                                            AerServCustomEventRewardedInterstitial.class
                                            , placement);

                                    rewardedKeyword = null;
                                }
                                break;

                            case AD_CLICKED:
                                if (customEventInterstitialListener != null) {
                                    MoPubLog.d("AerServSDK: Aerserv Interstitial ad clicked");
                                    customEventInterstitialListener.onInterstitialClicked();

                                } else if (customEventBannerListener != null) {
                                    MoPubLog.d("AerServSDK: Aerserv Banner ad clicked");
                                    customEventBannerListener.onBannerClicked();

                                } else if (rewarded_video_tag != null) {
                                    MoPubLog.d("AerServSDK: Aerserv Rewarded interstitial ad clicked.");
                                    MoPubRewardedVideoManager.onRewardedVideoClicked(
                                            AerServCustomEventRewardedInterstitial.class, placement);

                                }
                                break;

                            case AD_DISMISSED:
                                if (customEventInterstitialListener != null) {
                                    MoPubLog.d("AerServSDK: Aerserv Interstitial ad dismissed");
                                    customEventInterstitialListener.onInterstitialDismissed();

                                } else if (customEventBannerListener != null) {
                                    MoPubLog.d("AerServSDK: Aerserv Banner ad dismissed");
                                    customEventBannerListener.onBannerCollapsed();

                                } else if (rewarded_video_tag != null) {
                                    MoPubLog.d("AerServSDK: Aerserv Rewarded interstitial ad dismissed.");
                                    MoPubRewardedVideoManager.onRewardedVideoClosed(
                                            AerServCustomEventRewardedInterstitial.class, placement);

                                }
                                aerservPlacementListenerMap.remove(placement);
                                aerservPlacementAdMap.remove(placement);
                                break;

                            case AD_FAILED:
                                // AD_FAILED can occur before and after AD_LOADED occurs. When AD_FAILS
                                // occur before AD_LOADED, only the placement map will be populated. When
                                // the failure occurs during a show attempt, the listener map will be
                                // populated. At this point, we don't need to populate return a bid, but
                                // we'll still need to update the MoPubObject reference with the removed
                                // keyword.
                                if (customEventInterstitialListener != null) {
                                    MoPubLog.d("AerServSDK: Failed to load Aerserv Interstitial ad");

                                    String oldKeyword;

                                    MoPubInterstitial moPubInterstitial = (MoPubInterstitial) mopubObject;

                                    if (!TextUtils.isEmpty(oldKeyword = moPubInterstitial.getKeywords())) {
                                        String keyword;
                                        Matcher matcher;

                                        if ((matcher = Pattern.compile(",AS_" + placement + ":[0-9]+.[0-9]{2},").matcher(oldKeyword)).find()) {
                                            keyword = matcher.replaceAll(",");

                                        } else if ((matcher = Pattern.compile(",?AS_" + placement + ":[0-9]+.[0-9]{2},?").matcher(oldKeyword)).find()) {
                                            keyword = matcher.replaceAll("");

                                        } else {
                                            keyword = oldKeyword;

                                        }
                                        moPubInterstitial.setKeywords(keyword);

                                    }
                                    customEventInterstitialListener.onInterstitialFailed(MoPubErrorCode.NETWORK_NO_FILL);

                                } else if (customEventBannerListener != null) {
                                    MoPubLog.d("AerServSDK: Failed to load Aerserv Banner ad");

                                    String oldKeyword;

                                    MoPubView moPubView = (MoPubView) mopubObject;

                                    if (!TextUtils.isEmpty(oldKeyword = moPubView.getKeywords())) {
                                        String keyword;
                                        Matcher matcher;

                                        if ((matcher = Pattern.compile(",AS_" + placement + ":[0-9]+.[0-9]{2},").matcher(oldKeyword)).find()) {
                                            keyword = matcher.replaceAll(",");

                                        } else if ((matcher = Pattern.compile(",?AS_" + placement + ":[0-9]+.[0-9]{2},?").matcher(oldKeyword)).find()) {
                                            keyword = matcher.replaceAll("");

                                        } else {
                                            keyword = oldKeyword;

                                        }
                                        moPubView.setKeywords(keyword);
                                    }
                                    customEventBannerListener.onBannerFailed(MoPubErrorCode.NETWORK_NO_FILL);

                                } else if (rewarded_video_tag != null) {
                                    MoPubLog.d("AerServSDK: Failed to show Aerserv rewarded interstitial ad");
                                    MoPubRewardedVideoManager.onRewardedVideoLoadFailure(
                                            AerServCustomEventRewardedInterstitial.class,
                                            placement, MoPubErrorCode.NETWORK_NO_FILL);

                                } else if (aerServBidListener != null) {
                                    Error error = new Error("Failed to load mopub ad and the corresponding bid price");

                                    if (mopubObject instanceof MoPubInterstitial) {
                                        MoPubLog.d("AerServSDK: Failed to load Aerserv Interstitial ad");

                                        String oldKeyword;

                                        MoPubInterstitial moPubInterstitial = (MoPubInterstitial) mopubObject;

                                        if (!TextUtils.isEmpty(oldKeyword = moPubInterstitial.getKeywords())) {
                                            String keyword;
                                            Matcher matcher;

                                            if ((matcher = Pattern.compile(",AS_" + placement + ":[0-9]+.[0-9]{2},").matcher(oldKeyword)).find()) {
                                                keyword = matcher.replaceAll(",");

                                            } else if ((matcher = Pattern.compile(",?AS_" + placement + ":[0-9]+.[0-9]{2},?").matcher(oldKeyword)).find()) {
                                                keyword = matcher.replaceAll("");

                                            } else {
                                                keyword = oldKeyword;

                                            }
                                            moPubInterstitial.setKeywords(keyword);

                                        }
                                        aerServBidListener.onBidFailed(mopubObject, error);

                                    } else if (mopubObject instanceof MoPubView) {
                                        String oldKeyword;

                                        MoPubView moPubView = (MoPubView) mopubObject;

                                        if (!TextUtils.isEmpty(oldKeyword = moPubView.getKeywords())) {
                                            String keyword;
                                            Matcher matcher;

                                            if ((matcher = Pattern.compile(",AS_" + placement + ":[0-9]+.[0-9]{2},").matcher(oldKeyword)).find()) {
                                                keyword = matcher.replaceAll(",");

                                            } else if ((matcher = Pattern.compile(",?AS_" + placement + ":[0-9]+.[0-9]{2},?").matcher(oldKeyword)).find()) {
                                                keyword = matcher.replaceAll("");

                                            } else {
                                                keyword = oldKeyword;

                                            }
                                            moPubView.setKeywords(keyword);
                                        }
                                        aerServBidListener.onBidFailed(mopubObject, error);

                                    } else if (mopubObject instanceof RequestParameters) {
                                        Matcher matcher;
                                        String keywords;

                                        RequestParameters requestParameters = (RequestParameters) mopubObject;
                                        String oldKeyword = requestParameters.mKeywords;
                                        if (!TextUtils.isEmpty(rewardedKeyword)) {
                                            if ((matcher = Pattern.compile(",AS_" + placement + ":[0-9]+.[0-9]{2},").matcher(rewardedKeyword)).find()) {
                                                rewardedKeyword = matcher.replaceAll(",");

                                            } else if ((matcher = Pattern.compile(",?AS_" + placement + ":[0-9]+.[0-9]{2},?").matcher(rewardedKeyword)).find()) {
                                                rewardedKeyword = matcher.replaceAll("");

                                            }
                                        }

                                        if (!TextUtils.isEmpty(oldKeyword)) {
                                            if ((matcher = Pattern.compile(",(AS_[0-9]+:[0-9]+.[0-9]{2},)+").matcher(oldKeyword)).find()) {
                                                oldKeyword = matcher.replaceAll(",");

                                            }

                                            if ((matcher = Pattern.compile(",?AS_[0-9]+:[0-9]+.[0-9]{2},?").matcher(oldKeyword)).find()) {
                                                oldKeyword = matcher.replaceAll("");

                                            }
                                        }

                                        if (TextUtils.isEmpty(oldKeyword)) {
                                            keywords = rewardedKeyword;

                                        } else {
                                            keywords = rewardedKeyword + "," + oldKeyword;

                                        }

                                        RequestParameters newRequestParameters = new RequestParameters(
                                                keywords,
                                                requestParameters.mUserDataKeywords,
                                                requestParameters.mLocation,
                                                requestParameters.mCustomerId);

                                        aerServBidListener.onBidFailed(newRequestParameters, error);
                                    }

                                }

                                aerservPlacementListenerMap.remove(placement);
                                aerservPlacementAdMap.remove(placement);
                                break;

                            case LOAD_TRANSACTION:
                                AerServTransactionInformation aerServTransactionInformation = (AerServTransactionInformation) list.get(0);
                                MoPubLog.d("AerServSDK: Load Transaction Information PLC has: " +
                                        aerServTransactionInformation.toString());

                                if (mopubObject instanceof MoPubInterstitial) {
                                    MoPubInterstitial moPubInterstitial = (MoPubInterstitial) mopubObject;
                                    String bidKeyword = getBidKeyword(aerServTransactionInformation, placement, moPubInterstitial);

                                    String keyword;
                                    Matcher matcher;

                                    if (TextUtils.isEmpty(moPubInterstitial.getKeywords())) {
                                        keyword = bidKeyword;

                                    } else if ((matcher = Pattern.compile("AS_" + placement + ":[0-9]+.[0-9]{2}").matcher(moPubInterstitial.getKeywords())).find()) {
                                        keyword = matcher.replaceAll(bidKeyword);

                                    } else {
                                        keyword = bidKeyword + "," + moPubInterstitial.getKeywords();

                                    }

                                    moPubInterstitial.setKeywords(keyword);
                                    if (aerServBidListener != null) {
                                        aerServBidListener.onBidRecieved(moPubInterstitial);

                                    }

                                } else if (mopubObject instanceof MoPubView) {
                                    MoPubView moPubView = (MoPubView) mopubObject;
                                    String bidKeyword = getBidKeyword(aerServTransactionInformation, placement, moPubView);

                                    String keyword;
                                    Matcher matcher;

                                    if (TextUtils.isEmpty(moPubView.getKeywords())) {
                                        keyword = bidKeyword;

                                    } else if ((matcher = Pattern.compile("AS_" + placement + ":[0-9]+.[0-9]{2}").matcher(moPubView.getKeywords())).find()) {
                                        keyword = matcher.replaceAll(bidKeyword);

                                    } else {
                                        keyword = bidKeyword + "," + moPubView.getKeywords();

                                    }
                                    moPubView.setKeywords(keyword);

                                    if (aerServBidListener != null) {
                                        aerServBidListener.onBidRecieved(moPubView);
                                    }

                                }
                                break;

                            case SHOW_TRANSACTION:
                                if (!list.isEmpty() && list.get(0) instanceof AerServTransactionInformation) {
                                    aerServTransactionInformation = (AerServTransactionInformation) list.get(0);
                                    MoPubLog.d("AerServSDK: Show Transaction Information PLC has: " +
                                            aerServTransactionInformation.toString());

                                } else {
                                    MoPubLog.d("AerServSDK: Unable to retrieve the AerServTransaction for SHOW_TRANSACTION");

                                }
                                break;

                            default:
                                MoPubLog.d("AerServSDK: The following AerServ ad event cannot be "
                                        + "mapped and will be ignored: " + aerServEvent.name());
                                break;
                        }
                    }
                };
                handler.post(runnable);
            }
        });
        return aerServConfig;
    }

    private String getBidKeyword(@NonNull AerServTransactionInformation aerServTransactionInformation,
                                 @NonNull String placement, @NonNull Object moPubObject) {
        double bidPrice;
        double buyerPrice = aerServTransactionInformation.getBuyerPrice().doubleValue();

        if (moPubObject instanceof MoPubView) {
            bidPrice = getKeywordsForIntervalAndThreshold(buyerPrice, 0.25, 5.00, 10.00);

        } else {
            bidPrice = getKeywordsForIntervalAndThreshold(buyerPrice, 0.50, 10.00, 25.00);

        }
        Formatter formatter = new Formatter();
        String bidString = formatter.format("%.2f", bidPrice).toString();
        formatter.close();
        return "AS_" + placement + ":" + bidString;
    }

    private double getKeywordsForIntervalAndThreshold(double buyerPrice, double interval,
                                                      double lowThreshold, double highThreshold) {
        double decimal = buyerPrice - (int) buyerPrice;

        if (buyerPrice >= highThreshold) {
            buyerPrice = highThreshold;

        } else if (buyerPrice < lowThreshold) {
            buyerPrice = buyerPrice - decimal;
            if (Double.compare(interval, 0.50) == 0) {
                if (decimal >= 0.50) {
                    decimal = 0.50;

                } else {
                    decimal = 0.0;
                }

            } else if (Double.compare(interval, 0.25) == 0) {
                if (decimal >= 0.01 && decimal < 0.10 && (int) buyerPrice == 0) {
                    decimal = 0.01;

                } else if (decimal >= 0.10 && decimal < 0.25 && (int) buyerPrice == 0) {
                    decimal = 0.10;

                } else if (decimal < 0.25) {
                    decimal = 0.00;

                } else if (decimal >= 0.25 && decimal < 0.50) {
                    decimal = 0.25;

                } else if (decimal >= 0.50 && decimal < 0.75) {
                    decimal = 0.50;

                } else {
                    decimal = 0.75;

                }
            }
            buyerPrice = buyerPrice + decimal;

        } else {
            buyerPrice = buyerPrice - decimal;

        }
        return buyerPrice;
    }
}
