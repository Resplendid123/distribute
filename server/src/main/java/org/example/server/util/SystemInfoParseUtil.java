package org.example.server.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;

/**
 * 系统信息解析工具类
 * 用于解析和格式化Agent上报的系统信息
 */
public class SystemInfoParseUtil {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 解析JSON格式的系统信息
     * @param infoJson 系统信息JSON字符串
     * @return 解析后的系统信息Map
     */
    public static Map<String, Object> parseSystemInfo(String infoJson) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            if (infoJson == null || infoJson.isEmpty()) {
                return result;
            }
            
            JsonNode root = objectMapper.readTree(infoJson);
            
            // 解析磁盘信息
            if (root.has("disk")) {
                result.put("disk", parseDiskInfo(root.get("disk")));
            }
            
            // 解析内存信息
            if (root.has("memory")) {
                result.put("memory", parseMemoryInfo(root.get("memory")));
            }
            
            // 解析CPU信息
            if (root.has("cpu")) {
                result.put("cpu", parseCpuInfo(root.get("cpu")));
            }
            
        } catch (Exception e) {
            result.put("error", "Failed to parse system info: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 解析磁盘信息
     */
    private static Map<String, String> parseDiskInfo(JsonNode diskNode) {
        Map<String, String> diskInfo = new HashMap<>();
        
        diskInfo.put("total", getNodeAsString(diskNode, "total"));
        diskInfo.put("used", getNodeAsString(diskNode, "used"));
        diskInfo.put("usable", getNodeAsString(diskNode, "usable"));
        diskInfo.put("usagePercentage", getNodeAsString(diskNode, "usagePercentage"));
        
        return diskInfo;
    }

    /**
     * 解析内存信息
     */
    private static Map<String, Object> parseMemoryInfo(JsonNode memoryNode) {
        Map<String, Object> memoryInfo = new HashMap<>();
        
        // 解析堆内存
        if (memoryNode.has("heap")) {
            Map<String, String> heapInfo = new HashMap<>();
            JsonNode heapNode = memoryNode.get("heap");
            heapInfo.put("used", getNodeAsString(heapNode, "used"));
            heapInfo.put("max", getNodeAsString(heapNode, "max"));
            heapInfo.put("usagePercentage", getNodeAsString(heapNode, "usagePercentage"));
            memoryInfo.put("heap", heapInfo);
        }
        
        // 解析非堆内存
        if (memoryNode.has("nonHeap")) {
            Map<String, String> nonHeapInfo = new HashMap<>();
            JsonNode nonHeapNode = memoryNode.get("nonHeap");
            nonHeapInfo.put("used", getNodeAsString(nonHeapNode, "used"));
            nonHeapInfo.put("committed", getNodeAsString(nonHeapNode, "committed"));
            memoryInfo.put("nonHeap", nonHeapInfo);
        }
        
        // 解析系统内存
        if (memoryNode.has("system")) {
            Map<String, String> systemInfo = new HashMap<>();
            JsonNode systemNode = memoryNode.get("system");
            systemInfo.put("total", getNodeAsString(systemNode, "total"));
            systemInfo.put("used", getNodeAsString(systemNode, "used"));
            systemInfo.put("usagePercentage", getNodeAsString(systemNode, "usagePercentage"));
            memoryInfo.put("system", systemInfo);
        }
        
        return memoryInfo;
    }

    /**
     * 解析CPU信息
     */
    private static Map<String, String> parseCpuInfo(JsonNode cpuNode) {
        Map<String, String> cpuInfo = new HashMap<>();
        
        cpuInfo.put("processCpuUsage", getNodeAsString(cpuNode, "processCpuUsage"));
        cpuInfo.put("systemCpuUsage", getNodeAsString(cpuNode, "systemCpuUsage"));
        cpuInfo.put("availableProcessors", getNodeAsString(cpuNode, "availableProcessors"));
        cpuInfo.put("loadAverage", getNodeAsString(cpuNode, "loadAverage"));
        
        return cpuInfo;
    }

    /**
     * 从JsonNode获取字符串值
     */
    private static String getNodeAsString(JsonNode node, String field) {
        if (node == null || !node.has(field)) {
            return "N/A";
        }
        JsonNode fieldNode = node.get(field);
        return fieldNode.isNull() ? "N/A" : fieldNode.asText();
    }

    /**
     * 获取磁盘使用率百分比（简化版）
     * @param infoJson 系统信息JSON字符串
     * @return 磁盘使用率百分比，格式为 "XX.XX%"
     */
    public static String getDiskUsagePercentage(String infoJson) {
        try {
            if (infoJson == null || infoJson.isEmpty()) {
                return "N/A";
            }
            
            JsonNode root = objectMapper.readTree(infoJson);
            if (root.has("disk") && root.get("disk").has("usagePercentage")) {
                return root.get("disk").get("usagePercentage").asText();
            }
        } catch (Exception e) {
            // 忽略异常
        }
        
        return "N/A";
    }

    /**
     * 获取内存使用率百分比（简化版）
     * @param infoJson 系统信息JSON字符串
     * @return 内存使用率百分比，格式为 "XX.XX%"
     */
    public static String getMemoryUsagePercentage(String infoJson) {
        try {
            if (infoJson == null || infoJson.isEmpty()) {
                return "N/A";
            }
            
            JsonNode root = objectMapper.readTree(infoJson);
            if (root.has("memory") && root.get("memory").has("heap") && 
                root.get("memory").get("heap").has("usagePercentage")) {
                return root.get("memory").get("heap").get("usagePercentage").asText();
            }
        } catch (Exception e) {
            // 忽略异常
        }
        
        return "N/A";
    }

    /**
     * 获取CPU使用率（简化版）
     * @param infoJson 系统信息JSON字符串
     * @return CPU使用率，格式为 "XX.XX%"
     */
    public static String getCpuUsagePercentage(String infoJson) {
        try {
            if (infoJson == null || infoJson.isEmpty()) {
                return "N/A";
            }
            
            JsonNode root = objectMapper.readTree(infoJson);
            if (root.has("cpu") && root.get("cpu").has("processCpuUsage")) {
                return root.get("cpu").get("processCpuUsage").asText();
            }
        } catch (Exception e) {
            // 忽略异常
        }
        
        return "N/A";
    }

    /**
     * 格式化系统信息为易读的字符串
     */
    public static String formatSystemInfo(String infoJson) {
        StringBuilder sb = new StringBuilder();
        
        try {
            if (infoJson == null || infoJson.isEmpty()) {
                return "No system info available";
            }
            
            JsonNode root = objectMapper.readTree(infoJson);
            
            // 磁盘信息
            if (root.has("disk")) {
                JsonNode diskNode = root.get("disk");
                sb.append("【磁盘信息】\n");
                sb.append("  总容量: ").append(getNodeAsString(diskNode, "total")).append("\n");
                sb.append("  已用: ").append(getNodeAsString(diskNode, "used")).append("\n");
                sb.append("  可用: ").append(getNodeAsString(diskNode, "usable")).append("\n");
                sb.append("  使用率: ").append(getNodeAsString(diskNode, "usagePercentage")).append("\n");
            }
            
            // 内存信息
            if (root.has("memory")) {
                JsonNode memoryNode = root.get("memory");
                sb.append("\n【内存信息】\n");
                
                if (memoryNode.has("heap")) {
                    JsonNode heapNode = memoryNode.get("heap");
                    sb.append("  堆内存 - 已用: ").append(getNodeAsString(heapNode, "used"))
                            .append(", 最大: ").append(getNodeAsString(heapNode, "max"))
                            .append(", 使用率: ").append(getNodeAsString(heapNode, "usagePercentage")).append("\n");
                }
                
                if (memoryNode.has("system")) {
                    JsonNode systemNode = memoryNode.get("system");
                    sb.append("  系统内存 - 总量: ").append(getNodeAsString(systemNode, "total"))
                            .append(", 已用: ").append(getNodeAsString(systemNode, "used"))
                            .append(", 使用率: ").append(getNodeAsString(systemNode, "usagePercentage")).append("\n");
                }
            }
            
            // CPU信息
            if (root.has("cpu")) {
                JsonNode cpuNode = root.get("cpu");
                sb.append("\n【CPU信息】\n");
                sb.append("  进程CPU使用率: ").append(getNodeAsString(cpuNode, "processCpuUsage")).append("\n");
                sb.append("  系统CPU使用率: ").append(getNodeAsString(cpuNode, "systemCpuUsage")).append("\n");
                sb.append("  可用处理器数: ").append(getNodeAsString(cpuNode, "availableProcessors")).append("\n");
                sb.append("  平均负载: ").append(getNodeAsString(cpuNode, "loadAverage")).append("\n");
            }
            
        } catch (Exception e) {
            sb.append("Failed to format system info: ").append(e.getMessage());
        }
        
        return sb.toString();
    }
}
