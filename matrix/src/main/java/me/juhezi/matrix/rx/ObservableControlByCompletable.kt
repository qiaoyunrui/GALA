package me.juhezi.matrix.rx


import io.reactivex.rxjava3.core.CompletableObserver
import io.reactivex.rxjava3.core.CompletableSource
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.internal.disposables.DisposableHelper
import io.reactivex.rxjava3.internal.util.AtomicThrowable
import io.reactivex.rxjava3.internal.util.HalfSerializer
import io.reactivex.rxjava3.plugins.RxJavaPlugins
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * 实现 controlBy 操作符
 * 从 mergeBy 操作符魔改过来的
 */
class ObservableControlByCompletable<T : Any>(
    private val source: Observable<T>,
    private val other: CompletableSource
) : Observable<T>() {

    override fun subscribeActual(observer: Observer<in T>) {
        val parent = ControlByObserver(observer)
        observer.onSubscribe(parent)
        source.subscribe(parent)
        other.subscribe(parent.otherObserver)
    }

    /**
     * nothing
     */
    class ControlByObserver<T>(private val downstream: Observer<in T>) :
        AtomicInteger(), Observer<T>, Disposable {

        private val mainDisposable = AtomicReference<Disposable>()
        val otherObserver = OtherObserver(this)
        private val errors = AtomicThrowable()

        override fun onSubscribe(d: Disposable) {
            DisposableHelper.setOnce(mainDisposable, d)
        }

        override fun onNext(t: T) {
            HalfSerializer.onNext(downstream, t, this, errors)
        }

        override fun onError(ex: Throwable) {
            DisposableHelper.dispose(otherObserver)
            HalfSerializer.onError(downstream, ex, this, errors)
        }

        override fun onComplete() {
            DisposableHelper.dispose(otherObserver)
            HalfSerializer.onComplete(downstream, this, errors)
        }

        override fun isDisposed(): Boolean {
            return DisposableHelper.isDisposed(mainDisposable.get())
        }

        override fun dispose() {
            DisposableHelper.dispose(mainDisposable)
            DisposableHelper.dispose(otherObserver)
        }

        /**
         * nothing
         */
        fun otherError(ex: Throwable?) {
            DisposableHelper.dispose(mainDisposable)
            HalfSerializer.onError(downstream, ex, this, errors)
        }

        /**
         * nothing
         */
        fun otherComplete() {
            DisposableHelper.dispose(mainDisposable)
            HalfSerializer.onComplete(downstream, this, errors)
        }

        /**
         * nothing
         */
        class OtherObserver(val parent: ControlByObserver<*>) :
            AtomicReference<Disposable?>(),
            CompletableObserver {
            override fun onSubscribe(d: Disposable) {
                DisposableHelper.setOnce(this, d)
            }

            override fun onError(e: Throwable) {
                parent.otherError(e)
            }

            override fun onComplete() {
                parent.otherComplete()
            }
        }

        override fun toByte(): Byte {
            return get().toByte()
        }

        override fun toChar(): Char {
            return get().toChar()
        }

        override fun toShort(): Short {
            return get().toShort()
        }
    }

}

/**
 * 和 Complete 共同控制流，两条流只要有一个流发射了 onComplete 或者 onError 都会终止
 *
 * @param T
 * @param other
 * @return
 */
fun <T : Any> Observable<T>.controlBy(other: CompletableSource): Observable<T> {
    return RxJavaPlugins.onAssembly(ObservableControlByCompletable(this, other))
}