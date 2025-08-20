package com.cmict.internalpaas.service;

import com.cmict.internalpaas.model.Server;
import com.cmict.internalpaas.repository.ServerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ServerService {
    
    @Autowired
    private ServerRepository serverRepository;
    
    public List<Server> getAllServers() {
        return serverRepository.findAll();
    }
    
    public List<Server> getActiveServers() {
        return serverRepository.findByActiveTrueOrderByName();
    }
    
    public Optional<Server> getServerById(Long id) {
        return serverRepository.findById(id);
    }
    
    public Server saveServer(Server server) {
        return serverRepository.save(server);
    }
    
    public void deleteServer(Long id) {
        serverRepository.deleteById(id);
    }
    
    public Server updateServer(Long id, Server serverDetails) {
        Server server = serverRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Server not found"));
        
        server.setName(serverDetails.getName());
        server.setHostname(serverDetails.getHostname());
        server.setPort(serverDetails.getPort());
        server.setDescription(serverDetails.getDescription());
        server.setBaseWorkDirectory(serverDetails.getBaseWorkDirectory());
        server.setActive(serverDetails.getActive());
        
        return serverRepository.save(server);
    }
    
    public String getDefaultBaseWorkDirectory() {
        List<Server> activeServers = getActiveServers();
        if (activeServers.isEmpty()) {
            return "./workspaces";
        }
        return activeServers.get(0).getBaseWorkDirectory();
    }
}