package rezaei.mohammad.plds.views.reportIssue

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import rezaei.mohammad.plds.data.Result
import rezaei.mohammad.plds.data.local.LocalRepository
import rezaei.mohammad.plds.data.model.local.DocumentType
import rezaei.mohammad.plds.data.model.request.DocumentsInfoItem
import rezaei.mohammad.plds.data.model.request.FormResult
import rezaei.mohammad.plds.data.model.response.BaseResponse
import rezaei.mohammad.plds.data.model.response.CommonIssuesResponse
import rezaei.mohammad.plds.data.remote.RemoteRepository
import rezaei.mohammad.plds.util.Event

class ReportIssueViewModel(
    private val localRepository: LocalRepository,
    private val remoteRepository: RemoteRepository
) : ViewModel() {

    private val _dataLoading = MutableLiveData<Boolean>()
    val dataLoading: LiveData<Boolean> = _dataLoading


    val dataExist = MutableLiveData<Boolean>()

    private val _submitEvent = MutableLiveData<Event<Unit>>()
    val submitEvent: MutableLiveData<Event<Unit>> = _submitEvent

    private val _submitFormEvent = MutableLiveData<Event<Result<BaseResponse<Unit>>>>()
    val submitFormEvent: LiveData<Event<Result<BaseResponse<Unit>>>> = _submitFormEvent

    private val _commonIssues = MutableLiveData<Result<CommonIssuesResponse>>()
    val commonIssues: LiveData<Result<CommonIssuesResponse>> = _commonIssues

    init {
        setupDataExist()
    }

    fun getCommonIssues() {
        viewModelScope.launch {
            if (localRepository.getAllDocument(DocumentType.ReportIssue).isNotEmpty()) {
                _dataLoading.value = true
                val result = remoteRepository.getCommonIssues(
                    DocumentsInfoItem(getDocumentList().first().docRefNo)
                )
                _commonIssues.value = result
                _dataLoading.value = false
            }

        }
    }

    suspend fun getDocumentList() =
        localRepository.getAllDocument(DocumentType.ReportIssue)

    fun removeAllDocuments() {
        viewModelScope.launch {
            localRepository.deleteAllDocs(getDocumentList())
        }
    }

    fun submitForm() {
        _submitEvent.value = Event(Unit)
    }

    fun submitForm(formResult: FormResult) {
        viewModelScope.launch {
            _dataLoading.value = true
            val result = remoteRepository.sendDynamicFieldResponse(formResult)
            _submitFormEvent.value = Event(result)
            _dataLoading.value = false
        }
    }

    private fun setupDataExist() {
        _commonIssues.observeForever {
            dataExist.value = ((it as? Result.Success)?.response?.data?.isNotEmpty() == true)
        }
    }
}
