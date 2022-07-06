package com.atixlabs.semillasmiddleware.app.controller;

import com.atixlabs.semillasmiddleware.app.exceptions.CredentialNotExistsException;
import com.atixlabs.semillasmiddleware.app.exceptions.PersonDoesNotExistsException;
import com.atixlabs.semillasmiddleware.app.dto.ApiResponse;
import com.atixlabs.semillasmiddleware.app.dto.CredentialDto;
import com.atixlabs.semillasmiddleware.app.dto.RevokeRequestDto;
import com.atixlabs.semillasmiddleware.app.model.credential.ShareCredentialRequest;
import com.atixlabs.semillasmiddleware.app.model.credential.constants.CredentialStatesCodes;
import com.atixlabs.semillasmiddleware.app.model.credential.constants.CredentialTypesCodes;
import com.atixlabs.semillasmiddleware.app.model.provider.exception.InexistentProviderException;
import com.atixlabs.semillasmiddleware.app.processControl.exception.InvalidProcessException;
import com.atixlabs.semillasmiddleware.app.service.CredentialService;
import com.atixlabs.semillasmiddleware.app.service.ShareCredentialService;
import com.atixlabs.semillasmiddleware.excelparser.dto.ProcessExcelFileResult;
import com.atixlabs.semillasmiddleware.filemanager.exception.EmptyFileException;
import com.atixlabs.semillasmiddleware.filemanager.exception.FileManagerException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping(CredentialController.URL_MAPPING_CREDENTIAL)
@Slf4j
public class CredentialController {

    public static final String URL_MAPPING_CREDENTIAL = "/credentials";

    private CredentialService credentialService;
    private ShareCredentialService shareCredentialService;

    @Autowired
    public CredentialController(CredentialService credentialService,
                                ShareCredentialService shareCredentialService) {
        this.credentialService = credentialService;
        this.shareCredentialService = shareCredentialService;
    }

    @PostMapping("/importCredentials")
    @ResponseBody
    public ResponseEntity<ProcessExcelFileResult> importCredentials(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false, defaultValue = "false") boolean createCredentials)
            throws FileManagerException, IOException {

        log.info("uploadFile executed");

        if (file.isEmpty()) {
            throw new EmptyFileException("Empty file");
        }

        return ResponseEntity.ok(credentialService.importCredentials(file,createCredentials));
    }

    //TODO fix
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public Page<CredentialDto> findCredentials(@RequestParam @DefaultValue("0") Integer page,
                                          @RequestParam(required = false) String credentialType,
                                          @RequestParam(required = false) String name,
                                          @RequestParam(required = false) String surname,
                                          @RequestParam(required = false) String dniBeneficiary,
                                          @RequestParam(required = false) String creditHolderDni,
                                          @RequestParam(required = false) String idDidiCredential,
                                          @RequestParam(required = false) String lastUpdate,
                                          @RequestParam(required = false) List<String> credentialState) {

        Page<CredentialDto> credentials;
        List<String> personData = Arrays.asList(new String[]{name, surname, dniBeneficiary, creditHolderDni});
        try {
            log.info("find credentials "+credentialType+" "+name+" "+surname+" "+dniBeneficiary+" "+idDidiCredential+" "+lastUpdate+" "+credentialState.get(0));
            credentials = credentialService.findCredentials(credentialType, personData, idDidiCredential, lastUpdate, credentialState, page);
        } catch (Exception e) {
            log.error("There has been an error searching for credentials with the filters "+ credentialType + " " + name + " " + dniBeneficiary + " " + idDidiCredential + " " +
                    credentialState.toString(), e);
            return null;
        }
        return credentials;
    }

    @GetMapping("/states")
    @ResponseStatus(HttpStatus.OK)
    public Map<String, String> findCredentialStates() {
        Map<String, String> credentialStates = new HashMap<>();
        for (CredentialStatesCodes states : CredentialStatesCodes.values()) {
            credentialStates.put(states.name(), states.getCode());
        }

        return credentialStates;
    }

    @GetMapping("/types")
    @ResponseStatus(HttpStatus.OK)
    public List<String> findCredentialTypes() {
        return Arrays.stream(CredentialTypesCodes.values()).map(CredentialTypesCodes::getCode).collect(Collectors.toList());
    }

    @GetMapping("/revocation-reasons")
    @ResponseStatus(HttpStatus.OK)
    public Map<Long, String> findRevocationReasons() {
        //todo this is not the best option to obtain the reasons able by the user.
        return credentialService.getRevocationReasonsForUser();
    }

    @PatchMapping("/revoke/{idCredential}/reason/{idReason}")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<String> revokeCredential(@PathVariable @NotNull @Min(1) Long idCredential,
                                                   @PathVariable @NotNull @Min(1) Long idReason,
                                                   @RequestBody RevokeRequestDto revokeDto) {
        return credentialService.revokeCredentialProcess(idReason, idCredential, revokeDto);
    }

    @PostMapping("/generate")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<String> generateCredentialsCredit() {
        try {
            credentialService.generateCreditAndBenefitsCredentialsByLoans();
        }
        catch (InvalidProcessException ex){
            log.error("Could not get the process ! ", ex);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }


    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping("/share")
    public ResponseEntity<ApiResponse> notifyProvider(@Valid @RequestBody ShareCredentialRequest shareCredentialRequest){
        if (!shareCredentialRequest.getCustomProviderEmail().isPresent() && !shareCredentialRequest.getProviderId().isPresent())
            return ResponseEntity.badRequest().body(ApiResponse.error()
                    .setBody("You must either specify a provider id or an email")
                    .setUserMessage("Ocurrió un error al enviar el correo electrónico del Servicio que deseas utilizar. De persistir el problema, contactate con tu asesor de Programa Semillas."));

        try{
            shareCredentialService.shareCredential(shareCredentialRequest);
        }catch (PersonDoesNotExistsException pdnee){
            log.error("Person with dni "+shareCredentialRequest.getDni()+" does not exist");
            return ResponseEntity.badRequest().body(ApiResponse.error()
                    .setBody("person with dni "+shareCredentialRequest.getDni()+" not found")
                    .setUserMessage("Ocurrió un error al compartir la credencial de Beneficios Semillas con el Prestador del Servicio. Por favor, contactate con tu asesor de Programa Semillas."));

        }catch (InexistentProviderException ipe){
            log.error("Provider with id %s does not exist", shareCredentialRequest.getProviderId());
            return ResponseEntity.badRequest().body(ApiResponse.error()
                    .setBody("Provider with id "+shareCredentialRequest.getProviderId()+" does not exist")
                    .setUserMessage("El Servicio seleccionado ya no se encuentra activo en Programa Semillas. Disculpá las molestias..."));

        }catch (CredentialNotExistsException credEx){
            log.error("There are no Benefit credentials emitted for the specified beneficiary and credit holder");
            return ResponseEntity.badRequest().body(ApiResponse.error()
                    .setBody("There are no Benefit credentials emitted for the specified beneficiary and credit holder")
                    .setUserMessage("Ocurrió un error al compartir tu credencial de Identidad Semillas con el Prestador del Servicio. Por favor, contactate con tu asesor de Programa Semillas."));

        }catch (Exception ex){
            log.error("Could not send email message:" + ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error()
                    .setBody(ex.getMessage())
                    .setUserMessage("Ocurrió un error al compartir la credencial de Beneficios Semillas con el Prestador del Servicio. Por favor, contactate con tu asesor de Programa Semillas."));
        }

        return ResponseEntity.ok().body(ApiResponse.builder().body("shared.").build());
    }

}
