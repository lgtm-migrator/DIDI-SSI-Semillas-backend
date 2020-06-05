package com.atixlabs.semillasmiddleware.bondareaService;


import com.atixlabs.semillasmiddleware.app.bondarea.model.Loan;
import com.atixlabs.semillasmiddleware.app.bondarea.model.constants.LoanStatusCodes;
import com.atixlabs.semillasmiddleware.app.bondarea.repository.LoanRepository;
import com.atixlabs.semillasmiddleware.app.bondarea.service.BondareaService;
import com.atixlabs.semillasmiddleware.util.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@SpringBootTest
@Slf4j
public class BondareaServiceTest {

    @Mock
    private LoanRepository loanRepository;

    @InjectMocks
    private BondareaService bondareaService;

    @Captor
    private ArgumentCaptor<Loan> captor;


    @Before
    public void setupMocks(){
        MockitoAnnotations.initMocks(this);
    }

    public Loan getMockLoan(){
        Loan loan = new Loan();
        loan.setDniPerson(123456L);
        loan.setIdBondareaLoan("1a");
        //loan.setStatus(LoanStatusCodes.ACTIVE.getCode());
        loan.setExpiredAmount((float) 0);
        loan.setCreationDate(DateUtil.getLocalDateTimeNow().toLocalDate());

        return loan;
    }

    private  List<Loan> firstLoansData(){
        List<Loan> loans = new ArrayList<>();

        Loan loan = getMockLoan();
        loans.add(loan);

        Loan loan2 = getMockLoan();
        loan2.setIdBondareaLoan("2a");
        loans.add(loan2);

        Loan loan3 = getMockLoan();
        loan3.setIdBondareaLoan("3a");
        loans.add(loan3);

        Loan loan4 = getMockLoan();
        loan4.setIdBondareaLoan("4a");
        loans.add(loan4);

        return loans;
    }

    private  List<Loan> secondLoansData(){
        List<Loan> loans = new ArrayList<>();

        //id 1 is deleted

        //loan 2 is the same
        Loan loan2 = getMockLoan();
        loan2.setIdBondareaLoan("2a");
        loans.add(loan2);

        //loan 3 modified tag
        Loan loan3 = getMockLoan();
        loan3.setIdBondareaLoan("3a");
        loan3.setTagBondareaLoan("nuevo tag");
        loans.add(loan3);

        //loan 4 is in default
        Loan loan4 = getMockLoan();
        loan4.setIdBondareaLoan("4a");
        loan4.setExpiredAmount((float) 100);
        loans.add(loan4);

        //new loan
        Loan loan5 = getMockLoan();
        loan5.setIdBondareaLoan("5a");
        loans.add(loan5);

        return loans;
    }

    private  List<Loan> secondLoansDataAllNew(){
        List<Loan> loans = new ArrayList<>();

        //all new loans
        Loan loan2 = getMockLoan();
        loan2.setIdBondareaLoan("5a");
        loans.add(loan2);

        Loan loan3 = getMockLoan();
        loan3.setIdBondareaLoan("6a");
        loans.add(loan3);

        Loan loan4 = getMockLoan();
        loan4.setIdBondareaLoan("7a");
        loans.add(loan4);


        return loans;
    }

    /*private  List<Loan> secondLoansDataAllExpired(){
        List<Loan> loans = new ArrayList<>();

        Loan loan = getMockLoan();
        loan.setExpiredAmount((float) 100);
        loans.add(loan);

        Loan loan2 = getMockLoan();
        loan2.setIdBondareaLoan("2a");
        loan2.setExpiredAmount((float) 100);
        loans.add(loan2);

        Loan loan3 = getMockLoan();
        loan3.setIdBondareaLoan("3a");
        loan3.setExpiredAmount((float) 100);
        loans.add(loan3);

        Loan loan4 = getMockLoan();
        loan4.setIdBondareaLoan("4a");
        loan4.setExpiredAmount((float) 100);
        loans.add(loan4);

        return loans;
    }*/



    @Test
    public void saveNewDataOnLoan() {
        when(loanRepository.findAll()).thenReturn(Collections.emptyList());

        List<Loan> loans = firstLoansData();
        bondareaService.updateExistingLoans(loans);

        verify(loanRepository,times(4)).save(captor.capture());

        List<Loan> loansSaves = captor.getAllValues();
        Loan firstLoan = loansSaves.get(0);

        Assertions.assertEquals(firstLoansData().get(0).getIdBondareaLoan(), firstLoan.getIdBondareaLoan());
        Assertions.assertTrue(firstLoan.getDniPerson() != null);

    }

