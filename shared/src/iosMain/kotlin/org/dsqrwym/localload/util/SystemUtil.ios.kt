package org.dsqrwym.localload.util

import kotlinx.cinterop.*
import platform.darwin.sysctlbyname
import platform.posix.size_tVar

@OptIn(ExperimentalForeignApi::class)
actual fun getCpuCount(): Int = memScoped {
    // 分配存储结果的变量
    val count = alloc<IntVar>()
    // 分配存储 size 的变量，必须初始化为 sizeof(int)
    val size = alloc<size_tVar>()
    size.value = sizeOf<IntVar>().toULong()

    // sysctlbyname 是 POSIX/Darwin 的标准系统调用
    // 参数含义: "hw.ncpu" (查询项), 输出指针, size 指针, null (新值), 0 (新值大小)
    val result = sysctlbyname("hw.ncpu", count.ptr, size.ptr, null, 0.toULong())

    if (result == 0) {
        count.value
    } else {
        // 如果获取失败，返回 1 作为默认值
        1
    }
}