package com.atixlabs.semillasmiddleware.app.service;

import com.atixlabs.semillasmiddleware.app.bondarea.model.Loan;
import com.atixlabs.semillasmiddleware.app.bondarea.model.constants.LoanStateCodes;
import com.atixlabs.semillasmiddleware.app.bondarea.model.constants.LoanStatusCodes;
import com.atixlabs.semillasmiddleware.app.bondarea.repository.LoanRepository;
import com.atixlabs.semillasmiddleware.app.bondarea.service.LoanService;
import com.atixlabs.semillasmiddleware.app.exceptions.CredentialException;
import com.atixlabs.semillasmiddleware.app.exceptions.CredentialNotExistsException;
import com.atixlabs.semillasmiddleware.app.exceptions.PersonDoesNotExistsException;
import com.atixlabs.semillasmiddleware.app.model.credential.*;
import com.atixlabs.semillasmiddleware.app.model.credential.constants.*;
import com.atixlabs.semillasmiddleware.app.processControl.model.constant.ProcessControlStatusCodes;
import com.atixlabs.semillasmiddleware.app.repository.*;
import com.atixlabs.semillasmiddleware.app.didi.model.DidiAppUser;
import com.atixlabs.semillasmiddleware.app.didi.service.DidiAppUserService;
import com.atixlabs.semillasmiddleware.app.didi.service.DidiService;
import com.atixlabs.semillasmiddleware.app.dto.CredentialDto;
import com.atixlabs.semillasmiddleware.app.dto.RevokeRequestDto;
import com.atixlabs.semillasmiddleware.app.model.beneficiary.Person;
import com.atixlabs.semillasmiddleware.app.model.beneficiary.PersonBuilder;
import com.atixlabs.semillasmiddleware.app.model.configuration.ParameterConfiguration;
import com.atixlabs.semillasmiddleware.app.model.configuration.constants.ConfigurationCodes;
import com.atixlabs.semillasmiddleware.app.model.CredentialState.CredentialState;
import com.atixlabs.semillasmiddleware.app.model.CredentialState.RevocationReason;
import com.atixlabs.semillasmiddleware.app.model.CredentialState.constants.RevocationReasonsCodes;
import com.atixlabs.semillasmiddleware.app.model.excel.Child;
import com.atixlabs.semillasmiddleware.app.model.excel.ExcelUtils;
import com.atixlabs.semillasmiddleware.app.model.excel.FamilyMember;
import com.atixlabs.semillasmiddleware.app.model.excel.Form;
import com.atixlabs.semillasmiddleware.app.processControl.exception.InvalidProcessException;
import com.atixlabs.semillasmiddleware.app.processControl.model.constant.ProcessNamesCodes;
import com.atixlabs.semillasmiddleware.app.processControl.service.ProcessControlService;
import com.atixlabs.semillasmiddleware.excelparser.app.categories.Category;
import com.atixlabs.semillasmiddleware.excelparser.app.categories.DwellingCategory;
import com.atixlabs.semillasmiddleware.excelparser.app.categories.EntrepreneurshipCategory;
import com.atixlabs.semillasmiddleware.excelparser.app.categories.PersonCategory;
import com.atixlabs.semillasmiddleware.excelparser.app.constants.Categories;
import com.atixlabs.semillasmiddleware.excelparser.app.constants.PersonType;
import com.atixlabs.semillasmiddleware.excelparser.app.dto.SurveyForm;
import com.atixlabs.semillasmiddleware.excelparser.dto.ExcelErrorDetail;
import com.atixlabs.semillasmiddleware.excelparser.dto.ProcessExcelFileResult;
import com.atixlabs.semillasmiddleware.filemanager.exception.FileManagerException;
import com.atixlabs.semillasmiddleware.util.CredentialUtils;
import com.atixlabs.semillasmiddleware.util.DateUtil;
import com.poiji.bind.Poiji;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.persistence.criteria.Predicate;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.atixlabs.semillasmiddleware.app.model.credential.constants.CredentialTypesCodes.*;
import static com.atixlabs.semillasmiddleware.excelparser.app.constants.Categories.*;
import static com.atixlabs.semillasmiddleware.excelparser.app.constants.PersonType.*;
import static com.atixlabs.semillasmiddleware.excelparser.dto.ExcelErrorType.*;

@Slf4j
@Service
public class CredentialService {
    private CredentialRepository credentialRepository;
    private CredentialCreditRepository credentialCreditRepository;
    private CredentialIdentityService credentialIdentityService;
    private CredentialEntrepreneurshipRepository credentialEntrepreneurshipRepository;
    private CredentialDwellingRepository credentialDwellingRepository;
    private PersonRepository personRepository;
    private LoanRepository loanRepository;
    private CredentialBenefitsRepository credentialBenefitsRepository;
    private CredentialStateRepository credentialStateRepository;
    private ParameterConfigurationRepository parameterConfigurationRepository;
    private DidiService didiService;
    private RevocationReasonRepository revocationReasonRepository;
    private LoanService loanService;
    private ProcessControlService processControlService;
    private CredentialBenefitService credentialBenefitService;
    private CredentialStateService credentialStateService;
    private CredentialBenefitSancorService credentialBenefitSancorService;
    private DidiAppUserService didiAppUserService;
    private CredentialCreditService credentialCreditService;

    private CredentialIdentityRepository credentialIdentityRepository;

    private List<CredentialState> credentialStateActivePending;

    @Value("${credentials.pageSize}")
    private String size;


    @Autowired
    public CredentialService(
            CredentialIdentityRepository credentialIdentityRepository,
            CredentialCreditRepository credentialCreditRepository,
            CredentialRepository credentialRepository,
            PersonRepository personRepository,
            LoanRepository loanRepository,
            CredentialBenefitsRepository credentialBenefitsRepository,
            CredentialStateRepository credentialStateRepository,
            CredentialIdentityService credentialIdentityService,
            CredentialEntrepreneurshipRepository credentialEntrepreneurshipRepository,
            CredentialDwellingRepository credentialDwellingRepository,
            ParameterConfigurationRepository parameterConfigurationRepository,
            DidiService didiService,
            DidiAppUserService didiAppUserService,
            RevocationReasonRepository revocationReasonRepository, LoanService loanService, ProcessControlService processControlService, CredentialBenefitService credentialBenefitService, CredentialStateService credentialStateService, CredentialBenefitSancorService credentialBenefitSancorService, CredentialCreditService credentialCreditService) {
        this.credentialCreditRepository = credentialCreditRepository;
        this.credentialRepository = credentialRepository;
        this.personRepository = personRepository;
        this.loanRepository = loanRepository;
        this.credentialBenefitsRepository = credentialBenefitsRepository;
        this.credentialStateRepository = credentialStateRepository;
        this.parameterConfigurationRepository = parameterConfigurationRepository;
        this.credentialIdentityService = credentialIdentityService;
        this.credentialEntrepreneurshipRepository = credentialEntrepreneurshipRepository;
        this.credentialDwellingRepository = credentialDwellingRepository;
        this.didiService = didiService;
        this.revocationReasonRepository = revocationReasonRepository;
        this.loanService = loanService;
        this.processControlService = processControlService;
        this.credentialBenefitService = credentialBenefitService;
        this.credentialStateService = credentialStateService;
        this.credentialBenefitSancorService = credentialBenefitSancorService;
        this.didiAppUserService = didiAppUserService;
        this.credentialCreditService = credentialCreditService;
        this.credentialIdentityRepository = credentialIdentityRepository;
        this.credentialStateActivePending = credentialStateRepository.findByStateNameIn(
                List.of(CredentialStatesCodes.PENDING_DIDI.getCode(),
                        CredentialStatesCodes.CREDENTIAL_ACTIVE.getCode(),
                        CredentialStatesCodes.HOLDER_ACTIVE_KINSMAN_PENDING.getCode()));
    }


