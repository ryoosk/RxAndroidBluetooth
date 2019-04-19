package duoshine.androidbluetoothpro.observable

import android.bluetooth.BluetoothGatt
import io.reactivex.Observable
import io.reactivex.Observer

/**
 *Created by chen on 2019
 */
class CharacteristicNextObservable private constructor(private val more: MutableList<ByteArray>) : Observable<BluetoothGatt>() {

    override fun subscribeActual(observer: Observer<in BluetoothGatt>?) {
        if (observer is BleGattCallbackObservable.BleGattCallbackObserver) {
            observer.writeNextCharacteristic(more)
        }
    }

    companion object {
        fun create(more: MutableList<ByteArray>): CharacteristicNextObservable {
            return CharacteristicNextObservable(more)
        }
    }
}