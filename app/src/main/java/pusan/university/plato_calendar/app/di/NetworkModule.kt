package pusan.university.plato_calendar.app.di

import android.content.Context
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.CookieJar
import okhttp3.JavaNetCookieJar
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import pusan.university.plato_calendar.BuildConfig
import pusan.university.plato_calendar.BuildConfig.PLATO_BASE_URL
import pusan.university.plato_calendar.BuildConfig.PNU_BASE_URL
import pusan.university.plato_calendar.app.network.NetworkConnectionInterceptor
import retrofit2.Retrofit
import java.net.CookieManager
import javax.inject.Qualifier
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Singleton
    @Provides
    fun provideCookieManager(): CookieManager = CookieManager()

    @Singleton
    @Provides
    fun provideCookieJar(cookieManager: CookieManager): CookieJar = JavaNetCookieJar(cookieManager)

    @Singleton
    @Provides
    fun provideNetworkConnectionInterceptor(
        @ApplicationContext context: Context,
    ): NetworkConnectionInterceptor = NetworkConnectionInterceptor(context)

    @Singleton
    @Provides
    @Redirect
    fun provideReDirectOkHttpClient(
        cookieJar: CookieJar,
        networkConnectionInterceptor: NetworkConnectionInterceptor,
    ): OkHttpClient {
        val logging =
            HttpLoggingInterceptor().apply {
                level =
                    if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
            }

        return OkHttpClient
            .Builder()
            .cookieJar(cookieJar)
            .addInterceptor(networkConnectionInterceptor)
            .addInterceptor(logging)
            .build()
    }

    @Singleton
    @Provides
    @NonDirect
    fun provideNonDirectOkHttpClient(
        cookieJar: CookieJar,
        networkConnectionInterceptor: NetworkConnectionInterceptor,
    ): OkHttpClient {
        val logging =
            HttpLoggingInterceptor().apply {
                level =
                    if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
            }

        return OkHttpClient
            .Builder()
            .cookieJar(cookieJar)
            .addInterceptor(networkConnectionInterceptor)
            .addInterceptor(logging)
            .followRedirects(false)
            .build()
    }

    @Singleton
    @Provides
    fun provideJson(): Json =
        Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
            encodeDefaults = true
        }

    @Singleton
    @Provides
    @Plato
    fun providePlatoRetrofit(
        @Redirect okHttpClient: OkHttpClient,
        json: Json,
    ): Retrofit =
        Retrofit
            .Builder()
            .baseUrl(PLATO_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Singleton
    @Provides
    @PlatoNonDirect
    fun providePlatoNonDirectRetrofit(
        @NonDirect okHttpClient: OkHttpClient,
        json: Json,
    ): Retrofit =
        Retrofit
            .Builder()
            .baseUrl(PLATO_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Singleton
    @Provides
    @Pnu
    fun providePnuRetrofit(
        @Redirect okHttpClient: OkHttpClient,
        json: Json,
    ): Retrofit =
        Retrofit
            .Builder()
            .baseUrl(PNU_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
}

@Qualifier
annotation class Plato

@Qualifier
annotation class PlatoNonDirect

@Qualifier
annotation class Pnu

@Qualifier
annotation class NonDirect

@Qualifier
annotation class Redirect
