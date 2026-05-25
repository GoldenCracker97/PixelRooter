package com.pixelrooter.ui.home

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
import com.google.android.material.color.MaterialColors
import com.pixelrooter.R
import com.pixelrooter.data.model.RootStatus
import com.pixelrooter.databinding.FragmentHomeBinding
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HomeViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnRoot.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_root)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.loadingGroup.isVisible = state.isLoading
                    binding.contentGroup.isVisible = !state.isLoading

                    state.deviceInfo?.let { device ->
                        binding.tvModel.text = "${device.model} (${device.codename})"
                        binding.tvAndroid.text = "Android ${device.androidVersion} · Build ${device.buildId}"
                        binding.tvKernel.text = "Kernel ${device.kernelVersion}"
                        binding.tvPatch.text = "Security patch: ${device.securityPatchLevel}"
                        binding.tvPixelStatus.text = if (device.isPixel) "Pixel device ✓" else "Non-Pixel device"
                    }

                    binding.tvRootStatus.text = when (state.rootStatus) {
                        RootStatus.NotRooted -> "Not rooted"
                        is RootStatus.RootedWithMagisk -> "Rooted (Magisk ${state.rootStatus.magiskVersion})"
                        RootStatus.RootedOther -> "Already rooted"
                        RootStatus.Unknown -> "Root status unknown"
                    }

                    if (state.hasCompatibleExploit) {
                        binding.tvExploitStatus.text = "Compatible exploit available"
                        binding.tvExploitStatus.setTextColor(
                            MaterialColors.getColor(binding.root, com.google.android.material.R.attr.colorPrimary)
                        )
                        binding.btnRoot.isEnabled = true
                    } else {
                        binding.tvExploitStatus.text = "No exploit available for this patch level"
                        binding.tvExploitStatus.setTextColor(
                            MaterialColors.getColor(binding.root, com.google.android.material.R.attr.colorError)
                        )
                        binding.btnRoot.isEnabled = false
                    }

                    if (state.rootStatus !is RootStatus.NotRooted && state.rootStatus !is RootStatus.Unknown) {
                        binding.tvExploitStatus.text = "Device is already rooted"
                        binding.btnRoot.isEnabled = false
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
