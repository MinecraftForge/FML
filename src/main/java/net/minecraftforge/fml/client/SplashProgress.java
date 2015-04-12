package net.minecraftforge.fml.client;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Iterator;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.resources.FileResourcePack;
import net.minecraft.client.resources.FolderResourcePack;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.crash.CrashReport;
import net.minecraft.init.Bootstrap;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Timer;
import net.minecraftforge.fml.common.ProgressManager;
import net.minecraftforge.fml.common.ProgressManager.ProgressBar;
import net.minecraftforge.fml.common.asm.FMLSanityChecker;

import org.apache.commons.io.IOUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.Drawable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.SharedDrawable;

public class SplashProgress
{
    private static Drawable d;
    private static volatile boolean pause = false;
    private static volatile boolean done = false;
    private static Thread thread;
    private static int angle = 0;
    private static final Lock lock = new ReentrantLock(true);
    private static SplashFontRenderer fontRenderer;

    private static final IResourcePack mcPack = Minecraft.getMinecraft().mcDefaultResourcePack;
    private static final IResourcePack fmlPack = createFmlResourcePack();

    private static int fontTexture;
    private static ResourceLocation fontLocation = new ResourceLocation("textures/font/ascii.png");
    private static int logoTexture;
    private static ResourceLocation logoLocation = new ResourceLocation("textures/gui/title/mojang.png");
    private static int forgeTexture;
    private static ResourceLocation forgeLocation = new ResourceLocation("fml", "textures/gui/forge.png");

    private static ResourceLocation configLocation = new ResourceLocation("fml", "splash.properties");
    private static final Properties config = loadConfig();

    private static final boolean enabled = Boolean.parseBoolean(config.getProperty("enabled"));
    private static final int backgroundColor = getInt("background");
    private static final int fontColor = getInt("font");
    private static final int barBorderColor = getInt("barBorder");
    private static final int barColor = getInt("bar");
    private static final int barBackgroundColor = getInt("barBackground");

