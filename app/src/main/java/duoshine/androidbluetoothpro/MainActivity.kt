package duoshine.androidbluetoothpro

import android.Manifest
import android.annotation.TargetApi
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.support.annotation.RequiresApi
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.text.TextUtils
import android.util.Log
import duoshine.androidbluetoothpro.bluetoothprofile.BluetoothConnectProfile
import duoshine.androidbluetoothpro.bluetoothprofile.BluetoothNextProfile
import duoshine.androidbluetoothpro.bluetoothprofile.BluetoothWriteProfile.Companion.characteristicChanged
import duoshine.androidbluetoothpro.bluetoothprofile.BluetoothWriteProfile.Companion.writeFail
import duoshine.androidbluetoothpro.bluetoothprofile.BluetoothWriteProfile.Companion.writeSucceed
import duoshine.androidbluetoothpro.observable.Response
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Function
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * 1.是不是所有的发送数据到 方法都支持重发等操作
 * 2.异常创建对应的异常 这样用户可以定义对应的异常解决方案
 * 3.检查所有异常可能产生的地方 框架的严谨性
 * 4.内存泄漏?
 * 5.参数或者操作符乱使用
 * 6.考虑将collback的连接监听由外部传入?
 * 7.断开自动连接功能  √
 * 8.返回当前gatt对应的远程设备
 * 9.支持心跳包指令-用户实现 使用rx举例
 * 10.考虑支持传输等级
 * 11.考虑支持mtu扩展
 *
 */

class MainActivity : AppCompatActivity() {
    private val tag: String = "duo_shine"
    private var bluetoothController: BluetoothWorker? = null


    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        requestPermission()
        val serviceUUID = UUID.fromString("f000c0e0-0451-4000-b000-000000000000")
        val notifyUUID = UUID.fromString("f000c0e1-0451-4000-b000-000000000000")
        val writeUuid = UUID.fromString("f000c0e1-0451-4000-b000-000000000000")
        bluetoothController =
                BluetoothController.Builder(this)
                    .setNotifyUuid(notifyUUID)
                    .setServiceUuid(serviceUUID)
                    .setWriteUuid(writeUuid)
                    .build()
        //扫描
        startScan()
        //连接
        connect()
        //发送单包数据
        sendOnce()
        //发送多包数据  不需要根据回调决定是否发送下一包
        sendAutoMore()
        //发送多包数据 根据设备返回的指令决定是否发送下一包
        sendMore()
        //新的连接
        newConnect()
        //获取gatt对应的远程设备
        device()
    }

    private fun device() {
        device.setOnClickListener {
            bluetoothController!!.device()
        }
    }

    private fun newConnect() {
        newConnect.setOnClickListener {
           disposable?.dispose()
            bluetoothController!!
                .connect("BB:A0:50:0B:23:0D")
                .timer(6000, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { response -> checkResultState(response) },
                    { error -> Log.d(tag, "error:$error") }
                )
        }
    }

    private fun sendMore() {
        moreCallback.setOnClickListener {
            val byteArray = byteArrayOf(0x1D, 0x00, 0x00, 0xC6.toByte(), 0xE1.toByte(), 0x00)
            val list = mutableListOf(byteArray, byteArray, byteArray, byteArray, byteArray, byteArray, byteArray)
            bluetoothController!!
                .writeNext(list)
                .doOnNext(Function { byte ->
                    BluetoothNextProfile.next
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { response -> checkResult(response) }
        }
    }

    private fun sendAutoMore() {
        val byteArray = byteArrayOf(0x1D, 0x00, 0x00, 0xC6.toByte(), 0xE1.toByte(), 0x00)
        val list = mutableListOf(byteArray, byteArray, byteArray, byteArray, byteArray, byteArray, byteArray)
        more.setOnClickListener {
            bluetoothController!!
                .writeAuto(list)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { response -> checkResult(response) }
        }
    }

    private fun sendOnce() {
        send.setOnClickListener {
            val byteArray = byteArrayOf(0x1D, 0x00, 0x00, 0xC6.toByte(), 0xE1.toByte(), 0x00)
            bluetoothController!!
                .writeOnce(byteArray)
                .subscribe { response -> checkResult(response) }
        }

        ceshi1.setOnClickListener {

        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun startScan() {
        var dispose: Disposable? = null
        val filters = ArrayList<ScanFilter>()
        filters.add(ScanFilter.Builder().setDeviceName("TK-00000CB5").build())
        filters.add(ScanFilter.Builder().setDeviceName("TL-01020304").build())
        scanObservable.setOnClickListener {
            dispose?.dispose()
            dispose = bluetoothController!!
                .startLeScan(null, filters)
                .timer(6000, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .filter { response ->
                    !TextUtils.isEmpty(response.getDevice()?.name)
                }
                .map {
                    it.getDevice()
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    {
                        checkScanResult(it)
                    },
                    { error -> Log.d(tag, "扫描出错$error") },
                    { Log.d(tag, "扫描完成") })
        }

        stopScan.setOnClickListener {
            dispose?.dispose()
        }
    }
    var disposable: Disposable? = null
    private fun connect() {
        connect.setOnClickListener {
            disposable?.dispose()
            disposable = bluetoothController!!
                .connect("BB:A0:50:04:15:12")
                .auto()
                .timer(6000, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { response -> checkResultState(response) },
                    { error -> Log.d(tag, "error:$error") }
                )
        }

        //断开连接
        disconnected.setOnClickListener {
            disposable?.dispose()
        }
    }

    private fun checkScanResult(it: BluetoothDevice?) {
        Log.d(tag, " 扫描到设备:${it!!.name}")
    }

    @TargetApi(Build.VERSION_CODES.P)
    private fun add() = 2

    private fun checkResultState(response: Response) {
        when (response.code) {
            BluetoothConnectProfile.connected -> Log.d(tag, "连接成功")
            BluetoothConnectProfile.disconnected -> Log.d(tag, "断开连接")
            BluetoothConnectProfile.connectTimeout -> Log.d(tag, "连接超时")
            BluetoothConnectProfile.enableNotifySucceed -> Log.d(tag, "启用通知特征成功")
            BluetoothConnectProfile.enableNotifyFail -> Log.d(tag, "启用通知特征失败")
            BluetoothConnectProfile.serviceNotfound -> Log.d(tag, "未获取到对应uuid的服务特征")
            BluetoothConnectProfile.notifyNotFound -> Log.d(tag, "未获取到对应uuid的通知特征")
        }
    }

    private fun checkResult(response: Response) {
        when (response.code) {
            writeSucceed -> Log.d(tag, "写入成功")
            writeFail -> Log.d(tag, "写入失败")
            characteristicChanged -> Log.d(tag, "收到新值-${Arrays.toString(response.data)}")
        }
    }

    /*
   请求授权
    */
    private fun requestPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            val permissions = arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_PHONE_STATE
            )
            val permissionslist = ArrayList<String>()
            for (permission in permissions) {
                if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                    permissionslist.add(permission)
                }
            }
            if (permissionslist.size != 0) {
                val permissionsArray = permissionslist.toTypedArray()
                ActivityCompat.requestPermissions(
                    this, permissionsArray,
                    22
                )
            }
        }
    }

    private fun isMainThread(): Boolean {
        return Looper.getMainLooper().thread.id == Thread.currentThread().id
    }
}


