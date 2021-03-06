package rezaei.mohammad.plds.views.getDocReference

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.transition.TransitionManager
import kotlinx.android.synthetic.main.fragment_add_multi_doc.*
import kotlinx.android.synthetic.main.fragment_get_doc_reference.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import rezaei.mohammad.plds.R
import rezaei.mohammad.plds.data.ApiResult
import rezaei.mohammad.plds.data.model.local.DocumentType
import rezaei.mohammad.plds.data.model.request.GetDocumentsOnLocationRequest
import rezaei.mohammad.plds.data.model.response.DocumentStatusResponse
import rezaei.mohammad.plds.databinding.FragmentGetDocReferenceBinding
import rezaei.mohammad.plds.util.EventObserver
import rezaei.mohammad.plds.util.setActivityTitle
import rezaei.mohammad.plds.util.snack
import rezaei.mohammad.plds.util.tryNavigate
import rezaei.mohammad.plds.views.addMultiDoc.AddMultiDocFragment
import rezaei.mohammad.plds.views.main.MainActivity

class GetDocReferenceFragment : Fragment() {


    private val viewModel: GetDocReferenceViewModel by viewModel()
    private lateinit var viewDataBinding: FragmentGetDocReferenceBinding


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_get_doc_reference, container, false)
        viewDataBinding = FragmentGetDocReferenceBinding.bind(root).apply {
            viewModel = this@GetDocReferenceFragment.viewModel
        }
        viewDataBinding.lifecycleOwner = this.viewLifecycleOwner
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setActivityTitle(getString(R.string.check_document_status))
        addMoreDocFragment()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setupForDocumentStatusResponse()
    }

    private fun addMoreDocFragment() {
        if (childFragmentManager.findFragmentById(R.id.multiAddDoc) == null)
            childFragmentManager.beginTransaction()
                .replace(
                    multiAddDoc.id,
                    AddMultiDocFragment.newInstance(DocumentType.CheckProgress)
                )
                .runOnCommit { setupMultiDocFragmentInteractor() }
                .commit()
        else
            childFragmentManager.registerFragmentLifecycleCallbacks(object :
                FragmentManager.FragmentLifecycleCallbacks() {
                override fun onFragmentActivityCreated(
                    fm: FragmentManager,
                    f: Fragment,
                    savedInstanceState: Bundle?
                ) {
                    super.onFragmentActivityCreated(fm, f, savedInstanceState)
                    if (f is AddMultiDocFragment)
                        setupMultiDocFragmentInteractor()
                }
            }, false)
    }

    private fun setupForDocumentStatusResponse() {
        viewModel.documentStatusEvent.observe(this.viewLifecycleOwner, EventObserver {
            (it as? ApiResult.Success)?.let {
                navigateToDocProgress(it.response.data!!)
            }
            (it as? ApiResult.Error)?.let { error -> btnCheckProgress.snack(error.errorHandling) }
        })
    }

    private fun navigateToDocProgress(documentStatusResponse: DocumentStatusResponse.Data) {
        val action =
            GetDocReferenceFragmentDirections.actionGetDocReferenceFragmentToDocProgressFragment(
                documentStatusResponse
            )
        findNavController().tryNavigate(action)
    }

    private fun setupMultiDocFragmentInteractor() {
        (childFragmentManager.findFragmentById(R.id.multiAddDoc) as? AddMultiDocFragment)?.let { fragment ->
            //setDocumentListObserver
            fragment.documentList.observe(this.viewLifecycleOwner, Observer {
                TransitionManager.beginDelayedTransition(viewDataBinding.root as ViewGroup)
                if (it.isNotEmpty())
                    layCheckProgress.visibility = View.VISIBLE
                else
                    layCheckProgress.visibility = View.GONE
            })
            //setOnDocumentListButtonClickListener
            fragment.btnDocumentList.setOnClickListener {
                val location = (requireActivity() as MainActivity).checkInService?.checkedInLocation
                findNavController().navigate(
                    GetDocReferenceFragmentDirections
                        .actionGetDocReferenceFragmentToDocListByLocationFragment(
                            GetDocumentsOnLocationRequest(
                                location?.locationId,
                                location?.vTLocationId,
                                location?.vTLocation,
                                location?.uTPId,
                                location?.vTUTPId,
                                location?.locationType
                            )
                        )
                )
            }
        }
    }

}
