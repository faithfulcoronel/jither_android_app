package com.example.medialert_project.ui.main

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import com.example.medialert_project.R
import com.example.medialert_project.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private var appBarConfiguration: AppBarConfiguration? = null
    private var currentStartDestination: Int? = null

    private val sessionViewModel: SessionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        val navHost = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHost.navController

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                sessionViewModel.state.collectLatest { state ->
                    when (state) {
                        SessionState.Loading -> Unit
                        SessionState.Authenticated -> ensureGraph(startDestination = R.id.dashboardFragment)
                        SessionState.Unauthenticated -> ensureGraph(startDestination = R.id.loginFragment)
                    }
                }
            }
        }
    }

    private fun ensureGraph(startDestination: Int) {
        if (currentStartDestination == startDestination && navController.graph.startDestinationId == startDestination) {
            if (startDestination == R.id.dashboardFragment && navController.currentDestination?.id != R.id.dashboardFragment) {
                navController.popBackStack(R.id.loginFragment, true)
                navController.navigate(R.id.dashboardFragment)
            }
            return
        }
        val navGraph = navController.navInflater.inflate(R.navigation.nav_graph).apply {
            setStartDestination(startDestination)
        }
        navController.setGraph(navGraph, null)
        currentStartDestination = startDestination
        appBarConfiguration = AppBarConfiguration(setOf(R.id.dashboardFragment))
        appBarConfiguration?.let { setupActionBarWithNavController(navController, it) }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}