    public static void start()
    {
        if(!enabled) return;
        // getting debug info out of the way, while we still can
        CrashReport report = CrashReport.makeCrashReport(new Throwable(), "Loading screen debug info");
        Bootstrap.printToSYSOUT(report.getCompleteReport());
        fontTexture = GL11.glGenTextures();
        loadTexture(mcPack, fontTexture, fontLocation);
        logoTexture = GL11.glGenTextures();
        loadTexture(mcPack, logoTexture, logoLocation);
        forgeTexture = GL11.glGenTextures();
        loadTexture(fmlPack, forgeTexture, forgeLocation);

        try
        {
            d = new SharedDrawable(Display.getDrawable());
            Display.getDrawable().releaseContext();
            d.makeCurrent();
        }
        catch (LWJGLException e)
        {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        thread = new Thread(new Runnable()
        {
            private final int barWidth = 400;
            private final int barHeight = 20;
            private final int textHeight2 = 20;
            private final int barOffset = 55;

            public void run()
            {
                setGL();
                while(!done)
                {
                    ProgressBar first = null, penult = null, last = null;
                    Iterator<ProgressBar> i = ProgressManager.barIterator();
                    while(i.hasNext())
                    {
                        if(first == null) first = i.next();
                        else
                        {
                            penult = last;
                            last = i.next();
                        }
                    }

                    GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

                    // matrix setup
                    int w = Display.getWidth();
                    int h = Display.getHeight();
                    GL11.glViewport(0, 0, w, h);
                    GL11.glMatrixMode(GL11.GL_PROJECTION);
                    GL11.glLoadIdentity();
                    GL11.glOrtho(320 - w/2, 320 + w/2, 240 + h/2, 240 - h/2, -1, 1);
                    GL11.glMatrixMode(GL11.GL_MODELVIEW);
                    GL11.glLoadIdentity();

                    // mojang logo
                    setColor(backgroundColor);
                    GL11.glEnable(GL11.GL_TEXTURE_2D);
                    GL11.glBindTexture(GL11.GL_TEXTURE_2D, logoTexture);
                    GL11.glBegin(GL11.GL_QUADS);
                    GL11.glTexCoord2f(0, 0);
                    GL11.glVertex2f(320 - 256, 240 - 256);
                    GL11.glTexCoord2f(0, 1);
                    GL11.glVertex2f(320 - 256, 240 + 256);
                    GL11.glTexCoord2f(1, 1);
                    GL11.glVertex2f(320 + 256, 240 + 256);
                    GL11.glTexCoord2f(1, 0);
                    GL11.glVertex2f(320 + 256, 240 - 256);
                    GL11.glEnd();
                    GL11.glDisable(GL11.GL_TEXTURE_2D);

                    // bars
                    if(first != null)
                    {
                        GL11.glPushMatrix();
                        GL11.glTranslatef(320 - (float)barWidth / 2, 310, 0);
                        drawBar(first);
                        if(penult != null)
                        {
                            GL11.glTranslatef(0, barOffset, 0);
                            drawBar(penult);
                        }
                        if(last != null)
                        {
                            GL11.glTranslatef(0, barOffset, 0);
                            drawBar(last);
                        }
                        GL11.glPopMatrix();
                    }

                    angle += 1;

                    // forge logo
                    setColor(backgroundColor);
                    GL11.glTranslatef(680, 420, 0);
                    GL11.glRotatef(angle, 0, 0, 1);
                    GL11.glEnable(GL11.GL_TEXTURE_2D);
                    GL11.glBindTexture(GL11.GL_TEXTURE_2D, forgeTexture);
                    GL11.glBegin(GL11.GL_QUADS);
                    GL11.glTexCoord2f(0, 0);
                    GL11.glVertex2f(-50, -50);
                    GL11.glTexCoord2f(0, 1);
                    GL11.glVertex2f(-50, 50);
                    GL11.glTexCoord2f(1, 1);
                    GL11.glVertex2f(50, 50);
                    GL11.glTexCoord2f(1, 0);
                    GL11.glVertex2f(50, -50);
                    GL11.glEnd();
                    GL11.glDisable(GL11.GL_TEXTURE_2D);

                    Display.update();
                    if(pause)
                    {
                        clearGL();
                        setGL();
                    }
                    try
                    {
                        Thread.sleep(10);
                    }
                    catch (InterruptedException e)
                    {
                        e.printStackTrace();
                        throw new RuntimeException(e);
                    }
                }
                clearGL();
            }

            private void setColor(int color)
            {
                GL11.glColor3ub((byte)((color >> 16) & 0xFF), (byte)((color >> 8) & 0xFF), (byte)(color & 0xFF));
            }

            private void drawBox(int w, int h)
            {
                GL11.glBegin(GL11.GL_QUADS);
                GL11.glVertex2f(0, 0);
                GL11.glVertex2f(0, h);
                GL11.glVertex2f(w, h);
                GL11.glVertex2f(w, 0);
                GL11.glEnd();
            }

            private void drawBar(ProgressBar b)
            {
                GL11.glPushMatrix();
                // title - message
                setColor(fontColor);
                GL11.glScalef(2, 2, 1);
                GL11.glEnable(GL11.GL_TEXTURE_2D);
                fontRenderer.drawString(b.getTitle() + " - " + b.getMessage(), 0, 0, 0x000000);
                GL11.glDisable(GL11.GL_TEXTURE_2D);
                GL11.glPopMatrix();
                // border
                GL11.glPushMatrix();
                GL11.glTranslatef(0, textHeight2, 0);
                setColor(barBorderColor);
                drawBox(barWidth, barHeight);
                // interior
                setColor(barBackgroundColor);
                GL11.glTranslatef(1, 1, 0);
                drawBox(barWidth - 2, barHeight - 2);
                // slidy part
                setColor(barColor);
                drawBox((barWidth - 2) * b.getStep() / b.getSteps(), barHeight - 2);
                // progress text
                String progress = "" + b.getStep() + "/" + b.getSteps();
                GL11.glTranslatef(((float)barWidth - 2 - fontRenderer.getStringWidth(progress))/ 2, 2, 0);
                setColor(fontColor);
                GL11.glScalef(2, 2, 1);
                GL11.glEnable(GL11.GL_TEXTURE_2D);
                fontRenderer.drawString(progress, 0, 0, 0x000000);
                GL11.glPopMatrix();
            }

            private void setGL()
            {
                lock.lock();
                try
                {
                    Display.getDrawable().makeCurrent();
                }
                catch (LWJGLException e)
                {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
                GL11.glClearColor((float)((backgroundColor >> 16) & 0xFF) / 0xFF, (float)((backgroundColor >> 8) & 0xFF) / 0xFF, (float)(backgroundColor & 0xFF) / 0xFF, 1);
                GL11.glDisable(GL11.GL_LIGHTING);
                GL11.glDisable(GL11.GL_DEPTH_TEST);
                GL11.glEnable(GL11.GL_BLEND);
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                GL11.glEnable(GL11.GL_TEXTURE_2D);
                if(fontRenderer == null)
                {
                    fontRenderer = new SplashFontRenderer();
                }
                GL11.glDisable(GL11.GL_TEXTURE_2D);
            }

            private void clearGL()
            {
                Minecraft mc = Minecraft.getMinecraft();
                mc.displayWidth = Display.getWidth();
                mc.displayHeight = Display.getHeight();
                mc.resize(mc.displayWidth, mc.displayHeight);
                GL11.glClearColor(1, 1, 1, 1);
                GL11.glEnable(GL11.GL_DEPTH_TEST);
                GL11.glDepthFunc(GL11.GL_LEQUAL);
                GL11.glEnable(GL11.GL_ALPHA_TEST);
                GL11.glAlphaFunc(GL11.GL_GREATER, .1f);
                try
                {
                    Display.getDrawable().releaseContext();
                }
                catch (LWJGLException e)
                {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
                finally
                {
                    lock.unlock();
                }
            }
        });
        thread.start();
    }

    /*
     * Call before you need to explicitly modify GL context state during loading.
     * Resource loading doesn't usually require this call.
     * Call {@link #resume()} when you're done.
     */
    public static void pause()
    {
        if(!enabled) return;
        pause = true;
        lock.lock();
        try
        {
            d.releaseContext();
            Display.getDrawable().makeCurrent();
        }
        catch (LWJGLException e)
        {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static void resume()
    {
        if(!enabled) return;
        pause = false;
        try
        {
            Display.getDrawable().releaseContext();
            d.makeCurrent();
        }
        catch (LWJGLException e)
        {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        lock.unlock();
    }

    public static void finish()
    {
        if(!enabled) return;
        try
        {
            done = true;
            thread.join();
            d.releaseContext();
            Display.getDrawable().makeCurrent();
            GL11.glDeleteTextures(fontTexture);
            GL11.glDeleteTextures(logoTexture);
            GL11.glDeleteTextures(forgeTexture);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private static IResourcePack createFmlResourcePack()
    {
        if(FMLSanityChecker.fmlLocation.isDirectory())
        {
            return new FolderResourcePack(FMLSanityChecker.fmlLocation);
        }
        else
        {
            return new FileResourcePack(FMLSanityChecker.fmlLocation);
        }
    }

    private static Properties loadConfig()
    {
        InputStream s = null;
        try
        {
            s = fmlPack.getInputStream(configLocation);
            Properties config = new Properties();
            config.load(s);
            return config;
        }
        catch(IOException e)
        {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        finally
        {
            IOUtils.closeQuietly(s);
        }
    }

    private static int getInt(String name)
    {
        return Integer.decode(config.getProperty(name));
    }

    private static void loadTexture(IResourcePack pack, int name, ResourceLocation location)
    {
        InputStream s = null;
        try
        {
            s = pack.getInputStream(location);
            TextureUtil.uploadTextureImageAllocate(name, TextureUtil.readBufferedImage(s), false, false);
        }
        catch(IOException e)
        {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        finally
        {
            IOUtils.closeQuietly(s);
        }
    }

    private static class SplashFontRenderer extends FontRenderer
    {
        public SplashFontRenderer()
        {
            super(Minecraft.getMinecraft().gameSettings, fontLocation, null, false);
            super.onResourceManagerReload(null);
        }

        @Override
        protected void bindTexture(ResourceLocation location)
        {
            if(location != locationFontTexture) throw new IllegalArgumentException();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, fontTexture);
        }

        @Override
        protected InputStream getResourceInputStream(ResourceLocation location) throws IOException
        {
            return Minecraft.getMinecraft().mcDefaultResourcePack.getInputStream(location);
        }
    }
}
