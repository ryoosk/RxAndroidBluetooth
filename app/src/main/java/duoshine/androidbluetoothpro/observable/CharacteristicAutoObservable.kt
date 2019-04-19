package duoshine.androidbluetoothpro.observable

import android.bluetooth.BluetoothGatt
import io.reactivex.Observable
import io.reactivex.Observer

/**
 *Created by chen on 2019
 */
class CharacteristicAutoObservable private constructor(private val more: MutableList<ByteArray>) : Observable<BluetoothGatt>() {
    override fun subscribeActual(observer: Observer<in BluetoothGatt>?) {
        if (observer is BleGattCallbackObservable.BleGattCallbackObserver) {
            observer.writeAutoCharacteristic(more)
        }
    }

    companion object {
        fun create(more: MutableList<ByteArray>): CharacteristicAutoObservable {
            return CharacteristicAutoObservable(more)
        }
    }
}