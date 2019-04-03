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
import com.amazon.device.ads.DTBAdResponse;
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
    public String bannerAdUnitID = "549952a8447d4911b8d690c21b66abac";         // MoPub banner ad unit for 'Party Parrot' test app
    public String interstitialAdUnitId = "2beb37597378451f85ef0bfba0cd7908";    // Mopub interstitial ad unit for 'Party Parrot' test app
    public static String log = "PARROT";                                        // Log tag for ease of debugging

    /* IMAB required integration items */

    private IMAudienceBidder inMobiAudienceBidder;                              // Recommended we keep a reference to the IMAudienceBidder singleton


    public String AB_BannerPLC = "1064948";                                // InMobi AerServ platform Banner PLC to update the banner bid parameter
    public String AB_InterstitialPLC = "1064877";                               // InMobi AerServ platform Interstitial PLC to update the banner bid parameter
    private IMAudienceBidder.BidToken bannerBidToken = null;                    //

    public Boolean supportAB = true;                                            // Be able to toggle on / off Audience Bidder functionality.
    public Boolean bannerRefreshing = false;                                        // State variable to track the IMAB response


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_party_main);


        initializeAdSDK();

        configureBanner();
        // configureInterstitial();

        getDisplaySDKVersions();        // Update the view!
    }


    // Initialize any SDKs in this step
    public void initializeAdSDK() {

        // Required for MoPub integration
        SdkConfiguration sdkConfiguration = new SdkConfiguration.Builder(bannerAdUnitID)
                .withLogLevel(MoPubLog.LogLevel.DEBUG)
                .build();
        MoPub.initializeSdk(this, sdkConfiguration, initSdkListener());

        // Required for InMobi Audience Bidder integrations
        InMobiAudienceBidder.initialize(this, "1017084");
        inMobiAudienceBidder = IMAudienceBidder.getInstance();     // Get the singleton instance of the IMAB

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

        // Update the bid and save it to the bid token
        bannerBidToken = inMobiAudienceBidder.updateBid(this, AB_BannerPLC, moPubView, new IMAudienceBidder.IMAudienceBidderBannerListener() {

            @Override
            public void onBidRecieved(@NonNull final MoPubView m) {
                Log.d(log, "updateIMABForBanner - onBidRecieved. moPubAdView updated. Ready to load banner.");

                if (!bannerRefreshing) {
                    Log.d(log, "updateIMABForBanner - !bannerRefreshing, so loading Ad to begin mopub refresh.");
                    bannerRefreshing = true;
                    m.loadAd();
                }
            }

            @Override
            public void onBidFailed(@NonNull MoPubView m, @NonNull final Error error) {
                Log.d(log, "updateIMABForBanner - onBidFailed. Ready to load banner.");

                if (!bannerRefreshing) {
                    Log.d(log, "updateIMABForBanner - !bannerRefreshing, so loading Ad to begin mopub refresh.");
                    bannerRefreshing = true;
                    m.loadAd();
                }

            }

        });
        Log.d(log, "updateIMABForBanner - loadBanner AB -> Banner Loading for Audience Bidder");
    }



    /* Touch event for loading banner */
    public void loadBanner(View view){

        Log.d(log, "loadBanner called.");
        updateIMABForBanner();
    }




    public void configureInterstitial(){

        Log.d(log, "configureInterstitial called.");

        mInterstitial = new MoPubInterstitial(this, interstitialAdUnitId);
        mInterstitial.setInterstitialAdListener(this);

        // Audience Bidder
        /* Do something here */

    }



    public void loadInterstitial(View view) {
        if (supportAB){
            IMAB_updateBidForInterstitial();
            Log.d(log, "loadInterstitial -> Interstitial Loading, support Audience Bidder");

        } else {
            mInterstitial.load();
            Log.d(log, "loadInterstitial -> Interstitial Loading");
        }
    }



    // Method for updating the Audience bidder for interstitials
    public void IMAB_updateBidForInterstitial(){

        if (mInterstitial != null){
            inMobiAudienceBidder.updateBid(this, AB_InterstitialPLC, mInterstitial, new IMAudienceBidder.IMAudienceBidderInterstitialListener(){

                @Override
                public void onBidRecieved(@NonNull final MoPubInterstitial moPubInterstitial) {
                    moPubInterstitial.load();
                    Log.d(log, "IMAB_updateBidForInterstitial - onBidRecieved");
                }

                @Override
                public void onBidFailed(@NonNull MoPubInterstitial moPubInterstitial, @NonNull final Error error) {
                    moPubInterstitial.load();
                    Log.d(log, "IMAB_updateBidForInterstitial - onBidFailed");
                }

            });

            Log.d(log, "IMAB_updateBidForInterstitial - loadInterstitial AB -> Interstitial Loading for Audience Bidder");
        } else {
            Log.d(log, "IMAB_updateBidForInterstitial FAILED - mInterstitial should be initialized first");

        }

    }






    // Sent when the banner has successfully retrieved an ad.
    public void onBannerLoaded(MoPubView banner){
        Log.d(log, "Banner Loaded with KW: " + moPubView.getKeywords());
        // Update the bid right after the banner has been loaded.
        bannerBidToken.refreshBid(this, null, 5000);

    }



    // Sent when the banner has failed to retrieve an ad. You can use the MoPubErrorCode value to diagnose the cause of failure.
    public void onBannerFailed(MoPubView banner, MoPubErrorCode errorCode){
        Log.d(log, "Banner failed to load, " + errorCode);

        // Update the bid right after the banner has failed.
        bannerBidToken.refreshBid(this, null, 5000);



    }

    // Sent when the user has tapped on the banner.
    public void onBannerClicked(MoPubView banner){
        Log.d(log, "Banner clicked");

    }

    // Sent when the banner has just taken over the screen.
    public void onBannerExpanded(MoPubView banner){
        Log.d(log, "Banner expanded");
        moPubView.setAutorefreshEnabled(false);

    }

    // Sent when an expanded banner has collapsed back to its original size.
    public void onBannerCollapsed(MoPubView banner){
        Log.d(log, "Banner collapsed");
        moPubView.setAutorefreshEnabled(true);

    }



    // InterstitialAdListener methods

    @Override
    public void onInterstitialLoaded(MoPubInterstitial interstitial) {
        // The interstitial has been cached and is ready to be shown.

        Log.d(log, "An Interstitial was loaded with kw: " + interstitial.getKeywords());
            mInterstitial.show();
    }

    @Override
    public void onInterstitialFailed(MoPubInterstitial interstitial, MoPubErrorCode errorCode) {
        // The interstitial has failed to load. Inspect errorCode for additional information.
        // This is an excellent place to load more ads.
        Log.d(log, "Interstitial load failed: " + errorCode);

    }

    @Override
    public void onInterstitialShown(MoPubInterstitial interstitial) {
        // The interstitial has been shown. Pause / save state accordingly.
        Log.d(log, "Interstitial shown");

    }

    @Override
    public void onInterstitialClicked(MoPubInterstitial interstitial) {
        Log.d(log, "Interstitial clicked");

    }

    @Override
    public void onInterstitialDismissed(MoPubInterstitial interstitial) {
        // The interstitial has being dismissed. Resume / load state accordingly.
        // This is an excellent place to load more ads.
        Log.d(log, "Interstitial Dismissed");

    }



    @Override
    protected void onDestroy() {
        mInterstitial.destroy();
        super.onDestroy();
    }


    public void getDisplaySDKVersions(){

        TextView mpv = findViewById(R.id.MPSdkVersion);
        mpv.setText("MoPub SDK Version:" + MoPub.SDK_VERSION);

        TextView imv = findViewById(R.id.IMSdkVersion);
        imv.setText("IM SDK Version:" + AerServSdk.getSdkVersion());

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



}
