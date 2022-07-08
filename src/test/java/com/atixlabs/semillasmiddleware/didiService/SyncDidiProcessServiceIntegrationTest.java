package com.atixlabs.semillasmiddleware.didiService;

import com.atixlabs.semillasmiddleware.app.didi.constant.DidiSyncStatus;
import com.atixlabs.semillasmiddleware.app.didi.model.DidiAppUser;
import com.atixlabs.semillasmiddleware.app.didi.service.DidiAppUserService;
import com.atixlabs.semillasmiddleware.app.didi.service.DidiService;
import com.atixlabs.semillasmiddleware.app.didi.service.SyncDidiProcessService;
import com.atixlabs.semillasmiddleware.app.exceptions.CredentialException;
import com.atixlabs.semillasmiddleware.app.model.CredentialState.CredentialState;
import com.atixlabs.semillasmiddleware.app.model.credential.*;
import com.atixlabs.semillasmiddleware.app.model.credential.constants.CredentialCategoriesCodes;
import com.atixlabs.semillasmiddleware.app.model.credential.constants.CredentialStatesCodes;
import com.atixlabs.semillasmiddleware.app.model.credential.constants.PersonTypesCodes;
import com.atixlabs.semillasmiddleware.app.sancor.model.SancorPolicy;
import com.atixlabs.semillasmiddleware.app.sancor.service.SancorPolicyService;
import com.atixlabs.semillasmiddleware.app.service.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Optional;

import static org.mockito.Mockito.*;

//@RunWith(SpringRunner.class)
//@SpringBootTest
@Slf4j
public class SyncDidiProcessServiceIntegrationTest {

    @Mock
    private CredentialCreditService credentialCreditService;

    @Mock
    private CredentialBenefitService credentialBenefitService;

    @Mock
    private CredentialIdentityService credentialIdentityService;

    @Mock
    private CredentialDwellingService credentialDwellingService;

    @Mock
    private CredentialEntrepreneurshipService credentialEntrepreneurshipService;

    @Mock
    private DidiAppUserService didiAppUserService;

    @Mock
    private SancorPolicyService sancorPolicyService;

    @Mock
    private CredentialBenefitSancorService credentialBenefitSancorService;

    @Autowired
    @InjectMocks
    private SyncDidiProcessService syncDidiProcessService;

    @Mock
    private DidiService didiService;

    @Before
    public void setupMocks() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void whenEmmitCredentialCreditsAndCredentialCreditsToEmmitIsEmpty_thenDoNothing() throws CredentialException {

        when(credentialCreditService.getCredentialCreditsOnPendindDidiState()).thenReturn(new ArrayList<>());

        syncDidiProcessService.emmitCredentialsCredit();
        verify(credentialCreditService, times(1)).getCredentialCreditsOnPendindDidiState();
    }

    @Test
    public void whenHolderHaveDIDRegister_thenEmmitCredentialCredit(){

        CredentialCredit credentialCredit = this.getCredentialCreditMock();
        credentialCredit.setIdDidiReceptor(null);
        DidiAppUser didiAppUser = this.getDidiAppUserMock();

        when(didiAppUserService.getDidiAppUserByDni(credentialCredit.getCreditHolderDni())).thenReturn(Optional.of(didiAppUser));

        syncDidiProcessService.emmitCredentialCredit(credentialCredit);
        verify(didiAppUserService, times(1)).getDidiAppUserByDni(any());
    }

    @Test
    public void whenHolderHaveDIDRegisterCredentialBenefitPendingDidi_thenEmmitCredentialBenefit(){

        CredentialBenefits credentialBenefits = this.getCredentialBenefitsMock();
        credentialBenefits.setIdDidiReceptor(null);
        DidiAppUser didiAppUser = this.getDidiAppUserMock();

        when(didiAppUserService.getDidiAppUserByDni(credentialBenefits.getBeneficiaryDni())).thenReturn(Optional.of(didiAppUser));
        when(credentialBenefitService.save(credentialBenefits)).thenReturn(credentialBenefits);

        syncDidiProcessService.emmitCredentialBenefit(credentialBenefits, false);
        verify(didiAppUserService, times(1)).getDidiAppUserByDni(any());
    }

