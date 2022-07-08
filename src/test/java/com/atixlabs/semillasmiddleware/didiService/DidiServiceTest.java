package com.atixlabs.semillasmiddleware.didiService;


import com.atixlabs.semillasmiddleware.app.didi.constant.DidiSyncStatus;
import com.atixlabs.semillasmiddleware.app.didi.dto.DidiCreateCredentialResponse;
import com.atixlabs.semillasmiddleware.app.didi.dto.DidiCredential;
import com.atixlabs.semillasmiddleware.app.didi.model.DidiAppUser;
import com.atixlabs.semillasmiddleware.app.didi.repository.DidiAppUserRepository;
import com.atixlabs.semillasmiddleware.app.didi.service.DidiAppUserService;
import com.atixlabs.semillasmiddleware.app.didi.service.DidiService;
import com.atixlabs.semillasmiddleware.app.model.CredentialState.CredentialState;
import com.atixlabs.semillasmiddleware.app.model.beneficiary.Person;
import com.atixlabs.semillasmiddleware.app.model.credential.Credential;
import com.atixlabs.semillasmiddleware.app.model.credential.CredentialIdentity;
import com.atixlabs.semillasmiddleware.app.model.credential.constants.CredentialCategoriesCodes;
import com.atixlabs.semillasmiddleware.app.model.credential.constants.CredentialStatesCodes;
import com.atixlabs.semillasmiddleware.app.repository.CredentialRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.ArrayList;

//@RunWith(SpringRunner.class)
@SpringBootTest
@Slf4j
public class DidiServiceTest {

    @InjectMocks
    private DidiAppUserService didiAppUserService;
    @InjectMocks
    private DidiService didiService;

    @Mock
    private DidiAppUserRepository didiAppUserRepository;
    @Mock
    private CredentialRepository credentialRepository;

    //@Captor
    //private ArgumentCaptor<Loan> captor;



    public ArrayList<DidiAppUser> getAppUserMockOneMissing(){
        ArrayList<DidiAppUser> didiAppUsersMock = new ArrayList<>();
        didiAppUsersMock.add(new DidiAppUser(10000000L, "did:ethr:0x73c47226d044af432829b60d0de38d657b0643dc", DidiSyncStatus.SYNC_OK.getCode()));
        didiAppUsersMock.add(new DidiAppUser(20000000L, "did:ethr:0x73c47226d044af432829b60d0de38d657b0643dc", DidiSyncStatus.SYNC_OK.getCode()));
        didiAppUsersMock.add(new DidiAppUser(30000000L, "did:ethr:0x73c47226d044af432829b60d0de38d657b0643dc", DidiSyncStatus.SYNC_MISSING.getCode()));
        return didiAppUsersMock;
    }

    public ArrayList<Credential> getCredentialsCreditHolder(){
        CredentialIdentity credential = new CredentialIdentity();
        credential.setId(1L);
        credential.setIdDidiIssuer("texto-fijo-semillas");
        credential.setIdDidiReceptor("");
        credential.setIdDidiCredential("");
        //credential.setIdHistorical(1L);
        credential.setDateOfIssue(LocalDateTime.now());
        //this.dateOfRevocation = credential.dateOfRevocation;

        Person creditHolder = createCreditHolderMock();

        credential.setCreditHolder(creditHolder);
        credential.setCreditHolderDni(creditHolder.getDocumentNumber());
        credential.setCreditHolderFirstName(creditHolder.getFirstName());
        credential.setCreditHolderLastName(creditHolder.getLastName());

        credential.setBeneficiary(creditHolder);
        credential.setBeneficiaryDni(creditHolder.getDocumentNumber());
        credential.setBeneficiaryFirstName(creditHolder.getFirstName());
        credential.setBeneficiaryLastName(creditHolder.getLastName());

        credential.setCredentialState(createCredentialStateActiveMock());
        credential.setCredentialDescription(CredentialCategoriesCodes.IDENTITY.getCode());
        credential.setCredentialCategory(CredentialCategoriesCodes.IDENTITY.getCode());

        ArrayList<Credential> credentials = new ArrayList<>();
        credentials.add(credential);
        return credentials;
    }

    private Person createCreditHolderMock(){
        Person person = new Person();
        person.setId(1L);
        person.setFirstName("CreditHolder FirstName");
        person.setLastName("CreditHolder LastName");
        person.setDocumentNumber(30000000L);
        return person;
    }

    private CredentialState createCredentialStateActiveMock(){
        CredentialState credentialState = new CredentialState();
        credentialState.setId(1L);
        credentialState.setStateName(CredentialStatesCodes.CREDENTIAL_ACTIVE.getCode());
        return credentialState;
    }

    private DidiCreateCredentialResponse createDidiCredentialResponseOkMock(){
        DidiCreateCredentialResponse didiCreateCredentialResponse = new DidiCreateCredentialResponse();
        didiCreateCredentialResponse.setStatus("success");
        ArrayList<DidiCredential> didiCredentials = new ArrayList<>();
        DidiCredential didiCredential = new DidiCredential();
        didiCredential.setId("ID-VALIDO-DIDI");
        didiCredentials.add(didiCredential);
        didiCreateCredentialResponse.setData(didiCredentials);
        return didiCreateCredentialResponse;
    }

    @Test
    public void getUser(){
        Assert.assertNotNull(didiAppUserService.getDidiAppUserByDni(getAppUserMockOneMissing().get(1).getDni()));
    }

}
