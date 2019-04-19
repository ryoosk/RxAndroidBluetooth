package duoshine.androidbluetoothpro.observable

import duoshine.androidbluetoothpro.bluetoothprofile.BluetoothConnectProfile
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.disposables.Disposable
import java.util.concurrent.TimeUnit

/**
 *Created by chen on 2019
 */
class ConnectTimeoutObservable(
    private val source: Observable<Response>,
    private val time: Long,
    private val timeUnit: TimeUnit
) : Observable<Response>() {
    private val tag: String = "duo_shine"
    private var timeout: Disposable? = null

    override fun subscribeActual(observer: Observer<in Response>?) {
        val timeoutObserver = ConnectTimeoutObserver(observer)
        source.subscribe(timeoutObserver)
        observer?.onSubscribe(timeoutObserver)
        timeout = Observable
            .timer(time, timeUnit)
            .subscribe {
                //时间到后判断任务是否完成
                if (!timeoutObserver.isSucceed) {
                    timeoutObserver.onNext(Response(null, BluetoothConnectProfile.connectTimeout))
                }
            }
    }

    private class ConnectTimeoutObserver(private val observer: Observer<in Response>?) : Observer<Response>, Disposable {
        private var upstream: Disposable? = null
        private val tag: String = "duo_shine"
        /**
         * 是否成功
         */
        var isSucceed = false

        override fun isDisposed(): Boolean {
            return false
        }

        override fun dispose() {
            upstream?.dispose()
        }

        override fun onComplete() {
            observer?.onComplete()
        }

        override fun onSubscribe(d: Disposable) {
            upstream = d
        }

        override fun onNext(t: Response) {
            observer?.onNext(t)
            //成功
            if (t.code == BluetoothConnectProfile.connected) {
                isSucceed = true
            } else if (t.code == BluetoothConnectProfile.connectTimeout) {
                //超时
                dispose()
            }
        }

        override fun onError(e: Throwable) {
            observer?.onError(e)
        }
    }
}