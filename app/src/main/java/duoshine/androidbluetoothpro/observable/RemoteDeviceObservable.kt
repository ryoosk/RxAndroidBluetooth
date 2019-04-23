package duoshine.androidbluetoothpro.observable

import android.bluetooth.BluetoothDevice
import duoshine.androidbluetoothpro.exception.BluetoothException
import io.reactivex.Observable
import io.reactivex.Observer

/**
 *Created by chen on 2019
 */
class RemoteDeviceObservable : Observable<BluetoothDevice>() {

    override fun subscribeActual(observer: Observer<in BluetoothDevice?>?) {
        val callback = BleGattCallbackObservable.get(null)
        val device = callback.getDevice()
        if (device == null) {
            observer?.onError(BluetoothException("device获取失败"))
        } else {
            observer?.onNext(device)
            observer?.onComplete()
        }
    }

    companion object {
        fun create(): RemoteDeviceObservable {
            return RemoteDeviceObservable()
        }
    }
}