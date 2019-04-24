package duoshine.androidbluetoothpro.observable

import android.bluetooth.*
import android.content.Context
import android.text.TextUtils
import duoshine.androidbluetoothpro.bluetoothprofile.BluetoothConnectProfile
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

    /**
     * 是否自动连接自动断开连接的设备  用户手动断开的连接不会自动重连
     */
    private var isAutoConnect: Boolean = false

    override fun subscribeActual(observer: Observer<in Response>?) {
        if (TextUtils.isEmpty(address)) {
            observer?.onError(BluetoothException("address not null"))
            return
        }
        val connectObserver = ConnectObserver(observer)
        observer?.onSubscribe(connectObserver)
        /**
         * 监听gatt server端的断开连接状态 防止client的被后续的写操作的Observable覆盖导致接收不到断开的回调
         * gattServer一定要在dispose时close 否则导致内存泄漏
         *
         */
        val mBluetoothManager = context
            .getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val gattServer = mBluetoothManager.openGattServer(context, object : BluetoothGattServerCallback() {
            override fun onConnectionStateChange(device: BluetoothDevice?, status: Int, newState: Int) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    observer?.onNext(Response(BluetoothConnectProfile.connected))
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    observer?.onNext(Response(BluetoothConnectProfile.disconnected))
                }
            }
        })
        connectObserver.setBluetoothGattServer(gattServer)
        val callback = BleGattCallbackObservable
            .create(connectObserver, serviceUuid, writeUuid, notifyUuid, isAutoConnect)
        val remoteDevice = bluetoothAdapter.getRemoteDevice(address)
        val gatt = remoteDevice.connectGatt(context, false, callback)
        connectObserver.setBluetoothGatt(gatt)
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

    /**
     * 启用断开自动连接
     */
    fun auto(): ConnectObservable {
        isAutoConnect = true
        return this
    }

    private class ConnectObserver(private val observer: Observer<in Response>?) : Observer<Response>, Disposable {
        private val tag: String = "duo_shine"

        private var upDisposable: Disposable? = null

        /**
         * 用于取消连接
         */
        private var bluetoothGatt: BluetoothGatt? = null

        /**
         * dispose时要执行close 否则内存泄漏
         */
        private var gattServer: BluetoothGattServer? = null

        override fun isDisposed(): Boolean {
            return upDisposable?.isDisposed ?: false
        }

        override fun dispose() {
            //在这里取消连接使用的bluetoothGatt是启动连接时获取的 可以取消一个正在连接中的任务 而blegattcallback中的gatt无法取消一个
//            正在连接中的任务
            bluetoothGatt?.disconnect()
            bluetoothGatt?.close()
            //再往上流调用dispose也就是取消连接 这里直接省略调用了
            gattServer?.close()
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

        fun setBluetoothGatt(gatt: BluetoothGatt?) {
            bluetoothGatt = gatt
        }

        fun setBluetoothGattServer(gattServer: BluetoothGattServer?) {
            this.gattServer = gattServer
        }
    }
}