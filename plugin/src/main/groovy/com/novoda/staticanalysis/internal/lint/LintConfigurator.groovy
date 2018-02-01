package com.novoda.staticanalysis.internal.lint

import com.novoda.staticanalysis.StaticAnalysisExtension
import com.novoda.staticanalysis.internal.Configurator
import com.novoda.staticanalysis.internal.Violations
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.Task

class LintConfigurator implements Configurator {

    private final Project project
    private final Violations violations
    private final Task evaluateViolations

    static LintConfigurator create(Project project,
                                   NamedDomainObjectContainer<Violations> violationsContainer,
                                   Task evaluateViolations) {
        Violations violations = violationsContainer.maybeCreate('Lint')
        return new LintConfigurator(project, violations, evaluateViolations)
    }

    private LintConfigurator(Project project, Violations violations, Task evaluateViolations) {
        this.project = project
        this.violations = violations
        this.evaluateViolations = evaluateViolations
    }

    @Override
    void execute() {
        project.extensions.findByType(StaticAnalysisExtension).ext."lintOptions" = { Closure config ->
            if (!isAndroidProject(project)) {
                return
            }
            configureLint(config)
            configureToolTask()
        }
    }

    private void configureLint(Closure config) {
        project.android.lintOptions(config)
        project.android.lintOptions {
            xmlReport = true
            htmlReport = true
            abortOnError false
        }
    }

    private void configureToolTask() {
        // evaluate violations after lint
        def collectViolations = createCollectViolationsTask(violations)
        evaluateViolations.dependsOn collectViolations
        collectViolations.dependsOn project.tasks['lint']
    }

    private CollectLintViolationsTask createCollectViolationsTask(Violations violations) {
        project.tasks.create('collectLintViolations', CollectLintViolationsTask) { collectViolations ->
            collectViolations.xmlReportFile = xmlOutputFile
            collectViolations.htmlReportFile = new File(defaultOutputFolder, 'lint-results.html')
            collectViolations.violations = violations
        }
    }

    private File getXmlOutputFile() {
        project.android.lintOptions.xmlOutput ?: new File(defaultOutputFolder, 'lint-results.xml')
    }

    private File getDefaultOutputFolder() {
        new File(project.buildDir, 'reports')
    }

    private static boolean isAndroidProject(final Project project) {
        final boolean isAndroidApplication = project.plugins.hasPlugin('com.android.application')
        final boolean isAndroidLibrary = project.plugins.hasPlugin('com.android.library')
        return isAndroidApplication || isAndroidLibrary
    }
}