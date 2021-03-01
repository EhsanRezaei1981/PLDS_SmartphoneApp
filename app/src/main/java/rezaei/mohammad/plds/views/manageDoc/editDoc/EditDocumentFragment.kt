package rezaei.mohammad.plds.views.manageDoc.editDoc

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.yayandroid.locationmanager.LocationManager
import com.yayandroid.locationmanager.configuration.DefaultProviderConfiguration
import com.yayandroid.locationmanager.configuration.GooglePlayServicesConfiguration
import com.yayandroid.locationmanager.configuration.LocationConfiguration
import com.yayandroid.locationmanager.configuration.PermissionConfiguration
import com.yayandroid.locationmanager.listener.LocationListener
import kotlinx.android.synthetic.main.fragment_edit_document.*
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import rezaei.mohammad.plds.R
import rezaei.mohammad.plds.data.ApiResult
import rezaei.mohammad.plds.data.model.request.FormResult
import rezaei.mohammad.plds.data.model.request.GetFileRequest
import rezaei.mohammad.plds.data.model.request.Gps
import rezaei.mohammad.plds.data.model.response.CourtResponse
import rezaei.mohammad.plds.data.model.response.FormResponse
import rezaei.mohammad.plds.data.model.response.SheriffResponse
import rezaei.mohammad.plds.databinding.FragmentEditDocumentBinding
import rezaei.mohammad.plds.formBuilder.ElementParser
import rezaei.mohammad.plds.formBuilder.ElementsActivityRequestCallback
import rezaei.mohammad.plds.util.EventObserver
import rezaei.mohammad.plds.util.snack
import rezaei.mohammad.plds.util.tryNavigate

class EditDocumentFragment : Fragment() {

    private val viewModel: EditDocumentViewModel by viewModel()
    private lateinit var viewDataBinding: FragmentEditDocumentBinding
    private val args: EditDocumentFragmentArgs by navArgs()
    private lateinit var elementParser: ElementParser
    private lateinit var imageResult: MutableLiveData<Intent>
    private var selectedGps: Pair<Double, Double>? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewDataBinding = FragmentEditDocumentBinding.inflate(inflater, container, false)
            .apply {
                this.viewmodel = viewModel
                this.lifecycleOwner = this@EditDocumentFragment.viewLifecycleOwner
            }

        if (args.readOnly || args.type == "UnSuccess") {
            viewDataBinding.rootView.removeView(viewDataBinding.btnSubmit)
            viewDataBinding.txtCardTitle.text = getString(R.string.responded_fields)
        }
        return viewDataBinding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setGetFieldsResult()
        if (!args.readOnly) {
            setupSubmitEvent()
            setupSubmitFormEvent()
        }
        if (viewModel.fieldsResult.value == null)
            viewModel.getRespondedFields(
                args.DocumentStatusId,
                args.VT,
                args.readOnly,
                args.type,
                arguments?.getInt("DocumentStatusQueryId"),
                arguments?.getString("previousVT")
            )

