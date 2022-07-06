package com.atixlabs.semillasmiddleware.app.service;

import com.atixlabs.semillasmiddleware.app.bondarea.model.constants.LoanStateCodes;
import com.atixlabs.semillasmiddleware.app.exceptions.CredentialException;
import com.atixlabs.semillasmiddleware.app.repository.CredentialCreditRepository;
import com.atixlabs.semillasmiddleware.app.repository.CredentialRepository;
import com.atixlabs.semillasmiddleware.app.repository.RevocationReasonRepository;
import com.atixlabs.semillasmiddleware.app.didi.model.DidiAppUser;
import com.atixlabs.semillasmiddleware.app.didi.service.DidiService;
import com.atixlabs.semillasmiddleware.app.model.credential.CredentialCredit;
import com.atixlabs.semillasmiddleware.app.model.CredentialState.CredentialState;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class CredentialCreditService extends CredentialCommonService {

    private CredentialCreditRepository credentialCreditRepository;

    @Autowired
    public CredentialCreditService(CredentialStateService credentialStateService, DidiService didiService, RevocationReasonRepository revocationReasonRepository, CredentialRepository credentialRepository, CredentialCreditRepository credentialCreditRepository) {
        super(credentialStateService, didiService, revocationReasonRepository, credentialRepository);
        this.credentialCreditRepository = credentialCreditRepository;
    }

    public List<CredentialCredit> getCredentialCreditsOnPendindDidiState() throws CredentialException {
        CredentialState pendingDidiState = credentialStateService.getCredentialPendingDidiState();

        return  credentialCreditRepository.findByCredentialState(pendingDidiState);
    }

    public CredentialCredit save(CredentialCredit credentialCredit){
        return credentialCreditRepository.save(credentialCredit);
    }

    public List<CredentialCredit> getCredentialsCreditActiveForDni(Long dni) throws CredentialException {
        Optional<CredentialState> activeDidiState = credentialStateService.getCredentialActiveState();

        return credentialCreditRepository.findByCreditHolderDniAndCredentialState(dni, activeDidiState.orElse(new CredentialState()));
    }

    public CredentialCredit buildNewOnPendidgDidi(CredentialCredit credentialCredit, DidiAppUser newDidiAppUser) throws CredentialException {
        CredentialCredit newCredentialCredit =  new CredentialCredit(credentialCredit);
        newCredentialCredit.setIdDidiReceptor(newDidiAppUser.getDid());

        this.resetStateOnPendingDidi(newCredentialCredit);

        return newCredentialCredit;
    }

    public List<CredentialCredit> getCredentialCreditsForLoan(String idBondarea, LoanStateCodes code){
        return credentialCreditRepository.findByIdBondareaCreditAndCreditStateOrderByUpdatedDesc(idBondarea, code.getCode());
    }

    @Override
    protected Logger getLog() {
        return log;
    }
}
