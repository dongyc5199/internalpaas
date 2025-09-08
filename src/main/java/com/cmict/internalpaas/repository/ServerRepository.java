package com.cmict.internalpaas.repository;

import com.cmict.internalpaas.model.Server;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServerRepository extends JpaRepository<Server, Long> {
    List<Server> findByActiveTrue();
    List<Server> findByActiveTrueOrderByName();
    List<Server> findByConnectionStatusIn(List<Server.ConnectionStatus> connectionStatuses);
}