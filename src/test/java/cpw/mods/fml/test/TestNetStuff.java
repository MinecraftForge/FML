package cpw.mods.fml.test;

import static org.junit.Assert.*;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;
import com.google.common.base.Strings;
import cpw.mods.fml.common.network.ByteBufUtils;

public class TestNetStuff {

    @Test
    public void testByteBufUtilsByteCount()
    {
        assertEquals("length of 1 is 1", 1, ByteBufUtils.varIntByteCount(1));
        assertEquals("length of 127 is 1", 1, ByteBufUtils.varIntByteCount(127));
        assertEquals("length of 128 is 2", 2, ByteBufUtils.varIntByteCount(128));
        assertEquals("length of 16383 is 2", 2, ByteBufUtils.varIntByteCount(16383));
        assertEquals("length of 16384 is 3", 3, ByteBufUtils.varIntByteCount(16384));
        assertEquals("length of 2097151 is 3", 3, ByteBufUtils.varIntByteCount(2097151));
        assertEquals("length of 2097152 is 4", 4, ByteBufUtils.varIntByteCount(2097152));
        assertEquals("length of 268435455 is 4", 4, ByteBufUtils.varIntByteCount(268435455));
        assertEquals("length of 268435456 is 5", 5, ByteBufUtils.varIntByteCount(268435456));
        assertEquals("length of MIN_VAL is 5", 5, ByteBufUtils.varIntByteCount(Integer.MIN_VALUE));
        assertEquals("length of MAX_VAL is 5", 5, ByteBufUtils.varIntByteCount(Integer.MAX_VALUE));
        assertEquals("length of -1 is 5", 5, ByteBufUtils.varIntByteCount(-1));
    }

    @Test
    public void testByteBufUtilsByteArrays()
    {
        ByteBuf buf = Unpooled.buffer(5, 5);
        ByteBufUtils.writeVarInt(buf, 1, 1);
        assertArrayEquals("1 as byte[] is [1]", new byte[] { 1, 0, 0, 0, 0 }, buf.array());

        buf.clear();
        ByteBufUtils.writeVarInt(buf, 127, 1);
        assertArrayEquals("127 as byte[] is [127]", new byte[] { 127, 0, 0, 0, 0 }, buf.array());

        buf.clear();
        ByteBufUtils.writeVarInt(buf, 128, 2);
        assertArrayEquals("128 as byte[] is [-128, 1]", new byte[] { -128, 1, 0, 0, 0 }, buf.array());

        buf.clear();
        ByteBufUtils.writeVarInt(buf, 16383, 2);
        assertArrayEquals("16383 as byte[] is [-1, 127]", new byte[] { -1, 127, 0, 0, 0 }, buf.array());

        buf.clear();
        ByteBufUtils.writeVarInt(buf, 16384, 3);
        assertArrayEquals("16384 as byte[] is [-1, -128, 1]", new byte[] { -128, -128, 1, 0, 0 }, buf.array());

        buf.clear();
        ByteBufUtils.writeVarInt(buf, 2097151, 3);
        assertArrayEquals("2097151 as byte[] is [-1, -1, 127]", new byte[] { -1, -1, 127, 0, 0 }, buf.array());

        buf.clear();
        ByteBufUtils.writeVarInt(buf, 2097152, 4);
        assertArrayEquals("16384 as byte[] is [-128, -128, 1]", new byte[] { -128, -128, -128, 1, 0 }, buf.array());

        buf.clear();
        ByteBufUtils.writeVarInt(buf, 268435455, 4);
        assertArrayEquals("268435455 as byte[] is [-1, -1, -1, 127]", new byte[] { -1, -1, -1, 127, 0 }, buf.array());

        buf.clear();
        ByteBufUtils.writeVarInt(buf, 268435456, 5);
        assertArrayEquals("268435456 as byte[] is [-1, -128, 1]", new byte[] { -128, -128, -128, -128, 1 }, buf.array());
    }

