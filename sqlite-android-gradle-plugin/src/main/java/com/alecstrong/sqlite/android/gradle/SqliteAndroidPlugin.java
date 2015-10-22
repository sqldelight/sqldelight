package com.alecstrong.sqlite.android.gradle;

import org.gradle.api.Project;
import org.gradle.api.Plugin;
import org.gradle.api.plugins.JavaPluginConvention;

public class SqliteAndroidPlugin implements Plugin<Project> {
	@Override
	public void apply(Project project) {
		project.getConvention().getPlugin(JavaPluginConvention.class).getSourceSets().all((sourceSet) -> {
			project.getTasks().getByName(sourceSet.getCompileJavaTaskName())
			sourceSet.getCompileJavaTaskName().
		});
	}
}
