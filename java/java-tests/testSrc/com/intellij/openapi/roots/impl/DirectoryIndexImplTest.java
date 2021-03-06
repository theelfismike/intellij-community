/*
 * Copyright 2000-2012 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.openapi.roots.impl;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.fileTypes.ex.FileTypeManagerEx;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.StdModuleTypes;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.roots.ex.ProjectRootManagerEx;
import com.intellij.openapi.util.EmptyRunnable;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.*;
import com.intellij.testFramework.IdeaTestCase;
import com.intellij.testFramework.PlatformTestCase;
import com.intellij.testFramework.PsiTestUtil;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static java.util.Collections.singletonList;

@PlatformTestCase.WrapInCommand
public class DirectoryIndexImplTest extends IdeaTestCase {
  private static final Logger LOG = Logger.getInstance("#com.intellij.openapi.roots.impl.DirectoryIndexTest");

  private DirectoryIndex myIndex;

  private Module myModule2;
  private Module myModule3;
  private VirtualFile myRootVFile;
  private VirtualFile myModule1Dir;
  private VirtualFile myModule2Dir;
  private VirtualFile myModule3Dir;
  private VirtualFile mySrcDir1;
  private VirtualFile mySrcDir2;
  private VirtualFile myTestSrc1;
  private VirtualFile myPack1Dir;
  private VirtualFile myPack2Dir;
  private VirtualFile myFileLibDir;
  private VirtualFile myFileLibSrc;
  private VirtualFile myFileLibCls;
  private VirtualFile myLibDir;
  private VirtualFile myLibSrcDir;
  private VirtualFile myLibClsDir;
  private VirtualFile myCvsDir;
  private VirtualFile myExcludeDir;
  private VirtualFile myOutputDir;
  private VirtualFile myModule1OutputDir;

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    final File root = createTempDirectory();

    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      @Override
      public void run() {
        try {
          /*
            root
                lib
                    file.src
                    file.cls
                module1
                    src1
                        pack1
                        testSrc
                            pack2
                    lib
                        src
                        cls
                    module2
                        src2
                            CVS
                            excluded
                module3
                out
                    module1
          */
          myRootVFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(root);
          assertNotNull(myRootVFile);

          myFileLibDir = myRootVFile.createChildDirectory(DirectoryIndexImplTest.this, "lib");
          myFileLibSrc = myFileLibDir.createChildData(DirectoryIndexImplTest.this, "file.src");
          myFileLibCls = myFileLibDir.createChildData(DirectoryIndexImplTest.this, "file.cls");
          myModule1Dir = myRootVFile.createChildDirectory(DirectoryIndexImplTest.this, "module1");
          mySrcDir1 = myModule1Dir.createChildDirectory(DirectoryIndexImplTest.this, "src1");
          myPack1Dir = mySrcDir1.createChildDirectory(DirectoryIndexImplTest.this, "pack1");
          myTestSrc1 = mySrcDir1.createChildDirectory(DirectoryIndexImplTest.this, "testSrc");
          myPack2Dir = myTestSrc1.createChildDirectory(DirectoryIndexImplTest.this, "pack2");

          myLibDir = myModule1Dir.createChildDirectory(DirectoryIndexImplTest.this, "lib");
          myLibSrcDir = myLibDir.createChildDirectory(DirectoryIndexImplTest.this, "src");
          myLibClsDir = myLibDir.createChildDirectory(DirectoryIndexImplTest.this, "cls");
          myModule2Dir = myModule1Dir.createChildDirectory(DirectoryIndexImplTest.this, "module2");
          mySrcDir2 = myModule2Dir.createChildDirectory(DirectoryIndexImplTest.this, "src2");
          myCvsDir = mySrcDir2.createChildDirectory(DirectoryIndexImplTest.this, "CVS");
          myExcludeDir = mySrcDir2.createChildDirectory(DirectoryIndexImplTest.this, "excluded");

          myModule3Dir = myRootVFile.createChildDirectory(DirectoryIndexImplTest.this, "module3");

          myOutputDir = myRootVFile.createChildDirectory(DirectoryIndexImplTest.this, "out");
          myModule1OutputDir = myOutputDir.createChildDirectory(DirectoryIndexImplTest.this, "module1");

          getCompilerProjectExtension().setCompilerOutputUrl(myOutputDir.getUrl());
          ModuleManager moduleManager = ModuleManager.getInstance(myProject);

          // fill roots of module1
          {
            ModuleRootModificationUtil.setModuleSdk(myModule, null);
            PsiTestUtil.addContentRoot(myModule, myModule1Dir);
            PsiTestUtil.addSourceRoot(myModule, mySrcDir1);
            PsiTestUtil.addSourceRoot(myModule, myTestSrc1, true);
            ModuleRootModificationUtil.addModuleLibrary(myModule, "lib.js",
                                                        singletonList(myFileLibCls.getUrl()), singletonList(myFileLibSrc.getUrl()));
          }

          // fill roots of module2
          {
            VirtualFile moduleFile = myModule2Dir.createChildData(DirectoryIndexImplTest.this, "module2.iml");
            myModule2 = moduleManager.newModule(moduleFile.getPath(), StdModuleTypes.JAVA.getId());

            PsiTestUtil.addContentRoot(myModule2, myModule2Dir);
            PsiTestUtil.addSourceRoot(myModule2, mySrcDir2);
            PsiTestUtil.addExcludedRoot(myModule2, myExcludeDir);
            ModuleRootModificationUtil.addModuleLibrary(myModule2, "lib",
                                                        singletonList(myLibClsDir.getUrl()), singletonList(myLibSrcDir.getUrl()));
          }

          // fill roots of module3
          {
            VirtualFile moduleFile = myModule3Dir.createChildData(DirectoryIndexImplTest.this, "module3.iml");
            myModule3 = moduleManager.newModule(moduleFile.getPath(), StdModuleTypes.JAVA.getId());

            PsiTestUtil.addContentRoot(myModule3, myModule3Dir);
            ModuleRootModificationUtil.addDependency(myModule3, myModule2);
          }
        }
        catch (IOException e) {
          LOG.error(e);
        }
      }
    });

    myIndex = DirectoryIndex.getInstance(myProject);
  }

  private CompilerProjectExtension getCompilerProjectExtension() {
    final CompilerProjectExtension instance = CompilerProjectExtension.getInstance(myProject);
    assertNotNull(instance);
    return instance;
  }

  public void testDirInfos() {
    checkInfoNull(myRootVFile);

    // beware: files in directory index
    checkInfo(myFileLibSrc, null, false, false, false, true, "");
    checkInfo(myFileLibCls, null, false, false, true, false, "");

    checkInfo(myModule1Dir, myModule, false, false, false, false, null);
    checkInfo(mySrcDir1, myModule, true, false, false, false, "", myModule);
    checkInfo(myPack1Dir, myModule, true, false, false, false, "pack1", myModule);
    checkInfo(myTestSrc1, myModule, true, true, false, false, "", myModule);
    checkInfo(myPack2Dir, myModule, true, true, false, false, "pack2", myModule);

    checkInfo(myLibDir, myModule, false, false, false, false, null);
    checkInfo(myLibSrcDir, myModule, false, false, false, true, "", myModule2);
    checkInfo(myLibClsDir, myModule, false, false, true, false, "", myModule2);

    checkInfo(myModule2Dir, myModule2, false, false, false, false, null);
    checkInfo(mySrcDir2, myModule2, true, false, false, false, "", myModule2, myModule3);
    checkInfoNull(myCvsDir);
    checkInfoNull(myExcludeDir);

    checkInfo(myModule3Dir, myModule3, false, false, false, false, null);
  }

  public void testDirsByPackageName() {
    checkPackage("", myFileLibSrc, myFileLibCls, mySrcDir1, myTestSrc1, myLibSrcDir, myLibClsDir, mySrcDir2);
    checkPackage("pack1", myPack1Dir);
    checkPackage("pack2", myPack2Dir);
  }

  public void testCreateDir() throws Exception {
    String path = mySrcDir1.getPath().replace('/', File.separatorChar);
    assertTrue(new File(path + File.separatorChar + "dir1" + File.separatorChar + "dir2").mkdirs());
    assertTrue(new File(path + File.separatorChar + "CVS").mkdirs());
    VirtualFileManager.getInstance().refresh(false);

    myIndex.checkConsistency();
  }

  public void testDeleteDir() throws Exception {
    VirtualFile subdir1 = mySrcDir1.createChildDirectory(this, "subdir1");
    VirtualFile subdir2 = subdir1.createChildDirectory(this, "subdir2");
    subdir2.createChildDirectory(this, "subdir3");

    myIndex.checkConsistency();

    subdir1.delete(this);

    myIndex.checkConsistency();
  }

  public void testMoveDir() throws Exception {
    VirtualFile subdir = mySrcDir2.createChildDirectory(this, "subdir1");
    subdir.createChildDirectory(this, "subdir2");

    myIndex.checkConsistency();

    subdir.move(this, mySrcDir1);

    myIndex.checkConsistency();
  }

  public void testRenameDir() throws Exception {
    VirtualFile subdir = mySrcDir2.createChildDirectory(this, "subdir1");
    subdir.createChildDirectory(this, "subdir2");

    myIndex.checkConsistency();

    subdir.rename(this, "abc.d");

    myIndex.checkConsistency();
  }

  public void testRenameRoot() throws Exception {
    myModule1Dir.rename(this, "newName");

    myIndex.checkConsistency();
  }

  public void testMoveRoot() throws Exception {
    myModule1Dir.move(this, myModule3Dir);

    myIndex.checkConsistency();
  }

  public void testAddProjectDir() throws Exception {
    new WriteCommandAction.Simple(getProject()) {
      @Override
      protected void run() throws Throwable {
        VirtualFile newDir = myModule1Dir.getParent().createChildDirectory(DirectoryIndexImplTest.this, "newDir");
        newDir.createChildDirectory(DirectoryIndexImplTest.this, "subdir");

        myIndex.checkConsistency();
        PsiTestUtil.addContentRoot(myModule, newDir);
      }
    }.execute().throwException();


    myIndex.checkConsistency();
  }

  public void testChangeIgnoreList() throws Exception {
    myModule1Dir.createChildDirectory(this, "newDir");

    myIndex.checkConsistency();

    final FileTypeManagerEx fileTypeManager = (FileTypeManagerEx)FileTypeManager.getInstance();
    final String list = fileTypeManager.getIgnoredFilesList();
    final String list1 = list + ";" + "newDir";
    try {
      ApplicationManager.getApplication().runWriteAction(new Runnable() {
        @Override
        public void run() {
          fileTypeManager.setIgnoredFilesList(list1);
        }
      });

      myIndex.checkConsistency();
    }
    finally {
      ApplicationManager.getApplication().runWriteAction(new Runnable() {
        @Override
        public void run() {
          fileTypeManager.setIgnoredFilesList(list);
        }
      });
    }
  }

  public void testAddModule() throws Exception {
    myIndex.checkConsistency();

    new WriteCommandAction.Simple(getProject()) {
      @Override
      protected void run() throws Throwable {
        VirtualFile newModuleContent = myRootVFile.createChildDirectory(DirectoryIndexImplTest.this, "newModule");
        newModuleContent.createChildDirectory(DirectoryIndexImplTest.this, "subDir");
        ModuleManager moduleManager = ModuleManager.getInstance(myProject);
        Module module = moduleManager.newModule(myRootVFile.getPath() + "/newModule.iml", StdModuleTypes.JAVA.getId());
        PsiTestUtil.addContentRoot(module, newModuleContent);
      }
    }.execute().throwException();


    myIndex.checkConsistency();
  }

  public void testExplicitExcludeOfInner() throws Exception {
    PsiTestUtil.addExcludedRoot(myModule, myModule2Dir);

    myIndex.checkConsistency();

    checkInfo(myModule2Dir, myModule2, false, false, false, false, null);
    checkInfo(mySrcDir2, myModule2, true, false, false, false, "", myModule2, myModule3);
  }

  public void testResettingProjectOutputPath() throws Exception {
    VirtualFile output1 = myModule1Dir.createChildDirectory(this, "output1");
    VirtualFile output2 = myModule1Dir.createChildDirectory(this, "output2");

    assertNotNull(myIndex.getInfoForDirectory(output1));
    assertNotNull(myIndex.getInfoForDirectory(output2));

    getCompilerProjectExtension().setCompilerOutputUrl(output1.getUrl());
    fireRootsChanged();

    assertNull(myIndex.getInfoForDirectory(output1));
    assertNotNull(myIndex.getInfoForDirectory(output2));

    getCompilerProjectExtension().setCompilerOutputUrl(output2.getUrl());
    fireRootsChanged();

    assertNotNull(myIndex.getInfoForDirectory(output1));
    assertNull(myIndex.getInfoForDirectory(output2));
  }

  private void fireRootsChanged() {
    ProjectRootManagerEx.getInstanceEx(getProject()).makeRootsChange(EmptyRunnable.getInstance(), false, true);
  }

  public void testExcludedDirShouldBeExcludedRightAfterItsCreation() throws Exception {
    VirtualFile excluded = myModule1Dir.createChildDirectory(this, "excluded");
    VirtualFile projectOutput = myModule1Dir.createChildDirectory(this, "projectOutput");
    VirtualFile module2Output = myModule1Dir.createChildDirectory(this, "module2Output");
    VirtualFile module2TestOutput = myModule2Dir.createChildDirectory(this, "module2TestOutput");

    assertNotNull(myIndex.getInfoForDirectory(excluded));
    assertNotNull(myIndex.getInfoForDirectory(projectOutput));
    assertNotNull(myIndex.getInfoForDirectory(module2Output));
    assertNotNull(myIndex.getInfoForDirectory(module2TestOutput));

    getCompilerProjectExtension().setCompilerOutputUrl(projectOutput.getUrl());

    PsiTestUtil.addExcludedRoot(myModule, excluded);
    PsiTestUtil.setCompilerOutputPath(myModule2, module2Output.getUrl(), false);
    PsiTestUtil.setCompilerOutputPath(myModule2, module2TestOutput.getUrl(), true);
    PsiTestUtil.setExcludeCompileOutput(myModule2, true);

    assertNull(myIndex.getInfoForDirectory(excluded));
    assertNull(myIndex.getInfoForDirectory(projectOutput));
    assertNull(myIndex.getInfoForDirectory(module2Output));
    assertNull(myIndex.getInfoForDirectory(module2TestOutput));

    excluded.delete(this);
    projectOutput.delete(this);
    module2Output.delete(this);
    module2TestOutput.delete(this);

    final List<Boolean> isExcluded = new ArrayList<Boolean>();
    VirtualFileListener l = new VirtualFileAdapter() {
      @Override
      public void fileCreated(VirtualFileEvent e) {
        isExcluded.add(myIndex.getInfoForDirectory(e.getFile()) == null);
      }
    };
    VirtualFileManager.getInstance().addVirtualFileListener(l);
    try {
      excluded = myModule1Dir.createChildDirectory(this, excluded.getName());
      projectOutput = myModule1Dir.createChildDirectory(this, projectOutput.getName());
      module2Output = myModule1Dir.createChildDirectory(this, module2Output.getName());
      module2TestOutput = myModule2Dir.createChildDirectory(this, module2TestOutput.getName());
    }
    finally {
      VirtualFileManager.getInstance().removeVirtualFileListener(l);
    }

    assertNull(myIndex.getInfoForDirectory(excluded));
    assertNull(myIndex.getInfoForDirectory(projectOutput));
    assertNull(myIndex.getInfoForDirectory(module2Output));
    assertNull(myIndex.getInfoForDirectory(module2TestOutput));

    assertEquals(4, isExcluded.size());
    assertTrue(isExcluded.get(0));
    assertTrue(isExcluded.get(1));
    assertTrue(isExcluded.get(2));
    assertTrue(isExcluded.get(3));
  }

  public void testExcludesShouldBeRecognizedRightOnRefresh() throws Exception {
    final VirtualFile dir = myModule1Dir.createChildDirectory(this, "dir");
    final VirtualFile excluded = dir.createChildDirectory(this, "excluded");
    PsiTestUtil.addExcludedRoot(myModule, excluded);
    new WriteCommandAction.Simple(getProject()) {
      @Override
      protected void run() throws Throwable {
        dir.delete(DirectoryIndexImplTest.this);
      }
    }.execute().throwException();


    boolean created = new File(myModule1Dir.getPath(), "dir/excluded/foo").mkdirs();
    assertTrue(created);

    final List<Boolean> toAssert = new ArrayList<Boolean>();
    VirtualFileListener l = new VirtualFileAdapter() {
      @Override
      public void fileCreated(VirtualFileEvent e) {
        assertEquals("dir", e.getFileName());

        toAssert.add(myIndex.getInfoForDirectory(e.getFile()) != null);
        toAssert.add(myIndex.getInfoForDirectory(e.getFile().findFileByRelativePath("excluded")) == null);
        toAssert.add(myIndex.getInfoForDirectory(e.getFile().findFileByRelativePath("excluded/foo")) == null);
      }
    };

    VirtualFileManager.getInstance().addVirtualFileListener(l);
    try {
      VirtualFileManager.getInstance().refresh(false);
    }
    finally {
      VirtualFileManager.getInstance().removeVirtualFileListener(l);
    }

    assertEquals(Arrays.asList(true, true, true), toAssert);
  }

  public void testProcessingNestedContentRootsOfExcludedDirsOnCreation() {
    String rootPath = myModule1Dir.getPath();
    final File f = new File(rootPath, "excludedDir/dir/anotherContentRoot");
    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      @Override
      public void run() {
        ModifiableRootModel rootModel = ModuleRootManager.getInstance(myModule).getModifiableModel();
        rootModel.getContentEntries()[0]
          .addExcludeFolder(VfsUtilCore.pathToUrl(FileUtil.toSystemIndependentName(f.getParentFile().getParent())));
        rootModel.commit();

        rootModel = ModuleRootManager.getInstance(myModule2).getModifiableModel();
        rootModel.addContentEntry(VfsUtilCore.pathToUrl(FileUtil.toSystemIndependentName(f.getPath())));
        rootModel.commit();

        assertTrue(f.getPath(), f.exists() || f.mkdirs());
        LocalFileSystem.getInstance().refresh(false);
      }
    });


    assertNull(myIndex.getInfoForDirectory(LocalFileSystem.getInstance().findFileByIoFile(f.getParentFile().getParentFile())));
    assertNotNull(myIndex.getInfoForDirectory(LocalFileSystem.getInstance().findFileByIoFile(f)));
  }

  public void testLibraryDirInContent() throws Exception {
    ModuleRootModificationUtil.addModuleLibrary(myModule, myModule1Dir.getUrl());

    myIndex.checkConsistency();

    checkInfo(myModule1Dir, myModule, false, false, true, false, "", myModule);
    checkInfo(mySrcDir1, myModule, true, false, true, false, "", myModule);
  }


  public void testExcludeCompilerOutputOutsideOfContentRoot() throws Exception {
    final ProjectFileIndex fileIndex = ProjectRootManager.getInstance(myProject).getFileIndex();
    assertTrue(fileIndex.isIgnored(myOutputDir));
    assertTrue(fileIndex.isIgnored(myModule1OutputDir));
    assertFalse(fileIndex.isIgnored(myOutputDir.getParent()));
  }

  private void checkInfo(VirtualFile dir,
                         @Nullable Module module,
                         boolean isInModuleSource,
                         boolean isTestSource,
                         boolean isInLibrary,
                         boolean isInLibrarySource,
                         @Nullable String packageName,
                         Module... modulesOfOrderEntries) {
    DirectoryInfo info = myIndex.getInfoForDirectory(dir);
    assertNotNull(info);
    assertEquals(module, info.module);
    assertEquals(isInModuleSource, info.isInModuleSource);
    assertEquals(isTestSource, info.isTestSource);
    assertEquals(isInLibrary, info.libraryClassRoot != null);
    assertEquals(isInLibrarySource, info.isInLibrarySource);

    final ProjectFileIndex fileIndex = ProjectRootManager.getInstance(myProject).getFileIndex();
    if (dir.isDirectory()) {
      assertEquals(packageName, fileIndex.getPackageNameByDirectory(dir));
    }

    assertEquals(modulesOfOrderEntries.length, info.getOrderEntries().size());
    for (Module aModule : modulesOfOrderEntries) {
      boolean found = false;
      for (OrderEntry orderEntry : info.getOrderEntries()) {
        if (orderEntry.getOwnerModule() == aModule) {
          found = true;
          break;
        }
      }
      assertTrue(found);
    }
  }

  private void checkInfoNull(VirtualFile dir) {
    DirectoryInfo info = myIndex.getInfoForDirectory(dir);
    assertNull(info);
  }

  private void checkPackage(String packageName, VirtualFile... expectedDirs) {
    VirtualFile[] actualDirs = myIndex.getDirectoriesByPackageName(packageName, true).toArray(VirtualFile.EMPTY_ARRAY);
    assertNotNull(actualDirs);
    HashSet<VirtualFile> set1 = new HashSet<VirtualFile>();
    ContainerUtil.addAll(set1, expectedDirs);
    HashSet<VirtualFile> set2 = new HashSet<VirtualFile>();
    ContainerUtil.addAll(set2, actualDirs);
    assertEquals(set1, set2);
  }
}
