package net.minecraftforge.event;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.*;
import static java.lang.annotation.ElementType.*;

@Retention(RUNTIME)
@Target(METHOD)
public @interface ForgeSubscribe
{
    public EventPriority priority() default EventPriority.NORMAL;
    public boolean receiveCanceled() default false;
}
