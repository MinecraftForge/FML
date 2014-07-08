package cpw.mods.fml.common.registry;

public class DuplicateAliasException extends Exception {
    public DuplicateAliasException(String message)
    {
        super(message);
    }

    private static final long serialVersionUID = -8034465003500590597L;
}
