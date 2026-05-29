package io.depsight.api.analyse.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "dependencies", schema = "public", indexes = {@Index(name = "unique_maven_artifact",
        columnList = "analysis_id, group_id, artifact_id, version",
        unique = true)})
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Dependency {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @NotNull
    @Column(name = "name", nullable = false, length = Integer.MAX_VALUE)
    private String name;

    @NotNull
    @Column(name = "group_id", nullable = false, length = Integer.MAX_VALUE)
    private String groupId;

    @NotNull
    @Column(name = "artifact_id", nullable = false, length = Integer.MAX_VALUE)
    private String artifactId;

    @NotNull
    @Column(name = "version", nullable = false, length = Integer.MAX_VALUE)
    private String version;

    @ColumnDefault("'COMPILE'")
    @Column(name = "scope", columnDefinition = "scope")
    private Object scope;

    @ColumnDefault("'jar'")
    @Column(name = "packaging", length = Integer.MAX_VALUE)
    private String packaging;

    @Column(name = "classifier", length = Integer.MAX_VALUE)
    private String classifier;

    @ColumnDefault("false")
    @Column(name = "optional")
    private Boolean optional;

    @Column(name = "system_path", length = Integer.MAX_VALUE)
    private String systemPath;

    @Column(name = "artifact_size")
    private Long artifactSize;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "analysis_id", nullable = false)
    private Analysis analysis;

    @Column(name = "transitive_count")
    private Integer transitiveCount;

    @Column(name = "latest_version", length = Integer.MAX_VALUE)
    private String latestVersion;

    @ColumnDefault("false")
    @Column(name = "is_outdated")
    private Boolean isOutdated;


}