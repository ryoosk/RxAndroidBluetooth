# RxAndroidBluetooth
基于rxjava2的ble库

#### 初始化


	   private var bluetoothController: BluetoothWorker? = null

	
        bluetoothController = BluetoothController
            .Builder(this)
                      .setNotifyUuid(notifyUUID)
                      .setServiceUuid(serviceUUID)
                      .setWriteUuid(writeUuid)
                      .build()
  tips:所有可操作Api都在BluetoothWorker中.下面挨个介绍,使用不分先后顺序


#### 1.startLeScan
开启扫描,如果你的设备>=6.0请自行处理好定位权限,代码：
	
		scanDispose = bluetoothController!!
                .startLeScan()
		.subscribe(
                    { checkScanResult(it) },
                    { error -> checkError(error) },
                    { Log.d(tag, "扫描完成") })

startLeScan支持添加过滤条件,但是只支持API21及以上的设备扫描使用(调用者不需要维护版本差异,低于API21的设备RxAndroidBluetooth不会使用过滤条件,即使你传递了过滤规则)   
timer(非rxjava原生的静态操作符)定时扫描,如果不使用则一直扫描,直到dispose调用


扫描结果:ScanResult，这个对象需要介绍一下,它所支持的参数随着设备的等级提升而提升,对于API21一下的设备,稳定输出的只有(BluetoothDevice&rssi&scanRecord),使用时请注意   

停止扫描

	scanDispose?.dispose()

ps:每次扫描任务之前都需要.dispose(),否则你将开启两个扫描任务,这点很容易理解和rxjava+retrofit使用是一样的,你开启了两个request

#### 2.writeOnce
写操作-适用于单包指令,代码：
	
	 bluetoothController!!
                .writeOnce(byteArray)
			    .subscribe { response -> checkResult(response) }


使用它你只需要处理Response就可以了,Response的处理极为简单,只需要一个通用的解析函数即可：
	
	  private fun checkResult(response: Response) {
        when (response.code) {
            BluetoothWriteProfile.writeSucceed -> Log.d(tag, "写入成功")
            BluetoothWriteProfile.writeFail -> Log.d(tag, "写入失败")
            BluetoothWriteProfile.characteristicChanged -> Log.d(tag, "收到新值-${Arrays.toString(response.data)}")
        }
    } 

#### 3.writeAuto
写操作-适用于多包指令,它的表现形式是自动发送,接收一个list<ByteArray>集合


	  bluetoothController!!
                .writeAuto(list)
                .subscribe { response -> checkResult(response) }

我通常不建议使用此函数来执行写操作,它的执行原理是写入成功即发送下一包,它的结果处理:

	
	private fun checkResult(response: Response) {
        when (response.code) {
            BluetoothWriteProfile.writeSucceed -> Log.d(tag, "写入成功")
            BluetoothWriteProfile.writeFail -> Log.d(tag, "写入失败")
            BluetoothWriteProfile.characteristicChanged -> Log.d(tag, "收到新值-${Arrays.toString(response.data)}")
        }
    }


#### 4.writeNext
写操作-适用于多包指令,它的表现形式是调用者决定是否发送下一包,接收一个list<ByteArray>集合


	 bluetoothController!!
                .writeNext(list)
                .doOnNext(Function { byte ->
                    BluetoothNextProfile.next
                })
                .subscribe { response -> checkResult(response) }

使用此函数你只需要实现doOnNext(非rxjava原生，而是RxAndroidBluetooth的),它接收一个Function<ByteArray,Int>，输入类型是当前包返回的结果,调用者也许需要对此远程设备返回的数据进行效验？解密？或其他操作来决定是否继续发送下一包,请查看BluetoothNextProfile中的功能码,它支持重发等其他操作


#### 5.connect
连接远程设备

	 connectDisposable = bluetoothController!!
                .connect("xx:xx:xx:xx:xx:xx")
                .auto()
                .timer(6000, TimeUnit.MILLISECONDS)
                .subscribe(
                    { response -> checkResultState(response) },
                    { error -> checkError(error) }
                )


connect支持断开自动连接(非手动,如调用dispose后则不会重连),你只需要一个auto即可支持断开自动重连   
connect支持连接超时限制,你只需要一个timer操作符即可实现   
扫描结果处理（更多功能码请参考BluetoothConnectProfile）:


	private fun checkResultState(response: Response) {
        when (response.code) {
            BluetoothConnectProfile.connected -> Log.d(tag, "连接成功")
            BluetoothConnectProfile.disconnected -> Log.d(tag, "断开连接")
            BluetoothConnectProfile.connectTimeout -> Log.d(tag, "连接超时")
            BluetoothConnectProfile.enableNotifySucceed -> Log.d(tag, "启用通知特征成功")
            BluetoothConnectProfile.enableNotifyFail -> Log.d(tag, "启用通知特征失败")
            BluetoothConnectProfile.serviceNotfound -> Log.d(tag, "未获取到对应uuid的服务特征")
            BluetoothConnectProfile.notifyNotFound -> Log.d(tag, "未获取到对应uuid的通知特征")
            BluetoothConnectProfile.reconnection -> Log.d(tag, "重连中")
        }
    }

断开连接：

	connectDisposable?.dispose()

ps:每次连接任务之前最好都需要.dispose(),否则你将开启两个连接任务

#### 6.isEnabled
蓝牙是否启用

	bluetoothController!!.isEnabled()

#### 7.device
获取gatt对应的远程设备(不处于连接中也可以调用)  这个设备可能是当前正在连接的设备或是上一次连接的设备

	  bluetoothController!!
                .device()
                .subscribe(
                {device-> Log.d(tag, "$device")},
                {error-> Log.d(tag, "$error")},
                { Log.d(tag, "完成")})

#### 8.enable
开启蓝牙


	bluetoothController!!.enable()



#### note:
你可能需要在不需要扫描及断开连接的地方合适地调用dispose，这和平时使用rxjava是一样的,避免内存泄漏   
你如果不处理onError,那么它将由Android捕获

