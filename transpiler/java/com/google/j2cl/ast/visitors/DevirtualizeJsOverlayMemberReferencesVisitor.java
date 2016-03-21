/*
 * Copyright 2015 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.j2cl.ast.visitors;

import com.google.j2cl.ast.AbstractRewriter;
import com.google.j2cl.ast.AstUtils;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.FieldAccess;
import com.google.j2cl.ast.FieldDescriptor;
import com.google.j2cl.ast.FieldDescriptorBuilder;
import com.google.j2cl.ast.MethodCall;
import com.google.j2cl.ast.Node;
import com.google.j2cl.ast.TypeDescriptor;

/**
 * Replaces member references (method call and field access) to static JsOverlay members with
 * references to the method and field in the corresponding Overlay type impl.
 */
public class DevirtualizeJsOverlayMemberReferencesVisitor extends AbstractRewriter {
  public static void applyTo(CompilationUnit compilationUnit) {
    new DevirtualizeJsOverlayMemberReferencesVisitor()
        .devirtualizeJsOverlayMemberReferences(compilationUnit);
  }

  private void devirtualizeJsOverlayMemberReferences(CompilationUnit compilationUnit) {
    compilationUnit.accept(this);
  }

  @Override
  public Node rewriteFieldAccess(FieldAccess fieldAccess) {
    FieldDescriptor target = fieldAccess.getTarget();
    if (target.isStatic() && target.isJsOverlay()) {
      TypeDescriptor overlayTypeDescriptor =
          AstUtils.createJsOverlayImplTypeDescriptor(target.getEnclosingClassTypeDescriptor());
      return new FieldAccess(
          null, FieldDescriptorBuilder.from(target).enclosingClass(overlayTypeDescriptor).build());
    }
    return fieldAccess;
  }

  @Override
  public Node rewriteMethodCall(MethodCall methodCall) {
    if (!methodCall.getTarget().isJsOverlay()) {
      return methodCall;
    }
    TypeDescriptor originalTypeDescriptor =
        methodCall.getTarget().getEnclosingClassTypeDescriptor();
    TypeDescriptor overlayTypeDescriptor =
        AstUtils.createJsOverlayImplTypeDescriptor(originalTypeDescriptor);
    if (methodCall.getTarget().isStatic()) {
      // Devirtualize *static* JsOverlay method.
      return MethodCall.Builder.from(methodCall)
          .enclosingClass(overlayTypeDescriptor)
          .qualifier(null)
          .build();
    } else {
      // Devirtualize *instance* JsOverlay method.
      return AstUtils.createDevirtualizedMethodCall(methodCall, overlayTypeDescriptor);
    }
  }
}
