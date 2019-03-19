package party.parrot.partyparrot;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.aerserv.sdk.AerServSdk;
import com.mopub.common.MoPub;
import com.mopub.common.SdkConfiguration;
import com.mopub.common.SdkInitializationListener;
import com.mopub.common.logging.MoPubLog;
import com.mopub.mobileads.MoPubErrorCode;
import com.mopub.mobileads.MoPubInterstitial;
import com.mopub.mobileads.MoPubView;

import java.util.ArrayList;
import java.util.List;

public class PartyMain extends AppCompatActivity implements MoPubView.BannerAdListener, MoPubInterstitial.InterstitialAdListener {

    private MoPubView moPubView;
    private MoPubInterstitial mInterstitial;
    public static String log = "PARROT";
    public String bannerAdUnitID =  "549952a8447d4911b8d690c21b66abac";
    public String interstitialAdUnitId = "2beb37597378451f85ef0bfba0cd7908";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_party_main);

        SdkConfiguration sdkConfiguration = new SdkConfiguration.Builder(bannerAdUnitID)
                .build();

        MoPub.initializeSdk(this, sdkConfiguration, initSdkListener());
        AerServSdk.init(this, "1017084");
        getDisplaySDKVersions();

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
        if (moPubView != null) {
            moPubView.loadAd();
            Log.d(log, "loadBanner -> Banner Loading");

        }
    }


    public void loadInterstitial(View view) {
        if (mInterstitial != null){
            mInterstitial.load();
            Log.d(log, "loadInterstitial -> Interstitial Loading");
        }
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
