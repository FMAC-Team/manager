package me.nekosu.aqnya.ui.screens

enum class LinuxCap(
    val value: Int,
    val label: String,
    val description: String,
) {
    CAP_CHOWN(0, "CHOWN", "任意更改文件 UID/GID"),
    CAP_DAC_OVERRIDE(1, "DAC_OVERRIDE", "绕过文件读写执行权限检查"),
    CAP_DAC_READ_SEARCH(2, "DAC_READ_SEARCH", "绕过文件读取/目录搜索权限检查"),
    CAP_FOWNER(3, "FOWNER", "绕过需要文件所有者检查的操作"),
    CAP_FSETID(4, "FSETID", "文件修改后保留 setuid/setgid 位"),
    CAP_KILL(5, "KILL", "向任意进程发送信号"),
    CAP_SETGID(6, "SETGID", "任意更改进程 GID"),
    CAP_SETUID(7, "SETUID", "任意更改进程 UID"),
    CAP_SETPCAP(8, "SETPCAP", "修改进程 capability 集合"),
    CAP_LINUX_IMMUTABLE(9, "LINUX_IMMUTABLE", "修改文件的不可变 (i) 和只追加 (a) 属性"),
    CAP_NET_BIND_SERVICE(10, "NET_BIND_SERVICE", "绑定 1024 以下特权端口"),
    CAP_NET_BROADCAST(11, "NET_BROADCAST", "网络广播和多播访问"),
    CAP_NET_ADMIN(12, "NET_ADMIN", "网络配置（接口/路由/防火墙）"),
    CAP_NET_RAW(13, "NET_RAW", "使用原始/数据报套接字 (ping/tcpdump)"),
    CAP_IPC_LOCK(14, "IPC_LOCK", "锁定共享内存段"),
    CAP_IPC_OWNER(15, "IPC_OWNER", "绕过 IPC 所有者检查"),
    CAP_SYS_MODULE(16, "SYS_MODULE", "加载/卸载内核模块"),
    CAP_SYS_RAWIO(17, "SYS_RAWIO", "访问 /dev/mem、ioperm 等底层 IO"),
    CAP_SYS_CHROOT(18, "SYS_CHROOT", "调用 chroot()"),
    CAP_SYS_PTRACE(19, "SYS_PTRACE", "ptrace/调试任意进程"),
    CAP_SYS_PACCT(20, "SYS_PACCT", "配置进程记账"),
    CAP_SYS_ADMIN(21, "SYS_ADMIN", "万能管理权限（挂载/命名空间/设置主机名等）"),
    CAP_SYS_BOOT(22, "SYS_BOOT", "调用 reboot() 和加载新内核"),
    CAP_SYS_NICE(23, "SYS_NICE", "提升进程优先级/设置调度策略"),
    CAP_SYS_RESOURCE(24, "SYS_RESOURCE", "配置系统资源限制（磁盘配额/保留空间）"),
    CAP_SYS_TIME(25, "SYS_TIME", "修改系统时钟/实时时钟"),
    CAP_SYS_TTY_CONFIG(26, "SYS_TTY_CONFIG", "配置 TTY 设备"),
    CAP_MKNOD(27, "MKNOD", "使用 mknod() 创建特殊文件"),
    CAP_LEASE(28, "LEASE", "对文件建立租借锁"),
    CAP_AUDIT_WRITE(29, "AUDIT_WRITE", "写入内核审计日志"),
    CAP_AUDIT_CONTROL(30, "AUDIT_CONTROL", "配置内核审计子系统"),
    CAP_SETFCAP(31, "SETFCAP", "设置文件 Capabilities"),
    CAP_MAC_OVERRIDE(32, "MAC_OVERRIDE", "绕过强制访问控制 (LSM/SELinux)"),
    CAP_MAC_ADMIN(33, "MAC_ADMIN", "配置/修改策略 (LSM/SELinux)"),
    CAP_SYSLOG(34, "SYSLOG", "特权 syslog 操作/读取 /proc/kmsg"),
    CAP_WAKE_ALARM(35, "WAKE_ALARM", "设置定时唤醒系统的闹钟"),
    CAP_BLOCK_SUSPEND(36, "BLOCK_SUSPEND", "防止系统进入休眠状态"),
    CAP_AUDIT_READ(37, "AUDIT_READ", "读取审计日志流"),
    CAP_PERFMON(38, "PERFMON", "执行性能监控操作"),
    CAP_BPF(39, "BPF", "加载和管理 BPF 程序"),
    CAP_CHECKPOINT_RESTORE(40, "CHECKPOINT_RESTORE", "执行检查点和恢复操作"),
}

val DEFAULT_CAPS: Set<LinuxCap> =
    setOf(
        LinuxCap.CAP_CHOWN,
        LinuxCap.CAP_DAC_OVERRIDE,
        LinuxCap.CAP_DAC_READ_SEARCH,
        LinuxCap.CAP_FOWNER,
        LinuxCap.CAP_SETUID,
        LinuxCap.CAP_SETGID,
        LinuxCap.CAP_SYS_ADMIN,
    )
    
enum class NksuNamespace(val value: Int, val label: String, val description: String) {
    INHERITED(0, "继承", "沿用调用方的 mount 命名空间"),
    INDIVIDUAL(1, "独立", "为此 UID 创建独立 mount 命名空间"),
    GLOBAL(2, "全局", "加入全局共享命名空间"),
}

data class AppConfig(
    val allowed: Boolean = false,
    val caps: Set<LinuxCap> = DEFAULT_CAPS,
    val selinuxDomain: String = "u:r:nksu:s0",
    val namespace: NksuNamespace = NksuNamespace.INHERITED,
)