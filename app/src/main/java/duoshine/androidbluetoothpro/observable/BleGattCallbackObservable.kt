package duoshine.androidbluetoothpro.observable

import android.bluetooth.*
import duoshine.androidbluetoothpro.bluetoothprofile.BluetoothConnectProfile
import duoshine.androidbluetoothpro.bluetoothprofile.BluetoothConnectProfile.Companion.enableNotifyFail
import duoshine.androidbluetoothpro.bluetoothprofile.BluetoothConnectProfile.Companion.enableNotifySucceed
import duoshine.androidbluetoothpro.bluetoothprofile.BluetoothConnectProfile.Companion.notifyNotFound
import duoshine.androidbluetoothpro.bluetoothprofile.BluetoothConnectProfile.Companion.serviceNotfound
import duoshine.androidbluetoothpro.bluetoothprofile.BluetoothNextProfile
import duoshine.androidbluetoothpro.bluetoothprofile.BluetoothWriteProfile
import duoshine.androidbluetoothpro.bluetoothprofile.BluetoothWriteProfile.Companion.writeFail
import duoshine.androidbluetoothpro.bluetoothprofile.BluetoothWriteProfile.Companion.writeSucceed
import io.reactivex.Observer
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Function
import java.util.*


/**
 *Created by chen on 2019
 *
 *
 * 1.扫描和写操作都使用该类来操作
 * 2.外部调用还是正常的函数
 * 3.在controller中根据命令字来执行对应的命令 连接 写?
 * 4.好处是不需要更顶级的observable了 在他们中使用as来强转observer 很不优雅
 * 5.坏处是此类的代码增加 增加了更多的分支语句 可维护性阅读性更差 更容易出bug
 */
object BleGattCallbackObservable : BluetoothGattCallback(), Disposable {


    /**
     * downstream
     */
    private var observer: Observer<in Response>? = null

    /**
     * 服务uuid
     */
    private var serviceUuid: UUID? = null

    /**
     * 写uuid
     */
    private var writeUuid: UUID? = null

    /**
     * 通知uuid
     */
    private var notifyUuid: UUID? = null

    private val tag: String = "duo_shine"

    /**
     * UUID的描述符
     */
    private val descriptors: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    /**
     *GATT client  无法用于取消一个正在连接的任务 因为他是在连接成功时赋值的
     */
    private var bluetoothGatt: BluetoothGatt? = null

    /**
     * 写操作特征
     */
    private var writeCharacteristic: BluetoothGattCharacteristic? = null

    /**
     * 指令queue
     * note:不会维护多条指令  只会维护单条指令的多包
     */
    private var linked: LinkedList<ByteArray>? = null

    /**
     * 自动发送下一包  默认非自动 只有用户调用了自动发送的方法才会触发 断开连接 或新的连接建立后恢复默认值
     */
    private var autoNext = false

    /**
     * 上一包数据 用来重发
     */
    private var oldNext: ByteArray? = null

    /**
     * function
     * ByteArray：远程设备的返回包 由外部来处理
     * Boolean：是否继续发送 由外部来处理
     */
    private var function: io.reactivex.functions.Function<ByteArray, Int>? = null

    /**
     * 是否自动连接自动断开连接的设备  用户手动断开的连接不会自动重连
     */
    private var isAutoConnect: Boolean = false

    /**
     * 是否连接
     */
    private var isConnect: Boolean = false


    init {
        linked = LinkedList()
    }

    fun create(
        observer: Observer<Response>?,
        serviceUuid: UUID?,
        writeUuid: UUID?,
        notifyUuid: UUID?
    ): BleGattCallbackObservable {
        this.serviceUuid = serviceUuid
        this.writeUuid = writeUuid
        this.notifyUuid = notifyUuid
        return get(observer)
    }

    fun get(observer: Observer<Response>?): BleGattCallbackObservable {
        this.observer = observer
        observer?.onSubscribe(this)
        return this
    }

    override fun isDisposed(): Boolean = isConnect

    override fun dispose() {
        disconnected()
    }

