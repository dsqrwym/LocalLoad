package org.dsqrwym.localload.engine.config

/**
 * 压测引擎全局核心常量配置
 * * 本类存放所有与底层 I/O、网络性能及协程池相关的硬编码配置参数。
 * 调整本类中的参数会直接影响压测工具的 CPU 损耗与网络吞吐表现。
 */
object EngineConstants {
    /**
     * Worker 级常驻缓冲区大小 (L1-Cache Friendly Buffer Size)
     * 1. 本参数设置为操作系统的标准虚拟内存页大小（Page Size）的 2 倍，即 8192 字节。
     * 2. 内存页对齐: 现代主流操作系统(Linux/Windows)的虚拟内存页大小通常为 4096 字节 (4KB)。 8192 刚好是 2 个标准内存页(2 Pages)，能完美实现内存对齐，避免跨页断裂。
     * 3. CPU 缓存友好性 (CPU L1/L2 Cache Line): 现代 CPU 的 L1 Data Cache 通常为 32KB~48KB，缓存行(Cache Line)为 64 字节。 8192 字节可以完美整体嵌入 CPU 的 L1/L2 缓存中，在长寿命 Worker 的死循环消费中， 数据的覆盖读取(Overwrite)几乎 100% 命中 CPU 缓存，避免了昂贵的内存总线读写(Cache Miss)。
     * 4. 网络 I/O 性能: 8192 字节的缓冲区大小，可以最大化网络 I/O 的吞吐表现，避免了小包带来的网络开销。
     */
    const val IO_BUFFER_SIZE_BYTES = 8192
}