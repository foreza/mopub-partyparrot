package party.parrot.partyparrot;

import android.content.Context;
import android.os.Debug;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.aerserv.sdk.AerServSdk;

import com.amazon.device.ads.AdError;
import com.amazon.device.ads.AdRegistration;
import com.amazon.device.ads.DTBAdCallback;
import com.amazon.device.ads.DTBAdRequest;
import com.amazon.device.ads.DTBAdResponse;
import com.amazon.device.ads.DTBAdSize;
import com.inmobi.ads.InMobiAudienceBidder;
import com.inmobi.plugin.mopub.IMABCustomEventBanner;
import com.mopub.common.MoPub;
import com.mopub.common.SdkConfiguration;
import com.mopub.common.SdkInitializationListener;
import com.mopub.common.logging.MoPubDefaultLogger;
import com.mopub.common.logging.MoPubLog;
import com.mopub.mobileads.MoPubErrorCode;
import com.mopub.mobileads.MoPubInterstitial;
import com.mopub.mobileads.MoPubView;

import com.inmobi.plugin.mopub.IMAudienceBidder;        // Required for IM AB


import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;

public class PartyMain extends AppCompatActivity implements MoPubView.BannerAdListener, MoPubInterstitial.InterstitialAdListener {

    /* MoPub standard integration variables */

    private MoPubView moPubView;                                                // MoPub banner view
    private MoPubInterstitial mInterstitial;                                    // MoPub interstitial view
    private Boolean interstitialReady = false;
    public String bannerAdUnitID = "549952a8447d4911b8d690c21b66abac";         // MoPub banner ad unit for 'Party Parrot' test app
    public String interstitialAdUnitId = "2beb37597378451f85ef0bfba0cd7908";    // Mopub interstitial ad unit for 'Party Parrot' test app
    public static String log = "PARROT";                                        // Log tag for ease of debugging

    /* IMAB required integration items */

    private IMAudienceBidder inMobiAudienceBidder;                              // Recommended we keep a reference to the IMAudienceBidder singleton

    public String AB_APPID = "1020421";
    public String AB_BannerPLC = "1064941";        //1064948                         // InMobi AerServ platform Banner PLC to update the banner bid parameter
    public String AB_InterstitialPLC = "1064877";                               // InMobi AerServ platform Interstitial PLC to update the banner bid parameter

    private IMAudienceBidder.BidToken bannerBidToken;                    //
    private IMAudienceBidder.BidToken interstitialBidToken;
    private int bidTimeOut = 10000;

    public Boolean supportAB = true;                                            // Be able to toggle on / off Audience Bidder functionality.
    public Boolean bannerLoaded = false;                                        // State variable to track the IMAB response

    public Boolean supportAPS = true;

    public String APS_Banner_APPID = "a9_onboarding_app_id";
    public String APS_Banner_SLOTID = "5ab6a4ae-4aa5-43f4-9da4-e30755f2b295";

    public String APS_Interstitial_APPID = "a9_onboarding_app_id";
    public String APS_Interstitial_SLOTID = "4e918ac0-5c68-4fe1-8d26-4e76e8f74831";

