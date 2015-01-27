package com.tinkerpop.gremlin.process.graph.step.sideEffect;

import com.tinkerpop.gremlin.process.Traversal;
import com.tinkerpop.gremlin.process.computer.MapReduce;
import com.tinkerpop.gremlin.process.graph.marker.FunctionHolder;
import com.tinkerpop.gremlin.process.graph.marker.MapReducer;
import com.tinkerpop.gremlin.process.graph.marker.Reversible;
import com.tinkerpop.gremlin.process.graph.marker.SideEffectCapable;
import com.tinkerpop.gremlin.process.graph.marker.SideEffectRegistrar;
import com.tinkerpop.gremlin.process.graph.marker.TraversalHolder;
import com.tinkerpop.gremlin.process.graph.step.sideEffect.mapreduce.StoreMapReduce;
import com.tinkerpop.gremlin.process.traverser.TraverserRequirement;
import com.tinkerpop.gremlin.process.util.BulkSet;
import com.tinkerpop.gremlin.process.util.SmartLambda;
import com.tinkerpop.gremlin.process.util.TraversalHelper;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public final class StoreStep<S> extends SideEffectStep<S> implements SideEffectCapable, SideEffectRegistrar, Reversible, FunctionHolder<S, Object>, TraversalHolder, MapReducer<MapReduce.NullObject, Object, MapReduce.NullObject, Object, Collection> {

    private SmartLambda<S, Object> smartLambda = new SmartLambda<>();
    private String sideEffectKey;

    public StoreStep(final Traversal traversal, final String sideEffectKey) {
        super(traversal);
        this.sideEffectKey = sideEffectKey;
        StoreStep.generateConsumer(this);
    }

    @Override
    public void registerSideEffects() {
        if (null == this.sideEffectKey) this.sideEffectKey = this.getId();
        this.traversal.asAdmin().getSideEffects().registerSupplierIfAbsent(this.sideEffectKey, BulkSet::new);
    }

    @Override
    public String getSideEffectKey() {
        return this.sideEffectKey;
    }

    @Override
    public String toString() {
        return TraversalHelper.makeStepString(this, this.sideEffectKey, this.smartLambda);
    }

    @Override
    public MapReduce<MapReduce.NullObject, Object, MapReduce.NullObject, Object, Collection> getMapReduce() {
        return new StoreMapReduce(this);
    }

    @Override
    public void addFunction(final Function<S, Object> function) {
        this.smartLambda.setLambda(function);
    }

    @Override
    public List<Function<S, Object>> getFunctions() {
        return Collections.singletonList(this.smartLambda);
    }

    @Override
    public List<Traversal<S, Object>> getLocalTraversals() {
        return this.smartLambda.getTraversalAsList();
    }

    @Override
    public Set<TraverserRequirement> getRequirements() {
        final Set<TraverserRequirement> requirements = TraversalHolder.super.getRequirements();
        requirements.add(TraverserRequirement.SIDE_EFFECTS);
        requirements.add(TraverserRequirement.BULK);
        return requirements;
    }

    @Override
    public void reset() {
        super.reset();
        this.smartLambda.reset();
    }

    @Override
    public StoreStep<S> clone() throws CloneNotSupportedException {
        final StoreStep<S> clone = (StoreStep<S>) super.clone();
        clone.smartLambda = this.smartLambda.clone();
        clone.executeTraversalOperations(clone.smartLambda.getTraversal(), TYPICAL_LOCAL_OPERATIONS);
        StoreStep.generateConsumer(clone);
        return clone;
    }

    /////////////////////////

    private static final <S> void generateConsumer(final StoreStep<S> storeStep) {
        storeStep.setConsumer(traverser -> TraversalHelper.addToCollection(
                traverser.sideEffects(storeStep.sideEffectKey),
                storeStep.smartLambda.apply((S) traverser),
                traverser.bulk()));
    }
}
