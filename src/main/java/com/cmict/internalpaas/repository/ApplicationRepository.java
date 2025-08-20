package com.cmict.internalpaas.repository;

import com.cmict.internalpaas.model.Application;
import com.cmict.internalpaas.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {
    List<Application> findByUserOrderByCreatedAtDesc(User user);
    
    List<Application> findByUserAndStatus(User user, String status);
    
    List<Application> findByUser(User user);
    
    boolean existsByUserAndPort(User user, Integer port);
    
    boolean existsByUserAndDebugPort(User user, Integer debugPort);
}