    /**
     * This test will test that: the service takes an existing loans data from database compare with the new loans data and merge it.
     * Mix of all the different conditions
     */
    @Test
    public void updateAllLoans() {
        when(loanRepository.findByIdBondareaLoan("2a")).thenReturn(Optional.of(firstLoansData().get(1)));
        when(loanRepository.findByIdBondareaLoan("3a")).thenReturn(Optional.of(firstLoansData().get(2)));
        when(loanRepository.findByIdBondareaLoan("4a")).thenReturn(Optional.of(firstLoansData().get(3)));
        when(loanRepository.findByIdBondareaLoan("5a")).thenReturn(Optional.empty());

        when(loanRepository.findAllByModifiedTimeNotAndModifiedTimeNotNull(any())).thenReturn(List.of(firstLoansData().get(0)));

        List<Loan> loans = secondLoansData();
        bondareaService.updateExistingLoans(loans);

        verify(loanRepository,times(5)).save(captor.capture());

        List<Loan> loansSaves = captor.getAllValues();

        Assertions.assertTrue(loansSaves.size() > firstLoansData().size());
        Assertions.assertEquals(LoanStatusCodes.PENDING.getCode(), loansSaves.get(4).getStatus());
        Assertions.assertFalse(loansSaves.get(1).getTagBondareaLoan().equals(firstLoansData().get(2).getTagBondareaLoan()));

        List<Loan> pendingLoans = loansSaves.stream().filter(loan -> loan.getStatus().equals(LoanStatusCodes.PENDING.getCode())).collect(Collectors.toList());
        Assertions.assertEquals(1, pendingLoans.size() );

    }

    @Test
    public void updateLoansOldToPending() {
        when(loanRepository.findByIdBondareaLoan("5a")).thenReturn(Optional.empty());
        when(loanRepository.findByIdBondareaLoan("6a")).thenReturn(Optional.empty());
        when(loanRepository.findByIdBondareaLoan("7a")).thenReturn(Optional.empty());

        when(loanRepository.findAllByModifiedTimeNotAndModifiedTimeNotNull(any())).thenReturn(firstLoansData());

        List<Loan> loans = secondLoansDataAllNew();
        bondareaService.updateExistingLoans(loans);

        verify(loanRepository,times(7)).save(captor.capture());

        List<Loan> loansSaves = captor.getAllValues();

        Assertions.assertTrue(loansSaves.size() > firstLoansData().size());

        //the active loans will be all. But some are pending -> 3
        List<Loan> activeLoans = loansSaves.stream().filter(loan -> loan.getStatus().equals(LoanStatusCodes.ACTIVE.getCode())).collect(Collectors.toList());
        Assertions.assertEquals(3, activeLoans.size() );

        // the pending loans will be the older loans in this case -> 4
        List<Loan> pendingLoans = loansSaves.stream().filter(loan -> loan.getStatus().equals(LoanStatusCodes.PENDING.getCode())).collect(Collectors.toList());
        Assertions.assertEquals(4, pendingLoans.size() );

    }

    @Test
    public void determinatePendingLoanToFinish(){
      //  when(loanRepository.findAllByStatus(LoanStatusCodes.PENDING.getCode())).thenReturn(pendientes);
        //when(bondareaService.getLoans(BondareaLoanStatusCodes.FINALIZED.getCode(), anyString(),"")).thenReturn(vuelve 1 l)
    //TODO


    }

    /*@Test
    public void updateLoansAllExpired() {
        when(loanRepository.findAll()).thenReturn(firstLoansData());

        List<Loan> loans = secondLoansDataAllExpired();
        bondareaService.updateExistingLoans(loans);

        verify(loanRepository,times(4)).save(captor.capture());

        List<Loan> loansSaves = captor.getAllValues();


        //the active loans will be any.
        List<Loan> activeLoans = loansSaves.stream().filter(loan -> loan.getIsActive() == true).collect(Collectors.toList());
        Assertions.assertEquals(0, activeLoans.size() );

        // the pending loans will be any.
        List<Loan> pendingLoans = loansSaves.stream().filter(loan -> loan.getPending() == true).collect(Collectors.toList());
        Assertions.assertEquals(0, pendingLoans.size() );

    }
    */

}