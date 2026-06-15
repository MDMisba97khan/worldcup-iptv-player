package com.example.iptvplayer

import android.app.Activity
import android.widget.Toast
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.InterstitialAd
import com.google.android.gms.ads.InterstitialAdLoadCallback
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.banner.AdSize
import com.google.android.gms.ads.banner.AdView

/**
 * Thin wrapper around the Google Mobile Ads SDK.
 * Initialize once (e.g., in Application.onCreate) and use the static helpers.
 */
object AdMobHelper {

    // Call this once when your app starts (e.g., in a custom Application class)
    fun initialize(activity: Activity) {
        MobileAds.initialize(activity) {
            // Initialization complete
        }
    }

    /** Creates and loads a banner ad; returns the AdView ready to be added to a layout. */
    fun createBannerAd(activity: Activity, adUnitId: String): AdView {
        val adView = AdView(activity).apply {
            adSize = AdSize.BANNER
            adUnitId = adUnitId
            loadAd(AdRequest.Builder().build())
        }
        return adView
    }

    /** Loads and shows an interstitial ad; shows a toast on failure. */
    fun showInterstitial(activity: Activity, adUnitId: String, onClosed: (() -> Unit)? = null) {
        InterstitialAd.load(activity, adUnitId, AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    interstitialAd.fullScreenContentCallback = object : com.google.android.gms.ads.FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            interstitialAd = null
                            onClosed?.invoke()
                        }
                    }
                    interstitialAd.show(activity)
                }

                override fun onAdFailedToLoad(error: com.google.android.gms.ads.LoadAdError) {
                    Toast.makeText(activity, "Ad failed to load: $error", Toast.LENGTH_SHORT).show()
                }
            })
    }
}
