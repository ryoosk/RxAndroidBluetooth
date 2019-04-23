package duoshine.androidbluetoothpro.observable

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanSettings
import android.os.Build
import android.os.Looper
import android.support.annotation.RequiresApi
import android.util.Log
import duoshine.androidbluetoothpro.exception.BluetoothException
import duoshine.androidbluetoothpro.util.ScanResult
import duoshine.androidbluetoothpro.util.ScanResultConverter
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.disposables.Disposable
import java.util.*
import java.util.concurrent.TimeUnit


/**
 *Created by chen on 2019
 */
class ScanLeObservable private constructor(
    private val bluetoothAdapter: BluetoothAdapter?,
    private var settings: ScanSettings?, private var filters: MutableList<ScanFilter>?
) :
    Observable<ScanResult>() {
    private val tag: String = "duo_shine"

    companion object {
        fun create(
            bluetoothAdapter: BluetoothAdapter?,
            settings: ScanSettings?,
            filters: MutableList<ScanFilter>?
        ): ScanLeObservable {
            return ScanLeObservable(bluetoothAdapter, settings, filters)
        }
    }

    /**
     * 1.扫描模式可传入
     * 2.指定过滤的服务uuid
     * 3.
     */
    override fun subscribeActual(observer: Observer<in ScanResult>?) {
        if (bluetoothAdapter == null) {
            observer?.onError(BluetoothException("bluetoothAdapter not null"))
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val scanCallback = ScanLeObservable.ScanCallbackObserver(observer, bluetoothAdapter)
            observer?.onSubscribe(scanCallback)
            val scanner = bluetoothAdapter.bluetoothLeScanner
            //note:这里极可能为null 比如蓝牙未开启
            if (scanner == null) {
                observer?.onError(BluetoothException("bluetooth not in the STATE_ON or STATE_BLE_ON"))
                return
            }
            settings = settings ?: ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()
            filters = filters ?: ArrayList()
            scanner.startScan(filters, settings, scanCallback)
        } else {
            //开始扫描
            val scanCallback = ScanLeObservable.LeScanCallbackObserver(observer, bluetoothAdapter)
            observer?.onSubscribe(scanCallback)
            bluetoothAdapter.startLeScan(scanCallback)
        }
    }

    private fun isMainThread(): Boolean {
        return Looper.getMainLooper().thread.id == Thread.currentThread().id
    }

    /**
     * 定时扫描  不指定时间默认6秒 不调用timer 则扫描不会停止 直到dispose
     */
    fun timer(time: Long = 6000, timeUnit: TimeUnit = TimeUnit.MILLISECONDS): ScanTimerObservable {
        return ScanTimerObservable(this, time, timeUnit)
    }

    /**
     * < LOLLIPOP
     */
    private class LeScanCallbackObserver(
        private var observer: Observer<in ScanResult>?,
        private val bluetoothAdapter: BluetoothAdapter?
    ) : BluetoothAdapter.LeScanCallback, Disposable {

        private val tag: String = "duo_shine"

        override fun isDisposed(): Boolean {
            return false
        }

        override fun dispose() {
            Log.d(tag, "< LOLLIPOP-dispose")
            bluetoothAdapter?.stopLeScan(this)
            //不指定内存泄漏 由于bluetoothLeScanner持有这个callback
            observer = null
        }

        override fun onLeScan(device: BluetoothDevice?, rssi: Int, scanRecord: ByteArray?) {
            val convert = ScanResultConverter.convert0(device, rssi, scanRecord)
            if (convert == null) {
                observer?.onError(BluetoothException("device is null"))
                return
            }
            observer?.onNext(convert)
        }
    }

    /**
     * >=LOLLIPOP
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private class ScanCallbackObserver(
        private var observer: Observer<in ScanResult>?,
        private val bluetoothAdapter: BluetoothAdapter?
    ) : Disposable, ScanCallback() {

        private val tag: String = "duo_shine"

        override fun isDisposed(): Boolean {
            return false
        }

        override fun dispose() {
            val scanner = bluetoothAdapter?.bluetoothLeScanner
            scanner?.stopScan(this)
            Log.d(tag, "> LOLLIPOP-dispose")
            //不指定内存泄漏 由于bluetoothLeScanner持有这个callback
            observer = null
        }

        override fun onScanResult(callbackType: Int, result: android.bluetooth.le.ScanResult?) {
            if (result == null) {
                observer?.onError(BluetoothException("ScanResult is null"))
                return
            }
            val convert = ScanResultConverter.convert(callbackType, result)
            if (convert == null) {
                observer?.onError(BluetoothException("device is null"))
                return
            }
            observer?.onNext(convert)
        }
    }
}