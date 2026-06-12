package io.depsight.api.analyse.controller;

import io.depsight.api.analyse.dto.request.AnalyseRequest;
import io.depsight.api.analyse.dto.request.ParsedDependency;
import io.depsight.api.analyse.service.AnalyseService;
import io.depsight.api.common.config.ApiResponse;
import io.depsight.api.common.controller.BaseController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController()
@RequestMapping("/api/analyse")
@RequiredArgsConstructor
@Tag(name = "Analyse POM", description = "Analyse the pom.xml")
@Slf4j
public class AnalyseController extends BaseController {

  private final AnalyseService analyseService;

  @PostMapping
  @Operation(summary = "Analyse a sample pom.xml")
  public ResponseEntity<ApiResponse<List<ParsedDependency>>> analyse(
      @Valid @RequestBody AnalyseRequest request) {
    log.info("POST /api/analyse analysing pom.xml");
    return ok(analyseService.analyse(request), "POM parsed successfully");
  }
}
