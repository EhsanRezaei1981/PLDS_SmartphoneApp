package rezaei.mohammad.plds.views.manageDoc.imageViewer

import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.fragment_image_viewer.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import rezaei.mohammad.plds.data.ApiResult
import rezaei.mohammad.plds.databinding.FragmentImageViewerBinding
import rezaei.mohammad.plds.util.snack

class ImageViewerFragment : DialogFragment() {

    private val viewModel: ImageViewerViewModel by viewModel()
    private lateinit var viewDataBinding: FragmentImageViewerBinding
    private val args: ImageViewerFragmentArgs by navArgs()

    override fun onStart() {
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        super.onStart()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewDataBinding = FragmentImageViewerBinding.inflate(inflater, container, false)
            .apply {
                viewmodel = viewModel
                this.lifecycleOwner = this@ImageViewerFragment.viewLifecycleOwner
            }
        return viewDataBinding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setImageLoaderHandler()
        if (args.base64 != null)
            loadImage(args.base64)
        else if (args.getFileRequest != null)
            viewModel.loadImage(args.getFileRequest!!)
    }

    private fun setImageLoaderHandler() {
        viewModel.loadImageResult.observe(this.viewLifecycleOwner, Observer {
            when (it) {
                is ApiResult.Success -> {
                    it.response.data?.base64?.let { imageBase64 ->
                        loadImage(imageBase64)
                    }
                }
                is ApiResult.Error -> {
                    parentFragment?.view?.snack(it.errorHandling)
                }
            }

        })
    }

    private fun loadImage(image: String?) {
        with(Base64.decode(image, Base64.DEFAULT)) {
            Glide.with(this@ImageViewerFragment)
                .load(this)
                .thumbnail(0.2f)
                .into(imageView)
        }
    }

}