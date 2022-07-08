package com.atixlabs.semillasmiddleware.app.model.identityValidationRequest.service;

import com.atixlabs.semillasmiddleware.app.model.identityValidationRequest.constant.RequestState;
import com.atixlabs.semillasmiddleware.app.model.identityValidationRequest.exceptions.InexistentIdentityValidationRequestException;
import com.atixlabs.semillasmiddleware.app.model.identityValidationRequest.repository.IdentityValidationRequestRepository;
import com.atixlabs.semillasmiddleware.app.service.DidiServerService;
import com.atixlabs.semillasmiddleware.app.model.identityValidationRequest.dto.IdentityValidationFilter;
import com.atixlabs.semillasmiddleware.app.model.identityValidationRequest.dto.IdentityValidationRequestDto;
import com.atixlabs.semillasmiddleware.app.model.identityValidationRequest.dto.StatusChangeDto;
import com.atixlabs.semillasmiddleware.app.model.identityValidationRequest.model.IdentityValidationRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;
import javax.persistence.criteria.*;
import java.util.stream.Stream;

@Service
@Slf4j
public class IdentityValidationRequestService {

    @Autowired
    public IdentityValidationRequestService(IdentityValidationRequestRepository identityValidationRequestRepository,
                                            ShareStateChangeService shareStateChangeService,
                                            DidiServerService didiServerService){
        this.identityValidationRequestRepository = identityValidationRequestRepository;
        this.shareStateChangeService = shareStateChangeService;
        this.didiServerService = didiServerService;
    }

    @Value("${app.pageSize}")
    private String size;

    private IdentityValidationRequestRepository identityValidationRequestRepository;
    private ShareStateChangeService shareStateChangeService;
    private DidiServerService didiServerService;

    public IdentityValidationRequest create(IdentityValidationRequestDto identityValidationRequestDto){
        IdentityValidationRequest idr =
                new IdentityValidationRequest(identityValidationRequestDto.getDni(),
                        identityValidationRequestDto.getDid(),
                        identityValidationRequestDto.getEmail(),
                        identityValidationRequestDto.getPhone(),
                        identityValidationRequestDto.getName(),
                        identityValidationRequestDto.getLastName(),
                        RequestState.IN_PROGRESS,
                        LocalDate.now());

        log.info("Create Identity validation request: \n "+ idr.toString());
        return identityValidationRequestRepository.save(idr);
    }

    public Page<IdentityValidationRequest> findAll(Integer page, IdentityValidationFilter identityValidationFilter){
        Pageable pageRequest = PageRequest.of(page, Integer.valueOf(size), Sort.by("date").ascending());
        return identityValidationRequestRepository.findAll(getIdentityRequestSpecification(identityValidationFilter), pageRequest);
    }

    public Optional<IdentityValidationRequest> findById(Long idValidationRequest){
        return identityValidationRequestRepository.findById(idValidationRequest);
    }

    public void changeRequestState(Long idValidationRequest, StatusChangeDto statusChangeDto)throws InexistentIdentityValidationRequestException {
        Optional<String> rejectionObservations = statusChangeDto.getRejectionObservations();
        log.info("Changing identity validation request state, request id["+idValidationRequest+"], state["+statusChangeDto.getRequestState()+"], revocation reason["+rejectionObservations.orElse("")+"]");

        IdentityValidationRequest identityValidationRequest = this.findById(idValidationRequest)
                .orElseThrow(InexistentIdentityValidationRequestException::new);

        log.info("Request found");
        identityValidationRequest.setReviewDate(LocalDate.now());
        identityValidationRequest.setRequestState(statusChangeDto.getRequestState());
        if (statusChangeDto.getRequestState().equals(RequestState.FAILURE)) {
            statusChangeDto.getRejectReason().ifPresent(identityValidationRequest::setRejectReason);
            rejectionObservations.ifPresent(identityValidationRequest::setRejectionObservations);
        }
        statusChangeDto.getDni().ifPresent(identityValidationRequest::setDni);
        log.info("Final request state: \n "+ identityValidationRequest.toString());

        didiServerService.updateIdentityRequest(identityValidationRequest);
        identityValidationRequestRepository.save(identityValidationRequest);
        shareStateChangeService.shareStateChange(identityValidationRequest);

    }

     private Specification<IdentityValidationRequest> getIdentityRequestSpecification(IdentityValidationFilter identityValidationFilter) {
        return (Specification<IdentityValidationRequest>) (root, query, cb) -> {
            Stream<Predicate> predicates = Stream.of(
                    identityValidationFilter.getDateFrom().map(value -> cb.greaterThanOrEqualTo(root.get("date"), value)),
                    identityValidationFilter.getDateTo().map(value -> cb.lessThanOrEqualTo(root.get("date"), value)),
                    identityValidationFilter.getRequestState().map(value -> cb.equal(root.get("requestState"), value)),
                    identityValidationFilter.getDni().map(value -> cb.like(root.get("dni").as(String.class), "%" + value.toString() + "%")),
                    identityValidationFilter.getName().map(value -> cb.like(root.get("name").as(String.class), "%" + value.toString() + "%")),
                    identityValidationFilter.getSurname().map(value -> cb.like(root.get("lastName").as(String.class), "%" + value.toString() + "%"))
            ).flatMap(Optional::stream);
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}

