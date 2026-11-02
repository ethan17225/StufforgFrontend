package stufforg.capstone.frontend

import android.app.Application
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import stufforg.capstone.frontend.core.data.StuffOrgRepository
import stufforg.capstone.frontend.core.network.NetworkFactory
import stufforg.capstone.frontend.core.network.SessionStore

fun CreationExtras.stuffOrgRepository(): StuffOrgRepository =
    (checkNotNull(this[APPLICATION_KEY]) as StuffOrgApplication).container.repository

class AppContainer(application: Application) {
    val json = NetworkFactory.createJson()
    val sessionStore = SessionStore(application, json)
    val okHttpClient = NetworkFactory.createOkHttpClient(
        sessionStore = sessionStore,
        baseUrl = BuildConfig.API_BASE_URL,
        json = json,
    )
    val apiService = NetworkFactory.createApiService(
        okHttpClient = okHttpClient,
        baseUrl = BuildConfig.API_BASE_URL,
        json = json,
    )
    val repository = StuffOrgRepository(
        api = apiService,
        sessionStore = sessionStore,
        json = json,
    )
}

class StuffOrgApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
