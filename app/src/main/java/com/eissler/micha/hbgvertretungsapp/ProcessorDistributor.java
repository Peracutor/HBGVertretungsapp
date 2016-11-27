package com.eissler.micha.hbgvertretungsapp;

import android.content.Context;

import java.util.Arrays;
import java.util.List;

/**
 * Created by Micha.
 * 22.11.2016
 */

public class ProcessorDistributor<T> {

    private ProcessorRegisterer<T> processorRegisterer;
    private List<Processor<T>> processors;

    public ProcessorDistributor(ProcessorRegisterer<T> processorRegisterer) {
        this.processorRegisterer = processorRegisterer;
    }

    public void distribute(String action, T object, Context context) {
        if (processors == null) {
            processors = processorRegisterer.register().getProcessors();
        }
        Processor<T> processor = getProcessor(action, context);

        if (processor != null) {
            processor.process(object);
            processor.updateContext(null);
        }
    }

    private Processor<T> getProcessor(String action, Context context) {
        for (Processor<T> processor : processors) {
            if (processor.getAction().equals(action)) {
                processor.updateContext(context);
                return processor;
            }
        }
        //unregistered
        return null;
    }

    public interface ProcessorRegisterer<T> {
        ProcessorRegister<T> register();
    }

    public static class ProcessorRegister<T> {
        private Processor<T>[] processors;

        @SafeVarargs
        public ProcessorRegister(Processor<T>... processors) {
            this.processors = processors;
        }

        public List<Processor<T>> getProcessors() {
            return Arrays.asList(processors);
        }
    }

    public static abstract class Processor<T> {
        private Context context;

        public abstract String getAction();

        public abstract void process(T object);

        private void updateContext(Context context) {
            this.context = context;
        }

        public Context getContext() {
            return context;
        }
    }
}
