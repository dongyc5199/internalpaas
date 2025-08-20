package com.cmict.internalpaas.service;

import com.cmict.internalpaas.model.User;
import com.cmict.internalpaas.repository.ApplicationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
public class PortManagerService {

    @Value("${app.port.range.start:8000}")
    private int portRangeStart;
    
    @Value("${app.port.range.end:9000}")
    private int portRangeEnd;
    
    @Value("${app.debug.port.range.start:5000}")
    private int debugPortRangeStart;
    
    @Value("${app.debug.port.range.end:5999}")
    private int debugPortRangeEnd;
    
    @Autowired
    private ApplicationRepository applicationRepository;
    
    /**
     * 为用户分配可用端口
     */
    public PortAllocation allocatePorts(User user) {
        int appPort = findAvailablePort(user, portRangeStart, portRangeEnd, false);
        int debugPort = findAvailablePort(user, debugPortRangeStart, debugPortRangeEnd, true);
        
        return new PortAllocation(appPort, debugPort);
    }
    
    private int findAvailablePort(User user, int start, int end, boolean isDebug) {
        Set<Integer> usedPorts = new HashSet<>();
        
        if (isDebug) {
            applicationRepository.findByUser(user).forEach(app -> {
                if (app.getDebugPort() != null) {
                    usedPorts.add(app.getDebugPort());
                }
            });
        } else {
            applicationRepository.findByUser(user).forEach(app -> {
                if (app.getPort() != null) {
                    usedPorts.add(app.getPort());
                }
            });
        }
        
        for (int port = start; port <= end; port++) {
            if (!usedPorts.contains(port)) {
                return port;
            }
        }
        
        throw new RuntimeException("No available ports in range " + start + "-" + end);
    }
    
    public static class PortAllocation {
        private final int applicationPort;
        private final int debugPort;
        
        public PortAllocation(int applicationPort, int debugPort) {
            this.applicationPort = applicationPort;
            this.debugPort = debugPort;
        }
        
        public int getApplicationPort() { return applicationPort; }
        public int getDebugPort() { return debugPort; }
    }
}