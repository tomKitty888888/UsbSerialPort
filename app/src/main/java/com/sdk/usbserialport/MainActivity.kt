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
import org.example.adc.service.ADCService
import org.example.adc.service.impl.ADCService55Impl
import java.lang.Exception

class MainActivity : AppCompatActivity(), UsbService.CallBack {

    lateinit var mUsbService : UsbService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mBindService()
    }

    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName) {
            mBindService()
        }

        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            mUsbService = (service as UsbService.MsgBinder).getService()
            //设置listener
            mUsbService.setCallBack(this@MainActivity)

        }
    }

    /**
     * 启动服务
     */
    fun mBindService() {
        val intent = Intent()
        intent.action = UsbService.action
        intent.`package` = this.packageName
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    override fun onDestroy() {
        unbindService(serviceConnection)
        super.onDestroy()
    }

    override fun onNewData(data: ByteArray) {
       println("收到串口数据${data.size}")
    }

    override fun onRunError(e: Exception) {
        TODO("Not yet implemented")
    }



    /**
     * 获取设备控制指令
     *
     * @gatewayId 网关id
     * @barCode 需要控制的设备条码
     * @loopNumber 设备按键号
     * @action 按键动作（开，关）
     * @loopType 按键类型
     * @deviceType 设备类型
     */
//    fun getActionData(gatewayId: String,barCode: String,loopNumber: Int, action: Int, loopType: String,deviceType: String):ByteArray{
//        val keyStr = "" //平台获取
//        val service = ADCService55Impl(keyStr)
//        return service.getActionDate(gatewayId, barCode, loopNumber, action, loopType, deviceType)
//    }

    /**
     * 解析串口数据
     *
     * @byteArray 监听到的串口数据
     * @return 设备状态json数据
     */
//    fun parseSerialPortData(byteArray: ByteArray):String{
//        val keyStr = "" //平台获取
//        val service = ADCService55Impl(keyStr)
//        return service.parse(byteArray)
//    }

}