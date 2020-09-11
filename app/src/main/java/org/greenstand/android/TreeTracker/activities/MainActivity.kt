package org.greenstand.android.TreeTracker.activities

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.findNavController
import kotlinx.android.synthetic.main.activity_main.toolbar
import kotlinx.android.synthetic.main.activity_main.toolbarTitle
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.analytics.Analytics
import org.greenstand.android.TreeTracker.application.Permissions
import org.greenstand.android.TreeTracker.fragments.DataFragment
import org.greenstand.android.TreeTracker.fragments.MapsFragmentDirections
import org.greenstand.android.TreeTracker.models.FeatureFlags
import org.greenstand.android.TreeTracker.models.LanguageSwitcher
import org.greenstand.android.TreeTracker.models.LocationDataCapturer
import org.greenstand.android.TreeTracker.models.LocationUpdateManager
import org.greenstand.android.TreeTracker.models.User
import org.koin.android.ext.android.inject
import timber.log.Timber

class MainActivity : AppCompatActivity(), ActivityCompat.OnRequestPermissionsResultCallback,
    SensorEventListener {

    private val languageSwitcher: LanguageSwitcher by inject()
    private val user: User by inject()
    private val analytics: Analytics by inject()
    private val locationUpdateManager: LocationUpdateManager by inject()
    private val locationDataCapturer: LocationDataCapturer by inject()
    private val sharedPreferences: SharedPreferences by inject()
    private var fragment: Fragment? = null
    private lateinit var sensorManager: SensorManager
    private lateinit var stepCounter: Sensor
    /**
     * Called when the activity is first created.
     * @param savedInstanceState If the activity is being re-initialized after
     * previously being shut down then this Bundle contains the data it most
     * recently supplied in onSaveInstanceState(Bundle). **Note: Otherwise it is null.**
     */
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepCounter = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        languageSwitcher.applyCurrentLanguage(this)

        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        findViewById<View>(R.id.appbar_layout).visibility = View.GONE

        val navController = findNavController(R.id.nav_host_fragment)

        val listener = NavController
            .OnDestinationChangedListener { controller, destination, arguments ->
                if (destination.id != R.id.splashFragment2 &&
                    destination.id != R.id.orgWallFragment) {
                    findViewById<View>(R.id.appbar_layout).visibility = View.VISIBLE
                }

            invalidateOptionsMenu()

            analytics.tagScreen(this, controller.currentDestination?.label.toString())
        }

        navController.addOnDestinationChangedListener(listener)

        toolbar.setNavigationOnClickListener {
            navController.popBackStack()
        }

        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.title = ""

        if (!user.isLoggedIn) {
            user.expireCheckInStatus()
            toolbarTitle.text = resources.getString(R.string.user_not_identified)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        return if (
            findNavController(R.id.nav_host_fragment).currentDestination?.id == R.id.mapsFragment) {
            menuInflater.inflate(R.menu.menu_main, menu)
            menu.findItem(R.id.action_change_language).isVisible = FeatureFlags.DEBUG_ENABLED
            true
        } else {
            false
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val bundle: Bundle?
        when (item.itemId) {
            R.id.action_data -> {
                fragment = DataFragment()
                bundle = intent.extras
                fragment?.arguments = bundle

                findNavController(R.id.nav_host_fragment)
                    .navigate(MapsFragmentDirections.actionMapsFragmentToDataFragment())
                return true
            }
            R.id.action_about -> {
                findNavController(R.id.nav_host_fragment)
                    .navigate(MapsFragmentDirections.actionMapsFragmentToAboutFragment())
                return true
            }

            R.id.action_change_user -> {
                user.expireCheckInStatus()

                toolbarTitle.text = resources.getString(R.string.user_not_identified)
                findNavController(R.id.nav_host_fragment)
                    .navigate(R.id.action_global_login_flow_graph)
            }
            R.id.action_change_language -> {
                languageSwitcher.switch(this)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    public override fun onPause() {
        super.onPause()
        locationUpdateManager.stopLocationUpdates()
        sensorManager.unregisterListener(this)
    }

    public override fun onResume() {
        super.onResume()

        if (areNecessaryPermissionsNotGranted()) {
            requestNecessaryPermissions()
        } else {
            startPeriodicUpdates()
        }
        sensorManager.registerListener(this, stepCounter, SensorManager.SENSOR_DELAY_NORMAL)
    }

    private fun areNecessaryPermissionsNotGranted(): Boolean {
        return (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED
                ) ||
                (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED)
    }

    private fun requestNecessaryPermissions() {
        ActivityCompat.requestPermissions(this, arrayOf(
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.ACCESS_FINE_LOCATION),
                Permissions.NECESSARY_PERMISSIONS)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (grantResults.isNotEmpty()) {
            if (requestCode == Permissions.NECESSARY_PERMISSIONS &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startPeriodicUpdates()
            }
        }
    }

    /**
     * In response to a request to start updates, send a request
     * to Location Services
     */
    @SuppressLint("MissingPermission")
    private fun startPeriodicUpdates() {

        if (areNecessaryPermissionsNotGranted()) {
            Toast.makeText(
                this,
                "GPS Permissions Not Enabled",
                Toast.LENGTH_LONG
            ).show()
            requestNecessaryPermissions()
            return
        }

        // TODO this check may not longer be necessary
        if (!locationUpdateManager.isLocationEnabled()) {
            val builder = AlertDialog.Builder(this@MainActivity)

            builder.setTitle(R.string.enable_location_access)
            builder.setMessage(
                R.string.you_must_enable_location_access_in_your_settings_in_order_to_continue)

            builder.setPositiveButton(R.string.ok) { dialog, which ->
                if (Build.VERSION.SDK_INT >= 19) {
                    // LOCATION_MODE
                    // Sollution for problem 25 added the ability to pop up location start activity
                    startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                } else {
                    // LOCATION_PROVIDERS_ALLOWED

                    val locationProviders = Settings.Secure.getString(contentResolver,
                        Settings.Secure.LOCATION_PROVIDERS_ALLOWED)
                    if (locationProviders == null || locationProviders == "") {
                        startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                    }
                }

                dialog.dismiss()
            }

            builder.setNegativeButton(R.string.cancel) { dialog, which ->
                finish()
                dialog.dismiss()
            }

            val alert = builder.create()
            alert.setCancelable(false)
            alert.setCanceledOnTouchOutside(false)
            alert.show()

            return
        }

        locationUpdateManager.startLocationUpdates()
        locationDataCapturer.start()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Ignore
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            user.absoluteStepCount = it.values[0].toInt()
            Timber.d("Step count so far ${it.values[0].toInt()}")
        }
    }
}
