package com.atixlabs.semillasmiddleware.app.service;

import com.atixlabs.semillasmiddleware.app.dto.ActionDto;
import com.atixlabs.semillasmiddleware.app.model.action.ActionLevelEnum;
import com.atixlabs.semillasmiddleware.app.model.action.ActionLog;
import com.atixlabs.semillasmiddleware.app.model.action.ActionTypeEnum;
import com.atixlabs.semillasmiddleware.app.repository.ActionLogRepository;
import com.atixlabs.semillasmiddleware.security.util.AuthUtil;
import com.atixlabs.semillasmiddleware.util.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class ActionLogService {

    private ActionLogRepository actionLogRepository;

    private AuthUtil authUtil;

    @Autowired
    public ActionLogService(ActionLogRepository actionLogRepository, AuthUtil authUtil){
        this.actionLogRepository = actionLogRepository;
        this.authUtil = authUtil;
    }


    @Value("${app.pageSize}")
    private String size;

    public Page<ActionDto>  find(Integer page, String username, Integer level, Integer actionType, String message, LocalDateTime dateFrom, LocalDateTime dateTo){

        Page<ActionLog> actions;
        Pageable pageable = null;
        if (page != null && page >= 0 && this.size != null)
           pageable = PageRequest.of(page, Integer.parseInt(size), Sort.by(Sort.Direction.ASC, "executionDateTime"));
        else
            pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "executionDateTime"));

        actions = actionLogRepository.find(pageable, username, this.getActionLevelEnumByValue(level), this.getActionTypeEnumByValue(actionType),message, dateFrom, dateTo);
                //this.getListMockActions();

        //return new PageImpl<ActionDto>(actionDtos, pageable, 2);

        Page<ActionDto> pageDto = actions.map(ActionDto::new);

        return pageDto;

        //CredentialPage credentialSet = new CredentialPage(pageDto, credentials.getNumberOfElements());

      //  return pageDto;
    }

    private ActionLevelEnum getActionLevelEnumByValue(Integer value){
        if(value!=null) {
            Optional<ActionLevelEnum> actionLevelEnum = ActionLevelEnum.valueOf(value);
            if (actionLevelEnum.isPresent())
                return actionLevelEnum.get();
        }

        return null;
    }

    private ActionTypeEnum getActionTypeEnumByValue(Integer value){
        if(value!=null) {
            Optional<ActionTypeEnum> actionTypeEnum = ActionTypeEnum.valueOf(value);
            if (actionTypeEnum.isPresent())
                return actionTypeEnum.get();
        }

        return null;
    }

    //[DIDI] - ERROR - Error de conexión con DIDI.
    private List<ActionDto> getListMockActions(){
        ActionDto actionDto = new ActionDto();
        actionDto.setActionType("DIDI");
        actionDto.setTexecutionDateTime(DateUtil.getLocalDateTimeNow());
        actionDto.setLevel("ERROR");
        actionDto.setMessage("Error de conexión con Didi");
        actionDto.setUser("admin");

        ActionDto actionDto2 = new ActionDto();
        actionDto2.setActionType("DIDI");
        actionDto2.setTexecutionDateTime(DateUtil.getLocalDateTimeNow());
        actionDto2.setLevel("INFO");
        actionDto2.setMessage("Sincronización DIDI OK");
        actionDto2.setUser("admin");

        ArrayList<ActionDto> actionDtos = new ArrayList<ActionDto>();
        actionDtos.add(actionDto);
        actionDtos.add(actionDto2);

        return actionDtos;
    }

    public ActionLog save(ActionLog actionLog){

        return  this.actionLogRepository.save(actionLog);
    }


    public ActionLog registerAction(ActionTypeEnum type, ActionLevelEnum level, String message){
        ActionLog actionLog = new ActionLog();
        String username = this.getCurrentUsername();
        log.info(username);
        if(username==null) {
            username = this.getCurrentUsernameDefault();
        }
        actionLog.setUserName(username);
        log.info(actionLog.getUserName());
        actionLog.setActionType(type);
        actionLog.setLevel(level);
        actionLog.setMessage(message);
        actionLog.setExecutionDateTime(DateUtil.getLocalDateTimeNow());

        actionLog = actionLogRepository.save(actionLog);

        log.debug(String.format("new action log register type: %s, level: %s, message: %s",type.getDescription(), level.getDescription(), message));
        return actionLog;

    }

    private String getCurrentUsername(){
        String currentUsername = null;
        try{
            currentUsername = authUtil.getCurrentUsername();
        }catch (Exception e){
            log.error("Can't get username logged ",e);
        }

        return currentUsername;
    }

    private String getCurrentUsernameDefault(){
        return "desconocido"; //TODO unharcode
    }

}