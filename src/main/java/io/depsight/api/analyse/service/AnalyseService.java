package io.depsight.api.analyse.service;

import io.depsight.api.analyse.dto.request.AnalyseRequest;
import io.depsight.api.analyse.dto.response.AnalysisResult;

public interface AnalyseService {

  AnalysisResult analyse(AnalyseRequest request);
}
