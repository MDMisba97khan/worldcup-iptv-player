package com.example.iptvplayer

import android.app.Activity
import android.widget.Toast
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

object AdMobHelper {

    fun initialize(activity: Activity) {
        MobileAds.initialize(activity) {}
    }

    fun createBannerAd(activity: Activity, adUnitId: String): AdView {
        return AdView(activity).also {
            it.adUnitId = adUnitId
            it.loadAd(AdRequest.Builder().build())
        }
    }

    fun showInterstitial(
        activity: Activity,
        adUnitId: String,
        onClosed: (() -> Unit)? = null
    ) {
        InterstitialAd.load(
            activity,
            adUnitId,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    interstitialAd.fullScreenContentCallback =
                        object : com.google.android.gms.ads.FullScreenContentCallback() {
                            override fun onAdDismissedFullScreenContent() {
                                onClosed?.invoke()
                            }

                            override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                                Toast.makeText(
                                    activity,
                                    "Ad failed to show: ${adError.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                                onClosed?.invoke()
                            }
                        }
                    interstitialAd.show(activity)
                }

                override fun onAdFailedToLoad(loadAdError: com.google.android.gms.ads.LoadAdError) {
                    Toast.makeText(
                        activity,
                        "Ad failed to load: ${loadAdError.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    onClosed?.invoke()
                }
            }
        )
    }
}