    /**
     * 断开 | 连接状态监听
     */
    override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
        super.onConnectionStateChange(gatt, status, newState)
        if (newState == BluetoothProfile.STATE_CONNECTED) {
            bluetoothGatt = gatt
            isConnect = true
            gatt?.discoverServices()
            //连接上蓝牙设备
            onNext(BluetoothConnectProfile.connected)
        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            disconnected()
            onNext(BluetoothConnectProfile.disconnected)
            autoConnect()
        }
    }

    /**
     * 自动重连
     */
    private fun autoConnect() {
        if (isAutoConnect) {
            //todo
        }
    }

    /**
     * 对emit的数据封装
     */
    private fun onNext(state: Int, byteArray: ByteArray? = null) {
        val response = Response(byteArray, state)
        observer?.onNext(response)
    }

    /**
     * 断开蓝牙连接
     */
    private fun disconnected() {
        isConnect = false
        bluetoothGatt?.disconnect()
        /**
         * close将导致onConnectionStateChange无法再接收到断开的状态了 认定为用户手动断开连接或是应用退出执行的断开连接状态
         * 反之认为是自动断开连接了,这可能是用户关闭了设备的ble? 或者远程设备休眠等
         */
        bluetoothGatt?.close()
    }

    /**
     *已发现新服务
     */
    override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
        super.onServicesDiscovered(gatt, status)
        displayGattServices(gatt)
    }

    /**
     *写操作的结果
     */
    override fun onCharacteristicWrite(
        gatt: BluetoothGatt?,
        characteristic: BluetoothGattCharacteristic?,
        status: Int
    ) {
        super.onCharacteristicWrite(gatt, characteristic, status)
        onNext(writeSucceed, characteristic?.value)
        loop()
    }

    /**
     *特征已改变 获取新值
     */
    override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
        super.onCharacteristicChanged(gatt, characteristic)
        val value = characteristic?.value ?: return
        isNext(value)
    }

    /**
     * 判断是否需要发送下一包
     */
    private fun isNext(characteristic: ByteArray) {
        function?.let {
            val isNext = it.apply(characteristic)
            when (isNext) {
                //继续发送下一包
                BluetoothNextProfile.next -> next()
                //重发上一包
                BluetoothNextProfile.retry -> writeCharacteristic(oldNext)
                //继续发送下一包 并不拦截Observable到onNext
                BluetoothNextProfile.nextAll -> {
                    //先将结果emit onNext
                    onNext(BluetoothWriteProfile.characteristicChanged, characteristic)
                    //继续发送下一包
                    next()
                }
            }
        } ?: onNext(BluetoothWriteProfile.characteristicChanged, characteristic)
    }

    /*启动通知通道并将给定描述符的值写入到远程设备*/
    private fun displayGattServices(gatt: BluetoothGatt?) {
        val service = gatt?.getService(serviceUuid)
        if (service == null) {
            onNext(notifyNotFound)
            return
        }
        val notifyCharacteristic = service.getCharacteristic(notifyUuid)
        writeCharacteristic = service.getCharacteristic(writeUuid)
        notifyCharacteristic?.let {
            //启用通知
            val result = gatt.setCharacteristicNotification(it, true)
            //描述特诊和控制特诊的某些行为 BluetoothGattDescriptor  比如开启通知(之前就遇到过设备通知是需要开启的)
            val descriptor = it.getDescriptor(descriptors) ?: BluetoothGattDescriptor(
                descriptors,
                BluetoothGattDescriptor.PERMISSION_WRITE
            )
            //更新此描述符的本地存储值
            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            gatt.writeDescriptor(descriptor)
            onNext(
                if (result) {
                    enableNotifySucceed
                } else {
                    enableNotifyFail
                }
            )
        } ?: onNext(serviceNotfound)
    }

    /**
     * 写操作函数
     */
    private fun writeCharacteristic(byteArray: ByteArray?) {
        if (writeCharacteristic == null || bluetoothGatt == null || byteArray == null) {
            onNext(writeFail, byteArray)
            return
        }
        writeCharacteristic?.value = byteArray
        val result = bluetoothGatt?.writeCharacteristic(writeCharacteristic) ?: false
        //记录此byte 用来在重发时使用
        oldNext = byteArray
        if (!result) {
            onNext(writeFail, byteArray)
        }
    }

    /**
     * 写单包指令
     */
    fun writeOnce(byteArray: ByteArray) {
        autoNext = false
        function = null
        writeCharacteristic(byteArray)
    }

    /**
     * 自动写操作函数 收到写入成功 自动下一包
     */
    fun writeAutoCharacteristic(more: MutableList<ByteArray>) {
        autoNext = true
        function = null
        clear()
        addAll(more)
        loop()
    }

    /**
     * 非自动写操作函数 收到doOnNext结果 决定是否发送下一包
     */
    fun writeNextCharacteristic(more: MutableList<ByteArray>, function: Function<ByteArray, Int>?) {
        this.function = function
        autoNext = false
        clear()
        addAll(more)
        next()
    }

    /**
     * 取出队列顶部的指令 并发送
     * note:它不是一个循环 只在获取到写入成功之后继续发送下一包
     */
    private fun loop() {
        if (!autoNext) {
            return
        }
        val l = linked?.poll() ?: return
        writeCharacteristic(l)
    }

    /**
     * 发送下一包
     */
    private fun next() {
        val l = linked?.poll() ?: return
        writeCharacteristic(l)
    }

    /**
     * 添加指令到队列
     */
    private fun addAll(more: MutableList<ByteArray>) {
        linked?.addAll(more)
    }

    /**
     * 清空指令 新指令都需要清空 防止上一次通信出现异常导致的指令堆积
     */
    private fun clear() {
        linked?.clear()
    }
}

