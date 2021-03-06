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
package com.intellij.ide.favoritesTreeView.smartPointerPsiNodes;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiSubstitutor;
import com.intellij.psi.SmartPointerManager;
import com.intellij.psi.SmartPsiElementPointer;
import com.intellij.psi.util.PsiFormatUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;

public class FieldSmartPointerNode extends BaseSmartPointerPsiNode<SmartPsiElementPointer>{

  public FieldSmartPointerNode(Project project, PsiField value, ViewSettings viewSettings) {
    super(project, SmartPointerManager.getInstance(project).createLazyPointer(value), viewSettings);
  }

  public FieldSmartPointerNode(final Project project, final Object value, final ViewSettings viewSettings) {
    this(project, (PsiField)value, viewSettings);
  }

  @NotNull
  public Collection<AbstractTreeNode> getChildrenImpl() {
    return Collections.emptyList();
  }

  public void updateImpl(PresentationData data) {
    String name = PsiFormatUtil.formatVariable(
      (PsiField)getPsiElement(),
      PsiFormatUtil.SHOW_NAME | PsiFormatUtil.SHOW_TYPE | PsiFormatUtil.TYPE_AFTER | PsiFormatUtil.SHOW_INITIALIZER,
        PsiSubstitutor.EMPTY);
    int c = name.indexOf('\n');
    if (c > -1) {
      name = name.substring(0, c - 1);
    }
    data.setPresentableText(name);
  }
}
