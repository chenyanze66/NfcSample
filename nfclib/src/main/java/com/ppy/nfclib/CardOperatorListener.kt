package com.ppy.nfclib

/**
 * NFC与CPU卡片交互监听.
 * Created by ZP on 2017/9/20.
 */

interface CardOperatorListener {

    /**
     * CPU卡是否被NFC检测到.
     *
     * @param isConnected true 已连接 false 未连接
     */
    fun onCardConnected(isConnected: Boolean)

    /**
     * NFC异常，例如手机不支持NFC，手机NFC未开启.
     *
     * @param code    异常状态码
     * @param message 异常信息
     */
    fun onException(code: Int, message: String)
}
