package duoshine.androidbluetoothpro.bluetoothprofile

/**
 *Created by chen on 2019
 */
class BluetoothNextProfile {
    companion object {
        /**
         * 继续发送下一包
         */
        val next: Int = 0

        /**
         * 继续发送下一包但不拦截doOnNext 数据将会推到onNext  一般用于最后一包时返回
         */
        val nextAll: Int = 1

        /**
         * 终止
         */
        val termination: Int = 2

        /**
         * 重发
         */
        val retry: Int = 3
    }
}