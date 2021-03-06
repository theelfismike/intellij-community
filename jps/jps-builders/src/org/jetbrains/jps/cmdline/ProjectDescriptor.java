package org.jetbrains.jps.cmdline;

import org.jetbrains.jps.builders.BuildRootIndex;
import org.jetbrains.jps.builders.BuildTargetIndex;
import org.jetbrains.jps.builders.logging.BuildLoggingManager;
import org.jetbrains.jps.incremental.CompilerEncodingConfiguration;
import org.jetbrains.jps.incremental.fs.BuildFSState;
import org.jetbrains.jps.incremental.storage.BuildDataManager;
import org.jetbrains.jps.incremental.storage.BuildTargetsState;
import org.jetbrains.jps.incremental.storage.ProjectTimestamps;
import org.jetbrains.jps.indices.IgnoredFileIndex;
import org.jetbrains.jps.indices.ModuleExcludeIndex;
import org.jetbrains.jps.model.JpsModel;
import org.jetbrains.jps.model.JpsProject;
import org.jetbrains.jps.model.java.JpsJavaSdkType;
import org.jetbrains.jps.model.library.sdk.JpsSdk;
import org.jetbrains.jps.model.module.JpsModule;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
* @author Eugene Zhuravlev
*         Date: 1/8/12
*/
public final class ProjectDescriptor {
  private final JpsProject myProject;
  private final JpsModel myModel;
  public final BuildFSState fsState;
  public final ProjectTimestamps timestamps;
  public final BuildDataManager dataManager;
  private final BuildLoggingManager myLoggingManager;
  private final BuildTargetsState myTargetsState;
  private final ModuleExcludeIndex myModuleExcludeIndex;
  private int myUseCounter = 1;
  private Set<JpsSdk<?>> myProjectJavaSdks;
  private CompilerEncodingConfiguration myEncodingConfiguration;
  private final BuildRootIndex myBuildRootIndex;
  private final BuildTargetIndex myBuildTargetIndex;
  private final IgnoredFileIndex myIgnoredFileIndex;

  public ProjectDescriptor(JpsModel model,
                           BuildFSState fsState,
                           ProjectTimestamps timestamps,
                           BuildDataManager dataManager,
                           BuildLoggingManager loggingManager,
                           final ModuleExcludeIndex moduleExcludeIndex,
                           final BuildTargetsState targetsState,
                           final BuildTargetIndex buildTargetIndex, final BuildRootIndex buildRootIndex, IgnoredFileIndex ignoredFileIndex) {
    myModel = model;
    myIgnoredFileIndex = ignoredFileIndex;
    myProject = model.getProject();
    this.fsState = fsState;
    this.timestamps = timestamps;
    this.dataManager = dataManager;
    myBuildTargetIndex = buildTargetIndex;
    myBuildRootIndex = buildRootIndex;
    myLoggingManager = loggingManager;
    myModuleExcludeIndex = moduleExcludeIndex;
    myProjectJavaSdks = new HashSet<JpsSdk<?>>();
    myEncodingConfiguration = new CompilerEncodingConfiguration(model, buildRootIndex);
    for (JpsModule module : myProject.getModules()) {
      final JpsSdk<?> sdk = module.getSdk(JpsJavaSdkType.INSTANCE);
      if (sdk != null && !myProjectJavaSdks.contains(sdk) && sdk.getVersionString() != null && sdk.getHomePath() != null) {
        myProjectJavaSdks.add(sdk);
      }
    }
    myTargetsState = targetsState;
  }

  public BuildRootIndex getBuildRootIndex() {
    return myBuildRootIndex;
  }

  public BuildTargetIndex getBuildTargetIndex() {
    return myBuildTargetIndex;
  }

  public IgnoredFileIndex getIgnoredFileIndex() {
    return myIgnoredFileIndex;
  }

  public BuildTargetsState getTargetsState() {
    return myTargetsState;
  }

  public CompilerEncodingConfiguration getEncodingConfiguration() {
    return myEncodingConfiguration;
  }

  public Set<JpsSdk<?>> getProjectJavaSdks() {
    return myProjectJavaSdks;
  }

  public BuildLoggingManager getLoggingManager() {
    return myLoggingManager;
  }

  public synchronized void incUsageCounter() {
    myUseCounter++;
  }

  public void release() {
    boolean shouldClose;
    synchronized (this) {
      --myUseCounter;
      shouldClose = myUseCounter == 0;
    }
    if (shouldClose) {
      try {
        timestamps.close();
      }
      finally {
        try {
          dataManager.close();
        }
        catch (IOException e) {
          e.printStackTrace(System.err);
        }
      }
    }
  }

  public ModuleExcludeIndex getModuleExcludeIndex() {
    return myModuleExcludeIndex;
  }

  public JpsModel getModel() {
    return myModel;
  }

  public JpsProject getProject() {
    return myProject;
  }
}
