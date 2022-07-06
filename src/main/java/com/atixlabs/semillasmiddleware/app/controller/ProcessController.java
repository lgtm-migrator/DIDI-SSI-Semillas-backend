package com.atixlabs.semillasmiddleware.app.controller;

import com.atixlabs.semillasmiddleware.app.service.ProcessExecutorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping(ProcessController.URL_PROCESS)
@CrossOrigin(origins = {"http://localhost:8080", "${didi.server.url}"}, methods= {RequestMethod.GET,RequestMethod.POST, RequestMethod.PATCH})
@Slf4j
public class ProcessController {

    public static final String URL_PROCESS = "/process";

    private ProcessExecutorService processExecutorService;

    public ProcessController(ProcessExecutorService processExecutorService){
        this.processExecutorService = processExecutorService;
    }

    @GetMapping("/all")
    @ResponseStatus(HttpStatus.OK)
    public Map<String, String> processall() {
        return this.processExecutorService.execute();
    }
}
