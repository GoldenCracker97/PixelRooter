package com.pixelrooter.ui.root

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.pixelrooter.R
import com.pixelrooter.databinding.FragmentRootBinding
import kotlinx.coroutines.launch

class RootFragment : Fragment() {

    private var _binding: FragmentRootBinding? = null
    private val binding get() = _binding!!
    private val viewModel: RootViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRootBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnStartRoot.setOnClickListener {
            viewModel.startRooting()
        }

        binding.btnRetry.setOnClickListener {
            findNavController().navigateUp()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is RootUiState.Idle -> {
                            binding.btnStartRoot.isVisible = true
                            binding.progressBar.isVisible = false
                            binding.tvLog.isVisible = false
                            binding.errorCard.isVisible = false
                        }
                        is RootUiState.Running -> {
                            binding.btnStartRoot.isVisible = false
                            binding.progressBar.isVisible = true
                            binding.tvLog.isVisible = true
                            binding.errorCard.isVisible = false
                            binding.tvLog.text = state.log
                            binding.scrollView.post { binding.scrollView.fullScroll(View.FOCUS_DOWN) }
                        }
                        is RootUiState.Success -> {
                            findNavController().navigate(R.id.action_root_to_result)
                        }
                        is RootUiState.Error -> {
                            binding.btnStartRoot.isVisible = false
                            binding.progressBar.isVisible = false
                            binding.tvLog.isVisible = true
                            binding.tvLog.text = state.log
                            binding.errorCard.isVisible = true
                            binding.tvErrorReason.text = state.reason
                            binding.btnRetry.isVisible = true
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
