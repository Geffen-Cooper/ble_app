package com.lorenzofelletti.simpleblescanner

import android.Manifest
import android.graphics.Color
import android.bluetooth.BluetoothManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lorenzofelletti.permissions.PermissionManager
import com.lorenzofelletti.permissions.dispatcher.dsl.*
import com.lorenzofelletti.simpleblescanner.BuildConfig.DEBUG
import com.lorenzofelletti.simpleblescanner.blescanner.BleScanManager
import com.lorenzofelletti.simpleblescanner.blescanner.adapter.BleDeviceAdapter
import com.lorenzofelletti.simpleblescanner.blescanner.model.BleDevice
import com.lorenzofelletti.simpleblescanner.blescanner.model.BleScanCallback
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter

import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.Viewport
import com.jjoe64.graphview.helper.StaticLabelsFormatter
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries

class MainActivity : AppCompatActivity() {
    private lateinit var btnStartScan: Button
    private lateinit var etFilename: EditText

    private lateinit var permissionManager: PermissionManager

    private lateinit var btManager: BluetoothManager
    private lateinit var bleScanManager: BleScanManager

    private lateinit var foundDevices: MutableList<BleDevice>
    private lateinit var foundDevices2: MutableList<BleDevice>
    private lateinit var foundDevices3: MutableList<BleDevice>

    private lateinit var csvFile: File
    private var startTime: Long = 0 // Declare startTime as a class variable

    private lateinit var series: LineGraphSeries<DataPoint>
    private lateinit var graph: GraphView
    private lateinit var viewport: Viewport
    private lateinit var series1: LineGraphSeries<DataPoint>
    private lateinit var series2: LineGraphSeries<DataPoint>
    private lateinit var series3: LineGraphSeries<DataPoint>
    private var lastXValue = 0.0


    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        graph = findViewById(R.id.graph)

        series1 = LineGraphSeries()
        series2 = LineGraphSeries()
        series3 = LineGraphSeries()

        series1.color = Color.BLUE
        series2.color = Color.RED
        series3.color = Color.GREEN

        graph.addSeries(series1)
        graph.addSeries(series2)
        graph.addSeries(series3)

        graph.viewport.isXAxisBoundsManual = true
        graph.viewport.isYAxisBoundsManual = true
        graph.viewport.setMinX(0.0)
        graph.viewport.setMaxX(10.0)
        graph.viewport.setMinY(-20.0)
        graph.viewport.setMaxY(20.0)

        graph.viewport.isScrollable = true
        graph.viewport.isScalable = true

