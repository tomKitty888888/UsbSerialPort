package com.sdk.usb.service

import android.app.*
import android.content.Intent
import android.os.IBinder
import android.hardware.usb.UsbDeviceConnection

import android.hardware.usb.UsbManager

import android.hardware.usb.UsbDevice

import android.content.BroadcastReceiver

import android.content.IntentFilter

import android.content.Context
import android.os.Binder
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialProber
import java.io.IOException
import java.lang.Exception
import java.util.concurrent.Executors
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.util.SerialInputOutputManager
import com.sdk.usb.R
import org.example.adc.service.impl.ADCService55Impl


class UsbService :Service(){

    companion object{
        const val action = "android.intent.action.UsbService"
    }

    var manager: UsbManager? = null
    private var serialPort: UsbSerialPort? = null
    var driver: UsbSerialDriver? = null
    var ACTION_DEVICE_PERMISSION = "ACTION_DEVICE_PERMISSION"
    val CHANNEL_ID_STRING = "1"
    val SERVICE_ID = 146


    override fun onBind(p0: Intent?): IBinder? {
        return MsgBinder()
    }

    inner class MsgBinder : Binder() {
        /**
         * 获取当前Service的实例
         * @return
         */
        fun getService():UsbService{
            return this@UsbService
        }
    }

    fun sendData(byteArray: ByteArray){
        try {
            serialPort?.write(byteArray,0)
        }catch (e:Exception){
            e.printStackTrace()
        }
    }

    fun setCallBack(callBack : CallBack){
        this.callBack = callBack
    }
    private var callBack : CallBack? = null
    interface CallBack{
        /**
         * data 串口接收数据
         */
         fun onNewData(data: ByteArray)

        /**
         * 串口异常
         */
        fun onRunError(e: Exception)
    }


    override fun onCreate() {
        super.onCreate()

        try {
            val pm = getSystemService(POWER_SERVICE) as PowerManager
            val wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, UsbService::class.java.simpleName)
            wakeLock.acquire()
            onStartForeground()

        }catch (e:Exception){
            e.printStackTrace()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        onStartForeground()
        initSerial()
        return super.onStartCommand(intent, flags, startId)
    }


    /**
     * 初始化usb串口
     * @throws IOException
     */
    @Throws(IOException::class)
    fun initSerial() {
        manager = getSystemService(Context.USB_SERVICE) as UsbManager
        val availableDrivers: List<UsbSerialDriver> =
            UsbSerialProber.getDefaultProber().findAllDrivers(manager)
        if (availableDrivers.isEmpty()) {
            return
        }
        for (serialDriver in availableDrivers) {
            println(serialDriver.device.toString())
        }

        // Open a connection to the first available driver.
        driver = availableDrivers[0]
        if (!manager!!.hasPermission(driver!!.device)) {
            println("没有权限")
            //usbPermissionReceiver = new UsbPermissionReceiver();
            //申请权限
            val intent = Intent(ACTION_DEVICE_PERMISSION)
            val mPermissionIntent = PendingIntent.getBroadcast(this, 0, intent, 0)
            val permissionFilter = IntentFilter(ACTION_DEVICE_PERMISSION)
            this.registerReceiver(usbPermissionReceiver, permissionFilter)
            manager!!.requestPermission(driver!!.device, mPermissionIntent)
            return
        } else {
            println("有权限")
            openUsb(driver!!)
        }
    }

    private var usbPermissionReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            val action = intent.action
            if (ACTION_DEVICE_PERMISSION == action) {
                synchronized(this) {
                    val device =
                        intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
                    if (device!!.deviceName == driver?.device?.deviceName) {
                        if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                            //授权成功,在这里进行打开设备操作
                            println("授权成功")
                            try {
                                openUsb(driver!!)
                            } catch (e: IOException) {
                                e.printStackTrace()
                            }
                        } else {
                            println("授权失败")
                        }
                    }
                }
            }
        }
    }

    @Throws(IOException::class)
    fun openUsb(driver: UsbSerialDriver) {
        val connection: UsbDeviceConnection = manager?.openDevice(driver.device)?:
            return
        serialPort = driver.ports[0] // Most devices have just one port (port 0)
        serialPort?.open(connection)
        println("Dongle连接:已连接")
        serialPort?.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)
        val usbIoManager = SerialInputOutputManager(serialPort, object :
            SerialInputOutputManager.Listener {
            override fun onNewData(data: ByteArray) {
                callBack?.onNewData(data)
            }

            override fun onRunError(e: Exception) {
                serialPort = null
                println("Dongle连接:未连接")
                callBack?.onRunError(e)
            }
        })
        //ThreadUtils.getIoPool().execute(usbIoManager)
        Executors.newSingleThreadExecutor().submit(usbIoManager);
    }

    private fun onStartForeground() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val pendingIntent = PendingIntent.getActivity(this, 0, Intent(), PendingIntent.FLAG_MUTABLE)
                val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                val channel = NotificationChannel(CHANNEL_ID_STRING,getString(R.string.app_name),NotificationManager.IMPORTANCE_LOW)
                notificationManager.createNotificationChannel(channel)
                val nb: NotificationCompat.Builder = NotificationCompat.Builder(this,CHANNEL_ID_STRING)
                nb.setContentIntent(pendingIntent)
                val notification = nb.build()
                startForeground(SERVICE_ID,notification)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val notificationManager =getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                val channel = NotificationChannel(CHANNEL_ID_STRING,getString(R.string.app_name),NotificationManager.IMPORTANCE_LOW)
                notificationManager.createNotificationChannel(channel)
                val notification = Notification.Builder(applicationContext,CHANNEL_ID_STRING).build()
                startForeground(SERVICE_ID,notification)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}