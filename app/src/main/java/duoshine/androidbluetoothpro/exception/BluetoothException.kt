package duoshine.androidbluetoothpro.exception

/**
 *Created by chen on 2019
 */
data class BluetoothException(
     var detailMessage: String? = null
) : RuntimeException()