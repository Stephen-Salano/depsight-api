package io.depsight.api.analyse.service;

import io.depsight.api.analyse.dto.request.AnalyseRequest;
import io.depsight.api.analyse.dto.request.ParsedDependency;
import java.util.List;

public interface AnalyseService {

  List<ParsedDependency> analyse(AnalyseRequest request);
}
