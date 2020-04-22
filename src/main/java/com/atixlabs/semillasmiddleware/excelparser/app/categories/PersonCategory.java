package com.atixlabs.semillasmiddleware.excelparser.app.categories;

import com.atixlabs.semillasmiddleware.excelparser.app.constants.PersonQuestion;
import com.atixlabs.semillasmiddleware.excelparser.app.constants.PersonType;
import com.atixlabs.semillasmiddleware.excelparser.app.dto.AnswerDto;
import com.atixlabs.semillasmiddleware.excelparser.app.dto.AnswerRow;
import com.atixlabs.semillasmiddleware.excelparser.dto.ProcessExcelFileResult;
import com.atixlabs.semillasmiddleware.util.StringUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Getter
public class PersonCategory implements Category {

    String categoryOriginalName;

    AnswerDto name;
    AnswerDto surname;
    AnswerDto idType;
    AnswerDto idNumber;
    AnswerDto gender;
    AnswerDto birthDate;
    AnswerDto relation;
    PersonType personType;

    public PersonCategory(String categoryOriginalName){
        this.name = new AnswerDto(PersonQuestion.NAME);
        this.surname = new AnswerDto(PersonQuestion.SURNAME);
        this.idType = new AnswerDto(PersonQuestion.ID_TYPE);
        this.idNumber = new AnswerDto(PersonQuestion.ID_NUMBER);
        this.gender = new AnswerDto(PersonQuestion.GENDER);
        this.birthDate = new AnswerDto(PersonQuestion.BIRTHDATE);
        this.relation = new AnswerDto(PersonQuestion.RELATION);

        this.categoryOriginalName = categoryOriginalName;

        String personTypeString = StringUtil.removeNumbers(StringUtil.toUpperCaseTrimAndRemoveAccents(categoryOriginalName.replaceAll("DATOS|DEL","")));

        try{
            this.personType = PersonType.get(personTypeString);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void loadData(AnswerRow answerRow, ProcessExcelFileResult processExcelFileResult) {
        String question = StringUtil.toUpperCaseTrimAndRemoveAccents(answerRow.getQuestion());
        PersonQuestion questionMatch = PersonQuestion.get(question);

        if (questionMatch == null)
            return;

        switch (questionMatch) {
            case ID_TYPE:
                this.idType.setAnswer(answerRow, processExcelFileResult);
                break;
            case ID_NUMBER:
                this.idNumber.setAnswer(answerRow, processExcelFileResult);
                break;
            case NAME:
                this.name.setAnswer(answerRow, processExcelFileResult);
                break;
            case SURNAME:
                this.surname.setAnswer(answerRow, processExcelFileResult);
                break;
            case GENDER:
                this.gender.setAnswer(answerRow, processExcelFileResult);
                break;
            case BIRTHDATE:
                this.birthDate.setAnswer(answerRow, processExcelFileResult);
                break;
            case RELATION:
                this.relation.setAnswer(answerRow, processExcelFileResult);
                break;
        }
    }


    @Override
    public Category getData() {
        return this;
    }

    @Override
    public String getCategoryOriginalName(){
        return categoryOriginalName;
    }

    @Override
    public boolean isValid(ProcessExcelFileResult processExcelFileResult) {
        List<AnswerDto> answers = new LinkedList<>();
        answers.add(this.name);
        answers.add(this.surname);
        answers.add(this.idNumber);
        answers.add(this.gender);
        answers.add(this.birthDate);
        answers.add(this.relation);

        List<Boolean> validations = answers.stream().map(answerDto -> answerDto.isValid(processExcelFileResult, categoryOriginalName)).collect(Collectors.toList());
        return validations.stream().allMatch(v->v);
    }


    public String getName() {
        return (String) name.getAnswer();
    }

    public String getSurname() {
        return (String) surname.getAnswer();
    }

    public String getIdType() {
        return (String) idType.getAnswer();
    }

    public Long getIdNumber() {
        return (Long) idNumber.getAnswer();
    }

    public String getGender() {
        return (String) gender.getAnswer();
    }

    public LocalDate getBirthDate() {
        return (LocalDate) birthDate.getAnswer();
    }

    public String getRelation() {
        return (String) relation.getAnswer();
    }


    @Override
    public String toString() {
        return "PersonCategory{" +
                "categoryOriginalName='" + categoryOriginalName + '\'' +
                ", name=" + name +
                ", surname=" + surname +
                ", idType=" + idType +
                ", idNumber=" + idNumber +
                ", gender=" + gender +
                ", birthDate=" + birthDate +
                ", relation=" + relation +
                ", personType=" + personType +
                '}';
    }
}
