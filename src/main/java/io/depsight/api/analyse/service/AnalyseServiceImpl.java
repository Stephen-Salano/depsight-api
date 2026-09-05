package io.depsight.api.analyse.service;

import io.depsight.api.analyse.dto.request.AnalyseRequest;
import io.depsight.api.analyse.dto.request.MavenCooridinates;
import io.depsight.api.analyse.dto.request.ParsedDependency;
import io.depsight.api.analyse.dto.response.AnalysisResult;
import io.depsight.api.analyse.dto.response.ResolutionResult;
import io.depsight.api.analyse.parser.PomParser;
import io.depsight.api.analyse.resolver.AnalysisOrchestrator;
import io.depsight.api.analyse.resolver.BfsResolver;
import io.depsight.api.analyse.resolver.ParentBomResolver;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.model.Model;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyseServiceImpl implements AnalyseService {

    private final ParentBomResolver parentBomResolver;
    private final BfsResolver bfsResolver;
    private final AnalysisOrchestrator orchestrator;

    @Override
    public AnalysisResult analyse(AnalyseRequest request) {
        // extracting the maxDepth
        int maxDepth = Objects.requireNonNullElse(request.maxDepth(), 6);

        // extract the string and call Pom parser static method

        log.info("Parsing Pom from request");
        Model model = PomParser.parse(request.pomXml());

        Map<String, String> properties = PomParser.extractProperties(model);
        List<ParsedDependency> dependencies = PomParser.extractDependencies(model, properties);
        // Get the parent from the pomXml model;
        MavenCooridinates cooridinates = PomParser.extractParent(model);
        if (cooridinates == null) {
            ResolutionResult resolvedNodes = bfsResolver.resolve(dependencies, maxDepth);
            return orchestrator
                    .enrichTree(resolvedNodes.tree())
                    .block(); // NOTE: blocking becuase AnalyseService interface returns a synchronous AnalysisResult we
            // need to call block()
        }
        List<ParsedDependency> resolved = parentBomResolver.resolveParent(cooridinates, dependencies);
        ResolutionResult node = bfsResolver.resolve(resolved, maxDepth);
        return orchestrator
                .enrichTree(node.tree())
                .block(); // NOTE: blocking becuase AnalyseService interface returns a synchronous AnalysisResult,
        // calling block()
    }
}
