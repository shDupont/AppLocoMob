package br.com.fiap.applocomob

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.widget.addTextChangedListener
import com.android.volley.RequestQueue
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import org.json.JSONObject
import java.io.IOException
import androidx.fragment.app.FragmentActivity

class MapsActivity : FragmentActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var currentLocation: Location
    private lateinit var placesClient: PlacesClient
    private lateinit var currentLocationInput: AutoCompleteTextView
    private lateinit var destinationInput: AutoCompleteTextView
    private lateinit var requestQueue: RequestQueue

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        // Initialize the SDK
        Places.initialize(applicationContext, getMapsApiKey())
        placesClient = Places.createClient(this)
        requestQueue = Volley.newRequestQueue(this)

        currentLocationInput = findViewById(R.id.inptCurrentLocation)
        destinationInput = findViewById(R.id.inptDestinyLocation)
        val routeButton = findViewById<Button>(R.id.routeButton)

        // Set up the autocomplete adapter
        val autocompleteAdapter = ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line)
        currentLocationInput.setAdapter(autocompleteAdapter)
        destinationInput.setAdapter(autocompleteAdapter)

        // Set up the text changed listener
        currentLocationInput.addTextChangedListener { text ->
            val request = FindAutocompletePredictionsRequest.builder()
                .setQuery(text.toString())
                .build()

            placesClient.findAutocompletePredictions(request).addOnSuccessListener { response ->
                autocompleteAdapter.clear()
                for (prediction in response.autocompletePredictions) {
                    autocompleteAdapter.add(prediction.getFullText(null).toString())
                }
            }
        }

        destinationInput.addTextChangedListener { text ->
            val request = FindAutocompletePredictionsRequest.builder()
                .setQuery(text.toString())
                .build()

            placesClient.findAutocompletePredictions(request).addOnSuccessListener { response ->
                autocompleteAdapter.clear()
                for (prediction in response.autocompletePredictions) {
                    autocompleteAdapter.add(prediction.getFullText(null).toString())
                }
            }
        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        routeButton.setOnClickListener {
            val origin = if (currentLocationInput.text.toString().isEmpty()) {
                LatLng(currentLocation.latitude, currentLocation.longitude)
            } else {
                getLocationFromAddress(currentLocationInput.text.toString())
            }
            val destination = getLocationFromAddress(destinationInput.text.toString())

            if (origin != null && destination != null) {
                val url = getDirectionsUrl(origin, destination)

                Log.d("MapsActivity", "Directions URL: $url")

                val directionsRequest = StringRequest(url, { response ->
                    Log.d("MapsActivity", "Directions response: $response")

                    val (route, duration) = parseDirectionsResult(response)
                    if (route.isNotEmpty()) {
                        drawPolyline(route)

                        val formattedDuration = if (duration.contains("h")) {
                            val parts = duration.split(" ")
                            val hours = parts.getOrNull(0)?.replace("h", "") ?: "0"
                            val minutes = parts.getOrNull(2)?.replace("mins", "") ?: "0"
                            "${hours}h ${minutes}mins"
                        } else {
                            val minutes = duration.replace("mins", "")
                            "${minutes}mins"
                        }

                        // Set the duration in the 'txtTime' EditText
                        findViewById<TextView>(R.id.txtTime).setText(formattedDuration)
                        findViewById<TextView>(R.id.txtDestiny).setText("Destino: ${destinationInput.text.toString()}")
                    } else {
                        // Show an error message if the route is empty
                        Toast.makeText(this, "No route found between the two locations.", Toast.LENGTH_LONG).show()
                    }
                }, { error ->
                    error.printStackTrace()
                    Log.e("MapsActivity", "Failed to get route", error)

                    // Show an error message if the request fails
                    Toast.makeText(this, "Failed to get route: ${error.message}", Toast.LENGTH_LONG).show()
                })

                requestQueue.add(directionsRequest)
            }
        }
    }

    private fun getLocationFromAddress(strAddress: String): LatLng? {
        val geocoder = Geocoder(this)
        val address: List<Address>?
        var p1: LatLng? = null

        try {
            // May throw an IOException
            address = geocoder.getFromLocationName(strAddress, 5)
            if (address == null) {
                return null
            }

            val location = address[0]
            p1 = LatLng(location.latitude, location.longitude)

        } catch (ex: IOException) {
            ex.printStackTrace()
        }

        return p1
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.isZoomControlsEnabled = true
        mMap.uiSettings.isZoomGesturesEnabled = true
        mMap.uiSettings.isScrollGesturesEnabled = true

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                location?.let {
                    currentLocation = it
                    val currentLatLng = LatLng(it.latitude, it.longitude)
                    mMap.addMarker(MarkerOptions().position(currentLatLng).title("Current Location"))
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(currentLatLng))

                    // Set the current location in the 'inptCurrentLocation' EditText
                    currentLocationInput.setText("${it.latitude}, ${it.longitude}")
                }
            }
    }

    private fun getMapsApiKey(): String {
        try {
            val applicationInfo = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
            val bundle = applicationInfo.metaData
            return bundle.getString("com.google.android.geo.API_KEY") ?: ""
        } catch (e: PackageManager.NameNotFoundException) {
            return ""
        }
    }

    private fun drawPolyline(route: List<LatLng>) {
        if (route.isNotEmpty()) {
            val polylineOptions = PolylineOptions().apply {
                width(10f)
                color(Color.BLUE)
                addAll(route)
            }
            mMap.addPolyline(polylineOptions)
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(route[0], 10f))
        }
    }

    private fun getDirectionsUrl(origin: LatLng, dest: LatLng): String {
        // Origin of route
        val str_origin = "origin=" + origin.latitude + "," + origin.longitude

        // Destination of route
        val str_dest = "destination=" + dest.latitude + "," + dest.longitude

        // Sensor enabled
        val sensor = "sensor=false"

        // Building the parameters to the web service
        val parameters = "$str_origin&$str_dest&$sensor"

        // Output format
        val output = "json"

        // Building the url to the web service
        return "https://maps.googleapis.com/maps/api/directions/$output?$parameters&key=${getMapsApiKey()}"
    }

    private fun parseDirectionsResult(result: String): Pair<List<LatLng>, String> {
        val jsonObject = JSONObject(result)
        val routes = jsonObject.getJSONArray("routes")
        val polylineList = mutableListOf<LatLng>()
        var duration = ""

        if (routes.length() > 0) {
            val legs = routes.getJSONObject(0).getJSONArray("legs")

            if (legs.length() > 0) {
                val steps = legs.getJSONObject(0).getJSONArray("steps")

                for (i in 0 until steps.length()) {
                    val polyline = steps.getJSONObject(i).getJSONObject("polyline").getString("points")
                    polylineList.addAll(decodePolyline(polyline))
                }

                // Get the duration from the first leg
                duration = legs.getJSONObject(0).getJSONObject("duration").getString("text")
            }
        }

        return Pair(polylineList, duration)
    }

    private fun decodePolyline(encoded: String): List<LatLng> {
        val poly = mutableListOf<LatLng>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0

        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].toInt() - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat

            shift = 0
            result = 0
            do {
                b = encoded[index++].toInt() - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng

            val latLng = LatLng(lat.toDouble() / 1E5, lng.toDouble() / 1E5)
            poly.add(latLng)
        }

        return poly
    }
}