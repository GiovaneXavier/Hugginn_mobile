package com.srbr.huginn.di

import com.srbr.huginn.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * HMAC key for QR validation — injected from BuildConfig.
     * BuildConfig reads from local.properties or CI/CD env vars.
     * Never hardcoded in source.
     */
    @Provides
    @Singleton
    @Named("qrHmacKey")
    fun provideQrHmacKey(): String = BuildConfig.QR_HMAC_KEY

}
