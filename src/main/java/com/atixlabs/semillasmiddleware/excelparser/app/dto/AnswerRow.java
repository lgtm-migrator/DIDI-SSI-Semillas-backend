package com.atixlabs.semillasmiddleware.excelparser.app.dto;

import com.atixlabs.semillasmiddleware.excelparser.app.exception.InvalidAnswerCastException;
import com.atixlabs.semillasmiddleware.excelparser.exception.InvalidRowException;
import com.atixlabs.semillasmiddleware.excelparser.row.ExcelRow;
import com.atixlabs.semillasmiddleware.util.DateUtil;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@Getter
@Setter
@NoArgsConstructor
public class AnswerRow extends ExcelRow {

    @DateTimeFormat(pattern = "dd/MM/yy")
    @Temporal(TemporalType.DATE)
    private LocalDate surveyDate;
    private String surveyFormCode;
    private Long pdv;

    private String category;
    private String question;
    private String answer;

    public AnswerRow(Row row) throws InvalidRowException {
        super(row);
    }

    @Override
    protected void parseRow(Row row) {
            //7 formulario //9 fecha relevamiento //10 PDV
            this.surveyFormCode = getCellStringValue(row, 7, "Formulario");
            this.surveyDate = getCellStringToDate(row, 9, "Fecha de relevamiento");
            this.pdv = getCellLongValue(row, 10, "PDV");

            //14 categoria //15 pregunta //16 respuesta
            this.category = getCellStringValue(row, 14, "Categoria");
            this.question = getCellStringValue(row, 15, "Pregunta");
            this.answer = getCellWithType(row, 16, "Respuesta");
            if(this.answer != null) answer = answer.trim();
    }

    public boolean isEmpty(Row row){
        return row.getFirstCellNum() < 0;
    }

    //we only need these fields for getting data, so we validate.
    public boolean hasFormKeyValues(){
        return category != null && question != null;
    }

    public String getAnswerAsString(){
        return answer;
    }
    public Double getAnswerAsDouble() throws InvalidAnswerCastException {
        if (answer == null || answer.isBlank())
            return null;
        try { return Double.valueOf(answer);}
        catch (NumberFormatException e){
            throw new InvalidAnswerCastException(getAnswerAsString(), Constants.NUM_VALUE);
        }
    }
    public Long getAnswerAsLong() throws InvalidAnswerCastException {
        if (answer == null || answer.isBlank())
            return null;

        try {return Long.valueOf(answer);}
        catch (NumberFormatException e){
            throw new InvalidAnswerCastException(getAnswerAsString(), Constants.NUM_VALUE);
        }
    }
    public Integer getAnswerAsInteger() throws InvalidAnswerCastException {
        if (answer == null || answer.isBlank())
            return null;
        try {return Integer.valueOf(answer);}
        catch (NumberFormatException e){
            throw new InvalidAnswerCastException(getAnswerAsString(), Constants.NUM_VALUE);
        }
    }
    public LocalDate getAnswerAsDate(String datePattern) throws InvalidAnswerCastException {
        if (answer == null || answer.isBlank())
            return null;
        if(this.answer.contains("/")) {
            try {
                return LocalDate.parse(answer, DateTimeFormatter.ofPattern(datePattern));
            } catch (Exception e) {
                throw new InvalidAnswerCastException(getAnswerAsString(), "fecha");
            }
        }else{
            try {
                return LocalDate.parse(answer, DateUtil.getYearFormatDate()); //"YYYY" format with day 1 and month 1, only maters the year then
            } catch (Exception e) {
                throw new InvalidAnswerCastException(getAnswerAsString(), "fecha");
            }
        }
    }

    public LocalDate getCellStringToDate(Row row, int cellIndex, String description) {
        String dateString = null;
        String datePattern = "dd/MM/yy";
        try {
            dateString = getCellStringValue(row, cellIndex, description).replace("'", "").trim();
            return LocalDate.parse(dateString, DateTimeFormatter.ofPattern(datePattern));
        }
        catch (Exception e){
           log.error("Error :", e.getMessage());
        }
        return null;
    }

    public Object getAnswerAs(Class<?> dataType) throws InvalidAnswerCastException {
        switch (dataType.getName()) {
            case "java.lang.String":
                return getAnswerAsString();
            case "java.lang.Double":
                return getAnswerAsDouble();
            case "java.time.LocalDate":
                return getAnswerAsDate("dd/MM/yyyy");
            case "java.lang.Long":
                return getAnswerAsLong();
            case "java.lang.Integer":
                return getAnswerAsInteger();
            default:
                return null;
        }
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    @Override
    public String toString() {
        return "["+this.rowNum +"]{" +
                "surveyDate=" + surveyDate +
                ", surveyFormCode='" + surveyFormCode + '\'' +
                ", pdv=" + pdv +
                ", category='" + category + '\'' +
                ", question='" + question + '\'' +
                ", answer='" + answer + '\'' +
//                ", errorMessage='" + errorMessage + '\'' +
                ", cellIndex=" + cellIndex +
                ", cellIndexName='" + cellIndexName + '\'' +
                ", cellIndexDescription='" + cellIndexDescription + '\'' +
                '}';
    }

    private static class Constants{
        public static final String NUM_VALUE = "valor numérico";
    }
}
