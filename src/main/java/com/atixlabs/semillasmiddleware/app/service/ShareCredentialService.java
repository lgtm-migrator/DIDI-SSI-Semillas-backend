package com.atixlabs.semillasmiddleware.app.service;

import com.atixlabs.semillasmiddleware.app.exceptions.CredentialNotExistsException;
import com.atixlabs.semillasmiddleware.app.exceptions.PersonDoesNotExistsException;
import com.atixlabs.semillasmiddleware.app.model.beneficiary.Person;
import com.atixlabs.semillasmiddleware.app.model.credential.Credential;
import com.atixlabs.semillasmiddleware.app.model.credential.CredentialFilterDto;
import com.atixlabs.semillasmiddleware.app.model.credential.ShareCredentialRequest;
import com.atixlabs.semillasmiddleware.app.model.credential.constants.CredentialCategoriesCodes;
import com.atixlabs.semillasmiddleware.app.model.provider.service.ProviderService;
import com.atixlabs.semillasmiddleware.app.model.Email;
import com.atixlabs.semillasmiddleware.util.EmailTemplatesUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;


@Service
@Slf4j
public class ShareCredentialService {

    @Autowired
    public ShareCredentialService(ProviderService providerService,
                                  MailService mailService,
                                  PersonService personService,
                                  CredentialService credentialService){
        this.providerService = providerService;
        this.mailService = mailService;
        this.personService = personService;
        this.credentialService = credentialService;
    }

    @Value("${frontend.url}")
    private String frontendUrl;

    private static final String TEMPLATE_NAME = "share_credentials_template.html";
    private static final String PROVIDER_NAME_PARAM ="{providerName}";
    private static final String BENEFICIARY_NAME_PARAM ="{name}";
    private static final String BENEFICIARY_LASTNAME_PARAM ="{lastname}";
    private static final String BENEFICIARY_DNI_PARAM ="{dni}";
    private static final String BENEFICIARY_PHONE_PARAM ="{phone}";
    private static final String BENEFICIARY_EMAIL_PARAM ="{email}";
    private static final String BENEFICIARY_BIRTHDATE_PARAM ="{birthdate}";
    private static final String BENEFICIARY_CHARACTER_PARAM ="{character}";
    private static final String OWNER_NAME_PARAM ="{nameOwner}";
    private static final String OWNER_LASTNAME_PARAM ="{lastnameOwner}";
    //not yet implemented
    // private static final String EXPIRE_DATE_PARAM ="{expireDate}";
    private static final String SHARED_CREDENTIAL_LINK_PARAM ="{sharedLink}";
    private static final String HIMSELF_OR_FAMILIAR_PARAM ="{himselfOrFamiliar}";
    private static final String FRONTEND_URL_PARAM ="{frontendUrl}";


    private static final String FAMILY_BENEFIT_TEXT = "Integrante del grupo familiar";
    private static final String OWN_BENEFIT_TEXT = "Beneficio propio";
    private static final String HIMSELF_TEXT = "el mismo";
    private static final String FAMILIAR_TEXT = "un familiar";

    private static final String EMAIL_SUBJECT = "Solicitud de turno - Beneficio Semillas";

    private ProviderService providerService;
    private MailService mailService;
    private PersonService personService;
    private CredentialService credentialService;

    public void shareCredential(ShareCredentialRequest credentialRequest){
        Email email = Email.builder()
            .subject(EMAIL_SUBJECT)
            .to(getTo(credentialRequest))
            .template(getTemplate(credentialRequest))
            .build();

         mailService.send(email);
    }

    private String getTo(ShareCredentialRequest credentialRequest){
        return credentialRequest.getCustomProviderEmail()
                .orElseGet(() -> providerService.findById(credentialRequest.getProviderId().get()).getEmail());
    }


    private String getTemplate(ShareCredentialRequest credentialRequest){
        return EmailTemplatesUtil.replaceParams(EmailTemplatesUtil.getTemplate(TEMPLATE_NAME), getTemplateParameters(credentialRequest));
    }


    //TODO: change parameter replacement for a more light weight solution
    private Map<String, String> getTemplateParameters(ShareCredentialRequest credentialRequest ){
        Map<String, String> parameters = new HashMap<>();

        String name = credentialRequest.getProviderId()
                .map(provId -> " "+providerService.findById(provId).getName())
                .orElse("");

        Person person = personService.findByDocumentNumber(credentialRequest.getDni()).orElseThrow(() -> new PersonDoesNotExistsException(""));

        CredentialFilterDto credentialFilterDto = CredentialFilterDto
                .builder()
                .category(Optional.of(CredentialCategoriesCodes.BENEFIT.getCode()))
                .beneficiaryDni(Optional.of(credentialRequest.getDni()))
                .did(Optional.of(credentialRequest.getDid()))
                .build();

        List<Credential> credentials = credentialService.findAll(credentialFilterDto);
        Credential cred = credentials.stream().findFirst().orElseThrow(() -> new CredentialNotExistsException("There are no Benefit credentials emitted for the specified beneficiary and credit holder."));

        String character;
        String himselfOrFamiliar;
        if (!cred.getBeneficiaryDni().equals(cred.getCreditHolderDni())) {
            character = FAMILY_BENEFIT_TEXT;
            himselfOrFamiliar = FAMILIAR_TEXT;
        }else{
            character = OWN_BENEFIT_TEXT;
            himselfOrFamiliar = HIMSELF_TEXT;
        }

        parameters.put(PROVIDER_NAME_PARAM, name);
        parameters.put(BENEFICIARY_NAME_PARAM, person.getFirstName());
        parameters.put(BENEFICIARY_LASTNAME_PARAM, person.getLastName() );
        parameters.put(BENEFICIARY_DNI_PARAM ,person.getDocumentNumber().toString());
        parameters.put(BENEFICIARY_BIRTHDATE_PARAM, person.getBirthDate().toString());
        parameters.put(BENEFICIARY_PHONE_PARAM, credentialRequest.getPhone());
        parameters.put(BENEFICIARY_EMAIL_PARAM, credentialRequest.getEmail());
        parameters.put(OWNER_NAME_PARAM, credentials.stream().findFirst().orElse(new Credential()).getCreditHolderFirstName());
        parameters.put(OWNER_LASTNAME_PARAM, credentials.stream().findFirst().orElse(new Credential()).getCreditHolderLastName());
        parameters.put( BENEFICIARY_CHARACTER_PARAM, character);
        parameters.put(HIMSELF_OR_FAMILIAR_PARAM, himselfOrFamiliar);
        parameters.put(SHARED_CREDENTIAL_LINK_PARAM, credentialRequest.getViewerJWT());
        parameters.put(FRONTEND_URL_PARAM, frontendUrl);

        return parameters;
    }





}
