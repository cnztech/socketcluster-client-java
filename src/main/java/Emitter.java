/**
 * Created by sachin on 13/11/16.
 */

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;


/**
 * The event emitter which is ported from the JavaScript module. This class is thread-safe.
 *
 * @see <a href="https://github.com/component/emitter">https://github.com/component/emitter</a>
 */
public class Emitter {

    private ConcurrentMap<String, ConcurrentLinkedQueue<Listener>> callbacks
            = new ConcurrentHashMap<String, ConcurrentLinkedQueue<Listener>>();

    private ConcurrentMap<String, ConcurrentLinkedQueue<AckListener>> ackcallbacks
            = new ConcurrentHashMap<String, ConcurrentLinkedQueue<AckListener>>();

    /**
     * Listens on the event.
     * @param event event name.
     * @param fn
     * @return a reference to this object.
     */
    public Emitter on(String event, Listener fn) {
        ConcurrentLinkedQueue<Listener> callbacks = this.callbacks.get(event);
        if (callbacks == null) {
            callbacks = new ConcurrentLinkedQueue <Listener>();
            ConcurrentLinkedQueue<Listener> _callbacks = this.callbacks.putIfAbsent(event, callbacks);
            if (_callbacks != null) {
                callbacks = _callbacks;
            }
        }
        callbacks.add(fn);
        return this;
    }

    public Emitter on(String event, AckListener fn) {
        ConcurrentLinkedQueue<AckListener> ackcallbacks = this.ackcallbacks.get(event);
        if (ackcallbacks == null) {
            ackcallbacks = new ConcurrentLinkedQueue <AckListener>();
            ConcurrentLinkedQueue<AckListener> _callbacks = this.ackcallbacks.putIfAbsent(event, ackcallbacks);
            if (_callbacks != null) {
                ackcallbacks = _callbacks;
            }
        }
        ackcallbacks.add(fn);
        return this;
    }

    /**
     * Adds a one time listener for the event.
     *
     * @param event an event name.
     * @param fn
     * @return a reference to this object.
     */
    public Emitter once(final String event, final Listener fn) {
        this.on(event, new OnceListener(event, fn));
        return this;
    }

    /**
     * Removes all registered listeners.
     *
     * @return a reference to this object.
     */
    public Emitter off() {
        this.callbacks.clear();
        return this;
    }

    /**
     * Removes all listeners of the specified event.
     *
     * @param event an event name.
     * @return a reference to this object.
     */
    public Emitter off(String event) {
        this.callbacks.remove(event);
        return this;
    }

    /**
     * Removes the listener.
     *
     * @param event an event name.
     * @param fn
     * @return a reference to this object.
     */
    public Emitter off(String event, Listener fn) {
        ConcurrentLinkedQueue<Listener> callbacks = this.callbacks.get(event);
        if (callbacks != null) {
            Iterator<Listener> it = callbacks.iterator();
            while (it.hasNext()) {
                Listener internal = it.next();
                if (Emitter.sameAs(fn, internal)) {
                    it.remove();
                    break;
                }
            }
        }
        return this;
    }

    private static boolean sameAs(Listener fn, Listener internal) {
        if (fn.equals(internal)) {
            return true;
        } else if (internal instanceof OnceListener) {
            return fn.equals(((OnceListener) internal).fn);
        } else {
            return false;
        }
    }

    /**
     * Executes each of listeners with the given args.
     *
     * @param event an event name.
     * @param object
     * @return a reference to this object.
     */
    public Emitter handleEmit(String event, Object object) {
        ConcurrentLinkedQueue<Listener> callbacks = this.callbacks.get(event);
        if (callbacks != null) {
            for (Listener fn : callbacks) {
                fn.call(object);
            }
        }
        /**
         * Todo
         * Add here a above code to search the key in callbacks and according add the aclnowledgement
         */

        return this;
    }

    public boolean hasEventAck(String event){
        return this.ackcallbacks.get(event)!=null;
    };

    public Emitter handleEmitAck(String event, Object object , Ack ack){
        ConcurrentLinkedQueue<AckListener> callbacks = this.ackcallbacks.get(event);
            for (AckListener fn : callbacks) {
                fn.call(object,ack);
            }
        return this;
    }


    /**
     * Returns a list of listeners for the specified event.
     *
     * @param event an event name.
     * @return a reference to this object.
     */
    public List<Listener> listeners(String event) {
        ConcurrentLinkedQueue<Listener> callbacks = this.callbacks.get(event);
        return callbacks != null ?
                new ArrayList<Listener>(callbacks) : new ArrayList<Listener>(0);
    }

    /**
     * Check if this emitter has listeners for the specified event.
     *
     * @param event an event name.
     * @return a reference to this object.
     */
    public boolean hasListeners(String event) {
        ConcurrentLinkedQueue<Listener> callbacks = this.callbacks.get(event);
        return callbacks != null && !callbacks.isEmpty();
    }

    public interface Listener {
        void call(Object object);
    }

    public interface AckListener {
        void call (Object object,Ack ack);
    }

    private class OnceListener implements Listener {

        public final String event;
        public final Listener fn;

        public OnceListener(String event, Listener fn) {
            this.event = event;
            this.fn = fn;
        }


        public void call(Object object) {
            Emitter.this.off(this.event, this);
            this.fn.call(object);
        }
    }
}

