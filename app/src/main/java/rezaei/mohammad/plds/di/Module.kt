package rezaei.mohammad.plds.di

import androidx.lifecycle.MutableLiveData
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import rezaei.mohammad.plds.BuildConfig
import rezaei.mohammad.plds.data.local.LocalRepository
import rezaei.mohammad.plds.data.local.PLDSDatabase
import rezaei.mohammad.plds.data.preference.PreferenceManager
import rezaei.mohammad.plds.data.remote.ApiInterface
import rezaei.mohammad.plds.data.remote.AuthInterceptor
import rezaei.mohammad.plds.data.remote.RefreshTokenInterceptor
import rezaei.mohammad.plds.data.remote.RemoteRepository
import rezaei.mohammad.plds.views.addMultiDoc.AddMultiDocViewModel
import rezaei.mohammad.plds.views.docProgress.DocProgressViewModel
import rezaei.mohammad.plds.views.getDocReference.GetDocReferenceViewModel
import rezaei.mohammad.plds.views.login.LoginViewModel
import rezaei.mohammad.plds.views.main.GlobalViewModel
import rezaei.mohammad.plds.views.reportIssue.ReportIssueViewModel
import rezaei.mohammad.plds.views.submitForm.SubmitFormViewModel

object Module {
    val pldsModule = module {

        //ok http
        single {
            OkHttpClient.Builder().apply {
                addInterceptor(HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                })
                addInterceptor(AuthInterceptor(get()))
                addInterceptor(
                    RefreshTokenInterceptor(
                        lazy { get<PreferenceManager>() },
                        lazy { get<RemoteRepository>() })
                )
            }.build()
        }

        //retrofit
        single {
            Retrofit.Builder()
                .baseUrl(BuildConfig.BaseUrl)
                .client(get())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiInterface::class.java)
        }

        //remote repository
        single {
            RemoteRepository(get())
        }

        //pref manager
        single { PreferenceManager(androidContext()) }

        //database dao
        single { PLDSDatabase(androidContext()).PLDSDao() }

        //local repository
        single { LocalRepository(get()) }

        viewModel { LoginViewModel(get(), get()) }

        viewModel { GlobalViewModel() }

        viewModel { (docRefNo: MutableLiveData<String>) ->
            GetDocReferenceViewModel(
                get(),
                docRefNo
            )
        }

        viewModel { DocProgressViewModel(get()) }

        viewModel { (docRefNo: MutableLiveData<String>) -> AddMultiDocViewModel(get(), docRefNo) }

        viewModel { SubmitFormViewModel(get(), get()) }

        viewModel { ReportIssueViewModel(get(), get()) }
    }
}