    @Test
    public void whenHolderHaveDIDRegisterCredentialIdentityPendingDidi_thenEmmitCredentialIdentity(){

        CredentialIdentity credentialIdentity = this.getCredentialIdentityMock();
        credentialIdentity.setIdDidiReceptor(null);

        DidiAppUser didiAppUser = this.getDidiAppUserMock();

        when(didiAppUserService.getDidiAppUserByDni(credentialIdentity.getBeneficiaryDni())).thenReturn(Optional.of(didiAppUser));
        when(credentialIdentityService.save(credentialIdentity)).thenReturn(credentialIdentity);

        syncDidiProcessService.emmitCredentialIdentity(credentialIdentity, false);

        verify(didiAppUserService, times(1)).getDidiAppUserByDni(any());
        verify(credentialIdentityService, times(1)).save(any());
        verify(didiService, times(1)).createAndEmmitCertificateDidi(any());
    }


    @Test
    public void whenHolderHaveDIDRegisterCredentialDwellingPendingDidi_thenEmmitCredentialDwelling(){

        CredentialDwelling credentialDwelling = this.getCredentialDwellingMock();
        credentialDwelling.setIdDidiReceptor(null);

        DidiAppUser didiAppUser = this.getDidiAppUserMock();

        when(didiAppUserService.getDidiAppUserByDni(credentialDwelling.getBeneficiaryDni())).thenReturn(Optional.of(didiAppUser));
        when(credentialDwellingService.save(credentialDwelling)).thenReturn(credentialDwelling);

        syncDidiProcessService.emmitCredentialDwelling(credentialDwelling);

        verify(didiAppUserService, times(1)).getDidiAppUserByDni(any());
    }

    @Test
    public void whenHolderHaveDIDRegisterAndSancorPolicyAndCredentialBnefitSancorPendindDidi_thenEmmitCredentialBenefitSancor(){

        CredentialBenefitSancor credentialBenefitSancor = this.getCredentialBenefitSancorMock();
        credentialBenefitSancor.setIdDidiReceptor(null);
        DidiAppUser didiAppUser = this.getDidiAppUserMock();
        SancorPolicy sancorPolicy = this.getSancorPolicyMock();

        when(didiAppUserService.getDidiAppUserByDni(credentialBenefitSancor.getBeneficiaryDni()))
                .thenReturn(Optional.of(didiAppUser));
        when(sancorPolicyService.findByCertificateClientDni(credentialBenefitSancor.getBeneficiaryDni()))
                .thenReturn(Optional.of(sancorPolicy));
        when(credentialBenefitSancorService.save(credentialBenefitSancor)).thenReturn(credentialBenefitSancor);

        syncDidiProcessService.emmitCredentialBenefitSancor(credentialBenefitSancor);
        verify(didiAppUserService, times(1)).getDidiAppUserByDni(any());
    }

    private CredentialBenefitSancor getCredentialBenefitSancorMock(){
        CredentialBenefitSancor credentialBenefitSancor = new CredentialBenefitSancor();
        credentialBenefitSancor.setCredentialState(this.getPendingDidiCredentialStateMock());
        credentialBenefitSancor.setBeneficiaryFirstName("Flor");
        credentialBenefitSancor.setBeneficiaryLastName("Tior");
        credentialBenefitSancor.setCreditHolderFirstName("Flor");
        credentialBenefitSancor.setCreditHolderLastName("Tiore");
        credentialBenefitSancor.setCreditHolderDni(36637842L);
        credentialBenefitSancor.setBeneficiaryDni(36637842L);
        credentialBenefitSancor.setIdDidiReceptor(null);
        credentialBenefitSancor.setId(1L);
        credentialBenefitSancor.setCreditHolder(null);
        credentialBenefitSancor.setCredentialCategory(CredentialCategoriesCodes.BENEFIT_SANCOR.getCode());
        credentialBenefitSancor.setCertificateNumber(1L);
        credentialBenefitSancor.setPolicyNumber(429273L);
        credentialBenefitSancor.setRef(429273L);


        return credentialBenefitSancor;
    }

    private CredentialCredit getCredentialCreditMock(){
        CredentialCredit credentialCredit = new CredentialCredit();
        credentialCredit.setCreditHolderDni(36637842L);
        credentialCredit.setIdDidiReceptor(null);
        credentialCredit.setId(1L);
        credentialCredit.setCreditHolder(null);
        credentialCredit.setCreditType("CONAMI 31");
        credentialCredit.setCurrentCycle("1");
        credentialCredit.setCreditStatus("Vigente");
        credentialCredit.setExpiredAmount(new BigDecimal(9L));
        credentialCredit.setIdBondareaCredit("987654");
        credentialCredit.setIdGroup("idGroup");
        credentialCredit.setTotalCycles(12);
        credentialCredit.setBeneficiaryDni(36637842L);
        credentialCredit.setBeneficiaryFirstName("Flor");
        credentialCredit.setBeneficiaryLastName("Tior");
        credentialCredit.setCreditHolderFirstName("Flor");
        credentialCredit.setCreditHolderLastName("Tiore");
        credentialCredit.setCredentialCategory(CredentialCategoriesCodes.CREDIT.getCode());

        return credentialCredit;
    }

