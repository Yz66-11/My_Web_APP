package com.example.demo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserPetRepository extends JpaRepository<UserPet, Long> {

    Optional<UserPet> findByUserId(Long userId);

    boolean existsByUserId(Long userId);
}