        if (!args.readOnly && args.gpsNeeded && selectedGps == null)
            initGps()
    }

    private fun setGetFieldsResult() {
        viewModel.fieldsResult.observe(this.viewLifecycleOwner, Observer {
            when (it) {
                is ApiResult.Success -> {
                    drawForm(
                        if (args.type == "UnSuccess")
                            prepareCommonIssueFields(it.response.data)
                        else
                            it.response.data,
                        args.readOnly
                    )
                }
                is ApiResult.Error -> viewDataBinding.root.snack(it.errorHandling)
            }
        })
    }

    private fun prepareCommonIssueFields(data: List<FormResponse.DataItem>?): List<FormResponse.DataItem>? {
        val result = mutableListOf<FormResponse.DataItem>()
        data?.forEachIndexed { index, item ->
            if (item.commonIssue != null) {
                // Add group title
                result.add(
                    FormResponse.DataItem(
                        dataType = "Text"
                    ).also {
                        it.localText = FormResponse.LocalText(
                            "${index.plus(1)}.Report Issue",
                            args.readOnly.not()
                        ) {
                            redirectForEdit(true, item)
                        }
                    }
                )
                result.add(
                    FormResponse.DataItem(
                        isMandatory = 0,
                        dataType = "Date",
                        label = "Date",
                        value = FormResponse.Value(reply = item.date)
                    )
                )
                result.add(
                    FormResponse.DataItem(
                        isMandatory = 0,
                        dataType = "String",
                        label = "Reason",
                        value = FormResponse.Value(
                            reply = """${item.commonIssue.commonIssue} 
                            |${item.commonIssue.commentValue?.let { ", $it" } ?: ""}""".trimMargin()
                        )
                    )
                )
            } else if (item.statusQuery != null) {
                // Add group title
                result.add(
                    FormResponse.DataItem(
                        dataType = "Text"
                    ).also {
                        it.localText = FormResponse.LocalText(
                            "${index.plus(1)}.Query Result",
                            args.readOnly.not()
                        ) {
                            redirectForEdit(false, item)
                        }
                    }
                )
                item.statusQuery.forEach {
                    result.add(it)
                }
            }
        }
        return result
    }

    private fun redirectForEdit(isReportIssue: Boolean, formResponse: FormResponse.DataItem) {
        if (isReportIssue) {
            findNavController().navigate(
                R.id.reportIssuePerDocFragment,
                bundleOf(
                    "FormResponse" to formResponse,
                    "DocumentStatusQueryId" to formResponse.documentStatusQueryId,
                    "VT" to formResponse.vT,
                    "lastUpdateTime" to formResponse.lastUpdateDateTime
                )
            )
        } else {
            findNavController().navigate(R.id.editDocumentFragment, Bundle().apply {
                putInt("DocumentStatusId", args.DocumentStatusId)
                putString("VT", formResponse.vT)
                putString("previousVT", args.VT)
                putParcelable("documentBaseInfo", args.documentBaseInfo as Parcelable)
                putBoolean("readOnly", args.readOnly)
                putBoolean("gpsNeeded", args.gpsNeeded)
                putString("type", "Query")
                putInt("DocumentStatusQueryId", formResponse.documentStatusQueryId ?: -1)
                putString("lastUpdateTime", formResponse.lastUpdateDateTime)
            })
        }
    }

    private fun drawForm(data: List<FormResponse.DataItem>?, readOnly: Boolean) {
        elementParser = ElementParser(
            this,
            data, viewDataBinding.layoutContainer, object :
                ElementsActivityRequestCallback {
                override fun requestPermission(permission: String) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        requestPermissions(arrayOf(permission), 123)
                    }
                }

                override fun onImageSelected(result: MutableLiveData<Intent>) {
                    imageResult = result
                }

                override fun courtListNeeded(courtList: MutableLiveData<List<CourtResponse.Court>>) {
                }

                override fun sheriffListNeeded(sheriffList: MutableLiveData<List<SheriffResponse.Sheriff>>) {
                }

                override fun onPreviewImageClicked(fileId: Int?, fileVT: String?, base64: String?) {
                    findNavController().tryNavigate(
                        EditDocumentFragmentDirections
                            .actionEditDocumentFragmentToImageViewerFragment(
                                GetFileRequest(
                                    args.documentBaseInfo.vTDocumentId,
                                    args.documentBaseInfo.documentId,
                                    fileVT,
                                    fileId,
                                    args.documentBaseInfo.vTServiceId,
                                    args.documentBaseInfo.serviceId
                                ), base64
                            )
                    )
                }
            }, readOnly || args.type == "UnSuccess"
        )
    }

    private fun setupSubmitEvent() {
        viewModel.submitEvent.observe(this.viewLifecycleOwner, EventObserver {
            if (args.gpsNeeded && selectedGps == null) {
                initGps()
                if (selectedGps == null) {
                    initGps()
                    txtFormError.text = getString(R.string.gps_not_available)
                    return@EventObserver
                } else {
                    txtFormError.text = ""
                }
            }
            if (elementParser.isItemsValid()) {
                MainScope().launch {
                    val formResult = FormResult.DocumentProgress().apply {
                        if (args.type == "Success") {
                            this.responseType = "Successful"
                        } else {
                            this.responseType = "Unsuccessful"
                        }

                        this.lastUpdateDateTime = arguments?.getString("lastUpdateTime")
                    }
                    val result = elementParser.getResult(
                        formResult = formResult,
                        documentStatusQueryId = if (args.type != "Success")
                            arguments?.getInt("DocumentStatusQueryId")
                        else
                            args.DocumentStatusId,
                        vt = args.VT
                    )

                    if (args.gpsNeeded)
                        formResult.gps = Gps(selectedGps?.first, selectedGps?.second)

                    viewModel.submitForm(result as FormResult.DocumentProgress)
                }
            }
        })
    }

    private fun setupSubmitFormEvent() {
        viewModel.submitFormEvent.observe(this.viewLifecycleOwner, EventObserver {
            (it as? ApiResult.Success)?.let { error ->
                btnSubmit.snack(error.response.errorHandling, onDismissAction = {
                    if (isAdded)
                        findNavController().popBackStack()
                })
            }
            (it as? ApiResult.Error)?.let { error -> btnSubmit.snack(error.errorHandling) }
        })
    }

    private fun initGps() {
        val awesomeConfiguration = LocationConfiguration.Builder()
            .keepTracking(false)
            .askForPermission(
                PermissionConfiguration.Builder()
                    .rationaleMessage("Please accept location permission.")
                    .requiredPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
                    .build()
            )
            .useGooglePlayServices(
                GooglePlayServicesConfiguration.Builder()
                    .fallbackToDefault(true)
                    .askForGooglePlayServices(false)
                    .askForSettingsApi(true)
                    .failOnSettingsApiSuspended(false)
                    .ignoreLastKnowLocation(false)
                    .build()
            )
            .useDefaultProviders(
                DefaultProviderConfiguration.Builder()
                    .build()
            )
            .build()
        LocationManager.Builder(requireContext().applicationContext)
            .fragment(this)
            .configuration(awesomeConfiguration)
            .notify(object : LocationListener {
                override fun onLocationChanged(location: Location?) {
                    if (location?.latitude != null)
                        selectedGps = Pair(location.latitude, location.longitude)
                }

                override fun onPermissionGranted(alreadyHadPermission: Boolean) {
                }

                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
                }

                override fun onProviderEnabled(provider: String?) {
                }

                override fun onProviderDisabled(provider: String?) {
                }

                override fun onProcessTypeChanged(processType: Int) {
                }

                override fun onLocationFailed(type: Int) {
                }
            })
            .build().get()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            imageResult.value = data
        }
    }
}