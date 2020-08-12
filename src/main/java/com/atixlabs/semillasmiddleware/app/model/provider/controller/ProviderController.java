package com.atixlabs.semillasmiddleware.app.model.provider.controller;

import com.atixlabs.semillasmiddleware.app.model.provider.dto.ProviderDto;
import com.atixlabs.semillasmiddleware.app.model.provider.dto.ProviderFilterDto;
import com.atixlabs.semillasmiddleware.app.model.provider.exception.InexistentCategoryException;
import com.atixlabs.semillasmiddleware.app.model.provider.dto.ProviderCreateRequest;
import com.atixlabs.semillasmiddleware.app.model.provider.exception.InexistentProviderException;
import com.atixlabs.semillasmiddleware.app.model.provider.model.Provider;
import com.atixlabs.semillasmiddleware.app.model.provider.service.ProviderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.util.Optional;


@Slf4j
@RestController
@RequestMapping(ProviderController.URL_MAPPING)
@CrossOrigin(origins = "*", methods= {RequestMethod.GET,RequestMethod.POST})
public class ProviderController {
    public static final String URL_MAPPING = "/providers";

    @Autowired
    public ProviderController(ProviderService providerService){
        this.providerService = providerService;
    }

    private ProviderService providerService;

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping
    public ResponseEntity<String> createProvider(@RequestBody @Valid ProviderCreateRequest providerCreateRequest){
        try {
            providerService.create(providerCreateRequest);
        }catch (InexistentCategoryException ice){
            return ResponseEntity.badRequest().body("ProviderCategory id is incorrect");
        }
        return ResponseEntity.accepted().body("created.");
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public Page<Provider> findAllProviders(@RequestParam("page") @Min(0) int page,
                                           @RequestParam Optional<Boolean> activesOnly,
                                           @RequestParam Optional<String> criteriaQuery,
                                           @RequestParam Optional<Long> categoryId){

        ProviderFilterDto providerFilterDto = ProviderFilterDto.builder()
                .activesOnly(activesOnly)
                .criteriaQuery(criteriaQuery)
                .categoryId(categoryId)
                .build();
        return providerService.findAll(page, providerFilterDto);
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<?> findProvider(@PathVariable @Min(1) Long id){

        try {
            ProviderDto provider = providerService.findById(id).toDto();
            return ResponseEntity.ok().body(provider);
        }catch (InexistentProviderException ipe){
            return ResponseEntity.notFound().build();
        }
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping("/disable/{id}")
    public ResponseEntity<?> disableProvider(@PathVariable @Min(1) Long providerId){
        try {
            providerService.disable(providerId);
        }catch (InexistentProviderException ipe){
            return ResponseEntity.badRequest().body("There is no provider with id: "+providerId);
        }

        return ResponseEntity.ok().body("ok");
    }
}
