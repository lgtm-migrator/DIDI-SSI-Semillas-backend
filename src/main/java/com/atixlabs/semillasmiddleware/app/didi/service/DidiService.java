package com.atixlabs.semillasmiddleware.app.didi.service;

import com.atixlabs.semillasmiddleware.app.didi.constant.DidiSyncStatus;
import com.atixlabs.semillasmiddleware.app.didi.dto.*;
import com.atixlabs.semillasmiddleware.app.didi.model.DidiAppUser;
import com.atixlabs.semillasmiddleware.app.didi.repository.DidiAppUserRepository;
import com.atixlabs.semillasmiddleware.app.model.credential.*;
import com.atixlabs.semillasmiddleware.app.model.credential.constants.CredentialCategoriesCodes;
import com.atixlabs.semillasmiddleware.app.model.credential.constants.CredentialStatesCodes;
import com.atixlabs.semillasmiddleware.app.model.credentialState.CredentialState;
import com.atixlabs.semillasmiddleware.app.model.credentialState.constants.RevocationReasonsCodes;
import com.atixlabs.semillasmiddleware.app.repository.*;
import com.atixlabs.semillasmiddleware.app.service.CredentialService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import retrofit2.Call;
//import retrofit2.GsonConverterFactory;
import retrofit2.Response;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DidiService {

    private DidiEndpoint endpointInterface = null;
    private String didiAuthToken = null;


    private DidiAppUserRepository didiAppUserRepository;
    private DidiAppUserService didiAppUserService;

    private CredentialService credentialService;
    private CredentialRepository credentialRepository;
    private CredentialStateRepository credentialStateRepository;
    private CredentialIdentityRepository credentialIdentityRepository;
    private CredentialEntrepreneurshipRepository credentialEntrepreneurshipRepository;
    private CredentialDwellingRepository credentialDwellingRepository;
    private CredentialBenefitsRepository credentialBenefitsRepository;
    private CredentialCreditRepository credentialCreditRepository;

    private String didiBaseUrl;
    private String didiUsername;
    private String didiPassword;
    private String didiTemplateCodeIdentity;
    private String didiTemplateCodeEntrepreneurship;
    private String didiTemplateCodeDwelling;
    private String didiTemplateCodeBenefit;
    private String didiTemplateCodeCredit;
/*
    @Value("${didi.base_url}")
    private String didiBaseUrl;
    @Value("${didi.username}")
    private String didiUsername;
    @Value("${didi.password}")
    private String didiPassword;
    @Value("${didi.template_code_identity}")
    private String didiTemplateCodeIdentity;
    @Value("${didi.template_code_entrepreneurship}")
    private String didiTemplateCodeEntrepreneurship;
    @Value("${didi.template_code_dwelling}")
    private String didiTemplateCodeDwelling;
    @Value("${didi.template_code_benefit}")
    private String didiTemplateCodeBenefit;
*/
    @Autowired
    public DidiService(
            DidiAppUserService didiAppUserService,
            DidiAppUserRepository didiAppUserRepository,

            @Lazy CredentialService credentialService,
            CredentialRepository credentialRepository,
            CredentialStateRepository credentialStateRepository,

            CredentialIdentityRepository credentialIdentityRepository,
            CredentialEntrepreneurshipRepository credentialEntrepreneurshipRepository,
            CredentialDwellingRepository credentialDwellingRepository,
            CredentialBenefitsRepository credentialBenefitsRepository,
            CredentialCreditRepository credentialCreditRepository,
            @Value("${didi.base_url}") String didiBaseUrl,
            @Value("${didi.username}") String didiUsername,
            @Value("${didi.password}") String didiPassword,
            @Value("${didi.template_code_identity}") String didiTemplateCodeIdentity,
            @Value("${didi.template_code_entrepreneurship}") String didiTemplateCodeEntrepreneurship,
            @Value("${didi.template_code_dwelling}") String didiTemplateCodeDwelling,
            @Value("${didi.template_code_benefit}") String didiTemplateCodeBenefit,
            @Value("${didi.template_code_credit}") String didiTemplateCodeCredit) {

        this.didiAppUserService = didiAppUserService;
        this.didiAppUserRepository = didiAppUserRepository;

        this.credentialService = credentialService;
        this.credentialRepository = credentialRepository;
        this.credentialStateRepository = credentialStateRepository;

        this.credentialIdentityRepository = credentialIdentityRepository;
        this.credentialEntrepreneurshipRepository = credentialEntrepreneurshipRepository;
        this.credentialDwellingRepository = credentialDwellingRepository;
        this.credentialBenefitsRepository = credentialBenefitsRepository;
        this.credentialCreditRepository = credentialCreditRepository;
        this.didiBaseUrl = didiBaseUrl;
        this.didiUsername = didiUsername;
        this.didiPassword = didiPassword;
        this.didiTemplateCodeIdentity = didiTemplateCodeIdentity;
        this.didiTemplateCodeEntrepreneurship = didiTemplateCodeEntrepreneurship;
        this.didiTemplateCodeDwelling = didiTemplateCodeDwelling;
        this.didiTemplateCodeBenefit = didiTemplateCodeBenefit;
        this.didiTemplateCodeCredit = didiTemplateCodeCredit;

        this.setUpRetrofitForDidiAndGetToken();
    }

    private void setUpRetrofitForDidiAndGetToken() {
        if (this.endpointInterface == null)
            this.endpointInterface = (DidiEndpoint) endpointInterfaceBuilder(DidiEndpoint.class);
        if (didiAuthToken == null)
            this.didiAuthToken = getAuthToken();
    }

    private Object endpointInterfaceBuilder(Class<?> classToCreateEndpoint) {
        log.info("endpointInterfaceBuilder - setting up retrofit configuration:");

        //contains json converter and date format configuration
        Gson gson = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
                .create();

        OkHttpClient okHttpClient = new OkHttpClient().newBuilder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();

        //ScalarsConverterFactory - allows String response for debug purposes.
        //GsonConverterFactory - decodes response into final target object
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(didiBaseUrl)
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(okHttpClient)
                .build();

        return retrofit.create(classToCreateEndpoint);
    }


    public String getAuthToken() {
        log.info("getAuthToken:");
        DidiAuthRequestBody didiAuthRequestBody = new DidiAuthRequestBody(didiUsername, didiPassword);
        Call<DidiAuthResponse> callSync = endpointInterface.getAuthToken(didiAuthRequestBody);

        try {
            Response<DidiAuthResponse> response = callSync.execute();
            DidiAuthResponse didiAuthResponse = response.body();

            if (didiAuthResponse != null)
                return didiAuthResponse.getData().getToken();
        } catch (Exception ex) {
            log.error("getAuthTokenSync: Didi Authorization error", ex);
        }
        return null;
    }

    public String didiCredentialSync() {
        log.info("didiSync: started");

        //1-Busco registros en AppUser con estado SYNC_MISSING O SYNC_ERROR (todo: validar si SYNC_ERROR vuelvo a intentar):
        ArrayList<String> didiSyncStatus = new ArrayList<>();
        didiSyncStatus.add(DidiSyncStatus.SYNC_MISSING.getCode());
        didiSyncStatus.add(DidiSyncStatus.SYNC_ERROR.getCode());
        ArrayList<DidiAppUser> didiAppUsers = didiAppUserRepository.findBySyncStatusIn(didiSyncStatus);

        if (didiAppUsers.size() <= 0) {
            log.info("didiSync: No existen pedidos de didi-app pendientes para enviar hacia didi");
            return "didiSync: No existen pedidos de didi-app pendientes para enviar hacia didi";
        }

        List<Long> dniList;
        dniList = didiAppUsers.stream().map(DidiAppUser::getDni).collect(Collectors.toList());

        //2-Busco credenciales que tengan el DNI como creditHolder - indica si es titular.
        ArrayList<Credential> creditHolders = credentialRepository.findByCreditHolderDniIn(dniList);
        //3-Busco credenciales que tengan el DNI como beneficiary
        ArrayList<Credential> beneficiaries = credentialRepository.findByBeneficiaryDniIn(dniList);
        //todo ver pendientes -> borrar credenciales de los listados anteriores

        if (creditHolders.size()<=0 && beneficiaries.size()<=0) {
            log.info("didiSync: No existen credenciales pendientes para enviar hacia didi");
            return "didiSync: No existen credenciales pendientes para enviar hacia didi";
        }

        //3-Trabajo sobre cada credencial de beneficiario
        //  es beneficiario de algun credito - debo emitir solamente su credencial.
        //  Si getCreditHolderDni() != credential.getBeneficiaryDni() es beneficiario
        //  IMPORTANT: cuando creditHolder = beneficiary ya se cubrirá en siguiente for
        for (Credential credential : beneficiaries) {
            log.info("BENEFICIARIES");
            log.info(credential.toString());
            if (!credential.getCreditHolderDni().equals(credential.getBeneficiaryDni())) {
                String receivedDid = didiAppUserRepository.findByDni(credential.getBeneficiaryDni()).getDid();
                updateCredentialDidAndDidiSync(credential, receivedDid);
            }
        }

        //4-Creo y emito credenciales de titulares
        for (Credential credential : creditHolders) {
            log.info("CREDIT HOLDERS");
            log.info(credential.toString());
            String receivedDid = didiAppUserRepository.findByDni(credential.getCreditHolderDni()).getDid();
            this.updateCredentialDidAndDidiSync(credential, receivedDid);
        }

        log.info("didiSync: ended");
        return "didiSync: ended";
    }

    private void updateCredentialDidAndDidiSync(Credential credential, String receivedDid){
        log.info("didiSync: credencial para evaluar:");
        log.info(credential.toString());
        switch (CredentialStatesCodes.getEnumByStringValue(credential.getCredentialState().getStateName())){
            case CREDENTIAL_ACTIVE:
                if (credential.getIdDidiCredential() != null) {
                    log.info("didiSync: 1.a Revocar credencial activa en didi");

                    if (credentialService.revokeComplete(credential, RevocationReasonsCodes.UPDATE_INTERNAL.getCode())) {
                        credential.setIdDidiReceptor(receivedDid);//registro el did recibido
                        createAndEmmitCertificateDidi(credential);
                    }
                }
            break;

            case PENDING_DIDI:
                boolean haveRevokeOk = true;
                if (credential.isEmitted()) {
                    log.info("didiSync: 1.b no-break continuo revocacion en semillas");
                    //revoke on didi too. Because it has idDididCredential.
                    haveRevokeOk = credentialService.revokeComplete(credential, RevocationReasonsCodes.UPDATE_INTERNAL.getCode());
                }
                else {
                    haveRevokeOk = credentialService.revokeCredentialOnlyOnSemillas(credential, RevocationReasonsCodes.UPDATE_INTERNAL.getCode());
                }

                if(haveRevokeOk) {
                    log.info("didiSync: 2  doy de alta credenciales nuevas");
                    //String beneficiaryReceivedDid = didiAppUserRepository.findByDni(appUserDni).getDid();
                    credential.setIdDidiReceptor(receivedDid);//registro el did recibido
                    createAndEmmitCertificateDidi(credential);
                }

                break;
            case CREDENTIAL_REVOKE:
              //TODO: Definir accionar con credenciales revocadas, por ahora las ignora");
                break;
        }
    }



    private void createAndEmmitCertificateDidi(Credential credential) {

        DidiCreateCredentialResponse didiCreateCredentialResponse = this.createCertificateDidi(credential);

        if (didiCreateCredentialResponse != null && didiCreateCredentialResponse.getStatus().equals("success")) {

            log.info("didiSync: certificateId to emmit: "+didiCreateCredentialResponse.getData().get(0).get_id());
            DidiEmmitCredentialResponse didiEmmitCredentialResponse = emmitCertificateDidi(didiCreateCredentialResponse.getData().get(0).get_id());

            if (didiEmmitCredentialResponse!=null)
                log.info("didiSync: emmitCertificate Response: "+didiEmmitCredentialResponse.toString());

            if (didiEmmitCredentialResponse!=null && didiEmmitCredentialResponse.getStatus().equals("success")){
                log.info("didiSync: La credencial fue emitida, persistiendo datos en bd");
                this.saveEmittedCredential(didiEmmitCredentialResponse, credential);
                this.didiAppUserService.updateAppUserStatusByCode(credential.getCreditHolderDni(), DidiSyncStatus.SYNC_OK.getCode());
            }
            else {
                log.error("didiSync: Fallo la emision de la certificado, borrando el certificado creado pero no-emitido del didi-issuer");
                this.didiDeleteCertificate(didiCreateCredentialResponse.getData().get(0).get_id());
                this.didiAppUserService.updateAppUserStatusByCode(credential.getCreditHolderDni(), DidiSyncStatus.SYNC_ERROR.getCode());
                this.saveCredentialOnPending(credential);
            }
        } else {
            log.error("didiSync: fallo la creacion de la certificado");
            this.didiAppUserService.updateAppUserStatusByCode(credential.getCreditHolderDni(), DidiSyncStatus.SYNC_ERROR.getCode());
            this.saveCredentialOnPending(credential);
        }
    }

    //todo must be use a transaction to rollback
    private void saveCredentialOnPending(Credential credential){
        this.setCredentialState(CredentialStatesCodes.PENDING_DIDI.getCode(), credential);
        credential.setDateOfRevocation(null);
        credentialRepository.save(credential);
    }



    private DidiCreateCredentialResponse createCertificateDidi(Credential credential) {
        log.info("didiSync: createCertificateDidi");

        String didiTemplateCode = "";
        switch (CredentialCategoriesCodes.getEnumByStringValue(credential.getCredentialCategory())) {
            case IDENTITY:
                didiTemplateCode = didiTemplateCodeIdentity;
                break;
            case ENTREPRENEURSHIP:
                didiTemplateCode = didiTemplateCodeEntrepreneurship;
                break;
            case DWELLING:
                didiTemplateCode = didiTemplateCodeDwelling;
                break;
            case BENEFIT:
                didiTemplateCode = didiTemplateCodeBenefit;
                break;
            case CREDIT:
                didiTemplateCode = didiTemplateCodeCredit;
                break;
            default:
                log.error("didiSync: La categoria de credencial no es valida");
                return null;
        }


        DidiCredentialData didiCredentialData = new DidiCredentialData(credential);
        return createCertificateDidiCall(didiTemplateCode, didiCredentialData);
    }

    public DidiCreateCredentialResponse createCertificateDidiCall(String didiTemplateCode, DidiCredentialData didiCredentialData) {
        log.info("didiSync: createCertificateDidiCall");

        Call<DidiCreateCredentialResponse> callSync = endpointInterface.createCertificate(didiAuthToken,didiTemplateCode,true,didiCredentialData);

        log.info(didiCredentialData.toString());
        try {
            Response<DidiCreateCredentialResponse> response = callSync.execute();
            log.info("didiSync: createCertificateDidiCall - response:");
            if (response.body() != null)
                log.info(response.body().toString());
            return response.body();
        } catch (Exception ex) {
            log.error("didiSync: createCertificateDidiCall: Request error", ex);
        }
        return null;
    }


    private DidiEmmitCredentialResponse emmitCertificateDidi(String didiCredentialId) {

        Call<DidiEmmitCredentialResponse> callSync = endpointInterface.emmitCertificate(didiAuthToken, didiCredentialId);

        try {
            Response<DidiEmmitCredentialResponse> response = callSync.execute();
            log.info("didiSync: emmitCertificate - response:");
            if (response.body() != null)
                log.info(response.body().toString());
            return response.body();
        } catch (Exception ex) {
            log.error("didiSync: emmitCertificateDidi: Request error", ex);
        }

        return null;
    }

    public boolean didiDeleteCertificate(String CredentialToRevokeDidiId) {
        log.info("Revoking certificate on didi");
        Call<DidiEmmitCredentialResponse> callSync = endpointInterface.deleteCertificate(didiAuthToken, CredentialToRevokeDidiId);

        try {
            Response<DidiEmmitCredentialResponse> response = callSync.execute();
            log.info("didiSync: deleteCertificate - response:");
            if (response.body() != null)
                log.info(response.body().toString());
            //return response.body();
            return true;
        } catch (Exception ex) {
            log.error("didiSync: emmitCertificateDidi: Request error", ex);
        }
        return false;
    }

    private void saveEmittedCredential(DidiEmmitCredentialResponse didiEmmitCredentialResponse, Credential pendingCredential) {
        log.info("didiSync: saveEmittedCredential:");

        if (didiEmmitCredentialResponse != null && didiEmmitCredentialResponse.getStatus().equals("success")) {
            String credentialDidiId = didiEmmitCredentialResponse.getData().get_id();

            //todo: ACA ESTOY HACIENDO UN UPDATE QUE SOLO ESTA BIEN SI ES PRE-CREDENCIAL
            if (pendingCredential.getCredentialState().getStateName().equals(CredentialStatesCodes.PENDING_DIDI.getCode())) {
                //actualizo cuando es una pre-credencial en estado PENDING_DIDI sino doy de alta una nueva.
                pendingCredential.setIdDidiCredential(credentialDidiId);
                setCredentialState(CredentialStatesCodes.CREDENTIAL_ACTIVE.getCode(), pendingCredential);
                credentialRepository.save(pendingCredential);
            } else {
                //es una credencial con estado activo o revocado, debo crear una nueva.
                //todo the cases must use the different builds that exist for credentials.
                switch (CredentialCategoriesCodes.getEnumByStringValue(pendingCredential.getCredentialCategory())) {
                    case IDENTITY:
                        Optional<CredentialIdentity> credentialIdentityOp = credentialIdentityRepository.findById(pendingCredential.getId());
                        CredentialIdentity credentialIdentity = new CredentialIdentity(credentialIdentityOp.get());
                        credentialIdentity.setIdDidiCredential(credentialDidiId);
                        credentialIdentity.setDateOfRevocation(null);
                        setCredentialState(CredentialStatesCodes.CREDENTIAL_ACTIVE.getCode(), credentialIdentity);
                        credentialIdentityRepository.save(credentialIdentity);
                        break;
                    case DWELLING:
                        Optional<CredentialDwelling> credentialDwellingOp = credentialDwellingRepository.findById(pendingCredential.getId());
                        CredentialDwelling credentialDwelling = new CredentialDwelling(credentialDwellingOp.get());
                        credentialDwelling.setIdDidiCredential(credentialDidiId);
                        credentialDwelling.setDateOfRevocation(null);
                        setCredentialState(CredentialStatesCodes.CREDENTIAL_ACTIVE.getCode(), credentialDwelling);
                        credentialDwellingRepository.save(credentialDwelling);
                        break;
                    case ENTREPRENEURSHIP:
                        Optional<CredentialEntrepreneurship> credentialEntrepreneurshipOp = credentialEntrepreneurshipRepository.findById(pendingCredential.getId());
                        CredentialEntrepreneurship credentialEntrepreneurship = new CredentialEntrepreneurship(credentialEntrepreneurshipOp.get());
                        credentialEntrepreneurship.setIdDidiCredential(credentialDidiId);
                        credentialEntrepreneurship.setDateOfRevocation(null);
                        setCredentialState(CredentialStatesCodes.CREDENTIAL_ACTIVE.getCode(), credentialEntrepreneurship);
                        credentialEntrepreneurshipRepository.save(credentialEntrepreneurship);
                        break;
                    case BENEFIT:
                        Optional<CredentialBenefits> credentialBenefitsOp = credentialBenefitsRepository.findById(pendingCredential.getId());
                        CredentialBenefits credentialBenefits = new CredentialBenefits(credentialBenefitsOp.get());
                        credentialBenefits.setIdDidiCredential(credentialDidiId);
                        credentialBenefits.setDateOfRevocation(null);
                        setCredentialState(CredentialStatesCodes.CREDENTIAL_ACTIVE.getCode(), credentialBenefits);
                        credentialBenefitsRepository.save(credentialBenefits);
                        break;
                    case CREDIT:
                        Optional<CredentialCredit> credentialCreditOp = credentialCreditRepository.findById(pendingCredential.getId());
                        CredentialCredit credentialCredit = new CredentialCredit(credentialCreditOp.get());
                        credentialCredit.setIdDidiCredential(credentialDidiId);
                        credentialCredit.setDateOfRevocation(null);
                        setCredentialState(CredentialStatesCodes.CREDENTIAL_ACTIVE.getCode(), credentialCredit);
                        credentialCreditRepository.save(credentialCredit);
                        break;
                    default:
                        log.error("didiSync: El tipo de credencial indicado no existe");
                }
            }

            log.info("didiSync: La credencial fue actualizada con exito, se obtuvo el id de didi: " + credentialDidiId);
        } else
            log.error("didiSync: Ocurrio un error al intentar crear la credencial en didi");
    }

    private void setCredentialState(String credentialStateString, Credential credential) {
        Optional<CredentialState> credentialState = credentialStateRepository.findByStateName(credentialStateString);
        credentialState.ifPresent(credential::setCredentialState);
    }

    public DidiGetAllCredentialResponse didiGetAllCredentials(){

        Call<DidiGetAllCredentialResponse> callSync = endpointInterface.getAllCertificates(didiAuthToken);

        try {
            Response<DidiGetAllCredentialResponse> response = callSync.execute();
            log.info("didiGetAllCredentials: response:");
            if (response.body() != null)
                log.info(response.body().toString());
            return response.body();
        } catch (Exception ex) {
            log.error("getAllCredentials: Didi Request error", ex);
        }
        return null;
    }

}