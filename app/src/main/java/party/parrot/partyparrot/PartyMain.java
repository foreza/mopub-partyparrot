package party.parrot.partyparrot;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.aerserv.sdk.AerServSdk;
import com.mopub.common.MoPub;
import com.mopub.common.SdkConfiguration;
import com.mopub.common.SdkInitializationListener;
import com.mopub.mobileads.MoPubErrorCode;
import com.mopub.mobileads.MoPubInterstitial;
import com.mopub.mobileads.MoPubView;

import java.util.ArrayList;
import java.util.List;

public class PartyMain extends AppCompatActivity implements MoPubView.BannerAdListener, MoPubInterstitial.InterstitialAdListener {

    private MoPubView moPubView;

    private MoPubInterstitial mInterstitial;

    static String log = "PARROT";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_party_main);

       //  A list of rewarded video adapters to initialize
        List<String> networksToInit = new ArrayList();
        // networksToInit.add("com.mopub.mobileads.AerServCustomEventBanner");


        /*
        - MoPub Plugin Android: 81aaafbeefd74cd787ca26041c6c8833 (has no impressions, not sure if works)
        - MoPub Android Test Placement: 73bd31810f624db398332fa95e2d058d (serves a raw creative that we uploaded, works for sure)
         */

        SdkConfiguration sdkConfiguration = new SdkConfiguration.Builder("81aaafbeefd74cd787ca26041c6c8833")
                .withNetworksToInit(networksToInit)
                .build();

        MoPub.initializeSdk(this, sdkConfiguration, initSdkListener());


        /*
        - MocoSpace APP ID: 103156
        -

         */
        AerServSdk.init(this, "103156"); // ibugs, should hopefully init. if not, use


        /*
        Placements to try..
        1029840 - Publisher name: aerserv, Application name: My_Test_App, Placement name: iOSMopubPluginTest
         */


        /*
        AerServ Plugin Interstitial (replacement) : (https://app.mopub.com/advertise/line_items/ee62d34418eb4863bd663c59c3ba4a14/edit/)
        data optional: {"placement": "1029840", "appId": "1005911"}

        will try changing it to something i can actually control / redirect to a customer so i can test

                data optional: {"placement": "1007766", "appId": "103156"}



         */


        moPubView = (MoPubView) findViewById(R.id.adview);
        moPubView.setBannerAdListener(this);

        // This sample ad unit is from the MoPubSample aerserv application for an android banner: d73714cdd28d43bbbada62c6aaedfcc7
        // Android AerServ Mediated Banner

        /*

        MoPub Plugin Android - Banner Ad: 9fe062ebcd89471cb09caa57c4add202

         */

        moPubView.setAdUnitId("9fe062ebcd89471cb09caa57c4add202"); // Enter your Ad Unit ID from www.mopub.com
//        moPubView.loadAd();


         // 6482ffa195a54c369e7bea3288919f4b - MoPubSample sample interstitial // Android AerServ Mediated Interstitial
        // Loads an interstitial


        /*
        - MoPub Plugin Android, AerServ Fullscreen - a3fa142159ec45338c586765ec44c0f0
        - MoPub Android Test Placement, AerServ Interstitial_2 - 5786ec14412744a4b0f9a706bbc0cd8d

         */
        mInterstitial = new MoPubInterstitial(this, "a3fa142159ec45338c586765ec44c0f0");
        // Remember that "this" refers to your current activity.
        mInterstitial.setInterstitialAdListener(this);
        // mInterstitial.load();
}



    // Sent when the banner has successfully retrieved an ad.
    public void onBannerLoaded(MoPubView banner){
        Log.d(log, "Banner Loaded");
    }

    // Sent when the banner has failed to retrieve an ad. You can use the MoPubErrorCode value to diagnose the cause of failure.
    public void onBannerFailed(MoPubView banner, MoPubErrorCode errorCode){
        Log.d(log, "Banner failed to load, " + errorCode);

    }

    // Sent when the user has tapped on the banner.
    public void onBannerClicked(MoPubView banner){
        Log.d(log, "Banner clicked");

    }

    // Sent when the banner has just taken over the screen.
    public void onBannerExpanded(MoPubView banner){
        Log.d(log, "Banner expanded");

    }

    // Sent when an expanded banner has collapsed back to its original size.
    public void onBannerCollapsed(MoPubView banner){
        Log.d(log, "Banner collapsed");

    }

    // INTERSTITIALS


//    // Defined by your application, indicating that you're ready to show an interstitial ad.
//    void yourAppsShowInterstitialMethod() {
//        if (mInterstitial.isReady()) {
//            mInterstitial.show();
//        } else {
//            // Caching is likely already in progress if `isReady()` is false.
//            // Avoid calling `load()` here and instead rely on the callbacks as suggested below.
//        }
//    }

    // InterstitialAdListener methods
    @Override
    public void onInterstitialLoaded(MoPubInterstitial interstitial) {
        // The interstitial has been cached and is ready to be shown.
        Log.d(log, "Interstitial loaded");

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

    private SdkInitializationListener initSdkListener() {
        return new SdkInitializationListener() {
            @Override
            public void onInitializationFinished() {
           /* MoPub SDK initialized.
           Check if you should show the consent dialog here, and make your ad requests. */

                Log.d(log, "MoPub SDK init");

                // mInterstitial.load();
                moPubView.loadAd();

            }
        };
    }


}
