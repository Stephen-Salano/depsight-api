package io.depsight.api.analyse.service;

import io.depsight.api.analyse.dto.request.AnalyseRequest;
import io.depsight.api.analyse.resolver.DependencyNode;
import java.util.List;

public interface AnalyseService {

  List<DependencyNode> analyse(AnalyseRequest request);
}
