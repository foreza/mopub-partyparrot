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
        List<String> networksToInit = new ArrayList<String>();

//        networksToInit.add("com.mopub.mobileads.VungleRewardedVideo");

        SdkConfiguration sdkConfiguration = new SdkConfiguration.Builder("9cd3dce5635c45868a2e451c484dab0b")
                .withNetworksToInit(networksToInit)
                .build();

         // MoPub.initializeSdk(this, sdkConfiguration, initSdkListener());
        // ^ This currently causes issues. Not able to use this.

          AerServSdk.init(this, "380000");


        moPubView = (MoPubView) findViewById(R.id.adview);
        moPubView.setBannerAdListener(this);
        moPubView.setAdUnitId("9cd3dce5635c45868a2e451c484dab0b"); // Enter your Ad Unit ID from www.mopub.com
        moPubView.loadAd();

        // Loads an interstitial
        mInterstitial = new MoPubInterstitial(this, "b319d8ba93214763b61814284748800c");
        // Remember that "this" refers to your current activity.
        mInterstitial.setInterstitialAdListener(this);
        mInterstitial.load();
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
        Log.d(log, "Interstitial load failed");

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

            }
        };
    }



}
