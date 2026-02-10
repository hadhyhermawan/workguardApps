package com.workguard.navigation

import com.workguard.core.model.enums.FaceContext

object Routes {
    const val Auth = "auth"
    const val HomeRoot = "home_root"
    const val Home = "home"
    const val TaskStart = "task/start"
    const val TaskCamera = "task/camera"
    const val TaskComplete = "task/complete"
    const val FaceScan = "face/{context}"
    const val Patrol = "patrol"
    const val Payroll = "payroll"
    const val PayrollDetail = "payroll/detail"
    const val Profile = "profile"
    const val Chat = "chat"
    const val ChatThreadArg = "threadId"
    const val ChatThread = "chat/{$ChatThreadArg}"
    const val Scan = "scan"
    const val News = "news"
    const val NewsIdArg = "newsId"
    const val NewsDetail = "news/{$NewsIdArg}"
    const val Settings = "settings"
    const val PrivacyData = "settings/privacy"
    const val Policy = "settings/policy"
    const val Help = "settings/help"
    const val FaceEnroll = "settings/face-enroll"
    const val FaceEnrollCapture = "settings/face-enroll/capture"

    const val FaceResultKey = "face_result"

    fun faceScan(context: FaceContext): String = "face/${context.name}"

    fun newsDetail(id: Int): String = "news/$id"

    fun chatThread(id: String): String = "chat/$id"
}
