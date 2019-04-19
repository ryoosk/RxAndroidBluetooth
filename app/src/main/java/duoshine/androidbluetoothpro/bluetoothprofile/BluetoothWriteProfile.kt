package duoshine.androidbluetoothpro.bluetoothprofile

/**
 *Created by chen on 2019
 */
class BluetoothWriteProfile {
    companion object {
        //writeOnce 相关
        /**
         * 写入成功
         */
        val writeSucceed = 8

        /**
         * 写入失败
         */
        val writeFail = 9

        /**
         *  Characteristic变化 new值
         */
        val characteristicChanged = 10
    }
}