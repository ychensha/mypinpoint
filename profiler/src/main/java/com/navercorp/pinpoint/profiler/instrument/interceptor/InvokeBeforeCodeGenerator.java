/**
 * Copyright 2014 NAVER Corp.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.navercorp.pinpoint.profiler.instrument.interceptor;

import com.navercorp.pinpoint.bootstrap.context.TraceContext;
import com.navercorp.pinpoint.bootstrap.instrument.InstrumentClass;
import com.navercorp.pinpoint.bootstrap.instrument.InstrumentMethod;

import java.lang.reflect.Method;

/**
 * @author Jongho Moon
 *
 */
public class InvokeBeforeCodeGenerator extends InvokeCodeGenerator {
  private final int interceptorId;
  private final InstrumentClass targetClass;

  public InvokeBeforeCodeGenerator(int interceptorId, InterceptorDefinition interceptorDefinition,
      InstrumentClass targetClass, InstrumentMethod targetMethod, TraceContext traceContext) {
    super(interceptorId, interceptorDefinition, targetMethod, traceContext);

    this.interceptorId = interceptorId;
    this.targetClass = targetClass;
  }

  public String generate() {
    final CodeBuilder builder = new CodeBuilder();

    builder.begin();

    builder.append("try { ");
    builder.format("%1$s = %2$s.getInterceptor(%3$d); ", getInterceptorVar(),
        getInterceptorRegistryClassName(), interceptorId);

    final Method beforeMethod = interceptorDefinition.getBeforeMethod();
    if (beforeMethod != null) {
      builder.format("((%1$s)%2$s).before(", getInterceptorType(), getInterceptorVar());
      appendArguments(builder);
      builder.format(");");
    }

    builder.format(
        "} catch (java.lang.Throwable _$PINPOINT_EXCEPTION$_) { %1$s.handleException(_$PINPOINT_EXCEPTION$_); }",
        getInterceptorInvokerHelperClassName());

    builder.end();

    return builder.toString();
  }

  private void appendArguments(CodeBuilder builder) {
    final InterceptorType type = interceptorDefinition.getInterceptorType();
    switch (type) {
      case ARRAY_ARGS:
        appendSimpleBeforeArguments(builder);
        break;
      case STATIC:
        appendStaticBeforeArguments(builder);
        break;
      case API_ID_AWARE:
        appendApiIdAwareBeforeArguments(builder);
        break;
      case BASIC:
        appendCustomBeforeArguments(builder);
        break;
    }
  }

  private void appendSimpleBeforeArguments(CodeBuilder builder) {
    builder.format("%1$s, %2$s", getTarget(), getArguments());
  }

  private void appendStaticBeforeArguments(CodeBuilder builder) {
    builder.format("%1$s, \"%2$s\", \"%3$s\", \"%4$s\", %5$s", getTarget(), targetClass.getName(),
        targetMethod.getName(), getParameterTypes(), getArguments());
  }

  private void appendApiIdAwareBeforeArguments(CodeBuilder builder) {
    builder.format("%1$s, %2$d, %3$s", getTarget(), getApiId(), getArguments());
  }

  private void appendCustomBeforeArguments(CodeBuilder builder) {
    final Method interceptorMethod = interceptorDefinition.getBeforeMethod();
    final Class<?>[] paramTypes = interceptorMethod.getParameterTypes();

    if (paramTypes.length == 0) {
      return;
    }

    builder.append(getTarget());

    int i = 0;
    int argNum = targetMethod.getParameterTypes().length;
    int interceptorArgNum = paramTypes.length - 1;
    int matchNum = Math.min(argNum, interceptorArgNum);

    for (; i < matchNum; i++) {
      builder.append(", ($w)$" + (i + 1));
    }

    for (; i < interceptorArgNum; i++) {
      builder.append(", null");
    }
  }
}