        permissionManager = PermissionManager(this)
        permissionManager buildRequestResultsDispatcher {
            withRequestCode(BLE_PERMISSION_REQUEST_CODE) {
                checkPermissions(blePermissions)
                showRationaleDialog(getString(R.string.ble_permission_rationale))
                doOnGranted { startLogging() }
                doOnDenied {
                    Toast.makeText(
                        this@MainActivity,
                        getString(R.string.ble_permissions_denied_message),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        // RecyclerView handling
        val rvFoundDevices = findViewById<View>(R.id.rv_found_devices) as RecyclerView
        foundDevices = BleDevice.createBleDevicesList()
        val adapter = BleDeviceAdapter(foundDevices)
        rvFoundDevices.adapter = adapter
        rvFoundDevices.layoutManager = LinearLayoutManager(this)

        val rvFoundDevices2 = findViewById<View>(R.id.rv_found_devices2) as RecyclerView
        foundDevices2 = BleDevice.createBleDevicesList()
        val adapter2 = BleDeviceAdapter(foundDevices2)
        rvFoundDevices2.adapter = adapter2
        rvFoundDevices2.layoutManager = LinearLayoutManager(this)

        val rvFoundDevices3 = findViewById<View>(R.id.rv_found_devices3) as RecyclerView
        foundDevices3 = BleDevice.createBleDevicesList()
        val adapter3 = BleDeviceAdapter(foundDevices3)
        rvFoundDevices3.adapter = adapter3
        rvFoundDevices3.layoutManager = LinearLayoutManager(this)

        etFilename = findViewById((R.id.filenameEditText))


        // BleManager creation
        btManager = getSystemService(BluetoothManager::class.java)
        bleScanManager = BleScanManager(btManager, 50000, scanCallback = BleScanCallback({
            val name = it?.device?.address

            if (name.isNullOrBlank()) return@BleScanCallback

            if (name.contains("E8:1B:8A") or name.contains("E5:97:8A") or name.contains("ED:4D:65") or name.contains("73:6C:64:30:30:31")) {
                val device = BleDevice(name)
//                if (!foundDevices.contains(device)) {
                    if (DEBUG) {
                        Log.d(
                            BleScanCallback::class.java.simpleName,
                            "${this.javaClass.enclosingMethod?.name} - Found device: $name"
                        )

                        val scanRecordBytes = it.scanRecord?.bytes
                        if (scanRecordBytes != null) {
                            for (i in 0..7){
                                // data starts at i=4th byte, and the pattern is header-X-Y-Z
                                // [len, md, md, md, header,x,y,z,header,x,y,z,...]
                                val x = scanRecordBytes[i*3+4] // x byte
                                val y = scanRecordBytes[i*3+4+1] // y byte
                                val z = scanRecordBytes[i*3+4+2] // z byte


                                val formattedBytes = scanRecordBytes.joinToString(", ") { byte ->
                                    String.format("%02X", byte.toInt() and 0xFF)
                                }
                                Log.d(
                                    BleScanCallback::class.java.simpleName,
                                    "Scan Record Bytes: $formattedBytes"
                                )
                                Log.d("MyApp", "X: $x, Y: $y, Z: $z")
                                val x_conv = String.format("%.4f", x*(1.0/64)*9.8).toDouble()
                                val y_conv = String.format("%.4f", y*(1.0/64)*9.8).toDouble()
                                val z_conv = String.format("%.4f", z*(1.0/64)*9.8).toDouble()
                                val device = BleDevice("X: $x_conv, Y: $y_conv, Z: $z_conv")
                                if(name.contains("E8:1B:8A"))
                                {
                                    foundDevices.add(device)
                                    adapter.notifyItemInserted(foundDevices.size - 1)
                                    writeDataToCsv(1,Triple(x_conv,y_conv,z_conv))
                                }
                                else if(name.contains("E5:97:8A"))
                                {
                                    foundDevices2.add(device)
                                    adapter2.notifyItemInserted(foundDevices2.size - 1)
                                    writeDataToCsv(2,Triple(x_conv,y_conv,z_conv))
                                }
                                else if(name.contains("ED:4D:65"))
                                {
                                    foundDevices3.add(device)
                                    adapter3.notifyItemInserted(foundDevices3.size - 1)
                                    writeDataToCsv(3,Triple(x_conv,y_conv,z_conv))
                                }
                                else if(name.contains("73:6C:64:30:30:31"))
                                {
//                                    foundDevices3.add(device)
//                                    adapter3.notifyItemInserted(foundDevices3.size - 1)
//                                    writeDataToCsv(3,Triple(x_conv,y_conv,z_conv))
                                    val currentTime = System.currentTimeMillis()
                                    val timestamp = currentTime - startTime
                                    updateGraph(timestamp.toDouble()/1000+i/12.5,Triple(x_conv,y_conv,z_conv))
                                }

                            }
                            if(name.contains("E8:1B:8A"))
                            {
                                val device = BleDevice("===================")
                                foundDevices.add(device)
                                adapter.notifyItemInserted(foundDevices.size - 1)
                            }
                            else if(name.contains("E5:97:8A"))
                            {
                                val device = BleDevice("===================")
                                foundDevices2.add(device)
                                adapter2.notifyItemInserted(foundDevices2.size - 1)
                            }
                            else if(name.contains("ED:4D:65"))
                            {
                                val device = BleDevice("===================")
                                foundDevices3.add(device)
                                adapter3.notifyItemInserted(foundDevices3.size - 1)
                            }
//                            else if(name.contains("73:6C:64:30:30:31"))
//                            {
//                                val device = BleDevice("===================")
//                                foundDevices3.add(device)
//                                adapter3.notifyItemInserted(foundDevices3.size - 1)
//                            }
                            Log.d("MyApp", "++++++++++++++++++++++++++")



                        }

                    }
//                    foundDevices.add(device)
//                    adapter.notifyItemInserted(foundDevices.size - 1)
//                }
            }
        }))

        // Adding the actions the manager must do before and after scanning
        bleScanManager.beforeScanActions.add { btnStartScan.isEnabled = false }
        bleScanManager.beforeScanActions.add {
            foundDevices.size.let {
                foundDevices.clear()
                adapter.notifyItemRangeRemoved(0, it)
            }
        }
        bleScanManager.afterScanActions.add { btnStartScan.isEnabled = true }



        // Adding the onclick listener to the start scan button
        btnStartScan = findViewById(R.id.btn_start_scan)
        btnStartScan.setOnClickListener {
            if (DEBUG) Log.i(TAG, "${it.javaClass.simpleName}:${it.id} - onClick event")

            // Checks if the required permissions are granted and starts the scan if so, otherwise it requests them
            permissionManager checkRequestAndDispatch BLE_PERMISSION_REQUEST_CODE

            val et_file = etFilename.text
            Toast.makeText(this, et_file, Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateGraph(time_stamp: Double, xyz_data: Triple<Double,Double,Double>) {
        // Generate or retrieve new data points
        val (x,y,z) = xyz_data

        series1.appendData(DataPoint(time_stamp, x), true, 100)
        series2.appendData(DataPoint(time_stamp, y), true, 100)
        series3.appendData(DataPoint(time_stamp, z), true, 100)

    }

    private fun startLogging() {
        val filename = etFilename.text.toString()
        if (filename.isBlank()) {
            Toast.makeText(this, "Please enter a filename", Toast.LENGTH_LONG).show()
            return
        }

        // Initialize CSV file with the user-provided name and current timestamp
        csvFile = File(getExternalFilesDir(null), "${filename}.csv")
        if (!csvFile.exists()) {
            csvFile.createNewFile()
        }

        // Set startTime once when logging starts
        startTime = System.currentTimeMillis()

        bleScanManager.scanBleDevices()
    }

    private fun writeDataToCsv(deviceNumber: Int, xyz_data: Triple<Double,Double,Double>) {
        try {
            val fos = FileOutputStream(csvFile, true)
            val writer = OutputStreamWriter(fos)
            val currentTime = System.currentTimeMillis()
            val timestamp = currentTime - startTime

            val (x,y,z) = xyz_data

            if(deviceNumber == 1)
            {
                writer.append("$timestamp, $x, $y, $z, 0,0,0, 0,0,0\n")
            }
            else if(deviceNumber == 2)
            {
                writer.append("$timestamp, 0,0,0, $x, $y, $z, 0,0,0\n")
            }
            else if(deviceNumber == 3)
            {
                writer.append("$timestamp, 0,0,0, 0,0,0, $x, $y, $z\n")
            }

            writer.close()
            fos.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Function that checks whether the permission was granted or not
     */
    @RequiresApi(Build.VERSION_CODES.S)
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionManager.dispatchOnRequestPermissionsResult(requestCode, grantResults)
    }

    companion object {
        private val TAG = MainActivity::class.java.simpleName

        private const val BLE_PERMISSION_REQUEST_CODE = 1
        @RequiresApi(Build.VERSION_CODES.S)
        private val blePermissions = arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BLUETOOTH_ADMIN,
        )
    }
}