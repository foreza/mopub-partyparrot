package party.parrot.partyparrot;

import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.aerserv.sdk.AerServSdk;
import com.mopub.common.MoPub;
import com.mopub.common.SdkConfiguration;
import com.mopub.common.SdkInitializationListener;
import com.mopub.mobileads.MoPubErrorCode;
import com.mopub.mobileads.MoPubInterstitial;
import com.mopub.mobileads.MoPubView;

import com.inmobi.plugin.mopub.IMAudienceBidder;        // Required for IM AB


import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class PartyMain extends AppCompatActivity implements MoPubView.BannerAdListener, MoPubInterstitial.InterstitialAdListener {

    private MoPubView moPubView;
    private MoPubInterstitial mInterstitial;
    private IMAudienceBidder inMobiAudienceBidder;
    public static String log = "PARROT";

    public String bannerAdUnitID =  "549952a8447d4911b8d690c21b66abac";
    public String interstitialAdUnitId = "2beb37597378451f85ef0bfba0cd7908";

    public Boolean supportAB = true;
    public Boolean firstAdLoad = true;             // A bool to let us know that the first ad load has completed.
    public String AB_BannerPLC = "1064948";
    public String AB_InterstitialPLC = "1064949";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_party_main);

        SdkConfiguration sdkConfiguration = new SdkConfiguration.Builder(bannerAdUnitID)
                .build();

        MoPub.initializeSdk(this, sdkConfiguration, initSdkListener());
        AerServSdk.init(this, "1017084");

        getDisplaySDKVersions();


        inMobiAudienceBidder = IMAudienceBidder.getInstance();     // Get the singleton instance of the IMAB


        moPubView = (MoPubView) findViewById(R.id.adview);
        moPubView.setBannerAdListener(this);
        moPubView.setAdUnitId(bannerAdUnitID);

        mInterstitial = new MoPubInterstitial(this, interstitialAdUnitId);
        mInterstitial.setInterstitialAdListener(this);
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



    public void loadBanner(View view){
        if (supportAB) {
            IMAB_updateBidForBanner();
            Log.d(log, "loadBanner -> Banner Loading, support Audience Bidder");
        } else {
            moPubView.loadAd();
            Log.d(log, "loadBanner -> Banner Loading");
        }
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


    // Method for Audience bidder for banners
    public void IMAB_updateBidForBanner(){

        if (moPubView != null) {
            inMobiAudienceBidder.updateBid(this, AB_BannerPLC, moPubView, new IMAudienceBidder.IMAudienceBidderBannerListener() {

                @Override
                public void onBidRecieved(@NonNull final MoPubView moPubAdView) {
                    moPubView = moPubAdView;
                    moPubView.loadAd();
                    Log.d(log, "IMAB_updateBidForBanner - onBidRecieved");
                }

                @Override
                public void onBidFailed(@NonNull MoPubView moPubAdView, @NonNull final Error error) {
                    moPubView.loadAd();
                    Log.d(log, "IMAB_updateBidForBanner - onBidFailed");
                }

            });
            Log.d(log, "IMAB_updateBidForBanner - loadBanner AB -> Banner Loading for Audience Bidder");
        } else {
            Log.d(log, "IMAB_updateBidForBanner FAILED - moPubView should be initialized and added to view");
        }


    }


    public void IMAB_updateBidForBannerRefresh(){

        if (moPubView != null) {
            inMobiAudienceBidder.updateBid(this, AB_BannerPLC, moPubView, new IMAudienceBidder.IMAudienceBidderBannerListener() {

                @Override
                public void onBidRecieved(@NonNull final MoPubView moPubAdView) {
                    moPubView = moPubAdView;
                    Log.d(log, "IMAB_updateBidForBannerRefresh - onBidRecieved");
                }

                @Override
                public void onBidFailed(@NonNull MoPubView moPubAdView, @NonNull final Error error) {
                     moPubView.loadAd();
                    Log.d(log, "IMAB_updateBidForBannerRefresh - onBidFailed");
                }

            });
            Log.d(log, "IMAB_updateBidForBanner - loadBanner AB -> Banner Loading for Audience Bidder");
        } else {
            Log.d(log, "IMAB_updateBidForBanner FAILED - moPubView should be initialized and added to view");
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

        if (!firstAdLoad) {     // If this is NOT our first ad load
            Log.d(log, "onBannerLoaded, not my first bid!");
            IMAB_updateBidForBannerRefresh();
        } else {
            Log.d(log, "onBannerLoaded, my little first bid!");
            delayedUpdateBid();
            firstAdLoad = false;
        }

    }

    public void delayedUpdateBid(){

        Timer timer = new Timer();
        timer.schedule(new TimerTask() {

            @Override
            public void run() {
                IMAB_updateBidForBannerRefresh();
            }
        }, 5000);

    }


    // Sent when the banner has failed to retrieve an ad. You can use the MoPubErrorCode value to diagnose the cause of failure.
    public void onBannerFailed(MoPubView banner, MoPubErrorCode errorCode){
        Log.d(log, "Banner failed to load, " + errorCode);

        // IMAB_updateBidForBanner(); // JC: The issue with this approach is that it'll restart the waterfall.
        IMAB_updateBidForBannerRefresh();
        Log.d(log, "onBannerFailed, updating AudienceBidder");

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




}
