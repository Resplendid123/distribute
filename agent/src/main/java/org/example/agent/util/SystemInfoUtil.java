package org.example.agent.util;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.util.HashMap;
import java.util.Map;

/**
 * 系统信息工具类
 * 获取磁盘使用率和内存占用信息
 */
public class SystemInfoUtil {

    /**
     * 获取系统信息
     * @return 包含磁盘使用率和内存占用的信息Map
     */
    public static Map<String, Object> getSystemInfo() {
        Map<String, Object> info = new HashMap<>();
        
        // 获取磁盘信息
        info.put("disk", getDiskInfo());
        
        // 获取内存信息
        info.put("memory", getMemoryInfo());
        
        // 获取CPU信息
        info.put("cpu", getCpuInfo());
        
        return info;
    }

    /**
     * 获取磁盘使用信息
     */
    public static Map<String, Object> getDiskInfo() {
        Map<String, Object> diskInfo = new HashMap<>();
        
        try {
            File root = new File("/");
            long totalSpace = root.getTotalSpace();
            long usableSpace = root.getUsableSpace();
            long usedSpace = totalSpace - usableSpace;
            
            double usagePercentage = (double) usedSpace / totalSpace * 100;
            
            diskInfo.put("total", formatBytes(totalSpace));
            diskInfo.put("used", formatBytes(usedSpace));
            diskInfo.put("usable", formatBytes(usableSpace));
            diskInfo.put("usagePercentage", String.format("%.2f%%", usagePercentage));
            
        } catch (Exception e) {
            diskInfo.put("error", "Failed to get disk info: " + e.getMessage());
        }
        
        return diskInfo;
    }

    /**
     * 获取内存使用信息
     */
    public static Map<String, Object> getMemoryInfo() {
        Map<String, Object> memoryInfo = new HashMap<>();
        
        try {
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
            MemoryUsage nonHeapUsage = memoryBean.getNonHeapMemoryUsage();
            
            // 堆内存信息
            Map<String, Object> heapInfo = new HashMap<>();
            heapInfo.put("init", formatBytes(heapUsage.getInit()));
            heapInfo.put("used", formatBytes(heapUsage.getUsed()));
            heapInfo.put("committed", formatBytes(heapUsage.getCommitted()));
            heapInfo.put("max", formatBytes(heapUsage.getMax()));
            heapInfo.put("usagePercentage", String.format("%.2f%%", 
                    (double) heapUsage.getUsed() / heapUsage.getMax() * 100));
            
            // 非堆内存信息
            Map<String, Object> nonHeapInfo = new HashMap<>();
            nonHeapInfo.put("init", formatBytes(nonHeapUsage.getInit()));
            nonHeapInfo.put("used", formatBytes(nonHeapUsage.getUsed()));
            nonHeapInfo.put("committed", formatBytes(nonHeapUsage.getCommitted()));
            nonHeapInfo.put("max", nonHeapUsage.getMax() == -1 ? "unlimited" : formatBytes(nonHeapUsage.getMax()));
            
            // 系统内存
            // 在某些系统上使用 Runtime 获取内存信息
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long maxMemory = runtime.maxMemory();
            long usedMemory = totalMemory - freeMemory;
            
            Map<String, Object> systemInfo = new HashMap<>();
            systemInfo.put("total", formatBytes(totalMemory));
            systemInfo.put("used", formatBytes(usedMemory));
            systemInfo.put("free", formatBytes(freeMemory));
            systemInfo.put("max", formatBytes(maxMemory));
            systemInfo.put("usagePercentage", String.format("%.2f%%", 
                    (double) usedMemory / totalMemory * 100));
            
            memoryInfo.put("heap", heapInfo);
            memoryInfo.put("nonHeap", nonHeapInfo);
            memoryInfo.put("system", systemInfo);
            
        } catch (Exception e) {
            memoryInfo.put("error", "Failed to get memory info: " + e.getMessage());
        }
        
        return memoryInfo;
    }

    /**
     * 获取CPU使用信息
     */
    public static Map<String, Object> getCpuInfo() {
        Map<String, Object> cpuInfo = new HashMap<>();
        
        try {
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            
            // 获取可用处理器数和平均负载
            int availableProcessors = osBean.getAvailableProcessors();
            double loadAverage = osBean.getSystemLoadAverage();
            
            // 尝试通过反射获取 processCpuLoad 和 systemCpuLoad
            double processCpuLoad = -1;
            double systemCpuLoad = -1;
            
            try {
                processCpuLoad = (Double) osBean.getClass().getMethod("getProcessCpuLoad").invoke(osBean);
            } catch (Exception e) {
                // 方法不存在，使用 -1 表示不可用
            }
            
            try {
                systemCpuLoad = (Double) osBean.getClass().getMethod("getSystemCpuLoad").invoke(osBean);
            } catch (Exception e) {
                // 方法不存在，使用 -1 表示不可用
            }
            
            cpuInfo.put("processCpuUsage", processCpuLoad < 0 ? "N/A" : String.format("%.2f%%", processCpuLoad * 100));
            cpuInfo.put("systemCpuUsage", systemCpuLoad < 0 ? "N/A" : String.format("%.2f%%", systemCpuLoad * 100));
            cpuInfo.put("availableProcessors", availableProcessors);
            cpuInfo.put("loadAverage", loadAverage < 0 ? "N/A" : String.format("%.2f", loadAverage));
            
        } catch (Exception e) {
            cpuInfo.put("error", "Failed to get CPU info: " + e.getMessage());
        }
        
        return cpuInfo;
    }

    /**
     * 格式化字节数为易读格式
     */
    private static String formatBytes(long bytes) {
        if (bytes <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
        return String.format("%.2f %s", bytes / Math.pow(1024, digitGroups), units[digitGroups]);
    }

    /**
     * 获取简化的系统信息（仅包含使用百分比）
     */
    public static Map<String, String> getSimpleSystemInfo() {
        Map<String, String> simpleInfo = new HashMap<>();
        
        try {
            // 磁盘使用率
            File root = new File("/");
            long totalSpace = root.getTotalSpace();
            long usedSpace = totalSpace - root.getUsableSpace();
            double diskUsagePercentage = (double) usedSpace / totalSpace * 100;
            simpleInfo.put("diskUsage", String.format("%.2f%%", diskUsagePercentage));
            
            // 内存使用率
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
            double memoryUsagePercentage = (double) heapUsage.getUsed() / heapUsage.getMax() * 100;
            simpleInfo.put("memoryUsage", String.format("%.2f%%", memoryUsagePercentage));
            
            // CPU使用率
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            double processCpuUsage = -1;
            try {
                processCpuUsage = (Double) osBean.getClass().getMethod("getProcessCpuLoad").invoke(osBean);
            } catch (Exception e) {
                // 方法不存在，使用 -1 表示不可用
            }
            String cpuUsage = processCpuUsage < 0 ? "N/A" : String.format("%.2f%%", processCpuUsage * 100);
            simpleInfo.put("cpuUsage", cpuUsage);
            
        } catch (Exception e) {
            simpleInfo.put("error", "Failed to get system info: " + e.getMessage());
        }
        
        return simpleInfo;
    }
}
