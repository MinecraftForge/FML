package cpw.mods.fml.common.util;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;

public class WeakValuedLinkedListMultimap<K, V> {
    private final ConcurrentMap<K,AtomicReference<Link<V>>> tips = Maps.newConcurrentMap();
    private final WVLLCleaner cleaner;

    private final class WVLLCleaner extends Thread {
        private static final long CLEAN_INTERVAL = 5000;
        private WVLLCleaner()
        {
            setDaemon(true);
            setName("Cleaner thread for WeakValuedLinkedList");
        }
        @Override
        public void run()
        {
            boolean running = true;
            while (running)
            {
                try
                {
                    Thread.sleep(CLEAN_INTERVAL);
                    ImmutableSet<K> keys = ImmutableSet.copyOf(tips.keySet());
                    for (K k : keys)
                    {
                        cleanUp(k);
                    }
                }
                catch (InterruptedException ie)
                {
                    running = false;
                }
            }
        }
    }
    private final class LinkIterator implements Iterator<Link<V>> {
        private Link<V> curr;

        public LinkIterator(K key)
        {
            AtomicReference<Link<V>> atomicReference = tips.get(key);
            curr = atomicReference != null ? atomicReference.get() : null;
        }
        @Override
        public boolean hasNext()
        {
            return curr != null;
        }

        @Override
        public Link<V> next()
        {
            Link<V> rVal = curr;
            curr = curr.succ;
            return rVal;
        }

        @Override
        public void remove()
        {
            throw new UnsupportedOperationException();
        }
    }
    private final class LLIterator implements Iterator<V>
    {
        private Link<V> current;
        public LLIterator(K key)
        {
            AtomicReference<Link<V>> atomicReference = tips.get(key);
            current = atomicReference != null ? atomicReference.get() : null;
        }
        @Override
        public boolean hasNext()
        {
            return current != null;
        }

        @Override
        public V next()
        {
            V rVal = current.get();
            current = current.succ;
            return rVal;
        }

        @Override
        public void remove()
        {
            throw new UnsupportedOperationException();
        }

    }
    private static class Link<V> extends WeakReference<V>
    {
        public Link(V ref)
        {
            super(ref);
        }
        Link<V> succ = null;
    }

    public WeakValuedLinkedListMultimap() {
        this.cleaner = new WVLLCleaner();
        this.cleaner.start();
    }
    public void put(K key, V val)
    {
        Link<V> link = new Link<V>(val);
        AtomicReference<Link<V>> atomicReference= tips.get(key);
        if (atomicReference == null)
        {
            tips.putIfAbsent(key, new AtomicReference<Link<V>>());
            atomicReference = tips.get(key);
        }

        Link<V> oldTip = atomicReference.getAndSet(link);
        if (oldTip != null)
        {
            link.succ = oldTip;
        }
    }

    public void remove(K key, V val)
    {
        cleanUp(key);
        Iterator<Link<V>> iterator = linkValues(key).iterator();
        if (!iterator.hasNext())
        {
            return;
        }
        Link<V> prev = iterator.next();
        if (prev.get() == val)
        {
            tips.get(key).set(prev.succ);
            prev.succ = null;
            return;
        }
        while (iterator.hasNext())
        {
            Link<V> v = iterator.next();
            if (v.get() == val)
            {
                prev.succ = v.succ;
                v.succ = null;
                break;
            }
            prev = v;
        }
    }

    public void clearAndStop()
    {
        try
        {
            cleaner.interrupt();
            cleaner.join();
        } catch (InterruptedException e)
        {
        }
    }
    private void cleanUp(K key)
    {
        AtomicReference<Link<V>> ref = tips.get(key);
        if (ref == null)
            return;
        // find first valid head entry
        Link<V> pred = ref.get();
        Link<V> origPred = pred;
        while (pred != null && pred.get() == null)
        {
            pred = pred.succ;
        }
        // if something else came and updated tip, we just stop the clean. We'll get there next time
        if (!tips.get(key).compareAndSet(origPred, pred))
        {
            return;
        }
        // start walking to the tail
        while (pred != null)
        {
            Link<V> succ = pred.succ;
            // skip successor entries that are invalid
            while (succ != null && succ.get() == null)
            {
                succ = succ.succ;
            }
            pred.succ = succ;
            pred = succ;
        }
    }

    private Iterable<Link<V>> linkValues(final K key)
    {
        return new Iterable<Link<V>>() {
            @Override
            public Iterator<Link<V>> iterator()
            {
                return new LinkIterator(key);
            }
        };
    }
    public Iterable<V> values(final K key)
    {
        cleanUp(key);
        return new Iterable<V>() {
            @Override
            public Iterator<V> iterator()
            {
                return new LLIterator(key);
            }
        };
    }
}
