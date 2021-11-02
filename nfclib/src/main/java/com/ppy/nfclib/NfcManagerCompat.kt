package com.ppy.nfclib

import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.ppy.nfclib.exception.ConnectTagException
import com.ppy.nfclib.exception.ExceptionConstant
import com.ppy.nfclib.reader.BaseCardReader
import com.ppy.nfclib.reader.NfcCardReader
import com.ppy.nfclib.util.Logger
import com.ppy.nfclib.util.Printer
import com.ppy.nfclib.util.Util
import java.io.IOException

class NfcManagerCompat(
    activity: ComponentActivity, private var delay: Int = 0, enableSound: Boolean = true,
    val printer: Printer, val cardOperatorListener: CardOperatorListener? = null
) : INfcManagerCompat {

    private val mCallback = object : CardReaderInnerCallback {
        override fun onNfcNotExit() {
            cardOperatorListener?.onException(
                ExceptionConstant.NFC_NOT_EXIT,
                ExceptionConstant.mNFCException.get(ExceptionConstant.NFC_NOT_EXIT)
            )
        }

        override fun onNfcNotEnable() {
            cardOperatorListener?.onException(
                ExceptionConstant.NFC_NOT_ENABLE,
                ExceptionConstant.mNFCException.get(ExceptionConstant.NFC_NOT_ENABLE)
            )
        }

        override fun onCardConnected(isConnected: Boolean) {
            cardOperatorListener?.onCardConnected(isConnected)
        }

        override fun onException(exception: Exception) {
            Util.MainThreadExecutor().execute {
                if (exception is ConnectTagException) {
                    cardOperatorListener?.onException(
                        ExceptionConstant.CONNECT_TAG_FAIL,
                        exception.message!!
                    )
                }
            }
        }
    }

    private val mCardReader: BaseCardReader by lazy {
        NfcCardReader.Factory(activity, mCallback)
    }

    private val mNfcStateBroadcastReceiver by lazy {
        object : NfcStatusChangeBroadcastReceiver() {
            override fun onCardPayMode() {
                super.onCardPayMode()
                cardOperatorListener?.onCardPay()
            }

            override fun onNfcOff() {
                super.onNfcOff()
                cardOperatorListener?.onNfcEnable(false)
            }

            override fun onNfcOn() {
                super.onNfcOn()
                cardOperatorListener?.onNfcEnable(true)
            }

            override fun onNfcTurningOff() {
                super.onNfcTurningOff()
                cardOperatorListener?.onNfcTurning(false)
            }

            override fun onNfcTurningOn() {
                super.onNfcTurningOn()
                cardOperatorListener?.onNfcTurning(true)
            }
        }
    }


    init {
        Logger.get().setUserPrinter(printer)
        if (delay < 0) {
            delay = 0
        }
        mCardReader.enablePlatformSound(enableSound)
        mCardReader.setReaderPresenceCheckDelay(delay)
        //lifecycle for NfcManager
        activity.lifecycle.addObserver(object : LifecycleObserver {
            @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
            fun onCreate() {

                activity.registerReceiver(
                    mNfcStateBroadcastReceiver,
                    NfcStatusChangeBroadcastReceiver.nfcBroadcastReceiverIntentFilter
                )

                /**
                 * 如果action为android.nfc.action.TECH_DISCOVERED，可以直接读取Tag进去读写卡操作。
                 * 后续onResume中enableCardReader为了让当前activity有继续读写卡能力。
                 */
                dispatchIntent(activity.intent)
            }


            @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
            fun onResume() {
                if (mCardReader.checkNfc()) {
                    mCardReader.enableCardReader()
                }
            }


            @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            fun onPause() {
                mCardReader.disableCardReader()
            }

            @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            fun onDestroy() {
                activity.unregisterReceiver(mNfcStateBroadcastReceiver)
                mCardReader.stopCheckThread()
            }
        })
    }


    override fun isCardConnect(): Boolean {
        return mCardReader.isCardConnected
    }

    private fun dispatchIntent(intent: Intent?) {
        intent?.let {
            val tag = it.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
            if (tag != null) {
                mCardReader.dispatchTag(tag)
            } else {
                Logger.get().println("dispatchIntent: tag is null")
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        dispatchIntent(intent)
    }

    @Throws(IOException::class)
    override fun sendData(data: ByteArray): String {
        mCardReader.let {
            return Util.byteArrayToHexString(it.transceive(data))
        }
    }

    @Throws(IOException::class)
    override fun tranceive(data: ByteArray): ByteArray {
        mCardReader.let {
            return it.transceive(data)
        }
    }

}