    private Specification<Credential> getCredentialSpecification (CredentialFilterDto credentialFilterDto) {
        return (Specification<Credential>) (root, query, cb) -> {
            Stream<Predicate> predicates = Stream.of(
                    credentialFilterDto.getDid().map(value -> cb.equal(root.get("idDidiReceptor"), value)),
                    credentialFilterDto.getCategory().map(value -> cb.equal(root.get("credentialCategory"), value)),
                    credentialFilterDto.getBeneficiaryDni().map(value -> cb.equal(root.get("beneficiaryDni"), value))
            ).flatMap(Optional::stream);
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    public List<Credential> findAll(CredentialFilterDto credentialFilterDto){
        return credentialRepository.findAll(getCredentialSpecification(credentialFilterDto));
    }

    /**
     * Generate and update the credentials credit.
     * Checking for defaulters, and revoking or activate credentials (credit and benefit)
     */
    public void generateCreditAndBenefitsCredentialsByLoans() throws InvalidProcessException {
        //check if process in credentials is not running
        if (!processControlService.isProcessRunning(ProcessNamesCodes.BONDAREA) && !processControlService.isProcessRunning(ProcessNamesCodes.CHECK_DEFAULTERS)) {

            LocalDateTime lastTimeProcessRun = processControlService.getProcessTimeByProcessCode(ProcessNamesCodes.CREDENTIALS.getCode());

            processControlService.setStatusToProcess(ProcessNamesCodes.CREDENTIALS, ProcessControlStatusCodes.RUNNING);

            log.info(String.format("Generate Credential and Benefits Credential form %s", lastTimeProcessRun.toString()));

            try {
                this.handleDefaultCredits(lastTimeProcessRun);
                this.handleActiveCredits(lastTimeProcessRun);
                this.handleFinalizeCredits(lastTimeProcessRun);
                this.handleCancelledCredits(lastTimeProcessRun);
                this.handleNewCredits();

            } catch (Exception ex) {
                log.error("Error updating credentials credit ! " + ex.getMessage(), ex);
                processControlService.setStatusToProcess(ProcessNamesCodes.CREDENTIALS.getCode(), ProcessControlStatusCodes.FAIL.getCode());
            }


            //finish process
            processControlService.setStatusToProcess(ProcessNamesCodes.CREDENTIALS.getCode(), ProcessControlStatusCodes.OK.getCode());

        } else {
            log.info("Generate credentials can't run ! Process " + ProcessNamesCodes.BONDAREA.getCode() + " or " + ProcessNamesCodes.CHECK_DEFAULTERS.getCode() + " is still running");
        }

    }

    /**
     * Create a new credential only if
     * - The loan is active
     * - The loan is not in default
     * - The Holddr is not in default
     */
    private List<Loan> handleNewCredits() {
        //create credentials
        List<Loan> newLoans = loanService.findActiveAndOkLoansWithoutCredential();

        log.info(String.format("Generate Credential and Benefits Credential for %d new loans", newLoans.size()));

        List<Loan> loansForReview = new ArrayList<>();

        for (Loan newLoan : newLoans) {
            try {
                credentialBenefitService.createCredentialsBenefitsHolderForNewLoan(newLoan);
                credentialBenefitService.createCredentialsBenefitsFamilyForNewLoan(newLoan);
                credentialBenefitSancorService.createCredentialsBenefitsHolderForNewLoan(newLoan);
            } catch (PersonDoesNotExistsException | CredentialException ex) {
                log.error("Error creating new credential credit for loan " + newLoan.getIdBondareaLoan() + " " + ex.getMessage());
                loansForReview.add(newLoan);
            }
        }

        return loansForReview;
    }

    /**
     * Create a new credential only if
     * - The loan is active
     * - The loan is not in default
     * - The holder is not in default
     */
    public CredentialCredit createNewCreditCredential(Loan loan) throws CredentialException, PersonDoesNotExistsException {

        log.info("Creating New Credential Credit for loan " + loan.getIdBondareaLoan());

        if (Boolean.FALSE.equals(!this.isLoanValidForCredentialCredit(loan))) {
            log.error(loan + loan.getIdBondareaLoan() + " is not valid for credential credit");
            return null;
        }


        Optional<CredentialCredit> opCreditExistence = this.getCredentialCreditForLoan(loan);

        if (opCreditExistence.isEmpty() || opCreditExistence.get().isRevoked()) {

            Optional<Person> opBeneficiary = personRepository.findByDocumentNumber(loan.getDniPerson());

            if (opBeneficiary.isPresent()) {

                CredentialCredit credit = this.buildCreditCredential(loan, opBeneficiary.get(), null);
                loan.setHasCredential(true);

                credit = credentialCreditRepository.save(credit);
                //get the new id and save it on id historic
                credit.setIdHistorical(credit.getId());
                credentialCreditRepository.save(credit);
                loanRepository.save(loan);
                log.info("Credential Credit created for loan: " + loan.getIdBondareaLoan() + " dni: " + opBeneficiary.get().getDocumentNumber());
                return credit;

            } else {
                log.error("Person with dni " + loan.getDniPerson() + " has not been created. The loan exists but the survey with this person has not been loaded");
                throw new PersonDoesNotExistsException("Person with dni " + loan.getDniPerson() + " has not been created. The loan exists but the survey with this person has not been loaded");
            }
        } else {
            loan.setHasCredential(true);
            loanRepository.save(loan);
            log.error("The credit with idBondarea " + loan.getIdBondareaLoan() + " has an existent credential");
            return opCreditExistence.get();
        }
    }






    public Optional<Credential> getCredentialById(Long id) {
        //validate credential is in bd
        return credentialRepository.findById(id);
    }

    public Page<CredentialDto> findCredentials(String credentialType, List<String> personData,
                                               String idDidiCredential, String lastUpdate, List<String> credentialState, Integer pageNumber) {
        Page<Credential> credentials;
        Pageable pageable = null;
        if (pageNumber != null && pageNumber >= 0 && this.size != null)
            pageable = PageRequest.of(pageNumber, Integer.parseInt(size), Sort.by(Sort.Direction.ASC, "updated"));

        credentials = credentialRepository.findCredentialsWithFilter
                (credentialType, personData.get(0), personData.get(1), personData.get(2), personData.get(3),
                        idDidiCredential, lastUpdate, credentialState, pageable);

        return credentials.map(CredentialDto::constructBasedOnCredentialType);
    }

    public Map<Long, String> getRevocationReasonsForUser() {
        Map<Long, String> revocationReasons = new HashMap<>();
        Optional<RevocationReason> expiredReason = revocationReasonRepository.findByReason(RevocationReasonsCodes.EXPIRED_INFO.getCode());
        if (expiredReason.isPresent())
            revocationReasons.put(expiredReason.get().getId(), expiredReason.get().getReason());
        else
            log.error("Error getting expired reason of revocation");

        Optional<RevocationReason> unlinkingReason = revocationReasonRepository.findByReason(RevocationReasonsCodes.UNLINKING.getCode());
        if (unlinkingReason.isPresent())
            revocationReasons.put(unlinkingReason.get().getId(), unlinkingReason.get().getReason());
        else
            log.error("Error getting unlinking reason of revocation");

        Optional<RevocationReason> manualUpdateReason = revocationReasonRepository.findByReason(RevocationReasonsCodes.MANUAL_UPDATE.getCode());
        if (manualUpdateReason.isPresent())
            revocationReasons.put(manualUpdateReason.get().getId(), manualUpdateReason.get().getReason());
        else
            log.error("Error getting manual_update reason of revocation");

        return revocationReasons;
    }

    public Optional<String> getReasonFromId(Long idReason) {
        if (idReason != null) {
            Optional<RevocationReason> reason = revocationReasonRepository.findById(idReason);
            if (reason.isPresent()) {
                //validate if the reason could be one allowed to the user.
                Map<Long, String> reasonsForUser = getRevocationReasonsForUser();
                if (reasonsForUser.containsValue(reason.get().getReason()))
                    return Optional.of(reason.get().getReason());
            }
        }
        return Optional.empty();
    }


    /**
     * Si existe el titular como persona registrada
     * Set holder in default and
     * Credit Credential
     * If exists and is emitted, revoke complete
     * If exists and is Pending Didi, revoke only local
     * If exists and is revoked, do nothing
     * If not exists, do nothing
     * Credencial de BenefihandleDefaultCreditscio
     * Titular
     * Si existe y esta activa y emitida, revoco la crendencial en Didi y revoco la credencial localmente,
     * Si existe y esta pendiente de didi, revoco la credencial localmente
     * si existe y esta revocada, no hago nada
     * si no existe, no hago nada
     * Familiar
     * Credencial de Beneficio Familiar-Titular con Id Didi del Titular
     * Si existe y esta activa y emitida, revoco la crendencial en Didi y revoco la credencial localmente,
     * Si existe y esta pendiente de didi, revoco la credencial localmente
     * si existe y esta revocada, no hago nada
     * si no existe, no hago nada
     * Credendial de Beneficio Familiar-Titular con Id Didi del Familiar
     * Si existe y esta activa y emitida, revoco la crendencial en Didi y revoco la credencial localmente,
     * Si existe y esta pendiente de didi, revoco la credencial localmente
     * si existe y esta revocada, no hago nada
     * si no existe, no hago nada
     *
     * @param lastTimeProcessRun
     * @return
     */
    private List<Loan> handleDefaultCredits(LocalDateTime lastTimeProcessRun) {

        List<Loan> loansModifiedInDefault = loanService.findLastLoansModifiedInDefault(lastTimeProcessRun);

        log.info(String.format(" %d Loans in default founded",  loansModifiedInDefault.size()));

        List<Loan> loansToReview = new ArrayList<>();

        for (Loan defaultLoan : loansModifiedInDefault) {

            Optional<Person> opHolder = personRepository.findByDocumentNumber(defaultLoan.getDniPerson());

            if (opHolder.isPresent()) {

                Person holder = opHolder.get();

                try {
                    if (!this.revokeCredentialCredit(defaultLoan, holder)) {
                        loansToReview.add(defaultLoan);
                    }

                    credentialBenefitService.revokeHolderCredentialsBenefitsForLoan(holder);
                    credentialBenefitService.revokeFamilyCredentialsBenefitsForLoan(holder);
                    credentialBenefitSancorService.revokeHolderCredentialsBenefitsForLoan(holder);

                } catch (CredentialException ex) {
                    log.error("Error creating new credential credit for loan " + defaultLoan.getIdBondareaLoan() + " " + ex.getMessage());
                    loansToReview.add(defaultLoan);
                }
            } else { //Holder not exists
                String message = String.format("Can't evaluate default for Credit and Benefit Credential, Holder dni %d not exists", defaultLoan.getDniPerson());
                log.error(message);
                loansToReview.add(defaultLoan);
            }
        }

        return loansToReview;
    }

    /**
     *     Si existe el titular como persona registrada y el credito posee credenciales previas creadas (sino pasa al flujo de creditos nuevos)
     *     Si el credito actual esta en la lista de moras del Titular,  lo quito
     *     Verifico el estado del Titular
     *         Si esta en Mora
     *             Credencial Crediticia
     *                 Si la credencial crediticia esta activa y emitida
     *                     Revoco la credencial anterior Tanto en Didi como localmente y creo una nueva en estado pendiente de Didi
     *                 Si la credencial creditica esta pendiente de didi
     *                     Revoco la credencial anterior localmente, creo una nueva credencial pendiente de Didi
     *                 Si la credencial credicitia esta revocada
     *                     Creo una nueva credencial crediticia en estado Pendiente de Didi
     *             Credencial de Beneficio
     *                 No modifico el estado de ninguna de las credenciales de beneficio asociadas al credito
     *          Si el Titular NO esta en Mora
     *             Credencial Crediticia
     *                 Si la credencial crediticia esta activa y emitida
     *                     Revoco la credencial anterior tanto en Didi como localmente y creo una nueva en estado pendiente de Didi
     *                 Si la credencial creditica esta pendiente de Didi
     *                     Revoco la credencial anterior localmente, creo una nueva credencial pendiente de Didi
     *                 Si la credencial credicitia esta revocada
     *                     Creo una nueva credencial crediticia en estado Pendiente de Didi
     *             Credencial de Beneficio
     *                 Titular
     *                     Si la Crendencial desta Activa y Emitida, no hago nada
     *                     Si la Credencial esta Pendiende de Didi, no hago nada
     *                     Si la Credencial esta revocada, creo una nueva credencial en estado pendiente de Didi
     *                 Familiar
     *                     Si la Crendencial desta Activa y Emitida, no hago nada
     *                     Si la Credencial esta Pendiende de Didi, no hago nada
     *                     Si la Credencial esta revocada, creo una nueva credencial en estado pendiente de Didi
     *
     * @param lastTimeProcessRun
     * @return
     */
    private List<Loan> handleActiveCredits(LocalDateTime lastTimeProcessRun) {

        List<Loan> loansModifiedActive = loanService.findLastLoansModifiedActiveWithCredential(lastTimeProcessRun);

        log.info(String.format(" %d active credits found for evaluate credentials ", (loansModifiedActive != null ? loansModifiedActive.size() : 0)));

        List<Loan> loansToreview = new ArrayList<>();

        for (Loan loan : loansModifiedActive) {
            try {
                this.updateOrCreateCredentialsCreditsAndCredentialBenefits(loan);
            } catch (CredentialException  e) {
                loansToreview.add(loan);
            }
        }

        return loansToreview;

    }


    /**
     * Si el credito esta activo
     *  Cred Credito
     *      Cargo fecha de finalizacion a la credencial, no modifico su estado
     *  Beneficio
     *      Si es el unico credito activo para el titular
     *          Revoco beneficios asociados al titular y familiares, si estan vigentes o pendientes de didi
     *      Si no es el unico credito activo
     *          No hago nada
     *  Sancor
     *      Si es el unico credito, revoco la credencial
     *      si tiene mas creditos no hago nada
     * Si el Credito estaba en Mora (se supone credenciales revocadas)
     *      Quito el credito del listado de mora del titularCred Credito
     *      Cargo fecha de finalizacion a la credencial, no modifico su estadoCred
     *  Beneficio
     *      Si es el unico credito activo para el titular
     *          No hago nada, se supone que todas las credenciales estan revocadas
     *      Si Tiene mas creditos
     *          Marco los demas creditos para revision en el próximo proceso y que verifique segun su estado lo que deben hacer conlas credenciales
     *  Sancor
     *      Si es el unico credito, revoco la credencial si es necesario
     *      Si tiene mas creditos no hago nada
     * @param lastTimeProcessRun
     * @return
     * @throws PersonDoesNotExistsException
     */
    private List<Loan> handleFinalizeCredits(LocalDateTime lastTimeProcessRun) {

        List<Loan> loansModifiedFinalized = loanService.findLastLoansModifiedFinalized(lastTimeProcessRun);
        HashSet<Loan> loansToreview = new HashSet<>();

        for (Loan loan : loansModifiedFinalized) {

            Optional<CredentialCredit> opCredit = credentialCreditRepository.findFirstByIdBondareaCreditOrderByDateOfIssueDesc(loan.getIdBondareaLoan());
            if (opCredit.isPresent()) {
                try {

                    List<Loan> otherLoansActiveForHolder = this.getOthersLoansActivesForHolder(loan);

                    loansToreview.addAll(credentialBenefitService.handleLoanFinalized(loan, otherLoansActiveForHolder));
                    loansToreview.addAll(credentialBenefitSancorService.handleLoanFinalized(loan, otherLoansActiveForHolder));

                    this.closeCredit(opCredit.get(), loan);
                    log.info("Credential Credit is set to FINALIZE, for credential id historic" + opCredit.get().getIdHistorical());

                } catch (Exception e) {
                    loansToreview.add(loan);
                }
            } else {
                log.info("Loan {} don´t have credential, nothing to do", loan.getIdBondareaLoan());
            }
        }

        return  new ArrayList<>(loansToreview);

    }

    public List<Loan> getOthersLoansActivesForHolder(Loan loan){
        return this.loanService.findOthersLoansActivesForHolder(loan);
    }

    private void closeCredit(CredentialCredit credentialCredit, Loan loan) {

        credentialCredit.setFinishDate(DateUtil.getLocalDateTimeNow().toLocalDate());
        credentialCreditRepository.save(credentialCredit);
        log.info("Credential Credit is set to Finish Date, for credential id historic" + credentialCredit.getIdHistorical());

        Optional<Person> holder = personRepository.findByDocumentNumber(credentialCredit.getCreditHolderDni());

        if (holder.isPresent() && holder.get().isInDefault() && holder.get().removeLoanInDefault(loan)) {
            personRepository.save(holder.get());
            log.info("Loan {} in default remove for holder " + loan.getIdBondareaLoan() + "" + holder.get().getDocumentNumber());
        }

    }

    /**
     * Si cancelo un credito
     * Si el Credito estaba Activo
     * Revoco beneficios asociados al titular y familiares, solo si es el unico credito activo para el titular, queda credencial crediticia vigente (referencia a la pregunta que te contesté en mail anterior)
     * Si el Credito estaba en Mora (se supone credenciales revocadas)
     * Doy de baja el credito en Mora para el Titular
     * Verifico el estado del Titular
     * Reactivo sus beneficios de otros creditos activos de los que sea Titular en estado Pendiente de Didi  (Idem, casos anteriores:  La credencial de beneficio a nombre del titular es 1 sóla, NO tiene una por cada crédito. Por lo tanto sería:  Reactivo credencial de Beneficios sí el titular tiene otros créditos activos)
     * Reactivo los beneficios de los familiares relacionados con el Titular para ese Credito en estado Pendiente de Didi SI SOLO SI el titular tiene otros créditos activos
     */
    private List<Loan> handleCancelledCredits(LocalDateTime lastTimeProcessRun) {

        List<Loan> loansModifiedCancelled = loanService.findLastLoansModifiedCancelled(lastTimeProcessRun);
        HashSet<Loan> loansToreview = new HashSet<>();

        for (Loan loan : loansModifiedCancelled) {

            Optional<CredentialCredit> opCredit = credentialCreditRepository.findFirstByIdBondareaCreditOrderByDateOfIssueDesc(loan.getIdBondareaLoan());
            if (opCredit.isPresent()) {
                try {

                    List<Loan> otherLoansActiveForHolder = this.getOthersLoansActivesForHolder(loan);

                    loansToreview.addAll(credentialBenefitService.handleLoanFinalized(loan, otherLoansActiveForHolder));
                    loansToreview.addAll(credentialBenefitSancorService.handleLoanFinalized(loan, otherLoansActiveForHolder));

                    this.cancelCredit(opCredit.get(), loan);
                    log.info("Credential Credit is set to CANCELLED, for credential id historic" + opCredit.get().getIdHistorical());

                } catch (Exception e) {
                    loansToreview.add(loan);
                }
            } else {
                log.info("Loan " + loan.getIdBondareaLoan() + " dont have credential, nothing to do");
            }
        }

        return  new ArrayList<>(loansToreview);

    }

    private void cancelCredit(CredentialCredit credentialCredit, Loan loan) throws CredentialException {

        credentialCredit.setFinishDate(DateUtil.getLocalDateTimeNow().toLocalDate());
        if(Boolean.TRUE.equals(isCredentialRevoked(credentialCredit))){
            credentialCreditRepository.save(credentialCredit);
        }else
            this.revokeComplete(credentialCredit, RevocationReasonsCodes.CANCELLED);

        Optional<Person> holder = personRepository.findByDocumentNumber(credentialCredit.getCreditHolderDni());

        if (holder.isPresent() && holder.get().isInDefault() && holder.get().removeLoanInDefault(loan)) {
            personRepository.save(holder.get());
            log.info("Loan " + loan.getIdBondareaLoan() + "in default remove for holder " + holder.get().getDocumentNumber());
        }

        log.info("Credential Credit for loan {} cancelled", loan.getIdBondareaLoan());

    }

    /**
     *     Si existe el titular como persona registrada y el credito posee credenciales previas creadas (sino pasa al flujo de creditos nuevos)
     *     Si el credito actual esta en la lista de moras del Titular,  lo quito
     *     Verifico el estado del Titular
     *         Si esta en Mora
     *             Credencial Crediticia
     *                 Si la credencial crediticia esta activa y emitida
     *                     Revoco la credencial anterior Tanto en Didi como localmente y creo una nueva en estado pendiente de Didi
     *                 Si la credencial creditica esta pendiente de didi
     *                     Revoco la credencial anterior localmente, creo una nueva credencial pendiente de Didi
     *                 Si la credencial credicitia esta revocada
     *                     Creo una nueva credencial crediticia en estado Pendiente de Didi
     *             Credencial de Beneficio
     *                 No modifico el estado de ninguna de las credenciales de beneficio asociadas al credito
     *          Si el Titular NO esta en Mora
     *             Credencial Crediticia
     *                 Si la credencial crediticia esta activa y emitida
     *                     Revoco la credencial anterior tanto en Didi como localmente y creo una nueva en estado pendiente de Didi
     *                 Si la credencial creditica esta pendiente de Didi
     *                     Revoco la credencial anterior localmente, creo una nueva credencial pendiente de Didi
     *                 Si la credencial credicitia esta revocada
     *                     Creo una nueva credencial crediticia en estado Pendiente de Didi
     *             Credencial de Beneficio
     *                 Titular
     *                     Si la Crendencial desta Activa y Emitida, no hago nada
     *                     Si la Credencial esta Pendiende de Didi, no hago nada
     *                     Si la Credencial esta revocada, creo una nueva credencial en estado pendiente de Didi
     *                 Familiar
     *                     Si la Crendencial desta Activa y Emitida, no hago nada
     *                     Si la Credencial esta Pendiende de Didi, no hago nada
     *                     Si la Credencial esta revocada, creo una nueva credencial en estado pendiente de Didi
     * @param loan
     * @throws PersonDoesNotExistsException
     * @throws CredentialException
     */
    private void updateOrCreateCredentialsCreditsAndCredentialBenefits(Loan loan) throws CredentialNotExistsException, CredentialException {

        log.info("Updating credential credit for loan " + loan.getIdBondareaLoan());

        Optional<Person> opHolder = personRepository.findByDocumentNumber(loan.getDniPerson());

        if (opHolder.isPresent()) {

            Person holder = opHolder.get();

            if(holder.removeLoanInDefault(loan)){
                holder = personRepository.save(holder);
            }

            if (!holder.isInDefault()) {
                this.updateCredentialCreditForActiveLoan(loan, holder);

                credentialBenefitService.updateCredentialBenefitForActiveLoan(loan,holder);
                credentialBenefitSancorService.updateCredentialBenefitForActiveLoan(loan, holder);

            } else {
                this.updateCredentialCreditForActiveLoan(loan, holder);
            }
        } else { //Holder not exists
            String message = String.format("Can't handle loan %s, Holder dni %d not exists", loan.getIdBondareaLoan(),loan.getDniPerson());
            log.error(message);
            throw new CredentialException(message);
        }

    }

    /**
     *      Credencial Crediticia
     *          If Credit Credential is Active and Emmited
     *              Revoke current and build new in state Pending Didi
     *          If Credit Credential is pending didi
     *              Revoke current and build new in state Pending Didi
     *          If Credit Credential is Revoke
     *              build new in state Pending Didi
     * @param loan
     * @param holder
     */
    //TODO validar si es necesario revocar y crear de neuvo la entidad, porque si falla en beneficio va a retomar el proceso de nuevo
    private void updateCredentialCreditForActiveLoan(Loan loan, Person holder) throws CredentialNotExistsException,CredentialException {
        // verificar que haya solo una credencial para ese id bondarea y revocar las viejas.
        Optional<CredentialCredit> opCredit = credentialCreditRepository.findFirstByIdBondareaCreditOrderByDateOfIssueDesc(loan.getIdBondareaLoan());

        if(opCredit.isPresent()) {
            CredentialCredit currentCredentialCredit = opCredit.get();

            if ((currentCredentialCredit.isEmitted() && this.isCredentialActive(currentCredentialCredit))
                    || Boolean.TRUE.equals(this.isCredentialPendingDidi(currentCredentialCredit))) {

                if(Boolean.TRUE.equals(this.credentialCreditMustBeenUpdate(currentCredentialCredit, loan))) {
                    this.revokeComplete(currentCredentialCredit, RevocationReasonsCodes.UPDATE_INTERNAL);
                    CredentialCredit newCredentialCredit = this.buildCreditCredential(loan, holder, currentCredentialCredit);
                    credentialCreditRepository.save(newCredentialCredit);
                }else {
                    log.info(String.format("The Credential credit %d does not have to be updated for loan %s",currentCredentialCredit.getId(), loan.getIdBondareaLoan()));
                }

            } else {
                if (Boolean.TRUE.equals(this.isCredentialRevoked(currentCredentialCredit))) {

                    CredentialCredit newCredentialCredit = this.buildCreditCredential(loan, holder, currentCredentialCredit);
                    credentialCreditRepository.save(newCredentialCredit);

                } else {
                    String message = String.format("Current credential credit id %s is not in valid state for modifications", currentCredentialCredit.getIdBondareaCredit());
                    log.error(message);
                    throw new CredentialException(message);
                }

            }
        }else {
            String message = String.format("Credential credit for loan  %s is not present",loan.getIdBondareaLoan());
            throw new CredentialNotExistsException(message);
        }
    }


    /*
    #Este metodo se utilizaba previamente para la creacion de las credenciales, pero se redujo esta funcionalidad al metodo importCredentials, pero pienso
    que para no juntar toda la logica en un solo metodo es posible subdivirla y reutilizar estos metodos.
     */
    /* TODO: Validar la reutilizacion de este metodo para la creacion de credenciales para aligerar la carga del metodo import credentials. Por el momento
        la comentare para poder organizar y definir la traza del PDF.
    */
    @Transactional
    public void buildAllCredentialsFromForm(SurveyForm surveyForm, boolean skipIdentityCredentials) throws CredentialException, FileManagerException {
        log.info("buildAllCredentialsFromForm: " + this.toString());
        saveAllCredentialsFromForm(surveyForm, skipIdentityCredentials);
    }

    /**
     * The following are non-public methods, isolating functionality.
     * to make public methods easier to read.
     *
     * @param surveyForm
     */
    /* TODO
        Al igual que el metodo de buildAllCredentialsFromForm, analizar la posibilidad de reutilizar estos metodos para aligerar la carga sobre
        el metodo importCredentials, ya que las validaciones de este metodo se estan realizando en el mismo metodo.
     */
    public boolean validateAllCredentialsFromForm(SurveyForm surveyForm, ProcessExcelFileResult processExcelFileResult,
                                                  boolean skipIdentityCredentials,
                                                  boolean pdfValidation) throws FileManagerException {
        log.info("  validateIdentityCredentialFromForm");

        //1-get all people data from form, creditHolder will be a beneficiary as well.
        List<Category> categoryArrayList = surveyForm.getAllCompletedCategories();

        //2-get creditHolder Data
        PersonCategory creditHolderPersonCategory = (PersonCategory) surveyForm.getCategoryByUniqueName(BENEFICIARY_CATEGORY_NAME.getCode(), null);
        Person creditHolder = Person.getPersonFromPersonCategory(creditHolderPersonCategory);

        //en cada service crear el si existe o no
        //2-verify each person is new, or his data has not changed.
        boolean allCredentialsNewOrInactive = true;
        List<String> statesCodesToFind = new ArrayList<>();
        statesCodesToFind.add(CredentialStatesCodes.PENDING_DIDI.getCode());
        statesCodesToFind.add(CredentialStatesCodes.CREDENTIAL_ACTIVE.getCode());
        statesCodesToFind.add(CredentialStatesCodes.HOLDER_ACTIVE_KINSMAN_PENDING.getCode());
        credentialStateActivePending = credentialStateRepository.findByStateNameIn(statesCodesToFind);
        String beneficiaryAddress = "";
        for (Category category : categoryArrayList) {
            List<Credential> credentialsOptional = new ArrayList<>();
            Categories categoryName = category.getCategoryName();

            if (categoryName.equals(BENEFICIARY_CATEGORY_NAME) || categoryName.equals(SPOUSE_CATEGORY_NAME)
                    || categoryName.equals(CHILD_CATEGORY_NAME) || categoryName.equals(KINSMAN_CATEGORY_NAME)) {

                if (categoryName.equals(BENEFICIARY_CATEGORY_NAME) || !skipIdentityCredentials) {
                    PersonCategory beneficiaryPersonCategory = (PersonCategory) category;
                    Person beneficiary = Person.getPersonFromPersonCategory(beneficiaryPersonCategory);
                    credentialsOptional = getIdentityCredentials(credentialStateActivePending, beneficiary.getDocumentNumber(),
                            CredentialCategoriesCodes.IDENTITY.getCode(), creditHolder.getDocumentNumber());
                    if (categoryName.equals(BENEFICIARY_CATEGORY_NAME)) {
                        beneficiaryAddress = beneficiaryPersonCategory.getAddress();
                    }
                }

                if (skipIdentityCredentials) {
                    continue;
                }
            } else if (categoryName.equals(DWELLING_CATEGORY_NAME)) {
                if (Objects.isNull(category.isModification()) && !pdfValidation)
                    throw new FileManagerException("No se encuentra la pregunta de vivienda.");
                if ((Objects.isNull(category.isModification()) && pdfValidation))
                    credentialsOptional = getDwellingCredentials(credentialStateActivePending,
                            creditHolder.getDocumentNumber(), beneficiaryAddress);
                if (!Objects.isNull(category.isModification()) &&
                        (Boolean.TRUE.equals(category.isModification()) || pdfValidation))
                    credentialsOptional = getDwellingCredentials(credentialStateActivePending,
                            creditHolder.getDocumentNumber(), beneficiaryAddress);
            } else if (categoryName.equals(ENTREPRENEURSHIP_CATEGORY_NAME) &&
                    Objects.isNull(category.isModification()) && !pdfValidation) {
                throw new FileManagerException("No se encuentra la pregunta de emprendimiento.");
            }

            if (!credentialsOptional.isEmpty()) {
                log.info(bodyLog(credentialsOptional.get(0)));
                processExcelFileResult.addRowError(
                        ExcelErrorDetail.builder()
                                .errorHeader("Advertencia CREDENCIAL DUPLICADA")
                                .errorBody(bodyLog(credentialsOptional.get(0)))
                                .errorType(DUPLICATED_CREDENTIAL)
                                .credentialId(credentialsOptional.get(0).getId())
                                .category(credentialsOptional.get(0).getCredentialDescription())
                                .documentNumber(credentialsOptional.get(0).getBeneficiaryDni())
                                .name(credentialsOptional.get(0).getBeneficiaryFirstName())
                                .lastName(credentialsOptional.get(0).getBeneficiaryLastName())
                                .build()
                );

                allCredentialsNewOrInactive = false;
            }
        }
        return allCredentialsNewOrInactive;
    }

    private String bodyLog(Credential credential) {
        return "Existe al menos una credencial de tipo " + credential.getCredentialCategory() +
                " en estado " + credential.getCredentialState().getStateName() +
                " para el DNI " + credential.getBeneficiary().getDocumentNumber() + " si desea continuar debe revocarlas manualmente";
    }


    private List<Credential> getIdentityCredentials(List<CredentialState> credentialStateActivePending, Long beneficiaryDni, String credentialCategoryCode, Long creditHolderDni) {

        if (creditHolderDni != null) {
            return credentialRepository.findByBeneficiaryDniAndCredentialCategoryAndCreditHolderDniAndCredentialStateIn(
                    beneficiaryDni,
                    credentialCategoryCode,
                    creditHolderDni,
                    credentialStateActivePending
            );
        } else {
            return credentialRepository.findByBeneficiaryDniAndCredentialCategoryAndCredentialStateIn(
                    beneficiaryDni,
                    credentialCategoryCode,
                    credentialStateActivePending
            );
        }
    }

    private List<Credential> getDwellingCredentials(List<CredentialState> credentialStateActivePending, Long creditHolderDni, String dwellingAddress) {
        return credentialDwellingRepository.findByCreditHolderDniAndAddressAndCredentialStateIn(
                creditHolderDni,
                dwellingAddress,
                credentialStateActivePending
        );
    }

    /* TODO
        Al igual que el metodo de buildAllCredentialsFromForm, analizar la posibilidad de reutilizar estos metodos para aligerar la carga sobre
        el metodo importCredentials, ya que las validaciones de este metodo se estan realizando en el mismo metodo.
     */
    private void saveAllCredentialsFromForm(SurveyForm surveyForm, boolean skipIdentityCredentials) throws CredentialException, FileManagerException {
        //1-get creditHolder Data
        PersonCategory creditHolderPersonCategory = (PersonCategory) surveyForm.getCategoryByUniqueName(BENEFICIARY_CATEGORY_NAME.getCode(), null);
        Person creditHolder = Person.getPersonFromPersonCategory(creditHolderPersonCategory);

        //1-get all data from form
        List<Category> categoryArrayList = surveyForm.getAllCompletedCategories();

        //4-Now working with each beneficiary
        for (Category category : categoryArrayList) {
            if (!skipIdentityCredentials || !this.isIdentityCategoryAndHasIdentityCredential(creditHolder , category)) {
                saveCredential(category, creditHolder, surveyForm);
            }
        }
    }

    private boolean isIdentityCategoryAndHasIdentityCredential(Person creditHolder, Category category) {
        if(!this.isIdentityCategory(category)) {
            return false;
        } else {
            return this.hasIdentityCredential(creditHolder, (PersonCategory) category);
        }
    }

    private boolean isIdentityCategory(Category category) {
        return (category.getCategoryName().equals(BENEFICIARY_CATEGORY_NAME) || category.getCategoryName().equals(CHILD_CATEGORY_NAME)
                || category.getCategoryName().equals(SPOUSE_CATEGORY_NAME) || category.getCategoryName().equals(KINSMAN_CATEGORY_NAME));
    }

    private boolean hasIdentityCredential(Person creditHolder, PersonCategory beneficiaryPersonCategory) {
        Person beneficiary = Person.getPersonFromPersonCategory(beneficiaryPersonCategory);
        credentialStateActivePending = this.getCredentialStateActivePending();
        List<CredentialIdentity> credentials = credentialIdentityService.findByCreditHolderDniAndBeneficiaryDniAndCredentialStateIn(creditHolder.getDocumentNumber(), beneficiary.getDocumentNumber(), credentialStateActivePending);
        return credentials.isEmpty();
    }

    private List<CredentialState> getCredentialStateActivePending() {
        List<String> statesCodesToFind = new ArrayList<>();
        statesCodesToFind.add(CredentialStatesCodes.PENDING_DIDI.getCode());
        statesCodesToFind.add(CredentialStatesCodes.CREDENTIAL_ACTIVE.getCode());
        statesCodesToFind.add(CredentialStatesCodes.HOLDER_ACTIVE_KINSMAN_PENDING.getCode());
        return credentialStateRepository.findByStateNameIn(statesCodesToFind);
    }

    /* TODO
        Al igual que el metodo de buildAllCredentialsFromForm, analizar la posibilidad de reutilizar estos metodos para aligerar la carga sobre
        el metodo importCredentials, ya que las validaciones de este metodo se estan realizando en el mismo metodo.
     */
    private void saveCredential(Category category, Person creditHolder, SurveyForm surveyForm) throws CredentialException, FileManagerException {
        log.info("  saveCredential: " + category.getCategoryName());
        switch (category.getCategoryName()) {
            case BENEFICIARY_CATEGORY_NAME:
                credentialIdentityService.save(buildIdentityCredential(category, creditHolder));
                break;
            case CHILD_CATEGORY_NAME:
            case SPOUSE_CATEGORY_NAME:
            case KINSMAN_CATEGORY_NAME:
                CredentialIdentity credentialIdentity = credentialIdentityService.save(buildIdentityCredential(category,
                        creditHolder));
                this.createCredentialIdentityKinsman(credentialIdentity);
                break;
            case ENTREPRENEURSHIP_CATEGORY_NAME:
                EntrepreneurshipCategory entrepreneurshipCategory = (EntrepreneurshipCategory) category;
                if (Objects.isNull(entrepreneurshipCategory.isModification()))
                    throw new FileManagerException("No se encuentra la pregunta de emprendimiento.");
                if (Boolean.TRUE.equals(entrepreneurshipCategory.isModification())) {
                    credentialEntrepreneurshipRepository.save(buildEntrepreneurshipCredential(category, creditHolder));
                }
                break;
            case DWELLING_CATEGORY_NAME:
                DwellingCategory dwellingCategory = (DwellingCategory) category;
                if (Objects.isNull(dwellingCategory.isModification()))
                    throw new FileManagerException("No se encuentra la pregunta de vivienda.");
                if (Boolean.TRUE.equals(dwellingCategory.isModification())) {
                    PersonCategory beneficiaryCategory = (PersonCategory) surveyForm.getCategoryByUniqueName(
                            BENEFICIARY_CATEGORY_NAME.getCode(), null);
                    credentialDwellingRepository.save(buildDwellingCredential(category, creditHolder, beneficiaryCategory));
                }
                break;
            default:
                break;
        }
    }


    private Person savePersonIfNew(Person person) {
        Optional<Person> personOptional = personRepository.findByDocumentNumber(person.getDocumentNumber());
        if (personOptional.isEmpty())
            return personRepository.saveAndFlush(person);
        if (!(person.equalsIgnoreId(person, personOptional.get()))) {
            person.setId(personOptional.get().getId());
            return personRepository.saveAndFlush(person);
        }
        return personOptional.get();
    }

    //todo move into credential class
    private void buildCredential(Person creditHolder, Credential credential) {
        credential.setDateOfIssue(DateUtil.getLocalDateTimeNow());
        credential.setCreditHolder(creditHolder);
        credential.setCreditHolderDni(creditHolder.getDocumentNumber());
        credential.setCreditHolderFirstName(creditHolder.getFirstName());
        credential.setCreditHolderLastName(creditHolder.getLastName());

        //the beneficiary is the same as the credit holder for all credentials but identity
        //buildIdentityCredential overwrites this value with the different members.
        credential.setBeneficiary(creditHolder);
        credential.setBeneficiaryDni(creditHolder.getDocumentNumber());
        credential.setBeneficiaryFirstName(creditHolder.getFirstName());
        credential.setBeneficiaryLastName(creditHolder.getLastName());

        Optional<CredentialState> credentialStateOptional = credentialStateRepository.findByStateName(CredentialStatesCodes.PENDING_DIDI.getCode());
        credentialStateOptional.ifPresent(credential::setCredentialState);
    }

    /*todo move into credentia{} */ //NOSONAR
    private CredentialIdentity buildIdentityCredential(Category category, Person creditHolder)  {
        PersonCategory beneficiaryPersonCategory = (PersonCategory) category;
        Person beneficiary = Person.getPersonFromPersonCategory(beneficiaryPersonCategory);
        beneficiary = savePersonIfNew(beneficiary);

        CredentialIdentity credentialIdentity = new CredentialIdentity();
        buildCredential(creditHolder, credentialIdentity);

        credentialIdentity.setBeneficiary(beneficiary);
        credentialIdentity.setBeneficiaryDni(beneficiary.getDocumentNumber());
        credentialIdentity.setBeneficiaryFirstName(beneficiary.getFirstName());
        credentialIdentity.setBeneficiaryLastName(beneficiary.getLastName());

        credentialIdentity.setCredentialCategory(CredentialCategoriesCodes.IDENTITY.getCode());


        credentialIdentity.setBeneficiaryGender(beneficiary.getGender());
        credentialIdentity.setBeneficiaryBirthDate(beneficiary.getBirthDate());

        switch (beneficiaryPersonCategory.getPersonType()) {
            case BENEFICIARY:
                credentialIdentity.setCredentialDescription(CredentialTypesCodes.CREDENTIAL_IDENTITY.getCode());
                credentialIdentity.setRelationWithCreditHolder(CredentialRelationHolderType.HOLDER.getCode());
                break;
            case SPOUSE:
            case CHILD:
            case OTHER_KINSMAN:
                credentialIdentity.setCredentialDescription(CredentialTypesCodes.CREDENTIAL_IDENTITY_FAMILY.getCode());
                credentialIdentity.setRelationWithCreditHolder(CredentialRelationHolderType.KINSMAN.getCode());
                break;
        }

        return credentialIdentity;
    }

    private void createCredentialIdentityKinsman(CredentialIdentity credentialIdentity) throws CredentialException {

        log.info("verify credential kinsman type for beneficiary {}  and holder {}", credentialIdentity.getBeneficiaryDni(),
                credentialIdentity.getCreditHolderDni());

        Optional<DidiAppUser> didiAppUser = didiAppUserService.getDidiAppUserByDni(credentialIdentity.getBeneficiaryDni());

            if(Boolean.FALSE.equals(!this.credentialIdentityService.existsCredentialIdentityActivesOrPendingDidiForBeneficiaryDniAsFamilyAndTypeKinsman
                    (credentialIdentity.getCreditHolderDni(), credentialIdentity.getBeneficiaryDni())) && didiAppUser.isPresent()) {
                CredentialIdentity newCredentialidentityAsKinsmanType = this.credentialIdentityService.buildNewOnPendidgDidiAsKinsmanType(credentialIdentity, didiAppUser.get());
                this.credentialIdentityService.save(newCredentialidentityAsKinsmanType);
            }
    }

    //todo move into credential type class
    private CredentialEntrepreneurship buildEntrepreneurshipCredential(Category category, Person creditHolder) {
        EntrepreneurshipCategory entrepreneurshipCategory = (EntrepreneurshipCategory) category;

        CredentialEntrepreneurship credentialEntrepreneurship = new CredentialEntrepreneurship();
        buildCredential(creditHolder, credentialEntrepreneurship);
        credentialEntrepreneurship.setEntrepreneurshipType(entrepreneurshipCategory.getType());
        credentialEntrepreneurship.setStartActivity((entrepreneurshipCategory.getActivityStartDate() != null ? entrepreneurshipCategory.getActivityStartDate() : null));
        credentialEntrepreneurship.setMainActivity(entrepreneurshipCategory.getMainActivity());
        credentialEntrepreneurship.setEntrepreneurshipName(entrepreneurshipCategory.getName());
        credentialEntrepreneurship.setEntrepreneurshipAddress(entrepreneurshipCategory.getAddress());
        credentialEntrepreneurship.setEndActivity(entrepreneurshipCategory.getActivityEndingDate());

        credentialEntrepreneurship.setCredentialCategory(CredentialCategoriesCodes.ENTREPRENEURSHIP.getCode());
        credentialEntrepreneurship.setCredentialDescription(CredentialCategoriesCodes.ENTREPRENEURSHIP.getCode());

        return credentialEntrepreneurship;
    }

    //todo move into credential type class
    private CredentialDwelling buildDwellingCredential(Category category, Person creditHolder, PersonCategory beneficiary) {
        DwellingCategory entrepreneurshipCategory = (DwellingCategory) category;

        CredentialDwelling credentialDwelling = new CredentialDwelling();
        buildCredential(creditHolder, credentialDwelling);

        credentialDwelling.setDwellingType(entrepreneurshipCategory.getDwellingType());
        credentialDwelling.setDwellingAddress(entrepreneurshipCategory.getDistrict());
        credentialDwelling.setPossessionType(entrepreneurshipCategory.getHoldingType());
        credentialDwelling.setBrick(entrepreneurshipCategory.getBrick());
        credentialDwelling.setLock(entrepreneurshipCategory.getLock());
        credentialDwelling.setWood(entrepreneurshipCategory.getWood());
        credentialDwelling.setPaperBoard(entrepreneurshipCategory.getPaperBoard());
        credentialDwelling.setLightInstallation(entrepreneurshipCategory.getLightInstallation());
        credentialDwelling.setGeneralConditions(entrepreneurshipCategory.getGeneralConditions());
        credentialDwelling.setNeighborhoodType(entrepreneurshipCategory.getNeighborhoodType());
        credentialDwelling.setGas(entrepreneurshipCategory.getGas());
        credentialDwelling.setCarafe(entrepreneurshipCategory.getCarafe());
        credentialDwelling.setWater(entrepreneurshipCategory.getWater());
        credentialDwelling.setWatterWell(entrepreneurshipCategory.getWatterWell());
        credentialDwelling.setAntiquity(entrepreneurshipCategory.getAntiquity());
        credentialDwelling.setNumberOfEnvironments(entrepreneurshipCategory.getNumberOfEnvironments());
        credentialDwelling.setRental(entrepreneurshipCategory.getRental());

        credentialDwelling.setAddress(beneficiary.getAddress());
        credentialDwelling.setLocation(beneficiary.getLocation());
        credentialDwelling.setNeighborhood(beneficiary.getNeighborhood());

        credentialDwelling.setCredentialCategory(CredentialCategoriesCodes.DWELLING.getCode());
        credentialDwelling.setCredentialDescription(CredentialCategoriesCodes.DWELLING.getCode());

        return credentialDwelling;
    }

    public Boolean isLoanValidForCredentialCredit(Loan loan) {
        return loan.getState().equals(LoanStateCodes.OK.getCode()) && loan.getStatus().equals(LoanStatusCodes.ACTIVE.getCode());
    }


    public CredentialCredit buildCreditCredential(Loan loan, Person holder, CredentialCredit credentialCreditPrev) throws CredentialException {
        CredentialCredit credentialCredit = new CredentialCredit();

        Optional<ParameterConfiguration> config = parameterConfigurationRepository.findByConfigurationName(ConfigurationCodes.ID_DIDI_ISSUER.getCode());
        if (config.isPresent()) {
            credentialCredit.setIdDidiIssuer(config.get().getValue());
        } else {
            //
            throw new CredentialException("Id Didi Issuer Not exists, cant build credential");
        }

        this.setLoanInfoToCredential(credentialCredit,loan);

        credentialCredit.setBeneficiary(holder);

        credentialCredit.setCreditHolder(holder);

        //Credential Parent fields
        credentialCredit.setDateOfIssue(DateUtil.getLocalDateTimeNow());


        //The ID Didi Receptor leave blank for emmit step

        //Person do not have a DID yet -> set as pending didi
        CredentialState statePendindDidi = credentialStateService.getCredentialPendingDidiState();
        credentialCredit.setCredentialState(statePendindDidi);


        //This depends of the type of loan from bondarea
        credentialCredit.setCredentialDescription(CredentialTypesCodes.CREDENTIAL_CREDIT.getCode());
        credentialCredit.setCredentialCategory(CredentialCategoriesCodes.CREDIT.getCode());// TODO this column will be no longer useful

        if(credentialCreditPrev!=null){
            credentialCredit.setIdHistorical(credentialCreditPrev.getIdHistorical());
        }

        return credentialCredit;
    }

    private CredentialCredit setLoanInfoToCredential(CredentialCredit credentialCredit, Loan loan){

        credentialCredit.setIdBondareaCredit(loan.getIdBondareaLoan());
        /* TODO we need the type from bondarea - credentialCredit.setCreditType();*/ //NOSONAR
        credentialCredit.setCreditType(loan.getTagBondareaLoan());
        credentialCredit.setIdGroup(loan.getIdGroup());
        credentialCredit.setCurrentCycle(loan.getCycleDescription()); // si cambia, se tomara como cambio de ciclo
        credentialCredit.setCurrentCycleNumber(loan.getCurrentInstalmentNumber());
        credentialCredit.setExpirationDate(loan.getExpirationDate());
        credentialCredit.setTotalCycles(loan.getInstalmentTotalQuantity());

        credentialCredit.setAmountExpiredCycles(0);
        credentialCredit.setCreditStatus(loan.getStatus());
        credentialCredit.setCreditState(loan.getState());
        credentialCredit.setExpiredAmount(loan.getExpiredAmount());
        credentialCredit.setCreationDate(loan.getCreationDate());
        credentialCredit.setAmount(loan.getAmount());
        credentialCredit.setStartDate(loan.getDateFirstInstalment());

        return credentialCredit;

    }

    private Boolean credentialCreditMustBeenUpdate(CredentialCredit credentialCredit, Loan loan){

        boolean areEquals;

        areEquals = credentialCredit.getIdBondareaCredit().equals(loan.getIdBondareaLoan());
        areEquals = areEquals && credentialCredit.getIdGroup().equals(loan.getIdGroup());
        areEquals = areEquals && credentialCredit.getCurrentCycle().equals(loan.getCycleDescription());
        if (Objects.nonNull(credentialCredit.getCreditStatus()))
            areEquals = areEquals && credentialCredit.getCreditStatus().equals(loan.getStatus());
        areEquals = areEquals && credentialCredit.getExpiredAmount().equals(loan.getExpiredAmount());
        areEquals = areEquals && credentialCredit.getCreationDate().equals(loan.getCreationDate());
        areEquals = areEquals && !credentialCredit.getCurrentCycleNumber().equals(loan.getCurrentInstalmentNumber());
        areEquals = areEquals && credentialCredit.getBeneficiaryDni().equals(loan.getDniPerson());
        return areEquals;
    }


    /**
     * Set holder in default and
     * Credit Credential
     * If exists and is emitted, revoke complete, create credential on default
     * If exists and is Pending Didi, revoke only local
     * If exists and is revoked, do nothing
     * If not exists, do nothing
     *
     * @param loan
     * @param holder
     * @return
     */
    public boolean revokeCredentialCredit(Loan loan, Person holder) throws CredentialException {

        log.info(String.format("Revoking Credentials credit for Loan %s and holder %d", loan.getIdBondareaLoan(), holder.getDocumentNumber()));

        holder.addLoanInDefault(loan);
        personRepository.save(holder);

        Optional<CredentialCredit> opCredit = credentialCreditRepository.findFirstByIdBondareaCreditOrderByDateOfIssueDesc(loan.getIdBondareaLoan());

        if (opCredit.isPresent()) {
            CredentialCredit credit = opCredit.get();

            //If exists and is revoked, do nothing, if it is default cred do nth
            if (credit.getCredentialState().getStateName().equals(CredentialStatesCodes.CREDENTIAL_REVOKE.getCode()) || credit.getCreditState().equals(LoanStateCodes.DEFAULT.getCode())) {
                log.info(String.format("Credential credit %d is revoked and in default, no need to update, credit %s for holder %d ", credit.getId(), credit.getIdBondareaCredit(), holder.getDocumentNumber()));
                return true;
            }

            //if credit is in default, revoke.
            if (loan.getState().equals(LoanStateCodes.DEFAULT.getCode())) {
                if (this.revokeDefaultCredentialCredit(credit)) {
                    // create new default credential credit
                    CredentialCredit defaultCredentialCredit = this.buildCreditCredential(loan, holder, credit);
                    credentialCreditRepository.save(defaultCredentialCredit);
                    log.info(String.format("The credential for loan %s has been revoked for default successfully", credit.getIdBondareaCredit()));
                    log.info("A new credential in Default state has been created: id["+defaultCredentialCredit.getId()+"]");
                    return true;
                } else
                    log.error(String.format("The credential for loan %s was not set to default", credit.getIdBondareaCredit()));
                return false;
            }

        } else {//If not exists, do nothing
            log.info(String.format("No revoke Credentials credit for Loan %s not exists", loan.getIdBondareaLoan()));
        }

        return false;
    }


    /**
     * If exists and is emitted, revoke complete
     * If exists and is Pendiing Didi, revoke only local
     *
     * @param creditToRevoke
     * @return
     * @throws CredentialException
     */
    private boolean revokeDefaultCredentialCredit(CredentialCredit creditToRevoke) throws CredentialException {
        //revoke the previous credit and save the historic id
        log.info(String.format("Set Credential is in default for %s dni %d", creditToRevoke.getIdBondareaCredit(), creditToRevoke.getCreditHolderDni()));

        return this.revokeComplete(creditToRevoke, RevocationReasonsCodes.DEFAULT.getCode());

    }

    private boolean newRevokeCredentials(Credential credentialToRevoke, String reasonCode, CredentialTypesCodes credentialType) {
        List<CredentialState> activePendingStates = credentialStateRepository.findByStateNameIn
                (List.of(CredentialStatesCodes.CREDENTIAL_ACTIVE.getCode(), CredentialStatesCodes.PENDING_DIDI.getCode()));

        boolean haveRevokeOk = true;

        switch (credentialType){
            case CREDENTIAL_IDENTITY:
                //find all the identities that the dni of the holder is into. (with state active or pending)
                List<CredentialIdentity> holderIdentities = credentialIdentityService.findByCreditHolderDniAndCredentialStateIn
                        (credentialToRevoke.getCreditHolderDni(), activePendingStates);

                return revokeListOfCredentials(holderIdentities, reasonCode, new Credential(), credentialType);

            case CREDENTIAL_IDENTITY_FAMILY:
                //revoke the identities of the familiar: the one created by the survey and if it exists, the one created because the person download the app. (with state active or pending)
                List<CredentialIdentity> familiarIdentities = credentialIdentityService.findByCreditHolderDniAndBeneficiaryDniAndCredentialStateIn(credentialToRevoke.getCreditHolderDni(),
                        credentialToRevoke.getBeneficiaryDni(), activePendingStates);

                return revokeListOfCredentials(familiarIdentities, reasonCode, new Credential(), credentialType);

            case CREDENTIAL_BENEFITS:
                //revoke benefit if the holder does not have another credit(active or pending, and did not finish) and also revoke benefits family .
                List<CredentialCredit> creditsActivePending = credentialCreditRepository.findByCreditHolderDniAndCredentialStateIn(credentialToRevoke.getCreditHolderDni(), activePendingStates);
                if (creditsActivePending.isEmpty() && this.revokeComplete(credentialToRevoke, reasonCode)) {
                    //get the familiar benefits in which the holder is within
                    List<CredentialBenefits> familiarBenefits = credentialBenefitsRepository.findByCreditHolderDniAndCredentialStateInAndBeneficiaryType(credentialToRevoke.getCreditHolderDni(), activePendingStates, PersonTypesCodes.FAMILY.getCode());

                    return revokeListOfCredentials(familiarBenefits, reasonCode, new CredentialBenefits(), credentialType);
                }

                log.info("Impossible to revoke credential benefit. There are credits in state active or pending.");
                return false;

            case CREDENTIAL_CREDIT:
                //get the credit credential to get the group
                Optional<CredentialCredit> credentialCredit = credentialCreditRepository.findById(credentialToRevoke.getId());
                if (credentialCredit.isPresent()) {
                    //get the group that is not revoked
                    List<CredentialCredit> creditsGroup = this.getCreditGroup(credentialCredit.get().getIdGroup());
                    //for each holder credit -> revoke credit -> revoke benefits -> revoke familiar benefits
                    for (CredentialCredit credit : creditsGroup) {
                        haveRevokeOk = this.revokeComplete(credit, reasonCode); //todo validate successful revocation to continue

                        //get benefits with holder dni (holder benefits and familiar benefits)
                        List<CredentialBenefits> benefits = credentialBenefitsRepository.findByCreditHolderDniAndCredentialStateIn(credit.getCreditHolderDni(), activePendingStates);

                        if (benefits.isEmpty()) {
                            log.info("For the dni holder: " + credit.getCreditHolderDni() + " there is no benefits in active nor pending");
                        } else {
                            haveRevokeOk = revokeListOfCredentials(benefits, reasonCode, new CredentialBenefits(), credentialType);
                        }
                    }
                    return haveRevokeOk;
                }

                log.error("Error you are trying to revoke a credit that no longer exist, id: " + credentialToRevoke.getId());
                return false;

            default:
                return false;
        }
    }

    private <T> boolean revokeListOfCredentials(List<T> list, String reason, Credential toCast, CredentialTypesCodes credentialType){
        boolean processSuccess = false;

        if(list.isEmpty()){
            log.info("There is no credential type " + credentialType.getCode() + " to revoke! " +
                    "The credentials are not in state pending or active");
            throw new CredentialNotExistsException("No se encuentran credenciales en estado pendiente o activo para revocar.");
        }

        for (T castTest : list){
            processSuccess = this.revokeComplete(toCast.getClass().cast(castTest), reason);
        }

        return processSuccess;
    }

    //TODO all of the methods of revocation, could be separated in a special service

    /**
     * Revocation with the business logic.
     * For particular revocations use, this.revokeComplete()
     *
     * @param id
     * @return
     */
    public boolean revokeCredential(Long id, String reasonCode, boolean revokeOnlyThisCredential) throws CredentialException {
        CredentialTypesCodes credentialType;

        log.info("Filtering credential with id: " + id);
        Optional<Credential> opCredentialToRevoke = getCredentialById(id);
        if (opCredentialToRevoke.isPresent()){
            Credential credentialToRevoke = opCredentialToRevoke.get();
            credentialType = CredentialUtils.getCredentialCode(credentialToRevoke);

            log.info("credential type of : " + credentialType.getCode());
            if(credentialType == CREDENTIAL_DWELLING || credentialType == CREDENTIAL_ENTREPRENEURSHIP || credentialType == CREDENTIAL_BENEFITS_FAMILY
                    || ((credentialType == CREDENTIAL_IDENTITY || credentialType == CREDENTIAL_IDENTITY_FAMILY) && revokeOnlyThisCredential)){
                return this.revokeComplete(credentialToRevoke, reasonCode);
            }else{
                return this.newRevokeCredentials(credentialToRevoke, reasonCode, credentialType);
            }
        }else{
            log.error("Error you are trying to revoke a non existent credential with id {}.",id);
            throw new CredentialNotExistsException("Error, no se localiza la credencial indicada a revocar.");
        }
    }

    public ResponseEntity<String> revokeCredentialProcess(Long idReason, Long idCredential, RevokeRequestDto revokeDto){
        Optional<String> opRevocationReason = getReasonFromId(idReason);
        if (opRevocationReason.isPresent()) {
            Optional<Credential> credentialToRevoke = getCredentialById(idCredential);
            if (credentialToRevoke.isPresent()) {
                if (credentialToRevoke.get().isManuallyRevocable()) {
                    return revokeProcessState(opRevocationReason.get(), idCredential, revokeDto);
                } else
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Credential with id: " + idCredential + " is not manually revocable");
            } else {
                log.error("There is no credential with id: " + idCredential);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("There is no credential with id: " + idCredential);
            }
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Is not possible to revoke with reason " + idReason);
    }

    private ResponseEntity<String> revokeProcessState(String reason, Long idCredential, RevokeRequestDto revokeDto){
        ResponseEntity<String> successMSG = ResponseEntity.status(HttpStatus.OK).body("Revoked successfully");
        ResponseEntity<String> failedMSG =ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error trying to revoke credential with id: " + idCredential);

        //possibilities -> Emprendimiento, Vivienda, identididad familiar, identidad titular (only the last one have a business logic, the others only revoke itself)
        try {
            return revokeCredential(idCredential, reason, revokeDto.getRevokeOnlyThisCredential()) ? successMSG : failedMSG;
        } catch (CredentialException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error trying to revoke credential with id: " + idCredential);
        }
    }

    /**
     * Get the credit group with the idGroup as long as the credits are not revoked.
     *
     * @param idGroup
     * @return List<CredentialCredit>
     */
    private List<CredentialCredit> getCreditGroup(String idGroup) {
        List<CredentialState> activePendingStates = credentialStateRepository.findByStateNameIn(List.of(CredentialStatesCodes.CREDENTIAL_ACTIVE.getCode(), CredentialStatesCodes.PENDING_DIDI.getCode()));
        //get the group if it is not revoked
        return credentialCreditRepository.findByIdGroupAndCredentialStateIn(idGroup, activePendingStates);
    }

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

        log.info("Starting complete revoking process for credential id: " + credentialToRevoke.getId() + " | credential type: " + credentialToRevoke.getCredentialDescription() + " holder " + credentialToRevoke.getCreditHolderDni() + " beneficiary " + credentialToRevoke.getBeneficiaryDni());
        //revoke on didi if credential was emitted
        if (Boolean.TRUE.equals(credentialToRevoke.isEmitted())) {
            if (didiService.didiDeleteCertificate(credentialToRevoke.getIdDidiCredential(), reasonCode)) {
                // if didi fail the credential need to know that is needed to be revoked (here think in the best resolution).
                // if this revoke came from the revocation business we will need to throw an error to rollback any change done before.
                return this.revokeCredentialOnlyOnSemillas(credentialToRevoke, reasonCode);
            } else {
                String message = String.format("Error to delete Certificate %d on Didi ", credentialToRevoke.getId());
                log.error(message);
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
        log.info("Revoking the credential " + credentialToRevoke.getId() + " with reason " + reasonCode);
        boolean haveRevoke = true;

        Optional<RevocationReason> reason = revocationReasonRepository.findByReason(reasonCode);
        if (reason.isPresent()) {
            //validate if the credential is in db
            Optional<Credential> opCredential = this.getCredentialById(credentialToRevoke.getId());
            if (opCredential.isEmpty()) {
                haveRevoke = false;
                log.error(String.format("The credential with id: %d is not in the database", credentialToRevoke.getId()));
            } else {

                Credential credential = opCredential.get();

                //revoke if the credential is not revoked yet
                if (Boolean.TRUE.equals(this.isCredentialRevoked(credential))) {
                    log.info(String.format("The credential %d has already been revoked", credential.getId()));
                    haveRevoke = false;
                } else {
                    //revoke
                    Optional<CredentialState> opStateRevoke = credentialStateService.getCredentialRevokeState();
                    credentialToRevoke.setCredentialState(opStateRevoke.orElse(new CredentialState()));
                    credentialToRevoke.setRevocationReason(reason.orElse(new RevocationReason()));
                    credentialToRevoke.setDateOfRevocation(DateUtil.getLocalDateTimeNow());
                    credentialRepository.save(credentialToRevoke);
                    log.info(String.format("Credential with id %d has been revoked!", credentialToRevoke.getId())); //then append also the reason
                }

            }

        } else {
            String message = String.format("Can't find reason with code: %s ", reasonCode);
            log.error(message);
            throw new CredentialException(message);
        }

        return haveRevoke;
    }

    public void revokeDefaultCredentialsForLoan(Loan loan){
        Optional<CredentialCredit> credentialCredit = credentialCreditService.getCredentialCreditsForLoan(loan.getIdBondareaLoan(), LoanStateCodes.DEFAULT).stream().findFirst();
        credentialCredit.ifPresent(cred -> this.revokeComplete(cred, RevocationReasonsCodes.UPDATE_INTERNAL));
    }

    public Optional<CredentialCredit> getCredentialCreditForLoan(Loan loan) {
        return credentialCreditRepository.findFirstByIdBondareaCreditOrderByDateOfIssueDesc(loan.getIdBondareaLoan());
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

    /**
     *
     * @param file
     * @param createCredentials
     * @return String con los resultads del procesamiento del excel
     * @throws IOException
     */
    @Transactional
    public ProcessExcelFileResult importCredentials(MultipartFile file, boolean createCredentials) throws IOException, FileManagerException {

        XSSFWorkbook workbook = new XSSFWorkbook(file.getInputStream());

        List<Form> formList = ExcelUtils.parseKoboSurveyIntoList(workbook.getSheetAt(0), Form.class);
        ExcelUtils.validateFormData(formList);
        List<Child> childList = ExcelUtils.parseKoboSurveyIntoList(workbook.getSheet("grupo_hijos"), Child.class);
        List<FamilyMember> familyMemberList = ExcelUtils.parseKoboSurveyIntoList(workbook.getSheet("grupo_datos_miembro"), FamilyMember.class);

        ProcessExcelFileResult processExcelFileResult = new ProcessExcelFileResult();

        for (Form form: formList) {
            if (form.getEstadoEncuesta() == null) {
                //Validaciones
                if (!createCredentials) {
                    processExcelFileResult = verifyDuplicateCredentials(form, childList, familyMemberList);
                }else{
                    if (processExcelFileResult.getErrorRows().isEmpty()) {
                        createCredentialsFromForm(form, childList, familyMemberList);
                    } else {
                        regenerateCredentials(form);
                    }
                }
            }
        }//for
        return processExcelFileResult;
    }

    private void createCredentialsFromForm (Form form, List<Child> childList, List<FamilyMember> familyMemberList) throws FileManagerException {

        // Creamos las listas que contendran todas las credenciales del Excel, de todas las personas encuestadas y sus familiares.
        List<CredentialIdentity> idCredentials = new ArrayList<>();
        List<CredentialDwelling> dwellingCredentials = new ArrayList<>();
        List<CredentialEntrepreneurship> entrepreneurshipCredentials = new ArrayList<>();
            if (form.getEstadoEncuesta() == null) {
                // Generamos las correspondientes credenciales de identidad de las personas de la encuesta.
                idCredentials.addAll(buildAllCredentialsIdentityFromForm(form,
                        childList.stream().filter(child -> form.getIndex() == child.getParentIndex()).collect(Collectors.toList()),
                        familyMemberList.stream().filter(member -> form.getIndex() == member.getParentIndex()).collect(Collectors.toList())));

                // Obtenemos al titular de la encuesta, que sera necesario para la generacion de las demas credenciales.
                Person personBeneficiary = personRepository.findByDocumentNumber(form.getNumeroDniBeneficiario()).orElse(new Person());
                if (form.getHuboCambiosVivienda().equals("Si")) {
                    dwellingCredentials.add(buildDwellingCredentials(form, personBeneficiary));
                }
                if (form.getHuboCambiosActividad().equals("Si")) {
                    entrepreneurshipCredentials.add(buildEntrepreneurshipCredentials(form, personBeneficiary));
                }
            }

        if (!idCredentials.isEmpty()) {
            credentialIdentityRepository.saveAll(idCredentials);
            credentialIdentityRepository.flush();
        }
        if (!dwellingCredentials.isEmpty()) {
            credentialDwellingRepository.saveAll(dwellingCredentials);
            credentialDwellingRepository.flush();
        }
        if (!entrepreneurshipCredentials.isEmpty()) {
            credentialEntrepreneurshipRepository.saveAll(entrepreneurshipCredentials);
            credentialEntrepreneurshipRepository.flush();

        }
    }

    private void regenerateCredentials(Form form){
        Person personBeneficiary = new PersonBuilder().fromForm(form).build();
        if (form.getHuboCambiosActividad().equals("Si")) this.buildEntrepreneurshipCredentials(form, personBeneficiary);
        if (form.getHuboCambiosVivienda().equals("Si")) this.buildDwellingCredentials(form, personBeneficiary);
    }

    @Transactional
    public List<CredentialIdentity> buildAllCredentialsIdentityFromForm(Form form, List<Child> childList, List<FamilyMember> familyMemberList) throws CredentialException, FileManagerException {
        log.info("buildAllCredentialsFromForm: " + form.getNombreBeneficiario().concat(" ").concat(form.getApellidoBeneficiario()));
        // 0- Genero la lista temporal que retornara el metodo una vez terminado el proceso.
        List<CredentialIdentity> idCredentials = new ArrayList<>();

        //1. Creamos al titular del proceso y su correspondiente credencial de identidad.
        Person mainBeneficiary = savePersonIfNew(new PersonBuilder().fromForm(form).build());
        if(!validateIdentityCredential(mainBeneficiary)) idCredentials.add(generateIdentityCredential(mainBeneficiary, mainBeneficiary, BENEFICIARY));

        //2. Creamos al conyuge del Titular, con su correspondiente credencial de identidad.
        if(form.getEstadoCivilBeneficiario().equals("Casado/a") || form.getEstadoCivilBeneficiario().equals("Pareja de hecho")){
            Person beneficiarySpouse = savePersonIfNew(new PersonBuilder().fromSpouse(form).build());
            if(!validateIdentityCredential(beneficiarySpouse)) idCredentials.add(generateIdentityCredential(mainBeneficiary, beneficiarySpouse, SPOUSE));
        }

        //3. Creamos al resto de las personas en la encuesta, con su correspondiente credencial de identidad.
        if(form.getTieneHijos().equals("Si")){
            idCredentials.addAll(createFamilyCredentials(childList, mainBeneficiary)); // Primero iran los hijos del titular.
        }
        if(form.getTieneMasFamilia().equals("Si")){
            idCredentials.addAll(createFamilyCredentials(familyMemberList, mainBeneficiary)); // Luego va el resto de la familia del titular.
        }

        return idCredentials;
    }

    private <T> List<CredentialIdentity> createFamilyCredentials(List<T> list, Person creditHolder){
        Person tempPerson;
        List<CredentialIdentity> idCredentials = new ArrayList<>();
        for (Object obj : list){
            switch (obj.getClass().getSimpleName()){
                case "Child":
                    tempPerson = savePersonIfNew(new PersonBuilder().fromChild((Child) obj).build());
                    if(!validateIdentityCredential(tempPerson)) idCredentials.add(generateIdentityCredential(creditHolder, tempPerson, CHILD));
                    break;

                case "FamilyMember":
                    tempPerson = savePersonIfNew(new PersonBuilder().fromFamilyMember((FamilyMember) obj).build());
                    if(!validateIdentityCredential(tempPerson)) idCredentials.add(generateIdentityCredential(creditHolder, tempPerson, OTHER_KINSMAN));
                    break;

                default:
                    break;
            }
        }
        return idCredentials;
    }

    private CredentialIdentity generateIdentityCredential(Person creditHolder, Person beneficiary, PersonType type){
        CredentialIdentity idCredential = new CredentialIdentity(beneficiary, creditHolder, type);

        Optional<CredentialState> credentialStateOptional = credentialStateRepository.findByStateName(CredentialStatesCodes.PENDING_DIDI.getCode());
        credentialStateOptional.ifPresent(idCredential::setCredentialState);

        if(creditHolder != beneficiary) idCredential = modifyKinsmanIdentityCredential(idCredential);

        return idCredential;
    }

    private CredentialIdentity modifyKinsmanIdentityCredential(CredentialIdentity idCredential){
        log.info("verify credential kinsman type for beneficiary {}  and holder {}", idCredential.getBeneficiaryDni(),
                idCredential.getCreditHolderDni());

        Optional<DidiAppUser> didiAppUser = didiAppUserService.getDidiAppUserByDni(idCredential.getBeneficiaryDni());

        if(Boolean.FALSE.equals(!this.credentialIdentityService.existsCredentialIdentityActivesOrPendingDidiForBeneficiaryDniAsFamilyAndTypeKinsman
                (idCredential.getCreditHolderDni(), idCredential.getBeneficiaryDni())) && didiAppUser.isPresent()){
            idCredential = this.credentialIdentityService.buildNewOnPendidgDidiAsKinsmanType(idCredential, didiAppUser.get());
        }

        return idCredential;
    }

    private boolean validateIdentityCredential(Person person){
        return credentialIdentityService.existsCredentialIdentityActivesOrPendingDidiForBeneficiary
                (person.getDocumentNumber(), credentialStateActivePending);
    }

    private ProcessExcelFileResult verifyDuplicateCredentials(Form form, List<Child> childList, List<FamilyMember> familyMemberList){
        List<Credential> credentialsOptional = validateCredentials(form);
        ProcessExcelFileResult processExcelFileResult = new ProcessExcelFileResult();

        credentialsOptional.addAll(validateFamilyCredentials(form, CredentialUtils.wrapList(
                childList.stream().filter(child -> form.getIndex() == child.getParentIndex()).collect(Collectors.toList()),
                familyMemberList.stream().filter(member -> form.getIndex() == member.getParentIndex()).collect(Collectors.toList()))));

        if (!credentialsOptional.isEmpty()) {
            log.info(bodyLog(credentialsOptional.get(0)));
            processExcelFileResult.addRowError(
                    ExcelErrorDetail.builder()
                            .errorHeader("Advertencia CREDENCIAL DUPLICADA")
                            .errorBody(bodyLog(credentialsOptional.get(0)))
                            .errorType(DUPLICATED_CREDENTIAL)
                            .credentialId(credentialsOptional.get(0).getId())
                            .category(credentialsOptional.get(0).getCredentialDescription())
                            .documentNumber(credentialsOptional.get(0).getBeneficiaryDni())
                            .name(credentialsOptional.get(0).getBeneficiaryFirstName())
                            .lastName(credentialsOptional.get(0).getBeneficiaryLastName())
                            .build()
            );
        }
        return processExcelFileResult;
    }

    private List<Credential> validateCredentials(Form form){
        //compruebo que el beneficiario no tenga vivienda
        List<Credential> credentialList = getDwellingCredentials(credentialStateActivePending,
                form.getNumeroDniBeneficiario(), ExcelUtils.beneficiaryAddress(form));

        // compruebo que el beneficiario no se repita
        credentialList.addAll(getIdentityCredentials(credentialStateActivePending,
                form.getNumeroDniBeneficiario(), CredentialCategoriesCodes.IDENTITY.getCode(),form.getNumeroDniBeneficiario()));

        //compruebo que el conyuge no se repita
        credentialList.addAll(getIdentityCredentials(credentialStateActivePending, form.getNumeroDniConyuge(),
                CredentialCategoriesCodes.IDENTITY.getCode(), form.getNumeroDniBeneficiario()));

        return credentialList;
    }

    private <T> List<Credential> validateFamilyCredentials(Form form, List<T> lst){
        List<Credential> credentials = new ArrayList<>();
        for (Object obj : lst){
            switch (obj.getClass().getSimpleName()){
                case "Child":
                    Child child = (Child) obj;
                    credentials.addAll(getIdentityCredentials(credentialStateActivePending,
                            child.getNumeroDocumentoHijo(), CredentialCategoriesCodes.IDENTITY.getCode(),
                            form.getNumeroDniBeneficiario()));
                    break;

                case "FamilyMember":
                    FamilyMember familyMember = (FamilyMember) obj;
                    credentials.addAll(getIdentityCredentials(credentialStateActivePending,
                            familyMember.getNumeroDniFamiliar(), CredentialCategoriesCodes.IDENTITY.getCode(),
                            form.getNumeroDniBeneficiario()));
                    break;

                default:
                    break;
            }
        }
        return credentials;
    }

    public CredentialDwelling buildDwellingCredentials(Form form, Person creditHolder){
        //DWELLING_CATEGORY_NAME
        CredentialDwelling credentialDwelling = new CredentialDwelling();
        buildCredential(creditHolder, credentialDwelling);
        completeDwellingCredentialFromForm(form, credentialDwelling);
        return credentialDwelling;
    }

    public CredentialEntrepreneurship buildEntrepreneurshipCredentials(Form form, Person creditHolder){
        //ENTREPRENEURSHIP_CATEGORY
        CredentialEntrepreneurship credentialEntrepreneurship = new CredentialEntrepreneurship();
        buildCredential(creditHolder, credentialEntrepreneurship);
        completeEntrepreneurshipCredentialFormForm(form, credentialEntrepreneurship);
        return credentialEntrepreneurship;
    }

    public void completeDwellingCredentialFromForm(Form form, CredentialDwelling credentialDwelling){
        credentialDwelling.setAntiquity(form.getViviendaAntiguedad());
        credentialDwelling.setPossessionType((form.getViviendaTipoTenencia().equals("Otro"))? form.getViviendaTipoTenenciaOtro() : form.getViviendaTipoTenencia());
        credentialDwelling.setDwellingType((form.getViviendaTipoVivienda().equals("Otro")) ? form.getViviendaTipoViviendaOtro() : form.getViviendaTipoVivienda());
        credentialDwelling.setBrick(form.getViviendaMaterialesLadrillo() == 1);
        credentialDwelling.setLock(form.getViviendaMaterialesChapa() == 1);
        credentialDwelling.setWood(form.getViviendaMaterialesMadera() == 1);
        credentialDwelling.setPaperBoard(form.getViviendaMaterialesCarton() == 1);
        credentialDwelling.setGas(form.getViviendaRedDeGas() == 1);
        credentialDwelling.setCarafe(form.getViviendaGarrafa() == 1);
        credentialDwelling.setWater(form.getViviendaRedDeAgua() == 1);
        credentialDwelling.setWatterWell(form.getViviendaBomba() == 1);
        credentialDwelling.setLightInstallation(form.getViviendaInstalacionLuz());
        credentialDwelling.setNumberOfEnvironments(form.getViviendaCantAmbientes());
        credentialDwelling.setGeneralConditions(form.getViviendaCondicionesGenerales());
        credentialDwelling.setNeighborhoodType((form.getViviendaTipoBarrio().equals("Otro")) ? form.getViviendaTipoBarrioOtro() : form.getViviendaTipoBarrio());
        credentialDwelling.setRental(form.getViviendaMontoAlquiler());
        credentialDwelling.setDwellingAddress(form.getViviendaDireccionMunicipio());
        credentialDwelling.setAddress(ExcelUtils.beneficiaryAddress(form));
        credentialDwelling.setLocation(form.getViviendaDomicilioLocalidad());
        credentialDwelling.setNeighborhood(form.getViviendaDomicilioBarrio());

        credentialDwelling.setCredentialCategory(CredentialCategoriesCodes.DWELLING.getCode());
        credentialDwelling.setCredentialDescription(CredentialCategoriesCodes.DWELLING.getCode());
    }

    public void completeEntrepreneurshipCredentialFormForm(Form form, CredentialEntrepreneurship credentialEntrepreneurship){
        credentialEntrepreneurship.setEntrepreneurshipName(form.getActividadNombre());
        credentialEntrepreneurship.setMainActivity(form.getActividadPrincipal());
        credentialEntrepreneurship.setEntrepreneurshipType((form.getActividadTipo().equals("Otro"))? form.getActividadTipoOtro(): form.getActividadTipo());
        credentialEntrepreneurship.setStartActivity(form.getActividadAntiguedad());
        credentialEntrepreneurship.setEntrepreneurshipAddress(ExcelUtils.formatAddress(form));
        credentialEntrepreneurship.setEndActivity(null);

        credentialEntrepreneurship.setCredentialCategory(CredentialCategoriesCodes.ENTREPRENEURSHIP.getCode());
        credentialEntrepreneurship.setCredentialDescription(CredentialCategoriesCodes.ENTREPRENEURSHIP.getCode());
    }
}

