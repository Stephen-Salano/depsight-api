package io.depsight.api.analyse.entity;

import io.depsight.api.analyse.enums.AnalysisStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "analyses", schema = "public")
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Analysis {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "project_name", length = Integer.MAX_VALUE)
    private String projectName;

    @Column(name = "group_id", length = Integer.MAX_VALUE)
    private String groupId;

    @Column(name = "artifact_id", length = Integer.MAX_VALUE)
    private String artifactId;

    @Column(name = "version", length = Integer.MAX_VALUE)
    private String version;

    @ColumnDefault("'PENDING'")
    @Column(name = "status", columnDefinition = "analysis_status not null")
    @Enumerated(EnumType.STRING)
    private AnalysisStatus status;

    @ColumnDefault("0")
    @Column(name = "total_dependencies")
    private Integer totalDependencies;

    @ColumnDefault("0")
    @Column(name = "total_vulnerabilities")
    private Integer totalVulnerabilities;

    @ColumnDefault("false")
    @Column(name = "has_conflicts")
    private Boolean hasConflicts;

    @NotNull
    @ColumnDefault("now()")
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;


}