    private SancorPolicy getSancorPolicyMock(){
        SancorPolicy sancorPolicy = new SancorPolicy();
        sancorPolicy.setCertificateClientDni(36637842L);
        sancorPolicy.setPolicyNumber(429273L);
        sancorPolicy.setCertificateNumber(1L);

        return sancorPolicy;
    }

    private CredentialBenefits getCredentialBenefitsMock(){
        CredentialBenefits credentialBenefits = new CredentialBenefits();
        credentialBenefits.setCredentialState(this.getPendingDidiCredentialStateMock());
        credentialBenefits.setBeneficiaryType(PersonTypesCodes.FAMILY.getCode());
        credentialBenefits.setBeneficiaryFirstName("Flor");
        credentialBenefits.setBeneficiaryLastName("Tior");
        credentialBenefits.setCreditHolderFirstName("Flor");
        credentialBenefits.setCreditHolderLastName("Tiore");
        credentialBenefits.setCreditHolderDni(36637842L);
        credentialBenefits.setBeneficiaryDni(36637842L);
        credentialBenefits.setIdDidiReceptor(null);
        credentialBenefits.setId(1L);
        credentialBenefits.setCreditHolder(null);
        credentialBenefits.setCredentialCategory(CredentialCategoriesCodes.BENEFIT.getCode());

        return credentialBenefits;
    }

    private CredentialIdentity getCredentialIdentityMock(){
        CredentialIdentity credentialIdentity = new CredentialIdentity();
        credentialIdentity.setCredentialState(this.getPendingDidiCredentialStateMock());
        credentialIdentity.setBeneficiaryFirstName("Flor");
        credentialIdentity.setBeneficiaryLastName("Tior");
        credentialIdentity.setCreditHolderFirstName("Flor");
        credentialIdentity.setCreditHolderLastName("Tiore");
        credentialIdentity.setCreditHolderDni(36637842L);
        credentialIdentity.setBeneficiaryDni(36637842L);
        credentialIdentity.setIdDidiReceptor(null);
        credentialIdentity.setId(1L);
        credentialIdentity.setCredentialCategory(CredentialCategoriesCodes.IDENTITY.getCode());
        credentialIdentity.setRelationWithCreditHolder("familiar");
        credentialIdentity.setBeneficiaryGender("Masculino");
        credentialIdentity.setBeneficiaryBirthDate(LocalDate.of(1990,12,12));

        return credentialIdentity;
    }

    private CredentialDwelling getCredentialDwellingMock(){
        CredentialDwelling credentialDwelling = new CredentialDwelling();
        credentialDwelling.setCredentialState(this.getPendingDidiCredentialStateMock());
        credentialDwelling.setBeneficiaryFirstName("Flor");
        credentialDwelling.setBeneficiaryLastName("Tior");
        credentialDwelling.setCreditHolderFirstName("Flor");
        credentialDwelling.setCreditHolderLastName("Tiore");
        credentialDwelling.setCreditHolderDni(36637842L);
        credentialDwelling.setBeneficiaryDni(36637842L);
        credentialDwelling.setIdDidiReceptor(null);
        credentialDwelling.setId(1L);
        credentialDwelling.setCreditHolder(null);
        credentialDwelling.setCredentialCategory(CredentialCategoriesCodes.DWELLING.getCode());
        credentialDwelling.setDwellingType("casa");
        credentialDwelling.setDwellingAddress("Direccion 123");
        credentialDwelling.setPossessionType("Dueño");

        return credentialDwelling;
    }

    private CredentialState getPendingDidiCredentialStateMock(){
        return new CredentialState(CredentialStatesCodes.PENDING_DIDI.getCode());
    }

    private DidiAppUser getDidiAppUserMock(){
        DidiAppUser didiAppUser = new DidiAppUser();
        didiAppUser.setDni(36637842L);
        didiAppUser.setDid("did:ethr:0x45b5bf83cc010c18739110d8d3397f1fa8a4d20a");
        didiAppUser.setSyncStatus(DidiSyncStatus.SYNC_OK.getCode());

        return didiAppUser;
    }
}
