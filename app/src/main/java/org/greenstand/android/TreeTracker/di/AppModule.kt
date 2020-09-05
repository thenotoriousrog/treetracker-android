package org.greenstand.android.TreeTracker.di

import android.content.Context
import android.location.LocationManager
import androidx.core.app.NotificationManagerCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.work.WorkManager
import com.google.firebase.analytics.FirebaseAnalytics
import org.greenstand.android.TreeTracker.analytics.Analytics
import org.greenstand.android.TreeTracker.api.ObjectStorageClient
import org.greenstand.android.TreeTracker.background.SyncNotificationManager
import org.greenstand.android.TreeTracker.managers.LanguageSwitcher
import org.greenstand.android.TreeTracker.managers.LocationDataCapturer
import org.greenstand.android.TreeTracker.managers.LocationUpdateManager
import org.greenstand.android.TreeTracker.managers.Preferences
import org.greenstand.android.TreeTracker.managers.UserManager
import org.greenstand.android.TreeTracker.usecases.BundleTreeUploadStrategy
import org.greenstand.android.TreeTracker.usecases.CreateFakeTreesUseCase
import org.greenstand.android.TreeTracker.usecases.CreatePlanterCheckInUseCase
import org.greenstand.android.TreeTracker.usecases.CreatePlanterInfoUseCase
import org.greenstand.android.TreeTracker.usecases.CreateTreeRequestUseCase
import org.greenstand.android.TreeTracker.usecases.CreateTreeUseCase
import org.greenstand.android.TreeTracker.usecases.DeleteOldPlanterImagesUseCase
import org.greenstand.android.TreeTracker.usecases.ExpireCheckInStatusUseCase
import org.greenstand.android.TreeTracker.usecases.PlanterCheckInUseCase
import org.greenstand.android.TreeTracker.usecases.RemoveLocalTreeImagesWithIdsUseCase
import org.greenstand.android.TreeTracker.usecases.SyncDataUseCase
import org.greenstand.android.TreeTracker.usecases.TreeUploadStrategy
import org.greenstand.android.TreeTracker.usecases.UploadImageUseCase
import org.greenstand.android.TreeTracker.usecases.UploadLocationDataUseCase
import org.greenstand.android.TreeTracker.usecases.UploadPlanterCheckInUseCase
import org.greenstand.android.TreeTracker.usecases.UploadPlanterInfoUseCase
import org.greenstand.android.TreeTracker.usecases.UploadPlanterUseCase
import org.greenstand.android.TreeTracker.usecases.UploadTreeBundleUseCase
import org.greenstand.android.TreeTracker.usecases.ValidateCheckInStatusUseCase
import org.greenstand.android.TreeTracker.utilities.DeviceUtils
import org.greenstand.android.TreeTracker.viewmodels.DataViewModel
import org.greenstand.android.TreeTracker.viewmodels.LoginViewModel
import org.greenstand.android.TreeTracker.viewmodels.MapViewModel
import org.greenstand.android.TreeTracker.viewmodels.NewTreeViewModel
import org.greenstand.android.TreeTracker.viewmodels.SignupViewModel
import org.greenstand.android.TreeTracker.viewmodels.TermsPolicyViewModel
import org.greenstand.android.TreeTracker.viewmodels.TreeHeightViewModel
import org.greenstand.android.TreeTracker.viewmodels.TreePreviewViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.android.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {

    viewModel { LoginViewModel(get(), get()) }

    viewModel { SignupViewModel() }

    viewModel { TermsPolicyViewModel(get(), get()) }

    viewModel { TreeHeightViewModel(get(), get(), get()) }

    viewModel { DataViewModel(get(), get(), get(), get()) }

    viewModel { MapViewModel(get(), get(), get(), get(), get(), get()) }

    viewModel { TreePreviewViewModel(get(), get()) }

    viewModel { NewTreeViewModel(get(), get(), get(), get(), get()) }

    single { WorkManager.getInstance(get()) }

    single { LocalBroadcastManager.getInstance(get()) }

    single { FirebaseAnalytics.getInstance(get()) }

    single { UserManager(get()) }

    single { Analytics(get(), get(), get()) }

    single { DeviceUtils }

    single { SyncNotificationManager(get(), get()) }

    single { androidContext().getSharedPreferences("org.greenstand.android", Context.MODE_PRIVATE) }

    single { androidContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager }

    single { LocationUpdateManager(get(), get()) }

    single { ObjectStorageClient.instance() }

    single { NotificationManagerCompat.from(get()) }

    single {
        LocationDataCapturer(
            get(),
            get(),
            get()
        )
    }

    single { Preferences(get(), get()) }

    factory { LanguageSwitcher(get()) }

    factory { UploadImageUseCase(get()) }

    factory { UploadLocationDataUseCase(get()) }

    factory { UploadPlanterUseCase(get(), get(), get(), get()) }

    factory { CreateTreeUseCase(get(), get(), get()) }

    factory { CreateFakeTreesUseCase(get(), get(), get(), get()) }

    factory { UploadPlanterCheckInUseCase(get(), get()) }

    factory { CreatePlanterInfoUseCase(get(), get(), get()) }

    factory { CreatePlanterCheckInUseCase(get(), get(), get(), get(), get()) }

    factory { ExpireCheckInStatusUseCase(get()) }

    factory { ValidateCheckInStatusUseCase(get()) }

    factory { PlanterCheckInUseCase(get(), get()) }

    factory { UploadPlanterInfoUseCase(get(), get()) }

    factory { CreateTreeRequestUseCase(get()) }

    factory { UploadTreeBundleUseCase(get(), get(), get(), get(), get()) }

    factory { RemoveLocalTreeImagesWithIdsUseCase(get()) }

    factory { DeleteOldPlanterImagesUseCase(get(), get()) }

    factory<TreeUploadStrategy> { BundleTreeUploadStrategy(get()) }

    factory { SyncDataUseCase(get(), get(), get(), get()) }
}
