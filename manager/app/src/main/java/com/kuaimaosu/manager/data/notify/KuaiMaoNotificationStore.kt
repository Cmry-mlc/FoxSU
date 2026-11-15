package com.kuaimaosu.manager.data.notify

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * 通知已读状态。
 *
 * 通知内容本身由运行状态实时推导（版本更新 / 管理器签名 / 内核可用性），
 * 这里只持久化「哪些通知已经被看过」，用来算铃铛上的未读角标。
 */
object KuaiMaoNotificationStore {
    private const val PREF_NAME = "kuaimao_notifications"
    private const val KEY_READ_IDS = "read_ids"

    /** 已读 id 集合；变更会触发读取方重组。 */
    var readIds: Set<String> by mutableStateOf(emptySet())
        private set

    fun load(context: Context) {
        val stored = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getStringSet(KEY_READ_IDS, emptySet())
            .orEmpty()
        readIds = stored.toSet()
    }

    fun markRead(context: Context, ids: Collection<String>) {
        if (ids.isEmpty()) return
        val next = readIds + ids
        if (next == readIds) return
        readIds = next
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(KEY_READ_IDS, next)
            .apply()
    }

    fun markAllRead(context: Context, allIds: Collection<String>) {
        markRead(context, allIds)
    }
}
