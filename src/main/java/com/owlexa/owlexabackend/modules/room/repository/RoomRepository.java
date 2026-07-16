package com.owlexa.owlexabackend.modules.room.repository;

import com.owlexa.owlexabackend.modules.room.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Long> {

    List<Room> findAllByCenter_Id(Long centerId);

    List<Room> findAllByCenter_IdAndIsActiveTrue(Long centerId);

    Optional<Room> findByIdAndCenter_Id(Long id, Long centerId);

    boolean existsByCodeAndCenter_Id(String code, Long centerId);

    void deleteByCenter_Id(Long centerId);
}
