/*
 * Copyright 2012-2015 Comcast Cable Communications Management, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.comcast.money.spring3;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.comcast.money.core.BooleanNote;
import com.comcast.money.core.DoubleNote;
import com.comcast.money.core.LongNote;
import com.comcast.money.core.Note;
import com.comcast.money.core.StringNote;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:test-context.xml")
public class TracedMethodInterceptorSpec {

    @Autowired
    private SampleTraceBean sampleTraceBean;

    // This bean is intercepted by springockito, so it is actually a mock!  Living the life!
    @Autowired
    private SpringTracer springTracer;

    @Captor
    private ArgumentCaptor<Note<Object>> spanResultCaptor;

    @Before
    public void setUp() {
        // Needed to init the Argument Captor
        MockitoAnnotations.initMocks(this);
    }

    @After
    public void tearDown() {
        // Reset the mocks so we can continue to do verifies across tests
        reset(springTracer);
    }

    @Test
    public void testTracing() throws Exception {

        sampleTraceBean.doSomethingGood();
        verify(springTracer).startSpan("SampleTrace");
        verify(springTracer).record("foo", "bar", false);
        verifySpanResultsIn(true);
    }

    @Test
    public void testTracedDataParamsWithValues() throws Exception {

        ArgumentCaptor<Note> noteCaptor = ArgumentCaptor.forClass(Note.class);

        sampleTraceBean.doSomethingWithTracedParams("tp", true, 200L, 3.14);
        verify(springTracer, times(4)).record(noteCaptor.capture(), anyBoolean());

        StringNote stringNote = (StringNote)noteCaptor.getAllValues().get(0);
        assertThat(stringNote.name()).isEqualTo("STRING");
        assertThat(stringNote.value().get()).isEqualTo("tp");

        BooleanNote booleanNote = (BooleanNote)noteCaptor.getAllValues().get(1);
        assertThat(booleanNote.name()).isEqualTo("BOOLEAN");
        assertThat(booleanNote.value().get()).isEqualTo(true);

        LongNote longNote = (LongNote)noteCaptor.getAllValues().get(2);
        assertThat(longNote.name()).isEqualTo("LONG");
        assertThat(longNote.value().get()).isEqualTo(200L);

        DoubleNote doubleNote = (DoubleNote)noteCaptor.getAllValues().get(3);
        assertThat(doubleNote.name()).isEqualTo("DOUBLE");
        assertThat(doubleNote.value().get()).isEqualTo(3.14);
    }

    @Test
    public void testTracedDataParamsWithNullValues() throws Exception {

        ArgumentCaptor<Note> noteCaptor = ArgumentCaptor.forClass(Note.class);

        sampleTraceBean.doSomethingWithTracedParams(null, null, null, null);
        verify(springTracer, times(4)).record(noteCaptor.capture(), anyBoolean());

        StringNote stringNote = (StringNote)noteCaptor.getAllValues().get(0);
        assertThat(stringNote.name()).isEqualTo("STRING");
        assertThat(stringNote.value().isEmpty()).isEqualTo(true);

        BooleanNote booleanNote = (BooleanNote)noteCaptor.getAllValues().get(1);
        assertThat(booleanNote.name()).isEqualTo("BOOLEAN");
        assertThat(booleanNote.value().isEmpty()).isEqualTo(true);

        LongNote longNote = (LongNote)noteCaptor.getAllValues().get(2);
        assertThat(longNote.name()).isEqualTo("LONG");
        assertThat(longNote.value().isEmpty()).isEqualTo(true);

        DoubleNote doubleNote = (DoubleNote)noteCaptor.getAllValues().get(3);
        assertThat(doubleNote.name()).isEqualTo("DOUBLE");
        assertThat(doubleNote.value().isEmpty()).isEqualTo(true);
    }

    @Test
    public void testTracingRecordsFailureOnException() throws Exception {

        try {
            sampleTraceBean.doSomethingBad();
        }
        catch (Exception ex) {

        }
        verify(springTracer).startSpan("SampleTrace");
        verify(springTracer).record("foo", "bar", false);
        verifySpanResultsIn(false);
    }

    @Test
    public void testTracingDoesNotTraceMethodsWithoutAnnotation() {

        sampleTraceBean.doSomethingNotTraced();
        verifyZeroInteractions(springTracer);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTracingIgnoresException() {
        sampleTraceBean.doSomethingButIgnoreException();
        verifySpanResultsIn(true);
    }

    private void verifySpanResultsIn(Boolean result) {

        verify(springTracer).stopSpan(spanResultCaptor.capture());
        Note<Object> spanResult = spanResultCaptor.getValue();
        assertThat(spanResult.value().get()).isEqualTo(result);
    }
}
