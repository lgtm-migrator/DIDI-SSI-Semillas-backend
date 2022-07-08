package com.atixlabs.semillasmiddleware.app.service;

import com.atixlabs.semillasmiddleware.app.exceptions.CredentialException;
import com.atixlabs.semillasmiddleware.app.repository.CredentialRepository;
import com.atixlabs.semillasmiddleware.app.repository.RevocationReasonRepository;
import com.atixlabs.semillasmiddleware.app.didi.service.DidiService;
import com.atixlabs.semillasmiddleware.app.model.credential.Credential;
import com.atixlabs.semillasmiddleware.app.model.CredentialState.CredentialState;
import com.atixlabs.semillasmiddleware.app.model.CredentialState.RevocationReason;
import com.atixlabs.semillasmiddleware.app.model.CredentialState.constants.RevocationReasonsCodes;
import com.atixlabs.semillasmiddleware.util.DateUtil;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public abstract class  CredentialCommonService {

    protected Logger log;

    protected CredentialStateService credentialStateService;

    protected DidiService didiService;

    protected RevocationReasonRepository revocationReasonRepository;

    protected CredentialRepository credentialRepository;

    protected CredentialCommonService(CredentialStateService credentialStateService, DidiService didiService, RevocationReasonRepository revocationReasonRepository, CredentialRepository credentialRepository){
        this.credentialStateService = credentialStateService;
        this.didiService = didiService;
        this.revocationReasonRepository = revocationReasonRepository;
        this.credentialRepository = credentialRepository;

    }

    protected abstract Logger getLog();


    public boolean revokeComplete(Credential credentialToRevok, RevocationReasonsCodes revocationReasonsCodes) throws CredentialException {
        return this.revokeComplete(credentialToRevok, revocationReasonsCodes.getCode());
    }

    /**
     * If exists and is emitted, revoke complete
     * If exists and is Pendiing Didi, revoke only local
     *
     * @param credentialToRevoke
     * @return boolean
     */
    public boolean revokeComplete(Credential credentialToRevoke, String reasonCode) throws CredentialException {
       getLog().info("Starting complete revoking process for credential id: {} | credential type: {} holder {} beneficiary {}"
                ,credentialToRevoke.getId(), credentialToRevoke.getCredentialDescription(),
               credentialToRevoke.getCreditHolderDni(), credentialToRevoke.getBeneficiaryDni());

    //revoke on didi if credential was emitted
        if (Boolean.TRUE.equals(credentialToRevoke.isEmitted())) {
            if (didiService.didiDeleteCertificate(credentialToRevoke.getIdDidiCredential(), reasonCode)) {
                // if didi fail the credential need to know that is needed to be revoked (here think in the best resolution).
                // if this revoke came from the revocation business we will need to throw an error to rollback any change done before.
                return this.revokeCredentialOnlyOnSemillas(credentialToRevoke, reasonCode);
            } else {
                String message = String.format("Error to delete Certificate %d on Didi ", credentialToRevoke.getId());
                this.getLog().error(message);
                throw new CredentialException(message);
            }
        } else
            return this.revokeCredentialOnlyOnSemillas(credentialToRevoke, reasonCode);

    }


    /**
     * Revoke only for internal usage. Only revokes the credential on the DB.
     *
     * @param credentialToRevoke
     * @return boolean
     */

    public boolean revokeCredentialOnlyOnSemillas(Credential credentialToRevoke, String reasonCode) throws CredentialException {
        this.getLog().info("Revoking the credential {} with reason {}", credentialToRevoke.getId(), reasonCode);
        boolean haveRevoke = true;

        Optional<RevocationReason> reason = revocationReasonRepository.findByReason(reasonCode);
        if (reason.isPresent()) {
            //validate if the credential is in db
            Optional<Credential> opCredential = this.getCredentialById(credentialToRevoke.getId());
            if (opCredential.isEmpty()) {
                haveRevoke = false;
                this.getLog().error("The credential with id: {} is not in the database", credentialToRevoke.getId());
            } else {

                Credential credential = opCredential.get();

                //revoke if the credential is not revoked yet
                if (Boolean.TRUE.equals(this.isCredentialRevoked(credential))) {
                    this.getLog().info("The credential {} has already been revoked", credential.getId());
                    haveRevoke = false;
                } else {
                    //revoke
                    Optional<CredentialState> opStateRevoke = credentialStateService.getCredentialRevokeState();
                    credentialToRevoke.setCredentialState(opStateRevoke.orElse(new CredentialState()));
                    credentialToRevoke.setRevocationReason(reason.get());
                    credentialToRevoke.setDateOfRevocation(DateUtil.getLocalDateTimeNow());
                    credentialRepository.save(credentialToRevoke);
                    this.getLog().info("Credential with id {} has been revoked!", credentialToRevoke.getId()); //then append also the reason
                }

            }

        } else {
            String message = String.format("Can't find reason with code: %s ", reasonCode);
            this.getLog().error(message);
            throw new CredentialException(message);
        }

        return haveRevoke;
    }

    public Boolean isCredentialRevoked(Credential credential) throws CredentialException {
        Optional<CredentialState> opStateRevoke = credentialStateService.getCredentialRevokeState();

        return (credential.getCredentialState().equals(opStateRevoke.orElse(new CredentialState())));
    }

    public Boolean isCredentialActive(Credential credential) throws CredentialException {
        Optional<CredentialState> opStateActive = credentialStateService.getCredentialActiveState();

        return (credential.getCredentialState().equals(opStateActive.orElse(new CredentialState())));
    }

    public Boolean isCredentialPendingDidi(Credential credential) throws CredentialException {
        CredentialState statePendingDidi = credentialStateService.getCredentialPendingDidiState();

        return (credential.getCredentialState().equals(statePendingDidi));
    }

    public Optional<Credential> getCredentialById(Long id) {
        //validate credential is in bd
        return credentialRepository.findById(id);
    }


    public List<CredentialState> getCredentialActivesStates() throws CredentialException {
        Optional<CredentialState> activeState = this.credentialStateService.getCredentialActiveState();
        CredentialState pendingDidieState = this.credentialStateService.getCredentialPendingDidiState();

        List<CredentialState> states = new ArrayList<>();

        states.add(activeState.orElse(new CredentialState()));
        states.add(pendingDidieState);

        return  states;
    }

  public Credential resetStateOnPendingDidi(Credential credential) throws CredentialException {
        credential.setDateOfRevocation(null);
        credential.setRevocationReason(null);
        credential.setCredentialState(this.credentialStateService.getCredentialPendingDidiState());
        credential.setDateOfIssue(DateUtil.getLocalDateTimeNow());

        return credential;

    }

    public List<CredentialState> getActiveAndPendingDidiStates() throws CredentialException {
        Optional<CredentialState> activeState = credentialStateService.getCredentialActiveState();
        CredentialState pendingDidiState = credentialStateService.getCredentialPendingDidiState();

        List<CredentialState> states = new ArrayList<>();
        states.add(activeState.orElse(new CredentialState()));
        states.add(pendingDidiState);

        return states;
    }

}