    @Test
    public void testByteBufUtilsByteReversals()
    {
        ByteBuf buf = Unpooled.buffer(5, 5);
        ByteBufUtils.writeVarInt(buf, 1, 1);
        assertEquals("1 is 1", 1, ByteBufUtils.readVarInt(buf, 1));

        buf.clear();
        ByteBufUtils.writeVarInt(buf, 127, 1);
        assertEquals("127 is 127", 127, ByteBufUtils.readVarInt(buf, 1));

        buf.clear();
        ByteBufUtils.writeVarInt(buf, 128, 2);
        assertEquals("128 is 128", 128, ByteBufUtils.readVarInt(buf, 2));

        buf.clear();
        ByteBufUtils.writeVarInt(buf, 16383, 2);
        assertEquals("16383 is 16383", 16383, ByteBufUtils.readVarInt(buf, 2));

        buf.clear();
        ByteBufUtils.writeVarInt(buf, 16384, 3);
        assertEquals("16384 is 16384", 16384, ByteBufUtils.readVarInt(buf, 3));

        buf.clear();
        ByteBufUtils.writeVarInt(buf, 2097151, 3);
        assertEquals("2097151 is 2097151", 2097151, ByteBufUtils.readVarInt(buf, 3));

        buf.clear();
        ByteBufUtils.writeVarInt(buf, 2097152, 4);
        assertEquals("2097152 is 2097152", 2097152, ByteBufUtils.readVarInt(buf, 4));

        buf.clear();
        ByteBufUtils.writeVarInt(buf, 268435455, 4);
        assertEquals("268435455 is 268435455", 268435455, ByteBufUtils.readVarInt(buf, 4));

        buf.clear();
        ByteBufUtils.writeVarInt(buf, 268435456, 5);
        assertEquals("268435456 is 268435456", 268435456, ByteBufUtils.readVarInt(buf, 5));
    }

    @Test
    public void testByteBufUtilsStrings()
    {
        String test = new String("test");
        ByteBuf buf = Unpooled.buffer(20, 20);
        ByteBufUtils.writeUTF8String(buf, test);
        assertArrayEquals("String bytes", new byte[] { 4, 116, 101, 115, 116, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }, buf.array());

        String repeat = Strings.repeat("test", 100);
        buf = Unpooled.buffer(420, 420);
        ByteBufUtils.writeUTF8String(buf, repeat);
        assertArrayEquals("String repeat bytes", new byte[] {-112, 3, 116, 101, 115, 116, 116, 101, 115, 116, 116, 101, 115, 116, 116, 101, 115, 116, 116, 101, 115, 116, 116, 101, 115, 116, 116, 101, 115, 116, 116, 101, 115, 116, 116, 101, 115, 116, 116, 101, 115, 116, 116, 101, 115, 116, 116, 101, 115, 116, 116, 101, 115, 116, 116, 101, 115, 116, 116, 101, 115, 116, 116, 101, 115, 116, 116, 101, 115, 116, 116, 101, 115, 116, 116, 101, 115, 116, 116, 101, 115, 116, 116, 101, 115, 116, 116, 101, 115, 116, 116, 101, 115, 116, 116, 101, 115, 116, 116, 101, 115, 116, 116, 101, 115, 116, 116, 101, 115, 116, 116, 101, 115, 116, 116, 101, 115, 116, 116, 101, 115, 116, 116, 101, 115, 116, 116, 101, 115, 116, 116, 101, 115, 116, 116, 101, 115, 116, 116, 101, 115, 116, 116, 101, 115, 116, 116, 101, 115, 116, 116, 101, 115, 116, 116, 101, 115, 116, 116, 101, 115, 116, 116, 101, 115, 116, 116, 101, 115, 116, 116, 101, 115, 116, 116, 101, 115, 116, 116, 101, 115, 116, 116, 101, 115, 116, 116, 101, 115, 116, 116, 101, 115, 116, 116, 101, 115, 116, 116, 101, 115, 116, 116, 101, 115, 116, 116, 101, 115, 116, 116, 101, 115, 116, 116, 101, 115, 116, 116, 101, 115, 116, 116, 101, 115, 116, 116, 101, 115, 116, 116, 101, 115, 116, 116, 101, 115, 116, 116, 101, 115, 116, 116, 101, 115, 116, 116, 101, 115, 116, 116, 101, 115, 116, 116, 101, 115, 116, 116, 101, 115, 116, 116, 101, 115, 116, 116, 101, 115, 116, 116, 101, 115, 116, 116, 101, 115, 116, 116, 101, 115, 116, 116, 101, 115, 116, 116, 101, 115, 116, 116, 101, 115, 116, 116, 101, 115, 116, 116, 101, 115, 116, 116, 101, 115, 116, 116, 101, 115, 116, 116, 101, 115, 116, 116, 101, 115, 116, 116, 101, 115, 116, 116, 101, 115, 116, 116, 101, 115, 116, 116, 101, 115, 116, 116, 101, 115, 116, 116, 101, 115, 116, 116, 101, 115, 116, 116, 101, 115, 116, 116, 101, 115, 116, 116, 101, 115, 116, 116, 101, 115, 116, 116, 101, 115, 116, 116, 101, 115, 116, 116, 101, 115, 116, 116, 101, 115, 116, 116, 101, 115, 116, 116, 101, 115, 116, 116, 101, 115, 116, 116, 101, 115, 116, 116, 101, 115, 116, 116, 101, 115, 116, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}, buf.array());
    }

}
