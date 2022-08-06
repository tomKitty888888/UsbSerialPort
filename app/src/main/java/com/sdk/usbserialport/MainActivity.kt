package com.sdk.usbserialport

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import com.sdk.usb.service.UsbService
import java.lang.Exception

class MainActivity : AppCompatActivity(), UsbService.CallBack {

    lateinit var mUsbService : UsbService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

//        val intentMqtt = Intent(this, UsbService::class.java)
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            startForegroundService(intentMqtt)
//        } else {
//            startService(intentMqtt)
//        }

        mBindService()
    }

    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName) {
            mBindService()
        }

        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            mUsbService = (service as UsbService.MsgBinder).getService()
            mUsbService.setCallBack(this@MainActivity)
        }
    }

    fun mBindService() {
        val intent = Intent()
        intent.action = UsbService.action
        intent.`package` = this.packageName
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        startService(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onNewData(data: ByteArray) {
       println("收到数据${data.size}")
    }

    override fun onRunError(e: Exception) {
        TODO("Not yet implemented")
    }


}