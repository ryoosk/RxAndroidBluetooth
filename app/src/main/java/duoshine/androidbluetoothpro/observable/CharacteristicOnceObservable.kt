package duoshine.androidbluetoothpro.observable

import android.bluetooth.BluetoothGatt
import io.reactivex.Observable
import io.reactivex.Observer

/**
 *Created by chen on 2019
 */
class CharacteristicOnceObservable private constructor(private val byteArray: ByteArray) : Observable<BluetoothGatt>() {

    override fun subscribeActual(observer: Observer<in BluetoothGatt>?) {
        if (observer is BleGattCallbackObservable.BleGattCallbackObserver) {
            observer.writeOnce(byteArray)
        }
    }

    companion object {
        fun create(byteArray: ByteArray): CharacteristicOnceObservable {
            return CharacteristicOnceObservable(byteArray)
        }
    }
}