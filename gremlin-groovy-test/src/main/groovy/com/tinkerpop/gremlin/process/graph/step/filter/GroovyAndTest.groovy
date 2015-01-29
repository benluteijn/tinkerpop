package com.tinkerpop.gremlin.process.graph.step.filter

import com.tinkerpop.gremlin.process.Traversal
import com.tinkerpop.gremlin.process.graph.step.ComputerTestHelper
import com.tinkerpop.gremlin.structure.Vertex

import static com.tinkerpop.gremlin.process.graph.__.has
import static com.tinkerpop.gremlin.process.graph.__.outE
import static com.tinkerpop.gremlin.structure.Compare.gt
import static com.tinkerpop.gremlin.structure.Compare.gte

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public abstract class GroovyAndTest {

    public static class StandardTest extends AndTest {

        @Override
        public Traversal<Vertex, String> get_g_V_andXhasXage_gt_27X__outE_count_gt_2X_name() {
            g.V.and(has('age', gt, 27), outE().count.is(gte, 2l)).name
        }
    }

    public static class ComputerTest extends AndTest {

        @Override
        public Traversal<Vertex, String> get_g_V_andXhasXage_gt_27X__outE_count_gt_2X_name() {
            ComputerTestHelper.compute("g.V.and(has('age', gt, 27), outE().count.is(gte, 2l)).name", g)
        }
    }
}
