package io.depsight.api.analyse.resolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.when;

import io.depsight.api.analyse.dto.request.ParsedDependency;
import io.depsight.api.analyse.dto.response.ResolutionResult;
import io.depsight.api.analyse.resolver.dto.VersionRequest;
import io.depsight.api.infrastructure.maven.MavenCentralClient;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
public class BfsResolverTest {

    @Mock
    private MavenCentralClient mavenCentralClient;

    @Mock
    private ParentBomResolver parentBomResolver;

    @InjectMocks
    private BfsResolver bfsResolver;

    @Test
    void resolve_shouldCaptureBothVersionsWhenBranchesConflict() {
        String pomA = """
            <project>
              <modelVersion>4.0.0</modelVersion>
              <groupId>com.example</groupId>
              <artifactId>lib-a</artifactId>
              <version>1.0</version>
              <dependencies>
                <dependency>
                  <groupId>commons-logging</groupId>
                  <artifactId>commons-logging</artifactId>
                  <version>1.1</version>
                </dependency>
              </dependencies>
            </project>
            """;

        String pomB = """
            <project>
              <modelVersion>4.0.0</modelVersion>
              <groupId>com.example</groupId>
              <artifactId>lib-b</artifactId>
              <version>1.0</version>
              <dependencies>
                <dependency>
                  <groupId>commons-logging</groupId>
                  <artifactId>commons-logging</artifactId>
                  <version>1.2</version>
                </dependency>
              </dependencies>
            </project>
            """;

        String leafPom = """
            <project>
              <modelVersion>4.0.0</modelVersion>
              <groupId>commons-logging</groupId>
              <artifactId>commons-logging</artifactId>
              <version>1.0</version>
            </project>
            """;

        when(mavenCentralClient.fetchPomXml("com.example", "lib-a", "1.0")).thenReturn(Mono.just(pomA));
        when(mavenCentralClient.fetchPomXml("com.example", "lib-b", "1.0")).thenReturn(Mono.just(pomB));
        when(mavenCentralClient.fetchPomXml("commons-logging", "commons-logging", "1.1"))
                .thenReturn(Mono.just(leafPom));
        Mockito.lenient()
                .when(mavenCentralClient.fetchPomXml("commons-logging", "commons-logging", "1.2"))
                .thenReturn(Mono.just(leafPom));
        when(parentBomResolver.resolveBomImports(anyList(), anyMap())).thenReturn(Mono.just(Map.of()));

        List<ParsedDependency> directDeps = List.of(
                new ParsedDependency("com.example", "lib-a", "1.0", "compile"),
                new ParsedDependency("com.example", "lib-b", "1.0", "compile"));
        ResolutionResult result = bfsResolver.resolve(directDeps, 6);
        System.out.println("Collected keys: " + result.versionRequest().keySet());

        List<VersionRequest> commonsLoggingRequests = result.versionRequest().get("commons-logging:commons-logging");

        assertThat(commonsLoggingRequests).hasSize(2);
        assertThat(commonsLoggingRequests).extracting(VersionRequest::version).containsExactlyInAnyOrder("1.1", "1.2");
    }
}
