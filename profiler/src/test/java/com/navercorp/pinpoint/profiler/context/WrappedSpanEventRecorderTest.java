/*
 * *
 *  * Copyright 2014 NAVER Corp.
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package com.navercorp.pinpoint.profiler.context;

import com.navercorp.pinpoint.bootstrap.context.TraceContext;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;


/**
 * @author Woonduk Kang(emeroad)
 */
public class WrappedSpanEventRecorderTest {


  @Test public void testSetExceptionInfo_RootMarkError() throws Exception {
    Span span = new Span();
    SpanEvent spanEvent = new SpanEvent(span);
    TraceContext traceContext = Mockito.mock(TraceContext.class);

    WrappedSpanEventRecorder recorder = new WrappedSpanEventRecorder(traceContext);
    recorder.setWrapped(spanEvent);

    final String exceptionMessage1 = "exceptionMessage1";
    final Exception exception1 = new Exception(exceptionMessage1);
    recorder.recordException(false, exception1);

    Assert.assertEquals("Exception recoding", exceptionMessage1,
        spanEvent.getExceptionInfo().getStringValue());
    Assert.assertFalse("markRootError=false", span.isSetErrCode());


    final String exceptionMessage2 = "exceptionMessage2";
    final Exception exception2 = new Exception(exceptionMessage2);
    recorder.recordException(true, exception2);

    Assert.assertEquals("Exception recoding", exceptionMessage2,
        spanEvent.getExceptionInfo().getStringValue());
    Assert.assertTrue("markRootError=true", span.isSetErrCode());
  }


}
