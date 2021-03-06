/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

/*
 * User: anna
 * Date: 15-Jan-2008
 */
package com.intellij.analysis;

import com.intellij.ide.impl.dataRules.GetDataRule;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.search.GlobalSearchScope;

public class AnalysisScopeRule implements GetDataRule {
  @Override
  public Object getData(final DataProvider dataProvider) {
    final Object psiFile = dataProvider.getData(LangDataKeys.PSI_FILE.getName());
    if (psiFile instanceof PsiJavaFile) {
      return new JavaAnalysisScope((PsiJavaFile)psiFile);
    }
    Object psiTarget = dataProvider.getData(LangDataKeys.PSI_ELEMENT.getName());
    if (psiTarget instanceof PsiPackage) {
      PsiPackage pack = (PsiPackage)psiTarget;
      if (!pack.getManager().isInProject(pack)) return null;
      PsiDirectory[] dirs = pack.getDirectories(GlobalSearchScope.projectScope(pack.getProject()));
      if (dirs.length == 0) return null;
      return new JavaAnalysisScope(pack, (Module)dataProvider.getData(LangDataKeys.MODULE.getName()));
    }
    return null;
  }
}