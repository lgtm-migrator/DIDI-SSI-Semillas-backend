package com.atixlabs.semillasmiddleware.app.bondarea.controller;

import com.atixlabs.semillasmiddleware.app.bondarea.dto.BondareaLoanDto;
import com.atixlabs.semillasmiddleware.app.bondarea.model.Loan;
import com.atixlabs.semillasmiddleware.app.bondarea.service.BondareaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@Slf4j
@RequestMapping(BondareaController.URL_MAPPING_CREDENTIAL)
@CrossOrigin(origins = "*", methods= {RequestMethod.GET,RequestMethod.POST})
public class BondareaController {

    public static final String URL_MAPPING_CREDENTIAL = "/bondarea";

    private BondareaService bondareaService;

    @Autowired
    public BondareaController(BondareaService bondareaService) {
        this.bondareaService = bondareaService;
    }


    @PostMapping("/synchronize")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<List<BondareaLoanDto>> synchronizeBondareaLoans()  {
        log.info("BONDAREA - GET LOANS");
        List<BondareaLoanDto> loansDto;
        // idAccount not null!!
        try {
            //loans = bondareaService.getLoans("12345", "55"); //loanState 60 -> se consulta con segunda api
            loansDto = bondareaService.getMockLoans("","");
            List<Loan> loans = loansDto.stream().map(loanDto -> new Loan(loanDto)).collect(Collectors.toList());
            bondareaService.updateExistingLoans(loans);

        }
        catch (Exception ex){
            log.error("Could not synchronized data from Bondarea !");
            return new ResponseEntity<>(Collections.emptyList(), HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<>(loansDto, HttpStatus.OK);
    }




    }
