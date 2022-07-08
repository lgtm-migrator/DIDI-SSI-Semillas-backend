package com.atixlabs.semillasmiddleware.excelparser.app.dto;

import com.atixlabs.semillasmiddleware.excelparser.app.categories.Category;
import com.atixlabs.semillasmiddleware.excelparser.dto.ExcelErrorDetail;
import com.atixlabs.semillasmiddleware.excelparser.dto.ExcelErrorType;
import com.atixlabs.semillasmiddleware.excelparser.dto.ProcessExcelFileResult;
import com.atixlabs.semillasmiddleware.util.StringUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Slf4j
public class SurveyForm {
    @DateTimeFormat(pattern = "dd/MM/yy")
    @Temporal(TemporalType.DATE)
    private LocalDate surveyDate = null;
    private String surveyFormCode = null;
    private Long pdv = null;

    private List<Category> categoryList = new ArrayList<>();

    @Override
    public String toString() {
        return "SurveyForm{" +
                "surveyDate=" + surveyDate +
                ", surveyFormCode='" + surveyFormCode + '\'' +
                ", pdv=" + pdv +
                ", categoryList=" + categoryList.toString() +
                '}';
    }

    public SurveyForm(){
    }

    public SurveyForm(AnswerRow answerRow){
        initialize(answerRow);
    }


    public void initialize(AnswerRow answerRow){
        log.info("Initializing a new form");
        this.surveyFormCode = answerRow.getSurveyFormCode();
        this.surveyDate = answerRow.getSurveyDate();
        this.pdv = answerRow.getPdv();
    }

    public boolean isEmpty(){
        return this.pdv == null || this.surveyDate == null || this.surveyFormCode == null;
    }

    public boolean isRowFromSameForm(AnswerRow answerRow){

        if(this.isEmpty()) {
            this.initialize(answerRow);
            return true;
        }

        return this.pdv.equals(answerRow.getPdv())
                && this.surveyDate.isEqual(answerRow.getSurveyDate())
                && this.surveyFormCode.equals(answerRow.getSurveyFormCode());
    }

    public void setCategoryData(AnswerRow answerRow, ProcessExcelFileResult processExcelFileResult){
        Category category = this.getCategoryByUniqueName(answerRow.getCategory(), processExcelFileResult);
        if (category != null)
            category.loadData(answerRow, processExcelFileResult);
    }

    //OBTENGO A PARTIR DE UN STRING DE CATEGORIA LA CATEGORIA
    public Category getCategoryByUniqueName(String categoryToFind, ProcessExcelFileResult processExcelFileResult) {

        if (categoryToFind == null)
            return null;

        categoryToFind = StringUtil.toUpperCaseTrimAndRemoveAccents(categoryToFind);

        for (Category value : categoryList) {
            if (value.getCategoryUniqueName().equals(categoryToFind))
                return value;
        }
        if (processExcelFileResult!=null) {
            processExcelFileResult.addRowDebug(ExcelErrorDetail.builder()
                    .errorHeader("Categoría "+categoryToFind)
                    .errorBody("No fue definida en meta-data: será ignorada")
                    .errorType(ExcelErrorType.OTHER)
                    .build()
            );
        }
        return null;
    }

    public boolean isValid(ProcessExcelFileResult processExcelFileResult) {
        boolean allValid = true;
        String msg;

        for (Category category : categoryList) {
            if (category.isEmpty()) {
                if (category.isRequired()) {
                    allValid = false;
                    msg = "Empty and Required";
                    processExcelFileResult.addRowError(ExcelErrorDetail.builder()
                            .errorHeader(String.format("Categoría %s",category.getCategoryUniqueName()))
                            .errorBody("la categoria esta vacia o no completa y es obligatoria :"+ category.toString())
                            .errorType(ExcelErrorType.OTHER)
                            .build()
                    );
                }
                else
                    msg = "Empty but not Required";
            }
            else {
                if (!category.isValid(processExcelFileResult)) {
                    allValid = false;
                    msg = "Completed with errors";
                }
                else
                    msg = "Completed OK";
            }
            log.info("SurveyForm -> isValid: " + category.getCategoryUniqueName() + " "+msg);
        }
        return allValid;
    }

    public List<Category> getAllCompletedCategories() {
        List<Category> classArrayList = new ArrayList<>();

        for (Category category : categoryList) {
            if (!category.isEmpty())
                classArrayList.add(category);
        }
        return classArrayList;
    }

}
