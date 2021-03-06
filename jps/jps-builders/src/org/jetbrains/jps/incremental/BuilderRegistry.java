package org.jetbrains.jps.incremental;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.builders.BuildTargetType;
import org.jetbrains.jps.service.JpsServiceManager;

import java.util.*;

/**
 * @author Eugene Zhuravlev
 *         Date: 9/17/11
 */
public class BuilderRegistry {
  private static final Logger LOG = Logger.getInstance("#org.jetbrains.jps.incremental.BuilderRegistry");
  private static class Holder {
    static final BuilderRegistry ourInstance = new BuilderRegistry();
  }
  private final Map<BuilderCategory, List<ModuleLevelBuilder>> myModuleLevelBuilders = new HashMap<BuilderCategory, List<ModuleLevelBuilder>>();
  private final List<TargetBuilder<?,?>> myTargetBuilders = new ArrayList<TargetBuilder<?,?>>();
  private final Map<String, BuildTargetType<?>> myTargetTypes = new LinkedHashMap<String, BuildTargetType<?>>();

  public static BuilderRegistry getInstance() {
    return Holder.ourInstance;
  }

  private BuilderRegistry() {
    for (BuilderCategory category : BuilderCategory.values()) {
      myModuleLevelBuilders.put(category, new ArrayList<ModuleLevelBuilder>());
    }

    for (BuilderService service : JpsServiceManager.getInstance().getExtensions(BuilderService.class)) {
      myTargetBuilders.addAll(service.createBuilders());
      final List<? extends ModuleLevelBuilder> moduleLevelBuilders = service.createModuleLevelBuilders();
      for (ModuleLevelBuilder builder : moduleLevelBuilders) {
        myModuleLevelBuilders.get(builder.getCategory()).add(builder);
      }
      for (BuildTargetType<?> type : service.getTargetTypes()) {
        String id = type.getTypeId();
        BuildTargetType<?> old = myTargetTypes.put(id, type);
        if (old != null) {
          LOG.error("Two build target types (" + type + ", " + old + ") use same id (" + id + ")");
        }
      }
    }
  }

  @Nullable
  public BuildTargetType<?> getTargetType(String typeId) {
    return myTargetTypes.get(typeId);
  }

  public Collection<BuildTargetType<?>> getTargetTypes() {
    return myTargetTypes.values();
  }

  public int getModuleLevelBuilderCount() {
    int count = 0;
    for (BuilderCategory category : BuilderCategory.values()) {
      count += getBuilders(category).size();
    }
    return count;
  }

  public List<BuildTask> getBeforeTasks(){
    return Collections.emptyList(); // todo
  }

  public List<BuildTask> getAfterTasks(){
    return Collections.emptyList(); // todo
  }

  public List<ModuleLevelBuilder> getBuilders(BuilderCategory category){
    return Collections.unmodifiableList(myModuleLevelBuilders.get(category)); // todo
  }

  public List<ModuleLevelBuilder> getModuleLevelBuilders() {
    return ContainerUtil.concat(myModuleLevelBuilders.values());
  }

  public List<TargetBuilder<?,?>> getTargetBuilders() {
    return myTargetBuilders;
  }
}
