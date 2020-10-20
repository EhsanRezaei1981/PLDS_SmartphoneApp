package rezaei.mohammad.plds.data.model.request

import com.google.gson.annotations.SerializedName

sealed class FormResult {
    @field:SerializedName("GPS")
    var gps: Gps? = null

    data class DocumentProgress(
        @field:SerializedName("DocumentsInfo")
        var documentsInfo: List<DocumentsInfoItem?>? = null,
        @field:SerializedName("Unsuccessful")
        var unsuccessful: Result? = null,
        @field:SerializedName("Successful")
        var successful: Result? = null,
        @field:SerializedName("ReportIssue")
        var reportIssue: ElementResult? = null,
        @field:SerializedName("ResponseType")
        var responseType: String? = null
    ) : FormResult()

    data class RespondedFields(
        @field:SerializedName("DocumentStatusId")
        var documentStatusId: Int? = null,
        @field:SerializedName("Elements")
        var elements: MutableList<ElementResult?>? = null,
        @field:SerializedName("VT")
        var vT: String? = null
    ) : FormResult()

    data class CommonAction(
        @field:SerializedName("LocationType")
        var locationType: String? = null,
        @field:SerializedName("LocationId")
        var locationId: Int? = null,
        @field:SerializedName("Date")
        var date: String? = null,
        @field:SerializedName("CommonActionId")
        var commonActionId: Int? = null,
        @field:SerializedName("VTCommonActionId")
        var vTCommonActionId: String? = null,
        @field:SerializedName("Comment")
        var comment: String? = null,
        @field:SerializedName("File")
        var chosenFile: ChosenFile? = null
    ) : FormResult()
}

data class Result(

    @field:SerializedName("Elements")
    val elements: MutableList<ElementResult?>? = null
)

data class Gps(

    @field:SerializedName("Y")
    var Y: Double? = null,

    @field:SerializedName("X")
    var X: Double? = null

/*    @field:SerializedName("RadiusInMetters")
    var radius: Int? = null*/
)

data class DocumentsInfoItem(

    @field:SerializedName("DocumentReferenceNo")
    val documentReferenceNo: String? = null
)