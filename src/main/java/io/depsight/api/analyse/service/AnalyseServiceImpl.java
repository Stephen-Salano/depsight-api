package io.depsight.api.analyse.service;

import io.depsight.api.analyse.dto.request.AnalyseRequest;
import io.depsight.api.analyse.dto.request.ParsedDependency;
import io.depsight.api.analyse.parser.PomParser;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyseServiceImpl implements AnalyseService {

  @Override
  public List<ParsedDependency> analyse(AnalyseRequest request) {
    // extract the string and call Pom parser static method

    log.info("Parsing Pom from request");
    return PomParser.parse(request.pomXml());
  }
}
