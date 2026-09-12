package com.samdori93.yeoksadam

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/** Hilt 진입점. (FCM 토큰 등록·지오펜스 재등록 등 초기화는 추후 추가) */
@HiltAndroidApp
class YeoksadamApp : Application()
