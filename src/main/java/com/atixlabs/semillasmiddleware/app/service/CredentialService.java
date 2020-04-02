package com.atixlabs.semillasmiddleware.app.service;

import com.atixlabs.semillasmiddleware.app.dto.CredentialDto;
import com.atixlabs.semillasmiddleware.app.model.credential.CredentialCredit;
import com.atixlabs.semillasmiddleware.app.repository.CredentialCreditRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class CredentialService {

    @Autowired
    private CredentialCreditRepository credentialCreditRepository;

    public void addCredentialCredit(){
        CredentialCredit credentialCredit = new CredentialCredit();

        credentialCredit.setIdDidiIssueer(123L);
        credentialCredit.setIdDidiReceptor(234L);
        credentialCredit.setIdDidiCredential(456L);
        credentialCredit.setIdHistorical(77L);
        credentialCredit.setDateOfExpiry(LocalDateTime.now());
        credentialCredit.setIdRelatedCredential(534L);

        credentialCredit.setIdCredit(222L);
        credentialCredit.setCreditName("credit name");
        credentialCredit.setIdGroup(1111L);
        credentialCredit.setGroupName("GroupName");
        credentialCredit.setRol("rol");
        credentialCredit.setAmount(1d);
        credentialCredit.setCurrentCycle("Cycle");
        credentialCredit.setCreditState("state");
        credentialCredit.setDniBeneficiary(29302594L);

        credentialCreditRepository.save(credentialCredit);
    }

    public List<CredentialDto> findAllCredentialsMock(){
        List<CredentialDto> credentials = new ArrayList<>();


        CredentialDto credential1 = new CredentialDto(1L,2L,LocalDateTime.now(),LocalDateTime.now().plusDays(1),"Jorge Rodrigues",29302594L,"Estado");
        credentials.add(credential1);

        CredentialDto credential2 = new CredentialDto(2L,3L,LocalDateTime.now(),LocalDateTime.now().plusDays(1),"Uriel Brama",29302594L,"Estado");
        credentials.add(credential2);

        CredentialDto credential3 = new CredentialDto(3L,4L,LocalDateTime.now(),LocalDateTime.now().plusDays(2),"Pepe Grillo",293025464L,"Estado");
        credentials.add(credential3);

        CredentialDto credential4 = new CredentialDto(4L,5L,LocalDateTime.now(),LocalDateTime.now().plusDays(6),"Armando Thompson",29302594L,"Estado");
        credentials.add(credential4);

        return credentials;

    }
}
