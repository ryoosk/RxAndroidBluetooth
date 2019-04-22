package duoshine.androidbluetoothpro

import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanSettings
import duoshine.androidbluetoothpro.observable.*

/**
 *Created by chen on 2019
 */
interface BluetoothWorker {
    /**
     * scan
     * settings:scan mode 不传使用默认SCAN_MODE_LOW_LATENCY
     * filters:filter 不传则不过滤
     */
    fun startLeScan(
        settings: ScanSettings?,
        filters: MutableList<ScanFilter>?
    ): ScanLeObservable

    /**
     * writeOnce 单包
     */
    fun writeOnce(byteArray: ByteArray): CharacteristicOnceObservable

    /**
     * writeAuto 多包 自动发送
     *
     *适用于不需要检查结果就能继续发送下一包的指令 规则是收到当前包的写入成功 继续发送下一包
     * 一般我不推荐使用该操作 为什么？
     * 原因是你知道远程ble设备他究竟有没有收到,只是Android端单方面认为写入成功 如果使用该方法 请做好测试
     */
    fun writeAuto(more: MutableList<ByteArray>): CharacteristicAutoObservable

    /**
     * writeNext 多包 非自动发送  用户通过doOnNext决定是否发送下一包
     * 适用于多包指令 每包都需要检验结果才能发送下一包或上一包重发
     */
    fun writeNext(more: MutableList<ByteArray>): CharacteristicNextObservable

    /**
     * connect
     */
    fun connect(address: String): ConnectObservable

    /**
     * 蓝牙是否启用
     */
    fun isEnabled(): Boolean

    /**
     * 获取gatt对应的远程设备
     */
    /*fun device(): RemoteDeviceObservable {
        return
    }*/
}