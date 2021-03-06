/*
 *  * Copyright 2016 Skymind, Inc.
 *  *
 *  *    Licensed under the Apache License, Version 2.0 (the "License");
 *  *    you may not use this file except in compliance with the License.
 *  *    You may obtain a copy of the License at
 *  *
 *  *        http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  *    Unless required by applicable law or agreed to in writing, software
 *  *    distributed under the License is distributed on an "AS IS" BASIS,
 *  *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *    See the License for the specific language governing permissions and
 *  *    limitations under the License.
 */

package org.datavec.spark.transform;

import org.apache.spark.api.java.JavaRDD;
import org.datavec.api.transform.MathOp;
import org.datavec.api.transform.TransformProcess;
import org.datavec.api.transform.schema.Schema;
import org.datavec.api.transform.schema.SequenceSchema;
import org.datavec.api.writable.DoubleWritable;
import org.datavec.api.writable.IntWritable;
import org.datavec.api.writable.Text;
import org.datavec.api.writable.Writable;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertEquals;

/**
 * Created by Alex on 25/11/2016.
 */
public class ExecutionTest extends BaseSparkTest {

    @Test
    public void testExecutionSimple(){
        Schema schema = new Schema.Builder()
                .addColumnInteger("col0")
                .addColumnCategorical("col1","state0","state1","state2")
                .addColumnDouble("col2")
                .build();

        TransformProcess tp = new TransformProcess.Builder(schema)
                .categoricalToInteger("col1")
                .doubleMathOp("col2", MathOp.Add, 10.0)
                .build();

        List<List<Writable>> inputData = new ArrayList<>();
        inputData.add(Arrays.<Writable>asList(new IntWritable(0), new Text("state2"), new DoubleWritable(0.1)));
        inputData.add(Arrays.<Writable>asList(new IntWritable(1), new Text("state1"), new DoubleWritable(1.1)));
        inputData.add(Arrays.<Writable>asList(new IntWritable(2), new Text("state0"), new DoubleWritable(2.1)));

        JavaRDD<List<Writable>> rdd = sc.parallelize(inputData);

        List<List<Writable>> out = new ArrayList<>(SparkTransformExecutor.execute(rdd, tp).collect());

        Collections.sort(out, new Comparator<List<Writable>>() {
            @Override
            public int compare(List<Writable> o1, List<Writable> o2) {
                return Integer.compare(o1.get(0).toInt(), o2.get(0).toInt());
            }
        });

        List<List<Writable>> expected = new ArrayList<>();
        expected.add(Arrays.<Writable>asList(new IntWritable(0), new IntWritable(2), new DoubleWritable(10.1)));
        expected.add(Arrays.<Writable>asList(new IntWritable(1), new IntWritable(1), new DoubleWritable(11.1)));
        expected.add(Arrays.<Writable>asList(new IntWritable(2), new IntWritable(0), new DoubleWritable(12.1)));

        assertEquals(expected, out);
    }

    @Test
    public void testExecutionSequence(){

        Schema schema = new SequenceSchema.Builder()
                .addColumnInteger("col0")
                .addColumnCategorical("col1","state0","state1","state2")
                .addColumnDouble("col2")
                .build();

        TransformProcess tp = new TransformProcess.Builder(schema)
                .categoricalToInteger("col1")
                .doubleMathOp("col2", MathOp.Add, 10.0)
                .build();

        List<List<List<Writable>>> inputSequences = new ArrayList<>();
        List<List<Writable>> seq1 = new ArrayList<>();
        seq1.add(Arrays.<Writable>asList(new IntWritable(0), new Text("state2"), new DoubleWritable(0.1)));
        seq1.add(Arrays.<Writable>asList(new IntWritable(1), new Text("state1"), new DoubleWritable(1.1)));
        seq1.add(Arrays.<Writable>asList(new IntWritable(2), new Text("state0"), new DoubleWritable(2.1)));
        List<List<Writable>> seq2 = new ArrayList<>();
        seq2.add(Arrays.<Writable>asList(new IntWritable(3), new Text("state0"), new DoubleWritable(3.1)));
        seq2.add(Arrays.<Writable>asList(new IntWritable(4), new Text("state1"), new DoubleWritable(4.1)));

        inputSequences.add(seq1);
        inputSequences.add(seq2);

        JavaRDD<List<List<Writable>>> rdd = sc.parallelize(inputSequences);

        List<List<List<Writable>>> out = new ArrayList<>(SparkTransformExecutor.executeSequenceToSequence(rdd, tp).collect());

        Collections.sort(out, new Comparator<List<List<Writable>>>() {
            @Override
            public int compare(List<List<Writable>> o1, List<List<Writable>> o2) {
                return -Integer.compare(o1.size(), o2.size());
            }
        });

        List<List<List<Writable>>> expectedSequence = new ArrayList<>();
        List<List<Writable>> seq1e = new ArrayList<>();
        seq1e.add(Arrays.<Writable>asList(new IntWritable(0), new IntWritable(2), new DoubleWritable(10.1)));
        seq1e.add(Arrays.<Writable>asList(new IntWritable(1), new IntWritable(1), new DoubleWritable(11.1)));
        seq1e.add(Arrays.<Writable>asList(new IntWritable(2), new IntWritable(0), new DoubleWritable(12.1)));
        List<List<Writable>> seq2e = new ArrayList<>();
        seq2e.add(Arrays.<Writable>asList(new IntWritable(3), new IntWritable(0), new DoubleWritable(13.1)));
        seq2e.add(Arrays.<Writable>asList(new IntWritable(4), new IntWritable(1), new DoubleWritable(14.1)));

        expectedSequence.add(seq1e);
        expectedSequence.add(seq2e);

        assertEquals(expectedSequence, out);
    }

}
