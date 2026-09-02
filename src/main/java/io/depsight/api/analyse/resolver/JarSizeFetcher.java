package io.depsight.api.analyse.resolver;

import io.depsight.api.common.exception.ExternalApiException;
import io.depsight.api.infrastructure.maven.MavenCentralClient;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@RequiredArgsConstructor
public class JarSizeFetcher {

    private final MavenCentralClient mavenCentralClient;

    public Mono<Map<String, Long>> fetchJarSizes(List<DependencyNode> flatNodes) {
        return Flux.fromIterable(flatNodes)
                .flatMap(node -> {
                    String key = coordinateKey(node);
                    return mavenCentralClient
                            .fetchJarSize(node.groupId(), node.artifactId(), node.version())
                            .onErrorResume(ExternalApiException.class, error -> Mono.empty())
                            .map(size -> Map.entry(key, size));
                })
                .collectMap(Map.Entry::getKey, Map.Entry::getValue);
    }

    private String coordinateKey(DependencyNode node) {
        return node.groupId() + ":" + node.artifactId() + ":" + node.version();
    }
}
