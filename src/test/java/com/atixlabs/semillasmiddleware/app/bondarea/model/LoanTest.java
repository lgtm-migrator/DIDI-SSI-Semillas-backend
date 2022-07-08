package com.atixlabs.semillasmiddleware.app.bondarea.model;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;

//@RunWith(SpringRunner.class)
@SpringBootTest
@Slf4j
public class LoanTest {



    private Loan getBaseLoan(){
        Loan baseLoan = new Loan();
        baseLoan.setDniPerson(29302555L);
        baseLoan.setIdBondareaLoan("123456");
        baseLoan.setIdProductLoan("product01");
        baseLoan.setIdGroup("idGroup");
        baseLoan.setUserId("user01");
        baseLoan.setCreationDate(LocalDate.of(2020, 6, 20));

        baseLoan.setStatus("ACTIVE");
        baseLoan.setCycleDescription("Cl01");
        baseLoan.setExpiredAmount(new BigDecimal(1000));

        return  baseLoan;
    }

    @Test
    public void loanEqualsTrueOk(){

        Loan baseLoan = this.getBaseLoan();

        Loan newLoan = this.getBaseLoan();

        Assertions.assertTrue(baseLoan.equals(newLoan));
    }

    @Test
    public void loanNotEqualsAmountOk(){

        Loan baseLoan = this.getBaseLoan();

        Loan newLoan = this.getBaseLoan();
        newLoan.setExpiredAmount(new BigDecimal(100));

        Assertions.assertFalse(baseLoan.equals(newLoan));
    }

    @Test
    public void loanNotEqualsCreationDAteLoanOk(){

        Loan baseLoan = this.getBaseLoan();

        Loan newLoan = this.getBaseLoan();
        newLoan.setCreationDate(LocalDate.of(2020,6,19));

        Assertions.assertFalse(baseLoan.equals(newLoan));
    }

    @Test
    public void loanNotEqualsClassOk(){

        Loan baseLoan = this.getBaseLoan();

        Object newLoan = new Object();

        Assertions.assertFalse(baseLoan.equals(newLoan));
    }
}
