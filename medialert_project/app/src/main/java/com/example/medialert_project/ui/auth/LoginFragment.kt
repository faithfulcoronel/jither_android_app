package com.example.medialert_project.ui.auth

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
import com.example.medialert_project.R
import com.example.medialert_project.databinding.FragmentLoginBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.authButton.setOnClickListener {
            viewModel.authenticate(
                email = binding.emailEditText.text?.toString().orEmpty(),
                password = binding.passwordEditText.text?.toString().orEmpty()
            )
        }
        binding.toggleModeText.setOnClickListener { viewModel.toggleMode() }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { state ->
                    renderState(state)
                }
            }
        }
    }

    private fun renderState(state: AuthUiState) {
        binding.loadingIndicator.isVisible = state.isLoading
        binding.authButton.isEnabled = !state.isLoading
        binding.emailInputLayout.isEnabled = !state.isLoading
        binding.passwordInputLayout.isEnabled = !state.isLoading

        binding.errorText.isVisible = state.errorMessage != null
        binding.errorText.text = state.errorMessage

        binding.infoText.isVisible = state.infoMessage != null
        binding.infoText.text = state.infoMessage

        binding.authButton.text =
            if (state.mode == AuthMode.SIGN_IN) getString(R.string.action_sign_in)
            else getString(R.string.action_sign_up)

        binding.toggleModeText.text =
            if (state.mode == AuthMode.SIGN_IN)
                getString(R.string.auth_toggle_to_sign_up)
            else getString(R.string.auth_toggle_to_sign_in)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
