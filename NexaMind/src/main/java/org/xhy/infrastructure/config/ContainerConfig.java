package org.xhy.infrastructure.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** 容器管理配置 */
@Configuration
@EnableScheduling
@ConfigurationProperties(prefix = "nexamind.container")
public class ContainerConfig {

    private static final String WINDOWS_DOCKER_HOST = "npipe:////./pipe/dockerDesktopLinuxEngine";
    private static final String UNIX_DOCKER_HOST = "unix:///var/run/docker.sock";

    /** Docker连接配置 */
    @Value("${nexamind.container.docker-host:${NEXAMIND_CONTAINER_DOCKER_HOST:}}")
    private String dockerHost;

    /** 用户数据卷基础路径 */
    private String userVolumeBasePath = "/docker/users";

    /** 默认MCP网关镜像 */
    private String defaultMcpGatewayImage = "ghcr.nju.edu.cn/lucky-aeon/mcp-gateway:latest";

    /** 容器监控间隔（毫秒） */
    private long monitorInterval = 300000; // 5分钟

    /** 资源使用率更新间隔（毫秒） */
    private long statsUpdateInterval = 120000; // 2分钟

    @PostConstruct
    public void applyPlatformDockerHost() {
        if (dockerHost == null || dockerHost.isBlank()) {
            dockerHost = System.getProperty("os.name", "").toLowerCase().contains("win")
                    ? WINDOWS_DOCKER_HOST
                    : UNIX_DOCKER_HOST;
        }
    }

    public String getDockerHost() {
        return dockerHost;
    }

    public void setDockerHost(String dockerHost) {
        this.dockerHost = dockerHost;
    }

    public String getUserVolumeBasePath() {
        return userVolumeBasePath;
    }

    public void setUserVolumeBasePath(String userVolumeBasePath) {
        this.userVolumeBasePath = userVolumeBasePath;
    }

    public String getDefaultMcpGatewayImage() {
        return defaultMcpGatewayImage;
    }

    public void setDefaultMcpGatewayImage(String defaultMcpGatewayImage) {
        this.defaultMcpGatewayImage = defaultMcpGatewayImage;
    }

    public long getMonitorInterval() {
        return monitorInterval;
    }

    public void setMonitorInterval(long monitorInterval) {
        this.monitorInterval = monitorInterval;
    }

    public long getStatsUpdateInterval() {
        return statsUpdateInterval;
    }

    public void setStatsUpdateInterval(long statsUpdateInterval) {
        this.statsUpdateInterval = statsUpdateInterval;
    }
}
