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
package com.intellij.debugger;

import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.PsiDocumentManager;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class DebuggerInvocationUtil {
  public static void swingInvokeLater(final Project project, @NotNull final Runnable runnable) {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        if (project != null && !project.isDisposed()) {
          runnable.run();
        }
      }
    });
  }
  public static void invokeLater(final Project project, @NotNull final Runnable runnable) {
    ApplicationManager.getApplication().invokeLater(new Runnable() {
      public void run() {
        if (project != null && !project.isDisposed()) {
          runnable.run();
        }
      }
    });
  }

  public static void invokeLater(final Project project, @NotNull final Runnable runnable, ModalityState state) {
    ApplicationManager.getApplication().invokeLater(new Runnable() {
      public void run() {
        if(project == null || project.isDisposed()) return;

        runnable.run();
      }
    }, state);
  }

  public static void invokeAndWait(final Project project, @NotNull final Runnable runnable, ModalityState state) {
    ApplicationManager.getApplication().invokeAndWait(new Runnable() {
      public void run() {
        if(project == null || project.isDisposed()) return;

        runnable.run();
      }
    }, state);
  }

  public static  <T> T commitAndRunReadAction(Project project, final EvaluatingComputable<T> computable) throws EvaluateException {
    final Throwable[] ex = new Throwable[] { null };
    T result = PsiDocumentManager.getInstance(project).commitAndRunReadAction(new Computable<T>() {
          public T compute() {
            try {
              return computable.compute();
            }
            catch (RuntimeException e) {
              ex[0] = e;
            }
            catch (Exception th) {
              ex[0] = th;
            }

            return null;
          }
        });

    if(ex[0] != null) {
      if(ex[0] instanceof RuntimeException) {
        throw (RuntimeException)ex[0];
      }
      else {
        throw (EvaluateException) ex[0];
      }
    }

    return result;
  }
}