    public String APS_Video_APPID = "bd4fd892fc8b4f59bc2f6fa6c326a041";
    public String APS_Video_SLOTID = "288ad904-d0ab-44c4-8901-0cf43b3382e6";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_party_main);


        initializeAdSDK();

        configureBanner();
        configureInterstitial();

        getDisplaySDKVersions();        // Update the view!
    }

    private SdkInitializationListener initSdkListener() {
        return new SdkInitializationListener() {
            @Override
            public void onInitializationFinished() {
                //  MoPub SDK initialized.
                Log.d(log, "MoPub SDK initialized");

            }
        };
    }


    // Initialize any SDKs in this step
    public void initializeAdSDK() {

        // Required for MoPub integration
        SdkConfiguration sdkConfiguration = new SdkConfiguration.Builder(bannerAdUnitID)
                .withLogLevel(MoPubLog.LogLevel.DEBUG)
                .build();

        MoPub.initializeSdk(this, sdkConfiguration, initSdkListener());

        // Required for InMobi Audience Bidder integrations
        InMobiAudienceBidder.initialize(this, AB_APPID);
        inMobiAudienceBidder = IMAudienceBidder.getInstance();     // Get the singleton instance of the IMAB

        // Required if APS is included
        if (supportAPS) {
            AdRegistration.getInstance(APS_Banner_APPID, this);
            AdRegistration.enableLogging(true);
            AdRegistration.enableTesting(true);
            AdRegistration.useGeoLocation(true);
        }


    }

    // Call this once to initially configure the banner
    public void configureBanner() {

        Log.d(log, "configureBanner called.");

        moPubView = (MoPubView) findViewById(R.id.adview);
        moPubView.setBannerAdListener(this);
        moPubView.setAdUnitId(bannerAdUnitID);

    }


    // Will update the banner bid for the IMAB class.
    public void updateIMABForBanner() {

        Log.d(log, "updateIMABForBanner has been called.");

        // Create the loader and set the APS params
        final DTBAdRequest loader = new DTBAdRequest();
        loader.setSizes(new DTBAdSize(320, 50, APS_Banner_SLOTID));

        // Call load and specify callbacks

        loader.loadAd(new DTBAdCallback() {
            @Override
            public void onFailure(AdError adError) {

                // Bid should continue without APS. Update the bid.
                bannerBidToken = inMobiAudienceBidder.createBidToken(AB_BannerPLC,
                        moPubView, new IMAudienceBidder.IMAudienceBidderBannerListener() {

                    @Override
                    public void onBidRecieved(@NonNull final MoPubView m) {
                        // Bid was received from Audience Bidder. Call loadAd on the updated bid object.
                        // Note: If banner refresh is supported, we recommend that you keep track of this and only call loadAd the first time to kick off the refresh process.

                        if (!bannerLoaded) {
                            bannerLoaded = true;
                            m.loadAd();
                        }

                    }

                    @Override
                    public void onBidFailed(@NonNull MoPubView m, @NonNull final Error error) {
                        // No Bid received from Audience Bidder. Call loadAd on the bid object.
                        // Note: If banner refresh is supported, we recommend that you keep track of this and only call loadAd the first time to kick off the refresh process.

                        if (!bannerLoaded) {
                            bannerLoaded = true;
                            m.loadAd();
                        }

                    }

                });

            }

            @Override
            public void onSuccess(DTBAdResponse dtbAdResponse) {

                // Bid should continue with APS. Create the bid token. (update the bid token with the response after)
                bannerBidToken = inMobiAudienceBidder.createBidToken(AB_BannerPLC,
                        moPubView, new IMAudienceBidder.IMAudienceBidderBannerListener() {

                    @Override
                    public void onBidRecieved(@NonNull final MoPubView m) {

                        // Bid was received from Audience Bidder. Call loadAd on the updated bid object.
                        // Note: If banner refresh is supported, we recommend that you keep track of this and only call loadAd the first time to kick off the refresh process.

                        if (!bannerLoaded) {
                            bannerLoaded = true;
                            m.loadAd();
                        }
                    }

                    @Override
                    public void onBidFailed(@NonNull MoPubView m, @NonNull final Error error) {

                        // No Bid received from Audience Bidder. Call loadAd on the bid object.
                        // Note: If banner refresh is supported, we recommend that you keep track of this and only call loadAd the first time to kick off the refresh process.

                        if (!bannerLoaded) {
                            bannerLoaded = true;
                            m.loadAd();
                        }

                    }
                });

                // Add the APS ad response we received to the bid token
                bannerBidToken.setDTBAdResponse(dtbAdResponse);
            }

        });
    }


    /* Touch event for loading banner */
    public void loadBanner(View view) {

        Log.d(log, "loadBanner called.");
        updateIMABForBanner();
    }


    // Function to call the DTBAdloader again on the same banner slot to get a new ad, and then refresh the banner bid token
    public void IMAB_refreshBannerBidForAPS(){

        Log.d(log, "IMAB_refreshBannerBidForAPS called");

        DTBAdRequest refresher = new DTBAdRequest();
        refresher.setSizes(new DTBAdSize(320, 50, APS_Banner_SLOTID));

        refresher.loadAd(new DTBAdCallback() {
            @Override
            public void onFailure(AdError adError) {

                // No new ad from APS. Update bid for the next ad call
                bannerBidToken.updateBid(PartyMain.this, bidTimeOut);

            }

            @Override
            public void onSuccess(DTBAdResponse dtbAdResponse) {

                // We have a new ad from APS. Store that in the bid token, then update bid for the next ad call
                bannerBidToken.setDTBAdResponse(dtbAdResponse);
                bannerBidToken.updateBid(PartyMain.this, bidTimeOut);
            }
        });


    }

    // Sent when the banner has successfully retrieved an ad.
    public void onBannerLoaded(MoPubView banner) {
        Log.d(log, "Banner Loaded with KW: " + moPubView.getKeywords());

        // We will need to make a request to A9 each time the banner refreshes.
        IMAB_refreshBannerBidForAPS();

    }


    // Sent when the banner has failed to retrieve an ad. You can use the MoPubErrorCode value to diagnose the cause of failure.
    public void onBannerFailed(MoPubView banner, MoPubErrorCode errorCode) {
        Log.d(log, "Banner failed to load, " + errorCode);

        // bannerBidToken.refreshBid(this, APS_RESPONSE, 5000);
        IMAB_refreshBannerBidForAPS();
    }

    // Sent when the user has tapped on the banner.
    public void onBannerClicked(MoPubView banner) {
        Log.d(log, "Banner clicked");

    }

    // Sent when the banner has just taken over the screen.
    public void onBannerExpanded(MoPubView banner) {
        Log.d(log, "Banner expanded");
        moPubView.setAutorefreshEnabled(false);

    }

    // Sent when an expanded banner has collapsed back to its original size.
    public void onBannerCollapsed(MoPubView banner) {
        Log.d(log, "Banner collapsed");
        moPubView.setAutorefreshEnabled(true);

    }


    public void configureInterstitial() {

        Log.d(log, "configureInterstitial called.");

        mInterstitial = new MoPubInterstitial(this, interstitialAdUnitId);
        mInterstitial.setInterstitialAdListener(this);

    }


    public void loadInterstitial(View view) {
        if (supportAB) {
            IMAB_updateBidForInterstitial();
            Log.d(log, "loadInterstitial -> Interstitial Loading, support Audience Bidder");

        } else {
            mInterstitial.load();
            Log.d(log, "loadInterstitial -> Interstitial Loading");
        }
    }

    public void showInterstitial(View view){

        if (mInterstitial != null && mInterstitial.isReady()) {
            mInterstitial.show();
        }

    }


    // Method for updating the Audience bidder for interstitials



    public void IMAB_updateBidForInterstitial() {

        Log.d(log, "updateIMABForBanner has been called.");

        // Create the loader and set the APS params
        final DTBAdRequest loader = new DTBAdRequest();
        loader.setSizes(new DTBAdSize.DTBInterstitialAdSize(APS_Interstitial_SLOTID));

        loader.loadAd(new DTBAdCallback() {
            @Override
            public void onFailure(AdError adError) {

                // Bid should continue without APS. Update the bid.
                interstitialBidToken = inMobiAudienceBidder.createBidToken(
                        AB_InterstitialPLC, mInterstitial, new IMAudienceBidder.IMAudienceBidderInterstitialListener() {

                            @Override
                            public void onBidRecieved(@NonNull final MoPubInterstitial m) {

                                // Bid was received from Audience Bidder. Call load on the updated bid object.
                                m.load();
                            }

                            @Override
                            public void onBidFailed(@NonNull MoPubInterstitial m, @NonNull final Error error) {

                                // No Bid received from Audience Bidder. Call load on the bid object.
                                m.load();
                            }

                        });

            }

            @Override
            public void onSuccess(DTBAdResponse dtbAdResponse) {

                // Bid should continue with APS. Create the bid token. (update the bid token with the response after)
                interstitialBidToken = inMobiAudienceBidder.createBidToken(
                        AB_InterstitialPLC, mInterstitial, new IMAudienceBidder.IMAudienceBidderInterstitialListener() {

                            @Override
                            public void onBidRecieved(@NonNull final MoPubInterstitial m) {

                                // Bid was received from Audience Bidder. Call load on the updated bid object.
                                m.load();
                            }

                            @Override
                            public void onBidFailed(@NonNull MoPubInterstitial m, @NonNull final Error error) {

                                // No Bid received from Audience Bidder. Call load on the bid object.
                                m.load();
                            }

                        });

                // Add the APS ad response we received to the bid token
                interstitialBidToken.setDTBAdResponse(dtbAdResponse);

            }



        });

        Log.d(log, "IMAB_updateBidForInterstitial - loadInterstitial AB -> Interstitial Loading for Audience Bidder");
    }

    public void IMAB_refreshInterstitialBidForAPS() {


        Log.d(log, "IMAB_refreshInterstitialBidForAPS called");


        DTBAdRequest interstitialRefresher = new DTBAdRequest();
        interstitialRefresher.setSizes(new DTBAdSize.DTBInterstitialAdSize(APS_Interstitial_SLOTID));


        interstitialRefresher.loadAd(new DTBAdCallback() {
            @Override
            public void onFailure(AdError adError) {

                // No new ad from APS. Ensure the ad response is null, then update bid for the next ad call
                interstitialBidToken.updateBid(PartyMain.this,bidTimeOut);

            }

            @Override
            public void onSuccess(DTBAdResponse dtbAdResponse) {

                // We have a new ad from APS. Store that in the bid token, then update bid for the next ad call
                interstitialBidToken.setDTBAdResponse(dtbAdResponse);
                interstitialBidToken.updateBid(PartyMain.this,bidTimeOut);

            }
        });


    }


    // InterstitialAdListener methods

    @Override
    public void onInterstitialLoaded(MoPubInterstitial interstitial) {
        // The interstitial has been cached and is ready to be shown.
        Log.d(log, "An Interstitial was loaded with kw: " + interstitial.getKeywords());
        interstitialReady = true;
        // IMAB_refreshInterstitialBidForAPS();
    }

    @Override
    public void onInterstitialFailed(MoPubInterstitial interstitial, MoPubErrorCode errorCode) {
        // The interstitial has failed to load. Inspect errorCode for additional information.
        // This is an excellent place to load more ads.
        Log.d(log, "Interstitial load failed: " + errorCode);
        // IMAB_refreshInterstitialBidForAPS();

    }

    @Override
    public void onInterstitialShown(MoPubInterstitial interstitial) {
        // The interstitial has been shown. Pause / save state accordingly.
        Log.d(log, "Interstitial shown ");
        interstitialReady = false;


    }

    @Override
    public void onInterstitialClicked(MoPubInterstitial interstitial) {
        Log.d(log, "Interstitial clicked");

    }

    @Override
    public void onInterstitialDismissed(MoPubInterstitial interstitial) {
        // The interstitial has being dismissed. Resume / load state accordingly.
        // This is an excellent place to load more ads.
        Log.d(log, "Interstitial Dismissed, refreshing bid");

        IMAB_refreshInterstitialBidForAPS();

    }


    @Override
    protected void onDestroy() {
        mInterstitial.destroy();
        super.onDestroy();
    }


    public void getDisplaySDKVersions() {

        TextView mpv = findViewById(R.id.MPSdkVersion);
        mpv.setText("MoPub SDK Version:" + MoPub.SDK_VERSION);

        TextView imv = findViewById(R.id.IMSdkVersion);
        imv.setText("IM SDK Version:" + AerServSdk.getSdkVersion());

    }

}


