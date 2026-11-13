
package com.manan.cloudfilemanager.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.manan.cloudfilemanager.entity.AppUser;
import com.manan.cloudfilemanager.entity.Role;

@Repository
public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);
    
    long countByRole(Role role);
}
