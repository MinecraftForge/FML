package cpw.mods.fml.common.eventhandler;

public enum EventPriority implements IEventListener
{
    /*Priority of event listeners, listeners will be sorted with respect to this priority level.
     *
     * Note:
     *   Due to using a ArrayList in the ListenerList,
     *   these need to stay in a contiguous index starting at 0. {Default ordinal}
     */
    PRE, //First to execute.  Cancellation is not allowed
    AFTER_PRE, //Cancellation is not allowed
    HIGHEST, //First standard priority to execute
    HIGH,
    NORMAL,
    LOW,
    LOWEST, //Last standard priority to execute
    BEFORE_POST, //Cancellation is not allowed
    POST //Last to execute.  Cancellation is not allowed
;

    @Override
    public void invoke(Event event)
    {
        event.setPhase(this);
    }
}
