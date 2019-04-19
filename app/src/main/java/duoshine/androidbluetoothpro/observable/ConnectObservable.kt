package duoshine.androidbluetoothpro.observable

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.content.Context
import android.text.TextUtils
import duoshine.androidbluetoothpro.exception.BluetoothException
import io.reactivex.Observable
import io.reactivex.Observer

/**
 *Created by chen on 2019
 */
class ConnectObservable private constructor(
    context: Context,
    private val bluetoothAdapter: BluetoothAdapter,
    private val address: String
) : Observable<BluetoothGatt>() {

    private var mContext: Context? = null

    private val tag: String = "duo_shine"


    init {
        mContext = context.applicationContext
    }

    override fun subscribeActual(observer: Observer<in BluetoothGatt>?) {
        if (observer is BleGattCallbackObservable.BleGattCallbackObserver) {
            if (TextUtils.isEmpty(address)) {
                observer.onError(BluetoothException("address not null"))
                return
            }
            val remoteDevice = bluetoothAdapter.getRemoteDevice(address)
            val bluetoothGatt = remoteDevice.connectGatt(mContext, false, observer)
            observer.onNext(bluetoothGatt)
        }
    }

    companion object {
        fun create(context: Context, bluetoothAdapter: BluetoothAdapter, address: String): ConnectObservable {
            return ConnectObservable(context, bluetoothAdapter, address)
        }
    }
}