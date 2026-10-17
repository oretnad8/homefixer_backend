package com.homefixer.solicitudes.repository;

import com.homefixer.solicitudes.entity.MensajeChat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MensajeChatRepository extends JpaRepository<MensajeChat, Long> {
    List<MensajeChat> findBySolicitudIdOrderByCreadoEnAsc(Long solicitudId);
}
