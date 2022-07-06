package com.atixlabs.semillasmiddleware.excelparser.app.service;

import com.atixlabs.semillasmiddleware.app.sancor.model.SancorPolicy;
import com.atixlabs.semillasmiddleware.app.sancor.service.SancorPolicyService;
import com.atixlabs.semillasmiddleware.excelparser.dto.ExcelErrorDetail;
import com.atixlabs.semillasmiddleware.excelparser.dto.ExcelErrorType;
import com.atixlabs.semillasmiddleware.excelparser.dto.ProcessExcelFileResult;
import com.atixlabs.semillasmiddleware.excelparser.exception.InvalidRowException;
import com.atixlabs.semillasmiddleware.excelparser.service.ExcelParseService;
import com.atixlabs.semillasmiddleware.excelparser.validator.RowValidator;
import com.atixlabs.semillasmiddleware.excelparser.validatorfactory.ExcelRowValidatorFactory;
import com.atixlabs.semillasmiddleware.excelparser.app.dto.SancorPolicyRow;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class SancorSaludExcelParseService extends ExcelParseService {

    private List<SancorPolicy> sancorPolicys = new ArrayList<>();

    private SancorPolicyService sancorPolicyService;

    private ExcelRowValidatorFactory excelRowValidatorFactory;

    private RowValidator<SancorPolicyRow> rowRowValidator;

    @Autowired
    public SancorSaludExcelParseService(SancorPolicyService sancorPolicyService, ExcelRowValidatorFactory excelRowValidatorFactory){
        this.sancorPolicyService = sancorPolicyService;
        this.excelRowValidatorFactory = excelRowValidatorFactory;
        this.rowRowValidator = this.excelRowValidatorFactory.getSancorPolicyRowValidator();
    }

    @Override
    public ProcessExcelFileResult processRow(Row currentRow, boolean hasNext, ProcessExcelFileResult processExcelFileResult,
                                             boolean createCredentials, boolean skipIdentityCredentials, boolean pdfValidation) {

        try {
            log.info("Processing row " + currentRow.getRowNum());
            SancorPolicyRow sancorPolicyRow = this.parseRow(currentRow);

            if (this.checkIfRowIsEmpty(sancorPolicyRow)){
                log.debug("row {} is empty",currentRow.getRowNum());
                return processExcelFileResult;
            }
            processExcelFileResult.addTotalReadRow();
            List<String> errors =  rowRowValidator.validate(sancorPolicyRow);
            if (errors == null || errors.isEmpty()) {
                SancorPolicy sancorPolicy = this.buildSancorPolicy(sancorPolicyRow);
                this.sancorPolicys.add(sancorPolicy);
                processExcelFileResult.addTotalValidRows();
            } else {
                this.addErrors(processExcelFileResult,errors,sancorPolicyRow);
            }

        } catch (InvalidRowException e) {
            log.info(String.format("The row %s is invalid", currentRow.getRowNum()), e);
            processExcelFileResult.addRowError(ExcelErrorDetail.builder()
                    .errorHeader(String.valueOf(currentRow.getRowNum()))
                    .errorBody(e.toString())
                    .errorType(ExcelErrorType.OTHER)
                    .build()
            );
        }

        return processExcelFileResult;
    }

    private boolean checkIfRowIsEmpty(SancorPolicyRow row) {
        return row == null || row.isEmpty();
    }

    private void addErrors(ProcessExcelFileResult processExcelFileResult, List<String> errors, SancorPolicyRow sancorPolicyRow){

        for(String error : errors){
            processExcelFileResult.addRowError(ExcelErrorDetail.builder()
                    .errorHeader(String.valueOf(sancorPolicyRow.getRowNum()))
                    .errorBody(error)
                    .errorType(ExcelErrorType.OTHER)
                    .build()
            );
        }
    }

    public SancorPolicyRow parseRow(Row row) throws InvalidRowException {
        return  new SancorPolicyRow(row);
    }


    @Override
    public ProcessExcelFileResult processSingleSheetFile(String filePath, boolean createCredentials,
                                                         boolean skipIdentityCredentials,
                                                         boolean pdfValidation) throws Exception {
        log.info("Sancor Salud processSingleSheetFile");
        this.cleanSancorPolicies();
        ProcessExcelFileResult processExcelFileResult = null;
        try {
            processExcelFileResult = super.processSingleSheetFile(filePath, true,
                    false, false);
        }finally {
            this.cleanSancorPolicies();
        }
        return processExcelFileResult;

    }

    private void cleanSancorPolicies(){
        this.sancorPolicys =  new ArrayList<>();
    }


    public ProcessExcelFileResult processAndSaveSingleSheetFile(String filePath) throws Exception {

        log.info("Import and save Sancor Policies form {} ", filePath);
        this.cleanSancorPolicies();

        ProcessExcelFileResult processExcelFileResult = null;
        try {
            processExcelFileResult = super.processSingleSheetFile(filePath, true,
                    false, false);
            log.info("Sancor policies to save {}", (sancorPolicys != null ? sancorPolicys.size() : 0));

            for (SancorPolicy sancorPolicy : sancorPolicys) {
                Optional<SancorPolicy> opSancorPolicy = this.sancorPolicyService.findByCertificateClientDni(sancorPolicy.getCertificateClientDni());

                if (opSancorPolicy.isPresent()) {

                    log.debug("updating sancor policy for dni {}", opSancorPolicy.get().getCertificateClientDni());

                    sancorPolicy = opSancorPolicy.get().merge(sancorPolicy);
                }

                this.sancorPolicyService.save(sancorPolicy);
            }
        }finally {
            this.cleanSancorPolicies();
        }

        return processExcelFileResult;
    }

    public SancorPolicy buildSancorPolicy(SancorPolicyRow sancorPolicyRow){
        SancorPolicy sancorPolicy = new SancorPolicy();
        sancorPolicy.setBranchDescription(sancorPolicyRow.getBranchDescription()!= null ? sancorPolicyRow.getBranchDescription().trim() : null);
        sancorPolicy.setIdProduct(sancorPolicyRow.getProductId());
        sancorPolicy.setPolicyNumber(sancorPolicyRow.getPolicy());
        sancorPolicy.setPolicyClient(sancorPolicyRow.getPolicyClient());
        sancorPolicy.setPolicyClientName(sancorPolicyRow.getPolicyClientName());
        sancorPolicy.setCertificateNumber(sancorPolicyRow.getCertificateNumber());
        sancorPolicy.setCertificateClient(sancorPolicyRow.getCertificateClient());
        sancorPolicy.setValidityFrom(sancorPolicyRow.getValidityFrom());
        sancorPolicy.setValidityTo(sancorPolicyRow.getValidityTo());
        sancorPolicy.setCertificateClientName(sancorPolicyRow.getCertificateClientName());
        sancorPolicy.setCertificateClientAddress(sancorPolicyRow.getCertificateClientAddress());
        sancorPolicy.setCertificateClientDni(this.parseDni(sancorPolicyRow.getCertificateClient()));

        return sancorPolicy;

    }

    /**
     * expect format
     * Dnnnnnnnnnn..
     *
     * @return
     */
    public Long parseDni(String certificateClient){

        String result = certificateClient.replaceFirst("D", "").replaceFirst("^0+(?!$)", "");

        return Long.parseLong(result);
    }
}
