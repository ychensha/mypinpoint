/**
 * Copyright 2014 NAVER Corp. Licensed under the Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the License. You may obtain a copy of the License
 * at <p/> http://www.apache.org/licenses/LICENSE-2.0 <p/> Unless required by applicable law or
 * agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.navercorp.pinpoint.plugin.spring.beans;

import com.navercorp.pinpoint.bootstrap.config.ProfilerConfig;
import com.navercorp.pinpoint.bootstrap.instrument.InstrumentClass;
import com.navercorp.pinpoint.bootstrap.instrument.InstrumentException;
import com.navercorp.pinpoint.bootstrap.instrument.InstrumentMethod;
import com.navercorp.pinpoint.bootstrap.instrument.Instrumentor;
import com.navercorp.pinpoint.bootstrap.instrument.transformer.TransformCallback;
import com.navercorp.pinpoint.bootstrap.instrument.transformer.TransformTemplate;
import com.navercorp.pinpoint.bootstrap.instrument.transformer.TransformTemplateAware;
import com.navercorp.pinpoint.bootstrap.plugin.ObjectFactory;
import com.navercorp.pinpoint.bootstrap.plugin.ProfilerPlugin;
import com.navercorp.pinpoint.bootstrap.plugin.ProfilerPluginSetupContext;

import java.security.ProtectionDomain;

import static com.navercorp.pinpoint.common.util.VarArgs.va;

/**
 * @author Jongho Moon
 */
public class SpringBeansPlugin implements ProfilerPlugin, TransformTemplateAware {

  public static final String SPRING_BEANS_MARK_ERROR = "profiler.spring.beans.mark.error";

  private TransformTemplate transformTemplate;

  @Override
  public void setup(ProfilerPluginSetupContext context) {
    addAbstractAutowireCapableBeanFactoryTransformer(context);
  }

  private void addAbstractAutowireCapableBeanFactoryTransformer(
      final ProfilerPluginSetupContext context) {
    final ProfilerConfig config = context.getConfig();
    final boolean errorMark = config.readBoolean(SPRING_BEANS_MARK_ERROR, false);

    transformTemplate
        .transform("org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory",
            new TransformCallback() {

              @Override
              public byte[] doInTransform(Instrumentor instrumentor, ClassLoader loader,
                                          String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
                                          byte[] classfileBuffer) throws InstrumentException {
                InstrumentClass target =
                    instrumentor.getInstrumentClass(loader, className, classfileBuffer);

                final BeanMethodTransformer beanTransformer = new BeanMethodTransformer(errorMark);
                final ObjectFactory beanFilterFactory = ObjectFactory.byStaticFactory(
                    "com.navercorp.pinpoint.plugin.spring.beans.interceptor.TargetBeanFilter", "of",
                    config);

                final InstrumentMethod createBeanInstance = target
                    .getDeclaredMethod("createBeanInstance", "java.lang.String",
                        "org.springframework.beans.factory.support.RootBeanDefinition",
                        "java.lang.Object[]");
                createBeanInstance.addInterceptor(
                    "com.navercorp.pinpoint.plugin.spring.beans.interceptor.CreateBeanInstanceInterceptor",
                    va(beanTransformer, beanFilterFactory));

                final InstrumentMethod postProcessor = target
                    .getDeclaredMethod("applyBeanPostProcessorsBeforeInstantiation",
                        "java.lang.Class", "java.lang.String");
                postProcessor.addInterceptor(
                    "com.navercorp.pinpoint.plugin.spring.beans.interceptor.PostProcessorInterceptor",
                    va(beanTransformer, beanFilterFactory));

                return target.toBytecode();
              }
            });

  }

  @Override
  public void setTransformTemplate(TransformTemplate transformTemplate) {
    this.transformTemplate = transformTemplate;
  }
}
