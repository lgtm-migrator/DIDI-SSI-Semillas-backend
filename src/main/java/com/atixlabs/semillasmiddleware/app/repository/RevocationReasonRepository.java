package com.atixlabs.semillasmiddleware.app.repository;

import com.atixlabs.semillasmiddleware.app.model.CredentialState.RevocationReason;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RevocationReasonRepository extends JpaRepository<RevocationReason, Long>{

    Optional<RevocationReason> findByReason(String reason);
}
