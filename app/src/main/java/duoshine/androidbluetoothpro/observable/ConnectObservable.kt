package duoshine.androidbluetoothpro.observable

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.text.TextUtils
import duoshine.androidbluetoothpro.exception.BluetoothException
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.disposables.Disposable
import java.util.*
import java.util.concurrent.TimeUnit

/**
 *Created by chen on 2019
 */
class ConnectObservable private constructor(
    private val context: Context,
    private val bluetoothAdapter: BluetoothAdapter,
    private val address: String,
    private val serviceUuid: UUID?,
    private val writeUuid: UUID?,
    private val notifyUuid: UUID?
) : Observable<Response>() {

    private val tag: String = "duo_shine"

    override fun subscribeActual(observer: Observer<in Response>?) {
        if (TextUtils.isEmpty(address)) {
            observer?.onError(BluetoothException("address not null"))
            return
        }
        val connectObserver = ConnectObserver(observer)
        observer?.onSubscribe(connectObserver)
        val callback = BleGattCallbackObservable
            .create(connectObserver, serviceUuid, writeUuid, notifyUuid)
        val remoteDevice = bluetoothAdapter.getRemoteDevice(address)
        val gatt = remoteDevice.connectGatt(context, false, callback)

    }

    companion object {
        fun create(
            context: Context,
            bluetoothAdapter: BluetoothAdapter,
            address: String,
            serviceUuid: UUID?,
            writeUuid: UUID?,
            notifyUuid: UUID?
        ): ConnectObservable {
            return ConnectObservable(
                context.applicationContext,
                bluetoothAdapter,
                address,
                serviceUuid,
                writeUuid,
                notifyUuid
            )
        }
    }

    /**
     * 连接超时处理 不指定时间默认6秒 不调用timer 则等待系统连接超时 大概为12s(别问我 我管他多久啊 我一般设置的时间短,
     * 用户哪有耐心等你连个半天啊) 或dispose停止连接任务
     */
    fun timer(time: Long = 6000, timeUnit: TimeUnit = TimeUnit.MILLISECONDS): ConnectTimeoutObservable {
        return ConnectTimeoutObservable(this, time, timeUnit)
    }

    private class ConnectObserver(private val observer: Observer<in Response>?) : Observer<Response>, Disposable {
        private var upDisposable: Disposable? = null

        override fun isDisposed(): Boolean {
            return upDisposable?.isDisposed ?:false
        }

        override fun dispose() {
            upDisposable?.dispose()
        }

        override fun onComplete() {
            observer?.onComplete()
        }

        override fun onSubscribe(d: Disposable) {
            upDisposable = d
        }

        override fun onNext(t: Response) {
            observer?.onNext(t)
        }

        override fun onError(e: Throwable) {
            observer?.onError(e)
        }
    }
}
