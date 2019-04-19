package duoshine.androidbluetoothpro

import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanSettings
import duoshine.androidbluetoothpro.observable.BleGattCallbackObservable
import duoshine.androidbluetoothpro.observable.ScanLeObservable

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
    fun writeOnce(byteArray: ByteArray): BleGattCallbackObservable

    /**
     * writeAuto 多包 自动发送
     *
     *适用于不需要检查结果就能继续发送下一包的指令 规则是收到当前包的写入成功 继续发送下一包
     * 一般我不推荐使用该操作 为什么？
     * 原因是你知道远程ble设备他究竟有没有收到,只是Android端单方面认为写入成功 如果使用该方法 请做好测试
     */
    fun writeAuto(more: MutableList<ByteArray>): BleGattCallbackObservable

    /**
     * writeNext 多包 非自动发送  用户通过doOnNext决定是否发送下一包
     * 适用于多包指令 每包都需要检验结果才能发送下一包或上一包重发
     */
    fun writeNext(more: MutableList<ByteArray>): BleGattCallbackObservable

    /**
     * connect
     */
    fun connect(address: String): BleGattCallbackObservable

    /**
     * 蓝牙是否启用
     */
    fun isEnabled(): Boolean

    /**
     * 释放资源
     */
    fun recycler()

    /*
    //设置断开自动连接
    public abstract BluetoothBLeClass setAutoConnect(boolean isAutoConnect);  连接方式改为true试试
    在自动断开后进行连接 如果是手动断开则不进行连接

    //返回当前gatt对应的远程设备
    public abstract BluetoothDevice getRemoteDevice(); 使用rx的方式获取




    //考虑支持心跳包指令
    用户自实现 使用rx举例

    //考虑支持扫描等级
    在扫描时添加函数支持 放在后面做

    //考虑支持传输等级
    在发送数据时添加函数支持 放在后面做

    //考虑支持mtu扩展
     */
}