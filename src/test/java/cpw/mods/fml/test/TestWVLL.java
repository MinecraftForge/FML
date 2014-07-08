package cpw.mods.fml.test;

import static org.junit.Assert.*;
import java.util.ArrayList;
import java.util.Iterator;
import org.junit.Test;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import cpw.mods.fml.common.util.WeakValuedLinkedListMultimap;

public class TestWVLL {
    @Test
    public void testPut()
    {
        WeakValuedLinkedListMultimap<String, String> wvll = new WeakValuedLinkedListMultimap<String, String>();
        wvll.put("Hello", "fish");
        wvll.put("Hello", "pepper");
        wvll.put("Hello", "cheese");
        wvll.put("Hello", "jam");
        wvll.put("Hello", "honey");
        wvll.put("Bye", "honey");
        wvll.put("Bye", "jam");

        String[] array = Iterables.toArray(wvll.values("Hello"), String.class);
        assertArrayEquals(new String[] { "honey", "jam", "cheese", "pepper", "fish" }, array);
        array = Iterables.toArray(wvll.values("Bye"), String.class);
        assertArrayEquals(new String[] { "jam", "honey" }, array);
        wvll.clearAndStop();
    }


    @Test
    public void testRemove()
    {
        WeakValuedLinkedListMultimap<String, String> wvll = new WeakValuedLinkedListMultimap<String, String>();
        wvll.put("Hello", "fish");
        wvll.put("Hello", "pepper");
        wvll.put("Hello", "cheese");
        wvll.put("Hello", "jam");
        wvll.put("Hello", "honey");
        wvll.put("Bye", "honey");
        wvll.put("Bye", "jam");

        String[] array = Iterables.toArray(wvll.values("Hello"), String.class);
        assertArrayEquals(new String[] { "honey", "jam", "cheese", "pepper", "fish" }, array);

        wvll.remove("Hello","fish");
        array = Iterables.toArray(wvll.values("Hello"), String.class);
        assertArrayEquals(new String[] { "honey", "jam", "cheese", "pepper" }, array);

        wvll.remove("Hello","cheese");
        array = Iterables.toArray(wvll.values("Hello"), String.class);
        assertArrayEquals(new String[] { "honey", "jam", "pepper" }, array);

        wvll.remove("Hello","honey");
        array = Iterables.toArray(wvll.values("Hello"), String.class);
        assertArrayEquals(new String[] { "jam", "pepper" }, array);

        array = Iterables.toArray(wvll.values("Bye"), String.class);
        assertArrayEquals(new String[] { "jam", "honey" }, array);
        wvll.clearAndStop();
    }

    @Test
    public void testIterator()
    {
        WeakValuedLinkedListMultimap<String, String> wvll = new WeakValuedLinkedListMultimap<String, String>();
        wvll.put("Hello", "fish");
        wvll.put("Hello", "pepper");
        wvll.put("Hello", "cheese");
        wvll.put("Hello", "jam");
        wvll.put("Hello", "honey");
        wvll.put("Bye", "honey");
        wvll.put("Bye", "jam");

        Iterator<String> it = wvll.values("Hello").iterator();
        assertTrue(it.hasNext());
        assertTrue(it.next() == "honey");
        assertTrue(it.hasNext());
        assertTrue(it.next() == "jam");
        assertTrue(it.hasNext());
        assertTrue(it.next() == "cheese");
        assertTrue(it.hasNext());
        assertTrue(it.next() == "pepper");
        assertTrue(it.hasNext());
        assertTrue(it.next() == "fish");
        assertFalse(it.hasNext());

        it = wvll.values("Hello").iterator();
        assertTrue(it.hasNext());
        assertTrue(it.next() == "honey");
        assertTrue(it.hasNext());
        assertTrue(it.next() == "jam");
        assertTrue(it.hasNext());
        assertTrue(it.next() == "cheese");
        assertTrue(it.hasNext());
        assertTrue(it.next() == "pepper");
        assertTrue(it.hasNext());
        assertTrue(it.next() == "fish");
        assertFalse(it.hasNext());

        it = wvll.values("Huh?").iterator();
        assertFalse(it.hasNext());
        wvll.clearAndStop();
    }

    @Test
    public void testWeakBehaviour()
    {
        System.out.println("TWB");
        ArrayList<Object> obj = Lists.newArrayList();
        WeakValuedLinkedListMultimap<String, Object> wvll = new WeakValuedLinkedListMultimap<String, Object>();

        int len = 100000;
        buildList(obj, wvll, len, len);

        long nanoTime = System.nanoTime();
        System.gc();
        long dur = System.nanoTime() - nanoTime;
        System.out.printf("Nanotime %d\n",dur/1000000);

        Object[] array = Iterables.toArray(wvll.values("Hello"), Object.class);
        assertTrue(array.length == len/2);
        wvll.clearAndStop();
    }

    @Test
    public void testConcurrentBehaviour()
    {
        System.out.println("TCB");
        ArrayList<Object> obj = Lists.newArrayList();
        WeakValuedLinkedListMultimap<String, Object> wvll = new WeakValuedLinkedListMultimap<String, Object>();

        int len = 100000;
        buildList(obj, wvll, len, len);

        long nanoTime = System.nanoTime();
        System.gc();
        long dur = System.nanoTime() - nanoTime;
        System.out.printf("Nanotime %d\n",dur/1000000);

        Object[] array = Iterables.toArray(wvll.values("Hello"), Object.class);
        assertTrue(array.length == len/2);
        buildList(obj, wvll, len, len + len/2);

        wvll.clearAndStop();
    }

    private void buildList(ArrayList<Object> obj, WeakValuedLinkedListMultimap<String, Object> wvll, int len, int expectedLen)
    {
        ArrayList<Object> all = Lists.newArrayListWithExpectedSize(len);
        for (int i = 0; i < len; i++)
        {
            Object o = new Object();
            if (i % 2 == 0)
            {
                obj.add(o);
            }
            else
            {
                all.add(o);
            }
            wvll.put("Hello",o);
        }
        Object[] array = Iterables.toArray(wvll.values("Hello"), Object.class);
        assertEquals(expectedLen, array.length);
    }
}
